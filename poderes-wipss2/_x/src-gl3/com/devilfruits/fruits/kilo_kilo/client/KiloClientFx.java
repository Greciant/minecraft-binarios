package com.devilfruits.fruits.kilo_kilo.client;

import com.devilfruits.client.fx.ClientFxRegistry;
import com.devilfruits.client.render.vfx.FloorDebrisEffect;
import com.devilfruits.client.render.vfx.VfxManager;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.GroundWave;
import com.devilfruits.client.vfx.fx.ShockRing;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.post.VfxPost;
import com.devilfruits.core.network.FxPayload;
import com.devilfruits.fruits.kilo_kilo.KiloFx;
import com.devilfruits.fruits.kilo_kilo.KiloParticles;
import com.devilfruits.fruits.kilo_kilo.KiloSounds;
import com.devilfruits.fruits.kilo_kilo.client.vfx.GroundCracks;
import com.devilfruits.fruits.kilo_kilo.client.vfx.KiloLook;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

final class KiloClientFx {
   private static final int PALE = 15660287;

   static void register() {
      ClientFxRegistry.register(KiloFx.AWAKEN, KiloClientFx::awaken);
      ClientFxRegistry.register(KiloFx.RAMP_START, KiloClientFx::rampStart);
      ClientFxRegistry.register(KiloFx.RAMP_DONE, KiloClientFx::rampDone);
      ClientFxRegistry.register(KiloFx.LIGHT_JUMP, KiloClientFx::lightJump);
      ClientFxRegistry.register(KiloFx.FOOTSTEP, KiloClientFx::footstep);
      ClientFxRegistry.register(KiloFx.PRESS_START, KiloClientFx::pressStart);
      ClientFxRegistry.register(KiloFx.IMPACT, KiloClientFx::impact);
      ClientFxRegistry.register(KiloFx.PUNCH, KiloClientFx::punch);
   }

   private static void sound(ClientLevel level, Vec3 at, SoundEvent event, float volume, float pitch) {
      level.playLocalSound(at.x, at.y, at.z, event, SoundSource.PLAYERS, volume, pitch, false);
   }

   private static float pitch(double scale) {
      return (float)Mth.clamp(1.0 - 0.45 * scale, 0.5, 1.6);
   }

   private static Vec3 at(@Nullable Entity e, FxPayload p) {
      return e != null ? e.position() : p.pos();
   }

   private static void awaken(ClientLevel level, @Nullable Entity e, FxPayload p) {
      Vec3 at = at(e, p);
      RandomSource r = RandomSource.create(p.seed());
      sound(level, at, KiloSounds.WEIGHT_DOWN.get(), 1.0F, 1.2F);
      sound(level, at, KiloSounds.WEIGHT_UP.get(), 1.0F, 0.7F);

      for (int i = 0; i < 10; i++) {
         level.addParticle(
            (ParticleOptions)KiloParticles.FEATHER.get(), at.x + jit(r, 0.7), at.y + 1.0 + r.nextDouble(), at.z + jit(r, 0.7), jit(r, 0.05), 0.04, jit(r, 0.05)
         );
      }

      GroundCracks.patch(level, at.x, at.y, at.z, 2.2, 7.0F, 100, p.seed());
      if (!VfxSettings.legacy()) {
         for (int i = 0; i < VfxSettings.count(24.0F); i++) {
            VfxParticles.emit(
                  VfxPresets.GLASS_DUST,
                  at.x + jit(r, 0.5),
                  at.y + r.nextDouble() * 1.8,
                  at.z + jit(r, 0.5),
                  jit(r, 0.02),
                  0.05 + r.nextDouble() * 0.05,
                  jit(r, 0.02)
               )
               .tint(15660287)
               .life(2.0F);
         }

         double gy = VfxWorld.ground(level, at.x, at.y + 0.3, at.z, 3);
         if (!Double.isNaN(gy)) {
            GroundWave.at(at.x, gy, at.z, 4.0F, 14)
               .dust(VfxSettings.count(16.0F), KiloLook.earth(level, at.x, gy, at.z), 0.6F, 0.22F, 0.9F, p.seed())
               .strength(1.0F)
               .spawn();
         }

         ShockRing.at(at.add(0.0, 1.0, 0.0), new Vec3(0.0, 1.0, 0.0)).radius(0.4F, 2.6F).life(12).strength(0.6F).rim(0.0F, 15660287).spawn();
         CameraDirector.shake(0.25F, 0.06F, 2.5F, 3.0F);
      }
   }

