package com.awakenedmod.awakened.client.npc;

import com.awakenedmod.awakened.Awakened;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

public final class NpcClipLibrary {
   private static final String DIR = "player_animations";
   @Nullable
   private static Map<ResourceLocation, NpcClipLibrary.Clip> clips;

   private NpcClipLibrary() {
   }

   @Nullable
   public static NpcClipLibrary.Clip get(@Nullable ResourceLocation id) {
      if (id == null) {
         return null;
      } else {
         if (clips == null) {
            clips = load();
         }

         return clips.get(id);
      }
   }

   private static Map<ResourceLocation, NpcClipLibrary.Clip> load() {
      Map<ResourceLocation, NpcClipLibrary.Clip> out = new HashMap<>();
      Map<ResourceLocation, Resource> files = Minecraft.getInstance()
         .getResourceManager()
         .listResources("player_animations", filex -> filex.getPath().endsWith(".json"));

      for (Entry<ResourceLocation, Resource> file : files.entrySet()) {
         try (Reader reader = file.getValue().openAsReader()) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject animations = root.getAsJsonObject("animations");
            if (animations != null) {
               for (Entry<String, JsonElement> anim : animations.entrySet()) {
                  if (anim.getValue().isJsonObject()) {
                     out.put(
                        ResourceLocation.fromNamespaceAndPath(file.getKey().getNamespace(), anim.getKey()),
                        NpcClipLibrary.Clip.parse(anim.getValue().getAsJsonObject())
                     );
                  }
               }
            }
         } catch (Exception var11) {
            Awakened.LOGGER.warn("Failed to read NPC clip {}: {}", file.getKey(), var11.toString());
         }
      }

