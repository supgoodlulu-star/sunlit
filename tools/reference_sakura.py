"""Two reference-directed prototypes: slim curved layered blades, ivory grips, ribbon brooch.
Separate material library so the other 58 skins and all reward logic remain unchanged.
"""
from PIL import Image

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
    dark=.09*(x/(size-1))*(.4+y/(size-1))
    if mat in ['gem','accent']:
     light=.10+max(0,1-abs(x+y-55)/20)*.26
     if x>35 and y>35:dark=.10
    color=tuple(round(min(255,v*(1-dark)+(255-v)*light)) for v in base)
    p[x,y]=color
  # Tiny inlays only; keep large surfaces clean and pastel.
  if mat=='gem':
   for x,y in [(29,17),(34,24),(26,27)]:
    p[x,y]=(255,248,238);p[x+1,y]=(255,248,238)
  dest=assets/f'textures/item/fantasy_materials/sakura/reference_{mat}.png';dest.parent.mkdir(parents=True,exist_ok=True);image.save(dest)
  lines+=['newmtl '+mat,'Kd 1.0 1.0 1.0',f'map_Kd sunlit_journal_core:item/fantasy_materials/sakura/reference_{mat}']
 return '\n'.join(lines)+'\n'

# Geometry below is laid out against the supplied B concept's coordinates.
# Only authored contours are used; no pixels from the user's image enter the assets.
from sakura_reference_geometry import sculpt_reference
