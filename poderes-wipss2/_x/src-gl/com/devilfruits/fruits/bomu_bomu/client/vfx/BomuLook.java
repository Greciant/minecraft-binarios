package com.devilfruits.fruits.bomu_bomu.client.vfx;

import com.devilfruits.client.vfx.particle.ParticleStyle;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class BomuLook {
   public static final int HOT = 16756832;
   public static final int WHITE_HOT = 16773850;
   public static final int SOOT = 3024932;
   public static final int BOOGER = 12111936;
   public static final int BOOGER_LIGHT = 15004282;
   public static final ParticleStyle LIGHT_RAY = ParticleStyle.of(VfxSprites.PACK_SPARK, 4)
      .life(4, 7)
      .size(0.4F, 0.15F, 1.0F)
      .fade(0.0F, 0.4F)
      .heat(1.4F, 1.05F)
      .additive(1.0F)
      .emissive(3.5F)
      .soft(0.0F)
      .drag(0.12F)
      .gravity(0.0F)
      .stretch(1.5F)
      .spin(0.0F)
      .build();
   public static final ParticleStyle GLINT = ParticleStyle.of(VfxSprites.PACK_EMBER, 4)
      .life(3, 6)
      .size(0.22F, 0.1F, 1.0F)
      .fade(0.1F, 0.5F)
      .heat(1.4F, 1.1F)
      .additive(1.0F)
      .emissive(4.0F)
      .soft(0.0F)
      .drag(0.0F)
      .spin(0.0F)
      .build();
   public static final ParticleStyle DROP = ParticleStyle.colour(VfxSprites.GLOW)
      .life(10, 18)
      .size(0.12F, 0.08F, 1.0F)
      .fade(0.0F, 0.3F)
      .additive(0.15F)
      .emissive(0.3F)
      .soft(0.0F)
      .drag(0.02F)
      .gravity(0.035F)
      .collide(0.05F)
      .spin(0.0F)
      .build();
   public static final ParticleStyle WIND = ParticleStyle.colour(VfxSprites.SPARK)
      .life(10, 16)
      .size(0.5F, 0.3F, 1.0F)
      .fade(0.1F, 0.6F)
      .additive(0.0F)
      .emissive(0.0F)
      .soft(0.0F)
      .drag(0.14F)
      .gravity(0.0F)
      .stretch(1.3F)
      .spin(0.0F)
      .build();
   public static final ParticleStyle SPLAT = ParticleStyle.colour(VfxSprites.PACK_SOOT)
      .life(8, 10)
      .size(0.5F, 0.9F, 0.4F)
      .fade(0.0F, 0.6F)
      .additive(0.2F)
      .emissive(0.5F)
      .soft(0.0F)
      .spin(0.0F)
      .build();

   public static Vec3 fist(Player player, float partial, boolean mainHand, boolean firstPerson) {
      boolean right = player.getMainArm() == HumanoidArm.RIGHT == mainHand;
      if (firstPerson) {
         Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
         Vector3f look = camera.getLookVector();
         Vector3f left = camera.getLeftVector();
         Vector3f up = camera.getUpVector();
         float side = right ? -0.34F : 0.34F;
         Vec3 c = camera.getPosition();
         return c.add(
            (double)look.x * 0.62 + (double)(left.x * -side) - (double)up.x * 0.3,
            (double)look.y * 0.62 + (double)(left.y * -side) - (double)up.y * 0.3,
            (double)look.z * 0.62 + (double)(left.z * -side) - (double)up.z * 0.3
         );
      } else {
         float yaw = Mth.lerp(partial, player.yBodyRotO, player.yBodyRot) * (float) (Math.PI / 180.0);
         double fx = (double)(-Mth.sin(yaw));
         double fz = (double)Mth.cos(yaw);
         double rx = (double)(-Mth.cos(yaw));
         double rz = (double)(-Mth.sin(yaw));
         double s = right ? 0.38 : -0.38;
         Vec3 p = player.getPosition(partial);
         return new Vec3(p.x + rx * s + fx * 0.22, p.y + (double)player.getBbHeight() * 0.42, p.z + rz * s + fz * 0.22);
      }
   }

   public static Vec3 onBody(Player player, float partial, RandomSource random) {
      Vec3 p = player.getPosition(partial);
      double w = (double)player.getBbWidth() * 0.4;
      return new Vec3(
         p.x + (random.nextDouble() * 2.0 - 1.0) * w,
         p.y + 0.15 + random.nextDouble() * ((double)player.getBbHeight() - 0.3),
         p.z + (random.nextDouble() * 2.0 - 1.0) * w
      );
   }

   private BomuLook() {
   }
}
