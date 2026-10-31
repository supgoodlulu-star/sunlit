package dev.sunlit.reforged.mixin;
import dev.sunlit.reforged.StardewBridge;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets="com.bonker.stardewfishing.common.FishingHookLogic",remap=false)
public abstract class StardewStartMixin{
    @Inject(method="startStardewMinigame",at=@At("HEAD"),remap=false,require=1)
    private static void sunlit$rareFish(ServerPlayer player,CallbackInfoReturnable<Boolean> cir){StardewBridge.reroll(player);}
}
