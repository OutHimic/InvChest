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
        int v = config.getInt("bind.max-per-player", 10);
        if (v < 0) v = 0; // Legacy 0 means unlimited? Config comment says 0 means unlimited for legacy setting, but new setting default is 10.
        // Let's check if legacy key exists first? No, user updated config structure.
        // But if user has old config file, they might use old key.
        // Let's prioritize new key, fallback to old key if present.
        if (config.contains("max-bound-containers-per-player") && !config.contains("bind.max-per-player")) {
            v = config.getInt("max-bound-containers-per-player", 0);
        }
        return v;
    }

    public int getDefaultChunkRange() {
        return config.getInt("bind.default-chunk-range", -1);
    }

    public String getAutoNameFormat() {
        return config.getString("bind.auto-name-format", "绑定{number}");
    }

    public boolean isChunkLoadingEnabled() {
        return config.getBoolean("chunk-loading.enabled", true);
    }

    public int getMaxChunkRange() {
        return config.getInt("chunk-loading.max-range", 16);
    }
    
    public int getListItemsPerPage() {
        return Math.max(1, config.getInt("ui.list-items-per-page", 8));
    }

    public boolean isTransferMessageEnabled() {
        return config.getBoolean("transfer-message-enabled", false);
    }
}
