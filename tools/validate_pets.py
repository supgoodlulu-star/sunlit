#!/usr/bin/env python3
"""Check the shipped 60 original 3D meshes and their independent accessory layers."""
import json, hashlib, re
from pathlib import Path
root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/sunlit_journal_core'
catalog=json.loads((root/'tools/art-sources/pet_catalog.json').read_text())
java=(root/'src/main/java/dev/sunlit/journal/PetCatalog.java').read_text()
entries=re.findall(r'new Species\("([^"\n]+)","([^"\n]+)","([^"\n]+)","([^"\n]*)",(true|false)\)',java)
assert [list(row[:4])+[row[4]=='true'] for row in entries]==catalog,'Java and resource catalogs diverged'
assert len(catalog)==60 and len({r[0] for r in catalog})==60
assert sum(bool(r[3]) for r in catalog)==20
hashes=set()
for id,name,accessory,season,fantasy in catalog:
    model=json.loads((assets/'pets'/f'{id}.json').read_text())
    assert model.get('format')==2 and abs(model.get('voxelSize',0)-1/3)<1e-6,id
    faces=model['faces'];groups={f[5] for f in faces}
    assert {'eyes','smile','accessory'}<=groups,(id,groups)
    assert all(len(f)==9 and 0<=f[3]<=5 and 0<=f[4]<=0xffffff for f in faces),id
    assert all(0<value<=32 for f in faces for value in f[6:]),id
    assert all(-32<=value<=32 for f in faces for value in f[:3]),id
    assert model['scale']==(.032 if id.endswith('_fairy') else .045),id
    body=[f for f in faces if f[5]!='accessory']
    height=(max(f[1]+f[7] for f in body)-min(f[1] for f in body))*model['scale']
    assert height<=1.15,(id,height)
    if id.endswith('_fairy'):assert height<=.6,(id,height)
    accessory_hash=hashlib.sha256(json.dumps([f for f in faces if f[5]=='accessory']).encode()).hexdigest()
    hashes.add(accessory_hash)
assert len(hashes)==60,'Accessories unexpectedly collapsed to one shared mesh'
for name in ['pet_vending','pet_accessory_shop']:
    block=json.loads((assets/'models/block'/f'{name}.json').read_text())
    for key,texture in block['textures'].items():
        assert texture.startswith('sunlit_journal_core:block/')
        assert (assets/'textures'/f'{texture.split(":",1)[1]}.png').is_file(),texture
print('PASS: 60 species, 20 legends, separate eye/smile/accessory layers, sizes, meshes, machine textures.')
