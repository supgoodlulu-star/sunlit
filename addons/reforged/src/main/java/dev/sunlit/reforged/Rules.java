package dev.sunlit.reforged;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class Rules extends SimpleJsonResourceReloadListener {
    public final double[][] values=new double[19][4];public final int[] weights=new int[19];
    public final double[][] upgrades={{8,0,0,0},{15,5,0,0},{25,12,3,0}};
    public final double[] sameTier={100,20,15,10};public final int[] prices={500,2500,10000,3000};
    public final double[] rewardRates={.3,.1,2,5,10,2};
    public double lowHealth=.30;public String hammer="",rareFishingTable="";
    public List<String> skillCategories=List.of("society:farming","society:fishing","society:mining","society:cooking","society:husbandry");
    private static volatile Rules active=defaults();
    public Rules(){super(new Gson(),"potential_rules");}
    public static Rules get(){return active;}
    public static Rules defaults(){try(var in=Rules.class.getResourceAsStream("/data/sunlit_reforged/potential_rules/default.json")){return parse(JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(in),StandardCharsets.UTF_8)).getAsJsonObject());}catch(IOException e){throw new IllegalStateException(e);}}
    static double number(JsonElement n){double d=n.getAsDouble();if(!Double.isFinite(d)||d<0)throw new IllegalArgumentException("Expected finite non-negative number");return d;}
    public static Rules parse(JsonObject root){
        Rules r=new Rules();var opts=root.getAsJsonArray("options");var seen=EnumSet.noneOf(Potential.Option.class);
        for(var entry:opts){var o=entry.getAsJsonObject();var type=Potential.Option.valueOf(o.get("id").getAsString().toUpperCase(Locale.ROOT));if(!seen.add(type))throw new IllegalArgumentException("Duplicate option");r.weights[type.ordinal()]=o.get("weight").getAsInt();if(r.weights[type.ordinal()]<0||r.weights[type.ordinal()]>1000000)throw new IllegalArgumentException("Invalid weight");var v=o.getAsJsonArray("values");if(v.size()!=4)throw new IllegalArgumentException("Four tier values required");for(int i=0;i<4;i++)r.values[type.ordinal()][i]=number(v.get(i));}
        if(seen.size()!=19)throw new IllegalArgumentException("Exactly 19 options required");
        for(var gear:Potential.Gear.values())if(Arrays.stream(Potential.Option.values()).noneMatch(o->o.gear.contains(gear)&&r.weights[o.ordinal()]>0))throw new IllegalArgumentException("Empty gear pool");
        var cubes=root.getAsJsonArray("upgrades");if(cubes.size()!=3)throw new IllegalArgumentException("Three cubes required");for(int c=0;c<3;c++){var a=cubes.get(c).getAsJsonArray();if(a.size()!=4)throw new IllegalArgumentException();for(int i=0;i<4;i++){r.upgrades[c][i]=number(a.get(i));if(r.upgrades[c][i]>100)throw new IllegalArgumentException();}}
        if(r.upgrades[0][1]!=0||r.upgrades[0][2]!=0||r.upgrades[0][3]!=0||r.upgrades[1][2]!=0||r.upgrades[1][3]!=0||r.upgrades[2][3]!=0)throw new IllegalArgumentException("Cube tier limits are fixed");
        var chances=root.getAsJsonArray("sameTier");if(chances.size()!=4)throw new IllegalArgumentException();for(int i=0;i<4;i++){r.sameTier[i]=number(chances.get(i));if(r.sameTier[i]>100)throw new IllegalArgumentException();}if(r.sameTier[0]!=100)throw new IllegalArgumentException("Rare lines stay rare");
        var prices=root.getAsJsonArray("pricesSun");if(prices.size()!=4)throw new IllegalArgumentException();for(int i=0;i<4;i++){r.prices[i]=prices.get(i).getAsInt();if(r.prices[i]<=0)throw new IllegalArgumentException("Price exceeds safe transaction range");}
        var rates=root.getAsJsonArray("rewardRates");if(rates.size()!=6)throw new IllegalArgumentException();for(int i=0;i<6;i++){r.rewardRates[i]=number(rates.get(i));if(r.rewardRates[i]>100)throw new IllegalArgumentException();}
        r.lowHealth=number(root.get("lowHealthFraction"));if(r.lowHealth>1)throw new IllegalArgumentException();r.hammer=root.get("verifiedHammerItem").getAsString();r.rareFishingTable=root.get("verifiedRareFishingTable").getAsString();
        for(String id:List.of(r.hammer,r.rareFishingTable))if(!id.isBlank()&&ResourceLocation.tryParse(id)==null)throw new IllegalArgumentException("Invalid integration ID");
        r.skillCategories=new ArrayList<>();for(var id:root.getAsJsonArray("skillCategories")){String name=id.getAsString();if(ResourceLocation.tryParse(name)==null)throw new IllegalArgumentException();r.skillCategories.add(name);}return r;
    }
    @Override protected void apply(Map<ResourceLocation,JsonElement> files,ResourceManager manager,ProfilerFiller profiler){
        JsonElement entry=files.get(new ResourceLocation("sunlit_reforged","default"));try{active=entry==null?defaults():parse(entry.getAsJsonObject());SunlitReforged.LOG.info("Loaded Sunlit potential rules: 19 options");}catch(RuntimeException e){throw new IllegalArgumentException("Invalid Sunlit potential rules; reload rejected",e);}
    }
}
