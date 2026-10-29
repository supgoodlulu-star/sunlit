"""Sixty individually art-directed sculptures, based on the approved Sunlit sheets.
Coordinates are in original model units; the voxel kernel samples at three times
that resolution. All silhouettes are genuinely volumetric from every direction.
"""
import math
from .voxel import Sculpture, rgb, shade, S

SEASONS={
 'SPRING':('edb3bb','92b272','f6dbd9','d786a0'),
 'SUMMER':('77b9ce','74b8b9','b5dfdf','edb99c'),
 'AUTUMN':('d29863','bf753e','efb158','dc7736'),
 'WINTER':('a6c7e4','adc7e5','e3edf4','829fcf')}
CREAM='f5e8d2';DARK='3b302b';WHITE='f1ecdf';PINK='dca8a4'

def nose(m,x,y,z,color=DARK,size=.75):
    m.ell(x,y,z,size,.45,.42,color,'head',power=3)
    m.tube([(x,y-.3,z),(x,y-.8,z),(x-.7,y-1,z+.1)],.16,'75554c','head')
    m.tube([(x,y-.8,z),(x+.7,y-1,z+.1)],.16,'75554c','head')

def ears(m,y,z,main,kind='point',size=1):
    for sign in [-1,1]:
        x=sign*3.5
        if kind=='point':
            m.taper((x,y,z),(x+sign*.6,y+3.6*size,z-.2),1.55*size,.12,main,'head')
            m.taper((x,y+.5,z-1),(x+sign*.5,y+3*size,z-.5),.78*size,.1,PINK,'head')
        else:
            m.ell(sign*4,y,z,1.6*size,1.7*size,.9*size,main,'head')
            m.ell(sign*4,y,z-.65,1.05*size,1.1*size,.4*size,PINK,'head')

def paws(m,main,length=4,spread=3.2,back=4,front=-4,cream=False):
    for sign in [-1,1]:
        for z,group in [(front,'legs_front'),(back,'legs_back')]:
            m.ell(sign*spread,length/2,z,1.55,length/2,1.7,main,group,power=3)
            m.ell(sign*spread,.7,z-.55,1.65,.7,2,CREAM if cream else main,group,power=3)
            for toe in [-.55,.55]:m.box(sign*spread+toe,.25,z-2.3,.22,.5,.35,shade(main,-24),group)

def fur(m,center,rx,ry,rz,color,group='body',count=12):
    x,y,z=center
    for n in range(count):
        a=n*math.tau/count
        m.ell(x+math.cos(a)*rx*.8,y+math.sin(a)*ry*.65,z,rx*.33,ry*.36,rz,color,group)

def whiskers(m,y,z,width=4,color='d7c8ac'):
    for sign in [-1,1]:
        for k in [-1,0,1]:m.tube([(sign*2,y+k*.3,z),(sign*(width+1.7),y+k*.7,z+.3)],.12,color,'head')

