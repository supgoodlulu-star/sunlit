package dev.sunlit.journal;

import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.ModList;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Stream;
import com.mojang.logging.LogUtils;

/** Reflection is restricted to verified external public API names, never guessed game mappings. */
public final class Integrations {
    private static final Set<String> REPORTED = new HashSet<>();
    static void failure(String area, Throwable e) {
        if (JournalConfig.DEBUG.get() && REPORTED.add(area)) LogUtils.getLogger().warn("Sunlit integration {} unavailable", area, e);
    }
    static Object call(Object obj, String name, Class<?>[] types, Object... args) throws ReflectiveOperationException {
        Class<?> c = obj instanceof Class<?> cl ? cl : obj.getClass();
        Method m;
        // Implementations may be non-public: invoke a public interface declaration instead.
        try { m = c.getMethod(name, types); }
        catch (NoSuchMethodException e) { throw e; }
        if (!Modifier.isPublic(m.getDeclaringClass().getModifiers())) {
            for (Class<?> api : c.getInterfaces()) {
                try { m = api.getMethod(name, types); break; } catch (NoSuchMethodException ignored) {}
            }
        }
        return m.invoke(obj instanceof Class<?> ? null : obj, args);
    }
    static Object no(Object obj, String name) throws ReflectiveOperationException { return call(obj, name, new Class<?>[0]); }
    public static String nick(ServerPlayer p) throws ReflectiveOperationException {
        var c=Class.forName("com.ggserver.servernick.NickManager");
        Object manager=call(c,"get",new Class<?>[]{MinecraftServer.class},p.getServer());
        Object value=call(manager,"getNick",new Class<?>[]{UUID.class},p.getUUID());
        return value == null ? "" : value.toString();
    }
    public static void nick(ServerPlayer p, String input, boolean reset) throws ReflectiveOperationException {
        var manager=Class.forName("com.ggserver.servernick.NickManager");
        var format=Class.forName("com.ggserver.servernick.NickFormat");
        String clean=null;
        if (!reset) {
            clean=(String)call(format,"sanitize",new Class<?>[]{String.class},input);
            Object spec=Class.forName("com.ggserver.servernick.NickConfig").getField("MAX_LENGTH").get(null);
            int max=((Number)no(spec,"get")).intValue();
            if(clean==null || clean.isBlank())throw new IllegalArgumentException("닉네임을 입력해 주세요.");
            int length=((Number)call(format,"visibleLength",new Class<?>[]{String.class},clean)).intValue();
            if (clean==null || clean.isBlank() || length>max || clean.codePoints().anyMatch(Character::isISOControl))
                throw new IllegalArgumentException("닉네임 길이와 내용을 확인해 주세요.");
        }
        Object store=call(manager,"get",new Class<?>[]{MinecraftServer.class},p.getServer());
        String name=p.getGameProfile().getName();
        call(store,"setNick",new Class<?>[]{MinecraftServer.class,UUID.class,String.class,String.class,String.class},p.getServer(),p.getUUID(),clean,name,name);
    }
    public record Balance(long spurs, long perSun) {}
    public static Optional<Balance> balance(ServerPlayer p) {
        try {
            Class<?> mod=Class.forName("dev.ithundxr.createnumismatics.Numismatics");
            Object bank=mod.getField("BANK").get(null);
            // UUID overload is read-only: do not create an account or deposit starter funds.
            Object account=call(bank,"getAccount",new Class<?>[]{UUID.class},p.getUUID());
            if (account==null) return Optional.empty();
            Object raw=no(account,"getBalance");
            if (!(raw instanceof Number n)) return Optional.empty();
            long amount=new java.math.BigDecimal(n.toString()).longValueExact();
            // Upstream 1.1.0 stores overflow separately from its int getBalance().
            // Read verified field without invoking save(), which mutates the trust list.
            try {
                Field extra=account.getClass().getDeclaredField("additionalBalance");
                if(!extra.trySetAccessible())return Optional.empty();
                long overflow=extra.getLong(account);
                if(overflow<0)return Optional.empty();
                amount=Math.addExact(amount,overflow);
            } catch(NoSuchFieldException absent) {
                // Unknown implementations cannot prove that getBalance includes all reserves.
                return Optional.empty();
            }
            Class<?> coin=Class.forName("dev.ithundxr.createnumismatics.content.backend.Coin");
            Object sun=coin.getField("SUN").get(null);
            long unit=((Number)coin.getField("value").get(sun)).longValue();
            if (amount<0 || unit<=0) return Optional.empty();
            return Optional.of(new Balance(amount,unit));
        } catch (ReflectiveOperationException|RuntimeException|LinkageError e) { failure("balance",e); return Optional.empty(); }
    }
    public record Season(String name, int cycleTicks, int cycleLength) {}
    public static Optional<Season> season(ServerPlayer p) {
        try {
            var c=Class.forName("sereneseasons.api.season.SeasonHelper");
            Object s=call(c,"getSeasonState",new Class<?>[]{net.minecraft.world.level.Level.class},p.getServer().overworld());
            return Optional.of(new Season(no(s,"getSeason").toString(),((Number)no(s,"getSeasonCycleTicks")).intValue(),((Number)no(s,"getCycleDuration")).intValue()));
        } catch(ReflectiveOperationException|RuntimeException|LinkageError e) { failure("season",e); return Optional.empty(); }
    }
    public static CompoundTag skills(ServerPlayer p) {
        CompoundTag out=new CompoundTag(); ListTag list=new ListTag();
        try {
            var c=Class.forName("net.puffish.skillsmod.api.SkillsAPI");
            try (Stream<?> categories=(Stream<?>)no(c,"streamCategories")) {
                for (Object category:categories.limit(128).toList()) {
                    CompoundTag row=new CompoundTag();row.putString("id",no(category,"getId").toString());
                    row.putBoolean("unlocked",(Boolean)call(category,"isUnlocked",new Class<?>[]{ServerPlayer.class},p));
                    Optional<?> xp=(Optional<?>)no(category,"getExperience");
                    if (xp.isPresent()) {
                        Object x=xp.get();
                        int level=((Number)call(x,"getLevel",new Class<?>[]{ServerPlayer.class},p)).intValue();
                        row.putInt("level",level);
                        row.putInt("xp",((Number)call(x,"getCurrent",new Class<?>[]{ServerPlayer.class},p)).intValue());
                        row.putInt("required",((Number)call(x,"getRequired",new Class<?>[]{ServerPlayer.class,int.class},p,level)).intValue());
                        row.putBoolean("hasXp",true);
                    }
                    ListTag unlocked=new ListTag();
                    try(Stream<?> ss=(Stream<?>)call(category,"streamUnlockedSkills",new Class<?>[]{ServerPlayer.class},p)) {
                        for(Object skill:ss.limit(256).toList()) unlocked.add(StringTag.valueOf(no(skill,"getId").toString()));
                    }
                    row.put("skills",unlocked);list.add(row);
                }
            }
            out.putBoolean("available",true);
            out.putString("effectsNote","Skills 0.16.1 공개 API는 효과 설명을 제공하지 않습니다. 원본 스킬 트리에서 확인해 주세요.");
        } catch(ReflectiveOperationException|RuntimeException|LinkageError e){ failure("skills",e);out.putString("error","생활 기술 API를 조회할 수 없습니다."); }
        out.put("categories",list);return out;
    }
}
