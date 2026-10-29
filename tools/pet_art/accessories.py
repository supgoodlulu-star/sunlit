"""Independent permanent-unlock accessories, mounted on the refined silhouettes."""
from .voxel import rgb, S

def decorate(m,id,season):
    m.group='accessory'
    box,ell=m.box,m.ell
    v=m.v
    g='accessory';gold=rgb('d3ac64');green=rgb('7f9c62');pink=rgb('c894a7');brown=rgb('946b47');cream=rgb('f4e4c4');blue=rgb('78beca');purple=rgb('aa91c2')
    def flower(x,y,z,color):
     for dx,dy in [(-1,0),(1,0),(0,-1),(0,1)]:box(x+dx,y+dy,z,1,1,1,color)
     box(x,y,z,1,1,1,gold)
    def star(x,y,z,color):
     box(x-1,y,z,3,1,1,color);box(x,y-1,z,1,3,1,color);box(x,y,z-1,1,1,2,color)
    def bow(x,y,z,color):
     box(x-2,y-1,z,2,3,1,color);box(x+1,y-1,z,2,3,1,color);box(x,y,z,1,1,1,gold)
    def scarf(color,check=False):
     box(-4,7,-5,8,2,2,color);box(2,4,-6,2,5,1,color)
     if check:
      for x in [-3,0,3]:box(x,7,-6,1,2,1,cream)
    def bag(x,y,z,color,small=False):
     w=4 if small else 6;box(x-w//2,y,z,w,4,3,color);box(x-w//2,y+3,z,w,1,3,gold);box(x,y+1,z-1,1,2,1,gold)
     box(x-w//2,y+4,z+1,1,2,1,brown);box(x+w//2-1,y+4,z+1,1,2,1,brown);box(x-w//2,y+5,z+1,w,1,1,brown)
    def neck(color,y=7):box(-3,y,-6,6,1,1,gold);ell(0,y-2,-7,2,2,1,color)
    def brim(y,color):box(-6,y,-6,12,1,10,color);ell(0,y+2,-1,4,2,3,color)
    def basket(x,y,z,color):
     bag(x,y,z,color)
     for yy in [y+1,y+3]:box(x-3,yy,z-1,6,1,1,gold)
     for xx in [-2,0,2]:box(x+xx,y,z-1,1,4,1,brown)
    if id=='fox':
     bag(4,5,0,brown,True);box(3,9,0,2,4,2,cream);box(3,12,0,2,1,2,gold)
    elif id=='dog':scarf(rgb('bc6850'),True);box(-1,5,-6,3,1,1,cream)
    elif id=='cat':neck(pink);box(0,5,-8,3,1,1,gold);box(2,4,-8,1,3,1,gold)
    elif id=='capybara':ell(0,16,-4,2.5,2.5,2.5,rgb('eca244'));box(0,18,-4,3,1,1,green)
    elif id=='hyrax':
     bag(0,6,3,green)
     for x in [-2,1]:ell(x,11,4,1,3,1,rgb('adc279'))
    elif id=='red_panda':
     bag(0,7,4,rgb('b6bb78'));box(-3,9,3,6,1,1,brown);box(-1,8,7,2,2,1,green)
    elif id=='otter':neck(rgb('ead1ae'),6);box(-2,4,-8,4,1,1,cream)
    elif id=='ferret':bag(4,4,-1,rgb('947853'),True);box(4,6,-2,1,1,1,blue)
    elif id=='hedgehog':brim(12,rgb('c87f5c'));box(-2,14,-3,2,1,1,cream);box(2,14,0,2,1,1,cream)
    elif id=='chinchilla':bow(-5,17,-5,purple)
    elif id=='hamster':bag(4,3,-2,rgb('b79354'),True);ell(4,8,-1,1,2,1,cream)
    elif id=='guinea_pig':box(-3,4,-10,6,4,1,cream);box(0,5,-11,1,3,1,rgb('db9756'));box(-1,8,-11,3,1,1,green)
    elif id=='raccoon':
     ell(0,9,4,4,3,3,green);bow(0,12,3,green)
     for x in [-3,0,3]:box(x,8,1,1,3,1,cream)
    elif id=='squirrel':brim(17,rgb('b18b54'));box(-1,20,-1,2,1,2,brown)
    elif id=='quokka':flower(-5,15,-6,cream)
    elif id=='koala':
     for x in [-4,-2,0,2,4]:ell(x,15,-5,1.5,1,1.5,green)
    elif id=='meerkat':
     box(-4,17,-4,3,2,1,gold);box(1,17,-4,3,2,1,gold);box(-1,18,-4,2,1,1,brown);box(-3,17,-5,1,1,1,blue);box(2,17,-5,1,1,1,blue)
    elif id=='penguin':scarf(rgb('a96262'));box(2,3,-6,2,1,1,cream)
    elif id=='duck':brim(15,rgb('dec48c'));flower(4,17,-3,pink)
    elif id=='seal':star(-3,10,-7,rgb('d7a275'))
    elif id=='sprout':bag(4,3,0,blue,True);box(6,5,-1,3,1,1,blue);box(8,5,-2,1,2,1,blue)
    elif id=='mushroom':scarf(rgb('c98267'),True)
    elif id=='acorn':bow(0,5,-5,green)
    elif id=='moss_golem':
     box(-3,8,3,6,4,3,rgb('bb8966'));ell(0,13,4,3,2,2,green);flower(0,15,4,pink)
    elif id=='flowerbud':neck(pink,6);box(0,3,-8,1,1,1,cream)
    elif id=='crystal':box(-5,0,-4,10,1,9,gold);box(-4,1,-4,1,2,1,gold);box(3,1,-4,1,2,1,gold)
    elif id=='pebble':brim(15,rgb('a3875d'));box(-1,17,-5,2,2,1,gold)
    elif id=='amber':ell(2,6,-3,2,1,1,gold);box(1,5,-4,1,3,1,gold)
    elif id=='pearl':bow(-4,5,-3,rgb('d89292'));box(-5,4,-3,1,4,1,pink)
    elif id=='droplet':box(6,2,0,1,13,1,brown);ell(6,15,0,6,1.5,5,green)
    elif id=='star_slime':
     ell(-2,11,0,3,3,1,gold)
     for key in list(v):
      x,y,z,part=key
      if part=='accessory' and x>=-2*S and y>=11*S:del v[key]
    elif id=='cloud':bag(5,3,0,rgb('a77955'),True);box(5,5,-1,2,1,1,cream)
    elif id=='whirlwind':scarf(blue);box(4,8,-6,1,4,1,brown);star(4,12,-6,pink)
    elif id=='baby_dragon':scarf(rgb('be7b6e'));box(2,4,-7,1,2,1,gold)
    elif id=='griffin':neck(gold,8);box(1,5,-8,1,3,1,cream)
    elif id=='unicorn':star(-4,15,-5,gold)
    elif id=='phoenix':box(-4,1,-3,3,1,4,gold);box(-3,0,-4,1,2,1,gold)
    elif id=='goblin':box(-4,2,4,8,7,1,purple);star(0,7,-5,cream)
    elif id=='book':box(2,4,-5,1,1,8,gold);box(1,4,-7,3,1,2,gold);box(2,4,-8,1,1,1,gold)
    elif id=='clockwork':bag(5,2,0,rgb('806743'),True);box(4,6,1,1,3,1,cream);box(3,8,1,3,1,1,cream)
    elif id.endswith('_fairy'):
     if season=='SPRING':box(6,3,0,1,16,1,gold);ell(6,19,0,6,1.5,5,pink);flower(6,20,0,cream)
     elif season=='SUMMER':ell(5,4,-1,2.5,2,1.5,cream);box(4,6,-1,3,1,1,gold);box(5,3,-3,1,2,1,blue)
     elif season=='AUTUMN':ell(5,4,-1,2,2.5,2,rgb('dbb565'));ell(5,6,-1,2.5,1,2.5,brown);box(5,7,-1,1,2,1,brown)
     else:box(-4,1,3,8,7,1,blue);star(0,7,-4,cream)
    elif id.endswith('_dragon'):
     if season=='SPRING':bag(4,5,0,rgb('a17e58'),True);flower(4,7,-1,pink)
     elif season=='SUMMER':neck(cream,8);box(-2,6,-8,4,1,1,blue)
     elif season=='AUTUMN':basket(0,7,4,rgb('b48951'));ell(0,12,5,2,2,1,rgb('d29850'))
     else:scarf(rgb('adcbd8'));star(3,5,-7,cream)
    elif id.endswith('_haetae'):
     if season=='SPRING':neck(rgb('ba787a'));flower(0,5,-8,pink)
     elif season=='SUMMER':neck(blue);box(-2,3,-7,1,2,1,blue);box(2,3,-7,1,2,1,blue)
     elif season=='AUTUMN':neck(rgb('b86f47'));ell(0,5,-8,2,1,1,gold);box(0,2,-7,1,2,1,brown)
     else:neck(rgb('dce9de'));box(-2,6,-8,4,1,1,blue)
    elif id.endswith('_skywhale'):
     if season=='SPRING':basket(0,-3,-1,rgb('ad8558'));flower(-1,1,-1,pink);flower(2,1,0,cream)
     elif season=='SUMMER':box(0,1,-8,1,5,1,gold);box(-2,1,-8,5,1,1,gold);box(-2,2,-8,1,2,1,gold);box(2,2,-8,1,2,1,gold)
     elif season=='AUTUMN':bag(7,2,0,rgb('a78950'),True);box(7,6,0,1,3,1,gold)
     else:box(0,1,11,1,5,1,gold);star(0,0,11,cream)
    elif id.endswith('_tiger'):
     if season=='SPRING':scarf(rgb('ac8786'));flower(0,7,-6,pink)
     elif season=='SUMMER':box(-5,1,-6,5,1,5,blue);box(-4,2,-7,3,1,1,cream)
     elif season=='AUTUMN':ell(-5,15,-4,2,1,1,rgb('ca915b'));box(-5,14,-5,1,3,1,brown)
     else:
      for x in [-5,5]:ell(x,14,-4,2,2,2,cream)
      box(-5,16,-3,10,1,1,blue)
    offsets={'capybara':-3.3,'hedgehog':-4,'squirrel':-5,'duck':-2,'chinchilla':-3.5,'quokka':-2,'koala':-2,'meerkat':-3,'seal':-2,'pebble':-1.5,'unicorn':-1}
    dy=round(offsets.get(id,0)*S)
    if dy:
        moved={}
        for key,value in list(v.items()):
            x,y,z,g=key
            if g=='accessory':moved[(x,y+dy,z,g)]=value;del v[key]
        v.update(moved)
    m.group='body'
