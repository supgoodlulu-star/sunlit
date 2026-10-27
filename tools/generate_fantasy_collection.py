"""Original curved OBJ equipment meshes for twelve approved Sunlit concept themes.
No source artwork is traced or embedded. Deterministic build inputs for Forge's OBJ loader.
"""
from pathlib import Path
from math import sin,cos,pi,sqrt
import json
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/sunlit_journal_core';D=ROOT/'src/main/resources/data/sunlit_journal_core'
OUT=A/'models/fantasy';OUT.mkdir(parents=True,exist_ok=True);D.mkdir(parents=True,exist_ok=True)
# id, name, collection, primary/secondary/metal/gem, rarity, total theme weight
THEMES=[
('sakura','벚꽃','러블리 판타지',['#F8A9C9','#FFF0E7','#DCAA62','#F76EAC'],'legendary',2),
('moon_bunny','달토끼','러블리 판타지',['#B4A5F1','#FFF4E6','#E8CEA4','#E9C7FF'],'epic',8),
('star_cloud','별구름','러블리 판타지',['#9DCDF5','#FFF9EE','#E6BF68','#77B8F5'],'rare',18),
('fairy_forest','요정의 숲','자연과 정령',['#8DCB80','#EDFFD9','#C9AB68','#49AA85'],'rare',18),
('deep_pearl','심해의 진주','자연과 정령',['#A4E0E6','#FFE4EF','#DCA87E','#C6B8F0'],'rare',18),
('snow_blossom','설화','자연과 정령',['#B5D7FF','#F0F7FF','#B5C7DE','#8DABF1'],'epic',8),
('black_rose','흑장미','다크 판타지',['#604251','#D69AAE','#B78663','#B6457D'],'normal',22),
('abyss_sovereign','심연의 군주','다크 판타지',['#49425F','#A28DC0','#C4A17D','#955DE2'],'epic',8),
('blood_eclipse','피의 월식','다크 판타지',['#77384F','#E7A7AD','#BF8C5D','#C64465'],'legendary',2),
('sun_blessing','태양의 축복','신화와 천상',['#FFE4A4','#FFFFEB','#DBAA49','#FFBE52'],'legendary',2),
('sea_stars','별의 바다','신화와 천상',['#6383D7','#BCA8E7','#DAB46B','#7ACAF3'],'rare',18),
('heaven_relic','천상의 성유물','신화와 천상',['#D3DEEF','#FFFAEC','#DCBF7D','#B8CDF9'],'legendary',2),
]
KINDS=[('sword','검'),('pickaxe','곡괭이'),('axe','도끼'),('shovel','삽'),('hoe','괭이')]
class Mesh:
 def __init__(self):self.v=[];self.faces=[];self.normals=[]
 def face(self,points,mat,normals=None):
  ids=[]
  for i,p in enumerate(points):self.v.append(p);self.normals.append(normals[i] if normals else None);ids.append(len(self.v))
  self.faces.append((ids,mat))
 def ellipsoid(self,c,r,mat,seg=10,rings=6):
  for j in range(rings):
   a=-pi/2+j*pi/rings;b=-pi/2+(j+1)*pi/rings
   for i in range(seg):
    u=i*2*pi/seg;v=(i+1)*2*pi/seg
    f=lambda t,p:(c[0]+r[0]*cos(t)*cos(p),c[1]+r[1]*sin(t),c[2]+r[2]*cos(t)*sin(p))
    # outward winding
    pts=[f(a,u),f(a,v),f(b,v),f(b,u)]
    if j==0:pts=[f(a,u),f(b,v),f(b,u)]
    if j==rings-1:pts=[f(a,u),f(a,v),f(b,u)]
    pts=list(reversed(pts));ns=[tuple((p[k]-c[k])/(r[k]*r[k]) for k in range(3)) for p in pts];self.face(pts,mat,ns)
 def tube(self,pts,r,mat,seg=8):
  rings=[]
  for i,p in enumerate(pts):
   t=pts[min(i+1,len(pts)-1)];q=pts[max(0,i-1)];dx=t[0]-q[0];dy=t[1]-q[1];length=sqrt(dx*dx+dy*dy) or 1
   rings.append([(p[0]+r*cos(a)*dy/length,p[1]-r*cos(a)*dx/length,p[2]+r*sin(a)) for a in [k*2*pi/seg for k in range(seg)]])
  for i in range(len(rings)-1):
   for k in range(seg):self.face([rings[i+1][k],rings[i+1][(k+1)%seg],rings[i][(k+1)%seg],rings[i][k]],mat)
  self.face(rings[0],mat);self.face(list(reversed(rings[-1])),mat)
 def polygon(self,pts,z,depth,mat,edge='metal',bevel=.15):
  # Convex outlines; two nested rims produce actual bevels, not square boxes.
  if sum(pts[i][0]*pts[(i+1)%len(pts)][1]-pts[(i+1)%len(pts)][0]*pts[i][1] for i in range(len(pts)))<0:pts=list(reversed(pts))
  cx=sum(x for x,y in pts)/len(pts);cy=sum(y for x,y in pts)/len(pts)
  inner=[(x*(1-bevel)+cx*bevel,y*(1-bevel)+cy*bevel) for x,y in pts]
  for sign in [-1,1]:
   outer=[(x,y,z+sign*depth*.32) for x,y in pts];rim=[(x,y,z+sign*depth*.5) for x,y in inner]
   self.face(rim if sign==1 else list(reversed(rim)),mat)
   for i in range(len(pts)):
    q=(i+1)%len(pts);v=[outer[i],outer[q],rim[q],rim[i]];self.face(v if sign==1 else list(reversed(v)),edge)
  for i in range(len(pts)):
   q=(i+1)%len(pts);self.face([(pts[q][0],pts[q][1],z-depth*.32),(pts[i][0],pts[i][1],z-depth*.32),(pts[i][0],pts[i][1],z+depth*.32),(pts[q][0],pts[q][1],z+depth*.32)],edge)
 def ring(self,x,y,z,rx,ry,mat,r=.13,start=0,end=2*pi):
  self.tube([(x+rx*cos(start+(end-start)*i/28),y+ry*sin(start+(end-start)*i/28),z) for i in range(29)],r,mat)
 def leaf(self,x,y,z,size,angle,mat='main'):
  outline=[(0,-1),(.55,-.45),(.65,.15),(.35,.65),(0,1),(-.35,.65),(-.65,.15),(-.55,-.45)]
  pts=[(x+size*(a*cos(angle)-b*sin(angle)),y+size*(a*sin(angle)+b*cos(angle))) for a,b in outline]
  self.polygon(pts,z,.38,mat,'metal',.1)
 def flower(self,x,y,z,size,rose=False):
  for i in range(5):
   a=i*2*pi/5;self.ellipsoid((x+cos(a)*size*.62,y+sin(a)*size*.62,z),(size*.5,size*.5,.3),'main',8,4)
  self.ellipsoid((x,y,z+.25),(size*.35,size*.35,.27),'gem' if rose else 'metal',10,5)
 def star(self,x,y,z,size):
  # Convex little diamond rays avoid concave OBJ polygon triangulation.
  for i in range(5):
   a=pi/2+i*2*pi/5;b=a+.5;d=a-.5
   self.polygon([(x,y),(x+cos(d)*size*.4,y+sin(d)*size*.4),(x+cos(a)*size,y+sin(a)*size),(x+cos(b)*size*.4,y+sin(b)*size*.4)],z,.34,'gem','metal',.15)
 def ribbon(self,x,y,z,side=1):
  # A curved waving ribbon, with thickness from a second strip.
  pts=[(x+side*(.2+i*.24+sin(i*.7)*.35),y-i*.48,z+sin(i*.55)*.25) for i in range(9)]
  for i in range(8):
   a=pts[i];b=pts[i+1];w=.3*(1-i*.035)
   for face in [-1,1]:self.face([(a[0]-w,a[1],a[2]+face*.055),(a[0]+w,a[1],a[2]+face*.055),(b[0]+w,b[1],b[2]+face*.055),(b[0]-w,b[1],b[2]+face*.055)][::face],'main')
 def triangulate(self,ids):
  if len(ids)<=4:return [[ids[0],ids[i],ids[i+1]] for i in range(1,len(ids)-1)]
  normal=self.normals[ids[0]-1];axis=max(range(3),key=lambda k:abs(normal[k]));axes=[i for i in range(3) if i!=axis]
  pts={n:tuple(self.v[n-1][k] for k in axes) for n in ids}
  cross=lambda a,b,c:(b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0])
  area=sum(pts[ids[i]][0]*pts[ids[(i+1)%len(ids)]][1]-pts[ids[(i+1)%len(ids)]][0]*pts[ids[i]][1] for i in range(len(ids)))
  sign=1 if area>=0 else -1;remaining=ids[:];out=[]
  while len(remaining)>3:
   found=False
   for i,b in enumerate(remaining):
    a=remaining[i-1];c=remaining[(i+1)%len(remaining)]
    if cross(pts[a],pts[b],pts[c])*sign<=1e-8:continue
    inside=lambda p:all(cross(pts[q],pts[r],pts[p])*sign>=-1e-8 for q,r in [(a,b),(b,c),(c,a)])
    if any(inside(p) for p in remaining if p not in [a,b,c]):continue
    out.append([a,b,c]);remaining.pop(i);found=True;break
   if not found:return out+[[remaining[0],remaining[i],remaining[i+1]] for i in range(1,len(remaining)-1)]
  return out+[remaining]
 def save(self,path):
  lines=['# Original Sunlit sculpted equipment; coordinates in model units','mtllib materials.mtl','vt 0.5 0.5']
  lines += [f'v {(.5+x/24):.5f} {(y/24):.5f} {(.5+z/24):.5f}' for x,y,z in self.v]
  # Explicit smooth normals for round gems and cloud/bunny forms.
  for ids,mat in self.faces:
   aa,bb,cc=[self.v[n-1] for n in ids[:3]];u=[bb[k]-aa[k] for k in range(3)];v=[cc[k]-aa[k] for k in range(3)]
   normal=(u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0])
   for n in ids:
    if self.normals[n-1] is None:self.normals[n-1]=normal
  for n in self.normals:
   length=sqrt(sum(x*x for x in n)) or 1;lines.append('vn '+' '.join(f'{x/length:.5f}' for x in n))
  last=None
  for ids,mat in self.faces:
   if last!=mat:lines.append('usemtl '+mat);last=mat
   # Ear clipping preserves concave tool outlines instead of drawing overlapping fan triangles.
   for tri in self.triangulate(ids):lines.append('f '+' '.join(f'{n}/1/{n}' for n in tri))
  path.write_text('\n'.join(lines)+'\n')
