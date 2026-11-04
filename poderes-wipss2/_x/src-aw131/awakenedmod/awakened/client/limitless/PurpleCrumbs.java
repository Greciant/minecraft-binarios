package com.awakenedmod.awakened.client.limitless;

import com.awakenedmod.awakened.client.render.limitless.LimitlessFrame;
import com.awakenedmod.awakened.client.render.limitless.purple.PurpleFragments;
import com.awakenedmod.awakened.content.limitless.purple.PurpleSphereEntity;
import com.awakenedmod.awakened.core.Lab;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

final class PurpleCrumbs {
   private static final Lab.Value PER_TICK = Lab.CLIENT.def("limitless.purple.crumbs_per_tick", 22.0, 0.0, 80.0, 1.0);
   private static final double LEAD = 1.0;
   private static final int BODY_RGB = 4863312;
   private static final int ITEM_RGB = 8018586;
   private static final Int2ObjectOpenHashMap<PurpleCrumbs.Sphere> SPHERES = new Int2ObjectOpenHashMap();
   private static final RandomSource RANDOM = RandomSource.create();

   private PurpleCrumbs() {
   }

   static void tick(ClientLevel level, PurpleSphereEntity entity) {
      PurpleCrumbs.Sphere s = (PurpleCrumbs.Sphere)SPHERES.get(entity.getId());
      if (s == null || s.entity != entity) {
         s = new PurpleCrumbs.Sphere(entity);
         SPHERES.put(entity.getId(), s);
      }

      s.seen = true;
      int now = (int)level.getGameTime();
      s.shards.removeIf(sh -> now - sh.born() >= 10);
      double age = PurpleFragments.age((double)now, entity.releaseTick()) + 1.0;
      if (!(age >= (double)entity.gone())) {
         Vec3 c = entity.strength().centre(entity.merge(), entity.fwd(), age, entity.gone());
         double r = entity.strength().radius(age);
         List<BlockPos> solid = new ArrayList<>();
         List<BlockState> states = new ArrayList<>();
         MutableBlockPos m = new MutableBlockPos();

         for (int x = Mth.floor(c.x - r); x <= Mth.floor(c.x + r); x++) {
            for (int y = Mth.floor(c.y - r); y <= Mth.floor(c.y + r); y++) {
               for (int z = Mth.floor(c.z - r); z <= Mth.floor(c.z + r); z++) {
                  double dx = (double)x + 0.5 - c.x;
                  double dy = (double)y + 0.5 - c.y;
                  double dz = (double)z + 0.5 - c.z;
                  if (!(dx * dx + dy * dy + dz * dz > r * r) && s.looked.add(BlockPos.asLong(x, y, z))) {
                     BlockState state = level.getBlockState(m.set(x, y, z));
                     if (takeable(level, m, state)) {
                        solid.add(m.immutable());
                        states.add(state);
                     }
                  }
               }
            }
         }

         int budget = PER_TICK.getI();

         for (int i = 0; i < solid.size() && budget > 0; i++) {
            if (RANDOM.nextInt(solid.size() - i) < budget) {
               budget--;
               BlockPos pos = solid.get(i);
               Vec3 from = Vec3.atCenterOf(pos).add(jitter(0.35), jitter(0.35), jitter(0.35));
               int rgb = darken(states.get(i).getMapColor(level, pos).col);
               add(s, from, now, rgb, 0.16F + 0.18F * RANDOM.nextFloat());
            }
         }
      }
   }

   static void endTick() {
      Iterator<PurpleCrumbs.Sphere> it = SPHERES.values().iterator();

      while (it.hasNext()) {
         PurpleCrumbs.Sphere s = it.next();
         if (!s.seen) {
            it.remove();
         } else {
            s.seen = false;
         }
      }
   }

   static void onLeave(EntityLeaveLevelEvent event) {
      if (event.getLevel().isClientSide() && !SPHERES.isEmpty()) {
         Entity e = event.getEntity();
         boolean body = e instanceof LivingEntity && !(e instanceof Player);
         if (body || e instanceof ItemEntity || e instanceof Projectile) {
            int now = (int)event.getLevel().getGameTime();
            AABB box = e.getBoundingBox();
            Vec3 at = box.getCenter();
            ObjectIterator var6 = SPHERES.values().iterator();

            while (var6.hasNext()) {
               PurpleCrumbs.Sphere s = (PurpleCrumbs.Sphere)var6.next();
               PurpleSphereEntity sphere = s.entity;
               double age = PurpleFragments.age((double)now, sphere.releaseTick());
               if (!(age >= (double)sphere.gone()) && sphere.level() == event.getLevel()) {
                  double reach = sphere.strength().radius(age) + 1.0 + box.getSize() * 0.5;
                  if (!(at.distanceToSqr(sphere.centre(0.0F)) > reach * reach)) {
                     int n = body ? Mth.clamp((int)(box.getXsize() * box.getYsize() * box.getZsize() * 24.0), 6, 28) : 4;

                     for (int i = 0; i < n; i++) {
                        Vec3 from = new Vec3(
                           Mth.lerp(RANDOM.nextDouble(), box.minX, box.maxX),
                           Mth.lerp(RANDOM.nextDouble(), box.minY, box.maxY),
                           Mth.lerp(RANDOM.nextDouble(), box.minZ, box.maxZ)
                        );
                        add(s, from, now, body ? 4863312 : 8018586, body ? 0.14F + 0.16F * RANDOM.nextFloat() : 0.1F);
                     }

                     return;
                  }
               }
            }
         }
      }
   }

   static void draw(LimitlessFrame f, PurpleSphereEntity entity) {
      PurpleCrumbs.Sphere s = (PurpleCrumbs.Sphere)SPHERES.get(entity.getId());
      if (s != null && s.entity == entity && !s.shards.isEmpty()) {
         PurpleFragments.draw(
            f, s.shards, entity.fwd(), t -> entity.strength().centre(entity.merge(), entity.fwd(), PurpleFragments.age(t, entity.releaseTick()), entity.gone())
         );
      }
   }

   static void clear() {
      SPHERES.clear();
   }

   private static boolean takeable(ClientLevel level, BlockPos pos, BlockState state) {
      return !state.isAir()
         && state.getFluidState().isEmpty()
         && !state.hasBlockEntity()
         && !(state.getBlock() instanceof FallingBlock)
         && state.getDestroySpeed(level, pos) >= 0.0F;
   }

   private static void add(PurpleCrumbs.Sphere s, Vec3 from, int now, int rgb, float size) {
      float spin = (RANDOM.nextBoolean() ? 1.0F : -1.0F) * (1.2F + 1.6F * RANDOM.nextFloat());
      s.shards.add(new PurpleFragments.Shard(from, now, rgb, size, spin, RANDOM.nextInt(1000)));
   }

   private static double jitter(double k) {
      return (RANDOM.nextDouble() - 0.5) * 2.0 * k;
   }

   private static int darken(int rgb) {
      int r = (rgb >> 16 & 0xFF) * 4 / 5;
      int g = (rgb >> 8 & 0xFF) * 4 / 5;
      int b = (rgb & 0xFF) * 4 / 5;
      return r << 16 | g << 8 | b;
   }

   private static final class Sphere {
      final PurpleSphereEntity entity;
      final LongSet looked = new LongOpenHashSet();
      final List<PurpleFragments.Shard> shards = new ArrayList<>();
      boolean seen;

      Sphere(PurpleSphereEntity entity) {
         this.entity = entity;
      }
   }
}
