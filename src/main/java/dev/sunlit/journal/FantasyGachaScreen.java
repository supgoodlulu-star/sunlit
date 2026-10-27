package dev.sunlit.journal;

import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.resources.ResourceLocation;

/** Logical canvas: every hit target uses the same transform as the rendered artwork. */
public final class FantasyGachaScreen extends Screen {
    private CompoundTag data;
    private int theme,kind,age=200,waitTicks;
    private long sequence;
    private boolean pending,showRates;
    private float scale,ox,oy;
    private String feedback="";
    private WorkshopButton one,ten,apply,exchange,craftTicket;
    private final java.util.List<WorkshopButton> themeButtons=new ArrayList<>();
    private static final int W=960,H=600,INK=0xFFFFE9D5;
    private static final ResourceLocation BACKGROUND=new ResourceLocation(SunlitJournal.MODID,"textures/gui/fantasy_workshop.png");
    private static final String[] KINDS={"검","곡괭이","도끼","삽","괭이"};
    public FantasyGachaScreen(CompoundTag snapshot){super(Component.literal("별빛 공방 · 환상 도구 스킨 뽑기"));data=snapshot.getCompound("gacha").copy();sequence=data.getLong("sequence");}
    public void update(CompoundTag snapshot){
        data=snapshot.getCompound("gacha").copy();pending=false;waitTicks=0;
        if(sequence!=data.getLong("sequence")){sequence=data.getLong("sequence");age=0;feedback="";}
        refresh();
    }
    private void send(String action,String value){JournalNetwork.CHANNEL.sendToServer(new JournalNetwork.Request(action,value));}
    private void draw(int n){pending=true;waitTicks=0;feedback="별빛을 모으고 있어요…";refresh();send("draw",""+n);}
    private FantasyGacha.Skin skin(){return FantasyGacha.POOL.get(theme*5+kind);}
    private boolean owns(){return data.getCompound("owned").getBoolean(skin().id());}
    private WorkshopButton button(String text,int x,int y,int w,Runnable run){return addRenderableWidget(new WorkshopButton(text,x,y,w,run));}
    private final class WorkshopButton extends net.minecraft.client.gui.components.Button {
        WorkshopButton(String label,int x,int y,int w,Runnable action){super(x,y,w,25,Component.literal(label),b->action.run(),DEFAULT_NARRATION);}
        @Override public void renderWidget(GuiGraphics g,int mx,int my,float delta){
            boolean draw=getMessage().getString().contains("회 뽑기");
            int fill=!active?0xFF493D4C:isHoveredOrFocused()?0xFF86576B:draw?0xFF79445F:0xFF4C354D;
            JournalButton.rounded(g,getX(),getY(),width,height,7,active?0xFFE4B991:0xFF8B7883);
            JournalButton.rounded(g,getX()+1,getY()+1,width-2,height-2,6,fill);
            String label=font.plainSubstrByWidth(getMessage().getString(),(int)((width-10)/fontScale()));
            text(g,label,getX()+(int)((width-font.width(label)*fontScale())/2),getY()+7,active?INK:0xFFBCAEB8);
        }
    }
    @Override protected void init(){
        scale=Math.min((width-8)/(float)W,(height-8)/(float)H);ox=(width-W*scale)/2;oy=(height-H*scale)/2;themeButtons.clear();
        one=button("1회 뽑기 · 뽑기권 1장",292,558,174,()->draw(1));ten=button("10회 뽑기 · 뽑기권 10장",482,558,188,()->draw(10));
        button("확률·이용 안내",18,558,125,()->showRates=!showRates);
        button("닫기",858,558,84,this::onClose);
        apply=button("손에 든 장비에 스킨 적용",566,320,184,()->{send("fantasySkin",skin().id());feedback="손에 든 장비에 적용을 요청했어요.";});
        exchange=button("별가루 5개로 교환",758,320,174,()->{send("exchangeSkin",skin().id());feedback="선택한 스킨 교환을 요청했어요.";});
        craftTicket=button("조각 10개 → 뽑기권 1장",566,352,184,()->{send("craftTicket","");feedback="뽑기권 조각 교환을 요청했어요.";});
        button("외형 해제",762,352,170,()->send("skin","clear"));
        button("이전 테마",566,80,80,()->{theme=(theme+11)%12;refresh();});
        button("다음 테마",852,80,80,()->{theme=(theme+1)%12;refresh();});
        for(int i=0;i<5;i++){final int index=i;button(KINDS[i],568+i*74,260,69,()->{kind=index;refresh();});}
        for(int i=0;i<12;i++){final int index=i;themeButtons.add(button(FantasyGacha.POOL.get(i*5).themeName(),566+(i%4)*94,111+(i/4)*26,88,()->{theme=index;refresh();}));}
        refresh();
    }
    private void refresh(){
        if(one==null)return;boolean busy=pending||age<44;one.active=!busy&&data.getInt("tickets")>=1;ten.active=!busy&&data.getInt("tickets")>=10;
        craftTicket.active=!pending&&data.getInt("fragments")>=10;
        boolean compatible=minecraft!=null&&minecraft.player!=null&&FantasySkins.kind(minecraft.player.getMainHandItem()).equals(skin().kind());
        apply.active=!pending&&owns()&&compatible;exchange.active=!pending&&!owns()&&data.getInt("dust")>=5;
        for(int i=0;i<themeButtons.size();i++)themeButtons.get(i).active=i!=theme;
    }
    @Override public void tick(){
        age++;if(age%5==0)refresh();
        if(pending&&++waitTicks>100){pending=false;feedback="연결을 확인한 뒤 다시 시도해 주세요.";refresh();send("refresh","");}
        if(age%100==0&&!pending)send("refresh","");
    }
    private void panel(GuiGraphics g,int x,int y,int w,int h,int color){JournalButton.rounded(g,x,y,w,h,10,0xFFCDA478);JournalButton.rounded(g,x+1,y+1,w-2,h-2,9,color);}
    private float fontScale(){return (float)(Math.max(2,Math.ceil(minecraft.getWindow().getGuiScale()*scale))/(minecraft.getWindow().getGuiScale()*scale));}
    private void text(GuiGraphics g,String s,int x,int y,int color){g.pose().pushPose();double gui=minecraft.getWindow().getGuiScale();
        float snapX=(float)((Math.round((ox+x*scale)*gui)/gui-ox)/scale),snapY=(float)((Math.round((oy+y*scale)*gui)/gui-oy)/scale);
        g.pose().translate(snapX,snapY,0);g.pose().scale(fontScale(),fontScale(),1);g.drawString(font,s,0,0,color,false);g.pose().popPose();}
    private void fit(GuiGraphics g,String s,int x,int y,int w,int color){text(g,font.plainSubstrByWidth(s,(int)(w/fontScale())),x,y,color);}
    public static ItemStack preview(String id){
        var skin=FantasyGacha.find(id);if(skin==null)return ItemStack.EMPTY;
        var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(new ResourceLocation("minecraft","diamond_"+skin.kind()));
        var stack=new ItemStack(item);stack.getOrCreateTag().putString(FantasySkins.KEY,id);return stack;
    }
    private void model(GuiGraphics g,String id,int x,int y,float size){if(showRates)return;g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(size,size,size);g.renderItem(preview(id),0,0);g.pose().popPose();}
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        renderBackground(g);int x=(int)((mx-ox)/scale),y=(int)((my-oy)/scale);
        g.pose().pushPose();g.pose().translate(ox,oy,0);g.pose().scale(scale,scale,1);
        g.blit(BACKGROUND,0,0,0,0,W,H,W,H);g.fill(0,0,W,H,0x7730223B);
        panel(g,12,10,530,53,0xDD2A1F35);
        text(g,"환상 도구 스킨 뽑기",28,22,INK);text(g,"SUNLIT FANTASY COLLECTION · 60 SKINS",28,42,0xFFE2BEA2);
        panel(g,550,10,398,53,0xEE2C2237);
        text(g,"보유 뽑기권  "+data.getInt("tickets")+"장",568,24,0xFFFFD5A6);
        text(g,"별가루  "+data.getInt("dust")+"개",786,24,0xFFE4C9FF);
        int collected=0;for(var s:FantasyGacha.POOL)if(data.getCompound("owned").getBoolean(s.id()))collected++;
        text(g,"컬렉션 "+collected+" / 60 · 조각 "+data.getInt("fragments")+"개",568,44,0xFFE3D2E7);
        panel(g,550,72,398,310,0xE82B2335);
        var s=skin();
        // Dark text backing preserves contrast against the art and glow.
        panel(g,18,228,205,147,0xEE312338);
        text(g,s.rarityName().toUpperCase(Locale.ROOT),32,244,s.color());fit(g,s.name(),32,266,180,INK);
        text(g,s.collection(),32,290,0xFFEAC3D9);
        text(g,"성능은 그대로, 외형만 새롭게",32,314,0xFFE3CBD8);
        text(g,owns()?"수집한 스킨이에요":"뽑기 또는 별가루 교환으로 획득",32,341,0xFFFFD6A8);
        // ItemRenderer draws the actual Forge OBJ model used in hand and inventory.
        model(g,s.id(),178,52,22);
        text(g,s.themeName()+" · "+(theme+1)+" / 12",658,88,INK);
        for(int k=0;k<5;k++){
            int cx=568+k*74;panel(g,cx,194,69,61,kind==k?0xFF564055:0xFF352C40);
            var miniature=FantasyGacha.POOL.get(theme*5+k);model(g,miniature.id(),cx+12,198,3);text(g,data.getCompound("owned").getBoolean(miniature.id())?"보유":"미보유",cx+16,242,0xFFE8D9E5);
        }
        text(g,"중복은 별가루 1개 · 5개로 선택 교환",566,296,0xFFE8C7D8);

