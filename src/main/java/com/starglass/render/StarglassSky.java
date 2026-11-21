package com.starglass.render;

import com.mojang.blaze3d.ProjectionType;
import com.starglass.Scope;
import com.starglass.Starglass;
import com.starglass.Weather;
import com.starglass.sky.Cosmos;
import com.starglass.sky.CosmosSprites;
import com.starglass.sky.Frames;
import com.starglass.sky.Optics;
import com.starglass.sky.PlanetLight;
import com.starglass.sky.Spectrum;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PerspectiveProjectionMatrixBuffer;
import net.minecraft.client.renderer.state.SkyRenderState;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.level.MoonPhase;
import org.joml.Matrix4f;
import org.joml.Vector3fc;
import com.mojang.blaze3d.systems.RenderSystem;

/** The sky. */
public final class StarglassSky
{
    private static final double DEG = Cosmos.DEG, ARCSEC = Cosmos.ARCSEC;
    /** Eye resolution, arcseconds. */
    private static final double EYE_RESOLUTION = 60D;
    private static final double NAKED_LIMIT = 6.33D;
    private static final int GLOW_GRID = 32;
    /** Glare reach. */
    private static final double GLARE_REACH_COS = Math.cos(Math.toRadians(70D));

    private final SkyDraw draw = new SkyDraw();
    private Cosmos cosmos;
    private Sprites sprites;

    // Per frame
    private int frameNo;
    private long lastNanos;
    private double dt, timeSec, day, starAngle;
    private boolean scoping;
    /** Radio band. */
    private boolean radio;
    private double power, limit, twinkle, resolution;
    private double pxPerTan, shellPerPx, halfW, halfH, halfDiag;
    private final double cs[] = new double[3], us[] = new double[3], rs[] = new double[3];
    private double sxOut, syOut;
    private int drawn;
    private double targetDist;
    private double settle, lastPower = 1D;
    private final double lastCs[] = new double[3];
    private final double tmp[] = new double[3];
    private int moonsSeen;
    private double glareScale;
    private double glareCross, daylightNow, hazeNow;

    // Light meter
    /** Ring radius. */
    public static final double METER_RING = 0.06D;
    /** Meter units. */
    private static final double METER_UNITS = 10000D;
    private double meterR, meterSum;

    // Glass
    private double passAir = 1D, passScatter = 1D, passLamp = 1D, lossSky, skyPass = 1D;
    private final double passStar[] = new double[7];
    /** Glass cost per class. */
    private final double starDelta[] = new double[7];
    private double passComet = 1D, cometDelta;
    private final java.util.Map<Cosmos.Deep, double[]> deepLight = new java.util.IdentityHashMap<>();
    private final java.util.Map<Cosmos.Planet, PlanetLight> planetLight = new java.util.IdentityHashMap<>();
    private final double faceLight[] = new double[Spectrum.BINS];
    private final java.util.Random meterNoise = new java.util.Random();
    /** Meter reading. */
    public double hudMeter = -1D;
    /** Ring radius, arcseconds. */
    public double hudMeterRing;
    private final double moonSky[] = new double[3];
    private final float glowGrid[] = new float[(GLOW_GRID + 1) * (GLOW_GRID + 1) * 4];
    private final double glow[] = new double[4];
    // Screen grid
    private int gnx, gny;
    private double gstep, gx0, gy0;
    private boolean clouded;
    private double cloudTot[] = new double[0], cloudDeck[] = new double[0];
    private final double deckOut[] = new double[1], world[] = new double[3];
    private boolean flashSeen;

    // HUD
    public double hudLimit, hudDaylight, hudPower;
    /** Target cloud. */
    public double hudTargetCloud;
    public boolean hudValid;
    /** Radio last frame. */
    public boolean hudRadio;
    /** Test hook. */
    public int hudDrawn;
    /** Test hook. */
    public double hudDeepVis;
    private double deepVisDist;
    public double lastStarAngle;
    public boolean haveAngle;
    public int lastMoonsSeen;

    public Cosmos cosmos()
    {
        return cosmos;
    }

    public void setCosmos(Cosmos c)
    {
        if(sprites != null)
        {
            sprites.close();
        }
        cosmos = c;
        sprites = new Sprites(c);
    }

    public Sprites sprites()
    {
        return sprites;
    }

    private static double clamp(double v)
    {
        return v < 0D ? 0D : (v > 1D ? 1D : v);
    }

    private static double clamp11(double v)
    {
        return v < -1D ? -1D : (v > 1D ? 1D : v);
    }

    private void toSky(Vector3fc v, double out[])
    {
        tmp[0] = v.x();
        tmp[1] = v.y();
        tmp[2] = v.z();
        Frames.toSky(tmp, starAngle, out);
    }

