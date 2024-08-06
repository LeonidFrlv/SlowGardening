package org.s1queence.plugin.classes;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.type.CaveVinesPlant;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.s1queence.api.countdown.CountDownAction;
import org.s1queence.api.countdown.progressbar.ProgressBar;
import org.s1queence.plugin.SlowGardening;

import java.util.HashMap;

import static org.s1queence.api.S1Utils.setItemDamage;

public class GardenProcess extends CountDownAction {
    public final static HashMap<Block, Player> gardenHandlers = new HashMap<>();
    private final Block block;
    public GardenProcess(
            @NotNull Player player,
            @NotNull Player target,
            int seconds,
            boolean isDoubleRunnableAction,
            boolean isClosePlayersInventoriesEveryTick,
            @NotNull ProgressBar pb,
            @NotNull Block block,
            @NotNull GardenActionType gat,
            @NotNull SlowGardening plugin,
            @NotNull String everyTickBothActionBarMsg,
            @NotNull String everyTickPlayerTitle,
            @NotNull String everyTickPlayerSubtitle,
            @Nullable String everyTickTargetTitle,
            @Nullable String everyTickTargetSubtitle,
            @NotNull String completeBothActionBarMsg,
            @NotNull String completePlayerTitle,
            @NotNull String completePlayerSubtitle,
            @Nullable String completeTargetTitle,
            @Nullable String completeTargetSubtitle,
            @NotNull String cancelBothActionBarMsg,
            @NotNull String cancelPlayerTitle,
            @NotNull String cancelPlayerSubtitle,
            @Nullable String cancelTargetTitle,
            @Nullable String cancelTargetSubtitle) {
        super(player, target, seconds, isDoubleRunnableAction, isClosePlayersInventoriesEveryTick, pb, plugin, everyTickBothActionBarMsg, everyTickPlayerTitle, everyTickPlayerSubtitle, everyTickTargetTitle, everyTickTargetSubtitle, completeBothActionBarMsg, completePlayerTitle, completePlayerSubtitle, completeTargetTitle, completeTargetSubtitle, cancelBothActionBarMsg, cancelPlayerTitle, cancelPlayerSubtitle, cancelTargetTitle, cancelTargetSubtitle);
        this.block = block;
        Material blockType = block.getType();

        gardenHandlers.put(block, player);
        ItemStack launchItem = getLaunchItem();

        Location blockLocation = block.getLocation();

        actionCountDown();

        new BukkitRunnable() {
            @Override
            public void run() {
                if (isActionCanceled()) {
                    gardenHandlers.remove(block);
                    cancelAction(true);
                    cancel();
                    return;
                }

                if (!getPreprocessActionHandlers().containsKey(player)) {
                    if (gat.equals(GardenActionType.HARVESTING)) {

                        boolean isSweetBerries = blockType.equals(Material.SWEET_BERRY_BUSH);
                        if (isSweetBerries || blockType.equals(Material.CAVE_VINES_PLANT)) {
                            if (isSweetBerries) {
                                Ageable ageable = (Ageable) block.getBlockData();
                                ageable.setAge(ageable.getMaximumAge() - 2);
                                block.setBlockData(ageable);
                            } else {
                                CaveVinesPlant caveVinesPlant = (CaveVinesPlant) block.getBlockData();
                                caveVinesPlant.setBerries(false);
                                block.setBlockData(caveVinesPlant);
                            }

                            World world = block.getWorld();
                            world.dropItemNaturally(blockLocation, new ItemStack(isSweetBerries ? Material.SWEET_BERRIES : Material.GLOW_BERRIES , 3));
                            world.playSound(blockLocation, isSweetBerries ? Sound.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES : Sound.BLOCK_CAVE_VINES_PICK_BERRIES, 1.0f,1.0f);
                        } else {
                            block.breakNaturally();
                        }

                        setItemDamage(launchItem, player, 1);
                    }

                    if (gat.equals(GardenActionType.PLANTING)) {
                        blockLocation.getBlock().setType(blockType);
                        launchItem.setAmount(launchItem.getAmount() - 1);
                    }

                    gardenHandlers.remove(block);
                    cancelAction(false);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    @Override
    protected boolean isActionCanceled() {
        Player player = getPlayer();
        boolean isSneaking = player.isSneaking();
        boolean isLaunchItemInitial = player.getInventory().getItemInMainHand().equals(getLaunchItem());
        boolean isOnline = player.isOnline();
        boolean isDead = player.isDead();
        boolean isBlockIsBusy = gardenHandlers.containsKey(block);
        boolean isLeaveFromStartLocation = !(new Location(player.getWorld(), player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ())).equals(getStartLocation());
        return isSneaking || !isLaunchItemInitial || !isOnline || isDead || isLeaveFromStartLocation || !isBlockIsBusy;
    }
}
