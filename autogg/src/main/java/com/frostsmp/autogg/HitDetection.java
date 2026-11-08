package com.frostsmp.autogg;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.UUID;

/**
 * Helpers for remembering hits that bypass {@code attackEntity}
 * (spear jab / charge via piercing & kinetic weapon components).
 */
public final class HitDetection {

    private static final double SPEAR_REACH = 6.0;
    private static final int SPEAR_DEBOUNCE_TICKS = 5;

    private static UUID lastSpearTargetId;
    private static int lastSpearRememberTick = Integer.MIN_VALUE;

    private HitDetection() {
    }

    public static void rememberIfLocalPlayerHit(Entity attacker, Entity target) {
        if (attacker == null || target == null) {
            return;
        }
        World world = attacker.getEntityWorld();
        if (world == null || !world.isClient()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity self = client.player;
        if (self == null || attacker != self) {
            return;
        }
        if (target instanceof PlayerEntity hit && hit != self) {
            LastHitTracker.rememberDealt(hit);
        }
    }

    /**
     * When spear charge plays a hit sound we only get the attacker —
     * pick the best player target along the look ray / nearby in front.
     */
    public static void rememberNearestInFront(Entity attacker) {
        if (attacker == null) {
            return;
        }
        World world = attacker.getEntityWorld();
        if (world == null || !world.isClient()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity self = client.player;
        if (self == null || attacker != self) {
            return;
        }

        double lateralMax = 1.0;
        AutoGgConfig config = AutoGgClient.config();
        if (config != null && config.spearLateralMax > 0.0) {
            lateralMax = config.spearLateralMax;
        }

        // Prefer exact crosshair entity if it is a player in reach.
        if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            Entity focused = ((EntityHitResult) client.crosshairTarget).getEntity();
            if (focused instanceof PlayerEntity hit && hit != self
                    && self.squaredDistanceTo(hit) <= SPEAR_REACH * SPEAR_REACH) {
                rememberDealtDebounced(hit, client);
                return;
            }
        }

        Vec3d eye = self.getEyePos();
        Vec3d look = self.getRotationVec(1.0F);
        Vec3d end = eye.add(look.multiply(SPEAR_REACH));
        Box search = self.getBoundingBox().stretch(look.multiply(SPEAR_REACH)).expand(1.0);

        PlayerEntity best = null;
        double bestScore = Double.MAX_VALUE;

        for (PlayerEntity other : world.getEntitiesByClass(PlayerEntity.class, search, p -> p != self && p.isAlive())) {
            Optional<Vec3d> clip = other.getBoundingBox().expand(0.35).raycast(eye, end);
            if (clip.isEmpty()) {
                // Also allow near-miss along the spear line (charge hitboxes are generous).
                double along = projectedDistanceAlongLook(eye, look, other.getBoundingBox().getCenter());
                if (along < 0.5 || along > SPEAR_REACH) {
                    continue;
                }
                double lateral = lateralDistance(eye, look, other.getBoundingBox().getCenter());
                if (lateral > lateralMax) {
                    continue;
                }
                double score = along + lateral * 2.5;
                if (score < bestScore) {
                    bestScore = score;
                    best = other;
                }
                continue;
            }
            double dist = eye.distanceTo(clip.get());
            if (dist < bestScore) {
                bestScore = dist;
                best = other;
            }
        }

        if (best != null) {
            rememberDealtDebounced(best, client);
        }
    }

    private static void rememberDealtDebounced(PlayerEntity hit, MinecraftClient client) {
        int tick = client.player == null ? 0 : client.player.age;
        UUID id = hit.getUuid();
        if (id.equals(lastSpearTargetId) && tick - lastSpearRememberTick < SPEAR_DEBOUNCE_TICKS) {
            return;
        }
        lastSpearTargetId = id;
        lastSpearRememberTick = tick;
        LastHitTracker.rememberDealt(hit);
    }

    public static void clearDebounce() {
        lastSpearTargetId = null;
        lastSpearRememberTick = Integer.MIN_VALUE;
    }

    private static double projectedDistanceAlongLook(Vec3d eye, Vec3d look, Vec3d point) {
        return point.subtract(eye).dotProduct(look);
    }

    private static double lateralDistance(Vec3d eye, Vec3d look, Vec3d point) {
        Vec3d relative = point.subtract(eye);
        double along = relative.dotProduct(look);
        Vec3d closest = eye.add(look.multiply(along));
        return closest.distanceTo(point);
    }
}
