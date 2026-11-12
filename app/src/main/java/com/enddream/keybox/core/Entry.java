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
    public int fav;
    public int del;
    public long ctime;
    public long utime;
    public long dtime;

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
        e.fav = fav;
        e.del = del;
        e.ctime = ctime;
        e.utime = utime;
        e.dtime = dtime;
        return e;
    }
}
