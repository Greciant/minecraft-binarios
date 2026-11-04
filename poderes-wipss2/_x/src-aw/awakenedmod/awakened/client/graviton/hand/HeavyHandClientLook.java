package com.awakenedmod.awakened.client.graviton.hand;

import com.awakenedmod.awakened.client.graviton.GravitonDraw;
import com.awakenedmod.awakened.client.render.SteeredParticle;
import com.awakenedmod.awakened.content.graviton.HeavyHandBodyVisual;
import com.awakenedmod.awakened.content.graviton.HeavyHandFx;
import com.awakenedmod.awakened.core.Fx;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

final class HeavyHandClientLook {
   static final float SNAP_TICKS = 12.0F;
   private static final int WARM = 16747069;
   private static final int PALE = 16769732;
   private static final int DARK = 1971474;
   private static final int HOT = 16774888;
   private static final int EMBER = 12731932;
   private static final int ARCLETS = 10;
   private static final int LOOP_SEGS = 40;
   private static final float TAUT = 0.85F;
   private final Map<Integer, HeavyHandClientLook.Built> built = new HashMap<>();
   private final Map<Integer, Vec3> holdFloor = new HashMap<>();
   private final Map<Integer, Long> lastShed = new HashMap<>();
   @Nullable
   private ClientLevel memoryLevel;

   private static double springG(double x) {
      double c = Mth.clamp(x, 0.0, 1.0);
      return Math.cos((Math.PI * 3) * c) * Math.exp(-3.2 * c) * (1.0 - c);
   }

   private static void springWell(HeavyHandSheet.Shape sh, double amp, double x) {
      double g = springG(x);
      if (g >= 0.0) {
         sh.lip = amp * g;
      } else {
         sh.bump = -amp * g * 1.4;
         sh.bumpR = sh.lipR * 0.8;
      }
   }

   private static void springDome(HeavyHandSheet.Shape sh, double amp, double x) {
      double g = springG(x);
      if (g >= 0.0) {
         sh.bump = amp * g;
      } else {
         sh.lip = -amp * g * 0.9;
      }
   }

   private static double lipRadius(double w) {
      return 0.9 + 0.3 * w;
   }

   private static double reach(double w) {
      return 2.4 + 0.4 * w;
   }

   private static boolean marked(HeavyHandRenderer.Frame fr, @Nullable LivingEntity e, int phase) {
      if (e != null && e.hasData(HeavyHandBodyVisual.TYPE)) {
         HeavyHandBodyVisual bv = HeavyHandBodyVisual.of(e);
         return bv.phase() == phase && bv.remaining((double)((float)fr.level().getGameTime() + fr.pt())) > 0.0F;
      } else {
         return false;
      }
   }

   private static double radius(double h, double w) {
      return Math.max(0.5 * h + 0.75, 0.6 * w + 0.8);
   }