    /** Render. */
    public void render(Matrix4f view, float sunAngle, float moonAngle, float starAngleIn, MoonPhase phase)
    {
        Minecraft mc = Minecraft.getInstance();
        if(cosmos == null || mc.level == null)
        {
            return;
        }
        Scope scope = Starglass.scope();
        long now = System.nanoTime();
        dt = lastNanos == 0L ? 0D : Math.min(0.1D, (now - lastNanos) / 1.0E9D);
        lastNanos = now;
        timeSec = now / 1.0E9D;
        frameNo++;
        sprites.forget(frameNo, dt);
        starAngle = starAngleIn;
        lastStarAngle = starAngle;
        haveAngle = true;
        float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        day = (mc.level.getDayTime() + (double)partial) / 24000D;
        scoping = scope.active;
        radio = scoping && scope.radio && scope.optics.hasRadio();
        Filter.update(mc, scoping && !radio);
        meterSum = 0D;
        hudValid = false;
        if(!scoping)
        {
            scope.target = null;
        }

        // View
        Camera camera = mc.gameRenderer.getMainCamera();
        toSky(camera.forwardVector(), cs);
        toSky(camera.upVector(), us);
        toSky(camera.leftVector(), rs);
        rs[0] = -rs[0];
        rs[1] = -rs[1];
        rs[2] = -rs[2];
        double fov = Starglass.lastFov > 0D ? Starglass.lastFov : mc.options.fov().get();
        double tanHalf = Math.tan(Math.toRadians(fov) / 2D);
        int fbW = mc.getWindow().getWidth(), fbH = mc.getWindow().getHeight();
        halfW = fbW / 2D;
        halfH = fbH / 2D;
        pxPerTan = halfH / tanHalf;
        shellPerPx = SkyDraw.SHELL / pxPerTan;
        halfDiag = Math.atan(tanHalf * Math.sqrt(1D + (halfW / halfH) * (halfW / halfH)));
        power = Scope.powerOfFov(fov);
        meterR = METER_RING * halfH;

        // Weather
        Weather weather = Starglass.weather();
        weather.update(mc, camera, partial);
        scope.optics.seeingMul = weather.seeingMul;
        double sunY = Math.cos(sunAngle);
        double daylight = clamp((sunY + 0.1D) / 0.35D);
        // Moon glare
        double moonDir[] = new double[3], up[] = { 0D, 1D, 0D };
        Frames.toWorld(up, moonAngle, moonDir);
        double full = 0.5D + 0.5D * Math.cos(phase.index() * Math.PI / 4D);
        glareScale = 2.2D * weather.glareMul * full * clamp(moonDir[1] * 4D);
        Frames.toSky(moonDir, starAngleIn, moonSky);

        Optics optics = scope.optics;
        double reach;
        if(radio)
        {
            reach = optics.radioReach();
            resolution = optics.radioResolution();
            twinkle = 0D;
        } else if(scoping)
        {
            reach = optics.limitingMag(power);
            resolution = optics.resolution();
            twinkle = clamp(optics.seeing() / 4D);
        } else
        {
            reach = NAKED_LIMIT;
            resolution = EYE_RESOLUTION;
            twinkle = 1D;
        }
        limit = reach - 13D * daylight - weather.hazeMag;
        // Eye settling
        double moved = Math.acos(clamp11(cs[0] * lastCs[0] + cs[1] * lastCs[1] + cs[2] * lastCs[2])) / Math.max(1.0E-4D, Math.atan(tanHalf) * 2D);
        double zoomed = Math.abs(Math.log(power / lastPower));
        settle = clamp(settle + dt / Optics.SETTLE_SECONDS - moved * 1.5D - zoomed * 0.6D);
        System.arraycopy(cs, 0, lastCs, 0, 3);
        lastPower = power;
        // Lightning
        if(weather.flash && !radio)
        {
            if(scoping && !flashSeen)
            {
                scope.say("Lightning. Your eye needs a moment again.", 0xc0a060, 50);
            }
            settle = 0D;
            scope.stillSeconds = 0D;
        }
        flashSeen = weather.flash;
        if(scoping)
        {
            limit -= (1D - settle) * Optics.settleDepth(reach);
            if(optics.braced && moved < 0.002D && zoomed < 0.002D)
            {
                scope.stillSeconds += dt;
            } else
            {
                scope.stillSeconds = 0D;
            }
            double averted = clamp((scope.stillSeconds - Optics.AVERTED_SECONDS) / 4D);
            limit += Optics.AVERTED_GAIN * averted;
        }
        if(radio)
        {
            // Radio
            limit = reach;
        }
        hudRadio = radio;
        // HUD limit
        toSky(camera.forwardVector(), tmp);
        glareCross = glare(tmp[0], tmp[1], tmp[2]);
        daylightNow = daylight;
        hazeNow = weather.hazeMag;
        glassSetup(optics);
        hudLimit = limit - extinction(Math.max(-1D, Math.min(1D, camera.forwardVector().y()))) - glareCross;
        hudDaylight = daylight;
        hudPower = power;
        hudValid = true;
        double night = 1D - daylight;
        targetDist = 1.0E9D;
        hudTargetCloud = 0D;
        drawn = 0;
        hudDeepVis = 0D;
        deepVisDist = 0.25D * halfH;
        moonsSeen = 0;
        Object target = null;

        Matrix4f mv = new Matrix4f(view).rotateY((float)(-Math.PI / 2D)).rotateX(starAngleIn);
        draw.setModelView(mv);
        cosmos.place(day);
        layoutGrid();
        clouded = !radio && weather.anyCloud();
        if(clouded)
        {
            sampleCloud(weather);
        }

        if(radio)
        {
            hudLimit = limit;
            drawRadioField();
            target = drawRadio(target);
            draw.end();
            lastMoonsSeen = 0;
            hudDrawn = drawn;
            finishRadioMeter(scope);
            scope.target = target;
            return;
        }
        if(night > 0.02D)
        {
            drawGlow(night);
        }
        if(clouded && night > 0.02D && !over)
        {
            drawDeck(weather, night);
        }
        target = drawDeep(target);
        target = drawComets(target);
        target = drawStars(target);
        if(Starglass.config().meteors && night > 0.3D)
        {
            drawMeteor(night);
        }
        target = drawPlanets(target);
        draw.end();
        lastMoonsSeen = moonsSeen;
        hudDrawn = drawn;
        finishMeter(scope);
        if(scoping)
        {
            scope.target = target;
        }
    }

    // Shader overlay

    /** Sky state. */
    private DimensionType.Skybox skybox = DimensionType.Skybox.NONE;
    private float sunAngle, moonAngle, skyStarAngle;
    private MoonPhase moonPhase = MoonPhase.FULL_MOON;
    private boolean over;
    private final Matrix4f farProj = new Matrix4f();
    private PerspectiveProjectionMatrixBuffer overProjection;

    /** Capture state. */
    public void captureState(SkyRenderState state)
    {
        skybox = state.skybox;
        sunAngle = state.sunAngle;
        moonAngle = state.moonAngle;
        skyStarAngle = state.starAngle;
        moonPhase = state.moonPhase;
    }

    /** Render overlay. */
    public void renderOverlay(Matrix4f projection, Matrix4f view)
    {
        Minecraft mc = Minecraft.getInstance();
        if(mc.level == null || skybox != DimensionType.Skybox.OVERWORLD || !Starglass.replacesStars())
        {
            return;
        }
        Camera camera = mc.gameRenderer.getMainCamera();
        if(camera.getFluidInCamera() != FogType.NONE)
        {
            return;
        }
        if(camera.entity() instanceof LivingEntity e && (e.hasEffect(MobEffects.BLINDNESS) || e.hasEffect(MobEffects.DARKNESS)))
        {
            return;
        }
        if(overProjection == null)
        {
            overProjection = new PerspectiveProjectionMatrixBuffer("starglass sky over shaders");
        }
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(overProjection.getBuffer(SkyDraw.farProjection(projection, farProj)), ProjectionType.PERSPECTIVE);
        over = true;
        draw.setOver(true);
        try
        {
            render(view, sunAngle, moonAngle, skyStarAngle, moonPhase);
        }
        finally
        {
            draw.end();
            draw.setOver(false);
            over = false;
            RenderSystem.restoreProjectionMatrix();
        }
    }

    // Light meter

    /** Meter a source. */
    private void meter(double sx, double sy, double m, double pass, double extent)
    {
        if(!scoping || m > 30D)
        {
            return;
        }
        double d = Math.sqrt(sx * sx + sy * sy);
        if(d - extent > meterR)
        {
            return;
        }
        double frac = extent <= meterR ? 1D : (meterR / extent) * (meterR / extent);
        meterSum += Math.pow(10D, -0.4D * m) * frac * pass;
    }

    /** Finish meter. */
    private void finishMeter(Scope scope)
    {
        if(!scoping)
        {
            hudMeter = -1D;
            return;
        }
        double ringRad = meterR / pxPerTan;
        double ringArcsec = ringRad / ARCSEC;
        hudMeterRing = ringArcsec;
        double part[] = skyParts(scope.optics.lightLoss());
        double area = Math.PI * ringArcsec * ringArcsec;
        double sky = area * Math.pow(10D, -0.4D * 21.6D) * (part[0] * passAir + part[1] * passScatter + part[2] * passLamp);
        double raw = METER_UNITS * (meterSum + sky) * (1D + 0.02D * meterNoise.nextGaussian());
        hudMeter = hudMeter < 0D ? raw : hudMeter + (raw - hudMeter) * Math.min(1D, dt / 0.6D);
    }

    /** Sky glow parts. */
    private double[] skyParts(double lampLoss)
    {
        double t1 = Math.pow(10D, 0.48D * glareCross);
        double t2 = t1 * Math.pow(10D, 6.8D * daylightNow);
        double t3 = t2 * Math.pow(10D, 0.24D * hazeNow);
        double t4 = t3 * Math.pow(10D, 0.6D * lampLoss);
        return new double[] { 1D, t3 - 1D, t4 - t3 };
    }

    private static double loss(double pass)
    {
        return -2.5D * Math.log10(Math.max(1.0E-4D, pass));
    }

    /** Glass setup. */
    private void glassSetup(Optics optics)
    {
        passAir = Filter.pass(Spectrum.AIRGLOW);
        passScatter = Filter.pass(Spectrum.SCATTER);
        passLamp = Filter.pass(Spectrum.LAMP);
        double part[] = skyParts(0D);
        skyPass = (part[0] * passAir + part[1] * passScatter) / (part[0] + part[1]);
        lossSky = loss(skyPass);
        for(int c = 0; c < starDelta.length; c++)
        {
            passStar[c] = Filter.pass(Spectrum.STAR[c]);
            starDelta[c] = loss(passStar[c]) - POINT_SKY * lossSky;
        }
        passComet = Filter.pass(Spectrum.COMET);
        cometDelta = loss(passComet) - PATCH_SKY * lossSky;
        if(Filter.active())
        {
            double lamp = optics.lightLoss();
            double lampThrough = 2.5D * Math.log10(1D + (Math.pow(10D, 0.4D * lamp) - 1D) * passLamp / Math.max(1.0E-4D, skyPass));
            limit += lamp - lampThrough;
        }
    }

