package com.devilfruits.fruits.uo_uo_seiryu.client.vfx;

import com.devilfruits.client.model.geo.GeoAnimation;
import com.devilfruits.client.model.geo.GeoResources;
import com.devilfruits.client.render.vfx.FloorDebrisEffect;
import com.devilfruits.client.render.vfx.VfxManager;
import com.devilfruits.client.sound.SoundDesign;
import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.VfxSound;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.particle.Ribbon;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.post.VfxPost;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuSounds;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import com.devilfruits.fruits.uo_uo_seiryu.client.render.HybridAnchors;
import com.devilfruits.registry.DFSounds;
import com.mojang.blaze3d.vertex.BufferBuilder;
import javax.annotation.Nullable;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3d;

public class HellfireRayCinematic extends Cinematic {
   public static final int WHITE = 16774338;
   public static final int YELLOW = 16765503;
   public static final int ORANGE = 16742938;
   public static final int RED = 15217182;
   private static final int WARM_GRADE = 16769724;
   private static final int RINGS = 3;
   private static final int RING_POINTS = 44;
   private static final float[] RING_SCALE = new float[]{0.74F, 1.0F, 0.87F};
   private static final float[] RING_TILT = new float[]{0.42F, 0.95F, 1.35F};
   private static final float[] RING_SPEED = new float[]{0.62F, -0.74F, 0.86F};
   private static final float[] RING_PRECESS = new float[]{0.045F, -0.06F, 0.05F};
   private static final float RING_ARC = 5.0893803F;
   private static final int AFTER_TICKS = 40;
   private static final int TAG_HUM = 19985;
   private static final int TAG_HIT = 19986;
   private static final int[] FLARING = new int[]{0, 1, 2};
   private HellfireRayCinematic.Phase phase = HellfireRayCinematic.Phase.WINDUP;
   private int phaseAge;
   private final int windupTicks;
   private final int explodeAt;
   private int rayTicks = 70;
   private final float ringRadius;
   private final long seed;
   private final RandomSource random;
   private double x;
   private double y;
   private double z;
   private double ox;
   private double oy;
   private double oz;
   private float height = 3.6F;
   private double groundY = Double.NaN;
   private float facingX;
   private float facingZ;
   private float lx;
   private float ly = -0.7F;
   private float lz = 0.7F;
   private float olx;
   private float oly = -0.7F;
   private float olz = 0.7F;
   private float lanceLength = 4.0F;
   private float oLanceLength = 4.0F;
   private int aimId = -1;
   private int lastHitTick = -100;
   private float intensity = 0.6F;
   private int scorchTimer;
   private float light = 1.0F;
   private int releaseWave = -1;
   private int blastWave = -1;
   private double waveX;
   private double waveY;
   private double waveZ;
   private double blastX;
   private double blastY;
   private double blastZ;
   @Nullable
   private Ribbon tail;
   @Nullable
   private VfxSound swell;
   @Nullable
   private VfxSound loop;
   private final Vector3d anchor3 = new Vector3d();
   private final MutableBlockPos pos = new MutableBlockPos();
   private final float[] sx = new float[44];
   private final float[] sy = new float[44];
   private final float[] sz = new float[44];
   private final float[] sw = new float[44];
   private final float[] sh = new float[44];
   private final float[] sa = new float[44];

   public HellfireRayCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(anchor, params);
      this.ringRadius = Math.max(0.5F, params.power());
      this.seed = params.seed();
      this.random = RandomSource.create(this.seed);
      GeoAnimation windup = GeoResources.animation(UoUoSeiryu.anim("hybrid/hellfire_ray_windup"));
      GeoAnimation end = GeoResources.animation(UoUoSeiryu.anim("hybrid/hellfire_ray_end"));
      this.windupTicks = Math.max(1, Math.round(windup != null ? windup.marker("release", (float)params.extra()) : (float)params.extra()));
      this.explodeAt = Math.max(1, Math.round(end != null ? end.marker("explode", 4.0F) : 4.0F));
      Vec3 d = params.dir();
      double dl = Math.sqrt(d.x * d.x + d.z * d.z);
      this.facingX = dl > 1.0E-4 ? (float)(d.x / dl) : 0.0F;
      this.facingZ = dl > 1.0E-4 ? (float)(d.z / dl) : 1.0F;
      Entity e = anchor.entity(level);
      if (e != null) {
         this.follow(e);
      } else {
         this.x = this.ox = params.pos().x;
         this.y = this.oy = params.pos().y;
         this.z = this.oz = params.pos().z;
      }

      this.tail = Ribbon.create(24);
      if (this.tail != null) {
         this.tail.look(0.38F, 7.0F, 1.05F, 0.45F, 1.1F, 0).texture(MeshTex.FLAME_RIBBON, 0.1F);
      }

