package com.awakenedmod.awakened.content.exosavant;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.ExoGear;
import com.awakenedmod.awakened.core.data.PowerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public final class ExoGearDrops {
   private ExoGearDrops() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(EventPriority.HIGHEST, ExoGearDrops::onDeath);
   }

   private static void onDeath(LivingDeathEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         PowerData var3 = PowerService.data(player);
         if ("exosavant".equals(var3.powerId())) {
            if (!keepInventory(player)) {
               if (ExoLoadout.chipInstalled(player)) {
                  secureSuit(player);
               }
            }
         }
      }
   }

   private static void secureSuit(ServerPlayer player) {
      ExoSuit.setStowed(player, true);
      ExoGear store = ExoGear.get(player).copy();
      boolean changed = false;

      for (ExoPiece piece : ExoPiece.values()) {
         if (!store.get(piece.ordinal()).is(piece.item())) {
            ItemStack chosen = ItemStack.EMPTY;
            EquipmentSlot slot = piece.armorSlot();
            if (slot != null) {
               ItemStack worn = player.getItemBySlot(slot);
               if (worn.is(piece.item())) {
                  chosen = worn.copy();
                  player.setItemSlot(slot, ItemStack.EMPTY);
               }
            } else {
               chosen = ExoCurios.take(player, piece);
            }

            if (chosen.isEmpty()) {
               chosen = ExoSuit.takeFromInventory(player, piece);
            }

            if (!chosen.isEmpty()) {
               store.with(piece.ordinal(), chosen);
               changed = true;
            }
         }
      }

      if (changed) {
         ExoGear.write(player, store);
      }
   }

   private static boolean keepInventory(ServerPlayer player) {
      return player.serverLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
   }
}
