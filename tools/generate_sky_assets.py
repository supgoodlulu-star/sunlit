"""Generate original sky relic/tool artwork and vanilla-compatible item model predicates."""
from pathlib import Path
import json
from PIL import Image,ImageDraw
R=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,data):
 p=R/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
def art(name,draw,size=32):
 im=Image.new('RGBA',(size,size));d=ImageDraw.Draw(im);draw(d);p=R/f'assets/sunlit_journal_core/textures/{name}.png';p.parent.mkdir(parents=True,exist_ok=True);im.save(p)
def sword(d):
 d.line((7,26,23,5),fill='#62577E',width=6);d.line((11,21,24,4),fill='#D5DBFF',width=4)
 d.line((14,17,23,5),fill='#FFF5CF',width=2);d.line((6,18,15,25),fill='#B5A0DC',width=3)
 d.line((5,28,10,22),fill='#8F71B1',width=4);d.ellipse((2,26,7,31),fill='#F1D991')
 for x,y in [(23,4),(23,17),(9,8)]:d.line((x-2,y,x+2,y),fill='#FCF2B8');d.line((x,y-2,x,y+2),fill='#FCF2B8')
def pick(d):
 d.line((7,28,23,8),fill='#666A9C',width=5);d.line((8,28,24,8),fill='#ADACDE',width=3)
 d.polygon([(7,7),(15,3),(25,8),(30,17),(23,13),(16,10)],fill='#B6DCF0',outline='#637D9B')
 d.line((9,7,16,5,24,10,28,15),fill='#E8FBFF',width=2)
 for x,y in [(7,7),(15,5),(22,10)]:d.ellipse((x-3,y-2,x+3,y+2),fill='#EEF8FA')
def compass(d):
 d.ellipse((5,5,27,27),fill='#D3C7E8',outline='#655A82',width=2);d.ellipse((8,8,24,24),fill='#294C73')
 d.polygon([(16,8),(19,15),(16,24),(13,17)],fill='#FFEDB1');d.line((16,9,16,23),fill='#D4E7FF',width=2)
 d.ellipse((13,1,19,7),outline='#A99A77',width=2)
art('item/starlight_sword',sword);art('item/cloud_pickaxe',pick);art('item/star_compass',compass)
write(Path('assets/sunlit_journal_core/models/item/star_compass.json'),{'parent':'minecraft:item/generated','textures':{'layer0':'sunlit_journal_core:item/star_compass'}})
for kind,skin in [('sword','starlight_sword'),('pickaxe','cloud_pickaxe')]:
 write(Path(f'assets/sunlit_journal_core/models/item/{skin}.json'),{'parent':'minecraft:item/handheld','textures':{'layer0':f'sunlit_journal_core:item/{skin}'}})
 for tier in ['wooden','stone','iron','golden','diamond','netherite']:
  write(Path(f'assets/minecraft/models/item/{tier}_{kind}.json'),{'parent':'minecraft:item/handheld','textures':{'layer0':f'minecraft:item/{tier}_{kind}'},'overrides':[{'predicate':{'sunlit_journal_core:sky_skin':1},'model':f'sunlit_journal_core:item/{skin}'}]})
colors=['#B9CDE9','#B6D1A2','#DBC5EF','#9BBBDC','#A5C98D','#C9ACDF','#DFD1B2']
for i,name in enumerate(['moon_record','leaf_record','star_record','moon_rune','leaf_rune','star_rune','return_stone']):
 def tile(d,i=i):
  d.rectangle((0,0,31,31),fill='#A8A2AE');d.rectangle((2,2,29,29),fill=colors[i]);d.rectangle((5,5,26,26),outline='#F4EBDC',width=2)
  if i%3==0:d.ellipse((11,9,22,23),fill='#FFF4C9');d.ellipse((15,7,24,20),fill=colors[i])
  elif i%3==1:d.ellipse((10,9,22,20),fill='#547959');d.line((10,24,21,11),fill='#FFF4C9',width=2)
  else:d.polygon([(16,8),(18,13),(24,14),(19,18),(21,24),(16,21),(10,24),(12,18),(8,14),(14,13)],fill='#FFF4C9')
 art('block/'+name,tile)
 write(Path(f'assets/sunlit_journal_core/models/block/{name}.json'),{'parent':'minecraft:block/cube_all','textures':{'all':f'sunlit_journal_core:block/{name}'}})
 write(Path(f'assets/sunlit_journal_core/blockstates/{name}.json'),{'variants':{'':{'model':f'sunlit_journal_core:block/{name}'}}})
write(Path('data/sunlit_journal_core/dimension/sky_garden.json'),{'type':'sunlit_journal_core:sky_garden','generator':{'type':'minecraft:flat','settings':{'biome':'minecraft:the_void','lakes':False,'features':False,'layers':[{'height':1,'block':'minecraft:air'}],'structure_overrides':[]}}})
write(Path('data/sunlit_journal_core/dimension_type/sky_garden.json'),{'ultrawarm':False,'natural':False,'piglin_safe':False,'respawn_anchor_works':False,'bed_works':False,'has_raids':False,'has_skylight':True,'has_ceiling':False,'coordinate_scale':1,'ambient_light':0.5,'min_y':0,'height':256,'logical_height':256,'infiniburn':'#minecraft:infiniburn_overworld','effects':'minecraft:overworld','monster_spawn_block_light_limit':0,'monster_spawn_light_level':0,'fixed_time':18000})
write(Path('data/sunlit_journal_core/recipes/star_compass.json'),{'type':'minecraft:crafting_shaped','pattern':['AFA','FCF','AFA'],'key':{'A':{'item':'minecraft:amethyst_shard'},'F':{'item':'minecraft:feather'},'C':{'item':'minecraft:compass'}},'result':{'item':'sunlit_journal_core:star_compass'}})
for lang in ['ko_kr','en_us']:
 p=R/f'assets/sunlit_journal_core/lang/{lang}.json';data=json.loads(p.read_text());data.update({'item.sunlit_journal_core.star_compass':'별길 나침반' if lang=='ko_kr' else 'Starpath Compass','item.sunlit_journal_core.star_compass.hint':'우클릭: 하늘 정원으로 이동 / 정원에서는 귀환' if lang=='ko_kr' else 'Use: visit the sky garden / return home'})
 for name,label in zip(['moon_record','leaf_record','star_record','moon_rune','leaf_rune','star_rune','return_stone'],['달의 기록석','잎의 기록석','별의 기록석','달의 문양','잎의 문양','별의 문양','별길 귀환석']):data['block.sunlit_journal_core.'+name]=label
 p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
print('Generated sky dimension, recipe, 16 original art/model assets and vanilla skin overrides.')
