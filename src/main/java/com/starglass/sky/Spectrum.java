package com.starglass.sky;

import java.util.Arrays;

/** Spectra. */
public final class Spectrum
{
    public static final int BINS = 7;
    public static final double NM[] = { 420D, 460D, 500D, 540D, 580D, 620D, 660D };
    private static final double BIN_NM = 40D;

    /** Channel spread. */
    private static final double CR[] = { 0D, 0D, 0D, 0.02D, 0.3D, 1D, 1D };
    private static final double CG[] = { 0D, 0.15D, 0.75D, 1D, 0.65D, 0.1D, 0D };
    private static final double CB[] = { 1D, 1D, 0.45D, 0.05D, 0D, 0D, 0D };

    /** Class temperatures. */
    private static final double CLASS_KELVIN[] = { 32000D, 16000D, 9200D, 6900D, 5800D, 4500D, 3400D };

    /** Stretch scale. */
    private static final double STRETCH_LY = 4.0E9D;

    public static final double FLAT[] = norm(new double[] { 1D, 1D, 1D, 1D, 1D, 1D, 1D });
    public static final double SUN[] = blackbody(5800D, 0D);
    public static final double STAR[][] = new double[CLASS_KELVIN.length][];
    /** Lamplight. */
    public static final double LAMP[] = blackbody(1900D, 0D);
    /** Airglow. */
    public static final double AIRGLOW[];
    /** Scattered light. */
    public static final double SCATTER[];
    /** Comet. */
    public static final double COMET[];

    static
    {
        for(int c = 0; c < CLASS_KELVIN.length; c++)
        {
            STAR[c] = blackbody(CLASS_KELVIN[c], 0D);
        }
        double a[] = scaled(FLAT, 0.3D);
        line(a, 557.7D, 0.4D);
        line(a, 589D, 0.18D);
        line(a, 630D, 0.12D);
        AIRGLOW = norm(a);
        double s[] = new double[BINS];
        for(int i = 0; i < BINS; i++)
        {
            s[i] = SUN[i] * Math.pow(500D / NM[i], 4D);
        }
        SCATTER = norm(s);
        double c[] = scaled(SUN, 0.3D);
        line(c, 516.5D, 0.4D);
        line(c, 563.5D, 0.15D);
        line(c, 473.7D, 0.12D);
        COMET = norm(c);
    }

    private Spectrum()
    {
    }

    // Building

    public static double[] norm(double s[])
    {
        double t = 0D;
        for(double v : s)
        {
            t += v;
        }
        double out[] = new double[BINS];
        for(int i = 0; i < BINS; i++)
        {
            out[i] = t > 0D ? s[i] / t : 1D / BINS;
        }
        return out;
    }

    private static double[] scaled(double s[], double k)
    {
        double out[] = new double[BINS];
        for(int i = 0; i < BINS; i++)
        {
            out[i] = s[i] * k;
        }
        return out;
    }

    /** Blackbody. */
    public static double[] blackbody(double kelvin, double z)
    {
        double out[] = new double[BINS];
        for(int i = 0; i < BINS; i++)
        {
            double lam = NM[i] / (1D + z) * 1.0E-9D;
            out[i] = 1D / (Math.pow(lam, 5D) * (Math.exp(0.014388D / (lam * kelvin)) - 1D));
        }
        return norm(out);
    }

    /** Add a line. */
    public static void line(double s[], double nm, double weight)
    {
        double sigma = 16D;
        double k = BIN_NM / (sigma * Math.sqrt(2D * Math.PI));
        for(int i = 0; i < BINS; i++)
        {
            double d = (NM[i] - nm) / sigma;
            s[i] += weight * k * Math.exp(-0.5D * d * d);
        }
    }

    /** sRGB to linear. */
    public static double linear(int c)
    {
        double v = c / 255D;
        return v <= 0.04045D ? v / 12.92D : Math.pow((v + 0.055D) / 1.055D, 2.4D);
    }

    /** Colour curve. */
    public static double[] curve(double r, double g, double b)
    {
        double out[] = new double[BINS];
        for(int i = 0; i < BINS; i++)
        {
            out[i] = (r * CR[i] + g * CG[i] + b * CB[i]) / (CR[i] + CG[i] + CB[i]);
        }
        return out;
    }

    /** Glass curve. */
    public static double[] glass(int rgb)
    {
        int ri = rgb >> 16 & 255, gi = rgb >> 8 & 255, bi = rgb & 255;
        double r = ri / 255D, g = gi / 255D, b = bi / 255D;
        double max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        double sat = max <= 0D ? 0D : (max - min) / max;
        double hue;
        if(max - min < 1.0E-6D)
        {
            hue = 0D;
        } else if(max == r)
        {
            hue = 60D * (((g - b) / (max - min) + 6D) % 6D);
        } else if(max == g)
        {
            hue = 60D * ((b - r) / (max - min) + 2D);
        } else
        {
            hue = 60D * ((r - g) / (max - min) + 4D);
        }
        double peak = linear(Math.max(ri, Math.max(gi, bi)));
        double stop = Math.min(1D, sat * 1.15D);
        double out[] = new double[BINS];
        for(int i = 0; i < BINS; i++)
        {
            double lam = NM[i], p;
            if(hue < 70D || hue >= 345D)
            {
                // Long-pass
                double h = hue >= 345D ? hue - 360D : hue;
                double cut = 612D - 1.9D * h;
                p = 1D / (1D + Math.exp(-(lam - cut) / 10D));
            } else if(hue < 250D)
            {
                // Band-pass
                double centre = 600D - 0.62D * hue;
                double d = (lam - centre) / 40D;
                p = Math.exp(-0.5D * d * d);
            } else
            {
                // Notch
                double d = (lam - 550D) / 45D;
                p = 1D - Math.exp(-0.5D * d * d);
            }
            out[i] = peak * (1D - stop * (1D - p));
        }
        return out;
    }

