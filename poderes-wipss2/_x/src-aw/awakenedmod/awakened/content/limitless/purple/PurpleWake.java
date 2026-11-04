package com.awakenedmod.awakened.content.limitless.purple;

import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.terrain.RemovedBlock;
import java.util.List;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class PurpleWake {
   private static final DustParticleOptions DUST = Fx.dust(9059583, 1.4F);
   private static final DustParticleOptions ASH = Fx.dust(3875922, 1.8F);
   private static final double AIR_RING = 0.9;

   private PurpleWake() {
   }

   static void crumble(ServerLevel level, List<RemovedBlock> blocks, Vec3 centre) {
      int n = blocks.size();
      int take = Math.min(14, n);

      for (int i = 0; i < take; i++) {
         RemovedBlock b = blocks.get(take == n ? i : level.random.nextInt(n));
         Vec3 p = Vec3.atCenterOf(b.pos());
         Vec3 in = centre.subtract(p).normalize();
         level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, b.state()), p.x, p.y, p.z, 0, in.x, in.y, in.z, 1.0);
         if ((i & 1) == 0) {
            level.sendParticles(DUST, p.x, p.y, p.z, 2, 0.3, 0.3, 0.3, 0.0);
         }
      }
   }

   static void ash(ServerLevel level, Entity body) {
      AABB box = body.getBoundingBox();
      Vec3 c = box.getCenter();
      double w = box.getXsize() * 0.35;
      double h = box.getYsize() * 0.35;
      double d = box.getZsize() * 0.35;
      int n = Mth.clamp((int)(box.getXsize() * box.getYsize() * box.getZsize() * 14.0), 8, 30);
      level.sendParticles(ASH, c.x, c.y, c.z, n, w, h, d, 0.0);
      level.sendParticles(DUST, c.x, c.y, c.z, n / 2, w, h, d, 0.0);
   }

   static void speck(ServerLevel level, Vec3 at) {
      level.sendParticles(DUST, at.x, at.y, at.z, 3, 0.12, 0.12, 0.12, 0.0);
   }

   static void pressure(ServerLevel level, Vec3 c, Vec3 fwd, double r, PurpleStrength strength) {
      Vec3 u = across(fwd);
      Vec3 v = fwd.cross(u).normalize();
      int points = (int)Math.round(8.0 * strength.radius() / PurpleStrength.NORMAL.radius());
      double phase = level.random.nextDouble() * Math.PI * 2.0;
      MutableBlockPos m = new MutableBlockPos();

      for (int i = 0; i < points; i++) {
         double a = phase + (Math.PI * 2) * (double)i / (double)points;
         Vec3 dir = u.scale(Math.cos(a)).add(v.scale(Math.sin(a)));
         Vec3 wall = c.add(dir.scale(r + 0.6));
         BlockState st = level.getBlockState(m.set(wall.x, wall.y, wall.z));
         if (!st.isAir() && st.getFluidState().isEmpty()) {
            Vec3 at = c.add(dir.scale(r - 0.1));
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, st), at.x, at.y, at.z, 2, 0.25, 0.25, 0.25, 0.08);
         }
      }

      double reach = 5.0;
      Fx.Floor floor = Fx.floorUnder(level, c.x, c.y - r - 0.5, c.z, (int)Math.ceil(reach) + 1);
      if (floor != null) {
         double gap = Math.max(0.0, c.y - r - floor.topY());
         if (!(gap > reach)) {
            float k = (float)(1.0 - gap / reach);
            Vec3 at = new Vec3(c.x, floor.topY() + 0.05, c.z);
            Fx.debrisFan(level, new BlockParticleOption(ParticleTypes.BLOCK, floor.state()), at, 2 + Math.round(5.0F * k), 5.0, 30.0, 0.2, 0.5);
            level.sendParticles(ASH, at.x, at.y + 0.2, at.z, 1 + Math.round(3.0F * k), r * 0.5, 0.1, r * 0.5, 0.0);
         }
      }
   }

   static void launch(ServerLevel level, Entity caster, Vec3 hands, Vec3 fwd, PurpleStrength strength) {
      double k = strength.ballScale();
      Vec3 u = across(fwd);
      Vec3 v = fwd.cross(u).normalize();
      int air = (int)Math.round(14.0 * k);
      double phase = level.random.nextDouble() * Math.PI * 2.0;
      double speed = 0.16 * k;

      for (int i = 0; i < air; i++) {
         double a = phase + (Math.PI * 2) * (double)i / (double)air;
         Vec3 dir = u.scale(Math.cos(a)).add(v.scale(Math.sin(a)));
         Vec3 at = hands.add(dir.scale(0.9 * k));
         level.sendParticles((i & 1) == 0 ? ParticleTypes.POOF : ParticleTypes.CLOUD, at.x, at.y, at.z, 0, dir.x * speed, dir.y * speed, dir.z * speed, 1.0);
      }

      Fx.Floor floor = Fx.floorUnder(level, caster.getX(), caster.getY() + 0.5, caster.getZ(), 3);
      if (floor != null && !(caster.getY() - floor.topY() > 1.5)) {
         int ground = (int)Math.round(10.0 * k);
         BlockParticleOption dirt = new BlockParticleOption(ParticleTypes.BLOCK, floor.state());
         double y = floor.topY() + 0.1;

         for (int i = 0; i < ground; i++) {
            double a = phase + (Math.PI * 2) * (double)i / (double)ground;
            double cx = Math.cos(a);
            double cz = Math.sin(a);
            double x = caster.getX() + cx * 0.6;
            double z = caster.getZ() + cz * 0.6;
            level.sendParticles(dirt, x, y, z, 0, cx * 0.35 * k, 0.12, cz * 0.35 * k, 1.0);
            if ((i & 1) == 0) {
               level.sendParticles(ParticleTypes.POOF, x, y + 0.1, z, 0, cx * 0.12 * k, 0.02, cz * 0.12 * k, 1.0);
            }
         }
      }
   }

   private static Vec3 across(Vec3 fwd) {
      Vec3 ref = Math.abs(fwd.y) < 0.95 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
      return ref.cross(fwd).normalize();
   }
}
