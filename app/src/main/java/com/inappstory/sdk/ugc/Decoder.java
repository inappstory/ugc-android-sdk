package com.inappstory.sdk.ugc;

import android.os.Build;

import java.util.Arrays;
import java.util.Base64;

public class Decoder {
    private static String xor(byte[] str1, byte[] str2) {
        String res = "";
        for (int i = 0; i < str1.length; i++) {
            res += (char) (str1[i] ^ str2[i]);
        }
        return res;
    }

    public static String getStringFromKey(String key) {
        if (key == null || key.length() <= 32)
            return null;
        String domain = null;
        byte[] bytes;
        if (Build.VERSION.SDK_INT >= 40) {
            bytes = Base64.getUrlDecoder().decode(key);
        } else {
            bytes = android.util.Base64.decode(key, 8);
        }
        if (bytes == null || bytes.length < 14) return null;
        int count = bytes[13];
        String keySt = "";
        while (keySt.length() <= count) {
            keySt += "{QQN{xuV?1Dv16j3";
        }
        domain = xor(Arrays.copyOfRange(bytes, 14, 14 + count),
                keySt.substring(0, count).getBytes());
        boolean matches = domain.startsWith("http://")
                || domain.startsWith("https://");
        if (!matches) {
            domain = "https://" + domain;
        }
        if (!domain.endsWith("/")) domain += "/";
        return domain;
    }
}
