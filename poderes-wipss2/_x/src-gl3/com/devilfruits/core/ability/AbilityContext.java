package com.devilfruits.core.ability;

import com.devilfruits.core.config.AbilityConfig;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.AbilityModifiers;
import com.devilfruits.core.form.FormAbility;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.haki.HakiCombat;
import com.devilfruits.core.haki.HakiConfig;
import com.devilfruits.core.logia.LogiaManager;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.core.world.CastBudget;
import com.devilfruits.core.world.TerrainDestruction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class AbilityContext {
   private final FormAbility entry;
   private final ServerPlayer player;
   private final PlayerFruitData data;
   private final float charge;
   private long seed;
   private boolean seeded;
   private CastBudget budget;

   public AbilityContext(FormAbility entry, ServerPlayer player, PlayerFruitData data, float charge) {
      this.entry = entry;
      this.player = player;
      this.data = data;
      this.charge = charge;
   }

   public Ability ability() {
      return this.entry.ability();
   }

   public FormAbility entry() {
      return this.entry;
   }

   public AbilityModifiers modifiers() {
      return this.entry.modifiers();
   }

   public String variant() {
      return this.entry.modifiers().variant();
   }

   public ServerPlayer player() {
      return this.player;
   }

   public ServerLevel level() {
      return this.player.serverLevel();
   }

   public PlayerFruitData data() {
      return this.data;
   }

   public DevilFruit fruit() {
      return this.entry.ability().fruit();
   }

   public AbilityConfig config() {
      return this.entry.ability().config();
   }

   public int mastery() {
      return this.data.mastery();
   }

   public float charge() {
      return this.charge;
   }

   public RandomSource random() {
      return this.player.getRandom();
   }

   public long seed() {
      if (!this.seeded) {
         this.seed = this.player.getRandom().nextLong();
         this.seeded = true;
      }

      return this.seed;
   }

   public CastBudget budget(int blocks) {
      this.budget().atLeast(blocks);
      return this.budget;
   }

   public CastBudget budget() {
      if (this.budget == null) {
         this.budget = CastBudget.fromConfig();
      }

      return this.budget;
   }

   public boolean blockDamageAllowed() {
      return this.config().blockDamage() && TerrainDestruction.destructionAllowed(this.level());
   }

   public float damage() {
      return (float)this.config().damage() * Mastery.damageMultiplier(this.mastery()) * this.modifiers().damage() * LogiaManager.abilityPower(this.player);
   }

   public float range() {
      return (float)this.config().range() * Mastery.rangeMultiplier(this.mastery()) * this.modifiers().range();
   }

   public float knockback() {
      return (float)this.config().knockback() * this.modifiers().knockback();
   }

   public double extra(String name) {
      return this.config().extra(name);
   }

   public boolean flag(String name) {
      return this.config().flag(name);
   }

   public ResourceLocation animation(ResourceLocation fallback) {
      ResourceLocation override = this.modifiers().animation();
      return override != null ? override : fallback;
   }

   public Vec3 eye() {
      return this.player.getEyePosition();
   }

   public Vec3 look() {
      return AimHelper.look(this.player);
   }

   public Vec3 aimFrom(Vec3 origin) {
      return AimHelper.lookFrom(this.player, origin);
   }

   public Vec3 horizontalLook() {
      Vec3 look = this.look();
      Vec3 flat = new Vec3(look.x, 0.0, look.z);
      if (flat.lengthSqr() < 1.0E-4) {
         float yaw = this.player.getYRot() * (float) (Math.PI / 180.0);
         return new Vec3(-Math.sin((double)yaw), 0.0, Math.cos((double)yaw));
      } else {
         return flat.normalize();
      }
   }

   public ResourceKey<DamageType> damageType() {
      ResourceKey<DamageType> own = this.ability().damageType();
      return own != null ? own : this.fruit().damageType();
   }

   public boolean hurt(Entity target, float amount) {
      if (this.ability().coatable() && HakiCombat.coatsTechnique(this.player)) {
         float coated = amount * ((Double)HakiConfig.ABILITY_MULTIPLIER.get()).floatValue();
         return HakiCombat.techniqueHurt(this.player, () -> Targeting.hurt(this.player, target, coated, this.damageType()));
      } else {
         return Targeting.hurt(this.player, target, amount, this.damageType());
      }
   }
}
