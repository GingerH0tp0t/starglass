package com.starglass.sky;

/** Sky and world frames. */
public final class Frames
{
    private Frames()
    {
    }

    /** Sky to world. */
    public static void toWorld(double v[], double angle, double out[])
    {
        double c = Math.cos(angle), s = Math.sin(angle);
        double y1 = v[1] * c - v[2] * s;
        double z1 = v[1] * s + v[2] * c;
        double x1 = v[0];
        out[0] = -z1;
        out[1] = y1;
        out[2] = x1;
    }

    public static void toSky(double w[], double angle, double out[])
    {
        double c = Math.cos(angle), s = Math.sin(angle);
        double x = w[2];
        double y = w[1] * c - w[0] * s;
        double z = -w[1] * s - w[0] * c;
        out[0] = x;
        out[1] = y;
        out[2] = z;
    }

    /** Altitude sine. */
    public static double altitude(double x, double y, double z, double angle)
    {
        return y * Math.cos(angle) - z * Math.sin(angle);
    }

    /** Look vector. */
    public static void look(double yawDeg, double pitchDeg, double out[])
    {
        double yaw = Math.toRadians(yawDeg), pitch = Math.toRadians(pitchDeg);
        double cp = Math.cos(pitch);
        out[0] = -Math.sin(yaw) * cp;
        out[1] = -Math.sin(pitch);
        out[2] = Math.cos(yaw) * cp;
    }

    /** Yaw of a vector. */
    public static float yawOf(double v[])
    {
        return (float)Math.toDegrees(Math.atan2(-v[0], v[2]));
    }

    public static float pitchOf(double v[])
    {
        double y = v[1] < -1D ? -1D : (v[1] > 1D ? 1D : v[1]);
        return (float)Math.toDegrees(-Math.asin(y));
    }
}
