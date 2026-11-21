package com.starglass.render;

import com.starglass.sky.Spectrum;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TintedGlassBlock;

/** Off-hand glass. */
public final class Filter
{
    /** Tinted glass. */
    private static final float TINTED = 0.12F;

    private static boolean active;
    private static float r = 1F, g = 1F, b = 1F;
    private static String name;
    private static final double CLEAR[] = new double[Spectrum.BINS];
    private static double curve[] = CLEAR;

    static
    {
        Arrays.fill(CLEAR, 1D);
    }

    private Filter()
    {
    }

    /** Update. */
    public static void update(Minecraft mc, boolean scoping)
    {
        active = false;
        name = null;
        r = g = b = 1F;
        curve = CLEAR;
        if(!scoping || mc.player == null)
        {
            return;
        }
        ItemStack held = mc.player.getOffhandItem();
        if(!(held.getItem() instanceof BlockItem item))
        {
            return;
        }
        Block block = item.getBlock();
        if(block instanceof BeaconBeamBlock glass)
        {
            DyeColor dye = glass.getColor();
            int rgb = dye.getTextureDiffuseColor();
            r = (rgb >> 16 & 255) / 255F;
            g = (rgb >> 8 & 255) / 255F;
            b = (rgb & 255) / 255F;
            name = shortName(dye);
            curve = curve(dye);
            active = true;
        } else if(block instanceof TintedGlassBlock)
        {
            r = g = b = TINTED;
            curve = new double[Spectrum.BINS];
            Arrays.fill(curve, TINTED);
            name = "tinted";
            active = true;
        }
    }

    public static boolean active()
    {
        return active;
    }

    /** Glass name. */
    public static String name()
    {
        return name;
    }

    /** Tint a colour. */
    public static int apply(int argb)
    {
        if(!active)
        {
            return argb;
        }
        int cr = (int)((argb >> 16 & 255) * r), cg = (int)((argb >> 8 & 255) * g), cb = (int)((argb & 255) * b);
        return argb & 0xff000000 | cr << 16 | cg << 8 | cb;
    }

    /** Dye curve. */
    public static double[] curve(DyeColor dye)
    {
        return Spectrum.glass(dye.getTextureDiffuseColor());
    }

    /** Held curve. */
    public static double[] curve()
    {
        return curve;
    }

    /** Fraction passed. */
    public static double pass(double spectrum[])
    {
        return active ? Spectrum.pass(curve, spectrum) : 1D;
    }

    private static String shortName(DyeColor dye)
    {
        return switch(dye)
        {
            case LIGHT_BLUE -> "ltblue";
            case LIGHT_GRAY -> "ltgray";
            default -> dye.getName();
        };
    }
}
