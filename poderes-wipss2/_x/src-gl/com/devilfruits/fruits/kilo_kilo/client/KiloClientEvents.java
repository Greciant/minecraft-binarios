package com.devilfruits.fruits.kilo_kilo.client;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxSound;
import com.devilfruits.fruits.kilo_kilo.KiloKilo;
import com.devilfruits.fruits.kilo_kilo.KiloSounds;
import com.devilfruits.fruits.kilo_kilo.client.vfx.GroundCracks;
import com.devilfruits.fruits.kilo_kilo.client.vfx.WeightAuraCinematic;
import com.devilfruits.fruits.kilo_kilo.weight.BodyWeight;
import com.devilfruits.fruits.kilo_kilo.weight.WeightCurves;
import com.devilfruits.fruits.kilo_kilo.weight.WeightManager;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.ViewportEvent.ComputeFov;

public final class KiloClientEvents {
   private static final Int2ObjectMap<WeightAuraCinematic> AURAS = new Int2ObjectOpenHashMap();
   private static float sag;
   private static float sagO;
   private static float fovNarrow;
   @Nullable
   private static VfxSound glide;

   static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      ClientLevel level = mc.level;
      if (level != null) {
         if (!mc.isPaused()) {
            KiloClientState.tick(level);
            GroundCracks.tickAll();
            LocalPlayer self = mc.player;
            if (self != null && KiloKilo.isUser(self)) {
               move(mc, self);
            } else {
               sagO = sag;
               sag *= 0.8F;
               fovNarrow *= 0.8F;
            }

            for (AbstractClientPlayer p : level.players()) {
               BodyWeight w = KiloClientState.weight(p);
               if (w != null && (!(Math.abs(w.scale()) < 0.02) || w.ramping())) {
                  WeightAuraCinematic aura = (WeightAuraCinematic)AURAS.get(p.getId());
                  if (aura == null || aura.isDone()) {
                     aura = VfxDirector.spawn(new WeightAuraCinematic(VfxAnchor.of(p, p.position()), p.getId()));
                     AURAS.put(p.getId(), aura);
                  }
               }
            }

            AURAS.int2ObjectEntrySet().removeIf(e -> ((WeightAuraCinematic)e.getValue()).isDone());
         }
      }
   }

   static void onLogout(LoggingOut event) {
      AURAS.clear();
      KiloClientState.clear();
      GroundCracks.clear();
      fovNarrow = 0.0F;
      sagO = 0.0F;
      sag = 0.0F;
   }

   private static void move(Minecraft mc, LocalPlayer p) {
      double kg = KiloClientState.kg(p, 1.0F);
      WeightCurves.Tuning t = WeightManager.tuning();
      double light = WeightCurves.lightness(kg);
      double heavy = WeightCurves.heaviness(kg);
      Vec3 v = p.getDeltaMovement();
      boolean free = !p.getAbilities().flying && !p.isSpectator() && !p.isPassenger() && !p.isFallFlying();
      boolean air = !p.onGround();
      boolean gliding = false;
      if (free) {
         double cap = WeightCurves.fallCap(kg, t);
         if (v.y < -cap) {
            v = new Vec3(v.x, -cap, v.z);
         }

         if (p.isInWater() && heavy > 0.05) {
            v = new Vec3(v.x * (1.0 - 0.3 * heavy), Math.max(v.y - 0.05 * heavy, -0.25 - 0.9 * heavy), v.z * (1.0 - 0.3 * heavy));
         }

         if (air && light > 0.01 && !p.isInWater()) {
            if (!p.onClimbable() && !p.isNoGravity() && !p.hasEffect(MobEffects.LEVITATION)) {
               v = new Vec3(v.x, WeightCurves.keepFalling(v.y), v.z);
            }

            float fwd = p.input.forwardImpulse;
            float side = p.input.leftImpulse;
            if (fwd != 0.0F || side != 0.0F) {
               float yaw = p.getYRot() * (float) (Math.PI / 180.0);
               double sin = (double)Mth.sin(yaw);
               double cos = (double)Mth.cos(yaw);
               double dx = (double)side * cos - (double)fwd * sin;
               double dz = (double)fwd * cos + (double)side * sin;
               double len = Math.max(1.0, Math.sqrt(dx * dx + dz * dz));
               double accel = 0.025 * WeightManager.setting("airControl", 1.0) * light;
               v = v.add(dx / len * accel, 0.0, dz / len * accel);
            }

            if (p.input.jumping && v.y < 0.0) {
               double floor = Math.max(WeightManager.setting("glideFallSpeed", 0.05), WeightCurves.terminal(WeightCurves.gravity(kg, t)) * 0.5);
               double glideCap = Mth.lerp(light, WeightCurves.VANILLA_TERMINAL, floor);
               if (v.y < -glideCap) {
                  v = new Vec3(v.x, Math.max(v.y * 0.8, -glideCap), v.z);
               }

               Vec3 look = p.getLookAngle().multiply(1.0, 0.0, 1.0);
               if (look.lengthSqr() > 1.0E-4) {
                  v = v.add(look.normalize().scale(0.006 * light));
               }

               gliding = light > 0.3;
            }

            BlockPos at = p.blockPosition();
            if (p.level().isRaining() && p.level().canSeeSky(at)) {
               double drift = WeightManager.setting("windDrift", 0.012) * (p.level().isThundering() ? 1.0 : 0.5) * light;
               double angle = (double)p.level().getGameTime() / 600.0 * Math.PI * 2.0 + (double)Mth.sin((float)p.level().getGameTime() * 0.013F) * 0.8;
               v = v.add(Math.cos(angle) * drift, 0.0, Math.sin(angle) * drift);
            }
         }

         p.setDeltaMovement(v);
      }

      if (gliding && (glide == null || glide.isStopped())) {
         glide = VfxSound.play(
            KiloSounds.GLIDE_LOOP.get(), p, p.position(), true, age -> Math.min(0.5F, (float)age / 20.0F), age -> 1.1F, 2000, () -> glidingNow(p)
         );
      }

      float rate = (float)(0.5 * (1.0 + 0.4 * light - 0.55 * heavy));
      p.xBob = p.xBobO + (p.getXRot() - p.xBobO) * rate;
      p.yBob = p.yBobO + (p.getYRot() - p.yBobO) * rate;
      sagO = sag;
      sag = sag + ((float)heavy - sag) * 0.15F;
      float falling = (float)Mth.clamp(-v.y / Math.max(0.5, WeightCurves.fallCap(kg, t)), 0.0, 1.0);
      float targetNarrow = air ? (float)heavy * falling : 0.0F;
      fovNarrow = fovNarrow + (targetNarrow - fovNarrow) * 0.2F;
   }

   private static boolean glidingNow(LocalPlayer p) {
      return p.isAlive() && !p.onGround() && p.input.jumping && p.getDeltaMovement().y < 0.0 && WeightCurves.lightness(KiloClientState.kg(p, 1.0F)) > 0.3;
   }

   static double cameraSag(Entity entity, float partialTick) {
      Minecraft mc = Minecraft.getInstance();
      return entity != mc.player ? 0.0 : -0.12 * (double)Mth.lerp(partialTick, sagO, sag);
   }

   static void onFov(ComputeFov event) {
      if (fovNarrow > 0.005F && event.usedConfiguredFov()) {
         event.setFOV(event.getFOV() * (1.0 - 0.08 * (double)fovNarrow));
      }
   }

   private KiloClientEvents() {
   }
}
