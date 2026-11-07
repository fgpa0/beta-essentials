package dev.betaessentials.mixin;

import net.minecraft.level.dimension.DimensionFile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.io.File;

@Mixin(DimensionFile.class)
public interface DimensionFileAccessor {
    @Accessor("parentFolder")
    File ess$folder();
}
