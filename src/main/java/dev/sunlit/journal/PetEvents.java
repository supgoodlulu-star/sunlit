package dev.sunlit.journal;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.*;

public final class PetEvents {
 private static final Map<UUID,PetEntity> ACTIVE=new HashMap<>();
 private static final Map<UUID,Long> LAST_ACTIVITY=new HashMap<>();
 public static void remove(ServerPlayer p){var pet=ACTIVE.remove(p.getUUID());if(pet!=null)pet.discard();}
 public static void sync(ServerPlayer p){var pet=ACTIVE.get(p.getUUID());if(pet!=null&&pet.individual()!=null)pet.assign(p,pet.individual());}
 public static boolean teleportNear(PetEntity pet,ServerPlayer p){
  double oldX=pet.getX(),oldY=pet.getY(),oldZ=pet.getZ();
  for(int r=1;r<=3;r++)for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++)for(int dy=1;dy>=-2;dy--){
   if(dx==0&&dz==0)continue;BlockPos pos=p.blockPosition().offset(dx,dy,dz);
   if(!p.serverLevel().hasChunkAt(pos)||!p.level().getBlockState(pos.below()).isSolidRender(p.level(),pos.below())||!p.level().getFluidState(pos).isEmpty()||!p.level().getFluidState(pos.above()).isEmpty())continue;
   pet.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,p.getYRot(),0);
   if(p.level().noCollision(pet)){pet.getNavigation().stop();pet.setDeltaMovement(0,0,0);return true;}
  }pet.setPos(oldX,oldY,oldZ);return false;
 }
 public static void ensure(ServerPlayer p){
  if(!p.isAlive()||p.isSpectator()){remove(p);return;}
  var d=PetWorld.get(p).player(p.getUUID());if(!d.hasUUID("active"))return;UUID id=d.getUUID("active");if(!PetWorld.get(p).owns(p,id)){d.remove("active");PetWorld.get(p).setDirty();return;}
  var existing=ACTIVE.get(p.getUUID());if(existing!=null&&existing.isAlive()&&existing.level()==p.level())return;
  remove(p);var entity=PetContent.PET.get().create(p.serverLevel());if(entity==null)return;entity.assign(p,id);
  if(teleportNear(entity,p)&&p.serverLevel().addFreshEntity(entity))ACTIVE.put(p.getUUID(),entity);else entity.discard();
 }
 /** Inter-arrival active window limits rapid actions. Idle gaps never accrue a reward balance. */
 public static void activity(ServerPlayer p,int kind){
  if(kind<0||kind>4||p.isCreative()||p.isSpectator()||p instanceof net.minecraftforge.common.util.FakePlayer)return;
  long now=p.getServer().overworld().getGameTime();Long before=LAST_ACTIVITY.put(p.getUUID(),now);if(before==null||now<=before)return;
  // Farming/cooking rapid; fishing/breeding naturally slower. No AFK/login catch-up.
  int window=switch(kind){case 0,3->200;case 1,2->600;default->1200;};
  long ticks=Math.min(now-before,window);
  double probability=-Math.expm1(-PetConfig.TICKETS_PER_HOUR.get()*ticks/72000d);
  if(p.getRandom().nextDouble()<probability){var s=new net.minecraft.world.item.ItemStack(PetContent.TICKET.get());if(!p.getInventory().add(s))p.drop(s,false);PetWorld.message(p,"생활 활동으로 펫 뽑기권을 발견했습니다!");}
 }
 @SubscribeEvent public void tick(TickEvent.PlayerTickEvent e){if(e.phase==TickEvent.Phase.END&&e.player instanceof ServerPlayer p&&p.tickCount%20==0)ensure(p);}
 @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){remove(p);LAST_ACTIVITY.remove(p.getUUID());PetNetwork.forget(p);}}
 @SubscribeEvent public void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p){remove(p);PetNetwork.forget(p);ensure(p);}}
 @SubscribeEvent public void stopped(ServerStoppedEvent e){ACTIVE.clear();LAST_ACTIVITY.clear();PetNetwork.clear();}
 @SubscribeEvent public void commands(RegisterCommandsEvent e){
  e.getDispatcher().register(Commands.literal("sunlitpets")
   .executes(c->{PetNetwork.send(c.getSource().getPlayerOrException(),"vault",true);return 1;})
   .then(Commands.literal("recall").executes(c->{var p=c.getSource().getPlayerOrException();PetWorld.get(p).recall(p);return 1;}))
   .then(Commands.literal("give").requires(s->s.hasPermission(2)).then(Commands.argument("species",StringArgumentType.word()).suggests((c,b)->{for(var s:PetCatalog.ALL)b.suggest(s.id());return b.buildFuture();}).executes(c->{var p=c.getSource().getPlayerOrException();String id=StringArgumentType.getString(c,"species");if(PetCatalog.find(id).isEmpty()||p.getInventory().getFreeSlot()<0)return 0;p.getInventory().add(PetWorld.get(p).mint(id));return 1;})))
   .then(Commands.literal("place").requires(s->s.hasPermission(2)).executes(c->{var p=c.getSource().getPlayerOrException();var a=p.blockPosition().relative(p.getDirection(),2);var b=a.relative(p.getDirection().getClockWise());
    if(!p.level().getBlockState(a).isAir()||!p.level().getBlockState(b).isAir()||!p.level().getBlockState(a.above()).isAir()||!p.level().getBlockState(b.above()).isAir()){PetWorld.message(p,"앞쪽 두 칸과 그 위를 비워 주세요.");return 0;}
    p.level().setBlockAndUpdate(a,PetContent.VENDING.get().defaultBlockState());p.level().setBlockAndUpdate(b,PetContent.SHOP.get().defaultBlockState());PetWorld.message(p,"펫 자판기와 장식 가게를 설치했습니다.");return 1;})));
 }
}
