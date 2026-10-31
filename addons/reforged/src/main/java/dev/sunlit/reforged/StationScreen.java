package dev.sunlit.reforged;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Artwork contains no text or items: all information and interactions remain live. */
public final class StationScreen extends AbstractContainerScreen<StationMenu>{
    private static final ResourceLocation BACKGROUND=new ResourceLocation(SunlitReforged.ID,"textures/gui/workshop.png");
    private int quantity=1;private boolean wasPending;private double fit=1;
    private final List<Button> purchases=new ArrayList<>();private Button roll,oldChoice,newChoice;
    public StationScreen(StationMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=440;imageHeight=300;}
    @Override protected void init(){
        fit=Math.min(1,Math.min(width/448d,height/308d));width=(int)(width/fit);height=(int)(height/fit);
        super.init();wasPending=menu.pending();purchases.clear();
        button("×",416,8,16,16,()->onClose(),false);
        roll=button("큐브 사용하기",151,178,126,23,()->action("roll",0),true);
        oldChoice=button("기존 줄 유지",151,181,61,18,()->action("old",0),false);
        newChoice=button("새 줄 선택",216,181,62,18,()->action("new",0),true);
        button("−",300,37,16,16,()->quantity=Math.max(1,quantity-1),false);
        button("+",410,37,16,16,()->quantity=Math.min(64,quantity+1),false);
        for(int i=0;i<3;i++){final int product=i;purchases.add(button("구매",391,63+i*34,34,22,()->action("buy",product),false));}
        update();
    }
    private Button button(String label,int x,int y,int w,int h,Runnable action,boolean purple){var b=new WorkshopButton(label,leftPos+x,topPos+y,w,h,action,purple);addRenderableWidget(b);return b;}
    public void update(){if(roll==null)return;boolean pending=menu.pending();roll.visible=!pending;oldChoice.visible=pending;newChoice.visible=pending;for(int i=0;i<3;i++)purchases.get(i).active=!pending;roll.active=!pending&&!menu.work.getItem(0).isEmpty()&&!menu.work.getItem(1).isEmpty()&&(Potential.gear(menu.work.getItem(0)).orElse(null)!=Potential.Gear.ROD||menu.clientState.getBoolean("fishingReady"));wasPending=pending;}
    private void action(String name,int product){var state=menu.clientState;Network.CHANNEL.sendToServer(new Network.Action(menu.containerId,state.getInt("revision"),name,product,quantity,state.getCompound("pending").getString("rollId")));}
    @Override protected void containerTick(){super.containerTick();update();}
    private static void frame(GuiGraphics g,int x,int y,int w,int h,int fill){g.fill(x,y,x+w,y+h,0xff3c241d);g.fill(x+1,y+1,x+w-1,y+h-1,0xffb18a50);g.fill(x+2,y+2,x+w-2,y+h-2,fill);g.fill(x+3,y+3,x+w-3,y+4,0x447fffba);}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        g.blit(BACKGROUND,leftPos,topPos,imageWidth,imageHeight,0,0,1536,1024,1536,1024);

