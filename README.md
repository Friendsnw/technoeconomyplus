# TechnoEconomyPlus

A completely free economy ecosystem for your Minecraft server, consisting of over 20 modules.
All modules, features, languages, functions, placeholders and for developer informations are here.

For easy start, please visit [our wiki](https://wiki.teknodev.qzz.io/v1/getting-started?plugin=technoeconomyplus)

# FEATURES
- ✅ No any provider will be require 
- ✅ No any ads for players
- ✅ For high-performance optimized plugin
- ✅ Open source
- ✅ All-in 1 plugin
- ✅ Free forever
- ✅ Vault supported
- ✅ Compatible with all economy plugins.
- ✅ Security database with sql file.
- ✅ Thanks to the backup system, you can instantly back up your player data and server configuration whenever you wish.
- ✅ Basic and fully described config and language files. 
- ✅ You can change toggle of everything like commands and modules.
- ✅ You don't must to have any internet for work this plugin.
---
# Our modules
- ✅ Jobs
- ✅ HUD
- ✅ Mob health display
- ✅ Trash can
- ✅ Sell GUI
- ✅ Stock market
- ✅ Company
- ✅ Bank & check system
- ✅ Duel
- ✅ Bank
- ✅ Daily rewards
- ✅ Lottery
- ✅ Casino
- ✅ Bounty
- ✅ Auction house
- ✅ Market
- ✅ Chest shops
- ✅ Payday
- ✅ Clans
---
Coming soon:
- ❌ Point system
- ❌ Webmap system
- ❌ Web-based scoreboard

# Why TechnoEconomyPlus?
Because technoeconomyplus is not just a single plugin; it is a comprehensive framework—optimized specifically for Survival servers—that features dozens of modules designed to minimize the total number of plugins running on your server. It does not rely on any external economy providers, operating instead entirely within its own ecosystem. It is also fully compatible with the scripts you may choose to use (command: /eco give %player% (amount of money to grant)) .
**WARNİNG:** You can use Vault with our plugin but do not use Essentials or any economy provider. Some commands can be exist!

# Default languages
**Please note:** Translations may not be 100% accurate. Please ensure there are no linguistic errors in the relevant language before using the plugin on a public-facing server.

| Language | Country | Language code |
|---|---|---|
| **English** | USA (DEFAULT)  | en |
| **Turkish** | Turkiye        | tr |
| **French** | France          | fr |
| **German** | Germany         | de |
| **Russian** | Russia         | ru |
| **Italian** | Italy          | it |
| **Portuguese** | Portugal    | pt |

---

# TechnoEconomy Placeholders

All placeholders must be wrapped in `%...%` and use the `technoeconomy` identifier and can be used at scoreboards, messages, GUI's, tabs and more... (at only supported plugins) PLEASE NOTE: The plugin you are using must state "Placeholders supported" among its features. If you are a plugin developer, checking developers.teknodev.qzz.io might be helpful.

---

| Turkish Placeholder | English Placeholder | Description |
|---|---|---|
| `%technoeconomy_balance%` | `%technoeconomy_balance%` | Player's raw balance (e.g. `1250.5`) |
| `%technoeconomy_balance_formatted%` | `%technoeconomy_balance_formatted%` | Balance with currency symbol and formatting (e.g. `$1,250.50`) |
| `%technoeconomy_bank_balance%` | `%technoeconomy_bank_balance%` | Player's raw bank balance |
| `%technoeconomy_bank_balance_formatted%` | `%technoeconomy_bank_balance_formatted%` | Bank balance with currency symbol and formatting |
| `%technoeconomy_borsa_altin%` | `%technoeconomy_stock_gold%` | Current price of Gold Ingot on the stock market |
| `%technoeconomy_borsa_elmas%` | `%technoeconomy_stock_diamond%` | Current price of Diamond on the stock market |
| `%technoeconomy_borsa_portfoy_deger%` | `%technoeconomy_stock_portfolio_value%` | Total value of the player's stock portfolio |
| `%technoeconomy_borsa_kar_zarar%` | `%technoeconomy_stock_profit_loss%` | Player's profit/loss on stocks |
| `%technoeconomy_sirket_ad%` | `%technoeconomy_company_name%` | Player's company name (returns `Yok` = "None" if not in a company) |
| `%technoeconomy_sirket_bakiye%` | `%technoeconomy_company_balance%` | Company's balance (returns `0.0` if no company) |
| `%technoeconomy_sirket_vergi_borcu%` | `%technoeconomy_company_tax_debt%` | Company's outstanding tax debt |
| `%technoeconomy_sirket_piyasa_degeri%` | `%technoeconomy_company_market_cap%` | Company's market capitalization |
| `%technoeconomy_sirket_isci_sayisi%` | `%technoeconomy_company_workers%` | Number of workers employed by the company |
| `%technoeconomy_cek_sayisi%` | `%technoeconomy_check_count%` | Number of checks the player currently holds |
| `%technoeconomy_cek_son_miktar%` | `%technoeconomy_check_last_amount%` | Amount of the player's most recent check |
| `%technoeconomy_duello_kazanim%` | `%technoeconomy_duel_winnings%` | Total money won from duels |
| `%technoeconomy_duello_galibiyet%` | `%technoeconomy_duel_wins%` | Total duel wins |
| `%technoeconomy_duello_maglubiyet%` | `%technoeconomy_duel_losses%` | Total duel losses |
| `%technoeconomy_current_job%` | `%technoeconomy_job%` | Player's current job |
| `%technoeconomy_casino_spins%` | `%technoeconomy_casino_spins%` | Total number of casino spins |
| `%technoeconomy_casino_won%` | `%technoeconomy_casino_won%` | Total amount won in casino |
| `%technoeconomy_casino_lost%` | `%technoeconomy_casino_lost%` | Total amount lost in casino |
| `%technoeconomy_casino_net%` | `%technoeconomy_casino_net%` | Net casino result (`won − lost`) |

---

-💡 **Please note:** All placeholders require the `PlaceholderAPI` plugin and can be used in scoreboards, tabs, messages, and GUIs.

- **All placeholders return `""` (empty) if `player` is null** (e.g. when used in console or non-player contexts).
- **Turkish and English aliases are interchangeable** — use whichever you prefer.
- **Formatting**: `balance_formatted` and `bank_balance_formatted` use `DecimalFormat("#,##0.00")` plus the `economy.currency-symbol` config value (default `$`).
- **Missing data fallbacks**: `company_name` → `"Yok"`, `company_balance` / `company_tax_debt` / `company_market_cap` → `"0.0"`, `company_workers` → `"0"`.
- Unknown identifiers return `null` (PlaceholderAPI shows the placeholder unchanged).

# For developers
If you are a developer, you can utilize the TechnoEconomyPlusAPI as a supplementary tool within your own plugins. If you run a server and are developing plugins for it, your plugin may require an economy system. To integrate with this ecosystem, you can make use of placeholders or, if you have the necessary technical knowledge, the API itself. For further details, please visit [our website](https://wiki.teknodev.qzz.io/v1/developer-guide?plugin=technoeconomyplus)

# Spark results
In our long-term Spark tests using our plugin, the average total core usage is 0.47%*

*This figure was reached on a single-player server. On servers with a large number of players, this figure may be higher. If you are unsure which module is causing the issue, there is a 99% chance that it is the HUD module. You can disable the HUD module via the `config.yml` file or by using the `/hud toggle` command.

# Plugin support
If you need to a support, you can leave me a message at my Discord: technodev_mc
If you find a bug, you can report for us at [our report webpage](https://teknodev.qzz.io/v1/report-bugs).
Thanks to use our Minecraft plugin, developed with hearth >3
