package com.awakenedmod.awakened.content.pyrokinetic;

import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.caster.Caster;
import com.awakenedmod.awakened.core.caster.PlayerCaster;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.data.PyroVisual;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class PyroHeatState {
   private static final String KEY = "heat";
   public static final float GAIN_FIREBALL = 0.08F;
   public static final float GAIN_BEAM_PER_TICK = 0.003F;
   public static final float GAIN_BARRAGE_SHOT = 0.02F;
   public static final float GAIN_JET_PER_TICK = 0.004F;
   public static final float GAIN_JET_TAP = 0.02F;
   public static final float GAIN_BACKDRAFT = 0.3F;
   private static final int DECAY_DELAY_TICKS = 30;
   private static final float DECAY_PER_TICK = 0.003F;
   private static final float OVERDRIVE_DECAY_FACTOR = 3.0F;
   private static final float WATER_DECAY_FACTOR = 4.0F;
   private static final int QUENCH_SOUND_INTERVAL = 12;
   private static final int QUENCH_STEAM_MAX = 6;
   public static final float HEAT_DAMAGE_BONUS_MAX = 0.3F;
   public static final float CAP_BASE = 0.3F;
   public static final float CAP_PER_LEVEL = 0.08F;
   public static final float OVERDRIVE_BURN_MIN = 1.0F;
   public static final float OVERDRIVE_BURN_MAX = 3.0F;
   private static final int BURN_INTERVAL = 20;
   private static final int BURN_SOUND_INTERVAL = 40;
   private static final int SYNC_INTERVAL = 10;
   private static final ResourceKey<DamageType> OVERDRIVE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, AwId.of("overdrive"));
   private static final Map<UUID, Long> LAST_GAIN = new ConcurrentHashMap<>();
   private static final Map<UUID, Float> SYNCED = new ConcurrentHashMap<>();

   private PyroHeatState() {
   }

   public static float heat(PowerData data) {
      return Mth.clamp(data.var("heat", 0.0F), 0.0F, 1.0F);
   }

   public static float heat(Caster caster) {
      return Mth.clamp(caster.var("heat", 0.0F), 0.0F, 1.0F);
   }

   public static float visualHeat(Entity entity) {
      return ((PyroVisual)entity.getData(AwakenedAttachments.PYRO_VISUAL)).heat01();
   }

   public static float cap(int masteryLevel) {
      return Mth.clamp(0.3F + 0.08F * (float)masteryLevel, 0.0F, 1.0F);
   }

   public static float overdrive(float heat01, float cap) {
      return Mth.clamp((heat01 - cap) / Math.max(0.01F, 1.0F - cap), 0.0F, 1.0F);
   }

   public static float overdriveBurnPerSecond(float heat01, int masteryLevel) {
      float cap = cap(masteryLevel);
      return heat01 <= cap ? 0.0F : Mth.lerp(overdrive(heat01, cap), 1.0F, 3.0F);
   }

   public static float heatDamageBonus(Caster caster) {
      return heatDamageBonus(heat(caster));
   }

   public static float heatDamageBonus(float heat01) {
      return 0.3F * Mth.clamp(heat01, 0.0F, 1.0F);
   }

   public static void gainFireball(Caster caster) {
      gain(caster, 0.08F);
   }

   public static void gainBeamTick(Caster caster) {
      gain(caster, 0.003F);
   }

   public static void gainBarrageShot(Caster caster) {
      gain(caster, 0.02F);
   }

   public static void gainJetTap(Caster caster) {
      gain(caster, 0.02F);
   }

   public static void gainJetTick(Caster caster) {
      gain(caster, 0.004F);
   }

   public static void gainBackdraft(Caster caster) {
      float heat = heat(caster);
      raise(caster, Math.max(heat + 0.3F, 0.7F));
      syncOwner(caster, heat(caster));
   }

   private static void gain(Caster caster, float amount) {
      raise(caster, heat(caster) + amount);
   }

   private static void raise(Caster caster, float heat01) {
      LAST_GAIN.put(caster.body().getUUID(), caster.level().getGameTime());
      setQuiet(caster, Math.min(heat01, 1.0F));
   }

   public static void tick(Caster caster) {
      LivingEntity body = caster.body();
      UUID id = body.getUUID();
      float heat = heat(caster);
      if (heat > 0.0F) {
         float cap = cap(caster.masteryLevel());
         float atTickStart = heat;
         boolean over = heat > cap;
         boolean wet = body.isInWater();
         Long last = LAST_GAIN.get(id);
         boolean cooling = wet || last == null || caster.level().getGameTime() - last >= 30L;
         if (cooling) {
            float rate = 0.003F * (wet ? 4.0F : 1.0F);
            if (heat > cap) {
               float fast = rate * 3.0F;
               float overshoot = heat - cap;
               if (overshoot >= fast) {
                  heat -= fast;
               } else {
                  heat = cap - (fast - overshoot) / 3.0F;
               }

               heat = Math.max(0.0F, heat);
            } else {
               heat = Math.max(0.0F, heat - rate);
            }

            setQuiet(caster, heat);
            if (wet && heat > 0.0F && body.tickCount % 12 == 0) {
               PyroFlames.sound(caster, body.position(), SoundEvents.FIRE_EXTINGUISH, 0.35F, 1.2F, FlameTone.of(caster));
               int puffs = 2 + Math.round(heat * 6.0F);
               Vec3 chest = body.position().add(0.0, (double)body.getBbHeight() * 0.5, 0.0);
               Fx.burst(caster.level(), ParticleTypes.CLOUD, chest, puffs, (double)body.getBbWidth() * 0.5, 0.02);
               Fx.burst(caster.level(), ParticleTypes.SMOKE, chest, Math.max(1, puffs / 2), (double)body.getBbWidth() * 0.45, 0.01);
            }

            if (heat == 0.0F) {
               LAST_GAIN.remove(id);
            }
         }

         if (over) {
            burn(caster, Mth.lerp(overdrive(atTickStart, cap), 1.0F, 3.0F));
         }

         maybeSyncOwner(caster, heat);
      }

      pushVisual(body, heat);
   }

   private static void burn(Caster caster, float perSecond) {
      LivingEntity body = caster.body();
      if (body.tickCount % 20 == 0) {
         Holder<DamageType> type = caster.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(OVERDRIVE_TYPE);
         Dmg.hurtRapid(body, caster.level(), new DamageSource(type), perSecond);
      }

      if (body.tickCount % 40 == 0) {
         PyroFlames.sound(caster, body.position(), SoundEvents.GENERIC_BURN, 0.35F, 1.0F, FlameTone.of(caster));
      }
   }

   public static void set(ServerPlayer player, float heat01) {
      set(PlayerCaster.of(player), heat01);
   }

   public static void set(Caster caster, float heat01) {
      setQuiet(caster, heat01);
      syncOwner(caster, heat(caster));
   }

   private static void setQuiet(Caster caster, float heat01) {
      float clamped = Mth.clamp(heat01, 0.0F, 1.0F);
      caster.setVar("heat", clamped);
      pushVisual(caster.body(), clamped);
   }

   private static void pushVisual(LivingEntity body, float heat01) {
      PyroVisual desired = PyroVisual.of(heat01);
      if (((PyroVisual)body.getData(AwakenedAttachments.PYRO_VISUAL)).heatByte() != desired.heatByte()) {
         body.setData(AwakenedAttachments.PYRO_VISUAL, desired);
      }
   }

   private static void maybeSyncOwner(Caster caster, float heat) {
      float last = SYNCED.getOrDefault(caster.body().getUUID(), 0.0F);
      if (heat != last) {
         if (heat == 0.0F || caster.body().tickCount % 10 == 0) {
            syncOwner(caster, heat);
         }
      }
   }

   private static void syncOwner(Caster caster, float heat) {
      if (heat == 0.0F) {
         SYNCED.remove(caster.body().getUUID());
      } else {
         SYNCED.put(caster.body().getUUID(), heat);
      }

      caster.syncOwner();
   }

   private static void forget(UUID id) {
      LAST_GAIN.remove(id);
      SYNCED.remove(id);
   }

   public static void onLogin(ServerPlayer player) {
      forget(player.getUUID());
   }

   public static void onLogout(ServerPlayer player) {
      forget(player.getUUID());
   }

   public static void onDeath(ServerPlayer player) {
      forget(player.getUUID());
      setQuiet(PlayerCaster.of(player), 0.0F);
   }

   public static void forget(Caster caster) {
      forget(caster.body().getUUID());
   }

   public static void onReplaced(ServerPlayer player) {
      forget(player.getUUID());
      if (((PyroVisual)player.getData(AwakenedAttachments.PYRO_VISUAL)).heatByte() != 0) {
         player.setData(AwakenedAttachments.PYRO_VISUAL, PyroVisual.COLD);
      }
   }
}
