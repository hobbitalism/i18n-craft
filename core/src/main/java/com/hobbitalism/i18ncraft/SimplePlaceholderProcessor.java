package com.hobbitalism.i18ncraft;

import com.hobbitalism.i18ncraft.api.PlaceholderProcessor;

import java.util.Map;
import java.util.Objects;

/**
 * A simple sliding-window implementation of {@link PlaceholderProcessor} that uses
 * PlaceholderAPI-style {@code %key%} delimiters.
 *
 * <p>This processor scans the input text character by character. When an opening
 * {@code '%'} is encountered, it slides forward to find the matching closing
 * {@code '%'}. The key between them is looked up in the provided placeholder map;
 * if a mapping exists, it is substituted; otherwise the original {@code %key%}
 * token is preserved unchanged.</p>
 *
 * <p>If an opening {@code '%'} has no matching closing {@code '%'}, the character
 * is treated as a literal and appended as-is.</p>
 */
public class SimplePlaceholderProcessor implements PlaceholderProcessor {

    private static final char PLACEHOLDER_SEPARATOR = '%';

    @Override
    public String transform(String originalText, Map<String, String> placeholders) {
        Objects.requireNonNull(originalText, "originalText must not be null");

        if (placeholders == null || placeholders.isEmpty()) {
            return originalText;
        }

        int length = originalText.length();
        StringBuilder result = new StringBuilder(length + 32);
        int i = 0;

        while (i < length) {
            char c = originalText.charAt(i);

            if (c == PLACEHOLDER_SEPARATOR) {
                int end = i + 1;
                while (end < length && originalText.charAt(end) != PLACEHOLDER_SEPARATOR) {
                    end++;
                }

                if (end < length) {
                    String key = originalText.substring(i + 1, end);
                    String value = placeholders.get(key);

                    if (value != null || placeholders.containsKey(key)) {
                        result.append(value != null ? value : "");
                    } else {
                        result.append(originalText, i, end + 1);
                    }
                    i = end + 1;
                } else {
                    result.append(c);
                    i++;
                }
            } else {
                result.append(c);
                i++;
            }
        }

        return result.toString();
    }
}