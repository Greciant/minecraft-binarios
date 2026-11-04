package com.devilfruits.client.limb;

import com.devilfruits.core.limb.LimbUv;
import net.minecraft.client.player.AbstractClientPlayer;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

public final class LimbFrame {
   AbstractClientPlayer player;
   float partial;
   boolean firstPerson;
   LimbUv.Layout layout;
   boolean rootPosed;
   boolean rootFree;
   double camX;
   double camY;
   double camZ;
   final Matrix4f toWorld = new Matrix4f();
   final Matrix4f toPx = new Matrix4f();
   final Vector3f pivot = new Vector3f();
   final Quaternionf posed = new Quaternionf();
   private final Vector3f scratch = new Vector3f();

   public AbstractClientPlayer player() {
      return this.player;
   }

   public float partial() {
      return this.partial;
   }

   public boolean firstPerson() {
      return this.firstPerson;
   }

   public LimbUv.Layout layout() {
      return this.layout;
   }

   public float time() {
      return (float)this.player.tickCount + this.partial;
   }

   public void rootPosed() {
      this.rootPosed = true;
   }

   public void rootAt(double x, double y, double z) {
      this.px(x, y, z, this.pivot);
      this.rootFree = true;
   }

   public Vector3f px(double x, double y, double z, Vector3f out) {
      return this.toPx.transformPosition((float)(x - this.camX), (float)(y - this.camY), (float)(z - this.camZ), out);
   }

   public Vector3f pxDirection(double x, double y, double z, Vector3f out) {
      return this.toPx.transformDirection((float)x, (float)y, (float)z, out).normalize();
   }

   public float pxLength(double blocks) {
      return (float)blocks * this.toPx.transformDirection(0.0F, 1.0F, 0.0F, this.scratch).length();
   }

   public Vector3d pivotWorld(Vector3d out) {
      this.toWorld.transformPosition(this.pivot.x, this.pivot.y, this.pivot.z, this.scratch);
      return out.set((double)this.scratch.x + this.camX, (double)this.scratch.y + this.camY, (double)this.scratch.z + this.camZ);
   }

   public Vector3f posed(float x, float y, float z, Vector3f out) {
      out.set(this.layout.centreX() + x, this.layout.minY() + y, this.layout.centreZ() + z);
      return this.posed.transform(out).add(this.pivot);
   }
}
