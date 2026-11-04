package com.devilfruits.fruits.doru_doru.client.atelier;

import com.devilfruits.fruits.doru_doru.atelier.SculptRequest;
import com.devilfruits.fruits.doru_doru.atelier.WaxIntegrity;
import com.devilfruits.fruits.doru_doru.wax.WaxMask;
import com.devilfruits.fruits.doru_doru.wax.WaxPalette;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexBuffer.Usage;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.Long2LongMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;

public final class SculptGhost {
   private static final int CELL_LIMIT = 20000;
   private static final Direction[] DIRS = Direction.values();
   @Nullable
   private static VertexBuffer buffer;
   private static boolean dirty = true;
   private static Vec3 base = Vec3.ZERO;
   private static boolean empty = true;

   public static void init() {
      NeoForge.EVENT_BUS.addListener(SculptGhost::onRender);
   }

   static void dirty() {
      dirty = true;
   }

   private static void onRender(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_TRANSLUCENT_BLOCKS && SculptClient.active()) {
         Minecraft mc = Minecraft.getInstance();
         Camera camera = event.getCamera();
         Vec3 cam = camera.getPosition();
         if (dirty) {
            rebuild();
            dirty = false;
         }

         if (buffer != null && !empty) {
            Matrix4f mv = new Matrix4f(event.getModelViewMatrix()).translate((float)(base.x - cam.x), (float)(base.y - cam.y), (float)(base.z - cam.z));
            float pulse = 0.85F + 0.15F * (float)Math.sin((double)System.currentTimeMillis() / 220.0);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, pulse);
            buffer.bind();
            buffer.drawWithShader(mv, event.getProjectionMatrix(), GameRenderer.getPositionColorShader());
            VertexBuffer.unbind();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
         }

         PoseStack pose = event.getPoseStack();
         pose.pushPose();
         pose.translate(-cam.x, -cam.y, -cam.z);
         BufferSource buffers = mc.renderBuffers().bufferSource();
         VertexConsumer lines = buffers.getBuffer(RenderType.lines());
         Vec3 t = SculptClient.target;
         if (t != null) {
            double s = 0.14;
            LevelRenderer.renderLineBox(pose, lines, new AABB(t.x - s, t.y - s, t.z - s, t.x + s, t.y + s, t.z + s), 1.0F, 0.96F, 0.86F, 1.0F);
         }

         for (Vec3 p : SculptClient.points) {
            double s = 0.1;
            LevelRenderer.renderLineBox(pose, lines, new AABB(p.x - s, p.y - s, p.z - s, p.x + s, p.y + s, p.z + s), 1.0F, 0.75F, 0.3F, 1.0F);
         }

         if (SculptClient.tool == SculptRequest.Tool.CAST && SculptClient.castCapture && t != null) {
            Vec3 a = SculptClient.points.isEmpty() ? t : SculptClient.points.get(0);
            AABB box = new AABB(BlockPos.containing(a)).minmax(new AABB(BlockPos.containing(t)));
            boolean big = box.getXsize() > 32.0 || box.getYsize() > 32.0 || box.getZsize() > 32.0;
            LevelRenderer.renderLineBox(pose, lines, box, big ? 1.0F : 0.55F, big ? 0.35F : 0.9F, big ? 0.3F : 1.0F, 1.0F);
         }

         if (SculptClient.symmetry != SculptRequest.Symmetry.NONE) {
            Vec3 o = SculptClient.origin;
            LevelRenderer.renderLineBox(pose, lines, new AABB(o.x - 0.03, o.y, o.z - 0.03, o.x + 0.03, o.y + 12.0, o.z + 0.03), 0.55F, 0.85F, 1.0F, 1.0F);
            boolean mx = SculptClient.symmetry == SculptRequest.Symmetry.MIRROR_X || SculptClient.symmetry == SculptRequest.Symmetry.MIRROR_XZ;
            boolean mz = SculptClient.symmetry == SculptRequest.Symmetry.MIRROR_Z || SculptClient.symmetry == SculptRequest.Symmetry.MIRROR_XZ;
            if (mx) {
               LevelRenderer.renderLineBox(pose, lines, new AABB(o.x - 0.01, o.y, o.z - 8.0, o.x + 0.01, o.y + 0.02, o.z + 8.0), 0.55F, 0.85F, 1.0F, 0.8F);
            }

            if (mz) {
               LevelRenderer.renderLineBox(pose, lines, new AABB(o.x - 8.0, o.y, o.z - 0.01, o.x + 8.0, o.y + 0.02, o.z + 0.01), 0.55F, 0.85F, 1.0F, 0.8F);
            }
         }

