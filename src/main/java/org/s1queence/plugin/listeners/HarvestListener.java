package org.s1queence.plugin.listeners;

import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.s1queence.plugin.SlowGardening;
import org.s1queence.plugin.classes.GardenActionType;
import org.s1queence.plugin.classes.GardenProcess;
import org.s1queence.plugin.libs.YamlDocument;

import java.util.List;

import static org.s1queence.api.S1Booleans.isNotAllowableInteraction;
import static org.s1queence.api.S1TextUtils.getConvertedTextFromConfig;
import static org.s1queence.plugin.classes.GardenProcess.gardenHandlers;

public class HarvestListener implements Listener {
    private final SlowGardening plugin;
    public HarvestListener(final SlowGardening plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    private void onPlayerInteractEvent(PlayerInteractEvent e) {
        if (e.getHand() == null) return;
        if (!e.getHand().equals(EquipmentSlot.HAND)) return;
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
        Block block = e.getClickedBlock();
        if (block == null) return;
        Player player = e.getPlayer();
        if (!player.getGameMode().equals(GameMode.SURVIVAL)) return;

        List<String> plants = plugin.getPlants();
        if (!plants.contains(block.getType().toString())) return;

        if (isNotAllowableInteraction(player, block.getLocation())) {
            e.setCancelled(true);
            return;
        }

        if (gardenHandlers.containsKey(block) || gardenHandlers.containsValue(player)) {
            e.setCancelled(true);
            return;
        }

        YamlDocument cfg = plugin.getPluginConfig();
        String mainItemType = player.getInventory().getItemInMainHand().getType().toString();
        boolean hasHarvestingTool = plugin.getHarvestingTools().contains(mainItemType);
        boolean hasPermission = player.hasPermission(cfg.getString("farmer_permission"));

        int seconds = hasPermission ? cfg.getInt("farmer_harvest_time") : cfg.getInt("base_harvest_time");
        int reducer = cfg.getInt("tool_harvest_time_reducer");
        if (reducer > 0 && hasHarvestingTool) seconds /= reducer;

        YamlDocument textConfig = plugin.getTextConfig();
        String pName = plugin.getName();
        e.setCancelled(true);

        new GardenProcess(
                player,
                player,
                seconds,
                false,
                true,
                plugin.getProgressBar(),
                block,
                GardenActionType.HARVESTING,
                plugin,
                getConvertedTextFromConfig(textConfig,"harvest_process.every_tick.action_bar", pName),
                getConvertedTextFromConfig(textConfig,"harvest_process.every_tick.title", pName),
                getConvertedTextFromConfig(textConfig,"harvest_process.every_tick.subtitle", pName),
                null,
                null,
                getConvertedTextFromConfig(textConfig,"harvest_process.complete.action_bar", pName),
                getConvertedTextFromConfig(textConfig,"harvest_process.complete.title", pName),
                getConvertedTextFromConfig(textConfig,"harvest_process.complete.subtitle", pName),
                null,
                null,
                getConvertedTextFromConfig(textConfig,"harvest_process.cancel.action_bar", pName),
                getConvertedTextFromConfig(textConfig,"harvest_process.cancel.title", pName),
                getConvertedTextFromConfig(textConfig,"harvest_process.cancel.subtitle", pName),
                null,
                null
        );
    }
}
