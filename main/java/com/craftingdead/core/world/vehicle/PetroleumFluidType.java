package com.craftingdead.core.world.vehicle;

import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;

/** Dark, viscous crude oil used to manufacture vehicle fuel tanks. */
public final class PetroleumFluidType extends FluidType {

  private static final ResourceLocation STILL = ResourceLocation.withDefaultNamespace(
      "block/water_still");
  private static final ResourceLocation FLOWING = ResourceLocation.withDefaultNamespace(
      "block/water_flow");

  public PetroleumFluidType() {
    super(FluidType.Properties.create()
        .descriptionId("fluid_type.craftingdead.petroleum")
        .density(850).viscosity(3000).temperature(295)
        .canConvertToSource(false)
        .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
        .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY));
  }

  @Override
  public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
    consumer.accept(new IClientFluidTypeExtensions() {
      @Override
      public ResourceLocation getStillTexture() {
        return STILL;
      }

      @Override
      public ResourceLocation getFlowingTexture() {
        return FLOWING;
      }

      @Override
      public int getTintColor() {
        return 0xFF241A12;
      }
    });
  }
}
