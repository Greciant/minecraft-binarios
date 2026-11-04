package com.devilfruits.client.render.vfx;

import com.devilfruits.client.render.VfxDraw;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class ShockwaveEffect extends VfxEffect {
   private final ShockwaveEffect.Mode mode;
   private final double x;
   private final double y;
   private final double z;
   private final Vector3f dir = new Vector3f();
   private final Vector3f u = new Vector3f();
   private final Vector3f v = new Vector3f();
   private final float length;
   private final float spread;
   private final int duration;
   private final int rings;
   private final float strength;
   private int tintR = 255;
   private int tintG = 255;
   private int tintB = 255;
   private int shellR = 245;
   private int shellG = 245;
   private int shellB = 245;

   public ShockwaveEffect(ShockwaveEffect.Mode mode, Vec3 origin, Vec3 direction, float length, float spread, int duration, int rings, float strength) {
      this.mode = mode;
      this.x = origin.x;
      this.y = origin.y;
      this.z = origin.z;
      if (mode == ShockwaveEffect.Mode.GROUND) {
         this.dir.set(0.0F, 1.0F, 0.0F);
      } else {
         this.dir.set((float)direction.x, (float)direction.y, (float)direction.z);
         if (this.dir.lengthSquared() < 1.0E-6F) {
            this.dir.set(0.0F, 0.0F, 1.0F);
         }

         this.dir.normalize();
      }

      VfxDraw.basis(this.dir, this.u, this.v);
      this.length = length;
      this.spread = spread;
      this.duration = Math.max(2, duration);
      this.rings = rings;
      this.strength = strength;
   }

   public ShockwaveEffect tint(int r, int g, int b) {
      this.tintR = r;
      this.tintG = g;
      this.tintB = b;
      return this;
   }

   public ShockwaveEffect shell(int r, int g, int b) {
      this.shellR = r;
      this.shellG = g;
      this.shellB = b;
      return this;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.age > this.duration + this.rings * 2 + 2) {
         this.finish();
      }
   }

   @Override
   public void renderShell(VfxContext ctx) {
      this.draw(ctx, false);
   }

   @Override
   public void renderGlow(VfxContext ctx) {
      this.draw(ctx, true);
   }

   private void draw(VfxContext ctx, boolean glow) {
      for (int k = 0; k < this.rings; k++) {
         float t = ((float)this.age + ctx.partialTick - (float)k * 1.6F) / (float)this.duration;
         if (!(t <= 0.0F) && !(t >= 1.0F)) {
            float ease = 1.0F - (1.0F - t) * (1.0F - t);
            float fade = (float)Math.pow((double)(1.0F - t), 1.4) * this.strength;
            float cx;
            float cy;
            float cz;
            float radius;
            float thickness;
            if (this.mode == ShockwaveEffect.Mode.CONE) {
               float d = 0.3F + this.length * ease;
               cx = ctx.rx(this.x) + this.dir.x * d;
               cy = ctx.ry(this.y) + this.dir.y * d;
               cz = ctx.rz(this.z) + this.dir.z * d;
               radius = 0.3F + d * this.spread;
               thickness = 0.3F + radius * 0.35F;
            } else {
               cx = ctx.rx(this.x);
               cy = ctx.ry(this.y) + 0.12F + (float)k * 0.04F;
               cz = ctx.rz(this.z);
               radius = 0.5F + this.length * ease;
               thickness = 0.6F + radius * 0.18F;
            }

            if (glow) {
               VfxDraw.ring(ctx, cx, cy, cz, this.u, this.v, radius, thickness * 0.25F, 40, this.tintR, this.tintG, this.tintB, fade * 0.35F);
            } else {
               VfxDraw.ring(ctx, cx, cy, cz, this.u, this.v, radius, thickness, 40, this.shellR, this.shellG, this.shellB, fade * 0.45F);
               VfxDraw.ring(ctx, cx, cy, cz, this.u, this.v, radius * 0.92F, thickness * 0.6F, 40, 255, 255, 255, fade * 0.25F);
            }
         }
      }
   }

   @Override
   public double cullDistance() {
      return 128.0;
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

   public static enum Mode {
      CONE,
      GROUND;
   }
}
