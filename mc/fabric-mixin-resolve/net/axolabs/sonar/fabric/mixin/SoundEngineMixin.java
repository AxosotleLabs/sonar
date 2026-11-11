package net.axolabs.sonar.fabric.mixin;

import net.axolabs.sonar.mc.SonarClient;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {

    @Inject(method = "play", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/resources/sounds/SoundInstance;resolve(Lnet/minecraft/client/sounds/SoundManager;)Lnet/minecraft/client/sounds/WeighedSoundEvents;",
            shift = At.Shift.AFTER))
    private void sonar$onPlay(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        SonarClient.onSound(sound);
    }
}
