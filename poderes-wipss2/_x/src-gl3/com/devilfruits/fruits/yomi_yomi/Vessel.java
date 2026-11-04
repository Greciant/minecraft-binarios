package com.devilfruits.fruits.yomi_yomi;

import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;

public enum Vessel {
   HUMAN,
   CORPSE,
   SKELETON;

   public String formName() {
      return this.name().toLowerCase(Locale.ROOT);
   }

   public Component displayName() {
      return Component.translatable("form.devilfruits.yomi_yomi." + this.formName());
   }

   @Nullable
   public static Vessel byName(String name) {
      for (Vessel v : values()) {
         if (v.formName().equals(name)) {
            return v;
         }
      }

      return null;
   }
}
