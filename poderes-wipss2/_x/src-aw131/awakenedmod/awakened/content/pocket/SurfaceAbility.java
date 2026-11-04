package com.awakenedmod.awakened.content.pocket;

import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.TeleportGuard;
import com.awakenedmod.awakened.core.TeleportUtil;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;

public final class SurfaceAbility extends Ability {
   private static final float COST = 18.0F;
   private static final int UNLOCK = 8;
   private static final int NETHER_CEILING = 120;

   public SurfaceAbility() {
      super("surface", 18.0F, 8);
   }

   @Override
   public float staminaCost(ServerPlayer player) {
      return StaminaSystem.baseMax();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      int x = player.getBlockX();
      int z = player.getBlockZ();
      int feetY = player.getBlockY();
      int top = level.dimension() == Level.NETHER ? 120 : level.getHeight(Types.MOTION_BLOCKING, x, z);
      if (top <= feetY) {
         return this.fail(player, "awakened.pocket.surface_top", new Object[0]);
      } else {
         Vec3 dest = highestSafeSpot(level, player, x, z, feetY, top);
         if (dest == null || dest.y <= (double)feetY + 0.5) {
            return this.fail(player, "awakened.pocket.surface_blocked", new Object[0]);
         } else if (TeleportGuard.crosses(player, player.position(), dest)) {
            return this.fail(player, "awakened.pocket.surface_sealed", new Object[0]);
         } else {
            RiftFx.flatMawFlash(level, player.position().add(0.0, (double)player.getBbHeight() + 0.4, 0.0), 1.1, 18);
            Fx.burst(level, ParticleTypes.PORTAL, player.getEyePosition(), 40, 0.5, 0.4);
            player.teleportTo(dest.x, dest.y, dest.z);
            RiftFx.flatMawFlash(level, dest.add(0.0, 0.1, 0.0), 1.1, 24);
            player.setDeltaMovement(0.0, 0.25, 0.0);
            player.hurtMarked = true;
            player.resetFallDistance();
            RiftSpace.arrivalFx(level, dest);
            player.swing(InteractionHand.MAIN_HAND);
            player.displayClientMessage(Component.translatable("awakened.pocket.surface"), true);
            MasterySystem.award(player, MasteryReason.SURVIVAL);
            return true;
         }
      }
   }

   private static Vec3 highestSafeSpot(ServerLevel level, ServerPlayer player, int x, int z, int feetY, int top) {
      Vec3 submergedBackup = null;

      for (int y = top; y > feetY; y--) {
         Vec3 feet = new Vec3((double)x + 0.5, (double)y, (double)z + 0.5);
         if (TeleportUtil.fits(player, feet)) {
            BlockPos feetPos = new BlockPos(x, y, z);
            BlockPos headPos = feetPos.above();
            BlockPos floorPos = feetPos.below();
            BlockState floor = level.getBlockState(floorPos);
            boolean solidFloor = !floor.getCollisionShape(level, floorPos).isEmpty();
            boolean floorHazard = floor.getFluidState().is(FluidTags.LAVA) || floor.is(BlockTags.FIRE) || floor.is(Blocks.MAGMA_BLOCK);
            if (solidFloor && !floorHazard && !isHazardSpace(level, feetPos) && !isHazardSpace(level, headPos)) {
               boolean dry = level.getFluidState(feetPos).isEmpty() && level.getFluidState(headPos).isEmpty();
               if (dry) {
                  return feet;
               }

               if (submergedBackup == null) {
                  submergedBackup = feet;
               }
            }
         }
      }

      return submergedBackup;
   }

   private static boolean isHazardSpace(ServerLevel level, BlockPos pos) {
      return level.getFluidState(pos).is(FluidTags.LAVA) || level.getBlockState(pos).is(BlockTags.FIRE);
   }
}
