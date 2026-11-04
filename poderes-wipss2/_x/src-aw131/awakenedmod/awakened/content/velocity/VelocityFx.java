package com.awakenedmod.awakened.content.velocity;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class VelocityFx {
   private static final float DUST_SCALE = 1.2F;
   private static final int SONIC_CRACK_TICKS = 6;
   private static final int SONIC_BOOM_TICKS = 14;
   private static final int GEAR_BURST_TICKS = 6;
   private static final int ECHO_TICKS = 8;
   private static final double ECHO_LAG_BLOCKS = 1.4;
   private static final double ECHO_LAG_TICKS = 3.0;
   private static final int BOLT_TICKS = 7;
   private static final int BOLT_TRAVEL_TICKS = 2;
   private static final int BOLT_IMPACT_TICKS = 8;
   public static final int RING_TICKS = 8;
   public static final int BOUNDARY_FADE_TICKS = 6;
   private static final double SONIC_CRACK_SCALE = 1.0;
   private static final double SONIC_BOOM_SCALE = 1.0;
   private static final double SONIC_BOOM_LAG = 1.5;
   private static final double SONIC_BOOM_HEIGHT = 1.2;
   private static final float SONIC_BOOM_CRACK_VOLUME = 0.9F;
   private static final float SONIC_BOOM_CRACK_PITCH = 1.6F;
   private static final float SONIC_BOOM_THUMP_VOLUME = 0.4F;
   private static final float SONIC_BOOM_THUMP_PITCH = 0.6F;
   private static final double GEAR_BURST_BASE = 1.0;
   private static final double GEAR_BURST_PER_GEAR = 0.25;
   public static final int DIVE_TICKS = 5;
   public static final int EXIT_REVEAL_TICKS = 1;
   public static final double WORMHOLE_LIFT = 1.6;
   public static final int WORMHOLE_CLOSE_TICKS = 5;
   private static final double WORMHOLE_SCALE = 1.0;
   private static final float OPEN_BLOOM_VOLUME = 1.0F;
   private static final float OPEN_BLOOM_PITCH = 0.65F;
   private static final float OPEN_CRACKLE_VOLUME = 0.5F;
   private static final float OPEN_CRACKLE_PITCH = 1.8F;
   private static final float CLOSE_VOLUME = 0.8F;
   private static final float CLOSE_PITCH = 1.3F;
   private static final float CLOSE_SNAP_VOLUME = 0.4F;
   private static final float CLOSE_SNAP_PITCH = 1.6F;
   private static final float VANISH_WARP_VOLUME = 1.0F;
   private static final float VANISH_WARP_PITCH = 0.5F;
   private static final float VANISH_CRACKLE_VOLUME = 0.7F;
   private static final float VANISH_CRACKLE_PITCH = 1.4F;
   private static final float EMERGE_THUNDER_VOLUME = 0.9F;
   private static final float EMERGE_THUNDER_PITCH = 1.4F;
   private static final float EMERGE_CHORD_VOLUME = 0.8F;
   private static final float EMERGE_CHORD_PITCH = 0.9F;
   private static final double[] VANISH_ECHO_BACK = new double[]{0.4, 1.0, 1.7};
   private static final double[] EMERGE_ECHO_BACK = new double[]{0.3, 0.6, 0.9};
   private static final double TUNNEL_ECHO_STRETCH = 5.0;
   private static final float BOLT_INTENSITY_BASE = 0.75F;
   private static final float BOLT_INTENSITY_PER_GEAR = 0.05F;
   private static final float BOLT_POWER_INTENSITY = 0.25F;
   private static final float BOLT_POWER_STEPS = 2.0F;
   private static final float BOLT_FULL_POWER = 0.99F;
   private static final double HAND_HALF_WIDTH = 0.31;
   private static final double HAND_DROP = 0.25;
   private static final double HAND_REACH = 0.7;
   private static final double HAND_SIDE = 1.0;
   private static final float THROW_VOLUME = 0.3F;
   private static final float THROW_PITCH = 2.0F;
   private static final float THROW_FULL_VOLUME = 0.5F;
   private static final float THROW_FULL_PITCH = 1.5F;
   private static final float IMPACT_VOLUME = 0.6F;
   private static final float IMPACT_PITCH = 1.5F;

   public static DustParticleOptions dust(VelocityPalette palette) {
      return Fx.dust(palette.body(), 1.2F);
   }

   private VelocityFx() {
   }

   public static void sonicCrack(ServerLevel level, VelocityPalette palette, Vec3 pos, Vec3 dir) {
      Vec3 d = Vfx.safeDir(dir);
      Vfx.effect(VfxEntity.Kind.VELOCITY_SONIC_CRACK, pos)
         .scale(1.0)
         .secondary(d)
         .orient(Vfx.yawAlong(d), Vfx.pitchAlong(d))
         .color(palette.body(), palette.hot())
         .lifetime(6)
         .spawn(level);
      Fx.sound(level, pos, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.9F, 1.7F);
   }

   public static void gearBurst(ServerLevel level, ServerPlayer player, int gear) {
      gearBurst(level, player, gear, gear >= VelocityVisualState.of(player).gear());
   }

   public static void gearBurst(ServerLevel level, ServerPlayer player, int gear, boolean shiftUp) {
      int g = Math.max(0, Math.min(5, gear));
      VelocityPalette palette = VelocityPalette.of(player);
      Vfx.effect(VfxEntity.Kind.VELOCITY_GEAR_BURST, player.position().add(0.0, 0.1, 0.0))
         .scale(1.0 + 0.25 * (double)g)
         .secondary(new Vec3(0.0, shiftUp ? 1.0 : -1.0, 0.0))
         .intensity(0.6F + 0.4F * ((float)g / 5.0F))
         .orient(facingYaw(player), 0.0F)
         .color(palette.body(), palette.hot())
         .lifetime(6)
         .spawn(level);
      Fx.sound(level, player.position(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 0.8F + 0.12F * (float)g);
   }

   public static void sonicBoom(ServerLevel level, ServerPlayer player) {
      Vec3 run = player.getKnownMovement();
      Vec3 d = Vfx.safeDir(new Vec3(run.x, 0.0, run.z));
      sonicBoom(level, VelocityPalette.of(player), player.position().add(0.0, 1.2, 0.0).subtract(d.scale(1.5)), d);
   }

   public static void sonicBoom(ServerLevel level, VelocityPalette palette, Vec3 pos, Vec3 dir) {
      Vec3 d = Vfx.safeDir(dir);
      Vfx.effect(VfxEntity.Kind.VELOCITY_SONIC_BOOM, pos)
         .scale(1.0)
         .secondary(d)
         .orient(Vfx.yawAlong(d), Vfx.pitchAlong(d))
         .color(palette.body(), palette.hot())
         .lifetime(14)
         .spawn(level);
      Fx.sound(level, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.9F, 1.6F);
      Fx.sound(level, pos, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.4F, 0.6F);
   }

   public static void speedEcho(ServerPlayer player) {
      Vec3 run = player.getKnownMovement();
      Vec3 flat = new Vec3(run.x, 0.0, run.z);
      Vec3 back = flat.length() > 1.0E-4 ? flat.normalize().scale(-Math.max(1.4, flat.length() * 3.0)) : Vec3.ZERO;
      speedEcho(player, player.position().add(back), run, 8);
   }

   public static void speedEcho(ServerPlayer player, Vec3 feet, Vec3 velocity, int ticks) {
      ServerLevel level = player.serverLevel();
      SpeedEchoEntity echo = new SpeedEchoEntity(AwakenedEntities.SPEED_ECHO.get(), level);
      echo.setPos(feet.x, feet.y, feet.z);
      echo.configure(
         player.getUUID(),
         player.yBodyRot,
         EchoCadence.stridePhase(player),
         EchoCadence.strideAmount(player),
         velocity,
         Momentum.chosenGear(PowerService.data(player)),
         VelocityVisualState.of(player).colorId(),
         ticks
      );
      level.addFreshEntity(echo);
   }

   public static VfxEntity wormhole(ServerLevel level, VelocityPalette palette, Vec3 feet, Vec3 throatAlong, VelocityFx.WormholeRole role, int ticks) {
      return wormhole(level, palette, feet, throatAlong, role, VelocityFx.WormholeVariant.THROAT, ticks);
   }

   public static VfxEntity wormhole(
      ServerLevel level, VelocityPalette palette, Vec3 feet, Vec3 throatAlong, VelocityFx.WormholeRole role, VelocityFx.WormholeVariant variant, int ticks
   ) {
      Vec3 centre = wormholeCentre(feet);
      Fx.sound(level, centre, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 0.65F);
      Fx.sound(level, centre, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.5F, 1.8F);
      return Vfx.effect(VfxEntity.Kind.VELOCITY_WORMHOLE, centre)
         .scale(1.0)
         .secondary(new Vec3((double)role.code, 0.0, (double)variant.code))
         .orient(Vfx.yawAlong(flat(throatAlong)), 0.0F)
         .color(palette.body(), palette.hot())
         .lifetime(ticks)
         .spawnTracked(level);
   }

   public static Vec3 wormholeCentre(Vec3 feet) {
      return feet.add(0.0, 1.6, 0.0);
   }

   public static void closeWormhole(ServerLevel level, VfxEntity mouth) {
      if (mouth != null && mouth.isAlive()) {
         Vec3 centre = mouth.position();
         Fx.sound(level, centre, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8F, 1.3F);
         Fx.sound(level, centre, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.4F, 1.6F);
         Vfx.effect(VfxEntity.Kind.VELOCITY_WORMHOLE, centre)
            .scale((double)mouth.scale())
            .secondary(new Vec3(0.0, -1.0, mouth.secondary().z))
            .intensity(mouth.intensity())
            .orient(mouth.yaw(), mouth.pitch())
            .color(mouth.color(), mouth.color2())
            .lifetime(5)
            .spawn(level);
         mouth.discard();
      }
   }

   public static void tunnelVanish(ServerLevel level, ServerPlayer player, Vec3 discFeet, Vec3 dir) {
      Vec3 centre = wormholeCentre(discFeet);
      Fx.sound(level, centre, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.5F);
      Fx.sound(level, centre, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.7F, 1.4F);
      trailEchoes(player, discFeet, flat(dir), VANISH_ECHO_BACK);
   }

   public static void tunnelEmerge(ServerLevel level, ServerPlayer player, Vec3 discFeet, Vec3 feet, Vec3 heading) {
      Vec3 centre = wormholeCentre(discFeet);
      Fx.sound(level, centre, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.9F, 1.4F);
      Fx.sound(level, centre, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8F, 0.9F);
      trailEchoes(player, feet, flat(heading), EMERGE_ECHO_BACK);
   }

   private static void trailEchoes(ServerPlayer player, Vec3 from, Vec3 dir, double[] backs) {
      Vec3 stretch = dir.scale(5.0);

      for (double back : backs) {
         speedEcho(player, from.subtract(dir.scale(back)), stretch, 8);
      }
   }

   private static Vec3 flat(Vec3 dir) {
      return Vfx.safeDir(dir == null ? null : new Vec3(dir.x, 0.0, dir.z));
   }

   public static Vec3 throwHand(ServerPlayer player) {
      return AimUtil.shoulderHand(player.getEyePosition(), (double)player.getYRot(), (double)player.getXRot(), 1.0, 0.31, 0.25, 0.7);
   }

   public static void throwBolt(ServerLevel level, VelocityPalette palette, Vec3 from, Vec3 to, int gear, float power) {
      staticBolt(level, palette, from, to, gear, power);
      boltImpact(level, palette, to, to.subtract(from), gear, power);
   }

   public static void staticBolt(ServerLevel level, VelocityPalette palette, Vec3 from, Vec3 to, int gear, float power) {
      boltTrunk(level, palette, from, to, gear, power);
      boolean full = clampPower(power) >= 0.99F;
      Fx.sound(level, from, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, full ? 0.5F : 0.3F, full ? 1.5F : 2.0F);
   }

   public static void chainBolt(ServerLevel level, VelocityPalette palette, Vec3 from, Vec3 to, int gear, float power) {
      boltTrunk(level, palette, from, to, gear, power);
      boltImpact(level, palette, to, to.subtract(from), gear, power);
   }

   private static void boltTrunk(ServerLevel level, VelocityPalette palette, Vec3 from, Vec3 to, int gear, float power) {
      int g = clampGear(gear);
      float c = clampPower(power);
      Vec3 span = to.subtract(from);
      Vec3 d = Vfx.safeDir(span);
      Vfx.effect(VfxEntity.Kind.VELOCITY_BOLT, from)
         .scale(boltSteps(g, c))
         .secondary(span)
         .intensity(boltIntensity(g, c))
         .orient(Vfx.yawAlong(d), Vfx.pitchAlong(d))
         .color(palette.body(), palette.hot())
         .lifetime(7)
         .spawn(level);
   }

   public static void boltImpact(ServerLevel level, VelocityPalette palette, Vec3 pos, Vec3 dir, int gear, float power) {
      int g = clampGear(gear);
      float c = clampPower(power);
      Vec3 d = Vfx.safeDir(dir);
      Vfx.effect(VfxEntity.Kind.VELOCITY_BOLT_IMPACT, pos)
         .scale(boltSteps(g, c))
         .secondary(d)
         .intensity(boltIntensity(g, c))
         .orient(Vfx.yawAlong(d), Vfx.pitchAlong(d))
         .color(palette.body(), palette.hot())
         .lifetime(8)
         .delay(2)
         .spawn(level);
   }

   public static void boltImpactCrack(ServerLevel level, Vec3 pos) {
      Fx.sound(level, pos, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.6F, 1.5F);
   }

   public static int boltTravelTicks() {
      return 2;
   }

   private static int clampGear(int gear) {
      return Math.max(0, Math.min(5, gear));
   }

   private static float clampPower(float power) {
      return Math.max(0.0F, Math.min(1.0F, power));
   }

   private static double boltSteps(int gear, float power) {
      return (double)((float)gear + 2.0F * power);
   }

   private static float boltIntensity(int gear, float power) {
      return (0.75F + 0.05F * (float)gear) * (1.0F + 0.25F * power);
   }

   public static float ringFront(float t) {
      float u = 1.0F - Math.max(0.0F, Math.min(1.0F, t));
      return 1.0F - u * u;
   }

   public static void timelessOpen(ServerPlayer player, double radius) {
      timeRing(player.serverLevel(), VelocityPalette.of(player), player.position(), radius, true, 0);
   }

   public static void timelessClose(ServerPlayer player, double radius) {
      timeRing(player.serverLevel(), VelocityPalette.of(player), player.position(), radius, false, 0);
   }

   public static VfxEntity timeRing(ServerLevel level, VelocityPalette palette, Vec3 feet, double radius, boolean opening, int delay) {
      return Vfx.effect(VfxEntity.Kind.VELOCITY_TIME_RING, feet)
         .scale(radius)
         .secondary(new Vec3(0.0, opening ? 1.0 : -1.0, 0.0))
         .color(palette.body(), palette.hot())
         .lifetime(8)
         .delay(delay)
         .spawnTracked(level);
   }

   public static float facingYaw(ServerPlayer player) {
      return -player.getYRot();
   }

   public static enum WormholeRole {
      PLAIN(0.0F),
      ENTRY(1.0F),
      EXIT(-1.0F);

      public final float code;

      private WormholeRole(float code) {
         this.code = code;
      }

      public static VelocityFx.WormholeRole of(float code) {
         return code > 0.5F ? ENTRY : (code < -0.5F ? EXIT : PLAIN);
      }
   }

   public static enum WormholeVariant {
      CLASSIC(0.0F),
      THROAT(1.0F);

      public final float code;

      private WormholeVariant(float code) {
         this.code = code;
      }

      public static VelocityFx.WormholeVariant of(float code) {
         return code > 0.5F ? THROAT : CLASSIC;
      }
   }
}
