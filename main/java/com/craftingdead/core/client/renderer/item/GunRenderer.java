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


import com.craftingdead.core.client.util.ClientFrameTimeUtil;
import com.craftingdead.core.util.RegistryNameUtil;
import com.craftingdead.core.util.ItemStackNbtUtil;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;
import com.craftingdead.core.CraftingDead;
import com.craftingdead.core.client.model.geom.ModModelLayers;
import com.craftingdead.core.client.util.RenderUtil;
import com.craftingdead.core.util.EasingFunction;
import com.craftingdead.core.world.entity.extension.LivingExtension;
import com.craftingdead.core.world.inventory.GunCraftSlotType;
import com.craftingdead.core.world.item.GunItem;
import com.craftingdead.core.world.item.gun.Gun;
import com.craftingdead.core.world.item.gun.GunStackDataComponentUtil;
import com.craftingdead.core.world.item.gun.GunAmmoStateUtil;
import com.craftingdead.core.world.item.gun.attachment.Attachment;
import com.craftingdead.core.world.item.gun.attachment.Attachments;
import com.craftingdead.core.world.item.gun.skin.Paint;
import com.craftingdead.core.world.item.gun.skin.SkinLookupUtil;
import com.craftingdead.core.world.item.scope.Scope;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Transformation;
import org.joml.Vector3f;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.data.ModelData;

public class GunRenderer implements CombatSlotItemRenderer {

  private static final Pair<Transformation, Transformation> IDENTITY_HAND_TRANSFORMS =
      Pair.of(Transformation.identity(), Transformation.identity());

  private static final String GUN_TEXTURE_REFERENCE = "gun";

  private static final int FLASH_TEXTURE_CHANGE_TIMEOUT_MS = 250;

  private static final int SPRINT_TRANSITION_MS = 300;

  private static final int AIMING_TRANSITION_MS = 200;

  private static final RandomSource random = RandomSource.create();

  private final Minecraft minecraft = Minecraft.getInstance();

  private final Map<String, BakedModel> cachedModels = new HashMap<>();

  private final GunItem item;

  private final GunRendererProperties properties;

  private ModelPart muzzleFlashModel;

  private long lastFlashTime = 0;

  private long sprintStartTimeMs;
  private boolean wasSprinting;

  private long aimingStartTimeMs;
  private boolean wasAiming;

  public GunRenderer(GunItem item, GunRendererProperties properties) {
    this.item = item;
    this.properties = properties;
  }

  @Override
  public void renderInCombatSlot(ItemStack itemStack, PoseStack poseStack, float partialTick,
      MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
    var gun = Gun.get(itemStack).orElse(null);
    if (gun == null) {
      return;
    }
    this.renderGun(gun, false, true, itemStack.hasFoil(), 0.0F,
        ItemDisplayContext.GUI, partialTick, poseStack, bufferSource, packedLight,
        packedOverlay);
  }

  @Override
  public void rotateCamera(ItemStack itemStack, LivingEntity livingEntity, float partialTick,
      Vector3f rotations) {
    var gun = Gun.get(itemStack).orElse(null);
    if (gun != null) {
      gun.getClient().getAnimationController().applyCamera(partialTick, rotations);
    }
  }

  @Override
  public boolean handlePerspective(ItemStack item, ItemDisplayContext transformType) {
    return transformType != ItemDisplayContext.GUI
        && transformType != ItemDisplayContext.NONE;
  }

