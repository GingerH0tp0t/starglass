package com.starglass.sky;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** The sky. */
public class Cosmos
{
    public static final double DEG = Math.PI / 180D;
    public static final double ARCSEC = DEG / 3600D;

    /** First band magnitude. */
    public static final double BAND0_MAG = 5.6D;
    public static final double BAND_WIDTH = 1.2D;
    public static final int FIRST_LEVEL = 3;
    public static final int MAX_BAND = 9;

    /** Star loop attempts. */
    public static final int VANILLA_ATTEMPTS = 1500;
    public static final int NAKED_ATTEMPTS = 3000;

    public static final int SINGLE = 0;
    public static final int DOUBLE = 1;
    public static final int VARIABLE = 2;

    public static final class Star
    {
        public double x, y, z;
        public float mag;
        /** Spectral class, O-M. */
        public int spectral;
        public int kind;
        /** Double star. */
        public float sep, mag2, pa;
        public int spectral2;
        /** Variable star. */
        public float period, amp;
        public float lightYears;
        /** Giant. */
        public boolean giant;
        /** Naked-eye index. */
        public int index = -1;
        public int hash;
        public String name;
        public String proper;

        public float magAt(double day)
        {
            if(kind != VARIABLE)
            {
                return mag;
            }
            return mag + amp * (float)(0.5D - 0.5D * Math.cos(day / period * Math.PI * 2D));
        }

        /** Mean magnitude. */
        public double meanMag()
        {
            return kind == VARIABLE ? mag + amp * 0.5D : mag;
        }

        /** Absolute magnitude. */
        public double absMag()
        {
            return meanMag() - 5D * Math.log10(lightYears / LY_10PC);
        }
    }

    public static final int SPIRAL = 0;
    public static final int ELLIPTICAL = 1;
    public static final int IRREGULAR = 2;
    public static final int EMISSION = 3;
    public static final int REFLECTION = 4;
    public static final int PLANETARY = 5;
    public static final int REMNANT = 6;
    public static final int OPEN_CLUSTER = 7;
    public static final int GLOBULAR = 8;
    // Third-book nebulae
    public static final int NURSERY = 9;
    public static final int BUBBLE = 10;
    public static final int BIPOLAR = 11;
    public static final int DARK = 12;
    // Far sky
    public static final int QUASAR = 13;
    public static final int RADIO_GALAXY = 14;
    public static final int COLLISION = 15;
    public static final int LENS = 16;
    public static final int PULSAR_NEBULA = 17;
    public static final int BLACK_HOLE = 18;
    public static final int GALAXY_CLUSTER = 19;
    public static final int PULSAR = 20;
    public static final int CORE = 21;

    public static final String DSO_KIND[] = {
        "Spiral galaxy", "Elliptical galaxy", "Irregular galaxy", "Emission nebula",
        "Reflection nebula", "Planetary nebula", "Supernova remnant", "Open cluster", "Globular cluster",
        "Star nursery", "Wind-blown bubble", "Bipolar nebula", "Dark nebula",
        "Quasar", "Radio galaxy", "Colliding galaxies", "Gravitational lens", "Pulsar wind nebula",
        "Black hole", "Galaxy cluster", "Pulsar", "Galactic core"
    };

    /** Kind notes. */
    public static final String DSO_NOTE[] = {
        "A turning disc of a hundred billion suns, its young blue stars strung along the arms.",
        "An old galaxy with no gas left to make stars from: a ball of ancient yellow suns.",
        "A small galaxy without a shape, usually because a bigger one is pulling it apart.",
        "Hydrogen set glowing red by the hot young stars inside it.",
        "Dust, shining blue with the light of stars it does not belong to.",
        "The outer layers of a dying sun, puffed off round the white cinder that is left.",
        "The wreck of a star that blew itself apart, still spreading.",
        "A few hundred stars born together from one cloud and not yet drifted apart.",
        "A ball of a hundred thousand suns as old as the galaxy, in orbit about it.",
        "A cloud collapsing into stars as you watch: the gas glows and the dust shines.",
        "A shell blown in the gas by the wind of one very hot star.",
        "A dying star throwing its gas out through two poles instead of all round.",
        "Dust too cold to shine. It is seen as the hole in what lies behind it - and by radio.",
        "The core of a young galaxy, where gas falling into a giant black hole outshines every star round it. Its light is older than this world.",
        "A galaxy whose central black hole throws two jets far out beyond its stars. The lobes at their ends show only to a dish.",
        "Two galaxies passing through each other. The stars miss; the gas does not, and the tides draw out tails.",
        "A cluster so heavy that it bends the light of a galaxy far behind it into arcs. The arcs are that galaxy, seen several times.",
        "A spinning neutron star and the cloud its wind lights from within. The star at the middle flashes many times a second.",
        "A dead star too heavy to be anything. It is seen by what falls in: a disc of gas hot enough to glow, and two jets.",
        "Hundreds of galaxies held by one another's weight, in a cloud of thin hot gas.",
        "A neutron star, a sun's weight in a ball a few miles across, sweeping a beam past us as it spins. No eye will ever see it.",
        "The middle of our own galaxy, hidden by dust from every eye. A dish sees straight through to what all of this turns round."
    };

    /** Radio silent. */
    public static final float SILENT = 99F;

    public static boolean isGalaxy(int type)
    {
        return type <= IRREGULAR || type == QUASAR || type == RADIO_GALAXY || type == COLLISION || type == LENS
            || type == GALAXY_CLUSTER;
    }

    /** Far book? */
    public static boolean isFar(int type)
    {
        return type >= QUASAR;
    }

    public static boolean isCluster(int type)
    {
        return type == OPEN_CLUSTER || type == GLOBULAR;
    }

    public static final class Deep
    {
        public int number;
        public int type;
        public double x, y, z;
        public float mag;
        /** Size, arcseconds. */
        public float size;
        public float axis;
        public float pa;
        public long seed;
        public float lightYears;
        public String name;
        public String proper;
        /** Host star. */
        public Star host;
        /** Radio magnitude. */
        public float radio = SILENT;
        /** Radio scale. */
        public float radioScale = 1F;
        /** Pulse period. */
        public float pulse;
        /** Radio view key. */
        public final RadioView radioView = new RadioView(this);
    }

    public static final class RadioView
    {
        public final Deep of;

        RadioView(Deep d)
        {
            of = d;
        }
    }

    public static final int SCORCHED = 0;
    public static final int VEILED = 1;
    public static final int DESERT = 2;
    public static final int GIANT = 3;
    public static final int RINGED = 4;
    public static final int ICE = 5;
    // Second six
    public static final int CINDER = 6;
    public static final int ASHEN = 7;
    public static final int OCEAN = 8;
    public static final int EMBER = 9;
    public static final int FROZEN = 10;
    public static final int TWILIGHT = 11;
    /** Zorweth. */
    public static final int ZORWETH = 12;

    public static final String PLANET_KIND[] = {
        "Scorched world", "Veiled world", "Desert world", "Banded giant", "Ringed giant", "Ice giant",
        "Cinder world", "Ashen world", "Ocean world", "Ember giant", "Frozen world", "Twilight world", "Zorweth"
    };

    public static final class Moon
    {
        /** Moon orbit. */
        public float orbit, period, phase, size;
        public int color;
        public String name;
    }

