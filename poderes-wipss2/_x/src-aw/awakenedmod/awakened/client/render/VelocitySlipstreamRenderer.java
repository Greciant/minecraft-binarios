package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.velocity.VelocityPalette;
import com.awakenedmod.awakened.content.velocity.VelocityVisualState;
import com.awakenedmod.awakened.core.data.VelocityVisual;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import org.joml.Vector3f;

public final class VelocitySlipstreamRenderer {
   public static final int HISTORY_TICKS = 24;
   private static final int[] RIBBONS_BY_GEAR = new int[]{0, 2, 3, 4, 5, 6};
   private static final float WIDTH_GEAR1 = 0.1F;
   private static final float WIDTH_GEAR5 = 0.18F;
   private static final float TAIL_TICKS_PER_BPS = 2.5F;
   private static final float TAIL_TICKS_MIN = 4.0F;
   private static final float TAIL_GEAR_STRETCH = 0.3F;
   private static final float TAIL_WIDTH_FRAC = 0.35F;
   private static final float[] BODY_ALPHA_BY_GEAR = new float[]{0.0F, 0.17F, 0.2F, 0.23F, 0.25F, 0.28F};
   private static final float EMBER_START = 0.66F;
   private static final int CORE_FROM_GEAR = 1;
   private static final float CORE_WIDTH_FRAC = 0.6F;
   private static final float[] CORE_ALPHA_BY_GEAR = new float[]{0.0F, 95.0F, 110.0F, 125.0F, 140.0F, 155.0F};
   private static final float CORE_COOL_AT = 0.35F;
   private static final float WOBBLE = 0.06F;
   private static final float WOBBLE_WAVES = 2.2F;
   private static final float WOBBLE_RATE = 0.55F;
   private static final float WOBBLE_PHASE_STEP = 2.1F;
   private static final float[][] ANCHORS = new float[][]{
      {0.16F, 0.1F, 0.06F}, {-0.16F, 0.1F, 0.06F}, {0.0F, 0.88F, 0.1F}, {0.24F, 0.95F, 0.02F}, {-0.24F, 0.95F, 0.02F}, {0.0F, 1.3F, 0.12F}
   };
   private static final int FORK_GEAR = 3;
   private static final int CRACKLE_GEAR = 5;
   private static final int FORK_WINDOW_TICKS = 2;
   private static final int FORK_MIN = 1;
   private static final int FORK_MAX = 2;
   private static final int FORK_SEGS_MIN = 3;
   private static final int FORK_SEGS_MAX = 5;
   private static final float FORK_OFF_MIN = 0.6F;
   private static final float FORK_OFF_MAX = 1.2F;
   private static final float FORK_LIFE_TICKS = 3.0F;
   private static final float FORK_HALF = 0.015F;
   private static final float FORK_JITTER = 0.2F;
   private static final float FORK_ALPHA = 140.0F;
   private static final float FORK_TIP_LIGHTEN = 0.12F;
   private static final int FORK_ROOT_AGE_MIN = 1;
   private static final int FORK_ROOT_AGE_MAX = 12;
   private static final Map<Integer, Deque<VelocitySlipstreamRenderer.Sample>> TRAILS = new HashMap<>();
   private static ClientLevel trailLevel;

