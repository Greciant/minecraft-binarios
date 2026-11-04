package com.awakenedmod.awakened.content.graviton;

import com.awakenedmod.awakened.core.Attrs;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.PowerSuppression;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.PassiveToggle;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

public final class MassControl {
   public static final String VAR = "graviton_weight";
   public static final int MIN_TIER = -3;
   public static final int MAX_TIER = 3;
   public static final int TIER2_MASTERY = 4;
   public static final int TIER3_MASTERY = 8;
   public static final PassiveToggle MESSAGE_TOGGLE = new PassiveToggle(
      "graviton_weight_message", "awakened.screen.setting_graviton_weight_message", "awakened.screen.setting_graviton_weight_message_desc", false
   );
   private static final Set<UUID> FLOAT_HINTED = ConcurrentHashMap.newKeySet();
   private static final double VANILLA_JUMP_HEIGHT = 1.25;
   private static final double BASE_GRAVITY = 0.08;
   private static final double BASE_JUMP = 0.42;
   private static final double HEAVY3_JUMP_HEIGHT = 1.1;
   private static final double SINK_GRAVITY = 0.3;
   private static final double FLOOR_GRAVITY = 1.0;
   private static final MassControl.Tier[] TABLE = new MassControl.Tier[]{
      new MassControl.Tier(0.25, 4.25, 0.0, true, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0),
      new MassControl.Tier(0.5, 3.0, 10.0, false, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0),
      new MassControl.Tier(0.75, 2.0, 4.0, false, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0),
      new MassControl.Tier(1.0, 1.25, 0.0, false, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0),
      new MassControl.Tier(1.25, 1.25, 0.0, false, 0.5, 0.0, 1.0, 0.3, 0.0, 0.0),
      new MassControl.Tier(1.5, 1.25, 0.0, false, 1.0, 0.0, 1.0, 1.0, 1.0, 0.4),
      new MassControl.Tier(1.75, 1.1, 0.0, false, 1.0, 1.0, 0.85, 1.0, 1.0, 0.4)
   };
   private static final double[] JUMP = new double[TABLE.length];
   private static final ResourceLocation GRAVITY_ID;
   private static final ResourceLocation JUMP_ID;
   private static final ResourceLocation SAFE_FALL_ID;
   private static final ResourceLocation FALL_DAMAGE_ID;
   private static final ResourceLocation KNOCKBACK_ID;
   private static final ResourceLocation EXPLOSION_KB_ID;
   private static final ResourceLocation SPEED_ID;
   private static final ResourceLocation WATER_WALK_ID;
   private static final ResourceLocation WATER_STEP_ID;
   private static final String SECONDARY_KEY = "key.awakened.secondary_ability";

   static double solveJumpStrength(double gravity, double height) {
      double lo = 0.0;
      double hi = 5.0;

      for (int i = 0; i < 50; i++) {
         double mid = (lo + hi) / 2.0;
         if (jumpPeak(mid, gravity) < height) {
            lo = mid;
         } else {
            hi = mid;
         }
      }

      return lo;
   }

   private static double jumpPeak(double v, double gravity) {
      double y = 0.0;
      double peak = 0.0;

      for (int i = 0; i < 400 && v > 0.0; i++) {
         y += v;
         peak = Math.max(peak, y);
         v = (v - gravity) * 0.98;
      }

      return peak;
   }

   private MassControl() {
   }

   public static int tier(PowerData data) {
      int cap = reach(data);
      int stored = Math.round(data.var("graviton_weight", 0.0F));
      return Math.max(-cap, Math.min(cap, stored));
   }

   public static int reach(PowerData data) {
      return reachAt(data.masteryLevel());
   }

   public static int reachAt(int mastery) {
      return mastery >= 8 ? 3 : (mastery >= 4 ? 2 : 1);
   }

   public static double speedMultiplier(int tier) {
      return TABLE[Math.max(-3, Math.min(3, tier)) - -3].speed();
   }

   public static int unlockLevel(int step) {
      return step >= 3 ? 8 : (step == 2 ? 4 : 0);
   }

   public static int activeTier(ServerPlayer player) {
      PowerData data = PowerService.data(player);
      return "graviton".equals(data.powerId()) && data.awakened() && !PowerSuppression.isSuppressed(player) ? tier(data) : 0;
   }

   public static boolean isHeavy(PowerData data) {
      return tier(data) > 0;
   }

   public static boolean isLight(PowerData data) {
      return tier(data) < 0;
   }

   public static String nameKey(int tier) {
      return tier == 0 ? "awakened.graviton.weight.normal" : (tier > 0 ? "awakened.graviton.weight.heavy" : "awakened.graviton.weight.light") + Math.abs(tier);
   }

   public static void onKey(ServerPlayer player, boolean sneaking, boolean held) {
      if (held) {
         set(player, 0);
      } else {
         step(player, sneaking ? -1 : 1);
      }
   }

   public static void step(ServerPlayer player, int delta) {
      PowerData data = PowerService.data(player);
      int current = tier(data);
      int wanted = current + Integer.signum(delta);
      if (Math.abs(wanted) > 3) {
         player.displayClientMessage(Component.translatable("awakened.graviton.weight_max", new Object[]{Component.translatable(nameKey(current))}), true);
      } else if (Math.abs(wanted) > reach(data)) {
         player.displayClientMessage(
            Component.translatable("awakened.graviton.weight_locked", new Object[]{Component.translatable(nameKey(wanted)), unlockLevel(Math.abs(wanted))}),
            true
         );
      } else {
         set(player, wanted);
      }
   }

