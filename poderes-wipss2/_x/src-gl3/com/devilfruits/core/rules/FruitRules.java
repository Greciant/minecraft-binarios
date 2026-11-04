package com.devilfruits.core.rules;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.CoreEvents;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.config.CoreConfig;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.FormManager;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.FruitFx;
import com.devilfruits.core.fruit.FruitRegistry;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.logia.LogiaManager;
import com.devilfruits.core.network.AbilityTablePayload;
import com.devilfruits.core.network.DFNetwork;
import com.devilfruits.registry.DFAttachments;
import com.devilfruits.registry.DFDamageTypes;
import com.devilfruits.registry.DFDataComponents;
import com.devilfruits.registry.DFTriggers;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

public final class FruitRules {
   private static final List<BiConsumer<ServerPlayer, DevilFruit>> POWER_REMOVED = new ArrayList<>();
   private static final List<FruitRules.DeathInterceptor> DEATH_INTERCEPTORS = new ArrayList<>();

   public static ItemStack createWorldFruit(MinecraftServer server, DevilFruit fruit) {
      if (!fruit.hasItem()) {
         return ItemStack.EMPTY;
      } else {
         return CoreConfig.ONE_PER_WORLD.get() && WorldFruitData.get(server).state(fruit) != WorldFruitData.State.FREE
            ? ItemStack.EMPTY
            : forceCreateWorldFruit(server, fruit);
      }
   }

   public static ItemStack forceCreateWorldFruit(MinecraftServer server, DevilFruit fruit) {
      UUID id = UUID.randomUUID();
      ItemStack stack = new ItemStack((ItemLike)fruit.item().orElseThrow());
      stack.set((DataComponentType)DFDataComponents.FRUIT_INSTANCE.get(), id);
      if ((Boolean)CoreConfig.ONE_PER_WORLD.get()) {
         WorldFruitData.get(server).setItem(fruit, id);
      }

      return stack;
   }

   public static FruitRules.EatOutcome eat(ServerPlayer player, DevilFruit fruit, ItemStack stack) {
      PlayerFruitData data = DFAttachments.data(player);
      if (data.hasFruit()) {
         return secondFruit(player);
      } else if (!fruit.config().enabled()) {
         player.displayClientMessage(
            Component.translatable("message.devilfruits.fruit_disabled", new Object[]{fruit.displayName()}).withStyle(ChatFormatting.GRAY), true
         );
         return FruitRules.EatOutcome.DISABLED;
      } else {
         if ((Boolean)CoreConfig.ONE_PER_WORLD.get()) {
            WorldFruitData world = WorldFruitData.get(player.server);
            if (world.state(fruit) == WorldFruitData.State.EATEN && world.owner(fruit).filter(o -> !o.equals(player.getUUID())).isPresent()) {
               player.displayClientMessage(fruit.message("power_taken", world.ownerName(fruit)).withStyle(ChatFormatting.GRAY), false);
               return FruitRules.EatOutcome.POWER_TAKEN;
            }
         }

         grantPower(player, fruit, true);
         return FruitRules.EatOutcome.GRANTED;
      }
   }

   public static FruitRules.EatOutcome secondFruit(ServerPlayer player) {
      if (!(Boolean)CoreConfig.SECOND_FRUIT_KILLS.get()) {
         player.displayClientMessage(Component.translatable("message.devilfruits.second_fruit_safe").withStyle(ChatFormatting.GRAY), true);
         return FruitRules.EatOutcome.ALREADY_HAS_POWER;
      } else {
         DevilFruit current = DFAttachments.data(player).fruit();
         FruitFx fx = current != null ? current.fx() : FruitFx.DEFAULT;
         ServerFx.send(player, fx.awaken(), player.position(), Vec3.ZERO, 2.0F, player.getRandom().nextLong(), 1);
         player.hurt(DFDamageTypes.secondFruit(player.level()), Float.MAX_VALUE);
         return FruitRules.EatOutcome.KILLED;
      }
   }

