package com.devilfruits.fruits.gomu_gomu.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.fx.ClientFxRegistry;
import com.devilfruits.client.limb.LimbDriver;
import com.devilfruits.client.limb.LimbFrame;
import com.devilfruits.client.limb.LimbSpec;
import com.devilfruits.client.limb.Limbs;
import com.devilfruits.client.render.CameraEffects;
import com.devilfruits.client.vfx.fx.Impact;
import com.devilfruits.client.vfx.fx.ShockRing;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.core.limb.LimbPath;
import com.devilfruits.core.limb.LimbUv;
import com.devilfruits.core.network.FxPayload;
import com.devilfruits.fruits.gomu_gomu.GomuFx;
import com.devilfruits.fruits.gomu_gomu.strike.StrikeKind;
import com.devilfruits.fruits.gomu_gomu.strike.StrikePath;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Vector3f;

public final class StrikeClient {
   public static final ResourceLocation SWIRL = DevilFruits.id("textures/entity/gomu_gomu/swirl.png");
   private static final int CREAM = 16773598;
   private static final Map<StrikeClient.Key, StrikeClient.Shared> STRIKES = new HashMap<>();

   static void init() {
      ClientFxRegistry.register(GomuFx.STRIKE, StrikeClient::strike);
      ClientFxRegistry.register(GomuFx.STRIKE_HIT, StrikeClient::hit);
      ClientFxRegistry.register(GomuFx.ROCKET_ANCHOR, StrikeClient::anchor);
      ClientFxRegistry.register(GomuFx.ROCKET_RELEASE, StrikeClient::release);
      ClientFxRegistry.register(GomuFx.ROCKET_SLAM, StrikeClient::slam);
   }

   private static void anchor(ClientLevel level, Entity entity, FxPayload p) {
      StrikeClient.Shared shared = entity == null ? null : STRIKES.get(new StrikeClient.Key(entity.getId(), p.seed()));
      if (shared != null) {
         shared.anchor = p.pos();
         if (p.extra() > 0) {
            shared.anchorEntity = level.getEntity(p.extra() - 1);
            if (shared.anchorEntity != null) {
               shared.anchorOffset = p.pos().subtract(shared.anchorEntity.position());
            }
         }

         Impact.at(level, p.pos(), p.seed())
            .caster(entity)
            .flash(0.5F)
            .glow(16773598)
            .dust(p.extra() > 0 ? 4 : 10)
            .debris(p.extra() > 0 ? 0 : 4, 0.3F)
            .trauma(0.25F, 0.1F, 1.2F)
            .play();
         Vec3 feet = entity.position();

         for (int i = 0; i < 10; i++) {
            double a = (double)i / 10.0 * Math.PI * 2.0;
            VfxParticles.emit(VfxPresets.DUST, feet.x, feet.y + 0.1, feet.z, Math.cos(a) * 0.12, 0.02, Math.sin(a) * 0.12).tint(13155496);
         }
      }
   }

   private static void release(ClientLevel level, Entity entity, FxPayload p) {
      StrikeClient.Shared shared = entity == null ? null : STRIKES.get(new StrikeClient.Key(entity.getId(), p.seed()));
      if (shared != null) {
         shared.releasedAt = (double)(level.getGameTime() - p.seed());
         shared.anchor = shared.anchorNow(0.0F);
         shared.anchorEntity = null;
      }

      if (entity != null) {
         ShockRing.at(entity.getBoundingBox().getCenter(), p.dir()).radius(0.2F, 1.4F).life(6).strength(0.6F).rim(0.3F, 16773598).spawn();
      }
   }

   private static void slam(ClientLevel level, Entity entity, FxPayload p) {
      Impact.at(level, p.pos(), p.seed())
         .caster(entity)
         .flash(0.7F)
         .glow(16773598)
         .wave(2.2F, 8, 16773598)
         .dust(10)
         .trauma(0.45F, 0.08F, 2.0F)
         .hitStop(2)
         .play();
      ShockRing.at(p.pos(), p.dir()).radius(0.3F, 2.2F).life(8).strength(0.9F).rim(0.3F, 16773598).spawn();
   }

