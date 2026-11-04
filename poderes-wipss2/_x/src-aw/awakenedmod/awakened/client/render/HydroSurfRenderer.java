package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.client.HydroSurfMotion;
import com.awakenedmod.awakened.content.hydro.HydroVisual;
import com.awakenedmod.awakened.core.Lab;
import com.awakenedmod.awakened.core.Terrain;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;

public final class HydroSurfRenderer {
   private static final Lab.Value TOP_SPEED = Lab.CLIENT.def("hydro.surf.top_speed", 1.05, 0.1, 2.5, 0.01);
   private static final float MAX_BASE = 1.6F;
   private static final Lab.Value CUBE = Lab.CLIENT.def("hydro.surf.cube", 0.5, 0.2, 1.0, 0.05);
   private static final Lab.Value CREST_ABOVE = Lab.CLIENT.def("hydro.surf.crest_above", 0.5, -1.0, 2.0, 0.05);
   private static final Lab.Value CREST_BEHIND = Lab.CLIENT.def("hydro.surf.crest_behind", 1.5, -2.0, 3.0, 0.05);
   private static final Lab.Value WIDTH = Lab.CLIENT.def("hydro.surf.width", 1.3, 0.3, 5.0, 0.05);
   private static final Lab.Value WIDTH_SPEED = Lab.CLIENT.def("hydro.surf.width_speed", 0.5, 0.0, 4.0, 0.05);
   private static final Lab.Value LENGTH = Lab.CLIENT.def("hydro.surf.length", 0.9, 0.3, 6.0, 0.05);
   private static final Lab.Value LENGTH_SPEED = Lab.CLIENT.def("hydro.surf.length_speed", 0.8, 0.0, 6.0, 0.05);
   private static final Lab.Value FRONT = Lab.CLIENT.def("hydro.surf.front", 0.9, 0.2, 3.0, 0.05);
   private static final Lab.Value CHURN = Lab.CLIENT.def("hydro.surf.churn", 0.3, 0.0, 1.5, 0.02);
   private static final Lab.Value CHURN_BASE = Lab.CLIENT.def("hydro.surf.churn_base", 0.04, 0.0, 0.5, 0.01);
   private static final Lab.Value BOIL = Lab.CLIENT.def("hydro.surf.boil", 0.6, 0.0, 2.0, 0.05);
   private static final Lab.Value BOIL_RATE = Lab.CLIENT.def("hydro.surf.boil_rate", 0.3, 0.0, 1.5, 0.02);
   private static final Lab.Value SNAP = Lab.CLIENT.def("hydro.surf.snap", 3.0, 1.0, 10.0, 0.25);
   private static final Lab.Value BREAK_RATE = Lab.CLIENT.def("hydro.surf.break_rate", 0.5, 0.0, 4.0, 0.05);
   private static final Lab.Value BREAK_RATE_SPEED = Lab.CLIENT.def("hydro.surf.break_rate_speed", 1.0, 0.0, 4.0, 0.05);
   private static final Lab.Value LIP = Lab.CLIENT.def("hydro.surf.lip", -0.5, -1.0, 2.0, 0.05);
   private static final Lab.Value BANK = Lab.CLIENT.def("hydro.surf.bank", 0.45, 0.0, 1.5, 0.05);
   private static final Lab.Value FOAM = Lab.CLIENT.def("hydro.surf.foam", 0.6, 0.0, 1.5, 0.05);
   private static final Lab.Value WAKE_RATE = Lab.CLIENT.def("hydro.surf.wake_rate", 2.0, 0.0, 8.0, 0.1);
   private static final Lab.Value WAKE_LIFE = Lab.CLIENT.def("hydro.surf.wake_life", 22.0, 4.0, 80.0, 1.0);
   private static final Lab.Value WAKE_SPREAD = Lab.CLIENT.def("hydro.surf.wake_spread", 0.05, 0.0, 0.3, 0.01);
   private static final int LOOK_DOWN = 16;
   private static final float POUR_NO_GROUND = 5.0F;
   private static final float CURTAIN_FALL = 0.35F;
   private static final float WAKE_MAX_GAP = 2.5F;
   private static final int SPRAY_LIFE = 16;
   private static final float SPRAY_SPIN = 14.0F;
   private static final float SPRAY_WHITE = 0.5F;
   private static final int FORGET_TICKS = 100;
   private static final Map<Integer, HydroSurfRenderer.Ride> RIDES = new HashMap<>();

   private HydroSurfRenderer() {
   }

   public static void register(IEventBus gameBus) {
      WaterFramePass.register(gameBus, HydroSurfRenderer::draw);
   }

