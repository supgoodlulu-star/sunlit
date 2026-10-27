"""Theme-specific volumetric equipment. Each body is sculpted, not a shared flat cutout.
All sweeps are closed, bevelled solids with deliberately different front/ridge/edge materials.
Coordinates are design units; Mesh.save handles Minecraft model units.
"""
from math import sin,cos,pi,sqrt

# Primary, light, shadow, grip, secondary grip and contrasting enamel.
PALETTES={
 'sakura':['#F46DA7','#FFD4EA','#A32E64','#632C48','#CA6A8C','#57C8BE'],
 'moon_bunny':['#B39CF4','#F1E8FF','#61509E','#363D78','#929BD5','#79DFD5'],
 'star_cloud':['#63C9EB','#E4FCFF','#23658C','#213E68','#599ABC','#FFDC68'],
 'fairy_forest':['#4FBA66','#C9EF88','#246447','#563D2D','#9E7350','#F6CE60'],
 'deep_pearl':['#4BCAC6','#D0F7F1','#256D87','#294B69','#6AB2BD','#FF8F9D'],
 'snow_blossom':['#7CB6F2','#DEFCFF','#3F5EAF','#303C72','#748CCC','#D09AF0'],
 'black_rose':['#8D325E','#E6A3BB','#352435','#231D2B','#573645','#EE6F95'],
 'abyss_sovereign':['#60549D','#B698EC','#292A49','#202B3E','#556079','#59C8D8'],
 'blood_eclipse':['#C94153','#FF9F90','#642338','#302435','#744054','#FFBB62'],
 'sun_blessing':['#F3B544','#FFF1A7','#A7662A','#654027','#B77B38','#42C6BD'],
 'sea_stars':['#436DDD','#A6D5FF','#293566','#172D58','#5265A6','#75EFE8'],
 'heaven_relic':['#E6E9F3','#FFFFFF','#8AABC7','#384E81','#9BB9D3','#78C9C1'],
}

def normalize(v):
 l=sqrt(sum(n*n for n in v)) or 1
 return tuple(n/l for n in v)
def cross(a,b):return (a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0])
def plus(a,b):return tuple(x+y for x,y in zip(a,b))

def sweep(m,sections,mat='main',facets=8,faceted=True):
 """sections=(x,y,z,half_width,half_depth), tangent-aligned elliptical rings.
 Closed 3D sweep, even on curves/branches. Wide blades have a ridged diamond/octagon section.
 """
 rings=[];radials=[]
 for i,(x,y,z,w,d) in enumerate(sections):
  nxt=sections[min(len(sections)-1,i+1)];prev=sections[max(0,i-1)]
  t=normalize(tuple(nxt[k]-prev[k] for k in range(3)))
  u=normalize(cross(t,(0,0,1)))
  if sum(n*n for n in u)<.1:u=normalize(cross(t,(0,1,0)))
  v=cross(t,u);center=(x,y,z)
  ring=[];norm=[]
  for j in range(facets):
   a=2*pi*j/facets;delta=tuple(u[k]*w*cos(a)+v[k]*d*sin(a) for k in range(3))
   ring.append(plus(center,delta));norm.append(normalize(tuple(u[k]*cos(a)/max(w,.01)+v[k]*sin(a)/max(d,.01) for k in range(3))))
  rings.append(ring);radials.append(norm)
 for i in range(len(rings)-1):
  for j in range(facets):
   q=(j+1)%facets
   # Different face values make side planes readable even under Minecraft GUI lighting.
   face_mat=mat
   if mat=='grip':face_mat=['grip_shade','grip','grip_light','grip_light','grip','grip_shade','grip_shade','grip'][j%8]
   if faceted and mat=='main':face_mat=['edge','shade','main','light','light','main','shade','edge'][j%8]
   points=[rings[i][j],rings[i][q],rings[i+1][q],rings[i+1][j]]
   normals=None if faceted else [radials[i][j],radials[i][q],radials[i+1][q],radials[i+1][j]]
   m.face(points,face_mat,normals)
 m.face(list(reversed(rings[0])),'edge' if mat=='main' else mat)
 m.face(rings[-1],'light' if mat=='main' else mat)

def rod(m,pts,r,mat='metal',facets=10):
 sweep(m,[(x,y,z,r,r) for x,y,z in pts],mat,facets,False)

