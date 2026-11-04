package com.awakenedmod.awakened.content.kineticcharger;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.ForceBlast;
import com.awakenedmod.awakened.core.Lab;
import com.awakenedmod.awakened.core.PowerService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class RecoilShotAbility extends KineticChargedStrike {
   public static final String ID = "recoil_shot";
   private static final int UNLOCK = 3;
   private static final float COST = 10.0F;
   private static final int WINDUP_TICKS = 2;
   private static final int SAFETY_TICKS = 1200;
   private static final float MAX_SPEND = 30.0F;
   private static final String CHARGE_CLIP = "recoil_shot_charge";
   private static final String FIRE_CLIP = "recoil_shot_fire";
   public static final Lab.Value BLAST_LEAD = Lab.SERVER.def("kinetic.recoil.lead", 2.0, 0.5, 8.0, 0.1);
   public static final Lab.Value RADIUS_MIN = Lab.SERVER.def("kinetic.recoil.radius_min", 2.0, 0.5, 8.0, 0.1);
   public static final Lab.Value RADIUS = Lab.SERVER.def("kinetic.recoil.radius", 3.0, 0.5, 8.0, 0.1);
   public static final Lab.Value RECOIL_MIN = Lab.SERVER.def("kinetic.recoil.speed_min", 1.25, 0.1, 4.0, 0.05);
   public static final Lab.Value RECOIL = Lab.SERVER.def("kinetic.recoil.speed", 2.7, 0.1, 4.0, 0.05);
   private static final float DAMAGE_BASE = 2.0F;
   private static final float DAMAGE_PER_CHARGE = 0.1F;
   private static final double KNOCK_OUT_BASE = 0.8;
   private static final double KNOCK_OUT_SPENT = 0.9;
   private static final double KNOCK_UP = 0.25;
   private static final double FLAT_LIFT = 0.45;
   private static final double LOOK_FLAT = 0.3;
   private static final int FLIGHT_CAP_TICKS = 400;
   private static final int AIRBORNE_BEFORE_LANDING = 2;
   private static final ResourceLocation FALL_SAFE_ID = AwId.of("kinetic_recoil_safe_fall");
   private static final ResourceLocation FALL_MULT_ID = AwId.of("kinetic_recoil_fall");
   private final Map<UUID, RecoilShotAbility.Flight> flights = this.playerState();
   private final Map<UUID, Boolean> airShotSpent = this.playerState();

   public RecoilShotAbility() {
      super("recoil_shot", 10.0F, 3, 2, 30.0F, 1200);
   }

   @Override
   protected int swingTicks() {
      return 0;
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return "recoil_shot_charge";
   }

   @Override
   public boolean requiresFootingWhenRealistic() {
      return false;
   }

   @Override
   protected boolean canBegin(ServerPlayer player) {
      return !player.onGround() && this.airShotSpent.containsKey(player.getUUID())
         ? this.fail(player, "awakened.kinetic_charger.no_air_shot", new Object[0])
         : true;
   }

   @Override
   protected void windupFx(ServerPlayer player, float progress) {
      KineticFx.recoilWindup(player, progress);
   }

   @Override
   protected void pourFx(ServerPlayer player, int ticks, float fill) {
      KineticFx.recoilPour(player, ticks, fill);
   }

   @Override
   protected void releaseFx(ServerPlayer player) {
      PowerService.playAnimation(player, "recoil_shot_fire");
   }

   @Override
   protected void strike(ServerPlayer player, float spent) {
      ServerLevel level = player.serverLevel();
      float fill = this.fillOf(spent);
      Vec3 dir = player.getLookAngle().normalize();
      Vec3 muzzle = KineticFx.palms(player);
      BlockHitResult wall = AimUtil.clipBlocks(level, player, muzzle, muzzle.add(dir.scale(BLAST_LEAD.get())));
      Vec3 center = ventPoint(muzzle, dir, wall);
      double radius = radiusFor(fill);
      float damage = KineticBank.empoweredDamage(player, 2.0F + spent * 0.1F, this.balanceKey());
      double out = KineticBank.empowered(player, 0.8 + (double)fill * 0.9);
      List<LivingEntity> caught = ForceBlast.shockwave(player, level, center, radius, damage, out, 0.25);
      boolean connected = wall.getType() != Type.MISS || !caught.isEmpty();
      if (!connected && !player.onGround()) {
         this.airShotSpent.put(player.getUUID(), Boolean.TRUE);
      } else {
         this.airShotSpent.remove(player.getUUID());
      }

      KineticFx.recoilFire(player, muzzle, center, dir, radius, fill, !caught.isEmpty());
      this.kick(player, dir, fill);
   }

   private static Vec3 ventPoint(Vec3 muzzle, Vec3 dir, BlockHitResult wall) {
      return wall.getType() == Type.MISS ? muzzle.add(dir.scale(BLAST_LEAD.get())) : wall.getLocation().subtract(dir.scale(0.3));
   }

   private void kick(ServerPlayer player, Vec3 dir, float fill) {
      double speed = KineticBank.boosted(player, RECOIL_MIN.get(), (RECOIL.get() - RECOIL_MIN.get()) * (double)fill);
      Vec3 back = dir.scale(-speed);
      if (Math.abs(dir.y) < 0.3) {
         back = back.add(0.0, 0.45, 0.0);
      }

      player.setDeltaMovement(back);
      player.hurtMarked = true;
      this.flights.put(player.getUUID(), new RecoilShotAbility.Flight());
   }

   public static double radiusFor(float fill) {
      double min = RADIUS_MIN.get();
      return min + (RADIUS.get() - min) * (double)Math.min(1.0F, Math.max(0.0F, fill));
   }

   @Override
   protected void tickAftermath(ServerPlayer player) {
      RecoilShotAbility.Flight flight = this.flights.get(player.getUUID());
      if (flight != null) {
         KineticAttrs.fallImmune(player, FALL_SAFE_ID, FALL_MULT_ID, true);
         if (!player.onGround()) {
            flight.airborne++;
         }

         boolean landed = flight.airborne >= 2 && player.onGround();
         if (landed || ++flight.ticks >= 400) {
            this.land(player);
         }
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      this.land(player);
      super.clearPlayer(player);
   }

   private void land(ServerPlayer player) {
      this.flights.remove(player.getUUID());
      this.airShotSpent.remove(player.getUUID());
      KineticAttrs.fallImmune(player, FALL_SAFE_ID, FALL_MULT_ID, false);
   }

   private static final class Flight {
      private int ticks;
      private int airborne;
   }
}