    public static final class Planet
    {
        public int index;
        public int type;
        public String name;
        /** Orbit radius. */
        public double a;
        public double period;
        public double phase0;
        public double incl;
        public double node;
        /** Diameter and magnitude at 1. */
        public double diam1;
        public double h;
        public long seed;
        public int color;
        public float ringTilt;
        public Moon moons[];

        // Position
        public double x, y, z;
        public double distance;
        public double sunDistance;
        /** Lit fraction. */
        public double lit;
        public double mag;
        public double diameter;
    }

    /** Comet. */
    public static final class Comet
    {
        public int index;
        public String name;
        /** Orbit. */
        public double q, e, a;
        public double period;
        /** Perihelion day. */
        public double t0;
        public double incl, node, peri;
        public double h;
        public double tailScale;
        public long seed;

        // Position
        public double x, y, z;
        public double distance;
        public double sunDistance;
        public double mag;
        /** Coma width. */
        public double coma;
        /** Head and tails. */
        public final double head[] = new double[3], ion[] = new double[3], dust[] = new double[3];
    }

    public final long seed;
    public final Star naked[];
    public final Planet planets[];
    public final Comet comets[];
    public final Deep deep[];
    /** Survey plates. */
    public final Deep plates[];
    public final String constellations[];
    private final double conX[], conY[], conZ[];
    /** Constellation figures. */
    public final int figures[][];
    public final double figureAt[][];
    /** Galactic pole and centre. */
    public final double poleX, poleY, poleZ, cenX, cenY, cenZ;
    /** Hidden facts. */
    public final double centreLy, ageGyr, darkFraction, glowKelvin;
    public final int signalStar;
    private final double riftPhase;

    private final Map cells = new LinkedHashMap(1024, 0.75F, true)
    {
        protected boolean removeEldestEntry(Map.Entry entry)
        {
            return size() > 6000;
        }
    };

    private static Cosmos current;

    /** Sky for a seed. */
    public static Cosmos of(long seed)
    {
        if(current == null || current.seed != seed)
        {
            current = new Cosmos(seed);
        }
        return current;
    }

    public Cosmos(long seed)
    {
        this.seed = seed;
        Random r = new Random(seed ^ 0x5DEECE66DL * 31L);
        double p[] = randomDir(r);
        poleX = p[0];
        poleY = p[1];
        poleZ = p[2];
        double c[] = randomDir(r);
        double dot = c[0] * poleX + c[1] * poleY + c[2] * poleZ;
        c[0] -= dot * poleX;
        c[1] -= dot * poleY;
        c[2] -= dot * poleZ;
        double len = Math.sqrt(c[0] * c[0] + c[1] * c[1] + c[2] * c[2]);
        cenX = c[0] / len;
        cenY = c[1] / len;
        cenZ = c[2] / len;
        riftPhase = r.nextDouble() * Math.PI * 2D;
        Random truths = new Random(seed * 0x9E3779B97F4A7C15L + 0x51C0FFEEL);
        centreLy = 19000D + truths.nextInt(15) * 1000D;
        ageGyr = 9.5D + truths.nextInt(71) * 0.1D;
        darkFraction = 0.78D + truths.nextInt(13) * 0.01D;
        glowKelvin = 2.4D + truths.nextInt(8) * 0.1D;
        int pick = truths.nextInt(1 << 20);

        constellations = CONSTELLATIONS;
        conX = new double[constellations.length];
        conY = new double[constellations.length];
        conZ = new double[constellations.length];
        Random fixed = new Random(10842L * 7919L);
        for(int i = 0; i < constellations.length; i++)
        {
            double d[] = randomDir(fixed);
            conX[i] = d[0];
            conY[i] = d[1];
            conZ[i] = d[2];
        }

        naked = nakedEye();
        figures = new int[constellations.length][];
        figureAt = new double[constellations.length][3];
        makeFigures();
        planets = makePlanets(new Random(seed * 6364136223846793005L + 1442695040888963407L));
        deep = makeDeep(new Random(seed * 0x9E3779B97F4A7C15L + 77L));
        plates = makePlates();
        comets = makeComets(new Random(seed * 0x2545F4914F6CDD1DL + 9L));
        int suns = 0;
        for(int i = 0; i < naked.length; i++)
        {
            suns += sunlike(naked[i]) ? 1 : 0;
        }
        int chosen = -1;
        for(int i = 0, k = suns == 0 ? 0 : pick % suns; i < naked.length && chosen < 0; i++)
        {
            if(sunlike(naked[i]) && k-- == 0)
            {
                chosen = i;
            }
        }
        signalStar = chosen;
    }

    /** Sunlike? */
    public static boolean sunlike(Star s)
    {
        return s.spectral >= 3 && s.spectral <= 5 && !s.giant && s.kind != VARIABLE;
    }

    /** Light speed, km/s. */
    public static final double LIGHT_KMS = 299792D;

    /** Recession. */
    public double recession(Deep d)
    {
        double own = (unit((int)(d.seed ^ d.seed >>> 32) + 93) - 0.5D) * 60D;
        return LIGHT_KMS * d.lightYears / (ageGyr * 1.0E9D) + own;
    }

    /** Spiral spin. */
    public static double spin(Deep d)
    {
        return 150D + unit((int)(d.seed ^ d.seed >>> 32) + 95) * 150D;
    }

    /** Candle magnitude. */
    public static double candleMag(Deep d)
    {
        return 5D * Math.log10(d.lightYears / LY_10PC) + (isGalaxy(d.type) ? -10D : -1.5D);
    }

    /** Has candles? */
    public static boolean hasCandles(Deep d)
    {
        return d.type == GLOBULAR || d.type == SPIRAL || d.type == IRREGULAR;
    }

    // Frames

    /** Sky to world (Beta). */
    public static void toWorld(double v[], double angle, double out[])
    {
        double t = angle * Math.PI * 2D;
        double c = Math.cos(t), s = Math.sin(t);
        double y = v[1] * c - v[2] * s;
        double z = v[1] * s + v[2] * c;
        out[0] = v[0];
        out[1] = y;
        out[2] = z;
    }

    public static void toSky(double v[], double angle, double out[])
    {
        toWorld(v, -angle, out);
    }

    /** Tangent axes. */
    public static void axes(double d[], double ax, double ay, double az, double A[], double B[])
    {
        double dot = ax * d[0] + ay * d[1] + az * d[2];
        B[0] = ax - dot * d[0];
        B[1] = ay - dot * d[1];
        B[2] = az - dot * d[2];
        double l = Math.sqrt(B[0] * B[0] + B[1] * B[1] + B[2] * B[2]);
        if(l < 1.0E-6D)
        {
            B[0] = 0D;
            B[1] = 0D;
            B[2] = 1D;
            l = 1D;
        }
        B[0] /= l;
        B[1] /= l;
        B[2] /= l;
        A[0] = B[1] * d[2] - B[2] * d[1];
        A[1] = B[2] * d[0] - B[0] * d[2];
        A[2] = B[0] * d[1] - B[1] * d[0];
    }

