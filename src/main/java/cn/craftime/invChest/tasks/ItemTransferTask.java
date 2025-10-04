package cn.craftime.invChest.tasks;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import cn.craftime.invChest.BarrelManager;
import cn.craftime.invChest.ConfigManager;
import cn.craftime.invChest.InvChest;

public class ItemTransferTask extends BukkitRunnable {
    private final InvChest plugin;
    private final ConfigManager configManager;
    private final BarrelManager barrelManager;
    
    public ItemTransferTask(InvChest plugin, ConfigManager configManager, BarrelManager barrelManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.barrelManager = barrelManager;
    }
    
    @Override
    public void run() {
        if (!configManager.isGlobalTransferEnabled()) {
            return;
        }
        
        Set<Location> boundBarrels = barrelManager.getAllBoundBarrels();
        if (boundBarrels.isEmpty()) {
            return;
        }
        
        // 记录调试日志
        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("Starting global item transfer for " + boundBarrels.size() + " bound barrels");
        }
        
        // 遍历所有绑定木桶
        for (Location barrelLocation : boundBarrels) {
            // 检查木桶是否仍然存在且是木桶
            Block block = barrelLocation.getBlock();
            if (block.getType() != Material.BARREL) {
                // 木桶被破坏或改变了类型，移除绑定
                barrelManager.unbindBarrel(barrelLocation, null);
                continue;
            }
            
            // 获取绑定玩家
            UUID playerUUID = barrelManager.getBoundPlayer(barrelLocation);
            if (playerUUID == null) {
                continue;
            }
            
            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null || !player.isOnline()) {
                // 玩家不在线，跳过
                continue;
            }
            
            // 执行物品转移
            transferItemsFromBarrel(barrelLocation, player);
        }
    }
    
    /**
     * 从木桶转移物品到玩家
     */
    private void transferItemsFromBarrel(Location barrelLocation, Player player) {
        BarrelManager.TransferResult result = barrelManager.transferItemsToPlayer(barrelLocation, player);
        
        if (result.isSuccess() && result.getItemsTransferred() > 0) {
            // 记录调试日志
            if (configManager.isDebugEnabled()) {
                Map<String, String> debugPlaceholders = new HashMap<>();
                debugPlaceholders.put("player", player.getName());
                debugPlaceholders.put("location", barrelLocation.toString());
                debugPlaceholders.put("amount", String.valueOf(result.getItemsTransferred()));
                
                String debugMsg = configManager.getMessage("debug.transfer-completed", debugPlaceholders);
                plugin.getLogger().info(debugMsg);
            }
            
            // 显示转移消息给玩家
            if (configManager.showTransferMessages()) {
                Map<String, String> messagePlaceholders = new HashMap<>();
                messagePlaceholders.put("amount", String.valueOf(result.getItemsTransferred()));
                messagePlaceholders.put("item", "items");
                
                String message = configManager.getMessage("transfer.items-received", messagePlaceholders);
                player.sendMessage(message);
            }
        }
    }
    
    /**
     * 创建占位符映射
     */
    private Map<String, String> createPlaceholders(String... keyValuePairs) {
        Map<String, String> placeholders = new HashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            if (i + 1 < keyValuePairs.length) {
                placeholders.put(keyValuePairs[i], keyValuePairs[i + 1]);
            }
        }
        return placeholders;
    }
    
    /**
     * 启动任务
     */
    public void start() {
        int interval = configManager.getTransferInterval();
        this.runTaskTimer(plugin, 20L, interval * 20L); // 延迟1秒后开始，每interval秒执行一次
    }
    
    /**
     * 停止任务
     */
    public void stop() {
        this.cancel();
    }
}