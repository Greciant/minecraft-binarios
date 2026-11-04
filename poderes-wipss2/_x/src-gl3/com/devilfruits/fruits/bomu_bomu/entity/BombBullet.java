package com.devilfruits.fruits.bomu_bomu.entity;

import com.devilfruits.core.fx.ExplosionProfile;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.core.world.AbilityEntity;
import com.devilfruits.core.world.DebrisEntity;
import com.devilfruits.fruits.bomu_bomu.BomuAbilities;
import com.devilfruits.fruits.bomu_bomu.BomuBomu;
import com.devilfruits.fruits.bomu_bomu.BomuEntities;
import com.devilfruits.fruits.bomu_bomu.BomuFx;
import com.devilfruits.fruits.bomu_bomu.explosion.BombExplosionService;
import com.devilfruits.fruits.bomu_bomu.explosion.ExplosionSpec;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class BombBullet extends ThrowableProjectile implements AbilityEntity {
   public static final double GRAVITY = 0.004;
   private static final EntityDataAccessor<Boolean> GUST = SynchedEntityData.defineId(BombBullet.class, EntityDataSerializers.BOOLEAN);
   @Nullable
   private ExplosionSpec spec;
   private int maxLife = 40;
   private int life;
   private final Set<UUID> shoved = new HashSet<>();

   public BombBullet(EntityType<? extends BombBullet> type, Level level) {
      super(type, level);
   }

   public static BombBullet shoot(ServerPlayer owner, Vec3 from, Vec3 velocity, ExplosionSpec spec, int maxLife) {
      BombBullet b = create(owner, from, velocity, maxLife);
      b.spec = spec;
      owner.level().addFreshEntity(b);
      return b;
   }

   public static BombBullet gust(ServerPlayer owner, Vec3 from, Vec3 velocity, int maxLife) {
      BombBullet b = create(owner, from, velocity, maxLife);
      b.entityData.set(GUST, true);
      owner.level().addFreshEntity(b);
      return b;
   }

   private static BombBullet create(ServerPlayer owner, Vec3 from, Vec3 velocity, int maxLife) {
      BombBullet b = new BombBullet(BomuEntities.BULLET.get(), owner.level());
      b.setOwner(owner);
      b.setPos(from);
      b.setDeltaMovement(velocity);
      b.maxLife = Math.max(2, maxLife);
      return b;
   }

   public boolean isGust() {
      return (Boolean)this.entityData.get(GUST);
   }

   protected void defineSynchedData(Builder builder) {
      builder.define(GUST, false);
   }

   protected double getDefaultGravity() {
      return 0.004;
   }

   public void tick() {
      Vec3 from = this.position();
      super.tick();
      if (!this.level().isClientSide() && !this.isRemoved()) {
         this.draught(from, this.position());
         if (++this.life > this.maxLife) {
            Vec3 v = this.getDeltaMovement();
            this.land(this.position(), v.lengthSqr() > 1.0E-8 ? v.normalize().scale(-1.0) : new Vec3(0.0, 1.0, 0.0));
         }
      }
   }

   private void draught(Vec3 a, Vec3 b) {
      Vec3 d = b.subtract(a);
      double len = d.length();
      if (!(len < 1.0E-4)) {
         Vec3 dir = d.scale(1.0 / len);
         ServerPlayer owner = this.getOwner() instanceof ServerPlayer p ? p : null;
         AABB box = new AABB(a, b).inflate(1.2);

         for (Entity e : this.level().getEntities(this, box, x -> x != owner && !(x instanceof AbilityEntity) && !(x instanceof DebrisEntity) && x.isAlive())) {
            if (this.shoved.add(e.getUUID()) && (owner == null || Targeting.canPush(owner, e))) {
               Vec3 c = e.getBoundingBox().getCenter();
               double t = Math.max(0.0, Math.min(len, c.subtract(a).dot(dir)));
               Vec3 off = c.subtract(a.add(dir.scale(t)));
               double dist = off.length();
               if (!(dist > 1.4)) {
                  double k = (1.0 - dist / 1.4) * (this.isGust() ? 0.35 : 0.25);
                  Vec3 push = dir.scale(k).add(dist > 0.001 ? off.scale(0.12 / dist) : Vec3.ZERO).add(0.0, 0.05, 0.0);
                  Targeting.push(e, push);
               }
            }
         }
      }
   }

   protected boolean canHitEntity(Entity target) {
      return !(target instanceof AbilityEntity) && super.canHitEntity(target);
   }

   protected void onHit(HitResult hit) {
      super.onHit(hit);
      if (!this.level().isClientSide() && !this.isRemoved()) {
         Vec3 dir = this.getDeltaMovement().lengthSqr() > 1.0E-8 ? this.getDeltaMovement().normalize() : new Vec3(0.0, -1.0, 0.0);
         if (hit instanceof BlockHitResult b) {
            Vec3 n = PhysicsWorld.worldNormal(this.level(), b);
            this.land(PhysicsWorld.worldLocation(this.level(), b).add(n.scale(0.2)), n);
         } else if (hit instanceof EntityHitResult e) {
            this.land(e.getLocation().subtract(dir.scale(0.2)), dir.scale(-1.0));
         }
      }
   }

   private void land(Vec3 at, Vec3 normal) {
      if (this.level() instanceof ServerLevel server) {
         this.discard();
         ServerPlayer owner = this.getOwner() instanceof ServerPlayer p ? p : null;
         if (this.isGust()) {
            this.burst(server, owner, at, normal);
         } else {
            ExplosionSpec s = this.spec != null
               ? this.spec
               : new ExplosionSpec(4.0F, 4.0F, 1.0F, 14.0F, 1.2F, 0.0F, ExplosionProfile.MEDIUM, BomuAbilities.SHOT.id());
            BombExplosionService.detonate(server, at, normal, s, owner);
         }
      }
   }

   private void burst(ServerLevel level, @Nullable ServerPlayer owner, Vec3 at, Vec3 normal) {
      double reach = 2.2;
      double strength = BomuBomu.setting("gustKnockback");

      for (Entity e : level.getEntities(this, new AABB(at, at).inflate(reach), x -> x != owner && !(x instanceof AbilityEntity) && x.isAlive())) {
         if (owner == null || Targeting.canPush(owner, e)) {
            Vec3 off = e.getBoundingBox().getCenter().subtract(at);
            double dist = off.length();
            if (!(dist > reach)) {
               Vec3 dir = dist > 0.001 ? off.scale(1.0 / dist) : normal;
               Targeting.push(e, dir.scale(strength * (1.0 - dist / reach)).add(0.0, 0.12, 0.0));
            }
         }
      }

      ServerFx.sendNear(level, BomuFx.GUST, -1, at, this.getDeltaMovement(), 1.0F, this.random.nextLong(), 0, 48.0);
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      if (this.spec != null) {
         tag.put("Spec", this.spec.save());
      }

      tag.putBoolean("Gust", this.isGust());
      tag.putInt("MaxLife", this.maxLife);
      tag.putInt("Life", this.life);
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.spec = tag.contains("Spec") ? ExplosionSpec.load(tag.getCompound("Spec")) : null;
      this.entityData.set(GUST, tag.getBoolean("Gust"));
      this.maxLife = Math.max(2, tag.getInt("MaxLife"));
      this.life = tag.getInt("Life");
   }
}