    /** Sprite axes. */
    public static void spriteAxes(Deep o, double RA[], double RB[])
    {
        double d[] = { o.x, o.y, o.z }, A[] = new double[3], B[] = new double[3];
        axes(d, 1D, 0D, 0D, A, B);
        double cp = Math.cos(o.pa * DEG), sp = Math.sin(o.pa * DEG);
        for(int q = 0; q < 3; q++)
        {
            RA[q] = A[q] * cp + B[q] * sp;
            RB[q] = B[q] * cp - A[q] * sp;
        }
    }

    private static double[] randomDir(Random r)
    {
        double z = r.nextDouble() * 2D - 1D;
        double t = r.nextDouble() * Math.PI * 2D;
        double q = Math.sqrt(1D - z * z);
        return new double[] { q * Math.cos(t), q * Math.sin(t), z };
    }

    private static int mix(int h)
    {
        h ^= h >>> 16;
        h *= 0x85ebca6b;
        h ^= h >>> 13;
        h *= 0xc2b2ae35;
        h ^= h >>> 16;
        return h;
    }

    private static float unit(int h)
    {
        return (float)(mix(h) & 0xffffff) / 16777216F;
    }

    // Stars

    /** Spectral colours. */
    public static final int SPECTRAL_RGB[] = {
        0x8fb0ff, 0xa9c4ff, 0xdfe8ff, 0xfffbf0, 0xffeeb8, 0xffc27a, 0xff8d55
    };
    public static final String SPECTRAL_NAME[] = {
        "O, blue", "B, blue-white", "A, white", "F, cream", "G, yellow", "K, orange", "M, red"
    };

    private static int spectralFrom(float u)
    {
        if(u < 0.02F) return 0;
        if(u < 0.10F) return 1;
        if(u < 0.24F) return 2;
        if(u < 0.40F) return 3;
        if(u < 0.58F) return 4;
        if(u < 0.82F) return 5;
        return 6;
    }

    /** Ten parsecs, light-years. */
    public static final double LY_10PC = 32.6D;

    /** Main sequence. */
    public static final double MAIN_SEQUENCE[] = { -4.5D, -1.5D, 1.2D, 3.0D, 4.8D, 6.3D, 8.8D };

    /** Pulse law. */
    public static double pulseLaw(double period)
    {
        return -1.2D - 2.4D * Math.log10(period);
    }

    private void dress(Star s, int h, boolean bright)
    {
        s.hash = h;
        s.spectral = spectralFrom(unit(h + 1));
        float u = unit(h + 2);
        float doubles = bright ? 0.13F : 0.06F;
        if(u < doubles)
        {
            s.kind = DOUBLE;
            s.sep = (float)(1.2D * Math.pow(37.5D, unit(h + 3)));
            s.mag2 = s.mag + 0.2F + unit(h + 4) * 2.8F;
            s.pa = unit(h + 5) * 360F;
            s.spectral2 = spectralFrom(unit(h + 6));
        } else if(u < doubles + 0.05F)
        {
            s.kind = VARIABLE;
            s.period = (float)(0.3D * Math.pow(40D, unit(h + 3)));
            s.amp = 0.4F + unit(h + 4) * 1.6F;
        }
        // Distance
        double mean = s.meanMag(), abs;
        if(s.kind == VARIABLE)
        {
            abs = pulseLaw(s.period) + (unit(h + 7) - 0.5D) * 0.3D;
        } else
        {
            abs = MAIN_SEQUENCE[s.spectral] + (unit(h + 7) - 0.5D) * 1.2D;
            if(s.spectral >= 4)
            {
                float odds = !bright ? 0.08F : (s.spectral == 4 ? 0.5F : (s.spectral == 5 ? 0.8F : 0.93F));
                if(unit(h + 8) < odds || LY_10PC * Math.pow(10D, (mean - abs) / 5D) < 6D)
                {
                    s.giant = true;
                    abs = 0.4D - (s.spectral - 4) * 0.5D + (unit(h + 9) - 0.5D) * 1.5D;
                }
            }
        }
        double ly = LY_10PC * Math.pow(10D, (mean - abs) / 5D);
        s.lightYears = (float)(ly < 4.2D ? 4.2D : (ly > 45000D ? 45000D : ly));
    }

    /** Naked-eye stars. */
    private Star[] nakedEye()
    {
        Random random = new Random(10842L);
        List list = new ArrayList();
        int vanilla = 0;
        for(int i = 0; i < NAKED_ATTEMPTS; i++)
        {
            if(i == VANILLA_ATTEMPTS)
            {
                vanilla = list.size();
            }
            double d = random.nextFloat() * 2.0F - 1.0F;
            double d1 = random.nextFloat() * 2.0F - 1.0F;
            double d2 = random.nextFloat() * 2.0F - 1.0F;
            double d3 = 0.25F + random.nextFloat() * 0.25F;
            double d4 = d * d + d1 * d1 + d2 * d2;
            if(d4 >= 1.0D || d4 <= 0.01D)
            {
                continue;
            }
            d4 = 1.0D / Math.sqrt(d4);
            random.nextDouble();
            Star s = new Star();
            s.x = d * d4;
            s.y = d1 * d4;
            s.z = d2 * d4;
            double t = (d3 - 0.25D) / 0.25D;
            s.mag = (float)(BAND0_MAG - 4.4D * t * t - 0.6D * t);
            s.index = list.size();
            dress(s, i * 7919 + 13, true);
            list.add(s);
        }
        Star all[] = (Star[])list.toArray(new Star[list.size()]);

        // Names
        int count[] = new int[constellations.length];
        Integer order[] = new Integer[all.length];
        for(int i = 0; i < order.length; i++)
        {
            order[i] = Integer.valueOf(i);
        }
        final Star ref[] = all;
        java.util.Arrays.sort(order, new java.util.Comparator()
        {
            public int compare(Object a, Object b)
            {
                return Float.compare(ref[((Integer)a).intValue()].mag, ref[((Integer)b).intValue()].mag);
            }
        });
        for(int pass = 0; pass < 2; pass++)
        {
            Random names = new Random(10842L * 31L + 5L + pass * 977L);
            int n = 0;
            for(int o = 0; o < order.length; o++)
            {
                Star s = all[order[o].intValue()];
                if((s.index < vanilla) != (pass == 0))
                {
                    continue;
                }
                int con = constellation(s.x, s.y, s.z);
                int rank = count[con]++;
                s.name = (rank < GREEK.length ? GREEK[rank] : String.valueOf(rank + 1)) + " " + constellations[con];
                if(n++ < 36)
                {
                    s.proper = properName(names);
                }
            }
        }
        return all;
    }

    /** Figure size. */
    public static final int FIGURE_STARS = 7;
    public static final double FIGURE_REACH = 21D;

