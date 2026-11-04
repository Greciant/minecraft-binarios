package com.awakenedmod.awakened.content.cryokinetic;

import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.ParticleTypes;
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

public final class Frostbite {
   public static final int MAX_STACKS = 5;
   private static final int LIFETIME = 100;
   private static final int CHIP_INTERVAL = 20;
   private static final float CHIP_PER_STACK = 0.5F;
   static final String BALANCE_KEY = "CryokineticFrostbite";
   private static final int SLOW_TICKS = 30;
   private static final Map<LivingEntity, Frostbite.Mark> MARKED = new ConcurrentHashMap<>();
   private static int lastTick = -1;

   private Frostbite() {
   }

   public static void apply(ServerPlayer source, LivingEntity target, int add) {
      if (target != source && target.isAlive() && !target.isRemoved()) {
         if (Harm.allowed(source, target)) {
            Frostbite.Mark m = MARKED.computeIfAbsent(target, t -> new Frostbite.Mark());
            m.stacks = Math.min(5, m.stacks + add);
            m.expiry = target.level().getGameTime() + 100L;
            m.source = source.getUUID();
            chill(source, target, m.stacks);
         }
      }
   }

   public static void applyNear(ServerPlayer source, ServerLevel level, Vec3 center, double radius, int add) {
      for (LivingEntity target : level.getEntitiesOfClass(
         LivingEntity.class, new AABB(center, center).inflate(radius), e -> e != source && e.isAlive() && e.distanceToSqr(center) <= radius * radius
      )) {
         apply(source, target, add);
      }
   }

   public static int stacks(LivingEntity target) {
      Frostbite.Mark m = MARKED.get(target);
      return m == null ? 0 : m.stacks;
   }

   public static int consume(LivingEntity target) {
      Frostbite.Mark m = MARKED.remove(target);
      return m == null ? 0 : m.stacks;
   }

   private static void chill(ServerPlayer source, LivingEntity t, int stacks) {
      Harm.effect(source, t, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, Math.min(4, stacks - 1), false, false));
      if (Harm.controls(source, t)) {
         frostAtLeast(t, t.getTicksRequiredToFreeze() * stacks / 5);
      }
   }

   static void frostAtLeast(LivingEntity t, int ticks) {
      int target = Math.min(t.getTicksRequiredToFreeze(), ticks);
      if (t.getTicksFrozen() < target) {
         t.setTicksFrozen(target);
      }
   }

   public static void tickAll(MinecraftServer server) {
      int now = server.getTickCount();
      if (now != lastTick) {
         lastTick = now;
         Iterator<Entry<LivingEntity, Frostbite.Mark>> it = MARKED.entrySet().iterator();

         while (it.hasNext()) {
            Entry<LivingEntity, Frostbite.Mark> e = it.next();
            LivingEntity t = e.getKey();
            Frostbite.Mark m = e.getValue();
            if (t.isAlive() && !t.isRemoved() && t.level() instanceof ServerLevel level && level.getGameTime() < m.expiry) {
               Vec3 c = t.position().add(0.0, (double)t.getBbHeight() * 0.5, 0.0);
               Fx.burst(level, ParticleTypes.SNOWFLAKE, c, m.stacks, (double)t.getBbWidth() * 0.6 + 0.1, 0.01);
               ServerPlayer src = m.source == null ? null : level.getServer().getPlayerList().getPlayer(m.source);
               chill(src, t, m.stacks);
               if (now % 20 == 0) {
                  float chip = 0.5F * (float)m.stacks;
                  chip = src != null
                     ? MasteryScaling.scaledDamage(chip, src, "CryokineticFrostbite")
                     : MasteryScaling.configScaledDamage(chip, "CryokineticFrostbite");
                  Dmg.hurt(t, level, freezeFrom(level, m.source), chip);
               }
            } else {
               it.remove();
            }
         }
      }
   }

   private static DamageSource freezeFrom(ServerLevel level, UUID source) {
      ServerPlayer src = source == null ? null : level.getServer().getPlayerList().getPlayer(source);
      return src != null ? Dmg.by(src, DamageTypes.FREEZE) : level.damageSources().freeze();
   }

   private static final class Mark {
      private int stacks;
      private long expiry;
      private UUID source;
   }
}
