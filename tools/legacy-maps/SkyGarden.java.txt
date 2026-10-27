package dev.sunlit.journal;

import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.*;

/** Shared physical map; personal discovery, records, puzzle and permanent cosmetic unlocks. */
public final class SkyGarden {
    public static final ResourceKey<Level> DIMENSION=ResourceKey.create(Registries.DIMENSION,new ResourceLocation(SunlitJournal.MODID,"sky_citadel"));
    private static final ResourceKey<Level> LEGACY=ResourceKey.create(Registries.DIMENSION,new ResourceLocation(SunlitJournal.MODID,"sky_garden"));
    private static final ResourceKey<Level> GRAND=ResourceKey.create(Registries.DIMENSION,new ResourceLocation(SunlitJournal.MODID,"sky_garden_grand"));
    public static final BlockPos LANDING=new BlockPos(0,132,44);
    public static final BlockPos[] RECORDS={new BlockPos(-14,135,2),new BlockPos(45,132,0),new BlockPos(-45,132,0)};
    public static final BlockPos[] RUNES={new BlockPos(-3,135,-12),new BlockPos(0,135,-15),new BlockPos(3,135,-12)};
    public static final BlockPos RETURN=new BlockPos(0,132,47);
    record Placement(BlockPos pos,BlockState state) {}
    private static final Map<MinecraftServer,Build> BUILDS=new WeakHashMap<>();
    private static final class Build {
        final List<Placement> plan=plan();final Set<UUID> waiting=new HashSet<>();int index;
    }
    public static CompoundTag player(ServerPlayer p){
        var world=JournalWorld.get(p.getServer());var d=world.player(p);
        if(!d.contains("skyGarden",Tag.TAG_COMPOUND)){d.put("skyGarden",new CompoundTag());world.setDirty();}
        return d.getCompound("skyGarden");
    }
    private static void message(ServerPlayer p,String text){p.sendSystemMessage(Component.literal("[별길] "+text));}
    public static boolean inside(ServerPlayer p){return p.level().dimension().equals(DIMENSION)||p.level().dimension().equals(LEGACY)||p.level().dimension().equals(GRAND);}
    private static boolean built(MinecraftServer s){return JournalWorld.get(s).player(new UUID(0,0)).getBoolean("skyCitadelBuiltV1");}
    public static void travel(ServerPlayer p){
        if(p.isSpectator())return;
        if(inside(p)){leave(p);return;}
        ServerLevel level=p.getServer().getLevel(DIMENSION);
        if(level==null){message(p,"별길이 아직 열리지 않았어요. 서버를 완전히 재시작한 뒤 다시 시도해 주세요.");return;}
        if(!built(p.getServer())){
            var job=BUILDS.computeIfAbsent(p.getServer(),s->new Build());job.waiting.add(p.getUUID());
            message(p,"구름 사이로 길을 찾고 있어요. 잠시 후 하늘 정원으로 이동합니다.");return;
        }
        enter(p);
    }
    static void enter(ServerPlayer p){
        ServerLevel level=p.getServer().getLevel(DIMENSION);if(level==null||inside(p))return;
        var d=player(p);var back=new CompoundTag();back.putString("dimension",p.level().dimension().location().toString());
        back.putDouble("x",p.getX());back.putDouble("y",p.getY());back.putDouble("z",p.getZ());back.putFloat("yaw",p.getYRot());back.putFloat("pitch",p.getXRot());d.put("return",back);
        p.stopRiding();p.fallDistance=0;p.teleportTo(level,LANDING.getX()+.5,LANDING.getY(),LANDING.getZ()+.5,180,0);
        if(!d.getBoolean("discovered")){d.putBoolean("discovered",true);JournalWorld.grant(p,"구름 위의 손님");message(p,"잊힌 하늘 정원을 발견했어요! 성채와 바깥 회랑에 남은 기록을 찾아보세요.");}
        JournalWorld.get(p.getServer()).setDirty();
    }
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
        d.remove("return");JournalWorld.get(p.getServer()).setDirty();message(p,"별길을 따라 원래 있던 곳으로 돌아왔어요.");
    }
    public static void interact(ServerPlayer p,BlockPos pos,int type){
        if(!inside(p)||p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)>64)return;
        if(type==6){if(pos.equals(RETURN)||(p.level().dimension().equals(LEGACY)&&pos.equals(new BlockPos(0,151,14)))||(p.level().dimension().equals(GRAND)&&pos.equals(new BlockPos(0,151,31))))leave(p);return;}
        if(!p.level().dimension().equals(DIMENSION)){message(p,"더 넓은 하늘 정원으로 가는 별길이 열렸어요. 귀환한 뒤 나침반을 다시 사용해 주세요.");return;}
        if(type<0||type>5)return;
        BlockPos expected=type<3?RECORDS[type]:RUNES[type-3];if(!expected.equals(pos))return;
        var d=player(p);
        if(type<3){
            d.putInt("records",d.getInt("records")|(1<<type));
            String[] story={"달의 기록: 별이 잠들면 달이 먼저 길을 비춥니다.","잎의 기록: 달빛이 닿은 새싹은 두 번째로 깨어납니다.","별의 기록: 마지막 별빛이 모이면 정원의 문이 열립니다."};message(p,story[type]);
        }else{
            if(d.getInt("records")!=7){message(p,"아직 읽지 못한 기록이 있어요. 성채와 바깥 회랑의 빛나는 기록석을 찾아보세요.");return;}
            if(d.getBoolean("complete")){message(p,"이 정원은 이미 당신을 기억하고 있어요. 수첩의 비밀 기록에서 스킨을 적용하세요.");return;}
            int step=d.getInt("step");
            if(type-3!=step){d.putInt("step",0);message(p,"별빛이 흩어졌어요. 기록에 나온 순서를 다시 떠올려 보세요.");}
            else {d.putInt("step",++step);p.serverLevel().playSound(null,pos,SoundEvents.AMETHYST_BLOCK_CHIME,SoundSource.BLOCKS,1,.8f+step*.2f);
                if(step==3){d.putBoolean("complete",true);JournalWorld.grant(p,"하늘 정원의 수호자");message(p,"별빛의 잔상(검)과 구름결(곡괭이) 스킨을 영구 해금했어요!");}
                else message(p,"별자리가 이어졌어요. 다음 문양을 찾아보세요.");
            }
        }
        JournalWorld.get(p.getServer()).setDirty();
    }
    @SubscribeEvent public void stopped(net.minecraftforge.event.server.ServerStoppedEvent event){BUILDS.remove(event.getServer());}
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        for(var iterator=BUILDS.entrySet().iterator();iterator.hasNext();){var entry=iterator.next();var s=entry.getKey();var job=entry.getValue();var level=s.getLevel(DIMENSION);if(level==null){iterator.remove();continue;}
            for(int budget=0;budget<1800&&job.index<job.plan.size();budget++){var placement=job.plan.get(job.index++);level.setBlock(placement.pos(),placement.state(),2);}
            if(job.index==job.plan.size()){var world=JournalWorld.get(s);world.player(new UUID(0,0)).putBoolean("skyCitadelBuiltV1",true);world.setDirty();iterator.remove();for(UUID id:job.waiting){var p=s.getPlayerList().getPlayer(id);if(p!=null&&!inside(p))enter(p);}}
        }
    }
    @SubscribeEvent public void rescue(TickEvent.PlayerTickEvent event){if(event.phase==TickEvent.Phase.END&&event.player instanceof ServerPlayer p&&inside(p)&&p.getY()<(p.level().dimension().equals(DIMENSION)?80:115)){p.fallDistance=0;p.teleportTo(p.serverLevel(),.5,p.level().dimension().equals(DIMENSION)?132:151,p.level().dimension().equals(LEGACY)?11.5:p.level().dimension().equals(GRAND)?27.5:LANDING.getZ()+.5,180,0);}}
    @SubscribeEvent public void breakBlock(BlockEvent.BreakEvent e){if(e.getPlayer() instanceof ServerPlayer p&&inside(p)&&!p.isCreative())e.setCanceled(true);}
    @SubscribeEvent public void place(BlockEvent.EntityPlaceEvent e){if(e.getLevel() instanceof ServerLevel l&&(l.dimension().equals(DIMENSION)||l.dimension().equals(LEGACY)||l.dimension().equals(GRAND))&&!(e.getEntity() instanceof net.minecraft.world.entity.player.Player p&&p.isCreative()))e.setCanceled(true);}
    @SubscribeEvent public void use(PlayerInteractEvent.RightClickBlock e){
        if((e.getLevel().dimension().equals(DIMENSION)||e.getLevel().dimension().equals(LEGACY)||e.getLevel().dimension().equals(GRAND))&&!e.getEntity().isCreative()&&!SkyContent.BLOCKS.getEntries().stream().anyMatch(b->e.getLevel().getBlockState(e.getPos()).is(b.get())))e.setCanceled(true);
    }
    @SubscribeEvent public void damage(net.minecraftforge.event.entity.living.LivingAttackEvent e){if(e.getEntity() instanceof ServerPlayer p&&inside(p))e.setCanceled(true);}
    @SubscribeEvent public void explosion(ExplosionEvent.Detonate e){if(e.getLevel().dimension().equals(DIMENSION)||e.getLevel().dimension().equals(LEGACY)||e.getLevel().dimension().equals(GRAND))e.getAffectedBlocks().clear();}
    static void put(List<Placement> list,int x,int y,int z,Block block){list.add(new Placement(new BlockPos(x,y,z),block instanceof LeavesBlock?block.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true):block.defaultBlockState()));}
    static List<Placement> plan(){return SkyCitadelArchitecture.create();}
}
