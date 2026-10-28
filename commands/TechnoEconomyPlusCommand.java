package com.techno.economy.commands;

import com.techno.economy.TechnoEconomyPlus;
import com.techno.economy.license.LicenseMessages;
import com.techno.economy.util.BannerUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TechnoEconomyPlusCommand implements CommandExecutor, TabCompleter {

    private final TechnoEconomyPlus plugin;

    public TechnoEconomyPlusCommand(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean tr = false;

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender, tr);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("admin") || sub.equals("gui") || sub.equals("menu")) {
            if (!hasAdminPermission(sender)) {
                sender.sendMessage(LicenseMessages.getNoPermissionMessage(tr));
                return true;
            }
            if (sender instanceof Player player) {
                new com.techno.economy.gui.AdminGui(plugin).open(player);
                return true;
            }
            sendHelp(sender, tr);
            return true;
        }

        if (sub.equals("reload")) {
            if (!hasAdminPermission(sender)) {
                sender.sendMessage(LicenseMessages.getNoPermissionMessage(tr));
                return true;
            }

            try {
                plugin.reloadAllFiles();
                sender.sendMessage(LicenseMessages.getReloadSuccessMessage(tr));
            } catch (Exception e) {
                sender.sendMessage(LicenseMessages.getReloadErrorMessage(tr, e.getMessage()));
            }
            return true;
        }

        if (sub.equals("license")) {
            if (!hasAdminPermission(sender)) {
                sender.sendMessage(LicenseMessages.getNoPermissionMessage(tr));
                return true;
            }

            sendLicenseInfo(sender, tr);
            return true;
        }

        if (sub.equals("usages") || sub.equals("usage")) {
            if (!hasAdminPermission(sender)) {
                sender.sendMessage(LicenseMessages.getNoPermissionMessage(tr));
                return true;
            }

            sendUsagesInfo(sender, tr);
            return true;
        }

        sender.sendMessage(LicenseMessages.getUnknownSubcommandMessage(tr));
        return true;
    }

    private boolean hasAdminPermission(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) return true;
        return sender.hasPermission("technoeconomyplus.admin") || sender.isOp();
    }

    private void sendLicenseInfo(CommandSender sender, boolean tr) {
        var lm = plugin.getLicenseManager();
        sender.sendMessage("§8§m----------------------------------------------------");
        sender.sendMessage(tr ? "§e§lTechnoEconomyPlus - Lisans Bilgileri" : "§e§lTechnoEconomyPlus - License Information");
        sender.sendMessage("§8§m----------------------------------------------------");
        sender.sendMessage(tr ? "§6Durum: §aGeçerli" : "§6Status: §aValid");
        String pkg = (lm.getPackageTier() != null && !lm.getPackageTier().isBlank() && !lm.getPackageTier().equalsIgnoreCase("null"))
                ? lm.getPackageTier().toUpperCase() : (tr ? "Belirlenmemiş" : "Unspecified");
        sender.sendMessage(tr ? ("§6Paket: §f" + pkg) : ("§6Package: §f" + pkg));

        String ip = lm.getAllowedIp();
        if (ip == null || ip.isBlank() || ip.equalsIgnoreCase("null")) {
            ip = tr ? "Her sunucuda kullanılabilir" : "Usable on all servers";
        }
        sender.sendMessage(tr ? "§6İzin Verilen IP: §f" + ip : "§6Allowed IP: §f" + ip);

        String exp = lm.getExpiresAt();
        if (exp == null || exp.isBlank() || exp.equalsIgnoreCase("null")) {
            exp = tr ? "Belirlenmemiş" : "Unspecified";
        }
        sender.sendMessage(tr ? "§6Bitiş Tarihi: §f" + exp : "§6Expires At: §f" + exp);

        int days = lm.getDaysRemaining();
        if (days >= 0) {
            sender.sendMessage(tr ? "§6Kalan Gün: §f" + days : "§6Days Remaining: §f" + days);
        } else {
            sender.sendMessage(tr ? "§6Kalan Gün: §fBelirlenmemiş" : "§6Days Remaining: §fUnspecified");
        }
        sender.sendMessage("§8§m----------------------------------------------------");
    }

    private void sendUsagesInfo(CommandSender sender, boolean tr) {
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = Runtime.getRuntime().maxMemory();

        File root = new File(".");
        long totalDisk = root.getTotalSpace();
        long freeDisk = root.getFreeSpace();
        long usedDisk = totalDisk - freeDisk;

        double cpuPercent = 0.0;
        try {
            java.lang.management.OperatingSystemMXBean osBean = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            if (osBean instanceof com.sun.management.OperatingSystemMXBean sunBean) {
                double load = sunBean.getCpuLoad();
                if (load >= 0) cpuPercent = Math.round(load * 1000.0) / 10.0;
            }
        } catch (Exception ignored) {}

        double[] tpsArr = Bukkit.getTPS();
        double tps1m = Math.min(20.0, Math.round(tpsArr[0] * 100.0) / 100.0);
        double tps5m = Math.min(20.0, Math.round(tpsArr[1] * 100.0) / 100.0);
        double tps15m = Math.min(20.0, Math.round(tpsArr[2] * 100.0) / 100.0);
        double mspt = Math.round(Bukkit.getAverageTickTime() * 10.0) / 10.0;

        int pluginCount = Bukkit.getPluginManager().getPlugins().length;

        sender.sendMessage("§e======================");
        sender.sendMessage("§eᴛᴇᴄʜɴᴏ§6ᴇᴄᴏɴᴏᴍʏ§bᴘʟᴜꜱ");
        sender.sendMessage(tr ? "§eSunucu durum bilgisi" : "§eServer Status Information");
        sender.sendMessage("");
        sender.sendMessage(tr ? "§6Toplam RAM kullanımı: §f" + BannerUtil.formatBytes(usedMemory) : "§6Total RAM Usage: §f" + BannerUtil.formatBytes(usedMemory));
        sender.sendMessage(tr ? "§6Toplam Disk kullanımı: §f" + BannerUtil.formatBytes(usedDisk) + " / " + BannerUtil.formatBytes(totalDisk) : "§6Total Disk Usage: §f" + BannerUtil.formatBytes(usedDisk) + " / " + BannerUtil.formatBytes(totalDisk));
        sender.sendMessage(tr ? "§6Bu sunucuya ayrılan RAM: §f" + BannerUtil.formatBytes(maxMemory) : "§6Allocated RAM: §f" + BannerUtil.formatBytes(maxMemory));
        sender.sendMessage(tr ? "§6İşletim sistemi: §f" + BannerUtil.getOsInfo() : "§6Operating System: §f" + BannerUtil.getOsInfo());
        sender.sendMessage(tr ? "§6İşlemci*: §f" + BannerUtil.getCpuInfo() : "§6Processor*: §f" + BannerUtil.getCpuInfo());
        sender.sendMessage(tr ? "§6Eklenti sayısı: §f" + pluginCount : "§6Plugin Count: §f" + pluginCount);
        sender.sendMessage(tr ? "§6Toplam CPU kullanımı (%): §f%" + cpuPercent : "§6Total CPU Usage (%): §f%" + cpuPercent);
        sender.sendMessage(tr ? ("§6TPS (1d, 5d, 15d): §a" + tps1m + "§7, §a" + tps5m + "§7, §a" + tps15m) : ("§6TPS (1m, 5m, 15m): §a" + tps1m + "§7, §a" + tps5m + "§7, §a" + tps15m));
        sender.sendMessage(tr ? ("§6Anlık MSPT: §a" + mspt + " ms") : ("§6Instant MSPT: §a" + mspt + " ms"));
        sender.sendMessage(tr ? "§6Eklentilere göre kullanılan kaynak bilgisi:" : "§6Resource Usage by Plugins:");

        for (var p : Bukkit.getPluginManager().getPlugins()) {
            long pSize = 0;
            try {
                File jar = new File(p.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
                if (jar.exists()) pSize += jar.length();
            } catch (Exception ignored) {}
            try {
                File df = p.getDataFolder();
                if (df.exists()) pSize += getFolderSize(df);
            } catch (Exception ignored) {}

            String pMemStr = pSize > 0 ? BannerUtil.formatBytes(pSize) : "< 1 MB";
            double pCpuEst = p.isEnabled() ? (p.getName().equalsIgnoreCase("technoEconomyPlus") ? 0.1 : 0.0) : 0.0;
            sender.sendMessage("§7- §f" + p.getName() + " §e" + pMemStr + " §b%" + pCpuEst);
        }

        sender.sendMessage("");
        if (tr) {
            sender.sendMessage("§7*Bazı veriler işletim sisteminden çekildiği için bazı veriler tam anlamıyla doğru olmayabilir. Bazı veriler için yalnızca Windows ve Linux desteklenir. MacOS vb. diğer alt yapılarda bu veriler düzgün olmayabilir ya da Linux dağıtımına göre...");
        } else {
            sender.sendMessage("§7*As some data is retrieved from the OS, values might not be completely exact. Only Windows and Linux are fully supported for all metrics.");
        }
        sender.sendMessage("§e======================");
    }

    private long getFolderSize(File f) {
        if (!f.exists()) return 0;
        if (!f.isDirectory()) return f.length();
        long s = 0;
        File[] ch = f.listFiles();
        if (ch != null) {
            for (File c : ch) {
                if (c.isDirectory()) s += getFolderSize(c);
                else s += c.length();
            }
        }
        return s;
    }

    private void sendHelp(CommandSender sender, boolean tr) {
        sender.sendMessage("§8§m----------------------------------------------------");
        sender.sendMessage(tr ? "§e§lTechnoEconomyPlus - Komut Rehberi" : "§e§lTechnoEconomyPlus - Command Guide");
        sender.sendMessage("§8§m----------------------------------------------------");

        if (sender instanceof ConsoleCommandSender) {
            sender.sendMessage(tr ? "§b[Konsol Komutları]" : "§b[Console Commands]");
            sender.sendMessage("§e/tep reload §7- " + (tr ? "Yapılandırma ve dosyaları yeniden yükler." : "Reloads configuration and files."));
            sender.sendMessage("§e/tep license §7- " + (tr ? "Lisans ve paket bilgilerini gösterir." : "Shows license and package details."));
            sender.sendMessage("§e/tep usages §7- " + (tr ? "Sunucu donanım ve kaynak kullanımını gösterir." : "Displays server resource metrics."));
            sender.sendMessage("§e/eco <give|take|set|reset|backup|reload> §7- " + (tr ? "Admin ekonomi yönetimi." : "Admin economy management."));
            sender.sendMessage("§8§m----------------------------------------------------");
            return;
        }

        if (hasAdminPermission(sender)) {
            sender.sendMessage(tr ? "§c[Admin Komutları]" : "§c[Admin Commands]");
            sender.sendMessage("§e/tep reload §7- " + (tr ? "Yapılandırma ve dosyaları yeniden yükler." : "Reloads configuration and files."));
            sender.sendMessage("§e/tep license §7- " + (tr ? "Lisans ve paket bilgilerini gösterir." : "Shows license and package details."));
            sender.sendMessage("§e/tep usages §7- " + (tr ? "Sunucu donanım ve kaynak kullanımını gösterir." : "Displays server resource metrics."));
            sender.sendMessage("§e/eco <give|take|set|reset|backup|reload> §7- " + (tr ? "Admin ekonomi yönetimi." : "Admin economy management."));
            sender.sendMessage("§e/sellwand give <oyuncu> <kullanım> §7- " + (tr ? "Satış asası verir." : "Gives a sell wand."));
        }

        sender.sendMessage(tr ? "§a[Oyuncu Komutları]" : "§a[Player Commands]");
        sender.sendMessage(tr ? "§e/balance [give|set|history] §7- Bakiyenizi görür veya yönetir." : "§e/balance [give|set|history] §7- Check or manage balance.");
        sender.sendMessage(tr ? "§e/pay <oyuncu> <miktar> §7- Başka bir oyuncuya para transfer eder." : "§e/pay <player> <amount> §7- Transfer money to another player.");
        sender.sendMessage(tr ? "§e/baltop [sayfa] §7- En zengin oyuncuları listeler." : "§e/baltop [page] §7- View richest players leaderboard.");
        sender.sendMessage(tr ? "§e/bank §7- Banka hesabınızı yönetir." : "§e/bank §7- Manage your personal bank account.");
        sender.sendMessage(tr ? "§e/daily §7- Günlük giriş ödülünüzü talep eder." : "§e/daily §7- Claim your daily streak reward.");
        sender.sendMessage(tr ? "§e/bounty [oyuncu|list] §7- Oyuncuların başına ödül koyar veya listeler." : "§e/bounty [player|list] §7- Place or view player bounties.");
        sender.sendMessage(tr ? "§e/jobs §7- Bir mesleğe katılıp maden/tarım yaparak para kazandırır." : "§e/jobs §7- Join jobs to earn money working.");
        sender.sendMessage(tr ? "§e/stock §7- Borsa ve hisse senedi piyasasını açar." : "§e/stock §7- Open stock market and trading.");
        sender.sendMessage(tr ? "§e/company §7- Şirket kurma ve vergi yönetimini açar." : "§e/company §7- Open company and taxation panel.");
        sender.sendMessage(tr ? "§e/check write <miktar> §7- Banka çeki oluşturur." : "§e/check write <amount> §7- Write a bank check.");
        sender.sendMessage(tr ? "§e/duel <oyuncu> <miktar> §7- Para karşılığı bahisli düello teklif eder." : "§e/duel <player> <amount> §7- Send a wager duel offer.");
        sender.sendMessage(tr ? "§e/trash §7- Çöp kutusunu açar." : "§e/trash §7- Open trash can GUI.");
        sender.sendMessage(tr ? "§e/hud toggle §7- HUD bilgi çubuğunu açıp kapatır." : "§e/hud toggle §7- Toggle actionbar HUD.");
        sender.sendMessage(tr ? "§e/worth [eşya] §7- Elinizdeki veya belirtilen eşyanın değerini gösterir." : "§e/worth [item] §7- Check value of held or specified item.");
        sender.sendMessage(tr ? "§e/sell §7- Eşya satma menüsünü açar." : "§e/sell §7- Open GUI sell window.");
        sender.sendMessage(tr ? "§e/market §7- Sayfalı ve kategorili genel pazar menüsünü açar." : "§e/market §7- Open categorical GUI market.");
        sender.sendMessage(tr ? "§e/shop §7- Sunucu resmi mağazasını açar." : "§e/shop §7- Open official admin server shop.");
        sender.sendMessage(tr ? "§e/ah §7- Açık artırma evini açar veya eşya listeler." : "§e/ah §7- Open auction house or list items.");
        sender.sendMessage(tr ? "§e/chestshop create <fiyat> §7- Sandık marketi kurar." : "§e/chestshop create <price> §7- Create player chest shop.");
        sender.sendMessage(tr ? "§e/payday §7- Zamanlanmış maaşınızı talep eder." : "§e/payday §7- Claim scheduled periodic salary.");
        sender.sendMessage("§8§m----------------------------------------------------");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("help");
            if (hasAdminPermission(sender)) {
                suggestions.add("reload");
                suggestions.add("license");
                suggestions.add("usages");
            }
            String prefix = args[0].toLowerCase();
            suggestions.removeIf(s -> !s.startsWith(prefix));
            return suggestions;
        }
        return Collections.emptyList();
    }
}
