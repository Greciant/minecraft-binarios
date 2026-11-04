package com.devilfruits.fruits.yomi_yomi.soul;

import com.devilfruits.core.CoreEvents;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.config.CoreConfig;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.core.form.FormManager;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.rules.FruitRules;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.fruits.yomi_yomi.Vessel;
import com.devilfruits.fruits.yomi_yomi.YomiEntities;
import com.devilfruits.fruits.yomi_yomi.YomiFx;
import com.devilfruits.fruits.yomi_yomi.YomiItems;
import com.devilfruits.fruits.yomi_yomi.YomiNetwork;
import com.devilfruits.fruits.yomi_yomi.YomiSounds;
import com.devilfruits.fruits.yomi_yomi.YomiTriggers;
import com.devilfruits.fruits.yomi_yomi.YomiYomi;
import com.devilfruits.fruits.yomi_yomi.entity.PlayerCorpse;
import com.devilfruits.registry.DFAttachments;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;

public final class SoulPhase {
   public static final int DIVE_TICKS = 15;

   public static boolean deathPhase(Player player) {
      return YomiSoul.of(player).active();
   }

   public static boolean inSoul(Player player) {
      return deathPhase(player) || SoulProjection.active(player);
   }

   public static boolean interceptDeath(ServerPlayer player, DevilFruit fruit, DamageSource source) {
      if (fruit != YomiYomi.fruit()) {
         return false;
      } else {
         YomiSoul soul = YomiSoul.of(player);
         if (soul.active()) {
            discardCorpse(player, soul);
            player.setData(YomiSoul.ATTACHMENT, YomiSoul.NONE);
            return false;
         } else {
            PlayerFruitData data = DFAttachments.data(player);
            ServerLevel level = player.serverLevel();
            if (data.homeName().isEmpty() && !(player.getY() < (double)level.getMinBuildHeight())) {
               PlayerCorpse corpse = new PlayerCorpse(YomiEntities.PLAYER_CORPSE.get(), level);
               corpse.setup(player, (float)YomiYomi.setting("corpseHealth"));
               level.addFreshEntity(corpse);
               int ticks = (int)Math.round(YomiYomi.setting("soulSeconds") * 20.0);
               player.setData(YomiSoul.ATTACHMENT, new YomiSoul(ticks, Optional.of(corpse.getUUID()), level.dimension(), corpse.position(), true));
               YomiTriggers.SOUL_DEPARTED.get().trigger(player);
               ServerFx.sendNear(level, YomiFx.SOUL_DEPART, corpse.getId(), corpse.position(), Vec3.ZERO, 1.0F, player.getRandom().nextLong(), 0, 48.0);
               level.playSound(null, corpse.getX(), corpse.getY(), corpse.getZ(), YomiSounds.SOUL_DEPART.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
               return true;
            } else {
               return false;
            }
         }
      }
   }

   private static void onRespawn(PlayerRespawnEvent event) {
      if (!event.isEndConquered() && event.getEntity() instanceof ServerPlayer player && YomiYomi.isUser(player) && YomiSoul.of(player).pending()) {
         spawnSoul(player);
      }
   }

   public static void spawnSoul(ServerPlayer player) {
      YomiSoul soul = YomiSoul.of(player);
      ServerLevel level = player.server.getLevel(soul.dimension());
      if (level == null) {
         level = player.serverLevel();
      }

      double y = Math.min(soul.body().y + YomiYomi.setting("soulHeight"), (double)(level.getMaxBuildHeight() - 2));
      move(player, level, soul.body().x, y, soul.body().z, player.getYRot(), 30.0F);
      player.setData(YomiSoul.ATTACHMENT, soul.respawned());
      FormManager.request(player, "soul", FormManager.Reason.COMMAND);
      player.getAbilities().flying = true;
      player.onUpdateAbilities();
      player.displayClientMessage(Component.translatable("message.devilfruits.yomi_yomi.soul_departed").withStyle(ChatFormatting.GREEN), false);
      CoreEvents.syncNow(player);
   }

   private static void onTick(Post event) {
      if (event.getEntity() instanceof ServerPlayer player && player.isAlive()) {
         YomiSoul soul = YomiSoul.of(player);
         if (soul.active() && !soul.pending() && YomiYomi.isUser(player)) {
            tick(player, soul);
         }
      }
   }

   public static void tick(ServerPlayer player, YomiSoul soul) {
      if (player.tickCount % 10 == 0) {
         int left = soul.ticksLeft() - 10;
         if (left <= 0) {
            dissipate(player, SoulPhase.End.TIMEOUT);
            return;
         }

         player.setData(YomiSoul.ATTACHMENT, soul.withTicks(left));
      }

      if (player.tickCount % 20 == 0) {
         ServerLevel level = player.server.getLevel(soul.dimension());
         if (level != null && corpseLost(level, soul)) {
            dissipate(player, SoulPhase.End.DESTROYED);
            return;
         }

         if (!"soul".equals(DFAttachments.data(player).formName())) {
            FormManager.request(player, "soul", FormManager.Reason.COMMAND);
         }
      }
   }

   private static boolean corpseLost(ServerLevel level, YomiSoul soul) {
      BlockPos pos = BlockPos.containing(soul.body());
      return level.areEntitiesLoaded(ChunkPos.asLong(pos)) && level.getEntity(soul.corpseId()) == null;
   }

   public static void onCorpseDestroyed(ServerLevel level, @Nullable UUID owner) {
      ServerPlayer player = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
      if (player != null && player.isAlive() && deathPhase(player) && !YomiSoul.of(player).pending()) {
         dissipate(player, SoulPhase.End.DESTROYED);
      }
   }

   public static void dissipate(ServerPlayer player, SoulPhase.End reason) {
      YomiSoul soul = YomiSoul.of(player);
      discardCorpse(player, soul);
      player.setData(YomiSoul.ATTACHMENT, YomiSoul.NONE);
      ServerFx.send(player, YomiFx.SOUL_FADE, player.position(), Vec3.ZERO, 1.0F, player.getRandom().nextLong(), reason.ordinal());
      player.level().playSound(null, player.getX(), player.getY(), player.getZ(), YomiSounds.SOUL_FADE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
      FormManager.revert(player, FormManager.Reason.POWER_LOST);
      player.getAbilities().flying = false;
      player.onUpdateAbilities();
      player.displayClientMessage(
         Component.translatable("message.devilfruits.yomi_yomi.soul_faded." + reason.name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.DARK_GREEN),
         false
      );
      if (!(player instanceof FakePlayer)) {
         DimensionTransition home = player.findRespawnPositionAndUseSpawnBlock(false, DimensionTransition.DO_NOTHING);
         player.changeDimension(home);
      }

      DevilFruit fruit = YomiYomi.fruit();
      if ((Boolean)CoreConfig.LOSE_ON_DEATH.get()) {
         FruitRules.removePower(player);
         player.sendSystemMessage(fruit.message("power_lost").withStyle(new ChatFormatting[]{ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC}));
         ServerLevel bodyLevel = player.server.getLevel(soul.dimension());
         if (fruit.reincarnates() && bodyLevel != null) {
            FruitRules.reincarnate(bodyLevel, BlockPos.containing(soul.body()), fruit);
         }
      } else {
         CoreEvents.syncNow(player);
      }
   }

   public static void requestPossession(ServerPlayer player, PlayerCorpse corpse) {
      if (canPossess(player, corpse)) {
         YomiNetwork.openVesselMenu(player, corpse);
      }
   }

   public static boolean canPossess(ServerPlayer player, @Nullable Entity corpse) {
      YomiSoul soul = YomiSoul.of(player);
      if (corpse instanceof PlayerCorpse body
         && body.isAlive()
         && soul.active()
         && !soul.pending()
         && body.getUUID().equals(soul.corpseId())
         && YomiYomi.isUser(player)
         && (double)player.distanceTo(body) <= YomiYomi.setting("possessReach") + 1.0) {
         return true;
      }

      return false;
   }

   public static boolean beginPossession(ServerPlayer player, PlayerCorpse corpse, Vessel vessel) {
      if (canPossess(player, corpse) && !FormFlags.has(player, YomiYomi.POSSESSING)) {
         ServerLevel level = (ServerLevel)corpse.level();
         move(player, level, corpse.getX(), corpse.getY() + 1.2, corpse.getZ(), corpse.getYRot(), 35.0F);
         player.getAbilities().flying = true;
         player.onUpdateAbilities();
         FormFlags.set(player, YomiYomi.POSSESSING, true);
         AbilityManager.setBusy(player, 17);
         ServerFx.send(player, YomiFx.POSSESS_DIVE, corpse.position(), Vec3.ZERO, 1.0F, player.getRandom().nextLong(), vessel.ordinal());
         ServerScheduler.later(level, 15, () -> {
            boolean diving = FormFlags.has(player, YomiYomi.POSSESSING);
            FormFlags.set(player, YomiYomi.POSSESSING, false);
            if (diving && !player.isRemoved() && player.isAlive() && possess(player, corpse, vessel)) {
               ServerFx.animate(player, YomiYomi.id("revive"));
            }
         });
         return true;
      } else {
         return false;
      }
   }

   public static boolean possess(ServerPlayer player, PlayerCorpse corpse, Vessel vessel) {
      if (!canPossess(player, corpse)) {
         return false;
      } else {
         move(player, (ServerLevel)corpse.level(), corpse.getX(), corpse.getY() + 0.05, corpse.getZ(), corpse.getYRot() + 180.0F, 0.0F);
         player.setYBodyRot(corpse.getYRot() + 180.0F);
         player.setYHeadRot(corpse.getYRot() + 180.0F);
         corpse.discard();
         becomeVessel(player, vessel);
         YomiTriggers.REVIVED.get().trigger(player);
         if (vessel == Vessel.SKELETON) {
            YomiTriggers.SKELETON.get().trigger(player);
         }

         ServerFx.send(player, YomiFx.POSSESS, player.position(), Vec3.ZERO, 1.0F, player.getRandom().nextLong(), vessel.ordinal());
         player.level().playSound(null, player.getX(), player.getY(), player.getZ(), YomiSounds.POSSESS.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
         player.displayClientMessage(
            Component.translatable("message.devilfruits.yomi_yomi.revived", new Object[]{vessel.displayName()}).withStyle(ChatFormatting.GREEN), false
         );
         if (YomiYomi.flag("giftCane") && !hasCane(player)) {
            ItemStack cane = new ItemStack((ItemLike)YomiItems.SOUL_CANE.get());
            if (!player.getInventory().add(cane)) {
               player.drop(cane, false);
            }
         }

         return true;
      }
   }

   public static void becomeVessel(ServerPlayer player, Vessel vessel) {
      YomiSoul soul = YomiSoul.of(player);
      if (soul.active()) {
         discardCorpse(player, soul);
         player.setData(YomiSoul.ATTACHMENT, YomiSoul.NONE);
      }

      DFAttachments.data(player).setHome(vessel.formName());
      FormManager.request(player, vessel.formName(), FormManager.Reason.COMMAND);
      player.getAbilities().flying = false;
      player.onUpdateAbilities();
      CoreEvents.syncNow(player);
   }

   private static boolean hasCane(ServerPlayer player) {
      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
         ItemStack s = player.getInventory().getItem(i);
         if (s.is((Item)YomiItems.SOUL_CANE.get()) || s.is((Item)YomiItems.SCABBARD.get())) {
            return true;
         }
      }

      return false;
   }

   private static void move(ServerPlayer player, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
      if (player instanceof FakePlayer && level == player.level()) {
         player.moveTo(x, y, z, yaw, pitch);
      } else {
         player.teleportTo(level, x, y, z, yaw, pitch);
      }
   }

   public static void onPowerRemoved(ServerPlayer player) {
      YomiSoul soul = YomiSoul.of(player);
      if (soul.active()) {
         discardCorpse(player, soul);
         player.setData(YomiSoul.ATTACHMENT, YomiSoul.NONE);
         player.getAbilities().flying = false;
         player.onUpdateAbilities();
      }
   }

   private static void discardCorpse(ServerPlayer player, YomiSoul soul) {
      ServerLevel level = player.server.getLevel(soul.dimension());
      Entity corpse = level != null && soul.corpseId() != null ? level.getEntity(soul.corpseId()) : null;
      if (corpse != null) {
         corpse.discard();
      }
   }

   public static void register(IEventBus bus) {
      bus.addListener(EventPriority.LOW, SoulPhase::onRespawn);
      bus.addListener(SoulPhase::onTick);
   }

   private SoulPhase() {
   }

   public static enum End {
      TIMEOUT,
      DESTROYED,
      LOST;
   }
}
