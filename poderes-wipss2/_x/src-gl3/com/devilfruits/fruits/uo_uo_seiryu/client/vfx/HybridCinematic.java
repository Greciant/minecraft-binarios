package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.fruits.uo_uo_seiryu.client.render.HybridAnchors;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public abstract class HybridCinematic extends Cinematic {
   protected final RandomSource random;
   protected final long seed;
   protected double x;
   protected double y;
   protected double z;
   protected double ox;
   protected double oy;
   protected double oz;
   protected float height = 3.6F;
   protected double groundY = Double.NaN;
   protected float light = 1.0F;
   protected final Vector3d anchor3 = new Vector3d();

   protected HybridCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(anchor, params);
      this.seed = params.seed();
      this.random = RandomSource.create(this.seed ^ 1779033703L);
      Entity e = anchor.entity(level);
      if (e != null) {
         this.follow(e);
         this.ox = this.x;
         this.oy = this.y;
         this.oz = this.z;
      } else {
         this.x = this.ox = anchor.fallback().x;
         this.y = this.oy = anchor.fallback().y;
         this.z = this.oz = anchor.fallback().z;
      }

      this.groundY = VfxWorld.ground(level, this.x, this.y + 0.5, this.z, 8);
   }

   protected void follow(Entity e) {
      this.ox = this.x;
      this.oy = this.y;
      this.oz = this.z;
      this.x = e.getX();
      this.y = e.getY();
      this.z = e.getZ();
      this.height = e.getBbHeight();
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity e = this.caster(level);
      if (e != null) {
         this.follow(e);
      } else {
         this.ox = this.x;
         this.oy = this.y;
         this.oz = this.z;
      }

      if (this.age % 4 == 1) {
         this.groundY = VfxWorld.ground(level, this.x, this.y + 0.5, this.z, 8);
         this.light = VfxWorld.light(level, this.x, this.y + 1.0, this.z);
      }
   }

   @Nullable
   protected Entity caster(ClientLevel level) {
      return this.anchor.entity(level);
   }

   @Nullable
   protected Entity caster() {
      ClientLevel level = Minecraft.getInstance().level;
      return level != null ? this.anchor.entity(level) : null;
   }

   public boolean follows(Entity e) {
      return this.anchor.entityId() == e.getId();
   }

   protected boolean local() {
      return VfxWorld.local(this.anchor.entityId());
   }

   protected boolean firstPersonCaster() {
      return VfxWorld.firstPersonCaster(this.anchor.entityId());
   }

   protected float exposure() {
      return VfxWorld.exposure(this.caster(), new Vec3(this.x, this.y + (double)this.height * 0.5, this.z), 8.0, 40.0);
   }

   protected Vector3d bodyPoint(int id, float partial) {
      Entity e = this.caster();
      return e != null
         ? HybridAnchors.get(e, id, partial, this.anchor3)
         : this.anchor3.set(this.x, this.y + (double)this.height * (id == 3 ? 1.0 : 0.55), this.z);
   }

   protected double rx(float partial) {
      return Mth.lerp((double)partial, this.ox, this.x);
   }

   protected double ry(float partial) {
      return Mth.lerp((double)partial, this.oy, this.y);
   }

   protected double rz(float partial) {
      return Mth.lerp((double)partial, this.oz, this.z);
   }

   protected boolean onFloor(double maxAbove) {
      return !Double.isNaN(this.groundY) && this.y - this.groundY < maxAbove;
   }

   protected int shade(int rgb) {
      return VfxWorld.shade(rgb, this.light);
   }

   protected double spread(double amount) {
      return (this.random.nextDouble() * 2.0 - 1.0) * amount;
   }

   @Override
   public double x() {
      return this.x;
   }

   @Override
   public double y() {
      return this.y + (double)this.height * 0.5;
   }

   @Override
   public double z() {
      return this.z;
   }
}
