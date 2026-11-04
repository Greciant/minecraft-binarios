package com.awakenedmod.awakened.content.ruin;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.terrain.CarveReads;
import com.awakenedmod.awakened.core.terrain.TempTerrain;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

public final class RuinCollapseAbility extends RuinAbility {
   public static final String ID = "ruin_collapse";
   private final RuinCollapseControl control = new RuinCollapseControl();
   private final Map<UUID, RuinCollapseAbility.Tell> tells = this.activeState();

   public RuinCollapseAbility() {
      super("ruin_collapse", 70.0F, 10);
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (busy(player)) {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      } else {
         ServerLevel level = player.serverLevel();
         long now = level.getGameTime();
         if (!player.onGround()) {
            return this.fail(player, "awakened.ruin.collapse_no_ground", new Object[0]);
         } else {
            BlockPos feet = player.blockPosition();
            double radius = radius(player);
            int depth = depth(player);
            CarveReads reads = TempTerrain.reads(level, player);
            RuinCrater.Planner planner = RuinCrater.plan(level, feet, radius, depth, feet.asLong() ^ now, reads);
            if (!planner.mayRot()) {
               return this.refused(player);
            } else {
               float burst = MasteryScaling.scaledDamage(1.0F, player, this.balanceKey());
               this.control.begin(player, level, planner, reads, radius, depth, burst);
               this.tells.put(player.getUUID(), new RuinCollapseAbility.Tell(now + 11L, this.effectiveCost(player)));
               this.startCooldown(player, 600);
               return true;
            }
         }
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      RuinCollapseAbility.Tell tell = this.tells.get(player.getUUID());
      if (!this.control.tick(player) && tell != null) {
         this.tells.remove(player.getUUID());
         this.resource(player).refund(player, tell.cost());
         this.refused(player);
      } else {
         if (tell != null && player.serverLevel().getGameTime() >= tell.landsAt()) {
            this.tells.remove(player.getUUID());
         }
      }
   }

   private boolean refused(ServerPlayer player) {
      this.startCooldown(player, 40);
      return this.fail(player, "awakened.ruin.nothing_to_rot", new Object[0]);
   }

   private static double radius(ServerPlayer player) {
      double r = Mth.lerp(masteryProgress(player), 16.0, 48.0);
      return Math.min(64.0, r * (double)(1.0F + MasteryScaling.abilityPower(player)));
   }

   private static int depth(ServerPlayer player) {
      return (int)Math.round(Mth.lerp(masteryProgress(player), 6.0, 12.0));
   }

   private static double masteryProgress(ServerPlayer player) {
      int past = PowerService.data(player).masteryLevel() - 10;
      return Mth.clamp((double)past / 10.0, 0.0, 1.0);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(1.0).boostsRadius().build();
   }

   @Override
   public boolean requiresFootingWhenRealistic() {
      return true;
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      this.control.clear(player);
      super.clearPlayer(player);
   }

   private static record Tell(long landsAt, float cost) {
   }
}
