package com.awakenedmod.awakened.client.npc;

import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

public record NpcPoseFrame(@Nullable ResourceLocation clip, float clipAge, float ageInTicks, float partialTick, float mood, int entityId, float[] root) {
}
