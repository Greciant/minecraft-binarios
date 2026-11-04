package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.Awakened;
import com.awakenedmod.awakened.content.exosavant.ExoPiece;
import com.awakenedmod.awakened.content.item.ReaperScytheItem;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.ProneFlight;
import com.awakenedmod.awakened.core.data.AeroVisual;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.ExoVisual;
import com.awakenedmod.awakened.core.data.SpiderVisual;
import com.awakenedmod.awakened.core.npc.ClipBearer;
import com.zigythebird.playeranim.animation.PlayerAnimResources;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.animation.RawAnimation;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractFadeModifier;
import com.zigythebird.playeranimcore.animation.layered.modifier.AdjustmentModifier;
import com.zigythebird.playeranimcore.animation.layered.modifier.AdjustmentModifier.PartModifier;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonConfiguration;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.easing.EasingType;
import com.zigythebird.playeranimcore.enums.PlayState;
import com.zigythebird.playeranimcore.math.Vec3f;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

@EventBusSubscriber(
   modid = "awakened",
   value = {Dist.CLIENT}
)
public final class AwakenedAnimations {
   public static final ResourceLocation LAYER = AwId.of("ability_anim");
   private static final int PRIORITY = 1000;
   private static final float FIRST_PERSON_PITCH_FOLLOW = 1.0F;
   private static final float FIRST_PERSON_YAW_FOLLOW = 1.0F;
   private static final float HEAD_LOOK_FOLLOW = 0.45F;
   private static final ResourceLocation FIRE_JETS_ANIM = AwId.of("fire_jets");
   private static final ResourceLocation FIRE_JETS_TAP_ANIM = AwId.of("fire_jets_tap");
   private static final ResourceLocation BARRAGE_ANIM = AwId.of("pyro_barrage");
   private static final ResourceLocation FIRE_JETS_LAND_ANIM = AwId.of("fire_jets_land");
   private static final int JET_LAND_TICKS = 9;
   private static final Set<ResourceLocation> FP_VANILLA_CLIPS = Set.of(FIRE_JETS_ANIM, FIRE_JETS_TAP_ANIM, FIRE_JETS_LAND_ANIM);
   private static final Set<ResourceLocation> AIM_FOLLOW_ANIMS = Set.of(
      AwId.of("chest_beam"),
      AwId.of("flame_beam"),
      BARRAGE_ANIM,
      AwId.of("limitless_blue_hold"),
      AwId.of("limitless_red_point"),
      AwId.of("limitless_purple_charge")
   );
   private static final Set<ResourceLocation> AIM_HAND_CHANNELS = Set.of(
      AwId.of("flame_beam"), BARRAGE_ANIM, AwId.of("limitless_blue_hold"), AwId.of("limitless_red_point"), AwId.of("limitless_purple_charge")
   );
   private static final float AIM_BODY_PITCH_FOLLOW = 0.65F;
   private static final float AIM_BODY_PITCH_MAX = 55.0F;
   private static final float AIM_BODY_PITCH_MAX_CROUCH = 35.0F;
   private static final float AIM_ARM_RESIDUAL_MAX = 40.0F;
   private static final float AIM_HEAD_MIN = -60.0F;
   private static final float AIM_HEAD_MAX = 50.0F;
   private static final float AIM_TORSO_LAG_GAIN = 1.5F;
   private static final float AIM_TORSO_LAG_MAX = 12.0F;
   private static final float AIM_LAG_EASE = 0.14F;
   private static final AwakenedAnimations.PulseBeat AIM_PULSE_DEFAULT = new AwakenedAnimations.PulseBeat(10, 3, 60.0F, 3.0F);
   private static final Map<ResourceLocation, AwakenedAnimations.PulseBeat> AIM_PULSE_BEATS = Map.of(
      BARRAGE_ANIM,
      new AwakenedAnimations.PulseBeat(6, 2, 1.0F, 4.5F),
      AwId.of("limitless_blue_hold"),
      AwakenedAnimations.PulseBeat.NONE,
      AwId.of("limitless_red_point"),
      AwakenedAnimations.PulseBeat.NONE,
      AwId.of("limitless_purple_charge"),
      AwakenedAnimations.PulseBeat.NONE
   );
   private static final Map<Integer, ResourceLocation> ACTIVE_TRIGGERED = new HashMap<>();
   private static final Map<ResourceLocation, AwakenedAnimations.Transition> TRANSITIONS = new HashMap<>();
   private static final float WINDOW_RESIDUAL = 0.05F;
   private static final Map<Integer, Long> FADING_OUT = new HashMap<>();
   private static final Map<Integer, Integer> CLIP_GENERATION = new HashMap<>();
   private static boolean loggedLoaded;
   private static final List<AwakenedAnimations.PoseModifier> POSE_MODIFIERS = new ArrayList<>();
   private static boolean heldScytheLastTick;
   private static final Map<Integer, AwakenedAnimations.AimFollow> AIM_FOLLOW = new HashMap<>();
   private static final ClipClock AIM_CLOCK = new ClipClock((IntPredicate)(id -> {
      ResourceLocation active = triggeredClip(id);
      return active != null && AIM_FOLLOW_ANIMS.contains(active) && isPlaying(id, active);
   }));
   private static final Map<Integer, Boolean> JET_MEMO = new HashMap<>();
   private static long jetMemoTime = Long.MIN_VALUE;
   private static final float SURF_TWIST_DEG = 60.0F;
   private static final float SURF_BANK_GAIN = 4.0F;
   private static final float SURF_BANK_MAX_DEG = 28.0F;
   private static final float SURF_EASE = 0.18F;
   private static final Map<Integer, AwakenedAnimations.SurfPose> SURF_POSE = new HashMap<>();
   private static final float JET_BODY_AUTHORED = 4.0F;
   private static final float JET_ARM_AUTHORED = 20.0F;
   private static final float JET_ARM_SPLAY_AUTHORED = 22.0F;
   private static final float JET_LEG_AUTHORED = 2.0F;
   private static final float JET_LEG_SPLAY_AUTHORED = 4.0F;
   private static final float JET_BODY_CRUISE = 78.0F;
   private static final float JET_ARM_CRUISE = 16.0F;
   private static final float JET_ARM_SPLAY_CRUISE = 5.0F;
   private static final float JET_LEG_CRUISE = 6.0F;
   private static final float JET_LEG_SPLAY_CRUISE = 1.0F;
   private static final float JET_LEAN_GAIN = 74.0F;
   private static final float JET_ARM_CRUISE_SWEEP = -4.0F;
   private static final float JET_ARM_CRUISE_TUCK = -17.0F;
   private static final float JET_LEG_CRUISE_SWEEP = 4.0F;
   private static final float JET_LEG_CRUISE_TUCK = -3.0F;
   private static final float JET_ALIGN = 0.8F;
   private static final float JET_DIR_SPEED = 0.25F;
   private static final float JET_HOVER_ALIGN = 0.35F;
   private static final float JET_PITCH_MIN = -20.0F;
   private static final float JET_PITCH_MAX = 115.0F;
   private static final float JET_BANK_GAIN = 4.5F;
   private static final float JET_BANK_MAX = 25.0F;
   private static final float JET_BANK_DEADBAND = 0.25F;
   private static final float JET_HEAD_MIN = -85.0F;
   private static final float JET_HEAD_MAX = 60.0F;
   private static final float JET_HEAD_SOFT = 12.0F;
   private static final float JET_EASE_GROW = 0.25F;
   private static final float JET_EASE_SHRINK = 0.08F;
   private static final float JET_LEAN_EASE = 0.5F;
   private static final float JET_LAT_FULL = 0.18F;
   private static final float JET_LAT_ROLL_MAX = 40.0F;
   private static final float JET_ROLL_MAX = 50.0F;
   private static final float JET_ARM_OUT_SPLAY = 14.0F;
   private static final float JET_ARM_OUT_PITCH = -4.0F;
   private static final float JET_ARM_IN_SPLAY = 12.0F;
   private static final float JET_ARM_IN_PITCH = 10.0F;
   private static final float JET_ARM_PITCH_RATE_GAIN = 2.0F;
   private static final float JET_ARM_PITCH_RATE_MAX = 12.0F;
   private static final float JET_HEAD_LEAD_GAIN = 3.5F;
   private static final float JET_HEAD_LEAD_MAX = 18.0F;
   private static final float JET_HEAD_COUNTER_ROLL = 0.5F;
   private static final float JET_LEG_SWAY = 6.0F;
   private static final float JET_LEG_TRAIL = 3.0F;
   private static final float JET_ACCEL_FULL = 0.08F;
   private static final float JET_LEG_TUCK = 5.0F;
   private static final float JET_LEG_TUCK_SPLAY = 2.0F;
   private static final float JET_HOVER_SPEED = 0.15F;
   private static final float JET_BREATHE_PITCH = 1.5F;
   private static final float JET_BREATHE_ROLL = 1.0F;
   private static final float JET_BREATHE_PERIOD = 44.0F;
   private static final float JET_BREATHE_ARM = 1.2F;
   private static final float JET_BREATHE_ARM_PERIOD = 27.0F;
   private static final float JET_TAP_COIL = -9.0F;
   private static final float JET_TAP_PUNCH = 30.0F;
   public static final float HIP_HEIGHT = 0.75F;
   private static final float JET_LEG_REACH = 0.75F;
   private static final float JET_HEAD_REACH = 1.25F;
   private static final float JET_FIGURE_MID = 0.25F;
   private static final float JET_BODY_HALF_DEPTH = 0.12F;
   private static final Map<Integer, AwakenedAnimations.JetPoseState> JET_FLIGHT = new HashMap<>();
   private static final float BANK_GAIN = 3.5F;
   private static final float BANK_MAX_DEG = 22.0F;
   private static final float JET_BODY_EASE = 0.2F;
   private static final float FLAT = 85.0F;
   private static final float ALIGN = 1.0F;
   private static final float MIN_PITCH = -20.0F;
   private static final float MAX_PITCH = 160.0F;
   private static final float SWEEP_MAX = 15.0F;
   private static final float ARM_SPLAY_HOVER = 12.0F;
   private static final float ARM_SPLAY_MID = 24.0F;
   private static final float ARM_SPLAY_BOOST = 5.0F;
   private static final float LEG_SPLAY_HOVER = 3.0F;
   private static final float LEG_SPLAY_BOOST = 1.0F;
   private static final float HEAD_MIN = -90.0F;
   private static final float HEAD_MAX = 45.0F;
   private static final float BRAKE_BODY_BACK = -35.0F;
   private static final float ALIGN_BRAKE = 0.5F;
   private static final float BRAKE_MIN_PITCH = -50.0F;
   private static final float BRAKE_ARM_X = -75.0F;
   private static final float BRAKE_ARM_SPLAY = 16.0F;
   private static final float BRAKE_LEG_X = -22.0F;
   private static final float BRAKE_LEG_SPLAY = 8.0F;
   private static final float POSE_FULL_MS = 52.0F;
   private static final Map<Integer, AwakenedAnimations.JetPose> JET_POSE = new HashMap<>();
   private static final Map<ResourceLocation, Predicate<Player>> HELD_LOOPS = new LinkedHashMap<>();
   private static final Map<ResourceLocation, Predicate<Player>> VANILLA_FIRST_PERSON = new LinkedHashMap<>();

