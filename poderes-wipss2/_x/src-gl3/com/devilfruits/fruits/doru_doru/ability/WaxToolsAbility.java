package com.devilfruits.fruits.doru_doru.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.fruits.doru_doru.DoruAbilities;
import com.devilfruits.fruits.doru_doru.DoruDoru;
import com.devilfruits.fruits.doru_doru.item.WaxTools;
import com.devilfruits.registry.DFAttachments;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class WaxToolsAbility extends Ability {
   private static final Map<UUID, WaxTools.Kind> LAST = new HashMap<>();

   public WaxToolsAbility() {
      super(
         DoruDoru.id("wax_tools"),
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(4)
            .cooldown(6)
            .mastery(1)
            .damage(0.0)
            .range(0.0)
            .extra("toolDamageBonus", 0.0, -5.0, 20.0, "Attack damage added to every wax tool (they hit like gold tools by default).")
            .extra("keyEnergy", 15.0, 0.0, 500.0, "Energy the Wax Key costs (cast in the off hand on a Golden Key's mold; it opens a Devil Fruit Casket).")
            .build()
      );
   }

   @Override
   public boolean coatable() {
      return true;
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).manual().build();
   }

   @Override
   public Component checkRequirements(ServerPlayer player, PlayerFruitData data) {
      return DoruDoru.heatBlock(player);
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ItemStack held = player.getMainHandItem();
      if (WaxTools.isWaxTool(held)) {
         WaxTools.dissolve(player, held, player.getInventory().selected, false);
      } else {
         WaxTools.form(player, LAST.getOrDefault(player.getUUID(), WaxTools.Kind.PICKAXE), DoruAbilities.color(player));
      }
   }

   public static void choose(ServerPlayer player, int index) {
      WaxTools.Kind[] kinds = WaxTools.Kind.values();
      if (index >= 0 && index <= WaxTools.KEY_CHOICE && DoruDoru.canWax(player)) {
         PlayerFruitData data = DFAttachments.data(player);
         if (data.mastery() >= DoruAbilities.TOOLS.config().requiredMastery()) {
            if (index == WaxTools.KEY_CHOICE) {
               WaxTools.formKey(player, DoruAbilities.color(player));
            } else {
               LAST.put(player.getUUID(), kinds[index]);
               if (WaxTools.isWaxTool(player.getMainHandItem())) {
                  WaxTools.form(player, kinds[index], DoruAbilities.color(player));
               }
            }
         }
      }
   }

   public static void forget(UUID player) {
      LAST.remove(player);
   }
}
