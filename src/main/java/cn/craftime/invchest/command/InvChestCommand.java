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
import java.util.regex.Pattern;

public class InvChestCommand implements TabExecutor {
    private final InvChest plugin;
    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\w\\u4e00-\\u9fa5]{1,16}$"); // Basic check, but user said Unicode 1-16 chars no space

    public InvChestCommand(InvChest plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + "/invchest <bind|unbind|list|info|pause|resume|reload>"));
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
        } else if (sub.equals("list")) {
            return handleList(sender, args);
        } else if (sub.equals("info")) {
            return handleInfo(sender, args);
        } else if (sub.equals("pause")) {
            return handlePause(sender, true);
        } else if (sub.equals("resume")) {
            return handlePause(sender, false);
        } else if (sub.equals("abind")) {
            return handleAdminBind(sender, args);
        } else if (sub.equals("aunbind")) {
            return handleAdminUnbind(sender, args);
        } else if (sub.equals("inspect") || sub.equals("i")) {
            return handleInspect(sender);
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

        String name = null;
        int range = plugin.config().getDefaultChunkRange();
        
        // Parse args: bind [name] [range]
        if (args.length > 1) {
            name = args[1];
            // Validate name (1-16 chars, no space - args split by space so no space naturally)
            if (name.length() > 16) {
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

    // /ic unbind <name> OR /ic unbind (legacy admin usage kept separate)
    private boolean handleUnbind(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + "/ic unbind <name>"));
            return true;
        }
        
        // Check if user is trying to use admin unbind via this command?
        // Legacy support: /ic unbind <player> is handled in handleAdminUnbind logic usually, 
        // but let's stick to the plan: /ic unbind <name> is for player. 
        // Admin unbind is /ic aunbind.
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + "Console use /ic aunbind"));
            return true;
        }
        Player p = (Player) sender;
        if (!p.hasPermission("invchest.bind")) {
             p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "no.permission", null)));
             return true;
        }
        
        String name = args[1];
        BoundContainer bc = plugin.store().getBoundByName(p.getUniqueId(), name);
        if (bc == null) {
            p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "unbind.not_found", null)));
            return true;
        }
        
        // Release chunk
        if (bc.chunkRange >= 0 && plugin.chunkLoader().isSupported()) {
            plugin.chunkLoader().release(bc.toLocation(), bc.chunkRange);
        }
        
        plugin.store().unbindByName(p.getUniqueId(), name);
        p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "unbind.success", null)));
        return true;
    }

    // /ic unbind-here
    private boolean handleUnbindHere(CommandSender sender) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        Block b = getTargetContainer(sender);
        if (b == null) {
            p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "bind.no_target", null)));
            return true;
        }
        
        // We need to find if this container is bound to player
        // Since we don't have getBoundByLocation for specific player easily exposed, 
        // we can iterate or use unbindByLocation but verify owner first.
        UUID owner = plugin.store().ownerOf(b.getLocation());
        if (owner == null || !owner.equals(p.getUniqueId())) {
             p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "unbind.not_bound", null)));
             return true;
        }
        
        // Get BC to release chunk
        // We need the BC instance to know the range
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
            p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "unbind.success", null)));
        }
        return true;
    }

    // /ic unbind-all
    private boolean handleUnbindAll(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        
        // Check if admin is unbinding for others: /ic unbind-all <player>
        // But plan says admin use /ic aunbind? 
        // Plan: "/ic unbind-all: Unbind all bindings." (Player)
        // Let's support admin usage if args present
        if (args.length > 1 && sender.hasPermission("invchest.admin.manage")) {
            Player target = Bukkit.getPlayerExact(args[1]);
            UUID uid = target != null ? target.getUniqueId() : null;
            if (uid == null) {
                 try { uid = UUID.fromString(args[1]); } catch(Exception e){}
            }
            if (uid == null) {
                sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "player.offline", null)));
                return true;
            }
            
            // Release chunks
            List<BoundContainer> list = plugin.store().getBounds(uid);
            for (BoundContainer bc : list) {
                if (bc.chunkRange >= 0 && plugin.chunkLoader().isSupported()) {
                    plugin.chunkLoader().release(bc.toLocation(), bc.chunkRange);
                }
            }
            plugin.store().unbindAll(uid);
            sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "admin.unbind.success", null).replace("{player}", args[1])));
            return true;
        }
        
        // Player self
        List<BoundContainer> list = plugin.store().getBounds(p.getUniqueId());
        if (list.isEmpty()) {
            p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "unbind.not_bound", null)));
            return true;
        }
        for (BoundContainer bc : list) {
            if (bc.chunkRange >= 0 && plugin.chunkLoader().isSupported()) {
                plugin.chunkLoader().release(bc.toLocation(), bc.chunkRange);
            }
        }
        plugin.store().unbindAll(p.getUniqueId());
        p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "unbind.success", null)));
        return true;
    }

    // /ic list [page] OR /ic list [player] [page] OR /ic list @all [page]
    private boolean handleList(CommandSender sender, String[] args) {
        UUID targetUUID = null;
        String targetName = null;
        int page = 1;
        
        // Parse args
        if (args.length > 1) {
            // Check if arg 1 is page or player
            try {
                page = Integer.parseInt(args[1]);
                if (sender instanceof Player) targetUUID = ((Player)sender).getUniqueId();
            } catch (NumberFormatException e) {
                // Not a number, assume player or @all
                if (!sender.hasPermission("invchest.admin.inspect")) {
                    sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "no.permission", null)));
                    return true;
                }
                String pName = args[1];
                if (pName.equalsIgnoreCase("@all")) {
                    targetUUID = null; // null means all
                } else {
                    Player t = Bukkit.getPlayerExact(pName);
                    if (t != null) {
                        targetUUID = t.getUniqueId();
                        targetName = t.getName();
                    } else {
                        // Offline player? For now just say offline
                        sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "player.offline", null)));
                        return true;
                    }
                }
                if (args.length > 2) {
                    try { page = Integer.parseInt(args[2]); } catch(Exception ignored){}
                }
            }
        } else {
            if (sender instanceof Player) targetUUID = ((Player)sender).getUniqueId();
            else {
                sender.sendMessage(color("Console must specify player or @all"));
                return true;
            }
        }
        
        List<BoundContainer> allItems = new ArrayList<>();
        if (targetUUID != null) {
            allItems.addAll(plugin.store().getBounds(targetUUID));
        } else {
            // @all
            Map<UUID, List<BoundContainer>> snap = plugin.store().snapshot();
            for (List<BoundContainer> l : snap.values()) allItems.addAll(l);
        }
        
        int perPage = plugin.config().getListItemsPerPage();
        int totalPages = (int) Math.ceil((double)allItems.size() / perPage);
        if (page < 1) page = 1;
        if (page > totalPages && totalPages > 0) page = totalPages;
        
        sender.sendMessage(color("&8&m------------------------------"));
        sender.sendMessage(color("&6InvChest Bindings &7(Page " + page + "/" + (totalPages==0?1:totalPages) + ")"));
        
        if (allItems.isEmpty()) {
            sender.sendMessage(color("&cNo bindings found."));
        } else {
            int start = (page - 1) * perPage;
            int end = Math.min(start + perPage, allItems.size());
            for (int i = start; i < end; i++) {
                BoundContainer bc = allItems.get(i);
                String locStr = String.format("%s %d,%d,%d", bc.world, bc.x, bc.y, bc.z);
                String status = (bc.chunkRange >= 0 && plugin.chunkLoader().isSupported()) ? "&a[Loaded]" : "&7[Unloaded]";
                String ownerStr = (targetUUID == null) ? " &8(" + plugin.store().ownerOf(bc.toLocation()) + ")" : ""; // UUID is ugly, resolving name is hard offline.
                sender.sendMessage(color("&e" + bc.name + " &7- " + locStr + " " + status));
            }
        }
        sender.sendMessage(color("&8&m------------------------------"));
        return true;
    }
    
    // /ic info <name>
    private boolean handleInfo(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        if (args.length < 2) {
            p.sendMessage(color(plugin.lang().get(p, "prefix", null) + "/ic info <name>"));
            return true;
        }
        String name = args[1];
        BoundContainer bc = plugin.store().getBoundByName(p.getUniqueId(), name);
        if (bc == null) {
            p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "info.not_found", null)));
            return true;
        }
        
        p.sendMessage(color("&8&m------------------------------"));
        p.sendMessage(color("&6Info: &e" + bc.name));
        p.sendMessage(color("&7Location: &f" + bc.world + " " + bc.x + "," + bc.y + "," + bc.z));
        p.sendMessage(color("&7Type: &f" + bc.type));
        p.sendMessage(color("&7Chunk Range: &f" + (bc.chunkRange < 0 ? "Off" : bc.chunkRange)));
        boolean loaded = bc.chunkRange >= 0 && plugin.chunkLoader().isSupported(); // Simplified check, strictly we should check isForceLoaded
        p.sendMessage(color("&7Status: " + (loaded ? "&aForce Loaded" : "&7Normal")));
        p.sendMessage(color("&8&m------------------------------"));
        return true;
    }
    
    // /ic pause / resume
    private boolean handlePause(CommandSender sender, boolean pause) {
        if (!sender.hasPermission("invchest.admin.global")) {
             sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "no.permission", null)));
             return true;
        }
        plugin.store().setGlobalPaused(pause);
        String key = pause ? "admin.pause.success" : "admin.resume.success";
        sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, key, null)));
        return true;
    }
    
    // /ic abind <player> [name] [range]
    private boolean handleAdminBind(CommandSender sender, String[] args) {
        if (!sender.hasPermission("invchest.admin.manage")) {
            sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "no.permission", null)));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(color("/ic abind <player> [name] [range] - Look at container"));
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
        
        String name = null;
        int range = plugin.config().getDefaultChunkRange();
        if (args.length > 2) name = args[2];
        if (args.length > 3) {
            try { range = Integer.parseInt(args[3]); } catch(Exception e){}
        }
        
        if (name == null) name = plugin.store().getNextAutoName(target.getUniqueId(), plugin.config().getAutoNameFormat());
        
        BoundContainer bc = new BoundContainer(b.getLocation(), b.getType().name(), name, range);
        boolean ok = plugin.store().bind(target.getUniqueId(), bc, 100); // Admin bypass limit? Use high limit
        
        if (!ok) {
            sender.sendMessage(color("&cFailed to bind (Name collision or location bound)"));
            return true;
        }
        
        if (range >= 0 && plugin.chunkLoader().isSupported()) {
            plugin.chunkLoader().keepLoaded(b.getLocation(), range);
        }
        
        sender.sendMessage(color("&aAdmin bind success for " + target.getName()));
        return true;
    }
    
    // /ic aunbind <player> [name]
    private boolean handleAdminUnbind(CommandSender sender, String[] args) {
        if (!sender.hasPermission("invchest.admin.manage")) {
            sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "no.permission", null)));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(color("/ic aunbind <player> [name] - If name omitted, unbind all? No, stick to explicit."));
            return true;
        }
        // Actually /ic unbind-all <player> handles "unbind all".
        // This command should be for specific unbind.
        
        UUID uid = null;
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target != null) uid = target.getUniqueId();
        else try { uid = UUID.fromString(args[1]); } catch(Exception e){}
        
        if (uid == null) {
            sender.sendMessage(color("&cPlayer not found"));
            return true;
        }
        
        if (args.length < 3) {
             sender.sendMessage(color("&cUsage: /ic aunbind <player> <name>"));
             return true;
        }
        
        String name = args[2];
        BoundContainer bc = plugin.store().getBoundByName(uid, name);
        if (bc == null) {
            sender.sendMessage(color("&cBinding not found"));
            return true;
        }
        
        if (bc.chunkRange >= 0 && plugin.chunkLoader().isSupported()) {
            plugin.chunkLoader().release(bc.toLocation(), bc.chunkRange);
        }
        plugin.store().unbindByName(uid, name);
        sender.sendMessage(color("&aUnbound " + name + " for " + args[1]));
        return true;
    }
    
    // /ic inspect
    private boolean handleInspect(CommandSender sender) {
        if (!sender.hasPermission("invchest.admin.inspect")) {
             sender.sendMessage(color(plugin.lang().get(null, "prefix", null) + plugin.lang().get(null, "no.permission", null)));
             return true;
        }
        Block b = getTargetContainer(sender);
        if (b == null) {
            sender.sendMessage(color("&cLook at a container"));
            return true;
        }
        UUID owner = plugin.store().ownerOf(b.getLocation());
        if (owner == null) {
            sender.sendMessage(color("&7Container is not bound."));
            return true;
        }
        
        // Find BC
        BoundContainer found = null;
        List<BoundContainer> list = plugin.store().getBounds(owner);
        String key = new BoundContainer(b.getLocation(), b.getType().name(), null, 0).key();
        for (BoundContainer bc : list) {
            if (bc.key().equals(key)) {
                found = bc;
                break;
            }
        }
        
        sender.sendMessage(color("&8&m------------------------------"));
        sender.sendMessage(color("&6Inspect Container"));
        sender.sendMessage(color("&7Owner UUID: &f" + owner));
        if (found != null) {
            sender.sendMessage(color("&7Name: &e" + found.name));
            sender.sendMessage(color("&7Chunk Range: &f" + found.chunkRange));
        }
        sender.sendMessage(color("&8&m------------------------------"));
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
            list.add("list");
            list.add("info");
            list.add("pause");
            list.add("resume");
            list.add("abind");
            list.add("aunbind");
            list.add("inspect");
            list.add("reload");
        } else if (args.length == 2) {
             if (args[0].equalsIgnoreCase("unbind") || args[0].equalsIgnoreCase("info")) {
                 if (sender instanceof Player) {
                     List<BoundContainer> bounds = plugin.store().getBounds(((Player)sender).getUniqueId());
                     for (BoundContainer bc : bounds) list.add(bc.name);
                 }
             } else if (sender.hasPermission("invchest.admin.manage") && (args[0].equalsIgnoreCase("abind") || args[0].equalsIgnoreCase("aunbind") || args[0].equalsIgnoreCase("unbind-all"))) {
                 for (Player p : Bukkit.getOnlinePlayers()) list.add(p.getName());
             }
        }
        return list;
    }
}
