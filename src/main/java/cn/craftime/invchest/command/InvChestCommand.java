package cn.craftime.invchest.command;

import cn.craftime.invchest.InvChest;
import cn.craftime.invchest.storage.BoundContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
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
import java.util.regex.Pattern;

public class InvChestCommand implements TabExecutor {
    private final InvChest plugin;
    private static final Pattern NAME_PATTERN = Pattern.compile("^\\S{1,16}$");

    public InvChestCommand(InvChest plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + "/invchest <bind|unbind|list|start|stop|abind|aunbind|alist|astart|astop|reload>"));
            return true;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("bind")) {
            return handleBind(sender, args);
        } else if (sub.equals("unbind")) {
            return handleUnbind(sender, args);
        } else if (sub.equals("list")) {
            return handleList(sender, args);
        } else if (sub.equals("start")) {
            return handleStartStop(sender, args, false);
        } else if (sub.equals("stop")) {
            return handleStartStop(sender, args, true);
        } else if (sub.equals("abind")) {
            return handleAdminBind(sender, args);
        } else if (sub.equals("aunbind")) {
            return handleAdminUnbind(sender, args);
        } else if (sub.equals("alist")) {
            return handleAdminList(sender, args);
        } else if (sub.equals("astart")) {
            return handleAdminStartStop(sender, args, false);
        } else if (sub.equals("astop")) {
            return handleAdminStartStop(sender, args, true);
        } else if (sub.equals("reload")) {
            return handleReload(sender);
        }
        sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + "/invchest help"));
        return true;
    }

    // /ic bind [name] [range]
    private boolean handleBind(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + "Console not supported."));
            return true;
        }
        Player p = (Player) sender;
        if (!p.hasPermission("invchest.bind")) {
            p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "no.permission", null)));
            return true;
        }

        // Get container
        Block b = getTargetContainer(sender);
        if (b == null) {
            p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "bind.no_target", null)));
            return true;
        }
        if (isProtectedByExternal(p, b.getLocation())) {
            p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "bind.protected", null)));
            return true;
        }

        String name = null;
        int range = plugin.config().getDefaultChunkRange();
        
        // Parse args: bind [name] [range]
        if (args.length > 1) {
            name = args[1];
            if (!isValidName(name)) {
                p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "bind.name_invalid", null)));
                return true;
            }
        }
        if (args.length > 2) {
            try {
                range = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "bind.range_invalid", null)));
                return true;
            }
        }
        
        // Limit range
        int maxRange = plugin.config().getMaxChunkRange();
        if (range > maxRange) {
            range = maxRange;
            p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "bind.range_capped", null).replace("{max}", String.valueOf(maxRange))));
        }

        // Check auto name
        if (name == null) {
            name = plugin.store().getNextAutoName(p.getUniqueId(), plugin.config().getAutoNameFormat());
        } else {
            // Check uniqueness
            if (plugin.store().getBoundByName(p.getUniqueId(), name) != null) {
                p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "bind.name_exists", null)));
                return true;
            }
        }

        BoundContainer bc = new BoundContainer(b.getLocation(), b.getType().name(), name, range);
        boolean ok = plugin.store().bind(p.getUniqueId(), bc, plugin.config().getMaxBoundPerPlayer());
        
        if (!ok) {
            // Could be limit reached OR name exists (handled above) OR location exists
            if (plugin.store().getBoundByName(p.getUniqueId(), name) != null) {
                p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "bind.name_exists", null)));
            } else {
                p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "bind.limit_reached", null)));
            }
            return true;
        }

        // Apply chunk loading if needed
        if (range >= 0 && plugin.chunkLoader().isSupported()) {
            plugin.chunkLoader().keepLoaded(b.getLocation(), range);
        }
        plugin.store().save();

        Map<String, String> argsMap = new HashMap<>();
        Location loc = b.getLocation();
        argsMap.put("name", name);
        argsMap.put("world", loc.getWorld().getName());
        argsMap.put("x", String.valueOf(loc.getBlockX()));
        argsMap.put("y", String.valueOf(loc.getBlockY()));
        argsMap.put("z", String.valueOf(loc.getBlockZ()));
        argsMap.put("range", range < 0 ? "Off" : String.valueOf(range));
        
        p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "bind.success_detail", argsMap)));
        return true;
    }

    // /ic unbind [name]
    private boolean handleUnbind(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + "Console use /ic aunbind"));
            return true;
        }
        Player p = (Player) sender;
        if (!p.hasPermission("invchest.bind")) {
             p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "no.permission", null)));
             return true;
        }
        if (args.length < 2) {
            Block b = getTargetContainer(sender);
            if (b == null) {
                p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "bind.no_target", null)));
                return true;
            }
            UUID owner = plugin.store().ownerOf(b.getLocation());
            if (owner == null || !owner.equals(p.getUniqueId())) {
                p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "unbind.not_bound", null)));
                return true;
            }
            List<BoundContainer> list = plugin.store().getBounds(p.getUniqueId());
            String key = new BoundContainer(b.getLocation(), b.getType().name(), null, 0).key();
            BoundContainer target = null;
            for (BoundContainer bc : list) {
                if (bc.key().equals(key)) {
                    target = bc;
                    break;
                }
            }
            if (target != null) {
                if (target.chunkRange >= 0 && plugin.chunkLoader().isSupported()) {
                    plugin.chunkLoader().release(target.toLocation(), target.chunkRange);
                }
                plugin.store().unbind(p.getUniqueId(), target);
                plugin.store().save();
                p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "unbind.success", null)));
                return true;
            }
            p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "unbind.not_bound", null)));
            return true;
        }

        String name = args[1];
        BoundContainer bc = plugin.store().getBoundByName(p.getUniqueId(), name);
        if (bc == null) {
            p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "unbind.not_found", null)));
            return true;
        }

        if (bc.chunkRange >= 0 && plugin.chunkLoader().isSupported()) {
            plugin.chunkLoader().release(bc.toLocation(), bc.chunkRange);
        }

        plugin.store().unbindByName(p.getUniqueId(), name);
        plugin.store().save();
        p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "unbind.success", null)));
        return true;
    }

    // /ic list [page] OR /ic list [player] [page] OR /ic list @all [page]
    private boolean handleList(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + "/ic alist [ID] [page]"));
            return true;
        }
        Player p = (Player) sender;
        if (!p.hasPermission("invchest.bind")) {
            p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "no.permission", null)));
            return true;
        }
        int page = 1;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                p.sendMessage(color(plugin.lang().get(p, "prefix", null) + "/ic list [page]"));
                return true;
            }
        }
        List<BoundContainer> list = plugin.store().getBounds(p.getUniqueId());
        List<BoundEntry> entries = new ArrayList<>();
        for (BoundContainer bc : list) {
            entries.add(new BoundEntry(bc, p.getUniqueId()));
        }
        sendList(sender, entries, page, false);
        return true;
    }

    private boolean handleAdminList(CommandSender sender, String[] args) {
        if (!sender.hasPermission("invchest.abind")) {
            sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "no.permission", null)));
            return true;
        }
        UUID targetUUID = null;
        int page = 1;
        if (args.length > 1) {
            String id = args[1];
            if (id.equalsIgnoreCase("@all")) {
                targetUUID = null;
            } else {
                ResolvedTarget target = resolveTarget(id);
                if (target == null) {
                    sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "player.offline", null)));
                    return true;
                }
                targetUUID = target.uuid;
            }
            if (args.length > 2) {
                try { page = Integer.parseInt(args[2]); } catch (Exception ignored) {}
            }
        }
        List<BoundEntry> entries = new ArrayList<>();
        if (targetUUID != null) {
            List<BoundContainer> list = plugin.store().getBounds(targetUUID);
            for (BoundContainer bc : list) {
                entries.add(new BoundEntry(bc, targetUUID));
            }
        } else {
            Map<UUID, List<BoundContainer>> snap = plugin.store().snapshot();
            for (Map.Entry<UUID, List<BoundContainer>> e : snap.entrySet()) {
                for (BoundContainer bc : e.getValue()) {
                    entries.add(new BoundEntry(bc, e.getKey()));
                }
            }
        }
        boolean showOwner = targetUUID == null;
        sendList(sender, entries, page, showOwner);
        return true;
    }
    
    // /ic abind <player> [name] [range]
    private boolean handleAdminBind(CommandSender sender, String[] args) {
        if (!sender.hasPermission("invchest.abind")) {
            sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "no.permission", null)));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(color("/ic abind <player> [name] [range] - Look at container"));
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + "Console not supported."));
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
        Player p = (Player) sender;
        if (isProtectedByExternal(p, b.getLocation())) {
            sender.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "bind.protected", null)));
            return true;
        }
        
        String name = null;
        int range = plugin.config().getDefaultChunkRange();
        if (args.length > 2) name = args[2];
        if (args.length > 3) {
            try {
                range = Integer.parseInt(args[3]);
            } catch(Exception e) {
                sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "bind.range_invalid", null)));
                return true;
            }
        }
        
        if (name == null) name = plugin.store().getNextAutoName(target.getUniqueId(), plugin.config().getAutoNameFormat());
        if (!isValidName(name)) {
            sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "bind.name_invalid", null)));
            return true;
        }
        if (plugin.store().getBoundByName(target.getUniqueId(), name) != null) {
            sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "bind.name_exists", null)));
            return true;
        }
        
        BoundContainer bc = new BoundContainer(b.getLocation(), b.getType().name(), name, range);
        boolean ok = plugin.store().bind(target.getUniqueId(), bc, 0);
        
        if (!ok) {
            sender.sendMessage(color("&cFailed to bind (Name collision or location bound)"));
            return true;
        }
        
        if (range >= 0 && plugin.chunkLoader().isSupported()) {
            plugin.chunkLoader().keepLoaded(b.getLocation(), range);
        }
        plugin.store().save();

        sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "admin.bind.success", null).replace("{player}", target.getName())));
        return true;
    }
    
    // /ic aunbind <player> [name]
    private boolean handleAdminUnbind(CommandSender sender, String[] args) {
        if (!sender.hasPermission("invchest.abind")) {
            sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "no.permission", null)));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(color("/ic aunbind <ID> [name]"));
            return true;
        }
        ResolvedTarget target = resolveTarget(args[1]);
        if (target == null) {
            sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "player.offline", null)));
            return true;
        }
        UUID uid = target.uuid;
        
        if (args.length < 3) {
            List<BoundContainer> list = plugin.store().getBounds(uid);
            for (BoundContainer bc : list) {
                if (bc.chunkRange >= 0 && plugin.chunkLoader().isSupported()) {
                    plugin.chunkLoader().release(bc.toLocation(), bc.chunkRange);
                }
            }
            plugin.store().unbindAll(uid);
            plugin.store().save();
            sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "admin.unbind.success", null).replace("{player}", args[1])));
            return true;
        }
        
        String name = args[2];
        BoundContainer bc = plugin.store().getBoundByName(uid, name);
        if (bc == null) {
            sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "unbind.not_found", null)));
            return true;
        }
        
        if (bc.chunkRange >= 0 && plugin.chunkLoader().isSupported()) {
            plugin.chunkLoader().release(bc.toLocation(), bc.chunkRange);
        }
        plugin.store().unbindByName(uid, name);
        plugin.store().save();
        sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "admin.unbind.success", null).replace("{player}", args[1])));
        return true;
    }

    private boolean handleStartStop(CommandSender sender, String[] args, boolean paused) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + "Console not supported."));
            return true;
        }
        Player p = (Player) sender;
        if (!p.hasPermission("invchest.statue")) {
            p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "no.permission", null)));
            return true;
        }
        if (args.length > 1) {
            String name = args[1];
            boolean ok = plugin.store().setPaused(p.getUniqueId(), name, paused);
            if (!ok) {
                p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "unbind.not_found", null)));
                return true;
            }
            plugin.store().save();
            Map<String, String> map = new HashMap<>();
            map.put("name", name);
            String key = paused ? "statue.stop.success" : "statue.start.success";
            p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, key, map)));
            return true;
        }
        List<BoundContainer> list = plugin.store().getBounds(p.getUniqueId());
        if (list.isEmpty()) {
            p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "unbind.not_bound", null)));
            return true;
        }
        plugin.store().setAllPaused(p.getUniqueId(), paused);
        plugin.store().save();
        Map<String, String> map = new HashMap<>();
        map.put("count", String.valueOf(list.size()));
        String key = paused ? "statue.all.stop.success" : "statue.all.start.success";
        p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, key, map)));
        return true;
    }

    private boolean handleAdminStartStop(CommandSender sender, String[] args, boolean paused) {
        if (!sender.hasPermission("invchest.astatue")) {
            sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "no.permission", null)));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + "/ic astart <ID|@all> [name]"));
            return true;
        }
        String id = args[1];
        if (id.equalsIgnoreCase("@all")) {
            plugin.store().setGlobalPaused(paused);
            String key = paused ? "statue.global.stop.success" : "statue.global.start.success";
            sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, key, null)));
            return true;
        }
        ResolvedTarget target = resolveTarget(id);
        if (target == null) {
            sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "player.offline", null)));
            return true;
        }
        UUID uid = target.uuid;
        String playerName = target.name;
        if (args.length > 2) {
            String name = args[2];
            boolean ok = plugin.store().setPaused(uid, name, paused);
            if (!ok) {
                sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "unbind.not_found", null)));
                return true;
            }
            plugin.store().save();
            Map<String, String> map = new HashMap<>();
            map.put("player", playerName);
            map.put("name", name);
            String key = paused ? "statue.admin.stop.success" : "statue.admin.start.success";
            sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, key, map)));
            return true;
        }
        List<BoundContainer> list = plugin.store().getBounds(uid);
        if (list.isEmpty()) {
            sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "unbind.not_bound", null)));
            return true;
        }
        plugin.store().setAllPaused(uid, paused);
        plugin.store().save();
        Map<String, String> map = new HashMap<>();
        map.put("player", playerName);
        map.put("count", String.valueOf(list.size()));
        String key = paused ? "statue.admin.all.stop.success" : "statue.admin.all.start.success";
        sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, key, map)));
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
        if (name.equals("CHEST") || name.equals("BARREL") || name.contains("BARREL") || isShulkerBox(name)) {
            return b;
        }
        return null;
    }

    private boolean isShulkerBox(String name) {
        if (name == null) return false;
        if (name.equals("SHULKER_BOX")) return true;
        return name.endsWith("_SHULKER_BOX");
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
            list.add("list");
            list.add("start");
            list.add("stop");
            list.add("abind");
            list.add("aunbind");
            list.add("alist");
            list.add("astart");
            list.add("astop");
            list.add("reload");
        } else if (args.length == 2) {
             if (args[0].equalsIgnoreCase("unbind") || args[0].equalsIgnoreCase("start") || args[0].equalsIgnoreCase("stop")) {
                 if (sender instanceof Player) {
                     List<BoundContainer> bounds = plugin.store().getBounds(((Player)sender).getUniqueId());
                     for (BoundContainer bc : bounds) list.add(bc.name);
                 }
             } else if (sender.hasPermission("invchest.abind") && (args[0].equalsIgnoreCase("abind") || args[0].equalsIgnoreCase("aunbind") || args[0].equalsIgnoreCase("alist") || args[0].equalsIgnoreCase("astart") || args[0].equalsIgnoreCase("astop"))) {
                 list.add("@all");
                 for (Player p : Bukkit.getOnlinePlayers()) list.add(p.getName());
             }
        }
        return list;
    }

    private boolean isValidName(String name) {
        if (name == null) return false;
        if (!NAME_PATTERN.matcher(name).matches()) return false;
        return name.codePointCount(0, name.length()) <= 16;
    }

    private ResolvedTarget resolveTarget(String id) {
        Player online = Bukkit.getPlayerExact(id);
        if (online != null) {
            return new ResolvedTarget(online.getUniqueId(), online.getName());
        }
        try {
            UUID uid = UUID.fromString(id);
            return new ResolvedTarget(uid, id);
        } catch (Exception ignored) {}
        try {
            OfflinePlayer op = Bukkit.getOfflinePlayer(id);
            if (op != null && (op.isOnline() || op.hasPlayedBefore())) {
                String name = op.getName() != null ? op.getName() : id;
                return new ResolvedTarget(op.getUniqueId(), name);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void sendList(CommandSender sender, List<BoundEntry> entries, int page, boolean showOwner) {
        int perPage = plugin.config().getListItemsPerPage();
        int totalPages = (int) Math.ceil((double)entries.size() / perPage);
        if (page < 1) page = 1;
        if (page > totalPages && totalPages > 0) page = totalPages;
        sender.sendMessage(color("&8&m------------------------------"));
        sender.sendMessage(color("&6InvChest &7(Page " + page + "/" + (totalPages == 0 ? 1 : totalPages) + ")"));
        if (entries.isEmpty()) {
            sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "list.empty", null)));
        } else {
            int start = (page - 1) * perPage;
            int end = Math.min(start + perPage, entries.size());
            for (int i = start; i < end; i++) {
                BoundEntry e = entries.get(i);
                BoundContainer bc = e.container;
                String locStr = bc.world + " " + bc.x + "," + bc.y + "," + bc.z;
                String statusKey = bc.paused ? "list.status.paused" : "list.status.running";
                String status = plugin.lang().get(null, statusKey, null);
                String ownerStr = "";
                if (showOwner) {
                    ownerStr = " &7| &d" + ownerName(e.owner);
                }
                sender.sendMessage(color("&e" + (i + 1) + ". &f" + bc.name + " &7| &b" + bc.type + " &7| &a" + locStr + " &7| " + status + ownerStr));
            }
        }
        sender.sendMessage(color("&8&m------------------------------"));
    }

    private String ownerName(UUID owner) {
        try {
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(owner);
            if (op != null && op.getName() != null) return op.getName();
        } catch (Exception ignored) {}
        return owner.toString();
    }

    private boolean isProtectedByExternal(Player player, Location loc) {
        if (loc == null) return false;
        if (isResidenceProtected(player, loc)) return true;
        if (isChestShopBlock(loc)) return true;
        return false;
    }

    private boolean isResidenceProtected(Player player, Location loc) {
        if (!Bukkit.getPluginManager().isPluginEnabled("Residence")) return false;
        try {
            Class<?> residenceClass = Class.forName("com.bekvon.bukkit.residence.Residence");
            Method getInstance = residenceClass.getMethod("getInstance");
            Object residence = getInstance.invoke(null);
            Method getResidenceManager = residenceClass.getMethod("getResidenceManager");
            Object manager = getResidenceManager.invoke(residence);
            Method getByLoc = manager.getClass().getMethod("getByLoc", Location.class);
            Object res = getByLoc.invoke(manager, loc);
            if (res == null) return false;
            Method getPermissions = res.getClass().getMethod("getPermissions");
            Object perms = getPermissions.invoke(res);
            Class<?> flagsClass = Class.forName("com.bekvon.bukkit.residence.containers.Flags");
            Object buildFlag = flagsClass.getField("build").get(null);
            Method playerHas = perms.getClass().getMethod("playerHas", Player.class, flagsClass, boolean.class);
            Object allowed = playerHas.invoke(perms, player, buildFlag, false);
            if (allowed instanceof Boolean) {
                return !((Boolean) allowed);
            }
        } catch (Exception ignored) {}
        return false;
    }

    private boolean isChestShopBlock(Location loc) {
        if (!Bukkit.getPluginManager().isPluginEnabled("ChestShop")) return false;
        try {
            Class<?> signClass = Class.forName("com.Acrobot.ChestShop.Signs.ChestShopSign");
            Method isShopBlock = signClass.getMethod("isShopBlock", Block.class);
            Object result = isShopBlock.invoke(null, loc.getBlock());
            if (result instanceof Boolean) return (Boolean) result;
        } catch (Exception ignored) {}
        return false;
    }

    private static class BoundEntry {
        private final BoundContainer container;
        private final UUID owner;

        private BoundEntry(BoundContainer container, UUID owner) {
            this.container = container;
            this.owner = owner;
        }
    }

    private static class ResolvedTarget {
        private final UUID uuid;
        private final String name;

        private ResolvedTarget(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }
    }
}
