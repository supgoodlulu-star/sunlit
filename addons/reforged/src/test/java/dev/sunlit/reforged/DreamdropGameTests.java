package dev.sunlit.reforged;
import net.minecraftforge.gametest.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
@GameTestHolder("sunlit_reforged") @PrefixGameTestTemplate(false)
public final class DreamdropGameTests {
    @GameTest(template="empty") public static void legacyCubeInventorySurvivesVisualReplacement(GameTestHelper h){
        String[] ids={"suspicious_cube","artisan_cube","master_cube"};
        for(int i=0;i<ids.length;i++){
            var n=new CompoundTag();n.putString("id","sunlit_reforged:"+ids[i]);n.putByte("Count",(byte)37);
            var tag=new CompoundTag();tag.putString("ForeignOwner","legacy-player");tag.putInt("CustomModelData",712);n.put("tag",tag);
            var restored=ItemStack.of(n);h.assertTrue(restored.is(SunlitReforged.cube(Potential.Cube.values()[i]))&&restored.getCount()==37,"Existing inventory ID/count lost");
            h.assertTrue(restored.save(new CompoundTag()).equals(n),"Existing inventory metadata modified");
        }h.succeed();
    }
}
