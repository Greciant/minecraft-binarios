package com.impactframe.client;

import com.impactframe.ImpactConfig;
import com.impactframe.network.FreezeRequestPayload;
import com.impactframe.network.HighDamageAttackPayload;
import com.mojang.blaze3d.platform.InputConstants.Type;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.ClientTickEvent.Pre;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles;
import net.neoforged.neoforge.client.event.ViewportEvent.ComputeFov;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.common.Tags.EntityTypes;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.joml.Vector4f;

@EventBusSubscriber(
   modid = "impactframe",
   value = {Dist.CLIENT}
)
public final class ImpactFrameClient {
   private static final ResourceLocation EXPLOSION_SOUND = ResourceLocation.fromNamespaceAndPath("minecraft", "entity.generic.explode");
   private static final int KILL_WINDOW_TICKS = 40;
   private static final int ATTACK_DAMAGE_WINDOW_TICKS = 12;
   private static final int BREAK_WINDOW_TICKS = 200;
   private static final int PLACE_WINDOW_TICKS = 20;
   private static final double EXPLOSION_RANGE = 48.0;
   private static final float LAND_HEAVY_FALL = 10.0F;
   private static final float SMASH_MIN_FALL = 1.5F;
   private static final float WEAK_SWING = 0.4F;
   private static final float BIG_HIT = 6.0F;
   private static final float CINEMATIC_DAMAGE_THRESHOLD = 12.0F;
   private static final float CINEMATIC_ATTACK_THRESHOLD = 15.0F;
   private static final float LOW_HEALTH = 6.0F;
   private static final float HARD_BLOCK = 20.0F;
   private static final float TOUGH_BLOCK = 5.0F;
   public static final ImpactType[] PREVIEW_TYPES = new ImpactType[]{
      ImpactType.ATTACK, ImpactType.DAMAGE, ImpactType.CRIT, ImpactType.EXPLOSION, ImpactType.KILL, ImpactType.MACE
   };
   private static int clientTicks;
   private static float lastYaw;
   private static float lastPitch;
   private static boolean swingReady;
   private static float swingX;
   private static float swingY;
   private static float pendingDirX;
   private static float pendingDirY;
   private static LivingEntity killCandidate;
   private static int killExpiresAt;
   private static boolean killSmash;
   private static LivingEntity attackDamageCandidate;
   private static float attackHealthBefore;
   private static int attackDamageExpiresAt;
   private static long attackImpactSerial = -1L;
   private static boolean attackServerFeedback;
   private static boolean attackRequiresMinimumDamage;
   private static boolean attackImpactEvaluated;
   private static ImpactType pendingAttackType;
   private static ImpactTier pendingAttackTier;
   private static float pendingAttackScale;
   private static BlockPos breakCandidate;
   private static int breakExpiresAt;
   private static float breakHardness;
   private static BlockPos placeCandidate;
   private static BlockState placeOriginalState;
   private static int placeExpiresAt;
   private static int lastHurtTime;
   private static float lastHealth = -1.0F;
   private static float healthBeforeHit;
   private static int damageEvalAt = -1;
   private static boolean wasOnGround = true;
   private static float peakFall;
   private static float focusU = 0.5F;
   private static float focusV = 0.5F;
   private static float focusRadius;
   private static boolean freezeArmed;
   private static boolean freezeSeen;
   private static int freezeArmedUntil;

   private ImpactFrameClient() {
   }

   @SubscribeEvent
   public static void onClientTickPre(Pre event) {
      FreezeLock.tick(Minecraft.getInstance().player);
   }

   @SubscribeEvent
   public static void onMovementInput(MovementInputUpdateEvent event) {
      if (event.getEntity() == Minecraft.getInstance().player) {
         FreezeLock.applyToInput(event.getInput());
      }
   }

