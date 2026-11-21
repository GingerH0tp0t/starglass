package com.starglass.sky;

import java.util.Random;

/** Pictures. */
public class CosmosSprites
{
    public static final int DEEP_SIZE = 128;
    public static final int PLANET_W = 64, PLANET_H = 32;
    public static final int LIMB_SIZE = 32;
    public static final int MOON_SIZE = 128;
    public static final int COMA_SIZE = 64;
    public static final int RING_SIZE = 64;
    public static final int SKY_FACE = 256;

    // Noise

    private static double hash(int x, int y, int s)
    {
        int h = x * 374761393 + y * 668265263 + s * 1274126177;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (double)(h & 0xffffff) / 16777216D;
    }

    private static double noise(double x, double y, int s)
    {
        int xi = (int)Math.floor(x), yi = (int)Math.floor(y);
        double fx = x - xi, fy = y - yi;
        fx = fx * fx * (3D - 2D * fx);
        fy = fy * fy * (3D - 2D * fy);
        double a = hash(xi, yi, s), b = hash(xi + 1, yi, s), c = hash(xi, yi + 1, s), d = hash(xi + 1, yi + 1, s);
        return a + (b - a) * fx + (c - a) * fy + (a - b - c + d) * fx * fy;
    }

    private static double fbm(double x, double y, int s)
    {
        return (noise(x, y, s) + 0.5D * noise(x * 2D, y * 2D, s + 1) + 0.25D * noise(x * 4D, y * 4D, s + 2)
            + 0.125D * noise(x * 8D, y * 8D, s + 3)) / 1.875D;
    }

    /** Wrapping noise. */
    private static double ring(double x, double y, double wrap, int s)
    {
        double t = x / wrap;
        return fbm(x, y, s) * (1D - t) + fbm(x - wrap, y, s) * t;
    }

    /** Sphere noise. */
    private static double sphere(double x, double y, double z, double k, int s)
    {
        return (fbm(x * k + 11D, y * k + 3D, s) + fbm(y * k + 50D, z * k + 7D, s + 17) + fbm(z * k + 90D, x * k + 5D, s + 31)) / 3D;
    }

    private static int argb(double a, double r, double g, double b)
    {
        return clamp(a * 255D) << 24 | clamp(r) << 16 | clamp(g) << 8 | clamp(b);
    }

    private static int clamp(double v)
    {
        return v < 0D ? 0 : (v > 255D ? 255 : (int)v);
    }

    private static double unit(double v)
    {
        return v < 0D ? 0D : (v > 1D ? 1D : v);
    }

    /** Add light. */
    private static void add(int px[], int n, int x, int y, double a, double r, double g, double b)
    {
        if(x < 0 || y < 0 || x >= n || y >= n || a <= 0D)
        {
            return;
        }
        int old = px[x + y * n];
        double oa = (old >>> 24) / 255D;
        double t = a / (a + oa + 1.0E-6D);
        double nr = (old >> 16 & 0xff) * (1D - t) + r * t;
        double ng = (old >> 8 & 0xff) * (1D - t) + g * t;
        double nb = (old & 0xff) * (1D - t) + b * t;
        px[x + y * n] = argb(Math.min(1D, a + oa), nr, ng, nb);
    }

    private static void star(int px[], int n, int x, int y, double a, int rgb)
    {
        add(px, n, x, y, a, rgb >> 16 & 0xff, rgb >> 8 & 0xff, rgb & 0xff);
    }

    // Deep sky

