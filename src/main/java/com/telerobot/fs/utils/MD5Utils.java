package com.telerobot.fs.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Utils {
    public static final String MD5 = "MD5";
    private static final char[] DIGITS_LOWER = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public static String md5Hex(byte[] plainText, boolean needShort, boolean uppercase) {
        String encrypted = new String(encodeHex(Md5(plainText), DIGITS_LOWER));
        if (needShort) {
            encrypted = encrypted.substring(8, 24);
        }
        if (uppercase) {
            encrypted = encrypted.toUpperCase();
        }

        return encrypted;
    }

    public static String md5Hex(byte[] plainText) {
        return md5Hex(plainText, false, false);
    }

    public static String md5HexUpcase(byte[] plainText) {
        return md5Hex(plainText, false, true);
    }

    private static byte[] Md5(byte[] plainBytes) {
        byte[] digestBytes = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            digestBytes = md.digest(plainBytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return digestBytes;
    }

    protected static char[] encodeHex(byte[] data, char[] toDigits) {
        int l = data.length;
        char[] out = new char[l << 1];
        // two characters form the hex value.
        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = toDigits[(0xF0 & data[i]) >>> 4];
            out[j++] = toDigits[0x0F & data[i]];
        }
        return out;
    }
}
