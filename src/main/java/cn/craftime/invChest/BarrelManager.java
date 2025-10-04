package cn.craftime.invChest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class BarrelManager {
    private final InvChest plugin;
    private final ConfigManager configManager;
    
    // 存储木桶绑定关系：位置 -> 玩家UUID
    private final Map<Location, UUID> barrelBindings = new ConcurrentHashMap<>();
    // 存储玩家的绑定数量：玩家UUID -> 绑定数量
    private final Map<UUID, Integer> playerBindingCounts = new ConcurrentHashMap<>();
    
    public BarrelManager(InvChest plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }
    
    /**
     * 绑定木桶到玩家
     */
    public boolean bindBarrel(Player player, Block block, Player targetPlayer) {
        // 验证是否为木桶
        if (block.getType() != Material.BARREL) {
            return false;
        }
        
        Location location = block.getLocation();
        UUID targetUUID = targetPlayer.getUniqueId();
        
        // 检查木桶是否已被绑定
        if (barrelBindings.containsKey(location)) {
            return false;
        }
        
        // 检查玩家是否达到最大绑定数量
        int currentBindings = playerBindingCounts.getOrDefault(targetUUID, 0);
        int maxBindings = configManager.getMaxBindings();
        
        if (currentBindings >= maxBindings) {
            return false;
        }
        
        // 绑定木桶
        barrelBindings.put(location, targetUUID);
        playerBindingCounts.put(targetUUID, currentBindings + 1);
        
        // 记录调试日志
        if (configManager.isDebugEnabled()) {
            String debugMsg = configManager.getMessage("debug.barrel-bound", 
                Map.of("player", targetPlayer.getName(), "location", location.toString()));
            plugin.getLogger().info(debugMsg);
        }
        
        return true;
    }
    
    /**
     * 解除绑定木桶
     */
    public boolean unbindBarrel(Location location, Player player) {
        UUID boundPlayer = barrelBindings.get(location);
        if (boundPlayer == null) {
            return false;
        }
        
        // 检查权限：只有绑定者或拥有权限的玩家可以解除绑定
        if (!boundPlayer.equals(player.getUniqueId()) && 
            !player.hasPermission("invchest.bind.others")) {
            return false;
        }
        
        // 解除绑定
        barrelBindings.remove(location);
        playerBindingCounts.computeIfPresent(boundPlayer, (uuid, count) -> count > 1 ? count - 1 : null);
        
        // 发送解除绑定消息
        if (boundPlayer.equals(player.getUniqueId())) {
            player.sendMessage(configManager.getMessage("commands.barrel-unbound"));
        } else {
            player.sendMessage(configManager.getMessage("commands.barrel-unbound-other",
                Map.of("player", Bukkit.getOfflinePlayer(boundPlayer).getName())));
        }
        
        return true;
    }
    
    /**
     * 获取木桶的绑定玩家
     */
    public UUID getBoundPlayer(Location location) {
        return barrelBindings.get(location);
    }
    
    /**
     * 检查木桶是否被绑定
     */
    public boolean isBarrelBound(Location location) {
        return barrelBindings.containsKey(location);
    }
    
    /**
     * 检查玩家是否拥有该木桶
     */
    public boolean isBarrelOwner(Location location, Player player) {
        UUID boundPlayer = barrelBindings.get(location);
        return boundPlayer != null && boundPlayer.equals(player.getUniqueId());
    }
    
    /**
     * 获取玩家的绑定数量
     */
    public int getPlayerBindingCount(Player player) {
        return playerBindingCounts.getOrDefault(player.getUniqueId(), 0);
    }
    
    /**
     * 获取玩家的最大绑定数量
     */
    public int getPlayerMaxBindings(Player player) {
        return configManager.getMaxBindings();
    }
    
    /**
     * 转移木桶物品到玩家物品栏 - 使用安全的物品转移逻辑
     * 基于Minecraft原生的物品转移机制，避免状态同步问题
     */
    public TransferResult transferItemsToPlayer(Location barrelLocation, Player player) {
        if (!isBarrelBound(barrelLocation) || !isBarrelOwner(barrelLocation, player)) {
            return new TransferResult(false, 0, "not_owner");
        }
        
        Block block = barrelLocation.getBlock();
        if (block.getType() != Material.BARREL) {
            return new TransferResult(false, 0, "not_barrel");
        }
        
        // 重新获取木桶状态以确保最新数据
        Barrel barrel = (Barrel) block.getState();
        Inventory barrelInventory = barrel.getInventory();
        Inventory playerInventory = player.getInventory();
        
        int totalTransferred = 0;
        boolean inventoryFull = false;
        boolean anyItemsTransferred = false;
        
        // 记录调试日志
        if (configManager.isDebugEnabled()) {
            String debugMsg = configManager.getMessage("debug.transfer-started",
                Map.of("player", player.getName(), "location", barrelLocation.toString()));
            plugin.getLogger().info(debugMsg);
        }
        
        // 创建要转移的物品列表（使用克隆避免引用问题）
        List<ItemStack> itemsToTransfer = new ArrayList<>();
        for (int i = 0; i < barrelInventory.getSize(); i++) {
            ItemStack item = barrelInventory.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                itemsToTransfer.add(item.clone());
            }
        }
        
        // 先清空木桶，避免状态同步问题
        barrelInventory.clear();
        
        // 转移物品到玩家
        for (ItemStack item : itemsToTransfer) {
            int originalAmount = item.getAmount();
            
            // 记录调试信息
            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("Attempting to transfer " + originalAmount + " " + 
                    item.getType());
            }
            
            // 使用Bukkit的原生addItem方法
            Map<Integer, ItemStack> leftover = playerInventory.addItem(item);
            
            if (leftover.isEmpty()) {
                // 所有物品都成功转移
                totalTransferred += originalAmount;
                anyItemsTransferred = true;
                
                // 记录详细调试信息
                if (configManager.isDebugEnabled()) {
                    plugin.getLogger().info("Successfully transferred " + originalAmount + " " + 
                        item.getType());
                }
            } else {
                // 部分转移成功，将剩余物品放回木桶
                ItemStack remaining = leftover.values().iterator().next();
                int transferredAmount = originalAmount - remaining.getAmount();
                
                if (transferredAmount > 0) {
                    totalTransferred += transferredAmount;
                    anyItemsTransferred = true;
                    
                    // 记录详细调试信息
                    if (configManager.isDebugEnabled()) {
                        plugin.getLogger().info("Partially transferred " + transferredAmount + " " + 
                            item.getType() + ", " + remaining.getAmount() + " remaining");
                    }
                }
                
                // 将剩余物品放回木桶
                barrelInventory.addItem(remaining);
                inventoryFull = true;
            }
        }
        
        // 强制更新木桶状态
        if (anyItemsTransferred) {
            barrel.update(true);
            // 额外确保块状态更新
            block.getState().update(true, true);
        }
        
        // 记录调试日志
        if (configManager.isDebugEnabled() && totalTransferred > 0) {
            String debugMsg = configManager.getMessage("debug.transfer-completed",
                Map.of("player", player.getName(), "location", barrelLocation.toString(), 
                       "amount", String.valueOf(totalTransferred)));
            plugin.getLogger().info(debugMsg);
        }
        
        return new TransferResult(anyItemsTransferred, totalTransferred, inventoryFull ? "inventory_full" : "success");
    }
    
    /**
     * 获取所有绑定木桶的位置
     */
    public Set<Location> getAllBoundBarrels() {
        return Collections.unmodifiableSet(barrelBindings.keySet());
    }
    
    /**
     * 获取玩家所有绑定木桶的位置
     */
    public List<Location> getPlayerBoundBarrels(Player player) {
        List<Location> playerBarrels = new ArrayList<>();
        UUID playerUUID = player.getUniqueId();
        
        for (Map.Entry<Location, UUID> entry : barrelBindings.entrySet()) {
            if (entry.getValue().equals(playerUUID)) {
                playerBarrels.add(entry.getKey());
            }
        }
        
        return playerBarrels;
    }
    
    /**
     * 移除木桶绑定（当木桶被破坏时调用）
     */
    public void removeBarrelBinding(Location location) {
        UUID boundPlayer = barrelBindings.remove(location);
        if (boundPlayer != null) {
            playerBindingCounts.computeIfPresent(boundPlayer, (uuid, count) -> count > 1 ? count - 1 : null);
            
            // 记录调试日志
            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("Removed barrel binding at " + location + " for player " + boundPlayer);
            }
            
            // 通知玩家（如果在线）
            Player player = Bukkit.getPlayer(boundPlayer);
            if (player != null && player.isOnline()) {
                player.sendMessage(configManager.getMessage("commands.barrel-destroyed"));
            }
        }
    }
    
    /**
     * 清理所有绑定数据（插件卸载时调用）
     */
    public void clearAllBindings() {
        barrelBindings.clear();
        playerBindingCounts.clear();
    }
    
    /**
     * 转移结果类
     */
    public static class TransferResult {
        private final boolean success;
        private final int itemsTransferred;
        private final String status;
        
        public TransferResult(boolean success, int itemsTransferred, String status) {
            this.success = success;
            this.itemsTransferred = itemsTransferred;
            this.status = status;
        }
        
        public boolean isSuccess() { return success; }
        public int getItemsTransferred() { return itemsTransferred; }
        public String getStatus() { return status; }
    }
}