   @Nullable
   private HeavyHandSheet.Shape gripShape(HeavyHandRenderer.Frame fr, HeavyHandRenderer.Grip g) {
      LivingEntity t = g.target();
      double h = t != null ? (double)t.getBbHeight() : 1.8;
      double w = t != null ? (double)t.getBbWidth() : 0.6;
      Vec3 b = g.b();
      double feetY = b.y - h * 0.5;
      double floor = fr.groundBelow(new Vec3(b.x, feetY + 0.3, b.z), 12);
      if (Double.isNaN(floor)) {
         return null;
      } else {
         double air = 1.0 / (1.0 + Math.max(0.0, feetY - floor - 0.3) / 3.0);
         int id = g.caster().getId();
         HeavyHandSheet.Shape sh = new HeavyHandSheet.Shape(b.x, b.z, floor);
         sh.t = fr.t();
         sh.reach = reach(w);
         sh.lipR = lipRadius(w);
         sh.bumpR = 0.75 + 0.25 * w;
         float s = g.snap();
         if (s < 0.0F) {
            float f = g.f();
            boolean taut = f > 0.85F;
            if (g.up()) {
               sh.bump = (0.2 + 0.5 * (double)f + (taut ? 0.08 : 0.0)) * air;
            } else {
               sh.lip = (0.15 + 0.35 * (double)f + (taut ? 0.06 : 0.0)) * air;
               sh.dark = 0.4 + 0.3 * (double)f;
               sh.crack = (0.1 + 0.35 * (double)f) * air;
            }

            sh.pinch = (0.12 + 0.25 * (double)f) * air;
            sh.idle = taut ? 0.0 : 0.025 * air;
            this.holdFloor.put(id, new Vec3(b.x, floor, b.z));
            return sh;
         } else {
            Vec3 start = this.holdFloor.getOrDefault(id, new Vec3(b.x, floor, b.z));
            double lastLip = (0.15 + 0.35 * (double)g.f()) * air;
            double lastBump = (0.2 + 0.5 * (double)g.f()) * air;
            switch (g.snapKind()) {
               case 0:
                  slideFrom(sh, start, b, g.caster().position(), s, true, lastLip);
                  break;
               case 1:
                  if (marked(fr, t, 4)) {
                     return null;
                  }

                  slideFrom(sh, start, b, g.caster().position(), s, false, lastLip);
                  break;
               case 2:
                  if (marked(fr, t, 1) || marked(fr, t, 3)) {
                     return null;
                  }

                  sh.dark = 0.65;
                  sh.crack = 0.8 * (double)(1.0F - s);
                  if (s < 0.17F) {
                     sh.lip = Mth.lerp((double)s / 0.17, lastLip, 1.0);
                  } else {
                     springWell(sh, 1.0, ((double)s - 0.17) / 0.83);
                  }

                  sh.pinch = 0.4 * (double)(1.0F - s);
                  break;
               case 3:
                  if (marked(fr, t, 2)) {
                     return null;
                  }

                  springDome(sh, lastBump, (double)s);
                  break;
               default:
                  if (g.up()) {
                     springDome(sh, lastBump, (double)s);
                  } else {
                     springWell(sh, lastLip, (double)s);
                  }

                  sh.pinch = 0.2 * (double)(1.0F - s);
            }

            return sh;
         }
      }
   }

   private static void slideFrom(HeavyHandSheet.Shape sh, Vec3 start, Vec3 body, Vec3 caster, float s, boolean yank, double lastLip) {
      Vec3 flat = yank ? caster.subtract(start) : start.subtract(caster);
      flat = new Vec3(flat.x, 0.0, flat.z);
      if (flat.lengthSqr() < 1.0E-6) {
         flat = new Vec3(1.0, 0.0, 0.0);
      }

      flat = flat.normalize();
      double e = (double)ease(Math.min(1.0F, s * 1.6F));
      Vec3 bodyFlat = new Vec3(body.x, start.y, body.z);
      double run = Math.min(bodyFlat.subtract(start).length(), yank ? 6.0 : 3.0);
      Vec3 c = start.add(flat.scale(run * e * (yank ? 0.8 : 1.0)));
      sh.cx = c.x;
      sh.cz = c.z;
      sh.dirX = flat.x;
      sh.dirZ = flat.z;
      double left = Math.pow((double)(1.0F - s), 1.3);
      sh.lip = Math.max(lastLip, 0.35) * left;
      sh.dark = 0.3;
      sh.skew = 0.7;
      sh.pinch = 0.2 * left;
      if (yank) {
         sh.wake = 0.26 * left;
         sh.wakePhase = (double)(s * 12.0F) * 1.3;
         sh.reach++;
      }
   }