    public static int[] deep(Cosmos.Deep d)
    {
        int n = DEEP_SIZE;
        int px[] = new int[n * n];
        Random r = new Random(d.seed);
        int s = r.nextInt(100000);
        double arms = 2.6D + r.nextDouble() * 2.6D;
        double twist = r.nextBoolean() ? 1D : -1D;
        double phase = r.nextDouble() * 6.28D;
        boolean barred = r.nextInt(3) == 0;
        double hue = r.nextDouble();
        int variant = r.nextInt(3);
        // Shape
        int pickShape = r.nextInt(840), pickPal = r.nextInt(840);
        double stretch = 0.42D + 0.58D * r.nextDouble();
        double rot = r.nextDouble() * Math.PI;
        double ph2 = r.nextDouble() * 6.2832D;
        int pvar = r.nextInt(5), rvar = r.nextInt(3);
        int shapes[] = { BLOB }, pals[] = { 0 };
        switch(d.type)
        {
            case Cosmos.EMISSION:
                shapes = new int[] { BLOB, CRESCENT, PILLARS, STRANDS, WALL, TRIFID, PATCHES, BLOB };
                pals = new int[] { 0, 1, 2, 4, 5, 6, 7, 0 };
                break;
            case Cosmos.REFLECTION:
                shapes = new int[] { BLOB, STRANDS, PATCHES, WALL };
                pals = new int[] { 3, 3, 4, 7 };
                break;
            case Cosmos.NURSERY:
                shapes = new int[] { PATCHES, BLOB, PILLARS, TRIFID };
                pals = new int[] { 0, 4, 1, 6 };
                break;
            case Cosmos.BUBBLE:
                shapes = new int[] { RING, RING, CRESCENT };
                pals = new int[] { 5, 3, 1, 7 };
                break;
            case Cosmos.BIPOLAR:
                shapes = new int[] { HOURGLASS };
                pals = new int[] { 6, 1, 2, 0, 5 };
                stretch = 1D;
                break;
            case Cosmos.REMNANT:
                stretch = 0.75D + 0.25D * stretch;
                break;
            default:
                if(Cosmos.isFar(d.type))
                {
                    stretch = 1D;
                }
                break;
        }
        int shape = shapes[pickShape % shapes.length], pal = pals[pickPal % pals.length];
        boolean cloudy = shape == BLOB || shape == PILLARS || shape == WALL || shape == TRIFID || shape == PATCHES;
        double cosr = Math.cos(rot), sinr = Math.sin(rot);
        double col[] = new double[3];
        boolean edgeOn = d.type == Cosmos.SPIRAL && d.axis < 0.42F;
        double grid = 64D / n;
        for(int y = 0; y < n; y++)
        {
            for(int x = 0; x < n; x++)
            {
                double u = (x + 0.5D) / n * 2D - 1D;
                double yn = (y + 0.5D) / n * 2D - 1D;
                double v = yn / d.axis;
                double rad = Math.sqrt(u * u + v * v);
                double th = Math.atan2(v, u);
                double edge = rad > 0.8D ? Math.max(0D, 1D - (rad - 0.8D) / 0.2D) : 1D;
                double gx = x * grid, gy = y * grid;
                double f = fbm(gx * 0.11D + 7D, gy * 0.11D + 3D, s);
                double g = fbm(gx * 0.23D + 1D, gy * 0.23D + 9D, s + 40);
                double a = 0D, cr = 255D, cg = 255D, cb = 255D;
                // Nebula frame
                double su = u * cosr + yn * sinr, sv = (yn * cosr - u * sinr) / stretch;
                double srad = Math.sqrt(su * su + sv * sv);
                switch(d.type)
                {
                    case Cosmos.SPIRAL:
                    {
                        double core = Math.exp(-rad * rad / 0.018D);
                        double bar = barred ? Math.exp(-(v * v) / 0.004D - (u * u) / 0.09D) * 0.7D : 0D;
                        double disk = Math.exp(-rad / 0.5D);
                        double wind = th * 2D - twist * Math.log(rad + 0.04D) * arms + phase;
                        double arm = Math.pow(0.5D + 0.5D * Math.cos(wind), 2.2D);
                        // Arm dust
                        double dust = Math.pow(0.5D + 0.5D * Math.cos(wind + twist * 1.15D), 4D) * unit(1.2D - rad * 1.3D);
                        double light = disk * (0.16D + 0.95D * arm) * (0.55D + 0.9D * g) * (1D - 0.6D * dust);
                        a = (core * 1.2D + bar + light) * edge;
                        double warm = Math.min(1D, (core + bar) / (core + bar + light + 1.0E-6D));
                        cr = 140D + 115D * warm;
                        cg = 180D + 45D * warm;
                        cb = 255D - 105D * warm;
                        if(arm > 0.7D && g > 0.62D && rad > 0.15D)
                        {
                            // Knots
                            cr = 255D;
                            cg = 80D;
                            cb = 150D;
                            a *= 1.6D;
                        }
                        if(edgeOn)
                        {
                            // Edge-on lane
                            double lane = Math.exp(-Math.pow((yn - 0.012D + (f - 0.5D) * 0.03D) / 0.028D, 2D)) * unit(1.15D - Math.abs(u) * 1.35D);
                            a *= 1D - 0.85D * lane;
                        }
                        break;
                    }
                    case Cosmos.ELLIPTICAL:
                        a = Math.exp(-Math.pow(rad / 0.11D, 0.55D)) * 2.4D * edge;
                        cr = 255D;
                        cg = 214D;
                        cb = 150D;
                        break;
                    case Cosmos.IRREGULAR:
                        a = Math.max(0D, f - 0.36D) * 2.6D * Math.exp(-rad * rad / 0.4D) * edge;
                        cr = 150D;
                        cg = 195D;
                        cb = 255D;
                        if(g > 0.66D)
                        {
                            cr = 255D;
                            cg = 90D;
                            cb = 160D;
                        }
                        break;
                    case Cosmos.EMISSION:
                    case Cosmos.REFLECTION:
                    case Cosmos.NURSERY:
                    case Cosmos.BUBBLE:
                    case Cosmos.BIPOLAR:
                    {
                        double env = nebulaShape(shape, su, sv, f, g, ph2);
                        double ridge = 1D - Math.abs(f * 2D - 1D);
                        double body = cloudy ? Math.max(0D, ridge * 0.9D + g * 0.7D - 0.62D) * 2.6D
                            : 0.4D + 1.1D * unit(ridge * 0.6D + g * 0.7D - 0.25D);
                        a = env * body * edge;
                        if(d.type == Cosmos.EMISSION || d.type == Cosmos.NURSERY)
                        {
                            // Dust lanes
                            double lane = noise(su * 3.4D + 20D, sv * 7.5D + 3D, s + 9);
                            double floor = d.type == Cosmos.NURSERY ? 0.4D : 0.14D;
                            a *= floor + (1D - floor) * unit((lane - 0.24D) / 0.14D);
                            a *= 1D + 0.45D * unit(1D - Math.abs(lane - 0.41D) / 0.05D);
                        }
                        double hot = Math.exp(-srad * srad / 0.1D);
                        nebulaColour(d.type == Cosmos.NURSERY && g > 0.56D ? 3 : pal, hot, srad, f, g, hue, col);
                        cr = col[0];
                        cg = col[1];
                        cb = col[2];
                        break;
                    }
                    case Cosmos.DARK:
                    {
                        // Dark cloud
                        double lx = Math.cos(ph2) * 2.4D, ly = Math.sin(ph2) * 2.4D;
                        double reach = unit(1.15D - srad * 1.25D);
                        double m = unit((f * 0.7D + g * 0.3D - 0.4D) * 5D) * reach;
                        double f2 = fbm((gx + lx) * 0.11D + 7D, (gy + ly) * 0.11D + 3D, s);
                        double g2 = fbm((gx + lx) * 0.23D + 1D, (gy + ly) * 0.23D + 9D, s + 40);
                        double toward = unit((f2 * 0.7D + g2 * 0.3D - 0.4D) * 5D) * reach;
                        double rim = unit((m - toward) * 2.5D);
                        double behind = 0.62D * Math.exp(-srad * srad / 0.5D) * (0.7D + 0.6D * g);
                        a = (behind * (1D - 0.97D * m) + rim * 1.1D) * edge;
                        nebulaColour(pickPal % 2 == 0 ? 0 : 3, 0D, srad, f, g, hue, col);
                        cr = col[0] + (255D - col[0]) * rim;
                        cg = col[1] + (214D - col[1]) * rim;
                        cb = col[2] + (150D - col[2]) * rim;
                        break;
                    }
                    case Cosmos.PLANETARY:
                    {
                        double squash = Math.sqrt(u * u + v * v * (1D + hue * 0.8D));
                        if(pvar == 0)
                        {
                            // Ring
                            double ring = Math.exp(-Math.pow((squash - 0.58D) / 0.13D, 2D));
                            double fill = squash < 0.58D ? 0.3D : 0D;
                            double halo = Math.exp(-Math.pow((squash - 0.8D) / 0.12D, 2D)) * 0.22D;
                            a = (ring * (0.7D + 0.5D * g) + fill + halo) * edge;
                            cr = 40D + 215D * unit(halo * 4D);
                            cg = 255D - 70D * hue;
                            cb = 180D + 75D * hue;
                        } else if(pvar == 1)
                        {
                            // Eye
                            double inner = Math.exp(-Math.pow((squash - 0.3D) / 0.09D, 2D));
                            double outer = Math.exp(-Math.pow((squash - 0.7D) / 0.11D, 2D)) * (0.45D + 0.5D * g);
                            double fill = squash < 0.3D ? 0.35D : (squash < 0.7D ? 0.14D : 0D);
                            a = (inner + outer + fill) * edge;
                            double red = outer / (inner + outer + fill + 1.0E-6D);
                            cr = 70D + 185D * red;
                            cg = 235D - 150D * red;
                            cb = 255D - 150D * red;
                        } else if(pvar == 3)
                        {
                            // Handles
                            double inner = Math.exp(-Math.pow((squash - 0.34D) / 0.08D, 2D));
                            double outer = Math.exp(-Math.pow((squash - 0.62D) / 0.05D, 2D)) * 0.45D;
                            double knots = Math.exp(-Math.pow(Math.abs(u) - 0.78D, 2D) / 0.006D - v * v / 0.006D) * 1.2D;
                            a = (inner + outer + knots + (squash < 0.34D ? 0.3D : 0D)) * edge;
                            cr = 120D + 135D * unit(knots);
                            cg = 255D - 60D * hue;
                            cb = 150D + 60D * hue;
                        } else if(pvar == 4)
                        {
                            // Rectangle
                            double au = Math.abs(u), av = Math.abs(v);
                            double cones = Math.exp(-Math.pow((au - av) / 0.09D, 2D)) * Math.exp(-rad / 0.45D);
                            double box = au < 0.5D && av < 0.5D ? 0.18D * (1D - Math.max(au, av) * 1.6D) : 0D;
                            a = (cones * 1.3D + Math.max(0D, box) + Math.exp(-rad * rad / 0.01D)) * edge;
                            cr = 255D;
                            cg = 150D + 70D * unit(1D - rad * 2D);
                            cb = 70D + 60D * hue;
                        } else
                        {
                            // Butterfly
                            double au = Math.abs(u);
                            double lobe = Math.exp(-Math.pow((au - 0.42D) / 0.3D, 2D) - Math.pow(v / (0.07D + 0.5D * au), 2D));
                            double waist = Math.exp(-(u * u) / 0.004D - (v * v) / 0.05D) * 0.6D;
                            a = (lobe * (0.6D + 0.8D * g) + waist) * edge;
                            double out = unit(au * 1.6D - 0.2D);
                            cr = 90D + 165D * out;
                            cg = 240D - 130D * out;
                            cb = 200D - 130D * out;
                        }
                        break;
                    }
                    case Cosmos.REMNANT:
                    {
                        double shell = Math.exp(-Math.pow((srad - 0.68D) / 0.17D, 2D)) + 0.5D * Math.exp(-Math.pow((srad - 0.36D) / 0.12D, 2D));
                        if(rvar == 1)
                        {
                            // Arc
                            shell *= Math.pow(0.5D + 0.5D * Math.cos(Math.atan2(sv, su) - ph2), 1.3D) * 1.7D;
                        } else if(rvar == 2)
                        {
                            // Strands
                            shell = nebulaShape(STRANDS, su, sv, f, g, ph2) * 1.2D + shell * 0.15D;
                        }
                        double thread = Math.pow(1D - Math.abs(g * 2D - 1D), 5D);
                        a = shell * thread * 2.6D * edge;
                        boolean other = f > 0.52D;
                        if(pickPal % 2 == 0)
                        {
                            cr = other ? 60D : 255D;
                            cg = other ? 245D : 70D;
                            cb = other ? 225D : 70D;
                        } else
                        {
                            cr = other ? 110D : 255D;
                            cg = other ? 160D : 200D;
                            cb = other ? 255D : 90D;
                        }
                        break;
                    }
                    case Cosmos.QUASAR:
                    {
                        // Quasar
                        double host = Math.exp(-srad / 0.2D) * 0.3D;
                        double core = Math.exp(-srad * srad / 0.004D) * 3D;
                        double spikes = (Math.exp(-u * u / 0.0005D) * Math.exp(-Math.abs(yn) / 0.22D)
                            + Math.exp(-yn * yn / 0.0005D) * Math.exp(-Math.abs(u) / 0.22D)) * 0.55D;
                        double jet = su > 0.06D ? Math.exp(-sv * sv / 0.0022D) * Math.exp(-su / 0.55D)
                            * (0.45D + 0.55D * Math.cos(su * 24D + ph2)) * 1.3D : 0D;
                        jet = Math.max(0D, jet);
                        a = (host + core + spikes + jet) * edge;
                        double blue = jet / (host + core + spikes + jet + 1.0E-6D);
                        double white = unit(core + spikes);
                        cr = 255D * white + (1D - white) * (230D - 90D * blue);
                        cg = 255D * white + (1D - white) * (190D - 20D * blue);
                        cb = 255D * white + (1D - white) * (140D + 115D * blue);
                        break;
                    }
                    case Cosmos.RADIO_GALAXY:
                    {
                        // Radio galaxy
                        double body = Math.exp(-Math.pow(rad / 0.13D, 0.55D)) * 2.2D;
                        double lane = Math.exp(-Math.pow((sv + (f - 0.5D) * 0.05D) / 0.035D, 2D)) * unit(1D - Math.abs(su) * 2D);
                        double jet = Math.exp(-su * su / 0.0012D) * Math.exp(-Math.abs(sv) / 0.3D) * 0.55D * unit(Math.abs(sv) * 12D);
                        a = (body * (1D - 0.82D * lane) + jet) * edge;
                        double blue = jet / (body + jet + 1.0E-6D);
                        cr = 255D - 110D * blue - 60D * lane;
                        cg = 214D - 30D * blue - 90D * lane;
                        cb = 150D + 105D * blue - 80D * lane;
                        break;
                    }
                    case Cosmos.COLLISION:
                    {
                        // Collision
                        double light = 0D, warm = 0D;
                        for(int k = 0; k < 2; k++)
                        {
                            double ox = k == 0 ? -0.3D : 0.3D, oy = k == 0 ? -0.08D : 0.12D;
                            double du = su - ox, dv = (sv - oy) / (k == 0 ? 0.62D : 0.85D);
                            double rr = Math.sqrt(du * du + dv * dv), tt = Math.atan2(dv, du);
                            double core = Math.exp(-rr * rr / 0.004D) * 1.4D;
                            double arm = Math.pow(0.5D + 0.5D * Math.cos(tt * 2D - (k == 0 ? twist : -twist) * Math.log(rr + 0.03D) * 3.4D + phase), 2D);
                            double disk = Math.exp(-rr / 0.16D) * (0.2D + 0.9D * arm) * (0.6D + 0.8D * g);
                            light += core + disk;
                            warm += core;
                        }
                        double ta = su < -0.3D ? Math.exp(-Math.pow((sv - (-0.08D - 0.9D * (su + 0.3D) * (su + 0.3D))) / 0.05D, 2D))
                            * unit(1.25D + su) * 0.5D : 0D;
                        double tb = su > 0.3D ? Math.exp(-Math.pow((sv - (0.12D + 0.9D * (su - 0.3D) * (su - 0.3D))) / 0.05D, 2D))
                            * unit(1.25D - su) * 0.5D : 0D;
                        double bridge = Math.abs(su) < 0.3D ? Math.exp(-Math.pow((sv - (0.02D + su * 0.33D)) / 0.06D, 2D)) * 0.4D : 0D;
                        a = (light + (ta + tb + bridge) * (0.5D + g)) * edge;
                        double w = unit(warm / (light + 1.0E-6D));
                        cr = 150D + 105D * w;
                        cg = 190D + 30D * w;
                        cb = 255D - 100D * w;
                        if(bridge > 0.12D && g > 0.6D)
                        {
                            cr = 255D;
                            cg = 90D;
                            cb = 160D;
                            a *= 1.5D;
                        }
                        break;
                    }
                    case Cosmos.LENS:
                    {
                        // Lens
                        double front = Math.exp(-Math.pow(rad / 0.07D, 0.6D)) * 1.6D + Math.exp(-rad / 0.45D) * 0.1D;
                        double arcs = 0D;
                        for(int k = 0; k < 3; k++)
                        {
                            double at = ph2 + k * 2.3D + (k == 2 ? 0.5D : 0D);
                            double span = Math.pow(Math.max(0D, Math.cos(th - at)), k == 0 ? 5D : 14D);
                            double ring = k == 2 ? 0.36D : 0.56D;
                            arcs += Math.exp(-Math.pow((rad - ring) / 0.035D, 2D)) * span * (k == 0 ? 1.3D : 0.8D);
                        }
                        a = (front + arcs) * edge;
                        double blue = arcs / (front + arcs + 1.0E-6D);
                        cr = 255D - 135D * blue;
                        cg = 214D - 34D * blue;
                        cb = 150D + 105D * blue;
                        break;
                    }
                    case Cosmos.PULSAR_NEBULA:
                    {
                        // Wind nebula
                        double glow = Math.exp(-srad * srad / 0.1D) * (0.45D + 0.6D * f);
                        double thread = Math.pow(1D - Math.abs(g * 2D - 1D), 4D) * Math.exp(-srad * srad / 0.34D) * 1.9D;
                        a = (glow + thread) * edge;
                        double red = thread / (glow + thread + 1.0E-6D);
                        cr = 170D + 85D * red;
                        cg = 205D - 95D * red;
                        cb = 255D - 185D * red;
                        break;
                    }
                    case Cosmos.BLACK_HOLE:
                    {
                        // Black hole
                        double q = Math.sqrt(su * su + sv * sv / 0.1D);
                        double disc = Math.exp(-Math.pow((q - 0.44D) / 0.17D, 2D)) * (1D + 0.75D * su / 0.5D) * (0.7D + 0.5D * g);
                        double ringOfLight = Math.exp(-Math.pow((srad - 0.2D) / 0.028D, 2D)) * 1.1D;
                        double jet = Math.abs(sv) > 0.16D ? Math.exp(-su * su / 0.0016D) * Math.exp(-Math.abs(sv) / 0.42D) * 0.9D : 0D;
                        double hole = unit((srad - 0.1D) / 0.07D);
                        a = (Math.max(0D, disc) * hole + ringOfLight + jet) * edge;
                        double blue = jet / (Math.max(0D, disc) + ringOfLight + jet + 1.0E-6D);
                        double heat = unit(1.3D - q * 1.6D);
                        cr = 255D - 110D * blue;
                        cg = 120D + 110D * heat + 40D * blue;
                        cb = 40D + 120D * heat * heat + 215D * blue;
                        break;
                    }
                    case Cosmos.GALAXY_CLUSTER:
                        a = (Math.exp(-rad / 0.5D) * 0.1D + Math.exp(-Math.pow(rad / 0.05D, 0.6D)) * 1.5D) * edge;
                        cr = 255D;
                        cg = 214D;
                        cb = 150D;
                        break;
                    default:
                        break;
                }
                if(a > 0.004D)
                {
                    px[x + y * n] = argb(Math.min(1D, a), cr, cg, cb);
                }
            }
        }
        int half = n / 2;
        if(d.type == Cosmos.SPIRAL || d.type == Cosmos.IRREGULAR)
        {
            // Bright stars
            for(int i = 0; i < 150; i++)
            {
                int x = (int)(half + r.nextGaussian() * n * 0.2D), y = (int)(half + r.nextGaussian() * n * 0.2D * d.axis);
                if(x < 0 || y < 0 || x >= n || y >= n || (px[x + y * n] >>> 24) < 60)
                {
                    continue;
                }
                star(px, n, x, y, 0.35D + r.nextDouble() * 0.4D, r.nextInt(4) == 0 ? 0xffb080 : 0xc0d8ff);
            }
        }
        if(d.type == Cosmos.ELLIPTICAL)
        {
            // Globulars
            for(int i = 0; i < 34; i++)
            {
                double rad = 0.2D + r.nextDouble() * 0.6D, th = r.nextDouble() * 6.2832D;
                star(px, n, (int)(half + Math.cos(th) * rad * half), (int)(half + Math.sin(th) * rad * half * d.axis),
                    0.2D + r.nextDouble() * 0.3D, 0xffe6b0);
            }
        }
        if(d.type == Cosmos.PLANETARY || d.type == Cosmos.BUBBLE || d.type == Cosmos.BIPOLAR)
        {
            // Central star
            star(px, n, half, half, 1D, 0xebf5ff);
            if(d.type != Cosmos.PLANETARY)
            {
                star(px, n, half + 1, half, 0.6D, 0xc8dcff);
                star(px, n, half, half + 1, 0.6D, 0xc8dcff);
            }
        }
        if(d.type == Cosmos.NURSERY)
        {
            // Young stars
            for(int i = 0; i < 110; i++)
            {
                int x = (int)(half + r.nextGaussian() * n * 0.2D), y = (int)(half + r.nextGaussian() * n * 0.2D);
                if(x < 0 || y < 0 || x >= n || y >= n || (px[x + y * n] >>> 24) < 30)
                {
                    continue;
                }
                star(px, n, x, y, 0.5D + r.nextDouble() * 0.5D, i % 5 == 0 ? 0xffffff : 0xc8dcff);
            }
        }
        if(d.type == Cosmos.REMNANT && variant != 0)
        {
            // Remnant star
            star(px, n, half, half, 1D, 0xb0d0ff);
        }
        if(d.type == Cosmos.EMISSION || d.type == Cosmos.REFLECTION)
        {
            // Lighting stars
            for(int i = 0; i < 16; i++)
            {
                int x = (int)(half + r.nextGaussian() * n * 0.16D), y = (int)(half + r.nextGaussian() * n * 0.16D);
                star(px, n, x, y, 0.6D + r.nextDouble() * 0.4D, i < 4 ? 0xffffff : 0xc8dcff);
                if(i < 4)
                {
                    star(px, n, x + 1, y, 0.45D, 0xc8dcff);
                    star(px, n, x, y + 1, 0.45D, 0xc8dcff);
                }
            }
        }
        if(d.type == Cosmos.OPEN_CLUSTER)
        {
            int count = 90 + r.nextInt(130);
            double spread = 0.14D + r.nextDouble() * 0.1D;
            for(int i = 0; i < count; i++)
            {
                int x = (int)(half + r.nextGaussian() * n * spread), y = (int)(half + r.nextGaussian() * n * spread);
                int c = Cosmos.SPECTRAL_RGB[r.nextInt(6) == 0 ? 5 + r.nextInt(2) : r.nextInt(4)];
                double a = 0.5D + r.nextDouble() * 0.5D;
                star(px, n, x, y, a, c);
                if(r.nextInt(5) == 0)
                {
                    star(px, n, x + 1, y, a, c);
                    star(px, n, x, y + 1, a, c);
                    star(px, n, x + 1, y + 1, a, c);
                }
            }
        }
        if(d.type == Cosmos.PULSAR_NEBULA)
        {
            // Pulsar
            star(px, n, half, half, 1D, 0xffffff);
        }
        if(d.type == Cosmos.LENS || d.type == Cosmos.GALAXY_CLUSTER)
        {
            // Cluster galaxies
            int count = d.type == Cosmos.LENS ? 16 : 80;
            for(int i = 0; i < count; i++)
            {
                double rad = Math.pow(r.nextDouble(), 1.6D) * (d.type == Cosmos.LENS ? 0.34D : 0.85D), th = r.nextDouble() * 6.2832D;
                double gx = half + Math.cos(th) * rad * half, gy = half + Math.sin(th) * rad * half;
                double len = 1D + r.nextDouble() * (d.type == Cosmos.LENS ? 1.5D : 3D), tilt = r.nextDouble() * Math.PI;
                boolean blue = rad > 0.3D && r.nextInt(3) == 0;
                double lum = 0.35D + r.nextDouble() * 0.55D;
                for(int k = -(int)len; k <= (int)len; k++)
                {
                    double fall = 1D - Math.abs(k) / (len + 1D);
                    add(px, n, (int)(gx + Math.cos(tilt) * k), (int)(gy + Math.sin(tilt) * k), lum * fall,
                        blue ? 170D : 255D, blue ? 200D : 220D, blue ? 255D : 160D);
                }
            }
        }
        if(d.type == Cosmos.GLOBULAR)
        {
            for(int y = 0; y < n; y++)
            {
                for(int x = 0; x < n; x++)
                {
                    double u = (x + 0.5D) / n * 2D - 1D, v = (y + 0.5D) / n * 2D - 1D;
                    double a = Math.exp(-Math.sqrt(u * u + v * v) / 0.15D) * 0.55D;
                    add(px, n, x, y, a, 255D, 222D, 160D);
                }
            }
            for(int i = 0; i < 2300; i++)
            {
                double rad = Math.pow(r.nextDouble(), 2.3D) * 0.95D, th = r.nextDouble() * 6.2832D;
                int x = (int)(half + Math.cos(th) * rad * half), y = (int)(half + Math.sin(th) * rad * half);
                int kind = r.nextInt(12);
                star(px, n, x, y, 0.3D + r.nextDouble() * 0.6D, kind < 2 ? 0xffa060 : (kind == 2 ? 0xa8c8ff : 0xffeecd));
            }
        }
        return px;
    }

