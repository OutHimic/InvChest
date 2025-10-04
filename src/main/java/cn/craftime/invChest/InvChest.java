package cn.craftime.invChest;

import cn.craftime.invChest.commands.InvChestCommand;
import cn.craftime.invChest.listeners.BarrelBreakListener;
import cn.craftime.invChest.listeners.PlayerInteractionListener;
import cn.craftime.invChest.tasks.ItemTransferTask;
import org.bukkit.plugin.java.JavaPlugin;

public final class InvChest extends JavaPlugin {
    private ConfigManager configManager;
    private BarrelManager barrelManager;
    private ItemTransferTask transferTask;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("InvChest is starting up...");
        
        // 初始化配置管理器
        configManager = new ConfigManager(this);
        
        // 初始化木桶管理器
        barrelManager = new BarrelManager(this, configManager);
        
        // 注册命令
        getCommand("invchest").setExecutor(new InvChestCommand(this, configManager, barrelManager));
        getCommand("invchest").setTabCompleter(new InvChestCommand(this, configManager, barrelManager));
        
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new PlayerInteractionListener(this, configManager, barrelManager), this);
        getServer().getPluginManager().registerEvents(new BarrelBreakListener(this, configManager, barrelManager), this);
        
        // 启动自动转移任务
        transferTask = new ItemTransferTask(this, configManager, barrelManager);
        transferTask.start();
        
        getLogger().info("InvChest has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("InvChest is shutting down...");
        
        // 停止自动转移任务
        if (transferTask != null) {
            transferTask.stop();
        }
        
        // 清理绑定数据
        if (barrelManager != null) {
            barrelManager.clearAllBindings();
        }
        
        getLogger().info("InvChest has been disabled.");
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public BarrelManager getBarrelManager() {
        return barrelManager;
    }
}