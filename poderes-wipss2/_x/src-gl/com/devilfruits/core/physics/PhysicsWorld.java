package com.devilfruits.core.physics;

import com.devilfruits.DevilFruits;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.fml.ModList;

public final class PhysicsWorld {
   public static final String SABLE = "sable";
   private static PhysicsWorldBridge bridge = PhysicsWorldBridge.NONE;

   public static void init() {
      if (!ModList.get().isLoaded("sable")) {
         DevilFruits.LOGGER.info("Physics structures: Sable not installed, structure synergy off");
      } else {
         try {
            bridge = (PhysicsWorldBridge)Class.forName("com.devilfruits.compat.sable.SableBridge").getDeclaredConstructor().newInstance();
            DevilFruits.LOGGER.info("Physics structures: bound to {}", bridge.name());
         } catch (Throwable var1) {
            bridge = PhysicsWorldBridge.NONE;
            DevilFruits.LOGGER.error("Physics structures: Sable is installed but its API could not be bound; structures are ignored", var1);
         }
      }
   }

   public static PhysicsWorldBridge bridge() {
      return bridge;
   }

   public static boolean active() {
      return bridge.active() && (Boolean)PhysicsConfig.ENABLED.get();
   }

   @Nullable
   public static PhysicsStructure structureAt(Level level, BlockPos pos) {
      return bridge.active() ? bridge.structureAt(level, pos) : null;
   }

   @Nullable
   public static PhysicsStructure structureUnder(Entity entity) {
      return bridge.active() ? bridge.structureUnder(entity) : null;
   }

   public static BlockHitResult clip(Level level, ClipContext ctx) {
      BlockHitResult hit = level.clip(ctx);
      if (bridge.active() && hit.getType() == Type.BLOCK) {
         PhysicsStructure structure = bridge.structureAt(level, hit.getBlockPos());
         return structure == null ? hit : new BlockHitResult(structure.toWorld(hit.getLocation()), hit.getDirection(), hit.getBlockPos(), hit.isInside());
      } else {
         return hit;
      }
   }

   public static Vec3 worldLocation(Level level, BlockHitResult hit) {
      PhysicsStructure s = bridge.active() && hit.getType() == Type.BLOCK ? bridge.structureAt(level, hit.getBlockPos()) : null;
      return s == null ? hit.getLocation() : s.toWorld(hit.getLocation());
   }

   public static Vec3 worldNormal(Level level, BlockHitResult hit) {
      Vec3 n = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
      PhysicsStructure s = bridge.active() && hit.getType() == Type.BLOCK ? bridge.structureAt(level, hit.getBlockPos()) : null;
      return s == null ? n : s.dirToWorld(n);
   }

   public static boolean collidesWithStructure(Level level, AABB box) {
      if (!bridge.active()) {
         return false;
      } else {
         MutableBlockPos m = new MutableBlockPos();
         AABB near = box.inflate(0.5);

         for (PhysicsStructure s : bridge.structuresIn(level, box)) {
            AABB lb = s.localBounds();
            AABB local = localBox(s, box);
            if (lb != null && local.intersects(lb)) {
               local = local.intersect(lb);

               for (int x = Mth.floor(local.minX); x < Mth.ceil(local.maxX); x++) {
                  for (int y = Mth.floor(local.minY); y < Mth.ceil(local.maxY); y++) {
                     for (int z = Mth.floor(local.minZ); z < Mth.ceil(local.maxZ); z++) {
                        BlockState state = level.getBlockState(m.set(x, y, z));
                        if (!state.isAir()
                           && !state.getCollisionShape(level, m).isEmpty()
                           && near.contains(s.toWorld(new Vec3((double)x + 0.5, (double)y + 0.5, (double)z + 0.5)))) {
                           return true;
                        }
                     }
                  }
               }
            }
         }

         return false;
      }
   }