    // Radio

    /** Radio palette. */
    private static final int HEAT[] = { 0x2a1a66, 0x6e1e96, 0xc83278, 0xff6e28, 0xffc83c, 0xffffdc };

    /** Radio map. */
    public static int[] radio(Cosmos.Deep d)
    {
        int n = DEEP_SIZE;
        int px[] = new int[n * n];
        Random r = new Random(d.seed ^ 0x52414449L);
        int s = r.nextInt(100000);
        double rot = r.nextDouble() * Math.PI, ph = r.nextDouble() * 6.2832D;
        double cosr = Math.cos(rot), sinr = Math.sin(rot);
        boolean oneSided = d.type == Cosmos.QUASAR;
        double grid = 64D / n;
        for(int y = 0; y < n; y++)
        {
            for(int x = 0; x < n; x++)
            {
                double u = (x + 0.5D) / n * 2D - 1D, v = (y + 0.5D) / n * 2D - 1D;
                double su = u * cosr + v * sinr, sv = v * cosr - u * sinr;
                double rad = Math.sqrt(u * u + v * v);
                double f = fbm(x * grid * 0.11D + 7D, y * grid * 0.11D + 3D, s);
                double g = fbm(x * grid * 0.23D + 1D, y * grid * 0.23D + 9D, s + 40);
                double I;
                switch(d.type)
                {
                    case Cosmos.QUASAR:
                    case Cosmos.RADIO_GALAXY:
                    case Cosmos.BLACK_HOLE:
                    case Cosmos.ELLIPTICAL:
                    {
                        double core = Math.exp(-rad * rad / 0.003D);
                        double jets = Math.exp(-sv * sv / 0.0014D) * (Math.abs(su) < 0.66D ? 0.42D : 0D);
                        double lobeA = Math.exp(-((su - 0.6D) * (su - 0.6D) + sv * sv) / 0.05D) * (0.45D + 0.7D * f)
                            + Math.exp(-((su - 0.78D) * (su - 0.78D) + sv * sv) / 0.004D);
                        double lobeB = Math.exp(-((su + 0.6D) * (su + 0.6D) + sv * sv) / 0.05D) * (0.45D + 0.7D * g)
                            + Math.exp(-((su + 0.78D) * (su + 0.78D) + sv * sv) / 0.004D);
                        if(oneSided)
                        {
                            lobeB *= 0.3D;
                            jets *= su > 0D ? 1.3D : 0.3D;
                        }
                        I = core + jets + lobeA + lobeB;
                        break;
                    }
                    case Cosmos.REMNANT:
                    case Cosmos.BUBBLE:
                    case Cosmos.PLANETARY:
                        I = Math.exp(-Math.pow((rad - 0.62D) / 0.13D, 2D)) * (0.35D + 1.1D * g) + (rad < 0.62D ? 0.12D : 0D);
                        break;
                    case Cosmos.PULSAR_NEBULA:
                        I = Math.exp(-rad * rad / 0.2D) * (0.5D + 0.6D * f) + Math.exp(-rad * rad / 0.002D);
                        break;
                    case Cosmos.PULSAR:
                        I = Math.exp(-rad * rad / 0.012D) * 1.2D;
                        break;
                    case Cosmos.CORE:
                    {
                        // Galactic core
                        double point = Math.exp(-rad * rad / 0.0015D) * 1.4D;
                        double th = Math.atan2(sv, su);
                        double mini = Math.exp(-rad / 0.12D) * Math.pow(0.5D + 0.5D * Math.cos(th * 3D - Math.log(rad + 0.02D) * 4D + ph), 2D);
                        double plane = Math.exp(-(sv * sv / 0.02D + su * su / 0.55D)) * (0.3D + 0.5D * f);
                        double threads = Math.exp(-Math.pow((su - 0.3D + (g - 0.5D) * 0.05D) / 0.03D, 2D)) * Math.exp(-sv * sv / 0.12D) * 0.6D
                            + Math.exp(-Math.pow((su - 0.42D) / 0.02D, 2D)) * Math.exp(-sv * sv / 0.07D) * 0.4D;
                        I = point + mini + plane + threads;
                        break;
                    }
                    case Cosmos.GALAXY_CLUSTER:
                        I = Math.exp(-rad * rad / 0.3D) * (0.4D + 0.5D * f);
                        break;
                    case Cosmos.SPIRAL:
                    case Cosmos.IRREGULAR:
                    case Cosmos.COLLISION:
                        I = Math.exp(-rad / 0.3D) * (0.4D + 0.9D * f) + Math.exp(-rad * rad / 0.006D) * 0.6D;
                        break;
                    default:
                        // Gas
                        I = Math.max(0D, f * 0.7D + g * 0.5D - 0.47D) * 3.2D * Math.exp(-rad * rad / 0.45D);
                        break;
                }
                I *= rad > 0.85D ? Math.max(0D, 1D - (rad - 0.85D) / 0.15D) : 1D;
                if(I < 0.07D)
                {
                    continue;
                }
                // Contour steps
                int level = (int)(Math.log(I / 0.07D) / Math.log(1.75D));
                level = level > 5 ? 5 : level;
                px[x + y * n] = (clamp(70D + level * 37D) << 24) | HEAT[level];
            }
        }
        if(d.type == Cosmos.GALAXY_CLUSTER)
        {
            // Radio galaxies
            for(int i = 0; i < 9; i++)
            {
                double rad = r.nextDouble() * 0.6D, th = r.nextDouble() * 6.2832D;
                int cx = (int)(n / 2 + Math.cos(th) * rad * n / 2), cy = (int)(n / 2 + Math.sin(th) * rad * n / 2);
                for(int k = 0; k < 4; k++)
                {
                    int xx = cx + (k & 1), yy = cy + (k >> 1);
                    if(xx >= 0 && yy >= 0 && xx < n && yy < n)
                    {
                        px[xx + yy * n] = 0xff000000 | HEAT[4];
                    }
                }
            }
        }
        return px;
    }

