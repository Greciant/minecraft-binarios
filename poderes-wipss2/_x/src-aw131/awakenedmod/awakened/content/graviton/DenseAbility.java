package com.awakenedmod.awakened.content.graviton;

import com.awakenedmod.awakened.content.AwakenedSounds;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.Attrs;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.BlockCracks;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.LoopSound;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.Shockwaves;
import com.awakenedmod.awakened.core.Taunt;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class DenseAbility extends Ability {
   public static final String ID = "dense";
   public static final String ANIM_HOLD = "dense";
   public static final float COST = 10.0F;
   public static final int UNLOCK = 7;
   private static final float DRAIN_PER_TICK = 0.5F;
   private static final int SYNC_TICKS = 5;
   public static final int LOW_STAMINA_TICKS = 40;
   private static final float DAMAGE_REDUCTION = 0.6F;
   private static final int DAMAGE_REDUCTION_PCT = 60;
   private static final double SPEED_MOD = -0.65;
   private static final double JUMP_MOD = -0.55;
   private static final double TAUNT_RADIUS = 10.0;
   private static final int TAUNT_EVERY = 10;
   private static final int TAUNT_FEEDBACK_MAX = 3;
   public static final float VISUAL_CAP = 40.0F;
   private static final double RADIUS_BASE = 3.0;
   private static final double RADIUS_PER = 0.15;
   private static final double RADIUS_MAX = 10.0;
   private static final float DAMAGE_BASE = 4.0F;
   private static final float DAMAGE_PER = 0.75F;
   private static final float DAMAGE_MAX = 30.0F;
   public static final float DAMAGE_CAP_STORE = 34.666668F;
   private static final double KNOCK_BASE = 0.9;
   private static final double KNOCK_PER = 0.035;
   private static final double KNOCK_MAX = 2.4;
   private static final double LIFT_BASE = 0.35;
   private static final double LIFT_PER = 0.008;
   private static final double LIFT_MAX = 0.7;
   private static final double EDGE_FALLOFF = 0.5;
   public static final double PULSE_RADIUS = 2.5;
   private static final double PULSE_KNOCK = 0.6;
   private static final double PULSE_LIFT = 0.2;
   private static final float CLUTCH_HEALTH = 6.0F;
   private static final int FRESH_HIT_IFRAMES = 10;
   private static final int HIT_FX_EVERY = 4;
   private static final float RUMBLE_FROM = 0.25F;
   private static final int CRACK_EVERY = 10;
   private static final int CRACK_SPOKES = 6;
   private static final int CRACK_MAX = 110;
   private static final int HIT_CRACK_BUMP = 2;
   private static final double HEAVE_RING_STEP = 1.5;
   private static final int HEAVE_MAX = 72;
   private static final double HEAVE_PLATE = 0.96;
   private static final double HEAVE_THICK = 0.5;
   private static final double HEAVE_SEAT = 0.01;
   private static final int HEAVE_LIFE = 14;
   private static final String HUM_KEY = "graviton.dense.load";
   private static final ResourceLocation KNOCKBACK_ID = AwId.of("graviton_dense_knockback");
   private static final ResourceLocation EXPLOSION_KB_ID = AwId.of("graviton_dense_explosion_knockback");
   private static final ResourceLocation SPEED_ID = AwId.of("graviton_dense_speed");
   private static final ResourceLocation JUMP_ID = AwId.of("graviton_dense_jump");
   private final Map<UUID, DenseAbility.Hold> holds = this.activeState();

   public DenseAbility() {
      super("dense", 10.0F, 7);
   }

   @Override
   public String costLabel() {
      return (int)this.billed(10.0F) + "/s";
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().boostsDamage().build();
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   public static double releaseRadius(float stored) {
      return stored <= 0.0F ? 2.5 : Math.min(10.0, 3.0 + (double)stored * 0.15);
   }

   public static float chargeOf(float stored) {
      return Mth.clamp(stored / 40.0F, 0.0F, 1.0F);
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (this.holds.containsKey(player.getUUID())) {
         return false;
      } else {
         DenseAbility.Hold hold = new DenseAbility.Hold();
         this.holds.put(player.getUUID(), hold);
         applyStance(player);
         player.setSprinting(false);
         DenseVisual.set(player, new DenseVisual(true, 0, 0, 0, false));
         PowerService.playAnimation(player, "dense", true);
         ServerLevel level = player.serverLevel();
         Vec3 feet = player.position();
         Fx.groundImpact(level, feet, 1.0, false);
         floorSound(level, feet, true, 0.8F, 0.5F);
         Fx.sound(level, feet, (SoundEvent)AwakenedSounds.DENSE_PLANT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
         Fx.screenShake(level, feet, 4.0, 4, 16747069, true, true);
         refreshCracks(level, player, hold);
         taunt(level, player);
         return true;
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      DenseAbility.Hold hold = this.holds.get(player.getUUID());
      if (hold != null) {
         hold.ticks++;
         PowerData data = PowerService.data(player);
         boolean low = false;
         if (!data.infiniteStamina()) {
            float drain = this.billed(0.5F);
            float remaining = StaminaSystem.drainSustained(data, player.serverLevel().getGameTime(), drain);
            if (remaining <= 0.0F || hold.ticks % 5 == 0) {
               PowerService.sync(player);
            }

            if (remaining <= 0.0F && drain > 0.0F) {
               this.release(player);
               return;
            }

            low = remaining < drain * 40.0F;
         }

         applyStance(player);
         if (player.isSprinting()) {
            player.setSprinting(false);
         }

         ServerLevel level = player.serverLevel();
         if (hold.ticks % 10 == 0) {
            taunt(level, player);
         }

         float charge = hold.charge();
         DenseVisual.set(player, DenseVisual.of(player).withStore(charge, hold.stored, low));
         float wobble = low ? 0.05F * Mth.sin((float)hold.ticks * (float) (Math.PI * 2.0 / 5.0)) : 0.0F;
         LoopSound.keep(
            player,
            "graviton.dense.load",
            (SoundEvent)AwakenedSounds.DENSE_LOAD.get(),
            SoundSource.PLAYERS,
            0.5F + 0.4F * charge,
            0.6F + 0.3F * charge + wobble
         );
         int beat = Math.round(20.0F - 10.0F * charge);
         if (hold.ticks - hold.lastBeat >= beat) {
            hold.lastBeat = hold.ticks;
            floorSound(level, player.position(), false, 0.45F + 0.35F * charge, 0.5F);
         }

         if (hold.ticks % 10 == 0 || !player.blockPosition().equals(hold.crackFeet)) {
            refreshCracks(level, player, hold);
         }
      }
   }

   @Override
   public float onIncomingDamage(ServerPlayer player, DamageSource source, float amount) {
      DenseAbility.Hold hold = this.holds.get(player.getUUID());
      if (hold != null && !(amount <= 0.0F) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         float reduced = amount * 0.39999998F;
         float landed;
         if (player.invulnerableTime > 10 && !source.is(DamageTypeTags.BYPASSES_COOLDOWN)) {
            landed = Math.max(0.0F, reduced - hold.lastReduced);
            hold.lastReduced = Math.max(hold.lastReduced, reduced);
         } else {
            landed = reduced;
            hold.lastReduced = reduced;
         }

         if (landed > 0.0F && Foes.threat(source, player)) {
            hold.stored += landed / 0.39999998F;
            hold.hits++;
            absorb(player.serverLevel(), player, hold, source);
         }

         return reduced;
      } else {
         return amount;
      }
   }

   private static void absorb(ServerLevel level, ServerPlayer player, DenseAbility.Hold hold, DamageSource source) {
      Vec3 feet = player.position();
      Vec3 toward = attackerSide(level, player, source);
      Vec3 patch = feet.add(toward.scale(1.2));

      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            if (Math.abs(dx) + Math.abs(dz) <= 1) {
               BlockPos pos = BlockCracks.floorTop(level, patch.x + (double)dx, feet.y, patch.z + (double)dz);
               if (pos != null) {
                  hold.bump.merge(pos.asLong(), 2, (a, b) -> Math.min(6, a + b));
               }
            }
         }
      }

      refreshCracks(level, player, hold);
      DenseVisual now = DenseVisual.of(player);
      DenseVisual.set(player, now.withStore(hold.charge(), hold.stored, now.low()));
      if (hold.lastHitFx < 0 || hold.ticks - hold.lastHitFx >= 4) {
         hold.lastHitFx = hold.ticks;
         DenseVisual.set(player, DenseVisual.of(player).hit());
         float charge = hold.charge();
         Fx.Floor floor = Fx.floorUnder(level, patch.x, feet.y + 0.5, patch.z, 3);
         if (floor != null) {
            Vec3 at = new Vec3(patch.x, floor.topY() + 0.05, patch.z);
            Fx.blockSpall(level, at, floor.state(), 8);
            int chips = 1 + level.random.nextInt(2);

            for (int i = 0; i < chips; i++) {
               double spread = (level.random.nextDouble() - 0.5) * 0.12;
               Vec3 vel = new Vec3(toward.x * 0.12 - toward.z * spread, 0.28 + level.random.nextDouble() * 0.1, toward.z * 0.12 + toward.x * spread);
               Vfx.blockThrown(
                  level, at.add(0.0, 0.15, 0.0), floor.state(), 0.25, 0.25, 0.25, 30, level.random.nextFloat() * 360.0F, vel, floor.topY() + 0.125, 20.0F
               );
            }
         }

         Fx.sound(level, feet, (SoundEvent)AwakenedSounds.DENSE_ABSORB.get(), SoundSource.PLAYERS, 0.9F, 0.9F + 0.25F * charge);
         floorSound(level, feet, true, 0.6F, 0.6F);
      }
   }

   private static Vec3 attackerSide(ServerLevel level, ServerPlayer player, DamageSource source) {
      Entity from = source.getDirectEntity() != null ? source.getDirectEntity() : source.getEntity();
      Vec3 d = from != null ? from.position().subtract(player.position()) : Vec3.ZERO;
      d = new Vec3(d.x, 0.0, d.z);
      if (d.lengthSqr() < 1.0E-4) {
         double a = level.random.nextDouble() * Math.PI * 2.0;
         return new Vec3(Math.cos(a), 0.0, Math.sin(a));
      } else {
         return d.normalize();
      }
   }

   private static void taunt(ServerLevel level, ServerPlayer player) {
      List<Mob> turned = Taunt.turn(player, 10.0);

      for (int i = 0; i < turned.size() && i < 3; i++) {
         Mob mob = turned.get(i);
         Vec3 at = mob.position();
         Vec3 dir = player.position().subtract(at);
         dir = new Vec3(dir.x, 0.0, dir.z);
         if (!(dir.lengthSqr() < 1.0E-4)) {
            dir = dir.normalize();
            Fx.Floor floor = Fx.floorUnder(level, at.x, at.y + 0.5, at.z, 3);
            if (floor != null) {
               BlockParticleOption grit = new BlockParticleOption(ParticleTypes.BLOCK, floor.state());

               for (int k = 0; k < 3; k++) {
                  Vec3 p = at.add(dir.scale(0.35 * (double)k));
                  level.sendParticles(grit, p.x, floor.topY() + 0.05, p.z, 0, dir.x, 0.05, dir.z, 0.25);
               }

               Fx.sound(level, at, floor.state().getSoundType().getStepSound(), SoundSource.HOSTILE, 0.4F, 0.6F);
            }
         }
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      this.release(player);
   }

   private void release(ServerPlayer player) {
      DenseAbility.Hold hold = this.holds.remove(player.getUUID());
      if (hold != null) {
         endHold(player);
         this.rebound(player, hold);
         if (hold.hits > 0 && player.isAlive() && player.getHealth() <= 6.0F) {
            MasterySystem.award(player, MasteryReason.SURVIVAL);
         }
      }
   }

   private void rebound(ServerPlayer player, DenseAbility.Hold hold) {
      ServerLevel level = player.serverLevel();
      Vec3 feet = player.position();
      Vec3 center = feet.add(0.0, (double)player.getBbHeight() * 0.5, 0.0);
      boolean empty = hold.hits == 0;
      double s = (double)hold.stored;
      double radius = empty ? 2.5 : releaseRadius(hold.stored);
      float damage = empty ? 0.0F : MasteryScaling.scaledDamage(Math.min(30.0F, 4.0F + (float)s * 0.75F), player, this.balanceKey());
      double knock = empty ? 0.6 : Math.min(2.4, 0.9 + s * 0.035);
      double lift = empty ? 0.2 : Math.min(0.7, 0.35 + s * 0.008);
      List<LivingEntity> near = level.getEntitiesOfClass(
         LivingEntity.class,
         new AABB(center, center).inflate(radius),
         ex -> ex != player && !ex.isPassengerOfSameVehicle(player) && ex.isAlive() && ex.distanceToSqr(center) <= radius * radius
      );
      List<LivingEntity> hit = new ArrayList<>();

      for (LivingEntity target : Harm.hostilesOnly(player, near)) {
         double t = Math.sqrt(target.distanceToSqr(center)) / radius;
         double scale = 1.0 - 0.5 * t;
         if (damage > 0.0F && GravitySupport.crush(level, player, target, (float)((double)damage * scale))) {
            hit.add(target);
         }

         GravitySupport.shove(target, center, knock * scale, lift * scale);
      }

      if (!hit.isEmpty()) {
         MasterySystem.award(player, MasteryReason.HIT, hit);
      }

      PowerService.stopAnimation(player);
      PowerService.playAnimation(player, "dense_release");
      if (empty) {
         BlockCracks.clear(cracksOwner(player));
         Vfx.ring(level, feet.add(0.0, 0.05, 0.0), radius, 16769732, 8);
         Fx.groundImpact(level, feet, 0.8, false);
         floorSound(level, feet, false, 0.9F, 1.2F);
         Fx.sound(level, feet, (SoundEvent)AwakenedSounds.DENSE_PUFF.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
      } else {
         float charge = hold.charge();
         int travel = (int)Mth.clamp(radius * 1.5, 6.0, 14.0);

         for (Entry<Long, Integer> e : hold.cracks.entrySet()) {
            BlockPos pos = BlockPos.of(e.getKey());
            double d = Math.sqrt(sqrHoriz(Vec3.atCenterOf(pos), feet));
            int delay = (int)Math.round(Mth.clamp(d / radius, 0.0, 1.0) * (double)travel);
            BlockCracks.crack(level, cracksOwner(player), pos, Math.min(9, e.getValue() + 1), BlockCracks.Timing.holdThenFade(delay, 0));
         }

         hold.cracks.clear();
         heaveFront(level, feet, radius, travel, charge);
         Fx.groundShockwave(level, feet.add(0.0, 0.05, 0.0), 0.8, radius, travel, Shockwaves.Style.DEFAULT.noGlowRing().silent().noShake());
         Vfx.ring(level, feet.add(0.0, 0.5, 0.0), radius, 16769732, travel);
         Fx.groundImpact(level, feet, 1.2 + 1.5 * (double)charge, false);
         Fx.sound(level, feet, (SoundEvent)AwakenedSounds.DENSE_RELEASE.get(), SoundSource.PLAYERS, 0.55F + 0.45F * charge, 1.15F - 0.25F * charge);
         if (charge > 0.25F) {
            Fx.sound(level, feet, (SoundEvent)AwakenedSounds.DENSE_RUMBLE.get(), SoundSource.PLAYERS, charge, 1.05F - 0.15F * charge);
         }

         floorSound(level, feet, true, 0.8F, 0.5F);
         Fx.screenShake(level, center, radius + 6.0, 6 + Math.round(8.0F * charge), 16747069, true, true);
      }
   }

   private static void heaveFront(ServerLevel level, Vec3 feet, double radius, int travel, float charge) {
      List<double[]> cells = new ArrayList<>();
      int reach = (int)Math.ceil(radius);
      int cx = Mth.floor(feet.x);
      int cz = Mth.floor(feet.z);

      for (int dx = -reach; dx <= reach; dx++) {
         for (int dz = -reach; dz <= reach; dz++) {
            double x = (double)(cx + dx) + 0.5;
            double z = (double)(cz + dz) + 0.5;
            double d = Math.sqrt((x - feet.x) * (x - feet.x) + (z - feet.z) * (z - feet.z));
            if (!(d < 1.0) && !(d > radius + 0.3)) {
               double ring = (double)Math.round(d / 1.5) * 1.5;
               if (!(ring < 1.4849999999999999) && !(Math.abs(d - ring) > 0.5)) {
                  cells.add(new double[]{x, z, d});
               }
            }
         }
      }

      if (cells.size() > 72) {
         List<double[]> kept = new ArrayList<>();
         double stride = (double)cells.size() / 72.0;

         for (double i = 0.0; i < (double)cells.size() && kept.size() < 72; i += stride) {
            kept.add(cells.get((int)i));
         }

         cells = kept;
      }

      for (double[] c : cells) {
         BlockPos pos = BlockCracks.floorTop(level, c[0], feet.y, c[1]);
         if (pos != null) {
            BlockState state = level.getBlockState(pos);
            double d = c[2];
            double ox = c[0] - feet.x;
            double oz = c[1] - feet.z;
            boolean alongX = Math.abs(ox) >= Math.abs(oz);
            float yaw = alongX ? 0.0F : 90.0F;
            float side = (float)Math.signum(alongX ? ox : oz);
            float tilt = (15.0F + level.random.nextFloat() * 10.0F) * side * (alongX ? -1.0F : 1.0F);
            double rise = (0.25 + 0.45 * (double)charge) * (1.0 - 0.35 * d / radius);
            int delay = (int)Math.round(Mth.clamp(d / radius, 0.0, 1.0) * (double)travel);
            Vec3 bottom = new Vec3(c[0], (double)pos.getY() + 1.0 - 0.5 - 0.01, c[1]);
            Vfx.blockHeave(level, bottom, state, 0.96, 0.5, 0.96, 14, yaw, rise, tilt, delay, 16747069, 0.4F);
         }
      }
   }

   private static Object cracksOwner(ServerPlayer player) {
      return BlockCracks.owner(player, "dense_hold");
   }

   private static void refreshCracks(ServerLevel level, ServerPlayer player, DenseAbility.Hold hold) {
      Object owner = cracksOwner(player);
      Vec3 feet = player.position();
      hold.crackFeet = player.blockPosition();
      Map<Long, Integer> want = new HashMap<>();
      if (hold.hits == 0) {
         BlockPos under = BlockCracks.floorTop(level, feet.x, feet.y, feet.z);
         if (under != null) {
            want.put(under.asLong(), 3);
         }
      } else {
         float charge = hold.charge();
         double radius = releaseRadius(hold.stored);
         double core = 1.2 + 1.3 * (double)charge;
         int centreStage = Math.round(4.0F + 5.0F * charge);
         int edgeStage = Math.round(1.0F + 2.0F * charge);
         int reach = (int)Math.ceil(radius);
         int cx = Mth.floor(feet.x);
         int cz = Mth.floor(feet.z);
         List<double[]> picks = new ArrayList<>();

         for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
               double x = (double)(cx + dx) + 0.5;
               double z = (double)(cz + dz) + 0.5;
               double ox = x - feet.x;
               double oz = z - feet.z;
               double d = Math.sqrt(ox * ox + oz * oz);
               if (!(d > radius + 0.35)) {
                  int priority;
                  if (d <= core) {
                     priority = 0;
                  } else if (d >= radius - 0.65) {
                     priority = 1;
                  } else {
                     if (!onSpoke(hold.seed, ox, oz, d)) {
                        continue;
                     }

                     priority = 2;
                  }

                  picks.add(new double[]{x, z, d, (double)priority});
               }
            }
         }

         picks.sort((a, bx) -> a[3] != bx[3] ? Double.compare(a[3], bx[3]) : Double.compare(a[2], bx[2]));

         for (double[] p : picks) {
            if (want.size() >= 110) {
               break;
            }

            BlockPos pos = BlockCracks.floorTop(level, p[0], feet.y, p[1]);
            if (pos != null) {
               double u = Mth.clamp(p[2] / radius, 0.0, 1.0);
               int stage = (int)Math.round((double)edgeStage + (double)(centreStage - edgeStage) * Math.pow(1.0 - u, 1.4));
               if (p[3] == 1.0) {
                  stage = Math.max(stage, edgeStage + 2);
               }

               want.put(pos.asLong(), stage);
            }
         }
      }

      for (Entry<Long, Integer> b : hold.bump.entrySet()) {
         want.computeIfPresent(b.getKey(), (k, base) -> Math.min(9, base + b.getValue()));
      }

      Map<Long, Integer> shown = new HashMap<>();

      for (Entry<Long, Integer> e : want.entrySet()) {
         Integer had = hold.cracks.get(e.getKey());
         if (e.getValue().equals(had) || BlockCracks.crack(level, owner, BlockPos.of(e.getKey()), e.getValue(), BlockCracks.Timing.STEADY)) {
            shown.put(e.getKey(), e.getValue());
         }
      }

      for (Long gone : hold.cracks.keySet()) {
         if (!shown.containsKey(gone)) {
            BlockCracks.clear(level, owner, BlockPos.of(gone));
         }
      }

      hold.cracks.clear();
      hold.cracks.putAll(shown);
   }

   private static boolean onSpoke(long seed, double ox, double oz, double d) {
      double a = Math.atan2(oz, ox);

      for (int i = 0; i < 6; i++) {
         double spoke = (Math.PI * 2) * (double)i / 6.0 + (hash(seed, i) - 0.5) * 0.8;
         spoke += Math.sin(d * 0.9 + (double)i * 1.7) * 0.12;
         double diff = Mth.wrapDegrees(Math.toDegrees(a - spoke));
         double across = Math.abs(Math.sin(Math.toRadians(diff))) * d;
         if (Math.abs(diff) < 90.0 && across < 0.55) {
            return true;
         }
      }

      return false;
   }

   private static double hash(long seed, int i) {
      long h = seed * -7046029254386353131L + (long)i * -4417276706812531889L;
      h ^= h >>> 33;
      h *= -49064778989728563L;
      h ^= h >>> 33;
      return (double)(h >>> 11) * 1.110223E-16F;
   }

   private static double sqrHoriz(Vec3 a, Vec3 b) {
      double dx = a.x - b.x;
      double dz = a.z - b.z;
      return dx * dx + dz * dz;
   }

   private static void floorSound(ServerLevel level, Vec3 at, boolean breakSound, float volume, float pitch) {
      Fx.Floor floor = Fx.floorUnder(level, at.x, at.y - 0.01, at.z, 3);
      if (floor != null) {
         SoundType type = floor.state().getSoundType();
         Fx.sound(level, at, breakSound ? type.getBreakSound() : type.getStepSound(), SoundSource.PLAYERS, volume, pitch);
      }
   }

   private static void applyStance(ServerPlayer player) {
      Attrs.set(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, 1.0, Operation.ADD_VALUE);
      Attrs.set(player, Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, EXPLOSION_KB_ID, 1.0, Operation.ADD_VALUE);
      Attrs.set(player, Attributes.MOVEMENT_SPEED, SPEED_ID, -0.65, Operation.ADD_MULTIPLIED_TOTAL);
      Attrs.set(player, Attributes.JUMP_STRENGTH, JUMP_ID, -0.55, Operation.ADD_MULTIPLIED_TOTAL);
   }

   private static void endHold(ServerPlayer player) {
      Attrs.set(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, 0.0, Operation.ADD_VALUE);
      Attrs.set(player, Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, EXPLOSION_KB_ID, 0.0, Operation.ADD_VALUE);
      Attrs.set(player, Attributes.MOVEMENT_SPEED, SPEED_ID, 0.0, Operation.ADD_MULTIPLIED_TOTAL);
      Attrs.set(player, Attributes.JUMP_STRENGTH, JUMP_ID, 0.0, Operation.ADD_MULTIPLIED_TOTAL);
      LoopSound.stop(player, "graviton.dense.load");
      DenseVisual.set(player, DenseVisual.NONE);
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      if (this.holds.remove(player.getUUID()) != null) {
         PowerService.stopAnimation(player);
      }

      endHold(player);
      BlockCracks.clear(cracksOwner(player));
      super.clearPlayer(player);
   }

   private static final class Hold {
      int ticks;
      float stored;
      int hits;
      int lastBeat;
      int lastHitFx = -1;
      float lastReduced;
      final long seed = ThreadLocalRandom.current().nextLong();
      BlockPos crackFeet = BlockPos.ZERO;
      final Map<Long, Integer> cracks = new HashMap<>();
      final Map<Long, Integer> bump = new HashMap<>();

      float charge() {
         return DenseAbility.chargeOf(this.stored);
      }
   }
}
