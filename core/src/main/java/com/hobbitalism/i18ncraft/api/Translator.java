package com.hobbitalism.i18ncraft.api;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Core interface for managing localization and retrieving translated messages.
 */
public interface Translator {

    /**
     * Loads translation metadata and necessary files from the specified resource manifest.
     *
     * @param fileName the name of the resource file manifest to load
     */
    void loadFromResource(Path fileName) throws IOException;

    /**
     * Retrieves a translated string using the configured fallback locale and replaces its
     * placeholders. Use this when no specific locale is applicable (e.g. console messages).
     *
     * @param key          the translation key to look up
     * @param placeholders a map containing placeholder keys and their corresponding replacement values
     * @return the fully translated string with placeholders applied
     */
    String translate(String key, Map<String, String> placeholders);

    /**
     * Retrieves a translated string for a specific locale and replaces its placeholders.
     * If the locale is {@code null} or not found, the configured fallback locale is used instead.
     *
     * @param locale       the target locale identifier, or {@code null} to use the fallback
     * @param key          the translation key to look up
     * @param placeholders a map containing placeholder keys and their corresponding replacement values
     * @return the fully translated string with placeholders applied
     */
    String translate(String locale, String key, Map<String, String> placeholders);

    /**
     * Reloads all translation configurations synchronously.
     *
     * @throws IOException if an I/O error occurs during the reload process.
     */
    void reload() throws IOException;

    /**
     * Reloads all translation configurations asynchronously.
     *
     * @return a CompletableFuture that completes when the reload operation finishes.
     */
    CompletableFuture<Void> reloadAsync();
}