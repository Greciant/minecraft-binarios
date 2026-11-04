package com.awakenedmod.awakened.client.graviton.hand;

import com.awakenedmod.awakened.client.graviton.GravitonDraw;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

final class HeavyHandSheet {
   static final int SUB = 4;
   private static final double LIFT = 0.012;
   private static final double SUN_X = 0.35;
   private static final double SUN_Y = 0.9;
   private static final double SUN_Z = 0.25;
   private static final double SUN_LEN = Math.sqrt(0.995);
   private static final double FLAT_LIT = 0.9 / SUN_LEN;
   private static final double SLOPE_GAIN = 3.4;
   private static final int MIN_BLOCK_LIGHT = 3;
   private static final RandomSource RAND = RandomSource.create(42L);
   private static final Map<BlockState, HeavyHandSheet.Top> TOPS = new HashMap<>();
   private static long topsAt;

   private HeavyHandSheet() {
   }

   @Nullable
   static HeavyHandSheet.Mesh build(HeavyHandRenderer.Frame fr, HeavyHandSheet.Shape s) {
      if (s.visible() && !Double.isNaN(s.floor)) {
         int i0 = Mth.floor((s.cx - s.reach) * 4.0);
         int j0 = Mth.floor((s.cz - s.reach) * 4.0);
         int n = Mth.ceil(s.reach * 2.0 * 4.0) + 1;
         int bx0 = Math.floorDiv(i0, 4);
         int bz0 = Math.floorDiv(j0, 4);
         int nb = Math.floorDiv(i0 + n, 4) - bx0 + 1;
         HeavyHandSheet.Mesh m = new HeavyHandSheet.Mesh(s, i0, j0, n, bx0, bz0, Math.max(nb, Math.floorDiv(j0 + n, 4) - bz0 + 1));
         boolean any = columns(fr, m);
         if (!any) {
            return null;
         } else {
            double e = 0.05;

            for (int a = 0; a <= n; a++) {
               for (int b = 0; b <= n; b++) {
                  double wx = (double)(i0 + a) / 4.0;
                  double wz = (double)(j0 + b) / 4.0;
                  double dx = wx - s.cx;
                  double dz = wz - s.cz;
                  double d0 = Math.sqrt(dx * dx + dz * dz);
                  double d = s.dist(dx, dz);
                  double k = s.pull(d);
                  double h = s.height(dx, dz);
                  double hx = s.height(dx + e, dz) - s.height(dx - e, dz);
                  double hz = s.height(dx, dz + e) - s.height(dx, dz - e);
                  double nx = -hx / (2.0 * e);
                  double nz = -hz / (2.0 * e);
                  double lit = (nx * 0.35 + 0.9 + nz * 0.25) / (SUN_LEN * Math.sqrt(nx * nx + 1.0 + nz * nz));
                  double shade = Mth.clamp(1.0 + 3.4 * (lit - FLAT_LIT), 0.35, 1.2) * s.shadeOfDepth(d);
                  int idx = m.at(a, b);
                  m.x[idx] = s.cx + dx * (1.0 - k);
                  m.z[idx] = s.cz + dz * (1.0 - k);
                  m.y[idx] = s.floor + 0.012 + h;
                  m.shade[idx] = (float)shade;
                  m.fade[idx] = s.alpha * (float)(1.0 - smooth((d0 - s.reach * 0.72) / (s.reach * 0.28)));
               }
            }

            return m;
         }
      } else {
         return null;
      }
   }

   private static boolean columns(HeavyHandRenderer.Frame fr, HeavyHandSheet.Mesh m) {
      ClientLevel level = fr.level();
      long now = level.getGameTime();
      if (now - topsAt > 200L || now < topsAt || TOPS.size() > 64) {
         TOPS.clear();
         topsAt = now;
      }

      Minecraft mc = fr.mc();
      HeavyHandSheet.Shape s = m.s;
      boolean any = false;
      MutableBlockPos pos = new MutableBlockPos();

      for (int a = 0; a < m.nb; a++) {
         for (int b = 0; b < m.nb; b++) {
            int bx = m.bx0 + a;
            int bz = m.bz0 + b;
            double cx = (double)bx + 0.5 - s.cx;
            double cz = (double)bz + 0.5 - s.cz;
            if (!(Math.sqrt(cx * cx + cz * cz) > s.reach + 0.75)) {
               double top = fr.groundBelow(new Vec3((double)bx + 0.5, s.floor + 1.2, (double)bz + 0.5), 3);
               if (!Double.isNaN(top) && !(Math.abs(top - s.floor) > 0.02)) {
                  pos.set(bx, Mth.floor(s.floor - 0.01), bz);
                  BlockState state = level.getBlockState(pos);
                  if (!state.isAir() && state.getFluidState().isEmpty()) {
                     HeavyHandSheet.Top face = TOPS.computeIfAbsent(state, HeavyHandSheet::top);
                     int tint = 16777215;
                     if (face.tintIndex() >= 0) {
                        tint = mc.getBlockColors().getColor(state, level, pos, face.tintIndex()) & 16777215;
                     }

                     BlockPos above = pos.above();
                     int packed = LevelRenderer.getLightColor(level, above);
                     int light = LightTexture.pack(Math.max(LightTexture.block(packed), 3), LightTexture.sky(packed));
                     m.cols[a * m.nb + b] = new HeavyHandSheet.Col(face.sprite(), tint, light);
                     any = true;
                  }
               }
            }
         }
      }

      return any;
   }