   private static void strike(ClientLevel level, Entity entity, FxPayload p) {
      if (entity instanceof AbstractClientPlayer player) {
         StrikeKind kind = StrikeKind.of(p.extra() & 15);
         int arms = p.extra() >> 4 & 3;
         StrikeClient.Key key = new StrikeClient.Key(entity.getId(), p.seed());
         StrikeClient.Shared shared = new StrikeClient.Shared();
         if (STRIKES.size() > 64) {
            STRIKES.clear();
         }

         STRIKES.put(key, shared);

         for (int arm : new int[]{1, -1}) {
            if ((arms & (arm > 0 ? 1 : 2)) != 0) {
               LimbSpec spec = new LimbSpec().arm(arm > 0 ? LimbUv.Side.RIGHT : LimbUv.Side.LEFT);
               StrikeClient.Driver driver = new StrikeClient.Driver(level, player, kind, p.dir().normalize(), (double)p.power(), p.seed(), arm, shared);
               spec.shaper = driver::shape;
               if (kind.twist > 0.0F) {
                  spec.swirl = SWIRL;
               }

               driver.spec = spec;
               Limbs.add(player, spec, driver);
            }
         }
      }
   }

   private static void hit(ClientLevel level, Entity entity, FxPayload p) {
      StrikeKind kind = StrikeKind.of(p.extra() & 15);
      int arm = (p.extra() >> 4 & 3) == 1 ? 1 : -1;
      boolean body = (p.extra() >> 6 & 1) == 1;
      boolean through = (p.extra() >> 7 & 1) == 1;
      StrikeClient.Shared shared = entity == null ? null : STRIKES.get(new StrikeClient.Key(entity.getId(), p.seed()));
      if (shared != null) {
         shared.ends[arm > 0 ? 0 : 1] = (double)p.power();
      }

      if (through) {
         Impact.at(level, p.pos(), p.seed()).caster(entity).flash(0.3F).glow(16773598).dust(10).trauma(0.2F, 0.1F, 1.0F).play();
      } else if (kind != StrikeKind.ROCKET) {
         Vec3 at = p.pos();
         Vec3 aim = p.dir();
         switch (kind) {
            case BAZOOKA:
               Impact.at(level, at, p.seed())
                  .caster(entity)
                  .flash(0.9F)
                  .glow(16773598)
                  .wave(3.2F, 9, 16773598)
                  .dust(body ? 10 : 22)
                  .debris(body ? 0 : 10, 0.5F)
                  .trauma(0.55F, 0.07F, 2.4F)
                  .hitStop(3)
                  .fov(0.04F)
                  .play();
               ShockRing.at(at, aim).radius(0.3F, 2.6F).life(9).strength(1.0F).band(0.45F).rim(0.35F, 16773598).spawn();
               ShockRing.at(at.add(aim.scale(0.6)), aim).radius(0.2F, 1.8F).life(7).strength(0.7F).move(aim.scale(0.18)).rim(0.2F, 16773598).spawn();
               break;
            case RIFLE:
               Impact.at(level, at, p.seed())
                  .caster(entity)
                  .flash(0.7F)
                  .glow(16773598)
                  .dust(body ? 4 : 12)
                  .debris(body ? 0 : 6, 0.4F)
                  .trauma(0.35F, 0.09F, 1.6F)
                  .hitStop(2)
                  .play();

               for (int i = 0; i < 3; i++) {
                  ShockRing.at(at.add(aim.scale(0.4 * (double)i)), aim)
                     .radius(0.15F, 0.9F + (float)i * 0.3F)
                     .life(6 + i)
                     .strength(0.8F)
                     .twist(1.1F)
                     .move(aim.scale(0.35))
                     .rim(0.3F, 16773598)
                     .spawn();
               }
               break;
            default:
               Impact.at(level, at, p.seed())
                  .caster(entity)
                  .flash(0.5F)
                  .glow(16773598)
                  .wave(1.3F, 6, 16773598)
                  .dust(body ? 5 : 12)
                  .debris(body ? 0 : 5, 0.35F)
                  .trauma(0.3F, 0.1F, 1.4F)
                  .hitStop(2)
                  .play();
               ShockRing.at(at, aim).radius(0.15F, 1.2F).life(6).strength(0.7F).rim(0.3F, 16773598).spawn();
         }
      }
   }

