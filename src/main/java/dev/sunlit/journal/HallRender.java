package dev.sunlit.journal;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=SunlitJournal.MODID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class HallRender extends MobRenderer<HallFigure,PlayerModel<HallFigure>> {
    private final PlayerModel<HallFigure> wide,slim;
    public HallRender(EntityRendererProvider.Context c){super(c,new PlayerModel<>(c.bakeLayer(ModelLayers.PLAYER),false),.35f);wide=model;slim=new PlayerModel<>(c.bakeLayer(ModelLayers.PLAYER_SLIM),true);}
    @Override public void render(HallFigure figure,float yaw,float partial,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light){
        var mc=net.minecraft.client.Minecraft.getInstance();var info=mc.getConnection()==null?null:mc.getConnection().getPlayerInfo(figure.profile().getId());
        String kind=info!=null?info.getModelName():net.minecraft.client.resources.DefaultPlayerSkin.getSkinModelName(figure.profile().getId());
        if(info==null){var texture=mc.getSkinManager().getInsecureSkinInformation(figure.profile()).get(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN);if(texture!=null)kind=texture.getMetadata("model");}
        model="slim".equals(kind)?slim:wide;super.render(figure,yaw,partial,pose,buffers,light);
    }
    @Override public ResourceLocation getTextureLocation(HallFigure figure){
        if(!figure.occupied())return DefaultPlayerSkin.getDefaultSkin(figure.profile().getId());
        var mc=net.minecraft.client.Minecraft.getInstance();var player=mc.getConnection()==null?null:mc.getConnection().getPlayerInfo(figure.profile().getId());
        if(player!=null)return player.getSkinLocation();
        // The skin manager caches authenticated profile lookups for offline ranked players.
        return mc.getSkinManager().getInsecureSkinLocation(figure.profile());
    }
    @Override protected void renderNameTag(HallFigure figure,net.minecraft.network.chat.Component text,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light){
        if(entityRenderDispatcher.distanceToSqr(figure)>1024)return;
        String[] lines=text.getString().replace(" | "," · ").split(" · ");
        pose.pushPose();pose.translate(0,figure.getBbHeight()+.5,0);pose.mulPose(entityRenderDispatcher.cameraOrientation());pose.scale(-.025f,-.025f,.025f);
        for(int i=0;i<lines.length;i++){
            String line=getFont().plainSubstrByWidth(lines[i],120);
            getFont().drawInBatch(line,-getFont().width(line)/2f,(i-lines.length+1)*11,0xffffedcb,false,pose.last().pose(),buffers,net.minecraft.client.gui.Font.DisplayMode.NORMAL,0xcc49301e,15728880);
        }
        pose.popPose();
    }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(HallContent.FIGURE.get(),HallRender::new);}
}
