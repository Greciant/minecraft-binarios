package com.devilfruits.fruits.suna_suna.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.fx.ShockRing;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.render.MeshTex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public class SandBlastCinematic extends SandCinematic {
   private static final int LIFE = 11;
   private final float radius;
   private final boolean onGround;

   private SandBlastCinematic(ClientLevel level, Vec3 at, float radius, long seed) {
      super(level, new VfxAnchor(-1, at), new VfxParams(at, Vec3.ZERO, radius, seed, 0));
      this.radius = radius;
      this.onGround = !Double.isNaN(this.groundY) && at.y - this.groundY < 1.6;
   }

   @Override
   protected boolean followsCaster() {
      return false;
   }

   public static void at(ClientLevel level, Vec3 at, Vec3 dir, float radius, long seed) {
      SandBlastCinematic c = VfxDirector.spawn(new SandBlastCinematic(level, at, radius, seed));
      c.burst(level, at, dir);
   }

   private void burst(ClientLevel level, Vec3 at, Vec3 dir) {
      RandomSource r = this.random;
      int tint = lit(level, at.x, at.y + 0.5, at.z);
      float k = this.radius / 3.0F;
      Vec3 push = dir.lengthSqr() < 1.0E-6 ? Vec3.ZERO : dir.normalize().scale(0.12);
      double base = this.onGround ? this.groundY : at.y;
      int surge = VfxSettings.count(10.0F + 8.0F * k);

      for (int i = 0; i < surge; i++) {
         double a = ((double)i + r.nextDouble() * 0.6) * (float) (Math.PI * 2) / (double)surge;
         double s = (0.16 + r.nextDouble() * 0.14) * (0.6 + 0.4 * (double)k);
         double up = this.onGround ? 0.02 + r.nextDouble() * 0.05 : (r.nextDouble() - 0.4) * s;
         plume(
               r,
               at.x + Math.cos(a) * (double)this.radius * 0.2,
               base + (this.onGround ? 0.35 : 0.0) + r.nextDouble() * 0.3,
               at.z + Math.sin(a) * (double)this.radius * 0.2,
               Math.cos(a) * s + push.x,
               up,
               Math.sin(a) * s + push.z,
               tint
            )
            .size((0.8F + r.nextFloat() * 0.5F) * (0.6F + 0.5F * k));
      }

      int up = VfxSettings.count(5.0F + 4.0F * k);

      for (int i = 0; i < up; i++) {
         Vec3 d = jitter(r, new Vec3(0.0, 1.0, 0.0), 0.6F);
         double s = 0.08 + r.nextDouble() * 0.12 * (0.6 + 0.4 * (double)k);
         plume(r, at.x, base + 0.3, at.z, d.x * s + push.x, d.y * s, d.z * s + push.z, tint).size((0.7F + r.nextFloat() * 0.5F) * (0.5F + 0.5F * k));
      }

      int grains = VfxSettings.count(18.0F + 14.0F * k);

      for (int i = 0; i < grains; i++) {
         Vec3 d = jitter(r, new Vec3(0.0, 1.0, 0.0), this.onGround ? 0.75F : 1.0F);
         double s = (0.25 + r.nextDouble() * 0.35) * (0.7 + 0.3 * (double)k);
         grains(r, at.x, base + 0.3, at.z, d.x * s, Math.abs(d.y) * s * (this.onGround ? 1.2 : 1.0) + 0.08, d.z * s, tint)
            .life(1.2F + r.nextFloat() * 0.6F)
            .size(1.0F + r.nextFloat() * 0.5F);
      }

      int streaks = VfxSettings.count(6.0F + 6.0F * k);

      for (int i = 0; i < streaks; i++) {
         double a = r.nextDouble() * (float) (Math.PI * 2);
         double s = 0.5 + r.nextDouble() * 0.4;
         streak(at.x, base + 0.25 + r.nextDouble() * 0.6, at.z, Math.cos(a) * s, 0.04 + r.nextDouble() * 0.1, Math.sin(a) * s, tint).size(1.0F + k * 0.5F);
      }

      clouds(r, at.x, at.y + (this.onGround ? 0.6 : 0.0), at.z, 0.0, 0.03, 0.0, 1.4F + 0.8F * k, 4.0F + 3.0F * k, tint);
      if (this.onGround) {
         decal(r, at.x, this.groundY, at.z, this.radius * 2.2F, tint);
         decal(r, at.x, this.groundY + 0.005, at.z, this.radius * 1.2F, tint);
      }

      int haze = VfxSettings.count(2.0F + 2.0F * k);

      for (int i = 0; i < haze; i++) {
         haze(
               r,
               at.x + this.spread((double)this.radius * 0.5),
               base + 0.6 + r.nextDouble() * (double)this.radius * 0.4,
               at.z + this.spread((double)this.radius * 0.5),
               this.spread(0.02),
               0.01,
               this.spread(0.02),
               tint
            )
            .alpha(0.55F)
            .size(0.6F + 0.4F * k);
      }

      Vec3 c = new Vec3(at.x, base + (this.onGround ? 0.4 : 0.0), at.z);
      ShockRing.at(c, new Vec3(0.0, 1.0, 0.0)).radius(this.radius * 0.3F, this.radius * 1.8F).life(8).strength(0.7F).band(0.3F).rim(0.0F, 16777215).spawn();
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.age > 11) {
         this.finish();
      }
   }

   @Override
   public void render(VfxFrame f) {
      float t = (float)this.age + f.partial;
      float q = Math.min(1.0F, t / 11.0F);
      if (!(q >= 1.0F)) {
         ClientLevel level = Minecraft.getInstance().level;
         int tint = level != null ? lit(level, this.x, this.y + 0.5, this.z) : 16777215;
         float ease = 1.0F - (1.0F - q) * (1.0F - q) * (1.0F - q);
         double cy = this.onGround ? this.groundY : this.y;
         float dome = this.radius * (0.25F + 0.95F * ease);
         float alpha = 0.85F * (1.0F - q) * (1.0F - q);
         SandDraw.sphere(
            f,
            MeshTex.SAND_STREAM,
            this.x,
            cy,
            this.z,
            dome,
            0.0F,
            1.0F,
            0.0F,
            this.onGround ? 0.7F : 1.0F,
            -t * 0.25F,
            1.6F,
            t * 0.02F,
            3.0F,
            tint,
            alpha,
            7,
            22,
            true,
            this.onGround ? 1.8207964F : (float) Math.PI
         );
         if (this.onGround) {
            float rr = this.radius * (0.35F + 1.05F * ease);
            SandDraw.loop(
               f,
               this.x,
               this.groundY + 0.12,
               this.z,
               1.0F,
               0.0F,
               0.0F,
               0.0F,
               0.0F,
               1.0F,
               rr,
               this.radius * (0.45F - 0.2F * q),
               40,
               t * 0.1F,
               6.0F,
               tint,
               0.8F * (1.0F - q)
            );
         }
      }
   }

   @Override
   public double y() {
      return this.y;
   }

   public static int tint(ClientLevel level, Vec3 at) {
      return lit(level, at.x, at.y + 0.5, at.z);
   }

   static boolean onFloor(ClientLevel level, Vec3 at) {
      double g = VfxWorld.ground(level, at.x, at.y + 0.5, at.z, 4);
      return !Double.isNaN(g) && at.y - g < 1.6;
   }
}
