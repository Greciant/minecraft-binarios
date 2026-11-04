package com.awakenedmod.awakened.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

public final class FallGuard {
   private final ResourceLocation id;
   private final int graceTicks;
   private final int maxTicks;
   private final Map<UUID, int[]> guards = new HashMap<>();

   public FallGuard(ResourceLocation id, int graceTicks, int maxTicks) {
      this.id = id;
      this.graceTicks = graceTicks;
      this.maxTicks = maxTicks;
   }

   public void arm(ServerPlayer player) {
      int[] g = this.guards.computeIfAbsent(player.getUUID(), k -> new int[2]);
      g[0] = 0;
      g[1] = 0;
      Attrs.set(player, Attributes.FALL_DAMAGE_MULTIPLIER, this.id, -1.0, Operation.ADD_MULTIPLIED_TOTAL);
   }

   public boolean armed(ServerPlayer player) {
      return this.guards.containsKey(player.getUUID());
   }

   public void tick(ServerPlayer player, boolean holding) {
      int[] g = this.guards.get(player.getUUID());
      if (g != null && !holding) {
         g[0]++;
         g[1] = !player.onGround() && !player.isInWater() ? 0 : g[1] + 1;
         if (g[1] >= this.graceTicks || g[0] >= this.maxTicks) {
            this.clear(player);
         }
      }
   }

   public void clear(ServerPlayer player) {
      this.guards.remove(player.getUUID());
      Attrs.set(player, Attributes.FALL_DAMAGE_MULTIPLIER, this.id, 0.0, Operation.ADD_VALUE);
   }
}
