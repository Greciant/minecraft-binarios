package de.miraculixx.showmyworld;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Carga perezosa del panorama de cada mundo: registra 6 DynamicTexture bajo
 * "showmyworld:panorama/<levelId>_0".."_5" (la misma convencion de nombres que
 * CubeMap espera: base + "_" + indice) y arma un PanoramaRenderer vanilla con
 * ellas. Todo queda en memoria hasta cerrar la pantalla (releaseAll()).
 */
public final class WorldPanoramaManager {
    private static final Map<String, PanoramaRenderer> CACHE = new HashMap<>();
    private static final Map<String, Boolean> FAILED = new HashMap<>();

    private WorldPanoramaManager() {}

    /** Devuelve el renderer de ese mundo, o null si no tiene panorama propio o fallo al cargar. */
    public static PanoramaRenderer get(String levelId) {
        if (Boolean.TRUE.equals(FAILED.get(levelId))) return null;
        PanoramaRenderer cached = CACHE.get(levelId);
        if (cached != null) return cached;
        if (!PanoramaPaths.hasPanorama(levelId)) return null;

        try {
            Path dir = PanoramaPaths.panoramaDir(levelId);
            ResourceLocation base = ResourceLocation.fromNamespaceAndPath(
                    ShowMyWorld.MOD_ID, "panorama/" + sanitize(levelId));
            var textureManager = Minecraft.getInstance().getTextureManager();
            for (int i = 0; i < 6; i++) {
                Path file = dir.resolve("panorama_" + i + ".png");
                NativeImage image;
                try (InputStream in = new FileInputStream(file.toFile())) {
                    image = NativeImage.read(in);
                }
                ResourceLocation faceLoc = base.withPath(base.getPath() + "_" + i + ".png");
                textureManager.register(faceLoc, new DynamicTexture(image));
            }
            PanoramaRenderer renderer = new PanoramaRenderer(new CubeMap(base));
            CACHE.put(levelId, renderer);
            return renderer;
        } catch (IOException e) {
            ShowMyWorld.LOGGER.warn("No se pudo cargar el panorama de '{}': {}", levelId, e.toString());
            FAILED.put(levelId, true);
            return null;
        }
    }

    // El id del mundo puede traer caracteres que un ResourceLocation no acepta (espacios, mayusculas...)
    private static String sanitize(String levelId) {
        StringBuilder sb = new StringBuilder(levelId.length());
        for (char c : levelId.toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.') {
                sb.append(c);
            } else if (c >= 'A' && c <= 'Z') {
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append('_');
            }
        }
        return sb.length() == 0 ? "world" : sb.toString();
    }

    /** Libera las texturas cargadas; llamar al salir de la pantalla de seleccion. */
    public static void releaseAll() {
        if (CACHE.isEmpty()) return;
        var textureManager = Minecraft.getInstance().getTextureManager();
        for (String levelId : CACHE.keySet()) {
            ResourceLocation base = ResourceLocation.fromNamespaceAndPath(
                    ShowMyWorld.MOD_ID, "panorama/" + sanitize(levelId));
            for (int i = 0; i < 6; i++) {
                textureManager.release(base.withPath(base.getPath() + "_" + i + ".png"));
            }
        }
        CACHE.clear();
        FAILED.clear();
    }
}
