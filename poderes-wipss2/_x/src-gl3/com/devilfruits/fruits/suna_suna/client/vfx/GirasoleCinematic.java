package com.devilfruits.fruits.suna_suna.client.vfx;

import com.devilfruits.client.render.vfx.VfxManager;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.render.MeshTex;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class GirasoleCinematic extends SandCinematic {
   private static final int ARMS = 6;
   private static final int STEPS = 18;
   private static final float TIGHT = 2.2F;
   private static final float SPIN = 0.05F;
   private final Vec3 centre;
   private final float full;
   private float shown;
   private float lastShown;
   private float throat;
   private boolean closing;
   private int closedAt = -1;
   private final double[] px = new double[18];
   private final double[] py = new double[18];
   private final double[] pz = new double[18];
   private final float[] ax = new float[18];
   private final float[] ay = new float[18];
   private final float[] az = new float[18];
   private final float[] w = new float[18];
   private final float[] a = new float[18];
   private final float[] bright = new float[18];

   public GirasoleCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      this.centre = params.pos();
      this.full = Math.max(1.0F, params.power());
      this.throat = (float)params.dir().y;
   }

   @Override
   protected boolean followsCaster() {
      return false;
   }

   @Nullable
   private SandFunnelEffect funnel() {
      return VfxManager.find(SandFunnelEffect.class, e -> e.seed == this.seed);
   }

   public static void close(long seed, ClientLevel level) {
      GirasoleCinematic c = VfxDirector.find(GirasoleCinematic.class, g -> g.seed == seed);
      if (c != null && c.closedAt < 0) {
         c.closedAt = c.age;
         c.collapse(level);
      }
   }

   private void collapse(ClientLevel level) {
      int tint = lit(level, this.centre.x, this.centre.y + 0.5, this.centre.z);

      for (int i = 0; i < VfxSettings.count(10.0F); i++) {
         double ang = this.random.nextDouble() * (float) (Math.PI * 2);
         double d = this.random.nextDouble() * Math.max(1.0, (double)this.throat);
         plume(
               this.random,
               this.centre.x + Math.cos(ang) * d,
               this.centre.y + 0.2,
               this.centre.z + Math.sin(ang) * d,
               Math.cos(ang) * 0.08,
               0.06 + this.random.nextDouble() * 0.05,
               Math.sin(ang) * 0.08,
               tint
            )
            .size(1.1F);
      }

      burst(this.random, this.centre.add(0.0, 0.2, 0.0), new Vec3(0.0, 1.0, 0.0), 0.6F, 0.3F, 20.0F, 4.0F, tint);
      clouds(this.random, this.centre.x, this.centre.y + 0.5, this.centre.z, 0.0, 0.03, 0.0, 2.2F, 6.0F, tint);
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      SandFunnelEffect funnel = this.funnel();
      if (funnel != null && (this.closedAt < 0 || this.age - this.closedAt <= 20)) {
         this.lastShown = this.shown;
         this.shown = funnel.shown();
         this.throat = Math.min(this.throat, this.shown);
         this.closing = funnel.closing();
         if (!(this.shown < 0.5F)) {
            int tint = lit(level, this.centre.x, this.centre.y + 0.5, this.centre.z);
            float k = this.shown / this.full;
            int n = VfxSettings.count(this.closing ? 1.0F : 2.5F + 3.5F * k);

            for (int i = 0; i < n; i++) {
               double ang = this.random.nextDouble() * (float) (Math.PI * 2);
               double d = (double)this.throat + this.random.nextDouble() * (double)(this.shown - this.throat);
               double sx = this.centre.x + Math.cos(ang) * d;
               double sz = this.centre.z + Math.sin(ang) * d;
               double tang = 0.1 + 0.12 * (1.0 - d / (double)this.shown);
               plume(
                     this.random,
                     sx,
                     this.ground(funnel, sx, sz) + 0.25 + this.random.nextDouble() * 0.5,
                     sz,
                     -Math.sin(ang) * tang - Math.cos(ang) * 0.05,
                     0.005,
                     Math.cos(ang) * tang - Math.sin(ang) * 0.05,
                     tint
                  )
                  .size(0.9F + 0.6F * (float)(d / (double)this.shown))
                  .alpha(0.75F);
            }

            int g = VfxSettings.count(this.closing ? 1.0F : 3.0F + 3.0F * k);

            for (int i = 0; i < g; i++) {
               double ang = this.random.nextDouble() * (float) (Math.PI * 2);
               double d = (double)this.throat + this.random.nextDouble() * (double)(this.shown - this.throat);
               double sx = this.centre.x + Math.cos(ang) * d;
               double sz = this.centre.z + Math.sin(ang) * d;
               grains(
                     this.random,
                     sx,
                     this.ground(funnel, sx, sz) + 0.08,
                     sz,
                     -Math.cos(ang) * 0.1 - Math.sin(ang) * 0.08,
                     0.02,
                     -Math.sin(ang) * 0.1 + Math.cos(ang) * 0.08,
                     tint
                  )
                  .life(0.6F);
            }

            if (!this.closing && this.throat > 0.3F) {
               for (int i = 0; i < VfxSettings.count(2.0F); i++) {
                  double ang = this.random.nextDouble() * (float) (Math.PI * 2);
                  double d = (double)this.throat * (0.4 + this.random.nextDouble() * 0.6);
                  puff(
                        this.random,
                        this.centre.x + Math.cos(ang) * d,
                        this.centre.y + 0.15,
                        this.centre.z + Math.sin(ang) * d,
                        -Math.sin(ang) * 0.15,
                        -0.03,
                        Math.cos(ang) * 0.15,
                        tint
                     )
                     .size(0.9F);
               }
            }

            boolean spreading = this.shown > this.lastShown + 0.05F && !this.closing;
            int rim = VfxSettings.count(spreading ? 3.0F + this.shown * 0.6F : 0.8F);

            for (int i = 0; i < rim; i++) {
               double ang = this.random.nextDouble() * (float) (Math.PI * 2);
               double sx = this.centre.x + Math.cos(ang) * (double)this.shown;
               double sz = this.centre.z + Math.sin(ang) * (double)this.shown;
               plume(
                     this.random,
                     sx,
                     this.ground(funnel, sx, sz) + 0.2,
                     sz,
                     -Math.cos(ang) * 0.05 - Math.sin(ang) * 0.04,
                     spreading ? 0.06 : 0.02,
                     -Math.sin(ang) * 0.05 + Math.cos(ang) * 0.04,
                     tint
                  )
                  .size(spreading ? 1.3F : 1.0F);
               if (spreading && i % 2 == 0) {
                  grains(
                     this.random,
                     sx,
                     this.ground(funnel, sx, sz) + 0.1,
                     sz,
                     Math.cos(ang) * 0.06,
                     0.15 + this.random.nextDouble() * 0.1,
                     Math.sin(ang) * 0.06,
                     tint
                  );
               }
            }
         }
      } else {
         this.finish();
      }
   }

   private double ground(SandFunnelEffect funnel, double wx, double wz) {
      float s = funnel.surfaceAt(wx, wz);
      return Float.isNaN(s) ? this.centre.y : (double)s;
   }

   @Override
   public void render(VfxFrame f) {
      SandFunnelEffect funnel = this.funnel();
      if (funnel != null) {
         float t = (float)this.age + f.partial;
         float r = Mth.lerp(f.partial, this.lastShown, this.shown);
         if (!(r < 0.6F)) {
            ClientLevel level = Minecraft.getInstance().level;
            int tint = level != null ? lit(level, this.centre.x, this.centre.y + 0.5, this.centre.z) : 16777215;
            float fade = this.closedAt < 0 ? Math.min(1.0F, t / 8.0F) : Math.max(0.0F, 1.0F - ((float)(this.age - this.closedAt) + f.partial) / 12.0F);
            if (!(fade <= 0.01F)) {
               float th = Math.max(0.2F, Math.min(this.throat, r * 0.8F));

               for (int k = 0; k < 6; k++) {
                  float base = (float)k * (float) (Math.PI * 2) / 6.0F + t * 0.05F;

                  for (int i = 0; i < 18; i++) {
                     float q = (float)i / 17.0F;
                     float d = r - (r - th * 0.6F) * q;
                     float ang = base + 2.2F * (float)Math.log((double)((r + 0.6F) / (d + 0.6F)));
                     double wx = this.centre.x + (double)(Mth.cos(ang) * d);
                     double wz = this.centre.z + (double)(Mth.sin(ang) * d);
                     this.px[i] = wx;
                     this.py[i] = this.ground(funnel, wx, wz) + 0.06 + 0.02 * (double)k;
                     this.pz[i] = wz;
                     this.ax[i] = Mth.cos(ang);
                     this.ay[i] = 0.0F;
                     this.az[i] = Mth.sin(ang);
                     this.w[i] = (0.35F + 0.95F * (1.0F - q)) * Math.min(1.0F, r / 3.0F);
                     this.a[i] = Math.min(1.0F, q * 8.0F) * Math.min(1.0F, (1.0F - q) * 4.0F + 0.2F) * 0.95F * fade;
                     this.bright[i] = 1.05F - 0.4F * q * q;
                  }

                  SandDraw.band(
                     f,
                     MeshTex.SAND_STREAM,
                     18,
                     this.px,
                     this.py,
                     this.pz,
                     this.ax,
                     this.ay,
                     this.az,
                     this.w,
                     this.a,
                     this.bright,
                     -t * 0.35F + (float)k * 0.37F,
                     r / 18.0F * 0.8F,
                     tint,
                     true
                  );
               }

               if (this.closedAt < 0) {
                  SandDraw.loop(
                     f,
                     this.centre.x,
                     this.centre.y + 0.12,
                     this.centre.z,
                     1.0F,
                     0.0F,
                     0.0F,
                     0.0F,
                     0.0F,
                     1.0F,
                     th * 1.2F,
                     th * 1.4F,
                     28,
                     -t * 0.12F,
                     3.0F,
                     scale(tint, 0.7F),
                     0.6F * fade
                  );
               }
            }
         }
      }
   }

   @Override
   public double x() {
      return this.centre.x;
   }

   @Override
   public double y() {
      return this.centre.y;
   }

   @Override
   public double z() {
      return this.centre.z;
   }

   @Override
   public double cullDistance() {
      return 128.0;
   }
}