   @SubscribeEvent
   public static void onClientTickPost(Post event) {
      syncFreezeHold();
      ImpactState.tick();
      clientTicks++;
      Minecraft mc = Minecraft.getInstance();
      if (ImpactFrameClient.ModBusEvents.OPEN_SETTINGS.consumeClick()
         && !(mc.screen instanceof ImpactPresetScreen)
         && !(mc.screen instanceof AdvancedImpactScreen)
         && !(mc.screen instanceof ImpactOptionsScreen)
         && !(mc.screen instanceof ServerFeaturesScreen)) {
         mc.setScreen(new ImpactPresetScreen(mc.screen));
      }

      LocalPlayer player = mc.player;
      Level level = mc.level;
      if (player != null && level != null) {
         trackSwing(player);
         checkDamage(player);
         checkAttackDamage();
         checkLanding(player);
         checkKill();
         checkBlockBreak(level);
         checkBlockPlace(level);
      } else {
         resetTracking();
      }
   }

   @SubscribeEvent
   public static void onAttackEntity(AttackEntityEvent event) {
      Player player = event.getEntity();
      if (player == Minecraft.getInstance().player) {
         Entity target = event.getTarget();
         if (target instanceof EnderDragonPart part) {
            target = part.parentMob;
         }

         LivingEntity livingTarget = target instanceof LivingEntity livingEntity ? livingEntity : null;
         boolean living = livingTarget != null;
         float targetHealthBefore = livingTarget == null ? 0.0F : livingTarget.getHealth();
         float charge = player.getAttackStrengthScale(0.5F);
         float fall = player.fallDistance;
         boolean smash = living && charge > 0.5F && player.getMainHandItem().is(Items.MACE) && fall > 1.5F && !player.isFallFlying();
         boolean crit = living
            && charge > 0.9F
            && fall > 0.0F
            && !player.onGround()
            && !player.onClimbable()
            && !player.isInWater()
            && !player.hasEffect(MobEffects.BLINDNESS)
            && !player.isPassenger()
            && !player.isSprinting();
         setSwingDirection();
         ImpactType attackType = ImpactType.ATTACK;
         boolean smashFired = false;
         ImpactTier attackTier;
         float attackScale;
         if (smash && ImpactConfig.triggerEnabled(ImpactType.MACE.key)) {
            attackType = ImpactType.MACE;
            attackTier = ImpactTier.SMASH;
            attackScale = Mth.clamp(fall / 8.0F, 0.7F, 1.0F);
            smashFired = true;
         } else if (crit && ImpactConfig.triggerEnabled(ImpactType.CRIT.key)) {
            attackType = ImpactType.CRIT;
            attackTier = ImpactTier.HEAVY;
            attackScale = 1.0F;
         } else if (charge < 0.4F) {
            attackTier = ImpactTier.LIGHT;
            attackScale = 0.8F;
         } else {
            attackTier = ImpactTier.NORMAL;
            attackScale = 1.0F;
         }

         clearAttackDamageCandidate();
         boolean requiresDamage = livingTarget != null && ImpactConfig.useMinimumImpactThresholds();
         boolean attackImpactFired = false;
         if (requiresDamage) {
            pendingAttackType = attackType;
            pendingAttackTier = attackTier;
            pendingAttackScale = attackScale;
            attackRequiresMinimumDamage = true;
            attackImpactEvaluated = false;
         } else {
            attackImpactEvaluated = true;
            attackImpactFired = fire(attackType, attackTier, attackScale, target);
         }

         if (livingTarget != null) {
            killCandidate = livingTarget;
            killExpiresAt = clientTicks + 40;
            killSmash = smashFired;
            attackDamageCandidate = livingTarget;
            attackHealthBefore = targetHealthBefore;
            attackDamageExpiresAt = clientTicks + 12;
            ClientPacketListener connection = Minecraft.getInstance().getConnection();
            attackServerFeedback = connection != null && connection.hasChannel(HighDamageAttackPayload.TYPE);
            if (attackImpactFired) {
               attackImpactSerial = ImpactState.triggerSerial();
            }
         }
      }
   }