   @Nullable
   private static HeavyHandSheet.Shape bodyShape(HeavyHandRenderer.Frame fr, HeavyHandRenderer.Body bd) {
      LivingEntity e = bd.body();
      Vec3 feet = bd.feet();
      double w = (double)e.getBbWidth();
      float rem = bd.remaining();
      float age = bd.age();
      double floor = fr.groundBelow(feet.add(0.0, 0.3, 0.0), bd.phase() == 1 ? 4 : 40);
      if (Double.isNaN(floor)) {
         return null;
      } else {
         double high = Math.max(0.0, feet.y - floor);
         HeavyHandSheet.Shape sh = new HeavyHandSheet.Shape(feet.x, feet.z, floor);
         sh.t = fr.t();
         sh.reach = reach(w);
         double lipR = lipRadius(w);
         sh.lipR = lipR;
         sh.bumpR = 0.75 + 0.25 * w;
         switch (bd.phase()) {
            case 1:
               sh.lipR = lipR * (0.65 + 0.35 * (double)rem);
               double depth = 0.2 + 0.55 * (double)rem;
               if (age < 2.0F) {
                  sh.lip = Mth.lerp((double)(age / 2.0F), 0.5, 1.0);
               } else if (age < 5.0F) {
                  sh.lip = Mth.lerp((double)((age - 2.0F) / 3.0F), 1.0, depth);
               } else {
                  sh.lip = depth;
               }

               sh.crack = 0.4 + 0.6 * (double)rem;
               sh.dark = 0.55 + 0.2 * (double)rem;
               sh.pinch = 0.15 + 0.3 * (double)rem;
               if (rem < 0.2F) {
                  double x = 1.0 - (double)rem / 0.2;
                  sh.lip = 0.0;
                  springWell(sh, 0.31000000000000005, x);
                  sh.pinch *= 1.0 - x;
                  sh.crack *= 1.0 - x;
               }
               break;
            case 2:
               sh.coneR = 1.4 + 0.2 * w;
               double chase = Math.min(high, 3.2);
               if (age < 8.0F) {
                  sh.cone = chase * Math.min(1.0, (double)age / 1.0);
                  sh.pinch = 0.35;
               } else if (age < 15.0F) {
                  double x = (double)((age - 8.0F) / 7.0F);
                  double g = springG(x);
                  if (g >= 0.0) {
                     sh.cone = 3.2 * g;
                  } else {
                     sh.lip = -g * 0.5;
                  }

                  sh.pinch = 0.35 * (1.0 - x);
               } else {
                  double kx = Mth.clamp(1.0 - high / 10.0, 0.0, 1.0);
                  sh.lip = 0.1 * kx * kx;
                  sh.dark = 0.3;
                  sh.pinch = 0.1 * kx;
               }
               break;
            case 3:
               double k = Mth.clamp(1.0 - high / 8.0, 0.0, 1.0);
               sh.lip = 0.15 + 0.4 * k;
               sh.dark = 0.35;
               sh.pinch = 0.2 + 0.15 * k;
               sh.idle = 0.02;
               break;
            case 4:
               if (high > 0.6) {
                  return null;
               }

               Vec3 move = new Vec3(e.getX() - e.xOld, 0.0, e.getZ() - e.zOld);
               if (move.lengthSqr() > 1.0E-4) {
                  Vec3 dir = move.normalize();
                  sh.dirX = dir.x;
                  sh.dirZ = dir.z;
                  sh.skew = 0.7;
                  sh.wake = 0.05 * (double)rem;
                  sh.wakePhase = (double)(-age) * 1.5;
               }

               sh.lip = 0.45 * (double)rem;
               sh.dark = 0.3;
               sh.pinch = 0.2 * (double)rem;
               break;
            default:
               return null;
         }

         return sh;
      }
   }

