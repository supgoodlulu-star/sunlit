"""B reference layout, converted into closed bevelled 3D surfaces.

Coordinates are manually authored landmarks in the supplied 1280x853 concept.
The geometry, not an image billboard, carries the scallops, rims and petals.
"""
from math import pi, sin, cos, sqrt
from fantasy_sculpture import sweep, rod, normalize


def smooth(points, steps=3, closed=False):
    out=[];count=len(points)
    for i in range(count if closed else count-1):
        a=points[(i-1)%count] if closed else points[max(0,i-1)]
        b=points[i];c=points[(i+1)%count]
        d=points[(i+2)%count] if closed else points[min(count-1,i+2)]
        for j in range(steps):
            t=j/steps
            out.append(tuple(.5*((2*b[k])+(-a[k]+c[k])*t+(2*a[k]-5*b[k]+4*c[k]-d[k])*t*t+(-a[k]+3*b[k]-3*c[k]+d[k])*t*t*t) for k in range(len(b))))
    if not closed:out.append(points[-1])
    return out


class Reference:
    def __init__(self,m,kind):
        self.m=m;self.kind=kind
        self.origin=(539,455) if kind=='sword' else (610,735)
        self.scale=.058 if kind=='sword' else .070
        self.axis=(.5,-sqrt(.75)) if kind=='sword' else (.385,-sqrt(1-.385**2))

    def point(self,p,z=0):
        x=p[0]-self.origin[0];y=p[1]-self.origin[1];a,b=self.axis
        return ((-b*x+a*y)*self.scale,(a*x+b*y)*self.scale+2.6,z)

    def line(self,points,z=.8,r=.055,mat='metal',steps=2):
        rod(self.m,[self.point(p,z) for p in smooth(points,steps)],r,mat,6)

    def outline(self,points,z=0,depth=1.4,face='main',edge='secondary',steps=3,bevel=.10):
        """One closed solid with two bevel rings; concave contours are ear-clipped."""
        pts=[self.point(p)[:2] for p in smooth(points,steps,True)]
        if sum(pts[i][0]*pts[(i+1)%len(pts)][1]-pts[(i+1)%len(pts)][0]*pts[i][1] for i in range(len(pts)))<0:pts.reverse()
        # Offset the rim locally: centroid scaling cuts across a long blade's notches.
        inner=[]
        for i,p in enumerate(pts):
            a=pts[i-1];b=pts[(i+1)%len(pts)]
            n1=normalize((-(p[1]-a[1]),p[0]-a[0]));n2=normalize((-(b[1]-p[1]),b[0]-p[0]))
            den=max(.25,1+sum(x*y for x,y in zip(n1,n2)))
            inner.append((p[0]+bevel*(n1[0]+n2[0])/den,p[1]+bevel*(n1[1]+n2[1])/den))
        for sign in [-1,1]:
            outer=[(x,y,z+sign*depth*.30) for x,y in pts]
            rim=[(x,y,z+sign*depth*.5) for x,y in inner]
            cap=rim if sign==1 else list(reversed(rim))
            lo=[min(p[k] for p in rim) for k in [0,1]];hi=[max(p[k] for p in rim) for k in [0,1]]
            uvs=[(.02+.96*(p[0]-lo[0])/max(.001,hi[0]-lo[0]),.02+.96*(p[1]-lo[1])/max(.001,hi[1]-lo[1])) for p in cap]
            self.m.face(cap,face,[(0,0,sign)]*len(rim),uvs)
            for i in range(len(pts)):
                j=(i+1)%len(pts);q=[outer[i],outer[j],rim[j],rim[i]]
                self.m.face(q if sign==1 else q[::-1],edge)
        for i in range(len(pts)):
            j=(i+1)%len(pts)
            self.m.face([(pts[j][0],pts[j][1],z-depth*.30),(pts[i][0],pts[i][1],z-depth*.30),(pts[i][0],pts[i][1],z+depth*.30),(pts[j][0],pts[j][1],z+depth*.30)],edge)

    def pearl(self,p,z=.8,r=.15,mat='pearl'):
        self.m.ellipsoid(self.point(p,z),(r,r,r),mat,8,4)

    def petal(self,center,length,width,angle,z=.9,mat='light',detail=2):
        """Cupped sakura petal with a notch, a raised face and real rounded thickness."""
        # Angles are in image space, clockwise from upward.
        outline=[(0,0),(-.38,.18),(-.55,.46),(-.50,.78),(-.28,1),(-.10,1.02),(0,.94),(.10,1.02),(.28,1),(.50,.78),(.55,.46),(.38,.18)]
        ca,sa=cos(angle),sin(angle)
        raw=[(center[0]+u*width*ca+v*length*sa,center[1]+u*width*sa-v*length*ca) for u,v in outline]
        pts=[self.point(p,z) for p in smooth(raw,detail,True)]
        if sum(pts[i][0]*pts[(i+1)%len(pts)][1]-pts[(i+1)%len(pts)][0]*pts[i][1] for i in range(len(pts)))<0:pts.reverse()
        cx=sum(p[0] for p in pts)/len(pts);cy=sum(p[1] for p in pts)/len(pts)
        inner=[(cx+(x-cx)*.88,cy+(y-cy)*.88,z+.34) for x,y,_ in pts]
        center3=(cx,cy,z+.54)
        back=[(x,y,z-.12) for x,y,_ in pts]
        # Pale petal rim, pink centre and smooth continuous vertex normals.
        for i,p in enumerate(pts):
            j=(i+1)%len(pts);q=pts[j]
            np=lambda a:normalize(((a[0]-cx)*.80,(a[1]-cy)*.80,1))
            self.m.face([p,q,inner[j],inner[i]],'secondary',[np(p),np(q),np(inner[j]),np(inner[i])])
            self.m.face([inner[i],inner[j],center3],mat,[np(inner[i]),np(inner[j]),(0,0,1)])
            self.m.face([back[j],back[i],p,q],'shade')
        self.m.face(list(reversed(back)),mat,[(0,0,-1)]*len(back))

    def flower(self,c,r=34,z=1.0,back=False):
        start=len(self.m.v);face_start=len(self.m.faces)
        # Five broad outer petals, with five smaller offset petals opening around the gem.
        detail=1 if r<20 else 2
        for i in range(5):self.petal(c,r,r*.86,i*2*pi/5-.35,z,'light',detail)
        if not back and r>=20:
            for i in range(5):self.petal(c,r*.60,r*.60,(i+.5)*2*pi/5-.35,z+.20,'main')
        self.jewel(c,r*.28,z+.68)
        if back:
            for i in range(start,len(self.m.v)):
                x,y,k=self.m.v[i];self.m.v[i]=(x,y,-k)
                if self.m.normals[i] is not None:
                    x,y,k=self.m.normals[i];self.m.normals[i]=(x,y,-k)
            for i in range(face_start,len(self.m.faces)):
                ids,mat=self.m.faces[i];self.m.faces[i]=(ids[::-1],mat)

    def jewel(self,c,r=12,z=1.4):
        x,y=c
        self.outline([(x,y-r*1.12),(x+r,y),(x,y+r*1.12),(x-r,y)],z,.24,'metal','gold_light',1,.10)
        rim=[self.point(p,z+.20) for p in [(x,y-r),(x+r*.88,y),(x,y+r),(x-r*.88,y)]]
        top=[self.point(p,z+.49) for p in [(x,y-r*.54),(x+r*.45,y),(x,y+r*.54),(x-r*.45,y)]]
        # Source image Y points down; point() restores the usual CCW front face.
        for i,mat in enumerate(['light','accent','shade','gem']):
            j=(i+1)%4;self.m.face([rim[j],rim[i],top[i],top[j]],mat)
        self.m.face(list(reversed(top)),'gem',[(0,0,1)]*4)
        self.pearl((x-r*.20,y-r*.35),z+.51,.065,'shine')

    def ribbon(self,points,width=8,z=.3):
        pts=smooth(points,4)
        sections=[]
        for i,p in enumerate(pts):
            x,y,_=self.point(p,z+.12*sin(i*.3))
            # Full width through the tail; taper only at the last curled tip.
            t=i/(len(pts)-1);w=width*self.scale*(1 if t<.85 else max(.05,(1-t)/.15))
            sections.append((x,y,z+.12*sin(i*.3),w,.09))
        sweep(self.m,sections,'main',8,False)


