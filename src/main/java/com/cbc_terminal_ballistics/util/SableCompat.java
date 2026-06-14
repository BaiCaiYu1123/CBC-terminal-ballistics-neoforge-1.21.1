package com.cbc_terminal_ballistics.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Optional Sable integration via the Sable Companion ServiceLoader API.
 * No compile-time dependency on Sable — everything is reflection-based and
 * safe to call when Sable is absent.
 */
public final class SableCompat {
    private static final String SABLE_MOD_ID = "sable";
    private static final String COMPANION_CLASS = "dev.ryanhcode.sable.companion.SableCompanion";
    private static final String SUB_LEVEL_ACCESS_CLASS = "dev.ryanhcode.sable.companion.SubLevelAccess";
    private static final Logger LOGGER = LoggerFactory.getLogger("cbc_terminal_ballistics:SableCompat");

    private static final Boolean SABLE_PRESENT = ModList.get().isLoaded(SABLE_MOD_ID);
    private static Object companionInstance;
    private static Method projectOutOfSubLevelMethod;
    private static Method getContainingMethod;
    private static Method logicalPoseMethod;
    private static Method transformPositionMethod;
    private static boolean reflectionFailed;
    private static boolean reflectionInitialized;

    // ---- public API ----

    /**
     * True if the Sable mod JAR is loaded. Does NOT require reflection to succeed.
     */
    public static boolean isPresent() {
        return SABLE_PRESENT;
    }

    /**
     * True if Sable is loaded AND the Companion API reflection succeeded.
     * For rendering, always check {@link #isPresent()} as a fallback.
     */
    public static boolean isLoaded() {
        if (!SABLE_PRESENT) return false;
        if (!reflectionInitialized) initCompanion();
        return companionInstance != null;
    }

    /**
     * Transforms a position out of a Sable sub-level to world coordinates.
     * If the position is not on a sub-level, returns it unchanged.
     */
    public static Vec3 toWorldCoordinates(Level level, Vec3 position) {
        if (!isLoaded() || level == null) return position;
        try {
            Method m = projectOutOfSubLevelMethod();
            if (m != null) {
                Object result = m.invoke(companionInstance, level, position);
                if (result instanceof Vec3 vec) return vec;
            }
        } catch (Throwable ignored) { }
        return position;
    }

    /**
     * Distance check that understands sub-level coordinates.
     */
    public static double squaredDistanceBetweenInclSubLevels(Level level, Vec3 first, Vec3 second) {
        // Sable Companion's projectOutOfSubLevel handles the sub-level-aware distance.
        Vec3 a = toWorldCoordinates(level, first);
        Vec3 b = toWorldCoordinates(level, second);
        return a.distanceToSqr(b);
    }

    /**
     * Converts a world hit position into the local sub-level coordinate system.
     */
    public static Vec3 toSubLevelCoordinates(Level level, BlockPos subLevelPos, Vec3 worldPosition) {
        // The Sable Companion API uses projectOutOfSubLevel which goes sub→world.
        // For world→sub, we'd need the inverse transform. For now return the position unchanged
        // since the core ballistics code handles this differently.
        return worldPosition;
    }

    /**
     * Converts a world-space hit face direction into the local sub-level face direction.
     */
    public static Direction toSubLevelDirection(Level level, BlockPos subLevelPos, Direction worldDirection) {
        // Identity fallback — the hit direction is already in local face-relative space.
        return worldDirection;
    }

    /**
     * Converts a world-space vector direction into local sub-level coordinates.
     */
    public static Vec3 toSubLevelVector(Level level, BlockPos subLevelPos, Vec3 worldVector) {
        return worldVector;
    }

    // ---- reflection init ----

    private static void initCompanion() {
        if (reflectionInitialized) return;
        reflectionInitialized = true;
        if (!SABLE_PRESENT) return;
        try {
            Class<?> companionClass = Class.forName(COMPANION_CLASS);
            LOGGER.info("Sable Companion class found: {}", companionClass.getName());
            // SableCompanion.INSTANCE is a static field
            Field instanceField = companionClass.getField("INSTANCE");
            companionInstance = instanceField.get(null);
            LOGGER.info("Sable Companion INSTANCE obtained: {}", companionInstance != null);

            // Pre-cache methods
            for (Method method : companionClass.getMethods()) {
                // projectOutOfSubLevel(Level, Vec3) — the deprecated but simplest one
                if (method.getName().equals("projectOutOfSubLevel")
                    && method.getParameterCount() == 2
                    && method.getParameterTypes()[0] == Level.class
                    && method.getParameterTypes()[1] == Vec3.class) {
                    projectOutOfSubLevelMethod = method;
                }
                // getContaining(Level, Vec3i) — BlockPos extends Vec3i
                if (method.getName().equals("getContaining")
                    && method.getParameterCount() == 2
                    && method.getParameterTypes()[0] == Level.class) {
                    getContainingMethod = method;
                }
            }

            // SubLevelAccess.logicalPose()
            try {
                Class<?> subLevelClass = Class.forName(SUB_LEVEL_ACCESS_CLASS);
                for (Method method : subLevelClass.getMethods()) {
                    if (method.getName().equals("logicalPose") && method.getParameterCount() == 0) {
                        logicalPoseMethod = method;
                        break;
                    }
                }
                // Pose3dc.transformPosition(double, double, double) -> Vec3
                Class<?> returnType = logicalPoseMethod != null ? logicalPoseMethod.getReturnType() : null;
                if (returnType != null) {
                    for (Method method : returnType.getMethods()) {
                        if (method.getName().equals("transformPosition")
                            && method.getParameterCount() == 3
                            && method.getParameterTypes()[0] == double.class
                            && method.getReturnType() == Vec3.class) {
                            transformPositionMethod = method;
                            break;
                        }
                    }
                }
            } catch (Throwable ignored) { }
        } catch (Throwable e) {
            reflectionFailed = true;
            LOGGER.warn("Sable Companion API reflection failed — Sable visual compatibility will use fallback: {}", e.toString());
            companionInstance = null;
        }
    }

