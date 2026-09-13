package com.codegraph.bridge.ui;

import javafx.application.Platform;
import javafx.scene.Scene;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.prefs.Preferences;

/**
 * Manages the application appearance mode.
 *
 * <p>
 * Supported modes are Light, Dark, and System. The selected mode is
 * persisted between application restarts.
 * </p>
 */
public class ThemeManager {

    private static final String PREFERENCE_KEY = "theme-mode";

    private static final String LIGHT_THEME_CLASS = "theme-light";
    private static final String DARK_THEME_CLASS = "theme-dark";

    private static final String WINDOWS_PERSONALIZE_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize";

    private static final String WINDOWS_APPS_LIGHT_THEME_VALUE = "AppsUseLightTheme";

    private static final long SYSTEM_THEME_CHECK_INTERVAL_MS = 2000L;

    private final Preferences preferences = Preferences.userNodeForPackage(ThemeManager.class);

    private Scene scene;
    private Thread systemThemeMonitor;

    /**
     * Available application theme modes.
     */
    public enum ThemeMode {

        /** Always use the light theme. */
        LIGHT("Light"),

        /** Always use the dark theme. */
        DARK("Dark"),

        /** Follow the Windows application appearance setting. */
        SYSTEM("System");

        private final String displayName;

        ThemeMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /**
     * Attaches this theme manager to a JavaFX scene.
     *
     * @param scene JavaFX scene to manage
     */
    public void attach(Scene scene) {
        this.scene = scene;

        ThemeMode mode = getMode();
        applyTheme(mode);

        if (mode == ThemeMode.SYSTEM) {
            startSystemThemeMonitor();
        }
    }

    /**
     * Returns the currently configured theme mode.
     *
     * @return configured theme mode
     */
    public ThemeMode getMode() {
        String savedMode = preferences.get(
                PREFERENCE_KEY,
                ThemeMode.SYSTEM.name());

        try {
            return ThemeMode.valueOf(savedMode);
        } catch (IllegalArgumentException exception) {
            return ThemeMode.SYSTEM;
        }
    }

    /**
     * Changes and persists the application theme.
     *
     * @param mode theme mode to apply
     */
    public void setMode(ThemeMode mode) {
        if (mode == null) {
            mode = ThemeMode.SYSTEM;
        }

        preferences.put(PREFERENCE_KEY, mode.name());

        stopSystemThemeMonitor();
        applyTheme(mode);

        if (mode == ThemeMode.SYSTEM) {
            startSystemThemeMonitor();
        }
    }

    /**
     * Applies the specified theme to the attached scene.
     *
     * @param mode theme mode to apply
     */
    private void applyTheme(ThemeMode mode) {
        if (scene == null) {
            return;
        }

        ThemeMode effectiveMode = mode;

        if (mode == ThemeMode.SYSTEM) {
            effectiveMode = isWindowsDarkMode()
                    ? ThemeMode.DARK
                    : ThemeMode.LIGHT;
        }

        ThemeMode finalEffectiveMode = effectiveMode;

        Platform.runLater(() -> {
            if (scene == null) {
                return;
            }

            scene.getRoot().getStyleClass().removeAll(
                    LIGHT_THEME_CLASS,
                    DARK_THEME_CLASS);

            String themeClass = finalEffectiveMode == ThemeMode.DARK
                    ? DARK_THEME_CLASS
                    : LIGHT_THEME_CLASS;

            scene.getRoot().getStyleClass().add(themeClass);
        });
    }

    /**
     * Determines whether Windows is currently using dark mode
     * for applications.
     *
     * @return true when Windows application dark mode is enabled
     */
    private boolean isWindowsDarkMode() {
        if (!isWindows()) {
            return false;
        }

        try {
            Process process = new ProcessBuilder(
                    "reg",
                    "query",
                    WINDOWS_PERSONALIZE_KEY,
                    "/v",
                    WINDOWS_APPS_LIGHT_THEME_VALUE)
                    .redirectErrorStream(true)
                    .start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {

                String line;

                while ((line = reader.readLine()) != null) {
                    if (line.contains(WINDOWS_APPS_LIGHT_THEME_VALUE)) {
                        return line.matches(".*\\s+0x0\\s*$");
                    }
                }
            }

            process.waitFor();

        } catch (Exception exception) {
            // Fall back to light mode if the Windows setting cannot be read.
        }

        return false;
    }

    /**
     * Checks whether the current operating system is Windows.
     *
     * @return true when running on Windows
     */
    private boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase()
                .contains("win");
    }

    /**
     * Starts monitoring the Windows appearance setting.
     */
    private void startSystemThemeMonitor() {
        if (systemThemeMonitor != null
                && systemThemeMonitor.isAlive()) {
            return;
        }

        systemThemeMonitor = new Thread(
                this::monitorSystemTheme,
                "codegraph-theme-monitor");

        systemThemeMonitor.setDaemon(true);
        systemThemeMonitor.start();
    }

    /**
     * Monitors the Windows theme until System mode is disabled
     * or the monitor thread is interrupted.
     */
    private void monitorSystemTheme() {
        boolean previousDarkMode = isWindowsDarkMode();

        while (!Thread.currentThread().isInterrupted()
                && getMode() == ThemeMode.SYSTEM) {

            try {
                Thread.sleep(SYSTEM_THEME_CHECK_INTERVAL_MS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            }

            boolean currentDarkMode = isWindowsDarkMode();

            if (currentDarkMode != previousDarkMode) {
                previousDarkMode = currentDarkMode;
                applyTheme(ThemeMode.SYSTEM);
            }
        }
    }

    /**
     * Stops the system theme monitor.
     */
    private void stopSystemThemeMonitor() {
        if (systemThemeMonitor != null) {
            systemThemeMonitor.interrupt();
            systemThemeMonitor = null;
        }
    }

    /**
     * Releases resources used by the theme manager.
     */
    public void dispose() {
        stopSystemThemeMonitor();
        scene = null;
    }
}
