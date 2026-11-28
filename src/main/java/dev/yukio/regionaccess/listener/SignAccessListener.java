package dev.yukio.regionaccess.listener;

import dev.yukio.regionaccess.Permissions;
import dev.yukio.regionaccess.RegionAccessPlugin;
import io.papermc.paper.event.player.PlayerOpenSignEvent;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Право regionaccess.signs: в мирах из signs.worlds игрок может ставить, ломать
 * и редактировать таблички в чужих регионах.
 * <p>
 * Основную работу делает {@link WorldGuardBypassListener}: он разрешает действие до проверки
 * WorldGuard, поэтому тот ничего не отменяет и не пишет сообщение об отказе. Этот обработчик
 * (приоритет HIGHEST) — запасной: снимает отмену, если событие всё же отменил кто-то другой.
 */
public final class SignAccessListener implements Listener {

    private final RegionAccessPlugin plugin;

    public SignAccessListener(RegionAccessPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean allowed(Player player, Block block) {
        return isSign(block.getType())
                && player.hasPermission(Permissions.SIGNS)
                && plugin.settings().signsAllowedIn(block.getWorld().getName());
    }

    private static boolean isSign(Material type) {
        return Tag.ALL_SIGNS.isTagged(type);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlace(BlockPlaceEvent event) {
        if (event.isCancelled() && allowed(event.getPlayer(), event.getBlockPlaced())) {
            event.setCancelled(false);
            event.setBuild(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBreak(BlockBreakEvent event) {
        if (event.isCancelled() && allowed(event.getPlayer(), event.getBlock())) {
            event.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSignChange(SignChangeEvent event) {
        if (event.isCancelled() && allowed(event.getPlayer(), event.getBlock())) {
            event.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onOpenSign(PlayerOpenSignEvent event) {
        if (event.isCancelled() && allowed(event.getPlayer(), event.getSign().getBlock())) {
            event.setCancelled(false);
        }
    }

    /** Правый клик по табличке (открытие редактора, краситель, светящийся чернильный мешок). */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        if (event.useInteractedBlock() != Event.Result.DENY) {
            return;
        }
        if (allowed(event.getPlayer(), event.getClickedBlock())) {
            event.setUseInteractedBlock(Event.Result.ALLOW);
            if (event.useItemInHand() == Event.Result.DENY) {
                event.setUseItemInHand(Event.Result.DEFAULT);
            }
        }
    }
}
