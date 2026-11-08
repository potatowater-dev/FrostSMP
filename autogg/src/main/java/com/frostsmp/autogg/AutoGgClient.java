package com.frostsmp.autogg;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

public final class AutoGgClient implements ClientModInitializer {

    public static final String MOD_ID = "autogg";
    public static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
    public static KeyBinding quickMsgKey;

    private static RegistryKey<World> lastWorldKey;
    private static int lastLocalPlayerId = -1;

    @Override
    public void onInitializeClient() {
        AutoConfig.register(AutoGgConfig.class, GsonConfigSerializer::new);

        quickMsgKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autogg.quick_msg",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_GRAVE_ACCENT,
                CATEGORY
        ));

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient()) {
                return ActionResult.PASS;
            }
            if (entity instanceof PlayerEntity target && target != player) {
                LastHitTracker.rememberDealt(target);
            }
            return ActionResult.PASS;
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> clearCombatState());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            clearCombatState();
            lastWorldKey = null;
            lastLocalPlayerId = -1;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.world != null) {
                RegistryKey<World> worldKey = client.world.getRegistryKey();
                int playerId = client.player.getId();
                if (lastWorldKey != null && (!lastWorldKey.equals(worldKey) || lastLocalPlayerId != playerId)) {
                    clearCombatState();
                }
                lastWorldKey = worldKey;
                lastLocalPlayerId = playerId;
            }

            while (quickMsgKey.wasPressed()) {
                QuickMsgAction.send(client);
            }
        });
    }

    private static void clearCombatState() {
        LastHitTracker.clear();
        AntiSpamService.clear();
        HitDetection.clearDebounce();
    }

    public static AutoGgConfig config() {
        return AutoConfig.getConfigHolder(AutoGgConfig.class).getConfig();
    }
}