   private VelocitySlipstreamRenderer() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(VelocitySlipstreamRenderer::onClientTick);
      gameBus.addListener(event -> {
         if (event.getStage() == Stage.AFTER_TRANSLUCENT_BLOCKS) {
            draw(event);
         }
      });
   }

   public static Deque<VelocitySlipstreamRenderer.Sample> trail(int entityId) {
      Deque<VelocitySlipstreamRenderer.Sample> trail = TRAILS.get(entityId);
      return (Deque<VelocitySlipstreamRenderer.Sample>)(trail != null ? trail : new ArrayDeque<>());
   }

   private static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != trailLevel) {
         TRAILS.clear();
         trailLevel = mc.level;
      }

      if (mc.level != null && !mc.isPaused()) {
         long now = mc.level.getGameTime();

         for (Player p : mc.level.players()) {
            VelocityVisual vis = VelocityVisualState.of(p);
            if (vis.tunnel() || !vis.trail()) {
               TRAILS.remove(p.getId());
            } else if (vis.running()) {
               Deque<VelocitySlipstreamRenderer.Sample> trail = TRAILS.computeIfAbsent(p.getId(), k -> new ArrayDeque<>());
               trail.addLast(new VelocitySlipstreamRenderer.Sample(p.position(), p.yBodyRot, vis.gear(), now));
            }
         }

         Iterator<Entry<Integer, Deque<VelocitySlipstreamRenderer.Sample>>> it = TRAILS.entrySet().iterator();

         while (it.hasNext()) {
            Deque<VelocitySlipstreamRenderer.Sample> trail = it.next().getValue();

            while (!trail.isEmpty() && now - trail.peekFirst().tick() > 24L) {
               trail.pollFirst();
            }

            if (trail.isEmpty()) {
               it.remove();
            }
         }
      }
   }

   private static void draw(RenderLevelStageEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && !TRAILS.isEmpty()) {
         float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
         Vec3 cam = event.getCamera().getPosition();
         PoseStack poseStack = event.getPoseStack();
         if (poseStack != null) {
            long now = mc.level.getGameTime();
            float time = (float)now + partialTick;
            boolean firstPerson = mc.options.getCameraType().isFirstPerson();
            Vector3f camPos = new Vector3f((float)cam.x, (float)cam.y, (float)cam.z);
            List<VelocitySlipstreamRenderer.Ribbons> visible = new ArrayList<>();

            for (Entry<Integer, Deque<VelocitySlipstreamRenderer.Sample>> e : TRAILS.entrySet()) {
               Entity entity = mc.level.getEntity(e.getKey());
               if (entity instanceof Player) {
                  Player p = (Player)entity;
                  if (p != mc.player || !firstPerson) {
                     VelocitySlipstreamRenderer.Path path = build(p, e.getValue(), now, partialTick, VelocityPalette.of(p));
                     if (path != null) {
                        visible.add(prepare(p.getId(), path, camPos, time));
                     }
                  }
               }
            }

            if (!visible.isEmpty()) {
               BufferSource buffers = mc.renderBuffers().bufferSource();
               VfxDraw.Sink sink = new VfxDraw.Sink(buffers);
               poseStack.pushPose();
               poseStack.translate(-cam.x, -cam.y, -cam.z);
               VfxDraw.custom(poseStack, sink, FxLayer.PAINT, (pose, vc) -> {
                  for (VelocitySlipstreamRenderer.Ribbons r : visible) {
                     strip(pose, vc, r, 1.0F, false, colours(r, false));
                  }
               });
               buffers.endBatch();
               VfxDraw.additive(poseStack, sink, (pose, vc) -> {
                  for (VelocitySlipstreamRenderer.Ribbons r : visible) {
                     if (r.path().gear() >= 1) {
                        strip(pose, vc, r, 0.6F, true, colours(r, true));
                     }

                     if (r.path().gear() >= 3) {
                        drawForks(pose, vc, r.playerId(), r.path(), r.centres(), now, partialTick);
                     }
                  }
               });
               buffers.endBatch();
               poseStack.popPose();
            }
         }
      }
   }

   private static VelocitySlipstreamRenderer.Path build(
      Player p, Deque<VelocitySlipstreamRenderer.Sample> trail, long now, float partialTick, VelocityPalette palette
   ) {
      VelocitySlipstreamRenderer.Sample newest = trail.peekLast();
      if (newest == null) {
         return null;
      } else {
         int gear = Math.max(0, Math.min(RIBBONS_BY_GEAR.length - 1, newest.gear()));
         if (RIBBONS_BY_GEAR[gear] == 0) {
            return null;
         } else {
            boolean glued = newest.tick() >= now;
            int cap = trail.size() + 1;
            Vec3[] pos = new Vec3[cap];
            float[] yaw = new float[cap];
            float[] age = new float[cap];
            long[] tick = new long[cap];
            int n = 0;
            float time = (float)now + partialTick;
            double speedPerTick;
            if (glued) {
               pos[n] = p.getPosition(partialTick);
               yaw[n] = Mth.rotLerp(partialTick, p.yBodyRotO, p.yBodyRot);
               age[n] = 0.0F;
               tick[n] = now;
               n++;
               speedPerTick = p.position().distanceTo(new Vec3(p.xo, p.yo, p.zo));
            } else {
               speedPerTick = -1.0;
            }

            Iterator<VelocitySlipstreamRenderer.Sample> it = trail.descendingIterator();

            while (it.hasNext()) {
               VelocitySlipstreamRenderer.Sample s = it.next();
               if (!glued || s.tick() < now) {
                  pos[n] = s.pos();
                  yaw[n] = s.bodyYaw();
                  age[n] = Math.max(0.0F, time - (float)s.tick() - 1.0F);
                  tick[n] = s.tick();
                  n++;
               }
            }

            if (n < 2) {
               return null;
            } else {
               if (speedPerTick < 0.0) {
                  speedPerTick = pos[0].distanceTo(pos[1]) / (double)Math.max(1L, tick[0] - tick[1]);
               }

               float gearStretch = 1.0F + 0.3F * (float)(gear - 1) / ((float)RIBBONS_BY_GEAR.length - 2.0F);
               float visibleTicks = Mth.clamp((float)(2.5 * speedPerTick * 20.0) * gearStretch, 4.0F, 24.0F);
               int count = 0;
               float[] t = new float[n];

               for (int i = 0; i < n; i++) {
                  t[i] = Math.min(1.0F, age[i] / visibleTicks);
                  count++;
                  if (age[i] >= visibleTicks) {
                     break;
                  }
               }

               return count < 2 ? null : new VelocitySlipstreamRenderer.Path(pos, yaw, t, tick, count, gear, palette);
            }
         }
      }
   }

   private static VelocitySlipstreamRenderer.Ribbons prepare(int playerId, VelocitySlipstreamRenderer.Path path, Vector3f cam, float time) {
      int gear = path.gear();
      int ribbons = RIBBONS_BY_GEAR[gear];
      int n = path.count();
      float halfWidth = 0.5F * Mth.lerp((float)(gear - 1) / 4.0F, 0.1F, 0.18F);
      Vector3f[][] centres = new Vector3f[ribbons][n];
      Vector3f[][] sides = new Vector3f[ribbons][n];

      for (int r = 0; r < ribbons; r++) {
         float[] anchor = ANCHORS[r];
         float phase = (float)r * 2.1F;
         Vector3f[] centre = centres[r];

         for (int i = 0; i < n; i++) {
            double yawRad = Math.toRadians((double)path.yawDeg()[i]);
            float rx = (float)(-Math.cos(yawRad));
            float rz = (float)(-Math.sin(yawRad));
            float fx = (float)(-Math.sin(yawRad));
            float fz = (float)Math.cos(yawRad);
            float wob = 0.06F * (float)Math.sin(13.823007975400712 * (double)path.t()[i] + (double)phase + (double)(time * 0.55F));
            float side = anchor[0] + wob;
            Vec3 p = path.pos()[i];
            centre[i] = new Vector3f((float)p.x + rx * side - fx * anchor[2], (float)p.y + anchor[1], (float)p.z + rz * side - fz * anchor[2]);
         }

         Vector3f prevSide = null;

         for (int i = 0; i < n; i++) {
            Vector3f dir = new Vector3f(centre[Math.min(i + 1, n - 1)]).sub(centre[Math.max(i - 1, 0)]);
            Vector3f toCam = new Vector3f(cam).sub(centre[i]);
            Vector3f side = dir.cross(toCam);
            if (side.lengthSquared() < 1.0E-8F) {
               side = prevSide != null ? new Vector3f(prevSide) : new Vector3f(1.0F, 0.0F, 0.0F);
            } else {
               side.normalize();
            }

            sides[r][i] = side;
            prevSide = side;
         }
      }

      return new VelocitySlipstreamRenderer.Ribbons(playerId, path, centres, sides, halfWidth);
   }

   private static void strip(Pose pose, VertexConsumer vc, VelocitySlipstreamRenderer.Ribbons r, float widthFrac, boolean twoSided, int[] colours) {
      float[] t = r.path().t();
      int n = r.path().count();

      for (int b = 0; b < r.centres().length; b++) {
         Vector3f[] centre = r.centres()[b];
         Vector3f[] side = r.sides()[b];

         for (int i = 0; i + 1 < n; i++) {
            float w0 = r.halfWidth() * widthFrac * (1.0F - 0.65F * t[i]);
            float w1 = r.halfWidth() * widthFrac * (1.0F - 0.65F * t[i + 1]);
            Vector3f a0 = new Vector3f(side[i]).mul(w0).add(centre[i]);
            Vector3f a1 = new Vector3f(side[i]).mul(-w0).add(centre[i]);
            Vector3f b0 = new Vector3f(side[i + 1]).mul(w1).add(centre[i + 1]);
            Vector3f b1 = new Vector3f(side[i + 1]).mul(-w1).add(centre[i + 1]);
            VelocityStreaks.quad(pose, vc, a0, colours[i], b0, colours[i + 1], b1, colours[i + 1], a1, colours[i], twoSided);
         }
      }
   }

   private static int[] colours(VelocitySlipstreamRenderer.Ribbons r, boolean core) {
      int gear = r.path().gear();
      VelocityPalette palette = r.path().palette();
      int[] colours = new int[r.path().count()];

      for (int i = 0; i < colours.length; i++) {
         float t = r.path().t()[i];
         colours[i] = core ? coreColour(palette, gear, t) : bodyColour(palette, gear, t);
      }

      return colours;
   }

   private static int bodyColour(VelocityPalette palette, int gear, float t) {
      int rgb = t < 0.66F ? palette.body() : VelocityStreaks.lerpRgb(palette.body(), palette.ember(), (t - 0.66F) / 0.33999997F);
      return VfxColor.argb((int)(255.0F * BODY_ALPHA_BY_GEAR[gear] * (1.0F - t * t)), rgb);
   }

   private static int coreColour(VelocityPalette palette, int gear, float t) {
      int rgb = VelocityStreaks.lerpRgb(palette.hot(), palette.body(), t / 0.35F);
      float fall = 1.0F - t;
      return VfxColor.argb((int)(CORE_ALPHA_BY_GEAR[gear] * fall * fall), rgb);
   }

   private static void drawForks(
      Pose pose, VertexConsumer vc, int playerId, VelocitySlipstreamRenderer.Path path, Vector3f[][] centres, long now, float partialTick
   ) {
      float time = (float)now + partialTick;
      int ribbons = centres.length;
      int n = path.count();
      long newestWindow = Math.floorDiv(now, 2L);
      long oldestWindow = Math.floorDiv(now - (long)Math.ceil(3.0), 2L);
      int extra = path.gear() >= 5 ? 1 : 0;

      for (long w = newestWindow; w >= oldestWindow; w--) {
         float born = (float)(w * 2L);
         float forkAge = time - born;
         if (!(forkAge < 0.0F) && !(forkAge >= 3.0F)) {
            float fade = 1.0F - forkAge / 3.0F;
            int alpha = (int)(140.0F * fade * fade);
            if (alpha > 2) {
               int wSeed = (int)w * 31 + playerId;
               int forks = 1 + (VelocityStreaks.noise(0, 0, wSeed) < 0.5F ? 0 : 1) + extra;

               for (int k = 0; k < forks; k++) {
                  int r = Math.min(ribbons - 1, (int)(VelocityStreaks.noise(k, 1, wSeed) * (float)ribbons));
                  long rootTick = (long)born - 1L - (long)((int)(VelocityStreaks.noise(k, 2, wSeed) * 11.0F));
                  int idx = -1;

                  for (int i = 1; i < n; i++) {
                     if (path.tick()[i] == rootTick) {
                        idx = i;
                        break;
                     }
                  }

                  if (idx >= 0) {
                     Vector3f root = centres[r][idx];
                     Vector3f along = new Vector3f(centres[r][Math.max(idx - 1, 0)]).sub(centres[r][Math.min(idx + 1, n - 1)]);
                     if (along.lengthSquared() < 1.0E-8F) {
                        along.set(0.0F, 0.0F, 1.0F);
                     }

                     along.normalize();
                     Vector3f p1 = VelocityStreaks.perpendicular(along);
                     Vector3f p2 = new Vector3f(along).cross(p1).normalize();
                     double phi = (double)VelocityStreaks.noise(k, 3, wSeed) * Math.PI * 2.0;
                     float off = 0.6F + 0.6F * VelocityStreaks.noise(k, 4, wSeed);
                     Vector3f tip = new Vector3f(p1).mul((float)Math.cos(phi)).add(new Vector3f(p2).mul((float)Math.sin(phi))).mul(off).add(root);
                     int segs = 3 + (int)(VelocityStreaks.noise(k, 5, wSeed) * 3.0F);
                     int rootCol = VfxColor.argb(alpha, path.palette().hot());
                     int tipCol = VfxColor.argb(alpha, VfxColor.lighten(path.palette().body(), 0.12F));
                     VelocityStreaks.lightning(pose, vc, root, tip, 0.015F, Math.min(5, segs), 0.2F, wSeed * 7919 + k, rootCol, tipCol);
                  }
               }
            }
         }
      }
   }

   private static record Path(Vec3[] pos, float[] yawDeg, float[] t, long[] tick, int count, int gear, VelocityPalette palette) {
   }

   private static record Ribbons(int playerId, VelocitySlipstreamRenderer.Path path, Vector3f[][] centres, Vector3f[][] sides, float halfWidth) {
   }

   public static record Sample(Vec3 pos, float bodyYaw, int gear, long tick) {
   }
}
