package dev.sunlit.journal;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

@Mod(SunlitJournal.MODID)
public class SunlitJournal {
    public static final String MODID = "sunlit_journal_core";
    private static final String DATA_KEY = "SunlitJournalTitles";
    public static final String[] TITLES = {
        "새싹농부", "밭의일꾼", "황금손길", "풍년지기",
        "꼬마낚시꾼", "강태공", "파도사냥꾼", "물결지기",
        "돌멩이수집가", "광맥탐색가", "보석장인", "금맥장인",
        "견습요리사", "마을요리사", "별빛셰프", "주방지기",
        "동물친구", "목장의일꾼", "초원의수호자", "초원목자",
        "동전수집가", "알뜰상인", "황금상단주", "억만장자",
        "사계요정", "벚꽃요정", "별빛요정", "풍요요정", "눈꽃요정",
        "구름 위의 손님", "하늘 정원의 수호자"
    };
    private static final Set<String> ALLOWED = new LinkedHashSet<>(Arrays.asList(TITLES));

    public SunlitJournal() {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new JournalEvents());
        MinecraftForge.EVENT_BUS.register(new JournalChat());
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, JournalConfig.SPEC);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.CLIENT, JournalClientConfig.SPEC);
        SkyContent.register(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(new SkyGarden());
        JournalNetwork.register();
    }

    public static CompoundTag data(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(DATA_KEY, 10)) persistent.put(DATA_KEY, new CompoundTag());
        return persistent.getCompound(DATA_KEY);
    }
    public static Set<String> owned(ServerPlayer player) {
        Set<String> result = new LinkedHashSet<>();
        String raw = data(player).getString("owned");
        if (!raw.isEmpty()) for (String name : raw.split("\\|")) if (ALLOWED.contains(name)) result.add(name);
        return result;
    }
    public static void saveOwned(ServerPlayer player, Set<String> titles) {
        data(player).putString("owned", String.join("|", titles));
    }
    private static void reply(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal("[모험가의 수첩] " + message).withStyle(ChatFormatting.GOLD));
    }

    private static void replyTitle(ServerPlayer player,String text,String title) {
        player.sendSystemMessage(Component.literal("[모험가의 수첩] "+text).withStyle(ChatFormatting.GOLD).append(TitleThemes.badge(title)));
    }

    @SubscribeEvent
    public void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("sunlit")
            .then(Commands.literal("journal").executes(ctx -> {
                JournalNetwork.send(ctx.getSource().getPlayerOrException(), true, "profile"); return 1;
            }).then(Commands.argument("page", StringArgumentType.word()).executes(ctx -> {
                String page=StringArgumentType.getString(ctx,"page");
                if(!Set.of("profile","skills","quests","achievements","secrets").contains(page))return 0;
                JournalNetwork.send(ctx.getSource().getPlayerOrException(),true,page);return 1;
            })))
            .then(Commands.literal("titles").executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException();
                String equipped = data(p).getString("equipped");
                var line=Component.literal("[모험가의 수첩] 보유 칭호: ");
                if(owned(p).isEmpty())line.append("없음");
                else {boolean first=true;for(String name:owned(p)){if(!first)line.append(", ");line.append(TitleThemes.label(name,true));first=false;}}
                p.sendSystemMessage(line);
                if(equipped.isEmpty())reply(p,"대표 칭호: 없음");else replyTitle(p,"대표 칭호: ",equipped);
                return 1;
            }))
            .then(Commands.literal("equip")
                .then(Commands.argument("title", StringArgumentType.greedyString()).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    String title = StringArgumentType.getString(ctx, "title");
                    if (!owned(p).contains(title)) { reply(p, "보유하지 않은 칭호야: " + title); return 0; }
                    data(p).putString("equipped", title);
                    replyTitle(p, "대표 칭호 장착: ", title);
                    return 1;
                })))
            .then(Commands.literal("unequip").executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException();
                data(p).remove("equipped"); reply(p, "대표 칭호를 해제했어."); return 1;
            }))
            .then(Commands.literal("grant").requires(src -> src.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("title", StringArgumentType.greedyString()).executes(ctx -> {
                        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                        String title = StringArgumentType.getString(ctx, "title");
                        if (!ALLOWED.contains(title)) { ctx.getSource().sendFailure(Component.literal("등록되지 않은 칭호: " + title)); return 0; }
                        Set<String> titles = owned(target);
                        if (!titles.add(title)) { ctx.getSource().sendFailure(Component.literal("이미 보유한 칭호야.")); return 0; }
                        saveOwned(target, titles);
                        replyTitle(target, "새로운 칭호 획득: ", title);
                        return 1;
                    }))))
            .then(Commands.literal("revoke").requires(src -> src.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("title", StringArgumentType.greedyString()).executes(ctx -> {
                        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                        String title = StringArgumentType.getString(ctx, "title");
                        Set<String> titles = owned(target);
                        if (!titles.remove(title)) return 0;
                        saveOwned(target, titles);
                        if (title.equals(data(target).getString("equipped"))) data(target).remove("equipped");
                        replyTitle(target, "칭호 회수: ", title);
                        return 1;
                    }))))
        );
    }

    @SubscribeEvent
    public void clonePlayer(PlayerEvent.Clone event) {
        
        copyTitles(event.getOriginal(),event.getEntity());
    }
    public static void copyTitles(net.minecraft.world.entity.player.Player original, net.minecraft.world.entity.player.Player replacement) {
        CompoundTag old = original.getPersistentData();
        if (old.contains(DATA_KEY, 10))
            replacement.getPersistentData().put(DATA_KEY, old.getCompound(DATA_KEY).copy());
    }
}
