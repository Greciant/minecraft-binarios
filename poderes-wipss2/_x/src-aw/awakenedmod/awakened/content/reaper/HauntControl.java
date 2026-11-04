package com.awakenedmod.awakened.content.reaper;

import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.net.ReaperHauntPayload;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class HauntControl {
   private static final int HAUNT_TICKS = 120;
   private static final float DOT_PER_SECOND = 1.5F;
   private static final int DOT_INTERVAL = 20;
   private static final int DISORIENT_TICKS = 60;
   private static final Map<LivingEntity, HauntControl.Haunt> HAUNTED = new ConcurrentHashMap<>();
   private static int lastTick = -1;

   private HauntControl() {
   }

   public static void haunt(ServerPlayer caster, LivingEntity target, String abilityKey) {
      if (target != caster && target.isAlive() && !target.isRemoved() && Harm.allowed(caster, target)) {
         lay(target, caster.getUUID(), abilityKey, caster.level().getGameTime(), Harm.controls(caster, target));
      }
   }

   public static void previewSelf(ServerPlayer player) {
      if (player != null && player.isAlive() && !player.isRemoved()) {
         lay(player, null, null, player.serverLevel().getGameTime(), true);
      }
   }

   private static void lay(LivingEntity target, UUID caster, String abilityKey, long gameTime, boolean disorients) {
      HauntControl.Haunt h = HAUNTED.computeIfAbsent(target, t -> new HauntControl.Haunt());
      h.caster = caster;
      h.abilityKey = abilityKey;
      h.expiry = gameTime + 120L;
      h.disorients = disorients;
      if (disorients) {
         refreshDisorient(target);
         sendOverlay(target, 120);
      }
   }

   private static void sendOverlay(LivingEntity target, int remainingTicks) {
      if (target instanceof ServerPlayer victim) {
         PacketDistributor.sendToPlayer(victim, new ReaperHauntPayload(remainingTicks), new CustomPacketPayload[0]);
      }
   }

   public static void tickAll(MinecraftServer server) {
      int now = server.getTickCount();
      if (now != lastTick) {
         lastTick = now;
         Iterator<Entry<LivingEntity, HauntControl.Haunt>> it = HAUNTED.entrySet().iterator();

         while (it.hasNext()) {
            Entry<LivingEntity, HauntControl.Haunt> e = it.next();
            LivingEntity target = e.getKey();
            HauntControl.Haunt h = e.getValue();
            if (target.isAlive() && !target.isRemoved() && target.level() instanceof ServerLevel level && level.getGameTime() < h.expiry) {
               tickOne(level, target, h, (long)now);
            } else {
               sendOverlay(target, 0);
               it.remove();
            }
         }
      }
   }

   private static void tickOne(ServerLevel level, LivingEntity target, HauntControl.Haunt h, long now) {
      Vec3 center = target.position().add(0.0, (double)target.getBbHeight() * 0.6, 0.0);
      ReaperFx.hauntCling(level, center, (double)target.getBbWidth(), now);
      if (h.disorients && now % 60L == 0L) {
         refreshDisorient(target);
         sendOverlay(target, (int)Math.max(0L, h.expiry - now));
      }

      if (now % 20L == 0L) {
         ServerPlayer caster = h.caster == null ? null : level.getServer().getPlayerList().getPlayer(h.caster);
         float perPulse = 1.5F;
         if (caster != null && caster.isAlive()) {
            float dmg = MasteryScaling.scaledDamage(
               perPulse, caster, h.abilityKey, (float)ReaperScaling.offense(ReaperAttachments.souls(caster).souls()) - 1.0F
            );
            Dmg.hurt(target, level, Dmg.by(caster, DamageTypes.MAGIC), dmg);
         } else {
            float dmg = MasteryScaling.configScaledDamage(perPulse, h.abilityKey);
            Dmg.hurt(target, level, level.damageSources().magic(), dmg);
         }

         ReaperFx.hauntWail(level, center);
      }
   }

   private static void refreshDisorient(LivingEntity target) {
      target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, false, false));
   }

   private static final class Haunt {
      private UUID caster;
      private String abilityKey;
      private long expiry;
      private boolean disorients;
   }
}
