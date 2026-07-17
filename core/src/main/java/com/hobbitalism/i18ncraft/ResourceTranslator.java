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

@Builder
@AllArgsConstructor
public class ResourceTranslator implements Translator {

    private TranslationMetadata translationMetadata;
    private Map<String, TranslationConfig> translationConfigMap;
    private Plugin plugin;
    private TranslatorConfig translatorConfig;

    /**
     * {@inheritDoc}
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

    private String findLocaleOrUseFallback(String locale) {
        return translationMetadata.getItems().stream()
                .map(TranslationMetadataItem::getLocale)
                .filter(item -> item.equalsIgnoreCase(locale))
                .findFirst()
                .orElse(translatorConfig.getFallbackLanguage());
    }

    private Optional<TranslationMetadataItem> findMetadataItem(String locale) {
        return translationMetadata.getItems()
                .stream()
                .filter(item -> item.getLocale().equalsIgnoreCase(locale))
                .findFirst();
    }

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

    @Override
    public void reload() throws IOException {
        for (TranslationConfig translationConfig : translationConfigMap.values()) {
            translationConfig.reload();
        }
    }

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