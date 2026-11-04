package com.impactframe.client;

public enum ImpactPreset {
   OFF("off"),
   MINIMAL("minimal"),
   NORMAL("normal"),
   I_LOVE_IMPACT_FRAMES("love"),
   RIDICULOUS("ridiculous"),
   ADVANCED("advanced");

   public final String id;

   private ImpactPreset(String id) {
      this.id = id;
   }

   public String labelKey() {
      return "impactframe.gui.preset." + this.id;
   }

   public String descriptionKey() {
      return this.labelKey() + ".description";
   }

   public ImpactPreset next() {
      return values()[(this.ordinal() + 1) % values().length];
   }

   public static ImpactPreset fromId(String id) {
      for (ImpactPreset preset : values()) {
         if (preset.id.equals(id)) {
            return preset;
         }
      }

      return NORMAL;
   }
}
