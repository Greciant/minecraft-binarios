package com.awakenedmod.awakened.content.velocity;

import com.awakenedmod.awakened.content.brawler.BrawlerLift;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.PhaseState;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.TeleportUtil;
import com.awakenedmod.awakened.core.data.VelocityVisual;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class SpeedTunnelAbility extends VelocityHoldAbility {
   public static final float ENTER_COST = 20.0F;
   private static final float DRAIN_PER_SECOND = 8.0F;
   private static final int REQUIRED_GEAR = 5;
   private static final int MAX_RUN_TICKS = 600;
   static final double BLOCKS_PER_TICK = 5.0;
   private static final double WORLD_MARGIN = 2.0;
   private static final double CLIMB_OUT = 0.5;
   private static final double SKIM_BAND = 1.0;
   private static final double PORTAL_AHEAD = 3.0;
   private static final double DIVE_MAX_SPEED = 1.2;
   private static final double EXIT_SETBACK = 0.6;
   private static final int LONE_EXIT_TICKS = 30;
   private static final int ENTRY_MOUTH_TICKS = 1025;
   private static final double EXIT_SEARCH_RADIUS = 8.0;
   private static final int EXIT_MAX_DROP = 8;
   private static final double MIN_LANE_SPAN = 4.0;
   private static final int MASTERY_BLOCKS = 100;
   private static final int FEAT_BLOCKS = 300;
   private static final String FEAT_KEY = "velocity_tunnel_300";
   private final Map<UUID, TunnelLane> lanes = this.playerState();
   private final Map<UUID, SpeedTunnelAbility.Emergence> emerging = this.playerState();

   public SpeedTunnelAbility() {
      super("speed_tunnel", 9, 600, VelocityHoldAbility.Input.TOGGLE);
   }

   @Override
   public float staminaCost(ServerPlayer player) {
      return this.holding(player) ? 0.0F : 20.0F;
   }

   @Override
   protected float pressCost() {
      return 20.0F;
   }

   @Override
   protected boolean heldByViewer(VelocityVisual visual) {
      return visual.tunnelling() && !visual.transit();
   }

   @Override
   protected float drainPerSecond() {
      return 8.0F;
   }

   @Override
   protected int requiredGear() {
      return 5;
   }

   @Override
   public boolean requiresFootingWhenRealistic() {
      return false;
   }

   @Override
   public boolean ignoresSealWhileActive() {
      return true;
   }

   @Override
   protected VelocityHoldAbility.Hold newHold() {
      return new SpeedTunnelAbility.Run();
   }

   @Override
   protected boolean begin(ServerPlayer player, VelocityHoldAbility.Hold hold) {
      SpeedTunnelAbility.Run run = (SpeedTunnelAbility.Run)hold;
      ServerLevel level = player.serverLevel();
      Vec3 dive = Vfx.safeDir(AimUtil.lookFlat(player));
      run.entry = player.position();
      run.last = run.entry;
      run.dive = dive;
      run.portalFeet = run.entry.add(dive.scale(3.0));
      run.facing = dive;
      TunnelBody.phase(player);
      TunnelBody.fly(player);
      VelocityVisualState.setDiving(player, true);
      run.portal = VelocityFx.wormhole(level, VelocityPalette.of(player), run.portalFeet, dive, VelocityFx.WormholeRole.ENTRY, 1025);
      diveStep(player, run);
      return true;
   }

   @Override
   protected void tick(ServerPlayer player, VelocityHoldAbility.Hold hold) {
      SpeedTunnelAbility.Run run = (SpeedTunnelAbility.Run)hold;
      if (!player.getAbilities().mayfly) {
         this.stop(player, VelocityHoldAbility.End.RELEASED);
      } else {
         TunnelBody.phase(player);
         if (!run.inside) {
            if (++run.diveTicks >= 5) {
               this.vanish(player, run);
            } else {
               diveStep(player, run);
            }
         } else {
            Vec3 now = player.position();
            run.travelled = run.travelled + now.distanceTo(run.last);
            run.last = now;
            drive(player, run);
         }
      }
   }

   private static void diveStep(ServerPlayer player, SpeedTunnelAbility.Run run) {
      int left = Math.max(1, 5 - run.diveTicks);
      Vec3 toDisc = run.portalFeet.subtract(player.position());
      Vec3 step = toDisc.scale(1.0 / (double)left);
      if (step.length() > 1.2) {
         step = step.normalize().scale(1.2);
      }

      player.setDeltaMovement(step);
      player.hurtMarked = true;
      player.resetFallDistance();
   }

   private void vanish(ServerPlayer player, SpeedTunnelAbility.Run run) {
      run.inside = true;
      run.last = player.position();
      run.facing = Vfx.safeDir(player.getLookAngle());
      VelocityVisualState.setDiving(player, false);
      TunnelBody.hide(player);
      VelocityFx.tunnelVanish(player.serverLevel(), player, run.portalFeet, run.dive);
      drive(player, run);
   }

   private static void drive(ServerPlayer player, SpeedTunnelAbility.Run run) {
      run.facing = steer(player, run);
      player.setDeltaMovement(run.facing.scale(5.0));
      player.hurtMarked = true;
      player.resetFallDistance();
   }

   private static Vec3 steer(ServerPlayer player, SpeedTunnelAbility.Run run) {
      Vec3 look = Vfx.safeDir(player.getLookAngle());
      ServerLevel level = player.serverLevel();
      double floorY = (double)TeleportUtil.minY(level) + 2.0;
      double ceilY = (double)TeleportUtil.maxY(level) - 2.0;
      double y = player.getY();
      if (y < floorY) {
         return Vfx.safeDir(new Vec3(look.x, Math.max(look.y, 0.5), look.z));
      } else if (y > ceilY) {
         return Vfx.safeDir(new Vec3(look.x, Math.min(look.y, -0.5), look.z));
      } else if (y - floorY < 1.0 && look.y < 0.0 || ceilY - y < 1.0 && look.y > 0.0) {
         Vec3 flat = new Vec3(look.x, 0.0, look.z);
         return flat.lengthSqr() < 1.0E-6 ? run.facing : flat.normalize();
      } else {
         return look;
      }
   }

   @Override
   protected void end(ServerPlayer player, VelocityHoldAbility.Hold hold, VelocityHoldAbility.End reason) {
      SpeedTunnelAbility.Run run = (SpeedTunnelAbility.Run)hold;
      ServerLevel level = player.serverLevel();
      if (reason == VelocityHoldAbility.End.TORN_DOWN) {
         if (run.portal != null) {
            run.portal.discard();
         }

         TunnelBody.release(player);
      } else if (!run.inside) {
         VelocityFx.closeWormhole(level, run.portal);
         TunnelBody.release(player);
      } else {
         Vec3 from = player.position();
         run.travelled = run.travelled + from.distanceTo(run.last);
         Vec3 heading = run.facing;
         PhaseState.setServerFull(player.getUUID(), false);
         Vec3 dest = this.safeExit(player, level, from, run);
         player.teleportTo(dest.x, dest.y, dest.z);
         player.setDeltaMovement(Vec3.ZERO);
         player.hurtMarked = true;
         player.resetFallDistance();
         Vec3 flatHeading = Vfx.safeDir(new Vec3(heading.x, 0.0, heading.z));
         Vec3 exitFeet = dest.subtract(flatHeading.scale(0.6));
         boolean lane = exitFeet.distanceTo(run.portalFeet) >= 4.0;
         VfxEntity exitMouth = VelocityFx.wormhole(
            level, VelocityPalette.of(player), exitFeet, flatHeading.scale(-1.0), VelocityFx.WormholeRole.EXIT, lane ? 430 : 30
         );
         if (lane) {
            this.openLane(player, level, run, exitFeet, flatHeading, exitMouth);
         } else {
            VelocityFx.closeWormhole(level, run.portal);
         }

         this.emerging.put(player.getUUID(), new SpeedTunnelAbility.Emergence(exitFeet, dest, flatHeading, run.travelled));
      }
   }

   private Vec3 safeExit(ServerPlayer player, ServerLevel level, Vec3 from, SpeedTunnelAbility.Run run) {
      TeleportUtil.streamIn(level, from);
      Vec3 near = TeleportUtil.nearestSafeSpot(player, from, 8.0, 8);
      if (near != null) {
         return near;
      } else {
         Vec3 surface = TeleportUtil.surfaceSpot(player, from.x, from.z);
         if (surface != null) {
            player.displayClientMessage(Component.translatable("awakened.velocity.tunnel_exit_ground"), true);
            return surface;
         } else {
            this.fail(player, "awakened.feedback.velocity_tunnel_exit_unsafe", new Object[0]);
            return run.entry;
         }
      }
   }

   private void emerge(ServerPlayer player, SpeedTunnelAbility.Emergence out) {
      ServerLevel level = player.serverLevel();
      TunnelBody.release(player);
      BrawlerLift.coverFall(player, 60);
      Momentum.set(PowerService.data(player), 0.0F);
      VelocityPower.momentumSpent(player);
      PowerService.sync(player);
      VelocityFx.tunnelEmerge(level, player, out.discFeet, out.feet, out.heading);
      reward(player, out.travelled);
   }

   private void openLane(ServerPlayer player, ServerLevel level, SpeedTunnelAbility.Run run, Vec3 exitFeet, Vec3 heading, VfxEntity exitMouth) {
      TunnelLane previous = this.lanes.remove(player.getUUID());
      if (previous != null) {
         previous.close();
      }

      this.lanes.put(player.getUUID(), TunnelLane.open(level, run.portalFeet, run.dive, run.portal, exitFeet, heading, exitMouth));
      run.portal = null;
   }

   private static void reward(ServerPlayer player, double travelled) {
      int blocks = (int)travelled;
      if (blocks >= 100) {
         MasterySystem.award(player, MasteryReason.SURVIVAL);
      }

      if (blocks >= 300) {
         QuestSystem.feat(player, "velocity_tunnel_300");
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      super.serverTick(player);
      SpeedTunnelAbility.Emergence out = this.emerging.get(player.getUUID());
      if (out != null && ++out.ticks >= 1) {
         this.emerging.remove(player.getUUID());
         this.emerge(player, out);
      }

      TunnelLane lane = this.lanes.get(player.getUUID());
      if (lane != null && !lane.tick()) {
         this.lanes.remove(player.getUUID());
         lane.close();
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      this.finishEmerging(player);
      this.closeLane(player);
      super.clearPlayer(player);
   }

   @Override
   public void onDimensionChange(ServerPlayer player, ServerLevel from) {
      this.finishEmerging(player);
      this.closeLane(player);
      super.onDimensionChange(player, from);
   }

   private void finishEmerging(ServerPlayer player) {
      SpeedTunnelAbility.Emergence out = this.emerging.remove(player.getUUID());
      if (out != null) {
         this.emerge(player, out);
      }
   }

   private void closeLane(ServerPlayer player) {
      TunnelLane lane = this.lanes.remove(player.getUUID());
      if (lane != null) {
         lane.close();
      }
   }

   static void closeAllLanes() {
      TunnelLane.closeAll();
   }

   public static boolean running(Player player) {
      return player != null && VelocityVisualState.of(player).tunnelling();
   }

   private static final class Emergence {
      private final Vec3 discFeet;
      private final Vec3 feet;
      private final Vec3 heading;
      private final double travelled;
      private int ticks;

      private Emergence(Vec3 discFeet, Vec3 feet, Vec3 heading, double travelled) {
         this.discFeet = discFeet;
         this.feet = feet;
         this.heading = heading;
         this.travelled = travelled;
      }
   }

   private static final class Run extends VelocityHoldAbility.Hold {
      private Vec3 entry = Vec3.ZERO;
      private Vec3 last = Vec3.ZERO;
      private Vec3 facing = new Vec3(0.0, 0.0, 1.0);
      private Vec3 dive = new Vec3(0.0, 0.0, 1.0);
      private Vec3 portalFeet = Vec3.ZERO;
      private VfxEntity portal;
      private int diveTicks;
      private boolean inside;
      private double travelled;
   }
}
