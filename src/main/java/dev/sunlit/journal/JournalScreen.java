package dev.sunlit.journal;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.FormattedCharSequence;
import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Scrollable presentation cards. All activity, reward and ownership checks stay on the server. */
public final class JournalScreen extends Screen {
    private CompoundTag data;
    private String tab;
    private String questPeriod="daily";
    private int lineStep;
    private int heartbeat,left,top,panelWidth,panelHeight,contentTop,contentBottom,totalHeight,achievementFilter=-1;
    private double scroll;
    private boolean draggingScroll;
    private EditBox nickname;
    private final List<Card> cards=new ArrayList<>();
    private final Set<String> expandedSkills=new HashSet<>();
    private static final String[] PAGES={"profile","skills","quests","achievements"};
    private static final String[] LABELS={"프로필","생활 기술","업무 일지","업적 도감"};
    private static final String[] ICONS={"farming","fishing","mining","cooking","ranching","wealth","spring","summer","autumn","winter","fairy","profile","exploration","life","books","skills"};
    private static final String[] ACTIVITY_HELP={
        "다 자란 작물을 직접 부숴 수확하면 진행도가 올라요. 덜 자란 작물은 포함되지 않아요.",
        "낚싯대로 물고기를 낚으면 진행도가 올라요. 보물과 쓰레기는 포함되지 않아요.",
        "자연에서 생성된 광석을 캐면 진행도가 올라요. 직접 설치한 광석은 포함되지 않아요.",
        "작업대에서 음식을 만들거나 화로에서 완성된 음식을 꺼내면 진행도가 올라요.",
        "동물에게 먹이를 주어 번식시키세요. 새끼가 태어나야 진행도가 올라요."};
    private static final int TEXT=0xFF3F3028,MUTED=0xFF594435;
    private static final class Card {
        final String title,body,badge;
        final int icon;
        final long value,goal;
        final String actionLabel;
        final Runnable action;
        final boolean enabled;
        List<FormattedCharSequence> heading,lines;
        int height,y,bodyOffset,progressOffset,actionOffset,badgeWidth;
        boolean section;
        JournalButton button;
        Card(String title,String body,int icon,String badge,long value,long goal,String actionLabel,boolean enabled,Runnable action) {
            this.title=title;this.body=body;this.icon=icon;this.badge=badge;this.value=value;this.goal=goal;
            this.actionLabel=actionLabel;this.enabled=enabled;this.action=action;
        }
    }
    public JournalScreen(CompoundTag data,String tab){super(Component.literal("모험가의 수첩"));this.data=data;this.tab=Arrays.asList(PAGES).contains(tab)?tab:"profile";}
    private void request(String action,String value){JournalNetwork.CHANNEL.sendToServer(new JournalNetwork.Request(action,value));}
    @Override protected void init(){rebuild();}
    public void update(CompoundTag next){data=next;rebuild();}
    private JournalButton button(String label,int x,int y,int w,int h,boolean selected,Runnable action){return addRenderableWidget(new JournalButton(label,x,y,w,h,selected,action));}
    private void card(String title,String body,int icon,String badge,long value,long goal){cards.add(new Card(title,body,icon,badge,value,goal,null,false,null));}
    private void section(String title,String body,int icon){card(title,body,icon,"",0,0);cards.get(cards.size()-1).section=true;}
    private void actionCard(String title,String body,int icon,String badge,long value,long goal,String label,boolean enabled,Runnable action){cards.add(new Card(title,body,icon,badge,value,goal,label,enabled,action));}
    private void rebuild(){
        String editing=nickname==null?data.getString("nickname"):nickname.getValue();
        boolean focused=nickname!=null&&nickname.isFocused();
        clearWidgets();cards.clear();nickname=null;
        panelWidth=Math.min(480,width-12);panelHeight=Math.min(400,height-12);
        left=(width-panelWidth)/2;top=(height-panelHeight)/2;
        int tabWidth=(panelWidth-28)/4;
        for(int i=0;i<PAGES.length;i++){
            String page=PAGES[i];button(LABELS[i],left+14+i*tabWidth,top+32,tabWidth-4,23,tab.equals(page),()->{tab=page;scroll=0;rebuild();});
        }
        contentTop=top+65;contentBottom=top+panelHeight-39;
        switch(tab){
            case "profile" -> profile(editing,focused);
            case "skills" -> skills();
            case "quests" -> quests();
            case "achievements" -> achievements();
        }
        int cardWidth=panelWidth-40;
        lineStep=Math.max(14,font.lineHeight+5);
        totalHeight=0;
        for(Card c:cards){
            c.badgeWidth=c.badge.isEmpty()?0:Math.max(54,font.width(c.badge)+16);
            c.heading=font.split(Component.literal(c.title),Math.max(40,cardWidth-(c.section?60:57+c.badgeWidth+12)));
            c.lines=font.split(Component.literal(c.body),cardWidth-(c.section?60:24));
            c.bodyOffset=c.section?10+c.heading.size()*lineStep+4:10+Math.max(28,c.heading.size()*lineStep)+10;
            if(c.section)c.height=20+Math.max(28,c.heading.size()*lineStep+4+c.lines.size()*lineStep);
            else {
                int cursor=c.bodyOffset+c.lines.size()*lineStep;
                if(c.goal>0){c.progressOffset=cursor+3;cursor+=lineStep+14;}
                if(c.action!=null){c.actionOffset=cursor+8;cursor+=36;}
                c.height=cursor+10;
            }
            c.y=totalHeight;totalHeight+=c.height+8;
            if(c.action!=null){c.button=button(c.actionLabel,0,0,94,20,false,c.action);c.button.active=c.enabled;}
        }
        totalHeight=Math.max(0,totalHeight-8);clampScroll();layoutActions();
        button("위로",left+14,top+panelHeight-29,43,20,false,()->{scroll-=Math.max(50,contentBottom-contentTop-16);clampScroll();layoutActions();});
        button("아래로",left+62,top+panelHeight-29,43,20,false,()->{scroll+=Math.max(50,contentBottom-contentTop-16);clampScroll();layoutActions();});
        button("닫기",left+panelWidth-62,top+panelHeight-29,48,20,false,this::onClose);
    }
    private void profile(String editing,boolean focused){
        boolean available=data.getBoolean("nickAvailable");
        if(available){
            nickname=addRenderableWidget(new EditBox(font,left+16,top+88,panelWidth-160,20,Component.literal("닉네임")));
            nickname.setMaxLength(128);nickname.setValue(editing);nickname.setFocused(focused);if(focused)setFocused(nickname);
            button("변경",left+panelWidth-135,top+88,54,20,false,()->request("nick",nickname.getValue()));
            button("초기화",left+panelWidth-77,top+88,61,20,false,()->request("resetNick",""));
        }
        contentTop=top+(available?120:86);
        String equipped=data.getCompound("titles").getString("equipped");
        List<String> owned=Arrays.stream(data.getCompound("titles").getString("owned").split("\\|")).filter(s->!s.isBlank()).toList();
        if(owned.isEmpty())card("아직 비어 있는 칭호 보관함","생활 활동과 축제에 참여해 나만의 칭호를 모아 보세요.",11,"",0,0);
        for(String name:owned)actionCard(name,"마음에 드는 칭호를 대표 칭호로 골라 보세요.",titleIcon(name),equipped.equals(name)?"장착 중":"보유",0,0,equipped.equals(name)?"칭호 해제":"칭호 장착",true,()->request("equip",name));
    }
    private static int titleIcon(String title){int i=Arrays.asList(SunlitJournal.TITLES).indexOf(title);return i<24&&i>=0?i/4:title.equals("사계요정")?10:title.equals("벚꽃요정")?6:title.equals("별빛요정")?7:title.equals("풍요요정")?8:title.equals("눈꽃요정")?9:11;}
    private void skills(){
        var categories=data.getCompound("skillsData").getList("categories",Tag.TAG_COMPOUND);
        if(categories.isEmpty())card("생활 기술","배울 수 있는 기술이 생기면 분야별로 표시됩니다.",0,"",0,0);
        for(int i=0;i<categories.size();i++){
            var c=categories.getCompound(i);String id=c.getString("id"),name=SkillLabels.category(id);
            boolean expanded=expandedSkills.contains(id);var unlocked=c.getList("skills",Tag.TAG_STRING);
            StringBuilder detail=new StringBuilder();
            if(c.getBoolean("hasXp"))detail.append("레벨 ").append(c.getInt("level")).append(" · 다음 레벨까지 필요한 경험치 ").append(Math.max(0,c.getInt("required")-c.getInt("xp"))).append("\n");
            detail.append("배운 기술 ").append(unlocked.size()).append("개");
            if(!c.getBoolean("unlocked"))detail.append(" · 원본 트리에서 해금 조건을 확인해 보세요.");
            int shown=expanded?unlocked.size():Math.min(3,unlocked.size());
            for(int j=0;j<shown;j++)detail.append("\n• ").append(SkillLabels.skill(id,unlocked.getString(j),j+1));
            if(shown<unlocked.size())detail.append("\n그 밖의 기술 ").append(unlocked.size()-shown).append("개");
            if(unlocked.size()>3)actionCard(name,detail.toString(),SkillCategoryIcons.select(id,name),c.getBoolean("unlocked")?"해금됨":"잠김",c.getBoolean("hasXp")?c.getInt("xp"):0,c.getBoolean("hasXp")?c.getInt("required"):0,expanded?"간단히 보기":"기술 펼치기",true,()->{if(!expandedSkills.add(id))expandedSkills.remove(id);rebuild();});
            else card(name,detail.toString(),SkillCategoryIcons.select(id,name),c.getBoolean("unlocked")?"해금됨":"잠김",c.getBoolean("hasXp")?c.getInt("xp"):0,c.getBoolean("hasXp")?c.getInt("required"):0);
        }
        if(data.getCompound("skillsData").getBoolean("available"))actionCard("원본 스킬 트리","각 기술의 효과와 해금 조건을 원본 스킬 트리에서 확인하세요.",10,"",0,0,"스킬 트리 열기",true,()->request("skillsTree",""));
    }
    private static String subjectName(CompoundTag task){
        ResourceLocation id=ResourceLocation.tryParse(task.getString("subject"));int kind=task.getInt("kind");
        if(id==null)return JournalWorld.NAMES[Math.max(0,Math.min(4,kind))]+" 활동";
        if(kind==0&&id.getNamespace().equals("minecraft")&&Set.of("wheat","carrots","potatoes","beetroots").contains(id.getPath()))
            return Component.translatable("sunlit_journal_core.task.crop."+id.getPath()).getString();
        return switch(kind){
            case 0,2 -> BuiltInRegistries.BLOCK.containsKey(id)?BuiltInRegistries.BLOCK.get(id).getName().getString():"지정 블록";
            case 1,3 -> BuiltInRegistries.ITEM.containsKey(id)?BuiltInRegistries.ITEM.get(id).getDescription().getString():"지정 아이템";
            case 4 -> BuiltInRegistries.ENTITY_TYPE.containsKey(id)?BuiltInRegistries.ENTITY_TYPE.get(id).getDescription().getString():"지정 동물";
            default -> "생활 활동";
        };
    }
    private static String objectParticle(String name){
        if(name.isEmpty())return "를";
        int c=name.codePointBefore(name.length());
        return c>=0xAC00&&c<=0xD7A3&&(c-0xAC00)%28!=0?"을":"를";
    }
    private static String taskDescription(CompoundTag task){
        int kind=Math.max(0,Math.min(4,task.getInt("kind")));String name=subjectName(task);
        if(!task.contains("subject"))return ACTIVITY_HELP[kind]+"\n이전 버전에서 이어 온 업무예요.";
        return switch(kind){
            case 0 -> "다 자란 "+name+" 작물을 직접 부숴 수확하세요.";
            case 1 -> "낚싯대로 "+name+objectParticle(name)+" 낚으세요.";
            case 2 -> "자연에서 생성된 "+name+objectParticle(name)+" 캐세요. 직접 설치한 광석은 포함되지 않아요.";
            case 3 -> name+objectParticle(name)+" 직접 만드세요. 제작하거나 화로에서 완성된 음식을 꺼내면 진행됩니다.";
            case 4 -> name+" 두 마리에게 먹이를 주어 번식시키세요. 새끼가 태어나면 진행됩니다.";
            default -> "";
        };
    }
    private void quests(){
        String[] periods={"daily","weekly","seasonal"};
        String[] labels={"일일 업무","주간 업무","계절 특별 업무"};
        int width=(panelWidth-28)/3;
        for(int i=0;i<periods.length;i++){
            String period=periods[i];
            button(labels[i],left+14+i*width,top+61,width-4,23,questPeriod.equals(period),()->{questPeriod=period;scroll=0;rebuild();});
        }
        contentTop=top+92;
        if(!questPeriod.equals("seasonal")){
            String period=questPeriod;
            var tasks=data.getCompound(period);boolean daily=period.equals("daily");
            section(daily?"오늘의 작은 부탁":"이번 주의 모험",(daily?"매일":"매주 월요일")+" 새 부탁이 도착해요. · "+tasks.getString("date"),daily?6:7);
            for(int i=0;i<3;i++){
                var task=tasks.getCompound(""+i);int index=i,kind=Math.max(0,Math.min(4,task.getInt("kind")));
                boolean claimed=task.getBoolean("claimed"),ready=task.getInt("target")>0&&task.getInt("progress")>=task.getInt("target");
                String verb=switch(kind){case 0->" 수확하기";case 1->" 낚기";case 2->" 캐기";case 3->" 만들기";default->" 번식시키기";};
                String body=taskDescription(task)+"\n보상 · 에메랄드 "+task.getInt("reward")+"개 + 경험치 "+task.getInt("reward")*10;
                actionCard(subjectName(task)+verb,body,kind,claimed?"수령 완료":daily?"일일":"주간",task.getInt("progress"),task.getInt("target"),claimed?"수령 완료":ready?"보상 받기":"진행 중",!claimed&&ready,()->request("claim",period+":"+index));
            }
        }
        if(!questPeriod.equals("seasonal"))return;
        var f=data.getCompound("festival");
        if(f.getBoolean("available")){
            String season=f.getString("season");int icon=seasonIcon(season);
            String uuid=minecraft.player==null?"":minecraft.player.getUUID().toString();
            card(JournalWorld.festivalName(season),"생활 활동으로 마을의 축제를 함께 준비해요.\n나의 기여 "+f.getCompound("contributions").getLong(uuid)+" · 완료되면 기여자에게 보상을 보내드려요.\n보상 · 에메랄드 16개 + 경험치 160",icon,f.getBoolean("complete")?"축제 완료":"함께해요",f.getLong("progress"),f.getLong("goal"));
        }else section("계절 특별 업무","새로운 계절 업무가 열리면 이곳에서 확인할 수 있어요.",10);
    }
    private static int seasonIcon(String season){return switch(season){case "SPRING"->6;case "SUMMER"->7;case "AUTUMN"->8;default->9;};}
    private void achievements(){
        String[] filters={"전체","농사","낚시","채광","요리","목축","자산","축제"};int w=(panelWidth-28)/filters.length;
        for(int i=0;i<filters.length;i++){int filter=i-1;button(filters[i],left+14+i*w,top+61,w-3,20,achievementFilter==filter,()->{achievementFilter=filter;scroll=0;rebuild();});}
        contentTop=top+88;
        var owned=new HashSet<>(Arrays.asList(data.getCompound("titles").getString("owned").split("\\|")));var counts=data.getCompound("counts");
        for(int k=0;k<5;k++)if(achievementFilter==-1||achievementFilter==k)for(int tier=0;tier<4;tier++){
            String name=SunlitJournal.TITLES[k*4+tier];long count=counts.getLong(JournalWorld.ACTIVITIES[k]);
            card(name,ACTIVITY_HELP[k]+"\n목표 · "+JournalWorld.MILESTONES[tier]+(k==1||k==4?"마리":"개"),k,owned.contains(name)?"획득":"도전 중",count,JournalWorld.MILESTONES[tier]);
        }
        if(achievementFilter==-1||achievementFilter==5){
            boolean hasBalance=data.getBoolean("balanceAvailable");long unit=data.getLong("perSun");
            for(int i=0;i<4;i++){
                String name=SunlitJournal.TITLES[20+i];String body="개인 계좌에 "+JournalWorld.SUN_TARGETS[i]+" Sun 이상 모아 보세요. 현금과 공동 계좌는 포함하지 않아요.";
                if(hasBalance&&unit>0)body+="\n현재 저금액 · "+BigDecimal.valueOf(data.getLong("spurs")).divide(BigDecimal.valueOf(unit),4,RoundingMode.DOWN).stripTrailingZeros().toPlainString()+" Sun";
                card(name,body,5,owned.contains(name)?"획득":"도전 중",hasBalance&&unit>0?data.getLong("spurs")/unit:0,hasBalance&&unit>0?JournalWorld.SUN_TARGETS[i]:0);
            }
        }
        if(achievementFilter==-1||achievementFilter==6){
            int completed=0;
            for(String season:List.of("SPRING","SUMMER","AUTUMN","WINTER")){
                boolean done=data.getCompound("seasons").getBoolean(season);if(done)completed++;
                String name=JournalWorld.seasonTitle(season);
                card(name,JournalWorld.festivalName(season)+"에 생활 활동으로 1회 이상 기여하고, 마을이 공동 목표를 달성하면 얻어요.",seasonIcon(season),owned.contains(name)?"획득":"도전 중",done?1:0,1);
            }
            card("사계요정","봄·여름·가을·겨울, 네 계절의 축제에 기여하고 각 축제를 완료해 보세요.",10,owned.contains("사계요정")?"획득":"도전 중",completed,4);
        }
    }
    private void clampScroll(){scroll=Math.max(0,Math.min(scroll,Math.max(0,totalHeight-(contentBottom-contentTop))));}
    private void layoutActions(){
        for(Card c:cards)if(c.button!=null){
            int y=contentTop+c.y-(int)scroll+c.actionOffset;c.button.setX(left+panelWidth-134);c.button.setY(y);
            c.button.visible=y>=contentTop&&y+20<=contentBottom;
        }
    }
    private static void rounded(GuiGraphics g,int x,int y,int w,int h,int radius,int color){JournalButton.rounded(g,x,y,w,h,radius,color);}
    private void centered(GuiGraphics g,String text,int center,int y,int color){g.drawString(font,text,center-font.width(text)/2,y,color,false);}
    private void icon(GuiGraphics g,int index,int x,int y){com.mojang.blaze3d.systems.RenderSystem.enableBlend();g.blit(new ResourceLocation(SunlitJournal.MODID,"textures/gui/badges/"+ICONS[Math.max(0,Math.min(ICONS.length-1,index))]+".png"),x,y,0,0,28,28,28,28);com.mojang.blaze3d.systems.RenderSystem.disableBlend();}
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        renderBackground(g);
        rounded(g,left+3,top+4,panelWidth,panelHeight,15,0x803E353D);
        rounded(g,left,top,panelWidth,panelHeight,15,0xFFC4A184);
        rounded(g,left+2,top+2,panelWidth-4,panelHeight-4,13,0xFFF0DFCA);
        rounded(g,left+8,top+8,panelWidth-16,18,9,0xFFE5CAB2);
        centered(g,title.getString(),left+panelWidth/2,top+13,TEXT);
        if(tab.equals("profile")){
            String name=data.getString("username"),nick=data.getString("nickname");
            g.drawString(font,font.plainSubstrByWidth(name+(nick.isEmpty()?"":" · "+nick),panelWidth-32),left+16,top+68,TEXT,false);
        }
        g.enableScissor(left+12,contentTop,left+panelWidth-12,contentBottom);
        int cardWidth=panelWidth-40;
        for(Card c:cards){
            int x=left+16,y=contentTop+c.y-(int)scroll;
            if(y+c.height<contentTop||y>contentBottom)continue;
            rounded(g,x,y+1,cardWidth,c.height,10,0xFFCDB59C);
            rounded(g,x+1,y,cardWidth-2,c.height-1,9,0xFFF7ECDD);
            icon(g,c.icon,x+10,y+10);
            if(c.section){
                int textY=y+10;
                for(var line:c.heading){g.drawString(font,line,x+47,textY,TEXT,false);textY+=lineStep;}
                textY=y+c.bodyOffset;
                for(var line:c.lines){g.drawString(font,line,x+47,textY,MUTED,false);textY+=lineStep;}
                continue;
            }
            int headingY=y+13;
            for(var line:c.heading){g.drawString(font,line,x+47,headingY,TitleThemes.known(c.title)?TitleThemes.color(c.title,false):TEXT,false);headingY+=lineStep;}
            if(!c.badge.isEmpty()){
                boolean done=c.badge.equals("획득")||c.badge.equals("수령 완료")||c.badge.equals("장착 중");
                rounded(g,x+cardWidth-c.badgeWidth-10,y+12,c.badgeWidth,18,7,done?0xFFD1E0C5:0xFFE7D1BB);
                centered(g,c.badge,x+cardWidth-10-c.badgeWidth/2,y+17,done?0xFF305439:MUTED);
            }
            int textY=y+c.bodyOffset;
            for(var line:c.lines){g.drawString(font,line,x+12,textY,MUTED,false);textY+=lineStep;}
            if(c.goal>0){
                int progressY=y+c.progressOffset,barY=progressY+lineStep+1;long value=Math.max(0,Math.min(c.value,c.goal));
                g.drawString(font,value+" / "+c.goal,x+12,progressY,TEXT,false);
                rounded(g,x+12,barY,cardWidth-24,5,2,0xFFD9C5AF);
                int fill=(int)((cardWidth-24)*Math.min(1.0,(double)value/c.goal));
                if(fill>0)rounded(g,x+12,barY,fill,5,2,value>=c.goal?0xFF73965F:0xFF7FA992);
            }
        }
        g.disableScissor();
        int viewport=contentBottom-contentTop;
        if(totalHeight>viewport){
            int thumb=Math.max(18,viewport*viewport/totalHeight),railX=left+panelWidth-17;
            rounded(g,railX,contentTop,4,viewport,2,0xFFD7BFA5);
            int thumbY=contentTop+(int)(scroll/(totalHeight-viewport)*(viewport-thumb));
            rounded(g,railX,thumbY,4,thumb,2,0xFF9A755D);
        }
        if(panelWidth>350)centered(g,"마우스 휠로 넘겨보기",left+panelWidth/2,top+panelHeight-22,MUTED);
        super.render(g,mx,my,delta);
    }
    @Override public boolean mouseScrolled(double x,double y,double delta){
        if(x>=left&&x<=left+panelWidth&&y>=contentTop&&y<=contentBottom){scroll-=delta*30;clampScroll();layoutActions();return true;}
        return super.mouseScrolled(x,y,delta);
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if(button==0&&totalHeight>contentBottom-contentTop&&x>=left+panelWidth-22&&x<=left+panelWidth-9&&y>=contentTop&&y<=contentBottom){draggingScroll=true;scrollTo(y);return true;}
        return super.mouseClicked(x,y,button);
    }
    private void scrollTo(double y){int viewport=contentBottom-contentTop,thumb=Math.max(18,viewport*viewport/Math.max(1,totalHeight));scroll=(y-contentTop-thumb/2.0)/Math.max(1,viewport-thumb)*(totalHeight-viewport);clampScroll();layoutActions();}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(draggingScroll){scrollTo(y);return true;}return super.mouseDragged(x,y,button,dx,dy);}
    @Override public boolean mouseReleased(double x,double y,int button){draggingScroll=false;return super.mouseReleased(x,y,button);}
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(nickname==null||!nickname.isFocused()){
            if(key==266||key==267){scroll+=(key==266?-1:1)*(contentBottom-contentTop-12);clampScroll();layoutActions();return true;}
        }
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public void tick(){if(nickname!=null)nickname.tick();if(++heartbeat%200==0)request("refresh","");}
    @Override public void onClose(){request("close","");super.onClose();}
    @Override public boolean isPauseScreen(){return false;}
}
