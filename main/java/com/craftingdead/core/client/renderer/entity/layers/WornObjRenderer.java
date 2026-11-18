package com.craftingdead.core.client.renderer.entity.layers;

import com.craftingdead.core.CraftingDead;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.slf4j.Logger;

/** Dedicated renderer for legacy worn OBJ models; independent from item display transforms. */
final class WornObjRenderer {
  private static final Logger LOGGER = LogUtils.getLogger();
  private static final Map<ResourceLocation, Mesh> CACHE = new HashMap<>();

  private WornObjRenderer() {}

  static boolean render(ItemStack stack, EquipmentLayer.Attachment attachment,
      PoseStack poseStack, MultiBufferSource buffers, int light) {
    ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
    if (itemId == null || !CraftingDead.ID.equals(itemId.getNamespace())) return false;
    String modelName = attachment == EquipmentLayer.Attachment.BODY
        ? "tactical_vest" : hatModel(itemId.getPath());
    if (modelName == null) return false;
    String folder = attachment == EquipmentLayer.Attachment.BODY ? "vest" : "hats";
    ResourceLocation obj = ResourceLocation.fromNamespaceAndPath(CraftingDead.ID,
        "models/" + folder + "/" + modelName + ".obj");
    ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(CraftingDead.ID,
        "textures/" + folder + "/" + itemId.getPath() + ".png");
    Mesh mesh = CACHE.computeIfAbsent(obj, WornObjRenderer::load);
    if (mesh == Mesh.EMPTY) return false;
    applyLegacyModelTransform(poseStack, modelName, mesh, attachment);
    if (attachment == EquipmentLayer.Attachment.HEAD) {
      // Entity model space is Y-down. Apply model-specific facing corrections around
      // the OBJ's own center so orientation changes never disturb the head anchor.
      poseStack.translate(mesh.centerX, mesh.centerY, mesh.centerZ);
      if (requiresHorizontalFacingCorrection(modelName)) {
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
      }
      poseStack.scale(1.0F, -1.0F, 1.0F);
      poseStack.translate(-mesh.centerX, -mesh.centerY, -mesh.centerZ);
    }
    // Legacy equipment OBJ mixes clockwise and counter-clockwise face winding.
    // Render both sides so faces do not disappear or flicker as the player turns.
    VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
    var pose = poseStack.last();
    for (Triangle triangle : mesh.triangles) {
      emit(consumer, pose, triangle, triangle.vertices[0], light);
      emit(consumer, pose, triangle, triangle.vertices[1], light);
      emit(consumer, pose, triangle, triangle.vertices[2], light);
      // Entity cutout render types use QUADS. Repeating the final vertex makes
      // this triangle a degenerate quad and prevents adjacent faces being joined.
      emit(consumer, pose, triangle, triangle.vertices[2], light);
    }
    return true;
  }

  private static void emit(VertexConsumer consumer, PoseStack.Pose pose,
      Triangle triangle, Vertex vertex, int light) {
    consumer.addVertex(pose.pose(), vertex.x, vertex.y, vertex.z)
        .setColor(255, 255, 255, 255)
        .setUv(vertex.u, 1.0F - vertex.v)
        .setOverlay(OverlayTexture.NO_OVERLAY)
        .setLight(light)
        .setNormal(pose, triangle.nx, triangle.ny, triangle.nz);
  }

