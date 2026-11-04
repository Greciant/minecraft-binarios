package com.devilfruits.fruits.kilo_kilo.weight;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.EntityMassProvider;
import com.devilfruits.core.rules.DFGameRules;
import com.devilfruits.core.util.AirTime;
import com.devilfruits.fruits.kilo_kilo.KiloAbilities;
import com.devilfruits.fruits.kilo_kilo.KiloFx;
import com.devilfruits.fruits.kilo_kilo.KiloKilo;
import com.devilfruits.registry.DFAttachments;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class WeightManager {
   public static final ResourceLocation MODIFIER = KiloKilo.id("weight");
   public static final double IMMOVABLE_KG = 3000.0;
   private static final Map<Player, Double> APPLIED = new WeakHashMap<>();
   private static WeightCurves.Tuning tuning = WeightCurves.Tuning.DEFAULT;
   private static long tuningAt = Long.MIN_VALUE;
   public static final EntityMassProvider MASS = new EntityMassProvider() {
      @Override
      public double massKg(Entity entity) {
         if (entity instanceof Player player) {
            Optional<BodyWeight> w = player.getExistingData(KiloKilo.WEIGHT);
            if (w.isPresent()) {
               return w.get().kg();
            }
         }

         return Double.NaN;
      }

      @Override
      public boolean immovable(Entity entity) {
         if (entity instanceof Player player
            && player.getExistingData(KiloKilo.WEIGHT).map(w -> w.kg() >= 3000.0).orElse(false)
            && WeightManager.flag("heavyIgnoresPushing", true)) {
            return true;
         }

         return false;
      }
   };

   public static WeightCurves.Tuning tuning() {
      long now = System.currentTimeMillis();
      if (now - tuningAt > 1000L) {
         tuningAt = now;

         try {
            tuning = new WeightCurves.Tuning(
               setting("lightJumpBlocks"),
               setting("lightTerminalSpeed"),
               setting("heavyGravityMultiplier"),
               setting("maxFallSpeed"),
               setting("lightSpeedBonus"),
               setting("heavySpeedMultiplier"),
               setting("lightStepHeight"),
               setting("maxKnockbackMultiplier"),
               setting("lightDrainPerSecond"),
               setting("heavyDrainPerSecond"),
               setting("drainDeadZone")
            );
         } catch (RuntimeException var3) {
            tuning = WeightCurves.Tuning.DEFAULT;
         }
      }

      return tuning;
   }

   public static double setting(String name) {
      return KiloKilo.fruit().config().setting(name);
   }

   public static double setting(String name, double fallback) {
      try {
         return setting(name);
      } catch (RuntimeException var4) {
         return fallback;
      }
   }

   public static boolean flag(String name, boolean fallback) {
      try {
         return KiloKilo.fruit().config().flag(name);
      } catch (RuntimeException var3) {
         return fallback;
      }
   }

   public static double defaultRate() {
      return WeightCurves.rate(setting("rampSecondsFullRange", 3.0));
   }

   public static double targetOf(int mode) {
      return switch (mode) {
         case 1 -> 1.0;
         case 2 -> 10000.0;
         default -> 70.0;
      };
   }

   public static int wantedMode(PlayerFruitData data) {
      if (data.isToggleActive(KiloAbilities.ONE_KILO.id())) {
         return 1;
      } else {
         return data.isToggleActive(KiloAbilities.PRESS.id()) ? 2 : 0;
      }
   }

   public static void tick(ServerPlayer player) {
      Optional<BodyWeight> existing = player.getExistingData(KiloKilo.WEIGHT);
      if (!KiloKilo.isUser(player)) {
         if (existing.isPresent()) {
            if (!existing.get().normal()) {
               existing.get().reset();
               sync(player);
            }

            clearAttributes(player);
            HeavyBody.forget(player);
         }
      } else {
         BodyWeight w = KiloKilo.weight(player);
         PlayerFruitData data = DFAttachments.data(player);
         WeightCurves.Tuning t = tuning();
         boolean changed = false;
         int mode = wantedMode(data);
         if (mode != w.mode()) {
            double rate = 0.0;
            if (mode == 0 && data.energy() <= 0.01F && !AbilityManager.freeCast(player, data)) {
               rate = WeightCurves.rateBetween(w.kg(), 70.0, setting("exhaustedReturnSeconds", 2.0));
            }

            aim(player, w, mode, rate, false);
            changed = true;
         }

         if (w.advance(defaultRate())) {
            w.setRate(0.0);
            ServerFx.send(player, KiloFx.RAMP_DONE, player.position(), Vec3.ZERO, (float)w.scale(), player.getRandom().nextLong(), 0);
            changed = true;
         }

         double drain = WeightCurves.drainPerSecond(w.kg(), t);
         if (drain > 0.0 && !AbilityManager.freeCast(player, data)) {
            data.setEnergy((float)Math.max(0.0, (double)data.energy() - drain / 20.0));
         }

         applyAttributes(player, w.kg(), t);
         if (changed || w.ramping() && player.tickCount % 4 == 0) {
            sync(player);
         }

         HeavyBody.tick(player, w, t);
         if (WeightCurves.lightness(w.kg()) > 0.1 && !player.onGround()) {
            AirTime.keepAirborne(player);
         }
      }
   }

   public static void aim(ServerPlayer player, BodyWeight w, int mode, double rate, boolean fast) {
      double target = targetOf(mode);
      w.aim(mode, target, rate);
      ServerFx.send(player, KiloFx.RAMP_START, player.position(), Vec3.ZERO, (float)WeightCurves.scale(target), player.getRandom().nextLong(), fast ? 1 : 0);
      sync(player);
   }

   public static void sync(ServerPlayer player) {
      player.syncData(KiloKilo.WEIGHT);
   }

   public static void applyAttributes(Player player, double kg, WeightCurves.Tuning t) {
      Double last = APPLIED.get(player);
      if (last == null || !(Math.abs(Math.log(last) - Math.log(kg)) < 1.0E-4)) {
         APPLIED.put(player, kg);
         set(player, Attributes.GRAVITY, WeightCurves.gravity(kg, t) / 0.08 - 1.0, Operation.ADD_MULTIPLIED_TOTAL);
         set(player, Attributes.JUMP_STRENGTH, WeightCurves.jumpStrength(kg, t) / 0.42 - 1.0, Operation.ADD_MULTIPLIED_TOTAL);
         set(player, Attributes.SAFE_FALL_DISTANCE, WeightCurves.safeFallDistance(kg) - 3.0, Operation.ADD_VALUE);
         set(player, Attributes.FALL_DAMAGE_MULTIPLIER, -1.0, Operation.ADD_MULTIPLIED_TOTAL);
         set(player, Attributes.MOVEMENT_SPEED, WeightCurves.speedMultiplier(kg, t) - 1.0, Operation.ADD_MULTIPLIED_TOTAL);
         set(player, Attributes.STEP_HEIGHT, WeightCurves.stepHeight(kg, t) - 0.6, Operation.ADD_VALUE);
         double resist = WeightCurves.knockbackResistance(kg);
         set(player, Attributes.KNOCKBACK_RESISTANCE, resist, Operation.ADD_VALUE);
         set(player, Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, resist, Operation.ADD_VALUE);
      }
   }

   private static void set(Player player, Holder<Attribute> attribute, double value, Operation op) {
      AttributeInstance instance = player.getAttribute(attribute);
      if (instance != null) {
         AttributeModifier old = instance.getModifier(MODIFIER);
         if (old == null || !(Math.abs(old.amount() - value) < 1.0E-9) || old.operation() != op) {
            instance.addOrUpdateTransientModifier(new AttributeModifier(MODIFIER, value, op));
         }
      }
   }

   public static void clearAttributes(Player player) {
      if (APPLIED.remove(player) != null) {
         for (Holder<Attribute> a : List.of(
            Attributes.GRAVITY,
            Attributes.JUMP_STRENGTH,
            Attributes.SAFE_FALL_DISTANCE,
            Attributes.FALL_DAMAGE_MULTIPLIER,
            Attributes.MOVEMENT_SPEED,
            Attributes.STEP_HEIGHT,
            Attributes.KNOCKBACK_RESISTANCE,
            Attributes.EXPLOSION_KNOCKBACK_RESISTANCE
         )) {
            AttributeInstance instance = player.getAttribute(a);
            if (instance != null) {
               instance.removeModifier(MODIFIER);
            }
         }
      }
   }

   public static void settle(ServerPlayer player) {
      if (KiloKilo.isUser(player)) {
         BodyWeight w = KiloKilo.weight(player);
         PlayerFruitData data = DFAttachments.data(player);
         if (persists(player) && w.mode() != 0) {
            data.setToggle(((Ability)(w.mode() == 1 ? KiloAbilities.ONE_KILO : KiloAbilities.PRESS)).id(), true);
            w.setPlunging(false);
         } else {
            w.reset();
         }

         APPLIED.remove(player);
         applyAttributes(player, w.kg(), tuning());
         HeavyBody.forget(player);
         sync(player);
      }
   }

   public static boolean persists(ServerPlayer player) {
      return flag("keepWeightWithKeepFruit", false) && DFGameRules.keepFruit(player);
   }

   private WeightManager() {
   }
}
