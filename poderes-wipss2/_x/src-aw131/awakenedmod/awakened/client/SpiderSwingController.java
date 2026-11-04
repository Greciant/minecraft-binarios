package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.spider.WebStrikeAbility;
import com.awakenedmod.awakened.content.spider.WebWingsAbility;
import com.awakenedmod.awakened.content.spider.WebZipAbility;
import com.awakenedmod.awakened.core.RealisticMovement;
import com.awakenedmod.awakened.core.net.AbilityFeedbackPayload;
import com.awakenedmod.awakened.core.net.SpiderStrikePayload;
import com.awakenedmod.awakened.core.net.SpiderSwingPayload;
import com.awakenedmod.awakened.core.net.SpiderZipPayload;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.network.PacketDistributor;

public final class SpiderSwingController {
   private static final double MAX_RANGE = 40.0;
   private static final double MIN_ROPE = 4.0;
   private static final double MAX_ROPE = 32.0;
   private static final double MIN_ANCHOR_DY = -1.5;
   private static final double SKY_MIN_UP = 0.45;
   private static final double SKY_BASE = 18.0;
   private static final double SKY_PER_SPEED = 7.0;
   private static final double SKY_MAX = 34.0;
   private static final double SKY_HANG_UP = 22.0;
   private static final double SUBSTEP_DIST = 0.1;
   private static final int MIN_SUBSTEPS = 6;
   private static final int MAX_SUBSTEPS = 40;
   private static final double GRAVITY = 0.07;
   private static final double PUMP = 0.018;
   private static final double DRAG_REAL = 0.9995;
   private static final double DRAG_SKY = 0.998;
   private static final double ARC_PUMP_FALLOFF = 0.55;
   private static final double ARC_DRAG_BITE = 0.0012;
   private static final double ARC_DRAG_FLOOR = 0.985;
   private static final int ARC_MAX_DECAY = 8;
   private static final double MAX_SPEED = 2.0;
   private static final double RELEASE_BASE = 1.15;
   private static final double RELEASE_CLEAN = 1.4;
   private static final double RELEASE_LIFT = 0.2;
   private static final double CLEAN_CUE = 0.75;
   private static double capBoost = 1.0;
   private static final double GROUND_START_CLEAR = 3.0;
   private static final double GROUND_START_SPEED = 0.6;
   private static final int HAUL_TICKS = 11;
   private static final double HAUL_ACCEL = 0.12;
   private static final double HAUL_MAX_SPEED = 1.35;
   private static final double HAUL_MIN_BELOW = 0.55;
   private static final double HAUL_DONE_DIST = 6.0;
   private static final int HAUL_GROUND_GRACE = 4;
   private static final double GROUND_SKY_UP = 0.75;
   private static final double GROUND_SKY_LEN = 16.0;
   private static final double GROUND_MIN_ANCHOR_DY = 3.0;
   private static final double START_KICK = 0.4;
   private static final double START_KICK_BELOW = 0.32;
   private static final double DEAD_SPEED = 0.25;
   private static final int DEAD_DELAY = 10;
   private static final double FLIGHT_DRAG = 0.995;
   private static final double FLIGHT_MIN = 0.25;
   private static final int FLIGHT_MAX = 200;
   private static final double REEL_STEP = 0.45;
   private static final int REEL_EVERY = 2;
   private static final double ZIP_RANGE = 48.0;
   private static final double ZIP_CONE_DEG = 5.0;
   private static final double ZIP_MIN = 1.5;
   private static final double ZIP_MAX = 1.6;
   private static final double ZIP_SHOOT_SPEED = 14.0;
   private static final int ZIP_SHOOT_MIN = 1;
   private static final int ZIP_SHOOT_MAX = 3;
   private static final double ZIP_SHOOT_DRAG = 0.85;
   private static final double ZIP_SHOOT_SINK = 0.03;
   private static final double ZIP_YANK_IMPULSE = 0.7;
   private static final double ZIP_PULL_ACCEL = 0.18;
   private static final double ZIP_ARRIVE = 0.9;
   private static final double ZIP_RELEASE_DIST = 2.6;
   private static final double ZIP_RELEASE_UP = 0.28;
   private static final double ZIP_CLIMB = 0.5;
   private static final int ZIP_TIMEOUT = 40;
   private static final int ZIP_STALL = 7;
   private static final double AIR_ZIP_DIST = 14.0;
   private static final double YANK_CONE_DEG = 6.0;
   private static final double STRIKE_RANGE = 24.0;
   private static final double STRIKE_CONE_DEG = 7.0;
   private static final double STRIKE_PULL = 2.6;
   private static final double STRIKE_CONTACT = 1.4;
   private static final int STRIKE_TIMEOUT = 24;
   private static final double STRIKE_MAX_SPEED = 2.2;
   private static final double STRIKE_HOP_BACK = 0.55;
   private static boolean active;
   private static boolean flight;
   private static Vec3 anchor = Vec3.ZERO;
   private static double ropeLen;
   private static boolean sky;
   private static boolean left;
   private static boolean hanging;
   private static Vec3 vel = Vec3.ZERO;
   private static int stillTicks;
   private static int groundTicks;
   private static int flightTicks;
   private static int swayPhase;
   private static boolean lastLeft;
   private static int haulTicks;
   private static int arcCount;
   private static boolean descending;
   private static int reelTick;
   private static boolean zipping;
   private static boolean zipShooting;
   private static int zipShootTicks;
   private static int zipShootTotal;
   private static Vec3 zipTarget = Vec3.ZERO;
   private static int zipTicks;
   private static double zipPrevDist;
   private static int zipStallTicks;
   private static Vec3 zipVel = Vec3.ZERO;
   private static final int YANK_RENDER_TICKS = 8;
   private static int yankRenderId = -1;
   private static int yankRenderTicks;
   private static final double GLIDE_GRAVITY = 0.08;
   private static final double GLIDE_STEER = 0.1;
   private static final int FIREWORK_BOOST_TICKS = 20;
   private static boolean gliding;
   private static int glideBoostTicks;
   private static boolean wingsEnabled;
   private static boolean jumpHeld;
   private static boolean wasOnGround;
   private static final double GLIDE_BASE_PITCH = Math.toRadians(90.0);
   private static final double GLIDE_TILT_FACTOR = 1.0;
   private static final double GLIDE_TILT_CLAMP = 70.0;
   private static final double TUCK_START = 25.0;
   private static final double TUCK_FULL = 72.0;
   private static final double SPEED_TUCK_START = 1.0;
   private static final double SPEED_TUCK_FULL = 1.9;
   private static final float SPEED_TUCK_MAX = 0.6F;
   public static final double GLIDE_ARM_SPREAD_DEG = 72.0;
   public static final double GLIDE_ARM_TUCK_DEG = 48.0;
   public static final double GLIDE_LEG_SPREAD_DEG = 22.0;
   public static final double GLIDE_LEG_TUCK_DEG = 16.0;
   private static float glideLean;
   private static float glideLeanPrev;
   private static float smoothVelYaw;
   private static boolean velYawInit;
   private static final double VEL_YAW_SMOOTH = 1.0;
   private static final double GLIDE_SLIP_FULL = 30.0;
   private static final double GLIDE_BODY_BANK_DEG = 0.0;
   private static final double GLIDE_CAM_BANK_DEG = 0.0;
   private static boolean striking;
   private static LivingEntity strikeTarget;
   private static double strikeImpactSpeed;
   private static int strikeTicks;
   private static final int ZIP_POSE_SETTLE = 8;
   private static long zipPoseEndTick = -10000L;

