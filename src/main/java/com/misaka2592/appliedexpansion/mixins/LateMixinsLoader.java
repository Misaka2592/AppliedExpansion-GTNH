package com.misaka2592.appliedexpansion.mixins;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

import cpw.mods.fml.relauncher.FMLLaunchHandler;

@LateMixin
public final class LateMixinsLoader implements ILateMixinLoader {

    private static final String AE2_MOD_ID = "appliedenergistics2";
    private static final List<String> CLIENT_MIXINS = Arrays.asList("MixinCraftingCPU", "MixinCraftingCpuVisualState");

    @Override
    public String getMixinConfig() {
        return "mixins.appliedexpansion.late.json";
    }

    @Nonnull
    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        if (FMLLaunchHandler.side()
            .isClient() && loadedMods.contains(AE2_MOD_ID)) {
            return CLIENT_MIXINS;
        }
        return Collections.emptyList();
    }
}
