package com.devilfruits.client.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;

public final class PlayerAnimation implements AnimationClip {
   public final float length;
   public final boolean loop;
   public final float loopFrom;
   public final float fadeIn;
   public final float fadeOut;
   public final boolean bothArmsFirstPerson;
   public final Map<PlayerAnimation.Part, PlayerAnimation.Channel> parts;
   public final Map<PlayerAnimation.Part, Float> tremble;
   public final PlayerAnimation.Channel root;
   public final PlayerAnimation.Channel fpRight;
   public final PlayerAnimation.Channel fpLeft;

   private PlayerAnimation(
      float length,
      boolean loop,
      float loopFrom,
      float fadeIn,
      float fadeOut,
      boolean bothArms,
      Map<PlayerAnimation.Part, PlayerAnimation.Channel> parts,
      Map<PlayerAnimation.Part, Float> tremble,
      PlayerAnimation.Channel root,
      PlayerAnimation.Channel fpRight,
      PlayerAnimation.Channel fpLeft
   ) {
      this.length = length;
      this.loop = loop;
      this.loopFrom = loopFrom;
      this.fadeIn = fadeIn;
      this.fadeOut = fadeOut;
      this.bothArmsFirstPerson = bothArms;
      this.parts = parts;
      this.tremble = tremble;
      this.root = root;
      this.fpRight = fpRight;
      this.fpLeft = fpLeft;
   }

   @Override
   public float length() {
      return this.length;
   }

   @Override
   public boolean loop() {
      return this.loop;
   }

   @Override
   public float loopFrom() {
      return this.loopFrom;
   }

   @Override
   public float fadeIn() {
      return this.fadeIn;
   }

   @Override
   public float fadeOut() {
      return this.fadeOut;
   }

   public static PlayerAnimation parse(JsonObject json) {
      float length = GsonHelper.getAsFloat(json, "length");
      boolean loop = GsonHelper.getAsBoolean(json, "loop", false);
      float loopFrom = GsonHelper.getAsFloat(json, "loop_from", 0.0F);
      float fadeIn = GsonHelper.getAsFloat(json, "fade_in", 2.0F);
      float fadeOut = GsonHelper.getAsFloat(json, "fade_out", 3.0F);
      boolean bothArms = GsonHelper.getAsBoolean(json, "both_arms_first_person", false);
      Map<PlayerAnimation.Part, PlayerAnimation.Channel> parts = new EnumMap<>(PlayerAnimation.Part.class);
      JsonObject partsJson = GsonHelper.getAsJsonObject(json, "parts", new JsonObject());

      for (Entry<String, JsonElement> e : partsJson.entrySet()) {
         parts.put(PlayerAnimation.Part.parse(e.getKey()), channel(e.getValue().getAsJsonArray(), true));
      }

      Map<PlayerAnimation.Part, Float> tremble = new EnumMap<>(PlayerAnimation.Part.class);
      JsonObject trembleJson = GsonHelper.getAsJsonObject(json, "tremble", new JsonObject());

      for (Entry<String, JsonElement> e : trembleJson.entrySet()) {
         tremble.put(PlayerAnimation.Part.parse(e.getKey()), e.getValue().getAsFloat() * (float) (Math.PI / 180.0));
      }

      PlayerAnimation.Channel root = json.has("root") ? channel(json.getAsJsonArray("root"), true) : null;
      PlayerAnimation.Channel fpRight = json.has("fp_right") ? channel(json.getAsJsonArray("fp_right"), true) : null;
      PlayerAnimation.Channel fpLeft = json.has("fp_left") ? channel(json.getAsJsonArray("fp_left"), true) : null;
      return new PlayerAnimation(length, loop, loopFrom, fadeIn, fadeOut, bothArms, parts, tremble, root, fpRight, fpLeft);
   }

   private static PlayerAnimation.Channel channel(JsonArray array, boolean degrees) {
      List<PlayerAnimation.Keyframe> frames = new ArrayList<>();
      float[] lastRot = new float[]{0.0F, 0.0F, 0.0F};
      float[] lastPos = new float[]{0.0F, 0.0F, 0.0F};

      for (JsonElement element : array) {
         JsonObject k = element.getAsJsonObject();
         float[] rot = k.has("rot") ? vec(k.getAsJsonArray("rot"), degrees ? (float) (Math.PI / 180.0) : 1.0F) : (float[])lastRot.clone();
         float[] pos = k.has("pos") ? vec(k.getAsJsonArray("pos"), 1.0F) : (float[])lastPos.clone();
         frames.add(new PlayerAnimation.Keyframe(GsonHelper.getAsFloat(k, "t"), rot, pos, Easing.parse(GsonHelper.getAsString(k, "ease", "linear"))));
         lastRot = rot;
         lastPos = pos;
      }

      if (frames.isEmpty()) {
         frames.add(new PlayerAnimation.Keyframe(0.0F, new float[3], new float[3], Easing.LINEAR));
      }

      frames.sort((a, b) -> Float.compare(a.t, b.t));
      return new PlayerAnimation.Channel(frames);
   }

   private static float[] vec(JsonArray array, float scale) {
      return new float[]{array.get(0).getAsFloat() * scale, array.get(1).getAsFloat() * scale, array.get(2).getAsFloat() * scale};
   }

   public static final class Channel {
      private final PlayerAnimation.Keyframe[] frames;

      Channel(List<PlayerAnimation.Keyframe> frames) {
         this.frames = frames.toArray(new PlayerAnimation.Keyframe[0]);
      }

      public void sample(float time, float[] rotOut, float[] posOut) {
         PlayerAnimation.Keyframe[] f = this.frames;
         if (!(time <= f[0].t) && f.length != 1) {
            for (int i = 1; i < f.length; i++) {
               if (time < f[i].t) {
                  PlayerAnimation.Keyframe a = f[i - 1];
                  PlayerAnimation.Keyframe b = f[i];
                  float p = (time - a.t) / Math.max(1.0E-4F, b.t - a.t);
                  float e = b.ease.apply(Mth.clamp(p, 0.0F, 1.0F));

                  for (int k = 0; k < 3; k++) {
                     rotOut[k] = Mth.lerp(e, a.rot[k], b.rot[k]);
                     posOut[k] = Mth.lerp(e, a.pos[k], b.pos[k]);
                  }

                  return;
               }
            }

            copy(f[f.length - 1], rotOut, posOut);
         } else {
            copy(f[0], rotOut, posOut);
         }
      }

      private static void copy(PlayerAnimation.Keyframe k, float[] rotOut, float[] posOut) {
         System.arraycopy(k.rot, 0, rotOut, 0, 3);
         System.arraycopy(k.pos, 0, posOut, 0, 3);
      }
   }

   public static record Keyframe(float t, float[] rot, float[] pos, Easing ease) {
   }

   public static enum Part {
      HEAD,
      BODY,
      RIGHT_ARM,
      LEFT_ARM,
      RIGHT_LEG,
      LEFT_LEG;

      public static PlayerAnimation.Part parse(String name) {
         return valueOf(name.toUpperCase(Locale.ROOT));
      }
   }
}
