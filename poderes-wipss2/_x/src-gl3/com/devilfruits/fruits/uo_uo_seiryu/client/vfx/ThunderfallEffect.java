package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.fx.ClientFxUtil;
import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.CameraEffects;
import com.devilfruits.client.render.vfx.FloorDebrisEffect;
import com.devilfruits.client.render.vfx.GroundDecalEffect;
import com.devilfruits.client.render.vfx.SpinRingsEffect;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.client.render.vfx.VfxManager;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class ThunderfallEffect extends VfxEffect {
   public static final ResourceLocation CRATER = DevilFruits.id("textures/misc/uo_uo_seiryu/gpo/scorch_ring.png");
   private final int entityId;
   private final int maxTicks;
   private double x;
   private double y;
   private double z;
   private float height = 3.6F;
   private int strikes;
   private final FloorDebrisEffect debris;
   private final GroundDecalEffect crater;
   private final SpinRingsEffect wind;
   private boolean ended;

   public ThunderfallEffect(ClientLevel level, Entity caster, float range, int maxTicks, long seed) {
      this.entityId = caster.getId();
      this.maxTicks = maxTicks;
      this.x = caster.getX();
      this.y = caster.getY();
      this.z = caster.getZ();
      this.height = caster.getBbHeight();
      Vec3 feet = caster.position();
      this.debris = VfxManager.add(new FloorDebrisEffect(level, feet, 18, 0.12F, 0.35F, 90, seed).orbit(range * 0.42F, 0.32F));
      this.crater = VfxManager.add(new GroundDecalEffect(feet, CRATER, 4.0F, (float)(seed & 31L) * 0.2F, maxTicks + 220).growIn(0.3F, 6));
      this.wind = VfxManager.add(
         new SpinRingsEffect(feet.add(0.0, (double)this.height * 0.35, 0.0), range * 0.45F, 3, maxTicks + 40, seed ^ 81L)
            .tilt(4.0F, 16.0F)
            .spin(1.6F)
            .colors(16777215, 13158616, 0.035F)
      );
   }

   public int entityId() {
      return this.entityId;
   }

   public void strike(int number) {
      this.strikes = number;
      this.crater.growTo(1.0F + Math.min(2.2F, (float)number * 0.12F));
   }

   public void end() {
      if (!this.ended) {
         this.ended = true;
         this.debris.drop();
         this.wind.finish();
         this.finish();
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity e = level.getEntity(this.entityId);
      if (e != null && this.age <= this.maxTicks + 60) {
         this.x = e.getX();
         this.y = e.getY();
         this.z = e.getZ();
         this.height = e.getBbHeight();
         int n = Fx.count(2.5F + (float)this.strikes * 0.25F);

         for (int i = 0; i < n; i++) {
            double a = Fx.random().nextDouble() * Math.PI * 2.0;
            double d = (double)this.height * (0.2 + Fx.random().nextDouble() * 0.5);
            Fx.spawn(
               (ParticleOptions)SeiryuParticles.VIOLET_SPARK.get(),
               this.x + Math.cos(a) * d,
               this.y + Fx.random().nextDouble() * (double)this.height,
               this.z + Math.sin(a) * d,
               Fx.spread(0.05),
               0.08 + Fx.random().nextDouble() * 0.1,
               Fx.spread(0.05)
            );
         }

         if (ClientFxUtil.isLocal(e)) {
            CameraEffects.sustain(0.06F + 0.012F * (float)Math.min(this.strikes, 18), 3);
         }
      } else {
         this.end();
      }
   }

   @Override
   public void renderTextured(VfxContext ctx) {
      float pulse = 0.75F + 0.25F * Mth.sin(ctx.time * 0.6F);
      float near = ctx.proximity(this.x, this.y + (double)this.height * 0.6, this.z, 2.0F, 6.0F, 0.3F);
      SeiryuDraw.flare(
         ctx, ctx.rx(this.x), ctx.ry(this.y + (double)this.height * 0.95), ctx.rz(this.z), this.height * 0.45F * pulse, 11955455, 8011007, 0.45F * near
      );
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
