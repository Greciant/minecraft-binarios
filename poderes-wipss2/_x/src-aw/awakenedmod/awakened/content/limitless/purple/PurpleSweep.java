package com.awakenedmod.awakened.content.limitless.purple;

import com.awakenedmod.awakened.content.limitless.LimitlessFeats;
import com.awakenedmod.awakened.content.limitless.domain.VoidBody;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Bosses;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.terrain.CarvePlan;
import com.awakenedmod.awakened.core.terrain.TempTerrain;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class PurpleSweep {
   private final PurpleSphereEntity sphere;
   private final float damage;
   private final LongSet requested = new LongOpenHashSet();
   private final ArrayDeque<BlockPos> pending = new ArrayDeque<>();
   private final Set<Integer> hit = new HashSet<>();
   private int threats;
   private int ticks;

   PurpleSweep(PurpleSphereEntity sphere, float damage) {
      this.sphere = sphere;
      this.damage = damage;
   }

   boolean tick(ServerPlayer player, ServerLevel level) {
      if (this.sphere.isAlive() && this.sphere.level() == level) {
         double age = (double)this.sphere.age(0.0F);
         if (age >= (double)this.sphere.gone()) {
            this.carve(player, level);
            return false;
         } else {
            Vec3 c = this.sphere.centre(0.0F);
            double r = this.sphere.strength().radius(age);
            this.collect(c, r);
            if (++this.ticks % 2 == 0) {
               this.carve(player, level);
               PurpleWake.pressure(level, c, this.sphere.fwd(), r, this.sphere.strength());
            }

            this.bodies(player, level, c, r);
            erase(level, c, r);
            return true;
         }
      } else {
         return false;
      }
   }

   void end() {
      this.pending.clear();
      this.sphere.cutShort();
   }

   private void collect(Vec3 c, double r) {
      for (int x = Mth.floor(c.x - r); x <= Mth.floor(c.x + r); x++) {
         for (int y = Mth.floor(c.y - r); y <= Mth.floor(c.y + r); y++) {
            for (int z = Mth.floor(c.z - r); z <= Mth.floor(c.z + r); z++) {
               double dx = (double)x + 0.5 - c.x;
               double dy = (double)y + 0.5 - c.y;
               double dz = (double)z + 0.5 - c.z;
               if (dx * dx + dy * dy + dz * dz <= r * r && this.requested.add(BlockPos.asLong(x, y, z))) {
                  this.pending.add(new BlockPos(x, y, z));
               }
            }
         }
      }
   }

   private void carve(ServerPlayer player, ServerLevel level) {
      if (!this.pending.isEmpty()) {
         List<BlockPos> cells = new ArrayList<>(Math.min(this.pending.size(), 256));

         while (!this.pending.isEmpty() && cells.size() < 256) {
            cells.add(this.pending.poll());
         }

         TempTerrain.carve(level, player, CarvePlan.builder().wave(0, cells).hold(200).rise(6).permanent(TempTerrain.permanent()).build())
            .onWaveRemoved((wave, blocks) -> {
               PurpleWake.crumble(level, blocks, this.sphere.centre(0.0F));
               this.sphere.erased(blocks.size());
            });
      }
   }

   private void bodies(ServerPlayer player, ServerLevel level, Vec3 c, double radius) {
      double r = radius + 0.2;
      List<LivingEntity> struck = new ArrayList<>();

      for (LivingEntity e : Harm.hostilesOnly(player, AimUtil.livingAround(player, c, r + 2.5))) {
         if (!e.isSpectator()
            && !(AimUtil.reachToBox(e, c) > r)
            && (!(e instanceof Player) || !VoidBody.of(e).held())
            && this.hit.add(e.getId())
            && Dmg.hurtThrough(e, level, Dmg.by(player, DamageTypes.PLAYER_ATTACK), this.damage)) {
            struck.add(e);
         }
      }

      if (!struck.isEmpty()) {
         MasterySystem.award(player, MasteryReason.HIT, struck);
         this.threats = LimitlessFeats.purpleStruck(player, this.threats, struck);

         for (LivingEntity ex : struck) {
            if (ex.isDeadOrDying() && !ex.isRemoved() && !(ex instanceof Player) && !Bosses.is(ex)) {
               PurpleWake.ash(level, ex);
               ex.remove(RemovalReason.KILLED);
            }
         }
      }
   }

   private static void erase(ServerLevel level, Vec3 c, double radius) {
      double r = radius + 0.4;

      for (Entity e : level.getEntities((Entity)null, new AABB(c, c).inflate(r), PurpleSweep::erasable)) {
         if (e.position().distanceToSqr(c) <= r * r) {
            PurpleWake.speck(level, e.position());
            e.discard();
         }
      }
   }

   private static boolean erasable(Entity e) {
      return e.isAlive() && (e instanceof ItemEntity item && item.getAge() >= 20 || e instanceof Projectile && e.getType() != EntityType.TRIDENT);
   }
}
