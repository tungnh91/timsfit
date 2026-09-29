"""Independent host-only regressions. Never invoke the real adb or Gradle."""
import io
import os
from pathlib import Path
import shutil
import subprocess
import tarfile
import tempfile
import unittest
import test_backup_install as fixtures

SCRIPTS = Path(__file__).resolve().parents[1]


class IndependentBackupReview(unittest.TestCase):
    def setUp(self):
        self.fixture = fixtures.BackupInstallTests()
        self.fixture.setUp()
        self.addCleanup(self.fixture.doCleanups)

    def blocked(self, mode=''):
        result, calls = self.fixture.run_helper(mode)
        self.assertNotEqual(0, result.returncode)
        self.assertFalse(any('install' in call for call in calls))
        self.assertNotIn('corrupted JSON preserved', result.stdout + result.stderr)

    def test_absence_conflict_blocks_install(self):
        p = self.fixture.adb
        p.write_text(p.read_text().replace("elif a[:3] == ['shell','pm','path']: pass", "elif a[:3] == ['shell','pm','path']: print('package:/data/app/timsfit/base.apk')"))
        self.blocked('absent')

    def test_zero_exit_error_on_absence_probe_blocks_install(self):
        p = self.fixture.adb
        p.write_text(p.read_text().replace("elif a[:3] == ['shell','pm','path']: pass", "elif a[:3] == ['shell','pm','path']: print('Error: permission denied')"))
        self.blocked('absent')

    def test_wrong_android_user_blocks_install(self):
        p = self.fixture.adb
        p.write_text(p.read_text().replace("get-current-user']: print('0')", "get-current-user']: print('10')"))
        self.blocked()

    def test_archive_missing_end_markers_blocks_install(self):
        p = self.fixture.archive
        p.write_bytes(p.read_bytes()[:-1024] + b'x' * 1024)
        self.blocked()

    def test_symlink_member_blocks_install(self):
        with tarfile.open(self.fixture.archive, 'w') as archive:
            root = tarfile.TarInfo('.'); root.type = tarfile.DIRTYPE; archive.addfile(root)
            link = tarfile.TarInfo('./files/log'); link.type = tarfile.SYMTYPE; link.linkname = '/outside'; archive.addfile(link)
        self.blocked()

    def test_backup_preserves_other_saved_storage_and_binary_sidecars(self):
        expected = {'./files/training-log-v1.json': b'private-log', './files/training-log-v1.json.bak': b'old\x00', './files/training-log-v1.json.new': b'pending\xff', './databases/history.db': b'sqlite', './shared_prefs/settings.xml': b'prefs', './no_backup/other': b'extra'}
        with tarfile.open(self.fixture.archive, 'w') as archive:
            root = tarfile.TarInfo('.'); root.type = tarfile.DIRTYPE; archive.addfile(root)
            for path, data in expected.items():
                item = tarfile.TarInfo(path); item.size = len(data); archive.addfile(item, io.BytesIO(data))
        result, _ = self.fixture.run_helper()
        self.assertEqual(0, result.returncode, result.stderr)
        with tarfile.open(next(self.fixture.backups.rglob('data.tar'))) as archive:
            for path, data in expected.items(): self.assertEqual(data, archive.extractfile(path).read())
        self.assertNotIn('private-log', result.stdout + result.stderr)


class IndependentWrapperReview(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory(prefix='timsfit wrapper review ')
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        (self.root / 'scripts').mkdir()
        shutil.copy2(SCRIPTS / 'gradle.sh', self.root / 'scripts/gradle.sh')
        for name in ('guard-gradle.init.gradle',):
            if (SCRIPTS / name).exists(): shutil.copy2(SCRIPTS / name, self.root / 'scripts' / name)
        (self.root / 'scripts/backup-install.py').write_text('import os,sys\nprint("FAKE_BACKUP")\nsys.exit(int(os.environ.get("REVIEW_BACKUP_EXIT", "0")))\n')
        (self.root / 'gradlew').write_text('#!/bin/sh\necho REACHED_GRADLE "$@"\n')
        (self.root / 'gradlew').chmod(0o700)
        self.env = dict(os.environ)
        self.env.pop('ANDROID_SERIAL', None)
        self.env.pop('TIMSFIT_EDITS_SAVED', None)

    def invoke(self, task):
        return subprocess.run(['bash', str(self.root / 'scripts/gradle.sh'), task], env=self.env, capture_output=True, text=True)

    def test_abbreviated_install_requires_backup(self):
        result = self.invoke(':app:iD')
        self.assertNotEqual(0, result.returncode)
        self.assertNotIn('REACHED_GRADLE', result.stdout)

    def test_abbreviated_connected_requires_backup(self):
        result = self.invoke(':app:cDAT')
        self.assertNotEqual(0, result.returncode)
        self.assertNotIn('REACHED_GRADLE', result.stdout)

    def test_phone_connected_rejected_even_with_saved_ack(self):
        self.env.update(ANDROID_SERIAL='fake-phone', TIMSFIT_EDITS_SAVED='1')
        result = self.invoke(':app:connectedDebugAndroidTest')
        self.assertNotEqual(0, result.returncode)
        self.assertNotIn('REACHED_GRADLE', result.stdout)

    def test_backup_failure_never_reaches_gradle(self):
        self.env.update(ANDROID_SERIAL='emulator-FAKE', TIMSFIT_EDITS_SAVED='1', REVIEW_BACKUP_EXIT='1')
        result = self.invoke(':app:connectedDebugAndroidTest')
        self.assertNotEqual(0, result.returncode)
        self.assertNotIn('REACHED_GRADLE', result.stdout)

    def test_explicit_emulator_install_backs_up_before_gradle(self):
        self.env.update(ANDROID_SERIAL='emulator-FAKE', TIMSFIT_EDITS_SAVED='1')
        result = self.invoke(':app:installDebug')
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertLess(result.stdout.index('FAKE_BACKUP'), result.stdout.index('REACHED_GRADLE'))

    def test_saved_ack_required_for_full_install_task(self):
        self.env['ANDROID_SERIAL'] = 'emulator-FAKE'
        result = self.invoke(':app:installDebug')
        self.assertNotEqual(0, result.returncode)
        self.assertNotIn('REACHED_GRADLE', result.stdout)

    def test_uninstall_is_not_an_allowed_workflow(self):
        self.env.update(ANDROID_SERIAL='emulator-FAKE', TIMSFIT_EDITS_SAVED='1')
        result = self.invoke(':app:uninstallDebug')
        self.assertNotEqual(0, result.returncode)
        self.assertNotIn('REACHED_GRADLE', result.stdout)
