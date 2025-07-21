package me.watermanmc.waterdamage;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.bukkit.ChatColor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Plugin extends JavaPlugin implements Listener {

    private final Map<UUID, Double> fallingPlayers = new HashMap<>();
    private boolean featureEnabled;
    private File messagesFile;
    private FileConfiguration messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        createMessagesFile();
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info(ChatColor.GREEN + "Enabling WaterDamage v" + getDescription().getVersion() + "...");
        if (!featureEnabled) {
            getLogger().warning("Feature is currently disabled in config.yml!");
        }
    }

    @Override
    public void onDisable() {
        fallingPlayers.clear();
        getLogger().info(ChatColor.RED + "Disabling WaterDamage v" + getDescription().getVersion() + "...");
    }

    private void createMessagesFile() {
        messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            messagesFile.getParentFile().mkdirs();
            saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    private void loadConfig() {
        reloadConfig();
        featureEnabled = getConfig().getBoolean("feature-enabled", true);
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    private String getMessage(String path) {
        String msg = messages.getString(path, "Message not found: " + path);
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("waterdamagereload")) {
            if (sender.hasPermission("waterdamage.reload")) {
                loadConfig();
                sender.sendMessage(getMessage("command.reload-success"));
                if (!featureEnabled) {
                    sender.sendMessage(getMessage("command.feature-disabled-note"));
                }
            } else {
                sender.sendMessage(getMessage("command.no-permission"));
            }
            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!featureEnabled) return;

        Player player = event.getPlayer();
        Location to = event.getTo();
        Location from = event.getFrom();

        if (to.getBlockX() == from.getBlockX() && to.getBlockY() == from.getBlockY() && to.getBlockZ() == from.getBlockZ()) {
            return;
        }

        UUID playerUUID = player.getUniqueId();
        Vector velocity = player.getVelocity();

        Block toBlock = to.getBlock();

        if (!player.isOnGround() && velocity.getY() < -0.08 && !fallingPlayers.containsKey(playerUUID)) {
            if (!from.getBlock().isLiquid() && from.getBlock().getType() != Material.POWDER_SNOW) {
                fallingPlayers.put(playerUUID, from.getY());
            }
        }

        if (fallingPlayers.containsKey(playerUUID)) {
            double fallStartHeight = fallingPlayers.get(playerUUID);

            if (player.isOnGround()) {
                fallingPlayers.remove(playerUUID);
                return;
            }

            boolean landedInTargetBlock = false;
            if (toBlock.isLiquid() || toBlock.getType() == Material.POWDER_SNOW) {
                landedInTargetBlock = true;
            }

            if (landedInTargetBlock) {
                double fallEndHeight = to.getY();
                double fallDistance = fallStartHeight - fallEndHeight;

                if (fallDistance > 3.0) {
                    double damage = (fallDistance - 3.0) * 1.5;

                    if (damage > 0) {
                        player.damage(damage, player);
                    }
                }
                fallingPlayers.remove(playerUUID);
            }
        }
    }
}
