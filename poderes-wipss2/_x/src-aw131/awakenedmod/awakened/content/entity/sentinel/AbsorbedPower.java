package com.awakenedmod.awakened.content.entity.sentinel;

import com.awakenedmod.awakened.content.entity.ThreatMoves;
import com.awakenedmod.awakened.core.Fx;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public enum AbsorbedPower {
   HYDROKINETIC(
      "hydrokinetic",
      "water",
      -13726209,
      () -> List.of(
            proj(
               (byte)6,
               16,
               18,
               8.0,
               30.0,
               12,
               Items.PRISMARINE_SHARD,
               1,
               9.0F,
               3.0F,
               2.0F,
               0.3F,
               2.4F,
               0.0,
               dust(-13726209),
               0,
               4,
               0.0,
               SoundEvents.PLAYER_ATTACK_SWEEP
            ),
            push((byte)9, 20, 22, 0.0, 10.0, 9, 9.0, 12.0F, 1.8, 0.5, ParticleTypes.SPLASH, 3, 0, SoundEvents.AXOLOTL_SPLASH),
            pull((byte)11, 18, 18, 6.0, 20.0, 8, 14.0, 9.0F, 1.1, true, dust(-13726209))
         )
   ),
   VELOCITY(
      "velocity",
      "velocity",
      -1560004,
      () -> List.of(
            proj(
               (byte)6,
               14,
               16,
               8.0,
               32.0,
               12,
               Items.REDSTONE,
               1,
               11.0F,
               4.0F,
               2.5F,
               0.7F,
               3.0F,
               0.0,
               dust(-1560004),
               0,
               0,
               0.0,
               SoundEvents.LIGHTNING_BOLT_IMPACT
            ),
            domain((byte)10, 22, 24, 0.0, 20.0, 9, 12.0, 6.0F, dust(-1560004)),
            pounce((byte)7, 10, 14, 6.0, 26.0, 10, 9.0F, dust(-1560004))
         )
   ),
   POCKET(
      "pocket",
      "pocket",
      -7712769,
      () -> List.of(
            pull((byte)11, 18, 18, 6.0, 24.0, 11, 16.0, 12.0F, 0.9, true, dust(-7712769)), domain((byte)10, 24, 26, 0.0, 18.0, 8, 10.0, 8.0F, dust(-7712769))
         )
   ),
   NUCLEAR(
      "nuclear",
      "nuclear",
      -6561750,
      () -> List.of(
            proj(
               (byte)6,
               18,
               18,
               10.0,
               34.0,
               12,
               Items.FIRE_CHARGE,
               1,
               14.0F,
               7.0F,
               3.0F,
               0.6F,
               2.8F,
               1.2,
               dust(-24546),
               2,
               0,
               1.2,
               (SoundEvent)SoundEvents.GENERIC_EXPLODE.value()
            ),
            proj(
               (byte)7,
               14,
               16,
               6.0,
               24.0,
               10,
               Items.FIRE_CHARGE,
               5,
               5.0F,
               3.0F,
               2.0F,
               0.4F,
               2.0F,
               12.0,
               dust(-6561750),
               1,
               0,
               0.0,
               SoundEvents.FIREWORK_ROCKET_BLAST
            ),
            erupt((byte)8, 28, 24, 10.0, 30.0, 7, 5.0, 18.0F, 0.9, dust(-6561750))
         )
   ),
   GRAVITON(
      "graviton",
      "graviton",
      -9807155,
      () -> List.of(
            pull((byte)11, 18, 16, 6.0, 26.0, 11, 18.0, 8.0F, 1.0, true, dust(-9807155)),
            push((byte)9, 16, 18, 0.0, 12.0, 9, 8.0, 7.0F, 2.2, 0.6, dust(-9807155), 0, 0, SoundEvents.WARDEN_SONIC_BOOM)
         )
   ),
   CHRONOKINETIC(
      "chronokinetic",
      "chrono",
      -1653942,
      () -> List.of(
            pull((byte)11, 16, 16, 6.0, 22.0, 10, 12.0, 11.0F, 0.2, true, dust(-1653942)), domain((byte)10, 22, 24, 0.0, 16.0, 8, 9.0, 7.0F, dust(-1653942))
         )
   ),
   CRYOKINETIC(
      "cryokinetic",
      "cryo",
      -10034945,
      () -> List.of(
            proj(
               (byte)6,
               14,
               14,
               8.0,
               30.0,
               12,
               Items.PRISMARINE_CRYSTALS,
               3,
               5.0F,
               1.0F,
               2.0F,
               0.2F,
               2.4F,
               9.0,
               dust(-10034945),
               0,
               4,
               0.0,
               SoundEvents.GLASS_BREAK
            ),
            push((byte)9, 16, 18, 0.0, 10.0, 9, 7.0, 6.0F, 1.4, 0.4, dust(-10034945), 4, 0, SoundEvents.GLASS_BREAK)
         )
   ),
   PYROKINETIC(
      "pyrokinetic",
      "pyro",
      -34261,
      () -> List.of(
            proj((byte)6, 18, 18, 8.0, 30.0, 12, Items.FIRE_CHARGE, 1, 10.0F, 5.0F, 3.0F, 0.4F, 2.4F, 1.0, dust(-34261), 4, 0, 1.0, SoundEvents.BLAZE_SHOOT),
            beam((byte)10, 20, 22, 6.0, 24.0, 9, 20.0, 14.0, 6.0F, dust(-34261)),
            push((byte)12, 22, 20, 0.0, 10.0, 8, 10.0, 9.0F, 1.6, 0.5, ParticleTypes.FLAME, 0, 4, SoundEvents.FIRECHARGE_USE)
         )
   ),
   STORM_CALLER(
      "storm_caller",
      "storm",
      -7359233,
      () -> List.of(
            proj(
               (byte)6,
               14,
               14,
               8.0,
               34.0,
               12,
               Items.LIGHTNING_ROD,
               1,
               10.0F,
               3.0F,
               2.0F,
               0.3F,
               3.2F,
               0.0,
               dust(-7359233),
               0,
               0,
               0.0,
               SoundEvents.LIGHTNING_BOLT_THUNDER
            ),
            beam((byte)10, 20, 22, 6.0, 28.0, 9, 26.0, 18.0, 7.0F, dust(-1)),
            erupt((byte)8, 22, 20, 8.0, 30.0, 8, 3.5, 11.0F, 0.5, dust(-7359233))
         )
   ),
   REGENERATOR(
      "regenerator",
      "regen",
      -1750693,
      () -> List.of(slam((byte)12, 12, 14, 0.0, 6.0, 11, 11.0F, dust(-1750693)), pounce((byte)6, 12, 16, 6.0, 20.0, 9, 10.0F, dust(-1750693)))
   ),
   TELEPORTER(
      "teleporter",
      "teleport",
      -5211393,
      () -> List.of(pounce((byte)6, 10, 14, 6.0, 24.0, 11, 9.0F, dust(-5211393)), slam((byte)7, 12, 14, 0.0, 8.0, 9, 7.0F, dust(-5211393)))
   );

   private final String powerId;
   private final String shortKey;
   private final int glowColor;
   private List<SentinelMove> cachedMoves;
   private final Supplier<List<SentinelMove>> moveFactory;
   private static final Map<String, AbsorbedPower> BY_ID;

   private AbsorbedPower(String powerId, String shortKey, int glowColor, Supplier<List<SentinelMove>> moveFactory) {
      this.powerId = powerId;
      this.shortKey = shortKey;
      this.glowColor = glowColor;
      this.moveFactory = moveFactory;
   }

   public String powerId() {
      return this.powerId;
   }

   public String shortKey() {
      return this.shortKey;
   }

   public int glowColor() {
      return this.glowColor;
   }

   public String iconAbilityId() {
      return switch (this) {
         case HYDROKINETIC -> "water_lance";
         case VELOCITY -> "static_bolt";
         case POCKET -> "rift";
         case NUCLEAR -> "cluster_burst";
         case GRAVITON -> "heavy_hand";
         case CHRONOKINETIC -> "flicker";
         case CRYOKINETIC -> "frost_nova";
         case PYROKINETIC -> "fireball";
         case STORM_CALLER -> "lightning_strike";
         case REGENERATOR -> "claw_strike";
         case TELEPORTER -> "blink";
      };
   }

   public int bit() {
      return 1 << this.ordinal();
   }

   public List<SentinelMove> moves() {
      if (this.cachedMoves == null) {
         this.cachedMoves = this.moveFactory.get();
      }

      return this.cachedMoves;
   }

   public static AbsorbedPower byPowerId(String id) {
      return id == null ? null : BY_ID.get(id);
   }

   public static AbsorbedPower byBitOrdinal(int ordinal) {
      AbsorbedPower[] v = values();
      return ordinal >= 0 && ordinal < v.length ? v[ordinal] : null;
   }

   private static DustParticleOptions dust(int rgb) {
      return Fx.dust(rgb & 16777215, 1.3F);
   }

   private static SentinelMove proj(
      final byte action,
      final int windup,
      final int recover,
      final double min,
      final double max,
      final int weight,
      final Item item,
      final int count,
      final float impact,
      final float aoe,
      final float radius,
      final float kb,
      final float speed,
      double aura,
      final ParticleOptions flash,
      final int ignite,
      final int slow,
      final double flameAura,
      final SoundEvent fireSound
   ) {
      return new SentinelMove() {
         @Override
         public byte action() {
            return action;
         }

         @Override
         public int windup() {
            return windup;
         }

         @Override
         public int recover() {
            return recover;
         }

         @Override
         public double minRange() {
            return min;
         }

         @Override
         public double maxRange() {
            return max;
         }

         @Override
         public int weight() {
            return weight;
         }

         public void windupTick(SentinelEntity self, ServerLevel level, LivingEntity target, int tick) {
            Vec3 m = SentinelMoves.muzzle(self, target, 0.66, 1.2);
            Fx.burst(level, flash, m, 3, 0.15, 0.01);
         }

         public void strike(SentinelEntity self, ServerLevel level, LivingEntity target) {
            SentinelMoves.fireBolts(
               self,
               level,
               target,
               new ItemStack(item),
               count,
               self.scaleDamage(impact),
               aoe,
               radius,
               kb,
               speed,
               0.66,
               count > 1 ? 9.0 : 0.0,
               flash,
               ignite,
               slow,
               flameAura
            );
            Fx.sound(level, self.position(), fireSound, SoundSource.HOSTILE, 1.4F, 0.8F);
         }
      };
   }

   private static SentinelMove push(
      final byte action,
      final int windup,
      final int recover,
      final double min,
      final double max,
      final int weight,
      final double radius,
      final float damage,
      final double horiz,
      final double vert,
      final ParticleOptions fx,
      final int slowSeconds,
      final int igniteSeconds,
      final SoundEvent sound
   ) {
      return new SentinelMove() {
         @Override
         public byte action() {
            return action;
         }

         @Override
         public int windup() {
            return windup;
         }

         @Override
         public int recover() {
            return recover;
         }

         @Override
         public double minRange() {
            return min;
         }

         @Override
         public double maxRange() {
            return max;
         }

         @Override
         public int weight() {
            return weight;
         }

         public void strike(SentinelEntity self, ServerLevel level, LivingEntity target) {
            SentinelMoves.radialBurst(self, level, radius, self.scaleDamage(damage), horiz, vert, fx, slowSeconds, igniteSeconds);
            Fx.sound(level, self.position(), sound, SoundSource.HOSTILE, 1.4F, 0.7F);
         }
      };
   }

   private static SentinelMove pull(
      final byte action,
      final int windup,
      final int recover,
      final double min,
      final double max,
      final int weight,
      final double radius,
      final float damage,
      final double strength,
      final boolean slow,
      final ParticleOptions fx
   ) {
      return new SentinelMove() {
         @Override
         public byte action() {
            return action;
         }

         @Override
         public int windup() {
            return windup;
         }

         @Override
         public int recover() {
            return recover;
         }

         @Override
         public double minRange() {
            return min;
         }

         @Override
         public double maxRange() {
            return max;
         }

         @Override
         public int weight() {
            return weight;
         }

         public void strike(SentinelEntity self, ServerLevel level, LivingEntity target) {
            SentinelMoves.pull(self, level, radius, self.scaleDamage(damage), strength, slow, fx);
         }
      };
   }

   private static SentinelMove erupt(
      final byte action,
      final int windup,
      final int recover,
      final double min,
      final double max,
      final int weight,
      final double radius,
      final float damage,
      final double up,
      final ParticleOptions fx
   ) {
      return new SentinelMove() {
         @Override
         public byte action() {
            return action;
         }

         @Override
         public int windup() {
            return windup;
         }

         @Override
         public int recover() {
            return recover;
         }

         @Override
         public double minRange() {
            return min;
         }

         @Override
         public double maxRange() {
            return max;
         }

         @Override
         public int weight() {
            return weight;
         }

         public void windupTick(SentinelEntity self, ServerLevel level, LivingEntity target, int tick) {
            Fx.burst(level, fx, target.position().add(0.0, 0.1, 0.0), 6, 0.4, 0.01);
         }

         public void strike(SentinelEntity self, ServerLevel level, LivingEntity target) {
            ThreatMoves.groundErupt(level, self, target.position(), radius, self.scaleDamage(damage), up);
         }
      };
   }

   private static SentinelMove domain(
      final byte action,
      final int windup,
      final int recover,
      final double min,
      final double max,
      final int weight,
      final double radius,
      final float damage,
      final ParticleOptions fx
   ) {
      return new SentinelMove() {
         @Override
         public byte action() {
            return action;
         }

         @Override
         public int windup() {
            return windup;
         }

         @Override
         public int recover() {
            return recover;
         }

         @Override
         public double minRange() {
            return min;
         }

         @Override
         public double maxRange() {
            return max;
         }

         @Override
         public int weight() {
            return weight;
         }

         public void strike(SentinelEntity self, ServerLevel level, LivingEntity target) {
            SentinelMoves.pull(self, level, radius, self.scaleDamage(damage), 0.05, true, fx);
            Fx.disk(level, fx, self.position().add(0.0, 0.1, 0.0), radius);
            Fx.sound(level, self.position(), (SoundEvent)SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.HOSTILE, 1.5F, 0.6F);
         }
      };
   }

   private static SentinelMove beam(
      final byte action,
      final int windup,
      final int recover,
      final double min,
      final double max,
      final int weight,
      final double range,
      final double halfAngle,
      final float damage,
      final ParticleOptions fx
   ) {
      return new SentinelMove() {
         @Override
         public byte action() {
            return action;
         }

         @Override
         public int windup() {
            return windup;
         }

         @Override
         public int recover() {
            return recover;
         }

         @Override
         public double minRange() {
            return min;
         }

         @Override
         public double maxRange() {
            return max;
         }

         @Override
         public int weight() {
            return weight;
         }

         public void strike(SentinelEntity self, ServerLevel level, LivingEntity target) {
            SentinelMoves.coneBeam(self, level, target, range, halfAngle, self.scaleDamage(damage), fx);
            Fx.sound(level, self.position(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.4F, 1.4F);
         }
      };
   }

   private static SentinelMove slam(
      final byte action,
      final int windup,
      final int recover,
      final double min,
      final double max,
      final int weight,
      final float damage,
      final ParticleOptions fx
   ) {
      return new SentinelMove() {
         @Override
         public byte action() {
            return action;
         }

         @Override
         public int windup() {
            return windup;
         }

         @Override
         public int recover() {
            return recover;
         }

         @Override
         public double minRange() {
            return min;
         }

         @Override
         public double maxRange() {
            return max;
         }

         @Override
         public int weight() {
            return weight;
         }

         public void strike(SentinelEntity self, ServerLevel level, LivingEntity target) {
            SentinelMoves.frontSlam(self, level, self.scaleDamage(damage), fx);
         }
      };
   }

   private static SentinelMove pounce(
      final byte action,
      final int windup,
      final int recover,
      final double min,
      final double max,
      final int weight,
      final float damage,
      final ParticleOptions fx
   ) {
      return new SentinelMove() {
         @Override
         public byte action() {
            return action;
         }

         @Override
         public int windup() {
            return windup;
         }

         @Override
         public int recover() {
            return recover;
         }

         @Override
         public double minRange() {
            return min;
         }

         @Override
         public double maxRange() {
            return max;
         }

         @Override
         public int weight() {
            return weight;
         }

         public void strike(SentinelEntity self, ServerLevel level, LivingEntity target) {
            ThreatMoves.blinkNear(self, target, 3.0, fx);
            SentinelMoves.frontSlam(self, level, self.scaleDamage(damage), fx);
         }
      };
   }

   static {
      HashMap<String, AbsorbedPower> map = new HashMap<>();

      for (AbsorbedPower p : values()) {
         map.put(p.powerId, p);
      }

      BY_ID = Map.copyOf(map);
   }
}
