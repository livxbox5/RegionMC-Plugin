package org.KillerYT.regionMC.flags.players;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;
import org.KillerYT.regionMC.region.Region;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;

public class BlockCmdFlag extends ListenerFlag<List<String>> {

    public BlockCmdFlag(RegionMC plugin) {
        super(plugin, "blockcmd", null);
    }

    @Override
    public List<String> parseInput(String input) {
        // логика как раньше
        if (input == null || input.trim().isEmpty()) return getDefaultValue();
        String lower = input.toLowerCase().trim();
        return switch (lower) {
            case "true", "allow", "yes", "1", "разрешить" -> null;      // разрешить все
            case "false", "deny", "no", "0", "запретить" -> List.of();  // запретить все
            default -> List.of(input.split("[,\\s]+"));
        };
    }

    @Override
    public boolean isAllowed(List<String> value) { return value == null; }

    @Override
    public String getDescription() { return "Блокировка команд в регионе"; }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player p = event.getPlayer();
        String cmd = event.getMessage().toLowerCase().trim();
        if (cmd.startsWith("/regionmc") || cmd.startsWith("/rg") || cmd.startsWith("/region")) return;
        String regionName = regionManager.findRegionAtLocation(p.getLocation());
        if (regionName == null || shouldBypassRegionProtection(p, regionName)) return;
        Region region = regionManager.getRegion(regionName);
        if (region == null) return;
        Object flagValue = region.getFlag("blockcmd");
        if (flagValue == null) return;
        List<String> blocked = null;
        if (flagValue instanceof List<?> l) blocked = (List<String>) l;
        else if (flagValue instanceof Boolean b) blocked = b ? null : List.of();
        else if (flagValue instanceof String s) blocked = parseInput(s);
        if (blocked != null && isCommandBlocked(cmd, blocked)) {
            event.setCancelled(true);
            sendMessage(p, "protection.command", "§cВы не можете использовать команды здесь!");
        }
    }

    private boolean isCommandBlocked(String cmd, List<String> blocked) {
        if (blocked == null) return false;
        if (blocked.isEmpty()) return true;
        String n = normalizeCommand(cmd);
        return blocked.stream().anyMatch(b -> n.equals(b) || n.startsWith(b + " "));
    }

    private String normalizeCommand(String cmd) {
        String n = cmd.toLowerCase().trim();
        return n.startsWith("/") ? n.substring(1) : n;
    }
}