   public static boolean wingsEnabled() {
      return wingsEnabled;
   }

   public static double glidePitchRadians(float xRotDeg) {
      double clamped = Math.max(-70.0, Math.min(70.0, (double)xRotDeg));
      return GLIDE_BASE_PITCH + Math.toRadians(clamped) * 1.0;
   }

   public static float glideTuck(float xRotDeg, double speed) {
      float dive = (double)xRotDeg <= 25.0 ? 0.0F : (float)Math.min(1.0, ((double)xRotDeg - 25.0) / 47.0);
      float fast = speed <= 1.0 ? 0.0F : (float)Math.min(1.0, (speed - 1.0) / 0.8999999999999999) * 0.6F;
      return Math.max(dive, fast);
   }

   private static void updateGlideLean(Minecraft mc) {
      glideLeanPrev = 0.0F;
      glideLean = 0.0F;
      velYawInit = false;
      if (gliding) {
         float viewYaw = mc.player.getYRot();
         mc.player.yBodyRot = viewYaw;
         mc.player.yHeadRot = viewYaw;
      }
   }

   public static float glideLean(float partialTick) {
      return glideLeanPrev + (glideLean - glideLeanPrev) * partialTick;
   }

   public static double glideBodyRollRadians(float partialTick) {
      return Math.toRadians((double)glideLean(partialTick) * 0.0);
   }

   public static float glideCameraRollDeg(float partialTick) {
      return glideLean(partialTick) * 0.0F;
   }

