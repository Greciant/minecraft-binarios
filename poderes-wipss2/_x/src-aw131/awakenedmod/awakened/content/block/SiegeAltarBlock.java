package com.awakenedmod.awakened.content.block;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.content.entity.LeviathanEntity;
import com.awakenedmod.awakened.content.relic.RelicItem;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.Fx;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class SiegeAltarBlock extends Block {
   private static final SummonFrame.Spec FRAME = new SummonFrame.Spec(
      Blocks.CRYING_OBSIDIAN, Blocks.AMETHYST_BLOCK, Blocks.GLOWSTONE, "awakened.siege_altar.incomplete"
   );

   private static boolean isSacrifice(ItemStack stack) {
      return stack.getItem() instanceof RelicItem;
   }

   public SiegeAltarBlock(Properties properties) {
      super(properties);
      FrameHolograms.trackPedestalType(this, FRAME);
   }

   protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
      super.onPlace(state, level, pos, oldState, movedByPiston);
      if (level instanceof ServerLevel server) {
         FrameHolograms.activate(server, pos);
      }
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit
   ) {
      return this.offerRelic(stack, level, pos, player) == InteractionResult.PASS
         ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
         : ItemInteractionResult.SUCCESS;
   }

   private InteractionResult offerRelic(ItemStack stack, Level level, BlockPos pos, Player player) {
      if (!isSacrifice(stack)) {
         return InteractionResult.PASS;
      } else if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      } else if (level instanceof ServerLevel server) {
         if (!hasOpenSky(server, pos)) {
            player.displayClientMessage(Component.translatable("awakened.siege_altar.no_sky"), true);
            return InteractionResult.SUCCESS;
         } else {
            Component frameFault = frameFault(server, pos);
            if (frameFault != null) {
               FrameHolograms.activate(server, pos);
               player.displayClientMessage(frameFault, true);
               return InteractionResult.SUCCESS;
            } else {
               if (!player.getAbilities().instabuild) {
                  stack.shrink(1);
               }

               summon(server, pos);
               return InteractionResult.SUCCESS;
            }
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (level instanceof ServerLevel server) {
         FrameHolograms.activate(server, pos);
         Component fault = (Component)(!hasOpenSky(server, pos) ? Component.translatable("awakened.siege_altar.no_sky") : frameFault(server, pos));
         player.displayClientMessage((Component)(fault != null ? fault : Component.translatable("awakened.siege_altar.offer")), true);
      }

      return InteractionResult.SUCCESS;
   }

   private static boolean hasOpenSky(ServerLevel level, BlockPos pos) {
      return level.canSeeSky(pos.above());
   }

   @Nullable
   private static Component frameFault(ServerLevel level, BlockPos pos) {
      return SummonFrame.firstFault(level, pos, FRAME);
   }

   private static void summon(ServerLevel level, BlockPos pos) {
      Vec3 center = Vec3.atCenterOf(pos);
      Vfx.pillar(level, center.add(0.0, -0.4, 0.0), 1.4, 9.0, 16756768, 40);
      Vfx.dome(level, center, 6.0, 16738838, 30);
      Fx.burst(level, ParticleTypes.REVERSE_PORTAL, center.add(0.0, 1.0, 0.0), 80, 1.5, 0.2);
      Fx.burst(level, ParticleTypes.LAVA, center, 30, 1.0, 0.1);
      Fx.sound(level, center, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.4F, 0.6F);
      Fx.sound(level, center, SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 1.6F, 0.5F);
      LeviathanEntity boss = new LeviathanEntity(AwakenedEntities.LEVIATHAN.get(), level);
      boss.moveTo(center.x, (double)(pos.getY() + 1), center.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
      level.addFreshEntity(boss);
   }
}
