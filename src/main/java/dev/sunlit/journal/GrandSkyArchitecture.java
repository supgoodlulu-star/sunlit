package dev.sunlit.journal;

import net.minecraft.core.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Four districts, full building interiors and guarded five-block-wide aerial promenades. */
final class GrandSkyArchitecture {
    private final List<SkyGarden.Placement> blocks=new ArrayList<>();
    private void p(int x,int y,int z,Block b){SkyGarden.put(blocks,x,y,z,b);}
    private void state(int x,int y,int z,BlockState s){blocks.add(new SkyGarden.Placement(new BlockPos(x,y,z),s));}
    private void box(int ax,int ay,int az,int bx,int by,int bz,Block b){for(int x=ax;x<=bx;x++)for(int y=ay;y<=by;y++)for(int z=az;z<=bz;z++)p(x,y,z,b);}
    private void disk(int cx,int y,int cz,int r,Block b){for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++)if(x*x+z*z<=r*r)p(cx+x,y,cz+z,b);}
    private void island(int cx,int y,int cz,int r){
        for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++){
            double d=Math.sqrt(x*x+z*z),edge=r+Math.sin(x*.32+z*.21)*1.4;if(d>edge)continue;
            int depth=3+(int)(Math.pow(Math.max(0,1-d/(r+2)),.8)*(r*.65));
            for(int k=0;k<=depth;k++)p(cx+x,y-k,cz+z,k==0?Blocks.GRASS_BLOCK:k<3?Blocks.DIRT:(x*7+z*3+k)%13==0?Blocks.CALCITE:Blocks.STONE);
        }
    }
    private void lamp(int x,int y,int z){box(x,y,z,x,y+2,z,Blocks.QUARTZ_PILLAR);p(x,y+3,z,Blocks.SEA_LANTERN);p(x,y+4,z,Blocks.CHERRY_SLAB);}
    private void tree(int x,int y,int z,int height,int radius){
        box(x,y,z,x,y+height,z,Blocks.CHERRY_LOG);
        for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++)for(int dy=-2;dy<=3;dy++)if(dx*dx+dz*dz+dy*dy*3<radius*radius)p(x+dx,y+height+dy,z+dz,Blocks.CHERRY_LEAVES);
    }
    private void bridge(int ax,int ay,int az,int bx,int by,int bz){
        int n=Math.max(Math.abs(bx-ax),Math.abs(bz-az));boolean alongX=Math.abs(bx-ax)>=Math.abs(bz-az);int last=ay;
        Direction up=alongX?(bx>ax?Direction.EAST:Direction.WEST):(bz>az?Direction.SOUTH:Direction.NORTH);if(by<ay)up=up.getOpposite();
        for(int i=0;i<=n;i++){
            int x=Math.round(ax+(bx-ax)*(float)i/n),y=Math.round(ay+(by-ay)*(float)i/n),z=Math.round(az+(bz-az)*(float)i/n);
            for(int w=-2;w<=2;w++){
                int px=x+(alongX?0:w),pz=z+(alongX?w:0);
                if(y!=last)state(px,y,pz,Blocks.CHERRY_STAIRS.defaultBlockState().setValue(StairBlock.FACING,up));else p(px,y,pz,Blocks.CHERRY_PLANKS);
            }
            for(int w:new int[]{-3,3}){int px=x+(alongX?0:w),pz=z+(alongX?w:0);p(px,y,pz,Blocks.STRIPPED_CHERRY_LOG);p(px,y+1,pz,Blocks.CHERRY_FENCE);if(i%8==0)lamp(px,y+1,pz);}
            last=y;
        }
    }
    private void room(int x,int y,int z,int w,int d,int h,boolean library){
        box(x,y,z,x+w,y,z+d,Blocks.SMOOTH_QUARTZ);
        for(int k=1;k<=h;k++)for(int i=0;i<=w;i++)for(int j=0;j<=d;j++)if(i==0||j==0||i==w||j==d){
            boolean post=(i==0||i==w)?j%6==0:i%6==0;
            p(x+i,y+k,z+j,post?Blocks.QUARTZ_PILLAR:k>=2&&k<=4?Blocks.LIGHT_BLUE_STAINED_GLASS:Blocks.CALCITE);
        }
        box(x+w/2-2,y+1,z+d,x+w/2+2,y+5,z+d,Blocks.AIR);
        // Stepped, overhanging cherry roof with a continuous ridge.
        for(int k=0;k<=w/2+1;k++)box(x-1+k,y+h+1+k,z-2,x+w+1-k,y+h+1+k,z+d+2,Blocks.CHERRY_PLANKS);
        if(library){
            box(x+1,y+7,z+1,x+w-1,y+7,z+d-1,Blocks.CHERRY_PLANKS);
            box(x+w/2-3,y+7,z+5,x+w/2+3,y+7,z+d-4,Blocks.AIR); // two-storey reading atrium
            for(int i=3;i<w-2;i+=5)for(int k=1;k<=3;k++){box(x+i,y+k,z+2,x+i+2,y+k,z+3,Blocks.BOOKSHELF);box(x+i,y+7+k,z+2,x+i+2,y+7+k,z+3,Blocks.BOOKSHELF);}
            for(int i=0;i<8;i++)for(int j=0;j<3;j++)state(x+2+i,y+1+i,z+d-5-j,Blocks.CHERRY_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.EAST));
            for(int i=5;i<w;i+=6){p(x+i,y+1,z+d-7,Blocks.LECTERN);p(x+i,y+9,z+d-7,Blocks.SEA_LANTERN);}
        }else{
            box(x+2,y+1,z+2,x+w-2,y+1,z+3,Blocks.CHERRY_PLANKS);
            for(int i=3;i<w;i+=4){p(x+i,y+1,z+d-4,Blocks.CHERRY_STAIRS);p(x+i,y+1,z+d-6,Blocks.CHERRY_FENCE);p(x+i,y+2,z+d-6,Blocks.CHERRY_PRESSURE_PLATE);}
        }
        for(int i=3;i<w;i+=6)p(x+i,y+h-1,z+d/2,Blocks.SEA_LANTERN);
    }
    private void observatory(){
        disk(0,150,-36,16,Blocks.SMOOTH_QUARTZ);
        for(int y=151;y<=165;y++)for(int x=-15;x<=15;x++)for(int z=-15;z<=15;z++){
            double r=Math.sqrt(x*x+z*z);if(r<13.6||r>15)continue;
            if(z>=12&&Math.abs(x)<=3&&y<158)continue;
            boolean pillar=Math.floorMod(x,7)<=1||Math.floorMod(z,7)<=1;
            p(x,y,z-36,pillar?Blocks.QUARTZ_PILLAR:Blocks.LIGHT_BLUE_STAINED_GLASS);
        }
        for(int h=0;h<=13;h++){
            int r=(int)Math.round(15*Math.cos(h/13.0*Math.PI/2));
            for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++)if(x*x+z*z<=r*r&&(x*x+z*z>=Math.max(0,r-2)*Math.max(0,r-2)||h==13))p(x,166+h,z-36,(x%5==0||z%5==0)?Blocks.QUARTZ_BLOCK:Blocks.LIGHT_BLUE_STAINED_GLASS);
        }
        for(int z=-24;z<=-19;z++)box(-4,150,z,4,150,z,Blocks.SMOOTH_QUARTZ);
        // Elevated telescope and a star-shaped ceiling lamp.
        box(7,151,-40,9,153,-38,Blocks.QUARTZ_PILLAR);box(7,154,-41,9,155,-33,Blocks.DEEPSLATE_TILES);
        p(8,154,-32,Blocks.SEA_LANTERN);
        for(int x=-2;x<=2;x++){p(x,171,-36,Blocks.SEA_LANTERN);p(0,171,-36+x,Blocks.SEA_LANTERN);}
    }
    private void greenhouse(){
        for(int x=78;x<=106;x++)for(int z=-18;z<=10;z++)p(x,153,z,Blocks.SMOOTH_QUARTZ);
        for(int y=154;y<=165;y++)for(int x=78;x<=106;x++)for(int z=-18;z<=10;z++)if(x==78||x==106||z==-18||z==10)p(x,y,z,((x==78||x==106)?Math.floorMod(z+18,7)==0:Math.floorMod(x-78,7)==0)?Blocks.QUARTZ_PILLAR:Blocks.GLASS);
        box(89,154,10,95,158,10,Blocks.AIR);
        for(int k=0;k<=14;k++){
            box(78+k,166+k,-18,78+k,166+k,10,Blocks.GLASS);
            box(106-k,166+k,-18,106-k,166+k,10,Blocks.GLASS);
            for(int z:new int[]{-18,10})box(78+k,166+k,z,106-k,166+k,z,Blocks.GLASS);
        }
        for(int x:new int[]{82,99})for(int z=-14;z<=4;z++)for(int w=0;w<4;w++){
            p(x+w,153,z,Blocks.DIRT);p(x+w,154,z,(z+w)%3==0?Blocks.ALLIUM:(z+w)%3==1?Blocks.FERN:Blocks.AZURE_BLUET);
        }
        tree(86,154,-8,7,4);tree(101,154,-10,8,4);
        for(int z=-16;z<=8;z++)for(int x=90;x<=94;x++)p(x,153,z,Blocks.CHERRY_PLANKS);
        for(int x:new int[]{80,104})for(int z:new int[]{-16,8})lamp(x,154,z);
    }
    private void ruins(){
        disk(-88,155,28,17,Blocks.MOSSY_STONE_BRICKS);
        for(int x:new int[]{-100,-94,-82,-76})for(int z:new int[]{17,39})for(int k=0;k<(x+z)%5+8;k++)p(x,156+k,z,Blocks.STONE_BRICKS);
        box(-100,165,17,-76,165,17,Blocks.MOSSY_STONE_BRICKS);
        box(-100,156,17,-100,165,17,Blocks.STONE_BRICKS);box(-76,156,17,-76,165,17,Blocks.MOSSY_STONE_BRICKS);
        for(int z=20;z<=36;z++)for(int x=-91;x<=-85;x++)p(x,155,z,Blocks.CALCITE);
        for(int x=-104;x<=-72;x+=8)if(x!=-88)lamp(x,156,28);
        for(int x=-99;x<-94;x++)for(int z=32;z<36;z++)p(x,156,z,Blocks.MOSS_BLOCK);
    }
    private void tower(){
        disk(0,157,-100,17,Blocks.CALCITE);
        for(int y=158;y<=188;y++)for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)if(Math.abs(x)==6||Math.abs(z)==6)p(x,y,z-100,(x%3==0||z%3==0)?Blocks.QUARTZ_PILLAR:Blocks.LIGHT_BLUE_STAINED_GLASS);
        box(-2,158,-94,2,163,-94,Blocks.AIR);
        for(int y:new int[]{167,177,187})box(-8,y,-108,8,y,-92,Blocks.QUARTZ_SLAB);
        for(int k=0;k<9;k++)box(-8+k,189+k,-108+k,8-k,189+k,-92-k,Blocks.DARK_PRISMARINE);
        for(int y:new int[]{168,178,188})box(-2,y,-94,2,y+3,-94,Blocks.AIR);
        // A walkable internal stair winding up each wall, with intermediate landings.
        for(int lap=0;lap<3;lap++)for(int i=0;i<10;i++)state(-4+i,158+lap*10+i,-104,Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.EAST));
        for(int x:new int[]{-13,13})for(int z:new int[]{-113,-87})lamp(x,158,z);
    }
    private List<SkyGarden.Placement> build(){
        island(0,150,0,54);island(92,153,-4,25);island(-88,155,28,25);island(0,157,-100,23);
        bridge(47,150,-4,71,153,-4);bridge(-44,150,20,-69,155,28);bridge(0,150,-49,0,157,-80);
        // Broad avenues connecting the arrival plaza and all four destinations.
        for(int x=-47;x<=47;x++)for(int z=-6;z<=-2;z++)p(x,150,z,Blocks.SMOOTH_SANDSTONE);
        for(int z=-49;z<=38;z++)for(int x=-2;x<=2;x++)p(x,150,z,Blocks.SMOOTH_SANDSTONE);
        for(int x=-44;x<=-2;x++)for(int z=18;z<=22;z++)p(x,150,z,Blocks.SMOOTH_SANDSTONE);
        disk(0,150,19,12,Blocks.CALCITE);
        // Fountain court, with a continuous retaining rim.
        for(int x=-9;x<=9;x++)for(int z=-9;z<=9;z++)if(x*x+z*z<=81){p(x,149,z,Blocks.PRISMARINE_BRICKS);p(x,150,z,x*x+z*z>=56?Blocks.SMOOTH_QUARTZ:Blocks.WATER);}
        box(-1,150,-1,1,154,1,Blocks.QUARTZ_PILLAR);p(0,155,0,Blocks.SEA_LANTERN);
        room(-41,150,-21,24,25,12,true);room(20,150,17,19,17,7,false);
        observatory();greenhouse();ruins();tower();
        for(int x:new int[]{-18,12})for(int z:new int[]{20,43})tree(x,151,z,10,7);
        tree(38,151,-22,12,8);tree(-40,151,34,9,6);
        for(int x=-38;x<=38;x+=10)for(int z:new int[]{-8,8,25})if(x*x+z*z<45*45&&!(x<-14&&z<8)&&!(x>18&&z>14))lamp(x,151,z);
        for(int z=5;z<=20;z++)for(int x=-31;x<=-27;x++)p(x,150,z,Blocks.SMOOTH_SANDSTONE);
        for(int x=-31;x<=40;x++)for(int z=36;z<=39;z++)p(x,150,z,Blocks.SMOOTH_SANDSTONE);
        for(int x=27;x<=32;x++)for(int z=34;z<=36;z++)p(x,150,z,Blocks.SMOOTH_SANDSTONE);
        for(int x=-32;x<=32;x+=8)for(int z=41;z<=43;z+=2)if(Math.abs(x)>5&&x*x+z*z<49*49)p(x,151,z,(x+z)%3==0?Blocks.ALLIUM:Blocks.AZURE_BLUET);
        // Hedge-lined terraces and sheltered benches in the public garden.
        for(int x=-12;x<=12;x++)for(int z:new int[]{33,44})if(Math.abs(x)>3)p(x,151,z,Blocks.AZALEA_LEAVES);
        for(int x:new int[]{-11,7})for(int w=0;w<5;w++)p(x+w,151,35,Blocks.CHERRY_STAIRS);
        for(int i=0;i<3;i++){var q=SkyGarden.RECORDS[i];p(q.getX(),q.getY(),q.getZ(),SkyContent.RELICS[i].get());q=SkyGarden.RUNES[i];p(q.getX(),q.getY()-1,q.getZ(),Blocks.QUARTZ_PILLAR);p(q.getX(),q.getY(),q.getZ(),SkyContent.RELICS[i+3].get());}
        var q=SkyGarden.RETURN;p(q.getX(),q.getY(),q.getZ(),SkyContent.RELICS[6].get());
        return blocks;
    }
    static List<SkyGarden.Placement> create(){return new GrandSkyArchitecture().build();}
}
