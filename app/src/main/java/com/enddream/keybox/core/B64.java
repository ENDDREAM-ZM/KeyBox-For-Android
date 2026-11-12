package com.enddream.keybox.core;

public final class B64 {

    private static final char[] CH =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

    private B64() {}

    public static String encode(byte[] d) {
        if (d == null) return "";
        StringBuilder b = new StringBuilder(((d.length + 2) / 3) * 4);
        int i = 0;
        while (i < d.length) {
            int b0 = d[i] & 0xff;
            int b1 = (i + 1 < d.length) ? (d[i + 1] & 0xff) : 0;
            int b2 = (i + 2 < d.length) ? (d[i + 2] & 0xff) : 0;
            b.append(CH[(b0 >>> 2) & 63]);
            b.append(CH[((b0 << 4) | (b1 >>> 4)) & 63]);
            b.append((i + 1 < d.length) ? CH[((b1 << 2) | (b2 >>> 6)) & 63] : '=');
            b.append((i + 2 < d.length) ? CH[b2 & 63] : '=');
            i += 3;
        }
        return b.toString();
    }

    public static byte[] decode(String s) {
        if (s == null) return new byte[0];
        int len = s.length();
        byte[] out = new byte[len * 3 / 4 + 3];
        int o = 0;
        int acc = 0;
        int bits = 0;
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            int v;
            if (c >= 'A' && c <= 'Z') v = c - 'A';
            else if (c >= 'a' && c <= 'z') v = c - 'a' + 26;
            else if (c >= '0' && c <= '9') v = c - '0' + 52;
            else if (c == '+') v = 62;
            else if (c == '/') v = 63;
            else if (c == '=' || c == '\n' || c == '\r') continue;
            else throw new IllegalArgumentException("base64: bad char");
            acc = (acc << 6) | v;
            bits += 6;
            if (bits >= 8) {
                bits -= 8;
                out[o++] = (byte) ((acc >>> bits) & 0xff);
            }
        }
        byte[] r = new byte[o];
        System.arraycopy(out, 0, r, 0, o);
        return r;
    }
}
