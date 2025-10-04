package cn.craftime.invChest.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;

import cn.craftime.invChest.BarrelManager;
import cn.craftime.invChest.ConfigManager;
import cn.craftime.invChest.InvChest;

public class InvChestCommand implements CommandExecutor, TabCompleter {
    private final InvChest plugin;
    private final ConfigManager configManager;
    private final BarrelManager barrelManager;
    
    public InvChestCommand(InvChest plugin, ConfigManager configManager, BarrelManager barrelManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.barrelManager = barrelManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            // 控制台执行
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("invchest.reload") || !sender.isOp()) {
                    configManager.reload();
                    sender.sendMessage("§aInvChest configuration reloaded successfully!");
                    return true;
                } else {
                    sender.sendMessage("§cYou don't have permission to use this command.");
                    return true;
                }
            }
            sender.sendMessage("§cThis command can only be executed by players.");
            return true;
        }
        
        Player player = (Player) sender;
        
        // 检查基础权限
        if (!player.hasPermission("invchest.bind")) {
            player.sendMessage(configManager.getMessage("commands.no-permission"));
            return true;
        }
        
        // 处理 reload 命令
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (player.hasPermission("invchest.reload")) {
                configManager.reload();
                player.sendMessage(configManager.getMessage("commands.reload-success"));
                return true;
            } else {
                player.sendMessage(configManager.getMessage("commands.no-permission"));
                return true;
            }
        }
        
        // 获取玩家目视的方块
        Block targetBlock = getTargetBlock(player, 10);
        if (targetBlock == null || targetBlock.getType() != Material.BARREL) {
            player.sendMessage(configManager.getMessage("commands.no-barrel-in-sight"));
            return true;
        }
        
        Location barrelLocation = targetBlock.getLocation();
        
        // 处理绑定到其他玩家
        if (args.length > 0) {
            if (!player.hasPermission("invchest.bind.others")) {
                player.sendMessage(configManager.getMessage("commands.no-permission"));
                return true;
            }
            
            Player targetPlayer = Bukkit.getPlayer(args[0]);
            if (targetPlayer == null) {
                player.sendMessage(configManager.getMessage("commands.player-not-found"));
                return true;
            }
            
            // 绑定木桶到指定玩家
            if (barrelManager.bindBarrel(player, targetBlock, targetPlayer)) {
                player.sendMessage(configManager.getMessage("commands.barrel-bound-other",
                    Map.of("player", targetPlayer.getName())));
                
                // 通知目标玩家（如果在线且不同玩家）
                if (!targetPlayer.equals(player) && configManager.showTransferMessages()) {
                    targetPlayer.sendMessage(configManager.getMessage("commands.barrel-bound"));
                }
            } else {
                if (barrelManager.isBarrelBound(barrelLocation)) {
                    player.sendMessage(configManager.getMessage("commands.barrel-already-bound"));
                } else {
                    int maxBindings = barrelManager.getPlayerMaxBindings(targetPlayer);
                    player.sendMessage(configManager.getMessage("commands.max-bindings-reached",
                        Map.of("max", String.valueOf(maxBindings))));
                }
            }
            return true;
        }
        
        // 绑定木桶到自己
        if (barrelManager.bindBarrel(player, targetBlock, player)) {
            player.sendMessage(configManager.getMessage("commands.barrel-bound"));
        } else {
            if (barrelManager.isBarrelBound(barrelLocation)) {
                player.sendMessage(configManager.getMessage("commands.barrel-already-bound"));
            } else {
                int maxBindings = barrelManager.getPlayerMaxBindings(player);
                player.sendMessage(configManager.getMessage("commands.max-bindings-reached",
                    Map.of("max", String.valueOf(maxBindings))));
            }
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // 第一个参数：reload 或 玩家名称
            if (sender.hasPermission("invchest.reload")) {
                completions.add("reload");
            }
            
            if (sender.hasPermission("invchest.bind.others")) {
                // 添加在线玩家名称
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                        completions.add(player.getName());
                    }
                }
            }
        }
        
        return completions;
    }
    
    /**
     * 获取玩家目视的方块
     */
    private Block getTargetBlock(Player player, int range) {
        try {
            BlockIterator iterator = new BlockIterator(player, range);
            while (iterator.hasNext()) {
                Block block = iterator.next();
                if (block.getType() != Material.AIR) {
                    return block;
                }
            }
        } catch (IllegalStateException e) {
            // 玩家不在世界中
            return null;
        }
        return null;
    }
}