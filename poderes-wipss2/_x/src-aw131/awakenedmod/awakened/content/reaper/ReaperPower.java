package com.awakenedmod.awakened.content.reaper;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.net.AwakenScreenFxPayload;
import com.awakenedmod.awakened.core.power.CraftRecipe;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerProgress;
import com.awakenedmod.awakened.core.progression.BalanceConfig;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ReaperPower extends Power {
   public static final String ID = "reaper";
   private static final int THEME = -10505002;

   public ReaperPower() {
      super("reaper", List.of(new SoulBoltAbility(), new SpectralDriftAbility(), new SoulTetherAbility(), new WailingSoulsAbility(), new DeathMarkAbility()));
   }

   @Override
   public int themeColor() {
      return -10505002;
   }

   @Override
   public List<String> extraBalanceKeys() {
      return List.of(ReaperAura.class.getSimpleName());
   }

   @Override
   public int signatureSlot() {
      return 0;
   }

   @Override
   public void passiveTick(ServerPlayer player, PowerData data) {
      super.passiveTick(player, data);
      ReaperState.update(player, data);
      ReaperAura.tick(player, data);
      HauntControl.tickAll(player.serverLevel().getServer());
      DeathMarkControl.tickAll(player.serverLevel().getServer());
   }

   @Override
   public void onKill(ServerPlayer killer, LivingEntity victim) {
      ReaperSouls.onKill(killer, victim);
   }

   @Override
   public void onSuppressed(ServerPlayer player, PowerData data) {
      DeathMarkControl.clearAllBy(player.getUUID());
   }

   @Override
   public void onDeath(ServerPlayer victim, DamageSource source) {
      ReaperSouls.onPlayerDeath(victim, source);
   }

   @Override
   public float onIncomingDamage(ServerPlayer player, DamageSource source, float amount) {
      long souls = ReaperAttachments.souls(player).souls();
      return super.onIncomingDamage(player, source, amount) * ReaperScaling.damageTaken(souls);
   }

   @Override
   public float bonusMaxStamina(Player player) {
      if (player instanceof ServerPlayer sp) {
         long souls = ReaperAttachments.souls(sp).souls();
         float bonus = (ReaperScaling.staminaMult(souls) - 1.0F) * StaminaSystem.baseMax();
         return Math.max(bonus, -StaminaSystem.baseMax() * 0.9F);
      } else {
         return 0.0F;
      }
   }

   @Override
   public float bonusStaminaRegen(Player player) {
      if (player instanceof ServerPlayer sp) {
         long souls = ReaperAttachments.souls(sp).souls();
         float baseRegen = (float)BalanceConfig.regenPerSecond();
         return (ReaperScaling.staminaMult(souls) - 1.0F) * baseRegen * 0.5F;
      } else {
         return 0.0F;
      }
   }

   @Override
   public boolean progressOnCard() {
      return true;
   }

   @Override
   public PowerProgress progress(Player player, int masteryLevel) {
      long souls = ReaperAttachments.souls(player).souls();
      int idx = ReaperScaling.milestoneIndex(souls);
      Component tier = Component.translatable(ReaperScaling.milestoneTitleKey(idx));
      boolean capped = souls >= 5000L;
      float frac = capped ? 1.0F : ReaperScaling.progressToNext(souls);
      Component right = capped
         ? Component.translatable("awakened.reaper.progress.max", new Object[]{souls})
         : Component.literal(souls + " / " + ReaperScaling.nextThreshold(souls));
      int hearts = (int)Math.round(ReaperScaling.maxHealthHp(souls) / 2.0);
      List<PowerProgress.Stat> stats = List.of(
         new PowerProgress.Stat(Component.translatable("awakened.reaper.stat.damage_l"), Component.literal(fmt(ReaperScaling.offense(souls)) + "x")),
         new PowerProgress.Stat(
            Component.translatable("awakened.reaper.stat.health_l"), Component.translatable("awakened.reaper.stat.health_v", new Object[]{hearts})
         ),
         new PowerProgress.Stat(
            Component.translatable("awakened.reaper.stat.dmg_taken_l"), Component.literal(fmt((double)ReaperScaling.damageTaken(souls)) + "x")
         ),
         new PowerProgress.Stat(
            Component.translatable("awakened.reaper.stat.stamina_l"), Component.literal(fmt((double)ReaperScaling.staminaMult(souls)) + "x")
         )
      );
      return new PowerProgress(frac, tier, right, stats);
   }

   private static String fmt(double v) {
      return String.format(Locale.ROOT, "%.1f", v);
   }

   @Override
   public List<CraftRecipe> recipes() {
      return List.of(
         CraftRecipe.shapeless((ItemLike)AwakenedItems.SCYTHE_EDGE.get(), List.of(Items.NETHERITE_INGOT)),
         CraftRecipe.shaped(
            (ItemLike)AwakenedItems.SOUL_VESSEL.get(), List.of("GIG", "GBG", " G "), Map.of('G', Items.GLASS, 'I', Items.IRON_INGOT, 'B', Items.BONE)
         ),
         CraftRecipe.shaped(
            (ItemLike)AwakenedItems.REAPER_SCYTHE.get(),
            List.of("EBS", " CB", "B  "),
            Map.of(
               'E', (ItemLike)AwakenedItems.SCYTHE_EDGE.get(), 'B', Items.BONE, 'S', Items.WITHER_SKELETON_SKULL, 'C', (ItemLike)AwakenedItems.SOUL_CORE.get()
            )
         )
      );
   }

   @Override
   public void onAwaken(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Vec3 center = player.position().add(0.0, 1.0, 0.0);
      ReaperFx.awaken(level, center, player.position().add(0.0, 0.1, 0.0));
      PacketDistributor.sendToPlayer(player, new AwakenScreenFxPayload(6277344, 30), new CustomPacketPayload[0]);
   }
}
