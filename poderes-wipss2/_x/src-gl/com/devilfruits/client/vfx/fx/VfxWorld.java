package com.devilfruits.client.vfx.fx;

import com.devilfruits.client.vfx.camera.CameraDirector;
import javax.annotation.Nullable;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class VfxWorld {
   private static final MutableBlockPos POS = new MutableBlockPos();

   public static double ground(Level level, double x, double y, double z, int depth) {
      POS.set(Mth.floor(x), Mth.floor(y), Mth.floor(z));

      for (int i = 0; i < depth; i++) {
         VoxelShape shape = level.getBlockState(POS).getCollisionShape(level, POS);
         if (!shape.isEmpty()) {
            return (double)POS.getY() + shape.max(Axis.Y);
         }

         POS.move(0, -1, 0);
      }

      return Double.NaN;
   }

   public static int groundColour(Level level, double x, double y, double z) {
      POS.set(Mth.floor(x), Mth.floor(y), Mth.floor(z));
      int c = level.getBlockState(POS).getMapColor(level, POS).col;
      return c == 0 ? 9075302 : c;
   }

   public static float light(Level level, double x, double y, double z) {
      POS.set(Mth.floor(x), Mth.floor(y), Mth.floor(z));
      return 0.3F + 0.7F * (float)level.getMaxLocalRawBrightness(POS) / 15.0F;
   }

   public static float surfaceLight(Level level, double x, double y, double z) {
      POS.set(Mth.floor(x), Mth.floor(y), Mth.floor(z));
      float sky = (float)level.getBrightness(LightLayer.SKY, POS) / 15.0F;
      if (level instanceof ClientLevel cl) {
         sky *= cl.getSkyDarken(1.0F);
      }

      float b = Math.max(sky, (float)level.getBrightness(LightLayer.BLOCK, POS) / 15.0F);
      return 0.08F + 0.92F * b * (float)Math.sqrt((double)b);
   }

   public static int shade(int rgb, float light) {
      int r = (int)((float)(rgb >> 16 & 0xFF) * light);
      int g = (int)((float)(rgb >> 8 & 0xFF) * light);
      int b = (int)((float)(rgb & 0xFF) * light);
      return r << 16 | g << 8 | b;
   }

   public static boolean local(int id) {
      Minecraft mc = Minecraft.getInstance();
      return mc.player != null && mc.player.getId() == id;
   }

   public static boolean firstPersonCaster(int id) {
      return local(id) && Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON;
   }

   public static float exposure(@Nullable Entity caster, Vec3 at, double near, double far) {
      if (caster != null) {
         return CameraDirector.exposure(caster, at, near, far);
      } else {
         Minecraft mc = Minecraft.getInstance();
         double d = mc.gameRenderer.getMainCamera().getPosition().distanceTo(at);
         return d <= near ? 1.0F : (d >= far ? 0.0F : (float)(1.0 - (d - near) / (far - near)));
      }
   }

   public static float nearFade(float rx, float ry, float rz) {
      float d = Mth.sqrt(rx * rx + ry * ry + rz * rz);
      return Mth.clamp((d - 0.9F) / 1.6F, 0.0F, 1.0F);
   }

   private VfxWorld() {
   }
}
