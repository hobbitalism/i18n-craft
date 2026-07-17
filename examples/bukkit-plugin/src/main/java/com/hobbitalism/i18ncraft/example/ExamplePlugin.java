package com.hobbitalism.i18ncraft.example;

import com.hobbitalism.i18ncraft.I18nCraft;
import com.hobbitalism.i18ncraft.ResourceTranslator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Map;

public class ExamplePlugin extends JavaPlugin implements Listener {

    private ResourceTranslator translator;

    @Override
    public void onEnable() {
        try {
            translator = I18nCraft.createTranslator(this);
            getLogger().info("Loaded translations: " + translator.getTranslationMetadata().getItems().size() + " locales");
        } catch (IOException e) {
            getLogger().severe("Failed to load translations: " + e.getMessage());
        }

        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String locale = player.getLocale();
        Map<String, String> placeholders = Map.of("player", player.getName());

        player.sendMessage(tr(locale, "greeting.welcome", placeholders));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!"lang".equalsIgnoreCase(command.getName())) {
            return false;
        }

        if (sender instanceof Player player) {
            String locale = args.length > 0 ? args[0] : player.getLocale();
            String key = args.length > 1 ? args[1] : "greeting.welcome";
            player.sendMessage(translator.translate(locale, key, Map.of(
                    "player", player.getDisplayName()
            )));
        } else {
            String key = args.length > 0 ? args[0] : "greeting.welcome";
            sender.sendMessage(translator.translate(key, Map.of(
                    "player", "not a player ofc"
            )));
        }

        return true;
    }

    /**
     * Convenience method for translating a key with placeholders.
     */
    public String tr(String locale, String key, Map<String, String> placeholders) {
        return translator.translate(locale, key, placeholders);
    }
}
