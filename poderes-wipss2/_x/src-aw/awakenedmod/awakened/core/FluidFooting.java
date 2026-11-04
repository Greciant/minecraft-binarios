package com.awakenedmod.awakened.core;

import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(
   modid = "awakened"
)
public final class FluidFooting {
   public static final double SURFACE_TOP = 0.9375;
   public static final VoxelShape SHAPE = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.9375, 1.0);
   public static final double WADE_DEPTH = 0.4;
   private static final List<FluidFooting.Source> SOURCES = new CopyOnWriteArrayList<>(List.of(FluidFooting::heldUp));
   private static final Map<LivingEntity, Long> LAST_HELD = new WeakHashMap<>();
   private static final Set<LivingEntity> SETTLING = Collections.newSetFromMap(new WeakHashMap<>());

   private FluidFooting() {
   }

   public static void register(FluidFooting.Source source) {
      SOURCES.add(source);
   }

   public static boolean canStand(LivingEntity body, FluidState fluid) {
      if (!fluid.isEmpty() && (body instanceof Player || isHeldUp(body))) {
         if (!fluid.is(FluidTags.WATER) && !fluid.is(FluidTags.LAVA)) {
            return false;
         } else {
            for (FluidFooting.Source source : SOURCES) {
               if (source.canStand(body, fluid)) {
                  return true;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   @Nullable
   public static VoxelShape floor(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof LivingEntity body) {
         FluidState fluid = state.getFluidState();
         if (fluid.isSource() && canStand(body, fluid)) {
            if (level.getFluidState(pos.above()).getType().isSame(fluid.getType())) {
               return null;
            }

            return context.isAbove(SHAPE, pos, true) ? SHAPE : null;
         }

         return null;
      }

      return null;
   }

   public static boolean wading(LivingEntity body) {
      return body.getFluidHeight(FluidTags.WATER) >= 0.4 || body.getFluidHeight(FluidTags.LAVA) >= 0.4;
   }

   public static boolean standingOnFluid(LivingEntity body) {
      if (!body.onGround()) {
         return false;
      } else {
         BlockPos pos = body.blockPosition();
         BlockState state = body.level().getBlockState(pos);
         return state.getBlock() instanceof LiquidBlock && state.getFluidState().isSource() && Math.abs(body.getY() - ((double)pos.getY() + 0.9375)) < 0.001;
      }
   }

   public static boolean stillAtFeet(LivingEntity body) {
      FluidState fluid = body.level().getFluidState(body.blockPosition());
      return fluid.isSource() && (fluid.is(FluidTags.WATER) || fluid.is(FluidTags.LAVA));
   }

   public static void holdUp(LivingEntity body) {
      LAST_HELD.put(body, body.level().getGameTime());
      if (!Boolean.TRUE.equals(body.getExistingDataOrNull(AwakenedAttachments.FLUID_FOOTING.get()))) {
         body.setData(AwakenedAttachments.FLUID_FOOTING.get(), Boolean.TRUE);
      }
   }

   public static void letGo(LivingEntity body) {
      Long last = LAST_HELD.get(body);
      if (last != null && last == body.level().getGameTime()) {
         SETTLING.add(body);
      } else {
         drop(body);
      }
   }

   @SubscribeEvent
   public static void onServerTick(Post event) {
      if (!SETTLING.isEmpty()) {
         Iterator<LivingEntity> it = SETTLING.iterator();

         while (it.hasNext()) {
            LivingEntity body = it.next();
            Long last = LAST_HELD.get(body);
            if (body.isRemoved() || last == null || last < body.level().getGameTime()) {
               it.remove();
               drop(body);
            }
         }
      }
   }

   private static void drop(LivingEntity body) {
      LAST_HELD.remove(body);
      if (body.getExistingDataOrNull(AwakenedAttachments.FLUID_FOOTING.get()) != null) {
         body.removeData(AwakenedAttachments.FLUID_FOOTING.get());
      }
   }

   public static boolean isHeldUp(LivingEntity body) {
      return Boolean.TRUE.equals(body.getExistingDataOrNull(AwakenedAttachments.FLUID_FOOTING.get()));
   }

   private static boolean heldUp(LivingEntity body, FluidState fluid) {
      return isHeldUp(body) && (!(body instanceof Player player) || !player.isShiftKeyDown()) && !wading(body);
   }

   @FunctionalInterface
   public interface Source {
      boolean canStand(LivingEntity var1, FluidState var2);
   }
}
