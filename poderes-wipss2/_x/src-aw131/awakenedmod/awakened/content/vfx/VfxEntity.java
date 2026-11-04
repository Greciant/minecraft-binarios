package com.awakenedmod.awakened.content.vfx;

import com.awakenedmod.awakened.content.AwakenedParticles;
import com.awakenedmod.awakened.content.particle.NukeFxOptions;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class VfxEntity extends AbstractFxEntity {
   public static final int WEDGE_FEED = 2;
   public static final int WEDGE_LIFE = 4;
   public static final int RUPTURE_LINGER = 14;
   private static final int RUPTURE_LIGHT_PACK = 100;
   private static final float RUPTURE_LIGHT_STEPS = 20.0F;
   public static final int AIR_TORUS_POP = 1;
   public static final int AIR_TORUS_HOLD = 6;
   public static final int AIR_TORUS_FADE = 6;
   private static final int AIR_TORUS_CLOCK_PACK = 1000;
   public static final float PORTAL_ELLIPSE = 1.35F;
   public static final int PORTAL_OPEN_TICKS = 8;
   public static final int PORTAL_CLOSE_TICKS = 10;
   public static final int NO_SECOND_COLOR = -1;
   private VfxEntity.Kind kind = VfxEntity.Kind.SPHERE;
   private float scale = 1.0F;
   private float secX;
   private float secY;
   private float secZ;
   private float intensity = 1.0F;
   private int color = 16762170;
   private int color2 = -1;
   private float yaw;
   private float pitch;
   private int ownerId = -1;
   private float ownerDim = 1.0F;
   private boolean noDepth;
   private int delay;
   private Object drawerMemo;
   private static final int PORTAL_DRONE_CADENCE = 140;
   private static final double RENDER_DISTANCE = 128.0;

   public static float packRuptureLight(double halfNear, double glow) {
      float half = (float)Math.max(0.0, Math.min(99.0, halfNear));
      return (float)(Math.round(Math.max(0.0, glow) * 20.0) * 100L) + half;
   }

   public static float ruptureHalfNear(double packed) {
      return (float)(packed - Math.floor(packed / 100.0) * 100.0);
   }

   public static float ruptureGlow(double packed) {
      return (float)Math.floor(packed / 100.0) / 20.0F;
   }

   public static float packAirTorusClock(int pop, int hold) {
      return (float)(Math.max(1, pop) * 1000 + Math.max(0, Math.min(999, hold)));
   }

   public static int airTorusPop(double packed) {
      return (int)Math.round(packed) / 1000;
   }

   public static int airTorusHold(double packed) {
      return (int)Math.round(packed) % 1000;
   }

   public static float portalOpenWindow(float span) {
      return Math.min(span * 0.15F, 8.0F);
   }

   public static float portalCloseWindow(float span) {
      return Math.min(span * 0.15F, 10.0F);
   }

   public static float portalOpen(float age, float span) {
      if (span <= 0.0F) {
         return 1.0F;
      } else {
         float f = Math.min(age / Math.max(1.0F, portalOpenWindow(span)), (span - age) / Math.max(1.0F, portalCloseWindow(span)));
         return Mth.clamp(f, 0.0F, 1.0F);
      }
   }

   public VfxEntity(EntityType<? extends VfxEntity> type, Level level) {
      super(type, level);
   }

   public <T> T drawerMemo(Class<T> type, Supplier<T> init) {
      if (!type.isInstance(this.drawerMemo)) {
         this.drawerMemo = init.get();
      }

      return type.cast(this.drawerMemo);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.kind == VfxEntity.Kind.PORTAL && this.level().isClientSide()) {
         this.portalClientEffects();
      }
   }

   private void portalClientEffects() {
      float age = (float)(this.tickCount - this.delay);
      float span = (float)(this.lifetime - this.delay);
      if (!(age < 0.0F) && !(span <= 0.0F)) {
         if (this.tickCount == this.delay) {
            this.portalOpenExhale();
         }

         int collapseTick = this.delay + Math.max(1, Math.round(span - portalCloseWindow(span)));
         if (this.tickCount == collapseTick) {
            this.portalCollapseImplosion();
         }

         if (age < span - portalCloseWindow(span)) {
            this.emitPortalMist(age, span);
            if ((this.tickCount - this.delay) % 140 == 0) {
               this.level()
                  .playLocalSound(
                     this.getX(),
                     this.getY(),
                     this.getZ(),
                     SoundEvents.PORTAL_AMBIENT,
                     SoundSource.PLAYERS,
                     Math.min(0.9F, 0.3F + 0.15F * this.scale),
                     0.55F + this.random.nextFloat() * 0.2F,
                     false
                  );
            }
         }
      }
   }

   private void emitPortalMist(float age, float span) {
      float open = portalOpen(age, span);
      float budget = (0.5F + this.scale * 0.8F) * open;
      int count = (int)budget + (this.random.nextFloat() < budget - (float)((int)budget) ? 1 : 0);

      for (int i = 0; i < count; i++) {
         Vec3 offset = this.portalRimOffset((double)open * (0.78 + 0.3 * this.random.nextDouble()));
         double dist = offset.length();
         double rest = (double)(this.scale * open) * 0.72;
         if (!(dist < 0.001) && !(dist <= rest + 0.05)) {
            double px = this.getX() + offset.x;
            double py = this.getY() + offset.y;
            double pz = this.getZ() + offset.z;
            double speed = Mth.clamp((dist - rest) * 0.1, 0.03, 0.16);
            Vec3 pull = offset.scale(-speed / dist);
            float mistSize = Math.min(1.3F, 0.4F + 0.11F * this.scale);
            this.level().addParticle(new NukeFxOptions(AwakenedParticles.WARP_MIST.get(), mistSize), px, py, pz, pull.x, pull.y, pull.z);
            if (this.random.nextInt(3) == 0) {
               this.level().addParticle(ParticleTypes.PORTAL, this.getX(), this.getY(), this.getZ(), offset.x, offset.y, offset.z);
            }

            if (this.random.nextInt(12) == 0) {
               this.level().addParticle(ParticleTypes.DRAGON_BREATH, px, py, pz, pull.x * 0.5, pull.y * 0.5, pull.z * 0.5);
            }
         }
      }
   }

   private void portalOpenExhale() {
      int n = 8 + (int)(this.scale * 4.0F);
      float mistSize = Math.min(1.8F, 0.55F + 0.14F * this.scale);
      double speed = Math.min(0.3, (double)this.scale * 0.1);

      for (int i = 0; i < n; i++) {
         Vec3 dir = this.portalRimOffset(1.0);
         double dist = dir.length();
         if (!(dist < 0.001)) {
            Vec3 vel = dir.scale(speed / dist);
            this.level()
               .addParticle(
                  new NukeFxOptions(AwakenedParticles.WARP_MIST.get(), mistSize),
                  this.getX() + dir.x * 0.15,
                  this.getY() + dir.y * 0.15,
                  this.getZ() + dir.z * 0.15,
                  vel.x,
                  vel.y,
                  vel.z
               );
         }
      }
   }

   private void portalCollapseImplosion() {
      int n = 10 + (int)(this.scale * 4.0F);
      float mistSize = Math.min(1.6F, 0.45F + 0.12F * this.scale);

      for (int i = 0; i < n; i++) {
         Vec3 offset = this.portalRimOffset(1.15);
         double dist = offset.length();
         if (!(dist < 0.001)) {
            double speed = Mth.clamp(dist * 0.1, 0.08, 0.3);
            Vec3 pull = offset.scale(-speed / dist);
            this.level()
               .addParticle(
                  new NukeFxOptions(AwakenedParticles.WARP_MIST.get(), mistSize),
                  this.getX() + offset.x,
                  this.getY() + offset.y,
                  this.getZ() + offset.z,
                  pull.x,
                  pull.y,
                  pull.z
               );
            if (i % 2 == 0) {
               this.level().addParticle(ParticleTypes.PORTAL, this.getX(), this.getY(), this.getZ(), offset.x, offset.y, offset.z);
            }
         }
      }

      this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.AMBIENT, 0.9F, 0.4F, false);
   }

   private Vec3 portalRimOffset(double radiusFrac) {
      float ellipse = this.secX > 0.0F ? this.secX : 1.35F;
      double a = this.random.nextDouble() * Math.PI * 2.0;
      double r = (double)this.scale * radiusFrac;
      Vec3 local = new Vec3(Math.cos(a) * r, Math.sin(a) * r * (double)ellipse, (this.random.nextDouble() - 0.5) * 0.08);
      float pitchRad = this.pitch * (float) (Math.PI / 180.0);
      float yawRad = this.yaw * (float) (Math.PI / 180.0);
      return local.xRot(-pitchRad).yRot(yawRad);
   }

   public void configure(VfxEntity.Kind kind, float scale, Vec3 secondary, int lifetime, float intensity, int color, int color2, float yaw, float pitch) {
      this.kind = kind;
      this.scale = scale;
      this.secX = (float)secondary.x;
      this.secY = (float)secondary.y;
      this.secZ = (float)secondary.z;
      this.lifetime = lifetime;
      this.intensity = intensity;
      this.color = color;
      this.color2 = color2;
      this.yaw = yaw;
      this.pitch = pitch;
   }

   public boolean shouldRenderAtSqrDistance(double distance) {
      double d = 128.0 * getViewScale();
      return distance < d * d;
   }

   public VfxEntity.Kind kind() {
      return this.kind;
   }

   public float scale() {
      return this.scale;
   }

   public Vec3 secondary() {
      return new Vec3((double)this.secX, (double)this.secY, (double)this.secZ);
   }

   public static double idToSecondary(int entityId) {
      return (double)Float.intBitsToFloat(entityId);
   }

   public static int idFromSecondary(double slot) {
      return Float.floatToRawIntBits((float)slot);
   }

   public int color() {
      return this.color;
   }

   public int color2() {
      return this.color2;
   }

   public float intensity() {
      return this.intensity;
   }

   public float yaw() {
      return this.yaw;
   }

   public float pitch() {
      return this.pitch;
   }

   public void setOwnerId(int ownerId) {
      this.ownerId = ownerId;
   }

   public int ownerId() {
      return this.ownerId;
   }

   public void setOwnerDim(float ownerDim) {
      this.ownerDim = Mth.clamp(ownerDim, 0.0F, 1.0F);
   }

   public float ownerDim() {
      return this.ownerDim;
   }

   public void setNoDepth(boolean noDepth) {
      this.noDepth = noDepth;
   }

   public boolean noDepth() {
      return this.noDepth;
   }

   public void setDelay(int delay) {
      this.delay = delay;
   }

   public int delay() {
      return this.delay;
   }

   @Override
   protected void writeExtraSpawnData(RegistryFriendlyByteBuf buf) {
      buf.writeVarInt(this.kind.ordinal());
      buf.writeFloat(this.scale);
      buf.writeFloat(this.secX);
      buf.writeFloat(this.secY);
      buf.writeFloat(this.secZ);
      buf.writeFloat(this.intensity);
      buf.writeInt(this.color);
      buf.writeInt(this.color2);
      buf.writeFloat(this.yaw);
      buf.writeFloat(this.pitch);
      buf.writeInt(this.ownerId);
      buf.writeBoolean(this.noDepth);
      buf.writeVarInt(this.delay);
      buf.writeFloat(this.ownerDim);
   }

   @Override
   protected void readExtraSpawnData(RegistryFriendlyByteBuf buf) {
      this.kind = VfxEntity.Kind.values()[buf.readVarInt()];
      this.scale = buf.readFloat();
      this.secX = buf.readFloat();
      this.secY = buf.readFloat();
      this.secZ = buf.readFloat();
      this.intensity = buf.readFloat();
      this.color = buf.readInt();
      this.color2 = buf.readInt();
      this.yaw = buf.readFloat();
      this.pitch = buf.readFloat();
      this.ownerId = buf.readInt();
      this.noDepth = buf.readBoolean();
      this.delay = buf.readVarInt();
      this.ownerDim = buf.readFloat();
   }

   public static enum Kind {
      SPHERE,
      RING,
      BEAM,
      DOME,
      PILLAR,
      ARC,
      CRESCENT,
      MUSHROOM,
      DUST_DOME,
      SPLAT,
      PORTAL,
      HELIX,
      HALO,
      TORNADO,
      AIR_WALL,
      VELOCITY_SONIC_CRACK,
      VELOCITY_GEAR_BURST,
      VELOCITY_BOLT,
      VELOCITY_TIME_FIELD,
      VELOCITY_TUNNEL_MAW,
      VELOCITY_TUNNEL_BURST,
      VELOCITY_BOLT_IMPACT,
      VELOCITY_SONIC_BOOM,
      VELOCITY_TIME_RING,
      VELOCITY_WORMHOLE,
      FIRE_VOXEL_SPRITE,
      FIRE_VOXEL_INRUSH,
      KINETIC_VOXEL_SPRITE,
      KINETIC_VOXEL_INRUSH,
      KINETIC_CUBE_FRONT,
      KINETIC_PIXEL_ARC,
      KINETIC_WAKE,
      KINETIC_AIR_PUNCH,
      KINETIC_AIR_RING,
      KINETIC_AIR_RAYS,
      KINETIC_AIR_TORUS,
      KINETIC_RUPTURE,
      KINETIC_WEDGE,
      SCORCH,
      PIT,
      RUIN_VEINS,
      RUIN_GOUGE,
      RUIN_GRIP_CRACK,
      RUIN_FRACTURE,
      RUIN_COLLAPSE_FRONT,
      RUIN_DUST_TOWER,
      RUIN_CRACK_FLASH,
      RUIN_PIT_HAZE,
      WATER_SPLASH,
      WATER_RING,
      WATER_PUDDLE,
      WATER_STREAM,
      WATER_GEYSER,
      WATER_TENDRIL,
      WATER_WAVE;
   }
}
