package com.devilfruits.core.gacha;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.config.CoreConfig;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.network.DFNetwork;
import com.devilfruits.core.rules.FruitRules;
import com.devilfruits.core.rules.WorldFruitData;
import com.devilfruits.registry.DFDataComponents;
import com.devilfruits.registry.DFItems;
import com.devilfruits.registry.DFTags;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;

@EventBusSubscriber(
   modid = "devilfruits"
)
public final class CasketOpening {
   private static final Map<UUID, CasketOpening.Pending> PENDING = new HashMap<>();

   public static boolean isKey(ItemStack stack) {
      return stack.is(DFTags.CASKET_KEYS);
   }

   @Nullable
   public static Component refusal(ServerPlayer player) {
      if (PENDING.containsKey(player.getUUID())) {
         return Component.translatable("message.devilfruits.casket.busy").withStyle(ChatFormatting.GRAY);
      } else {
         return !GachaRoll.rollable(GachaRoll.candidates(player.server), CoreConfig::gachaTierWeight)
            ? Component.translatable("message.devilfruits.casket.no_fruit").withStyle(ChatFormatting.GRAY)
            : null;
      }
   }

   public static CasketOpening.Outcome open(ServerPlayer player, ItemStack casket) {
      if (casket.is((Item)DFItems.CASKET.get()) && player.getMainHandItem() == casket) {
         ItemStack key = player.getOffhandItem();
         if (!isKey(key)) {
            player.displayClientMessage(Component.translatable("message.devilfruits.casket.need_key").withStyle(ChatFormatting.GRAY), true);
            return CasketOpening.Outcome.NO_KEY;
         } else if (PENDING.containsKey(player.getUUID())) {
            player.displayClientMessage(Component.translatable("message.devilfruits.casket.busy").withStyle(ChatFormatting.GRAY), true);
            return CasketOpening.Outcome.BUSY;
         } else {
            MinecraftServer server = player.server;
            Optional<GachaRoll.Candidate<DevilFruit>> rolled = GachaRoll.roll(server, player.getRandom());
            ItemStack fruit = rolled.<ItemStack>map(c -> FruitRules.createWorldFruit(server, c.value())).orElse(ItemStack.EMPTY);
            if (fruit.isEmpty()) {
               player.displayClientMessage(Component.translatable("message.devilfruits.casket.no_fruit").withStyle(ChatFormatting.GRAY), true);
               return CasketOpening.Outcome.NO_FRUIT;
            } else {
               GachaRoll.Candidate<DevilFruit> result = rolled.get();
               if (!player.getAbilities().instabuild) {
                  casket.shrink(1);
                  if (key.getItem() instanceof CasketKey own) {
                     own.onCasketOpened(player, key);
                  } else if ((Boolean)CoreConfig.CASKET_KEY_CONSUMED.get()) {
                     key.shrink(1);
                  }
               }

               player.awardStat(Stats.ITEM_USED.get((Item)DFItems.CASKET.get()));
               int revealTicks = revealTicks();
               PENDING.put(player.getUUID(), new CasketOpening.Pending(fruit, result.value(), result.tier(), (long)(server.getTickCount() + revealTicks)));
               reveal(
                  player,
                  new CasketRevealPayload(
                     player.getId(),
                     result.tier(),
                     result.value().id(),
                     player.getRandom().nextLong(),
                     revealTicks,
                     (Boolean)CoreConfig.CASKET_CINEMATIC.get(),
                     false
                  )
               );
               DevilFruits.LOGGER.info("{} opened a casket: {} ({})", new Object[]{player.getGameProfile().getName(), result.value().id(), result.tier()});
               return CasketOpening.Outcome.OPENED;
            }
         }
      } else {
         return CasketOpening.Outcome.NO_CASKET;
      }
   }

   public static int revealTicks() {
      return Math.max(1, (int)Math.round((Double)CoreConfig.CASKET_REVEAL_SECONDS.get() * 20.0));
   }

