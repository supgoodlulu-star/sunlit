package dev.sunlit.journal;

/** Read-only observation -> normal hold/release input. No reward or progress mutations. */
public final class AutoFishingSteering {
    public record View(double bar,double velocity,double fish,double fishVelocity,int width,double progress,
                       boolean chestVisible,double chest,double chestProgress,double lossPerTick,boolean treasureBobber) {}
    public record Input(boolean down,boolean chasingChest,double target) {}
    private boolean chasing;
    public Input decide(View v){
        double max=Math.max(0,142-v.width),half=(v.width-14)/2d;
        double fishTarget=clamp(v.fish+v.fishVelocity*2-half,0,max),target=fishTarget;
        if(!v.chestVisible||v.chestProgress>=1)chasing=false;
        else {
            double lo=Math.max(0,Math.max(v.fish-v.width+14,v.chest-v.width+14)),hi=Math.min(max,Math.min(v.fish,v.chest));
            if(hi>=lo){target=clamp(fishTarget,lo,hi);chasing=true;}
            else {
                double returnTicks=Math.sqrt(Math.abs(v.chest-v.fish)/.7)*2+8;
                double reserve=Math.max(.25,(returnTicks*Math.abs(v.lossPerTick)+14)/120d);
                if(chasing&&v.progress<=reserve)chasing=false;
                if(!chasing&&v.progress>=Math.max(.78,reserve+.25))chasing=true;
                if(v.treasureBobber&&v.progress>Math.max(.35,reserve+.08))chasing=true;
                if(chasing)target=clamp(v.chest-half,0,max);
            }
        }
        // Brake before reaching the target instead of blindly following its current position.
        double stopping=v.velocity*Math.abs(v.velocity)/(2*.7);
        boolean down=target>v.bar+stopping;
        return new Input(down,chasing,target);
    }
    private static double clamp(double d,double min,double max){return Math.max(min,Math.min(max,d));}
}
