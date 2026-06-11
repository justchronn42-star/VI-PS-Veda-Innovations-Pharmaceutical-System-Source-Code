package com.vips.pharma.util;

import javafx.scene.image.Image;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

/**
 * Resolves the application logo at runtime.
 *
 * Resolution order (first match wins):
 *   1. logo.png  in the working directory (next to run.bat / run.sh)
 *   2. logo.jpg  in the working directory
 *   3. logo.jpeg in the working directory
 *   4. logo.gif  in the working directory
 *   5. null  →  caller falls back to the 💊 emoji Label
 *
 * To replace the pill emoji with your own logo, just drop a file called
 * "logo.png" (or .jpg/.jpeg/.gif) in the same folder as run.bat and restart.
 */
public final class LogoManager {

    private static final List<String> CANDIDATES = List.of(
            "logo.png", "logo.jpg", "logo.jpeg", "logo.gif"
    );

    private LogoManager() {}

    /**
     * Loads the logo image scaled to the requested size.
     *
     * @param size width and height in pixels
     * @return the Image, or {@code null} if no logo file is present
     */
    public static Image loadLogo(double size) {
        for (String name : CANDIDATES) {
            File f = new File(name);
            if (f.exists() && f.isFile()) {
                try (FileInputStream fis = new FileInputStream(f)) {
                    return new Image(fis, size, size, true, true);
                } catch (Exception ignored) {}
            }
        }
        return null; // no file found — use emoji fallback
    }

    /**
     * Loads the logo at full resolution (no scaling) for use as a stage icon.
     *
     * @return the Image, or {@code null} if no logo file is present
     */
    public static Image loadLogoRaw() {
        for (String name : CANDIDATES) {
            File f = new File(name);
            if (f.exists() && f.isFile()) {
                try (FileInputStream fis = new FileInputStream(f)) {
                    return new Image(fis);
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    /** Returns true if a logo file exists in the working directory. */
    public static boolean hasLogo() {
        return CANDIDATES.stream().anyMatch(n -> new File(n).exists());
    }
}