def life(m,id):
    palette={'fox':'cf7a36','dog':'b87c43','cat':'a59a89','capybara':'b1844f','hyrax':'97816a','red_panda':'af5430','otter':'806047','ferret':'b19a77','hedgehog':'9b795b','chinchilla':'b5acb4','hamster':'d4a069','guinea_pig':'a97443','raccoon':'8b8173','squirrel':'bf7438','quokka':'ad8b62','koala':'a7aaa3','meerkat':'c5a06d','penguin':'9ba4ae','duck':'ecd276','seal':'e7e0d2'}
    c=palette[id]
    if id in ['penguin','duck']:
        m.ell(0,4.5,1,4.3,4.8,3.7,c);m.ell(0,9,-1,4,3.8,3.7,c,'head')
        m.ell(0,4,-2,3.2,3.5,1.5,CREAM if id=='penguin' else 'f6e394')
        if id=='penguin':
            m.ell(0,9,-2.7,3.5,2.9,2.2,'f4ecdb','head');m.ell(0,11.7,-.5,3.7,1.2,3.4,'626871','head')
            m.ell(0,8,-5,1.2,.6,1.2,'9b744b','head',power=3)
        else:m.ell(0,7.7,-4.6,2.1,.65,1.8,'da9445','head',power=3);m.ell(-1,12,-1,.9,1.3,.8,'f4dd86','head')
        for sign in [-1,1]:
            m.ell(sign*3.8,5.3,1,1.3,3,1.7,c,'wings')
            m.ell(sign*1.8,.45,-.8,1.25,.45,1.8,'ae8756' if id=='penguin' else 'd69c4c','legs_back',power=3)
        m.eyes(9.8,2.3,1.05,1.25,'332d28',kind='dot');return
    if id=='seal':
        m.ell(0,3,2,4.5,2.8,7,c);m.ell(0,5,-4,4,3.3,3.7,c,'head')
        m.ell(0,4.2,-7,2.3,1.5,1.2,WHITE,'head');nose(m,0,4.7,-8)
        for sign in [-1,1]:m.ell(sign*4.3,1.3,1,2.5,.6,3.2,c,'wings');m.ell(sign*1.8,2,8.5,2.6,.65,2,c,'tail')
        m.eyes(6.2,2,1.1,1.1,kind='dot');whiskers(m,4,-8,3.5,'9a8873');return
    if id in ['hamster','guinea_pig','quokka','koala','meerkat']:
        if id=='meerkat':
            m.ell(0,5.5,1,2.8,5.3,2.6,c);m.ell(0,11,-1.2,3.4,2.9,3,c,'head');m.ell(0,10,-4,2,1.1,1.5,CREAM,'head')
            for sign in [-1,1]:m.ell(sign*2.4,5.7,-1.3,.7,2.4,.8,c,'arms');m.ell(sign*1.7,.7,.3,1.3,.7,1.7,c,'legs_back');m.ell(sign*2,12.4,-3,.9,.8,.4,'71604e','head')
            m.taper((0,1.8,3),(0,1,8),1,.3,c,'tail');ears(m,12,-.5,c,'round',.65);nose(m,0,10.3,-5.2,DARK,.55);m.eyes(12.4,1.9,1,1.1,kind='dot');return
        seated=id in ['quokka','koala','hamster']
        by=4.5 if seated else 3.2
        m.ell(0,by,1.5,4.3,4.4 if seated else 3.1,3.7 if seated else 5.6,c)
        hy=9 if seated else 5.7
        m.ell(0,hy,-2.5,4,3.6,3.3,c,'head')
        m.ell(0,hy-1,-5,2.9,1.9,1.1,CREAM if id!='koala' else 'd6d6c8','head')
        ears(m,hy+2,-1.9,c,'round',1.25 if id=='koala' else .65)
        if id=='koala':
            for sign in [-1,1]:m.ell(sign*4.2,hy+1.7,-1.4,2.4,2.3,1.5,c,'head');m.ell(sign*4.2,hy+1.7,-2.7,1.65,1.6,.45,'ddd7cb','head')
            m.ell(0,hy,-5.7,1.15,1.7,.65,'514846','head',power=3)
        else:nose(m,0,hy-.5,-6,'9c6b66' if id=='hamster' else DARK,.65)
        for sign in [-1,1]:
            m.ell(sign*3,1,-1.8,1.8,1.1,2.1,CREAM if id in ['hamster','guinea_pig'] else c,'legs_back')
            m.ell(sign*3.3,4.4,-2.8,.9,2,.9,c,'arms')
        if id=='hamster':
            for sign in [-1,1]:m.ell(sign*2.5,hy-1.8,-4.3,1.8,1.4,1.5,WHITE,'head');m.ell(sign*2.5,.6,-3,1,.5,.8,PINK,'legs_back')
            m.recolor(lambda x,y,z,color,g:WHITE if abs(x)<1.1 and z<-.5 and y>7 else color,{'head'})
        if id=='guinea_pig':m.recolor(lambda x,y,z,color,g:WHITE if -2<z<1 or x>1.4 and z< -1 else '553a2c' if x<-1 and z>1 else color,{'body','head'})
        m.eyes(hy+.5,2.15,1.1,1.1 if id!='quokka' else .85,kind='dot');return
    low=id in ['capybara','hyrax','otter','ferret','hedgehog']
    hy={'capybara':8,'hyrax':6.5,'otter':6,'ferret':6.7,'hedgehog':4.8,'fox':8.3,'dog':8,'cat':7.6,'red_panda':7.5,'chinchilla':7,'raccoon':7.3,'squirrel':7.5}[id]
    m.ell(0,4.5 if low else 5,1.8,4.5 if id!='ferret' else 3.4,3.5,6.7 if low else 5.8,c)
    paws(m,c,3.4 if low else 4,3 if id!='ferret' else 2.4,4.7,-3.7,cream=id in ['dog','cat'])
    if id=='capybara':
        m.ell(0,hy,-4.6,4,3.4,4.5,c,'head',power=3.1);m.ell(0,hy-1,-8,3.7,2.3,2.5,c,'head',power=3.6)
        m.box(-2,hy-.3,-10.5,4,.7,.35,'775640','head')
        for sign in [-1,1]:m.ell(sign*2.8,hy+2.9,-3.3,.9,1,1,c,'head')
        m.eyes(hy+1.25,2.9,.95,.48,kind='slit');return
    rx=3.9 if id not in ['dog','cat','red_panda','chinchilla'] else 4.3
    m.ell(0,hy,-4.3,rx,3.6 if not low else 3,3.6,c,'head',power=2.3)
    muzzlecolor=CREAM if id not in ['hyrax','chinchilla'] else shade(c,24)
    m.ell(0,hy-1.1,-7.5,2.8 if id!='ferret' else 2.3,1.8,1.6,muzzlecolor,'head')
    if id=='dog':
        for sign in [-1,1]:m.ell(sign*4.1,hy+.2,-3.6,1.8,3.3,2.1,'94603c','head')
        m.recolor(lambda x,y,z,color,g:CREAM if abs(x)<1 and y>hy-1 and z<-3 else color,{'head'})
        fur(m,(0,5,-4.8),3.5,2.4,1.6,CREAM);m.ell(0,hy-2.4,-8.8,.5,.6,.3,'d59691','head')
    elif id=='hedgehog':
        for ring in range(6):
            for n in range(12):
                a=n*math.tau/12;y=5.7+math.cos(a)*2;xx=math.sin(a)*3.3;z=-.3+ring*1.2
                m.taper((xx,y,z),(xx*1.3,y+1.8,z+.7),.46,.1,'d9bea0' if (n+ring)%3 else '80684f')
        ears(m,hy+1.9,-3.2,c,'round',.48)
    elif id in ['hyrax','otter','red_panda','raccoon','ferret']:

        ears(m,hy+2.8,-3,c,'round',.6 if id in ['hyrax','otter','ferret'] else .85)
    else:ears(m,hy+(3.4 if id=='chinchilla' else 2.2),-3,c,'round' if id=='chinchilla' else 'point',1.6 if id=='chinchilla' else .85)
    if id in ['fox','red_panda','raccoon','squirrel','cat','otter','ferret','chinchilla']:
        if id=='squirrel':
            m.tube([(0,3,6),(0,5,9),(0,9,10),(0,13,8),(0,13,5)],2.2,c,'tail');m.tube([(0,5,10),(0,10,10),(0,12,7)],1,CREAM,'tail')
        elif id=='cat':m.tube([(0,5,6),(1,7,9),(1,11,9),(1,13,7)],.95,c,'tail')
        elif id=='chinchilla':m.tube([(0,4,6),(0,5,8),(0,8,8),(0,9,6)],2,c,'tail')
        elif id=='fox':m.tube([(0,4,6),(0,7,9),(0,8.5,12)],2.6,c,'tail');m.taper((0,8.5,11),(0,9.3,14.5),2.5,.5,CREAM,'tail')
        else:m.tube([(0,4,6),(1,3,9),(2,2.6,11),(3,3,12)],1.3 if id in ['otter','ferret'] else 2,c,'tail')
    if id=='fox':
        fur(m,(0,5.4,-4.3),3.6,2,1.5,CREAM);m.recolor(lambda x,y,z,color,g:'493b32' if y<1.8 else color,{'legs_front','legs_back'})
    if id=='cat':
        def tabby(x,y,z,color,g):
            if g=='head' and z<-5 and abs(x)<2 and y<hy-.3:return CREAM
            stripe=abs(math.sin(z*1.4+x*.45))>.88
            if g=='tail':stripe=int(y*1.3)%4==0
            if stripe and y>3:return '625b52'
            return color
        m.recolor(tabby,{'head','body','tail'});whiskers(m,hy-1,-9,3.5)
    if id=='red_panda':
        for sign in [-1,1]:m.ell(sign*2.5,hy,-7.1,1.4,1.8,.8,CREAM,'head');m.ell(sign*2.7,hy-1.5,-7.6,.65,.65,.4,'85402c','head')
        m.recolor(lambda x,y,z,color,g:'4b3429' if y<3 else color,{'legs_front','legs_back'})
    if id in ['raccoon','ferret']:
        for sign in [-1,1]:m.ell(sign*2.2,hy+.3,-6.8,1.7,1.1,.75,'5f5145','head')
    if id in ['raccoon','red_panda','cat']:
        m.recolor(lambda x,y,z,color,g:shade(c,-45) if int((z if id!='cat' else y)*1.2)%4==0 else color,{'tail'})
    if id in ['otter','ferret','hyrax']:whiskers(m,hy-1,-9,3,'b3a187')
    nose(m,0,hy-.6,-9,'c28d87' if id in ['cat','chinchilla'] else DARK,.64)
    m.eyes(hy+.7,2.35,1.35 if id in ['cat','dog'] else 1.1,.9 if id in ['hyrax','otter'] else 1.35,'92a268' if id=='cat' else '68492f',kind='dot' if id in ['hyrax','hedgehog','otter','chinchilla'] else 'almond')

