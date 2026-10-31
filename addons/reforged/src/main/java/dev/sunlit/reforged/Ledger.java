package dev.sunlit.reforged;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** Per-world authoritative escrow and deduplication, independent of Journal/Reforged NBT. */
public final class Ledger extends SavedData{
    private final CompoundTag root;public Ledger(){this(new CompoundTag());}public Ledger(CompoundTag n){root=n.copy();}
    public static Ledger get(MinecraftServer s){return s.overworld().getDataStorage().computeIfAbsent(Ledger::new,Ledger::new,"sunlit_reforged");}
    @Override public CompoundTag save(CompoundTag n){n.merge(root);return n;}
    static CompoundTag child(CompoundTag n,String key){if(!n.contains(key,10))n.put(key,new CompoundTag());return n.getCompound(key);}
    public CompoundTag station(UUID owner){return child(child(root,"stations"),owner.toString());}
    public boolean reward(UUID owner,String id){var rewards=child(child(root,"rewards"),owner.toString());if(rewards.contains(id))return false;rewards.putBoolean(id,true);setDirty();return true;}
    public void placed(net.minecraft.server.level.ServerLevel level,net.minecraft.core.BlockPos pos){child(root,"placedOre").putBoolean(level.dimension().location()+":"+pos.asLong(),true);setDirty();}
    public boolean isPlaced(net.minecraft.server.level.ServerLevel level,net.minecraft.core.BlockPos pos){return root.getCompound("placedOre").contains(level.dimension().location()+":"+pos.asLong());}
    public void forget(net.minecraft.server.level.ServerLevel level,net.minecraft.core.BlockPos pos){root.getCompound("placedOre").remove(level.dimension().location()+":"+pos.asLong());setDirty();}
}
