package fr.justesmp.lobbyprotection;

import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class LobbyProtection extends JavaPlugin implements Listener {
    private static final String LOBBY_WORLD = "Lobby";

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Protection active pour le monde : " + LOBBY_WORLD);
    }

    private boolean isLobby(World world) {
        return world != null && LOBBY_WORLD.equalsIgnoreCase(world.getName());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && isLobby(player.getWorld())) {
            event.setCancelled(true);
            player.setFireTicks(0);
            player.setFreezeTicks(0);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onFood(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player && isLobby(player.getWorld())) {
            event.setCancelled(true);
            player.setFoodLevel(20);
            player.setSaturation(20.0f);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        protectIfLobby(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (isLobby(event.getRespawnLocation().getWorld())) {
            // The player is protected by the damage and hunger listeners once respawned.
        }
    }

    private void protectIfLobby(Player player) {
        if (isLobby(player.getWorld())) {
            player.setFoodLevel(20);
            player.setSaturation(20.0f);
            player.setFireTicks(0);
            player.setFreezeTicks(0);
        }
    }
}
