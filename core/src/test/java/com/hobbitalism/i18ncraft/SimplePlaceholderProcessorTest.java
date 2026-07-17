package com.hobbitalism.i18ncraft;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SimplePlaceholderProcessorTest {

    private final SimplePlaceholderProcessor processor = new SimplePlaceholderProcessor();

    @Test
    void transformNullOriginalTextThrowsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> processor.transform(null, Map.of("player", "Alex")));
    }

    @Test
    void transformNullPlaceholdersReturnsOriginalText() {
        String text = "Hello %player%";
        assertEquals(text, processor.transform(text, null));
    }

    @Test
    void transformEmptyPlaceholdersReturnsOriginalText() {
        String text = "Hello %player%";
        assertEquals(text, processor.transform(text, Map.of()));
    }

    @Test
    void transformSinglePlaceholderReplaced() {
        String result = processor.transform("Hello %player%", Map.of("player", "Alex"));
        assertEquals("Hello Alex", result);
    }

    @Test
    void transformPlaceholderAtStart() {
        String result = processor.transform("%player% joined", Map.of("player", "Alex"));
        assertEquals("Alex joined", result);
    }

    @Test
    void transformPlaceholderAtEnd() {
        String result = processor.transform("Welcome %player%", Map.of("player", "Alex"));
        assertEquals("Welcome Alex", result);
    }

    @Test
    void transformMultiplePlaceholders() {
        String result = processor.transform("%player% has %coins% coins", Map.of("player", "Alex", "coins", "42"));
        assertEquals("Alex has 42 coins", result);
    }

    @Test
    void transformConsecutivePlaceholders() {
        String result = processor.transform("%a%%b%", Map.of("a", "1", "b", "2"));
        assertEquals("12", result);
    }

    @Test
    void transformUnknownPlaceholderPreserved() {
        String result = processor.transform("Hello %unknown%", Map.of("player", "Alex"));
        assertEquals("Hello %unknown%", result);
    }

    @Test
    void transformUnmatchedSeparatorTreatedAsLiteral() {
        String text = "50% off";
        assertEquals(text, processor.transform(text, Map.of("off", "sale")));
    }

    @Test
    void transformEmptyKeyBetweenSeparators() {
        String result = processor.transform("Hello %%", Map.of("", "world"));
        assertEquals("Hello world", result);
    }

    @Test
    void transformTextWithoutAnySeparatorUnchanged() {
        String text = "Hello world";
        assertEquals(text, processor.transform(text, Map.of("player", "Alex")));
    }

    @Test
    void transformPlaceholderValueIsNullUsesEmptyString() {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", null);
        String result = processor.transform("Hello %player%", placeholders);
        assertEquals("Hello ", result);
    }

    @Test
    void transformMixedKnownAndUnknownPlaceholders() {
        String result = processor.transform("%a% %b% %c%", Map.of("a", "1", "c", "3"));
        assertEquals("1 %b% 3", result);
    }
}
