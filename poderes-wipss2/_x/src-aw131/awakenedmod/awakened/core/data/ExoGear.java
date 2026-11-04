package com.awakenedmod.awakened.core.data;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ExoGear {
   public static final int SIZE = 6;
   private static final String[] KEYS = new String[]{"gauntlets", "boots", "chestplate", "leggings", "helmet", "chip"};
   public static final MapCodec<ExoGear> MAP_CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
               ItemStack.OPTIONAL_CODEC.optionalFieldOf(KEYS[0], ItemStack.EMPTY).forGetter(g -> g.get(0)),
               ItemStack.OPTIONAL_CODEC.optionalFieldOf(KEYS[1], ItemStack.EMPTY).forGetter(g -> g.get(1)),
               ItemStack.OPTIONAL_CODEC.optionalFieldOf(KEYS[2], ItemStack.EMPTY).forGetter(g -> g.get(2)),
               ItemStack.OPTIONAL_CODEC.optionalFieldOf(KEYS[3], ItemStack.EMPTY).forGetter(g -> g.get(3)),
               ItemStack.OPTIONAL_CODEC.optionalFieldOf(KEYS[4], ItemStack.EMPTY).forGetter(g -> g.get(4)),
               ItemStack.OPTIONAL_CODEC.optionalFieldOf(KEYS[5], ItemStack.EMPTY).forGetter(g -> g.get(5))
            )
            .apply(instance, ExoGear::of)
   );
   private final ItemStack[] slots;

   public ExoGear() {
      this.slots = new ItemStack[6];
      Arrays.fill(this.slots, ItemStack.EMPTY);
   }

   private ExoGear(ItemStack[] slots) {
      this.slots = slots;
   }

   private static ExoGear of(ItemStack s0, ItemStack s1, ItemStack s2, ItemStack s3, ItemStack s4, ItemStack s5) {
      return new ExoGear(new ItemStack[]{s0, s1, s2, s3, s4, s5});
   }

   public ItemStack get(int index) {
      return this.slots[index];
   }

   public ExoGear with(int index, ItemStack stack) {
      this.slots[index] = stack.copy();
      return this;
   }

   public boolean isEmpty() {
      for (ItemStack stack : this.slots) {
         if (!stack.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   public ExoGear copy() {
      ItemStack[] out = new ItemStack[6];

      for (int i = 0; i < 6; i++) {
         out[i] = this.slots[i].copy();
      }

      return new ExoGear(out);
   }

   public static ExoGear get(Player player) {
      return (ExoGear)player.getData(AwakenedAttachments.EXO_GEAR);
   }

   public static void write(ServerPlayer player, ExoGear gear) {
      player.setData(AwakenedAttachments.EXO_GEAR, gear.copy());
   }

   public static void clear(ServerPlayer player) {
      player.setData(AwakenedAttachments.EXO_GEAR, new ExoGear());
   }
}
