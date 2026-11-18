package com.craftingdead.immerse.world.level.block.entity;

import com.craftingdead.immerse.CraftingDeadImmerse;
import com.craftingdead.immerse.world.level.block.ImmerseBlocks;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ImmerseBlockEntityTypes {

  public static final DeferredRegister<BlockEntityType<?>> deferredRegister =
      DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CraftingDeadImmerse.ID);

  public static final Supplier<BlockEntityType<BaseCenterBlockEntity>> BASE_CENTER =
      deferredRegister.register("base_center",
          () -> BlockEntityType.Builder.of(BaseCenterBlockEntity::new,
              ImmerseBlocks.BLOCKS.get("base_center").get()).build(null));

  private ImmerseBlockEntityTypes() {}
  public static final Supplier<BlockEntityType<BaseUtilityBlockEntity>> BASE_UTILITY =
      deferredRegister.register("base_utility",
          () -> BlockEntityType.Builder.of(BaseUtilityBlockEntity::new,
              ImmerseBlocks.BLOCKS.get("water_pump").get(), ImmerseBlocks.BLOCKS.get("grinder").get(),
              ImmerseBlocks.BLOCKS.get("cfm_stripped_oak_mail_box").get()).build(null));
}
