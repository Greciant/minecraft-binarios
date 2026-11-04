package com.awakenedmod.awakened.content.exosavant;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public enum ExoSkin {
   DEFAULT("default", "textures/entity/exosavant_suit.png", null, null),
   SLIM("slim", skinTex("slim"), null, null),
   AXOLOTL("axolotl", skinTex("axolotl"), null, null),
   SCULK("sculk", skinTex("sculk"), skinTex("sculk_emissive"), skinTex("sculk_chip"));

   private static final String DEFAULT_EMISSIVE = "textures/entity/exosavant_suit_emissive.png";
   private static final String DEFAULT_CHIP = "textures/block/exosavant_chip.png";
   private final String id;
   private final ResourceLocation suit;
   private final ResourceLocation emissive;
   private final ResourceLocation chip;

   private ExoSkin(String id, String suit, String emissive, String chip) {
      this.id = id;
      this.suit = AwId.of(suit);
      this.emissive = AwId.of(emissive == null ? "textures/entity/exosavant_suit_emissive.png" : emissive);
      this.chip = AwId.of(chip == null ? "textures/block/exosavant_chip.png" : chip);
   }

   private static String skinTex(String name) {
      return "textures/entity/exosavant_skin/" + name + ".png";
   }

   public String id() {
      return this.id;
   }

   public ResourceLocation suit() {
      return this.suit;
   }

   public ResourceLocation emissive() {
      return this.emissive;
   }

   public ResourceLocation chip() {
      return this.chip;
   }

   public boolean free() {
      return this == DEFAULT;
   }

   public Component displayName() {
      return Component.translatable("awakened.exo_skin." + this.id);
   }

   public Component unlockHint() {
      return Component.translatable("awakened.exo_skin." + this.id + ".unlock");
   }

   public static ExoSkin byId(String id) {
      for (ExoSkin skin : values()) {
         if (skin.id.equals(id)) {
            return skin;
         }
      }

      return DEFAULT;
   }
}
