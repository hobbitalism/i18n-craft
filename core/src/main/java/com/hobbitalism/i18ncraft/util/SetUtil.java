package com.hobbitalism.i18ncraft.util;

import lombok.experimental.UtilityClass;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@UtilityClass
public class SetUtil {

    /**
     * Returns a new {@link Set} containing all elements from {@code setA} that are not present
     * in {@code setB}. Neither input set is modified.
     *
     * @param <T>  the type of elements in the sets
     * @param setA the source set, must not be null
     * @param setB the set whose elements are to be excluded, must not be null
     * @return a new set containing the difference ({@code setA} minus {@code setB})
     * @throws NullPointerException if either parameter is null
     */
    public static <T> Set<T> difference(Set<T> setA, Set<T> setB) {
        Objects.requireNonNull(setA);
        Objects.requireNonNull(setB);

        Set<T> result = new HashSet<>(setA);
        result.removeAll(setB);
        return result;
    }

}
