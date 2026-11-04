package com.devilfruits.client.dev;

import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.core.config.CoreClientConfig;
import com.devilfruits.mobs.pirate.Pirates;
import com.devilfruits.mobs.pirate.entity.PirateFighter;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public abstract class CaptureStage implements AutoTest.Script {
   protected static final double START_Z = -2.5;
   protected static final double DUMMY_Z = -10.5;
   protected final String prefix;

   protected CaptureStage(String prefix) {
      this.prefix = prefix;
   }

   protected abstract ResourceLocation fruit();

   protected void setup(ServerPlayer p) {
   }

   protected void onReset(ServerPlayer p) {
   }

   protected abstract int takes(AutoTest var1, int var2);

   public static void command(ServerPlayer p, String cmd) {
      p.getServer().getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(), cmd);
   }

   public static void castAbility(ServerPlayer p, String ability, int hold) {
      command(p, "devilfruit cast " + ability + " " + hold);
   }

   private static void arena(AutoTest s, ServerPlayer p) {
      ServerLevel level = p.serverLevel();
      int y = s.origin().getY();

      for (int x = -7; x <= 7; x++) {
         for (int z = -16; z <= 2; z++) {
            boolean edge = x == -7 || x == 7 || z == -16 || z == 2;
            level.setBlockAndUpdate(new BlockPos(x, y - 1, z), edge ? Blocks.POLISHED_ANDESITE.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState());
         }
      }

      command(p, "weather clear");
      command(p, "gamerule doDaylightCycle false");
      command(p, "gamerule mobGriefing false");
   }

   protected static void cave(AutoTest s, ServerPlayer p, boolean build) {
      ServerLevel level = p.serverLevel();
      int y = s.origin().getY();
      BlockState wall = build ? Blocks.DEEPSLATE_BRICKS.defaultBlockState() : Blocks.AIR.defaultBlockState();

      for (int x = -20; x <= 20; x++) {
         for (int z = -30; z <= 14; z++) {
            boolean side = x == -20 || x == 20 || z == -30 || z == 14;
            level.setBlock(new BlockPos(x, y + 13, z), wall, 2);
            if (side) {
               for (int h = 0; h < 13; h++) {
                  level.setBlock(new BlockPos(x, y + h, z), wall, 2);
               }
            }
         }
      }
   }

   protected static void reset(AutoTest s, ServerPlayer p) {
      ServerLevel level = p.serverLevel();
      double y = (double)s.origin().getY();
      AABB area = new AABB(-30.0, y - 10.0, -40.0, 30.0, y + 30.0, 20.0);
      level.getEntitiesOfClass(PirateFighter.class, area).forEach(e -> e.discard());
      level.getEntitiesOfClass(Husk.class, area).forEach(e -> e.discard());
      PirateFighter dummy = (PirateFighter)Pirates.DECKHAND.get().create(level);
      if (dummy != null) {
         dummy.moveTo(0.5, y, -10.5, 0.0F, 0.0F);
         dummy.setYHeadRot(0.0F);
         dummy.setYBodyRot(0.0F);
         dummy.setNoAi(true);
         dummy.setPersistenceRequired();
         dummy.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack((ItemLike)Pirates.CUTLASS.get()));
         dummy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(4000.0);
         dummy.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
         dummy.setHealth(4000.0F);
         level.addFreshEntity(dummy);
      }

      p.teleportTo(level, 0.5, y, -2.5, 180.0F, 8.0F);
      p.setDeltaMovement(Vec3.ZERO);
   }

   protected static void dummyTo(AutoTest s, ServerPlayer p, double z) {
      double y = (double)s.origin().getY();
      p.serverLevel()
         .getEntitiesOfClass(PirateFighter.class, new AABB(-30.0, y - 10.0, -40.0, 30.0, y + 30.0, 20.0))
         .forEach(e -> e.moveTo(0.5, y, z, 0.0F, 0.0F));
   }

   protected static void quality(CoreClientConfig.CinematicQuality q) {
      CoreClientConfig.CINEMATIC_QUALITY.set(q);
   }

   protected int take(
      AutoTest s,
      int t,
      int rate,
      Consumer<ServerPlayer> prep,
      int prepTicks,
      Consumer<ServerPlayer> cast,
      Consumer<Minecraft> camera,
      int length,
      CaptureStage.Beats beats
   ) {
      s.add(t, mc -> s.server(mc, p -> {
            mc.execute(VfxParticles::clear);
            reset(s, p);
            this.onReset(p);
            command(p, "tick rate 20");
            prep.accept(p);
         }));
      int c = t + 10 + prepTicks;
      s.add(c - 5, mc -> s.server(mc, p -> command(p, "tick rate " + rate)));
      s.add(c - 3, camera);
      s.add(c, mc -> s.server(mc, cast));
      beats.add(c);
      return c + length;
   }

   protected static Consumer<ServerPlayer> none() {
      return p -> {
      };
   }

   protected static Consumer<Minecraft> rig(double dist, float yaw, float pitch, double lift) {
      return mc -> {
         mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
         if (mc.player != null) {
            CaptureRig.follow(mc.player, dist, yaw, pitch, lift);
         }
      };
   }

   protected static Consumer<Minecraft> firstPerson() {
      return mc -> {
         CaptureRig.off();
         mc.options.setCameraType(CameraType.FIRST_PERSON);
      };
   }

   protected void shot(AutoTest s, int tick, String name) {
      s.add(tick, mc -> s.shot(mc, this.prefix + name));
   }

   protected void clip(AutoTest s, int tick, int frames) {
      s.add(tick, mc -> VfxCapture.clip(this.prefix + "clip", frames, 0.6666666666666666));
   }

   @Override
   public int write(AutoTest s, int start) {
      s.add(start, mc -> {
         VfxCapture.exactWindow(1920, 1080);
         mc.options.hideGui = true;
         mc.options.pauseOnLostFocus = false;
         quality(CoreClientConfig.CinematicQuality.ULTRA);
         CoreClientConfig.REDUCE_FLASHING.set(false);
         s.refresh(mc, this.fruit());
      });
      s.add(start + 4, mc -> s.server(mc, p -> {
            arena(s, p);
            command(p, "time set 15000");
            this.setup(p);
         }));
      int t = this.takes(s, start + 70);
      s.add(t, mc -> {
         quality(CoreClientConfig.CinematicQuality.ULTRA);
         CaptureRig.off();
         mc.options.hideGui = false;
         mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
      });
      s.add(t + 1, mc -> s.server(mc, p -> {
            this.onReset(p);
            command(p, "tick rate 20");
         }));
      return t + 5;
   }

   @FunctionalInterface
   protected interface Beats {
      void add(int var1);
   }
}
