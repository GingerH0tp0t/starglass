package com.starglass;

import com.mojang.blaze3d.platform.NativeImage;
import com.starglass.mixin.ClientLevelAccessor;
import java.io.InputStream;
import net.minecraft.client.Camera;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;

/** Weather. */
public final class Weather
{
    private static final Identifier CLOUDS = Identifier.withDefaultNamespace("textures/environment/clouds.png");
    /** Deck noise scale. */
    private static final double DECK_SCALE = 150D;
    /** Deck depth. */
    private static final double DECK_MAG = 8D;
    /** Deck minimum rise. */
    private static final double DECK_MIN_RISE = 64D;
    /** Vanilla cloud depth. */
    private static final double VANILLA_MAG = 2.2D;
    /** Washed-air ticks. */
    private static final double WASH_TICKS = 9000D;

    // Tonight
    public double nightSteady, nightHaze;
    // Now
    public double rain, thunder, washed, cover;
    public boolean dry, snow, aboveClouds, inClouds;
    /** Haze and steadiness. */
    public double haze, steady;
    /** Haze losses. */
    public double hazeMag, deepMag;
    /** Seeing multiplier. */
    public double seeingMul = 1D;
    /** Glare multiplier. */
    public double glareMul = 1D;
    public boolean flash;
    public int cloudColor = 0xffffffff;
    /** Test hook. */
    public static boolean debugVanilla;

    private boolean valid;
    private double camX, camY, camZ, cloudY, cloudRange, windX, windZ, vanillaX, vanillaZ;
    private boolean vanillaClouds;
    private long seed = 1L;
    private long rainEnd = Long.MIN_VALUE;
    private boolean wasRaining;
    private boolean cells[];
    private int cellsW, cellsH;
    private boolean triedCells;

    public boolean valid()
    {
        return valid;
    }

    public void setSeed(long seed)
    {
        this.seed = seed;
    }

    /** Update. */
    public void update(Minecraft mc, Camera camera, float partial)
    {
        ClientLevel level = mc.level;
        if(level == null)
        {
            valid = false;
            return;
        }
        long time = level.getGameTime();
        long night = Math.floorDiv(level.getDayTime(), 24000L);
        nightSteady = unit(mix(seed * 0x9e3779b97f4a7c15L + night * 31L + 1L));
        nightHaze = clamp(0.62D * unit(mix(seed * 0x632be59bd9b4e019L + night * 37L + 2L)) + 0.42D * nightSteady - 0.12D);

        Vec3 cam = camera.position();
        camX = cam.x;
        camY = cam.y;
        camZ = cam.z;
        BlockPos at = BlockPos.containing(cam);
        Biome.Precipitation p = level.getBiome(at).value().getPrecipitationAt(at, level.getSeaLevel());
        dry = p == Biome.Precipitation.NONE;
        snow = p == Biome.Precipitation.SNOW;

        double r = level.getRainLevel(partial), t = level.getThunderLevel(partial);
        if(r > 0.3D)
        {
            wasRaining = true;
        } else if(wasRaining && r < 0.05D)
        {
            wasRaining = false;
            rainEnd = time;
        }
        washed = rainEnd == Long.MIN_VALUE || time < rainEnd ? 0D : clamp(1D - (time - rainEnd) / WASH_TICKS);

        cloudY = camera.attributeProbe().getValue(EnvironmentAttributes.CLOUD_HEIGHT, partial);
        cloudColor = camera.attributeProbe().getValue(EnvironmentAttributes.CLOUD_COLOR, partial);
        aboveClouds = camY > cloudY + 4D;
        inClouds = !aboveClouds && camY >= cloudY;
        // Dry biome
        rain = dry ? r * 0.3D : r;
        thunder = dry ? 0D : t;
        // Cover
        cover = aboveClouds ? 0D : clamp(rain * (snow ? 0.6D : 0.55D) + thunder * 0.3D);

        double low = clamp((camY - 90D) / 100D);
        haze = nightHaze * (1D - washed) + (aboveClouds ? 0.15D : 0.4D) * rain + (dry ? 0.35D * r : 0D);
        haze = clamp(haze * (1D - 0.55D * low) * (aboveClouds ? 0.5D : 1D));
        double churn = aboveClouds ? 0D : 1D;
        steady = clamp(nightSteady * (1D - 0.55D * washed) * (1D - 0.45D * rain * churn) - 0.3D * thunder * churn);
        hazeMag = 1.5D * Math.pow(haze, 1.3D) - 0.35D * washed;
        deepMag = 0.7D * haze;
        seeingMul = 0.6D + 1.25D * (1D - steady);
        glareMul = 1D + 1.8D * haze;
        flash = ((ClientLevelAccessor)level).starglass$skyFlashTime() > 0;

        // Wind
        windX = time * 0.15D + partial * 0.15D;
        windZ = time * 0.04D;
        CloudStatus status = mc.options.getCloudsType();
        vanillaClouds = status != CloudStatus.OFF && ARGB.alpha(cloudColor) > 0 && cells();
        if(vanillaClouds)
        {
            double o = (time % (cellsW * 400L)) + partial;
            vanillaX = camX + o * 0.030000001D;
            vanillaZ = camZ + 3.96D;
            cloudRange = mc.options.cloudRange().get() * 16D;
        }
        valid = true;
    }