  @Override
  public void render(
      ItemStack itemStack,
      ItemDisplayContext transformType,
      @Nullable LivingExtension<?, ?> living,
      PoseStack poseStack,
      MultiBufferSource bufferSource,
      int packedLight,
      int packedOverlay) {

    var scope = Scope.get(itemStack).orElse(null);
    var scoping = scope != null && living != null && scope.isScoping(living);
    if (scoping && scope.getOverlayTexture(living).isPresent()) {
      return;
    }

    var partialTick = this.minecraft.isPaused() ? 1.0F : ClientFrameTimeUtil.getFrameTime(this.minecraft);

    var gun = Gun.get(itemStack).orElse(null);
    if (gun == null) {
      return;
    }

    poseStack.pushPose();
    {
      switch (transformType) {
        case FIRST_PERSON_LEFT_HAND:
        case FIRST_PERSON_RIGHT_HAND:
          if (living != null && living.entity() instanceof AbstractClientPlayer player) {
            this.renderFirstPerson(player, itemStack, gun, scoping, transformType,
                partialTick, poseStack, bufferSource, packedLight, packedOverlay);
          } else {
            this.renderGun(gun, true, false, itemStack.hasFoil(), 0.0F,
                transformType, partialTick, poseStack, bufferSource, packedLight,
                packedOverlay);
          }
          break;
        case THIRD_PERSON_LEFT_HAND:
        case THIRD_PERSON_RIGHT_HAND:
          gun.getClient().getAnimationController().apply(partialTick, poseStack);
          this.renderGun(gun, true, false, itemStack.hasFoil(), 0.0F,
              transformType, partialTick, poseStack, bufferSource, packedLight,
              packedOverlay);
          break;
        case HEAD:
          poseStack.pushPose();
          RenderUtil.applyTransformation(poseStack, this.properties.backTransform());
          this.renderGun(gun, true, false, itemStack.hasFoil(), 0.0F,
              transformType, partialTick, poseStack, bufferSource, packedLight,
              packedOverlay);
          poseStack.popPose();
          break;
        case FIXED:
          poseStack.scale(1.5F, 1.5F, 1.5F);
          poseStack.translate(-0.25F, 0, 0);
          poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
          poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
          this.renderGun(gun, true, false, itemStack.hasFoil(), 0.0F,
              transformType, partialTick, poseStack, bufferSource, packedLight,
              packedOverlay);
          break;
        default:
          break;
      }
    }
    poseStack.popPose();
  }

