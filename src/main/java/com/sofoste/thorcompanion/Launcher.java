package com.sofoste.thorcompanion;

import java.util.Arrays;

/**
 * Stable entry point used by Maven, jpackage and operating-system launchers.
 * Keeping JavaFX out of this class also makes --version usable without a display.
 */
public final class Launcher {
    private static final String VERSION = "THOR Companion 1.0.0 · First Signal";

    private Launcher() {
    }

    public static void main(String[] args) {
        if (Arrays.asList(args).contains("--version")) {
            System.out.println(VERSION);
            return;
        }
        for (int index = 0; index < args.length - 1; index++) {
            if ("--screenshot".equals(args[index])) {
                ThorApp.setScreenshotPath(args[index + 1]);
            }
        }
        ThorApp.main(args);
    }
}