    // World below

    public static final int EARTH_SIZE = 256;

    /** Tiling noise. */
    private static double tile(double x, double y, int period, int s)
    {
        int xi = (int)Math.floor(x), yi = (int)Math.floor(y);
        double fx = x - xi, fy = y - yi;
        fx = fx * fx * (3D - 2D * fx);
        fy = fy * fy * (3D - 2D * fy);
        int x0 = ((xi % period) + period) % period, y0 = ((yi % period) + period) % period;
        int x1 = (x0 + 1) % period, y1 = (y0 + 1) % period;
        double a = hash(x0, y0, s), b = hash(x1, y0, s), c = hash(x0, y1, s), e = hash(x1, y1, s);
        return a + (b - a) * fx + (c - a) * fy + (a - b - c + e) * fx * fy;
    }

    private static double tiles(double u, double v, int base, int s)
    {
        double sum = 0D, amp = 1D, norm = 0D;
        for(int o = 0; o < 5; o++)
        {
            int period = base << o;
            sum += amp * tile(u * period, v * period, period, s + o * 13);
            norm += amp;
            amp *= 0.5D;
        }
        return sum / norm;
    }

    /** World from above. */
    public static int[] earth(long seed)
    {
        int n = EARTH_SIZE;
        int px[] = new int[n * n];
        int s = (int)(seed ^ seed >>> 32) & 0xffff;
        for(int y = 0; y < n; y++)
        {
            for(int x = 0; x < n; x++)
            {
                double u = (x + 0.5D) / n, v = (y + 0.5D) / n;
                double h = tiles(u, v, 6, s);
                double wet = tiles(u, v, 7, s + 300);
                double cloud = tiles(u + 0.37D, v + 0.11D, 9, s + 700);
                double cr, cg, cb;
                if(h < 0.5D)
                {
                    double depth = unit((0.5D - h) / 0.16D);
                    cr = 44D - 28D * depth;
                    cg = 104D - 62D * depth;
                    cb = 176D - 70D * depth;
                } else if(h > 0.72D)
                {
                    cr = 236D;
                    cg = 240D;
                    cb = 246D;
                } else if(h > 0.64D)
                {
                    cr = 112D;
                    cg = 104D;
                    cb = 94D;
                } else
                {
                    double dry = unit((0.56D - wet) / 0.14D);
                    cr = 62D + 96D * dry;
                    cg = 124D + 14D * dry;
                    cb = 54D + 34D * dry;
                    if(h < 0.512D)
                    {
                        // Beach
                        cr = 214D;
                        cg = 204D;
                        cb = 150D;
                    }
                }
                double white = unit((cloud - 0.6D) / 0.1D) * 0.9D;
                cr += (250D - cr) * white;
                cg += (250D - cg) * white;
                cb += (252D - cb) * white;
                px[x + y * n] = 0xff000000 | clamp(cr) << 16 | clamp(cg) << 8 | clamp(cb);
            }
        }
        return px;
    }

