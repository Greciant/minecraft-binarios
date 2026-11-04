package com.awakenedmod.awakened.content.gamma;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.content.scaler.GripAnchorEntity;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.AwakenedThreat;
import com.awakenedmod.awakened.core.BodyPinState;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.TickFreeze;
import com.awakenedmod.awakened.core.net.ForcedCameraPayload;
import com.awakenedmod.awakened.core.net.HeldRootPayload;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class SlamControl {
   private static final int GRAB_END = 6;
   private static final int[] SIDE_BEATS = new int[]{16, 28, 40};
   private static final int FINISH_BEAT = 50;
   private static final int END = 56;
   private static final int CAMERA_TICKS = 62;
   private static final float SLAM_DAMAGE = 6.0F;
   private static final float FINISH_DAMAGE = 14.0F;
   private static final double SHOCK_RADIUS = 2.0;
   private static final double SIDE_REACH = 0.5;
   private static final double FRONT_REACH = 0.9;
   private static final double SLAM_FORWARD = 0.6;
   private static final ResourceLocation ROOT_ID = AwId.of("gamma_slam_root");
   private static final Map<UUID, SlamControl.Slam> ACTIVE = new ConcurrentHashMap<>();

   private SlamControl() {
   }

   public static boolean isSlamming(ServerPlayer holder) {
      return ACTIVE.containsKey(holder.getUUID());
   }

   public static boolean isVictim(LivingEntity entity) {
      for (SlamControl.Slam slam : ACTIVE.values()) {
         if (slam.victim == entity) {
            return true;
         }
      }

      return false;
   }

   public static void begin(ServerPlayer holder, LivingEntity victim, float rage) {
      if (victim.isPassenger()) {
         victim.stopRiding();
      }

      boolean var10000;
      label25: {
         if (victim instanceof AwakenedThreat t && t.isBoss()) {
            var10000 = true;
            break label25;
         }

         var10000 = false;
      }

      boolean boss = var10000;
      SlamControl.Slam slam = new SlamControl.Slam(holder, victim, boss, rage);
      ACTIVE.put(holder.getUUID(), slam);
      root(holder, true);
      victim.noPhysics = true;
      if (boss) {
         TickFreeze.freeze(victim);
      } else if (victim instanceof ServerPlayer) {
         slam.anchor = spawnAnchor(holder, victim);
      } else if (victim instanceof Mob mob) {
         mob.setNoAi(true);
         mob.getNavigation().stop();
      }

      PowerService.playAnimation(holder, "gamma_slam");
      PacketDistributor.sendToPlayer(holder, new ForcedCameraPayload(62), new CustomPacketPayload[0]);
      GammaFx.slamGrab(Dmg.serverLevel(holder), victim);
   }

   public static void tick(ServerPlayer holder) {
      SlamControl.Slam slam = ACTIVE.get(holder.getUUID());
      if (slam != null) {
         LivingEntity victim = slam.victim;
         if (holder.isAlive() && victim.isAlive() && !victim.isRemoved() && victim.level() == holder.level()) {
            slam.age++;
            ServerLevel level = Dmg.serverLevel(holder);
            double scale = (double)holder.getScale();
            Vec3 side = right(holder.getYRot());
            Vec3 ahead = forward(holder.getYRot());
            Vec3 base = holder.position();
            double low = 0.6 * scale;
            Vec3 overhead = base.add(ahead.scale(0.2 * scale)).add(0.0, (double)holder.getBbHeight() + 0.3, 0.0);
            Vec3 leftGround = base.add(side.scale(-0.5 * scale)).add(ahead.scale(0.6 * scale)).add(0.0, low, 0.0);
            Vec3 rightGround = base.add(side.scale(0.5 * scale)).add(ahead.scale(0.6 * scale)).add(0.0, low, 0.0);
            Vec3 aheadGround = base.add(ahead.scale(0.9 * scale)).add(0.0, low, 0.0);
            Vec3 p = clearOfHolder(swingPoint(slam.age, holder, overhead, leftGround, rightGround, aheadGround), holder, victim);
            moveVictim(slam, p);

            for (int i = 0; i < SIDE_BEATS.length; i++) {
               if (slam.age == SIDE_BEATS[i]) {
                  Vec3 hitPos = i % 2 == 0 ? leftGround : rightGround;
                  GammaFx.slamImpact(level, floor(hitPos, base), slam.rage);
                  hit(holder, slam, hitPos, 6.0F);
                  return;
               }
            }

            if (slam.age == 50) {
               GammaFx.slamFinish(level, floor(aheadGround, base), slam.rage);
               hit(holder, slam, aheadGround, 14.0F);
               MasterySystem.award(holder, slam.boss ? MasteryReason.KILL : MasteryReason.HIT, victim);
               if (Foes.countsAsKill(holder, victim)) {
                  QuestSystem.feat(holder, "gamma_ragdoll");
               }
            } else if (slam.age >= 56) {
               toss(holder, slam, ahead);
               release(holder);
            }
         } else {
            release(holder);
         }
      }
   }

   private static Vec3 swingPoint(int age, ServerPlayer holder, Vec3 overhead, Vec3 leftGround, Vec3 rightGround, Vec3 aheadGround) {
      if (age <= 6) {
         Vec3 chest = holder.position().add(0.0, (double)holder.getBbHeight() * 0.6, 0.0);
         return chest.lerp(overhead, (double)frac(age, 0, 6));
      } else {
         int prev = 6;
         Vec3 prevPoint = overhead;

         for (int i = 0; i < SIDE_BEATS.length; i++) {
            Vec3 target = i % 2 == 0 ? leftGround : rightGround;
            if (age <= SIDE_BEATS[i]) {
               return i == 0
                  ? prevPoint.lerp(target, (double)frac(age, prev, SIDE_BEATS[i]))
                  : arc(prevPoint, target, overhead, frac(age, prev, SIDE_BEATS[i]));
            }

            prev = SIDE_BEATS[i];
            prevPoint = target;
         }

         return age <= 50 ? arc(prevPoint, aheadGround, overhead, frac(age, prev, 50)) : aheadGround;
      }
   }

   private static void hit(ServerPlayer holder, SlamControl.Slam slam, Vec3 pos, float base) {
      ServerLevel level = Dmg.serverLevel(holder);
      float factor = 0.5F + slam.rage;
      float damage = MasteryScaling.scaledDamage(base, holder, "RagdollSlamAbility", factor - 1.0F);
      Dmg.hurt(slam.victim, level, Dmg.by(holder, DamageTypes.PLAYER_ATTACK), damage);
      slam.victim.hurtMarked = true;
      float splash = damage * 0.5F;

      for (LivingEntity bystander : level.getEntitiesOfClass(
         LivingEntity.class, new AABB(pos, pos).inflate(2.0), e -> e != holder && e != slam.victim && e.isAlive() && Harm.allowed(holder, e)
      )) {
         Dmg.hurt(bystander, level, Dmg.by(holder, DamageTypes.PLAYER_ATTACK), splash);
         Vec3 push = bystander.position().subtract(pos);
         push = push.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : new Vec3(push.x, 0.0, push.z).normalize();
         bystander.push(push.x * 0.5, 0.3, push.z * 0.5);
         bystander.hurtMarked = true;
      }
   }

   private static void toss(ServerPlayer holder, SlamControl.Slam slam, Vec3 ahead) {
      LivingEntity victim = slam.victim;
      restore(slam);
      Vec3 throwV = ahead.scale(0.6).add(0.0, -0.1, 0.0);
      Harm.setMotion(holder, victim, throwV);
      victim.fallDistance = 0.0F;
   }

   private static void moveVictim(SlamControl.Slam slam, Vec3 p) {
      LivingEntity victim = slam.victim;
      if (slam.anchor != null) {
         slam.anchor.setPos(p.x, p.y, p.z);
         slam.anchor.setDeltaMovement(Vec3.ZERO);
         if (victim.getVehicle() != slam.anchor) {
            forceRide(victim, slam.anchor);
         }
      } else {
         victim.setDeltaMovement(Vec3.ZERO);
         victim.fallDistance = 0.0F;
         victim.setPos(p.x, p.y, p.z);
      }
   }

   public static void clearPlayer(ServerPlayer holder) {
      release(holder);
   }

   private static void release(ServerPlayer holder) {
      SlamControl.Slam slam = ACTIVE.remove(holder.getUUID());
      if (slam != null) {
         restore(slam);
         PowerService.stopAnimation(holder);
         PacketDistributor.sendToPlayer(holder, new ForcedCameraPayload(0), new CustomPacketPayload[0]);
      }
   }

   private static void restore(SlamControl.Slam slam) {
      if (!slam.restored) {
         slam.restored = true;
         LivingEntity victim = slam.victim;
         if (slam.boss) {
            TickFreeze.release(victim);
         }

         detachAnchor(slam);
         root(slam.holder, false);
         if (victim instanceof Mob mob) {
            mob.setNoAi(false);
         }

         victim.noPhysics = false;
         victim.setDeltaMovement(Vec3.ZERO);
         victim.fallDistance = 0.0F;
      }
   }

   private static void root(ServerPlayer holder, boolean rooted) {
      AttributeInstance inst = holder.getAttribute(Attributes.MOVEMENT_SPEED);
      if (inst != null) {
         inst.removeModifier(ROOT_ID);
         if (rooted) {
            inst.addTransientModifier(new AttributeModifier(ROOT_ID, -1.0, Operation.ADD_MULTIPLIED_TOTAL));
         }
      }

      BodyPinState.setServer(holder.getUUID(), rooted);
      PacketDistributor.sendToPlayer(holder, new HeldRootPayload(rooted), new CustomPacketPayload[0]);
   }

   private static GripAnchorEntity spawnAnchor(ServerPlayer holder, LivingEntity victim) {
      ServerLevel level = Dmg.serverLevel(holder);
      GripAnchorEntity anchor = new GripAnchorEntity(AwakenedEntities.GRIP_ANCHOR.get(), level);
      Vec3 overhead = holder.position().add(0.0, (double)holder.getBbHeight() + 0.6, 0.0);
      anchor.setPos(overhead.x, overhead.y, overhead.z);
      level.addFreshEntity(anchor);
      forceRide(victim, anchor);
      return anchor;
   }

   private static void detachAnchor(SlamControl.Slam slam) {
      if (slam.anchor != null) {
         if (slam.victim.getVehicle() == slam.anchor) {
            slam.victim.stopRiding();
         }

         slam.anchor.discard();
         slam.anchor = null;
      }
   }

   private static void forceRide(LivingEntity rider, GripAnchorEntity seat) {
      GripAnchorEntity.mount(rider, seat);
   }

   private static float frac(int age, int from, int to) {
      return Mth.clamp((float)(age - from) / (float)(to - from), 0.0F, 1.0F);
   }

   private static Vec3 floor(Vec3 p, Vec3 base) {
      return new Vec3(p.x, base.y + 0.05, p.z);
   }

   private static Vec3 clearOfHolder(Vec3 p, ServerPlayer holder, LivingEntity victim) {
      if (p.y >= holder.getY() + (double)holder.getBbHeight()) {
         return p;
      } else {
         double minClear = (double)(holder.getBbWidth() + victim.getBbWidth()) * 0.5 + 0.15;
         double dx = p.x - holder.getX();
         double dz = p.z - holder.getZ();
         double horiz = Math.sqrt(dx * dx + dz * dz);
         if (horiz >= minClear) {
            return p;
         } else if (horiz < 1.0E-4) {
            Vec3 fwd = forward(holder.getYRot());
            return new Vec3(holder.getX() + fwd.x * minClear, p.y, holder.getZ() + fwd.z * minClear);
         } else {
            double s = minClear / horiz;
            return new Vec3(holder.getX() + dx * s, p.y, holder.getZ() + dz * s);
         }
      }
   }

   private static Vec3 arc(Vec3 a, Vec3 b, Vec3 apex, float t) {
      float u = 1.0F - t;
      return a.scale((double)(u * u)).add(apex.scale((double)(2.0F * u * t))).add(b.scale((double)(t * t)));
   }

   private static Vec3 forward(float yawDeg) {
      double rad = Math.toRadians((double)yawDeg);
      return new Vec3(-Math.sin(rad), 0.0, Math.cos(rad));
   }

   private static Vec3 right(float yawDeg) {
      double rad = Math.toRadians((double)yawDeg);
      return new Vec3(-Math.cos(rad), 0.0, -Math.sin(rad));
   }

   private static final class Slam {
      private final ServerPlayer holder;
      private final LivingEntity victim;
      private final boolean boss;
      private final float rage;
      private GripAnchorEntity anchor;
      private int age;
      private boolean restored;

      private Slam(ServerPlayer holder, LivingEntity victim, boolean boss, float rage) {
         this.holder = holder;
         this.victim = victim;
         this.boss = boss;
         this.rage = rage;
      }
   }
}