def solid(m,pts,z,thickness,mat='main'):
 """Four visible contour rings and a raised face. Real thick side walls, bevel and crown."""
 if sum(pts[i][0]*pts[(i+1)%len(pts)][1]-pts[(i+1)%len(pts)][0]*pts[i][1] for i in range(len(pts)))<0:pts=pts[::-1]
 cx=sum(p[0] for p in pts)/len(pts);cy=sum(p[1] for p in pts)/len(pts)
 for sign in [-1,1]:
  rings=[]
  for scale,d in [(1,0),(.94,.55),(.78,.9),(.57,1)]:rings.append([(cx+(x-cx)*scale,cy+(y-cy)*scale,z+sign*thickness*d/2) for x,y in pts])
  for step in range(3):
   for j in range(len(pts)):
    q=(j+1)%len(pts);points=[rings[step][j],rings[step][q],rings[step+1][q],rings[step+1][j]]
    face_mat=('edge' if step==0 else ('light' if pts[j][0]<cx else 'shade') if step==1 else mat) if mat=='main' else mat
    m.face(points if sign==1 else points[::-1],face_mat)
  m.face(rings[-1] if sign==1 else rings[-1][::-1],'light' if mat=='main' else mat)

def arc(m,c,rx,ry,start,end,width,depth,mat='main',steps=18):
 sections=[]
 for i in range(steps+1):
  t=i/steps;a=start+(end-start)*t
  # Rounded tapered tips, broad shoulders; always a volumetric section, not a plate.
  taper=max(.10,sin(pi*t)**.55)
  sections.append((c[0]+rx*cos(a),c[1]+ry*sin(a),c[2],width*taper,depth*(.4+.6*taper)))
 sweep(m,sections,mat)

def gem(m,c,r=1,mat='gem'):
 # Faceted cabochon mounted on a thick socket, highlighted crown.
 x,y,z=c;m.ellipsoid((x,y,z),(r*1.22,r*1.16,r*.62),'metal',10,4)
 m.ellipsoid((x,y,z+r*.38),(r,r,r*.78),mat,8,4)
 m.ellipsoid((x-r*.24,y+r*.23,z+r*.98),(r*.18,r*.2,r*.08),'shine',6,3)

def petal(m,c,size,angle,mat='main'):
 x,y,z=c
 sections=[]
 for i in range(9):
  t=i/8;l=t*size*2
  sections.append((x+sin(angle)*l,y+cos(angle)*l,z+sin(t*pi)*size*.30,max(.06,sin(pi*t)**.7*size*.58),max(.09,sin(pi*t)*size*.34)))
 sweep(m,sections,mat)

def bloom(m,c,size,rose=False):
 x,y,z=c
 if rose:
  for layer in range(3):
   for i in range(5):
    a=i*2*pi/5+layer*.55;petal(m,(x+sin(a)*size*.16,y+cos(a)*size*.16,z+layer*.24),size*(.6-layer*.12),a,'shade' if layer==0 else 'main')
 else:
  for i in range(5):petal(m,(x,y,z),size*.65,i*2*pi/5)
 gem(m,(x,y,z+.3),size*.28,'accent')

def leaf(m,a,b,width,mat='main'):
 x,y,z=a;dx=b[0]-x;dy=b[1]-y;dz=b[2]-z
 sections=[]
 for i in range(9):
  t=i/8;sections.append((x+dx*t,y+dy*t,z+dz*t+sin(pi*t)*.2,max(.04,width*sin(pi*t)**.7),max(.08,width*.38*sin(pi*t))))
 sweep(m,sections,mat)

def wrap(m,base,top,radius,mat='wrap',turns=4,phase=0):
 points=[]
 for i in range(turns*18+1):
  t=i/(turns*18);a=t*turns*2*pi+phase
  points.append((radius*cos(a),base+(top-base)*t,radius*sin(a)))
 rod(m,points,.16,mat,6)

def ribbon(m,c,side=1,mat='main'):
 # Thicker folded ribbon solid with front/back/walls, never an open two-sided plane.
 x,y,z=c;pts=[]
 for i in range(8):
  t=i/7;pts.append((x+side*(sin(t*5)*.7+t*1.6),y-t*4,z+sin(t*4)*.5,.42*(1-.3*t),.18))
 sweep(m,pts,mat)

