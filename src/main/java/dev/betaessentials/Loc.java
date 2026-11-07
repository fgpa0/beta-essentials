package dev.betaessentials;

import net.minecraft.util.io.CompoundTag;

/** A saved position: feet coordinates, look direction and dimension. */
public final class Loc {
    public final double x, y, z;
    public final float yaw, pitch;
    public final int dim;

    public Loc(double x, double y, double z, float yaw, float pitch, int dim) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.dim = dim;
    }

    public CompoundTag toTag() {
        CompoundTag t = new CompoundTag();
        t.put("x", x);
        t.put("y", y);
        t.put("z", z);
        t.put("yaw", yaw);
        t.put("pitch", pitch);
        t.put("dim", dim);
        return t;
    }

    public static Loc fromTag(CompoundTag t) {
        return new Loc(t.getDouble("x"), t.getDouble("y"), t.getDouble("z"), t.getFloat("yaw"), t.getFloat("pitch"), t.getInt("dim"));
    }

    public String describe() {
        return (int) Math.floor(x) + ", " + (int) Math.floor(y) + ", " + (int) Math.floor(z) + (dim == -1 ? " (Nether)" : "");
    }
}
