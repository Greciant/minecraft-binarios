package com.awakenedmod.awakened.content.gamma;

import com.awakenedmod.awakened.content.vfx.BlockFxEntity;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class BoulderControl {
   private static final int RIP_TICK = 4;
   private static final int CARRY_TICK = 10;
   private static final int WHIP_TICKS = 4;
   private static final int MAX_CARRY_TICKS = 400;
   private static final double FLIGHT_SPEED = 2.4;
   static final double AIM_RANGE = 48.0;
   private static final ResourceLocation SLOW_ID = AwId.of("gamma_boulder_slow");
   private static final double RIP_SLOW = -0.7;
   private static final double CARRY_SLOW = -0.2;
   private static final float BASE_DAMAGE = 16.0F;
   private static final double BASE_KNOCKBACK = 1.2;
   private static final double KNOCKUP = 0.45;
   private static final double SPLASH_RADIUS = 3.0;
   private static final double SPLASH_PER_RAGE = 1.0;
   private static final Map<UUID, BoulderControl.Boulder> ACTIVE = new ConcurrentHashMap<>();

   private BoulderControl() {
   }

   public static boolean isThrowing(ServerPlayer player) {
      return ACTIVE.containsKey(player.getUUID());
   }

   public static void begin(ServerPlayer thrower, Vec3 ripPoint, float rage) {
      ACTIVE.put(thrower.getUUID(), new BoulderControl.Boulder(Dmg.serverLevel(thrower), thrower, ripPoint, rage));
      slow(thrower, -0.7);
   }

   public static void release(ServerPlayer thrower) {
      BoulderControl.Boulder boulder = ACTIVE.get(thrower.getUUID());
      if (boulder != null) {
         boulder.hurlRequested = true;
      }
   }

   public static void tick(ServerPlayer thrower) {
      BoulderControl.Boulder boulder = ACTIVE.get(thrower.getUUID());
      if (boulder != null) {
         if (!boulder.tick()) {
            ACTIVE.remove(thrower.getUUID());
            slow(thrower, 0.0);
         }
      }
   }

   public static void clearPlayer(ServerPlayer thrower) {
      BoulderControl.Boulder boulder = ACTIVE.remove(thrower.getUUID());
      if (boulder != null) {
         boulder.discard();
         if (!boulder.launched) {
            PowerService.stopAnimation(thrower);
         }
      }

      slow(thrower, 0.0);
   }

   private static void slow(ServerPlayer player, double amount) {
      AttributeInstance inst = player.getAttribute(Attributes.MOVEMENT_SPEED);
      if (inst != null) {
         inst.removeModifier(SLOW_ID);
         if (amount != 0.0) {
            inst.addTransientModifier(new AttributeModifier(SLOW_ID, amount, Operation.ADD_MULTIPLIED_TOTAL));
         }
      }
   }

   private static final class Boulder {
      private final ServerLevel level;
      private final ServerPlayer thrower;
      private final Vec3 ripPoint;
      private final float rage;
      private final Block material;
      private final double size;
      private BlockFxEntity chunk;
      private Vec3 launchFrom;
      private Vec3 lastChunkPos;
      private Vec3 target;
      private int flightTicks;
      private int age;
      private int whipAge = -1;
      private boolean hurlRequested;
      private boolean launched;

      private Boulder(ServerLevel level, ServerPlayer thrower, Vec3 ripPoint, float rage) {
         this.level = level;
         this.thrower = thrower;
         this.ripPoint = ripPoint;
         this.rage = rage;
         this.material = GammaFx.boulderMaterial(level, ripPoint);
         this.size = 0.8 + 0.7 * (double)rage;
      }

      private boolean tick() {
         this.age++;
         if (!this.thrower.isRemoved() && this.thrower.isAlive() && (this.chunk == null || !this.chunk.isRemoved() || this.launched)) {
            if (this.launched) {
               return this.tickFlight();
            } else {
               if (this.whipAge >= 0) {
                  this.tickWhip();
               } else if (this.age == 4) {
                  GammaFx.boulderRip(this.level, this.ripPoint, this.material);
                  this.chunk = Vfx.blockTracked(
                     this.level, this.ripPoint.add(0.0, 0.4, 0.0), this.material, this.size, this.size, this.size, 600, this.level.random.nextFloat() * 360.0F
                  );
                  this.lastChunkPos = this.ripPoint.add(0.0, 0.4, 0.0);
               } else if (this.age > 4 && this.age <= 10 && this.chunk != null) {
                  float s = (float)(this.age - 4) / 6.0F;
                  float ease = s * s * (3.0F - 2.0F * s);
                  this.setChunk(this.ripPoint.add(0.0, 0.4, 0.0).lerp(this.overhead(), (double)ease));
                  if (this.age % 2 == 0) {
                     GammaFx.boulderShedTick(this.level, this.lastChunkPos, this.material);
                  }

                  if (this.age == 10) {
                     BoulderControl.slow(this.thrower, -0.2);
                     if (this.hurlRequested) {
                        this.startWhip();
                     } else {
                        PowerService.playAnimation(this.thrower, "gamma_boulder_carry");
                     }
                  }
               } else if (this.age > 10 && this.chunk != null) {
                  this.setChunk(this.overhead());
                  if (this.age % 6 == 0) {
                     GammaFx.boulderShedTick(this.level, this.lastChunkPos, this.material);
                  }

                  if (this.hurlRequested || this.age - 10 >= 400) {
                     this.startWhip();
                  }
               }

               return true;
            }
         } else {
            this.discard();
            return false;
         }
      }

      private void startWhip() {
         this.whipAge = this.age;
         PowerService.stopAnimation(this.thrower);
         PowerService.playAnimation(this.thrower, "gamma_boulder_hurl");
      }

      private void tickWhip() {
         int t = this.age - this.whipAge;
         if (t < 4 && this.chunk != null) {
            float s = Math.min(1.0F, (float)t / 3.0F);
            this.setChunk(this.overhead().lerp(this.windback(), (double)s));
         } else {
            this.target = AimUtil.aimedPosition(this.thrower, 48.0);
            this.launchFrom = this.lastChunkPos == null ? this.windback() : this.lastChunkPos;
            this.flightTicks = Math.max(4, (int)Math.ceil(this.launchFrom.distanceTo(this.target) / 2.4));
            this.launched = true;
            BoulderControl.slow(this.thrower, 0.0);
            GammaFx.boulderHurl(this.level, this.launchFrom);
         }
      }

      private boolean tickFlight() {
         if (this.chunk != null && !this.chunk.isRemoved()) {
            float s = Math.min(1.0F, (float)(this.age - this.whipAge - 4) / (float)this.flightTicks);
            double h = 0.5 + this.launchFrom.distanceTo(this.target) * 0.02;
            Vec3 p = this.launchFrom.lerp(this.target, (double)s).add(0.0, 4.0 * h * (double)s * (1.0 - (double)s), 0.0);
            LivingEntity struck = this.sweptHit(p);
            if (struck != null) {
               this.shatter(struck.position().add(0.0, (double)struck.getBbHeight() * 0.4, 0.0));
               return false;
            } else {
               this.setChunk(p);
               if (this.age % 2 == 0) {
                  GammaFx.boulderFlightTick(this.level, p, this.material);
               }

               if (s >= 1.0F) {
                  this.shatter(this.target);
                  return false;
               } else {
                  return true;
               }
            }
         } else {
            return false;
         }
      }

      private void setChunk(Vec3 p) {
         this.lastChunkPos = p;
         this.chunk.setPos(p.x, p.y, p.z);
      }

      private LivingEntity sweptHit(Vec3 to) {
         Vec3 from = this.lastChunkPos == null ? to : this.lastChunkPos;
         AABB swept = new AABB(from, to).inflate(this.size * 0.5 + 0.3);
         List<LivingEntity> hits = this.level
            .getEntitiesOfClass(LivingEntity.class, swept, e -> e != this.thrower && e.isAlive() && !this.isOwnPet(e) && Harm.allowed(this.thrower, e));
         return hits.stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(from))).orElse(null);
      }

      private boolean isOwnPet(LivingEntity e) {
         if (e instanceof TamableAnimal pet && pet.getOwner() == this.thrower) {
            return true;
         }

         return false;
      }

      private void shatter(Vec3 pos) {
         if (this.chunk != null && !this.chunk.isRemoved()) {
            this.chunk.discard();
         }

         GammaFx.boulderShatter(this.level, pos, this.material, this.rage);
         float factor = 0.75F + 0.75F * this.rage;
         float damage = MasteryScaling.scaledDamage(16.0F, this.thrower, "BoulderThrowAbility", factor - 1.0F);
         double radius = 3.0 + 1.0 * (double)this.rage;
         boolean connected = false;
         LivingEntity threat = null;

         for (LivingEntity victim : this.level
            .getEntitiesOfClass(
               LivingEntity.class,
               new AABB(pos, pos).inflate(radius),
               e -> e != this.thrower && e.isAlive() && e.distanceToSqr(pos) <= radius * radius && Harm.allowed(this.thrower, e)
            )) {
            Dmg.hurt(victim, this.level, Dmg.by(this.thrower, DamageTypes.PLAYER_ATTACK), damage);
            Vec3 push = victim.position().subtract(pos);
            push = push.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : new Vec3(push.x, 0.0, push.z).normalize();
            victim.push(push.x * 1.2 * (double)factor, 0.45, push.z * 1.2 * (double)factor);
            victim.hurtMarked = true;
            connected = true;
            if (threat == null && Foes.counts(this.thrower, victim)) {
               threat = victim;
            }
         }

         if (connected) {
            MasterySystem.award(this.thrower, MasteryReason.HIT, threat);
         }
      }

      private Vec3 overhead() {
         double hands = (double)this.thrower.getBbHeight() + 0.15 * (double)this.thrower.getScale();
         return this.thrower.position().add(0.0, hands + this.size * 0.5, 0.0);
      }

      private Vec3 windback() {
         float yawRad = this.thrower.getYRot() * (float) (Math.PI / 180.0);
         Vec3 back = new Vec3(Math.sin((double)yawRad), 0.0, -Math.cos((double)yawRad));
         return this.thrower.position().add(back.scale(1.1)).add(0.0, (double)this.thrower.getBbHeight() + 0.35, 0.0);
      }

      private void discard() {
         if (this.chunk != null && !this.chunk.isRemoved()) {
            this.chunk.discard();
         }
      }
   }
}
