"""Reference-sculpted voxel primitives and greedy surface meshing (native units)."""
import math
from collections import defaultdict

S=3

def rgb(value):
    return int(value.lstrip('#'),16) if isinstance(value,str) else value

def shade(color,amount):
    c=rgb(color)
    return sum(max(0,min(255,round(((c>>shift)&255)+amount)))<<shift for shift in (16,8,0))

class Sculpture:
    def __init__(self):
        self.v={}
        self.group='body'
        self.fronts={}
        self.overlays={}
    def put(self,x,y,z,color,group=None):
        g=group or self.group
        self.v[(x,y,z,g)]=rgb(color)
        key=(x,y,g)
        if key not in self.fronts or z<self.fronts[key]:self.fronts[key]=z
    def box(self,x,y,z,w,h,d,color,group=None):
        for a in range(round(x*S),round((x+w)*S)):
            for b in range(round(y*S),round((y+h)*S)):
                for c in range(round(z*S),round((z+d)*S)):
                    self.put(a,b,c,color,group)
    def ell(self,x,y,z,rx,ry,rz,color,group=None,power=2):
        for a in range(math.floor((x-rx)*S),math.ceil((x+rx)*S)):
            xx=abs((a+.5)/S-x)/rx
            for b in range(math.floor((y-ry)*S),math.ceil((y+ry)*S)):
                yy=abs((b+.5)/S-y)/ry
                if xx**power+yy**power>1:continue
                for c in range(math.floor((z-rz)*S),math.ceil((z+rz)*S)):
                    if xx**power+yy**power+(abs((c+.5)/S-z)/rz)**power<=1:
                        self.put(a,b,c,color,group)
    def tube(self,points,radius,color,group=None):
        for start,end in zip(points,points[1:]):
            n=max(1,math.ceil(math.dist(start,end)*S*1.3))
            for k in range(n+1):
                t=k/n;pos=[a+(b-a)*t for a,b in zip(start,end)]
                self.ell(*pos,radius,radius,radius,color,group)
    def taper(self,start,end,r1,r2,color,group=None):
        n=max(1,math.ceil(math.dist(start,end)*S))
        for k in range(n+1):
            t=k/n;r=max(.18,r1+(r2-r1)*t)
            pos=[a+(b-a)*t for a,b in zip(start,end)]
            self.ell(*pos,r,r,r,color,group)
    def leaf(self,start,end,width,color,group=None,vein=None):
        # Thin curved lamina in 3D, not a rectangular wing billboard.
        dx,dy,dz=[b-a for a,b in zip(start,end)]
        norm=max(.001,math.hypot(dx,dy));px,py=-dy/norm,dx/norm
        for k in range(max(6,math.ceil(norm*S))+1):
            t=k/max(6,math.ceil(norm*S));spread=math.sin(math.pi*t)**.8*width
            center=[start[0]+dx*t,start[1]+dy*t,start[2]+dz*t-.45*math.sin(math.pi*t)]
            n=max(1,math.ceil(spread*S))
            for j in range(-n,n+1):
                off=j/S
                self.ell(center[0]+px*off,center[1]+py*off,center[2]+abs(off)*.08,.22,.22,.28,color,group)
        if vein:self.tube([start,end],.18,vein,group)
    def spiral(self,x,y,z,r,color,group=None,turns=1.25):
        points=[]
        for k in range(40):
            t=k/39;angle=t*math.tau*turns;rr=r*(1-.82*t)
            points.append((x+math.cos(angle)*rr,y+math.sin(angle)*rr,z-.25*t))
        self.tube(points,.42,color,group)
    def flower(self,x,y,z,color,size=1,group='head',center='e9bb61',petals=5):
        for n in range(petals):
            a=n*math.tau/petals
            self.ell(x+math.cos(a)*size*.65,y+math.sin(a)*size*.65,z,.48*size,.57*size,.28*size,shade(color,5 if n<3 else -4),group)
        self.ell(x,y,z-.25,.28*size,.28*size,.22*size,center,group)
    def front(self,x,y,group='head'):
        a,b=math.floor(x*S),math.floor(y*S)
        value=self.fronts.get((a,b,group))
        if value is None:
            vals=[self.fronts[(xx,yy,group)] for xx in range(a-1,a+2) for yy in range(b-1,b+2) if (xx,yy,group) in self.fronts]
            value=min(vals) if vals else -15
        return (value-1)/S
    def mark(self,x,y,color,group='head'):
        z=self.front(x,y,group)
        self.box(x,y,z,1/S,1/S,1/S,color,group)
    def surface_paint(self,pixels,layer,group='head'):
        # Pigment follows existing 3D surface faces; no raised cubes or eye stalks.
        directions=[(-1,0,0),(1,0,0),(0,-1,0),(0,1,0),(0,0,-1)]
        for (x,y),color in pixels.items():
            front=self.fronts.get((x,y,group))
            if front is None:continue
            for z in range(front,front+6):
                if (x,y,z,group) not in self.v:continue
                for side,(dx,dy,dz) in enumerate(directions):
                    if (x+dx,y+dy,z+dz,group) not in self.v:
                        self.overlays[(x,y,z,side,layer)]=rgb(color)
    def eyes(self,y,sep,width=1.3,height=1.35,iris='67452f',kind='almond',group='head',planar=False):
        opened={};closed={}
        for sign in [-1,1]:
            cx=sign*sep
            for a in range(round((cx-width*.7)*S),round((cx+width*.7)*S)+1):
                for b in range(round((y-height*.7)*S),round((y+height*.7)*S)+1):
                    dx=((a+.5)/S-cx)/(width/2);dy=((b+.5)/S-y)/(height/2)
                    if dx*dx+dy*dy>1:continue
                    color=rgb('322b2a')
                    if kind not in ['slit','dot']:
                        if dx*dx+dy*dy<.72:color=rgb(iris)
                        if abs(dx)<.27:color=rgb('272224')
                        if a==round((cx-width*.16)*S) and b==round((y+height*.23)*S):color=rgb('e8decc')
                    opened[(a,b)]=color
            for k in range(max(4,round(width*S))+1):
                t=k/max(4,round(width*S));x=cx-width/2+width*t
                yy=y+math.sin(math.pi*t)*height*.35
                closed[(math.floor(x*S),math.floor(yy*S))]=rgb('3e302b')
        self.surface_paint(opened,'eyes',group);self.surface_paint(closed,'smile',group)
    def recolor(self,fn,groups=None):
        for key,color in list(self.v.items()):
            x,y,z,g=key
            if groups is None or g in groups:
                self.v[key]=rgb(fn(x/S,y/S,z/S,color,g))
    def mesh(self):
        directions=[(-1,0,0),(1,0,0),(0,-1,0),(0,1,0),(0,0,-1),(0,0,1)]
        layers=defaultdict(set)
        for (x,y,z,g),color in self.v.items():
            for side,(dx,dy,dz) in enumerate(directions):
                if (x+dx,y+dy,z+dz,g) in self.v:continue
                # A few clustered pigment values, not glitter or uniformly banded rings.
                noise=((x//3*73856093)^(y//3*19349663)^(z//3*83492791))%7-3
                delta=noise*1.4 if g not in ['eyes','smile'] else 0
                c=shade(color,delta)
                axis=side//2
                plane=(x,y,z)[axis]
                u,v=((z,y) if axis==0 else (x,z) if axis==1 else (x,y))
                layers[(g,side,plane,c)].add((u,v))
        for (x,y,z,side,g),color in self.overlays.items():
            axis=side//2;plane=(x,y,z)[axis]
            u,v=((z,y) if axis==0 else (x,z) if axis==1 else (x,y))
            layers[(g,side,plane,color)].add((u,v))
        result=[]
        for (g,side,plane,color),points in sorted(layers.items()):
            while points:
                u,v=min(points,key=lambda p:(p[1],p[0]));w=1
                while (u+w,v) in points:w+=1
                h=1
                while all((xx,v+h) in points for xx in range(u,u+w)):h+=1
                for xx in range(u,u+w):
                    for yy in range(v,v+h):points.remove((xx,yy))
                axis=side//2
                pos=(plane,v,u) if axis==0 else (u,plane,v) if axis==1 else (u,v,plane)
                dims=(1,h,w) if axis==0 else (w,1,h) if axis==1 else (w,h,1)
                result.append([*[round(n/S,4) for n in pos],side,color,g,*[round(n/S,4) for n in dims]])
        return result
