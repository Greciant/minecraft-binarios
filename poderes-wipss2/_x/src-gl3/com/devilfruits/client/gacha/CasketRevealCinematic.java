package com.devilfruits.client.gacha;

import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.GroundWave;
import com.devilfruits.client.vfx.fx.ShockRing;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.post.VfxPost;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.FruitRegistry;
import com.devilfruits.core.gacha.CasketRevealPayload;
import com.devilfruits.core.gacha.GachaTier;
import com.devilfruits.registry.DFItems;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.Vec3;

public final class CasketRevealCinematic extends Cinematic {
   private static final float BASE_LENGTH = 60.0F;
   private final int openerId;
   private final GachaTier tier;
   @Nullable
   private final DevilFruit fruit;
   private final ItemStack fruitStack;
   private final ItemStack casketStack = new ItemStack((ItemLike)DFItems.CASKET.get());
   private final int length;
   private final float k;
   private final boolean simple;
   private final boolean local;
   private final RandomSource random;
   private final int fruitColour;
   private final float power;
   private final Vec3 hand;
   private final Vec3 at;
   private final Vec3 chest;
   private final float yaw;
   private final double floor;

   public CasketRevealCinematic(ClientLevel level, Entity opener, CasketRevealPayload payload) {
      super(VfxAnchor.of(opener, opener.position()), VfxParams.NONE);
      this.openerId = opener.getId();
      this.tier = payload.tier();
      this.fruit = FruitRegistry.get(payload.fruit());
      this.fruitStack = this.fruit != null && this.fruit.item().isPresent() ? new ItemStack((ItemLike)this.fruit.item().get()) : ItemStack.EMPTY;
      this.length = Math.max(20, payload.revealTicks());
      this.k = (float)this.length / 60.0F;
      this.simple = !payload.cinematic() || VfxSettings.legacy();
      this.local = VfxWorld.local(this.openerId);
      this.random = RandomSource.create(payload.seed());
      this.fruitColour = GachaLook.fruitColour(this.fruit);
      this.power = GachaLook.tierPower(this.tier);
      float yRot = opener.getYRot();
      Vec3 look = Vec3.directionFromRotation(0.0F, yRot);
      Vec3 right = new Vec3(-look.z, 0.0, look.x);
      Vec3 eye = opener.getEyePosition();
      this.hand = eye.add(look.scale(0.45)).add(right.scale(0.3)).add(0.0, -0.55, 0.0);
      this.at = eye.add(look.scale(1.5)).add(0.0, -0.65, 0.0);
      this.chest = eye.add(look.scale(0.35)).add(0.0, -0.5, 0.0);
      this.yaw = 180.0F - yRot;
      this.floor = VfxWorld.ground(level, this.at.x, this.at.y, this.at.z, 6);
      this.play(SoundEvents.VAULT_OPEN_SHUTTER, 0.9F, 1.05F);
      this.play(SoundEvents.IRON_TRAPDOOR_OPEN, 0.5F, 1.6F);
      CasketRevealClient.add(this);
   }

   private float t(float base) {
      return base * this.k;
   }

   private float time() {
      return (float)this.age + VfxClock.partial();
   }

   private Vec3 casketAt(float s) {
      float r = ease(s / this.t(12.0F));
      Vec3 p = this.hand.lerp(this.at, (double)r).add(0.0, (double)Mth.sin(r * (float) Math.PI) * 0.25, 0.0);
      float bob = 0.03F * Mth.sin(s * 0.18F);
      float sink = smooth((s - this.t(46.0F)) / this.t(10.0F));
      return p.add(0.0, (double)(bob - sink * 0.35F), 0.0);
   }

   private float casketScale(float s) {
      float grow = Mth.lerp(ease(s / this.t(12.0F)), 0.38F, 0.75F);
      return grow * (1.0F - smooth((s - this.t(46.0F)) / this.t(10.0F)));
   }

   private float lidAngle(float s) {
      float x = Mth.clamp((s - this.t(12.0F)) / this.t(10.0F), 0.0F, 1.0F);
      return -110.0F * backOut(x) - (s >= this.t(12.0F) && s < this.t(13.5F) ? 6.0F : 0.0F);
   }

