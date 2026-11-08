package com.frostsmp.autogg.mixin;

import com.frostsmp.autogg.HitDetection;
import net.minecraft.component.type.PiercingWeaponComponent;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PiercingWeaponComponent.class)
public class PiercingWeaponComponentMixin {

    /** Spear jab validates each target through canHit before damaging. */
    @Inject(method = "canHit", at = @At("RETURN"))
    private static void autogg$spearCanHit(Entity attacker, Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            HitDetection.rememberIfLocalPlayerHit(attacker, target);
        }
    }

    @Inject(method = "playHitSound", at = @At("HEAD"))
    private void autogg$spearJabHitSound(Entity entity, CallbackInfo ci) {
        HitDetection.rememberNearestInFront(entity);
    }
}
