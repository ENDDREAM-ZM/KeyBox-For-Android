package com.enddream.keybox.core;

import java.util.Map;

public class Entry {
    public String id = "";
    public String title = "";
    public String user = "";
    public String pass = "";
    public String url = "";
    public java.util.List<String> urls = new java.util.ArrayList<>();
    public String note = "";
    public String tags = "";
    public String cat = "";
    public String pkg = "";
    public java.util.List<Field> fields = new java.util.ArrayList<>();
    public int fav;
    public int del;
    public long ctime;
    public long utime;
    public long dtime;

    public static class Field {
        public String name = "";
        public int type;
        public String value = "";
        public int copy;

        public Field() {}

        public Field(String name, int type, String value, int copy) {
            this.name = name == null ? "" : name;
            this.type = type;
            this.value = value == null ? "" : value;
            this.copy = copy;
        }

        public boolean on() {
            return "1".equals(value);
        }

        public String text() {
            return type == 2 ? (on() ? "是" : "否") : value;
        }

        public String toJson() {
            StringBuilder b = new StringBuilder(64);
            b.append("{\"name\":").append(Json.quote(name));
            b.append(",\"type\":").append(type);
            b.append(",\"value\":").append(Json.quote(value));
            b.append(",\"copy\":").append(copy);
            b.append('}');
            return b.toString();
        }

        public static Field fromJson(Object o) {
            Map<String, Object> m = Json.asObj(o);
            if (m == null) return null;
            Field f = new Field();
            f.name = Json.getStr(m, "name", "");
            f.type = (int) Json.getNum(m, "type", 0);
            f.value = Json.getStr(m, "value", "");
            f.copy = (int) Json.getNum(m, "copy", 0);
            return f;
        }

        public Field copy() {
            return new Field(name, type, value, copy);
        }
    }

    public Entry() {}

    public String displayTitle() {
        if (title != null && title.length() > 0) return title;
        if (url != null && url.length() > 0) return url;
        return "(未命名)";
    }

    public java.util.List<String> urlList() {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String u : urls) {
            if (u != null && u.trim().length() > 0) out.add(u.trim());
        }
        if (out.isEmpty() && url != null && url.trim().length() > 0) out.add(url.trim());
        return out;
    }

    public String primaryUrl() {
        java.util.List<String> l = urlList();
        return l.isEmpty() ? "" : l.get(0);
    }

    public String host() {
        if (url == null) return "";
        String u = url.trim();
        int i = u.indexOf("://");
        if (i >= 0) u = u.substring(i + 3);
        int j = u.indexOf('/');
        if (j >= 0) u = u.substring(0, j);
        int k = u.indexOf('?');
        if (k >= 0) u = u.substring(0, k);
        int at = u.indexOf('@');
        if (at >= 0) u = u.substring(at + 1);
        return u;
    }

    public String toJson() {
        StringBuilder b = new StringBuilder(256);
        b.append("{\"id\":").append(Json.quote(id));
        b.append(",\"title\":").append(Json.quote(title));
        b.append(",\"user\":").append(Json.quote(user));
        b.append(",\"pass\":").append(Json.quote(pass));
        b.append(",\"url\":").append(Json.quote(primaryUrl()));
        b.append(",\"urls\":[");
        java.util.List<String> ul = urlList();
        for (int i = 0; i < ul.size(); i++) {
            if (i > 0) b.append(',');
            b.append(Json.quote(ul.get(i)));
        }
        b.append(']');
        b.append(",\"note\":").append(Json.quote(note));
        b.append(",\"tags\":").append(Json.quote(tags));
        b.append(",\"cat\":").append(Json.quote(cat));
        b.append(",\"pkg\":").append(Json.quote(pkg));
        if (!fields.isEmpty()) {
            b.append(",\"fields\":[");
            for (int i = 0; i < fields.size(); i++) {
                if (i > 0) b.append(',');
                b.append(fields.get(i).toJson());
            }
            b.append(']');
        }
        b.append(",\"fav\":").append(fav);
        b.append(",\"ctime\":").append(ctime);
        b.append(",\"utime\":").append(utime);
        b.append(",\"del\":").append(del);
        b.append(",\"dtime\":").append(dtime);
        b.append('}');
        return b.toString();
    }

    public static Entry fromJson(Object o) {
        Map<String, Object> m = Json.asObj(o);
        if (m == null) return null;
        Entry e = new Entry();
        e.id = Json.getStr(m, "id", "");
        e.title = Json.getStr(m, "title", "");
        e.user = Json.getStr(m, "user", "");
        e.pass = Json.getStr(m, "pass", "");
        e.url = Json.getStr(m, "url", "");
        java.util.List<Object> us = Json.asList(m.get("urls"));
        if (us != null) {
            for (Object uo : us) {
                if (uo instanceof String && ((String) uo).trim().length() > 0) {
                    e.urls.add(((String) uo).trim());
                }
            }
        }
        e.note = Json.getStr(m, "note", "");
        e.tags = Json.getStr(m, "tags", "");
        e.cat = Json.getStr(m, "cat", "");
        e.pkg = Json.getStr(m, "pkg", "");
        java.util.List<Object> fs = Json.asList(m.get("fields"));
        if (fs != null) {
            for (Object fo : fs) {
                Field f = Field.fromJson(fo);
                if (f != null && f.name.trim().length() > 0) e.fields.add(f);
            }
        }
        e.fav = (int) Json.getNum(m, "fav", 0);
        e.del = (int) Json.getNum(m, "del", 0);
        e.ctime = Json.getNum(m, "ctime", 0);
        e.utime = Json.getNum(m, "utime", e.ctime);
        e.dtime = Json.getNum(m, "dtime", 0);
        return e;
    }

    public Entry copy() {
        Entry e = new Entry();
        e.id = id;
        e.title = title;
        e.user = user;
        e.pass = pass;
        e.url = url;
        e.urls = new java.util.ArrayList<>(urls);
        e.note = note;
        e.tags = tags;
        e.cat = cat;
        e.pkg = pkg;
        e.fields = new java.util.ArrayList<>();
        for (Field f : fields) e.fields.add(f.copy());
        e.fav = fav;
        e.del = del;
        e.ctime = ctime;
        e.utime = utime;
        e.dtime = dtime;
        return e;
    }
}
