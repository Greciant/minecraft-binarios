package com.awakenedmod.awakened.content.summoning;

import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class SummonAbility extends Ability {
   private static final double AIM_RANGE = 6.0;

   public SummonAbility() {
      super("summon", 3.0F, 0);
   }

   @Override
   public float staminaCost(ServerPlayer player) {
      String type = SummonAttachments.souls(player).active();
      return type.isEmpty() ? this.staminaCost() : (float)SummonSouls.summonCost(type);
   }

   @Override
   public String costLabel() {
      return SummonSouls.costRangeLabel();
   }

   @Override
   public String costLabel(Player viewer) {
      String type = SummonAttachments.souls(viewer).active();
      return type.isEmpty() ? this.costLabel() : String.valueOf((int)this.billed((float)SummonSouls.summonCost(type)));
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().boosts("minion_health").boosts("minion_damage").build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      SoulData souls = SummonAttachments.souls(player);
      String type = souls.active();
      if (type.isEmpty() || souls.count(type) <= 0) {
         return this.fail(player, "awakened.summon.no_souls", new Object[0]);
      } else if (SummonSouls.isDormant(type)) {
         return this.fail(player, "awakened.summon.dormant_soul", new Object[]{SummonSouls.displayName(type)});
      } else if (MinionSupport.count(player) >= SummonSouls.maxMinions(player)) {
         return this.fail(player, "awakened.summon.at_capacity", new Object[]{SummonSouls.maxMinions(player)});
      } else {
         ServerLevel level = player.serverLevel();
         Vec3 spot = MinionSupport.groundSpot(player, 6.0);
         if (!souls.spendOne(type)) {
            return this.fail(player, "awakened.summon.no_souls", new Object[0]);
         } else {
            Mob mob = MinionSupport.spawnRising(player, type, spot);
            if (mob == null) {
               souls.add(type, 1, 100);
               return this.fail(player, "awakened.summon.no_ground", new Object[0]);
            } else {
               SummonFx.gather(level, mob.position());
               souls.recordUse(type);
               player.setData(SummonAttachments.SOULS, souls);
               return true;
            }
         }
      }
   }
}
