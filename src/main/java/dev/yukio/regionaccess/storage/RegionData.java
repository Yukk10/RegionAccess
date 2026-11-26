package dev.yukio.regionaccess.storage;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Настройки одного региона: белый список и чёрный список (UUID → последний известный ник). */
public final class RegionData {

    private boolean whitelist;
    private final Map<UUID, String> blacklist = new LinkedHashMap<>();

    public boolean whitelist() {
        return whitelist;
    }

    public void setWhitelist(boolean whitelist) {
        this.whitelist = whitelist;
    }

    public Map<UUID, String> blacklist() {
        return Collections.unmodifiableMap(blacklist);
    }

    public boolean isBlacklisted(UUID uuid) {
        return blacklist.containsKey(uuid);
    }

    public boolean addToBlacklist(UUID uuid, String name) {
        return blacklist.put(uuid, name) == null;
    }

    public boolean removeFromBlacklist(UUID uuid) {
        return blacklist.remove(uuid) != null;
    }

    /** Ищет UUID в чёрном списке по нику без учёта регистра. */
    public UUID findByName(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (var e : blacklist.entrySet()) {
            if (e.getValue().toLowerCase(Locale.ROOT).equals(lower)) {
                return e.getKey();
            }
        }
        return null;
    }

    public boolean isEmpty() {
        return !whitelist && blacklist.isEmpty();
    }
}
