package com.awakenedmod.awakened.core.npc;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags.Biomes;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck.Result;
import net.neoforged.neoforge.event.level.ModifyCustomSpawnersEvent;

public final class NpcSpawner implements CustomSpawner {
   private static final int ATTEMPTS = 6;
   private static final float BEHIND_SPREAD_DEG = 100.0F;
   private static final double FRONT_DOT = 0.2;
   private static final double MIN_PLAYER_GAP = 24.0;
   private static final double EYE = 1.6;
   private int countdown;

   private NpcSpawner() {
   }

   static void register(IEventBus gameBus) {
      gameBus.addListener(NpcSpawner::onModifySpawners);
   }

   private static void onModifySpawners(ModifyCustomSpawnersEvent event) {
      if (event.getLevel().dimension() == Level.OVERWORLD) {
         event.addCustomSpawner(new NpcSpawner());
      }
   }

   public int tick(ServerLevel level, boolean spawnEnemies, boolean spawnFriendlies) {
      return this.run(level, spawnEnemies);
   }

   private int run(ServerLevel level, boolean spawnEnemies) {
      if (spawnEnemies && NpcConfig.enabled() && level.getDifficulty() != Difficulty.PEACEFUL) {
         if (--this.countdown > 0) {
            return 0;
         } else {
            this.countdown = NpcConfig.intervalTicks();
            RandomSource random = level.getRandom();
            int spawned = 0;

            for (ServerPlayer player : List.copyOf(level.players())) {
               if (!player.isSpectator()
                  && !player.isCreative()
                  && !npcNear(level, player)
                  && random.nextDouble() < NpcConfig.chance()
                  && trySpawn(level, player, random)) {
                  spawned++;
               }
            }

            return spawned;
         }
      } else {
         return 0;
      }
   }

   private static boolean npcNear(ServerLevel level, ServerPlayer player) {
      double r = (double)NpcConfig.radius();
      return !level.getEntitiesOfClass(PoweredNpc.class, new AABB(player.blockPosition()).inflate(r), npc -> npc.distanceToSqr(player) <= r * r).isEmpty();
   }

   public static boolean spawnNear(ServerLevel level, ServerPlayer player) {
      return trySpawn(level, player, level.getRandom());
   }

   private static boolean trySpawn(ServerLevel level, ServerPlayer player, RandomSource random) {
      NpcKit kit = pickKit(random);
      if (kit == null) {
         return false;
      } else {
         for (int i = 0; i < 6; i++) {
            BlockPos feet = pickSpot(level, player, random);
            if (feet != null && spawn(level, kit, feet, random)) {
               return true;
            }
         }

         return false;
      }
   }

   @Nullable
   private static NpcKit pickKit(RandomSource random) {
      List<NpcKit> kits = new ArrayList<>(NpcKits.all());
      double total = 0.0;

      for (NpcKit kit : kits) {
         total += NpcConfig.weight(kit);
      }

      if (total <= 0.0) {
         return null;
      } else {
         double pick = random.nextDouble() * total;

         for (NpcKit kit : kits) {
            pick -= NpcConfig.weight(kit);
            if (pick < 0.0) {
               return kit;
            }
         }

         return null;
      }
   }

   @Nullable
   private static BlockPos pickSpot(ServerLevel level, ServerPlayer player, RandomSource random) {
      float yaw = player.getYRot() + 180.0F + (random.nextFloat() * 2.0F - 1.0F) * 100.0F;
      int min = NpcConfig.minDistance();
      double dist = (double)min + random.nextDouble() * (double)(NpcConfig.maxDistance() - min);
      double dx = (double)(-Mth.sin(yaw * (float) (Math.PI / 180.0))) * dist;
      double dz = (double)Mth.cos(yaw * (float) (Math.PI / 180.0)) * dist;
      BlockPos column = BlockPos.containing(player.getX() + dx, player.getY(), player.getZ() + dz);
      if (!level.isPositionEntityTicking(column)) {
         return null;
      } else {
         BlockPos feet = level.getHeightmapPos(Types.MOTION_BLOCKING_NO_LEAVES, column);
         if (level.canSeeSky(feet) && NpcNav.canStand(level, feet) && !level.getBiome(feet).is(Biomes.NO_DEFAULT_MONSTERS)) {
            Vec3 body = Vec3.atBottomCenterOf(feet);

            for (ServerPlayer other : level.players()) {
               if (other.distanceToSqr(body) < 576.0) {
                  return null;
               }
            }

            return inView(level, player, body) ? null : feet;
         } else {
            return null;
         }
      }
   }

   private static boolean inView(ServerLevel level, ServerPlayer player, Vec3 body) {
      Vec3 eye = player.getEyePosition();
      Vec3 to = new Vec3(body.x - eye.x, 0.0, body.z - eye.z).normalize();
      Vec3 look = player.getViewVector(1.0F);
      Vec3 flatLook = new Vec3(look.x, 0.0, look.z);
      return !(flatLook.lengthSqr() < 1.0E-4) && !(to.dot(flatLook.normalize()) < 0.2) ? NpcNav.clearLine(level, player, eye, body.add(0.0, 1.6, 0.0)) : false;
   }

   private static boolean positionAllowed(ServerLevel level, PoweredNpc npc) {
      PositionCheck check = new PositionCheck(npc, level, MobSpawnType.NATURAL, null);
      NeoForge.EVENT_BUS.post(check);
      return check.getResult() != Result.FAIL;
   }

   private static boolean spawn(ServerLevel level, NpcKit kit, BlockPos feet, RandomSource random) {
      PoweredNpc npc = (PoweredNpc)kit.type().get().create(level);
      if (npc == null) {
         return false;
      } else {
         float yaw = random.nextFloat() * 360.0F;
         npc.moveTo((double)feet.getX() + 0.5, (double)feet.getY(), (double)feet.getZ() + 0.5, yaw, 0.0F);
         npc.setYHeadRot(yaw);
         npc.yBodyRot = yaw;
         if (level.noCollision(npc) && positionAllowed(level, npc)) {
            EventHooks.finalizeMobSpawn(npc, level, level.getCurrentDifficultyAt(feet), MobSpawnType.NATURAL, null);
            return level.addFreshEntity(npc);
         } else {
            return false;
         }
      }
   }
}
