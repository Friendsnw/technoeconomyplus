package com.techno.economy;

import com.techno.economy.api.LicensablePlugin;
import com.techno.economy.auction.AuctionHouseManager;
import com.techno.economy.backup.BackupManager;
import com.techno.economy.balance.BalanceManager;
import com.techno.economy.bank.BankManager;
import com.techno.economy.bounty.BountyManager;
import com.techno.economy.check.CheckManager;
import com.techno.economy.clan.ClanManager;
import com.techno.economy.commands.*;
import com.techno.economy.company.CompanyManager;
import com.techno.economy.daily.DailyManager;
import com.techno.economy.database.DatabaseManager;
import com.techno.economy.duel.DuelManager;
import com.techno.economy.feature.FeatureManager;
import com.techno.economy.games.CasinoManager;
import com.techno.economy.games.LotteryManager;
import com.techno.economy.gui.ClanGui;
import com.techno.economy.hud.HudManager;
import com.techno.economy.jobs.JobsListener;
import com.techno.economy.jobs.JobsManager;
import com.techno.economy.language.LanguageManager;
import com.techno.economy.license.LicenseManager;
import com.techno.economy.listeners.ChatInputListener;
import com.techno.economy.listeners.CheckListener;
import com.techno.economy.listeners.ChestShopListener;
import com.techno.economy.listeners.ClanGuiListener;
import com.techno.economy.listeners.ClanListener;
import com.techno.economy.listeners.DuelListener;
import com.techno.economy.listeners.GuiListener;
import com.techno.economy.listeners.MobHealthListener;
import com.techno.economy.listeners.SellGuiListener;
import com.techno.economy.listeners.SellWandListener;
import com.techno.economy.logging.TransactionLogManager;
import com.techno.economy.menu.MenuLoader;
import com.techno.economy.placeholders.TechnoEconomyExpansion;
import com.techno.economy.security.TrojanDetector;
import com.techno.economy.shop.ChestShopManager;
import com.techno.economy.shop.MarketManager;
import com.techno.economy.shop.SellWandManager;
import com.techno.economy.shop.WorthManager;
import com.techno.economy.stock.StockManager;
import com.techno.economy.util.BannerUtil;
import com.techno.economy.util.ChatInputManager;
import com.techno.economy.util.PermissionGate;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class TechnoEconomyPlus extends JavaPlugin implements LicensablePlugin {

    private static TechnoEconomyPlus instance;
    private LanguageManager languageManager;
    private BalanceManager balanceManager;
    private LicenseManager licenseManager;
    private PermissionGate permissionGate;
    private BackupManager backupManager;
    private TransactionLogManager transactionLogManager;
    private DatabaseManager databaseManager;
    private MenuLoader menuLoader;
    private FeatureManager featureManager;
    private HudManager hudManager;
    private ChatInputManager chatInputManager;
    private com.techno.economy.util.SignInputManager signInputManager;
    private BankManager bankManager;
    private DailyManager dailyManager;
    private BountyManager bountyManager;
    private JobsManager jobsManager;
    private CasinoManager casinoManager;
    private LotteryManager lotteryManager;
    private WorthManager worthManager;
    private SellWandManager sellWandManager;
    private ChestShopManager chestShopManager;
    private MarketManager marketManager;
    private AuctionHouseManager auctionHouseManager;
    private StockManager stockManager;
    private CompanyManager companyManager;
    private CheckManager checkManager;
    private DuelManager duelManager;
    private ClanManager clanManager;
    private ClanGui clanGui;
    private TrojanDetector trojanDetector;

    @Override
    public void onEnable() {
        instance = this;

        if (!checkVersions()) return;

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            boolean tr = "tr".equalsIgnoreCase(getConfig().getString("language", "en"));
            getLogger().severe(tr
                    ? "PlaceholderAPI eklentisi bulunamadı! technoEconomyPlus, çalışabilmek için PlaceholderAPI gerektirir."
                    : "PlaceholderAPI plugin not found! technoEconomyPlus requires PlaceholderAPI to function. Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();

        languageManager = new LanguageManager(this);
        languageManager.load();

        featureManager = new FeatureManager(this);
        featureManager.load();

        permissionGate = new PermissionGate(this);
        backupManager = new BackupManager(this);
        chatInputManager = new ChatInputManager(this);
        signInputManager = new com.techno.economy.util.SignInputManager(this);

        licenseManager = new LicenseManager(this);
        licenseManager.registerPlugin(this);

        databaseManager = new DatabaseManager(this);
        databaseManager.init();

        backupManager.prepareBalancesFileOnStartup();

        menuLoader = new MenuLoader(this);
        menuLoader.load();

        transactionLogManager = new TransactionLogManager(this);

        balanceManager = new BalanceManager(this);
        balanceManager.load();
        balanceManager.scheduleAutoSave();

        bankManager = new BankManager(this);
        bankManager.load();

        dailyManager = new DailyManager(this);
        dailyManager.load();

        bountyManager = new BountyManager(this);
        bountyManager.load();

        jobsManager = new JobsManager(this);
        jobsManager.load();

        casinoManager = new CasinoManager(this);

        lotteryManager = new LotteryManager(this);
        lotteryManager.start();

        worthManager = new WorthManager(this);
        worthManager.load();

        sellWandManager = new SellWandManager(this);

        chestShopManager = new ChestShopManager(this);
        chestShopManager.load();

        marketManager = new MarketManager(this);
        marketManager.load();

        auctionHouseManager = new AuctionHouseManager(this);
        auctionHouseManager.load();

        stockManager = new StockManager(this);
        stockManager.start();

        companyManager = new CompanyManager(this);
        companyManager.start();

        checkManager = new CheckManager(this);
        duelManager = new DuelManager(this);

        clanManager = new ClanManager(this);
        clanManager.start();
        clanGui = new ClanGui(this);

        hudManager = new HudManager(this);
        if (featureManager.isEnabled("hud")) {
            hudManager.start();
        }

        trojanDetector = new TrojanDetector(this);
        trojanDetector.start();

        registerCommands();
        registerListeners();
        setupVault();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new TechnoEconomyExpansion(this).register();
        }

        BannerUtil.printStartupBanner(languageManager.getCurrentLanguage());

        Bukkit.getScheduler().runTaskLater(this, () ->
                BannerUtil.printStarMessage(languageManager.getCurrentLanguage()), 40L);

        if (getConfig().getBoolean("updates.check", true) && licenseManager.isUpdateAvailable()) {
            boolean tr = "tr".equalsIgnoreCase(getConfig().getString("language", "en"));
            getLogger().warning(licenseManager.getUpdateNoticeMessage(tr));
        }
    }

    private void registerCommands() {
        registerCmd("balance", new BalanceCommand(this));
        registerCmd("eco", new EcoCommand(this));
        registerCmd("pay", new PayCommand(this));
        registerCmd("baltop", new BaltopCommand(this));
        registerCmd("bank", new BankCommand(this));
        registerCmd("daily", new DailyCommand(this));
        registerCmd("bounty", new BountyCommand(this));
        registerCmd("jobs", new JobsCommand(this));
        registerCmd("casino", new CasinoCommand(this));
        registerCmd("coinflip", new CoinflipCommand(this));
        registerCmd("lottery", new LotteryCommand(this));
        registerCmd("worth", new WorthCommand(this));
        registerCmd("sell", new SellCommand(this));
        registerCmd("sellwand", new SellWandCommand(this));
        registerCmd("market", new MarketCommand(this));
        registerCmd("shop", new ShopCommand(this));
        registerCmd("ah", new AhCommand(this));
        registerCmd("chestshop", new ChestShopCommand(this));
        registerCmd("payday", new PaydayCommand(this));
        registerCmd("trash", new TrashCommand(this));
        registerCmd("hud", new HudCommand(this));
        registerCmd("technoeconomyplus", new TechnoEconomyPlusCommand(this));
        registerCmd("stock", new StockCommand(this));
        registerCmd("company", new CompanyCommand(this));
        registerCmd("check", new CheckCommand(this));
        registerCmd("duel", new DuelCommand(this));
        registerCmd("clan", new ClanCommand(this));
        registerCmd("clanadmin", new ClanAdminCommand(this));
    }

    private void registerCmd(String name, Object handler) {
        var cmd = getCommand(name);
        if (cmd != null) {
            if (handler instanceof org.bukkit.command.CommandExecutor ce) cmd.setExecutor(ce);
            if (handler instanceof org.bukkit.command.TabCompleter tc) cmd.setTabCompleter(tc);
        }
    }

    private void registerListeners() {
        var pm = Bukkit.getPluginManager();
        pm.registerEvents(new GuiListener(this), this);
        pm.registerEvents(new SellWandListener(this), this);
        pm.registerEvents(new ChestShopListener(this), this);
        pm.registerEvents(new JobsListener(this), this);
        pm.registerEvents(new CheckListener(this), this);
        pm.registerEvents(new DuelListener(this), this);
        pm.registerEvents(new MobHealthListener(this), this);
        pm.registerEvents(new SellGuiListener(this), this);
        pm.registerEvents(new ChatInputListener(this), this);
        pm.registerEvents(new com.techno.economy.listeners.SignInputListener(this), this);
        pm.registerEvents(new com.techno.economy.listeners.PlayerConnectionListener(this), this);
        pm.registerEvents(new ClanListener(this), this);
        pm.registerEvents(new ClanGuiListener(this), this);
        com.techno.economy.api.TechnoEconomyAPI.initialize(this);
    }

    private void setupVault() {
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            try {
                Bukkit.getServicesManager().register(
                        net.milkbowl.vault.economy.Economy.class,
                        new com.techno.economy.vault.VaultEconomyHook(this),
                        this,
                        org.bukkit.plugin.ServicePriority.Highest
                );
                getLogger().info("Successfully hooked into Vault Economy as service provider!");
            } catch (Throwable t) {
                getLogger().warning("Failed to register Vault Economy service: " + t.getMessage());
            }
        }
    }

    @Override
    public void onDisable() {
        Bukkit.getServicesManager().unregisterAll(this);
        com.techno.economy.api.TechnoEconomyAPI.shutdown();
        BannerUtil.printShutdownMessages(languageManager != null ? languageManager.getCurrentLanguage() : "en");

        if (trojanDetector != null) trojanDetector.stop();
        if (hudManager != null) hudManager.stop();
        if (lotteryManager != null) lotteryManager.stop();
        if (auctionHouseManager != null) auctionHouseManager.shutdown();
        if (bankManager != null) bankManager.shutdown();
        if (balanceManager != null) balanceManager.shutdown();
        if (dailyManager != null) dailyManager.save();
        if (bountyManager != null) bountyManager.save();
        if (jobsManager != null) jobsManager.save();
        if (chestShopManager != null) chestShopManager.shutdown();
        if (marketManager != null) marketManager.save();
        if (stockManager != null) stockManager.stop();
        if (companyManager != null) companyManager.stop();
        if (clanManager != null) clanManager.stop();
        if (databaseManager != null) databaseManager.close();
    }

    private boolean checkVersions() {
        String version = Bukkit.getVersion();
        boolean versionOk = false;
        try {
            String[] parts = version.split("MC: ");
            if (parts.length > 1) {
                String mcVer = parts[1].replace(")", "").trim();
                String[] nums = mcVer.split("\\.");
                if (nums.length >= 2) {
                    int major = Integer.parseInt(nums[0]);
                    int minor = Integer.parseInt(nums[1]);
                    versionOk = (major == 1 && minor >= 20) || major > 1;
                }
            }
        } catch (Exception e) {
            versionOk = version.contains("1.20") || version.contains("1.21") || version.contains("1.22");
        }

        boolean javaOk = false;
        try {
            String[] parts = System.getProperty("java.version").split("\\.");
            int major = parts[0].equals("1") ? Integer.parseInt(parts[1]) : Integer.parseInt(parts[0]);
            javaOk = major >= 17;
        } catch (Exception ignored) {}

        if (!versionOk || !javaOk) {
            getLogger().severe("Kullandığınız java veya Minecraft sürümü güncel değil gibi görünüyor. Yalnızca Minecraft 1.20x üzeri ve Java 17 üzeri desteklenmektedir.");
            Bukkit.getPluginManager().disablePlugin(this);
            return false;
        }
        return true;
    }

    public static TechnoEconomyPlus getInstance() { return instance; }
    public LanguageManager getLanguageManager() { return languageManager; }
    public BalanceManager getBalanceManager() { return balanceManager; }
    public LicenseManager getLicenseManager() { return licenseManager; }
    public PermissionGate getPermissionGate() { return permissionGate; }
    public BackupManager getBackupManager() { return backupManager; }
    public TransactionLogManager getTransactionLogManager() { return transactionLogManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public MenuLoader getMenuLoader() { return menuLoader; }
    public FeatureManager getFeatureManager() { return featureManager; }
    public HudManager getHudManager() { return hudManager; }
    public ChatInputManager getChatInputManager() { return chatInputManager; }
    public com.techno.economy.util.SignInputManager getSignInputManager() { return signInputManager; }
    public BankManager getBankManager() { return bankManager; }
    public DailyManager getDailyManager() { return dailyManager; }
    public BountyManager getBountyManager() { return bountyManager; }
    public JobsManager getJobsManager() { return jobsManager; }
    public CasinoManager getCasinoManager() { return casinoManager; }
    public LotteryManager getLotteryManager() { return lotteryManager; }
    public WorthManager getWorthManager() { return worthManager; }
    public SellWandManager getSellWandManager() { return sellWandManager; }
    public ChestShopManager getChestShopManager() { return chestShopManager; }
    public MarketManager getMarketManager() { return marketManager; }
    public AuctionHouseManager getAuctionHouseManager() { return auctionHouseManager; }
    public StockManager getStockManager() { return stockManager; }
    public CompanyManager getCompanyManager() { return companyManager; }
    public CheckManager getCheckManager() { return checkManager; }
    public DuelManager getDuelManager() { return duelManager; }
    public ClanManager getClanManager() { return clanManager; }
    public ClanGui getClanGui() { return clanGui; }

    public void reloadAllFiles() throws Exception {
        reloadConfig();
        languageManager.load();
        featureManager.load();
        menuLoader.load();
        worthManager.load();
        jobsManager.loadJobDefinitions();
        balanceManager.load();
    }

    @Override
    public String getPluginId() { return "technoEconomyPlus"; }

    @Override
    public String getDisplayName() { return "technoEconomyPlus"; }
}
