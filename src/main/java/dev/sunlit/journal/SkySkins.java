package dev.sunlit.journal;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import java.util.Set;

/** Vanilla-only model swaps; no item replacement, durability, enchantment or attribute changes. */
public final class SkySkins {
    private static final Set<String> TIERS=Set.of("wooden","stone","iron","golden","diamond","netherite");
    static String kind(ItemStack stack){
        var id=BuiltInRegistries.ITEM.getKey(stack.getItem());
        if(!id.getNamespace().equals("minecraft"))return "";
        for(String tier:TIERS){if(id.getPath().equals(tier+"_sword"))return "sword";if(id.getPath().equals(tier+"_pickaxe"))return "pickaxe";}
        return "";
    }
    public static boolean apply(ServerPlayer player,String requested){
        if(!requested.equals("sword")&&!requested.equals("pickaxe")&&!requested.equals("clear"))return false;
        ItemStack stack=player.getMainHandItem();CompoundTag tag=stack.getTag();
        if(requested.equals("clear")){
            if(tag==null||(!tag.contains("SunlitSkySkin")&&!tag.contains(FantasySkins.KEY)))return false;
            tag.remove("SunlitSkySkin");tag.remove(FantasySkins.KEY);if(tag.isEmpty())stack.setTag(null);
            player.getInventory().setChanged();player.containerMenu.broadcastChanges();return true;
        }
        var state=SkyGarden.player(player);
        if(!state.getBoolean("complete")||!kind(stack).equals(requested))return false;
        if(tag!=null&&tag.contains("CustomModelData"))return false;
        stack.getOrCreateTag().remove(FantasySkins.KEY);stack.getOrCreateTag().putString("SunlitSkySkin",requested);
        player.getInventory().setChanged();player.containerMenu.broadcastChanges();return true;
    }
}
