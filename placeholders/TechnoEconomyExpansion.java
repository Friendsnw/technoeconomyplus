package com.techno.economy.placeholders;

import com.techno.economy.TechnoEconomyPlus;
import com.techno.economy.clan.Clan;
import com.techno.economy.clan.ClanMember;
import com.techno.economy.clan.ClanRank;
import com.techno.economy.company.CompanyManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;

public class TechnoEconomyExpansion extends PlaceholderExpansion {

    private final TechnoEconomyPlus plugin;
    private final DecimalFormat formatter = new DecimalFormat("#,##0.00");

    public TechnoEconomyExpansion(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "technoeconomy";
    }

    @Override
    public @NotNull String getAuthor() {
        return "technodev";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) return "";

        double balance = plugin.getBalanceManager().getBalance(player.getUniqueId());
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");

        switch (identifier.toLowerCase()) {
            case "balance":
                return String.valueOf(balance);
            case "balance_formatted":
                return symbol + formatter.format(balance);
            case "bank_balance":
                return String.valueOf(plugin.getBankManager().getBankBalance(player.getUniqueId()));
            case "bank_balance_formatted":
                return symbol + formatter.format(plugin.getBankManager().getBankBalance(player.getUniqueId()));
            case "stock_gold":
            case "borsa_altin":
                return String.valueOf(plugin.getStockManager().getOrePrice(Material.GOLD_INGOT));
            case "stock_diamond":
            case "borsa_elmas":
                return String.valueOf(plugin.getStockManager().getOrePrice(Material.DIAMOND));
            case "stock_portfolio_value":
            case "borsa_portfoy_deger":
                return String.valueOf(plugin.getStockManager().getPortfolioValue(player.getUniqueId()));
            case "stock_profit_loss":
            case "borsa_kar_zarar":
                return String.valueOf(plugin.getStockManager().getProfitLoss(player.getUniqueId()));
            case "company_name":
            case "sirket_ad": {
                CompanyManager.CompanyData c = plugin.getCompanyManager().getPlayerCompany(player.getUniqueId());
                return c != null ? c.getName() : "None";
            }
            case "company_balance":
            case "sirket_bakiye": {
                CompanyManager.CompanyData c = plugin.getCompanyManager().getPlayerCompany(player.getUniqueId());
                return c != null ? String.valueOf(c.getBalance()) : "0.0";
            }
            case "company_tax_debt":
            case "sirket_vergi_borcu": {
                CompanyManager.CompanyData c = plugin.getCompanyManager().getPlayerCompany(player.getUniqueId());
                return c != null ? String.valueOf(c.getTaxDebt()) : "0.0";
            }
            case "company_market_cap":
            case "sirket_piyasa_degeri": {
                CompanyManager.CompanyData c = plugin.getCompanyManager().getPlayerCompany(player.getUniqueId());
                return c != null ? String.valueOf(plugin.getCompanyManager().getMarketCap(c.getName())) : "0.0";
            }
            case "company_workers":
            case "sirket_isci_sayisi": {
                CompanyManager.CompanyData c = plugin.getCompanyManager().getPlayerCompany(player.getUniqueId());
                return c != null ? String.valueOf(plugin.getCompanyManager().getWorkers(c.getName()).size()) : "0";
            }
            case "check_count":
            case "cek_sayisi":
                return String.valueOf(plugin.getCheckManager().getPlayerCheckCount(player.getUniqueId()));
            case "check_last_amount":
            case "cek_son_miktar":
                return String.valueOf(plugin.getCheckManager().getLastCheckAmount(player.getUniqueId()));
            case "duel_winnings":
            case "duello_kazanim":
                return String.valueOf(plugin.getDuelManager().getTotalWinnings(player.getUniqueId()));
            case "duel_wins":
            case "duello_galibiyet":
                return String.valueOf(plugin.getDuelManager().getWins(player.getUniqueId()));
            case "duel_losses":
            case "duello_maglubiyet":
                return String.valueOf(plugin.getDuelManager().getLosses(player.getUniqueId()));
            case "current_job":
            case "job":
                return plugin.getJobsManager().getJob(player.getUniqueId());
            case "casino_spins":
                return String.valueOf(plugin.getDatabaseManager().getCasinoStats(player.getUniqueId()).spins);
            case "casino_won":
                return String.valueOf(plugin.getDatabaseManager().getCasinoStats(player.getUniqueId()).totalWon);
            case "casino_lost":
                return String.valueOf(plugin.getDatabaseManager().getCasinoStats(player.getUniqueId()).totalLost);
            case "casino_net": {
                var stats = plugin.getDatabaseManager().getCasinoStats(player.getUniqueId());
                return String.valueOf(stats.totalWon - stats.totalLost);
            }
            case "clan_in_clan": {
                return String.valueOf(plugin.getClanManager().getClanByPlayer(player.getUniqueId()) != null);
            }
            case "clan_name": {
                Clan c = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
                return c != null ? c.getName() : "";
            }
            case "clan_tag": {
                Clan c = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
                return c != null ? c.getTag() : "";
            }
            case "clan_tag_formatted": {
                Clan c = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
                return c != null ? "[" + c.getTag() + "]" : "";
            }
            case "clan_rank": {
                Clan c = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
                if (c == null) return "";
                ClanRank r = c.getMemberRank(player.getUniqueId());
                return r != null ? r.name() : "";
            }
            case "clan_level": {
                Clan c = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
                return c != null ? String.valueOf(c.getLevel()) : "0";
            }
            case "clan_xp": {
                Clan c = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
                return c != null ? String.valueOf(c.getXp()) : "0";
            }
            case "clan_xp_next": {
                Clan c = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
                return c != null ? String.valueOf(plugin.getClanManager().getXpRequiredForLevel(c.getLevel())) : "0";
            }
            case "clan_xp_progress": {
                Clan c = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
                if (c == null) return "0.0%";
                long req = plugin.getClanManager().getXpRequiredForLevel(c.getLevel());
                double pct = req > 0 ? (double) c.getXp() / (double) req * 100.0 : 100.0;
                return String.format("%.1f%%", pct);
            }
            case "clan_bank": {
                Clan c = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
                return c != null ? String.valueOf(c.getBankBalance()) : "0.0";
            }
            case "clan_bank_formatted": {
                Clan c = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
                return c != null ? symbol + formatter.format(c.getBankBalance()) : symbol + "0.00";
            }
            case "clan_members_count": {
                Clan c = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
                return c != null ? String.valueOf(c.getMembers().size()) : "0";
            }
            case "clan_members_max": {
                Clan c = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
                return c != null ? String.valueOf(plugin.getClanManager().getMaxMembers(c)) : "0";
            }
            case "clan_allies_count": {
                Clan c = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
                return c != null ? String.valueOf(c.getAllies().size()) : "0";
            }
            case "clan_enemies_count": {
                Clan c = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
                return c != null ? String.valueOf(c.getEnemies().size()) : "0";
            }
            case "clan_kdr": {
                Clan c = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
                return c != null ? String.format("%.2f", c.getKdr()) : "0.00";
            }
            case "clan_kills": {
                Clan c = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
                return c != null ? String.valueOf(c.getKills()) : "0";
            }
            case "clan_deaths": {
                Clan c = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
                return c != null ? String.valueOf(c.getDeaths()) : "0";
            }
            case "clan_war_wins": {
                Clan c = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
                return c != null ? String.valueOf(c.getWarWins()) : "0";
            }
            case "clan_war_losses": {
                Clan c = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
                return c != null ? String.valueOf(c.getWarLosses()) : "0";
            }
            case "clan_war_status": {
                Clan c = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
                if (c == null) return "none";
                return c.getActiveWar() != null ? "active" : "none";
            }
            default:
                return null;
        }
    }
}
