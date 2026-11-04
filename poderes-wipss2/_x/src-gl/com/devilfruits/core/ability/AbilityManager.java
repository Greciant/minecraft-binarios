package com.devilfruits.core.ability;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.CoreEvents;
import com.devilfruits.core.config.CoreConfig;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.FormAbility;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fx.CoreFx;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.network.AbilityInputPayload;
import com.devilfruits.core.physics.AutoStrike;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.core.rules.Weakness;
import com.devilfruits.registry.DFAttachments;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class AbilityManager {
   private static final ResourceLocation CHARGE_SLOW = DevilFruits.id("charging_slow");
   private static final int MAX_HOLD_EXTRA = 60;
   private static final Map<UUID, AbilityManager.ChargeState> CHARGING = new HashMap<>();
   private static final Map<UUID, AbilityManager.ChannelState> CHANNELLING = new HashMap<>();
   private static final Set<UUID> RELEASE_PENDING = new HashSet<>();
   private static final Map<UUID, AbilityManager.ComboState> COMBOS = new HashMap<>();
   private static final Map<UUID, Long> BUSY_UNTIL = new HashMap<>();

   public static void handleInput(ServerPlayer player, AbilityInputPayload payload) {
      PlayerFruitData data = DFAttachments.data(player);
      switch (payload.action()) {
         case SELECT:
            FruitForm form = data.form();
            if (form != null && payload.slot() >= 0 && payload.slot() < form.abilities().size()) {
               data.setSelected(payload.slot());
            }
            break;
         case PRESS:
            Ability abilityx = AbilityRegistry.get(payload.ability());
            if (abilityx != null) {
               press(player, abilityx);
            }
            break;
         case RELEASE:
            Ability ability = AbilityRegistry.get(payload.ability());
            if (ability != null) {
               release(player, ability);
            }
      }
   }

   @Nullable
   public static FormAbility entryFor(PlayerFruitData data, Ability ability) {
      FruitForm form = data.form();
      return form == null ? null : form.find(ability.id());
   }

   public static CastResult press(ServerPlayer player, Ability ability) {
      PlayerFruitData data = DFAttachments.data(player);
      FormAbility entry = entryFor(data, ability);
      if (entry == null) {
         return data.hasFruit() ? CastResult.NOT_IN_FORM : CastResult.NO_FRUIT;
      } else if (ability.castType() == CastType.TOGGLE && data.isToggleActive(ability.id())) {
         setToggle(player, data, entry, false, true);
         return CastResult.TOGGLED;
      } else {
         if (ability.castType() == CastType.COMBO) {
            AbilityManager.ComboState combo = COMBOS.get(player.getUUID());
            long now = player.level().getGameTime();
            if (combo != null && combo.entry.ability() == ability && now - combo.lastStepTick <= (long)ability.comboWindowTicks()) {
               return continueCombo(player, data, combo);
            }
         }

         CastResult check = check(player, data, entry, 0.0F);
         if (check != CastResult.SUCCESS) {
            feedback(player, data, entry, check);
            return check;
         } else {
            return switch (ability.castType()) {
               case INSTANT -> cast(player, data, entry, 1.0F);
               case CHARGED -> {
                  CHARGING.put(player.getUUID(), new AbilityManager.ChargeState(entry, player.level().getGameTime()));
                  setChargeSlow(player, true);
                  AbilityContext ctx = new AbilityContext(entry, player, data, 0.0F);
                  ability.onChargeStart(ctx);
                  ServerFx.chargeStart(player, ability, (float)ability.maxChargeTicks(), ctx.seed());
                  yield CastResult.CHARGING;
               }
               case HELD -> {
                  pay(player, data, energyCost(entry, 1.0F));
                  data.setLastCombatTick(player.level().getGameTime());
                  AbilityContext ctx = new AbilityContext(entry, player, data, 1.0F);
                  CHANNELLING.put(player.getUUID(), new AbilityManager.ChannelState(entry, player.level().getGameTime(), ctx.seed()));
                  ability.onChannelStart(ctx);
                  ServerFx.chargeStart(player, ability, (float)ability.maxChannelTicks(), ctx.seed());
                  CoreEvents.syncNow(player);
                  yield CastResult.CHANNELLING;
               }
               case TOGGLE -> {
                  pay(player, data, energyCost(entry, 1.0F));
                  setToggle(player, data, entry, true, true);
                  yield CastResult.TOGGLED;
               }
               case COMBO -> {
                  AbilityManager.ComboState combo = new AbilityManager.ComboState(entry);
                  COMBOS.put(player.getUUID(), combo);
                  yield comboStep(player, data, combo, 0);
               }
               case FORM_SWITCH -> ((FormSwitchAbility)ability).trigger(player) ? CastResult.SUCCESS : CastResult.REQUIREMENT;
            };
         }
      }
   }

   public static CastResult release(ServerPlayer player, Ability ability) {
      AbilityManager.ChannelState channel = CHANNELLING.get(player.getUUID());
      if (channel != null && channel.entry.ability() == ability) {
         if (player.level().getGameTime() - channel.startTick() < (long)ability.minChannelTicks()) {
            RELEASE_PENDING.add(player.getUUID());
         } else {
            endChannel(player, false);
         }

         return CastResult.SUCCESS;
      } else {
         AbilityManager.ChargeState state = CHARGING.get(player.getUUID());
         if (state != null && state.entry.ability() == ability) {
            CHARGING.remove(player.getUUID());
            setChargeSlow(player, false);
            PlayerFruitData data = DFAttachments.data(player);
            int ticks = (int)(player.level().getGameTime() - state.startTick);
            float charge = Mth.clamp((float)ticks / (float)ability.maxChargeTicks(), 0.0F, 1.0F);
            if (!freeCast(player, data) && data.energy() < energyCost(state.entry, charge)) {
               charge = affordableCharge(state.entry, data.energy(), charge);
            }

            CastResult result = check(player, data, state.entry, charge);
            if (result != CastResult.SUCCESS) {
               ability.onChargeCancel(player);
               ServerFx.chargeStop(player, ability, false);
               ServerFx.stopAnimation(player);
               feedback(player, data, state.entry, result);
               return result;
            } else {
               CastResult cast = cast(player, data, state.entry, charge);
               ServerFx.chargeStop(player, ability, true);
               return cast;
            }
         } else {
            return CastResult.BUSY;
         }
      }
   }

   public static CastResult check(ServerPlayer player, PlayerFruitData data, FormAbility entry, float charge) {
      Ability ability = entry.ability();
      if (!data.hasFruit() || player.isSpectator()) {
         return CastResult.NO_FRUIT;
      } else if (Weakness.isSuppressed(player)) {
         return CastResult.SUPPRESSED;
      } else if (!ability.config().enabled()) {
         return CastResult.DISABLED;
      } else if (data.mastery() < ability.config().requiredMastery()) {
         return CastResult.LOCKED;
      } else {
         long now = player.level().getGameTime();
         UUID id = player.getUUID();
         if (!isBusy(player, now)
            && (!CHARGING.containsKey(id) || CHARGING.get(id).entry.ability() == ability)
            && (!CHANNELLING.containsKey(id) || CHANNELLING.get(id).entry.ability() == ability)) {
            if (ability.castType() == CastType.FORM_SWITCH) {
               return CastResult.SUCCESS;
            } else if (data.isOnCooldown(ability.id(), now)) {
               return CastResult.COOLDOWN;
            } else {
               float cost = ability.castType() == CastType.COMBO ? ability.comboStepCost(0) * entry.modifiers().energyCost() : energyCost(entry, charge);
               if (!freeCast(player, data) && data.energy() < cost) {
                  return CastResult.NO_ENERGY;
               } else {
                  Component requirement = ability.checkRequirements(player, data);
                  if (requirement != null) {
                     player.displayClientMessage(requirement.copy().withStyle(ChatFormatting.RED), true);
                     return CastResult.REQUIREMENT;
                  } else {
                     return CastResult.SUCCESS;
                  }
               }
            }
         } else {
            return CastResult.BUSY;
         }
      }
   }

   private static CastResult cast(ServerPlayer player, PlayerFruitData data, FormAbility entry, float charge) {
      Ability ability = entry.ability();
      long now = player.level().getGameTime();
      pay(player, data, energyCost(entry, charge));
      data.startCooldown(ability.id(), now, cooldown(entry, data, charge));
      data.setLastCombatTick(now);
      AbilityContext ctx = new AbilityContext(entry, player, data, charge);
      if (ability.castType() == CastType.CHARGED) {
         ability.release(ctx);
      } else {
         ability.activate(ctx);
      }

      AutoStrike.afterCast(ctx);
      Mastery.addXp(player, data, ability.masteryXp(charge));
      CoreEvents.syncNow(player);
      return CastResult.SUCCESS;
   }

   public static float energyCost(FormAbility entry, float charge) {
      return entry.ability().energyCost(charge) * entry.modifiers().energyCost();
   }

   public static int cooldown(FormAbility entry, PlayerFruitData data, float charge) {
      return Math.round((float)entry.ability().cooldownTicks(charge) * entry.modifiers().cooldown() * Mastery.cooldownMultiplier(data.mastery()));
   }

   private static void pay(ServerPlayer player, PlayerFruitData data, float cost) {
      if (!freeCast(player, data)) {
         data.setEnergy(data.energy() - cost);
      }
   }

   private static float affordableCharge(FormAbility entry, float energy, float maxCharge) {
      float lo = 0.0F;
      float hi = maxCharge;

      for (int i = 0; i < 10; i++) {
         float mid = (lo + hi) * 0.5F;
         if (energyCost(entry, mid) <= energy) {
            lo = mid;
         } else {
            hi = mid;
         }
      }

      return lo;
   }

   public static void stopToggle(ServerPlayer player, Ability ability) {
      PlayerFruitData data = DFAttachments.data(player);
      if (data.isToggleActive(ability.id())) {
         FormAbility entry = entryFor(data, ability);
         setToggle(player, data, entry == null ? new FormAbility(ability) : entry, false, true);
      }
   }

   private static void setToggle(ServerPlayer player, PlayerFruitData data, FormAbility entry, boolean active, boolean cooldownOnOff) {
      Ability ability = entry.ability();
      if (data.isToggleActive(ability.id()) != active) {
         data.setToggle(ability.id(), active);
         ability.onToggle(new AbilityContext(entry, player, data, 1.0F), active);
         if (!active && cooldownOnOff) {
            data.startCooldown(ability.id(), player.level().getGameTime(), cooldown(entry, data, 1.0F));
         }

         CoreEvents.syncNow(player);
      }
   }

   private static CastResult continueCombo(ServerPlayer player, PlayerFruitData data, AbilityManager.ComboState combo) {
      if (Weakness.isSuppressed(player)) {
         finishCombo(player, data, combo);
         feedback(player, data, combo.entry, CastResult.SUPPRESSED);
         return CastResult.SUPPRESSED;
      } else {
         int next = combo.step + 1;
         float cost = combo.entry.ability().comboStepCost(next) * combo.entry.modifiers().energyCost();
         if (!freeCast(player, data) && data.energy() < cost) {
            finishCombo(player, data, combo);
            feedback(player, data, combo.entry, CastResult.NO_ENERGY);
            return CastResult.NO_ENERGY;
         } else {
            return comboStep(player, data, combo, next);
         }
      }
   }

   private static CastResult comboStep(ServerPlayer player, PlayerFruitData data, AbilityManager.ComboState combo, int step) {
      Ability ability = combo.entry.ability();
      long now = player.level().getGameTime();
      pay(player, data, ability.comboStepCost(step) * combo.entry.modifiers().energyCost());
      data.setLastCombatTick(now);
      combo.step = step;
      combo.lastStepTick = now;
      ability.comboStep(new AbilityContext(combo.entry, player, data, 1.0F), step);
      Mastery.addXp(player, data, ability.masteryXp(1.0F) / (float)Math.max(1, ability.maxComboSteps()));
      if (step + 1 >= ability.maxComboSteps()) {
         finishCombo(player, data, combo);
      }

      CoreEvents.syncNow(player);
      return step + 1 >= ability.maxComboSteps() ? CastResult.SUCCESS : CastResult.COMBO_STEP;
   }

   private static void finishCombo(ServerPlayer player, PlayerFruitData data, AbilityManager.ComboState combo) {
      if (COMBOS.get(player.getUUID()) == combo) {
         COMBOS.remove(player.getUUID());
      }

      data.startCooldown(combo.entry.ability().id(), player.level().getGameTime(), cooldown(combo.entry, data, 1.0F));
   }

   private static void endChannel(ServerPlayer player, boolean interrupted) {
      AbilityManager.ChannelState state = CHANNELLING.remove(player.getUUID());
      RELEASE_PENDING.remove(player.getUUID());
      if (state != null) {
         PlayerFruitData data = DFAttachments.data(player);
         Ability ability = state.entry.ability();
         int ticks = (int)(player.level().getGameTime() - state.startTick);
         ability.onChannelEnd(new AbilityContext(state.entry, player, data, 1.0F), ticks, interrupted);
         data.startCooldown(ability.id(), player.level().getGameTime(), cooldown(state.entry, data, 1.0F));
         Mastery.addXp(player, data, ability.masteryXp(1.0F));
         ServerFx.chargeStop(player, ability, !interrupted);
         if (interrupted) {
            ServerFx.stopAnimation(player);
         }

         CoreEvents.syncNow(player);
      }
   }

   public static void tickPlayer(ServerPlayer player, PlayerFruitData data) {
      long now = player.level().getGameTime();
      UUID id = player.getUUID();
      boolean suppressed = Weakness.isSuppressed(player);
      AbilityManager.ChargeState charge = CHARGING.get(id);
      if (charge != null) {
         int ticks = (int)(now - charge.startTick);
         if (!suppressed && player.isAlive() && entryFor(data, charge.entry.ability()) != null) {
            if (ticks > charge.entry.ability().maxChargeTicks() + 60) {
               release(player, charge.entry.ability());
            } else {
               charge.entry.ability().onChargeTick(new AbilityContext(charge.entry, player, data, 0.0F), ticks);
            }
         } else {
            cancelCharge(player);
         }
      }

      AbilityManager.ChannelState channel = CHANNELLING.get(id);
      if (channel != null) {
         int ticks = (int)(now - channel.startTick);
         Ability ability = channel.entry.ability();
         if (!suppressed && player.isAlive() && entryFor(data, ability) != null) {
            if (ticks >= ability.maxChannelTicks() || !ability.channelTick(new AbilityContext(channel.entry, player, data, 1.0F), ticks)) {
               endChannel(player, false);
            } else if (ticks + 1 >= ability.minChannelTicks() && RELEASE_PENDING.contains(id)) {
               endChannel(player, false);
            }
         } else {
            endChannel(player, true);
         }
      }

      AbilityManager.ComboState combo = COMBOS.get(id);
      if (combo != null && now - combo.lastStepTick > (long)combo.entry.ability().comboWindowTicks()) {
         finishCombo(player, data, combo);
      }

      if (data.hasActiveToggles()) {
         for (ResourceLocation toggleId : List.copyOf(data.activeToggles())) {
            Ability ability = AbilityRegistry.get(toggleId);
            FormAbility entry = ability == null ? null : entryFor(data, ability);
            if (entry == null) {
               if (ability == null) {
                  data.setToggle(toggleId, false);
               } else {
                  setToggle(player, data, new FormAbility(ability), false, true);
               }
            } else if (!suppressed && (freeCast(player, data) || !(data.energy() <= 0.01F)) && ability.config().enabled()) {
               ability.toggleTick(new AbilityContext(entry, player, data, 1.0F));
            } else {
               setToggle(player, data, entry, false, true);
            }
         }
      }

      if (now % 100L == 0L) {
         data.pruneCooldowns(now);
      }
   }

   public static void cancelCharge(ServerPlayer player) {
      AbilityManager.ChargeState state = CHARGING.remove(player.getUUID());
      if (state != null) {
         setChargeSlow(player, false);
         state.entry.ability().onChargeCancel(player);
         ServerFx.chargeStop(player, state.entry.ability(), false);
         ServerFx.stopAnimation(player);
      }
   }

   public static void cancelAll(ServerPlayer player) {
      cancelCharge(player);
      endChannel(player, true);
      COMBOS.remove(player.getUUID());
      BUSY_UNTIL.remove(player.getUUID());
      PlayerFruitData data = DFAttachments.data(player);

      for (ResourceLocation toggleId : List.copyOf(data.activeToggles())) {
         Ability ability = AbilityRegistry.get(toggleId);
         data.setToggle(toggleId, false);
         if (ability != null) {
            FormAbility entry = entryFor(data, ability);
            ability.onToggle(new AbilityContext(entry != null ? entry : new FormAbility(ability), player, data, 1.0F), false);
         }
      }
   }

   public static void cancelNotIn(ServerPlayer player, FruitForm target) {
      UUID id = player.getUUID();
      AbilityManager.ChargeState charge = CHARGING.get(id);
      if (charge != null && target.find(charge.entry.ability().id()) == null) {
         cancelCharge(player);
      }

      AbilityManager.ChannelState channel = CHANNELLING.get(id);
      if (channel != null && target.find(channel.entry.ability().id()) == null) {
         endChannel(player, true);
      }

      AbilityManager.ComboState combo = COMBOS.get(id);
      if (combo != null && target.find(combo.entry.ability().id()) == null) {
         COMBOS.remove(id);
      }

      PlayerFruitData data = DFAttachments.data(player);

      for (ResourceLocation toggleId : List.copyOf(data.activeToggles())) {
         if (target.find(toggleId) == null) {
            Ability ability = AbilityRegistry.get(toggleId);
            if (ability == null) {
               data.setToggle(toggleId, false);
            } else {
               FormAbility entry = entryFor(data, ability);
               setToggle(player, data, entry != null ? entry : new FormAbility(ability), false, true);
            }
         }
      }
   }

   public static void forget(UUID player) {
      CHARGING.remove(player);
      CHANNELLING.remove(player);
      COMBOS.remove(player);
      BUSY_UNTIL.remove(player);
   }

   public static boolean isCharging(Player player) {
      return CHARGING.containsKey(player.getUUID());
   }

   @Nullable
   public static Ability chargingAbility(Player player) {
      AbilityManager.ChargeState state = CHARGING.get(player.getUUID());
      return state == null ? null : state.entry.ability();
   }

   @Nullable
   public static Ability channellingAbility(Player player) {
      AbilityManager.ChannelState state = CHANNELLING.get(player.getUUID());
      return state == null ? null : state.entry.ability();
   }

   public static boolean isOccupied(ServerPlayer player) {
      UUID id = player.getUUID();
      return CHARGING.containsKey(id) || CHANNELLING.containsKey(id) || isBusy(player, player.level().getGameTime());
   }

   public static void setBusy(ServerPlayer player, int ticks) {
      BUSY_UNTIL.put(player.getUUID(), player.level().getGameTime() + (long)ticks);
   }

   public static void clearBusy(ServerPlayer player) {
      BUSY_UNTIL.remove(player.getUUID());
   }

   private static boolean isBusy(ServerPlayer player, long now) {
      Long until = BUSY_UNTIL.get(player.getUUID());
      return until != null && until > now;
   }

   public static boolean freeCast(Player player, PlayerFruitData data) {
      return freeEnergy(player) || data.form() != null && data.form().config().freeAbilities();
   }

   public static boolean freeEnergy(Player player) {
      return player.isCreative() && (Boolean)CoreConfig.CREATIVE_FREE_ENERGY.get();
   }

   private static void setChargeSlow(ServerPlayer player, boolean slow) {
      AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
      if (speed != null) {
         if (slow) {
            speed.addOrUpdateTransientModifier(new AttributeModifier(CHARGE_SLOW, -0.45, Operation.ADD_MULTIPLIED_TOTAL));
         } else {
            speed.removeModifier(CHARGE_SLOW);
         }
      }
   }

   private static void feedback(ServerPlayer player, PlayerFruitData data, FormAbility entry, CastResult result) {
      Ability ability = entry.ability();
      DevilFruit fruit = data.fruit();

      Component message = switch (result) {
         case COOLDOWN -> Component.translatable(
         result.messageKey,
         new Object[]{
            ability.displayName(), String.format(Locale.ROOT, "%.1f", (float)data.cooldownTicksLeft(ability.id(), player.level().getGameTime()) / 20.0F)
         }
      );
         case LOCKED -> Component.translatable(result.messageKey, new Object[]{ability.displayName(), ability.config().requiredMastery()});
         case NO_ENERGY -> fruit == null ? null : fruit.message("no_energy", Mth.ceil(energyCost(entry, 0.0F)));
         case DISABLED -> Component.translatable(result.messageKey, new Object[]{ability.displayName()});
         case SUPPRESSED -> Component.translatable(result.messageKey);
         default -> null;
      };
      if (message != null) {
         player.displayClientMessage(message.copy().withStyle(result == CastResult.SUPPRESSED ? ChatFormatting.BLUE : ChatFormatting.RED), true);
         if (result == CastResult.SUPPRESSED) {
            ServerFx.send(player, CoreFx.SUPPRESSED, player.position(), Vec3.ZERO, 1.0F, 0L, 0);
         }
      }
   }

   public static List<String> describe(ServerPlayer player) {
      List<String> out = new ArrayList<>();
      UUID id = player.getUUID();
      if (CHARGING.containsKey(id)) {
         out.add("charging " + CHARGING.get(id).entry.ability().id());
      }

      if (CHANNELLING.containsKey(id)) {
         out.add("channelling " + CHANNELLING.get(id).entry.ability().id());
      }

      if (COMBOS.containsKey(id)) {
         out.add("combo " + COMBOS.get(id).entry.ability().id() + " step " + COMBOS.get(id).step);
      }

      return out;
   }

   private AbilityManager() {
   }

   private static record ChannelState(FormAbility entry, long startTick, long seed) {
   }

   private static record ChargeState(FormAbility entry, long startTick) {
   }

   private static final class ComboState {
      final FormAbility entry;
      int step;
      long lastStepTick;

      ComboState(FormAbility entry) {
         this.entry = entry;
      }
   }
}
