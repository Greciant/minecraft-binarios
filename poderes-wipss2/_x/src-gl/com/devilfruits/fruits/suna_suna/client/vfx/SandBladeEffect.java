package com.devilfruits.fruits.suna_suna.client.vfx;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.client.sound.SoundDesign;
import com.devilfruits.fruits.suna_suna.SunaParticles;
import com.devilfruits.fruits.suna_suna.SunaSounds;
import com.devilfruits.fruits.suna_suna.ability.DesertSpadaAbility;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class SandBladeEffect extends VfxEffect {
   private static final float FIN_LENGTH = 3.0F;
   private static final float FIN_HEIGHT = 3.0F;
   private static final int WAKE = 10;
   private static final int PEEL = 7;
   private static final int LUMPS = 8;
   public final long seed;
   private final Vec3 step;
   private final int steps;
   private final Vec3 dir;
   private final Vec3 side;
   private final float width;
   private final boolean cuts;
   private final int lifetime;
   private double x;
   private double y;
   private double z;
   private double px;
   private double py;
   private double pz;
   private final double startX;
   private final double startZ;
   private int stoppedAt = -1;
   private final Vec3[] path = new Vec3[10];
   private final Vec3[] across = new Vec3[10];
   private final float[] widths = new float[10];
   private final float[] alphas = new float[10];

   public SandBladeEffect(Vec3 start, Vec3 velocity, float power, int lifetime, long seed) {
      this.seed = seed;
      this.steps = DesertSpadaAbility.steps(velocity);
      this.step = velocity.scale(1.0 / (double)this.steps);
      this.dir = velocity.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : velocity.normalize();
      this.side = new Vec3(-this.dir.z, 0.0, this.dir.x);
      this.width = Math.max(0.6F, Math.abs(power));
      this.cuts = power > 0.0F;
      this.lifetime = lifetime;
      this.x = this.px = start.x;
      this.y = this.py = start.y;
      this.z = this.pz = start.z;
      this.startX = start.x;
      this.startZ = start.z;
   }

   public void stop(Vec3 at) {
      if (this.stoppedAt < 0) {
         this.stoppedAt = this.age;
         this.x = at.x;
         this.y = at.y;
         this.z = at.z;
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      this.px = this.x;
      this.py = this.y;
      this.pz = this.z;
      if (this.stoppedAt >= 0) {
         if (this.age - this.stoppedAt > 5) {
            this.finish();
         }
      } else if (this.age > this.lifetime + 10) {
         this.finish();
      } else {
         for (int i = 0; i < this.steps; i++) {
            Vec3 pos = new Vec3(this.x, this.y, this.z);
            Vec3 next = pos.add(this.step);
            double ground = DesertSpadaAbility.next(level, pos, next, this.cuts);
            if (Double.isNaN(ground) || this.cuts && DesertSpadaAbility.resisted(level, next.x, ground, next.z)) {
               this.stop(pos);
               return;
            }

            this.x = next.x;
            this.y = ground;
            this.z = next.z;
         }

         this.spray(level);
      }
   }

   private void spray(ClientLevel level) {
      RandomSource r = Fx.random();
      int n = Fx.count(10);

      for (int i = 0; i < n; i++) {
         double s = r.nextBoolean() ? 1.0 : -1.0;
         double out = 0.14 + r.nextDouble() * 0.2;
         double at = (double)this.width * (0.2 + r.nextDouble() * 0.4);
         Fx.spawn(
            r.nextInt(3) == 0 ? (ParticleOptions)SunaParticles.CLUMP.get() : (ParticleOptions)SunaParticles.GRAIN.get(),
            this.x + this.side.x * s * at - this.dir.x * r.nextDouble(),
            this.y + 0.15 + r.nextDouble() * 1.4,
            this.z + this.side.z * s * at - this.dir.z * r.nextDouble(),
            this.side.x * s * out + this.dir.x * 0.1,
            0.14 + r.nextDouble() * 0.2,
            this.side.z * s * out + this.dir.z * 0.1
         );
      }

      for (int i = 0; i < 2; i++) {
         Fx.spawn(
            (ParticleOptions)SunaParticles.DUST.get(),
            this.px + Fx.spread((double)this.width),
            this.py + 0.2,
            this.pz + Fx.spread((double)this.width),
            0.0,
            0.03,
            0.0
         );
      }

      if (this.age % 3 == 1) {
         SoundDesign.play(new Vec3(this.x, this.y + 0.8, this.z), SunaSounds.SPADA_CUT, 0.9F, 0.85F + r.nextFloat() * 0.2F);
      }
   }

   @Override
   public void renderTextured(VfxContext ctx) {
      float pt = ctx.partialTick;
      float sink = this.stoppedAt < 0 ? 0.0F : Mth.clamp(((float)(this.age - this.stoppedAt) + pt) / 5.0F, 0.0F, 1.0F);
      if (!(sink >= 1.0F)) {
         double wx = Mth.lerp((double)pt, this.px, this.x);
         double wy = Mth.lerp((double)pt, this.py, this.y);
         double wz = Mth.lerp((double)pt, this.pz, this.z);
         float t = (float)this.age + pt;
         float grow = Math.min(1.0F, t / 2.0F) * (1.0F - sink);
         int light = SandArt.light(wx, wy + 0.8, wz);
         Vec3 up = new Vec3(0.0, 1.0, 0.0);
         Vec3 ground = new Vec3(wx, wy, wz);
         float h = 3.0F * grow * (0.95F + 0.05F * Mth.sin(t * 1.3F));
         Vec3 foot = ground.add(0.0, -0.45, 0.0);
         Vec3 front = foot.add(this.dir.scale(0.8));
         Vec3 back = foot.add(this.dir.scale(-2.2));
         VertexConsumer fin = SandArt.solid(ctx, SandArt.FIN);

         for (int k = -1; k <= 1; k += 2) {
            Vec3 o = this.side.scale(0.05 * (double)k);
            SandArt.quad(
               ctx,
               fin,
               back.add(o).add(up.scale((double)h)),
               front.add(o).add(up.scale((double)h)),
               front.add(o),
               back.add(o),
               0.0F,
               0.0F,
               1.0F,
               1.0F,
               -1,
               light
            );
         }

         VertexConsumer stream = SandArt.wispy(ctx, SandArt.STREAM);

         for (int k = -1; k <= 1; k += 2) {
            Vec3 tilt = this.side.scale(0.55 * (double)k).add(0.0, 0.85, 0.0).normalize();

            for (int i = 0; i < 7; i++) {
               float f = (float)i / 6.0F;
               Vec3 base = ground.add(this.dir.scale(0.6 - (double)f * 3.2)).add(this.side.scale((double)k * ((double)this.width * 0.35 + (double)f * 0.6)));
               float lift = (0.75F - 0.25F * f) * grow;
               this.path[i] = base.add(tilt.scale((double)lift)).add(0.0, (double)(Mth.sin(t * 0.8F + f * 5.0F + (float)k) * 0.05F), 0.0);
               this.across[i] = tilt;
               this.widths[i] = lift;
               this.alphas[i] = Math.min(1.0F, f * 5.0F + 0.2F) * (1.0F - f) * 0.9F;
            }

            SandArt.ribbon(ctx, stream, this.path, this.across, this.widths, this.alphas, 7, 0.5F, -t * 0.35F + (float)k, 16777215, light);
         }

         double run = Math.sqrt(Mth.square(wx - this.startX) + Mth.square(wz - this.startZ));
         double length = Math.min(5.5, run + 0.4);
         if (length > 0.3) {
            for (int i = 0; i < 10; i++) {
               float f = (float)i / 9.0F;
               this.path[i] = ground.add(0.0, 0.05, 0.0).subtract(this.dir.scale((double)f * length - 0.4));
               this.across[i] = this.side;
               this.widths[i] = this.width * (0.35F + 0.4F * f);
               this.alphas[i] = (1.0F - f) * 0.85F * (1.0F - sink);
            }

            SandArt.ribbon(ctx, stream, this.path, this.across, this.widths, this.alphas, 10, 0.5F, -t * 0.25F, 16777215, light);
         }

         VertexConsumer lumps = SandArt.solid(ctx, SandArt.PACKED);

         for (int i = 0; i < 8; i++) {
            float phase = (t * 0.18F + (float)i / 8.0F) % 1.0F;
            double s = (double)(i % 2 == 0 ? 1 : -1) * ((double)this.width * 0.3 + (double)phase * (0.8 + (double)this.width * 0.4));
            double lift = 0.2 + Math.sin((double)phase * Math.PI) * (0.9 + 0.15 * (double)(i % 3));
            float size = (0.12F + 0.03F * (float)(i % 3)) * (1.0F - phase * 0.6F) * grow;
            Quaternionf rot = new Quaternionf().rotateY(t * 0.4F + (float)i).rotateX(t * 0.6F + (float)(i * 2));
            Vec3 at = ground.add(this.dir.scale(0.3 - (double)phase * 1.6)).add(this.side.scale(s));
            SandArt.chunk(ctx, lumps, at.x, at.y + lift, at.z, size, size, size, rot, i + (int)this.seed, light);
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
