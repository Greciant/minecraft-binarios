package com.devilfruits.fruits.supa_supa.client;

import com.devilfruits.client.fx.ChargeVisuals;
import com.devilfruits.client.fx.ClientFxRegistry;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.ShockRing;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.ParticleStyle;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.core.config.CoreClientConfig;
import com.devilfruits.core.network.ChargePayload;
import com.devilfruits.core.network.FxPayload;
import com.devilfruits.fruits.supa_supa.SupaAbilities;
import com.devilfruits.fruits.supa_supa.SupaFx;
import com.devilfruits.fruits.supa_supa.SupaSounds;
import com.devilfruits.fruits.supa_supa.client.vfx.AtomicSparCinematic;
import com.devilfruits.fruits.supa_supa.client.vfx.ClawRakeCinematic;
import com.devilfruits.fruits.supa_supa.client.vfx.CutHitCinematic;
import com.devilfruits.fruits.supa_supa.client.vfx.LungeCinematic;
import com.devilfruits.fruits.supa_supa.client.vfx.SlashChargeCinematic;
import com.devilfruits.fruits.supa_supa.client.vfx.SlashSeamCinematic;
import com.devilfruits.fruits.supa_supa.client.vfx.SpiralCinematic;
import com.devilfruits.fruits.supa_supa.client.vfx.SupaLook;
import com.devilfruits.fruits.supa_supa.client.vfx.SuperSlashCinematic;
import com.devilfruits.fruits.supa_supa.slash.SlashPlane;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

final class SupaClientFx {
   static final ChargeVisuals.ChargeVisual SLASH_CHARGE = new ChargeVisuals.ChargeVisual() {
      @Override
      public void start(ClientLevel level, Entity entity, ChargePayload payload) {
         if (entity instanceof Player player) {
            VfxDirector.spawn(new SlashChargeCinematic(player, payload.duration(), payload.seed()));
         }
      }

      @Override
      public void stop(ClientLevel level, Entity entity, boolean released) {
         SlashChargeCinematic c = VfxDirector.find(SlashChargeCinematic.class, x -> x.user() == entity);
         if (c != null) {
            c.stop(released);
         }
      }
   };

   static void register() {
      ClientFxRegistry.register(SupaFx.CLAW, SupaClientFx::claw);
      ClientFxRegistry.register(SupaFx.CUT_HIT, SupaClientFx::cutHit);
      ClientFxRegistry.register(SupaFx.ATOMIC_SPAR, SupaClientFx::atomicSpar);
      ClientFxRegistry.register(SupaFx.CLANG, SupaClientFx::clang);
      ClientFxRegistry.register(SupaFx.LUNGE, SupaClientFx::lunge);
      ClientFxRegistry.register(SupaFx.SPIRAL, SupaClientFx::spiral);
      ClientFxRegistry.register(SupaFx.SPIRAL_BITE, SupaClientFx::spiralBite);
      ClientFxRegistry.register(SupaFx.SLASH, SupaClientFx::slash);
      ClientFxRegistry.register(SupaFx.SLASH_END, SupaClientFx::slashEnd);
      ClientFxRegistry.register(SupaFx.SLASH_GLIDE, SupaClientFx::slashGlide);
      ClientFxRegistry.register(SupaFx.SLASH_EXIT, SupaClientFx::slashExit);
      ClientFxRegistry.register(SupaFx.SEAM_SPLIT, SupaClientFx::seamSplit);
      ChargeVisuals.register(SupaAbilities.SUPER_SLASH.id(), SLASH_CHARGE);
   }

   private static boolean low() {
      return VfxSettings.tier() == CoreClientConfig.CinematicQuality.LOW;
   }

   private static void claw(ClientLevel level, @Nullable Entity e, FxPayload p) {
      boolean left = (p.extra() & 1) != 0;
      boolean through = (p.extra() & 2) != 0;
      if (low()) {
         Vec3 at = p.pos().add(p.dir().scale(1.6)).add(0.0, -0.3, 0.0);
         level.addParticle(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, 0.0, 0.0, 0.0);
      } else {
         VfxDirector.spawn(new ClawRakeCinematic(e, p.pos(), p.dir(), p.power(), left, through, p.seed()));
      }
   }

   private static void cutHit(ClientLevel level, @Nullable Entity e, FxPayload p) {
      if (!low()) {
         VfxDirector.spawn(new CutHitCinematic(e, p.pos(), p.dir(), p.power(), p.seed()));
      } else {
         for (int i = 0; i < 6; i++) {
            level.addParticle(
               ParticleTypes.CRIT, p.pos().x, p.pos().y, p.pos().z, (level.random.nextDouble() - 0.5) * 0.5, 0.2, (level.random.nextDouble() - 0.5) * 0.5
            );
         }
      }
   }

