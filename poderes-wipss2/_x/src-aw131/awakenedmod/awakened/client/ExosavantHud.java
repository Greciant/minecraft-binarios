package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.exosavant.ArtilleryBarrageAbility;
import com.awakenedmod.awakened.content.exosavant.ExoPiece;
import com.awakenedmod.awakened.content.exosavant.ExoTags;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.ExoVisual;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import com.awakenedmod.awakened.mixin.GameRendererFovAccessor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw.Layer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import org.joml.Vector3fc;

public final class ExosavantHud implements Layer {
   private static final int STEEL_RGB = 13166335;
   private static final int LOCK_RGB = 16734810;
   private static final int SCAN_RED_RGB = 14692909;
   private static final int TAG_THRU_RGB = 16754234;
   private static final int WARN_RGB = 16728128;
   private static final int HP_OK_RGB = 6356842;
   private static final int HP_WARN_RGB = 16757066;
   private static final int ABSORB_RGB = 16773064;
   private static final int POWER_RGB = 5228799;
   private static final int LOWPOWER_RGB = 16747082;
   private static final int ARMOR_RGB = 12571360;
   private static final int READY_RGB = 4646568;
   private static final int FRAME_MARGIN = 14;
   private static final int FRAME_BRACKET = 20;
   private static final double STREAK_FLOOR_BPT = 0.9;
   private static final int LABEL_SEG_W = 24;
   private static final int GAUGE_W = 96;
   private static final int GAUGE_H = 5;
   private static final int GAUGE_CELL = 16;
   private static final int ROW_PITCH = 13;
   private static final int VALUE_GAP = 5;
   private static final int VALUE_COL_W = 22;
   private static final int LED_SIZE = 4;
   private static final int CARET_W = 2;
   private static final int SIL_W = 24;
   private static final int SIL_H = 40;
   private static final int CLUSTER_PAD = 4;
   private static final int CLUSTER_GAUGE_U = 28;
   private static final int CLUSTER_VALUE_U = 151;
   private static final int CLUSTER_TOUGH_U = 155;
   private static final int CLUSTER_TOUGH_COL_W = 22;
   private static final int CLUSTER_STACK_U = 37;
   private static final int CLUSTER_W = 173;
   private static final int CLUSTER_ROWS_TOP = 4;
   private static final int CLUSTER_BLOCK_TOP = 51;
   private static final int CLUSTER_STACK_ROWS = 4;
   private static final int CLUSTER_H = 107;
   private static final int CLUSTER_INSET = 12;
   private static final int FLIGHT_INSET_X = 16;
   private static final int FLIGHT_BADGE_OVERHANG = 2;
   private static final int FLIGHT_INSET_Y = 14;
   private static final int FLIGHT_BAR_W = 64;
   private static final int FLIGHT_W = 84;
   private static final int FLIGHT_FUEL_BAR_BOT = 48;
   private static final int FLIGHT_HOVER_EXTENT = 49;
   private static final int FLIGHT_JET_EXTENT = 78;
   private static final int FLIGHT_H = 80;
   private static final int DOSSIER_PANEL_W = 132;
   private static final int DOSSIER_PAD = 6;
   private static final int DOSSIER_BLEED = 3;
   private static final int DOSSIER_W = 138;
   private static final int DOSSIER_STATUS_H = 10;
   private static final int DOSSIER_ROWS_H = 80;
   private static final int DOSSIER_H = 95;
   private static final int DOSSIER_INSET_X = 15;
   private static final int DOSSIER_INSET_Y = 21;
   private static final long BOOT_MS = 1200L;
   private static final long COLLAPSE_MS = 350L;
   private static final long DAMAGE_FLASH_MS = 320L;
   private static final long FAILSAFE_FLASH_MS = 1600L;
   private static final long FAILSAFE_COOLDOWN_MS = 90000L;
   private static final float LOW_HP_FRACTION = 0.3F;
   private static final int PRIMARY_MIN_HALF_W = 9;
   private static final int PRIMARY_MIN_HALF_H = 12;
   private static final int LABEL_GAP = 6;
   private static final double AR_SCAN_RADIUS = 28.0;
   private static final double AR_RANGE = 28.0;
   private static final double AR_NAME_DIST = 24.0;
   private static final double AR_FADE_START = 14.0;
   private static final int AR_MAX_TAGS = 10;
   private static final int AR_BRACKET_ALPHA = 205;
   private static final int AR_NAME_ALPHA = 220;
   private static final int AR_HP_ALPHA = 200;
   private static final double THREAT_SCAN_RADIUS = 24.0;
   private static final int THREAT_PROCESS_CAP = 96;
   private static final int MAX_THREATS = 8;
   private static final int MAX_THREAT_LINES = 4;
   private static final int THREAT_ARROW_CAP = 6;
   private static final double PROJ_MIN_SPEED = 0.3;
   private static final double PROJ_MAX_DIST = 32.0;
   private static final double PROJ_APPROACH_COS = 0.5;
   private static final double PROJ_MAX_TTI = 2.5;
   private static final int O2_LOW_THRESHOLD = 60;
   private static final int THREAT_LIST_TOP = 34;
   private static final int THREAT_LINE_PITCH = 11;
   private static final int THREAT_ICON = 8;
   private static final int THREAT_EDGE_MARGIN = 24;
   private static final int THREAT_ARROW_H = 9;
   private static final int THREAT_ARROW_W = 5;
   private static final int PLATE_RGB = 329740;
   private Player lastPlayer;
   private long settleUntilMs;
   private boolean lastSuited;
   private boolean lastFailsafeReady;
   private float lastHealth = -1.0F;
   private long bootStartMs;
   private long collapseStartMs = -350L;
   private long damageFlashUntil;
   private long failsafeFlashUntil;
   private long failsafeWentBusyAt;
   private final float[] proj = new float[3];
   private final Set<Integer> arTagged = new HashSet<>();
   public static final ExosavantHud.VisorPanel STATUS_CLUSTER = new ExosavantHud.VisorPanel(
      "exo_status_cluster", 173, 107, 12, 12, ClientConfig.EXO_STATUS_CLUSTER, ClientConfig.EXO_STATUS_CLUSTER_CORNER, ExoCorner.TOP_LEFT
   ) {
      @Override
      protected void drawStandIn(GuiGraphics g, HudPlacement.Box box, ExoCorner corner) {
         Minecraft mc = Minecraft.getInstance();
         Player player = mc.player;
         if (player != null) {
            ExosavantHud.drawStatusCluster(
               g,
               mc.font,
               box,
               corner,
               ClientPowerState.get(),
               (ExoVisual)player.getData(AwakenedAttachments.EXO_VISUAL),
               player,
               ExosavantHud.selectedAbility(),
               ExosavantHud.FailsafeReadout.sample(),
               0.0F,
               ExosavantHud.standInTheme(),
               1.0F
            );
         }
      }
   };
   public static final ExosavantHud.VisorPanel TARGET_DOSSIER = new ExosavantHud.VisorPanel(
      "exo_target_dossier", 138, 95, 15, 21, ClientConfig.EXO_TARGET_DOSSIER, ClientConfig.EXO_TARGET_DOSSIER_CORNER, ExoCorner.BOTTOM_RIGHT
   ) {
      @Override
      protected void drawStandIn(GuiGraphics g, HudPlacement.Box box, ExoCorner corner) {
         ExosavantHud.drawDossier(g, Minecraft.getInstance().font, box, corner, ExosavantHud.Dossier.sample(), ExosavantHud.standInTheme(), 1.0F);
      }
   };
   public static final ExosavantHud.VisorPanel FLIGHT = new ExosavantHud.VisorPanel(
      "exo_flight", 84, 80, 16, 14, ClientConfig.EXO_FLIGHT_INSTRUMENTS, ClientConfig.EXO_FLIGHT_INSTRUMENTS_CORNER, ExoCorner.TOP_RIGHT
   ) {
      @Override
      protected void drawStandIn(GuiGraphics g, HudPlacement.Box box, ExoCorner corner) {
         Minecraft mc = Minecraft.getInstance();
         Player player = mc.player;
         if (player != null) {
            ExosavantHud.drawFlightColumn(
               g, mc.font, box, corner, ExosavantHud.FlightReadout.sample(ClientPowerState.get(), player), ExosavantHud.standInTheme()
            );
         }
      }
   };
   private static final int SIL_RECESS_RGB = 660510;
   private static final String[] CARDINALS = new String[]{"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
   private static final String[] BOOT_MODULES = new String[]{"OPTICS", "WEAPONS", "ARMOR", "FLIGHT", "CONTROL"};

   public void render(GuiGraphics g, DeltaTracker deltaTracker) {
      Minecraft mc = Minecraft.getInstance();
      Player player = mc.player;
      if (player != null && mc.level != null && !mc.options.hideGui) {
         ClientPowerState state = ClientPowerState.get();
         if ("exosavant".equals(state.powerId())) {
            Power power = PowerRegistry.get("exosavant");
            if (power != null) {
               ExoVisual vis = (ExoVisual)player.getData(AwakenedAttachments.EXO_VISUAL);
               long now = nowMs();
               boolean suited = vis.suited() && !vis.visorOpen() && (vis.builtMask() & ExoPiece.HELMET.bit()) != 0;
               if (player != this.lastPlayer) {
                  this.lastPlayer = player;
                  this.lastSuited = suited;
                  this.lastFailsafeReady = vis.failsafeReady();
                  this.lastHealth = suited ? player.getHealth() : -1.0F;
                  this.bootStartMs = suited ? now - 1200L : now;
                  this.collapseStartMs = -350L;
                  this.damageFlashUntil = 0L;
                  this.failsafeFlashUntil = 0L;
                  this.settleUntilMs = now + 500L;
               }

               if (suited && !this.lastSuited) {
                  this.bootStartMs = now < this.settleUntilMs ? now - 1200L : now;
                  this.lastHealth = player.getHealth();
               } else if (!suited && this.lastSuited && now >= this.settleUntilMs) {
                  this.collapseStartMs = now;
               }

               boolean fsReady = vis.failsafeReady();
               if (vis.failsafeArmed() && this.lastFailsafeReady && !fsReady) {
                  this.failsafeFlashUntil = now + 1600L;
                  this.failsafeWentBusyAt = now;
               }

               this.lastFailsafeReady = fsReady;
               if (suited) {
                  float hp = player.getHealth();
                  if (this.lastHealth >= 0.0F && hp < this.lastHealth - 0.01F) {
                     this.damageFlashUntil = now + 320L;
                  }

                  this.lastHealth = hp;
               } else {
                  this.lastHealth = -1.0F;
               }

               this.lastSuited = suited;
               int theme = power.themeColor();
               float intro = suited ? clamp01((float)(now - this.bootStartMs) / 1200.0F) : 1.0F - clamp01((float)(now - this.collapseStartMs) / 350.0F);
               if (!(intro <= 0.002F)) {
                  if (mc.options.getCameraType().isFirstPerson()) {
                     float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
                     int sw = g.guiWidth();
                     int sh = g.guiHeight();
                     int cx = sw / 2;
                     int cy = sh / 2;
                     Font font = mc.font;
                     float lowHp = lowHpSeverity(player);
                     float dmgFlash = this.damageFlashUntil > now ? (float)(this.damageFlashUntil - now) / 320.0F : 0.0F;
                     this.drawDamageFeedback(g, sw, sh, now, dmgFlash, lowHp, intro);
                     boolean glitching = dmgFlash > 0.0F;
                     this.drawFrame(g, sw, sh, intro, theme, glitching, now);
                     this.drawHeading(g, font, player, cx, intro, theme, partialTick);
                     LivingEntity primary = null;
                     if (suited && intro > 0.6F) {
                        if (!vis.taggedIds().isEmpty()) {
                           primary = computePrimary(mc, player, vis, partialTick);
                        }

                        this.arTagged.clear();
                        if ((vis.builtMask() & ExoPiece.HELMET.bit()) != 0) {
                           this.drawArTags(g, mc, font, player, primary, partialTick, sw, sh, theme, intro, this.arTagged);
                        }

                        if (!vis.taggedIds().isEmpty()) {
                           this.drawTargets(g, mc, font, player, vis, primary, partialTick, sw, sh, theme, this.arTagged);
                           if (primary != null && TARGET_DOSSIER.hudVisible()) {
                              drawDossier(
                                 g, font, TARGET_DOSSIER.layout(sw, sh), TARGET_DOSSIER.corner(), ExosavantHud.Dossier.of(player, primary), theme, intro
                              );
                           }
                        }
                     }

                     Ability selected = power.ability(state.activeSlot());
                     if (suited && intro > 0.6F && selected instanceof ArtilleryBarrageAbility) {
                        this.drawBarragePreview(g, mc, font, player, partialTick, sw, sh, theme);
                     }

                     int reticleMode = primary != null ? 2 : (vis.analyzing() ? 1 : 0);
                     this.drawReticle(g, crosshairCentre(sw), crosshairCentre(sh), reticleMode, now, intro, theme);
                     if (suited && intro > 0.6F) {
                        this.drawThreatWarnings(g, mc, font, player, partialTick, sw, sh, cx, cy, intro);
                     }

                     if (STATUS_CLUSTER.hudVisible()) {
                        drawStatusCluster(
                           g,
                           font,
                           STATUS_CLUSTER.layout(sw, sh),
                           STATUS_CLUSTER.corner(),
                           state,
                           vis,
                           player,
                           selected,
                           ExosavantHud.FailsafeReadout.of(
                              vis, this.failsafeFlashUntil > now, clamp01(1.0F - (float)(now - this.failsafeWentBusyAt) / 90000.0F)
                           ),
                           dmgFlash,
                           theme,
                           intro
                        );
                     }

                     if (vis.flying() && intro > 0.5F && FLIGHT.enabled()) {
                        this.drawFlightInstruments(
                           g, font, state, player, cx, cy, sw, sh, vis.jet(), vis.afterburner(), vis.flightSpeed(), vis.boostCd(), theme
                        );
                     }

                     this.drawHeadlines(g, font, cx, cy, sw, sh, now, intro, suited, lowHp, theme, vis.builtMask(), vis.chipInstalled());
                  }
               }
            }
         }
      }
   }

   private void drawFrame(GuiGraphics g, int sw, int sh, float intro, int theme, boolean glitch, long now) {
      int a = (int)(70.0F * intro);
      int col = argb(glitch ? 16728128 : theme, Math.min(110, a + (glitch ? 40 : 0)));
      int len = Math.round(20.0F * intro);
      int m = 14;
      corner(g, m, m, 1, 1, len, col);
      corner(g, sw - m, m, -1, 1, len, col);
      corner(g, m, sh - m, 1, -1, len, col);
      corner(g, sw - m, sh - m, -1, -1, len, col);
      int edgeA = (int)(36.0F * intro);
      int half = Math.round((float)sw * 0.18F * intro);
      int edge = argb(theme, edgeA);
      g.fill(sw / 2 - half, m, sw / 2 + half, m + 1, edge);
      g.fill(sw / 2 - half, sh - m - 1, sw / 2 + half, sh - m, edge);
      int vHalf = Math.round((float)sh * 0.16F * intro);
      g.fill(m, sh / 2 - vHalf, m + 1, sh / 2 + vHalf, edge);
      g.fill(sw - m - 1, sh / 2 - vHalf, sw - m, sh / 2 + vHalf, edge);
   }

   private static void corner(GuiGraphics g, int x, int y, int sx, int sy, int len, int color) {
      int x0 = sx > 0 ? x : x - len;
      int x1 = sx > 0 ? x + len : x;
      g.fill(x0, y, x1, y + 2, color);
      int y0 = sy > 0 ? y : y - len;
      int y1 = sy > 0 ? y + len : y;
      g.fill(x, y0, x + 2, y1, color);
   }

   private void drawReticle(GuiGraphics g, int cx, int cy, int mode, long now, float intro, int theme) {
      float t = (float)(now % 100000L) / 1000.0F;
      if (mode == 2) {
         int col = argb(16734810, (int)(230.0F * intro));
         int r = 7;
         tick(g, cx - r, cy - r, 1, 1, 4, col);
         tick(g, cx + r, cy - r, -1, 1, 4, col);
         tick(g, cx - r, cy + r, 1, -1, 4, col);
         tick(g, cx + r, cy + r, -1, -1, 4, col);
         g.fill(cx - 1, cy - 1, cx + 2, cy + 2, col);
      } else if (mode == 1) {
         int col = argb(theme, (int)(210.0F * intro));
         float base = t * 3.2F;
         int r = 9;

         for (int i = 0; i < 8; i++) {
            double ang = (double)base + (double)i * (Math.PI / 4);
            int px = cx + (int)Math.round(Math.cos(ang) * (double)r);
            int py = cy + (int)Math.round(Math.sin(ang) * (double)r);
            g.fill(px - 1, py - 1, px + 1, py + 1, col);
         }

         g.fill(cx, cy, cx + 1, cy + 1, col);
      } else {
         int col = argb(theme, (int)(150.0F * intro));
         int r = 5 + (int)Math.round(2.0 * (1.0 + Math.sin((double)t * 2.0)));
         g.fill(cx - r - 4, cy, cx - r, cy + 1, col);
         g.fill(cx + r + 1, cy, cx + r + 5, cy + 1, col);
         g.fill(cx, cy - r - 4, cx + 1, cy - r, col);
         g.fill(cx, cy + r + 1, cx + 1, cy + r + 5, col);
         g.fill(cx, cy, cx + 1, cy + 1, argb(theme, (int)(90.0F * intro)));
      }
   }

   private static int crosshairCentre(int size) {
      return (size - 15) / 2 + 7;
   }

   private static void tick(GuiGraphics g, int x, int y, int sx, int sy, int len, int color) {
      int x0 = sx > 0 ? x : x - len;
      int x1 = sx > 0 ? x + len : x;
      g.fill(x0, y, x1, y + 1, color);
      int y0 = sy > 0 ? y : y - len;
      int y1 = sy > 0 ? y + len : y;
      g.fill(x, y0, x + 1, y1, color);
   }

   private void drawTargets(
      GuiGraphics g,
      Minecraft mc,
      Font font,
      Player player,
      ExoVisual vis,
      LivingEntity primary,
      float partialTick,
      int sw,
      int sh,
      int theme,
      Set<Integer> arTagged
   ) {
      ExosavantHud.CamBasis cam = basis(mc, sw, sh, partialTick);

      for (int id : vis.taggedIds()) {
         Entity e = mc.level.getEntity(id);
         if (e instanceof LivingEntity) {
            LivingEntity foe = (LivingEntity)e;
            if (foe.isAlive() && foe != player) {
               Vec3 center = foe.getPosition(partialTick).add(0.0, (double)foe.getBbHeight() * 0.5, 0.0);
               if (project(center, cam.pos, cam.fwd, cam.up, cam.left, cam.tanHalfV, cam.aspect, sw, sh, this.proj)) {
                  float scrX = this.proj[0];
                  float scrY = this.proj[1];
                  float depth = this.proj[2];
                  if (!(scrX < -40.0F) && !(scrX > (float)(sw + 40)) && !(scrY < -40.0F) && !(scrY > (float)(sh + 40))) {
                     float pxPerWorld = (float)sh * 0.5F / (cam.tanHalfV * depth);
                     int halfH = (int)Math.max(6.0F, foe.getBbHeight() * 0.5F * pxPerWorld);
                     int halfW = (int)Math.max(5.0F, foe.getBbWidth() * 0.5F * pxPerWorld);
                     int ix = Math.round(scrX);
                     int iy = Math.round(scrY);
                     double dist = Math.sqrt(foe.distanceToSqr(player));
                     if (foe == primary) {
                        this.drawPrimaryBracket(g, font, ix, iy, halfW, halfH, foe, dist, theme);
                     } else if (RevealClient.isRevealed(foe)) {
                        boolean occluded = occludedByBlock(mc, player, cam.pos, center);
                        this.drawTaggedDiamond(g, font, ix, iy - halfH - 6, occluded, dist);
                     } else if (!arTagged.contains(id)) {
                        boolean occluded = occludedByBlock(mc, player, cam.pos, center);
                        this.drawContactDiamond(g, font, ix, iy - halfH - 6, theme, occluded, dist);
                     }
                  }
               }
            }
         }
      }
   }

   private void drawPrimaryBracket(GuiGraphics g, Font font, int cx, int cy, int halfW, int halfH, LivingEntity foe, double dist, int theme) {
      int col = argb(16734810, 235);
      halfW = Math.max(halfW, 9);
      halfH = Math.max(halfH, 12);
      int x0 = cx - halfW;
      int x1 = cx + halfW;
      int y0 = cy - halfH;
      int y1 = cy + halfH;
      int arm = Math.max(4, Math.min(halfW, halfH) / 2);
      tick(g, x0, y0, 1, 1, arm, col);
      tick(g, x1, y0, -1, 1, arm, col);
      tick(g, x0, y1, 1, -1, arm, col);
      tick(g, x1, y1, -1, -1, arm, col);
      float hpFrac = clamp01(foe.getHealth() / Math.max(1.0F, foe.getMaxHealth()));
      int by = y0 - 5;
      g.fill(x0 - 1, by - 1, x1 + 1, by + 2, argb(0, 160));
      g.fill(x0, by, x0 + Math.round((float)(x1 - x0) * hpFrac), by + 1, argb(hpFrac > 0.3F ? 6356842 : 16734810, 235));
      String distStr = (int)dist + "m";
      float ls = 0.6F;
      int distW = Math.round((float)font.width(distStr) * ls);
      int lockW = Math.round((float)font.width("LOCK") * ls);
      int needHalf = (distW + 6 + lockW + 1) / 2;
      int slotHalf = Math.max(halfW, needHalf);
      int ly = y1 + 3;
      backedString(g, font, distStr, (float)(cx - slotHalf), (float)ly, ls, argb(theme, 230));
      backedString(g, font, "LOCK", (float)(cx + slotHalf - lockW), (float)ly, ls, col);
      int pulse = (int)(160.0 + 80.0 * Math.sin((double)nowMs() / 120.0));
      diamond(g, cx, y0 + Math.max(3, halfH / 6), 3, argb(16734810, Math.min(255, pulse)));
   }

   private void drawTaggedDiamond(GuiGraphics g, Font font, int cx, int cy, boolean occluded, double dist) {
      int r = 5;
      diamond(g, cx, cy, r + 1, argb(329740, 170));
      int col = argb(14692909, occluded ? 225 : 230);
      if (occluded) {
         hollowDiamond(g, cx, cy, r, col);
         g.fill(cx, cy, cx + 1, cy + 1, col);
      } else {
         diamond(g, cx, cy, r, col);
         g.fill(cx, cy - 1, cx + 1, cy + 1, argb(16777215, 220));
      }

      backedCentered(g, font, (int)dist + "m", cx, cy + r + 3, 0.5F, argb(14692909, 205));
   }

   private void drawContactDiamond(GuiGraphics g, Font font, int cx, int cy, int theme, boolean occluded, double dist) {
      int r = 5;
      diamond(g, cx, cy, r + 1, argb(329740, 170));
      if (occluded) {
         int col = argb(16754234, 225);
         hollowDiamond(g, cx, cy, r, col);
         g.fill(cx, cy, cx + 1, cy + 1, col);
      } else {
         diamond(g, cx, cy, r, argb(theme, 220));
         g.fill(cx, cy - 1, cx + 1, cy + 1, argb(16777215, 220));
      }

      backedCentered(g, font, (int)dist + "m", cx, cy + r + 3, 0.5F, argb(occluded ? 16754234 : theme, 205));
   }

   private void drawArTags(
      GuiGraphics g, Minecraft mc, Font font, Player player, LivingEntity primary, float partialTick, int sw, int sh, int theme, float intro, Set<Integer> out
   ) {
      AABB scan = player.getBoundingBox().inflate(28.0);
      List<LivingEntity> foes = mc.level.getEntitiesOfClass(LivingEntity.class, scan, e -> e instanceof Enemy && e.isAlive() && e != player);
      if (!foes.isEmpty()) {
         foes.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
         ExosavantHud.CamBasis cam = basis(mc, sw, sh, partialTick);
         Vec3 eyePos = player.getEyePosition(partialTick);
         int drawn = 0;

         for (LivingEntity foe : foes) {
            if (drawn >= 10) {
               break;
            }

            if (foe != primary && !RevealClient.isRevealed(foe)) {
               Vec3 center = foe.getPosition(partialTick).add(0.0, (double)foe.getBbHeight() * 0.5, 0.0);
               if (project(center, cam.pos, cam.fwd, cam.up, cam.left, cam.tanHalfV, cam.aspect, sw, sh, this.proj)) {
                  float scrX = this.proj[0];
                  float scrY = this.proj[1];
                  float depth = this.proj[2];
                  if (!(scrX < 0.0F) && !(scrX > (float)sw) && !(scrY < 0.0F) && !(scrY > (float)sh) && !occludedByBlock(mc, player, eyePos, center)) {
                     float pxPerWorld = (float)sh * 0.5F / (cam.tanHalfV * depth);
                     int halfH = (int)Math.max(5.0F, foe.getBbHeight() * 0.5F * pxPerWorld);
                     int halfW = (int)Math.max(4.0F, foe.getBbWidth() * 0.5F * pxPerWorld);
                     double dist = Math.sqrt(foe.distanceToSqr(player));
                     this.drawArTag(g, font, Math.round(scrX), Math.round(scrY), halfW, halfH, foe, dist, theme, intro);
                     out.add(foe.getId());
                     drawn++;
                  }
               }
            }
         }
      }
   }

   private static boolean occludedByBlock(Minecraft mc, Player player, Vec3 eye, Vec3 target) {
      HitResult hit = mc.level.clip(new ClipContext(eye, target, Block.COLLIDER, Fluid.NONE, player));
      return hit.getType() == Type.BLOCK && hit.getLocation().distanceToSqr(eye) < target.distanceToSqr(eye);
   }

   private void drawArTag(GuiGraphics g, Font font, int cx, int cy, int halfW, int halfH, LivingEntity foe, double dist, int theme, float intro) {
      float distFade = clamp01((float)((28.0 - dist) / 14.0));
      if (!(distFade <= 0.02F)) {
         int bracketRgb = threatRating(foe) >= 4 ? lerpColor(theme, 16734810, 0.4F) : theme;
         int col = argb(bracketRgb, fade((int)(205.0F * distFade), intro));
         int x0 = cx - halfW;
         int x1 = cx + halfW;
         int y0 = cy - halfH;
         int y1 = cy + halfH;
         int arm = Math.max(2, Math.min(halfW, halfH) / 3);
         tick(g, x0, y0, 1, 1, arm, col);
         tick(g, x1, y0, -1, 1, arm, col);
         tick(g, x0, y1, 1, -1, arm, col);
         tick(g, x1, y1, -1, -1, arm, col);
         float hpFrac = clamp01(foe.getHealth() / Math.max(1.0F, foe.getMaxHealth()));
         int by = y0 - 4;
         g.fill(x0 - 1, by - 1, x1 + 1, by + 2, argb(0, fade((int)(140.0F * distFade), intro)));
         int hpw = Math.round((float)(x1 - x0) * hpFrac);
         if (hpw > 0) {
            g.fill(x0, by, x0 + hpw, by + 1, argb(hpBandColor(hpFrac), fade((int)(200.0F * distFade), intro)));
         }

         if (dist <= 24.0) {
            String name = foe.getDisplayName().getString().toUpperCase(Locale.ROOT);
            float ns = 0.6F;
            String shown = fit(font, name, ns, Math.max(40, halfW * 2 + 30));
            backedCentered(g, font, shown, cx, by - 9, ns, argb(13166335, fade((int)(220.0F * distFade), intro)));
         }
      }
   }

   private static boolean project(
      Vec3 world, Vec3 camPos, Vector3fc fwd, Vector3fc up, Vector3fc left, float tanHalfV, float aspect, int sw, int sh, float[] out
   ) {
      double dx = world.x - camPos.x;
      double dy = world.y - camPos.y;
      double dz = world.z - camPos.z;
      double depth = dx * (double)fwd.x() + dy * (double)fwd.y() + dz * (double)fwd.z();
      if (depth <= 0.05) {
         return false;
      } else {
         double h = -(dx * (double)left.x() + dy * (double)left.y() + dz * (double)left.z());
         double v = dx * (double)up.x() + dy * (double)up.y() + dz * (double)up.z();
         double ndcX = h / depth / (double)(tanHalfV * aspect);
         double ndcY = v / depth / (double)tanHalfV;
         out[0] = (float)((double)sw * 0.5 * (1.0 + ndcX));
         out[1] = (float)((double)sh * 0.5 * (1.0 - ndcY));
         out[2] = (float)depth;
         return true;
      }
   }

   private static LivingEntity computePrimary(Minecraft mc, Player player, ExoVisual vis, float partialTick) {
      Vec3 eye = player.getEyePosition(partialTick);
      Vec3 look = player.getViewVector(partialTick);
      List<LivingEntity> foes = new ArrayList<>();

      for (int id : vis.taggedIds()) {
         Entity var10 = mc.level.getEntity(id);
         if (var10 instanceof LivingEntity) {
            LivingEntity foe = (LivingEntity)var10;
            if (foe.isAlive() && foe != player) {
               foes.add(foe);
            }
         }
      }

      return ExoTags.selectPrimary(eye, look, foes);
   }

   private static void drawDossier(GuiGraphics g, Font font, HudPlacement.Box box, ExoCorner corner, ExosavantHud.Dossier foe, int theme, float intro) {
      boolean hasStatus = !foe.effects().isEmpty();
      int bedH = box.height() - (hasStatus ? 0 : 10);
      int bedLeft = box.x();
      int bedTop = box.y() + (corner.bottom() ? box.height() - bedH : 0);
      int x = bedLeft + 3;
      int right = x + 132;
      int tx = x + 6;
      int innerW = 120;
      backplate(g, bedLeft, bedTop, bedLeft + box.width(), bedTop + bedH, theme, intro, 165, false, corner);
      int y = bedTop + 3 + 5;
      caret(g, tx, y + 1, argb(theme, fade(210, intro)));
      backedString(g, font, "TARGET ANALYSIS", (float)(tx + 5), (float)y, 0.65F, argb(theme, fade(210, intro)));
      y += 11;
      backedString(g, font, fit(font, foe.name(), 0.85F, innerW), (float)tx, (float)y, 0.85F, argb(13166335, fade(240, intro)));
      y += 12;
      int hpCol = hpBandColor(foe.hpFrac());
      backedString(g, font, "HP", (float)tx, (float)y, 0.7F, argb(theme, fade(210, intro)));
      backedRight(g, font, foe.hp() + "/" + foe.maxHp(), (float)(right - 6), (float)y, 0.7F, argb(hpCol, fade(235, intro)));
      y += 9;
      int barX1 = right - 6;
      g.fill(tx - 1, y - 1, barX1 + 1, y + 4, argb(0, fade(170, intro)));
      g.fill(tx, y, barX1, y + 3, argb(1053720, fade(200, intro)));
      int hw = Math.round((float)(barX1 - tx) * foe.hpFrac());
      if (hw > 0) {
         g.fill(tx, y, tx + hw, y + 3, argb(hpCol, fade(235, intro)));
      }

      y += 8;
      backedString(g, font, "THREAT", (float)tx, (float)y, 0.7F, argb(theme, fade(210, intro)));
      threatPips(g, tx + Math.round((float)font.width("THREAT") * 0.7F) + 7, y + 2, foe.threat(), theme, intro);
      y += 10;
      backedString(g, font, "WEAK", (float)tx, (float)y, 0.7F, argb(theme, fade(210, intro)));
      backedString(g, font, "HEAD", (float)(tx + Math.round((float)font.width("WEAK") * 0.7F) + 8), (float)y, 0.7F, argb(16734810, fade(230, intro)));
      y += 10;
      backedString(g, font, "DIST", (float)tx, (float)y, 0.7F, argb(theme, fade(210, intro)));
      backedString(g, font, foe.dist() + "m", (float)(tx + Math.round((float)font.width("DIST") * 0.7F) + 8), (float)y, 0.7F, argb(13166335, fade(230, intro)));
      y += 10;
      if (hasStatus) {
         backedString(g, font, "STATUS", (float)tx, (float)y, 0.7F, argb(theme, fade(210, intro)));
         int stX = tx + Math.round((float)font.width("STATUS") * 0.7F) + 6;
         backedString(g, font, fit(font, String.join(" ", foe.effects()), 0.6F, right - 6 - stX), (float)stX, (float)y, 0.6F, argb(16757066, fade(225, intro)));
      }
   }

   private static int threatRating(LivingEntity foe) {
      float maxHp = foe.getMaxHealth();
      if (maxHp >= 150.0F) {
         return 4;
      } else {
         int rating = maxHp >= 60.0F ? 3 : (maxHp >= 25.0F ? 2 : 1);
         AttributeInstance atk = foe.getAttribute(Attributes.ATTACK_DAMAGE);
         if (atk != null && atk.getValue() >= 8.0) {
            rating++;
         }

         return Math.max(1, Math.min(4, rating));
      }
   }

   private static void threatPips(GuiGraphics g, int x, int cy, int rating, int theme, float intro) {
      for (int i = 0; i < 4; i++) {
         int px = x + i * 9;
         if (i < rating) {
            diamond(g, px, cy, 3, argb(16734810, fade(235, intro)));
         } else {
            hollowDiamond(g, px, cy, 3, argb(theme, fade(150, intro)));
         }
      }
   }

   private static void hollowDiamond(GuiGraphics g, int cx, int cy, int r, int color) {
      for (int i = 0; i <= r; i++) {
         int w = r - i;
         g.fill(cx - w, cy - i, cx - w + 1, cy - i + 1, color);
         g.fill(cx + w, cy - i, cx + w + 1, cy - i + 1, color);
         if (i != 0) {
            g.fill(cx - w, cy + i, cx - w + 1, cy + i + 1, color);
            g.fill(cx + w, cy + i, cx + w + 1, cy + i + 1, color);
         }
      }
   }

   private static String fit(Font font, String s, float scale, int maxPx) {
      if (s == null) {
         return "";
      } else {
         while (s.length() > 1 && (float)font.width(s) * scale > (float)maxPx) {
            s = s.substring(0, s.length() - 1);
         }

         return s;
      }
   }

   private void drawBarragePreview(GuiGraphics g, Minecraft mc, Font font, Player player, float partialTick, int sw, int sh, int theme) {
      ArtilleryBarrageAbility.ZoneSolution sol = ArtilleryBarrageAbility.resolveZone(player, mc.level, partialTick);
      if (!sol.grounded()) {
         centeredScaled(g, font, "NO FIRING SOLUTION", sw / 2, sh / 2 + 16, 0.8F, argb(theme, 110));
      } else {
         Vec3 zone = sol.zone();
         double radius = ArtilleryBarrageAbility.zoneRadius();
         ExosavantHud.CamBasis cam = basis(mc, sw, sh, partialTick);
         if (cam.pos.distanceToSqr(zone) >= (radius + 1.5) * (radius + 1.5)) {
            int segments = 56;
            int ringCol = argb(13166335, 205);
            float firstX = 0.0F;
            float firstY = 0.0F;
            float prevX = 0.0F;
            float prevY = 0.0F;
            boolean firstOk = false;
            boolean prevOk = false;

            for (int i = 0; i < segments; i++) {
               double ang = (double)i / (double)segments * (Math.PI * 2);
               Vec3 p = zone.add(Math.cos(ang) * radius, 0.1, Math.sin(ang) * radius);
               boolean ok = project(p, cam.pos, cam.fwd, cam.up, cam.left, cam.tanHalfV, cam.aspect, sw, sh, this.proj);
               float x = this.proj[0];
               float y = this.proj[1];
               if (ok) {
                  if (prevOk) {
                     line(g, prevX, prevY, x, y, sw, sh, ringCol);
                  }

                  if (!firstOk) {
                     firstX = x;
                     firstY = y;
                     firstOk = true;
                  }
               }

               prevX = x;
               prevY = y;
               prevOk = ok;
            }

            if (firstOk && prevOk) {
               line(g, prevX, prevY, firstX, firstY, sw, sh, ringCol);
            }
         }

         if (project(zone.add(0.0, 0.1, 0.0), cam.pos, cam.fwd, cam.up, cam.left, cam.tanHalfV, cam.aspect, sw, sh, this.proj)) {
            int zx = Math.round(this.proj[0]);
            int zy = Math.round(this.proj[1]);
            int markCol = argb(13166335, 220);
            g.fill(zx - 5, zy, zx - 1, zy + 1, markCol);
            g.fill(zx + 1, zy, zx + 5, zy + 1, markCol);
            g.fill(zx, zy - 5, zx + 1, zy - 1, markCol);
            g.fill(zx, zy + 1, zx + 1, zy + 5, markCol);
            int dist = (int)Math.round(Math.sqrt(player.distanceToSqr(zone)));
            float pulse = 0.78F + 0.22F * (float)Math.sin((double)nowMs() / 220.0);
            centeredScaled(g, font, "FIRE MISSION: " + dist + "m", zx, zy - 16, 0.8F, argb(13166335, (int)(235.0F * pulse)));
         }
      }
   }

   private static void line(GuiGraphics g, float x0, float y0, float x1, float y1, int sw, int sh, int color) {
      float dx = x1 - x0;
      float dy = y1 - y0;
      float m = 2.0F;
      float xmin = -m;
      float ymin = -m;
      float xmax = (float)sw + m;
      float ymax = (float)sh + m;
      float[] p = new float[]{-dx, dx, -dy, dy};
      float[] q = new float[]{x0 - xmin, xmax - x0, y0 - ymin, ymax - y0};
      float t0 = 0.0F;
      float t1 = 1.0F;

      for (int i = 0; i < 4; i++) {
         if (p[i] == 0.0F) {
            if (q[i] < 0.0F) {
               return;
            }
         } else {
            float r = q[i] / p[i];
            if (p[i] < 0.0F) {
               if (r > t1) {
                  return;
               }

               if (r > t0) {
                  t0 = r;
               }
            } else {
               if (r < t0) {
                  return;
               }

               if (r < t1) {
                  t1 = r;
               }
            }
         }
      }

      float cx0 = x0 + t0 * dx;
      float cy0 = y0 + t0 * dy;
      float cx1 = x0 + t1 * dx;
      float cy1 = y0 + t1 * dy;
      float cdx = cx1 - cx0;
      float cdy = cy1 - cy0;
      int steps = (int)Math.ceil((double)Math.max(Math.abs(cdx), Math.abs(cdy)));
      if (steps <= 0) {
         int ix = Math.round(cx0);
         int iy = Math.round(cy0);
         g.fill(ix, iy, ix + 1, iy + 1, color);
      } else {
         float sx = cdx / (float)steps;
         float sy = cdy / (float)steps;
         float x = cx0;
         float y = cy0;

         for (int ix = 0; ix <= steps; ix++) {
            int ixx = Math.round(x);
            int iy = Math.round(y);
            g.fill(ixx, iy, ixx + 1, iy + 1, color);
            x += sx;
            y += sy;
         }
      }
   }

   private static ExosavantHud.CamBasis basis(Minecraft mc, int sw, int sh, float partialTick) {
      Camera camera = mc.gameRenderer.getMainCamera();
      ExosavantHud.CamBasis b = new ExosavantHud.CamBasis();
      b.pos = camera.getPosition();
      b.fwd = camera.getLookVector();
      b.up = camera.getUpVector();
      b.left = camera.getLeftVector();
      double fov = ((GameRendererFovAccessor)mc.gameRenderer).awakened$getFov(camera, partialTick, true);
      b.tanHalfV = (float)Math.tan(Math.toRadians(fov) * 0.5);
      b.aspect = (float)sw / (float)sh;
      return b;
   }

   private static int standInTheme() {
      Power power = PowerRegistry.get("exosavant");
      return power == null ? 13166335 : power.themeColor();
   }

   private static Ability selectedAbility() {
      Power power = PowerRegistry.get("exosavant");
      return power == null ? null : power.ability(ClientPowerState.get().activeSlot());
   }

   private static void drawStatusCluster(
      GuiGraphics g,
      Font font,
      HudPlacement.Box box,
      ExoCorner corner,
      ClientPowerState state,
      ExoVisual vis,
      Player player,
      Ability selected,
      ExosavantHud.FailsafeReadout fs,
      float dmgFlash,
      int theme,
      float intro
   ) {
      ExosavantHud.Rail rail = ExosavantHud.Rail.of(corner, box);
      boolean fsArmed = fs.armed();
      int stackRows = (fsArmed ? 1 : 0) + 3;
      int stackH = stackRows * 13;
      int silOffset = Math.max(0, (stackH - 40) / 2);
      int contentH = 51 + Math.max(silOffset + 40, stackH) + 4;
      int bedLeft = box.x();
      int bedTop = box.y() + (corner.bottom() ? box.height() - contentH : 0);
      int powerTop = bedTop + 4;
      int hpTop = powerTop + 13;
      int armorTop = hpTop + 13;
      int blockTop = bedTop + 51;
      int silTop = blockTop + silOffset;
      backplate(g, bedLeft, bedTop, bedLeft + box.width(), bedTop + contentH, theme, intro, 125, true, corner);
      float frac = state.staminaMax() > 0.0F ? clamp01(state.stamina() / state.staminaMax()) : 0.0F;
      int pCol = state.flashTicks() > 0 ? 16728128 : (frac < 0.2F ? 16747082 : 5228799);
      statBar(g, font, rail, powerTop, intro, "POWER", frac, pCol, Math.round(frac * 100.0F) + "%", true);
      float maxHp = Math.max(1.0F, player.getMaxHealth());
      float hpFrac = clamp01(player.getHealth() / maxHp);
      int hpCol = hpBandColor(hpFrac);
      if (dmgFlash > 0.0F) {
         hpCol = lerpColor(hpCol, 16777215, dmgFlash * 0.7F);
      }

      String hpStr = (int)Math.ceil((double)player.getHealth()) + "/" + (int)maxHp;
      ExosavantHud.BarRow hp = statBar(g, font, rail, hpTop, intro, "HP", hpFrac, hpCol, hpStr, false);
      float absorb = player.getAbsorptionAmount();
      if (absorb > 0.0F) {
         int goldW = Math.min(96, Math.round(96.0F * clamp01(absorb / maxHp)));
         if (goldW > 0) {
            int capX0 = hp.barX1() - goldW;
            int top = hp.barTop();
            int bot = top + hp.barH();
            g.fill(capX0 - 1, top, capX0, bot, argb(329740, fade(225, intro)));
            g.fill(capX0, top, hp.barX1(), bot, argb(16773064, fade(240, intro)));
            g.fill(capX0, top, hp.barX1(), top + 1, argb(16775388, fade(195, intro)));
         }
      }

      float armor = (float)player.getArmorValue();
      statBar(g, font, rail, armorTop, intro, "ARMOR", clamp01(armor / 20.0F), 12571360, String.valueOf((int)armor), false);
      double tough = player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
      if (tough > 0.0) {
         String toughStr = fit(font, "+" + (int)tough, 0.7F, 18);
         scaledString(g, font, toughStr, rail.textX(font, toughStr, 0.7F, 155), (float)armorTop, 0.7F, argb(12571360, fade(210, intro)));
      }

      int rowTop = blockTop;
      if (fsArmed) {
         boolean ready = fs.ready();
         String fsState;
         int fsCol;
         if (fs.engaged()) {
            fsState = "ENGAGED";
            fsCol = 16728128;
         } else if (ready) {
            fsState = "READY";
            fsCol = 4646568;
         } else {
            fsState = "CHARGING";
            fsCol = 16757066;
         }

         ledRow(g, font, rail, blockTop, fsCol, "FAILSAFE", fsState, intro);
         if (!ready && !fs.engaged()) {
            int slY = blockTop + 9;
            int slW = 114;
            int slX = rail.x(37, slW);
            g.fill(slX, slY, slX + slW, slY + 1, argb(0, fade(150, intro)));
            g.fill(slX, slY, slX + Math.round((float)slW * fs.recharge()), slY + 1, argb(16757066, fade(210, intro)));
         }

         rowTop = blockTop + 13;
      }

      ledRow(g, font, rail, rowTop, 4646568, "SUIT", "ONLINE", intro);
      rowTop += 13;
      drawWeapons(g, font, selected, state, vis, rail, rowTop, theme, intro);
      drawSilhouette(g, rail.x(4, 24), silTop, vis.builtMask(), theme, intro);
   }

   private static void drawWeapons(
      GuiGraphics g, Font font, Ability selected, ClientPowerState state, ExoVisual vis, ExosavantHud.Rail rail, int y, int theme, float intro
   ) {
      float ls = 0.7F;
      int caretU = 37 + Math.round((float)font.width("ARMAMENT") * ls) + 6;
      int nameU = caretU + 5;
      scaledString(g, font, "ARMAMENT", rail.textX(font, "ARMAMENT", ls, 37), (float)y, ls, argb(theme, fade(215, intro)));
      if (selected == null) {
         scaledString(g, font, "-", rail.textX(font, "-", ls, caretU), (float)y, ls, argb(theme, fade(150, intro)));
      } else {
         String name = fit(font, selected.displayName().getString().toUpperCase(Locale.ROOT), ls, 173 - nameU);
         caret(g, rail.x(caretU, 2), y + 1, argb(13166335, fade(235, intro)), !rail.mirrored());
         scaledString(g, font, name, rail.textX(font, name, ls, nameU), (float)y, ls, argb(13166335, fade(235, intro)));
         int y2 = y + 13;
         boolean armed = state.infiniteStamina() || state.stamina() >= selected.shownCost(Minecraft.getInstance().player);
         String stStr = armed ? "ARMED" : "LOW POWER";
         int stateU = 37 + Math.round((float)font.width("STATUS") * ls) + 6;
         scaledString(g, font, "STATUS", rail.textX(font, "STATUS", ls, 37), (float)y2, ls, argb(theme, fade(215, intro)));
         scaledString(g, font, stStr, rail.textX(font, stStr, ls, stateU), (float)y2, ls, argb(armed ? 4646568 : 16728128, fade(235, intro)));
         int locks = vis.taggedIds().size();
         if (locks > 0) {
            String lockStr = "LOCK ×" + locks;
            int lockU = stateU + Math.round((float)font.width(stStr) * ls) + 7;
            scaledString(g, font, lockStr, rail.textX(font, lockStr, ls, lockU), (float)y2, ls, argb(16734810, fade(225, intro)));
         }
      }
   }

   private static ExosavantHud.BarRow statBar(
      GuiGraphics g, Font font, ExosavantHud.Rail rail, int rowTop, float intro, String label, float frac, int fillCol, String value, boolean hero
   ) {
      int barX0 = rail.x(28, 96);
      int barX1 = barX0 + 96;
      int barH = hero ? 6 : 5;
      int barTop = rowTop + 1;
      int bot = barTop + barH;
      scaledString(g, font, label, rail.textX(font, label, 0.7F, 4), (float)rowTop, 0.7F, argb(fillCol, fade(205, intro)));
      g.fill(barX0 - 1, barTop - 1, barX1 + 1, bot + 1, argb(0, fade(170, intro)));
      g.fill(barX0, barTop, barX1, bot, argb(1053720, fade(200, intro)));
      int cellCol = argb(0, fade(55, intro));

      for (int cx = barX0 + 16; cx < barX1; cx += 16) {
         g.fill(cx, barTop, cx + 1, bot, cellCol);
      }

      int fillW = Math.round(96.0F * clamp01(frac));
      if (fillW > 0) {
         g.fill(barX0, barTop, barX0 + fillW, bot, argb(fillCol, fade(235, intro)));
         g.fill(barX0, barTop, barX0 + fillW, barTop + 1, argb(16773296, fade(hero ? 195 : 150, intro)));
      }

      g.fill(barX1 - 1, barTop, barX1, bot, argb(13166335, fade(170, intro)));
      if (value != null) {
         scaledString(g, font, value, rail.textXIn(font, value, 0.7F, 151), (float)rowTop, 0.7F, argb(13166335, fade(215, intro)));
      }

      return new ExosavantHud.BarRow(barX0, barX1, barTop, barH);
   }

   private static void ledRow(GuiGraphics g, Font font, ExosavantHud.Rail rail, int rowTop, int stateCol, String label, String state, float intro) {
      ledSquare(g, rail.x(37, 4), rowTop + 1, stateCol, intro);
      scaledString(g, font, label, rail.textX(font, label, 0.7F, 45), (float)rowTop, 0.7F, argb(13166335, fade(215, intro)));
      scaledString(g, font, state, rail.textXIn(font, state, 0.7F, 151), (float)rowTop, 0.7F, argb(stateCol, fade(235, intro)));
   }

   private static void ledSquare(GuiGraphics g, int x, int y, int color, float intro) {
      int s = 4;
      g.fill(x - 1, y - 1, x + s + 1, y + s + 1, argb(0, fade(150, intro)));
      g.fill(x, y, x + s, y + s, argb(color, fade(235, intro)));
      g.fill(x, y, x + 1, y + 1, argb(16777215, fade(150, intro)));
   }

   private static void caret(GuiGraphics g, int x, int y, int color) {
      caret(g, x, y, color, true);
   }

   private static void caret(GuiGraphics g, int x, int y, int color, boolean pointsRight) {
      int edge = pointsRight ? x : x + 1;
      int tip = pointsRight ? x + 1 : x;
      g.fill(edge, y, edge + 1, y + 3, color);
      g.fill(tip, y + 1, tip + 1, y + 2, color);
   }

   private static void drawSilhouette(GuiGraphics g, int sx, int sy, int mask, int theme, float intro) {
      int scx = sx + 12;

      for (ExoPiece piece : ExoPiece.values()) {
         drawSilRegion(g, piece, scx, sy, (mask & piece.bit()) != 0, theme, intro);
      }

      g.fill(scx - 1, sy + 8, scx + 1, sy + 10, argb(theme, fade(55, intro)));
      g.fill(scx - 9, sy + 39, scx + 9, sy + 40, argb(theme, fade(55, intro)));
      g.fill(scx - 5, sy + 39, scx + 5, sy + 40, argb(theme, fade(115, intro)));
   }

   private static void drawSilRegion(GuiGraphics g, ExoPiece piece, int scx, int sy, boolean on, int theme, float intro) {
      switch (piece) {
         case HELMET:
            if (on) {
               silPlate(g, scx - 4, sy, scx + 4, sy + 8, theme, intro, false);
               g.fill(scx - 3, sy + 3, scx + 3, sy + 5, argb(660510, fade(235, intro)));
               g.fill(scx - 3, sy + 3, scx + 3, sy + 4, argb(silEdge(theme), fade(255, intro)));
            } else {
               silGhost(g, scx - 4, sy, scx + 4, sy + 8, theme, intro);
            }
            break;
         case CHESTPLATE:
            if (on) {
               silPlate(g, scx - 6, sy + 10, scx + 6, sy + 16, theme, intro, true);
               silPlate(g, scx - 5, sy + 16, scx + 5, sy + 22, theme, intro, false);
               g.fill(scx, sy + 17, scx + 1, sy + 21, argb(660510, fade(120, intro)));
            } else {
               silGhost(g, scx - 6, sy + 10, scx + 6, sy + 22, theme, intro);
            }
            break;
         case GAUNTLETS:
            for (int sxx = -1; sxx <= 1; sxx += 2) {
               if (on) {
                  silPlate(g, silX(scx, sxx, 7, 12), sy + 10, silX(scx, sxx, 12, 7), sy + 14, theme, intro, true);
                  silPlate(g, silX(scx, sxx, 8, 12), sy + 14, silX(scx, sxx, 12, 8), sy + 24, theme, intro, false);
                  g.fill(silX(scx, sxx, 8, 12), sy + 22, silX(scx, sxx, 12, 8), sy + 23, argb(silEdge(theme), fade(200, intro)));
               } else {
                  silGhost(g, silX(scx, sxx, 7, 12), sy + 10, silX(scx, sxx, 12, 7), sy + 24, theme, intro);
               }
            }
            break;
         case LEGGINGS:
            for (int sx = -1; sx <= 1; sx += 2) {
               if (on) {
                  silPlate(g, silX(scx, sx, 1, 6), sy + 23, silX(scx, sx, 6, 1), sy + 28, theme, intro, true);
                  silPlate(g, silX(scx, sx, 1, 5), sy + 28, silX(scx, sx, 5, 1), sy + 33, theme, intro, false);
               } else {
                  silGhost(g, silX(scx, sx, 1, 6), sy + 23, silX(scx, sx, 6, 1), sy + 33, theme, intro);
               }
            }
            break;
         case BOOTS:
            for (int s = -1; s <= 1; s += 2) {
               if (on) {
                  silPlate(g, silX(scx, s, 1, 6), sy + 33, silX(scx, s, 6, 1), sy + 37, theme, intro, false);
                  g.fill(silX(scx, s, 6, 7), sy + 35, silX(scx, s, 7, 6), sy + 37, argb(theme, fade(200, intro)));
               } else {
                  silGhost(g, silX(scx, s, 1, 7), sy + 33, silX(scx, s, 7, 1), sy + 37, theme, intro);
               }
            }
            break;
         case CHIP:
            if (on) {
               silDiamond(g, scx, sy + 13, 3, argb(329740, fade(200, intro)));
               silDiamond(g, scx, sy + 13, 2, argb(silEdge(theme), fade(255, intro)));
               g.fill(scx, sy + 13, scx + 1, sy + 14, argb(16777215, fade(255, intro)));
            } else {
               silDiamond(g, scx, sy + 13, 2, argb(theme, fade(60, intro)));
            }
      }
   }

   private static int silX(int scx, int s, int a, int b) {
      return scx + (s > 0 ? a : -b);
   }

   private static int silEdge(int theme) {
      return lerpColor(theme, 16777215, 0.5F);
   }

   private static void silPlate(GuiGraphics g, int x0, int y0, int x1, int y1, int theme, float intro, boolean sheen) {
      g.fill(x0, y0, x1, y1, argb(lerpColor(theme, 2242634, 0.28F), fade(240, intro)));
      g.fill(x0, y0, x1, y0 + 1, argb(silEdge(theme), fade(240, intro)));
      g.fill(x0, y0, x0 + 1, y1, argb(theme, fade(190, intro)));
      g.fill(x1 - 1, y0, x1, y1, argb(theme, fade(190, intro)));
      g.fill(x0, y1 - 1, x1, y1, argb(theme, fade(140, intro)));
      if (sheen) {
         g.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 2, argb(16777215, fade(60, intro)));
      }
   }