def grip(m,top):
    m.group='handle'
    sweep(m,[(0,2.0,0,.59,.67),(0,3.0,0,.62,.67),(0,top-1,0,.59,.67),(0,top,0,.63,.69)],'grip',12,False)
    # Diamond silk wrap, rather than a striped metal pole.
    for phase in [0,pi]:
        pts=[]
        for i in range(29):
            t=i/28;a=t*2*pi*(top-2)/4+phase
            pts.append((.64*cos(a),2+(top-2)*t,.70*sin(a)))
        rod(m,pts,.105,'wrap',6)
    for y in [2.05,top-.30]:
        for dy in [-.15,.15]:
            rod(m,[(.77*cos(i*2*pi/16),y+dy,.78*sin(i*2*pi/16)) for i in range(17)],.11,'metal',6)


def sculpt_reference(m,kind):
    m.material_library='reference_materials.mtl';r=Reference(m,kind)
    center=(607,339) if kind=='sword' else (697,530)
    top=r.point(center)[1]-.7
    grip(m,top)
    m.group='head'
    if kind=='sword':
        # One continuous scalloped left edge; NOT separate decorations glued to a sabre.
        outer=[(614,326),(616,302),(637,280),(647,251),(648,223),(669,210),(686,185),(696,159),(700,138),(700,116),(720,92),(745,48),(756,85),(759,114),(753,148),(739,184),(718,222),(696,255),(667,289),(637,318)]
        r.outline(outer,depth=1.4,face='light',edge='secondary',bevel=.075)
        # Gold outline inside the ivory cutting edge, following the same stepped shoulder.
        inset=[(622,312),(624,303),(645,283),(655,253),(655,227),(675,216),(693,188),(704,160),(708,140),(708,119),(725,98),(745,65),(750,91),(751,115),(745,147),(731,181),(711,217),(689,250),(660,284),(635,309)]
        r.outline(inset,z=0,depth=1.47,face='main',edge='metal',bevel=.045)
        # The illuminated inner facet is deliberately off-centre like the reference.
        r.outline([(632,307),(647,282),(665,255),(685,227),(707,192),(726,151),(737,112),(744,78),(745,117),(734,158),(716,198),(693,237),(669,271),(643,300)],z=.78,depth=.055,face='light',edge='light',bevel=.015)
        for side in [-1,1]:
            r.line([(621,316),(630,302),(650,288),(672,263),(692,231),(711,199),(728,160),(739,120)],z=side*.86,r=.055)
            r.line([(625,305),(623,291),(632,277),(646,272)],z=side*.86,r=.055)
        m.group='ornaments'
        r.flower((681,225),r=15,z=.94)
        r.flower((721,146),r=8,z=.93)
        # Narrow white glints inlaid into the blade surface.
        r.outline([(733,123),(737,130),(735,139),(732,132)],z=.9,depth=.04,face='shine',edge='shine',steps=1)
    else:
        # Authored broad leaf blades match the reference's asymmetry and shallow tips.
        left=[(692,510),(669,498),(641,492),(612,495),(584,506),(560,523),(539,548),(522,576),(548,560),(574,551),(599,547),(620,548),(645,548),(670,540),(689,532)]
        right=[(711,524),(740,530),(765,544),(787,566),(805,595),(815,625),(819,658),(803,639),(784,622),(761,611),(738,600),(719,581),(704,552)]
        for outline in [left,right]:r.outline(outline,depth=1.55,face='main',edge='secondary',bevel=.065)
        for side in [-1,1]:
            r.line([(528,571),(549,551),(576,535),(606,526),(637,527),(668,534)],z=side*.81,r=.07,mat='metal')
            r.line([(729,540),(752,550),(773,570),(793,599),(807,628),(816,651)],z=side*.81,r=.07,mat='metal')
        # Pink pearlescent broad facets and small white reflection marks.
        r.outline([(531,567),(557,534),(582,517),(613,506),(640,504),(665,513),(637,514),(611,517),(583,528),(555,548)],z=.80,depth=.05,face='light',edge='light',bevel=.02)
        r.outline([(739,544),(762,557),(781,578),(795,600),(804,625),(790,604),(775,585),(755,566)],z=.80,depth=.05,face='light',edge='light',bevel=.02)
        m.group='ornaments'
        for x,y,size in [(585,527,4),(769,579,4),(793,618,3)]:
            r.outline([(x,y-size),(x+size*.55,y),(x,y+size),(x-size*.55,y)],z=.9,depth=.06,face='shine',edge='shine',steps=1)

    m.group='ornaments'
    if kind=='sword':
        # The two open scrolls sit behind the bloom, not as a pair of large handles.
        r.line([(589,330),(569,321),(555,304),(540,308),(533,320),(539,337),(556,349),(578,351)],z=.30,r=.24,mat='metal')
        r.line([(623,352),(650,363),(665,377),(659,391),(641,391),(624,380),(617,365)],z=.30,r=.24,mat='metal')
        r.line([(586,332),(565,323),(551,311),(542,317),(543,329),(557,340),(576,345)],z=.55,r=.12,mat='gold_light')
        r.line([(626,358),(648,369),(654,380),(645,383),(627,374)],z=.55,r=.12,mat='gold_light')
        r.ribbon([(590,349),(564,372),(544,395),(521,416),(494,419),(476,411),(467,395)],width=8,z=.45)
        r.ribbon([(618,356),(619,383),(610,413),(611,438),(622,465),(632,473)],width=7,z=.42)
        r.ribbon([(585,351),(573,367),(563,387),(550,394)],width=7,z=.70)
        r.ribbon([(619,357),(632,376),(636,403),(627,424)],width=7,z=.70)
        r.line([(642,382),(647,408),(648,427)],z=.5,r=.045)
        r.pearl((648,430),.5,.16);r.jewel((648,439),7,.55)
    else:
        # Gold pointed leaves behind the blossom and an open ribbon bow beneath it.
        for c,l,w,a in [((689,517),35,19,-.6),((709,531),31,18,1.2),((681,541),26,16,-1.5)]:
            r.petal(c,l,w,a,z=.3,mat='metal')
        r.line([(695,550),(710,563),(725,574),(737,571),(737,560),(724,552),(709,550)],z=.3,r=.20,mat='metal')
        r.ribbon([(685,547),(660,566),(637,583),(619,605),(603,631),(584,638),(572,630)],width=8,z=.5)
        r.ribbon([(704,552),(714,584),(712,616),(714,652),(726,678),(722,692),(708,684)],width=8,z=.4)
    r.flower(center,r=44 if kind=='sword' else 48,z=1.03)
    r.flower(center,r=31,z=.82,back=True)

    # The pommel is a hanging pink flower bud, framed by a gold leafy collar.
    end=(539,455) if kind=='sword' else (610,735)
    m.ellipsoid(r.point((end[0],end[1]+7),.12),(.81,1.02,.80),'main',12,6)
    for offset,a in [(-1,-.4),(0,0),(1,.4)]:
        r.petal((end[0]+offset*5,end[1]-5),23,13,a+pi,z=.50,mat='light')
    r.line([(end[0]-19,end[1]-9),(end[0]-9,end[1]-15),(end[0]+3,end[1]-12),(end[0]+13,end[1]-2)],z=.75,r=.12)
    pendant=[(504,490),(505,499),(499,508)] if kind=='sword' else [(651,756),(660,769),(669,780)]
    r.line([(end[0]+(12 if kind=='pickaxe' else -10),end[1]+4),pendant[0]],z=.15,r=.05)
    r.line(pendant,z=.15,r=.05)
    r.pearl(pendant[0],.2,.18);r.pearl(pendant[1],.2,.13)
    px,py=pendant[-1];r.petal((px,py),22,13,pi+(.35 if kind=='sword' else -.6),z=.2,mat='main')
