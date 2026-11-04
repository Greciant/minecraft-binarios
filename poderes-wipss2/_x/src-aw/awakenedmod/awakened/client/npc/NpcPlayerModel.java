package com.awakenedmod.awakened.client.npc;

import com.awakenedmod.awakened.core.npc.PoweredNpc;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

public final class NpcPlayerModel extends PlayerModel<PoweredNpc> {
   private final NpcKitVisuals visuals;

   public NpcPlayerModel(ModelPart root, NpcKitVisuals visuals) {
      super(root, false);
      this.visuals = visuals;
   }

   public void setupAnim(PoweredNpc npc, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
      super.setupAnim(npc, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
      NpcPose.apply(NpcPose.frame(npc, this.visuals, ageInTicks - (float)npc.tickCount), this, this.visuals);
      this.hat.copyFrom(this.head);
      this.jacket.copyFrom(this.body);
      this.leftSleeve.copyFrom(this.leftArm);
      this.rightSleeve.copyFrom(this.rightArm);
      this.leftPants.copyFrom(this.leftLeg);
      this.rightPants.copyFrom(this.rightLeg);
   }
}
