package dev.sunlit.reforged;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.eventbus.api.IEventBus;
import org.slf4j.Logger;

@Mod(SunlitReforged.ID)
public final class SunlitReforged {
    public static final String ID="sunlit_reforged";public static final Logger LOG=LogUtils.getLogger();
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,ID);
    public static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,ID);
    public static final RegistryObject<Item> SUSPICIOUS=ITEMS.register("suspicious_cube",()->new CubeItem(Potential.Cube.SUSPICIOUS));
    public static final RegistryObject<Item> ARTISAN=ITEMS.register("artisan_cube",()->new CubeItem(Potential.Cube.ARTISAN));
    public static final RegistryObject<Item> MASTER=ITEMS.register("master_cube",()->new CubeItem(Potential.Cube.MASTER));
    public static final RegistryObject<MenuType<StationMenu>> STATION=MENUS.register("station",()->net.minecraftforge.common.extensions.IForgeMenuType.create(StationMenu::client));
    public static final ForgeConfigSpec SPEC;public static final ForgeConfigSpec.BooleanValue DEBUG;
    static{var b=new ForgeConfigSpec.Builder();DEBUG=b.define("debug",false);SPEC=b.build();}
    public SunlitReforged(){IEventBus bus=net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();ITEMS.register(bus);MENUS.register(bus);LootBonus.REGISTER.register(bus);Network.register();ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON,SPEC);MinecraftForge.EVENT_BUS.register(new Effects());MinecraftForge.EVENT_BUS.register(new Activity());MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.AddReloadListenerEvent e)->{e.addListener(new Rules());e.addListener(new FishingPools());});MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.RegisterCommandsEvent e)->{
        e.getDispatcher().register(net.minecraft.commands.Commands.literal("재련").executes(c->{StationMenu.open(c.getSource().getPlayerOrException());return 1;}));
        e.getDispatcher().register(net.minecraft.commands.Commands.literal("sunlitreforged").executes(c->{StationMenu.open(c.getSource().getPlayerOrException());return 1;}));
    });JournalBridge.register();StardewBridge.register();}
    public static void debug(String format,Object... args){if(DEBUG.get())LOG.info("[Sunlit Reforged DEBUG] "+format,args);}
    public static Item cube(Potential.Cube c){return switch(c){case SUSPICIOUS->SUSPICIOUS.get();case ARTISAN->ARTISAN.get();case MASTER->MASTER.get();};}
    public static class CubeItem extends Item{
        public final Potential.Cube type;CubeItem(Potential.Cube t){super(new Item.Properties().stacksTo(64).rarity(t==Potential.Cube.MASTER?Rarity.EPIC:t==Potential.Cube.ARTISAN?Rarity.UNCOMMON:Rarity.COMMON));type=t;}
        @Override public void appendHoverText(ItemStack stack,net.minecraft.world.level.Level level,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flags){
            String key="tooltip.sunlit_reforged.dreamdrop.";
            lines.add(net.minecraft.network.chat.Component.translatable(key+"first").withStyle(net.minecraft.ChatFormatting.GRAY));
            String kind=switch(type){case SUSPICIOUS->"suspicious";case ARTISAN->"artisan";case MASTER->"master";};
            lines.add(net.minecraft.network.chat.Component.translatable(key+kind).withStyle(net.minecraft.ChatFormatting.AQUA));
            if(type==Potential.Cube.MASTER){for(String detail:java.util.List.of("master_legendary","choice","promotion"))lines.add(net.minecraft.network.chat.Component.translatable(key+detail).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));}
        }
        @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level,net.minecraft.world.entity.player.Player p,net.minecraft.world.InteractionHand hand){if(p instanceof net.minecraft.server.level.ServerPlayer s)StationMenu.open(s);return net.minecraft.world.InteractionResultHolder.sidedSuccess(p.getItemInHand(hand),level.isClientSide);}
    }
}
