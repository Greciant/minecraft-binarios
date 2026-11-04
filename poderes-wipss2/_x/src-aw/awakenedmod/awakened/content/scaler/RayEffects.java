package com.awakenedmod.awakened.content.scaler;

import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Headroom;
import com.awakenedmod.awakened.core.net.ScalerStatePayload;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.neoforged.neoforge.network.PacketDistributor;

public final class RayEffects {
   public static final int MAX_OFFSET = 2;
   private static final int STRUGGLE_TICKS = 40;
   private static final int STRUGGLE_MIN_INTERVAL = 2;
   private static final ResourceLocation RAY_SCALE_ID = AwId.of("scaler_ray_scale");
   private static final ResourceLocation RAY_ATTACK_ID = AwId.of("scaler_ray_attack");
   private static final Map<UUID, RayEffects.Shift> SHIFTS = new ConcurrentHashMap<>();
   private static final Map<UUID, RayEffects.PausedShift> PAUSED = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> LAST_STRUGGLE = new ConcurrentHashMap<>();

   private RayEffects() {
   }

   public static boolean isShifted(LivingEntity entity) {
      return SHIFTS.containsKey(entity.getUUID());
   }

   private static SizeStage stageFor(int offset) {
      return SizeStage.NORMAL.shifted(offset);
   }

   public static float damageTakenMult(LivingEntity entity) {
      RayEffects.Shift shift = SHIFTS.get(entity.getUUID());
      return shift == null ? 1.0F : stageFor(shift.offset).damageTakenMult();
   }

   public static String apply(ServerPlayer caster, LivingEntity victim, int dir, int durationTicks) {
      RayEffects.Shift shift = SHIFTS.get(victim.getUUID());
      int current = shift == null ? 0 : shift.offset;
      int next = current + dir;
      if (Math.abs(next) > 2) {
         return "awakened.scaler.ray_at_cap";
      } else {
         SizeStage from = stageFor(current);
         SizeStage to = stageFor(next);
         if (next == 0) {
            revert(victim);
            return null;
         } else if (to.scale() > from.scale() && !Headroom.fits(victim, (double)(victim.getScale() / from.scale() * to.scale()))) {
            return "awakened.scaler.no_room_target";
         } else {
            applyModifiers(victim, to);
            if (shift == null) {
               SHIFTS.put(victim.getUUID(), new RayEffects.Shift(caster.getUUID(), victim, next, durationTicks));
            } else {
               shift.offset = next;
               shift.ticksLeft = durationTicks;
            }

            if (victim instanceof ServerPlayer sp) {
               syncRestraint(sp);
               sp.displayClientMessage(Component.translatable("awakened.scaler.ray_hint"), true);
            }

            return null;
         }
      }
   }

   private static void applyModifiers(LivingEntity victim, SizeStage to) {
      Scaling.setModifier(victim, Attributes.SCALE, RAY_SCALE_ID, (double)to.scale() - 1.0, Operation.ADD_MULTIPLIED_TOTAL);
      Scaling.setModifier(victim, Attributes.ATTACK_DAMAGE, RAY_ATTACK_ID, (double)to.damageDealtMult() - 1.0, Operation.ADD_MULTIPLIED_TOTAL);
   }

   public static void pause(UUID victimId) {
      RayEffects.Shift shift = SHIFTS.remove(victimId);
      if (shift != null) {
         PAUSED.put(victimId, new RayEffects.PausedShift(shift.caster, shift.offset, shift.ticksLeft));
      }
   }

   public static void resume(LivingEntity victim, int capTicks) {
      RayEffects.PausedShift paused = PAUSED.remove(victim.getUUID());
      if (paused != null) {
         applyModifiers(victim, stageFor(paused.offset()));
         SHIFTS.put(victim.getUUID(), new RayEffects.Shift(paused.caster(), victim, paused.offset(), Math.min(Math.max(1, paused.ticksLeft()), capTicks)));
         if (victim instanceof ServerPlayer sp) {
            syncRestraint(sp);
         }
      }
   }

