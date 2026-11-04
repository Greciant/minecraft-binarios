package com.devilfruits.fruits.suna_suna.client.vfx;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.CameraEffects;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.client.sound.LoopSound;
import com.devilfruits.fruits.suna_suna.SandStorm;
import com.devilfruits.fruits.suna_suna.SunaGround;
import com.devilfruits.fruits.suna_suna.SunaParticles;
import com.devilfruits.fruits.suna_suna.SunaSounds;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class SandstormEffect extends VfxEffect {
   private static final int LEVELS = 16;
   public final long seed;
   private final double initial;
   private final double speed;
   private final float radius;
   private final int life;
   private double x;
   private double y;
   private double z;
   private double px;
   private double py;
   private double pz;
   private int brokeAt = -1;

   public SandstormEffect(Vec3 at, Vec3 velocity, float radius, int life, long seed) {
      this.seed = seed;
      this.initial = Math.atan2(velocity.z, velocity.x);
      this.speed = velocity.horizontalDistance();
      this.radius = radius;
      this.life = life;
      this.x = this.px = at.x;
      this.y = this.py = at.y;
      this.z = this.pz = at.z;
      final SandstormEffect self = this;
      Minecraft.getInstance()
         .getSoundManager()
         .play(new LoopSound(SunaSounds.SABLES_LOOP.get(), null, at, 2.5F, 0.7F, 0, () -> !self.isDone() && self.brokeAt < 0) {
            @Override
            public void tick() {
               super.tick();
               this.x = self.x;
               this.y = self.y + 4.0;
               this.z = self.z;
            }
         });
   }

   public void blowApart(Vec3 at) {
      if (this.brokeAt < 0) {
         this.brokeAt = this.age;
         this.x = this.px = at.x;
         this.y = this.py = at.y;
         this.z = this.pz = at.z;
         RandomSource r = Fx.random();
         int n = Fx.count(this.radius * 14.0F);

         for (int i = 0; i < n; i++) {
            double a = r.nextDouble() * Math.PI * 2.0;
            double h = r.nextDouble() * 20.0 * 0.7;
            double s = 0.25 + r.nextDouble() * 0.35;
            Fx.spawn(
               r.nextInt(3) == 0
                  ? (ParticleOptions)SunaParticles.DUST.get()
                  : (r.nextInt(2) == 0 ? (ParticleOptions)SunaParticles.CLUMP.get() : (ParticleOptions)SunaParticles.GRAIN.get()),
               this.x + Math.cos(a) * (double)this.radius * 0.6,
               this.y + h,
               this.z + Math.sin(a) * (double)this.radius * 0.6,
               Math.cos(a) * s,
               0.05,
               Math.sin(a) * s
            );
         }
      }
   }

   private float size(float t) {
      float env = SandStorm.envelope(t, this.life);
      return this.brokeAt < 0 ? Math.max(0.15F, env) : Math.max(0.0F, 0.6F * (1.0F - ((float)(this.age - this.brokeAt) + 0.0F) / 10.0F));
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      this.px = this.x;
      this.py = this.y;
      this.pz = this.z;
      if (this.brokeAt >= 0) {
         if (this.age - this.brokeAt > 10) {
            this.finish();
         }
      } else if (this.age > this.life + 40) {
         this.blowApart(new Vec3(this.x, this.y, this.z));
      } else {
         int k = this.age - 1;
         if (k < this.life) {
            double h = SandStorm.heading(this.initial, this.seed, k);
            this.x = this.x + Math.cos(h) * this.speed;
            this.z = this.z + Math.sin(h) * this.speed;
            double g = SunaGround.surface(level, this.x, this.y, this.z, 3, 6);
            if (!Double.isNaN(g)) {
               this.y = g;
            }
         }

         float s = this.size((float)k);
         float r = this.radius * s;
         RandomSource rand = Fx.random();
         int n = Fx.count(10.0F * s + 2.0F * r);

         for (int i = 0; i < n; i++) {
            double h = rand.nextDouble() * 20.0 * (double)s;
            double rr = ringRadius(h / 20.0, (double)r) * (0.7 + rand.nextDouble() * 0.5);
            double a = rand.nextDouble() * Math.PI * 2.0;
            double tang = 0.35 + rand.nextDouble() * 0.2;
            Fx.spawn(
               rand.nextInt(3) == 0 ? (ParticleOptions)SunaParticles.GRAIN.get() : (ParticleOptions)SunaParticles.DUST.get(),
               this.x + Math.cos(a) * rr,
               this.y + h,
               this.z + Math.sin(a) * rr,
               -Math.sin(a) * tang,
               0.12 + rand.nextDouble() * 0.1,
               Math.cos(a) * tang
            );
         }

         int foot = Fx.count(r * 0.8F);

         for (int i = 0; i < foot; i++) {
            double a = rand.nextDouble() * Math.PI * 2.0;
            double d = (double)r * (0.8 + rand.nextDouble() * 0.6);
            Fx.spawn(
               (ParticleOptions)SunaParticles.CLUMP.get(),
               this.x + Math.cos(a) * d,
               this.y + 0.2,
               this.z + Math.sin(a) * d,
               -Math.sin(a) * 0.2 - Math.cos(a) * 0.08,
               0.15,
               Math.cos(a) * 0.2 - Math.sin(a) * 0.08
            );
         }

         if (this.age % 10 == 0) {
            CameraEffects.shakeAt(new Vec3(this.x, this.y, this.z), 0.15F * s, (double)(this.radius * 4.0F));
         }
      }
   }

   private static double ringRadius(double f, double r) {
      return r * (0.32 + 0.85 * Math.pow(Mth.clamp(f, 0.0, 1.0), 1.3));
   }

   @Override
   public void renderTextured(VfxContext ctx) {
      float pt = ctx.partialTick;
      float t = (float)this.age + pt;
      float s = this.size((float)(this.age - 1) + pt);
      if (!(s <= 0.02F)) {
         double cx = Mth.lerp((double)pt, this.px, this.x);
         double cy = Mth.lerp((double)pt, this.py, this.y);
         double cz = Mth.lerp((double)pt, this.pz, this.z);
         float r = this.radius * s;
         float flung = this.brokeAt < 0 ? 0.0F : ((float)(this.age - this.brokeAt) + pt) / 10.0F;
         int light = SandArt.light(cx, cy + 2.0, cz);
         VertexConsumer vc = SandArt.wispy(ctx, SandArt.STORM);
         funnel(ctx, vc, cx, cy, cz, r * 0.45F * (1.0F + flung), s, t * 0.07F + 0.15F, t * 0.06F, 1.0F * (1.0F - flung), light);
         funnel(ctx, vc, cx, cy, cz, r * 0.7F * (1.0F + flung), s, t * 0.05F, t * 0.05F, 1.0F * (1.0F - flung), light);
         funnel(ctx, vc, cx, cy, cz, r * (1.0F + flung * 1.5F), s, t * 0.035F + 0.3F, t * 0.035F, 0.8F * (1.0F - flung), light);
         funnel(ctx, vc, cx, cy, cz, r * 1.35F * (1.0F + flung * 2.0F), s, t * 0.02F + 0.6F, t * 0.02F, 0.45F * (1.0F - flung), light);
         VertexConsumer lumps = SandArt.solid(ctx, SandArt.PACKED);

         for (int l = 0; l < 16; l += 2) {
            float f = ((float)l + 0.5F) / 16.0F;
            double h = (double)f * 20.0 * (double)s;
            double rr = ringRadius((double)f, (double)r) * (0.75 + (double)flung * 1.5);
            int count = 3 + l / 3;
            float spin = t * (0.36F - 0.012F * (float)l) + (float)l * 0.9F;

            for (int i = 0; i < count; i++) {
               float a = spin + (float)i * (float) (Math.PI * 2) / (float)count;
               double bx = cx + (double)Mth.cos(a) * rr;
               double bz = cz + (double)Mth.sin(a) * rr;
               double by = cy + h + (double)Mth.sin(t * 0.3F + (float)i + (float)l) * 0.4;
               float size = (0.12F + 0.05F * (float)((i + l) % 3)) * (1.2F - 0.4F * f);
               Quaternionf rot = new Quaternionf().rotateY(-a).rotateX(t * 0.5F + (float)i).rotateZ((float)l * 0.7F);
               SandArt.chunk(ctx, lumps, bx, by, bz, size, size, size, rot, i * 13 + l, light);
            }
         }
      }
   }

   private static void funnel(
      VfxContext ctx, VertexConsumer vc, double cx, double cy, double cz, float r, float s, float turn, float rise, float alpha, int light
   ) {
      if (!(alpha <= 0.01F)) {
         int bands = 12;
         int slices = 24;
         float height = 20.0F * s;

         for (int l = 0; l < bands; l++) {
            float f0 = (float)l / (float)bands;
            float f1 = (float)(l + 1) / (float)bands;
            float r0 = (float)ringRadius((double)f0, (double)r);
            float r1 = (float)ringRadius((double)f1, (double)r);
            int c0 = SandArt.argb(16777215, alpha * fade(f0));
            int c1 = SandArt.argb(16777215, alpha * fade(f1));
            float around0 = (float)Math.max(2, Math.round((float) (Math.PI * 2) * r0 / 2.0F));
            float around1 = (float)Math.max(2, Math.round((float) (Math.PI * 2) * r1 / 2.0F));
            float v0 = f0 * height * 0.5F - rise;
            float v1 = f1 * height * 0.5F - rise;

            for (int i = 0; i < slices; i++) {
               float a0 = (float)i * (float) (Math.PI * 2) / (float)slices;
               float a1 = (float)(i + 1) * (float) (Math.PI * 2) / (float)slices;
               float g0 = (float)i / (float)slices + turn;
               float g1 = (float)(i + 1) / (float)slices + turn;
               SandArt.vertex(
                  ctx,
                  vc,
                  new Vec3(cx + (double)(Mth.cos(a0 + turn * 6.0F) * r0), cy + (double)(f0 * height), cz + (double)(Mth.sin(a0 + turn * 6.0F) * r0)),
                  g0 * around0,
                  v0,
                  c0,
                  light
               );
               SandArt.vertex(
                  ctx,
                  vc,
                  new Vec3(cx + (double)(Mth.cos(a1 + turn * 6.0F) * r0), cy + (double)(f0 * height), cz + (double)(Mth.sin(a1 + turn * 6.0F) * r0)),
                  g1 * around0,
                  v0,
                  c0,
                  light
               );
               SandArt.vertex(
                  ctx,
                  vc,
                  new Vec3(cx + (double)(Mth.cos(a1 + turn * 6.0F) * r1), cy + (double)(f1 * height), cz + (double)(Mth.sin(a1 + turn * 6.0F) * r1)),
                  g1 * around1,
                  v1,
                  c1,
                  light
               );
               SandArt.vertex(
                  ctx,
                  vc,
                  new Vec3(cx + (double)(Mth.cos(a0 + turn * 6.0F) * r1), cy + (double)(f1 * height), cz + (double)(Mth.sin(a0 + turn * 6.0F) * r1)),
                  g0 * around1,
                  v1,
                  c1,
                  light
               );
            }
         }
      }
   }

   private static float fade(float f) {
      return Math.min(1.0F, f * 5.0F + 0.25F) * Math.min(1.0F, (1.0F - f) * 3.5F);
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

   @Override
   public double cullDistance() {
      return 192.0;
   }
}
