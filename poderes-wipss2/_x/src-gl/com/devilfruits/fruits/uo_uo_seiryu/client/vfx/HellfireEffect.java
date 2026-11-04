package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.fx.ClientFxUtil;
import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.VfxRenderTypes;
import com.devilfruits.client.render.vfx.SpinRingsEffect;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.client.render.vfx.VfxManager;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuParticles;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class HellfireEffect extends VfxEffect {
   private static final ResourceLocation LANCE = DevilFruits.id("textures/misc/uo_uo_seiryu/breath_beam.png");
   private final int entityId;
   private final int maxTicks;
   private final float radius;
   private final SpinRingsEffect[] rings = new SpinRingsEffect[3];
   private double x;
   private double y;
   private double z;
   private float height = 3.6F;
   private float lx;
   private float ly = -0.7F;
   private float lz = 0.7F;
   private float lanceLength = 4.0F;
   private boolean firstPerson;

   public HellfireEffect(Entity caster, float radius, int maxTicks, long seed) {
      this.entityId = caster.getId();
      this.radius = radius;
      this.maxTicks = maxTicks;
      this.follow(caster);
      int[][] colors = new int[][]{{16774338, 16742938}, {16765503, 15217182}, {16774338, 16742938}};
      float[] sizes = new float[]{0.7F, 1.0F, 0.85F};

      for (int i = 0; i < this.rings.length; i++) {
         this.rings[i] = VfxManager.add(
            new SpinRingsEffect(caster.position(), radius * sizes[i], 1, maxTicks + 20, seed + (long)i * 7919L)
               .tilt(20.0F + (float)i * 22.0F, 30.0F + (float)i * 22.0F)
               .spin(i % 2 == 0 ? 2.2F : -2.6F)
               .colors(colors[i][0], colors[i][1], 0.17F + (float)i * 0.03F)
               .follow(caster, 0.5F)
         );
      }
   }

   public int entityId() {
      return this.entityId;
   }

   private void follow(Entity e) {
      this.x = e.getX();
      this.y = e.getY();
      this.z = e.getZ();
      this.height = e.getBbHeight();
   }

   public void collapse() {
      for (SpinRingsEffect r : this.rings) {
         r.finish();
      }

      this.finish();
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity e = level.getEntity(this.entityId);
      if (e != null && this.age <= this.maxTicks + 20) {
         this.follow(e);
         this.firstPerson = ClientFxUtil.firstPersonCaster(e);
         double cy = this.y + (double)this.height * 0.5;
         Vec3 look = e.getLookAngle();
         Vec3 aim = new Vec3(look.x, 0.0, look.z).normalize().add(0.0, -1.0, 0.0).normalize();
         double best = 49.0;

         for (Entity t : level.getEntities(e, new AABB(this.x - 6.0, cy - 6.0, this.z - 6.0, this.x + 6.0, cy + 2.0, this.z + 6.0))) {
            if (t instanceof LivingEntity && t.isAlive()) {
               Vec3 to = t.getBoundingBox().getCenter().subtract(this.x, cy, this.z);
               if (to.lengthSqr() < best) {
                  best = to.lengthSqr();
                  aim = to.normalize();
               }
            }
         }

         this.lx = this.lx + ((float)aim.x - this.lx) * 0.4F;
         this.ly = this.ly + ((float)aim.y - this.ly) * 0.4F;
         this.lz = this.lz + ((float)aim.z - this.lz) * 0.4F;
         this.lanceLength = (float)Math.max(2.5, Math.min(6.0, Math.sqrt(best) + 1.5));
         int n = Fx.count(5);

         for (int i = 0; i < n; i++) {
            double a = Fx.random().nextDouble() * Math.PI * 2.0;
            double r = (double)this.radius * (0.7 + Fx.random().nextDouble() * 0.35);
            double px = this.x + Math.cos(a) * r;
            double pz = this.z + Math.sin(a) * r;
            double py = cy + Fx.spread((double)this.radius * 0.6);
            Fx.spawn((ParticleOptions)SeiryuParticles.FLAME.get(), px, py, pz, -Math.sin(a) * 0.12, 0.02, Math.cos(a) * 0.12);
         }

         int m = Fx.count(3);

         for (int i = 0; i < m; i++) {
            double a = Fx.random().nextDouble() * Math.PI * 2.0;
            Fx.spawn(
               (ParticleOptions)SeiryuParticles.EMBER.get(),
               this.x + Math.cos(a) * (double)this.radius * 0.6,
               cy + Fx.spread(1.0),
               this.z + Math.sin(a) * (double)this.radius * 0.6,
               Math.cos(a) * 0.3,
               Fx.spread(0.15) + 0.05,
               Math.sin(a) * 0.3
            );
         }
      } else {
         this.collapse();
      }
   }

   @Override
   public void renderTextured(VfxContext ctx) {
      float in = Math.min(1.0F, ((float)this.age + ctx.partialTick) / 4.0F);
      float near = this.firstPerson ? 0.35F : ctx.proximity(this.x, this.y + (double)this.height * 0.5, this.z, 2.0F, 5.0F, 0.3F);
      float ax = ctx.rx(this.x);
      float ay = ctx.ry(this.y + (double)this.height * 0.5);
      float az = ctx.rz(this.z);
      float len = this.lanceLength * in;
      float bx = ax + this.lx * len;
      float by = ay + this.ly * len;
      float bz = az + this.lz * len;
      float scroll = -((float)this.age + ctx.partialTick) * 0.25F;
      VertexConsumer vc = ctx.buffers.getBuffer(VfxRenderTypes.glowTextured(LANCE));
      SeiryuDraw.texturedBeam(vc, ctx.matrix, ax, ay, az, bx, by, bz, 0.55F, 0.2F, scroll * 0.6F, 1.0F, 16742938, 0.55F * near);
      SeiryuDraw.texturedBeam(vc, ctx.matrix, ax, ay, az, bx, by, bz, 0.3F, 0.08F, scroll, 1.0F, 16765503, 0.85F * near);
      SeiryuDraw.texturedBeam(vc, ctx.matrix, ax, ay, az, bx, by, bz, 0.12F, 0.03F, scroll * 1.6F, 1.0F, 16777215, near);
      float pulse = 0.85F + 0.15F * Mth.sin(ctx.time * 0.9F);
      SeiryuDraw.flare(ctx, ax, ay, az, this.radius * 0.9F * pulse, 16765503, 16742938, 0.5F * near);
      SeiryuDraw.flare(ctx, bx, by, bz, 0.9F, 16774338, 16765503, 0.7F * near * in);
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
