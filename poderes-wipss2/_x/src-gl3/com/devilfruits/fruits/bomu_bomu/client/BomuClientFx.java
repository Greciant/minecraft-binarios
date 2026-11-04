package com.devilfruits.fruits.bomu_bomu.client;

import com.devilfruits.client.fx.ClientFxRegistry;
import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.ExplosionCinematic;
import com.devilfruits.client.vfx.fx.ShockRing;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.devilfruits.core.fx.BombExplosionPayload;
import com.devilfruits.core.fx.ExplosionProfile;
import com.devilfruits.core.network.FxPayload;
import com.devilfruits.fruits.bomu_bomu.BomuFx;
import com.devilfruits.fruits.bomu_bomu.BomuSounds;
import com.devilfruits.fruits.bomu_bomu.client.vfx.BomuLook;
import com.devilfruits.fruits.bomu_bomu.client.vfx.VanishCinematic;
import com.mojang.blaze3d.vertex.BufferBuilder;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

final class BomuClientFx {
   static void register() {
      ClientFxRegistry.register(BomuFx.AWAKEN, BomuClientFx::awaken);
      ClientFxRegistry.register(BomuFx.ABSORB, BomuClientFx::absorb);
      ClientFxRegistry.register(BomuFx.PUNCH_ARM, BomuClientFx::punchArm);
      ClientFxRegistry.register(BomuFx.PUNCH_FIZZLE, BomuClientFx::punchFizzle);
      ClientFxRegistry.register(BomuFx.CANNON_FIRE, BomuClientFx::cannonFire);
      ClientFxRegistry.register(BomuFx.SPLAT, BomuClientFx::splat);
      ClientFxRegistry.register(BomuFx.SHOT_FIRE, BomuClientFx::shotFire);
      ClientFxRegistry.register(BomuFx.BREATH, BomuClientFx::breath);
      ClientFxRegistry.register(BomuFx.GUST, BomuClientFx::gust);
      ClientFxRegistry.register(BomuFx.SELF_DESTRUCT_TIER, BomuClientFx::tier);
      ClientFxRegistry.register(BomuFx.SELF_DESTRUCT_VANISH, BomuClientFx::vanish);
      ClientFxRegistry.register(BomuFx.SELF_DESTRUCT_REFORM, BomuClientFx::reform);
   }

   private static void sound(ClientLevel level, Vec3 at, SoundEvent event, float volume, float pitch) {
      level.playLocalSound(at.x, at.y, at.z, event, SoundSource.PLAYERS, volume, pitch, false);
   }

   private static double jit(RandomSource r, double a) {
      return (r.nextDouble() * 2.0 - 1.0) * a;
   }

   private static void awaken(ClientLevel level, @Nullable Entity e, FxPayload p) {
      Vec3 at = e != null ? e.position().add(0.0, 1.0, 0.0) : p.pos();
      ExplosionCinematic.play(level, e, new BombExplosionPayload(at, new Vec3(0.0, 1.0, 0.0), ExplosionProfile.SMALL, 1.3F, p.seed(), 0));
      sound(level, at, BomuSounds.PUNCH_ARM.get(), 1.0F, 0.8F);
   }

   private static void absorb(ClientLevel level, @Nullable Entity e, FxPayload p) {
      if (e instanceof Player player) {
         if (VfxWorld.local(player.getId())) {
            BomuHud.absorbed();
         }

         sound(level, player.position(), BomuSounds.ABSORB.get(), 0.6F, 1.0F);
         if (!VfxSettings.legacy()) {
            VfxDirector.spawn(new BomuClientFx.Shimmer(player));
         } else {
            for (int i = 0; i < 6; i++) {
               level.addParticle(
                  ParticleTypes.SMALL_FLAME,
                  player.getX() + jit(level.random, 0.3),
                  player.getY() + level.random.nextDouble() * 1.8,
                  player.getZ() + jit(level.random, 0.3),
                  0.0,
                  0.02,
                  0.0
               );
            }
         }
      }
   }

