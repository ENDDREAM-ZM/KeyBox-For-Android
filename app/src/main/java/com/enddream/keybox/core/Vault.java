package com.enddream.keybox.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Vault {

    public final List<Entry> entries = new ArrayList<>();
    public final List<Category> cats = new ArrayList<>();

    public static long nowMs() {
        return System.currentTimeMillis();
    }

    public static String newId() {
        byte[] r = VaultCrypto.randomBytes(8);
        StringBuilder b = new StringBuilder(16);
        for (byte x : r) {
            b.append(Character.forDigit((x >> 4) & 15, 16));
            b.append(Character.forDigit(x & 15, 16));
        }
        return b.toString();
    }

    public Entry find(String id) {
        if (id == null) return null;
        for (Entry e : entries) if (id.equals(e.id)) return e;
        return null;
    }

    public Entry newEntry() {
        Entry e = new Entry();
        e.id = newId();
        e.ctime = nowMs();
        e.utime = e.ctime;
        entries.add(e);
        return e;
    }

    public boolean remove(String id) {
        Entry e = find(id);
        if (e == null) return false;
        entries.remove(e);
        return true;
    }

    public boolean trash(String id) {
        Entry e = find(id);
        if (e == null) return false;
        e.del = 1;
        e.dtime = nowMs();
        e.utime = e.dtime;
        return true;
    }

    public boolean restore(String id) {
        Entry e = find(id);
        if (e == null) return false;
        e.del = 0;
        e.dtime = 0;
        e.utime = nowMs();
        return true;
    }

    public void purge(long retentionMs) {
        long now = nowMs();
        List<Entry> kill = new ArrayList<>();
        for (Entry e : entries) {
            if (e.del == 1 && retentionMs > 0 && now - e.dtime > retentionMs) kill.add(e);
        }
        entries.removeAll(kill);
    }

    public Category catAdd(String name) {
        Category c = catByName(name);
        if (c != null) return c;
        c = new Category(newId(), name == null ? "" : name, cats.size());
        cats.add(c);
        return c;
    }

    public Category catById(String id) {
        if (id == null) return null;
        for (Category c : cats) if (id.equals(c.id)) return c;
        return null;
    }

    public Category catByName(String name) {
        if (name == null) return null;
        for (Category c : cats) if (name.equals(c.name)) return c;
        return null;
    }

    public String catName(String id) {
        Category c = catById(id);
        return c == null ? "" : c.name;
    }

    public boolean catRemove(String id) {
        Category c = catById(id);
        if (c == null) return false;
        cats.remove(c);
        for (Entry e : entries) if (id.equals(e.cat)) e.cat = "";
        return true;
    }

    public List<Entry> search(String needle) {
        List<Entry> r = new ArrayList<>();
        String n = needle == null ? "" : needle.toLowerCase();
        for (Entry e : entries) {
            if (e.del == 1) continue;
            if (n.length() == 0 || contains(e.title, n) || contains(e.user, n)
                    || contains(e.url, n) || contains(e.note, n) || contains(e.tags, n)
                    || contains(e.pass, n) || contains(catName(e.cat), n)) {
                r.add(e);
            }
        }
        return r;
    }

    public List<Entry> trashList() {
        List<Entry> r = new ArrayList<>();
        for (Entry e : entries) if (e.del == 1) r.add(e);
        return r;
    }

    private static boolean contains(String hay, String lowerNeedle) {
        return hay != null && hay.toLowerCase().contains(lowerNeedle);
    }

    public static int strength(String pass) {
        if (pass == null || pass.length() == 0) return 0;
        int sets = 0;
        for (int i = 0; i < pass.length(); i++) {
            char c = pass.charAt(i);
            if (c >= 'a' && c <= 'z') sets |= 1;
            else if (c >= 'A' && c <= 'Z') sets |= 2;
            else if (c >= '0' && c <= '9') sets |= 4;
            else sets |= 8;
        }
        int pool = 0;
        if ((sets & 1) != 0) pool += 26;
        if ((sets & 2) != 0) pool += 26;
        if ((sets & 4) != 0) pool += 10;
        if ((sets & 8) != 0) pool += 24;
        if (pool <= 0) pool = 26;
        double per;
        if (pool <= 26) per = 4.70;
        else if (pool <= 36) per = 5.17;
        else if (pool <= 52) per = 5.70;
        else if (pool <= 62) per = 5.95;
        else if (pool <= 72) per = 6.17;
        else per = 6.32;
        double bits = per * pass.length();
        if (bits >= 80) return 2;
        if (bits >= 50) return 1;
        return 0;
    }

    public int dupes(String pass, String selfId) {
        int n = 0;
        if (pass == null || pass.length() == 0) return 0;
        for (Entry e : entries) {
            if (e.del == 1) continue;
            if (pass.equals(e.pass) && (selfId == null || !selfId.equals(e.id))) n++;
        }
        return n;
    }

    public byte[] toPayload() {
        StringBuilder b = new StringBuilder(512 + entries.size() * 256);
        b.append("{\"app\":\"KeyBox\",\"ver\":2,\"updated\":").append(nowMs()).append(",\"categories\":[");
        for (int i = 0; i < cats.size(); i++) {
            Category c = cats.get(i);
            if (i > 0) b.append(',');
            b.append("{\"id\":").append(Json.quote(c.id))
                    .append(",\"name\":").append(Json.quote(c.name))
                    .append(",\"order\":").append(c.order).append('}');
        }
        b.append("],\"entries\":[");
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) b.append(',');
            b.append(entries.get(i).toJson());
        }
        b.append("]}");
        return b.toString().getBytes(VaultCrypto.UTF8);
    }

    public void loadPayload(byte[] payload) throws Exception {
        Object root = Json.parse(new String(payload, VaultCrypto.UTF8));
        Map<String, Object> m = Json.asObj(root);
        if (m == null) throw new VaultCrypto.VaultException("保险库内容损坏");
        cats.clear();
        List<Object> cs = Json.asList(m.get("categories"));
        if (cs != null) {
            for (Object o : cs) {
                Map<String, Object> cm = Json.asObj(o);
                if (cm == null) continue;
                Category c = new Category();
                c.id = Json.getStr(cm, "id", newId());
                c.name = Json.getStr(cm, "name", "未命名");
                c.order = Json.getNum(cm, "order", cats.size());
                cats.add(c);
            }
        }
        entries.clear();
        List<Object> arr = Json.asList(m.get("entries"));
        if (arr != null) {
            for (Object o : arr) {
                Entry e = Entry.fromJson(o);
                if (e == null) continue;
                if (e.id == null || e.id.length() == 0) e.id = newId();
                if (e.ctime == 0) e.ctime = e.utime = nowMs();
                entries.add(e);
            }
        }
    }

    public byte[] seal(String password, int iter) throws Exception {
        return VaultCrypto.sealVault(password, toPayload(), iter);
    }

    public void open(String password, byte[] fileBytes) throws Exception {
        loadPayload(VaultCrypto.openVault(password, fileBytes));
    }

    public static String generatePassword(int len, boolean lower, boolean upper, boolean digits,
                                          boolean symbols) {
        return generatePassword(len, lower, upper, digits, symbols, 0, 0);
    }

    public static String generatePassword(int len, boolean lower, boolean upper, boolean digits,
                                          boolean symbols, int minDigits, int minSymbols) {
        final String LOW = "abcdefghijkmnopqrstuvwxyz";
        final String UP = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        final String DIG = "23456789";
        final String SYM = "!@#$%^&*()-_=+[]{}:,.?";
        StringBuilder pool = new StringBuilder();
        if (lower) pool.append(LOW);
        if (upper) pool.append(UP);
        if (digits) pool.append(DIG);
        if (symbols) pool.append(SYM);
        if (pool.length() == 0) pool.append(LOW);
        if (len < 4) len = 4;
        if (len > 128) len = 128;
        if (minDigits < 0) minDigits = 0;
        if (minSymbols < 0) minSymbols = 0;
        if (!digits) minDigits = 0;
        if (!symbols) minSymbols = 0;
        while (minDigits + minSymbols > len) {
            if (minSymbols > minDigits) minSymbols--;
            else minDigits--;
        }
        StringBuilder b = new StringBuilder(len);
        for (int i = 0; i < minDigits; i++) b.append(pick(DIG));
        for (int i = 0; i < minSymbols; i++) b.append(pick(SYM));
        while (b.length() < len) b.append(pool.charAt(rand(b.length()) % pool.length()));
        char[] arr = b.toString().toCharArray();
        for (int i = arr.length - 1; i > 0; i--) {
            int j = rand(i + 1) % (i + 1);
            char tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }
        return new String(arr);
    }

    private static char pick(String set) {
        return set.charAt(rand(set.length()) % set.length());
    }

    private static int rand(int bound) {
        byte[] r = VaultCrypto.randomBytes(2);
        int v = ((r[0] & 0xff) << 8) | (r[1] & 0xff);
        return bound <= 0 ? 0 : v % bound;
    }

    private static final String[] NAME_POOL = {
        "alice", "bob", "carol", "david", "emma", "frank", "grace", "henry", "irene", "jack",
        "kate", "leo", "mary", "nick", "olivia", "peter", "quinn", "rose", "sam", "tina",
        "user", "admin", "test", "demo", "guest", "master", "shadow", "sunny", "river", "stone",
        "cloud", "forest", "silver", "golden", "happy", "lucky", "swift", "brave", "calm", "bright"};

    public static String generateUsername(int type, boolean cap, boolean digits, int len) {
        String s = generateUsername(type, cap, digits);
        if (len > 0 && s.length() > len) s = s.substring(0, len);
        return s;
    }

    public static String generateUsername(int type, boolean cap, boolean digits) {
        StringBuilder b = new StringBuilder();
        if (type == 1) {
            String cs = "abcdefghijkmnopqrstuvwxyz23456789";
            byte[] rnd = VaultCrypto.randomBytes(16);
            for (int i = 0; i < 8; i++) {
                int v = ((rnd[i * 2] & 0xff) << 8) | (rnd[i * 2 + 1] & 0xff);
                b.append(cs.charAt(v % cs.length()));
            }
        } else {
            byte[] rnd = VaultCrypto.randomBytes(4);
            int idx = (((rnd[0] & 0xff) << 8) | (rnd[1] & 0xff)) % NAME_POOL.length;
            b.append(NAME_POOL[idx]);
            if (digits) {
                int d = (((rnd[2] & 0xff) << 8) | (rnd[3] & 0xff)) % 100;
                b.append(d < 10 ? "0" + d : String.valueOf(d));
            }
        }
        String s = b.toString();
        if (cap && s.length() > 0 && s.charAt(0) >= 'a' && s.charAt(0) <= 'z') {
            s = (char) (s.charAt(0) - 'a' + 'A') + s.substring(1);
        }
        return s;
    }

    public static byte[] readAll(File f) throws IOException {
        return readAll(new FileInputStream(f));
    }

    public static byte[] readAll(InputStream in) throws IOException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toByteArray();
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static void writeAll(File f, byte[] data) throws IOException {
        File dir = f.getParentFile();
        if (dir != null && !dir.exists()) dir.mkdirs();
        OutputStream out = new FileOutputStream(f);
        try {
            out.write(data);
        } finally {
            out.close();
        }
    }
}
