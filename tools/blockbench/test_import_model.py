import unittest,copy,math,json
from pathlib import Path
from .import_model import compile_model,adapt,subtract

class BlockbenchImportTests(unittest.TestCase):
 def project(self):
  import base64
  from PIL import Image
  from io import BytesIO
  buf=BytesIO();Image.new('RGBA',(16,16),'white').save(buf,format='PNG')
  faces={s:{'uv':[0,0,16,16],'texture':0} for s in ['north','south','east','west','up','down']}
  def cube(id,x):return {'uuid':id,'name':id,'from':[x,0,0],'to':[x+1,1,1],'faces':copy.deepcopy(faces),'rotation':[0,0,0]}
  return {'textures':[{'source':'data:image/png;base64,'+base64.b64encode(buf.getvalue()).decode()}],'groups':[{'uuid':'body','name':'body','origin':[0,0,0],'rotation':[0,180,0]}],'outliner':[{'uuid':'body','children':['a','b']}],'elements':[cube('a',0),cube('b',1)],'animations':[]}
 def test_shared_internal_face_removed_and_uv_unchanged(self):
  p=self.project();raw,_=compile_model(p,False);out,_=compile_model(p,True)
  self.assertEqual(raw['statistics']['output_faces'],12);self.assertEqual(out['statistics']['output_faces'],10)
  original={tuple(q) for b in raw['bones'] for q in b['quads']}
  self.assertTrue(all(tuple(q) in original for b in out['bones'] for q in b['quads']))
 def test_no_culling_between_animated_bones(self):
  p=self.project();p['groups'].append({'uuid':'other','name':'other','origin':[0,0,0]});p['outliner'][0]['children']=['a',{'uuid':'other','children':['b']}]
  out,_=compile_model(p);self.assertEqual(out['statistics']['output_faces'],12)
 def test_rotated_cubes_retained(self):
  p=self.project();p['elements'][1]['rotation']=[0,30,0]
  out,_=compile_model(p);self.assertEqual(out['statistics']['output_faces'],12)
 def test_numeric_animation_only(self):
  p=self.project();p['animations']=[{'name':'unsafe','length':1,'loop':'loop','animators':{'body':{'keyframes':[{'channel':'rotation','time':0,'data_points':[{'x':'__import__("os")','y':'0','z':'0'}]}]}}}]
  with self.assertRaises(ValueError):compile_model(p)
 def test_root_orientation_bounds_and_ground(self):
  out,_=compile_model(self.project());lo,hi=out['bounds'];self.assertAlmostEqual(lo[1],0);self.assertAlmostEqual((hi[1]-lo[1])*out['scale'],.75);self.assertAlmostEqual(lo[0],-hi[0]);self.assertAlmostEqual(lo[2],-hi[2])
 def test_subtraction_retains_exposed_area(self):
  r=subtract([0,0,2,2],[.5,.5,1.5,1.5]);self.assertAlmostEqual(sum((q[2]-q[0])*(q[3]-q[1]) for q in r),3)
 def test_source_unchanged_and_eight_original_clips_retained(self):
  p=Path(__file__).resolve().parents[1]/'art-sources/pet-models/fox-source.bbmodel';source=json.loads(p.read_text());before=copy.deepcopy(source);out=adapt(source)
  self.assertEqual(source,before);self.assertEqual(out['elements'][:len(source['elements'])],source['elements']);self.assertEqual(out['animations'][:8],source['animations']);self.assertEqual(out['textures'],source['textures'])
  self.assertTrue(any(g.get('sunlit_petting_only') for g in out['groups']));self.assertTrue(any(g.get('sunlit_accessory') for g in out['groups']))

if __name__=='__main__':unittest.main()
