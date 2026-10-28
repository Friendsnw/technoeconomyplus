package com.techno.economy.api;

import com.techno.economy.TechnoEconomyPlus;
import org.bukkit.Material;

import java.util.Map;
import java.util.UUID;

/**
 * Provides access to the stock exchange: ore market prices, company share prices,
 * share trading, and player portfolio information.
 *
 * <h3>Threading</h3>
 * <p>{@link #buyShares} and {@link #sellShares} must be called from the
 * <strong>main server thread</strong>. All read-only methods are safe from any thread.</p>
 *
 * <h3>Error model</h3>
 * <p>{@link IllegalArgumentException} for null/invalid arguments. Business failures
 * (insufficient funds, not enough available shares, player has no shares to sell) are
 * returned as a failed {@link ApiResult}.</p>
 */
public final class StockService {

    private final TechnoEconomyPlus plugin;

    StockService(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    private static void requireNonNull(Object o, String name) {
        if (o == null) throw new IllegalArgumentException(name + " must not be null");
    }

    private static void requirePositiveInt(int value, String name) {
        if (value <= 0)
            throw new IllegalArgumentException(name + " must be greater than zero, got " + value);
    }

    /**
     * Returns the current market price for the ore identified by the given material name.
     * The name is case-insensitive and should match a {@link Material} name (e.g. {@code "DIAMOND"}).
     * Safe to call from any thread.
     *
     * @param materialName the material name of the ore; must not be {@code null}
     * @return the current ore price, or {@code 0.0} if the material is not tracked
     * @throws IllegalArgumentException if {@code materialName} is {@code null}
     */
    public double getOrePrice(String materialName) {
        requireNonNull(materialName, "materialName");
        return plugin.getStockManager().getOrePrice(materialName);
    }

    /**
     * Returns the current share price for the given company.
     * Safe to call from any thread.
     *
     * @param companyName the exact company name; must not be {@code null}
     * @return the share price, or {@code 0.0} if the company has no listed shares
     * @throws IllegalArgumentException if {@code companyName} is {@code null}
     */
    public double getSharePrice(String companyName) {
        requireNonNull(companyName, "companyName");
        return plugin.getStockManager().getCompanySharePrice(companyName);
    }

    /**
     * Returns the number of shares that are still available for purchase for the given company.
     * Safe to call from any thread.
     *
     * @param companyName the exact company name; must not be {@code null}
     * @return the number of available shares, or {@code 0} if none remain or the company does not exist
     * @throws IllegalArgumentException if {@code companyName} is {@code null}
     */
    public int getAvailableShares(String companyName) {
        requireNonNull(companyName, "companyName");
        return plugin.getStockManager().getAvailableShares(companyName);
    }

    /**
     * Purchases the specified number of company shares on behalf of the player.
     * The player's wallet is debited and their portfolio is updated.
     * Must be called from the main server thread.
     *
     * @param playerUuid  the buyer's UUID; must not be {@code null}
     * @param companyName the exact company name; must not be {@code null}
     * @param amount      the number of shares to buy; must be greater than zero
     * @return a successful {@link ApiResult} with the number of shares bought, or a failed result
     *         if the player cannot afford the purchase, if there are not enough available shares,
     *         or if the company does not exist
     * @throws IllegalArgumentException if any argument is {@code null} or {@code amount} is not positive
     */
    public ApiResult<Integer> buyShares(UUID playerUuid, String companyName, int amount) {
        requireNonNull(playerUuid, "playerUuid");
        requireNonNull(companyName, "companyName");
        requirePositiveInt(amount, "amount");
        boolean ok = plugin.getStockManager().buyCompanyShares(playerUuid, companyName, amount);
        if (!ok) return ApiResult.failure("Share purchase failed: insufficient funds, not enough available shares, or company not found");
        return ApiResult.success(amount);
    }

    /**
     * Sells the specified number of company shares on behalf of the player.
     * The player's portfolio is debited and their wallet is credited.
     * Must be called from the main server thread.
     *
     * @param playerUuid  the seller's UUID; must not be {@code null}
     * @param companyName the exact company name; must not be {@code null}
     * @param amount      the number of shares to sell; must be greater than zero
     * @return a successful {@link ApiResult} with the number of shares sold, or a failed result
     *         if the player does not own enough shares or the company does not exist
     * @throws IllegalArgumentException if any argument is {@code null} or {@code amount} is not positive
     */
    public ApiResult<Integer> sellShares(UUID playerUuid, String companyName, int amount) {
        requireNonNull(playerUuid, "playerUuid");
        requireNonNull(companyName, "companyName");
        requirePositiveInt(amount, "amount");
        boolean ok = plugin.getStockManager().sellCompanyShares(playerUuid, companyName, amount);
        if (!ok) return ApiResult.failure("Share sale failed: player does not own enough shares or company not found");
        return ApiResult.success(amount);
    }

    /**
     * Returns the total market value of all shares held by the player across all companies.
     * Safe to call from any thread.
     *
     * @param playerUuid the player's UUID; must not be {@code null}
     * @return the portfolio value in the server currency
     * @throws IllegalArgumentException if {@code playerUuid} is {@code null}
     */
    public double getPortfolioValue(UUID playerUuid) {
        requireNonNull(playerUuid, "playerUuid");
        return plugin.getStockManager().getPortfolioValue(playerUuid);
    }

    /**
     * Returns a map of company name to share count for all companies in which the player holds shares.
     * Safe to call from any thread.
     *
     * @param playerUuid the player's UUID; must not be {@code null}
     * @return an unmodifiable snapshot of the player's share holdings; empty if none
     * @throws IllegalArgumentException if {@code playerUuid} is {@code null}
     */
    public Map<String, Integer> getPlayerShares(UUID playerUuid) {
        requireNonNull(playerUuid, "playerUuid");
        return plugin.getStockManager().getPlayerShares(playerUuid);
    }

    /**
     * Returns the cumulative profit or loss from all share trades made by the player.
     * A positive value means net profit; a negative value means net loss.
     * Safe to call from any thread.
     *
     * @param playerUuid the player's UUID; must not be {@code null}
     * @return the player's total profit/loss
     * @throws IllegalArgumentException if {@code playerUuid} is {@code null}
     */
    public double getProfitLoss(UUID playerUuid) {
        requireNonNull(playerUuid, "playerUuid");
        return plugin.getStockManager().getProfitLoss(playerUuid);
    }
}
