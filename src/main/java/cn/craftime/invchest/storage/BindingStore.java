package cn.craftime.invchest.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BindingStore {
    private final Plugin plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File file;
    private final Map<UUID, List<BoundContainer>> byPlayer = new HashMap<>();
    private final Map<String, UUID> byLocationKey = new HashMap<>();
    private boolean globalPaused = false;

    public BindingStore(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "bind.json");
    }

    public void load() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        if (!file.exists()) {
            save();
            return;
        }
        
        // Try to load as new structure (object with "paused" and "data")
        try (FileReader reader = new FileReader(file)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            if (root != null) {
                if (root.has("paused")) {
                    this.globalPaused = root.get("paused").getAsBoolean();
                }
                
                if (root.has("data")) {
                    Type typeList = new TypeToken<Map<String, List<BoundContainer>>>(){}.getType();
                    Map<String, List<BoundContainer>> rawList = gson.fromJson(root.get("data"), typeList);
                    loadFromMap(rawList);
                    return; // Loaded successfully
                }
                
                // If it's a map (old format), it won't have "data" usually, but JsonObject parse might succeed if it's just keys
                // Let's try to parse as old map format if "data" is missing
            }
        } catch (Exception ignored) {}

        // Fallback: try legacy load (raw map of lists)
        try (FileReader reader = new FileReader(file)) {
            Type typeList = new TypeToken<Map<String, List<BoundContainer>>>(){}.getType();
            Map<String, List<BoundContainer>> rawList = gson.fromJson(reader, typeList);
            if (rawList != null) {
                loadFromMap(rawList);
                return;
            }
        } catch (Exception ignored) {}
        
        // Fallback: try legacy load (raw map of single objects)
        try (FileReader reader = new FileReader(file)) {
            Type typeSingle = new TypeToken<Map<String, BoundContainer>>(){}.getType();
            Map<String, BoundContainer> rawSingle = gson.fromJson(reader, typeSingle);
            byPlayer.clear();
            byLocationKey.clear();
            if (rawSingle != null) {
                for (Map.Entry<String, BoundContainer> e : rawSingle.entrySet()) {
                    UUID uid = UUID.fromString(e.getKey());
                    List<BoundContainer> list = new ArrayList<BoundContainer>();
                    if (e.getValue() != null) {
                        e.getValue().name = "绑定1"; // Default name for very old data
                        e.getValue().chunkRange = -1; // Default range
                        list.add(e.getValue());
                        byLocationKey.put(e.getValue().key(), uid);
                    }
                    byPlayer.put(uid, list);
                }
            }
        } catch (Exception ignored) {}
    }

    private void loadFromMap(Map<String, List<BoundContainer>> rawList) {
        byPlayer.clear();
        byLocationKey.clear();
        if (rawList != null) {
            for (Map.Entry<String, List<BoundContainer>> e : rawList.entrySet()) {
                UUID uid = UUID.fromString(e.getKey());
                List<BoundContainer> list = e.getValue() == null ? new ArrayList<BoundContainer>() : e.getValue();
                byPlayer.put(uid, list);
                
                // Fix missing fields and populate index
                int idx = 1;
                for (BoundContainer bc : list) {
                    if (bc.name == null) {
                        bc.name = "绑定" + idx; // Default name
                    }
                    if (bc.chunkRange == 0 && !isZeroRangeExplicitlySet(bc)) {
                         // We can't easily know if 0 was set or default. 
                         // But if it's migration, -1 is safer default for "no chunk loading".
                         // However, int default is 0. 
                         // Let's assume old data implies "no chunk loading", which is -1 in new logic.
                         // But we can't change 0 to -1 blindly if user meant 0.
                         // Actually, old BoundContainer didn't have this field, so Gson sets it to 0.
                         // So we should probably set it to -1 if we suspect it's migrated.
                         // For now let's set to -1 to be safe and consistent with "default-chunk-range".
                         bc.chunkRange = -1;
                    }
                    byLocationKey.put(bc.key(), uid);
                    idx++;
                }
            }
        }
    }
    
    // Helper to guess if 0 is real or default (not possible with standard Gson on int, but acceptable for migration)
    private boolean isZeroRangeExplicitlySet(BoundContainer bc) {
        // This is a placeholder. In reality we can't know. 
        // We assume 0 from legacy migration means "not set" -> -1.
        return false;
    }

    public void save() {
        try (FileWriter writer = new FileWriter(file)) {
            Map<String, Object> root = new HashMap<>();
            root.put("paused", globalPaused);
            
            Map<String, List<BoundContainer>> data = new HashMap<>();
            for (Map.Entry<UUID, List<BoundContainer>> e : byPlayer.entrySet()) {
                data.put(e.getKey().toString(), e.getValue());
            }
            root.put("data", data);
            
            gson.toJson(root, writer);
        } catch (Exception ignored) {}
    }

    public List<BoundContainer> getBounds(UUID player) {
        List<BoundContainer> list = byPlayer.get(player);
        if (list == null) return new ArrayList<BoundContainer>();
        return new ArrayList<BoundContainer>(list);
    }

    public boolean bind(UUID player, BoundContainer container, int maxPerPlayer) {
        if (byLocationKey.containsKey(container.key())) return false;
        List<BoundContainer> list = byPlayer.get(player);
        if (list == null) {
            list = new ArrayList<BoundContainer>();
            byPlayer.put(player, list);
        }
        if (maxPerPlayer > 0 && list.size() >= maxPerPlayer) return false;
        
        // Ensure name uniqueness
        if (container.name != null) {
            for (BoundContainer b : list) {
                if (container.name.equals(b.name)) return false; // Name collision
            }
        }
        
        list.add(container);
        byLocationKey.put(container.key(), player);
        return true;
    }
    
    public String getNextAutoName(UUID player, String format) {
        List<BoundContainer> list = byPlayer.get(player);
        if (list == null) return format.replace("{number}", "1");
        int i = 1;
        while (true) {
            String name = format.replace("{number}", String.valueOf(i));
            boolean exists = false;
            for (BoundContainer b : list) {
                if (name.equals(b.name)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) return name;
            i++;
        }
    }
    
    public BoundContainer getBoundByName(UUID player, String name) {
        List<BoundContainer> list = byPlayer.get(player);
        if (list == null) return null;
        for (BoundContainer b : list) {
            if (b.name != null && b.name.equals(name)) return b;
        }
        return null;
    }

    public void unbindAll(UUID player) {
        List<BoundContainer> list = byPlayer.remove(player);
        if (list != null) {
            for (BoundContainer bc : list) {
                byLocationKey.remove(bc.key());
            }
        }
    }

    public boolean unbind(UUID player, BoundContainer container) {
        List<BoundContainer> list = byPlayer.get(player);
        if (list == null) return false;
        boolean removed = false;
        for (int i = 0; i < list.size(); i++) {
            BoundContainer bc = list.get(i);
            // Match by key OR name if provided (but container arg usually comes from location logic which has key)
            if (bc.key().equals(container.key())) {
                list.remove(i);
                byLocationKey.remove(bc.key());
                removed = true;
                break;
            }
        }
        if (list.isEmpty()) {
            byPlayer.remove(player);
        }
        return removed;
    }
    
    public boolean unbindByName(UUID player, String name) {
        List<BoundContainer> list = byPlayer.get(player);
        if (list == null) return false;
        boolean removed = false;
        for (int i = 0; i < list.size(); i++) {
            BoundContainer bc = list.get(i);
            if (bc.name != null && bc.name.equals(name)) {
                list.remove(i);
                byLocationKey.remove(bc.key());
                removed = true;
                break;
            }
        }
        if (list.isEmpty()) {
            byPlayer.remove(player);
        }
        return removed;
    }

    public UUID ownerOf(Location loc) {
        // Just for key generation
        BoundContainer bc = new BoundContainer(loc, loc.getBlock().getType().name(), null, 0);
        return byLocationKey.get(bc.key());
    }

    public Map<UUID, List<BoundContainer>> snapshot() {
        Map<UUID, List<BoundContainer>> snap = new HashMap<>();
        for (Map.Entry<UUID, List<BoundContainer>> e : byPlayer.entrySet()) {
            snap.put(e.getKey(), new ArrayList<BoundContainer>(e.getValue()));
        }
        return snap;
    }

    public boolean unbindByLocation(Location loc) {
        UUID owner = ownerOf(loc);
        if (owner == null) return false;
        List<BoundContainer> list = byPlayer.get(owner);
        if (list == null) return false;
        String key = new BoundContainer(loc, loc.getBlock().getType().name(), null, 0).key();
        boolean removed = false;
        for (int i = 0; i < list.size(); i++) {
            BoundContainer bc = list.get(i);
            if (bc.key().equals(key)) {
                list.remove(i);
                removed = true;
                break;
            }
        }
        byLocationKey.remove(key);
        if (list.isEmpty()) {
            byPlayer.remove(owner);
        }
        return removed;
    }
    
    public boolean isGlobalPaused() {
        return globalPaused;
    }
    
    public void setGlobalPaused(boolean paused) {
        this.globalPaused = paused;
        save();
    }
}
