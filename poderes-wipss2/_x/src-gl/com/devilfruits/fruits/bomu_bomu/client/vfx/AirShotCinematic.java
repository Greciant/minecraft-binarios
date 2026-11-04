package com.devilfruits.fruits.bomu_bomu.client.vfx;

import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.devilfruits.fruits.bomu_bomu.entity.BombBullet;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public class AirShotCinematic extends Cinematic {
   private static final int TRAIL = 8;
   private final BombBullet shot;
   private final RandomSource random = RandomSource.create();
   private final double[] tx = new double[8];
   private final double[] ty = new double[8];
   private final double[] tz = new double[8];
   private int points;
   private int gone = -1;

   public AirShotCinematic(BombBullet shot) {
      super(VfxAnchor.of(shot, shot.position()), VfxParams.NONE);
      this.shot = shot;
      this.push(shot.position());
   }

   private void push(Vec3 p) {
      System.arraycopy(this.tx, 0, this.tx, 1, 7);
      System.arraycopy(this.ty, 0, this.ty, 1, 7);
      System.arraycopy(this.tz, 0, this.tz, 1, 7);
      this.tx[0] = p.x;
      this.ty[0] = p.y;
      this.tz[0] = p.z;
      this.points = Math.min(8, this.points + 1);
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.gone >= 0) {
         if (++this.gone > 6) {
            this.finish();
         }

         this.push(new Vec3(this.tx[0], this.ty[0], this.tz[0]));
      } else if (this.shot.isRemoved()) {
         this.gone = 0;
      } else {
         Vec3 now = this.shot.position();
         Vec3 before = new Vec3(this.tx[0], this.ty[0], this.tz[0]);
         this.push(now);
         Vec3 d = now.subtract(before);
         double len = d.length();
         if (!(len < 0.001)) {
            Vec3 dir = d.scale(1.0 / len);
            float strength = this.shot.isGust() ? 0.7F : 1.0F;
            Vec3 side = dir.cross(Math.abs(dir.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0)).normalize();
            Vec3 up = side.cross(dir).normalize();

            for (int i = 0; i < VfxSettings.count(6.0F * strength); i++) {
               double t = this.random.nextDouble();
               Vec3 p = before.add(d.scale(t)).add(this.spread(0.2), this.spread(0.2), this.spread(0.2));
               VfxParticles.emit(BomuLook.WIND, p.x, p.y, p.z, dir.x * 1.1, dir.y * 1.1, dir.z * 1.1).tint(15265524).alpha(0.6F * strength).size(1.2F);
            }

            for (int i = 0; i < VfxSettings.count(4.0F * strength); i++) {
               double t = this.random.nextDouble();
               double a = ((double)this.age * 1.3 + t * 4.0 + (double)i * 1.57) % (Math.PI * 2);
               double r = 0.35 + 0.15 * this.random.nextDouble();
               Vec3 ring = side.scale(Math.cos(a) * r).add(up.scale(Math.sin(a) * r));
               Vec3 tangent = side.scale(-Math.sin(a)).add(up.scale(Math.cos(a)));
               Vec3 p = before.add(d.scale(t)).add(ring);
               Vec3 v = dir.scale(0.45).add(tangent.scale(0.18));
               VfxParticles.emit(BomuLook.WIND, p.x, p.y, p.z, v.x, v.y, v.z).tint(14871024).alpha(0.5F * strength).size(0.9F);
            }

            if (this.age <= 3) {
               for (int i = 0; i < 2; i++) {
                  VfxParticles.emit(
                        VfxPresets.BLAST_SMOKE,
                        before.x + this.spread(0.15),
                        before.y + this.spread(0.15),
                        before.z + this.spread(0.15),
                        dir.x * 0.08 + this.spread(0.02),
                        dir.y * 0.08 + 0.01,
                        dir.z * 0.08 + this.spread(0.02)
                     )
                     .size(0.5F + (float)this.age * 0.2F)
                     .tint(15922424)
                     .alpha(0.22F * strength)
                     .life(0.18F)
                     .unflipped();
               }
            }

            double g = VfxWorld.ground(level, now.x, now.y, now.z, 3);
            if (!Double.isNaN(g) && now.y - g < 2.5 && this.random.nextFloat() < 0.8F) {
               int floor = VfxWorld.groundColour(level, now.x, g - 0.5, now.z);
               int pale = ((floor >> 16 & 0xFF) + 216) / 2 << 16 | ((floor >> 8 & 0xFF) + 212) / 2 << 8 | ((floor & 0xFF) + 204) / 2;
               int dust = VfxWorld.shade(pale, VfxWorld.light(level, now.x, g + 0.5, now.z));
               VfxParticles.emit(
                     VfxPresets.DUST,
                     now.x + this.spread(0.4),
                     g + 0.1,
                     now.z + this.spread(0.4),
                     dir.x * 0.15 + this.spread(0.03),
                     0.04,
                     dir.z * 0.15 + this.spread(0.03)
                  )
                  .tint(dust)
                  .size(0.55F)
                  .alpha(0.5F)
                  .life(0.6F);
            }
         }
      }
   }

   @Override
   public void render(VfxFrame f) {
      BufferBuilder d = f.distortion();
      if (d != null && this.points >= 2) {
         float p = f.partial;
         VfxSprites.Sprite glow = VfxSprites.GLOW;
         float base = this.shot.isGust() ? 0.7F : 1.1F;
         float fade = this.gone < 0 ? 1.0F : Math.max(0.0F, 1.0F - ((float)this.gone + p) / 6.0F);

         for (int i = 0; i < this.points - 1; i++) {
            float k = 1.0F - (float)i / (float)(this.points - 1);

            for (int j = 0; j < 2; j++) {
               double t = (double)j * 0.5;
               double x = Mth.lerp(t, this.tx[i], this.tx[i + 1]);
               double y = Mth.lerp(t, this.ty[i], this.ty[i + 1]);
               double z = Mth.lerp(t, this.tz[i], this.tz[i + 1]);
               MeshWriter.distortQuad(
                  d,
                  f.rx(x),
                  f.ry(y),
                  f.rz(z),
                  0.6F + 0.5F * (1.0F - k),
                  base * (0.4F + 0.6F * k) * fade,
                  1,
                  false,
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
                  f.time * 0.03F + (float)i * 0.21F,
                  0.6F + (float)j * 0.2F
               );
            }
         }
      }
   }

   private double spread(double a) {
      return (this.random.nextDouble() * 2.0 - 1.0) * a;
   }

   @Override
   public double x() {
      return this.tx[0];
   }

   @Override
   public double y() {
      return this.ty[0];
   }

   @Override
   public double z() {
      return this.tz[0];
   }
}
