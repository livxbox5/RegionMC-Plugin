package org.KillerYT.regionMC.managers;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.region.Region;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class GlobalRegionManager {
    private final RegionMC plugin;
    private final RegionManager regionManager;
    private final File regionsFolder;
    private TimeLockManager timeLockManager;

    public GlobalRegionManager(RegionMC plugin, RegionManager regionManager) {
        this.plugin = plugin;
        this.regionManager = regionManager;
        this.regionsFolder = regionManager.getRegionsFolder();
        createRegionsFolder();
        createGlobalRegions();
        initTimeLockManager();
    }

    private void initTimeLockManager() {
        this.timeLockManager = regionManager.getTimeLockManager() != null
                ? regionManager.getTimeLockManager()
                : new TimeLockManager(plugin);
    }

    private void createRegionsFolder() {
        if (!regionsFolder.exists() && regionsFolder.mkdirs()) {
            plugin.getLogger().info("Created regions folder: " + regionsFolder.getPath());
        }
    }

    public void createGlobalRegions() {
        loadGlobalRegionsFromFiles();
        ensureGlobalRegionsExist();
    }

    private void loadGlobalRegionsFromFiles() {
        File[] regionFiles = regionsFolder.listFiles((dir, name) ->
                name.endsWith(".yml") && name.startsWith("__Global__"));

        if (regionFiles == null || regionFiles.length == 0) {
            plugin.getLogger().info("No global region files found, will create them");
            return;
        }

        int loadedCount = 0;
        for (File regionFile : regionFiles) {
            if (loadGlobalRegionFromFile(regionFile)) {
                loadedCount++;
            }
        }
        plugin.getLogger().info("Successfully loaded " + loadedCount + " global regions from files");
    }

    private boolean loadGlobalRegionFromFile(File regionFile) {
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(regionFile);
            String fileName = regionFile.getName();
            String regionName = fileName.substring(0, fileName.length() - 4);

            if (!isGlobalRegionName(regionName)) {
                return false;
            }

            String worldName = extractWorldNameFromGlobalRegion(regionName);
            World world = Bukkit.getWorld(worldName);

            if (world == null) {
                plugin.getLogger().warning("World not found for global region: " + worldName);
                return false;
            }

            Location pos1 = new Location(world, -30000000, world.getMinHeight(), -30000000);
            Location pos2 = new Location(world, 30000000, world.getMaxHeight(), 30000000);

            if (config.contains("pos1") && config.contains("pos2")) {
                var pos1Section = config.getConfigurationSection("pos1");
                var pos2Section = config.getConfigurationSection("pos2");
                if (pos1Section != null && pos2Section != null) {
                    pos1 = new Location(world,
                            pos1Section.getDouble("x", -30000000),
                            pos1Section.getDouble("y", world.getMinHeight()),
                            pos1Section.getDouble("z", -30000000));
                    pos2 = new Location(world,
                            pos2Section.getDouble("x", 30000000),
                            pos2Section.getDouble("y", world.getMaxHeight()),
                            pos2Section.getDouble("z", 30000000));
                }
            }

            boolean success = regionManager.createWorldRegion(regionName, pos1, pos2);

            if (success) {
                plugin.getLogger().info("Loaded global region: " + regionName);
                Region region = regionManager.getRegion(regionName);
                if (region != null && config.contains("flags")) {
                    var flagsSection = config.getConfigurationSection("flags");
                    if (flagsSection != null) {
                        flagsSection.getKeys(false).forEach(flag ->
                                region.setFlag(flag, flagsSection.get(flag)));
                    }
                }
            }
            return success;
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading global region: " + e.getMessage());
            return false;
        }
    }

    private void ensureGlobalRegionsExist() {
        plugin.getLogger().info("Ensuring global regions exist for all worlds...");
        int count = 0;

        for (World world : Bukkit.getWorlds()) {
            String globalRegionName = getGlobalRegionNameForWorld(world.getName());

            if (regionManager.regionExists(globalRegionName)) {
                Region region = regionManager.getRegion(globalRegionName);
                if (region != null) {
                    region.setPriority(0);
                    setGlobalRegionFlags(region);
                    regionManager.saveRegion(region);
                    count++;
                }
            } else {
                Location pos1 = new Location(world, -30000000, world.getMinHeight(), -30000000);
                Location pos2 = new Location(world, 30000000, world.getMaxHeight(), 30000000);

                if (regionManager.createWorldRegion(globalRegionName, pos1, pos2)) {
                    Region region = regionManager.getRegion(globalRegionName);
                    if (region != null) {
                        region.setPriority(0);
                        setGlobalRegionFlags(region);
                        regionManager.saveRegion(region);
                        count++;
                    }
                }
            }
        }
        plugin.getLogger().info("Ensured global regions for " + Bukkit.getWorlds().size() +
                " worlds (updated: " + count + ")");
    }

    public void applyGlobalTimeLocks() {
        if (timeLockManager == null) {
            plugin.getLogger().warning("TimeLockManager is not initialized");
            return;
        }

        for (World world : Bukkit.getWorlds()) {
            Region globalRegion = getGlobalRegion(world);
            if (globalRegion == null) continue;

            String timeLockMode = globalRegion.getFlagValue("time-lock", String.class);
            if (timeLockMode == null) timeLockMode = "allow";

            switch (timeLockMode.toLowerCase()) {
                case "deny" -> {
                    long currentTime = world.getTime();
                    timeLockManager.setTimeLockMode(world.getName(), "deny", currentTime);
                }
                case "day" -> {
                    timeLockManager.setTimeLockMode(world.getName(), "day", 6000L);
                    world.setTime(6000L);
                }
                case "night" -> {
                    timeLockManager.setTimeLockMode(world.getName(), "night", 18000L);
                    world.setTime(18000L);
                }
                default -> timeLockManager.removeFixedTime(world.getName());
            }
        }
    }

    public Region getGlobalRegion(World world) {
        return world != null ? regionManager.getRegion(getGlobalRegionNameForWorld(world.getName())) : null;
    }

    private void setGlobalRegionFlags(Region region) {
        String[] flags = {"build", "place", "break", "interact", "use", "use-items",
                "entry", "pvp", "enderchest", "chest-access", "time-lock", "exit",
                "mob-damage", "block-chat", "blockcmd", "enderman-grief", "item-drop",
                "item-pickup", "pistons", "sleep", "explosions", "ender-dragon-damage",
                "block-chat"};

        for (String flag : flags) {
            region.setFlag(flag, "allow");
        }
    }

    private String extractWorldNameFromGlobalRegion(String regionName) {
        return regionName.equals("__Global__") ? "world"
                : regionName.substring("__Global__".length());
    }

    private boolean isGlobalRegionName(String regionName) {
        return regionName != null && regionName.startsWith("__Global__");
    }

    public String getGlobalRegionNameForWorld(String worldName) {
        return worldName.equals("world") ? "__Global__" : "__Global__" + worldName;
    }

    public void reloadGlobalRegions() {
        loadGlobalRegionsFromFiles();
        ensureGlobalRegionsExist();
        applyGlobalTimeLocks();
    }

    public TimeLockManager getTimeLockManager() {
        return timeLockManager;
    }
}