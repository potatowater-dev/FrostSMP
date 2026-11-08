package com.frostsmp.parkour.placeholder;

import com.frostsmp.parkour.model.Course;
import com.frostsmp.parkour.model.ParkourRecord;
import com.frostsmp.parkour.model.PlayerStats;
import com.frostsmp.parkour.service.CourseService;
import com.frostsmp.parkour.service.LeaderboardService;
import com.frostsmp.parkour.service.ParkourSessionService;
import com.frostsmp.parkour.session.ParkourSession;
import com.frostsmp.parkour.util.TimeUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ParkourExpansion extends PlaceholderExpansion {

    // Standard frost placeholder style: %frostparkour_top_1_<course>_player% and %frostparkour_top_1_have_<course>%
    private static final Pattern TOP_N_PLAYER = Pattern.compile("^top_(\\d+)_([a-zA-Z0-9_]+)_player$");
    private static final Pattern TOP_N_HAVE = Pattern.compile("^top_(\\d+)_have_([a-zA-Z0-9_]+)$");
    private static final Pattern TOP_N_TIME = Pattern.compile("^top_(\\d+)_([a-zA-Z0-9_]+)_time$");

    // Alternate style: %frostparkour_top_player_<course>_1% and %frostparkour_top_time_<course>_1%
    private static final Pattern TOP_PLAYER_ALT = Pattern.compile("^top_player_([a-zA-Z0-9_]+)_(\\d+)$");
    private static final Pattern TOP_TIME_ALT = Pattern.compile("^top_time_([a-zA-Z0-9_]+)_(\\d+)$");
    private static final Pattern TOP_TIME_RAW_ALT = Pattern.compile("^top_time_raw_([a-zA-Z0-9_]+)_(\\d+)$");

    private static final Pattern BEST_TIME = Pattern.compile("^(?:best_time_|time_|value_)?([a-zA-Z0-9_]+)$");
    private static final Pattern BEST_TIME_RAW = Pattern.compile("^best_time_raw_([a-zA-Z0-9_]+)$");
    private static final Pattern RANK = Pattern.compile("^(?:rank_|place_)([a-zA-Z0-9_]+)$");
    private static final Pattern COMPLETIONS = Pattern.compile("^completions_([a-zA-Z0-9_]+)$");
    private static final Pattern ATTEMPTS = Pattern.compile("^attempts_([a-zA-Z0-9_]+)$");

    private final JavaPlugin plugin;
    private final CourseService courseService;
    private final LeaderboardService leaderboardService;
    private final ParkourSessionService sessionService;

    public ParkourExpansion(JavaPlugin plugin, CourseService courseService,
                            LeaderboardService leaderboardService, ParkourSessionService sessionService) {
        this.plugin = plugin;
        this.courseService = courseService;
        this.leaderboardService = leaderboardService;
        this.sessionService = sessionService;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "frostparkour";
    }

    @Override
    public @NotNull String getAuthor() {
        return "FrostSMP";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @Nullable String onRequest(@Nullable OfflinePlayer offlinePlayer, @NotNull String params) {
        if (params.isEmpty()) {
            return "";
        }
        String lower = params.toLowerCase(Locale.ROOT).trim();

        // 1. %frostparkour_top_<rank>_<course>_player%
        Matcher m1 = TOP_N_PLAYER.matcher(lower);
        if (m1.matches()) {
            int rank = Integer.parseInt(m1.group(1));
            String course = m1.group(2);
            List<ParkourRecord> records = leaderboardService.getTopRecords(course);
            if (rank > 0 && rank <= records.size()) {
                return records.get(rank - 1).playerName();
            }
            return "-";
        }

        // 2. %frostparkour_top_<rank>_have_<course>% or %frostparkour_top_<rank>_<course>_time%
        Matcher m2 = TOP_N_HAVE.matcher(lower);
        if (m2.matches()) {
            int rank = Integer.parseInt(m2.group(1));
            String course = m2.group(2);
            List<ParkourRecord> records = leaderboardService.getTopRecords(course);
            if (rank > 0 && rank <= records.size()) {
                return TimeUtil.formatTime(records.get(rank - 1).timeMs());
            }
            return "N/A";
        }
        Matcher m2b = TOP_N_TIME.matcher(lower);
        if (m2b.matches()) {
            int rank = Integer.parseInt(m2b.group(1));
            String course = m2b.group(2);
            List<ParkourRecord> records = leaderboardService.getTopRecords(course);
            if (rank > 0 && rank <= records.size()) {
                return TimeUtil.formatTime(records.get(rank - 1).timeMs());
            }
            return "N/A";
        }

        // 3. %frostparkour_top_player_<course>_<rank>%
        Matcher topPlayerMatcher = TOP_PLAYER_ALT.matcher(lower);
        if (topPlayerMatcher.matches()) {
            String course = topPlayerMatcher.group(1);
            int rank = Integer.parseInt(topPlayerMatcher.group(2));
            List<ParkourRecord> records = leaderboardService.getTopRecords(course);
            if (rank > 0 && rank <= records.size()) {
                return records.get(rank - 1).playerName();
            }
            return "-";
        }

        // 4. %frostparkour_top_time_<course>_<rank>%
        Matcher topTimeMatcher = TOP_TIME_ALT.matcher(lower);
        if (topTimeMatcher.matches()) {
            String course = topTimeMatcher.group(1);
            int rank = Integer.parseInt(topTimeMatcher.group(2));
            List<ParkourRecord> records = leaderboardService.getTopRecords(course);
            if (rank > 0 && rank <= records.size()) {
                return TimeUtil.formatTime(records.get(rank - 1).timeMs());
            }
            return "N/A";
        }

        // 5. %frostparkour_top_time_raw_<course>_<rank>%
        Matcher topTimeRawMatcher = TOP_TIME_RAW_ALT.matcher(lower);
        if (topTimeRawMatcher.matches()) {
            String course = topTimeRawMatcher.group(1);
            int rank = Integer.parseInt(topTimeRawMatcher.group(2));
            List<ParkourRecord> records = leaderboardService.getTopRecords(course);
            if (rank > 0 && rank <= records.size()) {
                return String.valueOf(records.get(rank - 1).timeMs());
            }
            return "0";
        }

        // Player-specific placeholders
        if (offlinePlayer != null) {
            String playerName = offlinePlayer.getName();
            if (playerName == null || playerName.isBlank()) {
                playerName = offlinePlayer.getUniqueId().toString();
            }

            Matcher rankMatcher = RANK.matcher(lower);
            if (rankMatcher.matches()) {
                String course = rankMatcher.group(1);
                int r = leaderboardService.getRank(course, playerName);
                return r > 0 ? String.valueOf(r) : "-";
            }

            Matcher bestRawMatcher = BEST_TIME_RAW.matcher(lower);
            if (bestRawMatcher.matches()) {
                String course = bestRawMatcher.group(1);
                PlayerStats stats = leaderboardService.getStats(course, playerName);
                return String.valueOf(Math.max(0, stats.getBestTimeMs()));
            }

            Matcher compMatcher = COMPLETIONS.matcher(lower);
            if (compMatcher.matches()) {
                String course = compMatcher.group(1);
                return String.valueOf(leaderboardService.getStats(course, playerName).getCompletions());
            }

            Matcher attMatcher = ATTEMPTS.matcher(lower);
            if (attMatcher.matches()) {
                String course = attMatcher.group(1);
                return String.valueOf(leaderboardService.getStats(course, playerName).getAttempts());
            }

            if (lower.equals("status")) {
                return sessionService.isInParkour(offlinePlayer.getUniqueId()) ? "Playing" : "Idle";
            }

            if (lower.equals("current_course")) {
                ParkourSession session = sessionService.getSession(offlinePlayer.getUniqueId());
                if (session != null) {
                    Course c = courseService.getCourse(session.getCourseId());
                    return c != null ? c.getDisplayName() : session.getCourseId();
                }
                return "None";
            }

            if (lower.equals("current_time")) {
                ParkourSession session = sessionService.getSession(offlinePlayer.getUniqueId());
                return session != null ? TimeUtil.formatTime(session.getElapsedTimeMs()) : "00:00:000";
            }

            if (lower.equals("current_checkpoint")) {
                ParkourSession session = sessionService.getSession(offlinePlayer.getUniqueId());
                return session != null ? String.valueOf(Math.max(0, session.getCurrentCheckpointIndex() + 1)) : "0";
            }

            Matcher bestMatcher = BEST_TIME.matcher(lower);
            if (bestMatcher.matches()) {
                String course = bestMatcher.group(1);
                if (courseService.getCourse(course) != null || !leaderboardService.getTopRecords(course).isEmpty()) {
                    PlayerStats stats = leaderboardService.getStats(course, playerName);
                    return stats.getBestTimeMs() > 0 ? TimeUtil.formatTime(stats.getBestTimeMs()) : "N/A";
                }
            }
        }

        return null;
    }

    @Override
    public @Nullable String onPlaceholderRequest(@Nullable Player player, @NotNull String params) {
        return onRequest(player, params);
    }
}
