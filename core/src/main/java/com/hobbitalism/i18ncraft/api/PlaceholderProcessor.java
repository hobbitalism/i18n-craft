package com.hobbitalism.i18ncraft.api;

import java.util.Map;

public interface PlaceholderProcessor {

    String transform(String originalText, Map<String, String> placeholders);

}
