package dev.sunlit.reforged.mixin;
import dev.sunlit.reforged.Effects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(ItemStack.class)
public abstract class DurabilityMixin{
    @ModifyVariable(method={"hurt(ILnet/minecraft/util/RandomSource;Lnet/minecraft/server/level/ServerPlayer;)Z","m_220157_(ILnet/minecraft/util/RandomSource;Lnet/minecraft/server/level/ServerPlayer;)Z"},at=@At("HEAD"),argsOnly=true,ordinal=0,remap=false,require=1)
    private int sunlit$durability(int value,int original,RandomSource random,ServerPlayer p){return Effects.durability((ItemStack)(Object)this,value,random,p);}
}
