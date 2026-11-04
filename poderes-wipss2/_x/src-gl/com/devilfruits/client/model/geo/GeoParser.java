package com.devilfruits.client.model.geo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Vector3f;

final class GeoParser {
   static GeoModel parse(ResourceLocation id, JsonObject root) {
      JsonObject geo = root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
      JsonObject description = geo.getAsJsonObject("description");
      int tw = description.get("texture_width").getAsInt();
      int th = description.get("texture_height").getAsInt();
      Map<String, GeoParser.RawBone> raw = new LinkedHashMap<>();

      for (JsonElement e : geo.getAsJsonArray("bones")) {
         JsonObject b = e.getAsJsonObject();
         String name = b.get("name").getAsString();
         String parent = b.has("parent") ? b.get("parent").getAsString() : null;
         raw.put(name, new GeoParser.RawBone(name, parent, b));
      }

      List<GeoParser.RawBone> ordered = new ArrayList<>();
      Map<String, Integer> index = new HashMap<>();

      while (ordered.size() < raw.size()) {
         boolean progress = false;

         for (GeoParser.RawBone bone : raw.values()) {
            if (!index.containsKey(bone.name) && (bone.parent == null || index.containsKey(bone.parent) || !raw.containsKey(bone.parent))) {
               index.put(bone.name, ordered.size());
               ordered.add(bone);
               progress = true;
            }
         }

         if (!progress) {
            throw new IllegalStateException(id + ": bone hierarchy has a cycle");
         }
      }

      GeoModel.Bone[] bones = new GeoModel.Bone[ordered.size()];

      for (int i = 0; i < ordered.size(); i++) {
         GeoParser.RawBone rb = ordered.get(i);
         JsonObject b = rb.json;
         float[] pivot = toJavaPoint(vec(b, "pivot", 0.0F));
         float[] rot = toJavaRotation(vec(b, "rotation", 0.0F));
         boolean boneMirror = b.has("mirror") && b.get("mirror").getAsBoolean();
         FloatArrayList mesh = new FloatArrayList();
         if (b.has("cubes")) {
            for (JsonElement c : b.getAsJsonArray("cubes")) {
               bakeCube(c.getAsJsonObject(), boneMirror, tw, th, mesh);
            }
         }

         int parent = rb.parent != null && index.containsKey(rb.parent) ? index.get(rb.parent) : -1;
         bones[i] = new GeoModel.Bone(rb.name, i, parent, pivot, rot, mesh.toFloatArray());
      }

      return new GeoModel(id, tw, th, bones);
   }

   private static void bakeCube(JsonObject c, boolean boneMirror, int tw, int th, FloatArrayList out) {
      float[] origin = vec(c, "origin", 0.0F);
      float[] size = vec(c, "size", 0.0F);
      float inflate = c.has("inflate") ? c.get("inflate").getAsFloat() : 0.0F;
      boolean mirror = c.has("mirror") ? c.get("mirror").getAsBoolean() : boneMirror;
      float x0 = -(origin[0] + size[0]) - inflate;
      float x1 = -origin[0] + inflate;
      float y0 = origin[1] - inflate;
      float y1 = origin[1] + size[1] + inflate;
      float z0 = origin[2] - inflate;
      float z1 = origin[2] + size[2] + inflate;
      Matrix3f rotation = null;
      float[] pivot = null;
      if (c.has("rotation")) {
         float[] r = toJavaRotation(vec(c, "rotation", 0.0F));
         if (r[0] != 0.0F || r[1] != 0.0F || r[2] != 0.0F) {
            rotation = new Matrix3f().rotateZ(r[2]).rotateY(r[1]).rotateX(r[0]);
            pivot = toJavaPoint(vec(c, "pivot", 0.0F));
         }
      }

      float w = size[0];
      float h = size[1];
      float d = size[2];
      Map<String, float[]> uv = new HashMap<>();
      JsonElement uvJson = c.get("uv");
      if (uvJson != null && uvJson.isJsonObject()) {
         JsonObject faces = uvJson.getAsJsonObject();
         putFace(uv, "front", faces, "north");
         putFace(uv, "back", faces, "south");
         putFace(uv, "left", faces, "east");
         putFace(uv, "right", faces, "west");
         putFace(uv, "top", faces, "up");
         putFace(uv, "bottom", faces, "down");
      } else {
         float u = 0.0F;
         float v = 0.0F;
         if (uvJson != null && uvJson.isJsonArray()) {
            u = uvJson.getAsJsonArray().get(0).getAsFloat();
            v = uvJson.getAsJsonArray().get(1).getAsFloat();
         }

         float[] right = new float[]{u, v + d, u + d, v + d + h};
         float[] left = new float[]{u + d + w, v + d, u + 2.0F * d + w, v + d + h};
         if (mirror) {
            float[] t = right;
            right = left;
            left = t;
         }

         uv.put("right", right);
         uv.put("front", new float[]{u + d, v + d, u + d + w, v + d + h});
         uv.put("left", left);
         uv.put("back", new float[]{u + 2.0F * d + w, v + d, u + 2.0F * d + 2.0F * w, v + d + h});
         uv.put("top", new float[]{u + d, v, u + d + w, v + d});
         uv.put("bottom", new float[]{u + d + w, v, u + d + 2.0F * w, v + d});
         if (mirror) {
            for (float[] f : uv.values()) {
               float t = f[0];
               f[0] = f[2];
               f[2] = t;
            }
         }
      }

      boolean thinX = x1 - x0 < 1.0E-4F;
      boolean thinY = y1 - y0 < 1.0E-4F;
      boolean thinZ = z1 - z0 < 1.0E-4F;
      if (!thinX && !thinY) {
         face(out, uv.get("front"), tw, th, rotation, pivot, 0.0F, 0.0F, -1.0F, x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0);
         face(out, uv.get("back"), tw, th, rotation, pivot, 0.0F, 0.0F, 1.0F, x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1);
      }

      if (!thinZ && !thinY) {
         face(out, uv.get("right"), tw, th, rotation, pivot, 1.0F, 0.0F, 0.0F, x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0);
         face(out, uv.get("left"), tw, th, rotation, pivot, -1.0F, 0.0F, 0.0F, x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1);
      }

      if (!thinX && !thinZ) {
         face(out, uv.get("top"), tw, th, rotation, pivot, 0.0F, 1.0F, 0.0F, x1, y1, z1, x1, y1, z0, x0, y1, z0, x0, y1, z1);
         face(out, uv.get("bottom"), tw, th, rotation, pivot, 0.0F, -1.0F, 0.0F, x1, y0, z0, x1, y0, z1, x0, y0, z1, x0, y0, z0);
      }
   }