   private static void punchArm(ClientLevel level, @Nullable Entity e, FxPayload p) {
      if (e instanceof Player player) {
         boolean fp = VfxWorld.firstPersonCaster(player.getId());
         Vec3 fist = BomuLook.fist(player, 1.0F, true, fp);
         sound(level, fist, BomuSounds.PUNCH_ARM.get(), 0.9F, 1.0F);
         if (VfxSettings.legacy()) {
            level.addParticle(ParticleTypes.FLAME, fist.x, fist.y, fist.z, 0.0, 0.03, 0.0);
         } else {
            RandomSource r = RandomSource.create(p.seed());
            VfxParticles.emit(VfxPresets.FLARE, fist.x, fist.y, fist.z, 0.0, 0.0, 0.0).size(fp ? 0.25F : 0.55F);

            for (int i = 0; i < VfxSettings.count(fp ? 5.0F : 10.0F); i++) {
               VfxParticles.emit(VfxPresets.BLAST_SPARK, fist.x, fist.y, fist.z, jit(r, 0.2), 0.05 + r.nextDouble() * 0.15, jit(r, 0.2)).size(fp ? 0.4F : 0.7F);
            }
         }
      }
   }

   private static void punchFizzle(ClientLevel level, @Nullable Entity e, FxPayload p) {
      if (e instanceof Player player) {
         Vec3 fist = BomuLook.fist(player, 1.0F, true, VfxWorld.firstPersonCaster(player.getId()));
         sound(level, fist, BomuSounds.PUNCH_FIZZLE.get(), 0.7F, 1.0F);
         if (VfxSettings.legacy()) {
            level.addParticle(ParticleTypes.SMOKE, fist.x, fist.y, fist.z, 0.0, 0.03, 0.0);
         } else {
            for (int i = 0; i < 3; i++) {
               VfxParticles.emit(VfxPresets.SOOT, fist.x, fist.y + 0.1, fist.z, 0.0, 0.025, 0.0).size(0.5F).tint(4867136).alpha(0.6F);
            }
         }
      }
   }

   private static void cannonFire(ClientLevel level, @Nullable Entity e, FxPayload p) {
      Vec3 nose = p.pos();
      sound(level, nose, BomuSounds.CANNON_FLICK.get(), 0.6F, 1.0F + level.random.nextFloat() * 0.2F);
      sound(level, nose, BomuSounds.CANNON_FIRE.get(), 0.7F, 0.9F + level.random.nextFloat() * 0.3F);
      if (VfxSettings.legacy()) {
         level.addParticle(ParticleTypes.ITEM_SLIME, nose.x, nose.y, nose.z, 0.0, 0.0, 0.0);
      } else {
         RandomSource r = RandomSource.create(p.seed());
         Vec3 d = p.dir().lengthSqr() > 1.0E-6 ? p.dir().normalize() : Vec3.ZERO;

         for (int i = 0; i < 4; i++) {
            VfxParticles.emit(BomuLook.DROP, nose.x, nose.y, nose.z, d.x * 0.2 + jit(r, 0.05), d.y * 0.2 + 0.02, d.z * 0.2 + jit(r, 0.05))
               .tint(12111936)
               .alpha(0.9F);
         }
      }
   }

   private static void splat(ClientLevel level, @Nullable Entity e, FxPayload p) {
      Vec3 at = p.pos();
      if (!VfxSettings.legacy()) {
         RandomSource r = RandomSource.create(p.seed());
         Vec3 n = p.dir().lengthSqr() > 1.0E-6 ? p.dir().normalize() : new Vec3(0.0, 1.0, 0.0);
         VfxParticles.emit(BomuLook.SPLAT, at.x, at.y, at.z, 0.0, 0.0, 0.0).size(0.9F).tint(12111936).alpha(0.9F);
         VfxParticles.emit(VfxPresets.GLOW, at.x, at.y, at.z, 0.0, 0.0, 0.0).size(0.8F).tint(15004282).alpha(0.6F);

         for (int i = 0; i < VfxSettings.count(8.0F); i++) {
            Vec3 v = n.scale(0.08 + r.nextDouble() * 0.08).add(jit(r, 0.1), jit(r, 0.05) + 0.03, jit(r, 0.1));
            VfxParticles.emit(BomuLook.DROP, at.x, at.y, at.z, v.x, v.y, v.z).tint(r.nextBoolean() ? 12111936 : 15004282).alpha(0.9F);
         }
      } else {
         for (int i = 0; i < 6; i++) {
            level.addParticle(ParticleTypes.ITEM_SLIME, at.x, at.y, at.z, jit(level.random, 0.1), 0.1, jit(level.random, 0.1));
         }
      }
   }

