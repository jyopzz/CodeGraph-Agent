package com.codegraph.bridge;

import com.codegraph.bridge.service.PortManager;
import com.codegraph.bridge.service.SingleInstanceService;
import com.codegraph.bridge.ui.SystemTrayManager;
import javafx.application.Platform;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.awt.GraphicsEnvironment;
import java.awt.SystemTray;

@SpringBootApplication
public class Application {

    private static ConfigurableApplicationContext context;

    public static void main(String[] args) {

        // IMPORTANT:
        // CodeGraph Agent uses AWT System Tray.
        // Must be configured before Spring/JavaFX initialization.
        System.setProperty("java.awt.headless", "false");

        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println(
                "Headless: " + GraphicsEnvironment.isHeadless());
        System.out.println(
                "SystemTray supported: " + SystemTray.isSupported());

        boolean restarting = false;

        for (String arg : args) {
            if ("--restart".equalsIgnoreCase(arg)) {
                restarting = true;
                break;
            }
        }

        SingleInstanceService singleInstanceService = new SingleInstanceService();

        boolean acquired;

        if (restarting) {
            acquired = singleInstanceService.acquireWithRetry(15_000);
        } else {
            acquired = singleInstanceService.acquire();
        }

        if (!acquired) {
            System.out.println("CodeGraph Agent is already running.");
            return;
        }

        Runtime.getRuntime().addShutdownHook(
                new Thread(singleInstanceService::release));

        /*
         * Load the saved Agent port.
         *
         * Default port: 9870
         * Allowed range: 1024 - 65535
         */
        PortManager portManager = new PortManager();

        int configuredPort = portManager.getPort();

        System.setProperty(
                "server.port",
                String.valueOf(configuredPort));

        System.out.println(
                "Agent HTTP port: " + configuredPort);

        context = SpringApplication.run(
                Application.class,
                args);

        Platform.setImplicitExit(false);

        // Start JavaFX toolkit
        Platform.startup(() -> {
        });

        // Start system tray
        SystemTrayManager trayManager = context.getBean(SystemTrayManager.class);

        trayManager.initialize();

        System.out.println("CodeGraph Agent started.");
    }
}