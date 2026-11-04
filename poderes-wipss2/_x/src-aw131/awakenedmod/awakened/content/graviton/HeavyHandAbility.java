package com.awakenedmod.awakened.content.graviton;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.power.PowerResource;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class HeavyHandAbility extends Ability {
   public static final String ID = "heavy_hand";
   public static final float TAP_COST = 10.0F;
   public static final float HOLD_COST = 18.0F;
   public static final double RANGE = 24.0;
   public static final String CRUSH_FEAT = "graviton_crush";
   public static final int HOLD_TICKS = 8;
   public static final int TAP_WINDOW = 7;
   private static final double AIM_CONE_DEG = 3.0;
   private static final double LEASH = 30.0;
   private static final float YANK_DAMAGE = 4.0F;
   private static final float SHOVE_DAMAGE = 2.0F;
   private static final double YANK_REACH = 1.6;
   private static final double SHOVE_STRENGTH = 2.2;
   private static final double SHOVE_LIFT = 0.45;
   private static final String ANIM_YANK = "heavy_hand_yank";
   private static final String ANIM_SHOVE = "heavy_hand_shove";
   private static final String ANIM_CRUSH = "heavy_hand_crush";
   private static final String ANIM_LAUNCH = "heavy_hand_launch";
   private static final double FAR_LOOK = 64.0;
   private final Map<UUID, HeavyHandAbility.Hold> holds = this.activeState();
   private final HeavyHandBodies bodies = new HeavyHandBodies(this.balanceKey());

   public HeavyHandAbility() {
      super("heavy_hand", 10.0F, 0);
   }

   @Override
   public float staminaCost(ServerPlayer player) {
      return 0.0F;
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().boostsDamage().build();
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (this.holds.containsKey(player.getUUID())) {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      } else {
         PowerResource resource = this.resource(player);
         if (!resource.canAfford(player, this.billed(10.0F))) {
            resource.refuse(player);
            return this.failSilently(player);
         } else {
            LivingEntity target = AimUtil.aimedLivingSoft(player, 24.0, 3.0, e -> validTarget(player, e));
            if (target == null) {
               return this.refuseAim(player);
            } else {
               HeavyHandAbility.Hold hold = new HeavyHandAbility.Hold(target, player.isShiftKeyDown());
               this.holds.put(player.getUUID(), hold);
               HeavyHandFx.press(player.serverLevel(), player, target);
               show(player, hold, 0.0F);
               return true;
            }
         }
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      this.bodies.tick(player, level);
      HeavyHandAbility.Hold hold = this.holds.get(player.getUUID());
      if (hold != null) {
         hold.ticks++;
         if (!stillHeld(player, level, hold.target)) {
            this.holds.remove(player.getUUID());
            this.lose(player, level, hold);
         } else {
            hold.shift = player.isShiftKeyDown();
            float f = Math.min(1.0F, (float)hold.ticks / 8.0F);
            show(player, hold, f);
            HeavyHandFx.holding(level, player, hold.target, hold.shift, f, hold.ticks);
            shudder(hold.target, hold.shift, f, level);
            if (hold.ticks >= 8) {
               this.holds.remove(player.getUUID());
               end(player, hold);
               this.fireHold(player, level, hold);
            }
         }
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      HeavyHandAbility.Hold hold = this.holds.remove(player.getUUID());
      if (hold != null) {
         ServerLevel level = player.serverLevel();
         if (hold.ticks <= 7 && stillHeld(player, level, hold.target)) {
            end(player, hold);
            hold.shift = player.isShiftKeyDown();
            this.fireTap(player, level, hold);
         } else {
            this.lose(player, level, hold);
         }
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      HeavyHandAbility.Hold hold = this.holds.remove(player.getUUID());
      if (hold != null) {
         end(player, hold);
      }

      HeavyHandVisual.set(player, HeavyHandVisual.NONE);
      this.bodies.release(player);
      super.clearPlayer(player);
   }

   private void fireTap(ServerPlayer player, ServerLevel level, HeavyHandAbility.Hold hold) {
      if (this.pay(player, level, 10.0F, hold)) {
         LivingEntity target = hold.target;
         Vec3 was = target.position();
         snap(player, hold.shift ? 1 : 0);
         if (hold.shift) {
            PowerService.playAnimation(player, "heavy_hand_shove");
            this.hit(player, level, target, 2.0F);
            HeavyHandFx.shove(level, player, target);
            GravitySupport.shove(target, player.position(), 2.2, 0.45);
            this.bodies.slide(player, target);
         } else {
            PowerService.playAnimation(player, "heavy_hand_yank");
            this.hit(player, level, target, 4.0F);
            Vec3 toward = AimUtil.horizontalDir(player.position(), target.position());
            Vec3 dest = player.position().add(toward.scale(1.6));
            target.setDeltaMovement(yankVelocity(target.position(), dest, target.onGround()));
            target.hurtMarked = true;
            target.fallDistance = 0.0F;
            HeavyHandFx.yank(level, player, target, was);
         }

         MasterySystem.award(player, MasteryReason.HIT, target);
      }
   }

   private void fireHold(ServerPlayer player, ServerLevel level, HeavyHandAbility.Hold hold) {
      if (this.pay(player, level, 18.0F, hold)) {
         HeavyHandFx.commit(level, player, hold.target, hold.shift);
         snap(player, hold.shift ? 3 : 2);
         if (hold.shift) {
            PowerService.playAnimation(player, "heavy_hand_launch");
            this.bodies.launch(player, level, hold.target);
            MasterySystem.award(player, MasteryReason.HIT, hold.target);
         } else {
            PowerService.playAnimation(player, "heavy_hand_crush");
            this.bodies.crush(player, level, hold.target);
         }
      }
   }

   private static boolean validTarget(ServerPlayer player, LivingEntity e) {
      return e != player
         && e.isAlive()
         && !e.isSpectator()
         && Harm.allowed(player, e)
         && !Foes.fightsFor(player, e)
         && (!(e instanceof Player p) || !p.getAbilities().invulnerable);
   }

   private boolean refuseAim(ServerPlayer player) {
      if (AimUtil.aimedLivingSoft(player, 64.0, 3.0, e -> validTarget(player, e)) != null) {
         return this.fail(player, "awakened.graviton.hand_too_far", new Object[]{24});
      } else {
         LivingEntity any = AimUtil.aimedLivingSoft(player, 24.0, 3.0, e -> e != player && e.isAlive() && !e.isSpectator());
         return any == null || !Foes.fightsFor(player, any) && Harm.allowed(player, any)
            ? this.fail(player, "awakened.graviton.no_target", new Object[0])
            : this.fail(player, "awakened.graviton.hand_ally", new Object[0]);
      }
   }

   private static boolean stillHeld(ServerPlayer player, ServerLevel level, LivingEntity target) {
      return target.isAlive() && !target.isRemoved() && target.level() == level && target.distanceToSqr(player) <= 900.0 && Harm.allowed(player, target);
   }

   private static void end(ServerPlayer player, HeavyHandAbility.Hold hold) {
      HeavyHandFx.holdEnd(player, hold.target);
      PowerService.stopAnimation(player);
   }

   private void lose(ServerPlayer player, ServerLevel level, HeavyHandAbility.Hold hold) {
      end(player, hold);
      snap(player, 4);
      HeavyHandFx.fizzle(level, player, hold.target);
      this.fail(player, "awakened.graviton.hand_lost", new Object[0]);
   }

   private boolean pay(ServerPlayer player, ServerLevel level, float base, HeavyHandAbility.Hold hold) {
      if (this.charge(player, base)) {
         return true;
      } else {
         this.resource(player).refuse(player);
         snap(player, 4);
         HeavyHandFx.fizzle(level, player, hold.target);
         return false;
      }
   }

   private static void show(ServerPlayer player, HeavyHandAbility.Hold hold, float f) {
      HeavyHandVisual.set(player, HeavyHandVisual.of(player).holding(hold.target.getId(), f, hold.shift));
   }

   private static void snap(ServerPlayer player, int kind) {
      HeavyHandVisual.set(player, HeavyHandVisual.of(player).snapped(kind));
   }

   private void hit(ServerPlayer player, ServerLevel level, LivingEntity target, float base) {
      float damage = MasteryScaling.scaledDamage(base, player, this.balanceKey());
      Dmg.hurtNoKnockback(target, level, Dmg.by(player, DamageTypes.PLAYER_ATTACK), damage);
   }

   private static void shudder(LivingEntity target, boolean launch, float f, ServerLevel level) {
      if (!(target instanceof Player)) {
         double j = 0.04 + 0.05 * (double)f;
         double jx = (level.random.nextDouble() - 0.5) * 2.0 * j;
         double jz = (level.random.nextDouble() - 0.5) * 2.0 * j;
         double vy = launch ? 0.02 + 0.03 * (double)f : -0.06 - 0.06 * (double)f;
         target.setDeltaMovement(jx, Math.min(target.getDeltaMovement().y, 0.0) * 0.3 + vy, jz);
         target.hurtMarked = true;
      }
   }

   static Vec3 yankVelocity(Vec3 from, Vec3 dest, boolean grounded) {
      double dx = dest.x - from.x;
      double dz = dest.z - from.z;
      double dy = dest.y - from.y;
      double horiz = Math.sqrt(dx * dx + dz * dz);
      double vy = Math.min(1.3, Math.max(0.3, 0.3 + 0.025 * horiz + Math.max(0.0, dy) * 0.12));
      if (horiz < 1.0E-4) {
         return new Vec3(0.0, vy, 0.0);
      } else {
         double y = 0.0;
         double v = vy;

         int airTicks;
         for (airTicks = 1; airTicks < 60; airTicks++) {
            y += v;
            v = (v - 0.08) * 0.98;
            if (v < 0.0 && y <= dy) {
               break;
            }
         }

         double drag = 0.91;
         double first = grounded ? 0.546 : drag;
         double travel = 1.0 + first * (1.0 - Math.pow(drag, (double)(airTicks - 1))) / (1.0 - drag);
         double vh = horiz / travel;
         return new Vec3(dx / horiz * vh, vy, dz / horiz * vh);
      }
   }

   private static final class Hold {
      final LivingEntity target;
      boolean shift;
      int ticks;

      Hold(LivingEntity target, boolean shift) {
         this.target = target;
         this.shift = shift;
      }
   }
}
