#!/usr/bin/env python3
"""Fresh private-data backup before an optional in-place TimsFit APK update.

Usage: backup-install.py backup --serial SERIAL --saved
       backup-install.py install --serial SERIAL --saved --apk FILE.apk

--saved confirms pending edits are saved before force-stop. Do not interact with
or restart the app until this command completes. Backup failure leaves evidence
on disk and NEVER proceeds to install. No restore, uninstall, or clear-data path.
Requires debug run-as access and Android user 0; other users fail closed.
"""
import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import subprocess
import sys
import tarfile
import uuid

PACKAGE = 'com.timsfit.app'
SDK = Path.home() / '.local/share/timsfit/android-sdk'


def digest(path):
    checksum = hashlib.sha256()
    with path.open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b''):
            checksum.update(chunk)
    return checksum.hexdigest()


def run(args, output=None):
    result = subprocess.run(args, stdout=output or subprocess.PIPE,
                            stderr=subprocess.PIPE, timeout=180)
    if result.returncode or result.stderr.strip():
        # Do not relay device output: it may contain private data.
        raise RuntimeError('Command failed or reported an error; installation blocked.')
    return result.stdout.decode('utf-8', errors='strict').strip() if output is None else None


def durable(path, data):
    with path.open('xb') as stream:
        stream.write(data)
        stream.flush()
        os.fsync(stream.fileno())
    if path.read_bytes() != data:
        raise RuntimeError('Backup metadata read-back mismatch.')