   @SubscribeEvent
   public static void onLeftClickBlock(LeftClickBlock event) {
      Minecraft mc = Minecraft.getInstance();
      if (event.getEntity() == mc.player && event.getLevel() == mc.level) {
         if (event.getAction() == Action.START) {
            BlockPos pos = event.getPos();
            if (!mc.level.getBlockState(pos).isAir()) {
               breakCandidate = pos.immutable();
               breakExpiresAt = clientTicks + 200;
               breakHardness = mc.level.getBlockState(pos).getDestroySpeed(mc.level, pos);
            }
         }
      }
   }

   @SubscribeEvent
   public static void onRightClickBlock(RightClickBlock event) {
      Minecraft mc = Minecraft.getInstance();
      if (event.getEntity() == mc.player && event.getLevel() == mc.level) {
         if (event.getItemStack().getItem() instanceof BlockItem) {
            BlockHitResult hit = event.getHitVec();
            BlockPlaceContext placeContext = new BlockPlaceContext(
               new UseOnContext(event.getLevel(), event.getEntity(), event.getHand(), event.getItemStack(), hit)
            );
            BlockPos target = placeContext.getClickedPos();
            BlockState originalState = mc.level.getBlockState(target);
            if (originalState.canBeReplaced()) {
               placeCandidate = target.immutable();
               placeOriginalState = originalState;
               placeExpiresAt = clientTicks + 20;
            }
         }
      }
   }