def handle(m,theme,kind):
 top=8.6 if kind=='sword' else {'sakura':15.8,'moon_bunny':14.7,'star_cloud':14.5,'fairy_forest':16.5,'deep_pearl':15,'snow_blossom':14.8,'black_rose':16.1,'abyss_sovereign':13.8,'blood_eclipse':15,'sun_blessing':14.5,'sea_stars':15.2,'heaven_relic':15.8}[theme]
 if kind=='sword' and theme=='abyss_sovereign':top=7
 if theme=='sakura':
  sweep(m,[(0,1,0,.95,.85),(-.28,3,0,1.02,.88),(-.2,top*.6,.18,.82,.7),(0,top,0,1,.9)],'grip',10)
  wrap(m,1.7,top-.7,1.02,'wrap',4)
  m.ellipsoid((0,.9,0),(1.2,.75,1),'metal');gem(m,(0,.9,.8),.65)
 elif theme=='moon_bunny':
  sweep(m,[(0,1,0,.6,.7),(.15,2,0,1.35,1),(.4,top*.6,.15,1.2,.94),(0,top,0,.82,.75)],'grip',12,False)
  for y in [2.7,4.4,6.1]:arc(m,(0,y,0),1.2,1.2,0,2*pi,.16,.16,'wrap',14)
  m.ellipsoid((0,1,0),(1.05,1.05,1.05),'secondary');gem(m,(0,1,1),.48,'accent')
 elif theme=='star_cloud':
  sweep(m,[(0,1,0,.75,.85),(0,3,0,1.15,1),(0,top-2,0,.9,.8),(0,top,0,1.5,1.15)],'grip',8)
  for y in [2,top-1]:m.ellipsoid((0,y,0),(1.3,.7,1.1),'metal')
  for i in range(4):m.ellipsoid((sin(i*pi/2)*.6,1,cos(i*pi/2)*.6),(.7,.65,.7),'secondary')
 elif theme=='fairy_forest':
  points=[(sin(i*.5)*.65,1+i*(top-1)/10,sin(i*.7)*.18,.85 if i not in [2,6] else 1.1,.8) for i in range(11)]
  sweep(m,points,'grip',9,False)
  rod(m,[(-.8,top*.4,0),(-2,top*.5,0),(-2.3,top*.65,.2)],.42,'wrap')
  leaf(m,(-1.4,top*.5,.5),(-3.4,top*.65,.8),.7)
  rod(m,[(sin(i*.65)*.95,2+i*(top-3)/16,cos(i*.65)*.9) for i in range(17)],.18,'accent')
 elif theme=='deep_pearl':
  sweep(m,[(0,1,0,.8,.8),(.7,top*.35,.2,1.05,.85),(-.5,top*.75,.15,.9,.85),(0,top,0,1.25,1)],'grip',10,False)
  wrap(m,2.5,top-1,1.05,'wrap',3)
  for i in range(3):m.ellipsoid(((-1)**i*.45,1+i*.5,.5),(.66,.66,.66),'secondary')
 elif theme=='snow_blossom':
  sweep(m,[(0,1,0,.45,.45),(0,3,0,1.2,.95),(.2,top-2,0,.9,.75),(0,top,0,1.4,1.1)],'grip',6)
  for y in [3,7,11]:
   if y<top:solid(m,[(-1,y-1.1),(1,y-1),(1.2,y+.3),(0,y+1.4),(-1.2,y+.3)],0,2.1,'main')
  gem(m,(0,1,.5),.85,'accent')
 elif theme=='black_rose':
  for side in [-1,1]:rod(m,[(side*.45*cos(i*.6),1+i*(top-1)/18,side*.65*sin(i*.6)) for i in range(19)],.63,'grip')
  for i in range(3):
   y=3+i*3.5
   if y<top:leaf(m,(.5,y,.3),(1.8,y+1,.8),.35,'shade')
  wrap(m,2,top-1,.8,'accent',2);bloom(m,(0,1,.8),.9,True)
 elif theme=='abyss_sovereign':
  sweep(m,[(0,1,0,.8,.7),(0,2,0,1.3,1.15),(0,top-2,0,1.15,1),(0,top,0,1.9,1.3)],'grip',6)
  for y in [2.6,top-2]:solid(m,[(-1.4,y-.5),(1.4,y-.5),(1.6,y+.5),(0,y+1.1),(-1.6,y+.5)],0,2.8,'shade')
  gem(m,(0,top*.5,1),.65,'accent');solid(m,[(-1,1),(0,-.2),(1,1),(0,2)],0,2,'main')
 elif theme=='blood_eclipse':
  sweep(m,[(0,1,0,.6,.6),(-.4,3,0,1.1,.9),(.4,top*.7,0,.86,.8),(0,top,0,1.2,1)],'grip',8)
  wrap(m,2,top-1,1,'wrap',3)
  arc(m,(0,1,0),1.5,1.5,-pi*.8,pi*.8,.4,.45,'metal');gem(m,(0,1,.75),.65)
 elif theme=='sun_blessing':
  sweep(m,[(0,1,0,.75,.75),(0,2.2,0,1.5,1.25),(0,top-2,0,1.1,1),(0,top,0,1.5,1.2)],'grip',8)
  for y in [2.5,5.5,8.5,11.5]:
   if y<top:rod(m,[(-1,y,.9),(0,y+.6,1.2),(1,y,.9)],.22,'metal')
  m.ellipsoid((0,1,0),(1.15,1.05,1),'main');gem(m,(0,1,.8),.6,'accent')
 elif theme=='sea_stars':
  sweep(m,[(0,1,0,.65,.65),(.35,top*.3,0,1.05,.9),(-.35,top*.65,0,.95,.8),(0,top,0,1.2,.9)],'grip',8)
  for i in range(3):
   y=2+i*(top-4)/3;gem(m,((i%2-.5)*.6,y,.85),.4,'accent')
  arc(m,(0,1,0),1.45,.5,0,2*pi,.18,.16,'metal');m.ellipsoid((0,1,0),(.85,.85,.85),'gem')
 else:
  sweep(m,[(0,1,0,.8,.8),(0,3,0,1.1,.9),(0,top-1,0,.85,.8),(0,top,0,1.5,1.05)],'grip',10)
  for y in [2,top-1]:m.ellipsoid((0,y,0),(1.2,.6,1.05),'metal')
  wrap(m,3,top-2,1,'secondary',3)
  gem(m,(0,1,.8),.7,'accent')
 return top

