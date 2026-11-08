package com.frostsmp.autogg.mixin;

import com.frostsmp.autogg.HitDetection;
import net.minecraft.component.type.KineticWeaponComponent;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KineticWeaponComponent.class)
public class KineticWeaponComponentMixin {

    /** Spear charge only counts when vanilla plays the hit sound. */
    @Inject(method = "playHitSound", at = @At("HEAD"))
    private void autogg$spearChargeHitSound(Entity entity, CallbackInfo ci) {
        HitDetection.rememberNearestInFront(entity);
    }
}
