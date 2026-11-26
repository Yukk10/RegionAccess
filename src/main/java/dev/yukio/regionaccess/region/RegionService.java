package dev.yukio.regionaccess.region;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import dev.yukio.regionaccess.storage.RegionData;
import dev.yukio.regionaccess.storage.RegionDataStore;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

/** Обёртка над WorldGuard API. */
public final class RegionService {

    private final RegionDataStore store;

    public RegionService(RegionDataStore store) {
        this.store = store;
    }

    public ApplicableRegionSet regionsAt(Location location) {
        return WorldGuard.getInstance().getPlatform().getRegionContainer()
                .createQuery().getApplicableRegions(BukkitAdapter.adapt(location));
    }

    public LocalPlayer wrap(Player player) {
        return WorldGuardPlugin.inst().wrapPlayer(player);
    }

    /** Регионы в точке игрока, отсортированные по приоритету (сначала самый высокий). */
    public List<ProtectedRegion> regionsSorted(Player player) {
        return StreamSupport.stream(regionsAt(player.getLocation()).spliterator(), false)
                .sorted(Comparator.comparingInt(ProtectedRegion::getPriority).reversed())
                .toList();
    }

    /**
     * Нарушение для игрока в текущей точке или null.
     * Чёрный список важнее белого. Владельцы региона под ЧС не попадают,
     * участники (и владельцы) региона не попадают под белый список.
     */
    public Violation findViolation(Player player) {
        String world = player.getWorld().getName();
        if (!store.hasWorld(world)) {
            return null;
        }
        UUID uuid = player.getUniqueId();
        LocalPlayer local = null;
        Violation whitelistHit = null;

        for (ProtectedRegion region : regionsSorted(player)) {
            RegionData data = store.get(world, region.getId());
            if (data == null) {
                continue;
            }
            if (local == null) {
                local = wrap(player);
            }
            if (data.isBlacklisted(uuid) && !region.isOwner(local)) {
                return new Violation(ViolationType.BLACKLIST, world, region.getId());
            }
            if (whitelistHit == null && data.whitelist() && !region.isMember(local)) {
                whitelistHit = new Violation(ViolationType.WHITELIST, world, region.getId());
            }
        }
        return whitelistHit;
    }
}
