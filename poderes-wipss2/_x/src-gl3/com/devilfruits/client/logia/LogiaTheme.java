package com.devilfruits.client.logia;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.sound.SoundDesign;
import com.devilfruits.core.logia.LogiaState;
import com.devilfruits.core.network.LogiaHitPayload;
import com.devilfruits.registry.DFSounds;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class LogiaTheme {
   public static final LogiaTheme DEFAULT = builder().build();
   private final int color;
   private final int edgeColor;
   private final Function<RandomSource, ParticleOptions> shed;
   private final Function<RandomSource, ParticleOptions> burst;
   @Nullable
   private final Function<RandomSource, ParticleOptions> clumps;
   private final float elementalDensity;
   private final float trail;
   private final float partialLevel;
   private final float dissolveRate;
   private final float reformRate;
   private final int[] dissolveOrder;
   private final int holeTicks;
   private final Supplier<SoundEvent> dissolveSound;
   private final Supplier<SoundEvent> reformSound;
   private final Supplier<SoundEvent> passSound;
   private final Supplier<SoundEvent> suppressSound;
   private final Supplier<SoundEvent> dashSound;
   private final SoundDesign.Layer[] passLayers;
   private final LogiaTheme.Hooks hooks;
   @Nullable
   private final String iconFolder;

   private LogiaTheme(LogiaTheme.Builder b) {
      this.color = b.color;
      this.edgeColor = b.edgeColor;
      Function<RandomSource, ParticleOptions> fallback = dust(b.color);
      this.shed = b.shed != null ? b.shed : fallback;
      this.burst = b.burst != null ? b.burst : this.shed;
      this.clumps = b.clumps;
      this.elementalDensity = b.elementalDensity;
      this.trail = b.trail;
      this.partialLevel = b.partialLevel;
      this.dissolveRate = b.dissolveRate;
      this.reformRate = b.reformRate;
      this.dissolveOrder = (int[])b.dissolveOrder.clone();
      this.holeTicks = b.holeTicks;
      this.dissolveSound = b.dissolveSound;
      this.reformSound = b.reformSound;
      this.passSound = b.passSound;
      this.suppressSound = b.suppressSound;
      this.dashSound = b.dashSound;
      this.passLayers = b.passLayers;
      this.hooks = b.hooks;
      this.iconFolder = b.iconFolder;
   }

   public static LogiaTheme.Builder builder() {
      return new LogiaTheme.Builder();
   }

   public static Function<RandomSource, ParticleOptions> dust(int rgb) {
      Vector3f c = new Vector3f((float)(rgb >> 16 & 0xFF) / 255.0F, (float)(rgb >> 8 & 0xFF) / 255.0F, (float)(rgb & 0xFF) / 255.0F);
      DustParticleOptions small = new DustParticleOptions(c, 0.7F);
      DustParticleOptions big = new DustParticleOptions(c, 1.1F);
      return r -> r.nextInt(3) == 0 ? big : small;
   }

   public int color() {
      return this.color;
   }

   public int edgeColor() {
      return this.edgeColor;
   }

   public ParticleOptions shed(RandomSource random) {
      return this.shed.apply(random);
   }

   public ParticleOptions burst(RandomSource random) {
      return this.burst.apply(random);
   }

   public ParticleOptions clumps(RandomSource random) {
      return (this.clumps != null ? this.clumps : this.burst).apply(random);
   }

   public float elementalDensity() {
      return this.elementalDensity;
   }

   public float trail() {
      return this.trail;
   }

   public float partialLevel() {
      return this.partialLevel;
   }

   public float dissolveRate() {
      return this.dissolveRate;
   }

   public float reformRate() {
      return this.reformRate;
   }

   public int[] dissolveOrder() {
      return this.dissolveOrder;
   }

   public int holeTicks() {
      return this.holeTicks;
   }

   public Supplier<SoundEvent> dissolveSound() {
      return this.dissolveSound;
   }

   public Supplier<SoundEvent> reformSound() {
      return this.reformSound;
   }

   public Supplier<SoundEvent> passSound() {
      return this.passSound;
   }

   public Supplier<SoundEvent> suppressSound() {
      return this.suppressSound;
   }

   public Supplier<SoundEvent> dashSound() {
      return this.dashSound;
   }

   public SoundDesign.Layer[] passLayers() {
      return this.passLayers;
   }

   public LogiaTheme.Hooks hooks() {
      return this.hooks;
   }

   public ResourceLocation stateIcon(String state) {
      return DevilFruits.id(this.iconFolder == null ? "logia/" + state : "logia/" + this.iconFolder + "/" + state);
   }

   public static final class Builder {
      private int color = 10132122;
      private int edgeColor = 7237230;
      private Function<RandomSource, ParticleOptions> shed;
      private Function<RandomSource, ParticleOptions> burst;
      @Nullable
      private Function<RandomSource, ParticleOptions> clumps;
      private float elementalDensity = 3.0F;
      private float trail = 2.0F;
      private float partialLevel = 0.85F;
      private float dissolveRate = 0.25F;
      private float reformRate = 0.2F;
      private int[] dissolveOrder = new int[]{8, 4, 32, 16, 2, 1};
      private int holeTicks = 30;
      private Supplier<SoundEvent> dissolveSound = DFSounds.LOGIA_DISSOLVE;
      private Supplier<SoundEvent> reformSound = DFSounds.LOGIA_REFORM;
      private Supplier<SoundEvent> passSound = DFSounds.LOGIA_PASS;
      private Supplier<SoundEvent> suppressSound = DFSounds.LOGIA_SUPPRESS;
      private Supplier<SoundEvent> dashSound = DFSounds.LOGIA_DASH;
      private SoundDesign.Layer[] passLayers = new SoundDesign.Layer[0];
      private LogiaTheme.Hooks hooks = new LogiaTheme.Hooks() {
      };
      @Nullable
      private String iconFolder;

      public LogiaTheme.Builder colors(int color, int edgeColor) {
         this.color = color;
         this.edgeColor = edgeColor;
         return this;
      }

      public LogiaTheme.Builder shed(Function<RandomSource, ParticleOptions> particles) {
         this.shed = particles;
         return this;
      }

      public LogiaTheme.Builder clumps(Function<RandomSource, ParticleOptions> particles) {
         this.clumps = particles;
         return this;
      }

      public LogiaTheme.Builder burst(Function<RandomSource, ParticleOptions> particles) {
         this.burst = particles;
         return this;
      }

      public LogiaTheme.Builder shedding(float elemental, float trail) {
         this.elementalDensity = elemental;
         this.trail = trail;
         return this;
      }

      public LogiaTheme.Builder partialLevel(float level) {
         this.partialLevel = level;
         return this;
      }

      public LogiaTheme.Builder rates(float dissolvePerTick, float reformPerTick) {
         this.dissolveRate = dissolvePerTick;
         this.reformRate = reformPerTick;
         return this;
      }

      public LogiaTheme.Builder dissolveOrder(int... limbs) {
         if (limbs.length != 6) {
            throw new IllegalArgumentException("dissolve order needs all 6 limbs");
         } else {
            this.dissolveOrder = (int[])limbs.clone();
            return this;
         }
      }

      public LogiaTheme.Builder holeTicks(int ticks) {
         this.holeTicks = ticks;
         return this;
      }

      public LogiaTheme.Builder sounds(
         Supplier<SoundEvent> dissolve, Supplier<SoundEvent> reform, Supplier<SoundEvent> pass, Supplier<SoundEvent> suppress, Supplier<SoundEvent> dash
      ) {
         this.dissolveSound = dissolve;
         this.reformSound = reform;
         this.passSound = pass;
         this.suppressSound = suppress;
         this.dashSound = dash;
         return this;
      }

      public LogiaTheme.Builder passLayers(SoundDesign.Layer... layers) {
         this.passLayers = layers;
         return this;
      }

      public LogiaTheme.Builder hooks(LogiaTheme.Hooks hooks) {
         this.hooks = hooks;
         return this;
      }

      public LogiaTheme.Builder stateIcons(String folder) {
         this.iconFolder = folder;
         return this;
      }

      public LogiaTheme build() {
         return new LogiaTheme(this);
      }
   }

   public interface Hooks {
      default void onState(Player player, LogiaState before, LogiaState after) {
      }

      default void onHit(Player player, LogiaHitPayload.Hit hit) {
      }

      default void tick(Player player, LogiaRenderState state) {
      }

      default void renderElemental(
         AbstractClientPlayer player, LogiaRenderState state, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight
      ) {
      }

      default void dashing(Player player, Vec3 from, Vec3 to) {
      }

      default LogiaTheme look(Player player, LogiaTheme theme) {
         return theme;
      }
   }
}
