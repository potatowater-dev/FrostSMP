package com.frostsmp.parkour.service;

import com.frostsmp.parkour.FrostParkourPlugin;
import com.frostsmp.parkour.model.Course;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class CourseService {

    private final FrostParkourPlugin plugin;
    private final File file;
    private final Map<String, Course> courses = new ConcurrentHashMap<>();

    public CourseService(FrostParkourPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "courses.yml");
    }

    public void load() {
        courses.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection coursesSec = config.getConfigurationSection("courses");
        if (coursesSec == null) {
            return;
        }

        for (String id : coursesSec.getKeys(false)) {
            ConfigurationSection sec = coursesSec.getConfigurationSection(id);
            if (sec == null) {
                continue;
            }
            String displayName = sec.getString("display-name", id);
            Course course = new Course(id, displayName);
            course.setStartLocation(deserializeLocation(sec.getConfigurationSection("start")));
            course.setEndLocation(deserializeLocation(sec.getConfigurationSection("end")));
            if (sec.contains("fall-y")) {
                course.setCustomFallY(sec.getDouble("fall-y"));
            }

            ConfigurationSection cpSec = sec.getConfigurationSection("checkpoints");
            if (cpSec != null) {
                List<Location> cps = new ArrayList<>();
                for (String cpKey : cpSec.getKeys(false)) {
                    Location loc = deserializeLocation(cpSec.getConfigurationSection(cpKey));
                    if (loc != null) {
                        cps.add(loc);
                    }
                }
                course.setCheckpoints(cps);
            }
            courses.put(id.toLowerCase(Locale.ROOT), course);
        }
        plugin.getLogger().info("Loaded " + courses.size() + " parkour course(s).");
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection coursesSec = config.createSection("courses");

        for (Course course : courses.values()) {
            ConfigurationSection sec = coursesSec.createSection(course.getId());
            sec.set("display-name", course.getDisplayName());
            if (course.getStartLocation() != null) {
                serializeLocation(sec.createSection("start"), course.getStartLocation());
            }
            if (course.getEndLocation() != null) {
                serializeLocation(sec.createSection("end"), course.getEndLocation());
            }
            if (course.getCustomFallY() != null) {
                sec.set("fall-y", course.getCustomFallY());
            }
            if (!course.getCheckpoints().isEmpty()) {
                ConfigurationSection cpSec = sec.createSection("checkpoints");
                int idx = 0;
                for (Location cp : course.getCheckpoints()) {
                    serializeLocation(cpSec.createSection(String.valueOf(idx++)), cp);
                }
            }
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save courses.yml: " + e.getMessage());
        }
    }

    public Course getCourse(String id) {
        if (id == null) {
            return null;
        }
        return courses.get(id.toLowerCase(Locale.ROOT));
    }

    public boolean createCourse(String id, String displayName) {
        String key = id.toLowerCase(Locale.ROOT);
        if (courses.containsKey(key)) {
            return false;
        }
        Course course = new Course(id, displayName);
        courses.put(key, course);
        save();
        return true;
    }

    public boolean deleteCourse(String id) {
        String key = id.toLowerCase(Locale.ROOT);
        if (courses.remove(key) != null) {
            save();
            return true;
        }
        return false;
    }

    public Collection<Course> getAllCourses() {
        return Collections.unmodifiableCollection(courses.values());
    }

    private static void serializeLocation(ConfigurationSection sec, Location loc) {
        if (loc == null || loc.getWorld() == null || sec == null) {
            return;
        }
        sec.set("world", loc.getWorld().getName());
        sec.set("x", loc.getX());
        sec.set("y", loc.getY());
        sec.set("z", loc.getZ());
        sec.set("yaw", (double) loc.getYaw());
        sec.set("pitch", (double) loc.getPitch());
    }

    private static Location deserializeLocation(ConfigurationSection sec) {
        if (sec == null) {
            return null;
        }
        String worldName = sec.getString("world");
        if (worldName == null) {
            return null;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        double x = sec.getDouble("x");
        double y = sec.getDouble("y");
        double z = sec.getDouble("z");
        float yaw = (float) sec.getDouble("yaw", 0.0);
        float pitch = (float) sec.getDouble("pitch", 0.0);
        return new Location(world, x, y, z, yaw, pitch);
    }
}
