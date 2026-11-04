package com.devilfruits.fruits.suke_suke.client;

import com.devilfruits.fruits.suke_suke.SukeSuke;
import com.devilfruits.fruits.suke_suke.clear.Clear;
import com.devilfruits.fruits.suke_suke.clear.ShipView;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

public final class ShipLook {
   public static final int SWEEP = 22;
   public static final int WARN = 60;
   public static final int RETURN = 16;
   public static final float GLASS = 0.3F;
   public static ShipLook.Bridge bridge = new ShipLook.Bridge() {
      @Override
      public UUID shipAt(Level level, BlockPos pos) {
         return null;
      }

      @Override
      public UUID shipAround(Level level, Vec3 pos) {
         return null;
      }

      @Override
      public boolean corners(Level level, UUID ship, float partial, double[] out) {
         return false;
      }
   };

   public static boolean glassFor(ShipView.Entry e) {
      if (Visibility.viewer() != Visibility.Viewer.REAL) {
         return Visibility.viewer() != Visibility.Viewer.STRANGER;
      } else {
         Minecraft mc = Minecraft.getInstance();
         Player viewer = mc.player;
         if (viewer == null) {
            return false;
         } else if (viewer.getUUID().equals(e.group())) {
            return true;
         } else {
            Clear.State s = Clear.of(viewer);
            return s.invisible() && s.group().equals(e.group());
         }
      }
   }

   private static boolean spectator() {
      Player p = Minecraft.getInstance().player;
      return p != null && p.isSpectator() && SukeSuke.flag("spectatorsSee");
   }

   public static float alpha(@Nullable UUID ship, float partial) {
      if (ship != null && ShipView.any()) {
         Level level = Minecraft.getInstance().level;
         if (level == null) {
            return -1.0F;
         } else {
            float now = (float)level.getGameTime() + partial;
            ShipView.Entry e = ShipView.get(ship);
            if (e == null) {
               long back = ShipView.returned(ship);
               if (back < 0L) {
                  return -1.0F;
               } else {
                  float t = (now - (float)back) / 16.0F;
                  return t >= 1.0F ? -1.0F : Mth.lerp(ClearTransitions.smooth(t), 0.3F, 1.0F);
               }
            } else if (spectator()) {
               return -1.0F;
            } else {
               boolean glass = glassFor(e);
               float floor = glass ? 0.3F : 0.0F;
               float since = now - (float)e.start();
               float left = (float)e.end() - now;
               if (since < 22.0F) {
                  return Mth.lerp(ClearTransitions.smooth((since - 12.1F) / 9.9F), 1.0F, floor);
               } else {
                  return left < 60.0F ? Math.max(floor, warnPulse(since, left)) : floor;
               }
            }
         }
      } else {
         return -1.0F;
      }
   }

   public static float warnPulse(float since, float left) {
      float k = 1.0F - Mth.clamp(left / 60.0F, 0.0F, 1.0F);
      float rate = 0.05F + 0.09F * k;
      float wave = 0.5F + 0.5F * Mth.sin(since * rate * (float) (Math.PI * 2));
      return 0.15F + (0.2F + 0.2F * k) * wave;
   }

   public static boolean entityHidden(Entity e, float partial) {
      if (ShipView.any() && !(e instanceof Player)) {
         float a = alpha(bridge.shipAt(e.level(), e.blockPosition()), partial);
         return a >= 0.0F && a < 0.5F;
      } else {
         return false;
      }
   }

   public static float blockEntity(BlockEntity be, float partial) {
      return ShipView.any() && be.getLevel() != null ? alpha(bridge.shipAt(be.getLevel(), be.getBlockPos()), partial) : -1.0F;
   }

   static void onSound(PlaySoundEvent event) {
      SoundInstance sound = event.getSound();
      Level level = Minecraft.getInstance().level;
      if (sound != null && level != null && ShipView.any() && !sound.isRelative() && !(sound instanceof TickableSoundInstance)) {
         Vec3 at = new Vec3(sound.getX(), sound.getY(), sound.getZ());
         UUID ship = bridge.shipAt(level, BlockPos.containing(at));
         if (ship == null) {
            ship = bridge.shipAround(level, at);
         }

         ShipView.Entry e = ship == null ? null : ShipView.get(ship);
         if (e != null && !glassFor(e) && !spectator()) {
            float factor = (float)SukeSuke.setting("shipSoundFactor");
            event.setSound(new ShipLook.MuffledSound(sound, factor));
         }
      }
   }

   private ShipLook() {
   }

   public interface Bridge {
      @Nullable
      UUID shipAt(Level var1, BlockPos var2);

      @Nullable
      UUID shipAround(Level var1, Vec3 var2);

      boolean corners(Level var1, UUID var2, float var3, double[] var4);
   }

   private static final class MuffledSound extends AbstractSoundInstance {
      private final SoundInstance inner;
      private final float factor;

      MuffledSound(SoundInstance inner, float factor) {
         super(inner.getLocation(), inner.getSource(), SoundInstance.createUnseededRandom());
         this.inner = inner;
         this.factor = factor;
         this.x = inner.getX();
         this.y = inner.getY();
         this.z = inner.getZ();
         this.looping = inner.isLooping();
         this.delay = inner.getDelay();
         this.attenuation = inner.getAttenuation();
      }

      public WeighedSoundEvents resolve(SoundManager manager) {
         WeighedSoundEvents w = this.inner.resolve(manager);
         this.sound = this.inner.getSound();
         return w;
      }

      public float getVolume() {
         return this.inner.getVolume() * this.factor;
      }

      public float getPitch() {
         return this.inner.getPitch();
      }
   }
}
