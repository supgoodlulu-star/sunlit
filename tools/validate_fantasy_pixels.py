"""Validate the actual OBJ UV references and painted sprites, catching constant-UV flat colours."""
from pathlib import Path
from PIL import Image
import json
root=Path(__file__).resolve().parents[1];a=root/'src/main/resources/assets/sunlit_journal_core'
skins=json.loads((root/'src/main/resources/data/sunlit_journal_core/fantasy_skins.json').read_text())
textures=set();mapped=0;total=0
for skin in skins:
 folder=a/f'models/fantasy/{skin["theme"]}';uv=[];valid=0;faces=0
 for line in (folder/f'{skin["kind"]}.obj').read_text().splitlines():
  if line.startswith('vt '):
   p=tuple(map(float,line.split()[1:]));assert len(p)==2 and all(0<=n<=1 for n in p);uv.append(p)
  elif line.startswith('f '):
   ids=[int(p.split('/')[1])-1 for p in line.split()[1:]];assert all(0<=i<len(uv) for i in ids)
   aa,bb,cc=[uv[i] for i in ids];area=abs((bb[0]-aa[0])*(cc[1]-aa[1])-(bb[1]-aa[1])*(cc[0]-aa[0]))
   if area>1e-9:valid+=1
   faces+=1
 assert len(set(uv))>80,(skin['id'],'constant or missing UVs')
 assert valid/faces>.95,(skin['id'],valid,faces)
 mapped+=valid;total+=faces
 for line in (folder/'materials.mtl').read_text().splitlines():
  if line.startswith('Kd '):assert line=='Kd 1.0 1.0 1.0','double tint'
  if line.startswith('map_Kd '):
   ns,path=line.split()[1].split(':');assert path.startswith('item/fantasy_materials/'),path
   texture=root/f'src/main/resources/assets/{ns}/textures/{path}.png';assert texture.is_file();textures.add(texture)
for p in textures:
 with Image.open(p) as img:
  assert img.size in [(16,16),(32,32),(64,64)],p
  colors=img.getcolors(4096);assert colors is not None and len(colors)>=3,(p,'flat sprite')
  if p.stem in ['main','light','gem','grip','metal']:assert len(colors)>=5,(p,len(colors))
print(f'PASS: {len(textures)} painted sprites, valid in-sprite UV references on all 60 models, {mapped}/{total} nondegenerate UV triangles, neutral Kd.')