   private static void rampStart(ClientLevel level, @Nullable Entity e, FxPayload p) {
      Vec3 at;
      double target;
      boolean var10000;
      label69: {
         at = at(e, p);
         target = (double)p.power();
         label56:
         if (!(target < 0.0)) {
            if (e instanceof Player pl && KiloClientState.scale(pl, 1.0F) > target) {
               break label56;
            }

            var10000 = false;
            break label69;
         }

         var10000 = true;
      }

      boolean down = var10000;
      boolean fast = p.extra() == 1;
      sound(level, at, down ? KiloSounds.WEIGHT_DOWN.get() : KiloSounds.WEIGHT_UP.get(), fast ? 1.1F : 0.8F, pitch(target) * (fast ? 1.3F : 1.0F));
      if (!VfxSettings.legacy() && e != null) {
         double y0 = down ? at.y + 0.2 : at.y + 1.7;
         ShockRing.at(new Vec3(at.x, y0, at.z), new Vec3(0.0, 1.0, 0.0))
            .radius(0.5F, 1.1F)
            .life(fast ? 6 : 12)
            .move(new Vec3(0.0, down ? 0.14 : -0.14, 0.0))
            .strength(0.6F)
            .band(0.5F)
            .rim(0.0F, 15660287)
            .spawn();
      }
   }

   private static void rampDone(ClientLevel level, @Nullable Entity e, FxPayload p) {
      Vec3 at = at(e, p);
      double s = (double)p.power();
      sound(level, at, KiloSounds.RAMP_DONE.get(), 0.7F, pitch(s));
      if (e != null) {
         RandomSource r = RandomSource.create(p.seed());
         double gy = VfxWorld.ground(level, at.x, at.y + 0.3, at.z, 3);
         if (s > 0.05 && !Double.isNaN(gy)) {
            GroundCracks.patch(level, at.x, gy, at.z, 0.8 + 1.4 * s, (float)(3.0 + 5.0 * s), 70, p.seed());
         }

         if (!VfxSettings.legacy()) {
            if (s < -0.05) {
               for (int i = 0; i < VfxSettings.count(14.0F); i++) {
                  double a = r.nextDouble() * (float) (Math.PI * 2);
                  VfxParticles.emit(VfxPresets.GLASS_DUST, at.x, at.y + 0.6 + r.nextDouble(), at.z, Math.cos(a) * 0.12, 0.05, Math.sin(a) * 0.12)
                     .tint(15660287)
                     .life(1.4F);
               }

               ShockRing.at(at.add(0.0, 1.0, 0.0), new Vec3(0.0, 1.0, 0.0)).radius(0.4F, 2.4F).life(10).strength(0.5F).rim(0.0F, 15660287).twist(0.5F).spawn();
            } else if (s > 0.05 && !Double.isNaN(gy)) {
               GroundWave.at(at.x, gy, at.z, (float)(1.5 + 1.5 * s), 10)
                  .dust(VfxSettings.count(10.0F), KiloLook.earth(level, at.x, gy, at.z), 0.4F, 0.18F, 0.7F, p.seed())
                  .strength(0.8F)
                  .spawn();
               if (e == Minecraft.getInstance().player) {
                  CameraDirector.shake(0.18F * (float)s, 0.1F, 2.0F, 3.0F);
               }
            }
         }
      }
   }

