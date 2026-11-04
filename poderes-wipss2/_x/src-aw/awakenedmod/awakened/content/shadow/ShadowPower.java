package com.awakenedmod.awakened.content.shadow;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.PassiveToggle;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.Vec3;

public final class ShadowPower extends Power {
   private static final float MELEE_DARK_BONUS = 4.0F;
   private static final float[] TIER_MELEE_BONUS = new float[]{0.0F, 0.0F, 3.0F, 6.0F, 10.0F};
   private static boolean applyingMelee = false;
   private final ShadowMeldAbility meld;

   public ShadowPower() {
      this(new ShadowMeldAbility());
   }

   private ShadowPower(ShadowMeldAbility meld) {
      super("shadow", List.of(new ShadowBladeAbility(), new ShadowSqueezeAbility(), meld, new ShadowOrbAbility(), new DevourAbility()));
      this.meld = meld;
   }

   @Override
   public int themeColor() {
      return -7712769;
   }

   @Override
   public List<String> extraBalanceKeys() {
      return List.of("ShadowMeleeBonus");
   }

   @Override
   public int signatureSlot() {
      return 0;
   }

   @Override
   public void passiveTick(ServerPlayer player, PowerData data) {
      super.passiveTick(player, data);
      ShadowSight.tick(player, data);
      MinecraftServer server = player.serverLevel().getServer();
      if (server != null) {
         ShadowZones.tickAll(server);
      }

      if (player.tickCount % 5 == 0) {
         updateBladeStage(player.getMainHandItem(), player);
         updateBladeStage(player.getOffhandItem(), player);
      }
   }

   private static void updateBladeStage(ItemStack stack, ServerPlayer player) {
      if (stack.is((Item)AwakenedItems.SHADOW_BLADE.get())) {
         ShadowBladeItem.setTier(stack, ShadowSlash.tier(player));
      }
   }

   @Override
   public List<PassiveToggle> passiveToggles() {
      return List.of(ShadowSight.TOGGLE);
   }

   @Override
   public void onSuppressed(ServerPlayer player, PowerData data) {
      ShadowSight.clear(player);
   }

   @Override
   public void onReplaced(ServerPlayer player) {
      ShadowSight.clear(player);
   }

   @Override
   public void onDealDamage(ServerPlayer attacker, LivingEntity victim) {
      if (!applyingMelee) {
         float bonus = ShadowDark.scale(attacker, 0.0F, 4.0F);
         if (attacker.getMainHandItem().is((Item)AwakenedItems.SHADOW_BLADE.get())) {
            bonus += TIER_MELEE_BONUS[ShadowSlash.tier(attacker)];
         }

         if (!(bonus <= 0.05F)) {
            applyingMelee = true;

            try {
               Dmg.hurt(
                  victim, Dmg.serverLevel(attacker), Dmg.by(attacker, DamageTypes.MAGIC), MasteryScaling.scaledDamage(bonus, attacker, "ShadowMeleeBonus")
               );
            } finally {
               applyingMelee = false;
            }
         }
      }
   }

   @Override
   public void onKill(ServerPlayer killer, LivingEntity victim) {
      if (this.meld.leftShadowRecently(killer) && Foes.defeatedBy(victim, killer)) {
         QuestSystem.feat(killer, "shadow_ambush");
      }
   }

   @Override
   public void onAwaken(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Vec3 center = player.position().add(0.0, 1.0, 0.0);
      ShadowFx.awaken(level, center, player.position().add(0.0, 0.1, 0.0));
      if (ShadowBladeItem.powerExists() && !hasShadowBlade(player)) {
         ItemStack blade = ShadowBladeItem.stack();
         if (!player.getInventory().add(blade)) {
            player.drop(blade, false);
         }
      }
   }

   private static boolean hasShadowBlade(ServerPlayer player) {
      return player.getInventory().contains(new ItemStack((ItemLike)AwakenedItems.SHADOW_BLADE.get()));
   }
}
