package com.starglass;

import com.starglass.sky.Frames;
import com.starglass.sky.Optics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;

/** The spyglass. */
public final class Scope
{
    private static final double TAN30 = Math.tan(Math.toRadians(30D));

    public final Optics optics = new Optics();
    /** Wanted and actual power. */
    public double wanted = Optics.DEFAULT_POWER;
    public double power = Optics.DEFAULT_POWER;
    /** Drawn power. */
    public double shownPower = 1D;
    public boolean active;
    public boolean tracking;
    /** Radio band. */
    public boolean radio;
    /** Site in reach. */
    public RadioArray.Site site;
    /** Still seconds. */
    public double stillSeconds;
    /** Capped flag. */
    public int cappedTicks;

    /** Renderer target. */
    public Object target;
    public String toast;
    public int toastTicks;
    public int toastColor = 0xffffff;

    private final double trackSky[] = new double[3];
    private boolean haveTrack;
    private long lastNanos;

    public static boolean isScoping(Minecraft mc)
    {
        LocalPlayer p = mc.player;
        return p != null && p.isScoping() && mc.options.getCameraType().isFirstPerson();
    }

    /** Field of view. */
    public double fovDegrees()
    {
        return Math.toDegrees(2D * Math.atan(TAN30 / shownPower));
    }

    public static double powerOfFov(double fovDegrees)
    {
        return TAN30 / Math.tan(Math.toRadians(fovDegrees) / 2D);
    }

    public void scroll(double clicks)
    {
        double next = wanted * Math.pow(1.25D, clicks);
        double top = radio ? optics.radioMaxPower() : optics.absoluteMaxPower();
        wanted = next < Optics.MIN_POWER ? Optics.MIN_POWER : (next > top ? top : next);
        if(clicks > 0D && wanted > maxPower() + 0.01D)
        {
            cappedTicks = 50;
        }
    }

    /** Max power. */
    public double maxPower()
    {
        return radio ? optics.radioMaxPower() : optics.maxPower();
    }

    /** Band key. */
    public void toggleRadio(Minecraft mc)
    {
        if(radio)
        {
            radio = false;
            wanted = Math.min(wanted, optics.absoluteMaxPower());
            say("Light", 0x9ab0d0, 50);
            return;
        }
        if(site == null)
        {
            say("No radio array in reach", 0xc0a060, 60);
            return;
        }
        radio = true;
        say("Radio: " + site.name(), 0xff9a5a, 70);
    }

    /** Tick. */
    public void tick(Minecraft mc)
    {
        boolean now = isScoping(mc);
        if(now && !active)
        {
            shownPower = Math.max(1D, powerOfFov(mc.options.fov().get()));
            stillSeconds = 0D;
        }
        active = now;
        if(!active)
        {
            tracking = false;
            haveTrack = false;
            target = null;
        }
        LocalPlayer p = mc.player;
        if(p != null && mc.level != null)
        {
            boolean still = p.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4D;
            optics.braced = p.isCrouching() && still && p.onGround();
            BlockPos eye = BlockPos.containing(p.getX(), p.getEyeY(), p.getZ());
            optics.altitude = eye.getY();
            optics.strayLight = mc.level.getBrightness(LightLayer.BLOCK, eye);
        }
        site = RadioArray.at(mc);
        optics.dishes = site == null ? 0 : Math.max(1, Math.min(site.dishes(), Optics.DISH_FULL));
        optics.baseline = site == null ? 0D : Math.max(0D, site.baseline());
        if(radio && site == null)
        {
            radio = false;
            wanted = Math.min(wanted, optics.absoluteMaxPower());
            if(active)
            {
                say("Light", 0x9ab0d0, 50);
            }
        }
        power = Math.min(wanted, maxPower());
        if(cappedTicks > 0)
        {
            cappedTicks--;
        }
        if(toastTicks > 0)
        {
            toastTicks--;
        }
    }

    public void say(String s, int color, int ticks)
    {
        toast = s;
        toastColor = color;
        toastTicks = ticks;
    }

    /** Frame. */
    public void frame(Minecraft mc, double starAngle)
    {
        long t = System.nanoTime();
        double dt = lastNanos == 0L ? 0D : Math.min(0.1D, (t - lastNanos) / 1.0E9D);
        lastNanos = t;
        if(!active)
        {
            return;
        }
        shownPower += (power - shownPower) * Math.min(1D, dt * 9D);
        LocalPlayer p = mc.player;
        if(p == null)
        {
            return;
        }
        if(!tracking)
        {
            haveTrack = false;
            return;
        }
        double w[] = new double[3], out[] = new double[3];
        if(haveTrack)
        {
            Frames.look(p.getYRot(), p.getXRot(), w);
            Frames.toSky(w, lastAngle, trackSky);
            Frames.toWorld(trackSky, starAngle, out);
            float yaw = Frames.yawOf(out), pitch = Frames.pitchOf(out);
            float dy = yaw - p.getYRot();
            dy -= 360F * Math.round(dy / 360F);
            float ny = p.getYRot() + dy;
            p.setYRot(ny);
            p.yRotO = ny;
            p.setXRot(pitch);
            p.xRotO = pitch;
        }
        haveTrack = true;
        lastAngle = starAngle;
    }

    private double lastAngle;

    public void setTracking(boolean on)
    {
        tracking = on;
        haveTrack = false;
    }
}