    /** Sky weight, point and patch. */
    private static final double POINT_SKY = 0.5D, PATCH_SKY = 1D;

    private double[] deepLight(Cosmos.Deep o)
    {
        return deepLight.computeIfAbsent(o, d -> Spectrum.deep(d.type, d.lightYears));
    }

    private PlanetLight planetLight(Cosmos.Planet p)
    {
        return planetLight.computeIfAbsent(p, q -> new PlanetLight(CosmosSprites.planet(q), CosmosSprites.PLANET_W, CosmosSprites.PLANET_H));
    }

    /** Planet turn, days. */
    public static double planetTurn(Cosmos.Planet p)
    {
        return (0.31D + (p.seed >>> 8 & 255L) / 255D * 0.5D) * (p.a < 1D ? 9D : 1D);
    }

    /** Test hook. */
    public double planetFace(Cosmos.Planet p, double atDay, double glass[])
    {
        double turn = planetTurn(p);
        double out[] = new double[Spectrum.BINS];
        planetLight(p).visible(atDay / turn - Math.floor(atDay / turn), out);
        return Spectrum.pass(glass, out);
    }

    // Projection

    private boolean project(double x, double y, double z)
    {
        double d = x * cs[0] + y * cs[1] + z * cs[2];
        if(d < 0.2D)
        {
            return false;
        }
        sxOut = (x * rs[0] + y * rs[1] + z * rs[2]) / d * pxPerTan;
        syOut = -(x * us[0] + y * us[1] + z * us[2]) / d * pxPerTan;
        return true;
    }

    private boolean inField(double margin)
    {
        return Math.abs(sxOut) <= halfW + margin && Math.abs(syOut) <= halfH + margin;
    }

    /** Crosshair target. */
    private Object offer(Object current, Object o)
    {
        if(!scoping)
        {
            return current;
        }
        double d = Math.sqrt(sxOut * sxOut + syOut * syOut);
        if(!(o instanceof Cosmos.Star))
        {
            d *= 0.5D;
        }
        if(d < Math.min(halfW, halfH) * 0.09D && d < targetDist)
        {
            targetDist = d;
            hudTargetCloud = clouded ? cloudAt(sxOut, syOut) : 0D;
            return o;
        }
        return current;
    }

    /** Airmass. */
    private static double airmass(double sinAlt)
    {
        return 1D / Math.max(sinAlt, 0.06D);
    }

    /** Extinction. */
    private static double extinction(double sinAlt)
    {
        return Math.min(5D, 0.28D * (airmass(sinAlt) - 1D));
    }

    /** Moon glare. */
    private double glare(double x, double y, double z)
    {
        if(glareScale <= 0D)
        {
            return 0D;
        }
        double c = x * moonSky[0] + y * moonSky[1] + z * moonSky[2];
        if(c < GLARE_REACH_COS)
        {
            return 0D;
        }
        double d = Math.acos(clamp11(c)) / DEG;
        return glareScale * Math.exp(-(d * d) / (22D * 22D));
    }

    /** Air and glare. */
    private double skyLoss(double x, double y, double z)
    {
        return extinction(x, y, z) + glare(x, y, z);
    }

    /** Extinction. */
    private double extinction(double x, double y, double z)
    {
        return extinction(Frames.altitude(x, y, z, starAngle));
    }

    /** Horizon loss. */
    private double horizon(double x, double y, double z)
    {
        double s = Frames.altitude(x, y, z, starAngle);
        return clamp((s + 0.02D) / 0.08D);
    }

    /** Dot. */
    private void dot(double x, double y, double z, double px, int rgb, double alpha)
    {
        double h = px * 0.5D * shellPerPx;
        double X = x * SkyDraw.SHELL, Y = y * SkyDraw.SHELL, Z = z * SkyDraw.SHELL;
        int c = SkyDraw.argb(alpha, rgb);
        draw.vertex(X - rs[0] * h - us[0] * h, Y - rs[1] * h - us[1] * h, Z - rs[2] * h - us[2] * h, c);
        draw.vertex(X + rs[0] * h - us[0] * h, Y + rs[1] * h - us[1] * h, Z + rs[2] * h - us[2] * h, c);
        draw.vertex(X + rs[0] * h + us[0] * h, Y + rs[1] * h + us[1] * h, Z + rs[2] * h + us[2] * h, c);
        draw.vertex(X - rs[0] * h + us[0] * h, Y - rs[1] * h + us[1] * h, Z - rs[2] * h + us[2] * h, c);
    }

    /** Offset dot. */
    private void dotOff(double x, double y, double z, double ox, double oy, double px, int rgb, double alpha)
    {
        double sx = ox * shellPerPx / SkyDraw.SHELL, sy = -oy * shellPerPx / SkyDraw.SHELL;
        dot(x + rs[0] * sx + us[0] * sy, y + rs[1] * sx + us[1] * sy, z + rs[2] * sx + us[2] * sy, px, rgb, alpha);
    }

    // Screen grid and cloud

    /** Grid layout. */
    private void layoutGrid()
    {
        if(halfW >= halfH)
        {
            gnx = GLOW_GRID;
            gny = Math.max(2, (int)Math.ceil(GLOW_GRID * halfH / halfW));
        } else
        {
            gny = GLOW_GRID;
            gnx = Math.max(2, (int)Math.ceil(GLOW_GRID * halfW / halfH));
        }
        gstep = Math.max(halfW * 2D / gnx, halfH * 2D / gny);
        gx0 = -gnx * gstep / 2D;
        gy0 = -gny * gstep / 2D;
    }

    /** Sample cloud. */
    private void sampleCloud(Weather weather)
    {
        int n = (gnx + 1) * (gny + 1);
        if(cloudTot.length < n)
        {
            cloudTot = new double[n];
            cloudDeck = new double[n];
        }
        double d[] = new double[3];
        for(int j = 0; j <= gny; j++)
        {
            for(int i = 0; i <= gnx; i++)
            {
                double fx = (gx0 + i * gstep) / pxPerTan, fy = -(gy0 + j * gstep) / pxPerTan;
                d[0] = cs[0] + rs[0] * fx + us[0] * fy;
                d[1] = cs[1] + rs[1] * fx + us[1] * fy;
                d[2] = cs[2] + rs[2] * fx + us[2] * fy;
                double l = Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
                d[0] /= l;
                d[1] /= l;
                d[2] /= l;
                Frames.toWorld(d, starAngle, world);
                int k = i + j * (gnx + 1);
                cloudTot[k] = weather.cloudMag(world[0], world[1], world[2], deckOut);
                cloudDeck[k] = deckOut[0];
            }
        }
    }

    /** Cloud on screen. */
    private double cloudAt(double sx, double sy)
    {
        if(!clouded)
        {
            return 0D;
        }
        double u = (sx - gx0) / gstep, v = (sy - gy0) / gstep;
        u = u < 0D ? 0D : (u > gnx ? gnx : u);
        v = v < 0D ? 0D : (v > gny ? gny : v);
        int i = Math.min(gnx - 1, (int)u), j = Math.min(gny - 1, (int)v);
        double fu = u - i, fv = v - j;
        int a = i + j * (gnx + 1), b = a + 1, c = a + gnx + 1, d = c + 1;
        return (cloudTot[a] * (1D - fu) + cloudTot[b] * fu) * (1D - fv) + (cloudTot[c] * (1D - fu) + cloudTot[d] * fu) * fv;
    }

