package com.awakenedmod.awakened.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;

public final class StoneMechControlsHud extends ControlsHud {
   public static final StoneMechControlsHud CONTROLS = new StoneMechControlsHud();

   private StoneMechControlsHud() {
   }

   @Override
   public String hudId() {
      return "stone_mech_controls";
   }

   @Override
   public HudSettings hudSettings() {
      return ClientConfig.HUD_STONE_MECH_CONTROLS;
   }

   @Override
   public boolean hudRelevant() {
      return "geokinetic".equals(ClientPowerState.get().powerId()) || StoneMechPilot.isActive();
   }

   @Override
   protected boolean showing() {
      return StoneMechPilot.isActive();
   }

   @Override
   protected Component title() {
      return Component.translatable("awakened.stone_mech.controls.title");
   }

   @Override
   protected List<ControlsHud.Row> rows() {
      Options o = Minecraft.getInstance().options;
      Component move = Component.empty()
         .append(o.keyUp.getTranslatedKeyMessage())
         .append(o.keyLeft.getTranslatedKeyMessage())
         .append(o.keyDown.getTranslatedKeyMessage())
         .append(o.keyRight.getTranslatedKeyMessage());
      return List.of(
         new ControlsHud.Row(move, Component.translatable("awakened.stone_mech.controls.move")),
         ControlsHud.Row.of(o.keySprint, "awakened.stone_mech.controls.run"),
         ControlsHud.Row.of(o.keyJump, "awakened.stone_mech.controls.pound"),
         ControlsHud.Row.of(o.keyAttack, "awakened.stone_mech.controls.hammer"),
         ControlsHud.Row.of(o.keyUse, "awakened.stone_mech.controls.boulder"),
         ControlsHud.Row.of(o.keyTogglePerspective, "awakened.stone_mech.controls.view"),
         ControlsHud.Row.of(o.keyShift, "awakened.stone_mech.controls.exit")
      );
   }
}
