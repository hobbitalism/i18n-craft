package com.hobbitalism.i18ncraft.example;

import com.hobbitalism.i18ncraft.ResourceTranslator;
import com.hobbitalism.i18ncraft.model.TranslationMetadata;
import com.hobbitalism.i18ncraft.model.TranslatorConfig;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ExamplePlugin extends JavaPlugin implements Listener {

    private ResourceTranslator translator;

    @Override
    public void onEnable() {
        TranslatorConfig config = TranslatorConfig.builder()
                .fallbackLanguage("en_US")
                .configDirectory("languages")
                .build();

        Path targetDir = getDataFolder().toPath().resolve(config.getConfigDirectory());
        TranslationMetadata metadata = TranslationMetadata.builder()
                .path(targetDir)
                .build();

        translator = ResourceTranslator.builder()
                .plugin(this)
                .translationMetadata(metadata)
                .translatorConfig(config)
                .translationConfigMap(new HashMap<>())
                .build();

        try {
            translator.loadFromResource(Path.of("i18n/i18n-manifest.json"));
            getLogger().info("Loaded translations: " + metadata.getItems().size() + " locales");
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
            player.sendMessage(translator.translate(locale, key, Map.of()));
        } else {
            String key = args.length > 0 ? args[0] : "greeting.welcome";
            sender.sendMessage(translator.translate(key, Map.of()));
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
