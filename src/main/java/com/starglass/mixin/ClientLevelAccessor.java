package com.starglass.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lightning flash. */
@Mixin(ClientLevel.class)
public interface ClientLevelAccessor
{
    @Accessor("skyFlashTime")
    int starglass$skyFlashTime();
}
