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
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.entity.Player;

import java.util.List;
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
            UUID owner = boundOwner(ll);
            if (owner == null) owner = boundOwner(rl);
            if (owner != null) {
                event.setCancelled(true);
                Player p = (Player) event.getPlayer();
                sendProtectedMessage(p, owner);
            }
            return;
        }
        Location loc = holderLocation(holder);
        UUID owner = boundOwner(loc);
        if (owner != null) {
            event.setCancelled(true);
            Player p = (Player) event.getPlayer();
            sendProtectedMessage(p, owner);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block b = event.getBlock();
        String name = b.getType().name();
        if (!isSupportedContainer(name)) return;
        Location targetLoc = boundLocation(b);
        if (targetLoc == null) return;
        UUID owner = plugin.store().ownerOf(targetLoc);
        if (owner == null) return;
        Player p = event.getPlayer();
        if (p.getUniqueId().equals(owner)) {
            releaseChunk(targetLoc, owner);
            plugin.store().unbindByLocation(targetLoc);
            plugin.store().save();
            p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "unbind.success", null)));
            return;
        }
        event.setCancelled(true);
        sendProtectedMessage(p, owner);
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosion(event.blockList());
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
        if ("CHEST".equals(name)) return true;
        if (name.equals("BARREL") || name.contains("BARREL")) return true;
        return isShulkerBox(name);
    }

    private boolean isShulkerBox(String name) {
        if (name == null) return false;
        if (name.equals("SHULKER_BOX")) return true;
        return name.endsWith("_SHULKER_BOX");
    }

    private Location neighborBoundLocation(Block b) {
        String name = b.getType().name();
        if (!"CHEST".equals(name)) return null;
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

    private Location boundLocation(Block b) {
        Location direct = b.getLocation();
        if (plugin.store().ownerOf(direct) != null) return direct;
        return neighborBoundLocation(b);
    }

    private UUID boundOwner(Location loc) {
        if (loc == null) return null;
        return plugin.store().ownerOf(loc);
    }

    private void sendProtectedMessage(Player p, UUID owner) {
        if (p.hasPermission("invchest.inf") && owner != null) {
            String name = ownerName(owner);
            String msg = plugin.lang().get(p, "container.protected.owner", null).replace("{player}", name);
            p.sendMessage(color(plugin.lang().get(p, "prefix", null) + msg));
            return;
        }
        p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "container.protected", null)));
    }

    private String ownerName(UUID owner) {
        try {
            org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(owner);
            if (op != null && op.getName() != null) return op.getName();
        } catch (Exception ignored) {}
        return owner.toString();
    }

    private void handleExplosion(List<Block> blocks) {
        if (blocks == null || blocks.isEmpty()) return;
        for (Block b : new ArrayList<Block>(blocks)) {
            String name = b.getType().name();
            if (!isSupportedContainer(name)) continue;
            Location targetLoc = boundLocation(b);
            if (targetLoc == null) continue;
            UUID owner = plugin.store().ownerOf(targetLoc);
            if (owner == null) continue;
            releaseChunk(targetLoc, owner);
            plugin.store().unbindByLocation(targetLoc);
        }
        plugin.store().save();
    }

    private void releaseChunk(Location loc, UUID owner) {
        if (loc == null) return;
        List<BoundContainer> list = plugin.store().getBounds(owner);
        String key = new BoundContainer(loc, loc.getBlock().getType().name(), null, 0).key();
        for (BoundContainer bc : list) {
            if (bc.key().equals(key)) {
                if (bc.chunkRange >= 0 && plugin.chunkLoader().isSupported()) {
                    plugin.chunkLoader().release(loc, bc.chunkRange);
                }
                return;
            }
        }
    }

    private String color(String s) {
        return s.replace('&', '§');
    }
}
