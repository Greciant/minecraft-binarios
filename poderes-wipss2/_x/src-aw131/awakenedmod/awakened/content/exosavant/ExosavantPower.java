package com.awakenedmod.awakened.content.exosavant;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.core.AbilityGate;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.ExoVisual;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.CraftRecipe;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerProgress;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.Vec3;

public final class ExosavantPower extends Power {
   public static final String ID = "exosavant";
   private static final double PUNCH_KNOCKBACK = 0.5;
   private static final float TANK_THRESHOLD = 6.0F;

   public ExosavantPower() {
      super("exosavant", List.of(new RocketAbility(), new RocketChargeAbility(), new ChestBeamAbility(), new ScanPulseAbility(), new ArtilleryBarrageAbility()));
      AbilityGate.register(ExoSuit::isAssembling);
   }

   @Override
   public int themeColor() {
      return -3610881;
   }

   @Override
   public int signatureSlot() {
      return 0;
   }

   @Override
   public boolean hasSkins() {
      return true;
   }

   @Override
   public PowerProgress progress(Player player, int masteryLevel) {
      ExoVisual visual = (ExoVisual)player.getData(AwakenedAttachments.EXO_VISUAL);
      int builtMask = visual.builtMask();
      boolean chipInstalled = visual.chipInstalled();
      List<PowerProgress.Stat> cells = new ArrayList<>();
      ExoPiece next = null;
      int built = 0;

      for (ExoPiece piece : ExoPiece.values()) {
         boolean isBuilt = piece.countsAsSuit() ? (builtMask & piece.bit()) != 0 : chipInstalled;
         boolean unlocked = masteryLevel >= piece.threshold();
         if (isBuilt) {
            built++;
         } else if (next == null) {
            next = piece;
         }

         cells.add(new PowerProgress.Stat(piece.shortName(), cellStatus(isBuilt, unlocked, piece.threshold())));
      }

      Component barLeft;
      Component barRight;
      float fraction;
      if (next == null) {
         barLeft = Component.translatable("awakened.exosavant.progress.complete");
         barRight = Component.literal(built + " / " + ExoPiece.values().length);
         fraction = 1.0F;
      } else {
         barLeft = Component.translatable("awakened.exosavant.progress.next", new Object[]{next.shortName()});
         if (masteryLevel >= next.threshold()) {
            barRight = Component.translatable("awakened.exosavant.progress.ready");
            fraction = 1.0F;
         } else {
            barRight = Component.translatable("awakened.exosavant.progress.to_go", new Object[]{next.threshold(), next.threshold() - masteryLevel});
            fraction = gapFraction(masteryLevel, next);
         }
      }

      return new PowerProgress(fraction, barLeft, barRight, cells);
   }

   private static Component cellStatus(boolean built, boolean unlocked, int threshold) {
      if (built) {
         return Component.literal("✔");
      } else {
         return unlocked
            ? Component.translatable("awakened.exosavant.progress.cell_ready")
            : Component.translatable("awakened.exosavant.progress.cell_locked", new Object[]{threshold});
      }
   }

   private static float gapFraction(int masteryLevel, ExoPiece next) {
      int prev = next.ordinal() > 0 ? ExoPiece.values()[next.ordinal() - 1].threshold() : 0;
      int span = next.threshold() - prev;
      return span <= 0 ? 1.0F : Math.max(0.0F, Math.min(1.0F, (float)(masteryLevel - prev) / (float)span));
   }

   @Override
   public List<CraftRecipe> recipes() {
      return suitRecipes();
   }

