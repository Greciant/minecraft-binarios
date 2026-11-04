package com.devilfruits.fruits.gura_gura.client.vfx;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.CameraEffects;
import com.devilfruits.client.render.vfx.ShockwaveEffect;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.client.render.vfx.VfxManager;
import com.devilfruits.client.sound.LoopSound;
import com.devilfruits.client.sound.SoundDesign;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.core.util.DeterministicRandom;
import com.devilfruits.fruits.gura_gura.GuraSounds;
import com.devilfruits.registry.DFParticles;
import com.devilfruits.registry.DFSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;

public class TremorEffect extends VfxEffect {
   private static final double RING_VIEW_DISTANCE = 72.0;
   private final Vec3 epicentre;
   private final float radius;
   private final int duration;
   private final int pulseInterval;
   private final DeterministicRandom ringRandom;
   private final MutableBlockPos scratch = new MutableBlockPos();

   public TremorEffect(Vec3 epicentre, float radius, int duration, int pulseInterval, long seed) {
      this.epicentre = epicentre;
      this.radius = radius;
      this.duration = duration;
      this.pulseInterval = pulseInterval;
      this.ringRandom = new DeterministicRandom(seed ^ 20900L);
      Minecraft mc = Minecraft.getInstance();
      float strength = this.strengthAtViewer();
      if (strength > 0.0F) {
         CameraEffects.roll(duration, strength);
         CameraEffects.fovPunch(0.06F * strength);
      }

      if (VfxSettings.legacy()) {
         VfxManager.add(QuakeDraw.shockwave(ShockwaveEffect.Mode.GROUND, epicentre, Vec3.ZERO, radius, 0.0F, 26, 3, 0.85F));
      }

      mc.getSoundManager().play(new LoopSound(GuraSounds.SHIMA_YURASHI_TREMOR.get(), null, epicentre, 4.0F, 0.55F, duration, () -> !this.isDone()));
   }

   private float strengthAtViewer() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null) {
         return 0.0F;
      } else {
         double dist = player.position().distanceTo(this.epicentre);
         double reach = (double)this.radius * 2.5;
         return dist >= reach ? 0.0F : (float)Math.min(1.0, 1.2 * (1.0 - dist / reach));
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.age > this.duration) {
         this.finish();
      } else {
         if (this.age % this.pulseInterval == 1) {
            this.eruptRing(level, Mth.ceil(this.radius * 2.2F), true);
         } else {
            this.eruptRing(level, 2, false);
         }

         float strength = this.strengthAtViewer();
         LocalPlayer player = Minecraft.getInstance().player;
         if (!(strength <= 0.0F) && player != null) {
            CameraEffects.sustain(0.25F + 0.45F * strength, 2);
            if (this.age % 18 == 0) {
               Vec3 at = player.position().add(Fx.spread(12.0), 0.0, Fx.spread(12.0));
               SoundDesign.layerOnly(at, DFSounds.LAYER_RUMBLE, 1.2F * strength, 0.5F + Fx.random().nextFloat() * 0.15F);
            }

            int n = Fx.count((int)(14.0F * strength));
            double area = (double)Math.min(this.radius, 18.0F);

            for (int i = 0; i < n; i++) {
               double px = player.getX() + Fx.spread(area);
               double pz = player.getZ() + Fx.spread(area);
               if (!(
                  (px - this.epicentre.x) * (px - this.epicentre.x) + (pz - this.epicentre.z) * (pz - this.epicentre.z) > (double)(this.radius * this.radius)
               )) {
                  BlockState ground = Fx.groundAt(level, px, player.getY(), pz, this.scratch);
                  if (ground != null) {
                     double py = (double)this.scratch.getY() + 1.02;
                     if (i % 3 == 0) {
                        Fx.spawn((ParticleOptions)DFParticles.DUST_PUFF.get(), px, py, pz, Fx.spread(0.02), 0.03, Fx.spread(0.02));
                     } else {
                        Fx.spawn(
                           new BlockParticleOption(DFParticles.BLOCK_DEBRIS.get(), ground),
                           px,
                           py,
                           pz,
                           Fx.spread(0.08),
                           0.2 + Fx.random().nextDouble() * 0.35,
                           Fx.spread(0.08)
                        );
                     }
                  }
               }
            }
         }
      }
   }

   private void eruptRing(ClientLevel level, int points, boolean pulse) {
      Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
      int count = Fx.count(points);
      int cracks = 0;

      for (int i = 0; i < count; i++) {
         float angle = this.ringRandom.nextFloat() * (float) (Math.PI * 2);
         double r = (double)(this.radius + this.ringRandom.signed(1.2F));
         double dirX = (double)Mth.cos(angle);
         double dirZ = (double)Mth.sin(angle);
         double px = this.epicentre.x + dirX * r;
         double pz = this.epicentre.z + dirZ * r;
         double dx = px - cam.x;
         double dz = pz - cam.z;
         if (!(dx * dx + dz * dz > 5184.0)) {
            int bx = Mth.floor(px);
            int bz = Mth.floor(pz);
            if (level.isLoaded(this.scratch.set(bx, Mth.floor(this.epicentre.y), bz))) {
               int top = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
               BlockState ground = level.getBlockState(this.scratch.set(bx, top - 1, bz));
               if (!ground.isAir() && ground.getFluidState().isEmpty()) {
                  double py = (double)top + 0.05;
                  if (!pulse) {
                     Fx.spawnFar((ParticleOptions)DFParticles.DUST_PUFF.get(), px, py + 0.2, pz, dirX * 0.02, 0.03, dirZ * 0.02);
                  } else {
                     for (int k = 0; k < 3; k++) {
                        Fx.spawnFar(
                           new BlockParticleOption(DFParticles.BLOCK_DEBRIS.get(), ground),
                           px + Fx.spread(0.6),
                           py,
                           pz + Fx.spread(0.6),
                           dirX * 0.12 + Fx.spread(0.08),
                           0.35 + Fx.random().nextDouble() * 0.45,
                           dirZ * 0.12 + Fx.spread(0.08)
                        );
                     }

                     Fx.spawnFar((ParticleOptions)DFParticles.DUST_PUFF.get(), px, py + 0.3, pz, dirX * 0.04, 0.06, dirZ * 0.04);
                     Fx.spawnFar((ParticleOptions)DFParticles.DUST_PUFF.get(), px, py + 1.0, pz, dirX * 0.02, 0.04, dirZ * 0.02);
                     if (cracks < 2 && dx * dx + dz * dz < 1600.0 && this.ringRandom.chance(0.25F)) {
                        cracks++;
                        if (VfxSettings.legacy()) {
                           VfxManager.add(
                              new PlaneCrackEffect(
                                 new Vec3(px, (double)top + 0.04, pz),
                                 new Vec3(0.0, 1.0, 0.0),
                                 1.3F,
                                 this.ringRandom.nextLong(),
                                 false,
                                 2,
                                 10,
                                 8,
                                 4,
                                 false,
                                 new Vec3(0.0, 0.15, 0.0)
                              )
                           );
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public double cullDistance() {
      return Double.MAX_VALUE;
   }

   @Override
   public double x() {
      return this.epicentre.x;
   }

   @Override
   public double y() {
      return this.epicentre.y;
   }

   @Override
   public double z() {
      return this.epicentre.z;
   }
}
