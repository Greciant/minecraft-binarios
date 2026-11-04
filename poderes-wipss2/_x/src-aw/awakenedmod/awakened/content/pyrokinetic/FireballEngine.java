package com.awakenedmod.awakened.content.pyrokinetic;

import com.awakenedmod.awakened.core.caster.Caster;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

public final class FireballEngine {
   public static final String BALANCE_KEY = FireballAbility.class.getSimpleName();
   public static final float COST = 6.0F;
   public static final float DAMAGE = 5.0F;
   private static final int IGNITE_SECONDS = 3;
   private static final double BURST_RADIUS = 1.5;
   private static final double BURST_RADIUS_DIRECT = 1.8;
   public static final float VELOCITY = 1.6F;
   public static final double GRAVITY = 0.017;
   private static final int MAX_FLIGHT_TICKS = 200;
   private static final PyroFireball.Shot SHOT = new PyroFireball.Shot(5.0F, 3, 1.5, 1.8, 1.6F, 0.017, 0.0F, 200);
   public static final int RELEASE_DELAY_TICKS = 3;
   private static final double AIM_RANGE = 64.0;

   private FireballEngine() {
   }

   public static FireballEngine.Throw windUp(Caster caster, boolean offHand) {
      FlameTone style = FlameTone.of(caster);
      float damage = caster.scaledDamage(5.0F, BALANCE_KEY, PyroHeatState.heatDamageBonus(caster));
      FireballEngine.Throw wound = new FireballEngine.Throw(caster.level().getGameTime() + 3L, offHand, style, damage);
      PyroHeatState.gainFireball(caster);
      return wound;
   }

   public static boolean isDue(Caster caster, FireballEngine.Throw wound) {
      return caster.level().getGameTime() >= wound.releaseAt();
   }

   public static void release(Caster caster, FireballEngine.Throw wound) {
      ServerLevel level = caster.level();
      FlameTone style = wound.style();
      boolean offHand = wound.offHand();
      boolean rightArm = caster.rightArm(offHand);
      Vec3 origin = caster.hand(offHand, (double)caster.pitch(), 0.34, 0.46, 0.6);
      Vec3 dir = caster.throwDirection(origin, 64.0);
      PyroFireball.launch(caster, SHOT.withDamage(wound.damage()), origin, dir, rightArm, style);
      PyroFlames.impactFlare(level, origin, style);
      PyroFlames.sound(caster, origin, SoundEvents.FIRECHARGE_USE, 0.9F, 1.1F, style);
   }

   public static record Throw(long releaseAt, boolean offHand, FlameTone style, float damage) {
   }
}
