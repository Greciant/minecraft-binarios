package com.awakenedmod.awakened.client.render.limitless.sixeyes;

import com.awakenedmod.awakened.client.render.HeadAnchor;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;

public interface SixEyesCue {
   List<LivingEntity> seen();

   default float presence(LivingEntity body) {
      return 1.0F;
   }

   @Nullable
   HeadAnchor.Head head();

   long seed();
}
