package cn.craftime.invchest.listener;

import cn.craftime.invchest.InvChest;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ContainerProtectionListener implements Listener {
    private final InvChest plugin;

    public ContainerProtectionListener(InvChest plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        Inventory inv = event.getInventory();
        InventoryHolder holder = inv.getHolder();
        if (holder instanceof DoubleChest) {
            DoubleChest dc = (DoubleChest) holder;
            InventoryHolder l = dc.getLeftSide();
            InventoryHolder r = dc.getRightSide();
            Location ll = holderLocation(l);
            Location rl = holderLocation(r);
            if (isBound(ll) || isBound(rl)) {
                event.setCancelled(true);
                Player p = (Player) event.getPlayer();
                p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "container.protected", null)));
            }
            return;
        }
        Location loc = holderLocation(holder);
        if (isBound(loc)) {
            event.setCancelled(true);
            Player p = (Player) event.getPlayer();
            p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "container.protected", null)));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block b = event.getBlock();
        String name = b.getType().name();
        if (!isSupportedContainer(name)) return;
        UUID owner = plugin.store().ownerOf(b.getLocation());
        Location targetLoc = b.getLocation();
        if (owner == null) {
            Location neighborLoc = neighborBoundLocation(b);
            if (neighborLoc == null) return;
            owner = plugin.store().ownerOf(neighborLoc);
            targetLoc = neighborLoc;
            if (owner == null) return;
        }
        Player p = event.getPlayer();
        if (p.getUniqueId().equals(owner) || p.hasPermission("invchest.admin")) {
            plugin.store().unbindByLocation(targetLoc);
            p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "unbind.success", null)));
            return;
        }
        event.setCancelled(true);
        p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "break.not_allowed", null)));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        Inventory src = event.getSource();
        Inventory dst = event.getDestination();
        InventoryHolder sh = src.getHolder();
        InventoryHolder dh = dst.getHolder();
        Location sl = holderLocation(sh);
        Location dl = holderLocation(dh);
        if (isBound(sl)) {
            event.setCancelled(true);
            return;
        }
        if (isBound(dl)) {
            return;
        }
    }

    private Location holderLocation(InventoryHolder holder) {
        if (holder instanceof BlockState) {
            return ((BlockState) holder).getLocation();
        }
        return null;
    }

    private boolean isBound(Location loc) {
        if (loc == null) return false;
        return plugin.store().ownerOf(loc) != null;
    }

    private boolean isSupportedContainer(String name) {
        return "CHEST".equals(name) || "TRAPPED_CHEST".equals(name) || name.equals("BARREL") || name.contains("BARREL");
    }

    private Location neighborBoundLocation(Block b) {
        String name = b.getType().name();
        if (!"CHEST".equals(name) && !"TRAPPED_CHEST".equals(name)) return null;
        Block n1 = b.getRelative(1, 0, 0);
        Block n2 = b.getRelative(-1, 0, 0);
        Block n3 = b.getRelative(0, 0, 1);
        Block n4 = b.getRelative(0, 0, -1);
        Location l;
        l = n1.getLocation();
        if (plugin.store().ownerOf(l) != null) return l;
        l = n2.getLocation();
        if (plugin.store().ownerOf(l) != null) return l;
        l = n3.getLocation();
        if (plugin.store().ownerOf(l) != null) return l;
        l = n4.getLocation();
        if (plugin.store().ownerOf(l) != null) return l;
        return null;
    }

    private String color(String s) {
        return s.replace('&', '§');
    }
}
