package com.devilfruits.fruits.yomi_yomi.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.fruits.yomi_yomi.YomiYomi;
import com.devilfruits.fruits.yomi_yomi.soulking.SoulKing;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public abstract class KingAbility extends Ability {
   protected KingAbility(String name, CastType castType, AbilityStats stats) {
      super(YomiYomi.id(name), castType, stats);
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.NONE;
   }

   @Nullable
   @Override
   public Component checkRequirements(ServerPlayer player, PlayerFruitData data) {
      return SoulKing.ready(player) ? null : Component.translatable("message.devilfruits.yomi_yomi.king_rising");
   }

   protected static boolean present(ServerPlayer player, ServerLevel level) {
      return player.isAlive() && !player.isRemoved() && player.level() == level && SoulKing.active(player);
   }

   protected static Vec3 away(Vec3 from, Entity e, Vec3 fallback) {
      Vec3 d = new Vec3(e.getX() - from.x, 0.0, e.getZ() - from.z);
      return d.lengthSqr() < 1.0E-4 ? fallback : d.normalize();
   }

   protected static void breakShield(Entity e) {
      if (e instanceof Player p && p.isBlocking()) {
         p.disableShield();
      }
   }
}