   private static HeavyHandSheet.Top top(BlockState state) {
      BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
      Iterator var2 = model.getQuads(state, Direction.UP, RAND).iterator();
      if (var2.hasNext()) {
         BakedQuad q = (BakedQuad)var2.next();
         return new HeavyHandSheet.Top(q.getSprite(), q.getTintIndex());
      } else {
         for (BakedQuad q : model.getQuads(state, null, RAND)) {
            if (q.getDirection() == Direction.UP) {
               return new HeavyHandSheet.Top(q.getSprite(), q.getTintIndex());
            }
         }

         return new HeavyHandSheet.Top(model.getParticleIcon(), -1);
      }
   }

   private static RenderType layer() {
      return RenderType.translucentMovingBlock();
   }

   static void draw(HeavyHandRenderer.Frame fr, HeavyHandSheet.Mesh m) {
      RenderType layer = layer();
      VertexConsumer vc = fr.buffers().getBuffer(layer);
      Pose pose = fr.pose();

      for (int a = 0; a < m.n; a++) {
         for (int b = 0; b < m.n; b++) {
            int ci = m.i0 + a;
            int cj = m.j0 + b;
            int bx = Math.floorDiv(ci, 4);
            int bz = Math.floorDiv(cj, 4);
            HeavyHandSheet.Col col = m.col(bx, bz);
            if (col != null) {
               int v00 = m.at(a, b);
               int v01 = m.at(a, b + 1);
               int v11 = m.at(a + 1, b + 1);
               int v10 = m.at(a + 1, b);
               if (!(m.fade[v00] + m.fade[v01] + m.fade[v11] + m.fade[v10] < 0.02F)) {
                  float u0 = (float)(ci - bx * 4) / 4.0F;
                  float u1 = u0 + 0.25F;
                  float w0 = (float)(cj - bz * 4) / 4.0F;
                  float w1 = w0 + 0.25F;
                  TextureAtlasSprite sp = col.sprite();
                  vertex(pose, vc, m, v00, sp.getU(u0), sp.getV(w0), col);
                  vertex(pose, vc, m, v01, sp.getU(u0), sp.getV(w1), col);
                  vertex(pose, vc, m, v11, sp.getU(u1), sp.getV(w1), col);
                  vertex(pose, vc, m, v10, sp.getU(u1), sp.getV(w0), col);
               }
            }
         }
      }

      fr.buffers().endBatch(layer);
      cracks(fr, m);
   }

   private static void vertex(Pose pose, VertexConsumer vc, HeavyHandSheet.Mesh m, int v, float u, float w, HeavyHandSheet.Col col) {
      float k = m.shade[v];
      int r = Math.min(255, Math.round((float)(col.tint() >> 16 & 0xFF) * k));
      int g = Math.min(255, Math.round((float)(col.tint() >> 8 & 0xFF) * k));
      int bl = Math.min(255, Math.round((float)(col.tint() & 0xFF) * k));
      int a = Mth.clamp(Math.round(m.fade[v] * 255.0F), 0, 255);
      vc.addVertex(pose, (float)m.x[v], (float)m.y[v], (float)m.z[v]).setColor(r, g, bl, a).setUv(u, w).setLight(col.light()).setNormal(pose, 0.0F, 1.0F, 0.0F);
   }

   private static int stage(HeavyHandSheet.Shape s, int bx, int bz) {
      if (s.crack <= 0.05) {
         return -1;
      } else {
         double dx = (double)bx + 0.5 - s.cx;
         double dz = (double)bz + 0.5 - s.cz;
         double d = Math.sqrt(dx * dx + dz * dz);
         double fall = d < 0.75 ? 1.0 : Math.max(0.0, 1.0 - (d - 0.75) / 1.1);
         return Math.min(9, (int)Math.round(s.crack * 10.0 * fall) - 1);
      }
   }