def blade(m,points,mat='main'):
 sweep(m,[(x,y,z,w,d) for x,y,z,w,d in points],mat)

def heads_sakura(m,kind,t):
 if kind=='sword':
  blade(m,[(0,t,0,1.3,.9),(.5,13,.15,1.9,.95),(1.1,19,.4,2.3,.9),(1.8,24,.5,1.3,.65),(1.9,27,.4,.05,.09)])
  rod(m,[(-3,t+.5,0),(-1,t-.4,0),(1,t+.2,0),(3.6,t+1.7,.1)],.5,'metal')
 elif kind=='pickaxe':
  for side in [-1,1]:blade(m,[(0,t+1,0,1.9,1.3),(side*2.5,t+3,.25,1.8,1.2),(side*5.3,t+3.2,.15,1.3,.9),(side*7.7,t+.6,0,.08,.12)])
 elif kind=='axe':
  solid(m,[(-.8,t),(-3,t-1),(-6,t+.6),(-7,t+3),(-5.4,t+7),(-3.6,t+4.7),(-.7,t+4)],0,3.5)
  petal(m,(.3,t+1.4,0),1.8,.75)
 elif kind=='shovel':
  blade(m,[(0,t-1,0,.9,.8),(-.3,t+1,.4,2.5,1.2),(-.9,t+3.2,.8,3.5,1.6),(-.5,t+5.8,.6,2.7,1.2),(.2,t+7,.2,.08,.12)])
 else:
  blade(m,[(0,t+.5,0,1.8,1.2),(2.7,t+2,.2,1.5,1.1),(5.8,t+1.5,.3,1.35,.95),(7.5,t-.9,0,.12,.16)])
  petal(m,(-.6,t+.6,0),1.3,-.7)
 m.group="ornaments"
 bloom(m,(0,t+1.2,1.5),1.5);bloom(m,(2,t+2.4,1.1),.75)
 ribbon(m,(-1,t,.7),-1);ribbon(m,(.8,t-.2,.8),1)

def heads_moon(m,kind,t):
 if kind=='sword':
  arc(m,(-.3,t+8.2,0),4.5,8.4,-pi*.5,pi*.48,1.7,1.45)
  gem(m,(3.7,t+8,1.1),.6,'accent')
  arc(m,(0,t+.5,.1),3.6,2.4,-pi*.9,-pi*.1,.65,.7,'secondary')
 elif kind=='pickaxe':
  arc(m,(0,t+1,0),7.3,5.2,pi*.08,pi*.96,1.4,1.45)
  blade(m,[(0,t,0,1,1.1),(0,t+3.3,0,1.2,1.2)])
 elif kind=='axe':
  arc(m,(-.3,t+3,0),5.9,4.8,pi*.34,pi*1.55,2,1.5)
 elif kind=='shovel':
  blade(m,[(0,t-1,0,.8,.9),(0,t+1,0,2.2,1.4),(0,t+4,0,3.1,1.75),(0,t+6.4,0,2.2,1.4),(0,t+7.7,0,.1,.15)])
  arc(m,(0,t+3,1),2,2,-pi*.75,pi*.75,.42,.5,'secondary')
 else:
  arc(m,(.4,t+.7,0),6,3.2,pi*.08,pi*.7,1.5,1.3)
  blade(m,[(0,t,0,1.1,1.1),(.8,t+3.4,0,1,1)])
 m.group="ornaments"
 m.ellipsoid((0,t+.8,1.5),(1.5,1.25,1.05),'secondary',12,7)
 for x in [-.7,.7]:
  m.ellipsoid((x,t+2.55,1.2),(.44,1.28,.55),'secondary');m.ellipsoid((x,t+2.6,1.64),(.22,.83,.22),'main')
 for x in [-.48,.48]:m.ellipsoid((x,t+.95,2.5),(.11,.16,.09),'dark',8,4)
 m.ellipsoid((0,t+.55,2.57),(.13,.10,.08),'accent',8,4)

