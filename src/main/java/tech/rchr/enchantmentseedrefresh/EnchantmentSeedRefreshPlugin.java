package tech.rchr.enchantmentseedrefresh;

import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.StringUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class EnchantmentSeedRefreshPlugin extends JavaPlugin implements Listener, TabCompleter {

    private FileConfiguration playerData;
    private File playerDataFile;

    private long lastSeedGenerationTime = 0;
    private boolean isRefreshing = false;

    private static final Set<Material> ENCHANTABLE_ITEMS = Set.of(
        // **Armor**
        Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,
        Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS,
        Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS,
        Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS,
        Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
        Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS,
        Material.TURTLE_HELMET,

        // **Weapons & Tools**
        Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD,
        Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE,
        Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE, Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE,
        Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL, Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL,
        Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE, Material.GOLDEN_HOE, Material.DIAMOND_HOE, Material.NETHERITE_HOE,

        // **Other Items**
        Material.BOW, Material.CROSSBOW, Material.TRIDENT,
        Material.FISHING_ROD,
        Material.ELYTRA,
        Material.SHIELD,
        Material.SHEARS,
        Material.FLINT_AND_STEEL,
        Material.CARROT_ON_A_STICK, Material.WARPED_FUNGUS_ON_A_STICK,
        Material.BRUSH,
        Material.COMPASS,

        // **Book Enchanting**
        Material.BOOK, Material.ENCHANTED_BOOK
    );

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("EnchantmentSeedRefresh by RalleWhite - plugin enabled!");

        // Check if esr.yml exists, if not create it
        File configFile = new File(getDataFolder(), "esr.yml");
        if (!configFile.exists()) {
            saveResource("esr.yml", false);  // Saves esr.yml from resources to the plugin folder
        }

        // Load the config from the file
        playerDataFile = new File(getDataFolder(), "esr.yml");
        playerData = YamlConfiguration.loadConfiguration(playerDataFile);

        // Optionally, read the config or set default values
        long cooldownTime = playerData.getLong("refreshCooldown", 1000);  // Default to 1000ms if not found
        getLogger().info("Cooldown time loaded: " + cooldownTime);

        // Register tab completion for /esr command
        getCommand("esr").setTabCompleter(this);
    }

    @Override
    public void onDisable() {
        getLogger().info("EnchantmentSeedRefresh by RalleWhite - plugin disabled!");
    }

    // Check if a player has the plugin enabled
    public boolean isPluginEnabled(Player player) {
        return playerData.getBoolean("players." + player.getUniqueId() + ".enabled", false);
    }

    // Save the player's state (enabled/disabled)
    public void setPluginEnabled(Player player, boolean enabled) {
        playerData.set("players." + player.getUniqueId() + ".enabled", enabled);
        savePlayerData();
    }

    // Save the player data to the esr.yml file
    private void savePlayerData() {
        try {
            playerData.save(playerDataFile);
        } catch (Exception e) {
            getLogger().warning("Could not save esr.yml!");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            // If no arguments are provided, show the brief message to use /esr ?
            if (args.length == 0) {
                sendChatMessage(player, "Use /esr ? for more info.");
                return true;
            }

            // If ? is provided, show the detailed help page
            if (args[0].equalsIgnoreCase("?")) {
                sendChatMessage(player, "Detailed help page:");
                sendChatMessage(player, "/esr on - Enable the plugin.");
                sendChatMessage(player, "/esr off - Disable the plugin.");
                sendChatMessage(player, "/esr refresh - Manually refresh your enchantment seed.");
                sendChatMessage(player, "/esr cooldown <time in ms> - Set cooldown (Admin only).");
                sendChatMessage(player, "/esr status - Show your plugin status and current cooldown.");
                sendChatMessage(player, "/esr ? - Show this help message.");
                return true;
            }

            // Handle individual subcommands
            if (args[0].equalsIgnoreCase("on")) {
                setPluginEnabled(player, true);
                sendChatMessage(player, "Plugin enabled for you.");
                return true;
            }

            if (args[0].equalsIgnoreCase("off")) {
                setPluginEnabled(player, false);
                sendChatMessage(player, "Plugin disabled for you.");
                return true;
            }

            if (args[0].equalsIgnoreCase("cooldown") && args.length == 2) {
                if (player.hasPermission("esr.cooldown")) {
                    try {
                        int cooldownTime = Integer.parseInt(args[1]);
                        playerData.set("refreshCooldown", cooldownTime); // Save the new cooldown value in esr.yml
                        savePlayerData(); // Ensure the playerData is saved
                        sendChatMessage(player, "Cooldown time set to " + cooldownTime + "ms.");
                    } catch (NumberFormatException e) {
                        sendChatMessage(player, "Invalid time format.");
                    }
                } else {
                    sendChatMessage(player, "You don't have permission to use this command.");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("refresh")) {
                refreshEnchantmentSeed(player);
                return true;
            }

            if (args[0].equalsIgnoreCase("status")) {
                showStatus(player);
                return true;
            }

            // Handle unknown or invalid commands (e.g., "/esr something")
            sendChatMessage(player, "Unknown command, try /esr ? for more info.");
            return true; // Ensures no default usage message is shown
        }
        return false;
    }

    private void sendChatMessage(Player player, String message) {
        // Format the message using ChatColor to apply color and style
        String formattedMessage = ChatColor.translateAlternateColorCodes('&', message);
        
        // Send the message with a custom prefix
        player.sendMessage(ChatColor.DARK_PURPLE + "[ESR] " + ChatColor.WHITE + formattedMessage);
    }

    // New method to refresh the enchantment seed
    private void refreshEnchantmentSeed(Player player) {
        long currentTime = System.currentTimeMillis();
        int cooldownTime = playerData.getInt("refreshCooldown", 1000); // Use playerData to get cooldown from esr.yml

        // Check if the cooldown has passed
        if (currentTime - lastSeedGenerationTime < cooldownTime) {
            sendChatMessage(player, "You must wait before refreshing the enchantment seed again.");
            return; // Prevent generating a new seed if the cooldown hasn't expired
        }

        lastSeedGenerationTime = currentTime;

        Random random = new Random();
        int newSeed = random.nextInt();
        player.setEnchantmentSeed(newSeed);
        getLogger().info("Manually refreshed enchantment seed for " + player.getName() + ": " + newSeed);

        // Inform the player when the seed is refreshed
        sendChatMessage(player, "Your enchantment seed has been manually refreshed.");
    }

    // New method to show the plugin status and cooldown
    private void showStatus(Player player) {
        boolean isEnabled = isPluginEnabled(player);
        int cooldownTime = playerData.getInt("refreshCooldown", 1000); // Get the cooldown from playerData (esr.yml)
        long cooldownSeconds = cooldownTime / 1000; // Convert milliseconds to seconds

        sendChatMessage(player, "ESR Plugin Status: " + (isEnabled ? "Enabled" : "Disabled"));
        sendChatMessage(player, "Current Cooldown: " + cooldownSeconds + " seconds");
    }

    @EventHandler
    public void onPrepareEnchant(PrepareItemEnchantEvent event) {
        Player player = event.getEnchanter();
        if (!isPluginEnabled(player)) {
            return; // If the player has the plugin disabled, do not allow enchantment seed generation
        }

        if (isRefreshing) return;

        long currentTime = System.currentTimeMillis();
        int cooldownTime = playerData.getInt("refreshCooldown", 1000); // Use playerData to get cooldown from esr.yml
        if (currentTime - lastSeedGenerationTime < cooldownTime) {
            return;  // Prevent generating a new seed if the cooldown hasn't expired
        }

        lastSeedGenerationTime = currentTime;

        Inventory inventory = event.getInventory();
        ItemStack itemToEnchant = event.getItem();
        ItemStack lapisSlotItem = inventory.getItem(1); // Slot 1 is the lapis slot

        if (itemToEnchant == null || itemToEnchant.getType().isAir()) {
            return; // No item in slot, do nothing
        }

        if (!ENCHANTABLE_ITEMS.contains(itemToEnchant.getType())) {
            return; // Item is not enchantable, do nothing
        }

        boolean wasLapisPresent = lapisSlotItem != null && lapisSlotItem.getType() == Material.LAPIS_LAZULI;

        if (!wasLapisPresent) {
            return;
        }

        Random random = new Random();
        int newSeed = random.nextInt();
        player.setEnchantmentSeed(newSeed);
        getLogger().info("Generated new enchantment seed for " + player.getName() + ": " + newSeed);

        // Inform the player when the seed is refreshed
        sendChatMessage(player, "Your enchantment seed has been refreshed because ESR is enabled.");
        
        isRefreshing = true;
        isRefreshing = false;
    }

    // Tab completer for /esr command
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        
        // If no arguments are provided or it's just the first argument, suggest possible subcommands
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], List.of("on", "off", "refresh", "cooldown", "status", "?"), completions);
        } 
        // If the second argument is 'cooldown', suggest numbers or empty string
        else if (args.length == 2 && args[0].equalsIgnoreCase("cooldown")) {
            completions.add("<time in ms>"); // You could also dynamically suggest valid numbers or previous values
        }

        // Return the completions
        return completions;
    }
}
