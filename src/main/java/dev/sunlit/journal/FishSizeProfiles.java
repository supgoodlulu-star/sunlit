package dev.sunlit.journal;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Game length references, not biological maximums. Price bands are independent of species length. */
public final class FishSizeProfiles extends SimpleJsonResourceReloadListener {
    private static volatile Map<String,Double> lengths=defaults();
    public FishSizeProfiles(){super(new Gson(),"fish_size_profiles");}
    static Map<String,Double> defaults(){try(var in=FishSizeProfiles.class.getResourceAsStream("/data/sunlit_journal_core/fish_size_profiles/default.json")){return parse(JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(in),StandardCharsets.UTF_8)).getAsJsonObject());}catch(IOException e){throw new IllegalStateException(e);}}
    static Map<String,Double> parse(JsonObject root){var out=new HashMap<String,Double>();for(var entry:root.entrySet()){double d=entry.getValue().getAsDouble();if(ResourceLocation.tryParse(entry.getKey())==null||!Double.isFinite(d)||d<.2||d>10000)throw new IllegalArgumentException("Invalid fish reference length: "+entry.getKey());out.put(entry.getKey(),d);}return Map.copyOf(out);}
    static String id(ItemStack s){return BuiltInRegistries.ITEM.getKey(s.getItem()).toString();}
    public static boolean known(ItemStack s){return lengths.containsKey(id(s));}
    public static double baseCm(ItemStack s){return lengths.getOrDefault(id(s),40d);}
    @Override protected void apply(Map<ResourceLocation,JsonElement> files,ResourceManager manager,ProfilerFiller profiler){var config=files.get(new ResourceLocation("sunlit_journal_core","default"));lengths=config==null?defaults():parse(config.getAsJsonObject());}
}