    /** Figures. */
    private void makeFigures()
    {
        double reach = Math.cos(FIGURE_REACH * DEG);
        for(int c = 0; c < constellations.length; c++)
        {
            int pick[] = new int[FIGURE_STARS];
            int n = 0;
            for(int i = 0; i < naked.length; i++)
            {
                Star s = naked[i];
                if(s.x * conX[c] + s.y * conY[c] + s.z * conZ[c] < reach || constellation(s.x, s.y, s.z) != c)
                {
                    continue;
                }
                int at = n < FIGURE_STARS ? n++ : (naked[pick[n - 1]].mag > s.mag ? n - 1 : -1);
                if(at < 0)
                {
                    continue;
                }
                while(at > 0 && naked[pick[at - 1]].mag > s.mag)
                {
                    pick[at] = pick[at - 1];
                    at--;
                }
                pick[at] = i;
            }
            int lines[] = new int[Math.max(0, n - 1) * 2];
            boolean in[] = new boolean[n];
            if(n > 0)
            {
                in[0] = true;
            }
            for(int k = 0; k < n - 1; k++)
            {
                int ba = -1, bb = -1;
                double best = -2D;
                for(int a = 0; a < n; a++)
                {
                    for(int b = 0; in[a] && b < n; b++)
                    {
                        if(in[b])
                        {
                            continue;
                        }
                        Star p = naked[pick[a]], q = naked[pick[b]];
                        double dot = p.x * q.x + p.y * q.y + p.z * q.z;
                        if(dot > best)
                        {
                            best = dot;
                            ba = a;
                            bb = b;
                        }
                    }
                }
                in[bb] = true;
                lines[k * 2] = pick[ba];
                lines[k * 2 + 1] = pick[bb];
            }
            figures[c] = lines;
            double mx = 0D, my = 0D, mz = 0D;
            for(int k = 0; k < n; k++)
            {
                mx += naked[pick[k]].x;
                my += naked[pick[k]].y;
                mz += naked[pick[k]].z;
            }
            double ml = Math.sqrt(mx * mx + my * my + mz * mz);
            figureAt[c][0] = ml < 1.0E-9D ? conX[c] : mx / ml;
            figureAt[c][1] = ml < 1.0E-9D ? conY[c] : my / ml;
            figureAt[c][2] = ml < 1.0E-9D ? conZ[c] : mz / ml;
        }
    }

    public int constellation(double x, double y, double z)
    {
        int best = 0;
        double bd = -2D;
        for(int i = 0; i < conX.length; i++)
        {
            double d = x * conX[i] + y * conY[i] + z * conZ[i];
            if(d > bd)
            {
                bd = d;
                best = i;
            }
        }
        return best;
    }

    /** Star density. */
    public double density(double x, double y, double z, int k)
    {
        double sinb = x * poleX + y * poleY + z * poleZ;
        double b = Math.asin(sinb < -1D ? -1D : (sinb > 1D ? 1D : sinb)) / DEG;
        double px = x - sinb * poleX, py = y - sinb * poleY, pz = z - sinb * poleZ;
        double pl = Math.sqrt(px * px + py * py + pz * pz);
        double cosl = pl < 1.0E-9D ? 0D : (px * cenX + py * cenY + pz * cenZ) / pl;
        double sigma = 15D - 0.7D * k;
        if(sigma < 8D)
        {
            sigma = 8D;
        }
        double disk = Math.exp(-(b * b) / (sigma * sigma));
        double cd = x * cenX + y * cenY + z * cenZ;
        double ang = Math.acos(cd < -1D ? -1D : (cd > 1D ? 1D : cd)) / DEG;
        double bulge = Math.exp(-(ang * ang) / (17D * 17D));
        double rho = 0.42D + (1.5D + 0.28D * k) * disk * (0.55D + 0.45D * (0.5D + 0.5D * cosl)) + 2.6D * bulge;
        // Rift
        double qx = poleY * cenZ - poleZ * cenY, qy = poleZ * cenX - poleX * cenZ, qz = poleX * cenY - poleY * cenX;
        double l = Math.atan2(px * qx + py * qy + pz * qz, px * cenX + py * cenY + pz * cenZ);
        double lane = 0.5D + 0.5D * Math.sin(l * 5D + riftPhase) * Math.cos(l * 2D - riftPhase * 0.7D);
        double offset = 1.6D * Math.sin(l * 3D + riftPhase * 1.3D);
        double db = b - offset;
        rho *= 1D - 0.72D * lane * Math.exp(-(db * db) / (2.4D * 2.4D));
        return rho;
    }

    /** Face direction. */
    public static void faceDir(int face, double u, double v, double out[])
    {
        double x, y, z;
        switch(face)
        {
            case 0: x = 1D; y = v; z = -u; break;
            case 1: x = -1D; y = v; z = u; break;
            case 2: x = u; y = 1D; z = -v; break;
            case 3: x = u; y = -1D; z = v; break;
            case 4: x = u; y = v; z = 1D; break;
            default: x = -u; y = v; z = -1D; break;
        }
        double l = 1D / Math.sqrt(x * x + y * y + z * z);
        out[0] = x * l;
        out[1] = y * l;
        out[2] = z * l;
    }

    /** Cell stars. */
    public Star[] cell(int face, int level, int i, int j)
    {
        Long key = Long.valueOf(((long)face << 56) | ((long)level << 48) | ((long)i << 24) | (long)j);
        Star got[] = (Star[])cells.get(key);
        if(got != null)
        {
            return got;
        }
        int k = level - FIRST_LEVEL;
        int n = 1 << level;
        double u0 = -1D + 2D * i / n, v0 = -1D + 2D * j / n, w = 2D / n;
        double d[] = new double[3];
        faceDir(face, u0 + w * 0.5D, v0 + w * 0.5D, d);
        // Seed
        long stir = key.longValue() * 0x9E3779B97F4A7C15L;
        stir = (stir ^ stir >>> 30) * 0xBF58476D1CE4E5B9L;
        stir = (stir ^ stir >>> 27) * 0x94D049BB133111EBL;
        Random r = new Random(seed ^ stir ^ stir >>> 31);
        r.nextInt();
        double uc = u0 + w * 0.5D, vc = v0 + w * 0.5D;
        double area = Math.pow(1D + uc * uc + vc * vc, -1.5D);
        double lambda = 10.4D * Math.pow(0.9D, k) * density(d[0], d[1], d[2], k) * area;
        int count = (int)lambda;
        if(r.nextDouble() < lambda - count)
        {
            count++;
        }
        got = new Star[count];
        for(int s = 0; s < count; s++)
        {
            Star star = new Star();
            faceDir(face, u0 + w * r.nextDouble(), v0 + w * r.nextDouble(), d);
            star.x = d[0];
            star.y = d[1];
            star.z = d[2];
            double t = Math.log(1D + r.nextDouble() * 2.3D) / Math.log(3.3D);
            star.mag = (float)(BAND0_MAG + BAND_WIDTH * (k + t));
            dress(star, r.nextInt(), false);
            star.name = "FS " + (100000 + (star.hash & 0x7fffffff) % 900000);
            got[s] = star;
        }
        cells.put(key, got);
        return got;
    }

    // Planets

