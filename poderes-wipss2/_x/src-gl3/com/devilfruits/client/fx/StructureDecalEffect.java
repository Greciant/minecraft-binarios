package com.devilfruits.client.fx;

import com.devilfruits.client.render.VfxDraw;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.core.config.CoreClientConfig;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.PhysicsStructure;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.physics.PhysicsWorldBridge;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class StructureDecalEffect extends VfxEffect {
   private static final List<StructureDecalEffect> LIVE = new ArrayList<>();
   private final ClientLevel level;
   private final Vec3 local;
   private final Vec3 localU;
   private final Vec3 localV;
   private final BlockPos backing;
   private final BlockImpactService.Decal kind;
   private final float size;
   private final int life;
   @Nullable
   private final UUID structure;
   private final float[][] branches;
   private double wx;
   private double wy;
   private double wz;
   private final Vector3f c = new Vector3f();
   private final Vector3f u = new Vector3f();
   private final Vector3f v = new Vector3f();
   private final Vector3f a = new Vector3f();
   private final Vector3f b = new Vector3f();
   private final Vector3f w = new Vector3f();

   public StructureDecalEffect(ClientLevel level, Vec3 local, Vec3 normal, BlockImpactService.Decal kind, float size, long seed) {
      this.level = level;
      this.local = local;
      this.kind = kind;
      this.size = Mth.clamp(size, 0.4F, 6.0F);
      Vector3f n = new Vector3f((float)normal.x, (float)normal.y, (float)normal.z);
      if (n.lengthSquared() < 1.0E-6F) {
         n.set(0.0F, 1.0F, 0.0F);
      }

      n.normalize();
      Vector3f lu = new Vector3f();
      Vector3f lv = new Vector3f();
      VfxDraw.basis(n, lu, lv);
      this.localU = new Vec3((double)lu.x, (double)lu.y, (double)lu.z);
      this.localV = new Vec3((double)lv.x, (double)lv.y, (double)lv.z);
      this.backing = BlockPos.containing(local.subtract((double)n.x * 0.6, (double)n.y * 0.6, (double)n.z * 0.6));

      this.life = switch (kind) {
         case CRACK -> 160;
         case SCORCH -> 500;
         case LIGHTNING -> 240;
         case SAND -> 50;
         case HAKI -> 120;
         case RING -> 14;
         case DENT -> 400;
      };
      this.branches = branches(kind, this.size, RandomSource.create(seed));
      PhysicsStructure s = PhysicsWorld.bridge().structureAt(level, BlockPos.containing(local));
      this.structure = s == null ? null : s.id();
      Vec3 at = PhysicsWorld.bridge().renderPosition(level, local, 1.0F);
      if (at == null) {
         this.finish();
      } else {
         this.wx = at.x;
         this.wy = at.y;
         this.wz = at.z;
      }

      this.enforceCap();
      LIVE.add(this);
   }

   private void enforceCap() {
      if (this.structure != null) {
         int cap = Math.max(1, (Integer)CoreClientConfig.STRUCTURE_DECALS.get());
         int count = 0;

         for (int i = LIVE.size() - 1; i >= 0; i--) {
            StructureDecalEffect e = LIVE.get(i);
            if (!e.isDone() && this.structure.equals(e.structure)) {
               if (++count >= cap) {
                  e.finish();
               }
            }
         }
      }
   }

   private static float[][] branches(BlockImpactService.Decal kind, float size, RandomSource r) {
      int count;
      int steps;
      float jag;
      float reach;
      switch (kind) {
         case CRACK:
            count = 5 + r.nextInt(3);
            steps = 4;
            jag = 0.45F;
            reach = size;
            break;
         case SCORCH:
         case SAND:
         case RING:
         default:
            return new float[0][];
         case LIGHTNING:
            count = 3 + r.nextInt(2);
            steps = 6;
            jag = 0.8F;
            reach = size * 1.2F;
            break;
         case HAKI:
            count = 5 + r.nextInt(2);
            steps = 5;
            jag = 0.9F;
            reach = size * 1.1F;
            break;
         case DENT:
            count = 7 + r.nextInt(3);
            steps = 2;
            jag = 0.25F;
            reach = size * 0.6F;
      }

      float[][] out = new float[count][];

      for (int i = 0; i < count; i++) {
         float angle = ((float)i + r.nextFloat() * 0.6F) * (float) (Math.PI * 2) / (float)count;
         float length = reach * (0.5F + 0.5F * r.nextFloat());
         float[] p = new float[(steps + 1) * 2];
         float x = 0.0F;
         float y = 0.0F;

         for (int s = 1; s <= steps; s++) {
            angle += (r.nextFloat() - 0.5F) * jag;
            x += Mth.cos(angle) * length / (float)steps;
            y += Mth.sin(angle) * length / (float)steps;
            p[s * 2] = x;
            p[s * 2 + 1] = y;
         }

         out[i] = p;
      }

      return out;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.age > this.life) {
         this.finish();
      } else {
         if (this.age % 4 == 0) {
            Vec3 at = PhysicsWorld.bridge().renderPosition(level, this.local, 1.0F);
            if (at == null || level.getBlockState(this.backing).isAir()) {
               this.finish();
               return;
            }

            this.wx = at.x;
            this.wy = at.y;
            this.wz = at.z;
         }
      }
   }

   @Override
   public void removed() {
      LIVE.remove(this);
   }

   private boolean frame(VfxContext ctx) {
      PhysicsWorldBridge bridge = PhysicsWorld.bridge();
      Vec3 at = bridge.renderPosition(this.level, this.local, ctx.partialTick);
      Vec3 du = bridge.renderDirection(this.level, this.local, this.localU, ctx.partialTick);
      Vec3 dv = bridge.renderDirection(this.level, this.local, this.localV, ctx.partialTick);
      if (at != null && du != null && dv != null) {
         this.c.set(ctx.rx(at.x), ctx.ry(at.y), ctx.rz(at.z));
         this.u.set((float)du.x, (float)du.y, (float)du.z);
         this.v.set((float)dv.x, (float)dv.y, (float)dv.z);
         return true;
      } else {
         return false;
      }
   }

   private float fade() {
      float t = (float)this.age / (float)this.life;
      return t < 0.66F ? 1.0F : Mth.clamp((1.0F - t) / 0.34F, 0.0F, 1.0F);
   }

   @Override
   public void renderShell(VfxContext ctx) {
      float f = this.fade();
      switch (this.kind) {
         case SCORCH:
            if (this.frame(ctx)) {
               VfxDraw.ring(ctx, this.c.x, this.c.y, this.c.z, this.u, this.v, this.size * 0.5F, this.size, 20, 22, 16, 11, 0.6F * f);
            }
            break;
         case LIGHTNING:
            if (this.frame(ctx)) {
               VfxDraw.ring(ctx, this.c.x, this.c.y, this.c.z, this.u, this.v, this.size * 0.35F, this.size * 0.7F, 16, 26, 22, 18, 0.5F * f);
            }
            break;
         case SAND:
            if (this.frame(ctx)) {
               float t = (float)this.age / (float)this.life;
               VfxDraw.ring(
                  ctx, this.c.x, this.c.y, this.c.z, this.u, this.v, this.size * (0.4F + 1.1F * t), this.size * 0.5F, 20, 214, 184, 124, 0.5F * (1.0F - t)
               );
            }
            break;
         case HAKI:
            if (this.frame(ctx)) {
               this.drawBranches(ctx, 0.05F * this.size, 12, 10, 16, 0.9F * f);
            }
            break;
         case RING:
            if (this.frame(ctx)) {
               float t = (float)this.age / (float)this.life;
               VfxDraw.ring(
                  ctx, this.c.x, this.c.y, this.c.z, this.u, this.v, this.size * 1.6F * t, 0.35F + this.size * 0.15F, 28, 236, 236, 246, 0.6F * (1.0F - t)
               );
            }
            break;
         case DENT:
            if (this.frame(ctx)) {
               VfxDraw.ring(ctx, this.c.x, this.c.y, this.c.z, this.u, this.v, this.size * 0.3F, this.size * 0.6F, 16, 30, 26, 22, 0.35F * f);
               this.drawBranches(ctx, 0.035F * this.size, 36, 30, 26, 0.7F * f);
            }
      }
   }

   @Override
   public void renderGlow(VfxContext ctx) {
      switch (this.kind) {
         case CRACK:
            if (this.frame(ctx)) {
               this.drawBranches(ctx, 0.03F * this.size, 180, 220, 255, 0.9F * this.fade());
            }
         case SCORCH:
         case SAND:
         default:
            break;
         case LIGHTNING:
            float g = Mth.clamp(1.0F - (float)this.age / 30.0F, 0.0F, 1.0F);
            if (g > 0.0F && this.frame(ctx)) {
               this.drawBranches(ctx, 0.04F * this.size, 200, 230, 255, g);
            }
            break;
         case HAKI:
            if (this.frame(ctx)) {
               this.drawBranches(ctx, 0.012F * this.size, 150, 60, 200, 0.5F * this.fade());
            }
      }
   }

   private void drawBranches(VfxContext ctx, float halfWidth, int r, int g, int bl, float alpha) {
      if (!(alpha <= 0.003F)) {
         int al = Mth.clamp((int)(alpha * 255.0F), 0, 255);

         for (float[] p : this.branches) {
            for (int s = 2; s + 1 < p.length; s += 2) {
               float x0 = p[s - 2];
               float y0 = p[s - 1];
               float x1 = p[s];
               float y1 = p[s + 1];
               this.a.set(this.c).add(this.u.x * x0 + this.v.x * y0, this.u.y * x0 + this.v.y * y0, this.u.z * x0 + this.v.z * y0);
               this.b.set(this.c).add(this.u.x * x1 + this.v.x * y1, this.u.y * x1 + this.v.y * y1, this.u.z * x1 + this.v.z * y1);
               float dx = x1 - x0;
               float dy = y1 - y0;
               float len = Math.max(1.0E-4F, Mth.sqrt(dx * dx + dy * dy));
               this.w.set(this.u.x * -dy + this.v.x * dx, this.u.y * -dy + this.v.y * dx, this.u.z * -dy + this.v.z * dx).div(len);
               float taper = 1.0F - ((float)s / 2.0F - 1.0F) / ((float)p.length / 2.0F);
               VfxDraw.ribbon(ctx.buffer, ctx.matrix, this.a, this.b, this.w, halfWidth * (0.4F + 0.6F * taper), r, g, bl, al);
            }
         }
      }
   }

   @Override
   public double x() {
      return this.wx;
   }

   @Override
   public double y() {
      return this.wy;
   }

   @Override
   public double z() {
      return this.wz;
   }

   @Override
   public double cullDistance() {
      return 64.0;
   }
}
