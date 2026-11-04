package com.devilfruits.fruits.yomi_yomi.client;

import com.devilfruits.client.render.FormRenderers;
import com.devilfruits.client.render.VfxRenderTypes;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.fruits.yomi_yomi.YomiYomi;
import com.devilfruits.fruits.yomi_yomi.client.vfx.SoulFire;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class SoulRenderer implements FormRenderers.FormRenderer {
   static final YomiModel MODEL = new YomiModel("soul");
   private static final float HEIGHT = 1.15F;
   private static final float SCALE = 0.03125F;
   private static final float ALPHA = 0.8F;
   private static final Map<Integer, SoulRenderer.OneShot> ONE_SHOT = new HashMap<>();
   private static final Map<Integer, SoulRenderer.Yank> YANK = new HashMap<>();
   private static final Map<Integer, float[]> SPEED = new HashMap<>();
   private static final Map<Integer, float[]> COLD = new HashMap<>();
   private static final RandomSource FIRE = RandomSource.create();
   public static boolean suppressed;

   public static void play(int id, String clip) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null) {
         ONE_SHOT.put(id, new SoulRenderer.OneShot(clip, (float)level.getGameTime() + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)));
      }
   }

   public static void yank(int id, Vec3 body, int snapTicks) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null) {
         YANK.put(
            id, new SoulRenderer.Yank(body, (float)level.getGameTime() + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false), snapTicks)
         );
      }
   }

   public static void cold(int id, float ticks) {
      float now = VfxClock.time();
      COLD.put(id, new float[]{now, now + ticks});
   }

   static void tickFire(ClientLevel level) {
      if (VfxClock.advanced()) {
         Minecraft mc = Minecraft.getInstance();
         Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
         float now = VfxClock.time();

         for (Player p : level.players()) {
            if (YomiYomi.isSoul(p)
               && !p.isSpectator()
               && (p != mc.player || !mc.options.getCameraType().isFirstPerson())
               && !"possess".equals(current(p.getId()))) {
               Vec3 head = p.position().add(0.0, 1.15F, 0.0);
               if (!(head.distanceToSqr(cam) > 4096.0)) {
                  float cold = 0.0F;
                  float[] c = COLD.get(p.getId());
                  if (c != null) {
                     if (now > c[1] + 10.0F) {
                        COLD.remove(p.getId());
                     } else {
                        cold = Mth.clamp(Math.min((now - c[0]) / 3.0F, (c[1] + 10.0F - now) / 10.0F), 0.0F, 1.0F);
                     }
                  }

                  SoulFire.skull(FIRE, head, cold);
               }
            }
         }
      }
   }

   static void clear() {
      ONE_SHOT.clear();
      SPEED.clear();
      YANK.clear();
      COLD.clear();
   }

   @Override
   public boolean render(AbstractClientPlayer player, FruitForm form, float partial, PoseStack ps, MultiBufferSource buffers, int light) {
      if (!suppressed && MODEL.ready()) {
         float now = (float)player.level().getGameTime() + partial;
         MODEL.pose();
         double v = player.getDeltaMovement().length();
         float[] s = SPEED.computeIfAbsent(player.getId(), kx -> new float[1]);
         s[0] += (Mth.clamp((float)v / 0.35F, 0.0F, 1.0F) - s[0]) * 0.15F;
         float fly = s[0];
         SoulRenderer.OneShot shot = ONE_SHOT.get(player.getId());
         float shotWeight = 0.0F;
         if (shot != null) {
            float t = now - shot.start;
            float len = MODEL.length(shot.clip);
            if (t > len + ("possess".equals(shot.clip) ? 60.0F : 4.0F)) {
               ONE_SHOT.remove(player.getId());
            } else {
               shotWeight = "possess".equals(shot.clip) ? 1.0F : Mth.clamp(Math.min(t / 2.0F, (len + 4.0F - t) / 4.0F), 0.0F, 1.0F);
               MODEL.play(shot.clip, t, false, shotWeight);
            }
         }

         float rest = 1.0F - shotWeight;
         MODEL.play("fly", now, true, fly * rest);
         MODEL.play("idle", now, true, (1.0F - fly) * rest);
         float pitch = Mth.lerp(partial, player.xRotO, player.getXRot());
         float yaw = Mth.rotLerp(partial, player.yHeadRotO, player.yHeadRot);
         ps.pushPose();
         SoulRenderer.Yank yank = YANK.get(player.getId());
         if (yank != null) {
            float t = now - yank.start;
            if (t > (float)(yank.snap + 2)) {
               YANK.remove(player.getId());
            } else {
               float k = Mth.clamp((t - ((float)yank.snap - 3.0F)) / 3.0F, 0.0F, 1.0F);
               Vec3 off = yank.body.subtract(player.getPosition(partial)).scale((double)(k * k));
               ps.translate(off.x, off.y, off.z);
            }
         }

         ps.translate(0.0F, 0.9F, 0.0F);
         ps.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
         ps.mulPose(Axis.XP.rotationDegrees(-pitch * 0.6F));
         ps.scale(0.03125F, 0.03125F, 0.03125F);
         MODEL.drawGhost(ps, buffers, 0.8F);
         ps.popPose();
         wisps(player, now, ps, buffers);
         FormRenderers.renderNameTag(player, partial, ps, buffers, light);
         return true;
      } else {
         return false;
      }
   }

   private static void wisps(AbstractClientPlayer player, float now, PoseStack ps, MultiBufferSource buffers) {
      VertexConsumer vc = buffers.getBuffer(VfxRenderTypes.GLOW);
      Matrix4f m = ps.last().pose();
      Camera cam = Minecraft.getInstance().gameRenderer.getMainCamera();
      Vector3f right = new Vector3f(cam.getLeftVector()).negate();
      Vector3f up = new Vector3f(cam.getUpVector());

      for (int w = 0; w < 3; w++) {
         float phase = (float)w * (float) (Math.PI * 2) / 3.0F + (float)player.getId() * 0.7F;

         for (int k = 0; k < 6; k++) {
            float t = now * 0.11F + phase - (float)k * 0.09F;
            float tilt = 0.5F + (float)w * 0.4F;
            float r = 0.36F + 0.04F * Mth.sin(now * 0.2F + (float)w);
            float x = Mth.cos(t) * r;
            float z = Mth.sin(t) * r;
            float y = 1.15F + Mth.sin(t) * r * Mth.sin(tilt) * 0.6F + 0.06F * Mth.sin(now * 0.31F + (float)w * 2.0F);
            float size = (k == 0 ? 0.05F : 0.04F) * (1.0F - (float)k / 7.0F);
            float a = (k == 0 ? 0.95F : 0.7F) * (1.0F - (float)k / 6.5F);
            int g = k == 0 ? 255 : 230;
            quad(vc, m, x, y, z, right, up, size, k == 0 ? 200 : 124, g, k == 0 ? 106 : 58, a);
         }
      }
   }

   private static void quad(VertexConsumer vc, Matrix4f m, float x, float y, float z, Vector3f right, Vector3f up, float s, int r, int g, int b, float a) {
      int al = Math.round(a * 255.0F);
      float rx = right.x * s;
      float ry = right.y * s;
      float rz = right.z * s;
      float ux = up.x * s;
      float uy = up.y * s;
      float uz = up.z * s;
      vc.addVertex(m, x - rx - ux, y - ry - uy, z - rz - uz).setColor(r, g, b, al);
      vc.addVertex(m, x + rx - ux, y + ry - uy, z + rz - uz).setColor(r, g, b, al);
      vc.addVertex(m, x + rx + ux, y + ry + uy, z + rz + uz).setColor(r, g, b, al);
      vc.addVertex(m, x - rx + ux, y - ry + uy, z - rz + uz).setColor(r, g, b, al);
   }

   @Override
   public boolean renderFirstPerson(
      AbstractClientPlayer player,
      FruitForm form,
      InteractionHand hand,
      ItemStack stack,
      float partialTick,
      float swingProgress,
      float equipProgress,
      PoseStack poseStack,
      MultiBufferSource buffers,
      int packedLight
   ) {
      return true;
   }

   @Override
   public boolean renderArm(AbstractClientPlayer player, FruitForm form, HumanoidArm arm, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
      return true;
   }

   @Nullable
   static String current(int id) {
      SoulRenderer.OneShot s = ONE_SHOT.get(id);
      return s == null ? null : s.clip;
   }

   private static record OneShot(String clip, float start) {
   }

   private static record Yank(Vec3 body, float start, int snap) {
   }
}
