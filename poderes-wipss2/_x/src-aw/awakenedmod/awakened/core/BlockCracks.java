package com.awakenedmod.awakened.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.DoubleToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;

@EventBusSubscriber(
   modid = "awakened"
)
public final class BlockCracks {
   public static final int MIN_STAGE = 0;
   public static final int MAX_STAGE = 9;
   public static final int CLEAR = -1;
   public static final int MAX_BLOCKS_PER_CALL = 64;
   public static final int MAX_ACTIVE = 1024;
   public static final double MAX_RADIUS = 12.0;
   public static final int REFRESH_TICKS = 100;
   private static final double SEND_RANGE = 48.0;
   private static final int FLOOR_SCAN = 5;
   private static final int ID_BASE = -4096;
   private static final Map<BlockCracks.Key, BlockCracks.Crack> ACTIVE = new LinkedHashMap<>();
   private static final Set<Integer> IDS_IN_USE = new HashSet<>();
   private static int nextId = -4096;
   private static int serverTicks;

   private BlockCracks() {
   }

   public static Object owner(Entity entity, String slot) {
      return new BlockCracks.Owner(entity.getUUID(), slot);
   }

   public static void crack(ServerLevel level, Object owner, BlockPos pos, int stage) {
      crack(level, owner, pos, stage, BlockCracks.Timing.STEADY);
   }

   public static boolean crack(ServerLevel level, Object owner, BlockPos pos, int stage, BlockCracks.Timing timing) {
      BlockCracks.Key key = new BlockCracks.Key(owner, level, pos.asLong());
      if (stage < 0) {
         remove(key);
         return false;
      } else if (level.isLoaded(pos) && !level.getBlockState(pos).isAir()) {
         BlockCracks.Crack crack = ACTIVE.get(key);
         if (crack == null) {
            if (ACTIVE.size() >= 1024) {
               return false;
            }

            crack = new BlockCracks.Crack(allocateId(), pos.immutable());
            crack.from = 0;
            ACTIVE.put(key, crack);
         } else {
            crack.from = Math.max(0, crack.shown);
         }

         crack.to = Math.min(9, stage);
         crack.timing = timing;
         crack.age = 0;
         int now = crack.stageNow();
         if (now != crack.shown) {
            show(level, crack, now);
         }

         return true;
      } else {
         return false;
      }
   }

   public static List<BlockPos> disc(
      ServerLevel level, Object owner, Vec3 center, double radius, DoubleToIntFunction stageAtDistance, BlockCracks.Timing timing
   ) {
      double r = Math.min(Math.max(0.0, radius), 12.0);
      int reach = (int)Math.ceil(r);
      List<double[]> columns = new ArrayList<>();
      int cx = (int)Math.floor(center.x);
      int cz = (int)Math.floor(center.z);

      for (int dx = -reach; dx <= reach; dx++) {
         for (int dz = -reach; dz <= reach; dz++) {
            double x = (double)(cx + dx) + 0.5;
            double z = (double)(cz + dz) + 0.5;
            double d = Math.sqrt((x - center.x) * (x - center.x) + (z - center.z) * (z - center.z));
            if (d <= r + 0.5) {
               columns.add(new double[]{x, z, Math.min(d, r)});
            }
         }
      }

      columns.sort((a, b) -> Double.compare(a[2], b[2]));
      List<BlockPos> cracked = new ArrayList<>();

      for (double[] c : columns) {
         if (cracked.size() >= 64) {
            break;
         }

         int stage = stageAtDistance.applyAsInt(c[2]);
         if (stage >= 0) {
            BlockPos floor = floorTop(level, c[0], center.y, c[1]);
            if (floor != null && crack(level, owner, floor, stage, timing)) {
               cracked.add(floor);
            }
         }
      }

      return cracked;
   }

   public static List<BlockPos> disc(ServerLevel level, Object owner, Vec3 center, double radius, int centreStage, int edgeStage, BlockCracks.Timing timing) {
      double r = Math.max(0.001, radius);
      return disc(level, owner, center, radius, d -> Math.round((float)centreStage + (float)(edgeStage - centreStage) * (float)(d / r)), timing);
   }

