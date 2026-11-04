package com.devilfruits.core.gametest;

import com.devilfruits.core.limb.LimbBands;
import com.devilfruits.core.limb.LimbFill;
import com.devilfruits.core.limb.LimbPath;
import com.devilfruits.core.limb.LimbUv;
import java.util.function.IntUnaryOperator;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Vector3f;

@GameTestHolder("devilfruits")
@PrefixGameTestTemplate(false)
public final class LimbGameTests {
   private static final String PLATFORM = "platform";
   private static final float EPS = 0.001F;

   @GameTest(
      template = "platform"
   )
   public static void limbUvResolver(GameTestHelper h) {
      LimbUv.Layout arm = LimbUv.resolve(LimbUv.Model.CLASSIC, LimbUv.Side.RIGHT, LimbUv.Limb.ARM, 64, 64, LimbUv.Layer.SKIN);
      check(arm.u() == 40 && arm.v() == 16 && arm.width() == 4 && arm.depth() == 4 && !arm.mirror() && arm.grow() == 0.0F, "classic right arm box");
      check(arm.minX() == -3.0F && arm.minY() == -2.0F && arm.pivotX() == -5.0F && arm.pivotY() == 2.0F, "classic right arm pivot");
      check(arm.sideU(1, 1) == 44.0F && arm.sideU(1, 0) == 48.0F && arm.sideU(0, 1) == 40.0F && arm.sideU(3, 0) == 56.0F, "classic right arm faces");
      check(arm.sideTop() == 20.0F && near(arm.u(44.0F), 0.6875F) && near(arm.v(20.0F), 0.3125F), "classic right arm rows");
      float[] cap = new float[16];
      arm.cap(true, cap);
      check(cap[0] == 1.0F && cap[1] == 1.0F && cap[2] == 48.0F && cap[3] == 16.0F, "shoulder cap corner");
      arm.cap(false, cap);
      check(cap[0] == 1.0F && cap[1] == -1.0F && cap[2] == 52.0F && cap[3] == 20.0F, "hand cap corner");
      LimbUv.Layout slim = LimbUv.resolve(LimbUv.Model.SLIM, LimbUv.Side.LEFT, LimbUv.Limb.ARM, 128, 128, LimbUv.Layer.OVERLAY);
      check(slim.u() == 48 && slim.v() == 48 && slim.width() == 3 && slim.grow() == 0.25F && slim.pivotY() == 2.5F, "slim left sleeve box");
      check(slim.px(48.0F) == 96.0F && slim.bandRows() == 8.0F && near(slim.u(48.0F), 0.75F), "128x128 scale");
      check(LimbUv.resolve(LimbUv.Model.SLIM, LimbUv.Side.RIGHT, LimbUv.Limb.ARM, 64, 64, LimbUv.Layer.SKIN).minX() == -2.0F, "slim right arm box");
      LimbUv.Layout legacy = LimbUv.resolve(LimbUv.Model.CLASSIC, LimbUv.Side.LEFT, LimbUv.Limb.ARM, 64, 32, LimbUv.Layer.SKIN);
      check(legacy.u() == 40 && legacy.v() == 16 && legacy.mirror() && near(legacy.v(16.0F), 0.5F), "legacy left arm mirrors the right");
      check(LimbUv.resolve(LimbUv.Model.CLASSIC, LimbUv.Side.RIGHT, LimbUv.Limb.ARM, 64, 32, LimbUv.Layer.OVERLAY) == null, "legacy skins have no sleeves");
      LimbUv.Layout leg = LimbUv.resolve(LimbUv.Model.SLIM, LimbUv.Side.RIGHT, LimbUv.Limb.LEG, 64, 64, LimbUv.Layer.SKIN);
      check(leg.u() == 0 && leg.v() == 16 && leg.width() == 4 && leg.minY() == 0.0F && leg.pivotX() == -1.9F && leg.pivotY() == 12.0F, "right leg");
      LimbUv.Layout pants = LimbUv.resolve(LimbUv.Model.CLASSIC, LimbUv.Side.LEFT, LimbUv.Limb.LEG, 64, 64, LimbUv.Layer.OVERLAY);
      check(
         pants.u() == 0 && pants.v() == 48 && LimbUv.resolve(LimbUv.Model.CLASSIC, LimbUv.Side.LEFT, LimbUv.Limb.LEG, 64, 64, LimbUv.Layer.SKIN).u() == 16,
         "left leg layers"
      );
      h.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void limbBandsTexelDensity(GameTestHelper h) {
      float[] seg = new float[LimbBands.capacity(1024.0F)];
      int n = LimbBands.build(12.0F, 1.0F, 1.0F, seg);
      check(
         n == 4 && LimbBands.length(seg, n) == 12.0F && seg[4] == 4.0F && seg[6] == 4.0F && seg[7] == 8.0F, "the vanilla 12 px arm: shoulder, one tile, hand"
      );
      n = LimbBands.build(160.0F, 1.0F, 1.0F, seg);
      check(n == 41 && LimbBands.length(seg, n) == 160.0F, "10 model blocks: 38 middle tiles");
      n = LimbBands.build(13.3F, 1.0F, 1.0F, seg);
      check(LimbBands.length(seg, n) == 13.0F, "the last tile is cut at a texel row");
      n = LimbBands.build(13.6F, 2.0F, 1.0F, seg);
      check(LimbBands.length(seg, n) == 13.5F, "a 128x128 skin cuts at its half-pixel rows");
      n = LimbBands.build(341.3F, 1.0F, 1.25F, seg);
      float rows = 0.0F;

      for (int i = 0; i < n; i++) {
         float s0 = seg[i * 4];
         float s1 = seg[i * 4 + 1];
         float r0 = seg[i * 4 + 2];
         float r1 = seg[i * 4 + 3];
         boolean hand = r0 >= 8.0F;
         check(near((r1 - r0) / (s1 - s0), hand ? 0.8F : 1.0F), "texel density of segment " + i);
         check(i == 0 || seg[i * 4] == seg[i * 4 - 3], "segments join at " + i);
         if (!hand) {
            rows += r1 - r0;
         }
      }

      check(near(rows, LimbBands.length(seg, n) - 5.0F), "shoulder and middle bands: one row per pixel");
      h.succeed();
   }

   @GameTest(
      template = "platform"
   )
   public static void limbFillRows(GameTestHelper h) {
      LimbUv.Layout arm = LimbUv.resolve(LimbUv.Model.CLASSIC, LimbUv.Side.RIGHT, LimbUv.Limb.ARM, 64, 64, LimbUv.Layer.SKIN);
      int skin = -9662522;
      int cloth = -8500181;
      int clear = 0;
      check(LimbFill.firstRow(band(arm, r -> skin), 64, 64, arm) == 4, "uniform band");
      check(LimbFill.firstRow(band(arm, r -> r % 2 == 0 ? skin : cloth), 64, 64, arm) == 4, "striped sleeve");
      check(LimbFill.firstRow(band(arm, r -> r == 0 ? cloth : clear), 64, 64, arm) == 5, "a sleeve's trim on row 4");
      check(LimbFill.firstRow(band(arm, r -> r == 1 ? cloth : skin), 64, 64, arm) == 6, "a bracelet on row 5");
      check(LimbFill.firstRow(band(arm, r -> r < 3 ? cloth : skin), 64, 64, arm) == 7, "shorts ending on row 7");
      float[] seg = new float[LimbBands.capacity(1024.0F)];
      int n = LimbBands.build(12.0F, 1.0F, 1.0F, 6, seg);

      for (int i = 0; i < n && seg[i * 4 + 2] < 8.0F; i++) {
         check(seg[i * 4] == seg[i * 4 + 2] && seg[i * 4 + 1] == seg[i * 4 + 3], "a 12 px limb is the vanilla one whatever the fill (" + i + ")");
      }

      n = LimbBands.build(160.0F, 1.0F, 1.0F, 6, seg);
      check(n == 79 && seg[6] == 4.0F && seg[7] == 6.0F && seg[10] == 6.0F && seg[11] == 8.0F, "rows 4-6 once, then 2-row tiles");
      n = LimbBands.build(100.0F, 1.0F, 1.0F, 7, seg);
      check(n == 5 && seg[10] == 7.5F && seg[11] == 7.5F && seg[9] == 96.0F, "one repeated row is one stretch of constant V");
      n = LimbBands.build(100.0F, 2.0F, 1.0F, 7, seg);
      check(n > 80 && seg[10] == 7.0F && seg[11] == 8.0F, "on a 128x128 skin one row is two texture rows: tiled");
      h.succeed();
   }

   private static int[] band(LimbUv.Layout l, IntUnaryOperator row) {
      int[] px = new int[4096];

      for (int r = 0; r < 4; r++) {
         int y = (int)l.sideTop() + 4 + r;

         for (int x = l.u(); x < l.u() + 2 * l.depth() + 2 * l.width(); x++) {
            px[y * 64 + x] = row.applyAsInt(r);
         }
      }

      return px;
   }

   @GameTest(
      template = "platform"
   )
   public static void limbPathLength(GameTestHelper h) {
      LimbPath line = new LimbPath().add(0.0F, 0.0F, 0.0F).add(0.0F, 160.0F, 0.0F).build();
      Vector3f p = new Vector3f();
      Vector3f t = new Vector3f();
      line.sample(80.0F, p, t);
      check(Math.abs(line.length() - 160.0F) < 0.01F && p.distance(0.0F, 80.0F, 0.0F) < 0.01F && t.distance(0.0F, 1.0F, 0.0F) < 1.0E-4F, "straight path");
      LimbPath arc = new LimbPath();
      float r = 64.0F;

      for (int i = 0; i <= 6; i++) {
         double a = (Math.PI / 2) * (double)i / 6.0;
         arc.add((float)((double)r * Math.cos(a)), (float)((double)r * Math.sin(a)), 0.0F);
      }

      arc.build();
      check(Math.abs((double)arc.length() - Math.PI * (double)r / 2.0) < Math.PI * (double)r / 2.0 * 0.01, "quarter circle length " + arc.length());
      LimbPath bent = new LimbPath().add(0.0F, 0.0F, 0.0F).add(0.0F, 100.0F, 0.0F).startDirection(1.0F, 0.0F, 0.0F).build();
      bent.sample(0.0F, p, t);
      check(t.distance(1.0F, 0.0F, 0.0F) < 0.001F && bent.length() > 100.0F, "forced start direction");
      Vector3f p0 = new Vector3f();
      Vector3f t0 = new Vector3f();
      Vector3f n0 = new Vector3f(0.0F, 0.0F, 1.0F);
      Vector3f n1 = new Vector3f();
      arc.sample(0.0F, p0, t0);

      for (float s = 2.0F; s <= arc.length(); s += 2.0F) {
         arc.sample(s, p, t);
         LimbPath.transport(p0, t0, n0, p, t, n1);
         check(n1.distance(0.0F, 0.0F, 1.0F) < 0.001F && Math.abs(n1.dot(t)) < 0.001F, "rotation-minimising frame at " + s);
         p0.set(p);
         t0.set(t);
         n0.set(n1);
      }

      h.succeed();
   }

   private static boolean near(float a, float b) {
      return Math.abs(a - b) < 0.001F;
   }

   private static void check(boolean ok, String what) {
      if (!ok) {
         throw new GameTestAssertException("limb engine: " + what);
      }
   }

   private LimbGameTests() {
   }
}
