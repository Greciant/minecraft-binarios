package com.awakenedmod.awakened.client.render.limitless;

import com.awakenedmod.awakened.client.render.FxLayer;
import com.awakenedmod.awakened.client.render.FxLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class LimitlessFrame {
   public final Minecraft mc;
   public final ClientLevel level;
   public final Vec3 cam;
   public final Quaternionf orientation;
   public final Vector3f camRight;
   public final Vector3f camUp;
   public final Vector3f camForward;
   public final float partialTick;
   public final long gameTime;
   public final PoseStack poseStack;
   public final BufferSource buffers;

   private LimitlessFrame(Minecraft mc, Vec3 cam, Quaternionf orientation, float partialTick, PoseStack poseStack) {
      this.mc = mc;
      this.level = mc.level;
      this.cam = cam;
      this.orientation = orientation;
      this.camRight = new Vector3f(1.0F, 0.0F, 0.0F).rotate(orientation);
      this.camUp = new Vector3f(0.0F, 1.0F, 0.0F).rotate(orientation);
      this.camForward = new Vector3f(0.0F, 0.0F, -1.0F).rotate(orientation);
      this.partialTick = partialTick;
      this.gameTime = mc.level.getGameTime();
      this.poseStack = poseStack;
      this.buffers = mc.renderBuffers().bufferSource();
   }

   public Vector3f rel(Vec3 world) {
      return new Vector3f((float)(world.x - this.cam.x), (float)(world.y - this.cam.y), (float)(world.z - this.cam.z));
   }

   public Vector3f rel(Vec3 world, Vec3 dir, double dist) {
      return new Vector3f(
         (float)(world.x + dir.x * dist - this.cam.x), (float)(world.y + dir.y * dist - this.cam.y), (float)(world.z + dir.z * dist - this.cam.z)
      );
   }

   public float since(long startTick) {
      return (float)(this.gameTime - startTick) + this.partialTick;
   }

   public LimitlessMesh begin(FxLayer layer) {
      return new LimitlessMesh(this.poseStack.last(), FxLayers.begin(this.buffers, layer));
   }

   public void end(FxLayer layer) {
      FxLayers.end(this.buffers, layer);
   }

   public static void register(IEventBus gameBus, Consumer<LimitlessFrame> drawer) {
      gameBus.addListener(event -> {
         if (event.getStage() == Stage.AFTER_TRANSLUCENT_BLOCKS) {
            fire(event, drawer);
         }
      });
   }

   private static void fire(RenderLevelStageEvent event, Consumer<LimitlessFrame> drawer) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null) {
         float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
         Vec3 cam = event.getCamera().getPosition();
         Quaternionf orientation = new Quaternionf(event.getCamera().rotation());
         PoseStack ps = event.getPoseStack() != null ? event.getPoseStack() : new PoseStack();
         drawer.accept(new LimitlessFrame(mc, cam, orientation, pt, ps));
      }
   }
}
