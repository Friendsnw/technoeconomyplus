package com.techno.economy.util;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;

public final class BannerUtil {

    private BannerUtil() {}

    public static void printStartupBanner(String lang) {
        boolean tr = "tr".equalsIgnoreCase(lang);
        ConsoleCommandSender c = Bukkit.getConsoleSender();

        c.sendMessage("§6  _____ _____ ____ _   _ _   _  ___  _____ ____ ___  _   _  ___  __  ____   __");
        c.sendMessage("§e |_   _| ____/ ___| | | | \\ | |/ _ \\| ____/ ___/ _ \\| \\ | |/ _ \\|  \\/  \\ \\ / /");
        c.sendMessage("§a   | | |  _|| |   | |_| |  \\| | | | |  _|| |  | | | |  \\| | | | | |\\/| |\\ V / ");
        c.sendMessage("§b   | | | |__| |___|  _  | |\\  | |_| | |__| |__| |_| | |\\  | |_| | |  | | | |  ");
        c.sendMessage("§d   |_| |_____\\____|_| |_|_| \\_|\\___/|_____\\____\\___/|_| \\_|\\___/|_|  |_| |_|  ");
        c.sendMessage("§f   v1.0.0 §7- §bAdvanced Economy & Finance Suite");
        c.sendMessage("");

        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("windows")) {
            if (tr) {
                c.sendMessage("§e[TechnoEconomyPlus] ÖNERİ: Görünüşe göre bu sunucu, bir Windows ortamında çalışıyor. En iyi performansı alabilmeniz için Linux kullanmanızı öneririz.");
            } else {
                c.sendMessage("§e[TechnoEconomyPlus] RECOMMENDATION: This server appears to be running on Windows. For best performance, we recommend using Linux.");
            }
        }

        if (Bukkit.getPluginManager().getPlugin("ItemsAdder") != null) {
            if (tr) {
                c.sendMessage("§b[TechnoEconomyPlus] Sunucunuzda ItemsAdder algılanmıştır. Eklentimiz ItemsAdder'ı da desteklemektedir. Tüm menü ve mesajlarda kullanabilirsiniz (BETA).");
            } else {
                c.sendMessage("§b[TechnoEconomyPlus] ItemsAdder detected on your server. Our plugin also supports ItemsAdder. You may use it in all menus and messages (BETA).");
            }
        }

        if (Bukkit.getPluginManager().getPlugin("Essentials") != null
                || Bukkit.getPluginManager().getPlugin("EssentialsX") != null) {
            if (tr) {
                c.sendMessage("§c[TechnoEconomyPlus] Sunucunuzda bir Essentials eklentisi bulundu. Bu, çakışma yaratabilir. Silmeniz ya da devre dışı bırakmanız veya skript ile yönlendirmeniz önerilir.");
            } else {
                c.sendMessage("§c[TechnoEconomyPlus] An Essentials plugin was found on your server. This may cause conflicts. It is recommended to remove, disable, or redirect it via a script.");
            }
        }

        if (tr) {
            c.sendMessage("§a[TechnoEconomyPlus] Eklenti başarıyla başlatılıyor...");
            c.sendMessage("§a[TechnoEconomyPlus] Seçili dil: Türkçe (tr)");
            c.sendMessage("§a[TechnoEconomyPlus] Menüler ve yapılandırma dosyaları yüklendi.");
            c.sendMessage("§a[TechnoEconomyPlus] PlaceholderAPI bağlantısı kuruldu.");
            c.sendMessage("§b[TechnoEconomyPlus] Hoş geldiniz! İyi ve kazançlı oyunlar dileriz.");
        } else {
            c.sendMessage("§a[TechnoEconomyPlus] Plugin is initializing successfully...");
            c.sendMessage("§a[TechnoEconomyPlus] Active language: English (en)");
            c.sendMessage("§a[TechnoEconomyPlus] Menus and configuration files loaded.");
            c.sendMessage("§a[TechnoEconomyPlus] PlaceholderAPI hook established.");
            c.sendMessage("§b[TechnoEconomyPlus] Welcome! Wishing you a great server experience.");
        }
    }

    public static void printStarMessage(String lang) {
        boolean tr = "tr".equalsIgnoreCase(lang);
        ConsoleCommandSender c = Bukkit.getConsoleSender();
        if (tr) {
            c.sendMessage("§6[TechnoEconomyPlus] Eklentimizi beğenirseniz, bir yıldız verirseniz çok destek vermiş olursunuz >3");
        } else {
            c.sendMessage("§6[TechnoEconomyPlus] If you enjoy our plugin, giving it a star would mean a lot to us >3");
        }
    }

    public static void printShutdownMessages(String lang) {
        boolean tr = "tr".equalsIgnoreCase(lang);
        ConsoleCommandSender c = Bukkit.getConsoleSender();

        if (tr) {
            c.sendMessage("§e[TechnoEconomyPlus] Eklenti güvenli bir şekilde kapatılıyor...");
            c.sendMessage("§e[TechnoEconomyPlus] Oyuncu bakiyeleri ve banka hesapları veri tabanına kaydediliyor...");
            c.sendMessage("§e[TechnoEconomyPlus] Şirket, borsa ve açık artırma verileri güvenceye alındı.");
            c.sendMessage("§e[TechnoEconomyPlus] Çek ve düello kayıtları kaydedildi.");
            c.sendMessage("§e[TechnoEconomyPlus] Veri tabanı bağlantıları sorunsuz şekilde sonlandırıldı.");
            c.sendMessage("§c[TechnoEconomyPlus] technoEconomyPlus devre dışı bırakıldı. Güle güle!");
        } else {
            c.sendMessage("§e[TechnoEconomyPlus] Plugin is shutting down safely...");
            c.sendMessage("§e[TechnoEconomyPlus] Saving player balances and bank accounts to database...");
            c.sendMessage("§e[TechnoEconomyPlus] Company, stock market, and auction data secured.");
            c.sendMessage("§e[TechnoEconomyPlus] Check and wager records stored.");
            c.sendMessage("§e[TechnoEconomyPlus] Database connections closed cleanly.");
            c.sendMessage("§c[TechnoEconomyPlus] technoEconomyPlus disabled. Goodbye!");
        }
    }

    public static String getOsInfo() {
        return System.getProperty("os.name", "Unknown") + " " + System.getProperty("os.version", "");
    }

    public static String getCpuInfo() {
        return System.getProperty("os.arch", "Unknown") + " (Cores: " +
                Runtime.getRuntime().availableProcessors() + ")";
    }

    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
