package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.fx.ClientFxUtil;
import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.CameraEffects;
import com.devilfruits.client.render.VfxRenderTypes;
import com.devilfruits.client.render.vfx.ShockwaveEffect;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.client.render.vfx.VfxManager;
import com.devilfruits.client.sound.SoundDesign;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuParticles;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuSounds;
import com.devilfruits.registry.DFSounds;
import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class VeilCastEffect extends VfxEffect {
   public static final ResourceLocation ORB = DevilFruits.id("textures/misc/uo_uo_seiryu/gpo/black_orb.png");
   private static final int GATHER = 8;
   private static final int COCOON = 16;
   private static final int BURST = 4;
   private static final int WISPS = 4;
   private static final int WISP_DEPTH = 3;
   private final int entityId;
   private final long seed;
   private double x;
   private double y;
   private double z;
   private float height = 3.6F;
   private final float[][] wisps = new float[4][SeiryuDraw.pointCount(3) * 3];
   private final float[] wispAngle = new float[4];
   private final Vector3f c = new Vector3f();
   private final float[] scratchPath = new float[SeiryuDraw.pointCount(3) * 3];

   public VeilCastEffect(Entity entity, long seed) {
      this.entityId = entity.getId();
      this.seed = seed;
      this.follow(entity);
   }

   private void follow(Entity e) {
      this.x = e.getX();
      this.y = e.getY();
      this.z = e.getZ();
      this.height = e.getBbHeight();
   }

   private float radius() {
      return 0.52F * this.height;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity e = level.getEntity(this.entityId);
      if (e != null) {
         this.follow(e);
      }

      if (this.age > 28) {
         this.finish();
      } else {
         float r = this.radius();
         if (this.age == 1) {
            VfxManager.add(
               new ShockwaveEffect(ShockwaveEffect.Mode.GROUND, new Vec3(this.x, this.y + 0.08, this.z), Vec3.ZERO, r * 2.2F, 0.0F, 12, 1, 0.9F)
                  .tint(243, 232, 255)
                  .shell(255, 255, 255)
            );
            SoundDesign.play(
               new Vec3(this.x, this.y + 1.0, this.z),
               SeiryuSounds.VEIL_CAST,
               1.4F,
               0.9F,
               SoundDesign.layer(DFSounds.LAYER_BOOM, 0.9F, 0.55F),
               SoundDesign.layer(DFSounds.LAYER_THUNDER, 0.3F, 1.5F)
            );
         }

         if (this.age <= 8) {
            int n = Fx.count(5);

            for (int i = 0; i < n; i++) {
               double a = Fx.random().nextDouble() * Math.PI * 2.0;
               double d = (double)r * (0.9 + Fx.random().nextDouble() * 0.6);
               double px = this.x + Math.cos(a) * d;
               double pz = this.z + Math.sin(a) * d;
               double py = this.y + 0.1 + Fx.random().nextDouble() * 0.5;
               double k = 0.16 + 0.04 * (double)this.age;
               Fx.spawn((ParticleOptions)SeiryuParticles.VIOLET_SPARK.get(), px, py, pz, (this.x - px) * k, 0.02, (this.z - pz) * k);
            }
         }

         if (this.age == 9 && e != null && ClientFxUtil.firstPersonCaster(e)) {
            CameraEffects.flash(0.75F, 788504);
         }

         if (this.age > 8 && this.age <= 24 && this.age % 2 == 0) {
            this.regenerateWisps(this.age);
         }

         if (this.age == 24) {
            this.burst(r);
         }
      }
   }

   private void regenerateWisps(int frame) {
      for (int i = 0; i < 4; i++) {
         long s = SeiryuDraw.mix(this.seed + (long)frame * 131L + (long)i * 977L);
         this.wispAngle[i] = (float)i * (float) (Math.PI * 2) / 4.0F + SeiryuDraw.unit(s) * 1.3F + (float)frame * 0.21F;
         float len = 0.45F + SeiryuDraw.unit(s >>> 24) * 0.5F;
         float a0 = this.wispAngle[i];
         float a1 = a0 + len;
         SeiryuDraw.boltPath(this.wisps[i], Mth.cos(a1) - Mth.cos(a0), Mth.sin(a1) - Mth.sin(a0), 0.0F, 3, 0.22F, s);
      }
   }

   private void burst(float r) {
      Vec3 mid = new Vec3(this.x, this.y + (double)this.height * 0.5, this.z);
      VfxManager.add(new ImpactFlareEffect(mid, r * 2.2F, 7, 15984895, 11955455));
      VfxManager.add(
         new ShockwaveEffect(ShockwaveEffect.Mode.GROUND, new Vec3(this.x, this.y + 0.1, this.z), Vec3.ZERO, r * 1.8F, 0.0F, 9, 2, 0.8F)
            .tint(182, 108, 255)
            .shell(243, 232, 255)
      );

      for (int i = 0; i < 5; i++) {
         float a = (float)i * (float) (Math.PI * 2) / 5.0F + (float)(this.seed & 15L) * 0.4F;
         Vec3 from = mid.add((double)(Mth.cos(a) * r) * 0.6, Fx.spread((double)r * 0.6), (double)(Mth.sin(a) * r) * 0.6);
         VfxManager.add(
            new LightningEffect(
                  from, from.add((double)(Mth.cos(a) * r) * 1.2, Fx.spread((double)r), (double)(Mth.sin(a) * r) * 1.2), 6, 0.05F, 1, this.seed + (long)i
               )
               .quiet()
         );
      }

      int n = Fx.count(36);

      for (int i = 0; i < n; i++) {
         Fx.spawn(
            (ParticleOptions)SeiryuParticles.VIOLET_SPARK.get(),
            mid.x + Fx.spread((double)r),
            mid.y + Fx.spread((double)r),
            mid.z + Fx.spread((double)r),
            Fx.spread(0.6),
            Fx.spread(0.6),
            Fx.spread(0.6)
         );
      }

      for (int i = 0; i < Fx.count(10); i++) {
         Fx.spawn(
            ParticleTypes.LARGE_SMOKE,
            mid.x + Fx.spread((double)r),
            mid.y + Fx.spread((double)r),
            mid.z + Fx.spread((double)r),
            Fx.spread(0.12),
            0.04,
            Fx.spread(0.12)
         );
      }

      SoundDesign.play(mid, SeiryuSounds.VEIL_BURST, 1.3F, 1.0F, SoundDesign.layer(DFSounds.LAYER_THUNDER, 0.7F, 1.3F));
      CameraEffects.shakeAt(mid, 0.25F, 20.0);
   }

   @Override
   public void renderTextured(VfxContext ctx) {
      float t = (float)this.age + ctx.partialTick - 8.0F;
      if (!(t <= 0.0F) && !(t > 20.0F)) {
         float r = this.radius() * (t < 2.0F ? 0.4F + 0.6F * t / 2.0F : (t > 16.0F ? 1.0F + 0.15F * (t - 16.0F) / 4.0F : 1.0F));
         float alpha = t > 16.0F ? 1.0F - (t - 16.0F) / 4.0F : 1.0F;
         this.c.set(ctx.rx(this.x), ctx.ry(this.y + (double)this.height * 0.5), ctx.rz(this.z));
         float d = this.c.length();
         if (d > r * 1.1F) {
            float pulled = d - r * 0.95F;
            this.c.mul(pulled / d);
            float half = r * pulled / d;
            VertexConsumer vc = ctx.buffers.getBuffer(VfxRenderTypes.shadeTextured(ORB));
            quad(vc, ctx, this.c, half, 0.0F, 1.0F, (int)(alpha * 255.0F));
            if (t <= 16.0F) {
               VertexConsumer glow = ctx.buffers.getBuffer(VfxRenderTypes.GLOW);
               ctx.buffer = glow;
               Vector3f l = ctx.camLeft;
               Vector3f u = ctx.camUp;
               int pts = SeiryuDraw.pointCount(3);

               for (int i = 0; i < 4; i++) {
                  float a = this.wispAngle[i];
                  float rim = half * 0.86F;
                  float ox = this.c.x + (l.x * Mth.cos(a) + u.x * Mth.sin(a)) * rim;
                  float oy = this.c.y + (l.y * Mth.cos(a) + u.y * Mth.sin(a)) * rim;
                  float oz = this.c.z + (l.z * Mth.cos(a) + u.z * Mth.sin(a)) * rim;
                  toScreenPlane(this.wisps[i], pts, l, u, rim, this.scratchPath);
                  SeiryuDraw.ribbons(ctx, this.scratchPath, pts, ox, oy, oz, 0.025F * this.height / 3.6F + 0.01F, 0.5F, 15984895, 11955455, 8011007, 0.9F);
               }

               ctx.buffer = null;
            }
         }
      }
   }

   private static void toScreenPlane(float[] src, int count, Vector3f l, Vector3f u, float scale, float[] out) {
      for (int i = 0; i < count; i++) {
         float px = src[i * 3] * scale;
         float py = src[i * 3 + 1] * scale;
         out[i * 3] = l.x * px + u.x * py;
         out[i * 3 + 1] = l.y * px + u.y * py;
         out[i * 3 + 2] = l.z * px + u.z * py;
      }
   }

   private static void quad(VertexConsumer vc, VfxContext ctx, Vector3f c, float s, float v0, float v1, int alpha) {
      Vector3f l = ctx.camLeft;
      Vector3f u = ctx.camUp;
      Matrix4f m = ctx.matrix;
      vc.addVertex(m, c.x + (l.x + u.x) * s, c.y + (l.y + u.y) * s, c.z + (l.z + u.z) * s).setUv(0.0F, v0).setColor(255, 255, 255, alpha);
      vc.addVertex(m, c.x + (l.x - u.x) * s, c.y + (l.y - u.y) * s, c.z + (l.z - u.z) * s).setUv(0.0F, v1).setColor(255, 255, 255, alpha);
      vc.addVertex(m, c.x + (-l.x - u.x) * s, c.y + (-l.y - u.y) * s, c.z + (-l.z - u.z) * s).setUv(1.0F, v1).setColor(255, 255, 255, alpha);
      vc.addVertex(m, c.x + (-l.x + u.x) * s, c.y + (-l.y + u.y) * s, c.z + (-l.z + u.z) * s).setUv(1.0F, v0).setColor(255, 255, 255, alpha);
   }

   @Nullable
   public Entity entity(ClientLevel level) {
      return level.getEntity(this.entityId);
   }

   @Override
   public double x() {
      return this.x;
   }

   @Override
   public double y() {
      return this.y + (double)this.height * 0.5;
   }

   @Override
   public double z() {
      return this.z;
   }
}
