package com.awakenedmod.awakened.content.chronokinetic;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class RewindAbility extends Ability {
   private static final float COST = 30.0F;
   private static final int UNLOCK = 7;
   private static final int HISTORY_TICKS = 60;
   private static final double AIM_RANGE = 24.0;
   private static final double RECORD_RADIUS = 28.0;
   private static final int IFRAME_TICKS = 12;
   private static final float LOW_HEALTH = 6.0F;
   private static final int PRUNE_INTERVAL = 40;
   private static final int GLIDE_TICKS = 8;
   private final Map<UUID, Deque<RewindAbility.Snapshot>> selfHistory = this.playerState();
   private final Map<Integer, Deque<RewindAbility.Snapshot>> entityHistory = new ConcurrentHashMap<>();
   private final Map<Integer, RewindAbility.Glide> gliding = new ConcurrentHashMap<>();
   private long lastGlideTick = Long.MIN_VALUE;

   public RewindAbility() {
      super("rewind", 30.0F, 7);
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (player.isShiftKeyDown()) {
         return this.rewindSelf(player);
      } else {
         LivingEntity aimed = AimUtil.aimedLivingEntity(player, 24.0);
         return aimed == null ? this.fail(player, "awakened.chronokinetic.rewind_no_target", new Object[0]) : this.rewindEntity(player, aimed);
      }
   }

   private boolean rewindSelf(ServerPlayer player) {
      Deque<RewindAbility.Snapshot> buffer = this.selfHistory.get(player.getUUID());
      if (buffer != null && !buffer.isEmpty()) {
         RewindAbility.Snapshot past = buffer.peekFirst();
         ServerLevel level = player.serverLevel();
         boolean wasInDanger = player.getHealth() <= 6.0F;
         List<Vec3> path = pathOf(buffer, player.position());
         teleportToSnapshot(player, past);
         ChronoFx.rewind(player.serverLevel(), player, player.position(), 0, path);
         player.setDeltaMovement(Vec3.ZERO);
         player.hurtMarked = true;
         player.resetFallDistance();
         player.clearFire();
         float before = player.getHealth() + player.getAbsorptionAmount();
         restoreHealth(player, past);
         float healed = player.getHealth() + player.getAbsorptionAmount() - before;
         player.invulnerableTime = Math.max(player.invulnerableTime, 12);
         buffer.clear();
         ChronoSounds.rewind().play(player.serverLevel(), player.position());
         player.displayClientMessage(
            Component.translatable("awakened.chronokinetic.rewound", new Object[]{String.format("%.0f", healed)}).withStyle(ChatFormatting.GOLD), true
         );
         if (wasInDanger) {
            MasterySystem.award(player, MasteryReason.SURVIVAL);
         }

         return true;
      } else {
         return this.fail(player, "awakened.chronokinetic.no_past", new Object[0]);
      }
   }

   private boolean rewindEntity(ServerPlayer player, LivingEntity target) {
      Deque<RewindAbility.Snapshot> buffer = this.entityHistory.get(target.getId());
      if (buffer != null && !buffer.isEmpty()) {
         RewindAbility.Snapshot past = buffer.peekFirst();
         ServerLevel level = player.serverLevel();
         boolean mayDisplace = Harm.allowed(player, target);
         if (!mayDisplace && !(target instanceof ServerPlayer)) {
            return this.fail(player, "awakened.feedback.teammate", new Object[0]);
         } else {
            List<Vec3> path = pathOf(buffer, target.position());
            if (target instanceof ServerPlayer victim) {
               if (mayDisplace) {
                  teleportToSnapshot(victim, past);
                  victim.setDeltaMovement(Vec3.ZERO);
                  victim.hurtMarked = true;
                  victim.resetFallDistance();
               }

               restoreHealth(victim, past);
               if (mayDisplace) {
                  ChronoFx.rewind(Dmg.serverLevel(victim), victim, victim.position(), 0, path);
               }
            } else if (past.dimension.equals(target.level().dimension())) {
               Vec3 to = new Vec3(past.x, past.y, past.z);
               this.gliding.put(target.getId(), new RewindAbility.Glide(target, target.position(), to));
               ChronoFx.rewind(level, target, to, 8, path);
            }

            buffer.clear();
            ChronoSounds.rewind().play(level, target.position());
            player.displayClientMessage(
               Component.translatable("awakened.chronokinetic.rewound_other", new Object[]{target.getName()}).withStyle(ChatFormatting.GOLD), true
            );
            MasterySystem.award(player, MasteryReason.HIT, target);
            return true;
         }
      } else {
         return this.fail(player, "awakened.chronokinetic.target_no_past", new Object[0]);
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      this.advanceGlides(player);
      Deque<RewindAbility.Snapshot> self = this.selfHistory.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>());
      push(self, player);

      for (LivingEntity e : AimUtil.livingAround(player, player.position(), 28.0)) {
         push(this.entityHistory.computeIfAbsent(e.getId(), k -> new ArrayDeque<>()), e);
      }

      if (player.tickCount % 40 == 0 && !this.entityHistory.isEmpty()) {
         Set<Integer> dead = new HashSet<>();

         for (Integer id : this.entityHistory.keySet()) {
            if (level.getEntity(id) == null) {
               dead.add(id);
            }
         }

         this.entityHistory.keySet().removeAll(dead);
      }
   }

   private static void restoreHealth(LivingEntity e, RewindAbility.Snapshot past) {
      e.setHealth(Math.max(e.getHealth(), past.health));
      e.setAbsorptionAmount(Math.max(e.getAbsorptionAmount(), past.absorption));
   }

   private static List<Vec3> pathOf(Deque<RewindAbility.Snapshot> buffer, Vec3 now) {
      List<Vec3> path = new ArrayList<>(buffer.size() + 1);
      path.add(now);
      ResourceKey<Level> dim = buffer.peekLast() != null ? buffer.peekLast().dimension : null;
      Iterator<RewindAbility.Snapshot> it = buffer.descendingIterator();

      while (it.hasNext()) {
         RewindAbility.Snapshot s = it.next();
         if (!s.dimension.equals(dim)) {
            break;
         }

         path.add(new Vec3(s.x, s.y, s.z));
      }

      return path;
   }

   private static void push(Deque<RewindAbility.Snapshot> buffer, LivingEntity e) {
      buffer.addLast(new RewindAbility.Snapshot(e.getX(), e.getY(), e.getZ(), e.getHealth(), e.getAbsorptionAmount(), e.level().dimension()));

      while (buffer.size() > 60) {
         buffer.removeFirst();
      }
   }

   private static void teleportToSnapshot(ServerPlayer entity, RewindAbility.Snapshot past) {
      if (past.dimension.equals(entity.level().dimension())) {
         entity.teleportTo(past.x, past.y, past.z);
      } else {
         ServerLevel target = entity.level().getServer().getLevel(past.dimension);
         if (target == null) {
            entity.teleportTo(past.x, past.y, past.z);
         } else {
            entity.teleportTo(target, past.x, past.y, past.z, Set.of(), entity.getYRot(), entity.getXRot());
         }
      }
   }

   private void advanceGlides(ServerPlayer anyPlayer) {
      if (!this.gliding.isEmpty()) {
         long tick = (long)anyPlayer.level().getServer().getTickCount();
         if (tick != this.lastGlideTick) {
            this.lastGlideTick = tick;
            Iterator<RewindAbility.Glide> it = this.gliding.values().iterator();

            while (it.hasNext()) {
               RewindAbility.Glide g = it.next();
               LivingEntity e = g.entity;
               if (e != null && e.isAlive() && e.level() instanceof ServerLevel level) {
                  g.age++;
                  float var16 = Math.min(1.0F, (float)g.age / 8.0F);
                  float eased = 1.0F - (1.0F - var16) * (1.0F - var16);
                  double x = g.from.x + (g.to.x - g.from.x) * (double)eased;
                  double y = g.from.y + (g.to.y - g.from.y) * (double)eased;
                  double z = g.from.z + (g.to.z - g.from.z) * (double)eased;
                  e.setPos(x, y, z);
                  e.setDeltaMovement(Vec3.ZERO);
                  e.hurtMarked = true;
                  e.resetFallDistance();
                  if (var16 >= 1.0F) {
                     e.setPos(g.to.x, g.to.y, g.to.z);
                     it.remove();
                  }
               } else {
                  it.remove();
               }
            }
         }
      }
   }

   private static final class Glide {
      private final LivingEntity entity;
      private final Vec3 from;
      private final Vec3 to;
      private int age;

      private Glide(LivingEntity entity, Vec3 from, Vec3 to) {
         this.entity = entity;
         this.from = from;
         this.to = to;
      }
   }

   private static record Snapshot(double x, double y, double z, float health, float absorption, ResourceKey<Level> dimension) {
   }
}
