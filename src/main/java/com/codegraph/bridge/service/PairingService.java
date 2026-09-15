package com.codegraph.bridge.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PairingService {

        private static final long PAIRING_CODE_TTL_SECONDS = 300;
        private static final long CHALLENGE_TTL_SECONDS = 120;

        private static final String PAIRED_BROWSER_FILE = "paired-browser.json";

        private final SecureRandom secureRandom = new SecureRandom();

        private volatile String pairingCode;
        private volatile Instant pairingCodeExpiresAt;

        private final Map<String, PendingPairing> pendingPairings = new ConcurrentHashMap<>();

        private final Path pairedBrowserFile;

        public PairingService() {

                Path agentDirectory = Path.of(
                                System.getProperty("user.home"),
                                ".codegraph-agent");

                this.pairedBrowserFile = agentDirectory.resolve(PAIRED_BROWSER_FILE);

                regeneratePairingCode();
        }

        public synchronized void regeneratePairingCode() {

                int code = 100000 + secureRandom.nextInt(900000);

                pairingCode = String.valueOf(code);

                pairingCodeExpiresAt = Instant.now()
                                .plusSeconds(PAIRING_CODE_TTL_SECONDS);

                pendingPairings.clear();

                System.out.println(
                                "CodeGraph Agent pairing code: " + pairingCode);
        }

        public String getPairingCode() {
                return pairingCode;
        }

        public Instant getPairingCodeExpiresAt() {
    return pairingCodeExpiresAt;
}

        /*
         * Initial pairing using the 6-digit pairing code.
         */
        public synchronized PairingChallenge createChallenge(
                        String suppliedCode,
                        String publicKey) {

                validatePublicKey(publicKey);

                if (suppliedCode == null) {
                        throw new IllegalArgumentException(
                                        "Pairing code is required");
                }

                if (isPairingCodeExpired()) {

                        regeneratePairingCode();

                        throw new IllegalArgumentException(
                                        "Pairing code expired");
                }

                if (!secureEquals(
                                pairingCode,
                                suppliedCode)) {

                        throw new IllegalArgumentException(
                                        "Invalid pairing code");
                }

                return createPendingChallenge(publicKey);
        }

        /*
         * Reconnect using the previously paired browser key.
         *
         * No pairing code is required here.
         */
        public synchronized PairingChallenge createReconnectChallenge(
                        String publicKey) {

                validatePublicKey(publicKey);

                String pairedPublicKey = getPairedPublicKey();

                if (pairedPublicKey == null) {

                        throw new IllegalArgumentException(
                                        "No browser is paired with this Agent");
                }

                if (!secureEquals(
                                pairedPublicKey,
                                publicKey)) {

                        throw new IllegalArgumentException(
                                        "Browser is not paired with this Agent");
                }

                return createPendingChallenge(publicKey);
        }

        private PairingChallenge createPendingChallenge(
                        String publicKey) {

                String challengeId = randomToken(32);

                String challenge = randomToken(32);

                Instant expiresAt = Instant.now()
                                .plusSeconds(CHALLENGE_TTL_SECONDS);

                PendingPairing pending = new PendingPairing(
                                challengeId,
                                challenge,
                                publicKey,
                                expiresAt);

                pendingPairings.put(
                                challengeId,
                                pending);

                return new PairingChallenge(
                                challengeId,
                                challenge,
                                expiresAt);
        }

        /*
         * Called after successful initial pairing.
         */
        public synchronized void savePairedPublicKey(
                        String publicKey) {

                validatePublicKey(publicKey);

                try {

                        Files.createDirectories(
                                        pairedBrowserFile.getParent());

                        String json = "{\n" +
                                        "  \"publicKey\": " +
                                        quoteJson(publicKey) +
                                        "\n" +
                                        "}";

                        Files.writeString(
                                        pairedBrowserFile,
                                        json,
                                        StandardCharsets.UTF_8,
                                        StandardOpenOption.CREATE,
                                        StandardOpenOption.TRUNCATE_EXISTING,
                                        StandardOpenOption.WRITE);

                } catch (IOException e) {

                        throw new IllegalStateException(
                                        "Failed to save Agent pairing",
                                        e);
                }
        }

        public synchronized String getPairedPublicKey() {

                if (!Files.exists(pairedBrowserFile)) {
                        return null;
                }

                try {

                        String json = Files.readString(
                                        pairedBrowserFile,
                                        StandardCharsets.UTF_8);

                        String marker = "\"publicKey\"";

                        int markerIndex = json.indexOf(marker);

                        if (markerIndex < 0) {
                                return null;
                        }

                        int colonIndex = json.indexOf(
                                        ':',
                                        markerIndex);

                        int firstQuote = json.indexOf(
                                        '"',
                                        colonIndex + 1);

                        int secondQuote = findClosingQuote(
                                        json,
                                        firstQuote + 1);

                        if (firstQuote < 0 ||
                                        secondQuote < 0) {

                                return null;
                        }

                        return unescapeJson(
                                        json.substring(
                                                        firstQuote + 1,
                                                        secondQuote));

                } catch (IOException e) {

                        throw new IllegalStateException(
                                        "Failed to read Agent pairing",
                                        e);
                }
        }

        public synchronized boolean isPaired() {
                return getPairedPublicKey() != null;
        }

        public synchronized void clearPairedPublicKey() {

                try {

                        Files.deleteIfExists(
                                        pairedBrowserFile);

                } catch (IOException e) {

                        throw new IllegalStateException(
                                        "Failed to clear Agent pairing",
                                        e);
                }
        }

        public PendingPairing consumeChallenge(
                        String challengeId) {

                if (challengeId == null) {
                        return null;
                }

                /*
                 * remove() makes the challenge single-use.
                 */
                PendingPairing pending = pendingPairings.remove(
                                challengeId);

                if (pending == null) {
                        return null;
                }

                if (Instant.now().isAfter(
                                pending.expiresAt())) {

                        return null;
                }

                return pending;
        }

        private void validatePublicKey(
                        String publicKey) {

                if (publicKey == null ||
                                publicKey.isBlank()) {

                        throw new IllegalArgumentException(
                                        "Public key is required");
                }

                if (publicKey.length() > 4096) {

                        throw new IllegalArgumentException(
                                        "Public key is too large");
                }
        }

        private boolean isPairingCodeExpired() {

                return pairingCode == null
                                || pairingCodeExpiresAt == null
                                || Instant.now().isAfter(
                                                pairingCodeExpiresAt);
        }

        private String randomToken(int bytes) {

                byte[] value = new byte[bytes];

                secureRandom.nextBytes(value);

                return Base64.getUrlEncoder()
                                .withoutPadding()
                                .encodeToString(value);
        }

        private boolean secureEquals(
                        String expected,
                        String actual) {

                if (expected == null ||
                                actual == null ||
                                expected.length() != actual.length()) {

                        return false;
                }

                int result = 0;

                for (int i = 0; i < expected.length(); i++) {

                        result |= expected.charAt(i)
                                        ^ actual.charAt(i);
                }

                return result == 0;
        }

        private String quoteJson(String value) {

                return "\"" +
                                value
                                                .replace("\\", "\\\\")
                                                .replace("\"", "\\\"")
                                + "\"";
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

        private String unescapeJson(
                        String value) {

                return value
                                .replace("\\\"", "\"")
                                .replace("\\\\", "\\");
        }

        public record PairingChallenge(
                        String challengeId,
                        String challenge,
                        Instant expiresAt) {
        }

        public record PendingPairing(
                        String challengeId,
                        String challenge,
                        String publicKey,
                        Instant expiresAt) {
        }
}