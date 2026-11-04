package com.awakenedmod.awakened.content.block;

import com.awakenedmod.awakened.content.vfx.BlockFxEntity;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.terrain.TempTerrain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
import net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;

@EventBusSubscriber(
   modid = "awakened"
)
public final class FrameHolograms {
   private static final float GHOST_ALPHA = 0.35F;
   private static final int RESCAN_TICKS = 10;
   private static final int GHOST_LIFETIME = 1200;
   private static final double WAKE_RADIUS = 24.0;
   private static final Map<Block, SummonFrame.Spec> PEDESTAL_TYPES = new IdentityHashMap<>();
   private static final List<FrameHolograms.Entry> ACTIVE = new ArrayList<>();

   private FrameHolograms() {
   }

   static void trackPedestalType(Block pedestal, SummonFrame.Spec spec) {
      PEDESTAL_TYPES.put(pedestal, spec);
   }

   public static void activate(ServerLevel level, BlockPos pos) {
      Block block = level.getBlockState(pos).getBlock();
      SummonFrame.Spec spec = PEDESTAL_TYPES.get(block);
      if (spec != null) {
         for (FrameHolograms.Entry entry : ACTIVE) {
            if (entry.level == level && entry.pedestal.equals(pos)) {
               entry.cooldown = 0;
               return;
            }
         }

         FrameHolograms.Entry entryx = new FrameHolograms.Entry(level, pos.immutable(), block, spec);
         ACTIVE.add(entryx);
      }
   }

   @SubscribeEvent
   public static void onServerStopped(ServerStoppedEvent event) {
      ACTIVE.clear();
   }

   @SubscribeEvent
   public static void onBlockPlaced(EntityPlaceEvent event) {
      wakeAround(event.getLevel(), event.getPos());
   }

   @SubscribeEvent
   public static void onBlockBroken(BreakEvent event) {
      if (!TempTerrain.isProbe(event)) {
         wakeAround(event.getLevel(), event.getPos());
      }
   }

   private static void wakeAround(LevelAccessor levelAccessor, BlockPos changed) {
      if (levelAccessor instanceof ServerLevel level && !PEDESTAL_TYPES.isEmpty()) {
         for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
               for (int dz = -1; dz <= 1; dz++) {
                  BlockPos p = changed.offset(dx, dy, dz);
                  if (PEDESTAL_TYPES.containsKey(level.getBlockState(p).getBlock())) {
                     activate(level, p);
                  }
               }
            }
         }

         return;
      }
   }

   @SubscribeEvent
   public static void onServerTick(Post event) {
      if (!ACTIVE.isEmpty()) {
         Iterator<FrameHolograms.Entry> it = ACTIVE.iterator();

         while (it.hasNext()) {
            FrameHolograms.Entry entry = it.next();
            if (entry.level.getServer() != event.getServer()) {
               it.remove();
            } else if (--entry.cooldown <= 0) {
               entry.cooldown = 10;
               if (!rescan(entry)) {
                  it.remove();
               }
            }
         }
      }
   }

   private static boolean rescan(FrameHolograms.Entry entry) {
      ServerLevel level = entry.level;
      BlockPos pos = entry.pedestal;
      if (!level.isLoaded(pos)) {
         entry.markers.clear();
         return true;
      } else if (!level.hasNearbyAlivePlayer((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, 24.0)) {
         despawnMarkers(entry);
         return true;
      } else if (!level.getBlockState(pos).is(entry.pedestalBlock)) {
         despawnMarkers(entry);
         return false;
      } else {
         List<SummonFrame.Slot> missing = SummonFrame.missing(level, pos, entry.spec);
         if (missing.isEmpty()) {
            despawnMarkers(entry);
            return false;
         } else {
            Map<BlockPos, FrameHolograms.Marker> markers = entry.markers;
            markers.keySet().removeIf(markerPos -> {
               boolean stillMissing = missing.stream().anyMatch(slotx -> slotx.pos().equals(markerPos));
               if (!stillMissing) {
                  markers.get(markerPos).discard();
               }

               return !stillMissing;
            });

            for (SummonFrame.Slot slot : missing) {
               BlockState occupier = level.getBlockState(slot.pos());
               boolean shell = !occupier.canBeReplaced();
               Block shown = shell ? occupier.getBlock() : slot.required();
               FrameHolograms.Marker marker = markers.get(slot.pos());
               boolean alive = marker != null && marker.alive();
               boolean expiringSoon = alive && marker.fx().lifetime() - marker.fx().tickCount <= 20;
               if (!alive || marker.shell() != shell || marker.shown() != shown || expiringSoon) {
                  if (alive) {
                     marker.discard();
                  }

                  Vec3 center = Vec3.atCenterOf(slot.pos());
                  BlockFxEntity fx = shell ? Vfx.wrongBlockShell(level, center, shown, 1200) : Vfx.ghostBlock(level, center, shown, 0.35F, 1200);
                  markers.put(slot.pos(), new FrameHolograms.Marker(fx, shown, shell));
               }
            }

            return true;
         }
      }
   }

   private static void despawnMarkers(FrameHolograms.Entry entry) {
      for (FrameHolograms.Marker marker : entry.markers.values()) {
         marker.discard();
      }

      entry.markers.clear();
   }

   private static final class Entry {
      final ServerLevel level;
      final BlockPos pedestal;
      final Block pedestalBlock;
      final SummonFrame.Spec spec;
      final Map<BlockPos, FrameHolograms.Marker> markers = new HashMap<>();
      int cooldown;

      Entry(ServerLevel level, BlockPos pedestal, Block pedestalBlock, SummonFrame.Spec spec) {
         this.level = level;
         this.pedestal = pedestal;
         this.pedestalBlock = pedestalBlock;
         this.spec = spec;
      }
   }

   private static record Marker(BlockFxEntity fx, Block shown, boolean shell) {
      boolean alive() {
         return !this.fx.isRemoved();
      }

      void discard() {
         if (this.alive()) {
            this.fx.discard();
         }
      }
   }
}
