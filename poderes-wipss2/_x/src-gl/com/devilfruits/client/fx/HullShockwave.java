package com.devilfruits.client.fx;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.VfxDraw;
import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.fx.ShockRing;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.physics.PhysicsWorldBridge;
import com.devilfruits.registry.DFParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class HullShockwave extends Cinematic {
   private static final int LIFE = 14;
   private static final int LAG = 3;
   private static final ShockRing.Band BAND = new ShockRing.Band();
   private final ClientLevel level;
   private final Vec3 local;
   private final Vec3 localN;
   private final Vec3 localU;
   private final Vec3 localV;
   private final float radius;
   private final BlockState struck;
   private final int dust;
   private final RandomSource random;
   private Vec3 world;

   public HullShockwave(ClientLevel level, Vec3 local, Vec3 normal, float size, long seed) {
      super(new VfxAnchor(-1, local), VfxParams.NONE);
      this.level = level;
      this.local = local;
      Vector3f n = new Vector3f((float)normal.x, (float)normal.y, (float)normal.z);
      if (n.lengthSquared() < 1.0E-6F) {
         n.set(0.0F, 1.0F, 0.0F);
      }

      n.normalize();
      Vector3f u = new Vector3f();
      Vector3f v = new Vector3f();
      VfxDraw.basis(n, u, v);
      this.localN = new Vec3((double)n.x, (double)n.y, (double)n.z);
      this.localU = new Vec3((double)u.x, (double)u.y, (double)u.z);
      this.localV = new Vec3((double)v.x, (double)v.y, (double)v.z);
      this.radius = 1.0F + 1.4F * Mth.clamp(size, 0.4F, 6.0F);
      BlockPos backing = BlockPos.containing(local.subtract(this.localN.scale(0.6)));
      this.struck = level.getBlockState(backing);
      int c = this.struck.isAir() ? 9075298 : this.struck.getMapColor(level, backing).col;
      this.dust = c == 0 ? 9075298 : c;
      this.random = RandomSource.create(seed);
      Vec3 at = PhysicsWorld.bridge().renderPosition(level, local, 1.0F);
      this.world = at != null ? at : local;
      if (at == null) {
         this.finish();
      }
   }

   private float front(float t) {
      float k = Mth.clamp(t / 14.0F, 0.0F, 1.0F);
      return 0.3F + (this.radius - 0.3F) * (1.0F - (1.0F - k) * (1.0F - k));
   }

   @Override
   public void tick(ClientLevel lvl) {
      super.tick(lvl);
      if (this.age > 17) {
         this.finish();
      } else {
         PhysicsWorldBridge bridge = PhysicsWorld.bridge();
         Vec3 o = bridge.renderPosition(this.level, this.local, 1.0F);
         Vec3 n = bridge.renderDirection(this.level, this.local, this.localN, 1.0F);
         Vec3 u = bridge.renderDirection(this.level, this.local, this.localU, 1.0F);
         Vec3 v = bridge.renderDirection(this.level, this.local, this.localV, 1.0F);
         if (o != null && n != null && u != null && v != null) {
            this.world = o;
            int tint = VfxWorld.shade(this.dust, VfxWorld.light(this.level, o.x, o.y + 0.5, o.z));
            if (this.age == 1) {
               for (int i = 0; i < Fx.count(4.0F + this.radius * 1.5F); i++) {
                  double sx = (this.random.nextDouble() - 0.5) * 0.6;
                  double sy = (this.random.nextDouble() - 0.5) * 0.6;
                  Vec3 p = o.add(u.scale(sx)).add(v.scale(sy)).add(n.scale(0.15));
                  Vec3 vel = n.scale(0.15 + this.random.nextDouble() * 0.2).add(u.scale(sx * 0.3)).add(v.scale(sy * 0.3));
                  if (!this.struck.isAir()) {
                     Fx.spawn(new BlockParticleOption(DFParticles.BLOCK_DEBRIS.get(), this.struck), p.x, p.y, p.z, vel.x, vel.y, vel.z);
                  }
               }

               VfxParticles.emit(VfxPresets.DUST, o.x + n.x * 0.3, o.y + n.y * 0.3, o.z + n.z * 0.3, n.x * 0.03, n.y * 0.03, n.z * 0.03)
                  .tint(tint)
                  .size(0.9F + this.radius * 0.15F)
                  .alpha(0.75F);
            }

            if (this.age <= 14) {
               float r = this.front((float)this.age);
               float left = 1.0F - (float)this.age / 14.0F;
               int puffs = VfxSettings.count((3.0F + this.radius * 1.6F) * left);

               for (int ix = 0; ix < puffs; ix++) {
                  double a = this.random.nextDouble() * Math.PI * 2.0;
                  Vec3 out = u.scale(Math.cos(a)).add(v.scale(Math.sin(a)));
                  Vec3 p = o.add(out.scale((double)r + (this.random.nextDouble() - 0.5) * 0.3)).add(n.scale(0.12));
                  Vec3 vel = out.scale(0.04 + this.random.nextDouble() * 0.04).add(n.scale(0.02 + this.random.nextDouble() * 0.04));
                  VfxParticles.emit(VfxPresets.DUST, p.x, p.y, p.z, vel.x, vel.y, vel.z)
                     .tint(tint)
                     .size(0.45F + this.random.nextFloat() * 0.35F)
                     .alpha(0.55F * left + 0.2F);
               }
            }
         } else {
            this.finish();
         }
      }
   }

   @Override
   public void render(VfxFrame f) {
      if (f.distortion() != null) {
         PhysicsWorldBridge bridge = PhysicsWorld.bridge();
         Vec3 o = bridge.renderPosition(this.level, this.local, f.partial);
         Vec3 u = bridge.renderDirection(this.level, this.local, this.localU, f.partial);
         Vec3 v = bridge.renderDirection(this.level, this.local, this.localV, f.partial);
         Vec3 n = bridge.renderDirection(this.level, this.local, this.localN, f.partial);
         if (o != null && u != null && v != null && n != null) {
            float cx = f.rx(o.x + n.x * 0.04);
            float cy = f.ry(o.y + n.y * 0.04);
            float cz = f.rz(o.z + n.z * 0.04);
            float t = (float)this.age + f.partial;
            this.ripple(f, cx, cy, cz, u, v, t, 1.2F);
            this.ripple(f, cx, cy, cz, u, v, t - 3.0F, 0.55F);
         }
      }
   }

   private void ripple(VfxFrame f, float cx, float cy, float cz, Vec3 u, Vec3 v, float t, float strength) {
      if (!(t <= 0.0F) && !(t >= 14.0F)) {
         float k = t / 14.0F;
         float fade = (1.0F - k) * Math.min(1.0F, t / 1.5F);
         float r = this.front(t);
         BAND.draw(f, cx, cy, cz, (float)u.x, (float)u.y, (float)u.z, (float)v.x, (float)v.y, (float)v.z, r, Math.max(0.2F, r * 0.3F), strength * fade, 0.0F);
      }
   }

   @Override
   public double cullDistance() {
      return 96.0;
   }

   @Override
   public double x() {
      return this.world.x;
   }

   @Override
   public double y() {
      return this.world.y;
   }

   @Override
   public double z() {
      return this.world.z;
   }
}
