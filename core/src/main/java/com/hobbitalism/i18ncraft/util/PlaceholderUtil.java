package com.hobbitalism.i18ncraft.util;

import com.hobbitalism.i18ncraft.SimplePlaceholderProcessor;
import com.hobbitalism.i18ncraft.api.PlaceholderProcessor;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PlaceholderUtil {

    private static final PlaceholderProcessor DEFAULT_PROCESSOR = new SimplePlaceholderProcessor();

    public static PlaceholderProcessor defaultProcessor() {
        return DEFAULT_PROCESSOR;
    }
}
