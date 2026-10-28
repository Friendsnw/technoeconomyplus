package com.techno.economy.license;

public final class LicenseMessages {

    private LicenseMessages() {}

    public static String getNoInternetMessage(boolean tr) {
        return tr
                ? "İnternet bağlantısı kurulamadı! Lütfen sunucunuzun internet erişimini ve DNS ayarlarını kontrol edin."
                : "Could not establish an internet connection! Please check your server internet access and DNS settings.";
    }

    public static String getHttp500Message(boolean tr) {
        return tr
                ? "Lisans sunucusunda dahili hata oluştu (HTTP 500). Web sunucunuzdaki veritabanı (MySQL) bağlantısını ve yapılandırmasını kontrol edin."
                : "Internal license server error occurred (HTTP 500). Please check your web server database (MySQL) connection and settings.";
    }

    public static String getHttp403Message(boolean tr) {
        return tr
                ? "Lisans API erişimi reddedildi (HTTP 403). İstek yetkilendirme parametreleri eksik veya geçersiz."
                : "License API access denied (HTTP 403). Request authorization parameters are missing or invalid.";
    }

    public static String getHttp404Message(boolean tr) {
        return tr
                ? "Lisans doğrulama uç noktası bulunamadı (HTTP 404). API dosya yolunun (license-info.php) doğruluğunu kontrol edin."
                : "License verification endpoint not found (HTTP 404). Please verify the API file path (license-info.php).";
    }

    public static String getInvalidKeyMessage(boolean tr) {
        return tr
                ? "Girilen lisans anahtarı geçersiz veya sistemde kayıtlı değil. Lütfen config.yml dosyasındaki anahtarı kontrol edin."
                : "The entered license key is invalid or not registered. Please verify the key in config.yml.";
    }

    public static String getExpiredKeyMessage(boolean tr) {
        return tr
                ? "Lisans anahtarınızın kullanım süresi dolmuştur. Lütfen lisansınızı yenileyin."
                : "Your license key has expired. Please renew your license.";
    }

    public static String getIpMismatchMessage(boolean tr) {
        return tr
                ? "Bu lisans anahtarı farklı bir sunucu IP adresine kilitlenmiş. Sunucu IP adresiniz eşleşmiyor."
                : "This license key is locked to a different server IP address. Your server IP does not match.";
    }

    public static String getMaxServersMessage(boolean tr) {
        return tr
                ? "Bu lisans anahtarı için tanımlanmış maksimum sunucu limitine ulaşıldı."
                : "The maximum server limit defined for this license key has been reached.";
    }

    public static String getPluginNotLicensedMessage(boolean tr) {
        return tr
                ? "Bu lisans anahtarı technoEconomyPlus eklentisi için yetkilendirilmemiş."
                : "This license key is not authorized for the technoEconomyPlus plugin.";
    }

    public static String getInactiveMessage(boolean tr) {
        return tr
                ? "Bu lisans anahtarı devre dışı bırakılmış veya askıya alınmıştır."
                : "This license key has been deactivated or suspended.";
    }

    public static String getEmptyKeyMessage(boolean tr) {
        return tr
                ? "config.yml dosyasında herhangi bir lisans anahtarı tanımlanmamış."
                : "No license key is defined in config.yml.";
    }

    public static String getConnectingLog(boolean tr, String host) {
        return tr
                ? "Lisans sunucusuna baglaniliyor: " + host
                : "Connecting to license server: " + host;
    }

    public static String getChallengeLog(boolean tr) {
        return tr
                ? "Sunucu bot guvenlik dogrulamasi cozuluyor..."
                : "Solving server anti-bot challenge...";
    }

    public static String getChallengeRefreshLog(boolean tr) {
        return tr
                ? "Guvenlik anahtari yenileniyor..."
                : "Refreshing security challenge token...";
    }

    public static String getSuccessLog(boolean tr, String pkg) {
        return tr
                ? "Lisans basariyla dogrulandi! Paket: " + pkg
                : "License successfully verified! Package: " + pkg;
    }

    public static String getBoxTitle(boolean tr) {
        return tr
                ? "Üzgünüz, technoEconomyPlus etkinleştirilemiyor."
                : "Sorry, technoEconomyPlus could not be enabled.";
    }

    public static String getWebsiteLabel(boolean tr) {
        return tr
                ? "Web sitemiz: "
                : "Our website: ";
    }

    public static String getCurrentKeyLabel(boolean tr) {
        return tr
                ? "Mevcut anahtar: "
                : "Current key: ";
    }

    public static String getNoKeyDisplay(boolean tr) {
        return tr
                ? "Geçerli bir anahtar bulunamadı"
                : "No valid key found";
    }

    public static String getNoPermissionMessage(boolean tr) {
        return tr
                ? "§cBu komutu kullanmak için yetkiniz yok!"
                : "§cYou do not have permission to use this command!";
    }

    public static String getUnknownSubcommandMessage(boolean tr) {
        return tr
                ? "§cBöyle bir alt komut bulunamadı! Komut listesi için: /technoeconomyplus help"
                : "§cUnknown subcommand! For command list use: /technoeconomyplus help";
    }

    public static String getReloadSuccessMessage(boolean tr) {
        return tr
                ? "§aTüm yapılandırma ve dil dosyaları başarıyla yeniden yüklendi!"
                : "§aAll configuration and language files have been successfully reloaded!";
    }

    public static String getReloadErrorMessage(boolean tr, String error) {
        return tr
                ? "§cYeniden yükleme sırasında bir hata oluştu: " + error
                : "§cAn error occurred during reload: " + error;
    }

    public static String getLicenseHeader(boolean tr) {
        return tr
                ? "§8§m----------------------------------------------------\n§6§lTECHNOECONOMYPLUS §8» §fLisans Bilgileri\n§8§m----------------------------------------------------"
                : "§8§m----------------------------------------------------\n§6§lTECHNOECONOMYPLUS §8» §fLicense Information\n§8§m----------------------------------------------------";
    }

    public static String getLicenseFooter() {
        return "§8§m----------------------------------------------------";
    }

    public static String getLicenseStatusLabel(boolean tr, boolean active) {
        if (tr) {
            return "§7Lisans Durumu: " + (active ? "§aAktif" : "§cDevre Dışı / Geçersiz");
        }
        return "§7License Status: " + (active ? "§aActive" : "§cInactive / Invalid");
    }

    public static String getPackageLabel(boolean tr, String pkg) {
        return tr
                ? "§7Paket: §e" + (pkg != null ? pkg : "starter")
                : "§7Package: §e" + (pkg != null ? pkg : "starter");
    }

    public static String getIpLabel(boolean tr, String ip) {
        if (ip == null || ip.isBlank()) {
            return tr ? "§7Kilitli IP: §fTüm IP adresleri (Serbest)" : "§7Bound IP: §fAny IP address (Unrestricted)";
        }
        return tr ? "§7Kilitli IP: §f" + ip : "§7Bound IP: §f" + ip;
    }

    public static String getExpiryLabel(boolean tr, String expiresAt, int daysRemaining) {
        if (expiresAt == null || expiresAt.isBlank() || daysRemaining < 0) {
            return tr ? "§7Son Kullanım: §fSüresiz (Ömür Boyu)" : "§7Expiration: §fLifetime (Permanent)";
        }
        if (tr) {
            return "§7Son Kullanım: §f" + expiresAt + " §8(§e" + daysRemaining + " gün kaldı§8)";
        }
        return "§7Expiration: §f" + expiresAt + " §8(§e" + daysRemaining + " days remaining§8)";
    }
}
