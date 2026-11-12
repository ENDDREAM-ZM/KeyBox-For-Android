package com.enddream.keybox.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Exporter {

    private Exporter() {}

    public static byte[] exportPlain(Vault v, int kind) {
        if (kind == VaultCrypto.KIND_CSV) return exportCsv(v).getBytes(VaultCrypto.UTF8);
        if (kind == VaultCrypto.KIND_JSON) return exportJson(v).getBytes(VaultCrypto.UTF8);
        return v.toPayload();
    }

    public static byte[] exportSealed(Vault v, int kind, String password, int iter) throws Exception {
        return VaultCrypto.sealExport(password, kind, exportPlain(v, kind), iter);
    }

    private static void csvField(StringBuilder b, String v) {
        if (v == null) v = "";
        boolean need = v.indexOf(',') >= 0 || v.indexOf('"') >= 0
                || v.indexOf('\n') >= 0 || v.indexOf('\r') >= 0;
        if (!need) {
            b.append(v);
            return;
        }
        b.append('"');
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if (c == '"') b.append("\"\"");
            else b.append(c);
        }
        b.append('"');
    }

    public static String exportCsv(Vault v) {
        StringBuilder b = new StringBuilder(256);
        b.append("name,url,username,password,note\r\n");
        for (Entry e : v.entries) {
            if (e.del == 1) continue;
            csvField(b, e.title);
            b.append(',');
            csvField(b, e.url);
            b.append(',');
            csvField(b, e.user);
            b.append(',');
            csvField(b, e.pass);
            b.append(',');
            csvField(b, joinNote(e.note, v.catName(e.cat), e.tags));
            b.append("\r\n");
        }
        return b.toString();
    }

    private static String joinNote(String note, String cat, String tags) {
        StringBuilder b = new StringBuilder();
        if (note != null) b.append(note);
        if (cat != null && cat.length() > 0) {
            if (b.length() > 0) b.append('\n');
            b.append("分类: ").append(cat);
        }
        if (tags != null && tags.length() > 0) {
            if (b.length() > 0) b.append('\n');
            b.append("标签: ").append(tags);
        }
        return b.toString();
    }

    public static String exportJson(Vault v) {
        StringBuilder b = new StringBuilder(512);
        b.append("{\n  \"encrypted\": false,\n  \"folders\": [");
        for (int i = 0; i < v.cats.size(); i++) {
            Category c = v.cats.get(i);
            if (i > 0) b.append(',');
            b.append("\n    {\"id\": ").append(Json.quote(c.id))
                    .append(", \"name\": ").append(Json.quote(c.name)).append('}');
        }
        b.append("\n  ],\n  \"items\": [");
        boolean first = true;
        for (Entry e : v.entries) {
            if (e.del == 1) continue;
            b.append(first ? "\n" : ",\n");
            first = false;
            b.append("    {\"id\": ").append(Json.quote(e.id));
            b.append(", \"name\": ").append(Json.quote(e.title));
            b.append(", \"type\": 1");
            if (e.cat != null && e.cat.length() > 0) {
                b.append(", \"folderId\": ").append(Json.quote(e.cat));
            }
            b.append(", \"favorite\": ").append(e.fav == 1);
            b.append(", \"notes\": ").append(Json.quote(e.note));
            b.append(", \"login\": {\"username\": ").append(Json.quote(e.user));
            b.append(", \"password\": ").append(Json.quote(e.pass));
            b.append(", \"uris\": [");
            java.util.List<String> ul = e.urlList();
            for (int k = 0; k < ul.size(); k++) {
                if (k > 0) b.append(", ");
                b.append("{\"uri\": ").append(Json.quote(ul.get(k))).append('}');
            }
            b.append("]}");
            StringBuilder fld = new StringBuilder();
            if (e.tags != null && e.tags.length() > 0) {
                fld.append("{\"name\": \"tags\", \"value\": ").append(Json.quote(e.tags))
                        .append(", \"type\": 0}");
            }
            if (e.pkg != null && e.pkg.length() > 0) {
                if (fld.length() > 0) fld.append(", ");
                fld.append("{\"name\": \"pkg\", \"value\": ").append(Json.quote(e.pkg))
                        .append(", \"type\": 0}");
            }
            if (fld.length() > 0) b.append(", \"fields\": [").append(fld).append(']');
            if (e.pkg != null && e.pkg.length() > 0) {
                b.append(", \"pkg\": ").append(Json.quote(e.pkg));
            }
            b.append(", \"revisionDate\": ").append(e.utime).append('}');
        }
        b.append("\n  ]\n}\n");
        return b.toString();
    }

    public static void importAny(String password, byte[] data, Vault dst, boolean merge)
            throws VaultCrypto.VaultException {
        int kind = VaultCrypto.containerKind(data);
        if (kind == 0) {
            byte[] payload;
            try {
                payload = VaultCrypto.openVault(password == null ? "" : password, data);
            } catch (VaultCrypto.VaultException e) {
                throw e;
            } catch (Exception e) {
                throw new VaultCrypto.VaultException("解密失败：" + e.getMessage());
            }
            Vault tmp = new Vault();
            try {
                tmp.loadPayload(payload);
            } catch (VaultCrypto.VaultException e) {
                throw e;
            } catch (Exception e) {
                throw new VaultCrypto.VaultException("主库内容损坏");
            }
            apply(dst, tmp, merge);
            return;
        }
        if (kind == 1) {
            int[] ek = new int[1];
            byte[] plain;
            try {
                plain = VaultCrypto.openExport(password == null ? "" : password, data, ek);
            } catch (VaultCrypto.VaultException e) {
                throw e;
            } catch (Exception e) {
                throw new VaultCrypto.VaultException("解密失败：" + e.getMessage());
            }
            if (ek[0] == VaultCrypto.KIND_KEYBOX) {
                Vault tmp = new Vault();
                try {
                    tmp.loadPayload(plain);
                } catch (VaultCrypto.VaultException e) {
                    throw e;
                } catch (Exception e) {
                    throw new VaultCrypto.VaultException("导出内容损坏");
                }
                apply(dst, tmp, merge);
            } else {
                importPlain(plain, dst, merge);
            }
            return;
        }
        importPlain(data, dst, merge);
    }

    public static void importPlain(byte[] data, Vault dst, boolean merge)
            throws VaultCrypto.VaultException {
        String text = new String(data, VaultCrypto.UTF8);
        int i = 0;
        while (i < text.length() && (Character.isWhitespace(text.charAt(i)) || text.charAt(i) == '\uFEFF')) i++;
        Vault tmp = new Vault();
        if (i < text.length() && (text.charAt(i) == '{' || text.charAt(i) == '[')) {
            importJson(text.substring(i), tmp);
        } else {
            importCsv(text, tmp);
        }
        if (tmp.entries.isEmpty()) throw new VaultCrypto.VaultException("没有解析到任何密码条目");
        apply(dst, tmp, merge);
    }

    private static final String[] ARR_KEYS = {
        "items", "entries", "logins", "passwords", "records", "accounts", "data", "vault", "list"
    };

    private static void importJson(String text, Vault tmp) throws VaultCrypto.VaultException {
        Object root;
        try {
            root = Json.parse(text);
        } catch (Exception e) {
            throw new VaultCrypto.VaultException("JSON 解析失败");
        }
        java.util.Map<String, String> folders = null;
        List<Object> items = null;

        if (root instanceof List) {
            items = Json.asList(root);
        } else {
            Map<String, Object> m = Json.asObj(root);
            if (m == null) throw new VaultCrypto.VaultException("JSON 结构不支持");
            List<Object> cs = Json.asList(m.get("categories"));
            if (cs != null) loadCategories(cs, tmp);
            folders = buildFolderMap(m.get("folders"));
            for (String k : ARR_KEYS) {
                List<Object> arr = Json.asList(m.get(k));
                if (arr != null && !arr.isEmpty()) {
                    items = arr;
                    break;
                }
            }
        }
        if (items == null) throw new VaultCrypto.VaultException("JSON 里找不到密码列表");

        for (Object o : items) {
            Map<String, Object> m = Json.asObj(o);
            if (m == null) continue;
            Entry e = new Entry();
            e.title = firstStr(m, K_TITLE);
            e.user = firstStr(m, K_USER);
            e.pass = firstStr(m, K_PASS);
            e.url = firstStr(m, K_URL);
            e.note = firstStr(m, K_NOTE);
            e.tags = firstStr(m, K_TAGS);
            e.id = firstStr(m, K_ID);
            e.pkg = firstStr(m, K_PKG);
            e.fav = truthy(firstVal(m, K_FAV)) ? 1 : 0;
            e.ctime = numVal(firstVal(m, K_CTIME));
            e.utime = numVal(firstVal(m, K_UTIME));
            e.del = truthy(firstVal(m, K_DEL)) ? 1 : 0;
            e.dtime = numVal(firstVal(m, K_DTIME));
            if (e.utime == 0) e.utime = e.ctime;

            Map<String, Object> login = Json.asObj(m.get("login"));
            if (login != null) {
                if (empty(e.user)) e.user = firstStr(login, K_USER);
                if (empty(e.pass)) e.pass = firstStr(login, K_PASS);
                if (empty(e.url)) e.url = uriOf(login.get("uris"));
            java.util.List<Object> uris = Json.asList(login.get("uris"));
            if (uris != null) {
                for (Object uo : uris) {
                    String us = null;
                    if (uo instanceof String) us = (String) uo;
                    else {
                        Map<String, Object> um = Json.asObj(uo);
                        if (um != null) us = Json.getStr(um, "uri", Json.getStr(um, "url", ""));
                    }
                    if (us != null && us.trim().length() > 0) e.urls.add(us.trim());
                }
            }
                if (empty(e.note)) e.note = firstStr(login, K_NOTE);
            }
            List<Object> fields = Json.asList(m.get("fields"));
            if (fields != null) {
                StringBuilder extra = new StringBuilder();
                for (Object fo : fields) {
                    Map<String, Object> fm = Json.asObj(fo);
                    if (fm == null) continue;
                    String fn = Json.getStr(fm, "name", "");
                    String fv = Json.getStr(fm, "value", "");
                    if (fv.length() == 0) continue;
                    if (fn.equalsIgnoreCase("tags") && empty(e.tags)) e.tags = fv;
                    else extra.append(fn).append(": ").append(fv).append('\n');
                }
                if (extra.length() > 0) {
                    if (!empty(e.note)) e.note = e.note + "\n";
                    e.note = (e.note == null ? "" : e.note) + extra.toString().trim();
                }
            }
            if (empty(e.pkg)) {
                List<Object> f2 = Json.asList(m.get("fields"));
                if (f2 != null) {
                    for (Object fo : f2) {
                        Map<String, Object> fm = Json.asObj(fo);
                        if (fm == null) continue;
                        String fn = Json.getStr(fm, "name", "");
                        if (fn.equalsIgnoreCase("pkg") || fn.equalsIgnoreCase("app")) {
                            e.pkg = Json.getStr(fm, "value", "");
                            break;
                        }
                    }
                }
            }
            String catName = firstStr(m, K_CAT);
            if (empty(catName) && folders != null) {
                String fid = Json.getStr(m, "folderId", null);
                if (fid != null) catName = folders.get(fid);
            }
            if (!empty(catName)) {
                Category c = tmp.catAdd(catName);
                if (c != null) e.cat = c.id;
            }
            finishEntry(e, tmp);
        }
    }

    private static void loadCategories(List<Object> cs, Vault tmp) {
        for (Object o : cs) {
            if (o instanceof String) {
                tmp.catAdd((String) o);
                continue;
            }
            Map<String, Object> cm = Json.asObj(o);
            if (cm == null) continue;
            Category c = tmp.catAdd(Json.getStr(cm, "name", "未命名"));
            String id = Json.getStr(cm, "id", null);
            if (c != null && id != null && id.length() > 0) c.id = id;
        }
    }

    private static java.util.Map<String, String> buildFolderMap(Object foldersObj) {
        List<Object> arr = Json.asList(foldersObj);
        if (arr == null) return null;
        java.util.Map<String, String> map = new java.util.HashMap<>();
        for (Object o : arr) {
            Map<String, Object> fm = Json.asObj(o);
            if (fm == null) continue;
            String id = Json.getStr(fm, "id", null);
            String name = Json.getStr(fm, "name", null);
            if (id != null && name != null) map.put(id, name);
        }
        return map;
    }

    private static String uriOf(Object urisObj) {
        List<Object> uris = Json.asList(urisObj);
        if (uris == null) return "";
        for (Object o : uris) {
            if (o instanceof String) return (String) o;
            Map<String, Object> m = Json.asObj(o);
            if (m != null) {
                String u = Json.getStr(m, "uri", Json.getStr(m, "url", ""));
                if (u.length() > 0) return u;
            }
        }
        return "";
    }

    private static void finishEntry(Entry e, Vault tmp) {
        if (empty(e.title)) {
            if (!empty(e.url)) e.title = hostOf(e.url);
            else if (!empty(e.user)) e.title = e.user;
            else e.title = "未命名";
        }
        if (empty(e.id)) e.id = Vault.newId();
        if (e.ctime == 0) e.ctime = e.utime = Vault.nowMs();
        if (e.utime == 0) e.utime = e.ctime;
        tmp.entries.add(e);
    }

    private static String hostOf(String url) {
        String u = url;
        int i = u.indexOf("://");
        if (i >= 0) u = u.substring(i + 3);
        int j = u.indexOf('/');
        if (j >= 0) u = u.substring(0, j);
        return u.length() == 0 ? url : u;
    }

    private static boolean empty(String s) {
        return s == null || s.length() == 0;
    }

    private static String firstStr(Map<String, Object> m, String[] keys) {
        Object o = firstVal(m, keys);
        return o instanceof String ? (String) o : "";
    }

    private static Object firstVal(Map<String, Object> m, String[] keys) {
        if (m == null) return null;
        for (Map.Entry<String, Object> en : m.entrySet()) {
            String k = en.getKey();
            for (String alias : keys) {
                if (k.equalsIgnoreCase(alias)) {
                    Object v = en.getValue();
                    if (v != null && !(v instanceof String && ((String) v).length() == 0)) return v;
                }
            }
        }
        return null;
    }

    private static boolean truthy(Object o) {
        if (o == null) return false;
        if (o instanceof Boolean) return (Boolean) o;
        if (o instanceof Long) return (Long) o != 0;
        if (o instanceof String) {
            String s = ((String) o).toLowerCase();
            return s.equals("1") || s.equals("true") || s.equals("yes") || s.equals("是");
        }
        return false;
    }

    private static long numVal(Object o) {
        if (o == null) return 0;
        if (o instanceof Long) return (Long) o;
        if (o instanceof String) {
            try {
                return Long.parseLong(((String) o).trim());
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    private static List<List<String>> csvParse(String s) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inq = false;
        int i = 0;
        int n = s.length();
        if (n >= 1 && s.charAt(0) == '\uFEFF') i = 1;
        while (i <= n) {
            char ch = i < n ? s.charAt(i) : '\n';
            if (inq) {
                if (ch == '"') {
                    if (i + 1 < n && s.charAt(i + 1) == '"') {
                        cur.append('"');
                        i += 2;
                        continue;
                    }
                    inq = false;
                    i++;
                    continue;
                }
                cur.append(ch);
                i++;
                continue;
            }
            if (ch == '"') {
                if (cur.length() == 0) {
                    inq = true;
                    i++;
                    continue;
                }
                cur.append('"');
                i++;
                continue;
            }
            if (ch == ',' || ch == ';' || ch == '\t') {
                row.add(cur.toString());
                cur.setLength(0);
                i++;
                continue;
            }
            if (ch == '\r' || ch == '\n' || i >= n) {
                row.add(cur.toString());
                cur.setLength(0);
                if (row.size() > 1 || (row.size() == 1 && row.get(0).length() > 0)) rows.add(row);
                row = new ArrayList<>();
                if (ch == '\r' && i + 1 < n && s.charAt(i + 1) == '\n') i += 2;
                else i++;
                continue;
            }
            cur.append(ch);
            i++;
        }
        return rows;
    }

    private static final String[] K_TITLE = {"title", "name", "account", "标题", "名称", "site", "sitename", "login_title"};
    private static final String[] K_USER = {"username", "user", "login", "login_username", "login name",
        "user_name", "account_name", "email", "用户名", "账号", "帐号", "账户"};
    private static final String[] K_PASS = {"password", "pass", "login_password", "pwd", "密码"};
    private static final String[] K_URL = {"url", "uri", "website", "web site", "login_uri", "loginurl",
        "site_url", "href", "网址", "链接", "网站"};
    private static final String[] K_NOTE = {"note", "notes", "comment", "comments", "extra", "remarks",
        "备注", "说明"};
    private static final String[] K_CAT = {"category", "folder", "grouping", "group",
        "collection", "分类", "分组"};
    private static final String[] K_TAGS = {"tags", "tag", "labels", "标签"};
    private static final String[] K_FAV = {"favorite", "fav", "starred", "收藏"};
    private static final String[] K_CTIME = {"created", "ctime", "createdat", "timecreated", "创建时间"};
    private static final String[] K_UTIME = {"updated", "utime", "modified", "revisiondate",
        "timelastused", "updatedat", "修改时间"};
    private static final String[] K_ID = {"id", "uuid", "guid"};
    private static final String[] K_PKG = {"pkg", "package", "app", "apppackage", "androidpackage",
        "packagename", "应用", "包名"};
    private static final String[] K_DEL = {"del", "deleted", "trashed"};
    private static final String[] K_DTIME = {"dtime", "deletedat"};

    private static boolean matchAny(String key, String[] names) {
        String k = key.trim();
        for (String n : names) if (n.equalsIgnoreCase(k)) return true;
        return false;
    }

    private static void importCsv(String text, Vault tmp) throws VaultCrypto.VaultException {
        List<List<String>> rows = csvParse(text);
        if (rows.isEmpty()) throw new VaultCrypto.VaultException("CSV 内容为空");

        int idxTitle = -1, idxUser = -1, idxPass = -1, idxUrl = -1, idxNote = -1;
        int idxCat = -1, idxTags = -1, idxFav = -1, idxCt = -1, idxUt = -1, idxId = -1;
        int idxPkg = -1;
        List<String> head = rows.get(0);
        for (int k = 0; k < head.size(); k++) {
            String h = head.get(k);
            if (matchAny(h, K_TITLE) && idxTitle < 0) idxTitle = k;
            else if (matchAny(h, K_USER) && idxUser < 0) idxUser = k;
            else if (matchAny(h, K_PASS) && idxPass < 0) idxPass = k;
            else if (matchAny(h, K_URL) && idxUrl < 0) idxUrl = k;
            else if (matchAny(h, K_NOTE) && idxNote < 0) idxNote = k;
            else if (matchAny(h, K_CAT) && idxCat < 0) idxCat = k;
            else if (matchAny(h, K_TAGS) && idxTags < 0) idxTags = k;
            else if (matchAny(h, K_FAV) && idxFav < 0) idxFav = k;
            else if (matchAny(h, K_CTIME) && idxCt < 0) idxCt = k;
            else if (matchAny(h, K_UTIME) && idxUt < 0) idxUt = k;
            else if (matchAny(h, K_ID) && idxId < 0) idxId = k;
            else if (matchAny(h, K_PKG) && idxPkg < 0) idxPkg = k;
        }
        boolean noHeader = idxTitle < 0 && idxUser < 0 && idxPass < 0;
        if (noHeader) {
            idxTitle = 0;
            idxUser = head.size() > 2 ? 2 : -1;
            idxPass = head.size() > 3 ? 3 : -1;
            idxUrl = head.size() > 1 ? 1 : -1;
        }

        for (int r = noHeader ? 0 : 1; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            Entry e = new Entry();
            e.title = get(row, idxTitle);
            e.user = get(row, idxUser);
            e.pass = get(row, idxPass);
            e.url = get(row, idxUrl);
            if (e.url.length() > 0) e.urls.add(e.url);
            e.note = get(row, idxNote);
            e.tags = get(row, idxTags);
            e.id = get(row, idxId);
            e.pkg = get(row, idxPkg);
            e.fav = truthy(get(row, idxFav)) ? 1 : 0;
            e.ctime = parseLong(get(row, idxCt));
            e.utime = parseLong(get(row, idxUt));
            if (e.utime == 0) e.utime = e.ctime;
            String cn = get(row, idxCat);
            if (cn.length() > 0) {
                Category c = tmp.catAdd(cn);
                if (c != null) e.cat = c.id;
            }
            if (empty(e.title) && empty(e.user) && empty(e.pass) && empty(e.url)) continue;
            finishEntry(e, tmp);
        }
        if (tmp.entries.isEmpty()) throw new VaultCrypto.VaultException("CSV 里没有可识别的条目");
    }

    private static String get(List<String> row, int i) {
        return (i >= 0 && i < row.size() && row.get(i) != null) ? row.get(i).trim() : "";
    }

    private static long parseLong(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private static void apply(Vault dst, Vault src, boolean merge) {
        if (!merge) {
            dst.entries.clear();
            dst.cats.clear();
            for (Category c : src.cats) dst.cats.add(c);
            for (Entry e : src.entries) dst.entries.add(e);
            return;
        }
        for (Category sc : src.cats) {
            Category dc = dst.catAdd(sc.name);
            if (dc == null) continue;
            if (sc.id != null && !sc.id.equals(dc.id)) {
                for (Entry e : src.entries) {
                    if (sc.id.equals(e.cat)) e.cat = dc.id;
                }
            }
        }
        for (Entry se : src.entries) {
            Entry hit = null;
            if (se.id != null && se.id.length() > 0) hit = dst.find(se.id);
            if (hit == null) {
                for (Entry de : dst.entries) {
                    if (eq(de.title, se.title) && eq(de.user, se.user)) {
                        hit = de;
                        break;
                    }
                }
            }
            if (hit != null) {
                if (se.utime >= hit.utime) {
                    String keepId = hit.id;
                    Entry copy = se.copy();
                    if (copy.id == null || copy.id.length() == 0) copy.id = keepId;
                    int idx = dst.entries.indexOf(hit);
                    dst.entries.set(idx, copy);
                }
            } else {
                dst.entries.add(se);
            }
        }
    }

    private static boolean eq(String a, String b) {
        return (a == null ? "" : a).equals(b == null ? "" : b);
    }
}
