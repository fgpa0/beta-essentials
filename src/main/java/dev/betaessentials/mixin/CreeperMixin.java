package dev.betaessentials.mixin;

import net.minecraft.entity.EntityBase;
import net.minecraft.entity.monster.Creeper;
import net.minecraft.level.Level;
import net.minecraft.sortme.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Creeper.class)
public abstract class CreeperMixin {
    @Redirect(method = "tryAttack(Lnet/minecraft/entity/EntityBase;F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/level/Level;createExplosion(Lnet/minecraft/entity/EntityBase;DDDF)Lnet/minecraft/sortme/Explosion;"))
    private Explosion ess$explode(Level level, EntityBase cause, double x, double y, double z, float power) {
        return dev.betaessentials.MobExplosions.explode(level, cause, x, y, z, power, false);
    }
}
