package org.s1queence.plugin.listeners;

import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.s1queence.plugin.SlowGardening;
import org.s1queence.plugin.classes.GardenActionType;
import org.s1queence.plugin.classes.GardenProcess;
import org.s1queence.plugin.libs.YamlDocument;

import static org.s1queence.api.S1Booleans.isNotAllowableInteraction;
import static org.s1queence.api.S1TextUtils.getConvertedTextFromConfig;
import static org.s1queence.plugin.classes.GardenProcess.gardenHandlers;

public class PlantListener implements Listener {
    private final SlowGardening plugin;
    public PlantListener(final SlowGardening plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    private void onPlayerInteractEvent(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        Block block = e.getBlock();
        if (!player.getGameMode().equals(GameMode.SURVIVAL)) return;
        if (!plugin.getPlants().contains(block.getType().toString())) return;
        if (!e.getHand().equals(EquipmentSlot.HAND)) {
            e.setCancelled(true);
            return;
        }

        for (Block key : gardenHandlers.keySet()) {
            if (key.getLocation() != block.getLocation()) continue;
            e.setCancelled(true);
            return;
        }

        if (isNotAllowableInteraction(player, block.getLocation())) {
            e.setCancelled(true);
            return;
        }

        if (gardenHandlers.containsKey(block) || gardenHandlers.containsValue(player)) {
            e.setCancelled(true);
            return;
        }

        YamlDocument cfg = plugin.getPluginConfig();
        boolean hasPermission = player.hasPermission(cfg.getString("farmer_permission"));
        int seconds = hasPermission ? cfg.getInt("farmer_plant_time") : cfg.getInt("base_plant_time");
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
                GardenActionType.PLANTING,
                plugin,
                getConvertedTextFromConfig(textConfig,"plant_process.every_tick.action_bar", pName),
                getConvertedTextFromConfig(textConfig,"plant_process.every_tick.title", pName),
                getConvertedTextFromConfig(textConfig,"plant_process.every_tick.subtitle", pName),
                null,
                null,
                getConvertedTextFromConfig(textConfig,"plant_process.complete.action_bar", pName),
                getConvertedTextFromConfig(textConfig,"plant_process.complete.title", pName),
                getConvertedTextFromConfig(textConfig,"plant_process.complete.subtitle", pName),
                null,
                null,
                getConvertedTextFromConfig(textConfig,"plant_process.cancel.action_bar", pName),
                getConvertedTextFromConfig(textConfig,"plant_process.cancel.title", pName),
                getConvertedTextFromConfig(textConfig,"plant_process.cancel.subtitle", pName),
                null,
                null
        );
    }

}
