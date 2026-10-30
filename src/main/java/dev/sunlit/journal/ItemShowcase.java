package dev.sunlit.journal;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.*;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Server-authoritative item snapshots, using vanilla SHOW_ITEM hover data. */
public final class ItemShowcase {
    public static final String TOKEN="[아이템]";
    private ItemShowcase(){}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("자랑하기").executes(ctx->{
            var player=ctx.getSource().getPlayerOrException();
            if(player.getMainHandItem().isEmpty()){
                ctx.getSource().sendFailure(emptyHand());return 0;
            }
            var message=announcement(JournalChat.nickname(player),player.getMainHandItem());
            player.getServer().getPlayerList().broadcastSystemMessage(message,false);return 1;
        }));
    }
    static Component emptyHand(){return Component.literal("손에 아이템을 든 뒤 사용해 주세요.");}
    static Component item(ItemStack held){
        // Copy before constructing ItemStackInfo: later inventory changes must not
        // mutate the item that was already shared with chat recipients.
        ItemStack snapshot=held.copy();
        var label=snapshot.getDisplayName().copy();
        if(snapshot.getCount()>1)label.append(Component.literal(" × "+snapshot.getCount()).withStyle(s->s.withColor(0xF4E4C4)));
        return label;
    }
    static Component announcement(Component nickname,ItemStack held){
        return Component.empty()
            .append(Component.literal("✿ ").withStyle(s->s.withColor(0xE8A5BA)))
            .append(nickname.copy())
            .append(Component.literal("님이 아이템을 자랑합니다!\n").withStyle(s->s.withColor(0xF4E4C4)))
            .append(Component.literal("✦ ").withStyle(s->s.withColor(0xE3BD6B)))
            .append(item(held));
    }
    static boolean containsToken(Component message){return message.getString().contains(TOKEN);}
    private record Span(String text,Style style){}
    static Optional<Component> expand(Component message,ItemStack held){
        if(held.isEmpty()||!containsToken(message))return Optional.empty();
        List<Span> spans=new ArrayList<>();
        message.visit((style,text)->{spans.add(new Span(text,style));return Optional.empty();},Style.EMPTY);
        String text=message.getString();var result=Component.empty();Component item=item(held);
        int from=0,at;
        while((at=text.indexOf(TOKEN,from))>=0){
            appendRange(result,spans,from,at);result.append(item.copy());from=at+TOKEN.length();
        }
        appendRange(result,spans,from,text.length());return Optional.of(result);
    }
    private static void appendRange(MutableComponent result,List<Span> spans,int from,int to){
        int offset=0;
        for(Span span:spans){
            int start=Math.max(from-offset,0),end=Math.min(to-offset,span.text.length());
            if(start<end)result.append(Component.literal(span.text.substring(start,end)).setStyle(span.style));
            offset+=span.text.length();if(offset>=to)break;
        }
    }
}
