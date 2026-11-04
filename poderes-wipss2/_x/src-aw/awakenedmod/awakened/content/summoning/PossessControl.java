package com.awakenedmod.awakened.content.summoning;

import com.awakenedmod.awakened.core.BodyPinState;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.net.StartPossessPayload;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PossessControl {
   private static final Map<UUID, PossessControl.Pilot> PILOTS = new ConcurrentHashMap<>();
   private static final Set<Integer> PILOTED = ConcurrentHashMap.newKeySet();
   private static final double FRAY_MARGIN = 32.0;
   private static final double BRING_HOME_DIST = 24.0;

   private PossessControl() {
   }

   public static boolean isPiloting(ServerPlayer player) {
      return PILOTS.containsKey(player.getUUID());
   }

   public static boolean anyPiloting() {
      return !PILOTS.isEmpty();
   }

   public static Mob piloted(ServerPlayer player) {
      PossessControl.Pilot pilot = PILOTS.get(player.getUUID());
      return pilot == null ? null : pilot.mob;
   }

   public static Vec3 pilotViewPos(ServerPlayer player) {
      if (PILOTS.isEmpty()) {
         return null;
      } else {
         PossessControl.Pilot p = PILOTS.get(player.getUUID());
         return p != null && p.mob != null && p.mob.isAlive() ? p.mob.position() : null;
      }
   }

   public static boolean isPiloted(Entity entity) {
      return !PILOTED.isEmpty() && PILOTED.contains(entity.getId());
   }

   public static void begin(ServerPlayer player, Mob mob) {
      if (SummonFx.isRising(mob)) {
         SummonFx.settleNow(Dmg.serverLevel(player), mob);
      }

      PILOTS.put(player.getUUID(), new PossessControl.Pilot(mob, player.position(), player.getYRot(), player.getXRot()));
      BodyPinState.setServer(player.getUUID(), true);
      player.setDeltaMovement(Vec3.ZERO);
      player.hurtMarked = true;
      suspend(mob, true);
      PacketDistributor.sendToPlayer(player, new StartPossessPayload(mob.getId()), new CustomPacketPayload[0]);
   }

   public static void input(ServerPlayer player, float yaw, float pitch, float forward, float strafe, boolean jump, boolean sprint) {
      PossessControl.Pilot p = PILOTS.get(player.getUUID());
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
      PossessControl.Pilot p = PILOTS.get(player.getUUID());
      if (p != null) {
         switch (action) {
            case 0:
               if (p.attackCooldown <= 0) {
                  p.attackCooldown = PossessAttacks.attack(player.getUUID(), p.mob);
               }
               break;
            case 1:
               end(player, true);
               break;
            case 2:
               cycle(player, p);
         }
      }
   }

   public static void tick(ServerPlayer player) {
      PossessControl.Pilot p = PILOTS.get(player.getUUID());
      if (p != null) {
         if (p.attackCooldown > 0) {
            p.attackCooldown--;
         }

         PossessAttacks.tickBeam(p.mob);
         Mob mob = p.mob;
         if (mob.isAlive() && mob.level() == player.serverLevel() && SummonAttachments.minion(mob).isOwnedBy(player.getUUID())) {
            double distSqr = mob.distanceToSqr(player);
            double edge = pilotRangeBlocks(player);
            if (distSqr > edge * edge) {
               player.displayClientMessage(Component.translatable("awakened.possess.tether_lost"), true);
               MinionSupport.recall(player, p.mob);
            } else {
               double warnStart = edge - 32.0;
               if (distSqr > warnStart * warnStart) {
                  double frac = Math.max(0.0, Math.min(1.0, (Math.sqrt(distSqr) - warnStart) / 32.0));
                  warnFraying(player, mob, frac);
               }

               mob.setAirSupply(mob.getMaxAirSupply());
               pinBody(player, p);
               PossessMotion.step(mob, p.yaw, p.pitch, p.forward, p.strafe, p.jump, p.sprint);
            }
         } else {
            end(player, true);
         }
      }
   }

   private static double pilotRangeBlocks(ServerPlayer player) {
      int server = Dmg.serverLevel(player).getServer().getPlayerList().getViewDistance();
      int chunks = Math.max(2, Math.min(player.requestedViewDistance(), server));
      return Math.max(16.0, (double)chunks * 16.0 - 8.0);
   }

   private static void warnFraying(ServerPlayer player, Mob mob, double frac) {
      int interval = frac > 0.6 ? 5 : 10;
      if (player.tickCount % interval == 0) {
         ServerLevel pitch = player.serverLevel();
         if (pitch instanceof ServerLevel) {
            float pitchx = 1.2F + 0.8F * (float)frac;
            float volume = 0.35F + 0.35F * (float)frac;
            Fx.soundForViewer(pitch, player, mob.position(), (SoundEvent)SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, volume, pitchx);
         }
      }
   }

   private static void pinBody(ServerPlayer player, PossessControl.Pilot p) {
      player.setDeltaMovement(Vec3.ZERO);
      player.fallDistance = 0.0F;
      if (player.position().distanceToSqr(p.anchor) > 1.0) {
         player.teleportTo(p.anchor.x, p.anchor.y, p.anchor.z);
      }
   }

   private static void cycle(ServerPlayer player, PossessControl.Pilot p) {
      List<Mob> minions = MinionSupport.minionsOf(player);
      if (!minions.contains(p.mob)) {
         minions.add(p.mob);
      }

      if (minions.size() > 1) {
         minions.sort(Comparator.comparingInt(Entity::getId));
         int idx = 0;

         for (int i = 0; i < minions.size(); i++) {
            if (minions.get(i) == p.mob) {
               idx = i;
               break;
            }
         }

         Mob next = minions.get((idx + 1) % minions.size());
         suspend(p.mob, false);
         PossessMotion.clearVertical(p.mob);
         p.mob = next;
         suspend(next, true);
         ServerLevel var6 = player.serverLevel();
         if (var6 instanceof ServerLevel) {
            Fx.burst(var6, ParticleTypes.SCULK_SOUL, next.position().add(0.0, (double)next.getBbHeight() * 0.5, 0.0), 12, 0.3, 0.02);
         }

         PacketDistributor.sendToPlayer(player, new StartPossessPayload(next.getId()), new CustomPacketPayload[0]);
      }
   }

   public static void teleportBody(ServerPlayer player) {
      PossessControl.Pilot p = PILOTS.get(player.getUUID());
      if (p != null) {
         ServerLevel mob = player.serverLevel();
         if (mob instanceof ServerLevel) {
            Mob mobx = p.mob;
            if (mobx.isAlive() && SummonAttachments.minion(mobx).isOwnedBy(player.getUUID())) {
               Fx.burst(mob, ParticleTypes.SCULK_SOUL, player.position().add(0.0, 1.0, 0.0), 14, 0.3, 0.02);
               player.teleportTo(mobx.getX(), mobx.getY(), mobx.getZ());
               player.resetFallDistance();
               Fx.burst(mob, ParticleTypes.SCULK_SOUL, mobx.position().add(0.0, 1.0, 0.0), 18, 0.3, 0.02);
               Fx.sound(mob, mobx.position(), (SoundEvent)SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.8F, 1.1F);
            }

            end(player, true);
            return;
         }
      }
   }

   public static void end(ServerPlayer player, boolean notifyClient) {
      PossessControl.Pilot p = PILOTS.remove(player.getUUID());
      BodyPinState.setServer(player.getUUID(), false);
      if (p != null) {
         PILOTED.remove(p.mob.getId());
         PossessMotion.clearVertical(p.mob);
         PossessAttacks.clearBeam(p.mob);
         if (p.mob.isAlive()) {
            suspend(p.mob, false);
            if (p.mob.level() == player.serverLevel() && p.mob.distanceToSqr(player) > 576.0) {
               p.mob.teleportTo(player.getX(), player.getY(), player.getZ());
            }
         }

         if (notifyClient && !player.hasDisconnected()) {
            PacketDistributor.sendToPlayer(player, new StartPossessPayload(-1), new CustomPacketPayload[0]);
         }
      }
   }

   public static void stopPiloting(Mob mob) {
      for (Entry<UUID, PossessControl.Pilot> e : PILOTS.entrySet()) {
         if (e.getValue().mob == mob) {
            Mob m = e.getValue().mob;
            PILOTS.remove(e.getKey());
            BodyPinState.setServer(e.getKey(), false);
            PILOTED.remove(m.getId());
            PossessMotion.clearVertical(m);
            PossessAttacks.clearBeam(m);
            if (m.isAlive()) {
               suspend(m, false);
            }

            Level owner = m.level();
            if (owner instanceof ServerLevel) {
               ServerLevel level = (ServerLevel)owner;
               ServerPlayer ownerx = level.getServer().getPlayerList().getPlayer(e.getKey());
               if (ownerx != null) {
                  PacketDistributor.sendToPlayer(ownerx, new StartPossessPayload(-1), new CustomPacketPayload[0]);
               }
            }
         }
      }
   }

   private static void suspend(Mob mob, boolean on) {
      mob.setNoAi(on);
      if (on) {
         PILOTED.add(mob.getId());
         mob.getNavigation().stop();
         mob.setDeltaMovement(Vec3.ZERO);
      } else {
         PILOTED.remove(mob.getId());
         PossessAttacks.clearBeam(mob);
      }
   }

   private static final class Pilot {
      private Mob mob;
      private final Vec3 anchor;
      private float yaw;
      private float pitch;
      private float forward;
      private float strafe;
      private boolean jump;
      private boolean sprint;
      private int attackCooldown;

      private Pilot(Mob mob, Vec3 anchor, float yaw, float pitch) {
         this.mob = mob;
         this.anchor = anchor;
         this.yaw = yaw;
         this.pitch = pitch;
      }
   }
}
