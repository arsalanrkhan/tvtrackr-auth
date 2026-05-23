package com.tvtrackr.auth.bloomfilter.service;

public interface UsernameBFService {

  /**
   * Adds a username to the distributed Bloom filter.
   *
   * <p>Normalizes the username to lowercase before hashing. Computes {@code k} bit offsets
   * using double hashing (MurmurHash3 with two seeds) and sets the corresponding bits in the
   * shared Redis bitmap key {@code bloom:usernames} via {@code SETBIT}.
   *
   * <p>Called after every successful user registration to keep the filter in sync with the
   * database.
   *
   * @param username the username to add (normalization applied internally)
   */
  void add(String username);

  /**
   * Tests whether a username might exist in the filter.
   *
   * <p>A return value of {@code false} is a definite negative — the username has never been
   * added. A return value of {@code true} is a probable positive — the username is likely
   * taken, but a false positive rate of ~1% exists due to the probabilistic nature of Bloom
   * filters. Callers must verify probable positives against the database.
   *
   * @param username the username to test (normalization applied internally)
   * @return {@code false} if the username is definitely not taken;
   *         {@code true} if it is probably taken (verify with DB)
   */
  boolean mightExist(String username);

  /**
   * Returns whether the Bloom filter has been seeded from the database.
   *
   * <p>Checks for the presence of the Redis guard key {@code bloom:usernames:seeded}.
   * Used by {@code UsernameBFSeeder} on startup to skip re-seeding if already done.
   *
   * @return {@code true} if the filter has been seeded, {@code false} otherwise
   */
  boolean isSeeded();

  /**
   * Marks the Bloom filter as seeded by setting a permanent Redis guard key.
   *
   * <p>Key: {@code bloom:usernames:seeded}. No TTL — this key persists until explicitly
   * deleted (e.g. by {@link #clear()}).
   */
  void markSeeded();

  /**
   * Clears the Bloom filter and its seeded guard key from Redis.
   *
   * <p>Deletes both {@code bloom:usernames} and {@code bloom:usernames:seeded}, resetting
   * the filter to an empty, unseeded state. Called by the unverified user cleanup scheduler
   * before rebuilding the filter from the post-cleanup database state.
   *
   * <p><strong>Note:</strong> there is a brief window between {@code clear()} and the
   * completion of re-seeding where {@link #mightExist(String)} returns {@code false} for all
   * usernames. During this window, username availability checks bypass the database and may
   * incorrectly report taken usernames as available. The database unique constraint on
   * registration remains the hard safety net. A blue/green filter strategy would eliminate
   * this window entirely at the cost of additional complexity.
   */
  void clear();
}
