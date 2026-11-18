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

package com.craftingdead.core.world.item.gun.ammoprovider;

import com.craftingdead.core.network.NetworkBufferUtil;
import com.craftingdead.core.util.ItemStackNbtUtil;
import com.craftingdead.core.world.action.ActionTypes;
import com.craftingdead.core.world.entity.extension.LivingExtension;
import com.craftingdead.core.world.item.gun.magazine.Magazine;
import com.craftingdead.core.world.item.MagazineItem;
import com.craftingdead.core.world.item.gun.magazine.MagazineStackDataComponentUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public class MagazineAmmoProvider implements AmmoProvider {

  private ItemStack magazineStack;
  private boolean stackChanged;

  public MagazineAmmoProvider() {
    this(ItemStack.EMPTY);
  }

  public MagazineAmmoProvider(ItemStack magazineStack) {
    this.magazineStack = magazineStack;
  }

  @Override
  public CompoundTag serializeNBT() {
    CompoundTag nbt = new CompoundTag();
    nbt.put("magazineStack", ItemStackNbtUtil.save(this.magazineStack));
    return nbt;
  }

  @Override
  public void deserializeNBT(CompoundTag nbt) {
    if (nbt == null) {
      return;
    }
    if (nbt.contains("magazineStack", Tag.TAG_COMPOUND)) {
      this.magazineStack = ItemStackNbtUtil.parseOptional(nbt.getCompound("magazineStack"));
      this.stackChanged = true;
    }
  }

  @Override
  public void encode(FriendlyByteBuf out, boolean writeAll) {
    if (this.stackChanged || writeAll) {
      NetworkBufferUtil.writeBoolean(out, true);
      NetworkBufferUtil.writeItem(out, this.magazineStack);
      this.stackChanged = false;
    } else {
      NetworkBufferUtil.writeBoolean(out, false);
    }
    this.getMagazine().ifPresent(magazine -> magazine.encode(out, writeAll));
  }

  @Override
  public void decode(FriendlyByteBuf in) {
    if (in == null) {
      return;
    }
    if (NetworkBufferUtil.readBoolean(in, false)) {
      this.magazineStack = NetworkBufferUtil.readItem(in);
    }
    if (in.readableBytes() > 0) {
      this.getMagazine().ifPresent(magazine -> magazine.decode(in));
    }
  }

  @Override
  public boolean requiresSync() {
    return this.stackChanged
        || !this.magazineStack.isEmpty()
        || this.getMagazine().map(Magazine::requiresSync).orElse(false);
  }

  @Override
  public void reload(LivingExtension<?, ?> living) {
    if (living != null) {
      living.performAction(ActionTypes.MAGAZINE_RELOAD.get().decode(living, null), true);
    }
  }

  @Override
  public void unload(LivingExtension<?, ?> living) {
    if (living != null) {
      living.performAction(ActionTypes.REMOVE_MAGAZINE.get().decode(living, null), true);
    }
  }

  @Override
  public int getReserveSize() {
    return 0;
  }

  @Override
  public ItemStack getMagazineStack() {
    return this.magazineStack;
  }

  public void setMagazineStack(ItemStack magazineStack) {
    this.magazineStack = magazineStack == null ? ItemStack.EMPTY : magazineStack;
    if (!this.magazineStack.isEmpty()) {
      Magazine.get(this.magazineStack).ifPresent(magazine ->
          MagazineStackDataComponentUtil.writeMagazineState(this.magazineStack,
              magazine.serializeNBT()));
    }
    this.stackChanged = true;
  }

  @Override
  public AmmoProviderType getType() {
    return AmmoProviderTypes.MAGAZINE.get();
  }
}
