package dev.sunlit.journal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class PetScreen extends Screen {
 private CompoundTag data;private int page,selected;private int left,top,w,h;private boolean previewAccessory=true;
 public PetScreen(CompoundTag data){super(Component.literal("생활 친구"));this.data=data;}
 public static void request(String action,String value){JournalNetwork.CHANNEL.sendToServer(new PetNetwork.Request(action,value));}
 public static void receive(PetNetwork.Snapshot msg){var mc=Minecraft.getInstance();if(msg.open())mc.setScreen(new PetScreen(msg.data()));else if(mc.screen instanceof PetScreen s){s.data=msg.data();s.rebuildWidgets();}}
 private String mode(){return data.getString("mode");}
 private List<CompoundTag> owned(){var list=new ArrayList<CompoundTag>();for(var t:data.getList("pets",Tag.TAG_COMPOUND))list.add((CompoundTag)t);list.sort(Comparator.comparing(t->t.getString("id")));return list;}
 private int count(){return mode().equals("shop")?60:owned().size();}
 private PetCatalog.Species species(){if(mode().equals("draw"))return PetCatalog.find(data.getString("lastDraw")).orElse(PetCatalog.ALL.get(0));if(mode().equals("shop"))return PetCatalog.ALL.get(Math.min(selected,59));var pets=owned();return pets.isEmpty()?PetCatalog.ALL.get(0):PetCatalog.find(pets.get(Math.min(selected,pets.size()-1)).getString("species")).orElse(PetCatalog.ALL.get(0));}
 private JournalButton button(String text,int x,int y,int width,Runnable action){return addRenderableWidget(new JournalButton(text,x,y,width,20,false,action));}
 @Override protected void init(){
  w=Math.min(470,width-12);h=Math.min(290,height-12);left=(width-w)/2;top=(height-h)/2;int half=w/2;
  button("×",left+w-27,top+6,20,this::onClose);
  if(mode().equals("draw")){button("한 장으로 뽑기",left+12,top+h-30,w-24,()->request("draw",""));return;}
  int per=perPage();int total=count();selected=Math.max(0,Math.min(selected,Math.max(0,total-1)));page=Math.max(0,Math.min(page,Math.max(0,(total-1)/per)));
  for(int i=page*per;i<Math.min(total,(page+1)*per);i++){
   int at=i;String name=mode().equals("shop")?PetCatalog.ALL.get(i).name():PetCatalog.find(owned().get(i).getString("species")).map(PetCatalog.Species::name).orElse("?");
   if(!mode().equals("shop")){String id=owned().get(i).getString("id");long copies=owned().stream().filter(t->t.getString("species").equals(owned().get(at).getString("species"))).count();if(copies>1)name+=" · "+id.substring(0,4);}
   var row=addRenderableWidget(new JournalButton((at==selected?"▶ ":"")+name,left+10,top+34+(i-page*per)*23,half-17,21,at==selected,()->{selected=at;rebuildWidgets();}));
  }
  button("〈",left+10,top+h-29,28,()->{page=Math.max(0,page-1);rebuildWidgets();});button("〉",left+half-37,top+h-29,28,()->{if((page+1)*per<count())page++;rebuildWidgets();});
  int x=left+half+6,bw=w-half-17;
  if(mode().equals("shop")){
   boolean unlocked=data.getCompound("unlocks").getBoolean(species().id());button(unlocked?"영구 해금됨":"구매 · 이리듐 "+species().price()+"개",x,top+h-29,bw,()->request("buy",species().id())).active=!unlocked;
   button(previewAccessory?"장식 미리보기 ✓":"기본 외형 보기",x,top+h-53,bw,()->{previewAccessory=!previewAccessory;rebuildWidgets();});
  }else if(!owned().isEmpty()){
   var r=owned().get(selected);String id=r.getString("id");boolean active=data.getString("active").equals(id);
   button(active?"함께 쉬기 (소환 해제)":"이 친구 소환하기",x,top+h-77,bw,()->request(active?"recall":"summon",id));
   boolean unlocked=data.getCompound("unlocks").getBoolean(species().id());
   var accessory=button(!unlocked?"장식 미해금":r.getBoolean("accessory")?"장식 벗기기":"장식 착용",x,top+h-53,bw,()->request("accessory",id));accessory.active=unlocked;
   if(!unlocked)accessory.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("스폰 장식 가게에서 이리듐 "+species().price()+"개로 영구 해금할 수 있어요.")));
   button("인형으로 꺼내기",x,top+h-29,bw,()->{Minecraft.getInstance().setScreen(new net.minecraft.client.gui.screens.ConfirmScreen(ok->{Minecraft.getInstance().setScreen(this);if(ok)request("withdraw",id);},Component.literal("인형으로 꺼낼까요?"),Component.literal("소환은 해제되고 장식은 벗겨집니다. 구매한 장식은 내 계정에 남아요.")));});
  }
 }
 private int perPage(){return Math.max(1,(h-76)/23);}
 private void line(GuiGraphics g,String s,int x,int y,int max){var lines=font.split(Component.literal(s),Math.max(30,max));for(var l:lines){g.drawString(font,l,x,y,0x533c29,false);y+=11;}}
 @Override public void render(GuiGraphics g,int mx,int my,float partial){
  renderBackground(g);g.fill(left-3,top-3,left+w+3,top+h+3,0xff795333);g.fill(left,top,left+w,top+h,0xffffedcb);g.fill(left+4,top+4,left+w-4,top+h-4,0xfff8e4bc);
  g.drawString(font,mode().equals("draw")?"생활 친구 · 펫 자판기":mode().equals("shop")?"생활 친구 · 전용 장식":"생활 친구 · 펫 보관함",left+12,top+12,0x573a27,false);
  var s=species();int half=w/2;
  if(mode().equals("draw")){
   PetRender.previewFit(g,s.id(),false,left+w/2,top+Math.min(h-105,143),w-40,Math.max(35,Math.min(h-140,110)),152);
   line(g,data.getString("lastDraw").isEmpty()?"어떤 친구를 만나게 될까요?":s.name()+" 인형",left+12,top+h-100,w-24);
   line(g,"일반 95% · 전설 5% / 중복 가능\n현재 계절 전설 상대 확률 1.5배\n계절: "+(data.getString("season").isEmpty()?"연동 없음 · 전설 균등":seasonName(data.getString("season")))+" · 뽑기권 "+data.getInt("tickets")+"장",left+12,top+h-83,w-24);
  }else{
   if(count()==0)line(g,"펫 인형을 손에 들고 우클릭하면 친구를 등록할 수 있어요.",left+12,top+42,half-24);
   else{
    boolean acc=mode().equals("shop")?previewAccessory:owned().get(selected).getBoolean("accessory");
    int previewHeight=Math.max(40,h-145);PetRender.previewFit(g,s.id(),acc,left+half+(w-half)/2,top+45+previewHeight,w-half-24,previewHeight-5,152);
    line(g,s.name()+" · "+(s.legendary()?"전설":"일반"),left+half+8,top+33,w-half-22);
    if(mode().equals("vault"))line(g,"#"+owned().get(selected).getString("id").substring(0,8)+(data.getString("active").equals(owned().get(selected).getString("id"))?" · 소환 중":" · 보관 중"),left+half+8,top+h-94,w-half-22);
    if(mode().equals("shop"))line(g,s.accessory(),left+half+8,top+h-78,w-half-22);
   }
   g.drawString(font,(page+1)+" / "+Math.max(1,(count()+perPage()-1)/perPage()),left+44,top+h-23,0x795333,false);
  }
  super.render(g,mx,my,partial);
 }
 private static String seasonName(String s){return switch(s){case "SPRING"->"봄";case "SUMMER"->"여름";case "AUTUMN"->"가을";case "WINTER"->"겨울";default->s;};}
 @Override public boolean isPauseScreen(){return false;}
}
