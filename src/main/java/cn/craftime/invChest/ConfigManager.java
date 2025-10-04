package cn.craftime.invChest;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration languageConfig;
    private final Map<String, FileConfiguration> languageCache = new HashMap<>();
    
    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    public void loadConfig() {
        // 保存默认配置文件
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        // 加载语言文件
        loadLanguageFile();
    }
    
    private void loadLanguageFile() {
        String language = config.getString("language", "zh_cn");
        File languageFile = new File(plugin.getDataFolder(), "lang/" + language + ".yml");
        
        // 如果语言文件不存在，从jar中复制默认文件
        if (!languageFile.exists()) {
            plugin.saveResource("lang/" + language + ".yml", false);
        }
        
        // 从缓存中加载或重新加载语言文件
        if (languageCache.containsKey(language)) {
            languageConfig = languageCache.get(language);
        } else {
            try {
                languageConfig = YamlConfiguration.loadConfiguration(languageFile);
                languageCache.put(language, languageConfig);
                
                // 检查是否需要更新默认语言文件
                InputStream defaultStream = plugin.getResource("lang/" + language + ".yml");
                if (defaultStream != null) {
                    YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
                    languageConfig.setDefaults(defaultConfig);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load language file: " + language, e);
                // 回退到默认语言
                loadDefaultLanguage();
            }
        }
    }
    
    private void loadDefaultLanguage() {
        try {
            InputStream defaultStream = plugin.getResource("lang/zh_cn.yml");
            if (defaultStream != null) {
                languageConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load default language file", e);
        }
    }
    
    public String getMessage(String path) {
        if (languageConfig == null) {
            return "Message not found: " + path;
        }
        
        String message = languageConfig.getString(path);
        if (message == null) {
            return "Message not found: " + path;
        }
        
        // 添加前缀并使用Minecraft颜色代码
        String prefix = languageConfig.getString("prefix", "§6[InvChest]§f ");
        String fullMessage = prefix + message;
        
        // 将 & 转换为 §（Minecraft原生颜色代码）
        return ChatColor.translateAlternateColorCodes('&', fullMessage);
    }
    
    public String getMessage(String path, Map<String, String> placeholders) {
        String message = getMessage(path);
        
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        
        return message;
    }
    
    // 配置获取方法
    public boolean isDebugEnabled() {
        return config.getBoolean("debug", false);
    }
    
    public int getTransferInterval() {
        return config.getInt("transfer.interval", 5);
    }
    
    public boolean isGlobalTransferEnabled() {
        return config.getBoolean("transfer.global-transfer", true);
    }
    
    public boolean showTransferMessages() {
        return config.getBoolean("transfer.show-messages", false); // 默认改为false
    }
    
    public int getMaxBindings() {
        return config.getInt("binding.max-bindings", 10);
    }
    
    public boolean allowOthersOpen() {
        return config.getBoolean("binding.allow-others-open", false);
    }
    
    public boolean allowBreak() {
        return config.getBoolean("binding.allow-break", true); // 默认改为true
    }
    
    public void reload() {
        loadConfig();
    }
}