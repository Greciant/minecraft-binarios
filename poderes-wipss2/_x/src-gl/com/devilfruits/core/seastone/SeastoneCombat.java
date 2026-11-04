package com.devilfruits.core.seastone;

import com.devilfruits.core.combat.Stun;
import com.devilfruits.core.poneglyph.PoneglyphConfig;
import com.devilfruits.registry.DFAttachments;
import com.devilfruits.registry.DFTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post;

@EventBusSubscriber(
   modid = "devilfruits"
)
public final class SeastoneCombat {
   @SubscribeEvent
   public static void onDamaged(Post event) {
      if (event.getNewDamage() > 0.0F && event.getEntity() instanceof Player victim) {
         hit(victim, event.getSource());
      }
   }

   public static void hit(Player victim, DamageSource source) {
      if (DFAttachments.data(victim).fruit() != null) {
         ItemStack weapon;
         boolean seastoneBlow;
         boolean var10000;
         label36: {
            weapon = source.getWeaponItem();
            seastoneBlow = source.getDirectEntity() == source.getEntity() && weapon != null && weapon.is(SeastoneGear.GEAR);
            if (source.getDirectEntity() instanceof AbstractArrow arrow && SeastoneGear.isSeastoneArrow(arrow)) {
               var10000 = true;
               break label36;
            }

            var10000 = false;
         }

         boolean seastoneArrow = var10000;
         int ticks = (Integer)PoneglyphConfig.HIT_SUPPRESS_TICKS.get();
         if ((seastoneBlow || seastoneArrow) && ticks > 0) {
            victim.addEffect(new MobEffectInstance(SeastoneGear.SUPPRESSED, ticks, 0, false, true, true));
         }

         if (seastoneBlow && weapon.is((Item)SeastoneGear.JITTE.get())) {
            Stun.apply(victim, (Integer)PoneglyphConfig.JITTE_STUN_TICKS.get());
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOW
   )
   public static void onIncomingDamage(LivingIncomingDamageEvent event) {
      LivingEntity victim = event.getEntity();
      if (event.getSource().is(DFTags.DEVIL_FRUIT_ATTACKS)) {
         if (victim instanceof Player p && DFAttachments.data(p).fruit() != null) {
            return;
         }

         int pieces = armorPieces(victim);
         if (pieces > 0) {
            event.setAmount(event.getAmount() * (float)Math.max(0.0, 1.0 - (double)pieces * (Double)PoneglyphConfig.ARMOR_FRUIT_DAMAGE_REDUCTION.get()));
         }
      }
   }

   public static int armorPieces(LivingEntity entity) {
      int n = 0;

      for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
         if (entity.getItemBySlot(slot).is(SeastoneGear.GEAR)) {
            n++;
         }
      }

      return n;
   }

   private SeastoneCombat() {
   }
}
