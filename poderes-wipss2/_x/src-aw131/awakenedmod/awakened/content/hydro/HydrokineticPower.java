package com.awakenedmod.awakened.content.hydro;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.QuietEffect;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.CraftRecipe;
import com.awakenedmod.awakened.core.power.Power;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public final class HydrokineticPower extends Power {
   private static final int REFILL_INTERVAL = 30;
   private static final float REFILL_AMOUNT = 0.5F;
   private static final int WET_CHECK_INTERVAL = 4;
   private static final int WET_BAR_COLOR = -9449217;
   private static final QuietEffect DOLPHINS_GRACE = new QuietEffect(MobEffects.DOLPHINS_GRACE, 40, 20);

   public HydrokineticPower() {
      super("hydrokinetic", List.of(new WaterLanceAbility(), new GeyserAbility(), new WaveRiderAbility(), new TideHookAbility(), new TsunamiAbility()));
   }

   @Override
   public int themeColor() {
      return -13726209;
   }

   @Override
   public int signatureSlot() {
      return 0;
   }

   @Override
   public List<CraftRecipe> recipes() {
      return List.of(
         CraftRecipe.shaped((ItemLike)AwakenedItems.WATER_FLASK.get(), List.of("L L", "L L", " L "), Map.of('L', Items.LEATHER)),
         CraftRecipe.shaped((ItemLike)AwakenedItems.WATER_CANTEEN.get(), List.of(" I ", "I I", " I "), Map.of('I', Items.IRON_INGOT)),
         CraftRecipe.shaped((ItemLike)AwakenedItems.WATER_RESERVOIR.get(), List.of("GDG", "G G", " G "), Map.of('G', Items.GOLD_INGOT, 'D', Items.DIAMOND)),
         CraftRecipe.shaped(
            (ItemLike)AwakenedItems.TIDAL_VESSEL.get(),
            List.of(" N ", "PRP", " P "),
            Map.of('N', Items.NETHERITE_INGOT, 'R', (ItemLike)AwakenedItems.WATER_RESERVOIR.get(), 'P', Items.PRISMARINE_SHARD)
         ),
         CraftRecipe.shaped(
            (ItemLike)AwakenedItems.RESERVOIR_CORE.get(),
            List.of(" N ", " V ", " P "),
            Map.of('N', Items.NETHERITE_BLOCK, 'V', (ItemLike)AwakenedItems.TIDAL_VESSEL.get(), 'P', Items.PRISMARINE)
         )
      );
   }

   @Override
   public void passiveTick(ServerPlayer player, PowerData data) {
      super.passiveTick(player, data);
      if (player.getAirSupply() < player.getMaxAirSupply()) {
         player.setAirSupply(player.getMaxAirSupply());
      }

      if (player.isInWater()) {
         DOLPHINS_GRACE.keep(player);
      }

      if (player.tickCount % 4 == 0) {
         HydroVisual.setWet(player, Hydro.resolve(player).wet());
      }

      if (player.tickCount % 30 == 0 && Hydro.resolve(player).free()) {
         VesselItem.passiveRefill(player, 0.5F);
      }
   }

   @Override
   protected void onAbilityStateCleared(ServerPlayer player) {
      HydroVisual.clear(player);
   }

   @Override
   public void onSuppressed(ServerPlayer player, PowerData data) {
      DOLPHINS_GRACE.clear(player);
   }

   @Override
   public void onReplaced(ServerPlayer player) {
      DOLPHINS_GRACE.clear(player);
   }

   @Override
   public float modifyBreakSpeed(Player player, float speed) {
      if (!player.isInWater()) {
         return speed;
      } else {
         float restored = speed;
         if (player.isEyeInFluid(FluidTags.WATER)) {
            double submerged = player.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED);
            if (submerged > 0.0 && submerged < 1.0) {
               restored = speed / (float)submerged;
            }
         }

         if (!player.onGround()) {
            restored *= 5.0F;
         }

         return restored;
      }
   }

   @Override
   public boolean hasResourceBar() {
      return true;
   }

   @Override
   public int resourceBarColor() {
      return -13726209;
   }

   @Override
   public int resourceBarColor(@Nullable Player viewer, float fraction) {
      return viewer != null && HydroVisual.of(viewer).wet() ? -9449217 : this.resourceBarColor();
   }

   @Override
   public Component resourceBarBadge(Player viewer, float fraction) {
      return HydroVisual.of(viewer).wet() ? Component.translatable("awakened.hydrokinetic.wet_badge", new Object[]{Math.round(25.0F)}) : null;
   }

   @Override
   public Component resourceBarLabel() {
      return Component.translatable("awakened.hydrokinetic.water");
   }

   @Override
   public float resourceBarFraction(ServerPlayer player, PowerData data) {
      ItemStack best = VesselItem.best(player);
      if (best == null) {
         return 0.0F;
      } else {
         int cap = VesselItem.capacityOf(best);
         return cap <= 0 ? 0.0F : Math.min(1.0F, VesselItem.charges(best) / (float)cap);
      }
   }

   @Override
   public int resourceBarTier(ServerPlayer player, PowerData data) {
      ItemStack best = VesselItem.best(player);
      return best == null ? 0 : VesselItem.tierOf(best);
   }

   @Override
   public void onAwaken(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Fx.burst(level, ParticleTypes.SPLASH, player.position().add(0.0, 1.0, 0.0), 60, 0.8, 0.2);
      Fx.burst(level, ParticleTypes.BUBBLE, player.position().add(0.0, 1.0, 0.0), 40, 0.7, 0.15);
      Fx.sound(level, player.position(), SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 1.2F, 0.7F);
      player.displayClientMessage(Component.translatable("awakened.hydrokinetic.hint").withStyle(ChatFormatting.AQUA), false);
   }
}
