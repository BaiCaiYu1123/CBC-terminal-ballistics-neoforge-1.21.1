package com.cbc_terminal_ballistics.mixin;

import com.cbc_terminal_ballistics.compat.CBCNeoWarfareCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rbasamoyai.createbigcannons.munitions.config.components.BallisticPropertiesComponent;

@Pseudo
@Mixin(targets = "riftyboi.cbcmodernwarfare.munitions.medium_cannon.apfsds.APFSDSMediumcannonProjectile", remap = false)
public abstract class CBCMWAPFSDSProjectileMixin {
    @Inject(method = "getBallisticProperties", at = @At("RETURN"), cancellable = true, remap = false)
    private void ctb$boostApfsdsBallistics(CallbackInfoReturnable<BallisticPropertiesComponent> cir) {
        BallisticPropertiesComponent original = cir.getReturnValue();
        if (!CBCNeoWarfareCompat.isLoaded() || original == null) return;

        cir.setReturnValue(new BallisticPropertiesComponent(
            original.gravity(),
            original.drag(),
            original.isQuadraticDrag(),
            (float) (original.durabilityMass() * CBCNeoWarfareCompat.APFSDS_PENETRATION_MULTIPLIER),
            (float) (original.penetration() * CBCNeoWarfareCompat.APFSDS_PENETRATION_MULTIPLIER),
            original.toughness(),
            (float) CBCNeoWarfareCompat.APFSDS_RICOCHET_MAX_INCIDENCE
        ));
    }
}
