import hashlib
import io
import json
import os
from pathlib import Path
import subprocess
import sys
import tarfile
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / 'backup-install.py'

class BackupInstallTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix='timsfit tests ')
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.log = self.root / 'calls.jsonl'
        self.archive = self.root / 'source.tar'
        with tarfile.open(self.archive, 'w') as tar:
            d = tarfile.TarInfo('.'); d.type = tarfile.DIRTYPE; tar.addfile(d)
            for name in ('./files/state.json', './files/state.json.bak', './files/state.json.new'):
                data = b'corrupted JSON preserved verbatim\x00\xff'
                item = tarfile.TarInfo(name); item.size = len(data); tar.addfile(item, io.BytesIO(data))
        self.adb = self.root / 'fake adb'
        self.adb.write_text('''#!/usr/bin/env python3
import json, os, pathlib, sys
args=sys.argv[1:]
with open(os.environ['FAKE_LOG'],'a') as f: f.write(json.dumps(args)+'\\n')
a=args[2:]; mode=os.environ.get('FAKE_MODE','')
if a == ['get-state']: print('offline' if mode=='offline' else 'device')
elif a == ['shell','am','get-current-user']: print('0')
elif a[:4] == ['shell','pm','list','packages']: print('Error: denied' if mode=='inventory-error' else ('package:android' if mode=='absent' else 'package:android\\npackage:com.timsfit.app'))
elif a[:3] == ['shell','pm','path']: pass
elif a[:3] == ['shell','dumpsys','package']: print('versionCode=2 minSdk=26\\nversionName=0.2.0')
elif a[:3] == ['shell','am','force-stop']: pass
elif a[:2] == ['exec-out','run-as']:
 if mode=='permission': print('not debuggable',file=sys.stderr); sys.exit(1)
 sys.stdout.buffer.write(b'bad archive' if mode=='corrupt' else pathlib.Path(os.environ['FAKE_ARCHIVE']).read_bytes())
elif a[:2] == ['install','-r']:
 backups=list(pathlib.Path(os.environ['FAKE_BACKUPS']).rglob('manifest.json'))
 assert backups, 'Install before durable receipt'
 m=json.loads(backups[-1].read_text()); assert m['verified'] is True
 print('Success')
else: sys.exit(99)
''')
        self.adb.chmod(0o700)
        self.aapt = self.root / 'fake aapt'
        self.aapt.write_text("#!/usr/bin/env python3\nprint(\"package: name='com.timsfit.app' versionCode='2' versionName='0.2.0'\")\n")
        self.aapt.chmod(0o700)
        self.apk=self.root/'app with spaces.apk'; self.apk.write_bytes(b'apk')
        self.backups=self.root/'Backups with spaces'

    def run_helper(self, mode='', install=True):
        env=dict(os.environ, FAKE_LOG=str(self.log), FAKE_ARCHIVE=str(self.archive), FAKE_MODE=mode, FAKE_BACKUPS=str(self.backups))
        args=[sys.executable,str(SCRIPT),'install' if install else 'backup','--serial','test serial','--adb',str(self.adb),'--backup-root',str(self.backups),'--saved']
        if install: args+=['--apk',str(self.apk),'--aapt',str(self.aapt)]
        result=subprocess.run(args,env=env,capture_output=True,text=True)
        calls=[json.loads(line) for line in self.log.read_text().splitlines()] if self.log.exists() else []
        return result,calls

    def test_success_and_sidecars_before_install(self):
        r,c=self.run_helper(); self.assertEqual(r.returncode,0,r.stderr)
        self.assertEqual(c[-1],['-s','test serial','install','-r',str(self.apk.resolve())])
        self.assertTrue(all(x[:2]==['-s','test serial'] for x in c))
        archive=next(self.backups.rglob('data.tar'))
        self.assertEqual(archive.read_bytes(),self.archive.read_bytes())
        m=json.loads(next(self.backups.rglob('manifest.json')).read_text())
        self.assertEqual(m['sha256'],hashlib.sha256(archive.read_bytes()).hexdigest())
        self.assertEqual(archive.stat().st_mode & 0o777,0o600)

    def test_permission_failure_never_installs(self):
        r,c=self.run_helper('permission'); self.assertNotEqual(r.returncode,0); self.assertFalse(any('install' in a for a in c)); self.assertTrue(any('exec-out' in a for a in c))

    def test_corrupt_archive_never_installs(self):
        r,c=self.run_helper('corrupt'); self.assertNotEqual(r.returncode,0); self.assertFalse(any('install' in a for a in c))

    def test_unavailable_device_and_inventory_errors_block(self):
        for mode in ('offline', 'inventory-error'):
            with self.subTest(mode=mode):
                self.log.unlink(missing_ok=True)
                r,c=self.run_helper(mode); self.assertNotEqual(r.returncode,0)
                self.assertFalse(any('install' in a for a in c))

    def test_disk_destination_failure_blocks(self):
        self.backups.write_text('not a directory')
        r,c=self.run_helper(); self.assertNotEqual(r.returncode,0)
        self.assertFalse(any('install' in a for a in c))

    def test_unsafe_archive_blocks(self):
        with tarfile.open(self.archive,'w') as tar:
            item=tarfile.TarInfo('../escape'); item.size=1; tar.addfile(item,io.BytesIO(b'x'))
        r,c=self.run_helper(); self.assertNotEqual(r.returncode,0)
        self.assertFalse(any('install' in a for a in c))

    def test_prior_backups_retained(self):
        self.assertEqual(self.run_helper(install=False)[0].returncode,0)
        before={p:p.read_bytes() for p in self.backups.rglob('data.tar')}
        self.assertEqual(self.run_helper(install=False)[0].returncode,0)
        self.assertEqual(len(list(self.backups.rglob('data.tar'))),2)
        for p,b in before.items(): self.assertEqual(p.read_bytes(),b)

    def test_positive_absence_receipt(self):
        r,c=self.run_helper('absent'); self.assertEqual(r.returncode,0,r.stderr)
        m=json.loads(next(self.backups.rglob('manifest.json')).read_text())
        self.assertEqual(m['status'],'no-existing-install'); self.assertFalse(any('force-stop' in a for a in c))

    def test_backup_only_never_installs(self):
        r,c=self.run_helper(install=False); self.assertEqual(r.returncode,0,r.stderr); self.assertFalse(any('install' in a for a in c))

if __name__=='__main__': unittest.main()
