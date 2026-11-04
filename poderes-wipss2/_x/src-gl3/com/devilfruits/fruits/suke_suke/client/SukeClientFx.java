package com.devilfruits.fruits.suke_suke.client;

import com.devilfruits.client.fx.ClientFxRegistry;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.fx.ShockRing;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.fruits.suke_suke.SukeFx;
import com.devilfruits.fruits.suke_suke.SukeSuke;
import com.devilfruits.fruits.suke_suke.clear.ShipView;
import com.devilfruits.fruits.suke_suke.client.vfx.ShipCinematic;
import com.devilfruits.fruits.suke_suke.client.vfx.VanishCinematic;
import java.util.UUID;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class SukeClientFx {
   public static final int RING_TICKS = 12;

   static void register() {
      ClientFxRegistry.register(SukeFx.VANISH, (level, e, p) -> {
         if (e != null) {
            int delay = 0;
            if (p.power() < 0.5F && p.pos().lengthSqr() > 0.0) {
               double reach = Math.max(1.0, SukeSuke.setting("groupRadius"));
               delay = Mth.clamp((int)Math.round(e.position().distanceTo(p.pos()) / reach * 12.0), 0, 12);
            }

            ClearTransitions.vanish(e, delay);
            if (!VfxSettings.legacy()) {
               VfxDirector.spawn(new VanishCinematic(e, true));
            }
         }
      });
      ClientFxRegistry.register(SukeFx.APPEAR, (level, e, p) -> {
         if (e != null) {
            ClearTransitions.appear(e);
            if (!VfxSettings.legacy()) {
               VfxDirector.spawn(new VanishCinematic(e, false));
            }
         }
      });
      ClientFxRegistry.register(SukeFx.GROUP_CAST, (level, e, p) -> {
         if (e != null && !VfxSettings.legacy()) {
            groupRing(level, e, Math.max(1.0F, p.power()));
         }
      });
      ClientFxRegistry.register(SukeFx.SHIP_CAST, (level, e, p) -> {
         if (e != null && !VfxSettings.legacy()) {
            UUID newest = null;
            long start = Long.MIN_VALUE;

            for (Entry<UUID, ShipView.Entry> en : ShipView.all().entrySet()) {
               if (en.getValue().group().equals(e.getUUID()) && en.getValue().start() > start) {
                  start = en.getValue().start();
                  newest = en.getKey();
               }
            }

            if (newest != null) {
               boolean local = e == Minecraft.getInstance().player;
               ShipCinematic c = ship(newest, p.pos(), local);
               c.origin(p.pos(), local);
            }
         }
      });
   }

   static ShipCinematic ship(UUID ship, @Nullable Vec3 origin, boolean caster) {
      ShipCinematic c = VfxDirector.find(ShipCinematic.class, k -> !k.isDone() && k.ship().equals(ship));
      return c != null ? c : VfxDirector.spawn(new ShipCinematic(ship, origin, caster));
   }

   static void tickShips(ClientLevel level) {
      ShipView.forgetReturns(level.getGameTime(), 20);
      if (!VfxSettings.legacy()) {
         for (UUID id : ShipView.all().keySet()) {
            ship(id, null, false);
         }
      }
   }

   private static void groupRing(ClientLevel level, Entity caster, float radius) {
      Vec3 c = caster.position();
      double floor = VfxWorld.ground(level, c.x, c.y + 0.2, c.z, 3);
      Vec3 at = new Vec3(c.x, floor + 0.05, c.z);
      ShockRing.at(at, new Vec3(0.0, 1.0, 0.0)).radius(0.4F, radius + 0.6F).life(16).strength(0.85F).band(0.1F).rim(0.3F, 8378568).spawn();
      ShockRing.at(at, new Vec3(0.0, 1.0, 0.0)).radius(0.2F, radius * 0.7F).life(22).strength(0.4F).band(0.16F).rim(0.12F, 14546376).spawn();
      ShockRing.at(at.add(0.0, 1.0, 0.0), new Vec3(0.0, 1.0, 0.0)).radius(0.5F, radius + 0.4F).life(14).strength(0.35F).band(0.25F).spawn();
      RandomSource r = VfxSettings.random();
      float speed = radius / 12.0F;

      for (int i = 0; i < 28; i++) {
         double a = r.nextDouble() * (float) (Math.PI * 2);
         double s = (double)speed * (0.85 + r.nextDouble() * 0.2);
         VfxParticles.emit(
               VfxPresets.GLASS_GLINT,
               at.x + Math.cos(a) * 0.4,
               at.y + 0.05 + r.nextDouble() * 0.3,
               at.z + Math.sin(a) * 0.4,
               Math.cos(a) * s,
               0.01 + r.nextDouble() * 0.02,
               Math.sin(a) * s
            )
            .tint(i % 3 == 0 ? 16777215 : 12578784)
            .life(1.2F);
      }
   }

   private SukeClientFx() {
   }
}
