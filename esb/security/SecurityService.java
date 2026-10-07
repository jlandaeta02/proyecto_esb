package com.esb.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

public class SecurityService {

    private static final long MAX_TIME_DIFF_SECONDS = 300;

    public boolean isValidTimestamp(String timestampStr) {
        try {
            long requestEpoch = Long.parseLong(timestampStr);
            long currentEpoch = Instant.now().getEpochSecond();
            return Math.abs(currentEpoch - requestEpoch) <= MAX_TIME_DIFF_SECONDS;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public String calculateHMAC(String data, String secretKey) throws Exception {
        Mac sha256HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
            secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"
        );
        sha256HMAC.init(secretKeySpec);

        byte[] hashBytes = sha256HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hashBytes);
    }

    public boolean compareSignatures(String expectedSignature, String providedSignature) {
        if (expectedSignature == null || providedSignature == null) {
            return false;
        }
        return MessageDigest.isEqual(
            expectedSignature.getBytes(StandardCharsets.UTF_8),
            providedSignature.getBytes(StandardCharsets.UTF_8)
        );
    }
}