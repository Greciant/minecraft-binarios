package com.awakenedmod.awakened.content.item;

import com.awakenedmod.awakened.content.entity.archevoker.ArchEvokerMoves;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.progression.BalanceKeyed;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class ArchEvokerStaffItem extends Item implements BalanceKeyed {
   private static final int FANG_COOLDOWN = 55;
   private static final double FANG_RANGE = 22.0;
   private static final int FANG_COUNT = 11;

   public ArchEvokerStaffItem(Properties properties) {
      super(properties);
   }

   @Override
   public List<String> balanceKeys() {
      return List.of("ArchEvokerStaffFangs");
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack held = player.getItemInHand(hand);
      if (player instanceof ServerPlayer sp) {
         if (sp.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(held);
         } else {
            this.castFangLine(sp);
            sp.getCooldowns().addCooldown(this, 55);
            sp.swing(hand);
            return InteractionResultHolder.success(held);
         }
      } else {
         return InteractionResultHolder.success(held);
      }
   }

   private void castFangLine(ServerPlayer sp) {
      ServerLevel level = Dmg.serverLevel(sp);
      Vec3 aim = AimUtil.aimedPosition(sp, 22.0);
      ArchEvokerMoves.fangLineToward(level, sp, sp.position(), aim, 11, 1.6, 1.5, 1);
      Fx.sound(level, sp.position(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.0F);
   }
}
