"""Surface invariants for the nontrivial merged-quad art compiler."""
import unittest
from .voxel import Sculpture

class VoxelMeshingTests(unittest.TestCase):
    def area(self,faces):
        return sum((f[7]*f[8] if f[3]<2 else f[6]*f[8] if f[3]<4 else f[6]*f[7]) for f in faces)
    def test_cube_has_six_outer_faces(self):
        m=Sculpture();m.box(0,0,0,1,1,1,'ffffff');faces=m.mesh()
        self.assertEqual(len(faces),6)
        self.assertAlmostEqual(self.area(faces),6,places=3)
    def test_adjacent_volumes_have_no_internal_surface(self):
        m=Sculpture();m.box(0,0,0,1,1,1,'ffffff');m.box(1,0,0,1,1,1,'cccccc')
        self.assertAlmostEqual(self.area(m.mesh()),10,places=3)
    def test_eye_pigment_does_not_extrude_the_head(self):
        m=Sculpture();m.ell(0,3,0,3,3,3,'ddaabb','head');before=set(m.v)
        m.eyes(3.5,1.3,1.2,1.3)
        self.assertEqual(set(m.v),before)
        self.assertTrue(any(k[4]=='eyes' for k in m.overlays))
        self.assertTrue(any(k[4]=='smile' for k in m.overlays))
        self.assertTrue(all((*key[:3],'head') in before for key in m.overlays))
    def test_repeatable_mesh_output(self):
        m=Sculpture();m.ell(0,2,0,2,2,2,'cc8844');self.assertEqual(m.mesh(),m.mesh())

if __name__=='__main__':unittest.main()
