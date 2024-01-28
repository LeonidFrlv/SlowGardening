package org.s1queence.plugin.classes;

import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.s1queence.plugin.SlowGardening;
import org.s1queence.plugin.libs.YamlDocument;

public class TickSpeedReducer {
    public TickSpeedReducer(SlowGardening plugin, World world) {
        YamlDocument tsrConfig = plugin.getTsrConfig();
        world.setGameRule(GameRule.RANDOM_TICK_SPEED, 1);

        final int enabledTS = tsrConfig.getInt("reducer_values.enabled_tick_speed");
        final int initialEnabledTicks = tsrConfig.getInt("reducer_values.enabled_time") * 20;

        final int disabledTS = tsrConfig.getInt("reducer_values.disabled_tick_speed");
        final int initialDisabledTicks = tsrConfig.getInt("reducer_values.disabled_time") * 20;

        world.setGameRule(GameRule.RANDOM_TICK_SPEED, enabledTS);

        new BukkitRunnable() {
            private int ticks = initialEnabledTicks;
            private boolean enabled = true;
            @Override
            public void run() {
                if (ticks == 0) {
                    enabled = !enabled;
                    ticks = enabled ? initialEnabledTicks : initialDisabledTicks;
                    world.setGameRule(GameRule.RANDOM_TICK_SPEED, enabled ? enabledTS : disabledTS);
                    return;
                }

                ticks--;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

}