   private static void cracks(HeavyHandRenderer.Frame fr, HeavyHandSheet.Mesh m) {
      if (!(m.s.crack <= 0.05)) {
         Pose pose = fr.pose();

         for (int st = 0; st < 10; st++) {
            RenderType type = (RenderType)ModelBakery.DESTROY_TYPES.get(st);
            VertexConsumer vc = null;

            for (int a = 0; a < m.n; a++) {
               for (int b = 0; b < m.n; b++) {
                  int ci = m.i0 + a;
                  int cj = m.j0 + b;
                  int bx = Math.floorDiv(ci, 4);
                  int bz = Math.floorDiv(cj, 4);
                  HeavyHandSheet.Col col = m.col(bx, bz);
                  if (col != null && stage(m.s, bx, bz) == st) {
                     int v00 = m.at(a, b);
                     int v01 = m.at(a, b + 1);
                     int v11 = m.at(a + 1, b + 1);
                     int v10 = m.at(a + 1, b);
                     if (!(m.fade[v00] + m.fade[v01] + m.fade[v11] + m.fade[v10] < 0.4F)) {
                        if (vc == null) {
                           vc = fr.buffers().getBuffer(type);
                        }

                        float u0 = (float)(ci - bx * 4) / 4.0F;
                        float u1 = u0 + 0.25F;
                        float w0 = (float)(cj - bz * 4) / 4.0F;
                        float w1 = w0 + 0.25F;
                        crackVertex(pose, vc, m, v00, u0, w0, col);
                        crackVertex(pose, vc, m, v01, u0, w1, col);
                        crackVertex(pose, vc, m, v11, u1, w1, col);
                        crackVertex(pose, vc, m, v10, u1, w0, col);
                     }
                  }
               }
            }

            if (vc != null) {
               fr.buffers().endBatch(type);
            }
         }
      }
   }

   private static void crackVertex(Pose pose, VertexConsumer vc, HeavyHandSheet.Mesh m, int v, float u, float w, HeavyHandSheet.Col col) {
      vc.addVertex(pose, (float)m.x[v], (float)m.y[v] + 0.002F, (float)m.z[v])
         .setColor(255, 255, 255, 255)
         .setUv(u, w)
         .setLight(col.light())
         .setNormal(pose, 0.0F, 1.0F, 0.0F);
   }

   static void seams(HeavyHandRenderer.Frame fr, VertexConsumer vc, HeavyHandSheet.Mesh m, float alpha, int rgb, boolean own) {
      if (!(alpha <= 0.01F)) {
         double far = Mth.clamp(Math.sqrt(fr.cam().distanceToSqr(m.s.cx, m.s.floor, m.s.cz)) / 8.0, 1.0, 2.5);
         double half = 0.011 * far;

         for (int dir = 0; dir < 2; dir++) {
            for (int line = 0; line <= m.n; line++) {
               int g = (dir == 0 ? m.i0 : m.j0) + line;
               if (Math.floorMod(g, 4) == 0) {
                  Vec3[] pts = new Vec3[m.n + 1];
                  float[] fade = new float[m.n + 1];
                  boolean any = false;

                  for (int k = 0; k <= m.n; k++) {
                     int v = dir == 0 ? m.at(line, k) : m.at(k, line);
                     Vec3 p = new Vec3(m.x[v], m.y[v] + 0.006, m.z[v]);
                     pts[k] = p;
                     int other = (dir == 0 ? m.j0 : m.i0) + Math.min(k, m.n - 1);
                     int ob = Math.floorDiv(other, 4);
                     int gb = Math.floorDiv(g, 4);
                     boolean on = dir == 0 ? m.col(gb, ob) != null || m.col(gb - 1, ob) != null : m.col(ob, gb) != null || m.col(ob, gb - 1) != null;
                     fade[k] = on ? m.fade[v] * fr.nearFade(p, own) : 0.0F;
                     any |= fade[k] > 0.01F;
                  }

                  if (any) {
                     GravitonDraw.ribbon(fr.pose(), vc, pts, fade, fr.cam(), 0.0, half, alpha, 0.0F, rgb, rgb, false);
                  }
               }
            }
         }
      }
   }

   static double smooth(double x) {
      double c = Mth.clamp(x, 0.0, 1.0);
      return c * c * (3.0 - 2.0 * c);
   }

   private static record Col(TextureAtlasSprite sprite, int tint, int light) {
   }

   static final class Mesh {
      final HeavyHandSheet.Shape s;
      final int i0;
      final int j0;
      final int n;
      final double[] x;
      final double[] y;
      final double[] z;
      final float[] shade;
      final float[] fade;
      final HeavyHandSheet.Col[] cols;
      final int bx0;
      final int bz0;
      final int nb;

