"""Two reference-directed prototypes: slim curved layered blades, ivory grips, ribbon brooch.
Separate material library so the other 58 skins and all reward logic remain unchanged.
"""
from math import pi,sin,cos
from PIL import Image
from fantasy_sculpture import sweep,rod,arc,normalize

COLORS={'main':'#ECA1C1','light':'#F6C9DC','shade':'#D78BA8','edge':'#D8A2B1','secondary':'#FFF3E3','metal':'#DDB67F','gold_light':'#FFE3AB','grip':'#F9E9DC','grip_light':'#FFF9F0','grip_shade':'#DFC5BC','wrap':'#EBC2D3','gem':'#ED8FB5','accent':'#F4BDD1','shine':'#FFFFF6','pearl':'#FFF1E5'}

def paint(assets):
 lines=[]
 for mat,h in COLORS.items():
  base=tuple(int(h[i:i+2],16) for i in [1,3,5]);size=64
  image=Image.new('RGB',(size,size));p=image.load()
  for y in range(size):
   for x in range(size):
    # Fine value steps and a restrained pearly reflection, without camouflage blocks.
    band=max(0,1-abs(x-29-y*.08)/11)
    light=.10+band*.14
    if mat in ['grip_shade','shade','edge']:light*=.35
    if mat=='metal':light=.10+max(0,1-abs(x-27-y*.08)/6)*.30
    dark=.04*((x//5+y//9)%3)/2
    if mat in ['gem','accent']:
     light=.10+max(0,1-abs(x+y-55)/20)*.26
     if x>35 and y>35:dark=.10
    color=tuple(round(min(255,v*(1-dark)+(255-v)*light)) for v in base)
    p[x,y]=color
  # Tiny inlays only; keep large surfaces clean and pastel.
  if mat in ['main','light','gem']:
   for x,y in [(29,17),(34,24),(26,27)]:
    p[x,y]=(255,248,238);p[x+1,y]=(255,248,238)
  dest=assets/f'textures/item/fantasy_materials/sakura/reference_{mat}.png';dest.parent.mkdir(parents=True,exist_ok=True);image.save(dest)
  lines+=['newmtl '+mat,'Kd 1.0 1.0 1.0',f'map_Kd sunlit_journal_core:item/fantasy_materials/sakura/reference_{mat}']
 return '\n'.join(lines)+'\n'

def pearl(m,c,r=.28,mat='pearl'):
 m.ellipsoid(c,(r,r,r),mat,10,5)

def curve(m,points,r=.09,mat='metal'):
 rod(m,points,r,mat,6)

def layered_blade(m,sections):
 for factor,depth,mat in [(1,1,'secondary'),(.94,1.045,'metal'),(.91,1.09,'main')]:
  first=len(m.faces)
  sweep(m,[(x,y,z,w*factor,d*depth) for x,y,z,w,d in sections],mat,10,False)
  if mat=='main':
   for i in range(first,len(m.faces)):
    ids,role=m.faces[i]
    if sum((m.normals[n-1] or (0,0,0))[2] for n in ids)/len(ids)<-.5:m.faces[i]=(ids,'shade')

def petal(m,c,size,a,mat='light'):
 x,y,z=c;points=[]
 for i in range(6):
  t=i/5;l=t*size
  points.append((x+sin(a)*l,y+cos(a)*l,z+sin(pi*t)*size*.15,max(.035,sin(pi*t)**.65*size*.42),max(.055,sin(pi*t)*size*.21)))
 sweep(m,points,mat,6,False)

def blossom(m,c,r=2):
 x,y,z=c
 for i in range(5):
  a=i*2*pi/5
  petal(m,(x,y,z),r,a,'secondary')
  petal(m,(x+sin(a)*.12,y+cos(a)*.12,z+.10),r*.88,a,'light')
 # Raised rose-quartz diamond, scaled with the flower; outward front-face winding.
 g=r/1.9
 m.polygon([(x,y+.73*g),(x+.68*g,y),(x,y-.73*g),(x-.68*g,y)],z+.35*g,.3*g,'metal','metal',.10)
 rim=[(x,y+.64*g,z+.58*g),(x+.59*g,y,z+.58*g),(x,y-.64*g,z+.58*g),(x-.59*g,y,z+.58*g)]
 crown=(x-.06*g,y+.09*g,z+.98*g)
 for i,mat in enumerate(['gem','shade','accent','light']):m.face([rim[(i+1)%4],rim[i],crown],mat)
 pearl(m,(x-.16*g,y+.22*g,z+.96*g),.065*g,'shine')

def tail(m,c,side=1,mat='main'):
 x,y,z=c
 points=[]
 for i in range(10):
  t=i/9;points.append((x+side*(.5*sin(5*t)+1.4*t),y-3.7*t,z+.3*sin(4*t),max(.04,.46*(1-.3*t)*(1 if t<.85 else (1-t)/.15)),.07))
 sweep(m,points,mat,6,False)

def bow(m,y):
 for side in [-1,1]:
  path=[(side*(1.6+1.45*cos(a)),y+.75*sin(a),.15) for a in [i*2*pi/18 for i in range(19)]]
  rod(m,path,.23,'metal',6)
  rod(m,[(x,y,z+.12) for x,y,z in path],.15,'wrap',6)
  tail(m,(side*.8,y-.25,.25),side,'main' if side==1 else 'wrap')

def handle(m,top):
 m.group='handle'
 sweep(m,[(0,1.1,0,.58,.58),(0,2.5,0,.67,.62),(0,top-1,0,.60,.58),(0,top,0,.72,.63)],'grip',16,False)
 # Soft crossed silk binding; very thin gold collars.
 for phase in [0,pi]:
  path=[]
  for i in range(37):
   t=i/36;a=t*2*pi*2.6+phase
   path.append((.68*cos(a),1.9+(top-2.3)*t,.65*sin(a)))
  rod(m,path,.075,'wrap',6)
 for y in [1.6,top-.45]:
  for dy in [-.12,.12]:
   path=[(.78*cos(a),y+dy,.70*sin(a)) for a in [i*2*pi/16 for i in range(17)]]
   rod(m,path,.10,'metal',6)
 # Tulip-shaped pommel, not a dark faceted rock.
 for i in range(5):
  a=i*2*pi/5
  m.ellipsoid((.5*cos(a),.95,.5*sin(a)),(.48,.87,.48),'main',8,5)
 m.ellipsoid((0,1.2,0),(.76,.65,.76),'secondary',10,5)
 curve(m,[(0,.6,0),(0,-.35,0),(.12,-.9,0)],.055)
 pearl(m,(.08,-.05,0),.17);pearl(m,(.12,-.7,0),.15)
 petal(m,(.12,-1,0),.85,pi,'main')


def sculpt_reference(m,kind):
 m.material_library='reference_materials.mtl';top=8.6 if kind=='sword' else 16.8
 handle(m,top);m.group='head'
 if kind=='sword':
  # Long asymmetric S-curve with an ivory hairline and a thin gold edge.
  sections=[(0,9.4,0,1.0,.68),(.45,11,0,1.10,.70),(1.0,13,0,1.22,.73),(1.6,15,0,1.26,.74),(2.25,17,0,1.29,.74),(2.85,19,0,1.30,.72),(3.10,21,0,1.25,.70),(3.10,23,0,1.05,.66),(2.94,25,0,.78,.56),(2.7,26.7,0,.42,.34),(2.40,28.4,0,.035,.06)]
  layered_blade(m,sections)
  # Fine gold vine inlay follows the curved pink center and sits on both faces.
  for side in [-1,1]:
   curve(m,[(.1,10,side*.76),(.65,12,side*.78),(1.15,14,side*.80),(1.9,16.2,side*.80),(2.45,18.6,side*.78),(2.7,21,side*.74),(2.6,23.5,side*.66),(2.5,25.8,side*.43)],.055)
   curve(m,[(1.4,15.6,side*.78),(.9,16.6,side*.78),(1.2,17.4,side*.78)],.045)
  blossom(m,(1.65,16,.79),.55)
  # Raised scalloped petal collar on the left, visibly outside the blade outline.
  # The reference's asymmetric silhouette must survive the small GUI preview.
  collar=[(-.65,10.0),(-1.65,11.3),(-1.25,12.2),(-1.15,12.7),(-1.30,13.7),(-.55,14.4),(-.45,15.5),(.25,16.1),(.52,17.0),(1.0,16.7),(.65,14.2),(.1,12.0)]
  m.polygon(collar,0,1.5,'light','metal',.09)
  for face in [-1,1]:
   z=face*.82
   curve(m,[(-.7,10.5,z),(-1.6,11.6,z),(-1.8,12.5,z),(-1.2,13.3,z),(-.35,13.7,z),(.15,14.6,z)],.09)
   # Open gold scroll around the flower's left shoulder.
   curl=[]
   for i in range(29):
    t=i/28;a=-pi*.6+t*pi*1.8;r=1.65*(1-.58*t)
    curl.append((-2.0+r*cos(a),10.7+r*sin(a),z))
   curve(m,curl,.15)
  # A longer silk tail extends to the left rather than hiding behind the grip.
  sweep(m,[(-.8,9.0,1.05,.40,.09),(-1.8,8.2,1.1,.52,.10),(-3.0,7.9,1.0,.54,.09),(-4.0,8.1,.9,.42,.08),(-4.6,8.5,.8,.025,.04)],'main',12,False)
 else:
  # Broad, shallow petal wings; tips stay near the crown instead of hooking down.
  for side in [-1,1]:
   sections=[]
   for i in range(25):
    t=i/24;x=side*(.7+7.7*t);y=19.1+.65*sin(pi*t)-1.6*t*t
    w=.035+1.85*sin(pi*(.12+.88*t))**.75;d=.07+.84*sin(pi*(.10+.90*t))**.65
    sections.append((x,y,0,w,d))
   sections[-1]=(side*8.4,17.5,0,.035,.055)
   layered_blade(m,sections)
   for face in [-1,1]:
    line=[]
    for i in range(21):
     t=i/24
     line.append((side*(.7+7.7*t),19.1+.65*sin(pi*t)-1.6*t*t,face*(.07+.84*sin(pi*(.10+.90*t))**.65)*1.10))
    curve(m,line,.065)
  curve(m,[(0,16.8,0),(0,18.6,0)],.47,'metal')
 m.group='ornaments'
 bow(m,top+.8)
 blossom(m,(0,top+1,.75),1.9)
 # Small flower on the back provides a finished all-round mounting.
 vstart=len(m.v);fstart=len(m.faces);blossom(m,(0,top+1,.75),1.35)
 for i in range(vstart,len(m.v)):
  x,y,z=m.v[i];m.v[i]=(x,y,-z)
  if m.normals[i] is not None:
   nx,ny,nz=m.normals[i];m.normals[i]=(nx,ny,-nz)
 for i in range(fstart,len(m.faces)):
  ids,mat=m.faces[i];m.faces[i]=(ids[::-1],mat)
 # A delicate hanging pearl and blossom petal on the right side.
 curve(m,[(1.4,top+.3,.6),(1.55,top-.7,.6),(1.6,top-1.5,.6)],.045)
 pearl(m,(1.6,top-1.4,.6),.20)
 petal(m,(1.6,top-1.8,.6),.63,pi,'accent')
