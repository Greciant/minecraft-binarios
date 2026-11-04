package com.devilfruits.core.fruit;

import com.devilfruits.client.ClientHooks;
import com.devilfruits.core.rules.FruitRules;
import com.devilfruits.registry.DFDataComponents;
import com.devilfruits.registry.DFSounds;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.FoodProperties.Builder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

public class DevilFruitItem extends Item {
   public static final FoodProperties FRUIT_FOOD = new Builder().nutrition(4).saturationModifier(0.6F).alwaysEdible().build();
   private final ResourceLocation fruitId;

   public DevilFruitItem(ResourceLocation fruitId, Properties properties) {
      super(properties);
      this.fruitId = fruitId;
   }

   public void onDestroyed(ItemEntity entity, DamageSource source) {
      super.onDestroyed(entity, source);
      if (entity.level() instanceof ServerLevel level) {
         FruitRules.onFruitItemDestroyed(level.getServer(), entity.getItem());
      }
   }

   public static Properties properties(Rarity rarity) {
      return new Properties().stacksTo(1).rarity(rarity).fireResistant().food(FRUIT_FOOD);
   }

   public ResourceLocation fruitId() {
      return this.fruitId;
   }

   @Nullable
   public DevilFruit fruit() {
      return FruitRegistry.get(this.fruitId);
   }

   public int getUseDuration(ItemStack stack, LivingEntity entity) {
      return 40;
   }

   public SoundEvent getEatingSound() {
      return DFSounds.FRUIT_EAT.get();
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      if (player.isCreative() && player.isShiftKeyDown()) {
         if (level.isClientSide() && FMLEnvironment.dist == Dist.CLIENT) {
            ClientHooks.openEncyclopedia(this.fruitId);
         }

         return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
      } else {
         return super.use(level, player, hand);
      }
   }

   public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
      DevilFruit fruit = this.fruit();
      if (!level.isClientSide() && entity instanceof ServerPlayer player && fruit != null) {
         FruitRules.eat(player, fruit, stack);
      }

      return super.finishUsingItem(stack, level, entity);
   }

   public Component getName(ItemStack stack) {
      DevilFruit fruit = this.fruit();
      int color = fruit != null ? fruit.theme().nameColor() : 16777215;
      return Component.translatable(this.getDescriptionId(stack)).withStyle(style -> style.withColor(TextColor.fromRgb(color)));
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      DevilFruit fruit = this.fruit();
      if (fruit != null) {
         tooltip.add(Component.translatable(fruit.key("type")).withStyle(ChatFormatting.DARK_AQUA));
         tooltip.add(Component.translatable(fruit.key("lore1")).withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}));
         tooltip.add(Component.translatable(fruit.key("lore2")).withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}));
         tooltip.add(Component.translatable("item.devilfruits.devil_fruit.warning").withStyle(ChatFormatting.DARK_RED));
         if (flag.isAdvanced() && stack.has((DataComponentType)DFDataComponents.FRUIT_INSTANCE.get())) {
            tooltip.add(Component.translatable("item.devilfruits.devil_fruit.unique").withStyle(ChatFormatting.DARK_GRAY));
         }
      }
   }
}
