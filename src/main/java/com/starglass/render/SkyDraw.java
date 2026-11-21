package com.starglass.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** Sky drawing. */
public final class SkyDraw
{
    public static final float SHELL = 100F;

    /** Additive. */
    public static final RenderPipeline ADD_COLOR = pipeline("add_color", false, BlendFunction.LIGHTNING, DepthTestFunction.NO_DEPTH_TEST);
    public static final RenderPipeline ADD_TEX = pipeline("add_tex", true, BlendFunction.LIGHTNING, DepthTestFunction.NO_DEPTH_TEST);
    /** Opaque. */
    public static final RenderPipeline ALPHA_TEX = pipeline("alpha_tex", true, BlendFunction.TRANSLUCENT, DepthTestFunction.NO_DEPTH_TEST);
    public static final RenderPipeline ALPHA_COLOR = pipeline("alpha_color", false, BlendFunction.TRANSLUCENT, DepthTestFunction.NO_DEPTH_TEST);

    /* Overlay pipelines */
    private static final RenderPipeline OVER_ADD_COLOR = pipeline("over_add_color", false, BlendFunction.LIGHTNING, DepthTestFunction.LEQUAL_DEPTH_TEST);
    private static final RenderPipeline OVER_ADD_TEX = pipeline("over_add_tex", true, BlendFunction.LIGHTNING, DepthTestFunction.LEQUAL_DEPTH_TEST);
    private static final RenderPipeline OVER_ALPHA_TEX = pipeline("over_alpha_tex", true, BlendFunction.TRANSLUCENT, DepthTestFunction.LEQUAL_DEPTH_TEST);
    private static final RenderPipeline OVER_ALPHA_COLOR = pipeline("over_alpha_color", false, BlendFunction.TRANSLUCENT, DepthTestFunction.LEQUAL_DEPTH_TEST);

    private static RenderPipeline pipeline(String name, boolean textured, BlendFunction blend, DepthTestFunction depth)
    {
        RenderPipeline.Builder b = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation("starglass/pipeline/" + name)
            .withVertexShader(textured ? "core/position_tex_color" : "core/position_color")
            .withFragmentShader(textured ? "core/position_tex_color" : "core/position_color")
            .withBlend(blend)
            .withDepthWrite(false)
            .withDepthTestFunction(depth)
            .withCull(false)
            .withVertexFormat(textured ? DefaultVertexFormat.POSITION_TEX_COLOR : DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS);
        if(textured)
        {
            b.withSampler("Sampler0");
        }
        return b.build();
    }

    private static RenderPipeline overVersion(RenderPipeline p)
    {
        return p == ADD_COLOR ? OVER_ADD_COLOR : p == ADD_TEX ? OVER_ADD_TEX : p == ALPHA_TEX ? OVER_ALPHA_TEX
            : p == ALPHA_COLOR ? OVER_ALPHA_COLOR : p;
    }

    /** Far-plane projection. */
    public static Matrix4f farProjection(Matrix4f projection, Matrix4f out)
    {
        out.set(projection);
        float k = 1F - 2.0E-6F;
        out.m02(projection.m03() * k);
        out.m12(projection.m13() * k);
        out.m22(projection.m23() * k);
        out.m32(projection.m33() * k);
        return out;
    }

    private static final Vector4f WHITE = new Vector4f(1F, 1F, 1F, 1F);
    private static final Vector3f ZERO = new Vector3f();
    private static final Matrix4f IDENTITY = new Matrix4f();

    private final Matrix4f modelView = new Matrix4f();
    /** Overlay mode. */
    private boolean over;
    private RenderPipeline pipeline;
    private GpuTextureView texture;
    private GpuSampler sampler;
    private BufferBuilder buffer;
    private int vertices;

    /** Model-view. */
    public void setModelView(Matrix4f m)
    {
        modelView.set(m);
    }

    public void setOver(boolean b)
    {
        over = b;
    }

    public BufferBuilder begin(RenderPipeline p)
    {
        return begin(p, null, null);
    }

    public BufferBuilder begin(RenderPipeline p, GpuTextureView tex, GpuSampler samp)
    {
        if(buffer != null)
        {
            end();
        }
        pipeline = over ? overVersion(p) : p;
        texture = tex;
        sampler = samp;
        buffer = Tesselator.getInstance().begin(p.getVertexFormatMode(), p.getVertexFormat());
        vertices = 0;
        return buffer;
    }

    public BufferBuilder buffer()
    {
        return buffer;
    }

    // Vertices

