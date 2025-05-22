package net.mca.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mca.MCAClient;
import net.mca.client.model.PlayerArmorExtendedModel;
import net.mca.client.model.VillagerEntityModelMCA;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HumanoidArmorLayer.class)
public abstract class MixinArmorFeatureRenderer<T extends LivingEntity, A extends HumanoidModel<T>> {
    @Shadow
    protected abstract boolean usesInnerModel(EquipmentSlot slot);

    protected boolean mca$injectionActive;
    protected final A mca$leggingsModel = createModel(0.5F);
    protected final A mca$bodyModel = createModel(1.0F);

    private A createModel(float dilation) {
        //noinspection unchecked
        return (A)new PlayerArmorExtendedModel<T>(LayerDefinition.create(VillagerEntityModelMCA.armorData(new CubeDeformation(dilation)), 64, 32).bakeRoot());
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V", at = @At("HEAD"))
    public void render(PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int i, T livingEntity, float f, float g, float h, float j, float k, float l, CallbackInfo ci) {
        mca$injectionActive = livingEntity instanceof Player && MCAClient.useGeneticsRenderer(livingEntity.getUUID());
    }

    @Inject(method = "getArmorModel", at = @At("HEAD"), cancellable = true)
    private void getArmor(EquipmentSlot slot, CallbackInfoReturnable<A> cir) {
        if (mca$injectionActive) {
            cir.setReturnValue(this.usesInnerModel(slot) ? mca$leggingsModel : mca$bodyModel);
        }
    }
}