    // Nebula shapes

    static final int BLOB = 0, CRESCENT = 1, PILLARS = 2, HOURGLASS = 3, STRANDS = 4, WALL = 5, TRIFID = 6, RING = 7, PATCHES = 8;

    /** Nebula shape. */
    static double nebulaShape(int shape, double u, double v, double f, double g, double ph)
    {
        double rad = Math.sqrt(u * u + v * v), th = Math.atan2(v, u);
        switch(shape)
        {
            case CRESCENT:
            {
                // Crescent
                double ring = Math.exp(-Math.pow((rad - 0.55D + (f - 0.5D) * 0.2D) / 0.17D, 2D));
                return ring * Math.pow(0.5D + 0.5D * Math.cos(th - ph), 1.6D) * 1.5D + 0.1D * Math.exp(-rad * rad / 0.3D);
            }
            case PILLARS:
            {
                // Pillars
                double curtain = unit(1.1D - Math.abs(u)) * unit(0.9D - v * 0.6D) * Math.exp(-rad * rad / 0.9D) * 1.3D;
                for(int k = 0; k < 3; k++)
                {
                    double top = -0.3D + 0.22D * Math.sin(ph * (k + 1.3D));
                    double x = -0.36D + 0.36D * k + 0.05D * Math.sin(v * 6D + ph + k);
                    double w = 0.06D + 0.07D * unit(v - top);
                    double off = Math.abs(u - x);
                    if(v > top + off * 0.8D)
                    {
                        if(off < w)
                        {
                            return curtain * 0.04D;
                        }
                        if(off < w + 0.04D)
                        {
                            return curtain * 2D;
                        }
                    }
                }
                return curtain;
            }
            case HOURGLASS:
            {
                double au = Math.abs(u);
                double lobe = Math.exp(-Math.pow((au - 0.45D) / 0.3D, 2D) - Math.pow(v / (0.08D + 0.55D * au), 2D));
                return lobe * 1.2D + Math.exp(-(u * u) / 0.004D - (v * v) / 0.03D) * 0.5D;
            }
            case STRANDS:
            {
                // Strands
                double v0 = 0.22D * Math.sin(u * 3.2D + ph) + (f - 0.5D) * 0.25D;
                double v1 = -0.3D + 0.18D * Math.sin(u * 2.1D + ph * 1.7D) + (g - 0.5D) * 0.2D;
                return (Math.exp(-Math.pow((v - v0) / 0.08D, 2D)) + 0.6D * Math.exp(-Math.pow((v - v1) / 0.06D, 2D)))
                    * unit(1.2D - Math.abs(u) * 1.15D) * 1.3D;
            }
            case WALL:
            {
                // Wall
                double band = Math.exp(-Math.pow((v - 0.25D * u - 0.15D) / 0.28D, 2D)) * unit(1.15D - Math.abs(u));
                double du = u - 0.05D * Math.cos(ph), dv = v - 0.02D;
                boolean head = du * du / 0.035D + (dv + 0.12D) * (dv + 0.12D) / 0.02D < 1D;
                boolean neck = Math.abs(du - 0.03D) < 0.07D && dv > -0.1D && dv < 0.45D;
                return band * (head || neck ? 0.04D : 1.25D);
            }
            case TRIFID:
            {
                // Trifid
                double base = Math.exp(-rad * rad / 0.42D) * 1.2D;
                for(int k = 0; k < 3; k++)
                {
                    double ang = ph + k * 2.094D + (g - 0.5D) * 0.5D;
                    if(Math.cos(th - ang) > 0D && Math.abs(Math.sin(th - ang)) * rad < 0.035D + 0.03D * rad)
                    {
                        base *= 0.08D;
                    }
                }
                return base;
            }
            case RING:
            {
                // Ring
                double rr = rad * (1D + 0.3D * (f - 0.5D));
                return Math.exp(-Math.pow((rr - 0.62D) / 0.065D, 2D)) * 1.4D + (rr < 0.62D ? 0.16D : 0D);
            }
            case PATCHES:
                // Patches
                return unit((f - 0.42D) * 3.2D) * unit(1.25D - rad * 1.1D) * 1.2D;
            default:
                return Math.exp(-rad * rad / 0.5D);
        }
    }

