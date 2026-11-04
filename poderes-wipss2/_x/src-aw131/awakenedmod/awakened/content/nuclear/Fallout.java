package com.awakenedmod.awakened.content.nuclear;

import com.awakenedmod.awakened.content.AwakenedParticles;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Harm;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class Fallout {
   private static final int PULSE_INTERVAL = 15;
   private static final int POISON_TICKS = 60;
   private static final List<Fallout.Cloud> CLOUDS = new ArrayList<>();
   private static int lastTick = -1;

   private Fallout() {
   }

   public static void add(ServerPlayer source, ServerLevel level, Vec3 center, double radius, int durationTicks, float damagePerPulse) {
      Fallout.Cloud z = new Fallout.Cloud();
      z.level = level;
      z.center = center;
      z.radius = radius;
      z.expiry = level.getGameTime() + (long)durationTicks;
      z.damagePerPulse = damagePerPulse;
      z.source = source.getUUID();
      CLOUDS.add(z);
   }

   public static void tickAll(MinecraftServer server) {
      int now = server.getTickCount();
      if (now != lastTick) {
         lastTick = now;
         Iterator<Fallout.Cloud> it = CLOUDS.iterator();

         while (it.hasNext()) {
            Fallout.Cloud z = it.next();
            if (z.level.getGameTime() >= z.expiry) {
               it.remove();
            } else {
               renderHaze(z, now);
               if (now % 15 == 0) {
                  pulse(z);
               }
            }
         }
      }
   }

   private static void renderHaze(Fallout.Cloud z, int now) {
      Vec3 base = z.center.add(0.0, 0.3, 0.0);
      if (now % 2 == 0) {
         NuclearFx.scatter(z.level, AwakenedParticles.FALLOUT, base, z.radius * 0.5, 2, z.radius * 0.75);
      }

      if (now % 10 == 0) {
         NuclearFx.scatter(z.level, AwakenedParticles.SMOKE, base, z.radius * 0.4, 1, z.radius * 0.5);
      }
   }

   private static void pulse(Fallout.Cloud z) {
      AABB box = new AABB(z.center, z.center).inflate(z.radius, 2.0, z.radius);
      ServerPlayer src = sourcePlayer(z.level, z.source);

      for (LivingEntity victim : z.level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e.distanceToSqr(z.center) <= z.radius * z.radius)) {
         if (!victim.getUUID().equals(z.source)) {
            Dmg.hurt(victim, z.level, magicFrom(z.level, z.source), z.damagePerPulse);
            Harm.effect(src, victim, new MobEffectInstance(MobEffects.POISON, 60, 1));
            Harm.effect(src, victim, new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
         }
      }
   }

   private static DamageSource magicFrom(ServerLevel level, UUID source) {
      ServerPlayer src = sourcePlayer(level, source);
      return src != null ? Dmg.by(src, DamageTypes.MAGIC) : level.damageSources().magic();
   }

   private static ServerPlayer sourcePlayer(ServerLevel level, UUID source) {
      return source == null ? null : level.getServer().getPlayerList().getPlayer(source);
   }

   private static final class Cloud {
      private ServerLevel level;
      private Vec3 center;
      private double radius;
      private long expiry;
      private float damagePerPulse;
      private UUID source;
   }
}
