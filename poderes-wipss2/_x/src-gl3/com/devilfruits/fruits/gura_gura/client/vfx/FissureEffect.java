package com.devilfruits.fruits.gura_gura.client.vfx;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.VfxDraw;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.fruits.gura_gura.GuraParticles;
import com.devilfruits.fruits.gura_gura.quake.FissurePattern;
import com.devilfruits.registry.DFParticles;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class FissureEffect extends VfxEffect {
   private static final float SPREAD = 1.6F;
   private static final int GLOW_TICKS = 50;
   private static final int FADE_TICKS = 35;
   private final double x;
   private final double y;
   private final double z;
   private final float[][] px;
   private final float[][] py;
   private final float[][] pz;
   private final float[][] dist;
   private final float[] widths;
   private final BlockState[][] ground;
   private final float maxDist;
   private float lastReach;
   private static final Vector3f A = new Vector3f();
   private static final Vector3f B = new Vector3f();
   private static final Vector3f D = new Vector3f();
   private static final Vector3f W = new Vector3f();
   private static final Vector3f UP = new Vector3f(0.0F, 1.0F, 0.0F);

   public FissureEffect(ClientLevel level, Vec3 epicentre, long seed, int lines, float length) {
      this.x = epicentre.x;
      this.y = epicentre.y;
      this.z = epicentre.z;
      List<FissurePattern.Line> pattern = FissurePattern.generate(seed, lines, length);
      int n = pattern.size();
      this.px = new float[n][];
      this.py = new float[n][];
      this.pz = new float[n][];
      this.dist = new float[n][];
      this.widths = new float[n];
      this.ground = new BlockState[n][];
      float max = 0.0F;
      MutableBlockPos pos = new MutableBlockPos();

      for (int i = 0; i < n; i++) {
         FissurePattern.Line line = pattern.get(i);
         int size = line.size();
         this.px[i] = new float[size];
         this.py[i] = new float[size];
         this.pz[i] = new float[size];
         this.dist[i] = (float[])line.dist().clone();
         this.ground[i] = new BlockState[size];
         this.widths[i] = line.width();

         for (int k = 0; k < size; k++) {
            double wx = this.x + (double)line.x()[k];
            double wz = this.z + (double)line.z()[k];
            BlockState state = Fx.groundAt(level, wx, this.y - 1.0, wz, pos);
            double top = state != null ? (double)pos.getY() + 1.0 : this.y;
            this.px[i][k] = (float)(wx - this.x);
            this.py[i][k] = (float)(top + 0.03 - this.y);
            this.pz[i][k] = (float)(wz - this.z);
            this.ground[i][k] = state;
            max = Math.max(max, this.dist[i][k]);
         }
      }

      this.maxDist = max;
   }

   private int growTicks() {
      return Mth.ceil(this.maxDist / 1.6F);
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      float reach = (float)this.age * 1.6F;
      if (reach > this.lastReach) {
         for (int i = 0; i < this.px.length; i++) {
            for (int k = 1; k < this.px[i].length; k++) {
               if (this.dist[i][k] > this.lastReach && this.dist[i][k] <= reach) {
                  this.reveal(i, k);
               }
            }
         }

         this.lastReach = reach;
      }

      if (this.age > this.growTicks() + 50 + 35) {
         this.finish();
      }
   }

   private void reveal(int line, int k) {
      double wx = this.x + (double)this.px[line][k];
      double wy = this.y + (double)this.py[line][k];
      double wz = this.z + (double)this.pz[line][k];
      BlockState state = this.ground[line][k];
      if (state != null) {
         int debris = Fx.count(3);

         for (int j = 0; j < debris; j++) {
            Fx.spawn(
               new BlockParticleOption(DFParticles.BLOCK_DEBRIS.get(), state),
               wx + Fx.spread(0.4),
               wy + 0.1,
               wz + Fx.spread(0.4),
               Fx.spread(0.12),
               0.25 + Fx.random().nextDouble() * 0.35,
               Fx.spread(0.12)
            );
         }
      }

      if (Fx.random().nextFloat() < 0.5F * this.widths[line]) {
         Fx.spawn((ParticleOptions)DFParticles.DUST_PUFF.get(), wx, wy + 0.2, wz, Fx.spread(0.03), 0.04, Fx.spread(0.03));
      }

      if (Fx.random().nextFloat() < 0.35F) {
         Fx.spawn((ParticleOptions)GuraParticles.CRACK_SPARK.get(), wx, wy + 0.05, wz, 0.0, 0.12, 0.0);
      }
   }

   @Override
   public void renderGlow(VfxContext ctx) {
      float t = (float)this.age + ctx.partialTick;
      float reach = t * 1.6F;
      float alpha = 1.0F - Mth.clamp((t - (float)this.growTicks() - 50.0F) / 35.0F, 0.0F, 1.0F);
      if (!(alpha <= 0.0F)) {
         float ox = ctx.rx(this.x);
         float oy = ctx.ry(this.y);
         float oz = ctx.rz(this.z);

         for (int i = 0; i < this.px.length; i++) {
            float w = this.widths[i];

            for (int k = 1; k < this.px[i].length; k++) {
               float d0 = this.dist[i][k - 1];
               if (d0 >= reach) {
                  break;
               }

               float f = Math.min(1.0F, (reach - d0) / Math.max(0.01F, this.dist[i][k] - d0));
               A.set(ox + this.px[i][k - 1], oy + this.py[i][k - 1], oz + this.pz[i][k - 1]);
               B.set(
                  ox + Mth.lerp(f, this.px[i][k - 1], this.px[i][k]),
                  oy + Mth.lerp(f, this.py[i][k - 1], this.py[i][k]),
                  oz + Mth.lerp(f, this.pz[i][k - 1], this.pz[i][k])
               );
               D.set(B).sub(A);
               W.set(D).cross(UP);
               if (!(W.lengthSquared() < 1.0E-8F)) {
                  W.normalize();
                  float taper = 1.0F - 0.6F * (d0 / Math.max(1.0F, this.maxDist));
                  float flicker = 0.85F + 0.15F * Mth.sin(t * 1.3F + (float)k + (float)i * 2.1F);
                  int ga = (int)(alpha * flicker * 0.45F * 255.0F);
                  int ca = (int)(alpha * flicker * 255.0F);
                  VfxDraw.ribbon(ctx.buffer, ctx.matrix, A, B, W, 0.32F * w * taper, 120, 214, 255, ga);
                  VfxDraw.ribbon(ctx.buffer, ctx.matrix, A, B, W, 0.075F * w * taper + 0.02F, 255, 255, 255, ca);
               }
            }
         }
      }
   }

   @Override
   public double cullDistance() {
      return 128.0;
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
