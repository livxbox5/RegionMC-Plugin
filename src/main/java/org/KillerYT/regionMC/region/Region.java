package org.KillerYT.regionMC.region;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.ConfigurationSection;
//import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Getter
@SuppressWarnings("unused")
public class Region {
    private final String name;
    private final UUID owner;
    private final Set<UUID> owners;
    private final Set<UUID> members;
    private Location pos1;
    private Location pos2;
    private final Map<String, Object> flags;
    @Setter
    private int priority;

    private static final Logger LOGGER = Logger.getLogger("RegionMC");

    public Region(String name, UUID owner, Location pos1, Location pos2) {
        this.name = name;
        this.owner = owner;
        this.owners = new HashSet<>();
        this.owners.add(owner);
        this.members = new HashSet<>();
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.flags = new HashMap<>();
        this.priority = 0;
    }

    public Region(Map<String, Object> data) {
        this.name = (String) data.get("name");

        // Обработка владельца
        Object ownerObj = data.get("owner");
        UUID tempOwner;
        if (ownerObj instanceof String ownerStr) {
            try {
                tempOwner = UUID.fromString(ownerStr);
            } catch (IllegalArgumentException e) {
                tempOwner = new UUID(0, 0);
                LOGGER.warning("Invalid owner UUID in region " + name + ": " + ownerObj);
            }
        } else {
            tempOwner = new UUID(0, 0);
        }
        this.owner = tempOwner;

        // Загрузка владельцев
        Set<UUID> tempOwners = new HashSet<>();
        if (data.containsKey("owners")) {
            try {
                List<String> ownerStrings = safeCastToList(data.get("owners"));
                for (String ownerStr : ownerStrings) {
                    try {
                        tempOwners.add(UUID.fromString(ownerStr));
                    } catch (IllegalArgumentException e) {
                        LOGGER.warning("Invalid owner UUID in region " + name + ": " + ownerStr);
                    }
                }
            } catch (IllegalArgumentException e) {
                LOGGER.warning("Error parsing owners for region " + name + ": " + e.getMessage());
            }
        }
        if (tempOwners.isEmpty()) {
            tempOwners.add(this.owner);
        }
        this.owners = tempOwners;

        // Загрузка участников
        Set<UUID> tempMembers = new HashSet<>();
        if (data.containsKey("members")) {
            try {
                List<String> memberStrings = safeCastToList(data.get("members"));
                for (String memberStr : memberStrings) {
                    try {
                        tempMembers.add(UUID.fromString(memberStr));
                    } catch (IllegalArgumentException e) {
                        LOGGER.warning("Invalid member UUID in region " + name + ": " + memberStr);
                    }
                }
            } catch (IllegalArgumentException e) {
                LOGGER.warning("Error parsing members for region " + name + ": " + e.getMessage());
            }
        }
        this.members = tempMembers;

        // Загрузка позиций
        this.pos1 = loadLocationFromData(data, "pos1");
        this.pos2 = loadLocationFromData(data, "pos2");

        if (this.pos1 == null || this.pos2 == null) {
            LOGGER.warning("Region " + name + " has invalid positions: pos1=" +
                    (this.pos1 != null ? "valid" : "null") + ", pos2=" +
                    (this.pos2 != null ? "valid" : "null"));
        }

        // Загрузка флагов
        Map<String, Object> tempFlags;
        try {
            tempFlags = safeCastToMap(data.getOrDefault("flags", new HashMap<>()));
        } catch (IllegalArgumentException e) {
            tempFlags = new HashMap<>();
            LOGGER.warning("Error parsing flags for region " + name + ": " + e.getMessage());
        }
        this.flags = tempFlags;

        // Загрузка приоритета
        Object priorityObj = data.get("priority");
        this.priority = priorityObj instanceof Number number ? number.intValue() : 0;
    }

