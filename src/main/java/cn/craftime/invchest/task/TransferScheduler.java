package cn.craftime.invchest.task;

import cn.craftime.invchest.InvChest;
import cn.craftime.invchest.storage.BoundContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TransferScheduler {
    private final InvChest plugin;
    private BukkitTask task;

    public TransferScheduler(InvChest plugin) {
        this.plugin = plugin;
    }

    public void start(int intervalSeconds) {
        stop();
        long ticks = intervalSeconds * 20L;
        task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                iterate();
            }
        }, ticks, ticks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void iterate() {
        if (plugin.store().isGlobalPaused()) {
            return; // Global pause
        }
        
        for (Map.Entry<UUID, List<BoundContainer>> e : plugin.store().snapshot().entrySet()) {
            UUID uid = e.getKey();
            Player p = Bukkit.getPlayer(uid);
            if (p == null) continue;
            List<BoundContainer> list = e.getValue();
            for (BoundContainer bc : list) {
                Location loc = bc.toLocation();
                
                // If chunk is not loaded, we cannot safely access block
                if (loc == null || !loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                    // Skip if chunk unloaded
                    continue;
                }
                
                if (!loc.getBlock().getType().name().equals(bc.type)) {
                    plugin.store().unbind(uid, bc);
                    p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "container.missing.auto_unbound", null)));
                    continue;
                }
                Inventory inv = containerInventory(loc);
                if (inv == null) continue;
                int moved = transfer(p, inv, plugin.config().getTransferMode(), plugin.config().getMaxItemsPerTransfer());
                if (moved > 0 && plugin.config().isTransferMessageEnabled()) {
                    String key = plugin.config().getTransferMode().equals("all") ? "transfer.complete.all" : "transfer.complete.try";
                    p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, key, null)));
                }
            }
        }
    }

    private Inventory containerInventory(Location loc) {
        BlockState state = loc.getBlock().getState();
        if (state instanceof InventoryHolder) {
            return ((InventoryHolder) state).getInventory();
        }
        return null;
    }

    private int transfer(Player p, Inventory source, String mode, int maxItems) {
        int moved = 0;
        if (mode.equals("all")) {
            ItemStack[] items = source.getContents();
            source.clear();
            for (ItemStack it : items) {
                if (it == null) continue;
                if (moved >= maxItems) {
                    p.getWorld().dropItemNaturally(p.getLocation(), it);
                    continue;
                }
                Map<Integer, ItemStack> left = p.getInventory().addItem(it);
                if (!left.isEmpty()) {
                    for (ItemStack rem : left.values()) {
                        p.getWorld().dropItemNaturally(p.getLocation(), rem);
                    }
                }
                moved += it.getAmount();
            }
            return moved;
        }
        for (int i = 0; i < source.getSize(); i++) {
            ItemStack it = source.getItem(i);
            if (it == null) continue;
            if (moved + it.getAmount() > maxItems) break;
            Map<Integer, ItemStack> left = p.getInventory().addItem(it.clone());
            if (!left.isEmpty()) {
                break;
            }
            source.setItem(i, null);
            moved += it.getAmount();
        }
        return moved;
    }

    private String color(String s) {
        return s.replace('&', '§');
    }
}