   void gripOwn(HeavyHandRenderer.Frame fr, HeavyHandRenderer.Grip g) {
      this.forgetOnLevelChange(fr);
      int key = g.caster().getId();
      HeavyHandSheet.Shape sh = this.gripShape(fr, g);
      HeavyHandSheet.Mesh mesh = sh == null ? null : HeavyHandSheet.build(fr, sh);
      if (mesh != null) {
         HeavyHandSheet.draw(fr, mesh);
         float seams = g.holding() ? 0.22F + 0.3F * g.f() + (g.f() > 0.85F ? 0.18F : 0.0F) : 0.4F * (1.0F - g.snap());
         this.built.put(key, new HeavyHandClientLook.Built(fr.t(), mesh, seams));
      } else {
         this.built.remove(key);
      }

      LivingEntity t = g.target();
      if (t != null) {
         float s = g.snap();
         float k;
         if (s < 0.0F) {
            k = (float)smooth((double)g.f()) * (g.up() ? 1.0F : -1.0F);
         } else if (g.snapKind() == 2) {
            k = -1.25F * (1.0F - s);
         } else if (g.snapKind() == 3) {
            k = 1.25F;
         } else {
            k = (g.up() ? 1.0F : -1.0F) * (float)smooth((double)g.f()) * (1.0F - s) * (1.0F - s);
         }

         HeavyHandTidal.mark(t, k, fr.t());
         if (s < 0.0F && mesh != null && this.newTick(fr, key) && fr.level().getGameTime() % 2L == 0L) {
            chipIn(fr, sh, g.up());
         }
      }
   }

   void bodyOwn(HeavyHandRenderer.Frame fr, HeavyHandRenderer.Body bd) {
      this.forgetOnLevelChange(fr);
      LivingEntity e = bd.body();
      int key = -1 - e.getId();
      HeavyHandSheet.Shape sh = bodyShape(fr, bd);
      HeavyHandSheet.Mesh mesh = sh == null ? null : HeavyHandSheet.build(fr, sh);
      float rem = bd.remaining();
      if (mesh != null) {
         HeavyHandSheet.draw(fr, mesh);

         float seams = switch (bd.phase()) {
            case 1 -> 0.3F + 0.35F * rem;
            case 4 -> 0.4F * rem;
            default -> 0.4F;
         };
         this.built.put(key, new HeavyHandClientLook.Built(fr.t(), mesh, seams));
      } else {
         this.built.remove(key);
      }

      float age = bd.age();

      float k = switch (bd.phase()) {
         case 1 -> age < 3.0F ? -1.25F : -(0.4F + 0.6F * rem);
         case 2 -> age < 8.0F ? 1.2F : 0.5F;
         case 3 -> 0.7F;
         default -> -0.5F * rem;
      };
      HeavyHandTidal.mark(e, k, fr.t());
      if (this.newTick(fr, key)) {
         RandomSource rand = fr.level().random;
         if (bd.phase() == 1 && sh != null && mesh != null && fr.level().getGameTime() % 3L == 0L) {
            chipIn(fr, sh, false);
         } else if (bd.phase() == 3) {
            Vec3 top = bd.feet().add(0.0, (double)e.getBbHeight() + 0.4, 0.0);
            Vec3 from = top.add((rand.nextDouble() - 0.5) * 1.4, 0.3 + rand.nextDouble() * 0.8, (rand.nextDouble() - 0.5) * 1.4);
            particle(fr, Fx.dust(16747069, 0.8F), from, new Vec3(0.0, -0.08, 0.0));
         }
      }
   }

   private static void chipIn(HeavyHandRenderer.Frame fr, HeavyHandSheet.Shape sh, boolean lift) {
      BlockState floor = fr.level().getBlockState(BlockPos.containing(sh.cx, sh.floor - 0.01, sh.cz));
      if (!floor.isAir()) {
         RandomSource rand = fr.level().random;
         double ang = rand.nextDouble() * Math.PI * 2.0;
         double rad = sh.reach * (0.7 + 0.2 * rand.nextDouble());
         double vy = lift ? 0.26 : 0.14;
         double air = 2.0 * vy / 0.04;
         double speed = rad / air * 1.1;
         particle(
            fr,
            new BlockParticleOption(ParticleTypes.BLOCK, HeavyHandFx.chip(floor)),
            new Vec3(sh.cx + Math.cos(ang) * rad, sh.floor + 0.15, sh.cz + Math.sin(ang) * rad),
            new Vec3(-Math.cos(ang) * speed, vy, -Math.sin(ang) * speed)
         );
      }
   }

