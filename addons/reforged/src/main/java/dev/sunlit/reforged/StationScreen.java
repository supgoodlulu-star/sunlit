package dev.sunlit.reforged;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.*;
import java.util.*;

public final class StationScreen extends AbstractContainerScreen<StationMenu>{
    private int quantity=1;private boolean wasPending;private final List<Button> purchases=new ArrayList<>();private Button roll,oldChoice,newChoice;
    public StationScreen(StationMenu menu,Inventory inv,Component title){super(menu,inv,title);imageHeight=232;}
    @Override protected void init(){imageWidth=Math.min(380,width-8);super.init();wasPending=menu.pending();purchases.clear();
        roll=button("큐브 사용",20,80,82,()->action("roll",0));button("−",20,112,24,()->quantity=Math.max(1,quantity-1));button("+",78,112,24,()->quantity=Math.min(64,quantity+1));
        oldChoice=button("기존 줄 유지",114,126,Math.max(70,(imageWidth-132)/2),()->action("old",0));
        newChoice=button("새 줄 선택",imageWidth-(imageWidth-132)/2-12,126,Math.max(70,(imageWidth-132)/2),()->action("new",0));
        String[] labels={"수상한 큐브","장인의 큐브","명장의 큐브","재련 망치"};
        for(int i=0;i<4;i++){final int product=i;purchases.add(button(labels[i],198,146+i*20,imageWidth-210,()->action("buy",product)));}
        update();
    }
    private Button button(String label,int x,int y,int w,Runnable action){var b=new PaperButton(label,leftPos+x,topPos+y,w,18,action);addRenderableWidget(b);return b;}
    public void update(){if(roll==null)return;boolean pending=menu.pending();roll.visible=!pending;oldChoice.visible=pending;newChoice.visible=pending;var prices=menu.clientState.getList("prices",Tag.TAG_INT);String[] names={"수상한","장인","명장","망치"};for(int i=0;i<purchases.size();i++){purchases.get(i).active=!pending&&(i!=3||menu.clientState.getBoolean("hammerAvailable"));if(prices.size()==4)purchases.get(i).setMessage(Component.literal(names[i]+" "+prices.getInt(i)+" Sun"));}roll.active=!pending&&!menu.work.getItem(0).isEmpty()&&!menu.work.getItem(1).isEmpty()&&(Potential.gear(menu.work.getItem(0)).orElse(null)!=Potential.Gear.ROD||menu.clientState.getBoolean("fishingReady"));wasPending=pending;}
    private void action(String name,int product){CompoundTag state=menu.clientState;Network.CHANNEL.sendToServer(new Network.Action(menu.containerId,state.getInt("revision"),name,product,quantity,state.getCompound("pending").getString("rollId")));}
    @Override protected void containerTick(){super.containerTick();if(wasPending!=menu.pending())update();}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xff8d5c32);g.fill(leftPos+3,topPos+3,leftPos+imageWidth-3,topPos+imageHeight-3,0xffffefce);
        for(var slot:menu.slots){int x=leftPos+slot.x,y=topPos+slot.y;g.fill(x-1,y-1,x+17,y+17,0xffad8759);g.fill(x,y,x+16,y+16,0xfff3dfb8);}
    }
    private void text(GuiGraphics g,String s,int x,int y,int color,int width){g.drawString(font,font.plainSubstrByWidth(s,Math.max(1,width)),x,y,color,false);}
    private void potential(GuiGraphics g,Optional<Potential> value,int x,int y,int w,String label){
        text(g,label,x,y,0xff66462d,w);if(value.isEmpty()){text(g,"첫 큐브: 레어 3줄 부여",x,y+12,0xff594435,w);return;}
        var p=value.get();text(g,p.tier().label,x,y+12,tierColor(p.tier()),w);for(int i=0;i<3;i++)text(g,p.lines().get(i).label()+" ["+p.lines().get(i).tier().label+"]",x,y+24+i*11,0xff493020,w);
    }
    private static int tierColor(Potential.Tier tier){return switch(tier){case RARE->0xff2e5e8c;case EPIC->0xff66388c;case UNIQUE->0xff84621b;case LEGENDARY->0xff2e794a;};}
    private void compact(GuiGraphics g,Optional<Potential> value,int x,int y,int w,String label){
        if(value.isEmpty())return;var p=value.get();text(g,label+" · "+p.tier().label,x,y,tierColor(p.tier()),w);for(int i=0;i<3;i++)text(g,p.lines().get(i).label()+" ["+p.lines().get(i).tier().label+"]",x,y+13+i*11,0xff493020,w);
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        text(g,"선릿 재련 공방",10,7,0xff493020,imageWidth-20);text(g,menu.pending()?"최종 등급: "+Potential.load(menu.clientState.getCompound("pending").getCompound("next")).map(p->p.tier().label).orElse("?")+" · 기존 줄 선택에도 유지":"Reforged 파일 미확인 · 원본 NBT 보존",10,20,0xff815b3b,imageWidth-20);
        text(g,"장비",27,32,0xff493020,40);text(g,"큐브",73,32,0xff493020,40);text(g,"구매 수량 "+quantity,20,101,0xff493020,85);
        var state=menu.clientState;int w=imageWidth-126;
        if(menu.pending()){
            var pending=state.getCompound("pending");var old=Potential.load(pending.getCompound("old"));var next=Potential.load(pending.getCompound("next"));
            compact(g,old,114,33,w,"기존 줄");compact(g,next,114,81,w,"새 줄");
        }else potential(g,Potential.get(menu.work.getItem(0)),114,33,w,"잠재능력");
        text(g,"인벤토리",18,135,0xff493020,100);var balance=state.getCompound("balance");
        String money=balance.getBoolean("available")?java.math.BigDecimal.valueOf(balance.getLong("spurs")).divide(java.math.BigDecimal.valueOf(balance.getLong("unit")),6,java.math.RoundingMode.DOWN).stripTrailingZeros().toPlainString()+" Sun":"계좌 연동 확인 필요";
        if(!menu.pending())text(g,money,198,135,0xff35603b,imageWidth-210);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){renderBackground(g);super.render(g,mx,my,partial);renderTooltip(g,mx,my);
        var prices=menu.clientState.getList("prices",Tag.TAG_INT);for(int i=0;i<purchases.size();i++)if(purchases.get(i).isHoveredOrFocused()){
            String desc=i==3?"Reforged의 실제 망치 ID 확인 후 활성화":i==2?"기존 줄 / 새 줄 선택 · 승급 유지":"즉시 3줄 교체 · 등급 하락 없음";
            g.renderTooltip(font,List.of(Component.literal((prices.size()>i?prices.getInt(i):0)+" Sun × "+quantity),Component.literal(desc)),Optional.empty(),mx,my);break;
        }
    }
    static final class PaperButton extends Button{
        PaperButton(String text,int x,int y,int w,int h,Runnable action){super(x,y,w,h,Component.literal(text),b->action.run(),DEFAULT_NARRATION);}
        @Override public void renderWidget(GuiGraphics g,int x,int y,float t){g.fill(getX(),getY(),getX()+width,getY()+height,0xff79512b);g.fill(getX()+1,getY()+1,getX()+width-1,getY()+height-1,!active?0xffd5c5ac:isHoveredOrFocused()?0xff9cb66a:0xff799c4a);var font=net.minecraft.client.Minecraft.getInstance().font;String s=font.plainSubstrByWidth(getMessage().getString(),width-6);g.drawString(font,s,getX()+(width-font.width(s))/2,getY()+5,active?0xfffff8de:0xff72604a,false);}
    }
}
