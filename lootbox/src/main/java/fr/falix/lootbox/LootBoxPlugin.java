package fr.falix.lootbox;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class LootBoxPlugin extends JavaPlugin {
    private static final long COOLDOWN_MS = 60L * 60L * 1000L;
    private final Map<UUID, Long> lastClaim = new LinkedHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadCooldowns();
        getLogger().info("LootBoxHoraire activé !");
    }

    @Override
    public void onDisable() {
        saveCooldowns();
    }

    private void loadCooldowns() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        var file = new java.io.File(getDataFolder(), "cooldowns.yml");
        var yaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            try {
                lastClaim.put(UUID.fromString(key), yaml.getLong(key));
            } catch (IllegalArgumentException ignored) {
                getLogger().warning("UUID invalide dans cooldowns.yml : " + key);
            }
        }
    }

    private void saveCooldowns() {
        var yaml = new org.bukkit.configuration.file.YamlConfiguration();
        lastClaim.forEach((uuid, time) -> yaml.set(uuid.toString(), time));
        try {
            yaml.save(new java.io.File(getDataFolder(), "cooldowns.yml"));
        } catch (java.io.IOException e) {
            getLogger().severe("Impossible de sauvegarder les cooldowns : " + e.getMessage());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            if (!sender.hasPermission("lootbox.admin")) {
                sender.sendMessage(color("&cTu n'as pas la permission."));
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(color("&cCe joueur doit être connecté."));
                return true;
            }
            lastClaim.remove(target.getUniqueId());
            saveCooldowns();
            sender.sendMessage(color("&aCooldown de " + target.getName() + " réinitialisé."));
            target.sendMessage(color("&aTon cooldown LootBox a été réinitialisé !"));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Cette commande doit être utilisée en jeu.");
            return true;
        }

        long now = System.currentTimeMillis();
        long previous = lastClaim.getOrDefault(player.getUniqueId(), 0L);
        long remaining = COOLDOWN_MS - (now - previous);
        if (remaining > 0) {
            long minutes = (remaining + 59_999L) / 60_000L;
            long hours = minutes / 60;
            long mins = minutes % 60;
            player.sendMessage(color("&cTu as déjà récupéré ta LootBox ! Reviens dans &e" +
                    (hours > 0 ? hours + " h " : "") + mins + " min&c."));
            return true;
        }

        // Le cooldown est enregistré au moment de la récupération, même si l'inventaire est plein.
        lastClaim.put(player.getUniqueId(), now);
        saveCooldowns();
        ItemStack reward = rollReward();
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(reward);
        leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));

        player.sendMessage(color("&6&lLOOTBOX &8» &aTu as récupéré ta LootBox horaire !"));
        player.sendMessage(color("&eRécompense : &f" + displayName(reward)));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        return true;
    }

    private ItemStack rollReward() {
        double roll = ThreadLocalRandom.current().nextDouble(100.0);
        if (roll < 0.5) return new ItemStack(Material.ELYTRA, 1);                         // 0,5 %
        if (roll < 7.5) return new ItemStack(Material.NETHERITE_INGOT, 1);                 // 7 %
        if (roll < 27.5) return new ItemStack(Material.DIAMOND, 5);                       // 20 %
        if (roll < 30.0) return creeperSpawner();                                         // 2,5 %
        if (roll < 70.0) return new ItemStack(Material.GOLDEN_APPLE, 5);                  // 40 %
        if (roll < 80.0) return new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1);        // 10 %
        return new ItemStack(Material.TOTEM_OF_UNDYING, 1);                                // 20 %
    }

    private ItemStack creeperSpawner() {
        ItemStack item = new ItemStack(Material.SPAWNER, 1);
        if (item.getItemMeta() instanceof BlockStateMeta meta && meta.getBlockState() instanceof CreatureSpawner spawner) {
            spawner.setSpawnedType(EntityType.CREEPER);
            meta.setBlockState(spawner);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String displayName(ItemStack item) {
        return switch (item.getType()) {
            case ELYTRA -> "une paire d'élytras";
            case NETHERITE_INGOT -> "un lingot de netherite";
            case DIAMOND -> "5 diamants";
            case SPAWNER -> "un spawner à creeper";
            case GOLDEN_APPLE -> "5 pommes dorées";
            case ENCHANTED_GOLDEN_APPLE -> "une pomme de Notch";
            case TOTEM_OF_UNDYING -> "un totem d'immortalité";
            default -> item.getAmount() + "x " + item.getType().name();
        };
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
