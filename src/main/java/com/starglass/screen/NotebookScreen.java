package com.starglass.screen;

import com.starglass.Notebook;
import com.starglass.Starglass;
import com.starglass.hud.Panel;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.network.chat.Component;

/** Notebook screen. */
public final class NotebookScreen extends Screen
{
    private static final int LINE = 10;
    private static final int PAGE_W = BookEditScreen.TEXT_WIDTH, PAGE_LINES = 14;

    private int page;
    private String status;
    private int statusColor = Panel.HINT;
    private Button prev, next, copy, copyAll, strike;

    public NotebookScreen()
    {
        super(Component.literal("Observer's Notebook"));
        Notebook nb = Starglass.notebook();
        page = nb.pages() - 1;
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    @Override
    protected void init()
    {
        int w = frameWidth(), x0 = (width - w) / 2, bottom = height - 16 - 24;
        int bw = (w - 4 * 4) / 5;
        prev = addRenderableWidget(Button.builder(Component.literal("< Page"), b -> turn(-1)).bounds(x0, bottom, bw, 20).build());
        next = addRenderableWidget(Button.builder(Component.literal("Page >"), b -> turn(1)).bounds(x0 + (bw + 4), bottom, bw, 20).build());
        copy = addRenderableWidget(Button.builder(Component.literal("Copy page"), b -> copyPage()).bounds(x0 + 2 * (bw + 4), bottom, bw, 20).build());
        copyAll = addRenderableWidget(Button.builder(Component.literal("Copy all"), b -> copyAll()).bounds(x0 + 3 * (bw + 4), bottom, bw, 20).build());
        strike = addRenderableWidget(Button.builder(Component.literal("Strike last"), b -> strikeLast()).bounds(x0 + 4 * (bw + 4), bottom, bw, 20).build());
        refresh();
    }

    private int frameWidth()
    {
        return Math.min(width - 28, 360);
    }

    private void turn(int by)
    {
        Notebook nb = Starglass.notebook();
        page = Math.max(0, Math.min(nb.pages() - 1, page + by));
        status = null;
        refresh();
    }

    private void copyPage()
    {
        Notebook nb = Starglass.notebook();
        Minecraft.getInstance().keyboardHandler.setClipboard(nb.page(page));
        status = "Page " + (page + 1) + " copied. In a Book & Quill, Ctrl+V pastes it.";
        statusColor = Panel.GOOD;
    }

    private void copyAll()
    {
        Notebook nb = Starglass.notebook();
        Minecraft.getInstance().keyboardHandler.setClipboard(nb.all());
        status = "All " + nb.size() + " readings copied, one to a line.";
        statusColor = Panel.GOOD;
    }

    private void strikeLast()
    {
        Notebook nb = Starglass.notebook();
        Notebook.Entry e = nb.removeLast();
        status = e == null ? null : "Reading #" + e.number + " struck out.";
        statusColor = Panel.WARN;
        page = Math.min(page, nb.pages() - 1);
        refresh();
    }

    private void refresh()
    {
        Notebook nb = Starglass.notebook();
        boolean any = nb.size() > 0;
        prev.active = page > 0;
        next.active = page < nb.pages() - 1;
        copy.active = any;
        copyAll.active = any;
        strike.active = any;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial)
    {
        Notebook nb = Starglass.notebook();
        int w = frameWidth(), x0 = (width - w) / 2, y0 = 16, y1 = height - 16;
        TooltipRenderUtil.renderTooltipBackground(g, x0, y0, w, y1 - y0, Panel.STYLE);
        g.drawString(font, title, x0 + 4, y0 + 4, Panel.TITLE, false);
        String count = nb.size() == 1 ? "1 reading" : nb.size() + " readings";
        g.drawString(font, count, x0 + w - 4 - font.width(count), y0 + 4, Panel.SUBTITLE, false);

        int top = y0 + 20, listBottom = y1 - 40;
        int pageX = x0 + w - PAGE_W - 12;
        // Readings
        List<Notebook.Entry> all = nb.entries();
        int y = top;
        if(all.isEmpty())
        {
            drawWrapped(g, "Nothing yet. Raise the spyglass, point it, and press the record key: where it points, when, the glass "
                + "and the meter go down as a numbered reading.", x0 + 6, y, pageX - x0 - 18, Panel.LABEL);
        }
        for(int i = all.size() - 1; i >= 0 && y + 2 * LINE <= listBottom; i--)
        {
            Notebook.Entry e = all.get(i);
            String lines[] = e.reading.bookLines(e.number);
            g.drawString(font, lines[0], x0 + 6, y, Panel.VALUE, false);
            g.drawString(font, lines[1], x0 + 6 + font.width(lines[0]) + 8, y, Panel.SUBTITLE, false);
            g.drawString(font, lines[2] + "  " + lines[3], x0 + 12, y + LINE, Panel.LABEL, false);
            y += 2 * LINE + 3;
        }

        // Page
        int py = top;
        g.fill(pageX - 6, py - 4, pageX + PAGE_W + 6, py + PAGE_LINES * 9 + 4, 0xfff4ecd8);
        String text = nb.page(page);
        int line = 0;
        for(String l : text.split("\n", -1))
        {
            if(line >= PAGE_LINES)
            {
                break;
            }
            g.drawString(font, l, pageX, py + line * 9, 0xff000000, false);
            line++;
        }
        String label = "Book page " + (page + 1) + " of " + nb.pages();
        g.drawString(font, label, pageX + PAGE_W / 2 - font.width(label) / 2, py + PAGE_LINES * 9 + 8, Panel.SUBTITLE, false);

        if(status != null)
        {
            g.drawString(font, status, x0 + 6, y1 - 36 - LINE, statusColor, false);
        }
        super.render(g, mx, my, partial);
    }

    private void drawWrapped(GuiGraphics g, String s, int x, int y, int width, int color)
    {
        for(var seq : font.split(Component.literal(s), width))
        {
            g.drawString(font, seq, x, y, color, false);
            y += LINE;
        }
    }
}
