"""Verify the original backup; pass --restore explicitly to restore its two source files."""
from pathlib import Path
from datetime import datetime
import hashlib
import json
import shutil
import sys

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
manifest = json.loads((HERE/'manifest.json').read_text())
for entry in manifest['files']:
    source = HERE/entry['backup']
    assert hashlib.sha256(source.read_bytes()).hexdigest() == entry['sha256'], source
    assert (ROOT/entry['path']).resolve().is_relative_to(ROOT), entry['path']
print('Original backup verified:', manifest['source_commit'])
if '--restore' not in sys.argv:
    print('No changes made. Pass --restore only when you want the original artwork back.')
    raise SystemExit(0)

saved = HERE/('saved-upgrade-'+datetime.now().strftime('%Y%m%d-%H%M%S-%f'))
saved.mkdir()
tests = [
    'app/src/test/java/com/lsing/timego/ui/common/MuscleBodyArtTest.kt',
    'app/src/androidTest/java/com/lsing/timego/ui/common/MuscleBodyDiagramTest.kt',
]
for name in [e['path'] for e in manifest['files']] + tests:
    source = (ROOT/name).resolve()
    assert source.is_relative_to(ROOT)
    if source.exists():
        destination = saved/name
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, destination)
for entry in manifest['files']:
    shutil.copy2(HERE/entry['backup'], ROOT/entry['path'])
for name in tests:
    source = (ROOT/name).resolve()
    assert source.is_relative_to(ROOT)
    if source.exists():
        # Move instead of delete; the separate prior copy also preserves the original path.
        shutil.move(str(source), str(saved/(source.name+'.disabled')))
print('Original source restored. Previous upgrade preserved in', saved)
print('Rebuild and verify before installing. No device operation was performed.')
