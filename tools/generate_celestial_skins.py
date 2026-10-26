"""Original celestial equipment: sculpted vanilla JSON geometry and matching pixel icons."""
from pathlib import Path
from PIL import Image, ImageDraw
import json
ROOT=Path(__file__).resolve().parents[1]
R=ROOT/'src/main/resources/assets/sunlit_journal_core'
COLORS=['#F5F8FF','#CAD5E7','#8E9BB7','#52617F', '#FFF1BB','#E4BC68','#AC793F','#65482E', '#D8FFFF','#85E2F5','#359DBF','#245571', '#D4C9F0','#9C8AC6','#625681','#35334F']
palette=Image.new('RGB',(64,64));d=ImageDraw.Draw(palette)
for i,c in enumerate(COLORS):
 x=(i%4)*16;y=(i//4)*16;d.rectangle((x,y,x+15,y+15),fill=c)
p=R/'textures/item/celestial_palette.png';p.parent.mkdir(parents=True,exist_ok=True);palette.save(p)
def uv(i):
 x=(i%4)*4;y=(i//4)*4;return [x+.5,y+.5,x+3.5,y+3.5]
class Model:
 def __init__(self,name):self.name=name;self.elements=[]
 def box(self,a,b,color,shade=None,angle=0,origin=None):
  faces={side:{'texture':'#palette','uv':uv(color if side in ['north','up','south'] else shade if shade is not None else color)} for side in ['north','south','east','west','up','down']}
  e={'from':a,'to':b,'faces':faces}
  if angle:e['rotation']={'origin':origin or [(a[i]+b[i])/2 for i in range(3)],'axis':'z','angle':angle,'rescale':False}
  self.elements.append(e)
 def gem(self,x,y,z,size=1):self.box([x-size,y-size,z-.4],[x+size,y+size,z+.4],9,10,45,[x,y,z]);self.box([x-.35,y-.35,z+.41],[x+.35,y+.35,z+.5],8)
 def save(self):
  # Vanilla item transforms pivot about (8,8,8), not the world origin.
  for element in self.elements:
   element["from"][2]+=8; element["to"][2]+=8
   if "rotation" in element: element["rotation"]["origin"][2]+=8
  model={'textures':{'palette':'sunlit_journal_core:item/celestial_palette','particle':f'sunlit_journal_core:item/{self.name}'},'gui_light':'front','elements':self.elements,'display':{
   'gui':{'rotation':[12,20,-45],'translation':[0,-1,0],'scale':[.48,.48,.48]},
   'firstperson_righthand':{'rotation':[0,-90,25],'translation':[-1,1,0],'scale':[.44,.44,.44]},
   'firstperson_lefthand':{'rotation':[0,90,-25],'translation':[-1,1,0],'scale':[.44,.44,.44]},
   'thirdperson_righthand':{'rotation':[0,-90,55],'translation':[0,4,.5],'scale':[.52,.52,.52]},
   'thirdperson_lefthand':{'rotation':[0,90,-55],'translation':[0,4,.5],'scale':[.52,.52,.52]},
   'ground':{'translation':[0,2,0],'scale':[.3,.3,.3]},'fixed':{'rotation':[0,0,-45],'scale':[.5,.5,.5]}}}
  p=R/f'models/item/{self.name}.json';p.write_text(json.dumps(model,indent=2)+'\n')
def handle(m,top):
 m.box([7.25,2,-.7],[8.75,top,.7],15,3)
 for y in range(3,top,2):m.box([7.1,y,-.85],[8.9,y+.55,.85],13,14)
 m.box([6.8,top-.7,-.9],[9.2,top+.2,.9],5,6)
 m.box([6.8,1.2,-.9],[9.2,2.5,.9],5,6)
 m.gem(8,1.5,1.05,.9);m.gem(8,1.5,-1.05,.9)
s=Model('starlight_sword');handle(s,9)
# Stepped silver blade, deep steel edge and a raised cyan fuller on both faces.
for lo,hi,w in [(10.4,22.5,1.6),(22.5,26,1.25),(26,28,.8),(28,29,.4)]:
 s.box([8-w-.2,lo,-.8],[8+w+.2,hi,.8],2,3)
 s.box([8-w,lo+.1,-.95],[8+w,hi,.95],0,1)
 for face in [-1,1]:s.box([7.66,lo+.2,face*1.02-.08],[8.34,hi-.15,face*1.02+.08],9,10)
# Swept wing guard with gold ribs, white feathers, and crystal tips.
for side in [-1,1]:
 for i in range(3):
  x=8+side*(2.1+i*1.7);y=9.7+i*.65
  s.box([x-1.15,y-.5,-.8],[x+1.15,y+.5,.8],5,6,side*22.5)
  s.box([x-.8,y+.45,-.6],[x+.8,y+1.8,.6],0,1,side*22.5)
 s.gem(8+side*6,11.6,.9,.65)
s.box([6.3,8.5,-1.15],[9.7,11.5,1.15],5,6,45,[8,10,0]);s.gem(8,10,1.7,1.1);s.gem(8,10,-1.7,1.1)
s.save()
pick=Model('cloud_pickaxe');handle(pick,18)
pick.box([7.4,8,-.85],[8.6,19,.85],1,2)
# Ivory-and-gold crescent: separate faceted segments rather than a flat sprite.
for side in [-1,1]:
 for i in range(4):
  x=8+side*(1.7+i*2.15);y=20.7-i*.5-(max(0,i-1)**2)*.45
  angle=-side*(0 if i==0 else 22.5 if i<3 else 45)
  pick.box([x-1.5,y-.9,-.85],[x+1.5,y+1.2,.85],5,6,angle)
  pick.box([x-1.4,y-.25,-1],[x+1.4,y+1.7,1],0,1,angle)
  for face in [-1,1]:pick.box([x-1.15,y+.25,face*1.08-.08],[x+1.15,y+.85,face*1.08+.08],9,10,angle)
  if i==3:pick.box([x-.7,y-2.1,-.6],[x+.7,y+.2,.6],1,2,angle)
# A cluster of sculpted cloud lobes above the sapphire mounting.
for x,y,w in [(5.8,22,1.4),(8,23,1.8),(10.3,22,1.3)]:pick.box([x-w/2,y-.5,-.65],[x+w/2,y+.9,.65],0,1)
pick.box([6.2,18,-1.2],[9.8,21.7,1.2],5,6,45,[8,20,0]);pick.gem(8,20,1.8,1.3);pick.gem(8,20,-1.8,1.3)
pick.save()
# Matching 64px icon art, used for particles and the exported design sheet.
def icon(name,tool=False):
 im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im)
 d.line((13,55,38,24 if tool else 18),fill=COLORS[15],width=7)
 d.line((13,55,38,24 if tool else 18),fill=COLORS[13],width=3)
 if not tool:
  d.polygon([(24,35),(44,5),(49,2),(49,9),(30,39)],fill=COLORS[0],outline=COLORS[3],width=2)
  d.line((29,33,46,7),fill=COLORS[9],width=2)
  for side in [-1,1]:
   d.polygon([(25,34),(25+side*13,25),(26+side*10,35),(29,38)],fill=COLORS[5],outline=COLORS[6],width=1)
   d.polygon([(25+side*5,32),(25+side*13,21),(25+side*11,31)],fill=COLORS[0],outline=COLORS[2])
  d.polygon([(27,31),(32,36),(27,41),(22,36)],fill=COLORS[9],outline=COLORS[6],width=2)
 else:
  d.polygon([(8,17),(22,8),(39,12),(54,29),(55,39),(45,29),(33,21),(20,19),(8,24)],fill=COLORS[5],outline=COLORS[3],width=2)
  d.line((10,18,22,11,38,15,49,28,53,35),fill=COLORS[0],width=5)
  d.line((15,18,24,14,36,17,44,25),fill=COLORS[9],width=2)
  for x,y in [(20,9),(26,8),(32,10)]:d.rounded_rectangle((x-3,y-2,x+4,y+3),radius=2,fill=COLORS[0])
  d.polygon([(34,18),(40,24),(34,30),(28,24)],fill=COLORS[9],outline=COLORS[6],width=2)
 d.ellipse((9,52,16,59),fill=COLORS[5],outline=COLORS[6]);d.rectangle((11,54,14,57),fill=COLORS[9])
 im.save(R/f'textures/item/{name}.png');return im
sword=icon('starlight_sword');axe=icon('cloud_pickaxe',True)
print(f'Generated 3D skins: {len(s.elements)} sword elements, {len(pick.elements)} pickaxe elements.')
