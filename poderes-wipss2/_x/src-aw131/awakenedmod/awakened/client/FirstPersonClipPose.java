package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.client.npc.NpcClipLibrary;
import com.zigythebird.playeranimcore.animation.layered.modifier.AdjustmentModifier.PartModifier;
import com.zigythebird.playeranimcore.math.Vec3f;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public final class FirstPersonClipPose {
   private static final Map<ResourceLocation, FirstPersonClipPose.Track> TRACKS = new LinkedHashMap<>();
   private static final float[] FP = new float[3];
   private static final float[] AUTHORED = new float[3];
   private static final float[] FP_POS = new float[3];
   private static final float[] AUTHORED_POS = new float[3];

   private FirstPersonClipPose() {
   }

   public static void register() {
      AwakenedAnimations.addPoseModifier(FirstPersonClipPose::bone);
   }

   public static void add(ResourceLocation clip, ResourceLocation firstPersonTrack) {
      add(clip, firstPersonTrack, 0.0F, 0.0F);
   }

   public static void add(ResourceLocation clip, ResourceLocation firstPersonTrack, float extraFollowDown, float extraFollowUp) {
      TRACKS.put(clip, new FirstPersonClipPose.Track(firstPersonTrack, extraFollowDown, extraFollowUp));
   }

   private static Optional<PartModifier> bone(String bone, Entity avatar, boolean firstPerson, float partialTick) {
      if (firstPerson && !TRACKS.isEmpty()) {
         NpcClipLibrary.Bone b = NpcClipLibrary.Bone.of(bone);
         if (b == null) {
            return Optional.empty();
         } else {
            for (Entry<ResourceLocation, FirstPersonClipPose.Track> e : TRACKS.entrySet()) {
               if (AwakenedAnimations.isPlaying(avatar.getId(), e.getKey())) {
                  NpcClipLibrary.Clip fp = NpcClipLibrary.get(e.getValue().id());
                  NpcClipLibrary.Clip authored = NpcClipLibrary.get(e.getKey());
                  float t = AwakenedAnimations.clipSeconds(avatar.getId());
                  if (fp != null && authored != null && !(t < 0.0F) && fp.drives(b)) {
                     fp.rotation(b, t, FP);
                     authored.rotation(b, t, AUTHORED);
                     fp.position(b, t, FP_POS);
                     authored.position(b, t, AUTHORED_POS);
                     float follow = 0.0F;
                     if (b == NpcClipLibrary.Bone.RIGHT_ARM || b == NpcClipLibrary.Bone.LEFT_ARM) {
                        float look = avatar.getViewXRot(partialTick);
                        follow = look * (look > 0.0F ? e.getValue().extraFollowDown() : e.getValue().extraFollowUp());
                     }

                     return Optional.of(
                        new PartModifier(
                           new Vec3f(
                              (float)Math.toRadians((double)(FP[0] - AUTHORED[0] + follow)),
                              (float)Math.toRadians((double)(FP[1] - AUTHORED[1])),
                              (float)Math.toRadians((double)(FP[2] - AUTHORED[2]))
                           ),
                           new Vec3f(FP_POS[0] - AUTHORED_POS[0], FP_POS[1] - AUTHORED_POS[1], FP_POS[2] - AUTHORED_POS[2])
                        )
                     );
                  }

                  return Optional.empty();
               }
            }

            return Optional.empty();
         }
      } else {
         return Optional.empty();
      }
   }

   private static record Track(ResourceLocation id, float extraFollowDown, float extraFollowUp) {
   }
}
