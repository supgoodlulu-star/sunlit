package dev.sunlit.journal;

import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.gametest.*;
import com.mojang.authlib.GameProfile;
import java.util.*;

@GameTestHolder(SunlitJournal.MODID)
@PrefixGameTestTemplate(false)
public class ItemShowcaseGameTests {
    static List<ItemStack> hovers(Component c){
        var values=new ArrayList<ItemStack>();
        c.visit((s,t)->{var e=s.getHoverEvent();if(e!=null&&e.getAction()==HoverEvent.Action.SHOW_ITEM)values.add(e.getValue(HoverEvent.Action.SHOW_ITEM).getItemStack());return Optional.empty();},Style.EMPTY);
        return values;
    }
    static ItemStack pickaxe(){
        var stack=new ItemStack(Items.DIAMOND_PICKAXE);stack.setHoverName(Component.literal("룰루의 곡괭이"));stack.setDamageValue(37);stack.enchant(Enchantments.BLOCK_FORTUNE,3);
        var lore=new ListTag();lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("봄날의 선물"))));stack.getOrCreateTagElement("display").put("Lore",lore);return stack;
    }
    @GameTest(template="empty") public static void hoverSnapshotSurvivesNetworkAndInventoryMutation(GameTestHelper h){
        var stack=pickaxe();var saved=stack.copy();Component message=ItemShowcase.announcement(Component.literal("룰루"),stack);
        stack.setDamageValue(100);stack.setHoverName(Component.literal("변경된 이름"));
        var buf=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());Component decoded;
        try{buf.writeComponent(message);decoded=buf.readComponent();}finally{buf.release();}
        h.assertTrue(decoded.getString().equals("✿ 룰루님의 소중한 아이템 ✿\n└ ✦ [룰루의 곡괭이] ✦"),"Unexpected two-line announcement");
        h.assertTrue(!hovers(decoded).isEmpty()&&hovers(decoded).stream().allMatch(s->ItemStack.matches(s,saved)),"Item/NBT changed after inventory mutation or packet encoding");h.succeed();
    }
    @GameTest(template="empty") public static void placeholderPreservesStylesAcrossSegments(GameTestHelper h){
        var text=Component.literal("쨔안~ [아").withStyle(net.minecraft.ChatFormatting.GREEN).append(Component.literal("이템] / [아이템] 끝").withStyle(net.minecraft.ChatFormatting.AQUA));
        var expanded=ItemShowcase.expand(text,pickaxe()).orElseThrow();
        h.assertTrue(expanded.getString().equals("쨔안~ ✦ [룰루의 곡괭이] / ✦ [룰루의 곡괭이] 끝"),"Split or repeated token not replaced");
        final boolean[] green={false},aqua={false};expanded.visit((s,t)->{if(t.contains("쨔안"))green[0]=s.getColor().getValue()==net.minecraft.ChatFormatting.GREEN.getColor();if(t.contains("끝"))aqua[0]=s.getColor().getValue()==net.minecraft.ChatFormatting.AQUA.getColor();return Optional.empty();},Style.EMPTY);
        h.assertTrue(green[0]&&aqua[0],"Surrounding text formatting lost");h.assertTrue(!hovers(expanded).isEmpty(),"Missing item hover");h.succeed();
    }
    @GameTest(template="empty") public static void emptyAndOrdinaryMessagesDoNotCreateFakeItems(GameTestHelper h){
        h.assertTrue(ItemShowcase.expand(Component.literal("안녕"),pickaxe()).isEmpty(),"Ordinary message changed");
        h.assertTrue(ItemShowcase.expand(Component.literal("[아이템]"),ItemStack.EMPTY).isEmpty(),"Empty hand generated an item hover");h.succeed();
    }
    @GameTest(template="empty") public static void commandNeedsNoOperatorAndDoesNotConsumeItems(GameTestHelper h)throws Exception{
        var p=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"ShowcaseTest"));
        var stack=new ItemStack(Items.DIAMOND,32);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var source=p.createCommandSourceStack().withPermission(0);var dispatcher=p.getServer().getCommands().getDispatcher();
        h.assertTrue(dispatcher.execute("자랑하기",source)==1,"Non-operator could not execute command");
        h.assertTrue(p.getMainHandItem().getCount()==32,"Sharing consumed the held item");
        h.assertTrue(hovers(ItemShowcase.item(stack)).stream().allMatch(s->s.getCount()==32),"Stack count lost in hover");
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.EMERALD));
        h.assertTrue(dispatcher.execute("자랑하기",source)==0,"Empty main hand or off-hand-only item accepted");h.succeed();
    }
    @GameTest(template="empty") public static void potentialGradeColorsOverrideDisplayOnlyAndKeepHover(GameTestHelper h){
        String[] tiers={"RARE","EPIC","UNIQUE","LEGENDARY"};int[] colors={0x55aaff,0xbb66ff,0xffcc44,0x55dd88};
        for(int schema:new int[]{1,2})for(int i=0;i<tiers.length;i++){
            var s=pickaxe();s.setHoverName(Component.literal("분홍 이름").withStyle(st->st.withColor(0xff00ff)));var n=s.getOrCreateTagElement("sunlit_reforged");n.putInt("schemaVersion",schema);n.putString("potentialTier",tiers[i]);var saved=s.copy();
            var buf=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());Component decoded;try{buf.writeComponent(ItemShowcase.item(s));decoded=buf.readComponent();}finally{buf.release();}
            final int expected=colors[i];final boolean[] found={false};decoded.visit((style,text)->{if(text.contains("분홍 이름")){found[0]=true;h.assertTrue(style.getColor()!=null&&style.getColor().getValue()==expected,"Wrong grade color");}return Optional.empty();},Style.EMPTY);
            h.assertTrue(found[0]&&hovers(decoded).stream().anyMatch(it->ItemStack.matches(it,saved))&&ItemStack.matches(s,saved),"Grade rendering changed actual item/name/NBT or hover");
        }
        var ordinary=pickaxe();h.assertTrue(ItemShowcase.potentialColor(ordinary).isEmpty(),"Invented a potential grade");ordinary.getOrCreateTagElement("sunlit_reforged").putInt("schemaVersion",999);h.assertTrue(ItemShowcase.potentialColor(ordinary).isEmpty(),"Accepted unsupported schema");h.succeed();
    }

}
