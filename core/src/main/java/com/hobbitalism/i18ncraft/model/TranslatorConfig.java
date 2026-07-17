package com.hobbitalism.i18ncraft.model;

import lombok.Builder;
import lombok.Getter;

/**
 * Global configuration for the translator, controlling fallback locale and the directory
 * where translation files are stored relative to the plugin's data folder.
 */
@Getter
@Builder
public class TranslatorConfig {

    /**
     * The fallback locale used when a requested locale is not available.
     * Defaults to {@code "en_US"}.
     */
    @Builder.Default
    private String fallbackLanguage = "en_US";

    /**
     * The sub-directory name (relative to the plugin's data folder) where
     * translation files are stored. Defaults to {@code "languages"}.
     */
    @Builder.Default
    private String configDirectory = "languages";

}