def cloud(m,x,y,z,size):
 for dx,dy,r in [(-.7,0,.75),(0,.35,.95),(.8,0,.7),(0,-.2,.8)]:m.ellipsoid((x+dx*size,y+dy*size,z),(r*size,r*.8*size,r*.85*size),'secondary',10,6)
def star(m,c,r):
 x,y,z=c
 for i in range(5):
  a=pi/2+i*2*pi/5;blade(m,[(x,y,z,r*.38,r*.44),(x+cos(a)*r*.6,y+sin(a)*r*.6,z,r*.29,r*.33),(x+cos(a)*r,y+sin(a)*r,z,.06,.08)])
 gem(m,c,r*.32)
def heads_cloud(m,kind,t):
 if kind=='sword':
  solid(m,[(-1.3,t),(1.3,t),(2.8,14),(3.4,22),(1.5,25),(0,24.2),(-1.5,25),(-3.4,22),(-2.8,14)],0,3.8)
  star(m,(0,20,2),1.3)
 elif kind=='pickaxe':
  for side in [-1,1]:
   blade(m,[(side*.3,t+2,0,2,1.7),(side*4.7,t+2,0,2,1.6),(side*7.4,t-1.2,0,.08,.15)])
   cloud(m,side*2.6,t+2,.4,.9)
 elif kind=='axe':
  solid(m,[(-.6,t),(-4,t-1),(-6.5,t+2),(-4.2,t+6),(-.6,t+4)],0,4)
  star(m,(-3,t+2.5,2),1.7)
 elif kind=='shovel':
  blade(m,[(0,t,0,.9,1),(0,t+2,0,2.7,1.8),(0,t+4.5,0,3.4,1.8),(0,t+7,0,.1,.15)])
 else:
  blade(m,[(0,t,0,1.5,1.4),(3,t+2.2,0,2.2,1.7),(6.7,t+1.7,0,1.4,1.1),(7.5,t-.2,0,.1,.2)])
 m.group="ornaments"
 cloud(m,0,t+.8,1,1.2);star(m,(0,t+1,2.1),1.6)

def heads_forest(m,kind,t):
 if kind=='sword':
  leaf(m,(0,t,0),(1,26,.1),2.7);rod(m,[(0,t,1),(.2,15,1.2),(.7,22,1),(1,25,.4)],.18,'accent')
  leaf(m,(0,t,0),(-3.8,t+3,.1),1,'secondary')
 elif kind=='pickaxe':
  rod(m,[(-7,t-.4,0),(-5,t+2,0),(-1,t+3,0),(3,t+3,0),(6,t+5,.2)],.8,'grip')
  leaf(m,(-2,t+3,.1),(-8,t+2,.1),1.45);leaf(m,(2,t+3,.1),(7.5,t+6,.1),1.6)
 elif kind=='axe':
  leaf(m,(-.5,t,0),(-6,t+5,.1),3.3);leaf(m,(0,t+1,0),(3.3,t+5,.4),1.4)
 elif kind=='shovel':
  leaf(m,(0,t,0),(-.9,t+8,.2),3.4);rod(m,[(0,t,1),(-.4,t+3,1.5),(-.8,t+6,1)],.18,'accent')
 else:
  rod(m,[(0,t-1,0),(0,t+2,0),(3,t+4,0),(6,t+3,0)],.85,'grip')
  leaf(m,(2,t+3,0),(7,t+1,.3),1.9)
 m.group="ornaments"
 leaf(m,(0,t-.8,1),(-2.4,t+1,1.3),.9);gem(m,(0,t+1,1.4),.9)

def shell(m,c,r=3.5):
 x,y,z=c
 # A convex shell bowl, with arched ribs following its crown instead of thin radial cards.
 sweep(m,[(x,y,z,.45,.6),(x,y+r*.3,z,r*.6,r*.30),(x,y+r*.70,z,r*.84,r*.34),(x,y+r*.98,z,r*.62,r*.20),(x,y+r*1.08,z,.12,.15)])
 for i in range(7):
  a=-1.05+i*.35
  rod(m,[(x,y,z+.65),(x+sin(a)*r*.35,y+cos(a)*r*.52,z+r*.38),(x+sin(a)*r*.72,y+cos(a)*r*.94,z+r*.19)],max(.12,r*.045),'secondary' if i%2 else 'metal',8)
 gem(m,(x,y+.8,z+1.2),1.05,'pearl')