    /** Any cloud? */
    public boolean anyCloud()
    {
        return valid && (cover > 0.01D || vanillaClouds || inClouds);
    }

    /** Cloud loss. */
    public double cloudMag(double dx, double dy, double dz, double deck[])
    {
        deck[0] = 0D;
        if(!valid)
        {
            return 0D;
        }
        if(inClouds)
        {
            deck[0] = DECK_MAG * cover;
            return deck[0] + VANILLA_MAG;
        }
        if(aboveClouds || dy <= 0D)
        {
            return 0D;
        }
        double up = Math.max(dy, 0.035D);
        double t = Math.max(cloudY - camY, DECK_MIN_RISE) / up;
        double hx = dx * t, hz = dz * t;
        double total = 0D;
        if(cover > 0.01D)
        {
            double n = fbm((camX + hx + windX) / DECK_SCALE, (camZ + hz + windZ) / DECK_SCALE);
            double bar = 0.8D - 0.42D * cover;
            double thick = smooth((n - bar) / 0.12D);
            double slant = Math.min(3D, 0.45D / up);
            deck[0] = DECK_MAG * thick * (0.6D + 0.4D * slant) + 0.8D * cover * slant;
            total = deck[0];
        }
        if(vanillaClouds && hx * hx + hz * hz <= cloudRange * cloudRange)
        {
            int cx = Math.floorMod((int)Math.floor((vanillaX + hx) / 12D), cellsW);
            int cz = Math.floorMod((int)Math.floor((vanillaZ + hz) / 12D), cellsH);
            if(cells[cx + cz * cellsW])
            {
                total += VANILLA_MAG;
                if(debugVanilla)
                {
                    deck[0] += VANILLA_MAG;
                }
            }
        }
        return total;
    }

    /** Transmission. */
    public static double transmit(double mag)
    {
        return Math.pow(10D, -0.4D * mag);
    }

    // Words

    public String skyWord()
    {
        if(!valid)
        {
            return "-";
        }
        if(inClouds)
        {
            return "in cloud";
        }
        if(aboveClouds && rain > 0.2D)
        {
            return "above cloud";
        }
        if(thunder > 0.5D)
        {
            return "storm";
        }
        if(cover > 0.7D)
        {
            return "overcast";
        }
        if(cover > 0.4D)
        {
            return "broken cloud";
        }
        if(cover > 0.08D)
        {
            return "clouding";
        }
        if(washed > 0.3D)
        {
            return "rain-washed";
        }
        return haze > 0.6D ? "murky" : (haze > 0.38D ? "hazy" : (haze > 0.14D ? "clear" : "crisp"));
    }

    public String airWord()
    {
        return !valid ? "-" : (steady > 0.75D ? "steady" : (steady > 0.45D ? "fair" : (steady > 0.2D ? "unsteady" : "boiling")));
    }

    // Vanilla clouds

    /** Cloud cells. */
    private boolean cells()
    {
        if(triedCells)
        {
            return cells != null;
        }
        triedCells = true;
        try(InputStream in = Minecraft.getInstance().getResourceManager().open(CLOUDS); NativeImage img = NativeImage.read(in))
        {
            cellsW = img.getWidth();
            cellsH = img.getHeight();
            cells = new boolean[cellsW * cellsH];
            for(int z = 0; z < cellsH; z++)
            {
                for(int x = 0; x < cellsW; x++)
                {
                    cells[x + z * cellsW] = ARGB.alpha(img.getPixel(x, z)) >= 10;
                }
            }
        }
        catch(Exception e)
        {
            Starglass.LOG.warn("Starglass could not read the cloud texture; vanilla clouds will not hide stars", e);
            cells = null;
        }
        return cells != null;
    }

    // Noise

    private static double fbm(double x, double z)
    {
        return 0.55D * value(x, z, 11) + 0.3D * value(x * 2.13D + 5.2D, z * 2.13D - 1.7D, 23) + 0.15D * value(x * 4.41D - 3.1D, z * 4.41D + 7.9D, 37);
    }

    private static double value(double x, double z, int salt)
    {
        int ix = (int)Math.floor(x), iz = (int)Math.floor(z);
        double fx = x - ix, fz = z - iz;
        fx = fx * fx * (3D - 2D * fx);
        fz = fz * fz * (3D - 2D * fz);
        double a = corner(ix, iz, salt), b = corner(ix + 1, iz, salt), c = corner(ix, iz + 1, salt), d = corner(ix + 1, iz + 1, salt);
        return a + (b - a) * fx + (c - a) * fz + (a - b - c + d) * fx * fz;
    }

    private static double corner(int x, int z, int salt)
    {
        return unit(mix(x * 0x9e3779b97f4a7c15L ^ z * 0xc2b2ae3d27d4eb4fL ^ salt));
    }

    private static double smooth(double v)
    {
        v = clamp(v + 0.5D);
        return v * v * (3D - 2D * v);
    }

    private static int mix(long v)
    {
        v ^= v >>> 33;
        v *= 0xff51afd7ed558ccdL;
        v ^= v >>> 33;
        v *= 0xc4ceb9fe1a85ec53L;
        v ^= v >>> 33;
        return (int)v;
    }

    private static double unit(int h)
    {
        return (h & 0xffffff) / 16777216D;
    }

    private static double clamp(double v)
    {
        return v < 0D ? 0D : (v > 1D ? 1D : v);
    }
}
