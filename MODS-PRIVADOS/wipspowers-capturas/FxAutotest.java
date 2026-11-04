package wips.powers.devtest;

import com.awakenedmod.awakened.core.PowerService;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;
import wips.powers.WipsPowers;

/**
 * Prueba automatica (runFx, -Dwips.autotest=true): mundo plano, da el poder, lanza cada tecnica
 * y guarda capturas en run-fx/screenshots/wips_*.png. Log en run-fx/wips_autotest.txt.
 * WIPS_ONLY=0..5 limita a una tecnica (5 = Island Shaker de Gamma).
 */
@EventBusSubscriber(modid = WipsPowers.MOD_ID, value = Dist.CLIENT)
public final class FxAutotest {
    private static final boolean ENABLED = Boolean.getBoolean("wips.autotest");
    private static final String ONLY = System.getenv("WIPS_ONLY");

    /** nombre, poder, slot, ticks hasta soltar (0 = instantanea) */
    private record Cast(String name, String power, int slot, int hold) {}

    private static final Cast[] CASTS = {
        new Cast("0_thunder_bagua", "seiryuu", 0, 0),
        new Cast("1_flame_cloud", "seiryuu", 1, 0),
        new Cast("2_draconic_thunder", "seiryuu", 2, 45),
        new Cast("3_bolo_barrage", "seiryuu", 3, 30),
        new Cast("4_blast_breath", "seiryuu", 4, 30),
        new Cast("5_island_shaker", "gamma", 1, 0),
    };
    private static final int[] SHOTS = {2, 6, 10, 15, 22, 30, 40, 52, 66, 82, 100};
    private static final int SPAN = 150;
    private static final int START = 60;

    private static final List<String> OUT = new ArrayList<>();
    private static final List<Cast> PLAN = new ArrayList<>();
    private static boolean created;
    private static int t = -1;

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (!ENABLED) return;
        Minecraft mc = Minecraft.getInstance();
        if (!created && mc.screen instanceof AccessibilityOnboardingScreen) {
            mc.options.onboardAccessibility = false;
            mc.setScreen(new TitleScreen());
            return;
        }
        if (!created && mc.screen instanceof TitleScreen) {
            created = true;
            for (Cast c : CASTS) {
                if (ONLY == null || c.name().startsWith(ONLY + "_")) PLAN.add(c);
            }
            createWorld(mc);
            return;
        }
        if (mc.player == null || mc.getSingleplayerServer() == null) return;
        t++;
        MinecraftServer server = mc.getSingleplayerServer();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);

        if (t == 1) {
            long win = mc.getWindow().getWindow();
            GLFW.glfwSetWindowAttrib(win, GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
            GLFW.glfwRestoreWindow(win);
            GLFW.glfwSetWindowSize(win, 1280, 720);
            mc.options.hideGui = true;
        }
        if (t == 30) server.execute(() -> setup(server));
        if (t == 40) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);

        int rel = t - START;
        if (rel < 0) return;
        int idx = rel / SPAN;
        int dt = rel % SPAN;
        if (idx >= PLAN.size()) {
            if (dt == 10) { write(mc); mc.stop(); }
            return;
        }
        Cast c = PLAN.get(idx);
        if (dt == 0) server.execute(() -> prepare(server, c));
        if (dt == 20) server.execute(() -> {
            ServerPlayer p = player(server);
            PowerService.activate(p, c.slot());
            log(c.name() + ": activada (poder=" + PowerService.data(p).powerId() + ")");
        });
        if (c.hold() > 0 && dt == 20 + c.hold()) server.execute(() -> {
            PowerService.release(player(server), c.slot());
            log(c.name() + ": soltada");
        });
        for (int s : SHOTS) {
            if (dt == 20 + s) shot(mc, c.name() + "_" + String.format("%03d", s));
        }
        if (dt == 20 + 30) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        }
        if (dt == 20 + 31) {
            shot(mc, c.name() + "_frente");
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
    }

    private static void createWorld(Minecraft mc) {
        String name = "wips_fx_" + System.currentTimeMillis();
        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        LevelSettings settings = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.NORMAL, true,
            rules, WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(name, settings, new WorldOptions(12345L, false, false),
            reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
                .value().createWorldDimensions(), mc.screen);
        log("Creando mundo plano " + name + " plan=" + PLAN.size());
    }

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().get(0);
    }

    private static void setup(MinecraftServer server) {
        ServerPlayer p = player(server);
        cmd(server, p, "time set 6000");
        cmd(server, p, "effect give @s resistance 99999 4 true");
        cmd(server, p, "effect give @s regeneration 99999 4 true");
        cmd(server, p, "gamemode survival");
        log("Preparado en " + p.blockPosition());
    }

    private static void prepare(MinecraftServer server, Cast c) {
        ServerPlayer p = player(server);
        if (!c.power().equals(PowerService.data(p).powerId())) {
            cmd(server, p, "awakened power give @s " + c.power());
        }
        cmd(server, p, "awakened mastery set @s 30");
        PowerService.data(p).setInfiniteStamina(true);
        cmd(server, p, "kill @e[type=!player]");
        cmd(server, p, "tp @s 0 -60 0 0 0");
        ServerLevel level = p.serverLevel();
        for (int i = 0; i < 3; i++) {
            Mob m = EntityType.HUSK.create(level);
            if (m == null) continue;
            m.moveTo(-3 + i * 3, -60, 9 + i, 180f, 0f);
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.COMMAND, null);
            m.setNoAi(true);
            level.addFreshEntity(m);
        }
        log(c.name() + ": preparada");
    }

    private static void cmd(MinecraftServer server, ServerPlayer p, String c) {
        server.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(), c);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "wips_" + name + ".png", mc.getMainRenderTarget(), c -> { });
        log("Captura " + name);
    }

    private static synchronized void log(String s) {
        String line = "[wips-autotest] " + s;
        OUT.add(line);
        System.out.println(line);
    }

    private static synchronized void write(Minecraft mc) {
        try {
            Files.write(new File(mc.gameDirectory, "wips_autotest.txt").toPath(), OUT, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
