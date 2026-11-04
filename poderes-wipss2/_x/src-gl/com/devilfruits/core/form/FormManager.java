package com.devilfruits.core.form;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.CoreEvents;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.config.CoreConfig;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.FruitRegistry;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.core.rules.Weakness;
import com.devilfruits.registry.DFAttachments;
import com.devilfruits.registry.DFTriggers;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;

public final class FormManager {
   private static final ResourceLocation FLIGHT_ID = DevilFruits.id("form/flight");
   private static final ResourceLocation WINDUP_SLOW = DevilFruits.id("form_windup_slow");
   private static final Map<UUID, FormManager.Windup> WINDUPS = new HashMap<>();

   public static FormManager.Result request(ServerPlayer player, String formName, FormManager.Reason reason) {
      PlayerFruitData data = DFAttachments.data(player);
      DevilFruit fruit = data.fruit();
      if (fruit == null) {
         return FormManager.Result.NO_FRUIT;
      } else {
         FruitForm target = formName.isEmpty() ? fruit.baseForm() : fruit.form(formName);
         if (target == null) {
            return FormManager.Result.UNKNOWN_FORM;
         } else {
            if (WINDUPS.containsKey(player.getUUID())) {
               if (reason == FormManager.Reason.PLAYER && !target.isBase()) {
                  return FormManager.Result.BUSY;
               }

               cancelWindup(player);
               if (reason == FormManager.Reason.PLAYER) {
                  return FormManager.Result.CANCELLED;
               }
            }

            FruitForm current = fruit.formOrBase(data.formName());
            if (current == target) {
               return FormManager.Result.ALREADY;
            } else {
               if (reason == FormManager.Reason.PLAYER && !target.isBase()) {
                  Component[] conditionMessage = new Component[1];
                  FormManager.Result refusal = validate(player, data, target, conditionMessage);
                  if (refusal != null) {
                     feedback(player, data, target, refusal, conditionMessage[0]);
                     return refusal;
                  }

                  int castTicks = target.config().castTicks();
                  if (castTicks > 0) {
                     startWindup(player, target, castTicks);
                     return FormManager.Result.WINDUP;
                  }
               }

               apply(player, data, fruit, current, target, reason);
               return FormManager.Result.SWITCHED;
            }
         }
      }
   }

   public static FormManager.Result revert(ServerPlayer player, FormManager.Reason reason) {
      return request(player, "", reason);
   }

   public static FormManager.Result cycle(ServerPlayer player) {
      PlayerFruitData data = DFAttachments.data(player);
      DevilFruit fruit = data.fruit();
      if (fruit == null) {
         return FormManager.Result.NO_FRUIT;
      } else if (!fruit.hasForms()) {
         return FormManager.Result.ALREADY;
      } else {
         int index = fruit.formOrBase(data.formName()).index();
         FruitForm next = fruit.forms().get((index + 1) % fruit.forms().size());
         return request(player, next.isBase() ? "" : next.name(), FormManager.Reason.PLAYER);
      }
   }

   public static FormManager.Result check(ServerPlayer player, String formName) {
      PlayerFruitData data = DFAttachments.data(player);
      DevilFruit fruit = data.fruit();
      if (fruit == null) {
         return FormManager.Result.NO_FRUIT;
      } else {
         FruitForm target = formName.isEmpty() ? fruit.baseForm() : fruit.form(formName);
         if (target == null) {
            return FormManager.Result.UNKNOWN_FORM;
         } else if (WINDUPS.containsKey(player.getUUID())) {
            return FormManager.Result.BUSY;
         } else if (fruit.formOrBase(data.formName()) == target) {
            return FormManager.Result.ALREADY;
         } else if (target.isBase()) {
            return FormManager.Result.SWITCHED;
         } else {
            FormManager.Result refusal = validate(player, data, target, new Component[1]);
            return refusal == null ? FormManager.Result.SWITCHED : refusal;
         }
      }
   }