   private Vec3 fruitAt(float s, Vec3 casket, float scale) {
      Vec3 inside = casket.add(0.0, (double)(0.3125F * scale), 0.0);
      Vec3 top = casket.add(0.0, 0.95, 0.0);
      float rise = ease((s - this.t(16.0F)) / this.t(20.0F));
      Vec3 p = inside.lerp(top, (double)rise).add(0.0, (double)(0.05F * Mth.sin(s * 0.25F) * rise), 0.0);
      float drop = smooth((s - this.t(48.0F)) / this.t(12.0F));
      return p.lerp(this.chest, (double)drop);
   }

   private float fruitScale(float s) {
      float rise = ease((s - this.t(16.0F)) / this.t(20.0F));
      float drop = smooth((s - this.t(48.0F)) / this.t(12.0F));
      return Mth.lerp(drop, Mth.lerp(rise, 0.25F, 0.8F), 0.22F);
   }

   private int colour(float s) {
      return GachaLook.mix(16763210, this.fruitColour, smooth((s - this.t(34.0F)) / this.t(6.0F)));
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.age > this.length + 2) {
         this.finish();
      } else {
         float s = (float)this.age;
         Vec3 casket = this.casketAt(s);
         float scale = this.casketScale(s);
         Vec3 mouth = casket.add(0.0, (double)(0.40625F * scale), 0.0);
         int col = this.colour(s);
         if (this.age == Math.round(this.t(12.0F))) {
            this.play(SoundEvents.CHEST_OPEN, 0.8F, 0.75F);
            this.play(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 0.9F);
            this.burst(level, mouth, 0.4F);
         }

         if (this.age == Math.round(this.t(17.0F))) {
            this.play(SoundEvents.ENCHANTMENT_TABLE_USE, 0.7F, 1.3F);
         }

         if (this.age == Math.round(this.t(38.0F))) {
            this.finale(level, this.fruitAt(s, casket, scale));
         }

         if (this.age == Math.round(this.t(48.0F))) {
            this.play(SoundEvents.ALLAY_ITEM_GIVEN, 0.7F, 1.2F);
         }

         boolean open = s > this.t(12.0F) && s < this.t(54.0F);
         if (this.simple) {
            this.vanilla(level, s, casket, mouth, open);
         } else {
            if (s < this.t(12.0F) && this.random.nextFloat() < 0.7F) {
               VfxParticles.emit(
                     GachaLook.MOTE, casket.x + this.spread(0.12), casket.y + this.spread(0.1) + 0.15, casket.z + this.spread(0.12), 0.0, 0.004, 0.0
                  )
                  .tint(16770976)
                  .alpha(0.8F);
            }

            if (open) {
               float light = smooth((s - this.t(12.0F)) / this.t(6.0F)) * (1.0F - smooth((s - this.t(46.0F)) / this.t(8.0F)));
               VfxParticles.emit(GachaLook.HALO, mouth.x, mouth.y + 0.1, mouth.z, 0.0, 0.0, 0.0)
                  .size(1.1F + 0.25F * this.power * light)
                  .tint(col)
                  .alpha(0.4F * light);

               for (int i = 0; i < VfxSettings.count(1.5F + this.power); i++) {
                  VfxParticles.emit(
                        GachaLook.RAY,
                        mouth.x + this.spread(0.2),
                        mouth.y,
                        mouth.z + this.spread(0.12),
                        this.spread(0.03),
                        0.09 + (double)this.random.nextFloat() * 0.08,
                        this.spread(0.03)
                     )
                     .tint(i % 2 == 0 ? 16770976 : col)
                     .alpha(0.7F * light);
               }

               if (this.random.nextFloat() < 0.5F + 0.15F * this.power) {
                  VfxParticles.emit(
                        GachaLook.MOTE,
                        mouth.x + this.spread(0.4),
                        mouth.y + (double)this.random.nextFloat() * 0.8,
                        mouth.z + this.spread(0.4),
                        this.spread(0.006),
                        0.008,
                        this.spread(0.006)
                     )
                     .tint(col)
                     .alpha(0.9F * light);
               }
            }

            if (s >= this.t(38.0F) && s < this.t(52.0F) && this.power >= 2.0F && this.random.nextFloat() < 0.8F) {
               Vec3 f = this.fruitAt(s, casket, scale);
               VfxParticles.emit(GachaLook.MOTE, f.x + this.spread(1.4), f.y + this.spread(1.0), f.z + this.spread(1.4), 0.0, 0.001, 0.0)
                  .tint(col)
                  .size(1.3F)
                  .life(1.4F);
            }

            if (this.local && this.power >= 3.0F && s > this.t(32.0F) && s < this.t(38.0F)) {
               CameraDirector.dolly(0.9F, 0.12F);
            }
         }
      }
   }

   private void burst(ClientLevel level, Vec3 mouth, float size) {
      if (this.simple) {
         for (int i = 0; i < 10; i++) {
            level.addParticle(
               ParticleTypes.END_ROD, mouth.x, mouth.y, mouth.z, this.spread(0.08), 0.06 + (double)this.random.nextFloat() * 0.06, this.spread(0.08)
            );
         }
      } else {
         VfxParticles.emit(GachaLook.HALO, mouth.x, mouth.y, mouth.z, 0.0, 0.0, 0.0).size(2.0F * size / 0.4F).tint(16770976).alpha(0.75F).life(2.0F);

         for (int i = 0; i < VfxSettings.count(10.0F); i++) {
            VfxParticles.emit(GachaLook.GLINT, mouth.x, mouth.y, mouth.z, this.spread(0.12), 0.08 + (double)this.random.nextFloat() * 0.1, this.spread(0.12))
               .tint(16770976);
         }
      }
   }

   private void finale(ClientLevel level, Vec3 f) {
      int col = this.fruitColour;
      this.play(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.2F);
      this.play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 1.6F);
      this.play(SoundEvents.PLAYER_LEVELUP, 0.35F, 1.5F);
      if (this.simple) {
         int n = 20 + 20 * (int)this.power;

         for (int i = 0; i < n; i++) {
            level.addParticle(
               i % 3 == 0 ? ParticleTypes.FIREWORK : ParticleTypes.END_ROD, f.x, f.y, f.z, this.spread(0.15), this.spread(0.15) + 0.03, this.spread(0.15)
            );
         }

         if (this.power >= 2.0F) {
            for (int i = 0; i < 40; i++) {
               level.addParticle(ParticleTypes.END_ROD, f.x + this.spread(0.2), f.y + (double)i * 0.25, f.z + this.spread(0.2), 0.0, 0.05, 0.0);
            }
         }

         this.chords();
      } else {
         VfxParticles.emit(GachaLook.HALO, f.x, f.y, f.z, 0.0, 0.0, 0.0).size(1.4F + 0.3F * this.power).tint(col).alpha(0.5F).life(4.0F);
         VfxParticles.emit(GachaLook.RING, f.x, f.y - 0.1, f.z, 0.0, 0.0, 0.0).tint(col).alpha(0.8F).size(1.0F + 0.35F * this.power);

         for (int i = 0; i < VfxSettings.count(10.0F + 8.0F * this.power); i++) {
            VfxParticles.emit(GachaLook.MOTE, f.x, f.y, f.z, this.spread(0.09), this.spread(0.09) + 0.02, this.spread(0.09)).tint(i % 3 == 0 ? 16770976 : col);
         }

         if (this.power >= 1.0F) {
            for (int i = 0; i < VfxSettings.count(18.0F + 10.0F * this.power); i++) {
               double a = this.random.nextDouble() * Math.PI * 2.0;
               double sp = 0.12 + this.random.nextDouble() * 0.12;
               VfxParticles.emit(GachaLook.GLINT, f.x, f.y, f.z, Math.cos(a) * sp, 0.06 + this.random.nextDouble() * 0.12, Math.sin(a) * sp)
                  .tint(i % 2 == 0 ? 16774876 : col);
            }

            this.play(SoundEvents.AMETHYST_CLUSTER_BREAK, 0.8F, 1.5F);
            this.note(SoundEvents.NOTE_BLOCK_CHIME, 0.8F, 1.0F);
            this.note(SoundEvents.NOTE_BLOCK_CHIME, 0.7F, 1.5F);
         }

         if (this.power >= 2.0F) {
            VfxDirector.spawn(ShockRing.at(f, new Vec3(0.0, 1.0, 0.0)).radius(0.4F, 4.5F + this.power).life(14).strength(0.7F).rim(0.35F, col));
            this.play(SoundEvents.BEACON_ACTIVATE, 0.9F, 1.3F);
            this.play(SoundEvents.BELL_BLOCK, 0.5F, 1.6F);
            if (this.local) {
               VfxClock.slowMo(12, 0.5F);
               CameraDirector.shake(0.25F + 0.1F * this.power, 0.05F, 1.2F, 9.0F);
            }
         }

         if (this.power >= 3.0F) {
            VfxDirector.spawn(ShockRing.at(f, new Vec3(0.0, 1.0, 0.0)).radius(0.6F, 9.0F).life(20).strength(1.0F).rim(0.5F, 16774876));
            VfxDirector.spawn(GroundWave.at(f.x, this.floor, f.z, 9.0F, 22).strength(0.8F).ring(col, 0.6F));
            VfxPost.flash(16774876, 0.2F);
            VfxPost.bloomBoost(0.4F);
            if (this.local) {
               CameraDirector.fovPunch(0.06F);
               CameraDirector.shake(0.7F, 0.035F, 2.2F, 14.0F);
            }

            this.chords();
         }
      }
   }

   private void chords() {
      if (this.power >= 3.0F) {
         this.note(SoundEvents.NOTE_BLOCK_BELL, 1.0F, 0.5F);
         this.note(SoundEvents.NOTE_BLOCK_BELL, 0.9F, 0.63F);
         this.note(SoundEvents.NOTE_BLOCK_BELL, 0.9F, 0.75F);
         this.note(SoundEvents.NOTE_BLOCK_BASS, 1.0F, 0.5F);
         this.play(SoundEvents.BEACON_POWER_SELECT, 0.8F, 0.7F);
         this.play(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.45F, 1.0F);
      } else if (this.simple && this.power >= 1.0F) {
         this.note(SoundEvents.NOTE_BLOCK_CHIME, 0.8F, 1.2F);
      }
   }

   private void vanilla(ClientLevel level, float s, Vec3 casket, Vec3 mouth, boolean open) {
      if (open && this.random.nextFloat() < 0.6F) {
         level.addParticle(ParticleTypes.END_ROD, mouth.x + this.spread(0.15), mouth.y, mouth.z + this.spread(0.1), this.spread(0.01), 0.05, this.spread(0.01));
      }

      if (open && this.random.nextFloat() < 0.4F) {
         level.addParticle(
            ParticleTypes.ENCHANT, mouth.x + this.spread(0.6), mouth.y + 0.6, mouth.z + this.spread(0.6), this.spread(0.5), -0.3, this.spread(0.5)
         );
      }
   }

   @Override
   public void render(VfxFrame frame) {
      if (!this.simple && !(this.power < 1.0F)) {
         float s = this.time();
         float on = smooth((s - this.t(36.0F)) / this.t(3.0F)) * (1.0F - smooth((s - this.t(50.0F)) / this.t(8.0F)));
         if (!(on <= 0.01F) && frame.allowMesh()) {
            Vec3 f = this.fruitAt(s, this.casketAt(s), this.casketScale(s));
            float height = this.power >= 3.0F ? 160.0F : (this.power >= 2.0F ? 90.0F : 3.5F);
            float width = (this.power >= 3.0F ? 0.55F : (this.power >= 2.0F ? 0.32F : 0.22F)) * on;
            BufferBuilder b = frame.mesh(MeshTex.LANCE);
            int n = 6;
            float[] xs = new float[n];
            float[] ys = new float[n];
            float[] zs = new float[n];
            float[] w = new float[n];
            float[] h = new float[n];
            float[] a = new float[n];

            for (int i = 0; i < n; i++) {
               float u = (float)i / (float)(n - 1);
               xs[i] = frame.rx(f.x);
               ys[i] = frame.ry(f.y + 0.45 + (double)(u * height));
               zs[i] = frame.rz(f.z);
               w[i] = width * (this.power >= 2.0F ? 1.0F : 1.0F - 0.6F * u);
               h[i] = 1.05F;
               a[i] = on * (this.power >= 2.0F ? 1.0F - 0.5F * u : 1.0F - u) * VfxWorld.nearFade(xs[i], ys[i], zs[i]);
            }

            MeshWriter.strip(b, n, xs, ys, zs, w, h, a, -s * 0.5F, 0.12F, 16777215, 0, 2.2F);

            for (int i = 0; i < n; i++) {
               w[i] *= 0.35F;
               h[i] = 1.4F;
            }

            MeshWriter.strip(b, n, xs, ys, zs, w, h, a, -s * 0.9F, 0.12F, 16777215, 0, 3.2F);
         }
      }
   }

   void renderModels(PoseStack pose, MultiBufferSource buffers, Vec3 camera) {
      float s = this.time();
      if (!(s > (float)this.length)) {
         Vec3 casket = this.casketAt(s);
         float scale = this.casketScale(s);
         int light = 15728880;
         if (scale > 0.01F) {
            pose.pushPose();
            pose.translate(casket.x - camera.x, casket.y - camera.y, casket.z - camera.z);
            pose.mulPose(Axis.YP.rotationDegrees(this.yaw + (s < this.t(12.0F) ? (1.0F - ease(s / this.t(12.0F))) * 25.0F : 0.0F)));
            pose.scale(scale, scale, scale);
            pose.translate(-0.5F, 0.0625F, -0.5F);
            CasketItemRenderer.drawModel(CasketItemRenderer.BASE, this.casketStack, pose, buffers, light, OverlayTexture.NO_OVERLAY);
            pose.pushPose();
            pose.rotateAround(Axis.XP.rotationDegrees(this.lidAngle(s)), 0.0F, 0.4375F, 0.1875F);
            CasketItemRenderer.drawModel(CasketItemRenderer.LID, this.casketStack, pose, buffers, light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
            if (s < this.t(14.0F)) {
               CasketItemRenderer.glow(pose, buffers, 1.0F - smooth((s - this.t(12.0F)) / this.t(2.0F)), s);
            }

            pose.popPose();
         }

         if (!this.fruitStack.isEmpty() && s > this.t(16.0F)) {
            Vec3 f = this.fruitAt(s, casket, scale);
            float fs = this.fruitScale(s);
            pose.pushPose();
            pose.translate(f.x - camera.x, f.y - camera.y, f.z - camera.z);
            pose.mulPose(Axis.YP.rotationDegrees(s * 9.0F));
            pose.scale(fs, fs, fs);
            Minecraft.getInstance()
               .getItemRenderer()
               .renderStatic(
                  this.fruitStack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers, Minecraft.getInstance().level, this.openerId
               );
            pose.popPose();
         }
      }
   }

   private void play(SoundEvent event, float volume, float pitch) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null) {
         mc.level.playLocalSound(this.at.x, this.at.y, this.at.z, event, SoundSource.PLAYERS, volume, pitch, false);
      }
   }

   private void note(Holder<SoundEvent> event, float volume, float pitch) {
      this.play((SoundEvent)event.value(), volume, pitch);
   }

   private double spread(double a) {
      return (this.random.nextDouble() * 2.0 - 1.0) * a;
   }

   private static float smooth(float x) {
      float c = Mth.clamp(x, 0.0F, 1.0F);
      return c * c * (3.0F - 2.0F * c);
   }

   private static float ease(float x) {
      float c = Mth.clamp(x, 0.0F, 1.0F);
      return 1.0F - (1.0F - c) * (1.0F - c) * (1.0F - c);
   }

   private static float backOut(float x) {
      float c = Mth.clamp(x, 0.0F, 1.0F) - 1.0F;
      return 1.0F + c * c * (2.2F * c + 1.2F);
   }

   @Override
   public void removed() {
      CasketRevealClient.remove(this);
   }

   @Override
   public double x() {
      return this.at.x;
   }

   @Override
   public double y() {
      return this.at.y;
   }

   @Override
   public double z() {
      return this.at.z;
   }
}
