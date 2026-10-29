package dev.sunlit.journal;
import java.util.*;
import net.minecraft.util.RandomSource;
/** Stable IDs are save-format keys; never reuse a retired ID. */
public final class PetCatalog {
 public record Species(String id,String name,String accessory,String season,boolean fantasy){
  public boolean legendary(){return !season.isEmpty();}
  public int price(){return legendary()?20:10;}
  public boolean floats(){return id.contains("fairy")||id.contains("skywhale")||Set.of("cloud","whirlwind","flowerbud","pearl","crystal").contains(id);}
 }
 public static final List<Species> ALL=List.of(
  new Species("fox","여우","두루마리 가방","",false),
  new Species("dog","강아지","산책 스카프","",false),
  new Species("cat","고양이","붕어 방울 목걸이","",false),
  new Species("capybara","카피바라","머리 위 귤","",false),
  new Species("hyrax","바위너구리","풀잎 배낭","",false),
  new Species("red_panda","레서판다","대나무 도시락","",false),
  new Species("otter","수달","조개 목걸이","",false),
  new Species("ferret","페럿","탐험 파우치","",false),
  new Species("hedgehog","고슴도치","버섯 모자","",false),
  new Species("chinchilla","친칠라","라벤더 귀 리본","",false),
  new Species("hamster","햄스터","해바라기씨 주머니","",false),
  new Species("guinea_pig","기니피그","당근 턱받이","",false),
  new Species("raccoon","너구리","보자기 꾸러미","",false),
  new Species("squirrel","다람쥐","도토리 베레모","",false),
  new Species("quokka","쿼카","데이지 귀 장식","",false),
  new Species("koala","코알라","유칼립투스 화관","",false),
  new Species("meerkat","미어캣","탐험가 고글","",false),
  new Species("penguin","아기 펭귄","짧은 니트 목도리","",false),
  new Species("duck","아기 오리","밀짚모자","",false),
  new Species("seal","아기 물범","불가사리 머리핀","",false),
  new Species("sprout","새싹 정령","작은 물뿌리개 가방","",true),
  new Species("mushroom","버섯 정령","체크 목수건","",true),
  new Species("acorn","도토리 정령","잎사귀 나비넥타이","",true),
  new Species("moss_golem","이끼 골렘","미니 화분","",true),
  new Species("flowerbud","꽃봉오리 요정","꽃술 목걸이","",true),
  new Species("crystal","수정 정령","황동 받침 장식","",true),
  new Species("pebble","조약돌 골렘","작은 광부 헬멧","",true),
  new Species("amber","호박석 정령","금빛 나뭇잎 브로치","",true),
  new Species("pearl","진주 정령","산호 리본","",true),
  new Species("droplet","물방울 슬라임","나뭇잎 우산","",true),
  new Species("star_slime","별빛 슬라임","초승달 머리 장식","",true),
  new Species("cloud","구름 정령","작은 우편 가방","",true),
  new Species("whirlwind","꼬마 회오리","바람개비 스카프","",true),
  new Species("baby_dragon","아기 용","모험가 스카프","",true),
  new Species("griffin","아기 그리핀","황동 깃털 휘장","",true),
  new Species("unicorn","아기 유니콘","별 모양 머리핀","",true),
  new Species("phoenix","아기 불사조","금빛 발찌","",true),
  new Species("goblin","별사탕 도깨비","별사탕 단추 미니 망토","",true),
  new Species("book","책 요정","열쇠 모양 책갈피","",true),
  new Species("clockwork","태엽 인형","작은 공구통","",true),
  new Species("spring_fairy","봄의 요정","꽃잎 양산","SPRING",true),
  new Species("spring_dragon","봄의 용","꽃씨 우편 가방","SPRING",true),
  new Species("spring_haetae","봄의 해태","매화 매듭 장식","SPRING",true),
  new Species("spring_skywhale","봄의 하늘고래","꽃 배달 바구니","SPRING",true),
  new Species("spring_tiger","봄의 백호","꽃 자수 목수건","SPRING",true),
  new Species("summer_fairy","여름의 요정","진주 조개 손가방","SUMMER",true),
  new Species("summer_dragon","여름의 용","조개 방울 목걸이","SUMMER",true),
  new Species("summer_haetae","여름의 해태","물결 방울술","SUMMER",true),
  new Species("summer_skywhale","여름의 하늘고래","작은 닻 목걸이","SUMMER",true),
  new Species("summer_tiger","여름의 백호","파도 팔찌","SUMMER",true),
  new Species("autumn_fairy","가을의 요정","도토리 작은 등","AUTUMN",true),
  new Species("autumn_dragon","가을의 용","수확 바구니","AUTUMN",true),
  new Species("autumn_haetae","가을의 해태","단풍 노리개","AUTUMN",true),
  new Species("autumn_skywhale","가을의 하늘고래","곡식 가방","AUTUMN",true),
  new Species("autumn_tiger","가을의 백호","단풍 깃 장식","AUTUMN",true),
  new Species("winter_fairy","겨울의 요정","눈꽃 케이프","WINTER",true),
  new Species("winter_dragon","겨울의 용","눈꽃 니트 목도리","WINTER",true),
  new Species("winter_haetae","겨울의 해태","백옥 목걸이","WINTER",true),
  new Species("winter_skywhale","겨울의 하늘고래","별 모빌","WINTER",true),
  new Species("winter_tiger","겨울의 백호","목화 귀마개","WINTER",true)
 );
 public static Optional<Species> find(String id){return ALL.stream().filter(s->s.id().equals(id)).findFirst();}
 public static Species draw(RandomSource random,String season){
  if(random.nextInt(100)>=5)return ALL.get(random.nextInt(40));
  int total=0;for(int i=40;i<60;i++)total+=weight(ALL.get(i),season);
  int roll=random.nextInt(total);for(int i=40;i<60;i++){roll-=weight(ALL.get(i),season);if(roll<0)return ALL.get(i);}
  throw new AssertionError();
 }
 public static int weight(Species s,String season){return !season.isEmpty()&&s.season().equals(season)?3:2;}
}
