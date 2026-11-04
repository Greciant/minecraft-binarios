package com.awakenedmod.awakened.content.graviton;

import com.awakenedmod.awakened.core.Attrs;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

final class HeavyHandBodies {
   static final float CRUSH_DAMAGE = 9.0F;
   static final int PIN_TICKS = 30;
   private static final double DRAG_SPEED = 1.3;
   private static final int DRAG_MAX_TICKS = 40;
   private static final double AIRBORNE_GAP = 0.6;
   private static final int FLOOR_SCAN = 64;
   static final double LAUNCH_SPEED = 1.5;
   private static final int LAUNCH_MAX_TICKS = 140;
   private static final double SAFE_FALL = 3.0;
   private static final double FORCED_FALL_CAP = 1.6;
   private static final ResourceLocation PIN_SPEED_ID = AwId.of("graviton_heavy_hand_pin_speed");
   private static final ResourceLocation PIN_JUMP_ID = AwId.of("graviton_heavy_hand_pin_jump");
   private static final Map<UUID, Integer> PIN_COUNTS = new ConcurrentHashMap<>();
   private static final int SLIDE_TICKS = 8;
   private final Map<UUID, List<HeavyHandBodies.Body>> bodies = new ConcurrentHashMap<>();
   private final String balanceKey;

   HeavyHandBodies(String balanceKey) {
      this.balanceKey = balanceKey;
   }

   void crush(ServerPlayer caster, ServerLevel level, LivingEntity target) {
      target.stopRiding();
      HeavyHandBodies.Body body = new HeavyHandBodies.Body(target, HeavyHandBodies.Phase.DRAG);
      Fx.Floor floor = Fx.floorUnder(level, target.getX(), target.getY() + 0.1, target.getZ(), 64);
      body.floorY = floor == null ? Double.NaN : floor.topY();
      boolean airborne = !target.onGround() && !Double.isNaN(body.floorY) && target.getY() - body.floorY > 0.6;
      if (airborne) {
         HeavyHandBodyVisual.begin(target, 3, 40);
      }

      if (airborne || this.impact(caster, level, body)) {
         this.list(caster).add(body);
      }
   }

   void launch(ServerPlayer caster, ServerLevel level, LivingEntity target) {
      target.stopRiding();
      target.setDeltaMovement(0.0, 1.5, 0.0);
      target.hurtMarked = true;
      target.fallDistance = 0.0F;
      HeavyHandBodies.Body body = new HeavyHandBodies.Body(target, HeavyHandBodies.Phase.FLIGHT);
      this.list(caster).add(body);
      HeavyHandBodyVisual.begin(target, 2, 140);
      HeavyHandFx.launch(level, caster, target);
   }

   void slide(ServerPlayer caster, LivingEntity target) {
      this.list(caster).add(new HeavyHandBodies.Body(target, HeavyHandBodies.Phase.SLIDE));
      HeavyHandBodyVisual.begin(target, 4, 9);
   }

   private List<HeavyHandBodies.Body> list(ServerPlayer caster) {
      return this.bodies.computeIfAbsent(caster.getUUID(), id -> new ArrayList<>());
   }

   void tick(ServerPlayer caster, ServerLevel level) {
      List<HeavyHandBodies.Body> list = this.bodies.get(caster.getUUID());
      if (list != null) {
         Iterator<HeavyHandBodies.Body> it = list.iterator();

         while (it.hasNext()) {
            HeavyHandBodies.Body body = it.next();
            body.ticks++;

            boolean done = switch (body.phase) {
               case DRAG -> this.tickDrag(caster, level, body);
               case PIN -> this.tickPin(caster, level, body);
               case FLIGHT -> this.tickFlight(caster, level, body);
               case SLIDE -> tickSlide(level, body);
            };
            if (done) {
               finish(body);
               it.remove();
            }
         }

         if (list.isEmpty()) {
            this.bodies.remove(caster.getUUID());
         }
      }
   }

   void release(ServerPlayer caster) {
      List<HeavyHandBodies.Body> list = this.bodies.remove(caster.getUUID());
      if (list != null) {
         for (HeavyHandBodies.Body body : list) {
            finish(body);
            HeavyHandFx.released(body.target);
         }
      }
   }