    /** Cloud anywhere. */
    private double cloudSky(double x, double y, double z)
    {
        tmp[0] = x;
        tmp[1] = y;
        tmp[2] = z;
        Frames.toWorld(tmp, starAngle, world);
        return Starglass.weather().cloudMag(world[0], world[1], world[2], deckOut);
    }

    /** Painted face. */
    private void paintedFace(double d[], double A[], double B[], int n, double inset, double brightness)
    {
        int m = n + 1;
        double px[] = new double[m * m], py[] = new double[m * m], pz[] = new double[m * m];
        int col[] = new int[m * m];
        boolean any = false;
        for(int j = 0; j <= n; j++)
        {
            for(int i = 0; i <= n; i++)
            {
                double fa = ((double)i / n * 2D - 1D), fb = (1D - (double)j / n * 2D);
                double x = d[0] + A[0] * fa + B[0] * fb, y = d[1] + A[1] * fa + B[1] * fb, z = d[2] + A[2] * fa + B[2] * fb;
                double l = Math.sqrt(x * x + y * y + z * z);
                x /= l;
                y /= l;
                z /= l;
                int k = i + j * m;
                px[k] = x * SkyDraw.SHELL;
                py[k] = y * SkyDraw.SHELL;
                pz[k] = z * SkyDraw.SHELL;
                double a = brightness * horizon(x, y, z);
                if(a > 0.001D)
                {
                    a *= Weather.transmit(glare(x, y, z) + (clouded ? cloudSky(x, y, z) : 0D));
                }
                col[k] = SkyDraw.argb(a, 0xffffff);
                any |= a > 0.001D;
            }
        }
        if(!any)
        {
            return;
        }
        double span = 1D - 2D * inset;
        for(int j = 0; j < n; j++)
        {
            for(int i = 0; i < n; i++)
            {
                int c00 = i + j * m, c10 = c00 + 1, c01 = c00 + m, c11 = c01 + 1;
                if((col[c00] | col[c10] | col[c01] | col[c11]) >>> 24 == 0)
                {
                    continue;
                }
                float u0 = (float)(inset + (double)i / n * span), u1 = (float)(inset + (double)(i + 1) / n * span);
                float v0 = (float)(inset + (double)j / n * span), v1 = (float)(inset + (double)(j + 1) / n * span);
                draw.vertex(px[c01], py[c01], pz[c01], u0, v1, col[c01]);
                draw.vertex(px[c11], py[c11], pz[c11], u1, v1, col[c11]);
                draw.vertex(px[c10], py[c10], pz[c10], u1, v0, col[c10]);
                draw.vertex(px[c00], py[c00], pz[c00], u0, v0, col[c00]);
            }
        }
    }

    /** Rain deck. */
    private void drawDeck(Weather weather, double night)
    {
        int rgb = Weather.debugVanilla ? 0xff2020 : weather.cloudColor & 0xffffff;
        double S = SkyDraw.SHELL;
        double pos[] = new double[(gnx + 1) * (gny + 1) * 3];
        float alpha[] = new float[(gnx + 1) * (gny + 1)];
        boolean any = false;
        for(int j = 0; j <= gny; j++)
        {
            for(int i = 0; i <= gnx; i++)
            {
                int k = i + j * (gnx + 1);
                double fx = (gx0 + i * gstep) / pxPerTan, fy = -(gy0 + j * gstep) / pxPerTan;
                double x = cs[0] + rs[0] * fx + us[0] * fy, y = cs[1] + rs[1] * fx + us[1] * fy, z = cs[2] + rs[2] * fx + us[2] * fy;
                double l = Math.sqrt(x * x + y * y + z * z);
                pos[k * 3] = x / l * S;
                pos[k * 3 + 1] = y / l * S;
                pos[k * 3 + 2] = z / l * S;
                // Horizon fade
                double low = clamp(Frames.altitude(x / l, y / l, z / l, starAngle) / 0.2D);
                alpha[k] = (float)(Math.min(0.8D, 1D - Weather.transmit(cloudDeck[k])) * night * low * low * (3D - 2D * low));
                any |= alpha[k] > 0.01F;
            }
        }
        if(!any)
        {
            return;
        }
        draw.begin(SkyDraw.ALPHA_COLOR);
        for(int j = 0; j < gny; j++)
        {
            for(int i = 0; i < gnx; i++)
            {
                int a = i + j * (gnx + 1), b = a + 1, c = b + gnx + 1, d = a + gnx + 1;
                if(alpha[a] + alpha[b] + alpha[c] + alpha[d] <= 0.01F)
                {
                    continue;
                }
                deckVertex(pos, alpha, a, rgb);
                deckVertex(pos, alpha, d, rgb);
                deckVertex(pos, alpha, c, rgb);
                deckVertex(pos, alpha, b, rgb);
            }
        }
        draw.end();
    }

    private void deckVertex(double pos[], float alpha[], int k, int rgb)
    {
        draw.vertex(pos[k * 3], pos[k * 3 + 1], pos[k * 3 + 2], SkyDraw.argb(alpha[k], rgb));
    }

    // Galaxy

    /** Galaxy glow. */
    private void drawGlow(double night)
    {
        double unresolved = (0.3D + 0.7D * clamp((15.5D - limit) / 9.5D)) * night;
        if(unresolved <= 0.01D)
        {
            return;
        }
        double painted = clamp((3.5D - power) / 1.5D);
        if(painted > 0D)
        {
            double d[] = new double[3], A[] = new double[3], B[] = new double[3];
            for(int f = 0; f < 6; f++)
            {
                faceCorner(f, 0D, 0D, d);
                faceCorner(f, 1D, 0D, A);
                faceCorner(f, 0D, -1D, B);
                if(d[0] * cs[0] + d[1] * cs[1] + d[2] * cs[2] < -0.6D)
                {
                    continue;
                }
                SkyTexture face = sprites.face(f);
                if(face == null)
                {
                    continue;
                }
                for(int q = 0; q < 3; q++)
                {
                    A[q] -= d[q];
                    B[q] -= d[q];
                }
                draw.begin(SkyDraw.ADD_TEX, face.view, Sprites.linearClamp());
                paintedFace(d, A, B, 16, 0.5D / CosmosSprites.SKY_FACE, unresolved * painted);
                draw.end();
            }
            unresolved *= 1D - painted;
            if(unresolved <= 0.004D)
            {
                return;
            }
        }
        int nx = gnx, ny = gny;
        double step = gstep, cell = step / pxPerTan;
        double x0 = gx0, y0 = gy0;
        int seed = (int)(cosmos.seed & 0xffff);
        float grid[] = glowGrid.length >= (nx + 1) * (ny + 1) * 4 ? glowGrid : new float[(nx + 1) * (ny + 1) * 4];
        double pos[] = new double[(nx + 1) * (ny + 1) * 3];
        for(int j = 0; j <= ny; j++)
        {
            for(int i = 0; i <= nx; i++)
            {
                double fx = (x0 + i * step) / pxPerTan, fy = -(y0 + j * step) / pxPerTan;
                double x = cs[0] + rs[0] * fx + us[0] * fy, y = cs[1] + rs[1] * fx + us[1] * fy, z = cs[2] + rs[2] * fx + us[2] * fy;
                double l = Math.sqrt(x * x + y * y + z * z);
                x /= l;
                y /= l;
                z /= l;
                CosmosSprites.skyGlow(cosmos, x, y, z, cell, seed, glow);
                int k = i + j * (nx + 1);
                grid[k * 4] = (float)(glow[0] * unresolved * horizon(x, y, z) * (clouded ? Weather.transmit(cloudTot[k]) : 1D)
                    * Weather.transmit(glare(x, y, z)));
                grid[k * 4 + 1] = (float)(glow[1] / 255D);
                grid[k * 4 + 2] = (float)(glow[2] / 255D);
                grid[k * 4 + 3] = (float)(glow[3] / 255D);
                pos[k * 3] = x * SkyDraw.SHELL;
                pos[k * 3 + 1] = y * SkyDraw.SHELL;
                pos[k * 3 + 2] = z * SkyDraw.SHELL;
            }
        }
        draw.begin(SkyDraw.ADD_COLOR);
        for(int j = 0; j < ny; j++)
        {
            for(int i = 0; i < nx; i++)
            {
                int a = i + j * (nx + 1), b = a + 1, c = b + nx + 1, d = a + nx + 1;
                if(grid[a * 4] + grid[b * 4] + grid[c * 4] + grid[d * 4] <= 0.004F)
                {
                    continue;
                }
                corner(grid, pos, a);
                corner(grid, pos, d);
                corner(grid, pos, c);
                corner(grid, pos, b);
            }
        }
        draw.end();
    }

