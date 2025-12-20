package cn.craftime.invchest.storage;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;

public class BoundContainer {
    public String world;
    public int x;
    public int y;
    public int z;
    public String type;
    public String name;
    public int chunkRange;

    public BoundContainer() {}

    public BoundContainer(Location loc, String type, String name, int chunkRange) {
        this.world = loc.getWorld().getName();
        this.x = loc.getBlockX();
        this.y = loc.getBlockY();
        this.z = loc.getBlockZ();
        this.type = type;
        this.name = name;
        this.chunkRange = chunkRange;
    }

    public Location toLocation() {
        World w = Bukkit.getWorld(world);
        if (w == null) return null;
        return new Location(w, x, y, z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BoundContainer)) return false;
        BoundContainer that = (BoundContainer) o;
        return x == that.x && y == that.y && z == that.z && Objects.equals(world, that.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, x, y, z);
    }

    public String key() {
        return world + ":" + x + ":" + y + ":" + z;
    }
}
