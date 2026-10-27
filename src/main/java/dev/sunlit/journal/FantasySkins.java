package dev.sunlit.journal;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.Set;

/** Cosmetic marker only: original item, attributes, enchantments and durability remain intact. */
public final class FantasySkins {
    public static final String KEY="SunlitFantasySkin";
    private static final Set<String> TIERS=Set.of("wooden","stone","iron","golden","diamond","netherite");
    public static String kind(ItemStack stack){
        var id=BuiltInRegistries.ITEM.getKey(stack.getItem());if(!id.getNamespace().equals("minecraft"))return "";
        for(String tier:TIERS)for(String kind:new String[]{"sword","pickaxe","axe","shovel","hoe"})if(id.getPath().equals(tier+"_"+kind))return kind;
        return "";
    }
    public static boolean apply(ServerPlayer p,String id){
        var skin=FantasyGacha.find(id);var stack=p.getMainHandItem();
        if(skin==null||!FantasyGacha.owns(p,id)||!kind(stack).equals(skin.kind()))return false;
        if(stack.hasTag()&&stack.getTag().contains("CustomModelData"))return false;
        var tag=stack.getOrCreateTag();tag.remove("SunlitSkySkin");tag.putString(KEY,id);
        p.getInventory().setChanged();p.containerMenu.broadcastChanges();return true;
    }
}
