package com.devilfruits.fruits.yomi_yomi.client;

import com.devilfruits.client.model.geo.GeoResources;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.fruits.yomi_yomi.YomiYomi;
import com.devilfruits.fruits.yomi_yomi.client.vfx.SoulFire;
import com.devilfruits.fruits.yomi_yomi.client.vfx.SoulKingAura;
import com.devilfruits.registry.DFAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class SoulKingGhost {
   private static final float SCALE = 0.0625F;
   private static final double BEHIND = 1.5;
   private static final double ASIDE = 1.3;
   private static final double SINK = 0.3;
   private static final YomiModel MODEL = new YomiModel("soul_king");
   private static final String[] BONES = new String[]{
      "head", "crown", "jaw", "chest", "l_forearm", "l_hand", "r_forearm", "r_hand", "tail_0", "tail_3", "neck", "body", "l_arm", "r_arm"
   };
   public static final int HEAD = 0;
   public static final int CROWN = 1;
   public static final int JAW = 2;
   public static final int CHEST = 3;
   public static final int L_FOREARM = 4;
   public static final int L_HAND = 5;
   public static final int R_FOREARM = 6;
   public static final int R_HAND = 7;
   public static final int BASE = 8;
   public static final int TAIL = 9;
   public static final int NECK = 10;
   public static final int BODY = 11;
   public static final int L_SHOULDER = 12;
   public static final int R_SHOULDER = 13;
   private static final Map<Integer, SoulKingGhost.Ghost> GHOSTS = new HashMap<>();
   private static int[] bones;
   private static int bonesGeneration = -1;
   private static final Vector3f[] LOCAL = new Vector3f[BONES.length];

   public static void play(int id, String clip) {
      SoulKingGhost.Ghost g = GHOSTS.computeIfAbsent(id, k -> new SoulKingGhost.Ghost());
      float now = VfxClock.time();
      if (g.action != null) {
         g.last = g.action;
         g.lastTime = now - g.actionStart;
         g.lastEnd = now;
      }

      g.action = clip;
      g.actionStart = now;
      if ("vanish".equals(clip)) {
         g.vanish = now;
      } else if ("summon".equals(clip)) {
         g.vanish = Float.NaN;
         g.last = null;
      }
   }

   @Nullable
   public static Vec3 point(int id, int which) {
      SoulKingGhost.Ghost g = GHOSTS.get(id);
      if (g != null && g.placed) {
         Vector3f p = g.points[which];
         return new Vec3((double)p.x, (double)p.y, (double)p.z);
      } else {
         return null;
      }
   }

   public static float yaw(Player player) {
      SoulKingGhost.Ghost g = GHOSTS.get(player.getId());
      return g == null ? player.getYHeadRot() : g.yaw;
   }

   public static Vec3 root(Vec3 feet, float yaw) {
      double yr = Math.toRadians((double)yaw);
      return feet.add(Math.sin(yr) * 1.5 + Math.cos(yr) * 1.3, 0.0, -Math.cos(yr) * 1.5 + Math.sin(yr) * 1.3);
   }

   public static boolean present(int id) {
      return GHOSTS.containsKey(id);
   }

   public static float fire(int id) {
      SoulKingGhost.Ghost g = GHOSTS.get(id);
      return g == null ? 0.0F : g.fire;
   }

   public static void clear() {
      GHOSTS.clear();
   }

   private static boolean shows(Player player) {
      return YomiYomi.isUser(player) && "soul_king".equals(DFAttachments.data(player).formName());
   }

   public static void tick(ClientLevel level) {
      float now = VfxClock.time();

      for (Player p : level.players()) {
         if (shows(p) && !GHOSTS.containsKey(p.getId())) {
            SoulKingGhost.Ghost g = new SoulKingGhost.Ghost();
            g.yaw = g.yawO = p.getYHeadRot();
            g.turned = true;
            GHOSTS.put(p.getId(), g);
         }
      }

      Iterator<Entry<Integer, SoulKingGhost.Ghost>> it = GHOSTS.entrySet().iterator();

      while (it.hasNext()) {
         Entity entity;
         Entry<Integer, SoulKingGhost.Ghost> e;
         SoulKingGhost.Ghost g;
         boolean var10000;
         label84: {
            e = it.next();
            g = e.getValue();
            entity = level.getEntity(e.getKey());
            if (entity instanceof Player px && shows(px)) {
               var10000 = true;
               break label84;
            }

            var10000 = false;
         }

         boolean alive = var10000;
         if (!alive && Float.isNaN(g.vanish)) {
            play(e.getKey(), "vanish");
         }

         if ((Float.isNaN(g.vanish) || !(now - g.vanish > 32.0F)) && (entity != null || Float.isNaN(g.vanish) || g.placed)) {
            if (entity instanceof Player px) {
               if (g.aura == null || g.aura.isDone()) {
                  g.aura = new SoulKingAura(px);
                  VfxDirector.spawn(g.aura);
               }

               g.feet = px.position();
               float target = px.getYHeadRot();
               if (!g.turned) {
                  g.yaw = g.yawO = target;
                  g.turned = true;
               }

               g.yawO = g.yaw;
               boolean striking = g.action != null && !"idle".equals(g.action) && now - g.actionStart < 12.0F;
               g.yaw = g.yaw + Mth.wrapDegrees(target - g.yaw) * (striking ? 0.55F : 0.22F);
            }

            if (VfxClock.advanced() && g.placed) {
               flames(g, now);
            }
         } else {
            it.remove();
         }
      }
   }

   private static void flames(SoulKingGhost.Ghost g, float now) {
      Minecraft mc = Minecraft.getInstance();
      Vector3f base = g.points[8];
      if (!(mc.gameRenderer.getMainCamera().getPosition().distanceToSqr((double)base.x, (double)base.y, (double)base.z) > 6400.0)) {
         RandomSource r = g.random;
         float fade;
         if (!Float.isNaN(g.vanish)) {
            fade = Mth.clamp(1.0F - (now - g.vanish - 8.0F) / 16.0F, 0.0F, 1.0F);
         } else if ("summon".equals(g.action)) {
            fade = Mth.clamp((now - g.actionStart) / 12.0F, 0.0F, 1.0F);
         } else {
            fade = 1.0F;
         }

         g.fire = fade;
         if (!(fade <= 0.0F)) {
            double yr = Math.toRadians((double)g.yaw);
            double bx = Math.sin(yr);
            double bz = -Math.cos(yr);
            double sx = -bz;
            double sz = bx;
            double floor = g.feet.y;
            Vec3 root = root(g.feet, g.yaw);
            double rootX = root.x;
            double rootZ = root.z;
            int light = 8191802;
            SoulFire.pool(r, rootX, floor, rootZ, 1.5, 3.0F * fade, 1.6F);

            for (int i = 0; i < VfxSettings.count(2.0F * fade); i++) {
               double a = r.nextDouble() * (float) (Math.PI * 2);
               double d = 0.3 + r.nextDouble() * 0.35;
               SoulFire.lick(
                     g.feet.x + Math.cos(a) * d,
                     floor + 0.1,
                     g.feet.z + Math.sin(a) * d,
                     0.0,
                     0.02 + (double)r.nextFloat() * 0.02,
                     0.0,
                     0.5F + r.nextFloat() * 0.2F,
                     i % 2 == 1,
                     false
                  )
                  .life(0.55F);
            }

            if (VfxSettings.count(fade) > 0) {
               double k = 0.2 + r.nextDouble() * 0.6;
               SoulFire.lick(
                  Mth.lerp(k, g.feet.x, rootX) + (r.nextDouble() - 0.5) * 0.8,
                  floor + 0.15,
                  Mth.lerp(k, g.feet.z, rootZ) + (r.nextDouble() - 0.5) * 0.8,
                  0.0,
                  0.05,
                  0.0,
                  1.2F + r.nextFloat() * 0.4F,
                  false,
                  false
               );
            }

            Vector3f head = g.points[0];
            Vector3f crown = g.points[1];
            Vector3f neck = g.points[10];

            for (int i = 0; i < VfxSettings.count(2.5F * fade); i++) {
               float k = r.nextFloat();
               Vector3f from = r.nextFloat() < 0.3F ? neck : head;
               double x = (double)Mth.lerp(k, from.x, crown.x) + bx * 0.75 + (double)(r.nextFloat() - 0.5F) * 0.8;
               double y = (double)Mth.lerp(k, from.y, crown.y) + 0.35 + (double)r.nextFloat() * 0.35;
               double z = (double)Mth.lerp(k, from.z, crown.z) + bz * 0.75 + (double)(r.nextFloat() - 0.5F) * 0.8;
               SoulFire.lick(x, y, z, bx * 0.03, 0.05 + (double)r.nextFloat() * 0.04, bz * 0.03, 0.95F + r.nextFloat() * 0.4F, false, false);
            }

            if (VfxSettings.count(fade) > 0) {
               SoulFire.lick(
                  (double)crown.x + (double)(r.nextFloat() - 0.5F) * 0.6,
                  (double)crown.y + 0.5,
                  (double)crown.z + (double)(r.nextFloat() - 0.5F) * 0.6,
                  bx * 0.02,
                  0.08 + (double)r.nextFloat() * 0.04,
                  bz * 0.02,
                  0.85F + r.nextFloat() * 0.3F,
                  true,
                  false
               );
            }

            for (int side = 0; side < 2; side++) {
               Vector3f sh = g.points[side == 0 ? 12 : 13];
               if (VfxSettings.count(fade) > 0) {
                  SoulFire.lick(
                     (double)sh.x + bx * 0.3 + (double)(r.nextFloat() - 0.5F) * 0.5,
                     (double)sh.y + 0.35 + (double)r.nextFloat() * 0.2,
                     (double)sh.z + bz * 0.3 + (double)(r.nextFloat() - 0.5F) * 0.5,
                     bx * 0.015,
                     0.05 + (double)r.nextFloat() * 0.03,
                     bz * 0.015,
                     1.0F + r.nextFloat() * 0.35F,
                     r.nextFloat() < 0.3F,
                     false
                  );
               }
            }

            Vector3f chest = g.points[3];
            Vector3f body = g.points[11];

            for (int i = 0; i < VfxSettings.count(2.0F * fade); i++) {
               float k = r.nextFloat();
               double across = (double)(r.nextFloat() - 0.5F) * 1.4;
               double x = (double)Mth.lerp(k, body.x, neck.x) + bx * 0.6 + sx * across;
               double y = (double)Mth.lerp(k, body.y, neck.y) + 0.2;
               double z = (double)Mth.lerp(k, body.z, neck.z) + bz * 0.6 + sz * across;
               SoulFire.lick(x, y, z, bx * 0.02, 0.05 + (double)r.nextFloat() * 0.03, bz * 0.02, 0.95F + r.nextFloat() * 0.35F, false, false);
            }

            if (VfxSettings.count(fade) > 0) {
               int sidex = r.nextInt(2);
               Vector3f fore = g.points[sidex == 0 ? 4 : 6];
               Vector3f hand = g.points[sidex == 0 ? 5 : 7];
               float k = r.nextFloat() * 1.1F;
               SoulFire.lick(
                  (double)Mth.lerp(k, fore.x, hand.x),
                  (double)Mth.lerp(k, fore.y, hand.y) + 0.1,
                  (double)Mth.lerp(k, fore.z, hand.z),
                  0.0,
                  0.04,
                  0.0,
                  0.55F + r.nextFloat() * 0.3F,
                  true,
                  false
               );
            }

            if (r.nextFloat() < 0.8F * fade) {
               SoulFire.embers(r, new Vec3(rootX, floor + 0.4, rootZ), 1.2, 1);
            }

            if (r.nextFloat() < 0.5F * fade) {
               SoulFire.embers(r, new Vec3((double)crown.x, (double)crown.y + 0.4, (double)crown.z), 0.5, 1);
            }

            Vector3f lh = g.points[5];
            Vector3f rh = g.points[7];
            SoulFire.halo((double)chest.x + bx * 0.4, (double)chest.y, (double)chest.z + bz * 0.4, 5.5F, light, 0.24F * fade);
            SoulFire.halo((double)head.x + bx * 0.3, (double)head.y + 0.2, (double)head.z + bz * 0.3, 3.4F, light, 0.28F * fade);
            SoulFire.halo((double)body.x, (double)body.y - 0.8, (double)body.z, 3.6F, light, 0.22F * fade);
            SoulFire.halo((double)lh.x, (double)lh.y, (double)lh.z, 2.0F, light, 0.25F * fade);
            SoulFire.halo((double)rh.x, (double)rh.y, (double)rh.z, 2.0F, light, 0.25F * fade);
            SoulFire.floorGlow(rootX, floor, rootZ, 7.0F, light, 0.5F * fade);
            SoulFire.floorGlow(g.feet.x, floor, g.feet.z, 3.0F, light, 0.35F * fade);
         }
      }
   }

   public static void draw(PoseStack ps, BufferSource buffers, Vec3 cam, float partial) {
      if (!GHOSTS.isEmpty() && MODEL.ready()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.level != null) {
            int gen = GeoResources.MODELS.generation();
            if (bones == null || gen != bonesGeneration) {
               bonesGeneration = gen;
               bones = new int[BONES.length];

               for (int i = 0; i < BONES.length; i++) {
                  bones[i] = MODEL.bone(BONES[i]);
               }
            }

            float now = VfxClock.time();

            for (Entry<Integer, SoulKingGhost.Ghost> e : GHOSTS.entrySet()) {
               SoulKingGhost.Ghost g = e.getValue();
               Entity entity = mc.level.getEntity(e.getKey());
               Vec3 feet = entity != null ? entity.getPosition(mc.getTimer().getGameTimeDeltaPartialTick(false)) : g.feet;
               if (!(feet.distanceToSqr(cam) > 16384.0)) {
                  float yaw = Mth.rotLerp(partial, g.yawO, g.yaw);
                  Vec3 root = root(feet, yaw).add(0.0, -0.3, 0.0);
                  pose(g, now);
                  PoseStack local = new PoseStack();
                  local.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
                  local.scale(0.0625F, 0.0625F, 0.0625F);
                  MODEL.pivots(local.last(), bones, LOCAL);

                  for (int i = 0; i < BONES.length; i++) {
                     g.points[i].set(LOCAL[i]).add((float)root.x, (float)root.y, (float)root.z);
                  }

                  g.placed = true;
                  ps.pushPose();
                  ps.translate(root.x - cam.x, root.y - cam.y, root.z - cam.z);
                  ps.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
                  ps.scale(0.0625F, 0.0625F, 0.0625F);
                  RenderType fire = SoulBodyShader.type(MODEL.texture());
                  if (fire != null) {
                     SoulBodyShader.frame((float)(root.x - cam.x), (float)(root.y - cam.y), (float)(root.z - cam.z), 180.0F - yaw, now, 0.85F + 0.15F * g.fire);
                     MODEL.drawWith(ps, buffers, fire);
                  } else {
                     MODEL.drawGhost(ps, buffers, 1.0F);
                  }

                  ps.popPose();
               }
            }
         }
      }
   }

   private static void pose(SoulKingGhost.Ghost g, float now) {
      MODEL.pose();
      MODEL.play("idle", now, true, 1.0F);
      if (g.last != null) {
         float w = 1.0F - (now - g.lastEnd) / 4.0F;
         if (w > 0.0F) {
            MODEL.blend(g.last, g.lastTime + (now - g.lastEnd), w);
         } else {
            g.last = null;
         }
      }

      if (g.action != null) {
         float t = now - g.actionStart;
         float len = MODEL.length(g.action);
         boolean held = "summon".equals(g.action) || "vanish".equals(g.action);
         if (!held && t > len + 6.0F) {
            g.action = null;
         } else {
            float w = held
               ? (t > len ? Mth.clamp(1.0F - (t - len) / 6.0F, 0.0F, 1.0F) : 1.0F)
               : Mth.clamp(Math.min(t / 2.0F, (len + 6.0F - t) / 6.0F), 0.0F, 1.0F);
            if ("vanish".equals(g.action)) {
               w = 1.0F;
            }

            MODEL.blend(g.action, t, w);
            if ("summon".equals(g.action) && t > len + 6.0F) {
               g.action = null;
            }
         }
      }
   }

   public static boolean standing(AbstractClientPlayer player) {
      SoulKingGhost.Ghost g = GHOSTS.get(player.getId());
      return g != null && Float.isNaN(g.vanish);
   }

   private SoulKingGhost() {
   }

   static {
      for (int i = 0; i < LOCAL.length; i++) {
         LOCAL[i] = new Vector3f();
      }
   }

   private static final class Ghost {
      float yaw;
      float yawO;
      boolean turned;
      @Nullable
      String action;
      float actionStart;
      @Nullable
      String last;
      float lastTime;
      float lastEnd;
      float vanish = Float.NaN;
      Vec3 feet = Vec3.ZERO;
      final Vector3f[] points = new Vector3f[SoulKingGhost.BONES.length];
      boolean placed;
      final RandomSource random = RandomSource.create();
      float fire;
      @Nullable
      SoulKingAura aura;

      Ghost() {
         for (int i = 0; i < this.points.length; i++) {
            this.points[i] = new Vector3f();
         }
      }
   }
}
