package com.awakenedmod.awakened.client.render;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3fc;

public final class SuitModelUtil {
   public static final boolean THRUSTER_FACES_TO_UNDERSIDE = true;
   private static Field cubesField;
   private static Field polygonsField;
   private static Field verticesField;
   private static Field normalField;
   private static Field uField;
   private static Field vField;
   private static Method remapMethod;

   private SuitModelUtil() {
   }

   public static void applyThrusterUnderside(ModelPart root) {
      ModelPart gauntlets = root.getChild("Gauntlets");
      swapUpDownFaceUv(gauntlets.getChild("LeftGauntlet"), 0);
      swapUpDownFaceUv(gauntlets.getChild("RightGauntlet"), 0);
      swapUpDownFaceUv(root.getChild("LeftBoot"), 0);
      swapUpDownFaceUv(root.getChild("RightBoot"), 0);
   }

   public static void swapUpDownFaceUv(ModelPart part, int cubeIndex) {
      try {
         List<?> cubes = cubes(part);
         if (cubeIndex >= 0 && cubeIndex < cubes.size()) {
            Object[] polygons = polygons(cubes.get(cubeIndex));
            Object up = null;
            Object down = null;

            for (Object polygon : polygons) {
               float ny = normalY(polygon);
               if (ny > 0.5F) {
                  up = polygon;
               } else if (ny < -0.5F) {
                  down = polygon;
               }
            }

            if (up != null && down != null) {
               swapPolygonUv(up, down);
            } else {
               throw new IllegalStateException("Cube has no UP or DOWN face to swap");
            }
         } else {
            throw new IllegalArgumentException("cube index " + cubeIndex + " out of range (" + cubes.size() + ")");
         }
      } catch (ReflectiveOperationException var11) {
         throw new IllegalStateException("Failed to swap up/down face UVs", var11);
      }
   }

   private static void swapPolygonUv(Object up, Object down) throws ReflectiveOperationException {
      Object[] upV = vertices(up);
      Object[] downV = vertices(down);
      int n = Math.min(upV.length, downV.length);

      for (int i = 0; i < n; i++) {
         float upU = u(upV[i]);
         float upVv = v(upV[i]);
         float dnU = u(downV[i]);
         float dnVv = v(downV[i]);
         upV[i] = remap(upV[i], dnU, dnVv);
         downV[i] = remap(downV[i], upU, upVv);
      }
   }

   private static List<?> cubes(ModelPart part) throws ReflectiveOperationException {
      if (cubesField == null) {
         cubesField = ModelPart.class.getDeclaredField("cubes");
         cubesField.setAccessible(true);
      }

      return (List<?>)cubesField.get(part);
   }

   private static Object[] polygons(Object cube) throws ReflectiveOperationException {
      if (polygonsField == null) {
         polygonsField = cube.getClass().getDeclaredField("polygons");
         polygonsField.setAccessible(true);
      }

      return (Object[])polygonsField.get(cube);
   }

   private static Object[] vertices(Object polygon) throws ReflectiveOperationException {
      if (verticesField == null) {
         verticesField = polygon.getClass().getDeclaredField("vertices");
         verticesField.setAccessible(true);
      }

      return (Object[])verticesField.get(polygon);
   }

   private static float normalY(Object polygon) throws ReflectiveOperationException {
      if (normalField == null) {
         normalField = polygon.getClass().getDeclaredField("normal");
         normalField.setAccessible(true);
      }

      return ((Vector3fc)normalField.get(polygon)).y();
   }

   private static float u(Object vertex) throws ReflectiveOperationException {
      if (uField == null) {
         uField = vertex.getClass().getDeclaredField("u");
         uField.setAccessible(true);
      }

      return uField.getFloat(vertex);
   }

   private static float v(Object vertex) throws ReflectiveOperationException {
      if (vField == null) {
         vField = vertex.getClass().getDeclaredField("v");
         vField.setAccessible(true);
      }

      return vField.getFloat(vertex);
   }

   private static Object remap(Object vertex, float u, float v) throws ReflectiveOperationException {
      if (remapMethod == null) {
         remapMethod = vertex.getClass().getDeclaredMethod("remap", float.class, float.class);
         remapMethod.setAccessible(true);
      }

      return remapMethod.invoke(vertex, u, v);
   }
}
