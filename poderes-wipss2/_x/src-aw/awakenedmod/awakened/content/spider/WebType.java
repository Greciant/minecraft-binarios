package com.awakenedmod.awakened.content.spider;

import com.awakenedmod.awakened.core.data.PowerData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public enum WebType {
   SPLAT("splat", 3.0F, 0, false, WebType.Flight.GLOB, -1512203, Items.COBWEB) {
      @Override
      public void apply(ServerPlayer caster, ServerLevel level, @Nullable LivingEntity hit, Vec3 pos) {
         Webbing.splat(caster, level, hit, pos);
      }
   },
   RAZOR("razor", 15.0F, 2, true, WebType.Flight.RAZOR, -1950917, Items.IRON_SWORD) {
      @Override
      public float velocity() {
         return 3.4F;
      }

      @Override
      public double gravity() {
         return 0.0;
      }

      @Override
      public double contactRadius() {
         return 0.4;
      }

      @Override
      public void apply(ServerPlayer caster, ServerLevel level, @Nullable LivingEntity hit, Vec3 pos) {
         SpiderDeployables.razor(caster, level, hit, pos);
      }
   },
   IMPACT("impact", 30.0F, 3, true, WebType.Flight.BALL, -2056144, Items.TNT) {
      @Override
      public float velocity() {
         return 1.6F;
      }

      @Override
      public double gravity() {
         return 0.05;
      }

      @Override
      public double contactRadius() {
         return 0.9;
      }

      @Override
      public void apply(ServerPlayer caster, ServerLevel level, @Nullable LivingEntity hit, Vec3 pos) {
         SpiderDeployables.impact(caster, level, hit, pos);
      }
   },
   WALL("wall", 50.0F, 4, true, WebType.Flight.GLOB, -6643020, Items.IRON_BARS) {
      @Override
      public float velocity() {
         return 3.0F;
      }

      @Override
      public double gravity() {
         return 0.0;
      }

      @Override
      public void apply(ServerPlayer caster, ServerLevel level, @Nullable LivingEntity hit, Vec3 pos) {
         SpiderDeployables.wall(caster, level, hit, pos);
      }
   };

   private static final WebType[] VALUES = values();
   private static final float DEFAULT_VELOCITY = 2.4F;
   private static final double DEFAULT_GRAVITY = 0.01;
   private final String key;
   private final float cost;
   private final int unlockLevel;
   private final boolean gauntletGated;
   private final WebType.Flight flight;
   private final int themeColor;
   private final Item icon;

   private WebType(String key, float cost, int unlockLevel, boolean gauntletGated, WebType.Flight flight, int themeColor, Item icon) {
      this.key = key;
      this.cost = cost;
      this.unlockLevel = unlockLevel;
      this.gauntletGated = gauntletGated;
      this.flight = flight;
      this.themeColor = themeColor;
      this.icon = icon;
   }

   public Item icon() {
      return this.icon;
   }

   public int themeColor() {
      return this.themeColor;
   }

   public float velocity() {
      return 2.4F;
   }

   public double gravity() {
      return 0.01;
   }

   public double contactRadius() {
      return 0.0;
   }

   public void flightFx(ServerLevel level, Vec3 pos, Vec3 velocity) {
      SpiderFx.shotInFlight(level, this.flight, this.themeColor, pos, velocity);
   }

   public abstract void apply(ServerPlayer var1, ServerLevel var2, @Nullable LivingEntity var3, Vec3 var4);

   public String key() {
      return this.key;
   }

   public float cost() {
      return this.cost;
   }

   public int unlockLevel() {
      return this.unlockLevel;
   }

   public boolean gauntletGated() {
      return this.gauntletGated;
   }

   public boolean isUnlocked(PowerData data, boolean hasGauntlets) {
      return (!this.gauntletGated || hasGauntlets) && data.masteryLevel() >= this.unlockLevel;
   }

   public Component displayName() {
      return Component.translatable("awakened.spider.webtype." + this.key);
   }

   public static WebType byKey(String key) {
      for (WebType type : VALUES) {
         if (type.key.equals(key)) {
            return type;
         }
      }

      return SPLAT;
   }

   public static WebType byOrdinal(int ordinal) {
      return VALUES[Mth.clamp(ordinal, 0, VALUES.length - 1)];
   }

   public static int count() {
      return VALUES.length;
   }

   public static enum Flight {
      GLOB,
      RAZOR,
      BALL;
   }
}