   public static void revert(LivingEntity victim) {
      if (SHIFTS.remove(victim.getUUID()) != null) {
         Scaling.removeModifier(victim, Attributes.SCALE, RAY_SCALE_ID);
         Scaling.removeModifier(victim, Attributes.ATTACK_DAMAGE, RAY_ATTACK_ID);
         if (victim.level() instanceof ServerLevel level) {
            Fx.burst(
               level, ParticleTypes.CLOUD, victim.position().add(0.0, (double)victim.getBbHeight() * 0.4, 0.0), 8, (double)victim.getBbWidth() * 0.5, 0.04
            );
            Fx.sound(level, victim.position(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.7F, 1.5F);
         }

         if (victim instanceof ServerPlayer sp) {
            syncRestraint(sp);
         }
      }
   }

   public static void tick(ServerPlayer caster) {
      UUID casterId = caster.getUUID();
      Iterator<Entry<UUID, RayEffects.Shift>> it = SHIFTS.entrySet().iterator();

      while (it.hasNext()) {
         RayEffects.Shift shift = it.next().getValue();
         if (shift.caster.equals(casterId)) {
            LivingEntity victim = shift.victim;
            if (victim.isAlive() && !victim.isRemoved()) {
               if (--shift.ticksLeft <= 0) {
                  revert(victim);
               }
            } else {
               if (shift.offset < 0 && Foes.defeatedBy(victim, caster)) {
                  QuestSystem.feat(caster, "scaler_shrunk_kill");
               }

               it.remove();
            }
         }
      }
   }

   public static void clearCaster(ServerPlayer caster) {
      UUID casterId = caster.getUUID();

      for (RayEffects.Shift shift : SHIFTS.values()) {
         if (shift.caster.equals(casterId)) {
            revert(shift.victim);
         }
      }

      PAUSED.values().removeIf(paused -> paused.caster().equals(casterId));
   }

   public static void struggle(ServerPlayer sender) {
      if (sender != null) {
         long gameTime = sender.level().getGameTime();
         Long last = LAST_STRUGGLE.get(sender.getUUID());
         if (last == null || gameTime - last >= 2L) {
            LAST_STRUGGLE.put(sender.getUUID(), gameTime);
            if (SeizeControl.isSeizedVictim(sender)) {
               SeizeControl.struggle(sender);
            } else if (JarControl.isJarred(sender)) {
               JarControl.struggle(sender);
            } else {
               RayEffects.Shift shift = SHIFTS.get(sender.getUUID());
               if (shift == null) {
                  LAST_STRUGGLE.remove(sender.getUUID());
               } else {
                  shift.ticksLeft -= 40;
                  ServerLevel level = Dmg.serverLevel(sender);
                  Fx.burst(
                     level, ParticleTypes.CRIT, sender.position().add(0.0, (double)sender.getBbHeight() * 0.5, 0.0), 3, (double)sender.getBbWidth() * 0.4, 0.05
                  );
                  if (shift.ticksLeft <= 0) {
                     revert(sender);
                  }
               }
            }
         }
      }
   }

   public static void syncRestraint(ServerPlayer target) {
      PacketDistributor.sendToPlayer(
         target,
         new ScalerStatePayload(isShifted(target), SeizeControl.isSeizedVictim(target) || JarControl.isJarred(target), SeizeControl.isSeizing(target)),
         new CustomPacketPayload[0]
      );
   }

   private static record PausedShift(UUID caster, int offset, int ticksLeft) {
   }

   private static final class Shift {
      private final UUID caster;
      private final LivingEntity victim;
      private int offset;
      private int ticksLeft;

      private Shift(UUID caster, LivingEntity victim, int offset, int ticksLeft) {
         this.caster = caster;
         this.victim = victim;
         this.offset = offset;
         this.ticksLeft = ticksLeft;
      }
   }
}
