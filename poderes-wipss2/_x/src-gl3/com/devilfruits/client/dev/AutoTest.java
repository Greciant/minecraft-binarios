package com.devilfruits.client.dev;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.ClientFruitState;
import com.devilfruits.core.CoreEvents;
import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.FruitRegistry;
import com.devilfruits.core.network.AbilityInputPayload;
import com.devilfruits.core.rules.FruitRules;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.registry.DFAttachments;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent.Init.Post;
import net.neoforged.neoforge.network.PacketDistributor;

public final class AutoTest {
   public static final boolean ENABLED = Boolean.getBoolean("devilfruits.autotest");
   private static final String ONLY = System.getProperty("devilfruits.autotest.fruits", "");
   private static final Map<ResourceLocation, AutoTest.Script> SCRIPTS = new LinkedHashMap<>();
   private static final List<AutoTest.Step> STEPS = new ArrayList<>();
   private static final AutoTest INSTANCE = new AutoTest();
   private static boolean built;
   private static boolean worldRequested;
   private static int tick = -1;
   private static int cursor;
   private static BlockPos origin = BlockPos.ZERO;

   public static void addScript(ResourceLocation fruit, AutoTest.Script script) {
      if (ENABLED) {
         SCRIPTS.put(fruit, script);
      }
   }

   private static void build() {
      built = true;
      int t = 60;
      INSTANCE.add(t, mc -> INSTANCE.server(mc, AutoTest::setup));
      t += 30;

      for (Entry<ResourceLocation, AutoTest.Script> e : SCRIPTS.entrySet()) {
         if (ONLY.isEmpty() || List.of(ONLY.split(",")).contains(e.getKey().getPath())) {
            INSTANCE.add(t, mc -> DevilFruits.LOGGER.info("[devilfruits autotest] script {}", e.getKey()));
            t = e.getValue().write(INSTANCE, t + 1) + 20;
         }
      }

      INSTANCE.add(t, mc -> {
         DevilFruits.LOGGER.info("[devilfruits autotest] complete");
         mc.stop();
      });
      STEPS.sort((a, b) -> Integer.compare(a.at, b.at));
   }

   public void add(int at, Consumer<Minecraft> action) {
      STEPS.add(new AutoTest.Step(at, action));
   }

   public void server(Minecraft mc, Consumer<ServerPlayer> action) {
      MinecraftServer server = mc.getSingleplayerServer();
      if (server != null && mc.player != null) {
         UUID id = mc.player.getUUID();
         server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) {
               action.accept(player);
            }
         });
      }
   }

   public BlockPos origin() {
      return origin;
   }

   public void refresh(Minecraft mc, ResourceLocation fruitId) {
      this.server(mc, p -> {
         DevilFruit fruit = FruitRegistry.get(fruitId);
         if (fruit != null) {
            PlayerFruitData data = DFAttachments.data(p);
            if (!fruitId.equals(data.fruitId())) {
               FruitRules.grantPower(p, fruit, false);
            }

            Mastery.setLevel(p, data, 10, false);
            data.clearCooldowns();
            data.setEnergy((float)Mastery.maxEnergy(data));
            p.teleportTo(p.serverLevel(), 0.5, (double)origin.getY(), 0.5, 0.0F, 12.0F);
            CoreEvents.syncNow(p);
         }
      });
   }

   public void press(Ability ability) {
      FruitForm form = ClientFruitState.form();
      if (form != null && form.indexOf(ability.id()) >= 0) {
         ClientFruitState.select(form.indexOf(ability.id()));
      }

      PacketDistributor.sendToServer(AbilityInputPayload.press(ability.id()), new CustomPacketPayload[0]);
   }

   public void release(Ability ability) {
      PacketDistributor.sendToServer(AbilityInputPayload.release(ability.id()), new CustomPacketPayload[0]);
   }

   public void tap(Ability ability) {
      this.press(ability);
      this.release(ability);
   }

   public void camera(Minecraft mc, CameraType type) {
      mc.options.setCameraType(type);
   }

   public void shot(Minecraft mc, String name) {
      Screenshot.grab(
         mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), message -> DevilFruits.LOGGER.info("[devilfruits autotest] screenshot {}", name)
      );
   }

   @SubscribeEvent
   public static void onScreen(Post event) {
      if (!worldRequested && event.getScreen() instanceof TitleScreen) {
         worldRequested = true;
         Minecraft mc = Minecraft.getInstance();
         GameRules rules = new GameRules();
         ((BooleanValue)rules.getRule(GameRules.RULE_DAYLIGHT)).set(false, null);
         ((BooleanValue)rules.getRule(GameRules.RULE_WEATHER_CYCLE)).set(false, null);
         ((BooleanValue)rules.getRule(GameRules.RULE_DOMOBSPAWNING)).set(false, null);
         LevelSettings settings = new LevelSettings(
            "devilfruits_autotest_" + System.currentTimeMillis(), GameType.CREATIVE, false, Difficulty.EASY, true, rules, WorldDataConfiguration.DEFAULT
         );
         mc.createWorldOpenFlows()
            .createFreshLevel(
               settings.levelName(),
               settings,
               new WorldOptions(42L, false, false),
               registries -> ((WorldPreset)registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value())
                     .createWorldDimensions(),
               event.getScreen()
            );
      }
   }

   @SubscribeEvent
   public static void onTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null && mc.getSingleplayerServer() != null) {
         if (!built) {
            build();
         }

         tick++;

         for (; cursor < STEPS.size() && STEPS.get(cursor).at <= tick; cursor++) {
            try {
               STEPS.get(cursor).action.accept(mc);
            } catch (RuntimeException var3) {
               DevilFruits.LOGGER.error("[devilfruits autotest] step {} failed", cursor, var3);
            }
         }
      }
   }

   private static void setup(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      level.setDayTime(6000L);
      int y = level.getHeight(Types.MOTION_BLOCKING, 0, 0);
      origin = new BlockPos(0, y, 0);
      player.teleportTo(level, 0.5, (double)y, 0.5, 0.0F, 12.0F);

      for (int x = -6; x <= 6; x++) {
         for (int z = 3; z <= 16; z++) {
            level.setBlockAndUpdate(new BlockPos(x, y - 1, z), Blocks.WATER.defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(x, y - 2, z), Blocks.WATER.defaultBlockState());
         }
      }

      for (int i = -2; i <= 2; i++) {
         Mob husk = (Mob)EntityType.HUSK.create(level);
         if (husk != null) {
            husk.moveTo((double)i * 1.6 + 0.5, (double)y, 4.5, 180.0F, 0.0F);
            husk.setNoAi(true);
            husk.setPersistenceRequired();
            level.addFreshEntity(husk);
         }
      }

      Minecraft.getInstance().execute(() -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK));
   }

   private AutoTest() {
   }

   @FunctionalInterface
   public interface Script {
      int write(AutoTest var1, int var2);
   }

   private static record Step(int at, Consumer<Minecraft> action) {
   }
}