   private static void shotFire(ClientLevel level, @Nullable Entity e, FxPayload p) {
      boolean bomb = (p.extra() & 1) == 1;
      Vec3 muzzle = p.pos();
      Vec3 d = p.dir().lengthSqr() > 1.0E-6 ? p.dir().normalize() : new Vec3(0.0, 0.0, 1.0);
      if (e instanceof Player player && VfxWorld.local(player.getId())) {
         BombPistolClient.recoil(bomb ? 1.0F : 0.6F);
         CameraDirector.shake(bomb ? 0.1F : 0.05F, 0.2F, 1.2F, 6.0F);
         CameraDirector.fovPunch(bomb ? 0.015F : 0.008F);
      }

      sound(level, muzzle, BomuSounds.SHOT_WHIZ.get(), bomb ? 0.7F : 0.4F, 1.05F + level.random.nextFloat() * 0.1F);
      if (VfxSettings.legacy()) {
         level.addParticle(ParticleTypes.CLOUD, muzzle.x, muzzle.y, muzzle.z, d.x * 0.2, d.y * 0.2, d.z * 0.2);
      } else {
         boolean fp = e != null && VfxWorld.firstPersonCaster(e.getId());
         RandomSource r = RandomSource.create(p.seed());
         Vec3 out = muzzle.add(d.scale(fp ? 0.6 : 0.3));
         ShockRing.at(out, d).radius(0.1F, fp ? 0.9F : 1.4F).life(6).strength(bomb ? 1.1F : 0.7F).band(0.45F).rim(0.0F, 16777215).move(d.scale(0.25)).spawn();

         for (int i = 0; i < VfxSettings.count(bomb ? 10.0F : 6.0F); i++) {
            Vec3 v = d.scale(0.5 + r.nextDouble() * 0.6).add(jit(r, 0.1), jit(r, 0.1), jit(r, 0.1));
            VfxParticles.emit(BomuLook.WIND, out.x, out.y, out.z, v.x, v.y, v.z).tint(15265524).alpha(0.55F).size(fp ? 0.6F : 1.1F);
         }

         float light = VfxWorld.light(level, out.x, out.y, out.z);

         for (int i = 0; i < 3; i++) {
            Vec3 v = d.scale(0.06 + (double)i * 0.05).add(jit(r, 0.02), 0.01, jit(r, 0.02));
            VfxParticles.emit(VfxPresets.BLAST_SMOKE, out.x, out.y, out.z, v.x, v.y, v.z)
               .size(fp ? 0.3F : 0.55F + (float)i * 0.15F)
               .tint(VfxWorld.shade(16054010, light))
               .alpha(bomb ? 0.3F : 0.22F)
               .life(0.16F)
               .unflipped();
         }
      }
   }

