package com.devilfruits.fruits.gomu_gomu.client;

import com.devilfruits.client.fx.ClientFxRegistry;
import com.devilfruits.client.limb.LimbDriver;
import com.devilfruits.client.limb.LimbFrame;
import com.devilfruits.client.limb.LimbSpec;
import com.devilfruits.client.limb.Limbs;
import com.devilfruits.client.render.CameraEffects;
import com.devilfruits.client.vfx.fx.GroundWave;
import com.devilfruits.client.vfx.fx.Impact;
import com.devilfruits.client.vfx.fx.ShockRing;
import com.devilfruits.client.vfx.particle.ParticleStyle;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.devilfruits.core.limb.LimbPath;
import com.devilfruits.core.limb.LimbUv;
import com.devilfruits.core.network.FxPayload;
import com.devilfruits.fruits.gomu_gomu.GomuFx;
import com.devilfruits.fruits.gomu_gomu.strike.GatlingPath;
import com.devilfruits.fruits.gomu_gomu.strike.StrikePath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent.Post;
import net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector3f;

public final class GatlingClient {
   private static final int CREAM = 16773598;
   static final int GHOST_TICKS = 6;
   private static final float GHOST_ALPHA = 0.6F;
   private static final ParticleStyle WIND = ParticleStyle.colour(VfxSprites.SPARK)
      .life(5, 7)
      .size(0.3F, 0.18F, 1.0F)
      .fade(0.15F, 0.5F)
      .additive(0.25F)
      .emissive(0.1F)
      .soft(0.0F)
      .drag(0.0F)
      .stretch(1.1F)
      .spin(0.0F)
      .build();
   private static final double CAMERA_CLEAR_SIDE = 0.6;
   private static final double CAMERA_CLEAR_DROP = 0.4;
   private static final int MAX_TICKS = 640;
   private static final Map<Integer, GatlingClient.Barrage> BARRAGES = new HashMap<>();
   private static final List<Runnable> PENDING = new ArrayList<>();
   private static final boolean[] SAVED = new boolean[4];
   private static PlayerModel<?> hiddenOn;

