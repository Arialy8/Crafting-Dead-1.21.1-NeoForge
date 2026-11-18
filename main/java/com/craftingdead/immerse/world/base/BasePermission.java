package com.craftingdead.immerse.world.base;

import java.util.Locale;

/** Individually assignable base permissions. Administrator is stored separately. */
public enum BasePermission {
  BUILD,
  BREAK,
  CONTAINER,
  CRAFT;

  public static BasePermission byName(String name) {
    if (name == null) {
      return null;
    }
    try {
      return valueOf(name.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }

}
