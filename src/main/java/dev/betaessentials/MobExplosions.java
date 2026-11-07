package dev.betaessentials;

/** Holder for the helper shared by the creeper and fireball mixins. */
public final class MobExplosions {
    private MobExplosions() {}

    /** An explosion that hurts entities but (with mobGriefing off) leaves blocks alone. */
    public static net.minecraft.sortme.Explosion explode(net.minecraft.level.Level level, net.minecraft.entity.EntityBase cause,
                                                        double x, double y, double z, float power, boolean fire) {
        boolean grief = level.isServerSide || dev.betaessentials.Ess.mc == null || dev.betaessentials.Ess.mc.level != level
                || dev.betaessentials.GameRules.get(dev.betaessentials.GameRules.MOB_GRIEFING);
        if (grief) return level.createExplosion(cause, x, y, z, power, fire);
        net.minecraft.sortme.Explosion e = new net.minecraft.sortme.Explosion(level, cause, x, y, z, power);
        e.causeFires = false;
        e.kaboomPhase1();
        e.damagedTiles.clear();
        e.kaboomPhase2(true);
        return e;
    }
}
