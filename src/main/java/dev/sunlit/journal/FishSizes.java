package dev.sunlit.journal;

import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.*;
import java.util.*;
import java.util.function.Consumer;

/** Sizes are assigned on the server to fishing rewards, never to inventory pickups or sales. */
public final class FishSizes {
    public static final String KEY="sunlit_fish_size";
    public enum Size {
        SMALL("작은 개체",1,.5,.8,0xB8CDA6),NORMAL("보통 개체",1,.8,1.2,0xF4E4C4),
        LARGE("큰 개체",1.3,1.2,1.8,0x78C9DA),GIANT("초대형",2,1.8,3,0xD8A2EB),COLOSSAL("초거대",3,3,5,0xFFD475);
        public final String label;public final double multiplier,min,max;public final int color;
        Size(String label,double multiplier,double min,double max,int color){this.label=label;this.multiplier=multiplier;this.min=min;this.max=max;this.color=color;}
    }
    public static Size choose(double roll){if(!Double.isFinite(roll)||roll<0||roll>=1)throw new IllegalArgumentException("Roll must be in [0,1)");return roll<.2?Size.SMALL:roll<.8?Size.NORMAL:roll<.97?Size.LARGE:roll<.998?Size.GIANT:Size.COLOSSAL;}
    public static boolean fish(ItemStack s){return !s.isEmpty()&&(s.is(ItemTags.FISHES)||FishSizeProfiles.known(s));}
    public static Optional<Size> size(ItemStack s){
        if(!s.hasTag()||!s.getTag().contains(KEY,Tag.TAG_COMPOUND))return Optional.empty();var n=s.getTag().getCompound(KEY);
        if(n.getInt("schemaVersion")!=1||n.getInt("lengthTenthsCm")<1||!n.hasUUID("catcher")||!n.hasUUID("catchId")||!n.getString("fishId").equals(BuiltInRegistries.ITEM.getKey(s.getItem()).toString()))return Optional.empty();
        try{return Optional.of(Size.valueOf(n.getString("size")));}catch(IllegalArgumentException invalid){return Optional.empty();}
    }
    /** Called by the supplemental KubeJS shipping-bin integration after the existing calculation. */
    public static double saleMultiplier(ItemStack stack){return size(stack).map(s->s.multiplier).orElse(1d);}
    public static void stamp(ItemStack stack,ServerPlayer player,RandomSource random){
        if(!fish(stack)||stack.hasTag()&&stack.getTag().contains(KEY))return;
        Size size=choose(random.nextDouble());double base=FishSizeProfiles.baseCm(stack);int min=(int)Math.max(1,Math.ceil(base*size.min*10)),max=(int)Math.max(min,Math.ceil(base*size.max*10)-1);int length=min+random.nextInt(max-min+1);
        var n=new CompoundTag();n.putInt("schemaVersion",1);n.putString("fishId",BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());n.putString("size",size.name());n.putInt("lengthTenthsCm",length);n.putUUID("catcher",player.getUUID());n.putUUID("catchId",UUID.randomUUID());n.putString("catcherName",JournalChat.nickname(player).getString());n.putLong("caughtAt",System.currentTimeMillis());stack.getOrCreateTag().put(KEY,n);
    }
    /** Preserve reward groups: existing skills run once per reward stack, not once per unit. */
    public static void decorate(List<ItemStack> rewards,ServerPlayer player,RandomSource random){
        for(var stack:rewards)stamp(stack,player,random);
    }
    @SuppressWarnings({"rawtypes","unchecked"}) public static void register(){
        try{var type=Class.forName("com.bonker.stardewfishing.server.event.StardewMinigameModifyRewardsEvent");
            MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,false,(Class)type,(Consumer<Event>)event->{
                try{var p=(ServerPlayer)type.getMethod("getPlayer").invoke(event);var rewards=(List<ItemStack>)type.getMethod("getRewards").invoke(event);decorate(rewards,p,p.getRandom());}
                catch(ReflectiveOperationException e){throw new IllegalStateException("Stardew Fishing reward API changed",e);}
            });
        }catch(ClassNotFoundException absent){/* Vanilla fishing uses the global loot modifier. */}
        MinecraftForge.EVENT_BUS.register(new Events());
    }
    public static String length(ItemStack s){return String.format(Locale.ROOT,"%.1f",s.getTag().getCompound(KEY).getInt("lengthTenthsCm")/10d);}
    static void commit(ItemStack s,ServerPlayer p){
        var size=size(s);if(size.isEmpty())return;var n=s.getTag().getCompound(KEY);
        if(n.getBoolean("recorded")||!n.getUUID("catcher").equals(p.getUUID()))return;n.putBoolean("recorded",true);
        if(p.isCreative()||p.isSpectator())return;
        var data=SunlitJournal.data(p);if(!data.contains("fishRecords",10))data.put("fishRecords",new CompoundTag());var records=data.getCompound("fishRecords");String id=BuiltInRegistries.ITEM.getKey(s.getItem()).toString();int cm=n.getInt("lengthTenthsCm");
        // Copies granted by existing fishing skills share a catch ID; only a new maximum announces.
        if(cm>records.getInt(id)){records.putInt(id,cm);p.sendSystemMessage(Component.literal("✦ "+s.getHoverName().getString()+" 최대어 기록 갱신! "+length(s)+"cm").withStyle(st->st.withColor(size.get().color)));
            if(size.get()==Size.COLOSSAL)p.getServer().getPlayerList().broadcastSystemMessage(Component.literal("✦ "+n.getString("catcherName")+"님이 초거대 "+s.getHoverName().getString()+"을 낚았어요! · "+length(s)+"cm").withStyle(st->st.withColor(Size.COLOSSAL.color)),false);}
    }
    public static final class Events {
        @SubscribeEvent public void reload(net.minecraftforge.event.AddReloadListenerEvent e){e.addListener(new FishSizeProfiles());}
        @SubscribeEvent(priority=EventPriority.LOWEST) public void spawn(net.minecraftforge.event.entity.EntityJoinLevelEvent e){
            if(e.getLevel().isClientSide||e.loadedFromDisk()||!(e.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity item)||size(item.getItem()).isEmpty())return;
            var n=item.getItem().getTag().getCompound(KEY);var p=e.getLevel().getServer().getPlayerList().getPlayer(n.getUUID("catcher"));if(p!=null)commit(item.getItem(),p);
        }
        @SubscribeEvent public void inventory(net.minecraftforge.event.TickEvent.PlayerTickEvent e){
            if(e.phase!=net.minecraftforge.event.TickEvent.Phase.END||!(e.player instanceof ServerPlayer p)||p.tickCount%20!=0)return;
            for(var s:p.getInventory().items)commit(s,p);
        }
    }
}
