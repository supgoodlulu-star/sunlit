package dev.sunlit.reforged;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
import net.minecraft.util.RandomSource;
import java.util.*;

public record Potential(Tier tier,List<Line> lines) {
    public static final String KEY="sunlit_reforged";
    public enum Tier { RARE("레어",0x55aaff),EPIC("에픽",0xbb66ff),UNIQUE("유니크",0xffcc44),LEGENDARY("레전드리",0x55dd88);public final String label;public final int color;Tier(String n,int c){label=n;color=c;} }
    public enum Gear { WEAPON,ARMOR,PICKAXE,HOE,SHOVEL,ROD }
    public enum Cube { SUSPICIOUS,ARTISAN,MASTER }
    public enum Option {
        ATTACK_PERCENT("공격력",true,Gear.WEAPON),ATTACK_FLAT("공격력",false,Gear.WEAPON),ATTACK_SPEED("공격 속도",true,Gear.WEAPON),LIFESTEAL("흡혈",true,Gear.WEAPON),EXECUTE("낮은 체력 적 추가 피해",true,Gear.WEAPON),
        HEALTH_PERCENT("최대 체력",true,Gear.ARMOR),HEALTH_FLAT("최대 체력",false,Gear.ARMOR),ARMOR("방어력",false,Gear.ARMOR),REDUCTION("받는 피해 감소",true,Gear.ARMOR),KNOCKBACK("밀치기 저항",true,Gear.ARMOR),REFLECT("반사 피해",false,Gear.ARMOR),
        MINING_SPEED("채광 속도",true,Gear.PICKAXE),ORE_BONUS("광물 추가 획득 확률",true,Gear.PICKAXE),CROP_BONUS("작물 추가 수확 확률",true,Gear.HOE),RARE_FISH("희귀 어획 확률",true,Gear.ROD),LIFE_XP("생활 경험치",true,Gear.PICKAXE,Gear.HOE,Gear.SHOVEL,Gear.ROD),
        MOVEMENT("이동 속도",true,Gear.ARMOR),JUMP("점프력",true,Gear.ARMOR),DURABILITY("내구도 소모 방지 확률",true,Gear.values());
        public final String label;public final boolean percent;public final Set<Gear> gear;
        Option(String n,boolean p,Gear... g){label=n;percent=p;gear=Set.of(g);}
        public String id(){return name().toLowerCase(Locale.ROOT);}
    }
    public record Line(Option option,Tier tier,double value){public Line{Objects.requireNonNull(option);Objects.requireNonNull(tier);if(!Double.isFinite(value)||value<0)throw new IllegalArgumentException("Invalid line value");}public String label(){return option.label+" +"+java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()+(option.percent?"%":"");}}
    public Potential{Objects.requireNonNull(tier);lines=List.copyOf(lines);if(lines.size()!=3||lines.stream().anyMatch(l->l.tier.ordinal()>tier.ordinal()))throw new IllegalArgumentException("Invalid three-line potential");}
    public CompoundTag save(){var n=new CompoundTag();n.putInt("schemaVersion",1);n.putString("potentialTier",tier.name());var list=new ListTag();for(var l:lines){var t=new CompoundTag();t.putString("optionId",l.option.name());t.putString("lineTier",l.tier.name());t.putDouble("value",l.value);list.add(t);}n.put("lines",list);return n;}
    public static Optional<Potential> load(CompoundTag n){try{if(n.getInt("schemaVersion")!=1)return Optional.empty();var list=n.getList("lines",10);if(list.size()!=3)return Optional.empty();var out=new ArrayList<Line>();for(var tag:list){var t=(CompoundTag)tag;out.add(new Line(Option.valueOf(t.getString("optionId")),Tier.valueOf(t.getString("lineTier")),t.getDouble("value")));}return Optional.of(new Potential(Tier.valueOf(n.getString("potentialTier")),out));}catch(IllegalArgumentException e){return Optional.empty();}}
    public static Optional<Potential> get(ItemStack stack){return stack.hasTag()?load(stack.getTag().getCompound(KEY)):Optional.empty();}
    public void apply(ItemStack stack){stack.getOrCreateTag().put(KEY,save());}
    public Potential withTier(Tier tier){return new Potential(tier,lines);}
    public static Optional<Gear> gear(ItemStack s){
        Item i=s.getItem();String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(i).toString();
        if(i==Items.FISHING_ROD)return Optional.of(Gear.ROD);
        if(!id.startsWith("minecraft:diamond_")&&!id.startsWith("minecraft:netherite_"))return Optional.empty();
        if(i instanceof SwordItem||i instanceof AxeItem)return Optional.of(Gear.WEAPON);if(i instanceof ArmorItem)return Optional.of(Gear.ARMOR);if(i instanceof PickaxeItem)return Optional.of(Gear.PICKAXE);if(i instanceof HoeItem)return Optional.of(Gear.HOE);if(i instanceof ShovelItem)return Optional.of(Gear.SHOVEL);return Optional.empty();
    }
    public static Potential roll(Optional<Potential> old,Gear gear,Cube cube,RandomSource rng,Rules rules){
        Tier tier=old.map(Potential::tier).orElse(Tier.RARE);
        if(old.isPresent()&&tier!=Tier.LEGENDARY&&rng.nextDouble()<rules.upgrades[cube.ordinal()][tier.ordinal()]/100)tier=Tier.values()[tier.ordinal()+1];
        var pool=Arrays.stream(Option.values()).filter(o->o.gear.contains(gear)&&rules.weights[o.ordinal()]>0).toList();if(pool.isEmpty())throw new IllegalStateException("No enabled options for "+gear);
        long total=pool.stream().mapToLong(o->rules.weights[o.ordinal()]).sum();var lines=new ArrayList<Line>();
        for(int i=0;i<3;i++){
            Tier lineTier=tier;if(i>0&&tier!=Tier.RARE&&rng.nextDouble()>=rules.sameTier[tier.ordinal()]/100)lineTier=Tier.values()[tier.ordinal()-1];
            long pick=(long)(rng.nextDouble()*total);Option selected=pool.get(pool.size()-1);for(var o:pool){pick-=rules.weights[o.ordinal()];if(pick<0){selected=o;break;}}
            lines.add(new Line(selected,lineTier,rules.values[selected.ordinal()][lineTier.ordinal()]));
        }
        return new Potential(tier,lines);
    }
}
