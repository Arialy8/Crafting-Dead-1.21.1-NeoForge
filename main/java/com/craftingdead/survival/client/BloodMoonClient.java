package com.craftingdead.survival.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4fStack;

/**
 * 血月客户端表现：在 AFTER_SKY 阶段绘制一个全屏半透明四边形，
 * 让天空变暗（预兆日）或泛红（血月天），不影响地面与实体渲染。
 */
public final class BloodMoonClient {

  private static final int TICKS_PER_DAY = 24000;
  private static final int SUNRISE_TICK = 1000;
  private static final int SUNSET_TICK = 13000;
  private static final int NIGHT_END_TICK = 23000;
  private static final int FADE_TICKS = 2000;

  private static final float OMEN_ALPHA = 0.35F;
  private static final float BLOOD_MOON_DAY_ALPHA = 0.25F;
  private static final float BLOOD_MOON_NIGHT_ALPHA = 0.45F;

  // 预兆日：近黑色的暗天
  private static final int OMEN_RED = 12;
  private static final int OMEN_GREEN = 12;
  private static final int OMEN_BLUE = 14;

  // 血月天：暗红色天
  private static final int BLOOD_RED = 110;
  private static final int BLOOD_GREEN = 15;
  private static final int BLOOD_BLUE = 18;

  private boolean bloodMoonDay;
  private boolean omenDay;

  /**
   * 应用服务端同步来的血月状态。
   *
   * @param daysUntilBloodMoon 距离血月还有几天，保留给后续 HUD/提示使用
   */
  @SuppressWarnings("unused")
  public void sync(boolean bloodMoonDay, boolean omenDay, int daysUntilBloodMoon) {
    this.bloodMoonDay = bloodMoonDay;
    this.omenDay = omenDay;
  }

  @SubscribeEvent
  public void onRenderLevelStage(RenderLevelStageEvent event) {
    if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY
        || (!this.bloodMoonDay && !this.omenDay)) {
      return;
    }
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.level == null || minecraft.level.dimension() != Level.OVERWORLD) {
      return;
    }
    long timeOfDay = minecraft.level.getDayTime() % TICKS_PER_DAY;
    int red;
    int green;
    int blue;
    float alpha;
    if (this.bloodMoonDay) {
      red = BLOOD_RED;
      green = BLOOD_GREEN;
      blue = BLOOD_BLUE;
      alpha = Math.max(this.computeDaytimeAlpha(timeOfDay, BLOOD_MOON_DAY_ALPHA),
          this.computeNightAlpha(timeOfDay));
    } else {
      red = OMEN_RED;
      green = OMEN_GREEN;
      blue = OMEN_BLUE;
      alpha = this.computeDaytimeAlpha(timeOfDay, OMEN_ALPHA);
    }
    if (alpha <= 0.001F) {
      return;
    }
    this.renderSkyOverlay(red, green, blue, alpha);
  }

  /**
   * 白天时段的变暗强度：日出后逐渐加深，正午最暗，日落前逐渐恢复。
   */
  private float computeDaytimeAlpha(long timeOfDay, float peakAlpha) {
    if (timeOfDay < SUNRISE_TICK || timeOfDay >= SUNSET_TICK) {
      return 0.0F;
    }
    long fadeInEnd = SUNRISE_TICK + FADE_TICKS;
    long fadeOutStart = SUNSET_TICK - FADE_TICKS;
    if (timeOfDay < fadeInEnd) {
      return peakAlpha * (timeOfDay - SUNRISE_TICK) / FADE_TICKS;
    }
    if (timeOfDay >= fadeOutStart) {
      return peakAlpha * (SUNSET_TICK - timeOfDay) / FADE_TICKS;
    }
    return peakAlpha;
  }

  /**
   * 夜晚时段的血月强度：入夜后保持最强，临近黎明逐渐淡出。
   */
  private float computeNightAlpha(long timeOfDay) {
    if (timeOfDay >= SUNSET_TICK && timeOfDay < NIGHT_END_TICK) {
      return BLOOD_MOON_NIGHT_ALPHA;
    }
    if (timeOfDay >= NIGHT_END_TICK) {
      return BLOOD_MOON_NIGHT_ALPHA * (TICKS_PER_DAY - timeOfDay) / (TICKS_PER_DAY - NIGHT_END_TICK);
    }
    if (timeOfDay < SUNRISE_TICK) {
      return BLOOD_MOON_NIGHT_ALPHA * timeOfDay / SUNRISE_TICK;
    }
    return 0.0F;
  }

  /**
   * 绘制覆盖整个屏幕的半透明天空遮罩。
   */
  private void renderSkyOverlay(int red, int green, int blue, float alpha) {
    RenderSystem.disableDepthTest();
    RenderSystem.depthMask(false);
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    RenderSystem.disableCull();
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    RenderSystem.setShader(GameRenderer::getPositionColorShader);

    // AFTER_SKY 时模型视图矩阵为身份矩阵，相机位于原点并朝向 -Z，
    // 因此直接在相机前方绘制一块足够大的四边形即可铺满整个屏幕。
    Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
    modelViewStack.pushMatrix();
    modelViewStack.identity();
    RenderSystem.applyModelViewMatrix();

    int a = Math.round(alpha * 255.0F);
    Tesselator tesselator = Tesselator.getInstance();
    BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS,
        DefaultVertexFormat.POSITION_COLOR);
    buffer.addVertex(-256.0F, -256.0F, -32.0F).setColor(red, green, blue, a);
    buffer.addVertex(256.0F, -256.0F, -32.0F).setColor(red, green, blue, a);
    buffer.addVertex(256.0F, 256.0F, -32.0F).setColor(red, green, blue, a);
    buffer.addVertex(-256.0F, 256.0F, -32.0F).setColor(red, green, blue, a);
    BufferUploader.drawWithShader(buffer.buildOrThrow());

    modelViewStack.popMatrix();
    RenderSystem.applyModelViewMatrix();
    RenderSystem.enableCull();
    RenderSystem.disableBlend();
    RenderSystem.depthMask(true);
    RenderSystem.enableDepthTest();
  }
}