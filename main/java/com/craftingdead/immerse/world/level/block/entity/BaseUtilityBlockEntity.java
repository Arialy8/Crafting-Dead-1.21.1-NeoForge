package com.craftingdead.immerse.world.level.block.entity;
import com.craftingdead.core.world.item.ModItems;
import com.craftingdead.immerse.world.base.BaseSecurity;
import com.craftingdead.survival.world.item.SurvivalItems;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Persistent nine-slot base utilities. Processing and outputs are server-authoritative. */
public final class BaseUtilityBlockEntity extends BlockEntity implements Container, MenuProvider {
  private final NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
  private int progress, water, generation;
  private String activeInput = "";
  public BaseUtilityBlockEntity(BlockPos pos, BlockState state) { super(ImmerseBlockEntityTypes.BASE_UTILITY.get(), pos, state); }
  private String kind() { return BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).getPath(); }
  public String instructions() {
    return kind().equals("grinder") ? "粉碎机：左侧 5 格放枪械，右侧 4 格收取废金属；开始前请卸下弹匣和配件。"
        : kind().equals("water_pump") ? "水泵：左侧 3 格放空水瓶/水壶，右侧收取饮用水。储水：" + water + "/100；每分钟自动采水。"
        : "信箱：存放和领取物品，遵循基地容器权限。";
  }
  @Override public Component getDisplayName() { return Component.translatable(getBlockState().getBlock().getDescriptionId()); }
  @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
    return new com.craftingdead.immerse.world.inventory.BaseUtilityMenu(id, inventory, this);
  }
  public void tick() {
    if (level == null || level.isClientSide() || kind().equals("cfm_stripped_oak_mail_box")) return;
    boolean pump = kind().equals("water_pump");
    if (pump && ++generation >= (level.isRainingAt(worldPosition.above()) ? 200 : 1200)) {
      generation = 0;
      water = Math.min(100, water + 1);
      setChanged();
    }
    int slot = -1, ticks = 0;
    ItemStack output = ItemStack.EMPTY;
    for (int i = 0; i < (pump ? 3 : 5); i++) {
      var input = items.get(i);
      if (pump) {
        String id = BuiltInRegistries.ITEM.getKey(input.getItem()).toString();
        String filled = id.equals("craftingdeadsurvival:empty_water_bottle") ? "water_bottle"
            : id.equals("craftingdeadsurvival:empty_water_canteen") ? "water_canteen" : null;
        if (filled != null && water > 0) {
          slot = i; ticks = 100; output = new ItemStack(SurvivalItems.ITEMS.get(filled).get()); break;
        }
      } else {
        var recipe = com.craftingdead.immerse.world.base.LegacyGrinderRecipes.find(input);
        if (recipe != null) { slot = i; ticks = recipe[1]; output = new ItemStack(ModItems.SCRAP_METAL.get(), recipe[0]); break; }
      }
    }
    if (slot < 0) { if (progress != 0) { progress = 0; activeInput = ""; setChanged(); } return; }
    String identity = slot + ":" + BuiltInRegistries.ITEM.getKey(items.get(slot).getItem());
    if (!identity.equals(activeInput)) { progress = 0; activeInput = identity; }
    int target = -1;
    for (int i = pump ? 3 : 5; i < 9; i++) {
      var existing = items.get(i);
      if (existing.isEmpty() || (ItemStack.isSameItemSameComponents(existing, output)
          && existing.getCount() + output.getCount() <= existing.getMaxStackSize())) { target = i; break; }
    }
    if (target < 0) return;
    if (++progress >= ticks) {
      items.get(slot).shrink(1);
      if (items.get(target).isEmpty()) items.set(target, output); else items.get(target).grow(output.getCount());
      if (pump) water--;
      progress = 0;
    }
    setChanged();
  }
  @Override public int getContainerSize() { return 9; }
  @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
  @Override public boolean canPlaceItem(int slot, ItemStack stack) {
    if (kind().equals("cfm_stripped_oak_mail_box")) return true;
    if (kind().equals("grinder")) return slot < 5 && com.craftingdead.immerse.world.base.LegacyGrinderRecipes.find(stack) != null;
    String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    return slot < 3 && (id.equals("craftingdeadsurvival:empty_water_bottle")
        || id.equals("craftingdeadsurvival:empty_water_canteen"));
  }
  @Override public ItemStack getItem(int slot) { return items.get(slot); }
  @Override public ItemStack removeItem(int slot, int count) { var stack = ContainerHelper.removeItem(items, slot, count); setChanged(); return stack; }
  @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
  @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); stack.limitSize(stack.getMaxStackSize()); setChanged(); }
  @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player) && BaseSecurity.canOpenContainer(level, worldPosition, player); }
  @Override public void clearContent() { items.clear(); setChanged(); }
  @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
    super.saveAdditional(tag, lookup); ContainerHelper.saveAllItems(tag, items, lookup);
    tag.putInt("Progress", progress); tag.putInt("Water", water); tag.putInt("Generation", generation); tag.putString("ActiveInput", activeInput);
  }
  @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
    super.loadAdditional(tag, lookup); ContainerHelper.loadAllItems(tag, items, lookup);
    progress = Math.max(0, tag.getInt("Progress")); water = Math.clamp(tag.getInt("Water"), 0, 100);
    generation = Math.clamp(tag.getInt("Generation"), 0, 1200); activeInput = tag.getString("ActiveInput");
  }
}
