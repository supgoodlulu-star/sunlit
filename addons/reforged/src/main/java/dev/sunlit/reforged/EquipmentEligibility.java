package dev.sunlit.reforged;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import java.util.Optional;

/** Shared by the server slot, roll validation, effects and client UI. */
public final class EquipmentEligibility {
    private EquipmentEligibility() {}
    static TagKey<Item> tag(String id){return TagKey.create(Registries.ITEM,new ResourceLocation(id));}
    static final TagKey<Item> RODS=tag("forge:tools/fishing_rods"),KNIVES=tag("forge:tools/knives");
    static final TagKey<Item> EXCLUDED=tag("sunlit_reforged:reforgeable/excluded");
    private static final java.util.Map<Potential.Gear,TagKey<Item>> CATEGORIES=new java.util.EnumMap<>(Potential.Gear.class);
    static {for(var gear:Potential.Gear.values())CATEGORIES.put(gear,tag("sunlit_reforged:reforgeable/"+gear.name().toLowerCase(java.util.Locale.ROOT)));}
    public static TagKey<Item> allowed(Potential.Gear gear){return CATEGORIES.get(gear);}
    public static Optional<Potential.Gear> gear(ItemStack stack){
        if(stack.isEmpty())return Optional.empty();
        // Explicit data-pack categories also support custom pack weapons with nonstandard tiers.
        for(var gear:Potential.Gear.values())if(stack.is(allowed(gear)))return Optional.of(gear);
        Item item=stack.getItem();
        if(item instanceof FishingRodItem||stack.is(RODS))return Optional.of(Potential.Gear.ROD);
        if(item instanceof ArmorItem armor){
            var material=armor.getMaterial();
            return material==ArmorMaterials.DIAMOND||material==ArmorMaterials.NETHERITE?Optional.of(Potential.Gear.ARMOR):Optional.empty();
        }
        if(stack.is(EXCLUDED))return Optional.empty();
        var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
        // The pack raises gold's harvest level; that does not promote it to diamond equipment.
        if(id.getNamespace().equals("minecraft")&&!id.getPath().startsWith("diamond_")&&!id.getPath().startsWith("netherite_"))return Optional.empty();
        if(!(item instanceof TieredItem tool)||tool.getTier().getLevel()<Tiers.DIAMOND.getLevel())return Optional.empty();
        // Use actual item classes/tags, not registry-name prefixes or durability.
        if(item instanceof SwordItem||item instanceof AxeItem||stack.is(ItemTags.SWORDS)||stack.is(ItemTags.AXES)||stack.is(KNIVES))return Optional.of(Potential.Gear.WEAPON);
        if(item instanceof PickaxeItem||stack.is(ItemTags.PICKAXES))return Optional.of(Potential.Gear.PICKAXE);
        if(item instanceof HoeItem||stack.is(ItemTags.HOES))return Optional.of(Potential.Gear.HOE);
        if(item instanceof ShovelItem||stack.is(ItemTags.SHOVELS))return Optional.of(Potential.Gear.SHOVEL);
        return Optional.empty();
    }
}
