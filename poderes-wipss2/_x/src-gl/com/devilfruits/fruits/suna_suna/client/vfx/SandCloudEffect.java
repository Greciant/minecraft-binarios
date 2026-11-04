package com.devilfruits.fruits.suna_suna.client.vfx;

import com.devilfruits.client.logia.LogiaMovementClient;
import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.client.sound.SoundDesign;
import com.devilfruits.fruits.suna_suna.SandTravel;
import com.devilfruits.fruits.suna_suna.SunaParticles;
import com.devilfruits.fruits.suna_suna.SunaSounds;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class SandCloudEffect extends VfxEffect {
   private static final int TAIL = 8;
   private static final int SLICES = 16;
   public final int playerId;
   private double x;
   private double y;
   private double z;
   private double px;
   private double py;
   private double pz;
   private int leftAt = -1;
   private int recorded;
   private final Vec3[] history = new Vec3[8];
   private final Vec3[] across = new Vec3[8];
   private final float[] widths = new float[8];
   private final float[] alphas = new float[8];

   public SandCloudEffect(Player player) {
      this.playerId = player.getId();
      this.x = this.px = player.getX();
      this.y = this.py = player.getY() + 0.9;
      this.z = this.pz = player.getZ();
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      this.px = this.x;
      this.py = this.y;
      this.pz = this.z;
      Entity e = level.getEntity(this.playerId);
      if (e instanceof Player player && !e.isRemoved() && SandTravel.CLOUD.id().equals(LogiaMovementClient.modeOf(player))) {
         this.x = e.getX();
         this.y = e.getY() + 0.9;
         this.z = e.getZ();
         System.arraycopy(this.history, 0, this.history, 1, 7);
         this.history[0] = new Vec3(this.x, this.y, this.z);
         this.recorded = Math.min(8, this.recorded + 1);
         RandomSource r = Fx.random();
         int n = Fx.count(2);

         for (int i = 0; i < n; i++) {
            Fx.spawn(
               r.nextInt(4) == 0 ? (ParticleOptions)SunaParticles.DUST.get() : (ParticleOptions)SunaParticles.GRAIN.get(),
               this.x + Fx.spread(0.8),
               this.y - 0.5 + r.nextDouble() * 0.6,
               this.z + Fx.spread(0.8),
               Fx.spread(0.02),
               -0.03,
               Fx.spread(0.02)
            );
         }

         if (this.age % 24 == 1) {
            SoundDesign.play(new Vec3(this.x, this.y, this.z), SunaSounds.LAYER_GUST, 0.25F, 1.1F + r.nextFloat() * 0.2F);
         }

         return;
      }

      if (this.leftAt < 0) {
         this.leftAt = this.age;
      }

      if (this.age - this.leftAt > 6) {
         this.finish();
      }
   }

   @Override
   public void renderTextured(VfxContext ctx) {
      float pt = ctx.partialTick;
      float t = (float)this.age + pt;
      float fade = this.leftAt < 0 ? Math.min(1.0F, t / 5.0F) : Math.max(0.0F, 1.0F - ((float)(this.age - this.leftAt) + pt) / 6.0F);
      double cx = Mth.lerp((double)pt, this.px, this.x);
      double cy = Mth.lerp((double)pt, this.py, this.y);
      double cz = Mth.lerp((double)pt, this.pz, this.z);
      float near = ctx.proximity(cx, cy, cz, 1.3F, 2.8F, 0.0F) * fade;
      if (!(near <= 0.01F)) {
         int light = SandArt.light(cx, cy, cz);
         VertexConsumer vc = SandArt.wispy(ctx, SandArt.STORM);
         band(ctx, vc, cx, cy - 0.5, cz, 0.75F * fade, 0.55F, t * 0.09F, t * 0.03F, 0.95F * near, light);
         band(ctx, vc, cx, cy - 0.15, cz, 1.0F * fade, 0.7F, -t * 0.06F + 1.0F, t * 0.025F, 0.9F * near, light);
         band(ctx, vc, cx, cy + 0.3, cz, 0.8F * fade, 0.55F, t * 0.075F + 2.0F, t * 0.035F, 0.8F * near, light);
         band(ctx, vc, cx, cy - 0.3, cz, 0.45F * fade, 0.9F, -t * 0.11F, t * 0.05F, 0.9F * near, light);
         if (near > 0.5F) {
            for (int i = 0; i < 6; i++) {
               float a = t * 0.05F + (float)i * (float) (Math.PI * 2) / 6.0F;
               float rr = 0.4F + 0.15F * Mth.sin((float)i * 1.7F);
               double py2 = cy + (double)Mth.sin(t * 0.08F + (float)i) * 0.2 + (double)(i % 3 - 1) * 0.3;
               float size = (0.36F + 0.08F * (float)(i % 3)) * fade;
               Quaternionf rot = new Quaternionf().rotateY(-a + t * 0.02F).rotateX(0.4F * (float)i + t * 0.03F);
               SandArt.chunk(ctx, vc, cx + (double)(Mth.cos(a) * rr), py2, cz + (double)(Mth.sin(a) * rr), size, size * 0.8F, size, rot, i * 3, light);
            }

            VertexConsumer lumps = SandArt.solid(ctx, SandArt.PACKED);

            for (int i = 0; i < 4; i++) {
               float a = -t * 0.22F + (float)i * (float) (Math.PI * 2) / 4.0F;
               float size = (0.07F + 0.02F * (float)(i % 2)) * fade;
               Quaternionf rot = new Quaternionf().rotateY(t * 0.4F + (float)i).rotateX(t * 0.3F + (float)i);
               SandArt.chunk(
                  ctx, lumps, cx + (double)Mth.cos(a) * 0.95, cy - 0.3 + (double)i * 0.22, cz + (double)Mth.sin(a) * 0.95, size, size, size, rot, i, light
               );
            }
         }

         if (this.recorded >= 3) {
            int n = this.recorded;
            Vec3 head = new Vec3(cx, cy, cz);

            for (int i = 0; i < n; i++) {
               float f = (float)i / (float)(n - 1);
               Vec3 p = i == 0 ? head : this.history[i];
               Vec3 next = i + 1 < n ? this.history[i + 1] : p.subtract(this.history[i - 1].subtract(p));
               Vec3 along = next.subtract(p);
               Vec3 side = new Vec3(-along.z, 0.0, along.x);
               this.across[i] = side.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
               this.widths[i] = 0.55F * (1.0F - f) + 0.05F;
               this.alphas[i] = 0.55F * (1.0F - f) * near;
            }

            Vec3[] path = new Vec3[n];
            path[0] = head;
            System.arraycopy(this.history, 1, path, 1, n - 1);
            SandArt.ribbon(ctx, SandArt.wispy(ctx, SandArt.STREAM), path, this.across, this.widths, this.alphas, n, 0.5F, -t * 0.3F, 16777215, light);
         }
      }
   }

   private static void band(
      VfxContext ctx, VertexConsumer vc, double cx, double y0, double cz, float r, float h, float turn, float rise, float alpha, int light
   ) {
      int c0 = SandArt.argb(16777215, alpha * 0.6F);
      int c1 = SandArt.argb(16777215, alpha);
      int c2 = SandArt.argb(16777215, alpha * 0.6F);
      float around = (float)Math.max(2, Math.round((float) (Math.PI * 2) * r / 2.0F));

      for (int i = 0; i < 16; i++) {
         float a0 = turn + (float)i * (float) (Math.PI * 2) / 16.0F;
         float a1 = turn + (float)(i + 1) * (float) (Math.PI * 2) / 16.0F;
         float r0 = r * (1.0F + 0.12F * Mth.sin(a0 * 3.0F + turn * 2.0F));
         float r1 = r * (1.0F + 0.12F * Mth.sin(a1 * 3.0F + turn * 2.0F));
         float u0 = (float)i / 16.0F * around;
         float u1 = (float)(i + 1) / 16.0F * around;

         for (int k = 0; k < 2; k++) {
            float ya = k == 0 ? 0.0F : h * 0.5F;
            float yb = k == 0 ? h * 0.5F : h;
            float sa = k == 0 ? 0.8F : 1.0F;
            float sb = k == 0 ? 1.0F : 0.8F;
            int ca = k == 0 ? c0 : c1;
            int cb = k == 0 ? c1 : c2;
            float va = ya * 0.5F - rise;
            float vb = yb * 0.5F - rise;
            SandArt.vertex(ctx, vc, new Vec3(cx + (double)(Mth.cos(a0) * r0 * sa), y0 + (double)ya, cz + (double)(Mth.sin(a0) * r0 * sa)), u0, va, ca, light);
            SandArt.vertex(ctx, vc, new Vec3(cx + (double)(Mth.cos(a1) * r1 * sa), y0 + (double)ya, cz + (double)(Mth.sin(a1) * r1 * sa)), u1, va, ca, light);
            SandArt.vertex(ctx, vc, new Vec3(cx + (double)(Mth.cos(a1) * r1 * sb), y0 + (double)yb, cz + (double)(Mth.sin(a1) * r1 * sb)), u1, vb, cb, light);
            SandArt.vertex(ctx, vc, new Vec3(cx + (double)(Mth.cos(a0) * r0 * sb), y0 + (double)yb, cz + (double)(Mth.sin(a0) * r0 * sb)), u0, vb, cb, light);
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
