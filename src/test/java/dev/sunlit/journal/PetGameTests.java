package dev.sunlit.journal;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
@GameTestHolder(SunlitJournal.MODID)
@PrefixGameTestTemplate(false)
public class PetGameTests {
 static ServerPlayer player(GameTestHelper h){return net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"PetTest"));}
 @GameTest(template="empty") public static void catalogAndOdds(GameTestHelper h){
  h.assertTrue(PetCatalog.ALL.size()==60,"Need 60 species");h.assertTrue(PetCatalog.ALL.stream().map(PetCatalog.Species::id).distinct().count()==60,"Duplicate IDs");
  h.assertTrue(PetCatalog.ALL.stream().filter(PetCatalog.Species::legendary).count()==20,"Wrong rarity count");
  int current=0,off=0;for(var s:PetCatalog.ALL.subList(40,60)){if(s.season().equals("SPRING"))current+=PetCatalog.weight(s,"SPRING");else off+=PetCatalog.weight(s,"SPRING");}
  h.assertTrue(current==15&&off==30,"Wrong 1.5x season odds");
  var random=net.minecraft.util.RandomSource.create(67421);int legends=0,spring=0;Set<String> seen=new HashSet<>();for(int i=0;i<100000;i++){var s=PetCatalog.draw(random,"SPRING");seen.add(s.id());if(s.legendary()){legends++;if(s.season().equals("SPRING"))spring++;}}
  h.assertTrue(legends>4700&&legends<5300,"Legend probability differs from 5%");h.assertTrue(spring>1500&&spring<1850,"Season bias incorrect");h.assertTrue(seen.size()==60,"An off-season species cannot be drawn");h.succeed();
 }
 @GameTest(template="empty") public static void registerReplayAndTrade(GameTestHelper h){
  var a=player(h);var b=player(h);var w=new PetWorld();var doll=w.mint("fox");var stale=doll.copy();UUID id=doll.getTag().getUUID("pet");
  h.assertTrue(w.register(a,doll),"Register failed");h.assertTrue(doll.isEmpty(),"Doll not consumed");h.assertFalse(w.register(b,stale.copy()),"Duplicate registered");h.assertFalse(w.withdraw(b,id),"Non-owner withdrawal");
  h.assertTrue(w.withdraw(a,id),"Withdraw failed");h.assertFalse(w.owns(a,id),"Withdraw retained owner");h.assertFalse(w.register(b,stale),"Stale pre-withdrawal token accepted");
  var fresh=a.getInventory().getItem(0);h.assertTrue(w.register(b,fresh),"Trading rejected");h.assertTrue(w.owns(b,id),"Individual identity lost on trade");h.succeed();
 }
 @GameTest(template="empty") public static void fullInventoryRollback(GameTestHelper h){
  var p=player(h);var w=new PetWorld();var s=w.mint("winter_dragon");UUID id=s.getTag().getUUID("pet");w.register(p,s);
  for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));
  h.assertFalse(w.withdraw(p,id),"Full inventory withdrawal succeeded");h.assertTrue(w.owns(p,id),"Pet lost on failure");
  p.getInventory().setItem(0,new ItemStack(PetContent.TICKET.get(),2));h.assertFalse(w.draw(p),"Full inventory multi-ticket draw succeeded");h.assertTrue(p.getInventory().getItem(0).getCount()==2,"Failed draw consumed a ticket");
  p.getInventory().setItem(0,new ItemStack(PetContent.TICKET.get()));h.assertTrue(w.draw(p),"Single ticket cannot free its slot");h.assertTrue(p.getInventory().getItem(0).is(PetContent.DOLL.get()),"Doll not delivered");h.succeed();
 }
 @GameTest(template="empty") public static void permanentAccessoryOwnerIsolation(GameTestHelper h){
  var a=player(h);var b=player(h);var w=new PetWorld();var coin=TaskRewards.stack("numismatics:sun",1).getItem();
  h.assertFalse(w.buy(a,"fox"),"Free purchase");a.getInventory().add(new ItemStack(coin,30));h.assertTrue(w.buy(a,"fox"),"Purchase failed");h.assertTrue(a.getInventory().countItem(coin)==20,"Common price not 10");h.assertFalse(w.buy(a,"fox"),"Double purchase charged");
  var s=w.mint("fox");UUID id=s.getTag().getUUID("pet");w.register(a,s);h.assertTrue(w.accessory(a,id),"Unlocked accessory rejected");h.assertTrue(w.withdraw(a,id),"Withdraw failed");
  var fresh=a.getInventory().items.stream().filter(t->t.is(PetContent.DOLL.get())).findFirst().orElseThrow();w.register(b,fresh);
  h.assertTrue(w.unlocked(a,"fox"),"Original unlock lost");h.assertFalse(w.unlocked(b,"fox"),"Unlock transferred with pet");h.assertFalse(w.record(id).getBoolean("accessory"),"Accessory remained equipped after trade");h.assertFalse(w.accessory(b,id),"Other owner can use unbought accessory");
  h.assertTrue(w.buy(a,"winter_dragon"),"Legend purchase failed");h.assertTrue(a.getInventory().countItem(coin)==0,"Legend price not 20");h.succeed();
 }
 @GameTest(template="empty") public static void ledgerRoundTripAndForgery(GameTestHelper h){
  var p=player(h);var w=new PetWorld();var s=w.mint("spring_fairy");UUID id=s.getTag().getUUID("pet");w.register(p,s);w.player(p.getUUID()).putUUID("active",id);
  var copy=new PetWorld(w.save(new CompoundTag()));h.assertTrue(copy.owns(p,id),"Owner lost on save/load");h.assertTrue(copy.player(p.getUUID()).getUUID("active").equals(id),"Wanted summon lost");copy.player(p.getUUID()).remove("active");h.assertTrue(w.player(p.getUUID()).hasUUID("active"),"SavedData aliases input");
  var fake=new ItemStack(PetContent.DOLL.get());fake.getOrCreateTag().putUUID("pet",UUID.randomUUID());fake.getTag().putUUID("token",UUID.randomUUID());fake.getTag().putString("species","spring_fairy");h.assertFalse(w.register(p,fake),"Forged doll registered");h.assertFalse(w.withdraw(p,UUID.randomUUID()),"Unknown pet withdrawn");h.succeed();
 }
 @GameTest(template="empty") public static void remoteVendingAndShopDenied(GameTestHelper h){
  var p=player(h);p.getInventory().add(new ItemStack(PetContent.TICKET.get(),5));var w=PetWorld.get(p);long before=w.ownedCount(p);PetNetwork.handle(p,new PetNetwork.Request("draw",""));h.assertTrue(p.getInventory().countItem(PetContent.TICKET.get())==5,"Remote draw consumed ticket");h.assertTrue(w.ownedCount(p)==before,"Remote draw changed vault");PetNetwork.handle(p,new PetNetwork.Request("buy","fox"));h.assertFalse(w.unlocked(p,"fox"),"Remote purchase granted unlock");h.succeed();
 }
 @GameTest(template="empty") public static void oneCompanionAndRecall(GameTestHelper h){
  var p=player(h);var at=h.absolutePos(new net.minecraft.core.BlockPos(1,2,1));p.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);
  for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var floor=at.offset(x,-1,z);h.getLevel().setBlockAndUpdate(floor,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());h.getLevel().setBlockAndUpdate(floor.above(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());h.getLevel().setBlockAndUpdate(floor.above(2),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());}
  var w=PetWorld.get(p);var a=w.mint("fox");var b=w.mint("winter_fairy");var aid=a.getTag().getUUID("pet");var bid=b.getTag().getUUID("pet");w.register(p,a);w.register(p,b);
  h.assertTrue(w.summon(p,aid),"First summon failed");h.assertTrue(w.summon(p,bid),"Second summon failed");PetEvents.ensure(p);
  var list=p.serverLevel().getEntities(PetContent.PET.get(),v->p.getUUID().equals(v.owner()));h.assertTrue(list.size()==1&&list.get(0).individual().equals(bid),"More than one pet or wrong pet");var entity=list.get(0);
  h.assertFalse(entity.hurt(p.damageSources().generic(),999),"Pet is damageable");h.assertFalse(entity.isPushable(),"Pet obstructs players");h.assertTrue(entity.isIgnoringBlockTriggers(),"Pet activates pressure plates");h.assertFalse(entity.canTrample(net.minecraft.world.level.block.Blocks.FARMLAND.defaultBlockState(),at,8),"Pet destroys crops");h.assertFalse(entity.canBeHitByProjectile(),"Pet blocks arrows");h.assertFalse(entity.shouldBeSaved(),"Chunk can persist duplicate pets");h.assertFalse(entity.canBeLeashed(p),"Pet can be stolen with lead");
  h.assertFalse(w.summon(p,UUID.randomUUID()),"Forged summon accepted");w.recall(p);PetEvents.ensure(p);h.assertTrue(p.serverLevel().getEntities(PetContent.PET.get(),v->p.getUUID().equals(v.owner())).isEmpty(),"Manual recall did not stick");h.succeed();
 }
 @GameTest(template="empty") public static void animationLinearAndLoop(GameTestHelper h){
  var keys=java.util.List.of(new PetAnimation.Key(0,0,0,0,false),new PetAnimation.Key(1,10,20,30,false));
  var value=PetAnimation.sample(keys,.25f,1,false,0);h.assertTrue(Math.abs(value[0]-2.5f)<.0001&&Math.abs(value[2]-7.5f)<.0001,"Linear sample wrong");
  h.assertTrue(Math.abs(PetAnimation.sample(keys,1.25f,1,true,0)[1]-5)<.0001,"Loop did not wrap");h.succeed();
 }
 @GameTest(template="empty") public static void animationCatmullAndEndpoints(GameTestHelper h){
  var keys=java.util.List.of(new PetAnimation.Key(0,0,0,0,true),new PetAnimation.Key(1,1,1,1,true),new PetAnimation.Key(2,0,0,0,true),new PetAnimation.Key(3,-1,-1,-1,true));
  h.assertTrue(Math.abs(PetAnimation.sample(keys,1.5f,3,false,0)[0]-.625f)<.0001,"Catmull-Rom interpolation wrong");h.assertTrue(PetAnimation.sample(keys,1,3,false,0)[0]==1,"Exact keyframe changed");h.succeed();
 }
 @GameTest(template="empty") public static void animationMissingAndClamp(GameTestHelper h){
  var empty=PetAnimation.sample(java.util.List.of(),2,1,false,1);h.assertTrue(empty[0]==1&&empty[1]==1&&empty[2]==1,"Missing scale not identity");
  var keys=java.util.List.of(new PetAnimation.Key(0,2,3,4,false),new PetAnimation.Key(1,5,6,7,false));h.assertTrue(PetAnimation.sample(keys,3,1,false,0)[2]==7,"Once animation did not clamp");h.assertTrue(PetAnimation.sample(keys,-3,1,false,0)[0]==2,"Negative time not clamped");h.succeed();
 }
}
