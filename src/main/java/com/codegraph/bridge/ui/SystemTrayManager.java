package com.codegraph.bridge.ui;

import com.codegraph.bridge.service.AgentRestartService;
import com.codegraph.bridge.service.CliProcessService;
import com.codegraph.bridge.service.WindowsStartupService;
import javafx.application.Platform;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;

@Component
public class SystemTrayManager {

    private static final String APP_NAME = "CodeGraph Agent";
    private static final int PORT = 9870;

    private final ConfigurableApplicationContext context;
    private final CliProcessService cliProcessService;
    private final WindowsStartupService startupService;
    private final AgentRestartService restartService;

    private volatile TrayIcon trayIcon;
    private DashboardApp dashboard;

    public SystemTrayManager(
            ConfigurableApplicationContext context,
            CliProcessService cliProcessService,
            WindowsStartupService startupService,
            AgentRestartService restartService) {
        this.context = context;
        this.cliProcessService = cliProcessService;
        this.startupService = startupService;
        this.restartService = restartService;
    }

    public void initialize() {

        System.out.println("Initializing CodeGraph system tray...");

        if (!SystemTray.isSupported()) {
            System.err.println("System tray is not supported.");
            return;
        }

        try {

            createTray();

            System.out.println(
                    "CodeGraph system tray initialized successfully.");

        } catch (Exception e) {

            System.err.println(
                    "Failed to initialize CodeGraph system tray.");

            e.printStackTrace();

            showError(
                    "Unable to initialize CodeGraph system tray.",
                    e.getMessage());
        }
    }

    private void createTray() throws AWTException {

        if (trayIcon != null) {
            System.out.println(
                    "System tray is already initialized.");
            return;
        }

        SystemTray tray = SystemTray.getSystemTray();

        Image image = createTrayIcon();

        PopupMenu menu = createTrayMenu();

        TrayIcon newTrayIcon = new TrayIcon(
                image,
                APP_NAME,
                menu);

        newTrayIcon.setImageAutoSize(true);

        newTrayIcon.addActionListener(
                event -> openDashboard());

        tray.add(newTrayIcon);

        trayIcon = newTrayIcon;

        System.out.println(
                "Tray icon added to Windows notification area.");
    }

    private PopupMenu createTrayMenu() {

        PopupMenu menu = new PopupMenu();

        MenuItem open = new MenuItem("Open");

        open.addActionListener(
                event -> openDashboard());

        menu.add(open);

        MenuItem update = new MenuItem("Check for Update");

        update.addActionListener(
                event -> checkForUpdate());

        menu.add(update);

        menu.addSeparator();

        MenuItem restart = new MenuItem("Restart Agent");

        restart.addActionListener(
                event -> restartAgent());

        menu.add(restart);

        MenuItem port = new MenuItem("Port: " + PORT);

        port.setEnabled(false);

        menu.add(port);

        menu.addSeparator();

        MenuItem exit = new MenuItem("Exit");

        exit.addActionListener(
                event -> exitApplication());

        menu.add(exit);

        return menu;
    }

    private void openDashboard() {

    Platform.runLater(() -> {

        try {

            if (dashboard == null) {
                dashboard = new DashboardApp(
                        cliProcessService,
                        startupService,
                        this::exitApplication);
            }

            dashboard.show();

        } catch (Exception e) {

            e.printStackTrace();

            showError(
                    "Unable to open CodeGraph Agent dashboard.",
                    e.getMessage());
        }
    });
}

    private void checkForUpdate() {

        SwingUtilities.invokeLater(() -> {

            JOptionPane.showMessageDialog(
                    null,
                    """
                            CodeGraph Agent

                            Current version: 1.0.0

                            Update checking will be
                            connected to the CodeGraph
                            update service later.
                            """,
                    APP_NAME,
                    JOptionPane.INFORMATION_MESSAGE);
        });
    }

    private void restartAgent() {

        int result = JOptionPane.showConfirmDialog(
                null,
                "Restart CodeGraph Agent?",
                APP_NAME,
                JOptionPane.YES_NO_OPTION);

        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        removeTrayIcon();

        Platform.runLater(() -> {

            if (dashboard != null) {
                dashboard.hide();
            }
        });

        restartService.restart();
    }

    public void exitApplication() {

        removeTrayIcon();

        Platform.runLater(() -> {

            if (dashboard != null) {
                dashboard.hide();
            }
        });

        context.close();

        System.exit(0);
    }

    private void removeTrayIcon() {

        TrayIcon currentTrayIcon = trayIcon;

        if (currentTrayIcon == null) {
            return;
        }

        try {

            SystemTray
                    .getSystemTray()
                    .remove(currentTrayIcon);

            System.out.println(
                    "CodeGraph tray icon removed.");

        } catch (Exception e) {

            e.printStackTrace();

        } finally {

            trayIcon = null;
        }
    }

    private void showError(
            String message,
            String details) {

        SwingUtilities.invokeLater(() -> {

            JOptionPane.showMessageDialog(
                    null,
                    message + "\n\n" + details,
                    APP_NAME,
                    JOptionPane.ERROR_MESSAGE);
        });
    }

    private Image createTrayIcon() {

        int size = 32;

        BufferedImage image = new BufferedImage(
                size,
                size,
                BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = image.createGraphics();

        try {

            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g.setColor(new Color(25, 30, 38));

            g.fillRoundRect(
                    2,
                    2,
                    28,
                    28,
                    8,
                    8);

            g.setColor(new Color(0, 188, 212));

            g.fillOval(
                    7,
                    7,
                    18,
                    18);

            g.setColor(new Color(25, 30, 38));

            g.fillOval(
                    12,
                    12,
                    8,
                    8);

        } finally {

            g.dispose();
        }

        return image;
    }
}
