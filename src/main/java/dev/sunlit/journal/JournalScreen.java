package dev.sunlit.journal;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Client is a presentation layer. No rewards, balances, or ownership are trusted here. */
public final class JournalScreen extends Screen {
    private CompoundTag data;
    private String tab;
    private int offset,heartbeat;
    private EditBox nickname;
    private final List<String> lines=new ArrayList<>();
    private int left,top,panelWidth,panelHeight;
    private static final String[] PAGES={"profile","skills","quests","achievements"};
    private static final String[] LABELS={"프로필","생활 기술","업무 일지","업적 도감"};
    private static final ResourceLocation PAPER=new ResourceLocation(SunlitJournal.MODID,"textures/gui/journal.png");
    public JournalScreen(CompoundTag data,String tab){super(Component.literal("모험가의 수첩"));this.data=data;this.tab=Arrays.asList(PAGES).contains(tab)?tab:"profile";}
    private void request(String action,String value){JournalNetwork.CHANNEL.sendToServer(new JournalNetwork.Request(action,value));}
    @Override protected void init(){rebuild();}
    public void update(CompoundTag next){data=next;rebuild();}
    private void button(String text,int x,int y,int w,Runnable action){addRenderableWidget(Button.builder(Component.literal(text),b->action.run()).bounds(x,y,w,20).build());}
    private void line(String text){
        for(var wrapped:font.split(Component.literal(text),Math.max(100,panelWidth-42)))lines.add(net.minecraft.util.FormattedCharSequence.EMPTY==wrapped?"":plain(wrapped));
    }
    private static String plain(net.minecraft.util.FormattedCharSequence text){StringBuilder s=new StringBuilder();text.accept((i,style,c)->{s.appendCodePoint(c);return true;});return s.toString();}
    private void rebuild(){
        String editing=nickname==null?data.getString("nickname"):nickname.getValue();
        boolean focus=nickname!=null&&nickname.isFocused();
        clearWidgets();lines.clear();nickname=null;
        panelWidth=Math.min(430,width-12);panelHeight=Math.min(380,height-12);left=(width-panelWidth)/2;top=(height-panelHeight)/2;
        int w=(panelWidth-20)/4;
        for(int i=0;i<4;i++){String key=PAGES[i];button(LABELS[i],left+10+i*w,top+28,w-2,()->{tab=key;offset=0;rebuild();});}
        int y=top+58;
        if(tab.equals("profile")){
            nickname=addRenderableWidget(new EditBox(font,left+12,y+28,panelWidth-24,20,Component.literal("닉네임")));
            nickname.setMaxLength(128);nickname.setValue(editing);nickname.setEditable(data.getBoolean("nickAvailable"));
            nickname.setFocused(focus);if(focus)setFocused(nickname);
            button("닉네임 변경",left+12,y+52,110,()->request("nick",nickname.getValue()));
            button("초기화",left+126,y+52,70,()->request("resetNick",""));
            var titleData=data.getCompound("titles");String[] owned=titleData.getString("owned").split("\\|");
            List<String> titles=Arrays.stream(owned).filter(s->!s.isBlank()).toList();
            int slots=Math.max(1,(panelHeight-206)/23);offset=Math.min(offset,Math.max(0,titles.size()-slots));
            for(int i=offset;i<Math.min(titles.size(),offset+slots);i++){
                String title=titles.get(i);button((title.equals(titleData.getString("equipped"))?"[장착] ":"")+title,left+12,y+100+(i-offset)*23,panelWidth-24,()->request("equip",title));
            }
            navigation(titles.size(),slots);
        }else{
            switch(tab){case "skills"->skills();case "quests"->quests();case "achievements"->achievements();}
            int rows=visibleRows();offset=Math.min(offset,Math.max(0,lines.size()-rows));
            navigation(lines.size(),rows);
        }
        button("닫기",left+panelWidth-62,top+panelHeight-28,50,this::onClose);
    }
    private int visibleRows(){return Math.max(1,(panelHeight-(tab.equals("quests")?137:108))/13);}
    private void navigation(int total,int rows){
        button("이전",left+12,top+panelHeight-28,48,()->{offset=Math.max(0,offset-rows);rebuild();});
        button("다음",left+64,top+panelHeight-28,48,()->{offset=Math.min(Math.max(0,total-rows),offset+rows);rebuild();});
    }
    private void skills(){
        CompoundTag s=data.getCompound("skillsData");
        if(!s.getBoolean("available")){line("생활 기술 조회 불가 — 모드 설치와 서버 로그를 확인해 주세요.");return;}
        var categories=s.getList("categories",Tag.TAG_COMPOUND);
        if(categories.isEmpty())line("서버에 설정된 생활 기술 카테고리가 없습니다.");
        for(int i=0;i<categories.size();i++){
            var c=categories.getCompound(i);line(c.getString("id")+(c.getBoolean("unlocked")?"":" [잠김]"));
            if(c.getBoolean("hasXp"))line("레벨 "+c.getInt("level")+" / 경험치 "+c.getInt("xp")+" / 다음 단계 요구 "+c.getInt("required"));
            var skills=c.getList("skills",Tag.TAG_STRING);
            for(int j=0;j<skills.size();j++)line("  해금: "+skills.getString(j));
        }
        line(s.getString("effectsNote"));
        button("원본 스킬 트리",left+118,top+panelHeight-28,110,()->request("skillsTree",""));
    }
    private void quests(){
        for(String period:List.of("daily","weekly")){
            var tasks=data.getCompound(period);line((period.equals("daily")?"일일 업무":"주간 업무")+" · "+tasks.getString("date")+" UTC");
            for(int i=0;i<3;i++){
                var t=tasks.getCompound(""+i);int kind=t.getInt("kind");
                line((i+1)+". "+JournalWorld.NAMES[Math.max(0,Math.min(4,kind))]+" "+t.getInt("progress")+"/"+t.getInt("target")+" · "+(t.getBoolean("claimed")?"수령 완료":"에메랄드 "+t.getInt("reward")+" + 경험치 "+t.getInt("reward")*10));
            }
        }
        var festival=data.getCompound("festival");
        line("공동 축제: "+(festival.getBoolean("available")?JournalWorld.festivalName(festival.getString("season")):"계절 조회 불가"));
        if(festival.getBoolean("available")){
            line("공동 진행 "+festival.getLong("progress")+"/"+festival.getLong("goal")+(festival.getBoolean("complete")?" [완료]":""));
            String uuid=minecraft.player==null?"":minecraft.player.getUUID().toString();
            line("나의 기여 "+festival.getCompound("contributions").getLong(uuid)+" · 완료 보상은 기여자에게 자동 지급");
        }
        // Claims have stable IDs; server rechecks dates, progress and claimed flags.
        int claimWidth=Math.max(32,(panelWidth-24)/6);
        for(int k=0;k<2;k++)for(int i=0;i<3;i++){
            String key=k==0?"daily":"weekly";int index=i;
            var t=data.getCompound(key).getCompound(""+i);
            Button b=addRenderableWidget(Button.builder(Component.literal((k==0?"일":"주")+(i+1)+" 수령"),x->request("claim",key+":"+index)).bounds(left+12+(k*3+i)*claimWidth,top+panelHeight-55,claimWidth-2,20).build());
            b.active=t.getInt("target")>0&&t.getInt("progress")>=t.getInt("target")&&!t.getBoolean("claimed");
        }
    }
    private void achievements(){
        var owned=new HashSet<>(Arrays.asList(data.getCompound("titles").getString("owned").split("\\|")));
        var counts=data.getCompound("counts");
        for(int k=0;k<5;k++)for(int i=0;i<4;i++){
            String title=SunlitJournal.TITLES[k*4+i];long count=counts.getLong(JournalWorld.ACTIVITIES[k]);
            line((owned.contains(title)?"[획득] ":"[미획득] ")+title+" · "+Math.min(count,JournalWorld.MILESTONES[i])+"/"+JournalWorld.MILESTONES[i]);
        }
        if(data.getBoolean("balanceAvailable")){
            long amount=data.getLong("spurs"),unit=data.getLong("perSun");
            String suns=unit>0?BigDecimal.valueOf(amount).divide(BigDecimal.valueOf(unit),8,RoundingMode.DOWN).stripTrailingZeros().toPlainString():"조회 불가";
            line("현재 개인 계좌 자산: "+suns+" Sun");
            for(int i=0;i<4;i++)line((owned.contains(SunlitJournal.TITLES[20+i])?"[획득] ":"[미획득] ")+SunlitJournal.TITLES[20+i]+" · "+suns+"/"+JournalWorld.SUN_TARGETS[i]+" Sun");
        }else line("현재 자산 조회 불가 — 계좌 또는 Numismatics API를 확인해 주세요. 경제 업적 판정 보류.");
        for(String season:List.of("SPRING","SUMMER","AUTUMN","WINTER"))line((owned.contains(JournalWorld.seasonTitle(season))?"[획득] ":"[미획득] ")+JournalWorld.seasonTitle(season));
        line((owned.contains("사계요정")?"[획득] ":"[미획득] ")+"사계요정 · 네 계절 축제 기여 완료");
    }
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        renderBackground(g);g.blit(PAPER,left,top,panelWidth,panelHeight,0,0,430,380,430,380);
        g.drawCenteredString(font,title,left+panelWidth/2,top+10,0x60472F);
        if(tab.equals("profile")){
            g.drawString(font,"사용자명: "+data.getString("username"),left+12,top+58,0x60472F,false);
            g.drawString(font,"현재 닉네임: "+(data.getBoolean("nickAvailable")?(data.getString("nickname").isEmpty()?"기본 사용자명":data.getString("nickname")):"조회 불가"),left+12,top+72,0x60472F,false);
            g.drawString(font,"보유 칭호 · 클릭하여 장착 / 해제",left+12,top+145,0x60472F,false);
        }else{
            int rows=visibleRows();
            for(int i=offset;i<Math.min(lines.size(),offset+rows);i++)g.drawString(font,lines.get(i),left+12,top+58+(i-offset)*13,0x60472F,false);
        }
        super.render(g,mx,my,delta);
    }
    @Override public void tick(){if(nickname!=null)nickname.tick();if(++heartbeat%200==0)request("refresh","");}
    @Override public void onClose(){request("close","");super.onClose();}
    @Override public boolean isPauseScreen(){return false;}
}
