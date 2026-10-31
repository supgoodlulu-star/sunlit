package dev.sunlit.reforged;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootContext;
import java.util.*;
/** Tracks the actual nested treasure table, not item-name guesses. Weak keys do not retain loot contexts. */
public final class FishingTrace{
    private static final Map<LootContext,Boolean> TREASURE=Collections.synchronizedMap(new WeakHashMap<>());
    public static void visit(ResourceLocation table,LootContext context){if(table!=null&&table.equals(new ResourceLocation("minecraft","gameplay/fishing/treasure")))TREASURE.put(context,true);}
    public static boolean consume(LootContext context){return TREASURE.remove(context)!=null;}
}
