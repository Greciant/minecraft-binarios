package com.devilfruits.client.haki;

import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class ImpactFlashEffect extends VfxEffect {
   private static final float[] SIZE = new float[]{0.55F, 1.0F, 0.85F, 0.6F, 0.35F, 0.2F};
   private final double x;
   private final double y;
   private final double z;
   private final float scale;
   private final float turn;
   private final Vector3f ray = new Vector3f();
   private final Vector3f across = new Vector3f();

   public ImpactFlashEffect(Vec3 at, float scale, float turn) {
      this.x = at.x;
      this.y = at.y;
      this.z = at.z;
      this.scale = scale;
      this.turn = turn;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.age >= SIZE.length) {
         this.finish();
      }
   }

   @Override
   public void renderShell(VfxContext ctx) {
      if (this.age < SIZE.length) {
         float s = SIZE[this.age] * this.scale;
         float cx = ctx.rx(this.x);
         float cy = ctx.ry(this.y);
         float cz = ctx.rz(this.z);
         this.star(ctx, cx, cy, cz, s * 1.25F, s * 0.22F, 242, 242, 242);
         this.star(ctx, cx, cy, cz, s, s * 0.12F, 11, 11, 16);
      }
   }

   private void star(VfxContext ctx, float cx, float cy, float cz, float length, float width, int r, int g, int b) {
      VertexConsumer vc = ctx.buffer;
      Matrix4f m = ctx.matrix;

      for (int i = 0; i < 8; i++) {
         double a = (double)this.turn + (double)i * Math.PI / 4.0;
         float len = i % 2 == 0 ? length : length * 0.55F;
         float c = (float)Math.cos(a);
         float sn = (float)Math.sin(a);
         this.ray.set(ctx.camLeft).mul(c).add(ctx.camUp.x * sn, ctx.camUp.y * sn, ctx.camUp.z * sn);
         this.across.set(ctx.camLeft).mul(-sn).add(ctx.camUp.x * c, ctx.camUp.y * c, ctx.camUp.z * c).mul(width * (i % 2 == 0 ? 1.0F : 0.7F));
         float tx = cx + this.ray.x * len;
         float ty = cy + this.ray.y * len;
         float tz = cz + this.ray.z * len;
         vc.addVertex(m, cx - this.across.x, cy - this.across.y, cz - this.across.z).setColor(r, g, b, 255);
         vc.addVertex(m, cx + this.across.x, cy + this.across.y, cz + this.across.z).setColor(r, g, b, 255);
         vc.addVertex(m, tx + this.across.x * 0.15F, ty + this.across.y * 0.15F, tz + this.across.z * 0.15F).setColor(r, g, b, 255);
         vc.addVertex(m, tx - this.across.x * 0.15F, ty - this.across.y * 0.15F, tz - this.across.z * 0.15F).setColor(r, g, b, 255);
      }
   }

   @Override
   public double x() {
      return this.x;
   }

   @Override
   public double y() {
      return this.y;
   }

   @Override
   public double z() {
      return this.z;
   }
}
