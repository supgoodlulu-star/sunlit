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
 private record Face(float x,float y,float z,int side,int color,String group,float sx,float sy,float sz){}
 private record Mesh(float scale,Map<String,List<Face>> groups){}
 private static final Map<String,Mesh> CACHE=new HashMap<>();
 private record Bounds(float minX,float minY,float maxX,float maxY){}
 private static final Map<String,Bounds> BOUNDS=new HashMap<>();
 private static BlockEntityWithoutLevelRenderer DOLL;
 private static boolean PREVIEW;
 private static final org.joml.Matrix3f GUI_NORMAL=new org.joml.Matrix3f();
 private static final RenderType PREVIEW_TYPE=PreviewType.make();
 /** Emissive GUI shading with real depth writes; vanilla emissive entities only write color. */
 private static final class PreviewType extends RenderType {
  private PreviewType(){super("sunlit_pet_preview",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,65536,false,false,()->{},()->{});}
  static RenderType make(){return create("sunlit_pet_preview",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,65536,false,false,
   CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER).setTextureState(new TextureStateShard(TEX,false,false)).setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_DEPTH_WRITE).setOverlayState(OVERLAY).createCompositeState(false));}
 }
 public PetRender(EntityRendererProvider.Context ctx){super(ctx);shadowRadius=.3f;}
 @SubscribeEvent public static void reload(net.minecraftforge.client.event.RegisterClientReloadListenersEvent e){e.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)manager->{CACHE.clear();BOUNDS.clear();});}
 @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(PetContent.PET.get(),PetRender::new);}
 private static Mesh mesh(String species){return CACHE.computeIfAbsent(species,id->{
  try(var r=Minecraft.getInstance().getResourceManager().openAsReader(new ResourceLocation(SunlitJournal.MODID,"pets/"+id+".json"))){
   var obj=JsonParser.parseReader(r).getAsJsonObject();var faces=new ArrayList<Face>();for(var value:obj.getAsJsonArray("faces")){var a=value.getAsJsonArray();faces.add(new Face(a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat(),a.get(3).getAsInt(),a.get(4).getAsInt(),a.get(5).getAsString(),a.size()>6?a.get(6).getAsFloat():1,a.size()>7?a.get(7).getAsFloat():1,a.size()>8?a.get(8).getAsFloat():1));}var groups=new LinkedHashMap<String,List<Face>>();for(var face:faces)groups.computeIfAbsent(face.group,k->new ArrayList<>()).add(face);return new Mesh(obj.get("scale").getAsFloat(),groups);
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
  if(PetCatalog.find(species).isEmpty())return;Mesh mesh=mesh(species);pose.pushPose();pose.scale(mesh.scale,mesh.scale,mesh.scale);var out=buffers.getBuffer(PREVIEW?PREVIEW_TYPE:RenderType.entityCutoutNoCull(TEX));
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
 private static void face(Face f,PoseStack.Pose pose,VertexConsumer v,int light){var n=NORMALS[f.side];int c=f.color;float offset=f.group.equals("eyes")||f.group.equals("smile")?.012f:0;float shade=PREVIEW?(f.side==3?1f:f.side==4?.96f:f.side==2?.78f:.86f):1f;for(int i=0;i<4;i++){var p=QUADS[f.side][i];v.vertex(pose.pose(),f.x+p[0]*f.sx+n[0]*offset,f.y+p[1]*f.sy+n[1]*offset,f.z+p[2]*f.sz+n[2]*offset).color((int)((c>>16&255)*shade),(int)((c>>8&255)*shade),(int)((c&255)*shade),255).uv(.5f,.5f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(PREVIEW?GUI_NORMAL:pose.normal(),PREVIEW?0:n[0],PREVIEW?1:n[1],PREVIEW?0:n[2]).endVertex();}}
 /** Fit actual rotated geometry above a caption, including low-hanging ornaments. */
 public static void previewFit(GuiGraphics g,String species,boolean accessory,int centerX,int bottomY,float maxWidth,float maxHeight,float angle){
  if(PetCatalog.find(species).isEmpty())return;
  Mesh model=mesh(species);
  Bounds b=BOUNDS.computeIfAbsent(species+":"+accessory+":"+angle,key->{
   var rotation=new org.joml.Matrix3f().rotateX((float)Math.toRadians(12)).rotateY((float)Math.toRadians(angle));
   float minX=Float.POSITIVE_INFINITY,minY=minX,maxX=Float.NEGATIVE_INFINITY,maxY=maxX;var point=new org.joml.Vector3f();
   for(var group:model.groups.entrySet()){
    if(group.getKey().equals("smile")||group.getKey().equals("accessory")&&!accessory)continue;
    for(Face f:group.getValue())for(var q:QUADS[f.side]){
     point.set(f.x+q[0]*f.sx,f.y+q[1]*f.sy,f.z+q[2]*f.sz);rotation.transform(point);
     minX=Math.min(minX,point.x);maxX=Math.max(maxX,point.x);minY=Math.min(minY,point.y);maxY=Math.max(maxY,point.y);
    }
   }return new Bounds(minX,minY,maxX,maxY);
  });
  float pixels=Math.min(Math.max(8,maxWidth-4)/Math.max(1,b.maxX-b.minX),Math.max(8,maxHeight-4)/Math.max(1,b.maxY-b.minY));
  preview(g,species,accessory,Math.round(centerX-(b.minX+b.maxX)*.5f*pixels),Math.round(bottomY+b.minY*pixels),pixels/model.scale,angle);
 }
 public static void preview(GuiGraphics g,String species,boolean accessory,int x,int y,float size){preview(g,species,accessory,x,y,size,152);}
 public static void preview(GuiGraphics g,String species,boolean accessory,int x,int y,float size,float angle){
  g.flush();
  var pose=g.pose();pose.pushPose();pose.translate(x,y,180);pose.scale(size,-size,size);pose.mulPose(Axis.XP.rotationDegrees(12));pose.mulPose(Axis.YP.rotationDegrees(angle));
  com.mojang.blaze3d.systems.RenderSystem.setShaderLights(new org.joml.Vector3f(.2f,1f,-.7f).normalize(),new org.joml.Vector3f(-.2f,1f,.7f).normalize());
  boolean old=PREVIEW;PREVIEW=true;
  try{draw(species,accessory,false,(float)(net.minecraft.Util.getMillis()/50d),false,pose,g.bufferSource(),15728880);com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();g.bufferSource().endBatch();}finally{PREVIEW=old;pose.popPose();Lighting.setupFor3DItems();}
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
