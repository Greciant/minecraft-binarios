package com.devilfruits.fruits.yomi_yomi.client.vfx;

import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.fruits.yomi_yomi.YomiSounds;
import com.devilfruits.fruits.yomi_yomi.client.YomiModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public final class IceProp {
   private static final Map<String, YomiModel> MODELS = new HashMap<>();
   private static final int CRACK = 6;
   private final YomiModel model;
   private final String coming;
   private final Vec3 pos;
   private final float yaw;
   private final float tilt;
   private final float scale;
   private final float born;
   private final float hold;
   private final RandomSource random;
   private boolean cracked;
   private boolean erupted;
   private static final float HEIGHT = 1.3F;

   public IceProp(String name, Vec3 pos, float yaw, float tilt, float scale, float born, float hold, long seed) {
      this.model = MODELS.computeIfAbsent(name, YomiModel::new);
      this.coming = name.equals("ice_blade") ? "burst" : "grow";
      this.pos = pos;
      this.yaw = yaw;
      this.tilt = tilt;
      this.scale = scale;
      this.born = born;
      this.hold = hold;
      this.random = RandomSource.create(seed);
   }

   private float comingLength() {
      return this.model.ready() ? this.model.length(this.coming) : 14.0F;
   }

   public float end() {
      return this.born + this.comingLength() + this.hold + (this.model.ready() ? this.model.length("shatter") : 16.0F);
   }

   public Vec3 pos() {
      return this.pos;
   }

   public void tick(float t) {
      float shatter = t - this.born - this.comingLength() - this.hold;
      float h = 1.3F * this.scale;
      if (!this.erupted && t >= this.born) {
         this.erupted = true;
         Frost.snow(this.random, this.pos.add(0.0, 0.2, 0.0), 0.3 * (double)this.scale, 5);
         Frost.chips(this.random, this.pos.add(0.0, 0.15, 0.0), Vec3.ZERO, 3, 0.12 * (double)this.scale);
         Frost.glint(this.random, this.pos.add(0.0, (double)h * 0.85, 0.0), 1.1F * this.scale);
         Frost.vapour(this.random, this.pos.add(0.0, 0.1, 0.0), 0.25 * (double)this.scale);
      }

      if (this.erupted && shatter < 0.0F && t >= this.born + 3.0F) {
         SoulFire.halo(this.pos.x, this.pos.y + (double)h * 0.45, this.pos.z, 1.5F * this.scale, 7334143, 0.16F);
         if (this.random.nextFloat() < 0.12F) {
            double up = (0.35 + this.random.nextDouble() * 0.6) * (double)h;
            Frost.glint(
               this.random,
               this.pos.add((this.random.nextDouble() - 0.5) * 0.3 * (double)this.scale, up, (this.random.nextDouble() - 0.5) * 0.3 * (double)this.scale),
               0.8F * this.scale
            );
         }

         if (this.random.nextFloat() < 0.07F) {
            Frost.vapour(this.random, this.pos.add(0.0, (double)h * 0.3, 0.0), 0.15 * (double)this.scale);
         }
      }

      if (!this.cracked && shatter >= 6.0F) {
         this.cracked = true;
         Vec3 mid = this.pos.add(0.0, 0.6 * (double)this.scale, 0.0);
         Frost.chips(this.random, mid, Vec3.ZERO, 14, 0.24 * (double)this.scale);
         Frost.snow(this.random, mid, 0.6 * (double)this.scale, 10);
         Frost.glint(this.random, mid.add(0.0, 0.2 * (double)this.scale, 0.0), 1.6F * this.scale);
         Frost.mist(this.random, this.pos, 0.4 * (double)this.scale, 3);
         VfxParticles.emit(YomiLook.COLD_GLOW, mid.x, mid.y, mid.z, 0.0, 0.0, 0.0).size(0.9F * this.scale).alpha(0.6F);
         Minecraft mc = Minecraft.getInstance();
         if (mc.level != null) {
            mc.level
               .playLocalSound(
                  this.pos.x,
                  this.pos.y,
                  this.pos.z,
                  YomiSounds.ICE_SHATTER.get(),
                  SoundSource.PLAYERS,
                  0.5F + 0.2F * this.scale,
                  0.9F + this.random.nextFloat() * 0.3F,
                  false
               );
         }
      }
   }

   public void draw(PoseStack ps, MultiBufferSource buffers, Vec3 cam, float t) {
      float age = t - this.born;
      if (!(age < 0.0F) && !(age > this.end() - this.born) && this.model.ready()) {
         this.model.pose();
         float len = this.model.length(this.coming);
         if (age < len + this.hold) {
            this.model.play(this.coming, age, false, 1.0F);
         } else {
            this.model.play("shatter", age - len - this.hold, false, 1.0F);
         }

         ps.pushPose();
         ps.translate(this.pos.x - cam.x, this.pos.y - cam.y, this.pos.z - cam.z);
         ps.mulPose(Axis.YP.rotationDegrees(-this.yaw));
         ps.mulPose(Axis.XP.rotationDegrees(this.tilt));
         ps.scale(this.scale / 16.0F, this.scale / 16.0F, this.scale / 16.0F);
         this.model.drawGhost(ps, buffers, 0.86F);
         ps.popPose();
      }
   }
}
