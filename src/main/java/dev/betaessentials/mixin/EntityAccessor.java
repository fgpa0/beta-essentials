package dev.betaessentials.mixin;

import net.minecraft.entity.EntityBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityBase.class)
public interface EntityAccessor {
    @Accessor("fallDistance")
    void ess$setFallDistance(float v);


    @Accessor("field_1635")
    float ess$getWalkDistance();

    @Accessor("field_1611")
    int ess$getNextStep();

    @Accessor("field_1611")
    void ess$setNextStep(int v);
}
