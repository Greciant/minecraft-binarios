package com.devilfruits.client.logia;

import com.devilfruits.client.fx.ClientFxRegistry;
import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.render.CameraEffects;
import com.devilfruits.client.render.vfx.VfxManager;
import com.devilfruits.client.sound.SoundDesign;
import com.devilfruits.core.logia.LogiaDash;
import com.devilfruits.core.logia.LogiaManager;
import com.devilfruits.core.logia.LogiaState;
import com.devilfruits.core.network.FxPayload;
import com.devilfruits.core.network.LogiaHitPayload;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class LogiaClient {
   public static void register() {
      ClientFxRegistry.register(LogiaDash.FX, LogiaClient::dash);
      ClientFxRegistry.register(LogiaManager.FIZZLE, (level, entity, p) -> {
         if (entity instanceof Player player) {
            LogiaRenderState.fizzle(player);
         }
      });
   }

   public static void onState(Player player, LogiaState before, LogiaState after) {
      LogiaRenderState.of(player).observe(player);
   }

   public static void onHits(Player player, List<LogiaHitPayload.Hit> hits) {
      LogiaRenderState.of(player).onHits(player, hits);
   }

   private static void dash(ClientLevel level, @Nullable Entity entity, FxPayload p) {
      if (entity instanceof Player player) {
         LogiaTheme theme = LogiaThemes.current(player);
         int ticks = Math.max(1, p.extra());
         LogiaRenderState.of(player).dashed();
         VfxManager.add(new LogiaDashEffect(player, p.pos(), ticks));
         RandomSource r = Fx.random();
         SoundDesign.play(p.pos().add(0.0, (double)player.getBbHeight() * 0.5, 0.0), theme.dashSound(), 0.8F, 0.9F + r.nextFloat() * 0.2F);
         if (player instanceof LocalPlayer local && local == Minecraft.getInstance().player) {
            LogiaMovementClient.dashStarted(local, p.dir(), ticks);
            CameraEffects.fovPunch(0.07F);
         }
      }
   }

   private LogiaClient() {
   }
}
