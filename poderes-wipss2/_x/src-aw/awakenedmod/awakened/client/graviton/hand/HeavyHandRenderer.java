package com.awakenedmod.awakened.client.graviton.hand;

import com.awakenedmod.awakened.client.render.FxLayer;
import com.awakenedmod.awakened.client.render.FxLayers;
import com.awakenedmod.awakened.content.graviton.HeavyHandBodyVisual;
import com.awakenedmod.awakened.content.graviton.HeavyHandVisual;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;

@EventBusSubscriber(
   modid = "awakened",
   value = {Dist.CLIENT}
)
public final class HeavyHandRenderer {
   private static final float HOLD_TICKS = 8.0F;
   private static final HeavyHandClientLook LOOK = new HeavyHandClientLook();
   private static final Map<Integer, HeavyHandRenderer.Local> LOCALS = new HashMap<>();
   private static ClientLevel memoryLevel;

   private HeavyHandRenderer() {
   }

   @SubscribeEvent
   public static void onRender(RenderLevelStageEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && event.getStage() == Stage.AFTER_TRANSLUCENT_BLOCKS) {
         float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
         draw(mc, event.getPoseStack(), event.getCamera().getPosition(), pt);
      }
   }

   private static void draw(Minecraft mc, PoseStack poseStack, Vec3 cam, float pt) {
      if (memoryLevel != mc.level) {
         LOCALS.clear();
         memoryLevel = mc.level;
      }

      float t = (float)(mc.level.getGameTime() % 100000L) + pt;
      List<HeavyHandRenderer.Grip> grips = new ArrayList<>();

      for (Player p : mc.level.players()) {
         HeavyHandRenderer.Grip g = step(mc, p, t, pt);
         if (g != null) {
            grips.add(g);
         }
      }

      List<HeavyHandRenderer.Body> bodies = new ArrayList<>();
      double now = (double)((float)mc.level.getGameTime() + pt);

      for (Entity e : mc.level.entitiesForRendering()) {
         if (e instanceof LivingEntity) {
            LivingEntity le = (LivingEntity)e;
            if (le.hasData(HeavyHandBodyVisual.TYPE)) {
               HeavyHandBodyVisual bv = HeavyHandBodyVisual.of(le);
               float rem = bv.remaining(now);
               if (bv.phase() != 0 && rem > 0.0F && le.isAlive()) {
                  bodies.add(new HeavyHandRenderer.Body(le, le.getPosition(pt), bv.phase(), rem, bv.age(now), isFirstPersonOf(mc, le)));
               }
            }
         }
      }

      BufferSource buffers = mc.renderBuffers().bufferSource();
      poseStack.pushPose();
      poseStack.translate(-cam.x, -cam.y, -cam.z);
      HeavyHandRenderer.Frame frame = new HeavyHandRenderer.Frame(mc, mc.level, poseStack, buffers, cam, t, pt);
      Pose pose = frame.pose();

      for (HeavyHandRenderer.Grip g : grips) {
         LOOK.gripOwn(frame, g);
      }

      for (HeavyHandRenderer.Body b : bodies) {
         LOOK.bodyOwn(frame, b);
      }

      VertexConsumer dark = FxLayers.begin(buffers, FxLayer.PAINT);
      pass(frame, grips, bodies, dark, false);
      FxLayers.end(buffers, FxLayer.PAINT);
      FxLayer glow = FxLayer.LIGHT;
      VertexConsumer add = FxLayers.begin(buffers, glow);
      pass(frame, grips, bodies, add, true);
      FxLayers.end(buffers, glow);
      poseStack.popPose();
   }

   private static void pass(
      HeavyHandRenderer.Frame frame, List<HeavyHandRenderer.Grip> grips, List<HeavyHandRenderer.Body> bodies, VertexConsumer vc, boolean glow
   ) {
      for (HeavyHandRenderer.Grip g : grips) {
         LOOK.grip(frame, g, vc, glow);
      }

      for (HeavyHandRenderer.Body b : bodies) {
         LOOK.body(frame, b, vc, glow);
      }
   }

   private static boolean isFirstPersonOf(Minecraft mc, Entity e) {
      return e == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
   }

   private static HeavyHandRenderer.Grip step(Minecraft mc, Player p, float t, float pt) {
      HeavyHandVisual vis = HeavyHandVisual.of(p);
      HeavyHandRenderer.Local l = LOCALS.get(p.getId());
      if (l == null) {
         if (!vis.holding() && vis.snaps() == 0) {
            return null;
         }

         l = new HeavyHandRenderer.Local();
         LOCALS.put(p.getId(), l);
      }

      if (l.seenSnaps != Integer.MIN_VALUE && vis.snaps() != l.seenSnaps) {
         l.snapAt = t;
         l.snapKind = vis.snapKind();
      }

      l.seenSnaps = vis.snaps();
      float snap = (t - l.snapAt) / 12.0F;
      boolean snapping = snap >= 0.0F && snap < 1.0F && !vis.holding();
      if (!vis.holding() && !snapping) {
         return null;
      } else {
         LivingEntity target = mc.level.getEntity(vis.target()) instanceof LivingEntity le ? le : null;
         if (vis.holding()) {
            l.targetId = vis.target();
            l.up = vis.up();
            l.f = Math.min(1.0F, vis.gripFraction() + pt / 8.0F);
         }

         Vec3 b = target != null ? target.getPosition(pt).add(0.0, (double)target.getBbHeight() * 0.5, 0.0) : l.lastB;
         if (b == null) {
            return null;
         } else {
            l.lastB = b;
            return new HeavyHandRenderer.Grip(
               p,
               b,
               target,
               l.f,
               l.up,
               vis.holding(),
               vis.holding() ? -1.0F : snap,
               l.snapKind,
               isFirstPersonOf(mc, p),
               target != null && isFirstPersonOf(mc, target)
            );
         }
      }
   }

   static record Body(LivingEntity body, Vec3 feet, int phase, float remaining, float age, boolean victimView) {
   }

   static record Frame(Minecraft mc, ClientLevel level, PoseStack poseStack, BufferSource buffers, Vec3 cam, float t, float pt) {
      static final double NEAR_CLIP = 0.6;
      static final double NEAR_FADE = 0.7;
      static final double OWNER_CLEAR = 3.0;

      Pose pose() {
         return this.poseStack.last();
      }

      float nearFade(Vec3 p, boolean ownerView) {
         double d = Math.sqrt(this.cam.distanceToSqr(p));
         float near = (float)Mth.clamp((d - 0.6) / 0.7, 0.0, 1.0);
         return ownerView ? near * (float)Mth.clamp(0.25 + 0.75 * (d - 1.0) / 2.0, 0.0, 1.0) : near;
      }

      double groundBelow(Vec3 at, int blocks) {
         MutableBlockPos pos = BlockPos.containing(at.x, at.y, at.z).mutable();

         for (int i = 0; i < blocks; i++) {
            BlockState state = this.level.getBlockState(pos);
            if (!state.isAir()) {
               VoxelShape shape = state.getCollisionShape(this.level, pos);
               if (!shape.isEmpty()) {
                  return (double)pos.getY() + shape.max(Axis.Y);
               }

               if (!state.getFluidState().isEmpty()) {
                  return (double)pos.getY() + 1.0;
               }
            }

            pos.move(0, -1, 0);
         }

         return Double.NaN;
      }
   }

   static record Grip(
      Player caster,
      Vec3 b,
      @Nullable LivingEntity target,
      float f,
      boolean up,
      boolean holding,
      float snap,
      int snapKind,
      boolean ownerView,
      boolean victimView
   ) {
   }

   private static final class Local {
      int seenSnaps = Integer.MIN_VALUE;
      float snapAt = -1000.0F;
      int snapKind;
      int targetId = -1;
      Vec3 lastB;
      boolean up;
      float f;
   }
}
