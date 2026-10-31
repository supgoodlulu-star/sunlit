package dev.sunlit.reforged;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.loot.*;
import net.minecraftforge.registries.*;
import java.util.*;

public final class LootBonus extends LootModifier{
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> REGISTER=DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,SunlitReforged.ID);
    public static final RegistryObject<Codec<LootBonus>> TYPE=REGISTER.register("activity",()->RecordCodecBuilder.create(i->codecStart(i).apply(i,LootBonus::new)));
    public LootBonus(LootItemCondition[] conditions){super(conditions);}
    @Override public Codec<? extends IGlobalLootModifier> codec(){return TYPE.get();}
    @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot,LootContext context){
        var entity=context.getParamOrNull(LootContextParams.THIS_ENTITY);var state=context.getParamOrNull(LootContextParams.BLOCK_STATE);var origin=context.getParamOrNull(LootContextParams.ORIGIN);var tool=context.getParamOrNull(LootContextParams.TOOL);
        if(state!=null&&entity instanceof ServerPlayer p&&origin!=null&&Activity.eligible(p)&&tool!=null){
            var level=context.getLevel();var pos=BlockPos.containing(origin);boolean ore=state.is(Tags.Blocks.ORES)&&!loot.isEmpty()&&(!state.requiresCorrectToolForDrops()||tool.isCorrectToolForDrops(state))&&!Ledger.get(p.getServer()).isPlaced(level,pos);
            boolean crop=state.getBlock() instanceof CropBlock c&&c.isMaxAge(state);
            if((!ore&&!crop)||!Activity.once(p,level,pos))return loot;
            var eligible=loot.stream().filter(s->!s.isEmpty()&&!(s.getItem() instanceof SunlitReforged.CubeItem)&&(!ore||s.is(Tags.Items.GEMS)||s.is(Tags.Items.RAW_MATERIALS)||s.is(Tags.Items.INGOTS)||s.is(Tags.Items.DUSTS)||s.is(Tags.Items.ORES)||s.is(net.minecraft.tags.ItemTags.COALS))&&(!crop||s.isEdible()||s.getItem()!=state.getBlock().asItem())).toList();
            double bonus=Effects.sum(tool,ore?Potential.Option.ORE_BONUS:Potential.Option.CROP_BONUS);
            if(!eligible.isEmpty()&&context.getRandom().nextDouble()<Effects.chance(bonus))loot.add(eligible.get(context.getRandom().nextInt(eligible.size())).copyWithCount(1));
            if(context.getRandom().nextDouble()<Rules.get().rewardRates[ore?0:1]/100)loot.add(new ItemStack(SunlitReforged.SUSPICIOUS.get()));
        }
        // Fishing table IDs/rare-fish classification depend on the actual Stardew/Aquaculture pack.
        // Fail closed until a verified combined rare-fish+treasure table is provided by a data pack.
        if(state==null&&entity instanceof net.minecraft.world.entity.projectile.FishingHook hook&&hook.getPlayerOwner() instanceof ServerPlayer p&&Activity.eligible(p)){
            boolean treasure=FishingTrace.consume(context);
            if(!Rules.get().rareFishingTable.isBlank()){
                double chance=Effects.sum(p,Potential.Option.RARE_FISH);ResourceLocation id=ResourceLocation.tryParse(Rules.get().rareFishingTable);
                if(id!=null&&context.getRandom().nextDouble()<Effects.chance(chance)){
                    var table=p.getServer().getLootData().getLootTable(id);if(table!=LootTable.EMPTY){var rare=new ObjectArrayList<ItemStack>();table.getRandomItemsRaw(context,rare::add);if(!rare.isEmpty()){loot.clear();loot.addAll(rare);treasure|=FishingTrace.consume(context);}}
                }
            }
            if(treasure&&!loot.isEmpty()&&Activity.fishingOnce(hook,"cube")&&context.getRandom().nextDouble()<Rules.get().rewardRates[2]/100)loot.add(new ItemStack(SunlitReforged.SUSPICIOUS.get()));
        }
        return loot;
    }
}
