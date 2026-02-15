package cn.craftime.invchest.listener;

import cn.craftime.invchest.InvChest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerSessionListener implements Listener {
    private final InvChest plugin;

    public PlayerSessionListener(InvChest plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.chunkLoader().updatePlayer(event.getPlayer().getUniqueId(), true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.chunkLoader().updatePlayer(event.getPlayer().getUniqueId(), false);
    }
}
