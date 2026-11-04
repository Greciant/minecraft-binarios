package com.devilfruits.fruits.suke_suke.client.vfx;

import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.ShockRing;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.fruits.suke_suke.clear.ShipView;
import com.devilfruits.fruits.suke_suke.client.ShipLook;
import com.mojang.blaze3d.vertex.BufferBuilder;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public final class ShipCinematic extends Cinematic {
   private static final int GLINT = 16056312;
   private static final int[][] EDGES = new int[12][];
   private final UUID ship;
   private final boolean caster;
   @Nullable
   private Vec3 origin;
   private final double[] c = new double[24];
   private boolean have;
   private Vec3 centre = Vec3.ZERO;
   private boolean returned;
   private static final float[] XS;
   private static final float[] YS;
   private static final float[] ZS;
   private static final float[] WS;
   private static final float[] HS;
   private static final float[] AS;

   public ShipCinematic(UUID ship, @Nullable Vec3 origin, boolean caster) {
      super(VfxAnchor.of(null, origin == null ? Vec3.ZERO : origin), VfxParams.NONE);
      this.ship = ship;
      this.origin = origin;
      this.caster = caster;
      if (caster) {
         CameraDirector.dolly(1.22F, 0.06F);
      }
   }

   public UUID ship() {
      return this.ship;
   }

   public void origin(Vec3 at, boolean byLocal) {
      this.origin = at;
      if (byLocal && !this.caster) {
         CameraDirector.dolly(1.22F, 0.06F);
      }
   }

   @Override
   public double cullDistance() {
      return 320.0;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      this.have = ShipLook.bridge.corners(level, this.ship, 1.0F, this.c);
      if (this.have) {
         this.centre = new Vec3((this.c[0] + this.c[21]) * 0.5, (this.c[1] + this.c[22]) * 0.5, (this.c[2] + this.c[23]) * 0.5);
      }

      if (this.caster && this.age == 36) {
         CameraDirector.snapDolly();
      }

      ShipView.Entry e = ShipView.get(this.ship);
      long back = ShipView.returned(this.ship);
      if (e == null && back < 0L) {
         this.finish();
      } else if (this.have) {
         float now = (float)level.getGameTime();
         RandomSource r = VfxSettings.random();
         if (e != null && now - (float)e.start() < 22.0F) {
            float front = this.sweepRadius(now - (float)e.start());

            for (int[] edge : EDGES) {
               Vec3 a = this.corner(edge[0]);
               Vec3 b = this.corner(edge[1]);
               Vec3 mid = a.add(b).scale(0.5);
               if (Math.abs(this.horizontal(mid) - (double)front) < 1.2 && r.nextFloat() < 0.7F) {
                  Vec3 at = a.lerp(b, r.nextDouble());
                  VfxParticles.emit(
                        VfxPresets.GLASS_GLINT, at.x, at.y, at.z, (r.nextDouble() - 0.5) * 0.04, 0.02 + r.nextDouble() * 0.04, (r.nextDouble() - 0.5) * 0.04
                     )
                     .tint(r.nextBoolean() ? 16777215 : 12578784)
                     .size(1.4F);
               }
            }
         }

         if (e == null && !this.returned) {
            this.returned = true;
            double minY = Math.min(Math.min(this.c[1], this.c[4]), Math.min(this.c[7], this.c[10]));
            float reach = (float)this.maxReach();
            ShockRing.at(new Vec3(this.centre.x, minY + 0.1, this.centre.z), new Vec3(0.0, 1.0, 0.0))
               .radius(reach, 0.5F)
               .life(22)
               .strength(0.7F)
               .band(0.08F)
               .rim(0.25F, 14546376)
               .spawn();
         }
      }
   }

   private Vec3 corner(int k) {
      return new Vec3(this.c[k * 3], this.c[k * 3 + 1], this.c[k * 3 + 2]);
   }

   private Vec3 from() {
      return this.origin != null ? this.origin : this.centre;
   }

   private double horizontal(Vec3 p) {
      Vec3 o = this.from();
      double dx = p.x - o.x;
      double dz = p.z - o.z;
      return Math.sqrt(dx * dx + dz * dz);
   }

   private double maxReach() {
      double m = 2.0;

      for (int k = 0; k < 8; k++) {
         m = Math.max(m, this.horizontal(this.corner(k)));
      }

      return m + 1.0;
   }

   private float sweepRadius(float since) {
      float t = Mth.clamp(since / 16.5F, 0.0F, 1.0F);
      return (float)((double)(1.0F - (1.0F - t) * (1.0F - t)) * this.maxReach());
   }

   @Override
   public void render(VfxFrame f) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null && ShipLook.bridge.corners(level, this.ship, f.partial, this.c)) {
         float now = (float)level.getGameTime() + f.partial;
         ShipView.Entry e = ShipView.get(this.ship);
         if (e != null) {
            float since = now - (float)e.start();
            float left = (float)e.end() - now;
            if (since < 26.0F) {
               this.sweep(f, since);
            } else if (left < 60.0F) {
               float pulse = (ShipLook.warnPulse(since, left) - 0.15F) / 0.4F;
               float k = 1.0F - Math.max(0.0F, left) / 60.0F;
               this.edges(f, 0.45F + 0.55F * pulse, 0.04F + 0.02F * pulse, 0.6F + 0.6F * k * pulse);
            }
         } else {
            long back = ShipView.returned(this.ship);
            if (back >= 0L) {
               float t = (now - (float)back) / 16.0F;
               if (t < 1.0F) {
                  this.edges(f, (1.0F - t) * 0.9F, 0.04F, (1.0F - t) * 0.9F);
               }
            }
         }
      }
   }

   private void sweep(VfxFrame f, float since) {
      float front = this.sweepRadius(since);
      float fade = 1.0F - Mth.clamp((since - 22.0F) / 4.0F, 0.0F, 1.0F);
      Vec3 o = this.from();
      double minY = Double.MAX_VALUE;

      for (int k = 0; k < 8; k++) {
         minY = Math.min(minY, this.c[k * 3 + 1]);
      }

      float oy = (float)Math.max(minY, Math.min(o.y, minY + 3.0));
      new ShockRing.Band().draw(f, f.rx(o.x), f.ry((double)oy + 0.1), f.rz(o.z), 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, front, 0.7F, 0.4F * fade, 0.0F);
      if (f.allowMesh()) {
         BufferBuilder b = f.mesh(MeshTex.FLAME_RIBBON);

         for (int[] edge : EDGES) {
            Vec3 a = this.corner(edge[0]);
            Vec3 bb = this.corner(edge[1]);
            double d = this.horizontal(a.add(bb).scale(0.5));
            float lit = (float)Math.exp(-Math.pow(((double)front - d) / 1.6, 2.0)) * fade;
            float after = (double)front > d ? 0.18F * fade : 0.0F;
            float alpha = Math.max(lit, after);
            if (alpha > 0.01F) {
               strip(b, f, a, bb, 0.035F + 0.03F * lit, MeshWriter.argb(16056312, alpha), 2.8F);
            }
         }
      }
   }

   private void edges(VfxFrame f, float lineAlpha, float width, float bend) {
      BufferBuilder d = f.distortion();
      BufferBuilder b = f.allowMesh() ? f.mesh(MeshTex.FLAME_RIBBON) : null;

      for (int[] edge : EDGES) {
         Vec3 a = this.corner(edge[0]);
         Vec3 bb = this.corner(edge[1]);
         if (d != null && bend > 0.01F) {
            band(d, f, a, bb, 0.35F, 0.6F * bend);
         }

         if (b != null && lineAlpha > 0.01F) {
            strip(b, f, a, bb, width, MeshWriter.argb(14546376, Mth.clamp(lineAlpha, 0.0F, 1.0F)), 1.8F);
         }
      }
   }

   private static void strip(BufferBuilder b, VfxFrame f, Vec3 a, Vec3 c, float width, int argb, float emissive) {
      XS[0] = f.rx(a.x);
      YS[0] = f.ry(a.y);
      ZS[0] = f.rz(a.z);
      XS[1] = f.rx(c.x);
      YS[1] = f.ry(c.y);
      ZS[1] = f.rz(c.z);
      WS[0] = WS[1] = width;
      float alpha = (float)(argb >>> 24) / 255.0F;
      AS[0] = AS[1] = alpha;
      MeshWriter.strip(b, 2, XS, YS, ZS, WS, HS, AS, f.time * 0.04F, (float)a.distanceTo(c) * 0.25F, argb & 16777215, 0, emissive);
   }

   private static void band(BufferBuilder d, VfxFrame f, Vec3 a, Vec3 c, float width, float strength) {
      float ax = f.rx(a.x);
      float ay = f.ry(a.y);
      float az = f.rz(a.z);
      float cx = f.rx(c.x);
      float cy = f.ry(c.y);
      float cz = f.rz(c.z);
      float dx = cx - ax;
      float dy = cy - ay;
      float dz = cz - az;
      float nx = dy * f.look.z - dz * f.look.y;
      float ny = dz * f.look.x - dx * f.look.z;
      float nz = dx * f.look.y - dy * f.look.x;
      float nl = Mth.sqrt(nx * nx + ny * ny + nz * nz);
      if (!(nl < 1.0E-4F)) {
         nx /= nl;
         ny /= nl;
         nz /= nl;
         float sx = -(nx * f.left.x + ny * f.left.y + nz * f.left.z);
         float sy = nx * f.up.x + ny * f.up.y + nz * f.up.z;
         float sl = Mth.sqrt(sx * sx + sy * sy) + 1.0E-4F;
         sx /= sl;
         sy /= sl;
         MeshWriter.distortVertex(d, ax - nx * width, ay - ny * width, az - nz * width, -sx, -sy, strength, 0.0F, 3);
         MeshWriter.distortVertex(d, cx - nx * width, cy - ny * width, cz - nz * width, -sx, -sy, strength, 0.0F, 3);
         MeshWriter.distortVertex(d, cx + nx * width, cy + ny * width, cz + nz * width, sx, sy, strength, 1.0F, 3);
         MeshWriter.distortVertex(d, ax + nx * width, ay + ny * width, az + nz * width, sx, sy, strength, 1.0F, 3);
      }
   }

   @Override
   public double x() {
      return this.centre.x;
   }

   @Override
   public double y() {
      return this.centre.y;
   }

   @Override
   public double z() {
      return this.centre.z;
   }

   static {
      int i = 0;

      for (int k = 0; k < 8; k++) {
         for (int bit = 1; bit <= 4; bit <<= 1) {
            if ((k & bit) == 0) {
               EDGES[i++] = new int[]{k, k | bit};
            }
         }
      }

      XS = new float[2];
      YS = new float[2];
      ZS = new float[2];
      WS = new float[2];
      HS = new float[]{-1.0F, -1.0F};
      AS = new float[]{1.0F, 1.0F};
   }
}
