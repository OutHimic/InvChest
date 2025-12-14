package cn.craftime.invchest.command;

import cn.craftime.invchest.InvChest;
import cn.craftime.invchest.storage.BoundContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InvChestCommand implements TabExecutor {
    private final InvChest plugin;

    public InvChestCommand(InvChest plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + "/invchest <bind|unbind> [player]"));
            return true;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("bind")) {
            return handleBind(sender, args);
        } else if (sub.equals("unbind")) {
            return handleUnbind(sender, args);
        } else if (sub.equals("unbind-here")) {
            return handleUnbindHere(sender);
        } else if (sub.equals("unbind-all")) {
            return handleUnbindAll(sender, args);
        } else if (sub.equals("reload")) {
            return handleReload(sender);
        }
        sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + "/invchest <bind|unbind|unbind-here|unbind-all|reload> [player]"));
        return true;
    }

    private boolean handleBind(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            if (!sender.hasPermission("invchest.admin")) {
                sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "no.permission", null)));
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "player.offline", null)));
                return true;
            }
            Block b = getTargetContainer(sender);
            if (b == null) {
                sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "bind.no_target", null)));
                return true;
            }
            BoundContainer bc = new BoundContainer(b.getLocation(), b.getType().name());
            plugin.store().bind(target.getUniqueId(), bc, plugin.config().getMaxBoundPerPlayer());
            Map<String, String> argsMap = new HashMap<>();
            argsMap.put("player", target.getName());
            sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "admin.bind.success", argsMap)));
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + "Console not supported."));
            return true;
        }
        Player p = (Player) sender;
        if (!p.hasPermission("invchest.bind")) {
            p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "no.permission", null)));
            return true;
        }
        Block b = getTargetContainer(sender);
        if (b == null) {
            p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "bind.no_target", null)));
            return true;
        }
        BoundContainer bc = new BoundContainer(b.getLocation(), b.getType().name());
        boolean ok = plugin.store().bind(p.getUniqueId(), bc, plugin.config().getMaxBoundPerPlayer());
        if (!ok) {
            p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "bind.limit_reached", null)));
            return true;
        }
        Map<String, String> argsMap = new HashMap<>();
        Location loc = b.getLocation();
        argsMap.put("world", loc.getWorld().getName());
        argsMap.put("x", String.valueOf(loc.getBlockX()));
        argsMap.put("y", String.valueOf(loc.getBlockY()));
        argsMap.put("z", String.valueOf(loc.getBlockZ()));
        p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "bind.success", argsMap)));
        return true;
    }

    private boolean handleUnbind(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            if (!sender.hasPermission("invchest.admin")) {
                sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "no.permission", null)));
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            UUID uid = target != null ? target.getUniqueId() : null;
            if (uid == null) {
                try {
                    uid = UUID.fromString(args[1]);
                } catch (Exception ignored) {}
            }
            if (uid == null && target == null) {
                sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "player.offline", null)));
                return true;
            }
            UUID tid = uid != null ? uid : (target != null ? target.getUniqueId() : null);
            plugin.store().unbindAll(tid);
            Map<String, String> argsMap = new HashMap<>();
            argsMap.put("player", target != null ? target.getName() : args[1]);
            sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "admin.unbind.success", argsMap)));
            Player tp = Bukkit.getPlayer(tid);
            if (tp != null) {
                tp.sendMessage(color(plugin.lang().get(tp, "prefix", null) + plugin.lang().get(tp, "unbind.success", null)));
            }
            return true;
        }
        sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + "/invchest unbind <player> | use unbind-here or unbind-all"));
        return true;
    }

    private boolean handleUnbindHere(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + "Console not supported."));
            return true;
        }
        Player p = (Player) sender;
        if (!p.hasPermission("invchest.bind")) {
            p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "no.permission", null)));
            return true;
        }
        if (plugin.store().getBounds(p.getUniqueId()).isEmpty()) {
            p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "unbind.not_bound", null)));
            return true;
        }
        Block b = getTargetContainer(sender);
        if (b == null) {
            p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "bind.no_target", null)));
            return true;
        }
        BoundContainer bc = new BoundContainer(b.getLocation(), b.getType().name());
        boolean removed = plugin.store().unbind(p.getUniqueId(), bc);
        if (!removed) {
            p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "unbind.not_bound", null)));
            return true;
        }
        p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "unbind.success", null)));
        return true;
    }

    private boolean handleUnbindAll(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            if (!sender.hasPermission("invchest.admin")) {
                sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "no.permission", null)));
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            UUID uid = target != null ? target.getUniqueId() : null;
            if (uid == null) {
                try {
                    uid = UUID.fromString(args[1]);
                } catch (Exception ignored) {}
            }
            if (uid == null && target == null) {
                sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "player.offline", null)));
                return true;
            }
            UUID tid = uid != null ? uid : (target != null ? target.getUniqueId() : null);
            plugin.store().unbindAll(tid);
            Map<String, String> argsMap = new HashMap<>();
            argsMap.put("player", target != null ? target.getName() : args[1]);
            sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "admin.unbind.success", argsMap)));
            Player tp = Bukkit.getPlayer(tid);
            if (tp != null) {
                tp.sendMessage(color(plugin.lang().get(tp, "prefix", null) + plugin.lang().get(tp, "unbind.success", null)));
            }
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + "Console not supported."));
            return true;
        }
        Player p = (Player) sender;
        if (!p.hasPermission("invchest.bind")) {
            p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "no.permission", null)));
            return true;
        }
        if (plugin.store().getBounds(p.getUniqueId()).isEmpty()) {
            p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "unbind.not_bound", null)));
            return true;
        }
        plugin.store().unbindAll(p.getUniqueId());
        p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "unbind.success", null)));
        return true;
    }
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("invchest.reload")) {
            sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "no.permission", null)));
            return true;
        }
        plugin.reloadConfig();
        plugin.applyReload();
        sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "reload.success", null)));
        return true;
    }
    private Block getTargetContainer(CommandSender sender) {
        if (!(sender instanceof Player)) return null;
        Player p = (Player) sender;
        Block b = null;
        try {
            Method m = Player.class.getMethod("getTargetBlockExact", int.class);
            b = (Block) m.invoke(p, 10);
        } catch (Exception ignored) {
            try {
                b = p.getTargetBlock(new java.util.HashSet<Byte>(), 10);
            } catch (Exception ignored2) {}
        }
        if (b == null) return null;
        Material type = b.getType();
        String name = type.name();
        if (name.equals("CHEST") || name.equals("TRAPPED_CHEST") || name.equals("BARREL") || name.contains("BARREL")) {
            return b;
        }
        return null;
    }

    private String color(String s) {
        return s.replace('&', '§');
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.add("bind");
            list.add("unbind");
            list.add("unbind-here");
            list.add("unbind-all");
            list.add("reload");
        } else if (args.length == 2 && sender.hasPermission("invchest.admin")) {
            if (!"bind".equalsIgnoreCase(args[0]) && !"unbind".equalsIgnoreCase(args[0]) && !"unbind-all".equalsIgnoreCase(args[0])) {
                return list;
            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                list.add(p.getName());
            }
        }
        return list;
    }
}
