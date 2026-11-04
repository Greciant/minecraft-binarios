package com.devilfruits.fruits.suna_suna.client.vfx;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.VfxDraw;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.fruits.suna_suna.SunaParticles;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class SandBallEffect extends VfxEffect {
   private static final ResourceLocation TEXTURE = DevilFruits.id("textures/entity/suna_suna/sand_ball.png");
   private static final RenderType TYPE = RenderType.entityCutoutNoCull(TEXTURE);
   private static final float[][] BOXES = new float[][]{
      {-4.0F, -4.0F, -4.0F, 4.0F, 4.0F, 4.0F},
      {-5.0F, -3.0F, -3.0F, 5.0F, 3.0F, 3.0F},
      {-3.0F, -5.0F, -3.0F, 3.0F, 5.0F, 3.0F},
      {-3.0F, -3.0F, -5.0F, 3.0F, 3.0F, 5.0F}
   };
   public final long seed;
   private final float size;
   private final boolean heavy;
   private final int lifetime;
   private double x;
   private double y;
   private double z;
   private double px;
   private double py;
   private double pz;
   private double vx;
   private double vy;
   private double vz;
   private final RandomSource random;

   public SandBallEffect(Vec3 start, Vec3 velocity, float size, int lifetime, long seed) {
      this.x = this.px = start.x;
      this.y = this.py = start.y;
      this.z = this.pz = start.z;
      this.vx = velocity.x;
      this.vy = velocity.y;
      this.vz = velocity.z;
      this.size = size;
      this.heavy = lifetime > 0;
      this.lifetime = Math.max(1, Math.abs(lifetime));
      this.seed = seed;
      this.random = RandomSource.create(seed);
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.age > this.lifetime + 1) {
         this.finish();
      } else {
         this.px = this.x;
         this.py = this.y;
         this.pz = this.z;
         Vec3 from = new Vec3(this.x, this.y, this.z);
         Vec3 to = new Vec3(this.x + this.vx, this.y + this.vy, this.z + this.vz);
         HitResult hit = PhysicsWorld.clip(level, new ClipContext(from, to, Block.COLLIDER, Fluid.ANY, CollisionContext.empty()));
         if (hit.getType() != Type.MISS) {
            this.finish();
         } else {
            this.x = to.x;
            this.y = to.y;
            this.z = to.z;
            if (this.heavy) {
               this.vy -= 0.003;
            }

            Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            if (!(camera.distanceToSqr(this.x, this.y, this.z) < 2.5600000000000005)) {
               if (this.heavy) {
                  this.ball();
               } else {
                  this.stream();
               }
            }
         }
      }
   }

   private void ball() {
      double r = (double)this.size * 0.6;
      int shed = Fx.count(4.0F * this.size);

      for (int i = 0; i < shed; i++) {
         double a = this.random.nextDouble() * Math.PI * 2.0;
         double b = (this.random.nextDouble() - 0.5) * Math.PI;
         double ox = Math.cos(a) * Math.cos(b) * r;
         double oy = Math.sin(b) * r;
         double oz = Math.sin(a) * Math.cos(b) * r;
         Fx.spawn(
            this.random.nextInt(4) == 0 ? (ParticleOptions)SunaParticles.CLUMP.get() : (ParticleOptions)SunaParticles.GRAIN.get(),
            this.x + ox,
            this.y + oy,
            this.z + oz,
            -oz * 0.3 + this.vx * 0.3,
            0.02 + this.vy * 0.3,
            ox * 0.3 + this.vz * 0.3
         );
      }

      if (this.age % 2 == 0) {
         Fx.spawn((ParticleOptions)SunaParticles.DUST.get(), this.px + Fx.spread(r), this.py + Fx.spread(r), this.pz + Fx.spread(r), 0.0, 0.01, 0.0);
      }
   }

   @Override
   public void renderTextured(VfxContext ctx) {
      if (this.heavy) {
         float pt = ctx.partialTick;
         double wx = Mth.lerp((double)pt, this.px, this.x);
         double wy = Mth.lerp((double)pt, this.py, this.y);
         double wz = Mth.lerp((double)pt, this.pz, this.z);
         ClientLevel level = Minecraft.getInstance().level;
         if (level != null && !(ctx.proximity(wx, wy, wz, 1.0F, 1.2F, 0.0F) <= 0.0F)) {
            float t = (float)this.age + pt;
            float yaw = (float)Math.atan2(this.vx, this.vz);
            int light = LevelRenderer.getLightColor(level, BlockPos.containing(wx, wy, wz));
            float unit = this.size / 10.0F;
            VertexConsumer vc = ctx.buffers.getBuffer(TYPE);
            Quaternionf core = new Quaternionf().rotateY(yaw).rotateX(t * 0.45F);
            draw(vc, ctx, wx, wy, wz, core, unit, BOXES.length, 0, light);
            Quaternionf shell = new Quaternionf().rotateY(yaw).rotateZ(-t * 0.6F).rotateX(0.7F);
            draw(vc, ctx, wx, wy, wz, shell, unit * 1.6F, 1, 16, light);
         }
      }
   }

   private static void draw(VertexConsumer vc, VfxContext ctx, double wx, double wy, double wz, Quaternionf q, float unit, int boxes, int tileU, int light) {
      Matrix4f pose = new Matrix4f(ctx.matrix).translate(ctx.rx(wx), ctx.ry(wy), ctx.rz(wz)).rotate(q).scale(unit);
      Matrix3f rot = new Matrix3f().rotation(q);

      for (int i = 0; i < boxes; i++) {
         float[] b = BOXES[i];
         VfxDraw.texturedBox(vc, pose, rot, b[0], b[1], b[2], b[3], b[4], b[5], tileU, 0, 16, 32, 16, -1, light);
      }
   }

   private void stream() {
      int n = Fx.count(7);

      for (int i = 0; i < n; i++) {
         double t = this.random.nextDouble();
         double sx = Mth.lerp(t, this.px, this.x);
         double sy = Mth.lerp(t, this.py, this.y);
         double sz = Mth.lerp(t, this.pz, this.z);
         Fx.spawn(
            (ParticleOptions)SunaParticles.GRAIN.get(),
            sx + Fx.spread(0.12),
            sy + Fx.spread(0.12),
            sz + Fx.spread(0.12),
            this.vx * 0.25,
            this.vy * 0.25 + 0.02,
            this.vz * 0.25
         );
      }
   }

   public double vx() {
      return this.vx;
   }

   public double vy() {
      return this.vy;
   }

   public double vz() {
      return this.vz;
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

   @Override
   public double cullDistance() {
      return 128.0;
   }
}
