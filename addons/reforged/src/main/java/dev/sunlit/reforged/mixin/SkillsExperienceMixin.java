package dev.sunlit.reforged.mixin;
import dev.sunlit.reforged.Skills;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Pseudo
@Mixin(targets="net.puffish.skillsmod.SkillsMod",remap=false)
public abstract class SkillsExperienceMixin{
    @ModifyVariable(method="addExperience(Lnet/minecraft/server/level/ServerPlayer;Lnet/puffish/skillsmod/config/CategoryConfig;Lnet/puffish/skillsmod/config/experience/ExperienceConfig;Lnet/puffish/skillsmod/server/data/CategoryData;I)V",at=@At("HEAD"),argsOnly=true,ordinal=0,remap=false,require=1)
    private int sunlit$xp(int amount,ServerPlayer p,@Coerce Object category,@Coerce Object experience,@Coerce Object data,int original){return Skills.adjust(amount,p,category,data);}
}
