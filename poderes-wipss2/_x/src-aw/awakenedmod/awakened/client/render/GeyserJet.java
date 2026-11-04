package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.Lab;
import com.awakenedmod.awakened.core.Terrain;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

final class GeyserJet {
   private static final Lab.Value CUBE = Lab.CLIENT.def("hydro.geyser.cube", 0.5, 0.2, 1.0, 0.05);
   private static final Lab.Value RADIUS = Lab.CLIENT.def("hydro.geyser.radius", 0.7, 0.2, 2.0, 0.05);
   private static final Lab.Value WIDEN = Lab.CLIENT.def("hydro.geyser.widen", 1.6, 1.0, 3.0, 0.05);
   private static final Lab.Value RISE = Lab.CLIENT.def("hydro.geyser.rise", 0.35, 0.0, 1.5, 0.01);
   private static final Lab.Value CHURN = Lab.CLIENT.def("hydro.geyser.churn", 0.09, 0.0, 0.5, 0.01);
   private static final Lab.Value SPRAY = Lab.CLIENT.def("hydro.geyser.spray", 2.0, 0.0, 10.0, 1.0);
   private static final Lab.Value BURST = Lab.CLIENT.def("hydro.geyser.burst", 14.0, 0.0, 60.0, 1.0);
   private static final Lab.Value BURST_SIZE = Lab.CLIENT.def("hydro.geyser.burst_size", 0.55, 0.2, 1.5, 0.05);
   private static final Lab.Value DISSOLVE = Lab.CLIENT.def("hydro.geyser.dissolve_ticks", 7.0, 3.0, 10.0, 1.0);
   private static final Lab.Value DRIFT = Lab.CLIENT.def("hydro.geyser.drift", 0.5, 0.0, 2.0, 0.05);
   private static final Lab.Value FALL_SHARE = Lab.CLIENT.def("hydro.geyser.fall_share", 0.3, 0.0, 1.0, 0.05);
   private static final Lab.Value FOAM = Lab.CLIENT.def("hydro.geyser.foam", 0.6, 0.0, 1.5, 0.05);
   private static final float MAX_LEAN = 4.0F;
   private static final float MAX_HEIGHT = 30.0F;
   private static final float OWN_VIEW_DROP = 1.4F;
   private static final float OWN_VIEW_ALPHA = 0.5F;
   private static final float SHELL_LIFT = 0.06F;
   private static final int RIM_TIPS = 8;
   private static final float TIP_RATE = 0.12F;
   private static final float TIP_DEG = 100.0F;
   private static final float TIP_GROW = 0.3F;
   private static final int SPILL_LIFE = 34;
   private static final float SPILL_SPIN = 16.0F;
   private static final float LOOSE_SPAN = 0.45F;
   private static final float FLASH = 0.6F;
   private static final int FALL_MAX = 64;
   private static final int SETTLE = 20;
   private static final float FALL_DRIFT = 0.08F;
   private static final float FALL_SPIN = 7.0F;
   private static final int LOOK_DOWN = 48;
   private static final float LANDED_SHARE = 0.4F;
   private static final int LANDED_SPLASH = 12;
   private static final float SPRAY_WHITE = 0.5F;
   private static final Map<Integer, Long> THROWN = new HashMap<>();
   private static final Map<Integer, GeyserJet.Frame> LAST = new HashMap<>();
   private static final Map<Integer, GeyserJet.Ending> HELD = new HashMap<>();

   private GeyserJet() {
   }

