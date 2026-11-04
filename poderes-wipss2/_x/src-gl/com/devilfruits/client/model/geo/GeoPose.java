package com.devilfruits.client.model.geo;

import java.util.Arrays;

public final class GeoPose {
   public final float[] rot;
   public final float[] pos;
   public final float[] scale;
   public final boolean[] hidden;

   public GeoPose(GeoModel model) {
      int n = model.boneCount();
      this.rot = new float[n * 3];
      this.pos = new float[n * 3];
      this.scale = new float[n * 3];
      this.hidden = new boolean[n];
      this.reset();
   }

   public void reset() {
      Arrays.fill(this.rot, 0.0F);
      Arrays.fill(this.pos, 0.0F);
      Arrays.fill(this.scale, 1.0F);
      Arrays.fill(this.hidden, false);
   }

   public void addRotation(int bone, float x, float y, float z) {
      if (bone >= 0) {
         this.rot[bone * 3] = this.rot[bone * 3] + x;
         this.rot[bone * 3 + 1] = this.rot[bone * 3 + 1] + y;
         this.rot[bone * 3 + 2] = this.rot[bone * 3 + 2] + z;
      }
   }

   public void addPosition(int bone, float x, float y, float z) {
      if (bone >= 0) {
         this.pos[bone * 3] = this.pos[bone * 3] + x;
         this.pos[bone * 3 + 1] = this.pos[bone * 3 + 1] + y;
         this.pos[bone * 3 + 2] = this.pos[bone * 3 + 2] + z;
      }
   }

   public void setScale(int bone, float s) {
      if (bone >= 0) {
         this.scale[bone * 3] = s;
         this.scale[bone * 3 + 1] = s;
         this.scale[bone * 3 + 2] = s;
      }
   }

   public void hide(int bone, boolean hide) {
      if (bone >= 0) {
         this.hidden[bone] = hide;
      }
   }
}
