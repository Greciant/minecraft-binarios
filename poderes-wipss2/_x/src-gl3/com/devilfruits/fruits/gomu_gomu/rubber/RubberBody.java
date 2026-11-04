package com.devilfruits.fruits.gomu_gomu.rubber;

import com.devilfruits.core.logia.DamageCategory;
import com.devilfruits.core.logia.LogiaDamage;
import com.devilfruits.fruits.gomu_gomu.GomuGomu;
import com.devilfruits.fruits.gomu_gomu.GomuTags;
import javax.annotation.Nullable;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;

public final class RubberBody {
   @Nullable
   public static RubberBody.Shrug shrugs(Player victim, DamageSource source) {
      if (!GomuGomu.canUse(victim)) {
         return null;
      } else if (source.is(GomuTags.ELECTRIC)) {
         return GomuGomu.flag("lightningImmunity") ? RubberBody.Shrug.LIGHTNING : null;
      } else if (source.is(DamageTypeTags.IS_FALL) && !source.is(DamageTypes.STALAGMITE)) {
         return GomuGomu.flag("fallImmunity") ? RubberBody.Shrug.FALL : null;
      } else if (!GomuGomu.flag("bluntImmunity")) {
         return null;
      } else if (source.is(GomuTags.BLUNT_DAMAGE)) {
         return RubberBody.Shrug.BLUNT;
      } else if (DamageCategory.of(source) != DamageCategory.ALWAYS && !LogiaDamage.bypasses(source)) {
         boolean melee = source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO);
         Entity attacker = source.getEntity();
         if (melee && attacker instanceof LivingEntity living && source.getDirectEntity() == attacker) {
            ItemStack weapon = source.getWeaponItem();
            if (weapon == null) {
               weapon = living.getMainHandItem();
            }

            return blunt(living, weapon) ? RubberBody.Shrug.BLUNT : null;
         }

         return null;
      } else {
         return null;
      }
   }

   public static boolean blunt(LivingEntity attacker, ItemStack weapon) {
      if (!weapon.isEmpty()) {
         if (weapon.is(GomuTags.BLUNT_WEAPONS)) {
            return true;
         }

         if (sharp(weapon)) {
            return false;
         }

         if (attacker instanceof Player) {
            return true;
         }
      }

      if (attacker instanceof Player) {
         return true;
      } else {
         return attacker.getType().is(GomuTags.SHARP_ATTACKERS) ? false : attacker.getType().is(GomuTags.BLUNT_ATTACKERS);
      }
   }

   public static boolean sharp(ItemStack weapon) {
      return weapon.is(GomuTags.SHARP_WEAPONS)
         || weapon.getItem() instanceof SwordItem
         || weapon.getItem() instanceof AxeItem
         || weapon.getItem() instanceof PickaxeItem
         || weapon.getItem() instanceof HoeItem
         || weapon.getItem() instanceof TridentItem;
   }

   private RubberBody() {
   }

   public static enum Shrug {
      BLUNT,
      FALL,
      LIGHTNING;
   }
}