   static void draw(PoseStack ps, VfxDraw.Sink sink, VfxRenderState st, float life) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null) {
         if (st.secondary.y < 0.0) {
            collapse(ps, level, st);
         } else {
            live(ps, level, st);
         }
      }
   }

   private static void live(PoseStack ps, ClientLevel level, VfxRenderState st) {
      float age = st.ageInTicks;
      float pt = age - (float)Mth.floor(age);
      int fuse = Math.round((float)st.secondary.y);
      int erupt = Math.round((float)st.secondary.z);
      float k = thickness(st);
      int seed = st.seed;
      GeyserJet.Paints paints = GeyserJet.Paints.of(st, 1.0F);
      float cube = CUBE.getF();
      if (age < (float)fuse) {
         float u = age / Math.max(1.0F, (float)fuse);
         if (newTicks(level, seed) > 0 && Mth.floor(age) % 2 == 1) {
            WaterCubeSpray.burst(
               level,
               owner(seed),
               st.worldPos,
               Vec3.ZERO,
               WaterCubeSpray.Burst.of(2, 0.04F, cube * 0.4F).scattered(0.5F * k).lifted(0.12F + 0.1F * u).lasting(12),
               paints.spray()
            );
         }

         WaterFramePass.queue(ps, (pose, vc) -> rumble(ground(pose), vc, paints, u, age, k, cube, seed));
      } else {
         float t = age - (float)fuse;
         if (!(t > (float)erupt)) {
            GeyserJet.Top top = GeyserJet.Top.of(level.getEntity(VfxEntity.idFromSecondary(st.secondary.x)), st, pt, 2.0F + 2.5F * st.scale);
            boolean ownView = top.ownView();
            float h = Math.max(top.ty() + 0.06F, 1.3F * k * Math.min(1.0F, (t + 0.5F) / 1.5F));
            GeyserJet.Jet jet = new GeyserJet.Jet(h, top.lx(), top.lz(), k, cube, seed);
            remember(LAST, riderId(st.secondary.x), new GeyserJet.Frame(seed, age, jet, level.getGameTime()), level);
            int ticks = newTicks(level, seed);
            if (ticks > 0) {
               spill(level, st, jet, paints, age, ticks, ownView);
            }

            WaterFramePass.queue(ps, (pose, vc) -> {
               Pose g = ground(pose);
               jet.draw(g, vc, ownView ? paints.faint(0.5F) : paints, age, null);
               if (!ownView) {
                  jet.rimTips(g, vc, paints, age, 1.0F);
               }

               foot(g, vc, paints, jet.rBase(), age, seed, 1.0F);
            });
         }
      }
   }

   private static float thickness(VfxRenderState st) {
      return 0.85F + 0.25F * st.scale;
   }

   private static Pose ground(Pose pose) {
      Pose g = pose.copy();
      g.pose().translate(0.0F, -0.049999997F, 0.0F);
      return g;
   }

   private static String owner(int id) {
      return "geyser" + id;
   }

   private static void spill(ClientLevel level, VfxRenderState st, GeyserJet.Jet jet, GeyserJet.Paints p, float age, int ticks, boolean ownView) {
      Vector3f off = new Vector3f();
      jet.bend(jet.h(), off);
      Vec3 foot = st.worldPos.add(0.0, -0.06F, 0.0);
      Vec3 top = foot.add((double)off.x, (double)jet.h(), (double)off.z);
      WaterCubePool pool = WaterCubeSpray.pool(level, owner(st.seed), p.spray());
      float r = jet.rTop();
      float cube = jet.cube();
      RandomSource rnd = level.getRandom();
      if (!ownView) {
         float now = (float)Mth.floor(age);

         for (int i = 0; i < 8; i++) {
            if (Mth.floor(jet.tipBeat(i, now)) != Mth.floor(jet.tipBeat(i, now - (float)ticks))) {
               float a = jet.tipAngle(i);
               float out = 0.12F + 0.08F * rnd.nextFloat();
               float at = r + cube * 0.4F;
               pool.add(
                  level,
                  top.add((double)(Mth.cos(a) * at), 0.0, (double)(Mth.sin(a) * at)),
                  new Vec3((double)(Mth.cos(a) * out), 0.1F, (double)(Mth.sin(a) * out)),
                  WaterBlocks.yawOutward(a),
                  100.0F,
                  16.0F,
                  cube * 0.85F,
                  lifeAbove(top.y, 0.1F, foot.y + (double)cube),
                  (i & 1) == 0,
                  foot.y
               );
            }
         }
      }

      int n = SPRAY.getI() * Math.min(ticks, 2);

      for (int ix = 0; ix < n; ix++) {
         float a = rnd.nextFloat() * (float) (Math.PI * 2);
         float out = 0.12F + 0.14F * rnd.nextFloat();
         float lift = ownView ? -0.05F : 0.15F + 0.25F * rnd.nextFloat();
         float from = ownView ? r + 0.8F : r * (0.5F + 0.5F * rnd.nextFloat());
         pool.add(
            level,
            top.add((double)(Mth.cos(a) * from), 0.0, (double)(Mth.sin(a) * from)),
            new Vec3((double)(Mth.cos(a) * out), (double)lift, (double)(Mth.sin(a) * out)),
            WaterBlocks.yawOutward(a),
            0.0F,
            16.0F * (2.0F * rnd.nextFloat() - 1.0F),
            cube * (0.5F + 0.35F * rnd.nextFloat()),
            lifeAbove(top.y, lift, foot.y + (double)cube),
            rnd.nextFloat() < 0.4F,
            foot.y
         );
      }
   }

   private static int lifeAbove(double y, float vy, double floor) {
      return Math.max(4, Math.min(34, WaterCubePool.fallTicks(y, vy, floor)));
   }

   private static void foot(Pose pose, VertexConsumer vc, GeyserJet.Paints p, float rBase, float time, int seed, float left) {
      float cube = CUBE.getF();
      WaterBlocks.ring(pose, vc, p.water(), p.foam(), rBase * 1.6F, cube, cube * 0.7F * left, 0.08F * left, 0.35F, time * 3.0F, seed + 7);
   }

   private static void rumble(Pose pose, VertexConsumer vc, GeyserJet.Paints p, float u, float time, float k, float cube, int seed) {
      float beat = Math.abs(Mth.sin(time * (float) Math.PI / 2.0F));
      float rr = (0.45F + 0.7F * u) * k;
      float c = cube * 0.8F;
      WaterCubeGrid g = WaterBlocks.puddle(rr * 0.85F, c, c * 0.3F, 0.3F, seed);

      for (int kx = 0; kx < g.cols(); kx++) {
         for (int r = 0; r < g.rows(); r++) {
            float h0 = g.height(kx, r);
            if (!(h0 <= 0.0F)) {
               float x = g.x(kx);
               float z = g.z(r);
               float mid = 1.0F - Math.min(1.0F, Mth.sqrt(x * x + z * z) / Math.max(0.1F, rr));
               float churn = 0.5F + 0.5F * Mth.sin(time * 1.4F + (float) (Math.PI * 2) * WaterMesh.noise01(seed, kx * 31 + r, 61));
               g.set(kx, r, h0 + c * (0.2F + 1.2F * u * mid) * churn * (0.8F + 0.2F * beat));
               g.flash(kx, r, churn > 0.8F ? 0.6F : 0.0F);
            }
         }
      }

      g.drawWater(pose, vc, p.water(), false);
      WaterMesh.Paint sheen = p.water().sheen(p.water().opacity() * 0.3F);
      WaterFramePass.foam(pose, vc, (fp, fvc) -> g.drawFoam(fp, fvc, sheen, (kx, rx, h) -> 0.4F));
      WaterBlocks.ring(pose, vc, p.water(), p.foam(), rr, cube, cube * (0.3F + 0.9F * u) * (0.8F + 0.2F * beat), 0.1F, 0.4F, time * 3.0F, seed + 3);
   }

   private static void collapse(PoseStack ps, ClientLevel level, VfxRenderState st) {
      float age = st.ageInTicks;
      float dissolve = DISSOLVE.getF();
      float u = age / Math.max(1.0F, dissolve);
      if (!(u >= 1.0F)) {
         boolean ownView = ownView(level, st);
         GeyserJet.Paints paints = GeyserJet.Paints.of(st, 1.0F);
         GeyserJet.Ending end = HELD.get(st.seed);
         if (end == null) {
            end = begin(level, st, paints, ownView);
            remember(HELD, st.seed, end, level);
         }

         GeyserJet.Jet jet = end.frame.jet();
         float time = end.frame.time();
         int seed = end.frame.seed();
         int tick = Math.min(Mth.floor(age), DISSOLVE.getI());
         if (tick > end.shed) {
            shed(level, st, end, end.shed < 0 ? -1.0F : (float)end.shed / dissolve, (float)tick / dissolve, paints);
            end.shed = tick;
         }

         float thrown = (float)end.shed / dissolve;
         GeyserJet.Ending e = end;
         GeyserJet.Paints column = ownView ? paints.faint(0.5F) : paints;
         WaterBlocks.Loosen loosen = (ring, i, s) -> {
            float at = breakAt(seed, ring, i);
            if (e.falls(seed, ring, i)) {
               return at < thrown ? 1.0F : 0.0F;
            } else {
               return Mth.clamp((u - at) / 0.45F, 0.0F, 1.0F);
            }
         };
         float left = Mth.clamp(1.0F - u / 0.45F, 0.0F, 1.0F);
         float flash = u / 0.6F;
         WaterFramePass.queue(ps, (pose, vc) -> {
            Pose g = ground(pose);
            jet.draw(g, vc, column, time, loosen);
            if (!ownView && left > 0.0F) {
               jet.rimTips(g, vc, paints, time, left);
            }

            if (left > 0.0F) {
               foot(g, vc, paints, jet.rBase(), time, seed, left);
            }

            if (flash < 1.0F) {
               flash(g, vc, column, jet, time, flash);
            }
         });
      }
   }

   private static float breakAt(int seed, int ring, int i) {
      return 0.55F * WaterMesh.noise01(seed, ring * 31 + i, 71);
   }

   private static GeyserJet.Ending begin(ClientLevel level, VfxRenderState st, GeyserJet.Paints paints, boolean ownView) {
      GeyserJet.Frame held = stopped(level, st);
      GeyserJet.Jet jet = held.jet();
      int[] count = new int[]{0};
      WaterBlocks.columnCubes(jet.column(held.time()), jet.taper(held.time()), jet::bend, c -> count[0]++);
      float share = Math.min(FALL_SHARE.getF(), 64.0F / (float)Math.max(1, count[0]));
      WaterCubeSpray.pool(level, owner(held.seed()), paints.spray()).letLand(20);
      WaterCubePool pool = WaterCubeSpray.pool(level, owner(st.seed), paints.spray());
      burst(level, st, jet, pool, ownView);
      int falling = BURST.getI() + Math.round(share * (float)count[0]);
      pool.onLanded(Math.max(1, Math.round((float)falling * 0.4F)), at -> landed(level, st.worldPos, at.y, jet));
      return new GeyserJet.Ending(held, share);
   }

   private static void shed(ClientLevel level, VfxRenderState st, GeyserJet.Ending end, float from, float to, GeyserJet.Paints p) {
      GeyserJet.Jet jet = end.frame.jet();
      int seed = end.frame.seed();
      float time = end.frame.time();
      Vec3 foot = st.worldPos.add(0.0, -0.049999997F, 0.0);
      WaterCubePool pool = WaterCubeSpray.pool(level, owner(st.seed), p.spray());
      RandomSource rnd = level.getRandom();
      float drift = DRIFT.getF() * 0.08F;
      WaterBlocks.columnCubes(
         jet.column(time),
         jet.taper(time),
         jet::bend,
         c -> {
            float at = breakAt(seed, c.ring(), c.index());
            if (!(at < from) && !(at >= to) && end.falls(seed, c.ring(), c.index())) {
               Vector3f mid = c.center();
               Vec3 pos = foot.add((double)mid.x, (double)mid.y, (double)mid.z);
               float out = drift * (0.6F + 0.8F * rnd.nextFloat());
               pool.addFalling(
                  level,
                  pos,
                  new Vec3((double)(Mth.cos(c.angle()) * out), 0.0, (double)(Mth.sin(c.angle()) * out)),
                  WaterBlocks.yawOutward(c.angle()),
                  0.0F,
                  7.0F * (2.0F * rnd.nextFloat() - 1.0F),
                  c.hs() * 2.0F,
                  rnd.nextFloat() < 0.25F,
                  groundUnder(level, pos),
                  20
               );
            }
         }
      );
   }

   private static double groundUnder(ClientLevel level, Vec3 at) {
      return Terrain.floorNear(level, at.x, at.z, at.y, 1, 48, true);
   }

   private static void landed(ClientLevel level, Vec3 foot, double y, GeyserJet.Jet jet) {
      level.playLocalSound(foot.x, y, foot.z, SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.45F, 1.3F, false);
      RandomSource rnd = level.getRandom();
      float r = jet.rTop();

      for (int i = 0; i < 12; i++) {
         float a = rnd.nextFloat() * (float) (Math.PI * 2);
         float d = r * (0.3F + rnd.nextFloat());
         level.addParticle(ParticleTypes.SPLASH, foot.x + (double)(Mth.cos(a) * d), y + 0.1, foot.z + (double)(Mth.sin(a) * d), 0.0, 0.1, 0.0);
      }
   }

   private static int riderId(double packed) {
      return VfxEntity.idFromSecondary(packed);
   }

   private static boolean ownView(ClientLevel level, VfxRenderState st) {
      Entity rider = level.getEntity(riderId(st.secondary.z));
      return rider != null && HydroHands.ownFirstPerson(rider);
   }

   private static GeyserJet.Frame stopped(ClientLevel level, VfxRenderState st) {
      int rider = riderId(st.secondary.z);
      GeyserJet.Frame last = LAST.remove(rider);
      long now = level.getGameTime();
      if (last != null && now >= last.tick() && now - last.tick() <= 3L) {
         return last;
      } else {
         float age = st.ageInTicks;
         GeyserJet.Top top = GeyserJet.Top.of(level.getEntity(rider), st, age - (float)Mth.floor(age), (float)st.secondary.x - 0.06F);
         float k = thickness(st);
         return new GeyserJet.Frame(st.seed, 0.0F, new GeyserJet.Jet(Math.max(top.ty() + 0.06F, 1.3F * k), top.lx(), top.lz(), k, CUBE.getF(), st.seed), now);
      }
   }

   private static void burst(ClientLevel level, VfxRenderState st, GeyserJet.Jet jet, WaterCubePool pool, boolean ownView) {
      Vector3f off = new Vector3f();
      jet.bend(jet.h(), off);
      Vec3 top = st.worldPos.add((double)off.x, (double)(jet.h() - 0.06F), (double)off.z);
      RandomSource rnd = level.getRandom();
      int n = BURST.getI();
      float r = jet.rTop();
      float size = jet.cube() * BURST_SIZE.getF();

      for (int i = 0; i < n; i++) {
         float a = (float) (Math.PI * 2) * ((float)i + 0.6F * rnd.nextFloat()) / (float)Math.max(1, n);
         float out = 0.08F + 0.08F * rnd.nextFloat();
         float from = ownView ? r + 0.8F : r * (0.6F + 0.4F * rnd.nextFloat());
         Vec3 pos = top.add((double)(Mth.cos(a) * from), (double)(-jet.cube() * rnd.nextFloat()), (double)(Mth.sin(a) * from));
         pool.addFalling(
            level,
            pos,
            new Vec3((double)(Mth.cos(a) * out), 0.0, (double)(Mth.sin(a) * out)),
            WaterBlocks.yawOutward(a),
            360.0F * rnd.nextFloat(),
            16.0F * (2.0F * rnd.nextFloat() - 1.0F),
            size * (0.6F + 0.6F * rnd.nextFloat()),
            rnd.nextFloat() < 0.45F,
            groundUnder(level, pos),
            20
         );
      }
   }

   private static <T extends GeyserJet.Stamped> void remember(Map<Integer, T> map, int key, T frame, ClientLevel level) {
      if (map.size() > 32) {
         long now = level.getGameTime();
         map.values().removeIf(f -> now - f.tick() > 100L || now < f.tick());
      }

      map.put(key, frame);
   }

   private static void flash(Pose pose, VertexConsumer vc, GeyserJet.Paints p, GeyserJet.Jet jet, float time, float f) {
      Vector3f off = new Vector3f();
      jet.bend(jet.h(), off);
      Pose at = pose.copy();
      at.pose().translate(off.x, jet.h() - jet.cube() * 0.5F, off.z);
      float left = 1.0F - f;
      WaterMesh.Paint water = p.water().alpha(p.water().opacity() * left);
      WaterMesh.Paint foam = p.foam() == null ? null : p.foam().alpha(p.foam().opacity() * left);
      float cube = jet.cube() * (0.9F - 0.4F * f);
      WaterBlocks.ring(at, vc, water, foam, jet.rTop() * (1.0F + 1.6F * f), cube, cube * left, 0.05F, 1.0F, time * 3.0F, jet.seed() + 9);
   }

   private static int newTicks(ClientLevel level, int id) {
      long now = level.getGameTime();
      if (THROWN.size() > 32) {
         THROWN.values().removeIf(t -> now - t > 100L || now < t);
      }

      Long last = THROWN.put(id, now);
      return last == null ? 1 : (int)Mth.clamp(now - last, 0L, 4L);
   }

   private static final class Ending implements GeyserJet.Stamped {
      final GeyserJet.Frame frame;
      final float share;
      int shed = -1;

      Ending(GeyserJet.Frame frame, float share) {
         this.frame = frame;
         this.share = share;
      }

      boolean falls(int seed, int ring, int i) {
         return WaterMesh.noise01(seed, ring * 31 + i, 72) < this.share;
      }

      @Override
      public long tick() {
         return this.frame.tick();
      }
   }

   private static record Frame(int seed, float time, GeyserJet.Jet jet, long tick) implements GeyserJet.Stamped {
   }

   private static record Jet(float h, float lx, float lz, float k, float cube, int seed) {
      float rBase() {
         return GeyserJet.RADIUS.getF() * this.k;
      }

      float widen(float s) {
         s = Mth.clamp(s, 0.0F, 1.0F);
         float top = Math.max(GeyserJet.WIDEN.getF(), 0.06F * this.h / this.rBase());
         return Mth.lerp(s, 1.0F, top) + 0.6F * (float)Math.pow((double)(1.0F - s), 6.0);
      }

      void bend(float y, Vector3f out) {
         float s = y / Math.max(0.3F, this.h);
         out.set(this.lx * s * s, 0.0F, this.lz * s * s);
      }

      float rTop() {
         return this.rBase() * this.widen(1.0F);
      }

      void draw(Pose pose, VertexConsumer vc, GeyserJet.Paints p, float time, WaterBlocks.Loosen loosen) {
         if (!(this.h < 0.05F)) {
            WaterBlocks.Bend bent = this::bend;
            float drift = GeyserJet.DRIFT.getF();
            WaterBlocks.column(pose, vc, p.water(), p.foam(), this.column(time), this.taper(time), bent, loosen, drift);
            WaterBlocks.column(
               pose,
               vc,
               p.water(),
               loosen == null ? null : p.foam(),
               new WaterBlocks.Column(this.cube * 0.3F, this.h, this.cube, GeyserJet.RISE.getF() * 1.4F, 0.0F, 0.0F, time, this.seed + 1),
               null,
               bent,
               loosen,
               drift * 0.5F
            );
         }
      }

      WaterBlocks.Column column(float time) {
         return new WaterBlocks.Column(this.rBase(), this.h, this.cube, GeyserJet.RISE.getF(), GeyserJet.CHURN.getF(), 0.1F, time, this.seed);
      }

      WaterMesh.Spine.Radius taper(float time) {
         float full = Math.max(0.3F, this.h);
         float phase = WaterMesh.noise01(this.seed, 0, 5) * (float) (Math.PI * 2);
         return s -> {
            float y = s * this.h;
            return this.widen(y / full) * (1.0F + 0.12F * Mth.sin(y * 1.3F - time * 0.9F + phase));
         };
      }

      void rimTips(Pose pose, VertexConsumer vc, GeyserJet.Paints p, float time, float left) {
         PoseStack local = new PoseStack();
         local.last().pose().set(pose.pose());
         local.last().normal().set(pose.normal());
         float r = this.rTop();
         Vector3f off = new Vector3f();
         this.bend(this.h, off);

         for (int i = 0; i < 8; i++) {
            float a = this.tipAngle(i);
            float f = this.tipBeat(i, time);
            float ph = f - (float)Mth.floor(f);
            float size = this.cube * 0.9F * Math.min(1.0F, ph / 0.3F) * left;
            if (!(size < 0.03F)) {
               float tip = ph < 0.3F ? 0.0F : 100.0F * (ph - 0.3F) / 0.7F;
               local.pushPose();
               local.translate(off.x + Mth.cos(a) * r, this.h - this.cube * 0.5F, off.z + Mth.sin(a) * r);
               local.mulPose(Axis.YP.rotation((float) (Math.PI / 2) - a));
               WaterBlocks.tippedCube(local, vc, p.water(), (i & 1) == 0 ? p.foam() : null, 0.0F, 0.0F, 0.0F, size, tip);
               local.popPose();
            }
         }
      }

      float tipAngle(int i) {
         return (float) (Math.PI * 2) * ((float)i + 0.5F * WaterMesh.noise01(this.seed, i, 51)) / 8.0F;
      }

      float tipBeat(int i, float time) {
         return time * 0.12F * (0.8F + 0.4F * WaterMesh.noise01(this.seed, i, 52)) + WaterMesh.noise01(this.seed, i, 53);
      }
   }

   private static record Paints(WaterMesh.Paint water, WaterMesh.Paint foam, WaterCubeSpray.Look spray) {
      GeyserJet.Paints faint(float share) {
         return new GeyserJet.Paints(this.water.alpha(this.water.opacity() * share), null, this.spray);
      }

      static GeyserJet.Paints of(VfxRenderState st, float fade) {
         float alpha = Math.min(1.0F, 0.8F * st.intensity) * fade;
         WaterMesh.Paint water = WaterMesh.Paint.of(st.color, alpha, st.lightCoords);
         float foam = GeyserJet.FOAM.getF();
         return new GeyserJet.Paints(water, water.foam(alpha * foam), new WaterCubeSpray.Look(st.color, alpha, alpha * foam * 0.5F));
      }
   }

   private interface Stamped {
      long tick();
   }

   private static record Top(float lx, float lz, float ty, boolean ownView) {
      static GeyserJet.Top of(Entity rider, VfxRenderState st, float pt, float fallback) {
         if (rider == null) {
            return new GeyserJet.Top(0.0F, 0.0F, fallback, false);
         } else {
            boolean ownView = HydroHands.ownFirstPerson(rider);
            Vec3 rel = rider.getPosition(pt).subtract(st.worldPos);
            float lean = (float)Math.sqrt(rel.x * rel.x + rel.z * rel.z);
            float cut = lean > 4.0F ? 4.0F / lean : 1.0F;
            return new GeyserJet.Top((float)rel.x * cut, (float)rel.z * cut, (float)Math.min(30.0, rel.y) - (ownView ? 1.4F : 0.0F), ownView);
         }
      }
   }
}
