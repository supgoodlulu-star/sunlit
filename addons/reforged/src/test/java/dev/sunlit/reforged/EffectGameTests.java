package dev.sunlit.reforged;
import net.minecraftforge.gametest.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import com.mojang.authlib.GameProfile;
import java.util.*;
import static dev.sunlit.reforged.Potential.*;
import dev.sunlit.reforged.Potential.Tier;
@GameTestHolder("sunlit_reforged") @PrefixGameTestTemplate(false)
public final class EffectGameTests{
    static ServerPlayer player(GameTestHelper h){var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"EffectsLive"));try{var field=ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");field.setAccessible(true);field.setInt(p,0);}catch(ReflectiveOperationException e){throw new RuntimeException(e);}p.connection=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"PacketSink")).connection;return p;}
    static ItemStack item(Item i,Line... lines){var s=new ItemStack(i);new Potential(Tier.LEGENDARY,List.of(lines)).apply(s);return s;}
    static Line line(Option o,double v){return new Line(o,Tier.LEGENDARY,v);}
    @GameTest(template="empty") public static void actualMeleeExecuteAndLifesteal(GameTestHelper h){
        var p=player(h);p.setHealth(10);p.setItemInHand(InteractionHand.MAIN_HAND,item(Items.DIAMOND_SWORD,line(Option.EXECUTE,12),line(Option.LIFESTEAL,4),line(Option.ATTACK_FLAT,1)));
        var low=h.spawn(EntityType.ZOMBIE,new BlockPos(1,1,1));low.getAttribute(Attributes.ARMOR).setBaseValue(0);low.getAttribute(Attributes.ARMOR).removeModifiers();low.setHealth(4);low.hurt(p.damageSources().playerAttack(p),2);
        h.assertTrue(Math.abs(low.getHealth()-1.76)<.01&&Math.abs(p.getHealth()-10.0896)<.01,"Execute threshold / real health damage lifesteal wrong");
        var high=h.spawn(EntityType.ZOMBIE,new BlockPos(3,1,1));high.getAttribute(Attributes.ARMOR).setBaseValue(0);high.getAttribute(Attributes.ARMOR).removeModifiers();high.setHealth(8);high.hurt(p.damageSources().playerAttack(p),2);
        h.assertTrue(Math.abs(high.getHealth()-6)<.01,"Execute applied above 30 percent");low.discard();high.discard();h.succeed();
    }
    @GameTest(template="empty") public static void healthMovementJumpAndUnequip(GameTestHelper h){
        var p=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"JumpTest"));
        p.setItemSlot(EquipmentSlot.HEAD,item(Items.DIAMOND_HELMET,line(Option.HEALTH_FLAT,4),line(Option.HEALTH_PERCENT,8),line(Option.JUMP,8)));
        p.setItemSlot(EquipmentSlot.FEET,item(Items.DIAMOND_BOOTS,line(Option.MOVEMENT,5),line(Option.MOVEMENT,5),line(Option.MOVEMENT,5)));Effects.reconcile(p);
        h.assertTrue(Math.abs(p.getMaxHealth()-25.92)<.01,"Health flat/percent wrong");double base=p.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue();h.assertTrue(Math.abs(p.getAttributeValue(Attributes.MOVEMENT_SPEED)-base*1.15)<.00001,"Movement bonus wrong");
        p.setSprinting(false);p.jumpFromGround();h.assertTrue(Math.abs(p.getDeltaMovement().y-.42*1.08)<.001,"Actual jump hook wrong");
        p.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY);Effects.reconcile(p);h.assertTrue(Math.abs(p.getMaxHealth()-20)<.001&&Math.abs(p.getAttributeValue(Attributes.MOVEMENT_SPEED)-base)<.00001,"Modifiers remain after unequip");h.succeed();
    }
    @GameTest(template="empty") public static void damageReductionAndReflection(GameTestHelper h){
        var p=player(h);p.setItemSlot(EquipmentSlot.CHEST,item(Items.DIAMOND_CHESTPLATE,line(Option.REDUCTION,4),line(Option.REFLECT,4),line(Option.KNOCKBACK,20)));Effects.reconcile(p);
        p.getAttribute(Attributes.ARMOR).setBaseValue(0);p.getAttribute(Attributes.ARMOR).removeModifiers();var attacker=h.spawn(EntityType.ZOMBIE,new BlockPos(1,1,1));attacker.getAttribute(Attributes.ARMOR).setBaseValue(0);attacker.getAttribute(Attributes.ARMOR).removeModifiers();
        float raw=switch(h.getLevel().getDifficulty()){case PEACEFUL->0;case EASY->6;case NORMAL->10;case HARD->15;};p.hurt(p.damageSources().mobAttack(attacker),10);h.assertTrue(Math.abs(p.getHealth()-(20-raw*.96))<.01,"Reduction did not apply to real damage: health="+p.getHealth()+" difficulty="+h.getLevel().getDifficulty());h.assertTrue(Math.abs(attacker.getHealth()-16)<.01,"Reflection missing");h.assertTrue(Math.abs(p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)-.2)<.0001,"Knockback bonus wrong");attacker.discard();h.succeed();
    }
    @GameTest(template="empty") public static void naturalOreBonusExcludesPlayerPlacedAndAutomation(GameTestHelper h){
        var p=player(h);var tool=item(Items.DIAMOND_PICKAXE,line(Option.ORE_BONUS,100),line(Option.ORE_BONUS,100),line(Option.ORE_BONUS,100));var pos=h.absolutePos(new BlockPos(2,2,2));var state=Blocks.DIAMOND_ORE.defaultBlockState();h.getLevel().setBlockAndUpdate(pos,state);
        Ledger.get(p.getServer()).forget(h.getLevel(),pos);double before=Rules.get().rewardRates[0];Rules.get().rewardRates[0]=100;
        try{
            var drops=Block.getDrops(state,h.getLevel(),pos,null,p,tool);h.assertTrue(drops.stream().filter(s->s.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum()==2&&drops.stream().anyMatch(s->s.is(SunlitReforged.SUSPICIOUS.get())),"Natural ore extra item/cube wrong");
            Ledger.get(p.getServer()).placed(h.getLevel(),pos);var placed=Block.getDrops(state,h.getLevel(),pos,null,p,tool);h.assertTrue(placed.stream().filter(s->s.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum()==1&&placed.stream().noneMatch(s->s.is(SunlitReforged.SUSPICIOUS.get())),"Placed ore generated bonus");
            var fake=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"Automation"));var automated=Block.getDrops(state,h.getLevel(),pos.above(),null,fake,tool);h.assertTrue(automated.stream().noneMatch(s->s.is(SunlitReforged.SUSPICIOUS.get()))&&automated.stream().filter(s->s.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum()==1,"Fake player generated bonus");
        }finally{Rules.get().rewardRates[0]=before;Ledger.get(p.getServer()).forget(h.getLevel(),pos);}h.succeed();
    }
    @GameTest(template="empty") public static void matureCropBonusDoesNotRewardImmatureHarvest(GameTestHelper h){
        var p=player(h);var tool=item(Items.DIAMOND_HOE,line(Option.CROP_BONUS,100),line(Option.CROP_BONUS,100),line(Option.CROP_BONUS,100));var pos=h.absolutePos(new BlockPos(1,2,1));double before=Rules.get().rewardRates[1];Rules.get().rewardRates[1]=100;
        try{var mature=Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7);var grown=Block.getDrops(mature,h.getLevel(),pos,null,p,tool);h.assertTrue(grown.stream().filter(s->s.is(Items.WHEAT)).mapToInt(ItemStack::getCount).sum()==2&&grown.stream().anyMatch(s->s.is(SunlitReforged.SUSPICIOUS.get())),"Mature crop bonus wrong");var immature=Block.getDrops(Blocks.WHEAT.defaultBlockState(),h.getLevel(),pos.above(),null,p,tool);h.assertTrue(immature.stream().noneMatch(s->s.is(SunlitReforged.SUSPICIOUS.get())),"Immature crop rewarded");}finally{Rules.get().rewardRates[1]=before;}h.succeed();
    }
}
