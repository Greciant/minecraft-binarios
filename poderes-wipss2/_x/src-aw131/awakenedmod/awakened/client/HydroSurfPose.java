package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.hydro.HydroVisual;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.Lab;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.zigythebird.playeranimcore.animation.layered.modifier.AdjustmentModifier.PartModifier;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre;

public final class HydroSurfPose {
   private static final Lab.Value FORWARD = Lab.CLIENT.def("hydro.surf.rider_forward", 0.0, -2.0, 2.0, 0.02);
   private static final Lab.Value UP = Lab.CLIENT.def("hydro.surf.rider_up", 0.0, -2.0, 2.0, 0.02);
   private static final Lab.Value SIDE = Lab.CLIENT.def("hydro.surf.rider_side", 0.0, -2.0, 2.0, 0.02);
   private static final Lab.Value CARVE = Lab.CLIENT.def("hydro.surf.carve", 1.8, 0.0, 8.0, 0.1);
   private static final Lab.Value CARVE_MAX = Lab.CLIENT.def("hydro.surf.carve_max", 20.0, 0.0, 60.0, 1.0);
   private static final ResourceLocation SURF_CLIP = AwId.of("hydro_surf");
   private static boolean pushed;

   private HydroSurfPose() {
   }

   public static void register(IEventBus modBus, IEventBus gameBus) {
      AwakenedAnimations.addPoseModifier(HydroSurfPose::bone);
      gameBus.addListener(HydroSurfPose::onPre);
      gameBus.addListener(HydroSurfPose::onPost);
      gameBus.addListener(HydroSurfPose::onClientTick);
   }

   private static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && !mc.isPaused()) {
         for (Player p : mc.level.players()) {
            if (HydroVisual.of(p).surfing()) {
               p.yBodyRot = p.getYRot();
               p.yHeadRot = p.getYRot();
            }
         }
      }
   }

   private static boolean posed(Entity entity) {
      if (!HydroVisual.of(entity).surfing()) {
         return false;
      } else {
         Minecraft mc = Minecraft.getInstance();
         return entity != mc.getCameraEntity() || !mc.options.getCameraType().isFirstPerson();
      }
   }

   private static void apply(PoseStack ps, HydroSurfPose.Stance st) {
      ps.pushPose();
      pushed = true;
      ps.mulPose(Axis.YP.rotationDegrees(-st.heading()));
      ps.translate(-SIDE.getF(), UP.getF() + st.bob(), FORWARD.getF());
      ps.mulPose(Axis.ZP.rotationDegrees(st.carve()));
      ps.mulPose(Axis.YP.rotationDegrees(st.heading()));
   }

   private static Optional<PartModifier> bone(String bone, Entity avatar, boolean firstPerson, float partialTick) {
      if (!firstPerson && avatar instanceof LivingEntity body && HydroVisual.of(avatar).surfing() && AwakenedAnimations.isTriggered(avatar.getId(), SURF_CLIP)) {
         float headYaw = Mth.wrapDegrees(Mth.rotLerp(partialTick, body.yHeadRotO, body.yHeadRot) - Mth.rotLerp(partialTick, body.yBodyRotO, body.yBodyRot));
         return HydroSurfStance.bone(bone, HydroSurfMotion.riderCarve(avatar, partialTick), headYaw, body.getViewXRot(partialTick));
      }

      return Optional.empty();
   }

   private static void onPre(Pre event) {
      pushed = false;
      if (posed(event.getEntity())) {
         apply(event.getPoseStack(), HydroSurfPose.Stance.of(event.getEntity(), event.getPartialTick()));
      }
   }

   private static void onPost(net.neoforged.neoforge.client.event.RenderPlayerEvent.Post event) {
      if (pushed) {
         event.getPoseStack().popPose();
         pushed = false;
      }
   }

   private static record Stance(float heading, float carve, float bob) {
      static HydroSurfPose.Stance of(Entity rider, float pt) {
         float carve = Mth.clamp(
            HydroSurfMotion.riderTurn(rider, pt) * HydroSurfPose.CARVE.getF(), -HydroSurfPose.CARVE_MAX.getF(), HydroSurfPose.CARVE_MAX.getF()
         );
         return new HydroSurfPose.Stance(rider.getViewYRot(pt), carve, HydroSurfMotion.bob(rider, pt));
      }
   }
}