    private void corner(float grid[], double pos[], int k)
    {
        draw.vertex(pos[k * 3], pos[k * 3 + 1], pos[k * 3 + 2], SkyDraw.argb(grid[k * 4], grid[k * 4 + 1], grid[k * 4 + 2], grid[k * 4 + 3]));
    }

    /** Cube corner. */
    static void faceCorner(int face, double u, double v, double out[])
    {
        switch(face)
        {
            case 0: out[0] = 1D; out[1] = v; out[2] = -u; break;
            case 1: out[0] = -1D; out[1] = v; out[2] = u; break;
            case 2: out[0] = u; out[1] = 1D; out[2] = -v; break;
            case 3: out[0] = u; out[1] = -1D; out[2] = v; break;
            case 4: out[0] = u; out[1] = v; out[2] = 1D; break;
            default: out[0] = -u; out[1] = v; out[2] = -1D; break;
        }
    }

    // Stars

    private Object drawStars(Object target)
    {
        draw.begin(SkyDraw.ADD_COLOR);
        Cosmos.Star naked[] = cosmos.naked;
        for(Cosmos.Star s : naked)
        {
            target = star(s, target);
        }
        int kmax = (int)Math.floor((limit - Cosmos.BAND0_MAG) / Cosmos.BAND_WIDTH);
        if(kmax > Cosmos.MAX_BAND)
        {
            kmax = Cosmos.MAX_BAND;
        }
        if(kmax >= 0)
        {
            int deepest = Cosmos.FIRST_LEVEL + kmax;
            double reach[] = new double[deepest + 1];
            for(int l = 0; l <= deepest; l++)
            {
                reach[l] = Math.cos(Math.min(Math.PI, halfDiag * 1.04D + Math.atan(1.4143D / (1 << l)) * 1.06D));
            }
            for(int face = 0; face < 6; face++)
            {
                target = visit(face, 0, 0, 0, deepest, reach, target);
            }
        }
        draw.end();
        return target;
    }

    private Object visit(int face, int level, int i, int j, int deepest, double reach[], Object target)
    {
        int n = 1 << level;
        double t[] = new double[3];
        Cosmos.faceDir(face, -1D + (2D * i + 1D) / n, -1D + (2D * j + 1D) / n, t);
        if(t[0] * cs[0] + t[1] * cs[1] + t[2] * cs[2] < reach[level])
        {
            return target;
        }
        if(level >= Cosmos.FIRST_LEVEL && drawn < 12000)
        {
            for(Cosmos.Star s : cosmos.cell(face, level, i, j))
            {
                target = star(s, target);
            }
        }
        if(level < deepest)
        {
            target = visit(face, level + 1, i * 2, j * 2, deepest, reach, target);
            target = visit(face, level + 1, i * 2 + 1, j * 2, deepest, reach, target);
            target = visit(face, level + 1, i * 2, j * 2 + 1, deepest, reach, target);
            target = visit(face, level + 1, i * 2 + 1, j * 2 + 1, deepest, reach, target);
        }
        return target;
    }

    private Object star(Cosmos.Star s, Object target)
    {
        float mag = s.magAt(day);
        double over = limit - mag - starDelta[s.spectral];
        if(over < 0D || !project(s.x, s.y, s.z) || !inField(4D))
        {
            return target;
        }
        double sinAlt = Frames.altitude(s.x, s.y, s.z, starAngle);
        if((over -= extinction(sinAlt) + glare(s.x, s.y, s.z)) < 0D)
        {
            return target;
        }
        if(clouded && (over -= cloudAt(sxOut, syOut)) < 0D)
        {
            return target;
        }
        double fade = horizon(s.x, s.y, s.z);
        if(fade <= 0D)
        {
            return target;
        }
        drawn++;
        // Twinkle
        double flicker = 1D - 0.4D * twinkle * clamp(0.4D + airmass(sinAlt) * 0.2D) * (0.5D + 0.5D * Math.sin(timeSec * (6D + (s.hash & 7)) + (s.hash >> 3 & 63)));
        int rgb = Cosmos.SPECTRAL_RGB[s.spectral];
        double px = 1D + Math.min(over, 9D) * 0.55D;
        double alpha = (0.3D + over * 0.35D) * flicker * fade;
        double sx = sxOut, sy = syOut;
        // Meter
        double lost = extinction(sinAlt) + hazeNow + (clouded ? cloudAt(sx, sy) : 0D);
        meter(sx, sy, mag + lost - 2.5D * Math.log10(Math.max(0.05D, flicker)), passStar[s.spectral], 0D);
        if(s.kind == Cosmos.DOUBLE)
        {
            meter(sx, sy, s.mag2 + lost, passStar[s.spectral2], 0D);
        }
        if(s.kind == Cosmos.DOUBLE && split(s))
        {
            double off = s.sep * ARCSEC * pxPerTan / 2D;
            double pa = s.pa * DEG;
            double ox = Math.cos(pa) * off, oy = Math.sin(pa) * off;
            dotOff(s.x, s.y, s.z, -ox, -oy, px, rgb, alpha);
            double over2 = over + mag + starDelta[s.spectral] - s.mag2 - starDelta[s.spectral2];
            if(over2 > 0D)
            {
                dotOff(s.x, s.y, s.z, ox, oy, 1D + Math.min(over2, 9D) * 0.55D, Cosmos.SPECTRAL_RGB[s.spectral2],
                    (0.3D + over2 * 0.35D) * flicker * fade);
            }
        } else
        {
            dot(s.x, s.y, s.z, px, rgb, alpha);
        }
        if(over > 4.5D)
        {
            // Glare
            dot(s.x, s.y, s.z, px * 2.6D, rgb, Math.min(0.3D, (over - 4.5D) * 0.05D) * flicker * fade);
        }
        if(s.index >= 0 || over > 1D)
        {
            sxOut = sx;
            syOut = sy;
            target = offer(target, s);
        }
        return target;
    }

    /** Pixels per arcsecond. */
    public double pixels(double arcsec)
    {
        return arcsec * ARCSEC * pxPerTan;
    }

    public double resolution()
    {
        return resolution;
    }

    /** Pair split? */
    public boolean split(Cosmos.Star s)
    {
        return s.sep >= resolution && s.sep * ARCSEC * pxPerTan >= 2.5D;
    }

    // Deep sky