   private static void breath(ClientLevel level, @Nullable Entity e, FxPayload p) {
      if (e instanceof Player player) {
         boolean bomb = p.power() > 0.5F;
         boolean fp = VfxWorld.firstPersonCaster(player.getId());
         Vec3 mouth = player.getEyePosition().add(0.0, -0.18, 0.0).add(player.getLookAngle().scale(0.2));
         Vec3 cylinder = fp
            ? BomuLook.fist(player, 1.0F, (p.extra() & 1) == 0, true).add(0.0, 0.12, 0.0)
            : p.pos().add(player.getLookAngle().scale(-0.55)).add(0.0, 0.1, 0.0);
         if (VfxSettings.legacy()) {
            level.addParticle(ParticleTypes.CLOUD, mouth.x, mouth.y, mouth.z, 0.0, 0.0, 0.0);
         } else {
            RandomSource r = RandomSource.create(p.seed());
            Vec3 d = cylinder.subtract(mouth);

            for (int i = 0; i < VfxSettings.count(10.0F); i++) {
               Vec3 v = d.scale(0.12 + r.nextDouble() * 0.06).add(jit(r, 0.01), jit(r, 0.01), jit(r, 0.01));
               VfxParticles.emit(VfxPresets.SPEED_LINE, mouth.x, mouth.y, mouth.z, v.x, v.y, v.z)
                  .tint(16185599)
                  .alpha(fp ? 0.18F : 0.28F)
                  .size(fp ? 0.5F : 0.8F);
            }

            VfxParticles.emit(VfxPresets.GLASS_DUST, mouth.x, mouth.y, mouth.z, d.x * 0.1, d.y * 0.1, d.z * 0.1).tint(15397631).alpha(0.5F);
            if (bomb) {
               for (int i = 0; i < 3; i++) {
                  VfxParticles.emit(VfxPresets.BLAST_EMBER, cylinder.x + jit(r, 0.05), cylinder.y + jit(r, 0.05), cylinder.z + jit(r, 0.05), 0.0, 0.01, 0.0)
                     .life(0.4F)
                     .size(fp ? 0.5F : 1.0F);
               }
            }
         }
      }
   }

   private static void gust(ClientLevel level, @Nullable Entity e, FxPayload p) {
      Vec3 at = p.pos();
      sound(level, at, BomuSounds.SHOT_PLAIN.get(), 0.6F, 1.3F);
      if (VfxSettings.legacy()) {
         for (int i = 0; i < 4; i++) {
            level.addParticle(ParticleTypes.POOF, at.x, at.y, at.z, jit(level.random, 0.05), 0.03, jit(level.random, 0.05));
         }
      } else {
         Vec3 d = p.dir().lengthSqr() > 1.0E-6 ? p.dir().normalize() : new Vec3(0.0, 1.0, 0.0);
         ShockRing.at(at, d).radius(0.2F, 1.8F).life(7).strength(0.8F).band(0.45F).rim(0.0F, 16777215).spawn();
         RandomSource r = RandomSource.create(p.seed());
         int dust = VfxWorld.shade(VfxWorld.groundColour(level, at.x, at.y - 0.5, at.z), VfxWorld.light(level, at.x, at.y, at.z));

         for (int i = 0; i < VfxSettings.count(6.0F); i++) {
            VfxParticles.emit(VfxPresets.DUST, at.x, at.y, at.z, jit(r, 0.1), 0.03 + r.nextDouble() * 0.04, jit(r, 0.1)).tint(dust).size(0.6F).alpha(0.5F);
         }
      }
   }

   private static void tier(ClientLevel level, @Nullable Entity e, FxPayload p) {
      if (e != null) {
         Vec3 c = e.position().add(0.0, (double)e.getBbHeight() * 0.5, 0.0);
         sound(level, c, BomuSounds.SELF_DESTRUCT_TIER.get(), 0.8F, 0.9F + (float)p.extra() * 0.15F);
         if (VfxSettings.legacy()) {
            level.addParticle(ParticleTypes.FLASH, c.x, c.y, c.z, 0.0, 0.0, 0.0);
         } else {
            if (!VfxWorld.firstPersonCaster(e.getId())) {
               VfxParticles.emit(VfxPresets.BLAST_RING_UP, c.x, c.y, c.z, 0.0, 0.0, 0.0).size(1.5F + (float)p.extra()).tint(16756832).alpha(0.5F);
            }

            VfxParticles.emit(VfxPresets.GLOW, c.x, c.y, c.z, 0.0, 0.0, 0.0).size(2.0F + (float)p.extra() * 1.2F).tint(16773850).alpha(0.6F);
         }
      }
   }

   private static void vanish(ClientLevel level, @Nullable Entity e, FxPayload p) {
      if (e instanceof Player player && !VfxSettings.legacy()) {
         VfxDirector.spawn(new VanishCinematic(player, p.pos(), p.extra(), p.seed()));
         return;
      }

      if (e != null && VfxSettings.legacy()) {
         for (int i = 0; i < 12; i++) {
            level.addParticle(
               ParticleTypes.LARGE_SMOKE,
               e.getX() + jit(level.random, 0.4),
               e.getY() + level.random.nextDouble() * 1.8,
               e.getZ() + jit(level.random, 0.4),
               0.0,
               0.02,
               0.0
            );
         }
      }
   }

