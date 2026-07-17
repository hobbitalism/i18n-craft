package com.hobbitalism.i18ncraft.model;

import com.github.hobbitalism.miniconfig.yaml.YamlConfig;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * A YAML-backed configuration for a single locale's translation file. Extends
 * {@link com.github.hobbitalism.miniconfig.yaml.YamlConfig} to inherit file loading,
 * reloading, and key-value access for translation entries.
 */
public class TranslationConfig extends YamlConfig {

    /**
     * Creates a translation config backed by the given file.
     *
     * @param filePath the path to the YAML translation file, or {@code null} for
     *                 an in-memory-only configuration
     */
    public TranslationConfig(Path filePath) {
        super(filePath);
    }

    /**
     * Creates a translation config backed by the given file, with default values loaded
     * from the provided input stream.
     *
     * @param filePath the path to the YAML translation file, or {@code null} for
     *                 an in-memory-only configuration
     * @param defaults an input stream providing default key-value pairs to seed the config
     */
    public TranslationConfig(Path filePath, InputStream defaults) {
        super(filePath, defaults);
    }
}
