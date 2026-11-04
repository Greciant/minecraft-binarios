package com.devilfruits.fruits.yomi_yomi.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.ArcStrip;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.render.MeshTex;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class GavotteCinematic extends YomiCinematic {
   private static final int LEAP_TICKS = 12;
   private static final int SWEEP_TICKS = 9;
   private static final int MAX_FALL = 40;
   private final boolean sweep;
   private final float radius;
   private final float startYaw;
   @Nullable
   private SoulFlames.Web web;
   private int landed = -1;
   private Vec3 landAt = Vec3.ZERO;

   private GavotteCinematic(Entity user, Vec3 at, boolean sweep, float radius, long seed) {
      super(VfxAnchor.of(user, at), at, seed);
      this.sweep = sweep;
      this.radius = radius;
      this.startYaw = user.getYRot() * (float) (Math.PI / 180.0);
   }

   public static GavotteCinematic leap(Entity user, Vec3 at, long seed) {
      GavotteCinematic c = new GavotteCinematic(user, at, false, 0.0F, seed);
      SoulFlames.ring(c.random, at, 0.7, 7, 0.55F);
      SoulFlames.burst(c.random, at.add(0.0, 0.3, 0.0), 0.8F);
      Frost.chips(c.random, at.add(0.0, 0.1, 0.0), Vec3.ZERO, 5, 0.15);
      return c;
   }

   public static GavotteCinematic sweep(Entity user, Vec3 center, float radius, long seed) {
      GavotteCinematic c = new GavotteCinematic(user, center, true, radius, seed);
      if (VfxWorld.local(user.getId())) {
         CameraDirector.shake(0.2F, 0.07F, 1.4F, 12.0F);
      }

      return c;
   }

   @Override
   protected int length() {
      return this.sweep ? (this.landed >= 0 ? this.landed + 30 : 70) : 12;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity user = this.anchorEntity();
      if (user != null) {
         Vec3 body = user.position().add(0.0, (double)user.getBbHeight() * 0.5, 0.0);
         if (!this.sweep) {
            if (this.age <= 12) {
               SoulFlames.wispTrail(this.random, body, body.subtract(user.getDeltaMovement()), 4.0F, 0.9F);
               if (this.age % 2 == 0) {
                  Afterimages.add(user, 8, 8191802);
               }
            }
         } else {
            if (this.age <= 4) {
               for (int i = 0; i < 4; i++) {
                  double ang = (double)this.startYaw + (double)this.age / 3.0 * Math.PI * 2.0 + (double)i * 0.5;
                  Vec3 edge = body.add(Math.cos(ang) * (double)this.radius * 0.9, 0.0, Math.sin(ang) * (double)this.radius * 0.9);
                  Vec3 tan = new Vec3(-Math.sin(ang), 0.0, Math.cos(ang));
                  VfxParticles.emit(YomiLook.STREAK, edge.x, edge.y, edge.z, tan.x * 0.35, 0.03, tan.z * 0.35);
                  Frost.filings(this.random, edge, tan, 1);
               }
            }

            if (this.landed < 0 && this.age > 2 && (user.onGround() || this.age > 40)) {
               this.landed = this.age;
               this.landAt = user.position();
               SoulFlames.ring(this.random, this.landAt, 1.4, 12, 0.65F);
               SoulFlames.tongues(this.random, this.landAt, 0.8, 6, 0.7F);
               this.creeps.add(Frost.Creep.disc(level, this.random, this.landAt, 2.6, 0.35F));
               Frost.snow(this.random, this.landAt.add(0.0, 0.6, 0.0), 1.6, 14);
               Frost.mist(this.random, this.landAt, 1.0, 4);
               this.web = new SoulFlames.Web(this.random.nextLong(), this.landAt, 2.8, 6, 6.0F, 30.0F);
               if (Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(this.landAt) < 100.0) {
                  CameraDirector.shake(0.12F, 0.06F, 1.0F, 10.0F);
               }
            }
         }
      }
   }

   @Override
   public void render(VfxFrame f) {
      if (this.sweep) {
         float t = (float)this.age + f.partial;
         Entity user = this.anchorEntity();
         if (t <= 9.0F && user != null) {
            Vec3 c = user.getPosition(f.entityPartial).add(0.0, (double)user.getBbHeight() * 0.45, 0.0);
            float carved = Math.min(1.0F, t / 3.0F);
            float fade = Mth.clamp(1.0F - (t - 3.0F) / 6.0F, 0.0F, 1.0F);
            float head = this.startYaw + carved * (float) (Math.PI * 2);
            float arc = (float) (Math.PI * 2) * Math.min(1.0F, carved * 1.05F);
            ArcStrip.draw(
               f,
               MeshTex.CUT_RIBBON,
               f.rx(c.x),
               f.ry(c.y),
               f.rz(c.z),
               1.0F,
               0.0F,
               0.0F,
               0.0F,
               0.0F,
               1.0F,
               this.radius * 0.92F,
               head,
               arc,
               0.42F,
               0.85F + 0.2F * fade,
               fade,
               6,
               2.2F,
               16777215,
               -t * 0.5F,
               40
            );
            ArcStrip.draw(
               f,
               MeshTex.CUT_RIBBON,
               f.rx(c.x),
               f.ry(c.y),
               f.rz(c.z),
               1.0F,
               0.0F,
               0.0F,
               0.0F,
               0.0F,
               1.0F,
               this.radius * 0.92F,
               head,
               arc,
               0.14F,
               1.1F,
               fade,
               7,
               2.6F,
               16777215,
               -t * 0.7F,
               40
            );
         }

         if (this.web != null && this.landed >= 0) {
            this.web.render(f, t - (float)this.landed, 0.16F);
         }
      }
   }
}
