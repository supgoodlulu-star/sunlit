"""Extract the authored tier atlas and register inline chat bitmap glyphs.
Only mechanical cropping, padding and nearest-neighbour sizing are applied.
"""
from pathlib import Path
from PIL import Image
import json
root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/sunlit_journal_core'
out=assets/'textures/gui/title_badges';out.mkdir(parents=True,exist_ok=True)
im=Image.open(root/'tools/art-sources/title-tier-badges.png').convert('RGBA')
fields=['farming','fishing','mining','cooking','ranching','wealth']
rows=[(0,280),(280,520),(520,746),(746,1024)]
for row,(top,bottom) in enumerate(rows):
 for col,name in enumerate(fields):
  tile=im.crop((col*im.width//6,top,(col+1)*im.width//6,bottom))
  side=max(tile.size);square=Image.new('RGBA',(side,side));square.paste(tile,((side-tile.width)//2,(side-tile.height)//2))
  square.resize((256,256),Image.Resampling.NEAREST).save(out/(name+'_'+str(row+1)+'.png'))
names=[name+'_'+str(tier) for name in fields for tier in range(1,5)]
for name in ['four_seasons','spring','summer','autumn','winter']:
 im=Image.open(assets/'textures/gui/illustrations'/(name+'.png'));im.save(out/(name+'.png'));names.append(name)
# Pack small glyphs into one bitmap sheet so chat does not need 29 full-size font textures.
chat=Image.new('RGBA',(256,128))
for i,name in enumerate(names):
 tile=Image.open(out/(name+'.png')).resize((32,32),Image.Resampling.NEAREST)
 chat.paste(tile,((i%8)*32,(i//8)*32))
chat_path=assets/'textures/font/title_chat_badges.png';chat_path.parent.mkdir(parents=True,exist_ok=True);chat.save(chat_path)
chars=[chr(0xE200+i) for i in range(len(names))]+['\x00']*(32-len(names))
font=assets/'font/chat_titles.json';font.parent.mkdir(parents=True,exist_ok=True)
font.write_text(json.dumps({'providers':[{'type':'bitmap','file':'sunlit_journal_core:font/title_chat_badges.png','height':9,'ascent':8,'chars':[''.join(chars[i:i+8]) for i in range(0,32,8)]}]},ensure_ascii=False,indent=2)+'\n')
