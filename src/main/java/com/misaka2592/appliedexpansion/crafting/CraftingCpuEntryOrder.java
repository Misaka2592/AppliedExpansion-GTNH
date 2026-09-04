package com.misaka2592.appliedexpansion.crafting;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToLongFunction;

public final class CraftingCpuEntryOrder {

    private CraftingCpuEntryOrder() {}

    public static <T> void sortByState(List<T> entries, ToLongFunction<? super T> inProgressAmount,
        ToLongFunction<? super T> pendingAmount) {
        Collections.sort(entries, Comparator.comparingInt(entry -> {
            if (inProgressAmount.applyAsLong(entry) > 0) {
                return 0;
            }
            if (pendingAmount.applyAsLong(entry) > 0) {
                return 1;
            }
            return 2;
        }));
    }
}
