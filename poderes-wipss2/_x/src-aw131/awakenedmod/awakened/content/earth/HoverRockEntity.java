package com.awakenedmod.awakened.content.earth;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.content.vfx.AbstractFxEntity;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import java.util.Iterator;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class HoverRockEntity extends AbstractFxEntity {
   private static final int PHASE_RISE = 0;
   private static final int PHASE_HOVER = 1;
   private static final int PHASE_FALL = 2;
   private static final int PHASE_LAUNCH = 3;
   private static final int RISE_TICKS = 10;
   private static final int HOVER_HOLD = 50;
   private static final int FALL_MAX = 40;
   private static final int SAFETY_LIFETIME = 600;
   private static final double GRAVITY = -0.045;
   private int blockId;
   private float size = 1.0F;
   private UUID ownerId;
   private EarthMaterial mat = EarthMaterial.STONE;
   private int phase = 0;
   private int phaseTimer;
   private double hoverX;
   private double hoverY;
   private double hoverZ;
   private double startY;
   private float launchDamage;
   private String abilityKey;
   private float spin;
   private float prevSpin;
   private double lx;
   private double ly;
   private double lz;
   private boolean spinInit;

   public HoverRockEntity(EntityType<? extends HoverRockEntity> type, Level level) {
      super(type, level);
   }

   public void configure(EarthMaterial mat, float size, UUID owner, Vec3 hover, double startY) {
      this.mat = mat;
      this.blockId = BuiltInRegistries.BLOCK.getId(mat.state().getBlock());
      this.size = size;
      this.ownerId = owner;
      this.hoverX = hover.x;
      this.hoverY = hover.y;
      this.hoverZ = hover.z;
      this.startY = startY;
      this.lifetime = 600;
   }

   public static HoverRockEntity spawn(ServerLevel level, Vec3 spawnPos, Vec3 hover, EarthMaterial mat, float size, UUID owner) {
      HoverRockEntity e = new HoverRockEntity(AwakenedEntities.HOVER_ROCK.get(), level);
      e.configure(mat, size, owner, hover, spawnPos.y);
      e.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
      level.addFreshEntity(e);
      return e;
   }

   public UUID ownerId() {
      return this.ownerId;
   }

   public EarthMaterial material() {
      return this.mat;
   }

   public boolean launched() {
      return this.phase == 3;
   }

   public int blockId() {
      return this.blockId;
   }

   public float size() {
      return this.size;
   }

   public void launch(Vec3 dir, float damage, String abilityKey) {
      this.launch(dir, damage, 1.9, abilityKey);
   }

   public void launch(Vec3 dir, float damage, double speed, String abilityKey) {
      this.phase = 3;
      this.phaseTimer = 0;
      this.launchDamage = damage;
      this.abilityKey = abilityKey;
      this.noPhysics = false;
      this.lifetime = this.tickCount + 100;
      this.setDeltaMovement(dir.normalize().scale(speed));
      if (this.level() instanceof ServerLevel sl) {
         EarthFx.rockFling(sl, this.position(), this.mat);
      }
   }

   private void advanceSpin() {
      this.prevSpin = this.spin;
      if (this.spinInit) {
         double dx = this.getX() - this.lx;
         double dy = this.getY() - this.ly;
         double dz = this.getZ() - this.lz;
         double speed = Math.sqrt(dx * dx + dy * dy + dz * dz);
         this.spin += (float)(3.0 + speed * 260.0);
      }

      this.lx = this.getX();
      this.ly = this.getY();
      this.lz = this.getZ();
      this.spinInit = true;
   }

   public float renderSpin(float partialTick) {
      return this.prevSpin + (this.spin - this.prevSpin) * partialTick;
   }

   @Override
   public void tick() {
      super.tick();
      this.advanceSpin();
      if (this.level() instanceof ServerLevel level) {
         switch (this.phase) {
            case 0:
               this.tickRise();
               break;
            case 1:
               this.tickHover();
               break;
            case 2:
               this.tickFall(level);
               break;
            case 3:
               this.tickLaunch(level);
         }
      }
   }

   private void tickRise() {
      this.phaseTimer++;
      double t = Math.min(1.0, (double)this.phaseTimer / 10.0);
      double eased = 1.0 - (1.0 - t) * (1.0 - t);
      this.setPos(this.hoverX, this.startY + (this.hoverY - this.startY) * eased, this.hoverZ);
      this.setDeltaMovement(Vec3.ZERO);
      this.hurtMarked = true;
      if (this.phaseTimer >= 10) {
         this.setPos(this.hoverX, this.hoverY, this.hoverZ);
         this.phase = 1;
         this.phaseTimer = 0;
      }
   }

   private void tickHover() {
      this.setPos(this.hoverX, this.hoverY, this.hoverZ);
      this.setDeltaMovement(Vec3.ZERO);
      if (++this.phaseTimer >= 50) {
         this.phase = 2;
         this.phaseTimer = 0;
         this.noPhysics = false;
      }
   }

   private void tickFall(ServerLevel level) {
      this.phaseTimer++;
      this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.045, 0.0).scale(0.98));
      this.move(MoverType.SELF, this.getDeltaMovement());
      if (this.verticalCollision || this.onGround() || this.phaseTimer >= 40) {
         EarthFx.rockCrumble(level, this.position(), this.mat);
         this.discard();
      }
   }

   private void tickLaunch(ServerLevel level) {
      this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.045, 0.0).scale(0.99));
      this.move(MoverType.SELF, this.getDeltaMovement());
      if (!this.horizontalCollision && !this.verticalCollision) {
         Iterator var2 = level.getEntitiesOfClass(
               LivingEntity.class,
               this.getBoundingBox().inflate(0.35),
               ex -> ex.isAlive() && (this.ownerId == null || !ex.getUUID().equals(this.ownerId)) && Harm.allowed(this, ex)
            )
            .iterator();
         if (var2.hasNext()) {
            LivingEntity e = (LivingEntity)var2.next();
            this.detonate(level, e);
         }
      } else {
         this.detonate(level, null);
      }
   }

   private void detonate(ServerLevel level, LivingEntity direct) {
      Vec3 c = this.position();
      Vec3 v = this.getDeltaMovement();
      Vec3 punch = v.lengthSqr() > 1.0E-4 ? v.normalize() : new Vec3(0.0, 0.0, 1.0);
      ServerPlayer owner = this.ownerId == null ? null : level.getServer().getPlayerList().getPlayer(this.ownerId);
      DamageSource src = owner != null ? Dmg.by(owner, DamageTypes.FALLING_BLOCK) : this.damageSources().generic();
      Entity blame = (Entity)(owner != null ? owner : this);
      if (direct != null) {
         boolean dealt = Dmg.hurt(direct, level, src, this.launchDamage);
         if (dealt && owner != null) {
            MasterySystem.award(owner, MasteryReason.HIT, direct);
         }

         double kb = (double)this.mat.knockback() * 1.25;
         direct.push(punch.x * kb, 0.28, punch.z * kb);
         direct.hurtMarked = true;
         stagger(blame, direct, this.mat.staggerTicks());
         if (this.mat.blinds()) {
            Harm.effect(blame, direct, new MobEffectInstance(MobEffects.BLINDNESS, this.mat.blindTicks(), 0));
         }
      }

      float aoeDamage = owner != null
         ? MasteryScaling.scaledDamage(this.mat.aoeDamage(), owner, this.abilityKey)
         : MasteryScaling.configScaledDamage(this.mat.aoeDamage(), this.abilityKey);

      for (LivingEntity e : level.getEntitiesOfClass(
         LivingEntity.class,
         new AABB(c, c).inflate((double)this.mat.aoeRadius()),
         ex -> ex.isAlive() && ex != direct && (this.ownerId == null || !ex.getUUID().equals(this.ownerId)) && Harm.allowed(this, ex)
      )) {
         Dmg.hurt(e, level, src, aoeDamage);
         Vec3 push = e.position().subtract(c);
         push = push.lengthSqr() > 1.0E-4 ? push.normalize() : punch;
         e.push(push.x * (double)this.mat.knockback(), 0.2, push.z * (double)this.mat.knockback());
         e.hurtMarked = true;
         if (this.mat.blinds()) {
            Harm.effect(blame, e, new MobEffectInstance(MobEffects.BLINDNESS, this.mat.blindTicks(), 0));
         }
      }

      EarthFx.rockImpact(level, c, this.mat);
      EarthFx.materialImpact(level, c, this.mat);
      Fx.shake(level, c, 18.0, 0.3F, 2, 10);
      this.discard();
   }

   private static void stagger(Entity source, LivingEntity e, int ticks) {
      if (ticks > 0) {
         Harm.effect(source, e, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 3, false, true));
      }
   }

   @Override
   protected void writeExtraSpawnData(RegistryFriendlyByteBuf buf) {
      buf.writeVarInt(this.blockId);
      buf.writeFloat(this.size);
      buf.writeBoolean(this.ownerId != null);
      if (this.ownerId != null) {
         buf.writeUUID(this.ownerId);
      }
   }

   @Override
   protected void readExtraSpawnData(RegistryFriendlyByteBuf buf) {
      this.blockId = buf.readVarInt();
      this.size = buf.readFloat();
      this.ownerId = buf.readBoolean() ? buf.readUUID() : null;
   }
}
