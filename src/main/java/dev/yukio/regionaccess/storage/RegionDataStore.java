package dev.yukio.regionaccess.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Хранит настройки регионов в plugins/RegionAccess/data.yml.
 * Структура: мир → ID региона → {@link RegionData}.
 */
public final class RegionDataStore {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, Map<String, RegionData>> worlds = new HashMap<>();

    public RegionDataStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    public RegionData get(String world, String regionId) {
        Map<String, RegionData> regions = worlds.get(world);
        return regions == null ? null : regions.get(key(regionId));
    }

    public RegionData getOrCreate(String world, String regionId) {
        return worlds.computeIfAbsent(world, w -> new HashMap<>())
                .computeIfAbsent(key(regionId), id -> new RegionData());
    }

    public boolean hasWorld(String world) {
        Map<String, RegionData> regions = worlds.get(world);
        return regions != null && !regions.isEmpty();
    }

    public int size() {
        return worlds.values().stream().mapToInt(Map::size).sum();
    }

    /** Убирает пустые записи и сохраняет файл. */
    public void saveAndCleanup(String world, String regionId) {
        Map<String, RegionData> regions = worlds.get(world);
        if (regions != null) {
            RegionData data = regions.get(key(regionId));
            if (data != null && data.isEmpty()) {
                regions.remove(key(regionId));
            }
            if (regions.isEmpty()) {
                worlds.remove(world);
            }
        }
        save();
    }

    public void load() {
        worlds.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (Map<?, ?> entry : yaml.getMapList("regions")) {
            Object world = entry.get("world");
            Object id = entry.get("id");
            if (world == null || id == null) {
                continue;
            }
            RegionData data = getOrCreate(world.toString(), id.toString());
            data.setWhitelist(Boolean.TRUE.equals(entry.get("whitelist")));
            if (entry.get("blacklist") instanceof List<?> list) {
                for (Object o : list) {
                    if (o instanceof Map<?, ?> player && player.get("uuid") != null) {
                        try {
                            UUID uuid = UUID.fromString(player.get("uuid").toString());
                            Object name = player.get("name");
                            data.addToBlacklist(uuid, name == null ? uuid.toString() : name.toString());
                        } catch (IllegalArgumentException ignored) {
                            plugin.getLogger().warning("data.yml: неверный UUID " + player.get("uuid"));
                        }
                    }
                }
            }
        }
    }

    public void save() {
        List<Map<String, Object>> list = new ArrayList<>();
        worlds.forEach((world, regions) -> regions.forEach((id, data) -> {
            if (data.isEmpty()) {
                return;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("world", world);
            entry.put("id", id);
            entry.put("whitelist", data.whitelist());
            List<Map<String, Object>> players = new ArrayList<>();
            data.blacklist().forEach((uuid, name) -> {
                Map<String, Object> p = new LinkedHashMap<>();
                p.put("uuid", uuid.toString());
                p.put("name", name);
                players.add(p);
            });
            entry.put("blacklist", players);
            list.add(entry);
        }));

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().setHeader(List.of(
                "Данные RegionAccess. Редактировать вручную не рекомендуется —",
                "используйте /rg whitelist и /rg blacklist."));
        yaml.set("regions", list);
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось сохранить data.yml", e);
        }
    }

    private static String key(String regionId) {
        return regionId.toLowerCase(Locale.ROOT);
    }
}
