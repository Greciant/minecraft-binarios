package com.awakenedmod.awakened.content.exosavant;

import com.awakenedmod.awakened.core.PowerService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract;

public final class ExoSkinUnlocks {
   private ExoSkinUnlocks() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(EventPriority.LOWEST, ExoSkinUnlocks::onEntityInteract);
      gameBus.addListener(EventPriority.LOWEST, ExoSkinUnlocks::onDeath);
   }

   public static void tick(ServerPlayer player) {
      if (player.tickCount % 20 == 0 && ExoLoadout.equippedCount(player) == ExoPiece.WORN_COUNT && ExoLoadout.chipInstalled(player)) {
         ExoSkins.unlock(player, ExoSkin.SLIM);
      }
   }

   private static void onEntityInteract(EntityInteract event) {
      if (!event.isCanceled()
         && event.getEntity() instanceof ServerPlayer player
         && event.getTarget() instanceof Axolotl axolotl
         && axolotl.isAlive()
         && event.getItemStack().is(Items.WATER_BUCKET)
         && "exosavant".equals(PowerService.data(player).powerId())) {
         ExoSkins.unlock(player, ExoSkin.AXOLOTL);
      }
   }

   private static void onDeath(LivingDeathEvent event) {
      if (!event.isCanceled()
         && event.getEntity() instanceof Warden
         && event.getSource().getEntity() instanceof ServerPlayer player
         && ExoSuit.isSuited(player)) {
         ExoSkins.unlock(player, ExoSkin.SCULK);
      }
   }
}
