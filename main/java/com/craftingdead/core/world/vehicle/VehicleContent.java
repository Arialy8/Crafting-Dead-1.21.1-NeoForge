package com.craftingdead.core.world.vehicle;

import com.craftingdead.core.CraftingDead;
import com.craftingdead.core.world.item.ModItems;
import com.craftingdead.core.world.item.DamagedVehiclePartItem;
import com.craftingdead.core.world.item.VehicleWrenchItem;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Self-contained vehicle registry. It intentionally has no runtime link to
 * Superb Warfare, Immersive Vehicles, MTS, or any content pack.
 */
public final class VehicleContent {

  public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
      DeferredRegister.create(Registries.ENTITY_TYPE, CraftingDead.ID);
  public static final DeferredRegister<Item> ITEMS =
      DeferredRegister.create(Registries.ITEM, CraftingDead.ID);
  public static final DeferredRegister<Block> BLOCKS =
      DeferredRegister.create(Registries.BLOCK, CraftingDead.ID);
  public static final DeferredRegister<Fluid> FLUIDS =
      DeferredRegister.create(Registries.FLUID, CraftingDead.ID);
  public static final DeferredRegister<FluidType> FLUID_TYPES =
      DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, CraftingDead.ID);
  public static final DeferredRegister<CreativeModeTab> TABS =
      DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CraftingDead.ID);
  public static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES =
      DeferredRegister.create(NeoForgeRegistries.INGREDIENT_TYPES, CraftingDead.ID);

  public static final Supplier<IngredientType<StackedIngredient>> STACKED_INGREDIENT =
      INGREDIENT_TYPES.register("stacked", () -> new IngredientType<>(StackedIngredient.CODEC));

  public static final Supplier<FluidType> PETROLEUM_TYPE = FLUID_TYPES.register("petroleum",
      PetroleumFluidType::new);
  public static final Supplier<BaseFlowingFluid.Source> PETROLEUM = FLUIDS.register("petroleum",
      () -> new BaseFlowingFluid.Source(petroleumProperties()));
  public static final Supplier<BaseFlowingFluid.Flowing> FLOWING_PETROLEUM = FLUIDS.register(
      "flowing_petroleum", () -> new BaseFlowingFluid.Flowing(petroleumProperties()));
  public static final Supplier<LiquidBlock> PETROLEUM_BLOCK = BLOCKS.register("petroleum",
      () -> new LiquidBlock(PETROLEUM.get(),
          BlockBehaviour.Properties.ofFullCopy(Blocks.LAVA).lightLevel(state -> 0)));

  public static final Supplier<EntityType<T72B3Entity>> T72B3 = ENTITY_TYPES.register("t_72b3",
      () -> EntityType.Builder.of(T72B3Entity::new, MobCategory.MISC)
      // The physical hull is handled by T72B3Entity as an oriented 6 x 16 block rectangle.
      // Keep Minecraft's fallback selection box low so it does not become a giant vertical cube.
      .sized(6.1F, 1.0F)
          .clientTrackingRange(96)
          .updateInterval(1)
          .build(ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "t_72b3").toString()));
  public static final Supplier<EntityType<T72ShellEntity>> T72_SHELL = ENTITY_TYPES.register(
      "t72_shell", () -> EntityType.Builder.<T72ShellEntity>of(T72ShellEntity::new,
          MobCategory.MISC).sized(0.22F, 0.22F).clientTrackingRange(128).updateInterval(1)
          .setShouldReceiveVelocityUpdates(true)
          .build(ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "t72_shell").toString()));

  public static final Supplier<Item> T72B3_KIT = ITEMS.register("t_72b3",
      () -> new T72B3Item(new Item.Properties().stacksTo(1)));
  public static final Supplier<Item> SHELL_125MM_HE = ITEMS.register("125mm_he",
      () -> new Item(new Item.Properties().stacksTo(16)));
  public static final Supplier<Item> SHELL_125MM_AP = ITEMS.register("125mm_ap",
      () -> new Item(new Item.Properties().stacksTo(16)));
  public static final Supplier<Item> T72B3_TURRET = ITEMS.register("t_72b3_turret",
      () -> new Item(new Item.Properties().stacksTo(1)));
  public static final Supplier<Item> TRACK_S = ITEMS.register("track_s",
      () -> new Item(new Item.Properties().stacksTo(8)));
  public static final Supplier<Item> ENGINE_VTA = ITEMS.register("enginevta",
      () -> new Item(new Item.Properties().stacksTo(1)));
  public static final Supplier<Item> RESHETKA_EPTA = ITEMS.register("reshetka_epta",
      () -> new Item(new Item.Properties().stacksTo(16)));
  public static final Supplier<Item> BULLET_762 = ITEMS.register("bullet762",
      () -> new Item(new Item.Properties().stacksTo(64)));
  public static final Supplier<Item> SMOKE_40MM = ITEMS.register("40mm_smoke",
      () -> new Item(new Item.Properties().stacksTo(16)));
  public static final Supplier<Item> BLOWTORCH = ITEMS.register("blowtorch",
      () -> new Item(new Item.Properties().stacksTo(1).durability(256)));
  public static final Supplier<Item> DIESEL_CAN = ITEMS.register("diesel_can",
      () -> new Item(new Item.Properties().stacksTo(16)));
  public static final Supplier<Item> PETROLEUM_BUCKET = ITEMS.register("petroleum_bucket",
      () -> new BucketItem(PETROLEUM.get(),
          new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
  public static final Supplier<Item> IRON_WRENCH = ITEMS.register("iron_wrench",
      () -> new VehicleWrenchItem(5, -2.4F, new Item.Properties().durability(180)));
  public static final Supplier<Item> DAMAGED_ENGINE_VTA = ITEMS.register("damaged_enginevta",
      () -> new DamagedVehiclePartItem(new Item.Properties().stacksTo(1),
          "item.craftingdead.enginevta"));
  public static final Supplier<Item> DAMAGED_TRACK_S = ITEMS.register("damaged_track_s",
      () -> new DamagedVehiclePartItem(new Item.Properties().stacksTo(8),
          "item.craftingdead.track_s"));
  public static final Supplier<Item> DAMAGED_T72B3_TURRET = ITEMS.register(
      "damaged_t_72b3_turret", () -> new DamagedVehiclePartItem(
          new Item.Properties().stacksTo(1), "item.craftingdead.t_72b3_turret"));
  public static final Supplier<Item> DAMAGED_RESHETKA_EPTA = ITEMS.register(
      "damaged_reshetka_epta", () -> new DamagedVehiclePartItem(
          new Item.Properties().stacksTo(8), "item.craftingdead.reshetka_epta"));

  public static final Supplier<CreativeModeTab> TAB = TABS.register("vehicles",
      () -> CreativeModeTab.builder()
          .title(Component.translatable("itemGroup.craftingdead.vehicles"))
          .icon(() -> new ItemStack(T72B3_KIT.get()))
          .displayItems((parameters, output) -> {
            output.accept(T72B3_KIT.get());
            output.accept(SHELL_125MM_HE.get());
            output.accept(SHELL_125MM_AP.get());
            output.accept(IRON_WRENCH.get());
            output.accept(ModItems.SCRAP_METAL.get());
            output.accept(DAMAGED_ENGINE_VTA.get());
            output.accept(DAMAGED_TRACK_S.get());
            output.accept(DAMAGED_T72B3_TURRET.get());
            output.accept(DAMAGED_RESHETKA_EPTA.get());
            output.accept(T72B3_TURRET.get());
            output.accept(TRACK_S.get());
            output.accept(ENGINE_VTA.get());
            output.accept(RESHETKA_EPTA.get());
            output.accept(BULLET_762.get());
            output.accept(SMOKE_40MM.get());
            output.accept(BLOWTORCH.get());
            output.accept(PETROLEUM_BUCKET.get());
            output.accept(DIESEL_CAN.get());
          })
          .build());

  public static void register(IEventBus modEventBus) {
    VehicleGameRules.init();
    FLUID_TYPES.register(modEventBus);
    FLUIDS.register(modEventBus);
    BLOCKS.register(modEventBus);
    ENTITY_TYPES.register(modEventBus);
    ITEMS.register(modEventBus);
    TABS.register(modEventBus);
    INGREDIENT_TYPES.register(modEventBus);
    NeoForge.EVENT_BUS.addListener(VehicleContent::handleVehiclePartCrafted);
    NeoForge.EVENT_BUS.addListener(VehicleContent::protectClosedTankPassengers);
  }

  private static void protectClosedTankPassengers(LivingIncomingDamageEvent event) {
    if (!(event.getEntity().getVehicle() instanceof T72B3Entity tank)
        || tank.isApplyingInternalDamage(event.getEntity())) {
      return;
    }
    boolean insideArmour = !tank.isRearPassenger(event.getEntity())
        || !tank.isPassengerExposed(event.getEntity());
    if (insideArmour && event.getSource().is(DamageTypeTags.IS_PROJECTILE)) {
      event.setCanceled(true);
    }
  }

  private static void handleVehiclePartCrafted(PlayerEvent.ItemCraftedEvent event) {
    if (event.getEntity().level().isClientSide() || !usesFullScrapStacks(event.getCrafting())) {
      return;
    }
    // ResultSlot fires this event before vanilla removes one item from each occupied slot.
    // Remove the other 63 here, so every stacked ingredient consumes one complete stack.
    for (int slot = 0; slot < event.getInventory().getContainerSize(); slot++) {
      ItemStack stack = event.getInventory().getItem(slot);
      if (stack.is(ModItems.SCRAP_METAL.get())) {
        stack.shrink(63);
      }
    }
  }

  private static boolean usesFullScrapStacks(ItemStack stack) {
    return stack.is(ENGINE_VTA.get()) || stack.is(TRACK_S.get())
        || stack.is(T72B3_TURRET.get()) || stack.is(RESHETKA_EPTA.get())
        || stack.is(T72B3_KIT.get());
  }

  private static BaseFlowingFluid.Properties petroleumProperties() {
    // Lava-like spreading in the Overworld: short reach, two levels lost per block and a slow
    // 30-tick update. Petroleum is non-renewable and deliberately emits no light.
    return new BaseFlowingFluid.Properties(PETROLEUM_TYPE, PETROLEUM, FLOWING_PETROLEUM)
        .bucket(PETROLEUM_BUCKET).block(PETROLEUM_BLOCK)
        .slopeFindDistance(2).levelDecreasePerBlock(2).tickRate(30)
        .explosionResistance(100.0F);
  }

  private VehicleContent() {}
}
