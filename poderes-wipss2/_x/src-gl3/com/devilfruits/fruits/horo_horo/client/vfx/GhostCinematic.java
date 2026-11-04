package com.devilfruits.fruits.horo_horo.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.ShockRing;
import com.devilfruits.client.vfx.fx.Tether;
import com.devilfruits.client.vfx.particle.Ribbon;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.fruits.horo_horo.HoroHoro;
import com.devilfruits.fruits.horo_horo.entity.GhostBody;
import com.devilfruits.fruits.horo_horo.ghost.GhostForm;
import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class GhostCinematic extends HoroCinematic {
   private final Player ghost;
   private int bodyId;
   @Nullable
   private final Ribbon trail;
   private float fade;
   private float prevFade;
   private int possessAge = -1;
   private int possessTicks = 12;
   private Vec3 bodyPos;

   public GhostCinematic(Player ghost, int bodyId, Vec3 bodyPos, long seed) {
      super(VfxAnchor.of(ghost, ghost.position()), seed);
      this.ghost = ghost;
      this.bodyId = bodyId;
      this.bodyPos = bodyPos;
      this.trail = mistRibbon(14, 0.24F, 9.0F, 13219583);
   }

   public Player ghost() {
      return this.ghost;
   }

   @Nullable
   private GhostBody body(ClientLevel level) {
      Entity e = level.getEntity(this.bodyId);
      if (e instanceof GhostBody) {
         return (GhostBody)e;
      } else {
         for (Entity ex : level.entitiesForRendering()) {
            if (ex instanceof GhostBody b && this.ghost.getUUID().equals(b.ownerId())) {
               this.bodyId = b.getId();
               return b;
            }
         }

         return null;
      }
   }

   private boolean localFirstPerson() {
      Minecraft mc = Minecraft.getInstance();
      return this.ghost == mc.player && mc.options.getCameraType().isFirstPerson();
   }

   public void onExit(ClientLevel level) {
      Vec3 c = this.bodyPos.add(0.0, 0.8, 0.0);
      flash(c, 1.1F, 16775163, 0.8F);
      this.puffs(level, HoroLook.PUFF, c, 9, 0.12, 0.9F, new int[]{16775163, 13219583, 16758741});
      this.sparkles(level, c, 9, 0.5, 0.06, new int[]{16775163, 13219583, 16758741});
      ShockRing.at(c, new Vec3(0.0, 1.0, 0.0)).radius(0.3F, 2.2F).life(10).strength(0.4F).band(0.4F).rim(0.3F, 13219583).spawn();
      if (this.ghost == Minecraft.getInstance().player) {
         CameraDirector.fovPunch(0.05F);
         CameraDirector.shake(0.2F, 0.1F, 0.8F, 2.0F);
      }
   }

   public void onPossess(Vec3 body, int ticks) {
      if (this.possessAge < 0) {
         this.possessAge = this.age;
         this.possessTicks = Math.max(1, ticks);
         this.bodyPos = body;
      }
   }

   public float dive(float partial) {
      return this.possessAge < 0 ? -1.0F : Mth.clamp(((float)this.age + partial - (float)this.possessAge) / (float)this.possessTicks, 0.0F, 1.0F);
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      this.prevFade = this.fade;
      GhostBody body = this.body(level);
      if (body != null) {
         this.bodyPos = body.position();
      }

      boolean out = !this.ghost.isRemoved() && FormFlags.has(this.ghost, HoroHoro.ID, GhostForm.OUT);
      if (this.possessAge >= 0 && this.age == this.possessAge + this.possessTicks) {
         Vec3 c = this.bodyPos.add(0.0, 0.7, 0.0);
         flash(c, 1.2F, 16758741, 0.8F);
         this.puffs(level, HoroLook.PUFF, c, 10, 0.1, 0.9F, new int[]{16758741, 16775163, 13219583});
         this.sparkles(level, c, 10, 0.5, 0.06, new int[]{16758741, 16775163, 16740264});
         ShockRing.at(c, new Vec3(0.0, 1.0, 0.0)).radius(1.8F, 0.2F).life(8).strength(0.35F).band(0.4F).rim(0.3F, 16758741).spawn();
         if (this.ghost == Minecraft.getInstance().player) {
            CameraDirector.fovPunch(-0.04F);
            CameraDirector.shake(0.25F, 0.1F, 1.0F, 2.0F);
         }
      }

      this.fade = !out || this.possessAge >= 0 && this.age >= this.possessAge + this.possessTicks
         ? Math.max(0.0F, this.fade - 0.2F)
         : Math.min(1.0F, this.fade + 0.15F);
      if (this.fade <= 0.0F && this.prevFade <= 0.0F && this.age > 3) {
         this.finish();
      } else if (!(this.fade <= 0.01F)) {
         Vec3 p = this.ghost.position();
         double speed = p.distanceTo(new Vec3(this.ghost.xo, this.ghost.yo, this.ghost.zo));
         if (!this.localFirstPerson()) {
            Vec3 chest = p.add(0.0, 1.0, 0.0);
            halo(chest, 1.9F, 13219583, 0.16F * this.fade);
            halo(chest, 1.1F, 16758741, 0.1F * this.fade);
         }

         Vec3 low = p.add(0.0, 0.35, 0.0);
         if (speed > 0.05 || this.age % 4 == 0) {
            this.emit(
               level,
               HoroLook.MIST,
               low.add(this.jit(0.25), this.jit(0.15), this.jit(0.25)),
               new Vec3(this.jit(0.01), -0.004, this.jit(0.01)),
               this.random.nextInt(3) == 0 ? 16758741 : (this.random.nextBoolean() ? 16775163 : 13219583),
               0.9F + this.random.nextFloat() * 0.5F
            );
         }

         if (this.age % 9 == 0 && !VfxSettings.legacy()) {
            this.sparkles(level, p.add(0.0, 1.0, 0.0), 1, 0.6, 0.01, new int[]{16775163, 13219583});
         }

         if (this.trail != null) {
            this.trail.push(low.x, low.y, low.z, VfxClock.time());
         }
      }
   }

   @Override
   public void render(VfxFrame f) {
      float k = Mth.lerp(f.partial, this.prevFade, this.fade);
      if (!(k <= 0.01F)) {
         Vec3 g = this.ghost.getPosition(f.entityPartial);
         if (this.trail != null && !this.localFirstPerson()) {
            this.trail.render(f, g.x, g.y + 0.35, g.z, k * 0.8F);
         }

         this.tether(f, g, k);
      }
   }

   private void tether(VfxFrame f, Vec3 g, float k) {
      if (this.ghost.level() == Minecraft.getInstance().level) {
         Vec3 from = this.bodyPos.add(0.0, 0.7, 0.0);
         Vec3 to = g.add(0.0, this.localFirstPerson() ? 0.4 : 0.85, 0.0);
         float dive = this.dive(f.partial);
         if (dive >= 0.0F) {
            to = to.lerp(from, (double)(dive * dive));
         }

         double leash = leash();
         int rgb = Tether.lerpRgb(13219583, 16740264, Tether.strain(to.distanceTo(from), leash));
         Tether.draw(f, from, to, leash, MeshTex.HORO_MIST, rgb, 1.1F, 0.05F, 0.8F * k, this.localFirstPerson() ? 3 : 0, 0.0F);
      }
   }

   static double leash() {
      try {
         return HoroHoro.setting("ghostLeash");
      } catch (IllegalStateException var1) {
         return 64.0;
      }
   }

   @Override
   public void removed() {
      super.removed();
      if (this.trail != null) {
         this.trail.release();
      }
   }

   @Override
   public void draw(PoseStack ps, MultiBufferSource buffers, Vec3 cam, float partial) {
   }

   @Override
   public double cullDistance() {
      return 160.0;
   }

   @Override
   public double x() {
      return this.ghost.getX();
   }

   @Override
   public double y() {
      return this.ghost.getY() + 1.0;
   }

   @Override
   public double z() {
      return this.ghost.getZ();
   }
}
