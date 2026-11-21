package com.starglass.mixin;

import com.starglass.Scope;
import com.starglass.Starglass;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hand shake. */
@Mixin(Camera.class)
abstract class CameraMixin
{
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow public abstract float xRot();
    @Shadow public abstract float yRot();

    @Inject(method = "setup", at = @At("TAIL"))
    private void starglass$shake(Level level, Entity entity, boolean detached, boolean mirrored, float partial, CallbackInfo ci)
    {
        Scope scope = Starglass.scope();
        if(detached || !scope.active || !Scope.isScoping(Minecraft.getInstance()))
        {
            return;
        }
        double t = System.nanoTime() / 1.0E9D;
        double shake = scope.optics.shake();
        double dp = shake * (Math.sin(t * 2.3D) * 0.6D + Math.sin(t * 5.9D + 1D) * 0.4D);
        double dy = shake * (Math.sin(t * 1.7D + 2D) * 0.6D + Math.sin(t * 4.7D) * 0.4D)
            / Math.max(0.05D, Math.cos(Math.toRadians(xRot())));
        setRotation((float)(yRot() + dy), (float)(xRot() - dp));
    }
}
