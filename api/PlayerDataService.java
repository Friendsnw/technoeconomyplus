package com.techno.economy.api;

import com.techno.economy.TechnoEconomyPlus;
import com.techno.economy.database.DatabaseManager;

import java.util.UUID;

/**
 * Provides read-only access to player statistics: checks, duels, jobs, and casino records.
 *
 * <h3>Threading</h3>
 * <p>All methods in this service are read-only and safe to call from any thread.</p>
 *
 * <h3>Error model</h3>
 * <p>{@link IllegalArgumentException} is thrown for {@code null} UUID arguments.
 * No business failures occur in this service; every method returns a sensible default
 * (zero, empty string, or {@code null}) when no data exists for the given player.</p>
 */
public final class PlayerDataService {

    private final TechnoEconomyPlus plugin;

    PlayerDataService(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    private static void requireNonNull(Object o, String name) {
        if (o == null) throw new IllegalArgumentException(name + " must not be null");
    }

    /**
     * Returns the number of unredeemed checks the player currently holds.
     *
     * @param playerUuid the player's UUID; must not be {@code null}
     * @return the check count; zero if the player holds no checks
     * @throws IllegalArgumentException if {@code playerUuid} is {@code null}
     */
    public int getCheckCount(UUID playerUuid) {
        requireNonNull(playerUuid, "playerUuid");
        return plugin.getCheckManager().getPlayerCheckCount(playerUuid);
    }

    /**
     * Returns the monetary value of the last check the player created.
     *
     * @param playerUuid the player's UUID; must not be {@code null}
     * @return the last check amount; zero if the player has never created a check
     * @throws IllegalArgumentException if {@code playerUuid} is {@code null}
     */
    public double getLastCheckAmount(UUID playerUuid) {
        requireNonNull(playerUuid, "playerUuid");
        return plugin.getCheckManager().getLastCheckAmount(playerUuid);
    }

    /**
     * Returns the total number of duels the player has won.
     *
     * @param playerUuid the player's UUID; must not be {@code null}
     * @return duel win count; zero if the player has no wins on record
     * @throws IllegalArgumentException if {@code playerUuid} is {@code null}
     */
    public int getDuelWins(UUID playerUuid) {
        requireNonNull(playerUuid, "playerUuid");
        return plugin.getDuelManager().getWins(playerUuid);
    }

    /**
     * Returns the total number of duels the player has lost.
     *
     * @param playerUuid the player's UUID; must not be {@code null}
     * @return duel loss count; zero if the player has no losses on record
     * @throws IllegalArgumentException if {@code playerUuid} is {@code null}
     */
    public int getDuelLosses(UUID playerUuid) {
        requireNonNull(playerUuid, "playerUuid");
        return plugin.getDuelManager().getLosses(playerUuid);
    }

    /**
     * Returns the total amount of currency the player has won across all duels.
     *
     * @param playerUuid the player's UUID; must not be {@code null}
     * @return cumulative duel winnings; zero if the player has never won a duel
     * @throws IllegalArgumentException if {@code playerUuid} is {@code null}
     */
    public double getDuelTotalWinnings(UUID playerUuid) {
        requireNonNull(playerUuid, "playerUuid");
        return plugin.getDuelManager().getTotalWinnings(playerUuid);
    }

    /**
     * Returns the name of the job the player is currently assigned to.
     *
     * @param playerUuid the player's UUID; must not be {@code null}
     * @return the job name as defined in {@code jobs.yml}, or {@code "none"} / an empty string
     *         if the player has no active job
     * @throws IllegalArgumentException if {@code playerUuid} is {@code null}
     */
    public String getJob(UUID playerUuid) {
        requireNonNull(playerUuid, "playerUuid");
        String job = plugin.getJobsManager().getJob(playerUuid);
        return job == null ? "" : job;
    }

    /**
     * Returns aggregated casino statistics for the player.
     *
     * <p>The returned {@link DatabaseManager.CasinoStats} object contains:
     * <ul>
     *   <li>{@code spins} — total number of slot spins played</li>
     *   <li>{@code totalWon} — cumulative amount won across all casino games</li>
     *   <li>{@code totalLost} — cumulative amount lost across all casino games</li>
     * </ul>
     *
     * @param playerUuid the player's UUID; must not be {@code null}
     * @return the player's casino stats; never {@code null} (returns zeros if no data)
     * @throws IllegalArgumentException if {@code playerUuid} is {@code null}
     */
    public DatabaseManager.CasinoStats getCasinoStats(UUID playerUuid) {
        requireNonNull(playerUuid, "playerUuid");
        DatabaseManager.CasinoStats stats = plugin.getDatabaseManager().getCasinoStats(playerUuid);
        if (stats == null) return new DatabaseManager.CasinoStats(0, 0.0, 0.0);
        return stats;
    }
}
