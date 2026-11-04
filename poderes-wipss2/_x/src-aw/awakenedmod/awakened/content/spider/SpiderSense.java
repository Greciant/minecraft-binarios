package com.awakenedmod.awakened.content.spider;

import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.net.SpiderSenseTinglePayload;
import java.util.Optional;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class SpiderSense {
   private static final int SCAN_INTERVAL = 4;
   private static final double BASE_HORIZON = 8.0;
   private static final double HORIZON_PER_LEVEL = 1.2;
   private static final double MAX_HORIZON = 20.0;
   private static final int DIRECTIONAL_LEVEL = 2;
   private static final int PROJECTILE_LOOKAHEAD = 20;
   private static final String LAST_KIND = "spider.sense.kind";
   private static final String LAST_SIDE = "spider.sense.side";
   private static final String LAST_DIRECTIONAL = "spider.sense.dir";

   private SpiderSense() {
   }

   public static void tick(ServerPlayer player, PowerData data) {
      if (player.tickCount % 4 == 0) {
         ServerLevel level = player.serverLevel();
         int mastery = data.masteryLevel();
         double horizon = Math.min(20.0, 8.0 + 1.2 * (double)mastery);
         boolean directional = mastery >= 2;
         SpiderSense.Threat threat = scan(player, level, horizon);
         int kind = threat == null ? 0 : threat.kind;
         int side = threat == null ? -1 : Mth.positiveModulo(Math.round(threat.bearingDeg / 45.0F), 8);
         int lastKind = Math.round(data.var("spider.sense.kind", 0.0F));
         int lastSide = Math.round(data.var("spider.sense.side", -1.0F));
         boolean lastDirectional = data.var("spider.sense.dir", 0.0F) > 0.5F;
         boolean changed = kind != lastKind || directional && side != lastSide || directional != lastDirectional;
         data.setVar("spider.sense.kind", (float)kind);
         data.setVar("spider.sense.side", (float)side);
         data.setVar("spider.sense.dir", directional ? 1.0F : 0.0F);
         if (threat != null && changed) {
            PacketDistributor.sendToPlayer(
               player,
               new SpiderSenseTinglePayload(directional ? Math.round(threat.bearingDeg) : 0, threat.intensity, directional, threat.kind, mastery),
               new CustomPacketPayload[0]
            );
         }
      }
   }

   private static SpiderSense.Threat scan(ServerPlayer player, ServerLevel level, double horizon) {
      AABB area = player.getBoundingBox().inflate(horizon);
      Vec3 eye = player.getEyePosition();
      AABB hit = player.getBoundingBox().inflate(0.6);
      SpiderSense.Threat best = null;

      for (Mob mob : level.getEntitiesOfClass(Mob.class, area, m -> m.getTarget() == player)) {
         double dist = Math.sqrt(mob.distanceToSqr(player));
         float intensity = intensityFromDistance(dist, horizon);
         best = pick(best, new SpiderSense.Threat(1, bearing(player, mob.position()), intensity));
      }

      for (Projectile proj : level.getEntitiesOfClass(Projectile.class, area, p -> p.getOwner() != player)) {
         Vec3 start = proj.position();
         Vec3 vel = proj.getDeltaMovement();
         if (!(vel.lengthSqr() < 1.0E-4)) {
            Vec3 end = start.add(vel.scale(20.0));
            Optional<Vec3> clip = hit.clip(start, end);
            if (!clip.isEmpty()) {
               double ticksAway = start.distanceTo(clip.get()) / Math.sqrt(vel.lengthSqr());
               float intensity = intensityFromTime(ticksAway);
               best = pick(best, new SpiderSense.Threat(2, bearing(player, start), intensity));
            }
         }
      }

      for (Creeper creeper : level.getEntitiesOfClass(Creeper.class, area, c -> c.getSwellDir() > 0 || c.isIgnited())) {
         double dist = Math.sqrt(creeper.distanceToSqr(player));
         float intensity = Math.min(1.0F, intensityFromDistance(dist, horizon) + 0.25F);
         best = pick(best, new SpiderSense.Threat(3, bearing(player, creeper.position()), intensity));
      }

      return best;
   }

   private static SpiderSense.Threat pick(SpiderSense.Threat a, SpiderSense.Threat b) {
      return a != null && !(b.intensity > a.intensity) ? a : b;
   }

   private static float intensityFromDistance(double dist, double horizon) {
      return Mth.clamp(1.0F - (float)(dist / horizon), 0.15F, 1.0F);
   }

   private static float intensityFromTime(double ticksAway) {
      return Mth.clamp(1.0F - (float)(ticksAway / 20.0), 0.2F, 1.0F);
   }

   private static float bearing(ServerPlayer player, Vec3 threatPos) {
      Vec3 to = threatPos.subtract(player.position());
      double yawToThreat = Math.toDegrees(Math.atan2(-to.x, to.z));
      return Mth.wrapDegrees((float)(yawToThreat - (double)player.getYRot()));
   }

   private static record Threat(int kind, float bearingDeg, float intensity) {
   }
}