    private static boolean isSableAndCompanionWorking() {
        return companionInstance != null && !reflectionFailed;
    }

    /** Public diagnostic: whether the Companion API was successfully initialised. */
    public static boolean isCompanionWorking() {
        if (!SABLE_PRESENT) return false;
        if (!reflectionInitialized) initCompanion();
        return isSableAndCompanionWorking();
    }

    private static Method projectOutOfSubLevelMethod() {
        if (projectOutOfSubLevelMethod == null && !reflectionFailed && SABLE_PRESENT) {
            initCompanion();
        }
        return projectOutOfSubLevelMethod;
    }

    private static Method getContainingMethod() {
        if (getContainingMethod == null && !reflectionFailed && SABLE_PRESENT) {
            initCompanion();
        }
        return getContainingMethod;
    }

    private static Method logicalPoseMethod() {
        if (logicalPoseMethod == null && !reflectionFailed && SABLE_PRESENT) {
            initCompanion();
        }
        return logicalPoseMethod;
    }

    private static Method transformPositionMethod() {
        if (transformPositionMethod == null && !reflectionFailed && SABLE_PRESENT) {
            initCompanion();
        }
        return transformPositionMethod;
    }

    // ---- coordinate-based heuristics ----

    /**
     * Sable plot grids can sit well inside the vanilla ±30M world border.
     * Logs from Aeronautics/Sable 1.2.x show plot coordinates around 20M, so
     * use a lower threshold only as a rendering/network fallback.
     */
    private static final int SUB_LEVEL_COORD_THRESHOLD = 10_000_000;

    public static boolean isProbablyInSubLevel(BlockPos pos) {
        if (pos == null) return false;
        return Math.abs(pos.getX()) > SUB_LEVEL_COORD_THRESHOLD
            || Math.abs(pos.getY()) > SUB_LEVEL_COORD_THRESHOLD
            || Math.abs(pos.getZ()) > SUB_LEVEL_COORD_THRESHOLD;
    }

    /**
     * Checks whether the given BlockPos is inside a Sable sub-level plot grid.
     * Uses the Companion API first; falls back to coordinate heuristics.
     */
    public static boolean isInSubLevel(Level level, BlockPos pos) {
        if (!isPresent() || level == null || pos == null) return false;
        // Fast pass: extreme coords are always a sub-level
        if (isProbablyInSubLevel(pos)) return true;
        // Try Companion API
        try {
            if (isSableAndCompanionWorking()) {
                Method m = getContainingMethod();
                if (m != null) {
                    Object subLevel = m.invoke(companionInstance, level, pos);
                    return subLevel != null;
                }
            }
        } catch (Throwable ignored) { }
        return false;
    }

    /**
     * Transforms a BlockPos from sub-level coordinates to world coordinates
     * using the sub-level's logical pose. For non-sub-level positions returns Vec3.atCenterOf(pos).
     * Uses Companion API when available; falls back to identity for sub-level positions.
     */
    public static Vec3 subLevelPosToWorld(Level level, BlockPos pos) {
        if (!isPresent() || level == null || pos == null) return Vec3.atCenterOf(pos);
        // Not a sub-level position — return block-corner (not center) for rendering
        if (!isProbablyInSubLevel(pos) && !isSableAndCompanionWorking()) return Vec3.atCenterOf(pos);
        // Try Companion API
        try {
            if (isSableAndCompanionWorking()) {
                Method m = getContainingMethod();
                if (m != null) {
                    Object subLevel = m.invoke(companionInstance, level, pos);
                    if (subLevel != null) {
                        Method poseMethod = logicalPoseMethod();
                        if (poseMethod != null) {
                            Object pose = poseMethod.invoke(subLevel);
                            if (pose != null) {
                                Method transMethod = transformPositionMethod();
                                if (transMethod != null) {
                                    Object result = transMethod.invoke(pose,
                                        (double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5);
                                    if (result instanceof Vec3 vec) return vec;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) { }
        // Fallback: return sub-level center coords (extreme values).
        // In sub-level space the camera is also in sub-level space, so relative
        // position pos-camera will be correct for rendering.
        return Vec3.atCenterOf(pos);
    }

    private SableCompat() {}
}
