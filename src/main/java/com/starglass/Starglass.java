package com.starglass;

import com.mojang.blaze3d.platform.InputConstants;
import com.starglass.hud.ScopeHud;
import com.starglass.render.StarglassSky;
import com.starglass.screen.NotebookScreen;
import com.starglass.sky.Cosmos;
import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Starglass: Observer. */
public final class Starglass implements ClientModInitializer
{
    public static final String MOD_ID = "starglass_observer";
    public static final Logger LOG = LoggerFactory.getLogger("Starglass");

    private static StarglassConfig config;
    private static final Scope SCOPE = new Scope();
    private static final StarglassSky SKY = new StarglassSky();
    private static final Weather WEATHER = new Weather();
    private static Notebook notebook;
    /** Last world FOV. */
    public static volatile double lastFov;

    private static KeyMapping keyTrack, keyRecord, keyNotebook, keyRadio;

    @Override
    public void onInitializeClient()
    {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        config = StarglassConfig.load(FabricLoader.getInstance().getConfigDir().resolve("starglass_observer.properties"));
        long t0 = System.nanoTime();
        Cosmos cosmos = new Cosmos(config.seed);
        SKY.setCosmos(cosmos);
        WEATHER.setSeed(config.seed);
        notebook = new Notebook(gameDir.resolve("starglass_observer").resolve("notebook.tsv"));
        LOG.info("Starglass: Observer sky built in {} ms; {} readings in the notebook", (System.nanoTime() - t0) / 1000000L, notebook.size());

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("starglass", "starglass"));
        keyTrack = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.starglass.track", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, category));
        keyRecord = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.starglass.record", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, category));
        keyNotebook = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.starglass.notebook", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, category));
        keyRadio = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.starglass.radio", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, category));

        ClientTickEvents.END_CLIENT_TICK.register(Starglass::tick);
        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, Identifier.fromNamespaceAndPath("starglass", "eyepiece"), ScopeHud::render);
        // Reticle replaces crosshair
        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, crosshair -> (g, delta) -> {
            if(!SCOPE.active)
            {
                crosshair.render(g, delta);
            }
        });
    }

    private static void tick(Minecraft mc)
    {
        SCOPE.tick(mc);
        while(keyTrack.consumeClick())
        {
            if(SCOPE.active)
            {
                SCOPE.setTracking(!SCOPE.tracking);
                SCOPE.say(SCOPE.tracking ? "Clock drive on" : "Clock drive off", 0x9ab0d0, 40);
            }
        }
        while(keyRadio.consumeClick())
        {
            if(SCOPE.active)
            {
                SCOPE.toggleRadio(mc);
            }
        }
        while(keyRecord.consumeClick())
        {
            record(mc);
        }
        while(keyNotebook.consumeClick())
        {
            if(mc.screen == null && mc.level != null)
            {
                mc.setScreen(new NotebookScreen());
            }
        }
    }

    /** Record a reading. */
    public static Notebook.Entry record(Minecraft mc)
    {
        if(!SCOPE.active || mc.level == null)
        {
            if(mc.player != null)
            {
                mc.player.displayClientMessage(Component.literal("Raise the spyglass to take a reading."), true);
            }
            return null;
        }
        Notebook.Entry e = notebook.record(Reading.now(mc));
        SCOPE.say("Reading #" + e.number + " recorded", 0xffe0a0, 50);
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.3F, 0.5F));
        return e;
    }

    public static Scope scope()
    {
        return SCOPE;
    }

    public static StarglassSky sky()
    {
        return SKY;
    }

    public static Weather weather()
    {
        return WEATHER;
    }

    public static StarglassConfig config()
    {
        return config;
    }

    public static Notebook notebook()
    {
        return notebook;
    }

    /** Sky replaces stars? */
    public static boolean replacesStars()
    {
        return !failed && config != null && SKY.cosmos() != null && (config.nakedEye || SCOPE.active);
    }

    private static boolean failed;

    /** Sky failure. */
    public static void skyFailed(RuntimeException e)
    {
        failed = true;
        LOG.error("Starglass sky failed; vanilla stars restored for this session", e);
    }
}
