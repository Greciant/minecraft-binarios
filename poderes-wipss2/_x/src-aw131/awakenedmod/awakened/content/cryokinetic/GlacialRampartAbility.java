package com.awakenedmod.awakened.content.cryokinetic;

import com.awakenedmod.awakened.content.earth.StoneConstructEntity;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class GlacialRampartAbility extends Ability {
   private static final int UNLOCK = 10;
   private static final float COST = 100.0F;
   private static final float DAMAGE = 10.0F;
   private static final int WALL_TICKS = 240;
   private static final int HOLD_TICKS = 80;
   private static final int SHATTER_CHUNKS = 36;
   private final Map<UUID, GlacialRampartAbility.Rampart> ramparts = this.playerState();
   private final IceHold hold = new IceHold(this.activeState());

   public GlacialRampartAbility() {
      super("glacial_rampart", 100.0F, 10);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(10.0).build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (this.ramparts.containsKey(player.getUUID())) {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      } else {
         ServerLevel level = player.serverLevel();
         Optional<Vec3> ground = AimUtil.groundedPositionIfFound(level, player.position());
         if (ground.isEmpty()) {
            return this.fail(player, "awakened.cryokinetic.no_ground", new Object[0]);
         } else {
            Vec3 look = player.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0.0, look.z);
            if (flat.lengthSqr() < 1.0E-4) {
               float yaw = player.getYRot() * (float) (Math.PI / 180.0);
               flat = new Vec3((double)(-Mth.sin(yaw)), 0.0, (double)Mth.cos(yaw));
            }

            GlacierShape shape = GlacierShape.build(level, BlockPos.containing((Position)ground.get()), flat.normalize(), level.random);
            if (shape.cells.isEmpty()) {
               return this.fail(player, "awakened.cryokinetic.no_room", new Object[0]);
            } else {
               GlacierEntity.spawn(level, shape, 240);
               List<GlacierShape.Cell> cells = new ArrayList<>(shape.cells);
               cells.sort(Comparator.comparingInt(GlacierShape.Cell::delay));
               this.ramparts.put(player.getUUID(), new GlacialRampartAbility.Rampart(cells, shape));
               CryoFx.rampartOnset(level, ground.get());
               Fx.shake(level, ground.get(), 30.0, 0.08F, 4, 8);
               PowerService.playAnimation(player, "glacial_rampart");
               return true;
            }
         }
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      this.hold.tick(player);
      GlacialRampartAbility.Rampart rampart = this.ramparts.get(player.getUUID());
      if (rampart != null) {
         ServerLevel level = player.serverLevel();
         int age = rampart.age++;
         GlacierShape.Cell front = null;

         while (rampart.next < rampart.cells.size() && rampart.cells.get(rampart.next).delay() <= age) {
            GlacierShape.Cell c = rampart.cells.get(rampart.next);
            this.raise(player, level, rampart, c, age, rampart.next % 3 == 0);
            rampart.next++;
            front = c;
         }

         if (front != null) {
            CryoFx.rampartFront(level, front.feet());
         }

         boolean standing = age > 12 && age < 228;
         if (standing && age % 3 == 0 && rampart.next > 0) {
            for (int i = 0; i < 4; i++) {
               GlacierShape.Cell c = rampart.cells.get(level.random.nextInt(rampart.next));
               CryoFx.rampartHaze(level, c.feet().add(0.0, (double)c.height(), 0.0), 1.0);
            }
         }

         if (age == 228) {
            shatter(level, rampart.shape);
         }

         if (age >= 240) {
            this.ramparts.remove(player.getUUID());
         }
      }
   }

   private void raise(ServerPlayer player, ServerLevel level, GlacialRampartAbility.Rampart rampart, GlacierShape.Cell c, int age, boolean spray) {
      Vec3 f = c.feet();
      AABB box = new AABB(f.x - 0.5, f.y, f.z - 0.5, f.x + 0.5, f.y + (double)c.height(), f.z + 0.5);
      List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, box.inflate(0.3), LivingEntity::isAlive);
      int remaining = 240 - age;

      for (LivingEntity target : near) {
         if (target != player && Harm.allowed(player, target) && rampart.caught.add(target.getUUID())) {
            if (Harm.controls(player, target)) {
               this.hold.encase(player, level, target, Math.min(remaining, 80));
            }

            Dmg.hurt(target, level, Dmg.by(player, DamageTypes.FREEZE), MasteryScaling.scaledDamage(10.0F, player, this.balanceKey()));
            if (!rampart.awarded && Foes.counts(player, target)) {
               rampart.awarded = true;
               MasterySystem.award(player, MasteryReason.HIT, target);
            }
         }
      }

      if (near.stream().noneMatch(e -> e.getBoundingBox().intersects(box))) {
         StoneConstructEntity.collider(level, f, 1.0F, c.height(), remaining - 12);
      }

      if (spray) {
         CryoFx.rampartColumn(level, f, 1.0, (double)c.height());
      }
   }

   private static void shatter(ServerLevel level, GlacierShape shape) {
      List<GlacierShape.Voxel> v = shape.voxels;

      for (int i = 0; i < 36 && !v.isEmpty(); i++) {
         GlacierShape.Voxel p = v.get(level.random.nextInt(v.size()));
         CryoFx.rampartShatter(level, Vec3.atCenterOf(shape.origin.offset(p.x(), p.y(), p.z())), i % 6 == 0);
      }
   }

   @Override
   public boolean occupiesHands(ServerPlayer player) {
      return false;
   }

   @Override
   public boolean ownsBody(ServerPlayer player) {
      return false;
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      this.hold.releaseAll(player);
      super.clearPlayer(player);
   }

   private static final class Rampart {
      private final List<GlacierShape.Cell> cells;
      private final GlacierShape shape;
      private final Set<UUID> caught = new HashSet<>();
      private int next;
      private int age;
      private boolean awarded;

      private Rampart(List<GlacierShape.Cell> cells, GlacierShape shape) {
         this.cells = cells;
         this.shape = shape;
      }
   }
}