   private static void putFace(Map<String, float[]> uv, String key, JsonObject faces, String bedrockName) {
      if (faces.has(bedrockName)) {
         JsonObject f = faces.getAsJsonObject(bedrockName);
         float[] a = vec2(f, "uv");
         float[] s = vec2(f, "uv_size");
         uv.put(key, new float[]{a[0], a[1], a[0] + s[0], a[1] + s[1]});
      }
   }

   private static void face(
      FloatArrayList out, @Nullable float[] uv, int tw, int th, @Nullable Matrix3f rot, @Nullable float[] pivot, float nx, float ny, float nz, float... corners
   ) {
      if (uv != null) {
         float u0 = uv[0] / (float)tw;
         float v0 = uv[1] / (float)th;
         float u1 = uv[2] / (float)tw;
         float v1 = uv[3] / (float)th;
         float[][] uvs = new float[][]{{u0, v0}, {u0, v1}, {u1, v1}, {u1, v0}};
         Vector3f p = new Vector3f();
         Vector3f n = new Vector3f(nx, ny, nz);
         if (rot != null) {
            rot.transform(n);
         }

         for (int i = 0; i < 4; i++) {
            p.set(corners[i * 3], corners[i * 3 + 1], corners[i * 3 + 2]);
            if (rot != null) {
               p.sub(pivot[0], pivot[1], pivot[2]);
               rot.transform(p);
               p.add(pivot[0], pivot[1], pivot[2]);
            }

            out.add(p.x);
            out.add(p.y);
            out.add(p.z);
            out.add(uvs[i][0]);
            out.add(uvs[i][1]);
            out.add(n.x);
            out.add(n.y);
            out.add(n.z);
         }
      }
   }

   static float[] toJavaPoint(float[] bedrock) {
      return new float[]{-bedrock[0], bedrock[1], bedrock[2]};
   }

   static float[] toJavaRotation(float[] degrees) {
      return new float[]{-degrees[0] * (float) (Math.PI / 180.0), -degrees[1] * (float) (Math.PI / 180.0), degrees[2] * (float) (Math.PI / 180.0)};
   }

   private static float[] vec(JsonObject o, String key, float def) {
      if (!o.has(key)) {
         return new float[]{def, def, def};
      } else {
         JsonArray a = o.getAsJsonArray(key);
         return new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
      }
   }

   private static float[] vec2(JsonObject o, String key) {
      JsonArray a = o.getAsJsonArray(key);
      return new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat()};
   }

   private GeoParser() {
   }

   private static record RawBone(String name, @Nullable String parent, JsonObject json) {
   }
}
