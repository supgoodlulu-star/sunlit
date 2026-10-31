package dev.sunlit.reforged;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
/** QA-only screen: uses Minecraft's actual item renderer, never a pre-rendered model image. */
public final class DreamdropPreviewScreen extends Screen {
    public DreamdropPreviewScreen(){super(Component.literal("꿈방울 렌더링 검증"));}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        g.fill(0,0,width,height,0xff30252c);g.drawCenteredString(font,"꿈방울 · 실제 Minecraft 렌더링",width/2,18,0xffffdfac);
        g.drawCenteredString(font,"위: 인벤토리 아이콘 / 아래: 입체 아이템 모델",width/2,37,0xffd9c3bb);
        for(int i=0;i<3;i++){
            var stack=new ItemStack(SunlitReforged.cube(Potential.Cube.values()[i]));int x=width*(i+1)/4;
            g.drawCenteredString(font,stack.getHoverName(),x,67,0xffffedcf);
            g.pose().pushPose();g.pose().translate(x-32,90,0);g.pose().scale(4,4,1);g.renderItem(stack,0,0);g.pose().popPose();
            g.flush();Lighting.setupFor3DItems();g.pose().pushPose();g.pose().translate(x,245,120);g.pose().scale(115,-115,115);g.pose().mulPose(Axis.XP.rotationDegrees(12));g.pose().mulPose(Axis.YP.rotationDegrees(155));
            minecraft.getItemRenderer().renderStatic(stack,ItemDisplayContext.NONE,15728880,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,g.pose(),g.bufferSource(),minecraft.level,0);
            g.flush();g.pose().popPose();Lighting.setupForFlatItems();
            g.drawCenteredString(font,"둥근 병 · 마개 · 리본 · 장식",x,310,0xffd6c3bb);
        }
        super.render(g,mx,my,partial);
    }
    @Override public boolean isPauseScreen(){return false;}
}
