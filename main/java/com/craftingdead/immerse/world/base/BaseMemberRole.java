package com.craftingdead.immerse.world.base;

/** Member hierarchy used by the 1.18 TECU base screen. */
public enum BaseMemberRole {
  OWNER,
  ADMIN,
  TRUSTED,
  MEMBER;

  public int hierarchyLevel() {
    return switch (this) {
      case OWNER -> 3;
      case ADMIN -> 2;
      case TRUSTED -> 1;
      case MEMBER -> 0;
    };
  }

  public BaseMemberRole promoted() {
    return switch (this) {
      case MEMBER -> TRUSTED;
      case TRUSTED -> ADMIN;
      case ADMIN, OWNER -> this;
    };
  }

  public BaseMemberRole demoted() {
    return switch (this) {
      case ADMIN -> TRUSTED;
      case TRUSTED -> MEMBER;
      case MEMBER, OWNER -> this;
    };
  }
}