   private static void lightJump(ClientLevel level, @Nullable Entity e, FxPayload p) {
      Vec3 at = p.pos();
      float height = p.power();
      sound(level, at, KiloSounds.LIGHT_JUMP.get(), 0.6F + Math.min(0.5F, height / 60.0F), 1.4F - Math.min(0.5F, height / 100.0F));
      RandomSource r = RandomSource.create(p.seed());

      for (int i = 0; i < Math.min(8, 2 + (int)(height / 6.0F)); i++) {
         level.addParticle(
            (ParticleOptions)KiloParticles.FEATHER.get(),
            at.x + jit(r, 0.5),
            at.y + 0.2,
            at.z + jit(r, 0.5),
            jit(r, 0.06),
            0.05 + r.nextDouble() * 0.05,
            jit(r, 0.06)
         );
      }

      if (!VfxSettings.legacy()) {
         double gy = VfxWorld.ground(level, at.x, at.y + 0.3, at.z, 3);
         if (!Double.isNaN(gy)) {
            GroundWave.at(at.x, gy, at.z, 1.6F + Math.min(2.0F, height / 20.0F), 12)
               .dust(VfxSettings.count(10.0F), KiloLook.earth(level, at.x, gy, at.z), 0.3F, 0.12F, 0.7F, p.seed())
               .strength(0.4F)
               .spawn();
         }
      }
   }

   private static void footstep(ClientLevel level, @Nullable Entity e, FxPayload p) {
      Vec3 at = p.pos();
      float h = p.power();
      boolean landing = p.extra() == 1;
      sound(level, at, KiloSounds.HEAVY_STEP.get(), 0.5F + 0.6F * h, 1.15F - 0.5F * h);
      RandomSource r = RandomSource.create(p.seed());
      thump(at, (landing ? 0.3F : 0.16F) + 0.22F * h, 12.0F + 10.0F * h);
      BlockState floor = level.getBlockState(BlockPos.containing(at.x, at.y - 0.2, at.z));
      GroundCracks.patch(level, at.x, at.y, at.z, 0.5 + 1.0 * (double)h + (landing ? 0.6 : 0.0), 3.0F + 6.0F * h, 60, p.seed());
      if (!floor.isAir()) {
         for (int i = 0; (float)i < 4.0F + 6.0F * h; i++) {
            level.addParticle(
               new BlockParticleOption(ParticleTypes.BLOCK, floor),
               at.x + jit(r, 0.5),
               at.y + 0.1,
               at.z + jit(r, 0.5),
               jit(r, 0.15),
               0.12 + 0.1 * (double)h,
               jit(r, 0.15)
            );
         }
      }

      if (!VfxSettings.legacy()) {
         double gy = VfxWorld.ground(level, at.x, at.y + 0.3, at.z, 2);
         if (!Double.isNaN(gy)) {
            GroundWave.at(at.x, gy, at.z, 1.0F + 1.8F * h, 9)
               .dust(VfxSettings.count(4.0F + 6.0F * h), KiloLook.earth(level, at.x, gy, at.z), 0.35F, 0.08F + 0.1F * h, 0.45F + 0.3F * h, p.seed())
               .strength(0.5F + 0.6F * h)
               .spawn();
         }
      }
   }

