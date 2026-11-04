package com.devilfruits.compat.sable.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.fruits.suke_suke.clear.ShipView;
import com.devilfruits.fruits.suke_suke.client.ShipLook;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.ClientSubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class SableShipLook implements ShipLook.Bridge {
   private static final Matrix4f NOWHERE = new Matrix4f().scale(0.0F);
   private static final Matrix3f NO_NORMAL = new Matrix3f();
   private static Field visualEntity;
   private static Field visualEmbedding;
   private static Method embeddingTransforms;
   private static boolean reflectionFailed;

   public static void install() {
      ShipLook.bridge = new SableShipLook();
   }

   @Nullable
   private static SubLevel byId(Level level, UUID id) {
      SubLevelContainer container = SubLevelContainer.getContainer(level);
      SubLevel s = container == null ? null : container.getSubLevel(id);
      return s != null && !s.isRemoved() ? s : null;
   }

   @Nullable
   private static BoundingBox3ic bounds(SubLevel s) {
      BoundingBox3ic b = s.getPlot().getBoundingBox();
      return b != null && b != BoundingBox3i.EMPTY && b.maxX() >= b.minX() ? b : null;
   }

   private static Pose3dc pose(SubLevel s, float partial) {
      return (Pose3dc)(s instanceof ClientSubLevelAccess client ? client.renderPose(partial) : s.logicalPose());
   }

   public static boolean hidesParts(Vec3i origin) {
      Level level = Minecraft.getInstance().level;
      if (level == null) {
         return false;
      } else {
         SubLevel s = Sable.HELPER.getContaining(level, origin);
         if (s != null && !s.isRemoved()) {
            float a = ShipLook.alpha(s.getUniqueId(), 1.0F);
            return a >= 0.0F && a < 0.5F;
         } else {
            return false;
         }
      }
   }

   public static void collapseContraption(Object visual) {
      if (ShipView.any() && !reflectionFailed) {
         try {
            if (visualEntity == null) {
               Class<?> c = visual.getClass();
               Field embedding = null;
               Field entity = null;

               for (Class<?> k = c; k != null && (embedding == null || entity == null); k = k.getSuperclass()) {
                  for (Field f : k.getDeclaredFields()) {
                     if (embedding == null && f.getName().equals("embedding")) {
                        embedding = f;
                     } else if (entity == null && f.getName().equals("entity") && Entity.class.isAssignableFrom(f.getType())) {
                        entity = f;
                     }
                  }
               }

               if (embedding == null || entity == null) {
                  reflectionFailed = true;
                  return;
               }

               embedding.setAccessible(true);
               entity.setAccessible(true);
               embeddingTransforms = embedding.getType().getMethod("transforms", Matrix4fc.class, Matrix3fc.class);
               visualEmbedding = embedding;
               visualEntity = entity;
            }

            if (visualEntity.get(visual) instanceof Entity e && hidesParts(e.blockPosition())) {
               embeddingTransforms.invoke(visualEmbedding.get(visual), NOWHERE, NO_NORMAL);
            }
         } catch (RuntimeException | ReflectiveOperationException var9) {
            reflectionFailed = true;
            DevilFruits.LOGGER.warn("Ship Invisibility cannot hide Create contraptions on ships", var9);
         }
      }
   }

   @Nullable
   @Override
   public UUID shipAt(Level level, BlockPos pos) {
      SubLevel s = Sable.HELPER.getContaining(level, pos);
      return s != null && !s.isRemoved() ? s.getUniqueId() : null;
   }

   @Nullable
   @Override
   public UUID shipAround(Level level, Vec3 pos) {
      for (UUID id : ShipView.all().keySet()) {
         SubLevel s = byId(level, id);
         BoundingBox3ic b = s == null ? null : bounds(s);
         if (b != null) {
            Vec3 local = pose(s, 1.0F).transformPositionInverse(pos);
            if (local.x >= (double)(b.minX() - 1)
               && local.x <= (double)(b.maxX() + 2)
               && local.y >= (double)(b.minY() - 1)
               && local.y <= (double)(b.maxY() + 2)
               && local.z >= (double)(b.minZ() - 1)
               && local.z <= (double)(b.maxZ() + 2)) {
               return id;
            }
         }
      }

      return null;
   }

   @Override
   public boolean corners(Level level, UUID ship, float partial, double[] out) {
      SubLevel s = byId(level, ship);
      BoundingBox3ic b = s == null ? null : bounds(s);
      if (b == null) {
         return false;
      } else {
         Pose3dc pose = pose(s, partial);
         int i = 0;

         for (int k = 0; k < 8; k++) {
            Vec3 c = pose.transformPosition(
               new Vec3(
                  (k & 1) == 0 ? (double)b.minX() : (double)(b.maxX() + 1),
                  (k & 2) == 0 ? (double)b.minY() : (double)(b.maxY() + 1),
                  (k & 4) == 0 ? (double)b.minZ() : (double)(b.maxZ() + 1)
               )
            );
            out[i++] = c.x;
            out[i++] = c.y;
            out[i++] = c.z;
         }

         return true;
      }
   }
}