      if (e != null) {
         this.swell = VfxSound.play(
            SeiryuSounds.HELLFIRE_SWELL.get(),
            e,
            e.position(),
            false,
            a -> 0.25F + 0.75F * Math.min(1.0F, (float)a / (float)this.windupTicks),
            a -> 0.75F + 0.35F * Math.min(1.0F, (float)a / (float)this.windupTicks),
            this.windupTicks + 6,
            () -> !this.isDone()
         );
      }
   }

   public boolean follows(Entity e) {
      return this.anchor.entityId() == e.getId();
   }

   public boolean near(Vec3 at, double r) {
      double dx = at.x - this.x;
      double dy = at.y - (this.y + (double)this.height * 0.5);
      double dz = at.z - this.z;
      return dx * dx + dy * dy + dz * dz <= r * r;
   }

   private boolean local() {
      Minecraft mc = Minecraft.getInstance();
      return mc.player != null && mc.player.getId() == this.anchor.entityId();
   }

   private boolean firstPersonCaster() {
      return this.local() && Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON;
   }

   private float exposure() {
      Minecraft mc = Minecraft.getInstance();
      if (this.local()) {
         return 1.0F;
      } else if (mc.level == null) {
         return 0.0F;
      } else {
         Entity e = this.anchor.entity(mc.level);
         return e == null ? 0.0F : CameraDirector.exposure(e, new Vec3(this.x, this.y + (double)this.height * 0.5, this.z), 8.0, 40.0);
      }
   }

   private void follow(Entity e) {
      this.ox = this.x;
      this.oy = this.y;
      this.oz = this.z;
      this.x = e.getX();
      this.y = e.getY();
      this.z = e.getZ();
      this.height = e.getBbHeight();
   }

   public void release(ClientLevel level, int ticks, Vec3 dir) {
      if (this.phase == HellfireRayCinematic.Phase.WINDUP) {
         double dl = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
         if (dl > 1.0E-4) {
            this.facingX = (float)(dir.x / dl);
            this.facingZ = (float)(dir.z / dl);
         }

         this.rayTicks = Math.max(1, ticks);
         this.phase = HellfireRayCinematic.Phase.RAY;
         this.phaseAge = 0;
         if (this.swell != null) {
            this.swell.end();
         }

         Entity e = this.anchor.entity(level);
         double cy = this.y + (double)this.height * 0.5;
         Vec3 center = new Vec3(this.x, cy, this.z);
         VfxParticles.emit(VfxPresets.FLARE, this.x, cy, this.z, 0.0, 0.0, 0.0).size(0.95F).heat(1.05F);
         VfxParticles.emit(VfxPresets.FLARE, this.x, cy, this.z, 0.0, 0.0, 0.0).size(0.6F).rotation(this.random.nextFloat() * 6.28F).heat(1.2F);
         VfxParticles.emit(VfxPresets.GLOW, this.x, cy, this.z, 0.0, 0.0, 0.0).size(this.ringRadius * 2.6F).tint(16765503).alpha(0.7F).life(1.4F);

         for (int i = 0; i < VfxSettings.count(18.0F); i++) {
            double a = this.random.nextDouble() * (float) (Math.PI * 2);
            double up = this.random.nextDouble() * 0.6 - 0.2;
            VfxParticles.emit(VfxPresets.FIRE_TONGUE, this.x, cy, this.z, Math.cos(a) * 0.45, up * 0.4, Math.sin(a) * 0.45).size(1.3F);
         }

         double gy = this.ground(level, this.x, this.y + 0.5, this.z);
         if (!Double.isNaN(gy) && this.y - gy < 4.0) {
            this.waveX = this.x;
            this.waveY = gy + 0.04;
            this.waveZ = this.z;
            this.releaseWave = 0;
            VfxParticles.emit(VfxPresets.RING_WAVE, this.x, gy + 0.05, this.z, 0.0, 0.0, 0.0).size(this.ringRadius * 0.8F).tint(16742938).alpha(0.8F);
            int dust = this.shade(this.groundColour(level, this.x, gy - 0.5, this.z));

            for (int i = 0; i < VfxSettings.count(16.0F); i++) {
               double a = (double)((float) (Math.PI * 2) * (float)i / 16.0F) + this.random.nextDouble() * 0.3;
               double r = 0.8 + this.random.nextDouble() * 0.6;
               VfxParticles.emit(
                     VfxPresets.DUST,
                     this.x + Math.cos(a) * r,
                     gy + 0.3,
                     this.z + Math.sin(a) * r,
                     Math.cos(a) * 0.32,
                     0.02 + this.random.nextDouble() * 0.04,
                     Math.sin(a) * 0.32
                  )
                  .tint(dust);
            }

            if (!this.firstPersonCaster()) {
               VfxManager.add(new FloorDebrisEffect(level, new Vec3(this.x, gy, this.z), 10, 0.2F, 0.45F, 50, this.seed ^ 94L));
            }
         }

         float ex = this.exposure();
         if (ex > 0.0F) {
            float lens = this.firstPersonCaster() ? 0.35F : 1.0F;
            CameraDirector.snapDolly();
            CameraDirector.shake(0.6F * ex, 0.06F, 3.2F, 2.6F);
            CameraDirector.fovPunch(0.07F * ex);
            VfxPost.flash(16765503, 0.22F * ex * lens);
            VfxPost.chromatic(0.8F * ex * lens);
            VfxPost.radialBlur(center, 0.12F * ex * lens);
         }

         SoundDesign.play(
            center,
            SeiryuSounds.HELLFIRE_RELEASE,
            1.6F,
            1.0F,
            SoundDesign.layer(SeiryuSounds.HELLFIRE, 1.0F, 1.0F),
            SoundDesign.layer(DFSounds.LAYER_BOOM, 0.6F, 1.3F)
         );
         if (e != null) {
            this.loop = VfxSound.play(
               SeiryuSounds.HELLFIRE_LOOP.get(),
               e,
               e.position(),
               true,
               ax -> Math.min(1.0F, (float)ax / 4.0F) * (0.75F + 0.35F * this.intensity),
               ax -> 0.75F + 0.35F * this.intensity,
               this.rayTicks + 40,
               () -> this.phase == HellfireRayCinematic.Phase.RAY && !this.isDone()
            );
         }
      }
   }

   public void hit(ClientLevel level, @Nullable Entity target, Vec3 at, int count) {
      this.intensity = Math.min(1.0F, this.intensity + 0.25F);
      this.lastHitTick = this.age;
      if (target != null) {
         this.aimId = target.getId();
      }

      boolean flashes = VfxSettings.hitFlashes();
      VfxParticles.emit(VfxPresets.GLOW, at.x, at.y, at.z, 0.0, 0.0, 0.0).size(flashes ? 2.6F : 1.6F).tint(16742938).alpha(flashes ? 0.9F : 0.5F);
      if (flashes) {
         VfxParticles.emit(VfxPresets.FLARE, at.x, at.y, at.z, 0.0, 0.0, 0.0).size(0.28F).life(0.6F).rotation(this.random.nextFloat() * 6.28F);
      }

      for (int i = 0; i < VfxSettings.count((float)(3 + count)); i++) {
         VfxParticles.emit(VfxPresets.SPARK, at.x, at.y, at.z, this.spread(0.35), 0.1 + this.random.nextDouble() * 0.25, this.spread(0.35));
      }

      VfxParticles.emit(VfxPresets.FIRE_TONGUE, at.x + this.spread(0.3), at.y + this.spread(0.4), at.z + this.spread(0.3), 0.0, 0.04, 0.0);
      Minecraft mc = Minecraft.getInstance();
      boolean involved = this.local() || mc.player != null && target == mc.player;
      if (involved) {
         CameraDirector.shake(19986, 0.32F, 0.1F, 0.9F, 4.2F);
      }

      if (this.age % 3 == 0) {
         SoundDesign.layerOnly(at, SeiryuSounds.HELLFIRE_IMPACT, 0.7F, 0.9F + this.random.nextFloat() * 0.3F);
      }
   }

   public void end(ClientLevel level) {
      if (this.phase != HellfireRayCinematic.Phase.COLLAPSE && this.phase != HellfireRayCinematic.Phase.AFTER) {
         if (this.phase == HellfireRayCinematic.Phase.WINDUP && this.swell != null) {
            this.swell.end();
         }

         this.phase = HellfireRayCinematic.Phase.COLLAPSE;
         this.phaseAge = 0;
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      this.phaseAge++;
      Entity e = this.anchor.entity(level);
      if (e != null) {
         this.follow(e);
      } else {
         this.ox = this.x;
         this.oy = this.y;
         this.oz = this.z;
      }

      double cy = this.y + (double)this.height * 0.5;
      if (this.age % 4 == 1 || Double.isNaN(this.groundY)) {
         this.groundY = this.ground(level, this.x, this.y + 0.5, this.z);
      }

      if (this.releaseWave >= 0 && ++this.releaseWave > 14) {
         this.releaseWave = -1;
      }

      if (this.blastWave >= 0 && ++this.blastWave > 16) {
         this.blastWave = -1;
      }

      this.intensity = Math.max(0.55F, this.intensity - 0.04F);
      this.pos.set(Mth.floor(this.x), Mth.floor(this.y + 1.0), Mth.floor(this.z));
      this.light = 0.3F + 0.7F * (float)level.getMaxLocalRawBrightness(this.pos) / 15.0F;
      if (this.firstPersonCaster()) {
         VfxParticles.nearFade(4.5F);
      }

      if (this.tail != null && e != null && this.phase != HellfireRayCinematic.Phase.WINDUP) {
         HybridAnchors.get(e, 2, 1.0F, this.anchor3);
         this.tail.push(this.anchor3.x, this.anchor3.y, this.anchor3.z, VfxClock.time());
      }

      switch (this.phase) {
         case WINDUP:
            this.tickWindup(level, e, cy);
            break;
         case RAY:
            this.tickRay(level, e, cy);
            break;
         case COLLAPSE:
            this.tickCollapse(level, e, cy);
            break;
         case AFTER:
            this.tickAfter(level, e, cy);
      }
   }

   private void tickWindup(ClientLevel level, @Nullable Entity e, double cy) {
      float p = Math.min(1.0F, (float)this.phaseAge / (float)this.windupTicks);
      if (e != null) {
         for (int anchorId : FLARING) {
            HybridAnchors.get(e, anchorId, 1.0F, this.anchor3);

            for (int i = 0; i < VfxSettings.count(1.0F + 2.5F * p); i++) {
               VfxParticles.emit(
                     VfxPresets.FIRE_TONGUE,
                     this.anchor3.x + this.spread(0.15),
                     this.anchor3.y + this.spread(0.15),
                     this.anchor3.z + this.spread(0.15),
                     this.spread(0.03),
                     0.04 + 0.05 * (double)p,
                     this.spread(0.03)
                  )
                  .size(0.7F + 0.6F * p);
            }

            VfxParticles.emit(VfxPresets.GLOW, this.anchor3.x, this.anchor3.y, this.anchor3.z, 0.0, 0.0, 0.0)
               .size(0.9F + 1.4F * p)
               .tint(16742938)
               .alpha(0.6F + 0.4F * p);
         }
      }

      for (int i = 0; i < VfxSettings.count(3.0F + 4.0F * p); i++) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         double r = (double)this.ringRadius * (0.6 + this.random.nextDouble() * 0.6);
         VfxParticles.emit(
               VfxPresets.EMBER,
               this.x + Math.cos(a) * r,
               this.y + 0.2 + this.random.nextDouble() * 0.6,
               this.z + Math.sin(a) * r,
               0.0,
               0.07 + this.random.nextDouble() * 0.08,
               0.0
            )
            .orbit(this.x, this.z, 0.22F + 0.1F * p)
            .heat(1.1F);
      }

      this.groundGlow(0.35F + 0.4F * p, this.ringRadius * 2.6F);
      if (this.local()) {
         CameraDirector.dolly(1.0F - 0.07F * p, 0.3F);
      }

      float ex = this.exposure();
      if (ex > 0.0F) {
         VfxPost.grade(16769724, 0.3F * p * ex);
      }
   }

   private void tickRay(ClientLevel level, @Nullable Entity e, double cy) {
      float t = VfxClock.time();
      this.aim(level, e, cy);

      for (int i = 0; i < 3; i++) {
         this.ringPoint(i, t, 0.0F, 1.0F, this.anchor3);
         float sgn = Math.signum(RING_SPEED[i]);

         for (int k = 0; k < VfxSettings.count(this.firstPersonCaster() ? 0.7F : 2.0F); k++) {
            VfxParticles.emit(
                  VfxPresets.FIRE_TONGUE,
                  this.anchor3.x + this.spread(0.12),
                  this.anchor3.y + this.spread(0.12),
                  this.anchor3.z + this.spread(0.12),
                  this.spread(0.06),
                  0.03,
                  this.spread(0.06)
               )
               .size(1.1F);
         }

         this.ringPoint(i, t, 0.15F + this.random.nextFloat() * 0.6F, 1.0F, this.anchor3);
         if (this.random.nextFloat() < 0.8F * VfxSettings.spawnScale()) {
            VfxParticles.emit(VfxPresets.FIRE_PUFF, this.anchor3.x, this.anchor3.y, this.anchor3.z, this.spread(0.04), 0.02, this.spread(0.04))
               .size(0.75F)
               .heat(1.05F);
         }

         for (int k = 0; k < VfxSettings.count(1.0F); k++) {
            VfxParticles.emit(
                  VfxPresets.EMBER,
                  this.x + this.spread(0.4),
                  cy + this.spread(0.6),
                  this.z + this.spread(0.4),
                  this.spread(0.1),
                  this.spread(0.08),
                  this.spread(0.1)
               )
               .orbit(this.x, this.z, 0.32F * sgn);
         }
      }

      VfxParticles.emit(VfxPresets.GLOW, this.x, cy, this.z, 0.0, 0.0, 0.0)
         .size(this.ringRadius * 1.25F)
         .tint(16765503)
         .alpha(0.3F + 0.15F * this.intensity)
         .life(0.5F);
      float len = this.lanceLength;
      double tx = this.x + (double)(this.lx * len);
      double ty = cy + (double)(this.ly * len);
      double tz = this.z + (double)(this.lz * len);
      VfxParticles.emit(VfxPresets.GLOW, tx, ty, tz, 0.0, 0.0, 0.0).size(1.6F).tint(16765503).alpha(0.8F).life(0.5F);

      for (int k = 0; k < VfxSettings.count(2.0F); k++) {
         VfxParticles.emit(
               VfxPresets.FIRE_TONGUE,
               tx + this.spread(0.2),
               ty + this.spread(0.2),
               tz + this.spread(0.2),
               (double)this.lx * 0.1 + this.spread(0.05),
               0.04,
               (double)this.lz * 0.1 + this.spread(0.05)
            )
            .size(0.9F);
      }

      double gt = this.ground(level, tx, ty + 0.6, tz);
      if (!Double.isNaN(gt) && ty - gt < 1.2) {
         for (int k = 0; k < VfxSettings.count(2.5F); k++) {
            VfxParticles.emit(
               VfxPresets.SPARK,
               tx + this.spread(0.3),
               gt + 0.15,
               tz + this.spread(0.3),
               this.spread(0.32) + (double)this.lx * 0.15,
               0.15 + this.random.nextDouble() * 0.28,
               this.spread(0.32) + (double)this.lz * 0.15
            );
         }
      }

      if (!Double.isNaN(this.groundY) && this.y - this.groundY < 7.0 && --this.scorchTimer <= 0) {
         this.scorchTimer = 6;
         float s = this.ringRadius * (1.6F + this.random.nextFloat() * 0.5F);
         float rot = this.random.nextFloat() * 6.28F;
         VfxParticles.emit(VfxPresets.SCORCH, this.x, this.groundY + 0.02, this.z, 0.0, 0.0, 0.0).size(s).rotation(rot);
         VfxParticles.emit(VfxPresets.SCORCH_GLOW, this.x, this.groundY + 0.025, this.z, 0.0, 0.0, 0.0).size(s).rotation(rot);
      }

      this.groundGlow(0.5F + 0.25F * this.intensity, this.ringRadius * 3.6F);
      if (this.random.nextFloat() < 0.35F * VfxSettings.spawnScale()) {
         VfxParticles.emit(
               VfxPresets.SMOKE,
               this.x + this.spread((double)this.ringRadius),
               cy + (double)this.ringRadius * 0.6,
               this.z + this.spread((double)this.ringRadius),
               this.spread(0.02),
               0.06,
               this.spread(0.02)
            )
            .tint(this.shade(3024932))
            .alpha(0.55F)
            .size(0.8F);
      }

      float ex = this.exposure();
      if (ex > 0.0F) {
         VfxPost.grade(16769724, 0.36F * ex);
         VfxPost.vignette(0.22F * ex);
         if (this.age % 4 == 0) {
            CameraDirector.shake(19985, 0.3F * ex, 0.05F, 0.55F, 3.2F);
         }
      }
   }

   private void tickCollapse(ClientLevel level, @Nullable Entity e, double cy) {
      for (int i = 0; i < VfxSettings.count(6.0F); i++) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         double r = (double)this.ringRadius * 1.3;
         VfxParticles.emit(
               VfxPresets.EMBER, this.x + Math.cos(a) * r, cy + this.spread(1.0), this.z + Math.sin(a) * r, -Math.cos(a) * 0.35, 0.0, -Math.sin(a) * 0.35
            )
            .heat(1.3F);
      }

      VfxParticles.emit(VfxPresets.GLOW, this.x, cy, this.z, 0.0, 0.0, 0.0)
         .size(this.ringRadius * (1.4F + (float)this.phaseAge * 0.25F))
         .tint(16774338)
         .life(0.5F);
      float ex = this.exposure();
      if (ex > 0.0F) {
         VfxPost.grade(16769724, 0.4F * ex);
         VfxPost.vignette(0.25F * ex);
      }

      if (this.phaseAge >= this.explodeAt) {
         this.explode(level, cy);
         this.phase = HellfireRayCinematic.Phase.AFTER;
         this.phaseAge = 0;
      }
   }

   private void explode(ClientLevel level, double cy) {
      Vec3 center = new Vec3(this.x, cy, this.z);

      for (int i = 0; i < VfxSettings.count(5.0F) + 1; i++) {
         VfxParticles.emit(
               VfxPresets.FIREBALL,
               this.x + this.spread(0.9),
               cy + this.spread(0.7),
               this.z + this.spread(0.9),
               this.spread(0.1),
               0.03 + this.random.nextDouble() * 0.06,
               this.spread(0.1)
            )
            .size(1.05F + this.random.nextFloat() * 0.55F);
      }

      for (int i = 0; i < VfxSettings.count(20.0F); i++) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         double up = this.random.nextDouble() * 0.8 - 0.2;
         VfxParticles.emit(VfxPresets.FIRE_PUFF, this.x, cy, this.z, Math.cos(a) * 0.38, up * 0.3, Math.sin(a) * 0.38).size(1.4F);
      }

      VfxParticles.emit(VfxPresets.FLARE, this.x, cy, this.z, 0.0, 0.0, 0.0).size(1.1F).heat(1.05F);
      VfxParticles.emit(VfxPresets.GLOW, this.x, cy, this.z, 0.0, 0.0, 0.0).size(this.ringRadius * 4.0F).tint(16742938).alpha(0.75F).life(2.0F);

      for (int i = 0; i < VfxSettings.count(34.0F); i++) {
         VfxParticles.emit(VfxPresets.EMBER, this.x, cy, this.z, this.spread(0.55), this.random.nextDouble() * 0.5, this.spread(0.55)).life(1.3F);
      }

      for (int i = 0; i < VfxSettings.count(16.0F); i++) {
         VfxParticles.emit(VfxPresets.SPARK, this.x, cy, this.z, this.spread(0.6), 0.1 + this.random.nextDouble() * 0.4, this.spread(0.6));
      }

      double gy = this.ground(level, this.x, this.y + 0.5, this.z);
      if (!Double.isNaN(gy) && this.y - gy < 6.0) {
         this.blastX = this.x;
         this.blastY = gy + 0.04;
         this.blastZ = this.z;
         this.blastWave = 0;
         VfxParticles.emit(VfxPresets.RING_WAVE, this.x, gy + 0.05, this.z, 0.0, 0.0, 0.0).size(this.ringRadius * 1.05F).tint(16742938).alpha(0.85F).life(1.3F);

         for (int i = 0; i < VfxSettings.count(28.0F); i++) {
            double a = (double)((float) (Math.PI * 2) * (float)i / 28.0F) + this.random.nextDouble() * 0.2;
            double r = 0.6 + this.random.nextDouble() * 0.4;
            VfxParticles.emit(VfxPresets.FIRE_PUFF, this.x + Math.cos(a) * r, gy + 0.35, this.z + Math.sin(a) * r, Math.cos(a) * 0.42, 0.03, Math.sin(a) * 0.42)
               .size(1.2F)
               .life(1.2F);
         }

         int dust = this.shade(this.groundColour(level, this.x, gy - 0.5, this.z));

         for (int i = 0; i < VfxSettings.count(14.0F); i++) {
            double a = this.random.nextDouble() * (float) (Math.PI * 2);
            VfxParticles.emit(VfxPresets.DUST, this.x + Math.cos(a), gy + 0.3, this.z + Math.sin(a), Math.cos(a) * 0.4, 0.05, Math.sin(a) * 0.4)
               .tint(dust)
               .size(1.3F);
         }

         if (!this.firstPersonCaster()) {
            VfxManager.add(new FloorDebrisEffect(level, new Vec3(this.x, gy, this.z), 16, 0.25F, 0.6F, 70, this.seed ^ 727639L));
         }

         float s = this.ringRadius * 3.4F;
         float rot = this.random.nextFloat() * 6.28F;
         VfxParticles.emit(VfxPresets.SCORCH, this.x, gy + 0.03, this.z, 0.0, 0.0, 0.0).size(s).rotation(rot).life(1.6F);
         VfxParticles.emit(VfxPresets.SCORCH_GLOW, this.x, gy + 0.035, this.z, 0.0, 0.0, 0.0).size(s).rotation(rot).life(1.5F).heat(1.15F);
      }

      for (int i = 0; i < VfxSettings.count(18.0F); i++) {
         double h = this.random.nextDouble();
         VfxParticles.emit(
               VfxPresets.SMOKE, this.x + this.spread(0.7), cy + h * 2.0, this.z + this.spread(0.7), this.spread(0.04), 0.08 + h * 0.16, this.spread(0.04)
            )
            .tint(this.shade(3813418))
            .size(1.3F + 0.6F * (float)h)
            .life(1.3F);
      }

      float ex = this.exposure();
      if (ex > 0.0F) {
         float lens = this.firstPersonCaster() ? 0.35F : 1.0F;
         CameraDirector.shake(0.9F * ex, 0.09F, 4.5F, 3.2F);
         CameraDirector.fovPunch(0.09F * ex);
         VfxPost.flash(16742938, 0.3F * ex * lens);
         VfxPost.chromatic(0.9F * ex * lens);
         VfxPost.radialBlur(center, 0.1F * ex * lens);
         VfxClock.hitStop(2);
      }

      SoundDesign.play(
         center,
         SeiryuSounds.HELLFIRE_THOOM,
         2.0F,
         0.85F,
         true,
         SoundDesign.layer(SeiryuSounds.HELLFIRE_END, 1.0F, 0.9F),
         SoundDesign.layer(SeiryuSounds.LAYER_BLAST, 0.6F, 1.1F),
         SoundDesign.layer(DFSounds.LAYER_BOOM, 1.0F, 0.7F)
      );
   }

   private void tickAfter(ClientLevel level, @Nullable Entity e, double cy) {
      float p = Math.min(1.0F, (float)this.phaseAge / 40.0F);
      if (this.phaseAge == 6) {
         SoundDesign.layerOnly(new Vec3(this.x, this.y, this.z), SeiryuSounds.HELLFIRE_TAIL, 1.0F, 0.8F);
      }

      for (int i = 0; i < VfxSettings.count(2.5F * (1.0F - p)); i++) {
         double a = this.random.nextDouble() * (float) (Math.PI * 2);
         double r = this.random.nextDouble() * (double)this.ringRadius * 1.6;
         VfxParticles.emit(
               VfxPresets.EMBER,
               this.x + Math.cos(a) * r,
               this.y + this.random.nextDouble() * (double)this.height,
               this.z + Math.sin(a) * r,
               this.spread(0.02),
               0.03 + this.random.nextDouble() * 0.04,
               this.spread(0.02)
            )
            .heat(0.9F);
      }

      this.groundGlow(0.45F * (1.0F - p), this.ringRadius * 3.0F);
      float ex = this.exposure();
      if (ex > 0.0F) {
         VfxPost.grade(16769724, 0.36F * (1.0F - p) * ex);
      }

      if (this.phaseAge >= 40) {
         this.finish();
      }
   }

   @Override
   public void render(VfxFrame f) {
      float t = f.time;
      float ep = f.entityPartial;
      double bx = Mth.lerp((double)ep, this.ox, this.x);
      double by = Mth.lerp((double)ep, this.oy, this.y) + (double)this.height * 0.5;
      double bz = Mth.lerp((double)ep, this.oz, this.z);
      boolean firstPerson = this.local() && Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON;
      float near = firstPerson ? 0.35F : 1.0F;
      if (this.phase == HellfireRayCinematic.Phase.RAY || this.phase == HellfireRayCinematic.Phase.COLLAPSE) {
         float collapse = this.phase == HellfireRayCinematic.Phase.COLLAPSE
            ? Mth.clamp(((float)this.phaseAge + f.partial) / (float)this.explodeAt, 0.0F, 1.0F)
            : 0.0F;
         float scale = 1.0F - 0.85F * collapse * collapse;
         float grow = this.phase == HellfireRayCinematic.Phase.RAY ? Mth.clamp(((float)this.phaseAge + f.partial) / 4.0F, 0.0F, 1.0F) : 1.0F;
         this.renderRings(f, t, bx, by, bz, scale * (0.55F + 0.45F * grow), near * (0.6F + 0.4F * grow), 1.0F + collapse * 0.4F);
         if (collapse < 0.6F) {
            this.renderLance(f, t, bx, by, bz, (1.0F - collapse / 0.6F) * grow, near);
         }
      }

      if (this.tail != null && this.phase != HellfireRayCinematic.Phase.WINDUP) {
         Minecraft mc = Minecraft.getInstance();
         Entity e = mc.level != null ? this.anchor.entity(mc.level) : null;
         if (e != null) {
            HybridAnchors.get(e, 2, ep, this.anchor3);
            float fade = this.phase == HellfireRayCinematic.Phase.AFTER ? 1.0F - Mth.clamp(((float)this.phaseAge + f.partial) / 40.0F, 0.0F, 1.0F) : 1.0F;
            this.tail.render(f, this.anchor3.x, this.anchor3.y, this.anchor3.z, fade * near);
         }
      }

      this.renderDistortion(f, t, bx, by, bz);
   }

   private void ringPoint(int i, float t, float along, float scale, Vector3d out) {
      float radius = this.ringRadius * RING_SCALE[i] * scale;
      float tilt = RING_TILT[i];
      float azim = (float)i * 2.1F + t * RING_PRECESS[i];
      float nx = Mth.sin(tilt) * Mth.cos(azim);
      float ny = Mth.cos(tilt);
      float nz = Mth.sin(tilt) * Mth.sin(azim);
      float ux = 0.0F;
      float uz = -ny;
      float ul = Mth.sqrt(nz * nz + uz * uz);
      float uy = nz / ul;
      uz /= ul;
      float vx = ny * uz - nz * uy;
      float vy = nz * ux - nx * uz;
      float vz = nx * uy - ny * ux;
      float head = (float)i * 2.4F + t * RING_SPEED[i];
      float a = head - Math.signum(RING_SPEED[i]) * along * 5.0893803F;
      float rr = radius * (1.0F + 0.07F * Mth.sin(a * 3.0F + t * 0.8F)) * (1.0F - 0.2F * along);
      float off = (along - 0.35F) * 0.32F * radius;
      double cy = this.y + (double)this.height * 0.5;
      out.set(
         this.x + (double)((ux * Mth.cos(a) + vx * Mth.sin(a)) * rr) + (double)(nx * off),
         cy + (double)((uy * Mth.cos(a) + vy * Mth.sin(a)) * rr) + (double)(ny * off),
         this.z + (double)((uz * Mth.cos(a) + vz * Mth.sin(a)) * rr) + (double)(nz * off)
      );
   }

   private void renderRings(VfxFrame f, float t, double bx, double by, double bz, float scale, float alpha, float heatBoost) {
      int n = 44;

      for (int i = 0; i < 3; i++) {
         if (!f.allowMesh()) {
            return;
         }

         float radius = this.ringRadius * RING_SCALE[i] * scale;
         float tilt = RING_TILT[i];
         float azim = (float)i * 2.1F + t * RING_PRECESS[i];
         float nx = Mth.sin(tilt) * Mth.cos(azim);
         float ny = Mth.cos(tilt);
         float nz = Mth.sin(tilt) * Mth.sin(azim);
         float uz = -ny;
         float ul = Mth.sqrt(nz * nz + uz * uz);
         float uy = nz / ul;
         uz /= ul;
         float vx = ny * uz - nz * uy;
         float vy = -nx * uz;
         float vz = nx * uy;
         float head = (float)i * 2.4F + t * RING_SPEED[i];
         float sgn = Math.signum(RING_SPEED[i]);
         float cx = f.rx(bx);
         float cy = f.ry(by);
         float cz = f.rz(bz);

         for (int k = 0; k < n; k++) {
            float along = (float)k / (float)(n - 1);
            float a = head - sgn * along * 5.0893803F;
            float rr = radius * (1.0F + 0.07F * Mth.sin(a * 3.0F + t * 0.8F)) * (1.0F - 0.2F * along);
            float off = (along - 0.35F) * 0.32F * radius;
            float ca = Mth.cos(a);
            float sa2 = Mth.sin(a);
            this.sx[k] = cx + vx * sa2 * rr + nx * off;
            this.sy[k] = cy + (uy * ca + vy * sa2) * rr + ny * off;
            this.sz[k] = cz + (uz * ca + vz * sa2) * rr + nz * off;
            this.sw[k] = (0.42F - 0.28F * along) * Math.max(0.5F, scale);
            this.sh[k] = (1.1F - 0.55F * along) * heatBoost;
            float keep = 1.0F - along;
            this.sa[k] = keep * Mth.sqrt(keep) * alpha * nearFade(this.sx[k], this.sy[k], this.sz[k]);
         }

         BufferBuilder b = f.mesh(MeshTex.FLAME_RIBBON);
         MeshWriter.strip(b, n, this.sx, this.sy, this.sz, this.sw, this.sh, this.sa, -t * 0.14F + (float)i * 0.31F, 0.055F, 16777215, 0, 1.1F);
         int m = n / 2;

         for (int k = 0; k < m; k++) {
            this.sw[k] = 0.065F * Math.max(0.6F, scale);
            this.sh[k] = 1.35F * heatBoost;
            float keep = 1.0F - (float)k / (float)(m - 1);
            this.sa[k] = keep * alpha * nearFade(this.sx[k], this.sy[k], this.sz[k]);
         }

         MeshWriter.strip(b, m, this.sx, this.sy, this.sz, this.sw, this.sh, this.sa, -t * 0.2F, 0.05F, 16777215, 0, 2.2F);
      }
   }

   private void renderLance(VfxFrame f, float t, double bx, double by, double bz, float grow, float near) {
      if (!(grow <= 0.01F) && f.allowMesh()) {
         float p = f.partial;
         float dx = Mth.lerp(p, this.olx, this.lx);
         float dy = Mth.lerp(p, this.oly, this.ly);
         float dz = Mth.lerp(p, this.olz, this.lz);
         float dl = Mth.sqrt(dx * dx + dy * dy + dz * dz) + 1.0E-5F;
         dx /= dl;
         dy /= dl;
         dz /= dl;
         float len = Mth.lerp(p, this.oLanceLength, this.lanceLength) * grow;
         float cx = f.rx(bx);
         float cy = f.ry(by);
         float cz = f.rz(bz);
         int n = 10;

         for (int k = 0; k < n; k++) {
            float s = (float)k / (float)(n - 1);
            float d = 0.3F + s * len;
            this.sx[k] = cx + dx * d;
            this.sy[k] = cy + dy * d;
            this.sz[k] = cz + dz * d;
            this.sw[k] = 0.42F * (1.0F - 0.4F * s);
            this.sh[k] = 0.85F - 0.15F * s;
            this.sa[k] = 0.85F * near * (1.0F - s * s * 0.6F) * nearFade(this.sx[k], this.sy[k], this.sz[k]);
         }

         BufferBuilder lance = f.mesh(MeshTex.LANCE);
         MeshWriter.strip(lance, n, this.sx, this.sy, this.sz, this.sw, this.sh, this.sa, -t * 0.35F, 0.25F, 16777215, 0, 1.8F);

         for (int k = 0; k < n; k++) {
            float s = (float)k / (float)(n - 1);
            this.sw[k] = 0.09F * (1.0F - 0.4F * s);
            this.sh[k] = 1.6F;
            this.sa[k] = near * (1.0F - s * 0.5F) * nearFade(this.sx[k], this.sy[k], this.sz[k]);
         }

         MeshWriter.strip(lance, n, this.sx, this.sy, this.sz, this.sw, this.sh, this.sa, -t * 0.6F, 0.25F, 16777215, 0, 4.0F);
         if (f.allowMesh()) {
            MeshWriter.cone(
               f.mesh(MeshTex.NOISE),
               cx + dx * 0.2F,
               cy + dy * 0.2F,
               cz + dz * 0.2F,
               dx,
               dy,
               dz,
               len * 0.92F,
               0.6F,
               0.18F,
               28,
               t * 0.01F,
               2.0F,
               -t * 0.06F,
               1.5F,
               MeshWriter.argb(16777215, 0.45F * near),
               MeshWriter.argb(16777215, 0.1F * near),
               1.0F,
               0.7F,
               0,
               1.0F,
               0.25F,
               0.35F
            );
         }
      }
   }

   private void renderDistortion(VfxFrame f, float t, double bx, double by, double bz) {
      BufferBuilder d = f.distortion();
      if (d != null) {
         VfxSprites.Sprite glow = VfxSprites.GLOW;
         VfxSprites.Sprite ring = VfxSprites.RING_NORMAL;

         float haze = switch (this.phase) {
            case WINDUP -> 0.5F * Math.min(1.0F, (float)this.phaseAge / (float)this.windupTicks);
            case RAY, COLLAPSE -> 0.8F;
            case AFTER -> 0.8F * (1.0F - Math.min(1.0F, (float)this.phaseAge / 40.0F));
         };
         if (haze > 0.02F) {
            MeshWriter.distortQuad(
               d,
               f.rx(bx),
               f.ry(by),
               f.rz(bz),
               this.ringRadius * 1.5F,
               haze,
               1,
               false,
               f.left.x,
               f.left.y,
               f.left.z,
               f.up.x,
               f.up.y,
               f.up.z,
               glow.u0(),
               glow.v0(),
               glow.u1(),
               glow.v1(),
               t * 0.004F,
               0.0F
            );
            if (this.phase == HellfireRayCinematic.Phase.RAY) {
               for (int k = 1; k <= 3; k++) {
                  float s = (float)k / 3.0F * this.lanceLength;
                  MeshWriter.distortQuad(
                     d,
                     f.rx(bx) + this.lx * s,
                     f.ry(by) + this.ly * s,
                     f.rz(bz) + this.lz * s,
                     0.9F,
                     0.9F,
                     1,
                     false,
                     f.left.x,
                     f.left.y,
                     f.left.z,
                     f.up.x,
                     f.up.y,
                     f.up.z,
                     glow.u0(),
                     glow.v0(),
                     glow.u1(),
                     glow.v1(),
                     (float)k * 0.21F + t * 0.006F,
                     0.3F
                  );
               }
            }
         }

         if (this.releaseWave >= 0) {
            float p = Mth.clamp(((float)this.releaseWave + f.partial) / 14.0F, 0.0F, 1.0F);
            MeshWriter.distortQuad(
               d,
               f.rx(this.waveX),
               f.ry(this.waveY),
               f.rz(this.waveZ),
               1.0F + p * this.ringRadius * 4.0F,
               1.2F * (1.0F - p),
               0,
               true,
               0.0F,
               0.0F,
               0.0F,
               0.0F,
               0.0F,
               0.0F,
               ring.u0(),
               ring.v0(),
               ring.u1(),
               ring.v1(),
               0.0F,
               0.0F
            );
         }

         if (this.blastWave >= 0) {
            float p = Mth.clamp(((float)this.blastWave + f.partial) / 16.0F, 0.0F, 1.0F);
            float e = 1.0F - (1.0F - p) * (1.0F - p);
            MeshWriter.distortQuad(
               d,
               f.rx(this.blastX),
               f.ry(this.blastY),
               f.rz(this.blastZ),
               1.0F + e * this.ringRadius * 5.5F,
               1.4F * (1.0F - p),
               0,
               true,
               0.0F,
               0.0F,
               0.0F,
               0.0F,
               0.0F,
               0.0F,
               ring.u0(),
               ring.v0(),
               ring.u1(),
               ring.v1(),
               0.0F,
               0.0F
            );
            MeshWriter.distortQuad(
               d,
               f.rx(bx),
               f.ry(by),
               f.rz(bz),
               1.0F + e * this.ringRadius * 3.0F,
               1.2F * (1.0F - p),
               0,
               false,
               f.left.x,
               f.left.y,
               f.left.z,
               f.up.x,
               f.up.y,
               f.up.z,
               ring.u0(),
               ring.v0(),
               ring.u1(),
               ring.v1(),
               0.0F,
               0.0F
            );
         }
      }
   }

   private void aim(ClientLevel level, @Nullable Entity self, double cy) {
      this.olx = this.lx;
      this.oly = this.ly;
      this.olz = this.lz;
      this.oLanceLength = this.lanceLength;
      double ax = (double)this.facingX * 0.7071;
      double ay = -0.7071;
      double az = (double)this.facingZ * 0.7071;
      Entity aimed = this.aimId >= 0 ? level.getEntity(this.aimId) : null;
      if (aimed != null && aimed.isAlive() && aimed.distanceToSqr(this.x, cy, this.z) < 64.0) {
         Vec3 c = aimed.getBoundingBox().getCenter();
         double tx = c.x - this.x;
         double ty = c.y - cy;
         double tz = c.z - this.z;
         double tl = Math.sqrt(tx * tx + ty * ty + tz * tz);
         if (tl > 0.001) {
            ax = ax * 0.8 + tx / tl * 0.2;
            ay = ay * 0.8 + ty / tl * 0.2;
            az = az * 0.8 + tz / tl * 0.2;
         }
      }

      double l = Math.sqrt(ax * ax + ay * ay + az * az);
      this.lx = this.lx + ((float)(ax / l) - this.lx) * 0.4F;
      this.ly = this.ly + ((float)(ay / l) - this.ly) * 0.4F;
      this.lz = this.lz + ((float)(az / l) - this.lz) * 0.4F;
      float want = 6.0F;
      if (!Double.isNaN(this.groundY) && this.ly < -0.2F) {
         want = (float)Mth.clamp((cy - this.groundY) / (double)(-this.ly), 3.5, 8.0);
      }

      this.lanceLength = this.lanceLength + (want - this.lanceLength) * 0.4F;
   }

   private void groundGlow(float alpha, float size) {
      if (!Double.isNaN(this.groundY) && !(this.y - this.groundY > 8.0)) {
         float flicker = 0.85F + 0.15F * Mth.sin(VfxClock.time() * 1.7F) * Mth.sin(VfxClock.time() * 0.63F + 1.3F);
         float lift = (float)Math.max(0.0, this.y - this.groundY);
         VfxParticles.emit(VfxPresets.GROUND_GLOW, this.x, this.groundY + 0.04, this.z, 0.0, 0.0, 0.0)
            .size(size * (1.0F + lift * 0.1F))
            .tint(16742938)
            .alpha(alpha * flicker / (1.0F + lift * 0.25F));
      }
   }

   private static float nearFade(float rx, float ry, float rz) {
      float d = Mth.sqrt(rx * rx + ry * ry + rz * rz);
      return Mth.clamp((d - 0.9F) / 1.6F, 0.0F, 1.0F);
   }

   private double ground(ClientLevel level, double px, double py, double pz) {
      this.pos.set(Mth.floor(px), Mth.floor(py), Mth.floor(pz));

      for (int i = 0; i < 9; i++) {
         BlockState state = level.getBlockState(this.pos);
         VoxelShape shape = state.getCollisionShape(level, this.pos);
         if (!shape.isEmpty()) {
            return (double)this.pos.getY() + shape.max(Axis.Y);
         }

         this.pos.move(0, -1, 0);
      }

      return Double.NaN;
   }

   private int groundColour(ClientLevel level, double px, double py, double pz) {
      this.pos.set(Mth.floor(px), Mth.floor(py), Mth.floor(pz));
      int c = level.getBlockState(this.pos).getMapColor(level, this.pos).col;
      return c == 0 ? 9075302 : c;
   }

   private int shade(int rgb) {
      int r = (int)((float)(rgb >> 16 & 0xFF) * this.light);
      int g = (int)((float)(rgb >> 8 & 0xFF) * this.light);
      int b = (int)((float)(rgb & 0xFF) * this.light);
      return r << 16 | g << 8 | b;
   }

   private double spread(double amount) {
      return (this.random.nextDouble() * 2.0 - 1.0) * amount;
   }

   @Override
   public void removed() {
      if (this.tail != null) {
         this.tail.release();
      }

      if (this.swell != null) {
         this.swell.end();
      }

      if (this.loop != null) {
         this.loop.end();
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

   private static enum Phase {
      WINDUP,
      RAY,
      COLLAPSE,
      AFTER;
   }
}