   private StrikeClient() {
   }

   static final class Driver implements LimbDriver {
      final ClientLevel level;
      final AbstractClientPlayer player;
      final StrikeKind kind;
      final Vec3 aim;
      final double reach;
      final long start;
      final int arm;
      final StrikeClient.Shared shared;
      final double predicted;
      LimbSpec spec;
      private boolean cracked;
      private int lastLines = -1;
      private float t;
      private double end;
      private final Vector3f tmp = new Vector3f();

      Driver(ClientLevel level, AbstractClientPlayer player, StrikeKind kind, Vec3 aim, double reach, long start, int arm, StrikeClient.Shared shared) {
         this.level = level;
         this.player = player;
         this.kind = kind;
         this.aim = aim;
         this.reach = reach;
         this.start = start;
         this.arm = arm;
         this.shared = shared;
         Vec3 shoulder = StrikePath.shoulder(player, player.position(), aim, arm);
         BlockHitResult clip = level.clip(new ClipContext(shoulder, shoulder.add(aim.scale(reach)), Block.COLLIDER, Fluid.NONE, player));
         this.predicted = clip.getType() == Type.BLOCK ? Math.max(0.7, clip.getLocation().distanceTo(shoulder)) : reach;
      }

      @Override
      public boolean path(LimbFrame f, LimbPath path) {
         this.t = (float)(this.level.getGameTime() - this.start) + f.partial();
         double told = this.shared.ends[this.arm > 0 ? 0 : 1];
         this.end = Double.isNaN(told) ? this.predicted : told;
         Vec3 shoulder = StrikePath.shoulder(this.player, this.player.getPosition(f.partial()), this.aim, this.arm);
         Vec3 anchor = this.shared.anchorNow(f.partial());
         Vec3 fist;
         if (anchor != null) {
            Vec3 grip = anchor.add(StrikePath.side(this.aim).scale((double)this.arm * 0.18));
            if (Double.isNaN(this.shared.releasedAt)) {
               fist = grip;
            } else {
               double u = ((double)this.t - this.shared.releasedAt) / (double)this.kind.retract;
               if (u >= 1.0) {
                  return false;
               }

               fist = shoulder.add(grip.subtract(shoulder).scale(1.0 - StrikePath.back(u)));
            }

            this.pullBeats();
         } else {
            if ((double)this.t >= StrikePath.doneTick(this.kind, this.end) || this.t < 0.0F) {
               return this.t < 0.0F;
            }

            double d = StrikePath.distance(this.kind, (double)this.t, this.end);
            fist = StrikePath.fist(this.kind, shoulder, this.aim, this.arm, d);
         }

         path.add(f.px(fist.x, fist.y, fist.z, this.tmp));
         Vec3 dir = fist.subtract(shoulder);
         if (dir.lengthSqr() > 1.0E-4) {
            f.pxDirection(dir.x, dir.y, dir.z, this.tmp);
            path.endDirection(this.tmp.x, this.tmp.y, this.tmp.z);
         }

         if (this.kind.twist > 0.0F) {
            float wind = Mth.clamp((this.t - (float)this.kind.feint) / (float)this.kind.coil, 0.0F, 1.0F);
            this.spec.twistTurns = this.kind.twist * wind;
            this.spec.spin = this.t * 0.9F;
            this.spec.swirlScroll = this.t * 6.0F;
            this.spec.swirlColor = (int)(wind * 200.0F) << 24 | 16777215;
         }

         this.beats(shoulder, fist);
         return true;
      }