   void grip(HeavyHandRenderer.Frame fr, HeavyHandRenderer.Grip g, VertexConsumer vc, boolean glow) {
      HeavyHandClientLook.Built b = this.built.get(g.caster().getId());
      if (!glow && b != null && b.t() == fr.t()) {
         HeavyHandSheet.seams(fr, vc, b.mesh(), b.seams(), 1971474, g.ownerView());
      }

      if (!g.victimView()) {
         LivingEntity t = g.target();
         double h = t != null ? (double)t.getBbHeight() : 1.8;
         double width = t != null ? (double)t.getBbWidth() : 0.6;
         double r = radius(h, width) * 0.85;
         Vec3 c = g.b();
         float s = g.snap();
         float light = glowScale(fr, c);
         if (s < 0.0F) {
            float f = g.f();
            boolean taut = f > 0.85F;
            arclets(fr, vc, glow, g.ownerView(), c, r, g.up() ? -1 : 1, 0.35F + 0.45F * f, light);
            hairline(fr, vc, glow, g.ownerView(), c, r, g.up(), taut ? 0.85F : 0.1F + 0.3F * f, taut, 0.0F, light);
            if (b != null && b.t() == fr.t()) {
               double feetY = c.y - h * 0.5;
               if (feetY - b.mesh().s.floor > 1.2) {
                  seamCone(fr, vc, glow, new Vec3(c.x, feetY, c.z), b.mesh().s, 0.5F * f + 0.15F, light);
               }
            }
         } else {
            float ticks = s * 12.0F;
            if ((g.snapKind() == 2 || g.snapKind() == 3) && ticks < 3.0F) {
               float k = 1.0F - ticks / 3.0F;
               double rr = r * (g.snapKind() == 2 ? 1.0 - 0.5 * (double)(1.0F - k) : 1.0 + 0.3 * (double)(1.0F - k));
               hairline(fr, vc, glow, g.ownerView(), c, rr, g.snapKind() == 3, k, true, k, light);
            }
         }
      }
   }

   void body(HeavyHandRenderer.Frame fr, HeavyHandRenderer.Body bd, VertexConsumer vc, boolean glow) {
      LivingEntity e = bd.body();
      HeavyHandClientLook.Built b = this.built.get(-1 - e.getId());
      if (!glow && b != null && b.t() == fr.t()) {
         HeavyHandSheet.seams(fr, vc, b.mesh(), b.seams(), 1971474, false);
      }

      if (!bd.victimView()) {
         double h = (double)e.getBbHeight();
         double r = radius(h, (double)e.getBbWidth()) * 0.85;
         Vec3 c = bd.feet().add(0.0, h * 0.5, 0.0);
         float light = glowScale(fr, c);
         switch (bd.phase()) {
            case 2:
               arclets(fr, vc, glow, false, c, r, -1, 0.5F, light);
               break;
            case 3:
               arclets(fr, vc, glow, false, c, r, 1, 0.6F, light);
               if (b != null && b.t() == fr.t()) {
                  seamCone(fr, vc, glow, bd.feet(), b.mesh().s, 0.55F, light);
               }
         }
      }
   }

   private static float glowScale(HeavyHandRenderer.Frame fr, Vec3 at) {
      return 0.45F + 0.55F * (float)fr.level().getMaxLocalRawBrightness(BlockPos.containing(at)) / 15.0F;
   }

