package dev.sunlit.reforged;
import net.minecraft.server.level.ServerPlayer;
/** Exact 0.16.1 grant-time hook; no polling or recursive extra XP calls. */
public final class Skills{
    public static int adjust(int amount,ServerPlayer p,Object category,Object data){
        if(amount<=0||p==null||!Activity.eligible(p)||!p.getServer().isSameThread())return amount;
        try{
            String id=category.getClass().getMethod("id").invoke(category).toString();if(!Rules.get().skillCategories.contains(id))return amount;
            int current=((Number)data.getClass().getMethod("getExperience").invoke(data)).intValue();
            return calculate(p,id,amount,current,Effects.sum(p,Potential.Option.LIFE_XP));
        }catch(ReflectiveOperationException|RuntimeException e){SunlitReforged.debug("Skills bonus unavailable: {}",e.toString());return amount;}
    }
    static int calculate(ServerPlayer p,String id,int amount,int current,double percent){
        var ledger=Ledger.get(p.getServer());var fractions=Ledger.child(ledger.station(p.getUUID()),"xpRemainder");double bonus=amount*percent/100+fractions.getDouble(id);
        if(!Double.isFinite(bonus))bonus=Long.MAX_VALUE;long requested=(long)Math.floor(bonus);long base=Math.max(0,amount);long allowed=Math.max(0,(long)Integer.MAX_VALUE-current);
        int result=(int)Math.min(allowed,base+Math.min(Integer.MAX_VALUE,requested));fractions.putDouble(id,bonus-Math.floor(bonus));ledger.setDirty();
        SunlitReforged.debug("life XP player={} category={} base={} final={}",p.getUUID(),id,amount,result);return result;
    }
}
