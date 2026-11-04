package com.awakenedmod.awakened.client.npc;

import com.awakenedmod.awakened.client.AwakenedAnimations;
import com.awakenedmod.awakened.client.ClipBearers;
import com.awakenedmod.awakened.core.npc.PoweredNpc;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zigythebird.playeranimcore.animation.layered.modifier.AdjustmentModifier.PartModifier;
import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;

public final class NpcPose {
   static final int MIN_FADE_IN = 2;
   static final int MIN_FADE_OUT = 4;
   private static final float HIP = 0.75F;
   private static final NpcClipLibrary.Bone[] PARTS = new NpcClipLibrary.Bone[]{
      NpcClipLibrary.Bone.HEAD,
      NpcClipLibrary.Bone.TORSO,
      NpcClipLibrary.Bone.RIGHT_ARM,
      NpcClipLibrary.Bone.LEFT_ARM,
      NpcClipLibrary.Bone.RIGHT_LEG,
      NpcClipLibrary.Bone.LEFT_LEG
   };
   private static final Int2ObjectMap<NpcPose.Memory> MEMORY = new Int2ObjectOpenHashMap();

   private NpcPose() {
   }

   public static NpcPoseFrame frame(PoweredNpc npc, NpcKitVisuals visuals, float partialTick) {
      NpcPose.Memory m = (NpcPose.Memory)MEMORY.computeIfAbsent(npc.getId(), id -> new NpcPose.Memory());
      float age = (float)npc.tickCount + partialTick;
      if (m.frame != null && Math.abs(m.frameAge - age) < 0.001F) {
         return m.frame;
      } else {
         easeMood(m, npc, visuals);
         ResourceLocation clip = npc.activeClip();
         float clipAge = npc.clipAge(partialTick);
         NpcClipLibrary.Clip c = NpcClipLibrary.get(clip);
         if (c == null || !c.loops() && clipAge / 20.0F > c.length()) {
            clip = null;
            c = null;
         }

         int seq = npc.clipSequence();
         if (!m.seen) {
            m.seen = true;
            m.clip = clip;
            m.seq = seq;
         } else if (!Objects.equals(clip, m.clip) || clip != null && seq != m.seq) {
            startBlend(m, clip, age);
            m.seq = seq;
         }

         float[] root = new float[6];
         resolveRoot(m, npc, c, clipAge / 20.0F, partialTick, age, root);
         m.frameAge = age;
         m.frame = new NpcPoseFrame(clip, clipAge, age, partialTick, m.mood, npc.getId(), root);
         return m.frame;
      }
   }

   private static void startBlend(NpcPose.Memory m, @Nullable ResourceLocation clip, float age) {
      System.arraycopy(m.lastRoot, 0, m.fromRoot, 0, 6);

      for (int i = 0; i < PARTS.length; i++) {
         System.arraycopy(m.lastPart[i], 0, m.fromPart[i], 0, 3);
         m.fromOver[i] = m.lastOver[i];
      }

      if (clip != null) {
         m.fadeTicks = Math.max(2, AwakenedAnimations.transitionInTicks(clip));
         m.fadeEase = NpcEasing.of(AwakenedAnimations.transitionInEase(clip, null), "easeOutQuad");
      } else {
         m.fadeTicks = Math.max(4, AwakenedAnimations.transitionOutTicks(m.clip));
         m.fadeEase = NpcEasing.of(AwakenedAnimations.transitionOutEase(m.clip, null), "easeInOutSine");
      }

      m.fadeStart = age;
      m.clip = clip;
   }

   private static void easeMood(NpcPose.Memory m, LivingEntity npc, NpcKitVisuals visuals) {
      if (m.moodTick != npc.tickCount) {
         float target = visuals.moodTarget(npc);
         if (m.moodTick == Integer.MIN_VALUE) {
            m.mood = target;
         } else {
            int steps = Mth.clamp(npc.tickCount - m.moodTick, 1, 20);

            for (int i = 0; i < steps; i++) {
               m.mood = m.mood + (target - m.mood) * visuals.moodEase();
            }

            if (target == 0.0F && m.mood < 0.005F) {
               m.mood = 0.0F;
            }
         }

         m.moodTick = npc.tickCount;
      }
   }

   private static void resolveRoot(
      NpcPose.Memory m, LivingEntity npc, @Nullable NpcClipLibrary.Clip c, float seconds, float partialTick, float age, float[] out
   ) {
      if (c != null && c.drives(NpcClipLibrary.Bone.BODY)) {
         float[] tmp = new float[3];
         c.rotation(NpcClipLibrary.Bone.BODY, seconds, tmp);
         System.arraycopy(tmp, 0, out, 0, 3);
         c.position(NpcClipLibrary.Bone.BODY, seconds, tmp);
         System.arraycopy(tmp, 0, out, 3, 3);
      }

      PartModifier mod = AwakenedAnimations.thirdPersonModifier(NpcClipLibrary.Bone.BODY.libName, npc, partialTick);
      if (mod != null) {
         out[0] += mod.rotation().x() * (180.0F / (float)Math.PI);
         out[1] += mod.rotation().y() * (180.0F / (float)Math.PI);
         out[2] += mod.rotation().z() * (180.0F / (float)Math.PI);
         out[3] += mod.offset().x();
         out[4] += mod.offset().y();
         out[5] += mod.offset().z();
      }

      float w = m.fade(age);
      if (w < 1.0F) {
         for (int i = 0; i < 6; i++) {
            out[i] = Mth.lerp(w, m.fromRoot[i], out[i]);
         }
      }

      System.arraycopy(out, 0, m.lastRoot, 0, 6);
   }

