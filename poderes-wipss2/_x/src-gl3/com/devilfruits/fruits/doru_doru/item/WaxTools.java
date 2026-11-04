package com.devilfruits.fruits.doru_doru.item;

import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.fruits.doru_doru.DoruAbilities;
import com.devilfruits.fruits.doru_doru.DoruDoru;
import com.devilfruits.fruits.doru_doru.DoruFx;
import com.devilfruits.fruits.doru_doru.DoruItems;
import com.devilfruits.fruits.doru_doru.DoruSounds;
import com.devilfruits.fruits.doru_doru.wax.WaxHeatService;
import com.devilfruits.registry.DFAttachments;
import com.devilfruits.registry.DFItems;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class WaxTools {
   public static final Tier TIER = new Tier() {
      public int getUses() {
         return 200;
      }

      public float getSpeed() {
         return 12.0F;
      }

      public float getAttackDamageBonus() {
         return 0.0F;
      }

      public TagKey<Block> getIncorrectBlocksForDrops() {
         return BlockTags.INCORRECT_FOR_IRON_TOOL;
      }

      public int getEnchantmentValue() {
         return 22;
      }

      public Ingredient getRepairIngredient() {
         return Ingredient.EMPTY;
      }
   };
   public static final int KEY_CHOICE = WaxTools.Kind.values().length;

   public static boolean isWaxTool(ItemStack stack) {
      return stack.getItem() instanceof WaxTools.WaxTool || stack.getItem() instanceof WaxLanceItem || stack.getItem() instanceof WaxKeyItem;
   }

   public static boolean hasMold(Player player) {
      Inventory inv = player.getInventory();

      for (int i = 0; i < inv.getContainerSize(); i++) {
         if (inv.getItem(i).is((Item)DFItems.GOLD_KEY.get())) {
            return true;
         }
      }

      return false;
   }

   public static boolean formKey(ServerPlayer player, int color) {
      if (hasMold(player) && DoruDoru.canWax(player)) {
         float cost = (float)DoruAbilities.TOOLS.config().extra("keyEnergy");
         PlayerFruitData data = DFAttachments.data(player);
         if (!AbilityManager.freeEnergy(player)) {
            if (data.energy() < cost) {
               player.displayClientMessage(DoruDoru.fruit().message("no_energy", (int)Math.ceil((double)cost)), true);
               return false;
            }

            data.setEnergy(data.energy() - cost);
         }

         ItemStack current = player.getOffhandItem();
         ItemStack original = current;
         if (isWaxTool(current)) {
            WaxTools.Bound b = (WaxTools.Bound)current.get((DataComponentType)DoruItems.BOUND.get());
            original = b == null ? ItemStack.EMPTY : b.original();
         }

         ItemStack key = new ItemStack((ItemLike)DoruItems.WAX_KEY.get());
         key.set((DataComponentType)DoruItems.BOUND.get(), new WaxTools.Bound(original.copy(), 40, color));
         player.setItemInHand(InteractionHand.OFF_HAND, key);
         player.level().playSound(null, player.getX(), player.getY(), player.getZ(), DoruSounds.TOOL_FORM.get(), SoundSource.PLAYERS, 0.8F, 1.3F);
         ServerFx.send(player, DoruFx.TOOL_FORM, player.getEyePosition(), player.getLookAngle(), 1.0F, player.getRandom().nextLong(), 0);
         return true;
      } else {
         return false;
      }
   }

   public static Item item(WaxTools.Kind kind) {
      return switch (kind) {
         case PICKAXE -> (Item)DoruItems.WAX_PICKAXE.get();
         case SHOVEL -> (Item)DoruItems.WAX_SHOVEL.get();
         case AXE -> (Item)DoruItems.WAX_AXE.get();
         case HOE -> (Item)DoruItems.WAX_HOE.get();
         case SWORD -> (Item)DoruItems.WAX_SWORD.get();
      };
   }

   public static void form(ServerPlayer player, WaxTools.Kind kind, int color) {
      bind(player, new ItemStack(item(kind)), color);
      player.level()
         .playSound(
            null,
            player.getX(),
            player.getY(),
            player.getZ(),
            DoruSounds.TOOL_FORM.get(),
            SoundSource.PLAYERS,
            0.8F,
            0.9F + player.getRandom().nextFloat() * 0.2F
         );
      ServerFx.send(player, DoruFx.TOOL_FORM, player.getEyePosition(), player.getLookAngle(), 1.0F, player.getRandom().nextLong(), kind.ordinal());
      ServerFx.animate(player, DoruDoru.id("tool_form"));
   }

   public static void bind(ServerPlayer player, ItemStack tool, int color) {
      Inventory inv = player.getInventory();
      int slot = inv.selected;
      ItemStack current = inv.getItem(slot);
      ItemStack original = current;
      if (isWaxTool(current)) {
         WaxTools.Bound b = (WaxTools.Bound)current.get((DataComponentType)DoruItems.BOUND.get());
         original = b == null ? ItemStack.EMPTY : b.original();
      }

      tool.set((DataComponentType)DoruItems.BOUND.get(), new WaxTools.Bound(original.copy(), slot, color));
      inv.setItem(slot, tool);
   }

   public static void dissolveAll(ServerPlayer player, boolean melted) {
      Inventory inv = player.getInventory();

      for (int i = 0; i < inv.getContainerSize(); i++) {
         ItemStack s = inv.getItem(i);
         if (isWaxTool(s)) {
            dissolve(player, s, i, melted);
         }
      }

      if (isWaxTool(player.containerMenu.getCarried())) {
         restore(player, (WaxTools.Bound)player.containerMenu.getCarried().get((DataComponentType)DoruItems.BOUND.get()), -1);
         player.containerMenu.setCarried(ItemStack.EMPTY);
      }
   }

   public static void dissolve(ServerPlayer player, ItemStack tool, int slot, boolean melted) {
      WaxTools.Bound bound = (WaxTools.Bound)tool.get((DataComponentType)DoruItems.BOUND.get());
      Inventory inv = player.getInventory();
      if (slot >= 0 && slot < inv.getContainerSize() && inv.getItem(slot) == tool) {
         inv.setItem(slot, ItemStack.EMPTY);
      } else {
         tool.setCount(0);
      }

      restore(player, bound, slot);
      ServerLevel level = player.serverLevel();
      Vec3 at = player.getEyePosition().add(player.getLookAngle().scale(0.5)).add(0.0, -0.4, 0.0);
      level.sendParticles(
         new ItemParticleOption(ParticleTypes.ITEM, new ItemStack((ItemLike)DoruItems.WAX_LUMP.get())),
         at.x,
         at.y,
         at.z,
         melted ? 10 : 5,
         0.15,
         0.1,
         0.15,
         0.03
      );
      level.playSound(
         null, player.getX(), player.getY(), player.getZ(), melted ? DoruSounds.TOOL_MELT.get() : DoruSounds.DISSOLVE.get(), SoundSource.PLAYERS, 0.5F, 1.2F
      );
   }

   private static void restore(ServerPlayer player, @Nullable WaxTools.Bound bound, int slot) {
      if (bound != null && !bound.original().isEmpty()) {
         ItemStack back = bound.original().copy();
         Inventory inv = player.getInventory();
         int at = slot >= 0 ? slot : bound.slot();
         if (at >= 0 && at < inv.getContainerSize() && inv.getItem(at).isEmpty()) {
            inv.setItem(at, back);
         } else if (!inv.add(back)) {
            player.drop(back, false);
         }
      }
   }

   static void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
      if (entity instanceof ServerPlayer player && !level.isClientSide()) {
         boolean key = stack.getItem() instanceof WaxKeyItem;
         boolean held = key ? player.getOffhandItem() == stack : selected && slot == player.getInventory().selected;
         if (key && held) {
            slot = 40;
         }

         if (held && DoruDoru.canWax(player) && !player.isDeadOrDying()) {
            if (player.tickCount % 10 != 0) {
               return;
            }

            float heat = WaxHeatService.heatAt(player.serverLevel(), player.blockPosition()) + (player.isOnFire() ? 1.5F : 0.0F);
            int max = stack.getMaxDamage();
            int damage = stack.getDamageValue();
            if (heat > 0.0F) {
               damage += Math.round(heat * (float)max * 0.06F);
               if (damage >= max) {
                  dissolve(player, stack, slot, true);
                  player.displayClientMessage(Component.translatable("message.devilfruits.doru_doru.tool_melted").withStyle(ChatFormatting.GOLD), true);
                  return;
               }
            } else if (damage > 0) {
               damage = Math.max(0, damage - max / 40);
            }

            if (damage != stack.getDamageValue()) {
               stack.setDamageValue(damage);
            }

            return;
         }

         dissolve(player, stack, slot, false);
         return;
      }
   }

   static float softness(ItemStack stack) {
      return stack.getMaxDamage() <= 0 ? 1.0F : 1.0F - 0.6F * (float)stack.getDamageValue() / (float)stack.getMaxDamage();
   }

   private WaxTools() {
   }

   public static class Axe extends AxeItem implements WaxTools.WaxTool {
      public Axe(Properties p) {
         super(WaxTools.TIER, p.attributes(AxeItem.createAttributes(WaxTools.TIER, 6.0F, -3.0F)));
      }

      @Override
      public WaxTools.Kind kind() {
         return WaxTools.Kind.AXE;
      }

      public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
         WaxTools.inventoryTick(stack, level, entity, slot, selected);
      }

      public float getDestroySpeed(ItemStack stack, BlockState state) {
         return super.getDestroySpeed(stack, state) * WaxTools.softness(stack);
      }

      public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
         return 0;
      }

      public boolean onDroppedByPlayer(ItemStack item, Player player) {
         return false;
      }
   }

   public static record Bound(ItemStack original, int slot, int color) {
      public static final Codec<WaxTools.Bound> CODEC = RecordCodecBuilder.create(
         i -> i.group(
                  ItemStack.OPTIONAL_CODEC.fieldOf("original").forGetter(WaxTools.Bound::original),
                  Codec.INT.fieldOf("slot").forGetter(WaxTools.Bound::slot),
                  Codec.INT.fieldOf("color").forGetter(WaxTools.Bound::color)
               )
               .apply(i, WaxTools.Bound::new)
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, WaxTools.Bound> STREAM_CODEC = StreamCodec.composite(
         ItemStack.OPTIONAL_STREAM_CODEC,
         WaxTools.Bound::original,
         ByteBufCodecs.VAR_INT,
         WaxTools.Bound::slot,
         ByteBufCodecs.VAR_INT,
         WaxTools.Bound::color,
         WaxTools.Bound::new
      );
   }

   public static class Hoe extends HoeItem implements WaxTools.WaxTool {
      public Hoe(Properties p) {
         super(WaxTools.TIER, p.attributes(HoeItem.createAttributes(WaxTools.TIER, 0.0F, -3.0F)));
      }

      @Override
      public WaxTools.Kind kind() {
         return WaxTools.Kind.HOE;
      }

      public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
         WaxTools.inventoryTick(stack, level, entity, slot, selected);
      }

      public float getDestroySpeed(ItemStack stack, BlockState state) {
         return super.getDestroySpeed(stack, state) * WaxTools.softness(stack);
      }

      public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
         return 0;
      }

      public boolean onDroppedByPlayer(ItemStack item, Player player) {
         return false;
      }
   }

   public static enum Kind {
      PICKAXE,
      SHOVEL,
      AXE,
      HOE,
      SWORD;
   }

   public static class Pickaxe extends PickaxeItem implements WaxTools.WaxTool {
      public Pickaxe(Properties p) {
         super(WaxTools.TIER, p.attributes(PickaxeItem.createAttributes(WaxTools.TIER, 1.0F, -2.8F)));
      }

      @Override
      public WaxTools.Kind kind() {
         return WaxTools.Kind.PICKAXE;
      }

      public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
         WaxTools.inventoryTick(stack, level, entity, slot, selected);
      }

      public float getDestroySpeed(ItemStack stack, BlockState state) {
         return super.getDestroySpeed(stack, state) * WaxTools.softness(stack);
      }

      public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
         return 0;
      }

      public boolean onDroppedByPlayer(ItemStack item, Player player) {
         return false;
      }
   }

   public static class Shovel extends ShovelItem implements WaxTools.WaxTool {
      public Shovel(Properties p) {
         super(WaxTools.TIER, p.attributes(ShovelItem.createAttributes(WaxTools.TIER, 1.5F, -3.0F)));
      }

      @Override
      public WaxTools.Kind kind() {
         return WaxTools.Kind.SHOVEL;
      }

      public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
         WaxTools.inventoryTick(stack, level, entity, slot, selected);
      }

      public float getDestroySpeed(ItemStack stack, BlockState state) {
         return super.getDestroySpeed(stack, state) * WaxTools.softness(stack);
      }

      public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
         return 0;
      }

      public boolean onDroppedByPlayer(ItemStack item, Player player) {
         return false;
      }
   }

   public static class Sword extends SwordItem implements WaxTools.WaxTool {
      public Sword(Properties p) {
         super(WaxTools.TIER, p.attributes(SwordItem.createAttributes(WaxTools.TIER, 3, -2.4F)));
      }

      @Override
      public WaxTools.Kind kind() {
         return WaxTools.Kind.SWORD;
      }

      public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
         WaxTools.inventoryTick(stack, level, entity, slot, selected);
      }

      public float getDestroySpeed(ItemStack stack, BlockState state) {
         return super.getDestroySpeed(stack, state) * WaxTools.softness(stack);
      }

      public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
         return 0;
      }

      public boolean onDroppedByPlayer(ItemStack item, Player player) {
         return false;
      }
   }

   public interface WaxTool {
      WaxTools.Kind kind();
   }
}
