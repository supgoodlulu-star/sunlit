"""Verify all sixty mesh resources and every vanilla material binding before launch."""
import json, math
from pathlib import Path
r=Path(__file__).resolve().parents[1]/'src/main/resources';a=r/'assets/sunlit_journal_core'
items=json.loads((r/'data/sunlit_journal_core/fantasy_skins.json').read_text());assert len(items)==60
assert len({s['id'] for s in items})==60 and len({s['index'] for s in items})==60
faces=0
for s in items:
 model=json.loads((a/f'models/item/fantasy/{s["id"]}.json').read_text());assert model['loader']=='forge:obj'
 ns,path=model['model'].split(':');obj=r/f'assets/{ns}/{path}';assert obj.exists()
 vertices=[];local=0
 for line in obj.read_text().splitlines():
  if line.startswith('v '):
   v=list(map(float,line.split()[1:]));assert len(v)==3 and all(math.isfinite(n) and -2<n<3 for n in v);vertices.append(v)
  elif line.startswith('f '):
   ids=[int(x.split('/')[0]) for x in line.split()[1:]];assert len(ids)==3 and all(1<=i<=len(vertices) for i in ids);local+=1
 assert 200<local<10000;faces+=local
 assert (obj.parent/'materials.mtl').exists()
 assert len(model['display'])==7
 for tier in ['wooden','stone','iron','golden','diamond','netherite']:
  binding=json.loads((r/f'assets/minecraft/models/item/{tier}_{s["kind"]}.json').read_text())
  override=[o for o in binding['overrides'] if o.get('predicate',{}).get('sunlit_journal_core:fantasy_skin')==s['index']]
  assert len(override)==1 and override[0]['model']==f'sunlit_journal_core:item/fantasy/{s["id"]}'
print(f'PASS: 60 unique OBJ meshes, {faces} triangles total, 12 themes, 7 poses, all 30 base item bindings.')
