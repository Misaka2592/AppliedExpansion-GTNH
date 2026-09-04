package com.misaka2592.appliedexpansion.crafting;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class CraftingCpuEntryOrderTest {

    @Test
    public void inProgressEntriesComeBeforePendingEntries() {
        List<TestCraftingEntry> entries = new ArrayList<>(
            Arrays.asList(new TestCraftingEntry("pending", 0, 1), new TestCraftingEntry("in-progress", 1, 0)));

        CraftingCpuEntryOrder
            .sortByState(entries, TestCraftingEntry::inProgressAmount, TestCraftingEntry::pendingAmount);

        assertEquals(Arrays.asList("in-progress", "pending"), entryNames(entries));
    }

    @Test
    public void pendingEntriesComeBeforeExistingEntries() {
        List<TestCraftingEntry> entries = new ArrayList<>(
            Arrays.asList(new TestCraftingEntry("existing", 0, 0), new TestCraftingEntry("pending", 0, 1)));

        CraftingCpuEntryOrder
            .sortByState(entries, TestCraftingEntry::inProgressAmount, TestCraftingEntry::pendingAmount);

        assertEquals(Arrays.asList("pending", "existing"), entryNames(entries));
    }

    @Test
    public void completedAndExistingEntriesSharePriorityAndKeepInputOrder() {
        List<TestCraftingEntry> entries = new ArrayList<>(
            Arrays.asList(
                new TestCraftingEntry("completed-first", 0, 0),
                new TestCraftingEntry("existing-second", 0, 0),
                new TestCraftingEntry("completed-third", 0, 0)));

        CraftingCpuEntryOrder
            .sortByState(entries, TestCraftingEntry::inProgressAmount, TestCraftingEntry::pendingAmount);

        assertEquals(Arrays.asList("completed-first", "existing-second", "completed-third"), entryNames(entries));
    }

    @Test
    public void eachEntryStateKeepsItsInputOrder() {
        List<TestCraftingEntry> entries = new ArrayList<>(
            Arrays.asList(
                new TestCraftingEntry("existing-first", 0, 0),
                new TestCraftingEntry("in-progress-first", 2, 0),
                new TestCraftingEntry("pending-first", 0, 3),
                new TestCraftingEntry("in-progress-second", 1, 4),
                new TestCraftingEntry("existing-second", 0, 0),
                new TestCraftingEntry("pending-second", 0, 1)));

        CraftingCpuEntryOrder
            .sortByState(entries, TestCraftingEntry::inProgressAmount, TestCraftingEntry::pendingAmount);

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
    public void highestEntryStateWinsWhenAmountsAreMixed() {
        List<TestCraftingEntry> entries = new ArrayList<>(
            Arrays.asList(
                new TestCraftingEntry("existing", 0, 0),
                new TestCraftingEntry("pending-with-storage", 0, 2),
                new TestCraftingEntry("in-progress-with-pending-and-storage", 1, 3)));

        CraftingCpuEntryOrder
            .sortByState(entries, TestCraftingEntry::inProgressAmount, TestCraftingEntry::pendingAmount);

        assertEquals(
            Arrays.asList("in-progress-with-pending-and-storage", "pending-with-storage", "existing"),
            entryNames(entries));
    }

    @Test
    public void itemAndFluidEntriesUseTheSameStatePriority() {
        List<TestCraftingEntry> entries = new ArrayList<>(
            Arrays.asList(
                new TestCraftingEntry("item-existing", 0, 0),
                new TestCraftingEntry("fluid-pending", 0, 1),
                new TestCraftingEntry("item-in-progress", 1, 0),
                new TestCraftingEntry("fluid-in-progress", 2, 0)));

        CraftingCpuEntryOrder
            .sortByState(entries, TestCraftingEntry::inProgressAmount, TestCraftingEntry::pendingAmount);

        assertEquals(
            Arrays.asList("item-in-progress", "fluid-in-progress", "fluid-pending", "item-existing"),
            entryNames(entries));
    }

    @Test
    public void changedEntryStateMovesEntryOnTheNextSort() {
        TestCraftingEntry first = new TestCraftingEntry("first", 1, 0);
        TestCraftingEntry second = new TestCraftingEntry("second", 0, 1);
        List<TestCraftingEntry> entries = new ArrayList<>(Arrays.asList(first, second));
        CraftingCpuEntryOrder
            .sortByState(entries, TestCraftingEntry::inProgressAmount, TestCraftingEntry::pendingAmount);

        first.setAmounts(0, 0);
        second.setAmounts(1, 0);
        CraftingCpuEntryOrder
            .sortByState(entries, TestCraftingEntry::inProgressAmount, TestCraftingEntry::pendingAmount);

        assertEquals(Arrays.asList("second", "first"), entryNames(entries));
    }

    @Test
    public void emptyCraftingOrderRemainsEmpty() {
        List<TestCraftingEntry> entries = new ArrayList<>();

        CraftingCpuEntryOrder
            .sortByState(entries, TestCraftingEntry::inProgressAmount, TestCraftingEntry::pendingAmount);

        assertEquals(new ArrayList<>(), entries);
    }

    @Test
    public void singleEntryCraftingOrderKeepsItsEntry() {
        TestCraftingEntry only = new TestCraftingEntry("only", 0, 1);
        List<TestCraftingEntry> entries = new ArrayList<>(Arrays.asList(only));

        CraftingCpuEntryOrder
            .sortByState(entries, TestCraftingEntry::inProgressAmount, TestCraftingEntry::pendingAmount);

        assertEquals(Arrays.asList(only), entries);
    }

    @Test
    public void allSameStateCraftingOrderKeepsInputOrder() {
        List<TestCraftingEntry> entries = new ArrayList<>(
            Arrays.asList(
                new TestCraftingEntry("first", 0, 2),
                new TestCraftingEntry("second", 0, 1),
                new TestCraftingEntry("third", 0, 3)));

        CraftingCpuEntryOrder
            .sortByState(entries, TestCraftingEntry::inProgressAmount, TestCraftingEntry::pendingAmount);

        assertEquals(Arrays.asList("first", "second", "third"), entryNames(entries));
    }

    private static List<String> entryNames(List<TestCraftingEntry> entries) {
        List<String> names = new ArrayList<>();
        for (TestCraftingEntry entry : entries) {
            names.add(entry.name);
        }
        return names;
    }

    private static final class TestCraftingEntry {

        private final String name;
        private long inProgressAmount;
        private long pendingAmount;

        private TestCraftingEntry(String name, long inProgressAmount, long pendingAmount) {
            this.name = name;
            this.inProgressAmount = inProgressAmount;
            this.pendingAmount = pendingAmount;
        }

        private long inProgressAmount() {
            return inProgressAmount;
        }

        private long pendingAmount() {
            return pendingAmount;
        }

        private void setAmounts(long inProgressAmount, long pendingAmount) {
            this.inProgressAmount = inProgressAmount;
            this.pendingAmount = pendingAmount;
        }
    }
}
