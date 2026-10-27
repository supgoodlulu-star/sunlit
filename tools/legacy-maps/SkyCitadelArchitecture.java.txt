package dev.sunlit.journal;

import net.minecraft.core.*;
import net.minecraft.world.level.block.*;
import java.util.*;

/** Reference-led white sky citadel: recessed lancets, stepped spires, flying buttresses and blue orbs. */
final class SkyCitadelArchitecture {
    private final List<SkyGarden.Placement> plan=new ArrayList<>();
    private void p(int x,int y,int z,Block b){SkyGarden.put(plan,x,y,z,b);}
    private void box(int ax,int ay,int az,int bx,int by,int bz,Block b){for(int x=ax;x<=bx;x++)for(int y=ay;y<=by;y++)for(int z=az;z<=bz;z++)p(x,y,z,b);}
    private void disk(int cx,int y,int cz,int r,Block b){for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++)if(x*x+z*z<=r*r)p(cx+x,y,cz+z,b);}
    private void ring(int cx,int y,int cz,int r,int thickness,Block b){for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++)if(x*x+z*z<=r*r&&x*x+z*z>=(r-thickness)*(r-thickness))p(cx+x,y,cz+z,b);}
    private void spire(int x,int y,int z,int radius,int height){
        for(int h=0;h<height;h++){
            int r=Math.max(0,(int)Math.ceil(radius*Math.pow(1-h/(double)height,1.65)));
            disk(x,y+h,z,r,(h%7==0)?Blocks.QUARTZ_BRICKS:Blocks.SMOOTH_QUARTZ);
            if(h%7==0&&r>1){p(x+r+1,y+h,z,Blocks.QUARTZ_STAIRS);p(x-r-1,y+h,z,Blocks.QUARTZ_STAIRS);p(x,y+h,z+r+1,Blocks.QUARTZ_STAIRS);p(x,y+h,z-r-1,Blocks.QUARTZ_STAIRS);}
        }
        box(x,y+height,z,x,y+height+3,z,Blocks.END_ROD);p(x,y+height+4,z,Blocks.SEA_LANTERN);
    }
    private void tower(int cx,int floor,int cz,int r,int height){
        for(int y=floor-4;y<floor;y++)disk(cx,y,cz,r+1,Blocks.CALCITE);
        disk(cx,floor,cz,r+2,Blocks.QUARTZ_BRICKS);
        for(int h=1;h<=height;h++){
            int radius=h<4?r+1:r;
            for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
                int d=x*x+z*z;if(d>radius*radius||d<(radius-1)*(radius-1))continue;
                boolean window=(Math.abs(x)<=1||Math.abs(z)<=1)&&h%13>=4&&h%13<=10;
                p(cx+x,floor+h,cz+z,window?Blocks.BLUE_STAINED_GLASS:(h%9==0?Blocks.QUARTZ_BRICKS:Blocks.SMOOTH_QUARTZ));
            }
            if(h%9==0){ring(cx,floor+h,cz,r+1,2,Blocks.SMOOTH_QUARTZ);for(int q:new int[]{-r,r}){p(cx+q,floor+h+1,cz,Blocks.DIORITE_WALL);p(cx,floor+h+1,cz+q,Blocks.DIORITE_WALL);}}
            if(h%13==1)disk(cx,floor+h-1,cz,r-1,Blocks.CHISELED_QUARTZ_BLOCK);
        }
        // Cardinal ribs and a projecting crown, each with four little pinnacles.
        for(int h=0;h<height;h++)for(int q:new int[]{-r,r}){p(cx+q,floor+h,cz,Blocks.QUARTZ_PILLAR);p(cx,floor+h,cz+q,Blocks.QUARTZ_PILLAR);}
        ring(cx,floor+height,cz,r+2,3,Blocks.SMOOTH_QUARTZ);
        for(int q:new int[]{-r-1,r+1}){spire(cx+q,floor+height,cz,1,8);spire(cx,floor+height,cz+q,1,8);}
        spire(cx,floor+height+1,cz,r+1,24);
        box(cx-1,floor+1,cz+r-1,cx+1,floor+5,cz+r+1,Blocks.AIR);
        // The first platform is accessible through the base door; tall shafts remain a silhouette feature.
        p(cx,floor+1,cz,Blocks.SEA_LANTERN);
    }
    private void pitchedRoof(int x,int y,int z,int halfWidth,int length){
        for(int h=0;h<=halfWidth*2;h++){
            int inset=h/2;
            for(int side:new int[]{-1,1})box(x+side*(halfWidth-inset),y+h,z,x+side*(halfWidth-inset),y+h,z+length,Blocks.SMOOTH_QUARTZ);
            for(int end:new int[]{z,z+length})box(x-halfWidth+inset,y+h,end,x+halfWidth-inset,y+h,end,Blocks.CALCITE);
            if(h%4==0)for(int q=z;q<=z+length;q+=8){p(x-halfWidth+inset-1,y+h,q,Blocks.QUARTZ_STAIRS);p(x+halfWidth-inset+1,y+h,q,Blocks.QUARTZ_STAIRS);}
        }
        for(int q=z;q<=z+length;q+=7)spire(x,y+halfWidth*2+1,q,1,7);
    }
    private void hall(){
        box(-20,130,-27,20,134,21,Blocks.QUARTZ_BRICKS);
        box(-18,134,-25,18,134,19,Blocks.SMOOTH_QUARTZ);
        for(int y=135;y<=172;y++)for(int x=-18;x<=18;x++)for(int z=-25;z<=19;z++)if(Math.abs(x)==18||z==-25||z==19){
            boolean rib=(Math.abs(x)==18?Math.floorMod(z+25,8)<2:Math.floorMod(x+18,9)<2);
            p(x,y,z,rib?Blocks.QUARTZ_PILLAR:Blocks.CALCITE);
        }
        // Recessed two-block-deep windows, pointed white arches and transoms.
        for(int z=-19;z<=13;z+=8)for(int side:new int[]{-1,1}){
            int x=side*18;
            for(int y=141;y<=164;y++)for(int dz=-2;dz<=2;dz++)if(y<=161||Math.abs(dz)<=164-y)p(x,y,z+dz,Blocks.BLUE_STAINED_GLASS);
            for(int y=140;y<=162;y++){p(x+side,y,z-3,Blocks.QUARTZ_PILLAR);p(x+side,y,z+3,Blocks.QUARTZ_PILLAR);}
            for(int k=0;k<4;k++){p(x+side,163+k,z-3+k,Blocks.SMOOTH_QUARTZ);p(x+side,163+k,z+3-k,Blocks.SMOOTH_QUARTZ);}
            box(x-side*2,150,z-2,x-side*2,150,z+2,Blocks.CHISELED_QUARTZ_BLOCK);
        }
        for(int y:new int[]{139,156,171}){
            box(-20,y,-27,20,y,-26,Blocks.SMOOTH_QUARTZ);box(-20,y,20,20,y,21,Blocks.SMOOTH_QUARTZ);
            box(-20,y,-25,-19,y,19,Blocks.SMOOTH_QUARTZ);box(19,y,-25,20,y,19,Blocks.SMOOTH_QUARTZ);
        }
        // Flying buttresses step down toward independent outer piers.
        for(int z=-21;z<=19;z+=10)for(int side:new int[]{-1,1}){
            for(int k=0;k<7;k++)box(side*(19+k),167-k*2,z-1,side*(19+k),168-k*2,z+1,Blocks.SMOOTH_QUARTZ);
            box(side*25,134,z-1,side*25,155,z+1,Blocks.QUARTZ_PILLAR);spire(side*25,156,z,2,13);
        }
        pitchedRoof(0,173,-27,19,48);
        // South entrance chapel: lower pitched roof gives the main hall a layered silhouette.
        box(-11,134,18,11,134,31,Blocks.SMOOTH_QUARTZ);
        for(int y=135;y<=150;y++)for(int x=-11;x<=11;x++)for(int z=18;z<=31;z++)if(Math.abs(x)==11||z==31)p(x,y,z,Math.abs(x)==11&&y>=139&&y<=146?Blocks.LIGHT_BLUE_STAINED_GLASS:Blocks.SMOOTH_QUARTZ);
        pitchedRoof(0,151,18,12,14);
        box(-3,135,18,3,145,20,Blocks.AIR);box(-3,135,30,3,143,32,Blocks.AIR);
        for(int k=0;k<=4;k++)for(int x=-3+k/2;x<=3-k/2;x++)p(x,144+k,32,Blocks.CHISELED_QUARTZ_BLOCK);
        // Recessed rose windows break up the high gables; the cross and rim project outward.
        for(int end:new int[]{-27,21})for(int x=-6;x<=6;x++)for(int dy=-6;dy<=6;dy++){
            int d=x*x+dy*dy;if(d>36)continue;
            p(x,185+dy,end,d>=25||x==0||dy==0?Blocks.SMOOTH_QUARTZ:Blocks.CYAN_STAINED_GLASS);
            if(d>=25)p(x,185+dy,end+(end>0?1:-1),Blocks.CHISELED_QUARTZ_BLOCK);
        }
        for(int end:new int[]{-27,21})for(int y=197;y<=201;y++)for(int x=-1;x<=1;x++)p(x,y,end,Blocks.LIGHT_BLUE_STAINED_GLASS);
        // Broad approach steps, recessed reading alcoves and a star-lit nave.
        box(-6,131,32,6,138,36,Blocks.AIR);
        for(int k=0;k<4;k++)for(int x=-6;x<=6;x++)plan.add(new SkyGarden.Placement(new BlockPos(x,131+k,36-k),Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.NORTH)));
        for(int x:new int[]{-14,14})for(int z=-16;z<=8;z+=8){box(x-1,135,z-2,x+1,138,z-1,Blocks.BOOKSHELF);p(x,135,z+1,Blocks.LECTERN);}
        for(int z=-18;z<=10;z+=8){p(0,158,z,Blocks.SEA_LANTERN);for(int k=159;k<=171;k++)p(0,k,z,Blocks.CHAIN);}
        for(int x=-3;x<=3;x++)for(int z=-22;z<=28;z++)p(x,134,z,(x==0)?Blocks.LAPIS_BLOCK:Blocks.SMOOTH_QUARTZ);
    }
    private void orb(int cx,int cy,int cz,int r){
        for(int x=-r;x<=r;x++)for(int y=-r;y<=r;y++)for(int z=-r;z<=r;z++){
            int d=x*x+y*y+z*z;if(d>r*r||d<(r-2)*(r-2))continue;
            int pattern=Math.floorMod(x*7+y*11+z*3,17);
            p(cx+x,cy+y,cz+z,pattern<3?Blocks.BLUE_CONCRETE:pattern<6?Blocks.LIGHT_BLUE_CONCRETE:pattern==10?Blocks.SEA_LANTERN:Blocks.SNOW_BLOCK);
        }
    }
    private List<SkyGarden.Placement> build(){
        // A scalloped white floating shelf with irregular, deep mineral roots.
        for(int x=-66;x<=66;x++)for(int z=-60;z<=60;z++){
            double r=Math.sqrt(x*x+z*z*1.15),edge=61+Math.sin(x*.18)*2+Math.cos(z*.23)*2;
            if(r>edge)continue;int depth=4+(int)(Math.pow(Math.max(0,1-r/64),.7)*26)+Math.floorMod(x*13+z*7,4);
            for(int k=0;k<=depth;k++)p(x,130-k,z,k==0?Blocks.SNOW_BLOCK:k<4?Blocks.CALCITE:Math.floorMod(x+z+k,5)==0?Blocks.SMOOTH_BASALT:Blocks.DEEPSLATE);
        }
        // Moat and a continuous, dry ring promenade.
        for(int x=-46;x<=46;x++)for(int z=-46;z<=46;z++){
            double r=Math.sqrt(x*x+z*z);if(r>46)continue;
            if(r>=38&&r<=41){p(x,129,z,Blocks.PRISMARINE_BRICKS);p(x,130,z,Blocks.WATER);}
            else if(r>=42)p(x,130,z,Blocks.SMOOTH_QUARTZ);
        }
        for(int x=-47;x<=47;x++)for(int w=-3;w<=3;w++){p(x,131,w,Blocks.SMOOTH_QUARTZ);p(w,131,x,Blocks.SMOOTH_QUARTZ);}
        for(int y=131;y<=134;y++)disk(0,y,0,35,Blocks.CALCITE);
        hall();
        tower(-27,134,-24,6,57);tower(27,134,-24,6,65);
        tower(-28,134,23,6,42);tower(28,134,23,6,48);
        tower(0,172,-15,5,39);
        tower(-48,130,17,5,43);tower(48,130,-17,5,35);tower(-34,130,-44,3,25);
        // Corner balconies and traceried rails.
        for(int side:new int[]{-1,1}){
            for(int z=-30;z<=29;z++)for(int x=21;x<=31;x++)p(side*x,134,z,Blocks.SMOOTH_QUARTZ);
            for(int z=-29;z<=28;z+=2)p(side*31,135,z,Blocks.DIORITE_WALL);
            for(int z=-15;z<=15;z+=10)spire(side*31,136,z,1,8);
        }
        // Thin suspended waterfalls, each with a contained spillway at its lip.
        for(int[] q:new int[][]{{-55,0},{48,32},{-24,51},{31,-48}}){
            for(int w=-2;w<=2;w++){p(q[0]+w,130,q[1],Blocks.SMOOTH_QUARTZ);p(q[0]+w,131,q[1],Blocks.SMOOTH_QUARTZ);}
            for(int y=62;y<130;y++)plan.add(new SkyGarden.Placement(new BlockPos(q[0],y,q[1]),Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL,8)));
            p(q[0],130,q[1],Blocks.WATER);
        }
        orb(-18,230,-23,8);orb(31,223,-24,6);orb(-42,205,10,5);orb(45,235,-36,5);orb(-5,242,-15,4);
        // Floating white crystal fragments around the orbs.
        for(int i=0;i<12;i++){int x=-28+i*5,y=216+(i%4)*4,z=-28+(i%3)*5;p(x,y,z,Blocks.END_ROD);p(x,y+1,z,Blocks.SNOW_BLOCK);}
        for(int i=0;i<3;i++){var q=SkyGarden.RECORDS[i];p(q.getX(),q.getY()-1,q.getZ(),Blocks.SMOOTH_QUARTZ);p(q.getX(),q.getY(),q.getZ(),SkyContent.RELICS[i].get());q=SkyGarden.RUNES[i];p(q.getX(),q.getY()-1,q.getZ(),Blocks.QUARTZ_PILLAR);p(q.getX(),q.getY(),q.getZ(),SkyContent.RELICS[i+3].get());}
        var q=SkyGarden.RETURN;p(q.getX(),q.getY(),q.getZ(),SkyContent.RELICS[6].get());
        return plan;
    }
    static List<SkyGarden.Placement> create(){return new SkyCitadelArchitecture().build();}
}
