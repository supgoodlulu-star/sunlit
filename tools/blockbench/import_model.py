"""Compile trusted geometry/UV data from a Blockbench project; never evaluate notes or Molang.
Hidden surfaces are trimmed only within one opaque, unrotated bone. Different
bones remain intact so animation cannot reveal holes at joints.
"""
import base64,json,math,hashlib,uuid
from pathlib import Path
from collections import defaultdict
from PIL import Image
from io import BytesIO

EPS=2e-5
SIDES={'north':(2,-1),'south':(2,1),'west':(0,-1),'east':(0,1),'down':(1,-1),'up':(1,1)}

def corners(lo,hi,side):
 x,y,z=lo;X,Y,Z=hi
 return {'north':[(X,Y,z),(x,Y,z),(x,y,z),(X,y,z)],'south':[(x,Y,Z),(X,Y,Z),(X,y,Z),(x,y,Z)],'east':[(X,Y,Z),(X,Y,z),(X,y,z),(X,y,Z)],'west':[(x,Y,z),(x,Y,Z),(x,y,Z),(x,y,z)],'up':[(x,Y,z),(X,Y,z),(X,Y,Z),(x,Y,Z)],'down':[(x,y,Z),(X,y,Z),(X,y,z),(x,y,z)]}[side]

def rotate(p,origin,angles):
 x,y,z=[a-b for a,b in zip(p,origin)]
 for axis,deg in enumerate(angles):
  c,s=math.cos(math.radians(deg)),math.sin(math.radians(deg))
  if axis==0:y,z=y*c-z*s,y*s+z*c
  elif axis==1:x,z=x*c+z*s,-x*s+z*c
  else:x,y=x*c-y*s,x*s+y*c
 return [x+origin[0],y+origin[1],z+origin[2]]

def subtract(rect,cut):
 x,y,X,Y=rect;a,b,A,B=cut;a,b,A,B=max(a,x),max(b,y),min(A,X),min(B,Y)
 if A-a<=EPS or B-b<=EPS:return [rect]
 return [r for r in [(x,y,a,Y),(A,y,X,Y),(a,y,A,b),(a,B,A,Y)] if r[2]-r[0]>EPS and r[3]-r[1]>EPS]

def ident(name):return str(uuid.uuid5(uuid.NAMESPACE_URL,'sunlit:fox:'+name))

