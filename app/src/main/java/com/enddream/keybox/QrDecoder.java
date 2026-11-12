package com.enddream.keybox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class QrDecoder {

    private static final int[] EXP = new int[512];
    private static final int[] LOG = new int[256];

    static {
        int x = 1;
        for (int i = 0; i < 255; i++) {
            EXP[i] = x;
            LOG[x] = i;
            x <<= 1;
            if ((x & 0x100) != 0) x ^= 0x11d;
        }
        for (int i = 255; i < 512; i++) EXP[i] = EXP[i - 255];
    }

    private static int mul(int a, int b) {
        if (a == 0 || b == 0) return 0;
        return EXP[LOG[a] + LOG[b]];
    }

    private static final int[][] ALIGN = {
        {},
        {},
        {6, 18}, {6, 22}, {6, 26}, {6, 30}, {6, 34},
        {6, 22, 38}, {6, 24, 42}, {6, 26, 46}, {6, 28, 50},
        {6, 30, 54}, {6, 32, 58}, {6, 34, 62}
    };

    private static final class Ec {
        final int perBlock;
        final int[][] groups;

        Ec(int perBlock, int[][] groups) {
            this.perBlock = perBlock;
            this.groups = groups;
        }
    }

    private static final Map<Integer, Ec> EC = new HashMap<Integer, Ec>();

    private static void ec(int v, char l, int per, int[][] g) {
        EC.put(v * 4 + "LMQH".indexOf(l), new Ec(per, g));
    }

    static {
        ec(1, 'L', 7, new int[][]{{1, 19}});
        ec(1, 'M', 10, new int[][]{{1, 16}});
        ec(1, 'Q', 13, new int[][]{{1, 13}});
        ec(1, 'H', 17, new int[][]{{1, 9}});
        ec(2, 'L', 10, new int[][]{{1, 34}});
        ec(2, 'M', 16, new int[][]{{1, 28}});
        ec(2, 'Q', 22, new int[][]{{1, 22}});
        ec(2, 'H', 28, new int[][]{{1, 16}});
        ec(3, 'L', 15, new int[][]{{1, 55}});
        ec(3, 'M', 26, new int[][]{{1, 44}});
        ec(3, 'Q', 18, new int[][]{{2, 17}});
        ec(3, 'H', 22, new int[][]{{2, 13}});
        ec(4, 'L', 20, new int[][]{{1, 80}});
        ec(4, 'M', 18, new int[][]{{2, 32}});
        ec(4, 'Q', 26, new int[][]{{2, 24}});
        ec(4, 'H', 16, new int[][]{{4, 9}});
        ec(5, 'L', 26, new int[][]{{1, 108}});
        ec(5, 'M', 24, new int[][]{{2, 43}});
        ec(5, 'Q', 18, new int[][]{{2, 15}, {2, 16}});
        ec(5, 'H', 22, new int[][]{{2, 11}, {2, 12}});
        ec(6, 'L', 18, new int[][]{{2, 68}});
        ec(6, 'M', 16, new int[][]{{4, 27}});
        ec(6, 'Q', 24, new int[][]{{4, 19}});
        ec(6, 'H', 28, new int[][]{{4, 15}});
        ec(7, 'L', 20, new int[][]{{2, 78}});
        ec(7, 'M', 18, new int[][]{{4, 31}});
        ec(7, 'Q', 18, new int[][]{{2, 14}, {4, 15}});
        ec(7, 'H', 26, new int[][]{{4, 13}, {1, 14}});
        ec(8, 'L', 24, new int[][]{{2, 97}});
        ec(8, 'M', 22, new int[][]{{2, 38}, {2, 39}});
        ec(8, 'Q', 22, new int[][]{{4, 18}, {2, 19}});
        ec(8, 'H', 26, new int[][]{{4, 14}, {2, 15}});
        ec(9, 'L', 30, new int[][]{{2, 116}});
        ec(9, 'M', 22, new int[][]{{3, 36}, {2, 37}});
        ec(9, 'Q', 20, new int[][]{{4, 16}, {4, 17}});
        ec(9, 'H', 24, new int[][]{{4, 12}, {4, 13}});
        ec(10, 'L', 18, new int[][]{{2, 68}, {2, 69}});
        ec(10, 'M', 26, new int[][]{{4, 43}, {1, 44}});
        ec(10, 'Q', 24, new int[][]{{6, 19}, {2, 20}});
        ec(10, 'H', 28, new int[][]{{6, 15}, {2, 16}});
        ec(11, 'L', 20, new int[][]{{4, 81}});
        ec(11, 'M', 30, new int[][]{{1, 50}, {4, 51}});
        ec(11, 'Q', 28, new int[][]{{4, 22}, {4, 23}});
        ec(11, 'H', 24, new int[][]{{3, 12}, {8, 13}});
        ec(12, 'L', 24, new int[][]{{2, 92}, {2, 93}});
        ec(12, 'M', 22, new int[][]{{6, 36}, {2, 37}});
        ec(12, 'Q', 26, new int[][]{{4, 20}, {6, 21}});
        ec(12, 'H', 28, new int[][]{{7, 14}, {4, 15}});
        ec(13, 'L', 26, new int[][]{{4, 107}});
        ec(13, 'M', 22, new int[][]{{8, 37}, {1, 38}});
        ec(13, 'Q', 24, new int[][]{{8, 20}, {4, 21}});
        ec(13, 'H', 22, new int[][]{{12, 11}, {4, 12}});
    }

    private QrDecoder() {
    }

    public static String decode(byte[] luma, int width, int height, int stride) {
        try {
            boolean[][] bin = binarize(luma, width, height, stride);
            List<Finder> finders = findFinders(bin, width, height);
            if (finders.size() < 3) return null;
            Finder[] triple = pickTriple(finders);
            if (triple == null) return null;
            int dim = estimateDimension(triple);
            if (dim < 21 || dim > 77 || (dim - 17) % 4 != 0) return null;
            boolean[][] matrix = sample(bin, width, height, triple, dim);
            if (matrix == null) return null;
            return decodeMatrix(matrix);
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean[][] binarize(byte[] luma, int w, int h, int stride) {
        boolean[][] bin = new boolean[h][w];
        int[] hist = new int[256];
        int min = 255;
        int max = 0;
        long sumAll = 0;
        for (int y = 0; y < h; y++) {
            int off = y * stride;
            for (int x = 0; x < w; x++) {
                int v = luma[off + x] & 0xff;
                hist[v]++;
                if (v < min) min = v;
                if (v > max) max = v;
                sumAll += v;
            }
        }
        if (max - min < 20) return bin;
        int total = w * h;
        double sum = 0;
        for (int i = 0; i < 256; i++) sum += (double) i * hist[i];
        double sumB = 0;
        int wB = 0;
        double bestVar = -1;
        int otsu = (min + max) / 2;
        for (int t = 0; t < 256; t++) {
            wB += hist[t];
            if (wB == 0) continue;
            int wF = total - wB;
            if (wF == 0) break;
            sumB += (double) t * hist[t];
            double mB = sumB / wB;
            double mF = (sum - sumB) / wF;
            double between = (double) wB * wF * (mB - mF) * (mB - mF);
            if (between > bestVar) {
                bestVar = between;
                otsu = t;
            }
        }
        int bs = 8;
        int bw = (w + bs - 1) / bs;
        int bh = (h + bs - 1) / bs;
        int[] bmean = new int[bw * bh];
        for (int by = 0; by < bh; by++) {
            for (int bx = 0; bx < bw; bx++) {
                int s2 = 0;
                int n = 0;
                int y1 = Math.min((by + 1) * bs, h);
                int x1 = Math.min((bx + 1) * bs, w);
                for (int y = by * bs; y < y1; y++) {
                    int off = y * stride;
                    for (int x = bx * bs; x < x1; x++) {
                        s2 += luma[off + x] & 0xff;
                        n++;
                    }
                }
                bmean[by * bw + bx] = n > 0 ? s2 / n : otsu;
            }
        }
        for (int y = 0; y < h; y++) {
            int off = y * stride;
            int by = y / bs;
            for (int x = 0; x < w; x++) {
                int bx = x / bs;
                int s2 = 0;
                int n = 0;
                for (int dy = -2; dy <= 2; dy++) {
                    int yy = by + dy;
                    if (yy < 0 || yy >= bh) continue;
                    for (int dx = -2; dx <= 2; dx++) {
                        int xx = bx + dx;
                        if (xx < 0 || xx >= bw) continue;
                        s2 += bmean[yy * bw + xx];
                        n++;
                    }
                }
                int local = n > 0 ? s2 / n : otsu;
                int th = (local + otsu) / 2;
                bin[y][x] = (luma[off + x] & 0xff) < th;
            }
        }
        return bin;
    }

    private static final class Finder {
        double x;
        double y;
        double size;
        int count;

        Finder(double x, double y, double size) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.count = 1;
        }
    }

    private static double checkVertical(boolean[][] bin, int w, int h, int x, int y, double module) {
        if (x < 0 || x >= w) return -1;
        int[] rs = new int[h + 2];
        int[] rl = new int[h + 2];
        int n = 0;
        boolean cur = bin[0][x];
        int start = 0;
        for (int yy = 1; yy < h; yy++) {
            if (bin[yy][x] != cur) {
                rs[n] = start;
                rl[n] = yy - start;
                n++;
                cur = bin[yy][x];
                start = yy;
            }
        }
        rs[n] = start;
        rl[n] = h - start;
        n++;
        for (int i = 2; i + 2 < n; i++) {
            if (rs[i] > y || rs[i] + rl[i] <= y) continue;
            if (!bin[rs[i]][x]) return -1;
            int a = rl[i - 2];
            int b = rl[i - 1];
            int c = rl[i];
            int d = rl[i + 1];
            int e = rl[i + 2];
            int total = a + b + c + d + e;
            if (total < 7) return -1;
            if (i - 3 >= 0 && bin[rs[i - 3]][x]) return -1;
            if (i + 3 < n && bin[rs[i + 3]][x]) return -1;
            double m = total / 7.0;
            if (Math.abs(a - m) > m * 0.7 + 1) return -1;
            if (Math.abs(b - m) > m * 0.7 + 1) return -1;
            if (Math.abs(c - 3 * m) > 3 * m * 0.5 + 1) return -1;
            if (Math.abs(d - m) > m * 0.7 + 1) return -1;
            if (Math.abs(e - m) > m * 0.7 + 1) return -1;
            if (Math.abs(m - module) > module * 0.6) return -1;
            return rs[i] + rl[i] / 2.0;
        }
        return -1;
    }

    private static void addFinder(List<Finder> list, double x, double y, double size) {
        for (Finder f : list) {
            if (Math.abs(f.x - x) < size * 2 && Math.abs(f.y - y) < size * 2
                    && Math.abs(f.size - size) < size) {
                f.count++;
                return;
            }
        }
        list.add(new Finder(x, y, size));
    }

    private static List<Finder> findFinders(boolean[][] bin, int w, int h) {
        List<Finder> found = new ArrayList<Finder>();
        int[] rs = new int[w + 2];
        int[] rl = new int[w + 2];
        for (int y = 0; y < h; y++) {
            int n = 0;
            boolean cur = bin[y][0];
            int start = 0;
            for (int x = 1; x < w; x++) {
                if (bin[y][x] != cur) {
                    rs[n] = start;
                    rl[n] = x - start;
                    n++;
                    cur = bin[y][x];
                    start = x;
                }
            }
            rs[n] = start;
            rl[n] = w - start;
            n++;
            for (int i = 0; i + 4 < n; i++) {
                if (!bin[y][rs[i]]) continue;
                int a = rl[i];
                int b = rl[i + 1];
                int c = rl[i + 2];
                int d = rl[i + 3];
                int e = rl[i + 4];
                int total = a + b + c + d + e;
                if (total < 7) continue;
                if (i > 0 && bin[y][rs[i - 1]]) continue;
                if (i + 5 < n && bin[y][rs[i + 5]]) continue;
                double m = total / 7.0;
                if (Math.abs(a - m) > m * 0.7 + 1) continue;
                if (Math.abs(b - m) > m * 0.7 + 1) continue;
                if (Math.abs(c - 3 * m) > 3 * m * 0.5 + 1) continue;
                if (Math.abs(d - m) > m * 0.7 + 1) continue;
                if (Math.abs(e - m) > m * 0.7 + 1) continue;
                double cx = rs[i + 2] + rl[i + 2] / 2.0;
                double cy = checkVertical(bin, w, h, (int) cx, y, m);
                if (cy >= 0) addFinder(found, cx, cy, m);
            }
        }
        List<Finder> out = new ArrayList<Finder>();
        for (Finder f : found) if (f.count >= 3) out.add(f);
        for (int i = 0; i < out.size(); i++) {
            for (int j = i + 1; j < out.size(); j++) {
                if (out.get(j).count > out.get(i).count) {
                    Finder t = out.get(i);
                    out.set(i, out.get(j));
                    out.set(j, t);
                }
            }
        }
        return out;
    }

    private static double dist(Finder a, Finder b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private static Finder[] pickTriple(List<Finder> list) {
        if (list.isEmpty()) return null;
        int maxCount = list.get(0).count;
        List<Finder> cand = new ArrayList<Finder>();
        for (Finder f : list) if (f.count >= maxCount * 0.5) cand.add(f);
        int n = Math.min(cand.size(), 8);
        Finder[] best = null;
        double bestScore = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                for (int k = j + 1; k < n; k++) {
                    Finder a = cand.get(i);
                    Finder b = cand.get(j);
                    Finder c = cand.get(k);
                    if (Math.abs(a.size - b.size) > a.size * 0.35) continue;
                    if (Math.abs(a.size - c.size) > a.size * 0.35) continue;
                    Finder[] t = orderTriple(a, b, c);
                    if (t == null) continue;
                    double leg1 = dist(t[0], t[1]);
                    double leg2 = dist(t[0], t[2]);
                    if (Math.abs(leg1 - leg2) > Math.max(leg1, leg2) * 0.25) continue;
                    double score = Math.min(leg1, leg2);
                    if (score > bestScore) {
                        bestScore = score;
                        best = t;
                    }
                }
            }
        }
        return best;
    }

    private static Finder[] orderTriple(Finder a, Finder b, Finder c) {
        Finder[][] pairs = {{a, b, c}, {b, a, c}, {c, a, b}};
        for (Finder[] p : pairs) {
            double v1x = p[1].x - p[0].x;
            double v1y = p[1].y - p[0].y;
            double v2x = p[2].x - p[0].x;
            double v2y = p[2].y - p[0].y;
            double dot = v1x * v2x + v1y * v2y;
            double m1 = Math.sqrt(v1x * v1x + v1y * v1y);
            double m2 = Math.sqrt(v2x * v2x + v2y * v2y);
            if (m1 == 0 || m2 == 0) continue;
            if (Math.abs(dot) / (m1 * m2) < 0.25) return p;
        }
        return null;
    }

    private static int estimateDimension(Finder[] t) {
        double leg1 = dist(t[0], t[1]);
        double leg2 = dist(t[0], t[2]);
        double module = (t[0].size + t[1].size + t[2].size) / 3.0;
        if (module <= 0) return 0;
        double side = (leg1 + leg2) / 2.0;
        int dim = (int) Math.round(side / module) + 7;
        int v = (int) Math.round((dim - 17) / 4.0);
        if (v < 1 || v > 13) return 0;
        return 17 + 4 * v;
    }

    private static boolean[][] sample(boolean[][] bin, int w, int h, Finder[] t, int dim) {
        Finder corner = t[0];
        Finder p1 = t[1];
        Finder p2 = t[2];
        double cross = (p1.x - corner.x) * (p2.y - corner.y) - (p1.y - corner.y) * (p2.x - corner.x);
        Finder tr;
        Finder bl;
        if (cross > 0) {
            tr = p1;
            bl = p2;
        } else {
            tr = p2;
            bl = p1;
        }
        double[][] src = {
            {3.5, 3.5},
            {dim - 3.5, 3.5},
            {3.5, dim - 3.5},
            {dim - 3.5, dim - 3.5}
        };
        double[][] dst = {
            {corner.x, corner.y},
            {tr.x, tr.y},
            {bl.x, bl.y},
            {tr.x + bl.x - corner.x, tr.y + bl.y - corner.y}
        };
        double[] hm = homography(src, dst);
        if (hm == null) return null;
        boolean[][] out = new boolean[dim][dim];
        for (int j = 0; j < dim; j++) {
            for (int i = 0; i < dim; i++) {
                double px = hm[0] * (i + 0.5) + hm[1] * (j + 0.5) + hm[2];
                double py = hm[3] * (i + 0.5) + hm[4] * (j + 0.5) + hm[5];
                double pz = hm[6] * (i + 0.5) + hm[7] * (j + 0.5) + 1.0;
                if (pz == 0) return null;
                int x = (int) (px / pz);
                int y = (int) (py / pz);
                if (x < 0 || y < 0 || x >= w || y >= h) return null;
                out[j][i] = bin[y][x];
            }
        }
        return out;
    }

    private static double[] homography(double[][] src, double[][] dst) {
        double[][] a = new double[8][9];
        for (int i = 0; i < 4; i++) {
            double x = src[i][0];
            double y = src[i][1];
            double u = dst[i][0];
            double v = dst[i][1];
            a[i * 2] = new double[]{x, y, 1, 0, 0, 0, -u * x, -u * y, u};
            a[i * 2 + 1] = new double[]{0, 0, 0, x, y, 1, -v * x, -v * y, v};
        }
        for (int col = 0; col < 8; col++) {
            int piv = col;
            for (int r = col + 1; r < 8; r++) {
                if (Math.abs(a[r][col]) > Math.abs(a[piv][col])) piv = r;
            }
            if (Math.abs(a[piv][col]) < 1e-10) return null;
            double[] tmp = a[col];
            a[col] = a[piv];
            a[piv] = tmp;
            double d = a[col][col];
            for (int c = col; c < 9; c++) a[col][c] /= d;
            for (int r = 0; r < 8; r++) {
                if (r == col) continue;
                double f = a[r][col];
                if (f != 0) for (int c = col; c < 9; c++) a[r][c] -= f * a[col][c];
            }
        }
        return new double[]{a[0][8], a[1][8], a[2][8], a[3][8], a[4][8], a[5][8], a[6][8], a[7][8]};
    }

    private static String decodeMatrix(boolean[][] m) {
        int dim = m.length;
        int version = (dim - 17) / 4;
        int fmt = readFormat(m);
        if (fmt < 0) return null;
        char ecl = "MLQH".charAt(fmt >> 3 & 3);
        int mask = fmt & 7;
        Ec ec = EC.get(version * 4 + "LMQH".indexOf(ecl));
        if (ec == null) return null;
        boolean[] bits = readBits(m, version, mask);
        int totalData = 0;
        int blocks = 0;
        for (int[] g : ec.groups) {
            totalData += g[0] * g[1];
            blocks += g[0];
        }
        int totalCw = totalData + blocks * ec.perBlock;
        if (bits.length < totalCw * 8) return null;
        int[] cw = new int[totalCw];
        for (int i = 0; i < totalCw; i++) {
            int b = 0;
            for (int k = 0; k < 8; k++) b = (b << 1) | (bits[i * 8 + k] ? 1 : 0);
            cw[i] = b;
        }
        List<Integer> lens = new ArrayList<Integer>();
        for (int[] g : ec.groups) for (int c = 0; c < g[0]; c++) lens.add(g[1]);
        int nb = lens.size();
        int[][] dataBlocks = new int[nb][];
        for (int i = 0; i < nb; i++) dataBlocks[i] = new int[lens.get(i)];
        int idx = 0;
        int maxLen = 0;
        for (int l : lens) maxLen = Math.max(maxLen, l);
        for (int i = 0; i < maxLen; i++) {
            for (int b = 0; b < nb; b++) {
                if (i < lens.get(b)) dataBlocks[b][i] = cw[idx++];
            }
        }
        int[][] eccBlocks = new int[nb][ec.perBlock];
        for (int i = 0; i < ec.perBlock; i++) {
            for (int b = 0; b < nb; b++) eccBlocks[b][i] = cw[idx++];
        }
        List<Integer> data = new ArrayList<Integer>();
        for (int b = 0; b < nb; b++) {
            int[] corrected = correctBlock(dataBlocks[b], eccBlocks[b]);
            if (corrected == null) return null;
            for (int v : corrected) data.add(v);
        }
        return parseData(data);
    }

    private static int readFormat(boolean[][] m) {
        int dim = m.length;
        int v1 = 0;
        int v2 = 0;
        for (int i = 0; i < 15; i++) {
            boolean b1;
            if (i < 6) b1 = m[8][i];
            else if (i < 8) b1 = m[8][i + 1];
            else b1 = m[8][dim - 15 + i];
            if (b1) v1 |= 1 << (14 - i);
            boolean b2 = i < 8 ? m[dim - 1 - i][8] : m[15 - i - 1][8];
            if (b2) v2 |= 1 << (14 - i);
        }
        int f1 = correctFormat(v1);
        int f2 = correctFormat(v2);
        if (f1 >= 0) return f1;
        if (f2 >= 0) return f2;
        return -1;
    }

    private static int correctFormat(int v) {
        int best = -1;
        int bestDist = 99;
        for (int d = 0; d < 32; d++) {
            int diff = Integer.bitCount(bch15(d) ^ v);
            if (diff < bestDist) {
                bestDist = diff;
                best = d;
            }
        }
        return bestDist <= 3 ? best : -1;
    }

    private static int bch15(int d) {
        int v = d << 10;
        for (int i = 4; i >= 0; i--) {
            if (((v >> (i + 10)) & 1) == 1) v ^= 0x537 << i;
        }
        return ((d << 10) | v) ^ 0x5412;
    }

    private static boolean maskFn(int mask, int i, int j) {
        switch (mask) {
            case 0: return (i + j) % 2 == 0;
            case 1: return i % 2 == 0;
            case 2: return j % 3 == 0;
            case 3: return (i + j) % 3 == 0;
            case 4: return (i / 2 + j / 3) % 2 == 0;
            case 5: return (i * j) % 2 + (i * j) % 3 == 0;
            case 6: return ((i * j) % 2 + (i * j) % 3) % 2 == 0;
            default: return ((i + j) % 2 + (i * j) % 3) % 2 == 0;
        }
    }

    private static boolean[][] functionMask(int version) {
        int dim = 17 + 4 * version;
        boolean[][] f = new boolean[dim][dim];
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) f[i][j] = true;
        }
        for (int i = 0; i < 9; i++) {
            for (int j = dim - 8; j < dim; j++) f[i][j] = true;
        }
        for (int i = dim - 8; i < dim; i++) {
            for (int j = 0; j < 9; j++) f[i][j] = true;
        }
        for (int i = 0; i < dim; i++) {
            f[6][i] = true;
            f[i][6] = true;
        }
        int[] align = ALIGN[version];
        for (int a : align) {
            for (int b : align) {
                if ((a == 6 && b == 6) || (a == 6 && b == align[align.length - 1])
                        || (a == align[align.length - 1] && b == 6)) continue;
                for (int i = -2; i <= 2; i++) {
                    for (int j = -2; j <= 2; j++) {
                        int y = a + i;
                        int x = b + j;
                        if (y >= 0 && y < dim && x >= 0 && x < dim) f[y][x] = true;
                    }
                }
            }
        }
        if (version >= 7) {
            for (int i = 0; i < 6; i++) {
                for (int j = dim - 11; j < dim - 8; j++) {
                    f[i][j] = true;
                    f[j][i] = true;
                }
            }
        }
        return f;
    }

    private static boolean[] readBits(boolean[][] m, int version, int mask) {
        int dim = m.length;
        boolean[][] fn = functionMask(version);
        boolean[] bits = new boolean[dim * dim];
        int n = 0;
        int col = dim - 1;
        boolean up = true;
        while (col > 0) {
            if (col == 6) col--;
            for (int k = 0; k < dim; k++) {
                int r = up ? dim - 1 - k : k;
                for (int d = 0; d < 2; d++) {
                    int c = col - d;
                    if (!fn[r][c]) {
                        boolean v = m[r][c];
                        if (maskFn(mask, r, c)) v = !v;
                        bits[n++] = v;
                    }
                }
            }
            up = !up;
            col -= 2;
        }
        boolean[] out = new boolean[n];
        System.arraycopy(bits, 0, out, 0, n);
        return out;
    }

    private static int[] correctBlock(int[] data, int[] ecc) {
        int n = data.length + ecc.length;
        int[] recv = new int[n];
        System.arraycopy(data, 0, recv, 0, data.length);
        System.arraycopy(ecc, 0, recv, data.length, ecc.length);
        int nsym = ecc.length;
        int[] synd = new int[nsym];
        boolean clean = true;
        for (int i = 0; i < nsym; i++) {
            int s = 0;
            for (int j = 0; j < n; j++) s = mul(s, EXP[i]) ^ recv[j];
            synd[i] = s;
            if (s != 0) clean = false;
        }
        if (clean) return data;

        int[] lambda = new int[nsym + 1];
        int[] prev = new int[nsym + 1];
        lambda[0] = 1;
        prev[0] = 1;
        int l = 0;
        int m = 1;
        int b = 1;
        for (int k = 0; k < nsym; k++) {
            int d = synd[k];
            for (int i = 1; i <= l; i++) d ^= mul(lambda[i], synd[k - i]);
            if (d == 0) {
                m++;
            } else if (2 * l <= k) {
                int[] t = lambda.clone();
                int coef = mul(d, inv(b));
                for (int i = 0; i + m <= nsym; i++) lambda[i + m] ^= mul(coef, prev[i]);
                l = k + 1 - l;
                prev = t;
                b = d;
                m = 1;
            } else {
                int coef = mul(d, inv(b));
                for (int i = 0; i + m <= nsym; i++) lambda[i + m] ^= mul(coef, prev[i]);
                m++;
            }
        }
        if (l * 2 > nsym) return null;

        int[] pos = new int[l];
        int found = 0;
        for (int i = 0; i < n; i++) {
            int x = EXP[(255 - i) % 255];
            int v = 0;
            int xp = 1;
            for (int j = 0; j <= l; j++) {
                v ^= mul(lambda[j], xp);
                xp = mul(xp, x);
            }
            if (v == 0) {
                if (found >= l) return null;
                pos[found++] = i;
            }
        }
        if (found != l) return null;

        int[] omega = new int[l];
        for (int i = 0; i < l; i++) {
            int v = 0;
            for (int j = 0; j <= i; j++) v ^= mul(lambda[j], synd[i - j]);
            omega[i] = v;
        }
        for (int t = 0; t < l; t++) {
            int i = pos[t];
            int xi = EXP[(255 - i) % 255];
            int xinv = inv(xi);
            int num = 0;
            int xp = 1;
            for (int j = 0; j < l; j++) {
                num ^= mul(omega[j], xp);
                xp = mul(xp, xinv);
            }
            int den = 0;
            int xp2 = 1;
            for (int j = 1; j <= l; j++) {
                if ((j & 1) == 1) den ^= mul(lambda[j], xp2);
                xp2 = mul(xp2, xinv);
            }
            if (den == 0) return null;
            int mag = mul(xi, mul(num, inv(den)));
            recv[i] ^= mag;
        }
        for (int i = 0; i < nsym; i++) {
            int s = 0;
            for (int j = 0; j < n; j++) s = mul(s, EXP[i]) ^ recv[j];
            if (s != 0) return null;
        }
        int[] out = new int[data.length];
        System.arraycopy(recv, 0, out, 0, data.length);
        return out;
    }

    private static int inv(int a) {
        if (a == 0) return 0;
        return EXP[255 - LOG[a]];
    }

    private static String parseData(List<Integer> data) {
        boolean[] bits = new boolean[data.size() * 8];
        for (int i = 0; i < data.size(); i++) {
            for (int b = 0; b < 8; b++) bits[i * 8 + b] = ((data.get(i) >> (7 - b)) & 1) == 1;
        }
        int pos = 0;
        StringBuilder sb = new StringBuilder();
        while (pos + 4 <= bits.length) {
            int mode = read(bits, pos, 4);
            pos += 4;
            if (mode == 0) break;
            if (mode == 1) {
                if (pos + 10 > bits.length) break;
                int count = read(bits, pos, 10);
                pos += 10;
                while (count >= 3 && pos + 10 <= bits.length) {
                    int v = read(bits, pos, 10);
                    pos += 10;
                    sb.append((char) (v / 100 + '0'));
                    sb.append((char) (v / 10 % 10 + '0'));
                    sb.append((char) (v % 10 + '0'));
                    count -= 3;
                }
                if (count == 2 && pos + 7 <= bits.length) {
                    int v = read(bits, pos, 7);
                    pos += 7;
                    sb.append((char) (v / 10 + '0'));
                    sb.append((char) (v % 10 + '0'));
                } else if (count == 1 && pos + 4 <= bits.length) {
                    sb.append((char) (read(bits, pos, 4) + '0'));
                    pos += 4;
                }
            } else if (mode == 2) {
                String alnum = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:";
                if (pos + 9 > bits.length) break;
                int count = read(bits, pos, 9);
                pos += 9;
                while (count >= 2 && pos + 11 <= bits.length) {
                    int v = read(bits, pos, 11);
                    pos += 11;
                    sb.append(alnum.charAt(v / 45));
                    sb.append(alnum.charAt(v % 45));
                    count -= 2;
                }
                if (count == 1 && pos + 6 <= bits.length) {
                    sb.append(alnum.charAt(read(bits, pos, 6)));
                    pos += 6;
                }
            } else if (mode == 4) {
                if (pos + 8 > bits.length) break;
                int count = read(bits, pos, 8);
                pos += 8;
                if (pos + count * 8 > bits.length) break;
                byte[] bytes = new byte[count];
                for (int i = 0; i < count; i++) {
                    bytes[i] = (byte) read(bits, pos, 8);
                    pos += 8;
                }
                sb.append(new String(bytes, java.nio.charset.Charset.forName("UTF-8")));
            } else {
                return null;
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private static int read(boolean[] bits, int pos, int n) {
        int v = 0;
        for (int i = 0; i < n; i++) v = (v << 1) | (bits[pos + i] ? 1 : 0);
        return v;
    }
}