   @Nullable
   private static FormManager.Result validate(ServerPlayer player, PlayerFruitData data, FruitForm target, Component[] conditionMessage) {
      long now = player.level().getGameTime();
      if (Weakness.isSuppressed(player)) {
         return FormManager.Result.SUPPRESSED;
      } else if (data.mastery() < target.config().requiredMastery()) {
         return FormManager.Result.LOCKED;
      } else if (AbilityManager.isOccupied(player)) {
         return FormManager.Result.BUSY;
      } else if (data.isOnCooldown(PlayerFruitData.FORM_COOLDOWN, now)) {
         return FormManager.Result.COOLDOWN;
      } else if (!target.allowedIn(player.level().dimension())) {
         return FormManager.Result.DIMENSION;
      } else if (target.condition() != null && (conditionMessage[0] = target.condition().check(player, data)) != null) {
         return FormManager.Result.CONDITION;
      } else {
         return !AbilityManager.freeEnergy(player) && data.energy() < (float)target.config().enterEnergyCost() ? FormManager.Result.NO_ENERGY : null;
      }
   }

   private static void apply(ServerPlayer player, PlayerFruitData data, DevilFruit fruit, FruitForm current, FruitForm target, FormManager.Reason reason) {
      long now = player.level().getGameTime();
      AbilityManager.cancelNotIn(player, target);
      if (reason == FormManager.Reason.PLAYER && !target.isBase() && !AbilityManager.freeEnergy(player)) {
         data.setEnergy(data.energy() - (float)target.config().enterEnergyCost());
      }

      boolean airborne = !player.onGround() && !player.isInWater() && !player.isPassenger();
      applyAttributes(player, target);
      data.setForm(target.isBase() ? "" : target.name());
      data.setFlags(0);
      player.setForcedPose(target.forcedPose());
      if (current.canFly() && !target.canFly() && airborne && reason != FormManager.Reason.DEATH) {
         SafeLanding.grant(player);
      }

      if (data.selected() >= target.abilities().size()) {
         data.setSelected(Math.max(0, target.abilities().size() - 1));
      }

      if (reason == FormManager.Reason.PLAYER) {
         FruitForm timed = target.isBase() ? current : target;
         data.startCooldown(PlayerFruitData.FORM_COOLDOWN, now, timed.config().switchCooldownTicks());
      }

      player.refreshDimensions();
      ResourceLocation fx = target.isBase() ? current.exitFx() : target.enterFx();
      if (fx == null) {
         fx = fruit.fx().formChange();
      }

      if (reason != FormManager.Reason.LOGIN && reason != FormManager.Reason.DEATH && reason != FormManager.Reason.POWER_LOST) {
         ServerFx.send(player, fx, player.position(), Vec3.ZERO, target.isBase() ? 0.0F : 1.0F, player.getRandom().nextLong(), target.index());
         ResourceLocation animation = target.isBase() ? current.exitAnimation() : target.enterAnimation();
         if (animation != null) {
            ServerFx.animate(player, animation);
         }
      }

      if (reason == FormManager.Reason.WATER
         || reason == FormManager.Reason.DEPLETED
         || reason == FormManager.Reason.DIMENSION
         || reason == FormManager.Reason.CONDITION) {
         player.displayClientMessage(
            Component.translatable("message.devilfruits.form.reverted." + reason.name().toLowerCase(Locale.ROOT), new Object[]{current.displayName()})
               .withStyle(ChatFormatting.GRAY),
            true
         );
      }

      if (!target.isBase()) {
         DFTriggers.CHANGED_FORM.get().trigger(player, fruit.id(), target.name());
      }

      ServerFx.formState(player, reason == FormManager.Reason.DEATH ? 128 : 0);
      CoreEvents.syncNow(player);
   }

   private static void startWindup(ServerPlayer player, FruitForm target, int castTicks) {
      WINDUPS.put(player.getUUID(), new FormManager.Windup(target, player.level().getGameTime() + (long)castTicks));
      AbilityManager.setBusy(player, castTicks + 1);
      AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
      if (speed != null) {
         speed.addOrUpdateTransientModifier(new AttributeModifier(WINDUP_SLOW, -0.6, Operation.ADD_MULTIPLIED_TOTAL));
      }

      ServerFx.chargeStart(player, target.id(), (float)castTicks, player.getRandom().nextLong());
      if (target.windupAnimation() != null) {
         ServerFx.animate(player, target.windupAnimation());
      }
   }

