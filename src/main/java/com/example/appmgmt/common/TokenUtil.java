package com.example.appmgmt.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/** 確認用トークン（256 ビット乱数、Base64URL）と SHA-256 ハッシュ（16 進 64 文字）。 */
public final class TokenUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private TokenUtil() {
    }

    public static String newToken() {
        byte[] b = new byte[32];
        RANDOM.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    public static String sha256Hex(String value) {
        return sha256Hex(value.getBytes(StandardCharsets.UTF_8));
    }

    /** バイト列（PDF ファイルなど）の SHA-256（16 進 64 文字）。 */
    public static String sha256Hex(byte[] value) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(value);
            StringBuilder sb = new StringBuilder(64);
            for (byte x : d) {
                sb.append(String.format("%02x", x));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** トークンの文字種（Base64URL）だけを事前に確認する。 */
    public static boolean looksLikeToken(String token) {
        return token != null && token.length() >= 32 && token.length() <= 64 && token.matches("[A-Za-z0-9_-]+");
    }
}
