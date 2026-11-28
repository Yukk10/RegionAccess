package dev.yukio.regionaccess.config;

import net.kyori.adventure.title.Title;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public record PluginSettings(
        Set<String> commandAliases,
        long intervalMillis,
        int maxWarnings,
        boolean resetOnLeave,
        boolean showTitle,
        boolean showActionBar,
        boolean showChat,
        Title.Times titleTimes,
        List<String> punishCommands,
        Set<String> signWorlds,
        Set<String> entityWorlds
) {

    public static PluginSettings load(FileConfiguration cfg) {
        Set<String> aliases = lowerSet(cfg.getStringList("command-aliases"));
        if (aliases.isEmpty()) {
            aliases = Set.of("rg", "region", "regions");
        }

        long interval = Math.max(1, cfg.getLong("warnings.interval-seconds", 5)) * 1000L;
        int max = Math.max(1, cfg.getInt("warnings.max-warnings", 10));

        Title.Times times = Title.Times.times(
                ticks(cfg.getInt("warnings.title-fade-in", 5)),
                ticks(cfg.getInt("warnings.title-stay", 70)),
                ticks(cfg.getInt("warnings.title-fade-out", 15)));


        return new PluginSettings(
                aliases,
                interval,
                max,
                cfg.getBoolean("warnings.reset-on-leave", true),
                cfg.getBoolean("warnings.show-title", true),
                cfg.getBoolean("warnings.show-actionbar", true),
                cfg.getBoolean("warnings.show-chat", false),
                times,
                List.copyOf(cfg.getStringList("warnings.punish-commands")),
                lowerSet(cfg.getStringList("signs.worlds")),
                lowerSet(cfg.getStringList("entities.worlds")));
    }

    public boolean signsAllowedIn(String worldName) {
        return matches(signWorlds, worldName);
    }

    public boolean entitiesAllowedIn(String worldName) {
        return matches(entityWorlds, worldName);
    }

    private static boolean matches(Set<String> worlds, String worldName) {
        return worlds.contains("*") || worlds.contains(worldName.toLowerCase(Locale.ROOT));
    }

    private static Duration ticks(int ticks) {
        return Duration.ofMillis(Math.max(0, ticks) * 50L);
    }

    private static Set<String> lowerSet(List<String> list) {
        return list.stream()
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
}
