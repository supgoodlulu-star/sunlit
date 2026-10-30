package dev.sunlit.journal;

import java.util.List;

/** Numeric-only Blockbench keyframe sampling. Never evaluates strings or Molang. */
public final class PetAnimation {
 public record Key(float time,float x,float y,float z,boolean smooth) {
  float value(int axis){return axis==0?x:axis==1?y:z;}
 }
 public static float[] sample(List<Key> keys,float seconds,float duration,boolean loop,float fallback){
  if(keys.isEmpty())return new float[]{fallback,fallback,fallback};
  float t=loop&&duration>0?((seconds%duration)+duration)%duration:Math.max(0,Math.min(duration,seconds));
  if(t<=keys.get(0).time)return values(keys.get(0));
  if(t>=keys.get(keys.size()-1).time)return values(keys.get(keys.size()-1));
  int i=0;while(i+1<keys.size()&&keys.get(i+1).time<t)i++;
  Key b=keys.get(i),c=keys.get(i+1),a=keys.get(Math.max(0,i-1)),d=keys.get(Math.min(keys.size()-1,i+2));
  float u=(t-b.time)/(c.time-b.time);float[] out=new float[3];
  for(int axis=0;axis<3;axis++){
   float p=b.value(axis),q=c.value(axis);
   out[axis]=b.smooth?.5f*((2*p)+(-a.value(axis)+q)*u+(2*a.value(axis)-5*p+4*q-d.value(axis))*u*u+(-a.value(axis)+3*p-3*q+d.value(axis))*u*u*u):p+(q-p)*u;
  }
  return out;
 }
 private static float[] values(Key key){return new float[]{key.x,key.y,key.z};}
}
