/*
 * Crafting Dead
 */
package com.craftingdead.core.network.message.play;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import com.craftingdead.core.network.NetworkUtil;
import com.craftingdead.core.network.NetworkBufferUtil;
import com.craftingdead.core.network.payload.NetworkPayloadIds;
import com.craftingdead.core.network.payload.LegacyPayloadCodec;
import com.craftingdead.core.world.item.gun.Gun;
import com.craftingdead.core.world.item.gun.GunAmmoStateUtil;
import com.craftingdead.core.world.item.gun.GunStackDataComponentUtil;
import com.craftingdead.core.world.item.gun.ammoprovider.MagazineAmmoProvider;
import com.craftingdead.core.world.item.gun.magazine.Magazine;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public record SyncGunEquipmentSlotMessage(int entityId, EquipmentSlot slot,
    FriendlyByteBuf data, ItemStack stack) implements CustomPacketPayload {

  private static final Logger LOGGER = LogUtils.getLogger();

  public static final CustomPacketPayload.Type<SyncGunEquipmentSlotMessage> TYPE = NetworkPayloadIds.SYNC_GUN_EQUIPMENT_SLOT;

  public static final StreamCodec<FriendlyByteBuf, SyncGunEquipmentSlotMessage> STREAM_CODEC =
      LegacyPayloadCodec.codec(SyncGunEquipmentSlotMessage::encode, SyncGunEquipmentSlotMessage::decode);

  public SyncGunEquipmentSlotMessage(int entityId, EquipmentSlot slot, FriendlyByteBuf data) {
    this(entityId, slot, data, ItemStack.EMPTY);
  }

  public SyncGunEquipmentSlotMessage(int entityId, EquipmentSlot slot, Gun gun,
      boolean writeAll) {
    this(entityId, slot == null ? EquipmentSlot.MAINHAND : slot,
        new FriendlyByteBuf(Unpooled.buffer()), gun == null ? ItemStack.EMPTY : gun.getItemStack().copy());
    if (gun != null) {
      gun.encode(this.data, writeAll);
    }
  }

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public void encode(FriendlyByteBuf out) {
    NetworkBufferUtil.writeVarInt(out, this.entityId, -1, Integer.MAX_VALUE);
    NetworkBufferUtil.writeEnum(out, this.slot, EquipmentSlot.MAINHAND);
    NetworkBufferUtil.writePayload(out, this.data);
    NetworkBufferUtil.writeItem(out, this.stack == null ? ItemStack.EMPTY : this.stack);
  }

  public static SyncGunEquipmentSlotMessage decode(FriendlyByteBuf in) {
    int entityId = NetworkBufferUtil.readVarInt(in, -1);
    EquipmentSlot slot = NetworkBufferUtil.readEnum(in, EquipmentSlot.class, EquipmentSlot.MAINHAND);
    FriendlyByteBuf data = NetworkBufferUtil.readPayload(in);
    ItemStack stack = in != null && in.readableBytes() > 0
        ? NetworkBufferUtil.readItem(in)
        : ItemStack.EMPTY;
    return new SyncGunEquipmentSlotMessage(entityId, slot, data, stack);
  }

  public boolean handle(IPayloadContext context) {
    context.enqueueWork(() -> NetworkUtil.findEntity(context, this.entityId, LivingEntity.class)
        .filter(entity -> this.slot != null && this.data != null)
        .ifPresent(entity -> {
          ItemStack targetStack = entity.getItemBySlot(this.slot);

          // This is a state-only packet, not an inventory update. MAINHAND is a moving
          // target: a delayed packet can arrive after the player selects an empty hotbar slot.
          // Materializing the snapshot there duplicates the previous gun. Only vanilla inventory
          // synchronization may create/move stacks, including for remote players and mobs.
          if (targetStack == null || targetStack.isEmpty()
              || this.stack == null || this.stack.isEmpty()
              || !targetStack.is(this.stack.getItem())) {
            return;
          }
          String targetId = GunStackDataComponentUtil.readGunState(targetStack)
              .getString(GunAmmoStateUtil.GUN_INSTANCE_ID);
          String sourceId = GunStackDataComponentUtil.readGunState(this.stack)
              .getString(GunAmmoStateUtil.GUN_INSTANCE_ID);
          if (!targetId.isBlank() && !sourceId.isBlank() && !targetId.equals(sourceId)) {
            return;
          }
          var gun = Gun.get(targetStack).orElse(null);

          if (gun != null) {
            try {
              this.data.readerIndex(0);
            } catch (RuntimeException ignored) {
            }
            gun.decode(this.data);

            // The encoded Gun payload can be decoded into a freshly recreated client capability.
            // In that narrow case its provider may still be empty even though the authoritative
            // server ItemStack carried alongside this payload contains the newly loaded magazine.
            // Do not let that empty transient provider overwrite the authoritative component;
            // doing so made a successful reload turn into an empty magazine on the first shot.
            ItemStack authoritativeMagazine = this.stack == null || this.stack.isEmpty()
                ? ItemStack.EMPTY : GunAmmoStateUtil.getLoadedMagazineStack(this.stack);
            if (gun.getAmmoProvider() instanceof MagazineAmmoProvider magazineProvider
                && magazineProvider.getMagazine().map(Magazine::getSize).orElse(0) <= 0
                && !authoritativeMagazine.isEmpty()
                && gun.getAcceptedMagazines().contains(authoritativeMagazine.getItem())) {
              magazineProvider.setMagazineStack(authoritativeMagazine.copy());
              gun.setAmmoProvider(magazineProvider);
            }

            // Keep the current ItemStack object, but copy the authoritative gun CUSTOM_DATA from
            // the server stack and cache the decoded loaded magazine. In 1.21.1 the client can have
            // multiple Gun capability instances for the same visible stack; only updating the one
            // touched by this packet leaves older scheduled shooting tasks reading providerStack=air.
            if (this.stack != null && !this.stack.isEmpty()
                && targetStack != null && !targetStack.isEmpty()
                && targetStack.is(this.stack.getItem())) {
              var serverGunState = GunStackDataComponentUtil.readGunState(this.stack);
              // The server stack is authoritative even when it has an empty gun state. After a
              // drop/pickup or stale client shot rejection, leaving the old client CUSTOM_DATA in
              // place lets the client restore a cached magazine and locally shoot forever while
              // the server has providerStack=air. Always mirror the server state to clear stale
              // loadedMagazineStack/ammoProvider data.
              GunStackDataComponentUtil.writeGunState(targetStack, serverGunState);
            }
            if (gun.getAmmoProvider() != null) {
              var magazineStack = gun.getAmmoProvider().getMagazineStack();
              if (magazineStack != null && !magazineStack.isEmpty()) {
                GunAmmoStateUtil.writeLoadedMagazine(targetStack, magazineStack);
              } else {
                GunAmmoStateUtil.clearLoadedMagazine(targetStack);
              }
              GunAmmoStateUtil.cacheSyncedLoadedMagazine(this.entityId, this.slot,
                  targetStack, gun.getAmmoProvider());
              GunAmmoStateUtil.clearClientReloadPreview(this.entityId, this.slot, targetStack);
            }
            if (entity instanceof Player player && this.slot == EquipmentSlot.MAINHAND) {
              try {
                player.getInventory().setChanged();
              } catch (RuntimeException ignored) {
              }
            }
            int providerAmmo = gun.getAmmoProvider() == null ? -1
                : gun.getAmmoProvider().getMagazine().map(Magazine::getSize).orElse(-1);
            
          } else {
            
          }
        }));
    return true;
  }
}