   public static List<CraftRecipe> suitRecipes() {
      return List.of(
         CraftRecipe.shaped(
               (ItemLike)AwakenedItems.EXOSAVANT_GAUNTLETS.get(),
               List.of("G G", "R R", "I I"),
               Map.of('G', Items.GUNPOWDER, 'R', Items.REDSTONE, 'I', Items.IRON_BLOCK)
            )
            .withUnlock(ExoPiece.GAUNTLETS.threshold()),
         CraftRecipe.shaped(
               (ItemLike)AwakenedItems.EXOSAVANT_BOOTS.get(),
               List.of("D D", "CIC", "GIG"),
               Map.of('D', Items.DIAMOND, 'C', Items.COPPER_BLOCK, 'I', Items.IRON_BLOCK, 'G', Items.GUNPOWDER)
            )
            .withUnlock(ExoPiece.BOOTS.threshold()),
         CraftRecipe.shaped(
               (ItemLike)AwakenedItems.EXOSAVANT_CHESTPLATE.get(),
               List.of("LBL", "RXR", "LBL"),
               Map.of('L', Items.LAPIS_LAZULI, 'B', Items.BLUE_STAINED_GLASS, 'R', Items.REDSTONE, 'X', Items.DIAMOND_BLOCK)
            )
            .withUnlock(ExoPiece.CHESTPLATE.threshold()),
         CraftRecipe.shaped(
               (ItemLike)AwakenedItems.EXOSAVANT_LEGGINGS.get(),
               List.of("PAP", "AXA", "CPC"),
               Map.of('P', Items.BLAZE_POWDER, 'A', Items.AMETHYST_SHARD, 'X', Items.DIAMOND_BLOCK, 'C', Items.COPPER_INGOT)
            )
            .withUnlock(ExoPiece.LEGGINGS.threshold()),
         CraftRecipe.shaped(
               (ItemLike)AwakenedItems.EXOSAVANT_HELMET.get(),
               List.of("ABA", "OSN", "R R"),
               Map.of(
                  'A',
                  Items.AMETHYST_SHARD,
                  'B',
                  Items.BLUE_STAINED_GLASS,
                  'O',
                  Items.GOLD_BLOCK,
                  'S',
                  Items.SPYGLASS,
                  'N',
                  Items.NETHERITE_INGOT,
                  'R',
                  Items.REDSTONE
               )
            )
            .withUnlock(ExoPiece.HELMET.threshold()),
         CraftRecipe.shaped(
               (ItemLike)AwakenedItems.EXOSAVANT_CHIP.get(),
               List.of("RCR", "NON", "RCR"),
               Map.of('R', Items.REDSTONE, 'C', Items.COPPER_INGOT, 'N', Items.NETHERITE_INGOT, 'O', Items.GOLD_BLOCK)
            )
            .withUnlock(ExoPiece.CHIP.threshold())
      );
   }

   @Override
   public void onReplaced(ServerPlayer player) {
      ExoSuit.drainStoreToInventory(player);
   }

   @Override
   public void onLogin(ServerPlayer player, PowerData data) {
      ExoSuit.migrateLegacyStore(player);
      ExoFlight.pushPref(player);
   }

   @Override
   public void onAwaken(ServerPlayer player) {
      ExoFlight.pushPref(player);
   }

   @Override
   public void passiveTick(ServerPlayer player, PowerData data) {
      super.passiveTick(player, data);
      ExoSuit.update(player, data);
      ExoFlight.tick(player, data);
      ExoTags.tick(player);
      ScanReveal.tick(player);
      ExoAnalysis.tick(player, data);
      Failsafe.tick(player, data);
      ExoSkinUnlocks.tick(player);
      ExoVisualState.sync(player, data);
   }

   @Override
   public float onIncomingDamage(ServerPlayer player, DamageSource source, float amount) {
      float result = super.onIncomingDamage(player, source, amount);
      boolean suited = ExoSuit.isSuited(player);
      if (suited && source.is(DamageTypeTags.IS_FALL)) {
         return 0.0F;
      } else if (suited) {
         if (result >= 6.0F && isHostileThreat(player, source)) {
            MasterySystem.award(player, MasteryReason.SURVIVAL);
         }

         return result;
      } else {
         return Failsafe.onLethal(player, source, result, PowerService.data(player));
      }
   }

   @Override
   public void onKill(ServerPlayer killer, LivingEntity victim) {
      super.onKill(killer, victim);
      if (Foes.countsAsKill(killer, victim)) {
         if (ExoTags.isTagged(killer, victim)) {
            QuestSystem.feat(killer, "exo_marked_kill");
         }

         if (ExoFlight.isFlying(killer)) {
            QuestSystem.feat(killer, "exo_air_kill");
         }
      }
   }

   private static boolean isHostileThreat(ServerPlayer player, DamageSource source) {
      return ExoTags.isHostileThreat(source.getEntity(), player);
   }

   @Override
   public void onDealDamage(ServerPlayer attacker, LivingEntity victim) {
      super.onDealDamage(attacker, victim);
      if (Foes.counts(attacker, victim)) {
         MasterySystem.award(attacker, MasteryReason.HIT);
      }

      if (ExoSuit.isSuited(attacker)) {
         DamageSource last = victim.getLastDamageSource();
         if (last != null && last.is(DamageTypes.PLAYER_ATTACK) && last.getDirectEntity() == attacker) {
            Harm.knockback(attacker, victim, 0.5, attacker.getX() - victim.getX(), attacker.getZ() - victim.getZ());
            ServerLevel level = Dmg.serverLevel(attacker);
            Vec3 at = victim.position().add(0.0, (double)victim.getBbHeight() * 0.5, 0.0);
            Fx.sound(level, at, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.4F, 1.6F);
            Fx.burst(level, ParticleTypes.CRIT, at, 8, 0.3, 0.1);
         }
      }
   }

   @Override
   public List<String> extraBalanceKeys() {
      return List.of("WarhammerThrow");
   }
}
