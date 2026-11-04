package com.awakenedmod.awakened.client.graviton.move;

import com.awakenedmod.awakened.client.graviton.GravitonDraw;
import com.awakenedmod.awakened.client.render.FxLayer;
import com.awakenedmod.awakened.client.render.FxLayers;
import com.awakenedmod.awakened.client.render.SteeredParticle;
import com.awakenedmod.awakened.content.graviton.GravityWellMotion;
import com.awakenedmod.awakened.content.graviton.GravityWellPlanet;
import com.awakenedmod.awakened.content.graviton.GravityWellVisual;
import com.awakenedmod.awakened.core.Lab;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@EventBusSubscriber(
   modid = "awakened",
   value = {Dist.CLIENT}
)
public final class GravityWellRenderer {
   private static final int PALE = 16769732;
   private static final int WARM = 16747069;
   private static final int DARK = 1971474;
   private static final int END_SWAP = 99;
   private static final Lab.Value RADIUS = Lab.CLIENT.def("graviton.well.planet.radius", 1.5, 0.6, 2.5, 0.05);
   private static final Lab.Value VOXELS = Lab.CLIENT.def("graviton.well.planet.voxels", 11.0, 5.0, 19.0, 1.0);
   private static final Lab.Value SPIN = Lab.CLIENT.def("graviton.well.planet.spin", 1.8, 0.0, 8.0, 0.1);
   private static final Lab.Value FORM = Lab.CLIENT.def("graviton.well.planet.form_ticks", 7.0, 2.0, 20.0, 1.0);
   private static final Lab.Value DEBRIS = Lab.CLIENT.def("graviton.well.debris.rate", 1.2, 0.0, 5.0, 0.1);
   private static final Lab.Value DEBRIS_SIZE = Lab.CLIENT.def("graviton.well.debris.size", 0.12, 0.03, 0.4, 0.01);
   private static final Lab.Value SHATTER = Lab.CLIENT.def("graviton.well.shatter.speed", 0.26, 0.05, 1.0, 0.01);
   private static final Lab.Value PULL_RING = Lab.CLIENT.def("graviton.well.pull_ring_alpha", 0.22, 0.0, 1.0, 0.05);
   private static final Lab.Value HALO = Lab.CLIENT.def("graviton.well.star_halo", 0.55, 0.0, 1.0, 0.05);
   private static final Lab.Value LINE = Lab.CLIENT.def("graviton.well.fx.line", 0.03, 0.01, 0.1, 0.005);
   private static final Lab.Value LINE_PX = Lab.CLIENT.def("graviton.well.fx.line_px", 0.003, 0.0, 0.006, 2.0E-4);
   private static final Lab.Value TRAIL_ALPHA = Lab.CLIENT.def("graviton.well.fx.trail_alpha", 0.55, 0.0, 1.0, 0.05);
   private static final float CRUMBLE_TICKS = 10.0F;
   private static final float CLOSE_TICKS = 12.0F;
   private static final float POP_TICKS = 4.0F;
   private static final float POP = 0.14F;
   private static final int TRAIL_TICKS = 12;
   private static final float BEND_TICKS = 3.0F;
   private static final double TRAIL_CLEAR = 1.6;
   private static final double DEBRIS_CLEAR = 0.9;
   private static final double DUST_RANGE = 48.0;
   private static final int GROUND_REACH = 7;
   private static final int DEBRIS_CAP = 2400;
   private static final Map<Integer, GravityWellRenderer.Local> LOCALS = new HashMap<>();
   private static final List<GravityWellRenderer.Debris> BITS = new ArrayList<>();
   private static ClientLevel memoryLevel;

   private GravityWellRenderer() {
   }

