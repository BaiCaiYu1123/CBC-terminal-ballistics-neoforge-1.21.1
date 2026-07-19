package com.cbc_terminal_ballistics.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.fml.ModList;

public final class CBCNeoWarfareCompat {
    public static final double APFSDS_PENETRATION_MULTIPLIER = 3.0D;
    public static final double APFSDS_SPEED_MULTIPLIER = 2.0D;
    public static final double APFSDS_RICOCHET_SURFACE_ANGLE_DEGREES = 10.0D;
    public static final double APFSDS_RICOCHET_MAX_INCIDENCE =
        Math.sin(Math.toRadians(APFSDS_RICOCHET_SURFACE_ANGLE_DEGREES));

    private static final String MOD_ID = "cbcmodernwarfare";
    private static final ResourceLocation APFSDS_PROJECTILE =
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "apfsds_mediumshell");

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static boolean isApfsdsProjectile(Entity entity) {
        return isLoaded()
            && entity != null
            && APFSDS_PROJECTILE.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
    }

    public static boolean shouldForceApfsdsRicochet(Entity entity, double incidence) {
        return isApfsdsProjectile(entity)
            && incidence < APFSDS_RICOCHET_MAX_INCIDENCE;
    }

    private CBCNeoWarfareCompat() {}
}