        panel(g,12,390,936,151,0xED251F31);
        text(g,"획득한 스킨 · 최근 뽑기 결과",28,402,INK);
        var results=data.getList("results",Tag.TAG_COMPOUND);
        for(int i=0;i<Math.min(10,results.size());i++){
            int cx=23+i*92,cy=425;var result=results.getCompound(i);var reward=FantasyGacha.find(result.getString("id"));if(reward==null)continue;
            boolean reveal=age>=5+i*4;panel(g,cx,cy,86,104,reveal?reward.color():0xFF65506E);JournalButton.rounded(g,cx+2,cy+2,82,100,8,0xFF302638);
            if(reveal){model(g,reward.id(),cx+13,cy+7,3.8f);fit(g,reward.themeName(),cx+5,cy+55,77,reward.color());text(g,KINDS[(reward.index()-1)%5],cx+6,cy+71,INK);text(g,result.getBoolean("duplicate")?"별가루 +1":"새 스킨!",cx+6,cy+88,0xFFF1D8C3);
                if(x>=cx&&x<cx+86&&y>=cy&&y<cy+104)g.renderTooltip(font,Component.literal(reward.name()+" · "+reward.rarityName()),x,y);
            }else text(g,"✦",cx+38,cy+44,0xFFFFD6A2);
        }
        if(results.isEmpty())text(g,"뽑기를 하면 이곳에서 스킨을 한 장씩 확인할 수 있어요.",30,466,0xFFE7CFDE);
        if(!feedback.isEmpty())fit(g,feedback,154,545,600,0xFFFFE9CC);
        super.render(g,x,y,delta);
        if(showRates){g.flush();g.pose().pushPose();g.pose().translate(0,0,600);rates(g);g.pose().popPose();}
        g.pose().popPose();
    }
    private void rates(GuiGraphics g){
        g.fill(0,0,W,H,0xCC17121F);panel(g,75,60,810,468,0xFF261F31);
        text(g,"뽑기 확률과 이용 안내 · 전체 보상 60종",98,80,INK);
        text(g,"각 도구 확률은 해당 테마의 검·곡괭이·도끼·삽·괭이 각각에 적용돼요.",98,103,0xFFE0CBDE);
        for(int i=0;i<12;i++){
            var s=FantasyGacha.POOL.get(i*5);int xx=98+(i/6)*395,yy=134+(i%6)*34;
            text(g,s.themeName()+" · "+s.rarityName(),xx,yy,s.color());
            text(g,String.format(Locale.ROOT,"각 도구 %.4f%% · 테마 합계 %.4f%%",100.0*s.weight()/FantasyGacha.TOTAL_WEIGHT,500.0*s.weight()/FantasyGacha.TOTAL_WEIGHT),xx,yy+14,INK);
        }
        text(g,"매회 독립 추첨 · 10회 뽑기는 같은 확률로 10번 추첨해요.",98,355,INK);
        text(g,"등급 보장이나 누적 확정 보상은 없어요. 중복 스킨은 별가루 1개로 바뀌어요.",98,380,INK);
        text(g,"별가루 5개로 원하는 미보유 스킨 1종을 선택 교환할 수 있어요.",98,405,INK);
        text(g,"일일 5개 중 3개: 조각 1개 / 주간 3개: 뽑기권 1장",98,430,INK);
        text(g,"조각 10개 → 뽑기권 1장 · 완료 보상은 기간마다 한 번 자동 지급돼요.",98,455,INK);
        text(g,"아무 곳이나 눌러 닫기",98,496,0xFFD6B4CC);
    }
    @Override public boolean mouseClicked(double x,double y,int b){if(showRates){showRates=false;return true;}
        double lx=(x-ox)/scale,ly=(y-oy)/scale;
        if(b==0&&ly>=425&&ly<529){int i=(int)((lx-23)/92);var results=data.getList("results",Tag.TAG_COMPOUND);if(lx>=23&&i>=0&&i<results.size()&&lx<23+i*92+86&&age>=5+i*4){var s=FantasyGacha.find(results.getCompound(i).getString("id"));if(s!=null){theme=(s.index()-1)/5;kind=(s.index()-1)%5;refresh();return true;}}}
        return super.mouseClicked(lx,ly,b);}
    @Override public boolean mouseReleased(double x,double y,int b){return super.mouseReleased((x-ox)/scale,(y-oy)/scale,b);}
    @Override public boolean mouseDragged(double x,double y,int b,double dx,double dy){return super.mouseDragged((x-ox)/scale,(y-oy)/scale,b,dx/scale,dy/scale);}
    @Override public void onClose(){send("close","");super.onClose();}
    @Override public boolean isPauseScreen(){return false;}
}
