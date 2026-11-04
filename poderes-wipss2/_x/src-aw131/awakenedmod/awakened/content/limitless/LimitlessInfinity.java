package com.awakenedmod.awakened.content.limitless;

import com.awakenedmod.awakened.core.AbilityGate;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.HitKind;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.PowerResource;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.rules.PvpRules;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import com.awakenedmod.awakened.mixin.LivingHurtCooldownAccessor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class LimitlessInfinity {
   private static final int BRAKE_GIVE_UP = 20;
   static final String BALANCE_KEY = "LimitlessInfinity";
   private static final Map<UUID, Map<Integer, LimitlessInfinity.Shot>> SHOTS = new HashMap<>();
   private static final Map<Integer, LimitlessInfinity.Memo> LET_GO = new HashMap<>();
   private static final Map<UUID, Long> BROKE_CUE = new HashMap<>();

   private LimitlessInfinity() {
   }

   public static boolean up(ServerPlayer player) {
      if (player.isAlive() && !player.isSpectator()) {
         PowerData data = PowerService.data(player);
         return "limitless".equals(data.powerId()) && LimitlessPower.INFINITY.enabled(data) && !AbilityGate.isSealed(player);
      } else {
         return false;
      }
   }

   public static void tick(ServerPlayer player) {
      if (!up(player)) {
         releaseAll(player);
      } else {
         ServerLevel level = player.serverLevel();
         int now = (int)level.getGameTime();
         Map<Integer, LimitlessInfinity.Shot> shots = SHOTS.get(player.getUUID());
         if (shots != null) {
            advance(player, level, shots, now);
         }

         scan(player, level, now);
         if (now % 20 == 0) {
            LET_GO.values().removeIf(m -> m.until < (long)now);
         }
      }
   }

   private static void advance(ServerPlayer player, ServerLevel level, Map<Integer, LimitlessInfinity.Shot> shots, int now) {
      Iterator<LimitlessInfinity.Shot> it = shots.values().iterator();

      while (it.hasNext()) {
         LimitlessInfinity.Shot s = it.next();
         Projectile p = s.hold.shot();
         if (!p.isAlive()) {
            it.remove();
         } else if (p.level() != level) {
            it.remove();
            letGo(player, s, now);
         } else if (s.stoppedAt < 0 && now - s.started > 20) {
            it.remove();
            letGo(player, s, now);
         } else if (s.stoppedAt < 0) {
            brake(player, level, s, now);
         } else if (now - s.stoppedAt >= 10) {
            it.remove();
            letGo(player, s, now);
         } else {
            s.hold.hold(s.stop);
         }
      }

      if (shots.isEmpty()) {
         SHOTS.remove(player.getUUID());
      }
   }

   private static void brake(ServerPlayer player, ServerLevel level, LimitlessInfinity.Shot s, int now) {
      Projectile p = s.hold.shot();
      Vec3 pos = p.position();
      double left = pos.distanceTo(s.stop);
      if (left < 0.06) {
         s.stoppedAt = now;
         s.hold.hold(s.stop);
         LimitlessFx.shotStopped(level, s.stop);
         send(player, new InfinityStopPayload(player.getId(), false, p.getId(), s.stop, s.from, s.started, now));
         reward(player, s.shooter);
      } else {
         Vec3 dir = s.stop.subtract(pos).normalize();
         p.setDeltaMovement(dir.scale(Math.min(left, Math.max(0.05, left * 0.45))));
         ShotHold.push(p);
      }
   }

   private static void letGo(ServerPlayer player, LimitlessInfinity.Shot s, int now) {
      Projectile p = s.hold.shot();
      LET_GO.put(p.getId(), new LimitlessInfinity.Memo(player.getUUID(), (long)(now + 80)));
      if (p.level() instanceof ServerLevel shotLevel) {
         s.hold.drop(shotLevel);
      }
   }

   private static void scan(ServerPlayer player, ServerLevel level, int now) {
      double reach = 5.5;
      AABB box = player.getBoundingBox().inflate(reach);

      for (Projectile p : level.getEntitiesOfClass(Projectile.class, box, px -> candidate(player, px))) {
         double speed = p.getDeltaMovement().length();
         if (!(p.position().distanceTo(player.position()) > reach + 2.0 * speed)) {
            LimitlessInfinity.Course course = course(player, p);
            if (course != null && !(course.along > 2.5 + speed) && pay(player, shotCost(player, p))) {
               Vec3 heading = p.getDeltaMovement().normalize();
               catchShot(player, p, course.stop, heading.scale(-1.0), now, false);
            }
         }
      }
   }

   private static boolean candidate(ServerPlayer player, Projectile p) {
      if (p.isAlive() && !(p instanceof FishingHook) && !stopping(player, p) && !LET_GO.containsKey(p.getId())) {
         Entity owner = p.getOwner();
         return owner != player && (owner == null || Harm.allowed(owner, player)) ? p.getDeltaMovement().lengthSqr() >= 0.0025000000000000005 : false;
      } else {
         return false;
      }
   }

   @Nullable
   private static LimitlessInfinity.Course course(ServerPlayer player, Projectile p) {
      Vec3 pos = p.position();
      Vec3 v = p.getDeltaMovement();
      double speed = v.length();
      Vec3 dir = v.scale(1.0 / speed);
      double length = 5.5 + 3.0 * speed;
      double hit = (double)player.getBbWidth() * 0.5 + (double)p.getBbWidth() * 0.5 + 0.2;
      double stopDist = 0.9;
      double bottom = player.getY() + 0.1;
      double top = player.getY() + (double)player.getBbHeight() - 0.1;
      Vec3 stop = null;
      double along = 0.0;
      boolean onCourse = false;

      for (double t = 0.0; t <= length; t += 0.1) {
         Vec3 at = pos.add(dir.scale(t));
         double dx = at.x - player.getX();
         double dz = at.z - player.getZ();
         double dy = at.y < bottom ? bottom - at.y : (at.y > top ? at.y - top : 0.0);
         double d = Math.sqrt(dx * dx + dz * dz + dy * dy);
         if (stop == null && d <= stopDist) {
            stop = at;
            along = t;
         }

         if (d <= hit) {
            onCourse = true;
            break;
         }
      }

      return onCourse && stop != null ? new LimitlessInfinity.Course(stop, along) : null;
   }

   private static void catchShot(ServerPlayer player, Projectile p, Vec3 stop, Vec3 from, int now, boolean already) {
      ShotHold hold = new ShotHold(p, from.scale(-1.0));
      Entity shooter = HitKind.shooterOf(p);
      LimitlessInfinity.Shot s = new LimitlessInfinity.Shot(hold, stop, from, now, shooter);
      SHOTS.computeIfAbsent(player.getUUID(), k -> new HashMap<>()).put(p.getId(), s);
      if (already) {
         s.stoppedAt = now;
         hold.hold(stop);
         LimitlessFx.shotStopped(player.serverLevel(), stop);
         reward(player, shooter);
      }

      send(player, new InfinityStopPayload(player.getId(), false, p.getId(), stop, from, now, already ? now : -1));
   }

   public static boolean onImpact(ServerPlayer player, Projectile p) {
      if (!up(player)) {
         return false;
      } else if (stopping(player, p) || letGoBy(p, player)) {
         return true;
      } else if (p instanceof FishingHook) {
         return false;
      } else {
         Entity owner = p.getOwner();
         if (owner != player && (owner == null || Harm.allowed(owner, player)) && pay(player, shotCost(player, p))) {
            Vec3 v = p.getDeltaMovement();
            Vec3 from = v.lengthSqr() > 1.0E-6 ? v.normalize().scale(-1.0) : towardShooter(player, owner);
            catchShot(player, p, p.position(), from, (int)player.serverLevel().getGameTime(), true);
            return true;
         } else {
            return false;
         }
      }
   }

   public static boolean onIncoming(ServerPlayer player, DamageSource source, float amount) {
      if (up(player) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !source.is(DamageTypes.SONIC_BOOM)) {
         Entity direct = source.getDirectEntity();
         if (direct == null || !stopping(player, direct) && !letGoBy(direct, player)) {
            amount = PvpRules.scaled(player, source, amount);
            HitKind.Shape shape = HitKind.shape(source);
            if (shape == HitKind.Shape.PROJECTILE || source.is(DamageTypeTags.IS_PROJECTILE)) {
               return blockShot(player, source, direct, amount);
            } else {
               return shape != HitKind.Shape.BODY && shape != HitKind.Shape.FIST ? false : blockMelee(player, source, amount);
            }
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   private static boolean blockShot(ServerPlayer player, DamageSource source, @Nullable Entity direct, float amount) {
      Entity shooter = direct != null ? HitKind.shooterOf(direct) : null;
      if (shooter == null) {
         shooter = source.getEntity();
      }

      if (shooter != player
         && !source.is(DamageTypes.THORNS)
         && (shooter == null || Harm.allowed(shooter, player))
         && wouldLand(player, source, amount)
         && pay(player, hitCost(player, source, amount))) {
         int now = (int)player.serverLevel().getGameTime();
         if (direct instanceof Projectile p && p.isAlive()) {
            Vec3 v = p.getDeltaMovement();
            Vec3 from = v.lengthSqr() > 1.0E-6 ? v.normalize().scale(-1.0) : towardShooter(player, shooter);
            catchShot(player, p, p.position(), from, now, true);
            return true;
         }

         Vec3 from = towardShooter(player, shooter);
         Vec3 stop = chest(player).add(from.scale(0.9));
         LimitlessFx.shotStopped(player.serverLevel(), stop);
         send(player, new InfinityStopPayload(player.getId(), false, -1, stop, from, now, now));
         reward(player, shooter);
         return true;
      } else {
         return false;
      }
   }

   private static boolean blockMelee(ServerPlayer player, DamageSource source, float amount) {
      LivingEntity attacker = HitKind.responsible(player, source);
      if (attacker != null && inMeleeReach(player, attacker) && wouldLand(player, source, amount) && pay(player, hitCost(player, source, amount))) {
         Vec3 toward = attacker.position().subtract(player.position());
         Vec3 flat = new Vec3(toward.x, 0.0, toward.z);
         flat = flat.lengthSqr() < 1.0E-6 ? player.getLookAngle().multiply(1.0, 0.0, 1.0).normalize() : flat.normalize();
         showSwingStopped(player, flat);
         reward(player, attacker);
         return true;
      } else {
         return false;
      }
   }

   public static void showSwingStopped(ServerPlayer player, Vec3 flat) {
      Vec3 stop = chest(player).add(flat.scale(0.55)).add(0.0, 0.25, 0.0);
      int now = (int)player.serverLevel().getGameTime();
      LimitlessFx.meleeStopped(player.serverLevel(), stop);
      send(player, new InfinityStopPayload(player.getId(), true, -1, stop, flat, now, -1));
   }

   private static boolean inMeleeReach(ServerPlayer player, LivingEntity attacker) {
      if (!HitKind.inReach(player, attacker, 3.5 + (double)attacker.getBbWidth() * 0.5)) {
         return false;
      } else {
         if (attacker instanceof Player p && !(AimUtil.reachToBox(player, p.getEyePosition()) <= p.entityInteractionRange() + 0.5)) {
            return false;
         }

         return true;
      }
   }

   private static boolean wouldLand(ServerPlayer player, DamageSource source, float amount) {
      float lastHurt = ((LivingHurtCooldownAccessor)player).awakened$getLastHurt();
      return player.invulnerableTime <= 10 || source.is(DamageTypeTags.BYPASSES_COOLDOWN) || !(amount <= lastHurt);
   }

   private static float hitCost(ServerPlayer player, DamageSource source, float damage) {
      float landed = source.is(DamageTypeTags.BYPASSES_ARMOR)
         ? damage
         : CombatRules.getDamageAfterAbsorb(player, damage, source, (float)player.getArmorValue(), (float)player.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
      return Math.max(1.0F, landed * 1.0F);
   }

   private static float shotCost(ServerPlayer player, Projectile p) {
      Entity owner = p.getOwner();
      if (p instanceof AbstractArrow arrow) {
         float damage = (float)Mth.ceil(arrow.getDeltaMovement().length() * 2.0);
         return hitCost(player, player.damageSources().arrow(arrow, (Entity)(owner != null ? owner : arrow)), damage);
      } else {
         return hitCost(player, player.damageSources().thrown(p, owner), 2.0F);
      }
   }

   private static boolean pay(ServerPlayer player, float base) {
      PowerResource resource = PowerService.resource(player);
      float cost = StaminaSystem.effectiveCost(base, "LimitlessInfinity");
      if (resource.canAfford(player, cost)) {
         resource.spend(player, cost);
         PowerService.sync(player);
         return true;
      } else {
         long now = player.serverLevel().getGameTime();
         Long last = BROKE_CUE.get(player.getUUID());
         if (last == null || now - last >= 40L) {
            BROKE_CUE.put(player.getUUID(), now);
            PowerService.feedback(player, 0);
         }

         return false;
      }
   }

   private static void reward(ServerPlayer player, @Nullable Entity attacker) {
      boolean attacking = attacker instanceof Mob mob ? mob.getTarget() == player : attacker instanceof Player;
      if (attacking) {
         MasterySystem.award(player, MasteryReason.SURVIVAL, attacker);
      }
   }

   private static Vec3 chest(ServerPlayer player) {
      return player.position().add(0.0, 1.2, 0.0);
   }

   private static Vec3 towardShooter(ServerPlayer player, @Nullable Entity shooter) {
      Vec3 d = shooter != null ? shooter.getEyePosition().subtract(chest(player)) : player.getLookAngle();
      return d.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : d.normalize();
   }

   private static boolean stopping(ServerPlayer player, Entity shot) {
      Map<Integer, LimitlessInfinity.Shot> shots = SHOTS.get(player.getUUID());
      return shots != null && shots.containsKey(shot.getId());
   }

   private static boolean letGoBy(Entity shot, ServerPlayer player) {
      LimitlessInfinity.Memo memo = LET_GO.get(shot.getId());
      return memo != null && memo.by.equals(player.getUUID());
   }

   private static void send(ServerPlayer player, InfinityStopPayload payload) {
      PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, payload, new CustomPacketPayload[0]);
   }

   public static void releaseAll(ServerPlayer player) {
      Map<Integer, LimitlessInfinity.Shot> shots = SHOTS.remove(player.getUUID());
      if (shots != null) {
         int now = (int)player.serverLevel().getGameTime();

         for (LimitlessInfinity.Shot s : shots.values()) {
            if (s.hold.shot().isAlive()) {
               letGo(player, s, now);
            }
         }
      }
   }

   public static void forget(ServerPlayer player) {
      releaseAll(player);
      BROKE_CUE.remove(player.getUUID());
      LET_GO.values().removeIf(m -> m.by.equals(player.getUUID()));
   }

   public static void forgetAll() {
      SHOTS.clear();
      LET_GO.clear();
      BROKE_CUE.clear();
   }

   private static record Course(Vec3 stop, double along) {
   }

   private static record Memo(UUID by, long until) {
   }

   private static final class Shot {
      final ShotHold hold;
      final Vec3 stop;
      final Vec3 from;
      final int started;
      @Nullable
      final Entity shooter;
      int stoppedAt = -1;

      Shot(ShotHold hold, Vec3 stop, Vec3 from, int started, @Nullable Entity shooter) {
         this.hold = hold;
         this.stop = stop;
         this.from = from;
         this.started = started;
         this.shooter = shooter;
      }
   }
}
