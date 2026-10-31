package dev.sunlit.reforged;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkHooks;
import java.util.*;

public final class StationMenu extends AbstractContainerMenu{
    public final SimpleContainer work;final ServerPlayer owner;final Ledger ledger;final CompoundTag saved;private boolean ready;
    public static StationMenu client(int id,Inventory inventory,FriendlyByteBuf ignored){return new StationMenu(id,inventory,null);}
    public StationMenu(int id,Inventory inv,ServerPlayer serverPlayer){super(SunlitReforged.STATION.get(),id);owner=serverPlayer;ledger=owner==null?null:Ledger.get(owner.getServer());saved=ledger==null?new CompoundTag():ledger.station(owner.getUUID());
        work=new SimpleContainer(2){@Override public void setChanged(){super.setChanged();if(ready&&owner!=null)persist();}};
        if(owner!=null){work.setItem(0,ItemStack.of(saved.getCompound("equipment")));work.setItem(1,ItemStack.of(saved.getCompound("cubes")));}ready=true;
        addSlot(new Slot(work,0,85,77){@Override public boolean mayPlace(ItemStack s){return !pending()&&Potential.gear(s).isPresent()&&s.getCount()==1;}@Override public boolean mayPickup(Player p){return !pending();}@Override public int getMaxStackSize(){return 1;}});
        addSlot(new Slot(work,1,85,151){@Override public boolean mayPlace(ItemStack s){return !pending()&&s.getItem() instanceof SunlitReforged.CubeItem;}@Override public boolean mayPickup(Player p){return !pending();}});
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,col+row*9+9,139+col*18,216+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inv,col,139+col*18,274));
    }
    public static void open(ServerPlayer p){if(p.containerMenu instanceof StationMenu){Network.send(p);return;}NetworkHooks.openScreen(p,new SimpleMenuProvider((id,inv,pl)->new StationMenu(id,inv,p),Component.literal("선릿 재련 공방")));Network.send(p);}
    public boolean pending(){return owner==null?clientState.contains("pending",10):saved.contains("pending",10);}
    public CompoundTag clientState=new CompoundTag();
    int revision(){return saved.getInt("revision");}
    private void advance(){saved.putInt("revision",revision()+1);ledger.setDirty();}
    void persist(){if(owner==null)return;saved.put("equipment",work.getItem(0).save(new CompoundTag()));saved.put("cubes",work.getItem(1).save(new CompoundTag()));advance();}
    public CompoundTag snapshot(){var out=saved.copy();out.putInt("menuId",containerId);out.put("equipment",work.getItem(0).save(new CompoundTag()));out.put("cubes",work.getItem(1).save(new CompoundTag()));out.put("balance",Economy.view(owner));out.putString("prefixStatus","Reforged JAR 미확인 · 접두사 데이터는 원본 그대로 보존");out.putBoolean("hammerAvailable",Economy.hammer().isPresent());out.putBoolean("fishingReady",FishingPools.ready());var list=new ListTag();for(int price:Rules.get().prices)list.add(IntTag.valueOf(price));out.put("prices",list);return out;}
    public boolean roll(){
        if(owner==null||pending())return false;ItemStack gear=work.getItem(0),cubes=work.getItem(1);var type=Potential.gear(gear);
        if(type.orElse(null)==Potential.Gear.ROD&&!FishingPools.ready())return false;
        if(type.isEmpty()||gear.getCount()!=1||cubes.isEmpty()||!(cubes.getItem() instanceof SunlitReforged.CubeItem cube))return false;
        var old=Potential.get(gear);if(gear.hasTag()&&gear.getTag().contains(Potential.KEY)&&old.isEmpty())return false;
        var result=Potential.roll(old,type.get(),cube.type,owner.getRandom(),Rules.get());String rollId=UUID.randomUUID().toString();
        SunlitReforged.debug("roll player={} id={} old={} new={} cube={}",owner.getUUID(),rollId,old.map(p->p.tier().name()).orElse("none"),result.tier(),cube.type);
        cubes.shrink(1);
        if(cube.type==Potential.Cube.MASTER&&old.isPresent()){
            var pending=new CompoundTag();pending.putString("rollId",rollId);pending.put("old",old.get().save());pending.put("next",result.save());saved.put("pending",pending);
            old.get().withTier(result.tier()).apply(gear);
        }else result.apply(gear);
        persist();return true;
    }
    public boolean choose(boolean keepOld,String rollId){
        if(owner==null||!pending())return false;var state=saved.getCompound("pending");if(!state.getString("rollId").equals(rollId))return false;
        var next=Potential.load(state.getCompound("next"));var old=Potential.load(state.getCompound("old"));if(next.isEmpty()||old.isEmpty()||work.getItem(0).isEmpty())return false;
        (keepOld?old.get().withTier(next.get().tier()):next.get()).apply(work.getItem(0));saved.remove("pending");persist();SunlitReforged.debug("choose player={} id={} old={}",owner.getUUID(),rollId,keepOld);return true;
    }
    public boolean act(int expectedRevision,String action,int product,int quantity,String rollId){
        if(owner==null||expectedRevision!=revision())return false;
        boolean ok=switch(action){case "roll"->roll();case "old"->choose(true,rollId);case "new"->choose(false,rollId);case "buy"->!pending()&&Economy.buy(owner,product,quantity);default->false;};
        if(ok&&action.equals("buy"))persist();return ok;
    }
    @Override public ItemStack quickMoveStack(Player p,int index){if(index<0||index>=slots.size()||pending())return ItemStack.EMPTY;Slot slot=slots.get(index);if(!slot.hasItem()||!slot.mayPickup(p))return ItemStack.EMPTY;var stack=slot.getItem();var old=stack.copy();boolean moved=index<2?moveItemStackTo(stack,2,slots.size(),true):moveItemStackTo(stack,0,2,false);if(!moved)return ItemStack.EMPTY;if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,stack);return old;}
    @Override public boolean stillValid(Player p){return owner==null||owner==p;}
    @Override public void removed(Player p){super.removed(p);if(owner==null||pending())return;for(int i=0;i<2;i++){var stack=work.removeItemNoUpdate(i);if(!stack.isEmpty()&&!owner.getInventory().add(stack))owner.drop(stack,false);}persist();}
    @Override public void broadcastChanges(){super.broadcastChanges();if(owner!=null&&owner.tickCount%10==0)Network.send(owner);}
}
