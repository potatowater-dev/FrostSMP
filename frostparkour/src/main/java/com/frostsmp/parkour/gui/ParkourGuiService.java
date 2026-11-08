package com.frostsmp.parkour.gui;

import com.frostsmp.parkour.FrostParkourPlugin;
import com.frostsmp.parkour.model.Course;
import com.frostsmp.parkour.model.ParkourRecord;
import com.frostsmp.parkour.model.PlayerStats;
import com.frostsmp.parkour.service.CourseService;
import com.frostsmp.parkour.service.LeaderboardService;
import com.frostsmp.parkour.util.MessageUtil;
import com.frostsmp.parkour.util.TimeUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.util.*;

public final class ParkourGuiService {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private final FrostParkourPlugin plugin;
    private final CourseService courseService;
    private final LeaderboardService leaderboardService;

    public ParkourGuiService(FrostParkourPlugin plugin, CourseService courseService, LeaderboardService leaderboardService) {
        this.plugin = plugin;
        this.courseService = courseService;
        this.leaderboardService = leaderboardService;
    }

    public void openLeaderboard(Player player, String courseId) {
        Course course = courseService.getCourse(courseId);
        String courseDisplay = course != null ? course.getDisplayName() : courseId;

        FileConfiguration config = plugin.getConfig();
        int size = config.getInt("leaderboard-gui.size", 45);
        String title = MessageUtil.format(config.getString("leaderboard-gui.title", "<dark_gray>Top Times: <gold>{course}</gold></dark_gray>"), "course", courseDisplay);

        LeaderboardGuiHolder holder = new LeaderboardGuiHolder(courseId);
        Inventory inv = Bukkit.createInventory(holder, size, MessageUtil.parse(title));
        holder.bind(inv);

        // Fill background
        Material fillMat = Material.matchMaterial(config.getString("leaderboard-gui.filler.material", "GRAY_STAINED_GLASS_PANE"));
        if (fillMat != null) {
            ItemStack filler = createItem(fillMat, config.getString("leaderboard-gui.filler.name", " "), List.of());
            for (int i = 0; i < size; i++) {
                inv.setItem(i, filler);
            }
        }

        List<Integer> slots = config.getIntegerList("leaderboard-gui.rank-slots");
        if (slots.isEmpty()) {
            slots = List.of(10, 11, 12, 13, 14, 19, 20, 21, 22, 23);
        }

        List<ParkourRecord> records = leaderboardService.getTopRecords(courseId);

        for (int rank = 1; rank <= slots.size(); rank++) {
            int slot = slots.get(rank - 1);
            if (slot < 0 || slot >= size) {
                continue;
            }

            if (rank <= records.size()) {
                ParkourRecord record = records.get(rank - 1);
                ItemStack head = createPlayerHead(record, rank);
                inv.setItem(slot, head);
            } else {
                Material emptyMat = Material.matchMaterial(config.getString("leaderboard-gui.empty-slot.material", "BARRIER"));
                if (emptyMat == null) emptyMat = Material.BARRIER;
                String name = MessageUtil.format(config.getString("leaderboard-gui.empty-slot.name", "<red>#{rank} - Empty</red>"), "rank", String.valueOf(rank));
                List<String> lore = config.getStringList("leaderboard-gui.empty-slot.lore");
                inv.setItem(slot, createItem(emptyMat, name, lore));
            }
        }

        // Personal best item
        int pbSlot = config.getInt("leaderboard-gui.personal-best.slot", 31);
        if (pbSlot >= 0 && pbSlot < size) {
            PlayerStats stats = leaderboardService.getStats(courseId, player.getName());
            int rank = leaderboardService.getRank(courseId, player.getName());
            String rankStr = rank > 0 ? "#" + rank : "Unranked";
            String timeStr = stats.getBestTimeMs() > 0 ? TimeUtil.formatTime(stats.getBestTimeMs()) : "N/A";

            Material pbMat = Material.matchMaterial(config.getString("leaderboard-gui.personal-best.material", "NETHER_STAR"));
            if (pbMat == null) pbMat = Material.NETHER_STAR;
            String pbName = config.getString("leaderboard-gui.personal-best.name", "<gold><bold>Your Best Time</bold></gold>");
            List<String> rawLore = config.getStringList("leaderboard-gui.personal-best.lore");
            List<String> formattedLore = new ArrayList<>();
            for (String l : rawLore) {
                formattedLore.add(MessageUtil.format(l,
                        "course", courseDisplay,
                        "time", timeStr,
                        "rank", rankStr,
                        "completions", String.valueOf(stats.getCompletions()),
                        "attempts", String.valueOf(stats.getAttempts())
                ));
            }
            inv.setItem(pbSlot, createItem(pbMat, pbName, formattedLore));
        }

        // Close button
        int closeSlot = config.getInt("leaderboard-gui.close-button.slot", 40);
        if (closeSlot >= 0 && closeSlot < size) {
            Material closeMat = Material.matchMaterial(config.getString("leaderboard-gui.close-button.material", "BARRIER"));
            if (closeMat == null) closeMat = Material.BARRIER;
            String closeName = config.getString("leaderboard-gui.close-button.name", "<red><bold>Close</bold></red>");
            inv.setItem(closeSlot, createItem(closeMat, closeName, List.of()));
        }

        player.openInventory(inv);
    }

