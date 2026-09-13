package com.codegraph.bridge.service;

import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class AgentRestartService {

    public void restart() {

        try {

            String javaHome = System.getProperty("java.home");

            String javaExecutable =
                    javaHome + "\\bin\\javaw.exe";

            String classPath =
                    System.getProperty("java.class.path");

            ProcessBuilder processBuilder = new ProcessBuilder(
                    javaExecutable,
                    "-cp",
                    classPath,
                    "com.codegraph.bridge.Application",
                    "--restart"
            );

            processBuilder.start();

            // Give the new process time to initialize.
            Thread.sleep(500);

            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}