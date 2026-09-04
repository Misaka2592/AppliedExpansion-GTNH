package com.misaka2592.appliedexpansion.mixins.late;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.misaka2592.appliedexpansion.crafting.CraftingCpuEntryOrder;

import appeng.container.implementations.CraftingCpuEntry;

@Mixin(targets = "appeng.client.gui.implementations.GuiCraftingCPU$CraftingCpuVisualState", remap = false)
public abstract class MixinCraftingCpuVisualState {

    @Shadow
    public abstract List<CraftingCpuEntry> filteredEntries();

    @Inject(method = "rebuildFilteredEntries", at = @At("TAIL"), remap = false)
    private void appliedExpansion$sortByItemState(boolean hideStored, String search, CallbackInfo callbackInfo) {
        CraftingCpuEntryOrder
            .sortByState(filteredEntries(), CraftingCpuEntry::getActiveAmount, CraftingCpuEntry::getPendingAmount);
    }
}
