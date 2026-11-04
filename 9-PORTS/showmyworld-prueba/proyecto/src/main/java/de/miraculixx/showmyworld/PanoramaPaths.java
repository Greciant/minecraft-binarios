package de.miraculixx.showmyworld;

import net.minecraft.client.Minecraft;

import java.io.File;
import java.nio.file.Path;

/**
 * Resuelve la carpeta "panorama" de un mundo guardado.
 * Formato vanilla de resourcepack: panorama_0.png .. panorama_5.png dentro de esa carpeta
 * (el mismo que usa Minecraft para "textures/gui/title/background/panorama").
 */
public final class PanoramaPaths {
    private PanoramaPaths() {}

    public static Path worldFolder(String levelId) {
        return Minecraft.getInstance().getLevelSource().getBaseDir().resolve(levelId);
    }

    public static Path panoramaDir(String levelId) {
        return worldFolder(levelId).resolve("panorama");
    }

    /** true si las 6 caras existen en la carpeta panorama\ del mundo */
    public static boolean hasPanorama(String levelId) {
        Path dir = panoramaDir(levelId);
        File dirFile = dir.toFile();
        if (!dirFile.isDirectory()) return false;
        for (int i = 0; i < 6; i++) {
            if (!dir.resolve("panorama_" + i + ".png").toFile().isFile()) return false;
        }
        return true;
    }
}
