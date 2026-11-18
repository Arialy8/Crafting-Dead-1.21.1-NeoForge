/*
 * Crafting Dead 1.21.1 migration helper.
 */
package com.craftingdead.core.util;

import com.craftingdead.core.CraftingDead;
import com.craftingdead.core.world.action.ActionType;
import com.craftingdead.core.world.action.ActionTypes;
import com.craftingdead.core.world.item.gun.GunConfiguration;
import com.craftingdead.core.world.item.gun.GunConfigurations;
import com.craftingdead.core.world.item.gun.ammoprovider.AmmoProviderType;
import com.craftingdead.core.world.item.gun.ammoprovider.AmmoProviderTypes;
import com.craftingdead.core.world.item.gun.attachment.Attachment;
import com.craftingdead.core.world.item.gun.attachment.Attachments;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.core.particles.ParticleType;

/**
 * Replaces 1.18.x {@code IForgeRegistryEntry#getRegistryName()} and item/entity
 * {@code getRegistryName()} calls. In 1.20.5+/1.21.x registry names are resolved
 * from the owning registry instead of being stored on the object.
 */
public final class RegistryNameUtil {

  private static final ResourceLocation FALLBACK = ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "unknown");

  private RegistryNameUtil() {}

  public static ResourceLocation safe(ResourceLocation id) {
    return id == null ? FALLBACK : id;
  }

  public static ResourceLocation item(Item item) {
    return item == null ? FALLBACK : safe(BuiltInRegistries.ITEM.getKey(item));
  }

  public static ResourceLocation entityType(EntityType<?> entityType) {
    return entityType == null ? FALLBACK : safe(BuiltInRegistries.ENTITY_TYPE.getKey(entityType));
  }

  public static ResourceLocation sound(SoundEvent soundEvent) {
    return soundEvent == null ? FALLBACK : safe(BuiltInRegistries.SOUND_EVENT.getKey(soundEvent));
  }

  public static ResourceLocation particleType(ParticleType<?> particleType) {
    return particleType == null ? FALLBACK : safe(BuiltInRegistries.PARTICLE_TYPE.getKey(particleType));
  }

  public static ResourceLocation actionType(ActionType<?> actionType) {
    return actionType == null ? FALLBACK : safe(ActionTypes.REGISTRY.getKey(actionType));
  }

  public static ResourceLocation gunConfiguration(GunConfiguration configuration) {
    return configuration == null ? FALLBACK : safe(GunConfigurations.registry.getKey(configuration));
  }

  public static ResourceLocation ammoProviderType(AmmoProviderType type) {
    return type == null ? FALLBACK : safe(AmmoProviderTypes.registry.getKey(type));
  }

  public static ResourceLocation attachment(Attachment attachment) {
    return attachment == null ? FALLBACK : safe(Attachments.registry.getKey(attachment));
  }
}