   public static List<BlockPos> ring(ServerLevel level, Object owner, Vec3 center, double innerRadius, double outerRadius, int stage, BlockCracks.Timing timing) {
      return disc(level, owner, center, outerRadius, d -> d >= innerRadius ? stage : -1, timing);
   }

   public static BlockPos floorTop(ServerLevel level, double x, double y, double z) {
      MutableBlockPos m = BlockPos.containing(x, y + 1.0, z).mutable();
      if (!level.isLoaded(m)) {
         return null;
      } else {
         for (int i = 0; i < 5; i++) {
            BlockState state = level.getBlockState(m);
            if (!state.getFluidState().isEmpty()) {
               return null;
            }

            if (!state.isAir() && !state.getCollisionShape(level, m).isEmpty()) {
               BlockPos above = m.above();
               boolean exposed = !level.getBlockState(above).isFaceSturdy(level, above, Direction.DOWN);
               boolean crackable = exposed
                  && state.isFaceSturdy(level, m, Direction.UP)
                  && state.getDestroySpeed(level, m) >= 0.0F
                  && state.getRenderShape() != RenderShape.INVISIBLE;
               return crackable ? m.immutable() : null;
            }

            m.move(Direction.DOWN);
         }

         return null;
      }
   }

   public static void clear(ServerLevel level, Object owner, BlockPos pos) {
      remove(new BlockCracks.Key(owner, level, pos.asLong()));
   }

   public static void clear(Object owner) {
      Iterator<Entry<BlockCracks.Key, BlockCracks.Crack>> it = ACTIVE.entrySet().iterator();

      while (it.hasNext()) {
         Entry<BlockCracks.Key, BlockCracks.Crack> e = it.next();
         if (e.getKey().owner().equals(owner)) {
            it.remove();
            forget(e.getKey().level(), e.getValue());
         }
      }
   }

   public static void clearExcept(Object owner, Collection<BlockPos> keep) {
      Set<Long> kept = new HashSet<>();

      for (BlockPos p : keep) {
         kept.add(p.asLong());
      }

      Iterator<Entry<BlockCracks.Key, BlockCracks.Crack>> it = ACTIVE.entrySet().iterator();

      while (it.hasNext()) {
         Entry<BlockCracks.Key, BlockCracks.Crack> e = it.next();
         if (e.getKey().owner().equals(owner) && !kept.contains(e.getKey().pos())) {
            it.remove();
            forget(e.getKey().level(), e.getValue());
         }
      }
   }

   public static void release(Object owner, int fadeTicks) {
      for (Entry<BlockCracks.Key, BlockCracks.Crack> e : ACTIVE.entrySet()) {
         if (e.getKey().owner().equals(owner)) {
            BlockCracks.Crack crack = e.getValue();
            crack.to = Math.max(0, crack.shown);
            crack.timing = BlockCracks.Timing.countDown(fadeTicks);
            crack.age = 0;
         }
      }
   }

   public static boolean has(Object owner) {
      for (BlockCracks.Key key : ACTIVE.keySet()) {
         if (key.owner().equals(owner)) {
            return true;
         }
      }

      return false;
   }

