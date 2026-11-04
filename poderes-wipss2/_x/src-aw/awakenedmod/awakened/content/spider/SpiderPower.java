package com.awakenedmod.awakened.content.spider;

import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Power;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class SpiderPower extends Power {
   public static final String ID = "spider";
   private static final String GAUNTLET_FLAG = "spider.had_gauntlets";
   private static final String TYPES_ANNOUNCED = "spider.types_announced";

   public SpiderPower() {
      super("spider", List.of(new WebSwingAbility(), new WebShotAbility(), new WebZipAbility(), new WebStrikeAbility(), new WebWingsAbility()));
   }

   @Override
   public int themeColor() {
      return -3001542;
   }

   @Override
   public List<String> extraBalanceKeys() {
      return List.of("SpiderRazorMine", "SpiderWebImpact", "SpiderWebBleed");
   }

   @Override
   public int signatureSlot() {
      return 0;
   }

   @Override
   public void onAwaken(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Vec3 c = player.position().add(0.0, 1.0, 0.0);
      Fx.sound(level, player.position(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 1.0F, 0.8F);
      level.sendParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 30, 0.6, 0.8, 0.6, 0.1);
      PowerData data = PowerService.data(player);
      data.setStrVar("spider.web_type", WebType.SPLAT.key());
      SpiderTypeSync.push(player);
   }

   @Override
   public void passiveTick(ServerPlayer player, PowerData data) {
      super.passiveTick(player, data);
      SpiderSense.tick(player, data);
      SpiderFreefall.tick(player);
      if ((player.tickCount & 7) == 0) {
         boolean now = SpiderTypeSync.hasGauntlets(player);
         boolean was = data.var("spider.had_gauntlets", 0.0F) > 0.5F;
         if (now != was) {
            data.setVar("spider.had_gauntlets", now ? 1.0F : 0.0F);
            SpiderTypeSync.push(player);
         }

         SpiderVisualState.setGauntlets(player, now);
      }

      announceTypeUnlocks(player, data);
   }

   private static void announceTypeUnlocks(ServerPlayer player, PowerData data) {
      int level = data.masteryLevel();
      int from = (int)data.var("spider.types_announced", 0.0F);
      if (level > from) {
         for (WebType type : WebType.values()) {
            int unlock = type.unlockLevel();
            if (unlock > 0 && unlock > from && unlock <= level) {
               String key = type.gauntletGated() ? "awakened.spider.type_unlocked_gauntlet" : "awakened.spider.type_unlocked";
               player.displayClientMessage(Component.translatable(key, new Object[]{type.displayName()}).withStyle(ChatFormatting.GOLD), false);
            }
         }

         data.setVar("spider.types_announced", (float)level);
      }
   }
}