    /** Nebula colour. */
    static void nebulaColour(int pal, double hot, double rad, double f, double g, double hue, double out[])
    {
        switch(pal)
        {
            case 1:
            {
                // Oxygen and hydrogen
                double mixed = unit(hot * 1.5D + (g - 0.5D) * 0.8D);
                out[0] = 255D - 190D * mixed;
                out[1] = 50D + 205D * mixed;
                out[2] = 90D + 130D * mixed;
                break;
            }
            case 2:
            {
                // Sulphur and violet
                boolean violet = f > 0.56D;
                out[0] = violet ? 200D : 255D;
                out[1] = violet ? 80D : 170D + 60D * hot;
                out[2] = violet ? 255D : 50D + 60D * hot;
                break;
            }
            case 3:
                // Reflection blue
                out[0] = 70D + 50D * hue;
                out[1] = 140D + 40D * hue;
                out[2] = 255D;
                break;
            case 4:
            {
                // Magenta to blue
                double m = unit(g * 1.6D - 0.3D);
                out[0] = 255D - 150D * m;
                out[1] = 60D + 90D * m;
                out[2] = 150D + 105D * m;
                break;
            }
            case 5:
                // Oxygen
                out[0] = 60D + 80D * hot;
                out[1] = 255D - 40D * hue;
                out[2] = 150D + 80D * hot;
                break;
            case 6:
                // Amber
                out[0] = 255D;
                out[1] = 150D + 70D * hot;
                out[2] = 60D + 90D * hot * hue;
                break;
            case 7:
            {
                // Pink to blue
                double m = unit(rad * 1.3D);
                out[0] = 255D - 120D * m;
                out[1] = 200D - 60D * m;
                out[2] = 220D + 35D * m;
                break;
            }
            default:
                // Hydrogen
                out[0] = 255D;
                out[1] = 40D + 170D * hot + 30D * hue;
                out[2] = 85D + 140D * hot + 50D * hue;
                break;
        }
    }

    // Planets

