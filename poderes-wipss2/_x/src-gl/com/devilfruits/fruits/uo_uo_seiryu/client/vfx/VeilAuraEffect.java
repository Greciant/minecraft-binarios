package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.client.fx.ClientFxUtil;
import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuParticles;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import com.devilfruits.fruits.uo_uo_seiryu.ability.ThundercladVeilAbility;
import com.devilfruits.fruits.uo_uo_seiryu.client.render.HybridAnchors;
import java.util.Arrays;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3d;

public class VeilAuraEffect extends VfxEffect {
   private static final int WISPS = 3;
   private static final int DEPTH = 3;
   private final int entityId;
   private final float[][] paths = new float[3][SeiryuDraw.pointCount(3) * 3];
   private final double[] origins = new double[9];
   private final int[] born = new int[3];
   private final Vector3d anchor = new Vector3d();
   private double x;
   private double y;
   private double z;
   private float height = 3.6F;
   private boolean firstPerson;
   private int seed;

   public VeilAuraEffect(Entity entity) {
      this.entityId = entity.getId();
      this.x = entity.getX();
      this.y = entity.getY();
      this.z = entity.getZ();
      this.seed = entity.getId() * 7919;
      Arrays.fill(this.born, -100);
   }

   public int entityId() {
      return this.entityId;
   }

   public static boolean veiled(Entity e) {
      if (e instanceof Player p && e.isAlive() && UoUoSeiryu.isIn(p, "hybrid") && FormFlags.has(p, ThundercladVeilAbility.FLAG)) {
         return true;
      }

      return false;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity e = level.getEntity(this.entityId);
      if (e != null && veiled(e) && VfxSettings.legacy()) {
         this.x = e.getX();
         this.y = e.getY();
         this.z = e.getZ();
         this.height = e.getBbHeight();
         this.firstPerson = ClientFxUtil.firstPersonCaster(e);
         int n = Fx.count(1.4F);

         for (int i = 0; i < n; i++) {
            double a = Fx.random().nextDouble() * Math.PI * 2.0;
            double d = (double)this.height * (0.18 + Fx.random().nextDouble() * 0.16);
            Fx.spawn(
               (ParticleOptions)SeiryuParticles.VIOLET_SPARK.get(),
               this.x + Math.cos(a) * d,
               this.y + 0.05 + Fx.random().nextDouble() * 0.3,
               this.z + Math.sin(a) * d,
               Fx.spread(0.04),
               0.03 + Fx.random().nextDouble() * 0.05,
               Fx.spread(0.04)
            );
         }

         if (!this.firstPerson) {
            if (this.age % 2 == 0) {
               int slot = this.age / 2 % 3;
               this.seed = this.seed * 1103515245 + 12345;
               int which = (this.seed >>> 16) % 3;
               HybridAnchors.get(e, which == 0 ? 3 : 4, 1.0F, this.anchor);
               float yaw = (e instanceof Player p ? p.yBodyRot : e.getYRot()) * (float) (Math.PI / 180.0);
               double side = which == 0 ? 0.0 : (which == 1 ? 0.22 : -0.22) * (double)this.height;
               double up = which == 0 ? -0.05 * (double)this.height : 0.08 * (double)this.height;
               this.origins[slot * 3] = this.anchor.x - (double)Mth.cos(yaw) * side;
               this.origins[slot * 3 + 1] = this.anchor.y + up;
               this.origins[slot * 3 + 2] = this.anchor.z - (double)Mth.sin(yaw) * side;
               float len = this.height * (0.12F + (float)(this.seed >>> 8 & 7) * 0.02F);
               float ax = (float)(this.seed >>> 4 & 31) / 31.0F - 0.5F;
               float ay = (float)(this.seed >>> 9 & 31) / 31.0F - 0.2F;
               float az = (float)(this.seed >>> 14 & 31) / 31.0F - 0.5F;
               float inv = len / Math.max(0.05F, Mth.sqrt(ax * ax + ay * ay + az * az));
               SeiryuDraw.boltPath(this.paths[slot], ax * inv, ay * inv, az * inv, 3, 0.25F, (long)this.seed);
               this.born[slot] = this.age;
            }
         }
      } else {
         this.finish();
      }
   }

   @Override
   public void renderGlow(VfxContext ctx) {
      if (!this.firstPerson) {
         int pts = SeiryuDraw.pointCount(3);

         for (int i = 0; i < 3; i++) {
            float life = ((float)(this.age - this.born[i]) + ctx.partialTick) / 5.0F;
            if (!(life < 0.0F) && !(life > 1.0F)) {
               float alpha = life < 0.3F ? 1.0F : (1.0F - life) / 0.7F;
               SeiryuDraw.ribbons(
                  ctx,
                  this.paths[i],
                  pts,
                  ctx.rx(this.origins[i * 3]),
                  ctx.ry(this.origins[i * 3 + 1]),
                  ctx.rz(this.origins[i * 3 + 2]),
                  0.018F * this.height / 3.6F + 0.008F,
                  0.6F,
                  15984895,
                  11955455,
                  8011007,
                  alpha
               );
            }
         }
      }
   }

   @Override
   public void renderTextured(VfxContext ctx) {
      if (!this.firstPerson) {
         float pulse = 0.8F + 0.2F * Mth.sin(ctx.time * 0.35F);
         SeiryuDraw.flare(
            ctx, ctx.rx(this.x), ctx.ry(this.y + (double)this.height * 0.55), ctx.rz(this.z), this.height * 0.42F * pulse, 11955455, 8011007, 0.4F
         );
      }
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