   private SpiderSwingController() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(SpiderSwingController::onClientTick);
   }

   public static boolean active() {
      return active || zipping || striking || gliding;
   }

   public static boolean gliding() {
      return gliding;
   }

   public static void fireworkBoost() {
      if (gliding) {
         glideBoostTicks = 20;
      }
   }

   public static boolean swinging() {
      return active && !flight;
   }

   public static boolean hanging() {
      return hanging;
   }

   public static boolean striking() {
      return striking;
   }

   public static boolean zipping() {
      return zipping;
   }

   public static boolean zipPoseActive() {
      if (zipping) {
         return true;
      } else if (!active && !striking && !gliding) {
         ClientLevel level = Minecraft.getInstance().level;
         return level != null && level.getGameTime() - zipPoseEndTick < 8L;
      } else {
         return false;
      }
   }

   private static void markZipPoseEnded() {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null) {
         zipPoseEndTick = level.getGameTime();
      }
   }

   public static boolean zipShooting() {
      return zipShooting;
   }

   public static boolean zipPulling() {
      return zipping && !zipShooting;
   }

   public static float zipAttachProgress() {
      return zipShooting && zipShootTotal > 0 ? 1.0F - (float)zipShootTicks / (float)zipShootTotal : 1.0F;
   }

   public static int yankRenderId() {
      return yankRenderTicks > 0 ? yankRenderId : -1;
   }

   public static int yankRenderAge() {
      return 8 - yankRenderTicks;
   }

   private static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         clearLocal();
         reelTick = 0;
         glideLeanPrev = 0.0F;
         glideLean = 0.0F;
      } else {
         updateGlideLean(mc);
         if (yankRenderTicks > 0) {
            yankRenderTicks--;
         }

         String abilityId = activeSpiderAbilityId(mc);
         boolean spider = "spider".equals(ClientPowerState.get().livePowerId());
         boolean swingSlot = "web_swing".equals(abilityId);
         boolean zipSlot = "web_zip".equals(abilityId);
         boolean strikeSlot = "web_strike".equals(abilityId);
         boolean wingsSlot = "web_wings".equals(abilityId);
         boolean jumpDown = mc.options.keyJump.isDown() && mc.screen == null;
         boolean onGround = mc.player.onGround();
         if (spider
            && wingsEnabled
            && jumpDown
            && !jumpHeld
            && !onGround
            && !wasOnGround
            && !mc.player.isInWater()
            && !gliding
            && (!active || flight)
            && !zipping
            && !striking
            && !hasElytra(mc.player)) {
            startGlide(mc);
         }

         jumpHeld = jumpDown;
         wasOnGround = onGround;
         if (!spider || !unlockedLocally("web_wings")) {
            wingsEnabled = false;
         }

         if (!wingsEnabled && gliding) {
            stopGlide(true);
         }

         if (active && !flight && !AbilityKeyInput.heldFor(slotOf("web_swing"))) {
            end(true);
         }

         if (swingSlot || zipSlot || strikeSlot || wingsSlot) {
            int slot = slotOf(abilityId);
            boolean press = AbilityKeyInput.pressedFor(slot);
            if (press && !unlockedLocally(abilityId)) {
               press = false;
               ClientPowerState.onFeedback(new AbilityFeedbackPayload(1));
            }

            if (swingSlot) {
               if (press) {
                  tryStart(mc);
               }
            } else if (zipSlot) {
               if (press && !zipping) {
                  tryZip(mc);
               }
            } else if (strikeSlot) {
               if (press && !striking) {
                  tryStrike(mc);
               }
            } else if (press) {
               toggleWings(mc);
            }

            if (active && hanging && mc.screen == null) {
               if (mc.options.keyUp.isDown() || mc.options.keyDown.isDown() || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown()) {
                  exitHang();
                  reelTick = 0;
                  return;
               }

               boolean reelDown = mc.options.keyShift.isDown();
               boolean reelUp = mc.options.keyJump.isDown();
               if (!reelDown && !reelUp) {
                  reelTick = 0;
               } else if (reelTick++ % 2 == 0) {
                  if (reelDown) {
                     ropeLen = Math.min(ropeLen + 0.45, anchor.distanceTo(mc.player.position()) + 1.8);
                  }

                  if (reelUp) {
                     ropeLen = Math.max(4.0, ropeLen - 0.45);
                  }
               }
            }
         }
      }
   }

   private static String activeSpiderAbilityId(Minecraft mc) {
      Ability ability = AbilityKeyInput.driving();
      return ability != null ? ability.id() : null;
   }

   private static boolean unlockedLocally(String id) {
      Power power = PowerRegistry.get(ClientPowerState.get().powerId());
      int slot = slotOf(id);
      if (power != null && slot >= 0) {
         Ability ability = power.ability(slot);
         return ability != null && ClientPowerState.get().masteryLevel() >= ability.unlockLevel();
      } else {
         return false;
      }
   }

   private static int slotOf(String id) {
      Power power = PowerRegistry.get(ClientPowerState.get().powerId());
      if (power == null) {
         return -1;
      } else {
         for (int slot = 0; slot < power.abilityCount(); slot++) {
            if (id.equals(power.ability(slot).id())) {
               return slot;
            }
         }

         return -1;
      }
   }

   static int swingSlot() {
      return slotOf("web_swing");
   }

   private static void tryStart(Minecraft mc) {
      if ((!active || flight) && !gliding && mc.player != null && mc.level != null) {
         Player player = mc.player;
         boolean wantHang = false;
         Vec3 eye = player.getEyePosition();
         Vec3 look = player.getLookAngle();
         boolean grounded = isGroundStart(mc, player);
         Vec3 fallIn = player.getDeltaMovement();
         Vec3 incoming = SwingAssist.catchFall(fallIn, look);
         double minDy = grounded ? 3.0 : -1.5;
         Vec3 solid = SwingAssist.bestAnchor(mc.level, player, incoming, 40.0, 32.0, minDy, grounded);
         Vec3 a;
         boolean isSky;
         if (solid != null) {
            a = solid;
            isSky = false;
         } else {
            if (RealisticMovement.clientEnabled()) {
               if (mc.level != null) {
                  mc.level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6F, 1.2F, false);
               }

               return;
            }

            if (wantHang) {
               a = player.position().add(0.0, 22.0, 0.0);
            } else {
               double minUp = grounded ? 0.75 : 0.45;
               Vec3 dir;
               if (look.y < minUp) {
                  Vec3 flat = new Vec3(look.x, 0.0, look.z);
                  flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
                  dir = flat.scale(Math.sqrt(1.0 - minUp * minUp)).add(0.0, minUp, 0.0);
               } else {
                  dir = look;
               }

               dir = dir.normalize();
               double hSpeedIn = Math.sqrt(incoming.x * incoming.x + incoming.z * incoming.z);
               double skyLen = grounded ? 16.0 : Math.min(18.0 + hSpeedIn * 7.0, 34.0);
               a = player.position().add(dir.scale(skyLen));
            }

            isSky = true;
         }

         double len = a.distanceTo(player.position());
         if (!(len < 4.0)) {
            int slot = swingSlot();
            if (slot >= 0) {
               anchor = a;
               ropeLen = len;
               sky = isSky;
               left = !lastLeft;
               lastLeft = left;
               hanging = wantHang;
               flight = false;
               flightTicks = 0;
               stillTicks = 0;
               groundTicks = 0;
               double combo = SpiderTrickController.cashIn();
               capBoost = Math.max(combo, SwingAssist.diveCap(fallIn, 2.0));
               vel = SwingAssist.catchOnRope(fallIn, player.position(), anchor, look).scale(combo);
               double hSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
               if (!wantHang && hSpeed < 0.32) {
                  vel = vel.add(forwardKick(look));
               }

               haulTicks = grounded && !wantHang ? 11 : 0;
               arcCount = 0;
               descending = false;
               active = true;
               SpiderClientState.set(true, wantHang, anchor.x, anchor.y, anchor.z, left);
               send(new SpiderSwingPayload(wantHang ? 5 : 0, slot, anchor.x, anchor.y, anchor.z, left));
            }
         }
      }
   }

   private static void end(boolean released) {
      Minecraft mc = Minecraft.getInstance();
      if (active && !flight) {
         boolean wasHanging = hanging;
         Player player = mc.player;
         boolean onGround = player != null && player.onGround();
         int slot = swingSlot();
         Vec3 launch = vel;
         double timing = 0.0;
         if (wasHanging) {
            Vec3 look = player != null ? player.getLookAngle() : new Vec3(0.0, 0.0, 1.0);
            launch = new Vec3(look.x * 0.2, -0.2, look.z * 0.2);
         } else if (released) {
            timing = SwingAssist.releaseTiming(vel);
            launch = launch.scale(1.15 + 0.25 * timing).add(0.0, 0.2 * timing, 0.0);
         }

         if (player != null) {
            player.setDeltaMovement(launch);
         }

         if (slot >= 0 && !flight) {
            send(SpiderSwingPayload.of(released && !wasHanging ? 3 : 4, slot));
         }

         if (released && !wasHanging && mc.level != null && mc.player != null) {
            boolean clean = timing >= 0.75;
            mc.level
               .playLocalSound(
                  mc.player.getX(),
                  mc.player.getY(),
                  mc.player.getZ(),
                  SoundEvents.ARROW_SHOOT,
                  SoundSource.PLAYERS,
                  clean ? 0.8F : 0.6F,
                  clean ? 1.7F : 1.3F,
                  false
               );
         }

         boolean inWater = player != null && player.isInWater();
         if (released && !wasHanging && !onGround && !inWater && launch.length() >= 0.25) {
            flight = true;
            hanging = false;
            flightTicks = 0;
            haulTicks = 0;
            vel = launch;
            active = true;
            SpiderClientState.reset();
            SpiderTrickController.beginAirRun();
         } else {
            clearLocal();
         }
      }
   }

   private static boolean isGroundStart(Minecraft mc, Player player) {
      if (player.getDeltaMovement().length() > 0.6) {
         return false;
      } else if (player.onGround()) {
         return true;
      } else if (mc.level == null) {
         return false;
      } else {
         Vec3 from = player.position();
         Vec3 to = from.subtract(0.0, 3.0, 0.0);
         return mc.level.clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, player)).getType() == Type.BLOCK;
      }
   }

   private static Vec3 forwardKick(Vec3 look) {
      Vec3 h = new Vec3(look.x, 0.0, look.z);
      h = h.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : h.normalize();
      return h.scale(0.4).add(0.0, -0.1, 0.0);
   }

   private static void toggleWings(Minecraft mc) {
      wingsEnabled = !wingsEnabled;
      if (!wingsEnabled && gliding) {
         stopGlide(true);
      }

      if (mc.player != null) {
         Component msg;
         if (wingsEnabled && hasElytra(mc.player)) {
            msg = Component.translatable("awakened.spider.wings_on_elytra").withStyle(ChatFormatting.YELLOW);
         } else {
            msg = Component.translatable(wingsEnabled ? "awakened.spider.wings_on" : "awakened.spider.wings_off")
               .withStyle(wingsEnabled ? ChatFormatting.GREEN : ChatFormatting.GRAY);
         }

         mc.player.displayClientMessage(msg, true);
         mc.level
            .playLocalSound(
               mc.player.getX(),
               mc.player.getY(),
               mc.player.getZ(),
               SoundEvents.FISHING_BOBBER_RETRIEVE,
               SoundSource.PLAYERS,
               0.6F,
               wingsEnabled ? 1.4F : 0.9F,
               false
            );
      }
   }

   private static boolean hasElytra(Player player) {
      return player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
   }

   private static void startGlide(Minecraft mc) {
      Player player = mc.player;
      if (player != null && mc.level != null) {
         if (ClientPowerState.get().infiniteStamina() || !(ClientPowerState.get().stamina() < WebWingsAbility.cost())) {
            int slot = slotOf("web_wings");
            if (slot >= 0) {
               gliding = true;
               active = false;
               flight = false;
               flightTicks = 0;
               vel = player.getDeltaMovement();
               if (vel.horizontalDistance() < 0.1) {
                  Vec3 look = player.getLookAngle();
                  vel = vel.add(look.x * 0.25, 0.0, look.z * 0.25);
               }

               send(SpiderSwingPayload.of(6, slot));
               mc.level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 0.7F, 0.8F, false);
            }
         }
      }
   }

   private static void stopGlide(boolean notifyServer) {
      if (gliding) {
         gliding = false;
         Player player = Minecraft.getInstance().player;
         if (player != null) {
            player.setDeltaMovement(vel);
            player.hurtMarked = true;
         }

         if (notifyServer) {
            int slot = slotOf("web_wings");
            if (slot >= 0) {
               send(SpiderSwingPayload.of(7, slot));
            }
         }
      }
   }

   private static void tickGlide(Player player) {
      if (!player.onGround() && !player.isInWater()) {
         Vec3 look = player.getLookAngle();
         double pitch = (double)player.getXRot() * (Math.PI / 180.0);
         double horizLook = Math.sqrt(look.x * look.x + look.z * look.z);
         double horizVel = vel.horizontalDistance();
         double cos = Math.cos(pitch);
         double cosSq = cos * cos;
         Vec3 m = vel.add(0.0, 0.08 * (-1.0 + cosSq * 0.75), 0.0);
         if (m.y < 0.0 && horizLook > 0.0) {
            double d = m.y * -0.1 * cosSq;
            m = m.add(look.x * d / horizLook, d, look.z * d / horizLook);
         }

         if (pitch < 0.0 && horizLook > 0.0) {
            double d = horizVel * -Math.sin(pitch) * 0.04;
            m = m.add(-look.x * d / horizLook, d * 3.2, -look.z * d / horizLook);
         }

         if (horizLook > 0.0) {
            m = m.add((look.x / horizLook * horizVel - m.x) * 0.1, 0.0, (look.z / horizLook * horizVel - m.z) * 0.1);
         }

         m = m.multiply(0.99, 0.98, 0.99);
         if (glideBoostTicks > 0) {
            glideBoostTicks--;
            double target = 1.5;
            m = m.add(look.x * 0.1 + (look.x * target - m.x) * 0.5, look.y * 0.1 + (look.y * target - m.y) * 0.5, look.z * 0.1 + (look.z * target - m.z) * 0.5);
         }

         vel = moveAndReconcile(player, m, m);
      } else {
         stopGlide(true);
      }
   }

   private static void tryZip(Minecraft mc) {
      if (!zipping && mc.player != null && mc.level != null) {
         if (ClientPowerState.get().infiniteStamina() || !(ClientPowerState.get().stamina() < WebZipAbility.cost())) {
            int slot = slotOf("web_zip");
            if (slot >= 0) {
               Player player = mc.player;
               Vec3 terrain = zipRaycast(mc, player);
               Entity yankTarget = yankPick(mc, player);
               if (yankTarget != null && betterThanTerrain(player, yankTarget, terrain)) {
                  send(SpiderZipPayload.yank(slot, yankTarget.getId()));
                  yankRenderId = yankTarget.getId();
                  yankRenderTicks = 8;
                  mc.level
                     .playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 0.7F, 1.6F, false);
               } else if (terrain != null) {
                  if (!(terrain.distanceTo(player.position()) < 1.5)) {
                     startZip(mc, player, slot, terrain, true);
                  }
               } else if (RealisticMovement.clientEnabled()) {
                  mc.level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6F, 1.2F, false);
               } else {
                  startZip(mc, player, slot, airZipPoint(player), false);
               }
            }
         }
      }
   }

   private static Vec3 airZipPoint(Player player) {
      return player.getEyePosition().add(player.getLookAngle().scale(14.0));
   }

   private static void startZip(Minecraft mc, Player player, int slot, Vec3 anchor, boolean grace) {
      stopGlide(true);
      endSwingState();
      zipTarget = anchor;
      zipping = true;
      zipTicks = 0;
      zipStallTicks = 0;
      zipPrevDist = Double.MAX_VALUE;
      zipVel = Vec3.ZERO;
      Vec3 incoming = player.getDeltaMovement();
      double incomingSpeed = incoming.length();
      Vec3 toAnchor = anchor.subtract(player.position());
      if (incomingSpeed > 1.0E-4 && toAnchor.lengthSqr() > 1.0E-4) {
         player.setDeltaMovement(toAnchor.normalize().scale(Math.min(incomingSpeed, 1.6)));
      }

      double reach = anchor.distanceTo(player.position());
      zipShooting = true;
      zipShootTotal = (int)Math.max(1L, Math.min(3L, Math.round(reach / 14.0)));
      zipShootTicks = zipShootTotal;
      SpiderClientState.setZip(true, anchor.x, anchor.y, anchor.z, !lastLeft);
      lastLeft = !lastLeft;
      mc.level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 0.7F, 1.5F, false);
      send(SpiderZipPayload.self(slot, !grace));
   }

   private static Entity yankPick(Minecraft mc, Player player) {
      ClientLevel level = mc.level;
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle();
      Vec3 end = eye.add(look.scale(48.0));
      AABB search = player.getBoundingBox().expandTowards(look.scale(48.0)).inflate(1.0);
      double coneCos = Math.cos(Math.toRadians(6.0));
      Entity rayBest = null;
      double rayBestDistSqr = 2304.0;
      Entity coneBest = null;
      double coneBestCos = coneCos;

      for (Entity e : level.getEntitiesOfClass(Entity.class, search, c -> c != player && c.isAlive() && (c instanceof LivingEntity || c instanceof ItemEntity))) {
         Optional<Vec3> hit = e.getBoundingBox().inflate(0.3).clip(eye, end);
         if (hit.isPresent()) {
            double distSqr = eye.distanceToSqr(hit.get());
            if (distSqr < rayBestDistSqr) {
               rayBestDistSqr = distSqr;
               rayBest = e;
            }
         }

         Vec3 toCenter = e.getBoundingBox().getCenter().subtract(eye);
         double dist = toCenter.length();
         if (!(dist > 48.0) && !(dist < 0.001)) {
            double cos = toCenter.scale(1.0 / dist).dot(look);
            if (cos > coneBestCos) {
               coneBestCos = cos;
               coneBest = e;
            }
         }
      }

      return rayBest != null ? rayBest : coneBest;
   }

   private static boolean betterThanTerrain(Player player, Entity target, Vec3 terrain) {
      if (terrain == null) {
         return true;
      } else {
         double toTarget = target.getBoundingBox().getCenter().distanceTo(player.getEyePosition());
         double toTerrain = terrain.distanceTo(player.getEyePosition());
         return toTarget <= toTerrain + 1.0;
      }
   }

   private static Vec3 zipRaycast(Minecraft mc, Player player) {
      ClientLevel level = mc.level;
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle();
      Vec3 best = null;
      double bestDist = Double.MAX_VALUE;
      double s = Math.sin(Math.toRadians(5.0));
      Vec3 flat = new Vec3(look.x, 0.0, look.z);
      flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
      Vec3 rightAxis = flat.cross(new Vec3(0.0, 1.0, 0.0)).normalize();
      Vec3 upAxis = rightAxis.cross(look).normalize();
      Vec3[] dirs = new Vec3[]{
         look,
         look.add(upAxis.scale(s)).normalize(),
         look.add(upAxis.scale(-s)).normalize(),
         look.add(rightAxis.scale(s)).normalize(),
         look.add(rightAxis.scale(-s)).normalize()
      };

      for (Vec3 dir : dirs) {
         BlockHitResult res = level.clip(new ClipContext(eye, eye.add(dir.scale(48.0)), Block.COLLIDER, Fluid.NONE, player));
         if (res.getType() == Type.BLOCK) {
            double d = res.getLocation().distanceTo(eye);
            if (d < bestDist) {
               bestDist = d;
               best = res.getLocation();
            }
         }
      }

      return best;
   }

   public static void forceStop() {
      clearLocal();
   }

   private static void endSwingState() {
      active = false;
      flight = false;
      hanging = false;
      flightTicks = 0;
      haulTicks = 0;
      vel = Vec3.ZERO;
      capBoost = 1.0;
   }

   private static void clearLocal() {
      if (zipping) {
         markZipPoseEnded();
      }

      active = false;
      flight = false;
      hanging = false;
      zipping = false;
      zipShooting = false;
      zipShootTicks = 0;
      zipTicks = 0;
      striking = false;
      strikeTicks = 0;
      strikeTarget = null;
      strikeImpactSpeed = 0.0;
      gliding = false;
      glideBoostTicks = 0;
      vel = Vec3.ZERO;
      stillTicks = 0;
      groundTicks = 0;
      flightTicks = 0;
      haulTicks = 0;
      arcCount = 0;
      descending = false;
      capBoost = 1.0;
      SpiderClientState.reset();
   }

   private static void exitHang() {
      hanging = false;
      stillTicks = 0;
      Player player = Minecraft.getInstance().player;
      if (player != null && vel.length() < 0.32) {
         vel = vel.add(forwardKick(player.getLookAngle()));
      }

      SpiderClientState.set(true, false, anchor.x, anchor.y, anchor.z, left);
      int slot = swingSlot();
      if (slot >= 0) {
         send(SpiderSwingPayload.of(2, slot));
      }
   }

   public static void tickMovement(Player player) {
      if (striking) {
         tickStrike(player);
      } else if (zipping) {
         tickZip(player);
      } else if (gliding) {
         tickGlide(player);
      } else if (active) {
         if (hanging) {
            tickHang(player);
         } else if (flight) {
            tickFlight(player);
         } else {
            tickSwing(player);
         }
      }
   }

   private static void tickZip(Player player) {
      if (zipShooting) {
         Vec3 v = player.getDeltaMovement();
         Vec3 hang = new Vec3(v.x * 0.85, v.y * 0.85 - 0.03, v.z * 0.85);
         zipVel = moveAndReconcile(player, hang, hang);
         if (--zipShootTicks <= 0) {
            zipShooting = false;
            Vec3 pull = zipTarget.subtract(player.position());
            zipVel = pull.lengthSqr() < 1.0E-4 ? Vec3.ZERO : pull.normalize().scale(0.7);
            SpiderCamera.triggerZipDip();
            if (Minecraft.getInstance().level != null) {
               Minecraft.getInstance()
                  .level
                  .playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.8F, 0.7F, false);
            }

            int slot = slotOf("web_zip");
            if (slot >= 0) {
               send(SpiderZipPayload.pull(slot));
            }
         }
      } else {
         Vec3 remaining = zipTarget.subtract(player.position());
         double dist = remaining.length();
         if (!(dist <= 2.6) && ++zipTicks < 40) {
            if (zipTicks > 1 && dist > zipPrevDist - 0.08) {
               if (++zipStallTicks >= 7) {
                  releaseZip(player, false);
                  return;
               }
            } else {
               zipStallTicks = 0;
            }

            zipPrevDist = dist;
            Vec3 dir = dist > 1.0E-4 ? remaining.scale(1.0 / dist) : new Vec3(0.0, 1.0, 0.0);
            double speed = Math.min(1.6, zipVel.length() + 0.18);
            zipVel = dir.scale(speed);
            Vec3 moved = moveAndReconcile(player, zipVel, zipVel);
            if (player.horizontalCollision && dist > 1.9 && zipTarget.y > player.getY() + 0.5) {
               player.setDeltaMovement(moved.x, 0.5, moved.z);
               player.move(MoverType.SELF, new Vec3(0.0, 0.5, 0.0));
               player.resetFallDistance();
            }
         } else {
            releaseZip(player, true);
         }
      }
   }

   private static void releaseZip(Player player, boolean fling) {
      zipping = false;
      zipShooting = false;
      zipTicks = 0;
      markZipPoseEnded();
      if (player != null) {
         if (fling) {
            player.setDeltaMovement(zipVel.x, Math.max(zipVel.y, 0.0) + 0.28, zipVel.z);
            player.hurtMarked = true;
         } else {
            Vec3 v = player.getDeltaMovement();
            player.setDeltaMovement(0.0, Math.min(v.y, 0.0), 0.0);
         }
      }

      int slot = slotOf("web_zip");
      if (slot >= 0) {
         send(SpiderZipPayload.stop(slot));
      }

      SpiderClientState.reset();
   }

   private static void tryStrike(Minecraft mc) {
      if (!striking && mc.player != null && mc.level != null) {
         if (ClientPowerState.get().infiniteStamina() || !(ClientPowerState.get().stamina() < WebStrikeAbility.cost())) {
            int slot = slotOf("web_strike");
            if (slot >= 0) {
               Player player = mc.player;
               LivingEntity target = strikePick(mc, player);
               if (target == null) {
                  mc.level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.FENCE_GATE_OPEN, SoundSource.PLAYERS, 0.4F, 1.6F, false);
               } else {
                  strikeImpactSpeed = Math.min(player.getDeltaMovement().length(), 2.2);
                  endSwingState();
                  strikeTarget = target;
                  striking = true;
                  strikeTicks = 0;
                  Vec3 c = target.getBoundingBox().getCenter();
                  SpiderClientState.setZip(true, c.x, c.y, c.z, !lastLeft);
                  lastLeft = !lastLeft;
                  send(SpiderStrikePayload.start(slot));
               }
            }
         }
      }
   }

   private static LivingEntity strikePick(Minecraft mc, Player player) {
      ClientLevel level = mc.level;
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle();
      Vec3 end = eye.add(look.scale(24.0));
      AABB search = player.getBoundingBox().expandTowards(look.scale(24.0)).inflate(1.0);
      double coneCos = Math.cos(Math.toRadians(7.0));
      LivingEntity rayBest = null;
      double rayBestDistSqr = 576.0;
      LivingEntity coneBest = null;
      double coneBestCos = coneCos;

      for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, search, c -> c != player && c.isAlive())) {
         if (clearShot(level, eye, e, player)) {
            Optional<Vec3> hit = e.getBoundingBox().inflate(0.3).clip(eye, end);
            if (hit.isPresent()) {
               double distSqr = eye.distanceToSqr(hit.get());
               if (distSqr < rayBestDistSqr) {
                  rayBestDistSqr = distSqr;
                  rayBest = e;
               }
            }

            Vec3 toCenter = e.getBoundingBox().getCenter().subtract(eye);
            double dist = toCenter.length();
            if (!(dist > 24.0) && !(dist < 0.001)) {
               double cos = toCenter.scale(1.0 / dist).dot(look);
               if (cos > coneBestCos) {
                  coneBestCos = cos;
                  coneBest = e;
               }
            }
         }
      }

      return rayBest != null ? rayBest : coneBest;
   }

   private static boolean clearShot(Level level, Vec3 eye, Entity target, Player player) {
      Vec3 to = target.getBoundingBox().getCenter();
      BlockHitResult hit = level.clip(new ClipContext(eye, to, Block.COLLIDER, Fluid.NONE, player));
      return hit.getType() == Type.MISS || eye.distanceToSqr(hit.getLocation()) >= eye.distanceToSqr(to) - 0.5;
   }

   private static void tickStrike(Player player) {
      if (strikeTarget != null && strikeTarget.isAlive() && !strikeTarget.isRemoved() && ++strikeTicks <= 24) {
         Vec3 targetCenter = strikeTarget.getBoundingBox().getCenter();
         SpiderClientState.setZip(true, targetCenter.x, targetCenter.y, targetCenter.z, SpiderClientState.leftArm());
         Vec3 remaining = targetCenter.subtract(player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0));
         double dist = remaining.length();
         if (dist <= 1.4) {
            connect(player);
         } else {
            Vec3 dir = dist > 1.0E-4 ? remaining.scale(1.0 / dist) : player.getLookAngle();
            Vec3 v = dir.scale(2.6);
            Vec3 moved = moveAndReconcile(player, v, v);
            if (player.horizontalCollision && Math.abs(moved.x) + Math.abs(moved.z) < 0.05) {
               player.setDeltaMovement(moved.x, 0.5, moved.z);
               player.move(MoverType.SELF, new Vec3(0.0, 0.5, 0.0));
               player.resetFallDistance();
            }
         }
      } else {
         endStrike(player, false);
      }
   }

   private static void connect(Player player) {
      int slot = slotOf("web_strike");
      if (slot >= 0 && strikeTarget != null) {
         send(SpiderStrikePayload.hit(slot, strikeTarget.getId(), strikeImpactSpeed));
      }

      float intensity = (float)Math.min(1.0, strikeImpactSpeed / 2.2);
      SpiderCamera.triggerStrikeFlip(intensity);
      if (strikeTarget != null) {
         Vec3 back = new Vec3(player.getX() - strikeTarget.getX(), 0.0, player.getZ() - strikeTarget.getZ());
         back = back.lengthSqr() < 1.0E-4 ? player.getLookAngle().scale(-1.0) : back.normalize();
         player.setDeltaMovement(back.x * 0.55, 0.32, back.z * 0.55);
      }

      endStrike(player, true);
   }

   private static void endStrike(Player player, boolean connected) {
      striking = false;
      strikeTicks = 0;
      strikeTarget = null;
      strikeImpactSpeed = 0.0;
      if (player != null && !connected) {
         Vec3 v = player.getDeltaMovement();
         player.setDeltaMovement(0.0, Math.min(v.y, 0.0), 0.0);
      }

      SpiderClientState.reset();
   }

   private static void tickFlight(Player player) {
      if (player.isInWater()) {
         clearLocal();
      } else {
         Vec3 v = vel.add(0.0, -0.07, 0.0).scale(0.995);
         double speed = v.length();
         double cap = 2.0 * capBoost;
         if (speed > cap) {
            v = v.scale(cap / speed);
         }

         apply(player, v);
         if (player.onGround() || ++flightTicks >= 200) {
            clearLocal();
         }
      }
   }

   private static void tickHaul(Player player) {
      Vec3 toAnchor = anchor.subtract(player.position());
      double dist = toAnchor.length();
      boolean wellBelow = anchor.y - player.getY() > ropeLen * 0.55;
      if (wellBelow && !(dist <= 6.0) && !(dist < 1.0E-4)) {
         haulTicks--;
         Vec3 pull = toAnchor.scale(1.0 / dist);
         Vec3 v = vel.add(0.0, -0.07, 0.0).add(pull.scale(0.12));
         double speed = v.length();
         if (speed > 1.35) {
            v = v.scale(1.35 / speed);
         }

         vel = moveAndReconcile(player, v, v);
         ropeLen = Math.min(ropeLen, anchor.distanceTo(player.position()));
         stillTicks = 0;
         if (haulTicks > 7) {
            groundTicks = 0;
         } else if (player.onGround() && ++groundTicks >= 3) {
            end(false);
         }
      } else {
         haulTicks = 0;
         tickSwing(player);
      }
   }

   private static void tickSwing(Player player) {
      if (haulTicks > 0) {
         tickHaul(player);
      } else {
         ropeLen = SwingAssist.clearanceRope(Minecraft.getInstance().level, player, anchor, ropeLen, 4.0);
         Vec3 startPos = player.position();
         Vec3 pos = startPos;
         Vec3 v = vel;
         Vec3 look = player.getLookAngle();
         int subSteps = (int)Math.max(6.0, Math.min(40.0, Math.ceil(vel.length() / 0.1)));
         double gPer = 0.07 / (double)subSteps;
         int decayArcs = Math.min(arcCount, 8);
         double pumpPer = 0.018 * Math.pow(0.55, (double)decayArcs) / (double)subSteps;

         for (int i = 0; i < subSteps; i++) {
            v = v.add(0.0, -gPer, 0.0);
            Vec3 toAnchor = anchor.subtract(pos);
            double dist = toAnchor.length();
            Vec3 radialDir = dist > 1.0E-4 ? toAnchor.scale(1.0 / dist) : new Vec3(0.0, 1.0, 0.0);
            Vec3 tangent = look.subtract(radialDir.scale(look.dot(radialDir)));
            if (tangent.lengthSqr() > 1.0E-4) {
               v = v.add(tangent.normalize().scale(pumpPer));
            }

            Vec3 predicted = pos.add(v.scale(1.0 / (double)subSteps));
            Vec3 fromAnchor = predicted.subtract(anchor);
            double pd = fromAnchor.length();
            if (pd > ropeLen && pd > 1.0E-4) {
               pos = anchor.add(fromAnchor.scale(ropeLen / pd));
               Vec3 ropeDir = pos.subtract(anchor).scale(1.0 / ropeLen);
               double radialOut = v.dot(ropeDir);
               if (radialOut > 0.0) {
                  v = v.subtract(ropeDir.scale(radialOut));
               }
            } else {
               pos = predicted;
            }
         }

         double drag = Math.max(0.985, (sky ? 0.998 : 0.9995) - (double)decayArcs * 0.0012);
         v = v.scale(drag);
         double speed = v.length();
         double cap = 2.0 * capBoost;
         if (speed > cap) {
            v = v.scale(cap / speed);
         }

         boolean belowAnchorNow = anchor.y - startPos.y > ropeLen * 0.6;
         vel = moveAndReconcile(player, pos.subtract(startPos), v);
         speed = vel.length();
         if (vel.y < -0.05) {
            descending = true;
         } else if (descending && vel.y > 0.05) {
            descending = false;
            arcCount++;
         }

         if (speed < 0.25 && belowAnchorNow && !player.onGround()) {
            if (++stillTicks >= 10) {
               end(false);
               return;
            }
         } else {
            stillTicks = 0;
         }

         if (player.onGround()) {
            if (++groundTicks >= 3) {
               end(false);
            }
         } else {
            groundTicks = 0;
         }
      }
   }

   private static void tickHang(Player player) {
      Vec3 p = player.position();
      double sway = Math.sin((double)(player.tickCount + swayPhase) * 0.08) * 0.04;
      Vec3 hangPoint = anchor.add(sway, -ropeLen, 0.0);
      Vec3 toward = hangPoint.subtract(p).scale(0.25);
      if (toward.length() > 0.4) {
         toward = toward.normalize().scale(0.4);
      }

      apply(player, toward);
   }

   private static void apply(Player player, Vec3 v) {
      vel = moveAndReconcile(player, v, v);
   }

   private static Vec3 moveAndReconcile(Player player, Vec3 displacement, Vec3 modelVel) {
      Vec3 before = player.position();
      player.move(MoverType.SELF, displacement);
      Vec3 moved = player.position().subtract(before);
      player.calculateEntityAnimation(false);
      double vx = modelVel.x;
      double vy = modelVel.y;
      double vz = modelVel.z;
      if (Math.abs(moved.x) + 0.001 < Math.abs(displacement.x)) {
         vx = 0.0;
      }

      if (Math.abs(moved.y) + 0.001 < Math.abs(displacement.y)) {
         vy = 0.0;
      }

      if (Math.abs(moved.z) + 0.001 < Math.abs(displacement.z)) {
         vz = 0.0;
      }

      Vec3 reconciled = new Vec3(vx, vy, vz);
      player.setDeltaMovement(reconciled);
      player.resetFallDistance();
      return reconciled;
   }

   static void send(CustomPacketPayload payload) {
      PacketDistributor.sendToServer(payload, new CustomPacketPayload[0]);
   }
}
