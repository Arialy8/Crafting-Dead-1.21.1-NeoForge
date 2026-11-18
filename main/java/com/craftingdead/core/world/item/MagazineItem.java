/*
 * Crafting Dead
 * Copyright (C) 2022  NexusNode LTD
 *
 * This Non-Commercial Software License Agreement (the "Agreement") is made between
 * you (the "Licensee") and NEXUSNODE (BRAD HUNTER). (the "Licensor").
 * By installing or otherwise using Crafting Dead (the "Software"), you agree to be
 * bound by the terms and conditions of this Agreement as may be revised from time
 * to time at Licensor's sole discretion.
 *
 * If you do not agree to the terms and conditions of this Agreement do not download,
 * copy, reproduce or otherwise use any of the source code available online at any time.
 *
 * https://github.com/nexusnode/crafting-dead/blob/1.18.x/LICENSE.txt
 *
 * https://craftingdead.net/terms.php
 */

package com.craftingdead.core.world.item;

import com.craftingdead.core.capability.CapabilityUtil;
import com.craftingdead.core.world.item.gun.magazine.Magazine;
import com.craftingdead.core.world.item.gun.magazine.MagazineImpl;
import com.craftingdead.core.world.item.gun.magazine.MagazineStackDataComponentUtil;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;

public class MagazineItem extends Item {

  private final float armorPenetration;
  private final int size;

  public MagazineItem(Properties properties) {
    super(properties);
    this.size = properties.size;
    this.armorPenetration = properties.armorPenetration;
  }

  public float getArmorPenetration() {
    return this.armorPenetration;
  }

  public int getSize() {
    return this.size;
  }

  @Override
  public ItemStack getDefaultInstance() {
    ItemStack stack = super.getDefaultInstance();
    MagazineStackDataComponentUtil.setSize(stack, this.size);
    return stack;
  }

  public boolean isValidRepairItem(ItemStack itemStack, ItemStack materialStack) {
    return materialStack.is(Tags.Items.GUNPOWDERS)
        || super.isValidRepairItem(itemStack, materialStack);
  }

    public int getMaxDamage(ItemStack itemStack) {
    return this.size;
  }

    public int getDamage(ItemStack itemStack) {
    int currentSize = Magazine.get(itemStack)
        .map(Magazine::getSize)
        .orElseGet(() -> MagazineStackDataComponentUtil.getSize(itemStack, this.size));
    return this.size - currentSize;
  }

    public void setDamage(ItemStack itemStack, int damage) {
    int newSize = Math.max(0, this.size - damage);
    Magazine.get(itemStack)
        .ifPresent(magazine -> magazine.setSize(newSize));
    MagazineStackDataComponentUtil.setSize(itemStack, newSize);
  }

    public boolean canBeDepleted() {
    return true;
  }

    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines,
      TooltipFlag tooltipFlag) {
    super.appendHoverText(stack, context, lines, tooltipFlag);

    // Shows the current amount if the maximum size is higher than 1
    if (this.getSize() > 1) {
      int currentAmount =
          Magazine.get(stack)
              .map(Magazine::getSize)
              .orElseGet(() -> MagazineStackDataComponentUtil.getSize(stack, 0));

      Component amountText = Component.literal(currentAmount + "/" + this.getSize())
          .withStyle(ChatFormatting.RED);

      lines.add(Component.translatable("magazine.amount")
          .withStyle(ChatFormatting.GRAY)
          .append(amountText));
    }

    if (this.armorPenetration > 0) {
      lines.add(Component.translatable("magazine.armor_penetration")
          .withStyle(ChatFormatting.GRAY)
          .append(Component.literal(String.format("%.0f%%", this.armorPenetration))
              .withStyle(ChatFormatting.RED)));
    }
  }

  /**
   * Forge 1.21 removed the old Item#getShareTag hook. Kept as an explicit helper
   * until magazine state is fully mirrored through data components/network sync.
   */
  public CompoundTag getCraftingDeadShareTag(ItemStack itemStack) {
    CompoundTag nbt = new CompoundTag();
    CompoundTag magazineNbt = Magazine.get(itemStack)
        .map(Magazine::serializeNBT)
        .orElse(null);
    if (magazineNbt != null && !magazineNbt.isEmpty()) {
      nbt.put("magazine", magazineNbt);
      MagazineStackDataComponentUtil.writeMagazineState(itemStack, magazineNbt);
    }
    return nbt;
  }

  /** See {@link #getCraftingDeadShareTag(ItemStack)}. */
  public void readCraftingDeadShareTag(ItemStack itemStack, @Nullable CompoundTag nbt) {
    CompoundTag magazineTag = nbt != null && nbt.contains("magazine", Tag.TAG_COMPOUND)
        ? nbt.getCompound("magazine")
        : MagazineStackDataComponentUtil.readMagazineState(itemStack);
    if (!magazineTag.isEmpty()) {
      Magazine.get(itemStack)
          .ifPresent(magazine -> magazine.deserializeNBT(magazineTag));
      MagazineStackDataComponentUtil.writeMagazineState(itemStack, magazineTag);
    }
  }

  /**
   * Creates a magazine view backed by the supplied ItemStack data component.
   *
   * <p>NeoForge 1.21.1 no longer supplies the old Forge item capability automatically. Every call
   * reconstructs the lightweight runtime view from the stack, while all mutations are immediately
   * written back into the stack's magazine component.</p>
   */
  public Magazine createComponentBackedMagazine(ItemStack stack) {
    ComponentBackedMagazine magazine = new ComponentBackedMagazine(this, stack);
    CompoundTag state = MagazineStackDataComponentUtil.readMagazineState(stack);
    if (state.isEmpty()) {
      state.putInt(MagazineStackDataComponentUtil.SIZE, this.size);
      MagazineStackDataComponentUtil.writeMagazineState(stack, state);
    }
    magazine.loadFromStack(state);
    return magazine;
  }

  /**
   * Keeps magazine mutations mirrored into ItemStack CUSTOM_DATA. The old implementation relied on
   * the capability share tag path, but that path is no longer a safe source of truth in 1.21.1.
   */
  private static final class ComponentBackedMagazine extends MagazineImpl {

    private final ItemStack stack;

    private ComponentBackedMagazine(MagazineItem magazineItem, ItemStack stack) {
      super(magazineItem);
      this.stack = stack;
    }

    private void persist() {
      MagazineStackDataComponentUtil.writeMagazineState(this.stack, this.serializeNBT());
    }

    private void loadFromStack(CompoundTag nbt) {
      super.deserializeNBT(nbt);
    }

        public void deserializeNBT(CompoundTag nbt) {
      super.deserializeNBT(nbt);
      this.persist();
    }

        public void setSize(int size) {
      super.setSize(size);
      this.persist();
    }

        public void refill() {
      super.refill();
      this.persist();
    }

        public int decrementSize() {
      int size = super.decrementSize();
      this.persist();
      return size;
    }

        public void decode(net.minecraft.network.FriendlyByteBuf in) {
      super.decode(in);
      this.persist();
    }
  }

  public static class Properties extends Item.Properties {

    private float armorPenetration;
    private int size;

    public Properties setArmorPenetration(float armorPenetration) {
      this.armorPenetration = armorPenetration;
      return this;
    }

    public Properties setSize(int size) {
      this.size = size;
      return this;
    }
  }
}
