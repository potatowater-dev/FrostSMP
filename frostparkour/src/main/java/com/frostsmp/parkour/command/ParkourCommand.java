package com.frostsmp.parkour.command;

import com.frostsmp.parkour.FrostParkourPlugin;
import com.frostsmp.parkour.config.PluginConfig;
import com.frostsmp.parkour.gui.ParkourGuiService;
import com.frostsmp.parkour.model.Course;
import com.frostsmp.parkour.service.CourseService;
import com.frostsmp.parkour.service.LeaderboardService;
import com.frostsmp.parkour.service.ParkourSessionService;
import com.frostsmp.parkour.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public final class ParkourCommand implements CommandExecutor, TabCompleter {

    private final FrostParkourPlugin plugin;
    private final PluginConfig config;
    private final CourseService courseService;
    private final LeaderboardService leaderboardService;
    private final ParkourSessionService sessionService;
    private final ParkourGuiService guiService;

    public ParkourCommand(FrostParkourPlugin plugin, PluginConfig config, CourseService courseService,
                          LeaderboardService leaderboardService, ParkourSessionService sessionService,
                          ParkourGuiService guiService) {
        this.plugin = plugin;
        this.config = config;
        this.courseService = courseService;
        this.leaderboardService = leaderboardService;
        this.sessionService = sessionService;
        this.guiService = guiService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "start" -> handleStart(sender, args);
            case "quit", "leave" -> handleQuit(sender);
            case "restart" -> handleRestart(sender);
            case "checkpoint", "cp" -> handleCheckpoint(sender);
            case "top", "leaderboard", "lb" -> handleTop(sender, args);
            case "stats" -> handleStats(sender, args);
            case "list" -> handleList(sender);

            // Admin commands
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "setstart" -> handleSetStart(sender, args);
            case "setend" -> handleSetEnd(sender, args);
            case "addcheckpoint", "addcp" -> handleAddCheckpoint(sender, args);
            case "removecheckpoint", "delcheckpoint", "removecp", "delcp" -> handleRemoveCheckpoint(sender, args);
            case "setdisplayname", "setname" -> handleSetDisplayName(sender, args);
            case "setfally" -> handleSetFallY(sender, args);
            case "resetleaderboard", "resetlb" -> handleResetLeaderboard(sender, args);
            case "reload" -> handleReload(sender);

            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleStart(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, config.msg("players-only"));
            return;
        }
        if (!player.hasPermission("frostsmp.parkour.use")) {
            MessageUtil.send(player, config.msg("no-permission"));
            return;
        }
        if (args.length < 2) {
            MessageUtil.send(player, "<gray>Usage: /parkour start <course></gray>");
            return;
        }
        Course course = courseService.getCourse(args[1]);
        if (course == null) {
            MessageUtil.send(player, MessageUtil.format(config.msg("course-not-found"), "course", args[1]));
            return;
        }
        if (!course.isConfigured()) {
            MessageUtil.send(player, "<red>This course is not fully configured yet (needs start & end points).</red>");
            return;
        }
        sessionService.startCourse(player, course);
    }

    private void handleQuit(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, config.msg("players-only"));
            return;
        }
        sessionService.quitParkour(player, true);
    }

    private void handleRestart(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, config.msg("players-only"));
            return;
        }
        if (!sessionService.isInParkour(player.getUniqueId())) {
            MessageUtil.send(player, config.msg("not-in-parkour"));
            return;
        }
        sessionService.restartCourse(player);
    }

    private void handleCheckpoint(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, config.msg("players-only"));
            return;
        }
        if (!sessionService.isInParkour(player.getUniqueId())) {
            MessageUtil.send(player, config.msg("not-in-parkour"));
            return;
        }
        sessionService.teleportLastCheckpoint(player);
    }

    private void handleTop(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, config.msg("players-only"));
            return;
        }
        if (!player.hasPermission("frostsmp.parkour.use")) {
            MessageUtil.send(player, config.msg("no-permission"));
            return;
        }
        if (args.length < 2) {
            // If currently in parkour, default to current course
            if (sessionService.isInParkour(player.getUniqueId())) {
                String cId = sessionService.getSession(player.getUniqueId()).getCourseId();
                guiService.openLeaderboard(player, cId);
                return;
            }
            MessageUtil.send(player, "<gray>Usage: /parkour top <course></gray>");
            return;
        }
        Course course = courseService.getCourse(args[1]);
        if (course == null) {
            MessageUtil.send(player, MessageUtil.format(config.msg("course-not-found"), "course", args[1]));
            return;
        }
        guiService.openLeaderboard(player, course.getId());
    }

    private void handleStats(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, config.msg("players-only"));
            return;
        }
        if (!player.hasPermission("frostsmp.parkour.use")) {
            MessageUtil.send(player, config.msg("no-permission"));
            return;
        }
        OfflinePlayer target = player;
        if (args.length >= 2) {
            OfflinePlayer searched = Bukkit.getOfflinePlayer(args[1]);
            if (searched.hasPlayedBefore() || searched.isOnline()) {
                target = searched;
            }
        }
        guiService.openStats(player, target);
    }

    private void handleList(CommandSender sender) {
        Collection<Course> list = courseService.getAllCourses();
        if (list.isEmpty()) {
            MessageUtil.send(sender, "<gray>No parkour courses available.</gray>");
            return;
        }
        MessageUtil.send(sender, "<gold><bold>Available Parkour Courses:</bold></gold>");
        for (Course c : list) {
            String status = c.isConfigured() ? "<green>[Ready]</green>" : "<red>[Incomplete]</red>";
            int cpCount = c.getCheckpoints().size();
            MessageUtil.send(sender, "<gray>·</gray> <yellow>" + c.getId() + "</yellow> <white>(" + c.getDisplayName() + ")</white> " + status + " <gray>- Checkpoints: " + cpCount + "</gray>");
        }
    }

    // Admin Handlers
    private void handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("frostsmp.parkour.admin")) {
            MessageUtil.send(sender, config.msg("no-permission"));
            return;
        }
        if (args.length < 2) {
            MessageUtil.send(sender, "<gray>Usage: /parkour create <id> [displayName]</gray>");
            return;
        }
        String id = args[1];
        String displayName = args.length >= 3 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : id;
        if (!courseService.createCourse(id, displayName)) {
            MessageUtil.send(sender, MessageUtil.format(config.msg("course-exists"), "course", id));
            return;
        }
        MessageUtil.send(sender, MessageUtil.format(config.msg("course-created"), "course", id));
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("frostsmp.parkour.admin")) {
            MessageUtil.send(sender, config.msg("no-permission"));
            return;
        }
        if (args.length < 2) {
            MessageUtil.send(sender, "<gray>Usage: /parkour delete <id></gray>");
            return;
        }
        String id = args[1];
        if (!courseService.deleteCourse(id)) {
            MessageUtil.send(sender, MessageUtil.format(config.msg("course-not-found"), "course", id));
            return;
        }
        MessageUtil.send(sender, MessageUtil.format(config.msg("course-deleted"), "course", id));
    }

    private void handleSetStart(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, config.msg("players-only"));
            return;
        }
        if (!player.hasPermission("frostsmp.parkour.admin")) {
            MessageUtil.send(player, config.msg("no-permission"));
            return;
        }
        if (args.length < 2) {
            MessageUtil.send(player, "<gray>Usage: /parkour setstart <course></gray>");
            return;
        }
        Course course = courseService.getCourse(args[1]);
        if (course == null) {
            MessageUtil.send(player, MessageUtil.format(config.msg("course-not-found"), "course", args[1]));
            return;
        }
        course.setStartLocation(player.getLocation());
        courseService.save();
        MessageUtil.send(player, MessageUtil.format(config.msg("start-set"), "course", course.getId()));
    }

    private void handleSetEnd(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, config.msg("players-only"));
            return;
        }
        if (!player.hasPermission("frostsmp.parkour.admin")) {
            MessageUtil.send(player, config.msg("no-permission"));
            return;
        }
        if (args.length < 2) {
            MessageUtil.send(player, "<gray>Usage: /parkour setend <course></gray>");
            return;
        }
        Course course = courseService.getCourse(args[1]);
        if (course == null) {
            MessageUtil.send(player, MessageUtil.format(config.msg("course-not-found"), "course", args[1]));
            return;
        }
        course.setEndLocation(player.getLocation());
        courseService.save();
        MessageUtil.send(player, MessageUtil.format(config.msg("end-set"), "course", course.getId()));
    }

    private void handleAddCheckpoint(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, config.msg("players-only"));
            return;
        }
        if (!player.hasPermission("frostsmp.parkour.admin")) {
            MessageUtil.send(player, config.msg("no-permission"));
            return;
        }
        if (args.length < 2) {
            MessageUtil.send(player, "<gray>Usage: /parkour addcheckpoint <course></gray>");
            return;
        }
        Course course = courseService.getCourse(args[1]);
        if (course == null) {
            MessageUtil.send(player, MessageUtil.format(config.msg("course-not-found"), "course", args[1]));
            return;
        }
        course.addCheckpoint(player.getLocation());
        courseService.save();
        int idx = course.getCheckpoints().size();
        MessageUtil.send(player, MessageUtil.format(config.msg("checkpoint-added"), "course", course.getId(), "index", String.valueOf(idx)));
    }

    private void handleRemoveCheckpoint(CommandSender sender, String[] args) {
        if (!sender.hasPermission("frostsmp.parkour.admin")) {
            MessageUtil.send(sender, config.msg("no-permission"));
            return;
        }
        if (args.length < 3) {
            MessageUtil.send(sender, "<gray>Usage: /parkour removecheckpoint <course> <index></gray>");
            return;
        }
        Course course = courseService.getCourse(args[1]);
        if (course == null) {
            MessageUtil.send(sender, MessageUtil.format(config.msg("course-not-found"), "course", args[1]));
            return;
        }
        try {
            int index = Integer.parseInt(args[2]) - 1;
            if (!course.removeCheckpoint(index)) {
                MessageUtil.send(sender, MessageUtil.format(config.msg("invalid-checkpoint-index"), "total", String.valueOf(course.getCheckpoints().size())));
                return;
            }
            courseService.save();
            MessageUtil.send(sender, MessageUtil.format(config.msg("checkpoint-removed"), "course", course.getId(), "index", args[2]));
        } catch (NumberFormatException e) {
            MessageUtil.send(sender, "<red>Index must be a valid number.</red>");
        }
    }

    private void handleSetDisplayName(CommandSender sender, String[] args) {
        if (!sender.hasPermission("frostsmp.parkour.admin")) {
            MessageUtil.send(sender, config.msg("no-permission"));
            return;
        }
        if (args.length < 3) {
            MessageUtil.send(sender, "<gray>Usage: /parkour setdisplayname <course> <name></gray>");
            return;
        }
        Course course = courseService.getCourse(args[1]);
        if (course == null) {
            MessageUtil.send(sender, MessageUtil.format(config.msg("course-not-found"), "course", args[1]));
            return;
        }
        String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        course.setDisplayName(name);
        courseService.save();
        MessageUtil.send(sender, MessageUtil.format(config.msg("displayname-set"), "course", course.getId()));
    }

    private void handleSetFallY(CommandSender sender, String[] args) {
        if (!sender.hasPermission("frostsmp.parkour.admin")) {
            MessageUtil.send(sender, config.msg("no-permission"));
            return;
        }
        if (args.length < 3) {
            MessageUtil.send(sender, "<gray>Usage: /parkour setfally <course> <y></gray>");
            return;
        }
        Course course = courseService.getCourse(args[1]);
        if (course == null) {
            MessageUtil.send(sender, MessageUtil.format(config.msg("course-not-found"), "course", args[1]));
            return;
        }
        try {
            double y = Double.parseDouble(args[2]);
            course.setCustomFallY(y);
            courseService.save();
            MessageUtil.send(sender, MessageUtil.format(config.msg("fally-set"), "course", course.getId(), "y", String.valueOf(y)));
        } catch (NumberFormatException e) {
            MessageUtil.send(sender, "<red>Fall Y must be a valid number.</red>");
        }
    }

    private void handleResetLeaderboard(CommandSender sender, String[] args) {
        if (!sender.hasPermission("frostsmp.parkour.admin")) {
            MessageUtil.send(sender, config.msg("no-permission"));
            return;
        }
        if (args.length < 2) {
            MessageUtil.send(sender, "<gray>Usage: /parkour resetleaderboard <course></gray>");
            return;
        }
        Course course = courseService.getCourse(args[1]);
        if (course == null) {
            MessageUtil.send(sender, MessageUtil.format(config.msg("course-not-found"), "course", args[1]));
            return;
        }
        leaderboardService.resetLeaderboard(course.getId());
        MessageUtil.send(sender, MessageUtil.format(config.msg("leaderboard-reset"), "course", course.getId()));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("frostsmp.parkour.admin")) {
            MessageUtil.send(sender, config.msg("no-permission"));
            return;
        }
        plugin.reloadPlugin();
        MessageUtil.send(sender, config.msg("reloaded"));
    }

    private void sendHelp(CommandSender sender) {
        MessageUtil.send(sender, config.rawMsg("usage"));
        if (sender.hasPermission("frostsmp.parkour.admin")) {
            MessageUtil.send(sender, config.rawMsg("admin-usage"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("start", "quit", "restart", "checkpoint", "top", "stats", "list"));
            if (sender.hasPermission("frostsmp.parkour.admin")) {
                options.addAll(List.of("create", "delete", "setstart", "setend", "addcheckpoint", "removecheckpoint", "setdisplayname", "setfally", "resetleaderboard", "reload"));
            }
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (String o : options) {
                if (o.startsWith(prefix)) {
                    list.add(o);
                }
            }
            return list;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (List.of("start", "top", "leaderboard", "lb", "delete", "setstart", "setend", "addcheckpoint", "removecheckpoint", "setdisplayname", "setfally", "resetleaderboard").contains(sub)) {
                String prefix = args[1].toLowerCase(Locale.ROOT);
                for (Course c : courseService.getAllCourses()) {
                    if (c.getId().startsWith(prefix)) {
                        list.add(c.getId());
                    }
                }
                return list;
            }
            if ("stats".equals(sub)) {
                String prefix = args[1].toLowerCase(Locale.ROOT);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                        list.add(p.getName());
                    }
                }
                return list;
            }
        }

        return list;
    }
}
