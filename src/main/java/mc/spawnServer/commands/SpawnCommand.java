package mc.spawnServer.commands;

import mc.spawnServer.SpawnServer;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class SpawnCommand implements CommandExecutor {

    private final SpawnServer plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, BukkitRunnable> pendingTps = new HashMap<>();
    private final Map<UUID, Long> spamCheck = new HashMap<>();

    public SpawnCommand(SpawnServer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.color(plugin.getConfig().getString("messages.only-player")));
            return true;
        }

        Player player = (Player) sender;


        boolean antiSpamEnabled = plugin.getConfig().getBoolean("spawn.anti-spam-enabled", true);
        if (antiSpamEnabled) {
            int antiSpamSeconds = plugin.getConfig().getInt("spawn.anti-spam-seconds");
            long antiSpamMillis = antiSpamSeconds * 1000L;
            long now = System.currentTimeMillis();

            if (spamCheck.containsKey(player.getUniqueId())) {
                long last = spamCheck.get(player.getUniqueId());
                if (now - last < antiSpamMillis) {
                    long remaining = antiSpamMillis - (now - last);
                    long secondsLeft = Math.max(1, remaining / 1000);
                    String msg = plugin.color(plugin.getConfig().getString("messages.spawn-antispam"))
                            .replace("%seconds%", String.valueOf(secondsLeft));
                    player.sendMessage(msg);
                    return true;
                }
            }
            spamCheck.put(player.getUniqueId(), now);
        }

        if (!player.hasPermission("spawnserver.spawn")) {
            player.sendMessage(plugin.color(plugin.getConfig().getString("messages.no-permission")));
            return true;
        }

        if (!plugin.getConfig().getStringList("spawn.allowed-worlds").contains(player.getWorld().getName())
                && !player.hasPermission("spawnserver.worldbypass")) {
            player.sendMessage(plugin.color(plugin.getConfig().getString("messages.spawn-wrong-world")));
            return true;
        }

        if (plugin.isInCombat(player)) {
            player.sendMessage(plugin.color(plugin.getConfig().getString("messages.spawn-no-combat")));
            return true;
        }

        if (!plugin.getSpawnConfig().contains("x")) {
            player.sendMessage(plugin.color(plugin.getConfig().getString("messages.spawn-no-spawn")));
            return true;
        }

        long now = System.currentTimeMillis();
        boolean bypassDelay = player.hasPermission("spawnserver.bypass.delay");
        boolean bypassCooldown = player.hasPermission("spawnserver.bypass.cooldown");

        if (!bypassCooldown) {
            if (cooldowns.containsKey(player.getUniqueId())) {
                long nextAllowed = cooldowns.get(player.getUniqueId());
                if (nextAllowed > now) {
                    long secondsLeft = (nextAllowed - now) / 1000;
                    player.sendMessage(plugin.color(plugin.getConfig().getString("messages.spawn-cooldown"))
                            .replace("%seconds%", String.valueOf(secondsLeft)));
                    return true;
                }
            }
        }

        int delay = plugin.getConfig().getInt("spawn.delay-seconds");
        int cooldown = plugin.getConfig().getInt("spawn.cooldown-seconds");

        Sound tempSound;
        try {
            tempSound = Sound.valueOf("UI_BUTTON_CLICK");
        } catch (IllegalArgumentException ex) {
            tempSound = Sound.valueOf("CLICK");
        }
        final Sound countdownSound = tempSound;



        Sound tempTeleport;
        try {
            tempTeleport = Sound.valueOf("ENTITY_ENDERMAN_TELEPORT"); // 1.13+
        } catch (IllegalArgumentException ex) {
            tempTeleport = Sound.valueOf("ENDERMAN_TELEPORT"); // 1.8–1.12
        }
        final Sound teleportSound = tempTeleport;


        if (!bypassDelay && delay > 0) {
            Location startLoc = player.getLocation();
            boolean cancelOnMove = plugin.getConfig().getBoolean("spawn.cancel-on-move");

            AtomicInteger secondsLeft = new AtomicInteger(delay);

            BukkitRunnable task = new BukkitRunnable() {
                @Override
                public void run() {
                    int current = secondsLeft.getAndDecrement();

                    if (current <= 0) {
                        plugin.teleportToSpawn(player);
                        player.sendMessage(plugin.color(plugin.getConfig().getString("messages.spawn-success")));

                        player.playSound(player.getLocation(), teleportSound, 1.0f, 1.0f);

                        if (!bypassCooldown) {
                            cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + cooldown * 1000L);
                        }

                        pendingTps.remove(player.getUniqueId());
                        cancel();
                        return;
                    }

                    if (cancelOnMove && player.getLocation().distanceSquared(startLoc) > 0.1) {
                        player.sendMessage(plugin.color(plugin.getConfig().getString("messages.spawn-moved")));
                        pendingTps.remove(player.getUniqueId());
                        cancel();
                        return;
                    }

                    String delayMsg = plugin.color(plugin.getConfig().getString("messages.spawn-in-delay"))
                            .replace("%seconds%", String.valueOf(current));
                    player.sendMessage(delayMsg);

                    player.playSound(player.getLocation(), countdownSound, 1.0f, 1.0f);
                }
            };

            if (pendingTps.containsKey(player.getUniqueId())) {
                pendingTps.get(player.getUniqueId()).cancel();
                pendingTps.remove(player.getUniqueId());
            }

            pendingTps.put(player.getUniqueId(), task);
            task.runTaskTimer(plugin, 0L, 20L);

            return true;

        } else {
            plugin.teleportToSpawn(player);
            player.sendMessage(plugin.color(plugin.getConfig().getString("messages.spawn-success")));

            player.playSound(player.getLocation(), teleportSound, 1.0f, 1.0f);

            if (!bypassCooldown) {
                cooldowns.put(player.getUniqueId(), now + cooldown * 1000L);
            }
            return true;
        }
    }

    
}
