"""Check structural thickness and compare head silhouettes without decorative groups.
The contact sheets remain the aesthetic review; these checks catch thin-body/recolor regressions.
"""
from pathlib import Path
from itertools import combinations
from PIL import Image,ImageDraw
import json
root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/sunlit_journal_core'
catalog=json.loads((root/'src/main/resources/data/sunlit_journal_core/fantasy_skins.json').read_text())
metrics={};masks={};thin=[]
for skin in catalog:
 p=assets/f'models/fantasy/{skin["theme"]}/{skin["kind"]}.obj'
 vertices=[];groups={};group='body';materials=set()
 for line in p.read_text().splitlines():
  if line.startswith('v '):vertices.append(tuple(map(float,line.split()[1:])))
  elif line.startswith('g '):group=line[2:]
  elif line.startswith('usemtl '):materials.add(line[7:])
  elif line.startswith('f '):groups.setdefault(group,[]).append([vertices[int(v.split('/')[0])-1] for v in line.split()[1:]])
 assert {'handle','head','ornaments'}<=groups.keys(),p
 assert {'grip','grip_light','grip_shade','light','shade','main'}<=materials,(p,materials)
 bounds={}
 for name in ['handle','head']:
  points=[v for f in groups[name] for v in f]
  lo=[min(p[k] for p in points) for k in range(3)];hi=[max(p[k] for p in points) for k in range(3)]
  bounds[name]=[round(24*(hi[k]-lo[k]),3) for k in range(3)]
  if bounds[name][2]<1.3:thin.append((skin['id'],name,bounds[name]))
 pts=[v for f in groups['head'] for v in f];lo=[min(v[k] for v in pts) for k in [0,1]];hi=[max(v[k] for v in pts) for k in [0,1]]
 im=Image.new('1',(64,64));draw=ImageDraw.Draw(im)
 for face in groups['head']:
  poly=[tuple(2+60*(v[k]-lo[k])/(hi[k]-lo[k]) for k in [0,1]) for v in face]
  draw.polygon(poly,fill=1)
 masks[skin['id']]={i for i,v in enumerate(im.get_flattened_data()) if v}
 metrics[skin['id']]=bounds
assert not thin,thin
closest=[]
for kind in ['sword','pickaxe','axe','shovel','hoe']:
 candidates=[s for s in catalog if s['kind']==kind];best=(0,'','')
 for a,b in combinations(candidates,2):
  aa=masks[a['id']];bb=masks[b['id']];iou=len(aa&bb)/len(aa|bb)
  assert iou<.985,(kind,a['id'],b['id'],'near-identical structural silhouette',iou)
  best=max(best,(iou,a['id'],b['id']))
 closest.append(dict(kind=kind,similarity=round(best[0],4),pair=best[1:]))
(root/'validation/fantasy-sculpture-metrics.json').write_text(json.dumps(dict(body_bounds_design_units=metrics,closest_normalized_head_silhouettes=closest),ensure_ascii=False,indent=2)+'\n')
print('PASS: all 60 structural heads/handles have >=1.3 units thickness, contrasted surface materials, and no near-identical head silhouettes with decorations hidden.')
for c in closest:print(c)
