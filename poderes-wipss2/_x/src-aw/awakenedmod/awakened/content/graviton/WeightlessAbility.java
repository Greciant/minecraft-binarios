package com.awakenedmod.awakened.content.graviton;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.Lab;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class WeightlessAbility extends Ability {
   public static final String ID = "weightless";
   public static final float COST = 28.0F;
   public static final int UNLOCK = 5;
   public static final double RANGE = 24.0;
   public static final double RADIUS = 5.0;
   public static final int PREVIEW_TICKS = 6;
   private static final int MAX_HOLD_TICKS = 100;
   private static final float DROP_DAMAGE = 3.0F;
   private static final Lab.Value DROP_PER_BLOCK = Lab.SERVER.def("graviton.weightless.drop_per_block", 1.5, 0.0, 5.0, 0.1);
   public static final int MAX_DROP = 6;
   static final String ANIM = "weightless";
   private final Map<UUID, List<WeightlessField>> fields = this.playerState();
   private final Map<UUID, WeightlessAbility.Aiming> aiming = this.playerState();
   private int serial;

   public WeightlessAbility() {
      super("weightless", 28.0F, 5);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().boostsDamage().build();
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      UUID id = player.getUUID();
      if (this.aiming.containsKey(id)) {
         return this.failSilently(player);
      } else {
         Vec3 center = aim(player);
         if (center == null) {
            return this.fail(player, "awakened.graviton.no_spot", new Object[0]);
         } else {
            this.aiming.put(id, new WeightlessAbility.Aiming(center, this.effectiveCost(player)));
            return true;
         }
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      WeightlessAbility.Aiming held = this.aiming.remove(player.getUUID());
      if (held != null) {
         if (held.ticks < 6) {
            this.castAt(player, held.spot);
         } else {
            Vec3 center = aim(player);
            if (center == null) {
               this.resource(player).refund(player, held.cost);
               this.fail(player, "awakened.graviton.no_spot", new Object[0]);
            } else {
               this.castAt(player, center);
            }
         }
      }
   }

   public void castAt(ServerPlayer player, Vec3 center) {
      ServerLevel level = player.serverLevel();
      float drop = MasteryScaling.scaledDamage(3.0F, player, this.balanceKey());
      float perBlock = MasteryScaling.scaledDamage((float)DROP_PER_BLOCK.get(), player, this.balanceKey());
      double rise = WeightlessField.RISE.get() * (double)MasteryScaling.multiplier(PowerService.data(player).masteryLevel());
      boolean crowded = WeightlessField.crowded(level, center, 5.0);
      WeightlessField field = new WeightlessField(level, center, 5.0, rise, drop, perBlock, this.serial++);
      this.fields.computeIfAbsent(player.getUUID(), k -> new ArrayList<>()).add(field);
      WeightlessFx.cast(level, center, 5.0, rise, 60, crowded);
      PowerService.playAnimation(player, "weightless");
   }

   private static Vec3 aim(ServerPlayer player) {
      LivingEntity foe = AimUtil.aimedLivingEntity(player, 24.0, e -> Harm.allowed(player, e));
      return foe != null
         ? AimUtil.groundedPositionIfFound(player.serverLevel(), foe.position()).orElse(foe.position())
         : AimUtil.groundedPosition(player, 24.0, 6);
   }

   @Override
   public void serverTick(ServerPlayer player) {
      this.tickAiming(player);
      List<WeightlessField> list = this.fields.get(player.getUUID());
      if (list != null) {
         Iterator<WeightlessField> it = list.iterator();

         while (it.hasNext()) {
            WeightlessField field = it.next();
            if (field.level() != player.serverLevel()) {
               field.release();
               it.remove();
            } else if (!field.tick(player)) {
               it.remove();
            }
         }

         if (list.isEmpty()) {
            this.fields.remove(player.getUUID());
         }
      }
   }

   private void tickAiming(ServerPlayer player) {
      WeightlessAbility.Aiming held = this.aiming.get(player.getUUID());
      if (held != null) {
         held.ticks++;
         Vec3 center = aim(player);
         if (center != null) {
            held.spot = center;
         }

         if (held.ticks >= 100) {
            this.aiming.remove(player.getUUID());
            this.castAt(player, held.spot);
         }
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      WeightlessAbility.Aiming held = this.aiming.remove(player.getUUID());
      if (held != null) {
         this.resource(player).refund(player, held.cost);
      }

      List<WeightlessField> list = this.fields.remove(player.getUUID());
      if (list != null) {
         list.forEach(WeightlessField::release);
      }

      super.clearPlayer(player);
   }

   private static final class Aiming {
      Vec3 spot;
      final float cost;
      int ticks;

      Aiming(Vec3 spot, float cost) {
         this.spot = spot;
         this.cost = cost;
      }
   }
}
