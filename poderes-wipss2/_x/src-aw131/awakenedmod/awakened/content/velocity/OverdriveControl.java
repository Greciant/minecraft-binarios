package com.awakenedmod.awakened.content.velocity;

import com.awakenedmod.awakened.core.Attrs;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.FluidFooting;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.data.VelocityVisual;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public final class OverdriveControl {
   private static final ResourceLocation SPEED_ID = AwId.of("velocity_overdrive");
   private static final ResourceLocation STEP_ID = AwId.of("velocity_step");
   private static final ResourceLocation FALL_ID = AwId.of("velocity_fall");
   private static final ResourceLocation FLURRY_ID = AwId.of("velocity_flurry");
   public static final double[] GEAR_SPRINT_MULT = new double[]{2.0, 2.4, 2.8, 3.2, 3.6, 4.0};
   public static final double WALK_MULT = 1.3;
   public static final double STEP_BONUS = 0.6;
   public static final int STEP_GEAR = 1;
   public static final double HIGH_STEP_BONUS = 1.0;
   public static final int HIGH_STEP_GEAR = 3;
   public static final double FALL_DAMAGE_CUT = -0.5;
   public static final int SURFACE_GEAR = 2;
   private static final double DIVE_FALL_DISTANCE = 1.5;
   private static final int LAVA_COYOTE_TICKS = 20;
   public static final int CONTACT_GEAR = 3;
   public static final double FLURRY_PER_GEAR = 0.2;
   private static final double SONIC_REACH = 1.2;
   private static final float SONIC_DAMAGE = 2.0F;
   private static final double SONIC_PUSH = 0.6;
   private static final double SONIC_LIFT = 0.25;
   private static final int SONIC_COOLDOWN = 20;
   private static final String SONIC_BALANCE_KEY = "OverdriveAbility";
   private static final Map<UUID, Map<Integer, Long>> SONIC_HITS = new ConcurrentHashMap<>();
   private static final Set<UUID> LAVA_RUNNERS = ConcurrentHashMap.newKeySet();
   private static final Map<UUID, Long> LAVA_COYOTE = new ConcurrentHashMap<>();
   public static final double VANILLA_SPRINT_BPT = 0.2806;

   private OverdriveControl() {
   }

   public static double sprintMultiplier(int gear) {
      return GEAR_SPRINT_MULT[Math.max(0, Math.min(GEAR_SPRINT_MULT.length - 1, gear))];
   }

   public static double sprintMultiplier(int gear, ServerPlayer player) {
      return 1.0 + (sprintMultiplier(gear) - 1.0) * (1.0 + (double)MasteryScaling.progressionBonus(player));
   }

   public static double expectedSprintStride(ServerPlayer player, PowerData data) {
      return sprintMultiplier(Gearbox.effectiveGear(player, data), player) * 0.2806;
   }

   public static void tick(ServerPlayer player, PowerData data) {
      boolean on = OverdriveAbility.isOn(data);
      int gear = on ? Gearbox.effectiveGear(player, data) : 0;
      double speed = 0.0;
      double step = 0.0;
      double fall = 0.0;
      double flurry = 0.0;
      if (on) {
         boolean sprinting = player.isSprinting();
         speed = sprinting ? sprintMultiplier(gear, player) - 1.0 : 0.30000000000000004;
         step = !sprinting ? 0.0 : (gear >= 3 ? 1.0 : (gear >= 1 ? 0.6 : 0.0));
         fall = sprinting && gear >= 1 ? -0.5 : 0.0;
         flurry = (double)gear * 0.2;
      }

      setModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID, speed, Operation.ADD_MULTIPLIED_TOTAL);
      setModifier(player, Attributes.STEP_HEIGHT, STEP_ID, step, Operation.ADD_VALUE);
      setModifier(player, Attributes.FALL_DAMAGE_MULTIPLIER, FALL_ID, fall, Operation.ADD_MULTIPLIED_TOTAL);
      setModifier(player, Attributes.ATTACK_SPEED, FLURRY_ID, flurry, Operation.ADD_MULTIPLIED_TOTAL);
      Downstep.stepDown(player);
      lavaFootingTick(player, on && gear >= 2 && player.isSprinting());
      if (on && gear >= 3 && player.isSprinting()) {
         sonicContact(player);
      }
   }

   private static void lavaFootingTick(ServerPlayer player, boolean qualifies) {
      UUID id = player.getUUID();
      if (!overLava(player)) {
         LAVA_RUNNERS.remove(id);
         LAVA_COYOTE.remove(id);
      } else if (qualifies) {
         LAVA_RUNNERS.add(id);
         LAVA_COYOTE.remove(id);
      } else {
         long now = player.serverLevel().getGameTime();
         if (LAVA_RUNNERS.remove(id)) {
            LAVA_COYOTE.put(id, now + 20L);
            player.displayClientMessage(Component.translatable("awakened.velocity.lava_footing"), true);
         } else {
            Long until = LAVA_COYOTE.get(id);
            if (until != null && now >= until) {
               LAVA_COYOTE.remove(id);
            }
         }
      }
   }

   private static boolean overLava(ServerPlayer player) {
      Vec3 feet = player.position();
      return player.serverLevel().getFluidState(BlockPos.containing(feet.x, feet.y - 0.1, feet.z)).is(FluidTags.LAVA);
   }

   static boolean lavaCoyote(ServerPlayer player) {
      Long until = LAVA_COYOTE.get(player.getUUID());
      return until != null && player.serverLevel().getGameTime() < until;
   }

   public static boolean canSurfaceRun(Player runner, FluidState fluid) {
      if (!fluid.isEmpty() && (fluid.is(FluidTags.WATER) || fluid.is(FluidTags.LAVA))) {
         if (FluidFooting.wading(runner)) {
            return false;
         } else {
            VelocityVisual visual = VelocityVisualState.of(runner);
            boolean earned = visual.overdrive() && runner.isSprinting() && visual.gear() >= 2;
            return earned || visual.coyote() && fluid.is(FluidTags.LAVA) ? !fluid.is(FluidTags.WATER) || !((double)runner.fallDistance > 1.5) : false;
         }
      } else {
         return false;
      }
   }

   private static void sonicContact(ServerPlayer player) {
      Vec3 travel = player.getKnownMovement();
      Vec3 dir = new Vec3(travel.x, 0.0, travel.z);
      if (!(dir.lengthSqr() < 1.0E-6)) {
         dir = dir.normalize();
         ServerLevel level = player.serverLevel();
         long now = level.getGameTime();
         Map<Integer, Long> struck = SONIC_HITS.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>());
         struck.values().removeIf(when -> now - when >= 20L);

         for (LivingEntity foe : level.getEntitiesOfClass(
            LivingEntity.class, player.getBoundingBox().inflate(1.2), foex -> foex != player && foex.isAlive() && Harm.allowed(player, foex)
         )) {
            if (struck.putIfAbsent(foe.getId(), now) == null
               && Dmg.hurtNoKnockback(foe, level, Dmg.by(player, DamageTypes.PLAYER_ATTACK), Momentum.empoweredDamage(player, 2.0F, "OverdriveAbility"))) {
               Vec3 aside = asideOf(player, foe, dir);
               Harm.push(player, foe, aside.x * 0.6, 0.25, aside.z * 0.6);
               VelocityFx.sonicCrack(level, VelocityPalette.of(player), foe.position().add(0.0, (double)foe.getBbHeight() * 0.5, 0.0), dir);
            }
         }
      }
   }

   private static Vec3 asideOf(ServerPlayer player, LivingEntity foe, Vec3 dir) {
      Vec3 left = new Vec3(-dir.z, 0.0, dir.x);
      Vec3 offset = foe.position().subtract(player.position());
      return left.dot(new Vec3(offset.x, 0.0, offset.z)) < 0.0 ? left.reverse() : left;
   }

   public static void clear(ServerPlayer player) {
      if (player != null) {
         setModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID, 0.0, Operation.ADD_MULTIPLIED_TOTAL);
         setModifier(player, Attributes.STEP_HEIGHT, STEP_ID, 0.0, Operation.ADD_VALUE);
         setModifier(player, Attributes.FALL_DAMAGE_MULTIPLIER, FALL_ID, 0.0, Operation.ADD_MULTIPLIED_TOTAL);
         setModifier(player, Attributes.ATTACK_SPEED, FLURRY_ID, 0.0, Operation.ADD_MULTIPLIED_TOTAL);
         SONIC_HITS.remove(player.getUUID());
         LAVA_RUNNERS.remove(player.getUUID());
         LAVA_COYOTE.remove(player.getUUID());
         Downstep.forget(player.getUUID());
      }
   }

   public static void clearAll() {
      SONIC_HITS.clear();
      LAVA_RUNNERS.clear();
      LAVA_COYOTE.clear();
      Downstep.clearAll();
   }

   private static void setModifier(ServerPlayer player, Holder<Attribute> attr, ResourceLocation id, double amount, Operation op) {
      Attrs.set(player, attr, id, amount, op);
   }
}
