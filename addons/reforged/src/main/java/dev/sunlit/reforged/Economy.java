package dev.sunlit.reforged;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import java.util.*;

/** Verified Numismatics 1.1.0 signatures; never fabricates funds or accounts. */
public final class Economy{
    static Object account(UUID id)throws ReflectiveOperationException{Object bank=Class.forName("dev.ithundxr.createnumismatics.Numismatics").getField("BANK").get(null);return bank.getClass().getMethod("getAccount",UUID.class).invoke(bank,id);}
    public static CompoundTag view(ServerPlayer p){var n=new CompoundTag();try{Object a=account(p.getUUID());if(a==null)return n;long base=((Number)a.getClass().getMethod("getBalance").invoke(a)).longValue();var field=a.getClass().getDeclaredField("additionalBalance");if(!field.trySetAccessible())return n;long total=Math.addExact(base,field.getLong(a));Class<?> c=Class.forName("dev.ithundxr.createnumismatics.content.backend.Coin");long unit=c.getField("value").getInt(c.getField("SPUR").get(null));if(total<0||unit<=0)return n;n.putBoolean("available",true);n.putLong("spurs",total);n.putLong("unit",unit);}catch(ReflectiveOperationException|RuntimeException|LinkageError e){SunlitReforged.debug("balance unavailable: {}",e.toString());}return n;}
    public static boolean buy(ServerPlayer p,int product,int quantity){
        if(product<0||product>2||quantity<1||quantity>64)return false;Item item=SunlitReforged.cube(Potential.Cube.values()[product]);
        ItemStack stack=new ItemStack(item,quantity);if(quantity>stack.getMaxStackSize()||!fits(p,stack))return false;var balance=view(p);if(!balance.getBoolean("available"))return false;
        try{long total=Math.multiplyExact(Math.multiplyExact((long)Rules.get().prices[product],quantity),balance.getLong("unit"));if(total>Integer.MAX_VALUE||balance.getLong("spurs")<total)return false;
            Object a=account(p.getUUID());if(a==null)return false;boolean paid=(Boolean)a.getClass().getMethod("deduct",int.class,boolean.class).invoke(a,(int)total,false);if(!paid)return false;
            // Capacity was checked on this same server thread. No concurrent inventory mutation.
            if(!p.getInventory().add(stack))p.drop(stack,false);SunlitReforged.debug("purchase player={} product={} quantity={} spurs={}",p.getUUID(),product,quantity,total);return true;
        }catch(ReflectiveOperationException|RuntimeException|LinkageError e){SunlitReforged.LOG.warn("Purchase failed before successful delivery",e);return false;}
    }
    static boolean fits(ServerPlayer p,ItemStack stack){int count=stack.getCount();for(int i=0;i<36;i++){var s=p.getInventory().getItem(i);if(s.isEmpty())count-=stack.getMaxStackSize();else if(ItemStack.isSameItemSameTags(s,stack))count-=Math.max(0,s.getMaxStackSize()-s.getCount());if(count<=0)return true;}return false;}
}
