package com.devilfruits.core.gear;

import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.registry.DFAttachments;
import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Player;

public record GearState(GearDefinition gear, @Nullable GearDefinition subGear) {
   @Nullable
   public static GearState of(@Nullable DevilFruit fruit, @Nullable FruitForm form) {
      if (form == null) {
         return null;
      } else {
         for (GearDefinition gear : GearRegistry.of(fruit)) {
            if (gear.leaf()) {
               if (matches(gear, form)) {
                  return new GearState(gear, null);
               }
            } else {
               for (GearDefinition sub : gear.subGears()) {
                  if (matches(sub, form)) {
                     return new GearState(gear, sub);
                  }
               }
            }
         }

         return null;
      }
   }

   @Nullable
   public static GearState of(Player player) {
      if (!player.hasData(DFAttachments.PLAYER_DATA)) {
         return null;
      } else {
         PlayerFruitData data = DFAttachments.data(player);
         return of(data.fruit(), data.form());
      }
   }

   public GearDefinition current() {
      return this.subGear != null ? this.subGear : this.gear;
   }

   public static boolean matches(GearDefinition gear, FruitForm form) {
      return gear.available() && (gear.form() == null ? form.isBase() : gear.form().equals(form.name()));
   }
}
