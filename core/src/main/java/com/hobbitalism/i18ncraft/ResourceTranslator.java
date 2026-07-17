package com.hobbitalism.i18ncraft;

import com.github.hobbitalism.miniconfig.Config;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.hobbitalism.i18ncraft.api.Translator;
import com.hobbitalism.i18ncraft.model.TranslationConfig;
import com.hobbitalism.i18ncraft.model.TranslationMetadata;
import com.hobbitalism.i18ncraft.model.TranslationMetadataItem;
import com.hobbitalism.i18ncraft.model.TranslatorConfig;
import com.hobbitalism.i18ncraft.util.FileResourceUtil;
import com.hobbitalism.i18ncraft.util.SetUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * The core implementation of {@link Translator} that resolves translation keys
 * against locale-specific YAML configuration files.
 *
 * <p>{@code ResourceTranslator} supports the following workflow:
 * <ol>
 *   <li><b>Loading:</b> {@link #loadFromResource(Path)} reads a JSON manifest
 *       from the plugin JAR, copies missing translation files to a data
 *       directory, and registers each file's locale.</li>
 *   <li><b>Translation:</b> {@link #translate(String, String, Map)} resolves a
 *       key for a given locale, loads (and caches) the corresponding YAML
 *       config, applies placeholder substitution via the configured
 *       {@link com.hobbitalism.i18ncraft.api.PlaceholderProcessor}, and returns
 *       the result.</li>
 *   <li><b>Patching:</b> When a translation file is first loaded, any keys
 *       present in the bundled resource but missing from the on-disk file are
 *       automatically appended and the file is saved.</li>
 *   <li><b>Reloading:</b> {@link #reload()} and {@link #reloadAsync()} refresh
 *       all cached configurations from disk.</li>
 * </ol>
 *
 * <p>Instances are created via the Lombok {@code @Builder}:
 * <pre>{@code
 * ResourceTranslator translator = ResourceTranslator.builder()
 *         .plugin(plugin)
 *         .translationMetadata(metadata)
 *         .translationConfigMap(new HashMap<>())
 *         .translatorConfig(TranslatorConfig.builder()
 *                 .fallbackLanguage("en_US")
 *                 .build())
 *         .build();
 * }</pre>
 *
 * @see Translator
 * @see TranslationConfig
 * @see TranslationMetadata
 * @see TranslatorConfig
 */
@Builder
@AllArgsConstructor
@Getter
public class ResourceTranslator implements Translator {

    /** Metadata describing the available locales and their file paths. */
    private TranslationMetadata translationMetadata;

    /** Cache of loaded {@link TranslationConfig} instances keyed by locale. */
    private Map<String, TranslationConfig> translationConfigMap;

    /** The owning Bukkit {@link Plugin} used for resource access and logging. */
    private Plugin plugin;

    /** Configuration for fallback language, placeholder processing, etc. */
    private TranslatorConfig translatorConfig;

    /**
     * Reads a JSON manifest from the plugin JAR, copies any missing
     * translation files to the data directory, and registers the
     * resulting locale-to-path mappings in {@link TranslationMetadata}.
     *
     * <p>The manifest file is a JSON array of resource paths
     * (e.g. {@code ["i18n/en_US.yml", "i18n/vi_VN.yml"]}). Each listed
     * file is copied to the target directory if it does not already
     * exist, and its locale is derived from the file name stem
     * (e.g. {@code en_US} from {@code en_US.yml}).
     *
     * @param fileName path to the JSON manifest file inside the plugin JAR
     * @throws IOException              if reading or copying any file fails
     * @throws NullPointerException     if {@code fileName} is null, or if
     *                                  {@code translationMetadata} was not
     *                                  set before calling this method
     * @throws RuntimeException         if the manifest does not exist in the
     *                                  JAR, or if a listed resource file is
     *                                  missing
     */
    public void loadFromResource(Path fileName) throws IOException {
        Objects.requireNonNull(this.translationMetadata, "The translation metadata object is null.");
        Objects.requireNonNull(fileName, "The file name is null.");

        // Load metadata file
        Path targetDirectory = translationMetadata.getPath();
        if (targetDirectory == null) {
            throw new RuntimeException("The translation metadata target path is null.");
        }

        if (!Files.exists(targetDirectory)) {
            Files.createDirectories(targetDirectory);
        }

        // Perform copy all files
        List<String> fileNamesToCopy = new ArrayList<>();

        try (InputStream manifestStream = FileResourceUtil.getResourceStreamFromPlugin(plugin, fileName.toString())) {
            if (manifestStream == null) {
                throw new RuntimeException("The translation file does not exist in resources.");
            }

            try (JsonReader reader = new JsonReader(new InputStreamReader(manifestStream, StandardCharsets.UTF_8))) {
                JsonArray filesArray = JsonParser.parseReader(reader).getAsJsonArray();
                for (JsonElement element : filesArray) {
                    fileNamesToCopy.add(element.getAsString());
                }
            }
        }

        List<TranslationMetadataItem> items = new ArrayList<>();

        for (String fileToCopy : fileNamesToCopy) {
            Path targetFile = targetDirectory.resolve(fileToCopy);

            if (!Files.exists(targetFile)) {
                try (InputStream resourceStream = plugin.getResource(fileToCopy)) {
                    if (resourceStream == null) {
                        throw new RuntimeException("Translation resource file not found: " + fileToCopy);
                    }
                    Files.createDirectories(targetFile.getParent());
                    Files.copy(resourceStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            // Derive locale from the file name stem (e.g. "i18n/en_US.yml" -> "en_US")
            String fileName2 = Path.of(fileToCopy).getFileName().toString();
            String locale = fileName2.contains(".")
                    ? fileName2.substring(0, fileName2.lastIndexOf('.'))
                    : fileName2;

            items.add(TranslationMetadataItem.builder()
                    .locale(locale)
                    .path(targetFile)
                    .resourcePath(fileToCopy)
                    .build());
        }

        translationMetadata.setItems(items);
    }

    @Override
    public String translate(String key, Map<String, String> placeholders) {
        return translate(translatorConfig.getFallbackLanguage(), key, placeholders);
    }

    @Override
    public String translate(String locale, String key, Map<String, String> placeholders) {
        Objects.requireNonNull(locale, "The locale cannot be null.");
        Objects.requireNonNull(key, "The key cannot be null.");
        String localeOrUseFallback = findLocaleOrUseFallback(locale);
        Optional<TranslationMetadataItem> metadataItem = findMetadataItem(localeOrUseFallback);

        // If the metadata item is not found. Return a key with a warning
        if (metadataItem.isEmpty()) {
            plugin.getLogger().warning("Translation resource file not found: " + locale);
            return key;
        }

        TranslationMetadataItem translationMetadataItem = metadataItem.get();
        Config config = loadConfigurationFromMetadataItem(translationMetadataItem);
        String translated = config.getString(key, key);
        return translatorConfig.getPlaceholderProcessor().transform(translated, placeholders);
    }

    /**
     * Attempts to find the given locale among registered items. If no match
     * is found (case-insensitive), the configured {@code fallbackLanguage}
     * is returned instead.
     */
    private String findLocaleOrUseFallback(String locale) {
        return translationMetadata.getItems().stream()
                .map(TranslationMetadataItem::getLocale)
                .filter(item -> item.equalsIgnoreCase(locale))
                .findFirst()
                .orElse(translatorConfig.getFallbackLanguage());
    }

    /**
     * Returns the {@link TranslationMetadataItem} whose locale matches the
     * given string (case-insensitive), or empty if no such item exists.
     */
    private Optional<TranslationMetadataItem> findMetadataItem(String locale) {
        return translationMetadata.getItems()
                .stream()
                .filter(item -> item.getLocale().equalsIgnoreCase(locale))
                .findFirst();
    }

    /**
     * Loads (or retrieves from cache) the {@link Config} for the locale
     * represented by the given metadata item.
     *
     * <p>On first access, this method reads the on-disk YAML file and the
     * bundled resource, then calls {@link #patchConfigureFile} to merge
     * any missing keys from the resource into the disk file.
     *
     * @param item the metadata item pointing to the file to load
     * @return the loaded {@link Config} instance
     * @throws RuntimeException if the file does not exist or cannot be read
     */
    private Config loadConfigurationFromMetadataItem(TranslationMetadataItem item) {
        return translationConfigMap.computeIfAbsent(item.getLocale(), (key) -> {
            Path configPath = item.getPath();

            if (!Files.exists(configPath)) {
                throw new RuntimeException(("Translation resource file not found: %s." +
                        " Hint: define a file in resource and put it into metadata.json file.").formatted(configPath));
            }

            TranslationConfig localTranslationConfig = new TranslationConfig(null);
            TranslationConfig targetTranslationConfig = new TranslationConfig(configPath);
            try (InputStream resourceFileStream = FileResourceUtil.getResourceStreamFromPlugin(plugin, item.getResourcePath())) {
                localTranslationConfig.loadFromStream(resourceFileStream);
                targetTranslationConfig.load();
                patchConfigureFile(localTranslationConfig, targetTranslationConfig, configPath.getFileName().toString());
                return targetTranslationConfig;
            } catch (IOException e) {
                throw new RuntimeException("Unable to load a translation file into memory.", e);
            }
        });
    }

    /**
     * Merges keys from the bundled resource config into the on-disk config.
     *
     * <p>Any key that exists in {@code resourceConfig} but not in
     * {@code targetConfig} is written to the target and the file is saved
     * to disk. This ensures that newly added translation keys in plugin
     * updates are automatically picked up without overwriting existing
     * translations.
     *
     * @param resourceConfig the config loaded from the plugin JAR
     * @param targetConfig   the config loaded from the on-disk file
     * @param targetFileName the file name used for error messages
     * @throws IOException if the target file cannot be saved
     */
    private void patchConfigureFile(TranslationConfig resourceConfig,
                                    TranslationConfig targetConfig,
                                    String targetFileName) throws IOException {
        Set<String> resourceConfigKeys = resourceConfig.getKeys(true);
        Set<String> targetConfigKeys = targetConfig.getKeys(true);
        Set<String> difference = SetUtil.difference(resourceConfigKeys, targetConfigKeys);

        if (difference.isEmpty()) {
            return;
        }

        difference.forEach(k -> {
            Object value = resourceConfig.get(k);
            if (value instanceof Optional<?> opt) {
                targetConfig.set(k, opt.orElse(null));
            } else {
                targetConfig.set(k, value);
            }
        });
        try {
            targetConfig.save();
        } catch (IOException e) {
            throw new IOException("Unable to save the patched translation configuration file: %s."
                    .formatted(targetFileName), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reload() throws IOException {
        for (TranslationConfig translationConfig : translationConfigMap.values()) {
            translationConfig.reload();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<Void> reloadAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                this.reload();
            } catch (IOException e) {
                throw new RuntimeException("Unable to async reload the translator.", e);
            }
        });
    }

    @Override
    public String toString() {
        return "ResourceTranslator{" +
                "translationMetadata=" + translationMetadata +
                ", plugin=" + plugin +
                '}';
    }
}