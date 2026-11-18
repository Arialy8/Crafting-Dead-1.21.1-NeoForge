package com.craftingdead.core.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

/** Client-only binocular camera operations kept out of the common item class. */
public final class BinocularsClient {

  private BinocularsClient() {}

  public static void forceFirstPerson() {
    Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON);
  }
}
