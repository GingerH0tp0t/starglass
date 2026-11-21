package com.starglass.hud;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

/** Text panel. */
public final class Panel
{
    /** Tooltip style. */
    public static final Identifier STYLE = Identifier.fromNamespaceAndPath("starglass", "eyepiece");
    private static final Identifier BAR_BACK = Identifier.fromNamespaceAndPath("starglass", "hold_background");
    private static final Identifier BAR_FILL = Identifier.fromNamespaceAndPath("starglass", "hold_progress");

    // Colours
    public static final int TITLE = 0xffffaa00;      // gold, as a named item
    public static final int SUBTITLE = 0xffd9a65a;   // brass
    public static final int LABEL = 0xffaaaaaa;      // gray, as an attribute line
    public static final int VALUE = 0xffffe0a0;      // pale gold
    public static final int NOTE = 0xffb88a52;       // bronze, italic, as lore
    public static final int GOOD = 0xff55ff55;       // green
    public static final int WARN = 0xffffd060;       // amber
    public static final int HINT = 0xffa8c4e0;       // cold glass blue
    public static final int DIM = 0xff8a7a66;        // smoked brass

    static final int LINE = 10;
    private static final int TITLE_GAP = 2;

    private final Font font;
    private final int width;
    private final List<Row> rows = new ArrayList<>();
    private int height;

    private record Row(FormattedCharSequence left, String value, int color, int top, int height, double bar, boolean rule)
    {
    }

    Panel(Font font, int width)
    {
        this.font = font;
        this.width = width;
    }

    int height()
    {
        return height;
    }

    boolean isEmpty()
    {
        return rows.isEmpty();
    }

    private void add(FormattedCharSequence left, String value, int color, int h, double bar, boolean rule)
    {
        rows.add(new Row(left, value, color, height, h, bar, rule));
        height += h;
    }

    /** Title. */
    void title(String s, int color)
    {
        for(FormattedCharSequence l : font.split(Component.literal(s), width))
        {
            add(l, null, color, LINE, -1D, false);
        }
        height += TITLE_GAP;
    }

    void text(String s, int color)
    {
        wrap(Component.literal(s), color);
    }

    /** Note. */
    public void note(String s, int color)
    {
        wrap(Component.literal(s).withStyle(ChatFormatting.ITALIC), color);
    }

    private void wrap(Component c, int color)
    {
        for(FormattedCharSequence l : font.split(c, width))
        {
            add(l, null, color, LINE, -1D, false);
        }
    }

    /** Label and value. */
    void line(String label, String value)
    {
        if(value != null && font.width(label) + 6 + font.width(value) > width)
        {
            add(Component.literal(label).getVisualOrderText(), null, LABEL, LINE, -1D, false);
            add(FormattedCharSequence.EMPTY, value, LABEL, LINE, -1D, false);
            return;
        }
        add(Component.literal(label).getVisualOrderText(), value, LABEL, LINE, -1D, false);
    }

    void gap(int px)
    {
        if(!rows.isEmpty())
        {
            height += px;
        }
    }

    /** Rule. */
    void rule()
    {
        gap(2);
        add(null, null, 0, 4, -1D, true);
    }

    /** Progress bar. */
    void bar(double progress)
    {
        gap(2);
        add(null, null, 0, 7, progress, false);
    }

    /** Draw, scaled. */
    void draw(GuiGraphics g, float x, float y, float s)
    {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(s, s);
        draw(g, 0, 0);
        g.pose().popMatrix();
    }

    /** Frame margin. */
    static final int FRAME = 4;

    /** Panel layout. */
    static final class Layout
    {
        private static final float LENS = 1.125F, GAP = 3F;

        final float s;
        /** Fits beside the lens. */
        final boolean fits;
        private final double guiScale;
        private final float lensLeft, lensRight;
        private final int w, h;

        Layout(int w, int h, double guiScale, int widest)
        {
            this.w = w;
            this.h = h;
            this.guiScale = guiScale;
            float lens = LENS * Math.min(w, h);
            lensLeft = (w - lens) / 2F;
            lensRight = (w + lens) / 2F;
            // Scale
            double band = lensLeft - GAP - 4D;
            int px = (int)Math.min(Math.rint(guiScale), Math.floor(band * guiScale / (widest + 2 * FRAME)));
            fits = px >= 1;
            s = (float)(Math.max(1, px) / guiScale);
        }

        private float snap(double v)
        {
            return (float)(Math.rint(v * guiScale) / guiScale);
        }

        /** Left x. */
        float left(int width)
        {
            float x = fits ? lensLeft - GAP - (width + FRAME) * s : 4F + FRAME * s;
            return snap(Math.max(4F + FRAME * s, x));
        }

        /** Right x. */
        float right(int width)
        {
            float x = fits ? lensRight + GAP + FRAME * s : w - 4F - (width + FRAME) * s;
            return snap(Math.min(w - 4F - (width + FRAME) * s, x));
        }

        /** Top y. */
        float top()
        {
            return snap(4F + FRAME * s);
        }

        /** Bottom y. */
        float bottom(Panel p)
        {
            return snap(h - 4F - (p.height() - 2 + FRAME) * s);
        }

        /** Y under a panel. */
        float under(Panel p, float y)
        {
            return snap(y + (p.height() - 2 + 2 * FRAME + 3) * s);
        }
    }

    /** Draw. */
    void draw(GuiGraphics g, int x, int y)
    {
        if(rows.isEmpty())
        {
            return;
        }
        int h = height - 2;
        TooltipRenderUtil.renderTooltipBackground(g, x, y, width, h, STYLE);
        for(Row r : rows)
        {
            int ry = y + r.top;
            if(r.rule)
            {
                g.fill(x, ry + 1, x + width, ry + 2, 0x70d9a65a);
            } else if(r.bar >= 0D)
            {
                g.blitSprite(RenderPipelines.GUI_TEXTURED, BAR_BACK, x, ry, width, 5);
                int filled = (int)Math.round(width * Math.min(1D, r.bar));
                if(filled > 0)
                {
                    g.enableScissor(x, ry, x + filled, ry + 5);
                    g.blitSprite(RenderPipelines.GUI_TEXTURED, BAR_FILL, x, ry, width, 5);
                    g.disableScissor();
                }
            } else
            {
                g.drawString(font, r.left, x, ry, r.color);
                if(r.value != null)
                {
                    g.drawString(font, r.value, x + width - font.width(r.value), ry, VALUE);
                }
            }
        }
    }
}
