package com.devilfruits.fruits.suna_suna.client;

import com.devilfruits.client.fx.ChargeVisuals;
import com.devilfruits.client.fx.ClientFxRegistry;
import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.CameraEffects;
import com.devilfruits.client.render.vfx.ShockwaveEffect;
import com.devilfruits.client.render.vfx.VfxManager;
import com.devilfruits.client.sound.SoundDesign;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.core.network.ChargePayload;
import com.devilfruits.core.network.FxPayload;
import com.devilfruits.fruits.suna_suna.SunaAbilities;
import com.devilfruits.fruits.suna_suna.SunaFx;
import com.devilfruits.fruits.suna_suna.SunaParticles;
import com.devilfruits.fruits.suna_suna.SunaSounds;
import com.devilfruits.fruits.suna_suna.client.vfx.BarjanCinematic;
import com.devilfruits.fruits.suna_suna.client.vfx.DryGroundEffect;
import com.devilfruits.fruits.suna_suna.client.vfx.EncierroCinematic;
import com.devilfruits.fruits.suna_suna.client.vfx.GirasoleCinematic;
import com.devilfruits.fruits.suna_suna.client.vfx.LaSpadaCinematic;
import com.devilfruits.fruits.suna_suna.client.vfx.PesadoChargeCinematic;
import com.devilfruits.fruits.suna_suna.client.vfx.PesadoCinematic;
import com.devilfruits.fruits.suna_suna.client.vfx.SablesCinematic;
import com.devilfruits.fruits.suna_suna.client.vfx.SandBallEffect;
import com.devilfruits.fruits.suna_suna.client.vfx.SandBladeEffect;
import com.devilfruits.fruits.suna_suna.client.vfx.SandBlastCinematic;
import com.devilfruits.fruits.suna_suna.client.vfx.SandCinematic;
import com.devilfruits.fruits.suna_suna.client.vfx.SandCrescentEffect;
import com.devilfruits.fruits.suna_suna.client.vfx.SandFunnelEffect;
import com.devilfruits.fruits.suna_suna.client.vfx.SandGatherEffect;
import com.devilfruits.fruits.suna_suna.client.vfx.SandGripEffect;
import com.devilfruits.fruits.suna_suna.client.vfx.SandRiseEffect;
import com.devilfruits.fruits.suna_suna.client.vfx.SandSpearEffect;
import com.devilfruits.fruits.suna_suna.client.vfx.SandWaveEffect;
import com.devilfruits.fruits.suna_suna.client.vfx.SandstormEffect;
import com.devilfruits.fruits.suna_suna.client.vfx.SeccoCinematic;
import com.devilfruits.fruits.suna_suna.client.vfx.SpadaCinematic;
import com.devilfruits.fruits.suna_suna.client.vfx.TsunamiChargeCinematic;
import com.devilfruits.fruits.suna_suna.client.vfx.TsunamiCinematic;
import com.devilfruits.registry.DFSounds;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class SunaClientFx {
   public static void register() {
      ClientFxRegistry.register(SunaFx.AWAKEN, SunaClientFx::awaken);
      ClientFxRegistry.register(SunaFx.LEVEL_UP, (level, entity, p) -> swirl(entity, 18, 0.9));
      ClientFxRegistry.register(SunaFx.REINCARNATE, (level, entity, p) -> column(p.pos(), 24));
      ClientFxRegistry.register(SunaFx.DESICCATE, SunaClientFx::desiccate);
      ClientFxRegistry.register(SunaFx.SAND_BURST, SunaClientFx::burst);
      ClientFxRegistry.register(SunaFx.PESADO, SunaClientFx::pesado);
      ClientFxRegistry.register(SunaFx.PESADO_IMPACT, SunaClientFx::pesadoImpact);
      ClientFxRegistry.register(SunaFx.GIRASOLE, SunaClientFx::girasole);
      ClientFxRegistry.register(SunaFx.GIRASOLE_END, SunaClientFx::girasoleEnd);
      ClientFxRegistry.register(SunaFx.SPADA, SunaClientFx::spada);
      ClientFxRegistry.register(SunaFx.SPADA_END, SunaClientFx::spadaEnd);
      ClientFxRegistry.register(SunaFx.BARJAN, SunaClientFx::barjan);
      ClientFxRegistry.register(SunaFx.BARJAN_END, (level, entity, p) -> {
         BarjanCinematic.stop(p.seed(), p.pos());
         SandCrescentEffect crescent = VfxManager.find(SandCrescentEffect.class, e -> e.seed == p.seed());
         if (crescent != null) {
            crescent.stop(p.pos());
         }
      });
      ClientFxRegistry.register(SunaFx.SECCO, SunaClientFx::secco);
      ClientFxRegistry.register(SunaFx.SECCO_END, (level, entity, p) -> {
         SeccoCinematic.close(p.seed());
         DryGroundEffect dry = VfxManager.find(DryGroundEffect.class, e -> e.seed == p.seed());
         if (dry != null) {
            dry.close();
         }
      });
      ClientFxRegistry.register(SunaFx.LA_SPADA, SunaClientFx::laSpada);
      ClientFxRegistry.register(SunaFx.LA_SPADA_IMPACT, SunaClientFx::laSpadaImpact);
      ClientFxRegistry.register(SunaFx.CRUMBLE, SunaClientFx::crumble);
      ClientFxRegistry.register(SunaFx.ENCIERRO, SunaClientFx::encierro);
      ClientFxRegistry.register(SunaFx.ENCIERRO_END, SunaClientFx::encierroEnd);
      ClientFxRegistry.register(SunaFx.TSUNAMI, SunaClientFx::tsunami);
      ClientFxRegistry.register(SunaFx.TSUNAMI_END, (level, entity, p) -> {
         TsunamiCinematic.crash(p.seed());
         SandWaveEffect wave = VfxManager.find(SandWaveEffect.class, e -> e.seed == p.seed());
         if (wave != null) {
            wave.crash();
         }
      });
      ClientFxRegistry.register(SunaFx.SABLES, SunaClientFx::sables);
      ClientFxRegistry.register(SunaFx.SABLES_END, SunaClientFx::sablesEnd);
      ChargeVisuals.register(
         SunaAbilities.SAND_TSUNAMI.id(),
         new ChargeVisuals.ChargeVisual() {
            @Override
            public void start(ClientLevel level, Entity entity, ChargePayload payload) {
               if (VfxSettings.legacy()) {
                  VfxManager.add(new SandRiseEffect(entity, (int)payload.duration()));
               } else {
                  TsunamiChargeCinematic old = TsunamiChargeCinematic.of(entity);
                  if (old != null) {
                     old.stop();
                  }

                  VfxDirector.spawn(
                     new TsunamiChargeCinematic(
                        level,
                        VfxAnchor.of(entity, entity.position()),
                        new VfxParams(entity.position(), Vec3.ZERO, 1.0F, payload.seed(), Math.max(1, Math.round(payload.duration())))
                     )
                  );
               }

               SoundDesign.play(entity.position(), SunaSounds.TSUNAMI_CHARGE, 1.0F, 0.9F, SoundDesign.layer(DFSounds.LAYER_RUMBLE, 0.6F, 0.8F));
            }

            @Override
            public void stop(ClientLevel level, Entity entity, boolean released) {
               SandRiseEffect rise = VfxManager.find(SandRiseEffect.class, e -> e.follows(entity));
               if (rise != null) {
                  rise.stop();
               }

               TsunamiChargeCinematic charge = TsunamiChargeCinematic.of(entity);
               if (charge != null) {
                  charge.stop();
               }
            }
         }
      );
      ChargeVisuals.register(
         SunaAbilities.SABLES_PESADO.id(),
         new ChargeVisuals.ChargeVisual() {
            @Override
            public void start(ClientLevel level, Entity entity, ChargePayload payload) {
               if (VfxSettings.legacy()) {
                  VfxManager.add(new SandGatherEffect(entity, (int)payload.duration()));
               } else {
                  PesadoChargeCinematic old = PesadoChargeCinematic.of(entity);
                  if (old != null) {
                     old.stop();
                  }

                  VfxDirector.spawn(
                     new PesadoChargeCinematic(
                        level,
                        VfxAnchor.of(entity, entity.position()),
                        new VfxParams(entity.position(), Vec3.ZERO, 1.0F, payload.seed(), Math.max(1, Math.round(payload.duration())))
                     )
                  );
               }

               SoundDesign.play(entity.position(), SunaSounds.PESADO_CHARGE, 0.7F, 1.0F);
            }

            @Override
            public void stop(ClientLevel level, Entity entity, boolean released) {
               SandGatherEffect gather = VfxManager.find(SandGatherEffect.class, e -> e.follows(entity));
               if (gather != null) {
                  gather.finish();
               }

               PesadoChargeCinematic charge = PesadoChargeCinematic.of(entity);
               if (charge != null) {
                  charge.stop();
               }
            }
         }
      );
   }

   private static void pesado(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      boolean heavy = p.extra() > 0;
      RandomSource r = Fx.random();
      SoundDesign.play(
         p.pos(),
         heavy ? SunaSounds.PESADO_RELEASE : SunaSounds.PESADO_BLAST,
         heavy ? 1.0F : 0.8F,
         0.9F + r.nextFloat() * 0.2F,
         SoundDesign.layer(SunaSounds.LAYER_GUST, heavy ? 0.7F : 0.4F, heavy ? 0.8F : 1.3F)
      );
      if (!VfxSettings.legacy()) {
         VfxDirector.spawn(new PesadoCinematic(level, VfxAnchor.of(entity, p.pos()), VfxParams.of(p)));
         boolean atEyes = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(p.pos()) < 2.5600000000000005;
         if (!atEyes) {
            SandCinematic.burst(
               r, p.pos(), p.dir(), 0.35F, heavy ? 0.35F : 0.5F, heavy ? 8.0F : 5.0F, heavy ? 4.0F : 2.0F, SandBlastCinematic.tint(level, p.pos())
            );
         }

         if (entity == Minecraft.getInstance().player) {
            CameraEffects.fovPunch(heavy ? 0.06F : 0.025F);
            if (heavy) {
               CameraEffects.shake(0.12F);
            }
         }
      } else {
         VfxManager.add(new SandBallEffect(p.pos(), p.dir(), p.power(), p.extra(), p.seed()));
         boolean atEyesx = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(p.pos()) < 2.5600000000000005;
         int n = atEyesx ? 0 : Fx.count(heavy ? 10 : 5);
         Vec3 dir = p.dir().normalize();

         for (int i = 0; i < n; i++) {
            double s = 0.05 + r.nextDouble() * 0.15;
            Fx.spawn(
               (ParticleOptions)SunaParticles.GRAIN.get(),
               p.pos().x,
               p.pos().y,
               p.pos().z,
               dir.x * s + Fx.spread(0.06),
               dir.y * s + Fx.spread(0.06),
               dir.z * s + Fx.spread(0.06)
            );
         }

         if (entity == Minecraft.getInstance().player) {
            CameraEffects.fovPunch(heavy ? 0.06F : 0.025F);
            if (heavy) {
               CameraEffects.shake(0.12F);
            }
         }
      }
   }

   private static void pesadoImpact(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      SandBallEffect ball = VfxManager.find(SandBallEffect.class, e -> e.seed == p.seed());
      if (ball != null) {
         ball.finish();
      }

      RandomSource r = Fx.random();
      Vec3 at = p.pos();
      Vec3 dir = p.dir().lengthSqr() < 1.0E-6 ? new Vec3(0.0, -1.0, 0.0) : p.dir().normalize();
      if (!VfxSettings.legacy()) {
         PesadoCinematic.impact(p.seed());
         if (p.extra() == 0) {
            int tint = SandBlastCinematic.tint(level, at);
            SandCinematic.burst(r, at, dir.reverse().add(0.0, 0.4, 0.0), 0.6F, 0.3F, 10.0F, 3.0F, tint);
            SandCinematic.clouds(r, at.x, at.y, at.z, -dir.x * 0.05, 0.01, -dir.z * 0.05, 0.8F, 3.0F, tint);
            SoundDesign.play(at, SunaSounds.PESADO_IMPACT, 0.6F, 1.4F);
         } else {
            SandBlastCinematic.at(level, at, dir, Math.max(1.0F, p.power()), p.seed());
            SoundDesign.play(
               at,
               SunaSounds.PESADO_IMPACT,
               1.0F,
               0.9F + r.nextFloat() * 0.15F,
               SoundDesign.layer(DFSounds.LAYER_IMPACT, 0.5F, 1.2F),
               SoundDesign.layer(SunaSounds.LAYER_SAND, 0.8F, 0.7F)
            );
            CameraEffects.shakeAt(at, 0.3F, 18.0);
         }
      } else if (p.extra() == 0) {
         int n = Fx.count(12);

         for (int i = 0; i < n; i++) {
            double s = 0.08 + r.nextDouble() * 0.18;
            Fx.spawn(
               r.nextInt(3) == 0 ? (ParticleOptions)SunaParticles.CLUMP.get() : (ParticleOptions)SunaParticles.GRAIN.get(),
               at.x,
               at.y,
               at.z,
               -dir.x * s * 0.5 + Fx.spread(0.12),
               0.1 + r.nextDouble() * 0.15,
               -dir.z * s * 0.5 + Fx.spread(0.12)
            );
         }

         SoundDesign.play(at, SunaSounds.PESADO_IMPACT, 0.6F, 1.4F);
      } else {
         float radius = Math.max(1.0F, p.power());
         int n = Fx.count((int)(26.0F * radius));

         for (int i = 0; i < n; i++) {
            double a = r.nextDouble() * Math.PI * 2.0;
            double up = r.nextDouble();
            double s = 0.15 + r.nextDouble() * 0.35 * (double)(radius / 3.0F);
            Fx.spawn(
               r.nextInt(3) == 0 ? (ParticleOptions)SunaParticles.CLUMP.get() : (ParticleOptions)SunaParticles.GRAIN.get(),
               at.x,
               at.y + 0.2,
               at.z,
               Math.cos(a) * s,
               0.1 + up * 0.35,
               Math.sin(a) * s
            );
         }

         int dust = Fx.count((int)(8.0F * radius));

         for (int i = 0; i < dust; i++) {
            double a = r.nextDouble() * Math.PI * 2.0;
            double d = r.nextDouble() * (double)radius * 0.8;
            Fx.spawn(
               (ParticleOptions)SunaParticles.DUST.get(),
               at.x + Math.cos(a) * d,
               at.y + 0.1 + r.nextDouble() * 0.6,
               at.z + Math.sin(a) * d,
               Math.cos(a) * 0.05,
               0.02,
               Math.sin(a) * 0.05
            );
         }

         VfxManager.add(
            new ShockwaveEffect(ShockwaveEffect.Mode.GROUND, at.add(0.0, 0.15, 0.0), Vec3.ZERO, radius, 0.0F, 9, 2, 0.75F)
               .tint(184, 140, 72)
               .shell(196, 156, 88)
         );
         SoundDesign.play(
            at,
            SunaSounds.PESADO_IMPACT,
            1.0F,
            0.9F + r.nextFloat() * 0.15F,
            SoundDesign.layer(DFSounds.LAYER_IMPACT, 0.5F, 1.2F),
            SoundDesign.layer(SunaSounds.LAYER_SAND, 0.8F, 0.7F)
         );
         CameraEffects.shakeAt(at, 0.25F, 16.0);
      }
   }

   private static void spada(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      if (VfxSettings.legacy()) {
         VfxManager.add(new SandBladeEffect(p.pos(), p.dir(), p.power(), p.extra(), p.seed()));
      } else {
         VfxDirector.spawn(new SpadaCinematic(level, VfxAnchor.of(entity, p.pos()), VfxParams.of(p)));
         RandomSource r = Fx.random();
         int tint = SandBlastCinematic.tint(level, p.pos());
         SandCinematic.burst(r, p.pos().add(0.0, 0.2, 0.0), new Vec3(0.0, 1.0, 0.0), 0.6F, 0.3F, 14.0F, 5.0F, tint);
         SandCinematic.decal(r, p.pos().x, p.pos().y, p.pos().z, 2.6F, tint);
      }

      SoundDesign.play(p.pos(), SunaSounds.SPADA_CAST, 1.0F, 0.9F + Fx.random().nextFloat() * 0.15F, SoundDesign.layer(SunaSounds.LAYER_SAND, 0.8F, 0.8F));
      if (entity == Minecraft.getInstance().player) {
         CameraEffects.shake(0.1F);
      }
   }

   private static void spadaEnd(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      if (!VfxSettings.legacy()) {
         SpadaCinematic.stop(p.seed(), p.pos());
      } else {
         SandBladeEffect blade = VfxManager.find(SandBladeEffect.class, e -> e.seed == p.seed());
         if (blade != null) {
            blade.stop(p.pos());
         }

         RandomSource r = Fx.random();
         int n = Fx.count(10);

         for (int i = 0; i < n; i++) {
            Fx.spawn(
               r.nextInt(3) == 0 ? (ParticleOptions)SunaParticles.DUST.get() : (ParticleOptions)SunaParticles.CLUMP.get(),
               p.pos().x + Fx.spread(0.4),
               p.pos().y + 0.3,
               p.pos().z + Fx.spread(0.4),
               Fx.spread(0.12),
               0.12 + r.nextDouble() * 0.1,
               Fx.spread(0.12)
            );
         }
      }
   }

   private static void barjan(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      if (VfxSettings.legacy()) {
         VfxManager.add(new SandCrescentEffect(p.pos(), p.dir(), p.power(), p.extra(), p.seed()));
      } else {
         VfxDirector.spawn(new BarjanCinematic(level, VfxAnchor.of(entity, p.pos()), VfxParams.of(p)));
      }

      SoundDesign.play(p.pos(), SunaSounds.BARJAN_SWING, 1.0F, 0.9F + Fx.random().nextFloat() * 0.15F, SoundDesign.layer(SunaSounds.LAYER_GUST, 0.7F, 1.1F));
      if (entity == Minecraft.getInstance().player) {
         CameraEffects.fovPunch(0.04F);
      }
   }

   private static void secco(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      if (VfxManager.find(DryGroundEffect.class, e -> e.seed == p.seed()) == null) {
         VfxManager.add(new DryGroundEffect(level, p.pos(), p.power(), (int)p.dir().x, p.extra(), p.seed()));
         if (!VfxSettings.legacy()) {
            VfxDirector.spawn(new SeccoCinematic(level, VfxAnchor.of(entity, p.pos()), VfxParams.of(p)));
            RandomSource r = Fx.random();
            int tint = SandBlastCinematic.tint(level, p.pos());
            SandCinematic.burst(r, p.pos().add(0.0, 0.1, 0.0), new Vec3(0.0, 1.0, 0.0), 0.8F, 0.18F, 10.0F, 6.0F, tint);
            SandCinematic.clouds(r, p.pos().x, p.pos().y + 0.3, p.pos().z, 0.0, 0.02, 0.0, 1.2F, 4.0F, tint);
         }
      }

      SoundDesign.play(p.pos(), SunaSounds.DESICCATE, 1.0F, 0.8F, SoundDesign.layer(SunaSounds.LAYER_HISS, 0.8F, 0.7F));
   }

   private static void laSpada(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      RandomSource r = Fx.random();
      SoundDesign.play(p.pos(), SunaSounds.LA_SPADA_THROW, 0.9F, 0.9F + r.nextFloat() * 0.2F, SoundDesign.layer(SunaSounds.LA_SPADA_FORM, 0.5F, 1.1F));
      if (!VfxSettings.legacy()) {
         VfxDirector.spawn(new LaSpadaCinematic(level, VfxAnchor.of(entity, p.pos()), VfxParams.of(p)));
         if (entity == Minecraft.getInstance().player) {
            CameraEffects.fovPunch(0.02F);
         }
      } else {
         VfxManager.add(new SandSpearEffect(p.pos(), p.dir(), p.extra(), p.seed()));
         boolean atEyes = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(p.pos()) < 2.5600000000000005;
         int n = atEyes ? 0 : Fx.count(5);

         for (int i = 0; i < n; i++) {
            Fx.spawn(
               (ParticleOptions)SunaParticles.GRAIN.get(),
               p.pos().x + Fx.spread(0.2),
               p.pos().y + Fx.spread(0.2),
               p.pos().z + Fx.spread(0.2),
               Fx.spread(0.04),
               0.03,
               Fx.spread(0.04)
            );
         }

         if (entity == Minecraft.getInstance().player) {
            CameraEffects.fovPunch(0.02F);
         }
      }
   }

   private static void laSpadaImpact(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      if (!VfxSettings.legacy()) {
         LaSpadaCinematic.shatter(level, p.seed(), p.pos(), p.dir(), p.extra() == 1);
         SoundDesign.play(p.pos(), SunaSounds.LA_SPADA_IMPACT, 0.8F, 0.9F + Fx.random().nextFloat() * 0.25F);
      } else {
         SandSpearEffect spear = VfxManager.find(SandSpearEffect.class, e -> e.seed == p.seed());
         if (spear != null) {
            spear.finish();
         }

         RandomSource r = Fx.random();
         Vec3 dir = p.dir().lengthSqr() < 1.0E-6 ? Vec3.ZERO : p.dir().normalize();
         int n = Fx.count(p.extra() == 1 ? 14 : 10);

         for (int i = 0; i < n; i++) {
            double s = 0.05 + r.nextDouble() * 0.15;
            Fx.spawn(
               r.nextInt(3) == 0 ? (ParticleOptions)SunaParticles.CLUMP.get() : (ParticleOptions)SunaParticles.GRAIN.get(),
               p.pos().x,
               p.pos().y,
               p.pos().z,
               -dir.x * s + Fx.spread(0.12),
               0.08 + r.nextDouble() * 0.12,
               -dir.z * s + Fx.spread(0.12)
            );
         }

         SoundDesign.play(p.pos(), SunaSounds.LA_SPADA_IMPACT, 0.8F, 0.9F + r.nextFloat() * 0.25F);
      }
   }

   private static void crumble(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      Vec3 at = p.pos();
      long seed = p.seed();
      switch (p.extra()) {
         case 0:
            PesadoCinematic.impact(seed);
            SandBallEffect ball = VfxManager.find(SandBallEffect.class, e -> e.seed == seed);
            if (ball != null) {
               ball.finish();
            }
            break;
         case 1:
            if (!VfxSettings.legacy()) {
               LaSpadaCinematic.shatter(level, seed, at, p.dir(), false);
            }

            SandSpearEffect spear = VfxManager.find(SandSpearEffect.class, e -> e.seed == seed);
            if (spear != null) {
               spear.finish();
            }
            break;
         case 2:
            BarjanCinematic.stop(seed, at);
            SandCrescentEffect crescent = VfxManager.find(SandCrescentEffect.class, e -> e.seed == seed);
            if (crescent != null) {
               crescent.stop(at);
            }
            break;
         default:
            SpadaCinematic.stop(seed, at);
            SandBladeEffect blade = VfxManager.find(SandBladeEffect.class, e -> e.seed == seed);
            if (blade != null) {
               blade.stop(at);
            }
      }

      RandomSource r = Fx.random();
      float size = Mth.clamp(p.power(), 0.4F, 4.0F);
      Vec3 push = p.dir().lengthSqr() < 1.0E-6 ? Vec3.ZERO : p.dir().normalize();
      if (!VfxSettings.legacy()) {
         int tint = SandBlastCinematic.tint(level, at);
         SandCinematic.clouds(r, at.x, at.y, at.z, push.x * 0.06, 0.0, push.z * 0.06, 0.8F + 0.5F * size, 4.0F + 3.0F * size, tint);

         for (int i = 0; i < Fx.count(14.0F * size); i++) {
            double ox = (r.nextDouble() - 0.5) * 0.6 * (double)size;
            double oy = (r.nextDouble() - 0.5) * 0.5 * (double)size;
            double oz = (r.nextDouble() - 0.5) * 0.6 * (double)size;
            SandCinematic.grains(r, at.x + ox, at.y + oy, at.z + oz, push.x * 0.08 + ox * 0.12, 0.04 + r.nextDouble() * 0.08, push.z * 0.08 + oz * 0.12, tint);
         }

         for (int i = 0; i < Fx.count(2.0F + size); i++) {
            SandCinematic.haze(
               r, at.x + (r.nextDouble() - 0.5) * (double)size, at.y, at.z + (r.nextDouble() - 0.5) * (double)size, push.x * 0.02, 0.005, push.z * 0.02, tint
            );
         }
      } else {
         for (int i = 0; i < Fx.count((int)(14.0F * size)); i++) {
            Fx.spawn(
               r.nextInt(3) == 0 ? (ParticleOptions)SunaParticles.CLUMP.get() : (ParticleOptions)SunaParticles.GRAIN.get(),
               at.x + Fx.spread(0.3 * (double)size),
               at.y + Fx.spread(0.3 * (double)size),
               at.z + Fx.spread(0.3 * (double)size),
               push.x * 0.08 + Fx.spread(0.08),
               0.05 + r.nextDouble() * 0.08,
               push.z * 0.08 + Fx.spread(0.08)
            );
         }
      }

      SoundDesign.play(at, SunaSounds.PESADO_IMPACT, 0.6F + 0.15F * size, 1.3F + r.nextFloat() * 0.2F, SoundDesign.layer(SunaSounds.LAYER_SAND, 0.8F, 1.1F));
   }

   private static void encierro(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      if (entity != null && VfxManager.find(SandGripEffect.class, e -> e.seed == p.seed()) == null && EncierroCinematic.of(p.seed()) == null) {
         if (VfxSettings.legacy()) {
            VfxManager.add(new SandGripEffect(entity, p.extra(), (int)p.power(), p.seed()));
         } else {
            VfxDirector.spawn(new EncierroCinematic(level, VfxAnchor.of(entity, entity.position()), VfxParams.of(p)));
         }

         SoundDesign.play(p.pos(), SunaSounds.ENCIERRO_GRIP, 1.0F, 0.9F, SoundDesign.layer(SunaSounds.LAYER_SAND, 0.8F, 0.7F));
      }
   }

   private static void encierroEnd(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      SandGripEffect grip = VfxManager.find(SandGripEffect.class, e -> e.seed == p.seed());
      if (grip != null) {
         grip.release();
      }

      EncierroCinematic cinematic = EncierroCinematic.of(p.seed());
      if (cinematic != null) {
         cinematic.release();
      } else {
         RandomSource r = Fx.random();
         int n = Fx.count(14);

         for (int i = 0; i < n; i++) {
            Fx.spawn(
               r.nextInt(2) == 0 ? (ParticleOptions)SunaParticles.DRY_FLAKE.get() : (ParticleOptions)SunaParticles.GRAIN.get(),
               p.pos().x + Fx.spread(0.5),
               p.pos().y + 0.3 + r.nextDouble() * 1.4,
               p.pos().z + Fx.spread(0.5),
               Fx.spread(0.1),
               0.05,
               Fx.spread(0.1)
            );
         }
      }
   }

   private static void tsunami(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      if (VfxSettings.legacy()) {
         VfxManager.add(new SandWaveEffect(level, p.pos(), p.dir(), p.power(), p.extra(), p.seed()));
      } else {
         VfxDirector.spawn(new TsunamiCinematic(level, VfxAnchor.of(entity, p.pos()), VfxParams.of(p)));
      }

      SoundDesign.play(
         p.pos(),
         SunaSounds.TSUNAMI_LOOP,
         2.0F,
         0.6F,
         SoundDesign.layer(DFSounds.LAYER_RUMBLE, 1.0F, 0.6F),
         SoundDesign.layer(SunaSounds.LAYER_SAND, 1.0F, 0.5F)
      );
      CameraEffects.shakeAt(p.pos(), 0.3F, (double)(p.power() + 12.0F));
   }

   private static void sables(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      if (VfxManager.find(SandstormEffect.class, e -> e.seed == p.seed()) == null
         && VfxDirector.find(SablesCinematic.class, c -> c.anchor().fallback().equals(p.pos())) == null) {
         if (VfxSettings.legacy()) {
            VfxManager.add(new SandstormEffect(p.pos(), p.dir(), p.power(), p.extra(), p.seed()));
         } else {
            VfxDirector.spawn(new SablesCinematic(level, VfxAnchor.of(null, p.pos()), VfxParams.of(p)));
            RandomSource r = Fx.random();
            int tint = SandBlastCinematic.tint(level, p.pos());
            SandCinematic.burst(r, p.pos().add(0.0, 0.3, 0.0), new Vec3(0.0, 1.0, 0.0), 0.5F, 0.4F, 30.0F, 12.0F, tint);
            SandCinematic.decal(r, p.pos().x, p.pos().y, p.pos().z, p.power() * 3.0F, tint);
         }

         SoundDesign.play(
            p.pos(),
            SunaSounds.SABLES_CAST,
            2.0F,
            0.8F,
            SoundDesign.layer(DFSounds.LAYER_RUMBLE, 1.0F, 0.5F),
            SoundDesign.layer(SunaSounds.LAYER_GUST, 1.0F, 0.6F)
         );
      }
   }

   private static void sablesEnd(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      SandstormEffect storm = VfxManager.find(SandstormEffect.class, e -> e.seed == p.seed());
      if (storm != null) {
         storm.blowApart(p.pos());
      }

      SablesCinematic.blowApart(p.seed(), p.pos());
      SoundDesign.play(p.pos(), SunaSounds.SABLES_END, 2.0F, 0.8F, SoundDesign.layer(SunaSounds.LAYER_SAND, 1.0F, 0.6F));
   }

   private static void girasole(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      if (VfxManager.find(SandFunnelEffect.class, e -> e.seed == p.seed()) == null) {
         VfxManager.add(new SandFunnelEffect(level, p.entityId(), p.pos(), p.power(), p.dir(), p.extra(), p.seed()));
         if (!VfxSettings.legacy()) {
            VfxDirector.spawn(new GirasoleCinematic(level, VfxAnchor.of(entity, p.pos()), VfxParams.of(p)));
         }

         SoundDesign.play(
            p.pos(),
            SunaSounds.GIRASOLE_OPEN,
            1.6F,
            0.9F,
            SoundDesign.layer(DFSounds.LAYER_RUMBLE, 0.8F, 0.7F),
            SoundDesign.layer(SunaSounds.LAYER_SAND, 1.0F, 0.6F)
         );
         CameraEffects.shakeAt(p.pos(), 0.2F, (double)(p.power() + 10.0F));
      }
   }

   private static void girasoleEnd(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      SandFunnelEffect funnel = VfxManager.find(SandFunnelEffect.class, e -> e.seed == p.seed());
      if (funnel != null) {
         funnel.close();
      }

      if (!VfxSettings.legacy()) {
         GirasoleCinematic.close(p.seed(), level);
         SoundDesign.play(p.pos(), SunaSounds.GIRASOLE_CLOSE, 1.2F, 0.9F + Fx.random().nextFloat() * 0.1F, SoundDesign.layer(SunaSounds.LAYER_SAND, 0.8F, 0.7F));
      } else {
         RandomSource r = Fx.random();
         Vec3 at = p.pos();
         int n = Fx.count(24);

         for (int i = 0; i < n; i++) {
            double a = r.nextDouble() * Math.PI * 2.0;
            double s = 0.05 + r.nextDouble() * 0.15;
            Fx.spawn(
               r.nextInt(3) == 0 ? (ParticleOptions)SunaParticles.DUST.get() : (ParticleOptions)SunaParticles.GRAIN.get(),
               at.x + Fx.spread(1.2),
               at.y + 0.1,
               at.z + Fx.spread(1.2),
               Math.cos(a) * s,
               0.15 + r.nextDouble() * 0.2,
               Math.sin(a) * s
            );
         }

         SoundDesign.play(at, SunaSounds.GIRASOLE_CLOSE, 1.2F, 0.9F + r.nextFloat() * 0.1F, SoundDesign.layer(SunaSounds.LAYER_SAND, 0.8F, 0.7F));
      }
   }

   private static void awaken(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      column(p.pos(), 40);
      swirl(entity, 30, 1.4);
      SoundDesign.play(
         p.pos(), SunaSounds.AWAKEN, 1.0F, 1.0F, SoundDesign.layer(SunaSounds.LAYER_GUST, 0.8F, 0.7F), SoundDesign.layer(DFSounds.LAYER_RUMBLE, 0.4F, 1.2F)
      );
      if (entity != null && entity == Minecraft.getInstance().player) {
         CameraEffects.shakeAt(p.pos(), 0.4F, 8.0);
      }
   }

   private static void column(Vec3 at, int count) {
      RandomSource r = Fx.random();
      int n = Fx.count(count);

      for (int i = 0; i < n; i++) {
         double a = r.nextDouble() * Math.PI * 2.0;
         double speed = 0.05 + r.nextDouble() * 0.12;
         Fx.spawn(
            r.nextInt(3) == 0 ? (ParticleOptions)SunaParticles.CLUMP.get() : (ParticleOptions)SunaParticles.GRAIN.get(),
            at.x + Fx.spread(0.4),
            at.y + r.nextDouble() * 0.4,
            at.z + Fx.spread(0.4),
            Math.cos(a) * speed,
            0.25 + r.nextDouble() * 0.35,
            Math.sin(a) * speed
         );
      }
   }

   private static void swirl(@Nullable Entity entity, int count, double radius) {
      if (entity != null) {
         RandomSource r = Fx.random();
         int n = Fx.count(count);

         for (int i = 0; i < n; i++) {
            double a = r.nextDouble() * Math.PI * 2.0;
            double y = r.nextDouble() * (double)entity.getBbHeight();
            Fx.spawn(
               (ParticleOptions)SunaParticles.DUST.get(),
               entity.getX() + Math.cos(a) * radius,
               entity.getY() + y,
               entity.getZ() + Math.sin(a) * radius,
               -Math.sin(a) * 0.12,
               0.04,
               Math.cos(a) * 0.12
            );
         }
      }
   }

   private static void desiccate(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      RandomSource r = Fx.random();
      Vec3 c = p.pos();
      double radius = Math.max(1.0, (double)p.power());
      int n = Fx.count((int)Math.ceil(radius * radius * 1.6));

      for (int i = 0; i < n; i++) {
         double a = r.nextDouble() * Math.PI * 2.0;
         double d = Math.sqrt(r.nextDouble()) * radius;
         double x = c.x + Math.cos(a) * d;
         double z = c.z + Math.sin(a) * d;
         double y = c.y + 0.1 + r.nextDouble() * 0.4;
         Fx.spawn(
            r.nextInt(3) == 0 ? (ParticleOptions)SunaParticles.DRY_FLAKE.get() : (ParticleOptions)SunaParticles.DUST.get(),
            x,
            y,
            z,
            Fx.spread(0.02),
            0.03 + r.nextDouble() * 0.03,
            Fx.spread(0.02)
         );
      }

      SoundDesign.play(c, SunaSounds.DESICCATE, 0.9F, 0.9F + r.nextFloat() * 0.2F, SoundDesign.layer(SunaSounds.LAYER_HISS, 0.7F, 0.8F));
   }

   private static void burst(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      RandomSource r = Fx.random();
      Vec3 dir = p.dir();
      int n = Fx.count((int)(16.0F * Math.max(0.5F, p.power())));

      for (int i = 0; i < n; i++) {
         double speed = 0.25 + r.nextDouble() * 0.35;
         Fx.spawn(
            r.nextInt(4) == 0 ? (ParticleOptions)SunaParticles.DUST.get() : (ParticleOptions)SunaParticles.GRAIN.get(),
            p.pos().x,
            p.pos().y,
            p.pos().z,
            dir.x * speed + Fx.spread(0.06),
            dir.y * speed + Fx.spread(0.06) + 0.03,
            dir.z * speed + Fx.spread(0.06)
         );
      }

      SoundDesign.play(p.pos(), SunaSounds.TECHNIQUE, 0.8F, 1.0F, SoundDesign.layer(SunaSounds.LAYER_SAND, 0.6F, 1.1F));
   }

   private SunaClientFx() {
   }
}
