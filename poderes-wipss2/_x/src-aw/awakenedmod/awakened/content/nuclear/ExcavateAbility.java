package com.awakenedmod.awakened.content.nuclear;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class ExcavateAbility extends Ability {
   private static final int UNLOCK = 8;
   private static final float COST = 5.0F;
   private static final double SPHERE_RADIUS = 2.4;
   private static final double REACH = 5.0;
   private static final int DRILL_INTERVAL = 5;
   private static final double DRILL_ADVANCE = 1.6;
   private static final float DRILL_PULSE_COST = 6.0F;
   private static final Set<Block> EXTRA_DIGGABLE = Set.of(
      Blocks.COBBLESTONE,
      Blocks.MOSSY_COBBLESTONE,
      Blocks.COBBLED_DEEPSLATE,
      Blocks.DRIPSTONE_BLOCK,
      Blocks.BLACKSTONE,
      Blocks.NETHERRACK,
      Blocks.DIRT,
      Blocks.COARSE_DIRT,
      Blocks.ROOTED_DIRT,
      Blocks.MUD,
      Blocks.GRAVEL,
      Blocks.SAND,
      Blocks.RED_SAND,
      Blocks.CLAY,
      Blocks.SOUL_SAND,
      Blocks.SOUL_SOIL,
      Blocks.MAGMA_BLOCK
   );
   private final Map<UUID, Boolean> drilling = this.activeState();

   public ExcavateAbility() {
      super("excavate", 5.0F, 8);
   }

   @Override
   public boolean activate(ServerPlayer player) {
      this.carve(player, this.digFace(player));
      this.drilling.put(player.getUUID(), Boolean.TRUE);
      return true;
   }

   @Override
   public void serverTick(ServerPlayer player) {
      if (Boolean.TRUE.equals(this.drilling.get(player.getUUID()))) {
         PowerData data = PowerService.data(player);
         long gameTime = player.serverLevel().getGameTime();
         StaminaSystem.suppressRegen(data, gameTime);
         if (player.tickCount % 5 == 0) {
            if (!data.infiniteStamina()) {
               float pulse = this.billed(6.0F);
               if (data.stamina() < pulse) {
                  this.stopDrill(player);
                  return;
               }

               StaminaSystem.drainSustained(data, gameTime, pulse);
               PowerService.sync(player);
            }

            Vec3 face = this.digFace(player).add(player.getLookAngle().scale(1.6));
            this.carve(player, face);
         }
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      this.stopDrill(player);
   }

   private void stopDrill(ServerPlayer player) {
      if (this.drilling.remove(player.getUUID()) != null) {
         PowerService.playAnimation(player, "excavate_end");
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      this.drilling.remove(player.getUUID());
      super.clearPlayer(player);
   }

   private Vec3 digFace(ServerPlayer player) {
      return AimUtil.aimedPosition(player, 5.0);
   }

   private void carve(ServerPlayer player, Vec3 center) {
      ServerLevel level = player.serverLevel();
      BlockPos origin = BlockPos.containing(center);
      int r = (int)Math.ceil(2.4);
      double r2 = 5.76;
      boolean broke = false;

      for (int dx = -r; dx <= r; dx++) {
         for (int dy = -r; dy <= r; dy++) {
            for (int dz = -r; dz <= r; dz++) {
               if (!((double)(dx * dx + dy * dy + dz * dz) > r2)) {
                  BlockPos pos = origin.offset(dx, dy, dz);
                  if (isDiggable(level.getBlockState(pos))) {
                     level.destroyBlock(pos, true, player);
                     broke = true;
                  }
               }
            }
         }
      }

      NuclearFx.boom(level, center, 2.4, NuclearFx.Kind.DUST);
      if (!broke) {
         Fx.sound(level, center, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.5F, 1.7F);
      }
   }

   private static boolean isDiggable(BlockState state) {
      return state.is(BlockTags.BASE_STONE_OVERWORLD) || EXTRA_DIGGABLE.contains(state.getBlock());
   }
}
