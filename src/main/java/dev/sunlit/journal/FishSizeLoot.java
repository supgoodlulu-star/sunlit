package dev.sunlit.journal;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.*;
import net.minecraftforge.registries.*;
public final class FishSizeLoot extends LootModifier {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> REGISTER=DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,SunlitJournal.MODID);
    public static final RegistryObject<Codec<FishSizeLoot>> TYPE=REGISTER.register("fish_sizes",()->RecordCodecBuilder.create(i->codecStart(i).apply(i,FishSizeLoot::new)));
    public FishSizeLoot(LootItemCondition[] conditions){super(conditions);}
    @Override public Codec<? extends IGlobalLootModifier> codec(){return TYPE.get();}
    @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot,LootContext context){
        if(context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof net.minecraft.world.entity.projectile.FishingHook hook&&hook.getPlayerOwner() instanceof net.minecraft.server.level.ServerPlayer p)FishSizes.decorate(loot,p,context.getRandom());
        return loot;
    }
}
