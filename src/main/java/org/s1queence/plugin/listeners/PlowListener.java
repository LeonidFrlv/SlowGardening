package org.s1queence.plugin.listeners;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.s1queence.plugin.SlowGardening;
import org.s1queence.plugin.libs.YamlDocument;

import java.util.List;
import java.util.Map;

import static org.s1queence.api.S1Booleans.isNotAllowableInteraction;
import static org.s1queence.api.S1TextUtils.getConvertedTextFromConfig;
import static org.s1queence.api.S1Utils.sendActionBarMsg;
import static org.s1queence.api.S1Utils.setItemDamage;
import static org.s1queence.plugin.classes.GardenProcess.gardenHandlers;

public class PlowListener implements Listener {
    private final SlowGardening plugin;
    public PlowListener(final SlowGardening plugin) {
        this.plugin = plugin;
    }

    public static void setMoistureLevel(Block block, SlowGardening plugin) {
        YamlDocument cfg = plugin.getPluginConfig();
        if (!cfg.getBoolean("farmland_moisture_is_always_max")) return;
        BlockData bd = block.getBlockData();
        ((Farmland) bd).setMoisture(7);
        block.setBlockData(bd);
    }

    @EventHandler
    private void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getHand() == null) return;
        if (!e.getHand().equals(EquipmentSlot.HAND)) return;
        if (!e.getAction().equals(Action.LEFT_CLICK_BLOCK)) return;
        Block block = e.getClickedBlock();
        if (block == null) return;
        if (!plugin.isDirtBlock(block)) return;
        Player player = e.getPlayer();
        if (!player.getGameMode().equals(GameMode.SURVIVAL)) return;

        YamlDocument textCfg = plugin.getTextConfig();
        YamlDocument cfg = plugin.getPluginConfig();
        ItemStack mainItem = player.getInventory().getItemInMainHand();
        List<String> hoesLikeTools = plugin.getHoesLikeTools();
        String pName = plugin.getName();

        if (!hoesLikeTools.contains(mainItem.getType().toString())) return;
        if (!e.getBlockFace().equals(BlockFace.UP)) return;

        if (isNotAllowableInteraction(player, block.getLocation())) {
            e.setCancelled(true);
            return;
        }

        if (gardenHandlers.containsKey(block) || gardenHandlers.containsValue(player)) {
            e.setCancelled(true);
            return;
        }

        if (player.getAttackCooldown() != 1.0) {
            sendActionBarMsg(player, getConvertedTextFromConfig(textCfg, "attack_coolDown_is_not_expired", pName));
            return;
        }

        World world = block.getWorld();
        Map<Block, Integer> plowingBlocks = plugin.getPlowingBlocks();

        if (!plowingBlocks.containsKey(block)) plowingBlocks.put(block, cfg.getInt("hits_to_make_farmland"));
        int hits = plowingBlocks.get(block) - 1;

        world.playSound(player.getLocation(), Sound.ITEM_HOE_TILL, 1.0f, 1.0f);

        if (hits != 0) {
            String hitsLeftMsg = getConvertedTextFromConfig(textCfg, "farmland_hits_left", pName).replace("%hits_left%", hits + "");
            sendActionBarMsg(player, hitsLeftMsg);
            plowingBlocks.replace(block, hits);
            return;
        }

        plowingBlocks.remove(block);

        block.setType(Material.FARMLAND);
        setMoistureLevel(block, plugin);
        setItemDamage(mainItem, player, 1);
    }
}
