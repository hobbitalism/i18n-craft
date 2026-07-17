package com.hobbitalism.i18ncraft;

import com.hobbitalism.i18ncraft.model.TranslationMetadata;
import com.hobbitalism.i18ncraft.model.TranslationMetadataItem;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ResourceTranslatorTest {

    @Mock
    private Plugin plugin;

    @TempDir
    Path tempDir;

    // --- helpers ---

    private ResourceTranslator translatorWithPath(Path path) {
        return ResourceTranslator.builder()
                .plugin(plugin)
                .translationMetadata(TranslationMetadata.builder()
                        .path(path)
                        .build())
                .build();
    }

    private InputStream manifestWith(String... entries) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < entries.length; i++) {
            json.append("\"").append(entries[i]).append("\"");
            if (i < entries.length - 1) json.append(",");
        }
        json.append("]");
        return new ByteArrayInputStream(json.toString().getBytes(StandardCharsets.UTF_8));
    }

    private InputStream getTestResource(String path) {
        InputStream stream = ResourceTranslatorTest.class.getResourceAsStream(path);
        if (stream == null) {
            throw new IllegalStateException("Test resource not found: " + path);
        }
        return stream;
    }

    // --- null-guard tests ---

    @Test
    void loadFromResourceNullTranslationMetadataThrowsNullPointerException() {
        ResourceTranslator translator = ResourceTranslator.builder()
                .plugin(plugin)
                .translationMetadata(null)
                .build();

        assertThrows(NullPointerException.class,
                () -> translator.loadFromResource(Path.of("i18n/i18n-manifest.json")));
    }

    @Test
    void loadFromResourceNullFileNameThrowsNullPointerException() {
        ResourceTranslator translator = translatorWithPath(tempDir);

        assertThrows(NullPointerException.class,
                () -> translator.loadFromResource(null));
    }

    @Test
    void loadFromResourceNullTargetPathThrowsRuntimeException() {
        ResourceTranslator translator = ResourceTranslator.builder()
                .plugin(plugin)
                .translationMetadata(TranslationMetadata.builder()
                        .path(null)
                        .build())
                .build();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> translator.loadFromResource(Path.of("i18n/i18n-manifest.json")));
        assertEquals("The translation metadata target path is null.", ex.getMessage());
    }

    // --- manifest resolution tests ---

    @Test
    void loadFromResourceManifestNotFoundThrowsRuntimeException() {
        when(plugin.getResource("i18n/i18n-manifest.json")).thenReturn(null);

        ResourceTranslator translator = translatorWithPath(tempDir);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> translator.loadFromResource(Path.of("i18n/i18n-manifest.json")));
        assertEquals("The translation file does not exist in resources.", ex.getMessage());
    }

    @Test
    void loadFromResourceTranslationFileNotFoundThrowsRuntimeException() {
        when(plugin.getResource("i18n/i18n-manifest.json"))
                .thenReturn(manifestWith("i18n/en_US.yml"));
        when(plugin.getResource("i18n/en_US.yml")).thenReturn(null);

        ResourceTranslator translator = translatorWithPath(tempDir);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> translator.loadFromResource(Path.of("i18n/i18n-manifest.json")));
        assertEquals("Translation resource file not found: i18n/en_US.yml", ex.getMessage());
    }

    // --- happy path ---

    @Test
    void loadFromResourceCopiesFilesAndPopulatesItems() throws IOException {
        when(plugin.getResource("i18n/i18n-manifest.json"))
                .thenReturn(getTestResource("/i18n/i18n-manifest.json"));
        when(plugin.getResource("i18n/en_US.yml"))
                .thenReturn(getTestResource("/i18n/en_US.yml"));
        when(plugin.getResource("i18n/vi_VN.yml"))
                .thenReturn(getTestResource("/i18n/vi_VN.yml"));

        TranslationMetadata metadata = TranslationMetadata.builder().path(tempDir).build();
        ResourceTranslator translator = ResourceTranslator.builder()
                .plugin(plugin)
                .translationMetadata(metadata)
                .build();

        translator.loadFromResource(Path.of("i18n/i18n-manifest.json"));

        // Files must exist on disk
        assertTrue(Files.exists(tempDir.resolve("i18n/en_US.yml")));
        assertTrue(Files.exists(tempDir.resolve("i18n/vi_VN.yml")));

        // Items must be populated with correct locale and path
        List<TranslationMetadataItem> items = metadata.getItems();
        assertNotNull(items);
        assertEquals(2, items.size());

        TranslationMetadataItem en = items.get(0);
        assertEquals("en_US", en.getLocale());
        assertEquals(tempDir.resolve("i18n/en_US.yml"), en.getPath());

        TranslationMetadataItem vi = items.get(1);
        assertEquals("vi_VN", vi.getLocale());
        assertEquals(tempDir.resolve("i18n/vi_VN.yml"), vi.getPath());
    }

    // --- target directory creation ---

    @Test
    void loadFromResourceTargetDirectoryDoesNotExistIsCreated() throws IOException {
        Path nonExistentDir = tempDir.resolve("new-subdir");
        assertFalse(Files.exists(nonExistentDir));

        when(plugin.getResource("i18n/i18n-manifest.json"))
                .thenReturn(manifestWith("i18n/en_US.yml"));
        when(plugin.getResource("i18n/en_US.yml"))
                .thenReturn(getTestResource("/i18n/en_US.yml"));

        ResourceTranslator translator = ResourceTranslator.builder()
                .plugin(plugin)
                .translationMetadata(TranslationMetadata.builder()
                        .path(nonExistentDir)
                        .build())
                .build();

        translator.loadFromResource(Path.of("i18n/i18n-manifest.json"));

        assertTrue(Files.exists(nonExistentDir));
    }

    // --- file already exists ---

    @Test
    void loadFromResourceFileAlreadyExistsIsNotOverwrittenButStillInItems() throws IOException {
        Path existingFile = tempDir.resolve("i18n/en_US.yml");
        Files.createDirectories(existingFile.getParent());
        Files.writeString(existingFile, "original content");

        TranslationMetadata metadata = TranslationMetadata.builder().path(tempDir).build();
        ResourceTranslator translator = ResourceTranslator.builder()
                .plugin(plugin)
                .translationMetadata(metadata)
                .build();

        when(plugin.getResource("i18n/i18n-manifest.json"))
                .thenReturn(manifestWith("i18n/en_US.yml"));

        translator.loadFromResource(Path.of("i18n/i18n-manifest.json"));

        // plugin.getResource for the file must never be called since it already exists
        verify(plugin, never()).getResource("i18n/en_US.yml");

        // File content must be unchanged
        assertEquals("original content", Files.readString(existingFile));

        // Item must still be registered in metadata
        assertNotNull(metadata.getItems());
        assertEquals(1, metadata.getItems().size());
        assertEquals("en_US", metadata.getItems().get(0).getLocale());
        assertEquals(existingFile, metadata.getItems().get(0).getPath());
    }

}
