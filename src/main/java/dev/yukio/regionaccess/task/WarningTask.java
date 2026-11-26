package dev.yukio.regionaccess.task;

import dev.yukio.regionaccess.Permissions;
import dev.yukio.regionaccess.RegionAccessPlugin;
import dev.yukio.regionaccess.config.Messages;
import dev.yukio.regionaccess.config.PluginSettings;
import dev.yukio.regionaccess.region.Violation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Раз в секунду проверяет всех игроков онлайн. Посторонним на закрытой территории
 * раз в warnings.interval-seconds показывает предупреждение; после max-warnings
 * предупреждений выполняет punish-commands (телепорт на спавн).
 */
public final class WarningTask extends BukkitRunnable implements Listener {

    private static final class State {
        String violationKey;
        int count;
        long lastWarn;
    }

    private final RegionAccessPlugin plugin;
    private final Map<UUID, State> states = new HashMap<>();

    public WarningTask(RegionAccessPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        PluginSettings settings = plugin.settings();
        long now = System.currentTimeMillis();

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            Violation violation = player.hasPermission(Permissions.BYPASS)
                    ? null
                    : plugin.regionService().findViolation(player);

            State state = states.get(uuid);
            if (violation == null) {
                if (state != null && settings.resetOnLeave()) {
                    states.remove(uuid);
                }
                continue;
            }

            if (state == null) {
                state = new State();
                states.put(uuid, state);
            } else if (!violation.key().equals(state.violationKey) && settings.resetOnLeave()) {
                state.count = 0;
                state.lastWarn = 0;
            }
            state.violationKey = violation.key();

            if (now - state.lastWarn < settings.intervalMillis()) {
                continue;
            }

            if (state.count >= settings.maxWarnings()) {
                punish(player, violation, settings);
                states.remove(uuid);
                continue;
            }

            state.count++;
            state.lastWarn = now;
            warn(player, violation, state.count, settings);
        }
    }

    private void warn(Player player, Violation violation, int count, PluginSettings settings) {
        Messages msg = plugin.messages();
        String base = violation.type().warningKey();
        TagResolver tags = TagResolver.resolver(
                Messages.region(violation.regionId()),
                Messages.number("count", count),
                Messages.number("max", settings.maxWarnings()),
                Messages.number("remaining", settings.maxWarnings() - count));

        if (settings.showTitle()) {
            Component title = msg.get(base + ".title", tags);
            Component subtitle = msg.get(base + ".subtitle", tags);
            if (title != null || subtitle != null) {
                player.showTitle(Title.title(
                        title == null ? Component.empty() : title,
                        subtitle == null ? Component.empty() : subtitle,
                        settings.titleTimes()));
            }
        }
        if (settings.showActionBar()) {
            Component bar = msg.get(base + ".actionbar", tags);
            if (bar != null) {
                player.sendActionBar(bar);
            }
        }
        if (settings.showChat()) {
            msg.send(player, base + ".chat", tags);
        }
    }

    private void punish(Player player, Violation violation, PluginSettings settings) {
        for (String command : settings.punishCommands()) {
            String cmd = command.replace("{player}", player.getName());
            if (cmd.startsWith("/")) {
                cmd = cmd.substring(1);
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
        player.clearTitle();
        plugin.messages().send(player, violation.type().punishedKey(), Messages.region(violation.regionId()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        states.remove(event.getPlayer().getUniqueId());
    }
}
