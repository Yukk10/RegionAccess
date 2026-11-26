package dev.yukio.regionaccess.command;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import dev.yukio.regionaccess.Permissions;
import dev.yukio.regionaccess.RegionAccessPlugin;
import dev.yukio.regionaccess.config.Messages;
import dev.yukio.regionaccess.region.RegionService;
import dev.yukio.regionaccess.storage.RegionData;
import dev.yukio.regionaccess.storage.RegionDataStore;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.TabCompleteEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Перехватывает /rg whitelist ... и /rg blacklist ... до WorldGuard.
 * Все остальные подкоманды /rg проходят в WorldGuard без изменений.
 */
public final class RegionCommandListener implements Listener {

    private static final List<String> SUBCOMMANDS = List.of("whitelist", "blacklist");
    private static final List<String> WHITELIST_ARGS = List.of("on", "off");
    private static final List<String> BLACKLIST_ARGS = List.of("add", "remove", "list");

    private final RegionAccessPlugin plugin;

    public RegionCommandListener(RegionAccessPlugin plugin) {
        this.plugin = plugin;
    }

    private Messages msg() {
        return plugin.messages();
    }

    private RegionService service() {
        return plugin.regionService();
    }

    private RegionDataStore store() {
        return plugin.dataStore();
    }

    // ------------------------------------------------------------------ команды

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (!message.startsWith("/")) {
            return;
        }
        String[] parts = message.substring(1).trim().split("\\s+");
        if (parts.length < 2 || !plugin.settings().commandAliases().contains(parts[0].toLowerCase(Locale.ROOT))) {
            return;
        }
        String sub = parts[1].toLowerCase(Locale.ROOT);
        if (!SUBCOMMANDS.contains(sub)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!player.hasPermission(Permissions.USE)) {
            msg().send(player, "no-permission");
            return;
        }

        ProtectedRegion region = managedRegion(player);
        if (region == null) {
            return;
        }

