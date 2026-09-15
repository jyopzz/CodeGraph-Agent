package com.codegraph.bridge.controller;

import com.codegraph.bridge.service.CliProcessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class BridgeController {

    private final CliProcessService cliProcessService;

    public BridgeController(CliProcessService cliProcessService) {
        this.cliProcessService = cliProcessService;
    }

    @GetMapping("/telemetry")
    public ResponseEntity<?> getTelemetry() {
        CliProcessService.CodeGraphInfo info = cliProcessService.detectInstallation();
        long pid = ProcessHandle.current().pid();

        return ResponseEntity.ok(Map.of(
                "serviceName", "CodeGraph.JavaBridge.exe",
                "agentVersion", "v2.4.0-java",
                "pid", pid,
                "codeGraph", Map.of(
                        "installed", info.installed(),
                        "version", info.version() != null ? info.version() : "N/A",
                        "path", info.path() != null ? info.path() : "",
                        "status", info.status()
                )
        ));
    }

    public record ScriptRequest(String command, List<String> args) {}

    @PostMapping("/run-script")
    public ResponseEntity<?> runScript(@RequestBody ScriptRequest request) {
        try {
            String output = cliProcessService.runCommand(request.command(), request.args());
            return ResponseEntity.ok(Map.of("success", true, "output", output));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}