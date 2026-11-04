package com.devilfruits.core.haki;

import com.devilfruits.registry.DFItems;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

@EventBusSubscriber(
   modid = "devilfruits"
)
public final class HakiEvents {
   @SubscribeEvent
   public static void onPlayerTick(Post event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         HakiManager.tick(player);
      }
   }

   @SubscribeEvent
   public static void onVillagerTrades(VillagerTradesEvent event) {
      if (event.getType() == VillagerProfession.LIBRARIAN) {
         ((List)event.getTrades().get(5))
            .add(
               (trader, random) -> !HakiConfig.LIBRARIAN_TRADE.get()
                     ? null
                     : new MerchantOffer(
                        new ItemCost(Items.EMERALD, (Integer)HakiConfig.LIBRARIAN_PRICE.get()),
                        Optional.of(new ItemCost(Items.BOOK)),
                        new ItemStack((ItemLike)DFItems.HAKI_SCROLL.get()),
                        3,
                        30,
                        0.05F
                     )
            );
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOW
   )
   public static void onIncomingDamage(LivingIncomingDamageEvent event) {
      HakiCombat.onIncomingDamage(event);
   }

   @SubscribeEvent
   public static void onDamaged(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
      if (event.getNewDamage() > 0.0F && event.getSource().getEntity() instanceof ServerPlayer attacker && attacker != event.getEntity()) {
         HakiTraining.onFistHit(attacker, event.getEntity(), event.getSource());
      }
   }

   @SubscribeEvent
   public static void onDeath(LivingDeathEvent event) {
      if (event.getSource().getEntity() instanceof ServerPlayer killer && killer != event.getEntity()) {
         HakiTraining.onKill(killer, event.getEntity(), event.getSource());
      }

      if (event.getEntity() instanceof ServerPlayer player && HakiManager.unlocked(player)) {
         HakiManager.coatOff(player, false);
      }
   }

   @SubscribeEvent
   public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
      MobSpawnType type = event.getSpawnType();
      if (type == MobSpawnType.SPAWNER || type == MobSpawnType.TRIAL_SPAWNER) {
         event.getEntity().getPersistentData().putBoolean("devilfruits_spawner", true);
      }
   }

   @SubscribeEvent
   public static void onLogin(PlayerLoggedInEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && HakiManager.unlocked(player)) {
         HakiManager.data(player).setCoat(false, 0);
         HakiManager.sync(player);
      }
   }

   @SubscribeEvent
   public static void onRespawn(PlayerRespawnEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && HakiManager.unlocked(player)) {
         HakiManager.data(player).setCoat(false, 0);
         HakiManager.sync(player);
      }
   }

   private HakiEvents() {
   }
}
