package com.example.projekt;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class PasswordUtils {

    private PasswordUtils() {
    }

    public static String utworzSol() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    public static String hashujHaslo(String haslo, String sol) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] wynik = digest.digest((sol + ":" + haslo).getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(wynik, Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }
}