    public void openStats(Player player, OfflinePlayer target) {
        FileConfiguration config = plugin.getConfig();
        int size = config.getInt("stats-gui.size", 36);
        String title = MessageUtil.format(config.getString("stats-gui.title", "<dark_gray>Parkour Stats: <gold>{player}</gold></dark_gray>"), "player", target.getName() != null ? target.getName() : "Player");

        StatsGuiHolder holder = new StatsGuiHolder(target.getUniqueId());
        Inventory inv = Bukkit.createInventory(holder, size, MessageUtil.parse(title));
        holder.bind(inv);

        // Fill background
        Material fillMat = Material.matchMaterial(config.getString("stats-gui.filler.material", "GRAY_STAINED_GLASS_PANE"));
        if (fillMat != null) {
            ItemStack filler = createItem(fillMat, config.getString("stats-gui.filler.name", " "), List.of());
            for (int i = 0; i < size; i++) {
                inv.setItem(i, filler);
            }
        }

        Collection<Course> courses = courseService.getAllCourses();
        Material courseMat = Material.matchMaterial(config.getString("stats-gui.course-item.material", "COMPASS"));
        if (courseMat == null) courseMat = Material.COMPASS;

        int slot = 10;
        for (Course course : courses) {
            if (slot >= size - 9) {
                break;
            }
            if ((slot + 1) % 9 == 0) {
                slot += 2;
            }

            String targetName = target.getName() != null ? target.getName() : "Unknown";
            PlayerStats stats = leaderboardService.getStats(course.getId(), targetName);
            int rank = leaderboardService.getRank(course.getId(), targetName);
            String rankStr = rank > 0 ? "#" + rank : "Unranked";
            String bestTimeStr = stats.getBestTimeMs() > 0 ? TimeUtil.formatTime(stats.getBestTimeMs()) : "N/A";

            String name = MessageUtil.format(config.getString("stats-gui.course-item.name", "<gold><bold>{course}</bold></gold>"), "course", course.getDisplayName());
            List<String> rawLore = config.getStringList("stats-gui.course-item.lore");
            List<String> formattedLore = new ArrayList<>();
            for (String l : rawLore) {
                formattedLore.add(MessageUtil.format(l,
                        "course", course.getDisplayName(),
                        "best_time", bestTimeStr,
                        "rank", rankStr,
                        "completions", String.valueOf(stats.getCompletions()),
                        "attempts", String.valueOf(stats.getAttempts())
                ));
            }

            ItemStack item = createItem(courseMat, name, formattedLore);
            // Store courseId in PDC or metadata if needed, but we can also match slot/course
            inv.setItem(slot++, item);
        }

        // Close button
        int closeSlot = config.getInt("stats-gui.close-button.slot", 31);
        if (closeSlot >= 0 && closeSlot < size) {
            Material closeMat = Material.matchMaterial(config.getString("stats-gui.close-button.material", "BARRIER"));
            if (closeMat == null) closeMat = Material.BARRIER;
            String closeName = config.getString("stats-gui.close-button.name", "<red><bold>Close</bold></red>");
            inv.setItem(closeSlot, createItem(closeMat, closeName, List.of()));
        }

        player.openInventory(inv);
    }

    private ItemStack createPlayerHead(ParkourRecord record, int rank) {
        FileConfiguration config = plugin.getConfig();
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            // Prefer stored username so offline entries never show "Unknown"
            OfflinePlayer p = Bukkit.getOfflinePlayer(record.playerName());
            meta.setOwningPlayer(p);

            String nameTemplate = config.getString("leaderboard-gui.entry-format.name", "<gold>#{rank}</gold> <yellow>{player}</yellow>");
            meta.displayName(MessageUtil.parse(MessageUtil.format(nameTemplate, "rank", String.valueOf(rank), "player", record.playerName())));

            List<String> loreTemplate = config.getStringList("leaderboard-gui.entry-format.lore");
            List<Component> lore = new ArrayList<>();
            String formattedTime = TimeUtil.formatTime(record.timeMs());
            String formattedDate = DATE_FORMAT.format(new Date(record.timestamp()));
            for (String line : loreTemplate) {
                lore.add(MessageUtil.parse(MessageUtil.format(line, "time", formattedTime, "date", formattedDate)));
            }
            meta.lore(lore);
            head.setItemMeta(meta);
        }
        return head;
    }

    private static ItemStack createItem(Material mat, String name, List<String> loreLines) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (name != null && !name.isBlank()) {
                meta.displayName(MessageUtil.parse(name));
            }
            if (loreLines != null && !loreLines.isEmpty()) {
                List<Component> lore = new ArrayList<>();
                for (String l : loreLines) {
                    lore.add(MessageUtil.parse(l));
                }
                meta.lore(lore);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
