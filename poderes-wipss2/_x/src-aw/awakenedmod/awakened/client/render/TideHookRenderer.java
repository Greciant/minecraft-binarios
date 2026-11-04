package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.client.ClipClock;
import com.awakenedmod.awakened.content.hydro.HydroPalette;
import com.awakenedmod.awakened.content.hydro.TideHookVisual;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.Lab;
import com.awakenedmod.awakened.core.Terrain;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class TideHookRenderer {
   private static final Lab.Value WRAP_CUBE = Lab.CLIENT.def("hydro.hook.wrap_cube", 0.18, 0.08, 0.4, 0.01);
   private static final Lab.Value WRAP_FLOW = Lab.CLIENT.def("hydro.hook.wrap_flow", 0.04, 0.0, 0.3, 0.01);
   private static final Lab.Value BURST = Lab.CLIENT.def("hydro.hook.burst", 18.0, 0.0, 48.0, 1.0);
   private static final ClipClock CLOCK = new ClipClock(AwId.of("hydro_hook_grip"));
   private static final float SWAY_TICKS = 24.0F;
   private static final float THROW_TICKS = 2.5F;
   private static final float COIL_TURNS = 2.0F;
   private static final float COIL_HEIGHT = 0.3F;
   private static final float TENDRIL_RADIUS = 0.18F;
   private static final float WRIST_RADIUS = 0.12F;
   private static final float WRIST_CUBE = 0.07F;
   private static final float WOBBLE_DEG = 3.0F;
   private static final double BOW_PER_SPEED = 0.3;
   private static final double MAX_BOW = 0.8;
   private static final int DRIPS = 4;
   private static final int BURST_DELAY = 2;
   private static final int FORGET_TICKS = 200;
   private static final int BURST_LIFE = 26;
   private static final float BURST_SPIN = 20.0F;
   private static final float BURST_CARRY = 0.8F;
   private static final Map<Integer, TideHookRenderer.Band> BANDS = new HashMap<>();

   private TideHookRenderer() {
   }

   public static void register(IEventBus gameBus) {
      WaterFramePass.register(gameBus, TideHookRenderer::draw);
   }

   private static void draw(ClientLevel level, Player p, float pt, PoseStack poseStack, MultiBufferSource buffers, Vec3 cam) {
      TideHookVisual grip = TideHookVisual.of(p);
      TideHookRenderer.Band band = BANDS.get(p.getId());
      long now = level.getGameTime();
      if (grip.gripping()) {
         Entity target = level.getEntity(grip.target());
         if (target != null) {
            if (band == null) {
               band = new TideHookRenderer.Band();
               BANDS.put(p.getId(), band);
            }

            band.ended = -1L;
            band.seen = now;
            drawGrip(poseStack, buffers, cam, p, target, grip.wet(), pt, band);
         }
      } else if (band != null) {
         if (band.ended < 0L) {
            band.ended = now;
         }

         if (now - band.ended >= 2L || now < band.ended) {
            BANDS.remove(p.getId());
            burst(level, p, band);
         }
      }

      if (BANDS.size() > 8) {
         BANDS.values().removeIf(b -> now - b.seen > 200L || now < b.seen);
      }
   }

   private static void drawGrip(
      PoseStack poseStack, MultiBufferSource buffers, Vec3 cam, Player p, Entity target, boolean wet, float pt, TideHookRenderer.Band band
   ) {
      long clip = CLOCK.age(p);
      float age = clip < 0L ? 100.0F : (float)clip + pt;
      float reach = Mth.clamp((age + 0.4F) / 2.5F, 0.0F, 1.0F);
      Vec3 hand = HydroHands.hand(p, pt, gripLift(age), HydroHands.mainArmLeft(p));
      Vec3 feet = target.getPosition(pt);
      float height = target.getBbHeight();
      float width = target.getBbWidth();
      Vec3 waist = feet.add(0.0, (double)height * 0.5, 0.0);
      float time = (float)p.tickCount + pt;
      int seed = p.getId() * 31 + target.getId();
      int light = WaterBlocks.lightAt(p.level(), waist.x, waist.y, waist.z);
      WaterMesh.Paint water = HydroLook.linePaint(wet, light);
      float alpha = water.opacity();
      boolean ownView = HydroHands.ownFirstPerson(p);
      float cube = WRAP_CUBE.getF() * Mth.clamp(0.7F + 0.5F * width, 0.8F, 1.3F);
      final float coilR = (width * 0.5F + cube * 0.5F + 0.02F) * (1.0F + 0.9F * (1.0F - reach));
      Vec3 toHand = new Vec3(hand.x - waist.x, 0.0, hand.z - waist.z);
      final float knotA = toHand.lengthSqr() < 1.0E-4 ? 0.0F : (float)Mth.atan2(toHand.z, toHand.x);
      Vec3 knot = waist.add((double)(Mth.cos(knotA) * coilR), 0.0, (double)(Mth.sin(knotA) * coilR));
      Vector3f from = rel(hand, cam);
      Vector3f to = rel(knot, cam);
      Vec3 bow = feet.subtract(new Vec3(target.xo, target.yo, target.zo)).scale(-0.3);
      if (bow.length() > 0.8) {
         bow = bow.normalize().scale(0.8);
      }

      WaterTendril.Strain strain = WaterTendril.Strain.taut(new Vector3f((float)bow.x, (float)bow.y, (float)bow.z), ownView ? 0.45F : 1.0F);
      WaterMesh.submit(poseStack, buffers, (pose, vc) -> {
         WaterTendril.draw(pose, vc, water, from, to, 0.18F, reach, time, seed, strain);
         if (!ownView) {
            wristWrap(pose, vc, water, from, to, time, seed);
         }
      });
      final float coilHalf = height * 0.3F * 0.5F;
      band.target = target.getId();
      band.waist = waist;
      band.coilR = coilR;
      band.coilHalf = coilHalf;
      band.cube = cube;
      band.knotA = knotA;
      band.wet = wet;
      band.alpha = alpha;
      if (reach < 0.6F) {
         band.alpha = 0.0F;
      } else {
         float close = Mth.clamp((reach - 0.6F) / 0.4F, 0.0F, 1.0F);
         poseStack.pushPose();
         poseStack.translate(waist.x - cam.x, waist.y - cam.y, waist.z - cam.z);
         poseStack.mulPose(Axis.XP.rotationDegrees(3.0F * Mth.sin(time * 0.21F)));
         poseStack.mulPose(Axis.ZP.rotationDegrees(3.0F * Mth.sin(time * 0.17F + 1.3F)));
         WaterMesh.Paint bandWater = water.alpha(alpha * close);
         WaterMesh.Paint foam = bandWater.foam(bandWater.opacity() * 0.5F);
         final float half = cube * 0.5F;
         WaterMesh.Spine helix = new WaterMesh.Spine() {
            @Override
            public Vector3f center(float u) {
               float a = knotA + u * 2.0F * (float) (Math.PI * 2);
               return new Vector3f(Mth.cos(a) * coilR, Mth.lerp(u, -coilHalf, coilHalf), Mth.sin(a) * coilR);
            }

            @Override
            public float radius(float u) {
               return half;
            }
         };
         float helixLen = (float) (Math.PI * 4) * coilR;
         float kx = Mth.cos(knotA) * coilR;
         float kz = Mth.sin(knotA) * coilR;
         WaterMesh.submit(
            poseStack,
            buffers,
            (pose, vc) -> {
               WaterBlocks.chain(
                  pose,
                  vc,
                  bandWater,
                  foam,
                  WaterBlocks.Chain.along(helix, helixLen, cube * 1.05F).flowing(time * WRAP_FLOW.getF()).rough(0.01F, 12.0F).foamed(0.25F).seeded(seed)
               );
               WaterBlocks.cube(pose, vc, bandWater, foam, kx, 0.0F, kz, half * 1.5F);
               WaterBlocks.cube(pose, vc, bandWater, null, kx * 1.08F, coilHalf * 0.8F, kz * 1.08F, half);
               WaterBlocks.cube(pose, vc, bandWater, null, kx * 1.08F, -coilHalf * 0.8F, kz * 1.08F, half);

               for (int i = 0; i < 4; i++) {
                  float cyc = (time / 10.0F + WaterMesh.noise01(seed, i, 4)) % 1.0F;
                  float a = (float) (Math.PI * 2) * ((float)i + WaterMesh.noise01(seed, i, 5)) / 4.0F;
                  WaterBlocks.cube(
                     pose, vc, bandWater, null, Mth.cos(a) * coilR, -coilHalf - half - 1.4F * cyc * cyc, Mth.sin(a) * coilR, half * 0.5F * (1.0F - 0.5F * cyc)
                  );
               }
            }
         );
         poseStack.popPose();
      }
   }

   private static void burst(ClientLevel level, Player p, TideHookRenderer.Band band) {
      int n = BURST.getI();
      if (!(band.alpha <= 0.0F) && n > 0) {
         Entity t = level.getEntity(band.target);
         Vec3 waist = t != null ? t.position().add(0.0, (double)t.getBbHeight() * 0.5, 0.0) : band.waist;
         Vec3 drift = t != null ? new Vec3(t.getX() - t.xo, t.getY() - t.yo, t.getZ() - t.zo).scale(0.8F) : Vec3.ZERO;
         double ground = Terrain.floorNear(level, waist.x, waist.z, waist.y, 1, 16, true);
         WaterCubePool pool = WaterCubeSpray.pool(
            level, "tidehook" + p.getId(), new WaterCubeSpray.Look(HydroPalette.rgb(band.wet), band.alpha, band.alpha * 0.3F)
         );
         RandomSource rnd = level.getRandom();

         for (int i = 0; i < n; i++) {
            float u = ((float)i + 0.5F) / (float)n;
            float a = band.knotA + u * 2.0F * (float) (Math.PI * 2);
            float out = 0.1F + 0.08F * rnd.nextFloat();
            Vec3 at = waist.add((double)(Mth.cos(a) * band.coilR), (double)Mth.lerp(u, -band.coilHalf, band.coilHalf), (double)(Mth.sin(a) * band.coilR));
            Vec3 vel = drift.add((double)(Mth.cos(a) * out), (double)(0.06F + 0.1F * rnd.nextFloat()), (double)(Mth.sin(a) * out));
            pool.add(
               level,
               at,
               vel,
               WaterBlocks.yawOutward(a),
               0.0F,
               20.0F * (2.0F * rnd.nextFloat() - 1.0F),
               band.cube * (0.8F + 0.4F * rnd.nextFloat()),
               26,
               i % 3 == 0,
               ground
            );
         }
      }
   }

   private static float gripLift(float age) {
      return age < 3.0F ? Mth.lerp(age / 3.0F, -90.0F, liftAt(3.0F)) : liftAt(age);
   }

   private static float liftAt(float age) {
      float base = -86.0F + 2.0F * (1.0F - Mth.cos((float) (Math.PI * 2) * age / 24.0F));
      float tri = 1.0F - Math.abs(age / 2.0F % 2.0F - 1.0F);
      float armX = base + (tri * 4.0F - 2.0F);
      return -90.0F - armX;
   }

   private static void wristWrap(Pose pose, VertexConsumer vc, WaterMesh.Paint water, Vector3f hand, Vector3f to, float time, int seed) {
      Vector3f dir = new Vector3f(to).sub(hand);
      if (!(dir.lengthSquared() < 1.0E-4F)) {
         dir.normalize();
         Pose local = pose.copy();
         Quaternionf q = new Quaternionf().rotationTo(0.0F, 1.0F, 0.0F, dir.x, dir.y, dir.z);
         local.pose().translate(hand.x - dir.x * 0.16F, hand.y - dir.y * 0.16F, hand.z - dir.z * 0.16F).rotate(q);
         local.normal().rotate(q);
         WaterBlocks.ring(local, vc, water, null, 0.12F, 0.07F, 0.08400001F, 0.01F, 0.0F, time, seed);
      }
   }

   private static Vector3f rel(Vec3 p, Vec3 cam) {
      return new Vector3f((float)(p.x - cam.x), (float)(p.y - cam.y), (float)(p.z - cam.z));
   }

   private static final class Band {
      int target;
      Vec3 waist = Vec3.ZERO;
      float coilR;
      float coilHalf;
      float cube;
      float knotA;
      float alpha;
      boolean wet;
      long ended = -1L;
      long seen;
   }
}
