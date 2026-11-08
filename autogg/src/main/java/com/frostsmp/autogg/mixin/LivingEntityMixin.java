package com.frostsmp.autogg.mixin;

import com.frostsmp.autogg.LastHitTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "onDamaged", at = @At("TAIL"))
    private void autogg$rememberTakenHit(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity local = client.player;
        if (local == null || self != local) {
            return;
        }
        if (source == null) {
            return;
        }

        PlayerEntity attacker = null;
        if (source.getAttacker() instanceof PlayerEntity playerAttacker) {
            attacker = playerAttacker;
        } else if (source.getSource() instanceof PlayerEntity direct) {
            attacker = direct;
        }

        if (attacker == null || attacker == local) {
            return;
        }
        LastHitTracker.rememberTaken(attacker);
    }
}
