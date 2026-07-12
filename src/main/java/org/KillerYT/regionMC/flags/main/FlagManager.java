package org.KillerYT.regionMC.flags.main;

// флаги
import org.KillerYT.regionMC.flags.chat.*;
import org.KillerYT.regionMC.flags.environment.*;
import org.KillerYT.regionMC.flags.items.*;
import org.KillerYT.regionMC.flags.mechanics.*;
import org.KillerYT.regionMC.flags.mobs.*;
import org.KillerYT.regionMC.flags.players.*;
import org.KillerYT.regionMC.flags.protection.*;
import org.KillerYT.regionMC.flags.movement.*;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.managers.AdminManager;
import org.KillerYT.regionMC.managers.PlayerMoveListener;
import org.KillerYT.regionMC.utils.DebugUtils;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class FlagManager {


    private static FlagManager instance;
    private final RegionMC plugin;
    private final Map<String, AbstractRegionFlag<?>> flags = new ConcurrentHashMap<>();
    private final Map<String, List<AbstractRegionFlag<?>>> flagsByCategory = new LinkedHashMap<>();
    private final Map<String, Set<String>> groupFlags = new ConcurrentHashMap<>();
    private final Map<String, Integer> regionPriorities = new ConcurrentHashMap<>();
    private final DebugUtils debug = DebugUtils.getInstance();
    private final AdminManager adminManager;

    // Приватный конструктор, теперь принимает плагин
    private FlagManager(RegionMC plugin) {
        this.plugin = plugin;
        this.adminManager = plugin.getAdminManager();
        initCategories();
    }

    // Инициализация менеджера с плагином (вызывается один раз при старте)
    public static void initialize(RegionMC plugin) {
        if (instance == null) {
            instance = new FlagManager(plugin);
            instance.registerAllFlags(); // сразу регистрируем все флаги-слушатели
        }
    }

    public static FlagManager getInstance() {
        if (instance == null) throw new IllegalStateException("FlagManager не инициализирован! Вызовите initialize(plugin) первым.");
        return instance;
    }

    // Создаёт и регистрирует все флаги как слушатели событий
    private void registerAllFlags() {
        debug.debug(DebugUtils.DebugCategory.FLAGS, "Начало регистрации флагов");
        // PROTECTION
        addFlag(new BuildFlag(plugin));
        addFlag(new BreakFlag(plugin));
        addFlag(new PlaceFlag(plugin));
        addFlag(new ChestAccessFlag(plugin));
        addFlag(new EnderChestFlag(plugin));
        addFlag(new ExplosionsFlag(plugin));
        addFlag(new RespawnAnchorsFlag(plugin));

        // MECHANICS
        addFlag(new InteractFlag(plugin));
        addFlag(new UseFlag(plugin));
        addFlag(new PistonsFlag(plugin));
        addFlag(new RideFlag(plugin));

        // ITEMS
        addFlag(new UseItemsFlag(plugin));
        addFlag(new ItemPickupFlag(plugin));
        addFlag(new ItemDropFlag(plugin));
        addFlag(new TntFlag(plugin));

        // PLAYERS
        addFlag(new PvpFlag(plugin));
        addFlag(new FallDamageFlag(plugin));
        addFlag(new ExpDropsFlag(plugin));
        addFlag(new SleepFlag(plugin));
        addFlag(new BlockCmdFlag(plugin));
        addFlag(new GreetingFlag(plugin));
        addFlag(new FarewellFlag(plugin));
        addFlag(new EntryDenyMessageFlag(plugin));
        addFlag(new ExitDenyMessageFlag(plugin));

        // MOBS
        addFlag(new MobSpawningFlag(plugin));
        addFlag(new MobDamageFlag(plugin));
        addFlag(new CreeperExplosionFlag(plugin));
        addFlag(new EnderDragonDamageFlag(plugin));
        addFlag(new EndermanGriefFlag(plugin));
        addFlag(new DamageAnimalsFlag(plugin));

        // MOVEMENT
        addFlag(new EntryFlag(plugin));
        addFlag(new ExitFlag(plugin));
        addFlag(new EnterNotifyFlag(plugin));
        addFlag(new ExitNotifyFlag(plugin));

        // ENVIRONMENT
        addFlag(new FireSpreadFlag(plugin));
        addFlag(new GrassGrowthFlag(plugin));
        addFlag(new LeafDecayFlag(plugin));
        addFlag(new TimeLockFlag(plugin));

        // CHAT
        addFlag(new BlockChatFlag(plugin));

        // PlayerMoveListener
        PlayerMoveListener moveListener = new PlayerMoveListener(plugin);
        plugin.getServer().getPluginManager().registerEvents(moveListener, plugin);

        plugin.getLogger().info("✓ Все флаги зарегистрированы через FlagManager");
    }

    private void addFlag(ListenerFlag<?> flag) {
        if (flag == null) return;
        plugin.getServer().getPluginManager().registerEvents(flag, plugin);
        registerFlag(flag);
    }

    public void registerFlag(AbstractRegionFlag<?> flag) {
        if (flag == null) return;
        String flagName = flag.getName();
        if (flagName == null || flagName.isEmpty()) return;
        String upperName = flagName.toUpperCase();
        flags.put(upperName, flag);
        String category = getCategoryForFlag(flag);
        flagsByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(flag);
        flagsByCategory.get(category).sort(Comparator.comparing(AbstractRegionFlag::getName));
        plugin.getLogger().info("Registered flag: " + flagName);
    }

    // ==================== КАТЕГОРИИ ФЛАГОВ (WorldGuard Style) ====================

    private void initCategories() {
        flagsByCategory.put("protection", new ArrayList<>());
        flagsByCategory.put("mobs-fire-explosions", new ArrayList<>());
        flagsByCategory.put("natural-events", new ArrayList<>());
        flagsByCategory.put("movement", new ArrayList<>());
        flagsByCategory.put("map-making", new ArrayList<>());
        flagsByCategory.put("miscellaneous", new ArrayList<>());
        flagsByCategory.put("players", new ArrayList<>());
        flagsByCategory.put("chat", new ArrayList<>());
    }

    private String getCategoryForFlag(AbstractRegionFlag<?> flag) {
        String className = flag.getClass().getSimpleName().toLowerCase();
        String flagName = flag.getName().toLowerCase();

        if (className.contains("build") || className.contains("break") ||
                className.contains("chest") || className.contains("explosion") ||
                flagName.equals("build") || flagName.equals("break") ||
                flagName.equals("chest-access") || flagName.equals("enderchest")) {
            return "protection";
        }
        if (className.contains("mob") || className.contains("creeper") ||
                className.contains("ender") || flagName.contains("mob") ||
                flagName.contains("explosion") || flagName.contains("fire")) {
            return "mobs-fire-explosions";
        }
        if (className.contains("grass") || className.contains("leaf") ||
                className.contains("time") || flagName.contains("growth") ||
                flagName.contains("decay") || flagName.contains("spread")) {
            return "natural-events";
        }
        if (className.contains("entry") || className.contains("exit") ||
                flagName.equals("entry") || flagName.equals("exit")) {
            return "movement";
        }
        if (className.contains("item") || className.contains("exp") ||
                className.contains("fall") || flagName.contains("drop") ||
                flagName.contains("pickup") || flagName.contains("damage") ||
                flagName.contains("cmd")) {
            return "map-making";
        }
        if (className.contains("piston") || className.contains("interact") ||
                className.contains("use") || flagName.contains("piston") ||
                flagName.contains("interact") || flagName.contains("use")) {
            return "miscellaneous";
        }
        if (className.contains("pvp") || className.contains("sleep") ||
                flagName.equals("pvp") || flagName.equals("sleep")) {
            return "players";
        }
        if (className.contains("chat") || flagName.contains("chat")) {
            return "chat";
        }
        return "miscellaneous";
    }

    public Map<String, List<AbstractRegionFlag<?>>> getFlagsByCategory() {
        return Collections.unmodifiableMap(flagsByCategory);
    }

    public List<String> getCategoryNames() {
        return new ArrayList<>(flagsByCategory.keySet());
    }

    public List<AbstractRegionFlag<?>> getFlagsInCategory(String category) {
        return flagsByCategory.getOrDefault(category, Collections.emptyList());
    }

    // ==================== ГРУППОВЫЕ ФЛАГИ (-g owners, -g members) ====================

    public void registerGroupFlag(String flagName, String group) {
        String key = flagName.toUpperCase() + ":" + group.toLowerCase();
        groupFlags.computeIfAbsent(key, k -> new HashSet<>()).add(group);
    }

    public boolean isGroupFlag(String flagName) {
        return flagName != null && flagName.startsWith("-g ");
    }

    public String[] parseGroupFlag(String input) {
        if (input == null || !input.startsWith("-g ")) return null;
        String[] parts = input.substring(3).trim().split("\\s+", 2);
        if (parts.length == 2) return new String[]{parts[0], parts[1]};
        return null;
    }

    public List<String> getAvailableGroups() {
        return Arrays.asList("owners", "members", "nonmembers", "nonowners");
    }

    // ==================== ПРИОРИТЕТЫ РЕГИОНОВ ====================

    public void setRegionPriority(String regionName, int priority) {
        regionPriorities.put(regionName.toLowerCase(), priority);
    }

    public int getRegionPriority(String regionName) {
        return regionPriorities.getOrDefault(regionName.toLowerCase(), 0);
    }

    public void removeRegionPriority(String regionName) {
        regionPriorities.remove(regionName.toLowerCase());
    }

    public List<String> sortRegionsByPriority(Collection<String> regions) {
        return regions.stream()
                .sorted((r1, r2) -> Integer.compare(getRegionPriority(r2), getRegionPriority(r1)))
                .collect(Collectors.toList());
    }

    // ==================== ПОЛУЧЕНИЕ ФЛАГОВ ====================

    @SuppressWarnings("unchecked")
    public <T> AbstractRegionFlag<T> getFlag(String name) {
        if (name == null) return null;
        if (name.startsWith("-g ")) {
            String[] parsed = parseGroupFlag(name);
            if (parsed != null) name = parsed[1];
        }
        return (AbstractRegionFlag<T>) flags.get(name.toUpperCase());
    }

    public List<String> getAllFlagNames() {
        List<String> allNames = new ArrayList<>();
        for (AbstractRegionFlag<?> flag : flags.values()) {
            allNames.add(flag.getName());
        }
        Collections.sort(allNames);
        return allNames;
    }

    public boolean flagExists(String flagName) {
        if (flagName == null) return false;
        if (flagName.startsWith("-g ")) {
            String[] parsed = parseGroupFlag(flagName);
            if (parsed != null) flagName = parsed[1];
        }
        return flags.containsKey(flagName.toUpperCase());
    }

    // ==================== ОПИСАНИЯ И ВАЛИДАЦИЯ ====================

    public String getFlagDescription(String flagName) {
        AbstractRegionFlag<?> flag = getFlag(flagName);
        if (flag == null) return "Неизвестный флаг: " + flagName;
        try {
            java.lang.reflect.Method method = flag.getClass().getMethod("getDescription");
            if (method.getReturnType().equals(String.class)) {
                String desc = (String) method.invoke(flag);
                if (desc != null && !desc.isEmpty()) return desc;
            }
        } catch (Exception ignored) {}
        return String.format("Флаг: %s (по умолчанию: %s)", flag.getName(), flag.getDefaultValue());
    }

    public List<String> getFlagSuggestions(String flagName) {
        AbstractRegionFlag<?> flag = getFlag(flagName);
        if (flag == null) return Collections.emptyList();
        if (flag.getDefaultValue() instanceof Boolean)
            return Arrays.asList("allow", "deny", "true", "false", "yes", "no");
        if (flagName.equalsIgnoreCase("pvp")) return Arrays.asList("allow", "deny");
        if (flagName.equalsIgnoreCase("time-lock")) return Arrays.asList("allow", "deny", "day", "night");
        return Arrays.asList("allow", "deny");
    }

    public boolean isValidFlagValue(String flagName, String value) {
        AbstractRegionFlag<?> flag = getFlag(flagName);
        if (flag == null) return false;
        try {
            return flag.parseInput(value) != null;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== ПРОВЕРКА РАЗРЕШЕНИЙ ====================

    public boolean isAllowed(String flagName, Object value) {
        AbstractRegionFlag<Object> flag = getFlag(flagName);
        if (flag == null) return true;
        return flag.isAllowed(value);
    }

    public boolean hasFlagPermission(Player player, String flagName) {
        if (player == null || flagName == null) return false;
        if (adminManager != null && adminManager.isAdmin(player)) return true;
        if (player.hasPermission("regionmc.flags.*") || player.isOp()) return true;
        return player.hasPermission("regionmc.flags." + flagName.toLowerCase());
    }

    public List<AbstractRegionFlag<?>> getFlagsWithPermission(Player player) {
        List<AbstractRegionFlag<?>> allowed = new ArrayList<>();
        if (player == null) return allowed;
        for (AbstractRegionFlag<?> flag : flags.values())
            if (hasFlagPermission(player, flag.getName())) allowed.add(flag);
        allowed.sort(Comparator.comparing(AbstractRegionFlag::getName));
        return allowed;
    }

    public List<String> getFlagNamesWithPermission(Player player) {
        List<String> names = new ArrayList<>();
        if (player == null) return names;
        for (AbstractRegionFlag<?> flag : flags.values())
            if (hasFlagPermission(player, flag.getName())) names.add(flag.getName());
        Collections.sort(names);
        return names;
    }

    @Deprecated
    public List<String> getAvailableFlags() { return getAllFlagNames(); }

    @Deprecated
    public boolean isValidFlag(String flagName) { return flagExists(flagName); }
}