package com.devilfruits.fruits.kage_kage.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.fruits.kage_kage.KageTiming;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class SwarmCinematic extends KageCinematic {
   private final Player user;
   private final int targetId;
   private Vec3 targetPos;
   private final int n;
   private final ShadowChunk[] chunks;
   private final BatSwarm.Bat[] bats;
   private final int[] condense;
   private final int[] launch;
   private final int[] arrive;
   private final float[] angle;
   private final float[] height;
   private final Vec3[] from;
   private final Vec3[] bend1;
   private final Vec3[] bend2;
   private final Vec3[] aimedAt;
   private final int last;

   public SwarmCinematic(Player user, int targetId, Vec3 targetPos, int n, long seed) {
      super(VfxAnchor.of(user, user.position()), seed);
      this.user = user;
      this.targetId = targetId;
      this.targetPos = targetPos;
      this.n = Math.max(1, n);
      this.chunks = new ShadowChunk[this.n];
      this.bats = new BatSwarm.Bat[this.n];
      this.condense = new int[this.n];
      this.launch = new int[this.n];
      this.arrive = new int[this.n];
      this.angle = new float[this.n];
      this.height = new float[this.n];
      this.from = new Vec3[this.n];
      this.bend1 = new Vec3[this.n];
      this.bend2 = new Vec3[this.n];
      this.aimedAt = new Vec3[this.n];
      double distance = user.getEyePosition().distanceTo(targetPos);
      int latest = 0;
      AegisCinematic aegis = AegisCinematic.of(user);
      Vec3[] carried = aegis != null ? aegis.handOver() : new Vec3[0];

      for (int i = 0; i < this.n; i++) {
         this.launch[i] = 10 + Math.round((float)i * 1.2F);
         this.arrive[i] = KageTiming.batArrival(i, distance);
         this.condense[i] = 4 + i * 5 / this.n;
         this.angle[i] = (float) (Math.PI * 2) * (float)i / (float)this.n + this.random.nextFloat() * 0.5F;
         this.height[i] = 0.5F + this.random.nextFloat() * 1.2F;
         this.chunks[i] = new ShadowChunk(seed + (long)i * 17L).snap(i < carried.length ? carried[i] : user.position().add(0.0, 1.0, 0.0));
         if (i < carried.length) {
            this.chunks[i].radius = 0.18F;
         }

         latest = Math.max(latest, this.arrive[i]);
      }

      this.last = latest;
      ClientLevel level = (ClientLevel)user.level();
      KageLook.sound(level, user.position(), SoundEvents.BAT_TAKEOFF, 0.8F, 0.7F);
      KageLook.sound(level, user.position(), SoundEvents.BAT_TAKEOFF, 0.7F, 0.95F);
      KageLook.sound(level, user.position(), SoundEvents.ENDERMAN_TELEPORT, 0.35F, 0.5F);
   }

   private Vec3 target() {
      Entity e = this.user.level().getEntity(this.targetId);
      if (e != null && e.isAlive()) {
         this.targetPos = e.getBoundingBox().getCenter();
      }

      return this.targetPos;
   }

   private Vec3 orbit(int i, Vec3 centre) {
      float a = this.angle[i] + (float)this.age * 0.16F;
      float r = Mth.lerp(Mth.clamp((float)this.age / 8.0F, 0.0F, 1.0F), 0.5F, 1.3F);
      return new Vec3(
         centre.x + (double)(Mth.cos(a) * r),
         this.user.getY() + (double)this.height[i] + (double)(0.1F * Mth.sin((float)this.age * 0.3F + (float)i)),
         centre.z + (double)(Mth.sin(a) * r)
      );
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.user.isRemoved()) {
         this.finish();
      } else {
         Vec3 centre = this.user.position();
         Vec3 goal = this.target();

         for (int i = 0; i < this.n; i++) {
            ShadowChunk c = this.chunks[i];
            c.tick();
            BatSwarm.Bat b = this.bats[i];
            if (b != null) {
               b.tick();
            }

            int t = this.age;
            if (t < this.condense[i]) {
               c.pos = c.pos.lerp(this.orbit(i, centre), 0.45);
               c.radius = Math.min(0.18F, c.radius + 0.05F);
               c.heat = 1.0F;
            } else if (t < this.condense[i] + 4) {
               if (b == null) {
                  b = this.bats[i] = new BatSwarm.Bat(c.pos, this.random.nextFloat() * 6.0F);
                  b.scale = 0.0F;
                  this.flash(level, c.pos, 0.45F, 0.6F);
               }

               float k = (float)(t - this.condense[i] + 1) / 4.0F;
               c.radius = 0.18F * (1.0F - k);
               c.heat = 2.0F;
               b.scale = outBack(k);
               b.flapSpeed = 1.4F;
               c.pos = this.orbit(i, centre);
               b.moveTo(c.pos);
            } else if (t <= this.launch[i]) {
               c.radius = 0.0F;
               b.moveTo(this.orbit(i, centre));
            }

            if (b != null) {
               if (t == this.launch[i]) {
                  this.from[i] = b.pos;
                  Vec3 out = b.pos.subtract(centre).multiply(1.0, 0.0, 1.0);
                  out = out.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : out.normalize();
                  Vec3 to = goal.subtract(b.pos);
                  Vec3 side = new Vec3(-to.z, 0.0, to.x);
                  side = side.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
                  this.bend1[i] = b.pos.add(out.scale(1.6 + this.random.nextDouble())).add(0.0, 1.0 + this.random.nextDouble() * 1.2, 0.0);
                  this.bend2[i] = goal.add(side.scale(this.jit(2.6))).add(0.0, 0.6 + this.random.nextDouble() * 1.6, 0.0);
                  this.aimedAt[i] = goal;
                  b.swoopTime = 0.0F;
                  b.flapSpeed = 1.6F;
                  if (i % 3 == 0) {
                     KageLook.sound(level, b.pos, SoundEvents.BAT_TAKEOFF, 0.35F, 1.1F + this.random.nextFloat() * 0.3F);
                  }
               } else if (t > this.launch[i] && t <= this.arrive[i]) {
                  float u = (float)(t - this.launch[i]) / (float)(this.arrive[i] - this.launch[i]);
                  float s = (float)Math.pow((double)u, 1.35);
                  Vec3 shift = goal.subtract(this.aimedAt[i]);
                  Vec3 p = bezier(this.from[i], this.bend1[i], this.bend2[i].add(shift), goal, s);
                  Vec3 before = b.pos;
                  b.moveTo(p);
                  b.stretch = 1.0F + Mth.clamp((float)p.distanceTo(before) * 0.35F, 0.0F, 0.45F);
                  if (b.swoopTime > 14.0F) {
                     b.swoopTime = -1.0F;
                     b.flapSpeed = 2.0F;
                  }

                  this.flakes
                     .mote(
                        before.add(this.jit(0.05), this.jit(0.05), this.jit(0.05)),
                        new Vec3(this.jit(0.01), 0.004, this.jit(0.01)),
                        0.05F,
                        8 + this.random.nextInt(4)
                     );
                  if (t == this.arrive[i]) {
                     this.flash(level, p, 0.6F, 0.7F);
                     this.motes(level, p, 3, 0.05);

                     for (int f = 0; f < 3; f++) {
                        this.flakes.burst(p, new Vec3(this.jit(0.12), this.random.nextDouble() * 0.1, this.jit(0.12)), 0.05F, 10 + this.random.nextInt(6));
                     }

                     KageLook.sound(level, p, SoundEvents.BAT_HURT, 0.5F, 1.25F + this.random.nextFloat() * 0.3F);
                     if (i % 3 == 1) {
                        KageLook.sound(level, p, SoundEvents.AMETHYST_BLOCK_HIT, 0.6F, 0.8F);
                     }

                     b.scale = 0.0F;
                     b.alpha = 0.0F;
                  }
               }
            }
         }

         if (this.age > this.last + 1 && this.flakes.isEmpty()) {
            this.finish();
         }
      }
   }

   @Override
   protected void drawModels(boolean glow, PoseStack ps, MultiBufferSource buffers, Vec3 cam, float partial, float entityPartial) {
      for (int i = 0; i < this.n; i++) {
         ShadowChunk c = this.chunks[i];
         int light = light(c.pos);
         c.draw(glow, ps, buffers, cam, partial, light, this.rim);
         if (this.bats[i] != null) {
            BatSwarm.draw(this.bats[i], glow, ps, buffers, cam, partial, light, this.rim);
         }
      }
   }

   @Override
   public double x() {
      return this.user.getX();
   }

   @Override
   public double y() {
      return this.user.getY() + 1.0;
   }

   @Override
   public double z() {
      return this.user.getZ();
   }
}
