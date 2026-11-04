package com.devilfruits.fruits.uo_uo_seiryu.client;

import com.devilfruits.client.fx.ChargeVisuals;
import com.devilfruits.client.fx.ClientFxRegistry;
import com.devilfruits.client.fx.ClientFxUtil;
import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.CameraEffects;
import com.devilfruits.client.render.vfx.ShockwaveEffect;
import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.client.render.vfx.VfxManager;
import com.devilfruits.client.sound.LoopSound;
import com.devilfruits.client.sound.SoundDesign;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.fx.BoltStrike;
import com.devilfruits.client.vfx.fx.Impact;
import com.devilfruits.core.body.SerpentBody;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.core.network.ChargePayload;
import com.devilfruits.core.network.FxPayload;
import com.devilfruits.fruits.uo_uo_seiryu.DragonShells;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuAbilities;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuFx;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuParticles;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuSounds;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import com.devilfruits.fruits.uo_uo_seiryu.client.render.DragonRenderer;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.AwakeningCinematic;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.BaguaCinematic;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.BreathBurstCinematic;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.BreathEffect;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.DashStreakEffect;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.DragonDashCinematic;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.DragonTransformCinematic;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.FireballEffect;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.FlameCloudCinematic;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.FlameTrailEffect;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.GroundCrackEffect;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.ImpactFlareEffect;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.LightningEffect;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.MeteorCinematic;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.PlasmaBeamCinematic;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.RoarCinematic;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.SeiryuDraw;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.StormCinematic;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.StormCloudEffect;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.TailCinematic;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.TailTrailEffect;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.TransformSwirlEffect;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.TwisterCinematic;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.TwisterEffect;
import com.devilfruits.fruits.uo_uo_seiryu.entity.SeiryuDragonEntity;
import com.devilfruits.registry.DFSounds;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class SeiryuClientFx {
   private static final int BOLT_FLASH = 11955455;
   private static final int FIRE_FLASH = 16756832;
   private static final float[] LEG_AT = new float[]{0.16F, 0.58F};
   private static final double[] POINT = new double[3];
   private static final Int2ObjectMap<LoopSound> WIND = new Int2ObjectOpenHashMap();
   private static long lastSpit = -4611686018427387904L;

   public static void register() {
      ClientFxRegistry.register(SeiryuFx.AWAKEN, SeiryuClientFx::awaken);
      ClientFxRegistry.register(SeiryuFx.LEVEL_UP, SeiryuClientFx::levelUp);
      ClientFxRegistry.register(SeiryuFx.REINCARNATE, SeiryuClientFx::reincarnate);
      ClientFxRegistry.register(SeiryuFx.TRANSFORM_HYBRID, HybridFx::enterHybrid);
      ClientFxRegistry.register(SeiryuFx.TRANSFORM_DRAGON, SeiryuClientFx::enterDragon);
      ClientFxRegistry.register(SeiryuFx.REVERT, SeiryuClientFx::revert);
      ClientFxRegistry.register(SeiryuFx.THUNDER_BAGUA, SeiryuClientFx::thunderBagua);
      ClientFxRegistry.register(SeiryuFx.CHAIN_ARC, SeiryuClientFx::chainArc);
      ClientFxRegistry.register(SeiryuFx.BREATH, SeiryuClientFx::breath);
      ClientFxRegistry.register(SeiryuFx.BREATH_IMPACT, SeiryuClientFx::breathImpact);
      ClientFxRegistry.register(SeiryuFx.FLAME_CLOUD, SeiryuClientFx::flameCloud);
      ClientFxRegistry.register(SeiryuFx.FLAME_TRAIL, SeiryuClientFx::flameTrail);
      ClientFxRegistry.register(SeiryuFx.DASH_STREAK, SeiryuClientFx::dashStreak);
      ClientFxRegistry.register(SeiryuFx.MELEE_SPARK, SeiryuClientFx::meleeSpark);
      ClientFxRegistry.register(SeiryuFx.FIREBALL, SeiryuClientFx::fireball);
      ClientFxRegistry.register(SeiryuFx.FIREBALL_IMPACT, SeiryuClientFx::fireballImpact);
      ClientFxRegistry.register(SeiryuFx.ROAR, SeiryuClientFx::roar);
      ClientFxRegistry.register(SeiryuFx.TWISTER, SeiryuClientFx::twister);
      ClientFxRegistry.register(SeiryuFx.STORM, SeiryuClientFx::storm);
      ClientFxRegistry.register(SeiryuFx.STRIKE, SeiryuClientFx::strike);
      ClientFxRegistry.register(SeiryuFx.TAIL, SeiryuClientFx::tail);
      ChargeVisuals.register(UoUoSeiryu.fruit().form("hybrid").id(), HybridFx.windup());
      ChargeVisuals.register(
         UoUoSeiryu.fruit().form("dragon").id(),
         new ChargeVisuals.ChargeVisual() {
            private final ChargeVisuals.ChargeVisual old = SeiryuClientFx.windup(true);

            @Override
            public void start(ClientLevel level, Entity entity, ChargePayload payload) {
               if (VfxSettings.legacy()) {
                  this.old.start(level, entity, payload);
               } else {
                  VfxDirector.spawn(
                     new DragonTransformCinematic(
                        level,
                        VfxAnchor.of(entity, entity.position()),
                        new VfxParams(entity.position(), Vec3.ZERO, 0.0F, payload.seed(), Math.max(1, (int)payload.duration()))
                     )
                  );
                  SoundDesign.play(entity.position(), SeiryuSounds.FORM_WINDUP, 1.0F, 0.8F, SoundDesign.layer(DFSounds.LAYER_THUNDER, 0.6F, 0.8F));
               }
            }

            @Override
            public void stop(ClientLevel level, Entity entity, boolean released) {
               this.old.stop(level, entity, released);
               DragonTransformCinematic c = released ? null : DragonTransformCinematic.of(entity);
               if (c != null) {
                  c.finish();
               }
            }
         }
      );
      ChargeVisuals.register(
         SeiryuAbilities.THUNDER_BAGUA.id(),
         new ChargeVisuals.ChargeVisual() {
            @Override
            public void start(ClientLevel level, Entity entity, ChargePayload payload) {
               if (!SeiryuClientFx.cinematicHuman(entity) && !SeiryuClientFx.cinematicHybrid(entity)) {
                  VfxManager.add(new SeiryuClientFx.HandCrackle(entity, (int)payload.duration()));
               } else {
                  VfxDirector.spawn(
                     new BaguaCinematic(
                        level,
                        VfxAnchor.of(entity, entity.position()),
                        new VfxParams(
                           entity.position(),
                           Vec3.ZERO,
                           SeiryuClientFx.cinematicHybrid(entity) ? 1.0F : 0.0F,
                           payload.seed(),
                           Math.max(1, (int)payload.duration())
                        )
                     )
                  );
               }

               SoundDesign.layerOnly(entity.position(), SeiryuSounds.LAYER_CRACKLE, 0.8F, 1.2F);
            }

            @Override
            public void stop(ClientLevel level, Entity entity, boolean released) {
               SeiryuClientFx.stopAll(SeiryuClientFx.HandCrackle.class, entity);
            }
         }
      );
      final ChargeVisuals.ChargeVisual mouth = new ChargeVisuals.ChargeVisual() {
         @Override
         public void start(ClientLevel level, Entity entity, ChargePayload payload) {
            VfxManager.add(new SeiryuClientFx.MouthCharge(entity, (int)payload.duration()));
            SoundDesign.play(entity.position(), SeiryuSounds.BREATH_CHARGE, 0.9F, 1.0F);
         }

         @Override
         public void stop(ClientLevel level, Entity entity, boolean released) {
            SeiryuClientFx.stopAll(SeiryuClientFx.MouthCharge.class, entity);
         }
      };
      ChargeVisuals.register(
         SeiryuAbilities.BLAST_BREATH.id(),
         new ChargeVisuals.ChargeVisual() {
            @Override
            public void start(ClientLevel level, Entity entity, ChargePayload payload) {
               if (!SeiryuClientFx.cinematicHuman(entity) && !SeiryuClientFx.cinematicHybrid(entity) && !SeiryuClientFx.cinematicDragon(entity)) {
                  mouth.start(level, entity, payload);
               } else {
                  VfxDirector.spawn(
                     new BreathBurstCinematic(
                        level,
                        VfxAnchor.of(entity, entity.position()),
                        new VfxParams(
                           entity.position(),
                           Vec3.ZERO,
                           SeiryuClientFx.cinematicDragon(entity) ? 3.5F : 0.0F,
                           payload.seed(),
                           Math.max(1, (int)payload.duration())
                        )
                     )
                  );
                  SoundDesign.play(entity.position(), SeiryuSounds.BREATH_CHARGE, 0.9F, 1.0F);
               }
            }

            @Override
            public void stop(ClientLevel level, Entity entity, boolean released) {
               mouth.stop(level, entity, released);
               BreathBurstCinematic c = released ? null : BreathBurstCinematic.charging(entity);
               if (c != null) {
                  c.finish();
               }
            }
         }
      );
      ChargeVisuals.register(
         SeiryuAbilities.BOLO_BREATH.id(),
         new ChargeVisuals.ChargeVisual() {
            @Override
            public void start(ClientLevel level, Entity entity, ChargePayload payload) {
               if (!SeiryuClientFx.cinematicDragon(entity)) {
                  mouth.start(level, entity, payload);
               } else {
                  VfxDirector.spawn(
                     new BreathBurstCinematic(
                        level,
                        VfxAnchor.of(entity, entity.position()),
                        new VfxParams(entity.position(), Vec3.ZERO, 3.5F, payload.seed(), Math.max(1, (int)payload.duration()))
                     )
                  );
                  SoundDesign.play(entity.position(), SeiryuSounds.BREATH_CHARGE, 0.9F, 0.8F);
               }
            }

            @Override
            public void stop(ClientLevel level, Entity entity, boolean released) {
               mouth.stop(level, entity, released);
               BreathBurstCinematic c = BreathBurstCinematic.charging(entity);
               if (c != null) {
                  c.handOver();
               }
            }
         }
      );
   }

   public static void tickDragons(ClientLevel level) {
      List<SeiryuDragonEntity> shells = DragonRenderer.shells();

      for (int i = 0; i < shells.size(); i++) {
         SeiryuDragonEntity shell = shells.get(i);
         Player owner = shell.owner();
         SerpentBody body = shell.body();
         if (owner != null && body != null && !shell.isRemoved() && !owner.isInvisible()) {
            double speed = owner.getDeltaMovement().length();
            boolean flying = FormFlags.has(owner, 2);
            if (flying && speed > 0.2 && body.coil(1.0F) < 0.5F) {
               for (float at : LEG_AT) {
                  if (Fx.random().nextFloat() < 0.7F) {
                     body.point((double)(body.neck + body.length * at), 1.0F, POINT);
                     double r = (double)body.radiusAt(at);
                     Fx.spawnFar(
                        (ParticleOptions)SeiryuParticles.CLOUD.get(),
                        POINT[0] + Fx.spread(r),
                        POINT[1] - r * 0.8 + Fx.spread(0.4),
                        POINT[2] + Fx.spread(r),
                        Fx.spread(0.03),
                        -0.01,
                        Fx.spread(0.03)
                     );
                  }
               }

               if (Fx.random().nextInt(3) == 0) {
                  float atx = Fx.random().nextFloat();
                  body.point((double)(body.neck + body.length * atx), 1.0F, POINT);
                  Fx.spawnFar(
                     (ParticleOptions)SeiryuParticles.CLOUD.get(),
                     POINT[0] + Fx.spread(1.0),
                     POINT[1] + Fx.spread(1.0),
                     POINT[2] + Fx.spread(1.0),
                     0.0,
                     0.0,
                     0.0
                  );
               }
            }

            if (Fx.random().nextInt(60) == 0 && body.twist(1.0F) < 0.5F) {
               float atx = 0.1F + Fx.random().nextFloat() * 0.6F;
               body.point((double)(body.neck + body.length * atx), 1.0F, POINT);
               Vec3 a = new Vec3(POINT[0], POINT[1] + (double)body.radiusAt(atx) * 0.6, POINT[2]);
               body.point((double)(body.neck + body.length * (atx + 0.04F)), 1.0F, POINT);
               Vec3 b = new Vec3(POINT[0] + Fx.spread(1.0), POINT[1] + (double)body.radiusAt(atx) * 0.9, POINT[2] + Fx.spread(1.0));
               if (VfxSettings.legacy()) {
                  VfxManager.add(new LightningEffect(a, b, 5, 0.04F, 1, Fx.random().nextLong()).quiet());
               } else {
                  BoltStrike.between(a, b, Fx.random().nextLong()).width(0.12F).life(3).forks(2).rough(0.25F).endGlow(0.6F).spawn();
               }

               SoundDesign.layerOnly(a, SeiryuSounds.LAYER_CRACKLE, 0.35F, 1.3F);
            }

            if (flying && speed > 0.35) {
               LoopSound wind = (LoopSound)WIND.get(owner.getId());
               if (wind == null || wind.isStopped()) {
                  wind = new LoopSound(
                     SeiryuSounds.FLIGHT_LOOP.get(),
                     owner,
                     owner.position(),
                     0.8F,
                     0.9F,
                     0,
                     () -> owner.isAlive() && !owner.isRemoved() && UoUoSeiryu.isIn(owner, "dragon") && owner.getDeltaMovement().length() > 0.3
                  );
                  WIND.put(owner.getId(), wind);
                  Minecraft.getInstance().getSoundManager().play(wind);
               }
            }
         }
      }

      if (level.getGameTime() % 100L == 0L) {
         WIND.int2ObjectEntrySet().removeIf(e -> ((LoopSound)e.getValue()).isStopped());
      }
   }

   private static void awaken(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      if (!VfxSettings.legacy()) {
         VfxDirector.spawn(new AwakeningCinematic(level, VfxAnchor.of(entity, p.pos()), VfxParams.of(p)));
      } else {
         Vec3 at = entity != null ? entity.position() : p.pos();
         VfxManager.add(new LightningEffect(at.add(0.0, 22.0, 0.0), at.add(0.0, 1.2, 0.0), 12, 0.12F, 3, p.seed()));
         burst((ParticleOptions)SeiryuParticles.SCALE_SHARD.get(), at.add(0.0, 1.0, 0.0), 40, 0.35);
         burst((ParticleOptions)SeiryuParticles.GOLD_SPARK.get(), at.add(0.0, 1.0, 0.0), 20, 0.25);
         SoundDesign.play(at, SeiryuSounds.AWAKEN, 1.2F, 1.0F, SoundDesign.layer(DFSounds.LAYER_THUNDER, 0.8F, 1.2F));
         if (ClientFxUtil.isLocal(entity)) {
            CameraEffects.flash(0.65F, 11955455);
            CameraEffects.shake(0.35F);
         }
      }
   }

   private static void levelUp(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      Vec3 at = entity != null ? entity.position().add(0.0, 1.0, 0.0) : p.pos();

      for (int i = 0; i < Fx.count(24); i++) {
         float a = (float)i * (float) (Math.PI * 2) / 24.0F;
         Fx.spawn(
            (ParticleOptions)SeiryuParticles.GOLD_SPARK.get(),
            at.x + (double)Mth.cos(a) * 0.8,
            at.y + Fx.spread(0.6),
            at.z + (double)Mth.sin(a) * 0.8,
            0.0,
            0.06,
            0.0
         );
      }

      SoundDesign.play(at, DFSounds.MASTERY_LEVEL_UP, 1.0F, 1.0F, SoundDesign.layer(SeiryuSounds.LAYER_GROWL, 0.4F, 1.4F));
   }

   private static void reincarnate(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      Vec3 at = p.pos();
      VfxManager.add(new LightningEffect(at.add(0.0, 18.0, 0.0), at, 10, 0.1F, 2, p.seed()));
      burst((ParticleOptions)SeiryuParticles.CLOUD.get(), at, 12, 0.1);
      SoundDesign.play(at, DFSounds.FRUIT_REINCARNATE, 1.0F, 1.0F, SoundDesign.layer(DFSounds.LAYER_THUNDER, 0.6F, 1.3F));
   }

   private static void transform(ClientLevel level, @Nullable Entity entity, FxPayload p, boolean dragon) {
      Vec3 at = entity != null ? entity.position() : p.pos();
      float scale = dragon ? 2.0F : 1.0F;
      VfxManager.add(
         new ShockwaveEffect(ShockwaveEffect.Mode.GROUND, at.add(0.0, 0.1, 0.0), Vec3.ZERO, 4.5F * scale, 0.0F, 16, 2, 1.0F)
            .tint(122, 214, 255)
            .shell(242, 210, 90)
      );
      int bolts = dragon ? 7 : 4;

      for (int i = 0; i < bolts; i++) {
         float a = (float)i * (float) (Math.PI * 2) / (float)bolts;
         Vec3 foot = at.add((double)Mth.cos(a) * 1.4 * (double)scale, 0.0, (double)Mth.sin(a) * 1.4 * (double)scale);
         Vec3 top = at.add((double)Mth.cos(a + 1.2F) * 0.6, dragon ? 6.0 : 3.0, (double)Mth.sin(a + 1.2F) * 0.6);
         VfxManager.add(new LightningEffect(foot, top, 10, 0.07F, 1, p.seed() + (long)i).quiet());
      }

      if (dragon) {
         VfxManager.add(new LightningEffect(at.add(0.0, 2.0, 0.0), at.add(Fx.spread(3.0), 26.0, Fx.spread(3.0)), 14, 0.16F, 3, p.seed() ^ 77L));
         burst((ParticleOptions)SeiryuParticles.CLOUD.get(), at.add(0.0, 1.0, 0.0), 30, 0.25);
      }

      burst((ParticleOptions)SeiryuParticles.SCALE_SHARD.get(), at.add(0.0, 1.2, 0.0), dragon ? 80 : 40, 0.4);
      burst((ParticleOptions)SeiryuParticles.GOLD_SPARK.get(), at.add(0.0, 1.2, 0.0), dragon ? 40 : 20, 0.3);
      SoundDesign.play(
         at,
         dragon ? SeiryuSounds.FORM_DRAGON : SeiryuSounds.FORM_HYBRID,
         1.3F,
         1.0F,
         SoundDesign.layer(DFSounds.LAYER_THUNDER, dragon ? 1.0F : 0.6F, 1.0F),
         SoundDesign.layer(SeiryuSounds.LAYER_GROWL, 0.8F, dragon ? 0.8F : 1.1F)
      );
      if (ClientFxUtil.isLocal(entity)) {
         CameraEffects.flash(dragon ? 0.6F : 0.4F, 11955455);
         CameraEffects.shake(dragon ? 0.7F : 0.45F);
         CameraEffects.fovPunch(0.12F);
      } else {
         CameraEffects.shakeAt(at, dragon ? 0.5F : 0.3F, dragon ? 48.0 : 24.0);
      }
   }

   private static void revert(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      Vec3 at = entity != null ? entity.position().add(0.0, 1.0, 0.0) : p.pos();
      burst((ParticleOptions)SeiryuParticles.CLOUD.get(), at, 18, 0.12);
      burst((ParticleOptions)SeiryuParticles.SCALE_SHARD.get(), at, 24, 0.2);
      SoundDesign.play(at, SeiryuSounds.FORM_REVERT, 1.0F, 1.0F);
   }

   private static boolean cinematicHuman(@Nullable Entity entity) {
      if (entity instanceof Player p && !VfxSettings.legacy() && !UoUoSeiryu.isIn(p, "hybrid") && !UoUoSeiryu.isIn(p, "dragon")) {
         return true;
      }

      return false;
   }

   private static boolean cinematicDragon(@Nullable Entity entity) {
      if (entity instanceof Player p && !VfxSettings.legacy() && UoUoSeiryu.isIn(p, "dragon")) {
         return true;
      }

      return false;
   }

   private static void enterDragon(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      if (entity != null && !VfxSettings.legacy()) {
         DragonTransformCinematic c = DragonTransformCinematic.of(entity);
         if (c == null) {
            c = VfxDirector.spawn(
               new DragonTransformCinematic(level, VfxAnchor.of(entity, entity.position()), new VfxParams(entity.position(), Vec3.ZERO, 0.0F, p.seed(), 0))
            );
         }

         c.change(level);
         SoundDesign.play(
            entity.position(),
            SeiryuSounds.FORM_DRAGON,
            1.5F,
            1.0F,
            SoundDesign.layer(DFSounds.LAYER_THUNDER, 1.2F, 0.9F),
            SoundDesign.layer(DFSounds.LAYER_BOOM, 1.0F, 0.7F),
            SoundDesign.layer(SeiryuSounds.LAYER_GROWL, 0.9F, 0.8F)
         );
      } else {
         transform(level, entity, p, true);
      }
   }

   private static boolean cinematicHybrid(@Nullable Entity entity) {
      if (entity instanceof Player p && !VfxSettings.legacy() && UoUoSeiryu.isIn(p, "hybrid")) {
         return true;
      }

      return false;
   }

   private static void thunderBagua(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      boolean hybrid = p.extra() == 1;
      Vec3 at = p.pos();
      Vec3 dir = p.dir().lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : p.dir().normalize();
      float radius = p.power();
      if (entity != null && !VfxSettings.legacy()) {
         BaguaCinematic c = BaguaCinematic.of(entity);
         if (c == null) {
            c = VfxDirector.spawn(
               new BaguaCinematic(level, VfxAnchor.of(entity, entity.position()), new VfxParams(entity.position(), dir, hybrid ? 1.0F : 0.0F, p.seed(), 1))
            );
         }

         c.strike(level, at, dir, radius, hybrid);
         SoundDesign.play(
            at,
            SeiryuSounds.THUNDER_BAGUA,
            hybrid ? 1.8F : 1.3F,
            hybrid ? 0.8F : 1.0F,
            SoundDesign.layer(DFSounds.LAYER_THUNDER, hybrid ? 1.0F : 0.6F, 1.2F),
            SoundDesign.layer(DFSounds.LAYER_BOOM, hybrid ? 0.9F : 0.5F, 0.9F),
            SoundDesign.layer(SeiryuSounds.THUNDER_BAGUA_SWING, 0.8F, 1.0F)
         );
      } else {
         VfxManager.add(
            new ShockwaveEffect(
                  ShockwaveEffect.Mode.GROUND, at.add(0.0, 0.15, 0.0), Vec3.ZERO, radius * (hybrid ? 1.5F : 1.0F), 0.0F, hybrid ? 16 : 12, hybrid ? 3 : 2, 1.0F
               )
               .tint(182, 108, 255)
               .shell(230, 210, 255)
         );
         if (!ClientFxUtil.firstPersonCaster(entity)) {
            VfxManager.add(
               new ShockwaveEffect(ShockwaveEffect.Mode.CONE, at.add(0.0, 0.8, 0.0), dir, radius * 1.2F, 0.6F, 10, 2, 0.8F)
                  .tint(182, 108, 255)
                  .shell(243, 232, 255)
            );
         }

         int bolts = hybrid ? 10 : 6;

         for (int i = 0; i < bolts; i++) {
            float a = (float)Math.atan2(dir.z, dir.x) + ((float)i - (float)bolts * 0.5F) * (hybrid ? 0.62F : 0.45F);
            double reach = (double)radius * (0.7 + 0.3 * (double)(p.seed() >> i & 1L));
            Vec3 end = at.add((double)Mth.cos(a) * reach, 0.2 + ((i & 1) == 0 ? 0.0 : 0.8), (double)Mth.sin(a) * reach);
            VfxManager.add(new LightningEffect(at.add(0.0, 0.3, 0.0), end, hybrid ? 10 : 8, hybrid ? 0.09F : 0.07F, 1, p.seed() + (long)i * 13L));
         }

         VfxManager.add(new LightningEffect(at, at.add(0.0, hybrid ? 7.0 : 4.0, 0.0), 8, 0.1F, 2, p.seed() ^ 40503L));
         if (hybrid) {
            VfxManager.add(new GroundCrackEffect(at, radius * 1.4F, 9, 40, p.seed(), 15984895, 11955455));
         }

         burst((ParticleOptions)SeiryuParticles.BOLT_SPARK.get(), at.add(0.0, 0.5, 0.0), hybrid ? 30 : 16, 0.4);
         SoundDesign.play(
            at,
            SeiryuSounds.THUNDER_BAGUA,
            hybrid ? 1.8F : 1.3F,
            hybrid ? 0.8F : 1.0F,
            SoundDesign.layer(DFSounds.LAYER_THUNDER, hybrid ? 0.9F : 0.5F, 1.2F),
            SoundDesign.layer(DFSounds.LAYER_BOOM, hybrid ? 0.8F : 0.4F, 0.9F),
            SoundDesign.layer(SeiryuSounds.THUNDER_BAGUA_SWING, 0.8F, 1.0F)
         );
         if (ClientFxUtil.isLocal(entity)) {
            CameraEffects.shake(hybrid ? 0.6F : 0.35F);
            CameraEffects.fovPunch(hybrid ? 0.12F : 0.08F);
            CameraEffects.flash(hybrid ? 0.3F : 0.15F, 11955455);
         } else {
            CameraEffects.shakeAt(at, hybrid ? 0.45F : 0.25F, hybrid ? 32.0 : 20.0);
         }
      }
   }

   private static void chainArc(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      if (!VfxSettings.legacy()) {
         BoltStrike.between(p.pos(), p.dir(), p.seed()).width(0.16F).life(7).forks(3).rough(0.15F).heat(1.2F).endGlow(1.2F).spawn();
         Impact.at(level, p.dir(), p.seed() ^ 202L).thunder().flash(0.6F).glow(11955455).sparks(14, 0.45F).embers(8).play();
         SoundDesign.play(p.dir(), SeiryuSounds.CHAIN_ARC, 0.8F, 1.0F);
      } else {
         VfxManager.add(new LightningEffect(p.pos(), p.dir(), 7, 0.06F, 1, p.seed()));
         SoundDesign.play(p.dir(), SeiryuSounds.CHAIN_ARC, 0.8F, 1.0F);
      }
   }

   private static void breath(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      int duration = p.extra();
      boolean beam = duration > 0;
      if (!beam || !cinematicHybrid(entity) && !cinematicDragon(entity)) {
         if (!beam && cinematicHuman(entity)) {
            BreathBurstCinematic c = BreathBurstCinematic.charging(entity);
            if (c == null) {
               c = VfxDirector.spawn(
                  new BreathBurstCinematic(level, VfxAnchor.of(entity, entity.position()), new VfxParams(entity.position(), Vec3.ZERO, 0.0F, p.seed(), 1))
               );
            }

            c.release(level, p.pos(), p.dir(), p.power());
            SoundDesign.play(p.pos(), SeiryuSounds.BREATH_RELEASE, 1.1F, 1.15F);
         } else {
            boolean var10000;
            label101: {
               VfxManager.add(new BreathEffect(entity, p.pos(), p.dir(), p.power(), duration, p.seed()));
               if (entity instanceof Player pl && UoUoSeiryu.isIn(pl, "dragon")) {
                  var10000 = true;
                  break label101;
               }

               var10000 = false;
            }

            boolean dragonForm = var10000;
            SoundDesign.play(p.pos(), SeiryuSounds.BREATH_RELEASE, beam ? (dragonForm ? 2.0F : 1.5F) : 1.1F, beam ? (dragonForm ? 0.85F : 1.0F) : 1.15F);
            if (beam && entity != null) {
               Minecraft.getInstance()
                  .getSoundManager()
                  .play(
                     new LoopSound(
                        SeiryuSounds.BREATH_LOOP.get(),
                        entity,
                        entity.position(),
                        dragonForm ? 1.6F : 1.2F,
                        dragonForm ? 0.85F : 1.0F,
                        duration,
                        () -> entity.isAlive()
                     )
                  );
            }

            if (ClientFxUtil.isLocal(entity)) {
               CameraEffects.shake(beam ? 0.3F : 0.15F);
               CameraEffects.fovPunch(0.06F);
               if (beam) {
                  CameraEffects.sustain(0.18F, duration);
               }
            }
         }
      } else {
         BreathBurstCinematic charge = BreathBurstCinematic.charging(entity);
         if (charge != null) {
            charge.handOver();
         }

         boolean dragon = cinematicDragon(entity);
         VfxDirector.spawn(new PlasmaBeamCinematic(level, VfxAnchor.of(entity, p.pos()), VfxParams.of(p), dragon ? 5.0F : 1.7F));
         SoundDesign.play(p.pos(), SeiryuSounds.BREATH_RELEASE, dragon ? 2.0F : 1.5F, dragon ? 0.85F : 1.0F);
         Minecraft.getInstance()
            .getSoundManager()
            .play(
               new LoopSound(
                  SeiryuSounds.BREATH_LOOP.get(), entity, entity.position(), dragon ? 1.6F : 1.2F, dragon ? 0.85F : 1.0F, duration, () -> entity.isAlive()
               )
            );
      }
   }

   private static void breathImpact(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      Vec3 at = p.pos();
      boolean fromBeam = p.extra() == 1;
      PlasmaBeamCinematic plasma = !fromBeam || !cinematicHybrid(entity) && !cinematicDragon(entity) ? null : PlasmaBeamCinematic.of(entity);
      if (plasma != null) {
         plasma.explode(level, at, p.power());
         boolean big = cinematicDragon(entity);
         SoundDesign.play(at, SeiryuSounds.BREATH_BURST, big ? 1.6F : 1.0F, (big ? 0.85F : 1.0F) + Fx.random().nextFloat() * 0.15F);
      } else {
         BreathBurstCinematic burst = !fromBeam && cinematicHuman(entity) ? BreathBurstCinematic.fired(entity) : null;
         if (burst != null) {
            burst.impact(at, p.power());
            SoundDesign.play(at, SeiryuSounds.BREATH_BURST, 1.2F, 1.0F + Fx.random().nextFloat() * 0.1F);
         } else if (!level.getFluidState(BlockPos.containing(at.x, at.y - 0.05, at.z)).isEmpty()) {
            burst((ParticleOptions)SeiryuParticles.CLOUD.get(), at.add(0.0, 0.3, 0.0), fromBeam ? 6 : 14, 0.12);
            SoundDesign.layerOnly(at, SeiryuSounds.LAYER_FLAME, 0.5F, 1.6F);
         } else {
            int n = Fx.count(fromBeam ? 6 : 18);

            for (int i = 0; i < n; i++) {
               Fx.spawn(
                  (ParticleOptions)SeiryuParticles.FLAME.get(),
                  at.x + Fx.spread(0.6 * (double)p.power()),
                  at.y + Fx.spread(0.4),
                  at.z + Fx.spread(0.6 * (double)p.power()),
                  Fx.spread(0.15),
                  0.06 + (double)Fx.random().nextFloat() * 0.08,
                  Fx.spread(0.15)
               );
            }

            for (int i = 0; i < Fx.count(fromBeam ? 2 : 6); i++) {
               Fx.spawn(ParticleTypes.LARGE_SMOKE, at.x + Fx.spread(0.6), at.y + 0.2, at.z + Fx.spread(0.6), 0.0, 0.04, 0.0);
               Fx.spawn(
                  (ParticleOptions)SeiryuParticles.EMBER.get(), at.x + Fx.spread(0.6), at.y + 0.2, at.z + Fx.spread(0.6), Fx.spread(0.1), 0.1, Fx.spread(0.1)
               );
            }

            SoundDesign.play(at, SeiryuSounds.BREATH_BURST, fromBeam ? 1.0F : 1.2F, 0.95F + Fx.random().nextFloat() * 0.15F);
         }
      }
   }

   private static void flameCloud(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      if (entity != null && !VfxSettings.legacy()) {
         VfxDirector.spawn(new FlameCloudCinematic(level, VfxAnchor.of(entity, p.pos()), VfxParams.of(p)));
         SoundDesign.play(
            p.pos(),
            SeiryuSounds.FLAME_CLOUD,
            1.0F,
            1.0F,
            SoundDesign.layer(SeiryuSounds.LAYER_FLAME, 0.7F, 0.8F),
            SoundDesign.layer(DFSounds.LAYER_WIND, 0.6F, 1.2F)
         );
      }
   }

   private static void flameTrail(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      FlameCloudCinematic cloud = entity != null && !VfxSettings.legacy() ? FlameCloudCinematic.riding(entity) : null;
      if (cloud != null) {
         cloud.trail(level, p.pos(), p.dir(), p.power(), p.extra());
      } else {
         VfxManager.add(new FlameTrailEffect(p.pos(), p.dir(), p.power(), p.extra()));
         SoundDesign.play(p.pos(), SeiryuSounds.FLAME_CLOUD, 1.0F, 1.0F, SoundDesign.layer(SeiryuSounds.LAYER_FLAME, 0.6F, 0.8F));
         if (ClientFxUtil.isLocal(entity)) {
            CameraEffects.fovPunch(0.1F);
         }
      }
   }

   private static void dashStreak(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      if (entity != null && !VfxSettings.legacy()) {
         VfxDirector.spawn(new DragonDashCinematic(level, VfxAnchor.of(entity, p.pos()), VfxParams.of(p)));
         SoundDesign.play(
            p.pos(),
            SeiryuSounds.DRAGON_DASH,
            1.0F,
            1.0F,
            SoundDesign.layer(DFSounds.LAYER_WIND, 0.7F, 1.4F),
            SoundDesign.layer(DFSounds.LAYER_THUNDER, 0.45F, 1.5F),
            SoundDesign.layer(SeiryuSounds.LAYER_CRACKLE, 0.7F, 1.1F)
         );
      } else {
         if (entity != null) {
            VfxManager.add(new DashStreakEffect(entity, p.extra(), p.seed()));
         }

         SoundDesign.play(p.pos(), SeiryuSounds.DRAGON_DASH, 1.0F, 1.0F, SoundDesign.layer(DFSounds.LAYER_WIND, 0.7F, 1.4F));
         if (ClientFxUtil.isLocal(entity)) {
            CameraEffects.fovPunch(0.15F);
         }
      }
   }

   private static void meleeSpark(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      Vec3 at = p.pos();
      if (p.extra() == 1) {
         burst((ParticleOptions)SeiryuParticles.FLAME.get(), at, 10, 0.12);
         burst((ParticleOptions)SeiryuParticles.EMBER.get(), at, 6, 0.15);
         SoundDesign.layerOnly(at, SeiryuSounds.LAYER_FLAME, 0.5F, 1.3F);
      } else {
         for (int i = 0; i < 3; i++) {
            Vec3 end = at.add(Fx.spread(1.0), Fx.spread(1.0), Fx.spread(1.0));
            VfxManager.add(new LightningEffect(at, end, 4, 0.035F, 0, p.seed() + (long)i).quiet());
         }

         burst((ParticleOptions)SeiryuParticles.BOLT_SPARK.get(), at, 6, 0.2);
         SoundDesign.layerOnly(at, SeiryuSounds.LAYER_CRACKLE, 0.5F, 1.4F);
      }
   }

   private static void fireball(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      if (VfxSettings.legacy()) {
         VfxManager.add(new FireballEffect(p.pos(), p.dir(), p.power(), p.extra(), p.seed()));
      } else {
         VfxDirector.spawn(new MeteorCinematic(level, VfxAnchor.of(entity, p.pos()), VfxParams.of(p)));
      }

      boolean spat = entity == null || p.pos().distanceToSqr(entity.position()) < 144.0;
      long now = level.getGameTime();
      if (spat && now - lastSpit > 2L) {
         lastSpit = now;
         SoundDesign.play(p.pos(), SeiryuSounds.BOLO_BREATH, 1.3F, 0.9F + Fx.random().nextFloat() * 0.2F);
      }
   }

   private static void fireballImpact(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      if (!VfxSettings.legacy()) {
         MeteorCinematic m = MeteorCinematic.of(p.seed());
         if (m == null) {
            m = VfxDirector.spawn(new MeteorCinematic(level, VfxAnchor.of(entity, p.pos()), new VfxParams(p.pos(), p.dir(), p.power(), p.seed(), 2)));
         }

         m.impact(level, p.pos(), p.power());
         impactSound(p.pos(), p.power());
      } else {
         FireballEffect ball = VfxManager.find(FireballEffect.class, e -> e.seed == p.seed());
         if (ball != null) {
            ball.finish();
         }

         Vec3 at = p.pos();
         float size = p.power();
         VfxManager.add(new ImpactFlareEffect(at, 2.2F * size, 7, 16776160, 16758574));
         VfxManager.add(
            new ShockwaveEffect(ShockwaveEffect.Mode.GROUND, at.add(0.0, 0.1, 0.0), Vec3.ZERO, 2.6F * size, 0.0F, 8, 1, 0.7F)
               .tint(255, 154, 58)
               .shell(255, 228, 122)
         );
         burst((ParticleOptions)SeiryuParticles.FLAME.get(), at, 14, 0.2 * (double)size);
         burst((ParticleOptions)SeiryuParticles.EMBER.get(), at, 8, 0.25 * (double)size);

         for (int i = 0; i < Fx.count(3); i++) {
            Fx.spawn(ParticleTypes.LARGE_SMOKE, at.x + Fx.spread(0.5), at.y + 0.3, at.z + Fx.spread(0.5), 0.0, 0.05, 0.0);
         }

         if (size >= 1.5F) {
            VfxManager.add(new GroundCrackEffect(at, 2.2F * size, 8, 60, p.seed(), 16771496, 16758574));

            for (int i = 0; i < Fx.count(8); i++) {
               Fx.spawnFar(ParticleTypes.LARGE_SMOKE, at.x + Fx.spread(1.5), at.y + 0.5 + (double)i * 0.4, at.z + Fx.spread(1.5), 0.0, 0.08, 0.0);
            }

            burst((ParticleOptions)SeiryuParticles.FLAME.get(), at, 20, 0.35);
         }

         impactSound(at, size);
         CameraEffects.shakeAt(at, 0.12F * size, (double)(16.0F * size));
      }
   }

   private static void impactSound(Vec3 at, float size) {
      boolean meteor = size >= 1.5F;
      SoundDesign.play(at, SeiryuSounds.FIREBALL_IMPACT, meteor ? 1.5F : 0.9F, (meteor ? 0.9F : 1.1F) + (Fx.random().nextFloat() - 0.5F) * 0.1F);
   }

   private static void roar(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      Vec3 at = entity != null ? BreathEffect.mouth(entity, 1.0F) : p.pos();
      Vec3 dir = p.dir().lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : p.dir().normalize();
      float range = p.power();
      float spread = (float)Math.tan(Math.toRadians((double)Math.min(160, p.extra()) * 0.5)) * 0.5F;
      if (!VfxSettings.legacy()) {
         VfxDirector.spawn(new RoarCinematic(level, VfxAnchor.of(entity, at), new VfxParams(at, dir, range, p.seed(), p.extra())));
         SoundDesign.play(
            at,
            SeiryuSounds.ROAR,
            2.4F,
            0.8F,
            SoundDesign.layer(SeiryuSounds.LAYER_GROWL, 1.3F, 0.6F),
            SoundDesign.layer(DFSounds.LAYER_BOOM, 0.8F, 0.6F),
            SoundDesign.layer(DFSounds.LAYER_THUNDER, 0.6F, 0.7F)
         );
      } else {
         VfxManager.add(new ShockwaveEffect(ShockwaveEffect.Mode.CONE, at, dir, range, spread, 16, 4, 1.1F).tint(200, 228, 255).shell(255, 255, 255));
         VfxManager.add(
            new ShockwaveEffect(ShockwaveEffect.Mode.CONE, at, dir, range * 0.6F, spread * 0.7F, 10, 2, 0.9F).tint(122, 196, 255).shell(232, 248, 255)
         );

         for (int i = 0; i < Fx.count(30); i++) {
            Vec3 d = dir.add(Fx.spread((double)(spread * 2.0F)), Fx.spread((double)spread), Fx.spread((double)(spread * 2.0F))).normalize();
            double t = (double)(Fx.random().nextFloat() * 3.0F);
            Fx.spawnFar((ParticleOptions)SeiryuParticles.CLOUD.get(), at.x + d.x * t, at.y + d.y * t, at.z + d.z * t, d.x * 0.9, d.y * 0.9, d.z * 0.9);
         }

         SoundDesign.play(
            at, SeiryuSounds.ROAR, 2.4F, 0.8F, SoundDesign.layer(SeiryuSounds.LAYER_GROWL, 1.3F, 0.6F), SoundDesign.layer(DFSounds.LAYER_BOOM, 0.8F, 0.6F)
         );
         if (ClientFxUtil.isLocal(entity)) {
            CameraEffects.shake(0.6F);
            CameraEffects.fovPunch(0.12F);
         } else {
            CameraEffects.shakeAt(at, 0.7F, (double)range * 2.5);
         }
      }
   }

   private static void twister(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      if (entity != null) {
         if (!VfxSettings.legacy()) {
            TwisterCinematic spinning = TwisterCinematic.of(entity);
            if (spinning != null) {
               spinning.release(level);
            }

            if (p.extra() <= 0) {
               SoundDesign.play(
                  entity.position(),
                  SeiryuSounds.TAIL_WHIP,
                  1.4F,
                  0.7F,
                  SoundDesign.layer(DFSounds.LAYER_WIND, 1.0F, 0.7F),
                  SoundDesign.layer(DFSounds.LAYER_BOOM, 0.6F, 0.8F)
               );
            } else {
               VfxDirector.spawn(new TwisterCinematic(level, VfxAnchor.of(entity, p.pos()), VfxParams.of(p)));
               Minecraft.getInstance()
                  .getSoundManager()
                  .play(
                     new LoopSound(
                        SeiryuSounds.TWISTER_LOOP.get(),
                        entity,
                        entity.position(),
                        1.6F,
                        0.9F,
                        p.extra() + 10,
                        () -> FormFlags.has((Player)entity, DragonShells.TWISTING)
                     )
                  );
               SoundDesign.play(
                  entity.position(),
                  SeiryuSounds.TAIL_WHIP,
                  1.2F,
                  0.8F,
                  SoundDesign.layer(DFSounds.LAYER_WIND, 1.2F, 0.8F),
                  SoundDesign.layer(SeiryuSounds.LAYER_GROWL, 0.7F, 0.9F),
                  SoundDesign.layer(SeiryuSounds.LAYER_CRACKLE, 0.6F, 0.9F)
               );
            }
         } else {
            TwisterEffect old = VfxManager.find(TwisterEffect.class, e -> e.casterId() == entity.getId());
            if (old != null) {
               old.stop();
            }

            if (p.extra() <= 0) {
               burst((ParticleOptions)SeiryuParticles.CLOUD.get(), entity.position(), 30, 0.6);
               SoundDesign.play(entity.position(), SeiryuSounds.TAIL_WHIP, 1.4F, 0.7F, SoundDesign.layer(DFSounds.LAYER_WIND, 1.0F, 0.7F));
            } else {
               VfxManager.add(new TwisterEffect(entity, p.power(), p.extra(), p.seed()));
               Minecraft.getInstance()
                  .getSoundManager()
                  .play(
                     new LoopSound(
                        SeiryuSounds.TWISTER_LOOP.get(),
                        entity,
                        entity.position(),
                        1.6F,
                        0.9F,
                        p.extra() + 10,
                        () -> FormFlags.has((Player)entity, DragonShells.TWISTING)
                     )
                  );
               SoundDesign.play(
                  entity.position(),
                  SeiryuSounds.TAIL_WHIP,
                  1.2F,
                  0.8F,
                  SoundDesign.layer(DFSounds.LAYER_WIND, 1.2F, 0.8F),
                  SoundDesign.layer(SeiryuSounds.LAYER_GROWL, 0.7F, 0.9F)
               );
               if (ClientFxUtil.isLocal(entity)) {
                  CameraEffects.sustain(0.12F, p.extra());
               }
            }
         }
      }
   }

   private static void storm(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      StormCloudEffect old = entity == null ? null : VfxManager.find(StormCloudEffect.class, e -> e.casterId() == entity.getId());
      if (p.extra() <= 0) {
         if (old != null) {
            old.stop();
         }

         StormCinematic sc = entity == null ? null : StormCinematic.of(entity);
         if (sc != null) {
            sc.stop();
         }
      } else if (entity != null) {
         VfxManager.add(new StormCloudEffect(entity, p.pos(), p.power(), p.extra()));
         if (!VfxSettings.legacy()) {
            BoltStrike.between(entity.position().add(0.0, 2.0, 0.0), p.pos(), p.seed())
               .width(0.7F)
               .life(10)
               .forks(4)
               .rough(0.08F)
               .heat(1.3F)
               .endGlow(3.0F)
               .spawn();
            Impact.at(level, entity.position().add(0.0, 2.0, 0.0), p.seed() ^ 91L)
               .caster(entity)
               .thunder()
               .flash(2.0F)
               .glow(11955455)
               .sparks(40, 0.7F)
               .embers(24)
               .trauma(0.4F, 0.08F, 2.6F)
               .lens(0.3F)
               .screenFlash(0.15F)
               .play();
            StormCinematic sc = StormCinematic.of(entity);
            if (sc != null) {
               sc.moveTo(p.pos());
            } else {
               VfxDirector.spawn(new StormCinematic(level, VfxAnchor.of(entity, p.pos()), VfxParams.of(p)));
            }

            SoundDesign.play(
               p.pos(),
               SeiryuSounds.STORM,
               2.2F,
               0.8F,
               SoundDesign.layer(DFSounds.LAYER_THUNDER, 1.4F, 0.7F),
               SoundDesign.layer(SeiryuSounds.LAYER_GROWL, 1.0F, 0.7F)
            );
         } else {
            VfxManager.add(new LightningEffect(entity.position().add(0.0, 2.0, 0.0), p.pos(), 10, 0.16F, 3, p.seed()));
            SoundDesign.play(
               p.pos(),
               SeiryuSounds.STORM,
               2.2F,
               0.8F,
               SoundDesign.layer(DFSounds.LAYER_THUNDER, 1.4F, 0.7F),
               SoundDesign.layer(SeiryuSounds.LAYER_GROWL, 1.0F, 0.7F)
            );
            if (ClientFxUtil.isLocal(entity)) {
               CameraEffects.flash(0.35F, 11955455);
               CameraEffects.shake(0.5F);
            }
         }
      }
   }

   private static void strike(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      Vec3 ground = p.pos();
      Vec3 top = p.dir();
      if (!VfxSettings.legacy()) {
         StormCinematic.strike(level, entity, ground, top, p.seed());
         StormCloudEffect lit = entity == null ? null : VfxManager.find(StormCloudEffect.class, e -> e.casterId() == entity.getId());
         if (lit != null) {
            lit.flash(ground);
         }

         SoundDesign.play(
            ground,
            SeiryuSounds.STORM_STRIKE,
            2.4F,
            0.9F + Fx.random().nextFloat() * 0.2F,
            true,
            SoundDesign.layer(DFSounds.LAYER_THUNDER, 1.4F, 1.0F),
            SoundDesign.layer(DFSounds.LAYER_BOOM, 0.6F, 1.1F)
         );
      } else {
         VfxManager.add(new LightningEffect(top, ground, 9, 0.32F, 4, p.seed()));
         VfxManager.add(
            new LightningEffect(top.add(Fx.spread(3.0), 0.0, Fx.spread(3.0)), ground.add(Fx.spread(1.5), 0.0, Fx.spread(1.5)), 6, 0.12F, 2, p.seed() ^ 119L)
               .quiet()
         );
         VfxManager.add(new ImpactFlareEffect(ground.add(0.0, 0.6, 0.0), 3.2F, 8, 16777215, 11955455));
         VfxManager.add(
            new ShockwaveEffect(ShockwaveEffect.Mode.GROUND, ground.add(0.0, 0.12, 0.0), Vec3.ZERO, 4.5F, 0.0F, 10, 2, 0.9F)
               .tint(182, 108, 255)
               .shell(243, 232, 255)
         );
         VfxManager.add(new GroundCrackEffect(ground, 3.2F, 6, 50, p.seed(), 15984895, 11955455));
         burst((ParticleOptions)SeiryuParticles.BOLT_SPARK.get(), ground.add(0.0, 0.4, 0.0), 24, 0.45);

         for (int i = 0; i < Fx.count(4); i++) {
            Fx.spawn(ParticleTypes.LARGE_SMOKE, ground.x + Fx.spread(0.8), ground.y + 0.3, ground.z + Fx.spread(0.8), 0.0, 0.06, 0.0);
         }

         StormCloudEffect cloud = entity == null ? null : VfxManager.find(StormCloudEffect.class, e -> e.casterId() == entity.getId());
         if (cloud != null) {
            cloud.flash(ground);
         }

         SoundDesign.play(
            ground,
            SeiryuSounds.STORM_STRIKE,
            2.4F,
            0.9F + Fx.random().nextFloat() * 0.2F,
            true,
            SoundDesign.layer(DFSounds.LAYER_THUNDER, 1.4F, 1.0F),
            SoundDesign.layer(DFSounds.LAYER_BOOM, 0.6F, 1.1F)
         );
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null) {
            double d = mc.player.position().distanceTo(ground);
            if (d < 64.0) {
               CameraEffects.flash(0.22F * (float)(1.0 - d / 64.0), 11955455);
            }
         }

         CameraEffects.shakeAt(ground, 0.35F, 28.0);
      }
   }

   private static void tail(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      SeiryuDragonEntity shell = entity == null ? null : DragonRenderer.shellOf(entity);
      SerpentBody body = shell == null ? null : shell.body();
      int action = (int)Math.round(p.dir().x);
      if (!VfxSettings.legacy() && entity != null) {
         VfxDirector.spawn(new TailCinematic(level, VfxAnchor.of(entity, p.pos()), VfxParams.of(p)));
      }

      if (action == 1) {
         if (body != null) {
            body.sweep(p.extra(), (float)p.dir().z);
         }

         if (shell != null && VfxSettings.legacy()) {
            VfxManager.add(new TailTrailEffect(shell, p.extra()));
         }

         Vec3 at = entity != null ? entity.position() : p.pos();
         SoundDesign.play(
            at, SeiryuSounds.TAIL_WHIP, 2.0F, 0.8F, SoundDesign.layer(DFSounds.LAYER_WIND, 1.3F, 0.6F), SoundDesign.layer(SeiryuSounds.LAYER_GROWL, 0.6F, 1.0F)
         );
         if (ClientFxUtil.isLocal(entity)) {
            CameraEffects.roll(12, 0.6F * (float)p.dir().z);
         }
      } else if (action == 2) {
         Vec3 c = p.pos();
         if (body != null) {
            body.crush(p.extra(), c.x, c.y, c.z, (float)p.dir().y);
         }

         if (VfxSettings.legacy()) {
            burst((ParticleOptions)SeiryuParticles.CLOUD.get(), c.add(0.0, 0.5, 0.0), 24, 0.3);
            burst((ParticleOptions)SeiryuParticles.SCALE_SHARD.get(), c.add(0.0, 1.5, 0.0), 16, 0.3);
         }

         SoundDesign.play(
            c, SeiryuSounds.COIL_CRUSH, 1.8F, 0.9F, SoundDesign.layer(SeiryuSounds.LAYER_GROWL, 0.9F, 0.8F), SoundDesign.layer(DFSounds.LAYER_BOOM, 0.5F, 0.6F)
         );
         CameraEffects.shakeAt(c, 0.4F, 32.0);
      }
   }

   private static ChargeVisuals.ChargeVisual windup(final boolean dragon) {
      return new ChargeVisuals.ChargeVisual() {
         @Override
         public void start(ClientLevel level, Entity entity, ChargePayload payload) {
            VfxManager.add(new TransformSwirlEffect(entity, (int)payload.duration(), dragon));
            SoundDesign.play(entity.position(), SeiryuSounds.FORM_WINDUP, 1.0F, dragon ? 0.8F : 1.0F);
         }

         @Override
         public void stop(ClientLevel level, Entity entity, boolean released) {
            TransformSwirlEffect swirl = VfxManager.find(TransformSwirlEffect.class, e -> e.entityId() == entity.getId());
            if (swirl != null) {
               swirl.stop();
            }
         }
      };
   }

   private static <T extends SeiryuClientFx.FollowEffect> void stopAll(Class<T> type, Entity entity) {
      for (T effect = (T)VfxManager.find(type, e -> e.entityId == entity.getId());
         effect != null;
         effect = (T)VfxManager.find(type, e -> e.entityId == entity.getId())
      ) {
         effect.finish();
      }
   }

   private static void burst(ParticleOptions type, Vec3 at, int count, double speed) {
      int n = Fx.count(count);

      for (int i = 0; i < n; i++) {
         Fx.spawn(type, at.x, at.y, at.z, Fx.spread(speed), Fx.spread(speed) + speed * 0.3, Fx.spread(speed));
      }
   }

   private SeiryuClientFx() {
   }

   private abstract static class FollowEffect extends VfxEffect {
      final int entityId;
      final int duration;
      double x;
      double y;
      double z;

      FollowEffect(Entity entity, int duration) {
         this.entityId = entity.getId();
         this.duration = Math.max(1, duration);
         this.x = entity.getX();
         this.y = entity.getY();
         this.z = entity.getZ();
      }

      @Nullable
      Entity follow(ClientLevel level) {
         Entity e = level.getEntity(this.entityId);
         if (e != null && this.age <= this.duration + 60) {
            this.x = e.getX();
            this.y = e.getY();
            this.z = e.getZ();
            return e;
         } else {
            this.finish();
            return null;
         }
      }

      @Override
      public double x() {
         return this.x;
      }

      @Override
      public double y() {
         return this.y;
      }

      @Override
      public double z() {
         return this.z;
      }
   }

   private static final class HandCrackle extends SeiryuClientFx.FollowEffect {
      HandCrackle(Entity entity, int duration) {
         super(entity, duration);
      }

      @Override
      public void tick(ClientLevel level) {
         super.tick(level);
         Entity e = this.follow(level);
         if (e != null) {
            float yaw = e.getYRot() * (float) (Math.PI / 180.0);
            float scale = e.getBbHeight() / 1.8F;
            Vec3 hand = e.position()
               .add((double)(-Mth.cos(yaw)) * 0.45 * (double)scale, 1.1 * (double)scale, (double)(-Mth.sin(yaw)) * 0.45 * (double)scale)
               .add((double)(-Mth.sin(yaw)) * 0.3 * (double)scale, 0.0, (double)Mth.cos(yaw) * 0.3 * (double)scale);
            Fx.spawn(
               (ParticleOptions)SeiryuParticles.BOLT_SPARK.get(), hand.x + Fx.spread(0.3), hand.y + Fx.spread(0.5), hand.z + Fx.spread(0.3), 0.0, 0.0, 0.0
            );
            if (this.age % 3 == 0) {
               VfxManager.add(new LightningEffect(hand, hand.add(Fx.spread(0.9), Fx.spread(0.9), Fx.spread(0.9)), 3, 0.03F, 0, (long)this.age * 31L).quiet());
            }
         }
      }
   }

   private static final class MouthCharge extends SeiryuClientFx.FollowEffect {
      private float progress;
      private boolean dragon;

      MouthCharge(Entity entity, int duration) {
         super(entity, duration);
      }

      @Override
      public void tick(ClientLevel level) {
         super.tick(level);
         Entity e = this.follow(level);
         if (e != null) {
            boolean var10001;
            label49: {
               this.progress = Math.min(1.0F, (float)this.age / (float)this.duration);
               if (e instanceof Player p && UoUoSeiryu.isIn(p, "dragon")) {
                  var10001 = true;
                  break label49;
               }

               var10001 = false;
            }

            this.dragon = var10001;
            double ahead = ClientFxUtil.firstPersonCaster(e) ? 1.6 : 0.5 * (double)e.getBbHeight() / 1.8;
            Vec3 mouth = this.dragon ? BreathEffect.mouth(e, 1.0F) : e.getEyePosition().add(e.getLookAngle().scale(ahead));
            this.x = mouth.x;
            this.y = mouth.y;
            this.z = mouth.z;
            int n = Fx.count((2 + (int)(this.progress * 3.0F)) * (this.dragon ? 2 : 1));
            double reach = (ClientFxUtil.firstPersonCaster(e) ? 0.6 : 1.2) * (this.dragon ? 2.5 : 1.0);

            for (int i = 0; i < n; i++) {
               double ox = Fx.spread(reach);
               double oy = Fx.spread(reach);
               double oz = Fx.spread(reach);
               Fx.spawn((ParticleOptions)SeiryuParticles.EMBER.get(), this.x + ox, this.y + oy, this.z + oz, -ox * 0.15, -oy * 0.15, -oz * 0.15);
            }
         }
      }

      @Override
      public void renderTextured(VfxContext ctx) {
         float pulse = 0.8F + 0.2F * Mth.sin(ctx.time * 1.4F);
         float near = ctx.proximity(this.x, this.y, this.z, 1.2F, 3.0F, 0.2F);
         SeiryuDraw.flare(
            ctx,
            ctx.rx(this.x),
            ctx.ry(this.y),
            ctx.rz(this.z),
            (0.15F + 0.35F * this.progress) * pulse * (this.dragon ? 3.0F : 1.0F),
            16776160,
            16758574,
            (0.4F + 0.6F * this.progress) * near
         );
      }
   }
}
