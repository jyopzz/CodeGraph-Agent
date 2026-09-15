package com.codegraph.bridge.controller;

import com.codegraph.bridge.service.PairingService;
import com.codegraph.bridge.service.SessionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.*;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final PairingService pairingService;
    private final SessionService sessionService;

    public AuthController(
            PairingService pairingService,
            SessionService sessionService) {

        this.pairingService = pairingService;
        this.sessionService = sessionService;
    }

    @GetMapping("/status")
    public ResponseEntity<?> status() {

        return ResponseEntity.ok(Map.of(
                "pairingRequired", true,
                "pairingCodeExpiresAt",
                pairingService.getPairingCodeExpiresAt()
        ));
    }

    public record PairRequest(
            String code,
            String publicKey
    ) {}

    @PostMapping("/pair")
    public ResponseEntity<?> pair(
            @RequestBody PairRequest request) {

        try {

            PairingService.PairingChallenge challenge =
                    pairingService.createChallenge(
                            request.code(),
                            request.publicKey()
                    );

            return ResponseEntity.ok(Map.of(
                    "challengeId", challenge.challengeId(),
                    "challenge", challenge.challenge(),
                    "expiresAt", challenge.expiresAt()
            ));

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage()
                    ));
        }
    }

    public record VerifyRequest(
            String challengeId,
            String signature
    ) {}

    @PostMapping("/verify")
    public ResponseEntity<?> verify(
            @RequestBody VerifyRequest request) {

        PairingService.PendingPairing pending =
                pairingService.consumeChallenge(
                        request.challengeId()
                );

        if (pending == null) {

            return ResponseEntity
                    .status(401)
                    .body(Map.of(
                            "success", false,
                            "error", "Invalid or expired challenge"
                    ));
        }

        try {

            if (!verifySignature(
                    pending.publicKey(),
                    pending.challenge(),
                    request.signature()
            )) {
                throw new SecurityException("Invalid signature");
            }

            pairingService.savePairedPublicKey(pending.publicKey());

            String sessionToken =
                    sessionService.createSession();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "sessionToken", sessionToken,
                    "expiresInSeconds", 1800
            ));

        } catch (Exception e) {

            return ResponseEntity
                    .status(401)
                    .body(Map.of(
                            "success", false,
                            "error", "Signature verification failed"
                    ));
        }
    }

    private boolean verifySignature(
            String publicKeyJwk,
            String challenge,
            String signatureBase64Url
    ) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode jwk = objectMapper.readTree(publicKeyJwk);

        String x = jwk.get("x").asText();
        String y = jwk.get("y").asText();

        byte[] xBytes = Base64.getUrlDecoder().decode(x);
        byte[] yBytes = Base64.getUrlDecoder().decode(y);

        ECPoint point = new ECPoint(
                new BigInteger(1, xBytes),
                new BigInteger(1, yBytes)
        );

        AlgorithmParameters parameters =
                AlgorithmParameters.getInstance("EC");

        parameters.init(new ECGenParameterSpec("secp256r1"));

        ECParameterSpec ecSpec =
                parameters.getParameterSpec(ECParameterSpec.class);

        ECPublicKeySpec publicKeySpec =
                new ECPublicKeySpec(point, ecSpec);

        KeyFactory keyFactory =
                KeyFactory.getInstance("EC");

        PublicKey publicKey =
                keyFactory.generatePublic(publicKeySpec);

        byte[] rawSignature =
                Base64.getUrlDecoder().decode(signatureBase64Url);

        byte[] derSignature =
                rawEcdsaToDer(rawSignature);

        Signature verifier =
                Signature.getInstance("SHA256withECDSA");

        verifier.initVerify(publicKey);

        verifier.update(
                challenge.getBytes(StandardCharsets.UTF_8)
        );

        return verifier.verify(derSignature);
    }

    private boolean verifySignature(
            PublicKey publicKey,
            String challenge,
            String signatureBase64Url
    ) throws Exception {

        byte[] rawSignature =
                Base64.getUrlDecoder().decode(signatureBase64Url);

        byte[] derSignature =
                rawEcdsaToDer(rawSignature);

        Signature verifier =
                Signature.getInstance("SHA256withECDSA");

        verifier.initVerify(publicKey);

        verifier.update(
                challenge.getBytes(StandardCharsets.UTF_8)
        );

        return verifier.verify(derSignature);
    }

    private byte[] rawEcdsaToDer(byte[] raw) {

        if (raw.length != 64) {
            throw new IllegalArgumentException(
                    "Invalid P-256 ECDSA signature length: "
                            + raw.length
            );
        }

        byte[] r = new byte[32];
        byte[] s = new byte[32];

        System.arraycopy(raw, 0, r, 0, 32);
        System.arraycopy(raw, 32, s, 0, 32);

        r = stripLeadingZeros(r);
        s = stripLeadingZeros(s);

        // DER INTEGER is signed, so prepend 0x00
        // when the highest bit is set.
        if ((r[0] & 0x80) != 0) {
            r = prependZero(r);
        }

        if ((s[0] & 0x80) != 0) {
            s = prependZero(s);
        }

        int sequenceLength =
                2 + r.length +
                        2 + s.length;

        byte[] der =
                new byte[2 + sequenceLength];

        int offset = 0;

        // SEQUENCE
        der[offset++] = 0x30;
        der[offset++] = (byte) sequenceLength;

        // r INTEGER
        der[offset++] = 0x02;
        der[offset++] = (byte) r.length;

        System.arraycopy(
                r,
                0,
                der,
                offset,
                r.length
        );

        offset += r.length;

        // s INTEGER
        der[offset++] = 0x02;
        der[offset++] = (byte) s.length;

        System.arraycopy(
                s,
                0,
                der,
                offset,
                s.length
        );

        return der;
    }

    private byte[] stripLeadingZeros(byte[] value) {

        int firstNonZero = 0;

        while (
                firstNonZero < value.length - 1
                        && value[firstNonZero] == 0
        ) {
            firstNonZero++;
        }

        byte[] result =
                new byte[value.length - firstNonZero];

        System.arraycopy(
                value,
                firstNonZero,
                result,
                0,
                result.length
        );

        return result;
    }

    private byte[] prependZero(byte[] value) {

        byte[] result =
                new byte[value.length + 1];

        System.arraycopy(
                value,
                0,
                result,
                1,
                value.length
        );

        return result;
    }

    public record ReconnectRequest(
            String publicKey
    ) {}


    @PostMapping("/reconnect")
    public ResponseEntity<?> reconnect(
            @RequestBody ReconnectRequest request) {

        try {

            PairingService.PairingChallenge challenge =
                    pairingService.createReconnectChallenge(
                            request.publicKey()
                    );

            return ResponseEntity.ok(
                    Map.of(
                            "challengeId",
                            challenge.challengeId(),

                            "challenge",
                            challenge.challenge(),

                            "expiresAt",
                            challenge.expiresAt()
                    )
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .status(401)
                    .body(
                            Map.of(
                                    "success",
                                    false,

                                    "error",
                                    e.getMessage()
                            )
                    );
        }
    }

    @PostMapping("/unpair")
    public ResponseEntity<?> unpair() {

        pairingService.clearPairedPublicKey();

        // Revoke all active sessions as well.
        sessionService.revokeAll();

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "message", "Agent unpaired successfully"
                )
        );
    }
}