package dev.sunlit.journal;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.*;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class HallContent {
    static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,SunlitJournal.MODID);
    public static final RegistryObject<EntityType<HallFigure>> FIGURE=ENTITIES.register("hall_figure",()->EntityType.Builder.<HallFigure>of(HallFigure::new,MobCategory.MISC).sized(.6f,1.8f).clientTrackingRange(32).updateInterval(20).build("hall_figure"));
    static void register(IEventBus bus){ENTITIES.register(bus);bus.addListener(HallContent::attributes);}
    static void attributes(EntityAttributeCreationEvent e){e.put(FIGURE.get(),net.minecraft.world.entity.Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED,0).build());}
    static void commands(CommandDispatcher<CommandSourceStack> d){
        d.register(Commands.literal("명예의전당").executes(c->{HallNetwork.open(c.getSource().getPlayerOrException(),"tasks");return 1;}));
        d.register(Commands.literal("sunlithall").executes(c->{HallNetwork.open(c.getSource().getPlayerOrException(),"tasks");return 1;})
            .then(Commands.literal("place").requires(s->s.hasPermission(2)).then(Commands.argument("category",StringArgumentType.word()).suggests((c,b)->SharedSuggestionProvider.suggest(HallWorld.get(c.getSource().getServer()).categories().keySet(),b)).executes(c->{
                var p=c.getSource().getPlayerOrException();String category=StringArgumentType.getString(c,"category");var w=HallWorld.get(p.getServer());if(!w.categories().containsKey(category))return 0;
                // The complete volume must be empty: never overwrite user builds or liquid.
                BlockPos base=p.blockPosition().offset(0,0,3);var level=p.serverLevel();
                for(int x=-5;x<=5;x++)for(int y=0;y<=4;y++)for(int z=0;z<=2;z++)if(!level.isInWorldBounds(base.offset(x,y,z))||!level.getWorldBorder().isWithinBounds(base.offset(x,y,z))||!level.isEmptyBlock(base.offset(x,y,z))){c.getSource().sendFailure(Component.literal("전시대 공간이 필요합니다. 앞쪽 11×3×5 공간을 비워 주세요."));return 0;}
                for(int slot=0;slot<3;slot++){
                    int x=slot==0?0:slot==1?-4:4,height=slot==0?2:1;
                    for(int dx=-1;dx<=1;dx++)for(int dz=0;dz<=2;dz++)for(int y=0;y<height;y++)level.setBlockAndUpdate(base.offset(x+dx,y,dz),Blocks.STRIPPED_OAK_WOOD.defaultBlockState());
                    level.setBlockAndUpdate(base.offset(x-1,height,0),Blocks.POTTED_DANDELION.defaultBlockState());level.setBlockAndUpdate(base.offset(x+1,height,0),Blocks.POTTED_OXEYE_DAISY.defaultBlockState());
                    level.setBlockAndUpdate(base.offset(x-1,height,2),Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT,true));
                    var figure=FIGURE.get().create(level);figure.configure(category,slot);figure.moveTo(base.getX()+x+.5,base.getY()+height,base.getZ()+1.5,180,0);level.addFreshEntity(figure);
                }
                c.getSource().sendSuccess(()->Component.literal("명예의 전당 전시대 설치 완료 · "+w.categories().get(category)),false);return 1;
            })))
            .then(Commands.literal("remove").requires(s->s.hasPermission(2)).executes(c->{var p=c.getSource().getPlayerOrException();var list=p.serverLevel().getEntitiesOfClass(HallFigure.class,p.getBoundingBox().inflate(12));list.forEach(Entity::discard);c.getSource().sendSuccess(()->Component.literal("주변 전시 인형 "+list.size()+"개 제거. 단상 블록은 직접 회수해 주세요."),false);return list.size();})));
    }
}
