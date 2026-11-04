package com.devilfruits.fruits.bomu_bomu.explosion;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityRegistry;
import com.devilfruits.core.fx.ExplosionProfile;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public record ExplosionSpec(
   float power,
   float radius,
   float blockDamageScale,
   float entityDamage,
   float knockback,
   float fireChance,
   ExplosionProfile vfxProfile,
   ResourceLocation source
) {
   @Nullable
   public Ability ability() {
      return AbilityRegistry.get(this.source);
   }

   public CompoundTag save() {
      CompoundTag tag = new CompoundTag();
      tag.putFloat("Power", this.power);
      tag.putFloat("Radius", this.radius);
      tag.putFloat("Blocks", this.blockDamageScale);
      tag.putFloat("Damage", this.entityDamage);
      tag.putFloat("Knockback", this.knockback);
      tag.putFloat("Fire", this.fireChance);
      tag.putInt("Profile", this.vfxProfile.ordinal());
      tag.putString("Source", this.source.toString());
      return tag;
   }

   @Nullable
   public static ExplosionSpec load(CompoundTag tag) {
      ResourceLocation source = ResourceLocation.tryParse(tag.getString("Source"));
      return source != null && tag.contains("Radius")
         ? new ExplosionSpec(
            tag.getFloat("Power"),
            tag.getFloat("Radius"),
            tag.getFloat("Blocks"),
            tag.getFloat("Damage"),
            tag.getFloat("Knockback"),
            tag.getFloat("Fire"),
            ExplosionProfile.of(tag.getInt("Profile")),
            source
         )
         : null;
   }
}
