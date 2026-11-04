package com.devilfruits.client.render.vfx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class VfxContext {
   public Matrix4f matrix;
   public Camera camera;
   public double camX;
   public double camY;
   public double camZ;
   public float partialTick;
   public float time;
   public VertexConsumer buffer;
   public BufferSource buffers;
   public final Vector3f camLeft = new Vector3f();
   public final Vector3f camUp = new Vector3f();
   public final Vector3f camLook = new Vector3f();

   public float rx(double worldX) {
      return (float)(worldX - this.camX);
   }

   public float ry(double worldY) {
      return (float)(worldY - this.camY);
   }

   public float rz(double worldZ) {
      return (float)(worldZ - this.camZ);
   }

   public float proximity(double worldX, double worldY, double worldZ, float near, float far, float minAlpha) {
      double dx = worldX - this.camX;
      double dy = worldY - this.camY;
      double dz = worldZ - this.camZ;
      float d = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
      if (d >= far) {
         return 1.0F;
      } else {
         return d <= near ? minAlpha : minAlpha + (1.0F - minAlpha) * (d - near) / (far - near);
      }
   }
}