    public void vertex(double x, double y, double z, int argb)
    {
        buffer.addVertex((float)x, (float)y, (float)z).setColor(Filter.apply(argb));
        vertices++;
    }

    public void vertex(double x, double y, double z, float u, float v, int argb)
    {
        buffer.addVertex((float)x, (float)y, (float)z).setUv(u, v).setColor(Filter.apply(argb));
        vertices++;
    }

    public static int argb(double a, int rgb)
    {
        int ai = (int)(a < 0D ? 0D : (a > 1D ? 255D : a * 255D));
        return ai << 24 | rgb & 0xffffff;
    }

    public static int argb(double a, double r, double g, double b)
    {
        return argb(a, clamp255(r) << 16 | clamp255(g) << 8 | clamp255(b));
    }

    private static int clamp255(double v)
    {
        int i = (int)(v * 255D);
        return i < 0 ? 0 : (i > 255 ? 255 : i);
    }

    /** Square. */
    public void quad(double d[], double A[], double B[], double h, int argb)
    {
        double x = d[0] * SHELL, y = d[1] * SHELL, z = d[2] * SHELL;
        for(int q = 0; q < 4; q++)
        {
            double sa = (q == 1 || q == 2) ? h : -h, sb = q >= 2 ? h : -h;
            vertex(x + A[0] * sa + B[0] * sb, y + A[1] * sa + B[1] * sb, z + A[2] * sa + B[2] * sb, argb);
        }
    }

    /** Textured part-square. */
    public void skyQuad(double d[], double A[], double B[], double ha, double hb,
        double g0, double h0, double g1, double h1, double u0, double v0, double u1, double v1, int argb, boolean textured)
    {
        double gs[] = { g0, g1, g1, g0 }, hs[] = { h1, h1, h0, h0 };
        double us[] = { u0, u1, u1, u0 }, vs[] = { v1, v1, v0, v0 };
        for(int q = 0; q < 4; q++)
        {
            double fa = (gs[q] * 2D - 1D) * ha, fb = (1D - hs[q] * 2D) * hb;
            double x = d[0] + A[0] * fa + B[0] * fb, y = d[1] + A[1] * fa + B[1] * fb, z = d[2] + A[2] * fa + B[2] * fb;
            double l = SHELL / Math.sqrt(x * x + y * y + z * z);
            if(textured)
            {
                vertex(x * l, y * l, z * l, (float)us[q], (float)vs[q], argb);
            } else
            {
                vertex(x * l, y * l, z * l, argb);
            }
        }
    }

    /** Textured grid. */
    public void skyGrid(double d[], double A[], double B[], double ha, double hb, int n, double inset, int argb)
    {
        double span = 1D - 2D * inset;
        for(int j = 0; j < n; j++)
        {
            for(int i = 0; i < n; i++)
            {
                double u0 = (double)i / n, v0 = (double)j / n, u1 = (double)(i + 1) / n, v1 = (double)(j + 1) / n;
                skyQuad(d, A, B, ha, hb, u0, v0, u1, v1, inset + u0 * span, inset + v0 * span, inset + u1 * span,
                    inset + v1 * span, argb, true);
            }
        }
    }

    /** Draw. */
    public void end()
    {
        BufferBuilder b = buffer;
        buffer = null;
        if(b == null)
        {
            return;
        }
        MeshData mesh = b.build();
        if(mesh == null)
        {
            return;
        }
        try(mesh)
        {
            draw(mesh);
        }
    }

    private void draw(MeshData mesh)
    {
        Minecraft mc = Minecraft.getInstance();
        GpuBufferSlice transform = RenderSystem.getDynamicUniforms().writeTransform(modelView, WHITE, ZERO, IDENTITY);
        GpuBuffer vbo = pipeline.getVertexFormat().uploadImmediateVertexBuffer(mesh.vertexBuffer());
        RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(mesh.drawState().mode());
        GpuBuffer ibo = indices.getBuffer(mesh.drawState().indexCount());
        GpuTextureView colour = mc.getMainRenderTarget().getColorTextureView();
        GpuTextureView depth = mc.getMainRenderTarget().getDepthTextureView();
        try(RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
            .createRenderPass(() -> "Starglass sky", colour, OptionalInt.empty(), depth, OptionalDouble.empty()))
        {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transform);
            if(texture != null)
            {
                pass.bindTexture("Sampler0", texture, sampler);
            }
            pass.setVertexBuffer(0, vbo);
            pass.setIndexBuffer(ibo, indices.type());
            pass.drawIndexed(0, 0, mesh.drawState().indexCount(), 1);
        }
    }
}
