package com.awakenedmod.awakened.content;

import com.awakenedmod.awakened.core.AwakenedThreat;
import com.awakenedmod.awakened.core.Fx;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public final class TrackerItem extends Item {
   private static final double SCAN_RADIUS = 256.0;
   private static final int COOLDOWN_TICKS = 30;
   private static final String[] CARDINALS = new String[]{"e", "se", "s", "sw", "w", "nw", "n", "ne"};

   public TrackerItem(Properties properties) {
      super(properties);
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack held = player.getItemInHand(hand);
      if (player instanceof ServerPlayer sp && level instanceof ServerLevel server) {
         sp.getCooldowns().addCooldown(held.getItem(), 30);
         LivingEntity nearest = findNearestThreat(server, sp);
         if (nearest == null) {
            sp.displayClientMessage(Component.translatable("awakened.tracker.none").withStyle(ChatFormatting.GRAY), true);
            Fx.sound(server, sp.position(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.7F, 0.7F);
            return InteractionResultHolder.success(held);
         }

         double dist = Math.sqrt(sp.distanceToSqr(nearest));
         Component dir = Component.translatable("awakened.tracker.dir." + cardinal(nearest.getX() - sp.getX(), nearest.getZ() - sp.getZ()));
         sp.displayClientMessage(
            Component.translatable("awakened.tracker.found", new Object[]{nearest.getDisplayName(), (int)Math.round(dist), dir}).withStyle(ChatFormatting.RED),
            true
         );
         float pitch = (float)(0.7 + (1.0 - Math.min(1.0, dist / 256.0)) * 0.9);
         Fx.sound(server, sp.position(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.9F, pitch);
         return InteractionResultHolder.success(held);
      }

      return InteractionResultHolder.success(held);
   }

   private static LivingEntity findNearestThreat(ServerLevel level, Player player) {
      AABB box = player.getBoundingBox().inflate(256.0);
      List<Monster> found = level.getEntitiesOfClass(Monster.class, box, e -> e instanceof AwakenedThreat && e.isAlive());
      LivingEntity best = null;
      double bestSq = Double.MAX_VALUE;

      for (Monster m : found) {
         double sq = player.distanceToSqr(m);
         if (sq < bestSq) {
            bestSq = sq;
            best = m;
         }
      }

      return best;
   }

   private static String cardinal(double dx, double dz) {
      double angle = Math.toDegrees(Math.atan2(dz, dx));
      int step = ((int)Math.round(angle / 45.0) % 8 + 8) % 8;
      return CARDINALS[step];
   }
}
