package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.client.AwakenedAnimations;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.Lab;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class HydroLanceFirstPerson {
   private static final Lab.Value X = Lab.CLIENT.def("hydro.lance.fp_x", 0.85, -1.0, 1.0, 0.01);
   private static final Lab.Value Y = Lab.CLIENT.def("hydro.lance.fp_y", 0.28, -1.0, 1.0, 0.01);
   private static final Lab.Value Z = Lab.CLIENT.def("hydro.lance.fp_z", -0.95, -2.0, 0.0, 0.01);
   private static final Lab.Value PITCH = Lab.CLIENT.def("hydro.lance.fp_pitch", -4.5, -45.0, 45.0, 0.5);
   private static final Lab.Value YAW = Lab.CLIENT.def("hydro.lance.fp_yaw", 13.5, -45.0, 45.0, 0.5);
   private static final Lab.Value ROLL = Lab.CLIENT.def("hydro.lance.fp_roll", 0.0, -180.0, 180.0, 1.0);
   private static final Lab.Value SCALE = Lab.CLIENT.def("hydro.lance.fp_scale", 0.9, 0.2, 1.5, 0.01);
   private static final Lab.Value PULL = Lab.CLIENT.def("hydro.lance.fp_pull", 0.15, 0.0, 1.0, 0.01);
   private static final Lab.Value TREMBLE = Lab.CLIENT.def("hydro.lance.fp_tremble", 0.006, 0.0, 0.05, 0.001);
   private static final float RAISE_TICKS = 4.0F;
   private static final float THROW_TICKS = 6.0F;
   private static final Vector3f SHOULDER = new Vector3f(1.4F, -0.2F, 0.0F);
   private static final Vector3f ARM_BACK = new Vector3f(0.87F, -0.49F, 0.07F);
   private static final float FIST_X = -1.0F;
   private static final float FIST_Y = 8.5F;
   private static final float ARM_Z_ROT = 0.1F;
   private static final float START_LENGTH = 0.2F;
   private static final float MIN_FADE = 0.85F;
   private static final float GATHER_REACH = 0.25F;
   private static HydroLanceFirstPerson.Hold lastHold;
   private static double lastSeen = -1.0;
   private static double throwAt = -1.0;

   private HydroLanceFirstPerson() {
   }

   public static void register(IEventBus gameBus) {
      AwakenedAnimations.vanillaFirstPerson(AwId.of("hydro_lance_charge"), p -> false);
      AwakenedAnimations.vanillaFirstPerson(AwId.of("hydro_lance_throw"), p -> false);
      gameBus.addListener(HydroLanceFirstPerson::onRenderHand);
   }

   public static float scale() {
      return SCALE.getF();
   }

   public static HydroLanceFirstPerson.Hold hold(boolean left, float age, float time) {
      float side = left ? -1.0F : 1.0F;
      float f = Math.min(1.0F, age / 40.0F);
      float pitch = PITCH.getF() * (float) (Math.PI / 180.0);
      float yaw = YAW.getF() * (float) (Math.PI / 180.0);
      Vector3f dir = new Vector3f(-side * Mth.sin(yaw) * Mth.cos(pitch), Mth.sin(pitch), -Mth.cos(yaw) * Mth.cos(pitch));
      float draw = PULL.getF() * (1.0F - (1.0F - f) * (1.0F - f));
      float raise = Math.min(1.0F, age / 4.0F);
      float low = (1.0F - raise) * (1.0F - raise) * 0.55F;
      Vector3f grip = new Vector3f(side * X.getF(), Y.getF() + 0.5F * draw - low, Z.getF()).sub(new Vector3f(dir).mul(draw));
      if (f >= 1.0F) {
         float amp = TREMBLE.getF();
         grip.add(side * amp * 0.6F * Mth.sin(time * 2.3F + 1.0F), amp * Mth.sin(time * 1.9F), 0.0F);
      }

      return new HydroLanceFirstPerson.Hold(grip, dir, side * ROLL.getF() * (float) (Math.PI / 180.0), left);
   }

   public static Vec3 worldTip(Player p, float partialTick, float age, boolean left, float length) {
      Vector3f c = hold(left, age, (float)p.tickCount + partialTick).tip(length);
      Vec3 look = p.getViewVector(partialTick);
      Vec3 right = new Vec3(-look.z, 0.0, look.x);
      right = right.lengthSqr() < 1.0E-6 ? new Vec3(-1.0, 0.0, 0.0) : right.normalize();
      Vec3 up = right.cross(look);
      return p.getEyePosition(partialTick).add(right.scale((double)c.x)).add(up.scale((double)c.y)).add(look.scale((double)(-c.z)));
   }

   private static void onRenderHand(RenderHandEvent event) {
      Minecraft mc = Minecraft.getInstance();
      Player p = mc.player;
      if (event.getHand() == InteractionHand.MAIN_HAND
         && p != null
         && mc.level != null
         && HydroHands.ownFirstPerson(p)
         && !AwakenedAnimations.libraryOwnsFirstPerson()) {
         float pt = event.getPartialTick();
         double now = (double)((float)mc.level.getGameTime() + pt);
         long age = HydroLanceAnchors.chargeAge(p);
         if (age >= 0L) {
            float a = (float)age + pt;
            HydroLanceFirstPerson.Hold hold = hold(HydroHands.mainArmLeft(p), a, (float)p.tickCount + pt);
            event.setCanceled(true);
            drawArm(event, p, hold);
            drawLance(event, p, hold, a);
            lastHold = hold;
            lastSeen = now;
            throwAt = -1.0;
         } else {
            if (lastHold != null && now - lastSeen < 2.0) {
               throwAt = lastSeen;
               lastSeen = -1.0;
            }

            if (!(throwAt < 0.0) && lastHold != null) {
               float t = (float)((now - throwAt) / 6.0);
               if (!(t >= 1.0F) && !(t < 0.0F)) {
                  event.setCanceled(true);
                  float thrust = 1.0F - (1.0F - t) * (1.0F - t);
                  Vector3f grip = new Vector3f(lastHold.dir()).mul(0.55F * thrust).add(lastHold.grip()).add(0.0F, -0.9F * t * t, 0.0F);
                  drawArm(event, p, new HydroLanceFirstPerson.Hold(grip, lastHold.dir(), lastHold.roll(), lastHold.left()));
               } else {
                  throwAt = -1.0;
                  lastHold = null;
                  mc.getEntityRenderDispatcher().getItemInHandRenderer().itemUsed(InteractionHand.MAIN_HAND);
               }
            }
         }
      }
   }

   private static void drawLance(RenderHandEvent event, Player p, HydroLanceFirstPerson.Hold hold, float a) {
      float f = Math.min(1.0F, a / 40.0F);
      boolean wet = HydroLanceAnchors.wet(p);
      HydroLanceMesh.Dims dims = HydroLanceMesh.Dims.of(f, wet, f, 0.2F);
      HydroLanceMesh.Paints paints = HydroLanceMesh.Paints.of(wet, Math.max(0.85F, Mth.lerp(Math.min(1.0F, a / 6.0F), 0.3F, 1.0F)), event.getPackedLight());
      int seed = p.getId() * 31 + 7;
      float time = (float)p.tickCount + event.getPartialTick();
      float s = scale();
      Vector3f dir = hold.dir();
      PoseStack ps = event.getPoseStack();
      ps.pushPose();
      ps.translate(hold.grip().x, hold.grip().y, hold.grip().z);
      ps.mulPose(Axis.YP.rotation((float)(-Math.atan2((double)(-dir.x), (double)dir.z))));
      ps.mulPose(Axis.XP.rotation((float)(-Math.asin((double)Mth.clamp(dir.y, -1.0F, 1.0F)))));
      ps.scale(s, s, s);
      ps.translate(0.0F, 0.0F, dims.length() - 0.32F);
      VfxDraw.Sink sink = new VfxDraw.Sink(event.getMultiBufferSource());
      WaterMesh.submit(ps, sink, (pose, vc) -> {
         HydroLanceMesh.spear(pose, vc, paints, dims, seed, time);
         HydroLanceMesh.gather(pose, vc, paints, dims, seed, time, f, 0.25F);
      });
      ps.popPose();
   }

   private static void drawArm(RenderHandEvent event, Player p, HydroLanceFirstPerson.Hold hold) {
      if (!p.isInvisible() && p instanceof AbstractClientPlayer player) {
         boolean left = hold.left();
         float side = left ? -1.0F : 1.0F;
         Vector3f shoulder = new Vector3f(side * SHOULDER.x, SHOULDER.y, SHOULDER.z);
         Vector3f along = new Vector3f(hold.grip()).sub(shoulder).normalize();
         Vector3f back = new Vector3f(side * ARM_BACK.x, ARM_BACK.y, ARM_BACK.z);
         back.sub(new Vector3f(along).mul(back.dot(along)));
         if (back.lengthSquared() < 1.0E-6F) {
            back.set(0.0F, 0.0F, 1.0F);
         }

         Vector3f zAxis = back.normalize();
         Vector3f xAxis = new Vector3f(along).cross(zAxis);
         Quaternionf basis = new Matrix3f(xAxis, along, zAxis).getNormalizedRotation(new Quaternionf());
         basis = new Quaternionf().rotationAxis(hold.roll(), along).mul(basis);
         PlayerRenderer renderer = (PlayerRenderer)Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
         ModelPart arm = left ? ((PlayerModel)renderer.getModel()).leftArm : ((PlayerModel)renderer.getModel()).rightArm;
         PartPose pivot = arm.getInitialPose();
         float px = pivot.x;
         float py = pivot.y;
         float pz = pivot.z;
         PoseStack ps = event.getPoseStack();
         ps.pushPose();
         ps.translate(hold.grip().x, hold.grip().y, hold.grip().z);
         ps.mulPose(basis);
         ps.translate(-side * -1.0F / 16.0F, -0.53125F, 0.0F);
         ps.mulPose(Axis.ZP.rotation(-side * 0.1F));
         ps.translate(-px / 16.0F, -py / 16.0F, -pz / 16.0F);
         int light = event.getPackedLight();
         if (left) {
            renderer.renderLeftHand(ps, event.getMultiBufferSource(), light, player);
         } else {
            renderer.renderRightHand(ps, event.getMultiBufferSource(), light, player);
         }

         ps.popPose();
      }
   }

   public static record Hold(Vector3f grip, Vector3f dir, float roll, boolean left) {
      public Vector3f tip(float length) {
         return new Vector3f(this.dir).mul((length - 0.32F) * HydroLanceFirstPerson.scale()).add(this.grip);
      }
   }
}
