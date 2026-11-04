package com.devilfruits.fruits.kage_kage.item;

import com.devilfruits.fruits.kage_kage.KageEntities;
import com.devilfruits.fruits.kage_kage.entity.HollowCorpse;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

public class HollowCorpseItem extends Item {
   public HollowCorpseItem(Properties properties) {
      super(properties);
   }

   public InteractionResult useOn(UseOnContext ctx) {
      if (ctx.getClickedFace() != Direction.UP) {
         return InteractionResult.PASS;
      } else if (!(ctx.getLevel() instanceof ServerLevel level)) {
         return InteractionResult.SUCCESS;
      } else {
         Vec3 var7 = ctx.getClickLocation();
         HollowCorpse corpse = (HollowCorpse)KageEntities.HOLLOW_CORPSE.get().create(level);
         if (corpse == null) {
            return InteractionResult.FAIL;
         } else {
            Player player = ctx.getPlayer();
            float yaw = player != null ? player.getYRot() : 0.0F;
            corpse.moveTo(var7.x, var7.y, var7.z, yaw, 0.0F);
            if (!level.noCollision(corpse)) {
               return InteractionResult.FAIL;
            } else {
               level.addFreshEntity(corpse);
               level.playSound(null, var7.x, var7.y, var7.z, SoundEvents.ZOMBIE_VILLAGER_STEP, SoundSource.BLOCKS, 0.8F, 0.6F);
               if (player == null || !player.getAbilities().instabuild) {
                  ctx.getItemInHand().shrink(1);
               }

               return InteractionResult.CONSUME;
            }
         }
      }
   }
}
