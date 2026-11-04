package com.devilfruits.client.vfx;

import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.VfxRenderer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import org.joml.Vector3f;

public final class VfxFrame {
   public Camera camera;
   public double camX;
   public double camY;
   public double camZ;
   public float partial;
   public float entityPartial;
   public float time;
   public final Vector3f left = new Vector3f();
   public final Vector3f up = new Vector3f();
   public final Vector3f look = new Vector3f();

   public float rx(double worldX) {
      return (float)(worldX - this.camX);
   }

   public float ry(double worldY) {
      return (float)(worldY - this.camY);
   }

   public float rz(double worldZ) {
      return (float)(worldZ - this.camZ);
   }

   public BufferBuilder mesh(MeshTex tex) {
      return VfxRenderer.mesh(tex);
   }

   public boolean allowMesh() {
      return VfxRenderer.allowMesh();
   }

   @Nullable
   public BufferBuilder distortion() {
      return VfxRenderer.distortion();
   }
}
