package com.devilfruits.fruits.uo_uo_seiryu.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.fx.ChargeVisuals;
import com.devilfruits.client.fx.ClientFxRegistry;
import com.devilfruits.client.fx.ClientFxUtil;
import com.devilfruits.client.hud.AbilityBarLayer;
import com.devilfruits.client.hud.DFHud;
import com.devilfruits.client.input.DirectKeys;
import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.CameraEffects;
import com.devilfruits.client.render.vfx.FloorDebrisEffect;
import com.devilfruits.client.render.vfx.GroundDecalEffect;
import com.devilfruits.client.render.vfx.ShockwaveEffect;
import com.devilfruits.client.render.vfx.SpinRingsEffect;
import com.devilfruits.client.render.vfx.StarburstEffect;
import com.devilfruits.client.render.vfx.VfxManager;
import com.devilfruits.client.sound.LoopSound;
import com.devilfruits.client.sound.SoundDesign;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.core.network.ChargePayload;
import com.devilfruits.core.network.FxPayload;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuAbilities;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuClientConfig;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuFx;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuParticles;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuSounds;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import com.devilfruits.fruits.uo_uo_seiryu.client.render.HybridGhosts;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.AnchorFlamesEffect;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.AnchorTrailEffect;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.AscensionCinematic;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.CataclysmCinematic;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.ChargedJumpCinematic;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.EmberDriftEffect;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.FlameArcEffect;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.FlamePillarEffect;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.GroundCrackEffect;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.HellfireEffect;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.HellfireRayCinematic;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.ImpactFlareEffect;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.LightningEffect;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.RushCinematic;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.TailSlamCinematic;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.ThunderfallCinematic;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.ThunderfallEffect;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.TransformCinematic;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.VeilAuraCinematic;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.VeilAuraEffect;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.VeilCastEffect;
import com.devilfruits.fruits.uo_uo_seiryu.client.vfx.VeilCinematic;
import com.devilfruits.registry.DFSounds;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import javax.annotation.Nullable;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public final class HybridFx {
   static final ResourceLocation CLAW_SCRATCH = DevilFruits.id("textures/misc/uo_uo_seiryu/gpo/claw_scratch.png");
   static final ResourceLocation SCORCH_RING = DevilFruits.id("textures/misc/uo_uo_seiryu/gpo/scorch_ring.png");
   private static final Int2ObjectMap<long[]> VEIL = new Int2ObjectOpenHashMap();
   private static final IntSet HUMMING = new IntOpenHashSet();
   private static int hits;
   private static float hitDamage;
   private static long lastHit = -4611686018427387904L;

   public static void register(IEventBus modBus) {
      ClientFxRegistry.register(SeiryuFx.VEIL, HybridFx::veil);
      ClientFxRegistry.register(SeiryuFx.HIT, HybridFx::hit);
      ClientFxRegistry.register(SeiryuFx.RUSH, HybridFx::rush);
      ClientFxRegistry.register(SeiryuFx.RUSH_DASH, HybridFx::rushDash);
      ClientFxRegistry.register(SeiryuFx.RUSH_CLAW, HybridFx::rushClaw);
      ClientFxRegistry.register(SeiryuFx.RUSH_FINISHER, HybridFx::rushFinisher);
      ClientFxRegistry.register(SeiryuFx.THUNDERFALL, HybridFx::thunderfall);
      ClientFxRegistry.register(SeiryuFx.THUNDERFALL_STRIKE, HybridFx::thunderfallStrike);
      ClientFxRegistry.register(SeiryuFx.THUNDERFALL_MARK, HybridFx::thunderfallMark);
      ClientFxRegistry.register(SeiryuFx.THUNDERFALL_END, HybridFx::thunderfallEnd);
      ClientFxRegistry.register(SeiryuFx.HELLFIRE_WINDUP, HybridFx::hellfireWindup);
      ClientFxRegistry.register(SeiryuFx.HELLFIRE, HybridFx::hellfire);
      ClientFxRegistry.register(SeiryuFx.HELLFIRE_HIT, HybridFx::hellfireHit);
      ClientFxRegistry.register(SeiryuFx.HELLFIRE_END, HybridFx::hellfireEnd);
      VfxDirector.register(SeiryuFx.HELLFIRE_WINDUP, HellfireRayCinematic::new);
      VfxDirector.register(SeiryuFx.VEIL, VeilCinematic::new);
      VfxDirector.register(SeiryuFx.VEIL_AURA, VeilAuraCinematic::new);
      VfxDirector.register(SeiryuFx.RUSH, RushCinematic::new);
      VfxDirector.register(SeiryuFx.THUNDERFALL, ThunderfallCinematic::new);
      VfxDirector.register(SeiryuFx.ASCENSION, AscensionCinematic::new);
      VfxDirector.register(SeiryuFx.CATACLYSM_LEAP, CataclysmCinematic::new);
      VfxDirector.register(SeiryuFx.TAIL_SLAM, TailSlamCinematic::new);
      ClientFxRegistry.register(SeiryuFx.ASCENSION, HybridFx::ascension);
      ClientFxRegistry.register(SeiryuFx.ASCENSION_LAUNCH, HybridFx::ascensionLaunch);
      ClientFxRegistry.register(SeiryuFx.ASCENSION_IMPACT, HybridFx::ascensionImpact);
      ClientFxRegistry.register(SeiryuFx.CATACLYSM_LEAP, HybridFx::cataclysmLeap);
      ClientFxRegistry.register(SeiryuFx.CATACLYSM, HybridFx::cataclysm);
      ClientFxRegistry.register(SeiryuFx.TAIL_SLAM, HybridFx::tailSlam);
      ClientFxRegistry.register(SeiryuFx.CHARGED_JUMP, HybridFx::chargedJump);
      DirectKeys.enable(UoUoSeiryu.fruit().form("hybrid").id(), SeiryuClientConfig.HYBRID_DIRECT_KEYS::get);
      modBus.addListener(e -> e.registerAbove(VanillaGuiLayers.HOTBAR, UoUoSeiryu.id("hybrid_buffs"), HybridFx::renderHud));
   }

   private static void chargedJump(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      if (entity != null) {
         int beat = p.extra();
         if (!VfxSettings.legacy()) {
            ChargedJumpCinematic c = ChargedJumpCinematic.of(entity);
            if (beat == 0) {
               if (c != null) {
                  c.cancel();
               }

               VfxDirector.spawn(new ChargedJumpCinematic(level, VfxAnchor.of(entity, p.pos()), VfxParams.of(p)));
            } else if (beat == 3) {
               if (c != null) {
                  c.cancel();
               }
            } else {
               if (c == null) {
                  c = VfxDirector.spawn(new ChargedJumpCinematic(level, VfxAnchor.of(entity, p.pos()), new VfxParams(p.pos(), Vec3.ZERO, 1.0F, p.seed(), 0)));
               }

               if (beat == 1) {
                  c.launch(level, p.pos(), p.dir(), p.power());
               } else if (beat == 2) {
                  c.land(level, p.pos(), p.power());
               }
            }
         } else {
            if (beat == 1 || beat == 2) {
               float s = beat == 1 ? 0.4F + 0.6F * p.power() : Mth.clamp(p.power() / 30.0F, 0.15F, 1.0F);
               Vec3 at = p.pos();
               VfxManager.add(
                  new ShockwaveEffect(ShockwaveEffect.Mode.GROUND, at.add(0.0, 0.1, 0.0), Vec3.ZERO, 2.5F + 4.0F * s, 0.0F, 12, 2, 1.0F)
                     .tint(182, 108, 255)
                     .shell(243, 232, 255)
               );
               smokeRing(at, 0.8, Math.round(10.0F + 10.0F * s));
               burst((ParticleOptions)SeiryuParticles.VIOLET_SPARK.get(), at.add(0.0, 0.5, 0.0), Math.round(10.0F + 14.0F * s), 0.35);
               SoundDesign.play(
                  at,
                  beat == 1 ? SeiryuSounds.ASCENSION_LAUNCH : SeiryuSounds.ASCENSION_IMPACT,
                  1.2F,
                  1.0F,
                  SoundDesign.layer(DFSounds.LAYER_THUNDER, 0.5F + 0.4F * s, 1.1F)
               );
               if (ClientFxUtil.isLocal(entity)) {
                  CameraEffects.shake(0.2F + 0.4F * s);
               }
            }
         }
      }
   }

   private static void veil(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      if (entity != null) {
         if (p.extra() > 0) {
            VEIL.put(entity.getId(), new long[]{level.getGameTime() + (long)p.extra(), (long)Math.max(p.extra(), 1)});
            int delay = 0;
            if (p.power() > 0.0F) {
               if (VfxSettings.legacy()) {
                  VfxManager.add(new VeilCastEffect(entity, p.seed()));
               } else {
                  VeilCinematic old = VfxDirector.find(VeilCinematic.class, c -> c.follows(entity));
                  if (old != null) {
                     old.finish();
                  }

                  VeilCinematic cast = (VeilCinematic)VfxDirector.play(SeiryuFx.VEIL, VfxAnchor.of(entity, p.pos()), VfxParams.of(p));
                  delay = cast != null ? cast.burstTick() : 0;
                  VeilAuraCinematic aura = VeilAuraCinematic.of(entity);
                  if (aura != null) {
                     aura.finish();
                  }
               }
            }

            ensureAura(entity, delay);
         } else {
            VEIL.remove(entity.getId());
            Vec3 at = entity.position().add(0.0, (double)entity.getBbHeight() * 0.5, 0.0);
            if (VfxSettings.legacy()) {
               for (int i = 0; i < Fx.count(14); i++) {
                  Fx.spawn(
                     (ParticleOptions)SeiryuParticles.VIOLET_SPARK.get(),
                     at.x + Fx.spread(1.2),
                     at.y + Fx.spread(1.4),
                     at.z + Fx.spread(1.2),
                     Fx.spread(0.2),
                     0.05,
                     Fx.spread(0.2)
                  );
               }

               for (int i = 0; i < Fx.count(4); i++) {
                  Fx.spawn(ParticleTypes.SMOKE, at.x + Fx.spread(1.0), at.y + Fx.spread(1.0), at.z + Fx.spread(1.0), 0.0, 0.03, 0.0);
               }
            } else {
               for (int i = 0; i < VfxSettings.count(16.0F); i++) {
                  VfxParticles.emit(
                     VfxPresets.THUNDER_MOTE, at.x + Fx.spread(0.9), at.y + Fx.spread(1.3), at.z + Fx.spread(0.9), Fx.spread(0.12), 0.05, Fx.spread(0.12)
                  );
               }

               for (int i = 0; i < VfxSettings.count(5.0F); i++) {
                  VfxParticles.emit(VfxPresets.SMOKE, at.x + Fx.spread(0.8), at.y + Fx.spread(1.0), at.z + Fx.spread(0.8), 0.0, 0.04, 0.0)
                     .tint(1708080)
                     .size(0.6F)
                     .life(0.6F);
               }
            }

            SoundDesign.layerOnly(at, SeiryuSounds.VEIL_BURST, 0.5F, 1.4F);
         }
      }
   }

   private static void ensureAura(Entity e, int delay) {
      if (VfxSettings.legacy()) {
         if (VfxManager.find(VeilAuraEffect.class, a -> a.entityId() == e.getId()) == null) {
            VfxManager.add(new VeilAuraEffect(e));
         }
      } else if (VeilAuraCinematic.of(e) == null) {
         VfxDirector.play(SeiryuFx.VEIL_AURA, VfxAnchor.of(e, e.position()), new VfxParams(e.position(), Vec3.ZERO, 0.0F, (long)e.getId() * 7919L, delay));
      }

      int id = e.getId();
      if (HUMMING.add(id)) {
         Minecraft.getInstance().getSoundManager().play(new LoopSound(SeiryuSounds.VEIL_HUM.get(), e, e.position(), 0.45F, 1.0F, 0, () -> {
            boolean on = VeilAuraEffect.veiled(e);
            if (!on) {
               HUMMING.remove(id);
            }

            return on;
         }));
      }
   }

   static void veilPulse(@Nullable Entity e) {
      if (e != null && !VfxSettings.legacy()) {
         VeilAuraCinematic aura = VeilAuraCinematic.of(e);
         if (aura != null) {
            aura.pulse();
         }
      }
   }

   public static void tick(ClientLevel level) {
      HybridGhosts.tick(level);
      if (level.getGameTime() % 10L == 0L) {
         for (Player player : level.players()) {
            if (VeilAuraEffect.veiled(player)) {
               ensureAura(player, 0);
            }
         }

         VEIL.int2ObjectEntrySet().removeIf(en -> ((long[])en.getValue())[0] < level.getGameTime());
      }
   }

   private static void hit(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      long now = level.getGameTime();
      if (now - lastHit > 40L) {
         hits = 0;
         hitDamage = 0.0F;
      }

      hits++;
      hitDamage = hitDamage + p.power();
      lastHit = now;
      veilPulse(Minecraft.getInstance().player);
   }

   static Vec3 ground(ClientLevel level, Vec3 at) {
      MutableBlockPos pos = new MutableBlockPos(Mth.floor(at.x), Mth.floor(at.y), Mth.floor(at.z));

      for (int i = 0; i < 6; i++) {
         VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
         if (!shape.isEmpty()) {
            return new Vec3(at.x, (double)pos.getY() + shape.max(Axis.Y), at.z);
         }

         pos.move(0, -1, 0);
      }

      return at;
   }

   private static void rush(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      if (entity != null && !VfxSettings.legacy()) {
         RushCinematic old = VfxDirector.find(RushCinematic.class, c -> c.follows(entity));
         if (old != null) {
            old.finish();
         }

         VfxDirector.play(SeiryuFx.RUSH, VfxAnchor.of(entity, p.pos()), VfxParams.of(p));
      }
   }

   @Nullable
   private static RushCinematic rushOf(@Nullable Entity entity, FxPayload p) {
      if (entity == null) {
         return null;
      } else {
         RushCinematic c = VfxDirector.find(RushCinematic.class, r -> r.follows(entity));
         if (c == null) {
            c = (RushCinematic)VfxDirector.play(
               SeiryuFx.RUSH, VfxAnchor.of(entity, entity.position()), new VfxParams(entity.position(), p.dir(), p.power(), p.seed(), 0)
            );
         }

         return c;
      }
   }

   private static void rushDash(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      if (!VfxSettings.legacy()) {
         RushCinematic c = rushOf(entity, p);
         if (c != null) {
            c.dash(level, p.pos(), p.dir(), p.extra());
            return;
         }
      }

      boolean veiled = p.power() > 0.0F;
      Vec3 from = p.pos();
      Vec3 d = p.dir().subtract(from);
      if (entity != null) {
         HybridGhosts.start(entity, p.extra() + 2, veiled ? -1716097793 : -1711302086);
      }

      int n = Fx.count(14);

      for (int i = 0; i < n; i++) {
         Vec3 at = from.add(d.scale(Fx.random().nextDouble())).add(Fx.spread(0.5), 0.4 + Fx.random().nextDouble() * 1.6, Fx.spread(0.5));
         Fx.spawn((ParticleOptions)SeiryuParticles.FLAME.get(), at.x, at.y, at.z, d.x * 0.02, 0.02, d.z * 0.02);
         if (veiled) {
            Fx.spawn((ParticleOptions)SeiryuParticles.VIOLET_SPARK.get(), at.x, at.y, at.z, Fx.spread(0.1), 0.05, Fx.spread(0.1));
         }
      }

      SoundDesign.play(from, SeiryuSounds.RUSH_DASH, 1.2F, 1.0F, SoundDesign.layer(DFSounds.LAYER_WIND, 0.8F, 1.3F));
   }

   private static void rushClaw(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      boolean veiled = p.power() > 0.0F;
      Vec3 at = p.pos();
      Vec3 dir = p.dir().lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : p.dir().normalize();
      if (!VfxSettings.legacy()) {
         RushCinematic c = rushOf(entity, p);
         if (c != null) {
            c.claw(level, p.extra(), at, dir, nearestLiving(level, at, entity));
            return;
         }
      }

      Vec3 floor = ground(level, at);
      switch (p.extra()) {
         case 1:
            VfxManager.add(new StarburstEffect(at, 2.6F, 7, 14, p.seed()).colors(16774338, 16765503, 15217182));
            VfxManager.add(new ImpactFlareEffect(at, 2.4F, 6, 16774338, 16742938));
            VfxManager.add(
               new ShockwaveEffect(ShockwaveEffect.Mode.GROUND, floor.add(0.0, 0.05, 0.0), Vec3.ZERO, 3.2F, 0.0F, 10, 2, 0.9F)
                  .tint(255, 255, 255)
                  .shell(244, 244, 244)
            );
            smokeRing(floor, 1.2, 14);
            burst((ParticleOptions)SeiryuParticles.FLAME.get(), at, 16, 0.3);
            burst((ParticleOptions)SeiryuParticles.EMBER.get(), at, 10, 0.35);
            SoundDesign.play(
               at,
               SeiryuSounds.RUSH_CLAW,
               1.2F,
               1.0F,
               SoundDesign.layer(SeiryuSounds.LAYER_FLAME, 0.8F, 1.2F),
               SoundDesign.layer(DFSounds.LAYER_IMPACT, 0.7F, 1.1F)
            );
            break;
         case 2:
            Entity target = nearestLiving(level, at, entity);
            VfxManager.add(
               new SpinRingsEffect(at, 1.5F, 3, 14, p.seed())
                  .spin(1.4F)
                  .colors(veiled ? 15984895 : 16774338, veiled ? 8011007 : 16742938, 0.12F)
                  .follow(target, 0.5F)
            );
            VfxManager.add(
               new StarburstEffect(at, 2.0F, 6, 10, p.seed() ^ 5L)
                  .colors(veiled ? 15984895 : 16774338, veiled ? 11955455 : 16765503, veiled ? 8011007 : 16742938)
            );
            if (!veiled) {
               burst((ParticleOptions)SeiryuParticles.FLAME.get(), at, 12, 0.3);
            } else {
               for (int i = 0; i < 3; i++) {
                  float a = (float)i * (float) (Math.PI * 2) / 3.0F + (float)(p.seed() & 7L);
                  Vec3 from = at.add((double)Mth.cos(a) * 0.8, Fx.spread(0.6), (double)Mth.sin(a) * 0.8);
                  Vec3 to = from.add((double)Mth.cos(a + 1.4F) * 1.2, Fx.spread(0.8), (double)Mth.sin(a + 1.4F) * 1.2);
                  VfxManager.add(new LightningEffect(from, to, 6, 0.05F, 1, p.seed() + (long)i).quiet());
               }

               burst((ParticleOptions)SeiryuParticles.VIOLET_SPARK.get(), at, 18, 0.45);
            }

            SoundDesign.play(at, SeiryuSounds.RUSH_CLAW, 1.2F, 1.15F, SoundDesign.layer(veiled ? DFSounds.LAYER_THUNDER : SeiryuSounds.LAYER_FLAME, 0.6F, 1.6F));
            break;
         default:
            for (int i = 0; i < Fx.count(16); i++) {
               Fx.spawn(
                  ParticleTypes.LARGE_SMOKE,
                  at.x + Fx.spread(0.8),
                  at.y + Fx.spread(0.8),
                  at.z + Fx.spread(0.8),
                  Fx.spread(0.18) + dir.x * 0.12,
                  Fx.spread(0.1) + 0.04,
                  Fx.spread(0.18) + dir.z * 0.12
               );
            }

            for (int i = 0; i < Fx.count(8); i++) {
               Fx.spawn(ParticleTypes.SMOKE, at.x + Fx.spread(1.0), at.y + Fx.spread(1.0), at.z + Fx.spread(1.0), Fx.spread(0.1), 0.05, Fx.spread(0.1));
            }

            burst((ParticleOptions)SeiryuParticles.EMBER.get(), at, 18, 0.5);
            burst((ParticleOptions)SeiryuParticles.FLAME.get(), at, 10, 0.4);
            if (veiled) {
               burst((ParticleOptions)SeiryuParticles.VIOLET_SPARK.get(), at, 16, 0.5);
            }

            VfxManager.add(new ImpactFlareEffect(at, 2.0F, 5, 16765503, 8003343));
            float yaw = (float)Math.atan2(dir.z, dir.x);
            VfxManager.add(new GroundDecalEffect(floor.add(dir.scale(0.6)), CLAW_SCRATCH, 3.2F, yaw + 0.5F, 140).growIn(0.3F, 3));
            SoundDesign.play(
               at,
               SeiryuSounds.RUSH_SMASH,
               1.3F,
               1.0F,
               SoundDesign.layer(DFSounds.LAYER_BOOM, 0.6F, 1.3F),
               SoundDesign.layer(DFSounds.LAYER_IMPACT, 0.8F, 0.9F)
            );
      }

      if (ClientFxUtil.isLocal(entity)) {
         CameraEffects.shake(p.extra() == 3 ? 0.35F : 0.2F);
      } else {
         CameraEffects.shakeAt(at, 0.2F, 16.0);
      }
   }

   @Nullable
   private static Entity nearestLiving(ClientLevel level, Vec3 at, @Nullable Entity except) {
      Entity best = null;
      double bestD = 4.0;

      for (Entity e : level.getEntities(except, new AABB(at, at).inflate(1.5))) {
         if (e instanceof LivingEntity && e.getBoundingBox().getCenter().distanceToSqr(at) < bestD) {
            bestD = e.getBoundingBox().getCenter().distanceToSqr(at);
            best = e;
         }
      }

      return best;
   }

   private static void rushFinisher(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      Vec3 at = p.pos();
      float radius = p.power();
      if (!VfxSettings.legacy()) {
         RushCinematic c = rushOf(entity, new FxPayload(p.fx(), p.entityId(), p.pos(), Vec3.ZERO, 1.0F, p.seed(), 0));
         if (c != null) {
            c.finisher(level, at, radius);
            return;
         }
      }

      long seed = p.seed();
      Vec3 top = at.add(Fx.spread(1.5), 24.0, Fx.spread(1.5));
      VfxManager.add(new LightningEffect(top, at, 13, 0.42F, 4, seed));
      VfxManager.add(
         new LightningEffect(top.add(Fx.spread(3.0), 0.0, Fx.spread(3.0)), at.add(Fx.spread(1.0), 0.0, Fx.spread(1.0)), 9, 0.16F, 2, seed ^ 119L).quiet()
      );
      Vec3 mid = at.add(0.0, 1.0, 0.0);
      VfxManager.add(new ImpactFlareEffect(mid, radius * 2.0F, 12, 15984895, 11955455));
      VfxManager.add(new SpinRingsEffect(mid, radius * 0.6F, 3, 12, seed ^ 9L).spin(2.0F).colors(15984895, 8011007, 0.08F));

      for (int i = 0; i < 6; i++) {
         float a = (float)i * (float) (Math.PI * 2) / 6.0F + (float)(seed & 15L) * 0.3F;
         VfxManager.add(
            new LightningEffect(
                  mid,
                  mid.add((double)(Mth.cos(a) * radius), Fx.spread((double)radius * 0.6), (double)(Mth.sin(a) * radius)),
                  7,
                  0.06F,
                  1,
                  seed + (long)i * 31L
               )
               .quiet()
         );
      }

      VfxManager.add(new GroundDecalEffect(at, SCORCH_RING, radius * 2.6F, (float)(seed & 63L) * 0.1F, 240).growIn(0.4F, 4));
      VfxManager.add(new GroundCrackEffect(at, radius * 1.5F, 10, 80, seed, 15984895, 11955455));
      VfxManager.add(new FloorDebrisEffect(level, at, 16, 0.22F, 0.55F, 70, seed));
      VfxManager.add(
         new ShockwaveEffect(ShockwaveEffect.Mode.GROUND, at.add(0.0, 0.05, 0.0), Vec3.ZERO, radius * 2.2F, 0.0F, 12, 2, 1.0F)
            .tint(255, 255, 255)
            .shell(243, 232, 255)
      );
      burst((ParticleOptions)SeiryuParticles.VIOLET_SPARK.get(), mid, 40, 0.6);

      for (int i = 0; i < Fx.count(12); i++) {
         Fx.spawn(
            ParticleTypes.LARGE_SMOKE, at.x + Fx.spread((double)radius), at.y + 0.3, at.z + Fx.spread((double)radius), Fx.spread(0.1), 0.06, Fx.spread(0.1)
         );
      }

      SoundDesign.play(
         at,
         SeiryuSounds.RUSH_FINISHER,
         2.2F,
         0.95F,
         true,
         SoundDesign.layer(DFSounds.LAYER_THUNDER, 1.4F, 0.9F),
         SoundDesign.layer(DFSounds.LAYER_BOOM, 0.9F, 0.8F)
      );
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         double d = mc.player.position().distanceTo(at);
         if (d < 48.0) {
            CameraEffects.flash(0.3F * (float)(1.0 - d / 48.0), 11955455);
         }
      }

      CameraEffects.shakeAt(at, 0.6F, 36.0);
   }

   @Nullable
   private static ThunderfallCinematic stormOf(@Nullable Entity entity) {
      return entity == null ? null : VfxDirector.find(ThunderfallCinematic.class, c -> c.follows(entity));
   }

   private static void thunderfallMark(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      ThunderfallCinematic storm = VfxSettings.legacy() ? null : stormOf(entity);
      if (storm != null) {
         storm.mark(p.pos(), (int)p.power());
      }
   }

   private static void thunderfall(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      if (entity != null) {
         if (!VfxSettings.legacy()) {
            ThunderfallCinematic old = stormOf(entity);
            if (old != null) {
               old.end(level, false);
            }

            VfxDirector.play(SeiryuFx.THUNDERFALL, VfxAnchor.of(entity, p.pos()), VfxParams.of(p));
            Minecraft.getInstance()
               .getSoundManager()
               .play(new LoopSound(SeiryuSounds.THUNDERFALL_LOOP.get(), entity, entity.position(), 0.9F, 0.8F, p.extra() + 10, () -> {
                  ThunderfallCinematic c = stormOf(entity);
                  return c != null && !c.released();
               }));
            SoundDesign.play(entity.position(), SeiryuSounds.THUNDERFALL, 1.4F, 0.9F, SoundDesign.layer(DFSounds.LAYER_THUNDER, 0.7F, 0.7F));
         } else {
            ThunderfallEffect old = VfxManager.find(ThunderfallEffect.class, t -> t.entityId() == entity.getId());
            if (old != null) {
               old.end();
            }

            VfxManager.add(new ThunderfallEffect(level, entity, p.power(), p.extra(), p.seed()));
            Minecraft.getInstance()
               .getSoundManager()
               .play(
                  new LoopSound(
                     SeiryuSounds.THUNDERFALL_LOOP.get(),
                     entity,
                     entity.position(),
                     0.9F,
                     0.8F,
                     p.extra() + 10,
                     () -> VfxManager.find(ThunderfallEffect.class, t -> t.entityId() == entity.getId()) != null
                  )
               );
            SoundDesign.play(entity.position(), SeiryuSounds.THUNDERFALL, 1.4F, 0.9F, SoundDesign.layer(DFSounds.LAYER_THUNDER, 0.7F, 0.7F));
         }
      }
   }

   private static void thunderfallStrike(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      if (!VfxSettings.legacy()) {
         ThunderfallCinematic storm = stormOf(entity);
         if (storm == null && entity != null) {
            storm = (ThunderfallCinematic)VfxDirector.play(
               SeiryuFx.THUNDERFALL, VfxAnchor.of(entity, p.dir()), new VfxParams(p.dir(), Vec3.ZERO, 9.0F, p.seed(), 80)
            );
         }

         if (storm != null) {
            storm.strike(level, p.pos(), (int)p.power());
            return;
         }
      }

      Vec3 at = p.pos();
      long seed = p.seed();
      int number = (int)p.power();
      Vec3 top = at.add(Fx.spread(2.0), 22.0 + Fx.random().nextDouble() * 6.0, Fx.spread(2.0));
      VfxManager.add(new LightningEffect(top, at, 10, 0.34F, 4, seed));
      Vec3 user = p.dir();
      double a = (double)(seed & 1023L) / 1023.0 * Math.PI * 2.0;
      Vec3 ring = user.add(Math.cos(a) * 5.5, 0.0, Math.sin(a) * 5.5);
      VfxManager.add(
         new LightningEffect(ring.add(Fx.spread(2.0), 20.0, Fx.spread(2.0)), ground(level, ring.add(0.0, 2.0, 0.0)), 7, 0.14F, 2, seed ^ 165L).quiet()
      );
      VfxManager.add(new ImpactFlareEffect(at.add(0.0, 0.7, 0.0), 3.4F, 7, 15984895, 11955455));
      VfxManager.add(
         new ShockwaveEffect(ShockwaveEffect.Mode.GROUND, at.add(0.0, 0.06, 0.0), Vec3.ZERO, 3.6F, 0.0F, 9, 1, 0.9F).tint(255, 255, 255).shell(243, 232, 255)
      );
      VfxManager.add(new FloorDebrisEffect(level, at, 7, 0.16F, 0.5F, 80, seed));
      burst((ParticleOptions)SeiryuParticles.VIOLET_SPARK.get(), at.add(0.0, 0.5, 0.0), 14, 0.5);

      for (int i = 0; i < Fx.count(3); i++) {
         Fx.spawn(ParticleTypes.LARGE_SMOKE, at.x + Fx.spread(0.8), at.y + 0.3, at.z + Fx.spread(0.8), 0.0, 0.06, 0.0);
      }

      if (entity != null) {
         ThunderfallEffect stormx = VfxManager.find(ThunderfallEffect.class, t -> t.entityId() == entity.getId());
         if (stormx != null) {
            stormx.strike(number);
         }
      }

      SoundDesign.play(
         at,
         SeiryuSounds.THUNDERFALL_STRIKE,
         2.0F,
         0.85F + Fx.random().nextFloat() * 0.3F,
         true,
         SoundDesign.layer(DFSounds.LAYER_THUNDER, 1.2F, 1.0F),
         SoundDesign.layer(DFSounds.LAYER_BOOM, 0.5F, 1.2F)
      );
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         double d = mc.player.position().distanceTo(at);
         if (d < 40.0) {
            CameraEffects.flash(0.14F * (float)(1.0 - d / 40.0), 11955455);
         }
      }

      CameraEffects.shakeAt(at, 0.3F, 24.0);
   }

   private static void thunderfallEnd(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      ThunderfallCinematic cinematic = VfxSettings.legacy() ? null : stormOf(entity);
      if (cinematic != null) {
         cinematic.end(level, p.power() > 0.0F);
      } else {
         if (entity != null) {
            ThunderfallEffect storm = VfxManager.find(ThunderfallEffect.class, t -> t.entityId() == entity.getId());
            if (storm != null) {
               storm.end();
            }
         }

         if (!(p.power() <= 0.0F)) {
            Vec3 mid = (entity != null ? entity.position() : p.pos()).add(0.0, entity != null ? (double)entity.getBbHeight() * 0.5 : 1.5, 0.0);
            VfxManager.add(new StarburstEffect(mid, 6.5F, 10, 16, p.seed()).colors(15984895, 11955455, 8011007).streak(1.8F));
            VfxManager.add(new ImpactFlareEffect(mid, 5.0F, 9, 15984895, 8011007));
            VfxManager.add(
               new ShockwaveEffect(ShockwaveEffect.Mode.GROUND, p.pos().add(0.0, 0.08, 0.0), Vec3.ZERO, p.power() * 1.2F, 0.0F, 14, 2, 1.0F)
                  .tint(182, 108, 255)
                  .shell(243, 232, 255)
            );
            burst((ParticleOptions)SeiryuParticles.VIOLET_SPARK.get(), mid, 40, 0.7);
            SoundDesign.play(
               mid,
               SeiryuSounds.THUNDERFALL_END,
               1.8F,
               1.0F,
               SoundDesign.layer(DFSounds.LAYER_BOOM, 1.0F, 0.8F),
               SoundDesign.layer(DFSounds.LAYER_SONIC, 0.5F, 1.2F)
            );
            if (ClientFxUtil.isLocal(entity)) {
               CameraEffects.flash(0.35F, 11955455);
               CameraEffects.shake(0.6F);
               CameraEffects.fovPunch(0.1F);
            }
         }
      }
   }

   @Nullable
   private static HellfireRayCinematic hellfireOf(Entity entity) {
      return VfxDirector.find(HellfireRayCinematic.class, c -> c.follows(entity));
   }

   private static void hellfireWindup(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      if (entity != null) {
         if (VfxSettings.legacy()) {
            VfxManager.add(new AnchorFlamesEffect(entity, p.extra()));
         } else {
            HellfireRayCinematic old = hellfireOf(entity);
            if (old != null) {
               old.finish();
            }

            VfxDirector.play(SeiryuFx.HELLFIRE_WINDUP, VfxAnchor.of(entity, p.pos()), VfxParams.of(p));
         }
      }
   }

   private static void hellfire(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      if (entity != null) {
         if (VfxSettings.legacy()) {
            hellfireLegacy(level, entity, p);
         } else {
            HellfireRayCinematic c = hellfireOf(entity);
            if (c == null) {
               c = (HellfireRayCinematic)VfxDirector.play(
                  SeiryuFx.HELLFIRE_WINDUP, VfxAnchor.of(entity, p.pos()), new VfxParams(p.pos(), p.dir(), p.power(), p.seed(), 1)
               );
            }

            if (c != null) {
               c.release(level, p.extra(), p.dir());
            }
         }
      }
   }

   private static void hellfireHit(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      if (!VfxSettings.legacy()) {
         HellfireRayCinematic c = VfxDirector.find(HellfireRayCinematic.class, h -> h.near(p.dir(), 3.0));
         if (c != null) {
            c.hit(level, entity, p.pos(), (int)p.power());
         }
      }
   }

   private static void hellfireEnd(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      HellfireRayCinematic c = entity != null ? hellfireOf(entity) : null;
      if (c != null) {
         c.end(level);
      } else if (!VfxSettings.legacy() && entity != null) {
         c = (HellfireRayCinematic)VfxDirector.play(
            SeiryuFx.HELLFIRE_WINDUP, VfxAnchor.of(entity, p.pos()), new VfxParams(p.pos(), p.dir(), p.power(), p.seed(), 1)
         );
         if (c != null) {
            c.release(level, 1, p.dir());
            c.end(level);
         }
      } else {
         hellfireEndLegacy(level, entity, p);
      }
   }

   private static void hellfireLegacy(ClientLevel level, Entity entity, FxPayload p) {
      Vec3 at = p.pos();
      VfxManager.add(new StarburstEffect(at, 4.2F, 8, 16, p.seed()).colors(16774338, 16765503, 16742938));
      VfxManager.add(new ImpactFlareEffect(at, 3.4F, 7, 16774338, 16742938));
      HellfireEffect old = VfxManager.find(HellfireEffect.class, h -> h.entityId() == entity.getId());
      if (old != null) {
         old.collapse();
      }

      VfxManager.add(new HellfireEffect(entity, p.power(), p.extra(), p.seed()));
      burst((ParticleOptions)SeiryuParticles.FLAME.get(), at, 20, 0.35);
      Minecraft.getInstance()
         .getSoundManager()
         .play(
            new LoopSound(
               SeiryuSounds.HELLFIRE_LOOP.get(),
               entity,
               entity.position(),
               1.0F,
               1.0F,
               p.extra() + 10,
               () -> VfxManager.find(HellfireEffect.class, h -> h.entityId() == entity.getId()) != null
            )
         );
      SoundDesign.play(
         at, SeiryuSounds.HELLFIRE, 1.5F, 1.0F, SoundDesign.layer(SeiryuSounds.LAYER_FLAME, 1.0F, 0.9F), SoundDesign.layer(DFSounds.LAYER_BOOM, 0.5F, 1.4F)
      );
      if (ClientFxUtil.isLocal(entity)) {
         CameraEffects.flash(0.25F, 16765503);
         CameraEffects.fovPunch(0.08F);
      }
   }

   private static void hellfireEndLegacy(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      if (entity != null) {
         HellfireEffect effect = VfxManager.find(HellfireEffect.class, h -> h.entityId() == entity.getId());
         if (effect != null) {
            effect.collapse();
         }
      }

      Vec3 at = p.pos();
      float radius = p.power();
      Vec3 floor = ground(level, at);
      VfxManager.add(new StarburstEffect(at, radius * 2.4F, 10, 18, p.seed()).colors(16774338, 16742938, 15217182));
      VfxManager.add(new ImpactFlareEffect(at, radius * 2.2F, 10, 16765503, 15217182));
      VfxManager.add(
         new ShockwaveEffect(ShockwaveEffect.Mode.GROUND, floor.add(0.0, 0.06, 0.0), Vec3.ZERO, radius * 2.6F, 0.0F, 14, 3, 1.0F)
            .tint(255, 122, 26)
            .shell(255, 210, 63)
      );
      VfxManager.add(new GroundDecalEffect(floor, SCORCH_RING, radius * 2.2F, (float)(p.seed() & 63L) * 0.1F, 160).growIn(0.5F, 4));
      burst((ParticleOptions)SeiryuParticles.FLAME.get(), at, 40, 0.5);
      burst((ParticleOptions)SeiryuParticles.EMBER.get(), at, 30, 0.6);

      for (int i = 0; i < Fx.count(10); i++) {
         Fx.spawn(
            ParticleTypes.LARGE_SMOKE,
            at.x + Fx.spread((double)radius),
            at.y + Fx.spread(1.0),
            at.z + Fx.spread((double)radius),
            Fx.spread(0.1),
            0.08,
            Fx.spread(0.1)
         );
      }

      SoundDesign.play(
         at,
         SeiryuSounds.HELLFIRE_END,
         1.9F,
         0.9F,
         true,
         SoundDesign.layer(DFSounds.LAYER_BOOM, 1.0F, 0.8F),
         SoundDesign.layer(SeiryuSounds.LAYER_BLAST, 0.6F, 1.1F)
      );
      if (ClientFxUtil.isLocal(entity)) {
         CameraEffects.shake(0.55F);
         CameraEffects.flash(0.3F, 16742938);
      } else {
         CameraEffects.shakeAt(at, 0.45F, 32.0);
      }
   }

   @Nullable
   private static AscensionCinematic diveOf(@Nullable Entity entity, FxPayload p) {
      if (entity != null && !VfxSettings.legacy()) {
         AscensionCinematic c = VfxDirector.find(AscensionCinematic.class, d -> d.follows(entity));
         if (c == null) {
            c = (AscensionCinematic)VfxDirector.play(
               SeiryuFx.ASCENSION, VfxAnchor.of(entity, entity.position()), new VfxParams(entity.position(), Vec3.ZERO, 0.0F, p.seed(), 0)
            );
         }

         return c;
      } else {
         return null;
      }
   }

   private static void ascension(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      if (entity != null && !VfxSettings.legacy()) {
         AscensionCinematic old = VfxDirector.find(AscensionCinematic.class, d -> d.follows(entity));
         if (old != null) {
            old.finish();
         }

         VfxDirector.play(SeiryuFx.ASCENSION, VfxAnchor.of(entity, p.pos()), VfxParams.of(p));
      } else {
         if (entity != null) {
            VfxManager.add(new AnchorFlamesEffect(entity, p.extra()));
            SoundDesign.play(entity.position(), SeiryuSounds.ASCENSION_CHARGE, 1.1F, 1.0F, SoundDesign.layer(SeiryuSounds.LAYER_FLAME, 0.8F, 0.8F));
         }
      }
   }

   private static void ascensionLaunch(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      AscensionCinematic dive = diveOf(entity, p);
      if (dive != null) {
         dive.launch(level, p.pos(), p.dir(), p.extra());
      } else {
         Vec3 from = p.pos();
         if (entity != null) {
            VfxManager.add(new AnchorTrailEffect(entity, 4, p.extra() + 2, 0.9F * entity.getBbHeight() / 3.6F + 0.3F).length(12));
            VfxManager.add(new AnchorTrailEffect(entity, 2, p.extra() + 2, 0.4F).length(10));
         }

         VfxManager.add(
            new ShockwaveEffect(ShockwaveEffect.Mode.GROUND, from.add(0.0, 0.05, 0.0), Vec3.ZERO, 3.0F, 0.0F, 8, 1, 0.8F)
               .tint(255, 255, 255)
               .shell(244, 244, 244)
         );
         smokeRing(from, 0.9, 10);
         burst((ParticleOptions)SeiryuParticles.FLAME.get(), from.add(0.0, 0.5, 0.0), 12, 0.3);
         SoundDesign.play(
            from,
            SeiryuSounds.ASCENSION_LAUNCH,
            1.3F,
            1.0F,
            SoundDesign.layer(DFSounds.LAYER_WIND, 0.9F, 1.1F),
            SoundDesign.layer(SeiryuSounds.LAYER_FLAME, 0.7F, 1.2F)
         );
      }
   }

   private static void ascensionImpact(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      Vec3 at = p.pos();
      float radius = p.power();
      AscensionCinematic dive = diveOf(entity, p);
      if (dive != null) {
         dive.impact(level, at, radius);
      } else {
         VfxManager.add(
            new ShockwaveEffect(ShockwaveEffect.Mode.GROUND, at.add(0.0, 0.05, 0.0), Vec3.ZERO, radius * 2.2F, 0.0F, 12, 3, 1.0F)
               .tint(255, 255, 255)
               .shell(246, 246, 246)
         );
         VfxManager.add(
            new SpinRingsEffect(at.add(0.0, 0.5, 0.0), radius * 0.9F, 3, 12, p.seed()).tilt(2.0F, 12.0F).spin(2.4F).colors(16777215, 14211296, 0.12F)
         );
         VfxManager.add(new StarburstEffect(at.add(0.0, 1.0, 0.0), 3.2F, 6, 14, p.seed() ^ 3L).colors(16777215, 16765503, 16742938));
         VfxManager.add(new FloorDebrisEffect(level, at, 14, 0.2F, 0.5F, 70, p.seed()));
         VfxManager.add(new FlamePillarEffect(at, 6.0F, 0.45F, 18));
         smokeRing(at, 1.4, 18);
         burst((ParticleOptions)SeiryuParticles.EMBER.get(), at.add(0.0, 0.6, 0.0), 14, 0.4);
         SoundDesign.play(
            at,
            SeiryuSounds.ASCENSION_IMPACT,
            1.8F,
            0.9F,
            SoundDesign.layer(DFSounds.LAYER_BOOM, 1.0F, 0.9F),
            SoundDesign.layer(DFSounds.LAYER_IMPACT, 0.9F, 0.8F)
         );
         if (ClientFxUtil.isLocal(entity)) {
            CameraEffects.shake(0.55F);
            CameraEffects.fovPunch(0.1F);
         } else {
            CameraEffects.shakeAt(at, 0.45F, 32.0);
         }
      }
   }

   private static void cataclysmLeap(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      if (entity != null) {
         if (!VfxSettings.legacy()) {
            CataclysmCinematic old = VfxDirector.find(CataclysmCinematic.class, c -> c.follows(entity));
            if (old != null) {
               old.finish();
            }

            VfxDirector.play(SeiryuFx.CATACLYSM_LEAP, VfxAnchor.of(entity, p.pos()), VfxParams.of(p));
            SoundDesign.layerOnly(entity.position(), SeiryuSounds.LAYER_GROWL, 1.0F, 0.8F);
         } else {
            VfxManager.add(new AnchorFlamesEffect(entity, p.extra()));
            VfxManager.add(new AnchorTrailEffect(entity, 0, p.extra() + 30, 0.5F).length(8));
            VfxManager.add(new AnchorTrailEffect(entity, 1, p.extra() + 30, 0.5F).length(8));
            VfxManager.add(new AnchorTrailEffect(entity, 2, p.extra() + 30, 0.45F).length(10));
            if (p.power() > 0.0F) {
               burst((ParticleOptions)SeiryuParticles.VIOLET_SPARK.get(), entity.position().add(0.0, (double)entity.getBbHeight() * 0.5, 0.0), 20, 0.4);
            }

            SoundDesign.layerOnly(entity.position(), SeiryuSounds.LAYER_GROWL, 1.0F, 0.8F);
         }
      }
   }

   private static void cataclysm(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      Vec3 at = p.pos();
      float radius = p.power();
      boolean veiled = p.extra() == 1;
      if (!VfxSettings.legacy() && entity != null) {
         CataclysmCinematic c = VfxDirector.find(CataclysmCinematic.class, d -> d.follows(entity));
         if (c == null) {
            c = (CataclysmCinematic)VfxDirector.play(
               SeiryuFx.CATACLYSM_LEAP, VfxAnchor.of(entity, at), new VfxParams(at, Vec3.ZERO, veiled ? 1.0F : 0.0F, p.seed(), 0)
            );
         }

         if (c != null) {
            c.impact(level, at, radius);
            return;
         }
      }

      long seed = p.seed();
      Vec3 mid = at.add(0.0, 1.2, 0.0);
      VfxManager.add(new StarburstEffect(mid, radius * 0.9F, 10, 18, seed).colors(16774338, 16765503, 15217182).streak(1.4F));
      VfxManager.add(new ImpactFlareEffect(mid, radius * 1.1F, 12, 16774338, 16742938));
      VfxManager.add(
         new ShockwaveEffect(ShockwaveEffect.Mode.GROUND, at.add(0.0, 0.08, 0.0), Vec3.ZERO, radius * 1.8F, 0.0F, 22, 3, 1.2F)
            .tint(255, 255, 255)
            .shell(246, 246, 246)
      );
      VfxManager.add(
         new ShockwaveEffect(ShockwaveEffect.Mode.GROUND, at.add(0.0, 0.5, 0.0), Vec3.ZERO, radius * 1.1F, 0.0F, 14, 1, 1.0F)
            .tint(255, 122, 26)
            .shell(255, 210, 63)
      );
      VfxManager.add(new SpinRingsEffect(at.add(0.0, 0.6, 0.0), radius * 0.7F, 3, 16, seed ^ 4L).tilt(2.0F, 10.0F).spin(2.0F).colors(16777215, 14211296, 0.16F));
      VfxManager.add(new GroundDecalEffect(at, SCORCH_RING, radius * 1.7F, (float)(seed & 63L) * 0.1F, 300).growIn(0.3F, 5));
      VfxManager.add(new GroundCrackEffect(at, radius * 1.3F, 14, 70, seed, veiled ? 15984895 : 16774338, veiled ? 11955455 : 16742938));
      VfxManager.add(new FloorDebrisEffect(level, at, 26, 0.3F, 0.7F, 90, seed));
      VfxManager.add(new FlamePillarEffect(at, 8.0F, 0.7F, 20));
      if (veiled) {
         for (int i = 0; i < 8; i++) {
            float a = (float)i * (float) (Math.PI * 2) / 8.0F + (float)(seed & 7L) * 0.3F;
            double reach = (double)radius * (0.7 + 0.3 * (double)(seed >> i & 1L));
            Vec3 end = at.add((double)Mth.cos(a) * reach, 0.3, (double)Mth.sin(a) * reach);
            VfxManager.add(new LightningEffect(at.add(0.0, 0.4, 0.0), end, 12, 0.1F, 2, seed + (long)i * 17L));
         }

         VfxManager.add(new LightningEffect(at.add(Fx.spread(2.0), 24.0, Fx.spread(2.0)), at, 12, 0.3F, 3, seed ^ 24301L));
         burst((ParticleOptions)SeiryuParticles.VIOLET_SPARK.get(), mid, 40, 0.6);
      }

      burst((ParticleOptions)SeiryuParticles.FLAME.get(), mid, 40, 0.5);
      burst((ParticleOptions)SeiryuParticles.EMBER.get(), mid, 30, 0.6);

      for (int i = 0; i < Fx.count(14); i++) {
         Fx.spawn(
            ParticleTypes.LARGE_SMOKE,
            at.x + Fx.spread((double)radius * 0.6),
            at.y + 0.3,
            at.z + Fx.spread((double)radius * 0.6),
            Fx.spread(0.15),
            0.08,
            Fx.spread(0.15)
         );
      }

      SoundDesign.play(
         at,
         SeiryuSounds.RAGNARAKU_SLAM,
         2.2F,
         0.8F,
         SoundDesign.layer(DFSounds.LAYER_BOOM, 1.2F, 0.7F),
         SoundDesign.layer(veiled ? DFSounds.LAYER_THUNDER : SeiryuSounds.LAYER_BLAST, 1.2F, 0.9F),
         SoundDesign.layer(SeiryuSounds.LAYER_GROWL, 0.7F, 0.8F)
      );
      if (ClientFxUtil.isLocal(entity)) {
         CameraEffects.shake(1.0F);
         CameraEffects.fovPunch(0.18F);
         CameraEffects.flash(0.45F, veiled ? 11955455 : 16742938);
      } else {
         CameraEffects.shakeAt(at, 0.8F, 64.0);
      }
   }

   private static void tailSlam(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      Vec3 at = p.pos();
      float radius = p.power();
      if (entity != null && !VfxSettings.legacy()) {
         VfxDirector.play(SeiryuFx.TAIL_SLAM, VfxAnchor.of(entity, at), VfxParams.of(p));
      } else {
         VfxManager.add(new FlameArcEffect(at, p.dir(), radius * 0.85F, (float)p.extra()));
         if (entity != null) {
            VfxManager.add(new AnchorTrailEffect(entity, 2, 12, 0.6F).length(9));
         }

         Vec3 floor = ground(level, at);
         VfxManager.add(
            new SpinRingsEffect(floor.add(0.0, 0.4, 0.0), radius * 0.7F, 2, 10, p.seed()).tilt(2.0F, 8.0F).spin(-2.6F).colors(16777215, 14211296, 0.1F)
         );
         smokeRing(floor, (double)radius * 0.5, 12);
         SoundDesign.play(
            at, SeiryuSounds.TAIL_SLAM, 1.4F, 1.0F, SoundDesign.layer(DFSounds.LAYER_WIND, 1.0F, 0.9F), SoundDesign.layer(SeiryuSounds.LAYER_FLAME, 0.8F, 1.0F)
         );
         if (ClientFxUtil.isLocal(entity)) {
            CameraEffects.shake(0.25F);
         }
      }
   }

   public static ChargeVisuals.ChargeVisual windup() {
      return new ChargeVisuals.ChargeVisual() {
         @Override
         public void start(ClientLevel level, Entity entity, ChargePayload payload) {
            if (!VfxSettings.legacy()) {
               TransformCinematic old = TransformCinematic.of(entity);
               if (old != null) {
                  old.finish();
               }

               VfxDirector.spawn(
                  new TransformCinematic(
                     level,
                     VfxAnchor.of(entity, entity.position()),
                     new VfxParams(entity.position(), Vec3.ZERO, 0.0F, (long)entity.getId() * 6151L + level.getGameTime(), (int)payload.duration())
                  )
               );
            } else {
               VfxManager.add(new EmberDriftEffect(entity, (int)payload.duration(), true));
               Vec3 mid = entity.position().add(0.0, (double)entity.getBbHeight() * 0.55, 0.0);
               VfxManager.add(
                  new SpinRingsEffect(mid, entity.getBbHeight() * 0.7F, 1, 9, (long)entity.getId() * 31L)
                     .tilt(40.0F, 60.0F)
                     .spin(3.5F)
                     .colors(16777215, 15263984, 0.08F)
                     .follow(entity, 0.55F)
               );
               SoundDesign.play(entity.position(), SeiryuSounds.FORM_WINDUP, 1.0F, 1.0F, SoundDesign.layer(SeiryuSounds.LAYER_FLAME, 0.7F, 0.8F));
            }
         }

         @Override
         public void stop(ClientLevel level, Entity entity, boolean released) {
            TransformCinematic c = released ? null : TransformCinematic.of(entity);
            if (c != null) {
               c.cancel();
            }

            for (EmberDriftEffect e = VfxManager.find(EmberDriftEffect.class, d -> d.entityId() == entity.getId());
               e != null;
               e = VfxManager.find(EmberDriftEffect.class, d -> d.entityId() == entity.getId())
            ) {
               e.stop();
            }
         }
      };
   }

   public static void enterHybrid(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      if (entity != null && !VfxSettings.legacy()) {
         TransformCinematic c = TransformCinematic.of(entity);
         if (c == null) {
            c = VfxDirector.spawn(
               new TransformCinematic(level, VfxAnchor.of(entity, entity.position()), new VfxParams(entity.position(), Vec3.ZERO, 0.0F, p.seed(), 0))
            );
         }

         c.change(level);
      } else {
         Vec3 at = entity != null ? entity.position() : p.pos();
         float h = entity != null ? entity.getBbHeight() : 3.6F;
         Vec3 mid = at.add(0.0, (double)h * 0.55, 0.0);
         VfxManager.add(new StarburstEffect(mid, h * 1.4F, 10, 18, p.seed()).colors(16774338, 16765503, 16742938));
         VfxManager.add(new ImpactFlareEffect(mid, h * 1.1F, 9, 16774338, 16742938));
         VfxManager.add(
            new ShockwaveEffect(ShockwaveEffect.Mode.GROUND, at.add(0.0, 0.08, 0.0), Vec3.ZERO, h * 1.4F, 0.0F, 14, 2, 1.0F)
               .tint(255, 122, 26)
               .shell(255, 244, 194)
         );
         if (entity != null) {
            VfxManager.add(new SpinRingsEffect(mid, h * 0.55F, 3, 20, p.seed() ^ 21L).spin(2.4F).colors(16774338, 16742938, 0.14F).follow(entity, 0.55F));
            VfxManager.add(new EmberDriftEffect(entity, 40, false));
         }

         burst((ParticleOptions)SeiryuParticles.FLAME.get(), mid, 36, 0.4);
         burst((ParticleOptions)SeiryuParticles.EMBER.get(), mid, 30, 0.45);
         SoundDesign.play(
            at,
            SeiryuSounds.FORM_HYBRID,
            1.3F,
            1.0F,
            SoundDesign.layer(DFSounds.LAYER_BOOM, 0.8F, 0.9F),
            SoundDesign.layer(SeiryuSounds.LAYER_BLAST, 0.6F, 1.1F),
            SoundDesign.layer(SeiryuSounds.LAYER_GROWL, 0.8F, 1.1F)
         );
         if (ClientFxUtil.isLocal(entity)) {
            CameraEffects.flash(0.4F, 16742938);
            CameraEffects.shake(0.45F);
            CameraEffects.fovPunch(0.12F);
         } else {
            CameraEffects.shakeAt(at, 0.3F, 24.0);
         }
      }
   }

   static void burst(ParticleOptions type, Vec3 at, int count, double speed) {
      int n = Fx.count(count);

      for (int i = 0; i < n; i++) {
         Fx.spawn(type, at.x, at.y, at.z, Fx.spread(speed), Fx.spread(speed) + speed * 0.3, Fx.spread(speed));
      }
   }

   static void smokeRing(Vec3 floor, double radius, int count) {
      int n = Fx.count(count);

      for (int i = 0; i < n; i++) {
         double a = (double)i * Math.PI * 2.0 / (double)Math.max(1, n);
         Fx.spawn(
            (ParticleOptions)SeiryuParticles.CLOUD.get(),
            floor.x + Math.cos(a) * radius,
            floor.y + 0.15,
            floor.z + Math.sin(a) * radius,
            Math.cos(a) * 0.22,
            0.01,
            Math.sin(a) * 0.22
         );
      }
   }

   private static void renderHud(GuiGraphics gg, DeltaTracker delta) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player != null && !mc.options.hideGui && !DFHud.hidden() && mc.level != null) {
         renderHits(gg, mc);
         if (UoUoSeiryu.isIn(player, "hybrid")) {
            long[] veil = (long[])VEIL.get(player.getId());
            if (veil != null && VeilAuraEffect.veiled(player)) {
               float left = (float)veil[0] - ((float)mc.level.getGameTime() + delta.getGameTimeDeltaPartialTick(false));
               if (!(left <= 0.0F)) {
                  int x = AbilityBarLayer.vertical() ? AbilityBarLayer.barX() - 24 : AbilityBarLayer.barX() + 2;
                  int y = AbilityBarLayer.vertical() ? AbilityBarLayer.barY() : AbilityBarLayer.barY() - 24;
                  gg.fill(x - 1, y - 1, x + 19, y + 21, -1072690656);
                  gg.renderOutline(x - 1, y - 1, 20, 22, -8766209);
                  gg.blitSprite(SeiryuAbilities.THUNDERCLAD_VEIL.iconSprite(), x + 1, y + 1, 16, 16);
                  int strip = Math.round(18.0F * Math.min(1.0F, left / (float)veil[1]));
                  gg.fill(x, y + 18, x + strip, y + 20, -4821761);
                  String secs = String.valueOf((int)Math.ceil((double)(left / 20.0F)));
                  gg.drawString(mc.font, secs, x + 21, y + 5, -792321, true);
               }
            }
         }
      }
   }

   private static void renderHits(GuiGraphics gg, Minecraft mc) {
      long since = mc.level.getGameTime() - lastHit;
      if ((Boolean)SeiryuClientConfig.HIT_COUNTER.get() && hits > 0 && since <= 40L) {
         int x = gg.guiWidth() / 6;
         int y = gg.guiHeight() / 2 - 24;
         PoseStack pose = gg.pose();
         pose.pushPose();
         pose.translate((float)x, (float)y, 0.0F);
         pose.scale(3.0F, 3.0F, 1.0F);
         String n = String.valueOf(hits);
         gg.drawString(mc.font, n, -mc.font.width(n) / 2, 0, -50361, true);
         pose.popPose();
         pose.pushPose();
         pose.translate((float)x, (float)(y + 30), 0.0F);
         pose.scale(2.0F, 2.0F, 1.0F);
         String label = Component.translatable("hud.devilfruits.uo_uo_seiryu.hit").getString();
         gg.drawString(mc.font, label, -mc.font.width(label) / 2, 0, -1, true);
         pose.popPose();
         String dmg = String.valueOf((float)Math.round(hitDamage * 10.0F) / 10.0F);
         gg.drawString(mc.font, dmg, x - mc.font.width(dmg) / 2, y + 50, -1560034, true);
      }
   }

   private HybridFx() {
   }
}