   public static float firstPersonArmPitch(float lookPitchDeg) {
      return lookPitchDeg * 1.0F;
   }

   public static float aimedChannelBodyPitch(float lookPitchDeg, boolean crouching) {
      float max = crouching ? 35.0F : 55.0F;
      return Mth.clamp(lookPitchDeg * 0.65F, -max, max);
   }

   public static float aimedChannelArmPitch(float lookPitchDeg, boolean crouching) {
      float tilt = aimedChannelBodyPitch(lookPitchDeg, crouching);
      return tilt + aimedChannelArmResidual(lookPitchDeg, tilt);
   }

   public static float aimedChannelArmPitch(Entity avatar, float lookPitchDeg, boolean crouching) {
      AwakenedAnimations.AimFollow s = aimFollowState(avatar);
      return s == null ? 0.0F : aimedChannelArmPitch(lookPitchDeg, crouching) * s.weight;
   }

   private static float aimedChannelArmResidual(float lookPitchDeg, float bodyTiltDeg) {
      return Mth.clamp(lookPitchDeg - bodyTiltDeg, -40.0F, 40.0F);
   }

   public static float jetBodyPitch(Entity avatar) {
      AwakenedAnimations.JetPoseState s = jetPoseState(avatar);
      return s != null && s.lit ? 4.0F + s.lean : 0.0F;
   }

   public static float jetBodySink(Entity avatar) {
      AwakenedAnimations.JetPoseState s = jetPoseState(avatar);
      return s == null ? 0.0F : s.sink;
   }

   public static float jetArmPitch(Entity avatar) {
      AwakenedAnimations.JetPoseState s = jetPoseState(avatar);
      return s != null && s.lit ? 24.0F + s.lean + jetArmCommonPitch(s) : 20.0F;
   }

   private static float jetArmCommonPitch(AwakenedAnimations.JetPoseState s) {
      return -4.0F * s.cruise + s.armPitchRate;
   }

   public static float jetArmSplay(Entity avatar) {
      AwakenedAnimations.JetPoseState s = jetPoseState(avatar);
      return 22.0F + (s != null && s.lit ? -17.0F * s.cruise : 0.0F);
   }

   public static float jetLegPitch(Entity avatar) {
      AwakenedAnimations.JetPoseState s = jetPoseState(avatar);
      return s != null && s.lit ? 6.0F + s.lean + 4.0F * s.cruise : 2.0F;
   }

   public static float jetLegSplay(Entity avatar) {
      AwakenedAnimations.JetPoseState s = jetPoseState(avatar);
      return 4.0F + (s != null && s.lit ? -3.0F * s.cruise : 0.0F);
   }

   public static float jetCruise(Entity avatar) {
      AwakenedAnimations.JetPoseState s = jetPoseState(avatar);
      return s != null && s.lit ? s.cruise : 0.0F;
   }

   public static void transitions(ResourceLocation animationId, int inTicks, EasingType inEase, int outTicks, EasingType outEase) {
      TRANSITIONS.put(animationId, new AwakenedAnimations.Transition(inTicks, inEase, outTicks, outEase));
   }

