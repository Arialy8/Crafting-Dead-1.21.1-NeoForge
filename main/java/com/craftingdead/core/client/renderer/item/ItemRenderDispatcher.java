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

package com.craftingdead.core.client.renderer.item;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import com.craftingdead.core.world.entity.extension.LivingExtension;
import com.craftingdead.core.world.item.GunItem;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

public class ItemRenderDispatcher implements ResourceManagerReloadListener {

  private static final String FILE_EXTENSION = ".json";
  private static final int PATH_SUFFIX_LENGTH = FILE_EXTENSION.length();
  private static final String DIRECTORY = "renderers/item";

  private static final Gson gson = new Gson();

  private static final Logger logger = LogUtils.getLogger();

  private final Minecraft minecraft = Minecraft.getInstance();

  /** Forge 1.21.x removed the old public registry delegate access. Items are registry singletons,
   * so custom renderers can be keyed directly by Item. */
  private Map<Item, CustomItemRenderer> renderers = Map.of();

  private Map<ResourceLocation, Set<ResourceLocation>> materials = Map.of();

  public Set<ResourceLocation> getTextures(ResourceLocation atlasLocation) {
    return this.materials.getOrDefault(atlasLocation, Collections.emptySet());
  }

  public CustomItemRenderer getItemRenderer(Item item) {
    return this.renderers.get(item);
  }

  public boolean renderItem(ItemStack itemStack, ItemDisplayContext transformType,
      @Nullable LivingExtension<?, ?> living, PoseStack poseStack, MultiBufferSource bufferSource,
      int packedLight, int packedOverlay) {
    if (itemStack == null || itemStack.isEmpty() || poseStack == null || bufferSource == null) {
      return false;
    }
    var renderer = this.getOrCreateItemRenderer(itemStack.getItem());
    if (renderer != null && renderer.handlePerspective(itemStack, transformType)) {
      renderer.render(itemStack, transformType, living, poseStack, bufferSource,
          packedLight, packedOverlay);
      return true;
    }
    return false;
  }

  private CustomItemRenderer getOrCreateItemRenderer(Item item) {
    var renderer = this.renderers.get(item);
    if (renderer != null || !(item instanceof GunItem)) {
      return renderer;
    }

    var itemLocation = BuiltInRegistries.ITEM.getKey(item);
    if (itemLocation == null) {
      return null;
    }

    var rendererLocation = ResourceLocation.fromNamespaceAndPath(itemLocation.getNamespace(),
        DIRECTORY + "/" + itemLocation.getPath() + FILE_EXTENSION);
    var resource = this.minecraft.getResourceManager().getResource(rendererLocation).orElse(null);
    if (resource == null) {
      return null;
    }

    try (
        var inputStream = resource.open();
        var reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      var json = GsonHelper.fromJson(gson, reader, JsonElement.class);
      if (json == null) {
        return null;
      }

      renderer = tryCreate(item, new Dynamic<>(JsonOps.INSTANCE, json));
      if (renderer != null) {
        var mutableRenderers = new java.util.HashMap<>(this.renderers);
        mutableRenderers.put(item, renderer);
        this.renderers = Map.copyOf(mutableRenderers);
      }
      return renderer;
    } catch (IllegalArgumentException | IOException | JsonParseException e) {
      logger.error("Couldn't parse fallback item renderer {} from {}", itemLocation, rendererLocation, e);
      return null;
    }
  }

  public Set<ResourceLocation> gatherItemRenderers(ResourceManager resourceManager,
      @Nullable ProfilerFiller profiler) {
    logger.debug("Gathering item renderers");
    if (resourceManager == null) {
      this.renderers = Map.of();
      this.materials = Map.of();
      return Collections.emptySet();
    }

    Set<ResourceLocation> modelDependencies = new HashSet<>();
    var renderers = ImmutableMap.<Item, CustomItemRenderer>builder();

    int i = DIRECTORY.length() + 1;

    for (var entry : resourceManager.listResources(DIRECTORY,
        file -> file.getPath().endsWith(FILE_EXTENSION)).entrySet()) {

      var location = entry.getKey();
      var resource = entry.getValue();
      var path = location.getPath();
      var pathNoFileExtension = ResourceLocation.fromNamespaceAndPath(location.getNamespace(),
          path.substring(i, path.length() - PATH_SUFFIX_LENGTH));

      var item = BuiltInRegistries.ITEM.get(pathNoFileExtension);
      if (item == null) {
        logger.warn("No matching item for item renderer: {}", pathNoFileExtension.toString());
        continue;
      }

      try (
          var inputStream = resource.open();
          var reader =
              new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
        var json = GsonHelper.fromJson(gson, reader, JsonElement.class);
        if (json != null) {
          var renderer = tryCreate(item, new Dynamic<>(JsonOps.INSTANCE, json));
          if (renderer != null) {
            renderers.put(item, renderer);
            modelDependencies.addAll(renderer.getModelDependencies());
          }
        } else {
          logger.error("Couldn't load data file {} from {} as it's null or empty",
              pathNoFileExtension, location);
        }
      } catch (IllegalArgumentException | IOException | JsonParseException e) {
        logger.error("Couldn't parse data file {} from {}", pathNoFileExtension, location,
            e);
      }
    }

    this.renderers = renderers.build();

    this.materials = this.renderers.values().stream()
        .map(CustomItemRenderer::getMaterials)
        .flatMap(Collection::stream)
        .collect(Collectors.groupingBy(Material::atlasLocation,
            Collectors.mapping(Material::texture, Collectors.toSet())));

    return modelDependencies;
  }

  private static <I extends Item, P extends ItemRendererProperties, T extends ItemRendererType<I, P>> CustomItemRenderer tryCreate(
      Item item, Dynamic<?> dynamic) {
    var parsedProperties = ItemRendererProperties.CODEC.parse(dynamic)
        .resultOrPartial(message -> logger.error("Failed to parse item renderer properties: {}", message));
    if (parsedProperties.isEmpty()) {
      return null;
    }

    @SuppressWarnings("unchecked")
    var properties = (P) parsedProperties.get();

    @SuppressWarnings("unchecked")
    var type = (ItemRendererType<I, P>) properties.getItemRendererType();
    if (type == null) {
      logger.error("Item renderer properties did not declare a renderer type");
      return null;
    }

    if (type.getItemType().isInstance(item)) {
      try {
        return type.create(type.getItemType().cast(item), properties);
      } catch (RuntimeException ex) {
        logger.error("Failed to create item renderer for {}", item, ex);
        return null;
      }
    } else {
      logger.error("Item renderer {} expects item of type: {}",
          ItemRendererTypes.getKey(type).map(Object::toString).orElse("<unregistered>"),
          type.getItemType().getName());
      return null;
    }
  }

  @Override
  public void onResourceManagerReload(ResourceManager resourceManager) {
    if (this.minecraft == null) {
      return;
    }
    this.gatherItemRenderers(resourceManager, null);
    if (this.minecraft.getEntityModels() != null) {
      this.renderers.values().stream()
          .filter(java.util.Objects::nonNull)
          .forEach(
              renderer -> renderer.refreshCachedModels(this.minecraft.getEntityModels()::bakeLayer));
    }
  }
}
