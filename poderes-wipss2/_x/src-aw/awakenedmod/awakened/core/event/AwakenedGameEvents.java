package com.awakenedmod.awakened.core.event;

import com.awakenedmod.awakened.Config;
import com.awakenedmod.awakened.content.kineticcharger.KineticChoice;
import com.awakenedmod.awakened.core.AwakenedThreat;
import com.awakenedmod.awakened.core.DamageLedger;
import com.awakenedmod.awakened.core.DiscordAdvert;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.HandsBusy;
import com.awakenedmod.awakened.core.NearDeathAwaken;
import com.awakenedmod.awakened.core.PowerOwnershipRegistry;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.PowerSuppression;
import com.awakenedmod.awakened.core.RealisticMovement;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.glow.LimbGlow;
import com.awakenedmod.awakened.core.power.PassiveToggleService;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerMigrations;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import com.awakenedmod.awakened.core.power.PowerResource;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import com.awakenedmod.awakened.core.quest.QuestTrigger;
import com.awakenedmod.awakened.core.relic.RelicSystem;
import com.awakenedmod.awakened.core.rules.AwakeningRules;
import com.awakenedmod.awakened.core.skill.SkillSystem;
import com.awakenedmod.awakened.core.stamina.StaminaResource;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import com.awakenedmod.awakened.core.team.TeamActions;
import com.awakenedmod.awakened.core.team.TeamFeedback;
import com.awakenedmod.awakened.core.team.TeamService;
import com.awakenedmod.awakened.core.team.TeamSync;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangeGameModeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.StartTracking;

public final class AwakenedGameEvents {
   private static final int STAMINA_SYNC_INTERVAL = 3;
   private static final Set<UUID> PENDING_AWAKEN = new HashSet<>();
   private static final float KILL_CREDIT_FRACTION = 0.3F;

