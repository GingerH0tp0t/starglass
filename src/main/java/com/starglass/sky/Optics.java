package com.starglass.sky;

/** Optics. */
public final class Optics
{
    /** Aperture, mm. */
    public static final double APERTURE = 60D;
    /** Default power. */
    public static final double DEFAULT_POWER = 8D;
    public static final double MIN_POWER = 2D;

    public double aperture = APERTURE;
    public boolean braced;
    public int altitude = 64;
    public int strayLight;
    /** Seeing multiplier. */
    public double seeingMul = 1D;

    /** Seeing. */
    public double seeing()
    {
        double t = (altitude - 64) / 192D;
        t = t < 0D ? 0D : (t > 1D ? 1D : t);
        double air = (3.6D - 2.4D * t) * seeingMul;
        return braced ? air : Math.max(9D, air);
    }

    /** Resolution. */
    public double resolution()
    {
        return Math.max(116D / aperture, seeing());
    }

    public double maxPower()
    {
        return Math.min(2D * aperture, 240D / resolution());
    }

    /** Absolute max power. */
    public double absoluteMaxPower()
    {
        return Math.min(2D * aperture, 240D * aperture / 116D);
    }

    public double lightLoss()
    {
        return 0.22D * strayLight;
    }

    /** Limiting magnitude. */
    public double limitingMag(double power)
    {
        return 2.1D + 5D * Math.log10(Math.min(aperture, 7D * Math.max(power, 1D))) - lightLoss();
    }

    /** Shake. */
    public double shake()
    {
        return braced ? 0.012D : 0.05D;
    }

    /** Settle depth. */
    public static double settleDepth(double reach)
    {
        double beyond = (reach - 6.33D) * 0.35D;
        return beyond < 0D ? 0D : (beyond > 1.6D ? 1.6D : beyond);
    }

    /** Averted vision gain. */
    public static final double AVERTED_GAIN = 0.7D;
    public static final double SETTLE_SECONDS = 3.5D, AVERTED_SECONDS = 12D;

    // Radio

    /** Radio array. */
    public int dishes;
    public double baseline;

    public static final int DISH_FULL = 32;

    public boolean hasRadio()
    {
        return dishes > 0;
    }

    /** Radio reach. */
    public double radioReach()
    {
        int n = Math.min(dishes, DISH_FULL);
        return n <= 0 ? -99D : 5D + 4D * Math.log10(n);
    }

    /** Radio resolution. */
    public double radioResolution()
    {
        return 2400D / (1D + 2D * baseline);
    }

    /** Radio max power. */
    public double radioMaxPower()
    {
        double p = 1400D / radioResolution();
        return p < 2D ? 2D : (p > 1000D ? 1000D : p);
    }

    /** Focus seconds. */
    public static double depthSeconds(double lightYears)
    {
        return 1.2D + 2.3D * Math.log10(Math.max(lightYears, 300D) / 300D);
    }
}
