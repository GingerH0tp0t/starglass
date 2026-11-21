package com.starglass.sky;

/** Planet light. */
public final class PlanetLight
{
    private final int w;
    /** Column light. */
    private final double col[][];
    /** Mean light. */
    private final double mean;

    public PlanetLight(int map[], int w, int h)
    {
        this.w = w;
        col = new double[w][Spectrum.BINS];
        double rowWeight = 0D;
        for(int y = 0; y < h; y++)
        {
            rowWeight += Math.cos(((y + 0.5D) / h - 0.5D) * Math.PI);
        }
        for(int x = 0; x < w; x++)
        {
            for(int y = 0; y < h; y++)
            {
                int c = map[x + y * w];
                double k = Math.cos(((y + 0.5D) / h - 0.5D) * Math.PI) / rowWeight;
                double r[] = Spectrum.curve(c);
                for(int i = 0; i < Spectrum.BINS; i++)
                {
                    col[x][i] += k * r[i] * Spectrum.SUN[i];
                }
            }
        }
        double sum = 0D, out[] = new double[Spectrum.BINS];
        for(int x = 0; x < w; x++)
        {
            face((double)x / w, out);
            for(double v : out)
            {
                sum += v;
            }
        }
        mean = sum / w;
    }

    /** Visible face. */
    public void visible(double lon, double out[])
    {
        face(lon, out);
        for(int i = 0; i < Spectrum.BINS; i++)
        {
            out[i] /= mean;
        }
    }

    private void face(double lon, double out[])
    {
        java.util.Arrays.fill(out, 0D);
        int half = w / 2;
        double wsum = 0D;
        for(int k = 0; k < half; k++)
        {
            double u = lon + (k + 0.5D) / w;
            int x = Math.floorMod((int)Math.floor(u * w), w);
            double a = ((k + 0.5D) / half - 0.5D) * Math.PI;
            double wgt = Math.cos(a) * Math.cos(a);
            wsum += wgt;
            for(int i = 0; i < Spectrum.BINS; i++)
            {
                out[i] += wgt * col[x][i];
            }
        }
        for(int i = 0; i < Spectrum.BINS; i++)
        {
            out[i] /= wsum;
        }
    }
}
