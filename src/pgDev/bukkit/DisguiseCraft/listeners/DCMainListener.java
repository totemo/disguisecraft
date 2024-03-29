package pgDev.bukkit.DisguiseCraft.listeners;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetEvent.TargetReason;
import org.bukkit.event.player.*;

import pgDev.bukkit.DisguiseCraft.*;
import pgDev.bukkit.DisguiseCraft.disguise.*;
import pgDev.bukkit.DisguiseCraft.listeners.attack.InvalidInteractHandler;
import pgDev.bukkit.DisguiseCraft.threading.NamedThreadFactory;
import pgDev.bukkit.DisguiseCraft.update.DCUpdateNotifier;

public class DCMainListener implements Listener {
	final DisguiseCraft plugin;
	
	public ExecutorService invalidInteractExecutor = Executors.newFixedThreadPool(
			DisguiseCraft.pluginSettings.pvpThreads, new NamedThreadFactory("DCInvalidInteractHandler"));
	
	public DCMainListener(final DisguiseCraft plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		
		// Show disguises to newly joined players
		plugin.showWorldDisguises(player);
		
		// If he was a disguise-quitter, tell him
		if (plugin.disguiseQuitters.contains(player.getName())) {
			event.getPlayer().sendMessage(ChatColor.RED + "You were undisguised because you left the server.");
			plugin.disguiseQuitters.remove(player.getName());
		}
		
		// Has a disguise?
		if (plugin.disguiseDB.containsKey(player.getName())) {
			Disguise disguise = plugin.disguiseDB.get(player.getName());
			if (disguise.hasPermission(player)) {
				plugin.disguiseIDs.put(disguise.entityID, player);
				plugin.disguiseToWorld(player, player.getWorld());
				if (disguise.type.isPlayer()) {
					player.sendMessage(ChatColor.GOLD + "You were redisguised as player: " + disguise.data.getFirst());
				} else {
					player.sendMessage(ChatColor.GOLD + "You were redisguised as a " + ChatColor.DARK_GREEN + disguise.type.name());
				}
				
				// Start position updater
				plugin.setPositionUpdater(player, disguise);
			} else {
				plugin.disguiseDB.remove(player.getName());
				player.sendMessage(ChatColor.RED + "You do not have the permissions required to wear your disguise in this world.");
			}
		}
		
		// Updates?
		if (DisguiseCraft.pluginSettings.updateNotification && player.hasPermission("disguisecraft.update")) {
			// Check for new DisguiseCraft version
			plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new DCUpdateNotifier(plugin, player));
			
			// Bad configuration?
			if (DisguiseCraft.protocolManager == null) {
				if (DisguiseCraft.pluginSettings.disguisePVP) {
					player.sendMessage(ChatColor.RED + "DisguiseCraft's configuration has " + ChatColor.GOLD + "\"disguisePVP\" " +
							ChatColor.RED + "set to " + ChatColor.GOLD + "true " + ChatColor.RED + "but ProtocolLib is not installed!");
				}
				if (DisguiseCraft.pluginSettings.noTabHide) {
					player.sendMessage(ChatColor.RED + "DisguiseCraft's configuration has " + ChatColor.GOLD + "\"noTabHide\" " +
							ChatColor.RED + "set to " + ChatColor.GOLD + "true " + ChatColor.RED + "but ProtocolLib is not installed!");
				}
			}
		}
	}
	
	@EventHandler
	public void onDisguiseHit(PlayerInvalidInteractEvent event) {
		invalidInteractExecutor.execute(new InvalidInteractHandler(event, plugin));
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		
		// Undisguise them because they left
		if (plugin.disguiseDB.containsKey(player.getName())) {
			plugin.disguiseIDs.remove(plugin.disguiseDB.get(player.getName()).entityID);
			if (DisguiseCraft.pluginSettings.quitUndisguise) {
				plugin.unDisguisePlayer(player);
				plugin.disguiseQuitters.add(player.getName());
			} else {
				plugin.undisguiseToWorld(player, player.getWorld());
			}
		}
		
		// Undisguise others
		plugin.halfUndisguiseAllToPlayer(player);
		
		// Stop position updater
		plugin.removePositionUpdater(player);
	}
	
	@EventHandler
	public void onPlayerWorldChange(PlayerChangedWorldEvent event) {
		// Handle this next tick
		plugin.getServer().getScheduler().runTask(plugin, new WorldChangeUpdater(plugin, event));
	}
	
	@EventHandler(priority = EventPriority.LOW)
	public void onRespawn(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		if (plugin.disguiseDB.containsKey(player.getName())) {
			// Respawn disguise
			plugin.sendPacketToWorld(player.getWorld(), plugin.disguiseDB.get(player.getName()).packetGenerator.getSpawnPacket(event.getRespawnLocation()));
		}
		
		//Show the disguises to the player (in later ticks)
		plugin.getServer().getScheduler().runTaskLater(plugin, new DisguiseViewResetter(plugin, player), DisguiseCraft.pluginSettings.respawnResetDelay);
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onTarget(EntityTargetEvent event) {
		if (event.getTarget() instanceof Player) {
			Player player = (Player) event.getTarget();
			if (plugin.disguiseDB.containsKey(player.getName())) {
				if (player.hasPermission("disguisecraft.notarget")) {
					if (player.hasPermission("disguisecraft.notarget.strict")) {
						event.setCancelled(true);
					} else {
						if (!plugin.disguiseDB.get(player.getName()).type.isPlayer() && (event.getReason() == TargetReason.CLOSEST_PLAYER || event.getReason() == TargetReason.RANDOM_TARGET)) {
							event.setCancelled(true);
						}
					}
				}
			}
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPickup(PlayerPickupItemEvent event) {
		if (plugin.disguiseDB.containsKey(event.getPlayer().getName())) {
			Disguise disguise = plugin.disguiseDB.get(event.getPlayer().getName());
			if (disguise.data != null && disguise.data.contains("nopickup")) {
				event.setCancelled(true);
			}
		}
	}
}