  private static void applyLegacyModelTransform(PoseStack poseStack, String modelName,
      Mesh mesh, EquipmentLayer.Attachment attachment) {
    ModelTransform transform = switch (modelName) {
      case "ballistic_helmet" -> new ModelTransform(0.07F, -0.16F, 0.07F,
          0.0F, 0.0F, 0.0F, 0.57F, 0.57F, 0.57F);
      case "hard_hat" -> new ModelTransform(0.585F, -0.65F, 0.525F,
          0.0F, 0.0F, 0.0F, 1.05F, 1.05F, 1.05F);
      case "mask" -> new ModelTransform(0.5F, -0.75F, 0.185F,
          0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
      case "textured_helmet_multiple_of_8" -> new ModelTransform(0.7F, -0.8F, 0.7F,
          0.0F, 0.0F, 0.0F, 1.2F, 1.2F, 1.2F);
      case "textured_helmet_multiple_of_9" -> new ModelTransform(0.32F, -0.2F, -0.265F,
          0.0F, 0.0F, 0.0F, 0.525F, 0.525F, 0.525F);
      case "tactical_vest" -> new ModelTransform(-1.575F, -0.005F, 0.3735F,
          0.0F, 0.0F, 180.0F, 1.075F, 1.15F, 1.0F);
      default -> ModelTransform.IDENTITY;
    };
    Anchor target = targetAnchor(modelName, attachment);
    float localX = (mesh.centerX - 0.5F) * transform.scaleX;
    float localY = (mesh.centerY - 0.5F) * transform.scaleY;
    float localZ = (mesh.centerZ - 0.5F) * transform.scaleZ;
    float radians = (float) Math.toRadians(transform.rotZ);
    float rotatedX = localX * (float) Math.cos(radians) - localY * (float) Math.sin(radians);
    float rotatedY = localX * (float) Math.sin(radians) + localY * (float) Math.cos(radians);
    float transformedX = transform.x + rotatedX;
    float transformedY = transform.y + rotatedY;
    float transformedZ = transform.z + localZ;
    poseStack.translate(target.x - transformedX, target.y - transformedY,
        target.z - transformedZ);
    poseStack.translate(transform.x, transform.y, transform.z);
    if (transform.rotZ != 0.0F) poseStack.mulPose(Axis.ZP.rotationDegrees(transform.rotZ));
    if (transform.rotY != 0.0F) poseStack.mulPose(Axis.YP.rotationDegrees(transform.rotY));
    if (transform.rotX != 0.0F) poseStack.mulPose(Axis.XP.rotationDegrees(transform.rotX));
    poseStack.scale(transform.scaleX, transform.scaleY, transform.scaleZ);
    // Matches the final centering step used by the original 1.18 ItemRenderer.
    poseStack.translate(-0.5F, -0.5F, -0.5F);
  }

  private static Anchor targetAnchor(String modelName, EquipmentLayer.Attachment attachment) {
    if (attachment == EquipmentLayer.Attachment.BODY) {
      return new Anchor(0.0F, 0.375F, -0.0425F);
    }
    return switch (modelName) {
      case "bunny_hat" -> new Anchor(0.0F, -0.46375F, 0.0F);
      case "nv_goggles_hat" -> new Anchor(0.0F, -0.3925F, -0.13F);
      case "ballistic_helmet" -> new Anchor(0.0F, -0.27F, -0.040625F);
      case "hard_hat" -> new Anchor(0.0F, -0.4875F, -0.0625F);
      case "textured_helmet_multiple_of_9" -> new Anchor(0.0F, -0.21F, 0.0F);
      case "top_hat", "chief_fireman_hat", "fireman_hat" ->
          new Anchor(0.0F, -0.72F, 0.0F);
      case "gas_mask", "mask", "scuba_mask" -> new Anchor(0.0F, -0.22F, -0.28F);
      default -> new Anchor(0.0F, -0.27F, 0.0F);
    };
  }

  private static boolean requiresHorizontalFacingCorrection(String modelName) {
    return modelName.equals("nv_goggles_hat") || modelName.equals("knight_hat");
  }

  private static Mesh load(ResourceLocation location) {
    try {
      var resource = Minecraft.getInstance().getResourceManager().getResource(location).orElseThrow();
      List<Vector3f> positions = new ArrayList<>();
      List<float[]> uvs = new ArrayList<>();
      List<Triangle> triangles = new ArrayList<>();
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(
          resource.open(), StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          String[] parts = line.trim().split("\\s+");
          if (parts.length < 2) continue;
          if (parts[0].equals("v") && parts.length >= 4) {
            positions.add(new Vector3f(Float.parseFloat(parts[1]), Float.parseFloat(parts[2]),
                Float.parseFloat(parts[3])));
          } else if (parts[0].equals("vt") && parts.length >= 3) {
            uvs.add(new float[] {Float.parseFloat(parts[1]), Float.parseFloat(parts[2])});
          } else if (parts[0].equals("f") && parts.length >= 4) {
            Vertex first = vertex(parts[1], positions, uvs);
            for (int i = 2; i < parts.length - 1; i++) {
              triangles.add(triangle(first, vertex(parts[i], positions, uvs),
                  vertex(parts[i + 1], positions, uvs)));
            }
          }
        }
      }
      float minX = Float.POSITIVE_INFINITY;
      float minY = Float.POSITIVE_INFINITY;
      float minZ = Float.POSITIVE_INFINITY;
      float maxX = Float.NEGATIVE_INFINITY;
      float maxY = Float.NEGATIVE_INFINITY;
      float maxZ = Float.NEGATIVE_INFINITY;
      for (Vector3f position : positions) {
        minX = Math.min(minX, position.x); minY = Math.min(minY, position.y);
        minZ = Math.min(minZ, position.z); maxX = Math.max(maxX, position.x);
        maxY = Math.max(maxY, position.y); maxZ = Math.max(maxZ, position.z);
      }
      float centerX = (minX + maxX) * 0.5F;
      float centerY = (minY + maxY) * 0.5F;
      float centerZ = (minZ + maxZ) * 0.5F;
      return new Mesh(triangles, centerX, centerY, centerZ);
    } catch (Exception exception) {
      LOGGER.error("Failed to load worn OBJ {}", location, exception);
      return Mesh.EMPTY;
    }
  }

