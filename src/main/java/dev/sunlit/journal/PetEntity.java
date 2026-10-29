package dev.sunlit.journal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import java.util.*;

public final class PetEntity extends PathfinderMob {
 private static final EntityDataAccessor<String> SPECIES=SynchedEntityData.defineId(PetEntity.class,EntityDataSerializers.STRING);
 private static final EntityDataAccessor<Boolean> ACCESSORY=SynchedEntityData.defineId(PetEntity.class,EntityDataSerializers.BOOLEAN);
 private static final EntityDataAccessor<Integer> PETTING=SynchedEntityData.defineId(PetEntity.class,EntityDataSerializers.INT);
 private UUID owner,individual;
 private int stuckTicks;
 public PetEntity(EntityType<? extends PathfinderMob> type,Level level){super(type,level);setPersistenceRequired();setInvulnerable(true);}
 @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(SPECIES,"fox");entityData.define(ACCESSORY,false);entityData.define(PETTING,0);}
 public String species(){return entityData.get(SPECIES);}
 public boolean accessory(){return entityData.get(ACCESSORY);}
 public boolean petting(){return entityData.get(PETTING)>0;}
 public void assign(ServerPlayer p,UUID id){owner=p.getUUID();individual=id;var r=PetWorld.get(p).record(id);entityData.set(SPECIES,r.getString("species"));entityData.set(ACCESSORY,r.getBoolean("accessory")&&PetWorld.get(p).unlocked(p,species()));}
 public UUID owner(){return owner;}
 public UUID individual(){return individual;}
 @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
 @Override public boolean shouldBeSaved(){return false;}
 @Override public boolean isIgnoringBlockTriggers(){return true;}
 @Override public boolean canTrample(net.minecraft.world.level.block.state.BlockState state,net.minecraft.core.BlockPos pos,float distance){return false;}
 @Override public boolean canBeHitByProjectile(){return false;}
 @Override public boolean isPushable(){return false;}
 @Override protected void doPush(Entity other){}
 @Override public boolean canBeCollidedWith(){return false;}
 @Override public boolean hurt(DamageSource source,float amount){return false;}
 @Override public boolean canBeLeashed(Player p){return false;}
 @Override public boolean canChangeDimensions(){return false;}
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);}
 @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);discard();}
 @Override public InteractionResult mobInteract(Player p,InteractionHand hand){
  if(owner!=null&&p.getUUID().equals(owner)){if(!level().isClientSide&&entityData.get(PETTING)==0){entityData.set(PETTING,35);getNavigation().stop();level().broadcastEntityEvent(this,(byte)7);}return InteractionResult.sidedSuccess(level().isClientSide);}
  return InteractionResult.PASS;
 }
 @Override public void tick(){
  super.tick();
  if(level().isClientSide){PetRender.particles(this);return;}
  if(owner==null||individual==null){discard();return;}
  ServerPlayer p=((net.minecraft.server.level.ServerLevel)level()).getServer().getPlayerList().getPlayer(owner);
  if(p==null||p.level()!=level()||!p.isAlive()||p.isSpectator()||!PetWorld.get(p).owns(p,individual)){discard();return;}
  var data=PetWorld.get(p).player(owner);if(!data.hasUUID("active")||!data.getUUID("active").equals(individual)){discard();return;}
  if(entityData.get(PETTING)>0){entityData.set(PETTING,entityData.get(PETTING)-1);getLookControl().setLookAt(p,30,30);return;}
  if(tickCount%10==0){
   double distance=distanceToSqr(p);if(distance>6.25&&getDeltaMovement().horizontalDistanceSqr()<.0001)stuckTicks+=10;else stuckTicks=0;
   if(distance>144||stuckTicks>=100){PetEvents.teleportNear(this,p);stuckTicks=0;}else if(distance>6.25)getNavigation().moveTo(p,1.15);else getNavigation().stop();
  }
  if(getTarget()!=null)setTarget(null);
 }
 @Override public void handleEntityEvent(byte id){if(id==7){for(int i=0;i<4;i++)level().addParticle(net.minecraft.core.particles.ParticleTypes.HEART,getX()+(random.nextDouble()-.5)*.5,getY()+.7,getZ()+(random.nextDouble()-.5)*.5,0,.025,0);}else super.handleEntityEvent(id);}
}
