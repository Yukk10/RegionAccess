package dev.yukio.regionaccess.command;

import dev.yukio.regionaccess.Permissions;
import dev.yukio.regionaccess.RegionAccessPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.List;

/** /rgaccess reload */
public final class AdminCommand implements TabExecutor {

    private final RegionAccessPlugin plugin;

    public AdminCommand(RegionAccessPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(Permissions.RELOAD)) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.loadConfiguration();
            plugin.messages().send(sender, "reloaded");
            return true;
        }
        plugin.messages().send(sender, "admin-usage");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission(Permissions.RELOAD) && "reload".startsWith(args[0].toLowerCase())) {
            return List.of("reload");
        }
        return List.of();
    }
}
