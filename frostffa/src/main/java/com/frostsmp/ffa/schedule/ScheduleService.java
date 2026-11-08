package com.frostsmp.ffa.schedule;

import com.frostsmp.ffa.config.PluginConfig;
import com.frostsmp.ffa.mutator.MutatorManager;
import com.frostsmp.ffa.storage.FfaStorage;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ScheduleService {

    public record Slot(String type, LocalTime time) {
    }

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final MutatorManager mutators;
    private final FfaStorage storage;
    private BukkitTask task;
    private List<Slot> slots = List.of();
    private ZoneId zone = ZoneId.of("UTC");

    public ScheduleService(JavaPlugin plugin, PluginConfig config, MutatorManager mutators, FfaStorage storage) {
        this.plugin = plugin;
        this.config = config;
        this.mutators = mutators;
        this.storage = storage;
    }

    public void start() {
        reload();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::check, 20L, 20L * 15);
    }

    public void reload() {
        try {
            zone = ZoneId.of(config.timezone());
        } catch (Exception ex) {
            zone = ZoneId.of("UTC");
        }
        List<Slot> parsed = new ArrayList<>();
        for (Map<String, Object> raw : config.scheduleSlots()) {
            String type = String.valueOf(raw.getOrDefault("type", "mutator"));
            if ("koth".equalsIgnoreCase(type)) {
                continue;
            }
            String time = String.valueOf(raw.getOrDefault("time", "00:00"));
            try {
                parsed.add(new Slot("mutator", LocalTime.parse(time, HM)));
            } catch (Exception ignored) {
                plugin.getLogger().warning("Bad schedule time: " + time);
            }
        }
        slots = parsed;
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public List<Slot> slots() {
        return slots;
    }

    public long millisUntilNextMutator() {
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime soonest = null;
        for (Slot slot : slots) {
            ZonedDateTime at = now.with(slot.time()).withSecond(0).withNano(0);
            if (!at.isAfter(now)) {
                at = at.plusDays(1);
            }
            if (soonest == null || at.isBefore(soonest)) {
                soonest = at;
            }
        }
        if (soonest == null) {
            return 0L;
        }
        return Math.max(0L, soonest.toInstant().toEpochMilli() - System.currentTimeMillis());
    }

    private void check() {
        ZonedDateTime now = ZonedDateTime.now(zone);
        String today = now.toLocalDate().toString();
        for (Slot slot : slots) {
            if (now.toLocalTime().getHour() != slot.time().getHour()
                    || now.toLocalTime().getMinute() != slot.time().getMinute()) {
                continue;
            }
            String key = today + "-" + slot.time() + "-mutator";
            if (key.equals(storage.getLastSlotKey())) {
                return;
            }
            storage.setLastSlotKey(key);
            if (!mutators.isActive()) {
                mutators.startRandom();
            }
            return;
        }
    }
}
