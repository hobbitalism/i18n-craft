package com.hobbitalism.i18ncraft.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.nio.file.Path;
import java.util.List;

/**
 * Metadata describing where translation files are copied to and the individual locale items
 * discovered during the resource loading process.
 */
@Builder
@Getter
@ToString
public class TranslationMetadata {

    /**
     * The target directory where translation files are copied to on disk.
     */
    private Path path;

    /**
     * The list of discovered locale items, each containing the locale, disk path,
     * and resource path of a translation file. Populated by
     * {@code ResourceTranslator.loadFromResource}.
     */
    @Setter
    private List<TranslationMetadataItem> items;

}
