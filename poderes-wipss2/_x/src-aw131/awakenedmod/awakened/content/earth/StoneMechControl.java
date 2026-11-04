package com.awakenedmod.awakened.content.earth;

import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.net.StartMechPilotPayload;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class StoneMechControl {
   private static final int FOOTFALL_EVERY = 9;
   private static final int ASSEMBLE_TICKS = 30;
   private static final Map<UUID, StoneMechControl.Pilot> PILOTS = new ConcurrentHashMap<>();

   private StoneMechControl() {
   }

   public static boolean isPiloting(ServerPlayer player) {
      return PILOTS.containsKey(player.getUUID());
   }

   public static boolean isAssembling(ServerPlayer player) {
      StoneMechControl.Pilot p = PILOTS.get(player.getUUID());
      return p != null && p.mech.isAssembling();
   }

   public static ServerPlayer ownerOf(StoneMechEntity mech) {
      if (mech.level() instanceof ServerLevel level) {
         for (StoneMechControl.Pilot p : PILOTS.values()) {
            if (p.mech == mech) {
               return level.getServer().getPlayerList().getPlayer(p.owner);
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public static StoneMechEntity mechOf(ServerPlayer player) {
      StoneMechControl.Pilot p = PILOTS.get(player.getUUID());
      return p == null ? null : p.mech;
   }

   public static void begin(ServerPlayer player, StoneMechEntity mech) {
      mech.setNoAi(true);
      mech.getNavigation().stop();
      mech.setDeltaMovement(Vec3.ZERO);
      StoneMechControl.Pilot pilot = new StoneMechControl.Pilot(mech, player.getUUID(), player.getYRot(), player.getXRot());
      PILOTS.put(player.getUUID(), pilot);
      player.startRiding(mech, true);
      mech.setAssembling(true);
      startAction(pilot, mech, (byte)1, 30, -1);
      PacketDistributor.sendToPlayer(player, new StartMechPilotPayload(mech.getId()), new CustomPacketPayload[0]);
   }

   public static void input(ServerPlayer player, float yaw, float pitch, float forward, float strafe, boolean jump, boolean sprint) {
      StoneMechControl.Pilot p = PILOTS.get(player.getUUID());
      if (p != null) {
         p.yaw = yaw;
         p.pitch = pitch;
         p.forward = forward;
         p.strafe = strafe;
         p.jump = jump;
         p.sprint = sprint;
      }
   }

   public static void action(ServerPlayer player, int action) {
      StoneMechControl.Pilot p = PILOTS.get(player.getUUID());
      if (p != null) {
         switch (action) {
            case 0:
               StoneMechAttacks.hammer(player, p);
               break;
            case 1:
               StoneMechAttacks.boulder(player, p);
               break;
            case 2:
               crumble(player);
         }
      }
   }

   public static void tick(ServerPlayer player) {
      StoneMechControl.Pilot p = PILOTS.get(player.getUUID());
      if (p != null) {
         StoneMechEntity mech = p.mech;
         if (mech.isAlive() && mech.level() == player.serverLevel() && player.getVehicle() == mech) {
            if (p.hammerCd > 0) {
               p.hammerCd--;
            }

            if (p.boulderCd > 0) {
               p.boulderCd--;
            }

            if (p.poundCd > 0) {
               p.poundCd--;
            }

            if (p.action == 1) {
               mech.setDeltaMovement(Vec3.ZERO);
               advanceAction(player, p, mech);
            } else {
               boolean wasOnGround = mech.onGround();
               boolean leap = p.jump && wasOnGround && p.poundCd <= 0 && p.action == 0;
               MechMotion.step(mech, p.yaw, p.pitch, p.forward, p.strafe, p.jump, p.sprint, leap);
               mech.fallDistance = 0.0F;
               StoneMechAttacks.tickGroundPound(player, p, mech, leap, wasOnGround);
               advanceAction(player, p, mech);
               footfall(mech);
            }
         } else {
            crumble(player);
         }
      }
   }

   private static void advanceAction(ServerPlayer player, StoneMechControl.Pilot p, StoneMechEntity mech) {
      if (p.action != 0 && p.action != 3) {
         p.actionTicks++;
         mech.setActionProgress(Math.min(1.0F, (float)p.actionTicks / (float)Math.max(1, p.actionDuration)));
         if (p.strikeAt >= 0 && p.actionTicks >= p.strikeAt) {
            p.strikeAt = -1;
            StoneMechAttacks.onStrike(player, p, mech, p.action);
         }

         if (p.actionTicks >= p.actionDuration) {
            if (p.action == 1) {
               mech.setAssembling(false);
            }

            p.action = 0;
            p.actionTicks = 0;
            mech.setAction((byte)0);
            mech.setActionProgress(0.0F);
         }
      }
   }

   static void startAction(StoneMechControl.Pilot p, StoneMechEntity mech, byte action, int duration, int strikeAt) {
      p.action = action;
      p.actionTicks = 0;
      p.actionDuration = Math.max(1, duration);
      p.strikeAt = strikeAt;
      mech.setAction(action);
      mech.setActionProgress(0.0F);
   }

   private static void footfall(StoneMechEntity mech) {
      if (mech.level() instanceof ServerLevel level && mech.onGround()) {
         double speedSqr = mech.getDeltaMovement().horizontalDistanceSqr();
         if (!(speedSqr < 0.001) && mech.tickCount % 9 == 0) {
            Vec3 feet = mech.position();
            Fx.groundImpact(level, feet, 1.6, false);
            Fx.shake(level, feet, 16.0, 0.1F, 0, 6);
            return;
         }

         return;
      }
   }

   public static void crumble(ServerPlayer player) {
      StoneMechControl.Pilot p = PILOTS.remove(player.getUUID());
      if (p != null) {
         player.stopRiding();
         PacketDistributor.sendToPlayer(player, new StartMechPilotPayload(-1), new CustomPacketPayload[0]);
         StoneMechEntity mech = p.mech;
         if (mech != null && !mech.isRemoved()) {
            if (mech.level() instanceof ServerLevel level) {
               EarthFx.mechCrumble(level, mech.position());
            }

            mech.discard();
         }
      }
   }

   static final class Pilot {
      final StoneMechEntity mech;
      final UUID owner;
      float yaw;
      float pitch;
      float forward;
      float strafe;
      boolean jump;
      boolean sprint;
      int hammerCd;
      int boulderCd;
      int poundCd;
      byte action = 0;
      int actionTicks;
      int actionDuration;
      int strikeAt = -1;
      boolean poundAirborne;
      int poundAirTicks;
      int poundRecover;

      Pilot(StoneMechEntity mech, UUID owner, float yaw, float pitch) {
         this.mech = mech;
         this.owner = owner;
         this.yaw = yaw;
         this.pitch = pitch;
      }
   }
}
