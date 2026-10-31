package dev.sunlit.reforged.mixin;
import dev.sunlit.reforged.FishingTrace;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.item.ItemStack;
import java.util.function.Consumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LootTable.class)
public abstract class FishingLootMixin{
    @Inject(method={"getRandomItemsRaw(Lnet/minecraft/world/level/storage/loot/LootContext;Ljava/util/function/Consumer;)V","m_79131_(Lnet/minecraft/world/level/storage/loot/LootContext;Ljava/util/function/Consumer;)V"},at=@At("HEAD"),remap=false,require=1)
    private void sunlit$treasure(LootContext context,Consumer<ItemStack> output,CallbackInfo ci){FishingTrace.visit(((LootTable)(Object)this).getLootTableId(),context);}
}