      private void pullBeats() {
         int tick = (int)this.t;
         if (tick != this.lastLines && Double.isNaN(this.shared.releasedAt) && this.arm >= 0) {
            this.lastLines = tick;
            Vec3 v = this.player.getDeltaMovement();

            for (int i = 0; i < 3; i++) {
               Vec3 p = this.player
                  .position()
                  .add(this.level.random.nextGaussian() * 0.5, 0.4 + this.level.random.nextDouble() * 1.3, this.level.random.nextGaussian() * 0.5);
               VfxParticles.emit(VfxPresets.SPEED_LINE, p.x, p.y, p.z, -v.x * 0.6, -v.y * 0.6, -v.z * 0.6).tint(16773598);
            }

            if (this.player == Minecraft.getInstance().player && v.lengthSqr() > 0.5) {
               CameraEffects.fovPunch(0.02F);
            }
         }
      }

      private void beats(Vec3 shoulder, Vec3 fist) {
         if (!this.cracked && this.t >= (float)this.kind.release()) {
            this.cracked = true;
            Vec3 at = shoulder.add(this.aim.scale(0.9));
            ShockRing.at(at, this.aim).radius(0.08F, this.kind.twoArms() ? 1.1F : 0.8F).life(5).strength(0.55F).rim(0.45F, 16773598).spawn();
         }

         int tick = (int)this.t;
         if (StrikePath.shooting(this.kind, (double)this.t, this.end) && tick != this.lastLines) {
            this.lastLines = tick;

            for (int i = 0; i < 2; i++) {
               double u = this.level.random.nextDouble();
               Vec3 p = shoulder.lerp(fist, 0.3 + u * 0.7)
                  .add(this.level.random.nextGaussian() * 0.12, this.level.random.nextGaussian() * 0.12, this.level.random.nextGaussian() * 0.12);
               VfxParticles.emit(VfxPresets.SPEED_LINE, p.x, p.y, p.z, this.aim.x * 0.9, this.aim.y * 0.9, this.aim.z * 0.9).tint(16773598);
            }

            if (this.kind.twist > 0.0F) {
               ShockRing.at(fist.subtract(this.aim.scale(0.3)), this.aim).radius(0.15F, 0.55F).life(4).strength(0.35F).twist(1.2F).rim(0.25F, 16773598).spawn();
            }
         }
      }

      void shape(float s, float length, Vector3f centre, Vector3f across, Vector3f depth, float[] scale) {
         if (!(length <= 0.0F)) {
            if (this.shared.anchor != null && Double.isNaN(this.shared.releasedAt)) {
               centre.fma(0.3F * Mth.sin((float) Math.PI * s / length) * Mth.sin(this.t * 13.0F + s * 0.9F), across);
            } else {
               float u = s / length;
               float envelope = Mth.sin((float) Math.PI * u);
               float wave = 0.0F;
               float sinceSnap = this.t - (float)this.kind.release();
               if (sinceSnap > 0.0F) {
                  wave += 1.4F * (float)Math.exp((double)(-sinceSnap * 0.55F)) * Mth.sin((float) (Math.PI * 2) * u * 1.5F - sinceSnap * 1.7F);
               }

               double tEnd = StrikePath.endTick(this.kind, this.end);
               float sinceBack = (float)((double)this.t - tEnd - (double)this.kind.hold);
               if (sinceBack > 0.0F) {
                  wave += 1.8F * (float)Math.exp((double)(-sinceBack * 0.45F)) * Mth.sin((float) (Math.PI * 2) * u * 2.0F - sinceBack * 2.1F);
               }

               centre.fma(wave * envelope, across);
               if ((double)this.t >= tEnd && (double)this.t < tEnd + (double)this.kind.hold) {
                  centre.fma(0.35F * envelope * Mth.sin(this.t * 11.0F + s * 0.7F), depth);
               }
            }
         }
      }
   }

   private static record Key(int entity, long start) {
   }

   static final class Shared {
      final double[] ends = new double[]{Double.NaN, Double.NaN};
      @Nullable
      Vec3 anchor;
      @Nullable
      Entity anchorEntity;
      Vec3 anchorOffset = Vec3.ZERO;
      double releasedAt = Double.NaN;

      @Nullable
      Vec3 anchorNow(float partial) {
         return this.anchorEntity != null && !this.anchorEntity.isRemoved() ? this.anchorEntity.getPosition(partial).add(this.anchorOffset) : this.anchor;
      }
   }
}
