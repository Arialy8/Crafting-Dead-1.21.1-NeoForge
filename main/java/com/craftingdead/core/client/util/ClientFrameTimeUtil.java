/*
 * Crafting Dead 1.21.1 port helper.
 */
package com.craftingdead.core.client.util;

import net.minecraft.client.Minecraft;

/**
 * Small compatibility helper for partial tick/frame time access.
 *
 * <p>Minecraft's client timing API changed across 1.20.5/1.21.x. Older Crafting Dead
 * code called {@code Minecraft#getFrameTime()} directly. Keeping this access behind a helper
 * avoids scattering version-specific calls throughout gun rendering, camera recoil and HUD code.
 */
public final class ClientFrameTimeUtil {

  private ClientFrameTimeUtil() {}

  public static float getFrameTime(Minecraft minecraft) {
    return getFrameTime(minecraft, 1.0F);
  }

  public static float getFrameTime(Minecraft minecraft, float fallback) {
    if (minecraft == null) {
      return fallback;
    }

    // 1.18.x - 1.20.x path. Reflection keeps this source compiling when the method is removed.
    Float direct = invokeFloat(minecraft, "getFrameTime");
    if (direct != null) {
      return direct;
    }

    // 1.21.x timer/delta tracker path. Method names changed between minor loader builds, so keep
    // this defensive and fall back gracefully until final compile logs are available.
    Object timer = invokeObject(minecraft, "getTimer");
    if (timer != null) {
      for (String method : new String[] {
          "getGameTimeDeltaPartialTick", "getGameTimeDeltaTicks", "getRealtimeDeltaTicks"}) {
        Float value = invokeFloat(timer, method);
        if (value != null) {
          return value;
        }
      }
    }

    return fallback;
  }

  private static Object invokeObject(Object target, String methodName) {
    try {
      var method = target.getClass().getMethod(methodName);
      return method.invoke(target);
    } catch (ReflectiveOperationException | RuntimeException ignored) {
      return null;
    }
  }

  private static Float invokeFloat(Object target, String methodName) {
    try {
      var method = target.getClass().getMethod(methodName);
      Object value = method.invoke(target);
      return value instanceof Number number ? number.floatValue() : null;
    } catch (ReflectiveOperationException | RuntimeException ignored) {
      return null;
    }
  }
}
