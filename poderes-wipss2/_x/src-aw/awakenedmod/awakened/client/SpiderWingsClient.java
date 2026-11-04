package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.spider.SpiderVisualState;
import com.awakenedmod.awakened.content.spider.WebWingsAbility;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.event.entity.EntityEvent.Size;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem;

@EventBusSubscriber(
   modid = "awakened",
   value = {Dist.CLIENT}
)
public final class SpiderWingsClient {
   private SpiderWingsClient() {
   }

   @SubscribeEvent
   static void onFireworkBoost(RightClickItem event) {
      if (event.getLevel().isClientSide() && event.getEntity() == Minecraft.getInstance().player) {
         if (event.getItemStack().is(Items.FIREWORK_ROCKET) && SpiderSwingController.gliding()) {
            SpiderSwingController.fireworkBoost();
         }
      }
   }

   @SubscribeEvent
   static void onEntitySize(Size event) {
      if (event.getEntity() instanceof Player p && p.level() != null && p.level().isClientSide() && isGlidingClient(p)) {
         event.setNewSize(WebWingsAbility.glideDimensions(p));
      }
   }

   @SubscribeEvent
   static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null) {
         for (Player player : mc.level.players()) {
            EntityDimensions want = isGlidingClient(player) ? WebWingsAbility.glideDimensions(player) : player.getDimensions(player.getPose());
            if (player.getBbHeight() != want.height() || player.getEyeHeight() != want.eyeHeight()) {
               player.refreshDimensions();
            }
         }
      }
   }

   private static boolean isGlidingClient(Player player) {
      return player == Minecraft.getInstance().player ? SpiderSwingController.gliding() : SpiderVisualState.get(player).gliding();
   }
}
