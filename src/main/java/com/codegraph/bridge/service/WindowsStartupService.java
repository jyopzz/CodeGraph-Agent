package com.codegraph.bridge.service;

import org.springframework.stereotype.Service;

@Service
public class WindowsStartupService {

    private static final String APP_NAME = "CodeGraphAgent";

    private static final String REGISTRY_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";

    public boolean isWindows() {
        return System.getProperty("os.name")
                .toLowerCase()
                .contains("win");
    }

    public boolean isStartupEnabled() {

        if (!isWindows()) {
            return false;
        }

        try {

            Process process = new ProcessBuilder(
                    "reg",
                    "query",
                    REGISTRY_KEY,
                    "/v",
                    APP_NAME)
                    .redirectErrorStream(true)
                    .start();

            process.waitFor();

            return process.exitValue() == 0;

        } catch (Exception e) {

            return false;
        }
    }

    public boolean enableStartup() {

        if (!isWindows()) {
            return false;
        }

        try {

            String launchCommand = buildLaunchCommand();

            Process process = new ProcessBuilder(
                    "reg",
                    "add",
                    REGISTRY_KEY,
                    "/v",
                    APP_NAME,
                    "/t",
                    "REG_SZ",
                    "/d",
                    launchCommand,
                    "/f")
                    .redirectErrorStream(true)
                    .start();

            process.waitFor();

            return process.exitValue() == 0;

        } catch (Exception e) {

            return false;
        }
    }

    public boolean disableStartup() {

        if (!isWindows()) {
            return false;
        }

        try {

            Process process = new ProcessBuilder(
                    "reg",
                    "delete",
                    REGISTRY_KEY,
                    "/v",
                    APP_NAME,
                    "/f")
                    .redirectErrorStream(true)
                    .start();

            process.waitFor();

            return process.exitValue() == 0;

        } catch (Exception e) {

            return false;
        }
    }

    private String buildLaunchCommand() {

        String javaCommand = System.getProperty("java.home")
                + "\\bin\\javaw.exe";

        String classPath = System.getProperty(
                "java.class.path");

        return "\"" + javaCommand + "\" "
                + "-cp \""
                + classPath
                + "\" "
                + "com.codegraph.bridge.Application";
    }
}