   @Nullable
   public static BlockState blockUnder(Entity entity, double depth) {
      PhysicsStructure s = structureUnder(entity);
      return s == null ? null : entity.level().getBlockState(BlockPos.containing(s.toLocal(entity.position().add(0.0, -depth, 0.0))));
   }

   public static Vec3 worldCenter(Level level, BlockPos pos) {
      Vec3 c = Vec3.atCenterOf(pos);
      return bridge.active() ? bridge.projectOut(level, c) : c;
   }

   public static Vec3 carrierVelocity(Entity entity) {
      if (active() && (Boolean)PhysicsConfig.INHERIT_VELOCITY.get()) {
         PhysicsStructure s = bridge.structureUnder(entity);
         return s == null ? Vec3.ZERO : s.velocityAt(entity.position()).scale(0.05);
      } else {
         return Vec3.ZERO;
      }
   }

   public static List<StructureBlock> blocksIn(Level level, ImpactShape shape, @Nullable Predicate<PhysicsStructure> filter, int limit) {
      if (active() && limit > 0) {
         List<StructureBlock> out = new ArrayList<>();
         int maxScan = (Integer)PhysicsConfig.MAX_SCAN_VOLUME.get();
         MutableBlockPos m = new MutableBlockPos();

         for (PhysicsStructure s : bridge.structuresIn(level, shape.bounds())) {
            if (s.isValid() && (filter == null || filter.test(s))) {
               AABB lb = s.localBounds();
               if (lb != null) {
                  AABB local = localBox(s, shape.bounds());
                  if (local.intersects(lb)) {
                     local = local.intersect(lb);
                     int x0 = Mth.floor(local.minX);
                     int y0 = Mth.floor(local.minY);
                     int z0 = Mth.floor(local.minZ);
                     int x1 = Mth.ceil(local.maxX) - 1;
                     int y1 = Mth.ceil(local.maxY) - 1;
                     int z1 = Mth.ceil(local.maxZ) - 1;
                     long volume = (long)(x1 - x0 + 1) * (long)(y1 - y0 + 1) * (long)(z1 - z0 + 1);
                     int step = volume > (long)maxScan ? (int)Math.ceil(Math.cbrt((double)volume / (double)maxScan)) : 1;

                     for (int x = x0; x <= x1; x += step) {
                        for (int y = y0; y <= y1; y += step) {
                           for (int z = z0; z <= z1; z += step) {
                              BlockState state = level.getBlockState(m.set(x, y, z));
                              if (!state.isAir()) {
                                 Vec3 world = s.toWorld(new Vec3((double)x + 0.5, (double)y + 0.5, (double)z + 0.5));
                                 double power = shape.power(world);
                                 if (power > 0.0) {
                                    out.add(new StructureBlock(s, m.immutable(), state, world, power));
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         if (out.size() > 1) {
            out.sort((a, b) -> Double.compare(b.power(), a.power()));
         }

         return (List<StructureBlock>)(out.size() > limit ? new ArrayList<>(out.subList(0, limit)) : out);
      } else {
         return List.of();
      }
   }

   public static AABB localBox(PhysicsStructure s, AABB world) {
      double minX = Double.MAX_VALUE;
      double minY = Double.MAX_VALUE;
      double minZ = Double.MAX_VALUE;
      double maxX = -Double.MAX_VALUE;
      double maxY = -Double.MAX_VALUE;
      double maxZ = -Double.MAX_VALUE;

      for (int i = 0; i < 8; i++) {
         Vec3 c = s.toLocal(new Vec3((i & 1) == 0 ? world.minX : world.maxX, (i & 2) == 0 ? world.minY : world.maxY, (i & 4) == 0 ? world.minZ : world.maxZ));
         minX = Math.min(minX, c.x);
         minY = Math.min(minY, c.y);
         minZ = Math.min(minZ, c.z);
         maxX = Math.max(maxX, c.x);
         maxY = Math.max(maxY, c.y);
         maxZ = Math.max(maxZ, c.z);
      }

      return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
   }

   private PhysicsWorld() {
   }
}
