package com.devilfruits.client.fx;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.CameraEffects;
import com.devilfruits.client.render.vfx.ShockwaveEffect;
import com.devilfruits.client.render.vfx.StunLabelEffect;
import com.devilfruits.client.render.vfx.VfxManager;
import com.devilfruits.client.sound.SoundDesign;
import com.devilfruits.client.vfx.fx.ExplosionCinematic;
import com.devilfruits.core.fx.BombExplosionPayload;
import com.devilfruits.core.fx.CoreFx;
import com.devilfruits.core.network.FxPayload;
import com.devilfruits.registry.DFParticles;
import com.devilfruits.registry.DFSounds;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class CoreClientFx {
   public static void register() {
      ClientFxRegistry.register(CoreFx.AWAKEN, CoreClientFx::awaken);
      ClientFxRegistry.register(CoreFx.LEVEL_UP, (level, entity, p) -> levelUp(entity));
      ClientFxRegistry.register(CoreFx.REINCARNATE, (level, entity, p) -> reincarnate(p));
      ClientFxRegistry.register(CoreFx.BLOCK_BURST, CoreClientFx::blockBurst);
      ClientFxRegistry.register(CoreFx.EXPLOSION, (level, entity, p) -> ExplosionCinematic.play(level, entity, BombExplosionPayload.of(p)));
      ClientFxRegistry.register(CoreFx.SUPPRESSED, (level, entity, p) -> suppressed(entity));
      ClientFxRegistry.register(CoreFx.STUN, CoreClientFx::stun);
      ClientFxRegistry.register(CoreFx.SHAKE, (level, entity, p) -> CameraEffects.shakeAt(p.pos(), p.power(), (double)p.extra()));
      ClientFxRegistry.register(CoreFx.FORM_CHANGE, CoreClientFx::formChange);
      StructureClientFx.register();
   }

   private static void stun(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      if (entity != null) {
         StunLabelEffect label = VfxManager.find(StunLabelEffect.class, l -> l.entityId() == entity.getId());
         if (p.extra() <= 0) {
            if (label != null) {
               label.end();
            }
         } else if (label != null) {
            label.extend(level.getGameTime() + (long)p.extra());
         } else {
            VfxManager.add(new StunLabelEffect(entity, level.getGameTime() + (long)p.extra()));
         }
      }
   }

   private static void awaken(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      boolean death = p.extra() == 1;
      Vec3 center = entity != null ? entity.position().add(0.0, (double)entity.getBbHeight() * 0.55, 0.0) : p.pos();
      VfxManager.add(new ShockwaveEffect(ShockwaveEffect.Mode.GROUND, entity != null ? entity.position() : p.pos(), Vec3.ZERO, 5.0F, 0.0F, 16, 3, 0.8F));

      for (int i = 0; i < Fx.count(24); i++) {
         Fx.spawn(
            ParticleTypes.END_ROD,
            center.x + Fx.spread(1.0),
            center.y + Fx.spread(1.0),
            center.z + Fx.spread(1.0),
            Fx.spread(0.08),
            Fx.spread(0.08),
            Fx.spread(0.08)
         );
      }

      if (ClientFxUtil.isLocal(entity)) {
         CameraEffects.flash(death ? 1.0F : 0.5F);
         CameraEffects.shake(0.4F);
         CameraEffects.fovPunch(0.1F);
      }

      SoundDesign.play(center, DFSounds.FRUIT_AWAKEN, 1.3F, death ? 0.6F : 0.9F, SoundDesign.layer(DFSounds.LAYER_THUNDER, 0.5F, 0.9F));
      if (death) {
         SoundDesign.layerOnly(center, DFSounds.LAYER_BOOM, 1.5F, 0.6F);
      }
   }

   private static void levelUp(@Nullable Entity entity) {
      if (entity != null) {
         for (int i = 0; i < Fx.count(16); i++) {
            Fx.spawn(
               ParticleTypes.END_ROD,
               entity.getX() + Fx.spread(0.8),
               entity.getY() + Fx.random().nextDouble() * 2.0,
               entity.getZ() + Fx.spread(0.8),
               0.0,
               0.06,
               0.0
            );
         }

         SoundDesign.play(entity.position(), DFSounds.MASTERY_LEVEL_UP, 1.0F, 0.8F);
      }
   }

   private static void reincarnate(FxPayload p) {
      Vec3 c = p.pos();

      for (int i = 0; i < Fx.count(24); i++) {
         Fx.spawn(ParticleTypes.END_ROD, c.x + Fx.spread(0.6), c.y + Fx.random().nextDouble(), c.z + Fx.spread(0.6), 0.0, 0.05, 0.0);
      }

      SoundDesign.play(c, DFSounds.FRUIT_REINCARNATE, 1.2F, 1.0F);
   }

   private static void formChange(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      Vec3 at = entity != null ? entity.position() : p.pos();
      float height = entity != null ? entity.getBbHeight() : 1.8F;
      VfxManager.add(new ShockwaveEffect(ShockwaveEffect.Mode.GROUND, at, Vec3.ZERO, 2.5F + height * 0.5F, 0.0F, 10, 2, 0.7F));

      for (int i = 0; i < Fx.count(20); i++) {
         Fx.spawn(
            (ParticleOptions)DFParticles.DUST_PUFF.get(),
            at.x + Fx.spread(0.8),
            at.y + Fx.random().nextDouble() * (double)height,
            at.z + Fx.spread(0.8),
            Fx.spread(0.05),
            0.04,
            Fx.spread(0.05)
         );
      }

      if (ClientFxUtil.isLocal(entity)) {
         CameraEffects.fovPunch(0.06F);
      }

      SoundDesign.play(at.add(0.0, 1.0, 0.0), DFSounds.FORM_CHANGE, 1.0F, p.power() > 0.5F ? 0.8F : 1.1F, SoundDesign.layer(DFSounds.LAYER_WIND, 0.6F, 0.8F));
   }

   private static void blockBurst(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      BlockState state = Block.stateById(p.extra());
      if (!state.isAir()) {
         Vec3 at = p.pos();
         Vec3 carried = p.dir();
         int n = Fx.count(Mth.ceil(10.0F * p.power()));

         for (int i = 0; i < n; i++) {
            Fx.spawn(
               new BlockParticleOption(DFParticles.BLOCK_DEBRIS.get(), state),
               at.x + Fx.spread(0.45),
               at.y + Fx.spread(0.45),
               at.z + Fx.spread(0.45),
               carried.x + Fx.spread(0.15),
               carried.y + 0.15 + Fx.random().nextDouble() * 0.25,
               carried.z + Fx.spread(0.15)
            );
         }

         if (Fx.random().nextFloat() < 0.5F) {
            Fx.spawn((ParticleOptions)DFParticles.DUST_PUFF.get(), at.x, at.y, at.z, 0.0, 0.03, 0.0);
         }

         if (Fx.random().nextFloat() < 0.35F) {
            SoundType sound = state.getSoundType(level, BlockPos.containing(at), null);
            level.playLocalSound(at.x, at.y, at.z, sound.getBreakSound(), SoundSource.BLOCKS, 0.6F, sound.getPitch() * 0.8F, false);
         }
      }
   }

   private static void suppressed(@Nullable Entity entity) {
      if (ClientFxUtil.isLocal(entity)) {
         SoundDesign.play(entity.position(), DFSounds.POWER_SUPPRESSED, 0.8F, 0.8F);
      }
   }

   private CoreClientFx() {
   }
}