    /** Planet map. */
    public static int[] planet(Cosmos.Planet p)
    {
        int w = PLANET_W, h = PLANET_H;
        int px[] = new int[w * h];
        Random r = new Random(p.seed);
        int s = r.nextInt(100000);
        double tint = r.nextDouble();
        int spotX = r.nextInt(w), spotY = 12 + r.nextInt(10);
        int spot2X = r.nextInt(w), spot2Y = 6 + r.nextInt(6);
        double bands[] = new double[h];
        for(int y = 0; y < h; y++)
        {
            bands[y] = noise(y * 0.42D, 3.3D, s + 5);
        }
        for(int y = 0; y < h; y++)
        {
            for(int x = 0; x < w; x++)
            {
                double f = ring(x * 0.22D, y * 0.22D, w * 0.22D, s);
                double g = ring(x * 0.45D + 11D, y * 0.45D + 5D, w * 0.45D, s + 20);
                double cr, cg, cb;
                switch(p.type)
                {
                    case Cosmos.SCORCHED:
                    {
                        // Scorched
                        double crack = Math.abs(ring(x * 0.16D + 4D, y * 0.16D + 8D, w * 0.16D, s + 30) * 2D - 1D);
                        cr = 62D + 40D * g;
                        cg = 40D + 22D * g;
                        cb = 38D + 16D * g;
                        if(crack < 0.035D)
                        {
                            cr = 255D;
                            cg = 150D + 80D * f;
                            cb = 40D;
                        } else if(crack < 0.08D)
                        {
                            cr = 190D;
                            cg = 60D;
                            cb = 24D;
                        }
                        break;
                    }
                    case Cosmos.VEILED:
                    {
                        double swirl = ring(x * 0.09D + f * 1.6D, y * 0.3D, w * 0.09D, s + 3);
                        cr = 250D - 36D * swirl;
                        cg = 232D - 62D * swirl - 20D * tint;
                        cb = 170D - 84D * swirl;
                        break;
                    }
                    case Cosmos.DESERT:
                    {
                        boolean mare = f < 0.45D;
                        cr = mare ? 122D : 220D - 30D * g;
                        cg = mare ? 56D : 104D + 20D * tint - 20D * g;
                        cb = mare ? 42D : 52D;
                        // Dust storm
                        if(g > 0.8D && !mare)
                        {
                            cr = 240D;
                            cg = 176D;
                            cb = 110D;
                        }
                        int cap = 2 + (int)(g * 2.5D);
                        if(y < cap || y >= h - cap + 1)
                        {
                            cr = 240D;
                            cg = 244D;
                            cb = 250D;
                        }
                        break;
                    }
                    case Cosmos.GIANT:
                    case Cosmos.RINGED:
                    {
                        int yy = y + (int)((f - 0.5D) * 3D);
                        double b = bands[yy < 0 ? 0 : (yy >= h ? h - 1 : yy)];
                        boolean ringed = p.type == Cosmos.RINGED;
                        cr = (ringed ? 240D : 246D) - (ringed ? 62D : 86D) * b;
                        cg = (ringed ? 218D : 214D) - (ringed ? 76D : 116D) * b - 14D * tint;
                        cb = (ringed ? 150D : 168D) - (ringed ? 78D : 116D) * b;
                        if(!ringed)
                        {
                            // Storm spots
                            double dx = Math.abs(x - spotX), dy = (y - spotY) / 2.4D;
                            dx = Math.min(dx, w - dx) / 4.6D;
                            if(dx * dx + dy * dy < 1D)
                            {
                                cr = dx * dx + dy * dy < 0.4D ? 232D : 206D;
                                cg = dx * dx + dy * dy < 0.4D ? 96D : 72D;
                                cb = 48D;
                            }
                            dx = Math.abs(x - spot2X);
                            dx = Math.min(dx, w - dx) / 2.2D;
                            dy = (y - spot2Y) / 1.2D;
                            if(dx * dx + dy * dy < 1D)
                            {
                                cr = 255D;
                                cg = 244D;
                                cb = 226D;
                            }
                        }
                        break;
                    }
                    case Cosmos.CINDER:
                    {
                        // Pitted rock
                        double pit = ring(x * 0.6D + 2D, y * 0.6D + 9D, w * 0.6D, s + 12);
                        cr = 150D + 50D * g - 20D * tint;
                        cg = 138D + 46D * g - 16D * tint;
                        cb = 122D + 40D * g;
                        if(pit > 0.7D)
                        {
                            cr = 92D;
                            cg = 84D;
                            cb = 78D;
                        } else if(pit > 0.64D)
                        {
                            cr = 214D;
                            cg = 204D;
                            cb = 186D;
                        }
                        break;
                    }
                    case Cosmos.ASHEN:
                    {
                        // Lava plains
                        double haze = ring(x * 0.12D + f * 1.2D, y * 0.34D + 2D, w * 0.12D, s + 7);
                        cr = 150D + 50D * haze;
                        cg = 128D + 40D * haze - 14D * tint;
                        cb = 170D + 56D * haze;
                        if(g > 0.78D)
                        {
                            cr = 255D;
                            cg = 150D;
                            cb = 70D;
                        } else if(f < 0.32D)
                        {
                            cr = 86D;
                            cg = 72D;
                            cb = 104D;
                        }
                        break;
                    }
                    case Cosmos.OCEAN:
                    {
                        boolean land = f > 0.57D;
                        cr = land ? 96D + 40D * g : 36D;
                        cg = land ? 150D + 30D * g - 30D * tint : 96D + 50D * f;
                        cb = land ? 70D : 210D + 30D * g;
                        if(f > 0.7D)
                        {
                            // Desert
                            cr = 196D;
                            cg = 170D;
                            cb = 110D;
                        }
                        double cloud = ring(x * 0.3D + 31D, y * 0.5D + 2D, w * 0.3D, s + 44);
                        int cap = 2 + (int)(g * 2D);
                        if(cloud > 0.66D || y < cap || y >= h - cap + 1)
                        {
                            cr = 244D;
                            cg = 248D;
                            cb = 252D;
                        }
                        break;
                    }
                    case Cosmos.EMBER:
                    {
                        // Glowing giant
                        int yy = y + (int)((f - 0.5D) * 3D);
                        double b = bands[yy < 0 ? 0 : (yy >= h ? h - 1 : yy)];
                        cr = 96D + 150D * b;
                        cg = 26D + 90D * b * b;
                        cb = 30D + 26D * b;
                        if(b > 0.74D)
                        {
                            cr = 255D;
                            cg = 190D + 40D * g;
                            cb = 90D;
                        }
                        break;
                    }
                    case Cosmos.FROZEN:
                    {
                        double crack = Math.abs(ring(x * 0.2D + 4D, y * 0.2D + 8D, w * 0.2D, s + 30) * 2D - 1D);
                        cr = 214D + 30D * g;
                        cg = 232D + 18D * g;
                        cb = 250D;
                        if(crack < 0.04D)
                        {
                            cr = 70D;
                            cg = 130D;
                            cb = 200D;
                        } else if(crack < 0.09D)
                        {
                            cr = 150D;
                            cg = 196D;
                            cb = 236D;
                        } else if(f < 0.36D)
                        {
                            cr = 176D;
                            cg = 206D;
                            cb = 240D;
                        }
                        break;
                    }
                    case Cosmos.ZORWETH:
                    {
                        // Zorweth
                        double pit = ring(x * 0.5D + 3D, y * 0.5D + 6D, w * 0.5D, s + 15);
                        double big = ring(x * 0.18D + 7D, y * 0.18D + 1D, w * 0.18D, s + 27);
                        cr = 138D + 48D * g;
                        cg = 136D + 46D * g;
                        cb = 130D + 42D * g;
                        if(big > 0.74D || pit > 0.76D)
                        {
                            // Crater floor
                            double heart = Math.max(big - 0.74D, pit - 0.76D) / 0.2D;
                            cr = 52D + 40D * f;
                            cg = 170D + 60D * Math.min(1D, heart);
                            cb = 58D + 20D * f;
                        } else if(big > 0.69D || pit > 0.71D)
                        {
                            // Rim
                            cr = 206D;
                            cg = 204D;
                            cb = 196D;
                        } else if(f < 0.3D)
                        {
                            // Lowland
                            cr = 96D;
                            cg = 96D;
                            cb = 94D;
                        }
                        break;
                    }
                    case Cosmos.TWILIGHT:
                    {
                        // Frost world
                        cr = 92D + 50D * g;
                        cg = 64D + 30D * g;
                        cb = 140D + 60D * g - 20D * tint;
                        if(f > 0.62D)
                        {
                            cr = 206D;
                            cg = 190D;
                            cb = 232D;
                        } else if(f < 0.34D)
                        {
                            cr = 52D;
                            cg = 36D;
                            cb = 84D;
                        }
                        break;
                    }
                    default:
                    {
                        double b = bands[y];
                        cr = 120D + 30D * b;
                        cg = 216D + 18D * b - 20D * tint;
                        cb = 240D;
                        // Cloud streaks
                        if(g > 0.72D && y > 5 && y < h - 6)
                        {
                            cr = 232D;
                            cg = 248D;
                            cb = 255D;
                        }
                        break;
                    }
                }
                px[x + y * w] = argb(1D, cr, cg, cb);
            }
        }
        return px;
    }

    /** Limb shade. */
    public static int[] limb()
    {
        int n = LIMB_SIZE;
        int px[] = new int[n * n];
        double ring[] = { 0.34D, 0.17D, 0.07D };
        for(int y = 0; y < n; y++)
        {
            for(int x = 0; x < n; x++)
            {
                int d = Math.min(Math.min(x, y), Math.min(n - 1 - x, n - 1 - y));
                if(d < ring.length)
                {
                    px[x + y * n] = argb(ring[d], 0D, 0D, 6D);
                }
            }
        }
        return px;
    }

    /** Ring span. */
    public static final double RING_SPAN = 2.3D;

    public static int[] ring(Cosmos.Planet p)
    {
        int n = RING_SIZE;
        int px[] = new int[n * n];
        int s = (int)(p.seed & 0xffff);
        for(int y = 0; y < n; y++)
        {
            for(int x = 0; x < n; x++)
            {
                double u = Math.abs((x + 0.5D) / n * 2D - 1D), v = Math.abs((y + 0.5D) / n * 2D - 1D);
                double m = Math.max(u, v);
                if(m < 0.56D || m > 0.985D || (m > 0.79D && m < 0.835D))
                {
                    continue;
                }
                double b = noise(m * 34D, 0.5D, s);
                double a = m < 0.79D ? 0.9D : 0.6D;
                px[x + y * n] = argb(a, 232D - 60D * b, 208D - 62D * b, 150D - 50D * b);
            }
        }
        return px;
    }

    /** Comet head. */
    public static int[] coma(long seed)
    {
        int n = COMA_SIZE;
        int px[] = new int[n * n];
        int s = (int)(seed & 0xffff);
        for(int y = 0; y < n; y++)
        {
            for(int x = 0; x < n; x++)
            {
                double u = (x + 0.5D) / n * 2D - 1D, v = (y + 0.5D) / n * 2D - 1D;
                double rad = Math.sqrt(u * u + v * v);
                if(rad > 1D)
                {
                    continue;
                }
                double shells = 0.8D + 0.35D * Math.sin(rad * 22D + fbm(x * 0.1D, y * 0.1D, s) * 5D);
                double a = (Math.exp(-rad / 0.2D) * 1.1D * shells + Math.exp(-rad * rad / 0.004D)) * (1D - rad);
                double white = Math.exp(-rad * rad / 0.02D);
                px[x + y * n] = argb(Math.min(1D, a), 110D + 145D * white, 255D, 190D + 65D * white);
            }
        }
        return px;
    }