   static void init() {
      ClientFxRegistry.register(GomuFx.GATLING, GatlingClient::start);
      ClientFxRegistry.register(GomuFx.GATLING_FINISH, GatlingClient::finish);
      NeoForge.EVENT_BUS.addListener(e -> tick());
      NeoForge.EVENT_BUS.addListener(e -> {
         BARRAGES.clear();
         PENDING.clear();
      });
      NeoForge.EVENT_BUS.addListener(EventPriority.LOW, GatlingClient::onRenderPlayerPre);
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, GatlingClient::onRenderPlayerPost);
      NeoForge.EVENT_BUS.addListener(GatlingClient::onRenderHand);
   }

   private static void start(ClientLevel level, Entity entity, FxPayload p) {
      if (entity instanceof AbstractClientPlayer player) {
         BARRAGES.put(player.getId(), new GatlingClient.Barrage(player, p.seed()));
      }
   }

   private static void finish(ClientLevel level, Entity entity, FxPayload p) {
      if (entity != null) {
         GatlingClient.Barrage b = BARRAGES.get(entity.getId());
         if (b != null) {
            b.over = true;
         }
      }

      Vec3 at = p.pos();
      Impact.at(level, at, p.seed())
         .caster(entity)
         .flash(1.0F)
         .glow(16773598)
         .wave(3.0F, 9, 16773598)
         .dust(14)
         .trauma(0.6F, 0.07F, 2.6F)
         .hitStop(4)
         .fov(-0.06F)
         .play();
      ShockRing.at(at, p.dir()).radius(0.3F, 3.2F).life(9).strength(1.0F).band(0.45F).rim(0.35F, 16773598).spawn();
      if (entity != null) {
         GroundWave.at(entity.getX(), entity.getY(), entity.getZ(), 4.5F, 10).strength(0.8F).ring(16773598, 0.35F).spawn();
      }
   }

   private static void tick() {
      Minecraft mc = Minecraft.getInstance();
      if (!PENDING.isEmpty()) {
         PENDING.forEach(Runnable::run);
         PENDING.clear();
      }

      if (!BARRAGES.isEmpty() && mc.level != null) {
         long now = mc.level.getGameTime();
         BARRAGES.values().removeIf(b -> {
            int tick = (int)(now - b.seed);
            if (!b.over && !b.player.isRemoved() && tick <= 640) {
               wind(mc, b, tick);

               while (b.spawned <= tick) {
                  Vec3 aim = b.player.getViewVector(1.0F);

                  for (int k = 0; k < 2; k++) {
                     int index = GatlingPath.first(b.spawned) + k;
                     LimbSpec spec = new LimbSpec().arm(GatlingPath.arm(index) > 0 ? LimbUv.Side.RIGHT : LimbUv.Side.LEFT);
                     spec.replaces = false;
                     spec.swarm = true;
                     Limbs.add(b.player, spec, new GatlingClient.Punch(mc.level, b, index, b.seed + (long)b.spawned, aim));
                  }

                  b.spawned++;
               }

               if (b.player == mc.player && tick % 4 == 0) {
                  CameraEffects.shake(0.12F);
               }

               return false;
            } else {
               return true;
            }
         });
      }
   }

   private static void wind(Minecraft mc, GatlingClient.Barrage b, int tick) {
      if (tick >= 0) {
         RandomSource random = mc.level.random;
         AbstractClientPlayer player = b.player;
         Vec3 aim = player.getViewVector(1.0F);
         Vec3 centre = GatlingPath.centre(player, player.position());
         Vec3 to = centre.add(aim.scale(4.0));
         Vec3 eye = player == mc.player && mc.options.getCameraType().isFirstPerson() ? player.getEyePosition() : null;
         Vec3 side = StrikePath.side(aim);
         Vec3 up = side.cross(aim).normalize();

         for (int launched = tick - 2 + 1; launched <= tick; launched++) {
            if (launched >= 0) {
               int age = tick - launched;
               double u0 = GatlingPath.extension((double)age);
               double u1 = GatlingPath.extension((double)(age + 1));

               for (int k = 0; k < 2; k++) {
                  int index = GatlingPath.first(launched) + k;
                  Vec3 from = GatlingPath.origin(b.seed, index, centre, aim);
                  Vec3 end = GatlingPath.target(b.seed, index, centre, aim);
                  Vec3 a = GatlingPath.along(b.seed, index, from, end, aim, u0);
                  Vec3 c = GatlingPath.along(b.seed, index, from, end, aim, u1);
                  Vec3 v = c.subtract(a).scale(0.55);

                  for (int n = 0; n < 2; n++) {
                     Vec3 at = a.lerp(c, ((double)n + 0.5) / 2.0).add(random.nextGaussian() * 0.06, random.nextGaussian() * 0.06, random.nextGaussian() * 0.06);
                     line(eye, at, v, 0.85F);
                  }
               }
            }
         }

         for (int n = 0; n < 5; n++) {
            double along = 0.3 + random.nextDouble() * 3.2;
            double a = random.nextDouble() * Math.PI * 2.0;
            double r = (0.7 + random.nextDouble() * 0.9) * (1.0 - along / 4.0 * 0.6);
            Vec3 at = centre.add(aim.scale(along)).add(side.scale(Math.cos(a) * r)).add(up.scale(Math.sin(a) * r));
            line(eye, at, to.subtract(at).normalize().scale(0.9 + random.nextDouble() * 0.4), 0.7F);
         }

         for (int n = 0; n < 3; n++) {
            double a = random.nextDouble() * Math.PI * 2.0;
            Vec3 out = side.scale(Math.cos(a)).add(up.scale(Math.sin(a)));
            line(eye, to.add(out.scale(0.25)), out.scale(0.45 + random.nextDouble() * 0.2).add(aim.scale(0.2)), 0.75F);
         }
      }
   }

   private static void line(@Nullable Vec3 eye, Vec3 at, Vec3 v, float alpha) {
      if (eye == null || !(at.distanceToSqr(eye) < 1.0)) {
         VfxParticles.emit(WIND, at.x, at.y, at.z, v.x, v.y, v.z).tint(16777215).alpha(alpha);
      }
   }

   private static boolean channeling(Entity e) {
      GatlingClient.Barrage b = BARRAGES.get(e.getId());
      return b != null && !b.over;
   }

   private static void onRenderPlayerPre(Pre event) {
      if (!BARRAGES.isEmpty() && channeling(event.getEntity())) {
         PlayerModel<?> m = (PlayerModel<?>)event.getRenderer().getModel();
         hiddenOn = m;
         SAVED[0] = m.rightArm.visible;
         SAVED[1] = m.rightSleeve.visible;
         SAVED[2] = m.leftArm.visible;
         SAVED[3] = m.leftSleeve.visible;
         m.rightArm.visible = false;
         m.rightSleeve.visible = false;
         m.leftArm.visible = false;
         m.leftSleeve.visible = false;
      }
   }

   private static void onRenderPlayerPost(Post event) {
      if (hiddenOn == event.getRenderer().getModel()) {
         hiddenOn.rightArm.visible = SAVED[0];
         hiddenOn.rightSleeve.visible = SAVED[1];
         hiddenOn.leftArm.visible = SAVED[2];
         hiddenOn.leftSleeve.visible = SAVED[3];
         hiddenOn = null;
      }
   }

   private static void onRenderHand(RenderHandEvent event) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null && channeling(player)) {
         event.setCanceled(true);
      }
   }

   private GatlingClient() {
   }

   static final class Barrage {
      final AbstractClientPlayer player;
      final long seed;
      int spawned;
      boolean over;

      Barrage(AbstractClientPlayer player, long seed) {
         this.player = player;
         this.seed = seed;
      }
   }

   static final class Ghost implements LimbDriver {
      final ClientLevel level;
      final AbstractClientPlayer player;
      final LimbSpec spec;
      final Vec3 root;
      final Vec3 mid;
      final Vec3 fist;
      final long born;
      private final Vector3f tmp = new Vector3f();

      Ghost(ClientLevel level, AbstractClientPlayer player, LimbSpec spec, Vec3 root, Vec3 mid, Vec3 fist, long born) {
         this.level = level;
         this.player = player;
         this.spec = spec;
         this.root = root;
         this.mid = mid;
         this.fist = fist;
         this.born = born;
      }

      @Override
      public boolean path(LimbFrame f, LimbPath path) {
         double age = (double)((float)(this.level.getGameTime() - this.born) + f.partial());
         if (age >= 6.0) {
            return false;
         } else {
            float fade = (float)(1.0 - age / 6.0);
            this.spec.color = (int)(0.6F * fade * fade * 255.0F) << 24 | 16777215;
            Vec3 pos = this.player.getPosition(f.partial());
            Vec3 r = pos.add(this.root);
            Vec3 m = pos.add(this.mid);
            Vec3 e = pos.add(this.fist);
            f.rootAt(r.x, r.y, r.z);
            path.add(f.px(m.x, m.y, m.z, this.tmp));
            path.add(f.px(e.x, e.y, e.z, this.tmp));
            return true;
         }
      }
   }

   static final class Punch implements LimbDriver {
      final ClientLevel level;
      final GatlingClient.Barrage barrage;
      final int index;
      final long launched;
      final Vec3 aim;
      private boolean peaked;
      private final Vector3f tmp = new Vector3f();

      Punch(ClientLevel level, GatlingClient.Barrage barrage, int index, long launched, Vec3 aim) {
         this.level = level;
         this.barrage = barrage;
         this.index = index;
         this.launched = launched;
         this.aim = aim;
      }

      @Override
      public boolean path(LimbFrame f, LimbPath path) {
         double age = (double)((float)(this.level.getGameTime() - this.launched) + f.partial());
         double ext = GatlingPath.extension(age);
         if (ext < 0.0) {
            return age < 0.0;
         } else {
            AbstractClientPlayer player = this.barrage.player;
            Vec3 pos = player.getPosition(f.partial());
            Vec3 centre = GatlingPath.centre(player, pos);
            Vec3 from = GatlingPath.origin(this.barrage.seed, this.index, centre, this.aim);
            if (f.firstPerson()) {
               from = this.clearOfCamera(player.getEyePosition(f.partial()), from);
            }

            Vec3 to = GatlingPath.target(this.barrage.seed, this.index, centre, this.aim);
            Vec3 mid = GatlingPath.along(this.barrage.seed, this.index, from, to, this.aim, ext * 0.5);
            Vec3 fist = GatlingPath.along(this.barrage.seed, this.index, from, to, this.aim, ext);
            f.rootAt(from.x, from.y, from.z);
            if (fist.distanceTo(from) < 0.7) {
               fist = from.add(to.subtract(from).normalize().scale(0.7));
            } else {
               path.add(f.px(mid.x, mid.y, mid.z, this.tmp));
            }

            path.add(f.px(fist.x, fist.y, fist.z, this.tmp));
            if (!this.peaked && age >= 2.0) {
               this.peaked = true;
               this.peak(pos, from, mid, fist, to);
            }

            return true;
         }
      }

      private Vec3 clearOfCamera(Vec3 eye, Vec3 from) {
         double side = from.subtract(eye).dot(StrikePath.side(this.aim));
         return Math.abs(side) < 0.6 && from.y > eye.y - 0.4 ? new Vec3(from.x, eye.y - 0.4 - (from.y - (eye.y - 0.4)) * 0.3, from.z) : from;
      }

      private void peak(Vec3 pos, Vec3 from, Vec3 mid, Vec3 fist, Vec3 to) {
         long born = this.level.getGameTime();
         LimbSpec ghost = new LimbSpec().arm(GatlingPath.arm(this.index) > 0 ? LimbUv.Side.RIGHT : LimbUv.Side.LEFT);
         ghost.replaces = false;
         ghost.swarm = true;
         GatlingClient.PENDING
            .add(
               () -> Limbs.add(
                     this.barrage.player,
                     ghost,
                     new GatlingClient.Ghost(this.level, this.barrage.player, ghost, from.subtract(pos), mid.subtract(pos), fist.subtract(pos), born)
                  )
            );
         VfxParticles.emit(VfxPresets.SPEED_LINE, fist.x, fist.y, fist.z, this.aim.x * 0.7, this.aim.y * 0.7, this.aim.z * 0.7).tint(16773598);
         AABB box = new AABB(to, to).inflate(0.6);
         Iterator var10 = this.level.getEntities(this.barrage.player, box, x -> x instanceof LivingEntity).iterator();
         if (var10.hasNext()) {
            Entity e = (Entity)var10.next();
            ShockRing.at(fist, this.aim).radius(0.05F, 0.45F).life(3).strength(0.35F).rim(0.5F, 16773598).spawn();

            for (int i = 0; i < 3; i++) {
               VfxParticles.emit(
                     VfxPresets.SPARK,
                     fist.x,
                     fist.y,
                     fist.z,
                     this.level.random.nextGaussian() * 0.15,
                     this.level.random.nextGaussian() * 0.15,
                     this.level.random.nextGaussian() * 0.15
                  )
                  .tint(16771226);
            }
         }
      }
   }
}