    private Planet[] makePlanets(Random r)
    {
        double a[] = { 0.40D, 0.72D, 1.52D, 3.2D, 5.6D, 8.5D, 0.26D, 0.56D, 2.3D, 4.3D, 6.9D, 10.6D };
        double diam[] = { 44D, 92D, 52D, 720D, 900D, 500D, 30D, 70D, 140D, 640D, 200D, 260D };
        double h[] = { -0.6D, -4.1D, -1.5D, -7.0D, -6.4D, -3.4D, 0.6D, -2.0D, -2.9D, -6.3D, -2.0D, -1.4D };
        int moons[] = { 0, 0, r.nextInt(3), 4, 3 + r.nextInt(3), 1 + r.nextInt(2), 0, 0, 0, 0, 0, 0 };
        Planet out[] = new Planet[a.length];
        for(int i = 0; i < out.length; i++)
        {
            if(i == CINDER)
            {
                r = new Random(seed * 0x5851F42D4C957F2DL + 0x14057B7EF767814FL);
                moons[OCEAN] = 1;
                moons[EMBER] = 2 + r.nextInt(3);
                moons[FROZEN] = r.nextInt(2);
                moons[TWILIGHT] = 1;
            }
            Planet p = new Planet();
            p.index = i;
            p.type = i;
            p.a = a[i] * (0.93D + r.nextDouble() * 0.14D);
            p.period = 12D * Math.pow(p.a, 1.5D);
            p.phase0 = r.nextDouble() * Math.PI * 2D;
            p.incl = (r.nextDouble() * 2D - 1D) * (a[i] < 1D ? 5D : 2.5D) * DEG;
            p.node = r.nextDouble() * Math.PI * 2D;
            p.diam1 = diam[i] * (0.9D + r.nextDouble() * 0.2D);
            p.h = h[i];
            p.seed = r.nextLong();
            p.ringTilt = 10F + r.nextFloat() * 18F;
            p.name = planetName(r);
            p.moons = new Moon[moons[i]];
            for(int m = 0; m < p.moons.length; m++)
            {
                Moon moon = new Moon();
                moon.orbit = (i == RINGED ? 3.4F : 2.4F) + m * 1.9F + r.nextFloat() * 0.8F;
                moon.period = (float)(0.11D * Math.pow(moon.orbit, 1.5D));
                moon.phase = r.nextFloat() * 6.2832F;
                moon.size = 0.05F + r.nextFloat() * 0.07F;
                if(i == TWILIGHT)
                {
                    moon.size = 0.42F;
                }
                int g = 150 + r.nextInt(90);
                moon.color = (g + r.nextInt(16)) << 16 | g << 8 | (g - r.nextInt(40));
                moon.name = p.name + " " + ROMAN[m];
                p.moons[m] = moon;
            }
            out[i] = p;
        }
        int tint[] = { 0xff7a3c, 0xfff0c8, 0xe0683a, 0xf2c890, 0xf0dca0, 0x8ee8f0,
            0xc4b4a0, 0xc0a4d8, 0x58a4ff, 0xe05438, 0xe8f6ff, 0xa07ce0 };
        for(int i = 0; i < out.length; i++)
        {
            out[i].color = tint[i];
        }
        Planet all[] = new Planet[out.length + 1];
        System.arraycopy(out, 0, all, 0, out.length);
        all[ZORWETH] = zorweth();
        return all;
    }

    /** Zorweth. */
    private static Planet zorweth()
    {
        Random r = new Random(0x5A0B3E7L);
        Planet p = new Planet();
        p.index = ZORWETH;
        p.type = ZORWETH;
        p.name = "Zorweth";
        p.a = 1.22D;
        p.period = 12D * Math.pow(p.a, 1.5D);
        p.phase0 = r.nextDouble() * Math.PI * 2D;
        p.incl = 1.6D * DEG;
        p.node = r.nextDouble() * Math.PI * 2D;
        p.diam1 = 40D;
        p.h = -0.4D;
        p.seed = r.nextLong();
        p.ringTilt = 0F;
        p.moons = new Moon[0];
        p.color = 0xb4b8ac;
        return p;
    }

    /** Place planets. */
    public void place(double day)
    {
        for(int i = 0; i < planets.length; i++)
        {
            Planet p = planets[i];
            double th = p.phase0 + day / p.period * Math.PI * 2D;
            double oy = Math.cos(th), oz = Math.sin(th);
            double cn = Math.cos(p.node), sn = Math.sin(p.node);
            double along = oy * cn + oz * sn;
            double across = -oy * sn + oz * cn;
            double ax = across * Math.sin(p.incl);
            across *= Math.cos(p.incl);
            double hy = along * cn - across * sn;
            double hz = along * sn + across * cn;
            double px = p.a * ax, py = 1D + p.a * hy, pz = p.a * hz;
            p.distance = Math.sqrt(px * px + py * py + pz * pz);
            p.x = px / p.distance;
            p.y = py / p.distance;
            p.z = pz / p.distance;
            p.sunDistance = p.a;
            // Phase angle
            double sx = -p.a * ax, sy = -p.a * hy, sz = -p.a * hz;
            double cosPhase = (sx * -px + sy * -py + sz * -pz) / (p.a * p.distance);
            p.lit = 0.5D + 0.5D * cosPhase;
            p.diameter = p.diam1 / p.distance;
            p.mag = p.h + 5D * Math.log10(p.a * p.distance) - 2.5D * Math.log10(p.lit < 0.03D ? 0.03D : p.lit);
        }
        double before[] = new double[3], hel[] = new double[3];
        for(int i = 0; i < comets.length; i++)
        {
            Comet c = comets[i];
            helio(c, day - 0.02D, before);
            helio(c, day, hel);
            double r = Math.sqrt(hel[0] * hel[0] + hel[1] * hel[1] + hel[2] * hel[2]);
            c.sunDistance = r;
            c.head[0] = hel[0];
            c.head[1] = 1D + hel[1];
            c.head[2] = hel[2];
            c.distance = Math.sqrt(c.head[0] * c.head[0] + c.head[1] * c.head[1] + c.head[2] * c.head[2]);
            c.x = c.head[0] / c.distance;
            c.y = c.head[1] / c.distance;
            c.z = c.head[2] / c.distance;
            c.mag = c.h + 5D * Math.log10(c.distance) + 10D * Math.log10(r);
            double near = 1.6D / r;
            c.coma = Math.min(2400D, 700D / c.distance) * (near < 0.25D ? 0.25D : (near > 1D ? 1D : near));
            // Tails
            double len = Math.min(1.5D, 0.42D / (r * r)) * c.tailScale;
            double vx = hel[0] - before[0], vy = hel[1] - before[1], vz = hel[2] - before[2];
            double vl = Math.sqrt(vx * vx + vy * vy + vz * vz) + 1.0E-12D;
            for(int q = 0; q < 3; q++)
            {
                double radial = hel[q] / r;
                double lag = (q == 0 ? vx : (q == 1 ? vy : vz)) / vl;
                c.ion[q] = c.head[q] + radial * len;
                c.dust[q] = c.head[q] + (radial * 0.72D - lag * 0.42D) * len;
            }
        }
    }

    /** Comet position. */
    private static void helio(Comet c, double day, double out[])
    {
        double m = (day - c.t0) / c.period;
        m = (m - Math.floor(m + 0.5D)) * Math.PI * 2D;
        double lo = -Math.PI, hi = Math.PI, ecc = m;
        for(int i = 0; i < 48; i++)
        {
            ecc = (lo + hi) * 0.5D;
            if(ecc - c.e * Math.sin(ecc) < m)
            {
                lo = ecc;
            } else
            {
                hi = ecc;
            }
        }
        double xo = c.a * (Math.cos(ecc) - c.e), yo = c.a * Math.sqrt(1D - c.e * c.e) * Math.sin(ecc);
        double cw = Math.cos(c.peri), sw = Math.sin(c.peri);
        double oy = xo * cw - yo * sw, oz = xo * sw + yo * cw;
        double cn = Math.cos(c.node), sn = Math.sin(c.node);
        double along = oy * cn + oz * sn;
        double across = -oy * sn + oz * cn;
        out[0] = across * Math.sin(c.incl);
        across *= Math.cos(c.incl);
        out[1] = along * cn - across * sn;
        out[2] = along * sn + across * cn;
    }