   public static void applyRoot(PoseStack poseStack, @Nullable NpcPoseFrame frame) {
      if (frame != null) {
         float[] r = frame.root();
         if (r[0] != 0.0F || r[1] != 0.0F || r[2] != 0.0F || r[3] != 0.0F || r[4] != 0.0F || r[5] != 0.0F) {
            poseStack.translate(-r[3] / 16.0F, r[4] / 16.0F + 0.75F, r[5] / 16.0F);
            poseStack.mulPose(
               new Quaternionf().rotateZYX(r[2] * (float) (Math.PI / 180.0), -r[1] * (float) (Math.PI / 180.0), -r[0] * (float) (Math.PI / 180.0))
            );
            poseStack.translate(0.0F, -0.75F, 0.0F);
         }
      }
   }

   public static void apply(NpcPoseFrame frame, HumanoidModel<?> model, NpcKitVisuals visuals) {
      visuals.underlay(frame, model);
      NpcPose.Memory m = (NpcPose.Memory)MEMORY.get(frame.entityId());
      if (m != null) {
         NpcClipLibrary.Clip c = NpcClipLibrary.get(frame.clip());
         float seconds = frame.clipAge() / 20.0F;
         float w = m.fade(frame.ageInTicks());
         LivingEntity entity = living(frame.entityId());
         float[] key = new float[3];

         for (int i = 0; i < PARTS.length; i++) {
            ModelPart part = part(model, PARTS[i]);
            boolean over = false;
            float x = part.xRot;
            float y = part.yRot;
            float z = part.zRot;
            if (c != null && c.drives(PARTS[i])) {
               c.rotation(PARTS[i], seconds, key);
               x = key[0] * (float) (Math.PI / 180.0);
               y = key[1] * (float) (Math.PI / 180.0);
               z = key[2] * (float) (Math.PI / 180.0);
               over = true;
            }

            PartModifier mod = entity == null ? null : AwakenedAnimations.thirdPersonModifier(PARTS[i].libName, entity, frame.partialTick());
            if (mod != null) {
               x += mod.rotation().x();
               y += mod.rotation().y();
               z += mod.rotation().z();
               over = true;
            }

            if (w < 1.0F && (over || m.fromOver[i])) {
               float fx = m.fromOver[i] ? m.fromPart[i][0] : part.xRot;
               float fy = m.fromOver[i] ? m.fromPart[i][1] : part.yRot;
               float fz = m.fromOver[i] ? m.fromPart[i][2] : part.zRot;
               x = Mth.lerp(w, fx, x);
               y = Mth.lerp(w, fy, y);
               z = Mth.lerp(w, fz, z);
               over = true;
            }

            part.xRot = x;
            part.yRot = y;
            part.zRot = z;
            m.lastPart[i][0] = x;
            m.lastPart[i][1] = y;
            m.lastPart[i][2] = z;
            m.lastOver[i] = over;
         }
      }
   }

   @Nullable
   private static LivingEntity living(int id) {
      Minecraft mc = Minecraft.getInstance();
      return (mc.level == null ? null : mc.level.getEntity(id)) instanceof LivingEntity living ? living : null;
   }

   public static void add(ModelPart part, float xDeg, float yDeg, float zDeg) {
      part.xRot += xDeg * (float) (Math.PI / 180.0);
      part.yRot += yDeg * (float) (Math.PI / 180.0);
      part.zRot += zDeg * (float) (Math.PI / 180.0);
   }

   private static ModelPart part(HumanoidModel<?> model, NpcClipLibrary.Bone bone) {
      return switch (bone) {
         case HEAD -> model.head;
         case TORSO, BODY -> model.body;
         case RIGHT_ARM -> model.rightArm;
         case LEFT_ARM -> model.leftArm;
         case RIGHT_LEG -> model.rightLeg;
         case LEFT_LEG -> model.leftLeg;
      };
   }

   static {
      ClipBearers.onForget(MEMORY::remove);
   }

   private static final class Memory {
      boolean seen;
      @Nullable
      ResourceLocation clip;
      int seq;
      float fadeStart;
      int fadeTicks;
      Float2FloatFunction fadeEase;
      final float[] fromRoot = new float[6];
      final float[] lastRoot = new float[6];
      final float[][] fromPart = new float[NpcPose.PARTS.length][3];
      final boolean[] fromOver = new boolean[NpcPose.PARTS.length];
      final float[][] lastPart = new float[NpcPose.PARTS.length][3];
      final boolean[] lastOver = new boolean[NpcPose.PARTS.length];
      float mood;
      int moodTick = Integer.MIN_VALUE;
      float frameAge = Float.NaN;
      @Nullable
      NpcPoseFrame frame;

      float fade(float age) {
         if (this.fadeTicks <= 0) {
            return 1.0F;
         } else {
            float t = Mth.clamp((age - this.fadeStart) / (float)this.fadeTicks, 0.0F, 1.0F);
            return t >= 1.0F ? 1.0F : this.fadeEase.get(t);
         }
      }
   }
}
