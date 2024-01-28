package org.s1queence.plugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.s1queence.api.countdown.progressbar.ProgressBar;
import org.s1queence.plugin.classes.TickSpeedReducer;
import org.s1queence.plugin.libs.YamlDocument;
import org.s1queence.plugin.listeners.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.s1queence.api.S1TextUtils.consoleLog;
import static org.s1queence.api.S1TextUtils.getConvertedTextFromConfig;
import static org.s1queence.plugin.classes.GardenProcess.gardenHandlers;

public final class SlowGardening extends JavaPlugin implements CommandExecutor {
    private YamlDocument config;
    private YamlDocument listsConfig;
    private YamlDocument textConfig;
    private YamlDocument tsrConfig;
    private ProgressBar pb;
    private List<String> hoesLikeTools;
    private List<String> harvestingTools;
    private List<String> plants;
    private final HashMap<Block, Integer> plowingBlocks = new HashMap<>();

    @Override
    public void onEnable() {

        try {
            File configFile = new File(getDataFolder(), "config.yml");
            File listsConfigFile = new File(getDataFolder(), "lists.yml");
            File textConfigFile = new File(getDataFolder(), "text.yml");
            File tsrConfigFile = new File(getDataFolder(), "tick_speed_reducer.yml");

            config = configFile.exists() ? YamlDocument.create(configFile) : YamlDocument.create(new File(getDataFolder(), "config.yml"), Objects.requireNonNull(getResource("config.yml")));
            listsConfig = listsConfigFile.exists() ? YamlDocument.create(listsConfigFile) : YamlDocument.create(new File(getDataFolder(), "lists.yml"), Objects.requireNonNull(getResource("lists.yml")));
            textConfig = textConfigFile.exists() ? YamlDocument.create(textConfigFile) : YamlDocument.create(new File(getDataFolder(), "text.yml"), Objects.requireNonNull(getResource("text.yml")));
            tsrConfig = tsrConfigFile.exists() ? YamlDocument.create(tsrConfigFile) : YamlDocument.create(new File(getDataFolder(), "tick_speed_reducer.yml"), Objects.requireNonNull(getResource("tick_speed_reducer.yml")));
        } catch (IOException ignored) {

        }

        pb = new ProgressBar(
                0,
                1,
                config.getInt("progress_bar.max_bars"),
                config.getString("progress_bar.symbol"),
                ChatColor.translateAlternateColorCodes('&', config.getString("progress_bar.border_left")),
                ChatColor.translateAlternateColorCodes('&', config.getString("progress_bar.border_right")),
                ChatColor.getByChar(config.getString("progress_bar.color")),
                ChatColor.getByChar(config.getString("progress_bar.complete_color")),
                ChatColor.getByChar(config.getString("progress_bar.percent_color"))
        );

        if (tsrConfig.getBoolean("enabled")) {
            for (String name : tsrConfig.getStringList("enabled_worlds")) {
                World world = getServer().getWorld(name);
                if (world != null) new TickSpeedReducer(this, world);
            }
        }

        consoleLog(getConvertedTextFromConfig(textConfig, "onEnable_msg", this.getName()), this);

        hoesLikeTools = listsConfig.getStringList("hoe_like_items");
        harvestingTools = listsConfig.getStringList("harvesting_tools");
        plants = listsConfig.getStringList("plants");
        Objects.requireNonNull(getServer().getPluginCommand("slowGardening")).setExecutor(this);
        getServer().getPluginManager().registerEvents(new CancelingDefaultsListener(this), this);
        getServer().getPluginManager().registerEvents(new PlowListener(this), this);
        getServer().getPluginManager().registerEvents(new PlantListener(this), this);
        getServer().getPluginManager().registerEvents(new HarvestListener(this), this);

    }

    @Override
    public void onDisable() {
        consoleLog(getConvertedTextFromConfig(textConfig, "onDisable_msg", this.getName()), this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length != 1) return false;
        if (!args[0].equalsIgnoreCase("reload")) return false;

        try {
            File configFile = new File(getDataFolder(), "config.yml");
            File listsConfigFile = new File(getDataFolder(), "lists.yml");
            File textConfigFile = new File(getDataFolder(), "text.yml");

            config = configFile.exists() ? YamlDocument.create(configFile) : YamlDocument.create(new File(getDataFolder(), "config.yml"), Objects.requireNonNull(getResource("config.yml")));
            listsConfig = listsConfigFile.exists() ? YamlDocument.create(listsConfigFile) : YamlDocument.create(new File(getDataFolder(), "lists.yml"), Objects.requireNonNull(getResource("lists.yml")));
            textConfig = textConfigFile.exists() ? YamlDocument.create(textConfigFile) : YamlDocument.create(new File(getDataFolder(), "text.yml"), Objects.requireNonNull(getResource("text.yml")));

            if (config.hasDefaults()) Objects.requireNonNull(config.getDefaults()).clear();
            if (listsConfig.hasDefaults()) Objects.requireNonNull(listsConfig.getDefaults()).clear();
            if (textConfig.hasDefaults()) Objects.requireNonNull(textConfig.getDefaults()).clear();

            config.reload();
            listsConfig.reload();
            textConfig.reload();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        pb = new ProgressBar(
                0,
                1,
                config.getInt("progress_bar.max_bars"),
                config.getString("progress_bar.symbol"),
                ChatColor.translateAlternateColorCodes('&', config.getString("progress_bar.border_left")),
                ChatColor.translateAlternateColorCodes('&', config.getString("progress_bar.border_right")),
                ChatColor.getByChar(config.getString("progress_bar.color")),
                ChatColor.getByChar(config.getString("progress_bar.complete_color")),
                ChatColor.getByChar(config.getString("progress_bar.percent_color"))
        );

        hoesLikeTools = listsConfig.getStringList("hoe_like_items");
        harvestingTools = listsConfig.getStringList("harvesting_tools");
        plants = listsConfig.getStringList("plants");
        gardenHandlers.clear();
        plowingBlocks.clear();

        String reloadMsg = getConvertedTextFromConfig(textConfig, "onReload_msg", this.getName());
        consoleLog(reloadMsg, this);
        if (sender instanceof Player) sender.sendMessage(reloadMsg);

        return true;
    }

    public boolean isDirtBlock(Block block) {
        Material type = block.getType();
        return type.equals(Material.GRASS_BLOCK) || type.equals(Material.PODZOL) || type.toString().contains("DIRT");
    }

    public YamlDocument getPluginConfig() {
        return config;
    }

    public YamlDocument getListsConfig() {
        return listsConfig;
    }

    public List<String> getHarvestingTools() {
        return harvestingTools;
    }

    public List<String> getHoesLikeTools() {
        return hoesLikeTools;
    }

    public List<String> getPlants() {
        return plants;
    }
    public ProgressBar getProgressBar() {
        return pb;
    }

    public Map<Block, Integer> getPlowingBlocks() {
        return plowingBlocks;
    }

    public YamlDocument getTextConfig() {
        return textConfig;
    }

    public YamlDocument getTsrConfig() {
        return tsrConfig;
    }
}