  private void renderFirstPersonArms(
      AbstractClientPlayer playerEntity,
      ItemStack itemStack,
      Gun gun,
      float aimingPct,
      float partialTicks,
      PoseStack poseStack,
      MultiBufferSource bufferSource,
      int packedLight) {

    final var playerRenderer =
        (PlayerRenderer) this.minecraft.getEntityRenderDispatcher().getRenderer(playerEntity);

    var mainArm = playerEntity.getMainArm();

    var heldTransforms = this.properties.handTransforms().getOrDefault(
        mainArm == HumanoidArm.RIGHT
            ? GunRendererProperties.HandTransform.HELD_RIGHT_HANDED
            : GunRendererProperties.HandTransform.HELD_LEFT_HANDED,
        IDENTITY_HAND_TRANSFORMS);

    Transformation rightHandTransforms;
    Transformation leftHandTransforms;
    if (aimingPct > 0) {
      var aimingTransforms = this.properties.handTransforms().getOrDefault(
          mainArm == HumanoidArm.RIGHT
              ? GunRendererProperties.HandTransform.AIMING_RIGHT_HANDED
              : GunRendererProperties.HandTransform.AIMING_LEFT_HANDED,
          IDENTITY_HAND_TRANSFORMS);
      rightHandTransforms = slerp(heldTransforms.getFirst(),
          aimingTransforms.getFirst(), aimingPct);
      leftHandTransforms = slerp(heldTransforms.getSecond(),
          aimingTransforms.getSecond(), aimingPct);
    } else {
      rightHandTransforms = heldTransforms.getFirst();
      leftHandTransforms = heldTransforms.getSecond();
    }

    // Right Arm
    poseStack.pushPose();
    {
      gun.getClient().getAnimationController().applyArm(
          playerEntity.getMainArm() == HumanoidArm.RIGHT
              ? InteractionHand.MAIN_HAND
              : InteractionHand.OFF_HAND,
          HumanoidArm.RIGHT,
          partialTicks, poseStack);
      poseStack.translate(0.75F, -0.75F, -0.6F);

      RenderUtil.applyTransformation(poseStack, rightHandTransforms);
      {
        poseStack.mulPose(Axis.YP.rotationDegrees(95.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(100.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));

        poseStack.translate(0.9F, -0.75F, 0.5F);

        playerRenderer.renderRightHand(poseStack, bufferSource, packedLight, playerEntity);
      }
    }
    poseStack.popPose();

    // Left Arm
    poseStack.pushPose();
    {
      gun.getClient().getAnimationController().applyArm(
          playerEntity.getMainArm() == HumanoidArm.LEFT ? InteractionHand.MAIN_HAND
              : InteractionHand.OFF_HAND,
          HumanoidArm.LEFT,
          partialTicks, poseStack);

      poseStack.translate(0.75F, -0.75F, -0.75F);

      RenderUtil.applyTransformation(poseStack, leftHandTransforms);
      {
        poseStack.mulPose(Axis.YP.rotationDegrees(95.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(95.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(120.0F));

        poseStack.translate(0.05F, -1.05F, 0.30F);

        playerRenderer.renderLeftHand(poseStack, bufferSource, packedLight, playerEntity);
      }
    }
    poseStack.popPose();
  }

  private void renderFlash(Gun gun, PoseStack matrixStack, MultiBufferSource bufferSource,
      int packedOverlay) {
    if (this.muzzleFlashModel == null) {
      try {
        if (this.minecraft != null && this.minecraft.getEntityModels() != null) {
          this.muzzleFlashModel = this.minecraft.getEntityModels().bakeLayer(ModModelLayers.MUZZLE_FLASH);
        }
      } catch (RuntimeException ignored) {
        // Muzzle flash is visual-only. If the model layer has not been baked yet,
        // skip this frame instead of crashing the client when firing.
      }
      if (this.muzzleFlashModel == null) {
        return;
      }
    }

    matrixStack.pushPose();
    {
      var currentTime = Util.getMillis();
      var deltaTime = currentTime - this.lastFlashTime;

      int texture;
      if (deltaTime > FLASH_TEXTURE_CHANGE_TIMEOUT_MS) {
        this.lastFlashTime = currentTime;
        deltaTime = FLASH_TEXTURE_CHANGE_TIMEOUT_MS;
      }

      // Every n milliseconds, change the muzzle flash texture
      texture = (int) ((deltaTime / 100) + 1);
      var randomScale = (random.nextInt(8) + 3) / 10.0F;

      var transform = this.properties.muzzleFlashTransform();

      var scale = transform.getScale();
      matrixStack.scale(
          scale.x() + randomScale,
          scale.y() + randomScale,
          scale.z() + randomScale);

      var x = transform.getTranslation().x();
      var y = transform.getTranslation().y();
      var z = transform.getTranslation().z();
      if (this.getRenderAttachments(gun).containsValue(Attachments.SUPPRESSOR.get())) {
        z -= 0.4;
      }

      matrixStack.translate(
          x - randomScale * x / 2,
          y - randomScale * y / 2,
          z - randomScale * z / 2);

      var flashBuffer = bufferSource.getBuffer(RenderType.beaconBeam(
          ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "textures/flash/flash" + texture + ".png"),
          true));
      this.muzzleFlashModel.render(matrixStack, flashBuffer, RenderUtil.FULL_LIGHT,
          packedOverlay);
    }
    matrixStack.popPose();
  }

  private void renderFirstPerson(
      AbstractClientPlayer playerEntity,
      ItemStack itemStack,
      Gun gun,
      boolean aiming,
      ItemDisplayContext transformType,
      float partialTicks,
      PoseStack poseStack,
      MultiBufferSource renderTypeBuffer,
      int packedLight,
      int packedOverlay) {

    final var flash = gun.getClient().isFlashing();
    if (flash) {
      packedLight = RenderUtil.FULL_LIGHT;
    }

    // ============== Animations ==============

    if (this.wasSprinting != playerEntity.isSprinting()) {
      this.sprintStartTimeMs = Util.getMillis();
      this.wasSprinting = playerEntity.isSprinting();
    }

    var sprintingPct = Mth.clamp(
        (float) (Util.getMillis() - this.sprintStartTimeMs) / SPRINT_TRANSITION_MS,
        0.0F, 1.0F);
    if (!playerEntity.isSprinting()) {
      sprintingPct = 1.0F - sprintingPct;
    }

    sprintingPct = EasingFunction.SINE_IN_OUT.apply(sprintingPct);

    if (this.wasAiming != aiming) {
      this.aimingStartTimeMs = Util.getMillis();
      this.wasAiming = aiming;
    }

    var aimingPct = Mth.clamp(
        (float) (Util.getMillis() - this.aimingStartTimeMs) / AIMING_TRANSITION_MS,
        0.0F, 1.0F);
    if (!aiming) {
      aimingPct = 1.0F - aimingPct;
    }

    aimingPct = EasingFunction.SINE_IN_OUT.apply(aimingPct);

    // ============== Animations End ==============

    poseStack.pushPose();
    if (sprintingPct > 0) {
      RenderUtil.applyTransformation(poseStack,
          slerp(Transformation.identity(), this.properties.sprintingTransform(), sprintingPct));
    }
    {
      if (!aiming && flash) {
        this.renderFlash(gun, poseStack, renderTypeBuffer, packedOverlay);
      }

      this.renderFirstPersonArms(playerEntity, itemStack, gun, aimingPct, partialTicks,
          poseStack, renderTypeBuffer, packedLight);

      gun.getClient().getAnimationController().apply(partialTicks, poseStack);

      this.renderGun(gun, true, false, itemStack.hasFoil(), aimingPct, transformType,
          partialTicks, poseStack, renderTypeBuffer, packedLight, packedOverlay);
    }
    poseStack.popPose();
  }

  private Map<GunCraftSlotType, Attachment> getRenderAttachments(Gun gun) {
    var liveAttachments = gun.getAttachments();
    if (liveAttachments != null && !liveAttachments.isEmpty()) {
      return liveAttachments;
    }

    var itemStack = gun.getItemStack();
    if (itemStack == null || itemStack.isEmpty()) {
      return Map.of();
    }

    var state = GunStackDataComponentUtil.readGunState(itemStack);
    if (!state.contains(GunStackDataComponentUtil.ATTACHMENTS, Tag.TAG_LIST)) {
      return Map.of();
    }

    Map<GunCraftSlotType, Attachment> attachments = new HashMap<>();
    for (var tag : state.getList(GunStackDataComponentUtil.ATTACHMENTS, Tag.TAG_STRING)) {
      var id = ResourceLocation.tryParse(tag.getAsString());
      var attachment = id == null ? null : Attachments.registry.get(id);
      if (attachment != null && attachment.getInventorySlot() != null) {
        attachments.put(attachment.getInventorySlot(), attachment);
      }
    }
    return attachments.isEmpty() ? Map.of() : attachments;
  }

  @Nullable
  private ResourceLocation getSkinTextureLocation(Gun gun) {
    var skin = gun.getSkin();
    if (skin == null) {
      skin = Paint.get(gun.getPaintStack())
          .map(Paint::getSkin)
          .map(holder -> holder == null ? null : holder.value())
          .orElse(null);
    }

    if (skin == null) {
      var state = GunStackDataComponentUtil.readGunState(gun.getItemStack());
      var skinHolder = SkinLookupUtil.load(state);
      if (skinHolder != null) {
        skin = skinHolder.value();
      }
      if (skin == null && state.contains(GunStackDataComponentUtil.PAINT_STACK, Tag.TAG_COMPOUND)) {
        skin = Paint.get(ItemStackNbtUtil.parseOptional(state.getCompound(GunStackDataComponentUtil.PAINT_STACK)))
            .map(Paint::getSkin)
            .map(holder -> holder == null ? null : holder.value())
            .orElse(null);
      }
    }

    var gunName = RegistryNameUtil.item(gun.getItemStack().getItem());
    // Old stack data can retain a skin belonging to another weapon. Never apply
    // that weapon's UV sheet to this model.
    if (skin == null || !skin.getAcceptedGuns().contains(gunName)) {
      return null;
    }
    var texture = skin.getTextureLocation(gunName);
    var resource = ResourceLocation.fromNamespaceAndPath(texture.getNamespace(),
        "textures/" + texture.getPath() + ".png");
    return this.minecraft.getResourceManager().getResource(resource).isPresent() ? texture : null;
  }

  private void renderGun(
      Gun gun,
      boolean renderAttachments,
      boolean renderDefaultMagazine,
      boolean foil,
      float aimingPct,
      ItemDisplayContext transformType,
      float partialTick,
      PoseStack poseStack,
      MultiBufferSource bufferSource,
      int packedLight,
      int packedOverlay) {

    var attachmentsForRender = this.getRenderAttachments(gun);
    var skinTextureLocation = this.getSkinTextureLocation(gun);
    String paintedGunName = RegistryNameUtil.item(gun.getItemStack().getItem()).getPath();
    boolean legacyPaint = skinTextureLocation != null && switch (paintedGunName) {
      case "m4a1", "p250", "p90", "scarl", "as50", "m107", "m9",
          "dmr", "g18", "mac10", "minigun", "vector" -> true;
      default -> false; // AK47, AWP and Desert Eagle already have working paint mappings.
    };
    ResourceLocation bodyModel = legacyPaint
        ? ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "gun/" + paintedGunName + "_painted")
        : this.properties.modelLocation();
    ResourceLocation bodyTexture = legacyPaint
        ? ResourceLocation.fromNamespaceAndPath(skinTextureLocation.getNamespace(),
            skinTextureLocation.getPath().replace("gun/", "gun/legacy/"))
        : skinTextureLocation;

    final var color = Paint.get(gun.getPaintStack())
        .map(paint -> paint.getColor().orElse(0xFFFFFFFF))
        .orElse(0xFFFFFFFF);

    Map<String, Either<Material, String>> textures = null;
    if (skinTextureLocation != null) {
      textures = Map.of(GUN_TEXTURE_REFERENCE,
          Either.left(new Material(InventoryMenu.BLOCK_ATLAS, bodyTexture)));
    }
    var bakedModel = this.getBakedModel(bodyModel, textures);

    var tempPoseStack = new PoseStack();
    bakedModel = bakedModel.applyTransform(transformType, tempPoseStack, false);
    var normalTransform = new Transformation(tempPoseStack.last().pose());

    var overbarrelAttachment = attachmentsForRender.get(GunCraftSlotType.OVERBARREL_ATTACHMENT);
    var hasIronSight = overbarrelAttachment == null;
    var perspectiveTransform = aimingPct > 0
        ? slerp(normalTransform,
            hasIronSight
                ? this.properties.aimTransform()
                : this.properties.scopeAimTransform().getOrDefault(
                    RegistryNameUtil.attachment(overbarrelAttachment),
                    this.properties.aimTransform()),
            aimingPct)
        : normalTransform;

    poseStack.pushPose();
    RenderUtil.applyTransformation(poseStack, perspectiveTransform);
    {
      this.renderBakedModel(bakedModel, foil, color, transformType,
          poseStack, bufferSource, packedLight, packedOverlay);

      var magazineStack = renderDefaultMagazine
          ? gun.getDefaultMagazineStack()
          : gun.getAmmoProvider().getMagazineStack();
      if (magazineStack.isEmpty()) {
        magazineStack = GunAmmoStateUtil.getLoadedMagazineStack(gun.getItemStack());
      }
      if (!magazineStack.isEmpty()) {
        poseStack.pushPose();
        gun.getClient().getAnimationController().applyMagazine(partialTick, poseStack);
        this.renderMagazine(magazineStack, legacyPaint ? null : skinTextureLocation, color, foil, transformType,
            poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
      }

      if (renderAttachments) {
        this.renderAttachments(gun, attachmentsForRender, legacyPaint ? null : skinTextureLocation, color, foil, transformType,
            partialTick, poseStack, bufferSource, packedLight, packedOverlay);
      }
    }
    poseStack.popPose();
  }

  protected final void renderBakedModel(BakedModel bakedModel, boolean foil, int colour,
      ItemDisplayContext transformType, PoseStack poseStack,
      MultiBufferSource renderTypeBuffer, int packedLight, int packedOverlay) {
    poseStack.pushPose();
    {
      poseStack.translate(-0.5D, -0.5D, -0.5D);
      VertexConsumer vertexBuilder = ItemRenderer.getFoilBuffer(renderTypeBuffer,
          Sheets.cutoutBlockSheet(), true, foil);
      var bakedQuads = bakedModel.getQuads(null, null, random);
      var pose = poseStack.last();
      for (var bakedQuad : bakedQuads) {
        var red = (colour >> 16 & 255) / 255.0F;
        var green = (colour >> 8 & 255) / 255.0F;
        var blue = (colour & 255) / 255.0F;
        vertexBuilder.putBulkData(pose, bakedQuad, red, green, blue, 1.0F, packedLight,
            packedOverlay);
      }
    }
    poseStack.popPose();
  }

  private BakedModel getBakedModel(ResourceLocation modelLocation,
      @Nullable Map<String, Either<Material, String>> textures) {
    var cacheKey = modelLocation + "|" + (textures == null ? "default" : textures.toString());
    return this.cachedModels.computeIfAbsent(cacheKey, key -> {
          if (textures == null || textures.isEmpty()) {
            return this.minecraft.getModelManager()
                .getModel(new ModelResourceLocation(modelLocation, "standalone"));
          }

          return this.bakeModelWithTextureOverrides(modelLocation, textures);
        });
  }

  private BakedModel bakeModelWithTextureOverrides(ResourceLocation modelLocation,
      Map<String, Either<Material, String>> textures) {
    try {
      var resourceLocation = ResourceLocation.fromNamespaceAndPath(modelLocation.getNamespace(),
          "models/" + modelLocation.getPath() + ".json");
      var resource = this.minecraft.getResourceManager().getResourceOrThrow(resourceLocation);
      JsonObject root;
      try (var inputStream = resource.open();
          var reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
        root = JsonParser.parseReader(reader).getAsJsonObject();
      }

      var textureObject = root.has("textures") && root.get("textures").isJsonObject()
          ? root.getAsJsonObject("textures")
          : new JsonObject();
      for (var textureOverride : textures.entrySet()) {
        var value = textureOverride.getValue()
            .map(material -> material.texture().toString(), reference -> reference);
        textureObject.addProperty(textureOverride.getKey(), value);
      }
      root.add("textures", textureObject);
      // Composite children own their texture bindings. Only override the retro body,
      // never the current iron sights (which retain the current model's UV layout).
      if (root.has("children") && root.getAsJsonObject("children").has("body")) {
        root.getAsJsonObject("children").getAsJsonObject("body").add("textures", textureObject.deepCopy());
      }

      var blockModel = BlockModel.fromStream(new StringReader(root.toString()));
      blockModel.name = modelLocation.toString();
      blockModel.resolveParents(this::loadUnbakedModel);

      var spriteGetter = this.createSpriteGetter();
      var modelBaker = new TextureOverrideModelBaker(spriteGetter);
      var bakedModel = blockModel.bake(modelBaker, spriteGetter, BlockModelRotation.X0_Y0);
      return bakedModel == null
          ? this.minecraft.getModelManager().getMissingModel()
          : bakedModel;
    } catch (Exception exception) {
      com.mojang.logging.LogUtils.getLogger().warn(
          "Could not bake weapon paint for {} with {}", modelLocation, textures, exception);
      return this.minecraft.getModelManager().getModel(new ModelResourceLocation(modelLocation, "standalone"));
    }
  }

  private Function<Material, TextureAtlasSprite> createSpriteGetter() {
    return material -> this.minecraft.getModelManager()
        .getAtlas(material.atlasLocation())
        .getSprite(material.texture());
  }

  private UnbakedModel loadUnbakedModel(ResourceLocation modelLocation) {
    try {
      var resourceLocation = ResourceLocation.fromNamespaceAndPath(modelLocation.getNamespace(),
          "models/" + modelLocation.getPath() + ".json");
      var resource = this.minecraft.getResourceManager().getResourceOrThrow(resourceLocation);
      try (var inputStream = resource.open();
          var reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
        var blockModel = BlockModel.fromStream(reader);
        blockModel.name = modelLocation.toString();
        return blockModel;
      }
    } catch (Exception ignored) {
      return null;
    }
  }

  private final class TextureOverrideModelBaker implements ModelBaker {

    private final Function<Material, TextureAtlasSprite> spriteGetter;

    private TextureOverrideModelBaker(Function<Material, TextureAtlasSprite> spriteGetter) {
      this.spriteGetter = spriteGetter;
    }

    @Override
    public UnbakedModel getModel(ResourceLocation location) {
      return GunRenderer.this.loadUnbakedModel(location);
    }

    @Override
    public UnbakedModel getTopLevelModel(ModelResourceLocation location) {
      return this.getModel(location.id());
    }

    @Override
    public Function<Material, TextureAtlasSprite> getModelTextureGetter() {
      return this.spriteGetter;
    }

    @Override
    public BakedModel bake(ResourceLocation location, ModelState state) {
      return this.bake(location, state, this.spriteGetter);
    }

    @Override
    public BakedModel bake(ResourceLocation location, ModelState state,
        Function<Material, TextureAtlasSprite> sprites) {
      var model = this.getModel(location);
      if (model == null) {
        return GunRenderer.this.minecraft.getModelManager().getMissingModel();
      }
      var bakedModel = model.bake(this, sprites, state);
      return bakedModel == null
          ? GunRenderer.this.minecraft.getModelManager().getMissingModel()
          : bakedModel;
    }

    public BakedModel bakeUncached(UnbakedModel model, ModelState state) {
      return model.bake(this, this.spriteGetter, state);
    }

    public BakedModel bakeUncached(UnbakedModel model, ModelState state,
        Function<Material, TextureAtlasSprite> sprites) {
      return model.bake(this, sprites, state);
    }
  }

  private void renderAttachments(
      Gun gun,
      Map<GunCraftSlotType, Attachment> renderAttachments,
      @Nullable ResourceLocation skinTextureLocation,
      int colour,
      boolean foil,
      ItemDisplayContext transformType,
      float partialTicks,
      PoseStack matrixStack,
      MultiBufferSource renderTypeBuffer,
      int packedLight,
      int packedOverlay) {

    if (!renderAttachments.containsKey(GunCraftSlotType.OVERBARREL_ATTACHMENT)) {
      for (var ironSight : this.properties.ironSights()) {
        matrixStack.pushPose();
        RenderUtil.applyTransformation(matrixStack, ironSight.getSecond());
        {
          var bakedModel = this.getBakedModel(ironSight.getFirst(),
              Map.of(GUN_TEXTURE_REFERENCE, Either.left(skinTextureLocation == null
                  ? this.getGunRenderMaterial()
                  : new Material(InventoryMenu.BLOCK_ATLAS, skinTextureLocation))));
          this.renderBakedModel(bakedModel, foil, colour, transformType, matrixStack,
              renderTypeBuffer, packedLight, packedOverlay);
        }
        matrixStack.popPose();
      }
    }

    for (var attachment : renderAttachments.values()) {
      var transform = this.properties.attachmentTransforms()
          .getOrDefault(RegistryNameUtil.attachment(attachment), Transformation.identity());
      matrixStack.pushPose();
      RenderUtil.applyTransformation(matrixStack, transform);
      {
        var bakedModel =
            this.getBakedModel(getAttachmentModelLocation(RegistryNameUtil.attachment(attachment)), null);
        this.renderBakedModel(bakedModel, foil, colour, transformType, matrixStack,
            renderTypeBuffer, packedLight, packedOverlay);
      }
      matrixStack.popPose();
    }
  }

  private void renderMagazine(
      ItemStack magazineStack,
      @Nullable ResourceLocation skinTextureLocation,
      int color,
      boolean foil,
      ItemDisplayContext transformType,
      PoseStack poseStack,
      MultiBufferSource renderTypeBuffer,
      int packedLight,
      int packedOverlay) {
    var transform = this.properties.magazineTransforms()
        .getOrDefault(RegistryNameUtil.item(magazineStack.getItem()), Transformation.identity());
    poseStack.pushPose();
    RenderUtil.applyTransformation(poseStack, transform);
    {
      var modelLocation = getMagazineModelLocation(RegistryNameUtil.item(magazineStack.getItem()));

      var magazineBakedModel = this.getBakedModel(modelLocation,
          Map.of(GUN_TEXTURE_REFERENCE, Either.left(skinTextureLocation == null
              ? this.getGunRenderMaterial()
              : new Material(InventoryMenu.BLOCK_ATLAS, skinTextureLocation))));

      this.renderBakedModel(magazineBakedModel, foil, color, transformType, poseStack,
          renderTypeBuffer, packedLight, packedOverlay);
    }
    poseStack.popPose();
  }

  private Material getGunRenderMaterial() {
    return this.resolveModelMaterial(this.properties.modelLocation(), GUN_TEXTURE_REFERENCE);
  }

  private Material resolveModelMaterial(ResourceLocation modelLocation, String textureReference) {
    var resourceManager = this.minecraft.getResourceManager();
    var resourceLocation = ResourceLocation.fromNamespaceAndPath(modelLocation.getNamespace(),
        "models/" + modelLocation.getPath() + ".json");
    try {
      var resource = resourceManager.getResourceOrThrow(resourceLocation);
      try (var inputStream = resource.open();
          var reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        if (root.has("textures")) {
          String resolved = this.resolveTextureReference(root.getAsJsonObject("textures"), textureReference);
          if (resolved != null && !resolved.isBlank()) {
            return new Material(InventoryMenu.BLOCK_ATLAS, ResourceLocation.parse(resolved));
          }
        }
      }
    } catch (Exception ignored) {
    }
    return new Material(InventoryMenu.BLOCK_ATLAS, MissingTextureAtlasSprite.getLocation());
  }

  @Nullable
  private String resolveTextureReference(JsonObject textures, String key) {
    if (textures == null || key == null || key.isBlank() || !textures.has(key)) {
      return null;
    }
    String value = textures.get(key).getAsString();
    if (value.startsWith("#")) {
      return this.resolveTextureReference(textures, value.substring(1));
    }
    return value;
  }

  @Override
  public Collection<ResourceLocation> getModelDependencies() {
    final Set<ResourceLocation> dependencies = new HashSet<>();
    dependencies.addAll(this.properties.ironSights().stream()
        .map(Pair::getFirst)
        .collect(Collectors.toSet()));
    dependencies.addAll(this.item.getAcceptedAttachments().stream()
        .map(RegistryNameUtil::attachment)
        .map(GunRenderer::getAttachmentModelLocation)
        .collect(Collectors.toSet()));
    dependencies.addAll(this.item.getAcceptedMagazines().stream()
        .map(RegistryNameUtil::item)
        .map(GunRenderer::getMagazineModelLocation)
        .collect(Collectors.toSet()));
    dependencies.add(this.properties.modelLocation());
    return dependencies;
  }

  @Override
  public Collection<Material> getMaterials() {
    return Collections.emptySet();
  }

  @Override
  public void refreshCachedModels(Function<ModelLayerLocation, ModelPart> modelBaker) {
    this.cachedModels.clear();
    this.muzzleFlashModel = modelBaker.apply(ModModelLayers.MUZZLE_FLASH);
  }

  @Override
  public ItemRendererType<?, ?> getType() {
    return ItemRendererTypes.GUN;
  }

  private static ResourceLocation getAttachmentModelLocation(ResourceLocation attachmentName) {
    attachmentName = RegistryNameUtil.safe(attachmentName);
    return ResourceLocation.fromNamespaceAndPath(attachmentName.getNamespace(),
        "attachment/" + attachmentName.getPath());
  }

  private static ResourceLocation getMagazineModelLocation(ResourceLocation magazineName) {
    magazineName = RegistryNameUtil.safe(magazineName);
    return ResourceLocation.fromNamespaceAndPath(magazineName.getNamespace(), "magazine/" + magazineName.getPath());
  }

  public static LayerDefinition createMuzzleFlashBodyLayer() {
    var mesh = new MeshDefinition();
    var root = mesh.getRoot();
    root.addOrReplaceChild("box0",
        CubeListBuilder.create()
            .mirror()
            .texOffs(1, 1)
            .addBox(0F, 0F, 0F, 8, 8, 0)
            .mirror(false),
        PartPose.offset(-4F, -4F, 0F));
    root.addOrReplaceChild("box1",
        CubeListBuilder.create()
            .mirror()
            .texOffs(9, 1)
            .addBox(-4F, 0F, 0F, 8, 0, 15)
            .mirror(false),
        PartPose.offsetAndRotation(0F, 0F, -15F, 0F, 0F, -0.7853982F));
    root.addOrReplaceChild("box2",
        CubeListBuilder.create()
            .mirror()
            .texOffs(1, 17)
            .addBox(-4F, 0F, 0F, 8, 0, 15)
            .mirror(false),
        PartPose.offsetAndRotation(0F, 0F, -15F, 0F, 0F, -2.373648F));
    return LayerDefinition.create(mesh, 64, 64);
  }

  private static Transformation slerp(Transformation a, Transformation b, float delta) {
    if (delta <= 0.0F) return a;
    if (delta >= 1.0F) return b;
    var translation = new org.joml.Vector3f(a.getTranslation());
    translation.lerp(b.getTranslation(), delta);
    var leftRotation = new org.joml.Quaternionf(a.getLeftRotation());
    leftRotation.slerp(b.getLeftRotation(), delta);
    var scale = new org.joml.Vector3f(a.getScale());
    scale.lerp(b.getScale(), delta);
    var rightRotation = new org.joml.Quaternionf(a.getRightRotation());
    rightRotation.slerp(b.getRightRotation(), delta);
    return new Transformation(translation, leftRotation, scale, rightRotation);
  }
}
