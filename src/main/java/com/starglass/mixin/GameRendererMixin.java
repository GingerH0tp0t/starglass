package com.starglass.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.starglass.Scope;
import com.starglass.Starglass;
import com.starglass.render.Shaders;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.joml.Matrix4f;

/** Zoom and overlay. */
@Mixin(GameRenderer.class)
abstract class GameRendererMixin
{
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void starglass$power(Camera camera, float partial, boolean useFovSetting, CallbackInfoReturnable<Float> cir)
    {
        if(!useFovSetting)
        {
            return;
        }
        Scope scope = Starglass.scope();
        if(scope.active && Scope.isScoping(minecraft))
        {
            cir.setReturnValue((float)scope.fovDegrees());
        }
        Starglass.lastFov = cir.getReturnValueF();
    }

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void starglass$frame(DeltaTracker delta, CallbackInfo ci)
    {
        Starglass.scope().frame(minecraft, Starglass.sky().lastStarAngle);
    }

    /** Shader overlay. */
    @Inject(method = "renderLevel", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V",
        shift = At.Shift.AFTER))
    private void starglass$overShaders(DeltaTracker delta, CallbackInfo ci, @Local(ordinal = 0) Matrix4f projection,
        @Local(ordinal = 1) Matrix4f view)
    {
        if(Starglass.replacesStars() && Shaders.overlay())
        {
            try
            {
                Starglass.sky().renderOverlay(projection, view);
            }
            catch(RuntimeException e)
            {
                Starglass.skyFailed(e);
            }
        }
    }
}
