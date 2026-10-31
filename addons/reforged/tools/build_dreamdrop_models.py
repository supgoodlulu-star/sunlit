"""Build original cuboid bottle models, UV atlases and editable Java Blockbench projects.
Inventory cutouts are separate reference-derived assets, not used as flat held geometry.
Requires Pillow. Run from the addon root or any directory.
"""
from pathlib import Path
import math, json, uuid, base64, colorsys, subprocess, tempfile
from PIL import Image, ImageDraw
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/sunlit_reforged'
MODELS=ASSETS/'models/item'; TEX=ASSETS/'textures/item'; BB=ROOT/'art/blockbench'
for p in [MODELS,TEX,BB]:p.mkdir(parents=True,exist_ok=True)

def uid(s):return str(uuid.uuid5(uuid.NAMESPACE_URL,'sunlit-dreamdrop:'+s))
def mix(a,b,t):return tuple(round(x*(1-t)+y*t) for x,y in zip(a,b))
for key,old,palette in [('small','suspicious_cube',((67,157,215),(156,241,255),(100,171,233))),('radiant','artisan_cube',((141,85,196),(246,166,232),(224,140,218))),('brilliant','master_cube',((123,158,224),(255,217,206),(250,194,112)))]:
    dark,light,ribbon=palette; im=Image.new('RGBA',(128,128));d=ImageDraw.Draw(im)
    # The body occupies a continuous UV region: highlights and wave bands follow its surface.
    for y in range(64):
        for x in range(64):
            shade=.30+.44*(y/63)+.14*math.sin(x/17)
            c=mix(dark,light,shade)
            if key=='brilliant':
                rainbow=tuple(round(c*255) for c in colorsys.hsv_to_rgb((.48+y/160+x/350)%1,.27,.98));c=mix(c,rainbow,.65)
            wave=29+7*math.sin(x/14)
            if abs(y-wave)<2 or abs(y-(wave+17))<1:c=mix(c,(255,244,245),.65)
            if 8<=x<=11 and 10<y<45:c=mix(c,(243,253,255),.72)
            if 14<=x<=16 and 5<y<17:c=mix(c,(255,255,255),.82)
            im.putpixel((x,y),c+(255,))
    for x,y in [(24,43),(45,38),(38,15)]:
        d.line((x-2,y,x+2,y),fill=(255,249,206),width=1);d.line((x,y-2,x,y+2),fill=(255,249,206),width=1)
    mats={'glass':(64,0,mix(light,(255,255,255),.4)),'cork':(80,0,(221,192,157)),
          'ribbon':(96,0,ribbon),'gold':(112,0,(242,193,103)),
          'gem':(64,16,light),'shadow':(80,16,dark),'neck':(96,16,mix(ribbon,light,.5)),'ivory':(112,16,(255,236,202))}
    for name,(xx,yy,c) in mats.items():
        for y in range(16):
            for x in range(16):
                t=.12*math.sin(x*.45)+.10*math.cos(y*.4)
                col=mix(c,(255,255,255),max(0,t)) if t>=0 else mix(c,(84,53,73),-t)
                im.putpixel((xx+x,yy+y),col+(255,))
        d.line((xx+2,yy+2,xx+12,yy+2),fill=mix(c,(255,255,255),.5),width=1)
    texture=TEX/('dreamdrop_'+key+'_atlas.png');im.save(texture)
    # Bake a mechanical crop of the reference-derived liquid into the model's UV atlas.
    # Inventory artwork remains untouched; this transfers its glass/liquid detail to geometry.
    with tempfile.TemporaryDirectory() as temp:
        crop=Path(temp)/'liquid.png'
        subprocess.run(['convert',str(ROOT/'art/cutouts'/('dreamdrop_'+key+'.png')),'-crop','650x650+200+450','+repage','-filter','Lanczos','-resize','64x64!','-background','#799dcc','-alpha','remove','-alpha','off',str(crop)],check=True)
        subprocess.run(['convert',str(texture),str(crop),'-geometry','+0+0','-compose','Over','-composite',str(texture)],check=True)
    elements=[]
    def box(name,a,b,mat='glass',rot=None,body=False):
        faces={}
        for side in ['north','south','east','west','up','down']:
            if body and side in ['north','south','east','west']:
                # Project the same liquid art continuously across the faceted body.
                axis=2 if side in ['east','west'] else 0;offset=3 if axis==2 else 2
                uv=[max(0,(a[axis]-offset)/10*8),max(0,(11-b[1])/10*8),min(8,(b[axis]-offset)/10*8),min(8,(11-a[1])/10*8)]
            else:
                xx,yy,_=mats[mat];uv=[xx/8,yy/8,(xx+16)/8,(yy+16)/8]
            faces[side]={'uv':[round(v,4) for v in uv],'texture':'#0'}
        e={'name':name,'from':a,'to':b,'faces':faces}
        if rot:e['rotation']={'origin':[(a[i]+b[i])/2 for i in range(3)],'axis':rot[0],'angle':rot[1],'rescale':False}
        elements.append(e)
    cx=6.8;cz=8
    # Stacked circular cross sections form real volume, not an extruded sprite.
    for layer,r in enumerate([1.9,3.05,3.8,4.15,4.15,3.8,3.05,1.9]):
        y=1.4+layer*1.12;step=1.15;z=-r
        while z<r-.01:
            end=min(z+step,r);near=min(abs(z),abs(end)) if z*end>0 else 0
            half=math.sqrt(max(.05,r*r-near*near))
            box('rounded_body_%d_%d'%(layer,len(elements)),[round(cx-half,3),round(y,3),round(cz+z,3)],[round(cx+half,3),round(y+1.12,3),round(cz+end,3)],'glass',body=True);z=end
    box('bottle_neck',[5.1,10.2,6.3],[8.5,12,9.7],'neck')
    box('ribbon_collar',[4.9,10.5,6.1],[8.7,11.4,9.9],'ribbon')
    box('cork_base',[5,11.7,6.2],[8.6,12.6,9.8],'cork')
    box('cork_top',[5.3,12.6,6.5],[8.3,13.5,9.5],'cork')
    box('bow_left_loop',[8.4,10.8,5.8],[10.6,12.5,6.6],'ribbon',('z',-22.5))
    box('bow_right_loop',[10.3,10.8,5.8],[13.3,13,6.6],'ribbon',('z',22.5))
    box('bow_knot',[9.7,10.5,5.4],[10.8,11.7,6.8],'neck')
    box('bow_tail_right',[11.1,8.2,5.9],[12.5,11.4,6.6],'ribbon',('z',22.5))
    box('bow_tail_left',[9.8,8.5,5.7],[10.8,11,6.4],'ribbon',('z',-22.5))
    box('charm_chain',[9.1,8.8,4],[9.4,10.9,4.3],'gold')
    box('star_charm_frame',[8.5,7.8,3.7],[10.1,9.4,4.3],'gold',('z',45))
    box('star_charm_core',[8.8,8.1,3.5],[9.8,9.1,3.8],'gem',('z',45))
    for x,y in [(9.15,7.3),(9.15,9.3),(8,8.45),(10,8.45)]:box('charm_point_'+str(len(elements)),[x,y,3.8],[x+.4,y+.6,4.2],'gold')
    if key=='brilliant':
        for name,a,b in [('gold_base',[4.6,.9,5.9],[9,1.5,10.1]),('gold_cap',[4.6,11.6,5.9],[9,12.4,10.1]),('gold_cap_top',[5,12.4,6.3],[8.6,13,9.7])]:box(name,a,b,'gold')
        box('crown_crystal',[5.7,12.8,6.9],[7.9,15,9.1],'gem',('y',45))
        for x in [3,10.3]:box('gold_glass_brace_'+str(x),[x,2,6.8],[x+.4,6.8,7.2],'gold',('z',22.5 if x<6 else -22.5))
        box('gold_bow_trim',[11.7,8.3,5.6],[12,11,5.9],'gold',('z',22.5))
    display={'thirdperson_righthand':{'rotation':[0,90,-12],'translation':[0,1.5,-1],'scale':[.65,.65,.65]},
             'thirdperson_lefthand':{'rotation':[0,-90,12],'translation':[0,1.5,-1],'scale':[.65,.65,.65]},
             'firstperson_righthand':{'rotation':[0,-25,12],'translation':[-1,4,0],'scale':[.30,.30,.30]},
             'firstperson_lefthand':{'rotation':[0,25,-12],'translation':[-1,4,0],'scale':[.30,.30,.30]},
             'ground':{'translation':[0,2.5,0],'scale':[.45,.45,.45]},'fixed':{'rotation':[0,180,0],'scale':[.8,.8,.8]},
             'gui':{'rotation':[15,-30,0],'scale':[.85,.85,.85]}}
    held={'credit':'Sunlit Dreamdrop handcrafted volumetric model','ambientocclusion':False,'textures':{'0':'sunlit_reforged:item/dreamdrop_'+key+'_atlas','particle':'sunlit_reforged:item/dreamdrop_'+key+'_atlas'},'elements':elements,'display':display}
    (MODELS/('dreamdrop_'+key+'_held.json')).write_text(json.dumps(held,ensure_ascii=False,indent=2)+'\n')
    flat={'parent':'minecraft:item/generated','textures':{'layer0':'sunlit_reforged:item/dreamdrop_'+key}}
    (MODELS/('dreamdrop_'+key+'_icon.json')).write_text(json.dumps(flat,indent=2)+'\n')
    root={'textures':{'particle':'sunlit_reforged:item/dreamdrop_'+key},'gui_light':'front','loader':'forge:separate_transforms','base':{'parent':'sunlit_reforged:item/dreamdrop_'+key+'_held'},'perspectives':{'gui':{'parent':'sunlit_reforged:item/dreamdrop_'+key+'_icon'}}}
    (MODELS/(old+'.json')).write_text(json.dumps(root,indent=2)+'\n')
    bb_elements=[]
    for i,e in enumerate(elements):
        be={'name':e['name'],'uuid':uid(key+':'+str(i)),'type':'cube','from':e['from'],'to':e['to'],'inflate':0,'shade':True,'box_uv':False,'faces':{k:{'uv':[round(t*8,4) for t in v['uv']],'texture':0} for k,v in e['faces'].items()}}
        if 'rotation' in e:
            r=e['rotation'];be['origin']=r['origin'];be['rotation']=[r['angle'] if a==r['axis'] else 0 for a in ['x','y','z']]
        bb_elements.append(be)
    bb={'meta':{'format_version':'4.10','model_format':'java_block','box_uv':False},'name':'dreamdrop_'+key,'model_identifier':'dreamdrop_'+key,'resolution':{'width':128,'height':128},'elements':bb_elements,'outliner':[e['uuid'] for e in bb_elements],'textures':[{'name':texture.name,'id':'0','uuid':uid(key+':texture'),'width':128,'height':128,'uv_width':128,'uv_height':128,'source':'data:image/png;base64,'+base64.b64encode(texture.read_bytes()).decode()}],'display':display}
    (BB/('dreamdrop_'+key+'.bbmodel')).write_text(json.dumps(bb,ensure_ascii=False,indent=2)+'\n')
    print(key,len(elements),'cuboids')