   @Nullable
   public static FruitForm windupTarget(ServerPlayer player) {
      FormManager.Windup windup = WINDUPS.get(player.getUUID());
      return windup == null ? null : windup.target;
   }

   public static void cancelWindup(ServerPlayer player) {
      FormManager.Windup windup = endWindup(player);
      if (windup != null) {
         ServerFx.chargeStop(player, windup.target.id(), false);
         if (windup.target.windupAnimation() != null) {
            ServerFx.stopAnimation(player);
         }
      }
   }

   @Nullable
   private static FormManager.Windup endWindup(ServerPlayer player) {
      FormManager.Windup windup = WINDUPS.remove(player.getUUID());
      if (windup != null) {
         AbilityManager.clearBusy(player);
         AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
         if (speed != null) {
            speed.removeModifier(WINDUP_SLOW);
         }
      }

      return windup;
   }

   private static void tickWindup(ServerPlayer player, PlayerFruitData data) {
      FormManager.Windup windup = WINDUPS.get(player.getUUID());
      if (windup != null) {
         DevilFruit fruit = data.fruit();
         if (fruit == null || windup.target.fruit() != fruit || !player.isAlive() || Weakness.isSuppressed(player)) {
            cancelWindup(player);
         } else if (player.level().getGameTime() >= windup.endTick) {
            endWindup(player);
            Component[] conditionMessage = new Component[1];
            FormManager.Result refusal = validate(player, data, windup.target, conditionMessage);
            if (refusal != null) {
               ServerFx.chargeStop(player, windup.target.id(), false);
               feedback(player, data, windup.target, refusal, conditionMessage[0]);
            } else {
               ServerFx.chargeStop(player, windup.target.id(), true);
               apply(player, data, fruit, fruit.formOrBase(data.formName()), windup.target, FormManager.Reason.PLAYER);
            }
         }
      }
   }

   public static void forget(ServerPlayer player) {
      endWindup(player);
      SafeLanding.forget(player.getUUID());
   }

   public static void setBoosting(ServerPlayer player, boolean boosting) {
      FruitForm form = DFAttachments.data(player).form();
      FormFlags.set(player, 1, boosting && form != null && form.boostable() && !Weakness.isSuppressed(player));
   }

   public static void tick(ServerPlayer player, PlayerFruitData data) {
      tickWindup(player, data);
      SafeLanding.tick(player);
      FruitForm form = data.form();
      if (form != null && !form.isBase()) {
         if (data.suppressed() && (Boolean)CoreConfig.REVERT_IN_WATER.get()) {
            revert(player, FormManager.Reason.WATER);
         } else if (!form.allowedIn(player.level().dimension())) {
            revert(player, FormManager.Reason.DIMENSION);
         } else if (form.condition() != null && player.tickCount % 20 == 0 && form.condition().check(player, data) != null) {
            revert(player, FormManager.Reason.CONDITION);
         } else {
            double drain = drainPerSecond(form, data);
            if (drain > 0.0 && (form.config().timeLimitSeconds() > 0 || !AbilityManager.freeEnergy(player))) {
               data.setEnergy((float)((double)data.energy() - drain / 20.0));
               if (data.energy() <= 0.0F && (Boolean)CoreConfig.REVERT_ON_DEPLETION.get()) {
                  revert(player, FormManager.Reason.DEPLETED);
               }
            }
         }
      }
   }

   public static boolean isDraining(PlayerFruitData data) {
      FruitForm form = data.form();
      return form != null && !form.isBase() ? drainPerSecond(form, data) > 0.0 : false;
   }

   public static double drainPerSecond(FruitForm form, PlayerFruitData data) {
      int limit = form.config().timeLimitSeconds();
      if (limit > 0) {
         return (double)Mastery.maxEnergy(data) / (double)limit;
      } else {
         double drain = form.config().energyDrainPerSecond();
         if (data.hasFlag(1)) {
            drain += form.config().boostDrainPerSecond();
         }

         return drain;
      }
   }

