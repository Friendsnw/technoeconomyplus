package com.techno.economy.api;

import com.techno.economy.TechnoEconomyPlus;

/**
 * Entry point for the TechnoEconomyPlus public API.
 *
 * <p>All economy functionality available to third-party plugins is accessed through
 * this class. The instance is created when TechnoEconomyPlus enables and cleared when
 * it disables, so always guard calls with {@link #isAvailable()} before using
 * {@link #getInstance()}.</p>
 *
 * <h3>Quick start</h3>
 * <ol>
 *   <li>Add {@code softdepend: [TechnoEconomyPlus]} to your {@code plugin.yml}.</li>
 *   <li>In your {@code onEnable}, check {@code TechnoEconomyAPI.isAvailable()}.</li>
 *   <li>Obtain the instance with {@code TechnoEconomyAPI.getInstance()} and use the
 *       service getters to perform operations.</li>
 * </ol>
 *
 * <h3>Threading rules</h3>
 * <p>Mutating operations (deposit, withdraw, transfer, share trading, company fund
 * changes) must be called from the main server thread. All read-only operations are
 * thread-safe. See individual service Javadoc for per-method details.</p>
 *
 * <h3>Error model</h3>
 * <p>{@link IllegalArgumentException} is thrown for programming errors (null arguments,
 * non-positive amounts). Business failures (insufficient balance, company not found, etc.)
 * are communicated through a failed {@link ApiResult} — no exception is thrown.</p>
 */
public final class TechnoEconomyAPI {

    /**
     * The current API version string.
     * Compatible additions within the same major version will not change this constant.
     * Consuming plugins may read this at startup to verify compatibility.
     */
    public static final String API_VERSION = "1.0.0";

    private static volatile TechnoEconomyAPI instance;

    private final EconomyService economyService;
    private final StockService stockService;
    private final CompanyService companyService;
    private final PlayerDataService playerDataService;

    private TechnoEconomyAPI(TechnoEconomyPlus plugin) {
        this.economyService = new EconomyService(plugin);
        this.stockService = new StockService(plugin);
        this.companyService = new CompanyService(plugin);
        this.playerDataService = new PlayerDataService(plugin);
    }

    /**
     * Initialises the API singleton. This method is called automatically by
     * TechnoEconomyPlus during {@code onEnable}. External plugins must not call it.
     *
     * @param plugin the TechnoEconomyPlus plugin instance; must not be {@code null}
     * @throws IllegalArgumentException if {@code plugin} is {@code null}
     */
    public static void initialize(TechnoEconomyPlus plugin) {
        if (plugin == null) throw new IllegalArgumentException("plugin must not be null");
        instance = new TechnoEconomyAPI(plugin);
    }

    /**
     * Clears the API singleton. This method is called automatically by TechnoEconomyPlus
     * during {@code onDisable}. External plugins must not call it.
     */
    public static void shutdown() {
        instance = null;
    }

    /**
     * Returns {@code true} if the API has been initialised and is ready to use.
     * Always check this before calling {@link #getInstance()}, particularly in
     * plugins that declare TechnoEconomyPlus as a {@code softdepend}.
     *
     * @return {@code true} if the API is currently available
     */
    public static boolean isAvailable() {
        return instance != null;
    }

    /**
     * Returns the singleton API instance.
     *
     * @return the current {@code TechnoEconomyAPI} instance; never {@code null}
     * @throws IllegalStateException if {@link #isAvailable()} returns {@code false}
     */
    public static TechnoEconomyAPI getInstance() {
        TechnoEconomyAPI ref = instance;
        if (ref == null) {
            throw new IllegalStateException(
                    "TechnoEconomyAPI is not available. " +
                    "Call isAvailable() first and ensure TechnoEconomyPlus is listed " +
                    "as a softdepend in your plugin.yml.");
        }
        return ref;
    }

    /**
     * Returns the {@link EconomyService} for wallet balance reads/writes and bank operations.
     *
     * @return the economy service; never {@code null}
     */
    public EconomyService getEconomyService() {
        return economyService;
    }

    /**
     * Returns the {@link StockService} for ore prices, company share prices, and share trading.
     *
     * @return the stock service; never {@code null}
     */
    public StockService getStockService() {
        return stockService;
    }

    /**
     * Returns the {@link CompanyService} for company creation and company fund management.
     *
     * @return the company service; never {@code null}
     */
    public CompanyService getCompanyService() {
        return companyService;
    }

    /**
     * Returns the {@link PlayerDataService} for read-only player statistics
     * (checks, duels, jobs, casino).
     *
     * @return the player data service; never {@code null}
     */
    public PlayerDataService getPlayerDataService() {
        return playerDataService;
    }
}
