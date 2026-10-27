"""Original pixel-painted surface maps, created independently of reference/concept images.
Closed colour ramps, deliberate clusters, stepped reflections and material-specific marks.
No photographs, blur, random white noise or external painting files are used.
"""
from pathlib import Path
from PIL import Image, ImageDraw

GEMS={'gem','accent','pearl'}
GRIPS={'grip','grip_light','grip_shade','wrap'}

def rgb(c):return tuple(int(c[k:k+2],16) for k in [1,3,5])
def mix(a,b,t):return tuple(round(x+(y-x)*t) for x,y in zip(a,b))
def ramp(c,theme,material):
 base=rgb(c)
 # Cool shadow hue and warm/cold highlights give coloured lighting, not merely gray shading.
 shadow=(23,20,48) if theme not in ['fairy_forest','sun_blessing'] else (37,27,27)
 light=(225,251,255) if theme in ['snow_blossom','sea_stars','deep_pearl','heaven_relic'] else (255,240,214)
 if material=='metal':light=(255,251,221)
 return [mix(base,shadow,.58),mix(base,shadow,.35),mix(base,shadow,.16),base,mix(base,light,.22),mix(base,light,.48),mix(base,light,.76),light]

def pixel_material(theme,material,color,destination):
 size=16 if material in GEMS else 32 if material in GRIPS or material in {'dark','shine'} else 64
 colors=ramp(color,theme,material);im=Image.new('RGB',(size,size));px=im.load()
 for y in range(size):
  for x in range(size):
   # Broad 2x2 / 3x3 clusters: readable pixels instead of tiny stochastic noise.
   xx=x*64//size;yy=y*64//size
   cluster=((xx//3)*13+(yy//3)*7+(xx//9)*(yy//7))%19
   level=3+(1 if cluster in [1,2,8,9] else -1 if cluster in [0,5,14] else 0)
   if material in GEMS:
    # Flat cut planes plus a deliberately stepped diagonal internal reflection.
    mid=size//2
    if x+y<mid+2:level=6
    elif x>mid and y>mid:level=1
    elif x<mid and y>mid:level=2
    else:level=4
    if abs(x-y-2)<=1:level=min(7,level+2)
    if (x//2+y//2)%5==0:level=min(6,level+1)
   elif material in GRIPS:
    if theme=='fairy_forest':
     grain=(x+(y//5)%3+(y//11))%9
     level=1 if grain in [0,1] else 5 if grain==3 else level
    elif theme in ['snow_blossom','abyss_sovereign']:
     level=5 if x%9 in [2,3] else 1 if x%9 in [0,8] else level
     if y%11 in [0,1]:level=max(0,level-2)
    else:
     thread=(x+2*y)%12
     level=1 if thread in [0,1] else 5 if thread in [3,4] else level
     if (x+2*y)%12==6:level=4
   elif material=='metal':
    stripe=(xx+yy//4)%20
    level=[1,2,2,3,4,5,7,7,6,4,3,2,1,2,3,5,6,4,3,2][stripe]
   elif material=='edge':
    level=[1,1,2,3,4,5,3][(xx+yy//3)%7]
   elif material=='dark':
    level=2+(x//3+y//4)%3
   elif material=='shine':
    level=5+(x//4+y//4)%3
   else:
    # Broad reflection bands and shaded patches run along the carving.
    band=(xx-yy//5)%28
    if band in [7,8]:level=6
    elif band in [9,10,11]:level=5
    elif band in [0,1,24,25,26,27]:level=max(0,level-1)
    if material=='shade':level=max(1,level-1)
    if material=='light' and level<5:level+=1
   px[x,y]=colors[max(0,min(7,level))]
 d=ImageDraw.Draw(im)
 if material in GEMS:
  # Chunky, sharp cut highlights; no soft gradient or fake PBR texture.
  d.rectangle((3,2,5,3),fill=colors[7]);d.point((2,4),fill=colors[6])
  d.line((size-5,size-3,size-3,size-3),fill=colors[0],width=1)
 elif material in {'main','light','shade','secondary'}:
  # Stepped blade glints/engraved pixels at the actual head height in projected UV space.
  for x,y,w in [(28,14,5),(32,21,4),(25,27,5),(36,18,3),(22,23,4),(40,26,3)]:
   d.line((x,y,x+w,y),fill=colors[6],width=1);d.line((x+1,y+1,x+w,y+1),fill=colors[1],width=1)
  if theme in ['sakura','black_rose']:
   for x,y in [(30,17),(37,24),(25,20),(33,29)]:
    d.rectangle((x,y,x+1,y+1),fill=colors[5]);d.point((x-1,y+1),fill=colors[6])
  elif theme in ['snow_blossom','sea_stars','star_cloud']:
   for x,y in [(27,16),(35,23),(23,25),(38,15)]:
    d.line((x-1,y,x+1,y),fill=colors[7]);d.line((x,y-1,x,y+1),fill=colors[6])
  elif theme=='fairy_forest':
   for y in range(10,31,4):
    d.line((29,y,33,y+2),fill=colors[1]);d.line((32,y,35,y-2),fill=colors[5])
  elif theme in ['sun_blessing','heaven_relic','abyss_sovereign','blood_eclipse']:
   for y in [14,20,26]:
    d.line((29,y+2,31,y),fill=colors[1]);d.line((31,y,34,y+2),fill=colors[6])
 elif material=='metal':
  for y in range(5,58,9):d.line((24,y,37,y+2),fill=colors[7],width=1)
 elif material in GRIPS:
  if theme=='fairy_forest':
   d.line((13,4,12,9),fill=colors[0]);d.line((12,9,14,13),fill=colors[1]);d.point((15,16),fill=colors[6])
  elif material=='wrap':
   for y in range(2,size-2,7):d.line((size//2-2,y,size//2+2,y+1),fill=colors[6])
 destination.parent.mkdir(parents=True,exist_ok=True);im.save(destination)
 return size

def write_materials(theme,materials,assets):
 lines=[];details={}
 for material,color in materials.items():
  path=assets/f'textures/item/fantasy_materials/{theme}/{material}.png'
  details[material]=pixel_material(theme,material,color,path)
  lines += ['newmtl '+material,'Kd 1.0 1.0 1.0',f'map_Kd sunlit_journal_core:item/fantasy_materials/{theme}/{material}']
 return '\n'.join(lines)+'\n',details