def heads_pearl(m,kind,t):
 if kind=='sword':
  blade(m,[(0,t,0,.85,.85),(.8,14,.2,.8,.9),(-.5,20,.3,.65,.9),(.8,27,.1,.07,.1)])
  shell(m,(-.4,19.3,.1),3.2);shell(m,(0,t,0),2.6)
 elif kind=='pickaxe':
  for side in [-1,1]:arc(m,(side*2.4,t+1,0),4.1,3.3,.05 if side>0 else pi*.05,pi*.95 if side>0 else pi*1.95,1.25,1.4)
  shell(m,(0,t+.8,.5),2.8)
 elif kind=='axe':
  shell(m,(-2,t+.5,0),5.5)
 elif kind=='shovel':shell(m,(0,t-.3,0),6.2)
 else:
  arc(m,(2,t+1,0),5.3,2.5,.05,pi*.8,1.6,1.3);shell(m,(0,t,0),2.2)
 m.group="ornaments"
 for side in [-1,1]:rod(m,[(side*.8,t+.2,1),(side*2,t+2,1),(side*2.4,t+4,1)],.24,'accent')
 gem(m,(0,t+.8,1.4),1.15,'pearl')

def crystal(m,a,b,width):
 leaf(m,a,b,width)
 # A narrow bright interior shaft makes crystal depth visible on both sides.
 mid=tuple((a[i]+b[i])/2 for i in range(3));gem(m,(mid[0],mid[1],mid[2]+width*.35),width*.27,'accent')
def heads_snow(m,kind,t):
 if kind=='sword':
  crystal(m,(0,t,0),(0,27,0),1.65)
  crystal(m,(0,14,0),(-2.5,24,.1),1.1);crystal(m,(0,13,0),(2.2,21,.1),1)
  for side in [-1,1]:crystal(m,(side*.6,t,0),(side*3.7,t+4,0),1)
 elif kind=='pickaxe':
  for side in [-1,1]:
   crystal(m,(0,t+1,0),(side*5.5,t+4,0),1.7)
   crystal(m,(side*5,t+2,0),(side*8,t-1,0),1.2)
  crystal(m,(0,t,0),(0,t+5,0),1.2)
 elif kind=='axe':
  for x,dy,w in [(-5,4,2),(-4,7,1.8),(-2,8,1.3),(2,5,1.4)]:crystal(m,(0,t,0),(x,t+dy,.1),w)
 elif kind=='shovel':
  for x,dy,w in [(-2,5,1.5),(0,8,2.1),(2,5,1.5)]:crystal(m,(0,t,0),(x,t+dy,0),w)
 else:
  crystal(m,(0,t,0),(1,t+4,0),1.6);crystal(m,(0,t+3,0),(5.7,t+3,0),1.4);crystal(m,(5,t+3,0),(7,t,.1),1)
 m.group="ornaments"
 solid(m,[(-1.8,t-1),(-2,t+1),(0,t+2),(2,t+1),(1.8,t-1)],0,3,'grip');gem(m,(0,t+.6,1.7),.9,'accent')

def heads_rose(m,kind,t):
 if kind=='sword':
  blade(m,[(0,t,0,1.1,1),(-.5,14,0,1.4,1.1),(-2.1,20,0,1.7,1.3),(-1.6,24,0,1.3,1),(1,26,0,.06,.1)])
  for y in [15,19]:leaf(m,(-1,y,0),(-3.2,y+1.4,0),.65,'shade')
  for x in [-1,1]:leaf(m,(x,t+1,0),(x*3.6,t+4,0),.8,'shade')
 elif kind=='pickaxe':
  arc(m,(-2.1,t+.7,0),5.3,4.3,pi*.03,pi*1.1,1.2,1.25)
  rod(m,[(0,t+1,0),(2.7,t+2.1,0),(3.8,t+3.5,0)],.7,'grip')
  leaf(m,(2,t+1.5,0),(4.6,t+.1,0),.85,'shade')
 elif kind=='axe':
  arc(m,(-1,t+2,0),5.2,5,pi*.4,pi*1.6,1.8,1.3)
  leaf(m,(0,t+1,0),(3.2,t+5,0),1.15,'shade')
 elif kind=='shovel':
  blade(m,[(0,t,0,.9,.9),(0,t+2,0,2.7,1.7),(-.4,t+5,0,2.25,1.5),(-.8,t+7,0,.08,.1)])
 else:
  arc(m,(1,t,0),6,4,.05,pi*.75,1.25,1.2)
  leaf(m,(0,t,0),(-2.5,t+2,0),.9,'shade')
 m.group="ornaments"
 bloom(m,(0,t+1,1.5),1.65,True);ribbon(m,(-1,t,.9),-1,'accent')