def handle(m,theme,kind):
 top=11 if kind=='sword' else 18
 pts=[(.22*sin(i*.5) if theme in ['fairy_forest','black_rose'] else 0,1+i*(top-1)/12,0) for i in range(13)]
 m.tube(pts,.6,'secondary',12)
 for i in range(7):m.ring(0,2+i*(top-3)/7,0,.62,.17,'main',.12)
 m.ellipsoid((0,1,0),(.78,.6,.65),'metal');m.ellipsoid((0,.9,.55),(.42,.45,.25),'gem')
 m.ring(0,top-.5,0,.7,.22,'metal',.14)
def head(m,kind,theme):
 # Five distinct functional silhouettes, further ornamented with theme-specific shapes.
 if kind=='sword':
  width=1.2 if theme not in ['black_rose','blood_eclipse'] else 1.5
  m.polygon([(-width,11.2),(width,11.2),(width*.9,25),(0,29),(-width*.9,25)],0,.65,'secondary', 'metal',.18)
  m.polygon([(-.28,12),(.28,12),(.34,24),(0,26),(-.34,24)],.4,.12,'main','main',.05)
  m.tube([(-4.4,12.2,0),(-3.4,11.5,0),(-1.5,10.9,0),(0,11.2,0),(1.5,10.9,0),(3.4,11.5,0),(4.4,12.2,0)],.36,'metal')
 elif kind=='pickaxe':
  for side in [-1,1]:
   upper=[(side*(16*t-8*t*t),20+8*t-11*t*t) for t in [i/12 for i in range(13)]]
   lower=[(side*(16*t-8*t*t),18.6+6*t-7.6*t*t) for t in [i/12 for i in range(12,-1,-1)]]
   m.polygon(upper+lower,0,.9,'main','metal',.08)
 elif kind=='axe':
  m.polygon([(-.5,18),(-2.3,17),(-5.5,18.8),(-6.5,22),(-5.2,25.5),(-3.2,23.8),(-1.7,22),(-.5,23)],0,.8,'main','metal',.13)
  m.polygon([(.2,19),(.2,23),(2.9,24),(4.6,22.2),(3.9,20),(2,18)],0,.65,'secondary','metal',.14)
 elif kind=='shovel':
  # Rounded spade, not an axe silhouette.
  m.polygon([(-.7,17),(.7,17),(2.9,19),(3.3,21.3),(2.8,23.6),(1.5,25.1),(0,25.7),(-1.5,25.1),(-2.8,23.6),(-3.3,21.3),(-2.9,19)],0,.6,'main','metal',.13)
  m.tube([(0,17,.4),(0,20,.55),(0,23,.4)],.1,'secondary')
 else:
  m.polygon([(-.7,18),(-.7,21),(1.6,21.8),(4.7,21.1),(7.3,19.4),(6.5,18.5),(3.1,19.3),(1,18.3)],0,.7,'main','metal',.12)
  m.ellipsoid((0,19.5,0),(.9,.9,.65),'metal')
