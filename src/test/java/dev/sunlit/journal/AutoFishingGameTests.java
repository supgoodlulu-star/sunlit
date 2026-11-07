package dev.sunlit.journal;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
@GameTestHolder(SunlitJournal.MODID) @PrefixGameTestTemplate(false)
public final class AutoFishingGameTests {
    static AutoFishingSteering.View view(double bar,double speed,double fish,double progress,boolean chest,double chestPos){return new AutoFishingSteering.View(bar,speed,fish,0,36,progress,chest,chestPos,0,-1,false);}
    @GameTest(template="empty") public static void inputFollowsFishAndBrakesMomentum(GameTestHelper h){
        h.assertTrue(new AutoFishingSteering().decide(view(0,0,100,.2,false,0)).down(),"Does not lift the bar towards high fish");
        h.assertTrue(!new AutoFishingSteering().decide(view(90,0,20,.2,false,0)).down(),"Does not lower the bar towards low fish");
        h.assertTrue(!new AutoFishingSteering().decide(view(40,5,60,.2,false,0)).down(),"Fails to brake upward momentum before overshooting");h.succeed();
    }
    @GameTest(template="empty") public static void treasureOverlappingFishCanBeCollectedAtLowProgress(GameTestHelper h){
        var result=new AutoFishingSteering().decide(view(40,0,52,.2,true,58));h.assertTrue(result.chasingChest()&&result.target()>=36&&result.target()<=52,"Does not seek shared fish/chest interval");h.succeed();
    }
    @GameTest(template="empty") public static void dangerousTreasureDetoursReturnToFish(GameTestHelper h){
        var control=new AutoFishingSteering();h.assertTrue(!control.decide(view(0,0,10,.2,true,120)).chasingChest(),"Risky chest pursued on low progress");
        h.assertTrue(control.decide(view(0,0,10,.95,true,120)).chasingChest(),"Never attempts a chest when progress permits");h.assertTrue(!control.decide(view(90,0,10,.2,true,120)).chasingChest(),"Does not return to fish as progress falls");
        h.assertTrue(!new AutoFishingSteering().decide(new AutoFishingSteering.View(0,0,10,0,36,.36,true,120,0,-4,true)).chasingChest(),"Treasure bobber ignored dangerous travel time");h.succeed();
    }
    @GameTest(template="empty") public static void completedTreasureAndNativeBarSizesStayWithinBounds(GameTestHelper h){
        for(int width:new int[]{16,36,72,142}){var control=new AutoFishingSteering();var result=control.decide(new AutoFishingSteering.View(0,0,127,4,width,.9,true,5,1,-1,false));h.assertTrue(!result.chasingChest()&&result.target()>=0&&result.target()<=142-width,"Invalid target or collected chest pursued");
            for(double fish:new double[]{0,1,126,127})for(double chest:new double[]{5,120,129}){var edge=control.decide(new AutoFishingSteering.View(0,0,fish,0,width,.9,true,chest,0,-1,false));h.assertTrue(edge.target()>=0&&edge.target()<=142-width,"Treasure target escaped native bar bounds");}
        }h.succeed();
    }
}
