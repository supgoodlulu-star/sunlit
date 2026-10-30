package dev.sunlit.journal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Stationary, persistent display; never grants loot or modifies the represented player. */
public final class HallFigure extends net.minecraft.world.entity.PathfinderMob {
    static final EntityDataAccessor<String> PROFILE=SynchedEntityData.defineId(HallFigure.class,EntityDataSerializers.STRING);
    static final EntityDataAccessor<Boolean> OCCUPIED=SynchedEntityData.defineId(HallFigure.class,EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<String> TEXTURE=SynchedEntityData.defineId(HallFigure.class,EntityDataSerializers.STRING);
    private String category="tasks";private int slot;
    public HallFigure(EntityType<? extends HallFigure> type,Level level){super(type,level);setNoAi(true);setPersistenceRequired();setInvulnerable(true);setNoGravity(true);}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(PROFILE,"");entityData.define(OCCUPIED,false);entityData.define(TEXTURE,"");}
    void configure(String category,int slot){this.category=category;this.slot=slot;}
    public com.mojang.authlib.GameProfile profile(){try{var p=new com.mojang.authlib.GameProfile(UUID.fromString(entityData.get(PROFILE)),"HallPlayer");if(!entityData.get(TEXTURE).isEmpty())p.getProperties().put("textures",new com.mojang.authlib.properties.Property("textures",entityData.get(TEXTURE)));return p;}catch(IllegalArgumentException e){return new com.mojang.authlib.GameProfile(new UUID(0,0),"HallPlayer");}}
    public boolean occupied(){return entityData.get(OCCUPIED);}
    @Override public void tick(){super.tick();if(!level().isClientSide&&slot>0){discard();return;}yBodyRot=getYRot();setYHeadRot(getYRot());if(!level().isClientSide&&tickCount%40==0){var w=HallWorld.get(getServer());var rows=w.rows(w.season(),category);var label=w.categories().getOrDefault(category,category);boolean filled=rows.size()>slot;entityData.set(OCCUPIED,filled);
        if(filled){var r=rows.get(slot);entityData.set(PROFILE,r.id().toString());entityData.set(TEXTURE,w.person(r.id()).getString("texture"));setCustomName(Component.literal(r.rank()+"위 · "+r.nick()+" · "+String.format(java.util.Locale.ROOT,"%,d",r.score())+" | "+label));}
        else{entityData.set(PROFILE,"");entityData.set(TEXTURE,"");setCustomName(Component.literal(label+" · 도전자를 기다려요"));}setCustomNameVisible(true);
    }}
    @Override public InteractionResult mobInteract(Player p,InteractionHand hand){if(p instanceof net.minecraft.server.level.ServerPlayer s&&hand==InteractionHand.MAIN_HAND)HallNetwork.open(s,category);return InteractionResult.sidedSuccess(level().isClientSide);}
    @Override public void addAdditionalSaveData(CompoundTag n){super.addAdditionalSaveData(n);n.putString("HallCategory",category);n.putInt("HallSlot",slot);}
    @Override public void readAdditionalSaveData(CompoundTag n){super.readAdditionalSaveData(n);category=n.getString("HallCategory");slot=Math.max(0,Math.min(2,n.getInt("HallSlot")));}
    @Override public boolean hurt(DamageSource s,float a){return false;}
    @Override public boolean isPushable(){return false;}
    @Override protected void doPush(Entity e){}
    @Override public boolean canBeLeashed(Player p){return false;}
    @Override public boolean canChangeDimensions(){return false;}
    @Override public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance effect){return false;}
}
