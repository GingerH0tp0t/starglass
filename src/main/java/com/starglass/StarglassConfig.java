package com.starglass;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Config. */
public final class StarglassConfig
{
    /** Sky seed. */
    public static final long SEED = 10842L;

    public final long seed = SEED;
    public boolean nakedEye = true;
    public boolean meteors = true;
    public boolean shaderSky = true;

    public static StarglassConfig load(Path file)
    {
        StarglassConfig c = new StarglassConfig();
        Properties p = new Properties();
        try
        {
            if(Files.exists(file))
            {
                try(Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8))
                {
                    p.load(in);
                }
            }
            c.nakedEye = Boolean.parseBoolean(p.getProperty("nakedEye", Boolean.toString(c.nakedEye)).trim());
            c.meteors = Boolean.parseBoolean(p.getProperty("meteors", Boolean.toString(c.meteors)).trim());
            c.shaderSky = Boolean.parseBoolean(p.getProperty("shaderSky", Boolean.toString(c.shaderSky)).trim());
        }
        catch(Exception e)
        {
            Starglass.LOG.warn("Bad starglass_observer.properties, using defaults", e);
        }
        try
        {
            p.remove("seed");
            p.setProperty("nakedEye", Boolean.toString(c.nakedEye));
            p.setProperty("meteors", Boolean.toString(c.meteors));
            p.setProperty("shaderSky", Boolean.toString(c.shaderSky));
            Files.createDirectories(file.getParent());
            try(Writer out = Files.newBufferedWriter(file, StandardCharsets.UTF_8))
            {
                p.store(out, "Starglass: Observer. nakedEye = the sky without the spyglass too; shaderSky = draw the sky over shader packs."
                    + " The sky itself is fixed: every observer sees the same one.");
            }
        }
        catch(Exception e)
        {
            Starglass.LOG.warn("Could not write starglass_observer.properties", e);
        }
        return c;
    }
}
