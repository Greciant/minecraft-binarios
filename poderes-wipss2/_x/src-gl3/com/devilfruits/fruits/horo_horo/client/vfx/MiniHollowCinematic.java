package com.devilfruits.fruits.horo_horo.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.ShockRing;
import com.devilfruits.client.vfx.particle.Ribbon;
import com.devilfruits.fruits.horo_horo.HollowFlight;
import com.devilfruits.fruits.horo_horo.ability.MiniHollowAbility;
import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class MiniHollowCinematic extends HoroCinematic {
   private static final float UNIT = 0.027777778F;
   @Nullable
   private final Player user;
   private final int targetId;
   private final int n;
   private final HollowFlight[] flights;
   private final Vec3[] offsets;
   private final HollowGhost[] ghosts;
   private final Ribbon[] trails;
   private final boolean[] clung;
   private final boolean[] popped;
   private final int[] nextGiggle;
   private int clapAge = -1;
   private int lastPop = -1;
   private boolean anyPopped;

   public MiniHollowCinematic(@Nullable Entity user, Vec3 chest, Vec3 look, double speed, int targetId, long seed) {
      super(VfxAnchor.of(user, chest), seed);
      this.user = user instanceof Player p ? p : null;
      this.targetId = targetId;
      this.n = Math.max(1, (int)(seed & 15L));
      squareUp(user);
      this.flights = new HollowFlight[this.n];
      this.offsets = new Vec3[this.n];
      MiniHollowAbility.layout(seed, this.n, chest, look, speed, this.flights, this.offsets);
      this.ghosts = new HollowGhost[this.n];
      this.trails = new Ribbon[this.n];
      this.clung = new boolean[this.n];
      this.popped = new boolean[this.n];
      this.nextGiggle = new int[this.n];

      for (int i = 0; i < this.n; i++) {
         this.ghosts[i] = new HollowGhost(HoroModel.MINI, 0.027777778F, 4.0F, this.flights[i].pos, (float)i * 3.7F);
         this.ghosts[i].face(look);
         this.ghosts[i].play("emerge", false);
         this.trails[i] = mistRibbon(10, 0.09F, 7.0F, i % 2 == 0 ? 16775163 : 16758741);
         this.nextGiggle[i] = 20 + this.random.nextInt(20);
      }
   }

   public int targetId() {
      return this.targetId;
   }

   public boolean clapped() {
      return this.clapAge >= 0;
   }

   private boolean local() {
      return this.user != null && this.user == Minecraft.getInstance().player;
   }

   public void onClap(ClientLevel level) {
      if (this.clapAge < 0) {
         this.clapAge = this.age;
         if (this.user != null) {
            Vec3 hands = MiniHollowAbility.chest(this.user, this.user.getLookAngle());
            flash(hands, 0.7F, 16758741, 0.8F);
            this.sparkles(level, hands, 6, 0.25, 0.08, new int[]{16758741, 16775163, 16740264});
            this.puffs(level, HoroLook.PUFF, hands, 3, 0.06, 0.5F, new int[]{16775163});
            ShockRing.at(hands, this.user.getLookAngle()).radius(0.1F, 1.0F).life(6).strength(0.25F).band(0.5F).rim(0.3F, 16758741).spawn();
         }

         for (int i = 0; i < this.n; i++) {
            if (this.clung[i] && !this.popped[i]) {
               this.ghosts[i].play("pop", false);
            }
         }
      }
   }

   public void onPop(ClientLevel level, int i, Vec3 at, float power) {
      if (i >= 0 && i < this.n && !this.popped[i]) {
         this.popped[i] = true;
         this.lastPop = this.age;
         this.ghosts[i].alpha = 0.0F;
         if (power <= 0.0F) {
            this.puffs(level, HoroLook.PUFF, at, 3, 0.05, 0.6F, new int[]{16775163});
         } else {
            float k = power >= 1.0F ? 1.0F : 0.65F;
            flash(at, 1.5F * k, 16758741, 0.9F);
            flash(at, 0.7F * k, 16775163, 1.0F);
            this.puffs(level, HoroLook.PUFF_BIG, at, 5, 0.16 * (double)k, k, new int[]{16758741, 16775163, 13219583, 16740264});
            this.puffs(level, HoroLook.PUFF, at, 4, 0.22 * (double)k, 0.6F * k, new int[]{16775163, 16758741});
            this.sparkles(level, at, 10, 0.4 * (double)k, 0.12 * (double)k, new int[]{16758741, 16740264, 13219583, 16775163});
            Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            ShockRing.at(at, cam.subtract(at)).radius(0.2F, 1.7F * k).life(8).strength(0.45F * k).band(0.45F).rim(0.4F, 16740264).spawn();
            this.giggle(level, at.add(0.0, 0.2, 0.0));
            float exposure = this.user != null ? CameraDirector.exposure(this.user, at, 6.0, 24.0) : 0.0F;
            if (exposure > 0.0F) {
               CameraDirector.shake(0.25F * k * exposure, 0.12F, 1.2F, 3.0F);
            }

            if (this.local() && !this.anyPopped) {
               VfxClock.hitStop(1);
            }

            this.anyPopped = true;
         }
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity target = level.getEntity(this.targetId);
      int emerge = 5;
      boolean all = true;

      for (int i = 0; i < this.n; i++) {
         HollowGhost g = this.ghosts[i];
         g.tick();
         if (!this.popped[i]) {
            all = false;
            if (target != null && target.isAlive()) {
               Vec3 spot = MiniHollowAbility.clingPoint(target, this.offsets[i]);
               if (this.age > emerge && !this.clung[i]) {
                  if (this.flights[i].step(spot) < 0.35 || this.age - emerge > 80) {
                     this.clung[i] = true;
                     this.flights[i].pos = spot;
                     g.play(this.clapAge >= 0 ? "pop" : "cling", this.clapAge < 0);
                     this.puffs(level, HoroLook.PUFF, spot, 2, 0.04, 0.5F, new int[]{16775163, 16758741});
                  }

                  g.moveTo(this.flights[i].pos);
                  if (g.clip().equals("emerge") && g.clipTime() >= 12.0F) {
                     g.play("fly", true);
                  }
               } else if (this.clung[i]) {
                  this.flights[i].pos = spot;
                  g.moveTo(spot);
                  g.lean = 0.0F;
                  g.face(new Vec3(this.offsets[i].x, 0.0, this.offsets[i].z));
                  if (this.clapAge < 0 && this.age >= this.nextGiggle[i]) {
                     this.nextGiggle[i] = this.age + 14 + this.random.nextInt(14);
                     this.giggle(level, spot.add(this.offsets[i].x * 0.2, 0.25, this.offsets[i].z * 0.2));
                     this.sparkles(level, spot, 1, 0.2, 0.02, new int[]{16758741, 16775163});
                  }

                  if (this.clapAge >= 0) {
                     g.pos = g.pos.add(this.jit(0.03), this.jit(0.03), this.jit(0.03));
                  }
               }

               if (this.age <= emerge + 1 && this.age % 2 == 1) {
                  this.sparkles(level, this.flights[i].pos, 1, 0.15, 0.02, new int[]{16758741, 16775163});
               }

               if (this.age > 2 && !this.clung[i]) {
                  Vec3 p = g.pos.add(0.0, -0.12, 0.0);
                  if (this.trails[i] != null) {
                     this.trails[i].push(p.x, p.y, p.z, VfxClock.time());
                  }

                  if (this.age > emerge && this.age % 2 == i % 2) {
                     this.emit(level, HoroLook.MIST, p, new Vec3(this.jit(0.01), 0.006, this.jit(0.01)), i % 2 == 0 ? 16775163 : 16758741, 0.6F);
                  }
               }
            }
         }
      }

      if (this.age == 1 && this.user != null) {
         Vec3 c = MiniHollowAbility.chest(this.user, this.user.getLookAngle());
         this.puffs(level, HoroLook.PUFF, c, 4, 0.08, 0.55F, new int[]{16775163, 16758741});
         this.giggle(level, c.add(0.0, 0.3, 0.0));
      }

      if (all && this.age > this.lastPop + 30 || this.age > 1600) {
         this.finish();
      }
   }

   @Override
   public void render(VfxFrame f) {
      for (int i = 0; i < this.n; i++) {
         if (this.trails[i] != null && !this.popped[i]) {
            Vec3 p = this.ghosts[i].position(f.partial).add(0.0, -0.12, 0.0);
            this.trails[i].render(f, p.x, p.y, p.z, this.clung[i] ? 0.5F : 1.0F);
         }
      }
   }

   @Override
   public void removed() {
      super.removed();

      for (Ribbon r : this.trails) {
         if (r != null) {
            r.release();
         }
      }
   }

   @Override
   public void draw(PoseStack ps, MultiBufferSource buffers, Vec3 cam, float partial) {
      if (!this.isDone()) {
         for (int i = 0; i < this.n; i++) {
            if (!this.popped[i]) {
               this.ghosts[i].draw(ps, buffers, cam, partial);
            }
         }
      }
   }

   @Override
   public double x() {
      return this.user != null ? this.user.getX() : this.anchor.fallback().x;
   }

   @Override
   public double y() {
      return this.user != null ? this.user.getY() : this.anchor.fallback().y;
   }

   @Override
   public double z() {
      return this.user != null ? this.user.getZ() : this.anchor.fallback().z;
   }
}