    private Location loadLocationFromData(Map<String, Object> data, String posKey) {
        try {
            if (!data.containsKey(posKey)) {
                LOGGER.warning("Missing " + posKey + " in region data: " + name);
                return null;
            }

            Map<String, Object> posMap = convertToMap(data.get(posKey));
            if (posMap == null || posMap.isEmpty()) {
                LOGGER.warning("Invalid or empty " + posKey + " data in region " + name);
                return null;
            }

            Object worldObj = posMap.get("world");
            if (!(worldObj instanceof String worldName)) {
                LOGGER.warning("Invalid world type for " + posKey + " in region " + name + ": " +
                        (worldObj != null ? worldObj.getClass().getSimpleName() : "null"));
                return null;
            }

            World world = findWorld(worldName);
            if (world == null) {
                LOGGER.warning("World not found for " + posKey + " in region " + name + ": " + worldName);
                return null;
            }

            double x = safeGetDouble(posMap, "x");
            double y = safeGetDouble(posMap, "y");
            double z = safeGetDouble(posMap, "z");

            Location location = new Location(world, x, y, z);
            LOGGER.log(Level.INFO, "Successfully loaded {0} for region {1}: {2}",
                    new Object[]{posKey, name, formatLocation(location)});
            return location;

        } catch (Exception e) {
            LOGGER.warning("Error loading " + posKey + " for region " + name + ": " + e.getMessage());
            return new Location(Bukkit.getWorlds().getFirst(), 0, 0, 0);
        }
    }

