package com.frostsmp.ffa.mutator;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Shared Mojang-signed skin for Blackout (OperatorExit9 by default).
 */
public final class BlackoutSkinService {

    private static final UUID SOURCE_UUID = UUID.fromString("35a50903-1bd5-41a1-8226-83d44fe4c51d");
    private static final String SOURCE_NAME = "OperatorExit9";

    private static final String FALLBACK_VALUE =
            "ewogICJ0aW1lc3RhbXAiIDogMTc4NzI0ODQzNTQ2NywKICAicHJvZmlsZUlkIiA6ICIzNWE1MDkwMzFiZDU0MWExODIyNjgzZDQ0ZmU0YzUxZCIsCiAgInByb2ZpbGVOYW1lIiA6ICJvcGVyYXRvcmV4aXQ5IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzFiYTUyNjU2ZTlmZDNlYTgyZTQ3YzA4YzM0MzM3ZjMyNzNjM2EyODIyNGY0M2FhMDJhYjFiYTViMmFiMzhlNzUiCiAgICB9LAogICAgIkNBUEUiIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzVlYzkzMGNkZDI2MjljODc3MTY1NWM2MGVlYmViODY3YjRiNjU1OWIwZTZkM2JjNzFjNDBjOTYzNDdmYTAzZjAiCiAgICB9CiAgfQp9";

    private static final String FALLBACK_SIGNATURE =
            "LsvR08B47S49EKFShfZRpJgJBJ8AH7FrRxjXpbWP34/0UZN+6VucJ8xVWl5oKi7sKbATMPDgKP5VhyvCDKmyYahmY0TU5XeF0G0sBMAM3gJkfn6cguMGJTQdLtM0FZjl5teePq6l+1rGPLQtw74UBN/kedQ6jFbJVbebprCW5NApgiUcCgEwA9baVVgblYWdwW2AFKXcuuWHffidw4o1iiCdJlblryAIQLpx3+XjdeMn64FiIYYOWAhS/oTKWRiRgSx05jWT2J1dpc/XfUsek8iguyJnZmqBgnzrIrhPpu6jI9geCvVM3zNJmAMwO5o0T37wVQHvbHHSJAMdUbUvlOcuBCNMkJmlinbMRYepB/0lUk+q48Lev2DJjYj6S9a2jAfTQytaZvGXiq/1VshdbeloX5VQritYMTY25378CDpxxp8Acd3gM7GTgEOXzyMjyR4L4RBCbvGYHgFBVi1AOLaPtJQpNSixR7MMn6dLsGKJVWFXO/lo/ta2clE9v/VTd2P4FMUSJaVMu32KpKebxqJwU/ek+Wn/iJ6Am6RwOXKOODmOsSOAc4uTiCebwmln2xLxgEZVl8T5TcNgI0FLzOcKGWF3Vvx5wsyR3wfrNwaosSymiuy5pnhFv4+KpZiUqLuUmd2vAl/lXFmabeFcwUEI1T1p7DHjSaHBnV2Ycrw=";

    private final JavaPlugin plugin;
    private final AtomicReference<ProfileProperty> textures = new AtomicReference<>();

    public BlackoutSkinService(JavaPlugin plugin) {
        this.plugin = plugin;
        textures.set(new ProfileProperty("textures", FALLBACK_VALUE, FALLBACK_SIGNATURE));
    }

    public ProfileProperty textures() {
        return textures.get();
    }

    public void refreshAsync(String playerName, Runnable onUpdated) {
        String name = playerName == null || playerName.isBlank() ? SOURCE_NAME : playerName.trim();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean updated = false;
            try {
                UUID uuid = SOURCE_UUID;
                String lookup = name;
                if (!SOURCE_NAME.equalsIgnoreCase(name)) {
                    PlayerProfile lookupProfile = Bukkit.createProfile(name);
                    if (lookupProfile.complete(true) && lookupProfile.getId() != null) {
                        uuid = lookupProfile.getId();
                        if (lookupProfile.getName() != null) {
                            lookup = lookupProfile.getName();
                        }
                    }
                }
                PlayerProfile source = Bukkit.createProfile(uuid, lookup);
                if (source.complete(true)) {
                    for (ProfileProperty property : source.getProperties()) {
                        if ("textures".equalsIgnoreCase(property.getName())
                                && property.getSignature() != null
                                && !property.getSignature().isEmpty()) {
                            textures.set(new ProfileProperty(
                                    "textures",
                                    property.getValue(),
                                    property.getSignature()
                            ));
                            updated = true;
                            plugin.getLogger().info("Blackout skin loaded from " + lookup);
                            break;
                        }
                    }
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("Blackout skin refresh failed: " + t.getMessage());
            }
            if (updated && onUpdated != null) {
                onUpdated.run();
            }
        });
    }

    public PlayerProfile disguise(UUID uuid, String name) {
        String safeName = name == null || name.isBlank() ? "Player" : name;
        PlayerProfile profile = Bukkit.createProfileExact(uuid, safeName);
        profile.clearProperties();
        profile.setProperty(textures());
        return profile;
    }

    public static PlayerProfile copyOf(PlayerProfile source) {
        String name = source.getName() != null ? source.getName() : "Player";
        UUID id = source.getId() != null ? source.getId() : UUID.randomUUID();
        PlayerProfile copy = Bukkit.createProfileExact(id, name);
        copy.clearProperties();
        for (ProfileProperty property : source.getProperties()) {
            copy.setProperty(property);
        }
        return copy;
    }
}
