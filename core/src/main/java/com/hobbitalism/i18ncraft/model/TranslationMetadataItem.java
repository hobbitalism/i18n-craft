package com.hobbitalism.i18ncraft.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.nio.file.Path;

@Builder
@Getter
@ToString
public class TranslationMetadataItem {

    /**
     * The locale identifier (e.g. {@code "en_US"}, {@code "vi_VN"}).
     */
    private String locale;

    /**
     * The file-system path where the translation file is stored on disk after being copied
     * from the plugin resources.
     */
    private Path path;

    /**
     * The path of the translation file inside the plugin JAR resources
     * (e.g. {@code "i18n/en_US.yml"}).
     */
    private String resourcePath;
}
