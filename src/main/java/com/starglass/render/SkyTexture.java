package com.starglass.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;

/** GPU picture. */
public final class SkyTexture implements AutoCloseable
{
    public final GpuTexture texture;
    public final GpuTextureView view;
    public final int width, height;

    public SkyTexture(String label, int argb[], int w, int h)
    {
        width = w;
        height = h;
        GpuDevice device = RenderSystem.getDevice();
        texture = device.createTexture("starglass:" + label, GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
            TextureFormat.RGBA8, w, h, 1, 1);
        view = device.createTextureView(texture);
        try(NativeImage img = new NativeImage(w, h, false))
        {
            for(int y = 0; y < h; y++)
            {
                for(int x = 0; x < w; x++)
                {
                    img.setPixel(x, y, argb[x + y * w]);
                }
            }
            device.createCommandEncoder().writeToTexture(texture, img);
        }
    }

    @Override
    public void close()
    {
        view.close();
        texture.close();
    }
}