    private Comet[] makeComets(Random r)
    {
        double periods[] = { 30D + r.nextDouble() * 10D, 78D + r.nextDouble() * 30D, 0D, 0D };
        double q[] = { 0.30D + r.nextDouble() * 0.08D, 0.20D + r.nextDouble() * 0.08D, 0D, 0D };
        double h[] = { 3.5D, 2.3D, 3.1D, 1.9D };
        double tail[] = { 1D, 1.6D, 1.2D, 2D };
        Comet out[] = new Comet[periods.length];
        for(int i = 0; i < out.length; i++)
        {
            if(i == 2)
            {
                periods[2] = 48D + r.nextDouble() * 12D;
                periods[3] = 118D + r.nextDouble() * 34D;
                q[2] = 0.27D + r.nextDouble() * 0.08D;
                q[3] = 0.17D + r.nextDouble() * 0.08D;
            }
            Comet c = new Comet();
            c.index = i;
            c.period = periods[i];
            c.a = Math.pow(c.period / 12D, 2D / 3D);
            c.q = q[i];
            c.e = 1D - c.q / c.a;
            c.t0 = r.nextDouble() * c.period;
            c.incl = (r.nextDouble() * 2D - 1D) * 28D * DEG;
            c.node = r.nextDouble() * Math.PI * 2D;
            c.peri = r.nextDouble() * Math.PI * 2D;
            c.h = h[i];
            c.tailScale = tail[i];
            c.seed = r.nextLong();
            c.name = "Comet " + properName(r);
            out[i] = c;
        }
        return out;
    }

    // Sun

    /** Sun disc in sun.png. */
    public static final double SUN_DISC0 = 0.375D, SUN_DISC1 = 0.625D;
    public static final int SUN_SPOTS = 9;

    /** Sunspots. */
    public int sunSpots(double day, double out[][])
    {
        int n = 0;
        double half = (SUN_DISC1 - SUN_DISC0) / 2D;
        for(int k = 0; k < SUN_SPOTS; k++)
        {
            double life = (day + k * 1.37D) / 9D;
            int gen = (int)Math.floor(life);
            double age = life - gen;
            int hsh = mix((int)(seed ^ seed >>> 32) + gen * 7919 + k * 104729);
            double lat = (unit(hsh + 1) * 2D - 1D) * 0.62D;
            double lon = unit(hsh + 2) * Math.PI * 2D + day / 6.3D * Math.PI * 2D;
            double size = 0.005D + unit(hsh + 3) * 0.011D;
            double facing = Math.cos(lon);
            if(facing < 0.12D)
            {
                continue;
            }
            double uc = 0.5D + half * 0.82D * Math.sin(lon) * Math.cos(lat);
            double vc = 0.5D + half * 0.82D * Math.sin(lat);
            double hw = size * facing, hv = size * 0.8D;
            out[n][0] = uc - hw;
            out[n][1] = vc - hv;
            out[n][2] = uc + hw;
            out[n][3] = vc + hv;
            out[n][4] = Math.sin(age * Math.PI);
            n++;
        }
        return n;
    }

    // Deep sky

    /** Deep-sky catalogue. */
    private Deep[] makeDeep(Random r)
    {
        int counts[] = { 22, 14, 6, 14, 6, 10, 4, 18, 16 };
        List book = deepBook(r, counts, false);
        // Neighbour galaxy
        Deep near = (Deep)book.get(0);
        near.mag = 4.4F;
        near.size = 9000F;
        near.axis = 0.38F;
        near.lightYears = 2.1E6F;
        List supplement = deepBook(new Random(seed * 0x9E3779B97F4A7C15L + 78L), counts, false);
        int nebulae[] = { 0, 0, 0, 10, 4, 6, 4, 0, 0, 10, 8, 8, 6 };
        List third = deepBook(new Random(seed * 0x9E3779B97F4A7C15L + 79L), nebulae, true);
        int far[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10, 8, 8, 6, 6, 6, 8, 12, 1 };
        List fourth = deepBook(new Random(seed * 0x9E3779B97F4A7C15L + 80L), far, false);

        Deep all[] = new Deep[book.size() + supplement.size() + third.size() + fourth.size()];
        int at = 0;
        for(int pass = 0; pass < 4; pass++)
        {
            List list = pass == 0 ? book : (pass == 1 ? supplement : (pass == 2 ? third : fourth));
            Deep part[] = (Deep[])list.toArray(new Deep[list.size()]);
            java.util.Arrays.sort(part, new java.util.Comparator()
            {
                public int compare(Object a, Object b)
                {
                    return Float.compare(((Deep)a).mag, ((Deep)b).mag);
                }
            });
            String names[][] = pass == 0 ? DEEP_NAMES : (pass == 1 ? DEEP_NAMES_2 : (pass == 2 ? DEEP_NAMES_3 : DEEP_NAMES_4));
            int used[] = new int[DSO_KIND.length];
            for(int i = 0; i < part.length; i++)
            {
                Deep d = part[i];
                d.number = at + 1;
                d.name = "MC " + d.number;
                String pool[] = d.type < names.length ? names[d.type] : new String[0];
                if(used[d.type] < pool.length)
                {
                    d.proper = pool[used[d.type]++];
                }
                all[at++] = d;
            }
        }
        // Globular clusters
        for(int i = 0; i < all.length; i++)
        {
            Deep d = all[i];
            if(d.type == GLOBULAR)
            {
                double cd = d.x * cenX + d.y * cenY + d.z * cenZ;
                float u = unit((int)(d.seed ^ d.seed >>> 32) + 91);
                d.lightYears = (float)(cd > GLOBULAR_AXIS ? centreLy / cd * (0.8D + 0.4D * u)
                    : centreLy * (0.2D + 0.5D * u));
            } else if(d.type == CORE)
            {
                d.lightYears = (float)centreLy;
            }
        }
        // Radio
        for(int i = 0; i < all.length; i++)
        {
            if(!isFar(all[i].type))
            {
                all[i].radio = radioOf(all[i]);
            }
        }
        // Hosts
        double beside = Math.cos(7D * DEG);
        for(int i = 0; i < all.length; i++)
        {
            Deep d = all[i];
            double best = beside;
            for(int c = 0; c < figures.length; c++)
            {
                for(int k = 0; k < figures[c].length; k++)
                {
                    Star st = naked[figures[c][k]];
                    double dot = st.x * d.x + st.y * d.y + st.z * d.z;
                    if(dot > best)
                    {
                        best = dot;
                        d.host = st;
                    }
                }
            }
        }
        return all;
    }

    /** Globular axis. */
    public static final double GLOBULAR_AXIS = 0.77D;

