package com.devilfruits.core;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityRegistry;
import com.devilfruits.core.ability.AimHelper;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.command.DevilFruitCommand;
import com.devilfruits.core.config.CoreConfig;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.ChargedJump;
import com.devilfruits.core.form.FormManager;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.form.SafeLanding;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.DevilFruitItem;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.haki.HakiCommand;
import com.devilfruits.core.logia.LogiaManager;
import com.devilfruits.core.logia.LogiaState;
import com.devilfruits.core.network.AbilityTablePayload;
import com.devilfruits.core.network.DFNetwork;
import com.devilfruits.core.rules.FruitRules;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.core.rules.Weakness;
import com.devilfruits.registry.DFAttachments;
import com.devilfruits.registry.DFDataComponents;
import com.devilfruits.registry.DFItems;
import com.devilfruits.registry.DFTags;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.EntityEvent.Size;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Finish;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.StartTracking;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@EventBusSubscriber(
   modid = "devilfruits"
)
public final class CoreEvents {
   public static void syncNow(ServerPlayer player) {
      PlayerFruitData data = DFAttachments.data(player);
      player.syncData(DFAttachments.PLAYER_DATA);
      data.clearDirty();
   }

   @SubscribeEvent
   public static void onPlayerTick(Post event) {
      Player player = event.getEntity();
      PlayerFruitData data = DFAttachments.data(player);
      if (data.hasFruit()) {
         if (player instanceof ServerPlayer serverPlayer) {
            tickServer(serverPlayer, data);
         } else if (player.isLocalPlayer()) {
            Weakness.applySinking(player);
         }
      }
   }

   public static void tickServer(ServerPlayer player, PlayerFruitData data) {
      long now = player.level().getGameTime();
      LogiaManager.tick(player, data);
      Weakness.tickServer(player, data);
      ChargedJump.tick(player);
      int max = Mastery.maxEnergy(data);
      if (data.energy() > (float)max) {
         data.setEnergy((float)max);
      } else if (data.energy() < (float)max && !data.suppressed() && !regenerationPaused(data)) {
         DevilFruit fruit = data.fruit();
         boolean inCombat = now - data.lastCombatTick() < (long)((Integer)CoreConfig.COMBAT_SECONDS.get()).intValue() * 20L;
         double perSecond = fruit == null ? 0.0 : (inCombat ? fruit.config().regenInCombat() : fruit.config().regenOutOfCombat());
         FruitForm form = data.form();
         if (form != null) {
            perSecond *= form.config().regenMultiplier();
         }

         perSecond *= LogiaManager.regenMultiplier(player);
         data.setEnergy((float)Math.min((double)max, (double)data.energy() + perSecond / 20.0));
      }

      AbilityManager.tickPlayer(player, data);
      FormManager.tick(player, data);
      if (data.isUrgent() || data.isDirty() && now % 5L == 0L) {
         syncNow(player);
      }
   }

   private static boolean regenerationPaused(PlayerFruitData data) {
      if (!FormManager.isDraining(data) && data.logia().state() != LogiaState.ELEMENTAL_BODY) {
         for (ResourceLocation id : data.activeToggles()) {
            Ability ability = AbilityRegistry.get(id);
            if (ability != null && ability.castType() == CastType.TOGGLE && ability.pausesRegeneration()) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   @SubscribeEvent
   public static void onLogin(PlayerLoggedInEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         PlayerFruitData data = DFAttachments.data(player);
         data.clearToggles();
         DFNetwork.sendTo(player, AbilityTablePayload.of(data.fruit()));
         FormManager.reapply(player);
         LogiaManager.settle(player);
         syncNow(player);
      }
   }

   @SubscribeEvent
   public static void onLogout(PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         AbilityManager.cancelAll(player);
         AbilityManager.forget(player.getUUID());
         FormManager.forget(player);
         LogiaManager.forget(player);
         ChargedJump.forget(player);
         AimHelper.forget(player.getUUID());
      }
   }

   @SubscribeEvent
   public static void onChangedDimension(PlayerChangedDimensionEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         AbilityManager.cancelAll(player);
         FormManager.cancelWindup(player);
         FormManager.reapply(player);
         LogiaManager.settle(player);
         syncNow(player);
      }
   }

