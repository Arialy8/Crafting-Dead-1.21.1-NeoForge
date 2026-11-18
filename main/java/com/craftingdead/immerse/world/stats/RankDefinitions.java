package com.craftingdead.immerse.world.stats;

/** Defines the scoring formula, score thresholds and localized names for player ranks. */
public final class RankDefinitions {
  private RankDefinitions() {}

  public static final long SCORE_PER_DAY_SURVIVED = 100L;
  public static final long SCORE_PER_ZOMBIE_KILL = 2L;
  public static final long SCORE_PER_PLAYER_KILL = 20L;

  /** Computes the rank score from the tracked survival stats. */
  public static long scoreOf(int survivalTicks, int zombieKills, int playerKills) {
    return (long) (survivalTicks / SurvivalStatsData.TICKS_PER_DAY) * SCORE_PER_DAY_SURVIVED
        + zombieKills * SCORE_PER_ZOMBIE_KILL
        + playerKills * SCORE_PER_PLAYER_KILL;
  }
  public enum Rank {
    PRIVATE("rank.craftingdeadimmerse.rank1", 0),
    PRIVATE_FIRST_CLASS("rank.craftingdeadimmerse.rank2", 50),
    CORPORAL("rank.craftingdeadimmerse.rank3", 150),
    SERGEANT("rank.craftingdeadimmerse.rank4", 300),
    STAFF_SERGEANT("rank.craftingdeadimmerse.rank5", 500),
    MASTER_SERGEANT_4("rank.craftingdeadimmerse.rank6", 800),
    MASTER_SERGEANT_3("rank.craftingdeadimmerse.rank7", 1200),
    MASTER_SERGEANT_2("rank.craftingdeadimmerse.rank8", 1800),
    MASTER_SERGEANT_1("rank.craftingdeadimmerse.rank9", 2600),
    SECOND_LIEUTENANT("rank.craftingdeadimmerse.rank10", 4000),
    FIRST_LIEUTENANT("rank.craftingdeadimmerse.rank11", 6000),
    CAPTAIN("rank.craftingdeadimmerse.rank12", 9000),
    MAJOR("rank.craftingdeadimmerse.rank13", 13000),
    LIEUTENANT_COLONEL("rank.craftingdeadimmerse.rank14", 18000),
    COLONEL("rank.craftingdeadimmerse.rank15", 24000),
    SENIOR_COLONEL("rank.craftingdeadimmerse.rank16", 32000),
    MAJOR_GENERAL("rank.craftingdeadimmerse.rank17", 45000),
    LIEUTENANT_GENERAL("rank.craftingdeadimmerse.rank18", 60000),
    GENERAL("rank.craftingdeadimmerse.rank19", 80000);

    private final String nameKey;
    private final int minScore;

    Rank(String nameKey, int minScore) {
      this.nameKey = nameKey;
      this.minScore = minScore;
    }

    public String nameKey() { return this.nameKey; }
    public int minScore() { return this.minScore; }
  }

  public static Rank fromScore(long score) {
    Rank result = Rank.PRIVATE;
    for (Rank rank : Rank.values()) {
      if (score >= rank.minScore()) result = rank;
    }
    return result;
  }

  public static int levelOf(long score) { return fromScore(score).ordinal() + 1; }

  public static long nextLevelScore(long score) {
    Rank rank = fromScore(score);
    Rank[] ranks = Rank.values();
    return rank == ranks[ranks.length - 1] ? Long.MAX_VALUE : ranks[rank.ordinal() + 1].minScore();
  }

  public static long prevLevelScore(long score) {
    Rank rank = fromScore(score);
    return rank == Rank.PRIVATE ? 0 : Rank.values()[rank.ordinal() - 1].minScore();
  }

  public static long upgradeProgress(long score) {
    long next = nextLevelScore(score);
    return next == Long.MAX_VALUE ? 0 : next - fromScore(score).minScore();
  }
}