   private static void draw(ClientLevel level, Player p, float pt, PoseStack ps, MultiBufferSource buffers, Vec3 cam) {
      HydroVisual visual = HydroVisual.of(p);
      boolean surfing = visual.surfing();
      HydroSurfRenderer.Ride ride = RIDES.get(p.getId());
      if (ride == null) {
         if (!surfing) {
            return;
         }

         ride = new HydroSurfRenderer.Ride();
         RIDES.put(p.getId(), ride);
      }

      ride.seen = level.getGameTime();
      forgetStale(level);
      WaterWave.Look look = HydroLook.body(visual.wet());
      Vec3 feet = p.getPosition(pt);
      int light = WaterBlocks.lightAbove(level, feet.x, feet.y, feet.z);
      WaterMesh.Paint water = WaterMesh.Paint.of(look.rgb(), Math.min(1.0F, look.alpha()), light);
      WaterMesh.Paint foam = water.foam(water.opacity() * FOAM.getF());
      WaterMesh.Paint spray = water.foam(water.opacity() * FOAM.getF() * 0.5F);
      if (!surfing) {
         step(ride, p, level, null);
         ride.cubes.draw(ps, buffers, cam, pt, water, spray);
         if (ride.cubes.isEmpty()) {
            RIDES.remove(p.getId());
         }
      } else {
         double dx = p.getX() - p.xo;
         double dz = p.getZ() - p.zo;
         float rush = Mth.clamp((float)Math.sqrt(dx * dx + dz * dz) / TOP_SPEED.getF(), 0.0F, 1.0F);
         double surface = Terrain.floorNear(level, feet.x, feet.z, feet.y, 1, 16, true);
         boolean grounded = !Double.isNaN(surface);
         float gap = grounded ? (float)(feet.y - surface) : Float.NaN;
         float base = grounded ? Mth.clamp(gap, 0.05F, 1.6F) : 1.0F;
         float pourTo = grounded ? gap : base + 5.0F;
         boolean pour = pourTo > base + 0.1F;
         boolean floating = !grounded || gap > 1.6F;
         SurfBlockWave.Shape shape = new SurfBlockWave.Shape(
            CUBE.getF(),
            base + CREST_ABOVE.getF() + HydroSurfMotion.bob(p, pt),
            base + HydroSurfMotion.bob(p, pt),
            base,
            -CREST_BEHIND.getF(),
            FRONT.getF(),
            LENGTH.getF() + LENGTH_SPEED.getF() * rush,
            LENGTH.getF() + LENGTH_SPEED.getF(),
            WIDTH.getF() + WIDTH_SPEED.getF() * rush,
            WIDTH.getF() + WIDTH_SPEED.getF(),
            Mth.lerp(pt, ride.scrollO, ride.scroll),
            BANK.getF() * HydroSurfMotion.carve(p, pt),
            BOIL.getF(),
            BOIL_RATE.getF(),
            SNAP.getF(),
            LIP.getF(),
            Mth.lerp(pt, ride.beatO, ride.beat),
            (float)p.tickCount + pt,
            p.getId() * 7 + 3
         );
         step(ride, p, level, new HydroSurfRenderer.Tick(shape, rush, grounded && gap <= 2.5F ? surface : Double.NaN, grounded ? surface : Double.NaN));
         ps.pushPose();
         ps.translate(feet.x - cam.x, feet.y - cam.y, feet.z - cam.z);
         ps.mulPose(Axis.YP.rotationDegrees(-p.getViewYRot(pt)));
         WaterCubeGrid pile = SurfBlockWave.build(shape);
         WaterMesh.submit(ps, buffers, (pose, vc) -> {
            pile.drawWater(pose, vc, water, look.deepRgb(), look.crestRgb(), shape.height(), floating);
            WaterFramePass.foam(pose, vc, (fp, fvc) -> SurfBlockWave.drawFoam(fp, fvc, shape, pile, foam));
            if (pour) {
               SurfBlockWave.drawCurtain(pose, vc, shape, water, -base, -pourTo, 0.35F, 0.3F + 0.6F * rush, grounded);
            }
         });
         SurfBlockWave.drawLips(ps, buffers.getBuffer(ModRenderTypes.WATER_TRANSLUCENT), shape, water);
         ps.popPose();
         ride.cubes.draw(ps, buffers, cam, pt, water, spray);
      }
   }

