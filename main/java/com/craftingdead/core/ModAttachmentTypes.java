package com.craftingdead.core;

import com.craftingdead.core.world.entity.extension.BasicLivingExtension;
import com.craftingdead.core.world.entity.extension.LivingExtension;
import com.craftingdead.core.world.entity.extension.PlayerExtension;
import com.craftingdead.survival.world.hydration.HydrationData;
import com.craftingdead.immerse.world.stats.SurvivalStatsData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import java.util.function.Supplier;

public class ModAttachmentTypes {

  public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
      DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, CraftingDead.ID);

  @SuppressWarnings("rawtypes")
  private static final Supplier<AttachmentType<LivingExtension>> LIVING_EXTENSION =
      ATTACHMENT_TYPES.register("living_extension",
          () -> AttachmentType.serializable(holder -> {
            if (holder instanceof Player player) {
              return (LivingExtension) PlayerExtension.create(player);
            } else if (holder instanceof LivingEntity living) {
              return (LivingExtension) BasicLivingExtension.create(living);
            }
            throw new IllegalArgumentException("Invalid holder: " + holder);
          }).copyOnDeath().build());

  private static final Supplier<AttachmentType<HydrationData>> HYDRATION =
      ATTACHMENT_TYPES.register("hydration",
          () -> AttachmentType.serializable(holder -> new HydrationData()).build());

  private static final Supplier<AttachmentType<SurvivalStatsData>> SURVIVAL_STATS =
      ATTACHMENT_TYPES.register("survival_stats",
          () -> AttachmentType.serializable(holder -> new SurvivalStatsData()).copyOnDeath().build());

  public static AttachmentType<LivingExtension> getLivingExtension() {
    return LIVING_EXTENSION.get();
  }

  public static AttachmentType<HydrationData> getHydration() {
    return HYDRATION.get();
  }

  public static AttachmentType<SurvivalStatsData> getSurvivalStats() {
    return SURVIVAL_STATS.get();
  }
}
