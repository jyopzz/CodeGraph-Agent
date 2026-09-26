package com.codegraph.bridge.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class AgentIdentityService {

    private static final String IDENTITY_FILE = "agent-identity.json";

    private static final int APPLICATION_CODE_BYTES = 12;

    private final SecureRandom secureRandom = new SecureRandom();

    private final Path identityFile;

    private final String applicationCode;

    public AgentIdentityService() {

        Path agentDirectory = Path.of(
                System.getProperty("user.home"),
                ".codegraph-agent");

        this.identityFile = agentDirectory.resolve(IDENTITY_FILE);

        this.applicationCode = initialize();
    }

    private synchronized String initialize() {

        if (Files.exists(identityFile)) {

            String existingCode = readApplicationCode();

            if (existingCode == null ||
                    existingCode.isBlank()) {

                throw new IllegalStateException(
                        "CodeGraph Agent identity is invalid");
            }

            return existingCode;
        }

        String generatedCode = generateApplicationCode();

        saveApplicationCode(generatedCode);

        System.out.println(
                "CodeGraph Agent Application Code: "
                        + generatedCode);

        return generatedCode;
    }

    public String getApplicationCode() {
        return applicationCode;
    }

    private String generateApplicationCode() {

        byte[] bytes = new byte[APPLICATION_CODE_BYTES];

        secureRandom.nextBytes(bytes);

        String value = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        return "CG-" + value;
    }

    private void saveApplicationCode(
            String code) {

        try {

            Files.createDirectories(
                    identityFile.getParent());

            String json = "{\n" +
                    "  \"applicationCode\": \"" +
                    code +
                    "\"\n" +
                    "}";

            /*
             * CREATE_NEW is intentional.
             *
             * It prevents this service from overwriting
             * an existing identity file.
             */
            Files.writeString(
                    identityFile,
                    json,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE);

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Failed to create Agent identity",
                    e);
        }
    }

    private String readApplicationCode() {

        try {

            String json = Files.readString(
                    identityFile,
                    StandardCharsets.UTF_8);

            String marker = "\"applicationCode\"";

            int markerIndex = json.indexOf(marker);

            if (markerIndex < 0) {
                return null;
            }

            int colonIndex = json.indexOf(
                    ':',
                    markerIndex);

            if (colonIndex < 0) {
                return null;
            }

            int firstQuote = json.indexOf(
                    '"',
                    colonIndex + 1);

            if (firstQuote < 0) {
                return null;
            }

            int secondQuote = findClosingQuote(
                    json,
                    firstQuote + 1);

            if (secondQuote < 0) {
                return null;
            }

            return json.substring(
                    firstQuote + 1,
                    secondQuote);

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Failed to read Agent identity",
                    e);
        }
    }

    private int findClosingQuote(
            String value,
            int start) {

        boolean escaped = false;

        for (int i = start; i < value.length(); i++) {

            char c = value.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                continue;
            }

            if (c == '"') {
                return i;
            }
        }

        return -1;
    }
}