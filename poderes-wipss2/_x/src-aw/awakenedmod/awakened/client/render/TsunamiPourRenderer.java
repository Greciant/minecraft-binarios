package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.hydro.HydroVisual;
import com.awakenedmod.awakened.content.hydro.TsunamiAbility;
import com.awakenedmod.awakened.content.hydro.TsunamiFx;
import com.awakenedmod.awakened.core.Lab;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;

public final class TsunamiPourRenderer {
   private static final Lab.Value CUBE = Lab.CLIENT.def("hydro.tsunami.pour_cube", 0.5, 0.2, 1.5, 0.05);
   private static final Lab.Value BOIL = Lab.CLIENT.def("hydro.tsunami.pour_boil", 0.7, 0.0, 3.0, 0.05);
   private static final Lab.Value BOIL_RATE = Lab.CLIENT.def("hydro.tsunami.pour_boil_rate", 0.25, 0.0, 1.5, 0.01);
   private static final Lab.Value LIP_RATE = Lab.CLIENT.def("hydro.tsunami.pour_lip_rate", 0.9, 0.0, 4.0, 0.05);
   private static final Lab.Value FOAM = Lab.CLIENT.def("hydro.tsunami.pour_foam", 0.6, 0.0, 1.5, 0.05);
   private static final float RISE = 4.0F;
   private static final float BACK = 0.45F;
   private static final int MAX_ROWS = 8;
   private static final float SNAP = 3.0F;
   private static final float FP_EYE_CLEAR = 0.4F;
   private static final float FP_HOLD = 0.6F;
   private static final float OWN_ALPHA = 0.2F;
   private static final int OWN_ROWS = 2;
   private static final float LIP_GROW = 0.35F;
   private static final float LIP_TIP_DEG = 95.0F;
   private static final int SPRAY_LIFE = 18;
   private static final float SPRAY_SPIN = 12.0F;
   private static final float SPRAY_WHITE = 0.5F;
   private static final Map<Integer, Float> LIP_PHASE = new HashMap<>();

   private TsunamiPourRenderer() {
   }

   public static void register(IEventBus gameBus) {
      WaterFramePass.register(gameBus, TsunamiPourRenderer::draw);
   }

   private static void draw(ClientLevel level, Player p, float pt, PoseStack ps, MultiBufferSource buffers, Vec3 cam) {
      long clock = HydroVisual.of(p).pourAge(level.getGameTime());
      if (clock < 0L) {
         LIP_PHASE.remove(p.getId());
      } else {
         float age = (float)clock + pt;
         float f = TsunamiAbility.chargeAt(age);
         float rise = AnimEnvelopes.smooth(age / 4.0F);
         boolean wet = HydroVisual.of(p).wet();
         float size = TsunamiFx.pourSize(wet);
         float height = Math.max(0.25F, TsunamiFx.wallHeight(f) * size);
         float ceiling = TsunamiFx.wallHeight(1.0F) * size;
         float half = TsunamiAbility.halfWidth(f) * 0.75F * size;
         WaterWave.Look body = HydroLook.body(wet);
         boolean own = HydroHands.ownFirstPerson(p);
         float time = (float)p.tickCount + pt;
         int seed = p.getId() * 13 + 5;
         float yaw = p.getViewYRot(pt);
         float rad = -yaw * (float) (Math.PI / 180.0);
         float cos = Mth.cos(rad);
         float sin = Mth.sin(rad);
         Vec3 feet = p.getPosition(pt);
         float dist = TsunamiFx.wallDist(f);
         Vec3 wall = feet.add((double)(sin * dist), 0.0, (double)(cos * dist));
         WaterGround ground = WaterGround.kept(p, level, 2, 4);
         double wallFloor = (double)ground.floor(wall.x, wall.z, feet.y);
         double baseY = Double.isNaN(wallFloor) ? feet.y : wallFloor;
         if (own) {
            float clear = (float)(p.getEyePosition(pt).y - baseY) - 0.4F;
            float held = Math.min(height, Math.max(0.25F, clear));
            height = Mth.lerp(AnimEnvelopes.smooth((f - 0.6F) / 0.39999998F), held, height);
         }

         TsunamiPourGround.Floor under = (x, z) -> {
            float g = ground.floor(wall.x + (double)(x * cos) + (double)(z * sin), wall.z - (double)(x * sin) + (double)(z * cos), baseY);
            return Float.isNaN(g) ? Float.NaN : (float)((double)g - baseY);
         };
         int light = WaterBlocks.lightAbove(level, wall.x, baseY, wall.z);
         float alpha = Math.min(1.0F, body.alpha()) * Mth.lerp(rise, 0.4F, 1.0F) * (own ? 0.2F : 1.0F);
         WaterMesh.Paint water = WaterMesh.Paint.of(body.rgb(), alpha, light);
         WaterMesh.Paint foam = water.foam(water.opacity() * FOAM.getF());
         float c = CUBE.getF();
         float backLen = Math.max(c, 0.45F * height);
         int rows = own ? 2 : Math.min(8, Mth.ceil(backLen / c) + 1);
         float h = height;
         WaterCubeGrid pile = wall(c, h, ceiling, half, backLen, rows, time, seed, under);
         float lipPhase = time * LIP_RATE.getF() * (0.3F + 0.7F * f) / 20.0F;
         TsunamiPourGround.Tune tune = TsunamiPourGround.Tune.now();
         ps.pushPose();
         ps.translate(wall.x - cam.x, baseY - cam.y, wall.z - cam.z);
         ps.mulPose(Axis.YP.rotationDegrees(-yaw));
         WaterMesh.submit(ps, buffers, (pose, vc) -> {
            TsunamiPourGround.pool(pose, vc, body, alpha, f, half, backLen, c, tune, under, light, time, seed);
            pile.drawWater(pose, vc, water, body.deepRgb(), body.crestRgb(), ceiling, false);
            WaterFramePass.foam(pose, vc, (fp, fvc) -> pile.drawFoam(fp, fvc, foam, (k, r, hq) -> r == 0 && hq > 0.6F * h ? 0.5F : 0.0F));
            if (!own) {
               lips(pose, vc, pile, water, lipPhase, seed);
            }

            TsunamiPourGround.inflow(pose, vc, body, alpha, f, half, tune, under, light, time, seed);
         });
         ps.popPose();
         if (!own) {
            throwSpray(level, p, pile, body, lipPhase, seed, wall, baseY, cos, sin, yaw, f, under);
         }
      }
   }

