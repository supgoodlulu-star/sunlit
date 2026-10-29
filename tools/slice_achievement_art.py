"""Mechanical extraction and nearest-neighbour resize of the two authored illustrations."""
from pathlib import Path
from PIL import Image
root=Path(__file__).resolve().parents[1]
im=Image.open(root/'tools/art-sources/achievement-fairy-wealth.png').convert('RGBA')
out=root/'src/main/resources/assets/sunlit_journal_core/textures/gui/illustrations'
for i,name in enumerate(['four_seasons','wealth']):
 tile=im.crop((i*im.width//2,0,(i+1)*im.width//2,im.height))
 side=max(tile.size);square=Image.new('RGBA',(side,side));square.paste(tile,((side-tile.width)//2,(side-tile.height)//2))
 square.resize((256,256),Image.Resampling.NEAREST).save(out/(name+'.png'))
