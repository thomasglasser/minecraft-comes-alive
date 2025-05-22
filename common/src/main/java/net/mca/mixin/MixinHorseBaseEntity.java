package net.mca.mixin;

import net.mca.entity.VillagerEntityMCA;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractHorse.class)
abstract class MixinHorseBaseEntity extends Animal implements ContainerListener, PlayerRideableJumping, Saddleable {
    @Shadow @Nullable public abstract LivingEntity getControllingPassenger();

    MixinHorseBaseEntity() { super(null, null); }

    @Inject(method = "isImmobile()Z", at = @At("HEAD"), cancellable = true)
    private void onIsImmobile(CallbackInfoReturnable<Boolean> info) {
        if (getControllingPassenger() instanceof VillagerEntityMCA) {
            info.setReturnValue(false); // Fixes villagers not being able to move when riding a horse
        }
    }
}
