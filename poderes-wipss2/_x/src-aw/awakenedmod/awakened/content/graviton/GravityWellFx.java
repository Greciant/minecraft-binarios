package com.awakenedmod.awakened.content.graviton;

import com.awakenedmod.awakened.content.AwakenedSounds;
import com.awakenedmod.awakened.core.BlockCracks;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.LoopSound;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

final class GravityWellFx {
   private static final String HUM = "graviton_well_hum";
   private static final String RUSH = "graviton_well_rush";
   private static final int CRACK_SETS = 64;
   private static final double CRACK_R0 = 0.8;
   private static final double CRACK_R1 = 2.4;
   private static final int CRACK_GROW = 24;
   private static final int CRACK_EVERY = 3;
   private static final int CRACK_FADE = 14;
   private static final double RUSH_FROM = 0.35;
   private static final double RUSH_FULL = 2.2;

   private GravityWellFx() {
   }

   static void open(ServerLevel level, ServerPlayer player, GravityWellFx.Well well) {
      Fx.sound(level, well.center(), (SoundEvent)AwakenedSounds.WELL_OPEN.get(), SoundSource.PLAYERS, 0.8F, 1.0F + 0.04F * (float)Math.min(well.chain(), 4));
      Fx.sound(level, well.center(), well.material().getSoundType().getPlaceSound(), SoundSource.PLAYERS, 0.9F, 0.7F);
      if (well.onFloor()) {
         cracks(level, player, well, 0);
      }
   }

   static void tick(ServerLevel level, ServerPlayer player, GravityWellFx.Well well, int age, double speed) {
      float pull = Mth.clamp((float)age / 8.0F, 0.0F, 1.0F);
      LoopSound.keepAt(
         level,
         "graviton_well_hum" + player.getId(),
         well.center(),
         (SoundEvent)AwakenedSounds.WELL_HUM.get(),
         SoundSource.PLAYERS,
         0.8F * pull,
         0.9F + 0.05F * (float)Math.min(well.chain(), 4)
      );
      float rush = (float)Mth.clamp((speed - 0.35) / 1.85, 0.0, 1.0);
      LoopSound.keep(player, "graviton_well_rush", (SoundEvent)AwakenedSounds.WELL_RUSH.get(), SoundSource.PLAYERS, 0.75F * rush, 0.75F + 0.5F * rush);
      if (well.onFloor()) {
         if (age % 3 == 0) {
            cracks(level, player, well, age);
         }
      }
   }

   static void swap(ServerLevel level, ServerPlayer player, GravityWellFx.Well old) {
      Fx.sound(level, old.center(), (SoundEvent)AwakenedSounds.WELL_SNAP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
      breakSound(level, old, 1.0F, 0.8F);
      LoopSound.stopAt(level, "graviton_well_hum" + player.getId());
      BlockCracks.release(owner(player, old), 14);
      if (old.onFloor()) {
         GravitonGround.floorKick(level, old.target().point(), 6);
      }
   }

   static void close(ServerLevel level, ServerPlayer player, GravityWellFx.Well well, boolean arrived) {
      drop(level, player, well);
      if (!arrived) {
         Fx.sound(level, well.center(), (SoundEvent)AwakenedSounds.WELL_SNAP.get(), SoundSource.PLAYERS, 0.9F, 1.1F);
         breakSound(level, well, 1.0F, 0.8F);
      } else {
         Fx.sound(level, well.center(), (SoundEvent)AwakenedSounds.WELL_SNAP.get(), SoundSource.PLAYERS, 0.5F, 0.8F);
         breakSound(level, well, 0.7F, 0.6F);
         Vec3 feet = player.position();
         Fx.Floor floor = GravitonGround.floor(level, feet);
         if (floor != null && feet.y - floor.topY() < 0.6) {
            GravitonGround.floorKick(level, feet, 12);
            Fx.sound(level, feet, floor.state().getSoundType().getFallSound(), SoundSource.PLAYERS, 0.8F, 0.8F);
         }
      }
   }

   static void drop(ServerLevel level, ServerPlayer player, GravityWellFx.Well well) {
      LoopSound.stopAt(level, "graviton_well_hum" + player.getId());
      LoopSound.stop(player, "graviton_well_rush");
      BlockCracks.release(owner(player, well), 14);
   }

   private static void breakSound(ServerLevel level, GravityWellFx.Well well, float volume, float pitch) {
      Fx.sound(level, well.center(), well.material().getSoundType().getBreakSound(), SoundSource.PLAYERS, volume, pitch);
   }

   private static Object owner(ServerPlayer player, GravityWellFx.Well well) {
      return BlockCracks.owner(player, "gravity_well_" + Math.floorMod(well.serial(), 64));
   }

   private static void cracks(ServerLevel level, ServerPlayer player, GravityWellFx.Well well, int age) {
      float grow = Mth.clamp((float)age / 24.0F, 0.0F, 1.0F);
      double radius = Mth.lerp((double)grow, 0.8, 2.4);
      int centre = 4 + Math.round(5.0F * grow);
      Vec3 at = well.target().point().subtract(0.0, 0.3, 0.0);
      BlockCracks.disc(level, owner(player, well), at, radius, centre, 1, BlockCracks.Timing.STEADY);
   }

   static record Well(GravityWellMotion.Target target, int chain, int serial, int planet, Vec3 center) {
      BlockState material() {
         return GravityWellPlanet.kind(this.planet).material();
      }

      Vec3 throat() {
         return GravityWellVisual.throat(this.target.point(), this.target.face());
      }

      boolean onFloor() {
         return this.target.surface() && this.target.face() == Direction.UP;
      }
   }
}
