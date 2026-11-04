package com.devilfruits.compat.sable;

import com.devilfruits.core.physics.PhysicsStructure;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;

record SableStructure(SubLevel subLevel) implements PhysicsStructure {
   @Override
   public UUID id() {
      return this.subLevel.getUniqueId();
   }

   @Override
   public Level level() {
      return this.subLevel.getLevel();
   }

   @Override
   public boolean isValid() {
      return !this.subLevel.isRemoved();
   }

   @Override
   public double mass() {
      if (this.subLevel instanceof ServerSubLevel server) {
         MassData mass = server.getMassTracker();
         return mass == null ? 0.0 : Math.max(0.0, mass.getMass());
      } else {
         return 0.0;
      }
   }

   @Override
   public Vec3 toWorld(Vec3 local) {
      return this.subLevel.logicalPose().transformPosition(local);
   }

   @Override
   public Vec3 toLocal(Vec3 world) {
      return this.subLevel.logicalPose().transformPositionInverse(world);
   }

   @Override
   public Vec3 dirToWorld(Vec3 localDir) {
      return this.subLevel.logicalPose().transformNormal(localDir);
   }

   @Override
   public Vec3 dirToLocal(Vec3 worldDir) {
      return this.subLevel.logicalPose().transformNormalInverse(worldDir);
   }

   @Override
   public Vec3 velocityAt(Vec3 world) {
      return Sable.HELPER.getVelocity(this.subLevel.getLevel(), this.subLevel, this.toLocal(world));
   }

   @Nullable
   @Override
   public AABB localBounds() {
      BoundingBox3ic b = this.subLevel.getPlot().getBoundingBox();
      return b != null && b != BoundingBox3i.EMPTY && b.maxX() >= b.minX() && b.maxY() >= b.minY() && b.maxZ() >= b.minZ()
         ? new AABB((double)b.minX(), (double)b.minY(), (double)b.minZ(), (double)(b.maxX() + 1), (double)(b.maxY() + 1), (double)(b.maxZ() + 1))
         : null;
   }

   @Override
   public AABB worldBounds() {
      BoundingBox3dc b = this.subLevel.boundingBox();
      return new AABB(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ());
   }

   @Nullable
   @Override
   public Vec3 centerOfMassWorld() {
      if (this.subLevel instanceof ServerSubLevel server && server.getMassTracker() != null) {
         Vector3dc com = server.getMassTracker().getCenterOfMass();
         return com == null ? null : this.toWorld(new Vec3(com.x(), com.y(), com.z()));
      }

      return null;
   }

   @Override
   public void applyImpulse(Vec3 worldPoint, Vec3 worldImpulse) {
      if (this.subLevel instanceof ServerSubLevel server && !this.subLevel.isRemoved()) {
         RigidBodyHandle handle = RigidBodyHandle.of(server);
         if (handle != null && handle.isValid()) {
            handle.applyImpulseAtPoint(this.toLocal(worldPoint), this.dirToLocal(worldImpulse));
            return;
         }

         return;
      }
   }

   @Override
   public void addVelocity(Vec3 linear, Vec3 spin) {
      if (this.subLevel instanceof ServerSubLevel server && !this.subLevel.isRemoved() && this.subLevel.getLevel() instanceof ServerLevel level) {
         RigidBodyHandle handle = RigidBodyHandle.of(server);
         if (handle != null && handle.isValid()) {
            SubLevelContainer.getContainer(level).physicsSystem().getPipeline().wakeUp(server);
            handle.addLinearAndAngularVelocity(new Vector3d(linear.x, linear.y, linear.z), new Vector3d(spin.x, spin.y, spin.z));
            return;
         }

         return;
      }
   }
}
