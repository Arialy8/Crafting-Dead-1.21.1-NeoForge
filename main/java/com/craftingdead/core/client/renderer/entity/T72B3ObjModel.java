package com.craftingdead.core.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;
import org.slf4j.Logger;

/** Loads and renders the original LAV Wavefront mesh without its old MTS runtime. */
final class T72B3ObjModel {

  private static final Logger LOGGER = LogUtils.getLogger();
  private static final T72B3ObjModel EMPTY = new T72B3ObjModel(List.of(), 0.0F, 0.0F);

  private final List<Quad> quads;
  private final float minY;
  private final float centerZ;

  private T72B3ObjModel(List<Quad> quads, float minY, float centerZ) {
    this.quads = quads;
    this.minY = minY;
    this.centerZ = centerZ;
  }

  static T72B3ObjModel load(ResourceLocation location) {
    return load(location, new String[0]);
  }

  static T72B3ObjModel load(ResourceLocation location, String... excludedObjects) {
    return loadFiltered(location, Set.of(), new HashSet<>(Arrays.asList(excludedObjects)));
  }

  static T72B3ObjModel loadOnly(ResourceLocation location, String... includedObjects) {
    return loadFiltered(location, new HashSet<>(Arrays.asList(includedObjects)), Set.of());
  }

  private static T72B3ObjModel loadFiltered(ResourceLocation location, Set<String> inclusions,
      Set<String> exclusions) {
    try {
      var resource = Minecraft.getInstance().getResourceManager().getResource(location).orElseThrow();
      List<Vector3f> positions = new ArrayList<>();
      List<float[]> uvs = new ArrayList<>();
      List<Quad> quads = new ArrayList<>();
      String currentObject = "";
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(
          resource.open(), StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          String[] values = line.trim().split("\\s+");
          if (values.length < 2) continue;
          if (values[0].equals("o")) {
            currentObject = line.trim().substring(1).trim();
          } else if (values[0].equals("v") && values.length >= 4) {
            positions.add(new Vector3f(Float.parseFloat(values[1]), Float.parseFloat(values[2]),
                Float.parseFloat(values[3])));
          } else if (values[0].equals("vt") && values.length >= 3) {
            uvs.add(new float[] {Float.parseFloat(values[1]), Float.parseFloat(values[2])});
          } else if (values[0].equals("f") && values.length == 5
              && (inclusions.isEmpty() || inclusions.contains(currentObject))
              && !exclusions.contains(currentObject)) {
            Vertex first = vertex(values[1], positions, uvs);
            Vertex second = vertex(values[2], positions, uvs);
            Vertex third = vertex(values[3], positions, uvs);
            Vertex fourth = vertex(values[4], positions, uvs);
            quads.add(quad(first, second, third, fourth));
          }
        }
      }
      if (quads.isEmpty()) return EMPTY;
      float minY = Float.POSITIVE_INFINITY;
      float minZ = Float.POSITIVE_INFINITY;
      float maxZ = Float.NEGATIVE_INFINITY;
      for (Quad quad : quads) {
        for (Vertex vertex : List.of(quad.a, quad.b, quad.c, quad.d)) {
          minY = Math.min(minY, vertex.y);
          minZ = Math.min(minZ, vertex.z);
          maxZ = Math.max(maxZ, vertex.z);
        }
      }
      return new T72B3ObjModel(List.copyOf(quads), minY, (minZ + maxZ) * 0.5F);
    } catch (Exception exception) {
      LOGGER.error("Unable to load migrated T-72B3 OBJ model {}", location, exception);
      return EMPTY;
    }
  }

  boolean isEmpty() {
    return this.quads.isEmpty();
  }

  float minY() {
    return this.minY;
  }

  float centerZ() {
    return this.centerZ;
  }

  void render(VertexConsumer consumer, PoseStack.Pose pose, int light) {
    this.render(consumer, pose, light, 255, 255, 255, 255);
  }

  void render(VertexConsumer consumer, PoseStack.Pose pose, int light,
      int red, int green, int blue, int alpha) {
    for (Quad quad : this.quads) {
      emit(consumer, pose, quad, quad.a, light, red, green, blue, alpha);
      emit(consumer, pose, quad, quad.b, light, red, green, blue, alpha);
      emit(consumer, pose, quad, quad.c, light, red, green, blue, alpha);
      emit(consumer, pose, quad, quad.d, light, red, green, blue, alpha);
    }
  }

  private static void emit(VertexConsumer consumer, PoseStack.Pose pose, Quad quad,
      Vertex vertex, int light, int red, int green, int blue, int alpha) {
    consumer.addVertex(pose.pose(), vertex.x, vertex.y, vertex.z)
        .setColor(red, green, blue, alpha)
        .setUv(vertex.u, 1.0F - vertex.v)
        .setOverlay(OverlayTexture.NO_OVERLAY)
        .setLight(light)
        .setNormal(pose, quad.normalX, quad.normalY, quad.normalZ);
  }

  private static Vertex vertex(String token, List<Vector3f> positions, List<float[]> uvs) {
    String[] indices = token.split("/");
    Vector3f position = positions.get(Integer.parseInt(indices[0]) - 1);
    float[] uv = indices.length > 1 && !indices[1].isEmpty()
        ? uvs.get(Integer.parseInt(indices[1]) - 1) : new float[] {0.0F, 0.0F};
    return new Vertex(position.x, position.y, position.z, uv[0], uv[1]);
  }

  private static Quad quad(Vertex a, Vertex b, Vertex c, Vertex d) {
    Vector3f edgeAB = new Vector3f(b.x - a.x, b.y - a.y, b.z - a.z);
    Vector3f edgeAC = new Vector3f(c.x - a.x, c.y - a.y, c.z - a.z);
    Vector3f normal = edgeAB.cross(edgeAC).normalize();
    return new Quad(a, b, c, d, normal.x, normal.y, normal.z);
  }

  private record Vertex(float x, float y, float z, float u, float v) {}
  private record Quad(Vertex a, Vertex b, Vertex c, Vertex d, float normalX, float normalY,
      float normalZ) {}
}