    /** Moon. */
    public static int[] moon(int base[], int bw, int bh, long seed)
    {
        int n = MOON_SIZE;
        int px[] = new int[n * n];
        Random r = new Random(seed ^ 0x6d6f6f6eL);
        double shade[] = new double[n * n];
        for(int i = 0; i < 90; i++)
        {
            double cx = r.nextDouble() * n, cy = r.nextDouble() * n;
            double rad = 1.5D + r.nextDouble() * r.nextDouble() * 9D;
            int x0 = (int)(cx - rad - 2), x1 = (int)(cx + rad + 2), y0 = (int)(cy - rad - 2), y1 = (int)(cy + rad + 2);
            for(int y = Math.max(0, y0); y <= Math.min(n - 1, y1); y++)
            {
                for(int x = Math.max(0, x0); x <= Math.min(n - 1, x1); x++)
                {
                    double d = Math.sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy)) / rad;
                    if(d < 0.8D)
                    {
                        shade[x + y * n] -= 0.14D;
                    } else if(d < 1.1D)
                    {
                        shade[x + y * n] += (x - cx) + (y - cy) < 0D ? 0.16D : -0.05D;
                    }
                }
            }
        }
        int s = r.nextInt(100000);
        for(int y = 0; y < n; y++)
        {
            for(int x = 0; x < n; x++)
            {
                int b = base[(x * bw / n) + (y * bh / n) * bw];
                int br = b >> 16 & 0xff, bg = b >> 8 & 0xff, bb = b & 0xff;
                if((b >>> 24) < 8 || br + bg + bb < 24)
                {
                    continue;
                }
                double k = 1D + shade[x + y * n] + (fbm(x * 0.09D, y * 0.09D, s) - 0.5D) * 0.3D;
                px[x + y * n] = argb(1D, br * k, bg * k, bb * k);
            }
        }
        return px;
    }

    // Naked-eye sky

    /** Naked-eye deep limit. */
    public static final double SKY_DEEP_MAG = 6.7D;

    /** Sky face. */
    public static int[] skyFace(Cosmos c, int face)
    {
        int n = SKY_FACE;
        int px[] = new int[n * n];
        double d[] = new double[3], band[] = new double[4];
        int s = (int)(c.seed & 0xffff);
        int count = 0;
        for(int i = 0; i < c.deep.length; i++)
        {
            count += c.deep[i].mag < SKY_DEEP_MAG ? 1 : 0;
        }
        Cosmos.Deep bright[] = new Cosmos.Deep[count];
        double A[][] = new double[count][3], B[][] = new double[count][3];
        count = 0;
        for(int i = 0; i < c.deep.length; i++)
        {
            Cosmos.Deep o = c.deep[i];
            if(o.mag < SKY_DEEP_MAG)
            {
                bright[count] = o;
                Cosmos.spriteAxes(o, A[count], B[count]);
                count++;
            }
        }
        for(int j = 0; j < n; j++)
        {
            for(int i = 0; i < n; i++)
            {
                Cosmos.faceDir(face, -1D + 2D * i / (n - 1D), -1D + 2D * j / (n - 1D), d);
                band[1] = 176D;
                band[2] = 200D;
                band[3] = 255D;
                skyGlow(c, d[0], d[1], d[2], 2D / n, s, band);
                double a = band[0], cr = band[1], cg = band[2], cb = band[3];
                for(int k = 0; k < count; k++)
                {
                    Cosmos.Deep o = bright[k];
                    double dot = d[0] * o.x + d[1] * o.y + d[2] * o.z;
                    if(dot < 0.96D)
                    {
                        continue;
                    }
                    double sigma = Math.max(o.size * Cosmos.ARCSEC * 0.3D, 1.1D / n);
                    double ea = (d[0] * A[k][0] + d[1] * A[k][1] + d[2] * A[k][2]) / sigma;
                    double eb = (d[0] * B[k][0] + d[1] * B[k][1] + d[2] * B[k][2]) / (sigma * Math.max(o.axis, 0.45D));
                    double glow = Math.exp(-(ea * ea + eb * eb)) * unit((SKY_DEEP_MAG + 0.6D - o.mag) / 2.4D) * 0.85D;
                    if(glow > 0.01D)
                    {
                        double t = glow / (glow + a + 1.0E-6D);
                        int rgb = Cosmos.isGalaxy(o.type) ? 0xc0d0ff : (Cosmos.isCluster(o.type) ? 0xfff0c8 : 0xff7090);
                        cr = cr * (1D - t) + (rgb >> 16 & 0xff) * t;
                        cg = cg * (1D - t) + (rgb >> 8 & 0xff) * t;
                        cb = cb * (1D - t) + (rgb & 0xff) * t;
                        a = Math.min(0.9D, a + glow);
                    }
                }
                px[i + j * n] = argb(a > 0.008D ? a : 0D, cr, cg, cb);
            }
        }
        return px;
    }

    /** Galaxy glow. */
    public static void skyGlow(Cosmos c, double x, double y, double z, double cell, int s, double out[])
    {
        double excess = Math.max(0D, c.density(x, y, z, 5) - 0.55D);
        if(excess <= 0D)
        {
            out[0] = 0D;
            return;
        }
        double sum = 0D, norm = 0D, amp = 1D, k = 9D;
        for(int o = 0; o < 18; o++)
        {
            // Octave fade
            double w = unit((2D / k) / (cell * 2.5D) - 1D);
            if(w <= 0D)
            {
                break;
            }
            double v = (noise(x * k + 11D, y * k + 3D, s + o) + noise(y * k + 50D, z * k + 7D, s + o + 40)
                + noise(z * k + 90D, x * k + 5D, s + o + 80)) / 3D - 0.5D;
            double wide = (cell * 48D) / (2D / k);
            amp = wide < 1D ? Math.pow(wide, 0.8D) : 1D;
            sum += v * amp * w;
            norm += amp * w * amp * w;
            k *= 2D;
        }
        double mottle = norm > 0D ? 0.25D + 1.6D * unit(sum / Math.sqrt(norm) * 2.6D + 0.5D) : 1.05D;
        out[0] = Math.min(0.66D, excess * 0.095D * mottle);
        double warm = Math.pow(unit(x * c.cenX + y * c.cenY + z * c.cenZ), 8D);
        out[1] = 176D + 79D * warm;
        out[2] = 200D + 30D * warm;
        out[3] = 255D - 90D * warm;
    }

    // Mips

    public static int[][] mips(int full[], int n)
    {
        return mips(full, n, n);
    }

    /** Mip chain. */
    public static int[][] mips(int full[], int w, int h)
    {
        int levels = 1;
        for(int m = Math.min(w, h); m > 1; m >>= 1)
        {
            levels++;
        }
        int out[][] = new int[levels][];
        out[0] = full;
        for(int l = 1; l < levels; l++)
        {
            int mw = w >> l, mh = h >> l;
            int src[] = out[l - 1];
            int dst[] = new int[mw * mh];
            for(int y = 0; y < mh; y++)
            {
                for(int x = 0; x < mw; x++)
                {
                    double a = 0D, cr = 0D, cg = 0D, cb = 0D;
                    for(int q = 0; q < 4; q++)
                    {
                        int c = src[(x * 2 + (q & 1)) + (y * 2 + (q >> 1)) * mw * 2];
                        double ca = (c >>> 24) / 255D;
                        a += ca;
                        cr += (c >> 16 & 0xff) * ca;
                        cg += (c >> 8 & 0xff) * ca;
                        cb += (c & 0xff) * ca;
                    }
                    if(a > 0D)
                    {
                        dst[x + y * mw] = argb(a / 4D, cr / a, cg / a, cb / a);
                    }
                }
            }
            out[l] = dst;
        }
        return out;
    }
}
