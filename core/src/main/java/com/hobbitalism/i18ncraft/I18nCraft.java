package com.hobbitalism.i18ncraft;

import com.hobbitalism.i18ncraft.model.TranslationMetadata;
import com.hobbitalism.i18ncraft.model.TranslatorConfig;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;

/**
 * Central entry point for creating {@link ResourceTranslator} instances with
 * minimal configuration.
 */
public class I18nCraft {

    /** Default fallback locale used when none is specified. */
    public static final String DEFAULT_FALLBACK_LANGUAGE = "en_US";

    /** Default path to the JSON manifest inside the plugin JAR resources. */
    public static final String DEFAULT_MANIFEST_PATH = "i18n/i18n-manifest.json";

    /**
     * Creates a fully-configured {@link ResourceTranslator} using default
     * values: fallback language {@value #DEFAULT_FALLBACK_LANGUAGE} and
     * manifest path {@value #DEFAULT_MANIFEST_PATH}.
     *
     * @param plugin the owning Bukkit plugin
     * @return a fully initialized {@link ResourceTranslator}
     * @throws IOException if the manifest cannot be read or translation files
     *                     cannot be copied
     */
    public static ResourceTranslator createTranslator(Plugin plugin) throws IOException {
        return createTranslator(plugin, DEFAULT_FALLBACK_LANGUAGE, Path.of(DEFAULT_MANIFEST_PATH));
    }

    /**
     * Creates a fully-configured {@link ResourceTranslator} for the given
     * plugin, using sensible defaults.
     *
     * <p>This is the simplest way to get started — it handles every step:
     * <ol>
     *   <li>Creates a {@link TranslatorConfig} with the specified fallback
     *       locale and a default config directory of {@code "languages"}.</li>
     *   <li>Resolves the target directory to {@code <plugin-data-folder>/languages}.</li>
     *   <li>Builds the translator, calls {@link ResourceTranslator#loadFromResource(Path)},
     *       and returns the ready-to-use instance.</li>
     * </ol>
     *
     * @param plugin          the owning Bukkit plugin
     * @param fallbackLanguage the fallback locale (e.g. {@code "en_US"})
     * @param manifestPath    path to the JSON manifest inside the plugin JAR
     *                        (e.g. {@code Path.of("i18n/i18n-manifest.json")})
     * @return a fully initialized {@link ResourceTranslator}
     * @throws IOException if the manifest cannot be read or translation files
     *                     cannot be copied
     */
    public static ResourceTranslator createTranslator(Plugin plugin,
                                                      String fallbackLanguage,
                                                      Path manifestPath) throws IOException {
        TranslatorConfig config = TranslatorConfig.builder()
                .fallbackLanguage(fallbackLanguage)
                .build();

        Path targetDir = plugin.getDataFolder().toPath().resolve(config.getConfigDirectory());

        TranslationMetadata metadata = TranslationMetadata.builder()
                .path(targetDir)
                .build();

        ResourceTranslator translator = ResourceTranslator.builder()
                .plugin(plugin)
                .translationMetadata(metadata)
                .translatorConfig(config)
                .translationConfigMap(new HashMap<>())
                .build();

        translator.loadFromResource(manifestPath);
        return translator;
    }

    private I18nCraft() {
    }
}