def heads_abyss(m,kind,t):
 if kind=='sword':
  solid(m,[(-1.8,t),(1.8,t),(3.4,13),(3.4,23),(1.25,26),(.65,23.5),(-.65,23.5),(-1.25,26),(-3.4,23),(-3.4,13)],0,4.5)
  blade(m,[(0,11,2.1, .33,.2),(0,21,2.1,.33,.2)],'accent')
  for side in [-1,1]:solid(m,[(side*.8,t),(side*4,t+2),(side*4.4,t+4),(side*2,t+2.7)],0,3)
 elif kind=='pickaxe':
  for side in [-1,1]:blade(m,[(0,t+1,0,2.2,2),(side*3,t+2,0,2.6,2.1),(side*6,t+1.4,0,2.1,1.7),(side*7,t-2,0,.1,.2)])
 elif kind=='axe':
  for side in [-1,1]:solid(m,[(side*.8,t-1),(side*4.2,t-2),(side*6,t+1),(side*6,t+5),(side*3,t+6),(side*.8,t+3)],0,4.5)
 elif kind=='shovel':
  solid(m,[(-1,t),(-3.4,t+2),(-3.1,t+6),(0,t+8),(3.1,t+6),(3.4,t+2),(1,t)],0,4.3)
 else:
  solid(m,[(-1,t),(-1,t+4),(3,t+4.5),(7,t+2.9),(7.4,t-.7),(5.4,t+.3),(2.1,t+1.6)],0,4)
 m.group="ornaments"
 solid(m,[(-1.8,t-1),(-2.2,t+1),(0,t+2),(2.2,t+1),(1.8,t-1)],0,3,'grip');gem(m,(0,t+1,2),1.1,'accent')
 for x in [-1,0,1]:crystal(m,(x,t+1,.1),(x*1.5,t+4.4-abs(x),.1),.45)

def heads_eclipse(m,kind,t):
 if kind=='sword':
  blade(m,[(0,t,0,1,1.05),(0,15,0,1.35,1.2),(0,22,0,.65,.9)])
  arc(m,(0,21,0),3.8,5.2,-pi*.8,pi*.8,1.35,1.5)
  gem(m,(0,20.8,.7),.8,'accent')
 elif kind=='pickaxe':
  arc(m,(-2.4,t+2,0),3.9,4.5,-pi*.2,pi*1.47,1.25,1.5)
  blade(m,[(0,t+2,0,1.5,1.4),(3.8,t+3,0,1.6,1.5),(6.4,t-.9,0,.1,.2)])
 elif kind=='axe':arc(m,(-1,t+2,0),5.8,5.8,pi*.25,pi*1.67,2.1,1.8)
 elif kind=='shovel':
  arc(m,(0,t+3,0),3.6,4.2,pi*.04,pi*1.96,1.0,1.3)
  blade(m,[(0,t,0,.7,.9),(0,t+3,0,1.7,1.7),(0,t+7,0,.08,.12)])
 else:arc(m,(1,t+1,0),6,4.1,0,pi*.84,1.8,1.65)
 m.group="ornaments"
 gem(m,(0,t+1,1.5),1.35);ribbon(m,(1,t,.6),1,'wrap')

def sun(m,c,r):
 x,y,z=c;m.ellipsoid(c,(r,r,r*.6),'main',12,6)
 for i in range(8):
  a=i*pi/4;leaf(m,(x+cos(a)*r*.7,y+sin(a)*r*.7,z),(x+cos(a)*r*1.65,y+sin(a)*r*1.65,z),r*.3,'metal')
 gem(m,(x,y,z+r*.5),r*.58,'gem')
def heads_sun(m,kind,t):
 if kind=='sword':
  solid(m,[(-1.5,t),(1.5,t),(3.3,13.5),(3.3,22.5),(1.7,25.5),(-1.7,25.5),(-3.3,22.5),(-3.3,13.5)],0,4)
  blade(m,[(0,12,2,.35,.18),(0,23,2,.35,.18)],'accent')
  rod(m,[(-4,t+2,0),(-2,t,0),(0,t,0),(2,t,0),(4,t+2,0)],.65,'metal')
 elif kind=='pickaxe':
  sun(m,(0,t+2,0),2.2)
  for side in [-1,1]:blade(m,[(side*1.5,t+2,0,1.7,1.4),(side*4,t+2,0,1.1,1.2),(side*6,t,0,.1,.2)])
 elif kind=='axe':
  arc(m,(-.4,t+2,0),5.3,5.3,pi*.25,pi*1.65,2.2,1.85)
  sun(m,(-.4,t+2,1.8),1.4)
 elif kind=='shovel':
  blade(m,[(0,t,0,1,1.1),(0,t+2,0,3.4,1.8),(0,t+4.5,0,3.2,2),(0,t+7,0,.1,.15)])
 else:
  solid(m,[(-1,t),(-1,t+4),(2,t+4.2),(6.9,t+3),(7.5,t),(5,t+1.3),(1,t+1)],0,3.7)
 m.group="ornaments"
 sun(m,(0,t+1,1.3),1.3)

