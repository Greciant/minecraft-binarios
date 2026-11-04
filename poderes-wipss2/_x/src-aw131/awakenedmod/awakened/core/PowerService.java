package com.awakenedmod.awakened.core;

import com.awakenedmod.awakened.Awakened;
import com.awakenedmod.awakened.core.awaken.AwakeningCinematic;
import com.awakenedmod.awakened.core.awaken.RerollOffers;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.data.ProgressData;
import com.awakenedmod.awakened.core.net.AbilityFeedbackPayload;
import com.awakenedmod.awakened.core.net.CatalystSpinPayload;
import com.awakenedmod.awakened.core.net.PlayAnimationPayload;
import com.awakenedmod.awakened.core.net.StopAnimationPayload;
import com.awakenedmod.awakened.core.net.SyncPowerDataPayload;
import com.awakenedmod.awakened.core.net.SyncProgressPayload;
import com.awakenedmod.awakened.core.net.SyncRelicPayload;
import com.awakenedmod.awakened.core.net.SyncSkillPayload;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.power.AbilityFeedback;
import com.awakenedmod.awakened.core.power.PassiveToggleService;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import com.awakenedmod.awakened.core.power.PowerResource;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import com.awakenedmod.awakened.core.quest.QuestTrigger;
import com.awakenedmod.awakened.core.skill.SkillSystem;
import com.awakenedmod.awakened.core.stamina.StaminaResource;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PowerService {
   private static final int SECONDARY_KEY_MIN_TICKS = 2;
   private static final Map<UUID, Long> LAST_SECONDARY_KEY = new ConcurrentHashMap<>();
   private static final Map<UUID, String> HELD_CLIPS = new ConcurrentHashMap<>();

   private PowerService() {
   }

   public static PowerData data(ServerPlayer player) {
      return AwakenedAttachments.get(player);
   }

   public static void sync(ServerPlayer player) {
      PowerData data = data(player);
      data.markStaminaSynced();
      Power power = PowerRegistry.get(data.powerId());
      float resourceFraction = 0.0F;
      int resourceTier = 0;
      float resourceMark = 0.0F;
      int resourceCooldownSeconds = 0;
      if (power != null && power.hasResourceBar()) {
         resourceFraction = power.resourceBarFraction(player, data);
         resourceTier = power.resourceBarTier(player, data);
         resourceMark = power.resourceBarMark(player, data);
         resourceCooldownSeconds = power.resourceBarCooldownSeconds(player, data);
      }

      float staminaBonus = StaminaSystem.powerMaxBonus(player);
      float abilityPower = MasteryScaling.abilityPower(player);
      float staminaLocked = StaminaSystem.locked(player, data, power);
      data.markLockedSynced(staminaLocked);
      int slots = power != null ? power.abilityCount() : 0;
      int[] cooldownLeft = new int[slots];
      int[] cooldownLength = new int[slots];

      for (int slot = 0; slot < slots; slot++) {
         Ability ability = power.ability(slot);
         int left = ability != null ? ability.cooldownRemainingTicks(player) : 0;
         if (left > 0) {
            cooldownLeft[slot] = left;
            cooldownLength[slot] = Math.max(left, ability.cooldownLengthTicks(player));
         }
      }

      PacketDistributor.sendToPlayer(
         player,
         SyncPowerDataPayload.of(
            data,
            StaminaSystem.max(player),
            resourceFraction,
            resourceTier,
            resourceMark,
            staminaBonus,
            abilityPower,
            resourceCooldownSeconds,
            staminaLocked,
            cooldownLeft,
            cooldownLength
         ),
         new CustomPacketPayload[0]
      );
      syncProgress(player);
   }

   public static void syncSkill(ServerPlayer player) {
      PacketDistributor.sendToPlayer(player, SyncSkillPayload.of(AwakenedAttachments.skill(player)), new CustomPacketPayload[0]);
   }

   public static void syncRelics(ServerPlayer player) {
      PacketDistributor.sendToPlayer(player, SyncRelicPayload.of(AwakenedAttachments.relics(player)), new CustomPacketPayload[0]);
   }

   public static void syncProgress(ServerPlayer player) {
      ProgressData progress = AwakenedAttachments.progress(player);
      progress.recordMastery(data(player).masteryLevel());
      PacketDistributor.sendToPlayer(player, SyncProgressPayload.of(progress), new CustomPacketPayload[0]);
   }

   public static void awaken(ServerPlayer player) {
      if (!data(player).awakened() && !PowerRegistry.rollable().isEmpty()) {
         ProgressData progress = AwakenedAttachments.progress(player);
         progress.awakened();
         Power power = rollPower(player, List.of());
         applyPower(player, power);
         AwakeningCinematic.begin(player, power);
         if (progress.timesAwakened() == 1) {
            SkillSystem.award(player, 50.0);
         }

         AwakenedAdvancements.grant(player, "awakened/root");
         Awakened.LOGGER.info("Player {} awakened power '{}'", player.getName().getString(), power.id());
      }
   }

   public static void reroll(ServerPlayer player) {
      reroll(player, false);
   }

   public static void reroll(ServerPlayer player, boolean spinner) {
      reroll(player, spinner, List.of());
   }

   public static void reroll(ServerPlayer player, boolean spinner, List<Power> pool) {
      if (!pool.isEmpty() || !PowerRegistry.rollable().isEmpty()) {
         settlePending(player);
         Power power = rollPower(player, pool);
         List<String> poolIds = pool.stream().map(Power::id).toList();
         PowerData data = data(player);
         if (spinner && data.hasPower()) {
            RerollOffers.put(player, power);
            PacketDistributor.sendToPlayer(
               player, new CatalystSpinPayload(power.id(), player.getRandom().nextLong(), poolIds, data.powerId()), new CustomPacketPayload[0]
            );
         } else if (spinner) {
            PacketDistributor.sendToPlayer(player, new CatalystSpinPayload(power.id(), player.getRandom().nextLong(), poolIds, ""), new CustomPacketPayload[0]);
            AwakeningCinematic.beginDelayed(player, power, 92, revealed -> commitReroll(revealed, power));
         } else {
            commitReroll(player, power);
            AwakeningCinematic.begin(player, power);
         }

         AwakenedAdvancements.grant(player, "awakened/reroll");
         Awakened.LOGGER.info("Player {} rerolled into power '{}'", player.getName().getString(), power.id());
      }
   }

   public static void answerOffer(ServerPlayer player, boolean take) {
      if (player != null) {
         Power offered = RerollOffers.take(player);
         if (offered != null) {
            if (take && !PowerRegistry.obtainable(offered)) {
               player.displayClientMessage(Component.translatable("awakened.rules.offer_disabled"), true);
            } else if (take) {
               commitReroll(player, offered);
               AwakeningCinematic.begin(player, offered);
               Awakened.LOGGER.info("Player {} took rolled power '{}'", player.getName().getString(), offered.id());
            } else {
               Power kept = PowerRegistry.get(data(player).powerId());
               if (kept != null) {
                  player.displayClientMessage(Component.translatable("awakened.spinner.kept", new Object[]{kept.displayName()}), true);
               }
            }
         }
      }
   }

   public static void tradePowers(ServerPlayer first, ServerPlayer second) {
      settlePending(first);
      settlePending(second);
      Power firstPower = PowerRegistry.get(data(first).powerId());
      Power secondPower = PowerRegistry.get(data(second).powerId());
      if (firstPower != null && secondPower != null && firstPower != secondPower) {
         commitReroll(first, secondPower);
         commitReroll(second, firstPower);
         AwakeningCinematic.begin(first, secondPower);
         AwakeningCinematic.begin(second, firstPower);
         Awakened.LOGGER
            .info(
               "Players {} and {} traded powers '{}' and '{}'",
               new Object[]{first.getName().getString(), second.getName().getString(), firstPower.id(), secondPower.id()}
            );
      }
   }

   public static boolean powerChangePending(ServerPlayer player) {
      return AwakeningCinematic.pendingPower(player.getUUID()) != null || RerollOffers.has(player);
   }

   private static void settlePending(ServerPlayer player) {
      AwakeningCinematic.flushPending(player);
      RerollOffers.drop(player);
   }

   private static Power rollPower(ServerPlayer player, List<Power> pool) {
      if (!pool.isEmpty()) {
         return pool.get(player.serverLevel().getRandom().nextInt(pool.size()));
      } else {
         List<Power> candidates = PowerRegistry.rollable();
         if (candidates.isEmpty()) {
            throw new IllegalStateException("No obtainable powers to roll from");
         } else {
            String current = data(player).powerId();
            List<Power> changed = candidates.stream().filter(p -> !p.id().equals(current)).toList();
            if (!changed.isEmpty()) {
               candidates = changed;
            }

            List<Power> unclaimed = unclaimed(player, candidates);
            List<Power> pick = unclaimed.isEmpty() ? candidates : unclaimed;
            return pick.get(player.serverLevel().getRandom().nextInt(pick.size()));
         }
      }
   }

   public static List<Power> unclaimed(ServerPlayer player, List<Power> candidates) {
      if (player == null) {
         return candidates;
      } else {
         MinecraftServer server = player.serverLevel().getServer();
         if (!UniquePowerRoll.enabled(server)) {
            return candidates;
         } else {
            Map<String, Integer> holders = holderCounts(server, player.getUUID());
            return candidates.stream().filter(p -> !PowerLimits.atCapacity(p.id(), holders.getOrDefault(p.id(), 0))).toList();
         }
      }
   }

   public static Map<String, Integer> holderCounts(MinecraftServer server, UUID exclude) {
      Map<String, Integer> counts = new HashMap<>();
      if (server == null) {
         return counts;
      } else {
         Set<UUID> online = new HashSet<>();

         for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            online.add(other.getUUID());
            if (!other.getUUID().equals(exclude)) {
               PowerData otherData = data(other);
               String held = otherData.awakened() ? otherData.powerId() : null;
               if (held != null) {
                  counts.merge(held, 1, Integer::sum);
               }

               Power pending = AwakeningCinematic.pendingPower(other.getUUID());
               if (pending != null && !pending.id().equals(held)) {
                  counts.merge(pending.id(), 1, Integer::sum);
               }

               Power offered = RerollOffers.pending(other.getUUID());
               if (offered != null && !offered.id().equals(held) && (pending == null || !pending.id().equals(offered.id()))) {
                  counts.merge(offered.id(), 1, Integer::sum);
               }
            }
         }

         PowerOwnershipRegistry.all(server, PowerOwnershipRegistry.staleAfterMillis()).forEach((uuid, powerId) -> {
            if (!online.contains(uuid) && !uuid.equals(exclude)) {
               counts.merge(powerId, 1, Integer::sum);
            }
         });
         return counts;
      }
   }

   private static void commitReroll(ServerPlayer player, Power power) {
      QuestSystem.resetForPowerChange(player);
      applyPower(player, power);
      SkillSystem.respec(player);
   }

   private static void applyPower(ServerPlayer player, Power power) {
      PowerData data = data(player);
      clearPreviousPower(player);
      data.assignPower(power.id(), CatalystConfig.bankProgress());
      data.setStamina(StaminaSystem.max(player));
      data.setLastSpendTime(player.serverLevel().getGameTime());
      PowerOwnershipRegistry.set(player.serverLevel().getServer(), player.getUUID(), power.id());
      sync(player);
      QuestSystem.sync(player);
      PassiveToggleService.sync(player);
   }

   private static void clearPreviousPower(ServerPlayer player) {
      Power previous = PowerRegistry.get(data(player).powerId());
      PowerSuppression.release(player, previous);
      if (previous != null) {
         previous.clearAbilityState(player);
         previous.clearCooldowns(player);
         previous.onReplaced(player);
      }
   }

   public static void tearDownPower(ServerPlayer player) {
      clearPreviousPower(player);
   }

   public static void setPower(ServerPlayer player, String powerId) {
      settlePending(player);
      clearPreviousPower(player);
      PowerData data = data(player);
      data.assignPower(powerId, CatalystConfig.bankProgress());
      data.setStamina(StaminaSystem.max(player));
      PowerOwnershipRegistry.set(player.serverLevel().getServer(), player.getUUID(), powerId);
      PassiveToggleService.sync(player);
   }

   public static boolean expireIfStale(ServerPlayer player) {
      MinecraftServer server = player.serverLevel().getServer();
      if (data(player).awakened() && UniquePowerRoll.enabled(server) && PowerOwnershipRegistry.isStale(server, player.getUUID())) {
         clearPower(player);
         QuestSystem.resetForPowerChange(player);
         return true;
      } else {
         return false;
      }
   }

   public static void clearPower(ServerPlayer player) {
      settlePending(player);
      clearPreviousPower(player);
      data(player).clearPower();
      PowerOwnershipRegistry.clear(player.serverLevel().getServer(), player.getUUID());
   }

   public static void activate(ServerPlayer player, int slot) {
      if (player != null && !player.isDeadOrDying()) {
         PowerData data = data(player);
         if (data.awakened()) {
            if (player.isSpectator()) {
               AbilityFeedback.fail(player, "awakened.feedback.spectator");
            } else {
               Power power = PowerRegistry.get(data.powerId());
               if (power != null) {
                  Ability ability = power.ability(slot);
                  if (ability != null) {
                     if (!ability.isUnlocked(data)) {
                        feedback(player, 1);
                     } else if (!refusedBySeal(player, ability)) {
                        int cooling = ability.cooldownRemainingTicks(player);
                        if (cooling > 0 && ability.refusesPressWhileCooling() && !ability.isActive(player)) {
                           AbilityFeedback.fail(player, "awakened.feedback.cooldown", (cooling + 19) / 20);
                        } else if (ability.requiresFootingWhenRealistic() && RealisticMovement.blocksAirLaunch(player)) {
                           AbilityFeedback.fail(player, "awakened.feedback.no_footing");
                        } else {
                           PowerResource resource = power.resource();
                           float cost = ability.effectiveCost(player);
                           if (!resource.canAfford(player, cost)) {
                              resource.refuse(player);
                           } else {
                              AbilityFeedback.beginActivation(player);
                              boolean happened = ability.activate(player);
                              if (happened) {
                                 resource.spend(player, cost);
                                 String anim = ability.activationAnimation(player);
                                 if (anim != null) {
                                    playAnimation(player, anim);
                                 }

                                 AwakenedAdvancements.grant(player, "awakened/use_ability");
                                 MasterySystem.awardUse(player);
                                 if (cost > 0.0F && slot == power.signatureSlot()) {
                                    QuestSystem.report(player, QuestTrigger.USE_SIGNATURE, 1);
                                 }

                                 sync(player);
                              } else if (!AbilityFeedback.consumeSpoken(player)) {
                                 feedback(player, 2);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static PowerResource resource(ServerPlayer player) {
      Power power = PowerRegistry.get(data(player).powerId());
      return (PowerResource)(power != null ? power.resource() : StaminaResource.INSTANCE);
   }

   public static boolean charge(ServerPlayer player, Ability ability, float cost) {
      PowerData data = data(player);
      Power power = PowerRegistry.get(data.powerId());
      PowerResource resource = resource(player);
      if (!resource.canAfford(player, cost)) {
         return false;
      } else {
         resource.sustain(player, cost);
         if (cost > 0.0F && power != null && power.ability(power.signatureSlot()) == ability) {
            QuestSystem.report(player, QuestTrigger.USE_SIGNATURE, 1);
         }

         sync(player);
         return true;
      }
   }

   public static void release(ServerPlayer player, int slot) {
      if (player != null) {
         PowerData data = data(player);
         if (data.awakened()) {
            Power power = PowerRegistry.get(data.powerId());
            if (power != null) {
               Ability ability = power.ability(slot);
               if (ability != null && ability.isUnlocked(data)) {
                  ability.onRelease(player);
               }
            }
         }
      }
   }

   public static void abilityAction(ServerPlayer player, int slot, int action) {
      if (player != null) {
         PowerData data = data(player);
         if (data.awakened()) {
            Power power = PowerRegistry.get(data.powerId());
            if (power != null) {
               Ability ability = power.ability(slot);
               if (ability != null && ability.isUnlocked(data) && !refusedBySeal(player, ability)) {
                  ability.onSecondaryAction(player, action);
               }
            }
         }
      }
   }

   public static boolean refusedBySuppression(ServerPlayer player) {
      if (PowerSuppression.test(player)) {
         feedback(player, 3);
         return true;
      } else {
         return false;
      }
   }

   public static boolean refusedBySeal(ServerPlayer player, Ability ability) {
      if (!AbilityGate.refusesPress(player) || ability.ignoresSealWhileActive() && ability.isActive(player)) {
         return false;
      } else {
         feedback(player, 3);
         return true;
      }
   }

   public static void secondaryKey(ServerPlayer player, boolean sneaking, boolean held) {
      if (player != null) {
         PowerData data = data(player);
         if (data.awakened()) {
            Power power = PowerRegistry.get(data.powerId());
            if (power != null && power.handlesSecondaryKey()) {
               long now = player.serverLevel().getGameTime();
               Long last = LAST_SECONDARY_KEY.get(player.getUUID());
               if (last == null || now - last >= 2L || now < last) {
                  LAST_SECONDARY_KEY.put(player.getUUID(), now);
                  if (AbilityGate.refusesPress(player)) {
                     feedback(player, 3);
                  } else {
                     power.onSecondaryKey(player, sneaking, held);
                  }
               }
            }
         }
      }
   }

   public static void forgetSecondaryKey(ServerPlayer player) {
      LAST_SECONDARY_KEY.remove(player.getUUID());
   }

   public static void setActiveSlot(ServerPlayer player, int slot) {
      if (player != null) {
         PowerData data = data(player);
         Power power = PowerRegistry.get(data.powerId());
         if (power != null) {
            Ability outgoing = power.ability(data.activeSlot());
            if (outgoing != null) {
               outgoing.onRelease(player);
            }

            int clamped = Math.floorMod(slot, power.abilityCount());
            data.setActiveSlot(clamped);
            sync(player);
         }
      }
   }

   public static void feedback(ServerPlayer player, int kind) {
      PacketDistributor.sendToPlayer(player, new AbilityFeedbackPayload(kind), new CustomPacketPayload[0]);
   }

   public static void announceUnlock(ServerPlayer player, Component abilityName) {
      announceUnlock(player, abilityName, PowerService.Unlock.ABILITY);
   }

   public static void announceUnlock(ServerPlayer player, Component line, PowerService.Unlock kind) {
      if (kind.grantsAbilityAdvancement) {
         AwakenedAdvancements.grant(player, "awakened/unlock_ability");
      }

      player.connection.send(new ClientboundSetTitlesAnimationPacket(5, 50, 15));
      player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable(kind.titleKey).withStyle(ChatFormatting.AQUA)));
      player.connection.send(new ClientboundSetSubtitleTextPacket(line));
      Fx.sound(player.serverLevel(), player.position(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8F, 1.0F);
      player.displayClientMessage(Component.translatable(kind.chatKey, new Object[]{line}).withStyle(ChatFormatting.GOLD), false);
   }

   public static void playAnimation(ServerPlayer player, String animation) {
      playAnimation(player, animation, false);
   }

   public static void playAnimation(ServerPlayer player, String animation, boolean held) {
      if (held) {
         HELD_CLIPS.put(player.getUUID(), animation);
      } else {
         HELD_CLIPS.remove(player.getUUID());
      }

      PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new PlayAnimationPayload(player.getId(), animation), new CustomPacketPayload[0]);
   }

   public static void stopAnimation(ServerPlayer player) {
      HELD_CLIPS.remove(player.getUUID());
      PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new StopAnimationPayload(player.getId()), new CustomPacketPayload[0]);
   }

   public static boolean stopHeldAnimation(ServerPlayer player, String clip) {
      if (!clip.equals(HELD_CLIPS.get(player.getUUID()))) {
         return false;
      } else {
         stopAnimation(player);
         return true;
      }
   }

   public static String heldAnimation(ServerPlayer player) {
      return HELD_CLIPS.get(player.getUUID());
   }

   public static void resendHeldAnimation(ServerPlayer viewer, ServerPlayer target) {
      String held = HELD_CLIPS.get(target.getUUID());
      if (held != null) {
         PacketDistributor.sendToPlayer(viewer, new PlayAnimationPayload(target.getId(), held), new CustomPacketPayload[0]);
      }
   }

   public static void forgetHeldAnimation(ServerPlayer player) {
      HELD_CLIPS.remove(player.getUUID());
   }

   public static enum Unlock {
      ABILITY("awakened.unlock.title", "awakened.unlock.chat", true),
      FEATURE("awakened.unlock.title_feature", "awakened.unlock.chat_feature", false);

      private final String titleKey;
      private final String chatKey;
      private final boolean grantsAbilityAdvancement;

      private Unlock(String titleKey, String chatKey, boolean grantsAbilityAdvancement) {
         this.titleKey = titleKey;
         this.chatKey = chatKey;
         this.grantsAbilityAdvancement = grantsAbilityAdvancement;
      }
   }
}