        for(var slot:menu.slots){int x=leftPos+slot.x,y=topPos+slot.y;frame(g,x-1,y-1,18,18,0xff94836a);g.fill(x,y,x+16,y+1,0xff514337);}
        frame(g,leftPos+79,topPos+71,28,28,0xff372738);frame(g,leftPos+79,topPos+145,28,28,0xff433143);
        for(int i=0;i<3;i++){int y=topPos+59+i*34;frame(g,leftPos+294,y,134,31,0xff3d2c29);frame(g,leftPos+298,y+6,20,20,0xff342b38);Item item=i==0?SunlitReforged.SUSPICIOUS.get():i==1?SunlitReforged.ARTISAN.get():SunlitReforged.MASTER.get();g.renderItem(new ItemStack(item),leftPos+300,y+8);}
    }
    private void text(GuiGraphics g,String s,int x,int y,int color,int max){g.drawString(font,font.plainSubstrByWidth(s,max),x,y,color,false);}
    private void centered(GuiGraphics g,String s,int x,int y,int color,int max){String a=font.plainSubstrByWidth(s,max);g.drawString(font,a,x-font.width(a)/2,y,color,false);}
    private static int tierColor(Potential.Tier tier){return switch(tier){case RARE->0xff2e5e8c;case EPIC->0xff66388c;case UNIQUE->0xff84621b;case LEGENDARY->0xff2e794a;};}
    private void lines(GuiGraphics g,Potential p,int y,boolean compact){for(int i=0;i<3;i++){int yy=y+i*(compact?15:24);frame(g,151,yy,126,compact?14:22,0xffead0a2);text(g,p.lines().get(i).label(),155,yy+3,0xff493020,118);if(!compact)text(g,"["+p.lines().get(i).tier().label+"]",155,yy+12,tierColor(p.lines().get(i).tier()),118);}}
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        centered(g,"선릿 재련 공방",220,30,0xffffe6a6,240);
        centered(g,"장비",93,39,0xffffe6a6,95);centered(g,menu.work.getItem(0).isEmpty()?"장비를 올려주세요":menu.work.getItem(0).getHoverName().getString(),93,111,0xffffe9d2,105);
        centered(g,"큐브",93,131,0xffffe6a6,100);
        centered(g,menu.work.getItem(1).isEmpty()?"큐브를 올려주세요":menu.work.getItem(1).getHoverName().getString(),93,178,0xffe4c1ed,105);
        centered(g,menu.pending()?"선택 전까지 장비 보관":"장비·큐브를 넣고 사용",93,191,0xffd7ba99,108);
        var state=menu.clientState;var value=Potential.get(menu.work.getItem(0));text(g,menu.pending()?"잠재능력 비교":"현재 잠재능력",153,55,0xff674022,145);
        if(menu.pending()){
            var pending=state.getCompound("pending");var old=Potential.load(pending.getCompound("old"));var next=Potential.load(pending.getCompound("next"));
            text(g,"기존 줄",153,69,0xff674022,145);old.ifPresent(p->lines(g,p,80,true));
            text(g,"새 줄 · "+next.map(p->p.tier().label).orElse(""),153,126,0xff674022,145);next.ifPresent(p->lines(g,p,137,true));
        }else if(value.isPresent()){
            var p=value.get();frame(g,151,69,126,24,0xff855024);centered(g,p.tier().label,214,77,0xffffe3a0,140);lines(g,p,100,false);
        }else{centered(g,"첫 큐브 사용 시",214,91,0xff674022,148);centered(g,"레어 잠재능력 3줄",214,108,0xff674022,148);}
        var balance=state.getCompound("balance");String money=balance.getBoolean("available")?java.math.BigDecimal.valueOf(balance.getLong("spurs")).divide(java.math.BigDecimal.valueOf(balance.getLong("unit")),2,java.math.RoundingMode.DOWN).stripTrailingZeros().toPlainString()+" Sun":"은행 계좌 필요";
        text(g,"큐브 구매",295,27,0xffffe6a6,75);text(g,money,354,27,0xffffe6a6,74);centered(g,"수량 "+quantity,362,41,0xfff8deb1,86);
        String[] names={"수상한 큐브","장인의 큐브","명장의 큐브"};var prices=state.getList("prices",Tag.TAG_INT);
        for(int i=0;i<3;i++){text(g,names[i],321,64+i*34,0xffffe6c2,68);text(g,(prices.size()==3?String.format(java.util.Locale.ROOT,"%,d",prices.getInt(i)):"?")+" Sun",321,77+i*34,0xffffd889,68);}
        frame(g,294,164,134,34,0xffead0a2);text(g,"사용 가능한 장비",299,168,0xff674022,123);text(g,"다이아 · 네더라이트",299,179,0xff493020,123);text(g,"일반 낚싯대",299,189,0xff493020,123);
        centered(g,"인벤토리",220,204,0xffffe6a6,165);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){renderBackground(g);int x=(int)(mx/fit),y=(int)(my/fit);g.pose().pushPose();g.pose().scale((float)fit,(float)fit,1);super.render(g,x,y,partial);renderTooltip(g,x,y);
        for(int i=0;i<3;i++)if(purchases.get(i).isHoveredOrFocused()){var prices=menu.clientState.getList("prices",Tag.TAG_INT);g.renderTooltip(font,List.of(Component.literal((prices.size()==3?prices.getInt(i):0)+" Sun × "+quantity),Component.literal("실제 은행 계좌에서 차감합니다")),Optional.empty(),x,y);}
        if(x>=leftPos+151&&x<leftPos+280&&y>=topPos+69&&y<topPos+178){var p=menu.pending()?Potential.load(menu.clientState.getCompound("pending").getCompound(y<topPos+126?"old":"next")):Potential.get(menu.work.getItem(0));p.ifPresent(v->{var rows=new ArrayList<Component>();rows.add(Component.literal(v.tier().label));for(var l:v.lines())rows.add(Component.literal(l.label()+" ["+l.tier().label+"]"));if(menu.pending())rows.add(Component.literal("승급된 최종 등급은 어느 선택에도 유지"));g.renderTooltip(font,rows,Optional.empty(),x,y);});}
        g.pose().popPose();
    }
    @Override public boolean mouseClicked(double x,double y,int b){return super.mouseClicked(x/fit,y/fit,b);}
    @Override public boolean mouseReleased(double x,double y,int b){return super.mouseReleased(x/fit,y/fit,b);}
    @Override public boolean mouseDragged(double x,double y,int b,double dx,double dy){return super.mouseDragged(x/fit,y/fit,b,dx/fit,dy/fit);}
    @Override public boolean mouseScrolled(double x,double y,double d){return super.mouseScrolled(x/fit,y/fit,d);}
    static final class WorkshopButton extends Button{
        final boolean purple;WorkshopButton(String text,int x,int y,int w,int h,Runnable action,boolean purple){super(x,y,w,h,Component.literal(text),b->action.run(),DEFAULT_NARRATION);this.purple=purple;}
        @Override public void renderWidget(GuiGraphics g,int x,int y,float t){int color=getMessage().getString().equals("×")?0xff9c4037:!active?0xff756951:purple?(isHoveredOrFocused()?0xffac67bd:0xff743a91):(isHoveredOrFocused()?0xff8eac50:0xff547a35);frame(g,getX(),getY(),width,height,color);var font=net.minecraft.client.Minecraft.getInstance().font;String s=font.plainSubstrByWidth(getMessage().getString(),width-4);g.drawString(font,s,getX()+(width-font.width(s))/2,getY()+(height-8)/2,active?0xfffff5ce:0xffbbae94,false);}
    }
}