   public static void transitions(ResourceLocation animationId, int inTicks, int outTicks, EasingType ease) {
      transitions(animationId, inTicks, ease, outTicks, ease);
   }

   public static int transitionInTicks(ResourceLocation animationId) {
      AwakenedAnimations.Transition transition = animationId == null ? null : TRANSITIONS.get(animationId);
      return transition == null ? 0 : transition.inTicks();
   }

   public static int transitionOutTicks(ResourceLocation animationId) {
      AwakenedAnimations.Transition transition = animationId == null ? null : TRANSITIONS.get(animationId);
      return transition == null ? 0 : transition.outTicks();
   }

   public static EasingType transitionInEase(ResourceLocation animationId, EasingType fallback) {
      AwakenedAnimations.Transition transition = animationId == null ? null : TRANSITIONS.get(animationId);
      return transition != null && transition.inEase() != null ? transition.inEase() : fallback;
   }

   public static EasingType transitionOutEase(ResourceLocation animationId, EasingType fallback) {
      AwakenedAnimations.Transition transition = animationId == null ? null : TRANSITIONS.get(animationId);
      return transition != null && transition.outEase() != null ? transition.outEase() : fallback;
   }

   private static float windowUnwind(int windowTicks) {
      return windowTicks <= 0 ? 1.0F : 1.0F - (float)Math.pow(0.05F, 1.0 / (double)windowTicks);
   }

   public static int clipGeneration(int entityId) {
      ClipBearer bearer = ClipBearers.get(entityId);
      return bearer != null ? bearer.clipSequence() : CLIP_GENERATION.getOrDefault(entityId, 0);
   }

   public static boolean isTriggered(int entityId, ResourceLocation animationId) {
      return animationId.equals(ACTIVE_TRIGGERED.get(entityId));
   }

   private static ResourceLocation triggeredClip(int entityId) {
      ClipBearer bearer = ClipBearers.get(entityId);
      return bearer != null ? bearer.activeClip() : ACTIVE_TRIGGERED.get(entityId);
   }

   @Nullable
   public static PartModifier thirdPersonModifier(String boneName, LivingEntity entity, float partial) {
      Optional<PartModifier> aim = aimedChannelBone(boneName, entity, false, partial);
      Optional<PartModifier> jet = jetFlightBone(boneName, entity, false, partial);
      if (aim.isEmpty()) {
         return jet.orElse(null);
      } else {
         return jet.isEmpty() ? aim.get() : new PartModifier(aim.get().rotation().add(jet.get().rotation()), aim.get().offset().add(jet.get().offset()));
      }
   }

   public static boolean isPlaying(int entityId, ResourceLocation animationId) {
      ClipBearer bearer = ClipBearers.get(entityId);
      if (bearer != null) {
         return animationId.equals(bearer.activeClip());
      } else if (!isTriggered(entityId, animationId)) {
         return false;
      } else {
         PlayerAnimationController controller = controllerOf(entityId);
         return controller != null && controller.isPlayingTriggeredAnimation();
      }
   }

   public static float clipSeconds(int entityId) {
      PlayerAnimationController controller = controllerOf(entityId);
      return controller != null && controller.isPlayingTriggeredAnimation() ? controller.getAnimationTicks() / 20.0F : -1.0F;
   }

   @SubscribeEvent
   public static void onEntityLeave(EntityLeaveLevelEvent event) {
      if (event.getLevel().isClientSide()) {
         int id = event.getEntity().getId();
         ACTIVE_TRIGGERED.remove(id);
         FADING_OUT.remove(id);
         CLIP_GENERATION.remove(id);
         JET_MEMO.remove(id);
         JET_FLIGHT.remove(id);
         AIM_FOLLOW.remove(id);
      }
   }

   public static void addPoseModifier(AwakenedAnimations.PoseModifier modifier) {
      POSE_MODIFIERS.add(modifier);
   }

