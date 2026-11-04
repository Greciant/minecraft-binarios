package com.awakenedmod.awakened.client.beam;

import com.awakenedmod.awakened.client.AwakenedAnimations;
import com.awakenedmod.awakened.client.ClipBearers;
import com.awakenedmod.awakened.client.PyroClientHeat;
import com.awakenedmod.awakened.client.PyroNozzleTracker;
import com.awakenedmod.awakened.client.PyroSourceAnchors;
import com.awakenedmod.awakened.content.pyrokinetic.FlameBeamAbility;
import com.awakenedmod.awakened.core.AwId;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;

public final class PyroBeamSim {
   public static final int CAP = 400;
   static final int MAX_LIFE = 40;
   static final int CLAIM_PROBE = 8;
   static final int IMPACT_CAP = 64;
   private static final int THROTTLE_NEVER = -1073741824;
   static final float SPEED_MIN = 0.75F;
   static final float SPEED_SPAN = 0.6F;
   static final float SIZE_MIN = 0.7F;
   static final float SIZE_SPAN = 0.7F;
   static final float EARLY_DEATH_CHANCE = 0.25F;
   static final float EARLY_DEATH_MIN = 0.6F;
   static final float EARLY_DEATH_SPAN = 0.3F;
   static final float TIER_HOT_CHANCE = 0.15F;
   static final float TIER_COOL_CHANCE = 0.1F;
   static final float WANDER = 0.12F;
   static final float WANDER_AMP_MIN = 0.5F;
   static final float WANDER_AMP_SPAN = 1.0F;
   static final float WANDER_FREQ_MIN = 0.12F;
   static final float WANDER_FREQ_SPAN = 0.18F;
   static final float SPLASH_SPEED_MIN = 0.7F;
   static final float SPLASH_SPEED_SPAN = 0.6F;
   static final float SPLASH_DRAG = 0.88F;
   static final int SPLASH_LIFE = 4;
   static final float FLOW_RIGHT = 0.965F;
   static final float FLOW_LEFT = 1.035F;
   private static final float[] FLOW_BY_SLOT = new float[]{1.035F, 0.965F, 0.985F, 1.015F};
   static final float SWEEP_STEP = 0.4F;
   static final int SWEEP_BISECT = 2;
   static final double TARGET_SKIN = 0.15;
   private static final Predicate<LivingEntity> VISIBLE_TARGET = e -> !e.isSpectator();
   public static final byte KIND_CUBE = 0;
   public static final byte KIND_SPLASH = 2;
   public static final byte KIND_IMPACT_GLOW = 3;
   static final int EMIT_CAP_PER_HAND = 8;
   static final float DENSITY = 1.6F;
   private static final int FREE = 0;
   private static final int SOLID = 1;
   private static final int WATER = 2;
   private static final PyroBeamSim.Source[] SOURCES = PyroBeamSim.Source.values();
   private static final double EMIT_RANGE = 96.0;
   private static final Int2ObjectMap<PyroBeamSim.Buffer> BUFFERS = new Int2ObjectOpenHashMap();
   private static final MutableBlockPos POS = new MutableBlockPos();
   private static Level lastLevel;

