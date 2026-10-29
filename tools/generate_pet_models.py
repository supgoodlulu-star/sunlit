#!/usr/bin/env python3
"""Build the sixty reference-directed, three-times-resolution 3D pet sculptures."""
import json,time
from pathlib import Path
from pet_art.sculpt import sculpt
from pet_art.accessories import decorate
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/sunlit_journal_core'
(root/'pets').mkdir(parents=True,exist_ok=True)
rows=json.loads((Path(__file__).parent/'art-sources/pet_catalog.json').read_text())
for id,name,accessory,season,fantasy in rows:
    model=sculpt(id,season)
    decorate(model,id,season)
    faces=model.mesh()
    data={'format':2,'voxelSize':1/3,'scale':.032 if id.endswith('_fairy') else .045,'faces':faces}
    (root/'pets'/f'{id}.json').write_text(json.dumps(data,separators=(',',':')))
    print(f'{id}: {len(faces)} merged surface quads',flush=True)
# White pixel used with vertex colors; original resource not a concept image.
from PIL import Image,ImageDraw
tex=root/'textures/pets';tex.mkdir(parents=True,exist_ok=True);Image.new('RGBA',(2,2),'white').save(tex/'palette.png')
for item in ['pet_doll','pet_ticket','pet_vending','pet_accessory_shop']:
 path=root/'models/item';path.mkdir(parents=True,exist_ok=True)
 (path/f'{item}.json').write_text(json.dumps({'parent':'builtin/entity','display':{'firstperson_righthand':{'rotation':[0,25,0],'translation':[0,1,0],'scale':[.42,.42,.42]},'firstperson_lefthand':{'rotation':[0,-25,0],'translation':[0,1,0],'scale':[.42,.42,.42]},'thirdperson_righthand':{'rotation':[30,0,0],'translation':[0,2,0],'scale':[.45,.45,.45]},'thirdperson_lefthand':{'rotation':[30,0,0],'translation':[0,2,0],'scale':[.45,.45,.45]},'ground':{'translation':[0,2,0],'scale':[.5,.5,.5]}}} if item=='pet_doll' else {'parent':f'sunlit_journal_core:block/{item}'} if item in ['pet_vending','pet_accessory_shop'] else {'parent':'minecraft:item/generated','textures':{'layer0':'sunlit_journal_core:item/pet_ticket'}}))
im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im);d.rectangle((1,4,14,11),fill='#c69a51',outline='#67452f');d.rectangle((3,5,12,10),fill='#f7e5b8');d.rectangle((7,6,9,9),fill='#799866');im.save(root/'textures/item/pet_ticket.png')
blocktex=root/'textures/block';blocktex.mkdir(parents=True,exist_ok=True)
im=Image.new('RGBA',(16,16),'#bb8e55');d=ImageDraw.Draw(im)
for y in [0,5,10,15]:d.line((0,y,15,y),fill='#8b613d')
for y in [2,8,12]:d.line((3,y,11,y),fill='#cda36b')
im.save(blocktex/'pet_wood.png')
im=Image.new('RGBA',(16,16),'#6d9159');d=ImageDraw.Draw(im)
for x in [0,6,12]:d.rectangle((x,0,x+2,15),fill='#e4d3a3')
im.save(blocktex/'pet_canopy.png')
for name,greenfront in [('pet_vending',False),('pet_accessory_shop',True)]:
 im=Image.new('RGBA',(16,16),'#b9864d');d=ImageDraw.Draw(im)
 for y in [1,5,10,14]:d.line((0,y,15,y),fill='#98673d')
 d.rectangle((2,3,13,12),fill='#45664c' if greenfront else '#6c543d',outline='#e3bb6e');d.rectangle((4,5,10,9),fill='#f4dda3');d.rectangle((6,6,8,8),fill='#8b6d43');d.rectangle((11,8,12,10),fill='#daa54c');im.save(blocktex/f'{name}.png')
 block=root/'models/block';block.mkdir(parents=True,exist_ok=True)
 model={'textures':{'wood':'sunlit_journal_core:block/pet_wood','front':f'sunlit_journal_core:block/{name}','canopy':'sunlit_journal_core:block/pet_canopy','particle':'sunlit_journal_core:block/pet_wood'},'elements':[]}
 def element(a,b,texture='wood',front=False):model['elements'].append({'from':a,'to':b,'faces':{s:{'texture':'#front' if front and s in ['north','south'] else '#'+texture} for s in ['north','south','east','west','up','down']}})
 element([1,0,1],[15,14,15],front=True);element([0,14,0],[16,16,16])
 element([1,16,12],[3,24,14]);element([13,16,12],[15,24,14]);element([0,24,0],[16,26,16],'canopy')
 (block/f'{name}.json').write_text(json.dumps(model));bs=root/'blockstates';bs.mkdir(exist_ok=True);(bs/f'{name}.json').write_text(json.dumps({'variants':{'':{'model':f'sunlit_journal_core:block/{name}'}}}))
lang=root/'lang/ko_kr.json';data=json.loads(lang.read_text());data.update({'item.sunlit_journal_core.pet_ticket':'펫 뽑기권','item.sunlit_journal_core.pet_doll':'펫 인형','block.sunlit_journal_core.pet_vending':'펫 자판기','block.sunlit_journal_core.pet_accessory_shop':'펫 장식 가게','entity.sunlit_journal_core.companion':'생활 친구'});lang.write_text(json.dumps(data,ensure_ascii=False,indent=2))
print('Generated',len(rows),'original voxel models with separate accessories.')