def adapt(source):
 """Preserve the supplied model; append an optional scroll bag and pet/follow clips."""
 d=json.loads(json.dumps(source));groups={g['name']:g for g in d['groups']}
 body=groups['body'];bid=ident('scroll_bag')
 d['groups'].append({'name':'sunlit_scroll_bag','uuid':bid,'origin':body['origin'],'rotation':[0,0,0],'sunlit_accessory':True})
 cubes=[]
 def box(name,lo,hi,uv):
  uid=ident(name);cubes.append({'name':name,'uuid':uid,'type':'cube','from':lo,'to':hi,'origin':body['origin'],'rotation':[0,0,0],'box_uv':False,'faces':{s:{'uv':uv,'texture':0} for s in SIDES},'visibility':True,'export':True});return uid
 bag=[]
 bag.append(box('scroll_leather_pouch',[5.8,5.6,-1.1],[8.2,9.2,1.4],[1880,90,1884,94]))
 bag.append(box('scroll_leather_lid',[5.7,8.7,-1.2],[8.3,9.5,1.5],[1640,650,1644,654]))
 bag.append(box('scroll_brass_clasp',[6.6,7.5,1.4],[7.3,8.5,1.65],[1640,350,1644,354]))
 for k in range(2):
  x=6.1+k*1.1;bag.append(box('scroll_parchment_'+str(k),[x,8.8,-.9],[x+.65,12,-.2],[1640,90,1644,94]));bag.append(box('scroll_rim_'+str(k),[x-.1,11.7,-1],[x+.75,12.1,-.1],[1640,350,1644,354]))
 bag.append(box('scroll_shoulder_strap',[4.8,8.7,-.7],[6,10.5,-.25],[1640,650,1644,654]))
 def insert(nodes):
  for n in nodes:
   if isinstance(n,dict):
    if n['uuid']==body['uuid']:n['children'].append({'uuid':bid,'isOpen':True,'children':bag});return True
    if insert(n.get('children',[])):return True
  return False
 assert insert(d['outliner'])
 # Explicit closed smiling eyelids, separate from the original glossy eye bones.
 smile_groups=[]
 for side,cx,cy,z in [('left',-1.55,14.95,5.96),('right',4.05,16.5,5.8)]:
  uid=ident('smile_'+side);children=[]
  d['groups'].append({'name':'sunlit_smile_'+side,'uuid':uid,'origin':groups['head']['origin'],'rotation':[0,0,0],'sunlit_petting_only':True,'visibility':False})
  for k in range(6):
   t0,t1=k/6,(k+1)/6;x0,x1=cx-1.1+2.2*t0,cx-1.1+2.2*t1
   y0=cy+.46*math.sin(math.pi*t0)+.25*(x0-cx);y1=cy+.46*math.sin(math.pi*t1)+.25*(x1-cx)
   mid=[(x0+x1)/2,(y0+y1)/2,z];length=math.hypot(x1-x0,y1-y0)
   eid=box('closed_smile_'+side+str(k),[mid[0]-length/2,mid[1]-.075,z-.035],[mid[0]+length/2,mid[1]+.075,z+.035],[1640,650,1644,654]);children.append(eid)
   cubes[-1]['origin']=mid;cubes[-1]['rotation']=[0,0,math.degrees(math.atan2(y1-y0,x1-x0))]
  smile_groups.append({'uuid':uid,'isOpen':False,'children':children})
 def attach_smiles(nodes):
  for n in nodes:
   if isinstance(n,dict):
    if n['uuid']==groups['head']['uuid']:n['children'].extend(smile_groups);return True
    if attach_smiles(n.get('children',[])):return True
  return False
 assert attach_smiles(d['outliner']);d['elements'].extend(cubes)
 def clip(name,length,tracks):
  animators={}
  for bone,channel,keys in tracks:
   g=groups[bone];entry=animators.setdefault(g['uuid'],{'name':bone,'type':'bone','keyframes':[]})
   for t,values in keys:entry['keyframes'].append({'uuid':ident(name+bone+channel+str(t)),'channel':channel,'time':t,'interpolation':'catmullrom','data_points':[dict(zip('xyz',map(str,values)))]})
  return {'name':name,'uuid':ident(name),'loop':'loop','length':length,'animators':animators}
 d['animations'].append(clip('animation.sunlit.follow_hop',.65,[('body','position',[(0,[0,0,0]),(.325,[0,.65,0]),(.65,[0,0,0])]),('body','rotation',[(0,[0,0,-1]),(.325,[2,0,1]),(.65,[0,0,-1])]),('front_leg_left','rotation',[(0,[6,0,0]),(.325,[-6,0,0]),(.65,[6,0,0])]),('front_leg_right','rotation',[(0,[-6,0,0]),(.325,[6,0,0]),(.65,[-6,0,0])])]))
 d['animations'].append(clip('animation.sunlit.pet',1.75,[('head','rotation',[(0,[3,0,-3]),(.85,[5,0,3]),(1.75,[3,0,-3])]),('eye_left','scale',[(0,[1,.045,1]),(1.75,[1,.045,1])]),('eye_right','scale',[(0,[1,.045,1]),(1.75,[1,.045,1])]),('ear_left','rotation',[(0,[0,0,-5]),(.85,[0,0,-9]),(1.75,[0,0,-5])]),('ear_right','rotation',[(0,[0,0,5]),(.85,[0,0,9]),(1.75,[0,0,5])])]))
 d['name']='Sunlit Fox — imported reference';d['sunlit_import']={'source_sha256':hashlib.sha256(json.dumps(source,sort_keys=True).encode()).hexdigest(),'accessory_bone':bid,'notes':'Original geometry, UVs and eight clips retained. Optional scroll bag and two interaction clips appended.'}
 return d