def horns(m,season,x,y,z,sign=1):
    color=SEASONS.get(season,('94bdd5','92b9cc','f6e7d1','84abc8'))[1]
    if season=='SUMMER':
        m.taper((x,y,z),(x+sign*.7,y+3,z+.3),.7,.25,'edae91','head')
        m.taper((x+sign*.3,y+1.4,z),(x-sign*.8,y+2.3,z),.45,.15,'efc1a4','head')
    elif season=='WINTER':
        m.taper((x,y,z),(x+sign*.7,y+3.2,z+.5),.75,.12,'a6c7e7','head')
        m.taper((x,y+1,z),(x-sign*.8,y+2.4,z+.4),.5,.1,'d5e3f1','head')
    elif season=='AUTUMN':
        m.taper((x,y,z),(x+sign*.8,y+2.8,z+.4),.7,.2,'dfae6b','head')
        m.taper((x+sign*.2,y+1,z),(x-sign*.8,y+2,z),.4,.15,'e9c58a','head')
    elif season=='SPRING':
        m.taper((x,y,z),(x+sign*.7,y+2.5,z+.4),.7,.2,'db99ac','head')
        m.flower(x+sign*.6,y+1.8,z-.4,'efb5c5',.65)
    else:m.taper((x,y,z),(x+sign*.5,y+2,z+.5),.65,.15,'92b5d1','head')

def wings(m,season,y=7,z=2):
    body,leaf,light,ornament=SEASONS.get(season,('99bed8','8fb9d7','c9e0ed','86a9cc'))
    for sign in [-1,1]:
        origin=(sign*3.2,y,z)
        if season in ['SPRING','AUTUMN']:
            for k in range(3):m.leaf(origin,(sign*(8.8-k),y+5-k*1.5,z+1.5+k*.3),1.5,light if k%2==0 else ornament,'wings',ornament if season=='SPRING' else leaf)
        else:
            for k in range(3):m.leaf(origin,(sign*(9.2-k*.6),y+5.2-k*1.8,z+k),1.25,light,'wings',ornament if season=='SPRING' else leaf)
            m.tube([origin,(sign*7.5,y+5,z+1)],.26,leaf,'wings')

