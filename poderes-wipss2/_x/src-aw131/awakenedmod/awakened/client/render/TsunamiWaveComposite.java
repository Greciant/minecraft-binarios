package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.hydro.TsunamiAbility;
import com.awakenedmod.awakened.content.hydro.TsunamiFx;
import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.Lab;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class TsunamiWaveComposite {
   private static final Lab.Value CUBE = Lab.CLIENT.def("hydro.tsunami.wave_cube", 1.0, 0.3, 2.5, 0.05);
   private static final Lab.Value CHURN = Lab.CLIENT.def("hydro.tsunami.wave_churn", 0.12, 0.0, 1.0, 0.01);
   private static final Lab.Value BOIL = Lab.CLIENT.def("hydro.tsunami.wave_boil", 0.6, 0.0, 3.0, 0.05);
   private static final Lab.Value BOIL_RATE = Lab.CLIENT.def("hydro.tsunami.wave_boil_rate", 0.3, 0.0, 1.5, 0.01);
   private static final Lab.Value CURL = Lab.CLIENT.def("hydro.tsunami.wave_curl", 0.4, 0.0, 1.0, 0.01);
   private static final Lab.Value LIP_RATE = Lab.CLIENT.def("hydro.tsunami.wave_lip_rate", 0.6, 0.0, 4.0, 0.05);
   private static final Lab.Value CRASH = Lab.CLIENT.def("hydro.tsunami.wave_crash", 4.0, 0.0, 12.0, 1.0);
   private static final Lab.Value FLOOD_DEPTH = Lab.CLIENT.def("hydro.tsunami.flood_depth", 0.35, 0.02, 1.5, 0.01);
   private static final Lab.Value FOAM = Lab.CLIENT.def("hydro.tsunami.wave_foam", 0.6, 0.0, 1.5, 0.05);
   private static final float CATCH_UP = 6.0F;
   private static final float LEAN = 3.0F;
   private static final float LEAN_DEG = 8.0F;
   private static final float REAR = 6.0F;
   private static final float THROW = 8.0F;
   private static final float PEEL_LEAD = 3.0F;
   private static final float CUBE_HEIGHT = 14.0F;
   private static final float CUBE_MIN = 0.4F;
   private static final float CUBE_MAX = 2.0F;
   private static final float FACE = 0.3F;
   private static final float BACK = 0.6F;
   private static final float PLATEAU = 1.5F;
   private static final float SLUMP_RUN = 0.6F;
   private static final float SNAP = 3.0F;
   private static final double FAR = 48.0;
   private static final float LIP_GROW = 0.3F;
   private static final float LIP_TIP_DEG = 100.0F;
   private static final int SPRAY_LIFE = 20;
   private static final float SPRAY_SPIN = 14.0F;
   private static final float SPRAY_WHITE = 0.5F;
   private static final float CRASH_BAND = 2.0F;
   private static final int CRASH_LIFE = 45;
   private static final float FLOOD_LENGTH = 14.0F;
   private static final float FLOOD_CUBE = 1.5F;
   private static final float FLOOD_ALPHA = 0.75F;
   private static final float FLOOD_SHEEN = 0.3F;
   private static final long FORGET_TICKS = 100L;
   private static final Map<Integer, TsunamiWaveComposite.LipClock> LIP_PHASE = new HashMap<>();

   private TsunamiWaveComposite() {
   }

   public static void register() {
      VfxComposites.register(VfxEntity.Kind.WATER_WAVE, TsunamiWaveComposite::draw);
   }

   private static void draw(PoseStack ps, VfxDraw.Sink sink, VfxRenderState st, float life) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null) {
         float f = (float)st.secondary.x;
         float grow = st.secondary.y > 0.0 ? (float)st.secondary.y : 1.0F;
         boolean wet = HydroLook.wet(st);
         float reach = TsunamiAbility.reach(f) * grow;
         float fullHeight = TsunamiAbility.height(f) * grow;
         float age = st.ageInTicks;
         float travel = TsunamiAbility.travelTicks(reach);
         float collapse = Mth.clamp((age - travel) / 20.0F, 0.0F, 1.0F);
         if (collapse >= 1.0F) {
            LIP_PHASE.remove(st.seed);
         } else {
            float lean = age < 3.0F ? Mth.sin((float) Math.PI * age / 3.0F) : 0.0F;
            float thrown = Math.max(0.0F, age - 3.0F);
            float rear = AnimEnvelopes.smooth(thrown / 6.0F);
            float pour = TsunamiFx.pourSize(wet);
            float front = TsunamiAbility.frontAt(age, reach) + TsunamiFx.wallDist(f) * Math.max(0.0F, 1.0F - age / 6.0F);
            if (collapse > 0.0F) {
               front += Math.min(0.3F * fullHeight, (age - travel) * 1.25F * 0.45F);
            }

            float height = Mth.lerp(rear, TsunamiFx.wallHeight(f) * pour, fullHeight)
               * (1.0F - 0.1F * lean)
               * (1.0F - 0.9F * (float)Math.pow((double)collapse, 0.6));
            float half = Mth.lerp(rear, TsunamiAbility.halfWidth(f) * 0.75F * pour, st.scale);
            float back = Math.max(0.6F, Math.min(0.6F * height * Math.max(0.3F, rear), front - 3.5F));
            float wallCurl = 0.05F + 0.15F * f;
            float slump = 1.0F + 0.3F * collapse;
            int lead = (st.seed & 1) == 0 ? 1 : -1;
            TsunamiWaveComposite.Curl curlAt = x -> {
               float e = Math.min(1.0F, Math.abs(x) / Math.max(0.01F, half));
               float delay = 8.0F * (0.25F + 0.75F * e) - (x * (float)lead > 0.0F ? 3.0F * e : 0.0F);
               return Mth.lerp(AnimEnvelopes.smooth((thrown - delay) / 8.0F), wallCurl, 1.0F) * slump;
            };
            WaterWave.Look body = HydroLook.body(st);
            float alpha = Math.min(1.0F, body.alpha()) * (float)Math.pow((double)(1.0F - collapse), 0.7);
            Vec3 origin = st.worldPos;
            float yawRad = st.yaw * (float) (Math.PI / 180.0);
            TsunamiWaveComposite.Frame frame = new TsunamiWaveComposite.Frame(origin, Mth.cos(yawRad), Mth.sin(yawRad));
            Vec3 center = frame.world(0.0F, origin.y + (double)height * 0.5, front);
            boolean far = center.distanceTo(VfxCam.position()) > 48.0 + (double)half;
            float c = Mth.clamp(CUBE.getF() * fullHeight / 14.0F, 0.4F, 2.0F) * (far ? 2.0F : 1.0F);
            int up = TsunamiAbility.floorUp(fullHeight);
            int down = TsunamiAbility.floorDown(fullHeight);
            WaterGround ground = WaterGround.kept(st.seed, level, up, down);
            double crestFloor = origin.y;
            int steps = Math.min(Mth.ceil(travel), (int)age);

            for (int k = 1; k <= steps; k++) {
               float d = TsunamiAbility.frontAt((float)k, reach);
               float y = ground.floor(origin.x + (double)(d * frame.sin()), origin.z + (double)(d * frame.cos()), crestFloor);
               if (!Float.isNaN(y)) {
                  crestFloor = (double)y;
               }
            }

            double ref = crestFloor;
            float cos = frame.cos();
            float sin = frame.sin();
            TsunamiPourGround.Floor under = (x, z) -> {
               float g = ground.floor(origin.x + (double)(x * cos) + (double)(z * sin), origin.z - (double)(x * sin) + (double)(z * cos), ref);
               return Float.isNaN(g) ? Float.NaN : (float)((double)g - origin.y);
            };
            Vec3 crestAt = frame.world(0.0F, 0.0, front);
            int light = WaterBlocks.lightAbove(level, crestAt.x, ref, crestAt.z);
            WaterMesh.Paint water = WaterMesh.Paint.of(body.rgb(), alpha, light);
            WaterMesh.Paint foam = water.foam(water.opacity() * FOAM.getF());
            float face = Math.max(c, 0.3F * height + 0.6F * collapse * fullHeight);
            TsunamiWaveComposite.Shape s = new TsunamiWaveComposite.Shape(
               c, front, height, fullHeight, half, back, face, CURL.getF() * height, collapse, age, st.seed, curlAt, under
            );
            int n = Mth.ceil(half / c) + 1;
            float[] tipY = new float[2 * n + 1];
            float[] tipZ = new float[2 * n + 1];
            WaterCubeGrid wall = wall(s, n, age * CHURN.getF());
            WaterCubeGrid curl = curl(s, n, wall, tipY, tipZ);
            float lipPhase = age * LIP_RATE.getF() / 20.0F;
            boolean lips = !far && collapse <= 0.0F && rear > 0.5F;
            ps.pushPose();
            if (lean > 0.0F) {
               ps.translate(0.0F, 0.0F, front);
               ps.mulPose(Axis.XP.rotationDegrees(-8.0F * lean));
               ps.translate(0.0F, 0.0F, -front);
            }

            WaterFramePass.queue(ps, (pose, vc) -> {
               wall.drawWater(pose, vc, water, body.deepRgb(), body.crestRgb(), fullHeight, false);
               curl.drawWater(pose, vc, water, body.deepRgb(), body.crestRgb(), fullHeight, true);
               WaterFramePass.foam(pose, vc, (fp, fvc) -> {
                  wall.drawFoam(fp, fvc, foam, (kx, r, hq) -> {
                     float dz = wall.z(r) - s.front();
                     return dz > -s.plateau() && dz < c && hq > 0.7F * s.crestAt(wall.x(kx)) ? 0.6F : 0.0F;
                  });
                  curl.drawFoam(fp, fvc, foam, (kx, r, hq) -> 0.35F);
               });
               if (lips) {
                  drawLips(pose, vc, wall, tipY, tipZ, water, lipPhase, st.seed);
               }
            });
            ps.popPose();
            flood(ps, s, alpha, body, light, under);
            if (lips) {
               throwSpray(level, st, frame, s, wall, tipY, tipZ, body, lipPhase);
            } else {
               LIP_PHASE.remove(st.seed);
            }

            if (collapse > 0.0F) {
               crash(level, st, frame, s, body, under, collapse);
            }

            forgetStale(level);
         }
      }
   }

   private static WaterCubeGrid wall(TsunamiWaveComposite.Shape s, int n, float scroll) {
      float c = s.c();
      float zTop = s.front() + s.faceLen() + c;
      float zEnd = s.front() - s.plateau() - s.back() - c;
      int rows = Mth.ceil((zTop - zEnd) / c) + 1;
      int gMax = Mth.floor(scroll / c);
      float frac = scroll - (float)gMax * c;
      WaterCubeGrid g = new WaterCubeGrid(2 * n + 1, rows, c, 0.0F);

      for (int k = 0; k <= 2 * n; k++) {
         g.colX(k, (float)(k - n) * c);
      }

      float boil = BOIL.getF() * c;
      float rate = BOIL_RATE.getF();

      for (int r = 0; r < rows; r++) {
         float z = zTop - frac - (float)r * c;
         g.rowZ(r, z);
         float dz = z - s.front();
         int id = gMax - r;
         boolean flat = dz <= 0.0F && dz >= -s.plateau();

         for (int k = 0; k <= 2 * n; k++) {
            float x = g.x(k);
            float crest = s.crestAt(x);
            if (!(crest <= 0.02F)) {
               float curled = s.curl().at(x);
               float target = s.profile(dz, crest, curled);
               if (!(target <= 0.02F)) {
                  float y = s.under().at(x, z);
                  if (!Float.isNaN(y)) {
                     float wave = Mth.sin(s.time() * rate + (float) (Math.PI * 2) * WaterMesh.noise01(s.seed(), id, k));
                     target += flat ? boil * 0.5F * (wave - 1.0F) : boil * Math.min(1.0F, target / s.height() * 3.0F) * wave;
                     g.floor(k, r, y + 0.01F);
                     g.setStepped(k, r, target, s.ceiling(), 3.0F);
                     float len = s.overLen(x, curled);
                     if (dz > 0.0F && dz < len) {
                        float root = s.under().at(x, s.front());
                        if (!Float.isNaN(root)) {
                           float underside = root + crest - s.overThick(dz, len, crest) - s.overDrop(dz, len, crest, curled);
                           float cap = underside - c * 0.5F - (y + 0.01F);
                           if (g.height(k, r) > cap) {
                              g.set(k, r, cap);
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return g;
   }

   private static WaterCubeGrid curl(TsunamiWaveComposite.Shape s, int n, WaterCubeGrid wall, float[] tipY, float[] tipZ) {
      float c = s.c();
      int rows = Math.max(1, Mth.ceil(s.curlLen() / c) + 1);
      WaterCubeGrid g = new WaterCubeGrid(2 * n + 1, rows, c, 0.0F);

      for (int r = 0; r < rows; r++) {
         g.rowZ(r, s.front() + ((float)r + 0.5F) * c);
      }

      for (int k = 0; k <= 2 * n; k++) {
         float x = wall.x(k);
         g.colX(k, x);
         tipY[k] = Float.NaN;
         float crest = s.crestAt(x);
         float root = s.under().at(x, s.front());
         if (!(crest <= 0.02F) && !Float.isNaN(root)) {
            tipY[k] = root + crest;
            tipZ[k] = s.front();
            float curled = s.curl().at(x);
            float len = s.overLen(x, curled);

            for (int r = 0; r < rows; r++) {
               float dz = ((float)r + 0.5F) * c;
               if (dz >= len) {
                  break;
               }

               float thick = s.overThick(dz, len, crest);
               float bottom = root + crest - thick - s.overDrop(dz, len, crest, curled);
               g.floor(k, r, bottom);
               g.set(k, r, thick);
               tipY[k] = bottom + thick;
               tipZ[k] = s.front() + ((float)r + 1.0F) * c;
            }
         }
      }

      return g;
   }

   private static void drawLips(Pose pose, VertexConsumer vc, WaterCubeGrid wall, float[] tipY, float[] tipZ, WaterMesh.Paint water, float phase, int seed) {
      WaterMesh.Paint cap = water.foam(water.opacity() * 0.55F);
      float c = wall.cube();

      for (int k = 0; k < tipY.length; k++) {
         if (!Float.isNaN(tipY[k])) {
            float beat = WaterBlocks.lipBeat(phase, seed, k);
            float size = WaterBlocks.lipSize(c, beat, 0.3F);
            if (!(size < 0.05F)) {
               WaterBlocks.tippedCube(pose, vc, water, cap, wall.x(k), tipY[k] - 0.03F, tipZ[k], size, WaterBlocks.lipTip(beat, 0.3F, 100.0F));
            }
         }
      }
   }

   private static void throwSpray(
      ClientLevel level,
      VfxRenderState st,
      TsunamiWaveComposite.Frame frame,
      TsunamiWaveComposite.Shape s,
      WaterCubeGrid wall,
      float[] tipY,
      float[] tipZ,
      WaterWave.Look body,
      float phase
   ) {
      TsunamiWaveComposite.LipClock clock = LIP_PHASE.computeIfAbsent(st.seed, id -> new TsunamiWaveComposite.LipClock());
      float last = clock.seen == 0L ? phase : clock.phase;
      clock.phase = phase;
      clock.seen = level.getGameTime();
      if (!(last >= phase) && !(phase - last > 0.5F)) {
         WaterCubePool pool = WaterCubeSpray.pool(level, "tsunami_wave" + st.seed, sprayLook(body));
         float speed = 1.25F;

         for (int k = 0; k < tipY.length; k++) {
            if (!Float.isNaN(tipY[k]) && WaterBlocks.lipBroke(last, phase, s.seed(), k)) {
               float x = wall.x(k);
               Vec3 at = frame.world(x, frame.origin().y + (double)tipY[k] + (double)(s.c() * 0.4F), tipZ[k] + s.c() * 0.3F);
               float side = (WaterMesh.noise01(s.seed(), k, Mth.floor(phase)) - 0.5F) * 0.15F;
               Vec3 vel = frame.dir(side, 0.12F + 0.08F * WaterMesh.noise01(s.seed(), k, 5), speed * 0.9F + 0.1F);
               float land = s.under().at(x, tipZ[k] + 4.0F);
               double ground = Float.isNaN(land) ? Double.NaN : frame.origin().y + (double)land;
               pool.add(level, at, vel, -st.yaw, 100.0F, 14.0F, s.c() * 0.8F, 20, (k & 1) == 0, ground);
            }
         }
      }
   }

   private static void crash(
      ClientLevel level,
      VfxRenderState st,
      TsunamiWaveComposite.Frame frame,
      TsunamiWaveComposite.Shape s,
      WaterWave.Look body,
      TsunamiPourGround.Floor under,
      float collapse
   ) {
      int count = CRASH.getI();
      if (count > 0) {
         WaterCubeSpray.Look look = sprayLook(body);
         float c = s.c();
         float band = 2.0F * c;
         int bands = Math.max(1, Mth.ceil(s.half() / band));

         for (int second = 0; second < 2 && (second != 1 || !(collapse < 0.4F)); second++) {
            float z = s.front() + (second == 0 ? c : -s.plateau() - c);

            for (int b = -bands; b <= bands; b++) {
               float x = (float)b * band;
               float crest = s.crestAt(x);
               float y = under.at(x, z);
               if (!(crest <= 0.02F) && !Float.isNaN(y)) {
                  Vec3 at = frame.world(x, frame.origin().y + (double)y + (double)Math.max(c, crest * 0.4F), z);
                  WaterCubeSpray.burstOnce(
                     level,
                     "tsunami_crash" + st.seed + "_" + second + "_" + b,
                     at,
                     frame.dir(0.0F, 0.0F, 0.35F),
                     WaterCubeSpray.Burst.of(count, 0.22F, c * 0.7F).scattered(c).lifted(0.3F).lasting(45).spinning(16.0F).foamed(0.35F),
                     look
                  );
               }
            }
         }
      }
   }

   private static void flood(PoseStack ps, TsunamiWaveComposite.Shape s, float alpha, WaterWave.Look body, int light, TsunamiPourGround.Floor under) {
      float nearZ = s.front() - s.plateau() - s.back() + s.c();
      float farZ = Math.max(2.0F, nearZ - 14.0F);
      float a = 0.75F * alpha;
      if (!(nearZ - farZ < 1.0F) && !(a < 0.05F)) {
         float fc = s.c() * 1.5F;
         int j0 = Mth.ceil(farZ / fc);
         int j1 = Mth.floor(nearZ / fc);
         if (j1 >= j0) {
            int n = Mth.ceil(s.half() / fc);
            WaterCubeGrid g = new WaterCubeGrid(2 * n + 1, j1 - j0 + 1, fc, 0.0F);
            float depth = FLOOD_DEPTH.getF() * (1.0F - s.collapse());

            for (int k = 0; k <= 2 * n; k++) {
               g.colX(k, (float)(k - n) * fc);
            }

            for (int r = 0; r < g.rows(); r++) {
               float z = (float)(j0 + r) * fc;
               g.rowZ(r, z);
               float old = (nearZ - z) / (nearZ - farZ);

               for (int k = 0; k <= 2 * n; k++) {
                  float x = g.x(k);
                  float e = Math.abs(x) / Math.max(0.01F, s.half());
                  int id = (j0 + r) * 131 + k;
                  if (!(e > 1.0F - 0.2F * WaterMesh.noise01(s.seed(), id, 61)) && !(WaterMesh.noise01(s.seed(), id, 62) > 1.15F - old)) {
                     float y = under.at(x, z);
                     if (!Float.isNaN(y)) {
                        g.floor(k, r, y + 0.01F);
                        g.set(k, r, depth * (1.0F - 0.7F * old) * (0.6F + 0.4F * WaterMesh.noise01(s.seed(), id, 63)));
                     }
                  }
               }
            }

            int rgb = WaterMesh.mix(body.crestRgb(), 15004159, 0.15F);
            WaterMesh.Paint water = WaterMesh.Paint.of(rgb, a, light);
            WaterMesh.Paint sheen = water.sheen(0.3F);
            WaterFramePass.queue(ps, (pose, vc) -> {
               g.drawWater(pose, vc, water, false);
               WaterFramePass.foam(pose, vc, (fp, fvc) -> g.drawFoam(fp, fvc, sheen, (kx, r, h) -> 0.5F));
            });
         }
      }
   }

   private static WaterCubeSpray.Look sprayLook(WaterWave.Look body) {
      float a = Math.min(1.0F, body.alpha());
      return new WaterCubeSpray.Look(body.rgb(), a, a * FOAM.getF() * 0.5F);
   }

   private static void forgetStale(ClientLevel level) {
      if (LIP_PHASE.size() > 4) {
         long now = level.getGameTime();
         LIP_PHASE.values().removeIf(v -> now - v.seen > 100L || now < v.seen);
      }
   }

   @FunctionalInterface
   private interface Curl {
      float at(float var1);
   }

   private static record Frame(Vec3 origin, float cos, float sin) {
      Vec3 world(float x, double y, float z) {
         return new Vec3(this.origin.x + (double)(x * this.cos) + (double)(z * this.sin), y, this.origin.z - (double)(x * this.sin) + (double)(z * this.cos));
      }

      Vec3 dir(float x, float y, float z) {
         return new Vec3((double)(x * this.cos + z * this.sin), (double)y, (double)(-x * this.sin + z * this.cos));
      }
   }

   private static final class LipClock {
      float phase;
      long seen;
   }

   private static record Shape(
      float c,
      float front,
      float height,
      float ceiling,
      float half,
      float back,
      float faceLen,
      float curlLen,
      float collapse,
      float time,
      int seed,
      TsunamiWaveComposite.Curl curl,
      TsunamiPourGround.Floor under
   ) {
      float plateau() {
         return 1.5F * this.c;
      }

      float crestAt(float x) {
         float e = Math.abs(x) / Math.max(0.01F, this.half);
         float e3 = e * e * e;
         return e > 1.0F + 0.5F * this.c / this.half ? 0.0F : this.height * Math.max(0.3F, 1.0F - 0.7F * e3 * e3);
      }

      float profile(float dz, float crest, float curled) {
         if (dz > this.faceLen) {
            return 0.0F;
         } else if (dz > 0.0F) {
            return crest * (float)Math.pow((double)(1.0F - dz / this.faceLen), (double)Mth.lerp(Mth.clamp(curled, 0.0F, 1.0F), 0.6F, 1.4F));
         } else if (dz >= -this.plateau()) {
            return crest;
         } else {
            float t = (dz + this.plateau() + this.back) / this.back;
            return t <= 0.0F ? 0.0F : crest * t * t * (3.0F - 2.0F * t);
         }
      }

      float overLen(float x, float curled) {
         return this.curlLen * Mth.clamp(curled, 0.0F, 1.0F) * (1.0F - this.collapse) * Math.min(1.0F, this.crestAt(x) / this.height * 1.5F);
      }

      float overThick(float dz, float len, float crest) {
         return Math.min(Math.max(this.c, crest * 0.5F), this.c * (float)(1 + Mth.floor(2.99F * (1.0F - dz / len))));
      }

      float overDrop(float dz, float len, float crest, float curled) {
         float u = dz / len;
         float raw = crest * (0.3F * u * u * Math.max(1.0F, curled) + 0.6F * this.collapse);
         return this.c * (float)Math.round(raw / this.c);
      }
   }
}
