package com.awakenedmod.awakened.content.reaper;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class SoulBoltAbility extends Ability {
   private static final int UNLOCK = 0;
   private static final float BOLT_COST = 8.0F;
   private static final float BOLT_DAMAGE = 4.0F;
   private static final float VOLLEY_DAMAGE = 2.5F;
   private static final float VOLLEY_COST = 3.0F;
   private static final double WISP_RADIUS = 0.35;
   private static final double BOLT_SPEED = 1.7;
   private static final double VOLLEY_SPEED = 1.6;
   private static final double HOME_RANGE = 28.0;
   private static final double HOME_CONE = 16.0;
   private static final double BOLT_HOMING = 0.16;
   private static final double VOLLEY_HOMING = 0.08;
   private static final int BOLT_LIFE = 60;
   private static final int VOLLEY_LIFE = 44;
   private static final double SPAWN_FORWARD = 1.35;
   private static final double SPAWN_FWD_VARY = 0.4;
   private static final double SPAWN_HALF_WIDTH = 1.5;
   private static final double SPAWN_VERT_LOW = -0.5;
   private static final double SPAWN_VERT_HIGH = 0.45;
   private static final double HOVER_BOB = 0.05;
   private static final int TAP_TICKS = 6;
   private static final int CHARGE_PER_WISP = 5;
   private static final int HOVER_RENDER_INTERVAL = 2;
   private static final double SELECT_RANGE = 32.0;
   private static final double SELECT_CONE = 7.0;
   private static final int MAX_TARGETS = 8;
   private final Map<UUID, SoulBoltAbility.Hold> holds = this.activeState();

   public SoulBoltAbility() {
      super("soul_bolt", 8.0F, 0);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().boostsDamage().build();
   }

   @Override
   public float staminaCost(ServerPlayer player) {
      return 0.0F;
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (this.holds.containsKey(player.getUUID())) {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      } else {
         this.holds.put(player.getUUID(), new SoulBoltAbility.Hold());
         ReaperFx.boltCast(player.serverLevel(), AimUtil.handPosition(player));
         return true;
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      SoulBoltAbility.Hold hold = this.holds.get(player.getUUID());
      if (hold != null) {
         hold.ticks++;
         if (hold.ticks >= 6) {
            if (!hold.charging) {
               hold.charging = true;
               PowerService.playAnimation(player, "barrage");
            }

            int want = wispCount(hold.ticks, ReaperAttachments.souls(player).souls());

            while (hold.slots.size() < want) {
               SoulBoltAbility.Slot slot = newSlot(player.getRandom(), hold.slots.size());
               hold.slots.add(slot);
               ReaperFx.barrageSpark(player.serverLevel(), slotPos(player, slot, hold.ticks));
            }

            if (hold.targets.size() < 8) {
               LivingEntity aimed = AimUtil.aimedLivingSoft(player, 32.0, 7.0, ex -> ex != player && ex.isAlive());
               if (aimed != null && hold.targets.add(aimed.getId())) {
                  ReaperFx.targetSelect(player.serverLevel(), aimed);
               }
            }

            if (hold.ticks % 2 == 0) {
               for (SoulBoltAbility.Slot slot : hold.slots) {
                  ReaperFx.barrageHover(player.serverLevel(), slotPos(player, slot, hold.ticks));
               }

               for (int id : hold.targets) {
                  Entity var7 = player.serverLevel().getEntity(id);
                  if (var7 instanceof LivingEntity) {
                     LivingEntity e = (LivingEntity)var7;
                     if (e.isAlive()) {
                        ReaperFx.targetMark(player.serverLevel(), e);
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      SoulBoltAbility.Hold hold = this.holds.remove(player.getUUID());
      if (hold != null) {
         if (!hold.charging) {
            this.fireBolt(player);
         } else {
            this.fireVolley(player, hold);
            PowerService.stopAnimation(player);
         }
      }
   }

   private void fireBolt(ServerPlayer player) {
      if (!this.charge(player, 8.0F)) {
         this.fail(player, "awakened.feedback.no_stamina", new Object[0]);
      } else {
         ServerLevel level = player.serverLevel();
         Vec3 origin = AimUtil.handPosition(player);
         long souls = ReaperAttachments.souls(player).souls();
         float damage = MasteryScaling.scaledDamage(4.0F, player, this.balanceKey(), (float)ReaperScaling.offense(souls) - 1.0F);
         LivingEntity target = AimUtil.aimedLivingSoft(player, 28.0, 16.0, e -> e != player);
         this.launchWisp(player, level, origin, lookDir(player), damage, 1.7, 0.16, target, 60);
         ReaperFx.boltCast(level, origin);
         PowerService.playAnimation(player, "soul_bolt");
      }
   }

   private void fireVolley(ServerPlayer player, SoulBoltAbility.Hold hold) {
      int count = hold.slots.size();
      if (count != 0) {
         PowerData data = PowerService.data(player);
         float each = this.billed(3.0F);
         int afford = !data.infiniteStamina() && !(each <= 0.0F) ? Math.min(count, (int)(data.stamina() / each)) : count;

         while (afford > 0 && !this.charge(player, 3.0F * (float)afford)) {
            afford--;
         }

         if (afford <= 0) {
            this.fail(player, "awakened.feedback.no_stamina", new Object[0]);
            PowerService.stopAnimation(player);
         } else {
            ServerLevel level = player.serverLevel();
            long souls = ReaperAttachments.souls(player).souls();
            float damage = MasteryScaling.scaledDamage(2.5F, player, this.balanceKey(), (float)ReaperScaling.offense(souls) - 1.0F);
            Vec3 look = lookDir(player);
            List<LivingEntity> targets = this.resolveTargets(player, hold);

            for (int i = 0; i < afford; i++) {
               Vec3 origin = slotPos(player, hold.slots.get(i), hold.ticks);
               LivingEntity tgt = targets.isEmpty() ? null : targets.get(i % targets.size());
               Vec3 dir = tgt != null ? tgt.getBoundingBox().getCenter().subtract(origin).normalize() : look;
               this.launchWisp(player, level, origin, dir, damage, 1.6, 0.08, tgt, 44);
            }

            ReaperFx.barrageVolley(level, player.getEyePosition());
            PowerService.playAnimation(player, "barrage_release");
         }
      }
   }

   private void launchWisp(
      ServerPlayer player, ServerLevel level, Vec3 origin, Vec3 dir, float damage, double speed, double homing, LivingEntity target, int life
   ) {
      SoulBoltEntity wisp = new SoulBoltEntity(AwakenedEntities.SOUL_BOLT.get(), player, level);
      wisp.setPos(origin.x, origin.y, origin.z);
      wisp.configure(damage, 0.35);
      wisp.setSpeed(speed);
      wisp.setMaxLife(life);
      if (target != null) {
         wisp.setHoming(target, homing);
      }

      wisp.shoot(dir.x, dir.y, dir.z, (float)speed, 0.0F);
      level.addFreshEntity(wisp);
   }

   private static int wispCount(int ticks, long souls) {
      int n = 1 + (ticks - 6) / 5;
      return Math.max(1, Math.min(maxWisps(souls), n));
   }

   static int maxWisps(long souls) {
      int n = 5 + (int)(Math.sqrt((double)souls) / 9.0);
      return Math.max(5, Math.min(16, n));
   }

   private static SoulBoltAbility.Slot newSlot(RandomSource r, int seed) {
      double lat = (r.nextDouble() * 2.0 - 1.0) * 1.5;
      double vert = -0.5 + r.nextDouble() * 0.95;
      double fwd = 1.35 + (r.nextDouble() * 2.0 - 1.0) * 0.4;
      return new SoulBoltAbility.Slot(lat, vert, fwd, (double)seed * 1.7 + r.nextDouble() * 6.28);
   }

   private static Vec3 slotPos(ServerPlayer player, SoulBoltAbility.Slot slot, int ticks) {
      Vec3 look = lookDir(player);
      Vec3 fwd = new Vec3(look.x, 0.0, look.z);
      fwd = fwd.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : fwd.normalize();
      Vec3 right = new Vec3(fwd.z, 0.0, -fwd.x);
      double bob = Math.sin(((double)ticks + slot.phase) * 0.18) * 0.05;
      return player.getEyePosition().add(fwd.scale(slot.fwd)).add(right.scale(slot.lat)).add(0.0, slot.vert + bob, 0.0);
   }

   private List<LivingEntity> resolveTargets(ServerPlayer player, SoulBoltAbility.Hold hold) {
      List<LivingEntity> targets = new ArrayList<>();

      for (int id : hold.targets) {
         Entity var7 = player.serverLevel().getEntity(id);
         if (var7 instanceof LivingEntity) {
            LivingEntity e = (LivingEntity)var7;
            if (e.isAlive()) {
               targets.add(e);
            }
         }
      }

      if (targets.isEmpty()) {
         LivingEntity aimed = AimUtil.aimedLivingSoft(player, 28.0, 16.0, ex -> ex != player);
         if (aimed != null) {
            targets.add(aimed);
         }
      }

      return targets;
   }

   private static Vec3 lookDir(ServerPlayer player) {
      return player.getLookAngle().normalize();
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      if (this.holds.remove(player.getUUID()) != null) {
         PowerService.stopAnimation(player);
      }

      ReaperState.clear(player);
      super.clearPlayer(player);
   }

   private static final class Hold {
      private int ticks;
      private boolean charging;
      private final List<SoulBoltAbility.Slot> slots = new ArrayList<>();
      private final Set<Integer> targets = new LinkedHashSet<>();
   }

   private static record Slot(double lat, double vert, double fwd, double phase) {
   }
}
