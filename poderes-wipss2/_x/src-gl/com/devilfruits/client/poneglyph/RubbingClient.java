package com.devilfruits.client.poneglyph;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.poneglyph.PoneglyphFaceBlock;
import com.devilfruits.core.poneglyph.PoneglyphNetwork;
import com.devilfruits.core.poneglyph.Poneglyphs;
import com.devilfruits.core.poneglyph.RubbingService;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;

public final class RubbingClient {
   private static final ResourceLocation SMUDGE = DevilFruits.id("poneglyph/ink_smudge");
   private static int flash;
   private static int flashSeed;

   static void tick() {
      Minecraft mc = Minecraft.getInstance();
      if (flash > 0) {
         flash--;
      }

      if (mc.player != null && mc.level != null && mc.screen == null && mc.options.keyUse.isDown() && RubbingService.holdsTools(mc.player)) {
         HitResult hit = mc.hitResult;
         if (hit instanceof BlockHitResult bhr
            && hit.getType() == Type.BLOCK
            && mc.level.getBlockState(bhr.getBlockPos()).is((Block)Poneglyphs.PONEGLYPH_FACE.get())) {
            PacketDistributor.sendToServer(new PoneglyphNetwork.RubHeldPayload(bhr.getBlockPos()), new CustomPacketPayload[0]);
         }
      }
   }

   static void finished(BlockPos pos) {
      flash = 14;
      flashSeed = pos.hashCode();
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null) {
         BlockState state = mc.level.getBlockState(pos);
         if (state.is((Block)Poneglyphs.PONEGLYPH_FACE.get())) {
            Direction f = (Direction)state.getValue(PoneglyphFaceBlock.FACING);

            for (int i = 0; i < 10; i++) {
               mc.level
                  .addParticle(
                     (ParticleOptions)Poneglyphs.GLYPH_PARTICLE.get(),
                     (double)pos.getX() + 0.5 + (double)f.getStepX() * 0.6 + (mc.level.random.nextDouble() - 0.5) * 0.8,
                     (double)pos.getY() + 0.2 + mc.level.random.nextDouble() * 0.6,
                     (double)pos.getZ() + 0.5 + (double)f.getStepZ() * 0.6 + (mc.level.random.nextDouble() - 0.5) * 0.8,
                     (double)f.getStepX() * 0.02,
                     0.02,
                     (double)f.getStepZ() * 0.02
                  );
            }
         }
      }
   }

   static void renderHud(GuiGraphics gg, DeltaTracker delta) {
      Minecraft mc = Minecraft.getInstance();
      int cx = gg.guiWidth() / 2;
      int cy = gg.guiHeight() / 2;
      ClientPoneglyphState.Rub rub = ClientPoneglyphState.rub;
      if (rub != null && !mc.options.hideGui) {
         float progress = rub.progress(delta.getGameTimeDeltaPartialTick(false));
         int dots = 24;
         int lit = Mth.floor(progress * (float)dots);

         for (int i = 0; i < dots; i++) {
            double a = (-Math.PI / 2) + (double)i * ((Math.PI * 2) / (double)dots);
            int x = cx + (int)Math.round(Math.cos(a) * 12.0) - 1;
            int y = cy + (int)Math.round(Math.sin(a) * 12.0) - 1;
            gg.fill(x, y, x + 2, y + 2, i < lit ? -1450576 : 1881151504);
            if (i < lit) {
               gg.fill(x, y + 1, x + 2, y + 2, -4744612);
            }
         }
      }

      if (flash > 0) {
         float a = (float)flash / 14.0F;
         gg.setColor(1.0F, 1.0F, 1.0F, a * 0.85F);
         int size = 48 + (14 - flash) * 3;
         gg.blitSprite(SMUDGE, cx - size / 2 + flashSeed % 7 - 3, cy - size / 2 + flashSeed / 7 % 5 - 2, size, size);
         gg.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   static void renderGlow(RenderLevelStageEvent event) {
      ClientPoneglyphState.Rub rub = ClientPoneglyphState.rub;
      Minecraft mc = Minecraft.getInstance();
      if (rub != null && mc.level != null) {
         BlockState state = mc.level.getBlockState(rub.pos());
         if (state.is((Block)Poneglyphs.PONEGLYPH_FACE.get())) {
            float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
            float progress = rub.progress(partial);
            float pulse = 0.85F + 0.15F * Mth.sin(((float)mc.level.getGameTime() + partial) * 0.6F);
            int alpha = (int)(255.0F * Mth.clamp(0.15F + progress * 0.85F, 0.0F, 1.0F) * pulse);
            int carving = (Integer)state.getValue(PoneglyphFaceBlock.CARVING);
            int cell = (Integer)state.getValue(PoneglyphFaceBlock.CELL);
            ResourceLocation tex = DevilFruits.id("textures/block/poneglyph/carving_" + carving + "_rub.png");
            Direction f = (Direction)state.getValue(PoneglyphFaceBlock.FACING);
            Direction right = f.getCounterClockWise();
            Vec3 cam = event.getCamera().getPosition();
            Vec3 c = Vec3.atCenterOf(rub.pos()).add((double)f.getStepX() * 0.503, 0.0, (double)f.getStepZ() * 0.503).subtract(cam);
            float rx = (float)right.getStepX() * 0.5F;
            float rz = (float)right.getStepZ() * 0.5F;
            float u0 = (float)(cell % 3) / 3.0F;
            float v0 = (float)(cell / 3) / 3.0F;
            float u1 = u0 + 0.33333334F;
            float v1 = v0 + 0.33333334F;
            PoseStack ps = event.getPoseStack();
            BufferSource buffers = mc.renderBuffers().bufferSource();
            RenderType type = RenderType.eyes(tex);
            VertexConsumer vc = buffers.getBuffer(type);
            Matrix4f m = ps.last().pose();
            float x = (float)c.x;
            float y = (float)c.y;
            float z = (float)c.z;
            vertex(vc, m, ps, x - rx, y - 0.5F, z - rz, u0, v1, alpha, f);
            vertex(vc, m, ps, x + rx, y - 0.5F, z + rz, u1, v1, alpha, f);
            vertex(vc, m, ps, x + rx, y + 0.5F, z + rz, u1, v0, alpha, f);
            vertex(vc, m, ps, x - rx, y + 0.5F, z - rz, u0, v0, alpha, f);
            buffers.endBatch(type);
         }
      }
   }

   private static void vertex(VertexConsumer vc, Matrix4f m, PoseStack ps, float x, float y, float z, float u, float v, int alpha, Direction f) {
      vc.addVertex(m, x, y, z)
         .setColor(alpha, alpha, alpha, 255)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(ps.last(), (float)f.getStepX(), 0.0F, (float)f.getStepZ());
   }

   private RubbingClient() {
   }
}