    private World findWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) return world;

        for (World w : Bukkit.getWorlds()) {
            if (w.getName().equalsIgnoreCase(worldName)) {
                return w;
            }
        }

        try {
            world = Bukkit.createWorld(new WorldCreator(worldName));
            if (world != null) {
                LOGGER.info("Successfully loaded world: " + worldName);
                return world;
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToMap(Object data) {
        return switch (data) {
            case Map<?, ?> map -> (Map<String, Object>) map;
            case ConfigurationSection section -> {
                Map<String, Object> result = new HashMap<>();
                for (String key : section.getKeys(false)) {
                    result.put(key, section.get(key));
                }
                yield result;
            }
            case null, default -> null;
        };
    }

    private double safeGetDouble(Map<String, Object> map, String key) {
        try {
            Object value = map.get(key);
            return switch (value) {
                case Number number -> number.doubleValue();
                case String s -> {
                    try {
                        yield Double.parseDouble(s);
                    } catch (NumberFormatException e) {
                        LOGGER.warning("Invalid double value for key " + key + ": " + value);
                        yield 0.0;
                    }
                }
                case null -> 0.0;
                default -> {
                    try {
                        yield Double.parseDouble(value.toString());
                    } catch (NumberFormatException e) {
                        LOGGER.warning("Cannot convert to double for key " + key + ": " + value);
                        yield 0.0;
                    }
                }
            };
        } catch (Exception e) {
            LOGGER.warning("Error getting double for key " + key + ": " + e.getMessage());
            return 0.0;
        }
    }

    private List<String> safeCastToList(Object obj) {
        if (obj instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            return result;
        }
        throw new IllegalArgumentException("Expected List but got: " + (obj != null ? obj.getClass().getSimpleName() : "null"));
    }

    private Map<String, Object> safeCastToMap(Object obj) {
        return switch (obj) {
            case Map<?, ?> map -> {
                Map<String, Object> result = new HashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() instanceof String key) {
                        result.put(key, entry.getValue());
                    }
                }
                yield result;
            }
            case ConfigurationSection section -> {
                Map<String, Object> result = new HashMap<>();
                for (String key : section.getKeys(false)) {
                    result.put(key, section.get(key));
                }
                yield result;
            }
            case null -> new HashMap<>();
            default -> throw new IllegalArgumentException("Expected Map or ConfigurationSection but got: " + obj.getClass().getSimpleName());
        };
    }

    // Геттеры (кроме сгенерированных Lombok)
    public Set<UUID> getOwners() { return new HashSet<>(owners); }
    public Set<UUID> getMembers() { return new HashSet<>(members); }
    public Map<String, Object> getFlags() { return new HashMap<>(flags); }
    public World getWorld() {
        return pos1 != null && pos1.getWorld() != null ? pos1.getWorld() : null;
    }

    // Сеттеры
    public void setPos1(Location pos1) {
        if (pos1 != null && pos1.getWorld() == null) {
            throw new IllegalArgumentException("Location world cannot be null");
        }
        this.pos1 = pos1;
    }

    public void setPos2(Location pos2) {
        if (pos2 != null && pos2.getWorld() == null) {
            throw new IllegalArgumentException("Location world cannot be null");
        }
        this.pos2 = pos2;
    }

    // Флаги
    public void setFlag(String flag, Object value) {
        if (flag == null || flag.trim().isEmpty()) {
            throw new IllegalArgumentException("Flag name cannot be null or empty");
        }
        String flagName = flag.trim();
        if (value == null) {
            flags.remove(flagName);
        } else {
            flags.put(flagName, value);
        }
    }

    public Object getFlag(String flagName) {
        return (flagName == null || flagName.trim().isEmpty()) ? null : flags.get(flagName.trim());
    }

    public <T> T getFlagValue(String flagName, Class<T> type) {
        if (flagName == null || flagName.trim().isEmpty() || type == null) return null;
        Object value = flags.get(flagName.trim());
        return type.isInstance(value) ? type.cast(value) : null;
    }

    public Boolean getFlagBoolean(String flagName) {
        Object flagValue = getFlag(flagName);
        if (flagValue == null) return null;

        return switch (flagValue) {
            case Boolean b -> b;
            case String s -> {
                String lower = s.toLowerCase().trim();
                yield switch (lower) {
                    case "true", "allow", "yes", "1", "разрешить", "enable", "enabled", "on" -> true;
                    case "false", "deny", "no", "0", "запретить", "disable", "disabled", "off" -> false;
                    default -> {
                        try {
                            yield Boolean.parseBoolean(lower);
                        } catch (Exception e) {
                            LOGGER.warning("Cannot parse flag '" + flagName + "' value as boolean: " + flagValue);
                            yield null;
                        }
                    }
                };
            }
            case Number n -> n.intValue() != 0;
            default -> {
                try {
                    yield Boolean.parseBoolean(flagValue.toString());
                } catch (Exception e) {
                    LOGGER.warning("Cannot convert flag '" + flagName + "' value to boolean: " + flagValue);
                    yield null;
                }
            }
        };
    }

    // Права
    public boolean isOwner(UUID playerId) { return owners.contains(playerId); }
    public boolean isMember(UUID playerId) { return members.contains(playerId) || isOwner(playerId); }

    public boolean addOwner(UUID playerId) { return owners.add(playerId); }
    public boolean addMember(UUID playerId) { return members.add(playerId); }

    public boolean removeOwner(UUID playerId) {
        return !playerId.equals(owner) && owners.remove(playerId);
    }

    public boolean removeMember(UUID playerId) { return members.remove(playerId); }

    // Объем и проверки
    public int getVolume() {
        if (pos1 == null || pos2 == null) return 0;
        return calculateBounds().volume();
    }

    public boolean contains(Location location) {
        if (location == null || pos1 == null || pos2 == null) return false;
        World regionWorld = getWorld();
        return regionWorld != null && regionWorld.equals(location.getWorld()) && isLocationInBounds(location);
    }

    private boolean isLocationInBounds(Location location) {
        Bounds bounds = calculateBounds();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return x >= bounds.minX() && x <= bounds.maxX() &&
                y >= bounds.minY() && y <= bounds.maxY() &&
                z >= bounds.minZ() && z <= bounds.maxZ();
    }

    private Bounds calculateBounds() {
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        return new Bounds(minX, maxX, minY, maxY, minZ, maxZ);
    }

    private record Bounds(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        int volume() {
            return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        }
    }

    // Сериализация
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("owner", owner.toString());
        data.put("owners", owners.stream().map(UUID::toString).collect(Collectors.toList()));
        data.put("members", members.stream().map(UUID::toString).collect(Collectors.toList()));
        data.put("priority", priority);
        data.put("flags", flags);
        serializeLocation(data, "pos1", pos1);
        serializeLocation(data, "pos2", pos2);
        return data;
    }

    private void serializeLocation(Map<String, Object> data, String key, Location location) {
        Map<String, Object> locData = new HashMap<>();
        if (location != null && location.getWorld() != null) {
            locData.put("world", location.getWorld().getName());
            locData.put("x", location.getX());
            locData.put("y", location.getY());
            locData.put("z", location.getZ());
        } else {
            locData.put("world", "world");
            locData.put("x", 0.0);
            locData.put("y", 0.0);
            locData.put("z", 0.0);
        }
        data.put(key, locData);
    }

    // Строковые представления
    public String getOwnersString() {
        return owners.stream().map(UUID::toString).collect(Collectors.joining(", "));
    }

    public String getMembersString() {
        return members.stream().map(UUID::toString).collect(Collectors.joining(", "));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Region region = (Region) o;
        return Objects.equals(name, region.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Region{name='" + name + "', owners=" + owners.size() + ", members=" + members.size() +
                ", world=" + (getWorld() != null ? getWorld().getName() : "null") +
                ", pos1=" + formatLocation(pos1) + ", pos2=" + formatLocation(pos2) + "}";
    }

    private String formatLocation(Location location) {
        return location != null ? String.format("X: %.1f, Y: %.1f, Z: %.1f",
                location.getX(), location.getY(), location.getZ()) : "null";
    }
}