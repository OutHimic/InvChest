package cn.craftime.invchest.util;

import cn.craftime.invchest.InvChest;
import cn.craftime.invchest.storage.BoundContainer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

/**
 * Manages chunk force-loading logic.
 * Downgrades gracefully on versions that do not support setForceLoaded (pre-1.13.1).
 */
public class ChunkLoader {
    private final InvChest plugin;
    private final boolean supported;
    private Method setForceLoadedMethod;
    private Method isForceLoadedMethod;

    public ChunkLoader(InvChest plugin) {
        this.plugin = plugin;
        this.supported = checkSupport();
    }

    private boolean checkSupport() {
        try {
            setForceLoadedMethod = Chunk.class.getMethod("setForceLoaded", boolean.class);
            isForceLoadedMethod = Chunk.class.getMethod("isForceLoaded");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    public boolean isSupported() {
        return supported;
    }

    private void setForceLoaded(Chunk chunk, boolean force) {
        if (!supported || setForceLoadedMethod == null) return;
        try {
            setForceLoadedMethod.invoke(chunk, force);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isForceLoaded(Chunk chunk) {
        if (!supported || isForceLoadedMethod == null) return false;
        try {
            return (boolean) isForceLoadedMethod.invoke(chunk);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Applies force load to chunks around the location within range.
     * Only works if supported and range >= 0.
     * @param loc Center location
     * @param range Radius (0 = self, >0 = radius)
     * @return true if applied, false if not supported or range < 0
     */
    public boolean keepLoaded(Location loc, int range) {
        if (!supported || range < 0) return false;
        if (!plugin.config().isChunkLoadingEnabled()) return false;
        
        World w = loc.getWorld();
        int cx = loc.getBlockX() >> 4;
        int cz = loc.getBlockZ() >> 4;

        int maxRange = plugin.config().getMaxChunkRange();
        if (range > maxRange) range = maxRange;

        for (int x = cx - range; x <= cx + range; x++) {
            for (int z = cz - range; z <= cz + range; z++) {
                setForceLoaded(w.getChunkAt(x, z), true);
            }
        }
        return true;
    }

    /**
     * Releases force load for chunks around the location within range.
     * @param loc Center location
     * @param range Radius
     */
    public void release(Location loc, int range) {
        if (!supported || range < 0) return;
        
        World w = loc.getWorld();
        int cx = loc.getBlockX() >> 4;
        int cz = loc.getBlockZ() >> 4;

        int maxRange = plugin.config().getMaxChunkRange();
        if (range > maxRange) range = maxRange;

        for (int x = cx - range; x <= cx + range; x++) {
            for (int z = cz - range; z <= cz + range; z++) {
                if (shouldKeepLoaded(w, x, z)) {
                    continue;
                }
                setForceLoaded(w.getChunkAt(x, z), false);
            }
        }
    }
    
    private boolean shouldKeepLoaded(World w, int cx, int cz) {
        // Check all online players' bindings
        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
            List<BoundContainer> bounds = plugin.store().getBounds(p.getUniqueId());
            for (BoundContainer bc : bounds) {
                if (bc.chunkRange < 0) continue;
                Location loc = bc.toLocation();
                if (loc == null || !loc.getWorld().equals(w)) continue;
                
                int bcx = loc.getBlockX() >> 4;
                int bcz = loc.getBlockZ() >> 4;
                int r = Math.min(bc.chunkRange, plugin.config().getMaxChunkRange());
                
                if (cx >= bcx - r && cx <= bcx + r && cz >= bcz - r && cz <= bcz + r) {
                    return true;
                }
            }
        }
        return false;
    }

    public void updatePlayer(UUID uuid, boolean online) {
        if (!supported) return;
        List<BoundContainer> list = plugin.store().getBounds(uuid);
        for (BoundContainer bc : list) {
            if (bc.chunkRange < 0) continue;
            Location loc = bc.toLocation();
            if (loc == null) continue;
            if (online) {
                keepLoaded(loc, bc.chunkRange);
            } else {
                release(loc, bc.chunkRange);
            }
        }
    }
    
    public void unloadAll() {
        if (!supported) return;
        for (World w : Bukkit.getWorlds()) {
            for (Chunk c : w.getLoadedChunks()) {
                if (isForceLoaded(c)) {
                    setForceLoaded(c, false);
                }
            }
        }
    }
    
    public void refreshAll() {
        if (!supported) return;
        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
            updatePlayer(p.getUniqueId(), true);
        }
    }
}
