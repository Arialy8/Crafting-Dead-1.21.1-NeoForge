/*
 * Crafting Dead
 * Copyright (C) 2022  NexusNode LTD
 */

package com.craftingdead.core.data.guns;

import com.craftingdead.core.util.RegistryNameUtil;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import com.craftingdead.core.world.item.gun.GunConfiguration;
import com.craftingdead.core.world.item.gun.GunConfigurations;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

public class GunDataProvider implements DataProvider {

  private static final Logger logger = LogUtils.getLogger();

  private final PackOutput packOutput;
  private final CompletableFuture<HolderLookup.Provider> lookupProvider;

  public GunDataProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
    this.packOutput = packOutput;
    this.lookupProvider = lookupProvider;
  }

  @Override
  public CompletableFuture<?> run(CachedOutput cache) {
    return this.lookupProvider.thenCompose(provider -> {
      RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, provider);
      return CompletableFuture.allOf(GunConfigurations.registry.stream()
          .map(gunType -> encodeGun(gunType, GunConfigurations.REGISTRY_KEY.location(), cache, ops))
          .toArray(CompletableFuture[]::new));
    });
  }

  private CompletableFuture<?> encodeGun(GunConfiguration gun, ResourceLocation registryLocation,
      CachedOutput cache, RegistryOps<JsonElement> ops) {
    Path outputFolder = packOutput.getOutputFolder();
    var gunId = RegistryNameUtil.gunConfiguration(gun);
    final String pathString = String.join("/", PackType.SERVER_DATA.getDirectory(),
        gunId.getNamespace(), registryLocation.getPath(),
        gunId.getPath() + ".json");
    final Path path = outputFolder.resolve(pathString);
    return GunConfiguration.DIRECT_CODEC.encodeStart(ops, gun)
        .resultOrPartial(msg -> logger.error("Failed to encode {}: {}", path, msg))
        .map(json -> DataProvider.saveStable(cache, json, path))
        .orElseGet(() -> CompletableFuture.completedFuture(null));
  }

  @Override
  public String getName() {
    return "Crafting Dead Guns";
  }
}
