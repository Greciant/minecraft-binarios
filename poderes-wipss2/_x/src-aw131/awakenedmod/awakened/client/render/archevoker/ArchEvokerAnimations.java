package com.awakenedmod.awakened.client.render.archevoker;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.animation.AnimationChannel.Interpolations;
import net.minecraft.client.animation.AnimationChannel.Targets;
import net.minecraft.client.animation.AnimationDefinition.Builder;

public final class ArchEvokerAnimations {
   public static final AnimationDefinition Idle = Builder.withLength(1.0F)
      .looping()
      .addAnimation(
         "body",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(2.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "body",
         new AnimationChannel(
            Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 4.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, 6.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.posVec(0.0F, 4.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "Head",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(-2.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "leg0",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(2.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "leg0",
         new AnimationChannel(Targets.POSITION, new Keyframe[]{new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -3.0F, 0.0F), Interpolations.LINEAR)})
      )
      .addAnimation(
         "leg1",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(27.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(25.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.degreeVec(27.5F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "leg1",
         new AnimationChannel(Targets.POSITION, new Keyframe[]{new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -2.0F, -1.0F), Interpolations.LINEAR)})
      )
      .addAnimation(
         "RightArm",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.5F), Interpolations.LINEAR),
               new Keyframe(0.5417F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 15.0F), Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.5F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "LeftArm",
         new AnimationChannel(Targets.ROTATION, new Keyframe[]{new Keyframe(0.0F, KeyframeAnimations.degreeVec(15.0F, 0.0F, -17.5F), Interpolations.LINEAR)})
      )
      .addAnimation(
         "Staff",
         new AnimationChannel(
            Targets.ROTATION, new Keyframe[]{new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.4407F, 9.9904F, 65.0385F), Interpolations.LINEAR)}
         )
      )
      .addAnimation(
         "Staff",
         new AnimationChannel(Targets.POSITION, new Keyframe[]{new Keyframe(0.0F, KeyframeAnimations.posVec(-4.0F, 2.0F, 0.0F), Interpolations.LINEAR)})
      )
      .build();
   public static final AnimationDefinition SummonStart = Builder.withLength(1.1438F)
      .addAnimation(
         "body",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.9804F, KeyframeAnimations.degreeVec(2.5F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "body",
         new AnimationChannel(
            Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 4.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.9804F, KeyframeAnimations.posVec(0.0F, 6.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "Head",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.9804F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "leg0",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.9804F, KeyframeAnimations.degreeVec(2.5F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "leg0",
         new AnimationChannel(Targets.POSITION, new Keyframe[]{new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -3.0F, 0.0F), Interpolations.LINEAR)})
      )
      .addAnimation(
         "leg1",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(27.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.9804F, KeyframeAnimations.degreeVec(25.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "leg1",
         new AnimationChannel(Targets.POSITION, new Keyframe[]{new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -2.0F, -1.0F), Interpolations.LINEAR)})
      )
      .addAnimation(
         "RightArm",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.5F), Interpolations.LINEAR),
               new Keyframe(1.1438F, KeyframeAnimations.degreeVec(-84.5904F, -22.4098F, 12.7403F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "LeftArm",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(15.0F, 0.0F, -17.5F), Interpolations.LINEAR),
               new Keyframe(1.1438F, KeyframeAnimations.degreeVec(-74.2169F, 25.6652F, 6.1595F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "Staff",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.4407F, 9.9904F, 65.0385F), Interpolations.LINEAR),
               new Keyframe(1.1438F, KeyframeAnimations.degreeVec(10.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "Staff",
         new AnimationChannel(
            Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(-4.0F, 2.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.1438F, KeyframeAnimations.posVec(-3.0F, -1.0F, 2.0F), Interpolations.LINEAR)
            }
         )
      )
      .build();
   public static final AnimationDefinition Summon = Builder.withLength(1.5523F)
      .looping()
      .addAnimation(
         "body",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.4902F, KeyframeAnimations.degreeVec(2.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.5523F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "body",
         new AnimationChannel(
            Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 4.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.4902F, KeyframeAnimations.posVec(0.0F, 6.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.7353F, KeyframeAnimations.posVec(0.0F, 7.33F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.5523F, KeyframeAnimations.posVec(0.0F, 4.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "Head",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.7353F, KeyframeAnimations.degreeVec(-32.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.5523F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "Head",
         new AnimationChannel(
            Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.5523F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "leg0",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.7353F, KeyframeAnimations.degreeVec(10.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.5523F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "leg0",
         new AnimationChannel(Targets.POSITION, new Keyframe[]{new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -3.0F, 0.0F), Interpolations.LINEAR)})
      )
      .addAnimation(
         "leg1",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(27.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.7353F, KeyframeAnimations.degreeVec(37.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.5523F, KeyframeAnimations.degreeVec(27.5F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "leg1",
         new AnimationChannel(Targets.POSITION, new Keyframe[]{new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -2.0F, -1.0F), Interpolations.LINEAR)})
      )
      .addAnimation(
         "RightArm",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(-84.4862F, -24.8985F, 12.4805F), Interpolations.LINEAR),
               new Keyframe(0.0817F, KeyframeAnimations.degreeVec(-84.5904F, -22.4098F, 12.7403F), Interpolations.LINEAR),
               new Keyframe(0.7353F, KeyframeAnimations.degreeVec(-100.7199F, -22.8949F, -12.536F), Interpolations.LINEAR),
               new Keyframe(1.5523F, KeyframeAnimations.degreeVec(-84.4862F, -24.8985F, 12.4805F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "RightArm",
         new AnimationChannel(
            Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.7353F, KeyframeAnimations.posVec(0.0F, 1.0F, -4.0F), Interpolations.LINEAR),
               new Keyframe(1.5523F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "LeftArm",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(-74.2169F, 25.6652F, -6.1595F), Interpolations.LINEAR),
               new Keyframe(0.0817F, KeyframeAnimations.degreeVec(-73.8687F, 28.0689F, -5.3891F), Interpolations.LINEAR),
               new Keyframe(0.7353F, KeyframeAnimations.degreeVec(-122.1923F, 27.0022F, -13.9003F), Interpolations.LINEAR),
               new Keyframe(1.5523F, KeyframeAnimations.degreeVec(-74.2169F, 25.6652F, -6.1595F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "LeftArm",
         new AnimationChannel(
            Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.7353F, KeyframeAnimations.posVec(0.0F, 0.0F, -4.0F), Interpolations.LINEAR),
               new Keyframe(1.5523F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "Staff",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.5523F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "Staff",
         new AnimationChannel(
            Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(-3.0F, -2.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.7353F, KeyframeAnimations.posVec(-2.0F, -2.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.5523F, KeyframeAnimations.posVec(-3.0F, -2.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .build();
   public static final AnimationDefinition SummonEnd = Builder.withLength(1.2255F)
      .addAnimation(
         "body",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.9804F, KeyframeAnimations.degreeVec(2.5F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "body",
         new AnimationChannel(
            Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 4.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.9804F, KeyframeAnimations.posVec(0.0F, 6.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "Head",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.2255F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "leg0",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.9804F, KeyframeAnimations.degreeVec(2.5F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "leg0",
         new AnimationChannel(Targets.POSITION, new Keyframe[]{new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -3.0F, 0.0F), Interpolations.LINEAR)})
      )
      .addAnimation(
         "leg1",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(27.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.9804F, KeyframeAnimations.degreeVec(25.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "leg1",
         new AnimationChannel(Targets.POSITION, new Keyframe[]{new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -2.0F, -1.0F), Interpolations.LINEAR)})
      )
      .addAnimation(
         "RightArm",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(-84.4862F, -24.8985F, 12.4805F), Interpolations.LINEAR),
               new Keyframe(0.0817F, KeyframeAnimations.degreeVec(-84.5904F, -22.4098F, 12.7403F), Interpolations.LINEAR),
               new Keyframe(0.817F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.5F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "LeftArm",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(-74.2169F, 25.6652F, -6.1595F), Interpolations.LINEAR),
               new Keyframe(0.0817F, KeyframeAnimations.degreeVec(-73.8687F, 28.0689F, -5.3891F), Interpolations.LINEAR),
               new Keyframe(1.2255F, KeyframeAnimations.degreeVec(15.0F, 0.0F, -17.5F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "Staff",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5719F, KeyframeAnimations.degreeVec(-10.363F, 6.7527F, 10.332F), Interpolations.LINEAR),
               new Keyframe(0.8987F, KeyframeAnimations.degreeVec(-0.5538F, 12.4883F, 64.9392F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "Staff",
         new AnimationChannel(
            Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(-3.0F, -2.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5719F, KeyframeAnimations.posVec(-0.88F, -1.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.8987F, KeyframeAnimations.posVec(-2.78F, 0.8F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.2255F, KeyframeAnimations.posVec(-4.0F, 1.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .build();
   public static final AnimationDefinition Float = Builder.withLength(1.0F)
      .looping()
      .addAnimation(
         "body",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(12.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(2.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.degreeVec(12.5F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "body",
         new AnimationChannel(
            Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 4.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, 6.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.posVec(0.0F, 4.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "Head",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(-15.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(-2.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.degreeVec(-15.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "leg0",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(2.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "leg0",
         new AnimationChannel(Targets.POSITION, new Keyframe[]{new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -3.0F, 0.0F), Interpolations.LINEAR)})
      )
      .addAnimation(
         "leg1",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(27.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(25.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.degreeVec(27.5F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "leg1",
         new AnimationChannel(Targets.POSITION, new Keyframe[]{new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -2.0F, -1.0F), Interpolations.LINEAR)})
      )
      .addAnimation(
         "RightArm",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(17.5F, 0.0F, 12.5F), Interpolations.LINEAR),
               new Keyframe(0.5417F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 15.0F), Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.degreeVec(17.5F, 0.0F, 12.5F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "LeftArm",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(25.0F, 0.0F, -17.5F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(12.5F, 0.0F, -17.5F), Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.degreeVec(25.0F, 0.0F, -17.5F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "Staff",
         new AnimationChannel(
            Targets.ROTATION, new Keyframe[]{new Keyframe(0.0F, KeyframeAnimations.degreeVec(1.0292F, 15.4924F, 61.769F), Interpolations.LINEAR)}
         )
      )
      .addAnimation(
         "Staff",
         new AnimationChannel(Targets.POSITION, new Keyframe[]{new Keyframe(0.0F, KeyframeAnimations.posVec(-3.0F, 2.0F, 0.0F), Interpolations.LINEAR)})
      )
      .build();
   public static final AnimationDefinition Fangs = Builder.withLength(1.0F)
      .addAnimation(
         "body",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "body",
         new AnimationChannel(
            Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 4.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, -1.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.posVec(0.0F, 4.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "Head",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.25F, KeyframeAnimations.degreeVec(-1.25F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.375F, KeyframeAnimations.degreeVec(-8.54F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(15.67F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "leg0",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(-2.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "leg0",
         new AnimationChannel(
            Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -3.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, 1.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.posVec(0.0F, -3.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "leg1",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(27.5F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(-3.7492F, -1.407F, -5.6113F), Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.degreeVec(27.5F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "leg1",
         new AnimationChannel(
            Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -2.0F, -1.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, 1.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.posVec(0.0F, -2.0F, -1.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "RightArm",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.5F), Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.5F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "LeftArm",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(15.0F, 0.0F, -17.5F), Interpolations.LINEAR),
               new Keyframe(0.25F, KeyframeAnimations.degreeVec(-75.7205F, -33.1384F, -1.6059F), Interpolations.LINEAR),
               new Keyframe(0.3333F, KeyframeAnimations.degreeVec(-100.6404F, -29.4564F, -3.3719F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(-51.7303F, -22.0923F, -6.9039F), Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.degreeVec(15.0F, 0.0F, -17.5F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "LeftArm",
         new AnimationChannel(
            Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.25F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.3333F, KeyframeAnimations.posVec(0.0F, -2.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "Staff",
         new AnimationChannel(
            Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(-0.4407F, 9.9F, 65.0385F), Interpolations.LINEAR),
               new Keyframe(0.25F, KeyframeAnimations.degreeVec(-8.5182F, 9.1825F, 24.3146F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(-25.0694F, -4.0723F, 8.4108F), Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.degreeVec(-0.4407F, 9.9F, 65.0385F), Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "Staff",
         new AnimationChannel(
            Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(-4.0F, 2.0F, 0.0F), Interpolations.LINEAR),
               new Keyframe(0.25F, KeyframeAnimations.posVec(-1.0F, 0.0F, 5.0F), Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.posVec(-1.33F, -3.0F, 3.33F), Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.posVec(-4.0F, 2.0F, 0.0F), Interpolations.LINEAR)
            }
         )
      )
      .build();

   private ArchEvokerAnimations() {
   }
}