def ornaments(m,theme,kind):
 y=11.1 if kind=='sword' else 20;z=.75
 m.ellipsoid((0,y,z),(.95,1.05,.46),'metal');m.ellipsoid((0,y,z+.3),(.69,.8,.4),'gem')
 if theme=='sakura':
  for x,dy,s in [(0,0,1.15),(-2,1,.65),(2,.4,.72)]:m.flower(x,y+dy,z+.45,s)
  m.ribbon(-.8,y-.5,1,-1);m.ribbon(.8,y-.5,1,1)
 elif theme=='moon_bunny':
  m.ellipsoid((0,y,1.4),(1.1,.96,.67),'secondary')
  for x in [-.52,.52]:
   m.ellipsoid((x,y+1.38,1.1),(.35,1,.32),'secondary');m.ellipsoid((x,y+1.43,1.39),(.17,.68,.13),'main')
  for x in [-.37,.37]:m.ellipsoid((x,y+.1,2.01),(.09,.12,.06),'dark',8,4)
  m.ellipsoid((0,y-.17,2.04),(.1,.08,.06),'gem');m.ring(0,y,0,1.7,1.6,'metal',.15,.1,pi*1.7)
  m.ribbon(1,y-1,.8);m.ellipsoid((-1,y-3,.8),(.62,.62,.62),'secondary')
 elif theme=='star_cloud':
  for side in [-1,1]:
   for i in range(3):m.ellipsoid((side*(1+i*.75),y+.2+(i%2)*.4,.8),(.83,.68,.5),'secondary')
  m.star(0,y,1.6,1.45);m.ribbon(0,y-1,.7)
 elif theme=='fairy_forest':
  for side in [-1,1]:
   m.leaf(side*1.8,y+.7,.9,1.7,-side*.8);m.leaf(side*1.1,y-2,.7,.9,side*.5,'secondary')
  m.flower(0,y,1.5,.72)
  m.tube([(.6*sin(i*.6),3+i*1.8,.7) for i in range(8)],.12,'metal')
 elif theme=='deep_pearl':
  for side in [-1,1]:
   for i in range(3):m.tube([(side*.4,y,.6),(side*(1.3+i*.4),y+1+i*.3,.6),(side*(1.3+i*.6),y+2+i*.4,.6)],.13,'secondary')
  m.ellipsoid((0,y,1.45),(.83,.83,.7),'secondary');m.ellipsoid((1.4,y-3,.8),(.37,.55,.37),'gem')
 elif theme=='snow_blossom':
  for i in range(6):
   a=i*pi/3;dx=cos(a);dy=sin(a)
   m.tube([(0,y,1.5),(dx*2.2,y+dy*2.2,1.5)],.1,'secondary')
   for side in [-1,1]:m.tube([(dx*1.1,y+dy*1.1,1.5),(dx*1.5-side*dy*.5,y+dy*1.5+side*dx*.5,1.5)],.09,'secondary')
  m.ellipsoid((0,y,1.6),(.6,.6,.4),'gem');m.ellipsoid((0,5,0),(.8,.7,.7),'secondary')
 elif theme=='black_rose':
  m.flower(0,y,1.4,1.2,True)
  for a in [0,.8,1.6]:m.leaf(-2+a,y-1,.8,.85,a,'secondary')
  m.ribbon(0,y-1,1,-1)
 elif theme=='abyss_sovereign':
  for side in [-1,1]:
   m.polygon([(side*.7,y),(side*1.3,y+2),(side*3.7,y+2.9),(side*3.1,y+1.1),(side*4,y-.2),(side*2,y+.2)],.7,.3,'main','metal',.08)
  for x in [-.8,0,.8]:m.tube([(x,y+.6,1),(x*1.2,y+2.5-(abs(x)*.6),1)],.13,'metal')
 elif theme=='blood_eclipse':
  m.ring(0,y,1.4,1.6,1.6,'metal',.17);m.ellipsoid((0,y,1.6),(.98,.98,.5),'gem');m.star(1.7,y-1.8,1,.65);m.ribbon(-1,y-1,1,-1)
 elif theme in ['sun_blessing','heaven_relic']:
  for side in [-1,1]:
   for i in range(4):m.leaf(side*(1+i*.65),y+.1+i*.45,.8,1.1,-side*(.6+i*.12),'secondary')
  if theme=='sun_blessing':
   for i in range(10):
    a=i*pi/5;m.tube([(cos(a)*1.1,y+sin(a)*1.1,1.4),(cos(a)*1.6,y+sin(a)*1.6,1.4)],.1,'metal')
  else:m.ring(0,y+2.6,.6,1.5,.5,'metal',.15)
  m.ribbon(.7,y-1,.8)
 elif theme=='sea_stars':
  m.ring(0,y,1,2.1,.8,'metal',.14);m.ellipsoid((0,y,1.4),(.95,.95,.65),'gem')
  for x,dy in [(-2,-1),(1.4,1.7),(1,-2.5)]:m.star(x,y+dy,1,.55)
  m.ribbon(-1,y-1,.7,-1)
 # A pendant and chain physically modelled for every set.
 m.tube([(1,y-1,.75),(1.5,y-2.2,.75),(1.4,y-3.2,.75)],.07,'metal',6)
 m.ellipsoid((1.4,y-3.5,.75),(.3,.5,.25),'gem',8,5)