def dragon(m,season=''):
    c,accent,light,detail=SEASONS.get(season,('8eb5d0','87adc5','d2e3ec','89b1cf'))
    m.ell(0,4.8,1.2,3.4,4.2,3.3,c,power=2.3)
    m.ell(0,10.4,-3.1,5.1,4.1,4.2,c,'head',power=4.8)
    m.ell(0,9.1,-6.7,4,2.1,2.25,c,'head',power=3.4)
    # Warm ivory chin and belly retain the Mir-like baby silhouette.
    m.ell(0,7.4,-7.1,3.2,.65,1.9,CREAM,'head',power=3)
    m.ell(0,4.3,-1.65,2.25,2.9,.95,CREAM)
    for y in [2.9,4.3,5.7]:m.tube([(-1.7,y,-2.25),(1.7,y,-2.25)],.12,'d4c5ad')
    for sign in [-1,1]:
        m.ell(sign*2.7,1.7,.3,1.65,1.7,2.3,c,'legs_back')
        for toe in [-.6,0,.6]:m.ell(sign*2.7+toe,.45,-1.8,.25,.28,.48,CREAM,'legs_back')
        m.ell(sign*3.1,4.9,-1.6,1,1.4,1.05,c,'arms')
        m.ell(sign*3.1,4.2,-2.35,.75,.6,.6,light,'arms')
        horns(m,season,sign*3.3,13.6,-1.2,sign)
        m.box(sign*2.1,9.9,-8.7,.28,.45,.3,'4c6570','head')
    # Ivory forehead scales are layered tapered locks, not a rectangular cap.
    m.ell(0,13.5,-3.4,4.7,1.6,3.5,CREAM,'head')
    for x,length in [(-3,2.1),(-1.6,3.1),(0,3.7),(1.6,2.8),(3,1.8)]:
        m.taper((x,13.7,-4.3),(x-.35,13.6-length*.55,-6.2),1,.25,shade(CREAM,5 if x<0 else -8),'head')
    points=[(0,3,3.3),(0,2.4,6),(0,3,9),(0,4.8,11),(0,6.3,12)]
    for k in range(len(points)-1):m.taper(points[k],points[k+1],1.8-k*.35,1.45-k*.35,c,'tail')
    if season=='SPRING':m.flower(0,6.5,12,light,1,'tail');m.leaf((0,5.5,11),(2,7,12),.9,accent,'tail',shade(accent,-25))
    elif season=='AUTUMN':
        for x in [-1,0,1]:m.leaf((0,5.5,11),(x*2.1,8-abs(x),12),1,detail,'tail',accent)
    elif season in ['SUMMER','WINTER']:
        for x in [-1,0,1]:m.leaf((0,5.5,11),(x*2.3,8-abs(x),12),1,light,'tail',accent)
    wings(m,season,6.5,2.2)
    m.tube([(-2,8,-8.6),(0,7.7,-8.85),(2.3,8.1,-8.5)],.14,'6f7470','head')
    m.eyes(11.8,3.2,1.55,1.6,'68452b',planar=True)

def fairy(m,season):
    c,accent,light,detail=SEASONS[season]
    skin='f2d8b7';hair='e5c584'
    m.ell(0,10.7,-.6,3.3,3.4,2.7,skin,'head')
    m.ell(0,12.2,.1,3.6,2.6,2.9,hair,'head')
    # Clear face plane with individually curved blond bangs and curls.
    m.ell(0,10.6,-2,2.9,2.7,1.6,skin,'head')
    for x in [-2.6,-1.6,-.5,.6,1.7,2.7]:m.taper((x,13,-1.6),(x-.35,11.7+(abs(x)*.15),-3.1),.65,.28,shade(hair,x*2),'head')
    for sign in [-1,1]:
        for y in [9,10.6,12.2]:m.spiral(sign*3,y,.1,.9,shade(hair,-8),'head',.8)
        m.tube([(sign*1.7,7,-.3),(sign*3.2,5.5,-.9),(sign*3.6,6,-1.6)],.5,skin,'arms')
        m.ell(sign*3.7,6,-1.7,.55,.55,.5,skin,'arms')
        m.ell(sign*1,1.1,0,.65,1.1,.65,skin,'legs_back');m.ell(sign*1,.35,-.4,.8,.45,.9,accent,'legs_back')
    m.ell(0,6.3,.2,1.9,2.1,1.4,c)
    for k in range(7):
        a=k*math.tau/7;xx=math.cos(a)*1.1;zz=math.sin(a)*1.1
        m.leaf((xx,6.8,zz),(xx*2,2.4,zz*2),1.5,light if k%2 else c,'body',detail)
    m.ell(0,7,-1.4,.45,.45,.3,'edcf85')
    for sign in [-1,1]:
        for k in [0,1]:m.leaf((sign*1.2,7,1.5),(sign*(7-k),12-k*5,2.5),2 if k==0 else 1.6,accent if season=='SPRING' else light,'wings',shade(accent,-25))
    if season=='SPRING':m.flower(-2.7,13.2,-2,'eaa3b8',1.15);m.leaf((-2,13,0),(-4,14,0),.7,accent,'head')
    elif season=='SUMMER':
        m.ell(-2.6,13,-1.9,1.1,.9,.45,'edc3af','head');m.flower(-1.6,13.5,-2.1,'c4dce0',.65)
    elif season=='AUTUMN':m.ell(-2.6,13.3,-1.4,1.25,.8,1.1,'ad784b','head');m.ell(-2.6,14,-1.4,1.3,.4,1.2,'805a37','head')
    else:
        for a in range(6):
            t=a*math.tau/6;m.tube([(-2.5,13,-2),(-2.5+math.cos(t)*1.4,13+math.sin(t)*1.4,-2)],.18,'edf2ed','head')
    m.eyes(10.7,1.55,1.15,1.35,'95653d')
    m.ell(0,9.3,m.front(0,9.3)-.1,.35,.16,.2,'b37766','head');m.ell(0,10,m.front(0,10)-.05,.23,.25,.2,'e7c6a6','head')