      return out;
   }

   public static enum Bone {
      HEAD("head"),
      BODY("body"),
      TORSO("torso"),
      RIGHT_ARM("right_arm"),
      LEFT_ARM("left_arm"),
      RIGHT_LEG("right_leg"),
      LEFT_LEG("left_leg");

      public final String libName;

      private Bone(String libName) {
         this.libName = libName;
      }

      @Nullable
      public static NpcClipLibrary.Bone of(String name) {
         for (NpcClipLibrary.Bone b : values()) {
            if (b.libName.equals(name)) {
               return b;
            }
         }

         return null;
      }
   }

   public static final class Clip {
      private final float length;
      private final boolean loop;
      private final boolean keeps;
      private final NpcClipLibrary.Track[] rotation = new NpcClipLibrary.Track[NpcClipLibrary.Bone.values().length];
      private final NpcClipLibrary.Track[] position = new NpcClipLibrary.Track[NpcClipLibrary.Bone.values().length];

      private Clip(float length, boolean loop, boolean keeps) {
         this.length = length;
         this.loop = loop;
         this.keeps = keeps;
      }

      static NpcClipLibrary.Clip parse(JsonObject json) {
         float length = json.has("animation_length") ? json.get("animation_length").getAsFloat() : 0.0F;
         JsonElement loopEl = json.get("loop");
         boolean prim = loopEl != null && loopEl.isJsonPrimitive();
         boolean loop = prim && (loopEl.getAsJsonPrimitive().isBoolean() ? loopEl.getAsBoolean() : "true".equals(loopEl.getAsString()));
         boolean hold = prim && "hold_on_last_frame".equals(loopEl.getAsString());
         NpcClipLibrary.Clip clip = new NpcClipLibrary.Clip(length, loop, loop || hold);
         JsonObject bones = json.getAsJsonObject("bones");
         if (bones != null) {
            for (Entry<String, JsonElement> e : bones.entrySet()) {
               NpcClipLibrary.Bone bone = NpcClipLibrary.Bone.of(e.getKey());
               if (bone != null && e.getValue().isJsonObject()) {
                  JsonObject b = e.getValue().getAsJsonObject();
                  clip.rotation[bone.ordinal()] = NpcClipLibrary.Track.parse(b.get("rotation"));
                  clip.position[bone.ordinal()] = NpcClipLibrary.Track.parse(b.get("position"));
               }
            }
         }

         return clip;
      }

      public float length() {
         return this.length;
      }

      public boolean loops() {
         return this.keeps;
      }

      public boolean drives(NpcClipLibrary.Bone bone) {
         return this.rotation[bone.ordinal()] != null || this.position[bone.ordinal()] != null;
      }

      public void rotation(NpcClipLibrary.Bone bone, float seconds, float[] out) {
         this.sample(this.rotation[bone.ordinal()], seconds, out);
      }

      public void position(NpcClipLibrary.Bone bone, float seconds, float[] out) {
         this.sample(this.position[bone.ordinal()], seconds, out);
      }

      private void sample(@Nullable NpcClipLibrary.Track track, float seconds, float[] out) {
         if (track == null) {
            out[0] = out[1] = out[2] = 0.0F;
         } else {
            float t = this.loop && this.length > 0.0F ? seconds % this.length : seconds;
            track.sample(t, out);
         }
      }
   }

   private static final class Track {
      private final float[] times;
      private final float[][] values;
      private final Float2FloatFunction[] easings;

      private Track(float[] times, float[][] values, Float2FloatFunction[] easings) {
         this.times = times;
         this.values = values;
         this.easings = easings;
      }

      @Nullable
      static NpcClipLibrary.Track parse(@Nullable JsonElement el) {
         if (el == null) {
            return null;
         } else {
            List<float[]> keys = new ArrayList<>();
            List<Float2FloatFunction> eases = new ArrayList<>();
            if (el.isJsonArray()) {
               float[] v = vector(el);
               if (v != null) {
                  keys.add(new float[]{0.0F, v[0], v[1], v[2]});
                  eases.add(null);
               }
            } else if (el.isJsonObject()) {
               for (Entry<String, JsonElement> k : el.getAsJsonObject().entrySet()) {
                  float time;
                  try {
                     time = Float.parseFloat(k.getKey());
                  } catch (NumberFormatException var11) {
                     continue;
                  }

                  JsonElement value = k.getValue();
                  Float2FloatFunction ease = null;
                  float[] v;
                  if (value.isJsonObject()) {
                     JsonObject o = value.getAsJsonObject();
                     JsonElement vec = o.has("vector") ? o.get("vector") : (o.has("post") ? o.get("post") : o.get("pre"));
                     v = vector(vec);
                     if (o.has("easing")) {
                        ease = NpcEasing.named(o.get("easing").getAsString());
                     }
                  } else {
                     v = vector(value);
                  }

                  if (v != null) {
                     keys.add(new float[]{time, v[0], v[1], v[2]});
                     eases.add(ease);
                  }
               }
            }

            if (keys.isEmpty()) {
               return null;
            } else {
               Integer[] order = new Integer[keys.size()];

               for (int i = 0; i < order.length; i++) {
                  order[i] = i;
               }

               Arrays.sort(order, (a, b) -> Float.compare(keys.get(a)[0], keys.get(b)[0]));
               float[] times = new float[order.length];
               float[][] values = new float[order.length][];
               Float2FloatFunction[] easings = new Float2FloatFunction[order.length];

               for (int i = 0; i < order.length; i++) {
                  float[] k = keys.get(order[i]);
                  times[i] = k[0];
                  values[i] = new float[]{k[1], k[2], k[3]};
                  easings[i] = eases.get(order[i]);
               }

               return new NpcClipLibrary.Track(times, values, easings);
            }
         }
      }

      @Nullable
      private static float[] vector(@Nullable JsonElement el) {
         if (el != null && el.isJsonArray()) {
            JsonArray a = el.getAsJsonArray();
            if (a.size() < 3) {
               return null;
            } else {
               float[] v = new float[3];

               for (int i = 0; i < 3; i++) {
                  try {
                     v[i] = a.get(i).getAsFloat();
                  } catch (RuntimeException var5) {
                     v[i] = 0.0F;
                  }
               }

               return v;
            }
         } else {
            return null;
         }
      }

      void sample(float t, float[] out) {
         int n = this.times.length;
         if (!(t <= this.times[0]) && n != 1) {
            for (int i = 1; i < n; i++) {
               if (t < this.times[i]) {
                  float span = this.times[i] - this.times[i - 1];
                  float f = span <= 1.0E-5F ? 1.0F : (t - this.times[i - 1]) / span;
                  if (this.easings[i] != null) {
                     f = this.easings[i].get(f);
                  }

                  float[] a = this.values[i - 1];
                  float[] b = this.values[i];
                  out[0] = a[0] + (b[0] - a[0]) * f;
                  out[1] = a[1] + (b[1] - a[1]) * f;
                  out[2] = a[2] + (b[2] - a[2]) * f;
                  return;
               }
            }

            copy(this.values[n - 1], out);
         } else {
            copy(this.values[0], out);
         }
      }

      private static void copy(float[] v, float[] out) {
         out[0] = v[0];
         out[1] = v[1];
         out[2] = v[2];
      }
   }
}
