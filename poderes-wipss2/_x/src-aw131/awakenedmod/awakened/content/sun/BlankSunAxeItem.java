package com.awakenedmod.awakened.content.sun;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.LoopSound;
import com.awakenedmod.awakened.core.net.AwakenScreenFxPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class BlankSunAxeItem extends Item {
   private static final int CHANNEL = 80;
   private static final float ACTIVATE_LIGHT = 0.25F;

   public BlankSunAxeItem(Properties properties) {
      super(properties);
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack held = player.getItemInHand(hand);
      if (player instanceof ServerPlayer sp && !Solar.hasSunPower(sp)) {
         sp.displayClientMessage(Component.translatable("awakened.sun.axe_fail_power"), true);
         return InteractionResultHolder.fail(held);
      }

      if (player instanceof ServerPlayer sp && Solar.openSunlight(sp) < 0.25F) {
         sp.displayClientMessage(Component.translatable("awakened.sun.axe_fail"), true);
         return InteractionResultHolder.fail(held);
      }

      player.startUsingItem(hand);
      return InteractionResultHolder.consume(held);
   }

   public int getUseDuration(ItemStack stack, LivingEntity entity) {
      return 80;
   }

   public UseAnim getUseAnimation(ItemStack stack) {
      return UseAnim.BOW;
   }

   public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
      if (!(level instanceof ServerLevel server) || !(entity instanceof ServerPlayer player)) {
         return;
      }

      if (Solar.openSunlight(player) < 0.25F) {
         player.stopUsingItem();
      } else {
         float prog = 1.0F - (float)remaining / 80.0F;
         Vec3 up = player.position().add(0.0, 1.7, 0.0);
         Fx.burst(server, ParticleTypes.END_ROD, up, 2 + (int)(prog * 10.0F), 0.3 + (double)prog * 0.5, 0.02);
         if (prog > 0.5F) {
            Fx.burst(server, ParticleTypes.FLAME, up, (int)(prog * 6.0F), 0.3, 0.01);
         }

         LoopSound.keep(player, "sun_axe_channel", SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.4F + prog * 0.6F, 0.8F + prog * 0.9F);
      }
   }

   public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
      if (entity instanceof ServerPlayer player && timeLeft > 0) {
         String key = Solar.openSunlight(player) < 0.25F ? "awakened.sun.axe_fail" : "awakened.sun.axe_fail_early";
         player.displayClientMessage(Component.translatable(key), true);
         LoopSound.stop(player, "sun_axe_channel");
         Fx.sound(player.serverLevel(), player.position(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6F, 1.0F);
      }
   }

   public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
      if (entity instanceof ServerPlayer player && level instanceof ServerLevel server) {
         LoopSound.stop(player, "sun_axe_channel");
         if (Solar.openSunlight(player) < 0.25F) {
            player.displayClientMessage(Component.translatable("awakened.sun.axe_fail"), true);
            return stack;
         }

         igniteFlourish(server, player);
         return forge(stack);
      }

      return stack;
   }

   private static ItemStack forge(ItemStack blank) {
      ItemStack awakened = new ItemStack((ItemLike)AwakenedItems.SUN_AXE.get());
      ItemEnchantments enchantments = (ItemEnchantments)blank.get(DataComponents.ENCHANTMENTS);
      if (enchantments != null && !enchantments.isEmpty()) {
         awakened.set(DataComponents.ENCHANTMENTS, enchantments);
      }

      Integer repairCost = (Integer)blank.get(DataComponents.REPAIR_COST);
      if (repairCost != null) {
         awakened.set(DataComponents.REPAIR_COST, repairCost);
      }

      Component name = (Component)blank.get(DataComponents.CUSTOM_NAME);
      if (name != null) {
         awakened.set(DataComponents.CUSTOM_NAME, name);
      }

      return awakened;
   }

   private static void igniteFlourish(ServerLevel server, ServerPlayer player) {
      Vec3 c = player.position().add(0.0, 1.2, 0.0);
      Vfx.sphere(server, c, 3.0, 16770688, 24);
      Vfx.sphere(server, c, 1.6, 16761390, 24);
      Vfx.ring(server, player.position().add(0.0, 0.1, 0.0), 4.0, 16761390, 20);
      Fx.burst(server, ParticleTypes.END_ROD, c, 120, 1.2, 0.4);
      Fx.burst(server, ParticleTypes.FLAME, c, 50, 1.0, 0.3);
      Fx.sound(server, player.position(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.4F, 1.0F);
      Fx.sound(server, player.position(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.2F, 0.6F);
      PacketDistributor.sendToPlayer(player, new AwakenScreenFxPayload(16770688, 50), new CustomPacketPayload[0]);
      player.connection.send(new ClientboundSetTitlesAnimationPacket(5, 50, 15));
      player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("awakened.sun.axe_activated").withStyle(ChatFormatting.GOLD)));
      player.displayClientMessage(Component.translatable("awakened.sun.axe_activated"), false);
   }
}