   public static void grantPower(ServerPlayer player, DevilFruit fruit, boolean dramatic) {
      PlayerFruitData data = DFAttachments.data(player);
      if (data.hasFruit() && !fruit.id().equals(data.fruitId())) {
         removePower(player);
      }

      boolean keep = (Boolean)CoreConfig.KEEP_MASTERY_ON_LOSS.get() && fruit.id().equals(data.masteryFruit());
      if (!data.hasFruit() && !keep) {
         data.setMastery(1);
         data.setMasteryXp(0.0F);
      }

      data.setFruit(fruit.id());
      data.setMastery(data.mastery());
      data.setEnergy((float)Mastery.maxEnergy(fruit, data.mastery()));
      data.clearCooldowns();
      if ((Boolean)CoreConfig.ONE_PER_WORLD.get()) {
         WorldFruitData.get(player.server).setEaten(fruit, player.getUUID(), player.getGameProfile().getName());
      }

      DFTriggers.ATE_FRUIT.get().trigger(player, fruit.id());
      if (data.mastery() >= 10) {
         DFTriggers.MAX_MASTERY.get().trigger(player, fruit.id());
      }

      FormManager.applyAttributes(player, fruit.baseForm());
      player.refreshDimensions();
      if (dramatic) {
         ServerFx.send(player, fruit.fx().awaken(), player.position(), Vec3.ZERO, 1.0F, player.getRandom().nextLong(), 0);
         player.displayClientMessage(fruit.message("awakened").withStyle(new ChatFormatting[]{ChatFormatting.AQUA, ChatFormatting.BOLD}), true);
         player.sendSystemMessage(fruit.message("awakened_chat").withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}));
      }

      if (dramatic && fruit.isLogia()) {
         LogiaManager.awaken(player);
      }

      DFNetwork.sendTo(player, AbilityTablePayload.of(fruit));
      ServerFx.formState(player);
      CoreEvents.syncNow(player);
   }

   public static void onPowerRemoved(BiConsumer<ServerPlayer, DevilFruit> listener) {
      POWER_REMOVED.add(listener);
   }

   public static void removePower(ServerPlayer player) {
      PlayerFruitData data = DFAttachments.data(player);
      DevilFruit fruit = data.fruit();
      if (fruit != null) {
         POWER_REMOVED.forEach(l -> l.accept(player, fruit));
      }

      AbilityManager.cancelAll(player);
      LogiaManager.onPowerRemoved(player);
      data.reset((Boolean)CoreConfig.KEEP_MASTERY_ON_LOSS.get());
      FormManager.clearAttributes(player);
      player.refreshDimensions();
      if (fruit != null && (Boolean)CoreConfig.ONE_PER_WORLD.get()) {
         WorldFruitData world = WorldFruitData.get(player.server);
         if (world.owner(fruit).filter(player.getUUID()::equals).isPresent()) {
            world.setFree(fruit);
         }
      }

      DFNetwork.sendTo(player, AbilityTablePayload.of(null));
      ServerFx.formState(player);
      CoreEvents.syncNow(player);
   }

   public static boolean keepsFruit(ServerPlayer player, DamageSource source) {
      return DFGameRules.keepFruit(player) && !source.is(DFDamageTypes.SECOND_FRUIT);
   }

   public static void onDeathIntercept(FruitRules.DeathInterceptor interceptor) {
      DEATH_INTERCEPTORS.add(interceptor);
   }

   public static boolean interceptDeath(ServerPlayer player, DamageSource source) {
      DevilFruit fruit = DFAttachments.data(player).fruit();
      if (fruit != null && !source.is(DFDamageTypes.SECOND_FRUIT)) {
         for (FruitRules.DeathInterceptor interceptor : DEATH_INTERCEPTORS) {
            if (interceptor.intercept(player, fruit, source)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static void keepOnDeath(ServerPlayer player) {
      PlayerFruitData data = DFAttachments.data(player);
      DevilFruit fruit = data.fruit();
      if (fruit != null) {
         data.setEnergy((float)Mastery.maxEnergy(fruit, data.mastery()));
         data.clearCooldowns();
      }
   }

   public static void onUserDeath(ServerPlayer player) {
      DevilFruit fruit = DFAttachments.data(player).fruit();
      if ((Boolean)CoreConfig.LOSE_ON_DEATH.get() && fruit != null) {
         BlockPos deathPos = player.blockPosition();
         ServerLevel level = player.serverLevel();
         removePower(player);
         player.sendSystemMessage(fruit.message("power_lost").withStyle(new ChatFormatting[]{ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC}));
         if (fruit.reincarnates()) {
            reincarnate(level, deathPos, fruit);
         }
      }
   }

   public static void reincarnate(ServerLevel level, BlockPos origin, DevilFruit fruit) {
      CoreConfig.ReincarnationMode mode = (CoreConfig.ReincarnationMode)CoreConfig.REINCARNATION_MODE.get();
      if (mode != CoreConfig.ReincarnationMode.LOOT_POOL && fruit.hasItem()) {
         ItemStack item = forceCreateWorldFruit(level.getServer(), fruit);
         if (mode == CoreConfig.ReincarnationMode.NEARBY_CHEST) {
            BlockPos chest = insertIntoNearbyContainer(level, origin, (Integer)CoreConfig.REINCARNATION_RADIUS.get(), item);
            if (chest != null) {
               ServerFx.sendNear(level, fruit.fx().reincarnate(), -1, Vec3.atCenterOf(chest), Vec3.ZERO, 1.0F, 0L, 0, 48.0);
               DevilFruits.LOGGER.debug("{} reincarnated into container at {}", fruit.id(), chest);
               return;
            }
         }

         Vec3 at = Vec3.atCenterOf(origin);
         ItemEntity entity = new ItemEntity(level, at.x, at.y + 0.5, at.z, item, 0.0, 0.25, 0.0);
         entity.setPickUpDelay(40);
         entity.setUnlimitedLifetime();
         level.addFreshEntity(entity);
         ServerFx.sendNear(level, fruit.fx().reincarnate(), -1, at, Vec3.ZERO, 1.0F, 0L, 0, 48.0);
      } else {
         if ((Boolean)CoreConfig.ONE_PER_WORLD.get()) {
            WorldFruitData.get(level.getServer()).setFree(fruit);
         }
      }
   }

   @Nullable
   private static BlockPos insertIntoNearbyContainer(ServerLevel level, BlockPos origin, int radius, ItemStack fruit) {
      List<BlockEntity> candidates = new ArrayList<>();
      ChunkPos center = new ChunkPos(origin);
      int chunkRadius = Mth.ceil((double)radius / 16.0);
      double r2 = (double)radius * (double)radius;

      for (int cx = center.x - chunkRadius; cx <= center.x + chunkRadius; cx++) {
         for (int cz = center.z - chunkRadius; cz <= center.z + chunkRadius; cz++) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
            if (chunk != null) {
               for (BlockEntity be : chunk.getBlockEntities().values()) {
                  if ((be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity) && be.getBlockPos().distSqr(origin) <= r2) {
                     candidates.add(be);
                  }
               }
            }
         }
      }

      Collections.shuffle(candidates, new Random(level.getRandom().nextLong()));

      for (BlockEntity bex : candidates) {
         Container container = (Container)bex;
         List<Integer> free = new ArrayList<>();

         for (int i = 0; i < container.getContainerSize(); i++) {
            if (container.getItem(i).isEmpty()) {
               free.add(i);
            }
         }

         if (!free.isEmpty()) {
            container.setItem(free.get(level.getRandom().nextInt(free.size())), fruit);
            bex.setChanged();
            return bex.getBlockPos();
         }
      }

      return null;
   }

   public static void onFruitItemDestroyed(MinecraftServer server, ItemStack stack) {
      UUID id = (UUID)stack.get((DataComponentType)DFDataComponents.FRUIT_INSTANCE.get());
      DevilFruit fruit = FruitRegistry.byItem(stack.getItem());
      if (id != null && fruit != null && (Boolean)CoreConfig.ONE_PER_WORLD.get()) {
         WorldFruitData data = WorldFruitData.get(server);
         if (data.state(fruit) == WorldFruitData.State.ITEM && data.instance(fruit).filter(id::equals).isPresent()) {
            data.setFree(fruit);
         }
      }
   }

   private FruitRules() {
   }

   @FunctionalInterface
   public interface DeathInterceptor {
      boolean intercept(ServerPlayer var1, DevilFruit var2, DamageSource var3);
   }

   public static enum EatOutcome {
      GRANTED,
      KILLED,
      ALREADY_HAS_POWER,
      POWER_TAKEN,
      DISABLED;
   }
}
