package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.fx.ClientFxUtil;
import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.VfxRenderTypes;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.fruits.uo_uo_seiryu.DragonHead;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuParticles;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class BreathEffect extends VfxEffect {
   private static final ResourceLocation BEAM = DevilFruits.id("textures/misc/uo_uo_seiryu/breath_beam.png");
   private final int casterId;
   private final int duration;
   private final boolean beam;
   private final boolean dragon;
   private final float maxLength;
   private final float width;
   private final long seed;
   private double ox;
   private double oy;
   private double oz;
   private float dx;
   private float dy;
   private float dz;
   private float length;
   private float prevLength;

   public BreathEffect(@Nullable Entity caster, Vec3 mouth, Vec3 dir, float length, int duration, long seed) {
      boolean var10001;
      label34: {
         super();
         this.casterId = caster == null ? -1 : caster.getId();
         this.duration = duration;
         this.beam = duration > 0;
         if (caster instanceof Player p && UoUoSeiryu.isIn(p, "dragon")) {
            var10001 = true;
            break label34;
         }

         var10001 = false;
      }

      this.dragon = var10001;
      this.maxLength = length;
      this.width = this.beam ? (this.dragon ? 1.5F : 0.7F) : 0.45F + length * 0.03F;
      this.seed = seed;
      this.ox = mouth.x;
      this.oy = mouth.y;
      this.oz = mouth.z;
      Vec3 d = dir.normalize();
      this.dx = (float)d.x;
      this.dy = (float)d.y;
      this.dz = (float)d.z;
      this.length = this.beam ? 0.0F : length;
      this.prevLength = this.length;
   }

   public static Vec3 mouth(Entity e, float partialTick) {
      if (e instanceof Player p && UoUoSeiryu.isIn(p, "dragon")) {
         return DragonHead.mouth(e, partialTick);
      }

      double ahead = 0.45 * (double)(e instanceof Player p ? p.getScale() : 1.0F);
      Vec3 look = e.getViewVector(partialTick);
      float scale = e instanceof Player px ? px.getScale() : 1.0F;
      return e.getEyePosition(partialTick).add(look.scale(ahead)).add(0.0, -0.12 * (double)scale, 0.0);
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      int life = this.beam ? this.duration + 6 : 12;
      if (this.age > life) {
         this.finish();
      } else {
         this.prevLength = this.length;
         if (this.beam) {
            Entity caster = level.getEntity(this.casterId);
            if (caster == null || !caster.isAlive()) {
               this.finish();
               return;
            }

            Vec3 m = mouth(caster, 1.0F);
            Vec3 look = DragonHead.aim(level, caster, m, (double)this.maxLength, 1.0F);
            this.ox = m.x;
            this.oy = m.y;
            this.oz = m.z;
            this.dx = (float)look.x;
            this.dy = (float)look.y;
            this.dz = (float)look.z;
            if (this.age <= this.duration) {
               Vec3 to = m.add(look.scale((double)this.maxLength));
               BlockHitResult hit = PhysicsWorld.clip(level, new ClipContext(m, to, Block.COLLIDER, Fluid.ANY, caster));
               float target = hit.getType() == Type.MISS ? this.maxLength : (float)m.distanceTo(hit.getLocation());
               this.length = Math.min(target, this.length + this.maxLength / 3.0F);
               if (hit.getType() != Type.MISS && this.length >= target - 0.01F) {
                  this.impactParticles(level, hit.getLocation());
               }
            } else {
               this.length = Math.max(0.0F, this.length - this.maxLength / 5.0F);
            }
         }

         int n = Fx.count(this.beam ? (this.dragon ? 10 : 6) : 8);
         float front = this.beam ? this.length : this.maxLength * Math.min(1.0F, (float)this.age / 4.0F);
         float skip = this.firstPerson(level) ? Math.min(front, 2.5F) : 0.0F;

         for (int i = 0; i < n; i++) {
            float t = skip + Fx.random().nextFloat() * (front - skip);
            double spread = (double)this.width * (this.beam ? 0.8 : 0.35 + 0.65 * (double)t / (double)Math.max(1.0F, this.maxLength));
            Fx.spawn(
               i % 3 == 0 ? (ParticleOptions)SeiryuParticles.EMBER.get() : (ParticleOptions)SeiryuParticles.FLAME.get(),
               this.ox + (double)(this.dx * t) + Fx.spread(spread),
               this.oy + (double)(this.dy * t) + Fx.spread(spread),
               this.oz + (double)(this.dz * t) + Fx.spread(spread),
               (double)this.dx * 0.08 + Fx.spread(0.04),
               (double)this.dy * 0.08 + 0.03,
               (double)this.dz * 0.08 + Fx.spread(0.04)
            );
         }
      }
   }

   private boolean firstPerson(ClientLevel level) {
      return ClientFxUtil.firstPersonCaster(level.getEntity(this.casterId));
   }

   private void impactParticles(ClientLevel level, Vec3 at) {
      if (!level.getFluidState(BlockPos.containing(at.x, at.y - 0.05, at.z)).isEmpty()) {
         for (int i = 0; i < Fx.count(this.dragon ? 5 : 2); i++) {
            Fx.spawn(
               (ParticleOptions)SeiryuParticles.CLOUD.get(),
               at.x + Fx.spread((double)this.width),
               at.y + 0.2,
               at.z + Fx.spread((double)this.width),
               Fx.spread(0.05),
               0.12,
               Fx.spread(0.05)
            );
         }
      } else {
         int n = Fx.count(this.dragon ? 6 : 3);

         for (int i = 0; i < n; i++) {
            Fx.spawn(
               (ParticleOptions)SeiryuParticles.FLAME.get(), at.x + Fx.spread(0.6), at.y + 0.1, at.z + Fx.spread(0.6), Fx.spread(0.12), 0.08, Fx.spread(0.12)
            );
         }

         if (Fx.random().nextFloat() < 0.4F) {
            Fx.spawn(ParticleTypes.LARGE_SMOKE, at.x + Fx.spread(0.5), at.y + 0.2, at.z + Fx.spread(0.5), 0.0, 0.05, 0.0);
         }
      }
   }

   private float alpha(float partialTick) {
      float t = (float)this.age + partialTick;
      if (this.beam) {
         return t < (float)this.duration ? 1.0F : Math.max(0.0F, 1.0F - (t - (float)this.duration) / 6.0F);
      } else {
         return t < 5.0F ? 1.0F : Math.max(0.0F, 1.0F - (t - 5.0F) / 7.0F);
      }
   }

   private float front(float partialTick) {
      return this.beam ? Mth.lerp(partialTick, this.prevLength, this.length) : this.maxLength * Math.min(1.0F, ((float)this.age + partialTick) / 4.0F);
   }

   @Override
   public void renderTextured(VfxContext ctx) {
      float alpha = this.alpha(ctx.partialTick);
      float len = this.front(ctx.partialTick);
      if (!(alpha <= 0.01F) && !(len < 0.05F)) {
         float ax = ctx.rx(this.ox);
         float ay = ctx.ry(this.oy);
         float az = ctx.rz(this.oz);
         float bx = ax + this.dx * len;
         float by = ay + this.dy * len;
         float bz = az + this.dz * len;
         float scroll = -(ctx.time * (this.beam ? 0.35F : 0.25F)) % 1.0F;
         float repeat = len / (this.width * 4.0F);
         float pulse = this.dragon ? 1.0F + 0.25F * Mth.sin(ctx.time * 0.9F) : 1.0F;
         float swell = this.dragon ? 1.0F + 0.7F * Mth.clamp(((float)this.age + ctx.partialTick) / 20.0F, 0.0F, 1.0F) : 1.0F;
         float startW = (this.beam ? this.width * 0.45F : 0.12F) * swell;
         float endW = (this.beam ? this.width * pulse : this.width) * swell;
         float near = ctx.proximity(this.ox, this.oy, this.oz, 1.2F, 4.0F, 0.15F);
         VertexConsumer vc = ctx.buffers.getBuffer(VfxRenderTypes.glowTextured(BEAM));
         if (this.dragon) {
            SeiryuDraw.texturedBeam(
               vc, ctx.matrix, ax, ay, az, bx, by, bz, startW * 2.6F, endW * 2.8F, scroll * 0.6F, repeat * 0.5F, 14041882, alpha * 0.35F * near
            );
         }

         SeiryuDraw.texturedBeam(vc, ctx.matrix, ax, ay, az, bx, by, bz, startW * 1.6F, endW * 1.6F, scroll, repeat, 16747066, alpha * 0.7F * near);
         SeiryuDraw.texturedBeam(vc, ctx.matrix, ax, ay, az, bx, by, bz, startW * 0.6F, endW * 0.55F, scroll * 1.7F, repeat, 16777215, alpha * near);
         SeiryuDraw.flare(ctx, ax, ay, az, this.width * (this.beam ? 1.2F : 0.8F), 16776160, 16758574, alpha * near);
         if (len > 0.5F) {
            float flicker = 0.85F + 0.15F * Mth.sin(ctx.time * 3.1F + (float)this.seed);
            float far = ctx.proximity(this.ox + (double)(this.dx * len), this.oy + (double)(this.dy * len), this.oz + (double)(this.dz * len), 1.5F, 5.0F, 0.2F);
            SeiryuDraw.flare(ctx, bx, by, bz, this.width * (this.beam ? 2.2F : 1.4F) * flicker, 16776160, 14041882, alpha * 0.9F * far);
         }
      }
   }

   @Override
   public double x() {
      return this.ox + (double)(this.dx * this.length) * 0.5;
   }

   @Override
   public double y() {
      return this.oy + (double)(this.dy * this.length) * 0.5;
   }

   @Override
   public double z() {
      return this.oz + (double)(this.dz * this.length) * 0.5;
   }

   @Override
   public double cullDistance() {
      return 160.0;
   }
}
