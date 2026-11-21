package com.starglass.hud;

import com.starglass.Reading;
import com.starglass.Scope;
import com.starglass.Starglass;
import com.starglass.Weather;
import com.starglass.render.Filter;
import com.starglass.render.StarglassSky;
import com.starglass.sky.Optics;
import java.util.Locale;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Vector3fc;

/** Eyepiece HUD. */
public final class ScopeHud
{
    static final int PANEL = 96;
    static final int LINE = 10;

    /** Reticle steps, arcseconds. */
    private static final double STEPS[] = { 36000D, 18000D, 7200D, 3600D, 1800D, 1200D, 600D, 300D, 120D, 60D, 30D, 10D, 5D, 2D, 1D };
    /** Minimum tick spacing. */
    private static final double MIN_TICK = 7D;
    private static final int RETICLE = 0x90ff5a40, RING = 0x70ffd080;

    private ScopeHud()
    {
    }

    public static void render(GuiGraphics g, DeltaTracker delta)
    {
        Minecraft mc = Minecraft.getInstance();
        Scope scope = Starglass.scope();
        StarglassSky sky = Starglass.sky();
        if(!scope.active || mc.options.hideGui || sky.cosmos() == null || mc.level == null)
        {
            return;
        }
        Font font = mc.font;
        int w = g.guiWidth(), h = g.guiHeight();
        double scale = mc.getWindow().getWidth() / (double)w;
        int cx = w / 2, cy = h / 2;

        // Reticle
        double perArcsec = sky.pixels(1D) / scale;
        double step = STEPS[0];
        for(double s : STEPS)
        {
            if(s * perArcsec >= MIN_TICK)
            {
                step = s;
            }
        }
        double spacing = step * perArcsec;
        int reach = (int)(Math.min(w, h) * 0.42D);
        int gap = 3;
        g.fill(cx - reach, cy, cx - gap, cy + 1, RETICLE);
        g.fill(cx + gap + 1, cy, cx + reach + 1, cy + 1, RETICLE);
        g.fill(cx, cy - reach, cx + 1, cy - gap, RETICLE);
        g.fill(cx, cy + gap + 1, cx + 1, cy + reach + 1, RETICLE);
        for(int i = 1; i * spacing <= reach; i++)
        {
            int o = (int)Math.round(i * spacing);
            int len = i % 5 == 0 ? 4 : 2;
            g.fill(cx - o, cy - len, cx - o + 1, cy + len + 1, RETICLE);
            g.fill(cx + o, cy - len, cx + o + 1, cy + len + 1, RETICLE);
            g.fill(cx - len, cy - o, cx + len + 1, cy - o + 1, RETICLE);
            g.fill(cx - len, cy + o, cx + len + 1, cy + o + 1, RETICLE);
        }

        // Meter ring
        double ring = StarglassSky.METER_RING * h / 2D;
        int dots = Math.max(24, (int)(ring * 1.6D));
        for(int i = 0; i < dots; i++)
        {
            double a = i * Math.PI * 2D / dots;
            int x = cx + (int)Math.round(Math.cos(a) * ring), y = cy + (int)Math.round(Math.sin(a) * ring);
            g.fill(x, y, x + 1, y + 1, RING);
        }

        Panel.Layout at = new Panel.Layout(w, h, scale, PANEL);

        // Instrument panel
        Optics o = scope.optics;
        Vector3fc f = mc.gameRenderer.getMainCamera().forwardVector();
        int places = Reading.places(sky.hudPower);
        long time = mc.level.getDayTime();
        Panel left = new Panel(font, PANEL);
        boolean radio = sky.hudRadio && scope.site != null;
        if(radio)
        {
            left.title(scope.site.name(), Panel.TITLE);
            left.line("Power", Reading.power(sky.hudPower));
        } else
        {
            left.title("Spyglass", Panel.TITLE);
            left.line("Power", Reading.power(sky.hudPower));
            left.line("Held", o.braced ? "braced" : "in hand");
        }
        left.rule();
        left.line("Bearing", num(Reading.bearing(f.x(), f.z()), places) + "°");
        left.line("Altitude", num(Reading.altitude(f.y()), places) + "°");
        left.line("Day " + Reading.day(time), Reading.clock(time));
        left.rule();
        left.line("Division", angle(step));
        left.line("Ring", angle(sky.hudMeterRing * 2D) + " wide");
        left.line("Meter", Reading.meter(sky.hudMeter));
        if(!radio)
        {
            left.line("Glass", Filter.name() != null ? Filter.name() : "none");
        }
        if(o.strayLight > 2 && !radio)
        {
            left.line("Lamplight", "nearby");
        }
        String hint = null;
        int hintColor = Panel.WARN;
        Weather wx = Starglass.weather();
        if(radio)
        {
            if(scope.tracking)
            {
                hint = "Clock drive: following the sky.";
                hintColor = Panel.HINT;
            }
        } else if(sky.hudValid && sky.hudDaylight > 0.6D)
        {
            hint = "Daylight. Little shows against it.";
        } else if(scope.cappedTicks > 0)
        {
            hint = o.braced ? "As far as it goes here. Higher ground has steadier air."
                : "The glass shakes in the hand. Crouch and stand still to steady it.";
        } else if(wx.valid() && wx.inClouds)
        {
            hint = "You are inside the cloud.";
        } else if(wx.valid() && wx.cover > 0.6D)
        {
            hint = "Cloud. Watch the gaps as they drift past, or climb above it.";
        } else if(wx.valid() && wx.aboveClouds && wx.rain > 0.2D)
        {
            hint = "Above the weather.";
            hintColor = Panel.HINT;
        } else if(scope.stillSeconds >= Optics.AVERTED_SECONDS)
        {
            hint = "Looking a little to one side: the faintest things show.";
            hintColor = Panel.HINT;
        } else if(scope.tracking)
        {
            hint = "Clock drive: following the sky.";
            hintColor = Panel.HINT;
        }
        if(hint != null)
        {
            left.rule();
            left.text(hint, hintColor);
        }
        float leftY = at.top();
        left.draw(g, at.left(PANEL), leftY, at.s);

        if(scope.toastTicks > 0 && scope.toast != null)
        {
            int a = Math.min(255, scope.toastTicks * 12);
            g.drawCenteredString(font, scope.toast, cx, cy + Math.min(h / 2 - 40, 60), a << 24 | scope.toastColor & 0xffffff);
        }

        // Keys
        Panel keys = new Panel(font, KEYS_W);
        keys.line("Power", "Scroll");
        keys.line("Clock drive", keyName("key.starglass.track"));
        keys.line("Record", keyName("key.starglass.record"));
        keys.line("Notebook", keyName("key.starglass.notebook"));
        if(scope.site != null)
        {
            keys.line(scope.radio ? "Light" : "Radio", keyName("key.starglass.radio"));
        }
        keys.draw(g, at.left(PANEL), at.fits ? at.bottom(keys) : at.under(left, leftY), at.s);
    }

    private static final int KEYS_W = 92;

    private static String keyName(String id)
    {
        for(net.minecraft.client.KeyMapping k : Minecraft.getInstance().options.keyMappings)
        {
            if(k.getName().equals(id))
            {
                return k.getTranslatedKeyMessage().getString();
            }
        }
        return "?";
    }

    // Text

    public static String num(double v, int places)
    {
        return String.format(Locale.US, "%." + places + "f", v);
    }

    /** Angle text. */
    public static String angle(double arcsec)
    {
        if(arcsec >= 3600D)
        {
            return trim(arcsec / 3600D) + "°";
        }
        if(arcsec >= 60D)
        {
            return trim(arcsec / 60D) + "'";
        }
        return trim(arcsec) + "\"";
    }

    private static String trim(double v)
    {
        return Math.abs(v - Math.rint(v)) < 0.05D ? String.valueOf((long)Math.rint(v)) : num(v, 1);
    }
}