    private Object drawDeep(Object target)
    {
        double deepMag = Starglass.weather().deepMag;
        double d[] = new double[3], RA[] = new double[3], RB[] = new double[3];
        double fieldCos = Math.cos(Math.min(1.5D, halfDiag + 3D * DEG));
        boolean dots = false;
        for(Cosmos.Deep o : cosmos.deep)
        {
            double lim = limit - skyLoss(o.x, o.y, o.z);
            double through = Filter.pass(deepLight(o));
            double om = o.mag + loss(through) - PATCH_SKY * lossSky;
            double vis = clamp((lim - deepMag - om + 0.5D) / 2.5D);
            if(vis <= 0D || o.x * cs[0] + o.y * cs[1] + o.z * cs[2] < fieldCos || !project(o.x, o.y, o.z))
            {
                continue;
            }
            if(clouded && (vis = clamp((lim - deepMag - cloudAt(sxOut, syOut) - om + 0.5D) / 2.5D)) <= 0D)
            {
                continue;
            }
            double ox = sxOut, oy = syOut;
            double half = o.size * ARCSEC / 2D;
            double px = o.size * ARCSEC * pxPerTan;
            // Glass contrast
            double stand = Math.max(0.5D, Math.min(2D, Math.sqrt(through / skyPass)));
            if(!inField(half * pxPerTan))
            {
                continue;
            }
            vis *= horizon(o.x, o.y, o.z);
            double centre = Math.sqrt(sxOut * sxOut + syOut * syOut);
            if(centre < deepVisDist)
            {
                deepVisDist = centre;
                hudDeepVis = vis;
            }
            if(vis <= 0D)
            {
                continue;
            }
            meter(ox, oy, o.mag + deepMag + extinction(Frames.altitude(o.x, o.y, o.z, starAngle)) + hazeNow
                + (clouded ? cloudAt(ox, oy) : 0D), through, px / 2D);
            SkyTexture tex = null;
            double f = 0D;
            if(px >= 4D)
            {
                // Develop
                f = sprites.develop(o, Optics.depthSeconds(o.lightYears), dt, frameNo);
                tex = sprites.level(o, grown(blocksAcross(o), 4D, f));
            }
            if(tex == null)
            {
                if(!dots)
                {
                    draw.begin(SkyDraw.ADD_COLOR);
                    dots = true;
                }
                dot(o.x, o.y, o.z, 2D, Cosmos.isGalaxy(o.type) ? 0xc8d4ff : (Cosmos.isCluster(o.type) ? 0xfff0d0 : 0xff9ab0), Math.min(1D, vis * 0.6D * stand));
            } else
            {
                dots = false;
                d[0] = o.x;
                d[1] = o.y;
                d[2] = o.z;
                Cosmos.spriteAxes(o, RA, RB);
                draw.begin(SkyDraw.ADD_TEX, tex.view, Sprites.nearestClamp());
                draw.skyQuad(d, RA, RB, half, half, 0D, 0D, 1D, 1D, 0D, 0D, 1D, 1D, SkyDraw.argb(Math.min(1D, vis * 0.95D * (0.4D + 0.6D * f) * stand), 0xffffff), true);
                draw.end();
                if(o.pulse > 0F && px >= 24D)
                {
                    draw.begin(SkyDraw.ADD_COLOR);
                    dot(o.x, o.y, o.z, 2.5D, 0xffffff, vis * f * flash(o));
                    draw.end();
                }
            }
            if(px >= 6D)
            {
                sxOut = ox;
                syOut = oy;
                target = offer(target, o);
            }
        }
        draw.end();
        return target;
    }

    static double grown(double blocks, double floor, double f)
    {
        return blocks <= floor ? blocks : floor * Math.pow(blocks / floor, f);
    }

    /** Blocks across. */
    public double blocksAcross(Cosmos.Deep o)
    {
        double size = radio ? o.size * o.radioScale : o.size;
        return Math.min(size * ARCSEC * pxPerTan / 2D, size / resolution);
    }

    // Radio

    /** Radio field. */
    private void drawRadioField()
    {
        double fx = halfW / pxPerTan * 1.25D, fy = halfH / pxPerTan * 1.25D;
        double S = SkyDraw.SHELL;
        int c = SkyDraw.argb(1D, 0x03030a);
        double sx[] = { -fx, fx, fx, -fx }, sy[] = { -fy, -fy, fy, fy };
        draw.begin(SkyDraw.ALPHA_COLOR);
        for(int q = 0; q < 4; q++)
        {
            double x = cs[0] + rs[0] * sx[q] + us[0] * sy[q], y = cs[1] + rs[1] * sx[q] + us[1] * sy[q], z = cs[2] + rs[2] * sx[q] + us[2] * sy[q];
            double l = S / Math.sqrt(x * x + y * y + z * z);
            draw.vertex(x * l, y * l, z * l, c);
        }
        draw.end();
    }

    /** Radio sky. */
    private Object drawRadio(Object target)
    {
        draw.begin(SkyDraw.ADD_COLOR);
        for(Cosmos.Star s : cosmos.naked)
        {
            if(s.mag < 4.2F && project(s.x, s.y, s.z) && inField(2D))
            {
                double fade = horizon(s.x, s.y, s.z);
                if(fade > 0D)
                {
                    dot(s.x, s.y, s.z, 2D, 0x8090b0, 0.3D * fade);
                    drawn++;
                }
            }
        }
        draw.end();
        double d[] = new double[3], RA[] = new double[3], RB[] = new double[3];
        double fieldCos = Math.cos(Math.min(1.5D, halfDiag + 3D * DEG));
        double beam = resolution;
        double beamPx = beam * ARCSEC * pxPerTan;
        for(Cosmos.Deep o : cosmos.deep)
        {
            if(o.radio >= 90F || o.x * cs[0] + o.y * cs[1] + o.z * cs[2] < fieldCos || !project(o.x, o.y, o.z))
            {
                continue;
            }
            double ox = sxOut, oy = syOut;
            double size = o.size * o.radioScale;
            double half = size * ARCSEC / 2D;
            double px = size * ARCSEC * pxPerTan;
            double fade = horizon(o.x, o.y, o.z);
            double pulse = o.pulse > 0F ? flash(o) : 1D;
            // Meter
            if(fade > 0D)
            {
                meter(ox, oy, o.radio - 2.5D * Math.log10(fade * pulse), 1D, Math.max(half * pxPerTan, beamPx / 2D));
            }
            double vis = clamp((limit - o.radio + 0.5D) / 2.5D) * fade;
            if(vis <= 0D || !inField(Math.max(half * pxPerTan, beamPx / 2D)))
            {
                continue;
            }
            d[0] = o.x;
            d[1] = o.y;
            d[2] = o.z;
            SkyTexture map = px >= 4D && size >= beam * 2D ? sprites.level(o.radioView, Math.max(2D, blocksAcross(o))) : null;
            if(map == null)
            {
                // Beam-wide glow
                double blur = Math.max(16D, Math.min(Math.min(halfW, halfH) * 0.5D, beamPx));
                SkyTexture soft = sprites.level(Sprites.BEAM, 32D);
                if(soft != null)
                {
                    Cosmos.axes(d, 1D, 0D, 0D, RA, RB);
                    double h = blur / 2D / pxPerTan, hi = h * 0.45D;
                    draw.begin(SkyDraw.ADD_TEX, soft.view, Sprites.linearClamp());
                    draw.skyQuad(d, RA, RB, h, h, 0D, 0D, 1D, 1D, 0D, 0D, 1D, 1D, SkyDraw.argb(vis * 0.8D * pulse, 0xff6e28), true);
                    draw.skyQuad(d, RA, RB, hi, hi, 0D, 0D, 1D, 1D, 0D, 0D, 1D, 1D, SkyDraw.argb(vis * pulse, 0xffc83c), true);
                    draw.end();
                }
                if(soft == null || beamPx < 16D)
                {
                    // Core dot
                    draw.begin(SkyDraw.ADD_COLOR);
                    dot(o.x, o.y, o.z, 3D, 0xffe0a0, vis * 0.85D * pulse);
                    draw.end();
                }
            } else
            {
                Cosmos.spriteAxes(o, RA, RB);
                draw.begin(SkyDraw.ADD_TEX, map.view, Sprites.nearestClamp());
                draw.skyQuad(d, RA, RB, half, half, 0D, 0D, 1D, 1D, 0D, 0D, 1D, 1D, SkyDraw.argb(vis * 0.95D, 0xffffff), true);
                draw.end();
                if(o.pulse > 0F)
                {
                    draw.begin(SkyDraw.ADD_COLOR);
                    dot(o.x, o.y, o.z, 2.5D, 0xffffdc, vis * pulse);
                    draw.end();
                }
            }
            sxOut = ox;
            syOut = oy;
            target = offer(target, o);
        }
        return target;
    }