def compile_model(d,optimize=True):
 textures=[];images=[]
 for t in d.get('textures',[]):
  raw=base64.b64decode(t['source'].split(',',1)[1]);im=Image.open(BytesIO(raw)).convert('RGBA');images.append(im)
  textures.append({'width':t.get('uv_width',im.width),'height':t.get('uv_height',im.height),'raw':raw})
 group_data={g['uuid']:g for g in d.get('groups',[])};elements={e['uuid']:e for e in d['elements'] if e.get('visibility',True) and e.get('export',True)}
 bones=[];assigned=set()
 def hierarchy(nodes,parent=-1):
  for n in nodes:
   if isinstance(n,str):
    if n in elements:
     if parent<0:raise ValueError('Ungrouped element unsupported')
     bones[parent]['elements'].append(elements[n]);assigned.add(n)
   else:
    raw=group_data.get(n['uuid'],n);idx=len(bones);bones.append({'name':raw['name'],'uuid':n['uuid'],'parent':parent,'origin':raw.get('origin',[0,0,0]),'rotation':raw.get('rotation',[0,0,0]),'accessory':bool(raw.get('sunlit_accessory')),'pettingOnly':bool(raw.get('sunlit_petting_only')),'elements':[],'quads':[]});hierarchy(n.get('children',[]),idx)
 hierarchy(d['outliner']);assert assigned==set(elements),'Every visible element must be in hierarchy'
 stats={'fully_hidden_faces':0,'input_cubes':len(elements),'input_faces':0,'output_faces':0,'trimmed_or_hidden_faces':0,'split_faces':0}
 def opaque(e):
  for f in e.get('faces',{}).values():
   if f.get('texture') is None:return False
   im=images[int(f['texture'])];u,v,U,V=f.get('uv',[0,0,16,16]);region=im.crop((max(0,math.floor(min(u,U))),max(0,math.floor(min(v,V))),min(im.width,math.ceil(max(u,U))+1),min(im.height,math.ceil(max(v,V))+1)))
   if region.width<1 or region.height<1 or region.getchannel('A').getextrema()[0]<255:return False
  return True
 for bone in bones:
  cubes=bone.pop('elements');grid=defaultdict(set);eligible=[]
  for i,e in enumerate(cubes):
   ok=not any(abs(a)>1e-8 for a in e.get('rotation',[0,0,0]));eligible.append(ok and opaque(e))
   if ok:
    lo,hi=e['from'],e['to']
    for x in range(math.floor(lo[0]-EPS),math.floor(hi[0]+EPS)+1):
     for y in range(math.floor(lo[1]-EPS),math.floor(hi[1]+EPS)+1):
      for z in range(math.floor(lo[2]-EPS),math.floor(hi[2]+EPS)+1):grid[(x,y,z)].add(i)
  for i,e in enumerate(cubes):
   lo,hi=e['from'],e['to'];angles=e.get('rotation',[0,0,0]);origin=e.get('origin',[0,0,0]);rotated=any(abs(a)>1e-8 for a in angles)
   for side,face in e.get('faces',{}).items():
    if face.get('texture') is None:continue
    stats['input_faces']+=1;axis,sign=SIDES[side];axes=[k for k in range(3) if k!=axis];u,v=axes;plane=lo[axis] if sign<0 else hi[axis];rect=[lo[u],lo[v],hi[u],hi[v]];remaining=[rect]
    if optimize and not rotated:
     candidates=set();probe=plane+sign*EPS
     for a in range(math.floor(rect[0]-EPS),math.floor(rect[2]+EPS)+1):
      for b in range(math.floor(rect[1]-EPS),math.floor(rect[3]+EPS)+1):
       p=[0,0,0];p[axis]=math.floor(probe);p[u]=a;p[v]=b;candidates.update(grid.get(tuple(p),()))
     for j in sorted(candidates):
      if i==j or not eligible[j]:continue
      other=cubes[j];ol,oh=other['from'],other['to']
      covered=ol[axis]-EPS*.1<probe<oh[axis]+EPS*.1
      coplanar=j>i and abs((ol[axis] if sign<0 else oh[axis])-plane)<EPS
      if not covered and not coplanar:continue
      cut=[ol[u],ol[v],oh[u],oh[v]];remaining=[r for rect2 in remaining for r in subtract(rect2,cut)]
      if not remaining:break
    if remaining!=[rect]:stats['trimmed_or_hidden_faces']+=1
    if not remaining:stats['fully_hidden_faces']+=1
    # Keep partially hidden quads verbatim: no UV changes and fewer draw calls than fragmented trimming.
    if remaining:remaining=[rect]
    if len(remaining)>1:stats['split_faces']+=len(remaining)-1
    full=corners(lo,hi,side);tex=int(face['texture']);W,H=textures[tex]['width'],textures[tex]['height'];a,b,A,B=face.get('uv',[0,0,16,16]);uv=[(a/W,b/H),(A/W,b/H),(A/W,B/H),(a/W,B/H)];r=int(face.get('rotation',0)//90)%4;uv=uv[r:]+uv[:r]
    edge1=next(k for k in range(3) if abs(full[1][k]-full[0][k])>1e-9);edge2=next(k for k in range(3) if abs(full[3][k]-full[0][k])>1e-9)
    normal=[0,0,0];normal[axis]=sign;normal=rotate(normal,[0,0,0],angles)
    for fragment in remaining:
     points=[];uvs=[]
     for corner in full:
      p=list(corner);p[u]=fragment[0] if abs(corner[u]-lo[u])<EPS else fragment[2];p[v]=fragment[1] if abs(corner[v]-lo[v])<EPS else fragment[3]
      s=(p[edge1]-full[0][edge1])/(full[1][edge1]-full[0][edge1]);t=(p[edge2]-full[0][edge2])/(full[3][edge2]-full[0][edge2])
      uvs.extend([round(uv[0][k]*(1-s)*(1-t)+uv[1][k]*s*(1-t)+uv[2][k]*s*t+uv[3][k]*(1-s)*t,9) for k in range(2)])
      p=rotate(p,origin,angles);points.extend(round(a-b,6) for a,b in zip(p,bone['origin']))
     bone['quads'].append([tex,*points,*uvs,*normal]);stats['output_faces']+=1
 bone_index={b['uuid']:i for i,b in enumerate(bones)};clips={}
 for a in d.get('animations',[]):
  tracks={}
  for uid,animator in a.get('animators',{}).items():
   if uid not in bone_index:continue
   channels=defaultdict(list)
   for k in animator.get('keyframes',[]):
    ch=k['channel'];assert ch in {'rotation','position','scale'}
    assert len(k['data_points'])==1,'Pre/post Molang keyframes are not supported'
    assert k.get('interpolation','linear') in {'linear','catmullrom'},'Unsupported interpolation'
    p=k['data_points'][0];values=[float(p.get(axis,1 if ch=='scale' else 0)) for axis in 'xyz'];assert all(math.isfinite(x) for x in values)
    channels[ch].append([float(k['time']),*values,k.get('interpolation','linear')])
   tracks[str(bone_index[uid])]={ch:sorted(keys,key=lambda k:k[0]) for ch,keys in channels.items()}
  clips[a['name'].split('.')[-1]]={'length':float(a['length']),'loop':a.get('loop')=='loop','tracks':tracks}
 def posed(p,idx):
  while idx>=0:
   bone=bones[idx];p=rotate(p,bone['origin'],bone['rotation']);idx=bone['parent']
  return p
 pts=[];base=[]
 for idx,bone in enumerate(bones):
  for q in bone['quads']:
   for k in range(4):
    p=[q[1+k*3+a]+bone['origin'][a] for a in range(3)];p=posed(p,idx);pts.append(p)
    if not bone['accessory'] and not bone['pettingOnly']:base.append(p)
 lo=[min(p[k] for p in base) for k in range(3)];hi=[max(p[k] for p in base) for k in range(3)];offset=[-(lo[0]+hi[0])/2,-lo[1],-(lo[2]+hi[2])/2]
 def bounds(points):return [[min(p[k] for p in points)+offset[k] for k in range(3)],[max(p[k] for p in points)+offset[k] for k in range(3)]]
 for bone in bones:bone.pop('uuid',None)
 return {'format':3,'kind':'blockbench','scale':.75/(hi[1]-lo[1]),'offset':offset,'bounds':bounds(base),'accessoryBounds':bounds(pts),'textures':[{'path':'textures/pets/fox_bb_'+str(i)+'.png','width':t['width'],'height':t['height']} for i,t in enumerate(textures)],'bones':bones,'clips':clips,'statistics':stats},textures

def export(root=None):
 root=Path(root) if root else Path(__file__).resolve().parents[2]
 source=root/'tools/art-sources/pet-models/fox-source.bbmodel';d=json.loads(source.read_text());adapted=adapt(d)
 editable=root/'tools/art-sources/pet-models/fox-sunlit.bbmodel';editable.write_text(json.dumps(adapted,ensure_ascii=False,separators=(',',':')))
 model,textures=compile_model(adapted)
 assets=root/'src/main/resources/assets/sunlit_journal_core';(assets/'pets/fox.json').write_text(json.dumps(model,separators=(',',':')))
 for i,t in enumerate(textures):(assets/f'textures/pets/fox_bb_{i}.png').write_bytes(t['raw'])
 print(json.dumps(model['statistics'],indent=2));return model

if __name__=='__main__':export()
