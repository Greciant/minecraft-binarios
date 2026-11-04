package com.devilfruits.core.physics;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;

public interface PhysicsWorldBridge {
   PhysicsWorldBridge NONE = new PhysicsWorldBridge() {
      @Override
      public boolean active() {
         return false;
      }

      @Override
      public String name() {
         return "none";
      }

      @Override
      public PhysicsStructure structureAt(Level level, BlockPos pos) {
         return null;
      }

      @Override
      public List<PhysicsStructure> structuresIn(Level level, AABB worldBox) {
         return List.of();
      }

      @Override
      public PhysicsStructure structureUnder(Entity entity) {
         return null;
      }

      @Override
      public Vec3 projectOut(Level level, Vec3 pos) {
         return pos;
      }

      @Override
      public Vec3 renderPosition(Level level, Vec3 localPos, float partialTick) {
         return null;
      }

      @Override
      public Vec3 renderDirection(Level level, Vec3 localPos, Vec3 localDir, float partialTick) {
         return null;
      }
   };

   boolean active();

   String name();

   @Nullable
   PhysicsStructure structureAt(Level var1, BlockPos var2);

   List<PhysicsStructure> structuresIn(Level var1, AABB var2);

   @Nullable
   PhysicsStructure structureUnder(Entity var1);

   Vec3 projectOut(Level var1, Vec3 var2);

   @Nullable
   Vec3 renderPosition(Level var1, Vec3 var2, float var3);

   @Nullable
   Vec3 renderDirection(Level var1, Vec3 var2, Vec3 var3, float var4);

   @Nullable
   default PhysicsStructure split(ServerLevel level, PhysicsStructure s, List<BlockPos> localBlocks) {
      return null;
   }

   default Optional<StructureHit> raycast(Level level, Vec3 from, Vec3 to, @Nullable Entity source) {
      if (!this.active()) {
         return Optional.empty();
      } else {
         ClipContext ctx = source != null
            ? new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, source)
            : new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, CollisionContext.empty());
         BlockHitResult hit = level.clip(ctx);
         if (hit.getType() != Type.BLOCK) {
            return Optional.empty();
         } else {
            PhysicsStructure structure = this.structureAt(level, hit.getBlockPos());
            if (structure == null) {
               return Optional.empty();
            } else {
               Vec3 normal = structure.dirToWorld(Vec3.atLowerCornerOf(hit.getDirection().getNormal()));
               return Optional.of(new StructureHit(structure, hit.getBlockPos(), structure.toWorld(hit.getLocation()), normal));
            }
         }
      }
   }
}
