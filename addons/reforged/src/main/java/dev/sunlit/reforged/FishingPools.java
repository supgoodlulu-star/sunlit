package dev.sunlit.reforged;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.ForgeRegistries;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class FishingPools extends SimpleJsonResourceReloadListener{
    public record Fish(String id,int weight,boolean night,boolean rain,boolean clear){}
    private record Data(int maxWeight,Map<String,List<Fish>> pools){}
    private static volatile Data data=defaults();
    public FishingPools(){super(new Gson(),"fishing_pools");}
    private static Data defaults(){try(var in=FishingPools.class.getResourceAsStream("/data/sunlit_reforged/fishing_pools/society_4_1_5a.json")){return parse(JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(in),StandardCharsets.UTF_8)).getAsJsonObject());}catch(IOException e){throw new IllegalStateException(e);}}
    private static Data parse(JsonObject root){
        int max=root.get("rareMaxWeight").getAsInt();if(max<1)throw new IllegalArgumentException("Rare weight must be positive");var pools=new LinkedHashMap<String,List<Fish>>();
        for(var e:root.getAsJsonObject("pools").entrySet()){var rows=new ArrayList<Fish>();for(var value:e.getValue().getAsJsonArray()){var f=value.getAsJsonObject();String id=f.get("fish").getAsString();int weight=f.get("weight").getAsInt();if(ResourceLocation.tryParse(id)==null||weight<1||weight>1000000)throw new IllegalArgumentException("Invalid fish entry");rows.add(new Fish(id,weight,f.has("night")&&f.get("night").getAsBoolean(),f.has("requiresRain")&&f.get("requiresRain").getAsBoolean(),f.has("requiresClear")&&f.get("requiresClear").getAsBoolean()));}pools.put(e.getKey(),List.copyOf(rows));}
        return new Data(max,Map.copyOf(pools));
    }
    public static int rareMaxWeight(){return data.maxWeight;}
    public static List<Fish> eligible(String season,String region,boolean night,boolean rain,boolean thunder){
        if(!rain&&thunder)return List.of();String key=season.toLowerCase(Locale.ROOT)+region;return data.pools.getOrDefault(key,List.of()).stream().filter(f->f.night==night&&(!f.rain||rain)&&(!f.clear||!rain&&!thunder)).toList();
    }
    static Optional<Fish> choose(List<Fish> pool,net.minecraft.util.RandomSource random){
        var rows=pool.stream().filter(f->f.weight<=data.maxWeight).filter(f->{var item=ForgeRegistries.ITEMS.getValue(new ResourceLocation(f.id));return item!=null&&item!=Items.AIR;}).toList();int total=rows.stream().mapToInt(Fish::weight).sum();if(total==0)return Optional.empty();int pick=random.nextInt(total);for(var row:rows){pick-=row.weight;if(pick<0)return Optional.of(row);}return Optional.empty();
    }
    public static boolean ready(){return !Rules.get().rareFishingTable.isBlank()||StardewBridge.installed&&net.minecraftforge.fml.ModList.get().isLoaded("sereneseasons");}
    @Override protected void apply(Map<ResourceLocation,JsonElement> files,ResourceManager manager,ProfilerFiller profiler){var root=files.get(new ResourceLocation("sunlit_reforged","society_4_1_5a"));data=root==null?defaults():parse(root.getAsJsonObject());}
}
