package com.devilfruits.core.body;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;

public interface BodyShell {
   @Nullable
   UUID bodyOwner();

   @Nullable
   Entity bodyOwnerEntity();
}
