package com.example.parkingspot;

import android.annotation.SuppressLint;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class ActivationCodeUtil {

    private static final String DATE_FORMAT = "yyyy-MM-dd";

    public static String generateActivationCode(String deviceId, String expiryDate, String appName) {
        try {
            String data = deviceId + "|" + expiryDate;
            String hmac = generateHMAC(data, appName);

            // Μορφοποίηση του κωδικού xxxx-xxxx-xxxx-xxxx
            String formattedCode = hmac.substring(0, 16).toUpperCase();
            return formatActivationCode(formattedCode);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressLint("NewApi")
    private static String generateHMAC(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hmacBytes = mac.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(hmacBytes).replaceAll("[^A-Z0-9]", "").substring(0, 16);
    }

    private static String formatActivationCode(String code) {
        return code.replaceAll("(.{4})(?=.)", "$1-");
    }

    public static String extractExpiryDate(String activationCode, String deviceID, String secretKey) {
        for (int i = 0; i < 740; i++) {
            String potentialDate = getDateAfterDays(i);
            String generatedCode = generateActivationCode(deviceID, potentialDate, secretKey);

            if (generatedCode != null && generatedCode.equalsIgnoreCase(activationCode)) {
                return potentialDate;
            }
        }
        return null;
    }

    private static String getDateAfterDays(int days) {
        Date date = new Date(System.currentTimeMillis() + days * 24L * 60 * 60 * 1000);
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        return dateFormat.format(date);
    }
}
