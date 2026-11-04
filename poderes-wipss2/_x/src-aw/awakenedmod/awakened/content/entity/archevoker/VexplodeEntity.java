package com.awakenedmod.awakened.content.entity.archevoker;

import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.OwnedProjectile;
import java.util.Iterator;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class VexplodeEntity extends Mob implements Enemy, OwnedProjectile {
   private static final float AOE_RADIUS = 2.6F;
   private static final float AOE_DAMAGE = 6.0F;
   private static final int MAX_FLIGHT = 60;
   @Nullable
   private Vec3 heading;
   private double speed = 0.9;
   @Nullable
   private LivingEntity owner;
   private int lifeTicks = 60;
   private boolean detonated;

   public VexplodeEntity(EntityType<? extends Mob> type, Level level) {
      super(type, level);
      this.setNoGravity(true);
      this.setNoAi(true);
      this.xpReward = 0;
   }

   public static Builder createAttributes() {
      return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 4.0).add(Attributes.MOVEMENT_SPEED, 0.0).add(Attributes.FOLLOW_RANGE, 16.0);
   }

   @Nullable
   public LivingEntity shooter() {
      return this.owner != null && this.owner.isAlive() ? this.owner : null;
   }

   @Override
   public float shotDamage() {
      return 6.0F;
   }

   public void launch(@Nullable LivingEntity owner, Vec3 from, Vec3 toward, double speed) {
      this.owner = owner;
      this.speed = speed;
      Vec3 dir = toward.subtract(from);
      if (dir.lengthSqr() < 1.0E-6) {
         dir = new Vec3(0.0, 0.0, 1.0);
      }

      this.heading = dir.normalize();
      this.setPos(from.x, from.y, from.z);
      float yaw = (float)(Mth.atan2(dir.z, dir.x) * (180.0 / Math.PI)) - 90.0F;
      this.setYRot(yaw);
      this.setYBodyRot(yaw);
      this.setYHeadRot(yaw);
   }

   public void tick() {
      super.tick();
      if (this.level() instanceof ServerLevel server && !this.detonated) {
         if (this.heading != null) {
            Vec3 d = this.heading.scale(this.speed);
            this.setDeltaMovement(d);
            this.move(MoverType.SELF, d);
         }

         if (!this.horizontalCollision && !this.verticalCollision) {
            if (this.nearestVictim(server) != null) {
               this.detonate(server, true);
               return;
            }

            if (--this.lifeTicks <= 0) {
               this.detonate(server, true);
            }

            return;
         }

         this.detonate(server, true);
         return;
      }
   }

   @Nullable
   private Player nearestVictim(ServerLevel server) {
      AABB box = this.getBoundingBox().inflate(1.0);
      Iterator var3 = server.getEntitiesOfClass(Player.class, box, pl -> pl.isAlive() && !pl.isCreative() && !pl.isSpectator()).iterator();
      return var3.hasNext() ? (Player)var3.next() : null;
   }

   private void detonate(ServerLevel server, boolean removeSelf) {
      if (!this.detonated) {
         this.detonated = true;
         Vec3 c = this.position();
         Fx.burst(server, ParticleTypes.EXPLOSION, c, 14, 1.0, 0.1);
         Fx.burst(server, ParticleTypes.EXPLOSION_EMITTER, c, 1, 0.0, 0.0);
         Fx.sound(server, c, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.1F, 1.1F);
         LivingEntity src = this.owner != null && this.owner.isAlive() ? this.owner : null;
         AABB area = new AABB(c, c).inflate(2.6F);

         for (Player p : server.getEntitiesOfClass(
            Player.class, area, pl -> pl.isAlive() && !pl.isCreative() && !pl.isSpectator() && pl.position().distanceToSqr(c) <= 6.7599993F
         )) {
            Dmg.hurt(p, server, this.damageSources().mobProjectile(this, src), 6.0F);
            Vec3 push = p.position().subtract(c);
            if (push.lengthSqr() < 1.0E-4) {
               push = new Vec3(0.0, 1.0, 0.0);
            }

            push = push.normalize();
            p.push(push.x * 0.6, 0.35, push.z * 0.6);
            p.hurtMarked = true;
         }

         if (removeSelf) {
            this.discard();
         }
      }
   }

   public void die(DamageSource source) {
      if (this.level() instanceof ServerLevel server) {
         this.detonate(server, false);
      }

      super.die(source);
   }

   public boolean isPushable() {
      return false;
   }

   public boolean isPickable() {
      return !this.detonated && this.isAlive();
   }
}
