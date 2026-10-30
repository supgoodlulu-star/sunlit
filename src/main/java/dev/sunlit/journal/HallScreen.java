package dev.sunlit.journal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class HallScreen extends Screen {
    private CompoundTag data;private int left,top,w,h,rowsPerPage=8,ticks;
    public HallScreen(CompoundTag data){super(Component.literal("명예의 전당"));this.data=data;}
    CompoundTag snapshot(){return data.copy();}
    static void receive(CompoundTag data,boolean open){var mc=Minecraft.getInstance();if(mc.screen instanceof HallScreen s){s.data=data;s.subpage=Math.min(s.subpage,Math.max(0,(data.getList("rows",10).size()-1)/s.rowsPerPage));s.rebuildWidgets();}else if(open)mc.setScreen(new HallScreen(data));}
    @Override protected void init(){
        w=Math.min(560,width-12);h=Math.min(360,height-12);left=(width-w)/2;top=(height-h)/2;rowsPerPage=Math.max(1,Math.min(8,(h-(data.getString("category").equals("assets")?161:147))/22));
        var cats=data.getCompound("categories");var keys=new ArrayList<>(cats.getAllKeys());keys.sort(Comparator.naturalOrder());String current=data.getString("category");int index=keys.indexOf(current);
        addRenderableWidget(new JournalButton("◀",left+12,top+37,25,20,false,()->selectCategory(keys,index-1)));
        addRenderableWidget(new JournalButton(categoryLabel(current),left+41,top+37,w-82,20,true,()->selectCategory(keys,index+1)));
        addRenderableWidget(new JournalButton("▶",left+w-37,top+37,25,20,false,()->selectCategory(keys,index+1)));
        var seasons=data.getList("seasons",Tag.TAG_STRING);int si=0;for(int i=0;i<seasons.size();i++)if(seasons.getString(i).equals(data.getString("season")))si=i;final int seasonIndex=si;
        addRenderableWidget(new JournalButton("지난 시즌",left+12,top+64,75,20,false,()->{if(seasons.size()>1)load(seasons.getString((seasonIndex+1)%seasons.size()),current,0);}));
        addRenderableWidget(new JournalButton("이번 시즌",left+w-87,top+64,75,20,false,()->load(data.getString("current"),current,0)));
        addRenderableWidget(new JournalButton("이전",left+12,top+h-31,50,20,false,()->changePage(-1)));
        addRenderableWidget(new JournalButton("다음",left+66,top+h-31,50,20,false,()->changePage(1)));
        addRenderableWidget(new JournalButton("닫기",left+w-62,top+h-31,50,20,false,()->onClose()));
    }
    // Server pages are eight rows; small GUI viewports use a local subpage to avoid clipping.
    private int subpage;
    private String categoryLabel(String key){
        if(!key.startsWith("skill:"))return data.getCompound("categories").getString(key);
        String id=key.substring(6),label=SkillLabels.category(id);
        if(label.equals(net.minecraft.client.resources.language.I18n.get("sunlit_journal_core.skill_category.unknown")))label=HallNames.skill(id);
        return label+" 경험치";
    }
    void selectCategory(List<String> keys,int i){if(!keys.isEmpty())load(data.getString("season"),keys.get(Math.floorMod(i,keys.size())),0);}
    void load(String season,String category,int page){subpage=0;HallNetwork.request(season,category,page);}
    void changePage(int delta){int count=data.getList("rows",10).size(),subs=Math.max(1,(count+rowsPerPage-1)/rowsPerPage);if(delta>0&&subpage+1<subs){subpage++;return;}if(delta<0&&subpage>0){subpage--;return;}load(data.getString("season"),data.getString("category"),data.getInt("page")+delta);}
    @Override public void tick(){if(++ticks%200==0)HallNetwork.request(data.getString("season"),data.getString("category"),data.getInt("page"));}
    @Override public void onClose(){HallNetwork.request("","close",0);super.onClose();}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g);g.fill(left,top,left+w,top+h,0xff9d673d);g.fill(left+4,top+4,left+w-4,top+h-4,0xffffedcb);g.fill(left+8,top+8,left+w-8,top+29,0xffd7b16e);
        g.drawCenteredString(font,"✿ 선릿 · 명예의 전당 ✿",width/2,top+14,0xff56351e);
        boolean cumulative=data.getString("category").equals("achievements");
        g.drawCenteredString(font,cumulative?"누적 업적 기록":data.getString("season")+" 시즌",width/2,top+69,0xff56351e);
        var rows=data.getList("rows",10);int start=subpage*rowsPerPage;String tooltip=null;
        if(rows.isEmpty())g.drawCenteredString(font,"아직 기록이 없어요. 첫 주인공이 되어 주세요!",width/2,top+106,0xff6c4b30);
        for(int i=start;i<Math.min(rows.size(),start+rowsPerPage);i++){
            var r=rows.getCompound(i);int y=top+93+(i-start)*22;g.fill(left+12,y,left+w-12,y+20,i%2==0?0xfff3dab0:0xfff9e5c3);
            g.drawString(font,r.getInt("rank")+"위",left+18,y+6,0xff77501d,false);
            String value=r.contains("displayScore")?r.getString("displayScore"):String.format(Locale.ROOT,"%,d",r.getLong("score"));
            int available=Math.max(66,(w-78)/2);String shown=font.width(value)>available?font.plainSubstrByWidth(value,available-font.width("…"))+"…":value;int vx=left+w-20-font.width(shown);
            String nick=r.getString("nick");g.drawString(font,font.plainSubstrByWidth(nick,Math.max(12,vx-left-67)),left+55,y+6,0xff49301e,false);
            g.drawString(font,shown,vx,y+6,0xff35603b,false);if(mx>=vx&&mx<left+w-12&&my>=y&&my<y+20)tooltip=value;
        }
        if(!data.getString("note").isEmpty())g.drawCenteredString(font,font.plainSubstrByWidth(data.getString("note"),w-24),width/2,top+h-61,0xff6c4b30);
        g.drawCenteredString(font,"보상 없음 · 공동 순위 · "+(cumulative?"누적 기록":data.getString("season").equals(data.getString("current"))?"실시간 갱신":"시즌 최종 기록"),width/2,top+h-47,0xff6c4b30);
        g.drawCenteredString(font,(data.getInt("page")+1)+" / "+data.getInt("pages")+(w<360?"":" 쪽 · "+data.getInt("total")+"명"),width/2,top+h-23,0xff56351e);
        super.render(g,mx,my,partial);if(tooltip!=null)g.renderTooltip(font,Component.literal(tooltip),mx,my);
    }
}
