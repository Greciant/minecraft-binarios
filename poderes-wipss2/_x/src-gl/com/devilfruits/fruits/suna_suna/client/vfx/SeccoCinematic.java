package com.devilfruits.fruits.suna_suna.client.vfx;

import com.devilfruits.client.render.vfx.VfxManager;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.post.VfxPost;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.mojang.blaze3d.vertex.BufferBuilder;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class SeccoCinematic extends SandCinematic {
   private static final int VAPOUR = 15660022;
   private final Vec3 centre;
   private float shown;
   private float lastShown;
   private int closedAt = -1;

   public SeccoCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      this.centre = params.pos();
   }

   @Nullable
   private DryGroundEffect patch() {
      return VfxManager.find(DryGroundEffect.class, e -> e.seed == this.seed);
   }

   public static void close(long seed) {
      SeccoCinematic c = VfxDirector.find(SeccoCinematic.class, s -> s.seed == seed);
      if (c != null && c.closedAt < 0) {
         c.closedAt = c.age;
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      DryGroundEffect patch = this.patch();
      if (patch != null && (this.closedAt < 0 || this.age - this.closedAt <= 30)) {
         this.lastShown = this.shown;
         this.shown = Math.max(this.shown, patch.shown());
         if (this.closedAt < 0 && !(this.shown < 0.3F)) {
            int tint = lit(level, this.centre.x, this.centre.y + 0.5, this.centre.z);
            Entity e = this.caster(level);
            Vec3 hand = e != null ? new Vec3(e.getX(), e.getY() + 0.25, e.getZ()).add(e.getLookAngle().multiply(0.6, 0.0, 0.6)) : this.centre;
            int n = VfxSettings.count(3.0F + this.shown * 0.6F);

            for (int i = 0; i < n; i++) {
               double ang = this.random.nextDouble() * (float) (Math.PI * 2);
               double d = Math.sqrt(this.random.nextDouble()) * (double)this.shown;
               double sx = this.centre.x + Math.cos(ang) * d;
               double sz = this.centre.z + Math.sin(ang) * d;
               double sy = this.ground(sx, sz) + 0.1;
               double k = 0.03 + 0.02 * this.random.nextDouble();
               VfxParticles.emit(VfxPresets.SMOKE, sx, sy, sz, (hand.x - sx) * k, 0.03 + this.random.nextDouble() * 0.02, (hand.z - sz) * k)
                  .tint(15660022)
                  .alpha(0.3F)
                  .size(0.7F)
                  .life(0.5F);
               if (i % 2 == 0) {
                  VfxParticles.emit(VfxPresets.GLASS_DUST, sx, sy + 0.2, sz, (hand.x - sx) * k * 1.5, 0.04, (hand.z - sz) * k * 1.5).tint(14479359).alpha(0.6F);
               }
            }

            boolean spreading = this.shown > this.lastShown + 0.01F;
            int front = VfxSettings.count(spreading ? 4.0F + this.shown * 0.9F : 0.8F);

            for (int ix = 0; ix < front; ix++) {
               double ang = this.random.nextDouble() * (float) (Math.PI * 2);
               double d = (double)this.shown * (0.92 + this.random.nextDouble() * 0.1);
               double sx = this.centre.x + Math.cos(ang) * d;
               double sz = this.centre.z + Math.sin(ang) * d;
               double sy = this.ground(sx, sz);
               puff(this.random, sx, sy + 0.12, sz, Math.cos(ang) * 0.06, 0.03, Math.sin(ang) * 0.06, tint).size(1.25F);
               if (ix % 2 == 0) {
                  grains(
                        this.random,
                        sx,
                        sy + 0.05,
                        sz,
                        Math.cos(ang) * 0.03 + this.spread(0.03),
                        0.12 + this.random.nextDouble() * 0.12,
                        Math.sin(ang) * 0.03 + this.spread(0.03),
                        tint
                     )
                     .life(0.6F);
               }
            }

            for (int ixx = 0; ixx < VfxSettings.count(1.5F); ixx++) {
               double ang = this.random.nextDouble() * (float) (Math.PI * 2);
               double d = Math.sqrt(this.random.nextDouble()) * (double)this.shown;
               double sx = this.centre.x + Math.cos(ang) * d;
               double sz = this.centre.z + Math.sin(ang) * d;
               double sy = this.ground(sx, sz);
               puff(this.random, sx, sy + 0.1, sz, this.spread(0.02), 0.07 + this.random.nextDouble() * 0.04, this.spread(0.02), tint).size(0.7F);
               grains(this.random, sx, sy + 0.05, sz, this.spread(0.04), 0.18 + this.random.nextDouble() * 0.1, this.spread(0.04), tint).life(0.5F);
            }

            if (e != null) {
               for (int ixx = 0; ixx < VfxSettings.count(2.0F); ixx++) {
                  double b = this.random.nextDouble() * (float) (Math.PI * 2);
                  puff(
                        this.random,
                        hand.x + Math.cos(b) * 0.6,
                        hand.y + this.random.nextDouble() * 0.3,
                        hand.z + Math.sin(b) * 0.6,
                        -Math.sin(b) * 0.1 - Math.cos(b) * 0.04,
                        0.02,
                        Math.cos(b) * 0.1 - Math.sin(b) * 0.04,
                        tint
                     )
                     .size(0.6F)
                     .alpha(0.8F);
               }
            }

            if (this.age % 9 == 3 && this.shown > 2.0F) {
               double ang = this.random.nextDouble() * (float) (Math.PI * 2);
               double d = 1.0 + this.random.nextDouble() * (double)(this.shown - 1.0F);
               double sx = this.centre.x + Math.cos(ang) * d;
               double sz = this.centre.z + Math.sin(ang) * d;
               double sy = this.ground(sx, sz);

               for (int ixx = 0; ixx < VfxSettings.count(6.0F); ixx++) {
                  double b = (double)ixx * 1.05;
                  puff(
                        this.random,
                        sx + Math.cos(b) * 0.3,
                        sy + 0.1 + (double)ixx * 0.12,
                        sz + Math.sin(b) * 0.3,
                        -Math.sin(b) * 0.1,
                        0.06,
                        Math.cos(b) * 0.1,
                        tint
                     )
                     .size(0.55F)
                     .alpha(0.7F);
               }
            }

            if (this.age % 2 == 0) {
               double ang = this.random.nextDouble() * (float) (Math.PI * 2);
               double d = Math.sqrt(this.random.nextDouble()) * (double)this.shown;
               double sx = this.centre.x + Math.cos(ang) * d;
               double sz = this.centre.z + Math.sin(ang) * d;
               grains(this.random, sx, this.ground(sx, sz) + 0.05, sz, this.spread(0.02), 0.05, this.spread(0.02), scale(tint, 0.85F)).life(0.8F);
            }

            if (this.local()) {
               VfxPost.grade(16769976, 0.22F);
            }
         }
      } else {
         this.finish();
      }
   }

   private double ground(double wx, double wz) {
      DryGroundEffect patch = this.patch();
      float s = patch != null ? patch.surfaceAt(wx, wz) : Float.NaN;
      return Float.isNaN(s) ? this.centre.y : (double)s;
   }

   @Override
   public void render(VfxFrame f) {
      float t = (float)this.age + f.partial;
      float r = Mth.lerp(f.partial, this.lastShown, this.shown);
      float fade = this.closedAt < 0 ? Math.min(1.0F, t / 10.0F) : Math.max(0.0F, 1.0F - ((float)(this.age - this.closedAt) + f.partial) / 20.0F);
      if (!(r < 0.5F) && !(fade <= 0.01F)) {
         BufferBuilder d = f.distortion();
         if (d != null) {
            VfxSprites.Sprite glow = VfxSprites.GLOW;
            int cells = Mth.clamp(Mth.ceil(r / 2.2F), 1, 4);
            float cell = r / (float)cells;

            for (int i = -cells; i < cells; i++) {
               for (int j = -cells; j < cells; j++) {
                  double wx = this.centre.x + ((double)i + 0.5) * (double)cell;
                  double wz = this.centre.z + ((double)j + 0.5) * (double)cell;
                  double dd = Math.sqrt(Mth.square(wx - this.centre.x) + Mth.square(wz - this.centre.z));
                  if (!(dd > (double)r)) {
                     MeshWriter.distortQuad(
                        d,
                        f.rx(wx),
                        f.ry(this.ground(wx, wz) + 0.45),
                        f.rz(wz),
                        cell * 0.85F,
                        0.55F * fade,
                        1,
                        true,
                        f.left.x,
                        f.left.y,
                        f.left.z,
                        f.up.x,
                        f.up.y,
                        f.up.z,
                        glow.u0(),
                        glow.v0(),
                        glow.u1(),
                        glow.v1(),
                        t * 0.02F + (float)i * 0.13F,
                        t * 0.035F + (float)j * 0.17F
                     );
                  }
               }
            }
         }
      }
   }

   @Override
   public double x() {
      return this.centre.x;
   }

   @Override
   public double y() {
      return this.centre.y;
   }

   @Override
   public double z() {
      return this.centre.z;
   }

   @Override
   public double cullDistance() {
      return 96.0;
   }
}
