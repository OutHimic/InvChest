package cn.craftime.invChest.listeners;

import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import cn.craftime.invChest.BarrelManager;
import cn.craftime.invChest.ConfigManager;
import cn.craftime.invChest.InvChest;

public class PlayerInteractionListener implements Listener {
    private final InvChest plugin;
    private final ConfigManager configManager;
    private final BarrelManager barrelManager;
    
    public PlayerInteractionListener(InvChest plugin, ConfigManager configManager, BarrelManager barrelManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.barrelManager = barrelManager;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 只处理主手交互
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        
        if (block == null || block.getType() != Material.BARREL) {
            return;
        }
        
        Location barrelLocation = block.getLocation();
        
        // 检查木桶是否被绑定
        if (!barrelManager.isBarrelBound(barrelLocation)) {
            return;
        }
        
        // 检查玩家是否拥有该木桶
        if (!barrelManager.isBarrelOwner(barrelLocation, player)) {
            // 木桶被绑定但不是该玩家的
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                // 阻止打开木桶
                if (!configManager.allowOthersOpen()) {
                    event.setCancelled(true);
                    player.sendMessage(configManager.getMessage("commands.not-your-barrel"));
                }
            }
            return;
        }
        
        // 玩家拥有该木桶
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // 如果玩家蹲下，允许放置物品（如接漏斗）
            if (player.isSneaking()) {
                // 允许放置物品，不取消事件
                return;
            } else {
                // 阻止打开木桶
                event.setCancelled(true);
                
                // 立即尝试转移物品
                BarrelManager.TransferResult result = barrelManager.transferItemsToPlayer(barrelLocation, player);
                
                if (result.isSuccess() && result.getItemsTransferred() > 0) {
                    if (configManager.showTransferMessages()) {
                        // 这里可以显示转移了多少物品的消息
                        // 由于不知道具体物品名称，我们显示通用消息
                        player.sendMessage(configManager.getMessage("transfer.items-received",
                            Map.of("amount", String.valueOf(result.getItemsTransferred()), "item", "items")));
                    }
                } else if (result.getStatus().equals("inventory_full")) {
                    player.sendMessage(configManager.getMessage("transfer.inventory-full"));
                }
            }
        }
    }
}