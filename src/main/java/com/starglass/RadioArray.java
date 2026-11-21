package com.starglass;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.Level;

/** Radio array sites. */
public final class RadioArray
{
    /** One site. */
    public record Site(String name, int x, int y, int z, int dishes, double baseline)
    {
    }

    /** Reach, across and vertical. */
    public static final int REACH = 48, DEPTH = 16;

    /* Sites: name, x, y, z, dishes, baseline. */
    public static final List<Site> SITES = new ArrayList<>(List.of(
        new Site("Midoriheiki Radio-Telescope", -3149, 138, 271, 32, 96D)
    ));

    private RadioArray()
    {
    }

    private static boolean overworld(Minecraft mc)
    {
        return mc.level != null && mc.level.dimension() == Level.OVERWORLD;
    }

    /** Site in reach. */
    public static Site at(Minecraft mc)
    {
        LocalPlayer p = mc.player;
        if(p == null || !overworld(mc))
        {
            return null;
        }
        Site best = null;
        double bestD = Double.MAX_VALUE;
        for(Site s : SITES)
        {
            double dx = p.getX() - (s.x() + 0.5D), dz = p.getZ() - (s.z() + 0.5D), dy = p.getY() - s.y();
            double d = dx * dx + dz * dz;
            if(d <= (double)REACH * REACH && Math.abs(dy) <= DEPTH && d < bestD)
            {
                best = s;
                bestD = d;
            }
        }
        return best;
    }

    /** Nearest site. */
    public static Site nearest(Minecraft mc)
    {
        LocalPlayer p = mc.player;
        if(p == null || !overworld(mc))
        {
            return null;
        }
        Site best = null;
        double bestD = Double.MAX_VALUE;
        for(Site s : SITES)
        {
            double dx = p.getX() - (s.x() + 0.5D), dz = p.getZ() - (s.z() + 0.5D);
            double d = dx * dx + dz * dz;
            if(d < bestD)
            {
                best = s;
                bestD = d;
            }
        }
        return best;
    }
}
