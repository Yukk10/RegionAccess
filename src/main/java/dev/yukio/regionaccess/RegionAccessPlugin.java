package dev.yukio.regionaccess;

import dev.yukio.regionaccess.command.AdminCommand;
import dev.yukio.regionaccess.command.RegionCommandListener;
import dev.yukio.regionaccess.config.Messages;
import dev.yukio.regionaccess.config.PluginSettings;
import dev.yukio.regionaccess.listener.SignAccessListener;
import dev.yukio.regionaccess.region.RegionService;
import dev.yukio.regionaccess.storage.RegionDataStore;
import dev.yukio.regionaccess.task.WarningTask;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class RegionAccessPlugin extends JavaPlugin {

    private PluginSettings settings;
    private Messages messages;
    private RegionDataStore dataStore;
    private RegionService regionService;
    private WarningTask warningTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfiguration();

        dataStore = new RegionDataStore(this);
        dataStore.load();
        regionService = new RegionService(dataStore);

        var pm = getServer().getPluginManager();
        pm.registerEvents(new RegionCommandListener(this), this);
        pm.registerEvents(new SignAccessListener(this), this);

        warningTask = new WarningTask(this);
        pm.registerEvents(warningTask, this);
        warningTask.runTaskTimer(this, 20L, 20L);

        PluginCommand admin = getCommand("rgaccess");
        if (admin != null) {
            AdminCommand executor = new AdminCommand(this);
            admin.setExecutor(executor);
            admin.setTabCompleter(executor);
        }

        getLogger().info("RegionAccess включён. Регионов с настройками: " + dataStore.size());
    }

    @Override
    public void onDisable() {
        if (warningTask != null) {
            warningTask.cancel();
        }
        if (dataStore != null) {
            dataStore.save();
        }
    }

    /** Перечитывает config.yml (настройки и сообщения). */
    public void loadConfiguration() {
        reloadConfig();
        settings = PluginSettings.load(getConfig());
        messages = new Messages(getConfig());
    }

    public PluginSettings settings() {
        return settings;
    }

    public Messages messages() {
        return messages;
    }

    public RegionDataStore dataStore() {
        return dataStore;
    }

    public RegionService regionService() {
        return regionService;
    }
}