   private static void finish(HeavyHandBodies.Body body) {
      unroot(body);
      HeavyHandBodyVisual.end(body.target, visualPhase(body.phase));
   }

   private static int visualPhase(HeavyHandBodies.Phase phase) {
      return switch (phase) {
         case DRAG -> 3;
         case PIN -> 1;
         case FLIGHT -> 2;
         case SLIDE -> 4;
      };
   }

   private static boolean gone(ServerLevel level, LivingEntity target) {
      return !target.isAlive() || target.isRemoved() || target.level() != level;
   }

   private boolean tickDrag(ServerPlayer caster, ServerLevel level, HeavyHandBodies.Body body) {
      LivingEntity t = body.target;
      if (gone(level, t)) {
         creditKill(caster, t);
         return true;
      } else {
         boolean down = t.onGround() || t.getY() - body.floorY <= 0.05;
         if (!down && body.ticks < 40) {
            stepDown(t, body.floorY, 1.3);
            t.fallDistance = 0.0F;
            HeavyHandFx.dragging(level, caster, t, body.ticks);
            return false;
         } else {
            return !this.impact(caster, level, body);
         }
      }
   }

   private boolean impact(ServerPlayer caster, ServerLevel level, HeavyHandBodies.Body body) {
      LivingEntity t = body.target;
      double floorY = Double.isNaN(body.floorY) ? t.getY() : Math.min(t.getY(), body.floorY + 0.05);
      if (!(t instanceof Player) && !Double.isNaN(body.floorY)) {
         t.setPos(t.getX(), Math.max(body.floorY, Math.min(t.getY(), body.floorY + 0.05)), t.getZ());
      }

      t.setDeltaMovement(0.0, -0.2, 0.0);
      t.hurtMarked = true;
      HeavyHandFx.crush(level, caster, t, floorY);
      float damage = MasteryScaling.scaledDamage(9.0F, caster, this.balanceKey);
      boolean hit = Dmg.hurtNoKnockback(t, level, Dmg.by(caster, DamageTypes.PLAYER_ATTACK), damage);
      if (hit) {
         MasterySystem.award(caster, MasteryReason.HIT, t);
      }

      if (gone(level, t)) {
         creditKill(caster, t);
         return false;
      } else {
         body.phase = HeavyHandBodies.Phase.PIN;
         body.ticks = 0;
         body.anchor = t.position();
         root(body);
         HeavyHandBodyVisual.begin(t, 1, 30);
         return true;
      }
   }

   private boolean tickPin(ServerPlayer caster, ServerLevel level, HeavyHandBodies.Body body) {
      LivingEntity t = body.target;
      if (gone(level, t)) {
         creditKill(caster, t);
         HeavyHandFx.pinLost(t);
         return true;
      } else if (body.ticks >= 30) {
         HeavyHandFx.unpinned(level, t);
         return true;
      } else {
         HeavyHandFx.pinned(level, t, body.ticks);
         Vec3 v = t.getDeltaMovement();
         if (t instanceof Player) {
            if (v.y > 0.0) {
               t.setDeltaMovement(v.x, 0.0, v.z);
               t.hurtMarked = true;
            }
         } else {
            t.setDeltaMovement(0.0, Math.min(0.0, v.y), 0.0);
            t.setPos(body.anchor.x, Math.min(t.getY(), body.anchor.y), body.anchor.z);
            if (t instanceof Mob mob) {
               mob.getNavigation().stop();
            }
         }

         return false;
      }
   }

