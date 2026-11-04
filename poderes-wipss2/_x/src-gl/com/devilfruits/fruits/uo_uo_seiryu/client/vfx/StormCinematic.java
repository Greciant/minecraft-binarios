package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.fx.ArcBurst;
import com.devilfruits.client.vfx.fx.BoltStrike;
import com.devilfruits.client.vfx.fx.GroundWave;
import com.devilfruits.client.vfx.fx.Impact;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.render.MeshTex;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class StormCinematic extends HybridCinematic {
   private final float radius;
   private final int duration;
   private double sx;
   private double sy;
   private double sz;
   private double up = 18.0;
   private int stopped = -1;

   public StormCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      this.radius = Math.max(4.0F, params.power());
      this.duration = Math.max(10, params.extra());
      this.moveTo(params.pos());
      this.up = params.pos().y - this.y;
   }

   @Nullable
   public static StormCinematic of(Entity e) {
      return VfxDirector.find(StormCinematic.class, c -> c.follows(e) && c.stopped < 0);
   }

   public void moveTo(Vec3 centre) {
      this.sx = centre.x;
      this.sy = centre.y;
      this.sz = centre.z;
   }

   public void stop() {
      this.stopped = this.age;
   }

   public static void strike(ClientLevel level, @Nullable Entity caster, Vec3 ground, Vec3 top, long seed) {
      Random r = new Random(seed);
      BoltStrike.between(top, ground, seed).width(1.0F).life(10).forks(5).rough(0.08F).heat(1.3F).endGlow(3.0F).spawn();
      BoltStrike.between(
            top.add(r.nextGaussian() * 3.0, 0.0, r.nextGaussian() * 3.0), ground.add(r.nextGaussian() * 1.5, 0.0, r.nextGaussian() * 1.5), seed ^ 81L
         )
         .width(0.32F)
         .life(7)
         .forks(3)
         .rough(0.12F)
         .endGlow(1.0F)
         .spawn();
      BoltStrike.between(
            top.add(r.nextGaussian() * 4.0, -3.0, r.nextGaussian() * 4.0), ground.add(r.nextGaussian() * 2.5, 0.0, r.nextGaussian() * 2.5), seed ^ 82L
         )
         .width(0.18F)
         .life(5)
         .forks(2)
         .rough(0.15F)
         .endGlow(0.6F)
         .spawn();
      Vec3 c = ground.add(0.0, 0.5, 0.0);
      Impact.at(level, c, seed ^ 83L)
         .caster(caster)
         .thunder()
         .flash(2.4F)
         .glow(11955455)
         .wave(6.0F, 14, 11955455)
         .dust(24)
         .sparks(56, 0.8F)
         .embers(36)
         .smoke(12, 1708080, 1.8F)
         .debris(8, 0.26F)
         .decal(4.5F)
         .trauma(0.35F, 0.08F, 2.6F)
         .lens(0.3F)
         .screenFlash(0.1F)
         .play();
      double g = VfxWorld.ground(level, ground.x, ground.y + 1.0, ground.z, 4);
      double gy = Double.isNaN(g) ? ground.y : g;
      ArcBurst.at(new Vec3(ground.x, gy + 0.2, ground.z), seed ^ 84L)
         .arcs(4)
         .radius(0.8F, 6.5F)
         .life(12)
         .spin(0.35F)
         .arc((float) (Math.PI * 4.0 / 5.0))
         .look(MeshTex.CRACKLE, 1, 0.5F, 1.35F, 2.6F)
         .tilt(0.0F, 0.05F)
         .spawn();
      GroundWave.at(ground.x, gy, ground.z, 4.0F, 10).strength(1.3F).air(gy + 2.0, 4.0F).spawn();
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.age <= this.duration + 10 && (this.stopped < 0 || this.age - this.stopped <= 4)) {
         float fade = Math.min(1.0F, (float)this.age / 10.0F);
         Entity e = this.caster(level);
         if (e != null) {
            this.sx = this.sx + (e.getX() - this.sx) * 0.08;
            this.sz = this.sz + (e.getZ() - this.sz) * 0.08;
            this.sy = e.getY() + this.up;
         }

         if (this.random.nextFloat() < 0.4F * fade) {
            double a = this.random.nextDouble() * (float) (Math.PI * 2);
            double d = this.random.nextDouble() * (double)this.radius * 0.9;
            double b = a + this.spread(1.2);
            double len = 5.0 + this.random.nextDouble() * 10.0;
            Vec3 p0 = new Vec3(this.sx + Math.cos(a) * d, this.sy - 1.0 + this.spread(1.0), this.sz + Math.sin(a) * d);
            Vec3 p1 = p0.add(Math.cos(b) * len, this.spread(1.5), Math.sin(b) * len);
            BoltStrike.between(p0, p1, this.seed + (long)this.age * 113L).width(0.22F).life(4).forks(3).rough(0.18F).endGlow(1.8F).spawn();
            VfxParticles.emit(VfxPresets.GLOW, (p0.x + p1.x) * 0.5, p0.y, (p0.z + p1.z) * 0.5, 0.0, 0.0, 0.0).size(14.0F).tint(11955455).alpha(0.55F);
         }

         for (int i = 0; i < VfxSettings.count(2.0F * fade); i++) {
            double a = this.random.nextDouble() * (float) (Math.PI * 2);
            double d = this.random.nextDouble() * (double)this.radius;
            VfxParticles.emit(VfxPresets.THUNDER_MOTE, this.sx + Math.cos(a) * d, this.sy - 2.0, this.sz + Math.sin(a) * d, 0.0, -0.08, 0.0).life(1.5F);
         }
      } else {
         this.finish();
      }
   }

   @Override
   public double x() {
      return this.sx;
   }

   @Override
   public double y() {
      return this.sy;
   }

   @Override
   public double z() {
      return this.sz;
   }

   @Override
   public double cullDistance() {
      return 192.0;
   }
}
