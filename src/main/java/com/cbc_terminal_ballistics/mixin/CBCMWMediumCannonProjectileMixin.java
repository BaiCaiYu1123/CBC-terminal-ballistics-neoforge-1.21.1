package com.cbc_terminal_ballistics.mixin;

import com.cbc_terminal_ballistics.ballistics.TBImpactService;
import com.cbc_terminal_ballistics.compat.CBCNeoWarfareCompat;
import com.cbc_terminal_ballistics.debug.TBDebug;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "riftyboi.cbcmodernwarfare.munitions.medium_cannon.AbstractMediumcannonProjectile", remap = false)
public abstract class CBCMWMediumCannonProjectileMixin {
    @Unique
    private static final String CTB_APFSDS_SPEED_BOOSTED_TAG = "CTBNeoWarfareAPFSDSSpeedBoosted";

    @Unique
    private boolean ctb$apfsdsSpeedBoosted;

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void ctb$boostApfsdsSpeed(CallbackInfo ci) {
        Entity projectile = (Entity) (Object) this;
        if (projectile.level().isClientSide || ctb$apfsdsSpeedBoosted || !CBCNeoWarfareCompat.isApfsdsProjectile(projectile)) return;

        Vec3 velocity = projectile.getDeltaMovement();
        if (velocity.lengthSqr() <= 1.0E-8D) return;
        projectile.setDeltaMovement(velocity.scale(CBCNeoWarfareCompat.APFSDS_SPEED_MULTIPLIER));
        ctb$apfsdsSpeedBoosted = true;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"), remap = false)
    private void ctb$saveApfsdsSpeedBoost(CompoundTag tag, CallbackInfo ci) {
        if (ctb$apfsdsSpeedBoosted) tag.putBoolean(CTB_APFSDS_SPEED_BOOSTED_TAG, true);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"), remap = false)
    private void ctb$loadApfsdsSpeedBoost(CompoundTag tag, CallbackInfo ci) {
        ctb$apfsdsSpeedBoosted = tag.getBoolean(CTB_APFSDS_SPEED_BOOSTED_TAG);
    }

    @Inject(method = "calculateBlockPenetration", at = @At("HEAD"), cancellable = true, remap = false)
    private void ctb$calculateBlockPenetration(@Coerce Object projectileContext, BlockState state, BlockHitResult blockHitResult, CallbackInfoReturnable<Object> cir) {
        TBDebug.mixinHit("medium", (Entity) (Object) this, state, blockHitResult.getBlockPos());
        Object result = TBImpactService.calculate((Entity) (Object) this, projectileContext, state, blockHitResult, false);
        if (result != null) cir.setReturnValue(result);
    }
}
