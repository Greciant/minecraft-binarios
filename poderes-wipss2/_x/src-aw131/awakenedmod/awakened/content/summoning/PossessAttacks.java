package com.awakenedmod.awakened.content.summoning;

import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.mixin.GuardianBeamInvoker;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class PossessAttacks {
   private static final double REACH = 4.0;
   private static final double RANGED_REACH = 32.0;
   private static final int CD_MELEE = 10;
   private static final int CD_ARROW = 14;
   private static final int CD_GHAST = 30;
   private static final int CD_BLAZE = 12;
   private static final int CD_WITHER = 20;
   private static final int CD_SHULKER = 20;
   private static final int CD_GUARDIAN = 20;
   private static final int CD_LLAMA = 14;
   private static final int CD_WITCH = 25;
   private static final int BEAM_HOLD = 16;
   private static final Map<Integer, Integer> BEAM = new ConcurrentHashMap<>();

   private PossessAttacks() {
   }

   public static int attack(UUID owner, Mob mob) {
      if (mob.level() instanceof ServerLevel level) {
         ServerPlayer var6 = level.getServer().getPlayerList().getPlayer(owner);
         if (mob instanceof Creeper creeper) {
            if (!creeper.isIgnited()) {
               creeper.ignite();
               aSound(level, var6, mob, SoundEvents.CREEPER_PRIMED, 1.0F, 0.5F);
            }

            return 10;
         } else {
            mob.swing(InteractionHand.MAIN_HAND);
            int ranged = fireRanged(level, owner, mob);
            if (ranged >= 0) {
               return ranged;
            } else {
               LivingEntity target = lookTarget(owner, mob, 4.0);
               if (target != null) {
                  if (mob.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
                     mob.doHurtTarget(target);
                  } else {
                     Dmg.hurt(target, level, mob.damageSources().mobAttack(mob), 1.0F);
                  }
               }

               return 10;
            }
         }
      } else {
         return 0;
      }
   }

   private static int fireRanged(ServerLevel level, UUID owner, Mob mob) {
      Vec3 look = aimDir(mob);
      Vec3 muzzle = mob.getEyePosition().add(look.scale((double)mob.getBbWidth() * 0.5 + 0.5));
      LivingEntity aimed = lookTarget(owner, mob, 32.0);
      ServerPlayer pilot = level.getServer().getPlayerList().getPlayer(owner);
      if (mob instanceof Guardian guardian) {
         if (aimed == null) {
            return 20;
         } else {
            float f = 1.0F;
            if (level.getDifficulty() == Difficulty.HARD) {
               f += 2.0F;
            }

            boolean elder = guardian instanceof ElderGuardian;
            if (elder) {
               f += 2.0F;
            }

            Dmg.hurt(aimed, level, mob.damageSources().indirectMagic(mob, mob), f);
            ((GuardianBeamInvoker)guardian).awakened$setActiveAttackTarget(aimed.getId());
            BEAM.put(guardian.getId(), 16);
            aSound(level, pilot, mob, SoundEvents.GUARDIAN_ATTACK, 1.0F, 1.0F);
            return 20;
         }
      } else if (mob instanceof Ghast ghast) {
         LargeFireball fb = new LargeFireball(level, ghast, look.normalize(), ghast.getExplosionPower());
         fb.setPos(muzzle.x, muzzle.y, muzzle.z);
         level.addFreshEntity(fb);
         aSound(level, pilot, mob, SoundEvents.GHAST_SHOOT, 1.0F, 1.0F);
         return 30;
      } else if (mob instanceof Blaze) {
         SmallFireball fb = new SmallFireball(level, mob, look.normalize());
         fb.setPos(muzzle.x, muzzle.y, muzzle.z);
         level.addFreshEntity(fb);
         aSound(level, pilot, mob, SoundEvents.BLAZE_SHOOT, 1.0F, 1.0F);
         return 12;
      } else if (mob instanceof WitherBoss) {
         WitherSkull skull = new WitherSkull(level, mob, look.normalize());
         skull.setPos(muzzle.x, muzzle.y, muzzle.z);
         level.addFreshEntity(skull);
         aSound(level, pilot, mob, SoundEvents.WITHER_SHOOT, 1.0F, 1.0F);
         return 20;
      } else if (mob instanceof Shulker shulker) {
         LivingEntity homing = aimed != null ? aimed : nearestHostile(owner, mob);
         if (homing == null) {
            return -1;
         } else {
            level.addFreshEntity(new ShulkerBullet(level, shulker, homing, shulker.getAttachFace().getAxis()));
            aSound(level, pilot, mob, SoundEvents.SHULKER_SHOOT, 2.0F, 1.0F);
            return 20;
         }
      } else if (mob instanceof Llama llama) {
         LlamaSpit spit = new LlamaSpit(level, llama);
         spit.setPos(muzzle.x, muzzle.y, muzzle.z);
         spit.shoot(look.x, look.y, look.z, 1.5F, 1.0F);
         level.addFreshEntity(spit);
         aSound(level, pilot, mob, SoundEvents.LLAMA_SPIT, 1.0F, 1.0F);
         return 14;
      } else if (mob instanceof SnowGolem) {
         new ItemStack(Items.SNOWBALL);
         Snowball ball = new Snowball(level, mob);
         ball.setPos(muzzle.x, muzzle.y, muzzle.z);
         ball.shoot(look.x, look.y, look.z, 1.6F, 1.0F);
         level.addFreshEntity(ball);
         aSound(level, pilot, mob, SoundEvents.SNOW_GOLEM_SHOOT, 1.0F, 1.0F);
         return 14;
      } else if (mob instanceof Witch witch) {
         if (aimed == null) {
            return -1;
         } else {
            witch.performRangedAttack(aimed, 1.0F);
            return 25;
         }
      } else if (mob instanceof RangedAttackMob && !(mob instanceof WitherSkeleton) && !(mob instanceof Drowned) && !(mob instanceof PossessMelee)) {
         ItemStack ammo = new ItemStack(Items.ARROW);
         Arrow arrow = new Arrow(level, mob, ammo, null);
         arrow.setPos(muzzle.x, muzzle.y, muzzle.z);
         arrow.shoot(look.x, look.y, look.z, 2.8F, 1.0F);
         level.addFreshEntity(arrow);
         aSound(level, pilot, mob, SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F);
         return 14;
      } else {
         return -1;
      }
   }

   private static void aSound(ServerLevel level, ServerPlayer pilot, Mob mob, SoundEvent sound, float volume, float pitch) {
      if (pilot != null) {
         Fx.soundForViewer(level, pilot, mob.position(), sound, mob.getSoundSource(), volume, pitch);
      } else {
         Fx.sound(level, mob.position(), sound, mob.getSoundSource(), volume, pitch);
      }
   }

   public static void tickBeam(Mob mob) {
      Integer left = BEAM.get(mob.getId());
      if (left != null) {
         if (left > 1 && mob instanceof Guardian) {
            BEAM.put(mob.getId(), left - 1);
         } else {
            clearBeam(mob);
         }
      }
   }

   public static void clearBeam(Mob mob) {
      if (BEAM.remove(mob.getId()) != null && mob instanceof Guardian guardian) {
         ((GuardianBeamInvoker)guardian).awakened$setActiveAttackTarget(0);
      }
   }

   private static LivingEntity lookTarget(UUID owner, Mob mob, double range) {
      Vec3 eye = mob.getEyePosition();
      Vec3 look = aimDir(mob);
      Vec3 end = eye.add(look.scale(range));
      AABB search = mob.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
      LivingEntity best = null;
      double bestSqr = range * range;

      for (LivingEntity candidate : mob.level().getEntitiesOfClass(LivingEntity.class, search, e -> e != mob && e.isAlive() && !isFriendly(owner, e))) {
         Optional<Vec3> hit = candidate.getBoundingBox().inflate(0.3).clip(eye, end);
         if (hit.isPresent()) {
            double distSqr = eye.distanceToSqr(hit.get());
            if (distSqr < bestSqr) {
               bestSqr = distSqr;
               best = candidate;
            }
         }
      }

      return best != null && mob.level().clip(new ClipContext(eye, best.getBoundingBox().getCenter(), Block.COLLIDER, Fluid.NONE, mob)).getType() == Type.BLOCK
         ? null
         : best;
   }

   private static Vec3 aimDir(Mob mob) {
      float pitch = mob instanceof Phantom ? -mob.getXRot() : mob.getXRot();
      return Vec3.directionFromRotation(pitch, mob.getYRot());
   }

   private static LivingEntity nearestHostile(UUID owner, Mob mob) {
      AABB box = mob.getBoundingBox().inflate(32.0);
      LivingEntity best = null;
      double bestSqr = 1024.0;

      for (LivingEntity e : mob.level()
         .getEntitiesOfClass(LivingEntity.class, box, c -> c != mob && c.isAlive() && c instanceof Enemy && !isFriendly(owner, c))) {
         double d = e.distanceToSqr(mob);
         if (d < bestSqr) {
            bestSqr = d;
            best = e;
         }
      }

      return best;
   }

   private static boolean isFriendly(UUID owner, LivingEntity e) {
      return MinionSupport.alliedSummonForbidden(owner, e);
   }
}