   private AwakenedGameEvents() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(AwakenedGameEvents::onLivingDamage);
      gameBus.addListener(AwakenedGameEvents::onIncomingDamage);
      gameBus.addListener(AwakenedGameEvents::onPlayerTick);
      gameBus.addListener(AwakenedGameEvents::onLogin);
      gameBus.addListener(AwakenedGameEvents::onRespawn);
      gameBus.addListener(AwakenedGameEvents::onLogout);
      gameBus.addListener(AwakenedGameEvents::onDeath);
      gameBus.addListener(AwakenedGameEvents::onBreakSpeed);
      gameBus.addListener(AwakenedGameEvents::onChangedDimension);
      gameBus.addListener(e -> PowerRegistry.all().forEach(Power::clearAllCooldowns));
      gameBus.addListener(EventPriority.LOWEST, AwakenedGameEvents::onGameModeChange);
      gameBus.addListener(AwakenedGameEvents::onStartTracking);
   }

   private static void onStartTracking(StartTracking event) {
      if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer target) {
         PowerService.resendHeldAnimation(viewer, target);
      }
   }

   private static void onChangedDimension(PlayerChangedDimensionEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         Power power = PowerRegistry.get(AwakenedAttachments.get(player).powerId());
         if (power != null) {
            ServerLevel from = player.serverLevel().getServer().getLevel(event.getFrom());
            if (from != null) {
               power.onDimensionChange(player, from);
               PowerService.sync(player);
            }

            LimbGlow.clear(player);
         }
      }
   }

   private static void onBreakSpeed(BreakSpeed event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         PowerData data = AwakenedAttachments.get(player);
         if (data.hasPower()) {
            Power power = PowerRegistry.get(data.powerId());
            if (power != null && !PowerSuppression.isSuppressed(player)) {
               float modified = power.modifyBreakSpeed(player, event.getNewSpeed());
               if (modified != event.getNewSpeed()) {
                  event.setNewSpeed(modified);
               }
            }
         }
      }
   }

   private static void onLivingDamage(Post event) {
      LivingEntity victim = event.getEntity();
      if (victim instanceof ServerPlayer player) {
         PowerData data = AwakenedAttachments.get(player);
         if (!data.awakened()
            && (double)player.getHealth() <= (Double)Config.AWAKEN_HEALTH_THRESHOLD.get()
            && AwakeningRules.mayAwaken(player, event.getSource())) {
            if (player.isDeadOrDying()) {
               PENDING_AWAKEN.add(player.getUUID());
            } else {
               PowerService.awaken(player);
            }
         }

         Power victimPower = data.hasPower() ? PowerRegistry.get(data.powerId()) : null;
         if (victimPower != null && event.getNewDamage() > 0.0F && !PowerSuppression.isSuppressed(player)) {
            victimPower.onDamageTaken(player, event.getSource(), event.getNewDamage());
         }
      }

      if (event.getSource().getEntity() instanceof ServerPlayer attacker && attacker != victim) {
         if (!(victim instanceof Player)) {
            DamageLedger.record(victim, attacker.getUUID(), event.getNewDamage());
         }

         PowerData datax = AwakenedAttachments.get(attacker);
         if (datax.hasPower() && !PowerSuppression.isSuppressed(attacker)) {
            Power power = PowerRegistry.get(datax.powerId());
            if (power != null) {
               Dmg.ridingHit(() -> power.onDealDamage(attacker, victim, event.getSource()));
            }
         }
      }
   }

   private static void onIncomingDamage(LivingIncomingDamageEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         PowerData data = AwakenedAttachments.get(player);
         if (data.hasPower()) {
            Power power = PowerRegistry.get(data.powerId());
            if (power != null && !PowerSuppression.isSuppressed(player)) {
               float reduced = power.onIncomingDamage(player, event.getSource(), event.getAmount());
               if (reduced != event.getAmount()) {
                  event.setAmount(Math.max(0.0F, reduced));
               }
            }
         }
      }
   }

   private static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         HandsBusy.syncTick(player);
         PowerData data = AwakenedAttachments.get(player);
         if (!data.awakened()) {
            return;
         }

         Power power = PowerRegistry.get(data.powerId());
         boolean suppressed = PowerSuppression.update(player, power, data);
         if (power != null && player.isAlive() && !player.isSpectator() && !suppressed) {
            power.passiveTick(player, data);
         }

         RealisticMovement.trackFooting(player);
         float max = StaminaSystem.max(player);
         float locked = StaminaSystem.locked(player, data, power);
         float regenMax = Math.max(0.0F, max - locked);
         boolean active = power != null && power.hasActiveAbility(player) && !data.infiniteStamina();
         if (!active) {
            ((PowerResource)(power != null ? power.resource() : StaminaResource.INSTANCE)).regenTick(player, data, regenMax);
         }

         boolean full = data.stamina() >= regenMax;
         boolean stale = data.staminaSyncStale() || data.lockedSyncStale(locked);
         if (stale && (full || player.tickCount % 3 == 0)) {
            PowerService.sync(player);
         }
      }
   }

   private static void onLogin(PlayerLoggedInEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         PowerData data = AwakenedAttachments.get(player);
         PowerMigrations.apply(player, data);
         if (PowerService.expireIfStale(player)) {
            player.sendSystemMessage(Component.translatable("awakened.feedback.power_expired").withStyle(ChatFormatting.GRAY));
         }

         Power power = PowerRegistry.get(data.powerId());
         if (power != null) {
            power.onLogin(player, data);
         }

         if (data.awakened()) {
            PowerOwnershipRegistry.set(player.serverLevel().getServer(), player.getUUID(), data.powerId());
         }

         SkillSystem.applyAttributes(player);
         RelicSystem.refresh(player);
         PowerService.sync(player);
         PowerService.syncSkill(player);
         PowerService.syncRelics(player);
         PassiveToggleService.sync(player);
         QuestSystem.sync(player);
         RealisticMovement.syncTo(player);
         TeamService.refreshName(player);
         TeamSync.sendToTeam(player.serverLevel().getServer(), TeamService.teamOf(player.serverLevel().getServer(), player.getUUID()));
         TeamSync.send(player);
         DiscordAdvert.onLogin(player);
         KineticChoice.onLogin(player);
         if (!player.isCreative() && !player.isSpectator() && player.getAbilities().mayfly) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.getAbilities().setFlyingSpeed(0.05F);
            player.onUpdateAbilities();
         }

         if (!player.isCreative() && !player.isSpectator() && player.isInvulnerable()) {
            player.setInvulnerable(false);
         }
      }
   }

   private static void onRespawn(PlayerRespawnEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         clearAbilityState(player);
         SkillSystem.applyAttributes(player);
         RelicSystem.refresh(player);
         PowerData data = AwakenedAttachments.get(player);
         data.setStamina(StaminaSystem.max(player));
         Power respawned = PowerRegistry.get(data.powerId());
         if (respawned != null) {
            respawned.onRespawn(player, data);
         }

         PowerService.sync(player);
         PowerService.syncSkill(player);
         PowerService.syncRelics(player);
         QuestSystem.sync(player);
         if (PENDING_AWAKEN.remove(player.getUUID())
            && !AwakenedAttachments.get(player).awakened()
            && NearDeathAwaken.enabled(player.serverLevel().getServer())) {
            PowerService.awaken(player);
         }
      }
   }

   private static void onLogout(PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         clearAbilityState(player);
         Power leavingPower = PowerRegistry.get(AwakenedAttachments.get(player).powerId());
         if (leavingPower != null) {
            leavingPower.onLogout(player);
         }

         PowerData leavingData = AwakenedAttachments.get(player);
         if (leavingData.awakened()) {
            PowerOwnershipRegistry.set(player.serverLevel().getServer(), player.getUUID(), leavingData.powerId());
         }

         MasterySystem.forget(player.getUUID());
         PowerService.forgetHeldAnimation(player);
         PowerService.forgetSecondaryKey(player);
         RealisticMovement.forget(player.getUUID());
         TeamActions.inviteesForget(player);
         TeamFeedback.forget(player.getUUID());
         MinecraftServer server = player.serverLevel().getServer();
         UUID leftTeam = TeamService.teamOf(server, player.getUUID());
         if (server != null && leftTeam != null) {
            server.execute(() -> TeamSync.sendToTeam(server, leftTeam));
         }

         PENDING_AWAKEN.remove(player.getUUID());
      }
   }

   private static void onGameModeChange(PlayerChangeGameModeEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && event.getNewGameMode() == GameType.SPECTATOR && event.getCurrentGameMode() != GameType.SPECTATOR) {
         clearAbilityState(player);
         PowerService.stopAnimation(player);
      }
   }

   private static void clearAbilityState(ServerPlayer player) {
      Power power = PowerRegistry.get(AwakenedAttachments.get(player).powerId());
      if (power != null) {
         power.clearAbilityState(player);
      }
   }

   private static void onDeath(LivingDeathEvent event) {
      LivingEntity victim = event.getEntity();
      if (victim instanceof ServerPlayer deadPlayer) {
         Power victimPower = PowerRegistry.get(AwakenedAttachments.get(deadPlayer).powerId());
         if (victimPower != null) {
            victimPower.onDeath(deadPlayer, event.getSource());
            clearAbilityState(deadPlayer);
         }

         ServerPlayer finisher = killCredit(event);
         if (finisher != null && finisher != deadPlayer) {
            firePowerOnKill(finisher, deadPlayer);
         }
      }

      if (!(victim instanceof Player) && victim.level() instanceof ServerLevel level) {
         HashSet var10 = new HashSet<>(DamageLedger.creditedPlayers(victim, 0.3F));
         ServerPlayer finisher = killCredit(event);
         if (finisher != null) {
            var10.add(finisher.getUUID());
         }

         for (UUID id : var10) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
            if (player != null && AwakenedAttachments.get(player).hasPower()) {
               creditKill(player, victim);
            }
         }

         if (victim instanceof AwakenedThreat threat && threat.isBoss()) {
            announceBossDamage(level, victim);
         }
      }
   }

   private static void announceBossDamage(ServerLevel level, LivingEntity victim) {
      Map<UUID, Float> contributions = DamageLedger.contributions(victim);
      if (!contributions.isEmpty()) {
         float total = 0.0F;

         for (float dmg : contributions.values()) {
            total += dmg;
         }

         if (!(total <= 0.0F)) {
            List<Entry<UUID, Float>> ranked = new ArrayList<>(contributions.entrySet());
            ranked.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("awakened.boss.damage_header", new Object[]{victim.getDisplayName()}).withStyle(ChatFormatting.GOLD));
            int rank = 1;

            for (Entry<UUID, Float> entry : ranked) {
               ServerPlayer contributor = level.getServer().getPlayerList().getPlayer(entry.getKey());
               Component name = (Component)(contributor != null ? contributor.getDisplayName() : Component.translatable("awakened.boss.damage_unknown"));
               int percent = Math.round(entry.getValue() / total * 100.0F);
               lines.add(
                  Component.translatable(
                        "awakened.boss.damage_line", new Object[]{rank++, name, (int)Math.ceil((double)entry.getValue().floatValue()), percent}
                     )
                     .withStyle(ChatFormatting.YELLOW)
               );
            }

            for (UUID id : contributions.keySet()) {
               ServerPlayer contributor = level.getServer().getPlayerList().getPlayer(id);
               if (contributor != null) {
                  lines.forEach(contributor::sendSystemMessage);
               }
            }
         }
      }
   }

   private static void firePowerOnKill(ServerPlayer player, LivingEntity victim) {
      Power power = PowerRegistry.get(AwakenedAttachments.get(player).powerId());
      if (power != null && !PowerSuppression.isSuppressed(player)) {
         power.onKill(player, victim);
      }
   }

   private static void creditKill(ServerPlayer player, LivingEntity victim) {
      firePowerOnKill(player, victim);
      QuestSystem.report(player, QuestTrigger.DEFEAT_NAMED, BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString(), 1);
      if (Foes.countsAsKill(player, victim)) {
         MasterySystem.award(player, MasteryReason.KILL);
         AwakenedAttachments.progress(player).enemyDefeated();
         PowerService.syncProgress(player);
      }

      if (victim instanceof AwakenedThreat threat) {
         MasterySystem.grant(player, threat.bonusMastery());
         SkillSystem.award(player, threat.bonusXp());
         player.displayClientMessage(Component.translatable("awakened.threat.defeated", new Object[]{victim.getDisplayName(), (int)threat.bonusXp()}), false);
         threat.onCreditedKill(player);
      }
   }

   private static ServerPlayer killCredit(LivingDeathEvent event) {
      Entity var2 = event.getSource().getEntity();
      if (var2 instanceof ServerPlayer) {
         return (ServerPlayer)var2;
      } else {
         return event.getEntity().getKillCredit() instanceof ServerPlayer credited ? credited : null;
      }
   }
}