    /** Radio meter. */
    private void finishRadioMeter(Scope scope)
    {
        hudMeterRing = meterR / pxPerTan / ARCSEC;
        double floor = Math.pow(10D, -0.4D * (scope.optics.radioReach() + 1D));
        double raw = METER_UNITS * (meterSum + floor) * (1D + 0.02D * meterNoise.nextGaussian());
        hudMeter = hudMeter < 0D ? raw : hudMeter + (raw - hudMeter) * Math.min(1D, dt / 0.6D);
    }

    private double flash(Cosmos.Deep o)
    {
        double c = Math.cos(timeSec / o.pulse * Math.PI * 2D + (o.number & 31));
        return 0.2D + 0.8D * Math.pow(c < 0D ? 0D : c, 6D);
    }

    // Comets

    private Object drawComets(Object target)
    {
        double d[] = new double[3], A[] = new double[3], B[] = new double[3];
        for(Cosmos.Comet c : cosmos.comets)
        {
            double through = (clouded ? cloudSky(c.x, c.y, c.z) : 0D) + skyLoss(c.x, c.y, c.z) + cometDelta;
            if(limit - through < c.mag)
            {
                continue;
            }
            double vis = clamp((limit - through - c.mag) / 2D + 0.15D);
            double spread = clamp(6D / Math.max(power, 1D) + 0.12D);
            draw.begin(SkyDraw.ADD_COLOR);
            tail(c.head, c.dust, 0.003D, 0.03D, 0xfff0c0, vis * 0.5D * spread);
            tail(c.head, c.ion, 0.002D, 0.011D, 0x90c8ff, vis * 0.6D * spread);
            if(!project(c.x, c.y, c.z) || !inField(60D))
            {
                continue;
            }
            double fade = horizon(c.x, c.y, c.z);
            double ox = sxOut, oy = syOut;
            double px = c.coma * ARCSEC * pxPerTan;
            meter(ox, oy, c.mag + extinction(Frames.altitude(c.x, c.y, c.z, starAngle)) + hazeNow + (clouded ? cloudAt(ox, oy) : 0D),
                passComet, px / 2D);
            SkyTexture tex = px >= 6D ? sprites.level(c, Math.min(px / 2D, c.coma / resolution)) : null;
            if(tex == null)
            {
                dot(c.x, c.y, c.z, 6D, 0xb0ffd8, vis * 0.35D * fade);
                dot(c.x, c.y, c.z, 2.5D, 0xe8fff4, vis * fade);
            } else
            {
                d[0] = c.x;
                d[1] = c.y;
                d[2] = c.z;
                Cosmos.axes(d, 1D, 0D, 0D, A, B);
                double half = c.coma * ARCSEC / 2D;
                draw.begin(SkyDraw.ADD_TEX, tex.view, Sprites.nearestClamp());
                draw.skyQuad(d, A, B, half, half, 0D, 0D, 1D, 1D, 0D, 0D, 1D, 1D, SkyDraw.argb(vis * fade, 0xffffff), true);
                draw.end();
            }
            sxOut = ox;
            syOut = oy;
            target = offer(target, c);
        }
        draw.end();
        return target;
    }

    /** Tail. */
    private void tail(double head[], double end[], double w0, double w1, int col, double alpha)
    {
        int segs = 14;
        double dx = end[0] - head[0], dy = end[1] - head[1], dz = end[2] - head[2];
        double lx = 0, ly = 0, lz = 0, rx = 0, ry = 0, rz = 0, pa = 0D;
        boolean have = false;
        for(int i = 0; i <= segs; i++)
        {
            double f = (double)i / segs;
            double x = head[0] + dx * f, y = head[1] + dy * f, z = head[2] + dz * f;
            double l = Math.sqrt(x * x + y * y + z * z);
            double cx = y * dz - z * dy, cy = z * dx - x * dz, cz = x * dy - y * dx;
            double cl = Math.sqrt(cx * cx + cy * cy + cz * cz);
            if(l < 0.05D || cl < 1.0E-9D)
            {
                have = false;
                continue;
            }
            double h = (w0 + (w1 - w0) * f) / cl;
            x /= l;
            y /= l;
            z /= l;
            double ax = x - cx * h, ay = y - cy * h, az = z - cz * h;
            double bx = x + cx * h, by = y + cy * h, bz = z + cz * h;
            double al = SkyDraw.SHELL / Math.sqrt(ax * ax + ay * ay + az * az), bl = SkyDraw.SHELL / Math.sqrt(bx * bx + by * by + bz * bz);
            double a = alpha * Math.pow(1D - f, 1.5D) * horizon(x, y, z);
            if(have)
            {
                draw.vertex(lx, ly, lz, SkyDraw.argb(pa, col));
                draw.vertex(rx, ry, rz, SkyDraw.argb(pa, col));
                draw.vertex(bx * bl, by * bl, bz * bl, SkyDraw.argb(a, col));
                draw.vertex(ax * al, ay * al, az * al, SkyDraw.argb(a, col));
            }
            lx = ax * al;
            ly = ay * al;
            lz = az * al;
            rx = bx * bl;
            ry = by * bl;
            rz = bz * bl;
            pa = a;
            have = true;
        }
    }

    // Planets

