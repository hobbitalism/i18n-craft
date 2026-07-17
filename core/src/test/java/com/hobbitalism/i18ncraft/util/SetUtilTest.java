package com.hobbitalism.i18ncraft.util;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SetUtilTest {

    // --- null-guard tests ---

    @Test
    void differenceNullSetAThrowsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> SetUtil.difference(null, Set.of("a")));
    }

    @Test
    void differenceNullSetBThrowsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> SetUtil.difference(Set.of("a"), null));
    }

    // --- happy path tests ---

    @Test
    void differenceDisjointSetsReturnsSetA() {
        Set<String> result = SetUtil.difference(Set.of("a", "b"), Set.of("c", "d"));
        assertEquals(Set.of("a", "b"), result);
    }

    @Test
    void differenceIdenticalSetsReturnsEmptySet() {
        Set<String> result = SetUtil.difference(Set.of("a", "b"), Set.of("a", "b"));
        assertTrue(result.isEmpty());
    }

    @Test
    void differencePartialOverlapReturnsOnlyUniqueElementsFromSetA() {
        Set<String> result = SetUtil.difference(Set.of("a", "b", "c"), Set.of("b", "c", "d"));
        assertEquals(Set.of("a"), result);
    }

    @Test
    void differenceEmptySetAReturnsEmptySet() {
        Set<String> result = SetUtil.difference(Set.of(), Set.of("a", "b"));
        assertTrue(result.isEmpty());
    }

    @Test
    void differenceEmptySetBReturnsSetA() {
        Set<String> result = SetUtil.difference(Set.of("a", "b"), Set.of());
        assertEquals(Set.of("a", "b"), result);
    }

    @Test
    void differenceBothEmptyReturnsEmptySet() {
        Set<String> result = SetUtil.difference(Set.of(), Set.of());
        assertTrue(result.isEmpty());
    }

    // --- does not mutate inputs ---

    @Test
    void differenceDoesNotMutateSetA() {
        Set<String> setA = new java.util.HashSet<>(Set.of("a", "b", "c"));
        Set<String> setB = Set.of("b");
        SetUtil.difference(setA, setB);
        assertEquals(Set.of("a", "b", "c"), setA);
    }

}
