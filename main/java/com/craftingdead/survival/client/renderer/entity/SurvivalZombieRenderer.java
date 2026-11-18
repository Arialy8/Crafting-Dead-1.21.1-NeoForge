package com.craftingdead.survival.client.renderer.entity;

import com.craftingdead.core.client.renderer.entity.layers.ClothingLayer;
import com.craftingdead.core.client.renderer.entity.layers.EquipmentLayer;
import com.craftingdead.core.world.item.equipment.Equipment;
import com.craftingdead.survival.CraftingDeadSurvival;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;

public class SurvivalZombieRenderer extends ZombieRenderer {

  private static final Map<String, Integer> TEXTURES = Map.ofEntries(
      Map.entry("advanced_zombie", 0),
      Map.entry("alfa_zombie", 1),
      Map.entry("bounty_hunter_zombie", 2),
      Map.entry("desert_raider_zombie", 3),
      Map.entry("doctor_zombie", 4),
      Map.entry("fast_zombie", 5),
      Map.entry("firefighter_zombie", 6),
      Map.entry("giant_zombie", 7),
      Map.entry("hazmat_zombie", 8),
      Map.entry("juggernaut_zombie", 9),
      Map.entry("miner_zombie", 10),
      Map.entry("ninja_zombie", 11),
      Map.entry("pilot_zombie", 12),
      Map.entry("police_zombie", 13),
      Map.entry("scout_zombie", 14),
      Map.entry("sniper_zombie", 15),
      Map.entry("soldier_zombie", 16),
      Map.entry("swat_zombie", 17),
      Map.entry("tank_zombie", 18),
      Map.entry("weak_zombie", 19));

  public SurvivalZombieRenderer(EntityRendererProvider.Context context) {
    super(context);
    // 外显装备层（与玩家一致）：衣服覆盖身体模型，帽子/背心/背包挂在对应骨骼上。
    // 装备数据由 equipProfession 写入 LivingExtension，玩家靠近时经 SyncLivingMessage 同步。
    this.addLayer(new ClothingLayer<>(this));
    this.addLayer(EquipmentLayer.builder(this)
        .slot(Equipment.Slot.VEST)
        .attachment(EquipmentLayer.Attachment.BODY)
        .build());
    this.addLayer(EquipmentLayer.builder(this)
        .slot(Equipment.Slot.HAT)
        .attachment(EquipmentLayer.Attachment.HEAD)
        .build());
    this.addLayer(EquipmentLayer.builder(this)
        .slot(Equipment.Slot.BACKPACK)
        .useCrouchOrientation(true)
        .build());
  }

  @Override
  public ResourceLocation getTextureLocation(Zombie zombie) {
    String name = BuiltInRegistries.ENTITY_TYPE.getKey(zombie.getType()).getPath();
    int texture = Math.floorMod(zombie.getUUID().hashCode(), 20);
    return ResourceLocation.fromNamespaceAndPath(CraftingDeadSurvival.ID,
        "textures/entity/zombie/zombie" + texture + ".png");
  }

  @Override
  protected void scale(Zombie zombie, PoseStack poseStack, float partialTick) {
    String name = BuiltInRegistries.ENTITY_TYPE.getKey(zombie.getType()).getPath();
    float scale = switch (name) {
      case "giant_zombie" -> 2.0F;
      case "tank_zombie" -> 1.35F;
      case "juggernaut_zombie" -> 1.15F;
      case "weak_zombie" -> 0.9F;
      default -> 1.0F;
    };
    poseStack.scale(scale, scale, scale);
  }
}
