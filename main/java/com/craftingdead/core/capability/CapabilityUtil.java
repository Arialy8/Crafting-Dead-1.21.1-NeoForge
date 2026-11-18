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

package com.craftingdead.core.capability;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;

public class CapabilityUtil {

  @Nullable
  public static <T> T getOrThrow(Object capability, Object provider, Class<T> clazz) {
    return null;
  }

  @Nullable
  public static <T> T get(Object capability, Object provider, Class<T> clazz) {
    return null;
  }

  public static Object serializableProvider(Supplier<?> instanceSupplier, Object... capabilities) {
    return null;
  }

  public static Object serializableProvider(Object emptyTag, Supplier<?> instanceSupplier,
      Object... capabilities) {
    return null;
  }

  public static Object serializableProvider(Object emptyTag, Object instance,
      Object... capabilities) {
    return null;
  }

  public static Object serializableProvider(Object emptyTag, Object instance,
      Set<?> capabilities, @Nullable Function<?, Object> instanceMapper) {
    return null;
  }

  public static Object provider(Supplier<?> instanceSupplier, Object... capabilities) {
    return null;
  }

  public static Object provider(Object instance, Object... capabilities) {
    return null;
  }

  public static Object provider(Object instance, Set<?> capabilities,
      @Nullable Function<?, Object> instanceMapper) {
    return null;
  }
}
