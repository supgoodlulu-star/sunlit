import json, math, colorsys
from pathlib import Path
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/sunlit_journal_core'
(root/'pets').mkdir(parents=True,exist_ok=True)
rows=json.loads((Path(__file__).parent/'art-sources/pet_catalog.json').read_text())
# Original voxel geometry, generated separately from the reference/concept images.
colors=['c97838','b99366','b7a193','967958','9c886a','b95530','806046','bca382','8b654d','9f99ad','dba85e','aa7646','8a7869','aa6338','ac8860','929898','c89d67','778ca1','ead079','a9bbc8']
seasoncolors={'SPRING':('e8a5bb','b7d286','f7e7cb'),'SUMMER':('65c5cd','ebbf75','e0f3e9'),'AUTUMN':('c77e43','dcb860','f4dcaf'),'WINTER':('a3cedf','dcecf2','f3f5e9')}
def rgb(h):return int(h,16)
for index,(id,name,acc,season,fantasy) in enumerate(rows):
 v={}; g='body'; base=colors[index] if index<20 else ['82ad68','d67d5b','a37a43','6d9069','df9ba9','a78bcd','9a9c96','dba24d','e3dad0','70c7d5','a29bdb','dfe8e5','a0c6c6','91b2d0','bc9a66','e5dcd2','dd9b59','c5a6c9','ad8555','b4996e'][index-20] if index<40 else seasoncolors[season][0]
 main=rgb(base); pale=rgb('f1dfbf'); dark=rgb('3c302e'); accent=rgb(seasoncolors[season][1]) if season else rgb('78985b'); white=rgb('f7efdb')
 def box(x,y,z,w,h,d,c,group=None):
  for a in range(round(x),round(x+w)):
   for b in range(round(y),round(y+h)):
    for cc in range(round(z),round(z+d)):v[(a,b,cc,group or g)]=c
 def ell(x,y,z,rx,ry,rz,c,group=None):
  for a in range(math.floor(x-rx),math.ceil(x+rx)):
   for b in range(math.floor(y-ry),math.ceil(y+ry)):
    for cc in range(math.floor(z-rz),math.ceil(z+rz)):
     if ((a+.5-x)/rx)**2+((b+.5-y)/ry)**2+((cc+.5-z)/rz)**2<=1:v[(a,b,cc,group or g)]=c
 def eye(x,y,z,slim=False):
  box(x,y,z,2,1 if slim else 2,1,dark,'eyes');box(x,y,z,2,1,1,dark,'smile');box(x-1,y+1,z,1,1,1,dark,'smile');box(x+2,y+1,z,1,1,1,dark,'smile')
 def face(y,z,width=3,slim=False):eye(-width-1,y,z,slim);eye(width-1,y,z,slim)
 def ears(y,z,kind='point'):
  if kind=='round':
   for x in [-4,4]:ell(x,y,z,2.7,2.7,1.7,main,'head');ell(x,y,z-.9,1.5,1.5,1,pale,'head')
  else:
   for x in [-4,3]:
    for k in range(4):box(x-1+k//2,y+k,z,3-k//2,1,2,main,'head')
    box(x,y+1,z-1,1,2,1,pale,'head')
 def paws():
  for x in [-4,2]:
   for z in [-5,3]:box(x,0,z,3,4,3,main,'legs_front' if z<0 else 'legs_back')
 def quadruped(kind):
  low=kind in ['hedgehog','otter','ferret','guinea_pig','hyrax']
  head_y=8 if low else 10
  body_y=4.5 if low else 6
  roundpet=kind in ['capybara','hyrax','red_panda','otter','koala','raccoon','quokka','hamster','guinea_pig','chinchilla','hedgehog']
  ell(0,body_y,0,5,3.5 if low else 4,8 if kind in ['otter','ferret'] else 6,main)
  for x in [-4,2]:
   for z in [-4,3]:box(x,0,z,3,3 if low else 4,3,main,'legs_front' if z<0 else 'legs_back')
  ell(0,head_y,-5,5 if kind not in ['ferret','meerkat'] else 4,3.5 if low else 4.5,4.5,main,'head')
  muzzle=2 if kind in ['cat','red_panda','raccoon','koala','chinchilla','hamster'] else 3
  ell(0,head_y-1.5,-8,3,2,muzzle,main if kind in ['capybara','hyrax'] else pale,'head')
  box(-1,head_y-1,-8-muzzle,2,1,1,dark,'head')
  face(head_y+1,-9,3,kind in ['capybara','hyrax','quokka'])
  if kind!='dog':ears(head_y+3,-5,'round' if roundpet else 'point')
  if kind not in ['capybara','hyrax','koala','guinea_pig','hamster','hedgehog','chinchilla']:
   ell(0,body_y+1,7,2,2,5,main,'tail');ell(0,body_y+2,10,2,2,3,pale,'tail')
  if kind in ['raccoon','red_panda','ferret']:
   for z in [7,10]:box(-2,body_y, z,4,3,1,dark,'tail')
  if kind in ['raccoon','red_panda']:
   box(-5,head_y,-9,10,3,1,rgb('726256') if kind=='raccoon' else white,'head');face(head_y+1,-10)
  if kind=='fox':
   box(-4,6,-9,8,2,1,white,'head');ell(0,8,8,3,3,5,main,'tail');ell(0,8,12,2,2,2,white,'tail')
  if kind=='dog':
   # Spaniel-like dropped ears; not vanilla wolf geometry.
   for x in [-6,4]:box(x,9,-6,2,6,3,rgb('74553e'),'head')
  if kind=='hedgehog':
   for x in range(-4,5,2):
    for z in range(-2,6,2):box(x,7,z,1,3,1,rgb('5e4c42'))
  if kind=='squirrel':ell(0,10,7,3.5,7,3,main,'tail')
  if kind=='chinchilla':
   for x in [-4,4]:ell(x,15,-4,3.5,4,1.5,main,'head');ell(x,15,-5,2,2.5,1,pale,'head')
  if kind=='koala':
   for x in [-5,5]:ell(x,14,-4,3.5,3.5,2,main,'head')
   ell(0,10,-10,2,3,1.5,dark,'head')
  if kind=='hamster':
   for x in [-4,4]:ell(x,8,-8,2.5,2,2,pale,'head')
 def dragon():
  ell(0,6,1,4,5,3.5,main);ell(0,12,-2,6,5,4.5,main,'head');ell(0,10,-6,4.5,2.5,2,main,'head');ell(0,5,-2,2.5,3,1,pale)
  face(13,-7,3);box(-1,10,-8,2,1,1,dark,'head')
  for x in [-4,2]:box(x,0,-1,3,3,4,main,'legs_back')
  for x in [-5,3]:box(x,5,-3,2,3,2,main,'arms')
  for x in [-4,3]:box(x,16,-1,2,3,2,accent,'head')
  for x in [-3,-1,1]:box(x,16-abs(x)//2,-6,2,2,3,white,'head')
  for sign in [-1,1]:
   for k in range(4):box(sign*(5+k),8+k//2,1,1,4-k//2,2,accent,'wings')
  for k in range(5):box(-2+k//3,2+k//2,4+k,3,3,2,main,'tail')
 def fairy():
  ell(0,11,-1,4,4,3,pale,'head');ell(0,13,0,4.5,3,3.5,rgb('e6c779'),'head');box(-4,10,-4,8,3,1,pale,'head');face(11,-5,2,True)
  for y in range(1,8):box(-2-(7-y)//3,y,-2,4+2*((7-y)//3),1,4,main)
  for x in [-4,3]:box(x,5,-1,1,4,2,pale,'arms')
  for sign in [-1,1]:ell(sign*5,8,2,3,4,1,accent,'wings');ell(sign*5,4,2,2,2.5,1,main,'wings')
  box(-2,0,-1,1,2,1,pale,'legs_back');box(1,0,-1,1,2,1,pale,'legs_back')
 def whale():
  ell(0,6,0,7,5,8,main);ell(0,4,-2,6,3,6,white);face(7,-8,4,True)
  for sign in [-1,1]:ell(sign*7,5,0,4,1,3,accent,'wings');ell(sign*3,6,9,4,1.5,3,main,'tail')
 def bird(kind):
  ell(0,5,0,4.5,5,4,main);ell(0,11,-2,4,4,3.5,main,'head');ell(0,5,-3,3,4,1.5,pale);box(-2,9,-6,4,2,3,rgb('d5a451'),'head');face(12,-5,2)
  for x in [-3,1]:box(x,0,-2,3,1,4,rgb('c8954d'),'legs_back')
  for sign in [-1,1]:ell(sign*4,5,0,1.5,4,2,main,'wings')
  if kind=='phoenix':
   for x in [-2,0,2]:ell(x,4,5,1.2,2,6,rgb('eac867'),'tail');box(x,14,-1,1,3,1,rgb('f0cb67'),'head')
 if index<20:
  if id in ['penguin','duck']:bird(id)
  elif id=='meerkat':
   ell(0,7,1,3,6,3,main);ell(0,14,-1,4,3.5,3,main,'head');ell(0,13,-4,2,1.5,2,pale,'head');face(15,-4,2,True)
   for x in [-3,2]:box(x,6,-2,1,4,2,main,'arms');box(x,0,0,2,2,3,main,'legs_back')
   ell(0,2,5,1,1,4,main,'tail');ears(16,-1,'round')
  elif id=='seal':ell(0,4,1,5,3.5,8,main);ell(0,7,-5,4,3,4,main,'head');face(8,-9,2,True);ell(-5,2,0,3,1,3,main,'wings');ell(5,2,0,3,1,3,main,'wings');ell(0,3,9,4,1,2,main,'tail')
  else:quadruped(id)
 elif id.endswith('_dragon') or id=='baby_dragon':dragon()
 elif id.endswith('_fairy'):fairy()
 elif id.endswith('_skywhale'):whale()
 elif id.endswith('_tiger') or id.endswith('_haetae'):
  quadruped('quokka')
  if id.endswith('_tiger'):
   for key in list(v):
    x,y,z,group=key
    if v[key]==main:v[key]=white if (z//2+x//3)%4 else rgb('64798a')
  else:
   for x in [-5,5]:
    for y in [8,11,14]:ell(x,y,-4,2,2,2,accent,'head')
   box(-1,15,-4,2,4,2,accent,'head')
 elif id in ['griffin','unicorn']:
  quadruped('cat' if id=='griffin' else 'quokka')
  if id=='unicorn':box(-1,14,-5,2,4,2,rgb('d3ac64'),'head');ell(0,11,4,2,5,2,pale,'tail')
  else:
   box(-2,8,-12,4,3,4,rgb('d8af58'),'head')
   for sign in [-1,1]:
    for k in range(5):box(sign*(5+k),7+k//2,0,1,5-k//2,3,pale,'wings')

 elif id=='phoenix':bird(id)
 elif id in ['moss_golem','pebble','clockwork','goblin']:
  box(-4,3,-3,8,6,6,main);box(-4,9,-3,8,6,6,main,'head');face(12,-4,2,True)
  for x in [-6,4]:box(x,4,-2,2,6,4,accent,'arms')
  for x in [-3,1]:box(x,0,-2,2,3,4,main,'legs_back')
  if id=='clockwork':box(4,7,2,3,1,4,rgb('d6ba74'));box(6,5,4,1,5,1,rgb('d6ba74'))
 elif id=='book':
  box(-7,1,-4,14,2,8,rgb('79539b'));box(-6,3,-3,5,2,7,pale);box(1,3,-3,5,2,7,pale);box(-1,2,-4,2,4,9,main);face(4,-5,3,True)
 elif id in ['crystal','amber']:
  for k in range(12):box(-max(1,4-abs(k-6)//2),k,-2,max(2,8-abs(k-6)),1,5,main)
  face(7,-3,2,True)
 elif id=='pearl':ell(0,5,0,5,5,5,pale);ell(0,1,0,7,1.5,6,main);face(6,-5,2,True)
 else:
  ell(0,5,0,5,5,4,main);face(6,-4,2,True)
  if id in ['sprout','flowerbud']:
   box(-1,9,0,2,4,2,accent);ell(-3,12,0,3,1.5,2,accent);ell(3,13,0,3,1.5,2,accent)
  if id=='mushroom':ell(0,11,0,7,3,6,main);box(-6,10,-4,12,1,8,white)
  if id=='acorn':ell(0,10,0,6,2,5,rgb('775c3d'));box(-1,11,0,2,3,2,rgb('775c3d'))
  if id=='cloud':ell(-4,6,0,3,3,3,main);ell(4,6,0,3,3,3,main)
  if id=='whirlwind':
   for y in range(10):ell(0,y,0,1+y/3,1,1+y/3,main)
 # Innate seasonal anatomy (not purchased accessories).
 if season:
  c=accent
  # Seasonal details attach to each silhouette rather than floating over whales.
  original_box,original_ell=box,ell
  if id.endswith('_skywhale'):
   def box(x,y,z,w,h,d,col,group=None):return original_box(x,y-6,z,w,h,d,col,group)
   def ell(x,y,z,rx,ry,rz,col,group=None):return original_ell(x,y-6,z,rx,ry,rz,col,group)
  if season=='SPRING':
   for x,y,z in [(-4,14,-2),(4,12,0)]:
    for dx,dy in [(0,0),(-1,0),(1,0),(0,-1),(0,1)]:box(x+dx,y+dy,z,1,1,1,rgb('f7ccd7'),'head')
  elif season=='SUMMER':
   for x in [-3,2]:box(x,15,0,1,3,1,c,'head');box(x-1,16,0,3,1,1,c,'head')
  elif season=='AUTUMN':
   for x in [-4,3]:ell(x,14,0,2,1,2,c,'head')
  else:
   for x in [-3,2]:box(x,14,0,1,4,1,c,'head');box(x-1,16,0,3,1,1,white,'head')
  box,ell=original_box,original_ell
 # Independent species-specific ornament geometry. Unlocks are not baked into bodies.
 g='accessory';gold=rgb('d3ac64');green=rgb('7f9c62');pink=rgb('c894a7');brown=rgb('946b47');cream=rgb('f4e4c4');blue=rgb('78beca');purple=rgb('aa91c2')
 def flower(x,y,z,color):
  for dx,dy in [(-1,0),(1,0),(0,-1),(0,1)]:box(x+dx,y+dy,z,1,1,1,color)
  box(x,y,z,1,1,1,gold)
 def star(x,y,z,color):
  box(x-1,y,z,3,1,1,color);box(x,y-1,z,1,3,1,color);box(x,y,z-1,1,1,2,color)
 def bow(x,y,z,color):
  box(x-2,y-1,z,2,3,1,color);box(x+1,y-1,z,2,3,1,color);box(x,y,z,1,1,1,gold)
 def scarf(color,check=False):
  box(-4,7,-5,8,2,2,color);box(2,4,-6,2,5,1,color)
  if check:
   for x in [-3,0,3]:box(x,7,-6,1,2,1,cream)
 def bag(x,y,z,color,small=False):
  w=4 if small else 6;box(x-w//2,y,z,w,4,3,color);box(x-w//2,y+3,z,w,1,3,gold);box(x,y+1,z-1,1,2,1,gold)
  box(x-w//2,y+4,z+1,1,2,1,brown);box(x+w//2-1,y+4,z+1,1,2,1,brown);box(x-w//2,y+5,z+1,w,1,1,brown)
 def neck(color,y=7):box(-3,y,-6,6,1,1,gold);ell(0,y-2,-7,2,2,1,color)
 def brim(y,color):box(-6,y,-6,12,1,10,color);ell(0,y+2,-1,4,2,3,color)
 def basket(x,y,z,color):
  bag(x,y,z,color)
  for yy in [y+1,y+3]:box(x-3,yy,z-1,6,1,1,gold)
  for xx in [-2,0,2]:box(x+xx,y,z-1,1,4,1,brown)
 if id=='fox':
  bag(4,5,0,brown,True);box(3,9,0,2,4,2,cream);box(3,12,0,2,1,2,gold)
 elif id=='dog':scarf(rgb('bc6850'),True);box(-1,5,-6,3,1,1,cream)
 elif id=='cat':neck(pink);box(0,5,-8,3,1,1,gold);box(2,4,-8,1,3,1,gold)
 elif id=='capybara':ell(0,16,-4,2.5,2.5,2.5,rgb('eca244'));box(0,18,-4,3,1,1,green)
 elif id=='hyrax':
  bag(0,6,3,green)
  for x in [-2,1]:ell(x,11,4,1,3,1,rgb('adc279'))
 elif id=='red_panda':
  bag(0,7,4,rgb('b6bb78'));box(-3,9,3,6,1,1,brown);box(-1,8,7,2,2,1,green)
 elif id=='otter':neck(rgb('ead1ae'),6);box(-2,4,-8,4,1,1,cream)
 elif id=='ferret':bag(4,4,-1,rgb('947853'),True);box(4,6,-2,1,1,1,blue)
 elif id=='hedgehog':brim(12,rgb('c87f5c'));box(-2,14,-3,2,1,1,cream);box(2,14,0,2,1,1,cream)
 elif id=='chinchilla':bow(-5,17,-5,purple)
 elif id=='hamster':bag(4,3,-2,rgb('b79354'),True);ell(4,8,-1,1,2,1,cream)
 elif id=='guinea_pig':box(-3,4,-10,6,4,1,cream);box(0,5,-11,1,3,1,rgb('db9756'));box(-1,8,-11,3,1,1,green)
 elif id=='raccoon':
  ell(0,9,4,4,3,3,green);bow(0,12,3,green)
  for x in [-3,0,3]:box(x,8,1,1,3,1,cream)
 elif id=='squirrel':brim(17,rgb('b18b54'));box(-1,20,-1,2,1,2,brown)
 elif id=='quokka':flower(-5,15,-6,cream)
 elif id=='koala':
  for x in [-4,-2,0,2,4]:ell(x,15,-5,1.5,1,1.5,green)
 elif id=='meerkat':
  box(-4,17,-4,3,2,1,gold);box(1,17,-4,3,2,1,gold);box(-1,18,-4,2,1,1,brown);box(-3,17,-5,1,1,1,blue);box(2,17,-5,1,1,1,blue)
 elif id=='penguin':scarf(rgb('a96262'));box(2,3,-6,2,1,1,cream)
 elif id=='duck':brim(15,rgb('dec48c'));flower(4,17,-3,pink)
 elif id=='seal':star(-3,10,-7,rgb('d7a275'))
 elif id=='sprout':bag(4,3,0,blue,True);box(6,5,-1,3,1,1,blue);box(8,5,-2,1,2,1,blue)
 elif id=='mushroom':scarf(rgb('c98267'),True)
 elif id=='acorn':bow(0,5,-5,green)
 elif id=='moss_golem':
  box(-3,8,3,6,4,3,rgb('bb8966'));ell(0,13,4,3,2,2,green);flower(0,15,4,pink)
 elif id=='flowerbud':neck(pink,6);box(0,3,-8,1,1,1,cream)
 elif id=='crystal':box(-5,0,-4,10,1,9,gold);box(-4,1,-4,1,2,1,gold);box(3,1,-4,1,2,1,gold)
 elif id=='pebble':brim(15,rgb('a3875d'));box(-1,17,-5,2,2,1,gold)
 elif id=='amber':ell(2,6,-3,2,1,1,gold);box(1,5,-4,1,3,1,gold)
 elif id=='pearl':bow(-4,5,-3,rgb('d89292'));box(-5,4,-3,1,4,1,pink)
 elif id=='droplet':box(6,2,0,1,13,1,brown);ell(6,15,0,6,1.5,5,green)
 elif id=='star_slime':
  ell(-2,11,0,3,3,1,gold)
  for key in list(v):
   x,y,z,part=key
   if part=='accessory' and x>=-2 and y>=11:del v[key]
 elif id=='cloud':bag(5,3,0,rgb('a77955'),True);box(5,5,-1,2,1,1,cream)
 elif id=='whirlwind':scarf(blue);box(4,8,-6,1,4,1,brown);star(4,12,-6,pink)
 elif id=='baby_dragon':scarf(rgb('be7b6e'));box(2,4,-7,1,2,1,gold)
 elif id=='griffin':neck(gold,8);box(1,5,-8,1,3,1,cream)
 elif id=='unicorn':star(-4,15,-5,gold)
 elif id=='phoenix':box(-4,1,-3,3,1,4,gold);box(-3,0,-4,1,2,1,gold)
 elif id=='goblin':box(-4,2,4,8,7,1,purple);star(0,7,-5,cream)
 elif id=='book':box(2,4,-5,1,1,8,gold);box(1,4,-7,3,1,2,gold);box(2,4,-8,1,1,1,gold)
 elif id=='clockwork':bag(5,2,0,rgb('806743'),True);box(4,6,1,1,3,1,cream);box(3,8,1,3,1,1,cream)
 elif id.endswith('_fairy'):
  if season=='SPRING':box(6,3,0,1,16,1,gold);ell(6,19,0,6,1.5,5,pink);flower(6,20,0,cream)
  elif season=='SUMMER':ell(5,4,-1,2.5,2,1.5,cream);box(4,6,-1,3,1,1,gold);box(5,3,-3,1,2,1,blue)
  elif season=='AUTUMN':ell(5,4,-1,2,2.5,2,rgb('dbb565'));ell(5,6,-1,2.5,1,2.5,brown);box(5,7,-1,1,2,1,brown)
  else:box(-4,1,3,8,7,1,blue);star(0,7,-4,cream)
 elif id.endswith('_dragon'):
  if season=='SPRING':bag(4,5,0,rgb('a17e58'),True);flower(4,7,-1,pink)
  elif season=='SUMMER':neck(cream,8);box(-2,6,-8,4,1,1,blue)
  elif season=='AUTUMN':basket(0,7,4,rgb('b48951'));ell(0,12,5,2,2,1,rgb('d29850'))
  else:scarf(rgb('adcbd8'));star(3,5,-7,cream)
 elif id.endswith('_haetae'):
  if season=='SPRING':neck(rgb('ba787a'));flower(0,5,-8,pink)
  elif season=='SUMMER':neck(blue);box(-2,3,-7,1,2,1,blue);box(2,3,-7,1,2,1,blue)
  elif season=='AUTUMN':neck(rgb('b86f47'));ell(0,5,-8,2,1,1,gold);box(0,2,-7,1,2,1,brown)
  else:neck(rgb('dce9de'));box(-2,6,-8,4,1,1,blue)
 elif id.endswith('_skywhale'):
  if season=='SPRING':basket(0,-3,-1,rgb('ad8558'));flower(-1,1,-1,pink);flower(2,1,0,cream)
  elif season=='SUMMER':box(0,1,-8,1,5,1,gold);box(-2,1,-8,5,1,1,gold);box(-2,2,-8,1,2,1,gold);box(2,2,-8,1,2,1,gold)
  elif season=='AUTUMN':bag(7,2,0,rgb('a78950'),True);box(7,6,0,1,3,1,gold)
  else:box(0,1,11,1,5,1,gold);star(0,0,11,cream)
 elif id.endswith('_tiger'):
  if season=='SPRING':scarf(rgb('ac8786'));flower(0,7,-6,pink)
  elif season=='SUMMER':box(-5,1,-6,5,1,5,blue);box(-4,2,-7,3,1,1,cream)
  elif season=='AUTUMN':ell(-5,15,-4,2,1,1,rgb('ca915b'));box(-5,14,-5,1,3,1,brown)
  else:
   for x in [-5,5]:ell(x,14,-4,2,2,2,cream)
   box(-5,16,-3,10,1,1,blue)
 # Cull internal faces offline for small meshes and low GUI load.
 faces=[];dirs=[(-1,0,0),(1,0,0),(0,-1,0),(0,1,0),(0,0,-1),(0,0,1)]
 for (x,y,z,group),c in v.items():
  for side,(dx,dy,dz) in enumerate(dirs):
   if (x+dx,y+dy,z+dz,group) not in v:
    # subtle deterministic pixel variation on body surfaces only
    shade=((x*17+y*7+z*13)%13-6)*1.2 if group not in ['eyes','smile'] else 0
    col=[max(0,min(255,round((c>>s)&255)+round(shade))) for s in [16,8,0]]
    faces.append([x,y,z,side,(col[0]<<16)|(col[1]<<8)|col[2],group])
 (root/'pets'/f'{id}.json').write_text(json.dumps({'scale':.032 if id.endswith('_fairy') else .045,'faces':faces},separators=(',',':')))
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
