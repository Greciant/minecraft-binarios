package com.awakenedmod.awakened.core;

import com.awakenedmod.awakened.core.voidview.VoidZones;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class AimUtil {
   public static final float LOOK_DOWN_PITCH = 45.0F;
   private static final double GROUND_WEDGE_STEP = 3.0;
   private static final int GROUND_CLIMB = 6;
   private static final int GROUND_DROP = 16;
   private static final double NEAR_PICK = 2.0;
   public static final double BODY_HEIGHT = 1.85;

   private AimUtil() {
   }

   public static boolean wantsSelfLaunch(ServerPlayer player) {
      return player.isShiftKeyDown() || player.getXRot() >= 45.0F;
   }

   public static List<LivingEntity> livingAround(ServerPlayer player, Vec3 center, double radius) {
      return player.serverLevel()
         .getEntitiesOfClass(
            LivingEntity.class, new AABB(center, center).inflate(radius), e -> e != player && e.isAlive() && e.distanceToSqr(center) <= radius * radius
         );
   }

   public static List<LivingEntity> livingAlong(ServerPlayer player, Vec3 from, Vec3 to, double radius) {
      return player.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(radius + 1.0), e -> {
         if (e != player && e.isAlive()) {
            AABB reach = e.getBoundingBox().inflate(radius);
            return reach.contains(from) || reach.clip(from, to).isPresent();
         } else {
            return false;
         }
      });
   }

   public static List<LivingEntity> livingInFrustum(ServerPlayer player, Vec3 apex, Vec3 dir, double reach, double radiusNear, double radiusFar) {
      Vec3 axis = dir.normalize();
      double widest = Math.max(radiusNear, radiusFar);
      AABB search = new AABB(apex, apex.add(axis.scale(reach))).inflate(widest + 1.0);
      List<LivingEntity> caught = new ArrayList<>();

      for (LivingEntity e : player.serverLevel().getEntitiesOfClass(LivingEntity.class, search, c -> c != player && c.isAlive())) {
         AABB box = e.getBoundingBox();
         Vec3 center = box.getCenter();
         Vec3 half = new Vec3(box.getXsize() * 0.5, box.getYsize() * 0.5, box.getZsize() * 0.5);
         Vec3 toCenter = center.subtract(apex);
         double along = toCenter.dot(axis);
         double reachAlong = support(half, axis);
         if (!(along + reachAlong < 0.0) && !(along - reachAlong > reach)) {
            double width = reach <= 1.0E-6 ? radiusNear : radiusNear + (radiusFar - radiusNear) * (Mth.clamp(along, 0.0, reach) / reach);
            Vec3 perp = toCenter.subtract(axis.scale(along));
            double perpLen = perp.length();
            double gap = perpLen - (perpLen < 1.0E-6 ? 0.0 : support(half, perp.scale(1.0 / perpLen)));
            if (!(gap > width) && clipBlocks(player.serverLevel(), player, apex, center).getType() != Type.BLOCK) {
               caught.add(e);
            }
         }
      }

      return caught;
   }

   private static double support(Vec3 half, Vec3 unit) {
      return Math.abs(unit.x) * half.x + Math.abs(unit.y) * half.y + Math.abs(unit.z) * half.z;
   }

   public static List<LivingEntity> livingInGroundWedge(
      ServerPlayer player, Vec3 apex, Vec3 flatDir, double length, double halfNear, double halfFar, double heightAbove
   ) {
      Vec3 flat = new Vec3(flatDir.x, 0.0, flatDir.z);
      Vec3 axis = flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
      double widest = Math.max(halfNear, halfFar);
      AABB search = new AABB(apex, apex.add(axis.scale(length))).inflate(widest + 1.0, heightAbove + 3.0, widest + 1.0);
      int scanDown = (int)Math.ceil(heightAbove) + 2;
      ServerLevel level = player.serverLevel();
      List<LivingEntity> caught = new ArrayList<>();

      for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, search, c -> c != player && c.isAlive())) {
         double dx = e.getX() - apex.x;
         double dz = e.getZ() - apex.z;
         double along = dx * axis.x + dz * axis.z;
         if (!(along < 0.0) && !(along > length)) {
            double perp = Math.abs(dx * axis.z - dz * axis.x);
            double half = length <= 1.0E-6 ? halfNear : halfNear + (halfFar - halfNear) * (along / length);
            if (!(perp > half)) {
               Fx.Floor floor = Fx.floorUnder(level, e.getX(), e.getY() + 0.01, e.getZ(), scanDown);
               if (floor != null) {
                  double above = e.getY() - floor.topY();
                  if (!(above < -1.0) && !(above > heightAbove) && !(Math.abs(floor.topY() - apex.y) > 3.0)) {
                     caught.add(e);
                  }
               }
            }
         }
      }

      return caught;
   }

   public static BlockHitResult clipBlocks(Level level, @Nullable Entity shooter, Vec3 from, Vec3 to) {
      return clip(level, from, to, Block.COLLIDER, Fluid.NONE, shooter);
   }

   private static BlockHitResult clip(Level level, Vec3 from, Vec3 to, Block block, Fluid fluid, @Nullable Entity entity) {
      if (VoidZones.inside(level, from.x, from.y, from.z)) {
         Vec3 exit = VoidZones.exit(level, from, to);
         if (exit == null) {
            return BlockHitResult.miss(to, Direction.UP, BlockPos.containing(to));
         }

         from = exit;
      }

      return level.clip(new ClipContext(from, to, block, fluid, entity == null ? CollisionContext.empty() : CollisionContext.of(entity)));
   }

   public static LivingEntity aimedLivingCone(ServerPlayer player, double range, double maxAngleDeg, Predicate<LivingEntity> filter) {
      return conePick(player, player.serverLevel(), range, maxAngleDeg, filter).filtered();
   }

   private static AimUtil.Pick conePick(Player viewer, Level level, double range, double maxAngleDeg, Predicate<LivingEntity> filter) {
      Vec3 eye = viewer.getEyePosition();
      Vec3 look = viewer.getLookAngle().normalize();
      double margin = range * Math.sin(Math.toRadians(maxAngleDeg)) + 1.0;
      AABB box = viewer.getBoundingBox().expandTowards(look.scale(range)).inflate(margin);
      LivingEntity best = null;
      boolean sawAny = false;
      double coneCos = Math.cos(Math.toRadians(maxAngleDeg));
      double bestCos = coneCos;

      for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, c -> c != viewer && c.isAlive())) {
         Vec3 toCenter = e.getBoundingBox().getCenter().subtract(eye);
         double dist = toCenter.length();
         if (!(dist > range) && !(dist < 0.001)) {
            double cos = toCenter.scale(1.0 / dist).dot(look);
            if (!(cos <= coneCos) && clipBlocks(level, viewer, eye, e.getBoundingBox().getCenter()).getType() != Type.BLOCK) {
               sawAny = true;
               if (filter.test(e) && cos > bestCos) {
                  bestCos = cos;
                  best = e;
               }
            }
         }
      }

      return new AimUtil.Pick(best, sawAny);
   }

   public static LivingEntity aimedLivingEntity(ServerPlayer player, double range) {
      return aimedLivingEntity(player, range, e -> true);
   }

   public static LivingEntity aimedLivingSoft(ServerPlayer player, double range, double coneDeg, Predicate<LivingEntity> filter) {
      return aimedLivingSoftPick(player, range, coneDeg, filter).target();
   }

   public static AimUtil.SoftPick aimedLivingSoftPick(ServerPlayer player, double range, double coneDeg, Predicate<LivingEntity> filter) {
      return softPick(player, player.serverLevel(), range, coneDeg, filter);
   }

   public static LivingEntity aimedLivingSoft(Player viewer, Level level, double range, double coneDeg, Predicate<LivingEntity> filter) {
      return softPick(viewer, level, range, coneDeg, filter).target();
   }

   private static AimUtil.SoftPick softPick(Player viewer, Level level, double range, double coneDeg, Predicate<LivingEntity> filter) {
      AimUtil.Pick ray = rayPick(viewer, level, range, filter);
      if (ray.filtered() != null) {
         return new AimUtil.SoftPick(ray.filtered(), true);
      } else {
         AimUtil.Pick cone = conePick(viewer, level, range, coneDeg, filter);
         return new AimUtil.SoftPick(cone.filtered(), ray.sawAny() || cone.sawAny());
      }
   }

   public static LivingEntity aimedLivingEntity(ServerPlayer player, double range, Predicate<LivingEntity> filter) {
      return rayPick(player, player.serverLevel(), range, filter).filtered();
   }

   private static AimUtil.Pick rayPick(Player viewer, Level level, double range, Predicate<LivingEntity> filter) {
      Vec3 eye = viewer.getEyePosition();
      Vec3 look = viewer.getLookAngle();
      Vec3 end = eye.add(look.scale(range));
      AABB search = viewer.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
      LivingEntity best = null;
      boolean sawAny = false;
      double bestDistSqr = range * range;

      for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, search, e -> e != viewer && e.isAlive())) {
         Optional<Vec3> hit = candidate.getBoundingBox().inflate(0.3).clip(eye, end);
         if (hit.isPresent() && eye.distanceToSqr(hit.get()) <= range * range) {
            sawAny = true;
            double distSqr = eye.distanceToSqr(hit.get());
            if (filter.test(candidate) && distSqr < bestDistSqr) {
               bestDistSqr = distSqr;
               best = candidate;
            }
         }
      }

      return new AimUtil.Pick(best, sawAny);
   }

   public static Vec3 aimedPosition(ServerPlayer player, double range) {
      Vec3 eye = player.getEyePosition();
      Vec3 end = eye.add(player.getViewVector(1.0F).scale(range));
      BlockHitResult hit = clipBlocks(player.serverLevel(), player, eye, end);
      return hit.getType() == Type.MISS ? end : hit.getLocation();
   }

   @Nullable
   public static Vec3 groundedPosition(ServerPlayer player, double range, int maxDrop) {
      Vec3 eye = player.getEyePosition();
      HitResult hit = clip(player.serverLevel(), eye, eye.add(player.getLookAngle().scale(range)), Block.OUTLINE, Fluid.NONE, player);
      Vec3 loc = hit.getLocation();
      if (hit.getType() == Type.BLOCK) {
         return loc;
      } else {
         ServerLevel level = player.serverLevel();
         MutableBlockPos pos = BlockPos.containing(loc).mutable();

         for (int i = 0; i < maxDrop; i++) {
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
               return new Vec3(loc.x, (double)pos.getY() + 1.0, loc.z);
            }

            pos.move(0, -1, 0);
         }

         return null;
      }
   }

   public static Vec3 horizontalDir(Vec3 from, Vec3 to) {
      Vec3 d = new Vec3(to.x - from.x, 0.0, to.z - from.z);
      return d.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : d.normalize();
   }

   public static Vec3 lookFlat(Entity entity) {
      Vec3 look = entity.getLookAngle();
      Vec3 flat = new Vec3(look.x, 0.0, look.z);
      if (flat.lengthSqr() < 1.0E-4) {
         double yaw = Math.toRadians((double)entity.getYRot());
         return new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
      } else {
         return flat.normalize();
      }
   }

   public static Vec3 castOrigin(ServerPlayer player) {
      return player.getEyePosition().add(player.getLookAngle().scale(0.9)).add(0.0, -0.25, 0.0);
   }

   public static Vec3 groundedPosition(ServerLevel level, Vec3 p) {
      return scanToGround(level, p).point();
   }

   public static Optional<Vec3> groundedPositionIfFound(ServerLevel level, Vec3 p) {
      AimUtil.GroundScan scan = scanToGround(level, p);
      return scan.found() ? Optional.of(scan.point()) : Optional.empty();
   }

   private static AimUtil.GroundScan scanToGround(ServerLevel level, Vec3 p) {
      MutableBlockPos pos = new MutableBlockPos(Mth.floor(p.x), Mth.floor(p.y), Mth.floor(p.z));
      int guard = 0;

      while (guard++ < 6 && !level.getBlockState(pos).isAir()) {
         pos.move(0, 1, 0);
      }

      boolean found = !level.getBlockState(pos.below()).isAir();

      for (int var5 = 0; !found && var5++ < 16; found = !level.getBlockState(pos.below()).isAir()) {
         pos.move(0, -1, 0);
      }

      return new AimUtil.GroundScan(new Vec3(p.x, (double)pos.getY(), p.z), found);
   }

   public static Vec3 handPosition(ServerPlayer player) {
      return handPosition(player, false);
   }

   public static Vec3 handPosition(ServerPlayer player, boolean offHand) {
      return handPosition(player, offHand, 0.42, 0.28, 0.55);
   }

   public static Vec3 handPosition(Player player, boolean offHand, double sideOut, double down, double forward) {
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle();
      Vec3 right = horizontalRight((double)player.getYRot());
      return eye.add(look.scale(forward)).add(right.scale(sideOut * handSign(player, offHand))).add(0.0, -down, 0.0);
   }

   public static Vec3 horizontalRight(double yawDeg) {
      double yaw = Math.toRadians(yawDeg);
      return new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
   }

   public static Vec3 direction(double yawDeg, double pitchDeg) {
      double yaw = Math.toRadians(yawDeg);
      double pitch = Math.toRadians(pitchDeg);
      double c = Math.cos(pitch);
      return new Vec3(-Math.sin(yaw) * c, -Math.sin(pitch), Math.cos(yaw) * c);
   }

   public static double handSign(LivingEntity body, boolean offHand) {
      double mainSign = body.getMainArm() == HumanoidArm.RIGHT ? 1.0 : -1.0;
      return mainSign * (offHand ? -1.0 : 1.0);
   }

   public static Vec3 shoulderHand(Vec3 eye, double yawDeg, double armPitchDeg, double side, double halfWidth, double drop, double reach) {
      Vec3 shoulder = eye.add(0.0, -drop, 0.0).add(horizontalRight(yawDeg).scale(halfWidth * side));
      return shoulder.add(direction(yawDeg, armPitchDeg).scale(reach));
   }

   public static Vec3 shoulderHand(Player player, boolean offHand, double halfWidth, double drop, double reach) {
      return shoulderHand(player.getEyePosition(), (double)player.getYRot(), (double)player.getXRot(), handSign(player, offHand), halfWidth, drop, reach);
   }

   public static Vec3 throwDirection(ServerPlayer player, Vec3 origin, double range) {
      return throwDirection(player.serverLevel(), player, player.getEyePosition(), player.getLookAngle(), origin, range);
   }

   public static Vec3 throwDirection(Level level, Entity shooter, Vec3 eye, Vec3 look, Vec3 origin, double range) {
      Vec3 reach = eye.add(look.x * range, look.y * range, look.z * range);
      Vec3 target = clip(level, eye, reach, Block.OUTLINE, Fluid.NONE, shooter).getLocation();
      return converge(target, eye, look, origin);
   }

   private static Vec3 converge(Vec3 target, Vec3 eye, Vec3 look, Vec3 origin) {
      Vec3 dir = target.subtract(origin);
      return !(target.distanceToSqr(eye) < 4.0) && !(dir.lengthSqr() < 1.0E-6) ? dir.normalize() : look;
   }

   public static Vec3 throwDirectionAtBody(ServerPlayer player, Vec3 origin, double range, double coneDeg) {
      LivingEntity body = aimedLivingSoft(player, range, coneDeg, e -> true);
      if (body != null) {
         Vec3 centre = body.getBoundingBox().getCenter();
         Vec3 dir = centre.subtract(origin);
         boolean inSight = clipBlocks(player.serverLevel(), player, player.getEyePosition(), centre).getType() == Type.MISS;
         if (inSight && dir.lengthSqr() > 1.0E-6) {
            return dir.normalize();
         }
      }

      return throwDirection(player, origin, range);
   }

   @Nullable
   public static Vec3 wallWithin(LivingEntity target, Vec3 dir, double dist) {
      if (target.level() instanceof ServerLevel level) {
         Vec3 flat = new Vec3(dir.x, 0.0, dir.z);
         if (flat.lengthSqr() < 1.0E-6) {
            return null;
         } else {
            Vec3 from = target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0);
            Vec3 to = from.add(flat.normalize().scale(dist + (double)target.getBbWidth() * 0.5));
            BlockHitResult hit = clipBlocks(level, target, from, to);
            return hit.getType() == Type.BLOCK && hit.getDirection().getAxis().isHorizontal() ? hit.getLocation() : null;
         }
      } else {
         return null;
      }
   }

   public static AimUtil.Stand standPoint(Player body, double range, double wallOff) {
      Level level = body.level();
      Vec3 eye = body.getEyePosition();
      Vec3 end = eye.add(body.getLookAngle().scale(range));
      BlockHitResult block = clipBlocks(level, body, eye, end);
      Vec3 limit = block.getType() == Type.MISS ? end : block.getLocation();
      Entity hitEntity = null;
      double best = Double.MAX_VALUE;
      AABB sweep = new AABB(eye, limit).inflate(1.0);

      for (Entity e : level.getEntities(body, sweep, ex -> ex.isPickable() && !ex.isSpectator() && ex != body.getVehicle())) {
         Optional<Vec3> clip = e.getBoundingBox().inflate((double)e.getPickRadius() + 0.3).clip(eye, limit);
         if (clip.isPresent()) {
            double d = eye.distanceToSqr(clip.get());
            if (d < best) {
               best = d;
               hitEntity = e;
            }
         }
      }

      if (hitEntity != null) {
         Vec3 back = eye.subtract(hitEntity.position()).multiply(1.0, 0.0, 1.0);
         back = back.lengthSqr() < 1.0E-6 ? Vec3.ZERO : back.normalize();
         Vec3 at = hitEntity.position().add(back.scale((double)(hitEntity.getBbWidth() / 2.0F) + 0.6));
         return new AimUtil.Stand(at, Direction.UP, hitEntity.onGround());
      } else if (block.getType() == Type.MISS) {
         return new AimUtil.Stand(end.subtract(0.0, (double)body.getEyeHeight(), 0.0), Direction.UP, false);
      } else {
         Direction face = block.getDirection();
         Vec3 hit = block.getLocation();

         Vec3 point = switch (face) {
            case UP -> hit.add(0.0, 0.02, 0.0);
            case DOWN -> hit.subtract(0.0, 1.85, 0.0);
            default -> hit.add((double)face.getStepX() * wallOff, -0.9, (double)face.getStepZ() * wallOff);
         };
         return new AimUtil.Stand(point, face, true);
      }
   }

   public static double reachToBox(Entity e, Vec3 point) {
      AABB box = e.getBoundingBox();
      double x = Mth.clamp(point.x, box.minX, box.maxX);
      double y = Mth.clamp(point.y, box.minY, box.maxY);
      double z = Mth.clamp(point.z, box.minZ, box.maxZ);
      return Math.sqrt(point.distanceToSqr(x, y, z));
   }

   private static record GroundScan(Vec3 point, boolean found) {
   }

   private static record Pick(@Nullable LivingEntity filtered, boolean sawAny) {
   }

   public static record SoftPick(@Nullable LivingEntity target, boolean sawCandidate) {
      public boolean found() {
         return this.target != null;
      }

      public boolean rejected() {
         return this.target == null && this.sawCandidate;
      }
   }

   public static record Stand(Vec3 point, Direction face, boolean surface) {
   }
}
