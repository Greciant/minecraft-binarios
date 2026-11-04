package com.awakenedmod.awakened.content.hydro;

import com.awakenedmod.awakened.core.Fx;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class VesselItem extends Item {
   private static final String WATER_KEY = "water";
   private static final int BAR_COLOR = 3051007;
   private final int capacity;
   private final int tier;

   public VesselItem(Properties properties, int capacity, int tier) {
      super(properties);
      this.capacity = capacity;
      this.tier = tier;
   }

   public int capacity() {
      return this.capacity;
   }

   public int tier() {
      return this.tier;
   }

   public static int capacityOf(ItemStack stack) {
      return stack.getItem() instanceof VesselItem v ? v.capacity : 0;
   }

   public static int tierOf(ItemStack stack) {
      return stack.getItem() instanceof VesselItem v ? v.tier : 0;
   }

   public static float charges(ItemStack stack) {
      CustomData data = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
      return data == null ? 0.0F : data.copyTag().getFloat("water");
   }

   public static void setCharges(ItemStack stack, float value) {
      float clamped = Math.max(0.0F, Math.min((float)capacityOf(stack), value));
      CustomData existing = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
      CompoundTag tag = existing == null ? new CompoundTag() : existing.copyTag();
      tag.putFloat("water", clamped);
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
   }

   public ItemStack filled() {
      ItemStack stack = new ItemStack(this);
      setCharges(stack, (float)this.capacity);
      return stack;
   }

   public static float available(Player player) {
      Inventory inv = player.getInventory();
      float total = 0.0F;

      for (int i = 0; i < inv.getContainerSize(); i++) {
         ItemStack s = inv.getItem(i);
         if (s.getItem() instanceof VesselItem) {
            total += charges(s);
         }
      }

      return total;
   }

   public static float drain(Player player, float want) {
      Inventory inv = player.getInventory();
      float remaining = want;
      float drained = 0.0F;

      for (int i = 0; i < inv.getContainerSize() && remaining > 0.0F; i++) {
         ItemStack s = inv.getItem(i);
         if (s.getItem() instanceof VesselItem) {
            float have = charges(s);
            if (!(have <= 0.0F)) {
               float take = Math.min(have, remaining);
               setCharges(s, have - take);
               drained += take;
               remaining -= take;
            }
         }
      }

      return drained;
   }

   public static ItemStack best(Player player) {
      Inventory inv = player.getInventory();
      ItemStack best = null;

      for (int i = 0; i < inv.getContainerSize(); i++) {
         ItemStack s = inv.getItem(i);
         if (s.getItem() instanceof VesselItem
            && (best == null || capacityOf(s) > capacityOf(best) || capacityOf(s) == capacityOf(best) && charges(s) > charges(best))) {
            best = s;
         }
      }

      return best;
   }

   public static void passiveRefill(Player player, float amount) {
      Inventory inv = player.getInventory();

      for (int i = 0; i < inv.getContainerSize(); i++) {
         ItemStack s = inv.getItem(i);
         if (s.getItem() instanceof VesselItem && tierOf(s) >= 4) {
            float have = charges(s);
            if (have < (float)capacityOf(s)) {
               setCharges(s, have + amount);
               return;
            }
         }
      }
   }

   public InteractionResult useOn(UseOnContext ctx) {
      Level level = ctx.getLevel();
      BlockPos pos = ctx.getClickedPos();
      boolean water = isWater(level, pos) || isWater(level, pos.relative(ctx.getClickedFace()));
      if (!water) {
         return InteractionResult.PASS;
      } else {
         if (!level.isClientSide()) {
            this.fillAndChime(level, ctx.getItemInHand(), Vec3.atCenterOf(pos));
         }

         return InteractionResult.SUCCESS;
      }
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      Vec3 at = player.position();
      boolean water = player.isInWater();
      if (!water) {
         BlockHitResult hit = level.clip(
            new ClipContext(player.getEyePosition(), player.getEyePosition().add(player.getLookAngle().scale(5.0)), Block.OUTLINE, Fluid.SOURCE_ONLY, player)
         );
         if (hit.getType() == Type.BLOCK && isWater(level, hit.getBlockPos())) {
            water = true;
            at = Vec3.atCenterOf(hit.getBlockPos());
         }
      }

      if (!water) {
         return InteractionResultHolder.pass(stack);
      } else {
         if (!level.isClientSide()) {
            this.fillAndChime(level, stack, at);
         }

         return InteractionResultHolder.success(stack);
      }
   }

   private void fillAndChime(Level level, ItemStack stack, Vec3 at) {
      if (charges(stack) < (float)this.capacity) {
         setCharges(stack, (float)this.capacity);
         Fx.sound(level, at, SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 0.8F, 1.0F);
      }
   }

   private static boolean isWater(Level level, BlockPos pos) {
      return level.getFluidState(pos).is(FluidTags.WATER);
   }

   public boolean isBarVisible(ItemStack stack) {
      return true;
   }

   public int getBarWidth(ItemStack stack) {
      int cap = capacityOf(stack);
      return cap <= 0 ? 0 : Math.round(13.0F * charges(stack) / (float)cap);
   }

   public int getBarColor(ItemStack stack) {
      return 3051007;
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("item.awakened.vessel.water", new Object[]{round(charges(stack)), this.capacity}).withStyle(ChatFormatting.AQUA));
      tooltip.add(Component.translatable("item.awakened.vessel.fill").withStyle(ChatFormatting.DARK_GRAY));
   }

   private static String round(float v) {
      return (double)v == Math.rint((double)v) ? Integer.toString((int)v) : String.format("%.1f", v);
   }
}