   private static void step(HydroSurfRenderer.Ride ride, Player p, ClientLevel level, HydroSurfRenderer.Tick tick) {
      if (ride.tick != p.tickCount) {
         int steps = ride.tick == Integer.MIN_VALUE ? 1 : Mth.clamp(p.tickCount - ride.tick, 1, 4);
         ride.tick = p.tickCount;

         for (int i = 0; i < steps; i++) {
            ride.scrollO = ride.scroll;
            ride.beatO = ride.beat;
            if (tick != null) {
               float rush = tick.rush();
               ride.scroll = ride.scroll + CHURN_BASE.getF() + CHURN.getF() * rush * TOP_SPEED.getF();
               float beat = ride.beat + (BREAK_RATE.getF() + BREAK_RATE_SPEED.getF() * rush) / 20.0F;
               breakLips(ride, p, level, tick, ride.beat, beat);
               ride.beat = beat;
               shed(ride, p, level, tick);
            }

            ride.cubes.tick();
         }
      }
   }

   private static void breakLips(HydroSurfRenderer.Ride ride, Player p, ClientLevel level, HydroSurfRenderer.Tick tick, float from, float to) {
      SurfBlockWave.Shape s = tick.shape();
      HydroSurfRenderer.Frame f = new HydroSurfRenderer.Frame(p);

      for (int k = 0; k < s.cols(); k++) {
         if (SurfBlockWave.hasLip(s, k) && WaterBlocks.lipBroke(from, to, s.seed(), k)) {
            float[] pv = SurfBlockWave.lipPivot(s, k);
            float size = SurfBlockWave.lipSize(s, 1.0F);
            Vec3 at = f.world(pv[0], pv[1] + size * 0.4F, pv[2] + size * 0.3F);
            float jitter = WaterMesh.noise01(s.seed(), k, Mth.floor(to)) - 0.5F;
            Vec3 vel = motion(p).multiply(0.9, 0.0, 0.9).add(f.dir(jitter * 0.12F, 0.12F + 0.1F * tick.rush(), 0.1F + 0.06F * tick.rush()));
            ride.cubes.add(level, at, vel, p.getYRot(), 100.0F, 14.0F, size * 0.8F, 16, (k & 1) == 0, tick.ground());
         }
      }
   }

   private static void shed(HydroSurfRenderer.Ride ride, Player p, ClientLevel level, HydroSurfRenderer.Tick tick) {
      if (Double.isNaN(tick.wakeGround())) {
         ride.wakeDebt = 0.0F;
      } else {
         SurfBlockWave.Shape s = tick.shape();
         ride.wakeDebt = ride.wakeDebt + WAKE_RATE.getF() * (0.25F + 0.75F * tick.rush());
         HydroSurfRenderer.Frame f = new HydroSurfRenderer.Frame(p);
         int life = WAKE_LIFE.getI();

         while (ride.wakeDebt >= 1.0F) {
            ride.wakeDebt--;
            int i = ++ride.shed;
            float u = WaterMesh.noise01(s.seed(), i, 3);
            float v = WaterMesh.noise01(s.seed(), i, 5);
            float z = s.crestZ() - 0.45F - s.back() * (0.2F + 0.6F * u);
            float x = (v * 2.0F - 1.0F) * s.halfWidth();
            float height = s.heightAt(x, z);
            float size = s.cube() * (0.55F + 0.35F * WaterMesh.noise01(s.seed(), i, 9));
            Vec3 at = f.world(x, -s.base() + Math.max(size * 0.5F, height - size * 0.5F), z);
            Vec3 vel = motion(p).multiply(0.2, 0.0, 0.2).add(f.dir(Math.signum(x) * WAKE_SPREAD.getF(), 0.06F, 0.0F));
            ride.cubes.add(level, at, vel, p.getYRot(), 0.0F, 0.0F, size, life, i % 3 == 0, tick.wakeGround());
         }
      }
   }

   private static Vec3 motion(Player p) {
      return new Vec3(p.getX() - p.xo, p.getY() - p.yo, p.getZ() - p.zo);
   }

   private static void forgetStale(ClientLevel level) {
      if (RIDES.size() > 1) {
         long now = level.getGameTime();
         RIDES.values().removeIf(r -> now - r.seen > 100L);
      }
   }

   private static record Frame(Vec3 feet, float sin, float cos) {
      Frame(Player p) {
         this(p.position(), Mth.sin(p.getYRot() * (float) (Math.PI / 180.0)), Mth.cos(p.getYRot() * (float) (Math.PI / 180.0)));
      }

      Vec3 dir(float x, float y, float z) {
         return new Vec3((double)(x * this.cos - z * this.sin), (double)y, (double)(x * this.sin + z * this.cos));
      }

      Vec3 world(float x, float y, float z) {
         return this.feet.add(this.dir(x, y, z));
      }
   }

   private static final class Ride {
      int tick = Integer.MIN_VALUE;
      float scroll;
      float scrollO;
      float beat;
      float beatO;
      float wakeDebt;
      int shed;
      long seen;
      final WaterCubePool cubes = new WaterCubePool();
   }

   private static record Tick(SurfBlockWave.Shape shape, float rush, double wakeGround, double ground) {
   }
}
