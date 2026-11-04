package com.devilfruits.core.logia;

import com.devilfruits.core.network.LogiaHitPayload;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class LogiaHits {
   private static final Map<ServerPlayer, List<LogiaHitPayload.Hit>> PENDING = new IdentityHashMap<>();

   public static void add(ServerPlayer victim, DamageSource source, LogiaDamage.Decision decision) {
      Entity direct = source.getDirectEntity();
      Entity attacker = source.getEntity();
      Vec3 motion = null;
      Vec3 from;
      float size;
      if (direct != null && direct != attacker) {
         from = direct.position();
         motion = direct.getDeltaMovement();
         size = direct.getBbWidth();
      } else if (attacker instanceof LivingEntity living) {
         from = living.getEyePosition();
         motion = living.getLookAngle();
         size = living.getMainHandItem().isEmpty() ? 0.75F : 1.15F;
      } else if (source.getSourcePosition() != null) {
         from = source.getSourcePosition();
         size = 2.0F;
      } else {
         from = null;
         size = 0.5F;
      }

      add(victim, from, motion, decision, direct != null ? direct.getId() : (attacker != null ? attacker.getId() : -1), size);
   }

   public static void add(ServerPlayer victim, @Nullable Vec3 from, @Nullable Vec3 motion, LogiaDamage.Decision decision, int attackerId) {
      add(victim, from, motion, decision, attackerId, 0.5F);
   }

   public static void add(ServerPlayer victim, @Nullable Vec3 from, @Nullable Vec3 motion, LogiaDamage.Decision decision, int attackerId, float size) {
      AABB box = victim.getBoundingBox();
      Vec3 centre = box.getCenter();
      Vec3 dir;
      Vec3 pos;
      if (from == null) {
         dir = new Vec3(0.0, -1.0, 0.0);
         pos = centre;
      } else {
         dir = motion != null && motion.lengthSqr() > 1.0E-4 ? motion.normalize() : centre.subtract(from).normalize();
         Vec3 end = from.add(dir.scale(from.distanceTo(centre) + 2.0));
         pos = (Vec3)box.clip(from, end).orElse(null);
         if (pos == null) {
            dir = centre.subtract(from).normalize();
            pos = box.clip(from, from.add(dir.scale(from.distanceTo(centre) + 2.0))).orElse(clamp(box, from));
         }
      }

      List<LogiaHitPayload.Hit> hits = PENDING.computeIfAbsent(victim, k -> new ArrayList<>(2));
      if (hits.size() < 4) {
         int kind = decision.bypass() == LogiaBypassSource.HAKI ? 2 : (decision.rule() == HitRule.PARTIAL ? 1 : 0);
         hits.add(new LogiaHitPayload.Hit(pos, dir, decision.category().ordinal(), kind, attackerId, size));
      }
   }

   private static Vec3 clamp(AABB box, Vec3 p) {
      return new Vec3(Math.max(box.minX, Math.min(box.maxX, p.x)), Math.max(box.minY, Math.min(box.maxY, p.y)), Math.max(box.minZ, Math.min(box.maxZ, p.z)));
   }

   public static void flush() {
      if (!PENDING.isEmpty()) {
         for (Entry<ServerPlayer, List<LogiaHitPayload.Hit>> e : PENDING.entrySet()) {
            ServerPlayer victim = e.getKey();
            if (!victim.isRemoved()) {
               PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                  victim, new LogiaHitPayload(victim.getId(), List.copyOf(e.getValue())), new CustomPacketPayload[0]
               );
            }
         }

         PENDING.clear();
      }
   }

   private LogiaHits() {
   }
}
