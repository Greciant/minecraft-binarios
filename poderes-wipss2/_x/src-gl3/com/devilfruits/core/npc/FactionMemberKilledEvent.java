package com.devilfruits.core.npc;

import javax.annotation.Nullable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.Event;

public class FactionMemberKilledEvent extends Event {
   private final FactionFighter victim;
   @Nullable
   private final Entity killer;
   private final DamageSource source;

   public FactionMemberKilledEvent(FactionFighter victim, @Nullable Entity killer, DamageSource source) {
      this.victim = victim;
      this.killer = killer;
      this.source = source;
   }

   public FactionFighter victim() {
      return this.victim;
   }

   @Nullable
   public Entity killer() {
      return this.killer;
   }

   public DamageSource source() {
      return this.source;
   }

   public Faction faction() {
      return this.victim.faction();
   }

   public int rank() {
      return this.victim.rank();
   }

   public int bounty() {
      return this.victim.bounty();
   }
}