def haetae(m,season):
    c,accent,light,detail=SEASONS[season]
    stone='e3d8bd'
    m.ell(0,4.9,2,4.5,3.8,6,stone,power=2.8);paws(m,stone,3.8,3.2,5,-3.9)
    m.ell(0,8.5,-3.5,4.5,4.1,3.7,stone,'head',power=2.6)
    # Seven actual spiral locks form a lion-like guardian mane around the face.
    for n in range(9):
        a=n*math.tau/9;x=math.cos(a)*5;y=8.8+math.sin(a)*4.6
        m.ell(x,y,-2.8,1.9,1.9,1.6,('dba5b3' if n%3 else accent) if season=='SPRING' else accent,'head')
        m.spiral(x,y,-4,1.3,('eac2cb' if n%3 else light) if season=='SPRING' else detail if n%3==0 else light,'head',1.1)
        if n%3==1:m.flower(x,y,-4.6,light,.7)
    m.ell(0,6.8,-6.9,3.5,1.65,1.55,CREAM,'head',power=3.5)
    m.ell(0,7.6,-8.1,1.3,.65,.45,'715a46','head',power=3)
    for sign in [-1,1]:
        m.tube([(sign*.5,6.1,-8),(sign*2.4,6.4,-7.8)],.18,'725e4f','head')
        m.ell(sign*2,10.2,-6.6,1.5,.5,.55,stone,'head')
        m.taper((sign*2.4,6.4,-7.8),(sign*2.4,5.6,-7.8),.34,.12,WHITE,'head')
        # Flank guardian relief: a lighter curled stone carving.
        m.spiral(sign*3.7,5.6,1.5,1.15,shade(stone,12),'body',.9)
    m.taper((0,12.4,-3.4),(0,17.7,-3.9),1.2,.15,'debd82','head')
    for y in [13.2,14.5,15.8]:m.ell(0,y,-3.4,.85,.18,.8,'f0dcb6','head')
    m.tube([(0,4,6),(1,6,9),(2,8,9),(3.5,8.5,8)],1.35,accent,'tail');m.spiral(3,7.5,8,1.5,light,'tail')
    m.eyes(8.7,2.25,1.55,.9,'af7c3c')

def whale(m,season):
    c,accent,light,detail=SEASONS[season]
    m.ell(0,5.4,0,5.6,4,8.1,c,power=2.4)
    m.ell(0,4.3,-3.2,5,2.65,5.7,CREAM,power=2.8)
    m.ell(0,5.4,-6.1,5.3,2.8,3.1,c,'head',power=3)
    m.ell(0,3.6,-6.2,4.8,1.3,3,CREAM,'head',power=3.2)
    for sign in [-1,1]:
        m.leaf((sign*4,4.7,-1),(sign*9,2.4,2.8),2.5,light if season=='WINTER' else detail,'wings',accent)
        m.leaf((sign*3.7,4.3,-1),(sign*8,2.6,2.8),1.45,CREAM,'wings')
        m.leaf((0,7,8),(sign*4.9,10.8,13),2.4,light if season=='WINTER' else detail,'tail',accent)
        m.leaf((0,7.1,8),(sign*3.8,10.6,13),1.1,light,'tail',c)
    m.taper((0,5,6),(0,7,10),2.5,.9,c,'tail')
    for x in [-1.8,0,1.8]:m.tube([(x,3,-8),(x,2.8,-6)],.13,'cfbea5','head')
    m.tube([(-3.8,4.6,-8.8),(0,4.3,-9.1),(3.8,4.6,-8.8)],.15,'ad9981','head')
    if season=='SPRING':m.flower(-2,8.7,-2,light,1);m.flower(-.5,8.9,-2.4,detail,.65)
    elif season=='SUMMER':
        for x in [-2.5,-1.7]:m.taper((x,8.4,-1),(x,10.2,-1),.35,.15,'e9b49e','head');m.taper((x,9.2,-1),(x+.7,9.8,-1),.25,.12,'e9b49e','head')
    elif season=='AUTUMN':
        for a in range(5):m.leaf((-2,8.7,-1),(-2+math.cos(a*math.tau/5)*1.8,9.3+math.sin(a*math.tau/5)*1.2,-1.4),.6,detail,'head',accent)
    else:
        for a in range(6):m.tube([(-2,8.9,-1),(-2+math.cos(a*math.tau/6),8.9+math.sin(a*math.tau/6),-1)],.17,WHITE,'head')
    m.eyes(6.5,4.05,1.1,.48,'473b32',kind='slit')

def tiger(m,season):
    c,accent,light,detail=SEASONS[season]
    furcolor='f1eadc'
    m.ell(0,4.9,2,4.3,3.5,5.9,furcolor);paws(m,furcolor,3.8,3.1,5,-3.7)
    m.ell(0,8,-4,4.7,4,3.7,furcolor,'head',power=2.6)
    ears(m,11,-3,furcolor,'round',.9)
    for sign in [-1,1]:m.ell(sign*4,11,-3.7,1.4,1.45,.5,'403a37','head');m.ell(sign*4,11,-4.1,.9,.9,.25,PINK,'head')
    # Actual tapered tabby stripes around the volume, never a checkerboard.
    def stripes(x,y,z,color,g):
        if g=='body' and y>4 and abs(math.sin(z*1.45+x*.5))>.84:return '4a4541'
        if g=='head' and y>9 and abs(x)<3.5 and (abs(math.sin(x*1.35+z*.4))>.91):return '514b48'
        if g=='head' and abs(x)>3 and y<10 and int((y+x*.25)*1.5)%4==0:return '4a4541'
        return color
    m.recolor(stripes,{'body','head'})
    for sign in [-1,1]:m.ell(sign*1.3,6.9,-7.1,1.7,1.35,1.3,WHITE,'head')
    nose(m,0,7.4,-8.2,'bd8b85',.75);whiskers(m,6.5,-8.3,4,'ad9e89')
    # Seasonal ruff and tail bloom are innate anatomy, distinct from paid ornaments.
    for n in range(9):
        a=n*math.pi/8;x=math.cos(a)*4.4;y=5.7-math.sin(a)*1.9
        m.leaf((x*.7,y+1,-3.7),(x,y-1.1,-5.5),1.1,light if n%2 else detail,'body',accent)
    pts=[(0,4.5,6),(1,7,9),(1,10,9),(0,12,8)]
    m.tube(pts,1,furcolor,'tail');m.recolor(lambda x,y,z,col,g:'47433f' if int(y*1.3)%4==0 else col,{'tail'})
    m.flower(0,12,8,light if season in ['SPRING','WINTER'] else detail,1.8,'tail',center=accent,petals=7)
    m.eyes(8.9,2.45,1.6,1.05,'7fa4ba')

