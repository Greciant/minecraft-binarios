package com.devilfruits.core.poneglyph;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;

public final class PoneglyphItems {
   public static Runnable openPrimerScreen = () -> {
   };
   private static final String[] POSITIONS = new String[]{"top_left", "top", "top_right", "left", "center", "right", "bottom_left", "bottom", "bottom_right"};

   private PoneglyphItems() {
   }

   public static class Deciphered extends Item {
      public Deciphered(Properties properties) {
         super(properties);
      }

      public boolean isFoil(ItemStack stack) {
         return true;
      }

      public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
         ItemStack stack = player.getItemInHand(hand);
         BlueprintData.Deciphered data = (BlueprintData.Deciphered)stack.get((DataComponentType)Poneglyphs.DECIPHERED.get());
         if (data == null) {
            return InteractionResultHolder.pass(stack);
         } else if (player instanceof ServerPlayer sp) {
            Knowledge.Outcome outcome = Knowledge.learn(sp, data.knowledge());
            switch (outcome.result()) {
               case LEARNED:
                  stack.consume(1, player);
                  ServerLevel sl = sp.serverLevel();
                  sl.sendParticles(Poneglyphs.GLYPH_PARTICLE.get(), sp.getX(), sp.getY() + 1.2, sp.getZ(), 32, 0.5, 0.6, 0.5, 0.08);
                  sl.playSound(null, sp.blockPosition(), Poneglyphs.SOUND_LEARN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                  break;
               case ALREADY:
                  sp.displayClientMessage(
                     Component.translatable("poneglyph.devilfruits.message.already_known", new Object[]{Knowledge.name(data.knowledge())})
                        .withStyle(ChatFormatting.GRAY),
                     true
                  );
                  break;
               case UNKNOWN:
                  sp.displayClientMessage(Component.translatable("poneglyph.devilfruits.message.unreadable").withStyle(ChatFormatting.GRAY), true);
            }

            return InteractionResultHolder.consume(stack);
         } else {
            return InteractionResultHolder.success(stack);
         }
      }

      public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
         tooltip.add(Component.translatable("item.devilfruits.deciphered_blueprint.hint").withStyle(ChatFormatting.GOLD));
      }
   }

   public static class Encrypted extends Item {
      public Encrypted(Properties properties) {
         super(properties);
      }

      public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
         BlueprintData.Encrypted data = (BlueprintData.Encrypted)stack.get((DataComponentType)Poneglyphs.ENCRYPTED.get());
         if (data != null && data.progress().round() > 0) {
            tooltip.add(
               Component.translatable("item.devilfruits.encrypted_blueprint.progress", new Object[]{data.progress().round()})
                  .withStyle(ChatFormatting.DARK_AQUA)
            );
         }

         tooltip.add(Component.translatable("item.devilfruits.encrypted_blueprint.hint").withStyle(ChatFormatting.GRAY));
      }
   }

   public static class Primer extends Item {
      public Primer(Properties properties) {
         super(properties);
      }

      public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
         if (level.isClientSide) {
            PoneglyphItems.openPrimerScreen.run();
         }

         return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
      }

      public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
         tooltip.add(Component.translatable("item.devilfruits.ancient_script_primer.desc").withStyle(ChatFormatting.GRAY));
      }
   }

   public static class Rubbing extends Item {
      public Rubbing(Properties properties) {
         super(properties);
      }

      public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
         BlueprintData.Rubbing data = (BlueprintData.Rubbing)stack.get((DataComponentType)Poneglyphs.RUBBING.get());
         if (data != null) {
            tooltip.add(
               Component.translatable(
                     "item.devilfruits.poneglyph_rubbing.fragment",
                     new Object[]{data.cell() + 1, Component.translatable("poneglyph.devilfruits.position." + PoneglyphItems.POSITIONS[data.cell()])}
                  )
                  .withStyle(ChatFormatting.GRAY)
            );
         }
      }
   }
}
