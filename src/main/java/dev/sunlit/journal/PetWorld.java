package dev.sunlit.journal;

import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** World-level authority for individual pets. Never trusts species/owner supplied by a client. */
public final class PetWorld extends SavedData {
 private final CompoundTag root;
 public PetWorld(){this(new CompoundTag());}
 public PetWorld(CompoundTag root){this.root=root.copy();}
 public static PetWorld get(ServerPlayer p){return p.getServer().overworld().getDataStorage().computeIfAbsent(PetWorld::new,PetWorld::new,"sunlit_pets_v1");}
 @Override public CompoundTag save(CompoundTag tag){tag.merge(root);return tag;}
 static CompoundTag child(CompoundTag parent,String key){if(!parent.contains(key,Tag.TAG_COMPOUND))parent.put(key,new CompoundTag());return parent.getCompound(key);}
 public CompoundTag player(UUID id){return child(child(root,"players"),id.toString());}
 CompoundTag records(){return child(root,"pets");}
 public CompoundTag record(UUID id){return records().getCompound(id.toString());}
 public boolean owns(ServerPlayer p,UUID id){var r=record(id);return r.hasUUID("owner")&&r.getUUID("owner").equals(p.getUUID())&&r.getString("state").equals("stored");}
 public static void message(ServerPlayer p,String s){p.sendSystemMessage(Component.literal("[펫 보관함] "+s));}
 public ItemStack mint(String species){
  if(PetCatalog.find(species).isEmpty())throw new IllegalArgumentException("Unknown species");
  UUID id=UUID.randomUUID();var r=new CompoundTag();r.putString("species",species);r.putString("state","item");r.putUUID("token",UUID.randomUUID());records().put(id.toString(),r);setDirty();return doll(id,r);
 }
 private ItemStack doll(UUID id,CompoundTag r){var s=new ItemStack(PetContent.DOLL.get());s.getOrCreateTag().putUUID("pet",id);s.getOrCreateTag().putUUID("token",r.getUUID("token"));s.getOrCreateTag().putString("species",r.getString("species"));return s;}
 public boolean register(ServerPlayer p,ItemStack stack){
  if(!stack.is(PetContent.DOLL.get())||!stack.hasTag()||!stack.getTag().hasUUID("pet"))return false;
  UUID id=stack.getTag().getUUID("pet");var r=record(id);
  if(!r.getString("state").equals("item")||!r.hasUUID("token")||!stack.getTag().hasUUID("token")||!r.getUUID("token").equals(stack.getTag().getUUID("token"))||PetCatalog.find(r.getString("species")).isEmpty()){message(p,"이미 등록되었거나 이 세계에서 발급하지 않은 인형입니다.");return false;}
  if(ownedCount(p)>=512){message(p,"보관함이 가득 찼습니다. 먼저 인형으로 꺼내 주세요.");return false;}
  r.putString("state","stored");r.putUUID("owner",p.getUUID());r.remove("accessory");stack.shrink(1);setDirty();message(p,PetCatalog.find(r.getString("species")).orElseThrow().name()+" 등록 완료!");return true;
 }
 public long ownedCount(ServerPlayer p){return records().getAllKeys().stream().filter(k->{var r=records().getCompound(k);return r.hasUUID("owner")&&r.getUUID("owner").equals(p.getUUID())&&r.getString("state").equals("stored");}).count();}
 public boolean withdraw(ServerPlayer p,UUID id){
  if(!owns(p,id))return false;
  int slot=p.getInventory().getFreeSlot();if(slot<0){message(p,"인벤토리 빈 칸이 필요합니다.");return false;}
  var r=record(id);recallIf(p,id);r.remove("owner");r.remove("accessory");r.putString("state","item");r.putUUID("token",UUID.randomUUID());p.getInventory().setItem(slot,doll(id,r));setDirty();return true;
 }
 public void recall(ServerPlayer p){player(p.getUUID()).remove("active");PetEvents.remove(p);setDirty();}
 private void recallIf(ServerPlayer p,UUID id){var d=player(p.getUUID());if(d.hasUUID("active")&&d.getUUID("active").equals(id))recall(p);}
 public boolean summon(ServerPlayer p,UUID id){if(!owns(p,id))return false;PetEvents.remove(p);player(p.getUUID()).putUUID("active",id);setDirty();PetEvents.ensure(p);return true;}
 public boolean unlocked(ServerPlayer p,String species){return child(player(p.getUUID()),"unlocks").getBoolean(species);}
 /** Verified Numismatics registry ID is configurable; missing integration never charges a substitute. */
 public static String coinId(){return PetConfig.IRIDIUM.get();}
 public boolean buy(ServerPlayer p,String species){
  var spec=PetCatalog.find(species);if(spec.isEmpty()||unlocked(p,species))return false;
  Item coin=ForgeRegistries.ITEMS.getValue(new ResourceLocation(coinId()));if(coin==null||coin==Items.AIR){message(p,"이리듐 동전 연동이 없습니다. 서버 설정을 확인해 주세요.");return false;}
  int count=0;for(var s:p.getInventory().items)if(s.is(coin))count+=s.getCount();int cost=spec.get().price();
  if(count<cost){message(p,"이리듐 동전 "+cost+"개가 필요합니다.");return false;}
  int remaining=cost;for(var s:p.getInventory().items)if(s.is(coin)){int take=Math.min(remaining,s.getCount());s.shrink(take);remaining-=take;if(remaining==0)break;}
  child(player(p.getUUID()),"unlocks").putBoolean(species,true);setDirty();message(p,"장식을 영구 해금했습니다.");return true;
 }
 public boolean accessory(ServerPlayer p,UUID id){if(!owns(p,id))return false;var r=record(id);if(!unlocked(p,r.getString("species")))return false;r.putBoolean("accessory",!r.getBoolean("accessory"));setDirty();PetEvents.sync(p);return true;}
 public boolean draw(ServerPlayer p){
  if(p.isSpectator())return false;
  int ticket=-1;for(int i=0;i<p.getInventory().items.size();i++)if(p.getInventory().getItem(i).is(PetContent.TICKET.get())){ticket=i;break;}
  if(ticket<0){message(p,"펫 뽑기권 한 장이 필요합니다.");return false;}
  var stack=p.getInventory().getItem(ticket);int slot=p.getInventory().getFreeSlot();if(slot<0&&stack.getCount()==1)slot=ticket;
  if(slot<0){message(p,"인벤토리 빈 칸이 필요합니다.");return false;}
  String season=Integrations.season(p).map(Integrations.Season::name).orElse("");var spec=PetCatalog.draw(p.getRandom(),season);
  var doll=mint(spec.id());stack.shrink(1);p.getInventory().setItem(slot,doll);player(p.getUUID()).putString("lastDraw",spec.id());setDirty();message(p,spec.name()+" 인형을 뽑았습니다!");return true;
 }
 public CompoundTag snapshot(ServerPlayer p,String mode){
  var out=new CompoundTag();out.putString("mode",mode);var list=new ListTag();var d=player(p.getUUID());
  for(String key:records().getAllKeys()){var r=records().getCompound(key);if(r.hasUUID("owner")&&r.getUUID("owner").equals(p.getUUID())&&r.getString("state").equals("stored")){var row=r.copy();row.putString("id",key);list.add(row);}}
  out.put("pets",list);if(d.hasUUID("active"))out.putString("active",d.getUUID("active").toString());out.put("unlocks",child(d,"unlocks").copy());out.putString("lastDraw",d.getString("lastDraw"));out.putString("season",Integrations.season(p).map(Integrations.Season::name).orElse(""));
  int tickets=0;for(var s:p.getInventory().items)if(s.is(PetContent.TICKET.get()))tickets+=s.getCount();out.putInt("tickets",tickets);out.putString("coin",coinId());return out;
 }
}
