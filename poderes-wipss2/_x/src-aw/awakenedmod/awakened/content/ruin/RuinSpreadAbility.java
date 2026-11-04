package com.awakenedmod.awakened.content.ruin;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class RuinSpreadAbility extends RuinAbility {
   public static final String ID = "ruin_spread";
   private final Map<UUID, RuinSpreadAbility.Windup> windups = this.activeState();
   private final Map<UUID, List<RuinSpreadFront>> fronts = this.playerState();
   private final Map<UUID, RuinRotDrip> drips = this.playerState();

   public RuinSpreadAbility() {
      super("ruin_spread", 30.0F, 2);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().range(14.0).durationTicks(100).build();
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return "ruin_spread";
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (busy(player)) {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      } else {
         Optional<Vec3> floor = floorAtFeet(player);
         if (floor.isEmpty()) {
            return this.fail(player, "awakened.ruin.no_floor", new Object[0]);
         } else {
            this.windups.put(player.getUUID(), new RuinSpreadAbility.Windup(floor.get()));
            RuinCues.tense(player.serverLevel(), floor.get());
            return true;
         }
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      UUID id = player.getUUID();
      RuinSpreadAbility.Windup windup = this.windups.get(id);
      if (windup != null && ++windup.ticks >= 8) {
         this.windups.remove(id);
         this.slam(player, windup.pressFloor);
      }

      List<RuinSpreadFront> live = this.fronts.get(id);
      if (live != null) {
         Map<Integer, LivingEntity> onRot = new HashMap<>();
         Set<Integer> struck = new HashSet<>();
         live.removeIf(front -> front.tick(player, onRot, struck));
         this.drips.computeIfAbsent(id, k -> new RuinRotDrip()).tick(player, onRot, struck);
         if (live.isEmpty()) {
            this.fronts.remove(id);
            this.drips.remove(id);
         }
      }
   }

   private void slam(ServerPlayer player, Vec3 pressFloor) {
      ServerLevel level = player.serverLevel();
      Vec3 feet = floorAtFeet(player).orElse(pressFloor);
      Vec3 dir = AimUtil.lookFlat(player);
      double length = MasteryScaling.scaled(14.0, player);
      double grown = length / 14.0;
      RuinStrip strip = new RuinStrip(feet.add(dir.scale(0.6)), dir, length, 1.0, 3.25 * grown, 20);
      RuinSpreadFx.slam(level, player, strip);
      this.fronts.computeIfAbsent(player.getUUID(), k -> new ArrayList<>()).add(new RuinSpreadFront(level, strip));
   }

   private static Optional<Vec3> floorAtFeet(ServerPlayer player) {
      return AimUtil.groundedPositionIfFound(player.serverLevel(), player.position()).filter(p -> player.getY() - p.y <= 1.5);
   }

   private static final class Windup {
      final Vec3 pressFloor;
      int ticks;

      Windup(Vec3 pressFloor) {
         this.pressFloor = pressFloor;
      }
   }
}
