package com.awakenedmod.awakened.client;

import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;

public final class HudSettings {
   private final BooleanValue enabled;
   private final BooleanValue placed;
   private final EnumValue<HudAnchor> anchor;
   private final DoubleValue x;
   private final DoubleValue y;

   private HudSettings(BooleanValue enabled, BooleanValue placed, EnumValue<HudAnchor> anchor, DoubleValue x, DoubleValue y) {
      this.enabled = enabled;
      this.placed = placed;
      this.anchor = anchor;
      this.x = x;
      this.y = y;
   }

   public static HudSettings define(Builder builder, String key, String what, boolean on, boolean moved) {
      String path = "hud." + key;
      return new HudSettings(
         builder.comment("Show " + what + " on your HUD.").define(path, on),
         builder.comment("Whether you have moved " + what + ". False = default spot; the settings below are ignored.").define(path + "Placed", moved),
         builder.comment("Which point of " + what + "'s box the position refers to.").defineEnum(path + "Anchor", HudAnchor.TOP_LEFT),
         builder.comment("Horizontal position of " + what + ", as a fraction of screen width (0 = left, 1 = right).").defineInRange(path + "X", 0.0, 0.0, 1.0),
         builder.comment("Vertical position of " + what + ", as a fraction of screen height (0 = top, 1 = bottom).").defineInRange(path + "Y", 0.0, 0.0, 1.0)
      );
   }

   public static HudSettings define(Builder builder, String key, String what, boolean on) {
      return define(builder, key, what, on, false);
   }

   public boolean enabled() {
      return (Boolean)this.enabled.get();
   }

   public void setEnabled(boolean on) {
      this.enabled.set(on);
   }

   public boolean placed() {
      return (Boolean)this.placed.get();
   }

   public HudAnchor anchor() {
      return (HudAnchor)this.anchor.get();
   }

   public double x() {
      return (Double)this.x.get();
   }

   public double y() {
      return (Double)this.y.get();
   }

   public void store(HudAnchor newAnchor, double fx, double fy) {
      this.anchor.set(newAnchor);
      this.x.set(fx);
      this.y.set(fy);
      this.placed.set(true);
   }

   public void clearPlacement() {
      this.placed.set(false);
   }
}
