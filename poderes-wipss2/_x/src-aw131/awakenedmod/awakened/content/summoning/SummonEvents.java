package com.awakenedmod.awakened.content.summoning;

import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.PowerSuppression;
import com.awakenedmod.awakened.core.npc.PoweredNpc;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import com.awakenedmod.awakened.core.team.EntityOwners;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent.Post;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable.Result;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific;

public final class SummonEvents {
   private SummonEvents() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(SummonEvents::onJoin);
      gameBus.addListener(SummonEvents::onChangeTarget);
      gameBus.addListener(SummonEvents::onDeath);
      gameBus.addListener(SummonEvents::onIncomingDamage);
      gameBus.addListener(SummonEvents::onEffectApplicable);
      gameBus.addListener(SummonEvents::onMobGriefing);
      gameBus.addListener(SummonEvents::onConverted);
      gameBus.addListener(SummonEvents::onChangedDimension);
      gameBus.addListener(SummonEvents::onEntityInteractSpecific);
      gameBus.addListener(SummonEvents::onEntityInteract);
      SummonChain.register(gameBus);
      SummonMounts.register(gameBus);
      EntityOwners.register("summoning", e -> e instanceof Mob mob ? MinionSupport.allegianceOwner(mob) : null);
   }

   private static void onChangedDimension(PlayerChangedDimensionEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         ServerLevel from = player.serverLevel().getServer().getLevel(event.getFrom());
         if (from != null) {
            if (MinionSupport.recallAllIn(from, player) != 0) {
               player.displayClientMessage(Component.translatable("awakened.summon.dimension_recall"), true);
            }
         }
      }
   }

   private static boolean isMinion(Entity target) {
      if (target instanceof Mob mob && SummonAttachments.minion(mob).isMinion()) {
         return true;
      }

      return false;
   }

   private static void onEntityInteractSpecific(EntityInteractSpecific event) {
      if (event.getItemStack().is(Items.SHEARS) && isMinion(event.getTarget())) {
         event.setCanceled(true);
      }
   }

   private static void onEntityInteract(EntityInteract event) {
      if (event.getItemStack().is(Items.SHEARS) && isMinion(event.getTarget())) {
         event.setCanceled(true);
      }
   }

   private static void onJoin(EntityJoinLevelEvent event) {
      if (!event.getLevel().isClientSide() && event.getEntity() instanceof Mob mob) {
         if (SummonAttachments.minion(mob).isMinion()) {
            MinionSupport.ensureAllyGoals(mob);
         } else {
            if (!event.loadedFromDisk()) {
               SummonChain.adoptIfSummonedByMinion(mob);
            }
         }
      }
   }

   private static void onChangeTarget(LivingChangeTargetEvent event) {
      if (event.getEntity() instanceof Mob mob && MinionSupport.alliedSummonForbidden(mob, event.getNewAboutToBeSetTarget())) {
         event.setCanceled(true);
      }
   }

   private static void onDeath(LivingDeathEvent event) {
      LivingEntity victim = event.getEntity();
      if (!(victim instanceof Player) && victim.level() instanceof ServerLevel level && SummonSouls.harvestable(victim)) {
         if (event.getSource().getEntity() instanceof Mob killer && SummonAttachments.minion(killer).isMinion()) {
            UUID ownerId = SummonAttachments.minion(killer).owner();
            ServerPlayer owner = ownerId == null ? null : level.getServer().getPlayerList().getPlayer(ownerId);
            if (owner != null && PowerSuppression.isSuppressed(owner)) {
               return;
            }

            SummonSouls.dropSoul(level, victim, ownerId);
            reportPilotKill(level, killer, ownerId, victim);
         }
      }
   }

   private static void reportPilotKill(ServerLevel level, Mob killer, UUID ownerId, LivingEntity victim) {
      ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
      if (owner != null && Foes.countsAsKill(owner, victim)) {
         if (PossessControl.piloted(owner) == killer) {
            QuestSystem.feat(owner, "summon_pilot_kill");
         }
      }
   }

   private static void onIncomingDamage(LivingIncomingDamageEvent event) {
      if (event.getSource().getEntity() instanceof Mob source) {
         if (MinionSupport.alliedSummonForbidden(source, event.getEntity())) {
            event.setCanceled(true);
         } else {
            MinionData minion = SummonAttachments.minion(source);
            if (minion.isMinion()) {
               float bonus = minion.powerBonus();
               float amount = bonus > 1.0E-4F ? event.getAmount() * (1.0F + bonus) : event.getAmount();
               event.setAmount(source instanceof PoweredNpc ? amount : MasteryScaling.configScaledDamage(amount, SummonAbility.class.getSimpleName()));
            }
         }
      }
   }

   private static void onMobGriefing(EntityMobGriefingEvent event) {
      if (event.canGrief() && alliedSummonBehind(event.getEntity()) != null) {
         event.setCanGrief(false);
      }
   }

   private static Mob alliedSummonBehind(Entity entity) {
      if (entity instanceof Mob mob && MinionSupport.allegianceOwner(mob) != null) {
         return mob;
      }

      if (entity instanceof TraceableEntity traceable && traceable.getOwner() instanceof Mob shooter && MinionSupport.allegianceOwner(shooter) != null) {
         return shooter;
      }

      return null;
   }

   private static void onEffectApplicable(Applicable event) {
      if (((MobEffect)event.getEffectInstance().getEffect().value()).getCategory() == MobEffectCategory.HARMFUL) {
         Mob summon = alliedSummonBehind(event.getEffectSource());
         if (summon != null && MinionSupport.alliedSummonForbidden(summon, event.getEntity())) {
            event.setResult(Result.DO_NOT_APPLY);
         }
      }
   }

   private static void onConverted(Post event) {
      if (event.getEntity() instanceof Mob original && event.getOutcome() instanceof Mob outcome) {
         MinionData data = SummonAttachments.minion(original);
         if (data.isMinion()) {
            outcome.setData(SummonAttachments.MINION, data);
            outcome.setPersistenceRequired();
            MinionSupport.applyStoredScaling(outcome);
            MinionSupport.ensureAllyGoals(outcome);
         }

         return;
      }
   }
}
