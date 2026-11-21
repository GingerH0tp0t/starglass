package com.starglass.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.starglass.Scope;
import com.starglass.Starglass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Scroll and mouse. */
@Mixin(MouseHandler.class)
abstract class MouseHandlerMixin
{
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void starglass$wind(long window, double dx, double dy, CallbackInfo ci)
    {
        if(window != minecraft.getWindow().handle() || minecraft.screen != null || minecraft.getOverlay() != null
            || !Starglass.scope().active || !Scope.isScoping(minecraft))
        {
            return;
        }
        double clicks = minecraft.options.discreteMouseScroll().get() ? Math.signum(dy) : dy;
        if(clicks != 0D)
        {
            Starglass.scope().scroll(clicks);
        }
        ci.cancel();
    }

    @WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void starglass$steady(LocalPlayer player, double yaw, double pitch, Operation<Void> original)
    {
        Scope scope = Starglass.scope();
        if(scope.active && Scope.isScoping(minecraft))
        {
            double vanilla = Scope.powerOfFov(minecraft.options.fov().get() * 0.1D);
            double k = Math.min(1.5D, vanilla / Math.max(scope.shownPower, 0.5D));
            yaw *= k;
            pitch *= k;
        }
        original.call(player, yaw, pitch);
    }
}
