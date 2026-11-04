package com.awakenedmod.awakened.content.spider;

import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(
   modid = "awakened"
)
public final class Webbing {
   private static final int MAX_STACKS = 5;
   private static final int LIFETIME = 80;
   private static final int SLOW_TICKS = 30;
   private static final Map<LivingEntity, Webbing.Web> WEBBED = new ConcurrentHashMap<>();
   private static int lastTick = -1;

   private Webbing() {
   }

   public static void splat(ServerPlayer source, ServerLevel level, @Nullable LivingEntity hit, Vec3 pos) {
      impactFx(level, pos);
      if (hit != null && hit != source && hit.isAlive() && !hit.isRemoved() && Harm.allowed(source, hit)) {
         Webbing.Web w = WEBBED.computeIfAbsent(hit, t -> new Webbing.Web(source.getUUID()));
         w.source = source.getUUID();
         w.stacks = Math.min(5, w.stacks + 1);
         w.expiry = level.getGameTime() + 80L;
         slow(source, hit, w.stacks);
         if (w.stacks >= 5 && !w.peaked) {
            w.peaked = true;
            SpiderFx.webUp(level, hit.position().add(0.0, (double)hit.getBbHeight() * 0.5, 0.0), (double)Math.max(hit.getBbWidth(), hit.getBbHeight()) * 0.5);
         } else {
            wrapFx(level, hit, w.stacks);
         }
      }
   }

   public static int stacks(LivingEntity target) {
      Webbing.Web w = WEBBED.get(target);
      return w == null ? 0 : w.stacks;
   }

   private static void slow(ServerPlayer source, LivingEntity t, int stacks) {
      Harm.effect(source, t, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, Math.min(4, stacks), false, false));
   }

   public static void tickAll(MinecraftServer server) {
      int now = server.getTickCount();
      if (now != lastTick) {
         lastTick = now;
         Iterator<Entry<LivingEntity, Webbing.Web>> it = WEBBED.entrySet().iterator();

         while (it.hasNext()) {
            Entry<LivingEntity, Webbing.Web> e = it.next();
            LivingEntity t = e.getKey();
            Webbing.Web w = e.getValue();
            if (t.isAlive() && !t.isRemoved()) {
               Level var7 = t.level();
               if (var7 instanceof ServerLevel) {
                  ServerLevel level = (ServerLevel)var7;
                  if (level.getGameTime() < w.expiry) {
                     slow(server.getPlayerList().getPlayer(w.source), t, w.stacks);
                     if (now % 10 == 0) {
                        wrapFx(level, t, w.stacks);
                     }
                     continue;
                  }
               }

               it.remove();
            } else {
               creditFullWrap(server, t, w);
               it.remove();
            }
         }
      }
   }

   private static void creditFullWrap(MinecraftServer server, LivingEntity target, Webbing.Web web) {
      if (web.peaked) {
         ServerPlayer weaver = server.getPlayerList().getPlayer(web.source);
         if (weaver != null && Foes.defeatedBy(target, weaver)) {
            QuestSystem.feat(weaver, "web_full_wrap");
         }
      }
   }

   @SubscribeEvent
   static void onServerTick(Post e) {
      tickAll(e.getServer());
   }

   @SubscribeEvent
   static void onServerStopped(ServerStoppedEvent e) {
      WEBBED.clear();
      lastTick = -1;
   }

   public static void clear(ServerPlayer player) {
      UUID id = player.getUUID();
      WEBBED.entrySet().removeIf(e -> id.equals(e.getValue().source));
   }

   private static void impactFx(ServerLevel level, Vec3 pos) {
      SpiderFx.splatImpact(level, pos);
   }

   private static void wrapFx(ServerLevel level, LivingEntity t, int stacks) {
      double w = (double)t.getBbWidth() * 0.35;
      double h = (double)t.getBbHeight();
      double band = stacks <= 1 ? 0.25 : 0.65;
      Vec3 c = t.position().add(0.0, h * band, 0.0);
      Fx.burst(level, SpiderFx.SILK, c, 1 + stacks, w, 0.005);
   }

   private static final class Web {
      private UUID source;
      private int stacks;
      private long expiry;
      private boolean peaked;

      private Web(UUID source) {
         this.source = source;
      }
   }
}
