package com.starglass;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** The notebook. */
public final class Notebook
{
    public static final int PER_PAGE = 3;

    public static final class Entry
    {
        public final int number;
        public final Reading reading;

        Entry(int number, Reading reading)
        {
            this.number = number;
            this.reading = reading;
        }
    }

    private final Path file;
    private final List<Entry> entries = new ArrayList<>();

    public Notebook(Path file)
    {
        this.file = file;
        load();
    }

    public Path file()
    {
        return file;
    }

    public List<Entry> entries()
    {
        return entries;
    }

    public int size()
    {
        return entries.size();
    }

    /** Record a reading. */
    public Entry record(Reading r)
    {
        int n = entries.isEmpty() ? 1 : entries.get(entries.size() - 1).number + 1;
        Entry e = new Entry(n, r);
        entries.add(e);
        save();
        return e;
    }

    /** Strike the last reading. */
    public Entry removeLast()
    {
        if(entries.isEmpty())
        {
            return null;
        }
        Entry e = entries.remove(entries.size() - 1);
        save();
        return e;
    }

    public int pages()
    {
        return Math.max(1, (entries.size() + PER_PAGE - 1) / PER_PAGE);
    }

    /** One book page. */
    public String page(int p)
    {
        StringBuilder b = new StringBuilder();
        for(int i = p * PER_PAGE; i < Math.min(entries.size(), (p + 1) * PER_PAGE); i++)
        {
            Entry e = entries.get(i);
            if(b.length() > 0)
            {
                b.append("\n\n");
            }
            b.append(String.join("\n", e.reading.bookLines(e.number)));
        }
        return b.toString();
    }

    /** Every reading. */
    public String all()
    {
        StringBuilder b = new StringBuilder();
        for(Entry e : entries)
        {
            b.append(e.reading.plain(e.number)).append('\n');
        }
        return b.toString();
    }

    private void load()
    {
        entries.clear();
        if(!Files.exists(file))
        {
            return;
        }
        try
        {
            for(String line : Files.readAllLines(file, StandardCharsets.UTF_8))
            {
                String f[] = line.split("\t");
                Reading r = Reading.fromTsv(f);
                if(r != null)
                {
                    try
                    {
                        entries.add(new Entry(Integer.parseInt(f[0]), r));
                    }
                    catch(NumberFormatException ignored)
                    {
                    }
                }
            }
        }
        catch(IOException e)
        {
            Starglass.LOG.warn("Could not read the notebook at {}", file, e);
        }
    }

    private void save()
    {
        List<String> lines = new ArrayList<>();
        lines.add("# Starglass observer's notebook: number, world tick, bearing, altitude, power, braced, glass, meter, x, y, z");
        for(Entry e : entries)
        {
            lines.add(e.reading.toTsv(e.number));
        }
        try
        {
            Files.createDirectories(file.getParent());
            Files.write(file, lines, StandardCharsets.UTF_8);
        }
        catch(IOException e)
        {
            Starglass.LOG.warn("Could not write the notebook at {}", file, e);
        }
    }
}
