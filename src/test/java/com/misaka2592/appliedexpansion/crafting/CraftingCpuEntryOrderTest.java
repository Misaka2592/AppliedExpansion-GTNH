package com.misaka2592.appliedexpansion.crafting;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class CraftingCpuEntryOrderTest {

    @Test
    public void inProgressItemsComeBeforePendingItems() {
        List<TestItemEntry> entries = new ArrayList<>(
            Arrays.asList(new TestItemEntry("pending", 0, 1), new TestItemEntry("in-progress", 1, 0)));

        CraftingCpuEntryOrder.sortByState(entries, TestItemEntry::activeAmount, TestItemEntry::pendingAmount);

        assertEquals(Arrays.asList("in-progress", "pending"), entryNames(entries));
    }

    @Test
    public void pendingItemsComeBeforeExistingItems() {
        List<TestItemEntry> entries = new ArrayList<>(
            Arrays.asList(new TestItemEntry("existing", 0, 0), new TestItemEntry("pending", 0, 1)));

        CraftingCpuEntryOrder.sortByState(entries, TestItemEntry::activeAmount, TestItemEntry::pendingAmount);

        assertEquals(Arrays.asList("pending", "existing"), entryNames(entries));
    }

    @Test
    public void completedAndExistingItemsSharePriorityAndKeepInputOrder() {
        List<TestItemEntry> entries = new ArrayList<>(
            Arrays.asList(
                new TestItemEntry("completed-first", 0, 0),
                new TestItemEntry("existing-second", 0, 0),
                new TestItemEntry("completed-third", 0, 0)));

        CraftingCpuEntryOrder.sortByState(entries, TestItemEntry::activeAmount, TestItemEntry::pendingAmount);

        assertEquals(Arrays.asList("completed-first", "existing-second", "completed-third"), entryNames(entries));
    }

    @Test
    public void eachItemStateKeepsItsInputOrder() {
        List<TestItemEntry> entries = new ArrayList<>(
            Arrays.asList(
                new TestItemEntry("existing-first", 0, 0),
                new TestItemEntry("in-progress-first", 2, 0),
                new TestItemEntry("pending-first", 0, 3),
                new TestItemEntry("in-progress-second", 1, 4),
                new TestItemEntry("existing-second", 0, 0),
                new TestItemEntry("pending-second", 0, 1)));

        CraftingCpuEntryOrder.sortByState(entries, TestItemEntry::activeAmount, TestItemEntry::pendingAmount);

        assertEquals(
            Arrays.asList(
                "in-progress-first",
                "in-progress-second",
                "pending-first",
                "pending-second",
                "existing-first",
                "existing-second"),
            entryNames(entries));
    }

    @Test
    public void highestItemStateWinsWhenAmountsAreMixed() {
        List<TestItemEntry> entries = new ArrayList<>(
            Arrays.asList(
                new TestItemEntry("existing", 0, 0),
                new TestItemEntry("pending-with-storage", 0, 2),
                new TestItemEntry("in-progress-with-pending-and-storage", 1, 3)));

        CraftingCpuEntryOrder.sortByState(entries, TestItemEntry::activeAmount, TestItemEntry::pendingAmount);

        assertEquals(
            Arrays.asList("in-progress-with-pending-and-storage", "pending-with-storage", "existing"),
            entryNames(entries));
    }

    @Test
    public void itemAndFluidEntriesUseTheSameStatePriority() {
        List<TestItemEntry> entries = new ArrayList<>(
            Arrays.asList(
                new TestItemEntry("item-existing", 0, 0),
                new TestItemEntry("fluid-pending", 0, 1),
                new TestItemEntry("item-in-progress", 1, 0),
                new TestItemEntry("fluid-in-progress", 2, 0)));

        CraftingCpuEntryOrder.sortByState(entries, TestItemEntry::activeAmount, TestItemEntry::pendingAmount);

        assertEquals(
            Arrays.asList("item-in-progress", "fluid-in-progress", "fluid-pending", "item-existing"),
            entryNames(entries));
    }

    @Test
    public void changedItemStateMovesEntryOnTheNextSort() {
        TestItemEntry first = new TestItemEntry("first", 1, 0);
        TestItemEntry second = new TestItemEntry("second", 0, 1);
        List<TestItemEntry> entries = new ArrayList<>(Arrays.asList(first, second));
        CraftingCpuEntryOrder.sortByState(entries, TestItemEntry::activeAmount, TestItemEntry::pendingAmount);

        first.setAmounts(0, 0);
        second.setAmounts(1, 0);
        CraftingCpuEntryOrder.sortByState(entries, TestItemEntry::activeAmount, TestItemEntry::pendingAmount);

        assertEquals(Arrays.asList("second", "first"), entryNames(entries));
    }

    @Test
    public void emptyCraftingOrderRemainsEmpty() {
        List<TestItemEntry> entries = new ArrayList<>();

        CraftingCpuEntryOrder.sortByState(entries, TestItemEntry::activeAmount, TestItemEntry::pendingAmount);

        assertEquals(new ArrayList<>(), entries);
    }

    @Test
    public void singleItemCraftingOrderKeepsItsEntry() {
        TestItemEntry only = new TestItemEntry("only", 0, 1);
        List<TestItemEntry> entries = new ArrayList<>(Arrays.asList(only));

        CraftingCpuEntryOrder.sortByState(entries, TestItemEntry::activeAmount, TestItemEntry::pendingAmount);

        assertEquals(Arrays.asList(only), entries);
    }

    @Test
    public void allSameStateCraftingOrderKeepsInputOrder() {
        List<TestItemEntry> entries = new ArrayList<>(
            Arrays.asList(
                new TestItemEntry("first", 0, 2),
                new TestItemEntry("second", 0, 1),
                new TestItemEntry("third", 0, 3)));

        CraftingCpuEntryOrder.sortByState(entries, TestItemEntry::activeAmount, TestItemEntry::pendingAmount);

        assertEquals(Arrays.asList("first", "second", "third"), entryNames(entries));
    }

    private static List<String> entryNames(List<TestItemEntry> entries) {
        List<String> names = new ArrayList<>();
        for (TestItemEntry entry : entries) {
            names.add(entry.name);
        }
        return names;
    }

    private static final class TestItemEntry {

        private final String name;
        private long activeAmount;
        private long pendingAmount;

        private TestItemEntry(String name, long activeAmount, long pendingAmount) {
            this.name = name;
            this.activeAmount = activeAmount;
            this.pendingAmount = pendingAmount;
        }

        private long activeAmount() {
            return activeAmount;
        }

        private long pendingAmount() {
            return pendingAmount;
        }

        private void setAmounts(long activeAmount, long pendingAmount) {
            this.activeAmount = activeAmount;
            this.pendingAmount = pendingAmount;
        }
    }
}
