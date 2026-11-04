package com.awakenedmod.awakened.content.hydro;

import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.Lab;
import com.awakenedmod.awakened.core.LoopSound;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.Terrain;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import com.awakenedmod.awakened.core.stamina.ChargeBill;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class TsunamiAbility extends Ability {
   public static final int MAX_CHARGE_TICKS = 90;
   private static final Lab.Value CHARGE_TICKS = Lab.SERVER.def("hydro.tsunami.charge_ticks", 90.0, 20.0, 200.0, 1.0);
   private static final Lab.Value WALL_HEIGHT = Lab.SERVER.def("hydro.tsunami.wall_height", 0.6, 0.1, 1.2, 0.01);
   private static final Lab.Value SPRAY = Lab.SERVER.def("hydro.tsunami.spray", 1.0, 0.0, 3.0, 0.05);
   public static final double SPEED_START = 0.7;
   public static final double SPEED_TOP = 1.25;
   public static final double RAMP_TICKS = 12.0;
   public static final double HIT_BEHIND = 2.5;
   public static final double HIT_AHEAD = 0.45;
   public static final String POUR_CLIP = "hydro_tsunami_pour";
   private static final float WATER_MIN = 3.0F;
   private static final float WATER_MAX = 12.0F;
   private static final float STAMINA_MIN = 30.0F;
   private static final float STAMINA_MAX = 70.0F;
   private static final float DAMAGE_MIN = 14.0F;
   private static final float DAMAGE_MAX = 35.0F;
   private static final float REACH_MIN = 20.0F;
   private static final float REACH_MAX = 56.0F;
   private static final float HALF_WIDTH_MIN = 4.0F;
   private static final float HALF_WIDTH_MAX = 14.0F;
   private static final float HEIGHT_MIN = 4.0F;
   private static final float HEIGHT_MAX = 14.0F;
   private static final float KNOCK_MIN = 0.8F;
   private static final float KNOCK_MAX = 2.0F;
   private static final float GROWTH_CAP = 1.3F;
   public static final float WET_SIZE = 1.15F;
   private static final double CARRY = 0.85;
   private static final String POUR_SOUND = "hydro_pour";
   private final Map<UUID, TsunamiAbility.Pour> charging = this.activeState();
   private final Map<UUID, List<TsunamiAbility.Wave>> waves = this.playerState();
   private int waveSerial;

   public TsunamiAbility() {
      super("tsunami", 70.0F, 9);
   }

   public static int chargeTicks() {
      return Math.max(1, CHARGE_TICKS.getI());
   }

   public static float chargeAt(float age) {
      return Mth.clamp(age / (float)chargeTicks(), 0.0F, 1.0F);
   }

   public static float wallHeightFraction() {
      return WALL_HEIGHT.getF();
   }

   public static float spray() {
      return SPRAY.getF();
   }

   public static float reach(float f) {
      return Mth.lerp(f, 20.0F, 56.0F);
   }

   public static float halfWidth(float f) {
      return Mth.lerp(f, 4.0F, 14.0F);
   }

   public static float height(float f) {
      return Mth.lerp(f, 4.0F, 14.0F);
   }

   public static float sizeMul(float abilityPower, boolean wet) {
      return Math.min(1.3F, 1.0F + Math.max(0.0F, abilityPower)) * (wet ? 1.15F : 1.0F);
   }

   public static float frontAt(float age, float reach) {
      double t = (double)Math.max(0.0F, age);
      double ramp = Math.min(t, 12.0);
      double d = 0.7 * ramp + 0.55 * ramp * ramp / 24.0 + 1.25 * Math.max(0.0, t - 12.0);
      return (float)Math.min((double)reach, d);
   }

   public static float frontAt(float age, float f, float sizeMul) {
      return frontAt(age, reach(f) * sizeMul);
   }

   public static float travelTicks(float reach) {
      double rampDist = 11.7;
      if ((double)reach > rampDist) {
         return (float)(12.0 + ((double)reach - rampDist) / 1.25);
      } else {
         double a = 0.02291666666666667;
         return (float)((-0.7 + Math.sqrt(0.48999999999999994 + 4.0 * a * (double)reach)) / (2.0 * a));
      }
   }

   public static int floorUp(float height) {
      return 3 + (int)(height / 4.0F);
   }

   public static int floorDown(float height) {
      return 5 + (int)(height / 3.0F);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().boostsDamage().boostsRange().build();
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public float staminaCost(ServerPlayer player) {
      return 0.0F;
   }

   @Override
   public String costLabel() {
      return (int)this.billed(30.0F) + "-" + (int)this.billed(70.0F);
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (this.charging.containsKey(player.getUUID())) {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      } else if (!this.charge(player, 30.0F)) {
         HydroFeedback.tired(player);
         return false;
      } else {
         this.charging.put(player.getUUID(), new TsunamiAbility.Pour(ChargeBill.opened(this.billed(30.0F), this.billed(70.0F))));
         PowerService.playAnimation(player, "hydro_tsunami_pour", true);
         HydroVisual.startPour(player);
         Hydro.show(player, Hydro.peek(player), player.position().add(horizontal(player).scale(3.0)).add(0.0, 1.0, 0.0));
         Fx.sound(player.serverLevel(), player.position(), SoundEvents.CONDUIT_AMBIENT, SoundSource.PLAYERS, 0.8F, 0.6F);
         return true;
      }
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }

   @Override
   public void serverTick(ServerPlayer player) {
      this.tickCharge(player);
      this.tickWave(player);
   }

   private void tickCharge(ServerPlayer player) {
      TsunamiAbility.Pour pour = this.charging.get(player.getUUID());
      if (pour != null) {
         int full = chargeTicks();
         long age = HydroVisual.of(player).pourAge(player.serverLevel().getGameTime());
         pour.ticks = age < 0L ? Math.min(full, pour.ticks + 1) : (int)Math.min((long)full, age);
         float f = chargeAt((float)pour.ticks);
         pour.bill.payTo(player, this.resource(player), f);
         TsunamiFx.pourTick(player, horizontal(player), f, pour.ticks);
         LoopSound.keep(player, "hydro_pour", SoundEvents.WEATHER_RAIN, SoundSource.PLAYERS, Mth.lerp(f, 0.25F, 1.3F), Mth.lerp(f, 0.45F, 0.9F));
         if (pour.ticks >= full) {
            this.charging.remove(player.getUUID());
            this.fire(player, pour, 1.0F);
         }
      }
   }

   private void tickWave(ServerPlayer player) {
      List<TsunamiAbility.Wave> list = this.waves.get(player.getUUID());
      if (list != null) {
         list.removeIf(wave -> {
            if (wave.advance(player, player.serverLevel())) {
               return false;
            } else {
               wave.end();
               return true;
            }
         });
         if (list.isEmpty()) {
            this.waves.remove(player.getUUID());
         }
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      TsunamiAbility.Pour pour = this.charging.remove(player.getUUID());
      if (pour != null) {
         this.fire(player, pour, chargeAt((float)pour.ticks));
      }
   }

   private void fire(ServerPlayer player, TsunamiAbility.Pour pour, float f) {
      PowerService.stopHeldAnimation(player, "hydro_tsunami_pour");
      HydroVisual.endPour(player);
      LoopSound.stop(player, "hydro_pour");
      f = Math.min(f, pour.bill.paidCharge());
      PowerService.playAnimation(player, "hydro_tsunami_push");
      Hydro.Cast cast = Hydro.take(player, Mth.lerp(f, 3.0F, 12.0F));
      float grow = sizeMul(MasteryScaling.abilityPower(player), cast.wet());
      Vec3 origin = player.position();
      float damage = Hydro.damage(player, this, Mth.lerp(f, 14.0F, 35.0F), cast);
      Vec3 fwd = horizontal(player);
      TsunamiAbility.Wave wave = new TsunamiAbility.Wave(
         player.serverLevel(),
         this.soundKey(player),
         origin,
         fwd,
         (double)(reach(f) * grow),
         (double)(halfWidth(f) * grow),
         (double)(height(f) * grow),
         damage,
         (double)Mth.lerp(f, 0.8F, 2.0F),
         cast.wet()
      );
      this.waves.computeIfAbsent(player.getUUID(), k -> new ArrayList<>()).add(wave);
      TsunamiFx.release(player.serverLevel(), origin, fwd, f, grow, cast.wet());
   }

   private String soundKey(ServerPlayer player) {
      return "hydro_tsunami_" + player.getUUID() + "_" + this.waveSerial++;
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      TsunamiAbility.Pour pour = this.charging.remove(player.getUUID());
      if (pour != null) {
         PowerService.stopHeldAnimation(player, "hydro_tsunami_pour");
         pour.bill.refund(player, this.resource(player));
      }

      HydroVisual.endPour(player);
      LoopSound.stop(player, "hydro_pour");
      List<TsunamiAbility.Wave> list = this.waves.get(player.getUUID());
      if (list != null) {
         list.forEach(TsunamiAbility.Wave::silence);
      }

      super.clearPlayer(player);
   }

   private static Vec3 horizontal(ServerPlayer player) {
      Vec3 look = player.getLookAngle();
      Vec3 flat = new Vec3(look.x, 0.0, look.z);
      return flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
   }

   private static final class Pour {
      private final ChargeBill bill;
      private int ticks;

      private Pour(ChargeBill bill) {
         this.bill = bill;
      }
   }

   private static final class Wave {
      private final ServerLevel level;
      private final String soundKey;
      private final Vec3 origin;
      private final Vec3 fwd;
      private final Vec3 side;
      private final double maxDist;
      private final double halfWidth;
      private final double height;
      private final float damage;
      private final double knock;
      private final boolean wet;
      private final int travel;
      private final int floorUp;
      private final int floorDown;
      private int ticks;
      private final Set<Integer> hit = new HashSet<>();
      private final List<LivingEntity> carried = new ArrayList<>();
      private int finished;
      private boolean routPaid;
      private double front;
      private double floorY;

      private Wave(
         ServerLevel level, String soundKey, Vec3 origin, Vec3 fwd, double maxDist, double halfWidth, double height, float damage, double knock, boolean wet
      ) {
         this.level = level;
         this.soundKey = soundKey;
         this.origin = origin;
         this.fwd = fwd;
         this.side = new Vec3(-fwd.z, 0.0, fwd.x);
         this.maxDist = maxDist;
         this.halfWidth = halfWidth;
         this.height = height;
         this.damage = damage;
         this.knock = knock;
         this.wet = wet;
         this.travel = Mth.ceil(TsunamiAbility.travelTicks((float)maxDist));
         this.floorUp = TsunamiAbility.floorUp((float)height);
         this.floorDown = TsunamiAbility.floorDown((float)height);
         this.floorY = origin.y;
      }

      private boolean advance(ServerPlayer player, ServerLevel level) {
         this.ticks++;
         double last = this.front;
         this.front = (double)TsunamiAbility.frontAt((float)this.ticks, (float)this.maxDist);
         double speed = this.front - last;
         Vec3 head = this.origin.add(this.fwd.scale(this.front));
         double y = Terrain.floorNear(level, head.x, head.z, this.floorY, this.floorUp, this.floorDown, true);
         if (!Double.isNaN(y)) {
            this.floorY = y;
         }

         double ahead = 0.45 * this.height;
         Vec3 tail = this.origin.add(this.fwd.scale(this.front - 2.5));
         Vec3 lip = this.origin.add(this.fwd.scale(this.front + ahead));
         AABB band = new AABB(tail.x, this.floorY, tail.z, lip.x, this.floorY, lip.z).inflate(this.halfWidth + 1.0, this.height + 0.5, this.halfWidth + 1.0);
         boolean struck = false;

         for (LivingEntity e : level.getEntitiesOfClass(
            LivingEntity.class, band, c -> c != player && c.isAlive() && !this.hit.contains(c.getId()) && Harm.allowed(player, c)
         )) {
            Vec3 rel = e.position().subtract(this.origin);
            double along = rel.x * this.fwd.x + rel.z * this.fwd.z;
            if (!(along < this.front - 2.5) && !(along > this.front + ahead)) {
               double off = Math.abs(rel.x * this.side.x + rel.z * this.side.z);
               if (!(off > this.halfWidth) && !(Math.abs(e.getY() - this.floorY) > this.height + 0.5)) {
                  Hydro.hit(player, e, this.damage);
                  MasterySystem.award(player, MasteryReason.HIT, e);
                  struck = true;
                  if (Foes.defeatedBy(e, player)) {
                     this.finished++;
                  }

                  e.knockback(this.knock, -this.fwd.x, -this.fwd.z);
                  e.setDeltaMovement(e.getDeltaMovement().add(0.0, 0.4, 0.0));
                  e.hurtMarked = true;
                  this.hit.add(e.getId());
                  this.carried.add(e);
               }
            }
         }

         if (struck) {
            Fx.soundOnlyFor(player, player.getEyePosition(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.9F, this.wet ? 0.9F : 1.0F);
         }

         this.carry(player, speed);
         if (!this.routPaid && this.finished >= 3) {
            QuestSystem.feat(player, "hydro_wave_rout");
            this.routPaid = true;
         }

         TsunamiFx.wake(level, head, this.fwd, this.side, this.halfWidth, this.height, this.floorY, this.ticks, this.wet);
         LoopSound.keepAt(level, this.soundKey, head, SoundEvents.WEATHER_RAIN, SoundSource.PLAYERS, (float)(0.8 + this.height * 0.06), 0.45F);
         return this.ticks < this.travel;
      }

      private void carry(ServerPlayer player, double speed) {
         this.carried.removeIf(e -> {
            if (e.isAlive() && e.level() == this.level && Harm.allowed(player, e)) {
               Vec3 rel = e.position().subtract(this.origin);
               double along = rel.x * this.fwd.x + rel.z * this.fwd.z;
               if (along < this.front - 3.0) {
                  return true;
               } else if (along > this.front + 1.0) {
                  return false;
               } else {
                  Vec3 v = e.getDeltaMovement();
                  e.setDeltaMovement(this.fwd.x * speed * 0.85, Math.max(v.y, 0.15), this.fwd.z * speed * 0.85);
                  e.hurtMarked = true;
                  return false;
               }
            } else {
               return true;
            }
         });
      }

      private void end() {
         TsunamiFx.crash(this.level, this.origin.add(this.fwd.scale(this.front)), this.fwd, this.halfWidth, this.height, this.floorY, this.wet);
         this.silence();
      }

      private void silence() {
         LoopSound.stopAt(this.level, this.soundKey);
      }
   }
}
