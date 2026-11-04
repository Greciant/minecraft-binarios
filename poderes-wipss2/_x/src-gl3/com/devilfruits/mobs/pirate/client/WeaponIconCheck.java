package com.devilfruits.mobs.pirate.client;

import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.dev.CaptureStage;
import com.devilfruits.client.dev.VfxCapture;
import com.devilfruits.core.seastone.SeastoneGear;
import com.devilfruits.fruits.gura_gura.GuraItems;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuItems;
import com.devilfruits.mobs.pirate.Pirates;
import com.devilfruits.mobs.pirate.item.FlintlockItem;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public final class WeaponIconCheck implements AutoTest.Script {
   private static ItemStack of(ItemLike item, boolean loaded) {
      ItemStack s = new ItemStack(item);
      if (loaded) {
         FlintlockItem.setLoaded(s, true);
      }

      return s;
   }

   @Override
   public int write(AutoTest s, int t) {
      s.add(t, mc -> {
         VfxCapture.exactWindow(1920, 1080);
         mc.options.hideGui = false;
      });
      s.add(
         t + 2,
         mc -> s.server(
               mc,
               p -> {
                  CaptureStage.command(p, "gamemode survival");
                  p.getInventory().clearContent();
                  ItemStack[] row = new ItemStack[]{
                     of((ItemLike)SeastoneGear.JITTE.get(), false),
                     of((ItemLike)Pirates.CUTLASS.get(), false),
                     of((ItemLike)Pirates.CAPTAIN_CUTLASS.get(), false),
                     of((ItemLike)Pirates.FLINTLOCK_PISTOL.get(), false),
                     of((ItemLike)Pirates.FLINTLOCK_PISTOL.get(), true),
                     of((ItemLike)Pirates.MUSKET.get(), false),
                     of((ItemLike)Pirates.MUSKET.get(), true),
                     of((ItemLike)GuraItems.BISENTO.get(), false),
                     of((ItemLike)SeiryuItems.HASSAIKAI.get(), false)
                  };

                  for (int i = 0; i < row.length; i++) {
                     p.getInventory().setItem(i, row[i]);
                     p.getInventory().setItem(9 + i, row[i].copy());
                  }

                  p.getInventory().setItem(18, new ItemStack((ItemLike)SeastoneGear.SWORD.get()));
                  p.getInventory().setItem(20, new ItemStack(Items.DIAMOND_SWORD));
                  p.getInventory().setItem(21, new ItemStack(Items.TRIDENT));
                  p.getInventory().selected = 8;
               }
            )
      );
      s.add(t + 20, mc -> s.shot(mc, "weapon_icons_hotbar"));
      s.add(t + 24, mc -> {
         if (mc.player != null) {
            mc.setScreen(new InventoryScreen(mc.player));
         }
      });
      s.add(t + 34, mc -> s.shot(mc, "weapon_icons_inventory"));
      s.add(t + 38, mc -> mc.setScreen(null));
      return t + 42;
   }
}