   private static void thump(Vec3 at, float strength, float reach) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         double d = player.position().distanceTo(at);
         if (d < (double)reach) {
            CameraDirector.shake((float)((double)strength * (1.0 - d / (double)reach)), 0.14F, 2.2F, 3.4F);
         }
      }
   }

   private static void pressStart(ClientLevel level, @Nullable Entity e, FxPayload p) {
      Vec3 at = at(e, p);
      sound(level, at, KiloSounds.WEIGHT_UP.get(), 1.2F, 0.6F);
      if (e == Minecraft.getInstance().player) {
         CameraDirector.fovPunch(0.06F);
         CameraDirector.shake(0.2F, 0.08F, 2.0F, 4.0F);
      }

      if (!VfxSettings.legacy()) {
         RandomSource r = RandomSource.create(p.seed());

         for (int i = 0; i < 3; i++) {
            ShockRing.at(at.add(0.0, 0.9 - (double)i * 0.5, 0.0), new Vec3(0.0, 1.0, 0.0))
               .radius(0.5F + (float)i * 0.4F, 2.6F + (float)i)
               .life(9 + i * 2)
               .move(new Vec3(0.0, -0.3, 0.0))
               .strength(0.9F)
               .band(0.4F)
               .rim(0.0F, 15660287)
               .spawn();
         }

         for (int i = 0; i < VfxSettings.count(12.0F); i++) {
            double a = r.nextDouble() * (float) (Math.PI * 2);
            VfxParticles.emit(
                  VfxPresets.SPEED_LINE,
                  at.x + Math.cos(a) * 0.5,
                  at.y + r.nextDouble() * 1.8,
                  at.z + Math.sin(a) * 0.5,
                  Math.cos(a) * 0.5,
                  0.3,
                  Math.sin(a) * 0.5
               )
               .tint(15133424)
               .alpha(0.6F)
               .size(1.3F);
         }
      }
   }

   private static void impact(ClientLevel level, @Nullable Entity e, FxPayload p) {
      Vec3 at = p.pos();
      float power = p.power();
      float radius = (float)p.extra() / 16.0F;
      boolean press = p.dir().y > 0.5;
      float k = Mth.clamp(power / 2.6F, 0.15F, 1.2F);
      sound(level, at, KiloSounds.PRESS_IMPACT.get(), 1.0F + k, 1.05F - 0.35F * k);
      level.playLocalSound(at.x, at.y, at.z, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6F + 0.5F * k, 0.55F, false);
      RandomSource r = RandomSource.create(p.seed());
      double gy = VfxWorld.ground(level, at.x, at.y + 0.5, at.z, 4);
      double floorY = Double.isNaN(gy) ? at.y : gy;
      GroundCracks.patch(level, at.x, floorY, at.z, Math.max(1.5, (double)radius * 0.75), 9.0F, (int)(120.0F + 80.0F * k), p.seed());
      thump(at, 0.3F + 0.45F * k, radius * 5.0F);
      boolean firstPerson = e != null && VfxWorld.firstPersonCaster(e.getId());
      LocalPlayer self = Minecraft.getInstance().player;
      if (self != null && self.position().distanceTo(at) < (double)(radius * 3.0F)) {
         CameraDirector.fovPunch(-0.03F - 0.05F * k);
         if (k > 0.4F) {
            VfxClock.hitStop(press && k > 0.6F ? 3 : 2);
         }
      }

      if (VfxSettings.legacy()) {
         level.addParticle(ParticleTypes.EXPLOSION_EMITTER, at.x, floorY + 0.2, at.z, 0.0, 0.0, 0.0);
      } else {
         int earth = KiloLook.earth(level, at.x, floorY, at.z);
         VfxPost.chromatic((0.25F + 0.5F * k) * (firstPerson ? 0.35F : 1.0F));
         VfxPost.radialBlur(at, 0.05F * k);
         GroundWave.at(at.x, floorY, at.z, radius * 1.2F, 12 + (int)(8.0F * k))
            .dust(VfxSettings.count(14.0F + 22.0F * k), earth, 0.9F, 0.3F + 0.12F * k, 0.8F + 0.5F * k, p.seed())
            .strength(1.3F)
            .spawn();
         GroundWave.at(at.x, floorY, at.z, radius * 1.9F, 20)
            .air(floorY + 1.2, radius * 1.4F)
            .dust(VfxSettings.count(10.0F + 14.0F * k), earth, radius * 0.5F, 0.2F + 0.1F * k, 1.0F + 0.6F * k, p.seed() ^ 77L)
            .strength(1.2F * k)
            .spawn();
         if (!firstPerson) {
            VfxManager.add(
               new FloorDebrisEffect(level, new Vec3(at.x, floorY, at.z), VfxSettings.count(10.0F + 22.0F * k), 0.24F + 0.1F * k, 0.4F + 0.3F * k, 70, p.seed())
            );
            VfxManager.add(
               new FloorDebrisEffect(level, new Vec3(at.x, floorY, at.z), VfxSettings.count(6.0F + 10.0F * k), 0.1F, 0.55F + 0.35F * k, 70, p.seed() ^ 11L)
            );
         }

         for (int i = 0; i < VfxSettings.count(6.0F + 8.0F * k); i++) {
            double a = r.nextDouble() * (float) (Math.PI * 2);
            double d = r.nextDouble() * (double)radius * 0.7;
            VfxParticles.emit(
                  VfxPresets.DUST,
                  at.x + Math.cos(a) * d,
                  floorY + 0.4 + r.nextDouble() * 1.5,
                  at.z + Math.sin(a) * d,
                  Math.cos(a) * 0.04,
                  0.03,
                  Math.sin(a) * 0.04
               )
               .tint(earth)
               .size(1.4F + k)
               .alpha(0.8F)
               .life(1.5F);
         }

         for (int i = 0; i < VfxSettings.count(4.0F + 8.0F * k); i++) {
            double h = r.nextDouble();
            VfxParticles.emit(VfxPresets.SMOKE, at.x + jit(r, 0.8), floorY + h, at.z + jit(r, 0.8), jit(r, 0.06), 0.03 + h * 0.05, jit(r, 0.06))
               .tint(VfxWorld.shade(8025196, VfxWorld.light(level, at.x, floorY + 1.0, at.z)))
               .size(1.2F + 0.8F * k)
               .alpha(0.7F);
         }

         if (k > 0.8F && e == self) {
            VfxPost.vignette(0.3F * k);
         }
      }
   }

   private static void punch(ClientLevel level, @Nullable Entity e, FxPayload p) {
      Vec3 at = p.pos();
      Vec3 dir = p.dir();
      BlockState state = Block.stateById(p.extra());
      RandomSource r = RandomSource.create(p.seed());
      float hard = Mth.clamp(state.getDestroySpeed(level, BlockPos.containing(at)), 0.0F, 5.0F);
      sound(level, at, KiloSounds.PUNCH_THROUGH.get(), 0.9F, 1.1F - hard * 0.08F);
      level.playLocalSound(at.x, at.y, at.z, state.getSoundType(level, BlockPos.containing(at), null).getBreakSound(), SoundSource.BLOCKS, 1.0F, 0.7F, false);

      for (int i = 0; i < 14; i++) {
         level.addParticle(
            new BlockParticleOption(ParticleTypes.BLOCK, state),
            at.x + jit(r, 0.5),
            at.y + jit(r, 0.5),
            at.z + jit(r, 0.5),
            dir.x * 0.4 + jit(r, 0.25),
            dir.y * 0.4 + jit(r, 0.25) + 0.1,
            dir.z * 0.4 + jit(r, 0.25)
         );
      }

      thump(at, 0.18F, 14.0F);
      if (!VfxSettings.legacy()) {
         int colour = KiloLook.earth(level, at.x, at.y + 0.5, at.z);

         for (int i = 0; i < VfxSettings.count(6.0F); i++) {
            VfxParticles.emit(
                  VfxPresets.DUST, at.x + jit(r, 0.4), at.y + jit(r, 0.4), at.z + jit(r, 0.4), dir.x * 0.08 + jit(r, 0.06), 0.03, dir.z * 0.08 + jit(r, 0.06)
               )
               .tint(colour)
               .size(0.9F)
               .alpha(0.8F);
         }

         if (VfxSettings.hitFlashes()) {
            VfxParticles.emit(VfxPresets.GLOW, at.x, at.y, at.z, 0.0, 0.0, 0.0).tint(15920383).size(1.6F).alpha(0.5F).life(0.6F);
         }
      }
   }

   private static double jit(RandomSource r, double amount) {
      return (r.nextDouble() * 2.0 - 1.0) * amount;
   }

   private KiloClientFx() {
   }
}
