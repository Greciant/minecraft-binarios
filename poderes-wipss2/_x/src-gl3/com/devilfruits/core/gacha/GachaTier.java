package com.devilfruits.core.gacha;

import com.devilfruits.core.fruit.DevilFruitType;
import java.util.Locale;
import net.minecraft.ChatFormatting;

public enum GachaTier {
   LEGENDARY(2, ChatFormatting.GOLD),
   RARE(8, ChatFormatting.LIGHT_PURPLE),
   UNCOMMON(25, ChatFormatting.AQUA),
   COMMON(65, ChatFormatting.GREEN);

   private final int defaultWeight;
   private final ChatFormatting color;

   private GachaTier(int defaultWeight, ChatFormatting color) {
      this.defaultWeight = defaultWeight;
      this.color = color;
   }

   public int defaultWeight() {
      return this.defaultWeight;
   }

   public ChatFormatting color() {
      return this.color;
   }

   public String translationKey() {
      return "gacha.devilfruits.tier." + this.name().toLowerCase(Locale.ROOT);
   }

   public boolean atLeast(GachaTier other) {
      return this.ordinal() <= other.ordinal();
   }

   public static GachaTier of(DevilFruitType type) {
      return switch (type) {
         case MYTHICAL_ZOAN -> LEGENDARY;
         case LOGIA, ANCIENT_ZOAN -> RARE;
         case PARAMECIA, ZOAN -> COMMON;
      };
   }
}
