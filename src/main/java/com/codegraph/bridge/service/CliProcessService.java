package com.codegraph.bridge.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
public class CliProcessService {

    private static final String EXECUTABLE_NAME = "code.cmd";

    // ANSI terminal formatting
    private static final String RESET  = "\u001B[0m";
    private static final String BOLD   = "\u001B[1m";
    private static final String DIM    = "\u001B[2m";
    private static final String CYAN   = "\u001B[36m";
    private static final String GREEN  = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String GRAY   = "\u001B[90m";
    private static final String WHITE  = "\u001B[37m";

    private static final String AGENT_VERSION = "1.0.0";
    private static final String DEVELOPER = "Jyothis";

    // Banner layout — change these, everything re-aligns automatically.
    private static final int BOX_INNER_WIDTH = 63;   // chars between ╭ and ╮
    private static final int LABEL_WIDTH = 12;        // width of the "Label :" column
    private static final int COLUMN_GAP_TARGET = 32;  // visible column where col. 2 starts
    private static final Pattern ANSI_PATTERN = Pattern.compile("\u001B\\[[;\\d]*m");

    private final PortManager portManager;

    public CliProcessService(PortManager portManager) {
        this.portManager = portManager;
    }

    public record CodeGraphInfo(
            boolean installed,
            String version,
            String path,
            String status) {
    }

    public CodeGraphInfo detectInstallation() {
        String resolvedPath = findExecutableInPath(EXECUTABLE_NAME);

        if (resolvedPath == null) {
            resolvedPath = findExecutableInPath("code.exe");
        }

        if (resolvedPath == null || !new File(resolvedPath).exists()) {
            return new CodeGraphInfo(false, null, null, "not_found");
        }

        String version = getCliVersion(resolvedPath);
        return new CodeGraphInfo(
                true,
                version != null ? version : "unknown",
                resolvedPath,
                "ready");
    }

    public void openCommandPrompt() throws Exception {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            throw new UnsupportedOperationException("Command Prompt is only supported on Windows.");
        }

        String userHome = System.getProperty("user.home");
        String port = String.valueOf(portManager.getPort());
        String command = buildBannerScript(port);

        ProcessBuilder processBuilder = new ProcessBuilder(
                "cmd.exe",
                "/c",
                "start",
                "",
                "wt.exe",
                "new-tab",
                "--title",
                "CodeGraph Agent",
                "cmd.exe",
                "/k",
                "cd /d \"" + userHome + "\" && " + command
        );

        processBuilder.start();
    }

    // ------------------------------------------------------------------
    // Banner construction
    // ------------------------------------------------------------------

    @FunctionalInterface
    private interface SectionBody {
        void append(StringBuilder sb);
    }

    private String buildBannerScript(String port) {
        StringBuilder sb = new StringBuilder();
        sb.append("title CodeGraph Agent & cls & echo.");

        appendHeaderBox(sb);

        appendSection(sb, CYAN, "AGENT DETAILS", body -> {
            echo(body, detailRow("Version", AGENT_VERSION, "OS", "%OS% (%PROCESSOR_ARCHITECTURE%)"));
            echo(body, detailRow("Developer", DEVELOPER, "Host", "%COMPUTERNAME%"));
            echo(body, detailRow("User", "%USERNAME%", "Session", GREEN + "Active" + RESET));
        });

        appendSection(sb, CYAN, "NETWORK ^& BRIDGE", body -> {
            echo(body, detailRow("Endpoint", "https://localhost:" + port));
            echo(body, detailRow("Security", GREEN + "HTTPS / TLS Encrypted" + RESET));
            echo(body, detailRow("Bridge API", GREEN + "Listening ^& Healthy" + RESET));
        });

        echo(sb, GRAY + DIM + "  Background daemon active. Manage repos directly via desktop UI." + RESET);
        sb.append(" & echo.");

        return sb.toString();
    }

    private void appendHeaderBox(StringBuilder sb) {
        echo(sb, CYAN + "  \u256D" + "\u2500".repeat(BOX_INNER_WIDTH) + "\u256E" + RESET);
        String title = BOLD + WHITE + "CODEGRAPH AGENT" + RESET;
        String status = GREEN + "\u25CF CONNECTED" + RESET;
        echo(sb, CYAN + "  \u2502 " + boxRow(title, status) + " \u2502" + RESET);
        echo(sb, CYAN + "  \u2570" + "\u2500".repeat(BOX_INNER_WIDTH) + "\u256F" + RESET);
        sb.append(" & echo.");
    }

    private void appendSection(StringBuilder sb, String accent, String title, SectionBody body) {
        echo(sb, BOLD + accent + "  " + title + RESET);
        echo(sb, GRAY + "  " + "\u2500".repeat(BOX_INNER_WIDTH) + RESET);
        body.append(sb);
        sb.append(" & echo.");
    }

    /** Single "  Label : value" row. */
    private String detailRow(String label, String value) {
        return GRAY + "  " + pad(label, LABEL_WIDTH) + ": " + WHITE + value + RESET;
    }

    /** Two-column row, gap computed from the visible (ANSI-stripped) length of column 1. */
    private String detailRow(String label1, String value1, String label2, String value2) {
        String left = detailRow(label1, value1);
        int gap = Math.max(2, COLUMN_GAP_TARGET - visibleLength(left));
        String right = GRAY + " ".repeat(gap) + pad(label2, LABEL_WIDTH) + ": " + WHITE + value2 + RESET;
        return left + right;
    }

    /** Distributes left/right content across the box width based on visible length, not raw length. */
    private String boxRow(String left, String right) {
        int used = visibleLength(left) + visibleLength(right);
        int gap = Math.max(1, BOX_INNER_WIDTH - used - 2); // -2: one space each side of the border pipes
        return left + " ".repeat(gap) + right;
    }

    private String pad(String s, int width) {
        return s.length() >= width ? s : s + " ".repeat(width - s.length());
    }

    /** Strips ANSI escape codes so padding math reflects what actually prints. */
    private int visibleLength(String s) {
        return ANSI_PATTERN.matcher(s).replaceAll("").length();
    }

    private void echo(StringBuilder sb, String text) {
        sb.append(" & echo ").append(text);
    }

    // ------------------------------------------------------------------
    // CLI detection (unchanged)
    // ------------------------------------------------------------------

    private String getCliVersion(String binaryPath) {
        try {
            Process process = new ProcessBuilder(binaryPath, "--version").start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String versionLine = reader.readLine();
                process.waitFor(3, TimeUnit.SECONDS);
                return versionLine != null ? versionLine.trim() : "installed";
            }
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String findExecutableInPath(String fileName) {
        File directCheck = new File(fileName);
        if (directCheck.exists()) {
            return directCheck.getAbsolutePath();
        }

        String pathEnv = System.getenv("PATH");
        if (pathEnv == null || pathEnv.isBlank()) {
            return null;
        }

        for (String entry : pathEnv.split(Pattern.quote(File.pathSeparator))) {
            File target = new File(entry.trim(), fileName);
            if (target.exists()) {
                return target.getAbsolutePath();
            }
        }

        return null;
    }
}