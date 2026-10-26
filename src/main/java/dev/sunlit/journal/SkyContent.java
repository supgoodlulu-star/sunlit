package dev.sunlit.journal;

import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;
import net.minecraftforge.registries.*;
import net.minecraftforge.eventbus.api.IEventBus;

public final class SkyContent {
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,SunlitJournal.MODID);
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,SunlitJournal.MODID);
    public static final RegistryObject<Item> COMPASS=ITEMS.register("star_compass",()->new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)){
        @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
            if(!level.isClientSide&&player instanceof ServerPlayer p){SkyGarden.travel(p);p.getCooldowns().addCooldown(this,40);}
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
        }
        @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<Component> lines,TooltipFlag flag){lines.add(Component.translatable("item.sunlit_journal_core.star_compass.hint"));}
    });
    @SuppressWarnings("unchecked") public static final RegistryObject<Block>[] RELICS=new RegistryObject[7];
    static {
        String[] ids={"moon_record","leaf_record","star_record","moon_rune","leaf_rune","star_rune","return_stone"};
        for(int i=0;i<ids.length;i++){
            final int kind=i;RELICS[i]=BLOCKS.register(ids[i],()->new Block(BlockBehaviour.Properties.of().strength(-1,3600000).lightLevel(s->12).noLootTable()){
                @Override public InteractionResult use(net.minecraft.world.level.block.state.BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
                    if(!level.isClientSide&&player instanceof ServerPlayer p&&hand==InteractionHand.MAIN_HAND)SkyGarden.interact(p,pos,kind);
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            });
        }
    }
    public static void register(IEventBus bus){ITEMS.register(bus);BLOCKS.register(bus);}
}
