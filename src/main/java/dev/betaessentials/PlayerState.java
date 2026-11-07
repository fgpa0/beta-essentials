package dev.betaessentials;

import net.minecraft.client.PlayerKeypressManager;
import net.minecraft.entity.player.AbstractClientPlayer;
import net.minecraft.item.ItemInstance;
import org.lwjgl.input.Keyboard;

/** Flight, sprinting, game mode effects and keepInventory for the local player. */
public final class PlayerState {
    private PlayerState() {}

    public static boolean sprinting;
    private static boolean sprintByKey, wasSprinting;
    /** Two forward presses closer together than this (measured press to press, like modern Minecraft) start a sprint. */
    private static final long DOUBLE_TAP_NS = 250_000_000L;
    private static long firstTapNs;
    private static volatile boolean doubleTapped;

    private static long firstJumpNs;
    private static volatile boolean flightToggle;

    /** Jump key went down: two presses within the window toggle creative flight. */
    public static void jumpPressed() {
        long now = System.nanoTime();
        if (firstJumpNs != 0 && now - firstJumpNs <= DOUBLE_TAP_NS) {
            flightToggle = true;
            firstJumpNs = 0;
        } else {
            firstJumpNs = now;
        }
    }

    /** Forward key went down (from the keyboard event, so even very quick taps are seen). */
    public static void forwardPressed() {
        if (sprinting) return;
        long now = System.nanoTime();
        if (firstTapNs != 0 && now - firstTapNs <= DOUBLE_TAP_NS) {
            doubleTapped = true;
            firstTapNs = 0;
        } else {
            firstTapNs = now;
        }
    }
    private static float fov = 1F, prevFov = 1F;

    /** FOV multiplier for this frame. */
    public static float fovFactor(float delta) {
        if (!Ess.active()) return 1F;
        return prevFov + (fov - prevFov) * delta;
    }

    /** Sprint-jumping pushes you forward like in modern Minecraft. */
    public static void jumped(AbstractClientPlayer p) {
        if (!sprinting || flying()) return;
        float yaw = p.yaw * (float) Math.PI / 180F;
        p.velocityX -= Math.sin(yaw) * 0.2F;
        p.velocityZ += Math.cos(yaw) * 0.2F;
    }
    private static boolean lastJump, lastForward;
    private static int jumpReleasedTicks = 100, forwardReleasedTicks = 100;
    private static int tick;
    public static boolean killing;

    public static boolean flying() {
        EssData d = Ess.data();
        if (d == null) return false;
        if (d.mode == GameMode.SPECTATOR) return true;
        return d.mode == GameMode.CREATIVE && d.flying;
    }

    public static void setMode(GameMode m) {
        EssData d = Ess.data();
        if (d == null) return;
        d.mode = m;
        if (m == GameMode.SURVIVAL) d.flying = false;
        AbstractClientPlayer p = Ess.mc.player;
        if (p != null) {
            p.field_1642 = m == GameMode.SPECTATOR;
            ((dev.betaessentials.mixin.EntityAccessor) p).ess$setFallDistance(0);
            if (m != GameMode.SURVIVAL) {
                p.fire = 0;
                p.air = 300;
            }
        }
    }

    /** Once per tick, after the movement keys were read. */
    public static void input(PlayerKeypressManager in) {
        if (!Ess.active()) {
            sprinting = false;
            return;
        }
        AbstractClientPlayer p = Ess.mc.player;
        EssData d = Ess.data();
        tick++;
        boolean jump = in.jump;
        boolean forward = in.parallelMovement > 0;
        boolean spectator = d.mode == GameMode.SPECTATOR;
        p.field_1642 = spectator;

        // double-tap jump toggles flight in creative
        if (flightToggle) {
            flightToggle = false;
            if (d.mode == GameMode.CREATIVE) d.flying = !d.flying;
        }
        if (d.mode == GameMode.CREATIVE && d.flying && p.onGround && !jump) d.flying = false;

        // sprinting: double-tap forward or press Ctrl; either lasts until you stop moving forward, sneak or hit a wall
        boolean allowed = GameRules.get(GameRules.ALLOW_SPRINTING) || flying();
        boolean ctrl = Ess.mc.currentScreen == null && (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL));
        boolean canStart = allowed && forward && !(in.sneak && !flying()) && !p.isSleeping();
        if (doubleTapped) {
            doubleTapped = false;
            if (canStart && !sprinting) {
                sprinting = true;
                sprintByKey = false;
            }
        }
        if (ctrl && canStart && !sprinting) {
            sprinting = true;
            sprintByKey = true;
        }
        if (!forward || !allowed || (in.sneak && !flying()) || (p.field_1624 && !flying()) || p.isSleeping()) sprinting = false;
        if (!sprinting) {
            sprintByKey = false;
            // the next forward press after a sprint ends is always a fresh first tap
            if (wasSprinting) firstTapNs = 0;
        }
        wasSprinting = sprinting;

