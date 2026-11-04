package com.devilfruits.client.vfx.fx;

import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public class GroundWave extends Cinematic {
   private final double x;
   private final double y;
   private final double z;
   private final float radius;
   private final int ticks;
   private float strength = 1.2F;
   private double airY = Double.NaN;
   private float airRadius;

   private GroundWave(double x, double y, double z, float radius, int ticks) {
      super(new VfxAnchor(-1, new Vec3(x, y, z)), VfxParams.NONE);
      this.x = x;
      this.y = y;
      this.z = z;
      this.radius = radius;
      this.ticks = Math.max(2, ticks);
   }

   public static GroundWave at(double x, double groundY, double z, float radius, int ticks) {
      return new GroundWave(x, groundY + 0.04, z, radius, ticks);
   }

   public GroundWave strength(float s) {
      this.strength = s;
      return this;
   }

   public GroundWave air(double atY, float r) {
      this.airY = atY;
      this.airRadius = r;
      return this;
   }

   public GroundWave ring(int rgb, float alpha) {
      VfxParticles.emit(VfxPresets.RING_WAVE, this.x, this.y + 0.01, this.z, 0.0, 0.0, 0.0)
         .size(this.radius / 2.73F)
         .tint(rgb)
         .alpha(alpha)
         .life((float)this.ticks / 10.0F);
      return this;
   }

   public GroundWave dust(int count, int rgb, float inner, float speed, long seed) {
      return this.dust(count, rgb, inner, speed, 1.0F, seed);
   }

   public GroundWave dust(int count, int rgb, float inner, float speed, float size, long seed) {
      RandomSource r = RandomSource.create(seed);
      int n = VfxSettings.count((float)count);

      for (int i = 0; i < n; i++) {
         double a = (double)((float) (Math.PI * 2) * (float)i / (float)Math.max(1, n)) + r.nextDouble() * 0.3;
         double d = (double)inner * (0.8 + r.nextDouble() * 0.4);
         VfxParticles.emit(
               VfxPresets.DUST,
               this.x + Math.cos(a) * d,
               this.y + 0.25,
               this.z + Math.sin(a) * d,
               Math.cos(a) * (double)speed,
               0.015 + r.nextDouble() * 0.03,
               Math.sin(a) * (double)speed
            )
            .tint(rgb)
            .size(size);
      }

      return this;
   }

   public GroundWave spawn() {
      return VfxDirector.spawn(this);
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.age >= this.ticks) {
         this.finish();
      }
   }

   @Override
   public void render(VfxFrame f) {
      BufferBuilder d = f.distortion();
      if (d != null) {
         float p = Mth.clamp(((float)this.age + f.partial) / (float)this.ticks, 0.0F, 1.0F);
         float e = 1.0F - (1.0F - p) * (1.0F - p);
         VfxSprites.Sprite ring = VfxSprites.RING_NORMAL;
         MeshWriter.distortQuad(
            d,
            f.rx(this.x),
            f.ry(this.y),
            f.rz(this.z),
            (0.6F + e * this.radius) / 0.8F,
            this.strength * (1.0F - p),
            0,
            true,
            0.0F,
            0.0F,
            0.0F,
            0.0F,
            0.0F,
            0.0F,
            ring.u0(),
            ring.v0(),
            ring.u1(),
            ring.v1(),
            0.0F,
            0.0F
         );
         if (!Double.isNaN(this.airY)) {
            MeshWriter.distortQuad(
               d,
               f.rx(this.x),
               f.ry(this.airY),
               f.rz(this.z),
               (0.5F + e * this.airRadius) / 0.8F,
               this.strength * (1.0F - p),
               0,
               false,
               f.left.x,
               f.left.y,
               f.left.z,
               f.up.x,
               f.up.y,
               f.up.z,
               ring.u0(),
               ring.v0(),
               ring.u1(),
               ring.v1(),
               0.0F,
               0.0F
            );
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
