package com.hobbitalism.i18ncraft.util;

import lombok.experimental.UtilityClass;
import org.bukkit.plugin.Plugin;

import java.io.InputStream;

@UtilityClass
public class FileResourceUtil {

    /**
     * Retrieves an {@link InputStream} for a resource embedded in the plugin JAR, normalizing
     * the path to use forward-slashes and stripping any leading slash so it matches Bukkit's
     * {@code plugin.getResource()} lookup format.
     *
     * @param plugin       the plugin whose bundled resources are queried
     * @param resourcePath the resource path (e.g. {@code "i18n/en_US.yml"})
     * @return the input stream for the resource, or {@code null} if not found
     */
    public static InputStream getResourceStreamFromPlugin(Plugin plugin, String resourcePath) {
        String normalizedPath = resourcePath.replace('\\', '/');

        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }

        return plugin.getResource(normalizedPath);
    }

}
