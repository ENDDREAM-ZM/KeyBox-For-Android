package com.enddream.keybox.core;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class VaultCrypto {

    public static final byte[] MAGIC = {'K', 'E', 'Y', 'B', 'O', 'X'};
    public static final byte[] EXPORT_MAGIC = {'K', 'B', 'O', 'X', 'E', 'X', 'P', '1'};
    public static final int MAX_HEADER = 4096;
    public static final int DEFAULT_ITER = 600000;
    private static final int TAG_LEN = 16;
    private static final int MAC_LEN = 32;

    public static final Charset UTF8 = Charset.forName("UTF-8");
    private static final SecureRandom RNG = new SecureRandom();

    public static final int KIND_KEYBOX = 0;
    public static final int KIND_JSON = 1;
    public static final int KIND_CSV = 2;

    private VaultCrypto() {}

    public static final class VaultException extends Exception {
        public VaultException(String m) { super(m); }
    }

    public static byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        RNG.nextBytes(b);
        return b;
    }

    private static final ExecutorService KDF_POOL = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "keybox-kdf");
        t.setDaemon(true);
        return t;
    });

    private static final class KeyCache {
        private static final int MAX = 8;
        private final LinkedHashMap<String, byte[]> map = new LinkedHashMap<String, byte[]>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, byte[]> e) {
                return size() > MAX;
            }
        };
    }

    private static final KeyCache CACHE = new KeyCache();

    private static String kdfKey(String password, byte[] salt, int iter, int outLen) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(password.getBytes(UTF8));
            md.update(salt);
            md.update((byte) (iter >>> 24));
            md.update((byte) (iter >>> 16));
            md.update((byte) (iter >>> 8));
            md.update((byte) iter);
            md.update((byte) outLen);
            return B64.encode(md.digest());
        } catch (Exception e) {
            return null;
        }
    }

    public static void clearKeyCache() {
        synchronized (CACHE.map) {
            CACHE.map.clear();
        }
    }

    private static byte[] pbkdf2Block(byte[] pw, byte[] salt, int iter, int block) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(pw, "HmacSHA256"));
        byte[] msg = new byte[salt.length + 4];
        System.arraycopy(salt, 0, msg, 0, salt.length);
        msg[salt.length] = (byte) (block >>> 24);
        msg[salt.length + 1] = (byte) (block >>> 16);
        msg[salt.length + 2] = (byte) (block >>> 8);
        msg[salt.length + 3] = (byte) block;
        byte[] u = mac.doFinal(msg);
        byte[] t = u.clone();
        for (int k = 1; k < iter; k++) {
            u = mac.doFinal(u);
            for (int j = 0; j < t.length; j++) t[j] ^= u[j];
        }
        return t;
    }

    private static byte[] pbkdf2Raw(String password, byte[] salt, int iter, int outLen) throws Exception {
        byte[] pw = password.getBytes(UTF8);
        int blocks = (outLen + 31) / 32;
        byte[] out = new byte[outLen];
        if (blocks == 1 || iter < 4096) {
            for (int b = 1; b <= blocks; b++) {
                byte[] t = pbkdf2Block(pw, salt, iter, b);
                System.arraycopy(t, 0, out, (b - 1) * 32, Math.min(32, outLen - (b - 1) * 32));
            }
            return out;
        }
        @SuppressWarnings("unchecked")
        Future<byte[]>[] fs = new Future[blocks];
        for (int b = 1; b <= blocks; b++) {
            final int bb = b;
            fs[b - 1] = KDF_POOL.submit(new Callable<byte[]>() {
                public byte[] call() throws Exception {
                    return pbkdf2Block(pw, salt, iter, bb);
                }
            });
        }
        for (int b = 1; b <= blocks; b++) {
            byte[] t = fs[b - 1].get();
            System.arraycopy(t, 0, out, (b - 1) * 32, Math.min(32, outLen - (b - 1) * 32));
        }
        return out;
    }

    public static byte[] pbkdf2(String password, byte[] salt, int iter, int outLen) throws Exception {
        return pbkdf2Raw(password, salt, iter, outLen);
    }

    public static byte[] aesGcmEncrypt(byte[] key, byte[] nonce, byte[] aad, byte[] pt) throws Exception {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
        if (aad != null && aad.length > 0) c.updateAAD(aad);
        return c.doFinal(pt);
    }

    public static byte[] aesGcmDecrypt(byte[] key, byte[] nonce, byte[] aad, byte[] ct) throws Exception {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
        if (aad != null && aad.length > 0) c.updateAAD(aad);
        return c.doFinal(ct);
    }

    private static byte[] chachaEncrypt(byte[] key, byte[] nonce, byte[] aad, byte[] pt) throws Exception {
        Cipher c = Cipher.getInstance("ChaCha20-Poly1305");
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "ChaCha20"), new IvParameterSpec(nonce));
        if (aad != null && aad.length > 0) c.updateAAD(aad);
        return c.doFinal(pt);
    }

    private static byte[] chachaDecrypt(byte[] key, byte[] nonce, byte[] aad, byte[] ct) throws Exception {
        Cipher c = Cipher.getInstance("ChaCha20-Poly1305");
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "ChaCha20"), new IvParameterSpec(nonce));
        if (aad != null && aad.length > 0) c.updateAAD(aad);
        return c.doFinal(ct);
    }

    public static byte[] aesCtr(byte[] key, byte[] iv, byte[] in) throws Exception {
        Cipher c = Cipher.getInstance("AES/CTR/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        return c.doFinal(in);
    }

    private static byte[] hmac(byte[] key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private static byte[] seal(byte[] magic, String extra, String password, byte[] pt, int iter)
            throws Exception {
        byte[] salt = randomBytes(16);
        byte[] iv1 = randomBytes(16);
        byte[] nonce2 = randomBytes(12);
        byte[] nonce3 = randomBytes(12);
        byte[] keys = pbkdf2(password, salt, iter, 160);

        StringBuilder h = new StringBuilder();
        h.append("{\"ver\":4");
        if (extra != null && extra.length() > 0) h.append(',').append(extra);
        h.append(",\"kdf\":\"PBKDF2-HMAC-SHA256\",\"iter\":").append(iter);
        h.append(",\"layers\":\"aesctr+hmac+aesgcm+chacha20poly1305+hmac\"");
        h.append(",\"salt\":").append(Json.quote(B64.encode(salt)));
        h.append(",\"nonce\":").append(Json.quote(B64.encode(iv1)));
        h.append(",\"nonce2\":").append(Json.quote(B64.encode(nonce2)));
        h.append(",\"nonce3\":").append(Json.quote(B64.encode(nonce3)));
        h.append('}');
        byte[] header = h.toString().getBytes(UTF8);

        ByteArrayOutputStream aad = new ByteArrayOutputStream();
        aad.write(magic);
        aad.write((header.length >>> 24) & 0xff);
        aad.write((header.length >>> 16) & 0xff);
        aad.write((header.length >>> 8) & 0xff);
        aad.write(header.length & 0xff);
        aad.write(header, 0, header.length);
        byte[] aadBytes = aad.toByteArray();

        byte[] kCtr = slice(keys, 0, 32);
        byte[] kAes = slice(keys, 32, 32);
        byte[] kCha = slice(keys, 64, 32);
        byte[] kMac1 = slice(keys, 96, 32);
        byte[] kMac = slice(keys, 128, 32);

        byte[] l1ct = aesCtr(kCtr, iv1, pt);
        ByteArrayOutputStream l1 = new ByteArrayOutputStream();
        l1.write(l1ct, 0, l1ct.length);
        l1.write(hmac(kMac1, l1ct), 0, MAC_LEN);
        byte[] c1 = l1.toByteArray();

        byte[] c2 = aesGcmEncrypt(kAes, nonce2, aadBytes, c1);
        byte[] c3 = chachaEncrypt(kCha, nonce3, aadBytes, c2);

        ByteArrayOutputStream macIn = new ByteArrayOutputStream();
        macIn.write(aadBytes, 0, aadBytes.length);
        macIn.write(c3, 0, c3.length);
        byte[] mac = hmac(kMac, macIn.toByteArray());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(aadBytes, 0, aadBytes.length);
        out.write(c3, 0, c3.length);
        out.write(mac, 0, MAC_LEN);
        return out.toByteArray();
    }

    private static byte[] slice(byte[] a, int off, int len) {
        byte[] r = new byte[len];
        System.arraycopy(a, off, r, 0, len);
        return r;
    }

    private static byte[] open(byte[] magic, String password, byte[] data, String[] kindOut)
            throws Exception {
        if (data.length < magic.length + 4 + MAC_LEN) throw new VaultException("文件太小或已损坏");
        for (int k = 0; k < magic.length; k++) {
            if (data[k] != magic[k]) {
                throw new VaultException(magic.length == 6 ? "不是 KeyBox 保险库文件" : "不是 KeyBox 加密导出文件");
            }
        }
        int mlen = magic.length;
        int hlen = ((data[mlen] & 0xff) << 24) | ((data[mlen + 1] & 0xff) << 16)
                | ((data[mlen + 2] & 0xff) << 8) | (data[mlen + 3] & 0xff);
        if (hlen <= 0 || hlen > MAX_HEADER || mlen + 4 + hlen + MAC_LEN > data.length) {
            throw new VaultException("文件头长度非法");
        }
        String headerText = new String(data, mlen + 4, hlen, UTF8);
        java.util.Map<String, Object> h = Json.asObj(Json.parse(headerText));
        if (h == null) throw new VaultException("文件头 JSON 损坏");
        long iter = Json.getNum(h, "iter", DEFAULT_ITER);
        String saltB64 = Json.getStr(h, "salt", null);
        String nonceB64 = Json.getStr(h, "nonce", null);
        String nonce2B64 = Json.getStr(h, "nonce2", null);
        String nonce3B64 = Json.getStr(h, "nonce3", null);
        String layers = Json.getStr(h, "layers", "");
        if (kindOut != null) kindOut[0] = Json.getStr(h, "kind", "keybox");
        if (saltB64 == null || nonceB64 == null || nonce2B64 == null || iter < 1000
                || iter > 10000000) {
            throw new VaultException("文件头参数非法");
        }
        byte[] salt = B64.decode(saltB64);
        byte[] nonce = B64.decode(nonceB64);
        byte[] nonce2 = B64.decode(nonce2B64);
        byte[] nonce3 = nonce3B64 == null ? null : B64.decode(nonce3B64);
        if (salt.length != 16 || nonce2.length != 12 || nonce.length != 16) {
            throw new VaultException("文件头 salt/nonce 非法");
        }
        if (nonce3 == null || nonce3.length != 12) {
            throw new VaultException("文件头 nonce3 非法");
        }

        byte[] keys = pbkdf2(password, salt, (int) iter, 160);
        int off = mlen + 4 + hlen;
        int bodyLen = data.length - off;
        if (bodyLen < MAC_LEN + TAG_LEN) throw new VaultException("内容长度非法");
        int c3len = bodyLen - MAC_LEN;

        byte[] kCtr = slice(keys, 0, 32);
        byte[] kAes = slice(keys, 32, 32);
        byte[] kCha = slice(keys, 64, 32);
        byte[] kMac1 = slice(keys, 96, 32);
        byte[] kMac = slice(keys, 128, 32);

        byte[] aad = new byte[off];
        System.arraycopy(data, 0, aad, 0, off);

        ByteArrayOutputStream macIn = new ByteArrayOutputStream();
        macIn.write(aad, 0, aad.length);
        macIn.write(data, off, c3len);
        byte[] want = hmac(kMac, macIn.toByteArray());
        byte[] got = new byte[MAC_LEN];
        System.arraycopy(data, off + c3len, got, 0, MAC_LEN);
        if (!constEq(want, got)) throw new VaultException("密码错误，或文件已被篡改");

        byte[] c3 = new byte[c3len];
        System.arraycopy(data, off, c3, 0, c3len);
        byte[] c2;
        try {
            c2 = chachaDecrypt(kCha, nonce3, aad, c3);
        } catch (Exception e) {
            throw new VaultException("内容校验失败");
        }
        byte[] c1;
        try {
            c1 = aesGcmDecrypt(kAes, nonce2, aad, c2);
        } catch (Exception e) {
            throw new VaultException("内容校验失败");
        }
        if (c1.length < MAC_LEN) throw new VaultException("内容长度非法");
        byte[] inner = new byte[c1.length - MAC_LEN];
        System.arraycopy(c1, 0, inner, 0, inner.length);
        byte[] innerMac = new byte[MAC_LEN];
        System.arraycopy(c1, inner.length, innerMac, 0, MAC_LEN);
        if (!constEq(hmac(kMac1, inner), innerMac)) throw new VaultException("内容校验失败");
        return aesCtr(kCtr, nonce, inner);
    }

    private static boolean constEq(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int d = 0;
        for (int i = 0; i < a.length; i++) d |= a[i] ^ b[i];
        return d == 0;
    }

    public static byte[] sealVault(String password, byte[] payload, int iter) throws Exception {
        return seal(MAGIC, null, password, payload, iter);
    }

    public static byte[] openVault(String password, byte[] data) throws Exception {
        return open(MAGIC, password, data, null);
    }

    public static String kindName(int kind) {
        if (kind == KIND_CSV) return "csv";
        if (kind == KIND_JSON) return "json";
        return "keybox";
    }

    public static int kindOf(String name) {
        if ("csv".equals(name)) return KIND_CSV;
        if ("json".equals(name)) return KIND_JSON;
        return KIND_KEYBOX;
    }

    public static byte[] sealExport(String password, int kind, byte[] plain, int iter) throws Exception {
        return seal(EXPORT_MAGIC, "\"kind\":\"" + kindName(kind) + "\"", password, plain, iter);
    }

    public static byte[] openExport(String password, byte[] data, int[] kindOut) throws Exception {
        String[] k = new String[1];
        byte[] r = open(EXPORT_MAGIC, password, data, k);
        if (kindOut != null && kindOut.length > 0) kindOut[0] = kindOf(k[0]);
        return r;
    }

    public static int containerKind(byte[] data) {
        if (data == null || data.length < 8) return -1;
        if (startsWith(data, EXPORT_MAGIC)) return 1;
        if (startsWith(data, MAGIC)) return 0;
        return -1;
    }

    private static boolean startsWith(byte[] data, byte[] magic) {
        if (data.length < magic.length) return false;
        for (int i = 0; i < magic.length; i++) if (data[i] != magic[i]) return false;
        return true;
    }
}
