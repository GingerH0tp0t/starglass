package com.starglass.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.starglass.sky.Cosmos;
import com.starglass.sky.CosmosSprites;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** GPU pictures. */
public final class Sprites
{
    public static final class Sprite
    {
        public volatile int mips[][];
        SkyTexture tex[];
        public int w, h, span;
        /** Seen seconds. */
        public double seen;
        int stamp;
        boolean queued;
    }

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Starglass painter");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });

    private final Map<Object, Sprite> sprites = new HashMap<>();
    private final Cosmos cosmos;
    private final SkyTexture faces[] = new SkyTexture[6];
    private volatile int faceData[][];
    private boolean facesQueued;

    public Sprites(Cosmos cosmos)
    {
        this.cosmos = cosmos;
    }

    public static GpuSampler nearestClamp()
    {
        return RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
    }

    public static GpuSampler nearestRepeatU()
    {
        return RenderSystem.getSamplerCache().getSampler(AddressMode.REPEAT, AddressMode.CLAMP_TO_EDGE, FilterMode.NEAREST, FilterMode.NEAREST, false);
    }

    public static GpuSampler linearClamp()
    {
        return RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
    }

    public Sprite get(Object key)
    {
        Sprite s = sprites.get(key);
        if(s != null)
        {
            return s;
        }
        s = new Sprite();
        if(key instanceof Cosmos.Deep || key instanceof Cosmos.RadioView)
        {
            s.w = s.h = s.span = CosmosSprites.DEEP_SIZE;
        } else if(BEAM.equals(key))
        {
            s.w = s.h = s.span = BEAM_SIZE;
        } else if(key instanceof Cosmos.Planet)
        {
            s.w = CosmosSprites.PLANET_W;
            s.h = CosmosSprites.PLANET_H;
            s.span = s.w / 2;
        } else if(key instanceof Cosmos.Comet)
        {
            s.w = s.h = s.span = CosmosSprites.COMA_SIZE;
        } else if("limb".equals(key))
        {
            s.w = s.h = s.span = CosmosSprites.LIMB_SIZE;
        } else
        {
            s.w = s.h = s.span = CosmosSprites.RING_SIZE;
        }
        sprites.put(key, s);
        return s;
    }

    private static int[][] paint(Cosmos cosmos, Object key, Sprite s)
    {
        if(key instanceof Cosmos.Deep)
        {
            return CosmosSprites.mips(CosmosSprites.deep((Cosmos.Deep)key), s.w);
        } else if(key instanceof Cosmos.RadioView)
        {
            return CosmosSprites.mips(CosmosSprites.radio(((Cosmos.RadioView)key).of), s.w);
        } else if(BEAM.equals(key))
        {
            return new int[][] { beam(s.w) };
        } else if(key instanceof Cosmos.Planet)
        {
            return CosmosSprites.mips(CosmosSprites.planet((Cosmos.Planet)key), s.w, s.h);
        } else if(key instanceof Cosmos.Comet)
        {
            return CosmosSprites.mips(CosmosSprites.coma(((Cosmos.Comet)key).seed), s.w);
        } else if("limb".equals(key))
        {
            return CosmosSprites.mips(CosmosSprites.limb(), s.w);
        }
        return new int[][] { CosmosSprites.ring(cosmos.planets[Cosmos.RINGED]) };
    }

    /** Radio beam key. */
    public static final String BEAM = "beam";
    private static final int BEAM_SIZE = 32;

    /** Beam picture. */
    private static int[] beam(int n)
    {
        int px[] = new int[n * n];
        for(int y = 0; y < n; y++)
        {
            for(int x = 0; x < n; x++)
            {
                double u = (x + 0.5D) / n * 2D - 1D, v = (y + 0.5D) / n * 2D - 1D;
                double r2 = u * u + v * v;
                double a = r2 >= 1D ? 0D : Math.exp(-r2 * 2.5D) * (1D - r2);
                px[x + y * n] = (int)Math.round(a * 255D) << 24 | 0xffffff;
            }
        }
        return px;
    }

    /** Painted? */
    public boolean ready(Object key)
    {
        Sprite s = get(key);
        if(s.mips != null)
        {
            return true;
        }
        if(!s.queued)
        {
            s.queued = true;
            final Cosmos c = cosmos;
            WORKER.execute(() -> {
                try
                {
                    s.mips = paint(c, key, s);
                }
                catch(Throwable t)
                {
                    com.starglass.Starglass.LOG.error("Painting {} failed", key, t);
                }
            });
        }
        return false;
    }

    /** Mip level. */
    public SkyTexture level(Object key, double blocks)
    {
        Sprite s = get(key);
        if(!ready(key))
        {
            return null;
        }
        int mips[][] = s.mips;
        if(s.tex == null)
        {
            s.tex = new SkyTexture[mips.length];
        }
        int level = 0, n = s.span;
        while(level < mips.length - 1 && n > blocks)
        {
            n >>= 1;
            level++;
        }
        if(s.tex[level] == null)
        {
            int w = Math.max(1, s.w >> level), h = Math.max(1, s.h >> level);
            s.tex[level] = new SkyTexture("sprite", mips[level], w, h);
        }
        return s.tex[level];
    }

    /** Develop. */
    public double develop(Object key, double seconds, double dt, int frame)
    {
        Sprite s = get(key);
        if(s.stamp != frame)
        {
            s.stamp = frame;
            s.seen = Math.min(seconds, s.seen + dt);
        }
        double f = s.seen / seconds;
        f = f < 0D ? 0D : (f > 1D ? 1D : f);
        return f * f * (3D - 2D * f);
    }

    public double developed(Object key, double seconds)
    {
        Sprite s = sprites.get(key);
        double f = s == null ? 0D : Math.min(1D, s.seen / seconds);
        return f * f * (3D - 2D * f);
    }

    /** Forget. */
    public void forget(int frame, double dt)
    {
        for(Sprite s : sprites.values())
        {
            if(s.stamp != frame)
            {
                s.seen = Math.max(0D, s.seen - dt * 0.5D);
            }
        }
    }

    /** Sky face. */
    public SkyTexture face(int f)
    {
        if(faces[f] != null)
        {
            return faces[f];
        }
        int data[][] = faceData;
        if(data == null)
        {
            if(!facesQueued)
            {
                facesQueued = true;
                final Cosmos c = cosmos;
                WORKER.execute(() -> {
                    int out[][] = new int[6][];
                    for(int i = 0; i < 6; i++)
                    {
                        out[i] = CosmosSprites.skyFace(c, i);
                    }
                    faceData = out;
                });
            }
            return null;
        }
        faces[f] = new SkyTexture("sky_face_" + f, data[f], CosmosSprites.SKY_FACE, CosmosSprites.SKY_FACE);
        return faces[f];
    }

    public void close()
    {
        for(Sprite s : sprites.values())
        {
            if(s.tex != null)
            {
                for(SkyTexture t : s.tex)
                {
                    if(t != null)
                    {
                        t.close();
                    }
                }
            }
        }
        sprites.clear();
        for(int i = 0; i < 6; i++)
        {
            if(faces[i] != null)
            {
                faces[i].close();
                faces[i] = null;
            }
        }
    }
}
