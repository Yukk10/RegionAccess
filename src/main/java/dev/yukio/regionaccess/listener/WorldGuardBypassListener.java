package dev.yukio.regionaccess.listener;

import com.sk89q.worldguard.bukkit.event.DelegateEvent;
import com.sk89q.worldguard.bukkit.event.block.BreakBlockEvent;
import com.sk89q.worldguard.bukkit.event.block.PlaceBlockEvent;
import com.sk89q.worldguard.bukkit.event.block.UseBlockEvent;
import com.sk89q.worldguard.bukkit.event.entity.DamageEntityEvent;
import com.sk89q.worldguard.bukkit.event.entity.DestroyEntityEvent;
import dev.yukio.regionaccess.Permissions;
import dev.yukio.regionaccess.RegionAccessPlugin;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;

/**
 * Разрешает действия ещё ДО проверки WorldGuard.
 * <p>
 * WorldGuard переводит действия игроков в свои события (PlaceBlockEvent, DamageEntityEvent и т. д.)
 * и проверяет их в RegionProtectionListener. Событие с результатом ALLOW он пропускает целиком:
 * не отменяет и не пишет «Hey! Sorry, but you can't…». Этот обработчик (приоритет LOWEST)
 * выставляет ALLOW раньше WorldGuard, поэтому ложного сообщения больше нет.
 * <ul>
 *   <li>regionaccess.signs  — таблички (ставить, ломать, редактировать) в мирах signs.worlds;</li>
 *   <li>regionaccess.entity — бить и ломать сущности (кроме игроков) в мирах entities.worlds.</li>
 * </ul>
 */
public final class WorldGuardBypassListener implements Listener {

    private final RegionAccessPlugin plugin;

    public WorldGuardBypassListener(RegionAccessPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ таблички

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlace(PlaceBlockEvent event) {
        allowSigns(event, event.getWorld(), event.getBlocks());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreak(BreakBlockEvent event) {
        allowSigns(event, event.getWorld(), event.getBlocks());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onUse(UseBlockEvent event) {
        allowSigns(event, event.getWorld(), event.getBlocks());
    }

    // Общий предок WorldGuard (AbstractBlockEvent) не публичный, поэтому мир и блоки передаются отдельно.
    private void allowSigns(DelegateEvent event, World world, List<Block> blocks) {
        Player player = event.getCause().getFirstPlayer();
        if (player == null || !onlySigns(blocks)) {
            return;
        }
        if (player.hasPermission(Permissions.SIGNS)
                && plugin.settings().signsAllowedIn(world.getName())) {
            allow(event);
        }
    }

    private static boolean onlySigns(List<Block> blocks) {
        if (blocks.isEmpty()) {
            return false;
        }
        for (Block block : blocks) {
            if (!Tag.ALL_SIGNS.isTagged(block.getType())) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ сущности

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(DamageEntityEvent event) {
        allowEntity(event, event.getWorld(), event.getEntity());
    }

    /** Предметы в рамках, картины, вагонетки, лодки, стойки для брони. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDestroy(DestroyEntityEvent event) {
        allowEntity(event, event.getWorld(), event.getEntity());
    }

    private void allowEntity(DelegateEvent event, World world, Entity target) {
        // Игроков не трогаем — PvP по-прежнему решает флаг pvp WorldGuard.
        if (target == null || target instanceof Player) {
            return;
        }
        Player player = event.getCause().getFirstPlayer();
        if (player == null) {
            return;
        }
        if (player.hasPermission(Permissions.ENTITY)
                && plugin.settings().entitiesAllowedIn(world.getName())) {
            allow(event);
        }
    }

    // ------------------------------------------------------------------

    private static void allow(DelegateEvent event) {
        event.setAllowed(true);
    }
}
