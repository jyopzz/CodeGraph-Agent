package com.codegraph.bridge.service;

import org.springframework.stereotype.Service;

import java.util.prefs.Preferences;

@Service
public class PortManager {

    public static final int DEFAULT_PORT = 9870;

    private static final String PORT_KEY = "agent-port";

    private final Preferences preferences = Preferences.userNodeForPackage(PortManager.class);

    public int getPort() {

        int port = preferences.getInt(
                PORT_KEY,
                DEFAULT_PORT);

        return isValidPort(port)
                ? port
                : DEFAULT_PORT;
    }

    public boolean savePort(int port) {

        if (!isValidPort(port)) {
            return false;
        }

        preferences.putInt(
                PORT_KEY,
                port);

        return true;
    }

    public static boolean isValidPort(int port) {

        return port >= 1024 && port <= 65535;
    }
}