package com.devilfruits.fruits.gomu_gomu.client;

import com.devilfruits.client.fx.ClientFxRegistry;
import com.devilfruits.client.limb.LimbDriver;
import com.devilfruits.client.limb.LimbFrame;
import com.devilfruits.client.limb.LimbSpec;
import com.devilfruits.client.limb.Limbs;
import com.devilfruits.client.vfx.fx.GroundWave;
import com.devilfruits.client.vfx.fx.Impact;
import com.devilfruits.client.vfx.fx.ShockRing;
import com.devilfruits.core.limb.LimbPath;
import com.devilfruits.core.limb.LimbUv;
import com.devilfruits.core.network.FxPayload;
import com.devilfruits.fruits.gomu_gomu.GomuFx;
import com.devilfruits.fruits.gomu_gomu.strike.StrikePath;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class AxeClient {
   private static final int CREAM = 16773598;

   static void init() {
      ClientFxRegistry.register(GomuFx.AXE, AxeClient::start);
      ClientFxRegistry.register(GomuFx.AXE_IMPACT, AxeClient::impact);
   }

   private static void start(ClientLevel level, Entity entity, FxPayload p) {
      if (entity instanceof AbstractClientPlayer player) {
         LimbSpec spec = new LimbSpec().leg(LimbUv.Side.RIGHT);
         AxeClient.Leg leg = new AxeClient.Leg(level, player, p.pos(), p.dir().normalize(), p.seed(), spec);
         spec.shaper = leg::shape;
         Limbs.add(player, spec, leg);
      }
   }

   private static void impact(ClientLevel level, Entity entity, FxPayload p) {
      Vec3 at = p.pos();
      float radius = p.power();
      GroundWave.at(at.x, at.y, at.z, radius, 12).strength(1.1F).ring(16773598, 0.45F).dust(36, 12168858, 0.6F, 0.28F, p.seed()).spawn();
      Impact.at(level, at.add(0.0, 0.2, 0.0), p.seed())
         .caster(entity)
         .flash(1.0F)
         .glow(16773598)
         .decal(3.2F)
         .debris(18, 0.7F)
         .dust(24)
         .smoke(10, 10260604, 1.4F)
         .trauma(0.85F, 0.05F, 3.2F)
         .hitStop(3)
         .fov(0.05F)
         .play();
      ShockRing.at(at.add(0.0, 0.6, 0.0), new Vec3(0.0, 1.0, 0.0)).radius(0.6F, radius * 0.55F).life(10).strength(1.0F).band(0.4F).rim(0.3F, 16773598).spawn();
   }

   static Vec3 foot(double t, Vec3 hip, Vec3 flat, Vec3 target) {
      Vec3 down = new Vec3(0.0, -1.0, 0.0);
      Vec3 rest = hip.add(down.scale(0.7));
      Vec3 apex = new Vec3(target.x, hip.y + 5.0, target.z);
      if (t < 7.0) {
         Vec3 toApex = apex.subtract(hip);
         Vec3 ahead = new Vec3(toApex.x, 0.0, toApex.z);
         ahead = ahead.lengthSqr() < 1.0E-6 ? flat : ahead.normalize();
         double top = Math.acos(Mth.clamp(toApex.normalize().dot(down), -1.0, 1.0));
         double u = 1.0 - Math.pow(1.0 - t / 7.0, 3.0);
         double theta = top * u;
         Vec3 dir = down.scale(Math.cos(theta)).add(ahead.scale(Math.sin(theta)));
         return hip.add(dir.scale(Mth.lerp(Math.pow(u, 1.5), 0.7, toApex.length())));
      } else if (t < 11.0) {
         return apex;
      } else if (t < 14.0) {
         double u = (t - 7.0 - 4.0) / 3.0;
         return apex.lerp(target, u * u);
      } else if (t < 17.0) {
         return target;
      } else {
         double u = Math.min(1.0, (t - 14.0 - 3.0) / 7.0);
         double c = 0.9;
         double back = 1.0 + (c + 1.0) * Math.pow(u - 1.0, 3.0) + c * Math.pow(u - 1.0, 2.0);
         return target.lerp(rest, back);
      }
   }

   private AxeClient() {
   }

   static final class Leg implements LimbDriver {
      final ClientLevel level;
      final AbstractClientPlayer player;
      final Vec3 target;
      final Vec3 flat;
      final long start;
      final LimbSpec spec;
      private float t;
      private boolean cracked;
      private final Vector3f tmp = new Vector3f();

      Leg(ClientLevel level, AbstractClientPlayer player, Vec3 target, Vec3 flat, long start, LimbSpec spec) {
         this.level = level;
         this.player = player;
         this.target = target;
         this.flat = flat;
         this.start = start;
         this.spec = spec;
      }

      @Override
      public boolean path(LimbFrame f, LimbPath path) {
         this.t = (float)(this.level.getGameTime() - this.start) + f.partial();
         if (this.t >= 24.0F) {
            return false;
         } else if (this.t < 0.0F) {
            return true;
         } else {
            Vec3 pos = this.player.getPosition(f.partial());
            double s = (double)this.player.getBbHeight() / 1.8;
            Vec3 hip = pos.add(0.0, 0.7 * s, 0.0).add(StrikePath.side(this.flat).scale(0.12 * s));
            Vec3 foot = AxeClient.foot((double)this.t, hip, this.flat, this.target);
            path.add(f.px(foot.x, foot.y, foot.z, this.tmp));
            Vec3 dir = foot.subtract(hip);
            if (dir.lengthSqr() > 1.0E-4) {
               f.pxDirection(dir.x, dir.y, dir.z, this.tmp);
               path.endDirection(this.tmp.x, this.tmp.y, this.tmp.z);
            }

            if (!this.cracked && this.t >= 11.0F) {
               this.cracked = true;
               ShockRing.at(foot, new Vec3(0.0, -1.0, 0.0)).radius(0.1F, 1.0F).life(5).strength(0.5F).rim(0.35F, 16773598).spawn();
            }

            return true;
         }
      }

      void shape(float s, float length, Vector3f centre, Vector3f across, Vector3f depth, float[] scale) {
         if (!(length <= 0.0F)) {
            float env = Mth.sin((float) Math.PI * s / length);
            if (this.t >= 7.0F && this.t < 11.0F) {
               centre.fma(0.4F * env * Mth.sin(this.t * 12.0F + s * 0.5F), across);
            }

            float since = this.t - 14.0F;
            if (since > 0.0F) {
               centre.fma(2.0F * env * (float)Math.exp((double)(-since * 0.45F)) * Mth.sin((float) (Math.PI * 2) * s / length * 1.5F - since * 1.8F), depth);
            }
         }
      }
   }
}
