/*
 * Crafting Dead
 * Copyright (C) 2022  NexusNode LTD
 */

package com.craftingdead.core.mixin;

import net.minecraft.client.resources.model.ModelBakery;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Forge/Minecraft 1.21.1 changed ModelBakery internals. The old cache injection targets no longer
 * exist, so the mixin is kept as a harmless marker while item renderer model loading is migrated to
 * public client model events.
 */
@Mixin(ModelBakery.class)
public abstract class ModelBakeryMixin {
}
