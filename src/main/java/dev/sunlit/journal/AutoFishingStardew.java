package dev.sunlit.journal;

import java.lang.reflect.*;

/** Optional, client-only adapter for Stardew Fishing 3.7's own input API. */
public final class AutoFishingStardew {
    public static final String SCREEN="com.bonker.stardewfishing.client.FishingScreen";
    private final Object screen,game;
    private final Method input;
    private final AutoFishingSteering steering=new AutoFishingSteering();
    public AutoFishingStardew(Object screen)throws ReflectiveOperationException{
        this.screen=screen;this.game=field(screen,"minigame");this.input=screen.getClass().getMethod("setInputDown",boolean.class);
    }
    public static boolean isScreen(Object screen){return screen!=null&&screen.getClass().getName().equals(SCREEN);}
    static Object field(Object obj,String name)throws ReflectiveOperationException{var f=obj.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(obj);}
    double number(String method)throws ReflectiveOperationException{return ((Number)game.getClass().getMethod(method).invoke(game)).doubleValue();}
    boolean flag(String method)throws ReflectiveOperationException{return (Boolean)game.getClass().getMethod(method).invoke(game);}
    public String tick()throws ReflectiveOperationException{
        String phase=field(screen,"status").toString();
        if(!phase.equals("MINIGAME")){input.invoke(screen,false);return "animation";}
        var view=new AutoFishingSteering.View(number("getBobberPos"),((Number)field(game,"bobberVelocity")).doubleValue(),number("getFishPos"),((Number)field(game,"fishVelocity")).doubleValue(),(int)number("getBarSize"),number("getProgress"),flag("isChestVisible"),number("getChestPos"),number("getChestProgress"),((Number)field(game,"lineStrength")).doubleValue(),(Boolean)field(game,"hasTreasureBobber"));
        var action=steering.decide(view);input.invoke(screen,action.down());return action.chasingChest()?"treasure":"tracking";
    }
    public void release()throws ReflectiveOperationException{input.invoke(screen,false);}
    public boolean owns(Object current){return screen==current;}
}