      private Mesh(HeavyHandSheet.Shape s, int i0, int j0, int n, int bx0, int bz0, int nb) {
         this.s = s;
         this.i0 = i0;
         this.j0 = j0;
         this.n = n;
         int v = (n + 1) * (n + 1);
         this.x = new double[v];
         this.y = new double[v];
         this.z = new double[v];
         this.shade = new float[v];
         this.fade = new float[v];
         this.bx0 = bx0;
         this.bz0 = bz0;
         this.nb = nb;
         this.cols = new HeavyHandSheet.Col[nb * nb];
      }

      int at(int a, int b) {
         return a * (this.n + 1) + b;
      }

      @Nullable
      HeavyHandSheet.Col col(int bx, int bz) {
         int a = bx - this.bx0;
         int b = bz - this.bz0;
         return a >= 0 && b >= 0 && a < this.nb && b < this.nb ? this.cols[a * this.nb + b] : null;
      }
   }

   static final class Shape {
      double cx;
      double cz;
      double floor;
      double reach = 2.6;
      double lip;
      double lipR = 1.0;
      double bump;
      double bumpR = 0.9;
      double cone;
      double coneR = 1.2;
      double pinch;
      double dark;
      double crack;
      double idle;
      double dirX;
      double dirZ;
      double skew;
      double wake;
      double wakeLen = 1.1;
      double wakePhase;
      float t;
      float alpha = 1.0F;

      Shape(double cx, double cz, double floor) {
         this.cx = cx;
         this.cz = cz;
         this.floor = floor;
      }

      boolean visible() {
         return this.alpha > 0.01F
            && (this.lip > 0.004 || this.bump > 0.004 || this.cone > 0.004 || this.pinch > 0.01 || this.crack > 0.05 || this.wake > 0.004);
      }

      double dist(double dx, double dz) {
         if (this.skew == 0.0) {
            return Math.sqrt(dx * dx + dz * dz);
         } else {
            double along = dx * this.dirX + dz * this.dirZ;
            double across = -dx * this.dirZ + dz * this.dirX;
            along *= along > 0.0 ? 1.0 + this.skew : 1.0 / (1.0 + 0.6 * this.skew);
            return Math.sqrt(along * along + across * across);
         }
      }

      double lipProfile(double d) {
         if (d < this.lipR) {
            return HeavyHandSheet.smooth(d / this.lipR);
         } else {
            double outer = Math.max(this.lipR + 0.4, this.reach * 0.95);
            return 1.0 - HeavyHandSheet.smooth((d - this.lipR) / (outer - this.lipR));
         }
      }

      double height(double dx, double dz) {
         double d = this.dist(dx, dz);
         double h = this.lip * this.lipProfile(d);
         if (this.bump != 0.0) {
            h += this.bump * Math.exp(-(d * d) / (this.bumpR * this.bumpR));
         }

         if (this.cone > 0.0) {
            double c = Math.max(0.0, 1.0 - d / this.coneR);
            h += this.cone * Math.pow(c, 1.8);
         }

         if (this.idle > 0.0) {
            h += this.idle * (0.5 + 0.5 * Math.sin(d * 4.5 + (double)this.t * 0.45)) * this.lipProfile(d);
         }

         if (this.wake > 0.0 && this.skew >= 0.0) {
            double along = dx * this.dirX + dz * this.dirZ;
            double across = -dx * this.dirZ + dz * this.dirX;
            if (along < 0.0) {
               double behind = -along;
               double env = Math.exp(-(across * across) / 0.8) * Math.max(0.0, 1.0 - behind / (this.wakeLen * 3.2));
               double s = Math.sin(behind * Math.PI / this.wakeLen + this.wakePhase);
               h += this.wake * env * s * s;
            }
         }

         return Math.max(0.0, h);
      }

      double pull(double d) {
         double sigma = Math.max(this.lipR, Math.max(this.cone > 0.0 ? this.coneR : 0.0, this.bump > 0.0 ? this.bumpR : 0.0)) * 1.05;
         return this.pinch * Math.exp(-(d * d) / (sigma * sigma));
      }

      double shadeOfDepth(double d) {
         return !(this.dark <= 0.0) && !(this.lip <= 0.004) && !(d >= this.lipR)
            ? 1.0 - this.dark * Math.min(1.0, this.lip / 0.4) * Math.pow(1.0 - d / this.lipR, 0.6)
            : 1.0;
      }
   }

   private static record Top(TextureAtlasSprite sprite, int tintIndex) {
   }
}
