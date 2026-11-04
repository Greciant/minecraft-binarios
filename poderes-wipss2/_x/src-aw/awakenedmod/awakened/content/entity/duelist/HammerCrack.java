package com.awakenedmod.awakened.content.entity.duelist;

import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.Fx;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class HammerCrack {
   private static final Block SLAB_BLOCK = Blocks.CRACKED_STONE_BRICKS;

   private HammerCrack() {
   }

   public static void tick(ServerLevel level, VarkosEntity self, List<HammerCrack.Crack> cracks) {
      Iterator<HammerCrack.Crack> it = cracks.iterator();

      while (it.hasNext()) {
         HammerCrack.Crack c = it.next();
         double prevFront = c.front;
         c.front = Math.min(c.length, c.front + c.speed);
         paint(level, c, prevFront);
         damageSwept(level, self, c);
         if (c.done()) {
            it.remove();
            self.onHammerCrackFinished(c.hitPlayers);
         }
      }
   }

   private static void paint(ServerLevel level, HammerCrack.Crack c, double prevFront) {
      Vec3 frontPos = grounded(level, at(c, c.front));
      Fx.burst(level, ParticleTypes.CRIT, frontPos.add(0.0, 0.1, 0.0), 4, c.halfWidth, 0.04);
      Fx.burst(level, VarkosMoves.VIOLET, frontPos.add(0.0, 0.15, 0.0), 3, c.halfWidth, 0.02);

      for (double d = Math.ceil(c.lastPaintedTo / 0.8) * 0.8; d <= c.front; d += 0.8) {
         Vec3 p = grounded(level, at(c, d));
         float yaw = (float)Math.toDegrees(Math.atan2(c.dir.x, c.dir.z));
         Vfx.block(level, p.add(0.0, 0.2, 0.0), SLAB_BLOCK, 0.55, 0.35, 0.55, 14, yaw);
      }

      c.lastPaintedTo = c.front;
      Fx.shake(level, frontPos, 6.0, 0.12F, 0, 6);
   }

   private static void damageSwept(ServerLevel level, VarkosEntity self, HammerCrack.Crack c) {
      Vec3 from = c.origin;
      Vec3 to = at(c, c.front);
      AABB box = new AABB(from, to).inflate(c.halfWidth + 1.0, 4.0, c.halfWidth + 1.0);
      Vec3 seg = to.subtract(from);
      double len2 = seg.lengthSqr();

      for (Player victim : level.getEntitiesOfClass(Player.class, box, VarkosEntity::isHostileTarget)) {
         if (!c.hitPlayers.contains(victim.getUUID())) {
            Vec3 p = new Vec3(victim.getX(), from.y, victim.getZ());
            double t = len2 < 1.0E-6 ? 0.0 : Mth.clamp(p.subtract(from).dot(seg) / len2, 0.0, 1.0);
            Vec3 closest = from.add(seg.scale(t));
            if (!(closest.distanceToSqr(p) > c.halfWidth * c.halfWidth)
               && !(Math.abs(victim.getY() - floorY(level, closest)) > 2.5)
               && self.hurtChallenger(level, victim, c.damage)) {
               c.hitPlayers.add(victim.getUUID());
               Fx.burst(level, ParticleTypes.CRIT, victim.position().add(0.0, (double)victim.getBbHeight() * 0.5, 0.0), 8, 0.3, 0.08);
            }
         }
      }
   }

   private static Vec3 at(HammerCrack.Crack c, double dist) {
      return c.origin.add(c.dir.scale(dist));
   }

   private static Vec3 grounded(ServerLevel level, Vec3 pos) {
      return new Vec3(pos.x, floorY(level, pos), pos.z);
   }

   static double floorY(ServerLevel level, Vec3 pos) {
      MutableBlockPos m = new MutableBlockPos(Mth.floor(pos.x), Mth.floor(pos.y) + 2, Mth.floor(pos.z));

      for (int i = 0; i < 9; i++) {
         BlockState state = level.getBlockState(m);
         if (!state.isAir() && !state.getCollisionShape(level, m).isEmpty()) {
            return (double)m.getY() + 1.0;
         }

         m.move(0, -1, 0);
      }

      return pos.y;
   }

   public static final class Crack {
      private final Vec3 origin;
      private final Vec3 dir;
      private final double length;
      private final double speed;
      private final double halfWidth;
      private final float damage;
      private double front;
      private double lastPaintedTo;
      private final Set<UUID> hitPlayers;

      public Crack(Vec3 origin, Vec3 dir, double length, double speed, double width, float damage) {
         this(origin, dir, length, speed, width, damage, new HashSet<>());
      }

      public Crack(Vec3 origin, Vec3 dir, double length, double speed, double width, float damage, Set<UUID> sharedHits) {
         Vec3 flat = new Vec3(dir.x, 0.0, dir.z);
         this.dir = flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
         this.origin = new Vec3(origin.x, origin.y, origin.z);
         this.length = length;
         this.speed = speed;
         this.halfWidth = width * 0.5;
         this.damage = damage;
         this.hitPlayers = sharedHits;
      }

      private boolean done() {
         return this.front >= this.length;
      }

      boolean sharesHits(Set<UUID> set) {
         return this.hitPlayers == set;
      }
   }
}
