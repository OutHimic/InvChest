package cn.craftime.invChest.listeners;

import cn.craftime.invChest.BarrelManager;
import cn.craftime.invChest.ConfigManager;
import cn.craftime.invChest.InvChest;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BarrelBreakListener implements Listener {
    private final InvChest plugin;
    private final ConfigManager configManager;
    private final BarrelManager barrelManager;
    
    public BarrelBreakListener(InvChest plugin, ConfigManager configManager, BarrelManager barrelManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.barrelManager = barrelManager;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBarrelBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        
        // 只处理木桶
        if (block.getType() != Material.BARREL) {
            return;
        }
        
        Location barrelLocation = block.getLocation();
        
        // 检查木桶是否被绑定
        if (!barrelManager.isBarrelBound(barrelLocation)) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // 检查是否允许破坏
        if (!configManager.allowBreak()) {
            // 不允许破坏绑定木桶
            event.setCancelled(true);
            player.sendMessage(configManager.getMessage("commands.not-your-barrel"));
            return;
        }
        
        // 检查玩家权限
        if (!barrelManager.isBarrelOwner(barrelLocation, player) && 
            !player.hasPermission("invchest.bind.others")) {
            event.setCancelled(true);
            player.sendMessage(configManager.getMessage("commands.not-your-barrel"));
            return;
        }
        
        // 允许破坏，但移除绑定关系
        barrelManager.removeBarrelBinding(barrelLocation);
        
        // 记录调试日志
        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("Barrel at " + barrelLocation + " was broken by " + player.getName() + ", binding removed");
        }
    }
}