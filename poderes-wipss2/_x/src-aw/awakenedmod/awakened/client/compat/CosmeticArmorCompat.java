package com.awakenedmod.awakened.client.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;

public final class CosmeticArmorCompat {
   private static final String MOD_ID = "cosmeticarmorreworked";
   private static final int SLOT_FEET = 0;
   private static final int SLOT_LEGS = 1;
   private static final int SLOT_CHEST = 2;
   private static final int SLOT_HEAD = 3;
   private static final int SLOT_COUNT = 4;
   public static final int ALL_SKINNED = 15;
   private static Method getCAStacksClient;
   private static Method isSkinArmor;
   private static Field disabled;
   private static boolean resolved;
   private static boolean available;

   private CosmeticArmorCompat() {
   }

   public static int skinnedMask(LivingEntity entity) {
      if (entity != null && wire()) {
         try {
            if (disabled != null && disabled.getBoolean(null)) {
               return 0;
            } else {
               Object stacks = getCAStacksClient.invoke(null, entity.getUUID());
               if (stacks == null) {
                  return 0;
               } else {
                  int mask = 0;

                  for (int slot = 0; slot < 4; slot++) {
                     if (Boolean.TRUE.equals(isSkinArmor.invoke(stacks, slot))) {
                        mask |= 1 << slot;
                     }
                  }

                  return mask;
               }
            }
         } catch (Throwable var4) {
            available = false;
            return 0;
         }
      } else {
         return 0;
      }
   }

   public static boolean skinned(int mask, EquipmentSlot slot) {
      int index = index(slot);
      return index >= 0 && (mask & 1 << index) != 0;
   }

   public static boolean allSkinned(int mask) {
      return (mask & 15) == 15;
   }

   private static int index(EquipmentSlot slot) {
      if (slot == null) {
         return -1;
      } else {
         return switch (slot) {
            case FEET -> 0;
            case LEGS -> 1;
            case CHEST -> 2;
            case HEAD -> 3;
            default -> -1;
         };
      }
   }

   private static boolean wire() {
      if (!resolved) {
         resolved = true;
         available = false;
         if (ModList.get() != null && ModList.get().isLoaded("cosmeticarmorreworked")) {
            try {
               Class<?> api = Class.forName("lain.mods.cos.api.CosArmorAPI");
               Class<?> stacks = Class.forName("lain.mods.cos.api.inventory.CAStacksBase");
               getCAStacksClient = api.getMethod("getCAStacksClient", UUID.class);
               isSkinArmor = stacks.getMethod("isSkinArmor", int.class);
               available = true;

               try {
                  disabled = Class.forName("lain.mods.cos.impl.client.PlayerRenderHandler").getField("Disabled");
               } catch (Throwable var3) {
                  disabled = null;
               }
            } catch (Throwable var4) {
               available = false;
            }
         }
      }

      return available;
   }
}
