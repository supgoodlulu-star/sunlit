package dev.sunlit.reforged;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;
import java.util.*;

public final class Effects{
    static List<ItemStack> equipped(Player p){var list=new ArrayList<ItemStack>();if(Potential.gear(p.getMainHandItem()).filter(g->g!=Potential.Gear.ARMOR).isPresent())list.add(p.getMainHandItem());for(var s:p.getArmorSlots())if(Potential.gear(s).orElse(null)==Potential.Gear.ARMOR)list.add(s);return list;}
    public static double sum(ItemStack stack,Potential.Option option){if(Potential.gear(stack).filter(option.gear::contains).isEmpty())return 0;return Potential.get(stack).map(p->p.lines().stream().filter(l->l.option()==option).mapToDouble(Potential.Line::value).sum()).orElse(0d);}
    public static double sum(Player p,Potential.Option o){return equipped(p).stream().mapToDouble(s->sum(s,o)).sum();}
    public static double chance(double percent){return Math.max(0,Math.min(1,percent/100));}
    static float finite(double amount){return (float)Math.max(0,Math.min(Float.MAX_VALUE,amount));}
    static void modifier(ServerPlayer p,Attribute attr,String name,double amount,AttributeModifier.Operation operation){
        var a=p.getAttribute(attr);if(a==null)return;UUID id=UUID.nameUUIDFromBytes(("sunlit_reforged:"+name).getBytes(java.nio.charset.StandardCharsets.UTF_8));var old=a.getModifier(id);
        if(old!=null&&Double.compare(old.getAmount(),amount)==0)return;if(old!=null)a.removeModifier(id);if(amount!=0&&Double.isFinite(amount))a.addTransientModifier(new AttributeModifier(id,"Sunlit potential "+name,amount,operation));
    }
    public static void reconcile(ServerPlayer p){
        modifier(p,Attributes.ATTACK_DAMAGE,"attack_flat",sum(p,Potential.Option.ATTACK_FLAT),AttributeModifier.Operation.ADDITION);
        modifier(p,Attributes.ATTACK_DAMAGE,"attack_percent",sum(p,Potential.Option.ATTACK_PERCENT)/100,AttributeModifier.Operation.MULTIPLY_TOTAL);
        modifier(p,Attributes.ATTACK_SPEED,"attack_speed",sum(p,Potential.Option.ATTACK_SPEED)/100,AttributeModifier.Operation.MULTIPLY_TOTAL);
        modifier(p,Attributes.MAX_HEALTH,"health_flat",sum(p,Potential.Option.HEALTH_FLAT),AttributeModifier.Operation.ADDITION);
        modifier(p,Attributes.MAX_HEALTH,"health_percent",sum(p,Potential.Option.HEALTH_PERCENT)/100,AttributeModifier.Operation.MULTIPLY_TOTAL);
        modifier(p,Attributes.ARMOR,"armor",sum(p,Potential.Option.ARMOR),AttributeModifier.Operation.ADDITION);
        modifier(p,Attributes.KNOCKBACK_RESISTANCE,"knockback",sum(p,Potential.Option.KNOCKBACK)/100,AttributeModifier.Operation.ADDITION);
        modifier(p,Attributes.MOVEMENT_SPEED,"movement",sum(p,Potential.Option.MOVEMENT)/100,AttributeModifier.Operation.MULTIPLY_TOTAL);
        if(p.getHealth()>p.getMaxHealth())p.setHealth(p.getMaxHealth());
    }
    @SubscribeEvent public void tick(TickEvent.PlayerTickEvent e){if(e.phase==TickEvent.Phase.END&&e.player instanceof ServerPlayer p&&p.tickCount%5==0)reconcile(p);}
    @SubscribeEvent public void equipment(LivingEquipmentChangeEvent e){if(e.getEntity() instanceof ServerPlayer p)reconcile(p);}
    @SubscribeEvent(priority=EventPriority.LOW) public void hurt(LivingHurtEvent e){
        if(e.getEntity().level().isClientSide)return;
        double damage=e.getAmount();
        if(!e.getSource().is(DamageTypes.THORNS)&&e.getSource().getDirectEntity() instanceof ServerPlayer attacker&&e.getEntity().getHealth()<=e.getEntity().getMaxHealth()*Rules.get().lowHealth)damage*=1+sum(attacker,Potential.Option.EXECUTE)/100;
        if(e.getEntity() instanceof ServerPlayer defender)damage*=1-chance(sum(defender,Potential.Option.REDUCTION));
        e.setAmount(finite(damage));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void damage(LivingDamageEvent e){
        if(e.getEntity().level().isClientSide||e.getAmount()<=0||e.getSource().is(DamageTypes.THORNS))return;
        if(e.getSource().getDirectEntity() instanceof ServerPlayer p){float heal=finite(Math.min(e.getAmount(),e.getEntity().getHealth())*sum(p,Potential.Option.LIFESTEAL)/100);if(heal>0)p.heal(heal);}
        if(e.getEntity() instanceof ServerPlayer p&&e.getSource().getEntity() instanceof LivingEntity attacker&&attacker!=p){float reflect=finite(sum(p,Potential.Option.REFLECT));if(reflect>0)attacker.hurt(p.damageSources().thorns(p),reflect);}
    }
    @SubscribeEvent public void speed(PlayerEvent.BreakSpeed e){if(e.getEntity().level().isClientSide)return;e.setNewSpeed(finite(e.getNewSpeed()*(1+sum(e.getEntity(),Potential.Option.MINING_SPEED)/100)));}
    @SubscribeEvent public void jump(LivingEvent.LivingJumpEvent e){if(e.getEntity() instanceof ServerPlayer p){double amount=sum(p,Potential.Option.JUMP);if(amount>0){var motion=p.getDeltaMovement();p.setDeltaMovement(motion.x,finite(motion.y*(1+amount/100)),motion.z);p.hurtMarked=true;}}}
    public static int durability(ItemStack stack,int amount,net.minecraft.util.RandomSource random,ServerPlayer p){if(p==null||amount<=0)return amount;return random.nextDouble()<chance(sum(stack,Potential.Option.DURABILITY))?0:amount;}
}