   @SubscribeEvent
   public static void onExplosionSound(PlaySoundEvent event) {
      LocalPlayer player = Minecraft.getInstance().player;
      SoundInstance sound = event.getOriginalSound();
      if (player != null && sound != null) {
         Vec3 at = new Vec3(sound.getX(), sound.getY(), sound.getZ());
         if (ImpactConfig.triggerEnabled(ImpactType.FOOTSTEP.key)
            && sound.getLocation().getPath().endsWith(".step")
            && player.position().distanceToSqr(at) <= 4.0) {
            fire(ImpactType.FOOTSTEP, ImpactTier.LIGHT, 1.0F, null, at, 0.35F);
         }

         if (EXPLOSION_SOUND.equals(sound.getLocation())) {
            double distance = player.position().distanceTo(at);
            if (!(distance > 48.0)) {
               float scale = (float)Mth.clamp(1.0 - distance / 48.0, 0.35, 1.0);
               if (!ImpactConfig.useMinimumImpactThresholds() || !((double)scale < ImpactConfig.minimumExplosionStrength())) {
                  ImpactTier tier = scale >= 0.6F ? ImpactTier.HEAVY : ImpactTier.NORMAL;
                  fire(ImpactType.EXPLOSION, tier, scale, null, at, 2.5F);
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onRenderLevel(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_LEVEL) {
         syncFreezeHold();
         float strength = ImpactState.strength();
         if (!(strength <= 0.002F) && ImpactConfig.enabled()) {
            float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
            resolveFocus(event, partial);
            Matrix4f projection = event.getProjectionMatrix();
            float near = 0.05F;
            float far = 512.0F;
            float m22 = projection.m22();
            float m32 = projection.m32();
            if (Math.abs(m22 - 1.0F) > 1.0E-6F && Math.abs(m22 + 1.0F) > 1.0E-6F) {
               float n = m32 / (m22 - 1.0F);
               float f = m32 / (m22 + 1.0F);
               if (n > 0.005F && n < 2.0F && f > 8.0F && f < 8192.0F) {
                  near = n;
                  far = f;
               }
            }

            ImpactRenderer.render(
               strength,
               ImpactState.elapsedSeconds(),
               focusU,
               focusV,
               focusRadius,
               ImpactState.tier().ordinal(),
               ImpactState.invertNow(),
               near,
               far,
               partial,
               ImpactState.dirX(),
               ImpactState.dirY(),
               ImpactConfig.colorKeyed() && ImpactState.type() != null ? ImpactState.type().tint() : null,
               ImpactState.letterboxAmount()
            );
         }
      }
   }

   @SubscribeEvent
   public static void onRenderGui(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event) {
      ImpactType type = ImpactState.type();
      float strength = ImpactState.strength();
      ImpactTier tier = ImpactState.tier();
      if (type != null && !(strength <= 0.02F) && ImpactConfig.onomatopoeia() && ImpactConfig.enabled() && tier.ordinal() >= ImpactTier.NORMAL.ordinal()) {
         Minecraft mc = Minecraft.getInstance();
         GuiGraphics graphics = event.getGuiGraphics();
         int width = graphics.guiWidth();
         int height = graphics.guiHeight();
         boolean hasFocus = focusRadius > 0.0F;
         float x = (hasFocus ? focusU : 0.5F) * (float)width + ImpactState.textOffsetX() * (float)width;
         float y = (hasFocus ? 1.0F - focusV - focusRadius * 0.8F : 0.3F) * (float)height + ImpactState.textOffsetY() * (float)height;
         float pop = ImpactConfig.safeMode() ? 0.0F : Math.max(0.0F, 1.0F - ImpactState.elapsedSeconds() * 10.0F);
         int alpha = (int)(255.0F * Mth.clamp(strength * 2.5F, 0.0F, 1.0F));
         if (alpha >= 8) {
            float[] tint = type.tint();
            int rgb = tint != null && ImpactConfig.colorKeyed()
               ? (int)(tint[0] * 255.0F) << 16 | (int)(tint[1] * 255.0F) << 8 | (int)(tint[2] * 255.0F)
               : 16777215;
            Component text = Component.translatable(ImpactState.textKey()).withStyle(ChatFormatting.BOLD);
            float textWidth = (float)mc.font.width(text);
            float scale = (2.0F + (float)tier.ordinal() * 1.1F) * ImpactConfig.onomatopoeiaScale() * (1.0F + 0.45F * pop);
            float maxScale = Math.min((float)width * 0.62F / Math.max(textWidth, 1.0F), (float)height * 0.16F / (float)Math.max(9, 1));
            scale = Math.min(scale, Math.max(maxScale, 0.5F));
            float halfWidth = textWidth * scale * 0.55F + 4.0F;
            float halfHeight = 9.0F * scale * 0.75F + halfWidth * 0.12F + 4.0F;
            x = Mth.clamp(x, Math.min((float)width * 0.5F, halfWidth), Math.max((float)width * 0.5F, (float)width - halfWidth));
            y = Mth.clamp(y, Math.min((float)height * 0.5F, halfHeight), Math.max((float)height * 0.5F, (float)height - halfHeight));
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 400.0F);
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(-8.0F));
            graphics.pose().scale(scale, scale, 1.0F);
            int shadow = alpha << 24;
            int main = alpha << 24 | rgb;
            float left = -textWidth * 0.5F;

            for (int dx = -1; dx <= 1; dx++) {
               for (int dy = -1; dy <= 1; dy++) {
                  if (dx != 0 || dy != 0) {
                     graphics.drawString(mc.font, text, (int)(left + (float)dx), -4 + dy, shadow, false);
                  }
               }
            }

            graphics.drawString(mc.font, text, (int)left, -4, main, false);
            graphics.pose().popPose();
         }
      }
   }

   public static void preview(ImpactType type) {
      if (Minecraft.getInstance().level != null) {
         ImpactState.reset();
         resetTextFocus();
         ImpactTier tier = type.defaultTier;
         if (type == ImpactType.ATTACK || type == ImpactType.DAMAGE) {
            tier = ImpactTier.NORMAL;
         }

         if (ImpactState.trigger(type, tier, 1.0F, null, null, 0.5F)) {
            boolean bars = tier == ImpactTier.SMASH || type == ImpactType.EXPLOSION && tier.ordinal() >= ImpactTier.HEAVY.ordinal();
            ImpactState.setDecor(type == ImpactType.DAMAGE ? -1.0F : 1.0F, 0.0F, bars);
            pendingDirX = 0.0F;
            pendingDirY = 0.0F;
            ImpactAudio.play(tier, 1.0F);
         }
      }
   }

   @SubscribeEvent
   public static void onComputeCameraAngles(ComputeCameraAngles event) {
      syncFreezeHold();
      float strength = ImpactState.strength();
      if (!(strength <= 0.0F) && ImpactConfig.enabled() && !ImpactConfig.safeMode()) {
         ImpactTier tier = ImpactState.tier();
         float presetShake = 1.0F;
         if (ImpactConfig.preset() == ImpactPreset.NORMAL) {
            if (ImpactState.type() == ImpactType.KILL) {
               presetShake = 0.35F;
            } else if (tier.ordinal() < ImpactTier.HEAVY.ordinal()) {
               return;
            }
         }

         float envelope = strength * strength * tier.shake * presetShake;
         double t = (double)ImpactState.elapsedSeconds();
         float roll = (float)((Math.sin(t * 61.0) * 0.8 + Math.sin(t * 97.0 + 1.3) * 0.4) * (double)envelope);
         float pitch = (float)(Math.sin(t * 73.0 + 0.7) * 0.5 * (double)envelope);
         event.setRoll(event.getRoll() + roll);
         event.setPitch(event.getPitch() + pitch);
      }
   }

   @SubscribeEvent
   public static void onComputeFov(ComputeFov event) {
      syncFreezeHold();
      float strength = ImpactState.strength();
      if (!(strength <= 0.0F) && ImpactConfig.enabled()) {
         if (ImpactConfig.zoomEnabled()) {
            double depth = ImpactState.tier().fovPunch * ImpactConfig.zoomStrength() * (ImpactConfig.safeMode() ? 0.5 : 1.0);
            event.setFOV(event.getFOV() * (1.0 - Math.min(depth * (double)strength * (double)strength, 0.6)));
         }
      }
   }

   @SubscribeEvent
   public static void onLoggingOut(LoggingOut event) {
      FreezeLock.reset();
      ImpactState.reset();
      resetTracking();
   }

   private static void checkDamage(LocalPlayer player) {
      if (lastHealth < 0.0F) {
         lastHealth = player.getHealth();
      }

      int hurt = player.hurtTime;
      if (hurt > lastHurtTime && hurt > 0) {
         healthBeforeHit = lastHealth;
         damageEvalAt = clientTicks + 1;
      }

      lastHurtTime = hurt;
      if (damageEvalAt >= 0 && clientTicks >= damageEvalAt) {
         damageEvalAt = -1;
         float health = player.getHealth();
         float damage = healthBeforeHit - health;
         boolean big = damage >= 6.0F || health <= 6.0F;
         pendingDirX = (float)Math.cos(Math.toRadians((double)player.getHurtDir()));
         pendingDirY = 0.0F;
         if (Math.abs(pendingDirX) < 0.25F) {
            pendingDirX = 0.0F;
         }

         if (damage > 0.0F && (!ImpactConfig.useMinimumImpactThresholds() || (double)damage >= ImpactConfig.minimumDamage())) {
            fire(ImpactType.DAMAGE, big ? ImpactTier.HEAVY : ImpactTier.NORMAL, 1.0F, null, null, 0.5F, damage > 12.0F);
         }
      }

      lastHealth = player.getHealth();
   }

   private static void checkAttackDamage() {
      LivingEntity target = attackDamageCandidate;
      if (target != null) {
         if (attackServerFeedback) {
            if (clientTicks > attackDamageExpiresAt) {
               clearAttackDamageCandidate();
            }
         } else {
            float damage = attackHealthBefore - target.getHealth();
            if (attackRequiresMinimumDamage && !attackImpactEvaluated && damage > 0.0F && (double)damage >= ImpactConfig.minimumDamage()) {
               attackImpactEvaluated = true;
               if (fire(pendingAttackType, pendingAttackTier, pendingAttackScale, target)) {
                  attackImpactSerial = ImpactState.triggerSerial();
               }
            }

            if (clientTicks > attackDamageExpiresAt) {
               clearAttackDamageCandidate();
            }
         }
      }
   }

   public static void onHighDamageAttack(int targetId, float damage) {
      Entity target = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getEntity(targetId);
      if (attackDamageCandidate != null && attackDamageCandidate.getId() == targetId) {
         if (attackRequiresMinimumDamage && !attackImpactEvaluated && damage > 0.0F && (double)damage >= ImpactConfig.minimumDamage()) {
            attackImpactEvaluated = true;
            if (fire(pendingAttackType, pendingAttackTier, pendingAttackScale, attackDamageCandidate)) {
               attackImpactSerial = ImpactState.triggerSerial();
            }
         }

         if (damage > 15.0F) {
            if (attackImpactSerial >= 0L && ImpactState.addLetterboxToTrigger(attackImpactSerial)) {
               clearAttackDamageCandidate();
               return;
            }

            if (target == null) {
               target = attackDamageCandidate;
            }

            clearAttackDamageCandidate();
            fire(ImpactType.ATTACK, ImpactTier.HEAVY, 1.0F, target, null, 0.5F, true);
         } else {
            clearAttackDamageCandidate();
         }
      } else {
         if (damage > 15.0F) {
            fire(ImpactType.ATTACK, ImpactTier.HEAVY, 1.0F, target, null, 0.5F, true);
         }
      }
   }

   private static void clearAttackDamageCandidate() {
      attackDamageCandidate = null;
      attackImpactSerial = -1L;
      attackServerFeedback = false;
      attackRequiresMinimumDamage = false;
      attackImpactEvaluated = false;
      pendingAttackType = null;
      pendingAttackTier = null;
      pendingAttackScale = 0.0F;
   }

   private static void checkLanding(LocalPlayer player) {
      boolean onGround = player.onGround();
      if (!onGround) {
         peakFall = Math.max(peakFall, player.fallDistance);
      } else {
         if (!wasOnGround && peakFall > 0.0F && (double)peakFall >= ImpactConfig.minimumFallDistance() && !player.isInWater()) {
            ImpactTier tier = peakFall >= 10.0F ? ImpactTier.HEAVY : ImpactTier.NORMAL;
            fire(ImpactType.LAND, tier, Mth.clamp(peakFall / 8.0F, 0.6F, 1.0F));
         }

         peakFall = 0.0F;
      }

      wasOnGround = onGround;
   }

   private static void checkKill() {
      if (killCandidate != null) {
         if (killCandidate.getHealth() <= 0.0F) {
            boolean smash = killSmash || isBoss(killCandidate);
            fire(ImpactType.KILL, smash ? ImpactTier.SMASH : ImpactTier.HEAVY, 1.0F, killCandidate);
            killCandidate = null;
         } else if (clientTicks > killExpiresAt) {
            killCandidate = null;
         }
      }
   }

   private static void checkBlockBreak(Level level) {
      if (breakCandidate != null) {
         if (level.getBlockState(breakCandidate).isAir()) {
            ImpactTier tier = breakHardness >= 20.0F ? ImpactTier.HEAVY : (breakHardness >= 5.0F ? ImpactTier.NORMAL : ImpactTier.LIGHT);
            float scale = Mth.clamp(0.6F + breakHardness * 0.1F, 0.6F, 1.0F);
            if (!ImpactConfig.useMinimumImpactThresholds() || (double)breakHardness >= ImpactConfig.minimumBlockHardness()) {
               fire(ImpactType.BLOCK_BREAK, tier, scale, null, Vec3.atCenterOf(breakCandidate), 0.6F);
            }

            breakCandidate = null;
         } else if (clientTicks > breakExpiresAt) {
            breakCandidate = null;
         }
      }
   }

   private static void checkBlockPlace(Level level) {
      if (placeCandidate != null) {
         if (!level.getBlockState(placeCandidate).equals(placeOriginalState)) {
            fire(ImpactType.BLOCK_PLACE, ImpactTier.LIGHT, 1.0F, null, Vec3.atCenterOf(placeCandidate), 0.6F);
            placeCandidate = null;
            placeOriginalState = null;
         } else if (clientTicks > placeExpiresAt) {
            placeCandidate = null;
            placeOriginalState = null;
         }
      }
   }

   private static boolean isBoss(LivingEntity entity) {
      EntityType<?> type = entity.getType();
      return type.is(EntityTypes.BOSSES) || type == EntityType.WARDEN || type == EntityType.ELDER_GUARDIAN;
   }

   private static boolean fire(ImpactType type, ImpactTier tier, float scale) {
      return fire(type, tier, scale, null, null, 0.5F);
   }

   private static boolean fire(ImpactType type, ImpactTier tier, float scale, Entity focus) {
      return fire(type, tier, scale, focus, null, 0.5F);
   }

   private static boolean fire(ImpactType type, ImpactTier tier, float scale, Entity focus, Vec3 pos, float half) {
      return fire(type, tier, scale, focus, pos, half, false);
   }

   private static boolean fire(ImpactType type, ImpactTier tier, float scale, Entity focus, Vec3 pos, float half, boolean cinematicBars) {
      if (ImpactConfig.enabled() && ImpactConfig.triggerEnabled(type.key)) {
         ImpactTier eventTier = tier;
         ImpactPreset preset = ImpactConfig.preset();
         boolean advanced = preset == ImpactPreset.ADVANCED;
         if (preset == ImpactPreset.RIDICULOUS) {
            tier = ImpactTier.SMASH;
            scale = 1.0F;
         } else if (advanced) {
            tier = ImpactTier.SMASH;
            scale = 1.0F;
         }

         if (!ImpactState.trigger(type, tier, scale, focus, pos, half)) {
            pendingDirX = 0.0F;
            pendingDirY = 0.0F;
            return false;
         } else {
            resetTextFocus();
            boolean bigExplosion = type == ImpactType.EXPLOSION && eventTier.ordinal() >= ImpactTier.HEAVY.ordinal();
            boolean smash = eventTier == ImpactTier.SMASH && (type == ImpactType.KILL || type == ImpactType.MACE);
            ImpactState.setDecor(pendingDirX, pendingDirY, cinematicBars || bigExplosion || smash);
            pendingDirX = 0.0F;
            pendingDirY = 0.0F;
            ImpactAudio.play(tier, advanced ? (float)ImpactConfig.triggerIntensity(type) : scale);
            freezeArmed = false;
            freezeSeen = false;
            requestServerFreeze(eventTier);
            return true;
         }
      } else {
         pendingDirX = 0.0F;
         pendingDirY = 0.0F;
         return false;
      }
   }

   private static void requestServerFreeze(ImpactTier eventTier) {
      if (ImpactConfig.serverFeatures() && ImpactConfig.freezeOnImpact() && eventTier.ordinal() >= ImpactTier.HEAVY.ordinal()) {
         ClientPacketListener connection = Minecraft.getInstance().getConnection();
         if (connection != null && connection.hasChannel(FreezeRequestPayload.TYPE)) {
            Level level = Minecraft.getInstance().level;
            boolean alreadyFrozen = level != null && level.tickRateManager().isFrozen();
            PacketDistributor.sendToServer(new FreezeRequestPayload(ImpactConfig.freezeTicks()), new CustomPacketPayload[0]);
            if (!alreadyFrozen) {
               freezeArmed = true;
               freezeArmedUntil = clientTicks + 10;
            }
         }
      }
   }

   private static void syncFreezeHold() {
      Level level = Minecraft.getInstance().level;
      boolean frozen = level != null && level.tickRateManager().isFrozen();
      if (freezeArmed) {
         if (frozen) {
            freezeSeen = true;
         } else if (freezeSeen || clientTicks > freezeArmedUntil) {
            freezeArmed = false;
            freezeSeen = false;
         }
      }

      ImpactState.setFreezeHold(freezeArmed && frozen);
   }

   private static void resolveFocus(RenderLevelStageEvent event, float partial) {
      focusU = 0.5F;
      focusV = 0.5F;
      focusRadius = 0.0F;
      Entity entity = ImpactState.focusEntity();
      Vec3 pos = ImpactState.focusPos();
      float half = ImpactState.focusHalf();
      if (entity != null && !entity.isRemoved()) {
         pos = new Vec3(
            Mth.lerp((double)partial, entity.xo, entity.getX()),
            Mth.lerp((double)partial, entity.yo, entity.getY()) + (double)entity.getBbHeight() * 0.5,
            Mth.lerp((double)partial, entity.zo, entity.getZ())
         );
         half = Math.max(entity.getBbHeight(), entity.getBbWidth()) * 0.5F;
         ImpactState.updateFocus(pos, half);
      }

      if (pos != null) {
         Vec3 camera = event.getCamera().getPosition();
         Vector4f clip = new Vector4f((float)(pos.x - camera.x), (float)(pos.y - camera.y), (float)(pos.z - camera.z), 1.0F);
         event.getModelViewMatrix().transform(clip);
         event.getProjectionMatrix().transform(clip);
         if (!(clip.w <= 0.05F)) {
            float ndcX = clip.x / clip.w;
            float ndcY = clip.y / clip.w;
            if (!(Math.abs(ndcX) > 1.4F) && !(Math.abs(ndcY) > 1.4F)) {
               focusU = Mth.clamp(ndcX * 0.5F + 0.5F, 0.0F, 1.0F);
               focusV = Mth.clamp(ndcY * 0.5F + 0.5F, 0.0F, 1.0F);
               float radius = half * event.getProjectionMatrix().m11() / clip.w * 0.5F;
               focusRadius = Mth.clamp(radius, 0.06F, 0.6F);
            }
         }
      }
   }

   private static void trackSwing(LocalPlayer player) {
      float yaw = player.getYRot();
      float pitch = player.getXRot();
      if (swingReady) {
         swingX = swingX * 0.6F + Mth.wrapDegrees(yaw - lastYaw) * 0.4F;
         swingY = swingY * 0.6F - (pitch - lastPitch) * 0.4F;
      }

      lastYaw = yaw;
      lastPitch = pitch;
      swingReady = true;
   }

   private static void setSwingDirection() {
      if (Math.abs(swingX) + Math.abs(swingY) > 0.5F) {
         pendingDirX = swingX;
         pendingDirY = swingY;
      } else {
         pendingDirX = 0.0F;
         pendingDirY = 0.0F;
      }
   }

   private static void resetTextFocus() {
      focusU = 0.5F;
      focusV = 0.5F;
      focusRadius = 0.0F;
   }

   private static void resetTracking() {
      swingReady = false;
      swingX = 0.0F;
      swingY = 0.0F;
      pendingDirX = 0.0F;
      pendingDirY = 0.0F;
      resetTextFocus();
      killCandidate = null;
      clearAttackDamageCandidate();
      breakCandidate = null;
      placeCandidate = null;
      placeOriginalState = null;
      freezeArmed = false;
      freezeSeen = false;
      lastHurtTime = 0;
      lastHealth = -1.0F;
      damageEvalAt = -1;
      wasOnGround = true;
      peakFall = 0.0F;
   }

   @EventBusSubscriber(
      modid = "impactframe",
      value = {Dist.CLIENT},
      bus = Bus.MOD
   )
   public static final class ModBusEvents {
      private static final KeyMapping OPEN_SETTINGS = new KeyMapping("key.impactframe.open_settings", Type.KEYSYM, 74, "key.categories.misc");

      private ModBusEvents() {
      }

      @SubscribeEvent
      public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
         event.register(OPEN_SETTINGS);
      }

      @SubscribeEvent
      public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
         ResourceManagerReloadListener listener = manager -> ImpactRenderer.invalidate();
         event.registerReloadListener(listener);
      }
   }
}
