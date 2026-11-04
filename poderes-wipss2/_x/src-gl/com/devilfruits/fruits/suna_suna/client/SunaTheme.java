package com.devilfruits.fruits.suna_suna.client;

import com.devilfruits.client.logia.LogiaRenderState;
import com.devilfruits.client.logia.LogiaTheme;
import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.sound.SoundDesign;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.core.logia.DamageCategory;
import com.devilfruits.core.logia.LogiaManager;
import com.devilfruits.core.logia.LogiaState;
import com.devilfruits.core.network.LogiaHitPayload;
import com.devilfruits.fruits.suna_suna.SunaParticles;
import com.devilfruits.fruits.suna_suna.SunaSounds;
import com.devilfruits.fruits.suna_suna.client.vfx.SandCinematic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class SunaTheme implements LogiaTheme.Hooks {
   public static final int SAND = 14270082;
   public static final int SAND_EDGE = 10253376;
   public static final int WET_SAND = 9072198;
   public static final int WET_EDGE = 5521450;
   private LogiaTheme wet;

   public static LogiaTheme create() {
      SunaTheme hooks = new SunaTheme();
      hooks.wet = base(hooks)
         .colors(9072198, 5521450)
         .shed(r -> (ParticleOptions)SunaParticles.WET_CLUMP.get())
         .burst(r -> r.nextInt(3) == 0 ? (ParticleOptions)SunaParticles.CLUMP.get() : (ParticleOptions)SunaParticles.WET_CLUMP.get())
         .shedding(4.0F, 1.0F)
         .rates(0.16F, 0.3F)
         .build();
      return base(hooks)
         .colors(14270082, 10253376)
         .shed(r -> r.nextInt(5) == 0 ? (ParticleOptions)SunaParticles.CLUMP.get() : (ParticleOptions)SunaParticles.GRAIN.get())
         .burst(
            r -> r.nextInt(4) == 0
                  ? (ParticleOptions)SunaParticles.DUST.get()
                  : (r.nextInt(3) == 0 ? (ParticleOptions)SunaParticles.CLUMP.get() : (ParticleOptions)SunaParticles.GRAIN.get())
         )
         .clumps(r -> r.nextInt(3) == 0 ? (ParticleOptions)SunaParticles.CLUMP.get() : (ParticleOptions)SunaParticles.WET_CLUMP.get())
         .shedding(10.0F, 4.0F)
         .rates(0.3F, 0.22F)
         .build();
   }

   private static LogiaTheme.Builder base(SunaTheme hooks) {
      return LogiaTheme.builder()
         .partialLevel(0.9F)
         .holeTicks(26)
         .sounds(SunaSounds.DISSOLVE, SunaSounds.REFORM, SunaSounds.PASS, SunaSounds.WET, SunaSounds.REFORM_STEP)
         .passLayers(SoundDesign.layer(SunaSounds.LAYER_SAND, 0.6F, 1.3F))
         .stateIcons("suna_suna")
         .hooks(hooks);
   }

   @Override
   public LogiaTheme look(Player player, LogiaTheme theme) {
      return this.wet != null && LogiaManager.counterActive(player, "wet") ? this.wet : theme;
   }

   @Override
   public void onHit(Player player, LogiaHitPayload.Hit hit) {
      if (!VfxSettings.legacy() && !hit.haki() && !LogiaManager.counterActive(player, "wet")) {
         RandomSource r = Fx.random();
         int tint = SandCinematic.lit(player.level(), hit.pos().x, hit.pos().y, hit.pos().z);
         Vec3 dir = hit.dir().lengthSqr() < 1.0E-6 ? Vec3.ZERO : hit.dir().normalize();
         float k = hit.partial() ? 0.7F : 1.0F;
         float s = Math.max(0.6F, Math.min(2.5F, 0.5F + hit.size() * 1.2F)) * k;
         double var10004 = -dir.x * 0.03;
         double var10006 = -dir.z * 0.03;
         SandCinematic.clouds(r, hit.pos().x, hit.pos().y, hit.pos().z, var10004, 0.01, var10006, s * 0.9F, 3.0F + 2.0F * s, tint);
         Vec3 exit = hit.pos().add(dir.scale((double)player.getBbWidth()));
         SandCinematic.clouds(r, exit.x, exit.y, exit.z, dir.x * 0.12, 0.01, dir.z * 0.12, s * 1.1F, 4.0F + 3.0F * s, tint);
         SandCinematic.burst(r, exit, dir, 0.45F, 0.22F, 6.0F * s, 0.0F, tint);
         if (hit.category() == DamageCategory.MELEE.ordinal()) {
            for (int i = 0; i < Fx.count(10.0F * s); i++) {
               double ox = (r.nextDouble() - 0.5) * 0.5 * (double)s;
               double oy = (r.nextDouble() - 0.5) * 0.5 * (double)s;
               double oz = (r.nextDouble() - 0.5) * 0.5 * (double)s;
               SandCinematic.grains(
                  r,
                  hit.pos().x + ox,
                  hit.pos().y + oy,
                  hit.pos().z + oz,
                  dir.x * 0.05 + ox * 0.15,
                  0.02 + r.nextDouble() * 0.05,
                  dir.z * 0.05 + oz * 0.15,
                  tint
               );
            }

            for (int i = 0; i < Fx.count(4.0F * s); i++) {
               SandCinematic.plume(
                     r,
                     hit.pos().x + (r.nextDouble() - 0.5) * 0.4,
                     hit.pos().y - r.nextDouble() * 0.5,
                     hit.pos().z + (r.nextDouble() - 0.5) * 0.4,
                     dir.x * 0.03,
                     -0.03 - r.nextDouble() * 0.03,
                     dir.z * 0.03,
                     tint
                  )
                  .size(0.6F + 0.3F * s);
            }
         }
      }
   }

   @Override
   public void tick(Player player, LogiaRenderState state) {
      SandTravelClient.tick(player);
      RandomSource r = Fx.random();
      if (LogiaManager.counterActive(player, "wet") && state.visiblyChanged()) {
         int n = Fx.count(0.35F);

         for (int i = 0; i < n; i++) {
            Fx.spawn(
               (ParticleOptions)SunaParticles.WET_CLUMP.get(),
               player.getX() + Fx.spread(0.3),
               player.getY() + 0.4 + r.nextDouble() * (double)player.getBbHeight() * 0.8,
               player.getZ() + Fx.spread(0.3),
               0.0,
               -0.05,
               0.0
            );
         }
      }

      if (state.state() == LogiaState.ELEMENTAL_BODY && !player.isInvisible()) {
         int n = Fx.count(0.6F);

         for (int i = 0; i < n; i++) {
            double a = r.nextDouble() * Math.PI * 2.0;
            Fx.spawn(
               (ParticleOptions)SunaParticles.DUST.get(),
               player.getX() + Math.cos(a) * 0.5,
               player.getY() + r.nextDouble() * (double)player.getBbHeight(),
               player.getZ() + Math.sin(a) * 0.5,
               -Math.sin(a) * 0.04,
               0.01,
               Math.cos(a) * 0.04
            );
         }
      }
   }

   @Override
   public void dashing(Player player, Vec3 from, Vec3 to) {
      if (!player.level().getBlockState(BlockPos.containing(to.x, to.y - 0.6, to.z)).isAir()) {
         RandomSource r = Fx.random();
         int n = Fx.count((float)(1.0 + from.distanceTo(to) * 2.5));

         for (int i = 0; i < n; i++) {
            Vec3 at = from.lerp(to, r.nextDouble());
            Fx.spawn((ParticleOptions)SunaParticles.DUST.get(), at.x + Fx.spread(0.35), Math.floor(to.y + 0.01) + 0.1, at.z + Fx.spread(0.35), 0.0, 0.015, 0.0);
         }
      }
   }
}
