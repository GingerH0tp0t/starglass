package com.starglass.render;

import com.starglass.Starglass;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.fabricmc.loader.api.FabricLoader;

/** Shader pack detection. */
public final class Shaders
{
    private static final MethodHandle IN_USE = find();
    /** Force overlay. */
    private static final boolean FORCE = Boolean.getBoolean("starglass.forceOverlay");

    private Shaders()
    {
    }

    private static MethodHandle find()
    {
        if(!FabricLoader.getInstance().isModLoaded("iris"))
        {
            return null;
        }
        try
        {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            MethodHandles.Lookup l = MethodHandles.publicLookup();
            Object instance = l.findStatic(api, "getInstance", MethodType.methodType(api)).invoke();
            MethodHandle h = l.findVirtual(api, "isShaderPackInUse", MethodType.methodType(boolean.class)).bindTo(instance);
            Starglass.LOG.info("Starglass: Iris found; the sky will be drawn over shader packs");
            return h;
        }
        catch(Throwable t)
        {
            Starglass.LOG.warn("Starglass: Iris is loaded but its API could not be reached; the sky may not show under shader packs", t);
            return null;
        }
    }

    /** Pack active? */
    public static boolean active()
    {
        if(IN_USE == null)
        {
            return false;
        }
        try
        {
            return (boolean)IN_USE.invokeExact();
        }
        catch(Throwable t)
        {
            return false;
        }
    }

    /** Overlay path? */
    public static boolean overlay()
    {
        return Starglass.config() != null && Starglass.config().shaderSky && (FORCE || active());
    }
}
