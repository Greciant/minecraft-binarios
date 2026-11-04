package com.devilfruits.fruits.doru_doru.client.dev;

import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.dev.CaptureStage;
import com.devilfruits.core.gacha.CasketItem;
import com.devilfruits.fruits.doru_doru.DoruDoru;
import com.devilfruits.fruits.doru_doru.ability.WaxToolsAbility;
import com.devilfruits.fruits.doru_doru.client.WaxToolWheelScreen;
import com.devilfruits.fruits.doru_doru.item.WaxTools;
import com.devilfruits.registry.DFItems;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public final class WaxKeyCapture extends CaptureStage {
   public WaxKeyCapture() {
      super("vfx_waxkey_");
   }

   @Override
   protected ResourceLocation fruit() {
      return DoruDoru.ID;
   }

   private static Consumer<ServerPlayer> hands(boolean mold) {
      return p -> {
         command(p, "gamemode survival");
         p.getInventory().clearContent();
         if (mold) {
            p.getInventory().setItem(9, new ItemStack((ItemLike)DFItems.GOLD_KEY.get()));
         }
      };
   }

   private static Consumer<Minecraft> wheel() {
      return mc -> {
         mc.options.hideGui = false;
         CaptureStage.firstPerson().accept(mc);
         mc.setScreen(new WaxToolWheelScreen() {
            @Override
            public void tick() {
            }
         });
      };
   }

   @Override
   protected int takes(AutoTest s, int t) {
      t += 30;
      t = this.take(s, t, 20, hands(false), 4, none(), wheel(), 12, c -> {
         this.shot(s, c + 4, "wheel_plain");
         s.add(c + 8, mc -> mc.setScreen(null));
      });
      t = this.take(s, t, 20, hands(true), 4, none(), wheel(), 12, c -> {
         this.shot(s, c + 4, "wheel_key");
         s.add(c + 8, mc -> mc.setScreen(null));
      });
      int use = CasketItem.useTicks();
      Consumer<ServerPlayer> prep = hands(true).andThen(p -> {
         p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TORCH, 8));
         WaxToolsAbility.choose(p, WaxTools.KEY_CHOICE);
         p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack((ItemLike)DFItems.CASKET.get()));
      });
      Consumer<Minecraft> fp = mc -> {
         mc.options.hideGui = false;
         CaptureStage.firstPerson().accept(mc);
      };
      t = this.take(s, t, 8, prep, 10, none(), fp, use + 70, c -> {
         this.shot(s, c - 2, "held_fp");
         s.add(c, mc -> mc.options.keyUse.setDown(true));
         s.add(c + use + 16, mc -> mc.options.keyUse.setDown(false));
         this.shot(s, c + use / 2, "unlock_fp");
         this.shot(s, c + use + 30, "reveal_fp");
         this.shot(s, c + use + 68, "after_fp");
      });
      t = this.take(s, t, 8, prep, 10, none(), CaptureStage.rig(2.3, 140.0F, 10.0F, 0.5), 20, c -> this.shot(s, c + 2, "held_side"));
      s.add(t, mc -> s.server(mc, p -> command(p, "gamemode creative")));
      return t + 2;
   }
}