def verify_archive(path):
    size = path.stat().st_size
    if size < 1024 or size % 512:
        raise RuntimeError('Invalid/truncated tar length.')
    with path.open('rb') as stream:
        stream.seek(-1024, 2)
        if stream.read() != b'\0' * 1024:
            raise RuntimeError('Tar end marker missing.')
    count = 0
    root = False
    seen = set()
    with tarfile.open(path, 'r:') as archive:
        for member in archive:
            name = PurePosixPath(member.name)
            if name.is_absolute() or '..' in name.parts or str(name) in seen:
                raise RuntimeError('Unsafe or duplicate archive path.')
            seen.add(str(name))
            if not (member.isdir() or member.isfile()):
                raise RuntimeError('Unsupported archive member; preserve evidence and investigate.')
            if str(name) == '.':
                root = member.isdir()
            if member.isfile():
                # Read every byte without interpreting JSON or printing contents.
                with archive.extractfile(member) as stream:
                    remaining = member.size
                    while remaining:
                        chunk = stream.read(min(1024 * 1024, remaining))
                        if not chunk:
                            raise RuntimeError('Truncated archived file.')
                        remaining -= len(chunk)
                count += 1
    if not root:
        raise RuntimeError('Expected app-private root directory missing from tar.')
    return count


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=['backup', 'install'])
    parser.add_argument('--serial', required=True)
    parser.add_argument('--saved', required=True, action='store_true', help='Confirm edits saved before stopping app')
    parser.add_argument('--apk', type=Path)
    parser.add_argument('--adb', default=str(SDK / 'platform-tools/adb'))
    parser.add_argument('--aapt', default=str(SDK / 'build-tools/35.0.0/aapt2'))
    parser.add_argument('--backup-root', type=Path, default=Path.home() / 'Documents/TimsFit Backups')
    args = parser.parse_args()
    os.umask(0o077)
    if not args.serial.strip():
        raise RuntimeError('Explicit nonempty device serial required.')
    apk = None
    apk_hash = None
    if args.command == 'install':
        if not args.apk or not args.apk.is_file():
            raise RuntimeError('Readable APK file required for install.')
        apk = args.apk.resolve()
        badging = run([args.aapt, 'dump', 'badging', str(apk)])
        if not re.search(r"^package: name='com\.timsfit\.app' ", badging, re.M):
            raise RuntimeError('APK identity is not com.timsfit.app.')
        apk_hash = digest(apk)
    elif args.apk:
        raise RuntimeError('--apk is only valid for install.')
    root = args.backup_root.expanduser().resolve()
    if any((p / '.git').exists() for p in (root, *root.parents)):
        raise RuntimeError('Backup destination must be outside Git repositories.')
    adb = [args.adb, '-s', args.serial]
    if run(adb + ['get-state']) != 'device':
        raise RuntimeError('Selected device is not connected and authorized.')
    if run(adb + ['shell', 'am', 'get-current-user']) != '0':
        raise RuntimeError('Only Android user 0 is supported; no data was assumed empty.')
    packages = run(adb + ['shell', 'pm', 'list', 'packages', '--user', '0'])
    lines = packages.splitlines()
    if not lines or any(not re.fullmatch(r'package:[A-Za-z0-9_.]+', line) for line in lines):
        raise RuntimeError('Package inventory not positively verified.')
    installed = 'package:' + PACKAGE in lines
    if not installed and run(adb + ['shell', 'pm', 'path', '--user', '0', PACKAGE]):
        raise RuntimeError('Package absence confirmation disagrees with inventory.')
    now = datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S.%fZ')
    device_dir = re.sub(r'[^A-Za-z0-9_.-]', '_', args.serial)[:80] + '-' + hashlib.sha256(args.serial.encode()).hexdigest()[:8]
    folder = root / device_dir / (now + '-' + uuid.uuid4().hex[:8])
    folder.mkdir(parents=True, mode=0o700)
    os.chmod(root, 0o700)
    os.chmod(folder.parent, 0o700)
    metadata = dict(device=args.serial, android_user=0, package=PACKAGE, utc=now,
                    verified=False, status='pending', apk_sha256=apk_hash)
    if installed:
        version = run(adb + ['shell', 'dumpsys', 'package', PACKAGE])
        code = re.search(r'\bversionCode=(\d+)', version)
        name = re.search(r'\bversionName=([^\r\n]+)', version)
        if not code or not name:
            raise RuntimeError('Installed version could not be read.')
        metadata.update(version_code=code.group(1), version_name=name.group(1).strip())
        run(adb + ['shell', 'am', 'force-stop', '--user', '0', PACKAGE])
        archive = folder / 'data.tar'
        with archive.open('xb') as stream:
            run(adb + ['exec-out', 'run-as', PACKAGE, 'tar', '-cf', '-', '.'], output=stream)
            stream.flush()
            os.fsync(stream.fileno())
        metadata.update(status='backed-up', archive='data.tar',
                        file_count=verify_archive(archive), sha256=digest(archive))
        durable(folder / 'data.tar.sha256', (metadata['sha256'] + '  data.tar\n').encode())
        if digest(archive) != metadata['sha256']:
            raise RuntimeError('Archive checksum verification failed.')
    else:
        metadata.update(status='no-existing-install', version_code=None, version_name=None)
    metadata['verified'] = True
    manifest = folder / 'manifest.json'
    durable(manifest, (json.dumps(metadata, indent=2) + '\n').encode())
    durable(folder / 'manifest.json.sha256', (digest(manifest) + '  manifest.json\n').encode())
    descriptor = os.open(folder, os.O_RDONLY)
    try:
        os.fsync(descriptor)
    finally:
        os.close(descriptor)
    print('Verified backup/receipt:', folder, flush=True)
    if apk:
        if digest(apk) != apk_hash:
            raise RuntimeError('APK changed after identity check.')
        result = run(adb + ['install', '-r', str(apk)])
        if not re.search(r'^Success$', result, re.M):
            raise RuntimeError('Install did not report success; backup retained.')
        durable(folder / 'install-result.json', b'{"result":"Success","mode":"in-place"}\n')
        print('In-place installation succeeded; backup retained.')


if __name__ == '__main__':
    try:
        main()
    except (OSError, RuntimeError, ValueError, tarfile.TarError, subprocess.SubprocessError) as error:
        # No raw device output, archived names, or saved workout contents.
        reason = str(error) if isinstance(error, RuntimeError) else type(error).__name__
        print('Stopped safely: ' + reason + ' No further install attempted; retain backup evidence.', file=sys.stderr)
        sys.exit(1)
