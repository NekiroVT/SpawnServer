package mc.spawnServer.commands;

import mc.spawnServer.SpawnServer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SpawnServerCommand implements CommandExecutor, TabCompleter {

    private final SpawnServer plugin;

    public SpawnServerCommand(SpawnServer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        boolean isPlayer = sender instanceof Player;
        boolean hasAdmin = isPlayer ? ((Player) sender).hasPermission("spawnserver.admin") : true;

        if (!hasAdmin) {
            sender.sendMessage(plugin.color(plugin.getConfig().getString("messages.no-permission")));
            return true;
        }

        if (args.length == 0) {
            String usagePlayer = plugin.getConfig().getString("messages.usage-player")
                    .replace("%label%", label);
            String usageConsole = plugin.getConfig().getString("messages.usage-console")
                    .replace("%label%", label);

            if (isPlayer) {
                sender.sendMessage(plugin.color(usagePlayer));
            } else {
                sender.sendMessage(plugin.color(usageConsole));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {
            if (!isPlayer) {
                sender.sendMessage(plugin.color(plugin.getConfig().getString("messages.only-player")));
                return true;
            }

            Player player = (Player) sender;

            plugin.getSpawnConfig().set("world", player.getWorld().getName());
            plugin.getSpawnConfig().set("x", player.getLocation().getX());
            plugin.getSpawnConfig().set("y", player.getLocation().getY());
            plugin.getSpawnConfig().set("z", player.getLocation().getZ());
            plugin.getSpawnConfig().set("yaw", player.getLocation().getYaw());
            plugin.getSpawnConfig().set("pitch", player.getLocation().getPitch());
            plugin.saveSpawnConfig();

            player.sendMessage(plugin.color(plugin.getConfig().getString("messages.spawn-set")));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.setupSpawnConfig();
            sender.sendMessage(plugin.color(plugin.getConfig().getString("messages.reloaded")));
            return true;
        }


        String usagePlayer = plugin.getConfig().getString("messages.usage-player")
                .replace("%label%", label);
        String usageConsole = plugin.getConfig().getString("messages.usage-console")
                .replace("%label%", label);

        if (isPlayer) {
            sender.sendMessage(plugin.color(usagePlayer));
        } else {
            sender.sendMessage(plugin.color(usageConsole));
        }
        return true;
    }




    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            boolean isPlayer = sender instanceof Player;

            if (!isPlayer) {
                completions.add("reload");
                return completions;
            }

            Player player = (Player) sender;

            if (plugin.hasPermission(player, "spawnserver.set") || plugin.hasPermission(player, "spawnserver.admin")) {
                completions.add("set");
            }

            if (plugin.hasPermission(player, "spawnserver.reload") || plugin.hasPermission(player, "spawnserver.admin")) {
                completions.add("reload");
            }


        }

        return completions;
    }



}
