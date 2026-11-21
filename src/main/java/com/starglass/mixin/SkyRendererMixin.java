package com.starglass.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.starglass.Starglass;
import com.starglass.render.Shaders;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.SkyRenderState;
import net.minecraft.world.level.MoonPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sky pass. */
@Mixin(SkyRenderer.class)
abstract class SkyRendererMixin
{
    @Inject(method = "renderStars", at = @At("HEAD"), cancellable = true)
    private void starglass$noVanillaStars(float brightness, PoseStack poseStack, CallbackInfo ci)
    {
        if(Starglass.replacesStars())
        {
            ci.cancel();
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void starglass$state(ClientLevel level, float partial, Camera camera, SkyRenderState state, CallbackInfo ci)
    {
        Starglass.sky().captureState(state);
    }

    @Inject(method = "renderSunMoonAndStars", at = @At("TAIL"))
    private void starglass$sky(PoseStack poseStack, float sunAngle, float moonAngle, float starAngle, MoonPhase moonPhase,
        float rainBrightness, float starBrightness, CallbackInfo ci)
    {
        if(Starglass.replacesStars() && !Shaders.overlay())
        {
            try
            {
                Starglass.sky().render(RenderSystem.getModelViewMatrix(), sunAngle, moonAngle, starAngle, moonPhase);
            }
            catch(RuntimeException e)
            {
                Starglass.skyFailed(e);
            }
        }
    }
}