   private static void hairline(
      HeavyHandRenderer.Frame fr,
      VertexConsumer vc,
      boolean glow,
      boolean own,
      Vec3 c,
      double r,
      boolean lift,
      float alpha,
      boolean taut,
      float flash,
      float light
   ) {
      if (!(alpha <= 0.01F)) {
         Vec3[] basis = facing(fr.cam(), c);
         double ru = r * (lift ? 0.9 : 1.08);
         double rv = r * (lift ? 1.2 : 0.88);
         Vec3[] pts = loop(c, basis[0], basis[1], ru, rv, taut ? 0.0 : 0.03, fr.t(), 2.1F);
         double far = far(fr, c);
         if (glow) {
            int rgb = flash > 0.5F ? 16774888 : (taut ? 16747069 : 12731932);
            drawLoop(fr, vc, pts, own, (0.01 + 0.012 * (double)flash) * far, alpha * light * (taut ? 0.55F : 0.3F), 0.2F, rgb, true);
         } else {
            Vec3[] in = loop(c, basis[0], basis[1], ru - 0.03 * far, rv - 0.03 * far, taut ? 0.0 : 0.03, fr.t(), 2.1F);
            drawLoop(fr, vc, in, own, 0.012 * far, alpha * 0.45F, 0.3F, 1971474, false);
         }
      }
   }

   private static void arclets(HeavyHandRenderer.Frame fr, VertexConsumer vc, boolean glow, boolean own, Vec3 c, double r, int gather, float alpha, float light) {
      Vec3[] basis = facing(fr.cam(), c);
      Vec3 u = basis[0];
      Vec3 v = basis[1];
      double far = far(fr, c);
      float t = fr.t();
      int seg = 8;
      double span = Math.toRadians(25.0);

      for (int k = 0; k < 10; k++) {
         double mid = (Math.PI * 2) * (double)k / 10.0 + (double)t * 0.012 + (double)(k % 2) * 0.2;
         double below = -Math.sin(mid);
         float a = alpha * (float)Mth.clamp(0.35 + 0.65 * (double)gather * below, 0.1, 1.0);
         if (!(a <= 0.02F)) {
            double rad = r * (1.05 + 0.15 * (double)(k * 7 % 3) / 2.0);
            Vec3[] pts = new Vec3[seg + 1];
            float[] fade = new float[seg + 1];

            for (int i = 0; i <= seg; i++) {
               double th = mid - span / 2.0 + span * (double)i / (double)seg;
               pts[i] = c.add(u.scale(Math.cos(th) * rad)).add(v.scale(Math.sin(th) * rad));
               fade[i] = (float)Math.sin(Math.PI * (double)i / (double)seg) * fr.nearFade(pts[i], own);
            }

            if (glow) {
               GravitonDraw.ribbon(fr.pose(), vc, pts, fade, fr.cam(), 0.0, 0.016 * far, 0.45F * a * light, 0.0F, 16769732, 16769732, true);
            } else {
               GravitonDraw.ribbon(fr.pose(), vc, pts, fade, fr.cam(), 0.0, 0.024 * far, 0.5F * a, 0.0F, 1971474, 1971474, false);
            }
         }
      }
   }

   private static void seamCone(HeavyHandRenderer.Frame fr, VertexConsumer vc, boolean glow, Vec3 top, HeavyHandSheet.Shape sh, float alpha, float light) {
      double floor = sh.floor + 0.02;
      double far = far(fr, top);
      int lines = 6;
      int seg = 12;

      for (int k = 0; k < lines; k++) {
         double a = (Math.PI * 2) * (double)k / (double)lines + (double)fr.t() * 0.01;
         Vec3 foot = new Vec3(top.x + Math.cos(a) * sh.lipR * 1.2, floor, top.z + Math.sin(a) * sh.lipR * 1.2);
         Vec3 ctrl = new Vec3(Mth.lerp(0.7, top.x, foot.x), floor + 0.15, Mth.lerp(0.7, top.z, foot.z));
         Vec3[] pts = new Vec3[seg + 1];
         float[] fade = new float[seg + 1];

         for (int i = 0; i <= seg; i++) {
            double uu = (double)i / (double)seg;
            pts[i] = bezier(foot, ctrl, top, uu);
            fade[i] = (float)Math.sin(Math.PI * Math.min(1.0, uu * 0.9 + 0.1)) * fr.nearFade(pts[i], false);
         }

         if (glow) {
            GravitonDraw.ribbon(fr.pose(), vc, pts, fade, fr.cam(), 0.0, 0.012 * far, 0.35F * alpha * light, 0.0F, 12731932, 12731932, true);
         } else {
            GravitonDraw.ribbon(fr.pose(), vc, pts, fade, fr.cam(), 0.0, 0.014 * far, 0.45F * alpha, 0.0F, 1971474, 1971474, false);
         }
      }
   }