   public static void applyAttributes(ServerPlayer player, @Nullable FruitForm form) {
      float health = player.getHealth();
      removeModifiers(player);
      if (form == null) {
         capHealth(player, health);
      } else {
         for (FormAttribute attribute : form.attributes()) {
            AttributeInstance instance = player.getAttribute(attribute.attribute());
            if (instance != null) {
               double amount = form.config().attribute(attribute.attribute(), attribute.amount());
               instance.addOrUpdateTransientModifier(new AttributeModifier(modifierId(attribute.attribute()), amount, attribute.operation()));
            }
         }

         if (form.canFly()) {
            AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
            if (flight != null) {
               flight.addOrUpdateTransientModifier(new AttributeModifier(FLIGHT_ID, 1.0, Operation.ADD_VALUE));
            }
         }

         capHealth(player, health);
      }
   }

   private static void capHealth(ServerPlayer player, float health) {
      float target = Math.min(health, player.getMaxHealth());
      if (player.getHealth() != target) {
         player.setHealth(target);
      }
   }

   public static void clearAttributes(ServerPlayer player) {
      float health = player.getHealth();
      removeModifiers(player);
      capHealth(player, health);
   }

   private static void removeModifiers(ServerPlayer player) {
      for (DevilFruit fruit : FruitRegistry.all()) {
         for (FruitForm form : fruit.forms()) {
            for (FormAttribute attribute : form.attributes()) {
               AttributeInstance instance = player.getAttribute(attribute.attribute());
               if (instance != null) {
                  instance.removeModifier(modifierId(attribute.attribute()));
               }
            }
         }
      }

      AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
      if (flight != null) {
         flight.removeModifier(FLIGHT_ID);
      }
   }

   public static void reapply(ServerPlayer player) {
      PlayerFruitData data = DFAttachments.data(player);
      DevilFruit fruit = data.fruit();
      FruitForm shown = null;
      if (fruit == null) {
         clearAttributes(player);
      } else {
         shown = fruit.formOrBase(data.formName());
         if (!shown.name().equals(data.formName()) && !data.formName().isEmpty()) {
            data.setForm("");
         }

         applyAttributes(player, shown);
      }

      data.setFlags(0);
      player.setForcedPose(shown == null ? null : shown.forcedPose());
      player.refreshDimensions();
      ServerFx.formState(player);
   }

   public static ResourceLocation modifierId(Holder<Attribute> attribute) {
      ResourceLocation key = attribute.unwrapKey().map(k -> k.location()).orElseThrow(() -> new IllegalStateException("Unregistered attribute"));
      return DevilFruits.id("form/" + key.getNamespace() + "/" + key.getPath());
   }

   private static void feedback(ServerPlayer player, PlayerFruitData data, FruitForm target, FormManager.Result result, @Nullable Component condition) {
      Component message = (Component)(switch (result) {
         case LOCKED -> Component.translatable("message.devilfruits.form.locked", new Object[]{target.displayName(), target.config().requiredMastery()});
         case COOLDOWN -> Component.translatable(
         "message.devilfruits.form.cooldown",
         new Object[]{String.format(Locale.ROOT, "%.1f", (float)data.cooldownTicksLeft(PlayerFruitData.FORM_COOLDOWN, player.level().getGameTime()) / 20.0F)}
      );
         case NO_ENERGY -> target.fruit().message("no_energy", target.config().enterEnergyCost());
         case SUPPRESSED -> Component.translatable("message.devilfruits.suppressed");
         default -> null;
         case DIMENSION -> Component.translatable("message.devilfruits.form.dimension", new Object[]{target.displayName()});
         case CONDITION -> condition;
      });
      if (message != null) {
         player.displayClientMessage(message.copy().withStyle(result == FormManager.Result.SUPPRESSED ? ChatFormatting.BLUE : ChatFormatting.RED), true);
      }
   }

   private FormManager() {
   }

   public static enum Reason {
      PLAYER,
      COMMAND,
      DEATH,
      WATER,
      DEPLETED,
      DIMENSION,
      CONDITION,
      POWER_LOST,
      LOGIN;
   }

   public static enum Result {
      SWITCHED,
      ALREADY,
      NO_FRUIT,
      UNKNOWN_FORM,
      LOCKED,
      COOLDOWN,
      NO_ENERGY,
      SUPPRESSED,
      BUSY,
      DIMENSION,
      CONDITION,
      WINDUP,
      CANCELLED;
   }

   private static record Windup(FruitForm target, long endTick) {
   }
}
