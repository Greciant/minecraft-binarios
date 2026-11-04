package com.awakenedmod.awakened.content.brawler;

import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.power.MeleeProfile;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class BrawlerMelee implements MeleeProfile {
   public static final BrawlerMelee INSTANCE = new BrawlerMelee();
   private static final Map<UUID, long[]> STREAKS = new ConcurrentHashMap<>();
   private static final MeleeProfile.ClientAim CLIENT_AIM = new MeleeProfile.ClientAim(5.2, 30.0, 6);

   private BrawlerMelee() {
   }

   @Override
   public MeleeProfile.ClientAim clientAim() {
      return CLIENT_AIM;
   }

   @Override
   public double reach(ServerPlayer player) {
      return Marks.airborneNear(player, 8.0) ? 5.2 : 4.5;
   }

   @Override
   public double coneHalfDegrees(ServerPlayer player) {
      return 30.0;
   }

   @Override
   public int cadenceTicks(ServerPlayer player) {
      return 6;
   }

   @Override
   public boolean accepts(ServerPlayer player, LivingEntity target) {
      if (!Harm.allowed(player, target)) {
         return false;
      } else {
         return (double)player.distanceTo(target) <= 4.5 ? true : Marks.current(target) == Mark.AIRBORNE;
      }
   }

   @Override
   public float damage(ServerPlayer player, LivingEntity target) {
      return MasteryScaling.scaledDamage(2.0F, player, "BrawlerPunch", MarkBonus.of(MarkBonus.PUNCH, target), BrawlerCombo.bonusFraction(player));
   }

   @Override
   public double separation(ServerPlayer player, LivingEntity target) {
      return 1.2;
   }

   @Override
   public int reopenTicks(ServerPlayer player) {
      return 6;
   }

   @Override
   public void onHit(ServerPlayer player, LivingEntity target, boolean dealt) {
      ServerLevel level = player.serverLevel();
      if (!dealt) {
         BrawlerFx.whiff(level, player);
      } else {
         reaction(player, target);
         int counted = BrawlerCombo.hits(player, target, List.of(), hitStop(player), 0);
         if (counted > 0) {
            income(player);
         }

         BrawlerFx.punch(level, target, Marks.current(target));
      }
   }

   @Override
   public void onMiss(ServerPlayer player) {
      STREAKS.remove(player.getUUID());
      BrawlerFx.whiff(player.serverLevel(), player);
   }

   private static void reaction(ServerPlayer player, LivingEntity target) {
      if (Marks.current(target) == Mark.AIRBORNE) {
         if (!Marks.locked(target)) {
            Vec3 look = player.getLookAngle();
            Harm.push(player, target, look.x * 0.05, 0.06, look.z * 0.05);
         }

         if (!(target instanceof Player)) {
            float yaw = target.getYRot() + 25.0F;
            target.setYRot(yaw);
            target.setYBodyRot(yaw);
            target.setYHeadRot(yaw);
         }
      }
   }

   private static void income(ServerPlayer player) {
      BrawlerStamina.give(player, 0.75F);
   }

   private static int hitStop(ServerPlayer player) {
      long now = player.serverLevel().getGameTime();
      long[] streak = STREAKS.compute(
         player.getUUID(), (id, current) -> current != null && now - current[1] <= 20L ? new long[]{current[0] + 1L, now} : new long[]{1L, now}
      );
      return streak[0] <= 3L ? 1 : -1;
   }

   public static void clearPlayer(ServerPlayer player) {
      STREAKS.remove(player.getUUID());
   }

   public static void clearAll() {
      STREAKS.clear();
   }
}
