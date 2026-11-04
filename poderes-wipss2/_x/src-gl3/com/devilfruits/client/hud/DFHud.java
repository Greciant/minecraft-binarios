package com.devilfruits.client.hud;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.ClientFruitState;
import com.devilfruits.client.camera.ShoulderCamera;
import com.devilfruits.core.config.CoreClientConfig;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.FruitTheme;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public final class DFHud {
   public static final ResourceLocation BAR = DevilFruits.id("hud/ability_bar");
   public static final ResourceLocation BAR_VERTICAL = DevilFruits.id("hud/ability_bar_vertical");
   public static final ResourceLocation LOCK = DevilFruits.id("hud/lock");
   public static final ResourceLocation SUPPRESSED = DevilFruits.id("hud/suppressed");
   public static final ResourceLocation FORM_FRAME = DevilFruits.id("hud/form_frame");
   public static final ResourceLocation SLOT = DevilFruits.id("screen/slot");
   public static final ResourceLocation PANEL = DevilFruits.id("screen/panel");
   public static final ResourceLocation FLASH_TEXTURE = DevilFruits.id("textures/misc/screen_flash.png");
   public static final int BAR_TEXTURE_SLOTS = 10;
   public static final int BAR_TEXTURE_LENGTH = 202;
   public static final int ENERGY_LENGTH = 142;

   public static void registerLayers(RegisterGuiLayersEvent event) {
      event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, DevilFruits.id("screen_flash"), FlashLayer::render);
      event.registerAbove(VanillaGuiLayers.CROSSHAIR, DevilFruits.id("shoulder_crosshair"), ShoulderCamera::renderCrosshair);
      event.registerAbove(VanillaGuiLayers.CROSSHAIR, DevilFruits.id("charge_indicator"), ChargeIndicatorLayer::render);
      event.registerAbove(VanillaGuiLayers.HOTBAR, DevilFruits.id("ability_bar"), AbilityBarLayer::render);
   }

   public static boolean hidden() {
      return (Boolean)CoreClientConfig.HIDE_HUD.get();
   }

   public static int barLength(int slots) {
      return Math.max(1, slots) * 20 + 2;
   }

   public static FruitTheme theme() {
      DevilFruit fruit = ClientFruitState.fruit();
      return fruit != null ? fruit.theme() : FruitTheme.DEFAULT;
   }

   private DFHud() {
   }
}
