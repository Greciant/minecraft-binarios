package com.devilfruits.client.haki;

import com.devilfruits.client.render.VfxDraw;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.core.util.DeterministicRandom;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class BlackLightningEffect extends VfxEffect {
   private static final int DEPTH = 4;
   private static final int BRANCH_DEPTH = 3;
   private final double sx;
   private final double sy;
   private final double sz;
   private final float dx;
   private final float dy;
   private final float dz;
   private final int duration;
   private final float width;
   private final long seed;
   private final float[] main = new float[VfxDraw.boltPointCount(4) * 3];
   private final float[][] branches;
   private final float[] origins;
   private final Vector3f a = new Vector3f();
   private final Vector3f b = new Vector3f();
   private final Vector3f side = new Vector3f();

   public BlackLightningEffect(Vec3 from, Vec3 to, int duration, float width, int branches, long seed) {
      this.sx = from.x;
      this.sy = from.y;
      this.sz = from.z;
      this.dx = (float)(to.x - from.x);
      this.dy = (float)(to.y - from.y);
      this.dz = (float)(to.z - from.z);
      this.duration = Math.max(2, duration);
      this.width = width;
      this.seed = seed;
      this.branches = new float[branches][VfxDraw.boltPointCount(3) * 3];
      this.origins = new float[branches * 3];
      this.cut(0);
   }

   private void cut(int frame) {
      long s = this.seed + (long)frame * 7919L;
      VfxDraw.boltPath(this.main, this.dx, this.dy, this.dz, 4, 0.18F, s);
      DeterministicRandom r = new DeterministicRandom(s ^ 2685821657736338717L);
      int points = VfxDraw.boltPointCount(4);
      float len = (float)Math.sqrt((double)(this.dx * this.dx + this.dy * this.dy + this.dz * this.dz));

      for (int i = 0; i < this.branches.length; i++) {
         int at = 2 + r.nextInt(Math.max(1, points - 5));
         this.origins[i * 3] = this.main[at * 3];
         this.origins[i * 3 + 1] = this.main[at * 3 + 1];
         this.origins[i * 3 + 2] = this.main[at * 3 + 2];
         float bl = len * r.range(0.25F, 0.45F);
         VfxDraw.boltPath(
            this.branches[i],
            this.dx / len * bl + r.signed(bl),
            this.dy / len * bl + r.signed(bl),
            this.dz / len * bl + r.signed(bl),
            3,
            0.22F,
            s + (long)i * 31L
         );
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.age > this.duration) {
         this.finish();
      } else if (this.age % 2 == 0) {
         this.cut(this.age / 2);
      }
   }

   @Override
   public void renderShell(VfxContext ctx) {
      if (this.age <= this.duration * 2 / 3 || this.age % 2 != 1) {
         float ox = ctx.rx(this.sx);
         float oy = ctx.ry(this.sy);
         float oz = ctx.rz(this.sz);
         int points = VfxDraw.boltPointCount(4);
         int bp = VfxDraw.boltPointCount(3);
         this.bolt(ctx, this.main, points, ox, oy, oz, this.width * 2.4F, 242, 242, 242);

         for (int i = 0; i < this.branches.length; i++) {
            this.bolt(
               ctx,
               this.branches[i],
               bp,
               ox + this.origins[i * 3],
               oy + this.origins[i * 3 + 1],
               oz + this.origins[i * 3 + 2],
               this.width * 1.6F,
               242,
               242,
               242
            );
         }

         this.bolt(ctx, this.main, points, ox, oy, oz, this.width, 11, 11, 16);

         for (int i = 0; i < this.branches.length; i++) {
            this.bolt(
               ctx, this.branches[i], bp, ox + this.origins[i * 3], oy + this.origins[i * 3 + 1], oz + this.origins[i * 3 + 2], this.width * 0.6F, 21, 21, 29
            );
         }
      }
   }

   private void bolt(VfxContext ctx, float[] pts, int count, float ox, float oy, float oz, float w, int r, int g, int bl) {
      VertexConsumer vc = ctx.buffer;
      Matrix4f m = ctx.matrix;

      for (int i = 0; i + 1 < count; i++) {
         this.a.set(ox + pts[i * 3], oy + pts[i * 3 + 1], oz + pts[i * 3 + 2]);
         this.b.set(ox + pts[i * 3 + 3], oy + pts[i * 3 + 4], oz + pts[i * 3 + 5]);
         this.side.set(this.b).sub(this.a).cross(this.a.x + this.b.x, this.a.y + this.b.y, this.a.z + this.b.z);
         float l = this.side.length();
         if (!(l < 1.0E-6F)) {
            this.side.mul(w * 0.5F / l);
            vc.addVertex(m, this.a.x - this.side.x, this.a.y - this.side.y, this.a.z - this.side.z).setColor(r, g, bl, 255);
            vc.addVertex(m, this.a.x + this.side.x, this.a.y + this.side.y, this.a.z + this.side.z).setColor(r, g, bl, 255);
            vc.addVertex(m, this.b.x + this.side.x, this.b.y + this.side.y, this.b.z + this.side.z).setColor(r, g, bl, 255);
            vc.addVertex(m, this.b.x - this.side.x, this.b.y - this.side.y, this.b.z - this.side.z).setColor(r, g, bl, 255);
         }
      }
   }

   @Override
   public double x() {
      return this.sx + (double)this.dx * 0.5;
   }

   @Override
   public double y() {
      return this.sy + (double)this.dy * 0.5;
   }

   @Override
   public double z() {
      return this.sz + (double)this.dz * 0.5;
   }
}