   public static void reveal(ServerPlayer player, CasketRevealPayload payload) {
      for (ServerPlayer other : player.serverLevel().players()) {
         if (other == player || other.distanceToSqr(player) < 9216.0) {
            DFNetwork.sendTo(other, payload);
         }
      }
   }

   public static boolean pending(ServerPlayer player) {
      return PENDING.containsKey(player.getUUID());
   }

   public static void deliverNow(ServerPlayer player) {
      CasketOpening.Pending pending = PENDING.remove(player.getUUID());
      if (pending != null) {
         ItemStack stack = pending.fruit();
         if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
         }

         player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.4F, 1.4F);
         Component name = pending.definition().displayName().withStyle(pending.tier().color());
         Component tier = Component.translatable(pending.tier().translationKey()).withStyle(pending.tier().color());
         player.sendSystemMessage(Component.translatable("message.devilfruits.casket.found", new Object[]{name, tier}));
         if (CoreConfig.announces(pending.tier())) {
            Component line = Component.translatable("message.devilfruits.casket.announce", new Object[]{player.getDisplayName(), name, tier})
               .withStyle(ChatFormatting.GRAY);

            for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
               if (other != player) {
                  other.sendSystemMessage(line);
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onServerTick(Post event) {
      if (!PENDING.isEmpty()) {
         MinecraftServer server = event.getServer();
         long now = (long)server.getTickCount();

         for (UUID id : new ArrayList<>(PENDING.keySet())) {
            CasketOpening.Pending pending = PENDING.get(id);
            if (pending.deliverAt() <= now) {
               ServerPlayer player = server.getPlayerList().getPlayer(id);
               if (player != null) {
                  deliverNow(player);
               } else {
                  orphan(server, id);
               }
            }
         }
      }
   }

   private static void orphan(MinecraftServer server, UUID id) {
      CasketOpening.Pending pending = PENDING.remove(id);
      if (pending != null && (Boolean)CoreConfig.ONE_PER_WORLD.get()) {
         WorldFruitData world = WorldFruitData.get(server);
         UUID instance = (UUID)pending.fruit().get((DataComponentType)DFDataComponents.FRUIT_INSTANCE.get());
         if (world.state(pending.definition()) == WorldFruitData.State.ITEM && world.instance(pending.definition()).filter(i -> i.equals(instance)).isPresent()
            )
          {
            world.setFree(pending.definition());
         }

         DevilFruits.LOGGER.warn("Casket fruit {} of an absent player {} went back to the pool", pending.definition().id(), id);
      }
   }

   @SubscribeEvent
   public static void onLogout(PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         deliverNow(player);
      }
   }

   @SubscribeEvent
   public static void onChangedDimension(PlayerChangedDimensionEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         deliverNow(player);
      }
   }

   @SubscribeEvent
   public static void onDeath(LivingDeathEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         deliverNow(player);
      }
   }

   @SubscribeEvent
   public static void onDamage(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
      if (event.getNewDamage() > 0.0F
         && event.getEntity() instanceof ServerPlayer player
         && player.isUsingItem()
         && player.getUsedItemHand() == InteractionHand.MAIN_HAND
         && player.getUseItem().is((Item)DFItems.CASKET.get())) {
         player.stopUsingItem();
      }
   }

   @SubscribeEvent
   public static void onServerStopping(ServerStoppingEvent event) {
      for (UUID id : new ArrayList<>(PENDING.keySet())) {
         ServerPlayer player = event.getServer().getPlayerList().getPlayer(id);
         if (player != null) {
            deliverNow(player);
         } else {
            orphan(event.getServer(), id);
         }
      }
   }

   @SubscribeEvent
   public static void onServerStopped(ServerStoppedEvent event) {
      PENDING.clear();
   }

   private CasketOpening() {
   }

   public static enum Outcome {
      OPENED,
      NO_CASKET,
      NO_KEY,
      BUSY,
      NO_FRUIT;
   }

   private static record Pending(ItemStack fruit, DevilFruit definition, GachaTier tier, long deliverAt) {
   }
}
