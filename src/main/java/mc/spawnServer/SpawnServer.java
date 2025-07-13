package mc.spawnServer;

import mc.spawnServer.commands.SpawnServerCommand;
import mc.spawnServer.commands.SpawnCommand;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.io.File;
import java.io.IOException;

public class SpawnServer extends JavaPlugin implements Listener {

    private final String PREFIX = "§8[§aSpawnServer§8] ";

    private File spawnFile;
    private FileConfiguration spawnConfig;

    private BukkitAudiences adventure;

    @Override
    public void onEnable() {
        try {
            Bukkit.getConsoleSender().sendMessage("");
            Bukkit.getConsoleSender().sendMessage(PREFIX + "§eCargando plugin");
            Bukkit.getConsoleSender().sendMessage(PREFIX + "§dDeveloper: NekiroVT");
            Bukkit.getConsoleSender().sendMessage(PREFIX + "§a✅ SpawnServer cargado correctamente.");
            Bukkit.getConsoleSender().sendMessage("");

            this.adventure = BukkitAudiences.create(this);

            Bukkit.getPluginManager().registerEvents(this, this);

            SpawnServerCommand spawnServerCommand = new SpawnServerCommand(this);
            getCommand("spawnserver").setExecutor(spawnServerCommand);
            getCommand("spawnserver").setTabCompleter(spawnServerCommand);
            getCommand("spawn").setExecutor(new SpawnCommand(this));

            saveDefaultConfig();
            setupSpawnConfig();

        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(PREFIX + "§4❌ Error al cargar SpawnServer.");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(PREFIX + "§cDesactivando plugin...");
        Bukkit.getConsoleSender().sendMessage("");

        if (this.adventure != null) {
            this.adventure.close();
        }
    }

    public BukkitAudiences adventure() {
        return this.adventure;
    }

    public void setupSpawnConfig() {
        spawnFile = new File(getDataFolder(), "spawn.yml");
        if (!spawnFile.exists()) {
            spawnFile.getParentFile().mkdirs();
            try {
                spawnFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        spawnConfig = YamlConfiguration.loadConfiguration(spawnFile);
    }

    public void saveSpawnConfig() {
        try {
            spawnConfig.save(spawnFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getSpawnConfig() {
        return spawnConfig;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        boolean alwaysTp = getConfig().getBoolean("always-teleport-to-spawn");

        if (alwaysTp && spawnConfig.contains("x")) {
            teleportToSpawn(player);
        } else if (!alwaysTp && !player.hasPlayedBefore() && spawnConfig.contains("x")) {
            teleportToSpawn(player);
        }

        if (getConfig().getBoolean("show-join-message")) {
            String joinMessage = getConfig().getString("join-message");
            joinMessage = joinMessage.replace("&", "§").replace("%player%", player.getName());
            player.sendMessage(joinMessage);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        boolean forceSpawn = getConfig().getBoolean("force-spawn-on-death", false);

        if (!getSpawnConfig().contains("x")) return;

        String worldName = getSpawnConfig().getString("world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        double x = getSpawnConfig().getDouble("x");
        double y = getSpawnConfig().getDouble("y");
        double z = getSpawnConfig().getDouble("z");
        float yaw = (float) getSpawnConfig().getDouble("yaw");
        float pitch = (float) getSpawnConfig().getDouble("pitch");

        Location pluginSpawn = new Location(world, x, y, z, yaw, pitch);

        if (forceSpawn) {
            event.setRespawnLocation(pluginSpawn);
        } else {
            if (!event.isBedSpawn() && !event.isAnchorSpawn()) {
                event.setRespawnLocation(pluginSpawn);
            }
        }
    }

    public void teleportToSpawn(Player player) {
        String worldName = getSpawnConfig().getString("world");
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            player.sendMessage("§cNo se pudo encontrar el mundo: " + worldName);
            return;
        }

        double x = spawnConfig.getDouble("x");
        double y = spawnConfig.getDouble("y");
        double z = spawnConfig.getDouble("z");
        float yaw = (float) spawnConfig.getDouble("yaw");
        float pitch = (float) spawnConfig.getDouble("pitch");

        Location loc = new Location(world, x, y, z, yaw, pitch);
        player.teleport(loc);
    }

    public boolean isInCombat(Player player) {
        return false;
    }

    public String color(String msg) {
        return msg.replace("&", "§");
    }

    public boolean hasPermission(Player player, String perm) {
        return player.hasPermission(perm);
    }

}