catalog=[]
for ti,(theme,name,collection,colors,rarity,weight) in enumerate(THEMES):
 folder=OUT/theme;folder.mkdir(exist_ok=True)
 mats=dict(zip(['main','secondary','metal','gem'],colors));mats['dark']='#493747'
 # Standard white sprite tinted by MTL diffuse colors.
 lines=[]
 for key,c in mats.items():
  rgb=[int(c[i:i+2],16)/255 for i in [1,3,5]];lines += ['newmtl '+key,'Kd '+' '.join(f'{n:.4f}' for n in rgb),'map_Kd sunlit_journal_core:item/fantasy_white']
 (folder/'materials.mtl').write_text('\n'.join(lines)+'\n')
 for ki,(kind,ko) in enumerate(KINDS):
  m=Mesh();handle(m,theme,kind);head(m,kind,theme);ornaments(m,theme,kind);m.save(folder/f'{kind}.obj')
  sid=f'{theme}_{kind}';index=ti*5+ki+1
  model={'loader':'forge:obj','model':f'sunlit_journal_core:models/fantasy/{theme}/{kind}.obj','automatic_culling':False,'shade_quads':True,'flip_v':False,'gui_light':'front','textures':{'particle':f'sunlit_journal_core:item/fantasy_{theme}'},'display':{
   'gui':{'rotation':[8,-12,-35],'translation':[0,-1,0],'scale':[.72,.72,.72]},
   'firstperson_righthand':{'rotation':[0,-90,20],'translation':[1,1,0],'scale':[.78,.78,.78]},
   'firstperson_lefthand':{'rotation':[0,90,-20],'translation':[1,1,0],'scale':[.78,.78,.78]},
   'thirdperson_righthand':{'rotation':[0,-90,45],'translation':[0,2,0],'scale':[.8,.8,.8]},
   'thirdperson_lefthand':{'rotation':[0,90,-45],'translation':[0,2,0],'scale':[.8,.8,.8]},
   'ground':{'translation':[0,2,0],'scale':[.5,.5,.5]},'fixed':{'rotation':[0,0,-35],'scale':[.7,.7,.7]}}}
  p=A/f'models/item/fantasy/{sid}.json';p.parent.mkdir(exist_ok=True);p.write_text(json.dumps(model,indent=2)+'\n')
  catalog.append(dict(id=sid,name=f'{name} {ko}',theme=theme,themeName=name,collection=collection,kind=kind,rarity=rarity,weight=weight,index=index,description=f'{name} 테마의 장비 외형입니다. 장비 성능은 바뀌지 않아요.'))
 # New small original particle texture; no edits to generated concept art.
 img=Image.new('RGBA',(32,32));d=ImageDraw.Draw(img);d.ellipse((4,4,27,27),fill=colors[2]);d.ellipse((7,7,24,24),fill=colors[0]);d.ellipse((10,9,17,16),fill=colors[1]);img.save(A/f'textures/item/fantasy_{theme}.png')
