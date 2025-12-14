package cn.craftime.invchest.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
        try (FileReader reader = new FileReader(file)) {
            Type typeList = new TypeToken<Map<String, List<BoundContainer>>>(){}.getType();
            Map<String, List<BoundContainer>> rawList = gson.fromJson(reader, typeList);
            byPlayer.clear();
            byLocationKey.clear();
            if (rawList != null) {
                for (Map.Entry<String, List<BoundContainer>> e : rawList.entrySet()) {
                    UUID uid = UUID.fromString(e.getKey());
                    List<BoundContainer> list = e.getValue() == null ? new ArrayList<BoundContainer>() : e.getValue();
                    byPlayer.put(uid, list);
                    for (BoundContainer bc : list) {
                        byLocationKey.put(bc.key(), uid);
                    }
                }
                return;
            }
        } catch (Exception ignored) {}
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
                        list.add(e.getValue());
                        byLocationKey.put(e.getValue().key(), uid);
                    }
                    byPlayer.put(uid, list);
                }
            }
        } catch (Exception ignored) {}
    }

    public void save() {
        try (FileWriter writer = new FileWriter(file)) {
            Map<String, List<BoundContainer>> raw = new HashMap<>();
            for (Map.Entry<UUID, List<BoundContainer>> e : byPlayer.entrySet()) {
                raw.put(e.getKey().toString(), e.getValue());
            }
            gson.toJson(raw, writer);
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
        list.add(container);
        byLocationKey.put(container.key(), player);
        return true;
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

    public UUID ownerOf(Location loc) {
        BoundContainer bc = new BoundContainer(loc, loc.getBlock().getType().name());
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
        String key = new BoundContainer(loc, loc.getBlock().getType().name()).key();
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
}