    private static float radioOf(Deep d)
    {
        float u = unit((int)(d.seed ^ d.seed >>> 32) + 77);
        switch(d.type)
        {
            case SPIRAL: return d.mag + 1.5F;
            case ELLIPTICAL: return u < 0.25F ? d.mag - 3F : SILENT;
            case IRREGULAR: return d.mag + 2F;
            case EMISSION: return d.mag + 0.5F;
            case PLANETARY: return d.mag + 2F;
            case REMNANT: return d.mag - 3F;
            case NURSERY: return d.mag;
            case BUBBLE: return d.mag + 1F;
            case DARK: return d.mag - 1.5F;
            default: return SILENT;
        }
    }

    /** Survey plates. */
    private Deep[] makePlates()
    {
        List list = new ArrayList();
        for(int pass = 0; pass < 2; pass++)
        {
            for(int i = 0; i < deep.length; i++)
            {
                Deep d = deep[i];
                if(d.mag < 90F && (pass == 0 ? isFar(d.type) : (!isFar(d.type) && d.mag > 11.5F)))
                {
                    list.add(d);
                }
            }
        }
        return (Deep[])list.toArray(new Deep[list.size()]);
    }

    public Deep plateSubject(int exposure)
    {
        return plates[(exposure % plates.length + plates.length) % plates.length];
    }

    /** Beside a figure. */
    private double[] besideFigure(Random r)
    {
        for(int tries = 0; tries < 60; tries++)
        {
            int lines[] = figures[r.nextInt(figures.length)];
            if(lines.length == 0)
            {
                continue;
            }
            Star st = naked[lines[r.nextInt(lines.length)]];
            double d[] = { st.x, st.y, st.z }, A[] = new double[3], B[] = new double[3];
            axes(d, 1D, 0D, 0D, A, B);
            double off = (1.2D + r.nextDouble() * 4.3D) * DEG, th = r.nextDouble() * Math.PI * 2D;
            double ca = Math.cos(off), sa = Math.sin(off), ct = Math.cos(th), sn = Math.sin(th);
            double out[] = new double[3];
            for(int q = 0; q < 3; q++)
            {
                out[q] = d[q] * ca + (A[q] * ct + B[q] * sn) * sa;
            }
            return out;
        }
        return randomDir(r);
    }

    /** One book. */
    private List deepBook(Random r, int counts[], boolean hosted)
    {
        List list = new ArrayList();
        for(int type = 0; type < counts.length; type++)
        {
            for(int n = 0; n < counts[type]; n++)
            {
                Deep d = new Deep();
                d.type = type;
                d.seed = r.nextLong();
                double dir[] = null;
                for(int tries = 0; tries < 400; tries++)
                {
                    dir = randomDir(r);
                    double sinb = dir[0] * poleX + dir[1] * poleY + dir[2] * poleZ;
                    double b = Math.abs(Math.asin(sinb)) / DEG;
                    double cd = dir[0] * cenX + dir[1] * cenY + dir[2] * cenZ;
                    boolean ok;
                    if(isGalaxy(type))
                    {
                        ok = b > 16D;
                    } else if(type == GLOBULAR)
                    {
                        ok = cd > 0.15D + r.nextDouble() * 0.6D;
                    } else if(type == PLANETARY)
                    {
                        ok = b < 30D;
                    } else
                    {
                        ok = b < 9D;
                    }
                    if(ok)
                    {
                        break;
                    }
                }
                if(hosted)
                {
                    dir = besideFigure(r);
                }
                if(type == CORE)
                {
                    dir = new double[] { cenX, cenY, cenZ };
                }
                d.x = dir[0];
                d.y = dir[1];
                d.z = dir[2];
                d.pa = r.nextFloat() * 360F;
                d.axis = 1F;
                float u = r.nextFloat();
                switch(type)
                {
                    case SPIRAL:
                        d.mag = 8.2F + u * 5.2F;
                        d.size = 1500F - u * 1300F;
                        d.axis = 0.25F + r.nextFloat() * 0.75F;
                        d.lightYears = (3F + u * 60F) * 1.0E6F;
                        break;
                    case ELLIPTICAL:
                        d.mag = 8.8F + u * 4.8F;
                        d.size = 700F - u * 560F;
                        d.axis = 0.5F + r.nextFloat() * 0.5F;
                        d.lightYears = (8F + u * 90F) * 1.0E6F;
                        break;
                    case IRREGULAR:
                        d.mag = 9.5F + u * 3.5F;
                        d.size = 900F - u * 650F;
                        d.axis = 0.6F + r.nextFloat() * 0.4F;
                        d.lightYears = (1F + u * 20F) * 1.0E6F;
                        break;
                    case EMISSION:
                        d.mag = 5.5F + u * 5.5F;
                        d.size = 4200F - u * 3300F;
                        d.lightYears = 900F + u * 7000F;
                        break;
                    case REFLECTION:
                        d.mag = 7.5F + u * 3.5F;
                        d.size = 1500F - u * 1100F;
                        d.lightYears = 400F + u * 2500F;
                        break;
                    case PLANETARY:
                        d.mag = 8.0F + u * 4.5F;
                        d.size = 200F - u * 170F;
                        d.lightYears = 700F + u * 6000F;
                        break;
                    case REMNANT:
                        d.mag = 8.0F + u * 3.5F;
                        d.size = 2600F - u * 2000F;
                        d.lightYears = 1500F + u * 9000F;
                        break;
                    case OPEN_CLUSTER:
                        d.mag = 4.6F + u * 5.4F;
                        d.size = 3200F - u * 2600F;
                        d.lightYears = 400F + u * 6000F;
                        break;
                    case NURSERY:
                        d.mag = 4.8F + u * 4.6F;
                        d.size = 13000F - u * 9500F;
                        d.lightYears = 500F + u * 4500F;
                        break;
                    case BUBBLE:
                        d.mag = 7.2F + u * 4.6F;
                        d.size = 2200F - u * 1900F;
                        d.lightYears = 1800F + u * 9000F;
                        break;
                    case BIPOLAR:
                        d.mag = 8.4F + u * 4.4F;
                        d.size = 420F - u * 370F;
                        d.lightYears = 900F + u * 7000F;
                        break;
                    case DARK:
                        d.mag = 6.4F + u * 4.2F;
                        d.size = 6500F - u * 5000F;
                        d.lightYears = 350F + u * 1800F;
                        break;
                    case QUASAR:
                        d.mag = 14.5F + u * 3.5F;
                        d.size = 60F - u * 36F;
                        d.lightYears = (0.8F + u * 9F) * 1.0E9F;
                        d.radio = 4F + r.nextFloat() * 4F;
                        d.radioScale = 3F;
                        break;
                    case RADIO_GALAXY:
                        d.mag = 13F + u * 4F;
                        d.size = 400F - u * 280F;
                        d.axis = 0.7F + r.nextFloat() * 0.3F;
                        d.lightYears = (60F + u * 600F) * 1.0E6F;
                        d.radio = 3F + r.nextFloat() * 4F;
                        d.radioScale = 4F;
                        break;
                    case COLLISION:
                        d.mag = 12.5F + u * 4F;
                        d.size = 900F - u * 650F;
                        d.lightYears = (20F + u * 200F) * 1.0E6F;
                        d.radio = d.mag - 3.5F;
                        break;
                    case LENS:
                        d.mag = 16.5F + u * 2F;
                        d.size = 90F - u * 60F;
                        d.lightYears = (2F + u * 6F) * 1.0E9F;
                        break;
                    case PULSAR_NEBULA:
                        d.mag = 11F + u * 4.5F;
                        d.size = 420F - u * 300F;
                        d.lightYears = 2000F + u * 20000F;
                        d.radio = 4F + r.nextFloat() * 3F;
                        d.pulse = 0.03F + r.nextFloat() * 0.27F;
                        break;
                    case BLACK_HOLE:
                        d.mag = 15F + u * 3.5F;
                        d.size = 60F - u * 36F;
                        d.lightYears = 3000F + u * 25000F;
                        d.radio = 6F + r.nextFloat() * 3F;
                        d.radioScale = 2F;
                        break;
                    case GALAXY_CLUSTER:
                        d.mag = 14F + u * 4F;
                        d.size = 2400F - u * 1700F;
                        d.lightYears = (150F + u * 900F) * 1.0E6F;
                        d.radio = d.mag - 5F;
                        break;
                    case PULSAR:
                        d.mag = SILENT;
                        d.size = 20F;
                        d.lightYears = 800F + u * 30000F;
                        d.radio = 5F + u * 5F;
                        d.pulse = (float)(0.2D * Math.pow(15D, r.nextDouble()));
                        break;
                    case CORE:
                        d.mag = SILENT;
                        d.size = 2400F;
                        d.lightYears = 26000F;
                        d.radio = 1.5F;
                        break;
                    default:
                        d.mag = 6.0F + u * 5.5F;
                        d.size = 1300F - u * 1000F;
                        d.lightYears = 12000F + u * 80000F;
                        break;
                }
                if(hosted && type <= REMNANT)
                {
                    float wide = (float)Math.pow(5D, r.nextDouble() * 2D - 1.1D);
                    d.size *= wide;
                    d.mag -= (float)(1.1D * Math.log10(wide));
                }
                list.add(d);
            }
        }
        return list;
    }

