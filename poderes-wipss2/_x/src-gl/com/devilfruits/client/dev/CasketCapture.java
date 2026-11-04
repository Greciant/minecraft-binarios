package com.devilfruits.client.dev;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.config.CoreClientConfig;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.FruitRegistry;
import com.devilfruits.core.gacha.CasketItem;
import com.devilfruits.core.rules.WorldFruitData;
import com.devilfruits.registry.DFAttachments;
import com.devilfruits.registry.DFItems;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public final class CasketCapture extends CaptureStage {
   private static final int RATE = 8;
   private static final String[][] TIERS = new String[][]{
      {"common", "devilfruits:bomu_bomu"}, {"uncommon", "devilfruits:gura_gura"}, {"rare", "devilfruits:suna_suna"}, {"legendary", "devilfruits:uo_uo_seiryu"}
   };
   private static final int[] BEATS = new int[]{4, 10, 13, 16, 20, 26, 32, 36, 38, 40, 43, 47, 52, 57};
   private final boolean look;

   public CasketCapture(boolean look) {
      super("vfx_casket_");
      this.look = look;
   }

   @Override
   protected ResourceLocation fruit() {
      return DevilFruits.id("kilo_kilo");
   }

   private static Consumer<ServerPlayer> hands() {
      return p -> {
         for (DevilFruit fruit : FruitRegistry.playable()) {
            if (!fruit.id().equals(DFAttachments.data(p).fruitId())) {
               WorldFruitData.get(p.server).setFree(fruit);
            }
         }

         p.getInventory().clearContent();
         p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack((ItemLike)DFItems.CASKET.get()));
         p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack((ItemLike)DFItems.GOLD_KEY.get()));
      };
   }

   private static Consumer<ServerPlayer> empty() {
      return p -> p.getInventory().clearContent();
   }

   private static Consumer<Minecraft> clean(Consumer<Minecraft> camera, boolean hide) {
      return mc -> {
         mc.options.hideGui = hide;
         camera.accept(mc);
      };
   }

   private static Consumer<ServerPlayer> preview(String tier, String fruit) {
      return p -> command(p, "devilfruit casket preview " + tier + " " + fruit);
   }

   private int unlock(AutoTest s, int t, Consumer<Minecraft> camera, boolean hide, String name, int every) {
      int use = CasketItem.useTicks();
      return this.take(s, t, 8, hands(), 10, none(), clean(camera, hide), use + 66, c -> {
         s.add(c, mc -> mc.options.keyUse.setDown(true));
         s.add(c + use + 16, mc -> mc.options.keyUse.setDown(false));
         int k = 2;

         while (k <= use + 60) {
            this.shot(s, c + k, name + String.format("%03d", k));
            k += every;
         }
      });
   }

   @Override
   protected int takes(AutoTest s, int t) {
      t += 40;
      Consumer<Minecraft> side = rig(2.3, 140.0F, 10.0F, 0.5);
      Consumer<Minecraft> wide = clean(rig(3.3, 140.0F, 6.0F, 0.7), true);
      t = this.unlock(s, t, side, true, "unlock_", this.look ? 8 : 4);
      t = this.unlock(s, t, firstPerson(), false, "unlock_fp_", 8);

      for (String[] tier : TIERS) {
         t = this.take(s, t, 8, empty(), 0, preview(tier[0], tier[1]), wide, 64, c -> {
            for (int k : BEATS) {
               this.shot(s, c + k, tier[0] + String.format("_%03d", k));
            }
         });
      }

      t = this.take(s, t, 8, empty(), 0, preview("legendary", "devilfruits:uo_uo_seiryu"), clean(firstPerson(), false), 64, c -> {
         for (int k : new int[]{14, 26, 36, 39, 44, 54}) {
            this.shot(s, c + k, String.format("legendary_fp_%03d", k));
         }
      });
      if (this.look) {
         return t;
      } else {
         for (String[] tier : TIERS) {
            t = this.take(
               s,
               t,
               8,
               empty(),
               0,
               preview(tier[0], tier[1]),
               wide,
               66,
               c -> s.add(c - 1, mc -> VfxCapture.clip(this.prefix + "clip_" + tier[0], 96, 0.6666666666666666))
            );
         }

         s.add(t, mc -> quality(CoreClientConfig.CinematicQuality.LOW));
         t = this.take(s, t + 2, 8, empty(), 0, preview("legendary", "devilfruits:uo_uo_seiryu"), wide, 64, c -> {
            for (int k : new int[]{14, 26, 38, 44}) {
               this.shot(s, c + k, String.format("low_legendary_%03d", k));
            }
         });
         s.add(t, mc -> quality(CoreClientConfig.CinematicQuality.ULTRA));
         return t + 2;
      }
   }
}
