"""Mechanical atlas slicing only. Artwork is generated from the approved reference.
No background removal, recolouring or creative pixel edits are performed here.
"""
from pathlib import Path
from PIL import Image
root=Path(__file__).resolve().parents[1]
atlas=Image.open(root/'tools/art-sources/life-and-seasons.png').convert('RGBA')
names=['farming','fishing','mining','cooking','ranching','exploration','life','books','spring','summer','autumn','winter']
out=root/'src/main/resources/assets/sunlit_journal_core/textures/gui/illustrations';out.mkdir(parents=True,exist_ok=True)
for i,name in enumerate(names):
 col,row=i%4,i//4
 # The exported sheet has short gutters between rows. Exclude neighbouring silhouettes.
 top,bottom=[(0,350),(356,684),(690,1024)][row]
 tile=atlas.crop((col*atlas.width//4,top,(col+1)*atlas.width//4,bottom))
 edge=max(tile.size);square=Image.new('RGBA',(edge,edge));square.paste(tile,((edge-tile.width)//2,(edge-tile.height)//2))
 square.resize((256,256),Image.Resampling.NEAREST).save(out/(name+'.png'))