   @SubscribeEvent
   public static void onServerTick(Post event) {
      serverTicks++;
      if (!ACTIVE.isEmpty()) {
         Iterator<Entry<BlockCracks.Key, BlockCracks.Crack>> it = ACTIVE.entrySet().iterator();

         while (it.hasNext()) {
            Entry<BlockCracks.Key, BlockCracks.Crack> e = it.next();
            ServerLevel level = e.getKey().level();
            BlockCracks.Crack crack = e.getValue();
            if (level.getServer() != event.getServer()) {
               it.remove();
               IDS_IN_USE.remove(crack.id);
            } else {
               crack.age++;
               int stage = crack.stageNow();
               boolean refresh = Math.floorMod(serverTicks + crack.id, 100) == 0;
               if (stage >= 0 && (!refresh || !level.isLoaded(crack.pos) || !level.getBlockState(crack.pos).isAir())) {
                  if (stage != crack.shown || refresh) {
                     show(level, crack, stage);
                  }
               } else {
                  it.remove();
                  forget(level, crack);
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onLevelUnload(Unload event) {
      if (event.getLevel() instanceof ServerLevel level) {
         Iterator<Entry<BlockCracks.Key, BlockCracks.Crack>> it = ACTIVE.entrySet().iterator();

         while (it.hasNext()) {
            Entry<BlockCracks.Key, BlockCracks.Crack> e = it.next();
            if (e.getKey().level() == level) {
               it.remove();
               IDS_IN_USE.remove(e.getValue().id);
            }
         }
      }
   }

   @SubscribeEvent
   public static void onServerStopped(ServerStoppedEvent event) {
      ACTIVE.clear();
      IDS_IN_USE.clear();
      nextId = -4096;
   }

   private static void remove(BlockCracks.Key key) {
      BlockCracks.Crack crack = ACTIVE.remove(key);
      if (crack != null) {
         forget(key.level(), crack);
      }
   }

   private static void forget(ServerLevel level, BlockCracks.Crack crack) {
      IDS_IN_USE.remove(crack.id);
      ClientboundBlockDestructionPacket packet = new ClientboundBlockDestructionPacket(crack.id, crack.pos, -1);

      for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
         player.connection.send(packet);
      }
   }

   private static void show(ServerLevel level, BlockCracks.Crack crack, int stage) {
      crack.shown = stage;
      ClientboundBlockDestructionPacket packet = new ClientboundBlockDestructionPacket(crack.id, crack.pos, stage);
      Vec3 at = Vec3.atCenterOf(crack.pos);

      for (ServerPlayer player : level.players()) {
         if (player.distanceToSqr(at) < 2304.0) {
            player.connection.send(packet);
         }
      }
   }

   private static int allocateId() {
      int id;
      do {
         id = nextId;
         nextId = id <= -2147483647 ? -4096 : id - 1;
      } while (!IDS_IN_USE.add(id));

      return id;
   }

   private static final class Crack {
      final int id;
      final BlockPos pos;
      int from;
      int to;
      BlockCracks.Timing timing;
      int age;
      int shown = -1;

      Crack(int id, BlockPos pos) {
         this.id = id;
         this.pos = pos;
      }

      int stageNow() {
         int ramp = this.timing.rampTicks();
         if (this.age < ramp) {
            return this.from + Math.round((float)(this.to - this.from) * (float)this.age / (float)ramp);
         } else if (this.timing.holdTicks() != -1 && this.age >= ramp + this.timing.holdTicks()) {
            int f = this.age - ramp - this.timing.holdTicks();
            return f >= this.timing.fadeTicks() ? -1 : this.to - Math.round((float)(this.to + 1) * (float)f / (float)this.timing.fadeTicks());
         } else {
            return this.to;
         }
      }
   }

   private static record Key(Object owner, ServerLevel level, long pos) {
   }

   public static record Owner(UUID entity, String slot) {
   }

   public static record Timing(int rampTicks, int holdTicks, int fadeTicks) {
      public static final int FOREVER = -1;
      public static final BlockCracks.Timing STEADY = new BlockCracks.Timing(0, -1, 0);

      public Timing(int rampTicks, int holdTicks, int fadeTicks) {
         rampTicks = Math.max(0, rampTicks);
         holdTicks = holdTicks < 0 ? -1 : holdTicks;
         fadeTicks = Math.max(0, fadeTicks);
         this.rampTicks = rampTicks;
         this.holdTicks = holdTicks;
         this.fadeTicks = fadeTicks;
      }

      public static BlockCracks.Timing buildUp(int ticks) {
         return new BlockCracks.Timing(ticks, -1, 0);
      }

      public static BlockCracks.Timing countDown(int ticks) {
         return new BlockCracks.Timing(0, 0, ticks);
      }

      public static BlockCracks.Timing holdThenFade(int holdTicks, int fadeTicks) {
         return new BlockCracks.Timing(0, holdTicks, fadeTicks);
      }

      public static BlockCracks.Timing lease(int ticks) {
         return new BlockCracks.Timing(0, ticks, 0);
      }
   }
}
