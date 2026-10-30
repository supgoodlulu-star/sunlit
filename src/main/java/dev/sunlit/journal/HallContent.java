package dev.sunlit.journal;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.registries.*;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class HallContent {
    static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,SunlitJournal.MODID);
    public static final RegistryObject<EntityType<HallFigure>> FIGURE=ENTITIES.register("hall_figure",()->EntityType.Builder.<HallFigure>of(HallFigure::new,MobCategory.MISC).sized(.6f,1.8f).clientTrackingRange(32).updateInterval(20).build("hall_figure"));
    static void register(IEventBus bus){ENTITIES.register(bus);bus.addListener(HallContent::attributes);}
    static void attributes(EntityAttributeCreationEvent e){e.put(FIGURE.get(),net.minecraft.world.entity.Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED,0).build());}
    static void commands(CommandDispatcher<CommandSourceStack> d){
        d.register(korean(Commands.literal("명예의전당")));
        d.register(Commands.literal("명예의").then(korean(Commands.literal("전당"))));
        d.register(Commands.literal("sunlithall").executes(c->open(c.getSource()))
            .then(Commands.literal("place").requires(s->s.hasPermission(2))
                .then(Commands.argument("category",StringArgumentType.greedyString()).suggests((c,b)->SharedSuggestionProvider.suggest(suggestions(c.getSource()),b))
                    .executes(c->place(c.getSource(),StringArgumentType.getString(c,"category")))))
            .then(Commands.literal("remove").requires(s->s.hasPermission(2)).executes(c->remove(c.getSource()))));
    }
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> korean(com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> root){
        return root.executes(c->open(c.getSource()))
            .then(Commands.literal("제거").requires(s->s.hasPermission(2)).executes(c->remove(c.getSource())))
            .then(Commands.argument("category",StringArgumentType.greedyString()).requires(s->s.hasPermission(2))
                .suggests((c,b)->SharedSuggestionProvider.suggest(suggestions(c.getSource()),b))
                .executes(c->place(c.getSource(),StringArgumentType.getString(c,"category"))));
    }
    private static java.util.Set<String> suggestions(CommandSourceStack source){
        var out=new java.util.TreeSet<>(HallWorld.get(source.getServer()).categories().keySet());
        out.addAll(HallNames.known().values());out.addAll(java.util.List.of("업무","축제","업적"));return out;
    }
    private static int open(CommandSourceStack source)throws com.mojang.brigadier.exceptions.CommandSyntaxException{HallNetwork.open(source.getPlayerOrException(),"tasks");return 1;}
    static String resolve(HallWorld world,String input){
        if(world.categories().containsKey(input))return input;
        if(input.startsWith("skill:")&&HallNames.known().containsKey(input.substring(6)))return input;
        String basic=switch(input){case "업무"->"tasks";case "축제"->"festival";case "업적"->"achievements";default->"";};if(!basic.isEmpty())return basic;
        for(var entry:HallNames.known().entrySet())if(entry.getValue().equals(input))return "skill:"+entry.getKey();
        return "";
    }
    static int place(CommandSourceStack source,String input)throws com.mojang.brigadier.exceptions.CommandSyntaxException{
        var p=source.getPlayerOrException();var world=HallWorld.get(p.getServer());String category=resolve(world,input.strip());
        if(category.isEmpty()){source.sendFailure(Component.literal("등록되지 않은 분야입니다. 농사·낚시·채광·요리·목축·업무·축제·업적 또는 자동완성의 분야 ID를 입력해 주세요."));return 0;}
        if(category.startsWith("skill:")&&!world.categories().containsKey(category))world.declareSkill(category.substring(6));
        var figure=FIGURE.get().create(p.serverLevel());if(figure==null)return 0;
        figure.configure(category,0);figure.moveTo(p.getX(),p.getY(),p.getZ(),p.getYRot(),0);
        if(!p.serverLevel().addFreshEntity(figure))return 0;
        source.sendSuccess(()->Component.literal("현재 위치에 1위 전시 인형 설치 · "+world.categories().get(category)),false);return 1;
    }
    private static int remove(CommandSourceStack source)throws com.mojang.brigadier.exceptions.CommandSyntaxException{
        var p=source.getPlayerOrException();var nearest=p.serverLevel().getEntitiesOfClass(HallFigure.class,p.getBoundingBox().inflate(5)).stream()
            .filter(f->f.distanceToSqr(p)<=25).min(java.util.Comparator.comparingDouble(f->f.distanceToSqr(p)));
        if(nearest.isEmpty()){source.sendFailure(Component.literal("5칸 안에 전시 인형이 없습니다."));return 0;}
        nearest.get().discard();source.sendSuccess(()->Component.literal("가장 가까운 전시 인형을 제거했습니다."),false);return 1;
    }
}
