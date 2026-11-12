package com.enddream.keybox.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Json {

    private final String s;
    private int i;

    private Json(String s) {
        this.s = s;
    }

    public static Object parse(String text) {
        Json p = new Json(text);
        return p.value();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asObj(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : null;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> asList(Object o) {
        return o instanceof List ? (List<Object>) o : null;
    }

    public static String getStr(Map<String, Object> m, String key, String def) {
        if (m == null) return def;
        Object o = m.get(key);
        return o instanceof String ? (String) o : def;
    }

    public static long getNum(Map<String, Object> m, String key, long def) {
        if (m == null) return def;
        Object o = m.get(key);
        return o instanceof Long ? ((Long) o).longValue() : def;
    }

    public static String quote(String s) {
        StringBuilder b = new StringBuilder(s == null ? 2 : s.length() + 2);
        b.append('"');
        if (s != null) {
            for (int k = 0; k < s.length(); k++) {
                char c = s.charAt(k);
                switch (c) {
                    case '"': b.append("\\\""); break;
                    case '\\': b.append("\\\\"); break;
                    case '\n': b.append("\\n"); break;
                    case '\r': b.append("\\r"); break;
                    case '\t': b.append("\\t"); break;
                    case '\b': b.append("\\b"); break;
                    case '\f': b.append("\\f"); break;
                    default:
                        if (c < 0x20) {
                            String h = Integer.toHexString(c);
                            b.append("\\u");
                            for (int z = h.length(); z < 4; z++) b.append('0');
                            b.append(h);
                        } else {
                            b.append(c);
                        }
                }
            }
        }
        b.append('"');
        return b.toString();
    }

    private void ws() {
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') i++;
            else break;
        }
    }

    private Object value() {
        ws();
        if (i >= s.length()) throw new IllegalArgumentException("json: unexpected end");
        char c = s.charAt(i);
        switch (c) {
            case '{': return object();
            case '[': return array();
            case '"': return string();
            case 't': expect("true"); return Boolean.TRUE;
            case 'f': expect("false"); return Boolean.FALSE;
            case 'n': expect("null"); return null;
            default: return number();
        }
    }

    private void expect(String w) {
        if (!s.startsWith(w, i)) throw new IllegalArgumentException("json: bad literal");
        i += w.length();
    }

    private Map<String, Object> object() {
        Map<String, Object> m = new LinkedHashMap<>();
        i++;
        ws();
        if (i < s.length() && s.charAt(i) == '}') { i++; return m; }
        while (true) {
            ws();
            String k = string();
            ws();
            if (i < s.length() && s.charAt(i) == ':') i++;
            ws();
            m.put(k, value());
            ws();
            if (i < s.length() && s.charAt(i) == ',') { i++; continue; }
            if (i < s.length() && s.charAt(i) == '}') { i++; break; }
            throw new IllegalArgumentException("json: bad object");
        }
        return m;
    }

    private List<Object> array() {
        List<Object> l = new ArrayList<>();
        i++;
        ws();
        if (i < s.length() && s.charAt(i) == ']') { i++; return l; }
        while (true) {
            ws();
            l.add(value());
            ws();
            if (i < s.length() && s.charAt(i) == ',') { i++; continue; }
            if (i < s.length() && s.charAt(i) == ']') { i++; break; }
            throw new IllegalArgumentException("json: bad array");
        }
        return l;
    }

    private String string() {
        if (i >= s.length() || s.charAt(i) != '"') throw new IllegalArgumentException("json: expect string");
        i++;
        StringBuilder b = new StringBuilder();
        while (i < s.length()) {
            char c = s.charAt(i++);
            if (c == '"') return b.toString();
            if (c == '\\') {
                if (i >= s.length()) break;
                char e = s.charAt(i++);
                switch (e) {
                    case 'n': b.append('\n'); break;
                    case 't': b.append('\t'); break;
                    case 'r': b.append('\r'); break;
                    case 'b': b.append('\b'); break;
                    case 'f': b.append('\f'); break;
                    case '/': b.append('/'); break;
                    case '"': b.append('"'); break;
                    case '\\': b.append('\\'); break;
                    case 'u':
                        if (i + 4 <= s.length()) {
                            b.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                            i += 4;
                        }
                        break;
                    default: b.append(e); break;
                }
            } else {
                b.append(c);
            }
        }
        throw new IllegalArgumentException("json: unterminated string");
    }

    private Long number() {
        int start = i;
        while (i < s.length()) {
            char c = s.charAt(i);
            if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') i++;
            else break;
        }
        if (start == i) throw new IllegalArgumentException("json: bad number");
        String t = s.substring(start, i);
        try {
            return Long.valueOf(Long.parseLong(t));
        } catch (NumberFormatException ex) {
            return Long.valueOf((long) Double.parseDouble(t));
        }
    }
}
