package com.techno.economy.api;

import com.techno.economy.TechnoEconomyPlus;
import com.techno.economy.company.CompanyManager;

import java.util.Optional;
import java.util.UUID;

/**
 * Provides operations for querying and managing companies.
 *
 * <h3>Threading</h3>
 * <p>{@link #createCompany}, {@link #depositToCompany}, and {@link #withdrawFromCompany}
 * must be called from the <strong>main server thread</strong>.
 * Read-only methods ({@link #getCompany}, {@link #getPlayerCompany},
 * {@link #getCompanyBalance}) are safe from any thread.</p>
 *
 * <h3>Error model</h3>
 * <p>{@link IllegalArgumentException} for null/blank/invalid arguments.
 * Business failures (company already exists, insufficient company funds) are
 * returned through a failed {@link ApiResult}.</p>
 */
public final class CompanyService {

    private final TechnoEconomyPlus plugin;

    CompanyService(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    private static void requireNonNull(Object o, String name) {
        if (o == null) throw new IllegalArgumentException(name + " must not be null");
    }

    private static void requireNonBlank(String s, String name) {
        if (s == null || s.isBlank())
            throw new IllegalArgumentException(name + " must not be null or blank");
    }

    private static void requirePositive(double amount, String name) {
        if (amount <= 0)
            throw new IllegalArgumentException(name + " must be greater than zero, got " + amount);
    }

    /**
     * Creates a new company owned by the specified player.
     * Must be called from the main server thread.
     *
     * @param ownerUuid  the UUID of the player who will own the company; must not be {@code null}
     * @param ownerName  the display name of the owner; must not be {@code null} or blank
     * @param name       the desired company name (case-sensitive, must be unique); must not be {@code null} or blank
     * @return a successful {@link ApiResult} with {@code null} value on creation, or a failed result if
     *         a company with the same name already exists or the player already owns a company
     * @throws IllegalArgumentException if any argument is {@code null} or blank
     */
    public ApiResult<Void> createCompany(UUID ownerUuid, String ownerName, String name) {
        requireNonNull(ownerUuid, "ownerUuid");
        requireNonBlank(ownerName, "ownerName");
        requireNonBlank(name, "name");
        boolean ok = plugin.getCompanyManager().createCompany(ownerUuid, ownerName, name);
        if (!ok) return ApiResult.failure("Company creation failed: name already taken or player already owns a company");
        return ApiResult.success(null);
    }

    /**
     * Returns the company data for the given company name.
     * Safe to call from any thread.
     *
     * @param name the exact company name; must not be {@code null} or blank
     * @return an {@link Optional} containing the {@link CompanyManager.CompanyData} if found, or empty
     * @throws IllegalArgumentException if {@code name} is {@code null} or blank
     */
    public Optional<CompanyManager.CompanyData> getCompany(String name) {
        requireNonBlank(name, "name");
        return Optional.ofNullable(plugin.getCompanyManager().getCompany(name));
    }

    /**
     * Returns the company owned or joined by the specified player.
     * Safe to call from any thread.
     *
     * @param playerUuid the player's UUID; must not be {@code null}
     * @return an {@link Optional} containing the player's {@link CompanyManager.CompanyData}, or empty
     *         if the player has no company affiliation
     * @throws IllegalArgumentException if {@code playerUuid} is {@code null}
     */
    public Optional<CompanyManager.CompanyData> getPlayerCompany(UUID playerUuid) {
        requireNonNull(playerUuid, "playerUuid");
        return Optional.ofNullable(plugin.getCompanyManager().getPlayerCompany(playerUuid));
    }

    /**
     * Returns the fund balance of the specified company.
     * Safe to call from any thread.
     *
     * @param companyName the exact company name; must not be {@code null} or blank
     * @return the company's current fund balance, or {@code 0.0} if the company does not exist
     * @throws IllegalArgumentException if {@code companyName} is {@code null} or blank
     */
    public double getCompanyBalance(String companyName) {
        requireNonBlank(companyName, "companyName");
        CompanyManager.CompanyData data = plugin.getCompanyManager().getCompany(companyName);
        if (data == null) return 0.0;
        return data.getBalance();
    }

    /**
     * Adds funds to the specified company's treasury.
     * Must be called from the main server thread.
     *
     * @param companyName the exact company name; must not be {@code null} or blank
     * @param amount      the amount to deposit; must be greater than zero
     * @return a successful {@link ApiResult} with the new company balance, or a failed result if
     *         the company does not exist
     * @throws IllegalArgumentException if {@code companyName} is {@code null} or blank, or
     *                                  {@code amount} is not positive
     */
    public ApiResult<Double> depositToCompany(String companyName, double amount) {
        requireNonBlank(companyName, "companyName");
        requirePositive(amount, "amount");
        boolean ok = plugin.getCompanyManager().deposit(companyName, amount);
        if (!ok) return ApiResult.failure("Deposit failed: company not found");
        return ApiResult.success(getCompanyBalance(companyName));
    }

    /**
     * Deducts funds from the specified company's treasury.
     * Must be called from the main server thread.
     *
     * @param companyName the exact company name; must not be {@code null} or blank
     * @param amount      the amount to withdraw; must be greater than zero
     * @return a successful {@link ApiResult} with the new company balance, or a failed result if
     *         the company does not exist or has insufficient funds
     * @throws IllegalArgumentException if {@code companyName} is {@code null} or blank, or
     *                                  {@code amount} is not positive
     */
    public ApiResult<Double> withdrawFromCompany(String companyName, double amount) {
        requireNonBlank(companyName, "companyName");
        requirePositive(amount, "amount");
        boolean ok = plugin.getCompanyManager().withdraw(companyName, amount);
        if (!ok) return ApiResult.failure("Withdrawal failed: company not found or insufficient company funds");
        return ApiResult.success(getCompanyBalance(companyName));
    }
}