   private void forgetOnLevelChange(HeavyHandRenderer.Frame fr) {
      if (this.memoryLevel != fr.level()) {
         this.built.clear();
         this.holdFloor.clear();
         this.lastShed.clear();
         this.memoryLevel = fr.level();
      }
   }

   private boolean newTick(HeavyHandRenderer.Frame fr, int key) {
      long now = fr.level().getGameTime();
      Long last = this.lastShed.put(key, now);
      if (this.lastShed.size() > 256) {
         this.lastShed.clear();
      }

      return last == null || last != now;
   }

   private static void particle(HeavyHandRenderer.Frame fr, ParticleOptions options, Vec3 at, Vec3 v) {
      SteeredParticle.spawn(options, at, v);
   }

   private static Vec3[] facing(Vec3 cam, Vec3 c) {
      Vec3 toCam = cam.subtract(c);
      toCam = toCam.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : toCam.normalize();
      Vec3 up = new Vec3(0.0, 1.0, 0.0).subtract(toCam.scale(toCam.y));
      if (up.lengthSqr() < 0.01) {
         up = new Vec3(0.0, 0.0, 1.0).subtract(toCam.scale(toCam.z));
      }

      up = up.normalize();
      Vec3 right = up.cross(toCam).normalize();
      return new Vec3[]{right, up};
   }

   private static Vec3[] loop(Vec3 c, Vec3 u, Vec3 v, double ru, double rv, double wob, float t, float seed) {
      Vec3[] p = new Vec3[40];

      for (int i = 0; i < 40; i++) {
         double th = (Math.PI * 2) * (double)i / 40.0;
         double k = 1.0 + wob * (0.6 * Math.sin(3.0 * th + (double)t * 0.35 + (double)seed) + 0.4 * Math.sin(5.0 * th - (double)t * 0.52 + (double)seed * 1.7));
         p[i] = c.add(u.scale(Math.cos(th) * ru * k)).add(v.scale(Math.sin(th) * rv * k));
      }

      return p;
   }

   private static void drawLoop(
      HeavyHandRenderer.Frame fr, VertexConsumer vc, Vec3[] pts, boolean own, double half, float alpha, float edge, int rgb, boolean glow
   ) {
      float[] fade = new float[pts.length];
      boolean any = false;

      for (int i = 0; i < pts.length; i++) {
         fade[i] = fr.nearFade(pts[i], own);
         any |= fade[i] > 0.01F;
      }

      if (any) {
         GravitonDraw.ribbonLoop(fr.pose(), vc, pts, fade, fr.cam(), half, alpha, edge, rgb, glow);
      }
   }

   private static double far(HeavyHandRenderer.Frame fr, Vec3 p) {
      return Mth.clamp(Math.sqrt(fr.cam().distanceToSqr(p)) / 8.0, 1.0, 2.5);
   }

   private static Vec3 bezier(Vec3 a, Vec3 c, Vec3 b, double u) {
      double m = 1.0 - u;
      return a.scale(m * m).add(c.scale(2.0 * m * u)).add(b.scale(u * u));
   }

   private static double smooth(double x) {
      double c = Mth.clamp(x, 0.0, 1.0);
      return c * c * (3.0 - 2.0 * c);
   }

   private static float ease(float x) {
      float c = Mth.clamp(x, 0.0F, 1.0F);
      return 1.0F - (1.0F - c) * (1.0F - c);
   }

   private static record Built(float t, HeavyHandSheet.Mesh mesh, float seams) {
   }
}