    private Object drawPlanets(Object target)
    {
        double d[] = new double[3], A[] = new double[3], B[] = new double[3];
        for(Cosmos.Planet p : cosmos.planets)
        {
            // Face and glass
            double turn = planetTurn(p);
            double lon = day / turn - Math.floor(day / turn);
            planetLight(p).visible(lon, faceLight);
            double faceTotal = 0D;
            for(double v : faceLight)
            {
                faceTotal += v;
            }
            double through = Filter.pass(faceLight);
            double pmag = p.mag + loss(through / faceTotal) - POINT_SKY * lossSky - 2.5D * Math.log10(faceTotal);
            double lim = limit - skyLoss(p.x, p.y, p.z);
            if(lim < pmag || !project(p.x, p.y, p.z) || !inField(40D))
            {
                continue;
            }
            // Cloud
            double plimit = clouded ? lim - cloudAt(sxOut, syOut) : lim;
            if(plimit < pmag)
            {
                continue;
            }
            double fade = horizon(p.x, p.y, p.z);
            double vis = clamp((plimit - pmag) / 1.5D + 0.2D) * fade;
            if(vis <= 0D)
            {
                continue;
            }
            double ox = sxOut, oy = syOut;
            double half = p.diameter * ARCSEC / 2D;
            double px = p.diameter * ARCSEC * pxPerTan;
            meter(ox, oy, p.mag + extinction(Frames.altitude(p.x, p.y, p.z, starAngle)) + hazeNow + (clouded ? cloudAt(ox, oy) : 0D),
                through, px / 2D);
            double blocks = Math.min(px / 2D, p.diameter / resolution);
            // Develop
            blocks = grown(blocks, 2D, sprites.develop(p, 0.5D + 0.4D * p.distance, dt, frameNo));
            SkyTexture map = px >= 3D ? sprites.level(p, blocks) : null;
            SkyTexture limb = map != null ? sprites.level("limb", blocks) : null;
            if(map == null || limb == null)
            {
                draw.begin(SkyDraw.ALPHA_COLOR);
                dot(p.x, p.y, p.z, 1.5D + Math.min(plimit - p.mag, 9D) * 0.55D, p.color, vis);
                draw.end();
                target = offer(target, p);
                continue;
            }
            d[0] = p.x;
            d[1] = p.y;
            d[2] = p.z;
            // Axes
            Cosmos.axes(d, 1D, 0D, 0D, A, B);
            boolean rings = p.type == Cosmos.RINGED && blocks >= 8D;
            double rw = half * CosmosSprites.RING_SPAN, rh = rw * Math.sin(p.ringTilt * DEG);
            int white = SkyDraw.argb(vis, 0xffffff);
            SkyTexture ring = rings ? sprites.level(p.moons, 64D) : null;
            if(ring != null)
            {
                // Far rings
                draw.begin(SkyDraw.ALPHA_TEX, ring.view, Sprites.nearestClamp());
                draw.skyQuad(d, A, B, rw, rh, 0D, 0D, 1D, 0.5D, 0D, 0D, 1D, 0.5D, white, true);
                draw.end();
            }
            draw.begin(SkyDraw.ALPHA_TEX, map.view, Sprites.nearestRepeatU());
            draw.skyQuad(d, A, B, half, half, 0D, 0D, 1D, 1D, lon, 0D, lon + 0.5D, 1D, white, true);
            draw.end();
            draw.begin(SkyDraw.ALPHA_TEX, limb.view, Sprites.nearestClamp());
            draw.skyQuad(d, A, B, half, half, 0D, 0D, 1D, 1D, 0D, 0D, 1D, 1D, white, true);
            draw.end();
            if(p.lit < 0.985D)
            {
                // Night side
                boolean sunOnA = -d[0] * A[0] + (1D - d[1]) * A[1] - d[2] * A[2] > 0D;
                double dark = 1D - p.lit;
                draw.begin(SkyDraw.ALPHA_COLOR);
                int shade = SkyDraw.argb(0.9D * vis, 0x000005);
                if(sunOnA)
                {
                    draw.skyQuad(d, A, B, half, half, 0D, 0D, dark, 1D, 0D, 0D, 0D, 0D, shade, false);
                } else
                {
                    draw.skyQuad(d, A, B, half, half, 1D - dark, 0D, 1D, 1D, 0D, 0D, 0D, 0D, shade, false);
                }
                draw.end();
            }
            if(ring != null)
            {
                draw.begin(SkyDraw.ALPHA_TEX, ring.view, Sprites.nearestClamp());
                draw.skyQuad(d, A, B, rw, rh, 0D, 0.5D, 1D, 1D, 0D, 0.5D, 1D, 1D, white, true);
                draw.end();
            }
            if(px >= 6D)
            {
                draw.begin(SkyDraw.ALPHA_COLOR);
                for(int m = 0; m < p.moons.length; m++)
                {
                    Cosmos.Moon moon = p.moons[m];
                    double mm = p.mag - 5D * Math.log10(moon.size);
                    double ph = moon.phase + day / moon.period * Math.PI * 2D;
                    double mo = moon.orbit * Math.cos(ph), behind = Math.sin(ph);
                    if(plimit < mm || (behind > 0D && Math.abs(mo) < 1D))
                    {
                        continue;
                    }
                    double fa = mo * half, fb = moon.orbit * behind * 0.07D * half;
                    double mx = d[0] + A[0] * fa + B[0] * fb, my = d[1] + A[1] * fa + B[1] * fb, mz = d[2] + A[2] * fa + B[2] * fb;
                    double ml = Math.sqrt(mx * mx + my * my + mz * mz);
                    dot(mx / ml, my / ml, mz / ml, Math.max(1.5D, moon.size * px), moon.color, clamp((plimit - mm) / 1.5D + 0.3D) * fade);
                    moonsSeen |= 1 << m;
                }
                draw.end();
            }
            sxOut = ox;
            syOut = oy;
            target = offer(target, p);
        }
        return target;
    }

    // Meteors

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
        return (double)(h & 0xffffff) / 16777216D;
    }

    static final int METEOR_SLOT = 100;

    public static boolean shower(long seed, long day)
    {
        return (mix(seed * 31L + day * 7919L) & 0x7fffffff) % 7 == 0;
    }

    /** Meteor. */
    private void drawMeteor(double night)
    {
        Minecraft mc = Minecraft.getInstance();
        double ticks = day * 24000D;
        long slot = (long)Math.floor(ticks / METEOR_SLOT);
        long dayN = (long)Math.floor(ticks / 24000D);
        boolean shower = shower(cosmos.seed, dayN);
        int h = mix(cosmos.seed * 131L + slot * 104729L);
        if(unit(h) > (shower ? 0.85D : 0.2D))
        {
            return;
        }
        double start = unit(mix(h + 1)) * 60D, life = 9D + unit(mix(h + 2)) * 9D;
        double p = (ticks - slot * METEOR_SLOT - start) / life;
        if(p < 0D || p > 1D)
        {
            return;
        }
        double az = unit(mix(h + 3)) * Math.PI * 2D, alt = (25D + unit(mix(h + 4)) * 55D) * DEG;
        double s[] = { Math.cos(alt) * Math.cos(az), Math.sin(alt), Math.cos(alt) * Math.sin(az) };
        double dir[] = new double[3], A[] = new double[3], B[] = new double[3];
        double heading = unit(mix(h + 5)) * Math.PI * 2D;
        if(shower)
        {
            int dh = mix(cosmos.seed * 17L + dayN);
            double raz = unit(dh) * Math.PI * 2D, ralt = (35D + unit(mix(dh + 1)) * 40D) * DEG;
            double rx = Math.cos(ralt) * Math.cos(raz), ry = Math.sin(ralt), rz = Math.cos(ralt) * Math.sin(raz);
            Cosmos.axes(s, s[0] - rx, s[1] - ry, s[2] - rz, A, B);
            heading = Math.PI * 0.5D;
        } else
        {
            Cosmos.axes(s, 1D, 0D, 0D, A, B);
        }
        for(int q = 0; q < 3; q++)
        {
            dir[q] = A[q] * Math.cos(heading) + B[q] * Math.sin(heading);
        }
        double arc = (10D + unit(mix(h + 6)) * 16D) * DEG;
        double headAt = p * arc, tailAt = Math.max(0D, headAt - 7D * DEG);
        double hd[] = new double[3], td[] = new double[3], w[] = new double[3];
        for(int q = 0; q < 3; q++)
        {
            w[q] = s[q] * Math.cos(headAt) + dir[q] * Math.sin(headAt);
            td[q] = s[q] * Math.cos(tailAt) + dir[q] * Math.sin(tailAt);
        }
        Frames.toSky(w, starAngle, hd);
        Frames.toSky(td, starAngle, w);
        double cx = hd[1] * w[2] - hd[2] * w[1], cy = hd[2] * w[0] - hd[0] * w[2], cz = hd[0] * w[1] - hd[1] * w[0];
        double cl = Math.sqrt(cx * cx + cy * cy + cz * cz);
        if(cl < 1.0E-9D)
        {
            return;
        }
        double half = Math.max(0.16D / cl, 0.75D * shellPerPx / cl);
        double a = night * Math.sin(Math.min(1D, p * 1.4D + 0.25D) * Math.PI) * 0.95D;
        if(clouded)
        {
            a *= Weather.transmit(cloudSky(hd[0], hd[1], hd[2]));
        }
        double S = SkyDraw.SHELL;
        draw.begin(SkyDraw.ADD_COLOR);
        int head = SkyDraw.argb(a, 0xfff7db), tail = SkyDraw.argb(0D, 0xb3d9ff);
        draw.vertex(hd[0] * S - cx * half, hd[1] * S - cy * half, hd[2] * S - cz * half, head);
        draw.vertex(hd[0] * S + cx * half, hd[1] * S + cy * half, hd[2] * S + cz * half, head);
        draw.vertex(w[0] * S + cx * half, w[1] * S + cy * half, w[2] * S + cz * half, tail);
        draw.vertex(w[0] * S - cx * half, w[1] * S - cy * half, w[2] * S - cz * half, tail);
        draw.end();
    }
}