   private static WaterCubeGrid wall(
      float c, float height, float ceiling, float half, float backLen, int rows, float time, int seed, TsunamiPourGround.Floor under
   ) {
      int n = Mth.ceil(half / c);
      WaterCubeGrid g = new WaterCubeGrid(2 * n + 1, rows, c, 0.0F);
      float boil = BOIL.getF() * c;
      float rate = BOIL_RATE.getF();

      for (int k = 0; k <= 2 * n; k++) {
         g.colX(k, (float)(k - n) * c);
      }

      for (int r = 0; r < rows; r++) {
         float z = (float)(-r) * c;
         g.rowZ(r, z);
         float t = (float)r * c / backLen;
         float prof = t >= 1.0F ? 0.0F : (float)Math.pow((double)(1.0F - t), 0.7);

         for (int k = 0; k <= 2 * n; k++) {
            float x = (float)(k - n) * c;
            float e = Math.abs(x) / Math.max(0.01F, half);
            if (!(e > 1.0F + 0.5F * c / half)) {
               float target = height * Math.max(0.3F, 1.0F - 0.7F * e * e * e) * prof;
               if (!(target <= 0.02F)) {
                  float y = under.at(x, z);
                  if (!Float.isNaN(y)) {
                     target += boil * Math.min(1.0F, target / height * 3.0F) * Mth.sin(time * rate + (float) (Math.PI * 2) * WaterMesh.noise01(seed, r, k));
                     g.floor(k, r, y + 0.01F);
                     g.setStepped(k, r, target, ceiling, 3.0F);
                  }
               }
            }
         }
      }

      return g;
   }

   private static void lips(Pose pose, VertexConsumer vc, WaterCubeGrid g, WaterMesh.Paint water, float phase, int seed) {
      WaterMesh.Paint cap = water.foam(water.opacity() * 0.55F);

      for (int k = 0; k < g.cols(); k++) {
         float h = g.height(k, 0);
         if (!(h < g.cube())) {
            float beat = WaterBlocks.lipBeat(phase, seed, k);
            float s = WaterBlocks.lipSize(g.cube(), beat, 0.35F);
            if (!(s < 0.03F)) {
               WaterBlocks.tippedCube(pose, vc, water, cap, g.x(k), g.floor(k, 0) + h - 0.03F, g.cube() * 0.5F, s, WaterBlocks.lipTip(beat, 0.35F, 95.0F));
            }
         }
      }
   }

   private static void throwSpray(
      ClientLevel level,
      Player p,
      WaterCubeGrid g,
      WaterWave.Look body,
      float phase,
      int seed,
      Vec3 wall,
      double baseY,
      float cos,
      float sin,
      float yaw,
      float f,
      TsunamiPourGround.Floor under
   ) {
      Float last = LIP_PHASE.put(p.getId(), phase);
      if (last != null && !(last >= phase) && !(phase - last > 0.5F)) {
         float a = Math.min(1.0F, body.alpha());
         WaterCubeSpray.Look look = new WaterCubeSpray.Look(body.rgb(), a, a * FOAM.getF() * 0.5F);
         WaterCubePool pool = null;

         for (int k = 0; k < g.cols(); k++) {
            float h = g.height(k, 0);
            if (!(h < g.cube()) && WaterBlocks.lipBroke(last, phase, seed, k)) {
               if (pool == null) {
                  pool = WaterCubeSpray.pool(level, "tsunami_pour" + p.getId(), look);
               }

               float x = g.x(k);
               float y = g.floor(k, 0) + h + g.cube() * 0.3F;
               float z = g.cube() * 0.8F;
               Vec3 at = new Vec3(wall.x + (double)(x * cos) + (double)(z * sin), baseY + (double)y, wall.z - (double)(x * sin) + (double)(z * cos));
               float side = (WaterMesh.noise01(seed, k, Mth.floor(phase)) - 0.5F) * 0.1F;
               float out = 0.07F + 0.1F * f;
               float up = 0.1F + 0.12F * f;
               Vec3 vel = new Vec3((double)(side * cos + out * sin), (double)up, (double)(-side * sin + out * cos));
               float land = under.at(x, z + 1.5F);
               double groundY = Float.isNaN(land) ? baseY : baseY + (double)land;
               pool.add(level, at, vel, yaw, 95.0F, 12.0F, g.cube() * 0.8F, 18, (k & 1) == 0, groundY);
            }
         }
      }
   }
}
