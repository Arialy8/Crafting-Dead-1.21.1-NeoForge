package com.craftingdead.immerse.client;

import com.craftingdead.core.client.util.RenderUtil;
import com.craftingdead.immerse.CraftingDeadImmerse;
import com.craftingdead.immerse.world.stats.RankDefinitions;
import com.craftingdead.immerse.world.stats.SurvivalStatsData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayDeque;
import java.util.Deque;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public final class ImmerseSurvivalHud {

  public static final KeyMapping TOGGLE_STATS = new KeyMapping(
      "key.craftingdeadimmerse.toggle_stats", GLFW.GLFW_KEY_H,
      "key.categories.craftingdead");

  private static final long KILL_LIFETIME_MS = 5000L;
  private static final int MAX_KILLS = 5;
  private static final int RANK_COLOR = 0xDDE7B95E;
  private static final int RANK_SHADOW = 0x88000000;
  private static final Deque<KillEntry> KILLS = new ArrayDeque<>();
  private static volatile int survivalTicks;
  private static volatile int zombieKills;
  private static volatile int playerKills;
  private static volatile long currentRankScore;

  private static final ResourceLocation DAYS_SURVIVED = ResourceLocation.fromNamespaceAndPath(
      CraftingDeadImmerse.ID, "textures/gui/days_survived.png");
  private static final ResourceLocation ZOMBIES_KILLED = ResourceLocation.fromNamespaceAndPath(
      CraftingDeadImmerse.ID, "textures/gui/zombies_killed.png");
  private static final ResourceLocation PLAYERS_KILLED = ResourceLocation.fromNamespaceAndPath(
      CraftingDeadImmerse.ID, "textures/gui/players_killed.png");

  private final Minecraft minecraft = Minecraft.getInstance();
  private boolean showStats = true;
  private boolean hadLevel;

  @SubscribeEvent
  public void render(RenderGuiEvent.Post event) {
    if (this.minecraft.player == null || this.minecraft.options.hideGui) {
      return;
    }

    GuiGraphics graphics = event.getGuiGraphics();
    if (this.showStats) {
      int daysSurvived = survivalTicks / SurvivalStatsData.TICKS_PER_DAY;

      int x = 4;
      int centerY = this.minecraft.getWindow().getGuiScaledHeight() / 2;
      this.renderRank(graphics, x, centerY - 40);
      this.renderStat(graphics, DAYS_SURVIVED, daysSurvived, x, centerY - 20);
      this.renderStat(graphics, ZOMBIES_KILLED, zombieKills, x, centerY);
      this.renderStat(graphics, PLAYERS_KILLED, playerKills, x, centerY + 20);
    }
    this.renderKillFeed(graphics);
  }

  @SubscribeEvent
  public void tick(ClientTickEvent.Post event) {
    boolean hasLevel = this.minecraft.level != null && this.minecraft.player != null;
    if (!hasLevel && this.hadLevel) {
      KILLS.clear();
      setStats(0, 0, 0, 0);
      this.showStats = true;
    }
    this.hadLevel = hasLevel;
    while (TOGGLE_STATS.consumeClick()) {
      this.showStats = !this.showStats;
    }
  }

  public static void addKill(String killer, String target, ResourceLocation weapon) {
    KILLS.addFirst(new KillEntry(killer, target, weapon, System.currentTimeMillis()));
    while (KILLS.size() > MAX_KILLS) {
      KILLS.removeLast();
    }
  }

  public static void setStats(int syncedSurvivalTicks, int syncedZombieKills,
      int syncedPlayerKills, long syncedRankScore) {
    survivalTicks = Math.max(0, syncedSurvivalTicks);
    zombieKills = Math.max(0, syncedZombieKills);
    playerKills = Math.max(0, syncedPlayerKills);
    currentRankScore = Math.max(0, syncedRankScore);
  }

  private void renderKillFeed(GuiGraphics graphics) {
    long now = System.currentTimeMillis();
    KILLS.removeIf(entry -> now - entry.createdAt() >= KILL_LIFETIME_MS);
    int y = 6;
    int width = this.minecraft.getWindow().getGuiScaledWidth();
    for (KillEntry entry : KILLS) {
      long age = now - entry.createdAt();
      float alpha = age < 4000L ? 1.0F : 1.0F - (age - 4000L) / 1000.0F;
      String killer = abbreviate(entry.killer());
      String target = abbreviate(entry.target());
      ItemStack weapon = new ItemStack(BuiltInRegistries.ITEM.get(entry.weapon()));
      boolean showWeapon = !weapon.isEmpty() && !weapon.is(net.minecraft.world.item.Items.AIR);
      int weaponSpace = showWeapon ? 22 : 6;
      int textWidth = this.minecraft.font.width(killer)
          + this.minecraft.font.width(target) + weaponSpace + 8;
      int left = width - textWidth - 8;
      int opacity = Math.max(0, Math.min(170, (int) (170 * alpha)));
      graphics.fill(left, y, width - 4, y + 18, opacity << 24);
      graphics.drawString(this.minecraft.font, killer, left + 4, y + 5,
          withAlpha(0xE7B95E, alpha), true);
      if (showWeapon) {
        graphics.renderItem(weapon,
            left + 8 + this.minecraft.font.width(killer), y + 1);
      }
      graphics.drawString(this.minecraft.font, target,
          left + 8 + weaponSpace + this.minecraft.font.width(killer), y + 5,
          withAlpha(0xFFFFFF, alpha), true);
      y += 20;
    }
  }

  private static int withAlpha(int rgb, float alpha) {
    return (Math.max(0, Math.min(255, (int) (alpha * 255))) << 24) | rgb;
  }

  private static String abbreviate(String value) {
    String safe = value == null ? "" : value;
    return safe.length() <= 18 ? safe : safe.substring(0, 17) + "...";
  }

  private void renderStat(GuiGraphics graphics, ResourceLocation texture, int value,
      int x, int y) {
    RenderUtil.blitFull(graphics, texture, x, y, 16, 16);
    graphics.drawString(this.minecraft.font, Integer.toString(value), x + 20, y + 4,
        0xFFFFFFFF, true);
  }

  private void renderRank(GuiGraphics graphics, int x, int y) {
    long score = this.rankScore();
    int rankLevel = RankDefinitions.levelOf(score);
    this.renderRankInsignia(graphics, x, y, rankLevel);
    // As with the other three survival counters, show the current numeric value.
    graphics.drawString(this.minecraft.font, Long.toString(score), x + 20, y + 4,
        0xFFFFFFFF, true);
  }

  /** Draws a compact transparent-backed insignia instead of shrinking the old white 1024px art. */
  private void renderRankInsignia(GuiGraphics graphics, int x, int y, int rankLevel) {
    int color = rankLevel <= 6 ? 0xEE55FF55 : rankLevel <= 13 ? 0xEE5599FF : 0xEEFF5555;
    // Pixel chevrons for enlisted ranks; star and wings for officers.
    if (rankLevel <= 6) {
      int chevrons = (rankLevel + 1) / 2;
      for (int row = 0; row < chevrons; row++) {
        for (int step = 0; step < 5; step++) {
          mark(graphics, x + 3 + step, y + 3 + row * 3 + step / 2, color);
          mark(graphics, x + 12 - step, y + 3 + row * 3 + step / 2, color);
        }
      }
      if (rankLevel % 2 == 0) {
        graphics.fill(x + 5, y + 14, x + 11, y + 15, color);
      }
    } else {
      String[] star = {"...#...", "...#...", "#######", ".#####.",
          "..###..", ".##.##.", ".#...#."};
      for (int sy = 0; sy < star.length; sy++) {
        for (int sx = 0; sx < star[sy].length(); sx++) {
          if (star[sy].charAt(sx) == '#') {
            mark(graphics, x + 4 + sx, y + 2 + sy, color);
          }
        }
      }
      int grade = rankLevel <= 13 ? rankLevel - 6 : rankLevel - 13;
      for (int i = 0; i < grade; i++) {
        mark(graphics, x + 2 + i * 2, y + 12, color);
      }
      if (rankLevel >= 14) {
        for (int i = 0; i < 3; i++) {
          graphics.fill(x + i, y + 4 + i * 2, x + 3, y + 5 + i * 2, color);
          graphics.fill(x + 13, y + 4 + i * 2, x + 16 - i, y + 5 + i * 2, color);
        }
      }
    }
  }

  private static void mark(GuiGraphics graphics, int x, int y, int color) {
    graphics.fill(x + 1, y + 1, x + 2, y + 2, RANK_SHADOW);
    graphics.fill(x, y, x + 1, y + 1, color);
  }

  private long rankScore() {
    return currentRankScore;
  }

  private record KillEntry(String killer, String target, ResourceLocation weapon, long createdAt) {}
}
