package com.devilfruits.fruits.suna_suna.client.vfx;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.fruits.suna_suna.SunaParticles;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;

public class SandSpearEffect extends VfxEffect {
   public final long seed;
   private final Vec3 velocity;
   private final int lifetime;
   private static final float LENGTH = 1.5F;
   private static final float WIDTH = 0.75F;
   private static final int TRAIL = 5;
   private final Vec3 fwd;
   private final Vec3 side;
   private final Vec3 up;
   private final Vec3[] path = new Vec3[5];
   private final Vec3[] across = new Vec3[5];
   private final float[] widths = new float[5];
   private final float[] alphas = new float[5];
   private double x;
   private double y;
   private double z;
   private double px;
   private double py;
   private double pz;

   public SandSpearEffect(Vec3 start, Vec3 velocity, int lifetime, long seed) {
      this.seed = seed;
      this.velocity = velocity;
      this.lifetime = lifetime;
      this.fwd = velocity.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : velocity.normalize();
      Vec3 s = new Vec3(-this.fwd.z, 0.0, this.fwd.x);
      this.side = s.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : s.normalize();
      this.up = this.side.cross(this.fwd).normalize();
      this.x = this.px = start.x;
      this.y = this.py = start.y;
      this.z = this.pz = start.z;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      this.px = this.x;
      this.py = this.y;
      this.pz = this.z;
      if (this.age > this.lifetime + 2) {
         this.finish();
      } else {
         Vec3 from = new Vec3(this.x, this.y, this.z);
         Vec3 to = from.add(this.velocity);
         if (PhysicsWorld.clip(level, new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, CollisionContext.empty())).getType() != Type.MISS) {
            this.finish();
         } else {
            this.x = to.x;
            this.y = to.y;
            this.z = to.z;
            if (Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(this.x, this.y, this.z) > 4.0) {
               int n = Fx.count(2);

               for (int i = 0; i < n; i++) {
                  double t = Fx.random().nextDouble();
                  Fx.spawn(
                     (ParticleOptions)SunaParticles.GRAIN.get(),
                     Mth.lerp(t, this.px, this.x),
                     Mth.lerp(t, this.py, this.y),
                     Mth.lerp(t, this.pz, this.z),
                     this.velocity.x * 0.1,
                     0.01,
                     this.velocity.z * 0.1
                  );
               }
            }
         }
      }
   }

   @Override
   public void renderTextured(VfxContext ctx) {
      float pt = ctx.partialTick;
      double wx = Mth.lerp((double)pt, this.px, this.x);
      double wy = Mth.lerp((double)pt, this.py, this.y);
      double wz = Mth.lerp((double)pt, this.pz, this.z);
      if (!(ctx.proximity(wx, wy, wz, 0.9F, 1.2F, 0.0F) <= 0.0F)) {
         float t = (float)this.age + pt;
         float form = Math.min(1.0F, t / 2.0F);
         int light = SandArt.light(wx, wy, wz);
         Vec3 centre = new Vec3(wx, wy, wz);
         float roll = t * 0.25F + (float)(this.seed & 7L);
         Vec3 a = this.up.scale((double)Mth.cos(roll)).add(this.side.scale((double)Mth.sin(roll)));
         Vec3 b = this.up.scale((double)(-Mth.sin(roll))).add(this.side.scale((double)Mth.cos(roll)));
         Vec3 tip = centre.add(this.fwd.scale(0.75 * (double)form));
         Vec3 grip = centre.subtract(this.fwd.scale(0.75 * (double)form));
         VertexConsumer vc = SandArt.solid(ctx, SandArt.BLADE);

         for (Vec3 plane : new Vec3[]{a, b}) {
            Vec3 w = plane.scale(0.375 * (double)form);
            SandArt.quad(ctx, vc, grip.add(w), tip.add(w), tip.subtract(w), grip.subtract(w), 0.0F, 0.0F, 1.0F, 1.0F, -1, light);
         }

         VertexConsumer stream = SandArt.wispy(ctx, SandArt.STREAM);

         for (Vec3 plane : new Vec3[]{a, b}) {
            for (int i = 0; i < 5; i++) {
               float f = (float)i / 4.0F;
               this.path[i] = grip.subtract(this.fwd.scale((double)f * 1.6 * (double)form));
               this.across[i] = plane;
               this.widths[i] = 0.14F * (1.0F - f * 0.5F);
               this.alphas[i] = (1.0F - f) * 0.75F;
            }

            SandArt.ribbon(ctx, stream, this.path, this.across, this.widths, this.alphas, 5, 0.6F, -t * 0.5F, 16777215, light);
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