   private static AdjustmentModifier wrap(AwakenedAnimations.PoseModifier modifier, AbstractClientPlayer avatar) {
      return new AdjustmentModifier(
         boneName -> modifier.bone(
               boneName,
               avatar,
               FirstPersonMode.isFirstPersonPass() && avatar == Minecraft.getInstance().player,
               Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)
            )
      );
   }

   private AwakenedAnimations() {
   }

   public static void register() {
      PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, 1000, avatar -> {
         PlayerAnimationController controller = new PlayerAnimationController(avatar, (c, data, setter) -> PlayState.STOP);
         controller.setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);
         controller.setFirstPersonConfigurationHandler(ctrl -> firstPersonConfig(avatar));
         controller.addModifierLast(firstPersonFollow(avatar));
         controller.addModifierLast(glideDiveTuck(avatar));
         controller.addModifierLast(jetBodyDynamics(avatar));
         controller.addModifierLast(aimedChannelFollow(avatar));
         controller.addModifierLast(jetFlightPose(avatar));
         controller.addModifierLast(aeroSurfStance(avatar));

         for (AwakenedAnimations.PoseModifier modifier : POSE_MODIFIERS) {
            controller.addModifierLast(wrap(modifier, avatar));
         }

         return controller;
      });
   }

   private static FirstPersonConfiguration firstPersonConfig(Entity avatar) {
      boolean local = avatar == Minecraft.getInstance().player;
      boolean hideLeft = local && SpiderSwingController.zipPoseActive();
      return new FirstPersonConfiguration().setShowRightArm(true).setShowLeftArm(!hideLeft).setShowRightItem(true).setShowLeftItem(!hideLeft);
   }

   private static boolean suitedWithGauntlets(Entity avatar) {
      ExoVisual v = (ExoVisual)avatar.getData(AwakenedAttachments.EXO_VISUAL);
      return v.suited() && (v.builtMask() & ExoPiece.GAUNTLETS.bit()) != 0;
   }

   @SubscribeEvent
   public static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null) {
         ClipClock.observeAll(mc.level.entitiesForRendering());
      }

      LocalPlayer layer = mc.player;
      if (layer instanceof AbstractClientPlayer) {
         AbstractClientPlayer avatar = layer;
         pruneFadingOut(mc);
         tickHeldLoops(mc);
         pruneWhileLayerIdle(SURF_POSE);
         pruneWhileLayerIdle(JET_FLIGHT);
         pruneWhileLayerIdle(AIM_FOLLOW);

         try {
            if (PlayerAnimationAccess.getPlayerAnimationLayer(avatar, LAYER) instanceof PlayerAnimationController controller) {
               boolean scythe = heldScythe(mc.player);
               controller.setFirstPersonMode(firstPersonVanilla(mc.player) ? FirstPersonMode.NONE : FirstPersonMode.THIRD_PERSON_MODEL);
               if (heldScytheLastTick && !scythe) {
                  controller.stopTriggeredAnimation();
               }

               heldScytheLastTick = scythe;
            }
         } catch (IllegalArgumentException var6) {
         }
      } else {
         heldScytheLastTick = false;
         ACTIVE_TRIGGERED.clear();
         SURF_POSE.clear();
         JET_FLIGHT.clear();
         AIM_FOLLOW.clear();
         FADING_OUT.clear();
         CLIP_GENERATION.clear();
         JET_MEMO.clear();
      }
   }

   private static boolean heldScythe(Player player) {
      return player.getMainHandItem().getItem() instanceof ReaperScytheItem;
   }

   private static boolean firstPersonVanilla(Player player) {
      int id = player.getId();
      ResourceLocation active = ACTIVE_TRIGGERED.get(id);
      return heldScythe(player)
         || suitedWithGauntlets(player)
         || SpiderSwingController.gliding()
         || FADING_OUT.containsKey(id)
         || active != null && FP_VANILLA_CLIPS.contains(active)
         || vanillaFirstPersonClip(player);
   }

   private static AdjustmentModifier firstPersonFollow(AbstractClientPlayer avatar) {
      return new AdjustmentModifier(boneName -> {
         if (FirstPersonMode.isFirstPersonPass() && avatar == Minecraft.getInstance().player) {
            float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
            if ("body".equals(boneName)) {
               float camYaw = avatar.getViewYRot(partial);
               float bodyYaw = Mth.rotLerp(partial, avatar.yBodyRotO, avatar.yBodyRot);
               float yaw = (float)Math.toRadians((double)Mth.wrapDegrees(camYaw - bodyYaw)) * 1.0F;
               return Optional.of(new PartModifier(new Vec3f(0.0F, yaw, 0.0F), Vec3f.ZERO));
            } else if (!"right_arm".equals(boneName) && !"left_arm".equals(boneName)) {
               return Optional.empty();
            } else if (!isGliding(avatar) && !isExoJet(avatar) && !aeroSurfPoseActive(avatar)) {
               float pitch = rad(firstPersonArmPitch(avatar.getViewXRot(partial)));
               return Optional.of(new PartModifier(new Vec3f(pitch, 0.0F, 0.0F), Vec3f.ZERO));
            } else {
               return Optional.empty();
            }
         } else {
            return Optional.empty();
         }
      });
   }

   private static AdjustmentModifier aimedChannelFollow(AbstractClientPlayer avatar) {
      return new AdjustmentModifier(
         boneName -> aimedChannelBone(
               boneName,
               avatar,
               FirstPersonMode.isFirstPersonPass() && avatar == Minecraft.getInstance().player,
               Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)
            )
      );
   }

   private static AwakenedAnimations.AimFollow aimFollowState(Entity avatar) {
      ResourceLocation active = triggeredClip(avatar.getId());
      boolean aiming = active != null && AIM_FOLLOW_ANIMS.contains(active);
      int id = avatar.getId();
      AwakenedAnimations.AimFollow s = AIM_FOLLOW.get(id);
      if (s == null) {
         if (!aiming) {
            return null;
         }

         s = new AwakenedAnimations.AimFollow();
         AIM_FOLLOW.put(id, s);
      }

      if (avatar.tickCount != s.tick) {
         s.tick = avatar.tickCount;
         s.age = AIM_CLOCK.age(avatar);
         float yawRate = Mth.wrapDegrees(avatar.getYRot() - avatar.yRotO);
         if (aiming) {
            s.clip = active;
            s.hands = AIM_HAND_CHANNELS.contains(active);
            s.weight = s.weight + (1.0F - s.weight) * windowUnwind(transitionInTicks(active));
            s.yawLag = s.yawLag + (yawRate - s.yawLag) * 0.14F;
         } else {
            float unwind = windowUnwind(transitionOutTicks(s.clip));
            s.weight = s.weight + (0.0F - s.weight) * unwind;
            s.yawLag = s.yawLag + (0.0F - s.yawLag) * unwind;
            if (s.weight < 0.005F) {
               AIM_FOLLOW.remove(id);
               return null;
            }
         }
      }

      return s;
   }

   private static float aimPulseKick(AwakenedAnimations.AimFollow s) {
      if (s.age < 0L) {
         return 0.0F;
      } else {
         AwakenedAnimations.PulseBeat beat = s.clip == null ? AIM_PULSE_DEFAULT : AIM_PULSE_BEATS.getOrDefault(s.clip, AIM_PULSE_DEFAULT);
         long phase = s.age % (long)beat.period();
         if (phase >= (long)beat.decay()) {
            return 0.0F;
         } else {
            float effort = Mth.clamp((float)s.age / beat.effort(), 0.0F, 1.0F);
            return beat.kick() * effort * (1.0F - (float)phase / (float)beat.decay());
         }
      }
   }

   private static Optional<PartModifier> aimedChannelBone(String boneName, LivingEntity avatar, boolean firstPerson, float partial) {
      AwakenedAnimations.AimFollow s = aimFollowState(avatar);
      if (s != null && !firstPerson) {
         float w = s.weight;
         float p = avatar.getViewXRot(partial);
         boolean crouching = avatar.isCrouching();
         float tilt = aimedChannelBodyPitch(p, crouching);
         switch (boneName) {
            case "body":
               float camYaw = avatar.getViewYRot(partial);
               float bodyYaw = Mth.rotLerp(partial, avatar.yBodyRotO, avatar.yBodyRot);
               float yaw = (float)Math.toRadians((double)Mth.wrapDegrees(camYaw - bodyYaw)) * w;
               return Optional.of(new PartModifier(new Vec3f(rad(tilt * w), yaw, 0.0F), Vec3f.ZERO));
            case "torso":
               if (!s.hands) {
                  return Optional.empty();
               }

               float lag = Mth.clamp(-s.yawLag * 1.5F, -12.0F, 12.0F);
               return Optional.of(new PartModifier(new Vec3f(0.0F, rad(lag * w), 0.0F), Vec3f.ZERO));
            case "right_arm":
            case "left_arm":
               if (!s.hands) {
                  return Optional.empty();
               }

               float arm = (aimedChannelArmResidual(p, tilt) + aimPulseKick(s)) * w;
               return Optional.of(new PartModifier(new Vec3f(rad(arm), 0.0F, 0.0F), Vec3f.ZERO));
            case "head":
               float headX = Mth.clamp(p - tilt, -60.0F, 50.0F);
               return Optional.of(new PartModifier(new Vec3f(rad(headX * w), 0.0F, 0.0F), Vec3f.ZERO));
            default:
               return Optional.empty();
         }
      } else {
         return Optional.empty();
      }
   }

   private static AdjustmentModifier glideDiveTuck(AbstractClientPlayer avatar) {
      return new AdjustmentModifier(boneName -> tuckBone(boneName, avatar, avatar.getXRot()));
   }

   private static boolean isGliding(Entity avatar) {
      return avatar == Minecraft.getInstance().player
         ? SpiderSwingController.gliding()
         : ((SpiderVisual)avatar.getData(AwakenedAttachments.SPIDER_VISUAL)).gliding();
   }

   public static boolean isJetting(int entityId) {
      Minecraft mc = Minecraft.getInstance();
      long now = mc.level == null ? Long.MIN_VALUE : mc.level.getGameTime();
      if (now != jetMemoTime) {
         jetMemoTime = now;
         JET_MEMO.clear();
      }

      Boolean cached = JET_MEMO.get(entityId);
      if (cached != null) {
         return cached;
      } else {
         boolean lit = isPlaying(entityId, FIRE_JETS_ANIM) || isPlaying(entityId, FIRE_JETS_TAP_ANIM);
         JET_MEMO.put(entityId, lit);
         return lit;
      }
   }

   private static boolean isFireJetting(Entity avatar) {
      return isJetting(avatar.getId());
   }

   private static boolean isExoJet(Entity avatar) {
      return ((ExoVisual)avatar.getData(AwakenedAttachments.EXO_VISUAL)).jet();
   }

   private static boolean isAeroGliding(Entity avatar) {
      return ((AeroVisual)avatar.getData(AwakenedAttachments.AERO_VISUAL)).gliding();
   }

   private static boolean aeroSurfPoseActive(Entity avatar) {
      return isAeroGliding(avatar) && isTriggered(avatar.getId(), AwId.of("aero_glide"));
   }

   private static void pruneWhileLayerIdle(Map<Integer, ?> states) {
      if (!states.isEmpty()) {
         states.keySet().removeIf(id -> {
            PlayerAnimationController controller = controllerOf(id);
            return controller == null ? ClipBearers.get(id) == null : !controller.isActive();
         });
      }
   }

   private static void pruneFadingOut(Minecraft mc) {
      if (!FADING_OUT.isEmpty() && mc.level != null) {
         long now = mc.level.getGameTime();
         FADING_OUT.values().removeIf(until -> now >= until);
      }
   }

   private static AdjustmentModifier aeroSurfStance(AbstractClientPlayer avatar) {
      return new AdjustmentModifier(boneName -> surfPoseBone(boneName, avatar, FirstPersonMode.isFirstPersonPass()));
   }

   private static AwakenedAnimations.SurfPose surfState(Entity avatar) {
      boolean surfing = aeroSurfPoseActive(avatar);
      int id = avatar.getId();
      AwakenedAnimations.SurfPose s = SURF_POSE.get(id);
      if (s == null) {
         if (!surfing) {
            return null;
         }

         s = new AwakenedAnimations.SurfPose();
         SURF_POSE.put(id, s);
      }

      if (avatar.tickCount != s.tick) {
         s.tick = avatar.tickCount;
         float twistT = surfing ? 60.0F : 0.0F;
         float yawRate = Mth.wrapDegrees(avatar.getYRot() - avatar.yRotO);
         float bankT = surfing ? Mth.clamp(-yawRate * 4.0F, -28.0F, 28.0F) : 0.0F;
         s.twist = s.twist + (twistT - s.twist) * 0.18F;
         s.bank = s.bank + (bankT - s.bank) * 0.18F;
         if (!surfing && Math.abs(s.twist) < 0.05F && Math.abs(s.bank) < 0.05F) {
            SURF_POSE.remove(id);
            return null;
         }
      }

      return s;
   }

   public static float surfBankDeg(int entityId) {
      AwakenedAnimations.SurfPose s = SURF_POSE.get(entityId);
      return s == null ? 0.0F : s.bank;
   }

   private static Optional<PartModifier> surfPoseBone(String boneName, Entity avatar, boolean firstPerson) {
      AwakenedAnimations.SurfPose s = surfState(avatar);
      if (s == null || firstPerson) {
         return Optional.empty();
      } else if ("body".equals(boneName)) {
         double t = Math.toRadians((double)s.twist);
         float bankX = (float)(-Math.sin(t) * (double)s.bank);
         float bankZ = (float)(Math.cos(t) * (double)s.bank);
         return Optional.of(new PartModifier(new Vec3f(rad(bankX), rad(s.twist), rad(bankZ)), Vec3f.ZERO));
      } else {
         return "head".equals(boneName) ? Optional.of(new PartModifier(new Vec3f(0.0F, -rad(s.twist), 0.0F), Vec3f.ZERO)) : Optional.empty();
      }
   }

   private static Optional<PartModifier> tuckBone(String boneName, Entity avatar, float pitchDeg) {
      if (!isGliding(avatar)) {
         return Optional.empty();
      } else if ("head".equals(boneName)) {
         float headPitch = (float)Math.toRadians((double)(pitchDeg * 0.45F));
         return Optional.of(new PartModifier(new Vec3f(headPitch, 0.0F, 0.0F), Vec3f.ZERO));
      } else {
         float tuck = SpiderSwingController.glideTuck(pitchDeg, avatar.getDeltaMovement().length());
         if (tuck <= 0.0F) {
            return Optional.empty();
         } else {
            float armZ = (float)Math.toRadians((double)tuck * 48.0);
            float legZ = (float)Math.toRadians((double)tuck * 16.0);

            return switch (boneName) {
               case "right_arm" -> Optional.of(new PartModifier(new Vec3f(0.0F, 0.0F, -armZ), Vec3f.ZERO));
               case "left_arm" -> Optional.of(new PartModifier(new Vec3f(0.0F, 0.0F, armZ), Vec3f.ZERO));
               case "right_leg" -> Optional.of(new PartModifier(new Vec3f(0.0F, 0.0F, -legZ), Vec3f.ZERO));
               case "left_leg" -> Optional.of(new PartModifier(new Vec3f(0.0F, 0.0F, legZ), Vec3f.ZERO));
               default -> Optional.empty();
            };
         }
      }
   }

   private static float jetPitchSource(AwakenedAnimations.JetPoseState s, float speed, float lookPitch) {
      float w = Mth.clamp(speed / 0.25F, 0.0F, 1.0F);
      float flightPitch = (float)Math.toDegrees(-Math.asin((double)Mth.clamp(s.dirY, -1.0F, 1.0F)));
      return Mth.lerp(w, lookPitch * 0.35F, flightPitch);
   }

   public static Vec3 jetExhaustAim(Entity avatar, Vec3 look) {
      AwakenedAnimations.JetPoseState s = jetPoseState(avatar);
      return s != null && s.lit && !(s.speed < 0.25F) ? new Vec3((double)(-s.dirX), (double)(-s.dirY), (double)(-s.dirZ)) : look.scale(-1.0);
   }

   private static float softClamp(float value, float min, float max, float soft) {
      if (value > max - soft) {
         return max - soft + soft * (1.0F - (float)Math.exp((double)(-(value - (max - soft)) / soft)));
      } else {
         return value < min + soft ? min + soft - soft * (1.0F - (float)Math.exp((double)(-(min + soft - value) / soft))) : value;
      }
   }

   private static float jetEase(float current, float target, float shrink) {
      float rate = Math.abs(target) >= Math.abs(current) ? 0.25F : shrink;
      return current + (target - current) * rate;
   }

   private static int jetUnwindWindow(int entityId) {
      return FIRE_JETS_LAND_ANIM.equals(triggeredClip(entityId)) ? 9 : transitionOutTicks(FIRE_JETS_ANIM);
   }

   private static float jetTapBodyAuthored(float age) {
      if (age <= 0.0F) {
         return 0.0F;
      } else if (age < 2.4F) {
         return Mth.lerp(age / 2.4F, 0.0F, -9.0F);
      } else if (age < 4.0F) {
         return Mth.lerp((age - 2.4F) / 1.6F, -9.0F, 30.0F);
      } else if (age < 6.0F) {
         return Mth.lerp((age - 4.0F) / 2.0F, 30.0F, 4.0F);
      } else if (age < 9.0F) {
         return 4.0F;
      } else {
         return age < 14.0F ? Mth.lerp((age - 9.0F) / 5.0F, 4.0F, 0.0F) : 0.0F;
      }
   }

   private static AdjustmentModifier jetFlightPose(AbstractClientPlayer avatar) {
      return new AdjustmentModifier(
         boneName -> jetFlightBone(
               boneName,
               avatar,
               FirstPersonMode.isFirstPersonPass() && avatar == Minecraft.getInstance().player,
               Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)
            )
      );
   }

   private static AwakenedAnimations.JetPoseState jetPoseState(Entity avatar) {
      boolean lit = isFireJetting(avatar);
      int entityId = avatar.getId();
      AwakenedAnimations.JetPoseState s = JET_FLIGHT.get(entityId);
      if (s == null) {
         if (!lit) {
            return null;
         }

         s = new AwakenedAnimations.JetPoseState();
         JET_FLIGHT.put(entityId, s);
      }

      if (avatar.tickCount != s.tick) {
         boolean first = s.tick == Integer.MIN_VALUE;
         s.tick = avatar.tickCount;
         s.age = lit && s.lit ? s.age + 1.0F : 0.0F;
         s.lit = lit;
         s.hold = lit && isPlaying(entityId, FIRE_JETS_ANIM);
         float vx = (float)(avatar.getX() - avatar.xo);
         float vy = (float)(avatar.getY() - avatar.yo);
         float vz = (float)(avatar.getZ() - avatar.zo);
         float speed = Mth.sqrt(vx * vx + vy * vy + vz * vz);
         float sT = Mth.clamp(speed / 1.3F, 0.0F, 1.0F);
         float sE = sT * sT * (3.0F - 2.0F * sT);
         float look = Mth.clamp(avatar.getXRot(), -90.0F, 90.0F);
         if (speed > 1.0E-4F) {
            s.dirX = vx / speed;
            s.dirY = vy / speed;
            s.dirZ = vz / speed;
         } else {
            float lookRad = (float)Math.toRadians((double)look);
            float yawRad0 = (float)Math.toRadians((double)avatar.getYRot());
            float cosP = Mth.cos(lookRad);
            s.dirX = -Mth.sin(yawRad0) * cosP;
            s.dirY = -Mth.sin(lookRad);
            s.dirZ = Mth.cos(yawRad0) * cosP;
         }

         float shrink;
         float var10000;
         label118: {
            float total = Mth.clamp(4.0F + sE * (74.0F + 0.8F * jetPitchSource(s, speed, look)), -20.0F, 115.0F);
            float yawRate = Mth.wrapDegrees(avatar.getYRot() - avatar.yRotO);
            float banked = Math.abs(yawRate) < 0.25F ? 0.0F : yawRate;
            float bank = Mth.clamp(-banked * 4.5F, -25.0F, 25.0F);
            float pitchRate = Mth.wrapDegrees(avatar.getXRot() - avatar.xRotO);
            float armRateT = lit ? Mth.clamp(pitchRate * 2.0F, -12.0F, 12.0F) : 0.0F;
            float yawRad = (float)Math.toRadians((double)avatar.getYRot());
            float lateral = vx * -Mth.cos(yawRad) + vz * -Mth.sin(yawRad);
            float latRoll = Mth.clamp(lateral / 0.18F, -1.0F, 1.0F) * 40.0F;
            float rollT = lit ? Mth.clamp(bank + latRoll, -50.0F, 50.0F) : 0.0F;
            float headYawT = lit ? Mth.clamp(yawRate * 3.5F, -18.0F, 18.0F) : 0.0F;
            float accel = first ? 0.0F : speed - s.speed;
            float tuckT = s.hold ? Mth.clamp(accel / 0.08F, 0.0F, 1.0F) : 0.0F;
            float hoverT = s.hold ? Mth.clamp(2.0F - speed / 0.15F, 0.0F, 1.0F) : 0.0F;
            float cruiseT = s.hold ? sE : 0.0F;
            s.speed = speed;
            shrink = lit ? 0.08F : windowUnwind(jetUnwindWindow(entityId));
            float leanT = lit ? total - 4.0F : 0.0F;
            s.lean = s.lean + (leanT - s.lean) * (lit ? 0.5F : shrink);
            s.roll = jetEase(s.roll, rollT, shrink);
            s.headYaw = jetEase(s.headYaw, headYawT, shrink);
            s.tuck = jetEase(s.tuck, tuckT, shrink);
            s.hover = jetEase(s.hover, hoverT, shrink);
            s.cruise = jetEase(s.cruise, cruiseT, shrink);
            s.armPitchRate = jetEase(s.armPitchRate, armRateT, shrink);
            if (s.hold && avatar instanceof Player pl && ProneFlight.holds(pl)) {
               var10000 = jetSinkTarget(4.0F + s.lean, avatar.getBbHeight() * 0.5F);
               break label118;
            }

            var10000 = 0.0F;
         }

         float sinkT = var10000;
         s.sink = s.sink + (sinkT - s.sink) * (lit ? 0.5F : shrink);
         if (Math.abs(s.sink) < 0.002F) {
            s.sink = 0.0F;
            s.sinkOffset = Vec3f.ZERO;
         } else {
            s.sinkOffset = new Vec3f(0.0F, s.sink * 16.0F, 0.0F);
         }

         if (!lit
            && Math.abs(s.lean) < 0.05F
            && Math.abs(s.roll) < 0.05F
            && Math.abs(s.headYaw) < 0.05F
            && Math.abs(s.cruise) < 0.005F
            && Math.abs(s.armPitchRate) < 0.05F
            && s.sink == 0.0F) {
            JET_FLIGHT.remove(entityId);
            return null;
         }
      }

      return s;
   }

   private static Optional<PartModifier> jetFlightBone(String boneName, LivingEntity avatar, boolean firstPerson, float partial) {
      AwakenedAnimations.JetPoseState s = jetPoseState(avatar);
      if (s != null && !firstPerson) {
         float a = s.roll / 50.0F;
         float left = Math.max(a, 0.0F);
         float right = Math.max(-a, 0.0F);
         float c = s.cruise;
         switch (boneName) {
            case "body":
               float yaw = 0.0F;
               if (s.lit) {
                  float camYaw = avatar.getViewYRot(partial);
                  float bodyYaw = Mth.rotLerp(partial, avatar.yBodyRotO, avatar.yBodyRot);
                  yaw = (float)Math.toRadians((double)Mth.wrapDegrees(camYaw - bodyYaw));
               }

               float phase = ((float)avatar.tickCount + partial) * 0.14279968F;
               float bobX = s.hover * 1.5F * Mth.sin(phase);
               float bobZ = s.hover * 1.0F * Mth.cos(phase);
               float bank = s.roll + bobZ;
               float authored = jetAuthoredBodyPitch(s, partial);
               return Optional.of(jetRootBone(yaw, rad(bank), rad(authored + s.lean + bobX), rad(authored), s.sinkOffset));
            case "head":
               if (!s.lit) {
                  return Optional.empty();
               }

               return Optional.of(jetHeadBone(avatar, s, partial));
            case "right_arm":
            case "left_arm":
               if (!s.hold) {
                  return Optional.empty();
               }

               boolean rightArm = "right_arm".equals(boneName);
               float outside = rightArm ? left : right;
               float inside = rightArm ? right : left;
               float pitch = jetArmCommonPitch(s) + -4.0F * outside + 10.0F * inside;
               float splayNow = 22.0F + -17.0F * c;
               float tuck = Math.min(12.0F, splayNow) * inside;
               float armPhase = ((float)avatar.tickCount + partial) * 0.23271057F + (rightArm ? 0.0F : (float) Math.PI);
               float breath = s.hover * 1.2F * Mth.sin(armPhase);
               float out = -17.0F * c + 14.0F * outside - tuck + breath;
               float z = rightArm ? out : -out;
               return Optional.of(new PartModifier(new Vec3f(rad(pitch), 0.0F, rad(z)), Vec3f.ZERO));
            case "right_leg":
            case "left_leg":
               if (!s.hold) {
                  return Optional.empty();
               }

               boolean rightLeg = "right_leg".equals(boneName);
               float outside = rightLeg ? left : right;
               float pitch = 4.0F * c + 3.0F * outside - 5.0F * s.tuck;
               float closeIn = 3.0F * c + 2.0F * s.tuck;
               float z = a * 6.0F + (rightLeg ? -closeIn : closeIn);
               return Optional.of(new PartModifier(new Vec3f(rad(pitch), 0.0F, rad(z)), Vec3f.ZERO));
            default:
               return Optional.empty();
         }
      } else {
         return Optional.empty();
      }
   }

   private static float jetAuthoredBodyPitch(AwakenedAnimations.JetPoseState s, float partial) {
      return s.hold ? 4.0F : jetTapBodyAuthored(s.age + partial);
   }

   private static PartModifier jetRootBone(float yaw, float bank, float lean, float authored, Vec3f offset) {
      float sy = Mth.sin(yaw);
      float cy = Mth.cos(yaw);
      float sb = Mth.sin(bank);
      float cb = Mth.cos(bank);
      float sl = Mth.sin(lean);
      float cl = Mth.cos(lean);
      float boneY = (float)Math.asin((double)Mth.clamp(sy * cb, -1.0F, 1.0F));
      float boneZ = (float)Math.atan2((double)sb, (double)(cy * cb));
      float boneX = (float)Math.atan2((double)(sy * sb * cl + cy * sl), (double)(cy * cl - sy * sb * sl));
      return new PartModifier(new Vec3f(boneX - authored, boneY, boneZ), offset);
   }

   private static float jetSinkTarget(float leanDeg, float boxCentre) {
      float c = Mth.cos(rad(leanDeg));
      float centred = boxCentre - 0.25F * c;
      float lowest = Math.max(0.75F * c, -1.25F * c) + 0.12F;
      return Math.min(0.0F, Math.max(centred, lowest) - 0.75F);
   }

   private static PartModifier jetHeadBone(LivingEntity avatar, AwakenedAnimations.JetPoseState s, float partial) {
      float bodyPitch = jetAuthoredBodyPitch(s, partial) + s.lean;
      float headX = softClamp(avatar.getViewXRot(partial) - bodyPitch, -85.0F, 60.0F, 12.0F);
      float leadYaw = s.headYaw;
      float levelRoll = -s.roll * 0.5F;
      float lean = (float)Math.toRadians((double)bodyPitch);
      float cos = Mth.cos(lean);
      float sin = Mth.sin(lean);
      return new PartModifier(new Vec3f(rad(headX), rad(leadYaw * cos + levelRoll * sin), rad(-leadYaw * sin + levelRoll * cos)), Vec3f.ZERO);
   }

   private static AdjustmentModifier jetBodyDynamics(AbstractClientPlayer avatar) {
      return new AdjustmentModifier(boneName -> flightPoseBone(boneName, avatar));
   }

   private static float rad(float deg) {
      return (float)Math.toRadians((double)deg);
   }

   private static float armSplay(float s) {
      return s <= 0.5F ? 12.0F + 12.0F * (s / 0.5F) : 24.0F + -19.0F * ((s - 0.5F) / 0.5F);
   }

   private static AwakenedAnimations.JetPose poseState(Entity avatar) {
      ExoVisual v = (ExoVisual)avatar.getData(AwakenedAttachments.EXO_VISUAL);
      boolean jet = v.jet();
      float p = Mth.clamp(avatar.getXRot(), -90.0F, 90.0F);
      int id = avatar.getId();
      AwakenedAnimations.JetPose b = JET_POSE.get(id);
      if (b == null) {
         if (!jet) {
            return null;
         }

         b = new AwakenedAnimations.JetPose();
         JET_POSE.put(id, b);
      }

      if (avatar.tickCount != b.tick) {
         b.tick = avatar.tickCount;
         float sT = jet ? Mth.clamp((float)v.flightSpeed() / 52.0F, 0.0F, 1.0F) : 0.0F;
         float bk = jet ? Mth.clamp((float)v.brakeLevel() / 255.0F, 0.0F, 1.0F) : 0.0F;
         float sPitch = jet ? Mth.clamp(sT * (85.0F + 1.0F * p), -20.0F, 160.0F) : 0.0F;
         float sSweep = jet ? 15.0F * sT : 0.0F;
         float sSplay = jet ? armSplay(sT) : 0.0F;
         float sLegSplay = jet ? 3.0F - 2.0F * sT : 0.0F;
         float brakePitch = Mth.clamp(-35.0F + 0.5F * p, -50.0F, 160.0F);
         float pitchT = Mth.clamp(Mth.lerp(bk, sPitch, brakePitch), -50.0F, 160.0F);
         float sweepT = Mth.lerp(bk, sSweep, -75.0F);
         float splayT = Mth.lerp(bk, sSplay, 16.0F);
         float legSplayT = Mth.lerp(bk, sLegSplay, 8.0F);
         float legPitchT = Mth.lerp(bk, 0.0F, -22.0F);
         float yawRate = Mth.wrapDegrees(avatar.getYRot() - avatar.yRotO);
         float rollT = jet ? Mth.clamp(-yawRate * 3.5F, -22.0F, 22.0F) : 0.0F;
         b.pitch = b.pitch + (pitchT - b.pitch) * 0.2F;
         b.roll = b.roll + (rollT - b.roll) * 0.2F;
         b.armSweep = b.armSweep + (sweepT - b.armSweep) * 0.2F;
         b.armSplay = b.armSplay + (splayT - b.armSplay) * 0.2F;
         b.legPitch = b.legPitch + (legPitchT - b.legPitch) * 0.2F;
         b.legSplay = b.legSplay + (legSplayT - b.legSplay) * 0.2F;
         b.look = p;
         if (!jet
            && Math.abs(b.pitch) < 0.05F
            && Math.abs(b.roll) < 0.05F
            && Math.abs(b.armSweep) < 0.05F
            && Math.abs(b.armSplay) < 0.05F
            && Math.abs(b.legPitch) < 0.05F
            && Math.abs(b.legSplay) < 0.05F) {
            JET_POSE.remove(id);
            return null;
         }
      }

      return b;
   }

   private static Optional<PartModifier> flightPoseBone(String boneName, Entity avatar) {
      AwakenedAnimations.JetPose b = poseState(avatar);
      if (b == null) {
         return Optional.empty();
      } else {
         return switch (boneName) {
            case "body" -> Optional.of(new PartModifier(new Vec3f(rad(b.pitch), 0.0F, rad(b.roll)), Vec3f.ZERO));
            case "head" -> {
               float headX = Mth.clamp(b.look - b.pitch, -90.0F, 45.0F);
               yield Optional.of(new PartModifier(new Vec3f(rad(headX), 0.0F, 0.0F), Vec3f.ZERO));
            }
            case "right_arm" -> Optional.of(new PartModifier(new Vec3f(rad(b.armSweep), 0.0F, rad(b.armSplay)), Vec3f.ZERO));
            case "left_arm" -> Optional.of(new PartModifier(new Vec3f(rad(b.armSweep), 0.0F, -rad(b.armSplay)), Vec3f.ZERO));
            case "right_leg" -> Optional.of(new PartModifier(new Vec3f(rad(b.legPitch), 0.0F, rad(b.legSplay)), Vec3f.ZERO));
            case "left_leg" -> Optional.of(new PartModifier(new Vec3f(rad(b.legPitch), 0.0F, -rad(b.legSplay)), Vec3f.ZERO));
            default -> Optional.empty();
         };
      }
   }

   public static void holdLoop(ResourceLocation animationId, Predicate<Player> whileHeld) {
      HELD_LOOPS.put(animationId, whileHeld);
   }

   public static void vanillaFirstPerson(ResourceLocation animationId, Predicate<Player> keepFirstPerson) {
      VANILLA_FIRST_PERSON.put(animationId, keepFirstPerson);
   }

   private static boolean vanillaFirstPersonClip(Player local) {
      if (VANILLA_FIRST_PERSON.isEmpty()) {
         return false;
      } else {
         ResourceLocation active = ACTIVE_TRIGGERED.get(local.getId());
         Predicate<Player> keepFirstPerson = active == null ? null : VANILLA_FIRST_PERSON.get(active);
         return keepFirstPerson != null && !keepFirstPerson.test(local);
      }
   }

   public static boolean libraryOwnsFirstPerson() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         return false;
      } else {
         PlayerAnimationController controller = controllerOf(mc.player.getId());
         return controller != null && controller.isActive() && controller.getFirstPersonMode() != FirstPersonMode.NONE;
      }
   }

   private static void tickHeldLoops(Minecraft mc) {
      if (!HELD_LOOPS.isEmpty() && mc.level != null) {
         for (Player player : mc.level.players()) {
            int id = player.getId();

            for (Entry<ResourceLocation, Predicate<Player>> held : HELD_LOOPS.entrySet()) {
               if (!held.getValue().test(player)) {
                  if (isTriggered(id, held.getKey())) {
                     stop(id);
                  }
               } else {
                  PlayerAnimationController controller = controllerOf(id);
                  if (controller != null && !controller.isPlayingTriggeredAnimation()) {
                     play(id, held.getKey());
                  }
               }
            }
         }
      }
   }

   private static PlayerAnimationController controllerOf(int entityId) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         return null;
      } else if (mc.level.getEntity(entityId) instanceof AbstractClientPlayer avatar) {
         try {
            return PlayerAnimationAccess.getPlayerAnimationLayer(avatar, LAYER) instanceof PlayerAnimationController controller ? controller : null;
         } catch (IllegalArgumentException var6) {
            return null;
         }
      } else {
         return null;
      }
   }

   public static void play(int entityId, ResourceLocation animationId) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null) {
         if (mc.level.getEntity(entityId) instanceof AbstractClientPlayer avatar) {
            play(avatar, animationId);
         }
      }
   }

   public static void play(AbstractClientPlayer avatar, ResourceLocation animationId) {
      if (PlayerAnimResources.hasAnimation(animationId)) {
         Minecraft mc = Minecraft.getInstance();

         try {
            if (PlayerAnimationAccess.getPlayerAnimationLayer(avatar, LAYER) instanceof PlayerAnimationController controller) {
               AwakenedAnimations.Transition transition = TRANSITIONS.get(animationId);
               if (transition != null && transition.inTicks() > 0) {
                  controller.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(transition.inTicks(), transition.inEase()), animationId, true);
               } else {
                  controller.triggerAnimation(animationId);
               }

               int id = avatar.getId();
               if (animationId.equals(ACTIVE_TRIGGERED.get(id))) {
                  CLIP_GENERATION.merge(id, 1, Integer::sum);
               }

               ACTIVE_TRIGGERED.put(id, animationId);
               FADING_OUT.remove(id);
               JET_MEMO.remove(id);
               if (mc.player != null && mc.player.getId() == id) {
                  controller.setFirstPersonMode(firstPersonVanilla(mc.player) ? FirstPersonMode.NONE : FirstPersonMode.THIRD_PERSON_MODEL);
               }
            }
         } catch (IllegalArgumentException var7) {
         }
      }
   }

   public static void stop(int entityId) {
      if (!FADING_OUT.containsKey(entityId)) {
         ResourceLocation stopped = ACTIVE_TRIGGERED.remove(entityId);
         JET_MEMO.remove(entityId);
         PlayerAnimationController controller = controllerOf(entityId);
         if (controller == null) {
            FADING_OUT.remove(entityId);
         } else {
            AwakenedAnimations.Transition transition = stopped == null ? null : TRANSITIONS.get(stopped);
            if (transition != null && transition.outTicks() > 0 && controller.isActive()) {
               controller.replaceAnimationWithFade(
                  AbstractFadeModifier.standardFadeIn(transition.outTicks(), transition.outEase()), RawAnimation.begin().thenWait(transition.outTicks()), true
               );
               Minecraft mc = Minecraft.getInstance();
               if (mc.level != null) {
                  FADING_OUT.put(entityId, mc.level.getGameTime() + (long)transition.outTicks());
               }

               if (mc.player != null && mc.player.getId() == entityId) {
                  controller.setFirstPersonMode(FirstPersonMode.NONE);
               }
            } else {
               FADING_OUT.remove(entityId);
               controller.stopTriggeredAnimation();
            }
         }
      }
   }

   public static void logLoadedOnce() {
      if (!loggedLoaded) {
         loggedLoaded = true;
         Awakened.LOGGER.debug("Player animations loaded: {}", PlayerAnimResources.getAnimations().keySet());
      }
   }

   private static final class AimFollow {
      float weight;
      float yawLag;
      long age = -1L;
      boolean hands;
      ResourceLocation clip;
      int tick = Integer.MIN_VALUE;
   }

   private static final class JetPose {
      float pitch;
      float roll;
      float armSweep;
      float armSplay;
      float legPitch;
      float legSplay;
      float look;
      int tick = Integer.MIN_VALUE;
   }

   private static final class JetPoseState {
      float lean;
      float roll;
      float headYaw;
      float tuck;
      float hover;
      float cruise;
      float armPitchRate;
      float speed;
      float dirX;
      float dirY;
      float dirZ;
      float age;
      float sink;
      Vec3f sinkOffset = Vec3f.ZERO;
      boolean lit;
      boolean hold;
      int tick = Integer.MIN_VALUE;
   }

   @FunctionalInterface
   public interface PoseModifier {
      Optional<PartModifier> bone(String var1, Entity var2, boolean var3, float var4);
   }

   private static record PulseBeat(int period, int decay, float effort, float kick) {
      static final AwakenedAnimations.PulseBeat NONE = new AwakenedAnimations.PulseBeat(1, 1, 1.0F, 0.0F);
   }

   private static final class SurfPose {
      float twist;
      float bank;
      int tick = Integer.MIN_VALUE;
   }

   private static record Transition(int inTicks, EasingType inEase, int outTicks, EasingType outEase) {
   }
}
