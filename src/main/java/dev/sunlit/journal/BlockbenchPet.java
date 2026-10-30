package dev.sunlit.journal;

import com.google.gson.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import java.io.*;
import java.util.*;

/** Client-only bone/UV renderer for the supplied Blockbench fox. */
public final class BlockbenchPet {
 private record Bone(String name,int parent,float[] origin,float[] rotation,boolean accessory,boolean pettingOnly,List<float[]> quads){}
 private record Track(int bone,String channel,List<PetAnimation.Key> keys){}
 private record Clip(float length,boolean loop,List<Track> tracks){}
 private record Model(float scale,float[] offset,float[] bounds,float[] accessoryBounds,List<ResourceLocation> textures,List<Bone> bones,Map<String,Clip> clips){}
 private static Model FOX;
 private static final org.joml.Matrix3f GUI_NORMAL=new org.joml.Matrix3f();
 public static void clear(){FOX=null;}
 public static boolean supports(String species){return species.equals("fox");}
 private static float[] array(JsonArray a){float[] values=new float[a.size()];for(int i=0;i<values.length;i++)values[i]=a.get(i).getAsFloat();return values;}
 private static float[] bounds(JsonArray a){float[] out=new float[6];System.arraycopy(array(a.get(0).getAsJsonArray()),0,out,0,3);System.arraycopy(array(a.get(1).getAsJsonArray()),0,out,3,3);return out;}
 private static Model model(){
  if(FOX!=null)return FOX;
  try(var reader=Minecraft.getInstance().getResourceManager().openAsReader(new ResourceLocation(SunlitJournal.MODID,"pets/fox.json"))){
   var root=JsonParser.parseReader(reader).getAsJsonObject();if(root.get("format").getAsInt()!=3)throw new IllegalStateException("Expected imported Blockbench fox");
   var textures=new ArrayList<ResourceLocation>();for(var t:root.getAsJsonArray("textures"))textures.add(new ResourceLocation(SunlitJournal.MODID,t.getAsJsonObject().get("path").getAsString()));
   var bones=new ArrayList<Bone>();for(var b:root.getAsJsonArray("bones")){var obj=b.getAsJsonObject();var quads=new ArrayList<float[]>();for(var q:obj.getAsJsonArray("quads"))quads.add(array(q.getAsJsonArray()));bones.add(new Bone(obj.get("name").getAsString(),obj.get("parent").getAsInt(),array(obj.getAsJsonArray("origin")),array(obj.getAsJsonArray("rotation")),obj.get("accessory").getAsBoolean(),obj.has("pettingOnly")&&obj.get("pettingOnly").getAsBoolean(),quads));}
   var clips=new HashMap<String,Clip>();for(var entry:root.getAsJsonObject("clips").entrySet()){
    var obj=entry.getValue().getAsJsonObject();var tracks=new ArrayList<Track>();for(var bone:obj.getAsJsonObject("tracks").entrySet())for(var channel:bone.getValue().getAsJsonObject().entrySet()){
     var keys=new ArrayList<PetAnimation.Key>();for(var k:channel.getValue().getAsJsonArray()){var row=k.getAsJsonArray();keys.add(new PetAnimation.Key(row.get(0).getAsFloat(),row.get(1).getAsFloat(),row.get(2).getAsFloat(),row.get(3).getAsFloat(),row.get(4).getAsString().equals("catmullrom")));}
     tracks.add(new Track(Integer.parseInt(bone.getKey()),channel.getKey(),keys));
    }clips.put(entry.getKey(),new Clip(obj.get("length").getAsFloat(),obj.get("loop").getAsBoolean(),tracks));
   }
   FOX=new Model(root.get("scale").getAsFloat(),array(root.getAsJsonArray("offset")),bounds(root.getAsJsonArray("bounds")),bounds(root.getAsJsonArray("accessoryBounds")),textures,bones,clips);return FOX;
  }catch(IOException|RuntimeException e){throw new IllegalStateException("Cannot load Blockbench fox",e);}
 }
 public static float[] bounds(boolean accessory){Model m=model();float[] b=(accessory?m.accessoryBounds:m.bounds).clone();for(int i=0;i<b.length;i++)b[i]*=m.scale;return b;}
 private static final class Pose {
  final float[] position=new float[3],rotation=new float[3],scale={1,1,1};
 }
 private static void apply(Model m,String name,float seconds,Pose[] poses){
  Clip clip=m.clips.get(name);if(clip==null)return;
  for(Track track:clip.tracks){Pose p=poses[track.bone];float[] value=PetAnimation.sample(track.keys,seconds,clip.length,clip.loop,track.channel.equals("scale")?1:0);
   for(int a=0;a<3;a++)switch(track.channel){case "position"->p.position[a]+=value[a];case "rotation"->p.rotation[a]+=value[a];case "scale"->p.scale[a]*=Math.max(.025f,value[a]);default->throw new IllegalStateException("Unknown animation channel");}
  }
 }
 public static void draw(boolean accessory,boolean petting,float ticks,boolean moving,PoseStack stack,MultiBufferSource buffers,int light){
  Model m=model();Pose[] poses=new Pose[m.bones.size()];for(int i=0;i<poses.length;i++)poses[i]=new Pose();float seconds=ticks/20;
  apply(m,"sit_idle",seconds,poses);apply(m,"tail_sway",seconds,poses);
  if(petting)apply(m,"pet",seconds,poses);
  else {apply(m,"blink",seconds,poses);if(moving)apply(m,"follow_hop",seconds,poses);else if(!PetRender.isPreview()){apply(m,"ear_twitch",seconds,poses);if(seconds%18<4.8)apply(m,"curious_look",seconds%18,poses);}}
  stack.pushPose();stack.scale(m.scale,m.scale,m.scale);stack.translate(m.offset[0],m.offset[1],m.offset[2]);
  for(int i=0;i<m.bones.size();i++)if(m.bones.get(i).parent<0)bone(m,i,poses,accessory,petting,stack,buffers,light);
  stack.popPose();
 }
 private static void bone(Model m,int index,Pose[] animation,boolean accessory,boolean petting,PoseStack stack,MultiBufferSource buffers,int light){
  Bone b=m.bones.get(index);if(b.accessory&&!accessory||b.pettingOnly&&!petting||petting&&b.name.startsWith("eye_"))return;Pose p=animation[index];stack.pushPose();
  float[] parent=b.parent<0?new float[3]:m.bones.get(b.parent).origin;
  stack.translate(b.origin[0]-parent[0]+p.position[0],b.origin[1]-parent[1]+p.position[1],b.origin[2]-parent[2]+p.position[2]);
  stack.mulPose(Axis.ZP.rotationDegrees(b.rotation[2]+p.rotation[2]));stack.mulPose(Axis.YP.rotationDegrees(b.rotation[1]+p.rotation[1]));stack.mulPose(Axis.XP.rotationDegrees(b.rotation[0]+p.rotation[0]));stack.scale(p.scale[0],p.scale[1],p.scale[2]);
  VertexConsumer out=null;int last=-1;boolean preview=PetRender.isPreview();var pose=stack.last();
  // The supplied texture already paints directional shading. Keep vanilla block/sky light,
  // but avoid multiplying it by harsh, alternating light on thousands of tiny cube walls.
  for(float[] q:b.quads){int tex=(int)q[0];if(tex!=last){out=buffers.getBuffer(PetRender.material(m.textures.get(tex)));last=tex;}
   for(int v=0;v<4;v++)out.vertex(pose.pose(),q[1+v*3],q[2+v*3],q[3+v*3]).color(255,255,255,255).uv(q[13+v*2],q[14+v*2]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(preview?GUI_NORMAL:pose.normal(),0,1,0).endVertex();
  }
  for(int i=index+1;i<m.bones.size();i++)if(m.bones.get(i).parent==index)bone(m,i,animation,accessory,petting,stack,buffers,light);
  stack.popPose();
 }
}
