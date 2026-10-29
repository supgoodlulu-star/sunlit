package dev.sunlit.journal;

import com.google.gson.*;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.core.particles.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
import java.io.*;

/** Shared original 3D meshes for companions, dolls, vault and shop previews. */
@Mod.EventBusSubscriber(modid=SunlitJournal.MODID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class PetRender extends EntityRenderer<PetEntity> {
 private static final ResourceLocation TEX=new ResourceLocation(SunlitJournal.MODID,"textures/pets/palette.png");
 private record Face(float x,float y,float z,int side,int color,String group){}
 private record Mesh(float scale,Map<String,List<Face>> groups){}
 private static final Map<String,Mesh> CACHE=new HashMap<>();
 private static BlockEntityWithoutLevelRenderer DOLL;
 private static boolean PREVIEW;
 public PetRender(EntityRendererProvider.Context ctx){super(ctx);shadowRadius=.3f;}
 @SubscribeEvent public static void reload(net.minecraftforge.client.event.RegisterClientReloadListenersEvent e){e.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)manager->CACHE.clear());}
 @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(PetContent.PET.get(),PetRender::new);}
 private static Mesh mesh(String species){return CACHE.computeIfAbsent(species,id->{
  try(var r=Minecraft.getInstance().getResourceManager().openAsReader(new ResourceLocation(SunlitJournal.MODID,"pets/"+id+".json"))){
   var obj=JsonParser.parseReader(r).getAsJsonObject();var faces=new ArrayList<Face>();for(var value:obj.getAsJsonArray("faces")){var a=value.getAsJsonArray();faces.add(new Face(a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat(),a.get(3).getAsInt(),a.get(4).getAsInt(),a.get(5).getAsString()));}var groups=new LinkedHashMap<String,List<Face>>();for(var face:faces)groups.computeIfAbsent(face.group,k->new ArrayList<>()).add(face);return new Mesh(obj.get("scale").getAsFloat(),groups);
  }catch(IOException|RuntimeException ex){throw new IllegalStateException("Cannot load pet mesh "+id,ex);}
 });}
 @Override public ResourceLocation getTextureLocation(PetEntity e){return TEX;}
 @Override public void render(PetEntity e,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
  pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(180-yaw));
  var spec=PetCatalog.find(e.species()).orElse(PetCatalog.ALL.get(0));float time=e.tickCount+partial;shadowRadius=e.species().endsWith("_fairy")?.12f:.3f;
  if(spec.floats())pose.translate(0,.22+Math.sin(time*.08)*.045,0);
  draw(e.species(),e.accessory(),e.petting(),time,e.getDeltaMovement().horizontalDistanceSqr()>.0005,pose,buffers,light);pose.popPose();super.render(e,yaw,partial,pose,buffers,light);
 }
 public static void draw(String species,boolean accessory,boolean smiling,float time,boolean walking,PoseStack pose,MultiBufferSource buffers,int light){
  if(PetCatalog.find(species).isEmpty())return;Mesh mesh=mesh(species);pose.pushPose();pose.scale(mesh.scale,mesh.scale,mesh.scale);var out=buffers.getBuffer(PREVIEW?RenderType.entityTranslucentEmissive(TEX):RenderType.entityCutoutNoCull(TEX));
  for(var group:mesh.groups.entrySet()){
   String name=group.getKey();if(name.equals("accessory")&&!accessory||name.equals("eyes")&&smiling||name.equals("smile")&&!smiling)continue;
   pose.pushPose();
   if(name.equals("tail"))pose.translate(Math.sin(time*(smiling?.45:.12))*(smiling?1.2:.3),0,0);
   if(name.startsWith("legs")&&walking)pose.translate(0,Math.sin(time*.6+(name.endsWith("back")?Math.PI:0))*.7,0);
   if(name.equals("wings"))pose.translate(0,Math.sin(time*.15)*(smiling?.1:.35),0);
   if(smiling&&(name.equals("head")||name.equals("eyes")||name.equals("smile")))pose.mulPose(Axis.ZP.rotationDegrees((float)Math.sin(time*.18)*5));
   for(Face face:group.getValue())face(face,pose.last(),out,light);pose.popPose();
  }pose.popPose();
 }
 private static final float[][][] QUADS={{{0,0,1},{0,0,0},{0,1,0},{0,1,1}},{{1,0,0},{1,0,1},{1,1,1},{1,1,0}},{{0,0,0},{0,0,1},{1,0,1},{1,0,0}},{{0,1,1},{0,1,0},{1,1,0},{1,1,1}},{{0,0,0},{1,0,0},{1,1,0},{0,1,0}},{{1,0,1},{0,0,1},{0,1,1},{1,1,1}}};
 private static final int[][] NORMALS={{-1,0,0},{1,0,0},{0,-1,0},{0,1,0},{0,0,-1},{0,0,1}};
 private static void face(Face f,PoseStack.Pose pose,VertexConsumer v,int light){var n=NORMALS[f.side];int c=f.color;float shade=PREVIEW?(f.side==3?1f:f.side==4?.98f:.92f):1f;for(int i=0;i<4;i++){var p=QUADS[f.side][i];v.vertex(pose.pose(),f.x+p[0],f.y+p[1],f.z+p[2]).color((int)((c>>16&255)*shade),(int)((c>>8&255)*shade),(int)((c&255)*shade),255).uv(.5f,.5f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(),n[0],n[1],n[2]).endVertex();}}
 public static void preview(GuiGraphics g,String species,boolean accessory,int x,int y,float size){
  var pose=g.pose();pose.pushPose();pose.translate(x,y,180);pose.scale(size,-size,size);pose.mulPose(Axis.XP.rotationDegrees(12));pose.mulPose(Axis.YP.rotationDegrees(152));
  boolean old=PREVIEW;PREVIEW=true;
  try{draw(species,accessory,false,(float)(net.minecraft.Util.getMillis()/50d),false,pose,g.bufferSource(),15728880);g.flush();}finally{PREVIEW=old;pose.popPose();}
 }
 public static BlockEntityWithoutLevelRenderer dollRenderer(){
  if(DOLL==null){var mc=Minecraft.getInstance();DOLL=new BlockEntityWithoutLevelRenderer(mc.getBlockEntityRenderDispatcher(),mc.getEntityModels()){
   @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){String id=stack.getOrCreateTag().getString("species");pose.pushPose();pose.translate(.5,.08,.5);pose.scale(.8f,.8f,.8f);pose.mulPose(Axis.YP.rotationDegrees(180));draw(id,false,false,0,false,pose,buffers,light);pose.popPose();}
  };}return DOLL;
 }
 public static void particles(PetEntity e){
  var spec=PetCatalog.find(e.species());if(spec.isEmpty()||!spec.get().legendary())return;
  boolean summon=e.tickCount<18;if(!summon&&(e.tickCount%12!=0||e.getDeltaMovement().horizontalDistanceSqr()<.0003))return;
  var random=e.getRandom();ParticleOptions particle=switch(spec.get().season()){case "SPRING"->ParticleTypes.CHERRY_LEAVES;case "SUMMER"->ParticleTypes.SPLASH;case "AUTUMN"->new DustParticleOptions(new org.joml.Vector3f(.8f,.43f,.14f),.6f);default->ParticleTypes.SNOWFLAKE;};
  e.level().addParticle(particle,e.getX()+(random.nextDouble()-.5)*.6,e.getY()+.3+random.nextDouble()*.5,e.getZ()+(random.nextDouble()-.5)*.6,0,.005,0);
 }
}
