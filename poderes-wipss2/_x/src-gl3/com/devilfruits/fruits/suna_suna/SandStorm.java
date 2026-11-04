package com.devilfruits.fruits.suna_suna;

import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsConfig;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.core.world.CastBudget;
import com.devilfruits.core.world.TerrainDestruction;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SandStorm {
   public static final double HEIGHT = 20.0;
   public static final int RISE = 20;
   private final ServerLevel level;
   private final ServerPlayer caster;
   private final SandStorm.Power power;
   private final long seed;
   private final double initial;
   private final CastBudget drying = Desiccation.budget();
   private final CastBudget debris;
   private final RandomSource random;
   private double x;
   private double y;
   private double z;
   private int held;

   public SandStorm(ServerLevel level, ServerPlayer caster, Vec3 at, double headingRadians, SandStorm.Power power, CastBudget debris, long seed) {
      this.level = level;
      this.caster = caster;
      this.power = power;
      this.seed = seed;
      this.initial = headingRadians;
      this.debris = debris;
      this.random = RandomSource.create(seed);
      this.x = at.x;
      this.y = at.y;
      this.z = at.z;
   }

   public static double heading(double initial, long seed, int t) {
      float p1 = (float)(seed & 65535L) / 65536.0F * (float) (Math.PI * 2);
      float p2 = (float)(seed >>> 16 & 65535L) / 65536.0F * (float) (Math.PI * 2);
      return initial + 0.7 * (double)(Mth.sin((float)t * 0.035F + p1) - Mth.sin(p1)) + 0.35 * (double)(Mth.sin((float)t * 0.083F + p2) - Mth.sin(p2));
   }

   public static float envelope(float t, int life) {
      return Mth.clamp(Math.min(t / 20.0F, ((float)life - t) / 20.0F), 0.0F, 1.0F);
   }

   public Vec3 position() {
      return new Vec3(this.x, this.y, this.z);
   }

   public int held() {
      return this.held;
   }

   public boolean tick(int age) {
      if ((!this.caster.isRemoved() || age <= 1) && age < this.power.life) {
         double h = heading(this.initial, this.seed, age);
         this.x = this.x + Math.cos(h) * this.power.speed;
         this.z = this.z + Math.sin(h) * this.power.speed;
         double ground = SunaGround.surface(this.level, this.x, this.y, this.z, 3, 6);
         if (!Double.isNaN(ground)) {
            this.y = ground;
         }

         float grown = Math.max(0.25F, envelope((float)age, this.power.life));
         double r = this.power.radius * (double)grown;
         double reach = Math.max(this.power.radius, this.power.pullRadius) * (double)grown;
         Vec3 centre = new Vec3(this.x, this.y, this.z);
         boolean pulse = age > 0 && age % Math.max(1, this.power.hitInterval) == 0;
         List<Entity> caught = this.caught(reach);
         this.held = 0;

         for (Entity e : caught) {
            double rx = e.getX() - this.x;
            double rz = e.getZ() - this.z;
            double d = Math.sqrt(rx * rx + rz * rz);
            Vec3 in = d < 0.4 ? Vec3.ZERO : new Vec3(-rx / d, 0.0, -rz / d);
            Vec3 swirl = new Vec3(-in.z, 0.0, in.x);
            if (d > r) {
               double drag = 0.6 + 0.4 * (reach - d) / Math.max(0.5, reach - r);
               Targeting.launch(e, e.getDeltaMovement().scale(0.7).add(in.scale(this.power.pull * drag)).add(swirl.scale(0.2 * drag)));
            } else {
               this.held++;
               double strength = 1.0 - 0.5 * d / Math.max(1.0, r);
               double up = e.getY() < this.y + 9.0 ? this.power.lift * strength : -0.03;
               Vec3 v = e.getDeltaMovement().scale(0.55).add(in.scale(this.power.pull * strength)).add(swirl.scale(0.55 * strength)).add(0.0, up, 0.0);
               Targeting.launch(e, v);
               e.fallDistance = Math.min(e.fallDistance, 3.0F);
               if (pulse && e instanceof LivingEntity) {
                  LivingEntity living = (LivingEntity)e;
                  if (Targeting.canHurt(this.caster, e)) {
                     living.invulnerableTime = 0;
                     if (Targeting.hurt(this.caster, e, this.power.damage, SunaSuna.SAND_DAMAGE) && this.power.dehydrateSeconds > 0) {
                        SunaEffects.dehydrate(living, this.power.dehydrateSeconds, 1, 3, this.caster);
                     }
                  }
               }
            }
         }

         if (age % 5 == 0) {
            Desiccation.dry(this.level, this.caster, centre, r * 0.8, 0.0, Desiccation.Options.SURFACE.withLeaves(), this.drying);
         }

         if (this.power.blockDamage && TerrainDestruction.destructionAllowed(this.level)) {
            this.rip(age, r);
         }

         this.whirlStructures(centre, reach, PhysicsProfile.Impact.TANGENTIAL, 0.05);
         return false;
      } else {
         this.blowApart();
         return true;
      }
   }

   private void whirlStructures(Vec3 centre, double r, PhysicsProfile.Impact impact, double share) {
      if (PhysicsWorld.active() && (Boolean)PhysicsConfig.DRAG_EFFECTS.get()) {
         double strength = 40.0 * ((double)this.power.damage + 6.0 * this.power.knockback) * 0.5 * share;
         double chance = impact == PhysicsProfile.Impact.TANGENTIAL && this.power.blockDamage && TerrainDestruction.destructionAllowed(this.level)
            ? 0.004
            : 0.0;
         BlockImpactService.apply(
            new BlockImpactService.Strike(this.caster, SunaSuna.ID, impact, strength, chance, false, this.debris, null),
            ImpactShape.cylinder(centre.add(0.0, -2.0, 0.0), Math.max(1.0, r), 22.0),
            new Vec3(0.0, 1.0, 0.0)
         );
      }
   }

   private List<Entity> caught(double r) {
      AABB box = new AABB(this.x - r, this.y - 2.0, this.z - r, this.x + r, this.y + 20.0, this.z + r);
      List<Entity> out = Targeting.inBox(
         this.level, this.caster, box, e -> Targeting.canPush(this.caster, e) && (!(e instanceof Player p) || !SunaSuna.isUser(p))
      );
      out.removeIf(e -> Mth.square(e.getX() - this.x) + Mth.square(e.getZ() - this.z) > r * r);
      return out;
   }

   private void rip(int age, double r) {
      double perTick = this.power.debrisPerSecond / 20.0;
      int count = (int)Math.floor(perTick * (double)(age + 1)) - (int)Math.floor(perTick * (double)age);

      for (int i = 0; i < count; i++) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         double d = r * 0.8 * Math.sqrt(this.random.nextDouble());
         int bx = Mth.floor(this.x + Math.cos(a) * d);
         int bz = Mth.floor(this.z + Math.sin(a) * d);
         BlockPos surface = TerrainDestruction.findSurface(this.level, bx, Mth.floor(this.y), bz, 2, 3);
         if (surface != null) {
            Vec3 swirl = new Vec3(-Math.sin(a), 0.0, Math.cos(a));
            TerrainDestruction.tearOut(
               this.level, this.caster, surface, swirl.scale(0.45).add(0.0, 0.85 + this.random.nextDouble() * 0.4, 0.0), this.debris, i * 3
            );
         }
      }
   }

   private void blowApart() {
      Vec3 centre = new Vec3(this.x, this.y, this.z);

      for (Entity e : this.caught(this.power.radius)) {
         Vec3 out = e.position().subtract(centre).multiply(1.0, 0.0, 1.0);
         out = out.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : out.normalize();
         Targeting.launch(e, out.scale(this.power.knockback).add(0.0, 0.5, 0.0));
      }

      this.whirlStructures(centre, this.power.radius, PhysicsProfile.Impact.RADIAL, 0.8);
      ServerFx.sendNear(this.level, SunaFx.SABLES_END, this.caster.getId(), centre, Vec3.ZERO, (float)this.power.radius, this.seed, 0, 160.0);
      ServerFx.shake(this.level, centre, 0.3F, 40);
   }

   public static record Power(
      double radius,
      int life,
      double speed,
      double pull,
      double lift,
      float damage,
      int hitInterval,
      double debrisPerSecond,
      int dehydrateSeconds,
      double knockback,
      boolean blockDamage,
      double pullRadius
   ) {
      public Power(
         double radius,
         int life,
         double speed,
         double pull,
         double lift,
         float damage,
         int hitInterval,
         double debrisPerSecond,
         int dehydrateSeconds,
         double knockback,
         boolean blockDamage
      ) {
         this(radius, life, speed, pull, lift, damage, hitInterval, debrisPerSecond, dehydrateSeconds, knockback, blockDamage, radius);
      }
   }
}