   @SubscribeEvent
   public static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         LOCALS.clear();
         BITS.clear();
      } else {
         if (memoryLevel != mc.level) {
            LOCALS.clear();
            BITS.clear();
            memoryLevel = mc.level;
         }

         float now = (float)(mc.level.getGameTime() % 100000L);
         Set<Integer> present = new HashSet<>();

         for (Player p : mc.level.players()) {
            GravityWellVisual v = GravityWellClient.visualFor(p);
            GravityWellRenderer.Local local = LOCALS.get(p.getId());
            if (local == null) {
               if (!v.live()) {
                  continue;
               }

               local = new GravityWellRenderer.Local();
               LOCALS.put(p.getId(), local);
            }

            present.add(p.getId());
            follow(mc, local, v, now);
            sample(local, p, now);
            GravityWellRenderer.Well w = local.live;
            if (w != null && w.formed(now) && near(mc, w.center)) {
               pull(mc, w, now);
            }
         }

         LOCALS.keySet().removeIf(id -> !present.contains(id) || idle(LOCALS.get(id), now));
         BITS.removeIf(d -> now - d.t0 > d.life || now < d.t0 - 200.0F);
      }
   }

   private static boolean near(Minecraft mc, Vec3 at) {
      return mc.player != null && at.distanceToSqr(mc.player.position()) < 2304.0;
   }

   private static boolean idle(GravityWellRenderer.Local local, float now) {
      return local.live == null && local.dying.isEmpty() && local.trail.isEmpty() && now > local.trailUntil;
   }

   private static void follow(Minecraft mc, GravityWellRenderer.Local local, GravityWellVisual v, float now) {
      if (!local.seen) {
         local.seen = true;
         local.opens = v.opens();
         local.ends = v.ends();
         if (v.live()) {
            local.live = new GravityWellRenderer.Well(mc.level, v, now - 100.0F);
         }
      } else {
         if (v.opens() != local.opens) {
            local.opens = v.opens();
            if (local.live != null) {
               end(mc, local, 99, now);
               local.swapAt = now;
            }

            if (v.live()) {
               local.live = new GravityWellRenderer.Well(mc.level, v, now);
            }
         }

         if (v.ends() != local.ends) {
            local.ends = v.ends();
            if (local.live != null) {
               end(mc, local, v.endKind(), now);
            }
         }

         if (!v.live() && local.live != null) {
            end(mc, local, 1, now);
         }

         if (local.live != null && v.live() && v.planet() != local.live.planet) {
            local.live.setPlanet(mc.level, v.planet(), now);
         }

         local.dying.removeIf(w -> now - w.endAt > 13.0F);
      }
   }

   private static void end(Minecraft mc, GravityWellRenderer.Local local, int kind, float now) {
      GravityWellRenderer.Well w = local.live;
      w.endAt = now;
      w.endKind = kind;
      local.dying.add(w);
      local.live = null;
      local.trailUntil = now + 12.0F;
      if (w.model != null && near(mc, w.center)) {
         if (kind == 0) {
            crumble(mc, w, now);
         } else {
            shatter(mc, w, now);
         }
      }
   }

   private static void sample(GravityWellRenderer.Local local, Player p, float now) {
      while (!local.trail.isEmpty() && now - local.trail.peekLast().t() > 12.0F) {
         local.trail.pollLast();
      }

      if (local.live != null || now < local.trailUntil) {
         boolean bend = Math.abs(now - local.swapAt) <= 3.0F;
         local.trail.addFirst(new GravityWellRenderer.Sample(p.position().add(0.0, 0.9, 0.0), now + 1.0F, bend));
      }
   }

   private static void pull(Minecraft mc, GravityWellRenderer.Well w, float now) {
      RandomSource r = mc.level.random;
      int light = light(mc.level, w.center);
      w.debrisDue = w.debrisDue + (float)DEBRIS.get();

      while (w.debrisDue >= 1.0F && BITS.size() < 2400) {
         w.debrisDue--;
         Vec3 dir = randomDir(r);
         Vec3 from = w.center.add(dir.scale(w.radius * (2.3 + 0.9 * r.nextDouble())));
         PlanetModel.Mat m = crust(w.model, r);
         float size = (float)(DEBRIS_SIZE.get() * (0.6 + 0.8 * r.nextDouble()));
         BITS.add(
            new GravityWellRenderer.Debris(
               from,
               Vec3.ZERO,
               w.center.add(dir.scale(w.radius * 0.8)),
               0.0F,
               now,
               (float)(11 + r.nextInt(5)),
               size,
               r,
               spin(r),
               PlanetDraw.sprite(m),
               m.tint(),
               lightOf(m, light)
            )
         );
      }

      if (!(r.nextFloat() > 0.4F) && BITS.size() < 2400) {
         MutableBlockPos pos = new MutableBlockPos();
         double a = r.nextDouble() * Math.PI * 2.0;
         double d = 0.6 + r.nextDouble() * Math.max(1.2, w.radius * 1.6);
         double x = w.center.x + Math.cos(a) * d;
         double z = w.center.z + Math.sin(a) * d;
         int top = Mth.floor(w.center.y - w.radius);

         for (int y = top; y > top - 7; y--) {
            pos.set(Mth.floor(x), y, Mth.floor(z));
            BlockState state = mc.level.getBlockState(pos);
            if (!state.isAir() && state.getFluidState().isEmpty()) {
               if (state.getCollisionShape(mc.level, pos).isEmpty()) {
                  return;
               }

               Vec3 from = new Vec3(x, (double)y + 1.05, z);
               Vec3 into = w.center.add(from.subtract(w.center).normalize().scale(w.radius * 0.8));
               float size = (float)(DEBRIS_SIZE.get() * (0.9 + 0.9 * r.nextDouble()));
               BITS.add(
                  new GravityWellRenderer.Debris(
                     from, Vec3.ZERO, into, 0.0F, now, (float)(14 + r.nextInt(6)), size, r, spin(r), PlanetDraw.sprite(state), 16777215, light(mc.level, from)
                  )
               );
               if (r.nextFloat() < 0.5F) {
                  particle(mc, state, from, new Vec3(0.0, 0.12, 0.0));
               }

               return;
            }
         }
      }
   }

   private static void shatter(Minecraft mc, GravityWellRenderer.Well w, float now) {
      RandomSource r = mc.level.random;
      double speed = SHATTER.get();
      loose(mc, w, now, 1.0F, (at, out) -> out.scale(speed * (0.55 + 0.9 * r.nextDouble())).add(randomDir(r).scale(speed * 0.3)).add(0.0, 0.06, 0.0), 0.035F, 0);

      for (int i = 0; i < 26; i++) {
         Vec3 dir = randomDir(r);
         particle(mc, crust(w.model, r).state(), w.center.add(dir.scale(w.radius * 0.9)), dir.scale(0.2 + 0.2 * r.nextDouble()));
      }
   }

   private static void crumble(Minecraft mc, GravityWellRenderer.Well w, float now) {
      RandomSource r = mc.level.random;
      loose(mc, w, now, 0.3F, (at, out) -> out.scale(0.04 + 0.05 * r.nextDouble()), 0.04F, 5);

      for (int i = 0; i < 16; i++) {
         Vec3 dir = randomDir(r);
         particle(mc, crust(w.model, r).state(), w.center.add(dir.scale(w.radius * 0.7)), dir.scale(0.08));
      }
   }

   private static void loose(Minecraft mc, GravityWellRenderer.Well w, float now, float share, GravityWellRenderer.Fling fling, float g, int stagger) {
      RandomSource r = mc.level.random;
      int light = light(mc.level, w.center);
      float vs = w.voxel() * formScale(w, now);
      Quaternionf q = new Quaternionf();
      Vector3f off = new Vector3f();
      Vector3f p = new Vector3f();

      for (PlanetModel.Layer layer : w.model.layers) {
         frame(w, layer, now, q, off);

         for (int i = 0; i < layer.count && BITS.size() < 2400; i++) {
            if (!(share < 1.0F) || !(r.nextFloat() > share)) {
               q.transform(p.set(layer.x(i), layer.y(i), layer.z(i))).add(off).mul(vs);
               Vec3 at = w.center.add((double)p.x, (double)p.y, (double)p.z);
               Vec3 out = at.subtract(w.center);
               out = out.lengthSqr() < 1.0E-6 ? randomDir(r) : out.normalize();
               PlanetModel.Mat m = w.model.mats.get(layer.mat(i));
               BITS.add(
                  new GravityWellRenderer.Debris(
                     at,
                     fling.velocity(at, out),
                     null,
                     g,
                     now + (float)(stagger > 0 ? r.nextInt(stagger) : 0),
                     (float)(12 + r.nextInt(8)),
                     vs * 0.8F,
                     r,
                     spin(r),
                     PlanetDraw.sprite(m),
                     m.tint(),
                     lightOf(m, light)
                  )
               );
            }
         }
      }
   }

   private static void particle(Minecraft mc, BlockState state, Vec3 at, Vec3 v) {
      SteeredParticle.block(state, at, v);
   }

   private static PlanetModel.Mat crust(PlanetModel model, RandomSource r) {
      return model.mats.get(model.crust.get(r.nextInt(model.crust.size())));
   }

   private static int lightOf(PlanetModel.Mat m, int world) {
      return m.emissive() ? 15728880 : world;
   }

   private static float spin(RandomSource r) {
      return (float)Math.toRadians((12.0 + 20.0 * r.nextDouble()) * (double)(r.nextBoolean() ? 1 : -1));
   }

   private static Vec3 randomDir(RandomSource r) {
      double y = r.nextDouble() * 2.0 - 1.0;
      double a = r.nextDouble() * Math.PI * 2.0;
      double s = Math.sqrt(1.0 - y * y);
      return new Vec3(Math.cos(a) * s, y, Math.sin(a) * s);
   }

   private static int light(ClientLevel level, Vec3 at) {
      int packed = LevelRenderer.getLightColor(level, BlockPos.containing(at));
      return LightTexture.pack(Math.max(LightTexture.block(packed), 7), LightTexture.sky(packed));
   }

   private static void frame(GravityWellRenderer.Well w, PlanetModel.Layer layer, float t, Quaternionf rot, Vector3f off) {
      PlanetModel m = w.model;
      float age = t - w.formAt;
      float turn = (float)Math.toRadians(SPIN.get()) * age + (float)(PlanetModel.hash01(m.seed, 93) * Math.PI * 2.0);
      rot.identity().rotateY(m.tiltYaw).rotateX(m.tilt);
      if (layer.orbit == 0.0F) {
         rot.rotateY(turn * layer.spin);
         off.set(0.0F, 0.0F, 0.0F);
      } else {
         rot.rotateX(layer.incline).rotateY(layer.phase + layer.orbitRate * age);
         rot.transform(off.set(layer.orbit, 0.0F, 0.0F));
         rot.rotateY(turn * layer.spin);
      }
   }

   private static float formScale(GravityWellRenderer.Well w, float t) {
      float k = (t - w.formAt - (float)FORM.get()) / 4.0F;
      return !(k <= 0.0F) && !(k >= 1.0F) ? 1.0F + 0.14F * Mth.sin(k * (float) Math.PI) : 1.0F;
   }

   @SubscribeEvent
   public static void onRender(RenderLevelStageEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && event.getStage() == Stage.AFTER_TRANSLUCENT_BLOCKS) {
         float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
         draw(mc, event.getPoseStack(), event.getCamera().getPosition(), pt, true, true);
      }
   }

   private static void draw(Minecraft mc, PoseStack poseStack, Vec3 cam, float pt, boolean solidPass, boolean lightPass) {
      GravityWellMotion.Target aim = GravityWellClient.aim();
      if (!LOCALS.isEmpty() || aim != null || !BITS.isEmpty()) {
         float t = (float)(mc.level.getGameTime() % 100000L) + pt;
         boolean firstPerson = mc.options.getCameraType() == CameraType.FIRST_PERSON;
         BufferSource buffers = mc.renderBuffers().bufferSource();
         poseStack.pushPose();
         poseStack.translate(-cam.x, -cam.y, -cam.z);
         if (solidPass) {
            drawSolid(mc, poseStack, buffers, cam, t);
         }

         if (lightPass) {
            drawLight(mc, poseStack, buffers, cam, t, pt, aim, firstPerson);
         }

         poseStack.popPose();
      }
   }

   private static void drawSolid(Minecraft mc, PoseStack poseStack, BufferSource buffers, Vec3 cam, float t) {
      RenderType solid = PlanetDraw.layer();
      VertexConsumer blocks = buffers.getBuffer(solid);

      for (GravityWellRenderer.Local local : LOCALS.values()) {
         if (local.live != null) {
            planet(mc, poseStack, blocks, cam, local.live, t);
         }

         for (GravityWellRenderer.Well w : local.dying) {
            planet(mc, poseStack, blocks, cam, w, t);
         }
      }

      debris(poseStack, blocks, cam, t);
      buffers.endBatch(solid);
   }

   private static void drawLight(
      Minecraft mc, PoseStack poseStack, BufferSource buffers, Vec3 cam, float t, float pt, GravityWellMotion.Target aim, boolean firstPerson
   ) {
      Pose pose = poseStack.last();
      FxLayer glow = FxLayer.LIGHT;

      for (int pass = 0; pass < 2; pass++) {
         boolean light = pass == 1;
         VertexConsumer vc = FxLayers.begin(buffers, light ? glow : FxLayer.PAINT);

         for (Entry<Integer, GravityWellRenderer.Local> e : LOCALS.entrySet()) {
            boolean own = firstPerson && mc.player != null && e.getKey() == mc.player.getId();
            GravityWellRenderer.Local local = e.getValue();
            if (light) {
               if (local.live != null) {
                  aura(pose, vc, cam, local.live, t);
               }

               for (GravityWellRenderer.Well w : local.dying) {
                  aura(pose, vc, cam, w, t);
               }
            }

            Player p = mc.level.getEntity(e.getKey()) instanceof Player pl ? pl : null;
            if (p != null && !local.trail.isEmpty()) {
               trail(pose, vc, cam, local, p.getPosition(pt).add(0.0, 0.9, 0.0), t, own, light);
            }
         }

         if (aim != null) {
            aimMark(pose, vc, cam, aim, t, light);
         }

         FxLayers.end(buffers, light ? glow : FxLayer.PAINT);
      }
   }

   private static float size(GravityWellRenderer.Well w, float t) {
      if (Float.isNaN(w.endAt)) {
         return formScale(w, t);
      } else if (w.endKind != 0) {
         return 0.0F;
      } else {
         float k = Mth.clamp((t - w.endAt) / 10.0F, 0.0F, 1.0F);
         return (1.0F - k * k) * (1.0F - 0.15F * k);
      }
   }

   private static void planet(Minecraft mc, PoseStack poseStack, VertexConsumer vc, Vec3 cam, GravityWellRenderer.Well w, float t) {
      if (w.model != null) {
         float size = size(w, t);
         if (!(size < 0.02F) && !(cam.distanceToSqr(w.center) < Math.pow(w.radius * (double)size + 0.35, 2.0))) {
            float vs = w.voxel() * size;
            float form = (float)FORM.get();
            float age = t - w.formAt;
            int worldLight = light(mc.level, w.center);
            int mats = w.model.mats.size();
            TextureAtlasSprite[] sprites = new TextureAtlasSprite[mats];

            for (int i = 0; i < mats; i++) {
               sprites[i] = PlanetDraw.sprite(w.model.mats.get(i));
            }

            Quaternionf q = new Quaternionf();
            Vector3f off = new Vector3f();
            poseStack.pushPose();
            poseStack.translate(w.center.x, w.center.y, w.center.z);
            poseStack.scale(vs, vs, vs);

            for (PlanetModel.Layer layer : w.model.layers) {
               frame(w, layer, t, q, off);
               poseStack.pushPose();
               poseStack.translate(off.x, off.y, off.z);
               poseStack.mulPose(q);
               if (layer.thick != 1.0F) {
                  poseStack.scale(1.0F, layer.thick, 1.0F);
               }

               Pose pose = poseStack.last();

               for (int i = 0; i < layer.count; i++) {
                  float x = layer.x(i);
                  float y = layer.y(i);
                  float z = layer.z(i);
                  float half = 0.5F;
                  int faces = layer.faces(i);
                  if (age < form) {
                     double delay = PlanetModel.hash01(w.model.seed, 7000 + i) * (double)form * 0.45;
                     float k = Mth.clamp((float)(((double)age - delay) / ((double)form * 0.55)), 0.0F, 1.0F);
                     if (k <= 0.0F) {
                        continue;
                     }

                     float out = 1.0F + 2.4F * (1.0F - k) * (1.0F - k) * (1.0F - k);
                     x *= out;
                     y *= out;
                     z *= out;
                     half *= 0.45F + 0.55F * k;
                     faces = 63;
                  }

                  PlanetModel.Mat m = w.model.mats.get(layer.mat(i));
                  PlanetDraw.cube(pose, vc, q, x, y, z, half, faces, sprites[layer.mat(i)], m.tint(), lightOf(m, worldLight));
               }

               poseStack.popPose();
            }

            poseStack.popPose();
         }
      }
   }

   private static void debris(PoseStack poseStack, VertexConsumer vc, Vec3 cam, float t) {
      Quaternionf q = new Quaternionf();

      for (GravityWellRenderer.Debris d : BITS) {
         float age = t - d.t0;
         if (!(age < 0.0F) && !(age > d.life)) {
            float s = d.size * d.scale(age);
            Vec3 at = d.at(age);
            if (!(s < 0.005F) && !(at.distanceToSqr(cam) < 0.81)) {
               q.identity().rotateAxis(d.spin * age, d.axis.x, d.axis.y, d.axis.z);
               poseStack.pushPose();
               poseStack.translate(at.x, at.y, at.z);
               poseStack.mulPose(q);
               PlanetDraw.cube(poseStack.last(), vc, q, 0.0F, 0.0F, 0.0F, s * 0.5F, 63, d.sprite, d.tint, d.light);
               poseStack.popPose();
            }
         }
      }
   }

   private static void aura(Pose pose, VertexConsumer vc, Vec3 cam, GravityWellRenderer.Well w, float t) {
      if (w.model != null) {
         float size = size(w, t);
         float formK = Mth.clamp((t - w.formAt) / (float)FORM.get(), 0.0F, 1.0F);
         int glow = w.model.kind.glow;
         if (glow != 0 && size > 0.02F) {
            float a = (float)HALO.get() * formK * Math.min(1.0F, size) * (0.9F + 0.1F * Mth.sin(t * 0.3F));
            halo(pose, vc, cam, w.center, w.radius * (double)size * 0.75, w.radius * (double)size * 2.3, a, glow);
         }

         if (Float.isNaN(w.endAt) && !(formK < 1.0F)) {
            double half = Math.max(LINE.get(), cam.distanceTo(w.center) * LINE_PX.get());
            float base = (float)PULL_RING.get();
            Vec3 up = new Vec3(0.0, 1.0, 0.0);
            Vec3 u = new Vec3(1.0, 0.0, 0.0);
            Vec3 v = up.cross(u);

            for (int i = 0; i < 2; i++) {
               float k = frac((t - w.formAt) / 24.0F + (float)i * 0.5F);
               float a = base * Mth.sin(k * (float) Math.PI);
               if (!(a < 0.01F)) {
                  Vec3[] ring = circle(w.center, u, v, w.radius * (2.7 - 1.6 * (double)k * (double)k), 40);
                  GravitonDraw.ribbonLoop(pose, vc, ring, ones(ring.length), cam, half, a, 0.2F, 16769732, true);
               }
            }
         }
      }
   }

   private static void halo(Pose pose, VertexConsumer vc, Vec3 cam, Vec3 c, double inner, double outer, float alpha, int rgb) {
      Vec3 n = cam.subtract(c).normalize();
      Vec3 u = Math.abs(n.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
      u = u.subtract(n.scale(u.dot(n))).normalize();
      Vec3 v = n.cross(u);
      int segs = 32;
      int cIn = GravitonDraw.argb(alpha, rgb);
      int cOut = GravitonDraw.argb(0.0F, rgb);

      for (int i = 0; i < segs; i++) {
         double a0 = (Math.PI * 2) * (double)i / (double)segs;
         double a1 = (Math.PI * 2) * (double)(i + 1) / (double)segs;
         Vec3 d0 = u.scale(Math.cos(a0)).add(v.scale(Math.sin(a0)));
         Vec3 d1 = u.scale(Math.cos(a1)).add(v.scale(Math.sin(a1)));
         GravitonDraw.quad(pose, vc, c.add(d0.scale(inner)), c.add(d0.scale(outer)), c.add(d1.scale(outer)), c.add(d1.scale(inner)), cIn, cOut, true);
      }
   }

   private static void trail(Pose pose, VertexConsumer vc, Vec3 cam, GravityWellRenderer.Local local, Vec3 head, float t, boolean own, boolean light) {
      List<GravityWellRenderer.Sample> samples = new ArrayList<>(local.trail.size() + 1);
      samples.add(new GravityWellRenderer.Sample(head, t, false));

      for (GravityWellRenderer.Sample s : local.trail) {
         if (!(s.t() > t) && s.pos().distanceToSqr(samples.get(samples.size() - 1).pos()) > 1.0E-4) {
            samples.add(s);
         }
      }

      int n = samples.size();
      if (n >= 3) {
         float base = (float)TRAIL_ALPHA.get();
         Vec3[] mid = new Vec3[n];
         Vec3[] left = new Vec3[n];
         Vec3[] right = new Vec3[n];
         float[] fade = new float[n];
         float[] heat = new float[n];

         for (int i = 0; i < n; i++) {
            GravityWellRenderer.Sample sx = samples.get(i);
            Vec3 dir = samples.get(Math.max(0, i - 1)).pos().subtract(samples.get(Math.min(n - 1, i + 1)).pos());
            Vec3 side = dir.cross(new Vec3(0.0, 1.0, 0.0));
            side = side.lengthSqr() < 1.0E-8 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
            Vec3 up = side.cross(dir).lengthSqr() < 1.0E-8 ? new Vec3(0.0, 1.0, 0.0) : side.cross(dir).normalize();
            mid[i] = sx.pos();
            left[i] = sx.pos().add(side.scale(0.3)).add(up.scale(0.42));
            right[i] = sx.pos().subtract(side.scale(0.3)).add(up.scale(0.42));
            float age = Mth.clamp((t - sx.t()) / 12.0F, 0.0F, 1.0F);
            float f = (1.0F - age) * (1.0F - age) * (i == 0 ? 0.0F : 1.0F);
            if (own) {
               f *= (float)smooth(1.6, 3.6, cam.distanceTo(sx.pos()));
            }

            fade[i] = f;
            heat[i] = sx.bend() ? 1.0F : 0.0F;
         }

         double dist = cam.distanceTo(head);
         double half = Math.max(LINE.get(), dist * LINE_PX.get());
         if (light) {
            GravitonDraw.ribbon(pose, vc, mid, fade, cam, 0.0, 0.26, base * 0.35F, 0.0F, 16769732, 16769732, true);
            GravitonDraw.ribbon(pose, vc, left, fade, cam, 0.0, half * 0.9, base, base * 0.3F, 16769732, 16769732, true);
            GravitonDraw.ribbon(pose, vc, right, fade, cam, 0.0, half * 0.9, base, base * 0.3F, 16769732, 16769732, true);
            bend(pose, vc, cam, mid, fade, heat, base);
         } else {
            GravitonDraw.ribbon(pose, vc, left, fade, cam, 0.0, half * 2.0, base * 0.25F, 0.0F, 1971474, 1971474, false);
            GravitonDraw.ribbon(pose, vc, right, fade, cam, 0.0, half * 2.0, base * 0.25F, 0.0F, 1971474, 1971474, false);
         }
      }
   }

   private static void bend(Pose pose, VertexConsumer vc, Vec3 cam, Vec3[] mid, float[] fade, float[] heat, float base) {
      float[] f = new float[mid.length];
      boolean any = false;

      for (int i = 0; i < mid.length; i++) {
         f[i] = fade[i] * heat[i];
         any |= f[i] > 0.0F;
      }

      if (any) {
         GravitonDraw.ribbon(pose, vc, mid, f, cam, 0.0, 0.16, base * 1.2F, 0.0F, 16747069, 16747069, true);
      }
   }

   private static void aimMark(Pose pose, VertexConsumer vc, Vec3 cam, GravityWellMotion.Target aim, float t, boolean light) {
      Vec3 n = GravityWellVisual.axis(aim.face());
      Vec3 c = GravityWellVisual.throat(aim.point(), aim.face()).add(n.scale(0.03));
      Vec3 u = Math.abs(n.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
      u = u.subtract(n.scale(u.dot(n))).normalize();
      Vec3 v = n.cross(u);
      double half = Math.max(LINE.get(), cam.distanceTo(c) * LINE_PX.get());
      double r = 0.5 + half * 3.0;
      Vec3[] ring = circle(c, u, v, r, 32);
      float a = 0.55F + 0.1F * Mth.sin(t * 0.6F);
      if (light) {
         GravitonDraw.ribbonLoop(pose, vc, ring, ones(32), cam, half, a, 0.4F, 16769732, true);
      } else {
         GravitonDraw.ribbonLoop(pose, vc, ring, ones(32), cam, half * 2.4, a * 0.4F, 0.0F, 1971474, false);
      }

      for (int k = 0; k < 4; k++) {
         double ang = (Math.PI / 2) * (double)k + (Math.PI / 4);
         Vec3 d = u.scale(Math.cos(ang)).add(v.scale(Math.sin(ang)));
         Vec3[] tick = new Vec3[]{c.add(d.scale(r * 1.45)), c.add(d.scale(r * 1.1))};
         float[] f = new float[]{1.0F, 1.0F};
         if (light) {
            GravitonDraw.ribbon(pose, vc, tick, f, cam, 0.0, half, a, a * 0.4F, 16769732, 16769732, true);
         }
      }
   }

   private static Vec3[] circle(Vec3 c, Vec3 u, Vec3 v, double r, int segs) {
      Vec3[] pts = new Vec3[segs];

      for (int i = 0; i < segs; i++) {
         double a = (Math.PI * 2) * (double)i / (double)segs;
         pts[i] = c.add(u.scale(Math.cos(a) * r)).add(v.scale(Math.sin(a) * r));
      }

      return pts;
   }

   private static float[] ones(int n) {
      float[] f = new float[n];
      Arrays.fill(f, 1.0F);
      return f;
   }

   private static float frac(float x) {
      return x - (float)Math.floor((double)x);
   }

   private static double smooth(double e0, double e1, double x) {
      double k = Mth.clamp((x - e0) / (e1 - e0), 0.0, 1.0);
      return k * k * (3.0 - 2.0 * k);
   }

   private static final class Debris {
      final Vec3 p;
      final Vec3 v;
      final Vec3 target;
      final float g;
      final float t0;
      final float life;
      final float size;
      final Vector3f axis;
      final float spin;
      final TextureAtlasSprite sprite;
      final int tint;
      final int light;

      Debris(Vec3 p, Vec3 v, Vec3 target, float g, float t0, float life, float size, RandomSource r, float spin, TextureAtlasSprite sprite, int tint, int light) {
         this.p = p;
         this.v = v;
         this.target = target;
         this.g = g;
         this.t0 = t0;
         this.life = life;
         this.size = size;
         this.axis = new Vector3f(r.nextFloat() - 0.5F, r.nextFloat() - 0.5F, r.nextFloat() - 0.5F).normalize();
         this.spin = spin;
         this.sprite = sprite;
         this.tint = tint;
         this.light = light;
      }

      Vec3 at(float age) {
         if (this.target != null) {
            float k = Mth.clamp(age / this.life, 0.0F, 1.0F);
            return this.p.add(this.target.subtract(this.p).scale((double)(k * k)));
         } else {
            return this.p.add(this.v.scale((double)age)).add(0.0, -0.5 * (double)this.g * (double)age * (double)age, 0.0);
         }
      }

      float scale(float age) {
         float k = Mth.clamp(age / this.life, 0.0F, 1.0F);
         return (float)(1.0 - GravityWellRenderer.smooth(this.target != null ? 0.75 : 0.55, 1.0, (double)k));
      }
   }

   private interface Fling {
      Vec3 velocity(Vec3 var1, Vec3 var2);
   }

   private static final class Local {
      boolean seen;
      int opens;
      int ends;
      GravityWellRenderer.Well live;
      final List<GravityWellRenderer.Well> dying = new ArrayList<>();
      final ArrayDeque<GravityWellRenderer.Sample> trail = new ArrayDeque<>();
      float swapAt = -1000.0F;
      float trailUntil = -1000.0F;
   }

   private static record Sample(Vec3 pos, float t, boolean bend) {
   }

   private static final class Well {
      final Vec3 point;
      final Direction face;
      final boolean surface;
      int planet = -1;
      PlanetModel model;
      Vec3 center;
      double radius;
      float formAt;
      float endAt = Float.NaN;
      int endKind;
      float debrisDue;

      Well(ClientLevel level, GravityWellVisual v, float bornAt) {
         this.point = v.point();
         this.face = v.face();
         this.surface = v.surface();
         this.setPlanet(level, v.planet(), bornAt);
      }

      void setPlanet(ClientLevel level, int next, float now) {
         this.planet = next;
         if (!GravityWellPlanet.known(next)) {
            this.model = null;
         } else {
            this.model = PlanetModel.build(next, GravityWellRenderer.VOXELS.getI());
            this.radius = GravityWellPlanet.kind(next).radius(GravityWellRenderer.RADIUS.get());
            this.center = GravityWellPlanet.center(level, this.point, this.face, this.surface, this.radius);
            this.formAt = now;
         }
      }

      float voxel() {
         return (float)(this.radius / (double)this.model.halfAcross);
      }

      boolean formed(float t) {
         return this.model != null && (double)(t - this.formAt) >= GravityWellRenderer.FORM.get();
      }
   }
}
