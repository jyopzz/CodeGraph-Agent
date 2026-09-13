package com.codegraph.bridge;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.awt.*;
import java.awt.image.BufferedImage;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        // Disables headless mode to enable Windows Tray rendering
        ConfigurableApplicationContext context = new SpringApplicationBuilder(Application.class)
                .headless(false)
                .run(args);

        initSystemTray(context);
    }

    private static void initSystemTray(ConfigurableApplicationContext context) {
        if (!SystemTray.isSupported()) {
            System.out.println("System tray is not supported on this operating system.");
            return;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();

            // Draw a quick 16x16 icon programmatically for the tray
            Image image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = (Graphics2D) image.getGraphics();
            g2.setColor(new Color(0, 188, 212)); // Cyan primary accent
            g2.fillOval(2, 2, 12, 12);
            g2.dispose();

            PopupMenu popup = new PopupMenu();

            MenuItem statusItem = new MenuItem("CodeGraph Bridge (Port: 9870) - Active");
            statusItem.setEnabled(false);
            popup.add(statusItem);

            popup.addSeparator();

            MenuItem exitItem = new MenuItem("Exit Application");
            exitItem.addActionListener(e -> {
                context.close();
                System.exit(0);
            });
            popup.add(exitItem);

            TrayIcon trayIcon = new TrayIcon(image, "CodeGraph Bridge Daemon", popup);
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}