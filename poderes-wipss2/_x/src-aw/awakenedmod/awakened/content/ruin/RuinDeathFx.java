package com.awakenedmod.awakened.content.ruin;

import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.Lab;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public final class RuinDeathFx {
   private static final Lab.Value PIECES = Lab.SERVER.def("ruin.death.pieces", 7.0, 1.0, 20.0, 0.5);
   private static final int MIN_PIECES = 4;
   private static final int MAX_PIECES = 16;
   private static final Lab.Value SIZE = Lab.SERVER.def("ruin.death.size", 0.32, 0.1, 0.8, 0.02);
   private static final Lab.Value LIFE = Lab.SERVER.def("ruin.death.life", 50.0, 10.0, 160.0, 5.0);
   private static final int CRUMBLE_TICKS = 15;
   private static final int GROUND_SEARCH = 8;

   private RuinDeathFx() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(EventPriority.LOWEST, RuinDeathFx::onDeath);
   }

   private static void onDeath(LivingDeathEvent event) {
      if (!event.isCanceled()) {
         LivingEntity body = event.getEntity();
         if (body.level() instanceof ServerLevel level && RuinStacks.stacks(body) > 0) {
            Vec3 feet = body.position();
            double floorY = groundY(level, feet);
            double width = (double)body.getBbWidth();
            double height = (double)body.getBbHeight();
            int count = Mth.clamp((int)Math.round(PIECES.get() * width * height), 4, 16);
            double size = SIZE.get() * Math.max(0.5, width);
            int life = LIFE.getI();
            RandomSource r = level.random;

            for (int i = 0; i < count; i++) {
               double f = ((double)i + r.nextDouble()) / (double)count;
               int delay = (int)Math.round((1.0 - f) * 15.0);
               Vec3 at = feet.add((r.nextDouble() - 0.5) * width, f * height, (r.nextDouble() - 0.5) * width);
               double s = size * (0.6 + r.nextDouble() * 0.7);
               Vec3 drift = new Vec3((r.nextDouble() - 0.5) * 0.12, 0.02 + r.nextDouble() * 0.05, (r.nextDouble() - 0.5) * 0.12);
               BlockState ash = r.nextInt(3) == 0 ? Blocks.GRAY_CONCRETE_POWDER.defaultBlockState() : Blocks.LIGHT_GRAY_CONCRETE_POWDER.defaultBlockState();
               int pieceLife = life + r.nextInt(Math.max(1, life / 3));
               RuinFxClock.after(
                  level,
                  delay,
                  () -> Vfx.blockThrown(
                        level,
                        at,
                        ash,
                        s,
                        s * (0.5 + r.nextDouble() * 0.4),
                        s,
                        pieceLife,
                        r.nextFloat() * 360.0F,
                        drift,
                        floorY + s * 0.2,
                        6.0F + r.nextFloat() * 14.0F
                     )
               );
            }

            RuinCues.crumble(level, feet);
            return;
         }
      }
   }

   private static double groundY(ServerLevel level, Vec3 feet) {
      MutableBlockPos pos = BlockPos.containing(feet.x, feet.y - 0.01, feet.z).mutable();
      int i = 0;

      while (i < 8) {
         VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
         if (!shape.isEmpty()) {
            return (double)pos.getY() + shape.max(Axis.Y);
         }

         i++;
         pos.move(0, -1, 0);
      }

      return feet.y;
   }
}
