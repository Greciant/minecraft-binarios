package com.awakenedmod.awakened.core.curios;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public final class CuriosSlots {
   private CuriosSlots() {
   }

   public static ICuriosItemHandler inventory(LivingEntity entity) {
      return entity == null ? null : (ICuriosItemHandler)CuriosApi.getCuriosInventory(entity).orElse(null);
   }

   public static ICurioStacksHandler handler(LivingEntity entity, String slot) {
      ICuriosItemHandler inv = inventory(entity);
      return inv != null && slot != null ? (ICurioStacksHandler)inv.getStacksHandler(slot).orElse(null) : null;
   }

   public static IDynamicStackHandler stacks(LivingEntity entity, String slot) {
      ICurioStacksHandler handler = handler(entity, slot);
      return handler == null ? null : handler.getStacks();
   }

   public static List<ItemStack> worn(LivingEntity entity) {
      ICuriosItemHandler inv = inventory(entity);
      if (inv == null) {
         return List.of();
      } else {
         List<ItemStack> found = new ArrayList<>();

         for (ICurioStacksHandler handler : inv.getCurios().values()) {
            IDynamicStackHandler stacks = handler == null ? null : handler.getStacks();
            if (stacks != null) {
               for (int i = 0; i < stacks.getSlots(); i++) {
                  ItemStack stack = stacks.getStackInSlot(i);
                  if (!stack.isEmpty()) {
                     found.add(stack);
                  }
               }
            }
         }

         return found;
      }
   }

   public static int indexOf(LivingEntity entity, String slot, Item item) {
      IDynamicStackHandler stacks = stacks(entity, slot);
      if (stacks != null && item != null) {
         for (int i = 0; i < stacks.getSlots(); i++) {
            if (stacks.getStackInSlot(i).is(item)) {
               return i;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   public static boolean rendersWorn(LivingEntity entity, String slot, Item item) {
      int index = indexOf(entity, slot, item);
      if (index < 0) {
         return true;
      } else {
         ICurioStacksHandler handler = handler(entity, slot);
         if (handler == null) {
            return true;
         } else {
            List<Boolean> renders = handler.getRenders();
            if (renders != null && index < renders.size()) {
               Boolean render = renders.get(index);
               return render == null || render;
            } else {
               return true;
            }
         }
      }
   }
}
