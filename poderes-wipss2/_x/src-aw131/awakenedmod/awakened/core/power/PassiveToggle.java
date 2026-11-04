package com.awakenedmod.awakened.core.power;

import com.awakenedmod.awakened.client.PassiveToggleClient;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ItemLike;

public record PassiveToggle(String key, String nameKey, String descKey, boolean defaultOn) {
   private static final String VAR_PREFIX = "ptoggle_";

   public PassiveToggle.Wedge wedge(ItemLike icon) {
      return new PassiveToggle.Wedge(this, icon, null);
   }

   public PassiveToggle.Wedge wedge(ResourceLocation texture) {
      return new PassiveToggle.Wedge(this, null, texture);
   }

   public String var() {
      return varOf(this.key);
   }

   public static String varOf(String key) {
      return "ptoggle_" + key;
   }

   public boolean enabled(PowerData data) {
      return data.var(this.var(), this.defaultOn ? 1.0F : 0.0F) > 0.5F;
   }

   public boolean enabled(Player holder) {
      if (holder instanceof ServerPlayer server) {
         return this.enabled(PowerService.data(server));
      } else {
         return holder.level().isClientSide() ? PassiveToggleClient.enabled(this) : this.defaultOn;
      }
   }

   public static record Wedge(PassiveToggle toggle, @Nullable ItemLike item, @Nullable ResourceLocation texture) {
   }
}
