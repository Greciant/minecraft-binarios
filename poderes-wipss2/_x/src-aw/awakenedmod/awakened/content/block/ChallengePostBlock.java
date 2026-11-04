package com.awakenedmod.awakened.content.block;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.content.entity.duelist.VarkosEntity;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.Fx;
import java.util.List;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class ChallengePostBlock extends Block {
   private static final int ECHO_SHARDS_COST = 3;
   private static final int CLEARANCE_RADIUS = 6;
   private static final int CRAMPED_THRESHOLD = 12;
   private static final double SPAWN_DISTANCE = 10.0;
   private static final SummonFrame.Spec FRAME = new SummonFrame.Spec(
      Blocks.POLISHED_BLACKSTONE, Blocks.IRON_BLOCK, Blocks.SOUL_LANTERN, "awakened.challenge_post.incomplete"
   );

   public ChallengePostBlock(Properties properties) {
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
      return this.offerShards(stack, level, pos, player) == InteractionResult.PASS
         ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
         : ItemInteractionResult.SUCCESS;
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (level instanceof ServerLevel server) {
         FrameHolograms.activate(server, pos);
         Component message = frameFault(server, pos);
         if (message == null) {
            message = Component.translatable(activeBoundVarkos(server, pos) != null ? "awakened.challenge_post.active" : "awakened.challenge_post.offer");
         }

         player.displayClientMessage(message, true);
      }

      return InteractionResult.SUCCESS;
   }

   private InteractionResult offerShards(ItemStack stack, Level level, BlockPos pos, Player player) {
      if (!stack.is(Items.ECHO_SHARD)) {
         return InteractionResult.PASS;
      } else if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      } else if (level instanceof ServerLevel server) {
         Component frameFault = frameFault(server, pos);
         if (frameFault != null) {
            FrameHolograms.activate(server, pos);
            player.displayClientMessage(frameFault, true);
            return InteractionResult.SUCCESS;
         } else {
            VarkosEntity existing = activeBoundVarkos(server, pos);
            if (existing != null) {
               player.displayClientMessage(Component.translatable("awakened.challenge_post.active"), true);
               return InteractionResult.SUCCESS;
            } else {
               int held = countItemInInventory(player, Items.ECHO_SHARD);
               if (held < 3) {
                  player.displayClientMessage(Component.translatable("awakened.challenge_post.need_shards", new Object[]{3}), true);
                  return InteractionResult.SUCCESS;
               } else if (isCramped(server, pos)) {
                  player.displayClientMessage(Component.translatable("entity.awakened.varkos.cramped"), true);
                  return InteractionResult.SUCCESS;
               } else {
                  if (!player.getAbilities().instabuild) {
                     shrinkItemInInventory(player, Items.ECHO_SHARD, 3);
                  }

                  summon(server, pos, player);
                  return InteractionResult.SUCCESS;
               }
            }
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   @Nullable
   private static Component frameFault(ServerLevel level, BlockPos pos) {
      return SummonFrame.firstFault(level, pos, FRAME);
   }

   private static boolean isCramped(ServerLevel level, BlockPos pos) {
      int obstructions = 0;

      for (int dx = -6; dx <= 6; dx++) {
         for (int dz = -6; dz <= 6; dz++) {
            if (dx * dx + dz * dz <= 36) {
               BlockPos check = pos.offset(dx, 0, dz);
               BlockState state = level.getBlockState(check);
               if (!state.isAir() && !state.getCollisionShape(level, check).isEmpty()) {
                  obstructions++;
               }
            }
         }
      }

      return obstructions > 12;
   }

   private static VarkosEntity activeBoundVarkos(ServerLevel level, BlockPos pos) {
      AABB search = new AABB(pos).inflate(64.0);
      List<VarkosEntity> candidates = level.getEntitiesOfClass(VarkosEntity.class, search, e -> !e.isRemoved() && pos.equals(e.postPos()));
      return candidates.isEmpty() ? null : candidates.get(0);
   }

   private static void summon(ServerLevel level, BlockPos pos, Player challenger) {
      Vec3 center = Vec3.atCenterOf(pos);
      Vfx.ring(level, center.add(0.0, 0.1, 0.0), 2.0, 9462015, 18);
      Fx.burst(level, ParticleTypes.REVERSE_PORTAL, center.add(0.0, 0.5, 0.0), 40, 1.0, 0.15);
      Fx.sound(level, center, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.HOSTILE, 1.6F, 0.5F);
      Fx.sound(level, center, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 1.2F, 0.6F);
      float dir = level.getRandom().nextFloat() * 360.0F;
      double rad = Math.toRadians((double)dir);
      double sx = center.x + Math.sin(rad) * 10.0;
      double sz = center.z + Math.cos(rad) * 10.0;
      double sy = (double)pos.getY();
      float yaw = (float)Math.toDegrees(Math.atan2(challenger.getZ() - sz, challenger.getX() - sx)) - 90.0F;
      VarkosEntity boss = new VarkosEntity(AwakenedEntities.VARKOS.get(), level);
      boss.moveTo(sx, sy, sz, yaw, 0.0F);
      boss.bindToPost(pos);
      level.addFreshEntity(boss);
      boss.beginIntroBow(challenger);
   }

   private static int countItemInInventory(Player player, Item item) {
      int total = 0;

      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
         ItemStack s = player.getInventory().getItem(i);
         if (s.is(item)) {
            total += s.getCount();
         }
      }

      return total;
   }

   private static void shrinkItemInInventory(Player player, Item item, int amount) {
      int remaining = amount;

      for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
         ItemStack s = player.getInventory().getItem(i);
         if (s.is(item)) {
            int take = Math.min(s.getCount(), remaining);
            s.shrink(take);
            remaining -= take;
         }
      }
   }
}
