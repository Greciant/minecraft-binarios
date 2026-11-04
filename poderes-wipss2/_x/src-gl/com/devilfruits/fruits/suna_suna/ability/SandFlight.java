package com.devilfruits.fruits.suna_suna.ability;

import com.devilfruits.core.combat.AbilityProjectiles;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.fruits.suna_suna.SunaFx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

final class SandFlight extends AbilityProjectiles.Flight {
   static final int PESADO = 0;
   static final int LA_SPADA = 1;
   static final int BARJAN = 2;
   static final int SPADA = 3;
   private final ServerPlayer caster;
   private final long seed;
   private final float size;
   private final int kind;

   SandFlight(ServerPlayer caster, Vec3 at, double radius, long seed, float size, int kind) {
      super(caster, at, radius);
      this.caster = caster;
      this.seed = seed;
      this.size = size;
      this.kind = kind;
   }

   static SandFlight track(ServerLevel level, ServerPlayer caster, Vec3 at, double radius, long seed, float size, int kind) {
      return AbilityProjectiles.track(level, new SandFlight(caster, at, radius, seed, size, kind));
   }

   @Override
   protected void shatter(ServerLevel level, Entity by, Vec3 push) {
      ServerFx.sendNear(level, SunaFx.CRUMBLE, this.caster.getId(), this.position(), push, this.size, this.seed, this.kind, 96.0);
   }
}
