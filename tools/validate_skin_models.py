"""Check the shipped vanilla model geometry, bounds, texture bindings and item coverage."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]/'src/main/resources'
for name in ['starlight_sword','cloud_pickaxe']:
 data=json.loads((root/f'assets/sunlit_journal_core/models/item/{name}.json').read_text())
 assert data['elements'] and len(data['elements'])<200
 for element in data['elements']:
  assert all(-16<=a<b<=32 for a,b in zip(element['from'],element['to']))
  if 'rotation' in element: assert element['rotation']['angle'] in [-45,-22.5,0,22.5,45]
  for face in element['faces'].values():
   assert face['texture'][1:] in data['textures']
   assert all(0<=x<=16 for x in face['uv'])
 for texture in data['textures'].values():
  ns,path=texture.split(':');assert (root/f'assets/{ns}/textures/{path}.png').is_file()
 for transform in ['gui','firstperson_righthand','firstperson_lefthand','thirdperson_righthand','thirdperson_lefthand','ground','fixed']:
  assert transform in data['display']
 for tier in ['wooden','stone','iron','golden','diamond','netherite']:
  kind='sword' if name=='starlight_sword' else 'pickaxe'
  item=json.loads((root/f'assets/minecraft/models/item/{tier}_{kind}.json').read_text())
  assert item['overrides'][0]['model']==f'sunlit_journal_core:item/{name}'
print('PASS: both sculpted models, valid vanilla bounds/UV/rotations/textures, 7 display poses and all 12 item mappings.')
