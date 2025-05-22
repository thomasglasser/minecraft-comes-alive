package net.mca.mixin.client;

import net.mca.Config;
import net.mca.MCAClient;
import net.mca.entity.VillagerLike;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
abstract class MixinPlayerEntityClient extends LivingEntity {
    protected MixinPlayerEntityClient(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "getStandingEyeHeight", at = @At("RETURN"), cancellable = true)
    public void mca$getActiveEyeHeight(Pose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
        if (Config.getInstance().adjustPlayerEyesToHeight) {
            MCAClient.getPlayerData(getUUID()).ifPresent(data -> {
                if (data.getPlayerModel() != VillagerLike.PlayerModel.VANILLA) {
                    cir.setReturnValue(Math.min(this.getBbHeight() - 1.0f / 16.0f, cir.getReturnValue() * data.getRawScaleFactor()));
                }
            });
        }
    }
}
