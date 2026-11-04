package com.devilfruits.client.fx;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.CameraEffects;
import com.devilfruits.client.render.vfx.VfxManager;
import com.devilfruits.client.sound.SoundDesign;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.core.config.CoreClientConfig;
import com.devilfruits.core.fx.CoreFx;
import com.devilfruits.core.network.FxPayload;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.PhysicsStructure;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.registry.DFParticles;
import com.devilfruits.registry.DFSounds;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class StructureClientFx {
   public static void register() {
      ClientFxRegistry.register(CoreFx.STRUCTURE_IMPACT, StructureClientFx::impact);
      ClientFxRegistry.register(CoreFx.STRUCTURE_DECAL, StructureClientFx::decal);
   }

   private static void impact(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      Vec3 at = p.pos();
      Vec3 dir = p.dir();
      float k = Mth.clamp(p.power(), 0.0F, 1.0F);
      BlockPos anchor = BlockPos.of(p.seed());
      BlockState state = level.getBlockState(anchor);
      int n = Fx.count(4.0F + 16.0F * k);

      for (int i = 0; i < n; i++) {
         Fx.spawn(
            (ParticleOptions)DFParticles.DUST_PUFF.get(),
            at.x + Fx.spread(0.8),
            at.y + Fx.spread(0.5),
            at.z + Fx.spread(0.8),
            dir.x * 0.06 + Fx.spread(0.05),
            0.03 + Fx.random().nextDouble() * 0.04,
            dir.z * 0.06 + Fx.spread(0.05)
         );
         if (!state.isAir() && (i & 1) == 0) {
            Fx.spawn(
               new BlockParticleOption(DFParticles.BLOCK_DEBRIS.get(), state),
               at.x + Fx.spread(0.5),
               at.y + Fx.spread(0.4),
               at.z + Fx.spread(0.5),
               dir.x * 0.2 + Fx.spread(0.15),
               0.12 + Fx.random().nextDouble() * 0.2,
               dir.z * 0.2 + Fx.spread(0.15)
            );
         }
      }

      float mass = (float)Math.max(1, p.extra());
      float pitch = Mth.clamp(1.15F - (float)Math.log10((double)(mass + 1.0F)) * 0.22F, 0.45F, 1.15F);
      SoundDesign.play(
         at,
         DFSounds.STRUCTURE_CREAK,
         0.35F + 0.9F * k,
         pitch,
         SoundDesign.layer(DFSounds.LAYER_ANVIL, 0.25F * k, pitch * 0.55F),
         SoundDesign.layer(DFSounds.LAYER_RUMBLE, 0.6F * k, 0.6F)
      );
      if (k > 0.6F) {
         SoundDesign.layerOnly(at, DFSounds.STRUCTURE_THOOM, 0.6F + 0.9F * k, 0.5F + 0.1F * (1.0F - k));
      }

      aboard(level, anchor, at, k);
   }

   private static void aboard(ClientLevel level, BlockPos anchor, Vec3 at, float k) {
      LocalPlayer me = Minecraft.getInstance().player;
      if (me != null) {
         PhysicsStructure hit = PhysicsWorld.bridge().structureAt(level, anchor);
         PhysicsStructure mine = PhysicsWorld.bridge().structureUnder(me);
         if (hit != null && mine != null && hit.id().equals(mine.id())) {
            double dist = me.position().distanceTo(at);
            float s = k * (float)(1.0 / (1.0 + dist / 14.0)) * ((Double)CoreClientConfig.STRUCTURE_SHAKE.get()).floatValue();
            if (!(s <= 0.01F)) {
               CameraEffects.shake(Math.min(1.0F, s * 0.9F));
               if (k > 0.6F) {
                  CameraEffects.fovPunch(0.05F * k);
               }

               if (k > 0.45F && (Boolean)CoreClientConfig.STRUCTURE_EDGE_DUST.get()) {
                  Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
                  Vec3 eye = camera.getPosition();
                  Vector3f look = camera.getLookVector();
                  Vector3f left = camera.getLeftVector();
                  Vector3f up = camera.getUpVector();
                  int puffs = Fx.count(6.0F * k);

                  for (int i = 0; i < puffs; i++) {
                     float side = (i & 1) == 0 ? 1.0F : -1.0F;
                     double h = Fx.spread(0.35);
                     double x = eye.x + (double)look.x() * 0.7 + (double)(left.x() * side) * 0.55 + (double)up.x() * h;
                     double y = eye.y + (double)look.y() * 0.7 + (double)(left.y() * side) * 0.55 + (double)up.y() * h;
                     double z = eye.z + (double)look.z() * 0.7 + (double)(left.z() * side) * 0.55 + (double)up.z() * h;
                     Fx.spawn((ParticleOptions)DFParticles.DUST_PUFF.get(), x, y, z, Fx.spread(0.01), -0.01, Fx.spread(0.01));
                  }
               }
            }
         }
      }
   }

   private static void decal(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      BlockImpactService.Decal[] kinds = BlockImpactService.Decal.values();
      BlockImpactService.Decal kind = kinds[Mth.clamp(p.extra(), 0, kinds.length - 1)];
      if (kind != BlockImpactService.Decal.CRACK && kind != BlockImpactService.Decal.RING) {
         if ((Integer)CoreClientConfig.STRUCTURE_DECALS.get() > 0) {
            VfxManager.add(new StructureDecalEffect(level, p.pos(), p.dir(), kind, p.power(), p.seed()));
         }

         Vec3 at = PhysicsWorld.bridge().renderPosition(level, p.pos(), 1.0F);
         if (at != null) {
            switch (kind) {
               case LIGHTNING:
                  for (int i = 0; i < Fx.count(10); i++) {
                     Fx.spawn(
                        ParticleTypes.ELECTRIC_SPARK,
                        at.x + Fx.spread(0.6),
                        at.y + Fx.spread(0.3),
                        at.z + Fx.spread(0.6),
                        Fx.spread(0.2),
                        0.1 + Fx.random().nextDouble() * 0.2,
                        Fx.spread(0.2)
                     );
                  }
                  break;
               case HAKI:
                  for (int i = 0; i < Fx.count(8); i++) {
                     Fx.spawn(ParticleTypes.SMOKE, at.x + Fx.spread(0.5), at.y + Fx.spread(0.3), at.z + Fx.spread(0.5), Fx.spread(0.04), 0.03, Fx.spread(0.04));
                  }
                  break;
               case SCORCH:
                  Fx.spawn(ParticleTypes.LARGE_SMOKE, at.x, at.y + 0.2, at.z, 0.0, 0.03, 0.0);
            }
         }
      } else {
         VfxDirector.spawn(new HullShockwave(level, p.pos(), p.dir(), p.power(), p.seed()));
      }
   }

   private StructureClientFx() {
   }
}
