package dev.sunlit.journal;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public final class PetContent {
 static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,SunlitJournal.MODID);
 static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,SunlitJournal.MODID);
 static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,SunlitJournal.MODID);
 public static final RegistryObject<Item> TICKET=ITEMS.register("pet_ticket",()->new Item(new Item.Properties()));
 public static final RegistryObject<Item> DOLL=ITEMS.register("pet_doll",Doll::new);
 public static final RegistryObject<Block> VENDING=BLOCKS.register("pet_vending",()->new Counter("draw"));
 public static final RegistryObject<Block> SHOP=BLOCKS.register("pet_accessory_shop",()->new Counter("shop"));
 public static final RegistryObject<EntityType<PetEntity>> PET=ENTITIES.register("companion",()->EntityType.Builder.<PetEntity>of(PetEntity::new,MobCategory.CREATURE).sized(.65f,.8f).clientTrackingRange(10).updateInterval(3).build("sunlit_companion"));
 static {ITEMS.register("pet_vending",()->new BlockItem(VENDING.get(),new Item.Properties()));ITEMS.register("pet_accessory_shop",()->new BlockItem(SHOP.get(),new Item.Properties()));}
 public static void register(IEventBus bus){ITEMS.register(bus);BLOCKS.register(bus);ENTITIES.register(bus);bus.addListener(PetContent::attributes);}
 static void attributes(EntityAttributeCreationEvent e){e.put(PET.get(),net.minecraft.world.entity.Mob.createMobAttributes().add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH,20).add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,.3).add(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE,32).build());}
 static final class Counter extends Block {
  final String mode;
  Counter(String mode){super(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).strength(2.5f).noOcclusion());this.mode=mode;}
  @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){if(player instanceof ServerPlayer p)PetNetwork.openCounter(p,mode,pos);return InteractionResult.sidedSuccess(level.isClientSide);}
 }
 public static final class Doll extends Item {
  Doll(){super(new Item.Properties().stacksTo(1));}
  @Override public Component getName(ItemStack s){return Component.literal(PetCatalog.find(s.getOrCreateTag().getString("species")).map(p->p.name()+" 인형").orElse("등록되지 않은 펫 인형"));}
  @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){var stack=player.getItemInHand(hand);if(player instanceof ServerPlayer p)PetWorld.get(p).register(p,stack);return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);}
  @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> c){c.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){@Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){return PetRender.dollRenderer();}});}
 }
}
