package com.example.parkingspot;

import android.util.Base64;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class ActivationCodeUtil {

    private static final String DATE_FORMAT = "dd-MM-yyyy";

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

    private static String generateHMAC(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hmacBytes = mac.doFinal(data.getBytes());

        // Χρήση Android Base64 και αφαίρεση ειδικών χαρακτήρων
        return android.util.Base64.encodeToString(hmacBytes, Base64.NO_WRAP)
                .replaceAll("[^A-Z0-9]", "")  // Αφαίρεση ειδικών χαρακτήρων
                .substring(0, 16);  // Λήψη πρώτων 16 χαρακτήρων
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
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.add(Calendar.DAY_OF_YEAR, days);
        Date date = calendar.getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }
}