   private static void reform(ClientLevel level, @Nullable Entity e, FxPayload p) {
      Vec3 c = p.pos();
      sound(level, c, BomuSounds.SELF_DESTRUCT_REFORM.get(), 1.0F, 1.0F);
      if (VfxSettings.legacy()) {
         for (int i = 0; i < 10; i++) {
            level.addParticle(ParticleTypes.POOF, c.x + jit(level.random, 0.5), c.y + jit(level.random, 0.8), c.z + jit(level.random, 0.5), 0.0, 0.0, 0.0);
         }
      } else {
         RandomSource r = RandomSource.create(p.seed());
         float light = VfxWorld.light(level, c.x, c.y, c.z);

         for (int i = 0; i < VfxSettings.count(14.0F); i++) {
            double a = r.nextDouble() * (float) (Math.PI * 2);
            double h = jit(r, 0.9);
            Vec3 from = c.add(Math.cos(a) * 1.6, h, Math.sin(a) * 1.6);
            Vec3 v = c.subtract(from).scale(0.09);
            VfxParticles.emit(VfxPresets.BLAST_SMOKE, from.x, from.y, from.z, v.x, v.y, v.z)
               .size(0.7F + r.nextFloat() * 0.4F)
               .tint(VfxWorld.shade(4735550, light))
               .alpha(0.7F)
               .life(0.22F)
               .unflipped();
         }

         for (int i = 0; i < VfxSettings.count(16.0F); i++) {
            VfxParticles.emit(VfxPresets.BLAST_SPARK, c.x, c.y, c.z, jit(r, 0.3), 0.05 + r.nextDouble() * 0.25, jit(r, 0.3)).size(0.6F);
         }

         VfxParticles.emit(VfxPresets.GLOW, c.x, c.y, c.z, 0.0, 0.0, 0.0).size(2.6F).tint(16756832).alpha(0.7F);
      }
   }

   private BomuClientFx() {
   }

   private static final class Shimmer extends Cinematic {
      private final Player player;
      private final RandomSource random = RandomSource.create();

      Shimmer(Player player) {
         super(VfxAnchor.of(player, player.position()), VfxParams.NONE);
         this.player = player;
      }

      @Override
      public void tick(ClientLevel level) {
         super.tick(level);
         if (this.age <= 20 && !this.player.isRemoved()) {
            if (this.random.nextFloat() < 0.6F) {
               Vec3 at = BomuLook.onBody(this.player, 1.0F, this.random);
               VfxParticles.emit(VfxPresets.BLAST_EMBER, at.x, at.y, at.z, BomuClientFx.jit(this.random, 0.02), 0.04, BomuClientFx.jit(this.random, 0.02))
                  .life(0.6F);
            }
         } else {
            this.finish();
         }
      }

      @Override
      public void render(VfxFrame f) {
         BufferBuilder d = f.distortion();
         if (d != null && !VfxWorld.firstPersonCaster(this.player.getId())) {
            float k = 1.0F - (float)this.age / 20.0F;
            VfxSprites.Sprite glow = VfxSprites.GLOW;

            for (int i = 0; i < 2; i++) {
               double y = this.player.getY() + 0.5 + (double)i * 0.75 + (double)this.age * 0.03;
               MeshWriter.distortQuad(
                  d,
                  f.rx(this.player.getX()),
                  f.ry(y),
                  f.rz(this.player.getZ()),
                  0.75F,
                  0.5F * k,
                  1,
                  false,
                  f.left.x,
                  f.left.y,
                  f.left.z,
                  f.up.x,
                  f.up.y,
                  f.up.z,
                  glow.u0(),
                  glow.v0(),
                  glow.u1(),
                  glow.v1(),
                  f.time * 0.02F + (float)i * 0.37F,
                  0.5F
               );
            }
         }
      }

      @Override
      public double x() {
         return this.player.getX();
      }

      @Override
      public double y() {
         return this.player.getY();
      }

      @Override
      public double z() {
         return this.player.getZ();
      }
   }
}