   private boolean tickFlight(ServerPlayer caster, ServerLevel level, HeavyHandBodies.Body body) {
      LivingEntity t = body.target;
      if (!gone(level, t) && body.ticks < 140) {
         body.peakY = Math.max(body.peakY, t.getY());
         double vy = t.getDeltaMovement().y;
         if (!body.pastApex) {
            if (body.ticks <= 2 || !(vy <= 0.02) && !(t.getY() < body.peakY - 0.01)) {
               return false;
            }

            body.pastApex = true;
            HeavyHandFx.apex(level, t);
         }

         body.sinceApex++;
         boolean onFloor = t.onGround() || body.forced && t.getY() - body.floorY <= 0.05;
         if (body.sinceApex <= 2 || !onFloor && !t.isInWater() && !t.isInLava()) {
            if (!(t instanceof Player)) {
               double expected = Math.min(1.6, 0.08 * (double)body.sinceApex);
               if (body.forced || vy > -0.5 * expected && body.sinceApex > 3) {
                  body.forced = true;
                  if (Double.isNaN(body.floorY)) {
                     Fx.Floor floor = Fx.floorUnder(level, t.getX(), t.getY() + 0.1, t.getZ(), 64);
                     body.floorY = floor == null ? t.getY() - 64.0 : floor.topY();
                  }

                  stepDown(t, body.floorY, expected);
               }
            }

            return false;
         } else {
            if (body.forced && !t.isInWater()) {
               double drop = body.peakY - t.getY();
               float fall = (float)Math.ceil(drop - 3.0);
               if (fall > 0.0F) {
                  Dmg.hurt(t, level, Dmg.by(caster, DamageTypes.FALL), MasteryScaling.configScaledDamage(fall, this.balanceKey));
               }
            }

            if (!t.isInWater()) {
               HeavyHandFx.landed(level, t);
            }

            return true;
         }
      } else {
         return true;
      }
   }

   private static boolean tickSlide(ServerLevel level, HeavyHandBodies.Body body) {
      LivingEntity t = body.target;
      if (!gone(level, t) && body.ticks <= 8) {
         HeavyHandFx.sliding(level, t, body.ticks);
         return false;
      } else {
         return true;
      }
   }

   private static void stepDown(LivingEntity t, double floorY, double speed) {
      if (t instanceof Player) {
         t.setDeltaMovement(0.0, -speed, 0.0);
         t.hurtMarked = true;
      } else {
         double y = Double.isNaN(floorY) ? t.getY() - speed : Math.max(floorY, t.getY() - speed);
         t.setDeltaMovement(0.0, 0.0, 0.0);
         t.setPos(t.getX(), y, t.getZ());
      }
   }

   private static void creditKill(ServerPlayer caster, LivingEntity t) {
      if (Foes.defeatedBy(t, caster)) {
         QuestSystem.feat(caster, "graviton_crush");
      }
   }

   private static void root(HeavyHandBodies.Body body) {
      if (!body.rooted) {
         body.rooted = true;
         LivingEntity t = body.target;
         PIN_COUNTS.merge(t.getUUID(), 1, Integer::sum);
         Attrs.set(t, Attributes.MOVEMENT_SPEED, PIN_SPEED_ID, -1.0, Operation.ADD_MULTIPLIED_TOTAL);
         Attrs.set(t, Attributes.JUMP_STRENGTH, PIN_JUMP_ID, -1.0, Operation.ADD_MULTIPLIED_TOTAL);
      }
   }

   private static void unroot(HeavyHandBodies.Body body) {
      if (body.rooted) {
         body.rooted = false;
         LivingEntity t = body.target;
         Integer left = PIN_COUNTS.computeIfPresent(t.getUUID(), (id, n) -> n > 1 ? n - 1 : null);
         if (left == null) {
            Attrs.set(t, Attributes.MOVEMENT_SPEED, PIN_SPEED_ID, 0.0, Operation.ADD_MULTIPLIED_TOTAL);
            Attrs.set(t, Attributes.JUMP_STRENGTH, PIN_JUMP_ID, 0.0, Operation.ADD_MULTIPLIED_TOTAL);
         }
      }
   }

   static void forgetAll() {
      PIN_COUNTS.clear();
   }

   private static final class Body {
      final LivingEntity target;
      HeavyHandBodies.Phase phase;
      int ticks;
      double floorY;
      Vec3 anchor;
      double peakY;
      boolean pastApex;
      int sinceApex;
      boolean forced;
      boolean rooted;

      Body(LivingEntity target, HeavyHandBodies.Phase phase) {
         this.target = target;
         this.phase = phase;
         this.peakY = target.getY();
         this.floorY = Double.NaN;
      }
   }

   private static enum Phase {
      DRAG,
      PIN,
      FLIGHT,
      SLIDE;
   }
}
