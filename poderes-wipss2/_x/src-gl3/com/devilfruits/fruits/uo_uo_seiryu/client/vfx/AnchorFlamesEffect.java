package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuParticles;
import com.devilfruits.fruits.uo_uo_seiryu.client.render.HybridAnchors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3d;

public class AnchorFlamesEffect extends VfxEffect {
   private final int entityId;
   private final int duration;
   private final int[] anchors;
   private final double[] pos;
   private final Vector3d at = new Vector3d();
   private double x;
   private double y;
   private double z;

   public AnchorFlamesEffect(Entity entity, int duration, int... anchors) {
      this.entityId = entity.getId();
      this.duration = Math.max(1, duration);
      this.anchors = anchors.length == 0 ? new int[]{0, 1, 2} : anchors;
      this.pos = new double[this.anchors.length * 3];
      this.x = entity.getX();
      this.y = entity.getY();
      this.z = entity.getZ();
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity e = level.getEntity(this.entityId);
      if (e != null && this.age <= this.duration) {
         this.x = e.getX();
         this.y = e.getY();
         this.z = e.getZ();
         float grow = (float)this.age / (float)this.duration;

         for (int k = 0; k < this.anchors.length; k++) {
            HybridAnchors.get(e, this.anchors[k], 1.0F, this.at);
            this.pos[k * 3] = this.at.x;
            this.pos[k * 3 + 1] = this.at.y;
            this.pos[k * 3 + 2] = this.at.z;
            int n = Fx.count(1.5F + grow * 2.0F);

            for (int i = 0; i < n; i++) {
               double ox = Fx.spread(0.7);
               double oy = Fx.spread(0.7);
               double oz = Fx.spread(0.7);
               Fx.spawn((ParticleOptions)SeiryuParticles.FLAME.get(), this.at.x + ox, this.at.y + oy, this.at.z + oz, -ox * 0.18, -oy * 0.18 + 0.02, -oz * 0.18);
            }
         }
      } else {
         this.finish();
      }
   }

   @Override
   public void renderTextured(VfxContext ctx) {
      float grow = Math.min(1.0F, ((float)this.age + ctx.partialTick) / (float)this.duration);
      float pulse = 0.85F + 0.15F * Mth.sin(((float)this.age + ctx.partialTick) * 1.3F);

      for (int k = 0; k < this.anchors.length; k++) {
         SeiryuDraw.flare(
            ctx,
            ctx.rx(this.pos[k * 3]),
            ctx.ry(this.pos[k * 3 + 1]),
            ctx.rz(this.pos[k * 3 + 2]),
            (0.3F + 0.6F * grow) * pulse,
            16774338,
            16742938,
            0.4F + 0.5F * grow
         );
      }
   }

   @Override
   public double x() {
      return this.x;
   }

   @Override
   public double y() {
      return this.y + 1.0;
   }

   @Override
   public double z() {
      return this.z;
   }
}
