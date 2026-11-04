package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.client.render.CrackGeometry;
import com.devilfruits.client.render.VfxDraw;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class GroundCrackEffect extends VfxEffect {
   private static final Vector3f A = new Vector3f();
   private static final Vector3f B = new Vector3f();
   private static final Vector3f W = new Vector3f();
   private final double x;
   private final double y;
   private final double z;
   private final float radius;
   private final int duration;
   private final float[] cracks;
   private final int core;
   private final int glow;

   public GroundCrackEffect(Vec3 center, float radius, int rays, int duration, long seed, int core, int glow) {
      this.x = center.x;
      this.y = center.y + 0.03;
      this.z = center.z;
      this.radius = radius;
      this.duration = Math.max(4, duration);
      this.cracks = CrackGeometry.burst(seed, rays, 0.45F);
      this.core = core;
      this.glow = glow;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.age > this.duration) {
         this.finish();
      }
   }

   @Override
   public void renderGlow(VfxContext ctx) {
      float t = ((float)this.age + ctx.partialTick) / (float)this.duration;
      float grow = Math.min(1.0F, t * 4.0F);
      float alpha = t < 0.5F ? 1.0F : (1.0F - t) * 2.0F;
      float flicker = 0.8F + 0.2F * Mth.sin(((float)this.age + ctx.partialTick) * 2.3F);
      float cx = ctx.rx(this.x);
      float cy = ctx.ry(this.y);
      float cz = ctx.rz(this.z);

      for (int i = 0; i + 8 <= this.cracks.length; i += 8) {
         float birth = this.cracks[i + 6];
         if (!(birth > grow)) {
            A.set(cx + this.cracks[i] * this.radius, cy, cz + this.cracks[i + 1] * this.radius);
            B.set(cx + this.cracks[i + 3] * this.radius, cy, cz + this.cracks[i + 4] * this.radius);
            W.set(B).sub(A).cross(0.0F, 1.0F, 0.0F);
            if (!(W.lengthSquared() < 1.0E-8F)) {
               W.normalize();
               float w = 0.07F + 0.11F * this.cracks[i + 7];
               int a = (int)(alpha * flicker * 255.0F);
               VfxDraw.ribbon(ctx.buffer, ctx.matrix, A, B, W, w * 2.6F, this.glow >> 16 & 0xFF, this.glow >> 8 & 0xFF, this.glow & 0xFF, a / 3);
               VfxDraw.ribbon(ctx.buffer, ctx.matrix, A, B, W, w, this.core >> 16 & 0xFF, this.core >> 8 & 0xFF, this.core & 0xFF, a);
            }
         }
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
