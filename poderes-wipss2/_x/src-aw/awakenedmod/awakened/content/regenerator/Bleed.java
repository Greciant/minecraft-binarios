package com.awakenedmod.awakened.content.regenerator;

import com.awakenedmod.awakened.content.AwakenedEffects;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class Bleed {
   public static final int MAX_STACKS = 5;
   private static final int LIFETIME = 120;
   private static final int TICK_INTERVAL = 20;
   private static final float DAMAGE_PER_STACK = 1.0F;
   static final String BALANCE_KEY = "RegeneratorBleed";
   private static final float HEAL_PER_STACK = 0.6F;
   private static final DustParticleOptions BLOOD = Fx.dust(11541294, 1.4F);
   private static final ResourceKey<DamageType> BLEED_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, AwId.of("bleed"));
   private static final Map<LivingEntity, Bleed.Mark> BLEEDING = new ConcurrentHashMap<>();
   private static int lastTick = -1;
   private static boolean applyingTick = false;

   private Bleed() {
   }

   public static boolean isApplyingTick() {
      return applyingTick;
   }

   public static void apply(ServerPlayer source, LivingEntity target, int add) {
      if (target != source) {
         mark(source.getUUID(), target, add);
         notifyAttacker(source, target);
      }
   }

   public static void inflictSelf(ServerPlayer player, int add) {
      mark(player.getUUID(), player, add);
   }

   private static void mark(UUID source, LivingEntity target, int add) {
      if (target.isAlive() && !target.isRemoved() && add > 0) {
         Bleed.Mark m = BLEEDING.computeIfAbsent(target, t -> new Bleed.Mark());
         m.stacks = Math.min(5, m.stacks + add);
         m.expiry = target.level().getGameTime() + 120L;
         m.source = source;
         target.addEffect(new MobEffectInstance(AwakenedEffects.BLEED, 120, m.stacks - 1, false, false, true));
         if (target.level() instanceof ServerLevel level) {
            spray(level, target, 4 + m.stacks * 3);
         }

         notifyVictim(target, m.stacks);
      }
   }

   public static int stacks(LivingEntity target) {
      Bleed.Mark m = BLEEDING.get(target);
      return m == null ? 0 : m.stacks;
   }

   public static void tickAll(MinecraftServer server) {
      int now = server.getTickCount();
      if (now != lastTick) {
         lastTick = now;
         boolean payout = now % 20 == 0;
         Iterator<Entry<LivingEntity, Bleed.Mark>> it = BLEEDING.entrySet().iterator();

         while (it.hasNext()) {
            Entry<LivingEntity, Bleed.Mark> e = it.next();
            LivingEntity target = e.getKey();
            Bleed.Mark m = e.getValue();
            if (target.isAlive() && !target.isRemoved() && target.level() instanceof ServerLevel level && level.getGameTime() < m.expiry) {
               spray(level, target, 1 + m.stacks * 2);
               if (payout) {
                  ServerPlayer src = m.source == null ? null : level.getServer().getPlayerList().getPlayer(m.source);
                  DamageSource damageSource = bleedDamage(level, src);
                  float tickDamage = 1.0F * (float)m.stacks;
                  if (src != null) {
                     tickDamage = MasteryScaling.scaledDamage(tickDamage, src, "RegeneratorBleed");
                  } else {
                     tickDamage = MasteryScaling.configScaledDamage(tickDamage, "RegeneratorBleed");
                  }

                  applyingTick = true;

                  try {
                     Dmg.hurt(target, level, damageSource, tickDamage);
                  } finally {
                     applyingTick = false;
                  }

                  if (src != null && !target.isAlive() && Foes.defeatedBy(target, src)) {
                     QuestSystem.feat(src, "regen_bleed_out");
                  }

                  if (src != null && src.isAlive() && src.getHealth() < src.getMaxHealth()) {
                     src.heal(0.6F * (float)m.stacks);
                  }
               }
            } else {
               if (target.isAlive() && !target.isRemoved()) {
                  target.removeEffect(AwakenedEffects.BLEED);
               }

               it.remove();
            }
         }
      }
   }

   private static DamageSource bleedDamage(ServerLevel level, ServerPlayer src) {
      Holder<DamageType> type = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(BLEED_TYPE);
      return new DamageSource(type, src, src);
   }

   private static void spray(ServerLevel level, LivingEntity target, int count) {
      Vec3 c = target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0);
      Fx.burst(level, BLOOD, c, count, (double)target.getBbWidth() * 0.5 + 0.1, 0.02);
   }

   private static void notifyVictim(LivingEntity target, int stacks) {
      if (target instanceof ServerPlayer victim) {
         victim.displayClientMessage(Component.translatable("awakened.bleed.afflicted", new Object[]{stacks, dps(stacks), seconds()}), true);
      }
   }

   private static void notifyAttacker(ServerPlayer attacker, LivingEntity target) {
      int stacks = stacks(target);
      if (stacks > 0) {
         attacker.displayClientMessage(Component.translatable("awakened.bleed.inflicted", new Object[]{stacks, dps(stacks), seconds()}), true);
      }
   }

   private static int dps(int stacks) {
      return Math.round(1.0F * (float)stacks);
   }

   private static int seconds() {
      return 6;
   }

   private static final class Mark {
      private int stacks;
      private long expiry;
      private UUID source;
   }
}
