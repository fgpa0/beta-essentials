package dev.betaessentials.mixin;

import net.minecraft.entity.EntityBase;
import net.minecraft.entity.projectile.Fireball;
import net.minecraft.level.Level;
import net.minecraft.sortme.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Fireball.class)
public abstract class FireballMixin {
    @Redirect(method = "tick()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/level/Level;createExplosion(Lnet/minecraft/entity/EntityBase;DDDFZ)Lnet/minecraft/sortme/Explosion;"))
    private Explosion ess$explode(Level level, EntityBase cause, double x, double y, double z, float power, boolean fire) {
        return dev.betaessentials.MobExplosions.explode(level, cause, x, y, z, power, fire);
    }
}
