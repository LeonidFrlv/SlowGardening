package org.s1queence.plugin.classes;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.s1queence.api.countdown.CountDownAction;
import org.s1queence.api.countdown.progressbar.ProgressBar;
import org.s1queence.plugin.SlowGardening;

import java.util.HashMap;

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

        World world = block.getWorld();
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
                        block.breakNaturally();
                        ItemMeta im = launchItem.getItemMeta();
                        if (im != null) { // сука тут тоже добавь что оно инстанс об дамагебл блять) да и вообще уже высри нахуй в s1queence lib ебучую эту срань с нанесением урону предмету, а то что это за пизда?
                            Damageable dIM = (Damageable) im;
                            int damage = dIM.getDamage() + 1;
                            if (launchItem.getType().getMaxDurability() - damage <= 0) {
                                player.getInventory().remove(launchItem);
                                world.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 5.0f, 1.0f);
                                world.spawnParticle(Particle.ITEM_CRACK, player.getLocation(), 10, 0.3, 0.5, 0.3, 0, launchItem);
                            } else {
                                dIM.setDamage(damage);
                                launchItem.setItemMeta(dIM);
                            }
                        }
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
