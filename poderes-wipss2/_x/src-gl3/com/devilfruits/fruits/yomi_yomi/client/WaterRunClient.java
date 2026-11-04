package com.devilfruits.fruits.yomi_yomi.client;

import com.devilfruits.client.animation.AnimationController;
import com.devilfruits.client.form.FormMovements;
import com.devilfruits.client.render.VfxRenderTypes;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.fruits.yomi_yomi.Vessel;
import com.devilfruits.fruits.yomi_yomi.WaterRun;
import com.devilfruits.fruits.yomi_yomi.YomiYomi;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import net.neoforged.neoforge.event.tick.LevelTickEvent.Post;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class WaterRunClient implements FormMovements.FormMovement, AnimationController.PoseLayer {
   private static final int AFTERIMAGES = 3;
   private static final ResourceLocation RUN = YomiYomi.id("water_run");
   private static final Vector3f GREEN = new Vector3f(0.49F, 1.0F, 0.23F);
   private static final Map<Integer, ArrayDeque<Vec3>> TRAILS = new HashMap<>();
   private static final List<WaterRunClient.Ripple> RIPPLES = new ArrayList<>();
   private static boolean localRunning;

   public static boolean running(Player player) {
      return player == Minecraft.getInstance().player ? localRunning : FormFlags.has(player, YomiYomi.ID, WaterRun.FLAG);
   }

   @Override
   public void tick(LocalPlayer player, FruitForm form, boolean boosting) {
      localRunning = false;
      if (WaterRun.allowed(player) && !player.isUnderWater()) {
         double top = WaterRun.surface(player.level(), player.getX(), player.getY(), player.getZ());
         if (!Double.isNaN(top)) {
            Input in = player.input;
            float fwd = in.forwardImpulse;
            float side = in.leftImpulse;
            if (!(fwd <= 0.0F)) {
               localRunning = true;
               float yaw = player.getYRot() * (float) (Math.PI / 180.0);
               double len = Math.max(1.0E-4, Math.sqrt((double)(fwd * fwd + side * side)));
               double dx = (double)(side * Mth.cos(yaw) - fwd * Mth.sin(yaw)) / len;
               double dz = (double)(fwd * Mth.cos(yaw) + side * Mth.sin(yaw)) / len;
               double speed = (double)player.getSpeed() * 2.16 * YomiYomi.setting("waterRunSpeed");
               Vec3 v = player.getDeltaMovement();
               double vx = Mth.lerp(0.35, v.x, dx * speed);
               double vz = Mth.lerp(0.35, v.z, dz * speed);
               double vy = player.input.jumping && player.getY() > top - 0.05 ? player.getAttributeValue(Attributes.JUMP_STRENGTH) : top + 0.02 - player.getY();
               player.setDeltaMovement(vx, vy, vz);
               player.resetFallDistance();
            }
         }
      }
   }

   @Override
   public void leave(LocalPlayer player, FruitForm form) {
      localRunning = false;
   }

   @Override
   public void poses(Player player, float partialTick, AnimationController.PoseSink out) {
      if (running(player)) {
         out.add(RUN, (float)player.tickCount + partialTick, 1.0F);
      }
   }

   static void onLevelTick(Post event) {
      if (event.getLevel() instanceof ClientLevel level) {
         RandomSource var11 = level.getRandom();

         for (AbstractClientPlayer p : level.players()) {
            ArrayDeque<Vec3> trail = TRAILS.get(p.getId());
            if (running(p)) {
               p.walkAnimation.setSpeed(0.0F);
               p.walkAnimation.update(0.0F, 1.0F);
               if (trail == null) {
                  trail = new ArrayDeque<>();
                  TRAILS.put(p.getId(), trail);
               }

               trail.addFirst(p.position());

               while (trail.size() > 7) {
                  trail.removeLast();
               }

               double top = WaterRun.surface(level, p.getX(), p.getY(), p.getZ());
               double y = Double.isNaN(top) ? p.getY() : top;
               if (p.tickCount % 2 == 0) {
                  RIPPLES.add(new WaterRunClient.Ripple(new Vec3(p.getX(), y + 0.02, p.getZ()), level.getGameTime()));
               }

               for (int k = 0; k < 2; k++) {
                  level.addParticle(
                     new DustParticleOptions(GREEN, 0.9F + var11.nextFloat() * 0.5F),
                     p.getX() + var11.nextGaussian() * 0.2,
                     y + 0.1,
                     p.getZ() + var11.nextGaussian() * 0.2,
                     0.0,
                     0.05,
                     0.0
                  );
               }

               level.addParticle(ParticleTypes.SPLASH, p.getX() + var11.nextGaussian() * 0.25, y + 0.05, p.getZ() + var11.nextGaussian() * 0.25, 0.0, 0.1, 0.0);
            } else if (trail != null) {
               TRAILS.remove(p.getId());
            }
         }

         long now = level.getGameTime();
         RIPPLES.removeIf(rp -> now - rp.born > 24L);
      }
   }

   static void onRenderPlayer(net.neoforged.neoforge.client.event.RenderPlayerEvent.Post event) {
      AbstractClientPlayer p = (AbstractClientPlayer)event.getEntity();
      ArrayDeque<Vec3> trail = TRAILS.get(p.getId());
      if (trail != null && trail.size() >= 3 && running(p)) {
         PlayerModel<AbstractClientPlayer> model = (PlayerModel<AbstractClientPlayer>)event.getRenderer().getModel();
         PoseStack ps = event.getPoseStack();
         float partial = event.getPartialTick();
         Vec3 now = new Vec3(Mth.lerp((double)partial, p.xo, p.getX()), Mth.lerp((double)partial, p.yo, p.getY()), Mth.lerp((double)partial, p.zo, p.getZ()));
         float bodyYaw = Mth.rotLerp(partial, p.yBodyRotO, p.yBodyRot);
         boolean skeleton = SkeletonBody.shown(p) == Vessel.SKELETON;
         VertexConsumer vc = skeleton ? null : event.getMultiBufferSource().getBuffer(RenderType.entityTranslucent(event.getRenderer().getTextureLocation(p)));
         Iterator<Vec3> it = trail.iterator();
         int i = 0;

         while (it.hasNext()) {
            Vec3 at = it.next();
            i++;
            if (i % 2 == 0 && i / 2 <= 4 && i >= 4) {
               int k = i / 2;
               Vec3 d = at.subtract(now);
               if (!(d.horizontalDistanceSqr() < 0.16)) {
                  ps.pushPose();
                  ps.translate(d.x, d.y, d.z);
                  ps.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
                  ps.scale(-0.9375F, -0.9375F, 0.9375F);
                  ps.translate(0.0F, -1.501F, 0.0F);
                  int a = Math.round(60.0F * (1.0F - (float)(k - 2) / 4.0F));
                  if (skeleton) {
                     SkeletonBody.renderGhost(model, ps, event.getMultiBufferSource(), a << 24 | 8191834);
                  } else {
                     model.renderToBuffer(ps, vc, 15728880, OverlayTexture.NO_OVERLAY, a << 24 | 8191834);
                  }

                  ps.popPose();
               }
            }
         }
      }
   }

   static void onRenderStage(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_TRANSLUCENT_BLOCKS && !RIPPLES.isEmpty()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.level != null) {
            Vec3 cam = event.getCamera().getPosition();
            float now = (float)mc.level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
            PoseStack ps = event.getPoseStack();
            BufferSource buffers = mc.renderBuffers().bufferSource();
            VertexConsumer vc = buffers.getBuffer(VfxRenderTypes.GLOW);
            Matrix4f m = ps.last().pose();

            for (WaterRunClient.Ripple rp : RIPPLES) {
               float age = (now - (float)rp.born) / 24.0F;
               if (!(age < 0.0F) && !(age > 1.0F)) {
                  float rad = 0.25F + 1.3F * (1.0F - (1.0F - age) * (1.0F - age));
                  float w = 0.07F;
                  float alpha = 0.5F * (1.0F - age);
                  float cx = (float)(rp.pos.x - cam.x);
                  float cy = (float)(rp.pos.y - cam.y);
                  float cz = (float)(rp.pos.z - cam.z);
                  int seed = (int)(rp.born * 31L + Math.round(rp.pos.x * 7.0) + Math.round(rp.pos.z * 13.0));
                  int seg = 26;
                  int al = Math.round(alpha * 255.0F);

                  for (int s = 0; s < seg; s++) {
                     if (!(hash(seed, s) < 0.28F)) {
                        float a0 = (float)s * (float) (Math.PI * 2) / (float)seg;
                        float a1 = (float)(s + 1) * (float) (Math.PI * 2) / (float)seg;
                        float r0 = rad * (0.86F + 0.28F * hash(seed + 7, s));
                        float r1 = rad * (0.86F + 0.28F * hash(seed + 7, s + 1));
                        vc.addVertex(m, cx + Mth.cos(a0) * r0, cy, cz + Mth.sin(a0) * r0).setColor(200, 255, 170, al);
                        vc.addVertex(m, cx + Mth.cos(a1) * r1, cy, cz + Mth.sin(a1) * r1).setColor(200, 255, 170, al);
                        vc.addVertex(m, cx + Mth.cos(a1) * (r1 + w), cy, cz + Mth.sin(a1) * (r1 + w)).setColor(124, 255, 58, 0);
                        vc.addVertex(m, cx + Mth.cos(a0) * (r0 + w), cy, cz + Mth.sin(a0) * (r0 + w)).setColor(124, 255, 58, 0);
                     }
                  }
               }
            }

            buffers.endBatch(VfxRenderTypes.GLOW);
         }
      }
   }

   private static float hash(int seed, int i) {
      int h = seed * 374761393 + i * 668265263;
      h = (h ^ h >>> 13) * 1274126177;
      return (float)(h >>> 8 & 65535) / 65535.0F;
   }

   static void clear() {
      TRAILS.clear();
      RIPPLES.clear();
      localRunning = false;
   }

   private static record Ripple(Vec3 pos, long born) {
   }
}
