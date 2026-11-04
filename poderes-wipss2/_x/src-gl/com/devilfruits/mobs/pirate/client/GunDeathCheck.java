package com.devilfruits.mobs.pirate.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.dev.CaptureStage;
import com.devilfruits.mobs.pirate.Pirates;
import com.devilfruits.mobs.pirate.item.FlintlockItem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public final class GunDeathCheck implements AutoTest.Script {
   private static void log(Minecraft mc, String what) {
      if (mc.player == null) {
         DevilFruits.LOGGER.info("[gun] {} (no player)", what);
      } else {
         ItemStack gun = mc.player.getMainHandItem();
         DevilFruits.LOGGER
            .info(
               "[gun] {} t{} alive {} using {} remaining {} loaded {} item {} cooldown {}",
               new Object[]{
                  what,
                  mc.player.tickCount,
                  mc.player.isAlive(),
                  mc.player.isUsingItem(),
                  mc.player.getUseItemRemainingTicks(),
                  FlintlockItem.isLoaded(gun),
                  gun.getItem(),
                  mc.player.getCooldowns().isOnCooldown(gun.getItem())
               }
            );
      }
   }

   @Override
   public int write(AutoTest s, int t) {
      s.add(t, mc -> s.server(mc, p -> {
            CaptureStage.command(p, "gamerule keepInventory true");
            CaptureStage.command(p, "gamerule doImmediateRespawn false");
            CaptureStage.command(p, "gamemode survival");
            p.getInventory().clearContent();
            p.getInventory().setItem(0, new ItemStack((ItemLike)Pirates.FLINTLOCK_PISTOL.get()));
            p.getInventory().setItem(1, new ItemStack((ItemLike)Pirates.MUSKET_BALL.get(), 8));
            p.getInventory().setItem(2, new ItemStack(Items.GUNPOWDER, 8));
            p.getInventory().selected = 0;
         }));
      s.add(t + 10, mc -> mc.options.keyUse.setDown(true));

      for (int k = 10; k <= 120; k += 10) {
         int kk = k;
         s.add(t + k, mc -> log(mc, "life1 reload +" + kk));
      }

      s.add(t + 120, mc -> mc.options.keyUse.setDown(false));
      s.add(t + 125, mc -> KeyMapping.click(mc.options.keyAttack.getKey()));
      s.add(t + 130, mc -> log(mc, "life1 after shot"));
      s.add(t + 150, mc -> mc.options.keyUse.setDown(true));
      s.add(t + 170, mc -> log(mc, "life1 mid reload"));
      s.add(t + 172, mc -> s.server(mc, p -> CaptureStage.command(p, "kill @s")));
      s.add(t + 180, mc -> log(mc, "dead"));
      s.add(t + 190, mc -> {
         mc.options.keyUse.setDown(false);
         if (mc.player != null) {
            mc.player.respawn();
         }
      });
      s.add(t + 220, mc -> {
         if (mc.screen != null) {
            mc.setScreen(null);
         }

         log(mc, "respawned");
      });
      s.add(t + 230, mc -> s.server(mc, p -> p.getInventory().selected = 0));
      s.add(t + 235, mc -> mc.options.keyUse.setDown(true));

      for (int k = 240; k <= 350; k += 10) {
         int kk = k;
         s.add(t + k, mc -> log(mc, "life2 reload +" + kk));
      }

      s.add(t + 350, mc -> mc.options.keyUse.setDown(false));
      s.add(t + 360, mc -> KeyMapping.click(mc.options.keyAttack.getKey()));
      s.add(t + 366, mc -> log(mc, "life2 after shot"));
      return t + 380;
   }
}
