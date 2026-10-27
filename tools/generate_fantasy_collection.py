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
 def __init__(self):self.v=[];self.faces=[];self.normals=[];self.uvs=[];self.groups=[];self.group="body";self.material_library="materials.mtl"
 def face(self,points,mat,normals=None,uvs=None):
  ids=[]
  if uvs is None:
   aa,bb,cc=points[:3];u=[bb[k]-aa[k] for k in range(3)];v=[cc[k]-aa[k] for k in range(3)]
   n=[abs(u[1]*v[2]-u[2]*v[1]),abs(u[2]*v[0]-u[0]*v[2]),abs(u[0]*v[1]-u[1]*v[0])]
   axis=max(range(3),key=lambda k:n[k])
   # Uniform model-space pixel density. OBJ UVs stay inside the sprite, never wrap into neighbours.
   uvs=[((p[2]+16)/32,1-p[1]/32) if axis==0 else ((p[0]+16)/32,(p[2]+16)/32) if axis==1 else ((p[0]+16)/32,1-p[1]/32) for p in points]
  for i,p in enumerate(points):
   self.v.append(p);self.normals.append(normals[i] if normals else None);self.uvs.append(tuple(max(.003,min(.997,n)) for n in uvs[i]));ids.append(len(self.v))
  self.faces.append((ids,mat));self.groups.append(self.group)
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
    uv=[(i/seg,1-j/rings),((i+1)/seg,1-j/rings),((i+1)/seg,1-(j+1)/rings),(i/seg,1-(j+1)/rings)]
    if j==0:uv=[uv[0],uv[2],uv[3]]
    if j==rings-1:uv=[uv[0],uv[1],(i/seg,0)]
    pts=list(reversed(pts));uv=list(reversed(uv));ns=[tuple((p[k]-c[k])/(r[k]*r[k]) for k in range(3)) for p in pts]
    self.face(pts,mat,ns,uv if mat in ['gem','accent','pearl','shine'] else None)
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
  lines=['# Original Sunlit sculpted equipment; coordinates in model units','mtllib '+self.material_library]
  lines += [f'v {(.5+x/24):.5f} {(y/24):.5f} {(.5+z/24):.5f}' for x,y,z in self.v]
  lines += [f'vt {u:.6f} {v:.6f}' for u,v in self.uvs]
  # Explicit smooth normals for round gems and cloud/bunny forms.
  for ids,mat in self.faces:
   aa,bb,cc=[self.v[n-1] for n in ids[:3]];u=[bb[k]-aa[k] for k in range(3)];v=[cc[k]-aa[k] for k in range(3)]
   normal=(u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0])
   for n in ids:
    if self.normals[n-1] is None:self.normals[n-1]=normal
  for n in self.normals:
   length=sqrt(sum(x*x for x in n)) or 1;lines.append('vn '+' '.join(f'{x/length:.5f}' for x in n))
  last=None;last_group=None
  for face_index,(ids,mat) in enumerate(self.faces):
   group=self.groups[face_index]
   if group!=last_group:lines.append('g '+group);last_group=group
   if last!=mat:lines.append('usemtl '+mat);last=mat
   # Ear clipping preserves concave tool outlines instead of drawing overlapping fan triangles.
   for tri in self.triangulate(ids):lines.append('f '+' '.join(f'{n}/{n}/{n}' for n in tri))
  path.write_text('\n'.join(lines)+'\n')
from fantasy_sculpture import sculpt, PALETTES

catalog=[]
for ti,(theme,name,collection,colors,rarity,weight) in enumerate(THEMES):
 folder=OUT/theme;folder.mkdir(exist_ok=True)
 mats=dict(zip(['main','secondary','metal','gem'],colors));mats['dark']='#251E36'
 main,light,shade,grip,wrap,accent=PALETTES[theme]
 def tint(hex_color,factor):return '#'+''.join(f'{min(255,int(int(hex_color[k:k+2],16)*factor)):02x}' for k in [1,3,5])
 mats.update(main=main,light=light,shade=shade,edge=shade,grip=grip,grip_light=tint(grip,1.35),grip_shade=tint(grip,.65),wrap=wrap,accent=accent,shine='#FFFFFF',pearl='#FFEDE5')
 # Painted sprites supply the colour; neutral Kd avoids double-tinting.
 from fantasy_pixel_materials import write_materials
 material_text,texture_sizes=write_materials(theme,mats,A)
 (folder/'materials.mtl').write_text(material_text)
 if theme=='sakura':
  from reference_sakura import paint,sculpt_reference
  (folder/'reference_materials.mtl').write_text(paint(A))
 for ki,(kind,ko) in enumerate(KINDS):
  m=Mesh()
  if theme=='sakura' and kind in ['sword','pickaxe']:sculpt_reference(m,kind)
  else:sculpt(m,theme,kind)
  m.save(folder/f'{kind}.obj')
  sid=f'{theme}_{kind}';index=ti*5+ki+1
  model={'loader':'forge:obj','model':f'sunlit_journal_core:models/fantasy/{theme}/{kind}.obj','automatic_culling':False,'shade_quads':True,'flip_v':False,'gui_light':'front','textures':{'particle':f'sunlit_journal_core:item/fantasy_{theme}'},'display':{
   'gui':{'rotation':[18,-28,-35],'translation':[0,-1,0],'scale':[.72,.72,.72]},
   'firstperson_righthand':{'rotation':[0,-70,20],'translation':[1,1,0],'scale':[.78,.78,.78]},
   'firstperson_lefthand':{'rotation':[0,70,-20],'translation':[1,1,0],'scale':[.78,.78,.78]},
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