  private static Vertex vertex(String token, List<Vector3f> positions, List<float[]> uvs) {
    String[] indices = token.split("/");
    Vector3f position = positions.get(Integer.parseInt(indices[0]) - 1);
    float[] uv = indices.length > 1 && !indices[1].isEmpty()
        ? uvs.get(Integer.parseInt(indices[1]) - 1) : new float[] {0.0F, 0.0F};
    return new Vertex(position.x, position.y, position.z, uv[0], uv[1]);
  }

  private static Triangle triangle(Vertex a, Vertex b, Vertex c) {
    Vector3f ab = new Vector3f(b.x - a.x, b.y - a.y, b.z - a.z);
    Vector3f ac = new Vector3f(c.x - a.x, c.y - a.y, c.z - a.z);
    Vector3f normal = ab.cross(ac).normalize();
    return new Triangle(new Vertex[] {a, b, c}, normal.x, normal.y, normal.z);
  }

  private static String hatModel(String item) {
    return switch (item) {
      case "black_ballistic_hat", "green_ballistic_helmet" -> "ballistic_helmet";
      case "bunny_hat" -> "bunny_hat";
      case "chief_fireman_hat" -> "chief_fireman_hat";
      case "fireman_hat" -> "fireman_hat";
      case "gas_mask" -> "gas_mask";
      case "blue_hard_hat", "green_hard_hat", "orange_hard_hat", "yellow_hard_hat" -> "hard_hat";
      case "knight_hat" -> "knight_hat";
      case "cow_mask", "creeper_mask", "payday_mask", "payday2_mask", "pumpkin_mask",
          "sheep_mask", "zombie_mask" -> "mask";
      case "nv_goggles_hat" -> "nv_goggles_hat";
      case "scuba_mask" -> "scuba_mask";
      case "army_helmet", "camo_helmet", "combat_bdu_helmet", "ghillie_hat",
          "green_army_helmet", "grey_army_helmet", "hazmat_hat", "juggernaut_helmet",
          "military_hazmat_hat", "pilot_helmet", "santa_hat", "spetsnaz_helmet",
          "winter_military_helmet", "doctor_mask" -> "textured_helmet_multiple_of_9";
      case "beanie_hat", "clone_hat", "cookie_mask", "deadpool_mask", "hacker_mask",
          "ninja_hat", "radar_cap", "red_hard_hat", "riot_hat", "ski_mask",
          "trapper_hat", "ushanka_hat" -> "textured_helmet_multiple_of_8";
      case "top_hat" -> "top_hat";
      default -> null;
    };
  }

  private record Vertex(float x, float y, float z, float u, float v) {}
  private record Triangle(Vertex[] vertices, float nx, float ny, float nz) {}
  private record Mesh(List<Triangle> triangles, float centerX, float centerY, float centerZ) {
    private static final Mesh EMPTY = new Mesh(List.of(), 0.0F, 0.0F, 0.0F);
  }
  private record ModelTransform(float x, float y, float z,
      float rotX, float rotY, float rotZ, float scaleX, float scaleY, float scaleZ) {
    private static final ModelTransform IDENTITY = new ModelTransform(
        0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
  }
  private record Anchor(float x, float y, float z) {
  }
}
