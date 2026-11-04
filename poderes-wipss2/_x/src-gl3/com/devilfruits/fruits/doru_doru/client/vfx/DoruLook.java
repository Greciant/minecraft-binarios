package com.devilfruits.fruits.doru_doru.client.vfx;

import com.devilfruits.client.vfx.particle.ParticleStyle;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.devilfruits.fruits.doru_doru.wax.WaxPalette;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class DoruLook {
   public static final int WAX = 16776696;
   public static final int WARM = 16767400;
   public static final int STEAM = 15921388;
   public static final ParticleStyle BEAD = ParticleStyle.colour(VfxSprites.WAX_BEAD)
      .life(16, 26)
      .size(0.13F, 0.1F, 1.0F)
      .fade(0.0F, 0.2F)
      .additive(0.0F)
      .emissive(0.0F)
      .soft(0.0F)
      .drag(0.02F)
      .gravity(0.035F)
      .collide(0.05F)
      .spin(0.0F)
      .build();
   public static final ParticleStyle SPLASH = ParticleStyle.colour(VfxSprites.WAX_SPLASH)
      .life(14, 18)
      .size(0.55F, 0.8F, 0.6F)
      .fade(0.0F, 0.25F)
      .additive(0.0F)
      .emissive(0.0F)
      .soft(0.1F)
      .drag(0.2F)
      .gravity(0.0F)
      .spin(0.0F)
      .build();
   public static final ParticleStyle PUDDLE = ParticleStyle.colour(VfxSprites.WAX_PUDDLE)
      .life(160, 240)
      .size(0.6F, 1.1F, 0.25F)
      .fade(0.02F, 0.3F)
      .additive(0.0F)
      .emissive(0.0F)
      .soft(0.0F)
      .flat()
      .spin(0.0F)
      .build();
   public static final ParticleStyle GLINT = ParticleStyle.colour(VfxSprites.WAX_GLOSS)
      .life(6, 10)
      .size(0.35F, 0.15F, 1.0F)
      .fade(0.15F, 0.6F)
      .additive(1.0F)
      .emissive(1.4F)
      .soft(0.0F)
      .drag(0.0F)
      .gravity(0.0F)
      .spin(0.0F)
      .build();
   public static final ParticleStyle FRONT_GLOW = ParticleStyle.colour(VfxSprites.GLOW)
      .life(4, 7)
      .size(0.5F, 0.2F, 1.0F)
      .fade(0.1F, 0.6F)
      .additive(1.0F)
      .emissive(0.8F)
      .soft(0.2F)
      .drag(0.0F)
      .gravity(0.0F)
      .spin(0.0F)
      .build();
   public static final ParticleStyle STEAM_PUFF = ParticleStyle.colour(VfxSprites.SMOKE_PUFF)
      .life(22, 34)
      .size(0.35F, 1.1F, 0.6F)
      .fade(0.1F, 0.7F)
      .additive(0.0F)
      .emissive(0.0F)
      .soft(0.4F)
      .drag(0.06F)
      .gravity(-0.012F)
      .spin(0.02F)
      .build();
   public static final ParticleStyle[] FLAKES = new ParticleStyle[VfxSprites.WAX_FLAKES.length];
   public static final ParticleStyle CRACK = ParticleStyle.colour(VfxSprites.WAX_CRACK)
      .life(8, 12)
      .size(0.8F, 1.3F, 0.5F)
      .fade(0.0F, 0.5F)
      .additive(0.0F)
      .emissive(0.0F)
      .soft(0.0F)
      .spin(0.0F)
      .build();
   public static final ParticleStyle FLAME = ParticleStyle.of(VfxSprites.CANDLE_FLAME, 0)
      .life(3, 4)
      .size(0.7F, 0.7F, 1.0F)
      .fade(0.0F, 0.3F)
      .heat(1.0F, 0.95F)
      .additive(1.0F)
      .emissive(1.1F)
      .soft(0.0F)
      .drag(0.0F)
      .gravity(0.0F)
      .loop(12.0F)
      .spin(0.0F)
      .build();
   public static final ParticleStyle EMBER = VfxPresets.EMBER;

   public static int tint(int color) {
      return WaxPalette.tint(color, true);
   }

   public static Vec3 hand(Player player, float partial, boolean right, boolean firstPerson) {
      if (firstPerson) {
         Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
         Vector3f look = camera.getLookVector();
         Vector3f left = camera.getLeftVector();
         Vector3f up = camera.getUpVector();
         float side = right ? -0.3F : 0.3F;
         Vec3 c = camera.getPosition();
         return c.add(
            (double)look.x * 0.65 + (double)(left.x * side) - (double)up.x * 0.22,
            (double)look.y * 0.65 + (double)(left.y * side) - (double)up.y * 0.22,
            (double)look.z * 0.65 + (double)(left.z * side) - (double)up.z * 0.22
         );
      } else {
         float yaw = Mth.lerp(partial, player.yBodyRotO, player.yBodyRot) * (float) (Math.PI / 180.0);
         double fx = (double)(-Mth.sin(yaw));
         double fz = (double)Mth.cos(yaw);
         double rx = (double)(-Mth.cos(yaw));
         double rz = (double)(-Mth.sin(yaw));
         double s = right ? 0.3 : -0.3;
         Vec3 p = player.getPosition(partial);
         double scale = (double)player.getBbHeight() / 1.8;
         return new Vec3(p.x + rx * s * scale + fx * 0.6 * scale, p.y + 1.3 * scale, p.z + rz * s * scale + fz * 0.6 * scale);
      }
   }

   private DoruLook() {
   }

   static {
      for (int i = 0; i < FLAKES.length; i++) {
         FLAKES[i] = ParticleStyle.colour(VfxSprites.WAX_FLAKES[i])
            .life(60, 90)
            .size(0.14F, 0.12F, 1.0F)
            .fade(0.1F, 0.2F)
            .additive(0.0F)
            .emissive(0.0F)
            .soft(0.0F)
            .drag(0.08F)
            .gravity(0.006F)
            .collide(0.0F)
            .spin(0.12F)
            .build();
      }
   }
}
