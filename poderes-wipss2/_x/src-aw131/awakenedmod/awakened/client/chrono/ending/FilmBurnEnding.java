package com.awakenedmod.awakened.client.chrono.ending;

import com.awakenedmod.awakened.client.chrono.ChronoFxClient;
import com.awakenedmod.awakened.client.chrono.ChronoTimeStop;
import com.awakenedmod.awakened.client.render.ChronoGlass;
import com.awakenedmod.awakened.client.render.CrackWeb;
import com.awakenedmod.awakened.content.chronokinetic.ChronoSounds;
import com.awakenedmod.awakened.core.Lab;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class FilmBurnEnding {
   static final Lab.Value TONE = Lab.CLIENT.def("chrono.stop.burn.tone", 3.0, 0.5, 15.0, 0.5);
   static final Lab.Value BLISTER = Lab.CLIENT.def("chrono.stop.burn.blister_from", 5.0, 0.0, 15.0, 0.5);
   static final Lab.Value OPEN = Lab.CLIENT.def("chrono.stop.burn.open_at", 9.0, 0.0, 15.0, 0.5);
   static final Lab.Value GONE = Lab.CLIENT.def("chrono.stop.burn.gone_at", 15.0, 1.0, 30.0, 0.5);
   static final Lab.Value EASE = Lab.CLIENT.def("chrono.stop.burn.ease", 1.6, 0.5, 4.0, 0.05);
   static final Lab.Value PRINT = Lab.CLIENT.def("chrono.stop.burn.print", 0.18, 0.0, 1.0, 0.01);
   static final Lab.Value PRINT_EDGE = Lab.CLIENT.def("chrono.stop.burn.print_edge", 0.16, 0.0, 1.0, 0.01);
   static final Lab.Value MOTTLE = Lab.CLIENT.def("chrono.stop.burn.mottle", 0.14, 0.0, 0.6, 0.01);
   static final Lab.Value CHAR = Lab.CLIENT.def("chrono.stop.burn.char", 0.4, 0.02, 3.0, 0.02);
   static final Lab.Value SCORCH = Lab.CLIENT.def("chrono.stop.burn.scorch", 1.0, 0.0, 6.0, 0.05);
   static final Lab.Value RIM = Lab.CLIENT.def("chrono.stop.burn.rim", 0.05, 0.005, 0.5, 0.005);
   static final Lab.Value RIM_ALPHA = Lab.CLIENT.def("chrono.stop.burn.rim_alpha", 0.9, 0.0, 1.0, 0.01);
   static final Lab.Value JAG = Lab.CLIENT.def("chrono.stop.burn.jag", 1.1, 0.0, 5.0, 0.05);
   static final Lab.Value FEATURE = Lab.CLIENT.def("chrono.stop.burn.feature", 1.3, 0.2, 10.0, 0.1);
   static final Lab.Value LOBE = Lab.CLIENT.def("chrono.stop.burn.lobe", 0.25, 0.0, 1.5, 0.01);
   static final Lab.Value BLISTERS = Lab.CLIENT.def("chrono.stop.burn.blisters", 3.0, 1.0, 6.0, 1.0);
   static final Lab.Value BLISTER_SIZE = Lab.CLIENT.def("chrono.stop.burn.blister_size", 1.9, 0.2, 6.0, 0.05);
   static final Lab.Value STAGGER = Lab.CLIENT.def("chrono.stop.burn.stagger", 0.1, 0.0, 1.0, 0.01);
   static final Lab.Value MESH_EDGE = Lab.CLIENT.def("chrono.stop.burn.mesh_edge", 0.6, 0.2, 4.0, 0.05);
   static final Lab.Value SCRAPS = Lab.CLIENT.def("chrono.stop.burn.scraps", 22.0, 0.0, 60.0, 1.0);
   static final Lab.Value SCRAP_LIFE = Lab.CLIENT.def("chrono.stop.burn.scrap_life", 9.0, 2.0, 40.0, 0.5);
   static final Lab.Value SCRAP_SIZE = Lab.CLIENT.def("chrono.stop.burn.scrap_size", 0.7, 0.05, 2.0, 0.01);
   static final Lab.Value SCRAP_FALL = Lab.CLIENT.def("chrono.stop.burn.scrap_fall", 0.09, 0.0, 1.0, 0.005);
   static final Lab.Value SCRAP_SWAY = Lab.CLIENT.def("chrono.stop.burn.scrap_sway", 0.35, 0.0, 3.0, 0.05);
   private static final int PRINT_RGB = 14268810;
   private static final int SCORCH_RGB = 8146470;
   private static final int CHAR_RGB = 2364939;
   private static final int RIM_RGB = 16755269;
   private static final float CHAR_ALPHA = 0.92F;
   private static final float SCORCH_ALPHA = 0.7F;
   private static final float VISIBLE_FROM = -0.05F;
   private static final Map<ChronoFxClient.Effect, FilmBurnEnding.Burn> BURNS = new WeakHashMap<>();
   private static final Map<ChronoFxClient.Effect, FilmBurnEnding.Pending> NOISE = new WeakHashMap<>();

   private FilmBurnEnding() {
   }

   public static void preloadLabKeys() {
      HeldImpactLook.tailTicks();
   }

   public static int tailTicks() {
      return Mth.ceil(Math.max(0.0F, GONE.getF() - OPEN.getF()) + SCRAP_LIFE.getF() * 1.3F) + 2;
   }

   static float clock(StopMoment m) {
      return !m.released() ? Math.max(0.0F, m.r()) : Math.max(m.r(), OPEN.getF()) + m.since();
   }

   private static float front(float c, float charU) {
      float open = OPEN.getF();
      float t = (c - open) / Math.max(0.5F, GONE.getF() - open);
      if (t < 0.0F) {
         return -1.0E9F;
      } else {
         return t >= 1.0F ? 1.0E9F : (float)Math.pow((double)t, (double)Math.max(0.1F, EASE.getF())) * (1.0F + charU);
      }
   }

   public static void world(StopDraw d, ChronoFxClient.Effect field) {
      StopMoment m = StopMoment.of(field, d.partialTick());
      float c = clock(m);
      Vec3 centre = d.rel(field.pos);
      float formed = ChronoTimeStop.dome(field.ended() ? (float)field.endAge() : field.age(d.partialTick()));
      if (!(formed <= 0.0F)) {
         FilmBurnEnding.Burn burn = burn(field);
         if (c < GONE.getF()) {
            float k = smooth(c / Math.max(0.5F, TONE.getF()));
            if (k < 1.0F) {
               ChronoGlass.dome(d.pose(), d.vc(), d.layer(), centre, field.radius, formed * (1.0F - k));
            }

            if (k > 0.0F) {
               film(d, burn, centre, field.radius, c, formed * k);
            }
         }

         scraps(d, burn, centre, field.radius, c);
      }
   }

   public static boolean photograph(StopMoment m) {
      return !m.released();
   }

   public static void released(ClientLevel level, ChronoFxClient.Effect field) {
      ChronoSounds.stopBurn().playLocal(level, field.pos, 1.0F, 1.0F);
   }

   private static void film(StopDraw d, FilmBurnEnding.Burn b, Vec3 centre, float radius, float c, float alpha) {
      ChronoGlass.Icosphere mesh = b.mesh;
      int nv = mesh.verts.length / 3;
      float charU = CHAR.getF() / b.span;
      float p = front(c, charU);
      float cx = (float)centre.x;
      float cy = (float)centre.y;
      float cz = (float)centre.z;
      FilmBurnEnding.Scratch sc = FilmBurnEnding.Scratch.of(nv);
      float scorchU = Math.max(1.0E-4F, SCORCH.getF() / b.span);
      float[] blisterDark = blisterDarkness(b, c);
      boolean body = d.layer() == ChronoGlass.Layer.BODY;

      for (int v = 0; v < nv; v++) {
         int i = v * 3;
         float nx = mesh.verts[i];
         float ny = mesh.verts[i + 1];
         float nz = mesh.verts[i + 2];
         float px = cx + nx * radius;
         float py = cy + ny * radius;
         float pz = cz + nz * radius;
         sc.pos[i] = px;
         sc.pos[i + 1] = py;
         sc.pos[i + 2] = pz;
         float gap = b.u[v] - p;
         sc.gap[v] = gap;
         if (body) {
            float ahead = Mth.clamp(1.0F - (gap - charU) / scorchU, 0.0F, 1.0F);
            float dark = ahead * ahead;

            for (int k = 0; k < b.blisters; k++) {
               float dist = b.blisterDist[k][v];
               float rad = blisterDark[k * 2 + 1];
               if (rad > 0.0F && dist < rad) {
                  float s = 1.0F - dist / rad;
                  dark = Math.max(dark, blisterDark[k * 2] * s * (2.0F - s));
               }
            }

            sc.dark[v] = dark;
            float pl = Mth.sqrt(px * px + py * py + pz * pz);
            float face = pl < 1.0E-5F ? 1.0F : Math.abs((px * nx + py * ny + pz * nz) / pl);
            float rimShare = 1.0F - face;
            sc.fres[v] = rimShare * rimShare;
            sc.color[v] = filmColor(alpha, dark, sc.fres[v], b.mottle[v], false);
         }
      }

      if (body) {
         FilmBurnEnding.Clipper clip = sc.clip;

         for (int f = 0; f < mesh.faces.length; f += 3) {
            int a = mesh.faces[f];
            int bb = mesh.faces[f + 1];
            int cc = mesh.faces[f + 2];
            float ga = sc.gap[a];
            float gb = sc.gap[bb];
            float gc = sc.gap[cc];
            float lo = Math.min(ga, Math.min(gb, gc));
            float hi = Math.max(ga, Math.max(gb, gc));
            if (!(hi < 0.0F)) {
               if (lo >= charU) {
                  point(d.pose(), d.vc(), sc, a);
                  point(d.pose(), d.vc(), sc, bb);
                  point(d.pose(), d.vc(), sc, cc);
               } else {
                  clip.load(sc, b, a, bb, cc);
                  clip.save();
                  if (clip.keep(charU, 1.0F)) {
                     clip.emit(d.pose(), d.vc(), alpha, false);
                  }

                  clip.restore();
                  if (clip.keep(0.0F, 1.0F) && clip.keep(charU, -1.0F)) {
                     clip.emit(d.pose(), d.vc(), alpha, true);
                  }
               }
            }
         }
      } else {
         float width = RIM.getF();
         int rimColor = argb(alpha * RIM_ALPHA.getF(), 16755269);
         float[] q = new float[6];

         for (int fx = 0; fx < mesh.faces.length; fx += 3) {
            int n = 0;

            for (int e = 0; e < 3 && n < 2; e++) {
               int i = mesh.faces[fx + e];
               int j = mesh.faces[fx + (e + 1) % 3];
               float gi = sc.gap[i];
               float gj = sc.gap[j];
               if (gi < 0.0F != gj < 0.0F) {
                  float t = gi / (gi - gj);
                  q[n * 3] = Mth.lerp(t, sc.pos[i * 3], sc.pos[j * 3]);
                  q[n * 3 + 1] = Mth.lerp(t, sc.pos[i * 3 + 1], sc.pos[j * 3 + 1]);
                  q[n * 3 + 2] = Mth.lerp(t, sc.pos[i * 3 + 2], sc.pos[j * 3 + 2]);
                  n++;
               }
            }

            if (n == 2) {
               ChronoGlass.billboard(d.pose(), d.vc(), q[0], q[1], q[2], q[3], q[4], q[5], width, rimColor, true);
            }
         }
      }
   }

   private static void point(Pose pose, VertexConsumer vc, FilmBurnEnding.Scratch sc, int v) {
      vc.addVertex(pose, sc.pos[v * 3], sc.pos[v * 3 + 1], sc.pos[v * 3 + 2]).setColor(sc.color[v]);
   }

   private static float[] blisterDarkness(FilmBurnEnding.Burn b, float c) {
      float[] out = new float[b.blisters * 2];
      float from = BLISTER.getF();
      float span = Math.max(0.5F, OPEN.getF() - from);
      float size = BLISTER_SIZE.getF();

      for (int k = 0; k < b.blisters; k++) {
         float g = Mth.clamp((c - from - (float)k * 0.15F * span) / span, 0.0F, 1.0F);
         out[k * 2] = Mth.sqrt(g);
         out[k * 2 + 1] = g <= 0.0F ? 0.0F : size * b.blisterSize[k] * (0.4F + 0.6F * g);
      }

      return out;
   }

   private static int filmColor(float alpha, float dark, float fres, float mottle, boolean inChar) {
      if (inChar) {
         float shade = 0.85F + 0.3F * mottle;
         return argb(alpha * 0.92F, scale(2364939, shade));
      } else {
         float print = PRINT.getF() + PRINT_EDGE.getF() * fres;
         float tone = 1.0F + MOTTLE.getF() * (mottle - 0.5F) * 2.0F;
         int base = scale(14268810, tone);
         int rgb;
         if (dark <= 0.0F) {
            rgb = base;
         } else if (dark < 0.6F) {
            rgb = mix(base, 8146470, dark / 0.6F);
         } else {
            rgb = mix(8146470, 2364939, (dark - 0.6F) / 0.4F);
         }

         float a = Mth.lerp((float)Math.pow((double)dark, 0.7), print, dark < 0.6F ? 0.7F : 0.92F);
         return argb(alpha * a, rgb);
      }
   }

   private static void scraps(StopDraw d, FilmBurnEnding.Burn b, Vec3 centre, float radius, float c) {
      float life = SCRAP_LIFE.getF();
      float open = OPEN.getF();
      float window = Math.max(0.5F, GONE.getF() - open);
      float ease = Math.max(0.1F, EASE.getF());
      float fall = SCRAP_FALL.getF();
      float sway = SCRAP_SWAY.getF();
      float size = SCRAP_SIZE.getF() * Math.max(0.6F, Mth.sqrt(radius / 9.0F));
      boolean body = d.layer() == ChronoGlass.Layer.BODY;
      float[] pts = new float[12];

      for (int k = 0; k < b.scrapCount; k++) {
         int o = k * 8;
         float u = b.scraps[o + 3];
         float born = open + window * (float)Math.pow((double)Mth.clamp(u, 0.0F, 1.0F), (double)(1.0F / ease));
         float myLife = life * (0.75F + 0.5F * b.scraps[o + 4]);
         float t = c - born;
         if (!(t < 0.0F) && !(t > myLife)) {
            float fade = Mth.clamp((myLife - t) / Math.max(1.0F, myLife * 0.35F), 0.0F, 1.0F);
            float drop = fall * (t - 3.0F * (1.0F - (float)Math.exp((double)(-t / 3.0F))));
            float phase = b.scraps[o + 5] * (float) (Math.PI * 2);
            float out = 0.04F * t;
            float swayNow = sway * Mth.sin(0.45F * t + phase) * Mth.clamp(t / 3.0F, 0.0F, 1.0F);
            float dx = b.scraps[o];
            float dy = b.scraps[o + 1];
            float dz = b.scraps[o + 2];
            float hx = -dz;
            float hz = dx;
            float hl = Mth.sqrt(hx * hx + dx * dx);
            if (hl < 1.0E-4F) {
               hx = 1.0F;
               hz = 0.0F;
               hl = 1.0F;
            }

            hx /= hl;
            hz /= hl;
            float x = (float)centre.x + dx * (radius + out) + hx * swayNow;
            float y = (float)centre.y + dy * (radius + out) - drop;
            float z = (float)centre.z + dz * (radius + out) + hz * swayNow;
            float s = size * (0.6F + 0.6F * b.scraps[o + 6]);
            float spin = b.scraps[o + 7] * (float) (Math.PI * 2) + (0.25F + 0.35F * b.scraps[o + 4]) * t;
            float tilt = 0.6F * Mth.sin(0.45F * t + phase + 1.3F);
            float ax = Mth.cos(spin);
            float az = Mth.sin(spin);
            float ay = 0.3F * Mth.sin(spin * 0.7F + phase);
            float bx = -az * Mth.cos(tilt);
            float by = Mth.sin(tilt);
            float bz = ax * Mth.cos(tilt);
            float nx = ay * bz - az * by;
            float ny = az * bx - ax * bz;
            float nz = ax * by - ay * bx;
            float curl = 0.55F * s;
            int color = body ? argb(fade * 0.9F, 2364939) : argb(fade * RIM_ALPHA.getF() * Mth.clamp(1.0F - t / 2.5F, 0.0F, 1.0F), 16755269);
            if (body || !(t > 2.5F)) {
               for (int seg = 0; seg < 3; seg++) {
                  float l0 = (float)seg / 3.0F - 0.5F;
                  float l1 = (float)(seg + 1) / 3.0F - 0.5F;
                  float w0 = s * 0.28F * (0.7F + 0.3F * Mth.cos(l0 * 2.4F + phase));
                  float w1 = s * 0.28F * (0.7F + 0.3F * Mth.cos(l1 * 2.4F + phase));
                  float c0 = curl * l0 * l0;
                  float c1 = curl * l1 * l1;

                  for (int e = 0; e < 2; e++) {
                     float l = e == 0 ? l0 : l1;
                     float cu = e == 0 ? c0 : c1;
                     pts[e * 3] = x + ax * l * s + nx * cu;
                     pts[e * 3 + 1] = y + ay * l * s + ny * cu;
                     pts[e * 3 + 2] = z + az * l * s + nz * cu;
                  }

                  if (body) {
                     quadAcross(d.pose(), d.vc(), pts, bx, by, bz, w0, w1, color);
                  } else {
                     ChronoGlass.billboard(
                        d.pose(),
                        d.vc(),
                        pts[0] + bx * w0,
                        pts[1] + by * w0,
                        pts[2] + bz * w0,
                        pts[3] + bx * w1,
                        pts[4] + by * w1,
                        pts[5] + bz * w1,
                        RIM.getF() * 0.8F,
                        color,
                        true
                     );
                  }
               }
            }
         }
      }
   }

   private static void quadAcross(Pose pose, VertexConsumer vc, float[] p, float bx, float by, float bz, float w0, float w1, int color) {
      float ax0 = p[0] - bx * w0;
      float ay0 = p[1] - by * w0;
      float az0 = p[2] - bz * w0;
      float ax1 = p[0] + bx * w0;
      float ay1 = p[1] + by * w0;
      float az1 = p[2] + bz * w0;
      float bx0 = p[3] - bx * w1;
      float by0 = p[4] - by * w1;
      float bz0 = p[5] - bz * w1;
      float bx1 = p[3] + bx * w1;
      float by1 = p[4] + by * w1;
      float bz1 = p[5] + bz * w1;
      vc.addVertex(pose, ax0, ay0, az0).setColor(color);
      vc.addVertex(pose, ax1, ay1, az1).setColor(color);
      vc.addVertex(pose, bx1, by1, bz1).setColor(color);
      vc.addVertex(pose, ax0, ay0, az0).setColor(color);
      vc.addVertex(pose, bx1, by1, bz1).setColor(color);
      vc.addVertex(pose, bx0, by0, bz0).setColor(color);
   }

   private static FilmBurnEnding.Burn burn(ChronoFxClient.Effect field) {
      int detail = detailFor(field.radius);
      long stamp = stamp(detail);
      Vec3 from = field.burnFrom();
      FilmBurnEnding.Burn b = BURNS.get(field);
      if (b == null || b.from != from || b.stamp != stamp) {
         b = new FilmBurnEnding.Burn(field, from, stamp, pending(field, detail, stamp).noise().join());
         BURNS.put(field, b);
      }

      return b;
   }

   public static void prepare(ChronoFxClient.Effect field) {
      int detail = detailFor(field.radius);
      pending(field, detail, stamp(detail));
   }

   private static FilmBurnEnding.Pending pending(ChronoFxClient.Effect field, int detail, long stamp) {
      FilmBurnEnding.Pending p = NOISE.get(field);
      if (p == null || p.stamp() != stamp) {
         int seed = field.seed;
         float feature = Math.max(1.0F, field.radius) / Math.max(0.2F, FEATURE.getF());
         p = new FilmBurnEnding.Pending(stamp, CompletableFuture.supplyAsync(() -> new FilmBurnEnding.Noise(ChronoGlass.icosphere(detail), seed, feature)));
         NOISE.put(field, p);
      }

      return p;
   }

   private static float rag(float x, float y, float z, float feature, int seed) {
      return fbm(x * feature, y * feature, z * feature, seed + 7);
   }

   private static float lobe(float x, float y, float z, int seed) {
      return fbm(x * 1.7F, y * 1.7F, z * 1.7F, seed + 3);
   }

   private static float angle(float[] b, float x, float y, float z) {
      return (float)Math.acos((double)Mth.clamp(x * b[0] + y * b[1] + z * b[2], -1.0F, 1.0F));
   }

   private static int detailFor(float radius) {
      float level = (float)(Math.log((double)(1.1F * radius / Math.max(0.1F, MESH_EDGE.getF()))) / Math.log(2.0));
      return Mth.clamp(Math.round(level), 3, 5);
   }

   private static long stamp(int detail) {
      long h = (long)detail;

      for (Lab.Value v : new Lab.Value[]{JAG, FEATURE, LOBE, BLISTERS, BLISTER_SIZE, STAGGER, SCRAPS}) {
         h = h * 31L + Double.doubleToLongBits(v.get());
      }

      return h;
   }

   private static float smooth(float t) {
      float s = Mth.clamp(t, 0.0F, 1.0F);
      return s * s * (3.0F - 2.0F * s);
   }

   private static float hash01(int h) {
      return CrackWeb.hash01(h);
   }

   static float fbm(float x, float y, float z, int seed) {
      float sum = 0.0F;
      float amp = 0.5F;
      float norm = 0.0F;

      for (int o = 0; o < 3; o++) {
         sum += amp * (noise(x, y, z, seed + o * 1013) * 2.0F - 1.0F);
         norm += amp;
         x *= 2.03F;
         y *= 2.03F;
         z *= 2.03F;
         amp *= 0.5F;
      }

      return sum / norm;
   }

   private static float noise(float x, float y, float z, int seed) {
      int xi = Mth.floor(x);
      int yi = Mth.floor(y);
      int zi = Mth.floor(z);
      float fx = x - (float)xi;
      float fy = y - (float)yi;
      float fz = z - (float)zi;
      fx = fx * fx * (3.0F - 2.0F * fx);
      fy = fy * fy * (3.0F - 2.0F * fy);
      fz = fz * fz * (3.0F - 2.0F * fz);
      float c000 = lattice(xi, yi, zi, seed);
      float c100 = lattice(xi + 1, yi, zi, seed);
      float c010 = lattice(xi, yi + 1, zi, seed);
      float c110 = lattice(xi + 1, yi + 1, zi, seed);
      float c001 = lattice(xi, yi, zi + 1, seed);
      float c101 = lattice(xi + 1, yi, zi + 1, seed);
      float c011 = lattice(xi, yi + 1, zi + 1, seed);
      float c111 = lattice(xi + 1, yi + 1, zi + 1, seed);
      float x00 = Mth.lerp(fx, c000, c100);
      float x10 = Mth.lerp(fx, c010, c110);
      float x01 = Mth.lerp(fx, c001, c101);
      float x11 = Mth.lerp(fx, c011, c111);
      return Mth.lerp(fz, Mth.lerp(fy, x00, x10), Mth.lerp(fy, x01, x11));
   }

   private static float lattice(int x, int y, int z, int seed) {
      int h = x * 374761393 + y * 668265263 + z * 1274126177 + seed * 144665;
      h = (h ^ h >>> 13) * 1274126177;
      h ^= h >>> 16;
      return (float)(h & 16777215) / 1.6777216E7F;
   }

   private static int argb(float alpha, int rgb) {
      int a = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
      return a << 24 | rgb & 16777215;
   }

   private static int scale(int rgb, float k) {
      int r = Mth.clamp(Math.round((float)(rgb >> 16 & 0xFF) * k), 0, 255);
      int g = Mth.clamp(Math.round((float)(rgb >> 8 & 0xFF) * k), 0, 255);
      int b = Mth.clamp(Math.round((float)(rgb & 0xFF) * k), 0, 255);
      return r << 16 | g << 8 | b;
   }

   private static int mix(int a, int b, float t) {
      float s = Mth.clamp(t, 0.0F, 1.0F);
      int r = Math.round(Mth.lerp(s, (float)(a >> 16 & 0xFF), (float)(b >> 16 & 0xFF)));
      int g = Math.round(Mth.lerp(s, (float)(a >> 8 & 0xFF), (float)(b >> 8 & 0xFF)));
      int bl = Math.round(Mth.lerp(s, (float)(a & 0xFF), (float)(b & 0xFF)));
      return r << 16 | g << 8 | bl;
   }

   private static final class Burn {
      static final int SCRAP_STRIDE = 8;
      final ChronoGlass.Icosphere mesh;
      final Vec3 from;
      final long stamp;
      final float[] u;
      final float[] mottle;
      final float span;
      final int blisters;
      final float[][] blisterDist;
      final float[] blisterSize;
      final float[] scraps;
      final int scrapCount;

      Burn(ChronoFxClient.Effect field, Vec3 from, long stamp, FilmBurnEnding.Noise noise) {
         this.mesh = noise.mesh;
         this.from = from;
         this.stamp = stamp;
         int seed = field.seed;
         float radius = Math.max(1.0F, field.radius);
         this.blisters = FilmBurnEnding.BLISTERS.getI();
         float[][] dirs = blisterDirs(seed, from, this.blisters);
         this.blisterSize = new float[this.blisters];

         for (int k = 0; k < this.blisters; k++) {
            this.blisterSize[k] = k == 0 ? 1.0F : 0.7F + 0.4F * FilmBurnEnding.hash01(seed * 97 + k * 13);
         }

         float jag = FilmBurnEnding.JAG.getF() / radius;
         float feature = radius / Math.max(0.2F, FilmBurnEnding.FEATURE.getF());
         float lobe = FilmBurnEnding.LOBE.getF();
         float stagger = FilmBurnEnding.STAGGER.getF();
         int nv = this.mesh.verts.length / 3;
         this.u = new float[nv];
         this.mottle = noise.mottle;
         this.blisterDist = new float[this.blisters][nv];
         float min = Float.MAX_VALUE;
         float max = -Float.MAX_VALUE;

         for (int v = 0; v < nv; v++) {
            float x = this.mesh.verts[v * 3];
            float y = this.mesh.verts[v * 3 + 1];
            float z = this.mesh.verts[v * 3 + 2];
            float rag = noise.rag[v];
            float near = Float.MAX_VALUE;

            for (int k = 0; k < this.blisters; k++) {
               float ang = FilmBurnEnding.angle(dirs[k], x, y, z);
               near = Math.min(near, (float)k * stagger + ang);
               this.blisterDist[k][v] = Math.max(0.0F, ang * radius + 0.5F * FilmBurnEnding.JAG.getF() * rag);
            }

            float burnAt = near + lobe * noise.lobe[v] + jag * rag;
            this.u[v] = burnAt;
            if (y >= -0.05F) {
               min = Math.min(min, burnAt);
               max = Math.max(max, burnAt);
            }
         }

         float range = Math.max(1.0E-4F, max - min);

         for (int v = 0; v < nv; v++) {
            this.u[v] = Math.min(1.0F, (this.u[v] - min) / range);
         }

         this.span = radius * range;
         int want = FilmBurnEnding.SCRAPS.getI();
         float[] out = new float[want * 8];
         int count = 0;

         for (int tries = 0; tries < want * 6 && count < want; tries++) {
            int h = seed * 7919 + tries * 104729;
            float yy = 0.1F + 0.85F * FilmBurnEnding.hash01(h);
            float az = (float) (Math.PI * 2) * FilmBurnEnding.hash01(h + 1);
            float ring = Mth.sqrt(1.0F - yy * yy);
            float x = ring * Mth.cos(az);
            float z = ring * Mth.sin(az);
            float near = Float.MAX_VALUE;

            for (int k = 0; k < this.blisters; k++) {
               near = Math.min(near, (float)k * stagger + FilmBurnEnding.angle(dirs[k], x, yy, z));
            }

            float at = near + lobe * FilmBurnEnding.lobe(x, yy, z, seed) + jag * FilmBurnEnding.rag(x, yy, z, feature, seed);
            float uu = Mth.clamp((at - min) / range, 0.0F, 1.0F);
            if (!(uu < 0.25F) || !(FilmBurnEnding.hash01(h + 2) > 0.25F)) {
               int o = count * 8;
               out[o] = x;
               out[o + 1] = yy;
               out[o + 2] = z;
               out[o + 3] = uu;
               out[o + 4] = FilmBurnEnding.hash01(h + 3);
               out[o + 5] = FilmBurnEnding.hash01(h + 4);
               out[o + 6] = FilmBurnEnding.hash01(h + 5);
               out[o + 7] = FilmBurnEnding.hash01(h + 6);
               count++;
            }
         }

         this.scraps = out;
         this.scrapCount = count;
      }

      private static float[][] blisterDirs(int seed, Vec3 from, int count) {
         float[][] out = new float[count][];
         Vec3 first = from == null ? new Vec3(0.0, 0.4, 1.0) : from;
         float fy = Mth.clamp((float)first.y, 0.15F, 0.8F);
         float fa = (float)Math.atan2(first.z, first.x);
         out[0] = dir(fa, fy);

         for (int k = 1; k < count; k++) {
            float a = fa + (float) (Math.PI * 2) * (float)k / (float)count + (FilmBurnEnding.hash01(seed * 31 + k) - 0.5F) * 0.9F;
            float y = 0.15F + 0.6F * FilmBurnEnding.hash01(seed * 53 + k * 7);
            out[k] = dir(a, y);
         }

         return out;
      }

      private static float[] dir(float azimuth, float y) {
         float ring = Mth.sqrt(Math.max(0.0F, 1.0F - y * y));
         return new float[]{ring * Mth.cos(azimuth), y, ring * Mth.sin(azimuth)};
      }
   }

   private static final class Clipper {
      private static final int ATTR = 7;
      private static final int MAX = 9;
      private float[] poly = new float[63];
      private float[] next = new float[63];
      private final float[] saved = new float[63];
      private int n;
      private int savedN;

      void load(FilmBurnEnding.Scratch sc, FilmBurnEnding.Burn b, int a, int bb, int c) {
         this.n = 0;
         this.put(sc, b, a);
         this.put(sc, b, bb);
         this.put(sc, b, c);
      }

      private void put(FilmBurnEnding.Scratch sc, FilmBurnEnding.Burn b, int v) {
         int o = this.n * 7;
         this.poly[o] = sc.pos[v * 3];
         this.poly[o + 1] = sc.pos[v * 3 + 1];
         this.poly[o + 2] = sc.pos[v * 3 + 2];
         this.poly[o + 3] = sc.gap[v];
         this.poly[o + 4] = sc.dark[v];
         this.poly[o + 5] = sc.fres[v];
         this.poly[o + 6] = b.mottle[v];
         this.n++;
      }

      void save() {
         System.arraycopy(this.poly, 0, this.saved, 0, this.n * 7);
         this.savedN = this.n;
      }

      void restore() {
         System.arraycopy(this.saved, 0, this.poly, 0, this.savedN * 7);
         this.n = this.savedN;
      }

      boolean keep(float level, float sign) {
         int m = 0;

         for (int i = 0; i < this.n; i++) {
            int j = (i + 1) % this.n;
            float di = sign * (this.poly[i * 7 + 3] - level);
            float dj = sign * (this.poly[j * 7 + 3] - level);
            if (di >= 0.0F) {
               System.arraycopy(this.poly, i * 7, this.next, m * 7, 7);
               m++;
            }

            if (di >= 0.0F != dj >= 0.0F && m < 9) {
               float t = di / (di - dj);

               for (int k = 0; k < 7; k++) {
                  this.next[m * 7 + k] = Mth.lerp(t, this.poly[i * 7 + k], this.poly[j * 7 + k]);
               }

               m++;
            }
         }

         float[] swap = this.poly;
         this.poly = this.next;
         this.next = swap;
         this.n = m;
         return this.n >= 3;
      }

      void emit(Pose pose, VertexConsumer vc, float alpha, boolean inChar) {
         for (int i = 1; i + 1 < this.n; i++) {
            this.vertex(pose, vc, 0, alpha, inChar);
            this.vertex(pose, vc, i, alpha, inChar);
            this.vertex(pose, vc, i + 1, alpha, inChar);
         }
      }

      private void vertex(Pose pose, VertexConsumer vc, int i, float alpha, boolean inChar) {
         int o = i * 7;
         int color = FilmBurnEnding.filmColor(alpha, this.poly[o + 4], this.poly[o + 5], this.poly[o + 6], inChar);
         vc.addVertex(pose, this.poly[o], this.poly[o + 1], this.poly[o + 2]).setColor(color);
      }
   }

   private static final class Noise {
      final ChronoGlass.Icosphere mesh;
      final float[] rag;
      final float[] lobe;
      final float[] mottle;

      Noise(ChronoGlass.Icosphere mesh, int seed, float feature) {
         this.mesh = mesh;
         int nv = mesh.verts.length / 3;
         this.rag = new float[nv];
         this.lobe = new float[nv];
         this.mottle = new float[nv];

         for (int v = 0; v < nv; v++) {
            float x = mesh.verts[v * 3];
            float y = mesh.verts[v * 3 + 1];
            float z = mesh.verts[v * 3 + 2];
            this.rag[v] = FilmBurnEnding.rag(x, y, z, feature, seed);
            this.lobe[v] = FilmBurnEnding.lobe(x, y, z, seed);
            this.mottle[v] = 0.5F + 0.5F * FilmBurnEnding.fbm(x * 3.0F + 5.0F, y * 3.0F, z * 3.0F, seed + 11);
         }
      }
   }

   private static record Pending(long stamp, CompletableFuture<FilmBurnEnding.Noise> noise) {
   }

   private static final class Scratch {
      private static FilmBurnEnding.Scratch shared;
      float[] pos;
      float[] gap;
      float[] dark;
      float[] fres;
      int[] color;
      final FilmBurnEnding.Clipper clip = new FilmBurnEnding.Clipper();

      static FilmBurnEnding.Scratch of(int points) {
         if (shared == null || shared.gap.length < points) {
            shared = new FilmBurnEnding.Scratch();
            shared.pos = new float[points * 3];
            shared.gap = new float[points];
            shared.dark = new float[points];
            shared.fres = new float[points];
            shared.color = new int[points];
         }

         return shared;
      }
   }
}