        // FOV: modern sprinting widens the view by 15% (flying by 10%), eased in and out
        prevFov = fov;
        float target = (sprinting ? 1.15F : 1F) * (flying() ? 1.1F : 1F);
        fov += (target - fov) * 0.5F;

        lastJump = jump;
        lastForward = forward;

        // sneaking slows walking to 30%; while flying, sneak means "down" and should not slow you
        if (flying() && in.sneak) {
            in.perpendicularMovement /= 0.3F;
            in.parallelMovement /= 0.3F;
        }
    }

    /** Replaces Living.travel for the flying player. */
    public static void fly(AbstractClientPlayer p, float strafe, float forward) {
        EssData d = Ess.data();
        float speed = 0.05F * d.flySpeed * (sprinting ? 2F : 1F);
        p.movementInputToVelocity(strafe, forward, speed);
        PlayerKeypressManager in = p.playerKeypressManager;
        int dir = (in.jump ? 1 : 0) - (in.sneak ? 1 : 0);
        p.velocityY = dir * 0.375 * Math.max(1, d.flySpeed * 0.5);
        dev.betaessentials.mixin.EntityAccessor steps = (dev.betaessentials.mixin.EntityAccessor) p;
        steps.ess$setNextStep((int) steps.ess$getWalkDistance() + 100000); // no footsteps while flying
        p.move(p.velocityX, p.velocityY, p.velocityZ);
        steps.ess$setNextStep((int) steps.ess$getWalkDistance());
        p.velocityX *= 0.91;
        p.velocityZ *= 0.91;
        p.velocityY = 0;
        dev.betaessentials.mixin.EntityAccessor acc = (dev.betaessentials.mixin.EntityAccessor) p;
        acc.ess$setFallDistance(0);
        catchUpSteps(acc);
    }

    /**
     * Vanilla counts walked distance even in the air, but only plays a footstep (one per tick) when there is a block
     * underfoot, so after flying the backlog came out as hundreds of footsteps. Skip the backlog instead.
     */
    public static void catchUpSteps(dev.betaessentials.mixin.EntityAccessor acc) {
        int behind = (int) acc.ess$getWalkDistance();
        if (behind > acc.ess$getNextStep()) acc.ess$setNextStep(behind);
    }

    /** Ground speed multiplier for sprinting. */
    public static float sprintFactor(Object entity) {
        return sprinting && Ess.isLocalPlayer(entity) && !flying() ? 1.3F : 1F;
    }

    public static void kill() {
        AbstractClientPlayer p = Ess.mc.player;
        killing = true;
        try {
            p.damage(null, 1000);
        } finally {
            killing = false;
        }
    }

    // ---- keepInventory

    private static ItemInstance[] keptMain, keptArmour;

    public static void keepInventory(AbstractClientPlayer p) {
        keptMain = copy(p.inventory.main);
        keptArmour = copy(p.inventory.armour);
        java.util.Arrays.fill(p.inventory.main, null);
        java.util.Arrays.fill(p.inventory.armour, null);
    }

    public static void afterRespawn(AbstractClientPlayer p) {
        sprinting = false;
        if (keptMain == null || p == null) return;
        System.arraycopy(keptMain, 0, p.inventory.main, 0, Math.min(keptMain.length, p.inventory.main.length));
        System.arraycopy(keptArmour, 0, p.inventory.armour, 0, Math.min(keptArmour.length, p.inventory.armour.length));
        keptMain = keptArmour = null;
    }

    public static void resetWorld() {
        keptMain = keptArmour = null;
        sprinting = false;
        fov = prevFov = 1F;
    }

    private static ItemInstance[] copy(ItemInstance[] a) {
        ItemInstance[] c = new ItemInstance[a.length];
        System.arraycopy(a, 0, c, 0, a.length);
        return c;
    }
}
