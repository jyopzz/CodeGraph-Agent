package com.codegraph.bridge.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
public class CliProcessService {

    // 1. On Windows, VS Code CLI is 'code.cmd' (located in AppData or Program Files)
    private final String executableName = "code.cmd";

    public record CodeGraphInfo(boolean installed, String version, String path, String status) {}

    public CodeGraphInfo detectInstallation() {
        String resolvedPath = findExecutableInPath(executableName);

        // Fallback check: try 'code.exe' if 'code.cmd' is not found directly
        if (resolvedPath == null) {
            resolvedPath = findExecutableInPath("code.exe");
        }

        if (resolvedPath == null || !new File(resolvedPath).exists()) {
            return new CodeGraphInfo(false, null, null, "not_found");
        }

        String version = getCliVersion(resolvedPath);
        return new CodeGraphInfo(
                true,
                version != null ? version : "v1.0.0-detected",
                resolvedPath,
                "ready"
        );
    }

    public String runCommand(String command, List<String> args) throws Exception {
        CodeGraphInfo info = detectInstallation();
        List<String> processCmd = new ArrayList<>();

        if (info.installed() && info.path() != null) {
            processCmd.add(info.path());
            if (args != null) {
                processCmd.addAll(args);
            }
        } else {
            processCmd.add("cmd.exe");
            processCmd.add("/c");
            processCmd.add(command);
            if (args != null) {
                processCmd.addAll(args);
            }
        }

        ProcessBuilder pb = new ProcessBuilder(processCmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }

        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Command execution timed out after 30 seconds.");
        }

        return output.toString().trim();
    }

    private String getCliVersion(String binaryPath) {
        try {
            // VS Code returns its version and commit SHA via -v / --version
            Process process = new ProcessBuilder(binaryPath, "--version").start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String versionLine = reader.readLine(); // Line 1 is the semver (e.g. 1.91.1)
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