   public static void set(ServerPlayer player, int tier) {
      PowerData data = PowerService.data(player);
      int cap = reach(data);
      int target = Math.max(-cap, Math.min(cap, tier));
      int current = tier(data);
      if (target != current) {
         data.setVar("graviton_weight", (float)target);
         apply(player, data);
         if (MESSAGE_TOGGLE.enabled(data)) {
            player.displayClientMessage(Component.translatable("awakened.graviton.weight_set", new Object[]{Component.translatable(nameKey(target))}), true);
         }

         MassControlFx.changed(player.serverLevel(), player, current, target);
         PowerService.sync(player);
      }
   }

   public static void apply(ServerPlayer player, PowerData data) {
      int tier = tier(data);
      MassControl.Tier t = TABLE[tier - -3];
      Operation total = Operation.ADD_MULTIPLIED_TOTAL;
      Operation add = Operation.ADD_VALUE;
      boolean wet = player.isInWater();
      boolean submerged = wet && player.isUnderWater();
      double gravity = submerged && t.waterGravity() > 0.0 ? t.waterGravity() / 0.08 : t.gravity();
      Attrs.set(player, Attributes.GRAVITY, GRAVITY_ID, gravity - 1.0, total);
      Attrs.set(player, Attributes.WATER_MOVEMENT_EFFICIENCY, WATER_WALK_ID, wet ? t.waterWalk() : 0.0, add);
      Attrs.set(player, Attributes.STEP_HEIGHT, WATER_STEP_ID, wet ? t.waterStep() : 0.0, add);
      Attrs.set(player, Attributes.JUMP_STRENGTH, JUMP_ID, JUMP[tier - -3] - 1.0, total);
      Attrs.set(player, Attributes.SAFE_FALL_DISTANCE, SAFE_FALL_ID, t.safeFall(), add);
      Attrs.set(player, Attributes.FALL_DAMAGE_MULTIPLIER, FALL_DAMAGE_ID, t.noFallDamage() ? -1.0 : 0.0, total);
      Attrs.set(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, t.knockback(), add);
      Attrs.set(player, Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, EXPLOSION_KB_ID, t.explosionKnockback(), add);
      Attrs.set(player, Attributes.MOVEMENT_SPEED, SPEED_ID, t.speed() - 1.0, total);
      MassControlFx.body(player.serverLevel(), player, tier);
      floatHint(player, tier, submerged);
   }

   private static void floatHint(ServerPlayer player, int tier, boolean submerged) {
      if (!submerged) {
         FLOAT_HINTED.remove(player.getUUID());
      } else {
         if (tier > 0 && player.getAirSupply() < player.getMaxAirSupply() / 3 && FLOAT_HINTED.add(player.getUUID())) {
            player.displayClientMessage(
               Component.translatable("awakened.graviton.weight_float_hint", new Object[]{Component.keybind("key.awakened.secondary_ability")}), true
            );
         }
      }
   }

   public static void clear(ServerPlayer player) {
      FLOAT_HINTED.remove(player.getUUID());
      MassControlFx.forget(player);
      remove(player, Attributes.GRAVITY, GRAVITY_ID);
      remove(player, Attributes.JUMP_STRENGTH, JUMP_ID);
      remove(player, Attributes.SAFE_FALL_DISTANCE, SAFE_FALL_ID);
      remove(player, Attributes.FALL_DAMAGE_MULTIPLIER, FALL_DAMAGE_ID);
      remove(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID);
      remove(player, Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, EXPLOSION_KB_ID);
      remove(player, Attributes.MOVEMENT_SPEED, SPEED_ID);
      remove(player, Attributes.WATER_MOVEMENT_EFFICIENCY, WATER_WALK_ID);
      remove(player, Attributes.STEP_HEIGHT, WATER_STEP_ID);
   }

   private static void remove(ServerPlayer player, Holder<Attribute> attr, ResourceLocation id) {
      Attrs.set(player, attr, id, 0.0, Operation.ADD_VALUE);
   }

   public static void devShowOwnBody(boolean on) {
      MassControlFx.showOwnBody = on;
   }

   public static float fraction(int tier) {
      return (float)(tier - -3) / 6.0F;
   }

   static {
      for (int i = 0; i < TABLE.length; i++) {
         JUMP[i] = TABLE[i].jumpMultiplier();
      }

      GRAVITY_ID = AwId.of("graviton_weight_gravity");
      JUMP_ID = AwId.of("graviton_weight_jump");
      SAFE_FALL_ID = AwId.of("graviton_weight_safe_fall");
      FALL_DAMAGE_ID = AwId.of("graviton_weight_fall_damage");
      KNOCKBACK_ID = AwId.of("graviton_weight_knockback");
      EXPLOSION_KB_ID = AwId.of("graviton_weight_explosion_knockback");
      SPEED_ID = AwId.of("graviton_weight_speed");
      WATER_WALK_ID = AwId.of("graviton_weight_water_walk");
      WATER_STEP_ID = AwId.of("graviton_weight_water_step");
   }

   private static record Tier(
      double gravity,
      double jumpHeight,
      double safeFall,
      boolean noFallDamage,
      double knockback,
      double explosionKnockback,
      double speed,
      double waterGravity,
      double waterWalk,
      double waterStep
   ) {
      double jumpMultiplier() {
         return MassControl.solveJumpStrength(0.08 * this.gravity, this.jumpHeight) / 0.42;
      }
   }
}
