package com.devilfruits.fruits.gura_gura.client;

import com.devilfruits.client.ClientFruitState;
import com.devilfruits.client.fx.ChargeVisuals;
import com.devilfruits.client.fx.ClientFxRegistry;
import com.devilfruits.client.fx.ClientFxUtil;
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
import com.devilfruits.fruits.gura_gura.GuraAbilities;
import com.devilfruits.fruits.gura_gura.GuraFx;
import com.devilfruits.fruits.gura_gura.GuraParticles;
import com.devilfruits.fruits.gura_gura.GuraSounds;
import com.devilfruits.fruits.gura_gura.client.vfx.AuraCinematic;
import com.devilfruits.fruits.gura_gura.client.vfx.AuraEffect;
import com.devilfruits.fruits.gura_gura.client.vfx.AwakeningCinematic;
import com.devilfruits.fruits.gura_gura.client.vfx.BubbleEffect;
import com.devilfruits.fruits.gura_gura.client.vfx.FissureEffect;
import com.devilfruits.fruits.gura_gura.client.vfx.FistCrackCinematic;
import com.devilfruits.fruits.gura_gura.client.vfx.KabutowariCinematic;
import com.devilfruits.fruits.gura_gura.client.vfx.KaishinCinematic;
import com.devilfruits.fruits.gura_gura.client.vfx.LaunchTrailCinematic;
import com.devilfruits.fruits.gura_gura.client.vfx.LaunchTrailEffect;
import com.devilfruits.fruits.gura_gura.client.vfx.PlaneCrackEffect;
import com.devilfruits.fruits.gura_gura.client.vfx.QuakeDraw;
import com.devilfruits.fruits.gura_gura.client.vfx.QuakeStrikes;
import com.devilfruits.fruits.gura_gura.client.vfx.ShimaCinematic;
import com.devilfruits.fruits.gura_gura.client.vfx.TremorEffect;
import com.devilfruits.registry.DFParticles;
import com.devilfruits.registry.DFSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class GuraClientFx {
   public static void register() {
      ClientFxRegistry.register(GuraFx.AWAKEN, GuraClientFx::fruitAwaken);
      ClientFxRegistry.register(GuraFx.LEVEL_UP, (level, entity, p) -> levelUp(entity, p));
      ClientFxRegistry.register(GuraFx.REINCARNATE, (level, entity, p) -> reincarnate(p));
      ClientFxRegistry.register(GuraFx.KAISHIN, GuraClientFx::kaishin);
      ClientFxRegistry.register(GuraFx.GEKISHIN_HIT, GuraClientFx::gekishin);
      ClientFxRegistry.register(GuraFx.LAUNCH_TRAIL, (level, entity, p) -> {
         if (entity != null) {
            if (VfxSettings.legacy()) {
               VfxManager.add(new LaunchTrailEffect(entity, p.extra(), p.seed()));
            } else {
               VfxDirector.spawn(new LaunchTrailCinematic(level, VfxAnchor.of(entity, p.pos()), VfxParams.of(p)));
            }
         }
      });
      ClientFxRegistry.register(GuraFx.IMPACT_RING, (level, entity, p) -> impactRing(level, p));
      ClientFxRegistry.register(GuraFx.KABUTOWARI_LEAP, GuraClientFx::kabutowariLeap);
      ClientFxRegistry.register(GuraFx.KABUTOWARI_SLAM, GuraClientFx::kabutowariSlam);
      ClientFxRegistry.register(GuraFx.SHIMA_GRAB, GuraClientFx::shimaGrab);
      ClientFxRegistry.register(GuraFx.SHIMA_TREMOR, (level, entity, p) -> {
         int interval = Math.max(2, (int)Math.round(p.dir().y));
         VfxManager.add(new TremorEffect(p.pos(), p.power(), p.extra(), interval, p.seed()));
         if (!VfxSettings.legacy()) {
            ShimaCinematic shima = entity != null ? VfxDirector.find(ShimaCinematic.class, c -> c.follows(entity)) : null;
            if (shima == null) {
               shima = VfxDirector.spawn(new ShimaCinematic(level, VfxAnchor.of(entity, p.pos()), VfxParams.of(p), false));
            }

            shima.tremor(p.pos(), p.power(), p.extra(), interval);
         }
      });
      ClientFxRegistry.register(GuraFx.KAISHO_CAST, GuraClientFx::kaisho);
      ClientFxRegistry.register(GuraFx.AURA_ON, (level, entity, p) -> auraOn(entity));
      ClientFxRegistry.register(GuraFx.AURA_OFF, (level, entity, p) -> auraOff(entity));
      ClientFxRegistry.register(GuraFx.AURA_DEFLECT, (level, entity, p) -> auraDeflect(level, p));
      ClientFxRegistry.register(GuraFx.AURA_HIT, (level, entity, p) -> auraHit(level, p));
      ClientFxRegistry.register(GuraFx.SLASH_CAST, (level, entity, p) -> slashCast(level, entity, p));
      ClientFxRegistry.register(GuraFx.SLASH_IMPACT, (level, entity, p) -> slashImpact(level, p));
      ChargeVisuals.register(GuraAbilities.KAISHIN.id(), bubbleCharge(false));
      ChargeVisuals.register(GuraAbilities.GEKISHIN.id(), bubbleCharge(true));
   }

   private static ChargeVisuals.ChargeVisual bubbleCharge(final boolean gekishin) {
      return new ChargeVisuals.ChargeVisual() {
         @Override
         public void start(ClientLevel level, Entity entity, ChargePayload payload) {
            if (!VfxSettings.legacy() && entity != null) {
               FistCrackCinematic old = FistCrackCinematic.of(entity);
               if (old != null) {
                  old.stop();
               }

               float duration = Math.max(1.0F, payload.duration());
               VfxDirector.spawn(
                  new FistCrackCinematic(
                     level,
                     VfxAnchor.of(entity, entity.position()),
                     new VfxParams(entity.position(), Vec3.ZERO, gekishin ? 0.5F : 0.85F, payload.seed(), Math.round(duration))
                  )
               );
               SoundDesign.play(
                  entity.position().add(0.0, 1.2, 0.0),
                  GuraSounds.KAISHIN_CHARGE,
                  gekishin ? 0.6F : 0.9F,
                  gekishin ? 1.4F : 1.0F,
                  SoundDesign.layer(GuraSounds.LAYER_CRACK, 0.5F, 1.3F)
               );
            } else {
               GuraClientFx.chargeStart(entity, payload, gekishin);
            }
         }

         @Override
         public void stop(ClientLevel level, Entity entity, boolean released) {
            if (!released) {
               GuraClientFx.chargeStop(entity);
               FistCrackCinematic bubble = entity != null ? FistCrackCinematic.of(entity) : null;
               if (bubble != null) {
                  bubble.stop();
               }
            }
         }
      };
   }

   private static boolean isLocal(Entity entity) {
      return ClientFxUtil.isLocal(entity);
   }

   private static boolean firstPersonCaster(Entity caster) {
      return ClientFxUtil.firstPersonCaster(caster);
   }

   private static void flash(Vec3 at, double size) {
      Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
      double d = cam.distanceTo(at);
      if (!(d < 1.6)) {
         double scale = Math.min(1.0, 0.35 + (d - 1.6) / 3.0);
         Fx.spawn((ParticleOptions)GuraParticles.IMPACT_FLASH.get(), at.x, at.y, at.z, size * scale, 0.0, 0.0);
      }
   }

   private static void fruitAwaken(ClientLevel level, Entity entity, FxPayload p) {
      if (!VfxSettings.legacy()) {
         VfxDirector.spawn(new AwakeningCinematic(level, VfxAnchor.of(entity, p.pos()), VfxParams.of(p)));
      } else {
         boolean death = p.extra() == 1;
         Vec3 center = entity != null ? entity.position().add(0.0, (double)entity.getBbHeight() * 0.55, 0.0) : p.pos();
         if (entity != null) {
            VfxManager.add(new BubbleEffect(entity, BubbleEffect.Anchor.BODY, p.seed(), death ? 1.6F : 1.35F, 10.0F, 14, 10, 10));
         }

         VfxManager.add(QuakeDraw.shockwave(ShockwaveEffect.Mode.GROUND, entity != null ? entity.position() : p.pos(), Vec3.ZERO, 6.0F, 0.0F, 16, 3, 0.8F));

         for (int i = 0; i < Fx.count(30); i++) {
            Fx.spawn(
               (ParticleOptions)GuraParticles.CRACK_SPARK.get(),
               center.x + Fx.spread(1.2),
               center.y + Fx.spread(1.2),
               center.z + Fx.spread(1.2),
               Fx.spread(0.15),
               Fx.spread(0.15),
               Fx.spread(0.15)
            );
         }

         if (isLocal(entity)) {
            CameraEffects.flash(death ? 1.0F : 0.65F);
            CameraEffects.shake(0.55F);
            CameraEffects.fovPunch(0.12F);
         } else {
            CameraEffects.shakeAt(center, 0.4F, 20.0);
         }

         SoundDesign.play(
            center,
            DFSounds.FRUIT_AWAKEN,
            1.4F,
            death ? 0.6F : 0.85F,
            SoundDesign.layer(GuraSounds.LAYER_SHATTER, 1.2F, 0.6F),
            SoundDesign.layer(DFSounds.LAYER_THUNDER, 0.6F, 0.9F),
            SoundDesign.layer(GuraSounds.LAYER_CRACK, 1.0F, 0.7F)
         );
         if (death) {
            SoundDesign.layerOnly(center, DFSounds.LAYER_BOOM, 1.5F, 0.6F);
         }
      }
   }

   private static void levelUp(Entity entity, FxPayload p) {
      if (entity != null) {
         VfxManager.add(new BubbleEffect(entity, BubbleEffect.Anchor.BODY, p.seed(), 1.1F, 6.0F, 10, 7, 7));

         for (int i = 0; i < Fx.count(16); i++) {
            Fx.spawn(
               (ParticleOptions)GuraParticles.CRACK_SPARK.get(),
               entity.getX() + Fx.spread(0.8),
               entity.getY() + Fx.random().nextDouble() * 2.0,
               entity.getZ() + Fx.spread(0.8),
               0.0,
               0.08,
               0.0
            );
         }

         SoundDesign.play(entity.position(), DFSounds.MASTERY_LEVEL_UP, 1.0F, 0.8F, SoundDesign.layer(GuraSounds.LAYER_SHATTER_HIGH, 0.8F, 1.2F));
      }
   }

   private static void reincarnate(FxPayload p) {
      Vec3 c = p.pos();

      for (int i = 0; i < Fx.count(24); i++) {
         Fx.spawn((ParticleOptions)GuraParticles.CRACK_SPARK.get(), c.x + Fx.spread(0.6), c.y + Fx.random().nextDouble(), c.z + Fx.spread(0.6), 0.0, 0.06, 0.0);
      }

      flash(c.add(0.0, 0.5, 0.0), 1.2);
      SoundDesign.play(c, DFSounds.FRUIT_REINCARNATE, 1.2F, 1.0F, SoundDesign.layer(GuraSounds.LAYER_SHATTER_HIGH, 0.8F, 0.8F));
   }

   private static void chargeStart(Entity entity, ChargePayload p, boolean gekishin) {
      if (entity != null) {
         BubbleEffect old = VfxManager.find(BubbleEffect.class, b -> b.entityId() == entity.getId());
         if (old != null) {
            old.stop();
         }

         float duration = Math.max(1.0F, p.duration());
         VfxManager.add(
            new BubbleEffect(
               entity,
               BubbleEffect.Anchor.FIST,
               p.seed(),
               gekishin ? 0.45F : 0.8F,
               duration,
               gekishin ? Math.round(duration) + 8 : -1,
               gekishin ? 5 : 7,
               gekishin ? 6 : 9
            )
         );
         SoundDesign.play(
            entity.position().add(0.0, 1.2, 0.0),
            GuraSounds.KAISHIN_CHARGE,
            gekishin ? 0.6F : 0.9F,
            gekishin ? 1.4F : 1.0F,
            SoundDesign.layer(GuraSounds.LAYER_CRACK, 0.5F, 1.3F)
         );
      }
   }

   private static void chargeStop(Entity entity) {
      if (entity != null) {
         BubbleEffect bubble = VfxManager.find(BubbleEffect.class, b -> b.entityId() == entity.getId());
         if (bubble != null) {
            bubble.stop();
         }
      }
   }

   private static void shatterBubble(Entity entity, Vec3 dir) {
      if (entity != null) {
         BubbleEffect bubble = VfxManager.find(BubbleEffect.class, b -> b.entityId() == entity.getId());
         if (bubble != null) {
            bubble.shatter(dir.scale(0.6));
         }

         if (isLocal(entity)) {
            ClientFruitState.stopCharge();
         }
      }
   }

   private static void kaishin(ClientLevel level, Entity caster, FxPayload p) {
      float c = p.power();
      Vec3 dir = p.dir().normalize();
      Vec3 fist = p.pos();
      float length = (float)p.extra() / 16.0F;
      float spread = (float)Math.tan(Math.toRadians(28.0 * (1.0 + 0.35 * (double)c)));
      shatterBubble(caster, dir);
      boolean fp = firstPersonCaster(caster);
      if (!VfxSettings.legacy()) {
         VfxDirector.spawn(new KaishinCinematic(level, VfxAnchor.of(caster, fist), VfxParams.of(p)));
         kaishinSound(fist, c);
      } else {
         Vec3 crack = fist.add(dir.scale(fp ? 2.6 : 1.0 + 0.6 * (double)c));
         VfxManager.add(
            new PlaneCrackEffect(
               crack,
               dir.scale(-1.0),
               (0.8F + 1.6F * c) * (fp ? 0.6F : 1.0F),
               p.seed(),
               true,
               2,
               3 + Math.round(3.0F * c),
               6,
               14 + Math.round(30.0F * c),
               false,
               dir.scale(0.35 + 0.3 * (double)c)
            )
         );
         VfxManager.add(
            QuakeDraw.shockwave(ShockwaveEffect.Mode.CONE, fist, dir, length, spread, 9 + Math.round(6.0F * c), 3 + Math.round(2.0F * c), 0.7F + 0.3F * c)
         );
         flash(crack, (0.8 + (double)c) * (fp ? 0.5 : 1.0));
         MutableBlockPos pos = new MutableBlockPos();

         for (float t = 1.0F; t < length; t++) {
            Vec3 point = fist.add(dir.scale((double)t));
            double r = (double)(t * spread);

            for (int i = 0; i < Fx.count(2); i++) {
               Fx.spawn(
                  (ParticleOptions)GuraParticles.GLASS_SHARD.get(),
                  point.x + Fx.spread(r),
                  point.y + Fx.spread(r),
                  point.z + Fx.spread(r),
                  dir.x * 0.4,
                  dir.y * 0.4 + 0.05,
                  dir.z * 0.4
               );
            }

            if (Fx.random().nextFloat() < 0.6F) {
               Fx.spawn(
                  (ParticleOptions)DFParticles.DUST_PUFF.get(),
                  point.x + Fx.spread(r),
                  point.y + Fx.spread(r * 0.5),
                  point.z + Fx.spread(r),
                  dir.x * 0.15,
                  0.02,
                  dir.z * 0.15
               );
            }

            BlockState ground = Fx.groundAt(level, point.x, point.y - 1.0, point.z, pos);
            if (ground != null && point.y - (double)(pos.getY() + 1) < 2.5) {
               for (int i = 0; i < Fx.count(2 + Math.round(3.0F * c)); i++) {
                  Fx.spawn(
                     new BlockParticleOption(DFParticles.BLOCK_DEBRIS.get(), ground),
                     point.x + Fx.spread(r),
                     (double)pos.getY() + 1.05,
                     point.z + Fx.spread(r),
                     dir.x * 0.3 + Fx.spread(0.1),
                     0.25 + Fx.random().nextDouble() * 0.3 * (double)(1.0F + c),
                     dir.z * 0.3 + Fx.spread(0.1)
                  );
               }
            }
         }

         if (isLocal(caster)) {
            CameraEffects.shake(0.25F + 0.4F * c);
            CameraEffects.fovPunch(0.04F + 0.07F * c);
            if (c > 0.75F) {
               CameraEffects.flash(0.3F * c);
            }
         } else {
            CameraEffects.shakeAt(fist, 0.25F + 0.4F * c, (double)(18.0F + 14.0F * c));
         }

         kaishinSound(fist, c);
      }
   }

   private static void kaishinSound(Vec3 fist, float c) {
      SoundDesign.play(
         fist,
         GuraSounds.KAISHIN_RELEASE,
         1.0F + 0.6F * c,
         1.0F - 0.25F * c,
         SoundDesign.layer(GuraSounds.LAYER_SHATTER, 1.0F + 0.5F * c, 0.85F - 0.2F * c),
         SoundDesign.layer(DFSounds.LAYER_BOOM, 0.4F + 0.8F * c, 0.95F - 0.25F * c),
         SoundDesign.layer(GuraSounds.LAYER_SHATTER_HIGH, 0.5F, 0.9F)
      );
      if (c > 0.6F) {
         SoundDesign.layerOnly(fist, DFSounds.LAYER_SONIC, 0.7F * c, 1.1F);
      }
   }

   private static void gekishin(ClientLevel level, Entity caster, FxPayload p) {
      Vec3 dir = p.dir().normalize();
      Vec3 hit = p.pos();
      boolean target = p.extra() >= 0;
      if (!VfxSettings.legacy()) {
         QuakeStrikes.gekishin(level, caster, hit, dir, target, p.seed());
         Entity struck = target ? level.getEntity(p.extra()) : null;
         if (isLocal(struck)) {
            CameraEffects.shake(0.85F);
            CameraEffects.flash(0.45F);
         }

         gekishinSound(hit, target);
      } else {
         shatterBubble(caster, dir);
         VfxManager.add(new PlaneCrackEffect(hit, dir.scale(-1.0), target ? 1.8F : 1.0F, p.seed(), true, 1, 4, 6, target ? 36 : 16, false, dir.scale(0.45)));
         VfxManager.add(QuakeDraw.shockwave(ShockwaveEffect.Mode.CONE, hit.subtract(dir.scale(0.5)), dir, target ? 5.0F : 3.0F, 0.5F, 8, 3, 0.9F));
         flash(hit, target ? 1.8 : 1.0);

         for (int i = 0; i < Fx.count(14); i++) {
            Fx.spawn(
               (ParticleOptions)GuraParticles.CRACK_SPARK.get(),
               hit.x,
               hit.y,
               hit.z,
               dir.x * 0.4 + Fx.spread(0.3),
               dir.y * 0.4 + Fx.spread(0.3),
               dir.z * 0.4 + Fx.spread(0.3)
            );
         }

         Entity victim = target ? level.getEntity(p.extra()) : null;
         if (isLocal(caster)) {
            CameraEffects.shake(target ? 0.5F : 0.3F);
            CameraEffects.fovPunch(target ? 0.09F : 0.05F);
            if (target) {
               CameraEffects.flash(0.25F);
            }
         } else if (isLocal(victim)) {
            CameraEffects.shake(0.85F);
            CameraEffects.flash(0.45F);
         } else {
            CameraEffects.shakeAt(hit, 0.4F, 20.0);
         }

         gekishinSound(hit, target);
      }
   }

   private static void gekishinSound(Vec3 hit, boolean target) {
      SoundDesign.play(
         hit,
         GuraSounds.GEKISHIN,
         1.3F,
         1.0F,
         SoundDesign.layer(DFSounds.LAYER_IMPACT, 1.0F, 0.8F),
         SoundDesign.layer(GuraSounds.LAYER_SHATTER, 1.0F, 0.75F),
         SoundDesign.layer(DFSounds.LAYER_BOOM, target ? 0.8F : 0.4F, 0.9F)
      );
   }

   private static void impactRing(ClientLevel level, FxPayload p) {
      Vec3 at = p.pos();
      float radius = p.power();
      if (!VfxSettings.legacy()) {
         QuakeStrikes.landing(level, at, radius, p.seed());
         SoundDesign.play(
            at, GuraSounds.GEKISHIN, 0.9F, 0.7F, SoundDesign.layer(GuraSounds.LAYER_CRACK, 1.0F, 0.6F), SoundDesign.layer(DFSounds.LAYER_ANVIL, 0.5F, 0.6F)
         );
      } else {
         VfxManager.add(QuakeDraw.shockwave(ShockwaveEffect.Mode.GROUND, at, Vec3.ZERO, radius * 1.6F, 0.0F, 10, 2, 0.9F));
         VfxManager.add(
            new PlaneCrackEffect(at.add(0.0, 0.05, 0.0), new Vec3(0.0, 1.0, 0.0), radius, p.seed() ^ 77L, true, 2, 14, 10, 10, false, new Vec3(0.0, 0.15, 0.0))
         );
         MutableBlockPos pos = new MutableBlockPos();
         BlockState ground = Fx.groundAt(level, at.x, at.y, at.z, pos);

         for (int i = 0; i < Fx.count(18); i++) {
            if (ground != null) {
               Fx.spawn(
                  new BlockParticleOption(DFParticles.BLOCK_DEBRIS.get(), ground),
                  at.x + Fx.spread((double)radius),
                  (double)pos.getY() + 1.05,
                  at.z + Fx.spread((double)radius),
                  Fx.spread(0.2),
                  0.3 + Fx.random().nextDouble() * 0.4,
                  Fx.spread(0.2)
               );
            }

            if (i % 3 == 0) {
               Fx.spawn(
                  (ParticleOptions)DFParticles.DUST_PUFF.get(),
                  at.x + Fx.spread((double)radius),
                  at.y + 0.2,
                  at.z + Fx.spread((double)radius),
                  Fx.spread(0.05),
                  0.03,
                  Fx.spread(0.05)
               );
            }
         }

         CameraEffects.shakeAt(at, 0.45F, 18.0);
         SoundDesign.play(
            at, GuraSounds.GEKISHIN, 0.9F, 0.7F, SoundDesign.layer(GuraSounds.LAYER_CRACK, 1.0F, 0.6F), SoundDesign.layer(DFSounds.LAYER_ANVIL, 0.5F, 0.6F)
         );
      }
   }

   private static void kabutowariLeap(ClientLevel level, Entity caster, FxPayload p) {
      Vec3 at = caster != null ? caster.position() : p.pos();
      if (!VfxSettings.legacy()) {
         QuakeStrikes.leap(level, caster, at, p.seed());
         SoundDesign.layerOnly(at, DFSounds.LAYER_WIND, 0.9F, 0.8F);
      } else {
         VfxManager.add(QuakeDraw.shockwave(ShockwaveEffect.Mode.GROUND, at, Vec3.ZERO, 2.5F, 0.0F, 7, 1, 0.6F));

         for (int i = 0; i < Fx.count(10); i++) {
            Fx.spawn(
               (ParticleOptions)DFParticles.DUST_PUFF.get(), at.x + Fx.spread(0.8), at.y + 0.1, at.z + Fx.spread(0.8), Fx.spread(0.08), 0.02, Fx.spread(0.08)
            );
         }

         SoundDesign.layerOnly(at, DFSounds.LAYER_WIND, 0.9F, 0.8F);
      }
   }

   private static void kabutowariSlam(ClientLevel level, Entity caster, FxPayload p) {
      Vec3 at = p.pos();
      float length = p.power();
      if (!VfxSettings.legacy()) {
         VfxDirector.spawn(new KabutowariCinematic(level, VfxAnchor.of(caster, at), VfxParams.of(p)));
         kabutowariSound(at);
      } else {
         VfxManager.add(new FissureEffect(level, at, p.seed(), p.extra(), length));
         VfxManager.add(QuakeDraw.shockwave(ShockwaveEffect.Mode.GROUND, at, Vec3.ZERO, length * 0.9F, 0.0F, 12, 3, 1.0F));
         VfxManager.add(
            new PlaneCrackEffect(at.add(0.0, 0.06, 0.0), new Vec3(0.0, 1.0, 0.0), 2.6F, p.seed() ^ 991L, true, 2, 30, 20, 8, false, new Vec3(0.0, 0.2, 0.0))
         );
         flash(at.add(0.0, 0.3, 0.0), 2.2);
         MutableBlockPos pos = new MutableBlockPos();
         BlockState ground = Fx.groundAt(level, at.x, at.y - 1.0, at.z, pos);

         for (int i = 0; i < Fx.count(40); i++) {
            double a = Fx.random().nextDouble() * Math.PI * 2.0;
            double r = Fx.random().nextDouble() * 2.5;
            if (ground != null) {
               Fx.spawn(
                  new BlockParticleOption(DFParticles.BLOCK_DEBRIS.get(), ground),
                  at.x + Math.cos(a) * r,
                  at.y + 0.1,
                  at.z + Math.sin(a) * r,
                  Math.cos(a) * 0.15,
                  0.4 + Fx.random().nextDouble() * 0.5,
                  Math.sin(a) * 0.15
               );
            }

            if (i % 4 == 0) {
               Fx.spawn(
                  (ParticleOptions)DFParticles.DUST_PUFF.get(),
                  at.x + Math.cos(a) * r,
                  at.y + 0.2,
                  at.z + Math.sin(a) * r,
                  Math.cos(a) * 0.12,
                  0.02,
                  Math.sin(a) * 0.12
               );
            }
         }

         if (isLocal(caster)) {
            CameraEffects.shake(0.8F);
            CameraEffects.fovPunch(0.1F);
            CameraEffects.flash(0.3F);
         } else {
            CameraEffects.shakeAt(at, 0.75F, 32.0);
         }

         kabutowariSound(at);
      }
   }

   private static void kabutowariSound(Vec3 at) {
      SoundDesign.play(
         at,
         GuraSounds.KABUTOWARI,
         1.8F,
         0.9F,
         SoundDesign.layer(DFSounds.LAYER_BOOM, 1.4F, 0.6F),
         SoundDesign.layer(DFSounds.LAYER_ANVIL, 0.8F, 0.5F),
         SoundDesign.layer(GuraSounds.LAYER_CRACK, 1.5F, 0.6F),
         SoundDesign.layer(DFSounds.LAYER_RUMBLE, 1.0F, 0.6F)
      );
   }

   private static void shimaGrab(ClientLevel level, Entity caster, FxPayload p) {
      Vec3 at = p.pos();
      float radius = p.power();
      if (!VfxSettings.legacy()) {
         VfxDirector.spawn(new ShimaCinematic(level, VfxAnchor.of(caster, at), VfxParams.of(p), true));
         shimaSound(at);
      } else {
         for (int i = 0; i < Fx.count(30); i++) {
            Fx.spawnFar(
               (ParticleOptions)GuraParticles.CRACK_SPARK.get(),
               at.x + Fx.spread((double)radius * 0.4),
               at.y + Fx.spread((double)radius * 0.4),
               at.z + Fx.spread((double)radius * 0.4),
               Fx.spread(0.3),
               Fx.spread(0.3),
               Fx.spread(0.3)
            );
         }

         Fx.spawnFar((ParticleOptions)GuraParticles.IMPACT_FLASH.get(), at.x, at.y, at.z, 3.0, 0.0, 0.0);
         CameraEffects.flash(0.4F);
         CameraEffects.shakeAt(at, 0.5F, 96.0);
         shimaSound(at);
      }
   }

   private static void shimaSound(Vec3 at) {
      SoundDesign.play(
         at,
         GuraSounds.SHIMA_YURASHI,
         4.0F,
         0.6F,
         true,
         SoundDesign.layer(GuraSounds.LAYER_SHATTER, 4.0F, 0.5F),
         SoundDesign.layer(DFSounds.LAYER_ROAR, 3.0F, 0.5F),
         SoundDesign.layer(DFSounds.LAYER_THUNDER, 4.0F, 0.6F),
         SoundDesign.layer(DFSounds.LAYER_SONIC, 2.0F, 0.6F)
      );
   }

   private static void kaisho(ClientLevel level, Entity caster, FxPayload p) {
      boolean water = p.power() > 0.5F;
      Vec3 dir = p.dir().normalize();
      Vec3 at = p.pos();
      boolean fp = firstPersonCaster(caster);
      if (!VfxSettings.legacy()) {
         QuakeStrikes.kaisho(level, caster, at, dir, water, p.seed());
         kaishoSound(at, water);
      } else {
         Vec3 crack = at.add(dir.scale(fp ? 2.8 : 1.1));
         VfxManager.add(new PlaneCrackEffect(crack, dir.scale(-1.0), fp ? 1.0F : 1.6F, p.seed(), true, 2, 4, 6, 24, false, dir.scale(0.4)));
         VfxManager.add(QuakeDraw.shockwave(ShockwaveEffect.Mode.CONE, at, dir, 7.0F, 0.75F, 10, 4, 1.0F));
         flash(crack, fp ? 0.8 : 1.6);
         if (isLocal(caster)) {
            CameraEffects.shake(0.5F);
            CameraEffects.fovPunch(0.08F);
         } else {
            CameraEffects.shakeAt(at, 0.5F, 32.0);
         }

         kaishoSound(at, water);
      }
   }

   private static void kaishoSound(Vec3 at, boolean water) {
      if (water) {
         SoundDesign.play(
            at,
            GuraSounds.KAISHO,
            2.0F,
            0.6F,
            SoundDesign.layer(DFSounds.LAYER_SPLASH, 1.5F, 0.6F),
            SoundDesign.layer(DFSounds.LAYER_BOOM, 1.0F, 0.6F),
            SoundDesign.layer(DFSounds.LAYER_WIND, 1.0F, 0.5F)
         );
      } else {
         SoundDesign.play(
            at,
            GuraSounds.KABUTOWARI,
            1.4F,
            1.1F,
            SoundDesign.layer(DFSounds.LAYER_RUMBLE, 1.2F, 0.6F),
            SoundDesign.layer(DFSounds.LAYER_BOOM, 0.9F, 0.7F),
            SoundDesign.layer(DFSounds.LAYER_WIND, 1.0F, 0.5F)
         );
      }
   }

   private static void auraOn(Entity entity) {
      if (entity != null) {
         if (!VfxSettings.legacy()) {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null && AuraCinematic.of(entity) == null) {
               VfxDirector.spawn(
                  new AuraCinematic(level, VfxAnchor.of(entity, entity.position()), new VfxParams(entity.position(), Vec3.ZERO, 0.0F, (long)entity.getId(), 0))
               );
               SoundDesign.play(
                  entity.position().add(0.0, 1.0, 0.0), GuraSounds.KAISHIN_CHARGE, 0.8F, 0.7F, SoundDesign.layer(GuraSounds.LAYER_SHATTER_HIGH, 0.5F, 0.7F)
               );
            }
         } else if (VfxManager.find(AuraEffect.class, a -> a.entityId() == entity.getId()) == null) {
            VfxManager.add(new AuraEffect(entity));
            VfxManager.add(QuakeDraw.shockwave(ShockwaveEffect.Mode.GROUND, entity.position(), Vec3.ZERO, 3.0F, 0.0F, 8, 2, 0.6F));
            SoundDesign.play(
               entity.position().add(0.0, 1.0, 0.0), GuraSounds.KAISHIN_CHARGE, 0.8F, 0.7F, SoundDesign.layer(GuraSounds.LAYER_SHATTER_HIGH, 0.5F, 0.7F)
            );
         }
      }
   }

   private static void auraOff(Entity entity) {
      if (entity != null) {
         AuraEffect aura = VfxManager.find(AuraEffect.class, a -> a.entityId() == entity.getId());
         if (aura != null) {
            aura.stop();
         }

         AuraCinematic shell = AuraCinematic.of(entity);
         if (shell != null) {
            shell.stop();
         }
      }
   }

   private static void auraDeflect(ClientLevel level, FxPayload p) {
      Vec3 at = p.pos();
      Vec3 dir = p.dir().lengthSqr() > 1.0E-6 ? p.dir().normalize() : new Vec3(0.0, 0.0, 1.0);
      if (!VfxSettings.legacy()) {
         QuakeStrikes.deflect(level, at, p.dir(), Double.doubleToLongBits(at.x * 31.0 + at.z));
         SoundDesign.play(
            at, GuraSounds.AURA_DEFLECT, 1.0F, 1.0F + Fx.random().nextFloat() * 0.2F, SoundDesign.layer(GuraSounds.LAYER_SHATTER_HIGH, 0.7F, 1.3F)
         );
      } else {
         VfxManager.add(new PlaneCrackEffect(at, dir, 0.6F, Double.doubleToLongBits(at.x * 31.0 + at.z), false, 1, 3, 4, 10, false, dir.scale(-0.1)));
         flash(at, 0.6);
         SoundDesign.play(
            at, GuraSounds.AURA_DEFLECT, 1.0F, 1.0F + Fx.random().nextFloat() * 0.2F, SoundDesign.layer(GuraSounds.LAYER_SHATTER_HIGH, 0.7F, 1.3F)
         );
      }
   }

   private static void auraHit(ClientLevel level, FxPayload p) {
      Vec3 at = p.pos();
      Vec3 dir = p.dir().lengthSqr() > 1.0E-6 ? p.dir().normalize() : new Vec3(0.0, 1.0, 0.0);
      if (!VfxSettings.legacy()) {
         QuakeStrikes.auraHit(level, at, dir, p.seed());
         SoundDesign.layerOnly(at, GuraSounds.LAYER_CRACK, 0.8F, 1.1F);
         SoundDesign.layerOnly(at, GuraSounds.LAYER_SHATTER_HIGH, 0.5F, 1.2F);
      } else {
         VfxManager.add(new PlaneCrackEffect(at, dir.scale(-1.0), 0.7F, p.seed(), false, 1, 3, 4, 8, false, dir.scale(0.2)));

         for (int i = 0; i < Fx.count(6); i++) {
            Fx.spawn(
               (ParticleOptions)GuraParticles.CRACK_SPARK.get(), at.x, at.y, at.z, dir.x * 0.3 + Fx.spread(0.2), Fx.spread(0.2), dir.z * 0.3 + Fx.spread(0.2)
            );
         }

         SoundDesign.layerOnly(at, GuraSounds.LAYER_CRACK, 0.8F, 1.1F);
         SoundDesign.layerOnly(at, GuraSounds.LAYER_SHATTER_HIGH, 0.5F, 1.2F);
         CameraEffects.shakeAt(at, 0.12F, 6.0);
      }
   }

   private static void slashCast(ClientLevel level, Entity caster, FxPayload p) {
      Vec3 at = p.pos();
      if (!VfxSettings.legacy()) {
         QuakeStrikes.slashCast(level, caster, at, p.dir().normalize(), p.seed());
         SoundDesign.play(
            at,
            GuraSounds.WEAPON_SLASH,
            1.0F,
            1.0F,
            SoundDesign.layer(GuraSounds.LAYER_SHATTER_HIGH, 0.7F, 1.0F),
            SoundDesign.layer(DFSounds.LAYER_WIND, 0.7F, 1.2F)
         );
      } else {
         for (int i = 0; i < Fx.count(8); i++) {
            Fx.spawn(
               (ParticleOptions)GuraParticles.CRACK_SPARK.get(),
               at.x,
               at.y,
               at.z,
               p.dir().x * 0.4 + Fx.spread(0.2),
               Fx.spread(0.2),
               p.dir().z * 0.4 + Fx.spread(0.2)
            );
         }

         if (isLocal(caster)) {
            CameraEffects.shake(0.15F);
            CameraEffects.fovPunch(0.03F);
         }

         SoundDesign.play(
            at,
            GuraSounds.WEAPON_SLASH,
            1.0F,
            1.0F,
            SoundDesign.layer(GuraSounds.LAYER_SHATTER_HIGH, 0.7F, 1.0F),
            SoundDesign.layer(DFSounds.LAYER_WIND, 0.7F, 1.2F)
         );
      }
   }

   private static void slashImpact(ClientLevel level, FxPayload p) {
      Vec3 at = p.pos();
      Vec3 dir = p.dir().normalize();
      if (!VfxSettings.legacy()) {
         QuakeStrikes.slashImpact(level, at, dir, p.power(), Double.doubleToLongBits(at.y * 17.0 + at.x));
         SoundDesign.play(at, GuraSounds.AURA_DEFLECT, 1.0F, 0.7F, SoundDesign.layer(GuraSounds.LAYER_CRACK, 1.0F, 0.8F));
      } else {
         VfxManager.add(
            new PlaneCrackEffect(
               at, dir.scale(-1.0), Math.max(0.8F, p.power() * 0.5F), Double.doubleToLongBits(at.y * 17.0 + at.x), true, 1, 4, 6, 14, false, dir.scale(-0.2)
            )
         );
         flash(at, 1.0);
         SoundDesign.play(at, GuraSounds.AURA_DEFLECT, 1.0F, 0.7F, SoundDesign.layer(GuraSounds.LAYER_CRACK, 1.0F, 0.8F));
      }
   }

   private GuraClientFx() {
   }
}
