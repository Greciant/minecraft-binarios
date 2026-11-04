package com.devilfruits.fruits.suna_suna.client.vfx;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.client.sound.LoopSound;
import com.devilfruits.fruits.suna_suna.SunaParticles;
import com.devilfruits.fruits.suna_suna.SunaSounds;
import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class SandGripEffect extends VfxEffect {
   private static final int RIBBONS = 4;
   private static final int STEPS = 22;
   private static final int ARM = 8;
   public final long seed;
   private final Entity caster;
   private final int victimId;
   private final int maxAge;
   private int releasedAt = -1;
   private double x;
   private double y;
   private double z;
   private final Vec3[] path = new Vec3[22];
   private final Vec3[] across = new Vec3[22];
   private final float[] widths = new float[22];
   private final float[] alphas = new float[22];

   public SandGripEffect(Entity caster, int victimId, int maxTicks, long seed) {
      this.caster = caster;
      this.victimId = victimId;
      this.maxAge = maxTicks + 20;
      this.seed = seed;
      this.x = caster.getX();
      this.y = caster.getY();
      this.z = caster.getZ();
      Minecraft.getInstance()
         .getSoundManager()
         .play(new LoopSound(SunaSounds.ENCIERRO_DRAIN.get(), caster, caster.position(), 0.8F, 0.9F, 0, () -> !this.isDone() && this.releasedAt < 0));
   }

   public void release() {
      if (this.releasedAt < 0) {
         this.releasedAt = this.age;
      }
   }

   @Nullable
   private Entity victim(ClientLevel level) {
      Entity e = level.getEntity(this.victimId);
      return e != null && e.isAlive() ? e : null;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity victim = this.victim(level);
      if (!this.caster.isRemoved() && this.age <= this.maxAge && (this.releasedAt < 0 || this.age - this.releasedAt <= 6)) {
         if (victim == null) {
            this.release();
         } else {
            this.x = victim.getX();
            this.y = victim.getY();
            this.z = victim.getZ();
            if (this.releasedAt < 0) {
               RandomSource r = Fx.random();
               Vec3 hand = SandGatherEffect.hand(this.caster, 1.0F);
               Vec3 body = victim.getBoundingBox().getCenter();
               int n = Fx.count(4);

               for (int i = 0; i < n; i++) {
                  double t = r.nextDouble();
                  Vec3 p = body.lerp(hand, t);
                  Vec3 v = hand.subtract(body).scale(0.12);
                  Fx.spawn(
                     (ParticleOptions)SunaParticles.GRAIN.get(), p.x + Fx.spread(0.12), p.y + Fx.spread(0.12), p.z + Fx.spread(0.12), v.x, v.y + 0.01, v.z
                  );
               }

               int flakes = Fx.count(1.2F);

               for (int i = 0; i < flakes; i++) {
                  Fx.spawn(
                     (ParticleOptions)SunaParticles.DRY_FLAKE.get(),
                     victim.getX() + Fx.spread((double)victim.getBbWidth() * 0.6),
                     victim.getY() + r.nextDouble() * (double)victim.getBbHeight(),
                     victim.getZ() + Fx.spread((double)victim.getBbWidth() * 0.6),
                     Fx.spread(0.03),
                     0.02,
                     Fx.spread(0.03)
                  );
               }
            }
         }
      } else {
         this.finish();
      }
   }

   @Override
   public void renderTextured(VfxContext ctx) {
      ClientLevel level = Minecraft.getInstance().level;
      Entity victim = level == null ? null : this.victim(level);
      if (victim != null) {
         float pt = ctx.partialTick;
         float t = (float)this.age + pt;
         float close = this.releasedAt < 0 ? Math.min(1.0F, t / 6.0F) : Math.max(0.0F, 1.0F - ((float)(this.age - this.releasedAt) + pt) / 6.0F);
         if (!(close <= 0.02F)) {
            double vx = Mth.lerp((double)pt, victim.xo, victim.getX());
            double vy = Mth.lerp((double)pt, victim.yo, victim.getY());
            double vz = Mth.lerp((double)pt, victim.zo, victim.getZ());
            float w = victim.getBbWidth() * 0.5F + 0.3F;
            float h = victim.getBbHeight();
            int light = SandArt.light(vx, vy + (double)h * 0.5, vz);
            Vec3 up = new Vec3(0.0, 1.0, 0.0);
            VertexConsumer stream = SandArt.wispy(ctx, SandArt.STREAM);

            for (int k = 0; k < 4; k++) {
               float offset = (float)k * (float) (Math.PI * 2) / 4.0F;
               float spin = t * 0.18F * (float)(k % 2 == 0 ? 1 : -1);

               for (int i = 0; i < 22; i++) {
                  float f = (float)i / 21.0F;
                  float a = offset + spin + f * (float) (Math.PI * 2) * 1.25F;
                  float r = w * (1.7F - 0.7F * close) * (0.9F + 0.15F * Mth.sin(f * 9.0F + t * 0.3F + (float)k));
                  this.path[i] = new Vec3(vx + (double)(Mth.cos(a) * r), vy + 0.05 + (double)(f * h) * 0.98, vz + (double)(Mth.sin(a) * r));
                  this.across[i] = up;
                  this.widths[i] = 0.3F * close;
                  this.alphas[i] = Math.min(1.0F, Math.min(f, 1.0F - f) * 6.0F) * close;
               }

               SandArt.ribbon(ctx, stream, this.path, this.across, this.widths, this.alphas, 22, 0.7F, -t * 0.35F + (float)k, 16777215, light);
            }

            Vec3 hand = SandGatherEffect.hand(this.caster, pt);
            Vec3 body = new Vec3(vx, vy + (double)h * 0.55, vz);
            Vec3 along = hand.subtract(body);
            Vec3 look = new Vec3((double)ctx.camLook.x(), (double)ctx.camLook.y(), (double)ctx.camLook.z());
            Vec3 flat = along.cross(look);
            flat = flat.lengthSqr() < 1.0E-6 ? up : flat.normalize();

            for (int i = 0; i < 8; i++) {
               float f = (float)i / 7.0F;
               this.path[i] = body.add(along.scale((double)f)).add(0.0, (double)(Mth.sin(f * (float) Math.PI) * 0.12F), 0.0);
               this.across[i] = flat;
               this.widths[i] = 0.1F + 0.05F * Mth.sin(f * (float) Math.PI);
               this.alphas[i] = 0.75F * close * Math.min(1.0F, Math.min(f, 1.0F - f) * 5.0F + 0.2F);
            }

            SandArt.ribbon(ctx, stream, this.path, this.across, this.widths, this.alphas, 8, 0.8F, t * 0.6F, 16777215, light);
            VertexConsumer lumps = SandArt.solid(ctx, SandArt.PACKED);

            for (int i = 0; i < 6; i++) {
               float a = t * 0.2F + (float)i * (float) (Math.PI * 2) / 6.0F;
               float ry = h * (0.2F + 0.12F * (float)i);
               float r = w * (1.6F - 0.6F * close);
               float size = (0.07F + 0.03F * (float)(i % 3)) * close;
               Quaternionf rot = new Quaternionf().rotateY(-a).rotateX(t * 0.3F + (float)i);
               SandArt.chunk(ctx, lumps, vx + (double)(Mth.cos(a) * r), vy + (double)ry, vz + (double)(Mth.sin(a) * r), size, size, size, rot, i * 3, light);
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
