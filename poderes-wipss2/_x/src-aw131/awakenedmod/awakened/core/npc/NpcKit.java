package com.awakenedmod.awakened.core.npc;

import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.Vec3;

public abstract class NpcKit {
   private final String id;
   private final String powerId;
   private Supplier<EntityType<PoweredNpc>> type;

   protected NpcKit(String id, String powerId) {
      this.id = id;
      this.powerId = powerId;
   }

   public final String id() {
      return this.id;
   }

   public final String powerId() {
      return this.powerId;
   }

   public final Supplier<EntityType<PoweredNpc>> type() {
      return this.type;
   }

   final void bindType(Supplier<EntityType<PoweredNpc>> type) {
      this.type = type;
   }

   public Component powerName() {
      Power power = PowerRegistry.get(this.powerId);
      return (Component)(power != null ? power.displayName() : Component.literal(this.powerId));
   }

   public abstract ResourceLocation skin();

   public abstract List<NpcMove> createMoves();

   public List<NpcTactic> createTactics() {
      return List.of();
   }

   public abstract List<ResourceLocation> clips();

   public Builder attributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 30.0)
         .add(Attributes.MOVEMENT_SPEED, 0.3)
         .add(Attributes.ARMOR, 2.0)
         .add(Attributes.FOLLOW_RANGE, 32.0);
   }

   public void passiveTick(NpcCaster caster) {
   }

   public void onNotice(NpcContext ctx) {
   }

   public void noticeTick(NpcContext ctx, int age) {
   }

   public void onRemoved(NpcCaster caster) {
   }

   public float strain(NpcCaster caster) {
      return 0.0F;
   }

   public void onVent(NpcContext ctx) {
   }

   @Nullable
   public Vec3 hazardNear(NpcCaster caster) {
      return null;
   }

   public void idleFlourish(NpcCaster caster) {
   }

   @Nullable
   public Vec3 pointOfInterest(NpcCaster caster) {
      return null;
   }

   public double spawnWeight() {
      return 1.0;
   }

   public boolean avoidsWater() {
      return false;
   }

   public double preferredMin() {
      return 8.0;
   }

   public double preferredMax() {
      return 16.0;
   }
}