   private static void atomicSpar(ClientLevel level, @Nullable Entity e, FxPayload p) {
      float half = (float)p.extra() / 16.0F;
      if (!low()) {
         VfxDirector.spawn(new AtomicSparCinematic(e, p.pos(), p.dir(), p.power(), half, p.seed()));
      } else {
         for (int i = 1; i <= 4; i++) {
            Vec3 at = p.pos().add(p.dir().scale((double)((float)i * p.power() / 4.0F)));
            level.addParticle(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, 0.0, 0.0, 0.0);
         }
      }
   }

   private static void clang(ClientLevel level, @Nullable Entity e, FxPayload p) {
      Vec3 at = p.pos();
      float dmg = p.power();
      if (low()) {
         for (int i = 0; i < 5; i++) {
            level.addParticle(ParticleTypes.CRIT, at.x, at.y, at.z, (level.random.nextDouble() - 0.5) * 0.6, 0.2, (level.random.nextDouble() - 0.5) * 0.6);
         }
      } else {
         Vec3 out = p.dir().lengthSqr() < 1.0E-4 ? new Vec3(0.0, 1.0, 0.0) : p.dir().normalize();
         int sparks = VfxSettings.count(Math.min(22.0F, 3.0F + dmg * 0.6F));
         float sparkSize = dmg < 8.0F ? 0.5F : 0.8F;

         for (int i = 0; i < sparks; i++) {
            Vec3 v = out.scale(0.12 + level.random.nextDouble() * 0.25)
               .add((level.random.nextDouble() - 0.5) * 0.3, 0.05 + level.random.nextDouble() * 0.2, (level.random.nextDouble() - 0.5) * 0.3);
            VfxParticles.emit(SupaLook.SPARK, at.x, at.y, at.z, v.x, v.y, v.z).size(sparkSize);
         }

         VfxParticles.emit(SupaLook.GLINT, at.x, at.y, at.z, 0.0, 0.0, 0.0).size(0.35F + Math.min(1.1F, dmg * 0.035F));
         if (dmg > 4.0F && e != null && !VfxWorld.firstPersonCaster(e.getId())) {
            ShockRing.at(at, out).radius(0.1F, 0.5F + Math.min(1.2F, dmg * 0.05F)).life(6).strength(0.6F).band(0.45F).rim(0.0F, 15398143).spawn();
         }

         if (e != null && VfxWorld.local(e.getId()) && dmg > 6.0F) {
            VfxClock.hitStop(dmg > 18.0F ? 2 : 1);
            CameraDirector.shake(Math.min(0.4F, dmg * 0.02F), 0.15F, 1.2F, 13.0F);
         }
      }
   }

   private static void lunge(ClientLevel level, @Nullable Entity e, FxPayload p) {
      if (low()) {
         Vec3 at = p.pos().add(p.dir().scale((double)p.power() * 0.6));
         level.addParticle(ParticleTypes.SWEEP_ATTACK, at.x, at.y - 0.3, at.z, 0.0, 0.0, 0.0);
      } else {
         VfxDirector.spawn(new LungeCinematic(e, p.pos(), p.dir(), p.power(), (float)p.extra() / 100.0F, p.seed()));
      }
   }

   private static void spiral(ClientLevel level, @Nullable Entity e, FxPayload p) {
      SpiralCinematic c = SpiralCinematic.of(e, p.extra(), p.seed());
      if (c != null) {
         VfxDirector.spawn(c);
      }
   }

