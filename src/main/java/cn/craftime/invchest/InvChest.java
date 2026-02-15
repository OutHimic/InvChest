package cn.craftime.invchest;

import cn.craftime.invchest.command.InvChestCommand;
import cn.craftime.invchest.config.ConfigManager;
import cn.craftime.invchest.lang.LangManager;
import cn.craftime.invchest.listener.ContainerProtectionListener;
import cn.craftime.invchest.listener.PlayerSessionListener;
import cn.craftime.invchest.storage.BindingStore;
import cn.craftime.invchest.task.TransferScheduler;
import cn.craftime.invchest.util.ChunkLoader;
import org.bukkit.plugin.java.JavaPlugin;

public class InvChest extends JavaPlugin {
    private ConfigManager configManager;
    private LangManager langManager;
    private BindingStore bindingStore;
    private TransferScheduler transferScheduler;
    private ChunkLoader chunkLoader;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.configManager = new ConfigManager(getConfig());
        this.langManager = new LangManager(this);
        this.langManager.init(configManager.getLang());
        this.bindingStore = new BindingStore(this);
        this.bindingStore.load();
        this.chunkLoader = new ChunkLoader(this);
        
        getServer().getPluginManager().registerEvents(new ContainerProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerSessionListener(this), this);
        
        InvChestCommand command = new InvChestCommand(this);
        if (getCommand("invchest") != null) {
            getCommand("invchest").setExecutor(command);
            getCommand("invchest").setTabCompleter(command);
        }
        
        this.transferScheduler = new TransferScheduler(this);
        this.transferScheduler.start(configManager.getTransferIntervalSeconds());
        
        // Restore chunk loading for online players
        this.chunkLoader.refreshAll();
        
        getLogger().info("InvChest enabled");
    }

    @Override
    public void onDisable() {
        if (transferScheduler != null) {
            transferScheduler.stop();
        }
        if (chunkLoader != null) {
            chunkLoader.unloadAll();
        }
        if (bindingStore != null) {
            bindingStore.save();
        }
        getLogger().info("InvChest disabled");
    }

    public ConfigManager config() {
        return configManager;
    }

    public LangManager lang() {
        return langManager;
    }

    public BindingStore store() {
        return bindingStore;
    }
    
    public ChunkLoader chunkLoader() {
        return chunkLoader;
    }

    public void applyReload() {
        this.configManager = new ConfigManager(getConfig());
        this.langManager.init(configManager.getLang());
        if (this.transferScheduler != null) {
            this.transferScheduler.start(configManager.getTransferIntervalSeconds());
        }
        if (this.chunkLoader != null) {
            this.chunkLoader.refreshAll();
        }
    }
}
