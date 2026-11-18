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

package com.craftingdead.core.client.gui;

import java.util.Random;
import org.jetbrains.annotations.Nullable;
import com.craftingdead.core.CraftingDead;
import com.craftingdead.core.client.ClientDist;
import com.craftingdead.core.client.util.RenderUtil;
import com.craftingdead.core.world.action.ActionObserver;
import com.craftingdead.core.world.effect.ModMobEffects;
import com.craftingdead.core.world.entity.extension.PlayerExtension;
import com.craftingdead.core.world.item.gun.Gun;
import com.craftingdead.core.world.item.gun.ammoprovider.AmmoProvider;
import com.craftingdead.core.world.item.gun.GunAmmoStateUtil;
import com.craftingdead.core.world.item.gun.magazine.Magazine;
import com.craftingdead.core.world.item.scope.Scope;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class IngameGui {

  private static final Random random = new Random();

  private static final ResourceLocation HEALTH =
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "textures/gui/health.png");
  private static final ResourceLocation SHIELD =
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "textures/gui/shield.png");

  private final Minecraft minecraft;

  private final ClientDist client;

  private ResourceLocation crosshairLocation;

  private float lastSpread;

  private float lastFlashScale = 0;

  @Nullable
  private HitMarker hitMarker;

  private ItemStack lastAmmoGunStack = ItemStack.EMPTY;
  private int lastAmmoCount;
  private long lastAmmoUpdateMs;

  public IngameGui(Minecraft minecraft, ClientDist client, ResourceLocation crosshairLocation) {
    this.minecraft = minecraft;
    this.client = client;
    this.crosshairLocation = crosshairLocation;
  }

  public void setCrosshairLocation(ResourceLocation crosshairLocation) {
    this.crosshairLocation = crosshairLocation;
  }

  public void setHitMarker(@Nullable HitMarker hitMarker) {
    this.hitMarker = hitMarker;
  }

  private void renderGunFlash(GuiGraphics guiGraphics, Gun gun, int width, int height,
      float partialTicks) {
    var poseStack = guiGraphics.pose();
    if (gun.getClient().isFlashing()) {
      poseStack.pushPose();
      {
        final var flashIntensity = (random.nextInt(3) + 5) / 10.0F;
        final var scale = this.lastFlashScale =
            Mth.lerp(partialTicks, this.lastFlashScale, flashIntensity);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, flashIntensity - 0.15F);
        var flashTexture = ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "textures/flash/white_flash.png");
        final float x = width * 0.625F;
        final float y = height * 0.625F;
        final float flashWidth = 300;
        final float flashHeight = 300;
        poseStack.translate((x - x * scale), y - y * scale, 0.0F);
        poseStack.scale(scale, scale, 1.0F);
        RenderUtil.blit(guiGraphics, flashTexture, x - flashWidth / 2, y - flashHeight / 2, flashWidth,
            flashHeight);
        RenderSystem.disableBlend();
      }
      poseStack.popPose();
    }
  }

  private static void renderScopeOverlay(PlayerExtension<AbstractClientPlayer> player,
      Scope scope, GuiGraphics guiGraphics, int width, int height) {
    scope.getOverlayTexture(player).ifPresent(overlayTexture -> {
      var overlayTextureWidth = scope.getOverlayTextureWidth();
      var overlayTextureHeight = scope.getOverlayTextureHeight();
      var scale = RenderUtil.getFitScale(overlayTextureWidth, overlayTextureHeight);
      overlayTextureWidth *= scale;
      overlayTextureHeight *= scale;
      RenderSystem.enableBlend();
      RenderUtil.blit(guiGraphics, overlayTexture, width / 2.0F - overlayTextureWidth / 2.0F,
          height / 2.0F - overlayTextureHeight / 2.0F, overlayTextureWidth,
          overlayTextureHeight);
      RenderSystem.disableBlend();
    });
  }

  public void renderFlashBangOverlay(Player player, GuiGraphics guiGraphics, int width, int height,
      float partialTick) {
    // Draws the flashbang effect. The real Crafting Dead flash should be a full white screen first,
    // then fade out. Use a high GUI Z and GuiGraphics.fill directly so it draws above the world/HUD
    // in Minecraft 1.21.1.
    if (player == null || guiGraphics == null) {
      return;
    }

    var flashEffect = player.getEffect(ModMobEffects.holder(ModMobEffects.FLASH_BLINDNESS));
    if (flashEffect != null && flashEffect.getDuration() > 0) {
      float fade = flashEffect.getDuration() > 35
          ? 1.0F
          : Mth.clamp((flashEffect.getDuration() - partialTick) / 35.0F, 0.0F, 1.0F);
      int alpha = Mth.clamp((int) (255.0F * fade), 0, 255);
      int color = (alpha << 24) | 0x00FFFFFF;

      RenderSystem.disableDepthTest();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      var poseStack = guiGraphics.pose();
      poseStack.pushPose();
      poseStack.translate(0.0F, 0.0F, 1000.0F);
      guiGraphics.fill(0, 0, width, height, color);
      poseStack.popPose();
      RenderSystem.disableBlend();
      RenderSystem.enableDepthTest();
    }
  }


  public void renderOverlay(PlayerExtension<AbstractClientPlayer> player, ItemStack heldStack,
      @Nullable Gun gun, GuiGraphics guiGraphics, int width, int height, float partialTick) {
    var poseStack = guiGraphics.pose();
    this.renderDamageOverlay(player.entity(), guiGraphics, width, height, partialTick);

    if (this.hitMarker != null) {
      if (this.hitMarker.render(poseStack, width, height, partialTick)) {
        this.hitMarker = null;
      }
    }

    Scope.get(heldStack)
        .filter(scope -> scope.isScoping(player))
        .ifPresent(scope -> renderScopeOverlay(player, scope, guiGraphics, width, height));

    player.getActionObserver()
        .flatMap(ActionObserver::getProgressBar)
        .ifPresent(observer -> renderProgress(guiGraphics, this.minecraft.font, width,
            height, observer.getMessage(), observer.getSubMessage().orElse(null),
            observer.getProgress(partialTick)));

    if (gun != null) {
      this.renderGunFlash(guiGraphics, gun, width, height, partialTick);
    }

    // Needs to render after blood or else it causes Z level issues
    if (gun != null) {
      this.renderAmmo(guiGraphics, width, height, gun);
    }

    if (player.isCombatModeEnabled()) {
      this.renderCombatMode(player, guiGraphics, width, height, partialTick);
    }

    // Keep flashbang above the custom HUD when the 1.21 overlay callback is available.
    this.renderFlashBangOverlay(player.entity(), guiGraphics, width, height, partialTick);
  }

  private void renderDamageOverlay(AbstractClientPlayer player, GuiGraphics guiGraphics,
      int width, int height, float partialTick) {
    if (!ClientDist.isDamageFlashEnabled() || player.hurtTime <= 0) {
      return;
    }

    float duration = Math.max(1.0F, player.hurtDuration);
    float remaining = Mth.clamp((player.hurtTime - partialTick) / duration, 0.0F, 1.0F);
    int alpha = Mth.clamp((int) (72.0F * remaining), 0, 72);
    guiGraphics.fill(0, 0, width, height, (alpha << 24) | 0x00A00000);
  }

  private void renderHandcuffsDamage(GuiGraphics guiGraphics, ItemStack handcuffs,
      int width, int height) {
    var poseStack = guiGraphics.pose();
    if (!handcuffs.isEmpty()) {
      final var mWidth = width / 2;
      final var mHeight = height / 2;
      final var damage = handcuffs.getMaxDamage() - handcuffs.getDamageValue();
      RenderUtil.drawCenteredString(guiGraphics, this.minecraft.font,
          Component.literal(damage + "/" + handcuffs.getMaxDamage()), mWidth + 1, mHeight + 10,
          0xFFFFFFFF);

      poseStack.pushPose();
      {
        poseStack.translate(mWidth - 20.0F, mHeight - 30.0F, 0.0F);
        final var scale = 2.5F;
        poseStack.scale(scale, scale, scale);
        guiGraphics.renderItem(handcuffs, 0, 0);
      }
      poseStack.popPose();
    }
  }

  private void renderAmmo(GuiGraphics guiGraphics, int width, int height, Gun gun) {
    var poseStack = guiGraphics.pose();
    int x = width - 115;
    int boxHeight = 25;

    RenderUtil.fillGradient(guiGraphics, x - 10, height - boxHeight, x + 30, height, 0x00000000,
        0x55000000);
    RenderUtil.fillRaw(guiGraphics, x + 30, height - boxHeight, x + 30 + 90, height, 0x55000000);

    AmmoProvider ammoProvider = gun.getAmmoProvider();

    int providerAmmo = ammoProvider.getMagazine().map(Magazine::getSize).orElse(0);
    int componentAmmo = GunAmmoStateUtil.getLoadedAmmo(gun.getItemStack());
    int cachedAmmo = this.minecraft.player == null ? 0
        : GunAmmoStateUtil.getCachedLoadedAmmo(this.minecraft.player.getId(),
            EquipmentSlot.MAINHAND, gun.getItemStack());
    int previewAmmo = this.minecraft.player == null ? 0
        : GunAmmoStateUtil.getClientReloadPreviewAmmo(this.minecraft.player.getId(),
            EquipmentSlot.MAINHAND, gun.getItemStack());
    int ammoCount = Math.max(Math.max(providerAmmo, componentAmmo), Math.max(cachedAmmo, previewAmmo));

    // Client-side network decode can briefly report AIR/0 while the next server sync is already
    // carrying the correct gun state. Keep the last positive ammo value for a short time to avoid
    // the HUD flickering back to "empty magazine" between sync packets.
    long nowMs = Util.getMillis();
    if (ammoCount > 0) {
      this.lastAmmoGunStack = gun.getItemStack().copy();
      this.lastAmmoCount = ammoCount;
      this.lastAmmoUpdateMs = nowMs;
    } else if (ammoProvider.getMagazineStack().isEmpty()
        && !this.lastAmmoGunStack.isEmpty()
        && gun.getItemStack().is(this.lastAmmoGunStack.getItem())
        && nowMs - this.lastAmmoUpdateMs <= 350L) {
      // Only bridge very short AIR/stale decode gaps. If an empty magazine is present,
      // show 0 immediately instead of keeping the last positive ammo value.
      ammoCount = this.lastAmmoCount;
    }

    int reserveSize = ammoProvider.getReserveSize();
    var empty = ammoCount <= 0 && reserveSize <= 0;

    var ammoText = empty ? I18n.get("hud.empty_magazine")
        : String.valueOf(ammoCount);
    int ammoTextWidth = this.minecraft.font.width(ammoText);

    float reserveTextScale = 0.6F;
    String reserveText = " / " + reserveSize;
    int reserveTextWidth =
        (int) (this.minecraft.font.width(reserveText) * reserveTextScale);

    guiGraphics.drawString(this.minecraft.font,
        ammoText,
        x + 55 - ammoTextWidth - (empty ? 0 : reserveTextWidth),
        height - (boxHeight / 2) - this.minecraft.font.lineHeight / 2,
        empty ? ChatFormatting.RED.getColor() : 0xFFFFFFFF, true);

    if (!empty) {
      poseStack.pushPose();
      poseStack.translate(x + 55 - reserveTextWidth,
          height - (boxHeight / 2) - (reserveTextScale * 2), 0);
      poseStack.scale(reserveTextScale, reserveTextScale, reserveTextScale);
      guiGraphics.drawString(this.minecraft.font,
          reserveText, 0, 0, empty ? ChatFormatting.RED.getColor() : 0xFFFFFFFF, true);
      poseStack.popPose();
    }

    String fireMode = I18n.get(gun.getFireMode().getTranslationKey());
    guiGraphics.drawString(this.minecraft.font,
        fireMode, x + 65, height - 16, 0xFFFFFFFF, true);
  }

  private static void renderProgress(GuiGraphics guiGraphics, Font font,
      int width, int height, Component message, @Nullable Component subMessage, float percent) {
    final int barWidth = 100;
    final int barHeight = 10;
    final int barColour = 0xC0FFFFFF;
    final float x = width / 2 - barWidth / 2;
    final float y = height / 2;
    guiGraphics.drawString(font, message.getString(), (int) x,
        (int) (y - barHeight - ((font.lineHeight / 2) + 0.5F)), 0xFFFFFF, true);
    RenderUtil.fillGradient(guiGraphics, x, y, x + barWidth * percent, y + barHeight, barColour,
        barColour);
    if (subMessage != null) {
      guiGraphics.drawString(font, subMessage.getString(), (int) x,
          (int) (y + barHeight + ((font.lineHeight / 2) + 0.5F)), 0xFFFFFF, true);
    }
  }

  private void renderCombatMode(PlayerExtension<AbstractClientPlayer> player,
      GuiGraphics guiGraphics, int width, int height, float partialTick) {
    var poseStack = guiGraphics.pose();
    final var inventory = player.entity().getInventory();

    int boxX = width - 115;
    int boxWidth = 110;
    int boxY = height - 170;
    int boxHeight = 30;
    int boxMarginY = 31;

    int currentItemIndex = inventory.selected;

    var rightPadding = 15;
    var topPadding = 3;

    // Render primary
    var primaryStack = inventory.getItem(0);
    RenderUtil.fill(guiGraphics, boxX, boxY, boxWidth, boxHeight, 0x66000000);
    if (currentItemIndex == 0) {
      RenderUtil.fill(guiGraphics, boxX + 1, boxY + 1, boxWidth - 2, boxHeight - 2, 0x66FFFFFF);
    }
    guiGraphics.drawString(this.minecraft.font, "1", boxX + 7, boxY + 6, 0xFFFFFFFF, true);
    RenderUtil.renderItemInCombatSlot(primaryStack,
        boxX + boxWidth - rightPadding, boxY + topPadding, poseStack, partialTick);


    // Render secondary
    var secondaryStack = inventory.getItem(1);
    boxY += boxMarginY;
    RenderUtil.fill(guiGraphics, boxX, boxY, boxWidth, boxHeight, 0x66000000);
    if (currentItemIndex == 1) {
      RenderUtil.fill(guiGraphics, boxX + 1, boxY + 1, boxWidth - 2, boxHeight - 2, 0x66FFFFFF);
    }
    guiGraphics.drawString(this.minecraft.font, "2", boxX + 7, boxY + 6, 0xFFFFFFFF, true);
    RenderUtil.renderItemInCombatSlot(secondaryStack,
        boxX + boxWidth - rightPadding, boxY + topPadding, poseStack, partialTick);

    // Render melee
    var meleeStack = inventory.getItem(2);
    boxY += boxMarginY;
    RenderUtil.fill(guiGraphics, boxX, boxY, boxWidth, boxHeight, 0x66000000);
    if (currentItemIndex == 2) {
      RenderUtil.fill(guiGraphics, boxX + 1, boxY + 1, boxWidth - 2, boxHeight - 2, 0x66FFFFFF);
    }
    guiGraphics.drawString(this.minecraft.font, "3", boxX + 7, boxY + 6, 0xFFFFFFFF, true);
    RenderUtil.renderItemInCombatSlot(meleeStack,
        boxX + boxWidth - rightPadding, boxY + topPadding, poseStack, partialTick);

    // Render extras
    boxY += boxMarginY;
    boxHeight = 25;
    boxWidth = 25;
    for (int i = 0; i < 4; i++) {
      ItemStack extraStack = inventory.getItem(3 + i);
      RenderUtil.fill(guiGraphics, boxX, boxY, boxWidth, boxHeight, 0x66000000);
      if (currentItemIndex == 3 + i) {
        RenderUtil.fill(guiGraphics, boxX + 1, boxY + 1, boxWidth - 2, boxHeight - 2,
            0x66FFFFFF);
      }
      guiGraphics.drawString(this.minecraft.font, String.valueOf(4 + i), boxX + 2,
          boxY + 1, 0xFFFFFFFF, true);

      poseStack.pushPose();
      {
        poseStack.translate(boxX + boxWidth / 2 - 16 / 2,
            boxY + (boxHeight / 2) - 6, 0.0D);
        RenderUtil.renderGuiItem(poseStack, extraStack, 1.0F);
      }
      poseStack.popPose();

      boxX += 28;
    }

    final int healthBoxHeight = 25;
    // Render Health
    final var health = player.entity().getHealth();
    int healthWidth = 100;

    RenderUtil.fillRaw(guiGraphics, 0, height - healthBoxHeight, healthWidth, height, 0x55000000);

    RenderUtil.fillGradient(guiGraphics, healthWidth, height - healthBoxHeight, healthWidth + 40,
        height, 0x55000000, 0x00000000);

    RenderSystem.enableBlend();
    RenderUtil.blit(guiGraphics, HEALTH, 5, height - healthBoxHeight / 2 - 8, 16, 16);
    RenderSystem.disableBlend();

    RenderUtil.drawCenteredString(guiGraphics, this.minecraft.font,
        String.valueOf(Math.round(health)), 31,
        height - healthBoxHeight / 2 - this.minecraft.font.lineHeight / 2, 0xFFFFFFFF);
    RenderUtil.fill(guiGraphics, 42, height - healthBoxHeight / 2 - 5, 65, 10, 0x66000000);
    RenderUtil.fill(guiGraphics, 42, height - healthBoxHeight / 2 - 5,
        Math.round(65 * (health / player.entity().getMaxHealth())), 10, 0xCCFFFFFF);

  }

  public void renderCrosshairs(GuiGraphics guiGraphics, float accuracy, float partialTick,
      int width, int height) {
    var poseStack = guiGraphics.pose();
    final var imageWidth = 16.0F;
    final var imageHeight = 16.0F;

    final var x = (width / 2.0F) - (imageWidth / 2.0F) - 0.5F;
    final var y = (height / 2.0F) - (imageHeight / 2.0F);

    final var newSpread = (1.0F - accuracy + 0.075F) * 60.0F;
    final var lerpSpread = Mth.lerp(0.5F, this.lastSpread, newSpread);
    final var crosshair = this.client.getCrosshairManager().getCrosshair(this.crosshairLocation);

    poseStack.pushPose();
    {
      RenderSystem.enableBlend();

      RenderUtil.blit(guiGraphics, crosshair.getMiddle(), x, y, imageWidth, imageHeight);

      RenderUtil.blit(guiGraphics, crosshair.getTop(), x, y - lerpSpread, imageWidth, imageHeight);

      RenderUtil.blit(guiGraphics, crosshair.getBottom(), x, y + lerpSpread, imageWidth, imageHeight);

      RenderUtil.blit(guiGraphics, crosshair.getLeft(), x - lerpSpread, y, imageWidth, imageHeight);

      RenderUtil.blit(guiGraphics, crosshair.getRight(), x + lerpSpread, y, imageWidth, imageHeight);
      RenderSystem.disableBlend();
    }
    poseStack.popPose();

    this.lastSpread = lerpSpread;
  }
}
