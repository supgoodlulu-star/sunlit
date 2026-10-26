"""Validate shipped JSON, book icon/category targets, PNG headers and Forge metadata."""
from pathlib import Path
import json
import struct
import tomllib

root = Path(__file__).resolve().parents[1] / 'src/main/resources'
count = 0
for path in root.rglob('*.json'):
    data = json.loads(path.read_text(encoding='utf-8'))
    count += 1
    if path.parent.name in ('categories', 'entries'):
        namespace, icon = data['icon'].split(':', 1)
        assert (root / 'assets' / namespace / icon).is_file(), data['icon']
        if 'category' in data:
            namespace, name = data['category'].split(':', 1)
            language = path.parent.parent.name
            assert (root / f'assets/{namespace}/patchouli_books/sunlit_journal/{language}/categories/{name}.json').is_file()
images = list(root.rglob('*.png'))
for path in images:
    payload = path.read_bytes()
    assert payload[:8] == b'\x89PNG\r\n\x1a\n', path
    width, height = struct.unpack('>II', payload[16:24])
    assert width > 0 and height > 0, path
metadata = tomllib.loads((root / 'META-INF/mods.toml').read_text(encoding='utf-8'))
assert metadata['mods'][0]['modId'] == 'sunlit_journal_core'
print(f'PASS: {count} JSON documents, {len(images)} PNG headers, all icon/category targets, mods.toml')
