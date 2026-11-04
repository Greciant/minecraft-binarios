package com.awakenedmod.awakened.content.pocket;

import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

public final class RiftSpace {
   public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, AwId.of("pocket"));
   public static final BlockPos ENTRY = new BlockPos(0, 65, 0);
   private static final int PLATFORM_RADIUS = 4;
   private static final String EXILED_TAG = "awakened_rift_exiled";
   private static final int CELL_SPACING = 512;
   private static final int CELLS_PER_AXIS = 2048;
   private static final String RET_DIM = "pocketReturnDim";
   private static final String RET_X = "pocketReturnX";
   private static final String RET_Y = "pocketReturnY";
   private static final String RET_Z = "pocketReturnZ";
   private static final String RET_YAW = "pocketReturnYaw";
   private static final String RET_PITCH = "pocketReturnPitch";

   private RiftSpace() {
   }

   public static ServerLevel riftLevel(MinecraftServer server) {
      return server == null ? null : server.getLevel(DIMENSION);
   }

   public static boolean isInRift(Entity entity) {
      return entity.level().dimension().equals(DIMENSION);
   }

   public static boolean isExiledBy(Entity entity, UUID owner) {
      return isInRift(entity) && entity.getTags().contains(exileTag(owner));
   }

   private static String exileTag(UUID owner) {
      return "awakened_rift_exiled_" + owner;
   }

   private static void clearExileMarks(Entity entity) {
      for (String tag : List.copyOf(entity.getTags())) {
         if (tag.startsWith("awakened_rift_exiled")) {
            entity.removeTag(tag);
         }
      }
   }

   public static BlockPos entryFor(UUID owner) {
      int h = owner.hashCode();
      int gx = Math.floorMod(h, 2048);
      int gz = Math.floorMod(h >>> 11, 2048);
      return new BlockPos(gx * 512, ENTRY.getY(), gz * 512);
   }

   public static boolean enter(ServerPlayer player) {
      ServerLevel rift = riftLevel(player.serverLevel().getServer());
      if (rift == null) {
         return false;
      } else {
         recordReturn(player);
         clearExileMarks(player);
         BlockPos home = entryFor(player.getUUID());
         ensurePlatform(rift, home);
         Vec3 dest = Vec3.atBottomCenterOf(home);
         boolean ok = teleport(player, rift, dest, player.getYRot(), player.getXRot());
         if (ok) {
            arrivalFx(rift, dest);
         }

         return ok;
      }
   }

   public static boolean returnHome(ServerPlayer player) {
      MinecraftServer server = player.serverLevel().getServer();
      if (server == null) {
         return false;
      } else {
         RiftSpace.Landing landing = recordedLanding(server, PowerService.data(player), player.getYRot(), player.getXRot());
         Vec3 from = player.position();
         Fx.burst(player.serverLevel(), ParticleTypes.PORTAL, player.getEyePosition(), 40, 0.6, 0.4);
         boolean ok = teleport(player, landing.level(), landing.pos(), landing.yaw(), landing.pitch());
         if (ok) {
            clearExileMarks(player);
            player.resetFallDistance();
            arrivalFx(landing.level(), landing.pos());
         } else {
            Fx.burst(player.serverLevel(), ParticleTypes.PORTAL, from, 10, 0.4, 0.2);
         }

         return ok;
      }
   }

   public static boolean evict(LivingEntity foe, ServerPlayer owner, boolean dramatic) {
      MinecraftServer server = owner.level().getServer();
      if (server == null) {
         return false;
      } else {
         RiftSpace.Landing landing = recordedLanding(server, PowerService.data(owner), foe.getYRot(), foe.getXRot());
         Vec3 spot = landing.pos().add(2.5, 0.0, 2.5);
         Entity out = teleportEntity(foe, landing.level(), spot, landing.yaw(), landing.pitch());
         if (out == null) {
            return false;
         } else {
            clearExileMarks(out);
            if (dramatic) {
               arrivalFx(landing.level(), spot);
            }

            return true;
         }
      }
   }

   private static RiftSpace.Landing recordedLanding(MinecraftServer server, PowerData data, float defYaw, float defPitch) {
      float yaw = data.var("pocketReturnYaw", defYaw);
      float pitch = data.var("pocketReturnPitch", defPitch);
      String dimId = data.strVar("pocketReturnDim", "");
      if (!dimId.isEmpty()) {
         ServerLevel dest = server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimId)));
         if (dest != null) {
            return new RiftSpace.Landing(
               dest,
               new Vec3((double)data.var("pocketReturnX", 0.0F), (double)data.var("pocketReturnY", 64.0F), (double)data.var("pocketReturnZ", 0.0F)),
               yaw,
               pitch
            );
         }
      }

      ServerLevel overworld = server.overworld();
      return new RiftSpace.Landing(overworld, Vec3.atBottomCenterOf(overworld.getSharedSpawnPos()), yaw, pitch);
   }

   public static boolean sendEntity(LivingEntity target, UUID owner, MinecraftServer server, boolean grabbed) {
      ServerLevel rift = riftLevel(server);
      if (rift == null) {
         return false;
      } else {
         if (target instanceof ServerPlayer victim) {
            recordReturn(victim);
         }

         BlockPos home = entryFor(owner);
         ensurePlatform(rift, home);
         Vec3 dest = scatteredSpot(rift, home);
         if (grabbed) {
            Fx.burst((ServerLevel)target.level(), ParticleTypes.PORTAL, target.getEyePosition(), 40, 0.6, 0.4);
         }

         Entity moved = teleportEntity(target, rift, dest, target.getYRot(), target.getXRot());
         if (moved == null) {
            return false;
         } else {
            clearExileMarks(moved);
            moved.addTag(exileTag(owner));
            if (moved instanceof ServerPlayer victim) {
               victim.displayClientMessage(Component.translatable("awakened.pocket.cast_out"), false);
            }

            if (grabbed) {
               arrivalFx(rift, dest);
            }

            return true;
         }
      }
   }

   private static void recordReturn(ServerPlayer player) {
      PowerData data = PowerService.data(player);
      data.setStrVar("pocketReturnDim", player.serverLevel().dimension().location().toString());
      data.setVar("pocketReturnX", (float)player.getX());
      data.setVar("pocketReturnY", (float)player.getY());
      data.setVar("pocketReturnZ", (float)player.getZ());
      data.setVar("pocketReturnYaw", player.getYRot());
      data.setVar("pocketReturnPitch", player.getXRot());
   }

   public static void ensurePlatform(ServerLevel level, BlockPos center) {
      BlockPos floor = center.below();
      if (level.getBlockState(floor).isAir()) {
         BlockState core = Blocks.OBSIDIAN.defaultBlockState();
         BlockState edge = Blocks.OBSIDIAN.defaultBlockState();
         BlockState lamp = Blocks.SEA_LANTERN.defaultBlockState();
         int y = floor.getY();

         for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
               BlockPos p = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
               boolean rim = Math.abs(dx) == 4 || Math.abs(dz) == 4;
               boolean corner = Math.abs(dx) == 4 && Math.abs(dz) == 4;
               level.setBlockAndUpdate(p, corner ? lamp : (rim ? edge : core));
            }
         }
      }
   }

   public static void ensurePlatform(ServerLevel level) {
      ensurePlatform(level, ENTRY);
   }

   private static Vec3 scatteredSpot(ServerLevel rift, BlockPos center) {
      double angle = rift.random.nextDouble() * Math.PI * 2.0;
      double radius = 2.0 + rift.random.nextDouble() * 1.5;
      return new Vec3((double)center.getX() + 0.5 + Math.cos(angle) * radius, (double)center.getY(), (double)center.getZ() + 0.5 + Math.sin(angle) * radius);
   }

   static boolean teleport(Entity entity, ServerLevel level, Vec3 pos, float yaw, float pitch) {
      return teleportEntity(entity, level, pos, yaw, pitch) != null;
   }

   static Entity teleportEntity(Entity entity, ServerLevel level, Vec3 pos, float yaw, float pitch) {
      return entity.changeDimension(new DimensionTransition(level, pos, Vec3.ZERO, yaw, pitch, DimensionTransition.DO_NOTHING));
   }

   static void arrivalFx(ServerLevel level, Vec3 pos) {
      Fx.burst(level, ParticleTypes.PORTAL, pos.add(0.0, 1.0, 0.0), 60, 0.7, 0.5);
      Fx.burst(level, ParticleTypes.REVERSE_PORTAL, pos.add(0.0, 1.0, 0.0), 30, 0.5, 0.3);
      Fx.sound(level, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5F, 0.9F);
   }

   private static record Landing(ServerLevel level, Vec3 pos, float yaw, float pitch) {
   }
}