        if (sub.equals("whitelist")) {
            handleWhitelist(player, region, parts);
        } else {
            handleBlacklist(player, region, parts);
        }
    }

    /** Регион, которым игрок управляет в текущей точке (с наибольшим приоритетом). */
    private ProtectedRegion managedRegion(Player player) {
        List<ProtectedRegion> regions = service().regionsSorted(player);
        if (regions.isEmpty()) {
            msg().send(player, "not-in-region");
            return null;
        }
        boolean any = player.hasPermission(Permissions.MANAGE_OTHERS);
        LocalPlayer local = service().wrap(player);
        for (ProtectedRegion region : regions) {
            if (any || region.isOwner(local)) {
                return region;
            }
        }
        msg().send(player, "not-owner");
        return null;
    }

    private void handleWhitelist(Player player, ProtectedRegion region, String[] parts) {
        String world = player.getWorld().getName();
        String id = region.getId();
        RegionData current = store().get(world, id);
        boolean enabled = current != null && current.whitelist();

        String arg = parts.length >= 3 ? parts[2].toLowerCase(Locale.ROOT) : "";
        switch (arg) {
            case "on" -> {
                if (enabled) {
                    msg().send(player, "whitelist-already-enabled", Messages.region(id));
                    return;
                }
                store().getOrCreate(world, id).setWhitelist(true);
                store().saveAndCleanup(world, id);
                msg().send(player, "whitelist-enabled", Messages.region(id));
            }
            case "off" -> {
                if (!enabled) {
                    msg().send(player, "whitelist-already-disabled", Messages.region(id));
                    return;
                }
                current.setWhitelist(false);
                store().saveAndCleanup(world, id);
                msg().send(player, "whitelist-disabled", Messages.region(id));
            }
            default -> msg().send(player, "whitelist-usage",
                    Messages.region(id),
                    Placeholder.component("state", msg().getOrEmpty(enabled ? "state-on" : "state-off")));
        }
    }

    private void handleBlacklist(Player player, ProtectedRegion region, String[] parts) {
        String world = player.getWorld().getName();
        String id = region.getId();
        String action = parts.length >= 3 ? parts[2].toLowerCase(Locale.ROOT) : "";

        switch (action) {
            case "list" -> sendList(player, world, id);
            case "add" -> {
                if (parts.length < 4) {
                    msg().send(player, "blacklist-usage");
                    return;
                }
                add(player, region, world, parts[3]);
            }
            case "remove" -> {
                if (parts.length < 4) {
                    msg().send(player, "blacklist-usage");
                    return;
                }
                remove(player, world, id, parts[3]);
            }
            default -> msg().send(player, "blacklist-usage");
        }
    }

    private void add(Player player, ProtectedRegion region, String world, String name) {
        String id = region.getId();
        OfflinePlayer target = resolve(name);
        if (target == null) {
            msg().send(player, "player-not-found", Messages.player(name));
            return;
        }
        String targetName = target.getName() == null ? name : target.getName();
        UUID uuid = target.getUniqueId();

        if (uuid.equals(player.getUniqueId())) {
            msg().send(player, "blacklist-self");
            return;
        }
        if (region.getOwners().contains(uuid)) {
            msg().send(player, "blacklist-owner", Messages.player(targetName), Messages.region(id));
            return;
        }

        RegionData data = store().getOrCreate(world, id);
        if (!data.addToBlacklist(uuid, targetName)) {
            msg().send(player, "blacklist-already", Messages.player(targetName), Messages.region(id));
            return;
        }
        store().saveAndCleanup(world, id);
        msg().send(player, "blacklist-added", Messages.player(targetName), Messages.region(id));

        Player online = target.getPlayer();
        if (online != null) {
            msg().send(online, "blacklist-target-notify", Messages.region(id));
        }
    }

    private void remove(Player player, String world, String id, String name) {
        RegionData data = store().get(world, id);
        UUID uuid = data == null ? null : data.findByName(name);
        if (uuid == null && data != null) {
            OfflinePlayer target = resolve(name);
            if (target != null && data.isBlacklisted(target.getUniqueId())) {
                uuid = target.getUniqueId();
            }
        }
        if (uuid == null) {
            msg().send(player, "blacklist-not-listed", Messages.player(name), Messages.region(id));
            return;
        }
        String storedName = data.blacklist().getOrDefault(uuid, name);
        data.removeFromBlacklist(uuid);
        store().saveAndCleanup(world, id);
        msg().send(player, "blacklist-removed", Messages.player(storedName), Messages.region(id));
    }

    private void sendList(Player player, String world, String id) {
        RegionData data = store().get(world, id);
        if (data == null || data.blacklist().isEmpty()) {
            msg().send(player, "blacklist-empty", Messages.region(id));
            return;
        }
        msg().send(player, "blacklist-header", Messages.region(id), Messages.number("count", data.blacklist().size()));
        for (String name : data.blacklist().values()) {
            msg().send(player, "blacklist-entry", Messages.player(name), Messages.region(id));
        }
    }

    private static OfflinePlayer resolve(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }
        return Bukkit.getOfflinePlayerIfCached(name);
    }

    // ------------------------------------------------------------------ автодополнение

    @EventHandler(priority = EventPriority.HIGH)
    public void onTabComplete(TabCompleteEvent event) {
        if (!(event.getSender() instanceof Player player) || !player.hasPermission(Permissions.USE)) {
            return;
        }
        String buffer = event.getBuffer();
        if (!buffer.startsWith("/")) {
            return;
        }
        String[] parts = buffer.substring(1).split(" ", -1);
        if (parts.length < 2 || !plugin.settings().commandAliases().contains(parts[0].toLowerCase(Locale.ROOT))) {
            return;
        }

        String sub = parts[1].toLowerCase(Locale.ROOT);
        List<String> extra = switch (parts.length) {
            case 2 -> filter(SUBCOMMANDS, parts[1]);
            case 3 -> switch (sub) {
                case "whitelist" -> filter(WHITELIST_ARGS, parts[2]);
                case "blacklist" -> filter(BLACKLIST_ARGS, parts[2]);
                default -> List.of();
            };
            case 4 -> {
                if (!sub.equals("blacklist")) {
                    yield List.of();
                }
                String action = parts[2].toLowerCase(Locale.ROOT);
                if (action.equals("add")) {
                    yield filter(Bukkit.getOnlinePlayers().stream()
                            .filter(p -> !p.equals(player))
                            .map(Player::getName).toList(), parts[3]);
                }
                if (action.equals("remove")) {
                    yield filter(blacklistNamesHere(player), parts[3]);
                }
                yield List.of();
            }
            default -> List.of();
        };

        if (parts.length >= 3 && SUBCOMMANDS.contains(sub)) {
            // Подкоманда наша — подсказки WorldGuard здесь не нужны.
            event.setCompletions(new ArrayList<>(extra));
        } else if (!extra.isEmpty()) {
            List<String> merged = new ArrayList<>(event.getCompletions());
            for (String s : extra) {
                if (!merged.contains(s)) {
                    merged.add(s);
                }
            }
            event.setCompletions(merged);
        }
    }

    private Collection<String> blacklistNamesHere(Player player) {
        List<String> names = new ArrayList<>();
        String world = player.getWorld().getName();
        for (ProtectedRegion region : service().regionsSorted(player)) {
            RegionData data = store().get(world, region.getId());
            if (data != null) {
                names.addAll(data.blacklist().values());
            }
        }
        return names;
    }

    private static List<String> filter(Collection<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }
}