    /** Colour curve. */
    public static double[] curve(int rgb)
    {
        return curve(linear(rgb >> 16 & 255), linear(rgb >> 8 & 255), linear(rgb & 255));
    }

    /** Fraction passed. */
    public static double pass(double t[], double s[])
    {
        double v = 0D;
        for(int i = 0; i < BINS; i++)
        {
            v += t[i] * s[i];
        }
        return v;
    }

    // Deep sky

    /** Stretch. */
    public static double stretch(double lightYears)
    {
        return lightYears / STRETCH_LY;
    }

    /** Deep-sky spectrum. */
    public static double[] deep(int type, double lightYears)
    {
        double z = stretch(lightYears);
        double s[];
        switch(type)
        {
            case Cosmos.EMISSION:
            case Cosmos.NURSERY:
            case Cosmos.BUBBLE:
                // Emission
                s = scaled(blackbody(20000D, 0D), 0.1D);
                line(s, 656.3D, 0.55D);
                line(s, 486.1D, 0.12D);
                line(s, 500.7D, 0.1D);
                line(s, 658.4D, 0.08D);
                break;
            case Cosmos.PLANETARY:
            case Cosmos.BIPOLAR:
                // Planetary
                s = scaled(blackbody(60000D, 0D), 0.08D);
                line(s, 500.7D, 0.48D);
                line(s, 495.9D, 0.16D);
                line(s, 656.3D, 0.16D);
                line(s, 486.1D, 0.08D);
                break;
            case Cosmos.REMNANT:
                // Remnant
                s = scaled(FLAT, 0.15D);
                line(s, 656.3D, 0.3D);
                line(s, 671.6D, 0.25D);
                line(s, 500.7D, 0.25D);
                break;
            case Cosmos.PULSAR_NEBULA:
                // Wind nebula
                s = scaled(blackbody(40000D, 0D), 0.7D);
                line(s, 656.3D, 0.15D);
                line(s, 500.7D, 0.1D);
                break;
            case Cosmos.REFLECTION:
            {
                // Reflection
                double b[] = blackbody(15000D, 0D);
                s = new double[BINS];
                for(int i = 0; i < BINS; i++)
                {
                    s[i] = b[i] * Math.pow(500D / NM[i], 2D);
                }
                break;
            }
            case Cosmos.SPIRAL:
            case Cosmos.COLLISION:
                // Spiral
                s = scaled(blackbody(4800D, z), 0.6D);
                add(s, blackbody(12000D, z), 0.3D);
                line(s, 656.3D * (1D + z), type == Cosmos.COLLISION ? 0.18D : 0.1D);
                break;
            case Cosmos.IRREGULAR:
                s = scaled(blackbody(8000D, z), 0.72D);
                line(s, 656.3D * (1D + z), 0.28D);
                break;
            case Cosmos.QUASAR:
            {
                // Quasar
                s = new double[BINS];
                for(int i = 0; i < BINS; i++)
                {
                    s[i] = 0.5D / BINS * Math.pow(NM[i] / 500D, -1.5D);
                }
                double w = 1D + z;
                line(s, 121.6D * w, 0.4D);
                line(s, 154.9D * w, 0.18D);
                line(s, 279.8D * w, 0.14D);
                line(s, 486.1D * w, 0.08D);
                line(s, 656.3D * w, 0.22D);
                break;
            }
            case Cosmos.OPEN_CLUSTER:
                s = blackbody(11000D, 0D);
                break;
            case Cosmos.GLOBULAR:
                s = blackbody(5000D, 0D);
                break;
            case Cosmos.DARK:
                s = blackbody(3800D, 0D);
                break;
            default:
                // Old stars
                s = blackbody(type == Cosmos.ELLIPTICAL || type == Cosmos.RADIO_GALAXY || type == Cosmos.GALAXY_CLUSTER ? 4300D : 6000D, z);
                break;
        }
        return norm(s);
    }

    private static void add(double s[], double t[], double k)
    {
        for(int i = 0; i < BINS; i++)
        {
            s[i] += t[i] * k;
        }
    }

    /** Debug table. */
    public static String show(double s[])
    {
        StringBuilder b = new StringBuilder();
        for(double v : s)
        {
            b.append(String.format(java.util.Locale.US, " %.3f", v));
        }
        return b.toString().trim();
    }

    public static double[] copy(double s[])
    {
        return Arrays.copyOf(s, BINS);
    }
}
