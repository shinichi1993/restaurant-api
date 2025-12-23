package com.restaurant.api.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * MomoSignatureUtil
 * ------------------------------------------------------------
 * Sinh chữ ký HmacSHA256 theo format rawData của MoMo.
 */
public class MomoSignatureUtil {

    public static String hmacSHA256(String secretKey, String rawData) {
        try {
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSha256.init(secretKeySpec);

            byte[] hash = hmacSha256.doFinal(rawData.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception ex) {
            throw new RuntimeException("Không thể tạo chữ ký MoMo: " + ex.getMessage(), ex);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) sb.append('0');
            sb.append(hex);
        }
        return sb.toString();
    }
}
