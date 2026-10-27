package dev.sunlit.journal;

import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;

/** Retired map save compatibility only. No generation, entry, puzzle or rewards. */
public final class SkyGarden {
    public static final ResourceKey<Level> DIMENSION=ResourceKey.create(Registries.DIMENSION,new ResourceLocation(SunlitJournal.MODID,"sky_citadel"));
    private static final ResourceKey<Level> LEGACY=ResourceKey.create(Registries.DIMENSION,new ResourceLocation(SunlitJournal.MODID,"sky_garden"));
    private static final ResourceKey<Level> GRAND=ResourceKey.create(Registries.DIMENSION,new ResourceLocation(SunlitJournal.MODID,"sky_garden_grand"));
    // Coordinates and type remain for archived source compatibility; no plan is generated.
    public static final BlockPos LANDING=new BlockPos(0,132,44);
    public static final BlockPos[] RECORDS={new BlockPos(-14,135,2),new BlockPos(45,132,0),new BlockPos(-45,132,0)};
    public static final BlockPos[] RUNES={new BlockPos(-3,135,-12),new BlockPos(0,135,-15),new BlockPos(3,135,-12)};
    public static final BlockPos RETURN=new BlockPos(0,132,47);
    record Placement(BlockPos pos,BlockState state) {}
    public static CompoundTag player(ServerPlayer p){
        var world=JournalWorld.get(p.getServer());var data=world.player(p);
        if(!data.contains("skyGarden",Tag.TAG_COMPOUND)){data.put("skyGarden",new CompoundTag());world.setDirty();}
        return data.getCompound("skyGarden");
    }
    private static void message(ServerPlayer p,String text){p.sendSystemMessage(Component.literal(text));}
    public static boolean inside(ServerPlayer p){return p.level().dimension().equals(DIMENSION)||p.level().dimension().equals(LEGACY)||p.level().dimension().equals(GRAND);}
    public static void travel(ServerPlayer p){if(inside(p))leave(p);}
    static void enter(ServerPlayer p){}
    public static void interact(ServerPlayer p,BlockPos pos,int kind){if(kind==6)leave(p);}
    public static void leave(ServerPlayer p){
        if(!inside(p))return;
        var d=player(p);var back=d.getCompound("return");ServerLevel target=p.getServer().overworld();
        var id=ResourceLocation.tryParse(back.getString("dimension"));
        if(id!=null){var requested=p.getServer().getLevel(ResourceKey.create(Registries.DIMENSION,id));if(requested!=null&&!requested.dimension().equals(DIMENSION)&&!requested.dimension().equals(LEGACY)&&!requested.dimension().equals(GRAND))target=requested;}
        double x=back.contains("x")?back.getDouble("x"):target.getSharedSpawnPos().getX()+.5;
        double z=back.contains("z")?back.getDouble("z"):target.getSharedSpawnPos().getZ()+.5;
        int y=back.contains("y")?(int)Math.floor(back.getDouble("y")):target.getSharedSpawnPos().getY();
        BlockPos pos=BlockPos.containing(x,y,z);target.getChunkAt(pos);
        if(!target.getBlockState(pos).getCollisionShape(target,pos).isEmpty()||!target.getBlockState(pos.above()).getCollisionShape(target,pos.above()).isEmpty()||target.getBlockState(pos.below()).isAir())y=target.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,pos.getX(),pos.getZ())+1;
        p.stopRiding();p.fallDistance=0;p.teleportTo(target,x,y,z,back.getFloat("yaw"),back.getFloat("pitch"));
        d.remove("return");JournalWorld.get(p.getServer()).setDirty();message(p,"이전 버전의 구역에서 원래 있던 곳으로 돌아왔어요.");
    }
}