   @SubscribeEvent
   public static void onRespawn(PlayerRespawnEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         FormManager.reapply(player);
         LogiaManager.settle(player);
         syncNow(player);
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onDeath(LivingDeathEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && DFAttachments.data(player).hasFruit()) {
         AbilityManager.cancelAll(player);
         AbilityManager.forget(player.getUUID());
         boolean keep = FruitRules.interceptDeath(player, event.getSource()) || FruitRules.keepsFruit(player, event.getSource());
         if (keep || (Boolean)CoreConfig.REVERT_ON_DEATH.get()) {
            FormManager.revert(player, FormManager.Reason.DEATH);
         }

         LogiaManager.onDeath(player);
         if (keep) {
            FruitRules.keepOnDeath(player);
         } else {
            FruitRules.onUserDeath(player);
         }
      }
   }

   @SubscribeEvent
   public static void onStartTracking(StartTracking event) {
      if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer tracker) {
         PlayerFruitData data = DFAttachments.data(target);
         if (!data.hasFruit()) {
            return;
         }

         ServerFx.formStateTo(tracker, target);
         LogiaManager.sendTo(tracker, target);

         for (ResourceLocation id : data.activeToggles()) {
            Ability ability = AbilityRegistry.get(id);
            if (ability != null) {
               ability.onStartTracking(target, tracker);
            }
         }

         Ability charging = AbilityManager.chargingAbility(target);
         if (charging != null) {
            ServerFx.chargeTo(tracker, target, charging, (float)charging.maxChargeTicks(), (long)target.getId() * 31L);
         }

         Ability channelling = AbilityManager.channellingAbility(target);
         if (channelling != null) {
            ServerFx.chargeTo(tracker, target, channelling, (float)channelling.maxChannelTicks(), (long)target.getId() * 31L);
         }
      }
   }

   @SubscribeEvent
   public static void onEntitySize(Size event) {
      if (event.getEntity() instanceof Player player && player.hasData(DFAttachments.PLAYER_DATA)) {
         FruitForm form = DFAttachments.data(player).form();
         EntityDimensions dimensions = form == null ? null : form.dimensions();
         if (dimensions != null && (event.getPose() == Pose.STANDING || event.getPose() == Pose.CROUCHING)) {
            event.setNewSize(dimensions.scale(player.getScale()));
         }

         return;
      }
   }

   @SubscribeEvent
   public static void onFall(LivingFallEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && SafeLanding.consume(player)) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onIncomingDamage(LivingIncomingDamageEvent event) {
      if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof ServerPlayer victim) {
         PlayerFruitData data = DFAttachments.data(victim);
         if (data.hasFruit()) {
            data.setLastCombatTick(victim.level().getGameTime());
         }
      }
   }

   @SubscribeEvent
   public static void onFinishEating(Finish event) {
      if (event.getEntity() instanceof ServerPlayer player
         && event.getItem().is(DFTags.DEVIL_FRUITS)
         && !(event.getItem().getItem() instanceof DevilFruitItem)
         && DFAttachments.data(player).hasFruit()) {
         FruitRules.secondFruit(player);
      }
   }

   @SubscribeEvent
   public static void onEntityJoin(EntityJoinLevelEvent event) {
      if (!event.getLevel().isClientSide()
         && event.getEntity() instanceof ItemEntity item
         && (item.getItem().has((DataComponentType)DFDataComponents.FRUIT_INSTANCE.get()) || item.getItem().is((Item)DFItems.CASKET.get()))) {
         item.setUnlimitedLifetime();
      }
   }

   @SubscribeEvent
   public static void onEntityLeave(EntityLeaveLevelEvent event) {
      if (!event.getLevel().isClientSide()
         && event.getEntity() instanceof ItemEntity item
         && item.getItem().has((DataComponentType)DFDataComponents.FRUIT_INSTANCE.get())) {
         RemovalReason reason = item.getRemovalReason();
         boolean destroyed = reason == RemovalReason.KILLED
            || reason == RemovalReason.DISCARDED && item.getY() < (double)(event.getLevel().getMinBuildHeight() - 32);
         if (destroyed && item.getServer() != null) {
            FruitRules.onFruitItemDestroyed(item.getServer(), item.getItem());
         }
      }
   }

   @SubscribeEvent
   public static void onRegisterCommands(RegisterCommandsEvent event) {
      DevilFruitCommand.register(event.getDispatcher());
      HakiCommand.register(event.getDispatcher());
   }

   public static void broadcastAbilityTables() {
      MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
      if (server != null) {
         server.execute(() -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
               DFNetwork.sendTo(player, AbilityTablePayload.of(DFAttachments.data(player).fruit()));
            }
         });
      }
   }

   private CoreEvents() {
   }
}
