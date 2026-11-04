package com.devilfruits.client.vfx.fx;

import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class ShockRing extends Cinematic {
   private static final int SEGMENTS = 36;
   private double cx;
   private double cy;
   private double cz;
   private final Vector3f n = new Vector3f(0.0F, 1.0F, 0.0F);
   private final Vector3f u = new Vector3f();
   private final Vector3f v = new Vector3f();
   private float r0 = 0.5F;
   private float r1 = 4.0F;
   private int life = 10;
   private double mx;
   private double my;
   private double mz;
   private float strength = 1.0F;
   private float band = 0.35F;
   private float rim = 0.25F;
   private int rgb = 15398655;
   private float twistCos = 1.0F;
   private float twistSin = 0.0F;

   private ShockRing(Vec3 c, Vec3 normal) {
      super(new VfxAnchor(-1, c), VfxParams.NONE);
      this.cx = c.x;
      this.cy = c.y;
      this.cz = c.z;
      if (normal.lengthSqr() > 1.0E-6) {
         this.n.set((float)normal.x, (float)normal.y, (float)normal.z).normalize();
      }

      if (Math.abs(this.n.y) < 0.95F) {
         this.u.set(this.n.z, 0.0F, -this.n.x).normalize();
      } else {
         this.u.set(1.0F, 0.0F, 0.0F);
      }

      this.n.cross(this.u, this.v).normalize();
   }

   public static ShockRing at(Vec3 c, Vec3 normal) {
      return new ShockRing(c, normal);
   }

   public ShockRing radius(float start, float end) {
      this.r0 = start;
      this.r1 = end;
      return this;
   }

   public ShockRing life(int ticks) {
      this.life = Math.max(2, ticks);
      return this;
   }

   public ShockRing move(Vec3 perTick) {
      this.mx = perTick.x;
      this.my = perTick.y;
      this.mz = perTick.z;
      return this;
   }

   public ShockRing strength(float s) {
      this.strength = s;
      return this;
   }

   public ShockRing band(float fraction) {
      this.band = fraction;
      return this;
   }

   public ShockRing rim(float alpha, int colour) {
      this.rim = alpha;
      this.rgb = colour;
      return this;
   }

   public ShockRing twist(float radians) {
      this.twistCos = Mth.cos(radians);
      this.twistSin = Mth.sin(radians);
      return this;
   }

   public ShockRing spawn() {
      VfxDirector.spawn(this);
      return this;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.age > this.life) {
         this.finish();
      }
   }

   @Override
   public void render(VfxFrame f) {
      float t = Math.min(1.0F, ((float)this.age + f.partial) / (float)this.life);
      float e = 1.0F - (1.0F - t) * (1.0F - t);
      float r = Mth.lerp(e, this.r0, this.r1);
      float fade = (1.0F - t) * Math.min(1.0F, ((float)this.age + f.partial) / 1.5F);
      if (!(fade <= 0.01F)) {
         float ox = f.rx(this.cx + this.mx * (double)((float)this.age + f.partial));
         float oy = f.ry(this.cy + this.my * (double)((float)this.age + f.partial));
         float oz = f.rz(this.cz + this.mz * (double)((float)this.age + f.partial));
         float w = Math.max(0.2F, r * this.band);
         BufferBuilder d = f.distortion();
         float lx = -f.left.x;
         float ly = -f.left.y;
         float lz = -f.left.z;
         if (d != null) {
            for (int k = 0; k < 36; k++) {
               float a0 = (float)k * (float) (Math.PI * 2) / 36.0F;
               float a1 = (float)(k + 1) * (float) (Math.PI * 2) / 36.0F;
               this.ring(d, ox, oy, oz, a0, r - w, w, 0.0F, lx, ly, lz, f, fade);
               this.ring(d, ox, oy, oz, a1, r - w, w, 0.0F, lx, ly, lz, f, fade);
               this.ring(d, ox, oy, oz, a1, r + w, w, 1.0F, lx, ly, lz, f, fade);
               this.ring(d, ox, oy, oz, a0, r + w, w, 1.0F, lx, ly, lz, f, fade);
            }
         }

         if (this.rim > 0.01F && f.allowMesh()) {
            MeshWriter.ring(
               f.mesh(MeshTex.FLAME_RIBBON),
               ox,
               oy,
               oz,
               this.u.x,
               this.u.y,
               this.u.z,
               this.v.x,
               this.v.y,
               this.v.z,
               r,
               Math.max(0.08F, w * 0.4F),
               36,
               ((float)this.age + f.partial) * 0.03F,
               3.0F,
               MeshWriter.argb(this.rgb, this.rim * fade),
               -1.0F,
               0,
               0.6F,
               0.0F
            );
         }
      }
   }

   private void ring(
      BufferBuilder d, float ox, float oy, float oz, float a, float rr, float w, float across, float lx, float ly, float lz, VfxFrame f, float fade
   ) {
      float c = Mth.cos(a);
      float s = Mth.sin(a);
      float dx = this.u.x * c + this.v.x * s;
      float dy = this.u.y * c + this.v.y * s;
      float dz = this.u.z * c + this.v.z * s;
      float sx = dx * lx + dy * ly + dz * lz;
      float sy = dx * f.up.x + dy * f.up.y + dz * f.up.z;
      float sl = Mth.sqrt(sx * sx + sy * sy) + 1.0E-4F;
      float bx = sx / sl;
      float by = sy / sl;
      MeshWriter.distortVertex(
         d,
         ox + dx * rr,
         oy + dy * rr,
         oz + dz * rr,
         bx * this.twistCos - by * this.twistSin,
         bx * this.twistSin + by * this.twistCos,
         this.strength * fade * 0.6F,
         across,
         3
      );
   }

   @Override
   public double x() {
      return this.cx;
   }

   @Override
   public double y() {
      return this.cy;
   }

   @Override
   public double z() {
      return this.cz;
   }

   public static final class Band {
      public void draw(
         VfxFrame f,
         float cx,
         float cy,
         float cz,
         float ux,
         float uy,
         float uz,
         float vx,
         float vy,
         float vz,
         float radius,
         float width,
         float strength,
         float twist
      ) {
         BufferBuilder d = f.distortion();
         if (d != null && !(strength <= 0.005F)) {
            float tc = Mth.cos(twist);
            float ts = Mth.sin(twist);
            float lx = -f.left.x;
            float ly = -f.left.y;
            float lz = -f.left.z;

            for (int k = 0; k < 36; k++) {
               for (int q = 0; q < 4; q++) {
                  float a = (float)(k + (q != 1 && q != 2 ? 0 : 1)) * (float) (Math.PI * 2) / 36.0F;
                  float across = q >= 2 ? 1.0F : 0.0F;
                  float rr = radius + (across * 2.0F - 1.0F) * width;
                  float c = Mth.cos(a);
                  float s = Mth.sin(a);
                  float dx = ux * c + vx * s;
                  float dy = uy * c + vy * s;
                  float dz = uz * c + vz * s;
                  float sx = dx * lx + dy * ly + dz * lz;
                  float sy = dx * f.up.x + dy * f.up.y + dz * f.up.z;
                  float sl = Mth.sqrt(sx * sx + sy * sy) + 1.0E-4F;
                  float bx = sx / sl;
                  float by = sy / sl;
                  MeshWriter.distortVertex(d, cx + dx * rr, cy + dy * rr, cz + dz * rr, bx * tc - by * ts, bx * ts + by * tc, strength, across, 3);
               }
            }
         }
      }
   }
}
