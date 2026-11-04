package com.awakenedmod.awakened.content.exosavant;

import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.power.Ability;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class ScanPulseAbility extends Ability {
   private static final int UNLOCK = 7;
   private static final float COST = 16.0F;
   private static final double RANGE = 48.0;
   private static final double WAVE_VISUAL_RADIUS = 9.0;
   private static final int WAVE_LIFETIME = 24;
   private static final int FLASH_LIFETIME = 8;
   private static final int PAINT_LIFETIME = 8;
   private static final double PAINT_CONE_DEG = 62.0;
   private static final int PAINT_RINGS = 9;
   private static final double SPLAT_BASE_RADIUS = 0.9;
   private static final double SPLAT_PER_BLOCK = 0.05;
   private static final double SPLAT_MAX_RADIUS = 3.0;
   private static final float SPLAT_INTENSITY = 0.7F;
   private static final float FRONT_RING_INTENSITY = 0.5F;
   private final Map<UUID, Long> sweepingUntil = this.playerState();

   public ScanPulseAbility() {
      super("scan_pulse", 16.0F, 7);
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (!ExoLoadout.usable(player, ExoPiece.LEGGINGS)) {
         return false;
      } else {
         long now = player.serverLevel().getGameTime();
         Long sweeping = this.sweepingUntil.get(player.getUUID());
         if (sweeping != null && now < sweeping) {
            return this.fail(player, "awakened.exosavant.scan_cooling", new Object[0]);
         } else {
            ServerLevel level = player.serverLevel();
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle().normalize();
            int contacts = 0;
            double gather = 58.0;

            for (LivingEntity foe : AimUtil.livingAround(player, eye, gather)) {
               if (ExoTags.taggable(foe, player)) {
                  Vec3 to = foe.getBoundingBox().getCenter().subtract(eye);
                  double along = to.dot(look);
                  if (!(along < 0.0) && !(along > 48.0)) {
                     double perpDist = to.subtract(look.scale(along)).length();
                     if (!(perpDist > 9.0 + (double)foe.getBbWidth() * 0.5)) {
                        ExoTags.tag(player, foe);
                        ScanReveal.reveal(player, foe);
                        int delay = (int)Math.round(Math.min(1.0, to.length() / 48.0) * 24.0);
                        Vfx.sphere(level, foe.getBoundingBox().getCenter(), 0.7, 14692909, 8, 1.5F, true, delay);
                        contacts++;
                     }
                  }
               }
            }

            this.paintSurfaces(level, player, eye, look);
            Vfx.effect(VfxEntity.Kind.RING, player.position()).scale(24.0).color(4247807).lifetime(24).intensity(0.5F).noDepth(true).spawn(level);
            Fx.sound(level, player.position(), SoundEvents.CONDUIT_AMBIENT, SoundSource.PLAYERS, 0.9F, 1.5F);
            Fx.sound(level, player.position(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.5F, 1.8F);
            if (contacts > 0) {
               player.displayClientMessage(
                  Component.literal("SCAN // " + contacts + (contacts == 1 ? " THREAT DETECTED" : " THREATS DETECTED"))
                     .withStyle(s -> s.withColor(TextColor.fromRgb(14692909))),
                  true
               );
               Fx.sound(level, player.position(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.7F, 1.6F);
            } else {
               player.displayClientMessage(Component.literal("SCAN // NO THREATS").withStyle(ChatFormatting.GRAY), true);
            }

            this.sweepingUntil.put(player.getUUID(), now + 24L);
            return true;
         }
      }
   }

   private void paintSurfaces(ServerLevel level, ServerPlayer player, Vec3 eye, Vec3 look) {
      Vec3 up0 = Math.abs(look.y) > 0.95 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
      Vec3 right = look.cross(up0).normalize();
      Vec3 up = right.cross(look).normalize();
      double coneRad = Math.toRadians(62.0);
      double polarStep = coneRad / 8.0;

      for (int ring = 0; ring < 9; ring++) {
         double theta = polarStep * (double)ring;
         int az = Math.max(1, (int)Math.round((Math.PI * 2) * Math.sin(theta) / polarStep));
         double phiOffset = (double)(ring % 2) * Math.PI / (double)az;

         for (int a = 0; a < az; a++) {
            double phi = (Math.PI * 2) * (double)a / (double)az + phiOffset;
            Vec3 dir = look.scale(Math.cos(theta)).add(right.scale(Math.sin(theta) * Math.cos(phi))).add(up.scale(Math.sin(theta) * Math.sin(phi))).normalize();
            Vec3 to = eye.add(dir.scale(48.0));
            BlockHitResult hit = level.clip(new ClipContext(eye, to, Block.COLLIDER, Fluid.NONE, player));
            if (hit.getType() == Type.BLOCK) {
               double dist = eye.distanceTo(hit.getLocation());
               int delay = (int)Math.round(Math.min(1.0, dist / 48.0) * 24.0);
               Direction face = hit.getDirection();
               float splatYaw;
               float splatPitch;
               if (face.getAxis() == Axis.Y) {
                  splatYaw = 0.0F;
                  splatPitch = 0.0F;
               } else {
                  splatYaw = (float)Math.toDegrees(Math.atan2((double)face.getStepX(), (double)face.getStepZ()));
                  splatPitch = 90.0F;
               }

               Vec3 at = hit.getLocation().add((double)face.getStepX() * 0.02, (double)face.getStepY() * 0.02, (double)face.getStepZ() * 0.02);
               double splatR = Math.max(0.9, Math.min(3.0, 0.9 + 0.05 * dist));
               Vfx.effect(VfxEntity.Kind.SPLAT, at)
                  .scale(splatR)
                  .color(4247807)
                  .lifetime(8)
                  .intensity(0.7F)
                  .orient(splatYaw, splatPitch)
                  .noDepth(true)
                  .delay(delay)
                  .spawn(level);
            }
         }
      }
   }
}