   private PyroBeamSim() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(PyroBeamSim::onClientTick);
   }

   public static Collection<PyroBeamSim.Buffer> buffers() {
      return BUFFERS.values();
   }

   private static int key(int entityId, PyroBeamSim.Source source) {
      return entityId * SOURCES.length + source.ordinal();
   }

   private static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.player != null) {
         if (mc.level != lastLevel) {
            clear();
            lastLevel = mc.level;
         }

         if (!mc.isPaused()) {
            Level level = mc.level;
            ObjectIterator firstPersonCam = BUFFERS.values().iterator();

            while (firstPersonCam.hasNext()) {
               PyroBeamSim.Buffer b = (PyroBeamSim.Buffer)firstPersonCam.next();
               b.wasEmitting = b.emitting;
               b.emitting = false;
               b.retarget(level);
               b.integrate(level, b.source.style());
            }

            boolean firstPersonCamx = mc.options.getCameraType().isFirstPerson();
            ClipBearers.forEachCaster(level, p -> emit(mc, level, p, firstPersonCam));
            BUFFERS.values().removeIf(bx -> !bx.emitting && bx.live == 0);
         }
      } else {
         clear();
      }
   }

   private static void emit(Minecraft mc, Level level, LivingEntity p, boolean firstPersonCam) {
      if (p == mc.player || !(p.distanceToSqr(mc.player) > 9216.0)) {
         for (PyroBeamSim.Source source : SOURCES) {
            if (source.lit(p.getId())) {
               int key = key(p.getId(), source);
               PyroBeamSim.Buffer b = (PyroBeamSim.Buffer)BUFFERS.get(key);
               if (b == null) {
                  b = new PyroBeamSim.Buffer(p.getId(), source);
                  BUFFERS.put(key, b);
               }

               b.emitting = true;
               b.litAge = b.wasEmitting ? b.litAge + 1 : 0;
               b.blueFraction = PyroClientHeat.blueFraction(p);
               boolean firstPerson = p == mc.player && firstPersonCam;
               Vec3 aim0 = source.aim(p, 0.0F);
               PyroSourceAnchors.Nozzles nozzles0 = source.palms(p, 0.0F, p.getEyePosition(0.0F), firstPerson);
               PyroSourceAnchors.Nozzles nozzles1 = source.palms(p, 1.0F, p.getEyePosition(1.0F), firstPerson);
               boolean observed = source == PyroBeamSim.Source.JET && !firstPerson && PyroNozzleTracker.fresh(p);
               float f = observed ? PyroNozzleTracker.partial(p.getId()) : 0.0F;
               PyroBeamStyle style = source.style();

               for (int slot = 0; slot < nozzles0.count(); slot++) {
                  Vec3 n0 = nozzles0.point(slot);
                  Vec3 carry = nozzles1.point(slot).subtract(n0);
                  if (observed) {
                     n0 = PyroNozzleTracker.point(p.getId(), slot).add(carry.scale((double)(1.0F - f)));
                  }

                  emitNozzle(level, style, b, n0, carry, aim0, slot);
               }
            }
         }
      }
   }

   private static void emitNozzle(Level level, PyroBeamStyle style, PyroBeamSim.Buffer b, Vec3 origin, Vec3 carry, Vec3 aim, int slot) {
      if (probe(level, origin.x, origin.y, origin.z) == 0) {
         b.emitter.nozzle(origin, aim, carry, slot, FLOW_BY_SLOT[slot]);
         style.emit(b.emitter);
      }
   }

   private static void clear() {
      BUFFERS.clear();
      lastLevel = null;
   }

   private static int probe(Level level, double x, double y, double z) {
      int bx = Mth.floor(x);
      int by = Mth.floor(y);
      int bz = Mth.floor(z);
      POS.set(bx, by, bz);
      BlockState state = level.getBlockState(POS);
      if (state.isAir()) {
         return 0;
      } else if (state.getFluidState().is(FluidTags.WATER)) {
         return 2;
      } else {
         VoxelShape shape = state.getCollisionShape(level, POS);
         if (shape.isEmpty()) {
            return 0;
         } else if (state.isCollisionShapeFullBlock(level, POS)) {
            return 1;
         } else {
            double lx = x - (double)bx;
            double ly = y - (double)by;
            double lz = z - (double)bz;
            return lx >= shape.min(Axis.X)
                  && lx <= shape.max(Axis.X)
                  && ly >= shape.min(Axis.Y)
                  && ly <= shape.max(Axis.Y)
                  && lz >= shape.min(Axis.Z)
                  && lz <= shape.max(Axis.Z)
               ? 1
               : 0;
         }
      }
   }

   public static final class Buffer {
      final int entityId;
      final PyroBeamSim.Source source;
      final RandomSource rng = RandomSource.create();
      final PyroBeamSim.Emitter emitter = new PyroBeamSim.Emitter(this);
      final double[] px = new double[400];
      final double[] py = new double[400];
      final double[] pz = new double[400];
      final float[] vx = new float[400];
      final float[] vy = new float[400];
      final float[] vz = new float[400];
      final float[] cx = new float[400];
      final float[] cy = new float[400];
      final float[] cz = new float[400];
      final int[] age = new int[400];
      final int[] life = new int[400];
      final byte[] kind = new byte[400];
      final boolean[] left = new boolean[400];
      final int[] seed = new int[400];
      final float[] size = new float[400];
      final byte[] tierBias = new byte[400];
      final float[] spin = new float[400];
      final float[] ang0 = new float[400];
      final byte[] axis = new byte[400];
      final float[] wx = new float[400];
      final float[] wy = new float[400];
      final float[] wz = new float[400];
      final float[] wAmp = new float[400];
      final float[] wFreq = new float[400];
      final float[] wPhase = new float[400];
      private int head;
      private int live;
      int tick;
      private boolean emitting;
      private boolean wasEmitting;
      int litAge;
      float blueFraction;
      final float[] emitAcc = new float[4];
      private final int[] throttleLast = new int[8];
      private AABB targetHull;
      private final double[] ix = new double[64];
      private final double[] iy = new double[64];
      private final double[] iz = new double[64];
      private final float[] ivx = new float[64];
      private final float[] ivy = new float[64];
      private final float[] ivz = new float[64];
      private final byte[] iKind = new byte[64];
      private final boolean[] iLeft = new boolean[64];
      private final int[] iSeed = new int[64];
      private final float[] iSize = new float[64];
      private int impacts;

      Buffer(int entityId, PyroBeamSim.Source source) {
         this.entityId = entityId;
         this.source = source;
         Arrays.fill(this.throttleLast, -1073741824);
         this.emitAcc[0] = 0.5F;
         this.emitAcc[2] = 0.25F;
         this.emitAcc[3] = 0.75F;
      }

      public int entityId() {
         return this.entityId;
      }

      public PyroBeamSim.Source source() {
         return this.source;
      }

      public float blueFraction() {
         return this.blueFraction;
      }

      public boolean channeling() {
         return this.emitting;
      }

      public int litAge() {
         return this.litAge;
      }

      public int live() {
         return this.live;
      }

      void retarget(Level level) {
         this.targetHull = null;
         if (this.source == PyroBeamSim.Source.BEAM && this.source.lit(this.entityId)) {
            if (level.getEntity(this.entityId) instanceof LivingEntity caster) {
               Vec3 var5 = caster.getEyePosition();
               FlameBeamAbility.LineHit hit = FlameBeamAbility.firstOnLine(
                  level, caster, var5, FlameBeamAbility.beamEnd(level, caster, var5, caster.getViewVector(1.0F)), PyroBeamSim.VISIBLE_TARGET
               );
               if (hit != null) {
                  this.targetHull = hit.entity().getBoundingBox().inflate(0.15);
               }
            }
         }
      }

      void integrate(Level level, PyroBeamStyle style) {
         this.tick++;
         this.impacts = 0;
         float drag = this.source.drag;
         float buoyancy = this.source.buoyancy;

         for (int i = 0; i < 400; i++) {
            int l = this.life[i];
            if (l != 0) {
               if (++this.age[i] >= l) {
                  this.kill(i);
               } else {
                  byte k = this.kind[i];
                  if (k != 3) {
                     double dx = (double)(this.vx[i] + this.cx[i]);
                     double dy = (double)(this.vy[i] + this.cy[i]);
                     double dz = (double)(this.vz[i] + this.cz[i]);
                     this.cx[i] = 0.0F;
                     this.cy[i] = 0.0F;
                     this.cz[i] = 0.0F;
                     if (k == 2 || this.sweep(level, i, dx, dy, dz)) {
                        this.px[i] = this.px[i] + dx;
                        this.py[i] = this.py[i] + dy;
                        this.pz[i] = this.pz[i] + dz;
                        if (k == 2) {
                           this.vx[i] = this.vx[i] * 0.88F;
                           this.vy[i] = this.vy[i] * 0.88F;
                           this.vz[i] = this.vz[i] * 0.88F;
                        } else {
                           this.vx[i] = this.vx[i] * drag;
                           this.vy[i] = this.vy[i] * drag + buoyancy;
                           this.vz[i] = this.vz[i] * drag;
                        }
                     }
                  }
               }
            }
         }

         for (int n = 0; n < this.impacts; n++) {
            this.emitter.impact(this.ix[n], this.iy[n], this.iz[n], this.ivx[n], this.ivy[n], this.ivz[n], this.iLeft[n], this.iSeed[n], this.iSize[n]);
            style.impact(this.emitter, this.iKind[n]);
         }
      }

      private int obstacle(Level level, double x, double y, double z) {
         int r = PyroBeamSim.probe(level, x, y, z);
         if (r != 0) {
            return r;
         } else {
            AABB hull = this.targetHull;
            return hull != null && hull.contains(x, y, z) ? 1 : 0;
         }
      }

      private boolean sweep(Level level, int i, double dx, double dy, double dz) {
         double x = this.px[i];
         double y = this.py[i];
         double z = this.pz[i];
         int n = Math.max(1, Mth.ceil(Math.sqrt(dx * dx + dy * dy + dz * dz) / 0.4F));
         double lo = 0.0;

         for (int s = 1; s <= n; s++) {
            double f = (double)s / (double)n;
            int r = this.obstacle(level, x + dx * f, y + dy * f, z + dz * f);
            if (r != 0) {
               if (r == 2) {
                  this.kill(i);
                  return false;
               }

               double hi = f;

               for (int bsec = 0; bsec < 2; bsec++) {
                  double mid = (lo + hi) * 0.5;
                  if (this.obstacle(level, x + dx * mid, y + dy * mid, z + dz * mid) == 1) {
                     hi = mid;
                  } else {
                     lo = mid;
                  }
               }

               if (this.impacts < 64) {
                  int m = this.impacts++;
                  this.ix[m] = x + dx * lo;
                  this.iy[m] = y + dy * lo;
                  this.iz[m] = z + dz * lo;
                  this.ivx[m] = this.vx[i];
                  this.ivy[m] = this.vy[i];
                  this.ivz[m] = this.vz[i];
                  this.iKind[m] = this.kind[i];
                  this.iLeft[m] = this.left[i];
                  this.iSeed[m] = this.seed[i];
                  this.iSize[m] = this.size[i];
               }

               this.kill(i);
               return false;
            }

            lo = f;
         }

         return true;
      }

      private void kill(int i) {
         this.life[i] = 0;
         this.live--;
      }

      int claim() {
         int i = this.head;
         int k = 0;

         for (int probe = this.head; k < 8; probe = (probe + 1) % 400) {
            if (this.life[probe] == 0) {
               i = probe;
               break;
            }

            k++;
         }

         this.head = (i + 1) % 400;
         if (this.life[i] == 0) {
            this.live++;
         }

         this.life[i] = 1;
         this.age[i] = 0;
         this.cx[i] = 0.0F;
         this.cy[i] = 0.0F;
         this.cz[i] = 0.0F;
         return i;
      }
   }

   public static final class Emitter {
      private final PyroBeamSim.Buffer b;
      private double ox;
      private double oy;
      private double oz;
      private float ax;
      private float ay;
      private float az;
      private float kx;
      private float ky;
      private float kz;
      private int slot;
      private boolean left;
      private float handFlow;
      private int seedBase;
      private float impactSize;
      private int launched;

      Emitter(PyroBeamSim.Buffer b) {
         this.b = b;
      }

      void nozzle(Vec3 origin, Vec3 aim, Vec3 carry, int slot, float flow) {
         this.ox = origin.x;
         this.oy = origin.y;
         this.oz = origin.z;
         this.ax = (float)aim.x;
         this.ay = (float)aim.y;
         this.az = (float)aim.z;
         this.kx = (float)carry.x;
         this.ky = (float)carry.y;
         this.kz = (float)carry.z;
         this.slot = slot;
         this.left = slot == 0 || slot == 2;
         this.handFlow = flow;
         this.seedBase = this.b.entityId * 7919 + this.b.tick * 104729 + slot * 31;
         this.impactSize = 1.0F;
         this.launched = 0;
      }

      public boolean foot() {
         return this.slot >= 2;
      }

      void impact(double x, double y, double z, float vx, float vy, float vz, boolean left, int seed, float size) {
         this.ox = x;
         this.oy = y;
         this.oz = z;
         float len = Mth.sqrt(vx * vx + vy * vy + vz * vz);
         if (len < 1.0E-4F) {
            this.ax = 0.0F;
            this.ay = -1.0F;
            this.az = 0.0F;
         } else {
            this.ax = vx / len;
            this.ay = vy / len;
            this.az = vz / len;
         }

         this.kx = 0.0F;
         this.ky = 0.0F;
         this.kz = 0.0F;
         this.slot = left ? 0 : 1;
         this.left = left;
         this.handFlow = 1.0F;
         this.seedBase = seed * 31 + 93;
         this.impactSize = size;
         this.launched = 0;
      }

      public PyroBeamSim.Buffer buffer() {
         return this.b;
      }

      public boolean left() {
         return this.left;
      }

      public float impactSize() {
         return this.impactSize;
      }

      public int tick() {
         return this.b.tick;
      }

      public int owed(float perTick) {
         float acc = this.b.emitAcc[this.slot] + perTick;
         int n = (int)acc;
         this.b.emitAcc[this.slot] = acc - (float)n;
         return n;
      }

      public boolean throttle(int channel, int everyTicks) {
         int c = channel & 7;
         if (this.b.tick - this.b.throttleLast[c] < everyTicks) {
            return false;
         } else {
            this.b.throttleLast[c] = this.b.tick;
            return true;
         }
      }

      public float density() {
         return 1.6F;
      }

      public float speed() {
         return this.b.source.speed();
      }

      public RandomSource rng() {
         return this.b.rng;
      }

      public int launch(byte kind, float speedScale, float jitter, float swayScale, boolean earlyDeath) {
         if (this.launched >= this.b.source.emitCap) {
            return -1;
         } else {
            this.launched++;
            RandomSource r = this.b.rng;
            float v = this.speed() * speedScale * this.handFlow * (0.75F + 0.6F * r.nextFloat());
            float ux;
            float uy;
            float uz;
            if (Math.abs(this.ay) > 0.9F) {
               ux = 1.0F;
               uy = 0.0F;
               uz = 0.0F;
            } else {
               ux = 0.0F;
               uy = 1.0F;
               uz = 0.0F;
            }

            float p1x = this.ay * uz - this.az * uy;
            float p1y = this.az * ux - this.ax * uz;
            float p1z = this.ax * uy - this.ay * ux;
            float p1l = Mth.sqrt(p1x * p1x + p1y * p1y + p1z * p1z);
            p1x /= p1l;
            p1y /= p1l;
            p1z /= p1l;
            float p2x = this.ay * p1z - this.az * p1y;
            float p2y = this.az * p1x - this.ax * p1z;
            float p2z = this.ax * p1y - this.ay * p1x;
            float j1 = (r.nextFloat() - 0.5F) * 2.0F * jitter;
            float j2 = (r.nextFloat() - 0.5F) * 2.0F * jitter;
            int i = this.b.claim();
            this.b.kind[i] = kind;
            this.b.left[i] = this.left;
            this.b.seed[i] = this.seedBase + i * 374761 + r.nextInt(1048576);
            this.b.px[i] = this.ox;
            this.b.py[i] = this.oy;
            this.b.pz[i] = this.oz;
            this.b.vx[i] = this.ax * v + p1x * j1 + p2x * j2;
            this.b.vy[i] = this.ay * v + p1y * j1 + p2y * j2;
            this.b.vz[i] = this.az * v + p1z * j1 + p2z * j2;
            this.b.cx[i] = this.kx;
            this.b.cy[i] = this.ky;
            this.b.cz[i] = this.kz;
            int l;
            if (this.b.source.lifeMin > 0) {
               l = this.b.source.lifeMin + r.nextInt(this.b.source.lifeSpan + 1);
            } else {
               l = this.rangeLife(v);
            }

            if (earlyDeath && r.nextFloat() < 0.25F) {
               l = Math.round((float)l * (0.6F + 0.3F * r.nextFloat()));
            }

            this.b.life[i] = Mth.clamp(l, 2, 40);
            this.personality(i, r);
            float sa = r.nextFloat() * 6.2832F;
            float c = Mth.cos(sa);
            float s = Mth.sin(sa);
            this.b.wx[i] = p1x * c + p2x * s;
            this.b.wy[i] = p1y * c + p2y * s;
            this.b.wz[i] = p1z * c + p2z * s;
            this.b.wAmp[i] = swayScale * 0.12F * (0.5F + 1.0F * r.nextFloat());
            this.b.wFreq[i] = 0.12F + 0.18F * r.nextFloat();
            this.b.wPhase[i] = r.nextFloat() * 6.2832F;
            return i;
         }
      }

      public int place(byte kind, int life, float size) {
         int i = this.b.claim();
         this.b.kind[i] = kind;
         this.b.left[i] = this.left;
         this.b.seed[i] = this.seedBase + i * 374761;
         this.b.px[i] = this.ox;
         this.b.py[i] = this.oy;
         this.b.pz[i] = this.oz;
         this.b.vx[i] = 0.0F;
         this.b.vy[i] = 0.0F;
         this.b.vz[i] = 0.0F;
         this.b.life[i] = Math.max(2, life);
         this.b.size[i] = size;
         this.b.tierBias[i] = 0;
         this.b.spin[i] = 0.2F;
         this.b.ang0[i] = this.b.rng.nextFloat() * 6.2832F;
         this.b.axis[i] = 1;
         this.b.wAmp[i] = 0.0F;
         return i;
      }

      public void splash(int count, float speed) {
         RandomSource r = this.b.rng;

         for (int n = 0; n < count; n++) {
            double sx = (double)((r.nextFloat() - 0.5F) * 2.0F);
            double sy = (double)(r.nextFloat() * 0.9F);
            double sz = (double)((r.nextFloat() - 0.5F) * 2.0F);
            double dx = sx - (double)this.ax * 0.7;
            double dy = sy - (double)this.ay * 0.7;
            double dz = sz - (double)this.az * 0.7;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 0.01) {
               dx = (double)(-this.ax);
               dy = (double)(-this.ay);
               dz = (double)(-this.az);
               len = 1.0;
            }

            float v = speed * (0.7F + 0.6F * r.nextFloat());
            int i = this.b.claim();
            this.b.kind[i] = 2;
            this.b.left[i] = this.left;
            this.b.seed[i] = this.seedBase + i * 374761 + n * 7;
            this.b.px[i] = this.ox + dx / len * 0.12;
            this.b.py[i] = this.oy + dy / len * 0.12;
            this.b.pz[i] = this.oz + dz / len * 0.12;
            this.b.vx[i] = (float)(dx / len * (double)v);
            this.b.vy[i] = (float)(dy / len * (double)v);
            this.b.vz[i] = (float)(dz / len * (double)v);
            this.b.life[i] = 4 + (r.nextFloat() < 0.5F ? 1 : 0);
            this.personality(i, r);
            this.b.spin[i] = this.b.spin[i] * 2.0F;
            this.b.wAmp[i] = 0.0F;
         }
      }

      private void personality(int i, RandomSource r) {
         this.b.size[i] = 0.7F + 0.7F * r.nextFloat();
         float flick = r.nextFloat();
         this.b.tierBias[i] = (byte)(flick < 0.15F ? 1 : (flick > 0.9F ? -1 : 0));
         this.b.spin[i] = (0.12F + 0.18F * r.nextFloat()) * (r.nextBoolean() ? 1.0F : -1.0F);
         this.b.ang0[i] = r.nextFloat() * 6.2832F;
         this.b.axis[i] = (byte)r.nextInt(3);
      }

      private int rangeLife(float v) {
         double arg = 1.0 - 20.0 * (1.0 - (double)this.b.source.drag) / (double)v;
         return arg <= 0.0 ? 40 : (int)Math.ceil(Math.log(arg) / this.b.source.logDrag);
      }
   }

   public static enum Source {
      BEAM(id -> AwakenedAnimations.isPlaying(id, AwId.of("flame_beam")), 0.985F, 0.007F, 8, 0, 0),
      JET(AwakenedAnimations::isJetting, 0.96F, 0.012F, 16, 12, 4);

      private static final float BEAM_SPEED = 1.15F;
      private static final float JET_SPEED = 0.6F;
      final IntPredicate lit;
      final float drag;
      final double logDrag;
      final float buoyancy;
      final int emitCap;
      final int lifeMin;
      final int lifeSpan;

      private Source(IntPredicate lit, float drag, float buoyancy, int emitCap, int lifeMin, int lifeSpan) {
         this.lit = lit;
         this.drag = drag;
         this.logDrag = Math.log((double)drag);
         this.buoyancy = buoyancy;
         this.emitCap = emitCap;
         this.lifeMin = lifeMin;
         this.lifeSpan = lifeSpan;
      }

      public boolean lit(int entityId) {
         return this.lit.test(entityId);
      }

      public float speed() {
         return this == BEAM ? 1.15F : 0.6F;
      }

      public PyroBeamStyle style() {
         return (PyroBeamStyle)(this == BEAM ? CubeTorrentStyle.INSTANCE : JetExhaustStyle.INSTANCE);
      }

      public boolean fadesNearCamera() {
         return this == JET;
      }

      public Vec3 aim(LivingEntity p, float partialTick) {
         Vec3 look = p.getViewVector(partialTick);
         return this == BEAM ? look : AwakenedAnimations.jetExhaustAim(p, look);
      }

      public PyroSourceAnchors.Nozzles palms(LivingEntity p, float partialTick, Vec3 eye, boolean firstPerson) {
         return this == BEAM ? PyroSourceAnchors.beamPalms(p, partialTick, eye, firstPerson) : PyroSourceAnchors.jetAnchors(p, partialTick, eye, firstPerson);
      }
   }
}
