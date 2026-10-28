package com.techno.economy.api;

import com.techno.economy.TechnoEconomyPlus;

import java.util.UUID;

/**
 * Provides read and write access to a player's wallet balance and bank balance.
 *
 * <h3>Threading</h3>
 * <p>Mutating methods ({@link #deposit}, {@link #withdraw}, {@link #transfer},
 * {@link #depositToBank}, {@link #withdrawFromBank}) must be called from the
 * <strong>main server thread</strong>. Read-only methods ({@link #getBalance},
 * {@link #hasBalance}, {@link #getBankBalance}) are safe to call from any thread.</p>
 *
 * <h3>Error model</h3>
 * <p>{@link IllegalArgumentException} is thrown for programming errors (null UUID,
 * non-positive amount). Business failures such as insufficient funds are signalled
 * through a failed {@link ApiResult} — no exception is thrown.</p>
 */
public final class EconomyService {

    private final TechnoEconomyPlus plugin;

    EconomyService(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    private static void requireUuid(UUID uuid, String name) {
        if (uuid == null) throw new IllegalArgumentException(name + " must not be null");
    }

    private static void requirePositive(double amount, String name) {
        if (amount <= 0)
            throw new IllegalArgumentException(name + " must be greater than zero, got " + amount);
    }

    /**
     * Returns the wallet balance of the specified player.
     * Safe to call from any thread.
     *
     * @param uuid the player's unique ID; must not be {@code null}
     * @return the player's current wallet balance; zero if the player has no recorded balance
     * @throws IllegalArgumentException if {@code uuid} is {@code null}
     */
    public double getBalance(UUID uuid) {
        requireUuid(uuid, "uuid");
        return plugin.getBalanceManager().getBalance(uuid);
    }

    /**
     * Returns {@code true} if the player's wallet contains at least {@code amount}.
     * Safe to call from any thread.
     *
     * @param uuid   the player's unique ID; must not be {@code null}
     * @param amount the amount to check; must be greater than zero
     * @return {@code true} if the player can afford the amount
     * @throws IllegalArgumentException if {@code uuid} is {@code null} or {@code amount} is not positive
     */
    public boolean hasBalance(UUID uuid, double amount) {
        requireUuid(uuid, "uuid");
        requirePositive(amount, "amount");
        return plugin.getBalanceManager().has(uuid, amount);
    }

    /**
     * Adds the specified amount to the player's wallet.
     * Must be called from the main server thread.
     *
     * @param uuid   the player's unique ID; must not be {@code null}
     * @param amount the amount to add; must be greater than zero
     * @return a successful {@link ApiResult} containing the player's new wallet balance
     * @throws IllegalArgumentException if {@code uuid} is {@code null} or {@code amount} is not positive
     */
    public ApiResult<Double> deposit(UUID uuid, double amount) {
        requireUuid(uuid, "uuid");
        requirePositive(amount, "amount");
        plugin.getBalanceManager().addBalance(uuid, amount);
        return ApiResult.success(plugin.getBalanceManager().getBalance(uuid));
    }

    /**
     * Deducts the specified amount from the player's wallet.
     * Must be called from the main server thread.
     *
     * @param uuid   the player's unique ID; must not be {@code null}
     * @param amount the amount to deduct; must be greater than zero
     * @return a successful {@link ApiResult} with the new wallet balance on success, or a
     *         failed result with reason {@code "Insufficient wallet balance"} if the player
     *         cannot afford the amount
     * @throws IllegalArgumentException if {@code uuid} is {@code null} or {@code amount} is not positive
     */
    public ApiResult<Double> withdraw(UUID uuid, double amount) {
        requireUuid(uuid, "uuid");
        requirePositive(amount, "amount");
        boolean ok = plugin.getBalanceManager().removeBalance(uuid, amount);
        if (!ok) return ApiResult.failure("Insufficient wallet balance");
        return ApiResult.success(plugin.getBalanceManager().getBalance(uuid));
    }

    /**
     * Transfers the specified amount from one player's wallet to another.
     * Both balances are updated via the underlying manager, preserving atomicity.
     * Must be called from the main server thread.
     *
     * @param from   the UUID of the sending player; must not be {@code null}
     * @param to     the UUID of the receiving player; must not be {@code null}
     * @param amount the amount to transfer; must be greater than zero
     * @return a successful {@link ApiResult} with the sender's new balance, or a failed result if
     *         the sender cannot afford the amount or if {@code from} and {@code to} are the same
     * @throws IllegalArgumentException if either UUID is {@code null} or {@code amount} is not positive
     */
    public ApiResult<Double> transfer(UUID from, UUID to, double amount) {
        requireUuid(from, "from");
        requireUuid(to, "to");
        requirePositive(amount, "amount");
        if (from.equals(to)) return ApiResult.failure("Sender and recipient must be different players");
        boolean ok = plugin.getBalanceManager().removeBalance(from, amount);
        if (!ok) return ApiResult.failure("Insufficient wallet balance");
        plugin.getBalanceManager().addBalance(to, amount);
        return ApiResult.success(plugin.getBalanceManager().getBalance(from));
    }

    /**
     * Returns the bank balance of the specified player.
     * Safe to call from any thread.
     *
     * @param uuid the player's unique ID; must not be {@code null}
     * @return the player's current bank balance
     * @throws IllegalArgumentException if {@code uuid} is {@code null}
     */
    public double getBankBalance(UUID uuid) {
        requireUuid(uuid, "uuid");
        return plugin.getBankManager().getBankBalance(uuid);
    }

    /**
     * Deposits the specified amount into the player's bank account.
     * Must be called from the main server thread.
     *
     * @param uuid   the player's unique ID; must not be {@code null}
     * @param amount the amount to deposit; must be greater than zero
     * @return a successful {@link ApiResult} with the player's new bank balance
     * @throws IllegalArgumentException if {@code uuid} is {@code null} or {@code amount} is not positive
     */
    public ApiResult<Double> depositToBank(UUID uuid, double amount) {
        requireUuid(uuid, "uuid");
        requirePositive(amount, "amount");
        plugin.getBankManager().deposit(uuid, amount);
        return ApiResult.success(plugin.getBankManager().getBankBalance(uuid));
    }

    /**
     * Withdraws the specified amount from the player's bank account.
     * Must be called from the main server thread.
     *
     * @param uuid   the player's unique ID; must not be {@code null}
     * @param amount the amount to withdraw; must be greater than zero
     * @return a successful {@link ApiResult} with the new bank balance, or a failed result with
     *         reason {@code "Insufficient bank balance"} if funds are insufficient
     * @throws IllegalArgumentException if {@code uuid} is {@code null} or {@code amount} is not positive
     */
    public ApiResult<Double> withdrawFromBank(UUID uuid, double amount) {
        requireUuid(uuid, "uuid");
        requirePositive(amount, "amount");
        boolean ok = plugin.getBankManager().withdraw(uuid, amount);
        if (!ok) return ApiResult.failure("Insufficient bank balance");
        return ApiResult.success(plugin.getBankManager().getBankBalance(uuid));
    }
}