         buffers.endBatch(RenderType.lines());
         pose.popPose();
      }
   }

   private static void rebuild() {
      SculptClient.Ghost g = SculptClient.ghost;
      empty = true;
      if (g != null && !g.accepted().isEmpty()) {
         Long2LongOpenHashMap cells = g.accepted();
         long first = cells.keySet().iterator().nextLong();
         base = new Vec3((double)BlockPos.getX(first), (double)BlockPos.getY(first), (double)BlockPos.getZ(first));
         BufferBuilder b = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
         int total = 0;
         LongIterator fine = cells.values().iterator();

         while (fine.hasNext()) {
            long m = (Long)fine.next();
            total += WaxMask.count(m);
         }

         boolean finex = total <= 20000;
         int quads = 0;
         ObjectIterator mesh = cells.long2LongEntrySet().iterator();

         while (mesh.hasNext()) {
            Entry e = (Entry)mesh.next();
            long key = e.getLongKey();
            int bx = BlockPos.getX(key);
            int by = BlockPos.getY(key);
            int bz = BlockPos.getZ(key);

            int rgb = switch (g.mode()) {
               case REMOVE -> 16747120;
               case RESHAPE -> 10279167;
               case PAINT -> WaxPalette.tint(SculptClient.color, false);
               default -> colour(g.stress() == null ? WaxIntegrity.Stress.OK : g.stress().of(key), g.ok());
            };
            float ox = (float)((double)bx - base.x);
            float oy = (float)((double)by - base.y);
            float oz = (float)((double)bz - base.z);
            if (finex) {
               long mask = e.getLongValue();
               long left = mask;

               while (left != 0L) {
                  int c = Long.numberOfTrailingZeros(left);
                  left &= left - 1L;
                  int cx = c & 3;
                  int cz = c >> 2 & 3;
                  int cy = c >> 4;

                  for (Direction d : DIRS) {
                     if (!filled(cells, bx * 4 + cx + d.getStepX(), by * 4 + cy + d.getStepY(), bz * 4 + cz + d.getStepZ())) {
                        face(b, ox + (float)cx / 4.0F, oy + (float)cy / 4.0F, oz + (float)cz / 4.0F, 0.25F, d, rgb);
                        quads++;
                     }
                  }
               }
            } else {
               for (Direction dx : DIRS) {
                  if (!cells.containsKey(BlockPos.offset(key, dx))) {
                     face(b, ox, oy, oz, 1.0F, dx, rgb);
                     quads++;
                  }
               }
            }
         }

         MeshData meshx = b.build();
         if (meshx != null && quads != 0) {
            if (buffer == null) {
               buffer = new VertexBuffer(Usage.DYNAMIC);
            }

            buffer.bind();
            buffer.upload(meshx);
            VertexBuffer.unbind();
            empty = false;
         }
      }
   }

   private static boolean filled(Long2LongOpenHashMap cells, int sx, int sy, int sz) {
      long m = cells.get(BlockPos.asLong(sx >> 2, sy >> 2, sz >> 2));
      return m != 0L && WaxMask.has(m, sx & 3, sy & 3, sz & 3);
   }

   private static int colour(WaxIntegrity.Stress stress, boolean ok) {
      int mix;
      float k;
      if (!ok && stress == WaxIntegrity.Stress.OK) {
         mix = 16738890;
         k = 0.45F;
      } else {
         switch (stress) {
            case STRESSED:
               mix = 16757820;
               k = 0.6F;
               break;
            case UNSUPPORTED:
               mix = 16726574;
               k = 0.75F;
               break;
            default:
               mix = 8191884;
               k = 0.3F;
         }
      }

      int base = 16774886;
      int r = (int)((float)(base >> 16 & 0xFF) * (1.0F - k) + (float)(mix >> 16 & 0xFF) * k);
      int g = (int)((float)(base >> 8 & 0xFF) * (1.0F - k) + (float)(mix >> 8 & 0xFF) * k);
      int bl = (int)((float)(base & 0xFF) * (1.0F - k) + (float)(mix & 0xFF) * k);
      return r << 16 | g << 8 | bl;
   }

   private static void face(BufferBuilder b, float x, float y, float z, float s, Direction d, int rgb) {
      float shade = switch (d) {
         case UP -> 1.0F;
         case DOWN -> 0.55F;
         case NORTH, SOUTH -> 0.82F;
         default -> 0.68F;
      };
      int a = d == Direction.UP ? 120 : 92;
      int r = (int)((float)(rgb >> 16 & 0xFF) * shade);
      int g = (int)((float)(rgb >> 8 & 0xFF) * shade);
      int bl = (int)((float)(rgb & 0xFF) * shade);
      float x1 = x + s;
      float y1 = y + s;
      float z1 = z + s;
      switch (d) {
         case UP:
            quad(b, x, y1, z, x, y1, z1, x1, y1, z1, x1, y1, z, r, g, bl, a);
            break;
         case DOWN:
            quad(b, x, y, z1, x, y, z, x1, y, z, x1, y, z1, r, g, bl, a);
            break;
         case NORTH:
            quad(b, x1, y1, z, x1, y, z, x, y, z, x, y1, z, r, g, bl, a);
            break;
         case SOUTH:
            quad(b, x, y1, z1, x, y, z1, x1, y, z1, x1, y1, z1, r, g, bl, a);
            break;
         case WEST:
            quad(b, x, y1, z, x, y, z, x, y, z1, x, y1, z1, r, g, bl, a);
            break;
         default:
            quad(b, x1, y1, z1, x1, y, z1, x1, y, z, x1, y1, z, r, g, bl, a);
      }
   }

   private static void quad(
      BufferBuilder b,
      float x0,
      float y0,
      float z0,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      float x3,
      float y3,
      float z3,
      int r,
      int g,
      int bl,
      int a
   ) {
      b.addVertex(x0, y0, z0).setColor(r, g, bl, a);
      b.addVertex(x1, y1, z1).setColor(r, g, bl, a);
      b.addVertex(x2, y2, z2).setColor(r, g, bl, a);
      b.addVertex(x3, y3, z3).setColor(r, g, bl, a);
   }

   private SculptGhost() {
   }
}
