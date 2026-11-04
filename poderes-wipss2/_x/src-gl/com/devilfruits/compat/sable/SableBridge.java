package com.devilfruits.compat.sable;

import com.devilfruits.core.physics.PhysicsStructure;
import com.devilfruits.core.physics.PhysicsWorldBridge;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.mass.MassTracker;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.ClientSubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.heat.SubLevelHeatMapManager;
import dev.ryanhcode.sable.sublevel.plot.heat.SubLevelHeatMapManager.SplitListener;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SableBridge implements PhysicsWorldBridge {
   @Nullable
   private static Collection<SplitListener> listeners;

   @Override
   public boolean active() {
      return true;
   }

   @Override
   public String name() {
      return "Sable";
   }

   @Nullable
   private static SubLevel containing(Level level, Vec3i pos) {
      SubLevel s = Sable.HELPER.getContaining(level, pos);
      return s != null && !s.isRemoved() ? s : null;
   }

   @Nullable
   private static PhysicsStructure wrap(@Nullable SubLevel s) {
      return s != null && !s.isRemoved() ? new SableStructure(s) : null;
   }

   @Nullable
   @Override
   public PhysicsStructure structureAt(Level level, BlockPos pos) {
      return wrap(containing(level, pos));
   }

   @Override
   public List<PhysicsStructure> structuresIn(Level level, AABB worldBox) {
      List<PhysicsStructure> out = new ArrayList<>();

      for (SubLevel s : Sable.HELPER.getAllIntersecting(level, new BoundingBox3d(worldBox))) {
         if (!s.isRemoved()) {
            out.add(new SableStructure(s));
         }
      }

      return out;
   }

   @Nullable
   @Override
   public PhysicsStructure structureUnder(Entity entity) {
      SubLevel tracked = Sable.HELPER.getTrackingOrVehicleSubLevel(entity);
      if (tracked != null && !tracked.isRemoved()) {
         return new SableStructure(tracked);
      } else {
         Vec3 feet = entity.position();
         double w = Math.max(0.3, (double)entity.getBbWidth() * 0.5);
         AABB probe = new AABB(feet.x - w, feet.y - 0.6, feet.z - w, feet.x + w, feet.y + 0.1, feet.z + w);

         for (SubLevel s : Sable.HELPER.getAllIntersecting(entity.level(), new BoundingBox3d(probe))) {
            if (!s.isRemoved()) {
               Pose3dc pose = s.logicalPose();

               for (double dy : new double[]{-0.15, -0.5}) {
                  Vec3 local = pose.transformPositionInverse(feet.add(0.0, dy, 0.0));
                  BlockPos p = BlockPos.containing(local);
                  if (!entity.level().getBlockState(p).getCollisionShape(entity.level(), p).isEmpty()) {
                     return new SableStructure(s);
                  }
               }
            }
         }

         return null;
      }
   }

   @Nullable
   @Override
   public PhysicsStructure split(ServerLevel level, PhysicsStructure s, List<BlockPos> localBlocks) {
      if (s instanceof SableStructure structure && !structure.subLevel().isRemoved() && !localBlocks.isEmpty()) {
         List<BlockPos> blocks = new ArrayList<>(localBlocks);
         BoundingBox3i from = BoundingBox3i.from(blocks);
         if (from == null) {
            return null;
         }

         BoundingBox3i bounds = from.expand(1, 1, 1);

         for (SplitListener listener : splitListeners()) {
            listener.addBlocks(level, bounds, blocks);
         }

         ServerSubLevel piece = SubLevelAssemblyHelper.assembleBlocks(level, blocks.get(0), blocks, bounds);
         MassTracker mass = piece.getSelfMassTracker();
         if (mass.getCenterOfMass() != null && !(mass.getMass() <= 0.0)) {
            return new SableStructure(piece);
         }

         piece.getPlot().destroyAllBlocks();
         SubLevelContainer.getContainer(level).removeSubLevel(piece, SubLevelRemovalReason.REMOVED);
         return null;
      }

      return null;
   }

   private static Collection<SplitListener> splitListeners() {
      if (listeners == null) {
         try {
            Field f = SubLevelHeatMapManager.class.getDeclaredField("LISTENERS");
            f.setAccessible(true);
            listeners = (Collection<SplitListener>)f.get(null);
         } catch (RuntimeException | ReflectiveOperationException var1) {
            listeners = List.of();
         }
      }

      return listeners;
   }

   @Override
   public Vec3 projectOut(Level level, Vec3 pos) {
      return Sable.HELPER.projectOutOfSubLevel(level, pos);
   }

   @Nullable
   @Override
   public Vec3 renderPosition(Level level, Vec3 localPos, float partialTick) {
      Pose3dc pose = renderPose(level, localPos, partialTick);
      return pose == null ? null : pose.transformPosition(localPos);
   }

   @Nullable
   @Override
   public Vec3 renderDirection(Level level, Vec3 localPos, Vec3 localDir, float partialTick) {
      Pose3dc pose = renderPose(level, localPos, partialTick);
      return pose == null ? null : pose.transformNormal(localDir);
   }

   @Nullable
   private static Pose3dc renderPose(Level level, Vec3 localPos, float partialTick) {
      SubLevel s = Sable.HELPER.getContaining(level, localPos);
      if (s != null && !s.isRemoved()) {
         return (Pose3dc)(s instanceof ClientSubLevelAccess client ? client.renderPose(partialTick) : s.logicalPose());
      } else {
         return null;
      }
   }
}
