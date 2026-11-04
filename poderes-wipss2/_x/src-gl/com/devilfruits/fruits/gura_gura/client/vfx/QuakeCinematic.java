package com.devilfruits.fruits.gura_gura.client.vfx;

import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.fx.VfxWorld;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public abstract class QuakeCinematic extends Cinematic {
   public static final int CRACK = 15398655;
   public static final int GLINT = 14217983;
   protected final RandomSource random;
   protected final long seed;
   protected double x;
   protected double y;
   protected double z;
   protected float height = 1.8F;
   protected double groundY = Double.NaN;

   protected QuakeCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(anchor, params);
      this.seed = params.seed();
      this.random = RandomSource.create(this.seed ^ 1541459225L);
      Entity e = anchor.entity(level);
      if (e != null) {
         this.x = e.getX();
         this.y = e.getY();
         this.z = e.getZ();
         this.height = e.getBbHeight();
      } else {
         this.x = anchor.fallback().x;
         this.y = anchor.fallback().y;
         this.z = anchor.fallback().z;
      }

      this.groundY = VfxWorld.ground(level, this.x, this.y + 0.5, this.z, 8);
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity e = this.caster(level);
      if (e != null) {
         this.x = e.getX();
         this.y = e.getY();
         this.z = e.getZ();
         this.height = e.getBbHeight();
      }

      if (this.age % 4 == 1) {
         this.groundY = VfxWorld.ground(level, this.x, this.y + 0.5, this.z, 8);
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

   protected float exposure(Vec3 at) {
      return VfxWorld.exposure(this.caster(), at, 8.0, 40.0);
   }

   public static int dust(Level level, double px, double py, double pz) {
      int c = VfxWorld.groundColour(level, px, py, pz);
      int r = ((c >> 16 & 0xFF) + 308) / 3;
      int g = ((c >> 8 & 0xFF) + 296) / 3;
      int b = ((c & 0xFF) + 280) / 3;
      return VfxWorld.shade(r << 16 | g << 8 | b, VfxWorld.light(level, px, py + 1.0, pz));
   }

   protected double spread(double amount) {
      return (this.random.nextDouble() * 2.0 - 1.0) * amount;
   }

   public static Vec3 fist(Entity e, float partial, float side) {
      Minecraft mc = Minecraft.getInstance();
      Camera cam = mc.gameRenderer.getMainCamera();
      if (e == mc.player && !cam.isDetached()) {
         Vector3f look = cam.getLookVector();
         Vector3f left = cam.getLeftVector();
         Vector3f up = cam.getUpVector();
         Vec3 c = cam.getPosition();
         return new Vec3(
            c.x + (double)look.x * 1.15 - (double)left.x * 0.5 * (double)side - (double)up.x * 0.38,
            c.y + (double)look.y * 1.15 - (double)left.y * 0.5 * (double)side - (double)up.y * 0.38,
            c.z + (double)look.z * 1.15 - (double)left.z * 0.5 * (double)side - (double)up.z * 0.38
         );
      } else {
         float yaw = (e instanceof LivingEntity l ? Mth.rotLerp(partial, l.yBodyRotO, l.yBodyRot) : e.getYRot()) * (float) (Math.PI / 180.0);
         double fx = (double)(-Mth.sin(yaw));
         double fz = (double)Mth.cos(yaw);
         double rx = (double)(-Mth.cos(yaw));
         double rz = (double)(-Mth.sin(yaw));
         double scale = (double)e.getBbHeight() / 1.8;
         double px = Mth.lerp((double)partial, e.xo, e.getX());
         double py = Mth.lerp((double)partial, e.yo, e.getY());
         double pz = Mth.lerp((double)partial, e.zo, e.getZ());
         return new Vec3(
            px + (rx * 0.45 * (double)side + fx * 0.15) * scale,
            py + (e.isCrouching() ? 1.0 : 1.3) * scale,
            pz + (rz * 0.45 * (double)side + fz * 0.15) * scale
         );
      }
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