    // Names

    private static final String GREEK[] = {
        "Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Zeta", "Eta", "Theta", "Iota", "Kappa", "Lambda", "Mu",
        "Nu", "Xi", "Omicron", "Pi", "Rho", "Sigma", "Tau", "Upsilon", "Phi", "Chi", "Psi", "Omega"
    };
    private static final String ROMAN[] = { "I", "II", "III", "IV", "V", "VI" };

    /** Constellations. */
    private static final String CONSTELLATIONS[] = {
        "Pickaxe", "Creeper", "Lantern", "Minecart", "Furnace", "Boat", "Sapling", "Ghast", "Spider", "Wolf",
        "Squid", "Bucket", "Compass", "Clock", "Plough", "Portal", "Chest", "Torch", "Bow", "Saddle",
        "Shears", "Ladder", "Cauldron", "Anchor"
    };

    private static final String DEEP_NAMES[][] = {
        { "The Great Whirl", "Lapis Whirlpool", "The Minecart Wheel", "Spider's Web Galaxy" },
        { "The Snowball", "Slimeball Galaxy" },
        { "The Spilled Bucket" },
        { "Redstone Veil", "The Furnace Nebula", "Netherglow", "Rose Bush Nebula" },
        { "The Cold Lantern", "Ghast's Tear" },
        { "Creeper's Eye", "The Ender Ring", "Glass Bead Nebula" },
        { "The Burst Chest", "Cobweb Nebula" },
        { "The Torch Cluster", "Diamond Scatter", "The Seven Saplings", "Flint Sparks" },
        { "The Glowstone Cluster", "The Beehive", "Great Clay Ball" },
    };

    private static final String DEEP_NAMES_2[][] = {
        { "The Compass Rose", "Snowdrift Galaxy", "The Fishing Line" },
        { "The Egg", "Pumpkin Galaxy" },
        { "The Dropped Stack" },
        { "Lava Fall Nebula", "The Poppy", "Cinder Cloud" },
        { "The Ice Sheet", "Squid Ink Nebula" },
        { "The Slime Bubble", "Clock Face Nebula" },
        { "The Broken Pick", "String Nebula" },
        { "The Wheat Field", "Gravel Spill", "The Coal Seam" },
        { "The Snow Globe", "Mob Spawner Cluster", "The Lump of Gold" },
    };

    private static final String DEEP_NAMES_3[][] = {
        {}, {}, {},
        { "The Bed Nebula", "Cactus Flower", "The Minecart Track", "Sugar Cane Nebula" },
        { "The Blue Wool", "Lapis Dust" },
        { "The Jack o'Lantern", "Soul Sand Hourglass", "The Dispenser" },
        { "The Snapped Bow", "Feather Nebula" },
        {}, {},
        { "The Great Forge", "Mother Lode", "The Mob Farm", "Spawning Grounds" },
        { "The Water Bucket", "Glass Pane Bubble", "The Snowman" },
        { "The Double Chest", "Piston Nebula", "The Hourglass" },
        { "The Obsidian Pillar", "Coal Sack", "The Cave Mouth" },
    };

    private static final String DEEP_NAMES_4[][] = {
        {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
        { "The Beacon", "Glowstone Spark", "The Far Torch" },
        { "The Double Piston", "Bellows Galaxy" },
        { "The Crossed Swords", "Tangled Lines", "The Leash" },
        { "The Glass Ring", "Diamond Arc" },
        { "The Jukebox", "Redstone Clock" },
        { "The Void Eye", "Bottomless Well" },
        { "The Chicken Coop", "Slime Swarm" },
        { "The Metronome", "Note Block", "The Repeater" },
        { "The Heart of the Wheel" },
    };

    private static final String SYL_A[] = {
        "Al", "Ve", "Mir", "Tha", "Sa", "Den", "Ka", "Or", "Zu", "Ri", "Be", "Nu", "Ald", "Fo", "Men", "Shel", "Ach", "Pol"
    };
    private static final String SYL_B[] = {
        "de", "ga", "ta", "ra", "ni", "bel", "pha", "dra", "ku", "mal", "the", "si", "lo", "ber", "za", "ro"
    };
    private static final String SYL_C[] = {
        "ran", "rak", "bah", "neb", "ris", "tar", "ph", "geuse", "nar", "x", "th", "ir", "bol", "mar", "us", "yon"
    };

    private static String properName(Random r)
    {
        String s = SYL_A[r.nextInt(SYL_A.length)];
        if(r.nextInt(3) != 0)
        {
            s += SYL_B[r.nextInt(SYL_B.length)];
        }
        return s + SYL_C[r.nextInt(SYL_C.length)];
    }

    private static final String PL_A[] = { "Ob", "Car", "Thes", "Vol", "Ny", "Hal", "Quo", "Is", "Mor", "Ere", "Tal", "Zeph" };
    private static final String PL_B[] = { "sid", "ne", "si", "ca", "x", "cy", "ru", "me", "do", "bu", "o", "y" };
    private static final String PL_C[] = { "ia", "on", "us", "ar", "is", "a", "um", "ene", "os", "an" };

    private static String planetName(Random r)
    {
        return PL_A[r.nextInt(PL_A.length)] + PL_B[r.nextInt(PL_B.length)] + PL_C[r.nextInt(PL_C.length)];
    }
}
