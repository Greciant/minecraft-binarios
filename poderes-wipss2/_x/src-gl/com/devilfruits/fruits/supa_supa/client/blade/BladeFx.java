package com.devilfruits.fruits.supa_supa.client.blade;

import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.fx.ShockRing;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.core.config.CoreClientConfig;
import com.devilfruits.fruits.supa_supa.BladeLimbs;
import com.devilfruits.fruits.supa_supa.SupaSounds;
import com.devilfruits.fruits.supa_supa.client.vfx.SupaLook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

final class BladeFx {
   private static final RandomSource RANDOM = RandomSource.create();

   static void armBeat(Player player, boolean right, BladeLimbs.ArmMode mode, int beat) {
      switch (beat) {
         case 1:
            sound(player, SupaSounds.BLADE_GROW.get(), 0.7F, mode == BladeLimbs.ArmMode.CLAWS ? 1.35F : (mode == BladeLimbs.ArmMode.SAW ? 0.9F : 1.05F));
            break;
         case 2:
            settle(player, right, mode);
            break;
         case 3:
            sound(player, SupaSounds.BLADE_RETRACT.get(), 0.5F, 1.1F);
      }
   }

   static void legsBeat(Player player, int beat) {
      if (beat == 1) {
         sound(player, SupaSounds.SKATES_UNFOLD.get(), 0.7F, 1.0F);
      } else if (beat == 2) {
         Vec3 feet = player.position().add(0.0, 0.05, 0.0);
         filings(player, feet, new Vec3(0.0, -1.0, 0.0), 6);
      } else if (beat == 3) {
         sound(player, SupaSounds.BLADE_RETRACT.get(), 0.4F, 1.3F);
      }
   }

   static void steelBeat(Player player, int beat) {
      if (beat == 2 && VfxSettings.tier() != CoreClientConfig.CinematicQuality.LOW) {
         Vec3 chest = player.position().add(0.0, (double)player.getBbHeight() * 0.62, 0.0);
         if (!VfxWorld.firstPersonCaster(player.getId())) {
            ShockRing.at(chest, new Vec3(0.0, 1.0, 0.0)).radius(0.3F, 1.4F).life(9).strength(0.5F).band(0.4F).rim(0.0F, 15398143).spawn();
         }

         VfxParticles.emit(SupaLook.GLINT, chest.x, chest.y + 0.2, chest.z, 0.0, 0.0, 0.0).size(1.6F);
         filings(player, chest, new Vec3(0.0, 1.0, 0.0), 6);
      }
   }

   private static void settle(Player player, boolean right, BladeLimbs.ArmMode mode) {
      Vec3[] at = arm(player, right);
      Vec3 root = at[0];
      Vec3 tip = at[1];
      Vec3 axis = tip.subtract(root);
      if (axis.lengthSqr() < 1.0E-6) {
         axis = player.getLookAngle();
      }

      Vec3 mid = root.add(axis.scale(0.5));
      if (VfxSettings.tier() != CoreClientConfig.CinematicQuality.LOW) {
         boolean fp = VfxWorld.firstPersonCaster(player.getId());
         if (!fp) {
            ShockRing.at(mid, axis)
               .radius(0.12F, mode == BladeLimbs.ArmMode.X_BLADES ? 0.9F : 0.65F)
               .life(7)
               .strength(0.55F)
               .band(0.45F)
               .rim(0.0F, 15398143)
               .spawn();
         }

         filings(player, mid, axis.normalize(), fp ? 3 : 7);
         VfxParticles.emit(SupaLook.GLINT, tip.x, tip.y, tip.z, 0.0, 0.0, 0.0).size(fp ? 0.4F : 0.8F);
      } else {
         for (int i = 0; i < 4; i++) {
            player.level()
               .addParticle(
                  ParticleTypes.CRIT, mid.x, mid.y, mid.z, (double)((RANDOM.nextFloat() - 0.5F) * 0.3F), 0.15F, (double)((RANDOM.nextFloat() - 0.5F) * 0.3F)
               );
         }
      }
   }

   private static void filings(Player player, Vec3 at, Vec3 along, int count) {
      if (VfxSettings.tier() != CoreClientConfig.CinematicQuality.LOW) {
         for (int i = 0; i < count; i++) {
            double s = RANDOM.nextDouble();
            Vec3 p = at.add(along.scale((s - 0.5) * 0.4));
            VfxParticles.emit(
               SupaLook.FILING, p.x, p.y, p.z, (RANDOM.nextDouble() - 0.5) * 0.18, 0.05 + RANDOM.nextDouble() * 0.12, (RANDOM.nextDouble() - 0.5) * 0.18
            );
         }
      }
   }

   static Vec3[] arm(Player player, boolean right) {
      float[] var10000;
      label21: {
         if (player instanceof AbstractClientPlayer cp && !VfxWorld.firstPersonCaster(player.getId())) {
            var10000 = BladeLayer.where(cp, right);
            break label21;
         }

         var10000 = null;
      }

      float[] w = var10000;
      if (w != null) {
         return new Vec3[]{new Vec3((double)w[0], (double)w[1], (double)w[2]), new Vec3((double)w[3], (double)w[4], (double)w[5])};
      } else {
         Vec3 look = player.getLookAngle();
         Vec3 side = new Vec3(-look.z, 0.0, look.x).normalize().scale(right ? 0.38 : -0.38);
         Vec3 root = player.getEyePosition().add(look.scale(0.35)).add(side).add(0.0, -0.42, 0.0);
         return new Vec3[]{root, root.add(look.scale(0.7))};
      }
   }

   private static void sound(Player player, SoundEvent sound, float volume, float pitch) {
      if (Minecraft.getInstance().level != null) {
         Minecraft.getInstance()
            .level
            .playLocalSound(
               player.getX(), player.getY() + 1.2, player.getZ(), sound, SoundSource.PLAYERS, volume, pitch * (0.95F + RANDOM.nextFloat() * 0.1F), false
            );
      }
   }

   private BladeFx() {
   }
}
