package com.devilfruits.client.model.geo;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public final class GeoModel {
   public static final int STRIDE = 8;
   public final ResourceLocation id;
   public final int textureWidth;
   public final int textureHeight;
   public final GeoModel.Bone[] bones;
   private final Map<String, Integer> byName = new HashMap<>();

   GeoModel(ResourceLocation id, int textureWidth, int textureHeight, GeoModel.Bone[] bones) {
      this.id = id;
      this.textureWidth = textureWidth;
      this.textureHeight = textureHeight;
      this.bones = bones;

      for (GeoModel.Bone bone : bones) {
         this.byName.put(bone.name, bone.index);
      }
   }

   public int bone(String name) {
      Integer i = this.byName.get(name);
      return i == null ? -1 : i;
   }

   public int boneCount() {
      return this.bones.length;
   }

   public static final class Bone {
      public final String name;
      public final int index;
      public final int parent;
      public final float pivotX;
      public final float pivotY;
      public final float pivotZ;
      public final float rotX;
      public final float rotY;
      public final float rotZ;
      public final float[] mesh;

      Bone(String name, int index, int parent, float[] pivot, float[] rot, float[] mesh) {
         this.name = name;
         this.index = index;
         this.parent = parent;
         this.pivotX = pivot[0];
         this.pivotY = pivot[1];
         this.pivotZ = pivot[2];
         this.rotX = rot[0];
         this.rotY = rot[1];
         this.rotZ = rot[2];
         this.mesh = mesh;
      }

      public int vertexCount() {
         return this.mesh.length / 8;
      }
   }
}