def fantasy(m,id):
    if id=='baby_dragon':dragon(m);return
    if id=='unicorn':
        c='eee7da';m.ell(0,4.5,1,3.6,3.2,5,c);paws(m,c,3.4,2.5,4,-3)
        m.ell(0,7,-3,2.8,3.8,2.6,c,'head');m.ell(0,9,-4.5,3.2,3,2.8,c,'head');m.ell(0,7.7,-7,2.5,1.4,1.6,c,'head',power=3)
        ears(m,11,-3,c,'point',.6)
        for x,y,z in [(0,11,-2),(0,10,0),(0,8,1),(0,7,2)]:m.spiral(x,y,z,1.8,'b9a2c6','head')
        m.tube([(0,5,5),(0,4,8),(0,2,8)],1.4,'b9a2c6','tail')
        m.taper((0,11.5,-4.6),(0,15.8,-5.1),.8,.1,'d3b875','head')
        for y in [12,13,14,15]:m.ell(0,y,-4.7,.55,.13,.55,CREAM,'head')
        m.eyes(9.5,2,1.1,1.1,kind='dot');return
    if id in ['griffin','phoenix']:
        c='ab7944' if id=='griffin' else 'db813a';head='eee4ce' if id=='griffin' else 'eaa14c'
        m.ell(0,4.7,1.2,3.8,3.8,4.6,c);m.ell(0,9,-2.6,3.6,3.8,3.1,head,'head')
        m.ell(0,7.7,-5.4,1.5,1.25,1.5,'d5a34e','head');m.taper((0,7.7,-6),(0,6.5,-6),.7,.25,'be8d40','head')
        fur(m,(0,6,-3.2),3.2,2,1.3,head,'head',9)
        for sign in [-1,1]:
            m.ell(sign*2.2,.8,-2,1.2,.8,1.8,'d2a052','legs_front')
            for k in range(5):
                start=(sign*3,5,1);end=(sign*(7.5-k*.4),10-k*.9,3+k*.65)
                m.leaf(start,end,1.1,shade(c,15-k*4),'wings',head if id=='griffin' else 'f0c569')
            for toe in [-.55,0,.55]:m.taper((sign*2.2+toe,.5,-2),(sign*2.2+toe,.25,-3),.25,.12,'bd9154','legs_front')
        if id=='griffin':
            paws(m,c,2.8,2.5,4,-2)
            m.tube([(0,4,5),(1,5,8),(1,7,9)],.7,c,'tail');m.ell(1,7,9,1.1,1.3,1.1,'85592f','tail')
            for x in [-2.5,0,2.5]:m.taper((x,11,-2),(x*1.2,13.2,-1),.9,.1,head,'head')
        else:
            for x in [-2,-1,0,1,2]:m.leaf((0,3.5,4),(x*1.5,7+abs(x)*.8,11),1,'eab34e' if x%2 else 'c5662b','tail','f0cf77')
            for k in range(4):m.leaf((0,11,-1),(k*.4,13+k*.8,.5),.8,'d76c30','head','efb354')
        m.eyes(9.4,2.25,1.3,1.1,'7b542f');return
    if id=='goblin':
        c='d5a2a3';m.ell(0,3.2,1,2.7,2.6,2.6,c);m.ell(0,8,-1,4.8,3.8,3.5,c,'head',power=2.4)
        for sign in [-1,1]:
            m.taper((sign*3.5,9,0),(sign*7.5,9.7,-.4),1.6,.1,c,'head');m.taper((sign*4.1,9,-1),(sign*6.5,9.5,-1),.9,.1,'b97b83','head')
            m.ell(sign*2.2,.7,-1.7,1.3,.7,1.4,c,'legs_back');m.ell(sign*2.8,3,-1.5,.9,1.7,.9,c,'arms')
        m.taper((-2.5,10.7,0),(-2.7,13.5,0),1.1,.2,'d87499','head')
        for a in range(5):m.leaf((2.1,11.4,-1.6),(2.1+math.cos(a*math.tau/5)*1.5,11.4+math.sin(a*math.tau/5)*1.5,-1.6),.5,'edcc71','head')
        m.eyes(8.4,2.55,1.2,1.2,'715443');m.ell(0,6.4,-4,.4,.2,.3,'aa7174','head');return
    if id=='book':
        # Open vertical folio, volumetric fanned leaves and a living central spine.
        for sign in [-1,1]:
            for k in range(15):
                x=sign*(1+k*.43);z=abs(k)*.19;yy=.7+math.sin(k*.1)*.2
                m.box(x-.28,yy,z, .6,10.2,1.4,'e9d6ad' if k%3 else 'f6e7c6','head')
                m.box(x-.3,yy-.5,z+.65,.65,11.3,.7,'8c66a2','head')
                if k in [3,9,14]:m.box(x-.3,3,z-.25,.6,.3,.4,'bea264','head')
            m.box(sign*6.7-.4,.4,3.1,.8,11.5,1.5,'79548e','head')
        m.ell(0,5,-.6,1.6,4.7,1.1,CREAM,'head',power=3)
        for y in [2,8.5]:m.box(-.9,y,-1.8,1.8,.25,.25,'c5a15e','head')
        m.eyes(5.4,.75,.65,1.05,kind='dot');return
    if id=='clockwork':
        c='c29b59';m.ell(0,4.5,0,2.8,3.2,2.2,c,power=3.5);m.ell(0,10,-1,4.2,3.1,2.8,c,'head',power=5)
        m.box(-3.5,7.4,-3.8,7,5,.35,'e0bc78','head')
        for sign in [-1,1]:
            m.ell(sign*4.4,9.6,-.2,.9,1.1,1.2,'95703e','head');m.ell(sign*1.8,.6,-.5,1.3,.6,1.8,c,'legs_back')
            m.tube([(sign*2.6,5.6,0),(sign*3.6,3.3,-1)],.55,'956d3b','arms');m.ell(sign*3.6,3.3,-1,.8,.8,.8,c,'arms')
            for y in [8,11.8]:m.ell(sign*3,y,-4,.3,.3,.2,'93713e','head')
        m.ell(0,4.4,-2.2,1.3,1.4,.4,'e1bc75');m.box(-.3,4,-2.8,.6,1.2,.3,'84663d')
        m.box(-.4,5,2, .8,.8,2,'9e7944')
        for y in [5,7.2]:
            pts=[(0+math.cos(k*math.tau/24)*1.25,y+math.sin(k*math.tau/24)*1.1,4) for k in range(25)];m.tube(pts,.35,c)
        m.eyes(10,2.1,.65,1.3,kind='dot');return
    if id in ['moss_golem','pebble']:
        stone='a29e8d' if id=='moss_golem' else 'b2a99a'
        m.ell(0,4.3,1,3.5,3.1,2.7,stone,power=3);m.ell(0,9,-.6,4.2,3.3,3.1,stone,'head',power=2.8)
        for sign in [-1,1]:
            m.ell(sign*4.4,5.1,.3,1.9,2.4,2,shade(stone,-8),'arms',power=2.5);m.ell(sign*4.9,2.6,0,1.6,1.4,1.8,stone,'arms')
            m.ell(sign*2.3,.8,.3,1.8,.8,2.1,stone,'legs_back',power=2.6)
        if id=='moss_golem':
            def moss(x,y,z,col,g):return '829365' if (y>10 or abs(x)>3.7 and y>5) and math.sin(x*1.2+z*.8+y)>.05 else col
            m.recolor(moss,{'head','body','arms'})
            m.leaf((-3,10,1),(-5,7,1),1.1,'8fa56b','head');m.flower(2.4,11.9,-1,CREAM,.65)
        m.eyes(9,2.05,1.15,.55,kind='slit');m.ell(0,7.2,-3.5,.7,.15,.3,'6c665c','head');return
    if id in ['sprout','acorn','mushroom','flowerbud']:
        c={'sprout':'b78e58','acorn':'c28a42','mushroom':'efe1c4','flowerbud':'dca0ac'}[id]
        m.ell(0,4.8,0,3.6,4.6,3.2,c,'head')
        for sign in [-1,1]:m.ell(sign*2,.6,-.4,1.1,.6,1.2,shade(c,-15),'legs_back');m.ell(sign*3.5,3.7,.2,.8,1.3,.9,c,'arms')
        if id=='sprout':
            m.tube([(0,8,0),(0,11,.3),(1,12,.4)],.55,'7b9454','head')
            m.leaf((0,10.5,0),(-5,13,-.3),1.6,'91b563','head','6c8b43');m.leaf((1,11,0),(6,13.6,.3),1.5,'a3bc67','head','6c8b43')
            m.recolor(lambda x,y,z,col,g:shade(col,-10) if int((x+z)*1.5)%5==0 else col,{'head'})
        elif id=='mushroom':
            m.ell(0,10.4,.5,6.7,1.4,5.7,'d2b993','head')
            m.ell(0,11.1,.5,6.8,3.7,5.8,'cc7c58','head')
            # Cream cap freckles project into the cap rather than float in space.
            for x,y,z,r in [(-4,12.5,-2,1),(0,14.5,.1,1.2),(3,12.9,-3,1.1),(4,11.5,2,.8),(-2,13.5,3,1)]:m.ell(x,y,z,r,.35,r,'f4e6c9','head')
            spots=[(-4,-2,1.1),(0,.1,1.2),(3,-3,1.2),(4,2,.9),(-2,3,1)]
            m.recolor(lambda x,y,z,col,g:'f4e6c9' if y>11 and col==rgb('cc7c58') and any((x-a)**2+(z-b)**2<r*r for a,b,r in spots) else col,{'head'})
            for sign in [-1,1]:m.ell(sign*3.2,3.7,-1.5,1.1,1.4,1,c,'arms')
        elif id=='acorn':
            m.ell(0,9,.1,4.4,1.8,3.8,'81613f','head')
            for ring in range(3):
                for k in range(13):
                    a=k*math.tau/13;m.ell(math.cos(a)*(4-ring*.8),9+ring*.6,math.sin(a)*(3.2-ring*.6),.65,.6,.65,'96724b' if k%2 else '765839','head')
            m.tube([(0,10,0),(.4,12,0),(1.6,12.7,0)],.55,'765738','head')
        else:
            # A nonhuman flower face cradled in real tulip petals.
            m.ell(0,5.8,-2,2.25,2.7,1.7,'f5dbc5','head')
            for k in range(5):
                a=k*math.tau/5;start=(math.cos(a)*1.7,5,math.sin(a)*1.8);end=(math.cos(a)*3,12+math.sin(a),math.sin(a)*2.5)
                m.leaf(start,end,1.8,shade(c,7-k*4),'head','efc0c3')
            for sign in [-1,1]:m.leaf((sign*1.3,3,1),(sign*6.4,8,2),1.7,'90ab61','wings','688b45');m.leaf((sign*1,2,0),(sign*2.6,.3,-1),1.4,'7c9b54','body')
        m.eyes(5.4 if id!='flowerbud' else 6.1,1.7, .9,1.2,kind='dot');return
    if id in ['crystal','amber']:
        c='a18bd0' if id=='crystal' else 'dfab4a'
        # Tapered faceted crystal, with continuous bevels and multiple distinct shards.
        for y in range(1,round((15 if id=='crystal' else 13)*S)):
            yy=y/S;radius=(3.6 if id=='crystal' else 4.8)*math.sin(math.pi*yy/(15 if id=='crystal' else 13))**.65
            if radius>0:m.ell(0,yy,0,radius,.3,radius*.75,c,'head',power=4)
        if id=='crystal':
            for sign in [-1,1]:m.taper((sign*2.3,2,0),(sign*5.4,7,-.1),1.2,.2,shade(c,sign*12),'arms')
            m.recolor(lambda x,y,z,col,g:'c6b5e4' if x<-.7 and z<-.8 else '8971b9' if x>1.5 else col,{'head'})
        else:
            m.recolor(lambda x,y,z,col,g:'f3cf6c' if x<-1.5 and z<-1 else 'b77e30' if x>1.6 and math.sin(y*1.7+z)> .5 else col,{'head'})
            m.leaf((2,6,-2),(3,8,-1.8),.8,'a56b2c','head')
        for sign in [-1,1]:m.ell(sign*2,.6,-.3,1.5,.6,1.4,shade(c,-18),'legs_back')
        m.eyes(6.1,1.5,.85,1.1,kind='dot');return
    if id=='pearl':
        m.ell(0,6,3.8,5.4,4.1,.65,'bea3c5');m.ell(0,1.5,0,6,1.5,4.8,'b5a0c5');m.ell(0,2.4,-.4,5.2,.6,4,'f1c5bc')
        # Upright open fan shell with scalloped ribbed rim.
        for k in range(11):
            a=math.pi*k/10;m.tube([(0,2,2.5),(math.cos(a)*5.6,3.8+math.sin(a)*6,3.5)],.6,'c1a4c8','body')
            m.ell(math.cos(a)*5.6,3.8+math.sin(a)*6,3.5,.7,.7,.6,'e4b6bf')
        m.ell(0,5,-.4,3.4,3.4,3.1,'eee6d7','head');m.ell(-1.7,6.7,-2.4,.65,.8,.25,'f8f0df','head');m.eyes(5.6,1.65,.85,1.1,kind='dot');return
    if id in ['droplet','star_slime','cloud','whirlwind']:
        c={'droplet':'88cadd','star_slime':'a29bcc','cloud':'f0eadd','whirlwind':'bdd7d3'}[id]
        if id=='whirlwind':
            for ring in range(8):
                y=1+ring*1.2;r=1+ring*.55
                points=[(math.cos(k*math.tau/32)*r,y+math.sin(k*math.tau/32)*.35,math.sin(k*math.tau/32)*r) for k in range(33)]
                m.tube(points,.65,shade(c,ring%3*5),'head')
            m.leaf((4,6,1),(6,7.8,1),1,'92ae6a','body');m.eyes(6,1.8,.85,1.2,kind='dot')
        elif id=='cloud':
            for x,y,z,r in [(0,4,0,3.5),(-3.6,3.6,0,2.7),(3.6,3.8,0,2.8),(-1.5,6.5,.4,2.8),(2,6,.5,2.7),(-5,6.5,2,1.3),(5.7,7,2,1.1)]:m.ell(x,y,z,r,r*.8,r*.8,c,'head')
            m.eyes(4.6,1.8,1.05,.5,kind='slit')
        else:
            m.ell(0,3.5,0,5.3,3.4,4.4,c,'head')
            if id=='droplet':
                m.taper((.8,5,0),(3,11.8,0),3,.15,c,'head')
                m.tube([(-3.8,3,-2.8),(-3,6,-2),(0,8,-1)],.28,'d5edf0','head')
            else:
                m.ell(0,5.5,0,4.4,4,3.8,c,'head')
                for a in range(5):m.leaf((0,6,-3.7),(math.cos(a*math.tau/5)*1.6,6+math.sin(a*math.tau/5)*1.6,-3.8),.5,'efd480','head')
                for x,y in [(-3,4),(2,3),(2.5,7),(-2,8)]:m.ell(x,y,m.front(x,y),.3,.3,.2,'d4c5e7','head')
            m.eyes(3.7,2,.9,1.15 if id=='droplet' else .45,kind='dot' if id=='droplet' else 'slit')
        return
    raise ValueError(id)

def sculpt(id,season):
    m=Sculpture()
    if season:
        if id.endswith('_fairy'):fairy(m,season)
        elif id.endswith('_dragon'):dragon(m,season)
        elif id.endswith('_haetae'):haetae(m,season)
        elif id.endswith('_skywhale'):whale(m,season)
        elif id.endswith('_tiger'):tiger(m,season)
        else:raise ValueError(id)
    elif id in {'fox','dog','cat','capybara','hyrax','red_panda','otter','ferret','hedgehog','chinchilla','hamster','guinea_pig','raccoon','squirrel','quokka','koala','meerkat','penguin','duck','seal'}:life(m,id)
    else:fantasy(m,id)
    return m
