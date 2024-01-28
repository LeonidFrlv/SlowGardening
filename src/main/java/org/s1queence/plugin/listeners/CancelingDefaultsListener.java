package org.s1queence.plugin.listeners;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.MoistureChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.s1queence.plugin.SlowGardening;
import org.s1queence.plugin.libs.YamlDocument;

import java.util.List;

import static org.s1queence.S1queenceLib.getLib;
import static org.s1queence.api.S1Booleans.isAllowableInteraction;
import static org.s1queence.api.S1TextUtils.getConvertedTextFromConfig;
import static org.s1queence.api.S1Utils.sendActionBarMsg;
import static org.s1queence.plugin.classes.GardenProcess.gardenHandlers;
import static org.s1queence.plugin.listeners.PlowListener.setMoistureLevel;

public class CancelingDefaultsListener implements Listener {
    private final SlowGardening plugin;
    public CancelingDefaultsListener(final SlowGardening plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    private void onStructureGrow(StructureGrowEvent e) {
        Player player = e.getPlayer();
        YamlDocument listsCfg = plugin.getListsConfig();
        YamlDocument cfg = plugin.getPluginConfig();
        Object disabled_growing_structures = listsCfg.get("disabled_growing_structures");
        if (disabled_growing_structures == null) return;
        if (player != null && player.hasPermission(cfg.getString("tree_grower_permission"))) return;
        String treeType = e.getSpecies().toString();
        if (disabled_growing_structures instanceof String && disabled_growing_structures.equals("all")) e.setCancelled(true);
        if (!(disabled_growing_structures instanceof List)) return;
        if (((List<?>)disabled_growing_structures).contains(treeType)) e.setCancelled(true);
    }

    @EventHandler
    private void onMoistureChange(MoistureChangeEvent e) {
        if (e.getBlock().getType().equals(Material.FARMLAND) && plugin.getPluginConfig().getBoolean("farmland_moisture_is_always_max")) e.setCancelled(true);
    }

    @EventHandler
    private void onPlayerBlockBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        Player player = e.getPlayer();
        plugin.getPlowingBlocks().remove(block);
        gardenHandlers.remove(block);
        if (!player.getGameMode().equals(GameMode.SURVIVAL)) return;


        if (!plugin.getPlants().contains(block.getType().toString())) return;

        String errorText = isAllowableInteraction(player, block.getLocation(), getLib());
        if (errorText != null) {
            sendActionBarMsg(player, errorText);
            e.setCancelled(true);
            return;
        }

        e.setCancelled(true);
        sendActionBarMsg(player, getConvertedTextFromConfig(plugin.getTextConfig(), "default_harvesting", plugin.getName()));
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    private void onPlayerInteractEvent(PlayerInteractEvent e) {
        if (e.getHand() == null) return;
        if (!e.getHand().equals(EquipmentSlot.HAND)) return;
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
        Block block = e.getClickedBlock();
        if (block == null) return;
        Player player = e.getPlayer();
        GameMode gm = player.getGameMode();

        String mainItemType = player.getInventory().getItemInMainHand().getType().toString();
        String offItemType = player.getInventory().getItemInOffHand().getType().toString();
        List<String> hoesLikeTools = plugin.getHoesLikeTools();
        boolean isMainItemTool = hoesLikeTools.contains(mainItemType);
        boolean isOffItemTool = hoesLikeTools.contains(offItemType);
        if (!plugin.isDirtBlock(block)) return;
        if (!isOffItemTool && !isMainItemTool) return;

        if (!gm.equals(GameMode.SURVIVAL)) {
            if (gm.equals(GameMode.CREATIVE)) {
                e.setCancelled(true);
                block.setType(Material.FARMLAND);
                setMoistureLevel(block, plugin);
            }
            return;
        }

        String errorText = isAllowableInteraction(player, block.getLocation(), getLib());
        if (errorText != null) {
            sendActionBarMsg(player, errorText);
            e.setCancelled(true);
            return;
        }

        if (gardenHandlers.containsKey(block) || gardenHandlers.containsValue(player)) {
            e.setCancelled(true);
            return;
        }

        sendActionBarMsg(player, getConvertedTextFromConfig(plugin.getTextConfig(), "default_farmland_making", plugin.getName()));
        e.setCancelled(true);
    }
}
