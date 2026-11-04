package com.devilfruits.client.model.geo;

import com.devilfruits.client.animation.AnimationClip;
import com.devilfruits.client.animation.Easing;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class GeoAnimation implements AnimationClip {
   private static final int LINEAR = 0;
   private static final int CATMULLROM = 1;
   private static final int STEP = 2;
   public final ResourceLocation id;
   private final float lengthTicks;
   private final boolean loop;
   private final float fadeIn;
   private final float fadeOut;
   private final GeoAnimation.BoneTracks[] tracks;
   private final Map<GeoModel, int[]> bindings = new WeakHashMap<>();
   private final float[] scratch = new float[3];
   private Map<String, Float> markers = Map.of();

   public float marker(String name, float fallback) {
      Float t = this.markers.get(name);
      return t != null ? t : fallback;
   }

   private GeoAnimation(ResourceLocation id, float lengthTicks, boolean loop, float fadeIn, float fadeOut, GeoAnimation.BoneTracks[] tracks) {
      this.id = id;
      this.lengthTicks = lengthTicks;
      this.loop = loop;
      this.fadeIn = fadeIn;
      this.fadeOut = fadeOut;
      this.tracks = tracks;
   }

   @Override
   public float length() {
      return this.lengthTicks;
   }

   @Override
   public boolean loop() {
      return this.loop;
   }

   @Override
   public float fadeIn() {
      return this.fadeIn;
   }

   @Override
   public float fadeOut() {
      return this.fadeOut;
   }

   private int[] binding(GeoModel model) {
      int[] b = this.bindings.get(model);
      if (b == null) {
         b = new int[this.tracks.length];

         for (int i = 0; i < this.tracks.length; i++) {
            b[i] = model.bone(this.tracks[i].bone);
         }

         this.bindings.put(model, b);
      }

      return b;
   }

   public void add(GeoModel model, GeoPose pose, float timeTicks, float weight) {
      this.apply(model, pose, timeTicks, weight, false);
   }

   public void blend(GeoModel model, GeoPose pose, float timeTicks, float weight) {
      this.apply(model, pose, timeTicks, weight, true);
   }

   private void apply(GeoModel model, GeoPose pose, float timeTicks, float weight, boolean override) {
      if (!(weight <= 0.001F)) {
         int[] bind = this.binding(model);
         float seconds = timeTicks / 20.0F;
         float[] v = this.scratch;

         for (int i = 0; i < this.tracks.length; i++) {
            int bone = bind[i];
            if (bone >= 0) {
               GeoAnimation.BoneTracks bt = this.tracks[i];
               int o = bone * 3;
               if (bt.rotation != null) {
                  bt.rotation.sample(seconds, v);
                  mix(pose.rot, o, v, weight, override);
               }

               if (bt.position != null) {
                  bt.position.sample(seconds, v);
                  mix(pose.pos, o, v, weight, override);
               }

               if (bt.scale != null) {
                  bt.scale.sample(seconds, v);
                  if (override) {
                     mix(pose.scale, o, v, weight, true);
                  } else {
                     for (int k = 0; k < 3; k++) {
                        pose.scale[o + k] = pose.scale[o + k] * (1.0F + (v[k] - 1.0F) * weight);
                     }
                  }
               }
            }
         }
      }
   }

   private static void mix(float[] target, int o, float[] v, float w, boolean override) {
      for (int k = 0; k < 3; k++) {
         target[o + k] = override ? target[o + k] + (v[k] - target[o + k]) * w : target[o + k] + v[k] * w;
      }
   }

   static List<GeoAnimation> parseFile(String namespace, String prefix, JsonObject root) {
      List<GeoAnimation> out = new ArrayList<>();
      JsonObject animations = root.getAsJsonObject("animations");

      for (Entry<String, JsonElement> e : animations.entrySet()) {
         String name = e.getKey();
         String shortName = name.startsWith("animation.") && name.indexOf(46, 10) > 0 ? name.substring(name.indexOf(46, 10) + 1) : name;
         ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, prefix + shortName.toLowerCase(Locale.ROOT));
         out.add(parseClip(id, e.getValue().getAsJsonObject()));
      }

      return out;
   }

   private static GeoAnimation parseClip(ResourceLocation id, JsonObject json) {
      boolean loop = json.has("loop")
         && json.get("loop").isJsonPrimitive()
         && json.get("loop").getAsJsonPrimitive().isBoolean()
         && json.get("loop").getAsBoolean();
      float length = 0.0F;
      List<GeoAnimation.BoneTracks> tracks = new ArrayList<>();
      if (json.has("bones")) {
         for (Entry<String, JsonElement> e : json.getAsJsonObject("bones").entrySet()) {
            JsonObject b = e.getValue().getAsJsonObject();
            GeoAnimation.Track rot = b.has("rotation") ? track(b.get("rotation"), true, false) : null;
            GeoAnimation.Track pos = b.has("position") ? track(b.get("position"), false, true) : null;
            GeoAnimation.Track scale = b.has("scale") ? track(b.get("scale"), false, false) : null;

            for (GeoAnimation.Track t : new GeoAnimation.Track[]{rot, pos, scale}) {
               if (t != null) {
                  length = Math.max(length, t.times[t.times.length - 1]);
               }
            }

            tracks.add(new GeoAnimation.BoneTracks(e.getKey(), rot, pos, scale));
         }
      }

      if (json.has("animation_length")) {
         length = json.get("animation_length").getAsFloat();
      }

      float fadeIn = json.has("devilfruits:fade_in") ? json.get("devilfruits:fade_in").getAsFloat() : 3.0F;
      float fadeOut = json.has("devilfruits:fade_out") ? json.get("devilfruits:fade_out").getAsFloat() : 4.0F;
      GeoAnimation clip = new GeoAnimation(id, length * 20.0F, loop, fadeIn, fadeOut, tracks.toArray(new GeoAnimation.BoneTracks[0]));
      if (json.has("devilfruits:markers")) {
         Map<String, Float> markers = new HashMap<>();

         for (Entry<String, JsonElement> e : json.getAsJsonObject("devilfruits:markers").entrySet()) {
            markers.put(e.getKey(), e.getValue().getAsFloat());
         }

         clip.markers = Map.copyOf(markers);
      }

      return clip;
   }

   private static GeoAnimation.Track track(JsonElement json, boolean rotation, boolean position) {
      TreeMap<Float, JsonElement> frames = new TreeMap<>();
      if (json.isJsonObject() && !isValueObject(json.getAsJsonObject())) {
         for (Entry<String, JsonElement> e : json.getAsJsonObject().entrySet()) {
            frames.put(Float.parseFloat(e.getKey()), e.getValue());
         }
      } else {
         frames.put(0.0F, json);
      }

      int n = frames.size();
      float[] times = new float[n];
      float[] values = new float[n * 3];
      int[] modes = new int[n];
      Easing[] easings = new Easing[n];
      int i = 0;

      for (Entry<Float, JsonElement> e : frames.entrySet()) {
         times[i] = e.getKey();
         JsonElement value = e.getValue();
         int mode = 0;
         Easing easing = Easing.LINEAR;
         if (value.isJsonObject()) {
            JsonObject o = value.getAsJsonObject();
            if (o.has("lerp_mode")) {
               String m = o.get("lerp_mode").getAsString();
               mode = "catmullrom".equals(m) ? 1 : ("step".equals(m) ? 2 : 0);
            }

            if (o.has("easing")) {
               easing = Easing.parse(geckoEasing(o.get("easing").getAsString()));
            }

            value = o.has("vector") ? o.get("vector") : (o.has("post") ? o.get("post") : o.get("pre"));
         }

         float[] v = vector(value, rotation ? 0.0F : (position ? 0.0F : 1.0F));
         if (rotation) {
            v[0] = -v[0] * (float) (Math.PI / 180.0);
            v[1] = -v[1] * (float) (Math.PI / 180.0);
            v[2] *= (float) (Math.PI / 180.0);
         } else if (position) {
            v[0] = -v[0];
         }

         System.arraycopy(v, 0, values, i * 3, 3);
         modes[i] = mode;
         easings[i] = easing;
         i++;
      }

      return new GeoAnimation.Track(times, values, modes, easings);
   }

   private static boolean isValueObject(JsonObject o) {
      return o.has("vector") || o.has("post") || o.has("pre");
   }

   private static float[] vector(JsonElement value, float def) {
      if (value == null) {
         return new float[]{def, def, def};
      } else if (value.isJsonPrimitive()) {
         float f = number(value, def);
         return new float[]{f, f, f};
      } else {
         JsonArray a = value.getAsJsonArray();
         return new float[]{number(a.get(0), def), number(a.get(1), def), number(a.get(2), def)};
      }
   }

   private static float number(JsonElement e, float def) {
      try {
         return e.getAsFloat();
      } catch (RuntimeException var3) {
         return def;
      }
   }

   static String geckoEasing(String name) {
      if (!name.startsWith("ease")) {
         return name;
      } else {
         StringBuilder sb = new StringBuilder();

         for (char ch : name.substring(4).toCharArray()) {
            if (Character.isUpperCase(ch) && !sb.isEmpty()) {
               sb.append('_');
            }

            sb.append(Character.toLowerCase(ch));
         }

         return sb.toString();
      }
   }

   static record BoneTracks(String bone, @Nullable GeoAnimation.Track rotation, @Nullable GeoAnimation.Track position, @Nullable GeoAnimation.Track scale) {
   }

   static final class Track {
      final float[] times;
      final float[] values;
      final int[] modes;
      final Easing[] easings;

      Track(float[] times, float[] values, int[] modes, Easing[] easings) {
         this.times = times;
         this.values = values;
         this.modes = modes;
         this.easings = easings;
      }

      void sample(float t, float[] out) {
         int n = this.times.length;
         if (n != 1 && !(t <= this.times[0])) {
            if (t >= this.times[n - 1]) {
               this.copy(n - 1, out);
            } else {
               int i = 0;

               while (i + 1 < n && this.times[i + 1] <= t) {
                  i++;
               }

               float p = (t - this.times[i]) / Math.max(1.0E-5F, this.times[i + 1] - this.times[i]);
               if (this.modes[i] == 2) {
                  this.copy(i, out);
               } else {
                  p = this.easings[i + 1].apply(Mth.clamp(p, 0.0F, 1.0F));
                  boolean catmull = this.modes[i] == 1 || this.modes[i + 1] == 1;

                  for (int k = 0; k < 3; k++) {
                     float p1 = this.values[i * 3 + k];
                     float p2 = this.values[(i + 1) * 3 + k];
                     if (catmull) {
                        float p0 = this.values[Math.max(0, i - 1) * 3 + k];
                        float p3 = this.values[Math.min(n - 1, i + 2) * 3 + k];
                        out[k] = catmullRom(p0, p1, p2, p3, p);
                     } else {
                        out[k] = p1 + (p2 - p1) * p;
                     }
                  }
               }
            }
         } else {
            this.copy(0, out);
         }
      }

      private void copy(int i, float[] out) {
         out[0] = this.values[i * 3];
         out[1] = this.values[i * 3 + 1];
         out[2] = this.values[i * 3 + 2];
      }

      private static float catmullRom(float p0, float p1, float p2, float p3, float t) {
         float t2 = t * t;
         float t3 = t2 * t;
         return 0.5F * (2.0F * p1 + (-p0 + p2) * t + (2.0F * p0 - 5.0F * p1 + 4.0F * p2 - p3) * t2 + (-p0 + 3.0F * p1 - 3.0F * p2 + p3) * t3);
      }
   }
}