Image.new('RGB',(16,16),'white').save(A/'textures/item/fantasy_white.png')
# Original ticket artwork, isolated from concept sheets.
img=Image.new('RGBA',(32,32));d=ImageDraw.Draw(img);d.rounded_rectangle((2,7,29,24),radius=4,fill='#D79AAA',outline='#FFE4A6',width=2);d.rectangle((7,10,24,21),outline='#FFDAED');d.polygon([(16,10),(18,14),(22,15),(18,17),(16,21),(14,17),(10,15),(14,14)],fill='#FFF1BD');img.save(A/'textures/item/fantasy_ticket.png')
fragment=Image.new('RGBA',(32,32));fd=ImageDraw.Draw(fragment);fd.polygon([(4,8),(18,8),(15,13),(22,16),(16,20),(20,24),(4,24)],fill='#D79AAA',outline='#FFE4A6',width=2);fd.polygon([(11,12),(13,16),(16,17),(13,19),(11,22),(9,18),(6,17),(9,15)],fill='#FFF1BD');fragment.save(A/'textures/item/fantasy_ticket_fragment.png')
(A/'models/item/fantasy_ticket_fragment.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'sunlit_journal_core:item/fantasy_ticket_fragment'}},indent=2)+'\n')
(A/'models/item/fantasy_ticket.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'sunlit_journal_core:item/fantasy_ticket'}},indent=2)+'\n')
(D/'fantasy_skins.json').write_text(json.dumps(catalog,ensure_ascii=False,indent=2)+'\n')
for tier in ['wooden','stone','iron','golden','diamond','netherite']:
 for kind,_ in KINDS:
  p=ROOT/f'src/main/resources/assets/minecraft/models/item/{tier}_{kind}.json'
  data=json.loads(p.read_text()) if p.exists() else {'parent':'minecraft:item/handheld','textures':{'layer0':f'minecraft:item/{tier}_{kind}'}}
  data['overrides']=[x for x in data.get('overrides',[]) if 'sunlit_journal_core:fantasy_skin' not in x.get('predicate',{})]
  data['overrides'] += [{'predicate':{'sunlit_journal_core:fantasy_skin':s['index']},'model':f'sunlit_journal_core:item/fantasy/{s["id"]}'} for s in catalog if s['kind']==kind]
  p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,indent=2)+'\n')
print(f'Generated {len(catalog)} original OBJ equipment meshes, 12 material sets and 30 vanilla item bindings.')
