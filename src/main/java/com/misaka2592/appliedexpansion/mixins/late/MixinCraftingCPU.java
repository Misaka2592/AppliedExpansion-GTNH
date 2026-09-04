package com.misaka2592.appliedexpansion.mixins.late;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.api.storage.data.IAEStack;
import appeng.api.util.NamedDimensionalCoord;
import appeng.client.gui.implementations.GuiCraftingCPU;

@Mixin(value = GuiCraftingCPU.class, remap = false)
public abstract class MixinCraftingCPU {

    @Shadow
    private List<NamedDimensionalCoord> hoveredInterfaceLocations;

    @Inject(method = "addItemTooltip", at = @At("HEAD"), remap = false)
    private void appliedExpansion$clearStaleInterfaceLocations(IAEStack<?> stack, List<String> tooltipLines,
        boolean stackChanged, CallbackInfo callbackInfo) {
        if (stackChanged) {
            hoveredInterfaceLocations = null;
        }
    }
}