def heads_stars(m,kind,t):
 if kind=='sword':
  for i in range(3):
   y=t+i*5.2;blade(m,[(0,y,0,1.3 if i==0 else 1.8,1.1),(.5,y+3,0,2,1.4),(0,y+5.1,0,.1,.2)])
  rod(m,[(0,t,0),(0,25,0)],.35,'metal')
 elif kind=='pickaxe':
  star(m,(0,t+2,0),3)
  for side in [-1,1]:blade(m,[(side*1,t+2,0,1.5,1.3),(side*4,t+2.8,0,1.5,1.25),(side*6.6,t+1,0,.07,.12)])
 elif kind=='axe':
  star(m,(-2,t+3,0),4.5);blade(m,[(0,t,0,.8,1),(0,t+6,0,1.4,1),(0,t+8,0,.08,.1)])
 elif kind=='shovel':
  star(m,(0,t+3,0),4.2);blade(m,[(0,t,0,.9,.9),(0,t+3,0,1.6,1.3)])
 else:
  star(m,(.2,t+2,0),2.6);blade(m,[(.6,t+2,0,1.2,1.1),(4,t+2.7,0,1.7,1.4),(7,t+1,0,.1,.12)])
 m.group="ornaments"
 m.ellipsoid((0,t+1,1),(1.35,1.35,1.1),'gem',12,6);arc(m,(0,t+1,1),2.1,.7,0,2*pi,.22,.22,'metal')
 gem(m,(-.2,t+1,2),.55,'accent')

def wing(m,origin,side=1,size=1):
 x,y,z=origin
 for i in range(4):
  a=(x+side*i*.55*size,y+i*.35*size,z-i*.06)
  b=(x+side*(2.5+i*.25)*size,y+(4-i*.35)*size,z)
  leaf(m,a,b,(.7-i*.06)*size,'secondary')
  rod(m,[a,(b[0]*.25+a[0]*.75,b[1]*.25+a[1]*.75,z+.35)],.11*size,'metal')
def heads_heaven(m,kind,t):
 if kind=='sword':
  for side in [-1,1]:
   blade(m,[(side*.55,t,0,.7,.8),(side*1.2,15,0,.9,1),(side*1.5,21,0,.8,.9),(side*.4,27,0,.06,.1)])
   leaf(m,(side,15,.1),(side*3.1,19.5,.1),.8,'secondary')
  for side in [-1,1]:wing(m,(side*.7,t,0),side,1)
 elif kind=='pickaxe':
  for side in [-1,1]:
   wing(m,(side*.4,t+.4,0),side,1.3)
   blade(m,[(side*2,t+3,0,1.35,1.2),(side*5.3,t+3.2,0,1.1,1),(side*7.5,t+.1,0,.07,.1)])
 elif kind=='axe':
  wing(m,(-.5,t,0),-1,1.6);blade(m,[(-4,t+2,0,1.4,1.2),(-5,t+6,0,2,1.4),(-2,t+8,0,.08,.12)])
 elif kind=='shovel':
  for side in [-1,1]:wing(m,(0,t,0),side,1.15)
  blade(m,[(0,t,0,1,1),(0,t+4,0,2,1.5),(0,t+7,0,.1,.1)])
 else:
  wing(m,(0,t,0),1,1.3);blade(m,[(1,t+2,0,1.5,1.4),(5.7,t+4,0,1.3,1.2),(7,t+1.8,0,.08,.12)])
 m.group="ornaments"
 gem(m,(0,t+.8,1.4),1.2,'accent');arc(m,(0,t+4,.3),2.3,.8,0,2*pi,.24,.22,'metal')

BUILDERS={'sakura':heads_sakura,'moon_bunny':heads_moon,'star_cloud':heads_cloud,'fairy_forest':heads_forest,'deep_pearl':heads_pearl,'snow_blossom':heads_snow,'black_rose':heads_rose,'abyss_sovereign':heads_abyss,'blood_eclipse':heads_eclipse,'sun_blessing':heads_sun,'sea_stars':heads_stars,'heaven_relic':heads_heaven}
def sculpt(m,theme,kind):
 m.group="handle";top=handle(m,theme,kind);m.group="head";BUILDERS[theme](m,kind,top)
