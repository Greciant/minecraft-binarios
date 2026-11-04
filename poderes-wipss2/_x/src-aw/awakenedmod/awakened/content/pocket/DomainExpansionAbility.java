package com.awakenedmod.awakened.content.pocket;

import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.TeleportUtil;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import com.mojang.math.Transformation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Brightness;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Display.BlockDisplay;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class DomainExpansionAbility extends Ability {
   private static final float UPFRONT_COST = 30.0F;
   private static final int UNLOCK = 11;
   private static final float DRAIN_PER_TICK = 0.45F;
   private static final AbilityScaling.Entry RADIUS = AbilityScaling.radiusPerPointEntry(9.0, 1.2, 20);
   private static final int SYNC_INTERVAL = 5;
   private static final int RENDER_INTERVAL = 6;
   private static final int EFFECT_REFRESH = 40;
   private static final float DAMAGE_PER_SECOND = 1.5F;
   private static final int DAMAGE_INTERVAL = 20;
   private static final int SIGHT_INTERVAL = 5;
   private static final int BUILD_TICKS = 26;
   private static final float PANEL_THIN = 0.2F;
   private static final double CENTER_LIFT = 1.0;
   private static final DustParticleOptions EDGE_DUST = Fx.dust(9064447, 1.6F);
   private final Map<UUID, DomainExpansionAbility.Dome> domes = this.playerState();

   public DomainExpansionAbility() {
      super("domain_expansion", 30.0F, 11);
   }

   @Override
   public float staminaCost(ServerPlayer player) {
      return 0.0F;
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      UUID id = player.getUUID();
      if (this.domes.containsKey(id)) {
         this.beginCollapse(player, this.domes.get(id));
         return true;
      } else {
         PowerData data = PowerService.data(player);
         float upfront = this.billed(30.0F);
         if (!data.infiniteStamina() && data.stamina() < upfront) {
            return this.fail(player, "awakened.feedback.no_stamina", new Object[0]);
         } else {
            if (!data.infiniteStamina()) {
               data.setStamina(data.stamina() - upfront);
            }

            double radius = RADIUS.perPointValue(MasteryScaling.abilityPower(player));
            Vec3 center = player.position();
            ServerLevel level = player.serverLevel();
            DomainExpansionAbility.Dome dome = new DomainExpansionAbility.Dome(center, radius, this.computeSpecs(center, radius));
            this.domes.put(id, dome);
            DomainManager.set(id, new DomainManager.Dome(id, level.dimension(), center, radius));
            Fx.burst(level, ParticleTypes.SONIC_BOOM, center.add(0.0, 1.0, 0.0), 1, 0.0, 0.0);
            Fx.sound(level, center, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.45F, 0.55F);
            Fx.sound(level, center, SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.4F, 0.5F);
            Fx.sound(level, center, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.4F, 0.5F);
            this.applyEffects(player, level, dome);
            PowerService.playAnimation(player, "domain_expansion");
            player.displayClientMessage(Component.translatable("awakened.pocket.domain_open").withStyle(ChatFormatting.DARK_PURPLE), true);
            PowerService.sync(player);
            return true;
         }
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      DomainExpansionAbility.Dome dome = this.domes.get(player.getUUID());
      if (dome != null) {
         ServerLevel level = player.serverLevel();
         if (dome.collapsing) {
            if (dome.panels.isEmpty()) {
               this.finishCollapse(player, dome);
            } else {
               this.collapseStep(level, dome);
            }
         } else {
            if (dome.built < dome.specs.size()) {
               this.buildStep(level, dome);
               if (dome.built >= dome.specs.size()) {
                  this.creditSeal(player, level, dome);
               }
            }

            this.seal(player, level, dome);
            if (player.tickCount % 5 == 0) {
               DomainSight.dropFar(player, level);
            }

            if (dome.built >= dome.specs.size() && player.tickCount % 20 == 0) {
               this.hurtCreatures(player, level, dome);
            }

            if (player.tickCount % 6 == 0) {
               this.render(level, dome);
            }

            if (player.tickCount % 40 == 0) {
               this.applyEffects(player, level, dome);
            }

            PowerData data = PowerService.data(player);
            if (!data.infiniteStamina()) {
               float drain = this.billed(0.45F);
               float remaining = StaminaSystem.drainSustained(data, level.getGameTime(), drain);
               if (remaining <= 0.0F && drain > 0.0F) {
                  this.beginCollapse(player, dome);
                  return;
               }

               if (player.tickCount % 5 == 0) {
                  PowerService.sync(player);
               }
            }
         }
      }
   }

   private List<DomainExpansionAbility.PanelSpec> computeSpecs(Vec3 center, double radius) {
      List<DomainExpansionAbility.PanelSpec> specs = new ArrayList<>();
      int n = (int)Mth.clamp(radius * radius * 1.3, 90.0, 200.0);
      double arc = Math.sqrt((Math.PI * 4) / (double)n);
      float panel = (float)(radius * arc * 1.7);
      double golden = Math.PI * (3.0 - Math.sqrt(5.0));
      Vec3 origin = center.add(0.0, 1.0, 0.0);

      for (int i = 0; i < n; i++) {
         double y = 1.0 - ((double)i + 0.5) * 2.0 / (double)n;
         double ring = Math.sqrt(Math.max(0.0, 1.0 - y * y));
         double theta = golden * (double)i;
         double nx = Math.cos(theta) * ring;
         double nz = Math.sin(theta) * ring;
         Vector3f normal = new Vector3f((float)nx, (float)y, (float)nz);
         Vec3 pos = origin.add(nx * radius, y * radius, nz * radius);
         specs.add(new DomainExpansionAbility.PanelSpec(pos, normal, panel));
      }

      specs.sort((a, b) -> Double.compare(a.pos.y, b.pos.y));
      return specs;
   }

   private void buildStep(ServerLevel level, DomainExpansionAbility.Dome dome) {
      int perTick = Math.max(1, (int)Math.ceil((double)dome.specs.size() / 26.0));
      int end = Math.min(dome.specs.size(), dome.built + perTick);
      double frontY = dome.center.y;

      for (int k = dome.built; k < end; k++) {
         DomainExpansionAbility.PanelSpec s = dome.specs.get(k);
         dome.panels.add(this.spawnPanel(level, s.pos, s.normal, s.size));
         frontY = s.pos.y;
      }

      dome.built = end;
      this.sweepFront(level, dome, frontY);
      Fx.sound(level, dome.center, SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.PLAYERS, 0.4F, 0.6F);
   }

   private void sweepFront(ServerLevel level, DomainExpansionAbility.Dome dome, double frontY) {
      Vec3 origin = dome.center.add(0.0, 1.0, 0.0);
      double dy = frontY - origin.y;
      double rr = Math.sqrt(Math.max(0.25, dome.radius * dome.radius - dy * dy));
      Vec3 u = new Vec3(1.0, 0.0, 0.0);
      Vec3 v = new Vec3(0.0, 0.0, 1.0);
      int points = (int)Math.min(96.0, rr * 8.0);
      double phase = (double)(level.getGameTime() % 40L) / 40.0 * Math.PI * 2.0;
      Fx.ring(level, EDGE_DUST, new Vec3(origin.x, frontY, origin.z), u, v, rr, points, phase);
   }

   private BlockDisplay spawnPanel(ServerLevel level, Vec3 pos, Vector3f normal, float size) {
      BlockDisplay d = new BlockDisplay(EntityType.BLOCK_DISPLAY, level);
      d.setPos(pos.x, pos.y, pos.z);
      d.setBlockState(Blocks.SCULK.defaultBlockState());
      Quaternionf rot = new Quaternionf().rotationTo(0.0F, 0.0F, 1.0F, normal.x, normal.y, normal.z);
      Vector3f off = rot.transform(new Vector3f(size / 2.0F, size / 2.0F, 0.1F), new Vector3f());
      d.setTransformation(new Transformation(new Vector3f(-off.x, -off.y, -off.z), rot, new Vector3f(size, size, 0.2F), new Quaternionf()));
      d.setBrightnessOverride(new Brightness(15, 15));
      d.setViewRange(3.0F);
      d.addTag("awakened_domain");
      level.addFreshEntity(d);
      return d;
   }

   private void seal(ServerPlayer caster, ServerLevel level, DomainExpansionAbility.Dome dome) {
      double r = dome.radius;
      Vec3 origin = dome.center.add(0.0, 1.0, 0.0);

      for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(dome.center, dome.center).inflate(r + 2.0), LivingEntity::isAlive)) {
         double dist = Math.sqrt(e.position().distanceToSqr(origin));
         if (dist > r - 0.6 && dist > 0.01) {
            Vec3 inward = origin.subtract(e.position()).normalize().scale(0.6);
            Harm.setMotion(caster, e, inward.x, inward.y, inward.z);
         }

         if (dist <= r && (e == caster || Harm.allowed(caster, e))) {
            dome.held.add(e.getUUID());
         }
      }

      pullBackEscaped(caster, level, dome, origin);
   }

   private static void pullBackEscaped(ServerPlayer caster, ServerLevel level, DomainExpansionAbility.Dome dome, Vec3 origin) {
      double r = dome.radius;
      dome.held.removeIf(id -> {
         if (level.getEntity(id) instanceof LivingEntity e && e.isAlive()) {
            Vec3 out = e.position().subtract(origin);
            if (out.length() <= r + 0.5) {
               return false;
            }

            Vec3 back = origin.add(out.normalize().scale(Math.max(1.0, r - 1.5)));
            if (!TeleportUtil.fits(e, back)) {
               Vec3 safe = TeleportUtil.nearestSafeSpot(e, back, 3.0, 4);
               back = safe != null && safe.distanceTo(origin) < r ? safe : dome.center;
            }

            TeleportUtil.moveBody(e, back);
            e.setDeltaMovement(Vec3.ZERO);
            e.hurtMarked = true;
            if (e instanceof ServerPlayer player) {
               TeleportUtil.unwedge(player);
            }

            return false;
         }

         return true;
      });
   }

   private void creditSeal(ServerPlayer player, ServerLevel level, DomainExpansionAbility.Dome dome) {
      double r = dome.radius;
      List<LivingEntity> caught = level.getEntitiesOfClass(
         LivingEntity.class,
         new AABB(dome.center, dome.center).inflate(r),
         e -> e.isAlive() && Foes.counts(player, e) && e.position().distanceToSqr(dome.center) <= r * r
      );
      if (caught.size() >= 3) {
         QuestSystem.feat(player, "rift_domain_seal");
      }
   }

   private void hurtCreatures(ServerPlayer caster, ServerLevel level, DomainExpansionAbility.Dome dome) {
      double r = dome.radius;
      Vec3 origin = dome.center.add(0.0, 1.0, 0.0);
      List<LivingEntity> inside = level.getEntitiesOfClass(
         LivingEntity.class,
         new AABB(origin, origin).inflate(r),
         e -> e.isAlive() && !(e instanceof Player) && !(e instanceof ArmorStand) && e.position().distanceToSqr(origin) <= r * r
      );
      float damage = MasteryScaling.scaledDamage(1.5F, caster, this.balanceKey());
      DomainSight.ambient(() -> {
         for (LivingEntity e : Harm.hostilesOnly(caster, inside)) {
            if (Dmg.hurtNoKnockback(e, level, Dmg.by(caster, DamageTypes.MAGIC), damage)) {
               this.tickMark(level, e);
            }
         }
      });
   }

   private void tickMark(ServerLevel level, LivingEntity e) {
      double width = Math.max(0.5, (double)e.getBbWidth());
      Vec3 body = e.position().add(0.0, (double)e.getBbHeight() * 0.5, 0.0);
      Fx.burst(level, EDGE_DUST, body, 5, width * 0.45, 0.01);
      Fx.burst(level, ParticleTypes.REVERSE_PORTAL, body, 3, width * 0.4, 0.02);
   }

   private void applyEffects(ServerPlayer caster, ServerLevel level, DomainExpansionAbility.Dome dome) {
      caster.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false));
      double r = dome.radius;

      for (ServerPlayer e : level.getEntitiesOfClass(
         ServerPlayer.class,
         new AABB(dome.center, dome.center).inflate(r),
         ex -> ex != caster && ex.isAlive() && ex.position().distanceToSqr(dome.center) <= r * r
      )) {
         Harm.effect(caster, e, new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
      }
   }

   private void render(ServerLevel level, DomainExpansionAbility.Dome dome) {
      Vec3 u = new Vec3(1.0, 0.0, 0.0);
      Vec3 v = new Vec3(0.0, 0.0, 1.0);
      int ringPoints = (int)Math.min(80.0, dome.radius * 4.0);
      double phase = (double)(level.getGameTime() % 80L) / 80.0 * Math.PI * 2.0;
      Fx.ring(level, EDGE_DUST, dome.center.add(0.0, 0.1, 0.0), u, v, dome.radius - 1.0, ringPoints, phase);
      Fx.burst(level, ParticleTypes.SCULK_SOUL, dome.center.add(0.0, 1.0, 0.0), 4, dome.radius * 0.5, 0.02);
   }

   private void beginCollapse(ServerPlayer player, DomainExpansionAbility.Dome dome) {
      if (!dome.collapsing) {
         dome.collapsing = true;
         DomainManager.clear(player.getUUID());
         ServerLevel level = player.serverLevel();
         player.removeEffect(MobEffects.NIGHT_VISION);
         PowerService.playAnimation(player, "domain_expansion_close");
         Fx.sound(level, dome.center, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.45F, 1.2F);
         Fx.sound(level, dome.center, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.4F, 0.6F);
         PowerService.sync(player);
      }
   }

   private void collapseStep(ServerLevel level, DomainExpansionAbility.Dome dome) {
      int perTick = Math.max(1, (int)Math.ceil((double)dome.specs.size() / 26.0));
      double frontY = dome.center.y;

      for (int i = 0; i < perTick && !dome.panels.isEmpty(); i++) {
         BlockDisplay panel = dome.panels.remove(dome.panels.size() - 1);
         frontY = panel.getY();
         discardPanel(panel);
      }

      this.sweepFront(level, dome, frontY);
      Fx.sound(level, dome.center, SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.PLAYERS, 0.4F, 0.5F);
   }

   private static void discardPanel(BlockDisplay panel) {
      if (panel.level() instanceof ServerLevel level && level.getEntity(panel.getUUID()) instanceof BlockDisplay live) {
         live.discard();
      }

      panel.discard();
   }

   private void finishCollapse(ServerPlayer player, DomainExpansionAbility.Dome dome) {
      this.domes.remove(player.getUUID());
      ServerLevel level = player.serverLevel();
      Fx.burst(level, ParticleTypes.REVERSE_PORTAL, dome.center.add(0.0, 1.0, 0.0), 40, dome.radius * 0.3, 0.2);
      Fx.sound(level, dome.center, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.5F, 0.7F);
      PowerService.sync(player);
   }

   private void end(ServerPlayer player) {
      DomainExpansionAbility.Dome dome = this.domes.remove(player.getUUID());
      DomainManager.clear(player.getUUID());
      if (dome != null) {
         for (BlockDisplay panel : dome.panels) {
            discardPanel(panel);
         }

         player.removeEffect(MobEffects.NIGHT_VISION);
      }
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      if (this.domes.containsKey(player.getUUID())) {
         this.end(player);
      }

      DomainManager.clear(player.getUUID());
      super.clearPlayer(player);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().value("damage_per_second", AbilityScaling.Unit.RAW, 1.5).add(RADIUS).build();
   }

   private static final class Dome {
      private final Vec3 center;
      private final double radius;
      private final List<DomainExpansionAbility.PanelSpec> specs;
      private final List<BlockDisplay> panels = new ArrayList<>();
      private final Set<UUID> held = new HashSet<>();
      private int built;
      private boolean collapsing;

      private Dome(Vec3 center, double radius, List<DomainExpansionAbility.PanelSpec> specs) {
         this.center = center;
         this.radius = radius;
         this.specs = specs;
      }
   }

   private static record PanelSpec(Vec3 pos, Vector3f normal, float size) {
   }
}