   private static void spiralBite(ClientLevel level, @Nullable Entity e, FxPayload p) {
      Vec3 at = p.pos();
      if (low()) {
         level.addParticle(ParticleTypes.CRIT, at.x, at.y, at.z, 0.0, 0.1, 0.0);
      } else {
         RandomSource rnd = level.random;
         ParticleStyle spark = SupaLook.SPARK;
         if (p.power() < 0.5F) {
            for (int i = 0; i < 4; i++) {
               VfxParticles.emit(
                  SupaLook.FILING, at.x, at.y, at.z, (rnd.nextDouble() - 0.5) * 0.3, 0.08 + rnd.nextDouble() * 0.1, (rnd.nextDouble() - 0.5) * 0.3
               );
            }

            VfxParticles.emit(spark, at.x, at.y, at.z, (rnd.nextDouble() - 0.5) * 0.3, 0.12, (rnd.nextDouble() - 0.5) * 0.3).size(0.5F);
         } else {
            BlockPos pos = BlockPos.containing(at.add(p.dir().scale(0.6)));
            BlockState state = level.getBlockState(pos);
            int colour = state.isAir() ? 9079434 : state.getMapColor(level, pos).col;
            int light = VfxWorld.shade(colour, VfxWorld.light(level, at.x, at.y, at.z));
            Vec3 back = p.dir().scale(-0.15);

            for (int i = 0; i < 2 + p.extra(); i++) {
               ParticleStyle[] chunks = VfxPresets.BLAST_DEBRIS;
               VfxParticles.emit(
                     chunks[rnd.nextInt(chunks.length)],
                     at.x,
                     at.y,
                     at.z,
                     back.x + (rnd.nextDouble() - 0.5) * 0.3,
                     0.12 + rnd.nextDouble() * 0.15,
                     back.z + (rnd.nextDouble() - 0.5) * 0.3
                  )
                  .tint(light)
                  .size(0.4F);
            }

            VfxParticles.emit(SupaLook.DUST, at.x, at.y, at.z, back.x * 0.3, 0.02, back.z * 0.3).tint(light).size(0.7F).alpha(0.5F);
            if (!state.isAir() && state.getDestroySpeed(level, pos) >= 1.2F) {
               for (int i = 0; i < 3; i++) {
                  VfxParticles.emit(
                        spark,
                        at.x,
                        at.y,
                        at.z,
                        back.x + (rnd.nextDouble() - 0.5) * 0.4,
                        0.1 + rnd.nextDouble() * 0.15,
                        back.z + (rnd.nextDouble() - 0.5) * 0.4
                     )
                     .size(0.6F);
               }
            }
         }
      }
   }

   private static void slash(ClientLevel level, @Nullable Entity e, FxPayload p) {
      if (!low()) {
         VfxDirector.spawn(new SuperSlashCinematic(e, p.pos(), p.dir(), Math.round(p.power()), p.extra(), p.seed()));
         level.playLocalSound(p.pos().x, p.pos().y + 2.0, p.pos().z, SupaSounds.SLASH_FLY.get(), SoundSource.PLAYERS, 1.4F, 1.0F, false);
      } else {
         for (int i = 0; i < 8; i++) {
            Vec3 at = p.pos().add(p.dir().scale((double)i * 2.5)).add(0.0, (double)p.power() * 0.5, 0.0);
            level.addParticle(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, 0.0, 0.0, 0.0);
         }
      }
   }

   private static void slashEnd(ClientLevel level, @Nullable Entity e, FxPayload p) {
      SuperSlashCinematic c = VfxDirector.find(SuperSlashCinematic.class, x -> x.seed() == p.seed());
      if (c != null) {
         c.end(p.pos());
      }
   }

   private static void slashGlide(ClientLevel level, @Nullable Entity e, FxPayload p) {
      SlashPlane plane = SlashPlane.unpack(p.pos(), p.dir(), p.extra());
      SlashSeamCinematic seam = new SlashSeamCinematic(
         level, e != null ? e.getId() : -1, p.pos(), p.dir(), Math.round(p.power()), p.seed(), SlashPlane.index(p.extra()), plane
      );
      if (seam.valid()) {
         VfxDirector.spawn(seam);
      }

      SuperSlashCinematic c = VfxDirector.find(SuperSlashCinematic.class, x -> x.seed() == p.seed());
      if (c != null) {
         c.glide(level, p.pos(), p.dir(), Math.round(p.power()), plane.spanDir());
      }
   }

   private static void slashExit(ClientLevel level, @Nullable Entity e, FxPayload p) {
      SuperSlashCinematic c = VfxDirector.find(SuperSlashCinematic.class, x -> x.seed() == p.seed());
      if (c != null) {
         c.exit(p.pos(), p.dir(), (double)p.power());
      }

      SlashSeamCinematic seam = VfxDirector.find(SlashSeamCinematic.class, x -> x.seed() == p.seed() && !x.paused());
      if (seam != null) {
         seam.pause(p.extra());
      }
   }

   private static void seamSplit(ClientLevel level, @Nullable Entity e, FxPayload p) {
      SlashSeamCinematic seam = VfxDirector.find(SlashSeamCinematic.class, x -> x.seed() == p.seed() && x.index() == p.extra());
      if (seam != null) {
         seam.split(p.power() > 0.0F);
      }
   }

   private SupaClientFx() {
   }
}
