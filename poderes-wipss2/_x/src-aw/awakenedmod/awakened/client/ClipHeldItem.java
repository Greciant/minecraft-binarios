package com.awakenedmod.awakened.client;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class ClipHeldItem {
   private static final Map<ResourceLocation, ClipHeldItem.Entry> BY_CLIP = new LinkedHashMap<>();

   private ClipHeldItem() {
   }

   public static void register(ResourceLocation clip, Supplier<ItemStack> item) {
      BY_CLIP.put(clip, new ClipHeldItem.Entry(item, null, 0));
   }

   public static void registerFor(ResourceLocation clip, int ticks, Supplier<ItemStack> item) {
      BY_CLIP.put(clip, new ClipHeldItem.Entry(item, new ClipClock(clip), ticks));
   }

   @Nullable
   public static ItemStack held(LivingEntity entity) {
      if (BY_CLIP.isEmpty()) {
         return null;
      } else {
         for (Map.Entry<ResourceLocation, ClipHeldItem.Entry> e : BY_CLIP.entrySet()) {
            if (AwakenedAnimations.isPlaying(entity.getId(), e.getKey()) && e.getValue().showing(entity)) {
               return e.getValue().item().get();
            }
         }

         return null;
      }
   }

   public static ItemStack shown(LivingEntity entity, HumanoidArm arm, ItemStack real) {
      ItemStack held = held(entity);
      if (held == null) {
         return real;
      } else {
         return arm == entity.getMainArm() ? held : ItemStack.EMPTY;
      }
   }

   private static record Entry(Supplier<ItemStack> item, @Nullable ClipClock clock, int ticks) {
      boolean showing(LivingEntity entity) {
         if (this.clock == null) {
            return true;
         } else {
            long age = this.clock.age(entity);
            return age >= 0L && age < (long)this.ticks;
         }
      }
   }
}
