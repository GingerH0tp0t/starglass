package com.starglass;

import com.starglass.render.Filter;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.joml.Vector3fc;

/** One reading. */
public final class Reading
{
    public long dayTime;
    public double bearing, altitude, power, meter = -1D;
    public boolean braced;
    /** Glass name. */
    public String filter;
    /** Site. */
    public boolean hasSite;
    public int x, y, z;

    /** Reading now. */
    public static Reading now(Minecraft mc)
    {
        Reading r = new Reading();
        Vector3fc f = mc.gameRenderer.getMainCamera().forwardVector();
        r.bearing = bearing(f.x(), f.z());
        r.altitude = altitude(f.y());
        r.dayTime = mc.level == null ? 0L : mc.level.getDayTime();
        r.power = Starglass.sky().hudPower;
        r.braced = Starglass.scope().optics.braced;
        r.filter = Starglass.sky().hudRadio ? "radio" : Filter.name();
        r.meter = Starglass.sky().hudMeter;
        if(mc.player != null)
        {
            BlockPos at = mc.player.blockPosition();
            r.hasSite = true;
            r.x = at.getX();
            r.y = at.getY();
            r.z = at.getZ();
        }
        return r;
    }

    public static double bearing(double x, double z)
    {
        double b = Math.toDegrees(Math.atan2(x, -z));
        return b < 0D ? b + 360D : (b >= 360D ? b - 360D : b);
    }

    public static double altitude(double y)
    {
        return Math.toDegrees(Math.asin(y < -1D ? -1D : (y > 1D ? 1D : y)));
    }

    /** Decimal places. */
    public static int places(double power)
    {
        return power < 10D ? 1 : 2;
    }

    /** Day number. */
    public static long day(long dayTime)
    {
        return Math.floorDiv(dayTime, 24000L);
    }

    /** Clock. */
    public static String clock(long dayTime)
    {
        long t = Math.floorMod(dayTime, 24000L);
        long minutes = (t * 1440L / 24000L + 360L) % 1440L;
        return String.format(Locale.US, "%02d:%02d", minutes / 60L, minutes % 60L);
    }

    /** Meter text. */
    public static String meter(double v)
    {
        if(v < 0D)
        {
            return "-";
        }
        if(v >= 100D)
        {
            return String.format(Locale.US, "%.0f", v);
        }
        if(v >= 10D)
        {
            return String.format(Locale.US, "%.1f", v);
        }
        if(v >= 1D)
        {
            return String.format(Locale.US, "%.2f", v);
        }
        return String.format(Locale.US, "%.3f", v);
    }

    public static String power(double p)
    {
        return String.format(Locale.US, p < 10D ? "%.1fx" : "%.0fx", p);
    }

    private String deg(double v)
    {
        return String.format(Locale.US, "%." + places(power) + "f", v);
    }

    /** Book lines. */
    public String[] bookLines(int number)
    {
        String kit = power(power) + (filter != null ? " " + filter : "");
        return new String[] {
            "#" + number + " D" + day(dayTime) + " " + clock(dayTime),
            fit("At " + site(false), "At " + site(true), site(true)),
            "Az " + deg(bearing) + " Alt " + deg(altitude),
            fit(kit + " M " + meter(meter), kit + " M " + meterShort(meter), kit + " M" + meterShort(meter))
        };
    }

    /** Book page width, pixels. */
    public static final int PAGE_WIDTH = 114;

    /** First form that fits. */
    private static String fit(String... forms)
    {
        for(String f : forms)
        {
            if(width(f) <= PAGE_WIDTH)
            {
                return f;
            }
        }
        return forms[forms.length - 1];
    }

    /** Site text. */
    public String site(boolean shortForm)
    {
        if(!hasSite)
        {
            return "-";
        }
        return shortForm ? coord(x) + " " + y + " " + coord(z) : x + " " + y + " " + z;
    }

    private static String coord(int v)
    {
        return Math.abs(v) < 100000 ? String.valueOf(v) : Math.round(v / 1000D) + "k";
    }

    /** Short meter text. */
    private static String meterShort(double v)
    {
        return v < 1.0E6D ? meter(v) : String.format(Locale.US, "%.2fe%d", v / Math.pow(10D, Math.floor(Math.log10(v))),
            (int)Math.floor(Math.log10(v)));
    }

    /** Line width, pixels. */
    public static int width(String s)
    {
        Minecraft mc = Minecraft.getInstance();
        if(mc != null && mc.font != null)
        {
            return mc.font.width(s);
        }
        int w = 0;
        for(int i = 0; i < s.length(); i++)
        {
            char c = s.charAt(i);
            w += c == ' ' || c == 't' || c == 'i' || c == 'l' ? 4 : 6;
        }
        return w;
    }

    /** Full line. */
    public String plain(int number)
    {
        return "#" + number + "  day " + day(dayTime) + " " + clock(dayTime) + " (tick " + dayTime + ")  at "
            + (hasSite ? x + " " + y + " " + z : "-") + "  bearing " + deg(bearing)
            + "  altitude " + deg(altitude) + "  power " + power(power) + (braced ? " braced" : " in hand")
            + "  glass " + (filter != null ? filter : "none") + "  meter " + meter(meter);
    }

    // Notebook file

    String toTsv(int number)
    {
        return number + "\t" + dayTime + "\t" + bearing + "\t" + altitude + "\t" + power + "\t" + braced + "\t"
            + (filter == null ? "-" : filter) + "\t" + meter
            + (hasSite ? "\t" + x + "\t" + y + "\t" + z : "\t-\t-\t-");
    }

    /** Parse a line. */
    static Reading fromTsv(String f[])
    {
        if(f.length < 8)
        {
            return null;
        }
        try
        {
            Reading r = new Reading();
            r.dayTime = Long.parseLong(f[1]);
            r.bearing = Double.parseDouble(f[2]);
            r.altitude = Double.parseDouble(f[3]);
            r.power = Double.parseDouble(f[4]);
            r.braced = Boolean.parseBoolean(f[5]);
            r.filter = "-".equals(f[6]) ? null : f[6];
            r.meter = Double.parseDouble(f[7]);
            if(f.length >= 11 && !"-".equals(f[8]))
            {
                r.x = Integer.parseInt(f[8]);
                r.y = Integer.parseInt(f[9]);
                r.z = Integer.parseInt(f[10]);
                r.hasSite = true;
            }
            return r;
        }
        catch(NumberFormatException e)
        {
            return null;
        }
    }
}
