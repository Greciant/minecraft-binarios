package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.client.screen.RadialWheelScreen;
import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.exosavant.ExoPiece;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.ExoVisual;
import com.awakenedmod.awakened.core.net.ExoDeployPayload;
import com.awakenedmod.awakened.core.net.ExoFailsafePayload;
import com.awakenedmod.awakened.core.net.ExoFlightPrefPayload;
import com.awakenedmod.awakened.core.net.ExoVisorPayload;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ExoWheel {
   private static final int COLOR = -11549705;

   private ExoWheel() {
   }

   public static RadialWheelScreen create() {
      ExoVisual vis = (ExoVisual)Minecraft.getInstance().player.getData(AwakenedAttachments.EXO_VISUAL);
      int mastery = ClientPowerState.get().masteryLevel();
      boolean suited = vis.suited();
      boolean helmet = (vis.builtMask() & ExoPiece.HELMET.bit()) != 0;
      boolean chip = vis.chipInstalled();
      boolean maskLocked = !suited || !helmet;
      RadialWheelScreen.RadialOption mask = new RadialWheelScreen.RadialOption(
         null,
         -11549705,
         Component.translatable(vis.visorOpen() ? "awakened.exo_wheel.mask_close" : "awakened.exo_wheel.mask_open"),
         maskLocked,
         maskLocked ? Component.translatable("awakened.exo_wheel.need_helmet") : null,
         vis.visorOpen(),
         0,
         null,
         true,
         new ItemStack((ItemLike)AwakenedItems.EXOSAVANT_HELMET.get())
      );
      boolean fsLocked = !chip || mastery < 13;
      RadialWheelScreen.RadialOption failsafe = new RadialWheelScreen.RadialOption(
         null,
         -11549705,
         Component.translatable(vis.failsafeArmed() ? "awakened.exo_wheel.failsafe_on" : "awakened.exo_wheel.failsafe_off"),
         fsLocked,
         !fsLocked ? null : (!chip ? Component.translatable("awakened.exo_wheel.need_chip") : Component.literal("Lv 13")),
         vis.failsafeArmed(),
         1,
         null,
         true,
         new ItemStack(Items.TOTEM_OF_UNDYING)
      );
      boolean summonLocked = !suited && !chip;
      RadialWheelScreen.RadialOption summon = new RadialWheelScreen.RadialOption(
         null,
         -11549705,
         Component.translatable(suited ? "awakened.exo_wheel.recall" : "awakened.exo_wheel.summon"),
         summonLocked,
         summonLocked ? Component.translatable("awakened.exo_wheel.need_chip") : null,
         false,
         2,
         null,
         true,
         new ItemStack((ItemLike)AwakenedItems.EXOSAVANT_CHESTPLATE.get())
      );
      boolean flightLocked = mastery < 8;
      boolean defaultJet = ExoClientState.defaultJet();
      RadialWheelScreen.RadialOption flight = new RadialWheelScreen.RadialOption(
         null,
         -11549705,
         Component.translatable(defaultJet ? "awakened.exo_wheel.flight_jet" : "awakened.exo_wheel.flight_hover"),
         flightLocked,
         flightLocked ? Component.literal("Lv 8") : null,
         defaultJet,
         3,
         null,
         true,
         new ItemStack(defaultJet ? Items.FIREWORK_ROCKET : Items.FEATHER)
      );
      return new RadialWheelScreen(
         Component.translatable("awakened.exo_wheel.title"), List.of(mask, failsafe, summon, flight), ExoWheel::onSelect, AwakenedKeybinds.SECONDARY_ABILITY
      );
   }

   private static void onSelect(int index) {
      switch (index) {
         case 0:
            send(ExoVisorPayload.INSTANCE);
            break;
         case 1:
            send(ExoFailsafePayload.INSTANCE);
            break;
         case 2:
            send(ExoDeployPayload.INSTANCE);
            break;
         case 3:
            toggleDefaultFlight();
      }
   }

   public static void toggleDefaultFlight() {
      ExoClientState.setDefaultJet(!ExoClientState.defaultJet());
      send(ExoFlightPrefPayload.INSTANCE);
   }

   static void send(CustomPacketPayload payload) {
      PacketDistributor.sendToServer(payload, new CustomPacketPayload[0]);
   }
}
