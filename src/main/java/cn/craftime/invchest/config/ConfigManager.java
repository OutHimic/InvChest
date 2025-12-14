package cn.craftime.invchest.config;

import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    private final FileConfiguration config;

    public ConfigManager(FileConfiguration config) {
        this.config = config;
    }

    public int getTransferIntervalSeconds() {
        return Math.max(1, config.getInt("transfer-interval", 30));
    }

    public String getTransferMode() {
        String mode = config.getString("transfer-mode", "all");
        if (mode == null) return "all";
        mode = mode.toLowerCase();
        if (!mode.equals("all") && !mode.equals("try")) return "all";
        return mode;
    }

    public int getMaxItemsPerTransfer() {
        return Math.max(1, config.getInt("max-items-per-transfer", 64));
    }

    public String getLang() {
        return config.getString("lang", "auto_en_us");
    }

    public boolean isDebugMode() {
        return config.getBoolean("debug-mode", false);
    }

    public int getMaxBoundPerPlayer() {
        int v = config.getInt("max-bound-containers-per-player", 0);
        if (v < 0) v = 0;
        return v;
    }

    public boolean isTransferMessageEnabled() {
        return config.getBoolean("transfer-message-enabled", false);
    }
}