   private static void silGhost(GuiGraphics g, int x0, int y0, int x1, int y1, int theme, float intro) {
      g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, argb(theme, fade(22, intro)));
      int c = argb(theme, fade(70, intro));
      g.fill(x0, y0, x1, y0 + 1, c);
      g.fill(x0, y1 - 1, x1, y1, c);
      g.fill(x0, y0, x0 + 1, y1, c);
      g.fill(x1 - 1, y0, x1, y1, c);
   }

   private static void silDiamond(GuiGraphics g, int cx, int cy, int r, int color) {
      for (int dy = -r; dy <= r; dy++) {
         int w = r - Math.abs(dy);
         g.fill(cx - w, cy + dy, cx + w + 1, cy + dy + 1, color);
      }
   }

   private static void backplate(
      GuiGraphics g, int left, int top, int right, int bottom, int theme, float intro, int bedAlpha, boolean brackets, ExoCorner corner
   ) {
      g.fill(left, top, right, bottom, argb(329740, fade(bedAlpha, intro)));
      int accentY = corner.bottom() ? bottom - 1 : top;
      g.fill(left, accentY, right, accentY + 1, argb(theme, fade(150, intro)));
      int accentX = corner.right() ? right - 1 : left;
      g.fill(accentX, top, accentX + 1, bottom, argb(theme, fade(90, intro)));
      if (brackets) {
         int bl = 6;
         int bc = argb(theme, fade(175, intro));
         int bx = corner.right() ? right - 2 : left;
         int arms = corner.right() ? -1 : 1;
         corner(g, bx, top, arms, 1, bl, bc);
         corner(g, bx, bottom - 2, arms, -1, bl, bc);
      }
   }

   private void drawFlightInstruments(
      GuiGraphics g,
      Font font,
      ClientPowerState state,
      Player player,
      int cx,
      int cy,
      int sw,
      int sh,
      boolean jet,
      boolean afterburner,
      int syncedSpeedMps,
      int boostCd,
      int theme
   ) {
      Vec3 vel = player.getDeltaMovement();
      double horizBpt = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
      double speed = jet ? (double)syncedSpeedMps : horizBpt * 20.0;
      double speedBpt = speed / 20.0;
      float sFrac = clamp01((float)((speedBpt - 0.9) / 1.7000000000000002));
      if (jet && sFrac > 0.0F) {
         this.drawSpeedStreaks(g, cx, cy, sFrac, afterburner, theme);
      }

      if (FLIGHT.hudVisible()) {
         drawFlightColumn(
            g,
            font,
            FLIGHT.layout(sw, sh),
            FLIGHT.corner(),
            ExosavantHud.FlightReadout.of(state, player, jet, afterburner, speed, (int)Math.round(vel.y * 20.0), boostCd),
            theme
         );
      }

      drawHorizon(g, player, cx, cy, jet, theme);
   }

   private static void drawFlightColumn(GuiGraphics g, Font font, HudPlacement.Box box, ExoCorner corner, ExosavantHud.FlightReadout r, int theme) {
      ExosavantHud.Rail rail = ExosavantHud.Rail.of(corner, box);
      boolean jet = r.jet();
      boolean afterburner = r.afterburner();
      double speed = r.speed();
      int contentH = 2 + (jet ? 78 : 49);
      int top = box.y() + (corner.bottom() ? box.height() - contentH : 0) + 2;
      long now = nowMs();
      int col = argb(theme, 220);
      String speedStr = String.format("%.0f m/s", speed);
      int speedCol = afterburner ? argb(16734810, 235) : col;
      backedString(g, font, speedStr, rail.textX(font, speedStr, 0.8F, 0), (float)top, 0.8F, speedCol);
      drawModeBadge(g, font, jet, afterburner, rail, Math.round((float)font.width(speedStr) * 0.8F) + 6, top);
      String altStr = "ALT " + r.alt();
      backedString(g, font, altStr, rail.textX(font, altStr, 0.8F, 0), (float)(top + 11), 0.8F, col);
      int vs = r.verticalSpeed();
      String vsStr = "VS " + (vs > 0 ? "▲" : (vs < 0 ? "▼" : "")) + Math.abs(vs) + " m/s";
      int vsCol = vs > 0 ? argb(6356842, 220) : (vs < 0 ? argb(16757066, 220) : col);
      backedString(g, font, vsStr, rail.textX(font, vsStr, 0.8F, 0), (float)(top + 22), 0.8F, vsCol);
      float frac = r.fuelFrac();
      boolean infinite = r.infiniteFuel();
      int secsLeft = r.secondsLeft();
      boolean lowFuel = !infinite && secsLeft < 3;
      boolean blink = (now / 180L & 1L) == 0L;
      int fuelCol = lowFuel && blink ? argb(16728128, 235) : argb(theme, 220);
      int fy = top + 34;
      String fuelStr = infinite ? "FUEL" : "FUEL  ≈" + secsLeft + "s";
      backedString(g, font, fuelStr, rail.textX(font, fuelStr, 0.8F, 0), (float)fy, 0.8F, fuelCol);
      int barH = 4;
      int bx0 = rail.x(0, 64);
      int bx1 = bx0 + 64;
      int by = fy + 10;
      g.fill(bx0 - 1, by - 1, bx1 + 1, by + barH + 1, argb(0, 170));
      g.fill(bx0, by, bx1, by + barH, argb(1053720, 200));
      int fillW = Math.round(64.0F * (infinite ? 1.0F : frac));
      int barFill = lowFuel && blink ? 16728128 : (!infinite && frac < 0.2F ? 16747082 : 13166335);
      if (fillW > 0) {
         g.fill(bx0, by, bx0 + fillW, by + barH, argb(barFill, 235));
      }

      if (jet) {
         double cruiseTopMps = 34.0;
         double abTopMps = 52.0;
         int tBarH = 4;
         int ty = by + barH + 6;
         int tx0 = rail.x(0, 64);
         int tx1 = tx0 + 64;
         int mainW = (int)Math.round(64.0 * cruiseTopMps / abTopMps);
         int overX = tx0 + mainW;
         g.fill(tx0 - 1, ty - 1, tx1 + 1, ty + tBarH + 1, argb(0, 170));
         g.fill(tx0, ty, tx1, ty + tBarH, argb(1053720, 200));
         g.fill(overX, ty, tx1, ty + tBarH, argb(3804686, afterburner ? 200 : 80));
         int mainFillW = Math.round((float)mainW * clamp01((float)(speed / cruiseTopMps)));
         if (mainFillW > 0) {
            g.fill(tx0, ty, tx0 + mainFillW, ty + tBarH, argb(13166335, 235));
         }

         if (afterburner) {
            float overFrac = clamp01((float)((speed - cruiseTopMps) / (abTopMps - cruiseTopMps)));
            int overFillW = Math.round((float)(tx1 - overX) * overFrac);
            if (overFillW > 0) {
               g.fill(overX, ty, overX + overFillW, ty + tBarH, argb(16734810, 240));
            }
         }

         for (int gx = tx0 + 8; gx < tx1; gx += 8) {
            g.fill(gx, ty, gx + 1, ty + tBarH, argb(329740, 150));
         }

         g.fill(overX, ty - 1, overX + 1, ty + tBarH + 1, argb(theme, 220));
         backedString(g, font, "SPD", rail.textX(font, "SPD", 0.7F, 67), (float)(ty - 1), 0.7F, argb(theme, 200));
         drawBoostReadout(g, font, rail, ty + tBarH + 7, r.boostCd(), afterburner, now, theme);
      }
   }

   private static void drawHorizon(GuiGraphics g, Player player, int cx, int cy, boolean jet, int theme) {
      float pitch = player.getXRot();
      int hcol = argb(theme, jet ? 90 : 60);
      int pixPerDeg = Math.max(1, cy / 45);
      int hy = cy + (int)(pitch * (float)pixPerDeg);
      hy = Math.max(cy - 60, Math.min(cy + 60, hy));
      g.fill(cx - 70, hy, cx - 16, hy + 1, hcol);
      g.fill(cx + 16, hy, cx + 70, hy + 1, hcol);
      g.fill(cx - 10, cy, cx - 4, cy + 1, hcol);
      g.fill(cx + 4, cy, cx + 10, cy + 1, hcol);
      int r15 = 15 * pixPerDeg;
      g.fill(cx - 36, hy - r15, cx - 20, hy - r15 + 1, hcol);
      g.fill(cx + 20, hy + r15, cx + 36, hy + r15 + 1, hcol);
   }

   private static void drawBoostReadout(GuiGraphics g, Font font, ExosavantHud.Rail rail, int y, int boostCd, boolean afterburner, long now, int theme) {
      boolean recharging = !afterburner && boostCd > 0;
      float pulse = 0.6F + 0.4F * (float)Math.sin((double)now / 110.0);
      String label;
      int labelCol;
      int fillRgb;
      int fillA;
      if (afterburner) {
         label = "BOOST ON";
         labelCol = argb(16734810, (int)(200.0F + 55.0F * pulse));
         fillRgb = 16734810;
         fillA = (int)(200.0F + 55.0F * pulse);
      } else if (recharging) {
         int secs = (int)Math.ceil((double)((float)boostCd / 255.0F * 5.0F));
         label = "BOOST ≈" + secs + "s";
         labelCol = argb(theme, 150);
         fillRgb = 13166335;
         fillA = 215;
      } else {
         label = "BOOST RDY";
         labelCol = argb(4646568, 235);
         fillRgb = 4646568;
         fillA = 235;
      }

      backedString(g, font, label, rail.textX(font, label, 0.7F, 0), (float)y, 0.7F, labelCol);
      int barH = 3;
      int bx0 = rail.x(0, 64);
      int bx1 = bx0 + 64;
      int by = y + 9;
      g.fill(bx0 - 1, by - 1, bx1 + 1, by + barH + 1, argb(0, 170));
      g.fill(bx0, by, bx1, by + barH, argb(1053720, 200));
      float fillFrac = recharging ? clamp01(1.0F - (float)boostCd / 255.0F) : 1.0F;
      int fillW = Math.round(64.0F * fillFrac);
      if (fillW > 0) {
         g.fill(bx0, by, bx0 + fillW, by + barH, argb(fillRgb, Math.min(255, fillA)));
      }
   }

   private static void drawModeBadge(GuiGraphics g, Font font, boolean jet, boolean afterburner, ExosavantHud.Rail rail, int u, int y) {
      String text = afterburner ? "A/B" : (jet ? "JET" : "FREE");
      float s = 0.8F;
      int tw = Math.round((float)font.width(text) * s);
      int padX = 3;
      int boxW = tw + padX * 2;
      int boxH = Math.round(8.0F * s) + 3;
      int x0 = rail.x(u, boxW);
      int rightX = x0 + boxW;
      int top = y - 2;
      int bg;
      int bgA;
      int edge;
      int txt;
      if (afterburner) {
         float pulse = 0.6F + 0.4F * (float)Math.sin((double)nowMs() / 110.0);
         bg = 13114910;
         bgA = (int)(165.0F + 70.0F * pulse);
         edge = 16738922;
         txt = 16777215;
      } else if (jet) {
         bg = 16742942;
         bgA = 210;
         edge = 16760934;
         txt = 16777215;
      } else {
         bg = 1451054;
         bgA = 200;
         edge = 13166335;
         txt = 13166335;
      }

      g.fill(x0, top, rightX, top + boxH, argb(bg, bgA));
      g.fill(x0, top, rightX, top + 1, argb(edge, 150));
      int edgeX = rail.mirrored() ? x0 : rightX - 1;
      g.fill(edgeX, top, edgeX + 1, top + boxH, argb(edge, !jet && !afterburner ? 110 : 160));
      scaledString(g, font, text, (float)(x0 + padX), (float)y, s, argb(txt, 255));
   }

   private void drawSpeedStreaks(GuiGraphics g, int cx, int cy, float s, boolean afterburner, int theme) {
      int sw = cx * 2;
      int sh = cy * 2;
      int rgb = afterburner ? lerpColor(theme, 16734810, 0.5F) : theme;
      int peakA = (int)(s * (float)(afterburner ? 70 : 48));
      if (peakA > 0) {
         int edge = argb(rgb, peakA);
         int clear = argb(rgb, 0);
         int bandV = Math.round((float)sh * 0.2F);
         int bandH = Math.round((float)sw * 0.12F);
         g.fillGradient(0, 0, sw, bandV, edge, clear);
         g.fillGradient(0, sh - bandV, sw, sh, clear, edge);
         int steps = 6;

         for (int i = 0; i < steps; i++) {
            int a = peakA * (steps - i) / steps;
            if (a > 0) {
               int col = argb(rgb, a);
               int x0 = bandH * i / steps;
               int x1 = bandH * (i + 1) / steps;
               g.fill(x0, bandV, x1, sh - bandV, col);
               g.fill(sw - x1, bandV, sw - x0, sh - bandV, col);
            }
         }
      }
   }

   private void drawHeading(GuiGraphics g, Font font, Player player, int cx, float intro, int theme, float partialTick) {
      float yaw = player.getViewYRot(partialTick);
      float bearing = ((yaw + 180.0F) % 360.0F + 360.0F) % 360.0F;
      int y = 20;
      int halfW = 64;
      float degShown = 60.0F;
      float pixPerDeg = 1.0666667F;
      int minorA = fade(64, intro);
      int majorA = fade(96, intro);

      for (int b = 0; b < 360; b += 15) {
         float delta = ((float)b - bearing + 540.0F) % 360.0F - 180.0F;
         int x = cx + Math.round(delta * 1.0666667F);
         if (Math.abs(x - cx) <= 64) {
            boolean major = b % 45 == 0;
            g.fill(x, y, x + 1, y + (major ? 5 : 2), argb(theme, major ? majorA : minorA));
            if (major) {
               float edge = clamp01(1.15F - Math.abs(delta) / 60.0F);
               int la = fade(Math.round(168.0F * edge), intro);
               if (la > 6) {
                  backedCentered(g, font, CARDINALS[Math.round((float)b / 45.0F) % 8], x, y + 6, 0.7F, argb(theme, la));
               }
            }
         }
      }

      int markA = fade(190, intro);
      g.fill(cx - 2, y - 5, cx + 3, y - 4, argb(theme, markA));
      g.fill(cx - 1, y - 4, cx + 2, y - 3, argb(theme, markA));
      g.fill(cx, y - 3, cx + 1, y - 2, argb(theme, markA));
   }

   private void drawThreatWarnings(GuiGraphics g, Minecraft mc, Font font, Player player, float partialTick, int sw, int sh, int cx, int cy, float intro) {
      List<ExosavantHud.Threat> threats = this.scanThreats(mc, player, partialTick);
      if (!threats.isEmpty()) {
         long now = nowMs();
         this.drawThreatList(g, font, threats, cx, intro, now);
         this.drawThreatArrows(g, mc, threats, partialTick, sw, sh, cx, cy, intro, now);
      }
   }

   private List<ExosavantHud.Threat> scanThreats(Minecraft mc, Player player, float partialTick) {
      List<ExosavantHud.Threat> out = new ArrayList<>();
      Vec3 eye = player.getEyePosition(partialTick);
      AABB box = player.getBoundingBox().inflate(24.0);
      List<Entity> ents = mc.level.getEntitiesOfClass(Entity.class, box);
      int processed = 0;

      for (Entity e : ents) {
         if (out.size() >= 8 || processed >= 96) {
            break;
         }

         processed++;
         if (e != player) {
            if (e instanceof Creeper creeper) {
               float swell = creeper.getSwelling(partialTick);
               if (swell > 0.01F) {
                  float remaining = Math.max(0.0F, (1.0F - swell) * 1.5F);
                  Vec3 at = creeper.getPosition(partialTick).add(0.0, (double)creeper.getBbHeight() * 0.5, 0.0);
                  out.add(new ExosavantHud.Threat("DETONATION", String.format(Locale.ROOT, "%.1fs", remaining), 16728128, true, at, (double)remaining));
               }
            } else if (e instanceof Projectile proj && proj.getOwner() != player) {
               Vec3 vel = proj.getDeltaMovement();
               double speed = vel.length();
               if (!(speed <= 0.3)) {
                  Vec3 ppos = proj.getPosition(partialTick);
                  Vec3 toPlayer = eye.subtract(ppos);
                  double dist = toPlayer.length();
                  if (!(dist > 32.0) && !(dist < 0.001)) {
                     double approach = toPlayer.scale(1.0 / dist).dot(vel.scale(1.0 / speed));
                     if (!(approach < 0.5)) {
                        double tti = dist / (speed * 20.0);
                        if (!(tti > 2.5)) {
                           out.add(new ExosavantHud.Threat("INBOUND: " + projType(proj), null, 16728128, true, ppos, tti));
                        }
                     }
                  }
               }
            }
         }
      }

      if (player.isInLava()) {
         out.add(new ExosavantHud.Threat("HAZARD: LAVA", null, 16757066, false, null, 100.0));
      } else if (player.isOnFire() || player.getRemainingFireTicks() > 0) {
         out.add(new ExosavantHud.Threat("HAZARD: FIRE", null, 16757066, false, null, 101.0));
      }

      if (player.isUnderWater()) {
         int air = player.getAirSupply();
         if (air < 60) {
            int pct = Math.max(0, Math.min(100, air * 100 / Math.max(1, player.getMaxAirSupply())));
            out.add(new ExosavantHud.Threat("O2 LOW " + pct + "%", null, 16757066, false, null, 102.0));
         }
      }

      out.sort(Comparator.comparingDouble(t -> t.sortKey));
      return out;
   }

   private static String projType(Projectile proj) {
      if (proj instanceof ThrownTrident) {
         return "TRIDENT";
      } else if (proj instanceof AbstractArrow) {
         return "ARROW";
      } else {
         return proj instanceof AbstractHurtingProjectile ? "FIREBALL" : "PROJECTILE";
      }
   }

   private void drawThreatList(GuiGraphics g, Font font, List<ExosavantHud.Threat> threats, int cx, float intro, long now) {
      int y = 34;
      int shown = 0;

      for (ExosavantHud.Threat t : threats) {
         if (shown >= 4) {
            break;
         }

         int alpha = fade(threatAlpha(t.urgent, now), intro);
         if (alpha > 0) {
            String text = t.detail == null ? t.label : t.label + "  " + t.detail;
            float ls = 0.8F;
            int textW = Math.round((float)font.width(text) * ls);
            int gap = 4;
            int lineW = 8 + gap + textW;
            int x0 = cx - lineW / 2;
            warnTriangle(g, x0 + 4, y - 1, 8, argb(t.color, alpha), argb(329740, alpha));
            backedString(g, font, text, (float)(x0 + 8 + gap), (float)y, ls, argb(t.color, alpha));
            y += 11;
            shown++;
         }
      }
   }

   private void drawThreatArrows(
      GuiGraphics g, Minecraft mc, List<ExosavantHud.Threat> threats, float partialTick, int sw, int sh, int cx, int cy, float intro, long now
   ) {
      ExosavantHud.CamBasis cam = basis(mc, sw, sh, partialTick);
      int drawn = 0;

      for (ExosavantHud.Threat t : threats) {
         if (drawn >= 6) {
            break;
         }

         if (t.world != null) {
            drawn++;
            int alpha = fade(threatAlpha(t.urgent, now), intro);
            if (alpha > 0) {
               int col = argb(t.color, alpha);
               boolean front = project(t.world, cam.pos, cam.fwd, cam.up, cam.left, cam.tanHalfV, cam.aspect, sw, sh, this.proj);
               if (front && this.proj[0] >= 24.0F && this.proj[0] <= (float)(sw - 24) && this.proj[1] >= 24.0F && this.proj[1] <= (float)(sh - 24)) {
                  warnTriangle(g, Math.round(this.proj[0]), Math.round(this.proj[1]) - 14, 8, col, argb(329740, alpha));
               } else {
                  double dirx;
                  double diry;
                  if (front) {
                     dirx = (double)(this.proj[0] - (float)cx);
                     diry = (double)(this.proj[1] - (float)cy);
                  } else {
                     double ddx = t.world.x - cam.pos.x;
                     double ddy = t.world.y - cam.pos.y;
                     double ddz = t.world.z - cam.pos.z;
                     double hC = -(ddx * (double)cam.left.x() + ddy * (double)cam.left.y() + ddz * (double)cam.left.z());
                     dirx = hC;
                     diry = Math.abs(hC) + 1.0;
                  }

                  this.drawEdgeArrow(g, cx, cy, sw, sh, dirx, diry, col);
               }
            }
         }
      }
   }

   private static int threatAlpha(boolean urgent, long now) {
      return !urgent ? 220 : (int)(235.0 * (0.65 + 0.35 * Math.sin((double)now / 170.0)));
   }

   private static void warnTriangle(GuiGraphics g, int cx, int topY, int s, int color, int markColor) {
      for (int row = 0; row < s; row++) {
         int half = Math.round((float)row / (float)(s - 1) * (float)s * 0.5F);
         g.fill(cx - half, topY + row, cx + half + 1, topY + row + 1, color);
      }

      int barTop = topY + Math.max(2, s / 4);
      int barBot = topY + s - 3;
      if (barBot > barTop) {
         g.fill(cx, barTop, cx + 1, barBot, markColor);
      }

      g.fill(cx, topY + s - 2, cx + 1, topY + s - 1, markColor);
   }

   private void drawEdgeArrow(GuiGraphics g, int cx, int cy, int sw, int sh, double dirx, double diry, int color) {
      double len = Math.sqrt(dirx * dirx + diry * diry);
      if (!(len < 1.0E-6)) {
         double dx = dirx / len;
         double dy = diry / len;
         double left = 24.0;
         double right = (double)(sw - 24);
         double top = 24.0;
         double bottom = (double)(sh - 24);
         double tMax = Double.MAX_VALUE;
         if (dx > 1.0E-6) {
            tMax = Math.min(tMax, (right - (double)cx) / dx);
         } else if (dx < -1.0E-6) {
            tMax = Math.min(tMax, (left - (double)cx) / dx);
         }

         if (dy > 1.0E-6) {
            tMax = Math.min(tMax, (bottom - (double)cy) / dy);
         } else if (dy < -1.0E-6) {
            tMax = Math.min(tMax, (top - (double)cy) / dy);
         }

         if (tMax != Double.MAX_VALUE && !(tMax < 0.0)) {
            double ex = Math.max(left, Math.min(right, (double)cx + dx * tMax));
            double ey = Math.max(top, Math.min(bottom, (double)cy + dy * tMax));
            double bcx = ex - dx * 9.0;
            double bcy = ey - dy * 9.0;
            double px = -dy;
            fillTriangle(g, (float)ex, (float)ey, (float)(bcx + px * 5.0), (float)(bcy + dx * 5.0), (float)(bcx - px * 5.0), (float)(bcy - dx * 5.0), color);
         }
      }
   }

   private static void fillTriangle(GuiGraphics g, float ax, float ay, float bx, float by, float cx, float cy, int color) {
      int minX = (int)Math.floor((double)Math.min(ax, Math.min(bx, cx)));
      int maxX = (int)Math.ceil((double)Math.max(ax, Math.max(bx, cx)));
      int minY = (int)Math.floor((double)Math.min(ay, Math.min(by, cy)));
      int maxY = (int)Math.ceil((double)Math.max(ay, Math.max(by, cy)));

      for (int py = minY; py < maxY; py++) {
         for (int px = minX; px < maxX; px++) {
            double x = (double)px + 0.5;
            double y = (double)py + 0.5;
            double w0 = (double)(bx - ax) * (y - (double)ay) - (double)(by - ay) * (x - (double)ax);
            double w1 = (double)(cx - bx) * (y - (double)by) - (double)(cy - by) * (x - (double)bx);
            double w2 = (double)(ax - cx) * (y - (double)cy) - (double)(ay - cy) * (x - (double)cx);
            boolean neg = w0 < 0.0 || w1 < 0.0 || w2 < 0.0;
            boolean pos = w0 > 0.0 || w1 > 0.0 || w2 > 0.0;
            if (!neg || !pos) {
               g.fill(px, py, px + 1, py + 1, color);
            }
         }
      }
   }

   private void drawHeadlines(
      GuiGraphics g,
      Font font,
      int cx,
      int cy,
      int sw,
      int sh,
      long now,
      float intro,
      boolean suited,
      float lowHp,
      int theme,
      int builtMask,
      boolean chipInstalled
   ) {
      if (this.failsafeFlashUntil > now) {
         float k = (float)(this.failsafeFlashUntil - now) / 1600.0F;
         int alpha = (int)(255.0F * Math.min(1.0F, k * 1.6F));
         backedCentered(g, font, "FAILSAFE ENGAGED", cx, cy - 34, 1.6F, argb(16734810, alpha));
      } else if (suited && intro < 1.0F) {
         this.drawBootCascade(g, font, cx, cy, now, intro, theme, builtMask | (chipInstalled ? ExoPiece.CHIP.bit() : 0));
      } else if (!suited) {
         this.drawShutdown(g, font, cx, cy, now, intro, theme);
      }

      if (lowHp > 0.0F) {
         boolean blink = (now / 250L & 1L) == 0L;
         if (blink) {
            int integrity = (int)((1.0F - lowHp) * 30.0F);
            backedCentered(g, font, "WARNING  INTEGRITY " + integrity + "%", cx, sh / 5 + 4, 1.0F, argb(16728128, fade(235, intro)));
         }
      }
   }

   private void drawBootCascade(GuiGraphics g, Font font, int cx, int cy, long now, float intro, int theme, int builtMask) {
      int[] req = new int[]{
         ExoPiece.HELMET.bit(), ExoPiece.GAUNTLETS.bit(), ExoPiece.CHESTPLATE.bit(), ExoPiece.BOOTS.bit() | ExoPiece.LEGGINGS.bit(), ExoPiece.CHIP.bit()
      };
      int n = BOOT_MODULES.length;
      float scale = 0.9F;
      int pitch = 10;
      String dots = " ............ ";
      String ref = pad8("REACTOR") + dots + "OFFLINE";
      int x0 = cx - Math.round((float)font.width(ref) * scale * 0.5F);
      int y0 = cy - 62;
      boolean allOnline = true;

      for (int i = 0; i < n; i++) {
         boolean online = (builtMask & req[i]) == req[i];
         if (!online) {
            allOnline = false;
         }

         float th = (float)i / (float)(n + 1);
         if (!(intro < th)) {
            float localT = intro - th;
            int lineA = (int)(255.0F * clamp01(localT / 0.06F));
            boolean flicker = localT < 0.1F && (now / 45L & 1L) == 0L;
            int y = y0 + i * pitch;
            String lead = pad8(BOOT_MODULES[i]) + dots;
            backedString(g, font, lead, (float)x0, (float)y, scale, argb(theme, (int)((float)lineA * 0.55F)));
            int statusX = x0 + Math.round((float)font.width(lead) * scale);
            String status = online ? "ONLINE" : "OFFLINE";
            int statusCol = online ? 6356842 : 6976640;
            backedString(g, font, status, (float)statusX, (float)y, scale, argb(statusCol, flicker ? (int)((float)lineA * 0.35F) : lineA));
         }
      }

      float sumTh = (float)n / (float)(n + 1);
      if (intro >= sumTh) {
         int sa = (int)(255.0F * clamp01((intro - sumTh) / 0.08F));
         backedCentered(
            g, font, allOnline ? "▸ ALL SYSTEMS NOMINAL" : "▸ SYSTEMS PARTIAL", cx, y0 + n * pitch + 3, scale, argb(allOnline ? 6356842 : 16757066, sa)
         );
      }
   }

   private void drawShutdown(GuiGraphics g, Font font, int cx, int cy, long now, float intro, int theme) {
      int a = (int)(235.0F * clamp01(intro));
      boolean flick = (now / 60L & 1L) == 0L;
      backedCentered(g, font, "POWERING DOWN", cx, cy - 34, 1.1F, argb(flick ? 16728128 : theme, a));
      backedCentered(g, font, "▸ OFFLINE", cx, cy - 18, 0.8F, argb(theme, (int)(150.0F * clamp01(intro))));
   }

   private static String pad8(String s) {
      StringBuilder b = new StringBuilder(s);

      while (b.length() < 8) {
         b.append(' ');
      }

      return b.toString();
   }

   private void drawDamageFeedback(GuiGraphics g, int sw, int sh, long now, float dmgFlash, float lowHp, float intro) {
      float intensity = Math.max(dmgFlash, lowHp * 0.5F) * intro;
      if (!(intensity <= 0.0F)) {
         int band = sh / 5;
         int topA = (int)(110.0F * intensity);
         g.fillGradient(0, 0, sw, band, argb(16711680, topA), argb(16711680, 0));
         g.fillGradient(0, sh - band, sw, sh, argb(16711680, 0), argb(16711680, topA));
         if (dmgFlash > 0.35F) {
            int y1 = (int)(now / 40L % (long)sh);
            int y2 = (int)(now / 70L % (long)sh);
            g.fill(0, y1, sw, y1 + 1, argb(16724016, (int)(120.0F * dmgFlash * intro)));
            g.fill(0, y2, sw, y2 + 2, argb(16777215, (int)(70.0F * dmgFlash * intro)));
         }
      }
   }

   private static void diamond(GuiGraphics g, int cx, int cy, int r, int color) {
      for (int i = 0; i <= r; i++) {
         int w = r - i;
         g.fill(cx - w, cy - i, cx + w + 1, cy - i + 1, color);
         if (i != 0) {
            g.fill(cx - w, cy + i, cx + w + 1, cy + i + 1, color);
         }
      }
   }

   private static void scaledString(GuiGraphics g, Font font, String s, float x, float y, float scale, int color) {
      g.pose().pushPose();
      g.pose().translate(x, y, 0.0F);
      g.pose().scale(scale, scale, 1.0F);
      g.drawString(font, s, 0, 0, color);
      g.pose().popPose();
   }

   private static void centeredScaled(GuiGraphics g, Font font, String s, int cx, int y, float scale, int color) {
      scaledString(g, font, s, (float)cx - (float)font.width(s) * scale * 0.5F, (float)y, scale, color);
   }

   private static void backedString(GuiGraphics g, Font font, String s, float x, float y, float scale, int color) {
      plate(g, font, s, x, y, scale, color >>> 24);
      scaledString(g, font, s, x, y, scale, color);
   }

   private static void backedRight(GuiGraphics g, Font font, String s, float rx, float y, float scale, int color) {
      backedString(g, font, s, rx - (float)font.width(s) * scale, y, scale, color);
   }

   private static void backedCentered(GuiGraphics g, Font font, String s, int cx, int y, float scale, int color) {
      backedString(g, font, s, (float)cx - (float)font.width(s) * scale * 0.5F, (float)y, scale, color);
   }

   private static void plate(GuiGraphics g, Font font, String s, float x, float y, float scale, int textAlpha) {
      if (!s.isEmpty() && textAlpha > 0) {
         int w = Math.round((float)font.width(s) * scale);
         int h = Math.round(8.0F * scale);
         int ix = Math.round(x);
         int iy = Math.round(y);
         g.fill(ix - 1, iy - 1, ix + w + 1, iy + h, argb(329740, Math.min(165, (int)((float)textAlpha * 0.72F))));
      }
   }

   private static int fade(int alpha, float k) {
      return Math.round((float)alpha * clamp01(k));
   }

   private static int argb(int rgb, int alpha) {
      return (alpha & 0xFF) << 24 | rgb & 16777215;
   }

   private static int lerpColor(int a, int b, float t) {
      t = clamp01(t);
      int ar = a >> 16 & 0xFF;
      int ag = a >> 8 & 0xFF;
      int ab = a & 0xFF;
      int br = b >> 16 & 0xFF;
      int bg = b >> 8 & 0xFF;
      int bb = b & 0xFF;
      int r = Math.round((float)ar + (float)(br - ar) * t);
      int gg = Math.round((float)ag + (float)(bg - ag) * t);
      int bl = Math.round((float)ab + (float)(bb - ab) * t);
      return r << 16 | gg << 8 | bl;
   }

   private static int hpBandColor(float frac) {
      return frac < 0.3F ? 16728128 : (frac < 0.55F ? 16757066 : 6356842);
   }

   private static long nowMs() {
      return System.nanoTime() / 1000000L;
   }

   private static float clamp01(float v) {
      return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
   }

   private static float lowHpSeverity(Player player) {
      float frac = player.getHealth() / Math.max(1.0F, player.getMaxHealth());
      return frac >= 0.3F ? 0.0F : clamp01((0.3F - frac) / 0.3F);
   }

   private static record BarRow(int barX0, int barX1, int barTop, int barH) {
   }

   private static final class CamBasis {
      private Vec3 pos;
      private Vector3fc fwd;
      private Vector3fc up;
      private Vector3fc left;
      private float tanHalfV;
      private float aspect;
   }

   private static record Dossier(String name, int hp, int maxHp, float hpFrac, int threat, int dist, List<String> effects) {
      static ExosavantHud.Dossier of(Player viewer, LivingEntity foe) {
         float maxHp = Math.max(1.0F, foe.getMaxHealth());
         List<String> effects = new ArrayList<>(3);

         for (MobEffectInstance mei : foe.getActiveEffects()) {
            if (effects.size() >= 3) {
               break;
            }

            effects.add(((MobEffect)mei.getEffect().value()).getDisplayName().getString().toUpperCase(Locale.ROOT));
         }

         return new ExosavantHud.Dossier(
            foe.getDisplayName().getString().toUpperCase(Locale.ROOT),
            (int)Math.ceil((double)foe.getHealth()),
            (int)maxHp,
            ExosavantHud.clamp01(foe.getHealth() / maxHp),
            ExosavantHud.threatRating(foe),
            (int)Math.round(Math.sqrt(foe.distanceToSqr(viewer))),
            effects
         );
      }

      static ExosavantHud.Dossier sample() {
         return new ExosavantHud.Dossier(sampleText("name"), 18, 24, 0.75F, 3, 12, List.of(sampleText("effect")));
      }

      private static String sampleText(String key) {
         return Component.translatable("awakened.hud.exo_target_dossier.sample_" + key).getString().toUpperCase(Locale.ROOT);
      }
   }

   private static record FailsafeReadout(boolean armed, boolean ready, boolean engaged, float recharge) {
      static ExosavantHud.FailsafeReadout of(ExoVisual vis, boolean engaged, float recharge) {
         return new ExosavantHud.FailsafeReadout(vis.failsafeArmed(), vis.failsafeReady(), engaged, recharge);
      }

      static ExosavantHud.FailsafeReadout sample() {
         return new ExosavantHud.FailsafeReadout(true, true, false, 0.0F);
      }
   }

   private static record FlightReadout(
      boolean jet, boolean afterburner, double speed, int alt, int verticalSpeed, float fuelFrac, boolean infiniteFuel, int secondsLeft, int boostCd
   ) {
      static ExosavantHud.FlightReadout of(
         ClientPowerState state, Player player, boolean jet, boolean afterburner, double speed, int verticalSpeed, int boostCd
      ) {
         float max = state.staminaMax();
         float drainPerTick = jet ? 0.5F : 0.12F;
         return new ExosavantHud.FlightReadout(
            jet,
            afterburner,
            speed,
            (int)Math.floor(player.getY()),
            verticalSpeed,
            max > 0.0F ? ExosavantHud.clamp01(state.stamina() / max) : 0.0F,
            state.infiniteStamina(),
            drainPerTick > 0.0F ? (int)(state.stamina() / (drainPerTick * 20.0F)) : 0,
            boostCd
         );
      }

      static ExosavantHud.FlightReadout sample(ClientPowerState state, Player player) {
         return of(state, player, true, false, 23.799999999999997, 0, 0);
      }
   }

   private static record Rail(int outerX, boolean mirrored) {
      static ExosavantHud.Rail of(ExoCorner corner, HudPlacement.Box box) {
         return new ExosavantHud.Rail(corner.right() ? box.right() : box.x(), corner.right());
      }

      int x(int u, int w) {
         return this.mirrored ? this.outerX - u - w : this.outerX + u;
      }

      float textX(Font font, String s, float scale, int u) {
         return this.mirrored ? (float)(this.outerX - u) - (float)font.width(s) * scale : (float)(this.outerX + u);
      }

      float textXIn(Font font, String s, float scale, int u) {
         return this.mirrored ? (float)(this.outerX - u) : (float)(this.outerX + u) - (float)font.width(s) * scale;
      }
   }

   private static final class Threat {
      private final String label;
      private final String detail;
      private final int color;
      private final boolean urgent;
      private final Vec3 world;
      private final double sortKey;

      private Threat(String label, String detail, int color, boolean urgent, Vec3 world, double sortKey) {
         this.label = label;
         this.detail = detail;
         this.color = color;
         this.urgent = urgent;
         this.world = world;
         this.sortKey = sortKey;
      }
   }

   public abstract static class VisorPanel implements MovableHud {
      private final String id;
      private final int width;
      private final int height;
      private final int insetX;
      private final int insetY;
      private final BooleanValue on;
      private final EnumValue<ExoCorner> berth;
      private final ExoCorner shipped;

      private VisorPanel(String id, int width, int height, int insetX, int insetY, BooleanValue on, EnumValue<ExoCorner> berth, ExoCorner shipped) {
         this.id = id;
         this.width = width;
         this.height = height;
         this.insetX = insetX;
         this.insetY = insetY;
         this.on = on;
         this.berth = berth;
         this.shipped = shipped;
      }

      public final ExoCorner corner() {
         return (ExoCorner)this.berth.get();
      }

      @Override
      public String hudId() {
         return this.id;
      }

      @Override
      public int hudWidth() {
         return this.width;
      }

      @Override
      public int hudHeight() {
         return this.height;
      }

      @Override
      public boolean hudRelevant() {
         return "exosavant".equals(ClientPowerState.get().powerId());
      }

      @Override
      public boolean enabled() {
         return (Boolean)this.on.get();
      }

      @Override
      public void setEnabled(boolean value) {
         this.write((Boolean)this.on.get() != value, () -> this.on.set(value));
      }

      @Override
      public HudPlacement.Box layout(int screenW, int screenH) {
         return this.boxIn(this.corner(), this.width, this.height, screenW, screenH);
      }

      @Override
      public HudPlacement.Box previewDrop(int x, int y, int w, int h, int screenW, int screenH) {
         return this.boxIn(this.nearestBerth(x, y, w, h, screenW, screenH), w, h, screenW, screenH);
      }

      @Override
      public void place(int x, int y, int w, int h, int screenW, int screenH) {
         this.moveTo(this.nearestBerth(x, y, w, h, screenW, screenH));
      }

      @Override
      public void resetPlacement() {
         this.moveTo(this.shipped);
      }

      @Override
      public boolean hasStoredPlacement() {
         return this.corner() != this.shipped;
      }

      @Override
      public boolean snapsItself() {
         return true;
      }

      private HudPlacement.Box boxIn(ExoCorner corner, int w, int h, int screenW, int screenH) {
         return new HudPlacement.Box(corner.originX(screenW, this.insetX, w), corner.originY(screenH, this.insetY, h), w, h);
      }

      private ExoCorner nearestBerth(int x, int y, int w, int h, int screenW, int screenH) {
         double dropX = (double)x + (double)w / 2.0;
         double dropY = (double)y + (double)h / 2.0;
         ExoCorner best = this.corner();
         double bestDist = Double.MAX_VALUE;

         for (ExoCorner candidate : ExoCorner.values()) {
            HudPlacement.Box box = this.boxIn(candidate, w, h, screenW, screenH);
            double dx = (double)box.x() + (double)w / 2.0 - dropX;
            double dy = (double)box.y() + (double)h / 2.0 - dropY;
            double dist = dx * dx + dy * dy;
            if (dist < bestDist) {
               bestDist = dist;
               best = candidate;
            }
         }

         return best;
      }

      private void moveTo(ExoCorner corner) {
         this.write(this.berth.get() != corner, () -> this.berth.set(corner));
      }

      private void write(boolean isNew, Runnable change) {
         if (isNew) {
            change.run();
            ClientConfig.save();
         }
      }

      @Override
      public final void drawPreview(GuiGraphics g, HudPlacement.Box box) {
         this.drawStandIn(g, box, this.nearestBerth(box.x(), box.y(), box.width(), box.height(), g.guiWidth(), g.guiHeight()));
      }

      protected abstract void drawStandIn(GuiGraphics var1, HudPlacement.Box var2, ExoCorner var3);
   }
}
