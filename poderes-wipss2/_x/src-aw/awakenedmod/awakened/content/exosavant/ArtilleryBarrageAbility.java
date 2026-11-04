package com.awakenedmod.awakened.content.exosavant;

import com.awakenedmod.awakened.content.entity.projectile.KineticProjectile;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class ArtilleryBarrageAbility extends Ability {
   private static final int UNLOCK = 13;
   private static final float COST = 45.0F;
   private static final double RANGE = 40.0;
   private static final int SHELL_COUNT = 24;
   private static final int SALVO_TICKS = 50;
   private static final double ZONE_RADIUS = 5.0;
   private static final double GROUND_PROBE = 48.0;
   private static final double SHELL_GRAVITY = 0.06;
   private static final int FLIGHT_TICKS = 40;
   private static final double DRAG_COMP = 1.2;
   private static final float SHELL_INACCURACY = 0.5F;
   private static final float SHELL_IMPACT = 6.0F;
   private static final float SHELL_AOE = 5.0F;
   private static final float SHELL_AOE_RADIUS = 3.0F;
   private static final float SHELL_KNOCKBACK = 0.05F;
   private static final double SHELL_KNOCKBACK_LIFT = 0.0;
   private static final double SHELL_CONTACT_RADIUS = 0.6;
   private final Map<UUID, ArtilleryBarrageAbility.Salvo> salvos = this.playerState();

   public ArtilleryBarrageAbility() {
      super("artillery_barrage", 45.0F, 13);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(6.0).build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (!ExoLoadout.usableSuited(player)) {
         return false;
      } else if (this.salvos.containsKey(player.getUUID())) {
         return this.fail(player, "awakened.exosavant.barrage_busy", new Object[0]);
      } else {
         ServerLevel level = player.serverLevel();
         Vec3 zone = resolveZone(player, level, 1.0F).zone();
         this.salvos.put(player.getUUID(), new ArtilleryBarrageAbility.Salvo(zone));
         ExoDeploy.deployBarrage(player, 50);
         player.swing(InteractionHand.MAIN_HAND);
         Fx.sound(level, player.position(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 0.6F);
         Fx.sound(level, player.position(), (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.5F, 0.7F);
         Vfx.ring(level, zone.add(0.0, 0.1, 0.0), 5.0, 10475775, 50);
         return true;
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      ArtilleryBarrageAbility.Salvo salvo = this.salvos.get(player.getUUID());
      if (salvo != null) {
         salvo.elapsed++;
         ServerLevel level = player.serverLevel();

         for (int target = Math.min(24, (int)Math.ceil(24.0 * (double)salvo.elapsed / 50.0)); salvo.fired < target; salvo.fired++) {
            this.fireShell(player, level, salvo.zone);
         }

         if (salvo.elapsed >= 50 || salvo.fired >= 24) {
            this.salvos.remove(player.getUUID());
         }
      }
   }

   public static ArtilleryBarrageAbility.ZoneSolution resolveZone(Player player, Level level, float partialTicks) {
      Vec3 aim = player.pick(40.0, partialTicks, false).getLocation();
      Vec3 down = aim.subtract(0.0, 48.0, 0.0);
      BlockHitResult hit = level.clip(new ClipContext(aim, down, Block.COLLIDER, Fluid.NONE, player));
      boolean grounded = hit.getType() == Type.BLOCK;
      return new ArtilleryBarrageAbility.ZoneSolution(grounded ? hit.getLocation() : aim, grounded);
   }

   public static double zoneRadius() {
      return 5.0;
   }

   private void fireShell(ServerPlayer player, ServerLevel level, Vec3 zone) {
      RandomSource rng = player.getRandom();
      double yaw = Math.toRadians((double)player.getYRot());
      Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
      double side = rng.nextBoolean() ? 1.0 : -1.0;
      Vec3 launch = player.getEyePosition().add(0.0, 0.4, 0.0).add(right.scale(0.5 * side));
      double a = rng.nextDouble() * (Math.PI * 2);
      double r = Math.sqrt(rng.nextDouble()) * 5.0;
      Vec3 target = zone.add(Math.cos(a) * r, 0.0, Math.sin(a) * r);
      double dx = target.x - launch.x;
      double dz = target.z - launch.z;
      double dy = target.y - launch.y;
      double t = 40.0;
      double vx = dx / t * 1.2;
      double vz = dz / t * 1.2;
      double vy = (dy + 0.03 * t * t) / t;
      Vec3 vel = new Vec3(vx, vy, vz);
      double speed = vel.length();
      if (!(speed < 1.0E-4)) {
         Vec3 dir = vel.scale(1.0 / speed);
         float impact = MasteryScaling.scaledDamage(6.0F, player, this.balanceKey());
         float aoe = MasteryScaling.scaledDamage(5.0F, player, this.balanceKey());
         KineticProjectile shell = ExoProjectiles.spawnShell(
            player, level, Items.IRON_NUGGET, launch, dir, speed, 0.5F, impact, aoe, 3.0F, 0.05F, 0.06, 0.6, null, 0.0
         );
         shell.setKnockbackLift(0.0);
         ExoFx.exhaustTrail(level, launch);
         Fx.sound(level, launch, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.5F, 0.8F);
      }
   }

   private static final class Salvo {
      private final Vec3 zone;
      private int elapsed;
      private int fired;

      private Salvo(Vec3 zone) {
         this.zone = zone;
      }
   }

   public static record ZoneSolution(Vec3 zone, boolean grounded) {
   }
}
