package com.devilfruits.core.poneglyph;

import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

@EventBusSubscriber(
   modid = "devilfruits"
)
public final class PoneglyphEvents {
   @SubscribeEvent
   public static void onReloadListeners(AddReloadListenerEvent event) {
      event.addListener(PoneglyphData.Loader.poneglyphs());
      event.addListener(PoneglyphData.Loader.knowledge());
   }

   @SubscribeEvent
   public static void onDatapackSync(OnDatapackSyncEvent event) {
      event.getRelevantPlayers().forEach(player -> {
         PoneglyphNetwork.sync(player);
         Knowledge.reaward(player);
      });
   }

   @SubscribeEvent
   public static void onRightClickBlock(RightClickBlock event) {
      if (RubbingService.holdsTools(event.getEntity()) && event.getLevel().getBlockState(event.getPos()).is((Block)Poneglyphs.PONEGLYPH_FACE.get())) {
         event.setCanceled(true);
         event.setCancellationResult(InteractionResult.CONSUME);
      }
   }

   @SubscribeEvent
   public static void onServerTick(Post event) {
      RubbingService.tick(event.getServer());
   }

   @SubscribeEvent
   public static void onDamaged(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
      if (event.getEntity() instanceof ServerPlayer player && event.getNewDamage() > 0.0F) {
         RubbingService.cancel(player);
      }
   }

   @SubscribeEvent
   public static void onLogout(PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         RubbingService.cancel(player);
      }
   }

   @SubscribeEvent
   public static void onVillagerTrades(VillagerTradesEvent event) {
      if (event.getType() == VillagerProfession.LIBRARIAN) {
         ((List)event.getTrades().get(3))
            .add(
               (trader, random) -> !PoneglyphConfig.LIBRARIAN_TRADE.get()
                     ? null
                     : new MerchantOffer(
                        new ItemCost(Items.EMERALD, (Integer)PoneglyphConfig.LIBRARIAN_PRICE.get()),
                        Optional.of(new ItemCost(Items.BOOK)),
                        new ItemStack((ItemLike)Poneglyphs.PRIMER.get()),
                        2,
                        15,
                        0.05F
                     )
            );
      }
   }

   @SubscribeEvent
   public static void onRegisterCommands(RegisterCommandsEvent event) {
      PoneglyphCommand.register(event.getDispatcher());
   }

   private PoneglyphEvents() {
   }
}
