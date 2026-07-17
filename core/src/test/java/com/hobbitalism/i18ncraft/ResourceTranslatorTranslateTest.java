package com.hobbitalism.i18ncraft;

import com.hobbitalism.i18ncraft.model.TranslationConfig;
import com.hobbitalism.i18ncraft.model.TranslationMetadata;
import com.hobbitalism.i18ncraft.model.TranslationMetadataItem;
import com.hobbitalism.i18ncraft.model.TranslatorConfig;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceTranslatorTranslateTest {

    @Mock
    private Plugin plugin;

    @TempDir
    Path tempDir;

    // Paths to real YAML files written into tempDir
    private Path enFile;
    private Path viFile;

    // Pre-loaded configs seeded into the map to avoid hitting plugin.getResource in these tests
    private TranslationConfig enConfig;
    private TranslationConfig viConfig;

    private TranslationMetadataItem enItem;
    private TranslationMetadataItem viItem;

    @BeforeEach
    void setUp() throws IOException {
        enFile = tempDir.resolve("en_US.yml");
        viFile = tempDir.resolve("vi_VN.yml");

        Files.writeString(enFile,
                "error:\n" +
                "  generic: \"An error has occurred\"\n" +
                "  unknown: \"Unknown error\"\n");

        Files.writeString(viFile,
                "error:\n" +
                "  generic: \"Đã có lỗi xảy ra\"\n" +
                "  unknown: \"Lỗi không xác định\"\n");

        enConfig = new TranslationConfig(enFile);
        enConfig.load();

        viConfig = new TranslationConfig(viFile);
        viConfig.load();

        enItem = TranslationMetadataItem.builder()
                .locale("en_US")
                .path(enFile)
                .resourcePath("i18n/en_US.yml")
                .build();

        viItem = TranslationMetadataItem.builder()
                .locale("vi_VN")
                .path(viFile)
                .resourcePath("i18n/vi_VN.yml")
                .build();
    }

    // --- helpers ---

    private ResourceTranslator translatorWith(List<TranslationMetadataItem> items,
                                              Map<String, TranslationConfig> configMap,
                                              String fallback) {
        return ResourceTranslator.builder()
                .plugin(plugin)
                .translationMetadata(TranslationMetadata.builder()
                        .path(tempDir)
                        .items(items)
                        .build())
                .translationConfigMap(configMap)
                .translatorConfig(TranslatorConfig.builder()
                        .fallbackLanguage(fallback)
                        .build())
                .build();
    }

    private Map<String, TranslationConfig> configMapWith(String locale, TranslationConfig config) {
        Map<String, TranslationConfig> map = new HashMap<>();
        map.put(locale, config);
        return map;
    }

    // --- null-guard tests ---

    @Test
    void translateNullLocaleThrowsNullPointerException() {
        ResourceTranslator translator = translatorWith(
                List.of(enItem),
                configMapWith("en_US", enConfig),
                "en_US");

        assertThrows(NullPointerException.class,
                () -> translator.translate(null, "error.generic", Map.of()));
    }

    @Test
    void translateNullKeyThrowsNullPointerException() {
        ResourceTranslator translator = translatorWith(
                List.of(enItem),
                configMapWith("en_US", enConfig),
                "en_US");

        assertThrows(NullPointerException.class,
                () -> translator.translate("en_US", null, Map.of()));
    }

    // --- locale resolution tests ---

    @Test
    void translateKnownLocaleReturnsCorrectValue() {
        ResourceTranslator translator = translatorWith(
                List.of(enItem, viItem),
                new HashMap<>(Map.of("en_US", enConfig, "vi_VN", viConfig)),
                "en_US");

        assertEquals("An error has occurred", translator.translate("en_US", "error.generic", Map.of()));
        assertEquals("Đã có lỗi xảy ra", translator.translate("vi_VN", "error.generic", Map.of()));
    }

    @Test
    void translateLocaleIsCaseInsensitive() {
        ResourceTranslator translator = translatorWith(
                List.of(enItem),
                configMapWith("en_US", enConfig),
                "en_US");

        assertEquals("An error has occurred", translator.translate("EN_US", "error.generic", Map.of()));
        assertEquals("An error has occurred", translator.translate("en_us", "error.generic", Map.of()));
    }

    @Test
    void translateUnknownLocaleFallsBackToFallbackLanguage() {
        ResourceTranslator translator = translatorWith(
                List.of(enItem, viItem),
                new HashMap<>(Map.of("en_US", enConfig, "vi_VN", viConfig)),
                "en_US");

        // "fr_FR" is not registered — must fall back to en_US
        assertEquals("An error has occurred", translator.translate("fr_FR", "error.generic", Map.of()));
    }

    @Test
    void translateUnknownLocaleAndFallbackNotInItemsLogsWarningAndReturnsKey() {
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));

        ResourceTranslator translator = translatorWith(
                List.of(enItem),
                configMapWith("en_US", enConfig),
                "fr_FR"); // fallback locale is also not in items

        String result = translator.translate("zh_CN", "error.generic", Map.of());

        assertEquals("error.generic", result);
        verify(plugin).getLogger();
    }

    // --- key resolution tests ---

    @Test
    void translateMissingKeyReturnsKeyAsDefault() {
        ResourceTranslator translator = translatorWith(
                List.of(enItem),
                configMapWith("en_US", enConfig),
                "en_US");

        assertEquals("error.nonexistent", translator.translate("en_US", "error.nonexistent", Map.of()));
    }

    // --- config caching test ---

    @Test
    void translateConfigIsLoadedOnlyOnceForSameLocale() {
        Map<String, TranslationConfig> configMap = spy(new HashMap<>());
        configMap.put("en_US", enConfig);

        ResourceTranslator translator = translatorWith(List.of(enItem), configMap, "en_US");

        translator.translate("en_US", "error.generic", Map.of());
        translator.translate("en_US", "error.unknown", Map.of());

        verify(configMap, times(2)).computeIfAbsent(eq("en_US"), any());
    }

    // --- computeIfAbsent lambda tests (config not yet cached) ---

    @Test
    void translateComputeIfAbsentLoadsConfigFromDiskAndResource() {
        String resourceYml = "error:\n  generic: \"An error has occurred\"\n  unknown: \"Unknown error\"\n";
        InputStream resourceStream = new ByteArrayInputStream(resourceYml.getBytes(StandardCharsets.UTF_8));
        when(plugin.getResource("i18n/en_US.yml")).thenReturn(resourceStream);

        ResourceTranslator translator = translatorWith(
                List.of(enItem),
                new HashMap<>(),  // empty map — forces computeIfAbsent
                "en_US");

        String result = translator.translate("en_US", "error.generic", Map.of());
        assertEquals("An error has occurred", result);
    }

    @Test
    void translateComputeIfAbsentPatchesMissingKeysFromResource() throws IOException {
        // Disk file has only 1 key, resource has 2 keys
        Path diskFile = tempDir.resolve("patch_test.yml");
        Files.writeString(diskFile, "error:\n  generic: \"On disk\"\n");

        String resourceYml = "error:\n  generic: \"From resource\"\n  unknown: \"New key\"\n";
        InputStream resourceStream = new ByteArrayInputStream(resourceYml.getBytes(StandardCharsets.UTF_8));
        when(plugin.getResource("i18n/patch_test.yml")).thenReturn(resourceStream);

        TranslationMetadataItem item = TranslationMetadataItem.builder()
                .locale("en_US")
                .path(diskFile)
                .resourcePath("i18n/patch_test.yml")
                .build();

        long modBefore = Files.getLastModifiedTime(diskFile).toMillis();

        ResourceTranslator translator = translatorWith(
                List.of(item),
                new HashMap<>(),
                "en_US");

        // First call triggers computeIfAbsent which loads, patches, and saves
        String result = translator.translate("en_US", "error.unknown", Map.of());
        assertEquals("New key", result);

        // File should have been modified by the patch+save
        long modAfter = Files.getLastModifiedTime(diskFile).toMillis();
        assertTrue(modAfter > modBefore, "Disk file should have been saved with new keys");
    }

    @Test
    void translateComputeIfAbsentSkipsSaveWhenKeysMatch() throws IOException {
        Path diskFile = tempDir.resolve("no_patch_test.yml");
        Files.writeString(diskFile, "error:\n  generic: \"Foo\"\n");

        String resourceYml = "error:\n  generic: \"Bar\"\n";
        InputStream resourceStream = new ByteArrayInputStream(resourceYml.getBytes(StandardCharsets.UTF_8));
        when(plugin.getResource("i18n/no_patch_test.yml")).thenReturn(resourceStream);

        TranslationMetadataItem item = TranslationMetadataItem.builder()
                .locale("en_US")
                .path(diskFile)
                .resourcePath("i18n/no_patch_test.yml")
                .build();

        long modBefore = Files.getLastModifiedTime(diskFile).toMillis();

        ResourceTranslator translator = translatorWith(List.of(item), new HashMap<>(), "en_US");
        translator.translate("en_US", "error.generic", Map.of());

        // Keys match — patchConfigureFile should return early without save
        long modAfter = Files.getLastModifiedTime(diskFile).toMillis();
        assertEquals(modBefore, modAfter, "Disk file should not be modified when keys match");
    }

}
