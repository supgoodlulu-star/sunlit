package dev.sunlit.journal;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid=SunlitJournal.MODID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class JournalKeys {
    // Unbound by default: the supplied pack already uses J for JourneyMap and has many hotkeys.
    public static final KeyMapping OPEN_PROFILE=new KeyMapping("key.sunlit_journal_core.open_profile",KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_UNKNOWN,"key.categories.sunlit_journal_core");
    @SubscribeEvent public static void register(RegisterKeyMappingsEvent event){event.register(OPEN_PROFILE);}
}
